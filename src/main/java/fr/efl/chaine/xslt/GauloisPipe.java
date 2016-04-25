/**
 * This Source Code Form is subject to the terms of 
 * the Mozilla Public License, v. 2.0. If a copy of 
 * the MPL was not distributed with this file, You 
 * can obtain one at https://mozilla.org/MPL/2.0/.
 */
package fr.efl.chaine.xslt;

import fr.efl.chaine.xslt.utils.GauloisPipeURIResolver;
import fr.efl.chaine.xslt.utils.ParameterValue;
import fr.efl.chaine.xslt.config.CfgFile;
import fr.efl.chaine.xslt.config.Config;
import fr.efl.chaine.xslt.config.ConfigUtil;
import fr.efl.chaine.xslt.config.JavaStep;
import fr.efl.chaine.xslt.config.Output;
import fr.efl.chaine.xslt.config.ParametrableStep;
import fr.efl.chaine.xslt.config.Pipe;
import fr.efl.chaine.xslt.config.Tee;
import fr.efl.chaine.xslt.config.Xslt;
import fr.efl.chaine.xslt.utils.ParametersMerger;
import fr.efl.chaine.xslt.utils.ParametrableFile;
import fr.efl.chaine.xslt.utils.SaxonConfigurationFactory;
import net.sf.saxon.s9api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URI;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import net.sf.saxon.Configuration;
import net.sf.saxon.event.ProxyReceiver;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.trans.XPathException;

/**
 * The saxon pipe.
 */
public class GauloisPipe {

    /**
     * Logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(GauloisPipe.class);
    public static final String INSTANCE_DEFAULT_NAME = "instance1";
    
    private String instanceName;
    /**
     * The minimal number of arguments in command line.
     */
    private static final int MIN_ARGS_LEN = 6;
    /**
     * the input-file parameter qname.
     */
    private static final QName INPUT_FILE_NAME = new QName("input-file");
    /**
     * The uri mapping key to load mapping rule from system properties.
     */
    private static final String URI_MAPPING_KEY = "URIMapping-";
    private final Config config;
    
    private final Map<String,XsltExecutable> xslCache;
    
    private Processor processor;
    
    /**
     * Message listener for XSLT-SAXON messages (xsl:message)
     */
    private Class<MessageListener> messageListenerclass;
    private MessageListener messageListener = null;
    private final DocumentCache documentCache;
    private XsltCompiler xsltCompiler;
    private DocumentBuilder builder = null;

    
    /**
     * The default contructor.
     *
     * @param config
     * @param instanceName Le nom de l'instance de saxon-pipe à utiliser dans les logs
     */
    public GauloisPipe(Config config, String instanceName) {
        super();
        this.config = config;
        this.instanceName=instanceName;
        xslCache = new HashMap<>();
        documentCache = new DocumentCache(config.getMaxDocumentCacheSize());
    }
    /**
     * Pour compatibilité ascendante
     * @param config 
     */
    public GauloisPipe(Config config) {
        this(config,INSTANCE_DEFAULT_NAME);
    }

    /**
     * The constructor with detail config.
     *
     * @param inputs the input files
     * @param outputDirectory the output directory
     * @param templatePaths the template paths
     * @param nbThreads the nbThreads
     * @param instanceName
     * @throws fr.efl.chaine.xslt.InvalidSyntaxException
     */
    public GauloisPipe(List<String> inputs, String outputDirectory,List<String> templatePaths, int nbThreads, String instanceName) throws InvalidSyntaxException {
        super();
        this.instanceName=instanceName;
        Config cfg = new Config();
        xslCache = new HashMap<>();

        for (String input : inputs) {
            ConfigUtil.addInputFile(cfg, input);
        }
        ConfigUtil.setOutput(cfg, outputDirectory);
        for (String templatePath : templatePaths) {
            ConfigUtil.addTemplate(cfg, templatePath);
        }
        ConfigUtil.setNbThreads(cfg, Integer.toString(nbThreads));
        cfg.verify();
        this.config = cfg;
        documentCache = new DocumentCache(config.getMaxDocumentCacheSize());
    }

    /**
     * Launch the pipe.
     *
     * @throws fr.efl.chaine.xslt.InvalidSyntaxException
     * @throws java.io.FileNotFoundException
     * @throws net.sf.saxon.s9api.SaxonApiException
     * @throws java.net.URISyntaxException
     */
    public void launch() throws InvalidSyntaxException, FileNotFoundException, SaxonApiException, URISyntaxException, IOException {
        long start = System.currentTimeMillis();
        if (this.messageListenerclass != null) {
            try {
                this.messageListener = this.messageListenerclass.newInstance();
            } catch (InstantiationException | IllegalAccessException ex) {
                System.err.println("[WARN] Fail to instanciate " + this.messageListenerclass.getName());
                ex.printStackTrace(System.err);
            }
        }
        boolean retCode = true;
        try {
            Configuration saxonConfig = SaxonConfigurationFactory.buildConfiguration();
            saxonConfig.setURIResolver(buildUriResolver(saxonConfig.getURIResolver()));
            processor = new Processor(saxonConfig);
            xsltCompiler = processor.newXsltCompiler();
            builder = processor.newDocumentBuilder();

            List<CfgFile> sourceFiles = config.getSources().getFiles();
            LOGGER.info("[" + instanceName + "] works on {} files", sourceFiles.size());
            

            if (config.getPipe().getNbThreads() > 1) {
                if (config.hasFilesOverMultiThreadLimit()) {
                    List<ParametrableFile> files = new ArrayList<>(sourceFiles.size());
                    for (CfgFile f : config.getSources().getFilesOverLimit(config.getPipe().getMultithreadMaxSourceSize())) {
                        files.add(resolveInputFile(f));
                    }
                    if (!files.isEmpty()) {
                        LOGGER.info("[" + instanceName + "] Running mono-thread for {} huge files", files.size());
                        retCode = executesPipeOnMonoThread(config.getPipe(), files);
                    }
                }
                List<ParametrableFile> files = new ArrayList<>(sourceFiles.size());
                for (CfgFile f : config.getSources().getFilesUnderLimit(config.getPipe().getMultithreadMaxSourceSize())) {
                    files.add(resolveInputFile(f));
                }
                if (!files.isEmpty()) {
                    LOGGER.info("[" + instanceName + "] Running multi-thread for {} regular-size files", files.size());
                    retCode = executesPipeOnMultiThread(config.getPipe(), files, config.getPipe().getNbThreads());
                }
            } else {
                List<ParametrableFile> files = new ArrayList<>(sourceFiles.size());
                for (CfgFile f : sourceFiles) {
                    files.add(resolveInputFile(f));
                }
                LOGGER.info("[" + instanceName + "] Running mono-thread on all {} files", files.size());
                retCode = executesPipeOnMonoThread(config.getPipe(), files);
            }

        } catch (Throwable e) {
            LOGGER.warn("[" + instanceName + "] " + e.getMessage(), e);
            // on sort avec des codes d'erreur non-zero
            throw e;
        } finally {
            if(!retCode) {
                throw new SaxonApiException("An error occurs. See previous logs.");
            }
        }
        try {
            long duration = System.currentTimeMillis() - start;
            Duration duree = DatatypeFactory.newInstance().newDuration(duration);
            LOGGER.info("[" + instanceName + "] Process terminated: "+duree.toString());
        } catch(Exception ex) {
            LOGGER.info("[" + instanceName + "] Process terminated.");
        }
    }

    public int getDocumentCacheSize() {
        return documentCache.size();
    }
    public int getXsltCacheSize() {
        return xslCache.size();
    }
    private ParametrableFile resolveInputFile(CfgFile file) {
        ParametrableFile ret = new ParametrableFile(file.getSource());
        for (ParameterValue p : file.getParams()) {
            ret.getParameters().add(p);
        }
        return ret;
    }

    /**
     * Execute the specified templates on the specified files to the specified
     * output directory on the specified number of threads.
     *
     * @param templates the specified templates
     * @param inputs the specified input files
     * @param outputDirectory the specified output directory
     * @param nbThreads the specified number of thread
     * @param processor the processor
     * @return <tt>false</tt> if an error occurs while processing.
     */
    private boolean executesPipeOnMultiThread(
            final Pipe pipe,
            List<ParametrableFile> inputs,
            int nbThreads) {
        ExecutorService service = Executors.newFixedThreadPool(nbThreads);
        for(ParametrableFile pf: inputs) {
            final ParametrableFile fpf = pf;
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    try {
                        execute(pipe, fpf, messageListener);
                    } catch(SaxonApiException | MalformedURLException | InvalidSyntaxException | URISyntaxException | FileNotFoundException ex) {
                        LOGGER.error("[" + instanceName + "] while processing "+fpf.getFile().getName(), ex);
                    }
                }
            };
            service.execute(r);
        }
	// on ajoute plus rien
	service.shutdown();
        try {
            service.awaitTermination(5, TimeUnit.HOURS);
            return true;
        } catch (InterruptedException ex) {
            LOGGER.error("[" + instanceName + "] multi-thread processing interrupted, 5 hour limit exceed.");
        }
        return false;
    }

    /**
     * Execute the specified templates on the specified files to the specified
     * output directory in a single thread.
     *
     * @param templates the specified templates
     * @param inputs the specified input files
     * @param outputDirectory the specified output directory
     * @param test the test value "oui" or "non"
     * @throws SaxonApiException when a problem occurs
     * @throws IOException when a problem occurs
     */
    private boolean executesPipeOnMonoThread(Pipe pipe, List<ParametrableFile> inputs) {
        boolean ret = true;
        for (ParametrableFile inputFile : inputs) {
            try {
                execute(pipe, inputFile, messageListener);
            } catch(SaxonApiException | MalformedURLException | InvalidSyntaxException | URISyntaxException | FileNotFoundException ex) {
                LOGGER.error("[" + instanceName + "] while mono-thread processing of "+inputFile.getFile().getName(),ex);
                ret = false;
            }
        }
        return ret;
    }

    /**
     * Execute the pipe for the specified input stream to the specified
     * serializer.
     *
     * @param pipe the pipe to run
     * @param input the specified input stream
     * @param inputFilePath the input file path of the specified input stream
     * @param parameters the parameters
     * @param listener the message listener to use
     * @return the result
     * @throws SaxonApiException when a problem occurs
     */
    private void execute(Pipe pipe, ParametrableFile input, MessageListener listener)
            throws SaxonApiException, MalformedURLException, InvalidSyntaxException, URISyntaxException, FileNotFoundException {
        boolean avoidCache = input.getAvoidCache();
        long start = System.currentTimeMillis();
        String key = input.getFile().getAbsolutePath().intern();
        XdmNode source = documentCache.get(key);
        if(source==null || avoidCache) {
            if(!avoidCache && documentCache.isLoading(instanceName)) {
                source = documentCache.waitForLoading(key);
            }
            if(source==null || avoidCache) {
                synchronized(key) {
                    if(!avoidCache) {
                        source = documentCache.get(key);
                    }
                    if(source==null) {
                        documentCache.setLoading(key);
                        if(config.isLogFileSize()) {
                            LOGGER.info("["+instanceName+"] "+input.toString()+" as input: "+input.getFile().length());
                        }
                        source = builder.build(input.getFile());
                        if(!avoidCache && config.getSources().getFileUsage(input.getFile())>1) {
                            // on ne le met en cache que si il est utilisé plusieurs fois !
                            LOGGER.debug("["+instanceName+"] mise en cache de "+key);
                            documentCache.put(key, source);
                        } else {
                            documentCache.ignoreLoading(key);
                            if(avoidCache){
                                LOGGER.debug("["+instanceName+"] "+key+" est explicitement exclu du cache");
                            } else {
                                LOGGER.debug("["+instanceName+"] "+key+" n'est utilisé qu'une fois : pas de mise en cache");
                            }
                        }
                    }
                }
            }
        }

        XsltTransformer transformer = buildTransformer(
                pipe, 
                input.getFile(), 
                input.getFile().toURI().toURL().toExternalForm(), 
                ParametersMerger.merge(input.getParameters(), config.getParams()),
                listener);
        LOGGER.debug("["+instanceName+"] transformer build");
        transformer.setInitialContextNode(source);
        transformer.transform();
        long duration = System.currentTimeMillis() - start;
        String distinctName = input.toString();
        try {
            Duration duree = DatatypeFactory.newInstance().newDuration(duration);
            LOGGER.info("["+instanceName+"] - "+distinctName+" - transform terminated: "+duree.toString());
        } catch(Exception ex) {
            LOGGER.info("["+instanceName+"] - "+distinctName+" - transform terminated");
        }
    }
    
    private XsltTransformer buildTransformer(Pipe pipe, File inputFile, String inputFileUri, List<ParameterValue> parameters, MessageListener listener) throws InvalidSyntaxException, URISyntaxException, MalformedURLException, SaxonApiException, FileNotFoundException {
        XsltTransformer first = null;
        Iterator<ParametrableStep> it = pipe.getXslts();
        Object previousTransformer = null;
        while(it.hasNext()) {
            ParametrableStep step = it.next();
            if(step instanceof Xslt) {
                Xslt xsl = (Xslt)step;
                XsltTransformer currentTransformer = getXsltTransformer(xsl.getHref(), parameters);
                if(listener!=null) {
                    currentTransformer.setMessageListener(listener);
                }
                for(ParameterValue pv:xsl.getParams()) {
                    // on substitue les paramètres globaux dans ceux de la XSL
                    String value = ParametersMerger.processParametersReplacement(pv.getValue(), parameters);
                    currentTransformer.setParameter(new QName(pv.getKey()), new XdmAtomicValue(value));
                }
                for(ParameterValue pv:parameters) {
                    // la substitution a été faite avant, dans le merge
                    currentTransformer.setParameter(new QName(pv.getKey()), new XdmAtomicValue(pv.getValue()));
                }
                if(first==null) {
                    first = currentTransformer;
                }
                if(previousTransformer!=null) {
                    assignStepToDestination(previousTransformer, currentTransformer);
                }
                previousTransformer = currentTransformer;
            } else if(step instanceof JavaStep) {
                JavaStep javaStep = (JavaStep)step;
                try {
                    StepJava stepJava = javaStep.getStepClass().newInstance();
                    for(ParameterValue pv:javaStep.getParams()) {
                        stepJava.setParameter(new QName(pv.getKey()), new XdmAtomicValue(pv.getValue()));
                    }
                    for(ParameterValue pv:parameters) {
                        // la substitution a été faite avant, dans le merge
                        stepJava.setParameter(new QName(pv.getKey()), new XdmAtomicValue(pv.getValue()));
                    }
                    if(previousTransformer!=null) {
                        assignStepToDestination(previousTransformer, stepJava);
                    }
                    previousTransformer = stepJava;
                } catch (InstantiationException | IllegalAccessException ex) {
                    throw new InvalidSyntaxException(ex);
                }
                
            }

        }
        Destination nextStep = null;
        if(pipe.getTee()!=null) {
            nextStep = buildTransformer(pipe.getTee(), inputFile, inputFileUri, parameters, listener);
        } else if(pipe.getOutput()!=null) {
            nextStep = buildSerializer(pipe.getOutput(),inputFile,parameters);
        }
        if(nextStep!=null) {
            assignStepToDestination(previousTransformer, nextStep);
        } else {
            throw new InvalidSyntaxException("Pipe "+pipe.toString()+" has no terminal Step.");
        }
        return first;
    }
    
    private void assignStepToDestination(Object assignee, Destination assigned) throws IllegalArgumentException {
        if(assignee==null) throw new IllegalArgumentException("assignee must not be null");
        if(assigned==null) throw new IllegalArgumentException("assigned must not be null");
        if(assignee instanceof XsltTransformer) {
            ((XsltTransformer)assignee).setDestination(assigned);
        } else if(assignee instanceof StepJava) {
            ((StepJava)assignee).setDestination(assigned);
        } else {
            throw new IllegalArgumentException("assignee must be either a XsltTransformer or a SteJava instance");
        }
    }
    
    private XsltTransformer getXsltTransformer(String href, Collection<ParameterValue> parameters) throws MalformedURLException, SaxonApiException, URISyntaxException, FileNotFoundException {
        String __href = ParametersMerger.processParametersReplacement(href, parameters);
        XsltExecutable xsl = xslCache.get(__href);
        if(xsl==null) {
            File f;
            if(__href.startsWith("file:")) {
                f = new File(new URI(__href));
            } else {
                f = new File(__href);
            }
            FileInputStream input = new FileInputStream(f);
            StreamSource source = new StreamSource(input);
            source.setSystemId(f);
            
            xsl = xsltCompiler.compile(source);
            xslCache.put(__href, xsl);
        }
        return xsl.load();
    }
    
    private Destination buildTransformer(Tee tee, File inputFile, String inputFileUri, List<ParameterValue> parameters, MessageListener listener) throws InvalidSyntaxException, URISyntaxException, MalformedURLException, SaxonApiException, FileNotFoundException {
        Destination dest1 = buildShortPipeTransformer(tee.getPipe1(), inputFile, inputFileUri, parameters, listener);
        Destination dest2 = buildShortPipeTransformer(tee.getPipe2(), inputFile, inputFileUri, parameters, listener);
        TeeDestination teeDest = new TeeDestination(dest1, dest2);
        return teeDest;
    }
    private Destination buildShortPipeTransformer(Pipe pipe, File inputFile, String inputFileUri, List<ParameterValue> parameters, MessageListener listener) throws InvalidSyntaxException, URISyntaxException, MalformedURLException, SaxonApiException, FileNotFoundException {
        if(!pipe.getXslts().hasNext()) {
            if(pipe.getOutput()!=null) {
                return buildSerializer(pipe.getOutput(),inputFile, parameters);
            } else {
                return buildTransformer(pipe.getTee(), inputFile, inputFileUri, parameters, listener);
            }
        } else {
            return buildTransformer(pipe, inputFile, inputFileUri, parameters, listener);
        }
    }
    
    private Destination buildSerializer(Output output, File inputFile, List<ParameterValue> parameters) throws InvalidSyntaxException, URISyntaxException {
        final File destinationFile = output.getDestinationFile(inputFile, parameters);
        final Serializer ret = processor.newSerializer(destinationFile);
        Properties outputProps = output.getOutputProperties();
        for(Object key: outputProps.keySet()) {
            ret.setOutputProperty(Output.VALID_OUTPUT_PROPERTIES.get(key.toString()).getSaxonProperty(), outputProps.getProperty(key.toString()));
        }
        if(config.isLogFileSize()) {
            Destination dest = new Destination() {
                @Override
                public Receiver getReceiver(Configuration c) throws SaxonApiException {
                    return new ProxyReceiver(ret.getReceiver(c)) {
                        @Override
                        public void close() throws XPathException {
                            super.close();
                            LOGGER.info("["+instanceName+"] Written "+destinationFile.getAbsolutePath()+": "+destinationFile.length());
                        }
                    };
                }
                @Override
                public void close() throws SaxonApiException {
                    ret.close();
                }
            };
            return dest;
        } else {
            return ret;
        }
    }
    
    /**
     * Build the uri resolver.
     *
     * @param defaultUriResolver Default Saxon's URIResolver
     * @return the uri resolver built
     */
    protected URIResolver buildUriResolver(URIResolver defaultUriResolver) {
        LOGGER.info("["+instanceName+"] BuildUriResolver");
        return new GauloisPipeURIResolver(defaultUriResolver, buildUriMapping());
    }

    /**
     * Build URI mapping from system property.
     *
     * @return
     */
    protected Map<String, String> buildUriMapping() {
        LOGGER.info("["+instanceName+"] BuildUriMapping");
        Map<String, String> uriMapping = new HashMap<>();
        for (String propertyName : System.getProperties().stringPropertyNames()) {
            if (propertyName.startsWith(URI_MAPPING_KEY)) {
                String filename = propertyName.substring(URI_MAPPING_KEY.length());
                String filepath = System.getProperty(propertyName);
                LOGGER.debug("["+instanceName+"] BuildUriMapping urimapping with {} to {}", filename, filepath);
                uriMapping.put(filename, filepath);
            }
        }
        return uriMapping;
    }


    /**
     * Main entry point of saxon xslt pipe.<br/>
     * The arguments are :
     * <ul>
     * <li><tt>--config
     * config-file.xml&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</tt>the
     * pipe-definition config file (optional)</li>
     * <li><tt>-o, --output outputDirectory&nbsp;&nbsp;</tt>the output
     * directory</li>
     * <li><tt>-n,--nbthreads
     * n&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</tt>the
     * number of threads</li>
     * <li><tt>PARAMS p1=v1
     * p2=v2&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</tt>the
     * parameters to give to XSL
     * <li><tt>XSL f1.xsl f2.xsl
     * ...&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</tt>the
     * stylesheets list used by the pipe</li>
     * <li><tt>FILES file1.xml file2.xml ...&nbsp;</tt>the files to process</li>
     * </ul>
     * <p>
     * If <tt>--config</tt> option is given, it must be in first position, and
     * all other options may be ignored. It is impossible to overwrite a
     * config-file.xml option via command-line option, but it is possible to add
     * XSL, input files and parameters</p>
     * <p>
     * It is possible to define a pipe parameter for a single input file or a
     * single XSL : just add parenthesis after the file/xsl URI, with
     * comma-separated list of parameter=value</p>
     * <p>
     * Command samples :</p>
     * <ul>
     * <li><tt>SaxonPipe -o path/to/out -n 4 PARAMS p1=xxx p2=yyy XSL
     * path/to/xslt1 ... path/to/xsltN FILES path/to/input1 ...
     * path/to/inputN</tt>
     * <ul><li>Executes with 4 threads the pipe <tt>xslt1 to xsltN</tt> on
     * <tt>input1 to inputN</tt>, and writes output to
     * <tt>path/to/out</tt></li></ul></li>
     * <li><tt>SaxonPipe --config config.xml FILES f12.xml(foo=bar)</tt>
     * <ul><li>Executes pipe defined in <tt>config.xml</tt> on files defined in
     * <tt>config.xml</tt> <b>AND</b> on <tt>f12.xml</tt>, with param
     * <tt>foo</tt>=<tt>bar</tt></li></ul></li>
     * </ul>
     * <p>
     * Files whom size is over multi-thread limit (default is 10Mb) are
     * processed first, on a single-thread waiting queue. Then, files whom size
     * is under limit are processed on the multi-threaded waiting queue.</p>
     * <p>
     * Using config file gives much more control on Saxon-pipe on things taht
     * can't be set via command-line :</p>
     * <ul><li>pipe definition is enhanced</li>
     * <li>output naming strategy can be defined</li>
     * <li>pattern-matching and recursion can be applied on input-files</li>
     * <li>file-process order and on multi-thread limit size can be defined</li>
     * </ul>
     *
     * @param args the arguments
     */
    public static void main(String[] args) {
        try {
            GauloisPipe saxonPipe = parseCommandLine(args);
            saxonPipe.launch();
        } catch (InvalidSyntaxException ex) {
            LOGGER.error(ex.getMessage(), ex);
            System.exit(-1);
        } catch (SaxonApiException ex) {
            System.exit(1);
        } catch (URISyntaxException ex) {
            System.exit(2);
        } catch (IOException ex) {
            System.exit(3);
        }
    }
    
    private static GauloisPipe parseCommandLine(String[] args) throws InvalidSyntaxException {
        List<String> inputFiles = new ArrayList<>();
        List<String> inputParams = new ArrayList<>();
        List<String> inputXsls = new ArrayList<>();
        String nbThreads = null;
        String inputOutput = null;
        String messageListener = null;
        String configFileName = null;
        String __instanceName = INSTANCE_DEFAULT_NAME;
        boolean logFileSize = false;
        int inputMode = -1;
        for (String argument : args) {
            if (null != argument) {
                switch (argument) {
                    case "PARAMS":
                        inputMode = INPUT_PARAMS;
                        continue;
                    case "FILES":
                        inputMode = INPUT_FILES;
                        continue;
                    case "XSL":
                        inputMode = INPUT_XSL;
                        continue;
                    case "--output":
                    case "-o":
                        inputMode = INPUT_OUTPUT;
                        continue;
                    case "--nbthreads":
                    case "-n":
                        inputMode = INPUT_THREADS;
                        continue;
                    case "--msg-listener":
                        inputMode = MESSAGE_LISTENER;
                        continue;
                    case "--instance-name":
                    case "-iName":
                        inputMode = INSTANCE_NAME;
                        continue;
                    case "--config":
                        inputMode = CONFIG;
                    case "--logFileSize":
                        logFileSize=true;
                }
            }
                
            switch(inputMode) {
                case INPUT_FILES:
                    inputFiles.add(argument); break;
                case INPUT_PARAMS:
                    inputParams.add(argument); break;
                case INPUT_XSL:
                    inputXsls.add(argument); break;
                case INPUT_THREADS:
                    nbThreads = argument; break;
                case INPUT_OUTPUT:
                    inputOutput = argument; break;
                case MESSAGE_LISTENER:
                    messageListener = argument ; break;
                case INSTANCE_NAME:
                    __instanceName = argument; break;
                case CONFIG: 
                    configFileName = argument; break;
            }
        }
        Collection<ParameterValue> inputParameters = new ArrayList<>(inputParams.size());
        for(String paramPattern:inputParams) {
            inputParameters.add(ConfigUtil.parseParameterPattern(paramPattern));
        }
        Config config;
        if(configFileName!=null) {
            config=parseConfig(configFileName, inputParameters);
        } else {
            config = new Config();
        }
        for(ParameterValue pv: inputParameters) config.addParameter(pv);
        for(String inputFile: inputFiles) ConfigUtil.addInputFile(config, inputFile);
        for(String inputXsl: inputXsls) ConfigUtil.addTemplate(config, inputXsl);
        if(nbThreads!=null) ConfigUtil.setNbThreads(config, nbThreads);
        if(inputOutput!=null) ConfigUtil.setOutput(config, inputOutput);
        config.setLogFileSize(logFileSize);
        config.verify();
        GauloisPipe saxonPipe = new GauloisPipe(config, __instanceName);
        if (messageListener != null) {
            try {
              Class c = Class.forName(messageListener);
              if (MessageListener.class.isAssignableFrom(c))
                saxonPipe.messageListenerclass = c;
            }
            catch (Exception ex) {
              System.err.println("[WARN] Message Listener will not be set :");
              ex.printStackTrace(System.err);
            }
        }
        return saxonPipe;
    }

    private static Config parseConfig(String fileName, Collection<ParameterValue> inputParameters) throws InvalidSyntaxException {
        try {
            return new ConfigUtil(fileName).buildConfig(inputParameters);
        } catch (SaxonApiException ex) {
            throw new InvalidSyntaxException(ex);
        }
    }

    private static final transient String USAGE_PROMPT = "\nUSAGE:\njava " + GauloisPipe.class.getName() + "\n"
            + "\t--config config.xml\tthe config file to use\n"
            + "\t--msg-listener package.of.MessageListener The class to use as MessageListener\n"
            + "\t{--instance-name | -iName} <name>\t\tthe instance name to use in logs\n"
            + "\t{--output | -o} <outputfile>\t\t\toutput directory\n"
            + "\t{--nbthreads | -n} <n>\t\t\tnumber of threads to use\n"
            + "\t{--logFileSize}\t\t\tdisplays intput and output files size in logs as INFO\n"
            + "\txsl_file[ xsl_file]*\t\tthe XSLs to pipe\n"
            + "\t[PARAMS p1=xxx[ p2=yyy]*]\tthe params to give to XSLs\n"
            + "\tFILES file1[ filen]*\t\tthe files to apply pipe on\n.\n"
            + "\tAn XSL may be specified as this, if it needs specials parameters :\n"
            + "\t\txsl_file(param1=value1,param2=value2,...)\n"
            + "\tA source file may be specified as this, if it needs special parameters :\n"
            + "\t\tfile(param1=value1,param2=value2,...)\n\n"
            + "\tIt is impossible to override via command-line option something defined in config file, but it is possible to add inputs or templates.\n"
            + "\tIf a MessageListener is specified, the denoted class must implement net.sf.saxon.s9api.MessageListener";
    private static final int INPUT_XSL = 0x00;
    private static final int INPUT_PARAMS = 0x01;
    private static final int INPUT_FILES = 0x02;
    private static final int INPUT_OUTPUT = 0x04;
    private static final int INPUT_THREADS = 0x08;
    private static final int MESSAGE_LISTENER = 0x0F;
    private static final int INSTANCE_NAME = 0x10;
    private static final int CONFIG = 0x20;
    
    private class DocumentCache extends LinkedHashMap<String, XdmNode> {
        private final int cacheSize;
        private List<String> loading;
        public DocumentCache(final int cacheSize) {
            super();
            if(cacheSize<1) throw new IllegalArgumentException("cacheSize must be at least 1");
            this.cacheSize=cacheSize;
            loading = new ArrayList<>(10);
        }
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, XdmNode> eldest) {
            boolean ret = size()==cacheSize;
            if(ret)
                LOGGER.debug("removing entry from documentCache : "+eldest.getKey());
            return ret;
        }
        public void setLoading(String key) {
            loading.add(key);
        }
        public void ignoreLoading(String key) {
            loading.remove(key);
        }
        public boolean isLoading(String key) {
            return loading.contains(key);
        }

        @Override
        public XdmNode put(String key, XdmNode value) {
            XdmNode ret =  super.put(key, value);
            loading.remove(key);
            return ret;
        }
        
        public XdmNode waitForLoading(String key) {
            long endDate = System.currentTimeMillis() + 2000;
            while(isLoading(key)) {
                try { Thread.sleep(100); } catch(Throwable t) {}
                if(System.currentTimeMillis()> endDate) {
                    return null;
                }
            }
            return get(key);
        }
        
    }
}

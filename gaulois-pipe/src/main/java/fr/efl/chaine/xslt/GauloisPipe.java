/**
 * This Source Code Form is subject to the terms of 
 * the Mozilla Public License, v. 2.0. If a copy of 
 * the MPL was not distributed with this file, You 
 * can obtain one at https://mozilla.org/MPL/2.0/.
 */
package fr.efl.chaine.xslt;

import fr.efl.chaine.xslt.utils.ParameterValue;
import fr.efl.chaine.xslt.config.CfgFile;
import fr.efl.chaine.xslt.config.ChooseStep;
import fr.efl.chaine.xslt.config.Config;
import fr.efl.chaine.xslt.config.ConfigUtil;
import fr.efl.chaine.xslt.config.JavaStep;
import fr.efl.chaine.xslt.config.Listener;
import fr.efl.chaine.xslt.config.Output;
import fr.efl.chaine.xslt.config.ParametrableStep;
import fr.efl.chaine.xslt.config.Pipe;
import fr.efl.chaine.xslt.config.Tee;
import fr.efl.chaine.xslt.config.WhenEntry;
import fr.efl.chaine.xslt.config.Xslt;
import fr.efl.chaine.xslt.listener.HttpListener;
import fr.efl.chaine.xslt.utils.ParametersMerger;
import fr.efl.chaine.xslt.utils.ParametrableFile;
import fr.efl.chaine.xslt.utils.TeeDebugDestination;
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
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.transform.stream.StreamResult;
import net.sf.saxon.Configuration;
import net.sf.saxon.event.ProxyReceiver;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.lib.StandardLogger;
import net.sf.saxon.trace.XSLTTraceListener;
import net.sf.saxon.trans.XPathException;
import org.apache.commons.io.output.NullOutputStream;
import org.xmlresolver.Resolver;
import top.marchand.xml.gaulois.impl.DefaultSaxonConfigurationFactory;
import top.marchand.xml.protocols.ProtocolInstaller;

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
    private Config config;
    
    private final Map<String,XsltExecutable> xslCache;
    
    private Processor processor;
    private final SaxonConfigurationFactory configurationFactory;
    
    /**
     * Message listener for XSLT-SAXON messages (xsl:message)
     */
    private Class<MessageListener> messageListenerclass;
    private MessageListener messageListener = null;
    private XSLTTraceListener traceListener = null;
    private DocumentCache documentCache;
    private XsltCompiler xsltCompiler;
    private DocumentBuilder builder = null;
    
    private URIResolver uriResolver;
    private static transient boolean protocolInstalled = false;
    
    private List<Exception> errors;
    private XPathCompiler xpathCompiler;
    private File debugDirectory;
    
    private String currentDir = System.getProperty("user.dir");

    /**
     * The property name to specify the debug output directory
     */
    public static final transient String GAULOIS_DEBUG_DIR_PROPERTY = "gaulois.debug.dir";

    
    /**
     * Constructs a new GauloisPipe.
     * This constructor is the main one, the other one is only for backward compatibility.
     * @param configurationFactory The configuration factory to use
     */
    @SuppressWarnings("OverridableMethodCallInConstructor")
    public GauloisPipe(final SaxonConfigurationFactory configurationFactory) {
        super();
        if(!protocolInstalled) {
            ProtocolInstaller.registerAdditionalProtocols();
            protocolInstalled = true;
        }
        this.configurationFactory = configurationFactory;
        Configuration saxonConfig=configurationFactory.getConfiguration();
        saxonConfig.setURIResolver(getUriResolver());
        xslCache = new HashMap<>();
    }

    /**
     * The constructor with detail config.
     * It shouldn't be used anymore.
     *
     * @param configurationFactory The Saxon's Configuration factory to use
     * @param inputs the input files
     * @param outputDirectory the output directory
     * @param templatePaths the template paths
     * @param nbThreads the nbThreads
     * @param instanceName The instance name to use in the logs
     * @throws fr.efl.chaine.xslt.InvalidSyntaxException If config's syntax is incorrect
     */
    public GauloisPipe(final SaxonConfigurationFactory configurationFactory, List<String> inputs, String outputDirectory,List<String> templatePaths, int nbThreads, String instanceName) throws InvalidSyntaxException {
        this(configurationFactory);
        this.instanceName=instanceName;
        Config cfg = new Config();

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
     * @throws fr.efl.chaine.xslt.InvalidSyntaxException If config's syntax is incorrect
     * @throws java.io.FileNotFoundException If a file is not found...
     * @throws net.sf.saxon.s9api.SaxonApiException If a SaxonApi problem occurs
     * @throws java.net.URISyntaxException Because MVN forces to have comments...
     */
    @SuppressWarnings("ThrowFromFinallyBlock")
    public void launch() throws InvalidSyntaxException, FileNotFoundException, SaxonApiException, URISyntaxException, IOException {
        initDebugDirectory();
        Runtime.getRuntime().addShutdownHook(new Thread(new ErrorCollector(errors)));
        long start = System.currentTimeMillis();
        errors = Collections.synchronizedList(new ArrayList<Exception>());
        documentCache = new DocumentCache(config.getMaxDocumentCacheSize());
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
            Configuration saxonConfig = configurationFactory.getConfiguration();
            LOGGER.debug("configuration is a "+saxonConfig.getClass().getName());
            // this is now done in constructor
            // saxonConfig.setURIResolver(buildUriResolver(saxonConfig.getURIResolver()));
            processor = new Processor(saxonConfig);
            xsltCompiler = processor.newXsltCompiler();
            builder = processor.newDocumentBuilder();

            List<CfgFile> sourceFiles = config.getSources().getFiles();
            LOGGER.info("[" + instanceName + "] works on {} files", sourceFiles.size());
            
            if(config.getPipe().getTraceOutput()!=null) {
                traceListener = buildTraceListener(config.getPipe().getTraceOutput());
            }

            if (config.getPipe().getNbThreads() > 1) {
                if (config.hasFilesOverMultiThreadLimit()) {
                    List<ParametrableFile> files = new ArrayList<>(sourceFiles.size());
                    for (CfgFile f : config.getSources().getFilesOverLimit(config.getPipe().getMultithreadMaxSourceSize())) {
                        files.add(resolveInputFile(f));
                    }
                    if (!files.isEmpty()) {
                        LOGGER.info("[" + instanceName + "] Running mono-thread for {} huge files", files.size());
                        retCode = executesPipeOnMultiThread(config.getPipe(), files, 1, config.getSources().getListener());
                    }
                }
                List<ParametrableFile> files = new ArrayList<>(sourceFiles.size());
                for (CfgFile f : config.getSources().getFilesUnderLimit(config.getPipe().getMultithreadMaxSourceSize())) {
                    files.add(resolveInputFile(f));
                }
                if (!files.isEmpty() || config.getSources().getListener()!=null) {
                    LOGGER.info("[" + instanceName + "] Running multi-thread for {} regular-size files", files.size());
                    retCode = executesPipeOnMultiThread(config.getPipe(), files, config.getPipe().getNbThreads(), config.getSources().getListener());
                }
            } else {
                List<ParametrableFile> files = new ArrayList<>(sourceFiles.size());
                for (CfgFile f : sourceFiles) {
                    files.add(resolveInputFile(f));
                }
                LOGGER.info("[" + instanceName + "] Running mono-thread on all {} files", files.size());
                retCode = executesPipeOnMultiThread(config.getPipe(), files, 1, config.getSources().getListener());
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
            if(config.getSources().getListener()==null) {
                long duration = System.currentTimeMillis() - start;
                Duration duree = DatatypeFactory.newInstance().newDuration(duration);
                LOGGER.info("[" + instanceName + "] Process terminated: "+duree.toString());
            }
        } catch(Exception ex) {
            LOGGER.info("[" + instanceName + "] Process terminated.");
        }
    }

    /**
     * Returns ...
     * @return the size of the document cache. Mainly used for UT
     */
    public int getDocumentCacheSize() {
        return documentCache.size();
    }
    /**
     * Returns...
     * @return the size of XSLT cache. Mainly used by UT
     */
    public int getXsltCacheSize() {
        return xslCache.size();
    }
    private ParametrableFile resolveInputFile(CfgFile file) {
        ParametrableFile ret = new ParametrableFile(file.getSource());
        ret.getParameters().putAll(file.getParams());
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
     * @param listener The listener to start, if not null
     * @return <tt>false</tt> if an error occurs while processing.
     */
    private boolean executesPipeOnMultiThread(
            final Pipe pipe,
            List<ParametrableFile> inputs,
            int nbThreads,
            Listener listener) {
        ExecutorService service = (nbThreads==1) ? 
                Executors.newSingleThreadExecutor() : 
                Executors.newFixedThreadPool(nbThreads);
        // a try to solve multi-thraed compiling problem...
        // that's a pretty dirty hack, but just a try, to test...
        if(xslCache.isEmpty() && !inputs.isEmpty()) {
            // in the opposite case, there is only a listener, and probably the first
            // file will be proccess alone...
            try {
                XsltTransformer transformer = buildTransformer(
                    pipe, 
                    inputs.get(0).getFile(), 
                    inputs.get(0).getFile().toURI().toURL().toExternalForm(), 
                    ParametersMerger.merge(inputs.get(0).getParameters(), config.getParams()),
                    messageListener,null);
            } catch(IOException | InvalidSyntaxException | URISyntaxException | SaxonApiException ex) {
                String msg = "while pre-compiling for a multi-thread use...";
                LOGGER.error(msg);
                errors.add(new GauloisRunException(msg, ex));
            }
        }
        for(ParametrableFile pf: inputs) {
            final ParametrableFile fpf = pf;
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    try {
                        execute(pipe, fpf, messageListener);
                    } catch(SaxonApiException | IOException | InvalidSyntaxException | URISyntaxException ex) {
                        String msg = "[" + instanceName + "] while processing "+fpf.getFile().getName();
                        LOGGER.error(msg, ex);
                        errors.add(new GauloisRunException(msg, fpf.getFile()));
                    }
                }
            };
            service.execute(r);
        }
        if(listener==null) {
            // on ajoute plus rien
            service.shutdown();
            try {
                service.awaitTermination(5, TimeUnit.HOURS);
                return true;
            } catch (InterruptedException ex) {
                LOGGER.error("[" + instanceName + "] multi-thread processing interrupted, 5 hour limit exceed.");
                return false;
            }
        } else {
            ExecutionContext context = new ExecutionContext(this, pipe, messageListener, service);
            final HttpListener httpListener = new HttpListener(listener.getPort(), listener.getStopKeyword(), context);
            Runnable runner = new Runnable() {
                @Override
                public void run() {
                    httpListener.run();
                }
            };
            new Thread(runner).start();
            return true;
        }
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
     * @Deprecated Do not use anymore, prefer <tt>executePipeOnMultiThread(Pipe, List&t;ParametrableFile&gt;,1,Listener)</tt>
     */
    @Deprecated
    private boolean executesPipeOnMonoThread(Pipe pipe, List<ParametrableFile> inputs) {
        boolean ret = true;
        for (ParametrableFile inputFile : inputs) {
            try {
                execute(pipe, inputFile, messageListener);
            } catch(SaxonApiException | InvalidSyntaxException | URISyntaxException | IOException ex) {
                LOGGER.error("[" + instanceName + "] while mono-thread processing of "+inputFile.getFile().getName(),ex);
                errors.add(new GauloisRunException(ex.getMessage(), inputFile.getFile()));
                ret = false;
            }
        }
        return ret;
    }
    
    public List<Exception> getErrors() {
        return errors;
    }

    /**
     * Execute the pipe for the specified input stream to the specified
     * serializer.<br>
     * <b>Warning</b>: this method is public for implementation reasons,
     * and <b>must not</b> be called outside of GauloisPipe
     *
     * @param pipe the pipe to run
     * @param input the specified input stream
     * @param listener the message listener to use
     * @throws SaxonApiException when a problem occurs
     * @throws java.net.MalformedURLException When an URL is not correctly formed
     * @throws fr.efl.chaine.xslt.InvalidSyntaxException When config file is invalid
     * @throws java.net.URISyntaxException When URI is invalid
     * @throws java.io.FileNotFoundException And when the file can not be found !
     */
    public void execute(Pipe pipe, ParametrableFile input, MessageListener listener)
            throws SaxonApiException, MalformedURLException, InvalidSyntaxException, URISyntaxException, FileNotFoundException, IOException {
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
                            LOGGER.debug("["+instanceName+"] caching "+key);
                            documentCache.put(key, source);
                        } else {
                            documentCache.ignoreLoading(key);
                            if(avoidCache){
                                LOGGER.trace("["+instanceName+"] "+key+" exclued from cache");
                            } else {
                                LOGGER.trace("["+instanceName+"] "+key+" used only once, no cache");
                            }
                        }
                    }
                }
            }
        }
        HashMap<String,ParameterValue> parameters = ParametersMerger.addInputInParameters(ParametersMerger.merge(input.getParameters(), config.getParams()),input.getFile());
        XsltTransformer transformer = buildTransformer(
                pipe, 
                input.getFile(), 
                input.getFile().toURI().toURL().toExternalForm(), 
                parameters,
                listener, source);
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
    
    private XsltTransformer buildTransformer(Pipe pipe, File inputFile, String inputFileUri, HashMap<String,ParameterValue> parameters, MessageListener listener, XdmNode documentTree, boolean... isFake) throws InvalidSyntaxException, URISyntaxException, MalformedURLException, SaxonApiException, FileNotFoundException, IOException {
        LOGGER.trace("in buildTransformer(Pipe,...)");
        XsltTransformer first = null;
        Iterator<ParametrableStep> it = pipe.getXslts();
        Object previousTransformer = null;
        while(it.hasNext()) {
            LOGGER.trace("...in buildTransformer.tee.while");
            ParametrableStep step = it.next();
            if(step instanceof Xslt) {
                Xslt xsl = (Xslt)step;
                XsltTransformer currentTransformer = getXsltTransformer(xsl.getHref(), parameters);
                Destination currentDestination = currentTransformer;
                if(xsl.isTraceToAdd()) {
                    currentTransformer.setTraceListener(traceListener);
                }
                if(listener!=null) {
                    LOGGER.trace(xsl.getHref()+" setting messageListener "+listener);
                    currentTransformer.setMessageListener(listener);
                }
                for(ParameterValue pv:xsl.getParams()) {
                    // on substitue les paramètres globaux dans ceux de la XSL
                    String value = ParametersMerger.processParametersReplacement(pv.getValue(), parameters);
                    LOGGER.trace("Setting parameter ("+pv.getKey()+","+value+")");
                    currentTransformer.setParameter(new QName(pv.getKey()), new XdmAtomicValue(value));
                }
                for(ParameterValue pv:parameters.values()) {
                    // substitution has been before, in merge, but there is input-file relative parameters...
                    currentTransformer.setParameter(new QName(pv.getKey()), new XdmAtomicValue(
                            ParametersMerger.processParametersReplacement(pv.getValue(), parameters)
                    ));
                }
                if(xsl.isDebug()) {
                    File debugFile;
                    if(debugDirectory!=null) {
                        debugFile = new File(debugDirectory, xsl.getId()+"-"+inputFile.getName());
                    } else {
                        debugFile = new File(xsl.getId()+"-"+inputFile.getName());
                    }
                    Serializer debug = processor.newSerializer(debugFile);
                    // here currentTransformer==currentDestination
                    currentTransformer.setDestination(currentDestination=new TeeDebugDestination(debug));
                }
                if(first==null) {
                    first = currentTransformer;
                }
                if(previousTransformer!=null) {
                    assignStepToDestination(previousTransformer, currentDestination);
                }
                previousTransformer = currentDestination;
                LOGGER.trace(xsl.getHref()+" constructed and added to pipe");
            } else if(step instanceof ChooseStep) {
                ChooseStep cStep = (ChooseStep)step;
                XPathCompiler xpc = getXPathCompiler();
                if(documentTree==null) {
                    // here, we are in a pre-compile xsl step, ignore the choose...
                } else {
                    for(WhenEntry when:cStep.getConditions()) {
                        XPathSelector select = xpc.compile(when.getTest()).load();
                        select.setContextItem(documentTree);
                        XdmValue result = select.evaluate();
                        if(result.size()!=1) {
                            throw new InvalidSyntaxException(when.getTest()+" does not produce a xs:boolean result");
                        }
                        if("true".equals(result.itemAt(0).getStringValue())) {
                            // use this WHEN !
                            // on ne peut pas faire ça, il n'y a pas de terminal step.
                            Pipe fakePipe = new Pipe();
                            for(ParametrableStep innerStep:when.getSteps()) {
                                fakePipe.addXslt(innerStep);
                            }
                            Destination currentDestination = buildTransformer(fakePipe, inputFile, inputFileUri, parameters, listener, documentTree, true);
                            if(previousTransformer!=null) {
                                assignStepToDestination(previousTransformer, currentDestination);
                            }
                            previousTransformer = currentDestination;
                            break;
                        }
                    }
                }
            } else if(step instanceof JavaStep) {
                JavaStep javaStep = (JavaStep)step;
                LOGGER.debug("creating "+javaStep.getStepClass().getName());
                try {
                    LOGGER.debug("[JAVA-STEP] Creating "+javaStep.getStepClass().getName());
                    StepJava stepJava = javaStep.getStepClass().newInstance();
                    for(ParameterValue pv:javaStep.getParams()) {
                        stepJava.setParameter(new QName(pv.getKey()), new XdmAtomicValue(
                                ParametersMerger.processParametersReplacement(pv.getValue(), parameters)
                        ));
                    }
                    for(ParameterValue pv:parameters.values()) {
                        // la substitution a été faite avant, dans le merge
                        stepJava.setParameter(new QName(pv.getKey()), new XdmAtomicValue(
                                ParametersMerger.processParametersReplacement(pv.getValue(), parameters)
                        ));
                    }
                    if(previousTransformer!=null) {
                        assignStepToDestination(previousTransformer, stepJava);
                    }
                    previousTransformer = stepJava;
                } catch (InstantiationException | IllegalAccessException ex) {
                    throw new InvalidSyntaxException(ex);
                }
            } else if(step instanceof Tee) {
                throw new InvalidSyntaxException("A tee can not be the root of a pipe");
            }
        }
        Destination nextStep = null;
        if(pipe.getTee()!=null) {
            LOGGER.trace("after having construct xslts, build tee");
            nextStep = buildTransformer(pipe.getTee(), inputFile, inputFileUri, parameters, listener, documentTree);
        } else if(pipe.getOutput()!=null) {
            LOGGER.trace("after having construct xslts, build output");
            nextStep = buildSerializer(pipe.getOutput(),inputFile,parameters);
        }
        if(nextStep!=null) {
            assignStepToDestination(previousTransformer, nextStep);
        } else if(isFake.length==0 || !isFake[0]) {
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
        } else if(assignee instanceof TeeDebugDestination) {
            ((TeeDebugDestination)assignee).setDestination(assigned);
        } else {
            throw new IllegalArgumentException("assignee must be either a XsltTransformer or a StepJava instance");
        }
    }
    
    private XsltTransformer getXsltTransformer(String href, HashMap<String,ParameterValue> parameters) throws MalformedURLException, SaxonApiException, URISyntaxException, FileNotFoundException, IOException {
        // TODO : rewrite this, as cp: and jar: protocols are availabe and one can use new URL(cp:/...).getInputStream()
        String __href = ParametersMerger.processParametersReplacement(href, parameters);
        LOGGER.debug("loading "+__href);
        XsltExecutable xsl = xslCache.get(__href);
        if(xsl==null) {
            LOGGER.trace(__href+" not in cache");
            try {
                InputStream input ;
                if(__href.startsWith("file:")) {
                    File f = new File(new URI(__href));
                    input = new FileInputStream(f);
                } else if(__href.startsWith("jar:")) {
                    input = new URL(__href).openStream();
                } else if(__href.startsWith("cp:")) {
                    input = GauloisPipe.class.getResourceAsStream(__href.substring(3));
                } else {
                    File f = new File(__href);
                    input = new FileInputStream(f);
                }
                LOGGER.trace("input is "+input);
                StreamSource source = new StreamSource(input);
                source.setSystemId(__href);

                xsl = xsltCompiler.compile(source);
                xslCache.put(__href, xsl);
            } catch(SaxonApiException ex) {
                LOGGER.error("while compiling "+__href);
                LOGGER.error("SaxonAPIException: "+href+": ["+ex.getErrorCode()+"]:"+ex.getMessage());
                if(ex.getCause()!=null) {
                    LOGGER.error(ex.getCause().getMessage());
                }
                throw ex;
            } catch(URISyntaxException|FileNotFoundException ex) {
                LOGGER.error("while compiling "+__href);
                throw ex;
            }
        }
        return xsl.load();
    }
    
    private Destination buildTransformer(Tee tee, File inputFile, String inputFileUri, HashMap<String,ParameterValue> parameters, MessageListener listener, XdmNode documentTree) throws InvalidSyntaxException, URISyntaxException, MalformedURLException, SaxonApiException, FileNotFoundException, IOException {
        LOGGER.trace("in buildTransformer(Tee,...)");
        List<Destination> dests = new ArrayList<>();
        if(tee==null) {
            throw new InvalidSyntaxException("tee est null !");
        }
        if(tee.getPipes()==null) {
            throw new InvalidSyntaxException("tee.getPipes() est null !");
        }
        for(Pipe pipe:tee.getPipes()) {
            dests.add(buildShortPipeTransformer(pipe, inputFile, inputFileUri, parameters, listener, documentTree));
        }
        while(dests.size()>1) {
            Destination d1 = dests.remove(0);
            Destination d2 = dests.remove(0);
            if(d1==d2) throw new IllegalArgumentException("d1 et d2 sont la meme destination");
            dests.add(new TeeDestination(d2, d1));
        }
        return dests.get(0);
    }
    private Destination buildShortPipeTransformer(Pipe pipe, File inputFile, String inputFileUri, HashMap<String,ParameterValue> parameters, MessageListener listener, XdmNode documentTree) throws InvalidSyntaxException, URISyntaxException, MalformedURLException, SaxonApiException, FileNotFoundException, IOException {
        if(!pipe.getXslts().hasNext()) {
            if(pipe.getOutput()!=null) {
                return buildSerializer(pipe.getOutput(),inputFile, parameters);
            } else {
                return buildTransformer(pipe.getTee(), inputFile, inputFileUri, parameters, listener, documentTree);
            }
        } else {
            return buildTransformer(pipe, inputFile, inputFileUri, parameters, listener, documentTree);
        }
    }
    
    private Destination buildSerializer(Output output, File inputFile, HashMap<String,ParameterValue> parameters) throws InvalidSyntaxException, URISyntaxException {
        if(output.isNullOutput()) {
            return processor.newSerializer(new NullOutputStream());
        } else if(output.isConsoleOutput()) {
            return processor.newSerializer("out".equals(output.getConsole())?System.out:System.err);
        }
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
        return new Resolver();
    }
    
    public void doPostCloseService(ExecutionContext context) {
        try {
            context.getService().awaitTermination(5, TimeUnit.HOURS);
            if(config.getSources().getListener().getJavastep()!=null) {
                JavaStep javaStep = config.getSources().getListener().getJavastep();
                LOGGER.debug("creating "+javaStep.getStepClass().getName());
                try {
                    StepJava stepJava = javaStep.getStepClass().newInstance();
                    for(ParameterValue pv:javaStep.getParams()) {
                        stepJava.setParameter(new QName(pv.getKey()), new XdmAtomicValue(pv.getValue()));
                    }
                    for(ParameterValue pv:config.getParams().values()) {
                        // la substitution a été faite avant, dans le merge
                        stepJava.setParameter(new QName(pv.getKey()), new XdmAtomicValue(pv.getValue()));
                    }
                    Receiver r = stepJava.getReceiver(configurationFactory.getConfiguration());
                    r.open();
                    r.close();
                } catch (XPathException | SaxonApiException | InstantiationException | IllegalAccessException ex) {
                    LOGGER.error("while preparing doPostCloseService",ex);
                }
            }
        } catch (InterruptedException ex) {
            LOGGER.error("[" + instanceName + "] multi-thread processing interrupted, 5 hour limit exceed.");
        }
    }


    /**
     * Main entry point of saxon xslt pipe.<br>
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
        if(!protocolInstalled) {
            ProtocolInstaller.registerAdditionalProtocols();
            protocolInstalled = true;
        }
        LOGGER.debug("Additionals protocols installed");
        GauloisPipe gauloisPipe = new GauloisPipe(new DefaultSaxonConfigurationFactory());
        try {
            LOGGER.debug("gauloisPipe instanciated");
            Config config = gauloisPipe.parseCommandLine(args);
            gauloisPipe.setConfig(config);
            gauloisPipe.setInstanceName(config.__instanceName);
        } catch (InvalidSyntaxException ex) {
            LOGGER.error(ex.getMessage(), ex);
            System.exit(1);
        }
        try {
            gauloisPipe.launch();
        } catch (InvalidSyntaxException | SaxonApiException | URISyntaxException | IOException ex) {
            LOGGER.error(ex.getMessage(), ex);
            gauloisPipe.getErrors().add(ex);
        }
    }

    /**
     * Set the MessageListener class to use.
     * The only way to set a message listener is to define its class. Gaulois-pipe
     * wille create an instance of this class, calling the default constructor.
     * This method must be called before the {@link #launch() } call.
     * @param messageListenerclass The class of the listener to use
     */
    public void setMessageListenerclass(Class messageListenerclass) {
        if(MessageListener.class.isAssignableFrom(messageListenerclass)) {
            this.messageListenerclass = messageListenerclass;
        }
    }
    
    
    @SuppressWarnings("LocalVariableHidesMemberVariable")
    public Config parseCommandLine(String[] args) throws InvalidSyntaxException {
        List<String> inputFiles = new ArrayList<>();
        List<String> inputParams = new ArrayList<>();
        List<String> inputXsls = new ArrayList<>();
        String nbThreads = null;
        String inputOutput = null;
        String _messageListener = null;
        String configFileName = null;
        String __instanceName = INSTANCE_DEFAULT_NAME;
        boolean logFileSize = false;
        boolean skipSchemaValidation = false;
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
                        continue;
                    case "--logFileSize":
                        logFileSize=true;
                    case "--skipSchemaValidation":
                        skipSchemaValidation=true;
                    case "--working-dir":
                    case "-wd":
                        inputMode = CURRENT_DIR;
                        continue;
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
                    _messageListener = argument ; break;
                case INSTANCE_NAME:
                    __instanceName = argument; break;
                case CONFIG: 
                    configFileName = argument; break;
                case CURRENT_DIR:
                    currentDir = argument; break;
            }
        }
        HashMap<String,ParameterValue> inputParameters = new HashMap<>(inputParams.size());
        for(String paramPattern:inputParams) {
            LOGGER.debug("parsing parameter "+paramPattern);
            ParameterValue pv = ConfigUtil.parseParameterPattern(paramPattern);
            inputParameters.put(pv.getKey(), pv);
        }
        LOGGER.debug("parameters from command line are : "+inputParameters);
        Config config;
        if(configFileName!=null) {
            LOGGER.debug("loading config file "+configFileName);
            config=parseConfig(configFileName, inputParameters, configurationFactory.getConfiguration(), skipSchemaValidation);
            LOGGER.debug("computed parameters in config are :"+config.getParams());
        } else {
            config = new Config(currentDir);
        }
        for(String inputFile: inputFiles) ConfigUtil.addInputFile(config, inputFile);
        for(String inputXsl: inputXsls) ConfigUtil.addTemplate(config, inputXsl);
        if(nbThreads!=null) ConfigUtil.setNbThreads(config, nbThreads);
        if(inputOutput!=null) ConfigUtil.setOutput(config, inputOutput);
        config.setLogFileSize(logFileSize);
        config.skipSchemaValidation(skipSchemaValidation);
        config.verify();
        LOGGER.debug("merged parameters into config are : "+config.getParams());
        config.__instanceName=__instanceName;
        if(_messageListener!=null) {
            try {
                Class clazz = Class.forName(_messageListener);
                messageListenerclass = clazz.asSubclass(MessageListener.class);
            } catch(ClassNotFoundException ex) {
                LOGGER.warn(_messageListener+" is not a "+MessageListener.class.getName());
            }
        }
        return config;
    }

    private Config parseConfig(String fileName, HashMap<String,ParameterValue> inputParameters, Configuration saxonConfig, final boolean skipSchemaValidation) throws InvalidSyntaxException {
        try {
            return new ConfigUtil(saxonConfig, getUriResolver(), fileName, skipSchemaValidation, currentDir).buildConfig(inputParameters);
        } catch (SaxonApiException ex) {
            throw new InvalidSyntaxException(ex);
        }
    }

    public URIResolver getUriResolver() {
        if(uriResolver==null) {
            uriResolver = buildUriResolver(configurationFactory.getConfiguration().getURIResolver());
        }
        return uriResolver;
    }

    private static final transient String USAGE_PROMPT = "\nUSAGE:\njava " + GauloisPipe.class.getName() + "\n"
            + "\t--config config.xml\tthe config file to use\n"
            + "\t--msg-listener package.of.MessageListener The class to use as MessageListener\n"
            + "\t{--instance-name | -iName} <name>\t\tthe instance name to use in logs\n"
            + "\t{--working-dir | -wd} <cwd>\t\t\tThe director to use as current directory ; ${user.dir} if missing\n"
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
    private static final int CURRENT_DIR = 0x40;
    
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
        
        @SuppressWarnings("SleepWhileInLoop")
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
    
    private class ErrorCollector implements Runnable {
        private final List<Exception> errorsContainer;
        public ErrorCollector(List<Exception> errorsContainer) {
            super();
            this.errorsContainer=errorsContainer;
        }
        @Override
        public void run() {
            if(errorsContainer==null || errorsContainer.isEmpty()) {
                GauloisPipe.LOGGER.info("Gaulois-Pipe is exiting without error");
            } else {
                for(Exception ex:errorsContainer) {
                    GauloisPipe.LOGGER.error("",ex);
                }
                GauloisPipe.LOGGER.info("Gaulois-Pipe is exiting with error");
                Runtime.getRuntime().halt(errorsContainer.size());
            }
        }
    }
    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    public String getInstanceName() {
        return instanceName;
    }
    
    private XSLTTraceListener buildTraceListener(final String outputDest) {
        if(outputDest==null) return null;
        net.sf.saxon.lib.Logger logger;
        switch (outputDest) {
            case "#default":
                logger = configurationFactory.getConfiguration().getLogger();
                break;
            case "#logger":
                logger = new net.sf.saxon.lib.Logger() {
                    @Override
                    public void println(String string, int i) {
                        switch(i) {
                            case net.sf.saxon.lib.Logger.INFO: LOGGER.debug(string);
                            case net.sf.saxon.lib.Logger.WARNING: LOGGER.info(string);
                            case net.sf.saxon.lib.Logger.ERROR: LOGGER.warn(string);
                            case net.sf.saxon.lib.Logger.DISASTER: LOGGER.error(string);
                        }
                    }
                    @Override
                    public StreamResult asStreamResult() {
                        // TODO : make this cleaner !
                        return new StreamResult(System.out);
                    }
                };  break;
            default:
                try {
                    logger = new StandardLogger(new File(outputDest));
                } catch(FileNotFoundException ex) {
                    LOGGER.error("while creating traceListener output. Traces will be logged to standard output",ex);
                    logger = configurationFactory.getConfiguration().getLogger();
            }   break;
        }
        XSLTTraceListener tracer = new XSLTTraceListener();
        tracer.setOutputDestination(logger);
        return tracer;
    }
    private XPathCompiler getXPathCompiler() {
        if(xpathCompiler==null) {
            xpathCompiler = processor.newXPathCompiler();
            for(String prefix:config.getNamespaces().getMappings().keySet()) {
                xpathCompiler.declareNamespace(prefix, config.getNamespaces().getMappings().get(prefix));
            }
        }
        return xpathCompiler;
    }
    private void initDebugDirectory() {
        String property = System.getProperty(GAULOIS_DEBUG_DIR_PROPERTY);
        if(property!=null) {
            File directory = new File(property);
            if(!directory.exists()) {
                directory.mkdirs();
            }
            if(directory.exists() && directory.isDirectory()) {
                debugDirectory = directory;
            }
        }
    }
}

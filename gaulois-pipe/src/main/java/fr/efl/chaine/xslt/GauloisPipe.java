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
import fr.efl.chaine.xslt.utils.DoubleDestination;
import fr.efl.chaine.xslt.utils.ParametersMerger;
import fr.efl.chaine.xslt.utils.ParametrableFile;
import fr.efl.chaine.xslt.utils.TeeDebugDestination;
import net.sf.saxon.s9api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.transform.URIResolver;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.io.OutputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.Duration;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import net.sf.saxon.Configuration;
import net.sf.saxon.event.ProxyReceiver;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.lib.FeatureKeys;
import net.sf.saxon.lib.StandardErrorListener;
import net.sf.saxon.lib.StandardLogger;
import net.sf.saxon.trace.XSLTTraceListener;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.SchemaException;
import net.sf.saxon.type.ValidationException;
import org.apache.commons.io.output.NullOutputStream;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.EntityResolver2;
import org.xmlresolver.Resolver;
import top.marchand.xml.gaulois.config.typing.DatatypeFactory;
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
    
    private XPathCompiler xpathCompiler;
    private File debugDirectory;
    
    private String currentDir = System.getProperty("user.dir");
    private String currentDirUri;
    private GPErrorListener errorListener;
    
    private DatatypeFactory datatypeFactory;
    
    private SAXParserFactory saxParserFactory;
    private BlockingQueue<XMLReader> readers;
            

    /**
     * The property name to specify the debug output directory
     */
    public static final transient String GAULOIS_DEBUG_DIR_PROPERTY = "gaulois.debug.dir";
    private ThreadFactory threadFactory;
    
    private List<String> errors;
    private javax.xml.datatype.DatatypeFactory xmlDatatypeFactory;

    
    /**
     * Constructs a new GauloisPipe.
     * This constructor is the main one, the other one is only for backward compatibility.
     * @param configurationFactory The configuration factory to use
     * @throws fr.efl.chaine.xslt.InvalidSyntaxException If a problem in configuration exists
     */
    @SuppressWarnings("OverridableMethodCallInConstructor")
    public GauloisPipe(final SaxonConfigurationFactory configurationFactory) throws InvalidSyntaxException {
        super();
        if(!protocolInstalled) {
            ProtocolInstaller.registerAdditionalProtocols();
            protocolInstalled = true;
        }
        this.configurationFactory = configurationFactory;
        Configuration saxonConfig=configurationFactory.getConfiguration();
        saxonConfig.setURIResolver(getUriResolver());
        // issue #30
        if(getEntityResolver() != null) {
            saxonConfig.setConfigurationProperty(FeatureKeys.ENTITY_RESOLVER_CLASS, getEntityResolver().getClass().getName());
        }
        xslCache = new HashMap<>();
        try {
            datatypeFactory = DatatypeFactory.getInstance(saxonConfig);
            xmlDatatypeFactory = javax.xml.datatype.DatatypeFactory.newInstance();
        } catch(ValidationException | DatatypeConfigurationException ex) {
            throw new InvalidSyntaxException(ex);
        }
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
            ConfigUtil.addInputFile(cfg, input, datatypeFactory);
        }
        ConfigUtil.setOutput(cfg, outputDirectory);
        for (String templatePath : templatePaths) {
            ConfigUtil.addTemplate(cfg, templatePath, datatypeFactory);
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
     * @throws javax.xml.parsers.ParserConfigurationException If a SAXParser configuration issue is thrown
     * @throws org.xml.sax.SAXException If a SAXException is thrown
     */
    @SuppressWarnings("ThrowFromFinallyBlock")
    public void launch() throws 
            InvalidSyntaxException, 
            FileNotFoundException, 
            SaxonApiException, 
            URISyntaxException, 
            IOException, 
            ParserConfigurationException, 
            SAXException {
        initDebugDirectory();
        saxParserFactory = SAXParserFactory.newInstance();
        saxParserFactory.setNamespaceAware(true);
        readers = new ArrayBlockingQueue<>(5);
        while(readers.remainingCapacity()>0) {
            XMLReader reader = saxParserFactory.newSAXParser().getXMLReader();
            reader.setEntityResolver(getEntityResolver());
            readers.add(reader);
        }
        errors = Collections.synchronizedList(new ArrayList<String>());
        errorListener = new GPErrorListener(errors);
        long start = System.currentTimeMillis();
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
            // issue #40 : load schemas
            for(URL url: config.getSchemaLocations()) {
                try {
                    saxonConfig.addSchemaSource(new StreamSource(url.toExternalForm()));
                } catch(SchemaException ex) {
                    // we do not throw Exception, if no XSL requires schemas, pipe will work.
                    // but print ERRORS in output
                    errorListener.error(new TransformerException("unable to load schema "+url.toExternalForm(), ex));
                }
            }
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
                terminateErrorCollector();
                throw new SaxonApiException("An error occurs. See previous logs.");
            }
        }
        try {
            if(config.getSources().getListener()==null) {
                long duration = System.currentTimeMillis() - start;
                Duration duree = javax.xml.datatype.DatatypeFactory.newInstance().newDuration(duration);
                LOGGER.info("[" + instanceName + "] Process terminated: "+duree.toString());
                terminateErrorCollector();
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
                Executors.newSingleThreadExecutor(getThreadFactory()): 
                Executors.newFixedThreadPool(nbThreads, getThreadFactory());
        // a try to solve multi-thread compiling problem...
        // that's a pretty dirty hack, but just a try, to test...
        if(xslCache.isEmpty() && !inputs.isEmpty()) {
            // in the opposite case, there is only a listener, and probably the first
            // file will be proccess alone...
            try {
                buildTransformer(
                    pipe, 
                    inputs.get(0).getFile(), 
                    inputs.get(0).getFile().toURI().toURL().toExternalForm(), 
                    ParametersMerger.merge(inputs.get(0).getParameters(), config.getParams()),
                    messageListener,null);
            } catch(IOException | InvalidSyntaxException | URISyntaxException | SaxonApiException ex) {
                String msg = "while pre-compiling for a multi-thread use...";
                LOGGER.error(msg);
                collectError(new GauloisRunException(msg, ex));
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
                        collectError(new GauloisRunException(msg, ex, fpf.getFile()));
                    }
                }
            };
            LOGGER.debug("["+instanceName+"] submitting "+fpf.getFile().getName());
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
                collectError(new GauloisRunException(ex.getMessage(), ex, inputFile.getFile()));
                ret = false;
            }
        }
        return ret;
    }
    
    public List<String> getErrors() {
        return errors;
    }
    
    /**
     * Returns the config the gaulois-pipe is configured to process.
     * Pipe is inside the config.
     * @return The actual config.
     */
    public Config getConfig() {
        return config;
    }

    /**
     * Execute the pipe for the specified input stream to the specified
     * serializer.<br>
     * <b>Warning</b>: this method is public, but should not be called outside of
     * gaulois-pipe, as it doesn't use thread pool. Really be carfull if you need
     * to use it.
     * 
     * If <tt>outputs</tt> is provided, it must be a single Map, that contains
     * OutputStream where to redirect output to. The map's key entries are the
     * <tt>//output/@id</tt> defined in config file. Only <tt>output</tt> that have
     * an <tt>@id</tt> may be redirected to an existing OutputStream.
     * 
     *
     * @param pipe the pipe to run
     * @param input the specified input stream
     * @param listener the message listener to use
     * @param outputs An optional map that contains OutputStream to bind to cfg:output elements.
     * @throws SaxonApiException when a problem occurs
     * @throws java.net.MalformedURLException When an URL is not correctly formed
     * @throws fr.efl.chaine.xslt.InvalidSyntaxException When config file is invalid
     * @throws java.net.URISyntaxException When URI is invalid
     * @throws java.io.FileNotFoundException And when the file can not be found !
     */
    public void execute(Pipe pipe, ParametrableFile input, MessageListener listener, Map<String,OutputStream> ... outputs)
            throws SaxonApiException, MalformedURLException, InvalidSyntaxException, URISyntaxException, FileNotFoundException, IOException {
        if(outputs.length>1) {
            throw new InvalidSyntaxException("Only one outputs map is allowed.");
        }
        // sets the EntityResolver in a ThreadLocal, to be used by generated XMLReaders
        // See top.marchand.xml.gaulois.resolve.GauloisSAXParserFactory
        ThreadLocal<EntityResolver2> th = new ThreadLocal<>();
        th.set(getEntityResolver());
        boolean avoidCache = input.getAvoidCache();
        long start = System.currentTimeMillis();
        String key = input.getFile().getAbsolutePath().intern();
        LOGGER.debug("["+instanceName+"] starting execute on "+key);
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
                        // issue #39
                        XMLReader xmlReader = null;
                        try {
                            try {
                                xmlReader = readers.take();
                                SAXSource saxSource = new SAXSource(xmlReader, new InputSource(new FileInputStream(input.getFile())));
                                saxSource.setSystemId(input.getFile().toURI().toString());
                                source = builder.build(saxSource);
                            } finally {
                                readers.put(xmlReader);
                            }
                        } catch(InterruptedException ex) {
                            LOGGER.error("Problem with XMLReader pool");
                            throw new SaxonApiException("Problem with XMLReader pool. This has nothing to do with Saxon", ex);
                        }
                        // end issue #39
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
        HashMap<QName,ParameterValue> parameters = ParametersMerger.addInputInParameters(ParametersMerger.merge(input.getParameters(), config.getParams()),input.getFile(), datatypeFactory);
        DoubleDestination dd = buildTransformer(
                pipe, 
                input.getFile(), 
                input.getFile().toURI().toURL().toExternalForm(), 
                parameters,
                listener, source, false, outputs.length>0 ? outputs[0] : null);
        LOGGER.debug("["+instanceName+"] transformer build");
        XsltTransformer t = (XsltTransformer)(dd.getStart());
        t.setInitialContextNode(source);
        t.transform();
        long duration = System.currentTimeMillis() - start;
        String distinctName = input.toString();
        try {
            Duration duree = xmlDatatypeFactory.newDuration(duration);
            LOGGER.info("["+instanceName+"] - "+distinctName+" - transform terminated: "+duree.toString());
        } catch(Exception ex) {
            LOGGER.info("["+instanceName+"] - "+distinctName+" - transform terminated");
        }
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
     * @param isFake An optional parameter to indicate that we build a pipe not to run it, only to pre-compile XSLs
     * @throws SaxonApiException when a problem occurs
     * @throws java.net.MalformedURLException When an URL is not correctly formed
     * @throws fr.efl.chaine.xslt.InvalidSyntaxException When config file is invalid
     * @throws java.net.URISyntaxException When URI is invalid
     * @throws java.io.FileNotFoundException And when the file can not be found !
     */
    private DoubleDestination buildTransformer(Pipe pipe, File inputFile, String inputFileUri, HashMap<QName,ParameterValue> parameters, MessageListener listener, XdmNode documentTree, boolean... isFake) 
            throws InvalidSyntaxException, URISyntaxException, MalformedURLException, SaxonApiException, FileNotFoundException, IOException {
        return buildTransformer(pipe, inputFile, inputFileUri, parameters, listener, documentTree, false, new HashMap<String,OutputStream>());
    }
    private DoubleDestination buildTransformer(Pipe pipe, File inputFile, String inputFileUri, HashMap<QName,ParameterValue> parameters, MessageListener listener, XdmNode documentTree, boolean isFake, Map<String, OutputStream> ... outputs) 
            throws InvalidSyntaxException, URISyntaxException, MalformedURLException, SaxonApiException, FileNotFoundException, IOException {
        LOGGER.trace("in buildTransformer(Pipe,...)");
        XsltTransformer first = null;
        Iterator<ParametrableStep> it = pipe.getXslts();
        Object previousTransformer = null;
        Configuration _config = null;
        _config = configurationFactory.getConfiguration();
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
                    if(pv.getValue() instanceof String) {
                        String value = (String)ParametersMerger.processParametersReplacement(pv.getValue(), parameters);
                        LOGGER.trace("Setting parameter ("+pv.getKey()+","+value+")");
                        currentTransformer.setParameter(pv.getKey(), new XdmAtomicValue(value));
                    } else if(pv.getValue() instanceof XdmAtomicValue) {
                        String sValue = ((XdmAtomicValue)pv.getValue()).getStringValue();
                        sValue = (String)ParametersMerger.processParametersReplacement(sValue, parameters);
                        XdmValue newValue;
                        try {
                            newValue = pv.getDatatype().convert(sValue, _config);
                            currentTransformer.setParameter(pv.getKey(), newValue);
                        } catch (ValidationException ex) {
                            throw new SaxonApiException(ex);
                        }
                    } else if(pv.getValue() instanceof XdmValue){
                        currentTransformer.setParameter(pv.getKey(), (XdmValue)pv.getValue());
                    }
                }
                for(ParameterValue pv:parameters.values()) {
                    // substitution has been before, in merge, but there is input-file relative parameters...
                    if(pv.getValue() instanceof String) {
                        currentTransformer.setParameter(pv.getKey(), new XdmAtomicValue(
                                (String)ParametersMerger.processParametersReplacement(pv.getValue(), parameters)
                        ));
                    } else if(pv.getValue() instanceof XdmAtomicValue) {
                        String sValue = ((XdmAtomicValue)pv.getValue()).getStringValue();
                        sValue = (String)ParametersMerger.processParametersReplacement(sValue, parameters);
                        XdmValue newValue;
                        try {
                            newValue = pv.getDatatype().convert(sValue, _config);
                            currentTransformer.setParameter(pv.getKey(), newValue);
                        } catch (ValidationException ex) {
                            throw new SaxonApiException(ex);
                        }
                    } else {
                        currentTransformer.setParameter(pv.getKey(), (XdmValue)pv.getValue());
                    }
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
                    boolean whenEntrySelected=false;
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
                            DoubleDestination dd = buildTransformer(fakePipe, inputFile, inputFileUri, parameters, listener, documentTree, true, new HashMap<String,OutputStream>());
                            if(previousTransformer!=null) {
                                assignStepToDestination(previousTransformer, (Destination)(dd.getStart()));
                            }
                            // issue #31 : we have to found the last restination of this pipe
                            // TODO: correct problem
                            previousTransformer = dd.getEnd();
                            whenEntrySelected = true;
                            break;
                        }
                    }
                    if(!whenEntrySelected) {
                        throw new InvalidSyntaxException("no when or otherwise selected for "+inputFileUri);
                    }
                }
            } else if(step instanceof JavaStep) {
                JavaStep javaStep = (JavaStep)step;
                LOGGER.debug("creating "+javaStep.getStepClass().getName());
                try {
                    LOGGER.debug("[JAVA-STEP] Creating "+javaStep.getStepClass().getName());
                    StepJava stepJava = javaStep.getStepClass().newInstance();
                    for(ParameterValue pv:javaStep.getParams()) {
                        if(pv.getValue() instanceof String) {
                            stepJava.setParameter(pv.getKey(), new XdmAtomicValue(
                                    (String)ParametersMerger.processParametersReplacement(pv.getValue(), parameters)
                            ));
                        } else if(pv.getValue() instanceof XdmAtomicValue) {
                            // TODO : this code should be moved to ParametersMerger
                            String sValue = ((XdmAtomicValue)pv.getValue()).getStringValue();
                            sValue = (String)ParametersMerger.processParametersReplacement(sValue, parameters);
                            XdmValue newValue;
                            try {
                                newValue = pv.getDatatype().convert(sValue, _config);
                                stepJava.setParameter(pv.getKey(), newValue);
                            } catch (ValidationException ex) {
                                throw new SaxonApiException(ex);
                            }
                        } else if(pv.getValue() instanceof XdmValue) {
                            stepJava.setParameter(pv.getKey(), (XdmValue)pv.getValue());
                        }
                    }
                    for(ParameterValue pv:parameters.values()) {
                        // la substitution a été faite avant, dans le merge
                        if(pv.getValue() instanceof String) {
                            stepJava.setParameter(pv.getKey(), new XdmAtomicValue(
                                    (String)ParametersMerger.processParametersReplacement(pv.getValue(), parameters)
                            ));
                        } else if(pv.getValue() instanceof XdmAtomicValue) {
                            // TODO : this code should be moved to ParametersMerger
                            String sValue = ((XdmAtomicValue)pv.getValue()).getStringValue();
                            sValue = (String)ParametersMerger.processParametersReplacement(sValue, parameters);
                            XdmValue newValue;
                            try {
                                newValue = pv.getDatatype().convert(sValue, _config);
                                stepJava.setParameter(pv.getKey(), newValue);
                            } catch (ValidationException ex) {
                                throw new SaxonApiException(ex);
                            }
                        } else {
                            stepJava.setParameter(pv.getKey(), (XdmValue)pv.getValue());
                        }
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
        DoubleDestination nextStep = null;
        if(pipe.getTee()!=null) {
            LOGGER.trace("after having construct xslts, build tee");
            nextStep = buildTransformer(pipe.getTee(), inputFile, inputFileUri, parameters, listener, documentTree, outputs[0]);
        } else if(pipe.getOutput()!=null) {
            LOGGER.trace("after having construct xslts, build output");
            nextStep = buildSerializer(pipe.getOutput(),inputFile,parameters, outputs[0]);
        }
        if(nextStep!=null) {
            assignStepToDestination(previousTransformer, (Destination)(nextStep.getStart()));
        } else if(!isFake) {
            throw new InvalidSyntaxException("Pipe "+pipe.toString()+" has no terminal Step.");
        }
        return new DoubleDestination(first, (Destination)previousTransformer);
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
    
    private XsltTransformer getXsltTransformer(String href, HashMap<QName,ParameterValue> parameters) 
            throws MalformedURLException, SaxonApiException, URISyntaxException, FileNotFoundException, IOException {
        String __href = (String)ParametersMerger.processParametersReplacement(href, parameters);
        LOGGER.debug("loading "+__href);
        XsltExecutable xsl = xslCache.get(__href);
        Source xslSource = null;
        Exception sourceEx = null;
        try {
            xslSource = getUriResolver().resolve(href, getCurrentDirUri());
        } catch(TransformerException tEx) {
            sourceEx = tEx;
        }
        if(xsl==null) {
            LOGGER.trace(__href+" not in cache");
            try {
                if(xslSource==null) {
                    throw new FileNotFoundException("Unable to resolve "+href);
                }
                if(sourceEx!=null) throw sourceEx;
                xsl = xsltCompiler.compile(xslSource);
                xslCache.put(__href, xsl);
            } catch(SaxonApiException ex) {
                LOGGER.error("while compiling "+__href);
                LOGGER.error("SaxonAPIException: "+href+": ["+ex.getErrorCode()+"]:"+ex.getMessage());
                if(ex.getCause()!=null) {
                    LOGGER.error(ex.getCause().getMessage());
                }
                throw ex;
            } catch(TransformerException ex) {
                LOGGER.error("while compiling "+__href);
                throw new SaxonApiException(ex);
            } catch(FileNotFoundException ex) {
                LOGGER.error("while compiling "+__href);
                throw ex;
            } catch(Exception ex) {
                // implementation requirement, but we already have catch all throwable exceptions
            }
        }
        XsltTransformer ret = xsl.load();
        try {
            
            ret.setParameter(
                    ParametersMerger.GP_STATIC_BASE_URI, 
                    datatypeFactory.getDatatype(new QName(DatatypeFactory.NS_XSD, "anyURI")).convert(xslSource.getSystemId(), configurationFactory.getConfiguration()));
        } catch(ValidationException ex) {
            LOGGER.error("while setting gp:static-base-uri parameter", ex);
        }
        ret.setErrorListener(errorListener);
        return ret;
    }
    
    private DoubleDestination buildTransformer(Tee tee, File inputFile, String inputFileUri, HashMap<QName,ParameterValue> parameters, MessageListener listener, XdmNode documentTree, Map<String, OutputStream> outputs) throws InvalidSyntaxException, URISyntaxException, MalformedURLException, SaxonApiException, FileNotFoundException, IOException {
        LOGGER.trace("in buildTransformer(Tee,...)");
        List<Destination> dests = new ArrayList<>();
        if(tee==null) {
            throw new InvalidSyntaxException("tee est null !");
        }
        if(tee.getPipes()==null) {
            throw new InvalidSyntaxException("tee.getPipes() est null !");
        }
        for(Pipe pipe:tee.getPipes()) {
            dests.add(
                    (Destination)(buildShortPipeTransformer(pipe, inputFile, inputFileUri, parameters, listener, documentTree, outputs).getStart())
            );
        }
        while(dests.size()>1) {
            Destination d1 = dests.remove(0);
            Destination d2 = dests.remove(0);
            if(d1==d2) throw new IllegalArgumentException("d1 et d2 sont la meme destination");
            dests.add(new TeeDestination(d2, d1));
        }
        return new DoubleDestination(dests.get(0), dests.get(dests.size()-1));
    }
    private DoubleDestination buildShortPipeTransformer(Pipe pipe, File inputFile, String inputFileUri, HashMap<QName,ParameterValue> parameters, MessageListener listener, XdmNode documentTree, Map<String, OutputStream> outputs) throws InvalidSyntaxException, URISyntaxException, MalformedURLException, SaxonApiException, FileNotFoundException, IOException {
        if(!pipe.getXslts().hasNext()) {
            if(pipe.getOutput()!=null) {
                return buildSerializer(pipe.getOutput(),inputFile, parameters, outputs);
            } else {
                return buildTransformer(pipe.getTee(), inputFile, inputFileUri, parameters, listener, documentTree, outputs);
            }
        } else {
            return buildTransformer(pipe, inputFile, inputFileUri, parameters, listener, documentTree);
        }
    }
    
    private DoubleDestination buildSerializer(Output output, File inputFile, HashMap<QName,ParameterValue> parameters, Map<String,OutputStream> outputs) throws InvalidSyntaxException, URISyntaxException {
        if(output.isNullOutput()) {
            Destination s = processor.newSerializer(new NullOutputStream());
            return new DoubleDestination(s, s);
        } else if(output.isConsoleOutput()) {
            Destination s = processor.newSerializer("out".equals(output.getConsole())?System.out:System.err);
            return new DoubleDestination(s, s);
        }
        final File destinationFile = output.getDestinationFile(inputFile, parameters);
        final Serializer ret;
        if(output.getId()!=null && outputs!=null && outputs.get(output.getId())!=null) {
            ret = processor.newSerializer(outputs.get(output.getId()));
        } else {
            ret = processor.newSerializer(destinationFile);
        }
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
            return new DoubleDestination(dest, dest);
        } else {
            return new DoubleDestination(ret, ret);
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
                        if(pv.getValue() instanceof String) {
                            stepJava.setParameter(pv.getKey(), new XdmAtomicValue(pv.getValue().toString()));
                        } else if(pv.getValue() instanceof XdmValue) {
                            stepJava.setParameter(pv.getKey(), (XdmValue)pv.getValue());
                        }
                    }
                    for(ParameterValue pv:config.getParams().values()) {
                        // la substitution a été faite avant, dans le merge
                        if(pv.getValue() instanceof String) {
                            stepJava.setParameter(pv.getKey(), new XdmAtomicValue(pv.getValue().toString()));
                        } else {
                            stepJava.setParameter(pv.getKey(), (XdmValue)pv.getValue());
                        }
                    }
                    Receiver r = stepJava.getReceiver(configurationFactory.getConfiguration());
                    r.open();
                    r.close();
                } catch (XPathException | SaxonApiException | InstantiationException | IllegalAccessException ex) {
                    LOGGER.error("while preparing doPostCloseService",ex);
                }
            }
            terminateErrorCollector();
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
     * <li><tt>GauloisPipe -o path/to/out -n 4 PARAMS p1=xxx p2=yyy XSL
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
        GauloisPipe gauloisPipe=null;
        boolean hasListener = false;
        try {
            gauloisPipe = new GauloisPipe(new DefaultSaxonConfigurationFactory());
            LOGGER.debug("gauloisPipe instanciated");
            Config config = gauloisPipe.parseCommandLine(args);
            gauloisPipe.setConfig(config);
            gauloisPipe.setInstanceName(config.__instanceName);
            hasListener = config.getSources().getListener()!=null;
        } catch (InvalidSyntaxException ex) {
            LOGGER.error(ex.getMessage(), ex);
            System.exit(1);
        }
        try {
            gauloisPipe.launch();
        } catch (InvalidSyntaxException | SaxonApiException | URISyntaxException | IOException | SAXException | ParserConfigurationException ex) {
            LOGGER.error(ex.getMessage(), ex);
            gauloisPipe.collectError(ex);
        } finally {
            if(!hasListener) {
                System.exit(gauloisPipe.terminateErrorCollector());
            }
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
                        continue;
                    case "--skipSchemaValidation":
                        skipSchemaValidation=true;
                        continue;
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
        HashMap<QName,ParameterValue> inputParameters = new HashMap<>(inputParams.size());
        for(String paramPattern:inputParams) {
            LOGGER.debug("parsing parameter "+paramPattern);
            ParameterValue pv = ConfigUtil.parseParameterPattern(paramPattern, datatypeFactory);
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
        for(String inputFile: inputFiles) ConfigUtil.addInputFile(config, inputFile, datatypeFactory);
        for(String inputXsl: inputXsls) ConfigUtil.addTemplate(config, inputXsl, datatypeFactory);
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

    private Config parseConfig(String fileName, HashMap<QName,ParameterValue> inputParameters, Configuration saxonConfig, final boolean skipSchemaValidation) throws InvalidSyntaxException {
        try {
            return new ConfigUtil(saxonConfig, getUriResolver(), fileName, skipSchemaValidation, currentDir).buildConfig(inputParameters);
        } catch (SaxonApiException ex) {
            throw new InvalidSyntaxException(ex);
        }
    }

    /**
     * Returns the entity resolver to use.
     * In default implementation, returns an instance of <tt>org.xmlresolver.Resolver</tt>
     * @see #getEntityResolver() 
     * @return The URI resolver to use
     */
    public URIResolver getUriResolver() {
        if(uriResolver==null) {
            uriResolver = buildUriResolver(configurationFactory.getConfiguration().getURIResolver());
        }
        return uriResolver;
    }
    /**
     * Returns the Entity resolver to use.
     * Default implementation returns {@link #getUriResolver() }, which returns a <tt>org.xmlresolver.Resolver</tt>,
     * which is also an EntityResolver. If you override {@link #getUriResolver() }, be careful, you may need to override
     * this method too.
     * @return The entity resolver to use
     */
    public EntityResolver2 getEntityResolver() {
        return (EntityResolver2)getUriResolver();
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
//            + "\tIt is impossible to override via command-line option something defined in config file, but it is possible to add inputs or templates.\n"
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
    
    private class GPErrorListener implements ErrorListener {
        private final List<String> errors;
        public final StandardErrorListener sel;
        protected GPErrorListener(List<String> errors) {
            super();
            this.errors=errors;
            sel = new StandardErrorListener();
        }
        @Override
        public void warning(TransformerException exception) throws TransformerException {
            LOGGER.warn(exception.getMessage());
        }
        @Override
        public void error(TransformerException exception) throws TransformerException {
            // TODO : check if errors throw exceptions. Assume it does, so do not report anything here
            //collectError(exception);
            //collectError(exception, "ERROR", sel.getExpandedMessage(exception), sel.getLocationMessage(exception));
            //errors.add("ERROR\n"+ sel.getExpandedMessage(exception)+"\n"+sel.getLocationMessage(exception));
        }
        @Override
        public void fatalError(TransformerException exception) throws TransformerException {
            // fatal errors throw exception, so it is not reported here
            //errors.add("FATAL ERROR\n"+ sel.getExpandedMessage(exception)+"\n"+sel.getLocationMessage(exception));
        }
    }
    
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
    
//    private class ErrorCollector implements Runnable {
//        private final List<String> errorsContainer;
//        public ErrorCollector(List<String> errorsContainer) {
//            super();
//            this.errorsContainer=errorsContainer;
//        }
//        @Override
//        public void run() {
//            if(errorsContainer==null || errorsContainer.isEmpty()) {
//                GauloisPipe.LOGGER.info("Gaulois-Pipe is exiting without error");
//            } else {
//                for(String error: errorsContainer) {
//                    GauloisPipe.LOGGER.error(error);
//                }
//                GauloisPipe.LOGGER.info("Gaulois-Pipe is exiting with error");
//                Runtime.getRuntime().halt(errorsContainer.size());
//            }
//        }
//    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    public String getInstanceName() {
        return instanceName;
    }
    /**
     * Sets the ThreadFactory to be used by executors. If not defined, {@link Executors#defaultThreadFactory() } is used.
     * <b>Warning</b>: if you define your own ThreadFactory, be aware that log messages may change,
     * as thread names are defined by the ThreadFactory, not by gaulois-pipe.
     * @param threadFactory The threadFactory to use
     */
    public void setThreadFactory(ThreadFactory threadFactory) {
        this.threadFactory=threadFactory;
    }
    protected ThreadFactory getThreadFactory() {
        if(threadFactory==null) {
            threadFactory = Executors.defaultThreadFactory();
        }
        return threadFactory;
    }
    
    /**
     * Adds an error to the error reporting system
     * @param ex The exception thrown to be reported
     * @param message Optionnal messages to report
     */
    public void collectError(Exception ex, String... message) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        for(String m:message) {
            pw.println(m);
        }
        ex.printStackTrace(pw);
        pw.flush();
        sendError(sw.getBuffer().toString());
    }
    public void collectError(GauloisRunException ex, String... message) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
//        pw.println("while processing "+ex.getSource());
        pw.println(ex.getMessage());
        for(String m:message) {
            pw.println(m);
        }
        Throwable innerEx = ex;
        while(innerEx.getCause()!=null) {
            innerEx = innerEx.getCause();
            if(innerEx instanceof TransformerException) break;
        }
        if(innerEx instanceof TransformerException) {
            TransformerException tEx = (TransformerException)innerEx;
            pw.println(errorListener.sel.getExpandedMessage(tEx));
            pw.println(errorListener.sel.getLocationMessage(tEx));
        } else {
            innerEx.printStackTrace(pw);
        }
        pw.flush();
        sendError(sw.getBuffer().toString());
    }
    protected void sendError(String error) {
        errors.add(error);
    }
    
    protected void startErrorCollector() {
    }
    protected int terminateErrorCollector() {
        for(String error: errors) {
            LOGGER.error(error);
        }
        return errors.size();
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
    protected String getCurrentDirUri() {
        if (currentDirUri==null) {
            currentDirUri = new File(currentDir).toURI().toString();
        }
        return currentDirUri;
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
            File directory = null;
            if(property.startsWith("file:/")) {
                try {
                    directory = new File(new URI(property));
                } catch(URISyntaxException ex) {
                    LOGGER.warn(property + " is not a vlid URI");
                }
            }
            if(directory==null) {
                directory = new File(property);
            }
            if(!directory.exists()) {
                directory.mkdirs();
            }
            if(directory.exists() && directory.isDirectory()) {
                debugDirectory = directory;
            }
        }
    }
    public DatatypeFactory getDatatypeFactory() { return datatypeFactory; }
    /**
     * Tells Gaulois whether cp protocol impementation has been installed or not.
     * This method must be called <strong>before</strong> the first instanciation of GauloisPipe.
     * @param installed Define if CP protocol handler has already been installed
     */
    public static void setProtocolInstalled(final boolean installed) { protocolInstalled = installed; }
    public static boolean isProtocolInstalled() { return protocolInstalled; }
}

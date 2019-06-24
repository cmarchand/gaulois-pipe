/**
 * This Source Code Form is subject to the terms of 
 * the Mozilla Public License, v. 2.0. If a copy of 
 * the MPL was not distributed with this file, You 
 * can obtain one at https://mozilla.org/MPL/2.0/.
 */
package fr.efl.chaine.xslt.config;

import java.io.File;

import fr.efl.chaine.xslt.InvalidSyntaxException;
import fr.efl.chaine.xslt.utils.ParameterValue;
import fr.efl.chaine.xslt.utils.ParametersMerger;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import net.sf.saxon.Configuration;
import net.sf.saxon.om.InscopeNamespaceResolver;
import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.XPathSelector;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmNodeKind;
import net.sf.saxon.s9api.XdmSequenceIterator;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.type.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import top.marchand.xml.gaulois.config.typing.Datatype;
import top.marchand.xml.gaulois.config.typing.DatatypeFactory;

/**
 * A helper class to build the configuration.
 * @author ext-cmarchand
 */
public class ConfigUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigUtil.class);
    
    private static final QName CONFIG_EL = new QName(Config.NS, "config");
    private static final QName PARAM_NAME = new QName("name");
    private static final QName PARAM_VALUE = new QName("value");
    private static final QName PARAM_ABSTRACT = new QName("abstract");
    private static final QName PARAM_AS = new QName("as");
    private static final QName QN_PATTERN = new QName("pattern");
    private static final QName QN_RECURSE = new QName("recurse");
    private static final QName QN_PARAM = new QName(Config.NS,"param");
    private static final QName QN_NULL = new QName(Config.NS, "null");
    private static final QName AS_NAME = new QName("as");
    private static final QName QN_GRAMMARS = new QName(Config.NS, "grammars");
    private final File currentDir;
    private final Configuration saxonConfig;
    private final URIResolver uriResolver;
    private final String configUri;
    private final boolean skipSchemaValidation;
    private boolean __isConfigUriTrueURI = false;
    private DatatypeFactory factory;
    
    public ConfigUtil(Configuration saxonConfig, URIResolver uriResolver, String configUri) throws InvalidSyntaxException {
        this(saxonConfig, uriResolver, configUri, false, System.getProperty("user.dir"));
    }
    public ConfigUtil(Configuration saxonConfig, URIResolver uriResolver, String configUri, final boolean skipSchemaValidation, final String currentDir) throws InvalidSyntaxException {
        super();
        this.saxonConfig = saxonConfig;
        this.uriResolver=uriResolver;
        this.skipSchemaValidation=skipSchemaValidation;
        Pattern pattern = Pattern.compile("[a-z].+:.+");
        if(pattern.matcher(configUri).matches()) {
            try {            	
                URL url = new URL(configUri); 
                try (InputStream is = url.openStream()) {
                    if(is==null) {
                        throw new InvalidSyntaxException(configUri+" not found or can not be open");
                    }
                    __isConfigUriTrueURI = true;
                }
            } catch (IOException ex) {
                throw new InvalidSyntaxException(configUri+" not found or can not be open",ex);
            }
        } else {
            File file = new File(configUri);
            if(!file.exists() && !file.isFile()) {
                throw new InvalidSyntaxException(configUri+" not found or not a regular file");
            }
        }
        this.configUri=configUri;
        this.currentDir = new File(currentDir);
        try {
            factory = DatatypeFactory.getInstance(saxonConfig);
        } catch(ValidationException ex) {
            throw new InvalidSyntaxException(ex);
        }
    }
    
    public Config buildConfig(HashMap<QName,ParameterValue> inputParameters) throws SaxonApiException, InvalidSyntaxException {
        try {
            Processor processor = new Processor(saxonConfig);
            if(!skipSchemaValidation) {
                try {
                    System.setProperty("javax.xml.validation.SchemaFactory:http://www.w3.org/2001/XMLSchema/v1.1","org.apache.xerces.jaxp.validation.XMLSchema11Factory");
                    SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema/v1.1");
                    Source schemaSource = saxonConfig.getURIResolver().resolve("cp:/fr/efl/chaine/xslt/schemas/gaulois-pipe_config.xsd", null);
                    Schema schema = schemaFactory.newSchema(schemaSource);
                    SchemaValidationErrorListener errListener = new SchemaValidationErrorListener();
                    Validator validator = schema.newValidator();
                    validator.setErrorHandler(errListener);
                    SAXSource saxSource = new SAXSource(
                            __isConfigUriTrueURI ? new InputSource(new URL(configUri).openStream()) :
                                    new InputSource(new FileInputStream(new File(configUri)))
                    );
                    validator.validate(saxSource);
                    if(errListener.hasErrors()) {
                        throw new InvalidSyntaxException(configUri+" does not respect gaulois-pipe schema");
                    }
                } catch(SAXException | TransformerException | IOException ex) {
                    LOGGER.error("while verifying schema conformity",ex);
                } catch(Error er) {
                    LOGGER.error("java.protocol.handler.pkgs="+System.getProperty("java.protocol.handler.pkgs"));
                    LOGGER.error("while parsing config",er);
                    throw er;
                }
            }
            if(LOGGER.isDebugEnabled() && __isConfigUriTrueURI) {
                // check if resolver can resolve URI
                LOGGER.debug("trying to resolve "+configUri);
                Source source = uriResolver.resolve(configUri, null);
                if(source==null) {
                    LOGGER.error(configUri+" can not be resolved. Please try again, setting org.xmlresolver logger to DEBUG");
                }
            }
            XdmNode configRoot = 
                    __isConfigUriTrueURI ? 
                    processor.newDocumentBuilder().build(uriResolver.resolve(configUri,null)) :
                    processor.newDocumentBuilder().build(new File(configUri));
            XPathCompiler xc = processor.newXPathCompiler();
            xc.declareNamespace("cfg", Config.NS);
            XPathSelector xs = xc.compile("/*").load();
            xs.setContextItem(configRoot);
            XdmNode root = (XdmNode)xs.evaluateSingle();
            if(CONFIG_EL.equals(root.getNodeName())) {
                Config config = new Config(root);
                // namespaces
                XdmSequenceIterator it = root.axisIterator(Axis.CHILD, Namespaces.QNAME);
                Namespaces namespaces = new Namespaces();
                while(it.hasNext()) {
                    XdmNode ns = (XdmNode)it.next();
                    XdmSequenceIterator itm = ns.axisIterator(Axis.CHILD, Namespaces.QN_MAPPING);
                    while(itm.hasNext()) {
                        XdmNode node = (XdmNode)itm.next();
                        String prefix = node.getAttributeValue(Namespaces.ATTR_PREFIX);
                        String uri = node.getAttributeValue(Namespaces.ATTR_URI);
                        namespaces.getMappings().put(prefix, uri);
                    }
                }
                config.setNamespaces(namespaces);
                it.close();
                // grammars issue #40
                XPathSelector gs = xc.compile("cfg:grammars/cfg:schema/@href").load();
                gs.setContextItem(root);
                it=gs.evaluate().iterator();
                List<String> grammars = new ArrayList<>();
                while(it.hasNext()) {
                    String href = it.next().getStringValue();
                    grammars.add(href);
                }
                it.close();
                config.setSchemaLocations(grammars);
                // params
//                config.getParams().putAll(inputParameters);
                HashMap<QName, ParameterValue> configParameters = new HashMap<>();
                it = root.axisIterator(Axis.CHILD, Config.PARAMS_CHILD);
                while(it.hasNext()) {
                    XdmNode params = (XdmNode)it.next();
                    XdmSequenceIterator itp = params.axisIterator(Axis.CHILD, new QName(Config.NS, "param"));
                    while(itp.hasNext()) {
                        //config.addParameter(buildParameter((XdmNode)itp.next(),inputParameters));
                        ParameterValue pv = buildParameter((XdmNode)itp.next(),inputParameters);
                        configParameters.put(pv.getKey(), pv);
                    }
                }
                config.getParams().putAll(ParametersMerger.merge(inputParameters, configParameters));
                // pipe
                config.setPipe(buildPipe((XdmNode)(root.axisIterator(Axis.CHILD, Pipe.QNAME).next()),config.getParams()));
                // sources
                XdmSequenceIterator sourceIterator = root.axisIterator(Axis.CHILD, Sources.QNAME);
                if(sourceIterator.hasNext()) {
                    config.setSources(buildSources((XdmNode)(sourceIterator.next()), config.getParams()));
                }
                // output
                // pour compatibilité ascendante, si on a un output sous la config, on essaie de le mettre sur le pipe
                // possible uniquement si le pipe est rectiligne
                XdmSequenceIterator outputIt = root.axisIterator(Axis.CHILD, Output.QNAME);
                if(outputIt.hasNext()) {
                    LOGGER.warn("Defining <output/> in config is now deprecated - but still works. You should define it in <pipe/>");
                    if(config.getPipe().getOutput()==null) {
                        config.getPipe().setOutput(buildOutput((XdmNode)(outputIt.next()), config.getParams()));
                    } else {
                        throw new InvalidSyntaxException("Using output outside of pipe is deprecated but supported, only if pipe has no output defined");
                    }
                }
                return config;
            } else {
                throw new InvalidSyntaxException("The file "+configUri+" does not respect schema saxon-pipe_config.xsd");
            }
        } catch(TransformerException ex) {
            throw new InvalidSyntaxException(configUri+" can not be read.");
        }
    }
    
    private Pipe buildPipe(XdmNode pipeNode, HashMap<QName,ParameterValue> parameters) throws IllegalStateException, InvalidSyntaxException {
        return buildPipe(pipeNode, parameters, null);
    }
    private Pipe buildPipe(XdmNode pipeNode, HashMap<QName,ParameterValue> parameters, Tee parentTee) throws IllegalStateException, InvalidSyntaxException {
        LOGGER.trace("buildPipe on "+pipeNode.getNodeName());
        Pipe pipe = new Pipe(parentTee);
        try {
            int nbThreads = Integer.parseInt(resolveEscapes(pipeNode.getAttributeValue(new QName(Pipe.ATTR_NB_THREADS)),parameters));
            pipe.setNbThreads(nbThreads);
        } catch(Throwable t) {}
        try {
            int max = Integer.parseInt(resolveEscapes(pipeNode.getAttributeValue(new QName(Pipe.ATTR_MAX)),parameters));
            pipe.setMultithreadMaxSourceSize(max);
        } catch(Throwable t) {}
        pipe.setTraceOutput(resolveEscapes(pipeNode.getAttributeValue(new QName(Pipe.ATTR_TRACE)),parameters));
        XdmSequenceIterator it = pipeNode.axisIterator(Axis.CHILD);
        while(it.hasNext()) {
            XdmItem item = it.next();
            if(item instanceof XdmNode) {
                // c'est un élément
                XdmNode node = (XdmNode)item;
                if(Xslt.QNAME.equals(node.getNodeName())) {
                    pipe.addXslt(buildXslt(node,parameters));
                } else if(JavaStep.QNAME.equals(node.getNodeName())) {
                    pipe.addXslt(buildJavaStep(node, parameters));
                } else if(ChooseStep.QNAME.equals(node.getNodeName())) {
                    pipe.addXslt(buildChooseStep(node, parameters));
                } else if(Tee.QNAME.equals(node.getNodeName())) {
                    pipe.setTee(buildTee(node, parameters));
                } else if(Output.QNAME.equals(node.getNodeName())) {
                    pipe.setOutput(buildOutput(node, parameters));
                } else if(node.getNodeKind()==XdmNodeKind.TEXT) {
                    // on ignore
                } else if(node.getNodeKind()==XdmNodeKind.COMMENT) {
                    // on ignore
                } else {
                    throw new InvalidSyntaxException(node.getNodeKind().toString()+" - "+ node.getNodeName()+": unexpected element in "+Pipe.QNAME);
                }
            }
        }
        return pipe;
    }
    
    private Tee buildTee(XdmNode teeNode, HashMap<QName,ParameterValue> parameters) throws InvalidSyntaxException {
        LOGGER.trace("buildTee on "+teeNode.getNodeName());
        Tee tee = new Tee();
        XdmSequenceIterator seq = teeNode.axisIterator(Axis.CHILD,Pipe.QNAME);
        while(seq.hasNext()) {
            XdmNode node = (XdmNode)seq.next();
            Pipe pipe = buildPipe(node, parameters, tee);
            tee.addPipe(pipe);
        }
        return tee;
    }
    private Sources buildSources(XdmNode sourcesNode, HashMap<QName,ParameterValue> parameters) throws InvalidSyntaxException {
        String orderBy = sourcesNode.getAttributeValue(Sources.ATTR_ORDERBY);
        String sort = sourcesNode.getAttributeValue(Sources.ATTR_SORT);
        LOGGER.trace("buildSources from {} with orderBy={} and sort={}", new Object[]{sourcesNode.getNodeName(), orderBy, sort});
        Sources sources = new Sources(orderBy, sort);
        XdmSequenceIterator it = sourcesNode.axisIterator(Axis.CHILD);
        while(it.hasNext()) {
            XdmNode node = (XdmNode)it.next();
            LOGGER.trace("buildSource from {}", node.getNodeName());
            if(CfgFile.QNAME.equals(node.getNodeName())) {
                try {
                    CfgFile localFile = buildFile(node, parameters);
                    LOGGER.trace("add source {}", localFile);
                    sources.addFile(localFile);
                } catch (URISyntaxException ex) {
                    LOGGER.error(ex.getMessage(),ex);
                    throw new InvalidSyntaxException(ex);
                }
            } else if(CfgFile.QN_FOLDER.equals(node.getNodeName())) {
                Collection<CfgFile> files = buildFolderContent(node, parameters);
                LOGGER.trace("buildSources from folder contains {} files", files.size());
                sources.addFiles(files);
            } else if(Listener.QName.equals(node.getNodeName())) {
                sources.setListener(buildListener(node, parameters));
            }
        }
        return sources;
    }
    private Listener buildListener(XdmNode listenerNode, HashMap<QName,ParameterValue> parameters) throws InvalidSyntaxException {
        String tmp = resolveEscapes(listenerNode.getAttributeValue(Listener.ATTR_PORT),parameters);
        int port = Listener.DEFAULT_PORT;
        try {
            port = Integer.parseInt(tmp);
        } catch(NumberFormatException ex) {}
        String stopKeyword = resolveEscapes(listenerNode.getAttributeValue(Listener.ATTR_STOP),parameters);
        Listener list = new Listener(port, stopKeyword);
        XdmSequenceIterator it = listenerNode.axisIterator(Axis.CHILD, JavaStep.QNAME);
        if(it.hasNext()) {
            list.setJavastep(buildJavaStep((XdmNode)it.next(), parameters));
        }
        return list;
    }
    private Xslt buildXslt(XdmNode xsltNode, HashMap<QName,ParameterValue> parameters) throws InvalidSyntaxException {
        LOGGER.trace("buildXslt on {}", xsltNode.getNodeName());
        Xslt ret = new Xslt(resolveEscapes(xsltNode.getAttributeValue(Xslt.ATTR_HREF),parameters));
        if("true".equals(xsltNode.getAttributeValue(Xslt.ATTR_TRACE_ACTIVE))) {
            ret.setTraceToAdd(true);
        }
        if("true".equals(xsltNode.getAttributeValue(Xslt.ATTR_DEBUG))) {
            ret.setDebug(true);
            ret.setId(xsltNode.getAttributeValue(Xslt.ATTR_ID));
        }
        XdmSequenceIterator it = xsltNode.axisIterator(Axis.CHILD, QN_PARAM);
        while(it.hasNext()) {
            ret.addParameter(buildParameter((XdmNode)it.next(),parameters));
        }
        return ret;
    }
    private JavaStep buildJavaStep(XdmNode javaNode, HashMap<QName,ParameterValue> parameters) throws InvalidSyntaxException {
        LOGGER.trace("buildJavaStep on {}", javaNode.getNodeName());
        JavaStep ret = new JavaStep(resolveEscapes(javaNode.getAttributeValue(JavaStep.ATTR_CLASS), parameters));
        XdmSequenceIterator it = javaNode.axisIterator(Axis.CHILD, QN_PARAM);
        while(it.hasNext()) {
            ret.addParameter(buildParameter((XdmNode)it.next(),parameters));
        }
        return ret;
    }
    private ChooseStep buildChooseStep(XdmNode chooseNode, HashMap<QName,ParameterValue> parameters) throws InvalidSyntaxException {
        LOGGER.trace("buildChooseStep on {}", chooseNode.getNodeName());
        ChooseStep chooseStep = new ChooseStep();
        XdmSequenceIterator it = chooseNode.axisIterator(Axis.CHILD, WhenEntry.QNAME);
        while(it.hasNext()) {
            XdmNode whenNode = (XdmNode)it.next();
            chooseStep.addWhen(buildWhen(whenNode, parameters));
        }
        it = chooseNode.axisIterator(Axis.CHILD, WhenEntry.QN_OTHERWISE);
        while(it.hasNext()) {
            XdmNode whenNode = (XdmNode)it.next();
            chooseStep.addWhen(buildWhen(whenNode, parameters));
        }
        return chooseStep;
    }
    private WhenEntry buildWhen(XdmNode whenNode, HashMap<QName,ParameterValue> parameters) throws InvalidSyntaxException {
        String test = whenNode.getAttributeValue(WhenEntry.ATTR_TEST);
        // particular case of the otherwise, which is implemented as a when[@test='true()']
        if((test==null || test.length()==0) && WhenEntry.QN_OTHERWISE.equals(whenNode.getNodeName()) ) {
            test="true()";
        }
        WhenEntry when = new WhenEntry(test);
        XdmSequenceIterator it = whenNode.axisIterator(Axis.CHILD);
        while(it.hasNext()) {
            XdmItem item = it.next();
            if(item instanceof XdmNode) {
                XdmNode node = (XdmNode)item;
                if(Xslt.QNAME.equals(node.getNodeName())) {
                    when.addStep(buildXslt(node,parameters));
                } else if(JavaStep.QNAME.equals(node.getNodeName())) {
                    when.addStep(buildJavaStep(node, parameters));
                } else if(ChooseStep.QNAME.equals(node.getNodeName())) {
                    when.addStep(buildChooseStep(node, parameters));
                } else if(node.getNodeKind()==XdmNodeKind.TEXT) {
                    // on ignore
                } else if(node.getNodeKind()==XdmNodeKind.COMMENT) {
                    // on ignore
                } else {
                    throw new InvalidSyntaxException(node.getNodeKind().toString()+" - "+ node.getNodeName()+": unexpected element in "+Pipe.QNAME);
                }
            }
        }
        return when;
    }
    private ParameterValue buildParameter(XdmNode param, HashMap<QName,ParameterValue> parameters) throws InvalidSyntaxException {
        LOGGER.trace("buildParameter on "+param.getNodeName());
        // attributes already presents will no be added, so return the existing parameter
        // issue#15 : parameter names are not anymore resolved, only QNamed
        try {
            String sDatatype = param.getAttributeValue(PARAM_AS);
            Datatype dt ;
            if(sDatatype!=null) {
                QName datatypeName = null;
                int doubleDotPosition = sDatatype.indexOf(":");
                if(doubleDotPosition>0) {
                    String prefix = sDatatype.substring(0, doubleDotPosition);
                    String uri = new InscopeNamespaceResolver(param.getUnderlyingNode()).getURIForPrefix(prefix, true);
                    if(uri==null) throw new InvalidSyntaxException("prefix "+prefix+" is not bound to any namespace URI ("+param.getLineNumber()+","+param.getColumnNumber()+")");
                    datatypeName=new QName(prefix, uri, sDatatype.substring(doubleDotPosition+1));
                } else {
                    datatypeName = new QName(sDatatype);
                }
                dt = factory.getDatatype(datatypeName);
            } else {
                dt = factory.XS_STRING;
            }
            ParameterValue pv;
            if("true".equals(param.getAttributeValue(PARAM_ABSTRACT))) {
                pv = new ParameterValue(resolveQName(param.getAttributeValue(PARAM_NAME)), dt);
                ParameterValue fcli = parameters.get(pv.getKey());
                if(fcli!=null) {
                    XdmValue value = pv.getDatatype().convert(
                            resolveEscapes(fcli.getValue().toString(),parameters), 
                            saxonConfig);
                    pv.setValue(value);
                }
                return pv;
            } else {
                pv = new ParameterValue(
                        resolveQName(param.getAttributeValue(PARAM_NAME)), 
                        dt.convert(resolveEscapes(param.getAttributeValue(PARAM_VALUE),parameters),saxonConfig),
                        dt);
                if(parameters.containsKey(pv.getKey())) return parameters.get(pv.getKey());
                else return pv;
            }
        } catch(ValidationException ex) {
            throw new InvalidSyntaxException(ex);
        }
    }
    public static QName resolveQName(String name) {
        if(name==null) return null;
        if(name.length()==0) return null;
        else if(name.startsWith("Q{")) return QName.fromEQName(name);
        else if(name.startsWith("{")) return QName.fromClarkName(name);
        else return new QName(name);
    }
    private CfgFile buildFile(XdmNode node, HashMap<QName,ParameterValue> parameters) throws URISyntaxException, InvalidSyntaxException {
        String href = node.getAttributeValue(CfgFile.ATTR_HREF);
        LOGGER.trace("buildFile from {} with href={}", node.getNodeName(), href);
        File f;
        href = resolveEscapes(href, parameters);
        if(href.startsWith("file:")) {
            f = new File(new URI(href));
        } else {
            f = new File(href);
            if(!f.exists()) {
                f = new File(currentDir, href);
            }
        }
        LOGGER.trace("buildFile on {}", f.getName());
        CfgFile ret = new CfgFile(f);
        XdmSequenceIterator it = node.axisIterator(Axis.CHILD, QN_PARAM);
        while(it.hasNext()) {
            ret.addParameter(buildParameter((XdmNode)it.next(),parameters));
        }
        ParameterValue relativeFile = new ParameterValue(ParametersMerger.INPUT_RELATIVE_FILE, f.toURI().toString(), factory.XS_STRING);
        ret.addParameter(relativeFile);
        ParameterValue relativeDir = new ParameterValue(ParametersMerger.INPUT_RELATIVE_DIR, f.getParentFile().toURI().toString(), factory.XS_STRING);
        ret.addParameter(relativeDir);
        return ret;
    }
    Object resolveEscapes(Object input, HashMap<QName,ParameterValue> params) {
        LOGGER.debug("resolveEscapes in "+input+" with "+params);
        if(input==null) return input;
        if(input instanceof String) {
            String ret = input.toString();
            return ParametersMerger.processParametersReplacement(ret, params);
        } else {
            return input;
        }
    }
    String resolveEscapes(String input, HashMap<QName,ParameterValue> params) {
        if(input==null) return input;
        String ret = input;
        ret = (String)ParametersMerger.processParametersReplacement(ret, params);
        if(LOGGER.isDebugEnabled()) {
            LOGGER.debug("resolveEscapes in {} -> {}", input, ret);
        }
        return ret;
    }
    private Collection<CfgFile> buildFolderContent(XdmNode node, HashMap<QName,ParameterValue> parameters) throws InvalidSyntaxException {
        LOGGER.trace("buildFolderContent on "+node.getNodeName());
        String pattern = (String)resolveEscapes(node.getAttributeValue(QN_PATTERN), parameters);
        final boolean recurse = getBooleanValue(node.getAttributeValue(QN_RECURSE));
        HashMap<QName, ParameterValue> params = new HashMap<>();
        XdmSequenceIterator it = node.axisIterator(Axis.CHILD, QN_PARAM);
        while(it.hasNext()) {
            ParameterValue p = buildParameter((XdmNode)it.next(), parameters);
            params.put(p.getKey(),p);
        }
        if(pattern==null) pattern="$.*^";
        final Pattern regex = Pattern.compile(pattern);
        LOGGER.trace("checking filenames to "+regex.toString());
        FilenameFilter filter;
        filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                boolean match = regex.matcher(filename).matches();
                if(!match) {
                    LOGGER.trace(filename+" not matched");
                }
                return match;
            }
        };
        String fileName = (String)resolveEscapes(node.getAttributeValue(CfgFile.ATTR_HREF),parameters);
        File dir;
        try {
            dir = new File(new URI(fileName));
        } catch (URISyntaxException | IllegalArgumentException ex) {
            dir = new File(currentDir, fileName);
        }
        
        if(!dir.isDirectory()) {
            LOGGER.warn(dir.getAbsolutePath()+" is not a valid directory");
            return Collections.EMPTY_LIST;
        }
        LOGGER.trace("dir="+dir+", filter="+filter+", recurse="+recurse);
        List<CfgFile> files = getFilesFromDirectory(dir,filter,recurse);
        Path dirPath= dir.toPath();
        for(CfgFile sourceFile:files) {
            for(ParameterValue p:params.values()) {
                sourceFile.addParameter(p);
            }
            ParameterValue relativeFile = new ParameterValue(ParametersMerger.INPUT_RELATIVE_FILE, makeAsUri(dirPath.relativize(sourceFile.getSource().toPath())), factory.XS_STRING);
            sourceFile.addParameter(relativeFile);
            ParameterValue relativeDir = new ParameterValue(ParametersMerger.INPUT_RELATIVE_DIR, makeAsUri(dirPath.relativize(sourceFile.getSource().getParentFile().toPath())), factory.XS_STRING);
            sourceFile.addParameter(relativeDir);

        }
        return files;
    }
    private String makeAsUri(final Path p) {
        return p.toString().replaceAll("\\\\", "/");
    }
    private Output buildOutput(XdmNode node, HashMap<QName,ParameterValue> parameters) throws InvalidSyntaxException {
        LOGGER.trace("buildOutput from {}", node.getNodeName());
        Output ret = new Output();
        // searching for output parameters
        XdmSequenceIterator attributes = node.axisIterator(Axis.ATTRIBUTE);
        while(attributes.hasNext()) {
            XdmNode attr = (XdmNode)attributes.next();
            ret.setOutputProperty(attr.getNodeName().getLocalName(), (String)resolveEscapes(attr.getStringValue(),parameters));
        }
        boolean nullOutput = node.axisIterator(Axis.CHILD, QN_NULL).hasNext();
        if(nullOutput) {
            ret.setNull(true);
        } else {
            XdmSequenceIterator consoleIterator = node.axisIterator(Axis.CHILD, Output.QN_CONSOLE);
            if(consoleIterator.hasNext()) {
                XdmNode console = (XdmNode)consoleIterator.next();
                String sConsole = console.getAttributeValue(Output.ATTR_CONSOLE_WHICH);
                if(sConsole==null) sConsole = "out";
                ret.setConsole(sConsole);
            } else {
                XdmNode folder = (XdmNode)node.axisIterator(Axis.CHILD, CfgFile.QN_FOLDER).next();
                String relative = resolveEscapes(folder.getAttributeValue(new QName("relative")),parameters);
                String temp = folder.getAttributeValue(new QName("to"));
                String to = resolveEscapes(temp, parameters);
                String absolute = resolveEscapes(folder.getAttributeValue(new QName("absolute")), parameters);
                if(relative!=null) ret.setRelativePath(relative);
                if(to!=null) ret.setRelativeTo(to);
                if(absolute!=null) ret.setAbsolute(absolute);
                XdmNode filename = (XdmNode)node.axisIterator(Axis.CHILD, new QName(Config.NS,"fileName")).next();
                String prefix = resolveEscapes(filename.getAttributeValue(new QName("prefix")),parameters);
                String name = resolveEscapes(filename.getAttributeValue(new QName("name")),parameters);
                String suffix = resolveEscapes(filename.getAttributeValue(new QName("suffix")), parameters);
                if(prefix!=null) ret.setPrefix(prefix);
                if(name!=null) ret.setName(name);
                if(suffix!=null) ret.setSuffix(suffix);
            }
        }
        return ret;
    }
    /**
     * Visibility package private only for unit-tests
     * @param directory
     * @param filter
     * @param recurse
     * @return Found files
     */
    List<CfgFile> getFilesFromDirectory(File directory, FilenameFilter filter, boolean recurse) {
        File [] files = directory.listFiles();
        List<CfgFile> ret = new ArrayList<>();
        for(File f:files) {
            if(f.isDirectory() && recurse) {
                ret.addAll(getFilesFromDirectory(f, filter, recurse));
            } else if(filter.accept(f.getParentFile(), f.getName())) {
                ret.add(new CfgFile(f));
            }
        }
        return ret;
    }
    private boolean getBooleanValue(String v) {
        return "true".equals(v) || "1".equals(v) || "yes".equals(v);
    }
    
    /**
     * Adds a parameter to the specified config.
     * @param config The config to modify
     * @param parameterPattern The prameter definition
     * @param factory The datatype factory to use
     * @throws InvalidSyntaxException If the parameter definition is incorrect
     */
    public static void addConfigParameter(Config config, String parameterPattern, DatatypeFactory factory) throws InvalidSyntaxException {
        config.addParameter(parseParameterPattern(parameterPattern, factory));
    }
    public static ParameterValue parseParameterPattern(String parameterPattern, DatatypeFactory factory) throws InvalidSyntaxException {
        String[] dec = parameterPattern.split("=");
        if(dec.length!=2) {
            throw new InvalidSyntaxException(parameterPattern+" is not a valid parameter declaration");
        }
        return new ParameterValue(resolveQName(dec[0]), dec[1], factory.XS_STRING);
    }
    /**
     * Defines the thread number to use in this config
     * @param config The config to modify
     * @param argument The number of threads
     * @throws InvalidSyntaxException It should never throws anything... but if argument is not an integer...
     */
    public static void setNbThreads(Config config, String argument) throws InvalidSyntaxException {
        try {
            if(config.getPipe()==null) {
                Pipe pipe = new Pipe();
                pipe.setNbThreads(Integer.parseInt(argument));
                config.setPipe(pipe);
            }
        } catch(Exception ex) {
            throw new InvalidSyntaxException(ex);
        }
    }
    /**
     * Adds an input file to the config
     * @param config The config to modify
     * @param argument The file to add
     * @param factory The datatypeFactory to use
     * @throws InvalidSyntaxException If input file definition is incorrect
     */
    public static void addInputFile(Config config, String argument, DatatypeFactory factory) throws InvalidSyntaxException {
        Sources sources = config.getSources();
        if(sources==null) {
            sources = new Sources();
            config.setSources(sources);
        }
        sources.addFile(resolveInputFile(argument, factory));
    }
    /**
     * Adds a template (XSLT) to the specified config
     * @param config the config to modify
     * @param argument The Xsl to add
     * @param factory The datatype factory to use
     * @throws fr.efl.chaine.xslt.InvalidSyntaxException If xsl definition is incorrect
     */
    public static void addTemplate(Config config, String argument, DatatypeFactory factory) throws InvalidSyntaxException {
        Pipe pipe = config.getPipe();
        if(pipe==null) {
            pipe = new Pipe();
            config.setPipe(pipe);
        }
        pipe.addXslt(resolveTemplate(argument, factory));
    }
    /**
     * Defines the output of this config
     * @param config The config used
     * @param argument The absolute dir to put files in
     * @throws fr.efl.chaine.xslt.InvalidSyntaxException  If output definition is incorrect
     */
    public static void setOutput(Config config, String argument) throws InvalidSyntaxException {
        if(config.getPipe()==null) {
            Output output = new Output();
            output.setAbsolute(argument);
            output.setName("${name}");
            config.getPipe().setOutput(output);
        }
    }

    private static Xslt resolveTemplate(String path, DatatypeFactory factory) {
        int index = path.indexOf("(");
        String filePath = index>0 ? path.substring(0, index) : path;
        String sParams = index>0 ? path.substring(index+1, path.length()-1).trim() : "";
        Xslt ret = new Xslt(filePath);
        for(ParameterValue p:getParametersOfTemplate(sParams, factory)) {
            ret.addParameter(p);
        }
        return ret;
    }
    private static CfgFile resolveInputFile(String path, DatatypeFactory factory) {
        int index = path.indexOf("(");
        String filePath = index>0 ? path.substring(0, index) : path;
        String sParams = index>0 ? path.substring(index+1, path.length()-1).trim() : "";
        CfgFile ret = new CfgFile(new File(filePath));
        for(ParameterValue p:getParametersOfTemplate(sParams, factory)) {
            ret.addParameter(p);
        }
        ParameterValue relativeFile = new ParameterValue(ParametersMerger.INPUT_RELATIVE_FILE, ret.getSource().toURI().toString(), factory.XS_STRING);
        ret.addParameter(relativeFile);
        ParameterValue relativeDir = new ParameterValue(ParametersMerger.INPUT_RELATIVE_DIR, ret.getSource().getParentFile().toURI().toString(), factory.XS_STRING);
        ret.addParameter(relativeDir);
        return ret;
    }
    private static List<ParameterValue> getParametersOfTemplate(final String s, DatatypeFactory factory) {
        ArrayList<ParameterValue> ret = new ArrayList<>();
        String[] entries = s.split(",");
        for(String entry:entries) {
            String[] ps = entry.split("=");
            if(ps.length==2) {
                ParameterValue p = new ParameterValue(resolveQName(ps[0]), ps[1], factory.XS_STRING);
                ret.add(p);
            }
        }
        return ret;
    }
    
    private class SchemaValidationErrorListener implements ErrorHandler {
        private boolean errors = false;
        public boolean hasErrors() { return errors; }

        @Override
        public void warning(SAXParseException exception) throws SAXException {
            ConfigUtil.LOGGER.warn("validating configFile: "+exception.getMessage());
        }

        @Override
        public void error(SAXParseException exception) throws SAXException {
            ConfigUtil.LOGGER.warn("validating configFile: "+exception.getMessage());
            errors = true;
        }

        @Override
        public void fatalError(SAXParseException exception) throws SAXException {
            ConfigUtil.LOGGER.warn("validating configFile: "+exception.getMessage());
            errors = true;
        }
    }

}

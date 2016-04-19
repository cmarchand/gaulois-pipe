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
import fr.efl.chaine.xslt.utils.SaxonConfigurationFactory;
import java.io.FilenameFilter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import net.sf.saxon.Configuration;
import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XPathSelector;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmNodeKind;
import net.sf.saxon.s9api.XdmSequenceIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author ext-cmarchand
 */
public class ConfigUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigUtil.class);
    
    private static final QName CONFIG_EL = new QName(Config.NS, "config");
    private static final QName PARAM_NAME = new QName("name");
    private static final QName PARAM_VALUE = new QName("value");
    private static final QName QN_PATTERN = new QName("pattern");
    private static final QName QN_RECURSE = new QName("recurse");
    private static final QName QN_PARAM = new QName(Config.NS,"param");
    private final File file;
    public ConfigUtil(String fileName) throws InvalidSyntaxException {
        super();
        file = new File(fileName);
        if(!file.exists() && !file.isFile()) {
            throw new InvalidSyntaxException("fileName not found or not a regular file");
        }
    }
    
    public Config buildConfig(Collection<ParameterValue> inputParameters) throws SaxonApiException, InvalidSyntaxException {
        Configuration saxonConfig = SaxonConfigurationFactory.buildConfiguration();
        Processor processor = new Processor(saxonConfig);
        XdmNode configRoot = processor.newDocumentBuilder().build(file);
        XPathSelector xs = processor.newXPathCompiler().compile("/*").load();
        xs.setContextItem(configRoot);
        XdmNode root = (XdmNode)xs.evaluateSingle();
        if(CONFIG_EL.equals(root.getNodeName())) {
            Config config = new Config(root);
            // params
            XdmSequenceIterator it = root.axisIterator(Axis.CHILD, Config.PARAMS_CHILD);
            while(it.hasNext()) {
                XdmNode params = (XdmNode)it.next();
                XdmSequenceIterator itp = params.axisIterator(Axis.CHILD, new QName(Config.NS, "param"));
                while(itp.hasNext()) {
                    config.addParameter(buildParameter((XdmNode)itp.next(),null));
                }
            }
            Collection<ParameterValue> allParameters = new ArrayList<>(config.getParams());
            allParameters.addAll(inputParameters);
            // pipe
            config.setPipe(buildPipe((XdmNode)(root.axisIterator(Axis.CHILD, Pipe.QNAME).next()),allParameters));
            // sources
            config.setSources(buildSources((XdmNode)(root.axisIterator(Axis.CHILD, Sources.QNAME).next()), allParameters));
            // output
            // pour compatibilité ascendante, si on a un output sous la config, on essaie de le mettre sur le pipe
            // possible uniquement si le pipe est rectiligne
            XdmSequenceIterator outputIt = root.axisIterator(Axis.CHILD, Output.QNAME);
            if(outputIt.hasNext()) {
                LOGGER.warn("Defining <output/> in config is now deprecated - but still works. You should define it in <pipe/>");
                if(config.getPipe().getOutput()==null) {
                    config.getPipe().setOutput(buildOutput((XdmNode)(outputIt.next()), allParameters));
                } else {
                    throw new InvalidSyntaxException("Using output outside of pipe is deprecated but supported, only if pipe has no output defined");
                }
            }
            return config;
        } else {
            throw new InvalidSyntaxException("The file "+file.getAbsolutePath()+" does not respect schema saxon-pipe_config.xsd");
        }
    }
    
    private Pipe buildPipe(XdmNode pipeNode, Collection<ParameterValue> parameters) throws IllegalStateException, InvalidSyntaxException {
        LOGGER.debug("buildPipe on "+pipeNode.getNodeName());
        Pipe pipe = new Pipe();
        try {
            int nbThreads = Integer.parseInt(resolveEscapes(pipeNode.getAttributeValue(new QName(Pipe.ATTR_NB_THREADS)),parameters));
            pipe.setNbThreads(nbThreads);
        } catch(Throwable t) {}
        try {
            int max = Integer.parseInt(resolveEscapes(pipeNode.getAttributeValue(new QName(Pipe.ATTR_MAX)),parameters));
            pipe.setMultithreadMaxSourceSize(max);
        } catch(Throwable t) {}
        XdmSequenceIterator it = pipeNode.axisIterator(Axis.CHILD);
        while(it.hasNext()) {
            XdmItem item = it.next();
            if(item instanceof XdmNode) {
                // c'est un élément
                XdmNode node = (XdmNode)item;
                if(Xslt.QNAME.equals(node.getNodeName())) {
                    pipe.addXslt(buildXslt(node,parameters));
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
    
    private Tee buildTee(XdmNode teeNode, Collection<ParameterValue> parameters) throws InvalidSyntaxException {
        LOGGER.debug("buildTee on "+teeNode.getNodeName());
        Tee tee = new Tee();
        XdmSequenceIterator seq = teeNode.axisIterator(Axis.CHILD);
        while(seq.hasNext()) {
            XdmNode node = (XdmNode)seq.next();
            QName nodeName = node.getNodeName();
            if(Tee.PIPE1.equals(nodeName) || Tee.PIPE2.equals(nodeName)) {
                Pipe pipe = buildPipe(node, parameters);
                if(Tee.PIPE1.equals(nodeName)) {
                    tee.setPipe1(pipe);
                } else {
                    tee.setPipe2(pipe);
                }
            } else if(node.getNodeKind()!=XdmNodeKind.TEXT) {
                throw new InvalidSyntaxException("tee must only contains a pipe1 and a pipe2 element. Unexpected element: "+nodeName);
            }
        }
        return tee;
    }
    private Sources buildSources(XdmNode sourcesNode, Collection<ParameterValue> parameters) throws InvalidSyntaxException {
        String orderBy = sourcesNode.getAttributeValue(Sources.ATTR_ORDERBY);
        String sort = sourcesNode.getAttributeValue(Sources.ATTR_SORT);
        LOGGER.info("buildSources from {} with orderBy={} and sort={}", new Object[]{sourcesNode.getNodeName(), orderBy, sort});
        Sources sources = new Sources(orderBy, sort);
        XdmSequenceIterator it = sourcesNode.axisIterator(Axis.CHILD);
        while(it.hasNext()) {
            XdmNode node = (XdmNode)it.next();
            LOGGER.debug("buildSource from {}", node.getNodeName());
            if(CfgFile.QNAME.equals(node.getNodeName())) {
                try {
                    CfgFile localFile = buildFile(node, parameters);
                    LOGGER.debug("add source {}", localFile);
                    sources.addFile(localFile);
                } catch (URISyntaxException ex) {
                    LOGGER.error(ex.getMessage(),ex);
                    throw new InvalidSyntaxException(ex);
                }
            } else if(CfgFile.QN_FOLDER.equals(node.getNodeName())) {
                Collection<CfgFile> files = buildFolderContent(node, parameters);
                LOGGER.info("buildSources from folder contains {} files", files.size());
                sources.addFiles(files);
            }
        }
        return sources;
    }
    private Xslt buildXslt(XdmNode xsltNode, Collection<ParameterValue> parameters) {
        LOGGER.debug("buildXslt on {}", xsltNode.getNodeName());
        Xslt ret = new Xslt(resolveEscapes(xsltNode.getAttributeValue(Xslt.ATTR_HREF),parameters));
        XdmSequenceIterator it = xsltNode.axisIterator(Axis.CHILD, QN_PARAM);
        while(it.hasNext()) {
            ret.addParameter(buildParameter((XdmNode)it.next(),parameters));
        }
        return ret;
    }
    private ParameterValue buildParameter(XdmNode param, Collection<ParameterValue> parameters) {
        LOGGER.debug("buildParameter on "+param.getNodeName());
        return new ParameterValue(resolveEscapes(param.getAttributeValue(PARAM_NAME),parameters), resolveEscapes(param.getAttributeValue(PARAM_VALUE),parameters));
    }
    private CfgFile buildFile(XdmNode node, Collection<ParameterValue> parameters) throws URISyntaxException {
        String href = node.getAttributeValue(CfgFile.ATTR_HREF);
        LOGGER.debug("buildFile from {} with href={}", node.getNodeName(), href);
        File f;
        href = resolveEscapes(href, parameters);
        if(href.startsWith("file:")) {
            f = new File(new URI(href));
        } else {
            f = new File(href);
        }
        LOGGER.debug("buildFile on {}", f.getName());
        CfgFile ret = new CfgFile(f);
        XdmSequenceIterator it = node.axisIterator(Axis.CHILD, QN_PARAM);
        while(it.hasNext()) {
            ret.addParameter(buildParameter((XdmNode)it.next(),parameters));
        }
        return ret;
    }
    String resolveEscapes(String input, Collection<ParameterValue> params) {
        if(input==null) return input;
        String ret = input;
        if(params!=null) {
            for(ParameterValue pv: params) {
                ret = ret.replaceAll("\\$\\["+pv.getKey()+"\\]", pv.getValue());
            }
        }
        return ret;
    }
    private Collection<CfgFile> buildFolderContent(XdmNode node, Collection<ParameterValue> parameters) throws InvalidSyntaxException {
        LOGGER.debug("buildFolderContent on "+node.getNodeName());
        String pattern = resolveEscapes(node.getAttributeValue(QN_PATTERN), parameters);
        final boolean recurse = getBooleanValue(node.getAttributeValue(QN_RECURSE));
        HashMap<String, ParameterValue> params = new HashMap<>();
        XdmSequenceIterator it = node.axisIterator(Axis.CHILD, QN_PARAM);
        while(it.hasNext()) {
            ParameterValue p = buildParameter((XdmNode)it.next(), parameters);
            params.put(p.getKey(),p);
        }
        if(pattern==null) pattern="$.*^";
        final Pattern regex = Pattern.compile(pattern);
        LOGGER.debug("checking filenames to "+regex.toString());
        FilenameFilter filter;
        filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                if(recurse) return true;
                boolean match = regex.matcher(filename).matches();
                if(!match) {
                    LOGGER.debug(filename+" not matched");
                }
                return match;
            }
        };
        File dir = new File(resolveEscapes(node.getAttributeValue(CfgFile.ATTR_HREF),parameters));
        if(!dir.isDirectory()) {
            throw new InvalidSyntaxException(dir.getAbsolutePath()+" is not a valid directory");
        }
        LOGGER.debug("dir="+dir+", filter="+filter+", recurse="+recurse);
        List<CfgFile> files = getFilesFromDirectory(dir,filter,recurse);
        for(CfgFile sourceFile:files) {
            for(ParameterValue p:params.values()) {
                sourceFile.addParameter(p);
            }
        }
        return files;
    }
    private Output buildOutput(XdmNode node, Collection<ParameterValue> parameters) throws InvalidSyntaxException {
        LOGGER.debug("buildOutput from {}", node.getNodeName());
        Output ret = new Output();
        // searching for output parameters
        XdmSequenceIterator attributes = node.axisIterator(Axis.ATTRIBUTE);
        while(attributes.hasNext()) {
            XdmNode attr = (XdmNode)attributes.next();
            ret.setOutputProperty(attr.getNodeName().getLocalName(), resolveEscapes(attr.getStringValue(),parameters));
        }
        XdmNode folder = (XdmNode)node.axisIterator(Axis.CHILD, CfgFile.QN_FOLDER).next();
        String relative = resolveEscapes(folder.getAttributeValue(new QName("relative")),parameters);
        String to = resolveEscapes(folder.getAttributeValue(new QName("to")), parameters);
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
        return ret;
    }
    private List<CfgFile> getFilesFromDirectory(File directory, FilenameFilter filter, boolean recurse) {
        File [] files = directory.listFiles(filter);
        List<CfgFile> ret = new ArrayList<>();
        for(File f:files) {
            if(f.isDirectory() && recurse) {
                ret.addAll(getFilesFromDirectory(f,filter, recurse));
            } else {
                ret.add(new CfgFile(f));
            }
        }
        return ret;
    }
    private boolean getBooleanValue(String v) {
        return "true".equals(v) || "1".equals(v);
    }
    
    /**
     * Cette méthode statique ne peut être utilisée que pour construire un pipe linéaire (sans <tt>&lt;tee&gt;</tt>)
     * @param config
     * @param parameterPattern
     * @throws InvalidSyntaxException 
     */
    public static void addConfigParameter(Config config, String parameterPattern) throws InvalidSyntaxException {
        config.addParameter(parseParameterPattern(parameterPattern));
    }
    public static ParameterValue parseParameterPattern(String parameterPattern) throws InvalidSyntaxException {
        String[] dec = parameterPattern.split("=");
        if(dec.length!=2) {
            throw new InvalidSyntaxException(parameterPattern+" is not a valid parameter declaration");
        }
        return new ParameterValue(dec[0], dec[1]);
    }
    /**
     * Cette méthode statique ne peut être utilisée que pour construire un pipe linéaire (sans <tt>&lt;tee&gt;</tt>)
     * @param config
     * @param argument
     * @throws InvalidSyntaxException 
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
     * Cette méthode statique ne peut être utilisée que pour construire un pipe linéaire (sans <tt>&lt;tee&gt;</tt>)
     * @param config
     * @param argument
     * @throws InvalidSyntaxException 
     */
    public static void addInputFile(Config config, String argument) throws InvalidSyntaxException {
        Sources sources = config.getSources();
        if(sources==null) {
            sources = new Sources();
            config.setSources(sources);
        }
        sources.addFile(resolveInputFile(argument));
    }
    /**
     * Cette méthode statique ne peut être utilisée que pour construire un pipe linéaire (sans <tt>&lt;tee&gt;</tt>)
     * @param config
     * @param argument 
     * @throws fr.efl.chaine.xslt.InvalidSyntaxException 
     */
    public static void addTemplate(Config config, String argument) throws InvalidSyntaxException {
        Pipe pipe = config.getPipe();
        if(pipe==null) {
            pipe = new Pipe();
            config.setPipe(pipe);
        }
        pipe.addXslt(resolveTemplate(argument));
    }
    /**
     * Cette méthode statique ne peut être utilisée que pour construire un pipe linéaire (sans <tt>&lt;tee&gt;</tt>)
     * @param config
     * @param argument 
     * @throws fr.efl.chaine.xslt.InvalidSyntaxException 
     */
    public static void setOutput(Config config, String argument) throws InvalidSyntaxException {
        if(config.getPipe()==null) {
            Output output = new Output();
            output.setAbsolute(argument);
            output.setName("${name}");
            config.getPipe().setOutput(output);
        }
    }

    private static Xslt resolveTemplate(String path) {
        int index = path.indexOf("(");
        String filePath = index>0 ? path.substring(0, index) : path;
        String sParams = index>0 ? path.substring(index+1, path.length()-1).trim() : "";
        Xslt ret = new Xslt(filePath);
        for(ParameterValue p:getParametersOfTemplate(sParams)) {
            ret.addParameter(p);
        }
        return ret;
    }
    private static CfgFile resolveInputFile(String path) {
        int index = path.indexOf("(");
        String filePath = index>0 ? path.substring(0, index) : path;
        String sParams = index>0 ? path.substring(index+1, path.length()-1).trim() : "";
        CfgFile ret = new CfgFile(new File(filePath));
        for(ParameterValue p:getParametersOfTemplate(sParams)) {
            ret.addParameter(p);
        }
        return ret;
    }
    private static List<ParameterValue> getParametersOfTemplate(final String s) {
        ArrayList<ParameterValue> ret = new ArrayList<>();
        String[] entries = s.split(",");
        for(String entry:entries) {
            String[] ps = entry.split("=");
            if(ps.length==2) {
                ParameterValue p = new ParameterValue(ps[0], ps[1]);
                ret.add(p);
            }
        }
        return ret;
    }

}

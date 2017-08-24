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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;

import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XdmAtomicValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An output definition
 * @author ext-cmarchand
 */
public class Output implements Verifiable {
    final static QName QNAME = new QName(Config.NS, "output");
    static final QName QN_CONSOLE = new QName(Config.NS, "console");
    static final QName ATTR_CONSOLE_WHICH = new QName("which");
    public static final HashMap<String,OutputPropertyEntry> VALID_OUTPUT_PROPERTIES = new HashMap<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(Output.class);
    private String relativeTo, relativePath;
    private String absolute;
    private String prefix, suffix, name;
    private String console = null;
    private String id;
    private final OutputProperties outputProperties;
    private boolean nullOutput = false;
    
    static {
        VALID_OUTPUT_PROPERTIES.put("byte-order-mark", new OutputPropertyEntry(Serializer.Property.BYTE_ORDER_MARK, "yes", "no"));
        VALID_OUTPUT_PROPERTIES.put("cdata-section-elements", new OutputPropertyEntry(Serializer.Property.CDATA_SECTION_ELEMENTS));
        VALID_OUTPUT_PROPERTIES.put("doctype-public", new OutputPropertyEntry(Serializer.Property.DOCTYPE_PUBLIC));
        VALID_OUTPUT_PROPERTIES.put("doctype-system", new OutputPropertyEntry(Serializer.Property.DOCTYPE_SYSTEM));
        VALID_OUTPUT_PROPERTIES.put("encoding", new OutputPropertyEntry(Serializer.Property.ENCODING));
        VALID_OUTPUT_PROPERTIES.put("escape-uri-attributes", new OutputPropertyEntry(Serializer.Property.ESCAPE_URI_ATTRIBUTES, "yes", "no"));
//        VALID_OUTPUT_PROPERTIES.put("html-version", new OutputPropertyEntry(Serializer.Property.HTML_VERSION));
        VALID_OUTPUT_PROPERTIES.put("include-content-type", new OutputPropertyEntry(Serializer.Property.INCLUDE_CONTENT_TYPE, "yes", "no"));
        VALID_OUTPUT_PROPERTIES.put("indent", new OutputPropertyEntry(Serializer.Property.INDENT, "yes", "no"));
        VALID_OUTPUT_PROPERTIES.put("media-type", new OutputPropertyEntry(Serializer.Property.MEDIA_TYPE));
        VALID_OUTPUT_PROPERTIES.put("method", new OutputPropertyEntry(Serializer.Property.METHOD, "xml", "html", "xhtml", "text"));
        VALID_OUTPUT_PROPERTIES.put("normalization-form", new OutputPropertyEntry(Serializer.Property.NORMALIZATION_FORM, "NFC", "NFD", "NFKC", "NFKD", "none"));
        VALID_OUTPUT_PROPERTIES.put("omit-xml-declaration", new OutputPropertyEntry(Serializer.Property.OMIT_XML_DECLARATION, "yes", "no"));
        VALID_OUTPUT_PROPERTIES.put("saxon-attribute-order", new OutputPropertyEntry(Serializer.Property.SAXON_ATTRIBUTE_ORDER));
        VALID_OUTPUT_PROPERTIES.put("saxon-character-representation", new OutputPropertyEntry(Serializer.Property.SAXON_CHARACTER_REPRESENTATION));
        VALID_OUTPUT_PROPERTIES.put("saxon-double-space", new OutputPropertyEntry(Serializer.Property.SAXON_DOUBLE_SPACE));
        VALID_OUTPUT_PROPERTIES.put("saxon-implicit-result-document", new OutputPropertyEntry(Serializer.Property.SAXON_IMPLICIT_RESULT_DOCUMENT));
        VALID_OUTPUT_PROPERTIES.put("saxon-indent-spaces", new OutputPropertyEntry(Serializer.Property.SAXON_INDENT_SPACES));
        VALID_OUTPUT_PROPERTIES.put("saxon-line-length", new OutputPropertyEntry(Serializer.Property.SAXON_LINE_LENGTH));
//        VALID_OUTPUT_PROPERTIES.put("saxon-new-line", new OutputPropertyEntry(Serializer.Property.SAXON_NEWLINE));
        VALID_OUTPUT_PROPERTIES.put("saxon-recognize-binary", new OutputPropertyEntry(Serializer.Property.SAXON_RECOGNIZE_BINARY, "yes", "no"));
        VALID_OUTPUT_PROPERTIES.put("saxon-suppress-inndentation", new OutputPropertyEntry(Serializer.Property.SAXON_SUPPRESS_INDENTATION));
        VALID_OUTPUT_PROPERTIES.put("standalone", new OutputPropertyEntry(Serializer.Property.STANDALONE, "yes", "no"));
        VALID_OUTPUT_PROPERTIES.put("undeclare-prefixes", new OutputPropertyEntry(Serializer.Property.UNDECLARE_PREFIXES));
        VALID_OUTPUT_PROPERTIES.put("use-character-maps", new OutputPropertyEntry(Serializer.Property.USE_CHARACTER_MAPS));
        VALID_OUTPUT_PROPERTIES.put("version", new OutputPropertyEntry(Serializer.Property.VERSION, "1.0", "1.1"));
    }
    public Output() {
        super();
        outputProperties = new OutputProperties () {
            @Override
            public Object defineProperty(String key, String value) throws InvalidSyntaxException {
                // ignore @id attribute
                if("id".equals(key)) {
                    setId(value);
                    return value;
                }
                OutputPropertyEntry ope = VALID_OUTPUT_PROPERTIES.get(key);
                if(ope!=null) {
                    if(ope.isValueValid(value)) {
                        return super.setProperty(key, value); 
                    } else {
                        throw new InvalidSyntaxException(value+" is no a valid value for "+key);
                    }
                } else throw new InvalidSyntaxException(key+" is not a valid output property");
            }
        };
    }

    public void setRelativeTo(String relativeTo) {
        this.relativeTo = relativeTo;
        absolute = null;
    }
    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
        absolute = null;
    }
    public void setAbsolute(String absolute) {
        this.absolute = absolute;
        relativePath = null;
        relativeTo = null;
    }
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }
    public void setName(String name) {
        this.name = name;
    }
    private boolean isAbsolute() {
        return absolute!=null;
    }
    public void setNull(final boolean nullOutput) {
        this.nullOutput = nullOutput;
    }
    /**
     * Defines outputProperties, based on <tt>props</tt> values.
     * It does not modify {@link #outputProperties} <tt>Properties</tt> object, it defines the values in.
     * It call {@link #setOutputProperty(java.lang.String, java.lang.String)} for each property
     * All existing properties are removed before setting new properties.
     * @param props The properties to set
     * @throws fr.efl.chaine.xslt.InvalidSyntaxException  If a property or a value is invalid
     */
    public void setOutputProperties(final Properties props) throws InvalidSyntaxException {
        outputProperties.clear();
        for(Object key: props.keySet()) {
            outputProperties.defineProperty((String)key, (String)props.get(key));
        }
    }
    /**
     * Sets a new output property value. If this property was allready defined,
     * it is overriden
     * @param key The property name
     * @param value The property value
     * @throws fr.efl.chaine.xslt.InvalidSyntaxException If the property is invalid, or if the value is not allowed for the property
     */
    public void setOutputProperty(final String key, final String value) throws InvalidSyntaxException {
        outputProperties.defineProperty(key, value);
    }
    public void unsetOutputProperty(final String key) {
        outputProperties.remove(key);
    }
    /**
     * Returns <tt>true</tt> if the output should not be written anywhere
     * @return {@code true} if this output should produce nothing
     */
    public boolean isNullOutput() {
        return nullOutput;
    }
    /**
     * Returns a copy of outputProperties
     * @return A new copy of output properties
     */
    public Properties getOutputProperties() {
        Properties ret = new Properties();
        for(Object key: outputProperties.keySet()) {
            String sKey = key.toString();
            ret.setProperty(sKey,outputProperties.getProperty(sKey));
        }
        return ret;
    }
    public String getOutputProperty(final String key) {
        return outputProperties.getProperty(key);
    }
    
    /**
     * Renvoie le fichier destination
     * @param sourceFile Le fichier source traité
     * @param parameters Parameters to give. Should be used only to parse the escapes
     * @return The file to create
     * @throws InvalidSyntaxException If this output has no been correctly defined
     * @throws java.net.URISyntaxException If the constructed URI is no valid
     */
    public File getDestinationFile(File sourceFile, HashMap<QName,ParameterValue> parameters) throws InvalidSyntaxException, URISyntaxException {
        File ret;
        if(isAbsolute()) {
            String __abs = absolute;
            int pos = __abs.indexOf("${");
            while(pos>=0) {
                int closingPos = __abs.indexOf("}", pos);
                String propertyName = __abs.substring(pos+2, closingPos);
                String propertyValue = System.getProperty(propertyName);
                if(propertyValue!=null) {
                    LOGGER.debug("Replace system property {} with value {} in {} produces {}", new String[]{propertyName, propertyValue, __abs, __abs.replaceAll("\\$\\{"+propertyName+"\\}", Matcher.quoteReplacement(propertyValue))});
                    __abs = __abs.replaceAll("\\$\\{"+propertyName+"\\}", Matcher.quoteReplacement(propertyValue));
                } else {
                    LOGGER.warn("System property "+propertyName+" is not defined");
                }
                pos = __abs.indexOf("${", pos+1);
            }
            for(ParameterValue pv:parameters.values()) {
                LOGGER.debug("replacing $["+pv.getKey()+"]");
                if(pv.getValue() instanceof String) {
                    __abs = __abs.replaceAll("\\$\\["+pv.getKey()+"\\]", pv.getValue().toString());
                }
            }
            File directory = __abs.startsWith("file:") ? new File(new URI(__abs)) : new File(__abs);
            ret = new File(directory, getFileName(sourceFile, parameters));
        } else {
            File directory = null;
            if("source-file".equals(relativeTo)) directory = sourceFile.getParentFile();
            else if(relativeTo.startsWith("${")) {
                directory = new File(System.getProperty(relativeTo.substring(2).substring(0,relativeTo.length()-3)));
            } else {
                throw new InvalidSyntaxException("folder/@to must be either source-file or ${xxx} where xxx is a system-property name. "+relativeTo+" is not a valid value");
            }
            directory = new File(directory, relativePath);
            ret = new File(directory, getFileName(sourceFile, parameters));
        }
        return ret;
    }
    private String getFileName(File sourceFile, HashMap<QName,ParameterValue> parameters) {
        String filename = (prefix!=null?prefix:"") + name + (suffix!=null?suffix:"");
        String sourceName = sourceFile.getName();
        int ix = sourceName.lastIndexOf(".");
        // FIXME: this shouldn't be supported, it has been replaced by input-* pseudo-variables
        String extension = sourceName.substring(ix);
        String basename = sourceName.substring(0, ix);
        String ret = filename.replaceAll("\\$\\{name\\}", sourceName).replaceAll("\\$\\{basename\\}", basename).replaceAll("\\$\\{extension\\}", extension);
        for(ParameterValue pv:parameters.values()) {
            if(pv.getValue() instanceof String || pv.getValue() instanceof XdmAtomicValue) {
                ret = ret.replaceAll("\\$\\["+pv.getKey()+"\\]", pv.getValue().toString());
            }
        }
        return ret;
    }

    @Override
    public void verify() throws InvalidSyntaxException {
        if(nullOutput) return;
        if(isConsoleOutput()) return;
        if(!isAbsolute() && (relativePath==null || relativeTo==null)) throw new InvalidSyntaxException("output is neither absolute nor relative");
        if(name==null) throw new InvalidSyntaxException("no strategy to calculate output filename is defined");
    }

    @Override
    public String toString() {
        return "Output{" + "relativeTo=" + relativeTo + ", relativePath=" + relativePath + ", absolute=" + absolute + ", prefix=" + prefix + ", suffix=" + suffix + ", name=" + name + '}';
    }
    
    public static class OutputPropertyEntry {
        Serializer.Property saxonProp;
        List<String> validValuesList;
        public OutputPropertyEntry(Serializer.Property saxonProp, String... values) {
            super();
            this.saxonProp=saxonProp;
            if(values!=null && values.length>0) {
                Arrays.sort(values);
                validValuesList = Arrays.asList(values);
            }
        }
        public Serializer.Property getSaxonProperty() { return saxonProp; }
        public boolean isValueValid(String value) {
            if(validValuesList==null || validValuesList.isEmpty()) return true;
            if(value==null) return false;
            return validValuesList.contains(value);
        }
    }
    public abstract class OutputProperties extends Properties {
        public abstract Object defineProperty(String key, String value) throws InvalidSyntaxException;
    }
    public String toString(final String prefix) {
        StringBuilder sb = new StringBuilder();
        sb.append(prefix).append(toString());
        return sb.toString();
    }

    /**
     * Returns the console to use.
     * @return "out", "err" or null
     */
    public String getConsole() {
        return console;
    }

    /**
     * Defines which console to use.
     * @param console Only "out", "err" and null are valid values
     * @throws InvalidSyntaxException If console is not a valid value.
     */
    public void setConsole(String console) throws InvalidSyntaxException {
        if(!"out".equals(console) && !"err".equals(console) && console!=null) {
            throw new InvalidSyntaxException("Only out, err and null are valid values for console");
        }
        this.console = console;
    }
    public boolean isConsoleOutput() { return getConsole()!=null; }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
    
}

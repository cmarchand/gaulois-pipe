/**
 * This Source Code Form is subject to the terms of 
 * the Mozilla Public License, v. 2.0. If a copy of 
 * the MPL was not distributed with this file, You 
 * can obtain one at https://mozilla.org/MPL/2.0/.
 */
package top.marchand.xml.gaulois.config.typing;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.HashMap;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import net.sf.saxon.Configuration;
import net.sf.saxon.lib.ConversionRules;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.XPathSelector;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.StringConverter;
import net.sf.saxon.type.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tha factory that generates datatypes, used to parse values
 * @author cmarchand
 */
public class DatatypeFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatatypeFactory.class);
    public static final String NS_XSD = "http://www.w3.org/2001/XMLSchema";
    public final Datatype XS_STRING;
    private HashMap<QName,Datatype> constructed = new HashMap<>();
    private final BuiltInAtomicType[] ATOMIC_TYPES = {
        BuiltInAtomicType.ANY_ATOMIC, BuiltInAtomicType.ANY_URI,
        BuiltInAtomicType.BASE64_BINARY, BuiltInAtomicType.BOOLEAN,
        BuiltInAtomicType.BYTE, BuiltInAtomicType.DATE,
        BuiltInAtomicType.DATE_TIME, BuiltInAtomicType.DATE_TIME_STAMP,
        BuiltInAtomicType.DAY_TIME_DURATION, BuiltInAtomicType.DECIMAL,
        BuiltInAtomicType.DOUBLE, BuiltInAtomicType.DURATION,
        BuiltInAtomicType.ENTITY, BuiltInAtomicType.FLOAT,
        BuiltInAtomicType.G_DAY, BuiltInAtomicType.G_MONTH,
        BuiltInAtomicType.G_MONTH_DAY, BuiltInAtomicType.G_YEAR,
        BuiltInAtomicType.G_YEAR_MONTH, BuiltInAtomicType.HEX_BINARY,
        BuiltInAtomicType.ID, BuiltInAtomicType.IDREF,
        BuiltInAtomicType.INT, BuiltInAtomicType.INTEGER,
        BuiltInAtomicType.LANGUAGE, BuiltInAtomicType.LONG,
        BuiltInAtomicType.NAME, BuiltInAtomicType.NCNAME,
        BuiltInAtomicType.NEGATIVE_INTEGER, BuiltInAtomicType.NMTOKEN,
        BuiltInAtomicType.NON_NEGATIVE_INTEGER, BuiltInAtomicType.NON_POSITIVE_INTEGER,
        BuiltInAtomicType.NORMALIZED_STRING, BuiltInAtomicType.NOTATION,
        BuiltInAtomicType.POSITIVE_INTEGER, BuiltInAtomicType.QNAME,
        BuiltInAtomicType.SHORT, BuiltInAtomicType.STRING,
        BuiltInAtomicType.TIME, BuiltInAtomicType.TOKEN,
        BuiltInAtomicType.UNSIGNED_BYTE, BuiltInAtomicType.UNSIGNED_INT,
        BuiltInAtomicType.UNSIGNED_LONG, BuiltInAtomicType.UNSIGNED_SHORT,
        BuiltInAtomicType.UNTYPED_ATOMIC, BuiltInAtomicType.YEAR_MONTH_DURATION
    };
    private final ConversionRules conversionRules;
    
    @SuppressWarnings("OverridableMethodCallInConstructor")
    private DatatypeFactory(ConversionRules conversionRules) throws ValidationException {
        super();
        this.constructed = new HashMap<>();
        this.conversionRules = conversionRules;
        XS_STRING = getDatatype(new QName(NS_XSD,"string"));
    }
    public Datatype getDatatype(QName qn) throws ValidationException {
        Datatype ret = constructed.get(qn);
        if(ret==null) {
            ret = constructDatatype(qn);
            constructed.put(qn, ret);
        }
        return ret;
    }
    
    protected Datatype constructDatatype(final QName qn) throws ValidationException {
        String localName = qn.getLocalName();
        final boolean isAtomic = NS_XSD.equals(qn.getNamespaceURI());
        final boolean allowsMultiple = localName.endsWith("+") | localName.endsWith("*");
        final boolean allowsEmpty = localName.endsWith("?") | localName.endsWith("*");
        if(isAtomic) {
            return constructAtomicDatatype(qn, allowsEmpty, allowsMultiple);
        } else {
            return constructNodeDatatype( qn, allowsEmpty, allowsMultiple);
        }
    }
    private Datatype constructAtomicDatatype(final QName qn, final boolean allowsEmpty, final boolean allowsMultiple) {
        String localName = qn.getLocalName();
        StructuredQName baseType = new StructuredQName(qn.getPrefix(), qn.getNamespaceURI(), (allowsMultiple|allowsEmpty) ? localName.substring(0, localName.length()-1) : localName);
        BuiltInAtomicType theType = null;
        for(BuiltInAtomicType type:ATOMIC_TYPES) {
            if(type.getStructuredQName().equals(baseType)) {
                theType = type;
                break;
            }
        }
        if(theType==null) {
            throw new IllegalArgumentException("Unable to determine datatype of "+qn.getEQName());
        }
        final StringConverter converter = theType.getStringConverter(conversionRules);
        return new Datatype() {
            @Override
            public boolean isAtomic() { return true; }
            @Override
            public boolean allowsMultiple() { return allowsMultiple; }
            @Override
            public boolean allowsEmpty() { return allowsEmpty; }
            @Override
            public XdmValue convert(String input, Configuration configuration) throws ValidationException {
                if(input==null && allowsEmpty()) return XdmValue.wrap(null);
                else if(input==null) {
                    throw new ValidationException(qn.toString()+" does not allow empty sequence", new Exception());
                }
                if(allowsMultiple()) {
                    String sValue = input.trim();
                    if(sValue.startsWith("(") && sValue.endsWith(")")) {
                        sValue = sValue.substring(1, sValue.length()-1);
                    }
                    String[] values = sValue.split("[ ]*,[ ]*");
                    if(values.length>1) {
                        XdmValue xValue = null;
                        // certainly could be optimized, but I do not expect having 1000 values in a parameter definition...
                        for(String value:values) {
                            XdmValue ret = XdmValue.wrap(converter.convertString(value).asAtomic());
                            if(xValue==null) {
                                xValue = ret;
                            } else {
                                xValue= xValue.append(ret);
                            }
                        }
                        return xValue;
                    } else if(values.length==1) {
                        return XdmValue.wrap(converter.convertString(values[0]).asAtomic());
                    } else {
                        throw new ValidationException("can not cast "+input+" to "+qn, new Exception());
                    }
                } else {
                    return XdmValue.wrap(converter.convertString(input).asAtomic());
                }
            }
        };
    }
    
    /**
     * Here, only <tt>element()</tt> and <tt>document</tt> are supported.
     * @param qn
     * @param allowsEmpty
     * @param allowsMultiple
     * @return A document fragment
     */
    private Datatype constructNodeDatatype(final QName qn, final boolean allowsEmpty, final boolean allowsMultiple) throws ValidationException {
        String localName = qn.getLocalName();
        String typeName = (allowsEmpty | allowsMultiple) ? localName.substring(0, localName.length()) : localName;
        typeName = typeName.trim();
        typeName= typeName.replaceAll("[ ]*\\(", "(");
        if(typeName.startsWith("element(")) {
            return constructElementParserDatatype(qn, allowsEmpty, allowsMultiple);
        } else if(typeName.startsWith("document(")) {
            return constructDocumentParserDatatype(qn, allowsEmpty, allowsMultiple);
        } else {
            throw new ValidationException("Only document() and element() are supported for node types", new Exception());
        }
    }
    private Datatype constructElementParserDatatype(final QName qn, final boolean allowsEmpty, final boolean allowsMultiple) throws ValidationException {
        return new Datatype() {
            @Override
            public boolean isAtomic() { return false; }
            @Override
            public boolean allowsMultiple() { return allowsMultiple; }
            @Override
            public boolean allowsEmpty() { return allowsEmpty; }
            @Override
            public XdmValue convert(String input, Configuration configuration) throws ValidationException {
                Processor proc = new Processor(configuration);
                DocumentBuilder builder = proc.newDocumentBuilder();
                String wrappedInput="<fake:document xmlns:fake=\"top:marchand:xml:gaulois:wrapper\">".concat(input).concat("</fake:document>");
                InputStream is = new ByteArrayInputStream(wrappedInput.getBytes(Charset.forName("UTF-8")));
                try {
                    XdmNode documentNode = builder.build(new StreamSource(is));
                    XPathCompiler compiler = proc.newXPathCompiler();
                    compiler.declareNamespace("fake", "top:marchand:xml:gaulois:wrapper");
                    XPathSelector selector = compiler.compile("/fake:document/node()").load();
                    selector.setContextItem(documentNode);
                    XdmValue ret = selector.evaluate();
                    if(ret.size()==0 && !allowsEmpty()) throw new ValidationException(qn.toString()+" does not allow empty sequence", new Exception());
                    if(ret.size()>1 && !allowsMultiple()) throw new ValidationException(qn.toString()+" does not allow sequence with more than one element", new Exception());
                    return ret;
                } catch(SaxonApiException ex) {
                    throw new ValidationException(input+" can not be casted to "+qn.toString(), new Exception());
                }
            }
        };
    }
    private Datatype constructDocumentParserDatatype(final QName qn, final boolean allowsEmpty, final boolean allowsMultiple) throws ValidationException {
        String localName= qn.getLocalName();
        if(localName.endsWith("*") || localName.endsWith("+")) {
            throw new ValidationException("Multiple documents are not allowed", new Exception());
        }
        return new Datatype() {
            @Override
            public boolean isAtomic() { return false; }
            @Override
            public boolean allowsMultiple() { return allowsMultiple; }
            @Override
            public boolean allowsEmpty() { return allowsEmpty; }
            @Override
            public XdmValue convert(String input, Configuration configuration) throws ValidationException {
                Source source = null;
                try {
                    configuration.getURIResolver().resolve(input, null);
                } catch(TransformerException ex0) {
                    InputStream is = null;
                    try {
                        URI uri = new URI(input);
                        is = uri.toURL().openStream();
                    } catch (URISyntaxException | IOException ex1) {
                        File f = new File(input);
                        if(f.exists() && f.isFile()) {
                            try {
                                is= new FileInputStream(f);
                            } catch (FileNotFoundException ex2) {
                                // can not happens, checked before
                            }
                        }
                    }
                    if(is==null) {
                        throw new ValidationException("Unable to load document "+input, new Exception());
                    }
                    source = new StreamSource(is);
                }
                DocumentBuilder builder = new Processor(configuration).newDocumentBuilder();
                try {
                    return builder.build(source);
                } catch (SaxonApiException ex) {
                    throw new ValidationException("Unable to load document "+input,ex);
                }
            }
        };
    }
    public static DatatypeFactory getInstance(Configuration configuration) throws ValidationException {
        return new DatatypeFactory(configuration.getConversionRules());
    }
    
}

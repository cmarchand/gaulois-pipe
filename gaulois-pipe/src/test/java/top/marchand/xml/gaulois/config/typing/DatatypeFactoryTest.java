/**
 * This Source Code Form is subject to the terms of 
 * the Mozilla Public License, v. 2.0. If a copy of 
 * the MPL was not distributed with this file, You 
 * can obtain one at https://mozilla.org/MPL/2.0/.
 */
package top.marchand.xml.gaulois.config.typing;

import java.math.BigInteger;
import net.sf.saxon.Configuration;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.type.ValidationException;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.BeforeClass;

/**
 * Class to test DatatypeFactory
 * @author cmarchand
 */
public class DatatypeFactoryTest {
    private static DatatypeFactory instance;
    private static Configuration saxonConfiguration;
    
    @BeforeClass
    public static void beforeClass() throws ValidationException {
        saxonConfiguration = Configuration.newConfiguration();
        instance = DatatypeFactory.getInstance(saxonConfiguration);
    }
    
    @Test
    public void getInstanceTest() throws ValidationException {
        Configuration config = Configuration.newConfiguration();
        DatatypeFactory factory = DatatypeFactory.getInstance(config);
        assertNotNull("Datatype factory instance is null", factory);
        DatatypeFactory factory2 = DatatypeFactory.getInstance(config);
        assertNotEquals("two instances return are equals", factory, factory2);
    }
    
    @Test
    public void getDatatypeXsInt() throws ValidationException {
        QName xsInt= new QName(DatatypeFactory.NS_XSD, "xs:int");
        Datatype intDT = instance.getDatatype(xsInt);
        assertFalse("xs:int allows empty sequence", intDT.allowsEmpty());
        assertFalse("xs:int allows multiple values", intDT.allowsMultiple());
        XdmValue ret = intDT.convert("4", saxonConfiguration);
        assertTrue("value is not a XdmAtomicValue", ret instanceof XdmAtomicValue);
        XdmAtomicValue atomRet = (XdmAtomicValue)ret;
        Object javaValue = atomRet.getValue();
        assertTrue("java value is not a BigInteger", javaValue instanceof BigInteger);
    }
    @Test
    public void getDatatypeXsIntEmpty() throws ValidationException {
        QName xsInt= new QName(DatatypeFactory.NS_XSD, "xs:int?");
        Datatype intDT = instance.getDatatype(xsInt);
        assertTrue("xs:int? does not allow empty sequence", intDT.allowsEmpty());
        assertFalse("xs:int? allows multiple values", intDT.allowsMultiple());
        XdmValue ret = intDT.convert("4", saxonConfiguration);
        assertTrue("value is not a XdmAtomicValue", ret instanceof XdmAtomicValue);
        ret = intDT.convert(null, saxonConfiguration);
        assertEquals("value is not an empty sequence", 0, ret.size());
    }
    @Test
    public void getDatatypeXsIntMultiple() throws ValidationException {
        QName xsInt= new QName(DatatypeFactory.NS_XSD, "xs:int+");
        Datatype intDT = instance.getDatatype(xsInt);
        assertFalse("xs:int+ allows empty sequence", intDT.allowsEmpty());
        assertTrue("xs:int? doest not allow multiple values", intDT.allowsMultiple());
        XdmValue ret = intDT.convert("4", saxonConfiguration);
        assertTrue("value is not a XdmAtomicValue", ret instanceof XdmAtomicValue);
        ret = intDT.convert("(4,5, 6 , 7 )", saxonConfiguration);
        assertEquals("value is not a sequence", 4, ret.size());
    }
    @Test(expected = ValidationException.class)
    public void getDatatypeDocumentMultiple() throws ValidationException {
        QName qn= new QName("document()*");
        instance.getDatatype(qn);
        fail("document()* is not a valid datatype");
    }
    @Test
    public void getDatatypeDocumentEmpty() throws ValidationException {
        QName qn = new QName("document()?");
        Datatype dt = instance.getDatatype(qn);
        assertTrue("Datatype for document()? does not allow empty", dt.allowsEmpty());
    }
    
}

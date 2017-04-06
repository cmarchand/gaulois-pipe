/**
 * This Source Code Form is subject to the terms of 
 * the Mozilla Public License, v. 2.0. If a copy of 
 * the MPL was not distributed with this file, You 
 * can obtain one at https://mozilla.org/MPL/2.0/.
 */
package fr.efl.chaine.xslt.config;

import fr.efl.chaine.xslt.utils.ParameterValue;
import java.util.Date;
import net.sf.saxon.Configuration;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.type.ValidationException;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;
import top.marchand.xml.gaulois.config.typing.DatatypeFactory;

/**
 *
 * @author cmarchand
 */
public class ParameterValueTest {
    private final QName QN = new QName("top:marchand:xml:gaulois-pipe", "entry");
    private static DatatypeFactory factory;
    
    @BeforeClass
    public static void beforeClass() throws ValidationException {
        Configuration configuration= Configuration.newConfiguration();
        factory = DatatypeFactory.getInstance(configuration);
    }

    @Test
    public void setStringValueTest() {
        ParameterValue pv = new ParameterValue(QN, "A simple value", factory.XS_STRING);
        assertTrue(pv.getValue() instanceof String);
    }
    
    @Test
    public void setXdmValueValueTest() throws ValidationException {
        XdmValue value = new XdmAtomicValue(true);
        QName qn_xsBoolean = new QName("xs", DatatypeFactory.NS_XSD, "boolean");
        ParameterValue pv = new ParameterValue(QN, value, factory.getDatatype(qn_xsBoolean));
        assertTrue(pv.getValue() instanceof XdmValue);
        assertEquals(((XdmValue)pv.getValue()).toString(), "true");
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void setOtherValueTest() throws IllegalArgumentException, ValidationException {
        QName qn_xsDate = new QName("xs", DatatypeFactory.NS_XSD, "dateTime");
        ParameterValue pv = new ParameterValue(QN, new Date(), factory.getDatatype(qn_xsDate));
        fail("Setting a date as value should throw a IllegalArgumentException");
    }
}

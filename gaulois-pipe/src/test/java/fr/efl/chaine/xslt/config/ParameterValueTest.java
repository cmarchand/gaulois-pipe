/**
 * This Source Code Form is subject to the terms of 
 * the Mozilla Public License, v. 2.0. If a copy of 
 * the MPL was not distributed with this file, You 
 * can obtain one at https://mozilla.org/MPL/2.0/.
 */
package fr.efl.chaine.xslt.config;

import fr.efl.chaine.xslt.utils.ParameterValue;
import java.util.Date;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmValue;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author cmarchand
 */
public class ParameterValueTest {
    private final QName QN = new QName("top:marchand:xml:gaulois-pipe", "entry");

    @Test
    public void setStringValueTest() {
        ParameterValue pv = new ParameterValue(QN, "A simple value");
        assertTrue(pv.getValue() instanceof String);
    }
    
    @Test
    public void setXdmValueValueTest() {
        XdmValue value = new XdmAtomicValue(true);
        ParameterValue pv = new ParameterValue(QN, value);
        assertTrue(pv.getValue() instanceof XdmValue);
        assertEquals(((XdmValue)pv.getValue()).toString(), "true");
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void setOtherValueTest() throws IllegalArgumentException {
        ParameterValue pv = new ParameterValue(QN, new Date());
        fail("Setting a date as value should throw a IllegalArgumentException");
    }
}

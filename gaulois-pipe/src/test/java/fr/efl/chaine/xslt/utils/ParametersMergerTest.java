/**
 * This Source Code Form is subject to the terms of 
 * the Mozilla Public License, v. 2.0. If a copy of 
 * the MPL was not distributed with this file, You 
 * can obtain one at https://mozilla.org/MPL/2.0/.
 */
package fr.efl.chaine.xslt.utils;

import java.util.HashMap;
import net.sf.saxon.s9api.QName;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Tests ParametersMerger
 * @author Christophe Marchand christophe@marchand.top
 */
public class ParametersMergerTest {
    
    @Test
    public void initialContainsBackslash() {
        String initial = "C:\\Users\\ext-cmarchand\\$[source]";
        HashMap<QName, ParameterValue> parameters = new HashMap<>();
        QName qn = new QName("source");
        parameters.put(qn, new ParameterValue(qn,"src/main/xsl"));
        String ret = ParametersMerger.processParametersReplacement(initial, parameters);
        assertEquals("C:\\Users\\ext-cmarchand\\src/main/xsl", ret);
    }
}

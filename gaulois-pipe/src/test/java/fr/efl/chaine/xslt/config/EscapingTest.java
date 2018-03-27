/**
 * This Source Code Form is subject to the terms of 
 * the Mozilla Public License, v. 2.0. If a copy of 
 * the MPL was not distributed with this file, You 
 * can obtain one at https://mozilla.org/MPL/2.0/.
 */
package fr.efl.chaine.xslt.config;

import fr.efl.chaine.xslt.GauloisPipe;
import fr.efl.chaine.xslt.InvalidSyntaxException;
import fr.efl.chaine.xslt.SaxonConfigurationFactory;
import fr.efl.chaine.xslt.utils.ParameterValue;
import java.util.HashMap;
import net.sf.saxon.Configuration;
import net.sf.saxon.s9api.QName;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author ext-cmarchand
 */
public class EscapingTest {
    private static SaxonConfigurationFactory configFactory;
    @BeforeClass 
    public static void initialize() throws InvalidSyntaxException {
        configFactory = new SaxonConfigurationFactory() {
            Configuration config = Configuration.newConfiguration();
            @Override
            public Configuration getConfiguration() {
                return config;
            }
        };
        // to have the correct URI Resolver
        new GauloisPipe(configFactory);
    }
    
    @Test
    public void escapeParameter() throws InvalidSyntaxException {
        GauloisPipe piper = new GauloisPipe(configFactory);
        ParameterValue pv = new ParameterValue(new QName("workDir"), "file:/home/cmarchand/devel/data", piper.getDatatypeFactory().XS_STRING);
        HashMap<QName,ParameterValue> params = new HashMap<>();
        params.put(pv.getKey(), pv);
        // n'importe lequel, aucune importance
        ConfigUtil cu = new ConfigUtil(configFactory.getConfiguration(), piper.getUriResolver(), "./src/test/resources/same-source-file.xml");
        String result = cu.resolveEscapes("$[workDir]/collection.xml", params);
        assertEquals("file:/home/cmarchand/devel/data/collection.xml", result);
    }

    @Test
    public void escapeXslPathWithParam() throws InvalidSyntaxException {
        GauloisPipe piper = new GauloisPipe(configFactory);
        ParameterValue pv = new ParameterValue(new QName("path"), "file:/home/cmarchand/devel/data", piper.getDatatypeFactory().XS_STRING);
        HashMap<QName,ParameterValue> params = new HashMap<>();
        params.put(pv.getKey(), pv);
        // n'importe lequel, aucune importance
        ConfigUtil cu = new ConfigUtil(configFactory.getConfiguration(), piper.getUriResolver(), "./src/test/resources/same-source-file.xml");
        String result = cu.resolveEscapes("$[path]/src/test/resources/identity.xsl", params);
        assertEquals("file:/home/cmarchand/devel/data/src/test/resources/identity.xsl", result);
    }
    /**
     * Issue #14
     * @throws InvalidSyntaxException 
     */
    @Test
    public void escapeBackSlash() throws InvalidSyntaxException {
        GauloisPipe piper = new GauloisPipe(configFactory);
        ParameterValue pv = new ParameterValue(new QName("basedir"), "c:\\dev\\sie-efl-inneo-src", piper.getDatatypeFactory().XS_STRING);
        HashMap<QName,ParameterValue> params = new HashMap<>();
        params.put(pv.getKey(), pv);
        // n'importe lequel, aucune importance
        ConfigUtil cu = new ConfigUtil(configFactory.getConfiguration(), piper.getUriResolver(), "./src/test/resources/same-source-file.xml");
        String result = cu.resolveEscapes("file:$[basedir]/src/main/CheckBc/xslt", params);
        assertEquals("Received "+result, "file:c:\\dev\\sie-efl-inneo-src/src/main/CheckBc/xslt", result);
    }
    
}

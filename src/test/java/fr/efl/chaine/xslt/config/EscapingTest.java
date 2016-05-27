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
import java.util.ArrayList;
import java.util.Collection;
import net.sf.saxon.Configuration;
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
    public static void initialize() {
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
        ParameterValue pv = new ParameterValue("workDir", "file:/home/cmarchand/devel/data");
        Collection<ParameterValue> coll = new ArrayList<>();
        coll.add(pv);
        // n'importe lequel, aucune importance
        GauloisPipe piper = new GauloisPipe(configFactory);
        ConfigUtil cu = new ConfigUtil(configFactory.getConfiguration(), "./src/test/resources/same-source-file.xml");
        String result = cu.resolveEscapes("$[workDir]/collection.xml", coll);
        assertEquals("file:/home/cmarchand/devel/data/collection.xml", result);
    }
    
}

/**
 * This Source Code Form is subject to the terms of 
 * the Mozilla Public License, v. 2.0. If a copy of 
 * the MPL was not distributed with this file, You 
 * can obtain one at https://mozilla.org/MPL/2.0/.
 */
package fr.efl.chaine.xslt;

import fr.efl.chaine.xslt.config.CfgFile;
import fr.efl.chaine.xslt.config.Config;
import fr.efl.chaine.xslt.config.ConfigUtil;
import fr.efl.chaine.xslt.utils.ParameterValue;
import java.io.File;
import java.util.HashMap;
import net.sf.saxon.Configuration;
import net.sf.saxon.s9api.QName;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author cmarchand
 */
public class ErrorManagementTest {
    private static HashMap<QName,ParameterValue> emptyInputParams;
    private static SaxonConfigurationFactory configFactory;
    
    @BeforeClass
    public static void initialize() {
        emptyInputParams = new HashMap<>();
        configFactory = new SaxonConfigurationFactory() {
            Configuration config = Configuration.newConfiguration();
            @Override
            public Configuration getConfiguration() {
                return config;
            }
        };
    }
    
    @Test
    public void testNoError() throws Exception {
        GauloisPipe piper = new GauloisPipe(configFactory);
        ConfigUtil cu = new ConfigUtil(configFactory.getConfiguration(), piper.getUriResolver(), "./src/test/resources/errorManagement.xml");
        Config config = cu.buildConfig(emptyInputParams);
        config.getSources().addFile(new CfgFile(new File("./src/test/resources/xsl/typedFunctionWithValue.xml")));
        piper.setConfig(config);
        piper.setInstanceName("ERROR_MGMT-NO_ERROR");
        piper.launch();
        assertEquals(0, piper.terminateErrorCollector());
    }
    
    @Test
    public void testError() throws Exception {
        GauloisPipe piper = new GauloisPipe(configFactory);
        ConfigUtil cu = new ConfigUtil(configFactory.getConfiguration(), piper.getUriResolver(), "./src/test/resources/errorManagement.xml");
        Config config = cu.buildConfig(emptyInputParams);
        config.getSources().addFile(new CfgFile(new File("./src/test/resources/xsl/typedFunctionWithoutValue.xml")));
        piper.setConfig(config);
        piper.setInstanceName("ERROR_MGMT-ERROR");
        piper.launch();
        assertNotEquals(0, piper.terminateErrorCollector());
    }
}

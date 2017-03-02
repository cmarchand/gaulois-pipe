/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 *
 * @author cmarchand
 */
public class ManualConstructedSourcesTest {
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

    @Test()
    public void testAddSources() throws Exception {
        GauloisPipe piper = new GauloisPipe(configFactory);
        ConfigUtil cu = new ConfigUtil(configFactory.getConfiguration(), piper.getUriResolver(), "./src/test/resources/rename.xml");
        Config config = cu.buildConfig(emptyInputParams);
        assertEquals("There should be 0 file in config file pattern match", config.getSources().getFiles().size(), 0);
        config.getSources().addFile(new CfgFile(new File("./src/test/resources/source.xml")));
        assertEquals("There should be 1 file in sources", config.getSources().getFiles().size(), 1);
        piper.setConfig(config);
        piper.setInstanceName("ADDED_SOURCES");
        piper.launch();
        assertEquals(true, new File("target/generated-test-files/source-renamed.xml").exists());
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.efl.chaine.xslt.config;

import fr.efl.chaine.xslt.GauloisPipe;
import fr.efl.chaine.xslt.InvalidSyntaxException;
import fr.efl.chaine.xslt.SaxonConfigurationFactory;
import fr.efl.chaine.xslt.utils.ParameterValue;
import java.util.ArrayList;
import java.util.Collection;
import net.sf.saxon.Configuration;
import net.sf.saxon.s9api.SaxonApiException;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author cmarchand
 */
public class ConfigUtilTest {
    private static SaxonConfigurationFactory configFactory;
    private static Collection<ParameterValue> emptyInputParams;

    @BeforeClass
    public static void initialize() {
        emptyInputParams = new ArrayList<>();
        configFactory = new SaxonConfigurationFactory() {
            Configuration config = Configuration.newConfiguration();
            @Override
            public Configuration getConfiguration() {
                return config;
            }
        };
    }
    
    @Test
    public void testCpConfigFile() throws InvalidSyntaxException, SaxonApiException {
        GauloisPipe piper = new GauloisPipe(configFactory);
        ConfigUtil cu = new ConfigUtil(configFactory.getConfiguration(), piper.getUriResolver(), "cp:/cp-xsl.xml");
        assertTrue(cu.buildConfig(emptyInputParams)!=null);
    }
    
}

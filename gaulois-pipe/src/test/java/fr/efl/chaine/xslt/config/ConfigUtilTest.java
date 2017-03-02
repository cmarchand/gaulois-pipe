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
import java.io.File;
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import net.sf.saxon.Configuration;
import net.sf.saxon.s9api.QName;
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
    private static HashMap<QName,ParameterValue> emptyInputParams;

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
    public void testCpConfigFile() throws InvalidSyntaxException, SaxonApiException {
        GauloisPipe piper = new GauloisPipe(configFactory);
        ConfigUtil cu = new ConfigUtil(configFactory.getConfiguration(), piper.getUriResolver(), "cp:/cp-xsl.xml");
        assertTrue(cu.buildConfig(emptyInputParams)!=null);
    }
    
    @Test
    public void testFindFilesRecurse() throws InvalidSyntaxException {
        GauloisPipe piper = new GauloisPipe(configFactory);
        ConfigUtil cu = new ConfigUtil(configFactory.getConfiguration(), piper.getUriResolver(), "cp:/cp-xsl.xml");
        final Pattern regex = Pattern.compile(".*\\.xml");
        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                boolean match = regex.matcher(filename).matches();
                return match;
            }
        };
        List<CfgFile> files = cu.getFilesFromDirectory(new File("src/test/resources/findDir-recurse"), filter, true);
        assertEquals("5 files are expected ", files.size(), 5);
    }
    
    @Test
    public void testSourceFolderContent() throws InvalidSyntaxException, SaxonApiException, MalformedURLException {
        GauloisPipe piper = new GauloisPipe(configFactory);
        ConfigUtil cu = new ConfigUtil(configFactory.getConfiguration(), piper.getUriResolver(), "src/test/resources/folderContentRelative.xml");
        Config config = cu.buildConfig(emptyInputParams);
        assertTrue("relative path in source folder fails",config.getSources().getFiles().size()>=34);
        
        cu = new ConfigUtil(configFactory.getConfiguration(), piper.getUriResolver(), "cp:/folderContentAbsUri.xml");
        HashMap<QName, ParameterValue> params = new HashMap<>();
        File currentPath = new File(System.getProperty("user.dir"));
        // check if we are in the right directory
        File test = new File(currentPath, "src/test/resources/folderContentAbsUri.xml");
        if(!test.exists()) {
            // probably, we are in parent folder
            currentPath = new File(currentPath, "gaulois-pipe");
            test = new File(currentPath, "src/test/resources/folderContentAbsUri.xml");
            if(!test.exists()) {
                fail("Unable to locate gaulois-pipe folder. Try moving to gaulois-pipe module folder.");
            }
        }
        QName sourceQn = new QName("source");
        params.put(sourceQn, new ParameterValue(sourceQn, currentPath.toURI().toURL().toExternalForm()));
        config = cu.buildConfig(params);
        assertTrue("absolute URI in source folder fails", config.getSources().getFiles().size()>=34);
    }
    
    
}

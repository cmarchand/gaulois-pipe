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
import java.io.File;
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import net.sf.saxon.Configuration;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.type.ValidationException;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;
import top.marchand.xml.gaulois.config.typing.DatatypeFactory;

/**
 *
 * @author cmarchand
 */
public class ConfigUtilTest {
    private static SaxonConfigurationFactory configFactory;
    private static HashMap<QName,ParameterValue> emptyInputParams;
    private static DatatypeFactory datatypeFactory;

    @BeforeClass
    public static void initialize() throws ValidationException {
        emptyInputParams = new HashMap<>();
        configFactory = new SaxonConfigurationFactory() {
            Configuration config = Configuration.newConfiguration();
            @Override
            public Configuration getConfiguration() {
                return config;
            }
        };
        datatypeFactory = DatatypeFactory.getInstance(configFactory.getConfiguration());
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
        params.put(sourceQn, new ParameterValue(sourceQn, currentPath.toURI().toURL().toExternalForm(), datatypeFactory.XS_STRING));
        config = cu.buildConfig(params);
        assertTrue("absolute URI in source folder fails", config.getSources().getFiles().size()>=34);
    }
    
    @Test(expected = InvalidSyntaxException.class)
    public void testUnboundedPrefix() throws Exception {
        String configFilename = "src/test/resources/config/unmatchedPrefixDatatype.xml";
        GauloisPipe piper = new GauloisPipe(configFactory);
        ConfigUtil cu = new ConfigUtil(configFactory.getConfiguration(), piper.getUriResolver(), configFilename);
        cu.buildConfig(emptyInputParams);
        fail("This config file must throw an Exception and it doesn't : "+configFilename);
    }
    
    @Test(expected = InvalidSyntaxException.class)
    public void testUnboundedAbstractParam() throws Exception {
        String configFilename = "src/test/resources/config/abstractParam.xml";
        GauloisPipe piper = new GauloisPipe(configFactory);
        ConfigUtil cu = new ConfigUtil(configFactory.getConfiguration(), piper.getUriResolver(), configFilename);
        Config config = cu.buildConfig(emptyInputParams);
        config.verify();
//        fail("This config file declares an abstract param that is not valued : int");
    }

    @Test()
    public void testBoundedAbstractParam() throws Exception {
        String configFilename = "src/test/resources/config/abstractParam.xml";
        GauloisPipe piper = new GauloisPipe(configFactory);
        ConfigUtil cu = new ConfigUtil(configFactory.getConfiguration(), piper.getUriResolver(), configFilename);
        HashMap<QName,ParameterValue> cliParameters = new HashMap<>();
        ParameterValue pv = new ParameterValue(new QName("int"), "8", datatypeFactory.XS_STRING);
        cliParameters.put(pv.getKey(), pv);
        Config config = cu.buildConfig(cliParameters);
        config.verify();
    }

    @Test(expected = InvalidSyntaxException.class)
    public void testBoundedAbstractParamWrongValue() throws Exception {
        String configFilename = "src/test/resources/config/abstractParam.xml";
        GauloisPipe piper = new GauloisPipe(configFactory);
        ConfigUtil cu = new ConfigUtil(configFactory.getConfiguration(), piper.getUriResolver(), configFilename);
        HashMap<QName,ParameterValue> cliParameters = new HashMap<>();
        ParameterValue pv = new ParameterValue(new QName("int"), "foe", datatypeFactory.XS_STRING);
        cliParameters.put(pv.getKey(), pv);
        Config config = cu.buildConfig(cliParameters);
        config.verify();
    }
}

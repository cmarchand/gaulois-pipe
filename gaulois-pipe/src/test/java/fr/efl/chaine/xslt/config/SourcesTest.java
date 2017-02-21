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
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import net.sf.saxon.Configuration;
import net.sf.saxon.s9api.QName;
import static org.junit.Assert.assertEquals;
import org.junit.BeforeClass;

/**
 * Created by hrolland on 24/10/2015.
 */
public class SourcesTest {
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
    public void testGetFiles() throws InvalidSyntaxException {
        Sources sources = new Sources("size", "desc");
        sources.addFiles(Arrays.asList(
                new CfgFile(new File("./src/test/resources/sources-getfiles/toto.txt")),
                new CfgFile(new File("./src/test/resources/sources-getfiles/tata.txt")),
                new CfgFile(new File("./src/test/resources/sources-getfiles/titi.txt"))
        ));
        List<CfgFile> files = sources.getFiles();
        List<String> filesInString = new ArrayList<>(files.size());
        for (CfgFile file : files) {
            filesInString.add(file.getSource().getAbsolutePath());
        }
        Assert.assertArrayEquals(
                "files mismatch",
                Arrays.asList(
                        new File("./src/test/resources/sources-getfiles/titi.txt").getAbsolutePath(),
                        new File("./src/test/resources/sources-getfiles/toto.txt").getAbsolutePath(),
                        new File("./src/test/resources/sources-getfiles/tata.txt").getAbsolutePath()
                ).toArray(),
                filesInString.toArray()
        );
    }
    
    @Test()
    public void testFolderPattern() throws Exception {
        GauloisPipe piper = new GauloisPipe(configFactory);
        ConfigUtil cu = new ConfigUtil(configFactory.getConfiguration(), piper.getUriResolver(), "./src/test/resources/sources-folder.xml");
        Config cfg = cu.buildConfig(emptyInputParams);
        assertEquals(0, cfg.getSources().getFiles().size());
    }

}

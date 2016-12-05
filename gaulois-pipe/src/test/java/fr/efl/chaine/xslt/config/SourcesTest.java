/**
 * This Source Code Form is subject to the terms of 
 * the Mozilla Public License, v. 2.0. If a copy of 
 * the MPL was not distributed with this file, You 
 * can obtain one at https://mozilla.org/MPL/2.0/.
 */
package fr.efl.chaine.xslt.config;

import fr.efl.chaine.xslt.InvalidSyntaxException;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by hrolland on 24/10/2015.
 */
public class SourcesTest {

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

}

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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import net.sf.saxon.Configuration;
import net.sf.saxon.s9api.QName;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Permet de tester le formattage de l'output
 * @author ext-cmarchand
 */
public class OutputTest {
    private Output output;
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

    
    @Before
    public void before() { output = new Output(); }
    @After
    public void after() { output = null; }
    
    @Test
    public void addValidProperties() throws InvalidSyntaxException {
        output.setOutputProperty("byte-order-mark", "yes");
        output.setOutputProperty("byte-order-mark", "no");
        output.setOutputProperty("cdata-section-elements", "{fr:efl:inneo}pNonNum");
        output.setOutputProperty("doctype-public", "about:legacy-compat");
        output.setOutputProperty("doctype-system", "");
        output.setOutputProperty("encoding", "UTF-32");
        output.setOutputProperty("escape-uri-attributes", "yes");
        output.setOutputProperty("escape-uri-attributes", "no");
        output.setOutputProperty("include-content-type", "yes");
        output.setOutputProperty("include-content-type", "no");
        output.setOutputProperty("indent", "yes");
        output.setOutputProperty("indent", "no");
        output.setOutputProperty("media-type","application/xml");
        output.setOutputProperty("method", "xml");
        output.setOutputProperty("method", "xhtml");
        output.setOutputProperty("method", "html");
        output.setOutputProperty("method", "text");
        output.setOutputProperty("normalization-form", "NFC");
        output.setOutputProperty("normalization-form", "NFD");
        output.setOutputProperty("normalization-form", "NFKC");
        output.setOutputProperty("normalization-form", "NFKD");
        output.setOutputProperty("normalization-form", "none");
        output.setOutputProperty("omit-xml-declaration", "yes");
        output.setOutputProperty("omit-xml-declaration", "no");
        output.setOutputProperty("saxon-attribute-order", "{}id");
        output.setOutputProperty("saxon-character-representation", "decimal");
        output.setOutputProperty("saxon-double-space", "{}docNiv");
        output.setOutputProperty("saxon-implicit-result-document", "ben, Je sais pas quoi mettre");
        output.setOutputProperty("saxon-indent-spaces", "4");
        output.setOutputProperty("saxon-line-length", "140");
        output.setOutputProperty("saxon-recognize-binary", "yes");
        output.setOutputProperty("saxon-recognize-binary", "no");
        output.setOutputProperty("saxon-suppress-inndentation", "{}pNonNum");
        output.setOutputProperty("standalone", "yes");
        output.setOutputProperty("standalone", "no");
        output.setOutputProperty("undeclare-prefixes", "#all");
        output.setOutputProperty("use-character-maps", "myCm,yourCm");
        output.setOutputProperty("version","1.0");
        output.setOutputProperty("version","1.1");
        // on veut juste pas d'exception
        assertTrue(true);
    }
    
    @Test(expected = InvalidSyntaxException.class)
    public void addInvalidPropertyByteOrderMark() throws InvalidSyntaxException {
        output.setOutputProperty("byte-order-mark", "pouet");
    }
    @Test(expected = InvalidSyntaxException.class)
    public void addInvalidPropertyEscapeUriAttributes() throws InvalidSyntaxException {
        output.setOutputProperty("escape-uri-attributes", "pouet");
    }
    @Test(expected = InvalidSyntaxException.class)
    public void addInvalidPropertyIncludeContentType() throws InvalidSyntaxException {
        output.setOutputProperty("include-content-type", "pouet");
    }
    @Test(expected = InvalidSyntaxException.class)
    public void addInvalidPropertyIndent() throws InvalidSyntaxException {
        output.setOutputProperty("indent", "pouet");
    }
    @Test(expected = InvalidSyntaxException.class)
    public void addInvalidPropertMethody() throws InvalidSyntaxException {
        output.setOutputProperty("method", "pouet");
    }
    @Test(expected = InvalidSyntaxException.class)
    public void addInvalidPropertyNormalizationForm() throws InvalidSyntaxException {
        output.setOutputProperty("normalization-form", "XXXX");
    }
    @Test(expected = InvalidSyntaxException.class)
    public void addInvalidPropertyOmitXmlDeclaration() throws InvalidSyntaxException {
        output.setOutputProperty("omit-xml-declaration", "pouet");
    }
    @Test(expected = InvalidSyntaxException.class)
    public void addInvalidPropertySaxonRecognizeBinary() throws InvalidSyntaxException {
        output.setOutputProperty("saxon-recognize-binary", "pouet");
    }
    @Test(expected = InvalidSyntaxException.class)
    public void addInvalidPropertyStandalone() throws InvalidSyntaxException {
        output.setOutputProperty("standalone", "pouet");
    }
    @Test(expected = InvalidSyntaxException.class)
    public void addInvalidPropertyVersion() throws InvalidSyntaxException {
        output.setOutputProperty("version","2.0");
    }
    @Test(expected = InvalidSyntaxException.class)
    public void addInvalidProperty() throws InvalidSyntaxException {
        output.setOutputProperty("pouet","yes");
    }

    @Test
    public void testIndent() throws Exception {
        // checks a Tee can be an initial Step
        GauloisPipe piper = new GauloisPipe(configFactory);
        ConfigUtil cu = new ConfigUtil(configFactory.getConfiguration(), piper.getUriResolver(), "./src/test/resources/outputParams.xml");
        Config config = cu.buildConfig(emptyInputParams);
        config.verify();
        piper.setConfig(config);
        piper.setInstanceName("OUTPUT_PARAMS");
        piper.launch();
        File expect = new File("./target/generated-test-files/source-output-indented.xml");
        assertTrue("The file target/generated-test-files/source-output-indented.xml does not exists", expect.exists());
        try (BufferedReader br = new BufferedReader(new FileReader(expect))) {
            String line = br.readLine();
            assertTrue("output is not ISO-8859-1 encoded", line.contains("ISO-8859-1"));
            assertTrue("output is not indented (1)", !line.contains("<root>"));
            line=br.readLine();
            assertEquals("output is not indented (2)", "<root>",line);
            line=br.readLine();
            assertTrue("output is not indented (3)", line.startsWith(" "));
            assertTrue("CDATA is not set in <information/>",line.contains("<![CDATA["));
            assertTrue("CDATA is not set in <information/>",line.contains("]]>"));
            line=br.readLine();
            assertEquals("problen in indentation","</root>",line);
        }
    }
}

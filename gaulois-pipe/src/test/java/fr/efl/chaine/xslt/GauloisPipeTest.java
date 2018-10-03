/**
 * This Source Code Form is subject to the terms of 
 * the Mozilla Public License, v. 2.0. If a copy of 
 * the MPL was not distributed with this file, You 
 * can obtain one at https://mozilla.org/MPL/2.0/.
 */
package fr.efl.chaine.xslt;

import fr.efl.chaine.xslt.config.Config;
import fr.efl.chaine.xslt.config.ConfigUtil;
import fr.efl.chaine.xslt.config.ParametrableStep;
import fr.efl.chaine.xslt.utils.MutableBoolean;
import fr.efl.chaine.xslt.utils.ParameterValue;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;
import net.sf.saxon.Configuration;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.XPathExecutable;
import net.sf.saxon.s9api.XPathSelector;
import net.sf.saxon.s9api.XQueryEvaluator;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.type.ValidationException;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import org.xmlresolver.Catalog;
import org.xmlresolver.Resolver;
import top.marchand.xml.gaulois.config.typing.DatatypeFactory;
import top.marchand.xml.gaulois.impl.DefaultSaxonConfigurationFactory;

/**
 *
 * @author Christophe Marchand
 */
public class GauloisPipeTest {
    private static HashMap<QName,ParameterValue> emptyInputParams;
    private static SaxonConfigurationFactory configFactory;
    private static DatatypeFactory factory;
    
    public GauloisPipeTest() {
    }
    
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
        factory = DatatypeFactory.getInstance(configFactory.getConfiguration());
    }

    @Test(expected = InvalidSyntaxException.class)
    public void testOldConfig() throws Exception {
        GauloisPipe piper = new GauloisPipe(configFactory);
        ConfigUtil cu = new ConfigUtil(configFactory.getConfiguration(), piper.getUriResolver(), "./src/test/resources/old-config.xml");
        piper.setConfig(cu.buildConfig(emptyInputParams));
        piper.setInstanceName("OLD_CONFIG");
        piper.launch();
        File expect = new File("target/generated-test-files/result.xml");
        assertTrue(expect.exists());
        assertEquals(3, piper.getXsltCacheSize());
        assertEquals(0, piper.getDocumentCacheSize());
        expect.delete();
    }
    @Test
    public void testNewConfig() throws Exception {
        GauloisPipe piper = new GauloisPipe(configFactory);
        ConfigUtil cu = new ConfigUtil(configFactory.getConfiguration(), piper.getUriResolver(), "./src/test/resources/new-config.xml");
        piper.setConfig(cu.buildConfig(emptyInputParams));
        piper.setInstanceName("NEW_CONFIG");
        piper.launch();
        File f1 = new File("target/generated-test-files/step1-output.xml");
        File f2 = new File("target/generated-test-files/step2-output.xml");
        File f3 = new File("target/generated-test-files/step3-output.xml");
        assertTrue(f1.exists());
        assertTrue(f2.exists());
        assertTrue(f3.exists());
        assertEquals(0, piper.getDocumentCacheSize());
        f1.delete();
        f2.delete();
        f3.delete();
    }
    
    @Test
    public void testSameInputFile() throws Exception {
        GauloisPipe piper = new GauloisPipe(configFactory);
        ConfigUtil cu = new ConfigUtil(configFactory.getConfiguration(), piper.getUriResolver(), "./src/test/resources/same-source-file.xml");
        Config config = cu.buildConfig(emptyInputParams);
        config.setLogFileSize(true);
        assertEquals(2, config.getSources().getFiles().size());
        piper.setConfig(config);
        piper.setInstanceName("SAME_INPUT");
        piper.launch();
        File expect = new File("target/generated-test-files/source-first-result.xml");
        assertTrue(expect.exists());
        expect.delete();
        expect = new File("target/generated-test-files/source-second-result.xml");
        assertTrue(expect.exists());
        expect.delete();
        assertEquals(1, piper.getDocumentCacheSize());
    }

    @Test
    public void testSubstitution() throws Exception {
        GauloisPipe piper = new GauloisPipe(configFactory);
        ConfigUtil cu = new ConfigUtil(configFactory.getConfiguration(), piper.getUriResolver(), "./src/test/resources/substitution.xml");
        Config config = cu.buildConfig(emptyInputParams);
        config.setLogFileSize(true);
        config.verify();
        piper.setConfig(config);
        piper.setInstanceName("SUBSTITUTION");
        piper.launch();
        File expect = new File("./target/generated-test-files/source-substitution-result.xml");
        assertTrue(expect.exists());
        expect.delete();
    }

    @Test
    public void testNoSourceFile() throws Exception {
        GauloisPipe piper = new GauloisPipe(configFactory);
        ConfigUtil cu = new ConfigUtil(configFactory.getConfiguration(), piper.getUriResolver(), "./src/test/resources/empty-source.xml");
        Config config = cu.buildConfig(emptyInputParams);
        config.setLogFileSize(true);
        config.verify();
        piper.setConfig(config);
        piper.setInstanceName("EMPTY_SOURCE");
        // on veut juste s'assurer qu'on a pas d'exception
        assertTrue(true);
        piper.launch();
    }
    @Test
    public void validateComment() throws ValidationException {
        try {
            GauloisPipe piper = new GauloisPipe(configFactory);
            Config config = new ConfigUtil(configFactory.getConfiguration(),piper.getUriResolver(), "./src/test/resources/comment-xslt.xml").buildConfig(emptyInputParams);
            config.verify();
            Iterator<ParametrableStep> it = config.getPipe().getXslts();
            int count=0;
            while(it.hasNext()) {
                it.next();
                count++;
            }
            assertEquals(3, count);
        } catch (InvalidSyntaxException | SaxonApiException ex) {
            fail(ex.getMessage());
        }
    }
    
    @Test
    public void testJavaStep() throws Exception {
        GauloisPipe piper = new GauloisPipe(configFactory);
        ConfigUtil cu = new ConfigUtil(configFactory.getConfiguration(), piper.getUriResolver(), "./src/test/resources/FileAppenderHook.xml");
        Config config = cu.buildConfig(emptyInputParams);
        config.verify();
        piper.setConfig(config);
        piper.setInstanceName("STEP_JAVA");
        File expect = new File("./target/generated-test-files/appendee.txt");
        if(expect.exists()) expect.delete();
        piper.launch();
        assertTrue("Le fichier ./target/generated-test-files/appendee.txt n'existe pas", expect.exists());
        BufferedReader reader = new BufferedReader(new FileReader(expect));
        String content = reader.readLine();
        String expectedContent = "./target/generated-test-files/source.xml";
        assertEquals("Le fichier ./target/generated-test-files/appendee.txt ne contient pas ./target/generated-test-files/source.xml", expectedContent, content);
        expect.delete();
        expect = new File("./target/generated-test-files/source-identity-result.xml");
        assertTrue("Le fichier ./target/generated-test-files/source-identity-result.xml n'existe pas", expect.exists());
        expect.delete();
        
    }
    
    @Test
    public void testTeeJava() throws Exception {
        // checks a Java can be in a Tee
        GauloisPipe piper = new GauloisPipe(configFactory);
        ConfigUtil cu = new ConfigUtil(configFactory.getConfiguration(), piper.getUriResolver(), "./src/test/resources/java-tee.xml");
        Config config = cu.buildConfig(emptyInputParams);
        config.verify();
        piper.setConfig(config);
        piper.setInstanceName("JAVA_TEE");
        File expect1 = new File("./target/generated-test-files/tee-java.txt");
        if(expect1.exists()) expect1.delete();
        File expect2 = new File("./target/generated-test-files/source-pipe1.xml");
        if(expect2.exists()) expect2.delete();
        File expect3 = new File("./target/generated-test-files/source-pipe2.xml");
        if(expect3.exists()) expect3.delete();
        piper.launch();
        assertTrue("The file ./target/generated-test-files/tee-java.txt does not exists", expect1.exists());
        assertTrue("The file ./target/generated-test-files/source-pipe1.xml does not exists", expect2.exists());
        assertTrue("The file ./target/generated-test-files/source-pipe2.xml does not exists", expect3.exists());
        if(expect1.exists()) expect1.delete();
        if(expect2.exists()) expect2.delete();
        if(expect3.exists()) expect3.delete();
    }
    
    @Test(expected = InvalidSyntaxException.class)
    public void testInitialJavaStepKo() throws Exception {
        // checks a Java can not be an initial step
        GauloisPipe piper = new GauloisPipe(configFactory);
        ConfigUtil cu = new ConfigUtil(configFactory.getConfiguration(), piper.getUriResolver(), "./src/test/resources/java-initial-step-ko.xml");
        Config config = cu.buildConfig(emptyInputParams);
        config.verify();
    }
    @Test()
    public void testInitialJavaStepOk() throws Exception {
        // checks a Java can not be an initial step
        GauloisPipe piper = new GauloisPipe(configFactory);
        ConfigUtil cu = new ConfigUtil(configFactory.getConfiguration(), piper.getUriResolver(), "./src/test/resources/java-initial-step-ok.xml");
        Config config = cu.buildConfig(emptyInputParams);
        config.verify();
    }
    @Test()
    public void testInitialTeeStep() throws Exception {
        // checks a Tee can be an initial Step
        GauloisPipe piper = new GauloisPipe(configFactory);
        ConfigUtil cu = new ConfigUtil(configFactory.getConfiguration(), piper.getUriResolver(), "./src/test/resources/tee-initial-step.xml");
        Config config = cu.buildConfig(emptyInputParams);
        config.verify();
    }
    @Test()
    public void testXslInJar() throws Exception {
        GauloisPipe piper = new GauloisPipe(configFactory);
        ConfigUtil cu = new ConfigUtil(configFactory.getConfiguration(), piper.getUriResolver(), "./src/test/resources/jar-xsl.xml");
        Config config = cu.buildConfig(emptyInputParams);
        config.verify();
        piper.setConfig(config);
        piper.setInstanceName("JAR_XSL");
        piper.launch();
        File expect = new File("./target/generated-test-files/source-jar.xml");
        assertTrue("The file target/generated-test-files/source-jar.xml does not exists", expect.exists());
        expect.delete();
    }
    
    @Test
    public void testXslInCp() throws Exception {
        GauloisPipe piper = new GauloisPipe(configFactory);
        ConfigUtil cu = new ConfigUtil(configFactory.getConfiguration(), piper.getUriResolver(), "./src/test/resources/cp-xsl.xml");
        Config config = cu.buildConfig(emptyInputParams);
        config.verify();
        assertTrue("Problem while loading XSL from classpath", true);
        piper.setConfig(config);
        piper.setInstanceName("CP_XSL");
        piper.launch();
        File expect = new File("./target/generated-test-files/source-cp.xml");
        assertTrue("The file target/generated-test-files/source-cp.xml does not exists", expect.exists());
        expect.delete();
    }
    
    @Test
    public void testNullOutput() throws Exception {
        GauloisPipe piper = new GauloisPipe(configFactory);
        ConfigUtil cu = new ConfigUtil(configFactory.getConfiguration(), piper.getUriResolver(), "./src/test/resources/null.xml");
        Config config = cu.buildConfig(emptyInputParams);
        config.verify();
        assertTrue("Output is not a null output", config.getPipe().getOutput().isNullOutput());
        piper.setConfig(config);
        piper.setInstanceName("NULL OUTPUT");
        piper.launch();
        // it's difficult to check that no file has been written anywhere...
    }
    @Test
    public void testConsoleOutput() throws Exception {
        GauloisPipe piper = new GauloisPipe(configFactory);
        ConfigUtil cu = new ConfigUtil(configFactory.getConfiguration(), piper.getUriResolver(), "./src/test/resources/console.xml");
        Config config = cu.buildConfig(emptyInputParams);
        config.verify();
        assertTrue("Output is not a null output", config.getPipe().getOutput().isConsoleOutput());
        piper.setConfig(config);
        piper.setInstanceName("CONSOLE OUTPUT");
        piper.launch();
        // it's difficult to check that no file has been written anywhere...
    }
    
    @Test(expected = InvalidSyntaxException.class)
    public void testIncompleteTrace() throws Exception {
        GauloisPipe piper = new GauloisPipe(configFactory);
        ConfigUtil cu = new ConfigUtil(configFactory.getConfiguration(), piper.getUriResolver(), "./src/test/resources/incompleteTrace.xml");
        Config config = cu.buildConfig(emptyInputParams);
    }

    @Test
    public void testTrace() throws Exception {
        GauloisPipe piper = new GauloisPipe(configFactory);
        ConfigUtil cu = new ConfigUtil(configFactory.getConfiguration(), piper.getUriResolver(), "./src/test/resources/trace.xml");
        Config config = cu.buildConfig(emptyInputParams);
        config.verify();
        piper.setConfig(config);
        piper.setInstanceName("TRACE");
        piper.launch();
        File expect = new File("./target/generated-test-files/trace.log");
        assertTrue("The file target/generated-test-files/trace. does not exists", expect.exists());
        expect.delete();
    }

    @Test
    public void testDebug() throws Exception {
        File expect = new File("debug-source.xml");
        File expect2 = new File("./target/generated-test-files/source-debug-result.xml");
        if(expect.exists()) expect.delete();
        if(expect2.exists()) expect2.delete();
        
        GauloisPipe piper = new GauloisPipe(configFactory);
        ConfigUtil cu = new ConfigUtil(configFactory.getConfiguration(), piper.getUriResolver(), "./src/test/resources/debug.xml");
        Config config = cu.buildConfig(emptyInputParams);
        config.verify();
        piper.setConfig(config);
        piper.setInstanceName("DEBUG");
        piper.launch();
        
        assertTrue("The file debug-source.xml. does not exists", expect.exists());
        assertTrue("The file target/generated-test-files/source-debug-result.xml", expect2.exists());
        expect2.delete();
    }

    @Test
    public void testChoose() throws Exception {
        GauloisPipe piper = new GauloisPipe(configFactory);
        ConfigUtil cu = new ConfigUtil(configFactory.getConfiguration(), piper.getUriResolver(), "./src/test/resources/choose.xml");
        Config config = cu.buildConfig(emptyInputParams);
        config.verify();
        piper.setConfig(config);
        piper.setInstanceName("CHOOSE");
        piper.launch();
        File expect = new File("target/generated-test-files/paye1-choose.xml");
        assertTrue("The file target/generated-test-files/paye1-choose.xml does not exists", expect.exists());
        SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
        final MutableBoolean prefixed = new MutableBoolean();
        parser.parse(expect, new DefaultHandler() {
            @Override
            public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                super.startElement(uri, localName, qName, attributes);
                // WARNING : We have a SAX1.0 parser, not namespace aware. So localName is always empty
                // MUST check on qName
                if("FileName".equals(qName)) {
                    String value=attributes.getValue("V");
                    prefixed.setValue(value.startsWith("_"));
                }
            }
        });
        assertTrue("Attribute is not prefixed, second XSL has not been run", prefixed.getValue());
        expect.delete();
        expect = new File("target/generated-test-files/paye2-choose.xml");
        assertTrue("The file target/generated-test-files/paye2-choose.xml does not exists", expect.exists());
        expect.delete();
    }
    @Test
    public void testChooseInsideTee() throws Exception {
        GauloisPipe piper = new GauloisPipe(configFactory);
        ConfigUtil cu = new ConfigUtil(configFactory.getConfiguration(), piper.getUriResolver(), "./src/test/resources/chooseInsideTee.xml");
        Config config = cu.buildConfig(emptyInputParams);
        config.verify();
        piper.setConfig(config);
        piper.setInstanceName("CHOOSE_INSIDE_TEE");
        piper.launch();
        File expect = new File("target/generated-test-files/paye1-chooseInsideTee.xml");
        assertTrue("The file target/generated-test-files/paye1-chooseInsideTee.xml does not exists", expect.exists());
        expect.delete();
    }
    @Test(expected = InvalidSyntaxException.class)
    public void testNoSource() throws Exception {
        GauloisPipe piper = new GauloisPipe(configFactory);
        ConfigUtil cu = new ConfigUtil(configFactory.getConfiguration(), piper.getUriResolver(), "./src/test/resources/no-source.xml");
        Config config = cu.buildConfig(emptyInputParams);
        config.verify();
    }
    @Test
    public void testAddAttribute() throws Exception {
        GauloisPipe piper = new GauloisPipe(configFactory);
        ConfigUtil cu = new ConfigUtil(configFactory.getConfiguration(), piper.getUriResolver(), "./src/test/resources/AddAttributeJavaStep.xml");
        Config config = cu.buildConfig(emptyInputParams);
        config.verify();
        piper.setConfig(config);
        piper.setInstanceName("ADD_ATTRIBUTE");
        piper.launch();
        Processor proc = new Processor(configFactory.getConfiguration());
        File expect = new File("target/generated-test-files/source-identity-addAttribute.xml");
        XdmNode document = proc.newDocumentBuilder().build(expect);
        XPathExecutable exec = proc.newXPathCompiler().compile("//*[@test]");
        XPathSelector selector = exec.load();
        selector.setContextItem((XdmItem)document);
        XdmValue result = selector.evaluate();
        assertTrue(result.size()>0);
        expect.delete();
    }
    
    @Test
    public void testLoadingParams() throws Exception {
        GauloisPipe piper = new GauloisPipe(configFactory);
        ConfigUtil cu = new ConfigUtil(configFactory.getConfiguration(), piper.getUriResolver(), "./src/test/resources/param-override.xml");
        ParameterValue pv = new ParameterValue(new QName("outputDirPath"), "..", factory.XS_STRING);
        HashMap<QName,ParameterValue> params = new HashMap<>();
        params.put(pv.getKey(), pv);
        Config config = cu.buildConfig(params);
        // checks that a parameter from commandLine is not overwritten by a param from config file
        assertEquals(config.getParams().get(new QName("outputDirPath")).getValue(), "..");
    }
    
    @Test
    public void testExtFunctions() throws Exception {
        Configuration config = new DefaultSaxonConfigurationFactory().getConfiguration();
        Processor proc = new Processor(config);
        XPathCompiler xpc = proc.newXPathCompiler();
        xpc.declareNamespace("ex", "top:marchand:xml:extfunctions");
        QName var = new QName("connect");
        xpc.declareVariable(var);
        XPathExecutable xpe = xpc.compile("ex:basex-query('for $i in 1 to 10 return <test>{$i}</test>',$connect)");
        assertNotNull("unable to compile extension function", xpe);
    }
    @Test
    public void testInputParams() throws Exception {
        GauloisPipe piper = new GauloisPipe(configFactory);
        ConfigUtil cu = new ConfigUtil(configFactory.getConfiguration(), piper.getUriResolver(), "./src/test/resources/showParameters.xml");
        Config config = cu.buildConfig(emptyInputParams);
        config.verify();
        piper.setConfig(config);
        piper.setInstanceName("SHOW_PARAMETERS");
        piper.launch();
        File expect = new File("target/generated-test-files/source.properties");
        assertTrue("file source.properties does not exist",expect.exists());
        Properties props = new Properties();
        props.load(new FileInputStream(expect));
        // FIX Arkamy : Following assertion KO because input-absolute contains '\' and not '/' like  input-relative-file
        //assertTrue("file:".concat(props.getProperty("input-absolute")).equals(props.getProperty("input-relative-file")));
        expect.delete();
        expect = new File("target/generated-test-files/toto11.properties");
        assertTrue("file toto11.properties does not exist",expect.exists());
        props = new Properties();
        props.load(new FileInputStream(expect));
        assertTrue("findDir-recurse/dir1/dir11/toto11.xml".equals(props.getProperty("input-relative-file")));
        assertTrue("findDir-recurse/dir1/dir11".equals(props.getProperty("input-relative-dir")));
        expect.delete();
    }

    @Test()
    public void testIssue21() throws Exception {
        GauloisPipe piper = new GauloisPipe(configFactory);
        ConfigUtil cu = new ConfigUtil(configFactory.getConfiguration(), piper.getUriResolver(), "./src/test/resources/issue21.xml");
        Config config = cu.buildConfig(emptyInputParams);
        config.verify();
        piper.setConfig(config);
        piper.setInstanceName("ISSUE_21");
        piper.launch();
        File expect = new File("target/generated-test-files/log4j-issue21.xml");
        assertTrue("file log4j-issue21.xml does not exist",expect.exists());
        expect.delete();
        expect = new File("target/generated-test-files/paye2-issue21.xml");
        assertTrue("file paye2-issue21.xml does not exist",expect.exists());
        expect.delete();
    }
    
    @Test
    public void testXdmValueToXsl() throws Exception  {
        GauloisPipe piper = new GauloisPipe(configFactory);
        ConfigUtil cu = new ConfigUtil(configFactory.getConfiguration(), piper.getUriResolver(), "./src/test/resources/paramDate.xml");
        HashMap<QName,ParameterValue> params = new HashMap<>();
        QName qnDate = new QName("date");
        // to get a date XdmValue from saxon, we must be brilliants !
        Processor proc = new Processor(configFactory.getConfiguration());
        XQueryEvaluator ev = proc.newXQueryCompiler().compile("current-dateTime()").load();
        XdmItem item = ev.evaluateSingle();
        params.put(qnDate, new ParameterValue(qnDate, item, factory.getDatatype(new QName("xs","http://www.w3.org/2001/XMLSchema","dateTime"))));
        Config config = cu.buildConfig(params);
        config.verify();
        piper.setConfig(config);
        piper.setInstanceName("XDM_VALUE_TO_XSL");
        piper.launch();
        File expect = new File("target/generated-test-files/date-output.xml");
        XdmNode document = proc.newDocumentBuilder().build(expect);
        XPathExecutable exec = proc.newXPathCompiler().compile("/date");
        XPathSelector selector = exec.load();
        selector.setContextItem((XdmItem)document);
        XdmValue result = selector.evaluate();
        assertTrue(result.size()>0);
    }
    
    @Test
    public void testFilenameWithAccent() throws Exception {
        File expect = new File("target/generated-test-files/àâäéèêëïîùûüôö.xml");
        if(expect.exists()) expect.delete();
        GauloisPipe piper = new GauloisPipe(configFactory);
        ConfigUtil cu = new ConfigUtil(configFactory.getConfiguration(), piper.getUriResolver(), "./src/test/resources/filenameWithAccent.xml");
        HashMap<QName,ParameterValue> params = new HashMap<>();
        Config config = cu.buildConfig(params);
        config.verify();
        piper.setConfig(config);
        piper.setInstanceName("FILENAME_WITH_ACCENT");
        piper.launch();
        assertTrue(expect.exists());
        expect.delete();
    }
    
    public void testStaticBaseUri() throws Exception {
        File expect = new File("target/generated-test-file/static-base-uri-ret.xml");
        if(expect.exists()) expect.delete();
        GauloisPipe piper = new GauloisPipe(configFactory);
        ConfigUtil cu = new ConfigUtil(configFactory.getConfiguration(), piper.getUriResolver(), "./src/test/resources/static-base-uri.xml");
        HashMap<QName,ParameterValue> params = new HashMap<>();
        Config config = cu.buildConfig(params);
        config.verify();
        piper.setConfig(config);
        piper.setInstanceName("STATIC-BASE-URI");
        piper.launch();
        assertTrue(expect.exists());
        Processor proc = new Processor(configFactory.getConfiguration());
        XdmNode document = proc.newDocumentBuilder().build(expect);
        XPathExecutable exec = proc.newXPathCompiler().compile("/ret/*/text()");
        XPathSelector selector = exec.load();
        selector.setContextItem((XdmItem)document);
        XdmValue result = selector.evaluate();
        String staticBaseUri = result.itemAt(0).getStringValue();
        String gpStaticBaseUri = result.itemAt(1).getStringValue();
        assertTrue("gp:staticBaseUri does not ends with target/classes/xsl/static-base-uri-ret.xml", gpStaticBaseUri.endsWith("target/classes/xsl/static-base-uri-ret.xml"));
        expect.delete();
    }
    
    @Test
    public void testIllegalXml() throws Exception {
        File expect1 = new File("target/generated-test-files/illegal-xml-notWellFormed.xml");
        File expect2 = new File("target/generated-test-files/illegal-xml-illegal-xml.xml");
        if(expect1.exists()) expect1.delete();
        if(expect2.exists()) expect2.delete();
        GauloisPipe piper = new GauloisPipe(configFactory);
        ConfigUtil cu = new ConfigUtil(configFactory.getConfiguration(), piper.getUriResolver(), "./src/test/resources/illegal-xml.xml");
        try {
            Config config = cu.buildConfig(emptyInputParams);
            config.verify();
            piper.setConfig(config);
            piper.setInstanceName("ILLEGAL-XML");
            piper.launch();
        } catch(Throwable ex) {
            ex.printStackTrace(System.err);
            throw ex;
        }
        assertTrue(expect2.getAbsolutePath()+" does not exists", expect2.exists());
        assertTrue(!expect1.exists());
        expect2.delete();
        assertEquals("It should have 1 error", 1, piper.getErrors().size());
    }
    
    @Test
    public void testXslPathWithParam() throws Exception {
        File currentDir = new File(System.getProperty("user.dir")).getAbsoluteFile();
        if(!"gaulois-pipe".equals(currentDir.getName())) {
            throw new IllegalStateException("tests must be run from gaulois-pipe directory. currentDir is "+currentDir.getAbsolutePath());
        } else {
            File oxygenProject = new File(currentDir, "gaulois-pipe.xpr");
            if(!oxygenProject.exists()) {
                // we may be in parent project directory. Try to go to gaulois-pipe subdir
                currentDir = new File(currentDir, "gaulois-pipe");
                if(!currentDir.exists() && !currentDir.isDirectory()) {
                    throw new IllegalStateException("Unable to locate gaulois-pipe sub-directory");
                }
                oxygenProject = new File(currentDir, "gaulois-pipe.xpr");
                if(!oxygenProject.exists()) {
                    throw new IllegalStateException("Unable to locate gaulois-pipe.xpr project file in "+currentDir.getAbsolutePath()+", it should not be the correct folder");
                }
            }
        }
        GauloisPipe piper = new GauloisPipe(configFactory);
        ConfigUtil cu = new ConfigUtil(configFactory.getConfiguration(), piper.getUriResolver(), "./src/test/resources/config/xslPathWithParam.xml");
        HashMap<QName,ParameterValue> params = new HashMap<>();
        ParameterValue p = new ParameterValue(new QName("path"), currentDir.toURI().toString(), factory.XS_STRING);
        params.put(p.getKey(), p);
        try {
            Config config = cu.buildConfig(params);
            config.verify();
            piper.setConfig(config);
            piper.setInstanceName("XSL-PATH-WITH-PARAM");
            piper.launch();
        } catch(Throwable ex) {
            ex.printStackTrace(System.err);
            throw ex;
        }
    }
    
    @Test
    public void testAwfulDtd() throws Exception {
        GauloisPipe piper = new GauloisPipe(configFactory) {
            @Override
            protected URIResolver buildUriResolver(URIResolver defaultUriResolver) {
                return new Resolver(new Catalog("src/test/resources/awfulDtd/awful-catalog.xml"));
            }
        };
        ConfigUtil cu = new ConfigUtil(configFactory.getConfiguration(), piper.getUriResolver(), "./src/test/resources/awfulDtd/awfulPipe.xml");
        HashMap<QName,ParameterValue> params = new HashMap<>();
        Config config = cu.buildConfig(params);
        config.verify();
        piper.setConfig(config);
        piper.setInstanceName("AWFUL_DTD");
        piper.launch();
        File expect = new File("target/generated-test-files/inputFile-awful.xml");
        assertTrue("file inputFile-awful.xml does not exist",expect.exists());
        expect.delete();
    }

    @Test
    public void testAwfulDtdExternalDocument() throws Exception {
        GauloisPipe piper = new GauloisPipe(configFactory) {
            @Override
            protected URIResolver buildUriResolver(URIResolver defaultUriResolver) {
                return new Resolver(new Catalog("src/test/resources/awfulDtd/awful-catalog.xml"));
            }
        };
        ConfigUtil cu = new ConfigUtil(configFactory.getConfiguration(), piper.getUriResolver(), "./src/test/resources/awfulDtd/awfulPipeWithDocument.xml");
        HashMap<QName,ParameterValue> params = new HashMap<>();
        Config config = cu.buildConfig(params);
        config.verify();
        piper.setConfig(config);
        piper.setInstanceName("AWFUL_DTD");
        piper.launch();
        File expect = new File("target/generated-test-files/inputFile-awful-external.xml");
        assertTrue("file inputFile-awful.xml does not exist",expect.exists());
        expect.delete();
    }
    
    @Test
    public void testSchemaUseUncompiled() throws Exception {
        // check this is run only if we have a Saxon EE configuration
        Assume.assumeTrue(configFactory.getConfiguration().getClass().getName().equals("com.saxonica.config.EnterpriseConfiguration"));

        GauloisPipe piper = new GauloisPipe(configFactory);
        ConfigUtil cu = new ConfigUtil(configFactory.getConfiguration(), piper.getUriResolver(), "./src/test/resources/EE/gp-schema-aware.xml");
        HashMap<QName,ParameterValue> params = new HashMap<>();
        Config config = cu.buildConfig(params);
        config.verify();
        piper.setConfig(config);
        piper.setInstanceName("SCHEMA-AWARE-UNCOMPILED");
        piper.launch();
        File expect = new File("target/generated-test-files/input-schema-aware-result.xml");
        assertTrue("file "+expect.getName()+" does not exist",expect.exists());
        expect.delete();
    }
    @Test
    public void testSchemaUseCompiled() throws Exception {
        // check this is run only if we have a Saxon EE configuration
        Assume.assumeTrue(configFactory.getConfiguration().getClass().getName().equals("com.saxonica.config.EnterpriseConfiguration"));

        // compile XSL
        Processor proc = new Processor(configFactory.getConfiguration());
        File xslFile = new File("src/test/resources/EE/schema-aware.xsl");
        XsltExecutable xExec = proc.newXsltCompiler().compile(new StreamSource(xslFile));
        xExec.export(new FileOutputStream(new File("target/test-classes/EE/schema-aware.sef")));
        // run pipe
        GauloisPipe piper = new GauloisPipe(configFactory);
        ConfigUtil cu = new ConfigUtil(configFactory.getConfiguration(), piper.getUriResolver(), "./src/test/resources/EE/gp-schema-aware-compiled.xml");
        HashMap<QName,ParameterValue> params = new HashMap<>();
        Config config = cu.buildConfig(params);
        config.verify();
        piper.setConfig(config);
        piper.setInstanceName("SCHEMA-AWARE-COMPILED");
        piper.launch();
        File expect = new File("target/generated-test-files/input-schema-aware-result.xml");
        assertTrue("file "+expect.getName()+" does not exist",expect.exists());
        expect.delete();
    }

    @Test()
    public void testIssue42() throws Exception {
        GauloisPipe piper = new GauloisPipe(configFactory);
        ConfigUtil cu = new ConfigUtil(configFactory.getConfiguration(), piper.getUriResolver(), "./src/test/resources/issues/42/createDirectory.xml");
        Config config = cu.buildConfig(emptyInputParams);
        File dir = new File("target/generated-test-files/non-exist-dir");
        // ensure directory does not exists
        if(dir.exists()) {
            if(dir.isFile()) {
                dir.delete();
            } else {
                Files.walkFileTree(dir.toPath(), new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }
                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        if(exc==null) {
                            Files.delete(dir);
                            return FileVisitResult.CONTINUE;
                        } else {
                            throw exc;
                        }
                    }
                });
                if(dir.exists()) Files.delete(dir.toPath());
            }
        }
        config.verify();
        piper.setConfig(config);
        piper.setInstanceName("ISSUE_42");
        piper.launch();
        assertTrue("target directory has not been created", dir.exists());
        assertTrue("target directory is not a directory", dir.isDirectory());
        File expect = new File(dir,"trace-dir-non-exist.xml");
        assertTrue("file trace-dir-non-exist.xml does not exist",expect.exists());
        expect.delete();
        File subDir = new File(dir, "result-doc");
        expect = new File(subDir, "result.xml");
        assertTrue("result-doc subdir does not exists", subDir.exists());
        assertTrue("result-doc subdir is not a directory", subDir.isDirectory());
        assertTrue("result-doc/result.xml does not exists", expect.exists());
        expect.delete();
        subDir.delete();
        dir.delete();
    }
}

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
import fr.efl.chaine.xslt.utils.ParameterValue;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import net.sf.saxon.Configuration;
import net.sf.saxon.s9api.SaxonApiException;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.BeforeClass;

/**
 *
 * @author Christophe Marchand
 */
public class GauloisPipeTest {
    private static Collection<ParameterValue> emptyInputParams;
    private static SaxonConfigurationFactory configFactory;
    
    public GauloisPipeTest() {
    }
    
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

    @Test(expected = InvalidSyntaxException.class)
    public void testOldConfig() throws Exception {
        GauloisPipe piper = new GauloisPipe(configFactory);
        ConfigUtil cu = new ConfigUtil(configFactory.getConfiguration(), piper.getUriResolver(), "./src/test/resources/old-config.xml");
        piper.setConfig(cu.buildConfig(emptyInputParams));
        piper.setInstanceName("OLD_CONFIG");
        piper.launch();
        assertEquals(true, new File("target/generated-test-files/result.xml").exists());
        assertEquals(3, piper.getXsltCacheSize());
        assertEquals(0, piper.getDocumentCacheSize());
    }
    @Test
    public void testNewConfig() throws Exception {
        GauloisPipe piper = new GauloisPipe(configFactory);
        ConfigUtil cu = new ConfigUtil(configFactory.getConfiguration(), piper.getUriResolver(), "./src/test/resources/new-config.xml");
        piper.setConfig(cu.buildConfig(emptyInputParams));
        piper.setInstanceName("NEW_CONFIG");
        piper.launch();
        assertTrue(new File("target/generated-test-files/step1-output.xml").exists());
        assertTrue(new File("target/generated-test-files/step2-output.xml").exists());
        assertTrue(new File("target/generated-test-files/step3-output.xml").exists());
        assertEquals(0, piper.getDocumentCacheSize());
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
        assertTrue(new File("target/generated-test-files/source-first-result.xml").exists());
        assertTrue(new File("target/generated-test-files/source-second-result.xml").exists());
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
        assertTrue(new File("./target/generated-test-files/source-substitution-result.xml").exists());
    }

    @Test
    public void testNoSourceFile() throws Exception {
        GauloisPipe piper = new GauloisPipe(configFactory);
        ConfigUtil cu = new ConfigUtil(configFactory.getConfiguration(), piper.getUriResolver(), "./src/test/resources/no-source.xml");
        Config config = cu.buildConfig(emptyInputParams);
        config.setLogFileSize(true);
        config.verify();
        piper.setConfig(config);
        piper.setInstanceName("NO_SOURCE");
        // on veut juste s'assurer qu'on a pas d'exception
        assertTrue(true);
        piper.launch();
    }
    @Test
    public void validateComment() {
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
        expect = new File("./target/generated-test-files/source-identity-result.xml");
        assertTrue("Le fichier ./target/generated-test-files/source-identity-result.xml n'existe pas", expect.exists());
        
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
    }

    @Test
    public void testDebug() throws Exception {
        GauloisPipe piper = new GauloisPipe(configFactory);
        ConfigUtil cu = new ConfigUtil(configFactory.getConfiguration(), piper.getUriResolver(), "./src/test/resources/debug.xml");
        Config config = cu.buildConfig(emptyInputParams);
        config.verify();
        piper.setConfig(config);
        piper.setInstanceName("DEBUG");
        piper.launch();
        File expect = new File("debug-source.xml");
        boolean exists = expect.exists();
        expect.deleteOnExit();
        assertTrue("The file debug-source.xml. does not exists", exists);
    }
}

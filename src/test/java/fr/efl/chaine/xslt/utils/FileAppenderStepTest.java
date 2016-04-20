/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.efl.chaine.xslt.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import javax.xml.transform.stream.StreamSource;
import net.sf.saxon.Configuration;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XsltTransformer;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Christophe Marchand <christophe@marchand.top>
 */
public class FileAppenderStepTest {
    
    @Test
    public void doTest() throws SaxonApiException, FileNotFoundException, IOException {
        Configuration config = Configuration.newConfiguration();
        Processor processor = new Processor(config);
        XsltTransformer transformer = processor.newXsltCompiler().compile(new StreamSource("src/test/resources/identity.xsl")).load();
        Serializer serializer = processor.newSerializer(new File("target/output.xml"));
        FileAppenderStep fas = new FileAppenderStep();
        fas.setParameter(FileAppenderStep.FILE_NAME, new XdmAtomicValue("target/appendee.txt"));
        fas.setParameter(FileAppenderStep.VALUE, new XdmAtomicValue("blablabla"));
        fas.setParameter(FileAppenderStep.LINE_SEPARATOR, new XdmAtomicValue("LF"));
        fas.setDestination(serializer);
        transformer.setDestination(fas);
        transformer.setSource(new StreamSource("src/test/resources/source.xml"));
        transformer.transform();
        File expect = new File("target/appendee.txt");
        assertTrue(expect.isFile());
        BufferedReader br = new BufferedReader(new FileReader(expect));
        char[] buff = new char[10];
        int ret = br.read(buff);
        assertEquals(10, ret);
        char[] ex = new char[] { 'b', 'l', 'a', 'b', 'l', 'a', 'b', 'l', 'a', '\n'};
        assertArrayEquals(ex, buff);
    }
    
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.efl.chaine.xslt;

import java.io.File;
import java.io.FileInputStream;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;
import net.sf.saxon.Configuration;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.Processor;
import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xmlresolver.Catalog;
import org.xmlresolver.Resolver;

/**
 *
 * @author cmarchand
 */
public class CatalogTest {
    
    @Test
    public void testCatalogSaxon() throws Exception {
        Configuration config = Configuration.newConfiguration();
        Resolver resolver = new Resolver(new Catalog("src/test/resources/awfulDtd/awful-catalog.xml"));
        config.setURIResolver(resolver);
        Processor proc = new Processor(config);
        DocumentBuilder builder = proc.newDocumentBuilder();
        XMLReader reader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
        reader.setEntityResolver(resolver);
        builder.build(new SAXSource(reader, new InputSource(new FileInputStream("src/test/resources/awfulDtd/inputFile.xml"))));
        Assert.assertTrue("Ca a planté", true);
    }
    
    @Test
    public void testCatalogXerces() throws Exception {
        Resolver resolver = new Resolver(new Catalog("src/test/resources/awfulDtd/awful-catalog.xml"));
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();
        XMLReader reader = parser.getXMLReader();
        reader.setEntityResolver(resolver);
        reader.parse(new File("src/test/resources/awfulDtd/inputFile.xml").toURI().toURL().toString());
        Assert.assertTrue("Ca a planté", true);
    }
    
}

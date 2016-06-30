/**
 * This Source Code Form is subject to the terms of 
 * the Mozilla Public License, v. 2.0. If a copy of 
 * the MPL was not distributed with this file, You 
 * can obtain one at https://mozilla.org/MPL/2.0/.
 */
package fr.efl.chaine.xslt;

import fr.efl.chaine.xslt.config.Config;
import fr.efl.chaine.xslt.config.ConfigUtil;
import fr.efl.chaine.xslt.utils.ParameterValue;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import net.sf.saxon.Configuration;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.BeforeClass;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author cmarchand
 */
public class GauloisListenerTest {
    private static Collection<ParameterValue> emptyInputParams;
    private static SaxonConfigurationFactory configFactory;

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
    public void listenerStart() throws Exception {
        GauloisPipe piper = new GauloisPipe(configFactory);
        ConfigUtil cu = new ConfigUtil(configFactory.getConfiguration(), piper.getUriResolver(), "./src/test/resources/listener/start.xml");
        Config config = cu.buildConfig(emptyInputParams);
        config.setLogFileSize(true);
        config.verify();
        assertEquals("Port escape does not work", 8123, config.getSources().getListener().getPort());
        assertEquals("STOP keyword escape does not work", "ARRETE", config.getSources().getListener().getStopKeyword());
        piper.setConfig(config);
        piper.setInstanceName("LISTENER_1");
        piper.launch();
        DefaultHttpClient httpClient = new DefaultHttpClient();
        File userDir = new File(System.getProperty("user.dir"));
        File source = new File(userDir,"src/test/resources/source.xml");
        HttpPut put = new HttpPut("http://localhost:8123/?url="+URLEncoder.encode(source.toURI().toURL().toExternalForm(), "UTF-8"));
        HttpResponse response = httpClient.execute(put);
        System.out.println(response.getStatusLine().toString());
        assertEquals(200,response.getStatusLine().getStatusCode());
        put.releaseConnection();
        // we must let GauloisPipe process submitted file, because JUnit closes since the tested method returns.
        Thread.sleep(1000);
        File outputDir = new File("target/generated-test-files");
        File target = new File(outputDir,"source-listen1.xml");
        assertTrue("File "+target.toString()+" does not exists",target.exists());
        HttpDelete delete = new HttpDelete("http://localhost:8123/?keyword=ARRETE");
        response = httpClient.execute(delete);
        System.out.println(response.getStatusLine().toString());
        assertEquals(200, response.getStatusLine().getStatusCode());
        delete.releaseConnection();
        File appendee = new File(outputDir, "listener-appendee.txt");
        assertTrue(appendee.getAbsolutePath()+" does not exists.",appendee.exists());
        String previousLine = null;
        try (BufferedReader br = new BufferedReader(new FileReader(appendee))) {
            String currentLine=br.readLine();
            while(currentLine!=null) {
                previousLine=currentLine;
                currentLine = br.readLine();
            }
        }
        assertEquals(appendee.getAbsolutePath()+" does not ends with \"EOF\"", "EOF", previousLine);
    }
}

/**
 * This Source Code Form is subject to the terms of 
 * the Mozilla Public License, v. 2.0. If a copy of 
 * the MPL was not distributed with this file, You 
 * can obtain one at https://mozilla.org/MPL/2.0/.
 */
package top.marchand.xml.gaulois.impl;

import fr.efl.chaine.xslt.SaxonConfigurationFactory;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import javax.xml.transform.stream.StreamSource;
import net.sf.saxon.Configuration;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.XPathSelector;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmSequenceIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.marchand.xml.gaulois.resolve.GauloisSAXParserFactory;

/**
 * A default SaxonConfigurationFactory that loads xpath extension functions into configuration.
 * 
 * You should provide a xml file with this structure :
 * 
 * <pre>
 * &lt;gaulois-service&gt;
 *    &lt;saxon&gt;
 *       &lt;extensions&gt;
 *          &lt;function&gt;fully.qualified.classname.that.extends.net.sf.saxon.lib.ExtensionFunctionDefinition&lt;/functiongt;
 *          &lt;functiongt;another.fully.qualified.classname&gt;
 *       &lt;/extensions&gt;
 *    &lt;/saxon&gt;
 * &lt;/gaulois-service&gt;
 * </pre>
 * @author cmarchand
 */
public class DefaultSaxonConfigurationFactory extends SaxonConfigurationFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultSaxonConfigurationFactory.class);
    protected Configuration configuration;
    
    public DefaultSaxonConfigurationFactory() {
        super();
        __initConfiguration();
    }
    
    /**
     * To be overriden, if required
     */
    protected void createConfigurationObject() {
        configuration = Configuration.newConfiguration();
    }

    @Override
    public Configuration getConfiguration() {
        return configuration;
    }
    
    private void __initConfiguration() {
        // default construct
        createConfigurationObject();
        LOGGER.debug("configuration is a "+configuration.getClass().getName());
        // issue 39
        configuration.setSourceParserClass(GauloisSAXParserFactory.class.getName());
        ClassLoader cl = getClass().getClassLoader();
        if(cl instanceof URLClassLoader) {
            Processor proc = new Processor(configuration);
            DocumentBuilder builder = proc.newDocumentBuilder();
            XPathCompiler xpCompiler = proc.newXPathCompiler();
            URLClassLoader ucl = (URLClassLoader)cl;
            try {
                for(Enumeration<URL> enumer = ucl.findResources("META-INF/services/top.marchand.xml.gaulois.xml"); enumer.hasMoreElements();) {
                    URL url = enumer.nextElement();
                    LOGGER.debug("loading service "+url.toExternalForm());
                    XdmNode document = builder.build(new StreamSource(url.openStream()));
                    XPathSelector selector = xpCompiler.compile("/gaulois-services/saxon/extensions/function").load();
                    selector.setContextItem(document);
                    XdmSequenceIterator it = selector.evaluate().iterator();
                    while(it.hasNext()) {
                        String className = it.next().getStringValue();
                        try {
                            Class clazz = Class.forName(className);
                            if(extendsClass(clazz, ExtensionFunctionDefinition.class)) {
                                Class<ExtensionFunctionDefinition> cle = (Class<ExtensionFunctionDefinition>)clazz;
                                configuration.registerExtensionFunction(cle.newInstance());
                                LOGGER.debug(className+"registered as Saxon extension function");
                            } else {
                                LOGGER.warn(className+" does not extends "+ExtensionFunctionDefinition.class.getName());
                            }
                        } catch(ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
                            LOGGER.warn("unable to load extension function "+className);
                        }
                    }
                }
            } catch(IOException | SaxonApiException ex) {
                LOGGER.error("while looking for resources in /META-INF/services/top.marchand.xml.gaulois/", ex);
            }
        }
    }
    
    private boolean extendsClass(Class toCheck, Class inheritor) {
        if(toCheck.equals(inheritor)) return true;
        if(toCheck.equals(Object.class)) return false;
        return extendsClass(toCheck.getSuperclass(), inheritor);
    }
    
}

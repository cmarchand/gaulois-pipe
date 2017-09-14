/**
 * This Source Code Form is subject to the terms of 
 * the Mozilla Public License, v. 2.0. If a copy of 
 * the MPL was not distributed with this file, You 
 * can obtain one at https://mozilla.org/MPL/2.0/.
 */
package top.marchand.xml.gaulois.config;

import fr.efl.chaine.xslt.GauloisPipe;
import fr.efl.chaine.xslt.InvalidSyntaxException;
import fr.efl.chaine.xslt.SaxonConfigurationFactory;
import fr.efl.chaine.xslt.config.Config;
import fr.efl.chaine.xslt.config.ConfigUtil;
import fr.efl.chaine.xslt.utils.ParameterValue;
import java.util.HashMap;
import net.sf.saxon.Configuration;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.ValidationException;
import org.junit.BeforeClass;
import org.junit.Test;
import top.marchand.xml.gaulois.config.typing.DatatypeFactory;

/**
 * Class to test validity of configuration schema.
 * @author cmarchand
 */
public class SchemaConfigTest {
    private static HashMap<QName,ParameterValue> inputParams;
    private static SaxonConfigurationFactory configFactory;
    
    @BeforeClass
    public static void initialize() throws ValidationException {
        configFactory = new SaxonConfigurationFactory() {
            Configuration config = Configuration.newConfiguration();
            @Override
            public Configuration getConfiguration() {
                return config;
            }
        };
        inputParams = new HashMap<>();
        DatatypeFactory factory = DatatypeFactory.getInstance(configFactory.getConfiguration());
        ParameterValue pv = new ParameterValue(new QName("p1"), "v1", factory.XS_STRING);
        inputParams.put(pv.getKey(), pv);
        pv = new ParameterValue(new QName("p2"), "v2", factory.XS_STRING);
        inputParams.put(pv.getKey(), pv);
        pv = new ParameterValue(new QName("p3"), "v3", factory.XS_STRING);
        inputParams.put(pv.getKey(), pv);
        pv = new ParameterValue(new QName("p4"), "v4", factory.XS_STRING);
        inputParams.put(pv.getKey(), pv);
    }
    
    @Test(expected = InvalidSyntaxException.class)
    public void invalidAbstractUntypedTest() throws InvalidSyntaxException, SaxonApiException {
        GauloisPipe piper = new GauloisPipe(configFactory);
        ConfigUtil cu = new ConfigUtil(configFactory.getConfiguration(), piper.getUriResolver(), "./src/test/resources/schemas/invalid1.xml");
        Config config = cu.buildConfig(inputParams);
        config.verify();
    }
    @Test(expected = InvalidSyntaxException.class)
    public void invalidAbstractWithValueTest() throws InvalidSyntaxException, SaxonApiException {
        GauloisPipe piper = new GauloisPipe(configFactory);
        ConfigUtil cu = new ConfigUtil(configFactory.getConfiguration(), piper.getUriResolver(), "./src/test/resources/schemas/invalid2.xml");
        Config config = cu.buildConfig(inputParams);
        config.verify();
    }
    @Test(expected = InvalidSyntaxException.class)
    public void concreteWithoutValueTest() throws InvalidSyntaxException, SaxonApiException {
        GauloisPipe piper = new GauloisPipe(configFactory);
        ConfigUtil cu = new ConfigUtil(configFactory.getConfiguration(), piper.getUriResolver(), "./src/test/resources/schemas/invalid3.xml");
        Config config = cu.buildConfig(inputParams);
        config.verify();
    }
    @Test
    public void allValidAbstractParamsTest() throws InvalidSyntaxException, SaxonApiException {
        GauloisPipe piper = new GauloisPipe(configFactory);
        ConfigUtil cu = new ConfigUtil(configFactory.getConfiguration(), piper.getUriResolver(), "./src/test/resources/schemas/valid-params.xml");
        Config config = cu.buildConfig(inputParams);
        config.verify();
    }
    @Test
    public void datatypeTest() {
        BuiltInAtomicType type = BuiltInAtomicType.DECIMAL;
        System.out.println("description -> "+type.getDescription());
        System.out.println("displayName -> "+type.getDisplayName());
        System.out.println("EQName      -> "+type.getEQName());
        System.out.println("name        -> "+type.getName());
        System.out.println("typeName    -> "+type.getTypeName().toString());
    }
}

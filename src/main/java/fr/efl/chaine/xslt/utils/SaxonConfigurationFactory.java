/**
 * This Source Code Form is subject to the terms of 
 * the Mozilla Public License, v. 2.0. If a copy of 
 * the MPL was not distributed with this file, You 
 * can obtain one at https://mozilla.org/MPL/2.0/.
 */
package fr.efl.chaine.xslt.utils;

import net.sf.saxon.Configuration;

/**
 * This class generates a Configuration.
 * Dependending on Saxon release (HE, PE or EE), it returns a 
 * Configuration, a ProfessionalConfiguration or a EnterpriseConfiguration
 * @author cmarchand
 */
public class SaxonConfigurationFactory {

    /**
     * Returns aConfiguration. Depending on the release of saxon, and the licence availability,
     * it returns a <tt>EnterpriseConfiguration</tt>, a <tt>ProfessionalConfiguration</tt>, or
     * a <tt>Configuration</tt>
     * @return 
     */
    public static Configuration buildConfiguration() {
        try {
            Class clazz = Class.forName("com.saxonica.config.EnterpriseConfiguration");
            Configuration instance = (Configuration)clazz.newInstance();
            return instance;
        } catch(ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
            try {
                Class clazz = Class.forName("com.saxonica.config.ProfessionalConfiguration");
                Configuration instance = (Configuration)clazz.newInstance();
                return instance;
            } catch(ClassNotFoundException | InstantiationException | IllegalAccessException ex2) {
                return new Configuration();
            }
        }
    }
    
}

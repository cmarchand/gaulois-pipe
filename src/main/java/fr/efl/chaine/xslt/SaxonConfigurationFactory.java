/**
 * This Source Code Form is subject to the terms of 
 * the Mozilla Public License, v. 2.0. If a copy of 
 * the MPL was not distributed with this file, You 
 * can obtain one at https://mozilla.org/MPL/2.0/.
 */
package fr.efl.chaine.xslt;

import net.sf.saxon.Configuration;

/**
 * This class is used when a {@link net.sf.saxon.Configuration} is required.
 * You must implement your own one, according to the Saxon release you use.
 * 
 * @author cmarchand
 */
public abstract class SaxonConfigurationFactory {
    
    /**
     * Returns the Configuration. Many calls to this method <strong>must</strong>
     * return the same instance of <tt>Configuration</tt>.
     * 
     * @return The Configuration to use
     */
    public abstract Configuration getConfiguration();
    
}

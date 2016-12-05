/**
 * This Source Code Form is subject to the terms of 
 * the Mozilla Public License, v. 2.0. If a copy of 
 * the MPL was not distributed with this file, You 
 * can obtain one at https://mozilla.org/MPL/2.0/.
 */
package fr.efl.chaine.xslt.config;

import fr.efl.chaine.xslt.InvalidSyntaxException;
import java.util.HashMap;
import net.sf.saxon.s9api.QName;

/**
 *
 * @author cmarchand
 */
public class Namespaces implements Verifiable {
    private final HashMap<String,String> mapping;
    public static final transient QName QNAME = new QName(Config.NS, "namespaces");
    public static final transient QName QN_MAPPING = new QName(Config.NS, "mapping");
    public static final transient QName ATTR_PREFIX = new QName("prefix");
    public static final transient QName ATTR_URI= new QName("uri");
    
    public Namespaces() {
        super();
        this.mapping = new HashMap<>();
    }

    @Override
    public void verify() throws InvalidSyntaxException { }
    
    /**
     * Returns a HashMap where key is the prefix, and value the URI.
     * @return 
     */
    public HashMap<String,String> getMappings() {
        return mapping;
    }
    
}

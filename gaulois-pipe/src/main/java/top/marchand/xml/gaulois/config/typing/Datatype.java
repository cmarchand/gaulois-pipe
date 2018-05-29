/**
 * This Source Code Form is subject to the terms of 
 * the Mozilla Public License, v. 2.0. If a copy of 
 * the MPL was not distributed with this file, You 
 * can obtain one at https://mozilla.org/MPL/2.0/.
 */
package top.marchand.xml.gaulois.config.typing;

import net.sf.saxon.Configuration;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.type.ValidationException;

/**
 * A datatype, used to represent parameters datatypes, and to parse specified values.
 * @author cmarchand
 */
public interface Datatype {
    
    /**
     * Returns true if this datatype is an atomic datatype
     * @return if this datatype represents and atomic value
     */
    boolean isAtomic();
    
    /**
     * Returns true if sequences are allowed
     * @return if multiple values are allowed (sequence with more than 1 item)
     */
    boolean allowsMultiple();
    
    /**
     * Return true if empty sequence is allowed.
     * @return if empty is allowed, if empty-sequence aloowed
     */
    boolean allowsEmpty();
    
    /**
     * Convert <tt>input</tt> as this datatype
     * @param input The input value to convert
     * @param configuration The saxon Configuration to rely on
     * @return The corresponding XdmValue
     * @throws net.sf.saxon.type.ValidationException If <tt>input</tt>is not castable into this datatype
     */
    XdmValue convert(String input, Configuration configuration) throws ValidationException;
    
}

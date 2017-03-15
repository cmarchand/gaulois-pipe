/**
 * This Source Code Form is subject to the terms of 
 * the Mozilla Public License, v. 2.0. If a copy of 
 * the MPL was not distributed with this file, You 
 * can obtain one at https://mozilla.org/MPL/2.0/.
 */
package fr.efl.chaine.xslt.utils;

import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmValue;

/**
 * A parameter value in saxon pipe.
 */
public class ParameterValue {

    /**
     * the parameter key.
     * Since issue#15, it is a QName
     */
    private final QName key;
    /**
     * the parameter value.
     */
    private Object value;

    /**
     * Default constructor.
     *
     * @param key the parameter key
     * @param value the parameter value
     */
    public ParameterValue(QName key, Object value) {
        this.key = key;
        this.value = value;
    }

    /**
     * @return the parameter key
     */
    public QName getKey() {
        return key;
    }

    /**
     * @return the parameter value
     */
    public Object getValue() {
        return value;
    }
    
    public void setValue(Object value) {
        if(value instanceof String) {
            this.value=value;
        } else if(value instanceof XdmValue) {
            this.value=value;
        } else {
            throw new IllegalArgumentException("Only String or XdmValue are acceptable values for parameters");
        }
    }

    @Override
    public String toString() {
        return "[" + getKey() + "=" + getValue() + "]";
    }

}

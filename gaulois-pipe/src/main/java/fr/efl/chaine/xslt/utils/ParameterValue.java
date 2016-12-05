/**
 * This Source Code Form is subject to the terms of 
 * the Mozilla Public License, v. 2.0. If a copy of 
 * the MPL was not distributed with this file, You 
 * can obtain one at https://mozilla.org/MPL/2.0/.
 */
package fr.efl.chaine.xslt.utils;

/**
 * A parameter value in saxon pipe.
 */
public class ParameterValue {

    /**
     * the parameter key.
     */
    private final String key;
    /**
     * the parameter value.
     */
    private String value;

    /**
     * Default constructor.
     *
     * @param key the parameter key
     * @param value the parameter value
     */
    public ParameterValue(String key, String value) {
        this.key = key;
        this.value = value;
    }

    /**
     * @return the parameter key
     */
    public String getKey() {
        return key;
    }

    /**
     * @return the parameter value
     */
    public String getValue() {
        return value;
    }
    
    public void setValue(String value) {
        this.value=value;
    }

    @Override
    public String toString() {
        return "[" + getKey() + "=" + getValue() + "]";
    }

}

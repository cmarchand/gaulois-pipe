/**
 * This Source Code Form is subject to the terms of 
 * the Mozilla Public License, v. 2.0. If a copy of 
 * the MPL was not distributed with this file, You 
 * can obtain one at https://mozilla.org/MPL/2.0/.
 */
package fr.efl.chaine.xslt.utils;

import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmValue;
import top.marchand.xml.gaulois.config.typing.Datatype;

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
     * This parameter datatype. If not specified, it's xs:string
     */
    private Datatype datatype;
    private boolean abstractParam;

    /**
     * Default constructor.
     *
     * @param key the parameter key
     * @param value the parameter value
     * @param datatype the parameter datatype
     */
    @SuppressWarnings("OverridableMethodCallInConstructor")
    public ParameterValue(QName key, Object value, Datatype datatype) {
        this.key = key;
        setValue(value);
        setDatatype(datatype);
    }

    /**
     * Constructs an abstract Parameter
     * @param key
     * @param datatype 
     */
    @SuppressWarnings("OverridableMethodCallInConstructor")
    public ParameterValue(QName key, Datatype datatype) {
        this.key = key;
        setDatatype(datatype);
        abstractParam = true;
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
            abstractParam = false;
        } else if(value instanceof XdmValue) {
            this.value=value;
            abstractParam = false;
        } else if(value==null) {
            this.value=value;
            abstractParam = false;
        } else {
            throw new IllegalArgumentException("Only String or XdmValue are acceptable values for parameters");
        }
    }

    @Override
    public String toString() {
        return "[" + getKey() + "=" + (abstractParam ? "<abstract>" : getValue()) + "]";
    }

    public Datatype getDatatype() {
        return datatype;
    }

    public void setDatatype(Datatype datatype) {
        this.datatype = datatype;
    }
    
    public boolean isAbstract() {
        return abstractParam;
    }

}

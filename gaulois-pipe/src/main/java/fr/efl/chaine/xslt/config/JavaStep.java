/**
 * This Source Code Form is subject to the terms of 
 * the Mozilla Public License, v. 2.0. If a copy of 
 * the MPL was not distributed with this file, You 
 * can obtain one at https://mozilla.org/MPL/2.0/.
 */
package fr.efl.chaine.xslt.config;

import fr.efl.chaine.xslt.InvalidSyntaxException;
import fr.efl.chaine.xslt.StepJava;
import fr.efl.chaine.xslt.utils.ParameterValue;
import java.util.Collection;
import java.util.HashMap;
import net.sf.saxon.s9api.QName;

/**
 * A java step.
 * @author cmarchand
 */
public class JavaStep implements ParametrableStep {
    static final QName QNAME = new QName(Config.NS, "java");
    static final QName ATTR_CLASS = new QName("class");
    private String className;
    private HashMap<QName,ParameterValue> params;
    private Class clazz;
    
    public JavaStep() {
        super();
        params = new HashMap<>();
    }
    public JavaStep(String className) {
        this();
        this.className=className;
    }

    @Override
    public Collection<ParameterValue> getParams() {
        return params.values();
    }

    @Override
    public void addParameter(ParameterValue param) {
        if(param!=null) {
            params.put(param.getKey(), param);
        }
    }

    @Override
    public void verify() throws InvalidSyntaxException {
        try {
            clazz = Class.forName(className);
            if(!isDerivedFrom(clazz, StepJava.class)) {
                throw new InvalidSyntaxException("Class "+className+" do not extends "+StepJava.class.getName());
            }
        } catch(ClassNotFoundException | InvalidSyntaxException ex) {
            throw new InvalidSyntaxException(ex);
        }
    }
    
    public Class<StepJava> getStepClass() {
        return clazz;
    }
    
    /**
     * Returns true if {@link toTest} is or extends {@link isA}
     * @param toTest
     * @param isA
     * @return <tt>true</tt> if the <tt>toTest</tt> class extends the <tt>isA</tt> class.
     */
    private static boolean isDerivedFrom(Class toTest, Class isA) {
        Class hierarch = toTest;
        while(hierarch!=null && !hierarch.equals(isA)) {
            hierarch = hierarch.getSuperclass();
        }
        return hierarch!=null;
    }

    @Override
    public String toString(String prefix) {
        StringBuilder sb = new StringBuilder();
        sb.append(prefix).append("java class=").append(className).append("\n");
        return sb.toString();
    }

    @Override
    public String toString() {
        return toString("");
    }
    
    
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.efl.chaine.xslt.config;

import fr.efl.chaine.xslt.InvalidSyntaxException;
import fr.efl.chaine.xslt.StepJava;
import fr.efl.chaine.xslt.utils.ParameterValue;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import net.sf.saxon.s9api.QName;

/**
 *
 * @author cmarchand
 */
public class JavaStep implements ParametrableStep {
    static final QName QNAME = new QName(Config.NS, "java");
    static final QName ATTR_HREF = new QName("class");
    private String className;
    private HashMap<String,ParameterValue> params;
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
        params.put(param.getKey(), param);
    }

    @Override
    public void verify() throws InvalidSyntaxException {
        try {
            clazz = Class.forName(className);
            if(!isDerivedFrom(clazz, StepJava.class)) {
                throw new InvalidSyntaxException("La classe "+className+" n'implémente pas "+StepJava.class.getName());
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
     * @return 
     */
    private static boolean isDerivedFrom(Class toTest, Class isA) {
        Class hierarch = toTest;
        while(hierarch!=null && !hierarch.equals(isA)) {
            hierarch = hierarch.getSuperclass();
        }
        return hierarch!=null;
    }
    
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.efl.chaine.xslt.config;

import fr.efl.chaine.xslt.InvalidSyntaxException;
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
            if(!Arrays.asList(clazz.getInterfaces()).contains(/* TODO : mettre la bonne classe */)) {
                throw new InvalidSyntaxException("La classe "+className+" n'implémente pas "+)
            }
        } catch(Exception ex) {
            throw new InvalidSyntaxException(ex);
        }
    }
    
}

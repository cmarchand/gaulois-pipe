/**
 * This Source Code Form is subject to the terms of 
 * the Mozilla Public License, v. 2.0. If a copy of 
 * the MPL was not distributed with this file, You 
 * can obtain one at https://mozilla.org/MPL/2.0/.
 */
package fr.efl.chaine.xslt.config;

import fr.efl.chaine.xslt.InvalidSyntaxException;
import fr.efl.chaine.xslt.utils.ParameterValue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.sf.saxon.s9api.QName;

/**
 *
 * @author cmarchand
 */
public class WhenEntry implements ParametrableStep {
    public static final QName QNAME = new QName(Config.NS, "when");
    public static final QName QN_OTHERWISE = new QName(Config.NS, "otherwise");
    public static final QName ATTR_TEST = new QName("test");
    private String test;
    private final List<ParametrableStep> steps;
    
    public WhenEntry() {
        steps = new ArrayList<>();
    }
    
    public WhenEntry(final String test) {
        this();
        this.test=test;
    }

    public String getTest() {
        return test;
    }

    public List<ParametrableStep> getSteps() {
        return steps;
    }
    
    public void addStep(ParametrableStep step) {
        steps.add(step);
    }

    /**
     * Always return null, a When can not have parameters.
     * @return {@code null}
     */
    @Override
    public Collection<ParameterValue> getParams() {
        return null;
    }

    /**
     * Does nothing, a When  can not have parameters.
     * @param param Ignored
     */
    @Override
    public void addParameter(ParameterValue param) { }

    @Override
    public String toString(String prefix) {
        StringBuilder sb = new StringBuilder();
        sb.append(prefix).append("when test=\"").append(test).append("\"\n");
        String _p = prefix+"  ";
        for(ParametrableStep step:steps) {
            step.toString(_p);
        }
        return sb.toString();
    }

    @Override
    public void verify() throws InvalidSyntaxException {
        if(test==null || test.length()==0) {
            throw new InvalidSyntaxException("when/@test must be a XPath expression that returns null");
        }
        for(ParametrableStep step:steps) {
            step.verify();
        }
    }
}

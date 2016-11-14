/**
 * This Source Code Form is subject to the terms of 
 * the Mozilla Public License, v. 2.0. If a copy of 
 * the MPL was not distributed with this file, You 
 * can obtain one at https://mozilla.org/MPL/2.0/.
 */
package fr.efl.chaine.xslt.utils;

import java.util.ArrayList;
import java.util.List;
import net.sf.saxon.s9api.XsltExecutable;

/**
 * Used to carry a XsltExecutable and parameters to use on
 * @author ext-cmarchand
 */
public class XsltExecutableCarrier {
    private final XsltExecutable executable;
    private final List<ParameterValue> parameters;
    
    public XsltExecutableCarrier(XsltExecutable executable) {
        super();
        this.executable = executable;
        parameters = new ArrayList<>();
    }

    public XsltExecutable getExecutable() {
        return executable;
    }

    public List<ParameterValue> getParameters() {
        return parameters;
    }
    public void addParameter(ParameterValue p) {
        if(p==null) return;
        parameters.add(p);
    }
    
}

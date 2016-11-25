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
public class ChooseStep implements ParametrableStep {
    private final List<WhenEntry> conditions;
    public static final QName QNAME = new QName(Config.NS, "choose");
    
    public ChooseStep() {
        super();
        conditions = new ArrayList<>();
    }

    /**
     * Always return null, a step may not have parameters.
     * @return {@code null}
     */
    @Override
    public Collection<ParameterValue> getParams() {
        return null;
    }
    
    public List<WhenEntry> getConditions() {
        return conditions;
    }
    public void addWhen(WhenEntry when) {
        conditions.add(when);
    }

    /**
     * Does nothing, a ChooseStep may not have parameters
     * @param param Ignored
     */
    @Override
    public void addParameter(ParameterValue param) { }

    @Override
    public String toString(String prefix) {
        StringBuilder sb = new StringBuilder();
        sb.append(prefix).append("choose\n");
        String _p=prefix+"  ";
        for(WhenEntry when:conditions) {
            sb.append(when.toString(_p));
        }
        return sb.toString();
    }

    @Override
    public void verify() throws InvalidSyntaxException {
        if(conditions.isEmpty()) {
            throw new InvalidSyntaxException("a choose can not be empty.");
        }
        for(WhenEntry when: conditions)
            when.verify();
    }
}

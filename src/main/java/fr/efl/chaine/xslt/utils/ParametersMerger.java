/**
 * This Source Code Form is subject to the terms of 
 * the Mozilla Public License, v. 2.0. If a copy of 
 * the MPL was not distributed with this file, You 
 * can obtain one at https://mozilla.org/MPL/2.0/.
 */
package fr.efl.chaine.xslt.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author Christophe Marchand
 */
public class ParametersMerger {

    /**
     * Merges the two lists into a new list, and let the two original lists unchanged.
     * @param l1
     * @param l2
     * @return A new list taht contains all elements from <tt>l1</tt> and <tt>l2</tt>
     */
    public static List<ParameterValue> merge(final Collection<ParameterValue> l1, final Collection<ParameterValue> l2) {
        ArrayList<ParameterValue> ret = new ArrayList<>(l1.size()+l2.size());
        ret.addAll(l2);
        ret.addAll(l1);
        // on fait les éventuelles substitutions, pour ne les faire qu'une fois.
        for(ParameterValue pv:ret) {
            pv.setValue(processParametersReplacement(pv.getValue(), ret));
        }
        return ret;
    }
    
    public static String processParametersReplacement(String initialValue, final Collection<ParameterValue> parameters) {
        String ret = initialValue;
        if(ret.contains("$[")) {
            for(ParameterValue pv: parameters) {
                ret = ret.replaceAll("\\$\\["+pv.getKey()+"\\]", pv.getValue());
                if(!ret.contains("$[")) break;
            }
        }
        return ret;
    }
    
}

/**
 * This Source Code Form is subject to the terms of 
 * the Mozilla Public License, v. 2.0. If a copy of 
 * the MPL was not distributed with this file, You 
 * can obtain one at https://mozilla.org/MPL/2.0/.
 */
package fr.efl.chaine.xslt.utils;

import java.io.File;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Merges parameters
 * @author Christophe Marchand
 */
public class ParametersMerger {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParametersMerger.class);

    /**
     * Merges the two lists into a new list, and let the two original lists unchanged.
     * @param highPriority First list of parameters
     * @param lowPriority Second list of parameters
     * @return A new list that contains all elements from <tt>l1</tt> and <tt>l2</tt>
     */
    public static HashMap<String,ParameterValue> merge(final HashMap<String,ParameterValue> highPriority, final HashMap<String,ParameterValue> lowPriority) {
//        LOGGER.debug("merging : "+highPriority+" with "+lowPriority);
        HashMap<String,ParameterValue> ret = new HashMap<>();
        ret.putAll(highPriority);
        for(ParameterValue pv: lowPriority.values()) {
            if(!ret.containsKey(pv.getKey()))
                ret.put(pv.getKey(), pv);
        }
        // on fait les éventuelles substitutions, pour ne les faire qu'une fois.
        for(ParameterValue pv:ret.values()) {
            pv.setValue(processParametersReplacement(pv.getValue(), ret));
        }
        return ret;
    }
    
    /**
     * Replaces the parameters in string
     * @param initialValue The String to change parameters in
     * @param parameters The parameters values
     * @return The initialValue with all parameters replaced
     */
    public static String processParametersReplacement(String initialValue, final HashMap<String,ParameterValue> parameters) {
        String ret = initialValue;
        if(ret.contains("$[")) {
            for(ParameterValue pv: parameters.values()) {
                ret = ret.replaceAll("\\$\\["+pv.getKey()+"\\]", pv.getValue());
                if(!ret.contains("$[")) break;
            }
        }
        return ret;
    }
    /**
     * Replaces the parameters in string
     * @param parameters The parameters values
     * @param inputFile The input file actually processed. input-basename, input-name and input-extension are added, so can be used.
     * 
     * @return The parameters whith inpu-name, input-basename and input-extension added
     */
    public static HashMap<String,ParameterValue> addInputInParameters(final HashMap<String,ParameterValue> parameters, final File inputFile) {
        HashMap<String,ParameterValue> fileParams = new HashMap<>();
        String name = inputFile.getName();
        String basename = name.substring(0, name.lastIndexOf("."));
        String extension = name.substring(basename.length()+1);
        fileParams.put("input-basename", new ParameterValue("input-basename", basename));
        fileParams.put("input-name", new ParameterValue("input-name", name));
        fileParams.put("input-extension", new ParameterValue("input-extension", extension));
        return merge(parameters, fileParams);
    }
}

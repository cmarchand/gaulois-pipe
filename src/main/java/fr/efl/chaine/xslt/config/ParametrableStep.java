/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.efl.chaine.xslt.config;

import fr.efl.chaine.xslt.utils.ParameterValue;
import java.util.Collection;

/**
 * A step which can have parameters
 * @author cmarchand
 */
public interface ParametrableStep extends Verifiable {
    
    /**
     * Returns the parameters 
     * @return The parameters
     */
    Collection<ParameterValue> getParams();
    
    /**
     * Adds a parameter
     * @param param the parameter to add. <tt>param</tt> may be null.
     */
    void addParameter(ParameterValue param);
    
    /**
     * A tostring() with a prefix on each line
     * @param prefix The prefix to use on each line
     * @return The formal description, with a prefix
     */
    public String toString(final String prefix);
}

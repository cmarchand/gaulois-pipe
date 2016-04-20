/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.efl.chaine.xslt.config;

import fr.efl.chaine.xslt.utils.ParameterValue;
import java.util.Collection;

/**
 *
 * @author cmarchand
 */
public interface ParametrableStep extends Verifiable {
    
    /**
     * Returns the parameters 
     * @return 
     */
    Collection<ParameterValue> getParams();
    
    /**
     * Adds a parameter
     * @param param 
     */
    void addParameter(ParameterValue param);
}

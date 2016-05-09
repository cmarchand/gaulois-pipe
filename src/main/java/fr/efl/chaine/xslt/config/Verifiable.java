/**
 * This Source Code Form is subject to the terms of 
 * the Mozilla Public License, v. 2.0. If a copy of 
 * the MPL was not distributed with this file, You 
 * can obtain one at https://mozilla.org/MPL/2.0/.
 */
package fr.efl.chaine.xslt.config;

import fr.efl.chaine.xslt.InvalidSyntaxException;

/**
 * Quelque chose qui peut être vérifié
 * @author ext-cmarchand
 */
public interface Verifiable {
    /**
     * Réalise les vérifications nécessaires sur l'instance, et jette une exception en cas de problème
     * @throws InvalidSyntaxException If something is wrong
     */
    void verify() throws InvalidSyntaxException;
    
}

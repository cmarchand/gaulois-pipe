/**
 * This Source Code Form is subject to the terms of 
 * the Mozilla Public License, v. 2.0. If a copy of 
 * the MPL was not distributed with this file, You 
 * can obtain one at https://mozilla.org/MPL/2.0/.
 */
package fr.efl.chaine.xslt;

/**
 * An exception that materialize an invlaid syntax in command line
 * @author ext-cmarchand
 */
public class InvalidSyntaxException extends Exception {

    public InvalidSyntaxException() {
    }

    public InvalidSyntaxException(String message) {
        super(message);
    }

    public InvalidSyntaxException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidSyntaxException(Throwable cause) {
        super(cause);
    }

    public InvalidSyntaxException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
    
}

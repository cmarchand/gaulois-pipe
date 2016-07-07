/**
 * This Source Code Form is subject to the terms of 
 * the Mozilla Public License, v. 2.0. If a copy of 
 * the MPL was not distributed with this file, You 
 * can obtain one at https://mozilla.org/MPL/2.0/.
 */
package fr.efl.chaine.xslt;

import java.io.File;

/**
 * These exception are thrown during a gaulois runtime
 * @author cmarchand
 */
public class GauloisRunException extends Exception {
    private final File source;

    public GauloisRunException(File source) {
        super();
        this.source=source;
    }
    public GauloisRunException() {
        this((File)null);
    }

    public GauloisRunException(String message, File source) {
        super(message);
        this.source=source;
    }
    public GauloisRunException(String message) {
        this(message, (File)null);
    }

    public GauloisRunException(String message, Throwable cause, File source) {
        super(message, cause);
        this.source=source;
    }
    public GauloisRunException(String message, Throwable cause) {
        this(message, cause, (File)null);
    }

    public GauloisRunException(Throwable cause, File source) {
        super(cause);
        this.source=source;
    }
    public GauloisRunException(Throwable cause) {
        this(cause, (File)null);
    }

    public GauloisRunException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, File source) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.source=source;
    }
    public GauloisRunException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        this(message, cause, enableSuppression, writableStackTrace, (File)null);
    }

    public File getSource() {
        return source;
    }
    
}

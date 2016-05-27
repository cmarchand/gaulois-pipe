/**
 * This Source Code Form is subject to the terms of 
 * the Mozilla Public License, v. 2.0. If a copy of 
 * the MPL was not distributed with this file, You 
 * can obtain one at https://mozilla.org/MPL/2.0/.
 */
package fr.efl.chaine.xslt.config;

import fr.efl.chaine.xslt.InvalidSyntaxException;
import java.io.Serializable;
import net.sf.saxon.s9api.QName;

/**
 * A listener to listen to new files to process
 * @author cmarchand
 */
public class Listener implements Verifiable, Serializable {
    public static final int DEFAULT_PORT=8888;
    public static final transient QName QName = new QName(Config.NS, "listener");
    public static final transient QName ATTR_PORT = new QName("port");
    public static final transient QName ATTR_STOP = new QName("stopKeyword");
    private final int port;
    private final String stopKeyword;
    private JavaStep javastep;
    
    public Listener(final int port, final String stopKeyword) {
        super();
        this.port=port;
        this.stopKeyword=stopKeyword;
    }
    public Listener(final String stopKeyword) {
        this(DEFAULT_PORT, stopKeyword);
    }

    @Override
    public void verify() throws InvalidSyntaxException {
        if(stopKeyword==null || stopKeyword.length()<3) {
            throw new InvalidSyntaxException("STOP keyword must be at least 3 chars length");
        }
        if(javastep!=null) {
            javastep.verify();
        }
    }

    public int getPort() {
        return port;
    }

    public String getStopKeyword() {
        return stopKeyword;
    }

    public JavaStep getJavastep() {
        return javastep;
    }

    public void setJavastep(JavaStep javastep) {
        this.javastep = javastep;
    }
    
    
}

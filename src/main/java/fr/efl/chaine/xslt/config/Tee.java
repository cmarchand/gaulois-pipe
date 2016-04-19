/**
 * This Source Code Form is subject to the terms of 
 * the Mozilla Public License, v. 2.0. If a copy of 
 * the MPL was not distributed with this file, You 
 * can obtain one at https://mozilla.org/MPL/2.0/.
 */
package fr.efl.chaine.xslt.config;

import fr.efl.chaine.xslt.InvalidSyntaxException;
import net.sf.saxon.s9api.QName;

/**
 *
 * @author Christophe Marchand
 */
public class Tee implements Verifiable {
    static final QName QNAME = new QName(Config.NS, "tee");
    static final QName PIPE1 = new QName(Config.NS, "pipe1");
    static final QName PIPE2 = new QName(Config.NS, "pipe2");
    private Pipe pipe1, pipe2;
    
    public Tee() {
        super();
    }

    public Pipe getPipe1() {
        return pipe1;
    }

    public void setPipe1(Pipe pipe1) {
        this.pipe1 = pipe1;
    }

    public Pipe getPipe2() {
        return pipe2;
    }

    public void setPipe2(Pipe pipe2) {
        this.pipe2 = pipe2;
    }
    
    @Override
    public void verify() throws InvalidSyntaxException {
        if(pipe1==null) throw new InvalidSyntaxException("pipe1 n'est pas définit");
        if(pipe2==null) throw new InvalidSyntaxException("pipe2 n'est pas définit");
        pipe1.verify();
        pipe2.verify();
    }
    
}

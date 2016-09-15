/**
 * This Source Code Form is subject to the terms of 
 * the Mozilla Public License, v. 2.0. If a copy of 
 * the MPL was not distributed with this file, You 
 * can obtain one at https://mozilla.org/MPL/2.0/.
 */
package fr.efl.chaine.xslt.config;

import fr.efl.chaine.xslt.InvalidSyntaxException;
import java.util.ArrayList;
import java.util.List;
import net.sf.saxon.s9api.QName;

/**
 *
 * @author Christophe Marchand
 */
public class Tee implements Verifiable {
    static final QName QNAME = new QName(Config.NS, "tee");
    static final QName PIPE = new QName(Config.NS, "pipe");
    private List<Pipe> pipes = new ArrayList<>();
    
    public Tee() {
        super();
    }

    public List<Pipe> getPipes() {
        return pipes;
    }

    public void addPipe(Pipe pipe) {
        pipes.add(pipe);
    }
    @Override
    public void verify() throws InvalidSyntaxException {
        if(pipes.size()<2) throw new InvalidSyntaxException("At least 2 sub-pipes are required");
        for(Pipe pipe:pipes) pipe.verify();
    }
    public String toString(String prefix) {
        StringBuilder sb = new StringBuilder();
        sb.append(prefix).append("tee").append("\n");
        String _p = prefix.concat("  ");
        for(Pipe pipe:pipes) {
            sb.append(pipe.toString(_p));
        }
        return sb.toString();
    }
    
}

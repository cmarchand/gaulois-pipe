/**
 * This Source Code Form is subject to the terms of 
 * the Mozilla Public License, v. 2.0. If a copy of 
 * the MPL was not distributed with this file, You 
 * can obtain one at https://mozilla.org/MPL/2.0/.
 */
package fr.efl.chaine.xslt.config;

import fr.efl.chaine.xslt.InvalidSyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.sf.saxon.s9api.QName;

/**
 *
 * @author ext-cmarchand
 */
public class Pipe implements Verifiable {
    static final QName QNAME = new QName(Config.NS, "pipe");
    static final String ATTR_NB_THREADS = "nbThreads";
    static final String ATTR_MAX = "mutiThreadMaxSourceSize";
    // par défaut, 10Mo
    private int multithreadMaxSourceSize = 10*1024*1024;
    private int nbThreads = 1;
    private final List<ParametrableStep> xslts;
    private Output output;
    private Tee tee;
    private Tee parentTee = null;
    
    public Pipe() {
        super();
        xslts = new ArrayList<>();
    }
    public Pipe(Tee parent) {
        this();
        this.parentTee=parent;
    }

    public int getMultithreadMaxSourceSize() {
        return multithreadMaxSourceSize;
    }

    void setMultithreadMaxSourceSize(int multithreadMaxSourceSize) {
        this.multithreadMaxSourceSize = multithreadMaxSourceSize;
    }

    public int getNbThreads() {
        return nbThreads;
    }

    void setNbThreads(int nbThreads) {
        this.nbThreads = nbThreads;
    }

    public Iterator<ParametrableStep> getXslts() {
        return xslts.iterator();
    }
    
    /**
     * Ajoute une XSL au pipe
     * @param xsl The xsl to add
     * @throws fr.efl.chaine.xslt.InvalidSyntaxException If this xsl is added in a invlid location
     * @throws IllegalStateException Si on a déjà ajouté un <tt>&lt;tee&gt;</tt> ou un <tt>&lt;output&gt;</tt>
     */
    public void addXslt(ParametrableStep xsl) throws InvalidSyntaxException {
        if(output!=null || tee!=null) {
            throw new InvalidSyntaxException("xsl|javaStep elements must not be added after a output or a tee element");
        }
        xslts.add(xsl);
    }

    @Override
    public void verify() throws InvalidSyntaxException {
        for(ParametrableStep x:xslts) x.verify();
        if(!xslts.isEmpty()) {
            if(xslts.get(0) instanceof JavaStep) {
                if(parentTee==null)
                    throw new InvalidSyntaxException("A java step must not be the first step of a pipe. Please a identity XSL to start pipe.");
            }
        }
        if(tee!=null) {
            tee.verify();
        }
        if(output!=null) {
            output.verify();
        }
    }

    public Output getOutput() {
        return output;
    }

    public void setOutput(Output output) throws InvalidSyntaxException {
        if(tee!=null) {
            throw new InvalidSyntaxException("a pipe must be terminated either by a tee or by an output");
        }
        this.output = output;
    }

    public Tee getTee() {
        return tee;
    }

    public void setTee(Tee tee) throws InvalidSyntaxException {
        if(output!=null) {
            throw new InvalidSyntaxException("a pipe must be terminated either by a tee or by an output");
        }
        this.tee = tee;
    }
    
    public boolean isFinal() {
        return output!=null;
    }
    /**
     * Return <tt>true</tt> if this pipe does not contains any <tt>tee</tt>.
     * @return <tt>true</tt> if this pipe does not contains a tee
     */
    public boolean isStraight() {
        return tee==null;
    }
    

}

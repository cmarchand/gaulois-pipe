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
    static final String ATTR_TRACE = "traceOutput";
    // par défaut, 10Mo
    private int multithreadMaxSourceSize = 10*1024*1024;
    private int nbThreads = 1;
    private String traceOutput;
    private final List<ParametrableStep> steps;
    private Output output;
    private Tee tee;
    private Tee parentTee = null;
    
    public Pipe() {
        super();
        steps = new ArrayList<>();
    }
    public Pipe(Tee parent) {
        this();
        this.parentTee=parent;
    }

    public int getMultithreadMaxSourceSize() {
        return multithreadMaxSourceSize;
    }

    public void setMultithreadMaxSourceSize(int multithreadMaxSourceSize) {
        this.multithreadMaxSourceSize = multithreadMaxSourceSize;
    }

    public int getNbThreads() {
        return nbThreads;
    }

    public void setNbThreads(int nbThreads) {
        this.nbThreads = nbThreads;
    }

    public Iterator<ParametrableStep> getXslts() {
        return steps.iterator();
    }
    
    /**
     * Adds an xslt to the pipe
     * @param xsl The xsl to add
     * @throws fr.efl.chaine.xslt.InvalidSyntaxException If this xsl is added in a invalid location
     * @throws IllegalStateException If a <tt>&lt;tee&gt;</tt> or a <tt>&lt;output&gt;</tt> has already been added
     */
    public void addXslt(ParametrableStep xsl) throws InvalidSyntaxException {
        if(output!=null || tee!=null) {
            throw new InvalidSyntaxException("xsl|javaStep elements must not be added after a output or a tee element");
        }
        steps.add(xsl);
    }

    @Override
    public void verify() throws InvalidSyntaxException {
        for(ParametrableStep x:steps) x.verify();
        
        /*
         * TODO cf. Christophe
         * - S'il y a des steps dans le pipe, le premier doit être un xslt.
         * - S'il y a un tee, on doit également mettre un step xslt avant (sinon ça plante !)
         * - Par ailleurs, pour vérifier que le premier step est un xslt,
         *   il faut d'abord vérifier que la liste n'est pas vide (sinon, ArrayOutOfBoundsMachinTruc...)
         *   
         *   
         * - Je veux un code lisible, et
         * - j'aime pas les if dans les if
         * => Donc voici. Right ?
         * 
         */
        boolean shouldHaveXsltAsFirstStep = !steps.isEmpty() || tee != null;
        boolean hasXsltAsFirstStep = !steps.isEmpty() && steps.get(0) instanceof Xslt;
        if(shouldHaveXsltAsFirstStep && !hasXsltAsFirstStep) {
            throw new InvalidSyntaxException("The first step of a pipe must be an XSLT. Please a identity XSL to start pipe.");      
        }
        
        if(tee==null && output==null) {
            throw new InvalidSyntaxException("a pipe must be terminated either by a tee or by an output");
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

    /**
     * Retuns the trace output. Valid values are #default, #logger, or an URI
     * @return Where to output traces
     */
    public String getTraceOutput() {
        return traceOutput;
    }

    void setTraceOutput(String traceOutput) {
        this.traceOutput = traceOutput;
    }
    
    @Override
    public String toString() {
        return toString("");
    }
    public String toString(final String prefix) {
        String _p = prefix+"  ";
        StringBuilder sb = new StringBuilder();
        sb.append(prefix).append("pipe\n");
        for(ParametrableStep ps:steps) {
            sb.append(ps.toString(_p));
        }
        if(output!=null) {
            sb.append(output.toString(_p));
        }
        if(tee!=null) {
            sb.append(tee.toString(_p));
        }
        return sb.toString();
    }
    

}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.efl.chaine.xslt;

import java.util.HashMap;
import net.sf.saxon.Configuration;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.s9api.Destination;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmValue;

/**
 * All user-defined java steps must extends this class.
 * An implementation of StepJava must not be a pipe final Step. 
 * It must have a following step.
 * It must not be a root pipe Step.
 * @author cmarchand
 */
public abstract class StepJava implements Destination {
    protected Destination nextStep;
    private final HashMap<QName, XdmValue> parameters;
    
    public StepJava() {
        super();
        this.parameters = new HashMap<>();
    }

    /**
     * Defines the next step in the pipeline.
     * This call must be done when constructing the pipeline. A StepJava must not be a terminal Step.
     * @param nextStep The next step to process
     */
    public void setDestination(Destination nextStep) {
        this.nextStep=nextStep;
    }
    
    /**
     * Use this method when you construct the ProxyReceiver in {@link #getReceiver(Configuration)}
     * @param config The config to use.
     * @return The receiver of the next step
     * @throws SaxonApiException If something goes wrong
     */
    protected Receiver getNextReceiver(Configuration config) throws SaxonApiException {
        return nextStep.getReceiver(config);
    }
    
    /**
     * Defines a parameter
     * @param name Param name...
     * @param value and Param value
     */
    public void setParameter(QName name, XdmValue value) {
        parameters.put(name, value);
    }
    
    /**
     * Returns the parameter value, or <tt>null</tt> if it does not exists.
     * @param name The parameter name
     * @return The Parameter value, if defined
     */
    public XdmValue getParameter(QName name) {
        return parameters.get(name);
    }

    /**
     * Propagates the close on the chain.
     * Transform are actually done one {@link #close() } call.
     * See <a href="http://saxon.markmail.org/search/?q=#query:+page:1+mid:r4mkgcg7awklwqwq+state:results">Saxon mailing list</a>
     * @throws SaxonApiException In case of problem.
     */
    @Override
    public void close() throws SaxonApiException {
        nextStep.close();
    }
    
    
}

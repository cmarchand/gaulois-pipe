/**
 * This Source Code Form is subject to the terms of 
 * the Mozilla Public License, v. 2.0. If a copy of 
 * the MPL was not distributed with this file, You 
 * can obtain one at https://mozilla.org/MPL/2.0/.
 */
package fr.efl.chaine.xslt;

import fr.efl.chaine.xslt.config.Pipe;
import java.io.Serializable;
import java.util.concurrent.ExecutorService;
import net.sf.saxon.s9api.MessageListener;
import top.marchand.xml.gaulois.config.typing.DatatypeFactory;

/**
 * This represents a pipe execution context :
 * pipe, parameters, listeners, etc...
 * 
 * @author cmarchand
 * @since 1.00.01
 */
public class ExecutionContext implements Serializable {
    private final Pipe pipe;
    private final MessageListener msgListener;
    private final ExecutorService service;
    private final GauloisPipe gaulois;

    public ExecutionContext(final GauloisPipe gaulois, final Pipe pipe, final MessageListener msgListener, final ExecutorService service) {
        super();
        this.pipe = pipe;
        this.msgListener = msgListener;
        this.service = service;
        this.gaulois = gaulois;
    }

    public Pipe getPipe() {
        return pipe;
    }

    public MessageListener getMsgListener() {
        return msgListener;
    }

    public ExecutorService getService() {
        return service;
    }
    
    public boolean isValid() {
        return !(gaulois==null || pipe==null || service==null || service.isTerminated());
    }

    public GauloisPipe getGaulois() {
        return gaulois;
    }
    
    public String getErrorMessage() {
        if(pipe==null) return "context must have a non null pipe";
        if(gaulois==null) return "context must have a non null GauloisPipe";
        if(service==null) return "context must have a non null service";
        if(service.isTerminated()) return "context must not have a terminated service";
        return null;
    }
    public DatatypeFactory getDatatypeFactory() { return gaulois.getDatatypeFactory(); }
    
}

/**
 * This Source Code Form is subject to the terms of 
 * the Mozilla Public License, v. 2.0. If a copy of 
 * the MPL was not distributed with this file, You 
 * can obtain one at https://mozilla.org/MPL/2.0/.
 */
package fr.efl.chaine.xslt.utils;

import net.sf.saxon.Configuration;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.s9api.Destination;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.TeeDestination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A TeeDestination where main destination in unknown when building.
 * @author cmarchand
 */
public class TeeDebugDestination implements Destination {
//    private static final Logger LOGGER = LoggerFactory.getLogger(TeeDebugDestination.class);
    private TeeDestination tee;
    private final Destination debugDestination;

    public TeeDebugDestination(Destination debugDestination) {
//        LOGGER.debug("<new>(debugDestination)");
        this.debugDestination = debugDestination;
    }
    
    public void setDestination(Destination mainDestination) {
//        LOGGER.debug("setDestination(mainDestination)");
        tee = new TeeDestination(debugDestination, mainDestination);
    }

    @Override
    public Receiver getReceiver(Configuration c) throws SaxonApiException {
//        LOGGER.debug("getReceiver(Configuration)");
        return tee.getReceiver(c);
    }

    @Override
    public void close() throws SaxonApiException {
        tee.close();
    }
    
}

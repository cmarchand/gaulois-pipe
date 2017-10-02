/**
 * This Source Code Form is subject to the terms of 
 * the Mozilla Public License, v. 2.0. If a copy of 
 * the MPL was not distributed with this file, You 
 * can obtain one at https://mozilla.org/MPL/2.0/.
 */
package fr.efl.chaine.xslt.utils;

import net.sf.saxon.s9api.Destination;
import net.sf.saxon.s9api.XsltTransformer;

/**
 *
 * @author <a href="christophe@marchand.top">Christophe Marchand</a>
 */
public class DoubleDestination {
    private Object start;
    private Destination end;
    
    public DoubleDestination() {
        super();
    }
    public DoubleDestination(final Object start, final Destination end) {
        this();
        this.start=start;
        this.end=end;
    }

    public Object getStart() {
        return start;
    }

    public void setStart(XsltTransformer start) {
        this.start = start;
    }

    public Destination getEnd() {
        return end;
    }

    public void setEnd(Destination end) {
        this.end = end;
    }
    
    
}

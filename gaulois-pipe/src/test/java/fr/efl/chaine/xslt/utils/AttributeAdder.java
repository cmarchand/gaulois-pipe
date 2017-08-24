/**
 * This Source Code Form is subject to the terms of 
 * the Mozilla Public License, v. 2.0. If a copy of 
 * the MPL was not distributed with this file, You 
 * can obtain one at https://mozilla.org/MPL/2.0/.
 */
package fr.efl.chaine.xslt.utils;

import fr.efl.chaine.xslt.StepJava;
import net.sf.saxon.Configuration;
import net.sf.saxon.event.ProxyReceiver;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.expr.parser.Location;
import net.sf.saxon.om.FingerprintedQName;
import net.sf.saxon.om.NodeName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.util.AttributeCollectionImpl;
import net.sf.saxon.type.AnySimpleType;
import net.sf.saxon.type.SchemaType;

/**
 *
 * @author cmarchand
 */
public class AttributeAdder extends StepJava {

    private AttributeCollectionImpl attributes;

    private Receiver underlyingReceiver;
    
    @Override
    public Receiver getReceiver(Configuration c) throws SaxonApiException {
        underlyingReceiver = new AttributeAdderReceiver(getNextReceiver(c));
        return underlyingReceiver;
    }

    @Override
    public void close() throws SaxonApiException {
    }
    
    
    private class AttributeAdderReceiver extends ProxyReceiver {
        
        public AttributeAdderReceiver(Receiver nextReceiver) {
            super(nextReceiver);
        }

        @Override
        public void startElement(NodeName elemName, SchemaType typeCode, Location location, int properties) throws XPathException {
            super.startElement(elemName, typeCode, location, properties);
            attributes = new AttributeCollectionImpl(getConfiguration());
            attributes.addAttribute(
                    new FingerprintedQName("", "", "test"), 
                    AnySimpleType.getInstance(), 
                    Long.toString(System.nanoTime()),
                    location, 
                    properties);
        }

        @Override
        public void startContent() throws XPathException {
            for (int i = 0; i < attributes.getLength(); i++) {
                nextReceiver.attribute(
                        attributes.getNodeName(i),
                        attributes.getTypeAnnotation(i),
                        attributes.getValue(i),
                        attributes.getLocation(i),
                        attributes.getProperties(i));
            }
            super.startContent();
        }
        
        
    }
}

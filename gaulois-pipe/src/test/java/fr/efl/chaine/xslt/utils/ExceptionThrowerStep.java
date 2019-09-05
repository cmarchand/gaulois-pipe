package fr.efl.chaine.xslt.utils;

import fr.efl.chaine.xslt.StepJava;
import net.sf.saxon.Configuration;
import net.sf.saxon.event.ProxyReceiver;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.trans.XPathException;

/**
 * This test Step will always throw an exception
 * when startDocument() is triggered.
 * 
 * @author Jim Etevenard
 *
 */
public class ExceptionThrowerStep extends StepJava {
	
	/**
	 * Public "signature" that we can recognize in the error message.
	 */
	public static final String MARKER = "very stupid Java step";
	

	@Override
	public Receiver getReceiver(Configuration config) throws SaxonApiException {
		
		return new ExceptionThrowerReceiver(getNextReceiver(config));
	}
	
	public class ExceptionThrowerReceiver extends ProxyReceiver {
		public ExceptionThrowerReceiver(Receiver nextReceiver) {
			super(nextReceiver);
		}
		
		@Override
		public void startDocument(int properties) throws XPathException {
			throw new XPathException(String.format("This is a %s that will always throw an exception !", MARKER));
		}	
		
	}

}

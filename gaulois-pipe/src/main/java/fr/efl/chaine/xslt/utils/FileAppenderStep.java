/**
 * This Source Code Form is subject to the terms of 
 * the Mozilla Public License, v. 2.0. If a copy of 
 * the MPL was not distributed with this file, You 
 * can obtain one at https://mozilla.org/MPL/2.0/.
 */
package fr.efl.chaine.xslt.utils;

import fr.efl.chaine.xslt.StepJava;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import net.sf.saxon.Configuration;
import net.sf.saxon.event.ProxyReceiver;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.trans.XPathException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A step to append something in a text-file
 * @author Christophe Marchand christophe@marchand.top
 */
public class FileAppenderStep extends StepJava {
    /**
     * The file name to append to
     */
    public static final QName FILE_NAME = new QName("filePath");
    /**
     * The value to append to file
     */
    public static final QName VALUE = new QName("value");
    /**
     * The line separator to use. Default is platform default.
     * Valid values or CR, LF, CRLF, or empty string fo no separator
     */
    public static final QName LINE_SEPARATOR = new QName("lineSeparator");
    /**
     * The encoding to use to append to file. Default is UTF-8
     */
    public static final QName ENCODING = new QName("encoding");
    
    private static final Logger LOGGER = LoggerFactory.getLogger(FileAppenderStep.class);
    
    private Receiver underlyingReceiver;

    @Override
    public Receiver getReceiver(Configuration c) throws SaxonApiException {
        underlyingReceiver = new FileAppenderReceiver(getNextReceiver(c));
        return underlyingReceiver;
    }

    @Override
    public void close() throws SaxonApiException {
        // nothing to do
    }
    
    private class FileAppenderReceiver extends ProxyReceiver {

        public FileAppenderReceiver(Receiver nextReceiver) {
            super(nextReceiver);
        }

        @Override
        public void close() throws XPathException {
            super.close(); 
            String fileName = getParameter(FILE_NAME).toString();
            String value = getParameter(VALUE).toString();
            String lineSeparator = getLineSeparator(getParameter(LINE_SEPARATOR));
            Charset encoding = getCharset(getParameter(ENCODING));
            File f = new File(fileName);
            try {
                FileOutputStream fos = new FileOutputStream(f, true);
                try (OutputStreamWriter osw = new OutputStreamWriter(fos, encoding)) {
                    osw.append(value);
                    osw.append(lineSeparator);
                    osw.flush();
                }
            } catch(IOException ex) {
                LOGGER.error("while writting to "+fileName,ex);
            }
            
        }
        
    }
    static String getLineSeparator(final XdmValue in) {
        if(in==null) return System.getProperty("line.separator");
        String sIn = in.toString();
        if(sIn.isEmpty()) return "";
        return sIn.replaceAll("CR", "\r").replaceAll("LF", "\n");
    }

    static Charset getCharset(final XdmValue value) {
        if(value==null) return Charset.forName("UTF-8");
        String in = value.toString();
        if(Charset.isSupported(in)) {
            return Charset.forName(in);
        } else {
            return Charset.forName("UTF-8");
        }
    }
    
}

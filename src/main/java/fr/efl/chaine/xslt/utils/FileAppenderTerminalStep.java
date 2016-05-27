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
import net.sf.saxon.event.PipelineConfiguration;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.expr.parser.Location;
import net.sf.saxon.om.NamespaceBinding;
import net.sf.saxon.om.NodeName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.SchemaType;
import net.sf.saxon.type.SimpleType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Same as {@link FileAppenderStep}, but it is designed to be a terminal step, with no next step,
 * and so can be used in a &lt;listener&gt;
 * 
 * @author cmarchand
 */
public class FileAppenderTerminalStep extends StepJava {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileAppenderTerminalStep.class);

    @Override
    public Receiver getReceiver(Configuration c) throws SaxonApiException {
        return new FileAppenderTerminalReceiver();
    }

    @Override
    public void close() throws SaxonApiException {
        // nothing to do
    }
    
    private class FileAppenderTerminalReceiver implements Receiver {
        private PipelineConfiguration pipelineConfiuration;
        private String systemId;

        @Override
        public void setPipelineConfiguration(PipelineConfiguration pc) {
            this.pipelineConfiuration=pc;
        }

        @Override
        public PipelineConfiguration getPipelineConfiguration() {
            return pipelineConfiuration;
        }

        @Override
        public void setSystemId(String ssId) {
            this.systemId=ssId;
        }

        @Override
        public void open() throws XPathException {}

        @Override
        public void startDocument(int i) throws XPathException {}

        @Override
        public void endDocument() throws XPathException {}

        @Override
        public void setUnparsedEntity(String string, String string1, String string2) throws XPathException {}

        @Override
        public void startElement(NodeName nn, SchemaType st, Location lctn, int i) throws XPathException {}

        @Override
        public void namespace(NamespaceBinding nb, int i) throws XPathException {}

        @Override
        public void attribute(NodeName nn, SimpleType st, CharSequence cs, Location lctn, int i) throws XPathException {}

        @Override
        public void startContent() throws XPathException {}

        @Override
        public void endElement() throws XPathException {}

        @Override
        public void characters(CharSequence cs, Location lctn, int i) throws XPathException {}

        @Override
        public void processingInstruction(String string, CharSequence cs, Location lctn, int i) throws XPathException {}

        @Override
        public void comment(CharSequence cs, Location lctn, int i) throws XPathException {}

        @Override
        public void close() throws XPathException {
            String fileName = getParameter(FileAppenderStep.FILE_NAME).toString();
            String value = getParameter(FileAppenderStep.VALUE).toString();
            String lineSeparator = FileAppenderStep.getLineSeparator(getParameter(FileAppenderStep.LINE_SEPARATOR));
            Charset encoding = FileAppenderStep.getCharset(getParameter(FileAppenderStep.ENCODING));
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

        @Override
        public boolean usesTypeAnnotations() {
            return false;
        }

        @Override
        public String getSystemId() {
            return systemId;
        }
        
    }
    
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.efl.chaine.xslt.utils;

import fr.efl.chaine.xslt.GauloisPipe;
import javax.xml.transform.SourceLocator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.MessageListener;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmNodeKind;

public class XsltMessageListener implements MessageListener {

    private String nomXSLT="";
   private Logger logger ;

    public XsltMessageListener(String nomXSLT) {
        super();
        this.nomXSLT = "." + nomXSLT.replaceAll("[:/.]", "_");
          logger = LoggerFactory.getLogger(GauloisPipe.class.getName()+ this.nomXSLT );
          
    }
   public XsltMessageListener() {
        super();
        this.nomXSLT = ".XSLT";
           logger = LoggerFactory.getLogger(GauloisPipe.class.getName() + ".XSLT" );
       
    }
    
    @Override
    public void message(XdmNode content, boolean terminate, SourceLocator locator) {
        /*
         * Le contenu du message (content) est soit directement du texte,
         * soit un document XML de type log qui suit la structure suivante : 
         * <log level="niveau de log" 
         * 		source="xslt ou schéma sch source" ... >Message du log</log>
         */

        XdmNode xdmMessage = (XdmNode) content.axisIterator(Axis.CHILD).next();
        String textMessage = xdmMessage.getStringValue();
        XdmNodeKind kind = xdmMessage.getNodeKind();
        if (kind.equals(XdmNodeKind.TEXT)) {
             logger.info((terminate?"[TERMINATE] ":"")+textMessage);
        } else {
            String level = xdmMessage.getAttributeValue(new QName("level"));
            String channel = xdmMessage.getAttributeValue(new QName("channel"));            
            Logger loggerLocal = LoggerFactory.getLogger(GauloisPipe.class.getName() + (channel==null? "" : "." +channel) + nomXSLT);
            switch (level) {
                case "info":
                    loggerLocal.info((terminate?"[TERMINATE] ":"")+textMessage);
                    break;
                case "warn":
                    loggerLocal.warn((terminate?"[TERMINATE] ":"")+textMessage);
                    break;
                case "error":
                    loggerLocal.error((terminate?"[TERMINATE] ":"")+textMessage);
                    break;
                default:
                    break;
            }
        }
    }
}

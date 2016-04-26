/**
 * This Source Code Form is subject to the terms of 
 * the Mozilla Public License, v. 2.0. If a copy of 
 * the MPL was not distributed with this file, You 
 * can obtain one at https://mozilla.org/MPL/2.0/.
 */
package fr.efl.chaine.xslt.config;

import de.schlichtherle.truezip.file.TFile;
import fr.efl.chaine.xslt.InvalidSyntaxException;
import fr.efl.chaine.xslt.utils.ParameterValue;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashMap;
import net.sf.saxon.s9api.QName;

/**
 *
 * @author ext-cmarchand
 */
public class Xslt implements ParametrableStep {
    static final QName QNAME = new QName(Config.NS, "xslt");
    static final QName ATTR_HREF = new QName("href");
    private String href;
    private HashMap<String,ParameterValue> params;
    // pour usage interne
    private File file;
    
    public Xslt() {
        super();
        params = new HashMap<>();
    }
    
    public Xslt(String href) {
        this();
        this.href = href;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    @Override
    public Collection<ParameterValue> getParams() {
        return params.values();
    }
    
    @Override
    public void addParameter(ParameterValue param) {
        params.put(param.getKey(), param);
    }
    
    // cette méthode ne peut être appelée, il faut passer par la substitution dans le href
    private File getFile() throws URISyntaxException {
        if(file==null) {
            if(href.startsWith("file:")) {
                file = new File(new URI(href));
            } else if(href.startsWith("jar:")) {
                String xslUri = href.substring(href.indexOf("!")+1);
                if(!xslUri.startsWith("/")) xslUri = "/"+xslUri;
                String jarUri = href.substring(4,href.length()-xslUri.length()-1);
                if(jarUri.startsWith("file://")) {
                    jarUri = jarUri.substring(7);
                } else if(jarUri.startsWith("file:")) {
                    jarUri = jarUri.substring(5);
                }
                file = new TFile(jarUri+xslUri);
            } else {
                file = new File(href);
            }
        }
        return file;
    }

    @Override
    public void verify() throws InvalidSyntaxException {
        try {
            if(!href.contains("$[")) {
                File xslFile = getFile();
                if(xslFile instanceof TFile) {
                    TFile tf = (TFile)xslFile;
                    if(!tf.canRead()) {
                        throw new InvalidSyntaxException(tf.getAbsolutePath()+" is not readable");
                    }
                } else {
                    if(!getFile().exists() || !getFile().isFile()) {
                        throw new InvalidSyntaxException(getFile().getAbsolutePath()+" does not exists or is not a regular file");
                    }
                }
            } else {
                // on ne peut pas effectuer cette vérification.
                // on ne peut pas vérifier que les paramètres existent
                // on ne fait pas de contrôle
            }
        } catch(URISyntaxException ex) {
            throw new InvalidSyntaxException(ex);
        }
    }
    
}

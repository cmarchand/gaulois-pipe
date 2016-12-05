/**
 * This Source Code Form is subject to the terms of 
 * the Mozilla Public License, v. 2.0. If a copy of 
 * the MPL was not distributed with this file, You 
 * can obtain one at https://mozilla.org/MPL/2.0/.
 */
package fr.efl.chaine.xslt.config;

import fr.efl.chaine.xslt.GauloisPipe;
import fr.efl.chaine.xslt.InvalidSyntaxException;
import fr.efl.chaine.xslt.utils.ParameterValue;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
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
    static final QName ATTR_TRACE_ACTIVE = new QName("traceActive");
    static final QName ATTR_DEBUG= new QName("debug");
    static final QName ATTR_ID = new QName("id");
    private String href;
    private HashMap<String,ParameterValue> params;
    private boolean traceToAdd = false;
    // pour usage interne
    private File file;
    private boolean debug;
    private String id;
    
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
        if(param!=null) {
            params.put(param.getKey(), param);
        }
    }
    
    // cette méthode ne peut être appelée, il faut passer par la substitution dans le href
    private File getFile() throws URISyntaxException {
        if(file==null) {
            if(href.startsWith("jar:")) {
                file = new File(new URI(href));
            } else if(href.startsWith("cp:")) {
                // nope
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
                if(href.startsWith("jar:")) {
                    try (InputStream is = new URL(href).openStream()){
                    } catch(Exception ex) {
                        throw new InvalidSyntaxException("Unable to load "+href+". Either jar file does not exist, or entry does not exist in.");
                    }
                } else if(href.startsWith("cp:")) {
                    URL url = GauloisPipe.class.getResource(href.substring(3));
                    if(url==null) throw new InvalidSyntaxException("Unable to load "+href+" from classpath. Did you start with a '/' ?");
                } else  {
                    File xslFile = getFile();
                    if(!xslFile.exists() || !xslFile.isFile()) {
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

    /**
     * Return true if a TraceListener should be added to this Xslt
     * @return true if a TraceListener should be added to this Xslt
     */
    public boolean isTraceToAdd() {
        return traceToAdd;
    }

    void setTraceToAdd(boolean traceToAdd) {
        this.traceToAdd = traceToAdd;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString(String prefix) {
        StringBuilder sb = new StringBuilder();
        sb.append(prefix).append("xslt href=").append(href);
        if(id!=null) {
            sb.append(" id=").append(id);
        }
        sb.append("\n");
        return sb.toString();
    }

    @Override
    public String toString() {
        return toString("");
    }
    
    
}

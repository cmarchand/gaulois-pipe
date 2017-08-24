/**
 * This Source Code Form is subject to the terms of 
 * the Mozilla Public License, v. 2.0. If a copy of 
 * the MPL was not distributed with this file, You 
 * can obtain one at https://mozilla.org/MPL/2.0/.
 */
package fr.efl.chaine.xslt.config;

import fr.efl.chaine.xslt.InvalidSyntaxException;
import fr.efl.chaine.xslt.utils.ParameterValue;
import java.io.File;
import java.util.HashMap;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author ext-cmarchand
 */
public class Config implements Verifiable {
    public static final int MAX_DOCUMENT_CACHE_SIZE = 1;
    static final QName ATTR_DOCUMENT_CACHE_SIZE = new QName("documentCacheSize");
    static final String NS = "http://efl.fr/chaine/saxon-pipe/config";
    static final QName PARAMS_CHILD = new QName(NS, "params");
    private Pipe pipe;
    private final HashMap<QName,ParameterValue> params;
    private Sources sources;
    private int maxDocumentCacheSize = MAX_DOCUMENT_CACHE_SIZE;
    private static final Logger LOGGER = LoggerFactory.getLogger(Config.class);
    private boolean logFileSize;
    private boolean skipSchemaValidation;
    public Namespaces namespaces;
    private File currentDir;
    /**
     * This has nothing to do in Configuration, but for implementation reason,
     * it's here...
     */
    public transient String __instanceName;
    

    public Config(XdmNode node) {
        super();
        params = new HashMap<>();
        String val = node.getAttributeValue(ATTR_DOCUMENT_CACHE_SIZE);
        if(val!=null) {
            try {
                maxDocumentCacheSize = Integer.parseInt(val);
            } catch(Exception ex) {
                LOGGER.warn(val+" n'est pas une valeur acceptable pour la taille maximale du cache de documents. "+MAX_DOCUMENT_CACHE_SIZE+" sera utilisé");
            }
        }
    }
    /**
     * Pour compatibilité avec l'ancienne version
     */
    public Config() {
        super();
        params = new HashMap<>();
        currentDir = new File(System.getProperty("user.dir"));
    }
    public Config(String currentDir) {
        super();
        params = new HashMap<>();
        this.currentDir = new File(currentDir);
    }
    public Pipe getPipe() {
        return pipe;
    }

    void setPipe(Pipe pipe) {
        this.pipe = pipe;
    }

    public HashMap<QName,ParameterValue> getParams() {
        return params;
    }

    public void addParameter(ParameterValue p) {
        if(p!=null) {
            params.put(p.getKey(),p);
        }
    }

    public Sources getSources() {
        return sources;
    }

    void setSources(Sources sources) {
        this.sources = sources;
    }

    @Override
    public void verify() throws InvalidSyntaxException {
        if(pipe==null) throw new InvalidSyntaxException("No pipe defined");
        pipe.verify();
        if(sources==null) throw new InvalidSyntaxException("No input file defined");
        sources.verify();
        if(namespaces!=null) {
            namespaces.verify();
        }
        for(ParameterValue pv: params.values()) {
            LOGGER.debug(pv.toString());
            if(pv.isAbstract()) {
                throw new InvalidSyntaxException("Parameter "+pv.getKey().getEQName()+" is declared abstract and no value is provided for it.");
            }
        }
    }
    /**
     * Returns <tt>true</tt> if input files with size over multi-thread limit exist.
     * @return <tt>true</tt> if at least one file exceeds the limit size
     */
    public boolean hasFilesOverMultiThreadLimit() {
        return getSources().hasFileOverLimit(getPipe().getMultithreadMaxSourceSize());
    }

    public int getMaxDocumentCacheSize() {
        return maxDocumentCacheSize;
    }

    public void setMaxDocumentCacheSize(int maxDocumentCacheSize) {
        this.maxDocumentCacheSize = maxDocumentCacheSize;
    }

    public boolean isLogFileSize() {
        return logFileSize;
    }

    public void setLogFileSize(boolean logFileSize) {
        this.logFileSize = logFileSize;
    }
    
    public void skipSchemaValidation(final boolean skipSchemaValidation) {
        this.skipSchemaValidation=skipSchemaValidation;
    }

    public Namespaces getNamespaces() {
        return namespaces;
    }

    public void setNamespaces(Namespaces namespaces) {
        this.namespaces = namespaces;
    }
    
}

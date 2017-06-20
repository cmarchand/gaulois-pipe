/**
 * This Source Code Form is subject to the terms of 
 * the Mozilla Public License, v. 2.0. If a copy of 
 * the MPL was not distributed with this file, You 
 * can obtain one at https://mozilla.org/MPL/2.0/.
 */
package fr.efl.chaine.xslt.config;

import net.sf.saxon.s9api.QName;
import fr.efl.chaine.xslt.InvalidSyntaxException;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author ext-cmarchand
 */
public class Sources implements Verifiable {
    private static final Logger LOGGER = LoggerFactory.getLogger(Sources.class);
    static final QName QNAME = new QName(Config.NS, "sources");
    static final QName ATTR_ORDERBY = new QName("orderBy");
    static final QName ATTR_SORT = new QName("sort");
    private final String orderBy, sort;
    private final List<CfgFile> files;
    private long maxFileSize = 0l;
    private Map<File,Integer> hrefCount;
    private Listener listener;

    
    public Sources(String orderBy, String sort) throws InvalidSyntaxException {
        if(orderBy==null) orderBy="size";
        if(sort==null) sort="desc";
        if(!"size".equals(orderBy) && !"name".equals(orderBy) && !"define".equals(orderBy)) {
            throw new InvalidSyntaxException("orderBy must be one of (size,name,define)");
        } else {
            this.orderBy=orderBy;
        }
        if(!"define".equals(orderBy) && !"asc".equals(sort) && !"desc".equals(sort)) {
            throw new InvalidSyntaxException("sort must be one of (asc,desc)");
        } else {
            this.sort = sort;
        }
        files = new ArrayList<>();
        hrefCount = new HashMap<>();
    }
    public Sources(String orderBy) throws InvalidSyntaxException {
        this(orderBy, "desc");
    }
    public Sources() throws InvalidSyntaxException {
        this("size");
    }
    public List<CfgFile> getFiles() {
        return getFiles(orderBy, sort);
    }
    
    private static Comparator<CfgFile> getComparator(final String orderBy, final String sort) {
        return new Comparator<CfgFile>() {
            @Override
            public int compare(CfgFile t, CfgFile t1) {
                long ret = "name".equals(orderBy) ? t.getSource().getName().compareTo(t1.getSource().getName()) : t.getSource().length()-t1.getSource().length();
                if("desc".equals(sort)) ret = -ret;
                if(ret==0l) {
                    return (int)ret;
                }
                else if (ret<0l) return -1;
                else return 1;
            }
        };
    }
    private List<CfgFile> getFiles(final String orderBy, final String sort) {
        LOGGER.trace("getFiles({}, {})", orderBy, sort);
        if("define".equals(orderBy)) {
            return files;
        } else {
            LOGGER.trace("getFiles() sort {} files with orderBy={} and sort={}", new Object[]{files.size(), orderBy, sort});
            List<CfgFile> ret = new ArrayList<>(files);
            Collections.sort(ret, getComparator(orderBy, sort));
            LOGGER.trace("getFiles() return {} files", ret.size());
            return ret;
        }
    }

    public void addFile(CfgFile file) {
        long fileLength = file.getSource().length();
        LOGGER.info("adding file {} with length {}", file, fileLength);
        files.add(file);
        maxFileSize = Math.max(maxFileSize, fileLength);
        Integer count = hrefCount.get(file.getSource());
        if(count==null) {
            count = 1;
        } else {
            count++;
        }
        hrefCount.put(file.getSource(), count);
    }
    public void addFiles(Collection<CfgFile> files) {
        // on énumère pour calculer la taille max
        for(CfgFile file: files) {
            addFile(file);
        }
    }
    
    public int getFileUsage(File f) {
        Integer ret =  hrefCount.get(f);
        return ret==null ? 0 : ret;
    }

    @Override
    public void verify() throws InvalidSyntaxException {
        // pas besoin de vérifier les orderBy et sort, ils sont immutables finaux et vérifiés dans le constructeur
        // on ne jette plus d'exception, juste un gros warning
        if(files.isEmpty()) {
            LOGGER.warn("No input file to process. If you have used a pattern to find files, check your regex.");
        }
        for(CfgFile f:files) f.verify();
        if(listener!=null) listener.verify();
    }
    
    public boolean hasFileOverLimit(long limit) {
        return maxFileSize>limit;
    }
    
    public List<CfgFile> getFilesOverLimit(long limit) {
        // on va quand même les renvoyer triés comme demandés
        List<CfgFile> _files = getFiles("size", "asc");
        List<CfgFile> ret = new ArrayList<>(_files.size());
        for(CfgFile file: _files) {
            if(file.getSource().length()>limit) {
                ret.add(file);
            }
        }
        Collections.sort(_files, getComparator(orderBy, sort));
        LOGGER.debug("getFilesOverLimit() -> {}", ret.size());
        return ret;
    }
    public List<CfgFile> getFilesUnderLimit(long limit) {
        // on va quand même les renvoyer triés comme demandés
        List<CfgFile> _files = getFiles("size", "asc");
        List<CfgFile> ret = new ArrayList<>(_files.size());
        for(CfgFile file: _files) {
            if(file.getSource().length()<=limit) {
                ret.add(file);
            }
        }
        Collections.sort(_files, getComparator(orderBy, sort));
        LOGGER.debug("getFilesUnderLimit() -> {}", ret.size());
        return ret;
    }

    public Listener getListener() {
        return listener;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }
    
}

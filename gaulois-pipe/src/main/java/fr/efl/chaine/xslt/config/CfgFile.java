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

/**
 *
 * @author ext-cmarchand
 */
public class CfgFile implements Verifiable {
    static final QName QNAME = new QName(Config.NS, "file");
    static final QName QN_FOLDER = new QName(Config.NS, "folder");
    static final QName ATTR_HREF = new QName("href");
    private final File source;
    private final HashMap<QName, ParameterValue> params;
    
    public CfgFile(File source) {
        super();
        this.source=source;
        params = new HashMap<>();
    }
    public void addParameter(ParameterValue param) {
        if(param==null) return;
        params.put(param.getKey(), param);
    }
    public File getSource() { return source; }
    public HashMap<QName,ParameterValue> getParams() {
        return params;
    }

    @Override
    public void verify() throws InvalidSyntaxException {
        if(!getSource().exists() || !getSource().isFile()) throw new InvalidSyntaxException(getSource().getAbsolutePath()+" does no exists or is not a regular file.");
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(source.getAbsolutePath());
        if(params.size()>0) {
            sb.append("[");
            for(ParameterValue pv:params.values()) {
                sb.append("(").append(pv.getKey()).append(",").append(pv.getValue()).append("),");
            }
            sb.deleteCharAt(sb.length()-1);
            sb.append("]");
        }
        return sb.toString();
    }
    
    
}

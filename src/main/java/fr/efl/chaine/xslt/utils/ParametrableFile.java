/**
 * This Source Code Form is subject to the terms of 
 * the Mozilla Public License, v. 2.0. If a copy of 
 * the MPL was not distributed with this file, You 
 * can obtain one at https://mozilla.org/MPL/2.0/.
 */
package fr.efl.chaine.xslt.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Used to carry a file and the parameters to give to pipe when processing this file
 * @author ext-cmarchand
 */
public class ParametrableFile {
    private final List<ParameterValue> parameters;
    private final File file;
    
    public ParametrableFile(final File file) {
        super();
        this.file=file;
        parameters = new ArrayList<>();
    }

    public List<ParameterValue> getParameters() {
        return parameters;
    }

    public File getFile() {
        return file;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if(file!=null) {
            sb.append(file.getAbsolutePath());
        } else {
            sb.append("<null>");
        }
        if(!parameters.isEmpty()) {
            sb.append("(");
            for(ParameterValue pv: parameters) {
                sb.append(pv).append(",");
            }
            sb.deleteCharAt(sb.length()-1);
            sb.append(")");
        }
        return sb.toString();
    }
}

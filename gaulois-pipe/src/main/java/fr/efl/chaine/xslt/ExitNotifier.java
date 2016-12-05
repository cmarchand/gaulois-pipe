/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.efl.chaine.xslt;

import java.util.List;

/**
 *
 * @author cmarchand
 */
public class ExitNotifier implements Runnable {
    private List<Exception> errors;
    public ExitNotifier(List<Exception> errors) {
        super();
        this.errors = errors;
    }

    @Override
    public void run() {
        if(errors==null) {
            
        }
    }
    
}

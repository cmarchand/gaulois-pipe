/**
 * This Source Code Form is subject to the terms of 
 * the Mozilla Public License, v. 2.0. If a copy of 
 * the MPL was not distributed with this file, You 
 * can obtain one at https://mozilla.org/MPL/2.0/.
 */
package fr.efl.chaine.xslt.listener;

import fr.efl.chaine.xslt.utils.ParameterValue;
import java.util.List;
import java.util.concurrent.ExecutorService;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

/**
 * The server that receives incoming requests
 * @author cmarchand
 */
public class HttpListener {
    private final long limitSize;
    private final ExecutorService service;
    private final int port;
    private final String stopKeyword;
    static final transient String SERVICE_ENTRY = "SERVICE";
    static final transient String STOP_ENTRY = "STOP_ENTRY";
    
    public HttpListener(final int port, final String stopKeyword, final ExecutorService service, final long limitSize) {
        super();
        this.limitSize=limitSize;
        this.service=service;
        this.port=port;
        this.stopKeyword=stopKeyword;
    }
    
    public void run() {
        final Server jettyServer = new Server(port);
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        context.setContextPath("");
        context.getServletContext().setAttribute(SERVICE_ENTRY, service);
        context.getServletContext().setAttribute(STOP_ENTRY, stopKeyword);
        context.addServlet(new ServletHolder(FileServlet.class), "/*");
    }
    
}

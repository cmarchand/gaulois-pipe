/**
 * This Source Code Form is subject to the terms of 
 * the Mozilla Public License, v. 2.0. If a copy of 
 * the MPL was not distributed with this file, You 
 * can obtain one at https://mozilla.org/MPL/2.0/.
 */
package fr.efl.chaine.xslt.listener;

import fr.efl.chaine.xslt.ExecutionContext;
import fr.efl.chaine.xslt.GauloisPipe;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class has reponsability to initiate protocol listening, and to
 * be the commucation between {@link GauloisPipe} and protocal handler
 * (Servlet)
 * 
 * @author cmarchand
 * @since 1.00.01
 */
public class HttpListener {
    static final transient String CONTEXT_ENTRY = "CTX_KEY";
    static final transient String STOP_ENTRY = "STOP_KEY";
    static final transient String LISTENER = "__XX__LISTENER__XX__";
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpListener.class);
    private final int port;
    private final String stopKeyWord;
    private final ExecutionContext execCtx;
    private Server jettyServer;

    public HttpListener(int port, String stopKeyWord, ExecutionContext context) {
        super();
        this.port = port;
        this.stopKeyWord = stopKeyWord;
        this.execCtx = context;
    }
    
    public void run() {
        jettyServer = new Server(port);
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        context.getServletContext().setAttribute(STOP_ENTRY, stopKeyWord);
        context.getServletContext().setAttribute(CONTEXT_ENTRY, execCtx);
        context.getServletContext().setAttribute(LISTENER, this);
        context.setContextPath("");
        context.addServlet(new ServletHolder(ListenerServlet.class), "/*");
        
        
        jettyServer.setHandler(context);
        try {
            jettyServer.start();
            jettyServer.join();
        } catch(Exception ex) {
            LOGGER.error("While starting", ex);
            try {
                jettyServer.stop();
            } catch(Exception ex2) {
            } finally {
                jettyServer.destroy();
            }
        }
    }
    
    /**
     * This method stop the Listener
     * and returns when the server is actually stopped.
     * <b>Warning</b>: if the stop keyword has not been
     * received before, the service will not be terminated.
     */
    void stop() {
//        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
//        final ExecutionContext ctx = context;
        Runnable runner = new Runnable() {
            @Override
            public void run() {
                try {
                    jettyServer.stop();
                    try {
                        execCtx.getService().awaitTermination(5, TimeUnit.HOURS);
                    } catch (InterruptedException ex) {}
//                    Map<Thread,StackTraceElement[]> threads = Thread.getAllStackTraces();
//                    for(Thread t:threads.keySet()) {
//                        LOGGER.debug(t.getName());
//                        for(StackTraceElement s:threads.get(t)) {
//                            LOGGER.debug("\t"+s.toString());
//                        }
//                    }
                    LOGGER.info("asynchronous processing terminated, JVM is going to terminate");
                } catch(Exception ex) {
                    LOGGER.error("while stopping", ex);
                }
            }
        };
        new Thread(runner).start();
    }
}

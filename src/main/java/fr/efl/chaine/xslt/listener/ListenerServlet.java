/**
 * This Source Code Form is subject to the terms of 
 * the Mozilla Public License, v. 2.0. If a copy of 
 * the MPL was not distributed with this file, You 
 * can obtain one at https://mozilla.org/MPL/2.0/.
 */
package fr.efl.chaine.xslt.listener;

import fr.efl.chaine.xslt.ExecutionContext;
import fr.efl.chaine.xslt.GauloisRunException;
import fr.efl.chaine.xslt.InvalidSyntaxException;
import fr.efl.chaine.xslt.utils.ParameterValue;
import fr.efl.chaine.xslt.utils.ParametrableFile;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.concurrent.RejectedExecutionException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.sf.saxon.s9api.SaxonApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The servlet that process requests
 * @author cmarchand
 */
public class ListenerServlet extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(ListenerServlet.class);
    private ExecutionContext context;
    private String stopKeyword;
    private String sSizeLimit;

    @Override
    public void init() throws ServletException {
        super.init();
        context = (ExecutionContext)getServletContext().getAttribute(HttpListener.CONTEXT_ENTRY);
        stopKeyword = getServletContext().getAttribute(HttpListener.STOP_ENTRY).toString();
        if(context==null) {
            throw new ServletException("context must not be null");
        }
        if(!context.isValid()) {
            LOGGER.error("context is invalid: "+context.getErrorMessage());
            throw new ServletException(context.getErrorMessage());
        }
        if(stopKeyword==null || stopKeyword.length()==0) {
            throw new ServletException("stopKeyword must not be null or empty");
        }
        if(context.getPipe().getMultithreadMaxSourceSize()<1024) {
            sSizeLimit = Integer.toString(context.getPipe().getMultithreadMaxSourceSize())+"b";
        } else if(context.getPipe().getMultithreadMaxSourceSize()<(1024*1024)) {
            sSizeLimit = Integer.toString(context.getPipe().getMultithreadMaxSourceSize()/1024)+"kb";
        } else {
            sSizeLimit = Integer.toString(context.getPipe().getMultithreadMaxSourceSize()/1024/1024)+"mb";
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HashMap<String,ParameterValue> parameters = new HashMap<>();
        String sUrl=null;
        Enumeration<String> enumer = req.getParameterNames();
        while(enumer.hasMoreElements()) {
            String paramName = enumer.nextElement();
            if("url".equals(paramName)) {
                sUrl = req.getParameter(paramName);
            } else if(paramName!=null) {
                parameters.put(paramName,new ParameterValue(paramName, req.getParameter(paramName)));
            } else {
                LOGGER.info("received null parameter with value="+req.getParameter(paramName));
            }
        }
        if(sUrl==null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "url parameter must be provided");
        } else if(context.getService().isShutdown()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "The service has been shutdown, no other file will be accepted");
        } else {
            URL url = new URL(sUrl);
            if(!"file".equals(url.getProtocol())) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Only file: protocol are allowed.");
            } else {
                File inputFile = new File(sUrl.substring(5));
                try {
                    if(inputFile.length()>context.getPipe().getMultithreadMaxSourceSize()) {
                        resp.sendError(HttpServletResponse.SC_FORBIDDEN,sUrl+" size exceeds "+sSizeLimit);
                    } else {
                        // it is acceptable, will process it
                        final ExecutionContext iCtx = context;
                        final ParametrableFile fpf = new ParametrableFile(inputFile);
                        fpf.getParameters().putAll(parameters);
                        Runnable r = new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    iCtx.getGaulois().execute(iCtx.getPipe(), fpf, iCtx.getMsgListener());
                                } catch(SaxonApiException | MalformedURLException | InvalidSyntaxException | URISyntaxException | FileNotFoundException ex) {
                                    String msg = "[" + iCtx.getGaulois().getInstanceName() + "] while processing "+fpf.getFile().getName();
                                    LOGGER.error(msg, ex);
                                    iCtx.getGaulois().getErrors().add(new GauloisRunException(msg, fpf.getFile()));
                                }
                            }
                        };
                        try {
                            iCtx.getService().execute(r);
                            resp.getWriter().println("OK");
                            resp.getWriter().flush();
                            resp.getWriter().close();
                        } catch(RejectedExecutionException ex) {
                            resp.sendError(HttpServletResponse.SC_FORBIDDEN,ex.getMessage());
                        }
                    }
                } catch(IOException ex) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, sUrl+" is not a regular file");
                }
            }
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String keyword = req.getParameter("keyword");
        if(!stopKeyword.equals(keyword)) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "You are not authorized to terminate GauloisPipe. Do you have the correct keyword ?");
        } else {
            context.getService().shutdown();
            context.getGaulois().doPostCloseService(context);
            HttpListener listener = (HttpListener)getServletContext().getAttribute(HttpListener.LISTENER);
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().println("Server stopped");
            resp.getWriter().flush();
            resp.getWriter().close();
            listener.stop();
        }
    }
    
}

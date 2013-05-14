/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.catalina.core;

import org.apache.catalina.*;
import org.apache.catalina.connector.ClientAbortException;
import org.apache.catalina.deploy.ErrorPage;
import org.apache.catalina.util.ResponseUtil;
import org.apache.catalina.valves.ValveBase;
import org.glassfish.logging.annotation.LogMessageInfo;
import org.glassfish.web.valve.GlassFishValve;


import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ResourceBundle;
// END SJSAS 6374691

/**
 * Valve that implements the default basic behavior for the
 * <code>StandardHost</code> container implementation.
 * <p>
 * <b>USAGE CONSTRAINT</b>:  This implementation is likely to be useful only
 * when processing HTTP requests.
 *
 * @author Craig R. McClanahan
 * @author Remy Maucherat
 * @version $Revision: 1.21 $ $Date: 2007/04/10 17:12:22 $
 */

final class StandardHostValve
    extends ValveBase {

    private static final Logger log = StandardServer.log;
    private static final ResourceBundle rb = log.getResourceBundle();

    private static final ClassLoader standardHostValveClassLoader =
        StandardHostValve.class.getClassLoader();

    @LogMessageInfo(
        message = "Remote Client Aborted Request, IOException: {0}",
        level = "FINE"
    )
    public static final String REMOTE_CLIENT_ABORTED_EXCEPTION = "AS-WEB-CORE-00229";

    @LogMessageInfo(
        message = "The error-page {0} or {1} does not exist",
        level = "WARNING"
    )
    public static final String ERROR_PAGE_NOT_EXIST = "AS-WEB-CORE-00230";

    @LogMessageInfo(
        message = "No Context configured to process this request",
        level = "WARNING"
    )
    public static final String NO_CONTEXT_TO_PROCESS = "AS-WEB-CORE-00231";


    // ----------------------------------------------------- Instance Variables


    /**
     * The descriptive information related to this implementation.
     */
    private static final String info =
        "org.apache.catalina.core.StandardHostValve/1.0";


    // START SJSAS 6374691
    private GlassFishValve errorReportValve;
    // END SJSAS 6374691


    // ------------------------------------------------------------- Properties


    /**
     * Return descriptive information about this Valve implementation.
     */
    public String getInfo() {

        return (info);

    }


    // --------------------------------------------------------- Public Methods


    /**
     * Select the appropriate child Context to process this request,
     * based on the specified request URI.  If no matching Context can
     * be found, return an appropriate HTTP error.
     *
     * @param request Request to be processed
     * @param response Response to be produced
     *
     * @exception IOException if an input/output error occurred
     * @exception ServletException if a servlet error occurred
     */
    @Override
    public int invoke(Request request, Response response)
            throws IOException, ServletException {

        Context context = preInvoke(request, response);
        if (context == null) {
            return END_PIPELINE;
        }

        // Ask this Context to process this request
        if (context.getPipeline().hasNonBasicValves() ||
                context.hasCustomPipeline()) {
            context.getPipeline().invoke(request, response);
        } else {
            context.getPipeline().getBasic().invoke(request, response);
        }

        return END_PIPELINE;
    }


    /**
     * Tomcat style invocation.
     */
    @Override
    public void invoke(org.apache.catalina.connector.Request request,
                       org.apache.catalina.connector.Response response)
            throws IOException, ServletException {

        Context context = preInvoke(request, response);
        if (context == null) {
            return;
        }

        // Ask this Context to process this request
        if (context.getPipeline().hasNonBasicValves() ||
                context.hasCustomPipeline()) {
            context.getPipeline().invoke(request, response);
        } else {
            context.getPipeline().getBasic().invoke(request, response);
        }

        postInvoke(request, response);
    }


    @Override
    public void postInvoke(Request request, Response response)
        // START SJSAS 6374691
        throws IOException, ServletException
        // END SJSAS 6374691
    {
        try {
            /*
            // START SJSAS 6374990
            if (((ServletResponse) response).isCommitted()) {
                return;
            }
            // END SJSAS 6374990
            */

            HttpServletRequest hreq = (HttpServletRequest) request.getRequest();

            // Error page processing
            response.setSuspended(false);

            Throwable t = (Throwable) hreq.getAttribute(
                RequestDispatcher.ERROR_EXCEPTION);

            if (t != null) {
                throwable(request, response, t);
            } else {
                status(request, response);
            }

            // See IT 11423
            boolean isDefaultErrorPageEnabled = true;
            Wrapper wrapper = request.getWrapper();
            if (wrapper != null) {
                String initParam = wrapper.findInitParameter(Constants.IS_DEFAULT_ERROR_PAGE_ENABLED_INIT_PARAM);
                if (initParam != null) {
                    isDefaultErrorPageEnabled = Boolean.parseBoolean(initParam);
                }
            }

            // START SJSAS 6374691
            if (errorReportValve != null && response.isError() && isDefaultErrorPageEnabled) {
                errorReportValve.postInvoke(request, response);
            }
            // END SJSAS 6374691

            Context context = request.getContext();
            if (context != null) {
                context.fireRequestDestroyedEvent(hreq);
            }
        } finally {
            Thread.currentThread().setContextClassLoader
                (standardHostValveClassLoader);
        }
    }


    // ------------------------------------------------------ Protected Methods


    /**
     * Handle the specified Throwable encountered while processing
     * the specified Request to produce the specified Response.  Any
     * exceptions that occur during generation of the exception report are
     * logged and swallowed.
     *
     * @param request The request being processed
     * @param response The response being generated
     * @param throwable The throwable that occurred (which possibly wraps
     *  a root cause exception
     */
    protected void throwable(Request request, Response response,
                             Throwable throwable) {
        Context context = request.getContext();
        if (context == null)
            return;
        
        Throwable realError = throwable;
        if (realError instanceof ServletException) {
            realError = ((ServletException) realError).getRootCause();
            if (realError == null) {
                realError = throwable;
            }
        } 

        // If this is an aborted request from a client just log it and return
        if (realError instanceof ClientAbortException ) {
            if (log.isLoggable(Level.FINE)) {
                log.log(Level.FINE, REMOTE_CLIENT_ABORTED_EXCEPTION, realError.getCause().getMessage());
            }
            return;
        }

        ErrorPage errorPage = findErrorPage(context, throwable);
        if ((errorPage == null) && (realError != throwable)) {
            errorPage = findErrorPage(context, realError);
        }

        if (errorPage != null) {
            dispatchToErrorPage(request, response, errorPage, throwable,
                                realError, 0);
        } else if (context.getDefaultErrorPage() != null) {
            dispatchToErrorPage(request, response,
                context.getDefaultErrorPage(), throwable, realError, 0);  
        } else {
            // A custom error-page has not been defined for the exception
            // that was thrown during request processing. Check if an
            // error-page for error code 500 was specified and if so, 
            // send that page back as the response.
            ServletResponse sresp = (ServletResponse) response;

            /* GlassFish 6386229
            if (sresp instanceof HttpServletResponse) {
                ((HttpServletResponse) sresp).setStatus(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                // The response is an error
                response.setError();

                status(request, response);
            }
            */
            // START GlassFish 6386229
            ((HttpServletResponse) sresp).setStatus(
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            // The response is an error
            response.setError();

            status(request, response);
            // END GlassFish 6386229
        }
            

    }


    /**
     * Handle the HTTP status code (and corresponding message) generated
     * while processing the specified Request to produce the specified
     * Response.  Any exceptions that occur during generation of the error
     * report are logged and swallowed.
     *
     * @param request The request being processed
     * @param response The response being generated
     */
    protected void status(Request request, Response response) {

        // Handle a custom error page for this status code
        Context context = request.getContext();
        if (context == null)
            return;

        /*
         * Only look for error pages when isError() is set.
         * isError() is set when response.sendError() is invoked.
         */
        if (!response.isError()) {
            return;
        }

        int statusCode = ((HttpResponse) response).getStatus();
        ErrorPage errorPage = context.findErrorPage(statusCode);
        if (errorPage != null) {
            if (errorPage.getLocation() != null) {
                File file = new File(context.getDocBase(), errorPage.getLocation());
                if (!file.exists()) {
                    File file2 = new File(errorPage.getLocation());
                    if (!file2.exists()) {
                        log.log(Level.WARNING, ERROR_PAGE_NOT_EXIST,
                                new Object[]{file.getAbsolutePath(), file2.getAbsolutePath()});
                    }
                }
            }
            setErrorPageContentType(response, errorPage.getLocation(), context);
            dispatchToErrorPage(request, response, errorPage, null, null, statusCode);
        } else if (statusCode >= 400 && statusCode < 600 &&
                context.getDefaultErrorPage() != null) {
            dispatchToErrorPage(request, response,
                context.getDefaultErrorPage(), null, null, statusCode);
        }
        // START SJSAS 6324911
        else {
            errorPage = ((StandardHost) getContainer()).findErrorPage(
                                                        statusCode);
            if (errorPage != null) {
                if (errorPage.getLocation() != null) {
                    File file = new File(context.getDocBase(), errorPage.getLocation());
                    if (!file.exists()) { 
                        File file2 = new File(errorPage.getLocation());
                        if (!file2.exists()) {
                            log.log(Level.WARNING, ERROR_PAGE_NOT_EXIST,
                                    new Object[]{file.getAbsolutePath(), file2.getAbsolutePath()});
                        }
                    }
                }
                try {
                    setErrorPageContentType(response, errorPage.getLocation(), context);
                    handleHostErrorPage(response, errorPage, statusCode);
                } catch (Exception e) {
                    log("Exception processing " + errorPage, e);
                }
            }
        }
        // END SJSAS 6324911
    }


    /**
     * Find and return the ErrorPage instance for the specified exception's
     * class, or an ErrorPage instance for the closest superclass for which
     * there is such a definition.  If no associated ErrorPage instance is
     * found, return <code>null</code>.
     *
     * @param context The Context in which to search
     * @param exception The exception for which to find an ErrorPage
     */
    protected static ErrorPage findErrorPage
        (Context context, Throwable exception) {

        if (exception == null)
            return (null);
        Class<?> clazz = exception.getClass();
        String name = clazz.getName();
        while (!Object.class.equals(clazz)) {
            ErrorPage errorPage = context.findErrorPage(name);
            if (errorPage != null)
                return (errorPage);
            clazz = clazz.getSuperclass();
            if (clazz == null)
                break;
            name = clazz.getName();
        }
        return (null);

    }


    /**
     * Handle an HTTP status code or Java exception by forwarding control
     * to the location included in the specified errorPage object.  It is
     * assumed that the caller has already recorded any request attributes
     * that are to be forwarded to this page.  Return <code>true</code> if
     * we successfully utilized the specified error page location, or
     * <code>false</code> if the default error report should be rendered.
     *
     * @param request The request being processed
     * @param response The response being generated
     * @param errorPage The errorPage directive we are obeying
     */
    protected boolean custom(Request request, Response response,
                             ErrorPage errorPage) {

        if (debug >= 1)
            log("Processing " + errorPage);

        // Validate our current environment
        /* GlassFish 6386229
        if (!(request instanceof HttpRequest)) {
            if (debug >= 1)
                log(" Not processing an HTTP request --> default handling");
            return (false);     // NOTE - Nothing we can do generically
        }
        */
        HttpServletRequest hreq =
            (HttpServletRequest) request.getRequest();
        /* GlassFish 6386229
        if (!(response instanceof HttpResponse)) {
            if (debug >= 1)
                log("Not processing an HTTP response --> default handling");
            return (false);     // NOTE - Nothing we can do generically
        }
        */
        HttpServletResponse hres =
            (HttpServletResponse) response.getResponse();

        ((HttpRequest) request).setPathInfo(errorPage.getLocation());

        try {
            Integer statusCodeObj = (Integer) hreq.getAttribute(
                RequestDispatcher.ERROR_STATUS_CODE);
            int statusCode = statusCodeObj.intValue();
            String message = (String) hreq.getAttribute(
                RequestDispatcher.ERROR_MESSAGE);
            hres.setStatus(statusCode, message);

            // Forward control to the specified location
            ServletContext servletContext =
                request.getContext().getServletContext();
            ApplicationDispatcher dispatcher = (ApplicationDispatcher)
                servletContext.getRequestDispatcher(errorPage.getLocation());

            if (hres.isCommitted()) {
                // Response is committed - including the error page is the
                // best we can do 
                dispatcher.include(hreq, hres);
            } else {
                // Reset the response (keeping the real error code and message)
                response.resetBuffer(true);

                dispatcher.dispatch(hreq, hres, DispatcherType.ERROR);

                // If we forward, the response is suspended again
                response.setSuspended(false);
            }

            // Indicate that we have successfully processed this custom page
            return (true);

        } catch (Throwable t) {
            // Report our failure to process this custom page
            log("Exception Processing " + errorPage, t);
            return (false);
        }

    }


    /**
     * Log a message on the Logger associated with our Container (if any).
     *
     * @param message Message to be logged
     */
    protected void log(String message) {
        org.apache.catalina.Logger logger = container.getLogger();
        if (logger != null) {
            logger.log(this.toString() + ": " + message);
        } else {
            if (log.isLoggable(Level.INFO)) {
                log.log(Level.INFO, this.toString() + ": " + message);
            }
        }
    }


    /**
     * Log a message on the Logger associated with our Container (if any).
     *
     * @param message Message to be logged
     * @param t Associated exception
     */
    protected void log(String message, Throwable t) {
        org.apache.catalina.Logger logger = container.getLogger();
        if (logger != null) {
            logger.log(this.toString() + ": " + message, t,
                org.apache.catalina.Logger.WARNING);
        } else {
            log.log(Level.WARNING, this.toString() + ": " + message, t);
        }
    }


    // START SJSAS 6324911
    /**
     * Copies the contents of the given error page to the response, and
     * updates the status message with the reason string of the error page.
     *
     * @param response The response object
     * @param errorPage The error page whose contents are to be copied
     * @param statusCode The status code
     */
    private void handleHostErrorPage(Response response,
                                     ErrorPage errorPage,
                                     int statusCode)
            throws Exception {

        ServletOutputStream ostream = null;
        PrintWriter writer = null;
        FileReader reader = null;
        BufferedInputStream istream = null;
        IOException ioe = null;

        if (!response.getResponse().isCommitted()) {
            response.resetBuffer(true);
        }
        String message = errorPage.getReason();
        if (message != null) {
            ((HttpResponse) response).reset(statusCode, message);
        }
         
        try {
            ostream = response.getResponse().getOutputStream();
        } catch (IllegalStateException e) {
            // If it fails, we try to get a Writer instead if we're
            // trying to serve a text file
            writer = response.getResponse().getWriter();
        }

        if (writer != null) {
            reader = new FileReader(errorPage.getLocation());
            ioe = ResponseUtil.copy(reader, writer);
            try {
                reader.close();
            } catch (Throwable t) {
                ;
            }
        } else {
            istream = new BufferedInputStream(
                new FileInputStream(errorPage.getLocation()));
            ioe = ResponseUtil.copy(istream, ostream);
            try {
                istream.close();
            } catch (Throwable t) {
                ;
            }
        }

        // Rethrow any exception that may have occurred
        if (ioe != null) {
            throw ioe;
        }
    }
    // END SJSAS 6324911


    // START SJSAS 6374691
    void setErrorReportValve(GlassFishValve errorReportValve) {
        this.errorReportValve = errorReportValve;
    }
    // END SJSAS 6374691


    private Context preInvoke(Request request, Response response)
            throws IOException, ServletException {

        // Select the Context to be used for this Request
        Context context = request.getContext();
        if (context == null) {
            // BEGIN S1AS 4878272
            ((HttpServletResponse) response.getResponse()).sendError
                (HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setDetailMessage(rb.getString(NO_CONTEXT_TO_PROCESS));
            // END S1AS 4878272
            return null;
        }

        // Bind the context CL to the current thread
        if( context.getLoader() != null ) {
            // Not started - it should check for availability first
            // This should eventually move to Engine, it's generic.
            Thread.currentThread().setContextClassLoader
                    (context.getLoader().getClassLoader());
        }
                 
        // START GlassFish Issue 1057
        // Update the session last access time for our session (if any)
        HttpServletRequest hreq = (HttpServletRequest) request.getRequest();
        hreq.getSession(false);
        // END GlassFish Issue 1057

        context.fireRequestInitializedEvent(hreq);

        return context;
    }


    private void dispatchToErrorPage(Request request, Response response,
            ErrorPage errorPage, Throwable throwable, Throwable realError,
            int statusCode) {

        response.setAppCommitted(false);

        ServletRequest sreq = request.getRequest();
        ServletResponse sresp = response.getResponse();

        sreq.setAttribute(Globals.DISPATCHER_REQUEST_PATH_ATTR,
                          errorPage.getLocation());
        sreq.setAttribute(RequestDispatcher.ERROR_REQUEST_URI,
                          ((HttpServletRequest) sreq).getRequestURI());
        Wrapper wrapper = request.getWrapper();
        if (wrapper != null) {
            sreq.setAttribute(RequestDispatcher.ERROR_SERVLET_NAME,
                              wrapper.getName());
        }

        if (throwable != null) {
            sreq.setAttribute(RequestDispatcher.ERROR_STATUS_CODE,
                Integer.valueOf(HttpServletResponse.SC_INTERNAL_SERVER_ERROR));
            sreq.setAttribute(RequestDispatcher.ERROR_MESSAGE,
                              throwable.getMessage());
            sreq.setAttribute(RequestDispatcher.ERROR_EXCEPTION,
                              realError);
            sreq.setAttribute(RequestDispatcher.ERROR_EXCEPTION_TYPE,
                              realError.getClass());
        } else {
            sreq.setAttribute(RequestDispatcher.ERROR_STATUS_CODE,
                              Integer.valueOf(statusCode));
            String message = ((HttpResponse) response).getMessage();
            if (message == null) {
                message = "";
            }
            sreq.setAttribute(RequestDispatcher.ERROR_MESSAGE, message);
        }

        if (custom(request, response, errorPage)) {
            try {
                sresp.flushBuffer();
            } catch (IOException e) {
                log("Exception processing " + errorPage, e);
            }
        }
    }

    private void setErrorPageContentType(Response response,
                                         String location, Context context) {

        if (response.getContentType() == null && location != null) {
            String str = location.substring(location.lastIndexOf('.') + 1);
            str = context.findMimeMapping(str.toLowerCase(Locale.ENGLISH));
            if(str != null)
                ((ServletResponse) response).setContentType(str);
        }
    }

}

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

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Context;
import org.apache.catalina.Globals;
import org.apache.catalina.HttpRequest;
import org.apache.catalina.Request;
import org.apache.catalina.Response;
import org.apache.catalina.connector.ClientAbortException;
import org.apache.catalina.connector.RequestFacade;
import org.apache.catalina.valves.ValveBase;
import org.glassfish.grizzly.http.util.DataChunk;
import org.glassfish.logging.annotation.LogMessageInfo;

/**
 * Valve that implements the default basic behavior for the
 * <code>StandardWrapper</code> container implementation.
 *
 * @author Craig R. McClanahan
 * @version $Revision: 1.10 $ $Date: 2007/05/05 05:31:54 $
 */

final class StandardWrapperValve extends ValveBase {

    private static final Logger log = StandardServer.log;
    private static final ResourceBundle rb = log.getResourceBundle();

    @LogMessageInfo(
        message = "This application is not currently available",
        level = "WARNING"
    )
    public static final String APP_UNAVAILABLE = "AS-WEB-CORE-00274";

    @LogMessageInfo(
        message = "Servlet {0} is currently unavailable",
        level = "WARNING"
    )
    public static final String SERVLET_UNAVAILABLE  = "AS-WEB-CORE-00275";

    @LogMessageInfo(
        message = "Servlet {0} is not available",
        level = "WARNING"
    )
    public static final String SERVLET_NOT_FOUND = "AS-WEB-CORE-00276";

    @LogMessageInfo(
        message = "Allocate exception for servlet {0}",
        level = "WARNING"
    )
    public static final String SERVLET_ALLOCATE_EXCEPTION = "AS-WEB-CORE-00277";

    @LogMessageInfo(
        message = "Exception for sending acknowledgment of a request: {0}",
        level = "WARNING"
    )
    public static final String SEND_ACKNOWLEDGEMENT_EXCEPTION = "AS-WEB-CORE-00278";

    @LogMessageInfo(
        message = "Servlet.service() for servlet {0} threw exception",
        level = "WARNING"
    )
    public static final String SERVLET_SERVICE_EXCEPTION = "AS-WEB-CORE-00279";

    @LogMessageInfo(
        message = "Release filters exception for servlet {0}",
        level = "WARNING"
    )
    public static final String RELEASE_FILTERS_EXCEPTION = "AS-WEB-CORE-00280";

    @LogMessageInfo(
        message = "Deallocate exception for servlet {0}",
        level = "WARNING"
    )
    public static final String DEALLOCATE_EXCEPTION = "AS-WEB-CORE-00281";

    @LogMessageInfo(
        message = "Servlet {0} threw unload() exception",
        level = "WARNING"
    )
    public static final String SERVLET_UNLOAD_EXCEPTION = "AS-WEB-CORE-00282";

    @LogMessageInfo(
        message = "StandardWrapperValve[{0}]: {1}",
        level = "INFO"
    )
    public static final String STANDARD_WRAPPER_VALVE = "AS-WEB-CORE-00283";

    // --------------------------------------------------------- Public Methods


    /**
     * Invoke the servlet we are managing, respecting the rules regarding
     * servlet lifecycle and SingleThreadModel support.
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

        boolean unavailable = false;
        Throwable throwable = null;
        Servlet servlet = null;

        StandardWrapper wrapper = (StandardWrapper) getContainer();
        Context context = (Context) wrapper.getParent();

        HttpRequest hrequest = (HttpRequest) request;

        /*
         * Create a request facade such that if the request was received
         * at the root context, and the root context is mapped to a
         * default-web-module, the default-web-module mapping is masked from
         * the application code to which the request facade is being passed.
         * For example, the request.facade's getContextPath() method will 
         * return "/", rather than the context root of the default-web-module,
         * in this case.
         */
        RequestFacade hreq = (RequestFacade) request.getRequest(true);
        HttpServletResponse hres =
            (HttpServletResponse) response.getResponse();

        // Check for the application being marked unavailable
        if (!context.getAvailable()) {
            // BEGIN S1AS 4878272
            hres.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.setDetailMessage(rb.getString(APP_UNAVAILABLE));
            // END S1AS 4878272
            unavailable = true;
        }

        // Check for the servlet being marked unavailable
        if (!unavailable && wrapper.isUnavailable()) {
            String msg = MessageFormat.format(rb.getString(SERVLET_UNAVAILABLE), wrapper.getName());
            log(msg);
            if (hres == null) {
                ;       // NOTE - Not much we can do generically
            } else {
                long available = wrapper.getAvailable();
                if ((available > 0L) && (available < Long.MAX_VALUE)) {
                    hres.setDateHeader("Retry-After", available);
                    // BEGIN S1AS 4878272
                    hres.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);

                    response.setDetailMessage(msg);
                    // END S1AS 4878272
                } else if (available == Long.MAX_VALUE) {
                    // BEGIN S1AS 4878272
                    hres.sendError(HttpServletResponse.SC_NOT_FOUND);
                    msg = MessageFormat.format(rb.getString(SERVLET_NOT_FOUND), wrapper.getName());
                    response.setDetailMessage(msg);
                    // END S1AS 4878272
                }
            }
            unavailable = true;
        }

        // Allocate a servlet instance to process this request
        try {
            if (!unavailable) {
                servlet = wrapper.allocate();
            }
        } catch (UnavailableException e) {
            if (e.isPermanent()) {
                // BEGIN S1AS 4878272
                hres.sendError(HttpServletResponse.SC_NOT_FOUND);

                String msg = MessageFormat.format(rb.getString(SERVLET_NOT_FOUND), wrapper.getName());
                response.setDetailMessage(msg);

                // END S1AS 4878272
            } else {
                hres.setDateHeader("Retry-After", e.getUnavailableSeconds());
                // BEGIN S1AS 4878272
                hres.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                String msg = MessageFormat.format(rb.getString(SERVLET_UNAVAILABLE), wrapper.getName());
                response.setDetailMessage(msg);
                // END S1AS 4878272
            }
        } catch (ServletException e) {

            String msg = MessageFormat.format(rb.getString(SERVLET_ALLOCATE_EXCEPTION), wrapper.getName());
            log(msg, StandardWrapper.getRootCause(e));

            throwable = e;
            exception(request, response, e);
            servlet = null;
        } catch (Throwable e) {

            String msg = MessageFormat.format(rb.getString(SERVLET_ALLOCATE_EXCEPTION), wrapper.getName());
            log(msg, e);

            throwable = e;
            exception(request, response, e);
            servlet = null;
        }

        // Acknowlege the request
        try {
            response.sendAcknowledgement();
        } catch (IOException e) {
            String msg = MessageFormat.format(rb.getString(SEND_ACKNOWLEDGEMENT_EXCEPTION), wrapper.getName());
            log(msg, e);
            throwable = e;
            exception(request, response, e);
        } catch (Throwable e) {
            String msg = MessageFormat.format(rb.getString(SEND_ACKNOWLEDGEMENT_EXCEPTION), wrapper.getName());
            log(msg, e);
            throwable = e;
            exception(request, response, e);
            servlet = null;
        }
        DataChunk requestPathMB = hrequest.getRequestPathMB();
        hreq.setAttribute(Globals.DISPATCHER_REQUEST_PATH_ATTR,
                          requestPathMB);

        // Create the filter chain for this request
        ApplicationFilterFactory factory =
            ApplicationFilterFactory.getInstance();
        ApplicationFilterChain filterChain =
            factory.createFilterChain((ServletRequest) request,
                                      wrapper, servlet);

        // Call the filter chain for this request
        // NOTE: This also calls the servlet's service() method
        try {
            String jspFile = wrapper.getJspFile();
            if (jspFile != null) {
                hreq.setAttribute(Globals.JSP_FILE_ATTR, jspFile);
            } 
            /* IASRI 4665318
            if ((servlet != null) && (filterChain != null)) {
                filterChain.doFilter(hreq, hres);
            }
            */
            // START IASRI 4665318
            if (servlet != null) {
                if (filterChain != null) {
                    filterChain.setWrapper(wrapper);
                    filterChain.doFilter(hreq, hres);
                } else {
                    wrapper.service(hreq, hres, servlet);
                }
            }
            // END IASRI 4665318
        } catch (ClientAbortException e) {
            throwable = e;
            exception(request, response, e);
        } catch (IOException e) {
            String msg = MessageFormat.format(rb.getString(SERVLET_SERVICE_EXCEPTION), wrapper.getName());
            log(msg, e);
            throwable = e;
            exception(request, response, e);
        } catch (UnavailableException e) {
            String msg = MessageFormat.format(rb.getString(SERVLET_SERVICE_EXCEPTION), wrapper.getName());
            log(msg, e);
            //            throwable = e;
            //            exception(request, response, e);
            wrapper.unavailable(e);
            long available = wrapper.getAvailable();
            if ((available > 0L) && (available < Long.MAX_VALUE)) {
                hres.setDateHeader("Retry-After", available);
                // BEGIN S1AS 4878272
                hres.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                String msgServletUnavailable = MessageFormat.format(rb.getString(SERVLET_UNAVAILABLE), wrapper.getName());
                response.setDetailMessage(msgServletUnavailable);
                // END S1AS 4878272
            } else if (available == Long.MAX_VALUE) {
                // BEGIN S1AS 4878272
                hres.sendError(HttpServletResponse.SC_NOT_FOUND);
                String msgServletNotFound = MessageFormat.format(rb.getString(SERVLET_NOT_FOUND), wrapper.getName());
                response.setDetailMessage(msgServletNotFound);
                // END S1AS 4878272
            }
            // Do not save exception in 'throwable', because we
            // do not want to do exception(request, response, e) processing
        } catch (ServletException e) {
            Throwable rootCause = StandardWrapper.getRootCause(e);
            if (!(rootCause instanceof ClientAbortException)) {
                String msg = MessageFormat.format(rb.getString(SERVLET_SERVICE_EXCEPTION), wrapper.getName());
                log(msg, rootCause);
            }
            throwable = e;
            exception(request, response, e);
        } catch (Throwable e) {
            String msg = MessageFormat.format(rb.getString(SERVLET_SERVICE_EXCEPTION), wrapper.getName());
            log(msg, e);
            throwable = e;
            exception(request, response, e);
        }

        // Release the filter chain (if any) for this request
        try {
            if (filterChain != null)
                filterChain.release();
        } catch (Throwable e) {
            String msg = MessageFormat.format(rb.getString(RELEASE_FILTERS_EXCEPTION), wrapper.getName());
            log(msg, e);
            if (throwable == null) {
                throwable = e;
                exception(request, response, e);
            }
        }

        // Deallocate the allocated servlet instance
        try {
            if (servlet != null) {
                wrapper.deallocate(servlet);
            }
        } catch (Throwable e) {
            String msg = MessageFormat.format(rb.getString(DEALLOCATE_EXCEPTION), wrapper.getName());
            log(msg, e);
            if (throwable == null) {
                throwable = e;
                exception(request, response, e);
            }
        }

        // If this servlet has been marked permanently unavailable,
        // unload it and release this instance
        try {
            if ((servlet != null) &&
                (wrapper.getAvailable() == Long.MAX_VALUE)) {
                wrapper.unload();
            }
        } catch (Throwable e) {
            String msg = MessageFormat.format(rb.getString(SERVLET_UNLOAD_EXCEPTION), wrapper.getName());
            log(msg, e);
            if (throwable == null) {
                exception(request, response, e);
            }
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
        invoke((Request) request, (Response) response);
    }


    // -------------------------------------------------------- Private Methods


    /**
     * Log a message on the Logger associated with our Container (if any)
     *
     * @param message Message to be logged
     */
    private void log(String message) {
        org.apache.catalina.Logger logger = null;
        String containerName = null;
        if (container != null) {
            logger = container.getLogger();
            containerName = container.getName();
        }
        if (logger != null) {
            logger.log("StandardWrapperValve[" + containerName + "]: " +
                       message);
        } else {
            if (log.isLoggable(Level.INFO)) {
                log.log(Level.INFO, STANDARD_WRAPPER_VALVE, new Object[] {containerName, message});
            }
        }
    }


    /**
     * Log a message on the Logger associated with our Container (if any)
     *
     * @param message Message to be logged
     * @param t Associated exception
     */
    private void log(String message, Throwable t) {
        org.apache.catalina.Logger logger = null;
        String containerName = null;
        if (container != null) {
            logger = container.getLogger();
            containerName = container.getName();
        }
        if (logger != null) {
            logger.log("StandardWrapperValve[" + containerName + "]: " +
                message, t, org.apache.catalina.Logger.WARNING);
        } else {
            String msg = MessageFormat.format(rb.getString(STANDARD_WRAPPER_VALVE),
                                              new Object[] {containerName, message});
            log.log(Level.WARNING, msg, t);
        }
    }


    /**
     * Handle the specified ServletException encountered while processing
     * the specified Request to produce the specified Response.  Any
     * exceptions that occur during generation of the exception report are
     * logged and swallowed.
     *
     * @param request The request being processed
     * @param response The response being generated
     * @param exception The exception that occurred (which possibly wraps
     *  a root cause exception
     */
    private void exception(Request request, Response response,
                           Throwable exception) {
        ServletRequest sreq = request.getRequest();
        sreq.setAttribute(RequestDispatcher.ERROR_EXCEPTION, exception);

        ServletResponse sresponse = response.getResponse();
        
        /* GlassFish 6386229
        if (sresponse instanceof HttpServletResponse)
            ((HttpServletResponse) sresponse).setStatus
                (HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        */
        // START GlassFish 6386229
        ((HttpServletResponse) sresponse).setStatus
            (HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        // END GlassFish 6386229
    }

    // Don't register in JMX

    public ObjectName createObjectName(String domain, ObjectName parent)
            throws MalformedObjectNameException {
        return null;
    }
}

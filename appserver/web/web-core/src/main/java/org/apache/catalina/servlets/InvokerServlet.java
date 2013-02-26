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

package org.apache.catalina.servlets;


import org.apache.catalina.ContainerServlet;
import org.apache.catalina.Context;
import org.apache.catalina.Globals;
import org.apache.catalina.Wrapper;
import org.apache.catalina.core.StandardServer;
import org.apache.catalina.util.StringManager;
import org.glassfish.logging.annotation.LogMessageInfo;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ResourceBundle;


/**
 * The default servlet-invoking servlet for most web applications,
 * used to serve requests to servlets that have not been registered
 * in the web application deployment descriptor.
 *
 * @author Craig R. McClanahan
 * @version $Revision: 1.3 $ $Date: 2005/12/08 01:27:56 $
 */

public final class InvokerServlet
    extends HttpServlet implements ContainerServlet {

    public static final ResourceBundle rb = StandardServer.log.getResourceBundle();

    @LogMessageInfo(
            message = "Container has not called setWrapper() for this servlet",
            level = "WARNING"
    )
    public static final String SET_WRAPPER_NOT_CALLED_EXCEPTION = "AS-WEB-CORE-00329";

    @LogMessageInfo(
            message = "Cannot call invoker servlet with a named dispatcher",
            level = "WARNING"
    )
    public static final String CANNOT_CALL_INVOKER_SERVLET = "AS-WEB-CORE-00330";

    @LogMessageInfo(
            message = "No servlet name or class was specified in path {0}",
            level = "WARNING"
    )
    public static final String INVALID_PATH_EXCEPTION = "AS-WEB-CORE-00331";

    @LogMessageInfo(
            message = "Cannot create servlet wrapper for path {0}",
            level = "WARNING"
    )
    public static final String CANNOT_CREATE_SERVLET_WRAPPER_EXCEPTION = "AS-WEB-CORE-00332";

    @LogMessageInfo(
            message = "Cannot allocate servlet instance for path {0}",
            level = "WARNING"
    )
    public static final String CANNOT_ALLOCATE_SERVLET_INSTANCE_EXCEPTION = "AS-WEB-CORE-00333";

    @LogMessageInfo(
            message = "Cannot deallocate servlet instance for path {0}",
            level = "WARNING"
    )
    public static final String CANNOT_DEALLOCATE_SERVLET_INSTANCE_EXCEPTION = "AS-WEB-CORE-00334";


    // ----------------------------------------------------- Instance Variables


    /**
     * The Context container associated with our web application.
     */
    private Context context = null;


    /**
     * The debugging detail level for this servlet.
     */
    private int debug = 0;


    /**
     * The Wrapper container associated with this servlet.
     */
    private Wrapper wrapper = null;


    // ----------------------------------------------- ContainerServlet Methods


    /**
     * Return the Wrapper with which we are associated.
     */
    public synchronized Wrapper getWrapper() {

        return (this.wrapper);

    }


    /**
     * Set the Wrapper with which we are associated.
     *
     * @param wrapper The new wrapper
     */
    public synchronized void setWrapper(Wrapper wrapper) {
        this.wrapper = wrapper;
        if (wrapper == null)
            context = null;
        else
            context = (Context) wrapper.getParent();
    }


    // --------------------------------------------------------- Public Methods


    /**
     * Finalize this servlet.
     */
    public void destroy() {

        ;       // No actions necessary

    }


    /**
     * Process a GET request for the specified resource.
     *
     * @param request The servlet request we are processing
     * @param response The servlet response we are creating
     *
     * @exception IOException if an input/output error occurs
     * @exception ServletException if a servlet-specified error occurs
     */
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response)
        throws IOException, ServletException {

        serveRequest(request, response);

    }


    /**
     * Process a HEAD request for the specified resource.
     *
     * @param request The servlet request we are processing
     * @param response The servlet response we are creating
     *
     * @exception IOException if an input/output error occurs
     * @exception ServletException if a servlet-specified error occurs
     */
    public void doHead(HttpServletRequest request,
                       HttpServletResponse response)
        throws IOException, ServletException {

        serveRequest(request, response);

    }


    /**
     * Process a POST request for the specified resource.
     *
     * @param request The servlet request we are processing
     * @param response The servlet response we are creating
     *
     * @exception IOException if an input/output error occurs
     * @exception ServletException if a servlet-specified error occurs
     */
    public void doPost(HttpServletRequest request,
                       HttpServletResponse response)
        throws IOException, ServletException {

        serveRequest(request, response);

    }


    /**
     * Initialize this servlet.
     */
    public synchronized void init() throws ServletException {

        // Ensure that our ContainerServlet properties have been set
        if ((wrapper == null) || (context == null))
            throw new UnavailableException
                (rb.getString(SET_WRAPPER_NOT_CALLED_EXCEPTION));

        // Set our properties from the initialization parameters
        String value = null;
        try {
            value = getServletConfig().getInitParameter("debug");
            debug = Integer.parseInt(value);
        } catch (Throwable t) {
            ;
        }
        if (debug >= 1)
            log("init: Associated with Context '" + context.getPath() + "'");

    }



    // -------------------------------------------------------- Private Methods


    /**
     * Serve the specified request, creating the corresponding response.
     * After the first time a particular servlet class is requested, it will
     * be served directly (like any registered servlet) because it will have
     * been registered and mapped in our associated Context.
     *
     * <p>Synchronize to avoid race conditions when multiple requests
     * try to initialize the same servlet at the same time
     *
     * @param request The servlet request we are processing
     * @param response The servlet response we are creating
     *
     * @exception IOException if an input/output error occurs
     * @exception ServletException if a servlet-specified error occurs
     */
    public synchronized void serveRequest(HttpServletRequest request,
            HttpServletResponse response)
        throws IOException, ServletException {

        // Disallow calling this servlet via a named dispatcher
        if (request.getAttribute(Globals.NAMED_DISPATCHER_ATTR) != null)
            throw new ServletException
                (rb.getString(CANNOT_CALL_INVOKER_SERVLET));

        // Identify the input parameters and our "included" state
        String inRequestURI = null;
        String inServletPath = null;
        String inPathInfo = null;
        boolean included =
            (request.getAttribute(RequestDispatcher.INCLUDE_REQUEST_URI) != null);

        if (included) {
            inRequestURI = (String) request.getAttribute(
                RequestDispatcher.INCLUDE_REQUEST_URI);
            inServletPath = (String) request.getAttribute(
                RequestDispatcher.INCLUDE_SERVLET_PATH);
            inPathInfo = (String) request.getAttribute(
                RequestDispatcher.INCLUDE_PATH_INFO);
        } else {
            inRequestURI = request.getRequestURI();
            inServletPath = request.getServletPath();
            inPathInfo = request.getPathInfo();
        }
        if (debug >= 1) {
            log("included='" + included + "', requestURI='" +
                inRequestURI + "'");
            log("  servletPath='" + inServletPath + "', pathInfo='" +
                inPathInfo + "'");
        }

        // Make sure a servlet name or class name was specified
        if (inPathInfo == null) {
            if (debug >= 1)
                log("Invalid pathInfo 'null'");
            String msg = MessageFormat.format(rb.getString(INVALID_PATH_EXCEPTION),
                    inRequestURI);
            if (included) {
                throw new ServletException(msg);
            }
            else {
                /* IASRI 4878272
                response.sendError(HttpServletResponse.SC_NOT_FOUND,
                                   inRequestURI);
                */
                // BEGIN IASRI 4878272
                log(msg);
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                // END IASRI 4878272
                return;
            }
        }

        // Identify the outgoing servlet name or class, and outgoing path info
        String pathInfo = inPathInfo;
        String servletClass = pathInfo.substring(1);
        int slash = servletClass.indexOf('/');
        //        if (debug >= 2)
        //            log("  Calculating with servletClass='" + servletClass +
        //                "', pathInfo='" + pathInfo + "', slash=" + slash);
        if (slash >= 0) {
            pathInfo = servletClass.substring(slash);
            servletClass = servletClass.substring(0, slash);
        } else {
            pathInfo = "";
        }

        if (servletClass.startsWith("org.apache.catalina")) {
            /* IASRI 4878272
            response.sendError(HttpServletResponse.SC_NOT_FOUND,
                               inRequestURI);
            */
            // BEGIN IASRI 4878272
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            // END IASRI 4878272
            return;
        }

        if (debug >= 1)
            log("Processing servlet '" + servletClass +
                "' with path info '" + pathInfo + "'");
        String name = "org.apache.catalina.INVOKER." + servletClass;
        String pattern = inServletPath + "/" + servletClass + "/*";
        Wrapper wrapper = null;

        // Are we referencing an existing servlet class or name?
        wrapper = (Wrapper) context.findChild(servletClass);
        if (wrapper == null)
            wrapper = (Wrapper) context.findChild(name);
        if (wrapper != null) {
            String actualServletClass = wrapper.getServletClassName();
            if ((actualServletClass != null)
                && (actualServletClass.startsWith
                    ("org.apache.catalina"))) {
                /* IASRI 4878272
                response.sendError(HttpServletResponse.SC_NOT_FOUND,
                                   inRequestURI);
                */
                // BEGIN IASRI 4878272
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                // END IASRI 4878272
                return;
            }
            if (debug >= 1)
                log("Using wrapper for servlet '" +
                    wrapper.getName() + "' with mapping '" +
                    pattern + "'");
            context.addServletMapping(pattern, wrapper.getName());
        }

        // No, create a new wrapper for the specified servlet class
        else {
            if (debug >= 1)
                log("Creating wrapper for '" + servletClass +
                    "' with mapping '" + pattern + "'");
            try {
                wrapper = context.createWrapper();
                wrapper.setName(name);
                wrapper.setLoadOnStartup(1);
                wrapper.setServletClassName(servletClass);
                context.addChild(wrapper);
                context.addServletMapping(pattern, name);
            } catch (Throwable t) {
                String msg = MessageFormat.format(rb.getString(CANNOT_CREATE_SERVLET_WRAPPER_EXCEPTION),
                                                  inRequestURI);
                log(msg, t);
                context.removeServletMapping(pattern);
                context.removeChild(wrapper);
                if (included)
                    throw new ServletException
                        (msg, t);
                else {
                    /* IASRI 4878272
                    response.sendError(HttpServletResponse.SC_NOT_FOUND,
                                       inRequestURI);
                    */
                    // BEGIN IASRI 4878272
                    String invalidPathMsg = MessageFormat.format(rb.getString(INVALID_PATH_EXCEPTION),
                                                                 inRequestURI);
                    log(invalidPathMsg);
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    // END IASRI 4878272
                    return;
                }
            }
        }

        // Create a request wrapper to pass on to the invoked servlet
        InvokerHttpRequest wrequest =
            new InvokerHttpRequest(request);
        wrequest.setRequestURI(inRequestURI);
        StringBuilder sb = new StringBuilder(inServletPath);
        sb.append("/");
        sb.append(servletClass);
        wrequest.setServletPath(sb.toString());
        if ((pathInfo == null) || (pathInfo.length() < 1)) {
            wrequest.setPathInfo(null);
            wrequest.setPathTranslated(null);
        } else {
            wrequest.setPathInfo(pathInfo);
            wrequest.setPathTranslated
                (getServletContext().getRealPath(pathInfo));
        }

        // Allocate a servlet instance to perform this request
        Servlet instance = null;

        String cannotAllocateMsg = MessageFormat.format(rb.getString(CANNOT_ALLOCATE_SERVLET_INSTANCE_EXCEPTION),
                inRequestURI);
        String invalidPathMsg = MessageFormat.format(rb.getString(INVALID_PATH_EXCEPTION),
                inRequestURI);
        try {
            //            if (debug >= 2)
            //                log("  Allocating servlet instance");
            instance = wrapper.allocate();
        } catch (ServletException e) {

            log(cannotAllocateMsg, e);
            context.removeServletMapping(pattern);
            context.removeChild(wrapper);
            Throwable rootCause = e.getRootCause();
            if (rootCause == null)
                rootCause = e;
            if (rootCause instanceof ClassNotFoundException) {
                /* IASRI 4878272
                response.sendError(HttpServletResponse.SC_NOT_FOUND,
                                   inRequestURI);
                */
                // BEGIN IASRI 4878272
                log(invalidPathMsg);
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                // END IASRI 4878272
                return;
            } else if (rootCause instanceof IOException) {
                throw (IOException) rootCause;
            } else if (rootCause instanceof RuntimeException) {
                throw (RuntimeException) rootCause;
            } else if (rootCause instanceof ServletException) {
                throw (ServletException) rootCause;
            } else {
                throw new ServletException(cannotAllocateMsg, rootCause);
            }
        } catch (Throwable e) {
            log(cannotAllocateMsg, e);
            context.removeServletMapping(pattern);
            context.removeChild(wrapper);
            throw new ServletException
                (cannotAllocateMsg, e);
        }

        // After loading the wrapper, restore some of the fields when including
        if (included) {
            wrequest.setRequestURI(request.getRequestURI());
            wrequest.setPathInfo(request.getPathInfo());
            wrequest.setServletPath(request.getServletPath());
        }

        // Invoke the service() method of the allocated servlet
        try {
            String jspFile = wrapper.getJspFile();
            if (jspFile != null)
                request.setAttribute(Globals.JSP_FILE_ATTR, jspFile);
            else
                request.removeAttribute(Globals.JSP_FILE_ATTR);
            request.setAttribute(Globals.INVOKED_ATTR,
                                 request.getServletPath());
            //            if (debug >= 2)
            //                log("  Calling service() method, jspFile=" +
            //                    jspFile);
            instance.service(wrequest, response);
            request.removeAttribute(Globals.INVOKED_ATTR);
            request.removeAttribute(Globals.JSP_FILE_ATTR);
        } catch (IOException e) {
            //            if (debug >= 2)
            //                log("  service() method IOException", e);
            request.removeAttribute(Globals.INVOKED_ATTR);
            request.removeAttribute(Globals.JSP_FILE_ATTR);
            try {
                wrapper.deallocate(instance);
            } catch (Throwable f) {
                ;
            }
            throw e;
        } catch (UnavailableException e) {
            //            if (debug >= 2)
            //                log("  service() method UnavailableException", e);
            context.removeServletMapping(pattern);
            request.removeAttribute(Globals.INVOKED_ATTR);
            request.removeAttribute(Globals.JSP_FILE_ATTR);
            try {
                wrapper.deallocate(instance);
            } catch (Throwable f) {
                ;
            }
            throw e;
        } catch (ServletException e) {
            //            if (debug >= 2)
            //                log("  service() method ServletException", e);
            request.removeAttribute(Globals.INVOKED_ATTR);
            request.removeAttribute(Globals.JSP_FILE_ATTR);
            try {
                wrapper.deallocate(instance);
            } catch (Throwable f) {
                ;
            }
            throw e;
        } catch (RuntimeException e) {
            //            if (debug >= 2)
            //                log("  service() method RuntimeException", e);
            request.removeAttribute(Globals.INVOKED_ATTR);
            request.removeAttribute(Globals.JSP_FILE_ATTR);
            try {
                wrapper.deallocate(instance);
            } catch (Throwable f) {
                ;
            }
            throw e;
        } catch (Throwable e) {
            //            if (debug >= 2)
            //                log("  service() method Throwable", e);
            request.removeAttribute(Globals.INVOKED_ATTR);
            request.removeAttribute(Globals.JSP_FILE_ATTR);
            try {
                wrapper.deallocate(instance);
            } catch (Throwable f) {
                ;
            }
            throw new ServletException("Invoker service() exception", e);
        }

        // Deallocate the allocated servlet instance
        String cannotDeallocateMsg = MessageFormat.format(rb.getString(CANNOT_DEALLOCATE_SERVLET_INSTANCE_EXCEPTION),
                                          inRequestURI);
        try {
            //            if (debug >= 2)
            //                log("  deallocate servlet instance");
            wrapper.deallocate(instance);
        } catch (ServletException e) {
            log(cannotDeallocateMsg, e);
            throw e;
        } catch (Throwable e) {
            log(cannotDeallocateMsg, e);
            throw new ServletException
                (cannotDeallocateMsg, e);
        }

    }


}

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
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.RequestFacade;
import org.apache.catalina.connector.ResponseFacade;
import org.apache.catalina.util.InstanceSupport;
import org.glassfish.logging.annotation.LogMessageInfo;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.text.MessageFormat;


import static org.apache.catalina.InstanceEvent.EventType.AFTER_DISPATCH_EVENT;

/**
 * Standard implementation of <code>RequestDispatcher</code> that allows a
 * request to be forwarded to a different resource to create the ultimate
 * response, or to include the output of another resource in the response
 * from this resource.  This implementation allows application level servlets
 * to wrap the request and/or response objects that are passed on to the
 * called resource, as long as the wrapping classes extend
 * <code>javax.servlet.ServletRequestWrapper</code> and
 * <code>javax.servlet.ServletResponseWrapper</code>.
 *
 * @author Craig R. McClanahan
 * @version $Revision: 1.16 $ $Date: 2007/02/26 22:57:08 $
 */

public final class ApplicationDispatcher
    implements RequestDispatcher {

    private static final Logger log = StandardServer.log;
    private static final ResourceBundle rb = log.getResourceBundle();

    @LogMessageInfo(
        message = "Cannot forward after response has been committed",
        level = "INFO"
    )
    public static final String ILLEGAL_STATE_EXCEPTION = "AS-WEB-CORE-00086";

    @LogMessageInfo(
        message = "Servlet {0} is currently unavailable",
        level = "WARNING"
    )
    public static final String UNAVAILABLE_SERVLET = "AS-WEB-CORE-00087";

    @LogMessageInfo(
        message = "Allocate exception for servlet {0}",
        level = "SEVERE",
        cause = "Could not allocate servlet instance",
        action = "Verify the configuration of wrapper"
    )
    public static final String ALLOCATE_SERVLET_EXCEPTION = "AS-WEB-CORE-00088";

    @LogMessageInfo(
        message = "Exceeded maximum depth for nested request dispatches: {0}",
        level = "INFO"
    )
    public static final String MAX_DISPATCH_DEPTH_REACHED = "AS-WEB-CORE-00089";

    @LogMessageInfo(
        message = "Servlet.service() for servlet {0} threw exception",
        level = "WARNING"
    )
    public static final String SERVLET_SERVICE_EXCEPTION = "AS-WEB-CORE-00090";

    @LogMessageInfo(
        message = "Release filters exception for servlet {0}",
        level = "SEVERE",
        cause = "Could not release filter chain",
        action = "Verify the availability of current filter chain"
    )
    public static final String RELEASE_FILTERS_EXCEPTION = "AS-WEB-CORE-00091";

    @LogMessageInfo(
        message = "Deallocate exception for servlet {0}",
        level = "SEVERE",
        cause = "Could not deallocate the allocated servlet instance",
        action = "Verify the availability of servlet instance"
    )
    public static final String DEALLOCATE_SERVLET_EXCEPTION = "AS-WEB-CORE-00092";

    @LogMessageInfo(
        message = "ApplicationDispatcher[{0}]: {1}",
        level = "INFO"
    )
    public static final String APPLICATION_DISPATCHER_INFO = "AS-WEB-CORE-00093";

    @LogMessageInfo(
        message = "ApplicationDispatcher[{0}]: {1}",
        level = "WARNING",
        cause = "Could not get logger from parent context",
        action = "Verify if logger is null"
    )
    public static final String APPLICATION_DISPATCHER_WARNING = "AS-WEB-CORE-00094";

    // This attribute corresponds to a String[] which acts like a stack
    // containing the last two pushed elements
    public static final String LAST_DISPATCH_REQUEST_PATH_ATTR =
        "org.apache.catalina.core.ApplicationDispatcher.lastDispatchRequestPathAttr";

    protected class PrivilegedDispatch implements PrivilegedExceptionAction<Void> {

        private ServletRequest request;
        private ServletResponse response;
        private DispatcherType dispatcherType;

        PrivilegedDispatch(ServletRequest request, ServletResponse response,
                           DispatcherType dispatcherType) {
            this.request = request;
            this.response = response;
            this.dispatcherType = dispatcherType;
        }

        public Void run() throws java.lang.Exception {
            doDispatch(request, response, dispatcherType);
            return null;
        }
    }

    protected class PrivilegedInclude implements PrivilegedExceptionAction<Void> {

        private ServletRequest request;
        private ServletResponse response;

        PrivilegedInclude(ServletRequest request, ServletResponse response) {
            this.request = request;
            this.response = response;
        }

        public Void run() throws ServletException, IOException {
            doInclude(request,response);
            return null;
        }
    }

    /**
     * Used to pass state when the request dispatcher is used. Using instance
     * variables causes threading issues and state is too complex to pass and
     * return single ServletRequest or ServletResponse objects.
     */
    private static class State {

        /**
         * Outermost request that will be passed on to the invoked servlet
         */
        ServletRequest outerRequest = null;

        /**
         * Outermost response that will be passed on to the invoked servlet.
         */
        ServletResponse outerResponse = null;
        
        /**
         * Request wrapper we have created and installed (if any).
         */
        ServletRequest wrapRequest = null;

        /**
         * Response wrapper we have created and installed (if any).
         */
        ServletResponse wrapResponse = null;
        
        /**
         * The type of dispatch we are performing
         */
        DispatcherType dispatcherType;

        /**
         * Outermost HttpServletRequest in the chain
         */
        HttpServletRequest hrequest = null;

        /**
         * Outermost HttpServletResponse in the chain
         */
        HttpServletResponse hresponse = null;

        State(ServletRequest request, ServletResponse response,
              DispatcherType dispatcherType) {
            this.outerRequest = request;
            this.outerResponse = response;
            this.dispatcherType = dispatcherType;
        }
    }

    // ----------------------------------------------------------- Constructors


    /**
     * Construct a new instance of this class, configured according to the
     * specified parameters.  If both servletPath and pathInfo are
     * <code>null</code>, it will be assumed that this RequestDispatcher
     * was acquired by name, rather than by path.
     *
     * @param wrapper The Wrapper associated with the resource that will
     *  be forwarded to or included (required)
     * @param requestURI The request URI to this resource (if any)
     * @param servletPath The revised servlet path to this resource (if any)
     * @param pathInfo The revised extra path information to this resource
     *  (if any)
     * @param queryString Query string parameters included with this request
     *  (if any)
     * @param name Servlet name (if a named dispatcher was created)
     *  else <code>null</code>
     */
    public ApplicationDispatcher
        (Wrapper wrapper, String requestURI, String servletPath,
         String pathInfo, String queryString, String name) {

        super();

        // Save all of our configuration parameters
        this.wrapper = wrapper;
        this.context = (Context) wrapper.getParent();
        this.requestURI = requestURI;
        this.servletPath = servletPath;
        this.pathInfo = pathInfo;
        this.queryString = queryString;
        this.name = name;

        if (log.isLoggable(Level.FINE))
            log.log(Level.FINE, "servletPath= " + this.servletPath + ", pathInfo= "
                    + this.pathInfo + ", queryString= " + queryString + ", name= "
                    + this.name + "");
    }


    // ----------------------------------------------------- Instance Variables

    //START OF 6364900
    /**
     * is this dispatch cross context
     */
    private Boolean crossContextFlag = null;
    //END OF 6364900

    /**
     * The Context this RequestDispatcher is associated with.
     */
    private Context context = null;

    /**
     * Descriptive information about this implementation.
     */
    private static final String info =
        "org.apache.catalina.core.ApplicationDispatcher/1.0";

    /**
     * The servlet name for a named dispatcher.
     */
    private String name = null;

    /**
     * The extra path information for this RequestDispatcher.
     */
    private String pathInfo = null;

    /**
     * The query string parameters for this RequestDispatcher.
     */
    private String queryString = null;

    /**
     * The request URI for this RequestDispatcher.
     */
    private String requestURI = null;

    /**
     * The servlet path for this RequestDispatcher.
     */
    private String servletPath = null;

    /**
     * The Wrapper associated with the resource that will be forwarded to
     * or included.
     */
    private Wrapper wrapper = null;


    // ------------------------------------------------------------- Properties


    /**
     * Return the descriptive information about this implementation.
     */
    public String getInfo() {
        return (this.info);
    }


    // --------------------------------------------------------- Public Methods

    /**
     * Forwards the given request and response to the resource
     * for which this dispatcher was acquired.
     *
     * <p>Any runtime exceptions, IOException, or ServletException thrown
     * by the target will be propagated to the caller.
     *
     * @param request The request to be forwarded
     * @param response The response to be forwarded
     *
     * @throws IOException if an input/output error occurs
     * @throws ServletException if a servlet exception occurs
     */
    public void forward(ServletRequest request, ServletResponse response)
            throws ServletException, IOException {
        dispatch(request, response, DispatcherType.FORWARD);
    }

    /**
     * Dispatches the given request and response to the resource
     * for which this dispatcher was acquired.
     *
     * <p>Any runtime exceptions, IOException, or ServletException thrown
     * by the target will be propagated to the caller.
     *
     * @param request The request to be forwarded
     * @param response The response to be forwarded
     * @param dispatcherType The type of dispatch to be performed
     *
     * @throws IOException if an input/output error occurs
     * @throws ServletException if a servlet exception occurs
     * @throws IllegalArgumentException if the dispatcher type is different
     * from FORWARD, ERROR, and ASYNC
     */
    public void dispatch(ServletRequest request, ServletResponse response,
                  DispatcherType dispatcherType)
            throws ServletException, IOException {

        if (DispatcherType.FORWARD != dispatcherType &&
                DispatcherType.ERROR != dispatcherType &&
                DispatcherType.ASYNC != dispatcherType) {
            throw new IllegalArgumentException("Illegal dispatcher type");
        }

        boolean isCommit = (DispatcherType.FORWARD == dispatcherType ||
            DispatcherType.ERROR == dispatcherType);

        if (Globals.IS_SECURITY_ENABLED) {
            try {
                PrivilegedDispatch dp = new PrivilegedDispatch(
                    request, response, dispatcherType);
                AccessController.doPrivileged(dp);
                // START SJSAS 6374990
                if (isCommit && !request.isAsyncStarted()) {
                    ApplicationDispatcherForward.commit(request, response,
                        context, wrapper);
                }
                // END SJSAS 6374990
            } catch (PrivilegedActionException pe) {
                Exception e = pe.getException();
                if (e instanceof ServletException)
                    throw (ServletException) e;
                throw (IOException) e;
            }
        } else {
            doDispatch(request, response, dispatcherType);
            // START SJSAS 6374990
            if (isCommit && !request.isAsyncStarted()) {
                ApplicationDispatcherForward.commit(request, response,
                    context, wrapper);
            }
            // END SJSAS 6374990
        }
    }

    private void doDispatch(ServletRequest request, ServletResponse response,
                            DispatcherType dispatcherType)
        throws ServletException, IOException {

        if (DispatcherType.ASYNC != dispatcherType) {
            // Reset any output that has been buffered, but keep
            // headers/cookies
            if (response.isCommitted()) {
                if (log.isLoggable(Level.FINE))
                    log.log(Level.FINE, "Forward on committed response --> ISE");
                throw new IllegalStateException
                        (rb.getString(ILLEGAL_STATE_EXCEPTION));
            }

            try {
                response.resetBuffer();
            } catch (IllegalStateException e) {
                if (log.isLoggable(Level.FINE))
                    log.log(Level.FINE,
                           "Forward resetBuffer() returned ISE: " + e.toString(), e);
                throw e;
            }
        }

        if (DispatcherType.INCLUDE != dispatcherType) {
            DispatchTargetsInfo dtInfo =
                    (DispatchTargetsInfo)request.getAttribute(
                    LAST_DISPATCH_REQUEST_PATH_ATTR);
            if (dtInfo == null) {
                dtInfo = new DispatchTargetsInfo();
                request.setAttribute(LAST_DISPATCH_REQUEST_PATH_ATTR, dtInfo);
            }

            if (servletPath == null && pathInfo == null) {
                // Handle an HTTP named dispatcher forward
                dtInfo.addDispatchTarget(wrapper.getServletName(), true);
            } else {
                dtInfo.addDispatchTarget(getCombinedPath(), false);
            }
        }

        // Set up to handle the specified request and response
        State state = new State(request, response, dispatcherType);

        ServletRequest sr = wrapRequest(state);
        wrapResponse(state);

        // Identify the HTTP-specific request and response objects (if any)
        HttpServletRequest hrequest = state.hrequest;
        HttpServletResponse hresponse = state.hresponse;

        if ((hrequest == null) || (hresponse == null)) {
            // Handle a non-HTTP forward
            processRequest(request, response, state);
        } else if ((servletPath == null) && (pathInfo == null)) {
            // Handle an HTTP named dispatcher forward
            ApplicationHttpRequest wrequest = (ApplicationHttpRequest)sr;
            wrequest.setRequestURI(hrequest.getRequestURI());
            wrequest.setContextPath(hrequest.getContextPath());
            wrequest.setServletPath(hrequest.getServletPath());
            wrequest.setPathInfo(hrequest.getPathInfo());
            wrequest.setQueryString(hrequest.getQueryString());
            
            processRequest(request, response, state);

        } else {
            // Handle an HTTP path-based forward
            ApplicationHttpRequest wrequest = (ApplicationHttpRequest)sr;

            // If the request is being FORWARD- or ASYNC-dispatched for 
            // the first time, initialize it with the required request
            // attributes
            if ((DispatcherType.FORWARD == dispatcherType &&
                    hrequest.getAttribute(
                        RequestDispatcher.FORWARD_REQUEST_URI) == null) ||
                    (DispatcherType.ASYNC == dispatcherType &&
                        hrequest.getAttribute(
                            AsyncContext.ASYNC_REQUEST_URI) == null)) { 
                wrequest.initSpecialAttributes(hrequest.getRequestURI(),
                                               hrequest.getContextPath(),
                                               hrequest.getServletPath(),
                                               hrequest.getPathInfo(),
                                               hrequest.getQueryString());
            }

            String targetContextPath = context.getPath();
            // START IT 10395
            RequestFacadeHelper reqFacHelper = RequestFacadeHelper.getInstance(wrequest);
            String originContextPath = null;
            if (reqFacHelper != null) {
                originContextPath = reqFacHelper.getContextPath(false);
            } else {
                originContextPath = wrequest.getContextPath();
            }
            if (originContextPath != null &&
                    originContextPath.equals(targetContextPath)) {
                targetContextPath = hrequest.getContextPath();
            }
            // END IT 10395
            wrequest.setContextPath(targetContextPath);
            wrequest.setRequestURI(requestURI);
            wrequest.setServletPath(servletPath);
            wrequest.setPathInfo(pathInfo);
            if (queryString != null) {
                wrequest.setQueryString(queryString);
                wrequest.setQueryParams(queryString);
            }

            processRequest(request, response, state);

        }

        recycleRequestWrapper(state);
        unwrapRequest(state);
        unwrapResponse(state);
    }


    /**
     * Prepare the request based on the filter configuration.
     *
     * @param request The servlet request we are processing
     * @param response The servlet response we are creating
     *
     * @throws IOException if an input/output error occurs
     * @throws ServletException if a servlet error occurs
     */
    private void processRequest(ServletRequest request, 
                                ServletResponse response,
                                State state)
        throws IOException, ServletException {
                
        if (request != null) {
            if (state.dispatcherType != DispatcherType.ERROR) {
                state.outerRequest.setAttribute(
                    Globals.DISPATCHER_REQUEST_PATH_ATTR,
                    getCombinedPath());
                invoke(state.outerRequest, response, state);
            } else {
                invoke(state.outerRequest, response, state);
            }
        }
    }

    /**
     * Combines the servletPath and the pathInfo.
     *
     * If pathInfo is <code>null</code>, it is ignored. If servletPath
     * is <code>null</code>, then <code>null</code> is returned.
     *
     * @return The combined path with pathInfo appended to servletInfo
     */
    private String getCombinedPath() {
        if (servletPath == null) {
            return null;
        }
        if (pathInfo == null) {
            return servletPath;
        }
        return servletPath + pathInfo;
    }
    

    /**
     * Include the response from another resource in the current response.
     * Any runtime exception, IOException, or ServletException thrown by the
     * called servlet will be propagated to the caller.
     *
     * @param request The servlet request that is including this one
     * @param response The servlet response to be appended to
     *
     * @throws IOException if an input/output error occurs
     * @throws ServletException if a servlet exception occurs
     */
    public void include(ServletRequest request, ServletResponse response)
        throws ServletException, IOException
    {
        if (Globals.IS_SECURITY_ENABLED) {
            try {
                PrivilegedInclude dp = new PrivilegedInclude(request,response);
                AccessController.doPrivileged(dp);
            } catch (PrivilegedActionException pe) {
                Exception e = pe.getException();
                if (e instanceof ServletException)
                    throw (ServletException) e;
                throw (IOException) e;
            }
        } else {
            doInclude(request,response);
        }
    }


    private void doInclude(ServletRequest request, ServletResponse response)
        throws ServletException, IOException
    {

        // Set up to handle the specified request and response
        State state = new State(request, response, DispatcherType.INCLUDE);

        // Create a wrapped response to use for this request
        wrapResponse(state);

        // Handle a non-HTTP include
        /* GlassFish 6386229
        if (!(request instanceof HttpServletRequest) ||
            !(response instanceof HttpServletResponse)) {

            if ( log.isDebugEnabled() )
                log.debug(" Non-HTTP Include");
            request.setAttribute(ApplicationFilterFactory.DISPATCHER_TYPE_ATTR,
                                             Integer.valueOf(ApplicationFilterFactory.INCLUDE));
            request.setAttribute(ApplicationFilterFactory.DISPATCHER_REQUEST_PATH_ATTR, 
                                             //origServletPath);
                                             servletPath);
            try{
                invoke(request, state.outerResponse, state);
            } finally {
                unwrapResponse(state);
            }
        }

        // Handle an HTTP named dispatcher include
        else if (name != null) {
        */
        // START GlassFish 6386229
        // Handle an HTTP named dispatcher include
        if (name != null) {
        // END GlassFish 6386229
            ApplicationHttpRequest wrequest = (ApplicationHttpRequest)wrapRequest(state);
            wrequest.setAttribute(Globals.NAMED_DISPATCHER_ATTR, name);
            if (servletPath != null)
                wrequest.setServletPath(servletPath);
            wrequest.setAttribute(Globals.DISPATCHER_REQUEST_PATH_ATTR,
                                  getCombinedPath());
            try{
                invoke(state.outerRequest, state.outerResponse, state);
            } finally {
                recycleRequestWrapper(state);
                unwrapRequest(state);
                unwrapResponse(state);
            }

        }

        // Handle an HTTP path based include
        else {
            ApplicationHttpRequest wrequest = (ApplicationHttpRequest)wrapRequest(state);
            wrequest.initSpecialAttributes(requestURI,
                                           context.getPath(),
                                           servletPath,
                                           pathInfo,
                                           queryString);
            wrequest.setQueryParams(queryString);
            wrequest.setAttribute(Globals.DISPATCHER_REQUEST_PATH_ATTR,
                                  getCombinedPath());
            try{
                invoke(state.outerRequest, state.outerResponse, state);
            } finally {
                recycleRequestWrapper(state);
                unwrapRequest(state);
                unwrapResponse(state);
           }
        }
    }


    // -------------------------------------------------------- Private Methods
    
    
    /**
     * Ask the resource represented by this RequestDispatcher to process
     * the associated request, and create (or append to) the associated
     * response.
     * <p>
     * <strong>IMPLEMENTATION NOTE</strong>: This implementation assumes
     * that no filters are applied to a forwarded or included resource,
     * because they were already done for the original request.
     *
     * @param request The servlet request we are processing
     * @param response The servlet response we are creating
     *
     * @throws IOException if an input/output error occurs
     * @throws ServletException if a servlet error occurs
     */
    private void invoke(ServletRequest request, ServletResponse response,
                State state)
            throws IOException, ServletException {
        //START OF 6364900 original invoke has been renamed to doInvoke
        boolean crossContext = false;
        if (crossContextFlag != null && crossContextFlag.booleanValue()) {
            crossContext = true;
        }
        if (crossContext) {
            context.getManager().lockSession(request); 
        }       
        try {
            if (crossContext) {
                context.getManager().preRequestDispatcherProcess(request,
                                                                 response);
            }            
            doInvoke(request, response, crossContext, state);
            if (crossContext) {
                context.getManager().postRequestDispatcherProcess(request,
                                                                  response);
            }
        } finally {
            if (crossContext) {
                context.getManager().unlockSession(request);
            }
            crossContextFlag = null;
        }
        //END OF 6364900
    }
    
    
    /**
     * Ask the resource represented by this RequestDispatcher to process
     * the associated request, and create (or append to) the associated
     * response.
     * <p>
     * <strong>IMPLEMENTATION NOTE</strong>: This implementation assumes
     * that no filters are applied to a forwarded or included resource,
     * because they were already done for the original request.
     *
     * @param request The servlet request we are processing
     * @param response The servlet response we are creating
     * @param crossContext true if the request dispatch is crossing context
     * boundaries, false otherwise
     * @param state the state of this ApplicationDispatcher
     *
     * @throws IOException if an input/output error occurs
     * @throws ServletException if a servlet error occurs
     */
    private void doInvoke(ServletRequest request, ServletResponse response,
                          boolean crossContext, State state)
            throws IOException, ServletException {

        // Checking to see if the context classloader is the current context
        // classloader. If it's not, we're saving it, and setting the context
        // classloader to the Context classloader
        ClassLoader oldCCL = null;
        if (crossContext) {
            oldCCL = Thread.currentThread().getContextClassLoader();
            ClassLoader contextClassLoader = context.getLoader().getClassLoader();
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }

        HttpServletResponse hresponse = state.hresponse;
        Servlet servlet = null;
        IOException ioException = null;
        ServletException servletException = null;
        RuntimeException runtimeException = null;
        boolean unavailable = false;
              

        // Check for the servlet being marked unavailable
        if (wrapper.isUnavailable()) {
            String msg = MessageFormat.format(rb.getString(UNAVAILABLE_SERVLET),
                                              wrapper.getName());
            log.log(Level.WARNING, msg);
            if (hresponse == null) {
                ;       // NOTE - Not much we can do generically
            } else {
                long available = wrapper.getAvailable();
                if ((available > 0L) && (available < Long.MAX_VALUE))
                    hresponse.setDateHeader("Retry-After", available);
                hresponse.sendError
                        (HttpServletResponse.SC_SERVICE_UNAVAILABLE, msg);
            }
            unavailable = true;
        }

        // Allocate a servlet instance to process this request
        String allocateServletMsg =
                MessageFormat.format(rb.getString(ALLOCATE_SERVLET_EXCEPTION),
                                     wrapper.getName());
        try {
            if (!unavailable) {
                servlet = wrapper.allocate();
            }
        } catch (ServletException e) {
            log.log(Level.SEVERE, allocateServletMsg,
                    StandardWrapper.getRootCause(e));
            servletException = e;
            servlet = null;
        } catch (Throwable e) {
            log.log(Level.SEVERE, allocateServletMsg, e);
            servletException = new ServletException(allocateServletMsg, e);
            servlet = null;
        }
                
        // Get the FilterChain Here
        ApplicationFilterFactory factory = ApplicationFilterFactory.getInstance();
        ApplicationFilterChain filterChain = factory.createFilterChain(
            request, wrapper, servlet);

        InstanceSupport support = ((StandardWrapper) wrapper).getInstanceSupport();

        // Call the service() method for the allocated servlet instance
        String servletServiceExceptionMsg =
                MessageFormat.format(rb.getString(SERVLET_SERVICE_EXCEPTION),
                                     wrapper.getName());
        RequestFacadeHelper reqFacHelper = RequestFacadeHelper.getInstance(request);
        try {
            String jspFile = wrapper.getJspFile();
            if (jspFile != null) {
                request.setAttribute(Globals.JSP_FILE_ATTR, jspFile);
            } 
            support.fireInstanceEvent(
                InstanceEvent.EventType.BEFORE_DISPATCH_EVENT,
                servlet, request, response);
            // for includes/forwards
            /* IASRI 4665318
            if ((servlet != null) && (filterChain != null)) {
            */
            // START IASRI 4665318
            if (servlet != null) {
            // END IASRI 4665318
                // START OF S1AS 4703023
                if (reqFacHelper != null) {
                    reqFacHelper.incrementDispatchDepth();
                    if (reqFacHelper.isMaxDispatchDepthReached()) {
                        String msg = MessageFormat.format(MAX_DISPATCH_DEPTH_REACHED,
                                                          new Object[]{Integer.valueOf(Request.getMaxDispatchDepth())});
                        throw new ServletException(msg);
                    }
                }
                // END OF S1AS 4703023 
                /* IASRI 4665318
                filterChain.doFilter(request, response);
                */
                // START IASRI 4665318
                if (filterChain != null) {
                    filterChain.setWrapper((StandardWrapper)wrapper);
                    filterChain.doFilter(request, response);
                } else {
                    ((StandardWrapper)wrapper).service(
                        request, response, servlet);
                }
                // END IASRI 4665318
            }
            // Servlet Service Method is called by the FilterChain
            support.fireInstanceEvent(AFTER_DISPATCH_EVENT,
                                      servlet, request, response);
        } catch (ClientAbortException e) {
            support.fireInstanceEvent(AFTER_DISPATCH_EVENT,
                                      servlet, request, response);
            ioException = e;
        } catch (IOException e) {
            support.fireInstanceEvent(AFTER_DISPATCH_EVENT,
                                      servlet, request, response);
            log.log(Level.WARNING, servletServiceExceptionMsg, e);
            ioException = e;
        } catch (UnavailableException e) {
            support.fireInstanceEvent(AFTER_DISPATCH_EVENT,
                                      servlet, request, response);
            log.log(Level.WARNING, servletServiceExceptionMsg, e);
            servletException = e;
            wrapper.unavailable(e);
        } catch (ServletException e) {
            support.fireInstanceEvent(AFTER_DISPATCH_EVENT,
                                      servlet, request, response);
            Throwable rootCause = StandardWrapper.getRootCause(e);
            if (!(rootCause instanceof ClientAbortException)) {
                log.log(Level.WARNING, servletServiceExceptionMsg, rootCause);
            }
            servletException = e;
        } catch (RuntimeException e) {
            support.fireInstanceEvent(AFTER_DISPATCH_EVENT,
                                      servlet, request, response);
            log.log(Level.WARNING, servletServiceExceptionMsg, e);
            runtimeException = e;
        // START OF S1AS 4703023
        } finally {
            if (reqFacHelper != null) {
                reqFacHelper.decrementDispatchDepth();
            }
        // END OF S1AS 4703023
        }

        // Release the filter chain (if any) for this request
        try {
            if (filterChain != null)
                filterChain.release();
        } catch (Throwable e) {
            String msg = MessageFormat.format(rb.getString(RELEASE_FILTERS_EXCEPTION),
                                                           wrapper.getName());
            log.log(Level.SEVERE, msg, e);
            // FIXME Exception handling needs to be simpiler to what is
            // in the StandardWrapperValue
        }

        // Deallocate the allocated servlet instance
        String deallocateServletExceptionMsg =
                MessageFormat.format(rb.getString(DEALLOCATE_SERVLET_EXCEPTION),
                                                  wrapper.getName());
        try {
            if (servlet != null) {
                wrapper.deallocate(servlet);
            }
        } catch (ServletException e) {
            log.log(Level.SEVERE, deallocateServletExceptionMsg, e);
            servletException = e;
        } catch (Throwable e) {
            log.log(Level.SEVERE, deallocateServletExceptionMsg, e);
            servletException = new ServletException(deallocateServletExceptionMsg, e);
        }

        // Reset the old context class loader
        if (oldCCL != null)
            Thread.currentThread().setContextClassLoader(oldCCL);

        // Rethrow an exception if one was thrown by the invoked servlet
        if (ioException != null)
            throw ioException;
        if (servletException != null)
            throw servletException;
        if (runtimeException != null)
            throw runtimeException;
    }


    /**
     * Log a message on the Logger associated with our Context (if any)
     *
     * @param message Message to be logged
     *
    private void log(String message) {
        org.apache.catalina.Logger logger = context.getLogger();
        if (logger != null) {
            logger.log("ApplicationDispatcher[" + context.getPath() +
                       "]: " + message);
        } else {
            if (log.isLoggable(Level.INFO)) {
                String msg = MessageFormat.format(rb.getString(APPLICATION_DISPATCHER_INFO),
                                                  context.getPath(), message);
                log.log(Level.INFO, msg);
            }
        }
    }


    /**
     * Log a message on the Logger associated with our Container (if any)
     *
     * @param message Message to be logged
     * @param t Associated exception
     *
    private void log(String message, Throwable t) {
        org.apache.catalina.Logger logger = context.getLogger();
        if (logger != null) {
            logger.log("ApplicationDispatcher[" + context.getPath() +
                "] " + message, t, org.apache.catalina.Logger.WARNING);
        } else {
            String msg = MessageFormat.format(rb.getString(APPLICATION_DISPATCHER_WARNING),
                                              context.getPath(), message);
            log.log(Level.WARNING, msg, t);
        }
    }  */


    /**
     * Unwrap the request if we have wrapped it.
     */
    private void unwrapRequest(State state) {

        if (state.wrapRequest == null)
            return;

        ServletRequest previous = null;
        ServletRequest current = state.outerRequest;

        while (current != null) {

            // If we run into the container request we are done
            if ((current instanceof org.apache.catalina.Request)
                || (current instanceof RequestFacade))
                break;

            // Remove the current request if it is our wrapper
            if (current == state.wrapRequest) {
                ServletRequest next =
                  ((ServletRequestWrapper) current).getRequest();
                if (previous == null)
                    state.outerRequest = next;
                else
                    ((ServletRequestWrapper) previous).setRequest(next);
                break;
            }

            // Advance to the next request in the chain
            previous = current;
            current = ((ServletRequestWrapper) current).getRequest();
        }
    }


    /**
     * Unwrap the response if we have wrapped it.
     */
    private void unwrapResponse(State state) {

        if (state.wrapResponse == null)
            return;

        ServletResponse previous = null;
        ServletResponse current = state.outerResponse;

        while (current != null) {

            // If we run into the container response we are done
            if ((current instanceof org.apache.catalina.Response) ||
                    (current instanceof ResponseFacade))
                break;

            // Remove the current response if it is our wrapper
            if (current == state.wrapResponse) {
                ServletResponse next =
                  ((ServletResponseWrapper) current).getResponse();
                if (previous == null)
                    state.outerResponse = next;
                else
                    ((ServletResponseWrapper) previous).setResponse(next);
                break;
            }

            // Advance to the next response in the chain
            previous = current;
            current = ((ServletResponseWrapper) current).getResponse();
        }
    }


    /**
     * Create and return a request wrapper that has been inserted in the
     * appropriate spot in the request chain.
     */
    private ServletRequest wrapRequest(State state) {

        // Locate the request we should insert in front of
        ServletRequest previous = null;
        ServletRequest current = state.outerRequest;

        while (current != null) {
            if (state.hrequest == null && (current instanceof HttpServletRequest)) {
                state.hrequest = (HttpServletRequest)current;
            }
            if ("org.apache.catalina.servlets.InvokerHttpRequest".
                    equals(current.getClass().getName())) {
                break; // KLUDGE - Make nested RD.forward() using invoker work
            }
            if (!(current instanceof ServletRequestWrapper)) {
                break;
            }
            // If we find container-generated wrapper, break out
            if (current instanceof ApplicationHttpRequest) {
                break;
            }
            if (current instanceof ApplicationRequest) {
                break;
            }
            previous = current;
            current = ((ServletRequestWrapper) current).getRequest();
        }

        // Instantiate a new wrapper at this point and insert it in the chain
        ServletRequest wrapper = null;
        if ((current instanceof ApplicationHttpRequest) ||
            (current instanceof Request) ||
            (current instanceof HttpServletRequest)) {
            // Compute a crossContext flag
            HttpServletRequest hcurrent = (HttpServletRequest) current;
            boolean crossContext = false;
            if ((state.outerRequest instanceof ApplicationHttpRequest) ||
                (state.outerRequest instanceof Request) ||
                (state.outerRequest instanceof HttpServletRequest)) {
                HttpServletRequest houterRequest =
                    (HttpServletRequest) state.outerRequest;
                Object contextPath = houterRequest.getAttribute(
                        RequestDispatcher.INCLUDE_CONTEXT_PATH);
                if (contextPath == null) {
                    // Forward
                    contextPath = houterRequest.getContextPath();
                }
                crossContext = !(context.getPath().equals(contextPath));
            }
            //START OF 6364900
            crossContextFlag = Boolean.valueOf(crossContext);
            //END OF 6364900
            wrapper = new ApplicationHttpRequest
                (hcurrent, context, crossContext, state.dispatcherType);
        } else {
            wrapper = new ApplicationRequest(current);
        }

        if (previous == null) {
            state.outerRequest = wrapper;
        } else {
            ((ServletRequestWrapper) previous).setRequest(wrapper);
        }

        state.wrapRequest = wrapper;

        return wrapper;
    }


    /**
     * Create and return a response wrapper that has been inserted in the
     * appropriate spot in the response chain.
     */
    private ServletResponse wrapResponse(State state) {

        // Locate the response we should insert in front of
        ServletResponse previous = null;
        ServletResponse current = state.outerResponse;

        while (current != null) {
            if(state.hresponse == null && (current instanceof HttpServletResponse)) {
                state.hresponse = (HttpServletResponse)current;
                if (DispatcherType.INCLUDE != state.dispatcherType) // Forward only needs hresponse
                    return null;
            }

            if (!(current instanceof ServletResponseWrapper))
                break;
            if (current instanceof ApplicationHttpResponse)
                break;
            if (current instanceof ApplicationResponse)
                break;
            previous = current;
            current = ((ServletResponseWrapper) current).getResponse();
        }

        // Instantiate a new wrapper at this point and insert it in the chain
        ServletResponse wrapper = null;
        if ((current instanceof ApplicationHttpResponse) ||
            (current instanceof HttpResponse) ||
            (current instanceof HttpServletResponse))
            wrapper =
                new ApplicationHttpResponse((HttpServletResponse) current,
                    DispatcherType.INCLUDE == state.dispatcherType);
        else
            wrapper = new ApplicationResponse(current,
                DispatcherType.INCLUDE == state.dispatcherType);
        if (previous == null)
            state.outerResponse = wrapper;
        else
            ((ServletResponseWrapper) previous).setResponse(wrapper);
        state.wrapResponse = wrapper;

        return (wrapper);
    }

    private void recycleRequestWrapper(State state) {
        if (state.wrapRequest instanceof ApplicationHttpRequest) {
            ((ApplicationHttpRequest) state.wrapRequest).recycle();
        }
    }
}

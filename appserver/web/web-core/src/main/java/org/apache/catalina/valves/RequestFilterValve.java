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

package org.apache.catalina.valves;


import org.apache.catalina.Request;
import org.apache.catalina.Response;
import org.apache.catalina.core.ApplicationDispatcher;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.deploy.ErrorPage;
import org.glassfish.logging.annotation.LogMessageInfo;

import javax.servlet.DispatcherType;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


/**
 * Implementation of a Valve that performs filtering based on comparing the
 * appropriate request property (selected based on which subclass you choose
 * to configure into your Container's pipeline) against a set of regular
 * expressions configured for this Valve.
 * <p>
 * This valve is configured by setting the <code>allow</code> and/or
 * <code>deny</code> properties to a comma-delimited list of regular
 * expressions (in the syntax supported by the jakarta-regexp library) to
 * which the appropriate request property will be compared.  Evaluation
 * proceeds as follows:
 * <ul>
 * <li>The subclass extracts the request property to be filtered, and
 *     calls the common <code>process()</code> method.
 * <li>If there are any deny expressions configured, the property will
 *     be compared to each such expression.  If a match is found, this
 *     request will be rejected with a "Forbidden" HTTP response.</li>
 * <li>If there are any allow expressions configured, the property will
 *     be compared to each such expression.  If a match is found, this
 *     request will be allowed to pass through to the next Valve in the
 *     current pipeline.</li>
 * <li>If one or more deny expressions was specified but no allow expressions,
 *     allow this request to pass through (because none of the deny
 *     expressions matched it).
 * <li>The request will be rejected with a "Forbidden" HTTP response.</li>
 * </ul>
 * <p>
 * This Valve may be attached to any Container, depending on the granularity
 * of the filtering you wish to perform.
 *
 * @author Craig R. McClanahan
 * @version $Revision: 1.5 $ $Date: 2006/03/07 22:30:07 $
 */

public abstract class RequestFilterValve
    extends ValveBase {

    @LogMessageInfo(
            message = "Syntax error in request filter pattern {0}",
            level = "WARNING"
    )
    public static final String SYNTAX_ERROR = "AS-WEB-CORE-00515";

    @LogMessageInfo(
            message = "Cannot process the error page: {0}",
            level = "INFO"
    )
    public static final String CANNOT_PROCESS_ERROR_PAGE_INFO = "AS-WEB-CORE-00516";
    // ----------------------------------------------------- Class Variables


    /**
     * The descriptive information related to this implementation.
     */
    private static final String info =
        "org.apache.catalina.valves.RequestFilterValve/1.0";



    // ----------------------------------------------------- Instance Variables


    /**
     * The comma-delimited set of <code>allow</code> expressions.
     */
    protected String allow = null;


    /**
     * The set of <code>allow</code> regular expressions we will evaluate.
     */
    protected Pattern allows[] = new Pattern[0];


    /**
     * The set of <code>deny</code> regular expressions we will evaluate.
     */
    protected Pattern denies[] = new Pattern[0];


    /**
     * The comma-delimited set of <code>deny</code> expressions.
     */
    protected String deny = null;


    // ------------------------------------------------------------- Properties


    /**
     * Return a comma-delimited set of the <code>allow</code> expressions
     * configured for this Valve, if any; otherwise, return <code>null</code>.
     */
    public String getAllow() {

        return (this.allow);

    }


    /**
     * Set the comma-delimited set of the <code>allow</code> expressions
     * configured for this Valve, if any.
     *
     * @param allow The new set of allow expressions
     */
    public void setAllow(String allow) {

        this.allow = allow;
        allows = precalculate(allow);

    }


    /**
     * Return a comma-delimited set of the <code>deny</code> expressions
     * configured for this Valve, if any; otherwise, return <code>null</code>.
     */
    public String getDeny() {

        return (this.deny);

    }


    /**
     * Set the comma-delimited set of the <code>deny</code> expressions
     * configured for this Valve, if any.
     *
     * @param deny The new set of deny expressions
     */
    public void setDeny(String deny) {

        this.deny = deny;
        denies = precalculate(deny);

    }


    /**
     * Return descriptive information about this Valve implementation.
     */
    public String getInfo() {

        return (info);

    }


    // --------------------------------------------------------- Public Methods


    /**
     * Extract the desired request property, and pass it (along with the
     * specified request and response objects) to the protected
     * <code>process()</code> method to perform the actual filtering.
     * This method must be implemented by a concrete subclass.
     *
     * @param request The servlet request to be processed
     * @param response The servlet response to be created
     *
     * @exception IOException if an input/output error occurs
     * @exception ServletException if a servlet error occurs
     */
    public abstract int invoke(Request request, Response response)
        throws IOException, ServletException;


    // ------------------------------------------------------ Protected Methods


    /**
     * Return an array of regular expression objects initialized from the
     * specified argument, which must be <code>null</code> or a comma-delimited
     * list of regular expression patterns.
     *
     * @param list The comma-separated list of patterns
     *
     * @exception IllegalArgumentException if one of the patterns has
     *  invalid syntax
     */
    protected Pattern[] precalculate(String list) {

        if (list == null)
            return (new Pattern[0]);
        list = list.trim();
        if (list.length() < 1)
            return (new Pattern[0]);
        list += ",";

        ArrayList<Pattern> reList = new ArrayList<Pattern>();
        while (list.length() > 0) {
            int comma = list.indexOf(',');
            if (comma < 0)
                break;
            String pattern = list.substring(0, comma).trim();
            try {
                reList.add(Pattern.compile(pattern));
            } catch (PatternSyntaxException e) {
                String msg = MessageFormat.format(rb.getString(SYNTAX_ERROR),
                                                  pattern);
                IllegalArgumentException iae = new IllegalArgumentException
                    (msg);
                iae.initCause(e);
                throw iae;
            }
            list = list.substring(comma + 1);
        }

        Pattern reArray[] = new Pattern[reList.size()];
        return reList.toArray(reArray);

    }


    /**
     * Perform the filtering that has been configured for this Valve, matching
     * against the specified request property.
     *
     * @param property The request property on which to filter
     * @param request The servlet request to be processed
     * @param response The servlet response to be processed
     *
     * @exception IOException if an input/output error occurs
     * @exception ServletException if a servlet error occurs
     */
    protected int process(String property, Request request, Response response)
        throws IOException, ServletException {

        // Check the deny patterns, if any
        for (int i = 0; i < denies.length; i++) {
            if (denies[i].matcher(property).matches()) {
                //ServletResponse sres = response.getResponse();
                /* GlassFish 6386229 
                if (sres instanceof HttpServletResponse) {
                    HttpServletResponse hres = (HttpServletResponse) sres;
                    hres.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return END_PIPELINE;
                }
                */
                // START GlassFish 6386229 
                //HttpServletResponse hres = (HttpServletResponse) sres;
                //hres.sendError(HttpServletResponse.SC_FORBIDDEN);
                handleError(request, response, HttpServletResponse.SC_FORBIDDEN);
                // END GlassFish 6386229                 
                return END_PIPELINE;
                // GlassFish 638622                   
            }
        }

        // Check the allow patterns, if any
        for (int i = 0; i < allows.length; i++) {
            if (allows[i].matcher(property).matches()) {
                return INVOKE_NEXT;
            }
        }

        // Allow if denies specified but not allows
        if ((denies.length > 0) && (allows.length == 0)) {
            return INVOKE_NEXT;
        }

        // Deny this request
        //ServletResponse sres = response.getResponse();
        /* GlassFish 6386229 
        if (sres instanceof HttpServletResponse) {
            HttpServletResponse hres = (HttpServletResponse) sres;
            hres.sendError(HttpServletResponse.SC_FORBIDDEN);
            return END_PIPELINE;
        }
        */
        // START GlassFish 6386229 
        //HttpServletResponse hres = (HttpServletResponse) sres;
        //hres.sendError(HttpServletResponse.SC_FORBIDDEN);
        handleError(request, response, HttpServletResponse.SC_FORBIDDEN);
        // END GlassFish 6386229        
        return END_PIPELINE;
    }

    private void handleError(Request request, Response response, int statusCode)
            throws IOException {

        ServletRequest sreq = request.getRequest();
        ServletResponse sres = response.getResponse();
        HttpServletResponse hres = (HttpServletResponse)sres;


        ErrorPage errorPage = null;
        if (getContainer() instanceof StandardHost) {
            errorPage = ((StandardHost)getContainer()).findErrorPage(statusCode);
        } else if (getContainer() instanceof StandardContext){
            errorPage = ((StandardContext)getContainer()).findErrorPage(statusCode);
        }
        if (errorPage != null) {
            try {
                hres.setStatus(statusCode);   
                ServletContext servletContext =
                    request.getContext().getServletContext();
                ApplicationDispatcher dispatcher = (ApplicationDispatcher)
                    servletContext.getRequestDispatcher(errorPage.getLocation());

                if (hres.isCommitted()) {
                    // Response is committed - including the error page is the
                    // best we can do 
                    dispatcher.include(sreq, sres);
                } else {
                    // Reset the response (keeping the real error code and message)
                    response.resetBuffer(true);

                    dispatcher.dispatch(sreq, sres, DispatcherType.ERROR);

                    // If we forward, the response is suspended again
                    response.setSuspended(false);
                }
                sres.flushBuffer();
            } catch(Throwable t) {
                if (log.isLoggable(Level.INFO)) {
                    String msg = MessageFormat.format(rb.getString(CANNOT_PROCESS_ERROR_PAGE_INFO),
                                                      errorPage.getLocation());
                    log.log(Level.INFO, msg, t);
                }
            }
        } else {
            hres.sendError(statusCode);
        }
    }
}

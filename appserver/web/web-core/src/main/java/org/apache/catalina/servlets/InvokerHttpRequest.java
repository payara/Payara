/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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


import org.apache.catalina.util.StringManager;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;


/**
 * Wrapper around a <code>javax.servlet.http.HttpServletRequest</code>
 * utilized when <code>InvokerServlet</code> processes the initial request
 * for an invoked servlet.  Subsequent requests will be mapped directly
 * to the servlet, because a new servlet mapping will have been created.
 *
 * @author Craig R. McClanahan
 * @version $Revision: 1.2 $ $Date: 2005/12/08 01:27:56 $
 */

class InvokerHttpRequest extends HttpServletRequestWrapper {


    // ----------------------------------------------------------- Constructors


    /**
     * Construct a new wrapped request around the specified servlet request.
     *
     * @param request The servlet request being wrapped
     */
    public InvokerHttpRequest(HttpServletRequest request) {

        super(request);
        this.pathInfo = request.getPathInfo();
        this.pathTranslated = request.getPathTranslated();
        this.requestURI = request.getRequestURI();
        this.servletPath = request.getServletPath();

    }


    // ----------------------------------------------------- Instance Variables


    /**
     * Descriptive information about this implementation.
     */
    protected static final String info =
        "org.apache.catalina.servlets.InvokerHttpRequest/1.0";


    /**
     * The path information for this request.
     */
    protected String pathInfo = null;


    /**
     * The translated path information for this request.
     */
    protected String pathTranslated = null;


    /**
     * The request URI for this request.
     */
    protected String requestURI = null;


    /**
     * The servlet path for this request.
     */
    protected String servletPath = null;


    // --------------------------------------------- HttpServletRequest Methods


    /**
     * Override the <code>getPathInfo()</code> method of the wrapped request.
     */
    public String getPathInfo() {

        return (this.pathInfo);

    }


    /**
     * Override the <code>getPathTranslated()</code> method of the
     * wrapped request.
     */
    public String getPathTranslated() {

        return (this.pathTranslated);

    }


    /**
     * Override the <code>getRequestURI()</code> method of the wrapped request.
     */
    public String getRequestURI() {

        return (this.requestURI);

    }


    /**
     * Override the <code>getServletPath()</code> method of the wrapped
     * request.
     */
    public String getServletPath() {

        return (this.servletPath);

    }


    // -------------------------------------------------------- Package Methods



    /**
     * Return descriptive information about this implementation.
     */
    public String getInfo() {

        return (this.info);

    }


    /**
     * Set the path information for this request.
     *
     * @param pathInfo The new path info
     */
    void setPathInfo(String pathInfo) {

        this.pathInfo = pathInfo;

    }


    /**
     * Set the translated path info for this request.
     *
     * @param pathTranslated The new translated path info
     */
    void setPathTranslated(String pathTranslated) {

        this.pathTranslated = pathTranslated;

    }


    /**
     * Set the request URI for this request.
     *
     * @param requestURI The new request URI
     */
    void setRequestURI(String requestURI) {

        this.requestURI = requestURI;

    }


    /**
     * Set the servlet path for this request.
     *
     * @param servletPath The new servlet path
     */
    void setServletPath(String servletPath) {

        this.servletPath = servletPath;

    }


}

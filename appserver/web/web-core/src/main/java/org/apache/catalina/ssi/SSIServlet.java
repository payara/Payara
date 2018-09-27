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

package org.apache.catalina.ssi;


import org.apache.catalina.Globals;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.Locale;
/**
 * Servlet to process SSI requests within a webpage. Mapped to a path from
 * within web.xml.
 * 
 * @author Bip Thelin
 * @author Amy Roh
 * @author Dan Sandberg
 * @author David Becker
 * @version $Revision: 1.4 $, $Date: 2007/05/05 05:32:20 $
 */
public class SSIServlet extends HttpServlet {
    /** Debug level for this servlet. */
    protected int debug = 0;
    /** Should the output be buffered. */
    protected boolean buffered = false;
    /** Expiration time in seconds for the doc. */
    protected Long expires = null;
    /** virtual path can be webapp-relative */
    protected boolean isVirtualWebappRelative = false;
    /** Input encoding. If not specified, uses platform default */
    protected String inputEncoding = null;
    /** Output encoding. If not specified, uses platform default */
    protected String outputEncoding = "UTF-8";


    //----------------- Public methods.
    /**
     * Initialize this servlet.
     * 
     * @exception ServletException
     *                if an error occurs
     */
    public void init() throws ServletException {
        
        if (getServletConfig().getInitParameter("debug") != null)
            debug = Integer.parseInt(getServletConfig().getInitParameter("debug"));
        
        isVirtualWebappRelative = 
            Boolean.parseBoolean(getServletConfig().getInitParameter("isVirtualWebappRelative"));
        
        if (getServletConfig().getInitParameter("expires") != null)
            expires = Long.valueOf(getServletConfig().getInitParameter("expires"));
        
        buffered = Boolean.parseBoolean(getServletConfig().getInitParameter("buffered"));
        
        inputEncoding = getServletConfig().getInitParameter("inputEncoding");
        
        if (getServletConfig().getInitParameter("outputEncoding") != null)
            outputEncoding = getServletConfig().getInitParameter("outputEncoding");
        
        if (debug > 0)
            log("SSIServlet.init() SSI invoker started with 'debug'=" + debug);

    }


    /**
     * Process and forward the GET request to our <code>requestHandler()</code>*
     * 
     * @param req
     *            a value of type 'HttpServletRequest'
     * @param res
     *            a value of type 'HttpServletResponse'
     * @exception IOException
     *                if an error occurs
     * @exception ServletException
     *                if an error occurs
     */
    public void doGet(HttpServletRequest req, HttpServletResponse res)
            throws IOException, ServletException {
        if (debug > 0) log("SSIServlet.doGet()");
        requestHandler(req, res);
    }


    /**
     * Process and forward the POST request to our
     * <code>requestHandler()</code>.
     * 
     * @param req
     *            a value of type 'HttpServletRequest'
     * @param res
     *            a value of type 'HttpServletResponse'
     * @exception IOException
     *                if an error occurs
     * @exception ServletException
     *                if an error occurs
     */
    public void doPost(HttpServletRequest req, HttpServletResponse res)
            throws IOException, ServletException {
        if (debug > 0) log("SSIServlet.doPost()");
        requestHandler(req, res);
    }


    /**
     * Process our request and locate right SSI command.
     * 
     * @param req
     *            a value of type 'HttpServletRequest'
     * @param res
     *            a value of type 'HttpServletResponse'
     */
    protected void requestHandler(HttpServletRequest req,
            HttpServletResponse res) throws IOException, ServletException {
        ServletContext servletContext = getServletContext();
        String path = SSIServletRequestUtil.getRelativePath(req);
        if (debug > 0)
            log("SSIServlet.requestHandler()\n" + "Serving "
                    + (buffered?"buffered ":"unbuffered ") + "resource '"
                    + path + "'");
        // Exclude any resource in the /WEB-INF and /META-INF subdirectories
        // (the "toUpperCase()" avoids problems on Windows systems)
        if (path == null || path.toUpperCase(Locale.ENGLISH).startsWith("/WEB-INF")
                || path.toUpperCase(Locale.ENGLISH).startsWith("/META-INF")) {
            res.sendError(HttpServletResponse.SC_NOT_FOUND, path);
            log("Can't serve file: " + path);
            return;
        }
        URL resource = servletContext.getResource(path);
        if (resource == null) {
            res.sendError(HttpServletResponse.SC_NOT_FOUND, path);
            log("Can't find file: " + path);
            return;
        }
        String resourceMimeType = servletContext.getMimeType(path);
        if (resourceMimeType == null) {
            resourceMimeType = "text/html";
        }
        res.setContentType(resourceMimeType + ";charset=" + outputEncoding);
        if (expires != null) {
            res.setDateHeader("Expires", (new java.util.Date()).getTime()
                    + expires.longValue() * 1000);
        }
        req.setAttribute(Globals.SSI_FLAG_ATTR, "true");
        processSSI(req, res, resource);
    }


    protected void processSSI(HttpServletRequest req, HttpServletResponse res,
            URL resource) throws IOException {
        SSIExternalResolver ssiExternalResolver =
            new SSIServletExternalResolver(getServletContext(), req, res,
                    isVirtualWebappRelative, debug, inputEncoding);
        SSIProcessor ssiProcessor = new SSIProcessor(ssiExternalResolver,
                debug);
        PrintWriter printWriter = null;
        StringWriter stringWriter = null;
        if (buffered) {
            stringWriter = new StringWriter();
            printWriter = new PrintWriter(stringWriter);
        } else {
            printWriter = res.getWriter();
        }

        URLConnection resourceInfo = resource.openConnection();
        try (InputStream resourceInputStream = resourceInfo.getInputStream()) {
            String encoding = resourceInfo.getContentEncoding();
            if (encoding == null) {
                encoding = inputEncoding;
            }
            InputStreamReader isr;
            if (encoding == null) {
                isr = new InputStreamReader(resourceInputStream);
            } else {
                isr = new InputStreamReader(resourceInputStream, encoding);
            }
            try (BufferedReader bufferedReader = new BufferedReader(isr)) { //this should clode isr as well
                long lastModified = ssiProcessor.process(bufferedReader,
                  resourceInfo.getLastModified(), printWriter);
                if (lastModified > 0) {
                    res.setDateHeader("last-modified", lastModified);
                }
                if (buffered) {
                    printWriter.flush();
                    String text = stringWriter.toString();
                    res.getWriter().write(text);
                }
            }
        }
    }
}

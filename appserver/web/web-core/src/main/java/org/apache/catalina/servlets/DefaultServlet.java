/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2016 Oracle and/or its affiliates. All rights reserved.
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
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.catalina.servlets;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.AccessController;
import java.text.MessageFormat;
import java.util.*;
import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.catalina.Globals;
import org.apache.catalina.LogFacade;
import org.apache.catalina.core.ContextsAdapterUtility;
import org.apache.catalina.util.ServerInfo;
import org.apache.catalina.util.URLEncoder;
import org.apache.naming.resources.CacheEntry;
import org.apache.naming.resources.ProxyDirContext;
import org.apache.naming.resources.Resource;
import org.apache.naming.resources.ResourceAttributes;
import org.apache.tomcat.util.security.PrivilegedGetTccl;
import org.apache.tomcat.util.security.PrivilegedSetTccl;
import org.glassfish.grizzly.http.server.util.AlternateDocBase;
import org.glassfish.web.util.HtmlEntityEncoder;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.ext.EntityResolver2;

/**
 * <p>The default resource-serving servlet for most web applications,
 * used to serve static resources such as HTML pages and images.
 * </p>
 * <p>
 * This servlet is intended to be mapped to <em>/</em> e.g.:
 * </p>
 * <pre>
 *   &lt;servlet-mapping&gt;
 *       &lt;servlet-name&gt;default&lt;/servlet-name&gt;
 *       &lt;url-pattern&gt;/&lt;/url-pattern&gt;
 *   &lt;/servlet-mapping&gt;
 * </pre>
 * <p>It can be mapped to sub-paths, however in all cases resources are served
 * from the web appplication resource root using the full path from the root
 * of the web application context.
 * <br/>e.g. given a web application structure:
 *</p>
 * <pre>
 * /context
 *   /images
 *     tomcat2.jpg
 *   /static
 *     /images
 *       tomcat.jpg
 * </pre>
 * <p>
 * ... and a servlet mapping that maps only <code>/static/*</code> to the default servlet:
 * </p>
 * <pre>
 *   &lt;servlet-mapping&gt;
 *       &lt;servlet-name&gt;default&lt;/servlet-name&gt;
 *       &lt;url-pattern&gt;/static/*&lt;/url-pattern&gt;
 *   &lt;/servlet-mapping&gt;
 * </pre>
 * <p>
 * Then a request to <code>/context/static/images/tomcat.jpg</code> will succeed
 * while a request to <code>/context/images/tomcat2.jpg</code> will fail. 
 * </p>
 * @author Craig R. McClanahan
 * @author Remy Maucherat
 * @version $Revision: 1.16 $ $Date: 2007/06/06 16:01:12 $
 */

public class DefaultServlet
    extends HttpServlet {

    protected static final ResourceBundle rb = LogFacade.getLogger().getResourceBundle();

    private static final DocumentBuilderFactory factory;

    private static final SecureEntityResolver secureEntityResolver;

    // ----------------------------------------------------- Instance Variables


    /**
     * The debugging detail level for this servlet.
     */
    protected int debug = 0;


    /**
     * The input buffer size to use when serving resources.
     */
    protected int input = 2048;


    /**
     * Should we generate directory listings?
     */
    protected volatile boolean listings = false;


    /**
     * The sorting mechanism for directory listings
     */
    protected SortedBy sortedBy = SortedBy.NAME;


    /**
     * Read only flag. By default, it's set to true.
     */
    protected boolean readOnly = true;


    /**
     * The output buffer size to use when serving resources.
     */
    protected int output = 2048;


    /**
     * Array containing the safe characters set.
     */
    protected static final URLEncoder urlEncoder;


    /**
     * Allow customized directory listing per directory.
     */
    protected String  localXsltFile = null;


    /**
     * Allow customized directory listing per context.
     */
    protected String contextXsltFile = null;


    /**
     * Allow customized directory listing per instance.
     */
    protected String  globalXsltFile = null;


    /**
     * Allow a readme file to be included.
     */
    protected String readmeFile = null;


    /**
     * Proxy directory context.
     */
    protected transient ProxyDirContext resources = null;


    /**
     * Alternate doc bases
     */
    protected transient ArrayList<AlternateDocBase> alternateDocBases = null;


    /**
     * File encoding to be used when reading static files. If none is specified
     * the platform default is used.
     */
    protected String fileEncoding = null;


    /**
     * Minimum size for sendfile usage in bytes.
     */
    protected int sendfileSize = 48 * 1024;
    
    
    /**
     * Should the Accept-Ranges: bytes header be send with static resources?
     */
    protected boolean useAcceptRanges = true;


    /**
     * Full range marker.
     */
    protected static final ArrayList<Range> FULL = new ArrayList<Range>();
    
    /**
     * The maximum number of items allowed in Range header.
     * -1 means unbounded.
     */
    protected int maxHeaderRangeItems = 10;

    
    // ----------------------------------------------------- Static Initializer


    /**
     * GMT timezone - all HTTP dates are on GMT
     */
    static {
        urlEncoder = new URLEncoder();
        urlEncoder.addSafeCharacter('-');
        urlEncoder.addSafeCharacter('_');
        urlEncoder.addSafeCharacter('.');
        urlEncoder.addSafeCharacter('*');
        urlEncoder.addSafeCharacter('/');

        if (Globals.IS_SECURITY_ENABLED) {
            factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setValidating(false);
            secureEntityResolver = new SecureEntityResolver();
        } else {
            factory = null;
            secureEntityResolver = null;
        }
    }


    /**
     * MIME multipart separation string
     */
    protected static final String mimeSeparation = "CATALINA_MIME_BOUNDARY";


    /**
     * JNDI resources name.
     */
    protected static final String RESOURCES_JNDI_NAME = "java:/comp/Resources";


    /**
     * Size of file transfer buffer in bytes.
     */
    protected static final int BUFFER_SIZE = 4096;


    // --------------------------------------------------------- Public Methods


    /**
     * Finalize this servlet.
     */
    public void destroy() {
        // NOOP
    }


    /**
     * Initialize this servlet.
     */
    public void init() throws ServletException {
        ServletConfig sc = getServletConfig();
        if (sc.getInitParameter("debug") != null)
            debug = Integer.parseInt(sc.getInitParameter("debug"));

        if (sc.getInitParameter("input") != null)
            input = Integer.parseInt(sc.getInitParameter("input"));

        if (sc.getInitParameter("output") != null)
            output = Integer.parseInt(sc.getInitParameter("output"));

        listings = Boolean.parseBoolean(sc.getInitParameter("listings"));

        String sortedByInitParam = sc.getInitParameter("sortedBy");
        if (sortedByInitParam != null) {
            sortedBy = Enum.valueOf(SortedBy.class, sortedByInitParam);
        }

        if (sc.getInitParameter("readonly") != null)
            readOnly = Boolean.parseBoolean(sc.getInitParameter("readonly"));

        if (sc.getInitParameter("sendfileSize") != null)
            sendfileSize = 
                Integer.parseInt(sc.getInitParameter("sendfileSize")) * 1024;

        if (sc.getInitParameter("maxHeaderRangeItems") != null) {
            maxHeaderRangeItems = 
                Integer.parseInt(sc.getInitParameter("maxHeaderRangeItems"));
        }

        fileEncoding = sc.getInitParameter("fileEncoding");

        globalXsltFile = sc.getInitParameter("globalXsltFile");
        contextXsltFile = sc.getInitParameter("contextXsltFile");
        localXsltFile = sc.getInitParameter("localXsltFile");
        readmeFile = sc.getInitParameter("readmeFile");

        if (sc.getInitParameter("useAcceptRanges") != null)
            useAcceptRanges = Boolean.parseBoolean(sc.getInitParameter("useAcceptRanges"));

        // Sanity check on the specified buffer sizes
        if (input < 256)
            input = 256;
        if (output < 256)
            output = 256;

        if (debug > 0) {
            log("DefaultServlet.init:  input buffer size=" + input +
                ", output buffer size=" + output);
        }

        // Load the proxy dir context.
        resources = (ProxyDirContext) getServletContext()
                .getAttribute(Globals.RESOURCES_ATTR);
        if (resources == null) {
            try {
                resources =
                    (ProxyDirContext) new InitialContext()
                    .lookup(RESOURCES_JNDI_NAME);
            } catch (NamingException e) {
                throw new ServletException("No resources", e);
            } catch (ClassCastException e) {
                // Failed : Not the right type
            }
        }

        if (resources == null) {
            throw new UnavailableException("No resources");
        }

        try {
            alternateDocBases = getAlternateDocBases();
        } catch(ClassCastException e) {
            // Failed : Not the right type
        }

    }


    @SuppressWarnings("unchecked")
    private ArrayList<AlternateDocBase> getAlternateDocBases() {

        return (ArrayList<AlternateDocBase>)
                getServletContext().getAttribute(
                Globals.ALTERNATE_RESOURCES_ATTR);
    }
    
    
    /**
     * Return if directory listings are enabled
     */
    public boolean isListings() {
        return this.listings;
    }

    
    /**
     * Enables or disables directory listings for this DefaultServlet.
     *
     * @param listings true if directory listings are to be enabled, false
     * otherwise
     */
    public void setListings(boolean listings) {
        this.listings = listings;
    }


    // ------------------------------------------------------ Protected Methods


    /**
     * Return the relative path associated with this servlet.
     *
     * @param request The servlet request we are processing
     */
    protected String getRelativePath(HttpServletRequest request) {
        // IMPORTANT: DefaultServlet can be mapped to '/' or '/path/*' but always
        // serves resources from the web app root with context rooted paths.
        // i.e. it can not be used to mount the web app root under a sub-path
        // This method must construct a complete context rooted path, although
        // subclasses can change this behaviour.

        // Are we being processed by a RequestDispatcher.include()?
        if (request.getAttribute(RequestDispatcher.INCLUDE_REQUEST_URI) != null) {
            String result = (String) request.getAttribute(
                RequestDispatcher.INCLUDE_PATH_INFO);
            if (result == null)
                result = (String) request.getAttribute(
                    RequestDispatcher.INCLUDE_SERVLET_PATH);
            if ((result == null) || (result.equals("")))
                result = "/";
            return (result);
        }

        // No, extract the desired path directly from the request
        String result = request.getPathInfo();
        if (result == null) {
            result = request.getServletPath();
        } else {
            result = request.getServletPath() + result;
        }
        if ((result == null) || (result.equals(""))) {
            result = "/";
        }
        return (result);

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
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
        throws IOException, ServletException {

        // Serve the requested resource, including the data content
        serveResource(request, response, true);

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
    protected void doHead(HttpServletRequest request,
                          HttpServletResponse response)
        throws IOException, ServletException {

        // Serve the requested resource, without the data content
        serveResource(request, response, false);

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
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
        throws IOException, ServletException {
        doGet(request, response);
    }


    /**
     * Process a POST request for the specified resource.
     *
     * @param req The servlet request we are processing
     * @param resp The servlet response we are creating
     *
     * @exception IOException if an input/output error occurs
     * @exception ServletException if a servlet-specified error occurs
     */
    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {

        if (readOnly) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String path = getRelativePath(req);

        boolean exists = true;
        try {
            resources.lookup(path);
        } catch (NamingException e) {
            exists = false;
        }

        boolean result = true;

        // Temp. content file used to support partial PUT
        File contentFile = null;

        Range range = parseContentRange(req, resp);

        InputStream resourceInputStream = null;

        // Append data specified in ranges to existing content for this
        // resource - create a temp. file on the local filesystem to
        // perform this operation
        // Assume just one range is specified for now
        if (range != null) {
            contentFile = executePartialPut(req, range, path);
            resourceInputStream = new FileInputStream(contentFile);
        } else {
            resourceInputStream = req.getInputStream();
        }

        try {
            Resource newResource = new Resource(resourceInputStream);
            // FIXME: Add attributes
            if (exists) {
                resources.rebind(path, newResource);
            } else {
                resources.bind(path, newResource);
            }
        } catch(NamingException e) {
            result = false;
        }

        if (result) {
            if (exists) {
                resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
            } else {
                resp.setStatus(HttpServletResponse.SC_CREATED);
            }
        } else {
            resp.sendError(HttpServletResponse.SC_CONFLICT);
        }

    }


    /**
     * Handle a partial PUT.  New content specified in request is appended to
     * existing content in oldRevisionContent (if present). This code does
     * not support simultaneous partial updates to the same resource.
     */
    protected File executePartialPut(HttpServletRequest req, Range range,
                                     String path)
        throws IOException {

        // Append data specified in ranges to existing content for this
        // resource - create a temp. file on the local filesystem to
        // perform this operation
        File tempDir = (File) getServletContext().getAttribute(
            ServletContext.TEMPDIR);
        // Convert all '/' characters to '.' in resourcePath
        String convertedResourcePath = path.replace('/', '.');
        File contentFile = new File(tempDir, convertedResourcePath);
        if (contentFile.createNewFile()) {
            // Clean up contentFile when Tomcat is terminated
            contentFile.deleteOnExit();
        }

        Resource oldResource = null;
        try {
            Object obj = resources.lookup(path);
            if (obj instanceof Resource)
                oldResource = (Resource) obj;
        } catch (NamingException e) {
            // Ignore
        }

        RandomAccessFile randAccessContentFile =
            new RandomAccessFile(contentFile, "rw");
        try {
            // Copy data in oldRevisionContent to contentFile
            if (oldResource != null) {
                BufferedInputStream bufOldRevStream = null;
                try {
                    bufOldRevStream = 
                        new BufferedInputStream(oldResource.streamContent(),
                                                BUFFER_SIZE);

                    int numBytesRead;
                    byte[] copyBuffer = new byte[BUFFER_SIZE];
                    while ((numBytesRead = bufOldRevStream.read(copyBuffer)) != -1) {
                        randAccessContentFile.write(copyBuffer, 0, numBytesRead);
                    }
                } finally {
                    if (bufOldRevStream != null) {
                        bufOldRevStream.close();
                    }
                }
            }

            randAccessContentFile.setLength(range.length);

            // Append data in request input stream to contentFile
            randAccessContentFile.seek(range.start);
            int numBytesRead;
            byte[] transferBuffer = new byte[BUFFER_SIZE];
            BufferedInputStream requestBufInStream = null;
            try {
                requestBufInStream =
                    new BufferedInputStream(req.getInputStream(), BUFFER_SIZE);

                while ((numBytesRead = requestBufInStream.read(transferBuffer)) != -1) {
                    randAccessContentFile.write(transferBuffer, 0, numBytesRead);
                }
            } finally {
                if (requestBufInStream != null) {
                    requestBufInStream.close();
                }
            }
        } finally {
            randAccessContentFile.close();
        }

        return contentFile;

    }


    /**
     * Process a POST request for the specified resource.
     *
     * @param req The servlet request we are processing
     * @param resp The servlet response we are creating
     *
     * @exception IOException if an input/output error occurs
     * @exception ServletException if a servlet-specified error occurs
     */
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {

        if (readOnly) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String path = getRelativePath(req);

        boolean exists = true;
        try {
            resources.lookup(path);
        } catch (NamingException e) {
            exists = false;
        }

        if (exists) {
            boolean result = true;
            try {
                resources.unbind(path);
            } catch (NamingException e) {
                result = false;
            }
            if (result) {
                resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
            } else {
                resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            }
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }

    }


    /**
     * Check if the conditions specified in the optional If headers are
     * satisfied.
     *
     * @param request The servlet request we are processing
     * @param response The servlet response we are creating
     * @param resourceAttributes The resource information
     * @return boolean true if the resource meets all the specified conditions,
     * and false if any of the conditions is not satisfied, in which case
     * request processing is stopped
     */
    protected boolean checkIfHeaders(HttpServletRequest request,
                                     HttpServletResponse response,
                                     ResourceAttributes resourceAttributes)
        throws IOException {

        return checkIfMatch(request, response, resourceAttributes)
            && checkIfModifiedSince(request, response, resourceAttributes)
            && checkIfNoneMatch(request, response, resourceAttributes)
            && checkIfUnmodifiedSince(request, response, resourceAttributes);

    }


    /**
     * URL rewriter.
     *
     * @param path Path which has to be rewritten
     */
    protected String rewriteUrl(String path) {
        return urlEncoder.encode( path );
    }


    /**
     * Display the size of a file.
     */
    protected void displaySize(StringBuilder buf, int filesize) {

        int leftside = filesize / 1024;
        int rightside = (filesize % 1024) / 103;  // makes 1 digit
        // To avoid 0.0 for non-zero file, we bump to 0.1
        if (leftside == 0 && rightside == 0 && filesize != 0)
            rightside = 1;
        buf.append(leftside).append(".").append(rightside);
        buf.append(" KB");

    }


    /**
     * Serve the specified resource, optionally including the data content.
     *
     * @param request The servlet request we are processing
     * @param response The servlet response we are creating
     * @param content Should the content be included?
     *
     * @exception IOException if an input/output error occurs
     * @exception ServletException if a servlet-specified error occurs
     */
    protected void serveResource(HttpServletRequest request,
                                 HttpServletResponse response,
                                 boolean content)
        throws IOException, ServletException {

        // Identify the requested resource path
        String path = getRelativePath(request);
        if (debug > 0) {
            if (content)
                log("DefaultServlet.serveResource:  Serving resource '" +
                    path + "' headers and data");
            else
                log("DefaultServlet.serveResource:  Serving resource '" +
                    path + "' headers only");
        }

        CacheEntry cacheEntry = null;
        ProxyDirContext proxyDirContext = resources;
        if (alternateDocBases == null
                || alternateDocBases.size() == 0) {
            cacheEntry = proxyDirContext.lookupCache(path);
        } else {
            AlternateDocBase match = AlternateDocBase.findMatch(
                                            path, alternateDocBases);
            if (match != null) {
                cacheEntry = ((ProxyDirContext) ContextsAdapterUtility.unwrap(match.getResources())).lookupCache(path);
            } else {
                // None of the url patterns for alternate docbases matched
                cacheEntry = proxyDirContext.lookupCache(path);
            }
        }

        if (!cacheEntry.exists) {
            // Check if we're included so we can return the appropriate 
            // missing resource name in the error
            String requestUri = (String) request.getAttribute(
                RequestDispatcher.INCLUDE_REQUEST_URI);
            /* IASRI 4878272 
            if (requestUri == null) {
                requestUri = request.getRequestURI();
            } else {
            */
            if (requestUri != null) {
                /*
                 * We're included, and the response.sendError() below is going
                 * to be ignored by the including resource (see SRV.8.3,
                 * "The Include Method").
                 * Therefore, the only way we can let the including resource
                 * know about the missing resource is by throwing an 
                 * exception
                */
                throw new FileNotFoundException(requestUri);
            }

            /* IASRI 4878272
            response.sendError(HttpServletResponse.SC_NOT_FOUND,
                               requestUri);
            */
            // BEGIN IASRI 4878272
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            // END IASRI 4878272
            return;
        }

        // If the resource is not a collection, and the resource path
        // ends with "/" or "\", return NOT FOUND
        if (cacheEntry.context == null) {
            if (path.endsWith("/") || (path.endsWith("\\"))) {
                /* IASRI 4878272
                // Check if we're included so we can return the appropriate 
                // missing resource name in the error
                String requestUri = (String) request.getAttribute(
                    RequestDispatcher.INCLUDE_REQUEST_URI);
                if (requestUri == null) {
                    requestUri = request.getRequestURI();
                }
                 response.sendError(HttpServletResponse.SC_NOT_FOUND,
                                    requestUri);
                */
                // BEGIN IASRI 4878272
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                // END IASRI 4878272
                return;
            }
        }

        // Check if the conditions specified in the optional If headers are
        // satisfied.
        if (cacheEntry.context == null) {

            // Checking If headers
            boolean included =
                (request.getAttribute(RequestDispatcher.INCLUDE_CONTEXT_PATH) != null);
            if (!included
                && !checkIfHeaders(request, response, cacheEntry.attributes)) {
                return;
            }

        }

        // Find content type.
        String contentType = cacheEntry.attributes.getMimeType();
        if (contentType == null && !cacheEntry.attributes.isMimeTypeInitialized()) {
            contentType = getServletContext().getMimeType(cacheEntry.name);
            cacheEntry.attributes.setMimeType(contentType);
        }

        ArrayList<Range> ranges = null;
        long contentLength = -1L;

        if (cacheEntry.context != null) {

            // Skip directory listings if we have been configured to
            // suppress them
            if (!listings) {
                /* IASRI 4878272
                 response.sendError(HttpServletResponse.SC_NOT_FOUND,
                                    request.getRequestURI());
                */
                // BEGIN IASRI 4878272
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                // END IASRI 4878272
                return;
            }
            contentType = "text/html;charset=UTF-8";

        } else {
            if (useAcceptRanges) {
                // Accept ranges header
                response.setHeader("Accept-Ranges", "bytes");
            }

            // Parse range specifier
            ranges = parseRange(request, response, cacheEntry.attributes);

            // ETag header
            response.setHeader("ETag", cacheEntry.attributes.getETag());

            // Last-Modified header
            response.setHeader("Last-Modified",
                    cacheEntry.attributes.getLastModifiedHttp());

            // Get content length
            contentLength = cacheEntry.attributes.getContentLength();
            // Special case for zero length files, which would cause a
            // (silent) ISE when setting the output buffer size
            if (contentLength == 0L) {
                content = false;
            }

        }

        ServletOutputStream ostream = null;
        PrintWriter writer = null;

        if (content) {

            // Trying to retrieve the servlet output stream

            try {
                ostream = response.getOutputStream();
            } catch (IllegalStateException e) {
                // If it fails, we try to get a Writer instead if we're
                // trying to serve a text file
                if ( (contentType == null)
                     || (contentType.startsWith("text"))
                     || (contentType.startsWith("xml")) ) {
                    writer = response.getWriter();
                } else {
                    throw e;
                }
            }

        }

        if ( (cacheEntry.context != null) 
                || ( ((ranges == null) || (ranges.isEmpty()))
                        && (request.getHeader("Range") == null) )
                || (ranges == FULL) ) {

            // Set the appropriate output headers
            if (contentType != null) {
                if (debug > 0)
                    log("DefaultServlet.serveFile:  contentType='" +
                        contentType + "'");
                response.setContentType(contentType);
            }
            if ((cacheEntry.resource != null) && (contentLength >= 0)) {
                if (debug > 0)
                    log("DefaultServlet.serveFile:  contentLength=" +
                        contentLength);
                if (contentLength < Integer.MAX_VALUE) {
                    response.setContentLength((int) contentLength);
                } else {
                    // Set the content-length as String to be able to use a long
                    response.setHeader("content-length", "" + contentLength);
                }
            }

            InputStream renderResult = null;
            if (cacheEntry.context != null) {

                if (content) {
                    // Serve the directory browser
                    renderResult =
                        render(request.getContextPath(), cacheEntry, proxyDirContext);
                }

            }

            // Copy the input stream to our output stream (if requested)
            if (content) {
                try {
                    response.setBufferSize(output);
                } catch (IllegalStateException e) {
                    // Silent catch
                }
                if (ostream != null) {
                    if (!checkSendfile(request, response, cacheEntry, contentLength, null))
                        copy(cacheEntry, renderResult, ostream);
                } else {
                    copy(cacheEntry, renderResult, writer);
                }
            }

        } else {

            if ((ranges == null) || (ranges.isEmpty()))
                return;

            if (maxHeaderRangeItems >= 0 && ranges.size() > maxHeaderRangeItems) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            // Partial content response.

            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);

            if (ranges.size() == 1) {

                Range range = ranges.get(0);
                response.addHeader("Content-Range", "bytes "
                                   + range.start
                                   + "-" + range.end + "/"
                                   + range.length);
                long length = range.end - range.start + 1;
                if (length < Integer.MAX_VALUE) {
                    response.setContentLength((int) length);
                } else {
                    // Set the content-length as String to be able to use a long
                    response.setHeader("content-length", "" + length);
                }

                if (contentType != null) {
                    if (debug > 0)
                        log("DefaultServlet.serveFile:  contentType='" +
                            contentType + "'");
                    response.setContentType(contentType);
                }

                if (content) {
                    try {
                        response.setBufferSize(output);
                    } catch (IllegalStateException e) {
                        // Silent catch
                    }
                    if (ostream != null) {
                        if (!checkSendfile(request, response, cacheEntry, range.end - range.start + 1, range))
                            copy(cacheEntry, ostream, range);
                    } else {
                        copy(cacheEntry, writer, range);
                    }
                }

            } else {

                response.setContentType("multipart/byteranges; boundary="
                                        + mimeSeparation);

                if (content) {
                    try {
                        response.setBufferSize(output);
                    } catch (IllegalStateException e) {
                        // Silent catch
                    }
                    if (ostream != null) {
                        copy(cacheEntry, ostream, ranges.iterator(),
                             contentType);
                    } else {
                        copy(cacheEntry, writer, ranges.iterator(),
                             contentType);
                    }
                }

            }

        }

    }


    /**
     * Parse the content-range header.
     *
     * @param request The servlet request we are processing
     * @param response The servlet response we are creating
     * @return Range
     */
    protected Range parseContentRange(HttpServletRequest request,
                                      HttpServletResponse response)
        throws IOException {

        // Retrieving the content-range header (if any is specified
        String rangeHeader = request.getHeader("Content-Range");

        if (rangeHeader == null)
            return null;

        // bytes is the only range unit supported
        if (!rangeHeader.startsWith("bytes")) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return null;
        }

        rangeHeader = rangeHeader.substring(6).trim();

        int dashPos = rangeHeader.indexOf('-');
        int slashPos = rangeHeader.indexOf('/');

        if (dashPos == -1) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return null;
        }

        if (slashPos == -1) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return null;
        }

        Range range = new Range();

        try {
            range.start = Long.parseLong(rangeHeader.substring(0, dashPos));
            range.end =
                Long.parseLong(rangeHeader.substring(dashPos + 1, slashPos));
            range.length = Long.parseLong
                (rangeHeader.substring(slashPos + 1, rangeHeader.length()));
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return null;
        }

        if (!range.validate()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return null;
        }

        return range;

    }


    /**
     * Parse the range header.
     *
     * @param request The servlet request we are processing
     * @param response The servlet response we are creating
     * @return Vector of ranges
     */
    protected ArrayList<Range> parseRange(HttpServletRequest request,
        HttpServletResponse response,
        ResourceAttributes resourceAttributes) throws IOException {

        // Checking If-Range
        String headerValue = request.getHeader("If-Range");

        if (headerValue != null) {

            long headerValueTime = (-1L);
            try {
                headerValueTime = request.getDateHeader("If-Range");
            } catch (IllegalArgumentException e) {
                // Ignore
            }

            String eTag = resourceAttributes.getETag();
            long lastModified = resourceAttributes.getLastModified();

            if (headerValueTime == (-1L)) {

                // If the ETag the client gave does not match the entity
                // etag, then the entire entity is returned.
                if (!eTag.equals(headerValue.trim()))
                    return FULL;

            } else {

                // If the timestamp of the entity the client got is older than
                // the last modification date of the entity, the entire entity
                // is returned.
                if (lastModified > (headerValueTime + 1000))
                    return FULL;

            }

        }

        long fileLength = resourceAttributes.getContentLength();

        if (fileLength == 0)
            return null;

        // Retrieving the range header (if any is specified
        String rangeHeader = request.getHeader("Range");

        if (rangeHeader == null)
            return null;
        // bytes is the only range unit supported (and I don't see the point
        // of adding new ones).
        if (!rangeHeader.startsWith("bytes")) {
            response.addHeader("Content-Range", "bytes */" + fileLength);
            response.sendError
                (HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
            return null;
        }

        rangeHeader = rangeHeader.substring(6);

        // Vector which will contain all the ranges which are successfully
        // parsed.
        ArrayList<Range> result = new ArrayList<Range>();
        StringTokenizer commaTokenizer = new StringTokenizer(rangeHeader, ",");

        // Parsing the range list
        while (commaTokenizer.hasMoreTokens()) {
            String rangeDefinition = commaTokenizer.nextToken().trim();

            Range currentRange = new Range();
            currentRange.length = fileLength;

            int dashPos = rangeDefinition.indexOf('-');

            if (dashPos == -1) {
                response.addHeader("Content-Range", "bytes */" + fileLength);
                response.sendError
                    (HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                return null;
            }

            if (dashPos == 0) {

                try {
                    long offset = Long.parseLong(rangeDefinition);
                    currentRange.start = fileLength + offset;
                    currentRange.end = fileLength - 1;
                } catch (NumberFormatException e) {
                    response.addHeader("Content-Range",
                                       "bytes */" + fileLength);
                    response.sendError
                        (HttpServletResponse
                         .SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                    return null;
                }

            } else {

                try {
                    currentRange.start = Long.parseLong
                        (rangeDefinition.substring(0, dashPos));
                    if (dashPos < rangeDefinition.length() - 1)
                        currentRange.end = Long.parseLong
                            (rangeDefinition.substring
                             (dashPos + 1, rangeDefinition.length()));
                    else
                        currentRange.end = fileLength - 1;
                } catch (NumberFormatException e) {
                    response.addHeader("Content-Range",
                                       "bytes */" + fileLength);
                    response.sendError
                        (HttpServletResponse
                         .SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                    return null;
                }

            }

            if (!currentRange.validate()) {
                response.addHeader("Content-Range", "bytes */" + fileLength);
                response.sendError
                    (HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                return null;
            }

            result.add(currentRange);
        }

        return result;
    }



    /**
     *  Decide which way to render. HTML or XML.
     */
    protected InputStream render(String contextPath, CacheEntry cacheEntry)
            throws IOException, ServletException {
        return render(contextPath, cacheEntry, resources);
    }

    private InputStream render(String contextPath, CacheEntry cacheEntry,
            ProxyDirContext proxyDirContext)
            throws IOException, ServletException {

        Source xsltSource = findXsltInputStream(cacheEntry.context);

        if (xsltSource == null) {
            return renderHtml(contextPath, cacheEntry, proxyDirContext);
        } else {
            return renderXml(contextPath, cacheEntry, xsltSource, proxyDirContext);
        }

    }

    /**
     * Return an InputStream to an HTML representation of the contents
     * of this directory.
     *
     * @param contextPath Context path to which our internal paths are
     *  relative
     */
    protected InputStream renderXml(String contextPath,
                                    CacheEntry cacheEntry,
                                    Source xsltSource)
            throws IOException, ServletException {
        return renderXml(contextPath, cacheEntry, xsltSource, resources);
    }

    private InputStream renderXml(String contextPath,
                                    CacheEntry cacheEntry,
                                    Source xsltSource,
                                    ProxyDirContext proxyDirContext)
            throws IOException, ServletException {

        StringBuilder sb = new StringBuilder();

        sb.append("<?xml version=\"1.0\"?>");
        sb.append("<listing ");
        sb.append(" contextPath='");
        sb.append(contextPath);
        sb.append("'");
        sb.append(" directory='");
        sb.append(cacheEntry.name);
        sb.append("' ");
        sb.append(" hasParent='").append(!cacheEntry.name.equals("/"));
        sb.append("'>");

        sb.append("<entries>");

        try {

            // Render the directory entries within this directory
            Enumeration<NameClassPair> enumeration =
                proxyDirContext.list(cacheEntry.name);
            if (sortedBy.equals(SortedBy.LAST_MODIFIED)) {
                ArrayList<NameClassPair> list = 
                    Collections.list(enumeration);
                Comparator<NameClassPair> c = new LastModifiedComparator(
                    proxyDirContext, cacheEntry.name);
                Collections.sort(list, c);
                enumeration = Collections.enumeration(list);
            } else if (sortedBy.equals(SortedBy.SIZE)) {
                ArrayList<NameClassPair> list = 
                    Collections.list(enumeration);
                Comparator<NameClassPair> c = new SizeComparator(
                    proxyDirContext, cacheEntry.name);
                Collections.sort(list, c);
                enumeration = Collections.enumeration(list);
            }

            // rewriteUrl(contextPath) is expensive. cache result for later reuse
            String rewrittenContextPath =  rewriteUrl(contextPath);

            while (enumeration.hasMoreElements()) {

                NameClassPair ncPair = enumeration.nextElement();
                String resourceName = ncPair.getName();
                String trimmed = resourceName/*.substring(trim)*/;
                if (trimmed.equalsIgnoreCase("WEB-INF") ||
                    trimmed.equalsIgnoreCase("META-INF") ||
                    trimmed.equalsIgnoreCase(localXsltFile))
                    continue;

                if ((cacheEntry.name + trimmed).equals(contextXsltFile))
                    continue;

                CacheEntry childCacheEntry =
                    proxyDirContext.lookupCache(cacheEntry.name + resourceName);
                if (!childCacheEntry.exists) {
                    continue;
                }

                sb.append("<entry");
                sb.append(" type='")
                  .append((childCacheEntry.context != null)?"dir":"file")
                  .append("'");
                sb.append(" urlPath='")
                  .append(rewrittenContextPath)
                  .append(rewriteUrl(cacheEntry.name + resourceName))
                  .append((childCacheEntry.context != null)?"/":"")
                  .append("'");
                if (childCacheEntry.resource != null) {
                    sb.append(" size='")
                      .append(renderSize(childCacheEntry.attributes.getContentLength()))
                      .append("'");
                }
                sb.append(" date='")
                  .append(childCacheEntry.attributes.getLastModifiedHttp())
                  .append("'");

                sb.append(">");
                sb.append(HtmlEntityEncoder.encodeXSS(trimmed));
                if (childCacheEntry.context != null)
                    sb.append("/");
                sb.append("</entry>");

            }

        } catch (NamingException e) {
            // Something went wrong
            throw new ServletException("Error accessing resource", e);
        }

        sb.append("</entries>");

        String readme = getReadme(cacheEntry.context);

        if (readme!=null) {
            sb.append("<readme><![CDATA[");
            sb.append(readme);
            sb.append("]]></readme>");
        }


        sb.append("</listing>");

        // Prevent possible memory leak. Ensure Transformer and
        // TransformerFactory are not loaded from the web application.
        ClassLoader original;
        if (Globals.IS_SECURITY_ENABLED) {
            PrivilegedGetTccl pa = new PrivilegedGetTccl();
            original = AccessController.doPrivileged(pa);
        } else {
            original = Thread.currentThread().getContextClassLoader();
        }
        try {
            if (Globals.IS_SECURITY_ENABLED) {
                PrivilegedSetTccl pa =
                        new PrivilegedSetTccl(DefaultServlet.class.getClassLoader());
                AccessController.doPrivileged(pa);
            } else {
                Thread.currentThread().setContextClassLoader(
                        DefaultServlet.class.getClassLoader());
            }
            TransformerFactory tFactory = TransformerFactory.newInstance();
            Source xmlSource = new StreamSource(new StringReader(sb.toString()));
            Transformer transformer = tFactory.newTransformer(xsltSource);

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            OutputStreamWriter osWriter = new OutputStreamWriter(stream, "UTF8");
            StreamResult out = new StreamResult(osWriter);
            transformer.transform(xmlSource, out);
            osWriter.flush();
            return (new ByteArrayInputStream(stream.toByteArray()));
        } catch (Exception e) {
            log("directory transform failure: " + e.getMessage());
            return renderHtml(contextPath, cacheEntry);
        } finally {
            if (Globals.IS_SECURITY_ENABLED) {
                PrivilegedSetTccl pa = new PrivilegedSetTccl(original);
                AccessController.doPrivileged(pa);
            } else {
                Thread.currentThread().setContextClassLoader(original);
            }
        }
    }

    /**
     * Return an InputStream to an HTML representation of the contents
     * of this directory.
     *
     * @param contextPath Context path to which our internal paths are
     *  relative
     */
    protected InputStream renderHtml (String contextPath, CacheEntry cacheEntry)
            throws IOException, ServletException {
        return renderHtml(contextPath, cacheEntry, resources);
    }

    private InputStream renderHtml (String contextPath, CacheEntry cacheEntry,
            ProxyDirContext proxyDirContext)
            throws IOException, ServletException {
        String name = cacheEntry.name;

        /*
        // Number of characters to trim from the beginnings of filenames
        int trim = name.length();
        if (!name.endsWith("/"))
            trim += 1;
        if (name.equals("/"))
            trim = 1;
        */

        // Prepare a writer to a buffered area
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        OutputStreamWriter osWriter = null;
        try {
            osWriter = new OutputStreamWriter(stream, "UTF8");
        } catch (Exception e) {
            // Should never happen
            osWriter = new OutputStreamWriter(stream);
        }
        PrintWriter writer = new PrintWriter(osWriter);

        StringBuilder sb = new StringBuilder();
        
        // rewriteUrl(contextPath) is expensive. cache result for later reuse
        String rewrittenContextPath =  rewriteUrl(contextPath);

        String dirTitle = MessageFormat.format(rb.getString(LogFacade.DIR_TITLE_INFO), name);

        // Render the page header
        sb.append("<html>\r\n");
        sb.append("<head>\r\n");
        sb.append("<title>");
        sb.append(dirTitle);
        sb.append("</title>\r\n");
        sb.append("<STYLE><!--");
        sb.append(org.apache.catalina.util.TomcatCSS.TOMCAT_CSS);
        sb.append("--></STYLE> ");
        sb.append("</head>\r\n");
        sb.append("<body>");
        sb.append("<h1>");
        sb.append(dirTitle);

        // Render the link to our parent (if required)
        String parentDirectory = name;

        if (parentDirectory.endsWith("/")) {
            parentDirectory =
                parentDirectory.substring(0, parentDirectory.length() - 1);
        }
        int slash = parentDirectory.lastIndexOf('/');
        if (slash >= 0) {
            String parent = name.substring(0, slash);
            String dirParent = MessageFormat.format(rb.getString(LogFacade.DIR_PARENT_INFO), parent);
            sb.append(" - <a href=\"");
            sb.append(rewrittenContextPath);
            if (parent.equals(""))
                parent = "/";
            sb.append(rewriteUrl(parent));
            if (!parent.endsWith("/"))
                sb.append("/");
            sb.append("\">");
            sb.append("<b>");
            sb.append(dirParent);
            sb.append("</b>");
            sb.append("</a>");
        }

        sb.append("</h1>");
        sb.append("<HR size=\"1\" noshade=\"noshade\">");

        sb.append("<table width=\"100%\" cellspacing=\"0\"" +
                     " cellpadding=\"5\" align=\"center\">\r\n");

        // Render the column headings
        sb.append("<tr>\r\n");
        sb.append("<td align=\"left\"><font size=\"+1\"><strong>");
        sb.append(rb.getString(LogFacade.DIR_FILENAME_INFO));
        sb.append("</strong></font></td>\r\n");
        sb.append("<td align=\"center\"><font size=\"+1\"><strong>");
        sb.append(rb.getString(LogFacade.DIR_SIZE_INFO));
        sb.append("</strong></font></td>\r\n");
        sb.append("<td align=\"right\"><font size=\"+1\"><strong>");
        sb.append(rb.getString(LogFacade.DIR_LAST_MODIFIED_INFO));
        sb.append("</strong></font></td>\r\n");
        sb.append("</tr>");

        try {

            // Render the directory entries within this directory
            Enumeration<NameClassPair> enumeration =
                proxyDirContext.list(cacheEntry.name);
            if (sortedBy.equals(SortedBy.LAST_MODIFIED)) {
                ArrayList<NameClassPair> list = 
                    Collections.list(enumeration);
                Comparator<NameClassPair> c = new LastModifiedComparator(
                    proxyDirContext, cacheEntry.name);
                Collections.sort(list, c);
                enumeration = Collections.enumeration(list);
            } else if (sortedBy.equals(SortedBy.SIZE)) {
                ArrayList<NameClassPair> list = 
                    Collections.list(enumeration);
                Comparator<NameClassPair> c = new SizeComparator(
                    proxyDirContext, cacheEntry.name);
                Collections.sort(list, c);
                enumeration = Collections.enumeration(list);
            }

            boolean shade = false;
            while (enumeration.hasMoreElements()) {

                NameClassPair ncPair = enumeration.nextElement();
                String resourceName = ncPair.getName();
                String trimmed = resourceName/*.substring(trim)*/;
                if (trimmed.equalsIgnoreCase("WEB-INF") ||
                    trimmed.equalsIgnoreCase("META-INF"))
                    continue;

                CacheEntry childCacheEntry =
                    proxyDirContext.lookupCache(cacheEntry.name + resourceName);
                if (!childCacheEntry.exists) {
                    continue;
                }

                sb.append("<tr");
                if (shade)
                    sb.append(" bgcolor=\"#eeeeee\"");
                sb.append(">\r\n");
                shade = !shade;

                sb.append("<td align=\"left\">&nbsp;&nbsp;\r\n");
                sb.append("<a href=\"");
                sb.append(rewrittenContextPath);
                resourceName = rewriteUrl(name + resourceName);
                sb.append(resourceName);
                if (childCacheEntry.context != null)
                    sb.append("/");
                sb.append("\"><tt>");
                sb.append(HtmlEntityEncoder.encodeXSS(trimmed));
                if (childCacheEntry.context != null)
                    sb.append("/");
                sb.append("</tt></a></td>\r\n");

                sb.append("<td align=\"right\"><tt>");
                if (childCacheEntry.context != null)
                    sb.append("&nbsp;");
                else
                    sb.append(renderSize(childCacheEntry.attributes.getContentLength()));
                sb.append("</tt></td>\r\n");

                sb.append("<td align=\"right\"><tt>");
                sb.append(childCacheEntry.attributes.getLastModifiedHttp());
                sb.append("</tt></td>\r\n");

                sb.append("</tr>\r\n");
            }

        } catch (NamingException e) {
            // Something went wrong
            throw new ServletException("Error accessing resource", e);
        }

        // Render the page footer
        sb.append("</table>\r\n");

        sb.append("<HR size=\"1\" noshade=\"noshade\">");

        String readme = getReadme(cacheEntry.context);
        if (readme!=null) {
            sb.append(readme);
            sb.append("<HR size=\"1\" noshade=\"noshade\">");
        }

        String serverInfo = ServerInfo.getPublicServerInfo();
        if (serverInfo != null && !serverInfo.isEmpty()) {
            sb.append("<h3>").append(serverInfo).append("</h3>");
        }
        sb.append("</body>\r\n");
        sb.append("</html>\r\n");

        // Return an input stream to the underlying bytes
        writer.write(sb.toString());
        writer.flush();
        return (new ByteArrayInputStream(stream.toByteArray()));

    }


    /**
     * Render the specified file size (in bytes).
     *
     * @param size File size (in bytes)
     */
    protected String renderSize(long size) {

        long leftSide = size / 1024;
        long rightSide = (size % 1024) / 103;   // Makes 1 digit
        if ((leftSide == 0) && (rightSide == 0) && (size > 0))
            rightSide = 1;

        return ("" + leftSide + "." + rightSide + " kb");

    }


    /**
     * Get the readme file as a string.
     */
    protected String getReadme(DirContext directory)
        throws IOException, ServletException {

        if (readmeFile!=null) {
            try {
                Object obj = directory.lookup(readmeFile);

                if (obj!=null && obj instanceof Resource) {
                    StringWriter buffer = new StringWriter();
                    InputStream is = ((Resource)obj).streamContent();
                    copyRange(new InputStreamReader(is),
                              new PrintWriter(buffer));

                    return buffer.toString();
                 }
             } catch(Throwable e) {
                 ; /* Should only be IOException or NamingException
                    * can be ignored
                    */
                 if (debug > 10)
                    log("readme '" + readmeFile + "' not found", e);
             }
        }

        return null;
    }


    /**
     * Return a Source for the xsl template (if possible)
     */
    protected Source findXsltInputStream(DirContext directory)
        throws IOException, ServletException {

        if (localXsltFile!=null) {
            try {
                Object obj = directory.lookup(localXsltFile);
                if (obj!=null && obj instanceof Resource) {
                    InputStream is = ((Resource)obj).streamContent();
                    if (is != null) {
                        if (Globals.IS_SECURITY_ENABLED) {
                            return secureXslt(is);
                        } else {
                            return new StreamSource(is);
                        }
                    }
                }
             } catch(Throwable e) {
                 ; /* Should only be IOException or NamingException
                    * can be ignored
                    */
                if (debug > 10)
                    log("localXsltFile '" + localXsltFile + "' not found", e);

                return null;
             }
        }

        if (contextXsltFile != null) {
            InputStream is =
                getServletContext().getResourceAsStream(contextXsltFile);
            if (is != null) {
                if (Globals.IS_SECURITY_ENABLED) {
                    return secureXslt(is);
                } else {
                    return new StreamSource(is);
                }
            }

            if (debug > 10)
                log("contextXsltFile '" + contextXsltFile + "' not found");
        }

        /*  Open and read in file in one fell swoop to reduce chance
         *  chance of leaving handle open.
         */
        if (globalXsltFile!=null) {
            File f = validateGlobalXsltFile();
            if (f != null){
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(f);
                    long len = f.length();
                    byte b[] = new byte[(int)len]; /* danger! */
                    if (len != fis.read(b)) {
                        throw new IOException(MessageFormat.format(LogFacade.READ_FILE_EXCEPTION, f.getAbsolutePath()));
                    }
                    return new StreamSource(new ByteArrayInputStream(b));
                } finally {
                    if (fis != null) {
                        try {
                            fis.close();
                        } catch (IOException ioe) {
                            if (debug > 10) {
                                log(ioe.getMessage(), ioe);
                            }
                        }
                    }
                }
            }
        }

        return null;

    }

    private File validateGlobalXsltFile() {

        File result = null;
        String base = System.getProperty("catalina.base");

        if (base != null) {
            File baseConf = new File(base, "conf");
            result = validateGlobalXsltFile(baseConf);
        }

        if (result == null) {
            String home = System.getProperty("catalina.home");
            if (home != null && !home.equals(base)) {
                File homeConf = new File(home, "conf");
                result = validateGlobalXsltFile(homeConf);
            }
        }

        return result;
    }


    private File validateGlobalXsltFile(File base) {
        File candidate = new File(globalXsltFile);
        if (!candidate.isAbsolute()) {
            candidate = new File(base, globalXsltFile);
        }

        if (!candidate.isFile()) {
            return null;
        }

        // First check that the resulting path is under the provided base
        try {
            if (!candidate.getCanonicalPath().startsWith(base.getCanonicalPath())) {
                return null;
            }
        } catch (IOException ioe) {
            return null;
        }

        // Next check that an .xsl or .xslt file has been specified
        String nameLower = candidate.getName().toLowerCase(Locale.ENGLISH);
        if (!nameLower.endsWith(".xslt") && !nameLower.endsWith(".xsl")) {
            return null;
        }

        return candidate;
    }


    private Source secureXslt(InputStream is) {
        // Need to filter out any external entities
        Source result = null;
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setEntityResolver(secureEntityResolver);
            Document document = builder.parse(is);
            result = new DOMSource(document);
        } catch (ParserConfigurationException e) {
            if (debug > 0) {
                log(e.getMessage(), e);
            }
        } catch (SAXException e) {
            if (debug > 0) {
                log(e.getMessage(), e);
            }
        } catch (IOException e) {
            if (debug > 0) {
                log(e.getMessage(), e);
            }
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    if (debug > 10) {
                        log(e.getMessage(), e);
                    }
                }
            }
        }
        return result;
    }
 

    // -------------------------------------------------------- protected Methods


    /**
     * Check if sendfile can be used.
     */
    protected boolean checkSendfile(HttpServletRequest request,
                                  HttpServletResponse response,
                                  CacheEntry entry,
                                  long length, Range range) {
        if ((sendfileSize > 0)
            && (entry.resource != null)
            && ((length > sendfileSize) || (entry.resource.getContent() == null))
            && (entry.attributes.getCanonicalPath() != null)
            && (Boolean.TRUE.equals(request.getAttribute("org.apache.tomcat.sendfile.support")))
            && (request.getClass().getName().equals("org.apache.catalina.connector.RequestFacade"))
            && (response.getClass().getName().equals("org.apache.catalina.connector.ResponseFacade"))) {
            request.setAttribute("org.apache.tomcat.sendfile.filename", entry.attributes.getCanonicalPath());
            if (range == null) {
                request.setAttribute("org.apache.tomcat.sendfile.start", Long.valueOf(0L));
                request.setAttribute("org.apache.tomcat.sendfile.end", Long.valueOf(length));
            } else {
                request.setAttribute("org.apache.tomcat.sendfile.start", Long.valueOf(range.start));
                request.setAttribute("org.apache.tomcat.sendfile.end", Long.valueOf(range.end + 1));
            }
            request.setAttribute("org.apache.tomcat.sendfile.token", this);
            return true;
        } else {
            return false;
        }
    }


    /**
     * Check if the if-match condition is satisfied.
     *
     * @param request The servlet request we are processing
     * @param response The servlet response we are creating
     * @param resourceAttributes File object
     * @return boolean true if the resource meets the specified condition,
     * and false if the condition is not satisfied, in which case request
     * processing is stopped
     */
    protected boolean checkIfMatch(HttpServletRequest request,
                                 HttpServletResponse response,
                                 ResourceAttributes resourceAttributes)
        throws IOException {

        String eTag = resourceAttributes.getETag();
        String headerValue = request.getHeader("If-Match");
        if (headerValue != null) {
            if (headerValue.indexOf('*') == -1) {

                StringTokenizer commaTokenizer = new StringTokenizer
                    (headerValue, ",");
                boolean conditionSatisfied = false;

                while (!conditionSatisfied && commaTokenizer.hasMoreTokens()) {
                    String currentToken = commaTokenizer.nextToken();
                    if (currentToken.trim().equals(eTag))
                        conditionSatisfied = true;
                }

                // If none of the given ETags match, 412 Precodition failed is
                // sent back
                if (!conditionSatisfied) {
                    response.sendError
                        (HttpServletResponse.SC_PRECONDITION_FAILED);
                    return false;
                }

            }
        }
        return true;

    }


    /**
     * Check if the if-modified-since condition is satisfied.
     *
     * @param request The servlet request we are processing
     * @param response The servlet response we are creating
     * @param resourceAttributes File object
     * @return boolean true if the resource meets the specified condition,
     * and false if the condition is not satisfied, in which case request
     * processing is stopped
     */
    protected boolean checkIfModifiedSince(HttpServletRequest request,
                                           HttpServletResponse response,
                                           ResourceAttributes resourceAttributes)
        throws IOException {
        try {
            long headerValue = request.getDateHeader("If-Modified-Since");
            long lastModified = resourceAttributes.getLastModified();
            if (headerValue != -1) {

                // If an If-None-Match header has been specified,
                // If-Modified-Since is ignored.
                if ((request.getHeader("If-None-Match") == null)
                    && (lastModified < headerValue + 1000)) {
                    // The entity has not been modified since the date
                    // specified by the client. This is not an error case.
                    response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                    response.setHeader("ETag", resourceAttributes.getETag());
                    return false;
                }
            }
        } catch(IllegalArgumentException illegalArgument) {
            return true;
        }
        return true;

    }


    /**
     * Check if the if-none-match condition is satisfied.
     *
     * @param request The servlet request we are processing
     * @param response The servlet response we are creating
     * @param resourceAttributes File object
     * @return boolean true if the resource meets the specified condition,
     * and false if the condition is not satisfied, in which case request
     * processing is stopped
     */
    protected boolean checkIfNoneMatch(HttpServletRequest request,
                                       HttpServletResponse response,
                                       ResourceAttributes resourceAttributes)
        throws IOException {

        String eTag = resourceAttributes.getETag();
        String headerValue = request.getHeader("If-None-Match");
        if (headerValue != null) {

            boolean conditionSatisfied = false;

            if (!headerValue.equals("*")) {

                StringTokenizer commaTokenizer =
                    new StringTokenizer(headerValue, ",");

                while (!conditionSatisfied && commaTokenizer.hasMoreTokens()) {
                    String currentToken = commaTokenizer.nextToken();
                    if (currentToken.trim().equals(eTag))
                        conditionSatisfied = true;
                }

            } else {
                conditionSatisfied = true;
            }

            if (conditionSatisfied) {

                // For GET and HEAD, we should respond with
                // 304 Not Modified.
                // For every other method, 412 Precondition Failed is sent
                // back.
                if ( ("GET".equals(request.getMethod()))
                     || ("HEAD".equals(request.getMethod())) ) {
                    response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                    response.setHeader("ETag", eTag);
                    return false;
                } else {
                    response.sendError
                        (HttpServletResponse.SC_PRECONDITION_FAILED);
                    return false;
                }
            }
        }
        return true;

    }


    /**
     * Check if the if-unmodified-since condition is satisfied.
     *
     * @param request The servlet request we are processing
     * @param response The servlet response we are creating
     * @param resourceAttributes File object
     * @return boolean true if the resource meets the specified condition,
     * and false if the condition is not satisfied, in which case request
     * processing is stopped
     */
    protected boolean checkIfUnmodifiedSince(HttpServletRequest request,
                                           HttpServletResponse response,
                                           ResourceAttributes resourceAttributes)
        throws IOException {
        try {
            long lastModified = resourceAttributes.getLastModified();
            long headerValue = request.getDateHeader("If-Unmodified-Since");
            if (headerValue != -1) {
                if ( lastModified >= (headerValue + 1000)) {
                    // The entity has not been modified since the date
                    // specified by the client. This is not an error case.
                    response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
                    return false;
                }
            }
        } catch(IllegalArgumentException illegalArgument) {
            return true;
        }
        return true;

    }


    /**
     * Copy the contents of the specified input stream to the specified
     * output stream, and ensure that both streams are closed before returning
     * (even in the face of an exception).
     *
     * @param cacheEntry The CacheEntry object
     * @param is The InputStream
     * @param ostream The output stream to write to
     *
     * @exception IOException if an input/output error occurs
     */
    protected void copy(CacheEntry cacheEntry, InputStream is,
                        ServletOutputStream ostream)
        throws IOException {

        IOException exception = null;
        InputStream resourceInputStream = null;

        // Optimization: If the binary content has already been loaded, send
        // it directly
        if (cacheEntry.resource != null) {
            byte buffer[] = cacheEntry.resource.getContent();
            if (buffer != null) {
                ostream.write(buffer, 0, buffer.length);
                return;
            }
            resourceInputStream = cacheEntry.resource.streamContent();
        } else {
            resourceInputStream = is;
        }

        InputStream istream = new BufferedInputStream
            (resourceInputStream, input);

        try {
            // Copy the input stream to the output stream
            exception = copyRange(istream, ostream);
        } finally {
            // Clean up the input stream
            istream.close();
        }

        // Rethrow any exception that has occurred
        if (exception != null)
            throw exception;

    }


    /**
     * Copy the contents of the specified input stream to the specified
     * output stream, and ensure that both streams are closed before returning
     * (even in the face of an exception).
     *
     * @param cacheEntry The cache entry
     * @param is The InputStream
     * @param writer The writer to write to
     *
     * @exception IOException if an input/output error occurs
     */
    protected void copy(CacheEntry cacheEntry, InputStream is,
                        PrintWriter writer)
        throws IOException {

        IOException exception = null;

        InputStream resourceInputStream = null;
        if (cacheEntry.resource != null) {
            resourceInputStream = cacheEntry.resource.streamContent();
        } else {
            resourceInputStream = is;
        }

        Reader reader;
        if (fileEncoding == null) {
            reader = new InputStreamReader(resourceInputStream);
        } else {
            reader = new InputStreamReader(resourceInputStream,
                                           fileEncoding);
        }

        // Copy the input stream to the output stream
        exception = copyRange(reader, writer);

        // Clean up the reader
        reader.close();

        // Rethrow any exception that has occurred
        if (exception != null)
            throw exception;

    }


    /**
     * Copy the contents of the specified input stream to the specified
     * output stream, and ensure that both streams are closed before returning
     * (even in the face of an exception).
     *
     * @param cacheEntry The CacheEntry object
     * @param ostream The output stream to write to
     * @param range Range the client wanted to retrieve
     * @exception IOException if an input/output error occurs
     */
    protected void copy(CacheEntry cacheEntry, ServletOutputStream ostream,
                        Range range)
        throws IOException {

        IOException exception = null;

        InputStream resourceInputStream = cacheEntry.resource.streamContent();
        InputStream istream =
            new BufferedInputStream(resourceInputStream, input);
        try {
            exception = copyRange(istream, ostream, range.start, range.end);
        } finally {
            // Clean up the input stream
            istream.close();
        }

        // Rethrow any exception that has occurred
        if (exception != null)
            throw exception;

    }


    /**
     * Copy the contents of the specified input stream to the specified
     * output stream, and ensure that both streams are closed before returning
     * (even in the face of an exception).
     *
     * @param cacheEntry The CacheEntry object
     * @param writer The writer to write to
     * @param range Range the client wanted to retrieve
     * @exception IOException if an input/output error occurs
     */
    protected void copy(CacheEntry cacheEntry, PrintWriter writer,
                        Range range)
        throws IOException {

        IOException exception = null;

        InputStream resourceInputStream = cacheEntry.resource.streamContent();

        Reader reader;
        if (fileEncoding == null) {
            reader = new InputStreamReader(resourceInputStream);
        } else {
            reader = new InputStreamReader(resourceInputStream,
                                           fileEncoding);
        }

        exception = copyRange(reader, writer, range.start, range.end);

        // Clean up the input stream
        reader.close();

        // Rethrow any exception that has occurred
        if (exception != null)
            throw exception;

    }


    /**
     * Copy the contents of the specified input stream to the specified
     * output stream, and ensure that both streams are closed before returning
     * (even in the face of an exception).
     *
     * @param cacheEntry The CacheEntry object
     * @param ostream The output stream to write to
     * @param ranges Enumeration of the ranges the client wanted to retrieve
     * @param contentType Content type of the resource
     * @exception IOException if an input/output error occurs
     */
    protected void copy(CacheEntry cacheEntry, ServletOutputStream ostream,
                        Iterator<Range> ranges, String contentType)
        throws IOException {

        IOException exception = null;

        while ( (exception == null) && (ranges.hasNext()) ) {

            InputStream resourceInputStream = cacheEntry.resource.streamContent();
            InputStream istream = null;
            try {
                istream = 
                    new BufferedInputStream(resourceInputStream, input);

                Range currentRange = ranges.next();

                // Writing MIME header.
                ostream.println();
                ostream.println("--" + mimeSeparation);
                if (contentType != null)
                    ostream.println("Content-Type: " + contentType);
                ostream.println("Content-Range: bytes " + currentRange.start
                               + "-" + currentRange.end + "/"
                               + currentRange.length);
                ostream.println();

                // Printing content
                exception = copyRange(istream, ostream, currentRange.start,
                                      currentRange.end);

            } finally {
                if (istream != null) {
                    istream.close();
                }
            }
        }

        ostream.println();
        ostream.print("--" + mimeSeparation + "--");

        // Rethrow any exception that has occurred
        if (exception != null)
            throw exception;

    }


    /**
     * Copy the contents of the specified input stream to the specified
     * output stream, and ensure that both streams are closed before returning
     * (even in the face of an exception).
     *
     * @param cacheEntry The CacheEntry object
     * @param writer The writer to write to
     * @param ranges Enumeration of the ranges the client wanted to retrieve
     * @param contentType Content type of the resource
     * @exception IOException if an input/output error occurs
     */
    protected void copy(CacheEntry cacheEntry, PrintWriter writer,
                        Iterator<Range> ranges, String contentType)
        throws IOException {

        IOException exception = null;

        while ( (exception == null) && (ranges.hasNext()) ) {

            InputStream resourceInputStream = cacheEntry.resource.streamContent();
            
            Reader reader;
            if (fileEncoding == null) {
                reader = new InputStreamReader(resourceInputStream);
            } else {
                reader = new InputStreamReader(resourceInputStream,
                                               fileEncoding);
            }

            Range currentRange = ranges.next();

            // Writing MIME header.
            writer.println();
            writer.println("--" + mimeSeparation);
            if (contentType != null)
                writer.println("Content-Type: " + contentType);
            writer.println("Content-Range: bytes " + currentRange.start
                           + "-" + currentRange.end + "/"
                           + currentRange.length);
            writer.println();

            // Printing content
            exception = copyRange(reader, writer, currentRange.start,
                                  currentRange.end);

            reader.close();  
        }

        writer.println();
        writer.print("--" + mimeSeparation + "--");

        // Rethrow any exception that has occurred
        if (exception != null)
            throw exception;

    }


    /**
     * Copy the contents of the specified input stream to the specified
     * output stream, and ensure that both streams are closed before returning
     * (even in the face of an exception).
     *
     * @param istream The input stream to read from
     * @param ostream The output stream to write to
     * @return Exception which occurred during processing
     */
    protected IOException copyRange(InputStream istream,
                                    ServletOutputStream ostream) {
        // Copy the input stream to the output stream
        IOException exception = null;
        byte buffer[] = new byte[input];
        int len;
        while (true) {
            try {
                len = istream.read(buffer);
                if (len == -1)
                    break;
                ostream.write(buffer, 0, len);
            } catch (IOException e) {
                exception = e;
                break;
            }
        }
        return exception;

    }


    /**
     * Copy the contents of the specified input stream to the specified
     * output stream, and ensure that both streams are closed before returning
     * (even in the face of an exception).
     *
     * @param reader The reader to read from
     * @param writer The writer to write to
     * @return Exception which occurred during processing
     */
    protected IOException copyRange(Reader reader, PrintWriter writer) {
        // Copy the input stream to the output stream
        IOException exception = null;
        char buffer[] = new char[input];
        int len;
        while (true) {
            try {
                len = reader.read(buffer);
                if (len == -1)
                    break;
                writer.write(buffer, 0, len);
            } catch (IOException e) {
                exception = e;
                break;
            }
        }
        return exception;

    }


    /**
     * Copy the contents of the specified input stream to the specified
     * output stream, and ensure that both streams are closed before returning
     * (even in the face of an exception).
     *
     * @param istream The input stream to read from
     * @param ostream The output stream to write to
     * @param start Start of the range which will be copied
     * @param end End of the range which will be copied
     * @return Exception which occurred during processing
     */
    protected IOException copyRange(InputStream istream,
                                    ServletOutputStream ostream,
                                    long start, long end) {
        if (debug > 10)
            log("Serving bytes:" + start + "-" + end);

        long skipped = 0;
        try {
            skipped = istream.skip(start);
        } catch (IOException e) {
            return e;
        }
        if (skipped < start) {
            String msg = MessageFormat.format(rb.getString(LogFacade.SKIP_BYTES_EXCEPTION),
                                              new Object[] {Long.valueOf(skipped),
                                                            Long.valueOf(start)});
            return new IOException(msg);
        }

        IOException exception = null;
        long bytesToRead = end - start + 1;

        byte buffer[] = new byte[input];
        int len = buffer.length;
        while ( (bytesToRead > 0) && (len >= buffer.length)) {
            try {
                len = istream.read(buffer);
                if (bytesToRead >= len) {
                    ostream.write(buffer, 0, len);
                    bytesToRead -= len;
                } else {
                    ostream.write(buffer, 0, (int) bytesToRead);
                    bytesToRead = 0;
                }
            } catch (IOException e) {
                exception = e;
                len = -1;
            }
            if (len < buffer.length)
                break;
        }

        return exception;

    }


    /**
     * Copy the contents of the specified input stream to the specified
     * output stream, and ensure that both streams are closed before returning
     * (even in the face of an exception).
     *
     * @param reader The reader to read from
     * @param writer The writer to write to
     * @param start Start of the range which will be copied
     * @param end End of the range which will be copied
     * @return Exception which occurred during processing
     */
    protected IOException copyRange(Reader reader, PrintWriter writer,
                                    long start, long end) {

        long skipped = 0;
        try {
            skipped = reader.skip(start);
        } catch (IOException e) {
            return e;
        }
        if (skipped < start) {
            String msg = MessageFormat.format(rb.getString(LogFacade.SKIP_BYTES_EXCEPTION),
                                              new Object[] {Long.valueOf(skipped),
                                                            Long.valueOf(start)});
            return new IOException(msg);
        } 

        IOException exception = null;
        long bytesToRead = end - start + 1;

        char buffer[] = new char[input];
        int len = buffer.length;
        while ( (bytesToRead > 0) && (len >= buffer.length)) {
            try {
                len = reader.read(buffer);
                if (bytesToRead >= len) {
                    writer.write(buffer, 0, len);
                    bytesToRead -= len;
                } else {
                    writer.write(buffer, 0, (int) bytesToRead);
                    bytesToRead = 0;
                }
            } catch (IOException e) {
                exception = e;
                len = -1;
            }
            if (len < buffer.length)
                break;
        }

        return exception;

    }


    // ------------------------------------------------------ Inner Classes

    protected static class Range {

        public long start;
        public long end;
        public long length;

        /**
         * Validate range.
         */
        public boolean validate() {
            if (end >= length)
                end = length - 1;
            return ( (start >= 0) && (end >= 0) && (start <= end)
                     && (length > 0) );
        }
    }

    /**
     * Enumeration of sorting mechanisms for directory listings.
     */
    private enum SortedBy {
        NAME,
        LAST_MODIFIED,
        SIZE
    }

    /**
     * Comparator which sorts directory listings by their creation
     * or lastModified date
     *
     * This comparator class cannot be used with TreeSet and TreeMap
     * as it is not Serializable.
     */
    private static class LastModifiedComparator
            implements Comparator<NameClassPair> {

        private ProxyDirContext resources;
        private String dirName;

        public LastModifiedComparator(ProxyDirContext resources,
                                      String dirName) {
            this.resources = resources;
            this.dirName = dirName;
        }

        public int compare(NameClassPair p1, NameClassPair p2) {

            CacheEntry ce1 = resources.lookupCache(
                dirName + p1.getName());
            Date date1 = ce1.attributes.getCreationOrLastModifiedDate();
            CacheEntry ce2 = resources.lookupCache(
                dirName + p2.getName());
            Date date2 = ce2.attributes.getCreationOrLastModifiedDate();
            if (date1.before(date2)) {
                return -1;
            } else if (date1.after(date2)) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    /**
     * Comparator which sorts directory listings by their file size
     *
     * This comparator class cannot be used with TreeSet and TreeMap
     * as it is not Serializable.
     */
    private static class SizeComparator
            implements Comparator<NameClassPair> {

        private ProxyDirContext resources;
        private String dirName;

        public SizeComparator(ProxyDirContext resources,
                              String dirName) {
            this.resources = resources;
            this.dirName = dirName;
        }

        public int compare(NameClassPair p1, NameClassPair p2) {

            CacheEntry ce1 = resources.lookupCache(
                dirName + p1.getName());
            long size1 = ce1.attributes.getContentLength();
            CacheEntry ce2 = resources.lookupCache(
                dirName + p2.getName());
            long size2 = ce2.attributes.getContentLength();
            if (size1 < size2) {
                return -1;
            } else if (size1 > size2) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    /**
     * This is secure in the sense that any attempt to use an external entity
     * will trigger an exception.
     */
    private static class SecureEntityResolver implements EntityResolver2  {

        public InputSource resolveEntity(String publicId, String systemId)
                throws SAXException, IOException {
            throw new SAXException(
                    MessageFormat.format(LogFacade.BLOCK_EXTERNAL_ENTITY, publicId, systemId));
        }

        public InputSource getExternalSubset(String name, String baseURI)
                throws SAXException, IOException {
            throw new SAXException(
                    MessageFormat.format(LogFacade.BLOCK_EXTERNAL_SUBSET, name, baseURI));
        }

        public InputSource resolveEntity(String name, String publicId,
                String baseURI, String systemId) throws SAXException,
                IOException {
            throw new SAXException(
                    MessageFormat.format(LogFacade.BLOCK_EXTERNAL_ENTITY2,
                    name, publicId, baseURI, systemId));
        }
    }
}

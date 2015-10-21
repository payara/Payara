/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.apache.catalina.connector;

import java.nio.charset.Charset;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sun.appserv.ProxyHandler;
import java.io.CharConversionException;
import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Globals;
import org.apache.catalina.Host;
import org.apache.catalina.Wrapper;
import org.apache.catalina.core.ContainerBase;
import org.apache.catalina.core.StandardServer;
import org.apache.catalina.util.ServerInfo;
import org.apache.catalina.util.StringManager;
import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.http.server.AfterServiceListener;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.Note;
import org.glassfish.grizzly.http.server.util.MappingData;
import org.glassfish.grizzly.http.util.ByteChunk;
import org.glassfish.grizzly.http.util.CharChunk;
import org.glassfish.grizzly.http.util.DataChunk;
import org.glassfish.grizzly.http.util.MessageBytes;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.logging.annotation.LogMessageInfo;
import org.glassfish.web.valve.GlassFishValve;
import org.glassfish.web.valve.ServletContainerInterceptor;

/**
 * Implementation of a request processor which delegates the processing to a
 * Coyote processor.
 *
 * @author Craig R. McClanahan
 * @author Remy Maucherat
 * @version $Revision: 1.34 $ $Date: 2007/08/24 18:38:28 $
 */

public class CoyoteAdapter extends HttpHandler {

    private static final Logger log = StandardServer.log;
    private static final ResourceBundle rb = log.getResourceBundle();

    @LogMessageInfo(
            message = "An exception or error occurred in the container during the request processing",
            level = "SEVERE",
            cause = "Could not process the request in the container",
            action = "Verify certificate chain retrieved from the request header and the correctness of request"
    )
    public static final String REQUEST_PROCESSING_EXCEPTION = "AS-WEB-CORE-00037";

    @LogMessageInfo(
            message = "HTTP listener on port {0} has been disabled",
            level = "FINE"
    )
    public static final String HTTP_LISTENER_DISABLED = "AS-WEB-CORE-00038";

    @LogMessageInfo(
            message = "Error parsing client cert chain into array of java.security.cert.X509Certificate instances",
            level = "SEVERE",
            cause = "Could not get the SSL client certificate chain",
            action = "Verify certificate chain and the request"
    )
    public static final String PARSING_CLIENT_CERT_EXCEPTION = "AS-WEB-CORE-00039";

    @LogMessageInfo(
            message = "No Host matches server name {0}",
            level = "INFO"
    )
    public static final String NO_HOST_MATCHES_SERVER_NAME_INFO = "AS-WEB-CORE-00040";

    @LogMessageInfo(
            message = "Internal Error",
            level = "SEVERE",
            cause = "Error during invoke the servlet application",
            action = "Trying to invoke the servlet application"
    )
    public static final String INTERNAL_ERROR = "AS-WEB-CORE-00493";
    
    @LogMessageInfo(
            message = "Failed to initialize the interceptor",
            level = "SEVERE",
            cause = "Error in initializing the servlet application",
            action = "initialize the servlet interceptor"
    )
    public static final String FAILED_TO_INITIALIZE_THE_INTERCEPTOR = "AS-WEB-CORE-00494";
    
    // -------------------------------------------------------------- Constants
    private static final String POWERED_BY = "Servlet/3.1 JSP/2.3 " +
            "(" + ServerInfo.getServerInfo() + " Java/" +
            System.getProperty("java.vm.vendor") + "/" +
            System.getProperty("java.specification.version") + ")";


//    protected boolean v3Enabled =
//        Boolean.valueOf(System.getProperty("v3.grizzly.useMapper", "true"));
    
    
//    public static final int ADAPTER_NOTES = 1;

    static final String JVM_ROUTE = System.getProperty("jvmRoute");

    private Collection<ServletContainerInterceptor> interceptors = null;

    protected static final boolean ALLOW_BACKSLASH =
        Boolean.valueOf(System.getProperty("org.glassfish.grizzly.tcp.tomcat5.CoyoteAdapter.ALLOW_BACKSLASH", "false"));

    private static final boolean COLLAPSE_ADJACENT_SLASHES =
        Boolean.valueOf(System.getProperty(
            "com.sun.enterprise.web.collapseAdjacentSlashes", "true"));

    /**
     * When mod_jk is used, the adapter must be invoked the same way 
     * Tomcat does by invoking service(...) and the afterService(...). This
     * is a hack to make it compatible with Tomcat 5|6.
     */
    private boolean compatWithTomcat = false;
    
    private String serverName = ServerInfo.getPublicServerInfo();
    
    // Make sure this value is always aligned with {@link ContainerMapper}
    // (@see com.sun.enterprise.v3.service.impl.ContainerMapper)
    protected final static Note<MappingData> MAPPING_DATA =
            org.glassfish.grizzly.http.server.Request.<MappingData>createNote("MappingData");

    static final Note<Request> CATALINA_REQUEST_NOTE =
            org.glassfish.grizzly.http.server.Request.createNote(Request.class.getName());
    static final Note<Response> CATALINA_RESPONSE_NOTE =
            org.glassfish.grizzly.http.server.Request.createNote(Response.class.getName());

    static final CatalinaAfterServiceListener catalinaAfterServiceListener =
            new CatalinaAfterServiceListener();

    // Make sure this value is always aligned with {@link ContainerMapper}
    // (@see com.sun.enterprise.v3.service.impl.ContainerMapper)
    private final static Note<DataChunk> DATA_CHUNK =
            org.glassfish.grizzly.http.server.Request.<DataChunk>createNote("DataChunk");
    
    // ----------------------------------------------------------- Constructors


    /**
     * Construct a new CoyoteProcessor associated with the specified connector.
     *
     * @param connector CoyoteConnector that owns this processor
     */
    public CoyoteAdapter(Connector connector) {
        super();
        this.connector = connector;
        initServletInterceptors();
    }


    // ----------------------------------------------------- Instance Variables


    /**
     * The CoyoteConnector with which this processor is associated.
     */
    private Connector connector = null;


    // -------------------------------------------------------- Adapter Methods


    /**
     * Service method.
     */
    @Override
    public void service(org.glassfish.grizzly.http.server.Request req,
                        org.glassfish.grizzly.http.server.Response res)
            throws Exception {

        res.getResponse().setAllowCustomReasonPhrase(Constants.USE_CUSTOM_STATUS_MSG_IN_HEADER);

        Request request = req.getNote(CATALINA_REQUEST_NOTE);
        Response response = req.getNote(CATALINA_RESPONSE_NOTE);
        
        // Grizzly already parsed, decoded, and mapped the request.
        // Let's re-use this info here, before firing the
        // requestStartEvent probe, so that the mapping data will be
        // available to any probe event listener via standard
        // ServletRequest APIs (such as getContextPath())
        MappingData md = req.getNote(MAPPING_DATA);
        final boolean v3Enabled = md != null;
        if (request == null) {

            // Create objects
            request = (Request) connector.createRequest();
            response = (Response) connector.createResponse();

            // Link objects
            request.setResponse(response);
            response.setRequest(request);

            // Set as notes
            req.setNote(CATALINA_REQUEST_NOTE, request);
            req.setNote(CATALINA_RESPONSE_NOTE, response);
//            res.setNote(ADAPTER_NOTES, response);

            // Set query string encoding
            req.getRequest().getRequestURIRef().setDefaultURIEncoding(Charset.forName(connector.getURIEncoding()));
        }

        request.setCoyoteRequest(req);
        response.setCoyoteResponse(res);

        if (v3Enabled && !compatWithTomcat) {
            request.setMappingData(md);
            request.updatePaths(md);
        }

        req.addAfterServiceListener(catalinaAfterServiceListener);
        
        try {
            doService(req, request, res, response, v3Enabled);

            // Request may want to initialize async processing
            request.onExitService();
        } catch (Throwable t) {
            log.log(Level.SEVERE, REQUEST_PROCESSING_EXCEPTION, t);
        }
    }

    private void enteringServletContainer(Request req, Response res) {
        if (interceptors == null)
            return;
        for(ServletContainerInterceptor interceptor:interceptors) {
            try{
                interceptor.preInvoke(req, res);
            } catch (Throwable th) {
                log.log(Level.SEVERE, INTERNAL_ERROR, th);
            }
        }
    }

    private void leavingServletContainer(Request req, Response res) {
        if (interceptors == null)
            return;
        for(ServletContainerInterceptor interceptor:interceptors) {
            try{
                interceptor.postInvoke(req, res);
            } catch (Throwable th) {
                log.log(Level.SEVERE, INTERNAL_ERROR, th);
            }
        }
    }

    private void initServletInterceptors() {
        try {
            ServiceLocator services = org.glassfish.internal.api.Globals.getDefaultHabitat();
            interceptors = services.getAllServices(ServletContainerInterceptor.class);
        } catch (Throwable th) {
            log.log(Level.SEVERE, FAILED_TO_INITIALIZE_THE_INTERCEPTOR, th);
        }
    }
    

    private void doService(final org.glassfish.grizzly.http.server.Request req,
                           final Request request,
                           final org.glassfish.grizzly.http.server.Response res,
                           final Response response,
                           final boolean v3Enabled)
            throws Exception {
        
        // START SJSAS 6331392
        // Check connector for disabled state
        if (!connector.isEnabled()) {
            String msg = MessageFormat.format(rb.getString(HTTP_LISTENER_DISABLED),
                                              String.valueOf(connector.getPort()));
            if (log.isLoggable(Level.FINE)) {
                log.log(Level.FINE, msg);
            }
            response.sendError(HttpServletResponse.SC_NOT_FOUND, msg);
            return;
        }
        // END SJSAS 6331392

////            "X-Powered-By" header is set by GlassfishHttpCodecFilter
//        if (connector.isXpoweredBy()) {
//            response.addHeader("X-Powered-By", POWERED_BY);
//        }


        // Parse and set Catalina and configuration specific 
        // request parameters
        if ( postParseRequest(req, request, res, response, v3Enabled) ) {

            // START S1AS 6188932
            boolean authPassthroughEnabled = 
                connector.getAuthPassthroughEnabled();
            ProxyHandler proxyHandler = connector.getProxyHandler();
            if (authPassthroughEnabled && proxyHandler != null) {

                // START SJSAS 6397218
                if (proxyHandler.getSSLKeysize(
                        (HttpServletRequest)request.getRequest()) > 0) {
                    request.setSecure(true);
                }
                // END SJSAS 6397218

                X509Certificate[] certs = null;
                try {
                    certs = proxyHandler.getSSLClientCertificateChain(
                                request.getRequest());
                } catch (CertificateException ce) {
                    log.log(Level.SEVERE, PARSING_CLIENT_CERT_EXCEPTION,
                            ce);
                }
                if (certs != null) {
                    request.setAttribute(Globals.CERTIFICATES_ATTR,
                                         certs);
                }
                    
            }
            // END S1AS 6188932

////            "Server" header is set by GlassfishHttpCodecFilter
//            if (serverName != null && !serverName.isEmpty()) {
//                response.addHeader("Server", serverName);
//            }
                
            // Invoke the web container
            connector.requestStartEvent(request.getRequest(),
                request.getHost(), request.getContext());
            Container container = connector.getContainer();
            enteringServletContainer(request, response);
            try {
                request.lockSession();
                if (container.getPipeline().hasNonBasicValves() ||
                        container.hasCustomPipeline()) {
                    container.getPipeline().invoke(request, response);
                } else {
                    // Invoke host directly
                    Host host = request.getHost();
                    if (host == null) {
                        response.sendError(HttpServletResponse.SC_BAD_REQUEST);

                        String msg = MessageFormat.format(rb.getString(NO_HOST_MATCHES_SERVER_NAME_INFO),
                                                          request.getRequest().getServerName());
                        response.setDetailMessage(msg);
                        return;
                    }
                    if (host.getPipeline().hasNonBasicValves() ||
                            host.hasCustomPipeline()) {
                        host.getPipeline().invoke(request, response);
                    } else {
                        GlassFishValve hostValve = host.getPipeline().getBasic();
                        hostValve.invoke(request, response);
                        // Error handling
                        hostValve.postInvoke(request, response); 
                    }
                }
            } finally {
                try {
                    connector.requestEndEvent(request.getRequest(),
                        request.getHost(), request.getContext(),
                        response.getStatus());
                } finally {
                    leavingServletContainer(request, response);
                }
            }
        }

    }
    // ------------------------------------------------------ Protected Methods


    /**
     * Parse additional request parameters.
     */
    protected boolean postParseRequest(final org.glassfish.grizzly.http.server.Request req,
                                       final Request request,
                                       final org.glassfish.grizzly.http.server.Response res,
                                       final Response response,
                                       final boolean v3Enabled)
        throws Exception {
        // XXX the processor may have set a correct scheme and port prior to this point, 
        // in ajp13 protocols dont make sense to get the port from the connector...
        // otherwise, use connector configuration
        request.setSecure(req.isSecure());

        // URI decoding
        DataChunk decodedURI;
        try {
            decodedURI = req.getRequest().getRequestURIRef().getDecodedRequestURIBC();
        } catch (CharConversionException cce) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid URI");
            return false;
        }
        
        if (compatWithTomcat || !v3Enabled) {
//            decodedURI.duplicate(req.requestURI());
//            try {
//              req.getURLDecoder().convert(decodedURI, false);
//            } catch (IOException ioe) {
//              res.setStatus(400);
//              res.setMessage("Invalid URI: " + ioe.getMessage());
//              return false;
//            }

            /* GlassFish Issue 2339
            // Normalize decoded URI
            if (!normalize(req.decodedURI())) {
                res.setStatus(400);
                res.setMessage("Invalid URI");
                return false;
            }
            */

            // Set the remote principal
            String principal = req.getRemoteUser();
            if (principal != null) {
                request.setUserPrincipal(new CoyotePrincipal(principal));
            }

            // Set the authorization type
            String authtype = req.getAuthType();
            if (authtype != null) {
                request.setAuthType(authtype);
            }

            /* CR 6309511
            // URI character decoding
            convertURI(decodedURI, request);

            // Parse session Id
            parseSessionId(req, request);
             */
            // START CR 6309511
//             URI character decoding
//            request.convertURI(decodedURI);

            // START GlassFish Issue 2339
            // Normalize decoded URI
//            if (!normalize(decodedURI)) {
//                res.setStatus(400);
//                res.setMessage("Invalid URI");
//                return false;
//            }
            // END GlassFish Issue 2339
        }
        // END CR 6309511

        /*
         * Remove any parameters from the URI, so they won't be considered
         * by the mapping algorithm, and save them in a temporary CharChunk,
         * so that any session id param may be parsed once the target
         * context, which may use a custom session parameter name, has been
         * identified
         */
        final CharChunk uriParamsCC = request.getURIParams();
        final CharChunk uriCC = decodedURI.getCharChunk();
        final int semicolon = uriCC.indexOf(';');
        if (semicolon > 0) {
            final int absSemicolon = uriCC.getStart() + semicolon;
            uriParamsCC.setChars(uriCC.getBuffer(), absSemicolon,
                uriCC.getEnd() - absSemicolon);
            decodedURI.setChars(uriCC.getBuffer(), uriCC.getStart(),
                absSemicolon - uriCC.getStart());
        }
 
        if (compatWithTomcat || !v3Enabled) {
            /*mod_jk*/
            DataChunk localDecodedURI = decodedURI;
            if (semicolon > 0) {
                localDecodedURI = req.getNote(DATA_CHUNK);
                if (localDecodedURI == null) {
                    localDecodedURI = DataChunk.newInstance();
                    req.setNote(DATA_CHUNK, localDecodedURI);
                }
                localDecodedURI.duplicate(decodedURI);
            }
            connector.getMapper().map(req.getRequest().serverName(), localDecodedURI,
                                  request.getMappingData());
            MappingData md = request.getMappingData();
            req.setNote(MAPPING_DATA, md);
            request.updatePaths(md);
        }

        // FIXME: the code below doesnt belongs to here,
        // this is only have sense
        // in Http11, not in ajp13..
        // At this point the Host header has been processed.
        // Override if the proxyPort/proxyHost are set
        String proxyName = connector.getProxyName();
        int proxyPort = connector.getProxyPort();
        if (proxyPort != 0) {
            req.setServerPort(proxyPort);
        }
        if (proxyName != null) {
            req.setServerName(proxyName);
        }

        Context ctx = (Context) request.getMappingData().context;

        // Parse session id
        if (ctx != null) {
            if (req.isRequestedSessionIdFromURL() &&
                    Globals.SESSION_PARAMETER_NAME.equals(ctx.getSessionParameterName())) {
                request.obtainSessionId();
            } else if (!uriParamsCC.isNull()) {
//            String sessionParam = ";" + ctx.getSessionParameterName() + "=";
                request.parseSessionId(ctx.getSessionParameterName(), uriParamsCC);
            }
        }

        // START GlassFish 1024
        request.setDefaultContext(request.getMappingData().isDefaultContext);
        // END GlassFish 1024

        // START SJSAS 6253524
        // request.setContext((Context) request.getMappingData().context);
        // END SJSAS 6253524
        // START SJSAS 6253524
        request.setContext(ctx);
        // END SJSAS 6253524

        if (ctx != null && !uriParamsCC.isNull()) {
            request.parseSessionVersion(uriParamsCC);
        }

        if (!uriParamsCC.isNull()) {
            request.parseJReplica(uriParamsCC);
        }

        request.setWrapper((Wrapper) request.getMappingData().wrapper);

        // Filter trace method
        if (!connector.getAllowTrace() && Method.TRACE.equals(req.getMethod())) {
            Wrapper wrapper = request.getWrapper();
            String header = null;
            if (wrapper != null) {
                String[] methods = wrapper.getServletMethods();
                if (methods != null) {
                    for (String method : methods) {
                        // Exclude TRACE from methods returned in Allow header
                        if ("TRACE".equals(method)) {
                            continue;
                        }
                        if (header == null) {
                            header = method;
                        } else {
                            header += ", " + method;
                        }
                    }
                }
            }                               
            res.setStatus(405, "TRACE method is not allowed");
            res.addHeader("Allow", header);
            return false;
        }

        // Possible redirect
        DataChunk redirectPathMB = request.getMappingData().redirectPath;
        // START SJSAS 6253524
        // if (!redirectPathMB.isNull()) {
        // END SJSAS 6253524
        // START SJSAS 6253524
        if (!redirectPathMB.isNull()
            && (!ctx.hasAdHocPaths()
                || (ctx.getAdHocServletName(((HttpServletRequest)
                        request.getRequest()).getServletPath()) == null))) {
        // END SJSAS 6253524
            String redirectPath = redirectPathMB.toString();
            String query = request.getQueryString();
            if (request.isRequestedSessionIdFromURL()) {
                // This is not optimal, but as this is not very common, it
                // shouldn't matter
                redirectPath = redirectPath + ";" + ctx.getSessionParameterName() + "=" 
                    + request.getRequestedSessionId();
            }            
            // START GlassFish 936
            redirectPath = response.encode(redirectPath);
            // END GlassFish 936
            if (query != null) {
                // This is not optimal, but as this is not very common, it
                // shouldn't matter
                redirectPath = redirectPath + "?" + query;
            }

            // START CR 6590921
            boolean authPassthroughEnabled = 
                connector.getAuthPassthroughEnabled();
            ProxyHandler proxyHandler = connector.getProxyHandler();
            if (authPassthroughEnabled && proxyHandler != null) {

                if (proxyHandler.getSSLKeysize(
                        (HttpServletRequest)request.getRequest()) > 0) {
                    request.setSecure(true);
                }
            }
            // END CR 6590921
            // Issue a permanent redirect
            response.sendRedirect(redirectPath, false);

            return false;
        }

        // Parse session Id
        /* CR 6309511
        parseSessionCookiesId(req, request);
         */
        // START CR 6309511
        request.parseSessionCookiesId();
        // END CR 6309511

        // START SJSAS 6346226
        request.parseJrouteCookie();
        // END SJSAS 6346226

        return true;
    }


    /**
     * Normalize URI.
     * <p>
     * This method normalizes "\", "//", "/./" and "/../". This method will
     * return false when trying to go above the root, or if the URI contains
     * a null byte.
     * 
     * @param uriMB URI to be normalized
     */
    public static boolean normalize(MessageBytes uriMB) {

        int type = uriMB.getType();
        if (type == MessageBytes.T_CHARS) {
            return normalizeChars(uriMB);
        } else {
            return normalizeBytes(uriMB);
        }
    }


    private static boolean normalizeBytes(MessageBytes uriMB) {

        ByteChunk uriBC = uriMB.getByteChunk();
        byte[] b = uriBC.getBytes();
        int start = uriBC.getStart();
        int end = uriBC.getEnd();

        // An empty URL is not acceptable
        if (start == end)
            return false;

        // URL * is acceptable
        if ((end - start == 1) && b[start] == (byte) '*')
          return true;

        int pos = 0;
        int index = 0;

        // Replace '\' with '/'
        // Check for null byte
        for (pos = start; pos < end; pos++) {
            if (b[pos] == (byte) '\\') {
                if (ALLOW_BACKSLASH) {
                    b[pos] = (byte) '/';
                } else {
                    return false;
                }
            }
            if (b[pos] == (byte) 0) {
                return false;
            }
        }

        // The URL must start with '/'
        if (b[start] != (byte) '/') {
            return false;
        }

        // Replace "//" with "/"
        if (COLLAPSE_ADJACENT_SLASHES) {
            for (pos = start; pos < (end - 1); pos++) {
                if (b[pos] == (byte) '/') {
                    while ((pos + 1 < end) && (b[pos + 1] == (byte) '/')) {
                        copyBytes(b, pos, pos + 1, end - pos - 1);
                        end--;
                    }
                }
            }
        }

        // If the URI ends with "/." or "/..", then we append an extra "/"
        // Note: It is possible to extend the URI by 1 without any side effect
        // as the next character is a non-significant WS.
        if (((end - start) > 2) && (b[end - 1] == (byte) '.')) {
            if ((b[end - 2] == (byte) '/') 
                || ((b[end - 2] == (byte) '.') 
                    && (b[end - 3] == (byte) '/'))) {
                b[end] = (byte) '/';
                end++;
            }
        }

        uriBC.setEnd(end);

        index = 0;

        // Resolve occurrences of "/./" in the normalized path
        while (true) {
            index = uriBC.indexOf("/./", 0, 3, index);
            if (index < 0)
                break;
            copyBytes(b, start + index, start + index + 2, 
                      end - start - index - 2);
            end = end - 2;
            uriBC.setEnd(end);
        }

        index = 0;

        // Resolve occurrences of "/../" in the normalized path
        while (true) {
            index = uriBC.indexOf("/../", 0, 4, index);
            if (index < 0)
                break;
            // Prevent from going outside our context
            if (index == 0)
                return false;
            int index2 = -1;
            for (pos = start + index - 1; (pos >= 0) && (index2 < 0); pos --) {
                if (b[pos] == (byte) '/') {
                    index2 = pos;
                }
            }
            copyBytes(b, start + index2, start + index + 3,
                      end - start - index - 3);
            end = end + index2 - index - 3;
            uriBC.setEnd(end);
            index = index2;
        }

        uriBC.setBytes(b, start, end);

        return true;

    }


    private static boolean normalizeChars(MessageBytes uriMB) {

        CharChunk uriCC = uriMB.getCharChunk();
        char[] c = uriCC.getChars();
        int start = uriCC.getStart();
        int end = uriCC.getEnd();

        // URL * is acceptable
        if ((end - start == 1) && c[start] == (char) '*')
          return true;

        int pos = 0;
        int index = 0;

        // Replace '\' with '/'
        // Check for null char
        for (pos = start; pos < end; pos++) {
            if (c[pos] == (char) '\\') {
                if (ALLOW_BACKSLASH) {
                    c[pos] = (char) '/';
                } else {
                    return false;
                }
            }
            if (c[pos] == (char) 0) {
                return false;
            }
        }

        // The URL must start with '/'
        if (c[start] != (char) '/') {
            return false;
        }

        // Replace "//" with "/"
        if (COLLAPSE_ADJACENT_SLASHES) {
            for (pos = start; pos < (end - 1); pos++) {
                if (c[pos] == (char) '/') {
                    while ((pos + 1 < end) && (c[pos + 1] == (char) '/')) {
                        copyChars(c, pos, pos + 1, end - pos - 1);
                        end--;
                    }
                }
            }
        }	

        // If the URI ends with "/." or "/..", then we append an extra "/"
        // Note: It is possible to extend the URI by 1 without any side effect
        // as the next character is a non-significant WS.
        if (((end - start) > 2) && (c[end - 1] == (char) '.')) {
            if ((c[end - 2] == (char) '/') 
                || ((c[end - 2] == (char) '.') 
                    && (c[end - 3] == (char) '/'))) {
                c[end] = (char) '/';
                end++;
            }
        }

        uriCC.setEnd(end);

        index = 0;

        // Resolve occurrences of "/./" in the normalized path
        while (true) {
            index = uriCC.indexOf("/./", 0, 3, index);
            if (index < 0)
                break;
            copyChars(c, start + index, start + index + 2, 
                      end - start - index - 2);
            end = end - 2;
            uriCC.setEnd(end);
        }

        index = 0;

        // Resolve occurrences of "/../" in the normalized path
        while (true) {
            index = uriCC.indexOf("/../", 0, 4, index);
            if (index < 0)
                break;
            // Prevent from going outside our context
            if (index == 0)
                return false;
            int index2 = -1;
            for (pos = start + index - 1; (pos >= 0) && (index2 < 0); pos --) {
                if (c[pos] == (char) '/') {
                    index2 = pos;
                }
            }
            copyChars(c, start + index2, start + index + 3,
                      end - start - index - 3);
            end = end + index2 - index - 3;
            uriCC.setEnd(end);
            index = index2;
        }

        uriCC.setChars(c, start, end);

        return true;

    }


    // ------------------------------------------------------ Protected Methods


    /**
     * Copy an array of bytes to a different position. Used during 
     * normalization.
     */
    protected static void copyBytes(byte[] b, int dest, int src, int len) {
        for (int pos = 0; pos < len; pos++) {
            b[pos + dest] = b[pos + src];
        }
    }


    /**
     * Copy an array of chars to a different position. Used during 
     * normalization.
     */
    private static void copyChars(char[] c, int dest, int src, int len) {
        for (int pos = 0; pos < len; pos++) {
            c[pos + dest] = c[pos + src];
        }
    }


    /**
     * Log a message on the Logger associated with our Container (if any)
     *
     * @param message Message to be logged
     */
    protected void log(String message) {
        log.log(Level.INFO, message);
    }


    /**
     * Log a message on the Logger associated with our Container (if any)
     *
     * @param message Message to be logged
     * @param throwable Associated exception
     */
    protected void log(String message, Throwable throwable) {
        log.log(Level.SEVERE, message, throwable);
    }


     /**
      * Character conversion of the a US-ASCII MessageBytes.
      */
    /* CR 6309511
     protected void convertMB(MessageBytes mb) {
 
        // This is of course only meaningful for bytes
        if (mb.getType() != MessageBytes.T_BYTES)
            return;
        
        ByteChunk bc = mb.getByteChunk();
        CharChunk cc = mb.getCharChunk();
        cc.allocate(bc.getLength(), -1);

        // Default encoding: fast conversion
        byte[] bbuf = bc.getBuffer();
        char[] cbuf = cc.getBuffer();
        int start = bc.getStart();
        for (int i = 0; i < bc.getLength(); i++) {
            cbuf[i] = (char) (bbuf[i + start] & 0xff);
        }
        mb.setChars(cbuf, 0, bc.getLength());
   
     }
     */

    
    // START SJSAS 6349248
    /**
     * Notify all container event listeners that a particular event has
     * occurred for this Adapter.  The default implementation performs
     * this notification synchronously using the calling thread.
     *
     * @param type Event type
     * @param data Event data
     */
    public void fireAdapterEvent(String type, Object data) {
        if ( connector != null && connector.getContainer() != null) {
            try{
                ((ContainerBase)connector.getContainer())
                    .fireContainerEvent(type,data);
            } catch (Throwable t){
                log.log(Level.SEVERE, REQUEST_PROCESSING_EXCEPTION, t);
            }
        }
    }
    // END SJSAS 6349248

    
    /**
     * Return true when an instance is executed the same way it does in Tomcat.
     */
    public boolean isCompatWithTomcat() {
        return compatWithTomcat;
    }

    
    /**
     * <tt>true</tt> if this class needs to be compatible with Tomcat
     * Adapter class. Since Tomcat Adapter implementation doesn't support 
     * the afterService method, the afterService method must be invoked 
     * inside the service method.
     */
    public void setCompatWithTomcat(boolean compatWithTomcat) {
        this.compatWithTomcat = compatWithTomcat;

        // Add server header
        if (compatWithTomcat){
            serverName = "Apache/" + serverName;
        } else {
            // Recalculate.
            serverName = ServerInfo.getPublicServerInfo();
        }
    }


    /**
     * Gets the port of this CoyoteAdapter.
     *
     * @return the port of this CoyoteAdapter
     */
    public int getPort() {
        return connector.getPort();
    }

    /**
     * AfterServiceListener, which is responsible for recycle catalina request and response
     * objects.
     */
    static final class CatalinaAfterServiceListener implements AfterServiceListener {

        @Override
        public void onAfterService(final org.glassfish.grizzly.http.server.Request request) {
            final Request servletRequest = request.getNote(CATALINA_REQUEST_NOTE);
            final Response servletResponse = request.getNote(CATALINA_RESPONSE_NOTE);

            if (servletRequest != null) {
                try {
                    if (!servletRequest.isUpgrade()) {
                        servletResponse.finishResponse();
                    } else {
                        servletResponse.setUpgrade(servletRequest.isUpgrade());
                    }
                } catch (Exception e) {
                    log.log(Level.SEVERE, REQUEST_PROCESSING_EXCEPTION, e);
                } finally {
                    try {
                        servletRequest.unlockSession();
                    } finally {
                        servletRequest.recycle();
                        servletResponse.recycle();
                    }
                }
            }
        }
    }
}

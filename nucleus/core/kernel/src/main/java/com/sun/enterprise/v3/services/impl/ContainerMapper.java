/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007-2013 Oracle and/or its affiliates. All rights reserved.
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
 */
package com.sun.enterprise.v3.services.impl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.appserv.server.util.Version;
import org.glassfish.api.container.Adapter;
import org.glassfish.api.container.Sniffer;
import org.glassfish.api.deployment.ApplicationContainer;
import org.glassfish.grizzly.config.ContextRootInfo;
import org.glassfish.grizzly.config.GrizzlyListener;
import org.glassfish.grizzly.http.server.AfterServiceListener;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpHandlerChain;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.Note;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.server.StaticHttpHandler;
import org.glassfish.grizzly.http.server.util.Mapper;
import org.glassfish.grizzly.http.server.util.MappingData;
import org.glassfish.grizzly.http.util.ByteChunk;
import org.glassfish.grizzly.http.util.CharChunk;
import org.glassfish.grizzly.http.util.DataChunk;
import org.glassfish.grizzly.http.util.MimeType;
import org.glassfish.internal.grizzly.V3Mapper;

/**
 * Container's mapper which maps {@link ByteBuffer} bytes representation to an  {@link HttpHandler}, {@link
 * ApplicationContainer} and ProtocolFilter chain. The mapping result is stored inside {@link MappingData} which
 * is eventually shared with the CoyoteAdapter, which is the entry point with the Catalina Servlet Container.
 *
 * @author Jeanfrancois Arcand
 * @author Alexey Stashok
 */
@SuppressWarnings({"NonPrivateFieldAccessedInSynchronizedContext"})
public class ContainerMapper extends StaticHttpHandler {

    private static final Logger LOGGER = Logger.getLogger(ContainerMapper.class.getName());
    private final static String ROOT = "";
    private Mapper mapper;
    private final GrizzlyListener listener;
    private String defaultHostName = "server";
//    private final UDecoder urlDecoder;
    private final GrizzlyService grizzlyService;
    protected final static Note<MappingData> MAPPING_DATA =
            Request.<MappingData>createNote("MappingData");
    // Make sure this value is always aligned with {@link org.apache.catalina.connector.CoyoteAdapter}
    // (@see org.apache.catalina.connector.CoyoteAdapter)
    private final static Note<DataChunk> DATA_CHUNK =
            Request.<DataChunk>createNote("DataChunk");

    private String version;
    private static final AfterServiceListener afterServiceListener =
            new AfterServiceListenerImpl();
    /**
     * Are we running multiple {@ Adapter} or {@link HttpHandlerChain}
     */
    private boolean mapMultipleAdapter;

    public ContainerMapper(final GrizzlyService service,
            final GrizzlyListener grizzlyListener) {
        listener = grizzlyListener;
//        urlDecoder = embeddedHttp.getUrlDecoder();
        grizzlyService = service;

        version = System.getProperty("product.name");
        if (version == null) {
            version = Version.getVersion();
        }
    }

    /**
     * Set the default host that will be used when we map.
     *
     * @param defaultHost
     */
    protected synchronized void setDefaultHost(String defaultHost) {
        defaultHostName = defaultHost;
    }

    /**
     * Set the {@link V3Mapper} instance used for mapping the container and its associated {@link Adapter}.
     *
     * @param mapper
     */
    protected void setMapper(Mapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Configure the {@link V3Mapper}.
     */
    protected synchronized void configureMapper() {
        mapper.setDefaultHostName(defaultHostName);
        mapper.addHost(defaultHostName, new String[]{}, null);
        mapper.addContext(defaultHostName, ROOT,
                new ContextRootInfo(this, null),
                new String[]{"index.html", "index.htm"}, null);
        // Container deployed have the right to override the default setting.
        Mapper.setAllowReplacement(true);
    }
    
//    private static void dispatch(HttpHandler adapter,
//            ClassLoader cl,
//            Request req,
//            Response res) throws Exception {
//        ClassLoader currentCL = Thread.currentThread().getContextClassLoader();
//        try {
//            if (cl==null) {
//                cl = adapter.getClass().getClassLoader();
//            }
//            Thread.currentThread().setContextClassLoader(cl);
//
//            adapter.service(req, res);
//        }
//        finally {
//            Thread.currentThread().setContextClassLoader(currentCL);
//        }
//    }

    /**
     * Map the request to its associated {@link Adapter}.
     *
     * @param request
     * @param response
     *
     * @throws IOException
     */
    @Override
    public void service(final Request request, final Response response) throws Exception {
        MappingData mappingData;
        try {

            request.addAfterServiceListener(afterServiceListener);

            // If we have only one Adapter deployed, invoke that Adapter
            // directly.
            // TODO: Not sure that will works with JRuby.
            if (!mapMultipleAdapter && mapper instanceof V3Mapper) {
                // Remove the MappingData as we might delegate the request
                // to be serviced directly by the WebContainer
                final HttpHandler httpHandler = ((V3Mapper) mapper).getHttpHandler();
                if (httpHandler != null) {
                    request.setNote(MAPPING_DATA, null);
//                    req.setNote(MAPPED_ADAPTER, a);
                    httpHandler.service(request, response);
                    return;
                }
            }

            final DataChunk decodedURI = request.getRequest()
                    .getRequestURIRef().getDecodedRequestURIBC(isAllowEncodedSlash());

            mappingData = request.getNote(MAPPING_DATA);
            if (mappingData == null) {
                mappingData = new MappingData();
                request.setNote(MAPPING_DATA, mappingData);
            } else {
                mappingData.recycle();
            }
            HttpHandler httpService;

            final CharChunk decodedURICC = decodedURI.getCharChunk();
            final int semicolon = decodedURICC.indexOf(';', 0);

            // Map the request without any trailling.
            httpService = mapUriWithSemicolon(request, decodedURI, semicolon, mappingData);
            if (httpService == null || httpService instanceof ContainerMapper) {
                String ext = decodedURI.toString();
                String type = "";
                if (ext.lastIndexOf(".") > 0) {
                    ext = "*" + ext.substring(ext.lastIndexOf("."));
                    type = ext.substring(ext.lastIndexOf(".") + 1);
                }

                if (!MimeType.contains(type) && !"/".equals(ext)) {
                    initializeFileURLPattern(ext);
                    mappingData.recycle();
                    httpService = mapUriWithSemicolon(request, decodedURI, semicolon, mappingData);
                } else {
                    super.service(request, response);
                    return;
                }
            }

            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Request: {0} was mapped to Adapter: {1}",
                        new Object[]{decodedURI.toString(), httpService});
            }

            // The Adapter used for servicing static pages doesn't decode the
            // request by default, hence do not pass the undecoded request.
            if (httpService == null || httpService instanceof ContainerMapper) {
                super.service(request, response);
            } else {
//                req.setNote(MAPPED_ADAPTER, adapter);

//                ContextRootInfo contextRootInfo = null;
//                if (mappingData.context != null && mappingData.context instanceof ContextRootInfo) {
//                    contextRootInfo = (ContextRootInfo) mappingData.context;
//                }

                //if (contextRootInfo == null) {
                    httpService.service(request, response);
//                } else {
//                    ClassLoader cl = null;
//                    if (contextRootInfo.getContainer() instanceof ApplicationContainer) {
//                        cl = ((ApplicationContainer) contextRootInfo.getContainer()).getClassLoader();
//                    }
//
//                    dispatch(httpService, cl, request, response);
//                }
            }
        } catch (Exception ex) {
            try {
                response.setStatus(500);
                if (LOGGER.isLoggable(Level.WARNING)) {
                    LOGGER.log(Level.WARNING, "Internal Server error: "
                            + request.getRequest().getRequestURIRef().getDecodedRequestURIBC(), ex);
                }
                customizedErrorPage(request, response);
            } catch (Exception ex2) {
                if (LOGGER.isLoggable(Level.WARNING)) {
                    LOGGER.log(Level.WARNING, "Unable to error page", ex2);
                }
            }
        }
    }

    public synchronized void initializeFileURLPattern(String ext) {
        for (Sniffer sniffer : grizzlyService.getHabitat().<Sniffer>getAllServices(Sniffer.class)) {
            boolean match = false;
            if (sniffer.getURLPatterns() != null) {

                for (String pattern : sniffer.getURLPatterns()) {
                    if (pattern.equalsIgnoreCase(ext)) {
                        match = true;
                        break;
                    }
                }

                HttpHandler adapter;
                if (match) {
                    adapter = grizzlyService.getHabitat().getService(SnifferAdapter.class);
                    ((SnifferAdapter) adapter).initialize(sniffer, this);
                    ContextRootInfo c = new ContextRootInfo(adapter, null);

                    for (String pattern : sniffer.getURLPatterns()) {
                        for (String host : grizzlyService.hosts) {
                            mapper.addWrapper(host, ROOT, pattern, c,
                                    "*.jsp".equals(pattern) || "*.jspx".equals(pattern));
                        }
                    }
                    return;
                }
            }
        }
    }

    /**
     * Maps the decodedURI to the corresponding Adapter, considering that URI
     * may have a semicolon with extra data followed, which shouldn't be a part
     * of mapping process.
     *
     * @param req HTTP request
     * @param decodedURI URI
     * @param semicolonPos semicolon position. Might be <tt>0</tt> if position wasn't resolved yet (so it will be resolved in the method), or <tt>-1</tt> if there is no semicolon in the URI.
     * @param mappingData
     * @return
     * @throws Exception
     */
    final HttpHandler mapUriWithSemicolon(final Request req, final DataChunk decodedURI,
            int semicolonPos, final MappingData mappingData) throws Exception {

        final CharChunk charChunk = decodedURI.getCharChunk();
        final int oldStart = charChunk.getStart();
        final int oldEnd = charChunk.getEnd();

        if (semicolonPos == 0) {
            semicolonPos = decodedURI.indexOf(';', 0);
        }

        DataChunk localDecodedURI = decodedURI;
        if (semicolonPos >= 0) {
            charChunk.setEnd(semicolonPos);
            // duplicate the URI path, because Mapper may corrupt the attributes,
            // which follow the path
            localDecodedURI = req.getNote(DATA_CHUNK);
            if (localDecodedURI == null) {
                localDecodedURI = DataChunk.newInstance();
                req.setNote(DATA_CHUNK, localDecodedURI);
            }
            localDecodedURI.duplicate(decodedURI);
        }


        try {
            return map(req, localDecodedURI, mappingData);
        } finally {
            charChunk.setStart(oldStart);
            charChunk.setEnd(oldEnd);
        }
    }

    HttpHandler map(final Request req, final DataChunk decodedURI,
            MappingData mappingData) throws Exception {
        
        if (mappingData == null) {
            mappingData = req.getNote(MAPPING_DATA);
        }
        // Map the request to its Adapter/Container and also it's Servlet if
        // the request is targetted to the CoyoteAdapter.
        mapper.map(req.getRequest().serverName(), decodedURI, mappingData);

        updatePaths(req, mappingData);

        ContextRootInfo contextRootInfo;
        if (mappingData.context != null && (mappingData.context instanceof ContextRootInfo
                || mappingData.wrapper instanceof ContextRootInfo)) {
            if (mappingData.wrapper != null) {
                contextRootInfo = (ContextRootInfo) mappingData.wrapper;
            } else {
                contextRootInfo = (ContextRootInfo) mappingData.context;
            }
            return contextRootInfo.getHttpHandler();
        } else if (mappingData.context != null
                && "com.sun.enterprise.web.WebModule".equals(mappingData.context.getClass().getName())) {
            return ((V3Mapper) mapper).getHttpHandler();
        }
        return null;
    }

    /**
     * Recycle the mapped {@link Adapter} and this instance.
     *
     * @param req
     * @param res
     *
     * @throws Exception
     */
//    @Override
//    public void afterService(Request req, Response res) throws Exception {
//        MappingData mappingData = (MappingData) req.getNote(MAPPING_DATA);
//        try {
//            HttpHandler adapter = (HttpHandler) req.getNote(MAPPED_ADAPTER);
//            if (adapter != null) {
//                adapter.afterService(req, res);
//            }
//            super.afterService(req, res);
//        } finally {
//            req.setNote(MAPPED_ADAPTER, null);
//            if (mappingData != null){
//                mappingData.recycle();
//            }
//        }
//    }
    /**
     * Return an error page customized for GlassFish v3.
     *
     * @param req
     * @param res
     *
     * @throws Exception
     */
    @Override
    protected void customizedErrorPage(Request req, Response res) throws Exception {
        byte[] errorBody;
        if (res.getStatus() == 404) {
            errorBody = HttpUtils.getErrorPage(Version.getVersion(),
                    "The requested resource is not available.", "404");
        } else {
            errorBody = HttpUtils.getErrorPage(Version.getVersion(),
                    "The server encountered an internal error that prevented it from fulfilling this request.", "500");
        }
        ByteChunk chunk = new ByteChunk();
        chunk.setBytes(errorBody, 0, errorBody.length);
        res.setContentLength(errorBody.length);
        res.setContentType("text/html");
        if (!version.isEmpty()) {
            res.addHeader("Server", version);
        }
        res.flush();
        res.getOutputBuffer().write(chunk.getBuffer());
    }

    public void register(String contextRoot, Collection<String> vs, HttpHandler httpService,
            ApplicationContainer container) {

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "MAPPER({0}) REGISTER contextRoot: {1} adapter: {2} container: {3} port: {4}",
                    new Object[]{this, contextRoot, httpService, container, listener.getPort()});
        }
        /*
         * In the case of CoyoteAdapter, return, because the context will
         * have already been registered with the mapper by the connector's
         * MapperListener, in response to a JMX event
         */
//        if ("org.apache.catalina.connector.CoyoteAdapter".equals(httpService.getClass().getName())) {
//            return;
//        }

        mapMultipleAdapter = true;
//        String ctx = getContextPath(contextRoot);
//        String wrapper = getWrapperPath(ctx, contextRoot);
        ContextRootInfo c = new ContextRootInfo(httpService, container);
        for (String host : vs) {
            mapper.addContext(host, contextRoot, c, new String[0], null);
            /*
            if (adapter instanceof StaticResourcesAdapter) {
            mapper.addWrapper(host, ctx, wrapper, c);
            }
             */
        }
    }

    /*
    private String getWrapperPath(String ctx, String mapping) {
    if (mapping.indexOf("*.") > 0) {
    return mapping.substring(mapping.lastIndexOf("/") + 1);
    } else if (!"".equals(ctx)) {
    return mapping.substring(ctx.length());
    } else {
    return mapping;
    }
    }

    private String getContextPath(String mapping) {
    String ctx;
    int slash = mapping.indexOf("/", 1);
    if (slash != -1) {
    ctx = mapping.substring(0, slash);
    } else {
    ctx = mapping;
    }

    if (ctx.startsWith("/*.") ||ctx.startsWith("*.") ) {
    if (ctx.indexOf("/") == ctx.lastIndexOf("/")){
    ctx = "";
    } else {
    ctx = ctx.substring(1);
    }
    }


    if (ctx.startsWith("/*") || ctx.startsWith("*")) {
    ctx = "";
    }

    // Special case for the root context
    if ("/".equals(ctx)) {
    ctx = "";
    }

    return ctx;
    }
     */
    public void unregister(String contextRoot) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "MAPPER ({0}) UNREGISTER contextRoot: {1}",
                    new Object[]{this, contextRoot});
        }
        for (String host : grizzlyService.hosts) {
            mapper.removeContext(host, contextRoot);
        }
    }

    public void register(final Endpoint endpoint) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "MAPPER({0}) REGISTER endpoint: {1}", endpoint);
        }
        /*
         * In the case of CoyoteAdapter, return, because the context will
         * have already been registered with the mapper by the connector's
         * MapperListener, in response to a JMX event
         */
//        if ("org.apache.catalina.connector.CoyoteAdapter".equals(
//                endpoint.getEndpointHandler().getClass().getName())) {
//            return;
//        }

        mapMultipleAdapter = true;
//        String ctx = getContextPath(contextRoot);
//        String wrapper = getWrapperPath(ctx, contextRoot);
        final String contextRoot = endpoint.getContextRoot();
        final Collection<String> vs = endpoint.getVirtualServers();
        
        ContextRootInfo c = new ContextRootInfo(new ContextRootInfo.Holder() {
            @Override
            public HttpHandler getHttpHandler() {
                return endpoint.getEndpointHandler();
            }

            @Override
            public Object getContainer() {
                return endpoint.getContainer();
            }
        });
        
        for (String host : vs) {
            mapper.addContext(host, contextRoot, c, new String[0], null);
            /*
            if (adapter instanceof StaticResourcesAdapter) {
            mapper.addWrapper(host, ctx, wrapper, c);
            }
             */
        }
    }

    public void unregister(final Endpoint endpoint) {
        unregister(endpoint.getContextRoot());
    }


    
    private static final class AfterServiceListenerImpl implements AfterServiceListener {

        @Override
        public void onAfterService(final Request request) {
            final MappingData mappingData = request.getNote(MAPPING_DATA);
            if (mappingData != null) {
                mappingData.recycle();
            }
        }
    }
}

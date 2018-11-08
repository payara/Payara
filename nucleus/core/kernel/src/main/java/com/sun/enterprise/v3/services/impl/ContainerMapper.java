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
// Portions Copyright [2016-2018] [Payara Foundation and/or affiliates]

package com.sun.enterprise.v3.services.impl;

import fish.payara.nucleus.healthcheck.stuck.StuckThreadsStore;
import fish.payara.nucleus.requesttracing.RequestTracingService;
import fish.payara.notification.requesttracing.RequestTraceSpan;
import fish.payara.nucleus.requesttracing.domain.PropagationHeaders;
import org.glassfish.api.container.Adapter;
import org.glassfish.api.container.Sniffer;
import org.glassfish.api.deployment.ApplicationContainer;
import org.glassfish.api.logging.LogHelper;
import org.glassfish.grizzly.config.ContextRootInfo;
import org.glassfish.grizzly.config.GrizzlyListener;
import org.glassfish.grizzly.http.Note;
import org.glassfish.grizzly.http.server.*;
import org.glassfish.grizzly.http.server.util.Mapper;
import org.glassfish.grizzly.http.server.util.MappingData;
import org.glassfish.grizzly.http.util.CharChunk;
import org.glassfish.grizzly.http.util.DataChunk;
import org.glassfish.grizzly.http.util.MimeType;
import org.glassfish.internal.grizzly.ContextMapper;
import org.glassfish.kernel.KernelLoggerInfo;

import java.io.CharConversionException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Container's mapper which maps {@link ByteBuffer} bytes representation to an  {@link HttpHandler}, {@link
 * ApplicationContainer} and ProtocolFilter chain. The mapping result is stored inside {@link MappingData} which
 * is eventually shared with the CoyoteAdapter, which is the entry point with the Catalina Servlet Container.
 *
 * @author Jeanfrancois Arcand
 * @author Alexey Stashok
 */
@SuppressWarnings({"NonPrivateFieldAccessedInSynchronizedContext"})
public class ContainerMapper extends ADBAwareHttpHandler {

    private static final Logger LOGGER = KernelLoggerInfo.getLogger();
    private final static String ROOT = "";
    private ContextMapper mapper;
    private final GrizzlyListener listener;
    private String defaultHostName = "server";
    private final GrizzlyService grizzlyService;
    protected final static Note<MappingData> MAPPING_DATA =
            Request.<MappingData>createNote("MappingData");
    // Make sure this value is always aligned with {@link org.apache.catalina.connector.CoyoteAdapter}
    // (@see org.apache.catalina.connector.CoyoteAdapter)
    private final static Note<DataChunk> DATA_CHUNK =
            Request.<DataChunk>createNote("DataChunk");
    private final ReentrantReadWriteLock mapperLock;
    private RequestTracingService requestTracing;
    private StuckThreadsStore stuckThreadsStore;
    
    private static final AfterServiceListener afterServiceListener =
            new AfterServiceListenerImpl();
    /**
     * Are we running multiple {@ Adapter} or {@link HttpHandlerChain}
     */
    private boolean mapMultipleAdapter;

    public ContainerMapper(final GrizzlyService service,
            final GrizzlyListener grizzlyListener) {
        listener = grizzlyListener;
        grizzlyService = service;
        mapperLock = service.obtainMapperLock();
        requestTracing = grizzlyService.getHabitat().getService(RequestTracingService.class);
        stuckThreadsStore = grizzlyService.getHabitat().getService(StuckThreadsStore.class);

    }

    /**
     * Set the default host that will be used when we map.
     *
     * @param defaultHost
     */
    protected void setDefaultHost(String defaultHost) {
        mapperLock.writeLock().lock();
        try {
            defaultHostName = defaultHost;
        } finally {
            mapperLock.writeLock().unlock();
        }
    }

    /**
     * Set the {@link ContextMapper} instance used for mapping the container and its associated {@link Adapter}.
     *
     * @param mapper
     */
    protected void setMapper(ContextMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Configure the {@link ContextMapper}.
     */
    protected void configureMapper() {
        mapperLock.writeLock().lock();

        try {
            mapper.setDefaultHostName(defaultHostName);
            mapper.addHost(defaultHostName, new String[]{}, null);
            mapper.addContext(defaultHostName, ROOT,
                    new ContextRootInfo(this, null),
                    new String[]{"index.html", "index.htm"}, null);
            // Container deployed have the right to override the default setting.
            Mapper.setAllowReplacement(true);
        } finally {
            mapperLock.writeLock().unlock();
        }
    }

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
        try {
            request.addAfterServiceListener(afterServiceListener);
            
            final Callable handler = lookupHandler(request, response);
            if (stuckThreadsStore != null){
                stuckThreadsStore.registerThread(Thread.currentThread().getId());
            }
            if (requestTracing != null) {
                try {
                    // Try to get the propagated trace ID if there is one
                    String propagatedTraceIdHeader = request.getHeader(PropagationHeaders.PROPAGATED_TRACE_ID);
                    
                    if (propagatedTraceIdHeader == null) {
                        // No header with ID provided: just try to start a normal trace
                        requestTracing.startTrace("processContainerRequest");
                    } else {
                        UUID propagatedTraceId = UUID.fromString(propagatedTraceIdHeader);
                        
                        try {
                            // Try to get the propagated parent ID if there is one
                            UUID propagatedParentId = 
                                    UUID.fromString(request.getHeader(PropagationHeaders.PROPAGATED_PARENT_ID));
                            // Try to get the relationship type if present
                            RequestTraceSpan.SpanContextRelationshipType propagatedSpanContextRelationshipType = RequestTraceSpan
                                    .SpanContextRelationshipType.valueOf(request.getHeader(
                                            PropagationHeaders.PROPAGATED_RELATIONSHIP_TYPE));
                            requestTracing.startTrace(propagatedTraceId, propagatedParentId, 
                                    propagatedSpanContextRelationshipType, "processContainerRequest");
                        } catch (NullPointerException | IllegalArgumentException ex) {
                            LogHelper.log(LOGGER, Level.WARNING, KernelLoggerInfo.invalidPropagatedParentId, ex, 
                                    request.getHeader(PropagationHeaders.PROPAGATED_PARENT_ID));
                            // Just try to start a normal trace
                            requestTracing.startTrace("processContainerRequest");
                        }
                    }
                } catch (IllegalArgumentException ex) {
                    if (LOGGER.isLoggable(Level.WARNING)) {
                        LogHelper.log(LOGGER, Level.WARNING, KernelLoggerInfo.invalidPropagatedTraceId, ex, 
                                request.getHeader(PropagationHeaders.PROPAGATED_TRACE_ID));
                    }
                    // Just try to start a normal trace
                    requestTracing.startTrace("processContainerRequest");
                }
            }
            if (stuckThreadsStore != null){
                stuckThreadsStore.registerThread(Thread.currentThread().getId());
            }
            handler.call();
        } catch (Exception ex) {
            try {
                if (LOGGER.isLoggable(Level.WARNING)) {
                    LogHelper.log(LOGGER, Level.WARNING, KernelLoggerInfo.exceptionMapper, ex, 
                            request.getRequest().getRequestURIRef().getDecodedRequestURIBC());
                }
                
                response.sendError(500);
            } catch (Exception ex2) {
                if (LOGGER.isLoggable(Level.WARNING)) {
                    LOGGER.log(Level.WARNING, KernelLoggerInfo.exceptionMapper2, ex2);
                }
                if (ex2 instanceof CharConversionException) {
                    response.sendError(500);
                }
            }
        }
        finally {
            if (requestTracing != null) {
                requestTracing.endTrace();
            }
            if (stuckThreadsStore != null){
                stuckThreadsStore.deregisterThread(Thread.currentThread().getId());
            }
        }
    }

    private Callable lookupHandler(final Request request,
            final Response response) throws CharConversionException, Exception {
        MappingData mappingData;
        
        mapperLock.readLock().lock();
        
        try {
            // If we have only one Adapter deployed, invoke that Adapter directly.
            if (!mapMultipleAdapter) {
                // Remove the MappingData as we might delegate the request
                // to be serviced directly by the WebContainer
                final HttpHandler httpHandler = mapper.getHttpHandler();
                if (httpHandler != null) {
                    request.setNote(MAPPING_DATA, null);
//                    httpHandler.service(request, response);
//                    return;
                    return new HttpHandlerCallable(httpHandler,
                            request, response);
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
            HttpHandler httpHandler;

            final CharChunk decodedURICC = decodedURI.getCharChunk();
            final int semicolon = decodedURICC.indexOf(';', 0);

            // Map the request without any trailling.
            httpHandler = mapUriWithSemicolon(request, decodedURI,
                    semicolon, mappingData);
            if (httpHandler == null || httpHandler instanceof ContainerMapper) {
                String ext = decodedURI.toString();
                String type = "";
                if (ext.lastIndexOf(".") > 0) {
                    ext = "*" + ext.substring(ext.lastIndexOf("."));
                    type = ext.substring(ext.lastIndexOf(".") + 1);
                }

                if (!MimeType.contains(type) && !"/".equals(ext)) {
                    initializeFileURLPattern(ext);
                    mappingData.recycle();
                    httpHandler = mapUriWithSemicolon(request, decodedURI,
                            semicolon, mappingData);
                } else {
//                    super.service(request, response);
//                    return;
                    return new SuperCallable(request, response);
                }
            }

            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Request: {0} was mapped to Adapter: {1}",
                        new Object[]{decodedURI.toString(), httpHandler});
            }

            // The Adapter used for servicing static pages doesn't decode the
            // request by default, hence do not pass the undecoded request.
            if (httpHandler == null || httpHandler instanceof ContainerMapper) {
//                super.service(request, response);
                return new SuperCallable(request, response);
            } else {
//                httpHandler.service(request, response);
                return new HttpHandlerCallable(httpHandler, request, response);
            }
        } finally {
            mapperLock.readLock().unlock();
        }         
    }
    
    private void initializeFileURLPattern(String ext) {
        for (Sniffer sniffer : grizzlyService.getHabitat().<Sniffer>getAllServices(Sniffer.class)) {
            boolean match = false;
            if (sniffer.getURLPatterns() != null) {

                for (String pattern : sniffer.getURLPatterns()) {
                    if (pattern.equalsIgnoreCase(ext)) {
                        match = true;
                        break;
                    }
                }

                HttpHandler httpHandler;
                if (match) {
                    httpHandler = grizzlyService.getHabitat().getService(SnifferAdapter.class);
                    ((SnifferAdapter) httpHandler).initialize(sniffer, this);
                    ContextRootInfo c = new ContextRootInfo(httpHandler, null);

                    mapperLock.readLock().unlock();
                    mapperLock.writeLock().lock();
                    try {
                        for (String pattern : sniffer.getURLPatterns()) {
                            for (String host : grizzlyService.hosts) {
                                mapper.addWrapper(host, ROOT, pattern, c,
                                        "*.jsp".equals(pattern) || "*.jspx".equals(pattern));
                            }
                        }
                    } finally {
                        mapperLock.readLock().lock();
                        mapperLock.writeLock().unlock();
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
    final HttpHandler mapUriWithSemicolon(final Request req,
            final DataChunk decodedURI, int semicolonPos,
            final MappingData mappingData) throws Exception {

        mapperLock.readLock().lock();

        try {
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
        } finally {
            mapperLock.readLock().unlock();
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
        } else if (mappingData.context != null &&
                "com.sun.enterprise.web.WebModule".equals(mappingData.context.getClass().getName())) {
            return mapper.getHttpHandler();
        }
        return null;
    }

    public void register(String contextRoot, Collection<String> vs, HttpHandler httpService,
            ApplicationContainer container) {

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "MAPPER({0}) REGISTER contextRoot: {1} adapter: {2} container: {3} port: {4}",
                    new Object[]{this, contextRoot, httpService, container, String.valueOf(listener.getPort())});
        }

        mapMultipleAdapter = true;
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

        mapMultipleAdapter = true;
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

    private final static class HttpHandlerCallable implements Callable {
        private final HttpHandler httpHandler;
        private final Request request;
        private final Response response;

        public HttpHandlerCallable(final HttpHandler httpHandler,
                final Request request, final Response response) {
            this.httpHandler = httpHandler;
            this.request = request;
            this.response = response;
        }

        @Override
        public Object call() throws Exception {
            httpHandler.service(request, response);
            return null;
        }
    }

    private final class SuperCallable implements Callable {
        final Request req;
        final Response res;

        public SuperCallable(Request req, Response res) {
            this.req = req;
            this.res = res;
        }

        @Override
        public Object call() throws Exception {
            ContainerMapper.super.service(req, res);
            return null;
        }
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

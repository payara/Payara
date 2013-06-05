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

import com.sun.appserv.server.util.Version;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.enterprise.config.serverbeans.VirtualServer;
import com.sun.enterprise.v3.services.impl.monitor.ConnectionMonitor;
import com.sun.enterprise.v3.services.impl.monitor.FileCacheMonitor;
import com.sun.enterprise.v3.services.impl.monitor.GrizzlyMonitoring;
import com.sun.enterprise.v3.services.impl.monitor.KeepAliveMonitor;
import com.sun.enterprise.v3.services.impl.monitor.ThreadPoolMonitor;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.config.GenericGrizzlyListener;
import org.glassfish.grizzly.config.dom.Http;
import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.grizzly.config.dom.Protocol;
import org.glassfish.grizzly.config.dom.ThreadPool;
import org.glassfish.grizzly.config.dom.Transport;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.http.HttpHeader;
import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.http.HttpResponsePacket;
import org.glassfish.grizzly.http.KeepAlive;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.filecache.FileCache;
import org.glassfish.grizzly.http.server.util.Mapper;
import org.glassfish.grizzly.http.util.Header;
import org.glassfish.grizzly.threadpool.ThreadPoolConfig;
import org.glassfish.grizzly.utils.DelayedExecutor;
import org.glassfish.hk2.api.DynamicConfiguration;
import org.glassfish.hk2.api.DynamicConfigurationService;
import org.glassfish.hk2.api.IndexedFilter;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.AbstractActiveDescriptor;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.internal.grizzly.V3Mapper;
import org.jvnet.hk2.config.types.Property;

public class GlassfishNetworkListener extends GenericGrizzlyListener {
    private final GrizzlyService grizzlyService;
    private final Logger logger;

    private volatile HttpAdapter httpAdapter;
    
    public GlassfishNetworkListener(GrizzlyService grizzlyService,
            Logger logger) {
        this.grizzlyService = grizzlyService;
        this.logger = logger;
    }

    @Override
    public void start() throws IOException {
        registerMonitoringStatsProviders();
        
        super.start();
    }

    @Override
    public void stop() throws IOException {
        ServiceLocator locator = grizzlyService.getHabitat();
        IndexedFilter removeFilter = BuilderHelper.createNameAndContractFilter(Mapper.class.getName(),
                (address.toString() + port));

        DynamicConfigurationService dcs = locator.getService(DynamicConfigurationService.class);
        DynamicConfiguration config = dcs.createDynamicConfiguration();

        config.addUnbindFilter(removeFilter);

        config.commit();

        unregisterMonitoringStatsProviders();
        super.stop();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getAdapter(Class<T> adapterClass) {
        if (HttpAdapter.class.equals(adapterClass)) {
            return (T) httpAdapter;
        }

        return super.getAdapter(adapterClass);
    }

    @Override
    protected void configureTransport(final NetworkListener networkListener,
                                      final Transport transportConfig,
                                      final FilterChainBuilder filterChainBuilder) {

        super.configureTransport(networkListener, transportConfig,
                filterChainBuilder);

        transport.getConnectionMonitoringConfig().addProbes(new ConnectionMonitor(
                grizzlyService.getMonitoring(), name, transport));
    }

    @Override
    protected void configureHttpProtocol(final ServiceLocator habitat,
            final NetworkListener networkListener,
            final Http http, final FilterChainBuilder filterChainBuilder,
            boolean securityEnabled) {

        if (httpAdapter == null) {
            registerMonitoringStatsProviders();

            final V3Mapper mapper = new V3Mapper(logger);

            mapper.setPort(port);
            mapper.setId(name);

            final ContainerMapper containerMapper = new ContainerMapper(grizzlyService, this);
            containerMapper.setMapper(mapper);
            containerMapper.setDefaultHost(http.getDefaultVirtualServer());
            containerMapper.configureMapper();

            VirtualServer vs = null;
            String webAppRootPath = null;

            final Collection<VirtualServer> list = grizzlyService.getHabitat().getAllServices(VirtualServer.class);
            final String vsName = http.getDefaultVirtualServer();
            for (final VirtualServer virtualServer : list) {
                if (virtualServer.getId().equals(vsName)) {
                    vs = virtualServer;
                    webAppRootPath = vs.getDocroot();

                    if (!grizzlyService.hasMapperUpdateListener() && vs.getProperty() != null
                            && !vs.getProperty().isEmpty()) {
                        for (final Property p : vs.getProperty()) {
                            final String propertyName = p.getName();
                            if (propertyName.startsWith("alternatedocroot")) {
                                String value = p.getValue();
                                String[] mapping = value.split(" ");

                                if (mapping.length != 2) {
                                    logger.log(Level.WARNING, "Invalid alternate_docroot {0}", value);
                                    continue;
                                }

                                String docBase = mapping[1].substring("dir=".length());
                                String urlPattern = mapping[0].substring("from=".length());
                                containerMapper.addAlternateDocBase(urlPattern, docBase);
                            }
                        }
                    }
                    break;
                }
            }

            httpAdapter = new HttpAdapterImpl(vs, containerMapper, webAppRootPath);
            containerMapper.addDocRoot(webAppRootPath);

            AbstractActiveDescriptor<V3Mapper> aad = BuilderHelper.createConstantDescriptor(mapper);
            aad.addContractType(Mapper.class);
            aad.setName(address.toString() + port);
            
            ServiceLocatorUtilities.addOneDescriptor(grizzlyService.getHabitat(), aad);
            super.configureHttpProtocol(habitat, networkListener, http, filterChainBuilder, securityEnabled);
            final Protocol protocol = http.getParent();
            for (NetworkListener listener : protocol.findNetworkListeners()) {
                grizzlyService.notifyMapperUpdateListeners(listener, mapper);
            }
        } else {
            super.configureHttpProtocol(habitat, networkListener, http, filterChainBuilder, securityEnabled);
        }
    }

    @Override
    protected HttpHandler getHttpHandler() {
        return httpAdapter.getMapper();
    }

    @Override
    protected KeepAlive configureKeepAlive(Http http) {
        final KeepAlive keepAlive = super.configureKeepAlive(http);
        keepAlive.getMonitoringConfig().addProbes(new KeepAliveMonitor(
                grizzlyService.getMonitoring(), name, keepAlive));
        return keepAlive;
    }

    @Override
    protected FileCache configureHttpFileCache(org.glassfish.grizzly.config.dom.FileCache cache) {
        
        final FileCache fileCache = super.configureHttpFileCache(cache);
        fileCache.getMonitoringConfig().addProbes(new FileCacheMonitor(
                grizzlyService.getMonitoring(), name, fileCache));
        
        return fileCache;
    }

    @Override
    protected ThreadPoolConfig configureThreadPoolConfig(final NetworkListener networkListener,
                                                         final ThreadPool threadPool) {
        
        final ThreadPoolConfig config = super.configureThreadPoolConfig(
                networkListener, threadPool);
        config.getInitialMonitoringConfig().addProbes(new ThreadPoolMonitor(
                grizzlyService.getMonitoring(), name, config));
        return config;
    }

    @Override
    protected org.glassfish.grizzly.http.HttpServerFilter createHttpServerCodecFilter(
            final Http http,
            final boolean isChunkedEnabled, final int headerBufferLengthBytes,
            final String defaultResponseType, final KeepAlive keepAlive,
            final DelayedExecutor delayedExecutor,
            final int maxRequestHeaders, final int maxResponseHeaders) {
        
        return new GlassfishHttpCodecFilter(
                http == null || Boolean.parseBoolean(http.getXpoweredBy()),
                isChunkedEnabled,
                headerBufferLengthBytes,
                defaultResponseType,
                keepAlive,
                delayedExecutor,
                maxRequestHeaders,
                maxResponseHeaders);
    }



    protected void registerMonitoringStatsProviders() {
        final String nameLocal = name;
        final GrizzlyMonitoring monitoring = grizzlyService.getMonitoring();

        monitoring.registerThreadPoolStatsProvider(nameLocal);
        monitoring.registerKeepAliveStatsProvider(nameLocal);
        monitoring.registerFileCacheStatsProvider(nameLocal);
        monitoring.registerConnectionQueueStatsProvider(nameLocal);
    }

    protected void unregisterMonitoringStatsProviders() {
        final String localName = name;
        final GrizzlyMonitoring monitoring = grizzlyService.getMonitoring();

        monitoring.unregisterThreadPoolStatsProvider(localName);
        monitoring.unregisterKeepAliveStatsProvider(localName);
        monitoring.unregisterFileCacheStatsProvider(localName);
        monitoring.unregisterConnectionQueueStatsProvider(localName);
    }

    static List<String> toArray(String s, String token) {
        final ArrayList<String> list = new ArrayList<String>();
        
        int from = 0;
        do {
            final int idx = s.indexOf(token, from);

            if (idx == -1) {
                final String str = s.substring(from, s.length()).trim();
                list.add(str);
                break;
            }

            final String str = s.substring(from, idx).trim();
            list.add(str);

            from = idx + 1;
            
        } while (true);

        return list;
    }
    
    protected static class HttpAdapterImpl implements HttpAdapter {
        private final VirtualServer virtualServer;
        private final ContainerMapper conainerMapper;
        private final String webAppRootPath;

        public HttpAdapterImpl(VirtualServer virtualServer, ContainerMapper conainerMapper, String webAppRootPath) {
            this.virtualServer = virtualServer;
            this.conainerMapper = conainerMapper;
            this.webAppRootPath = webAppRootPath;
        }


        @Override
        public ContainerMapper getMapper() {
            return conainerMapper;
        }

        @Override
        public VirtualServer getVirtualServer() {
            return virtualServer;
        }

        @Override
        public String getWebAppRootPath() {
            return webAppRootPath;
        }
    }
    
    /**
     * Glassfish specific HttpCodecFilter extension.
     */
    private static class GlassfishHttpCodecFilter extends org.glassfish.grizzly.http.HttpServerFilter {
        private final String serverVersion;
        private final String xPoweredBy;
        
        public GlassfishHttpCodecFilter(
                final boolean isXPoweredByEnabled,
                final boolean chunkingEnabled,
                final int maxHeadersSize,
                final String defaultResponseContentType,
                final KeepAlive keepAlive, final DelayedExecutor executor,
                final int maxRequestHeaders, final int maxResponseHeaders) {
            super(chunkingEnabled, maxHeadersSize, defaultResponseContentType,
                    keepAlive, executor, maxRequestHeaders, maxResponseHeaders);
            
            /*
            * Set the server info.
            * By default, the server info is taken from Version#getVersion.
            * However, customers may override it via the product.name system
            * property.
            * Some customers prefer not to disclose the server info
            * for security reasons, in which case they would set the value of the
            * product.name system property to the empty string. In this case,
            * the server name will not be publicly disclosed via the "Server"
            * HTTP response header (which will be suppressed) or any container
            * generated error pages. However, it will still appear in the
            * server logs (see IT 6900).
            * 
            * Taken from com.sun.enterprise.web.WebContainer code
            */
            String serverInfo = System.getProperty("product.name");
            
            serverVersion = serverInfo != null ? serverInfo : Version.getVersion();
            
            if (isXPoweredByEnabled) {
                xPoweredBy = "Servlet/3.1 JSP/2.3 "
                        + "(" + ((serverInfo != null && !serverInfo.isEmpty()) ? serverInfo : Version.getVersion())
                        + " Java/"
                        + System.getProperty("java.vm.vendor") + "/"
                        + System.getProperty("java.specification.version") + ")";
            } else {
                xPoweredBy = null;
            }
        }
        
        @Override
        protected boolean onHttpHeaderParsed(final HttpHeader httpHeader,
                final Buffer buffer,
                final FilterChainContext ctx) {
            
            final boolean result = super.onHttpHeaderParsed(httpHeader,
                    buffer, ctx);
            
            final HttpRequestPacket request = (HttpRequestPacket) httpHeader;
            final HttpResponsePacket response = request.getResponse();
            
            // Set response "Server" header
            if (serverVersion != null && !serverVersion.isEmpty()) {
                response.addHeader(Header.Server, serverVersion);
            }
            
            // Set response "X-Powered-By" header
            if (xPoweredBy != null) {
                response.addHeader(Header.XPoweredBy, xPoweredBy);
            }
            
            return result;
        }
    }
}

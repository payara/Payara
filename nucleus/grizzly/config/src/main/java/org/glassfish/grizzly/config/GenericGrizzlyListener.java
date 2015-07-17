/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007-2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.grizzly.config;

import java.beans.PropertyChangeEvent;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.IOStrategy;
import org.glassfish.grizzly.SocketBinder;
import org.glassfish.grizzly.config.dom.Http;
import org.glassfish.grizzly.config.dom.Http2;
import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.grizzly.config.dom.PortUnification;
import org.glassfish.grizzly.config.dom.Protocol;
import org.glassfish.grizzly.config.dom.ProtocolChain;
import org.glassfish.grizzly.config.dom.ProtocolChainInstanceHandler;
import org.glassfish.grizzly.config.dom.ProtocolFilter;
import org.glassfish.grizzly.config.dom.SelectionKeyHandler;
import org.glassfish.grizzly.config.dom.Spdy;
import org.glassfish.grizzly.config.dom.Ssl;
import org.glassfish.grizzly.config.dom.ThreadPool;
import org.glassfish.grizzly.config.dom.Transport;
import org.glassfish.grizzly.config.dom.Transports;
import org.glassfish.grizzly.config.portunif.HttpRedirectFilter;
import org.glassfish.grizzly.filterchain.Filter;
import org.glassfish.grizzly.filterchain.FilterChain;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.http.ContentEncoding;
import org.glassfish.grizzly.http.GZipContentEncoding;
import org.glassfish.grizzly.http.KeepAlive;
import org.glassfish.grizzly.http.LZMAContentEncoding;
import org.glassfish.grizzly.http.server.AddOn;
import org.glassfish.grizzly.http.server.BackendConfiguration;
import org.glassfish.grizzly.http.server.CompressionEncodingFilter;
import org.glassfish.grizzly.http.server.CompressionLevel;
import org.glassfish.grizzly.http.server.FileCacheFilter;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServerFilter;
import org.glassfish.grizzly.http.server.ServerFilterConfiguration;
import org.glassfish.grizzly.http.server.StaticHttpHandler;
import org.glassfish.grizzly.http.server.filecache.FileCache;
import org.glassfish.grizzly.http.util.MimeHeaders;
import org.glassfish.grizzly.memory.AbstractMemoryManager;
import org.glassfish.grizzly.memory.ByteBufferManager;
import org.glassfish.grizzly.nio.NIOTransport;
import org.glassfish.grizzly.nio.RoundRobinConnectionDistributor;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.glassfish.grizzly.nio.transport.UDPNIOTransport;
import org.glassfish.grizzly.nio.transport.UDPNIOTransportBuilder;
import org.glassfish.grizzly.portunif.PUFilter;
import org.glassfish.grizzly.portunif.PUProtocol;
import org.glassfish.grizzly.portunif.ProtocolFinder;
import org.glassfish.grizzly.portunif.finders.SSLProtocolFinder;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.grizzly.ssl.SSLBaseFilter;
import org.glassfish.grizzly.strategies.WorkerThreadIOStrategy;
import org.glassfish.grizzly.threadpool.DefaultWorkerThread;
import org.glassfish.grizzly.threadpool.GrizzlyExecutorService;
import org.glassfish.grizzly.threadpool.ThreadPoolConfig;
import org.glassfish.grizzly.utils.DelayedExecutor;
import org.glassfish.grizzly.utils.IdleTimeoutFilter;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.config.ConfigBeanProxy;

/**
 * Generic {@link GrizzlyListener} implementation, which is not HTTP dependent, and can support any Transport
 * configuration, based on {@link FilterChain}.
 *
 * @author Alexey Stashok
 */
public class GenericGrizzlyListener implements GrizzlyListener {
    /**
     * The logger to use for logging messages.
     */
    private static final Logger LOGGER =
            Grizzly.logger(GenericGrizzlyListener.class);
    
    protected volatile String name;
    protected volatile InetAddress address;
    protected volatile int port;
    protected NIOTransport transport;
    protected FilterChain rootFilterChain;
    private volatile ExecutorService workerExecutorService;
    private volatile ExecutorService auxExecutorService;
    private volatile DelayedExecutor delayedExecutor;
    private volatile long transactionTimeoutMillis = -1;
    
    // ajp enabled flag
    protected volatile boolean isAjpEnabled;
    // spdy enabled flag
    protected volatile boolean isSpdyEnabled;
    // spdy enabled flag
    protected volatile boolean isHttp2Enabled;
    // websocket enabled flag
    protected volatile boolean isWebSocketEnabled;
    // comet enabled flag
    protected volatile boolean isCometEnabled;

    @Override
    public String getName() {
        return name;
    }

    protected final void setName(String name) {
        this.name = name;
    }

    @Override
    public InetAddress getAddress() {
        return address;
    }

    protected final void setAddress(InetAddress inetAddress) {
        address = inetAddress;
    }

    @Override
    public int getPort() {
        return port;
    }

    protected void setPort(int port) {
        this.port = port;
    }

    @Override
    public void start() throws IOException {
        startDelayedExecutor();
        ((SocketBinder) transport).bind(new InetSocketAddress(address, port));
        transport.start();
    }

    @Override
    public void stop() throws IOException {
        stopDelayedExecutor();
        final NIOTransport localTransport = transport;
        transport = null;
        if (localTransport != null) {
            localTransport.shutdownNow();
        }
        
        if (workerExecutorService != null) {
            final ExecutorService localExecutorService = workerExecutorService;
            workerExecutorService = null;
            localExecutorService.shutdownNow();
        }
        rootFilterChain = null;
    }

    @Override
    public void destroy() {
    }

    @Override
    public void processDynamicConfigurationChange(ServiceLocator habitat,
        PropertyChangeEvent[] events) {
    }

    @Override
    public <T> T getAdapter(Class<T> adapterClass) {
        return null;
    }

    public <E> List<E> getFilters(Class<E> clazz) {
        return getFilters(clazz, rootFilterChain, new ArrayList<E>(2));
    }

    public org.glassfish.grizzly.Transport getTransport() {
        return transport;
    }

    /**
     * @return <tt>true</tt> if AJP (or JK) is enabled for this listener, or
     * <tt>false</tt> otherwise.
     */
    public boolean isAjpEnabled() {
        return isAjpEnabled;
    }

    /**
     * @return <tt>true</tt> if SPDY is enabled for this listener, or
     * <tt>false</tt> otherwise.
     */
    public boolean isSpdyEnabled() {
        return isSpdyEnabled;
    }

    /**
     * @return <tt>true</tt> if HTTP2 is enabled for this listener, or
     * <tt>false</tt> otherwise.
     */
    public boolean isHttp2Enabled() {
        return isHttp2Enabled;
    }

    /**
     * @return <tt>true</tt> if WebSocket is enabled for this listener, or
     * <tt>false</tt> otherwise.
     */
    public boolean isWebSocketEnabled() {
        return isWebSocketEnabled;
    }

    /**
     * @return <tt>true</tt> if Comet is enabled for this listener, or
     * <tt>false</tt> otherwise.
     */
    public boolean isCometEnabled() {
        return isCometEnabled;
    }
    
    @SuppressWarnings({"unchecked"})
    public static <E> List<E> getFilters(Class<E> clazz,
        FilterChain filterChain, List<E> filters) {
        for (final Filter filter : filterChain) {
            if (clazz.isAssignableFrom(filter.getClass())) {
                filters.add((E) filter);
            }
            if (PUFilter.class.isAssignableFrom(filter.getClass())) {
                final Set<PUProtocol> puProtocols = ((PUFilter) filter).getProtocols();
                for (PUProtocol puProtocol : puProtocols) {
                    getFilters(clazz, puProtocol.getFilterChain(), filters);
                }
            }
        }
        return filters;
    }

    /*
     * Configures the given grizzlyListener.
     *
     * @param networkListener The NetworkListener to configure
     */
    // TODO: Must get the information from domain.xml Config objects.
    // TODO: Pending Grizzly issue 54
    @Override
    public void configure(final ServiceLocator habitat,
        final NetworkListener networkListener) throws IOException {
        setName(networkListener.getName());
        setAddress(InetAddress.getByName(networkListener.getAddress()));
        setPort(Integer.parseInt(networkListener.getPort()));

        final FilterChainBuilder filterChainBuilder = FilterChainBuilder.stateless();
        
        configureTransport(networkListener,
                           networkListener.findTransport(),
                           filterChainBuilder);

        configureProtocol(habitat, networkListener,
                networkListener.findProtocol(), filterChainBuilder);

        configureThreadPool(habitat, networkListener,
                networkListener.findThreadPool());

        rootFilterChain = filterChainBuilder.build();
        transport.setProcessor(rootFilterChain);
    }

    protected void configureTransport(final NetworkListener networkListener,
                                      final Transport transportConfig,
                                      final FilterChainBuilder filterChainBuilder) {
        
        final String transportClassName = transportConfig.getClassname();
        if (TCPNIOTransport.class.getName().equals(transportClassName)) {
            transport = configureTCPTransport(transportConfig);
        } else if (UDPNIOTransport.class.getName().equals(transportClassName)) {
            transport = configureUDPTransport();
        } else {
            throw new GrizzlyConfigException("Unsupported transport type " + transportConfig.getName());
        }

        String selectorName = transportConfig.getSelectionKeyHandler();
        if (selectorName != null) {
            if (getSelectionKeyHandlerByName(selectorName, transportConfig) != null) {
                if (LOGGER.isLoggable(Level.INFO)) {
                    LOGGER.warning("Element, selection-key-handler, has been deprecated and is effectively ignored by the runtime.");
                }
            }
        }

        if (!Transport.BYTE_BUFFER_TYPE.equalsIgnoreCase(transportConfig.getByteBufferType())) {
            transport.setMemoryManager(
                    new ByteBufferManager(true,
                                          AbstractMemoryManager.DEFAULT_MAX_BUFFER_SIZE,
                                          ByteBufferManager.DEFAULT_SMALL_BUFFER_SIZE));
        }
        
        final int acceptorThreads = Integer.parseInt(transportConfig.getAcceptorThreads());
        transport.setSelectorRunnersCount(acceptorThreads);

        final int readSize = Integer.parseInt(transportConfig.getSocketReadBufferSize());
        if (readSize > 0) {
            transport.setReadBufferSize(readSize);
        }

        final int writeSize = Integer.parseInt(transportConfig.getSocketWriteBufferSize());
        if (writeSize > 0) {
            transport.setWriteBufferSize(writeSize);
        }
        
        final ThreadPoolConfig kernelThreadPoolConfig = transport.getKernelThreadPoolConfig();

        kernelThreadPoolConfig.setPoolName(networkListener.getName() + "-kernel");
        if (acceptorThreads > 0) {
            kernelThreadPoolConfig.setCorePoolSize(acceptorThreads)
                .setMaxPoolSize(acceptorThreads);
        }
        
        transport.setIOStrategy(loadIOStrategy(transportConfig.getIoStrategy()));
        transport.setNIOChannelDistributor(
                new RoundRobinConnectionDistributor(
                transport,
                Boolean.parseBoolean(transportConfig.getDedicatedAcceptorEnabled())));
        
        filterChainBuilder.add(new TransportFilter());
    }

    protected NIOTransport configureTCPTransport(final Transport transportConfig) {
        
        final TCPNIOTransport tcpTransport = configureDefaultThreadPoolConfigs(
                TCPNIOTransportBuilder.newInstance().build());
        tcpTransport.setTcpNoDelay(Boolean.parseBoolean(transportConfig.getTcpNoDelay()));
        tcpTransport.setLinger(Integer.parseInt(transportConfig.getLinger()));
        tcpTransport.setWriteTimeout(Long.parseLong(transportConfig.getWriteTimeoutMillis()), TimeUnit.MILLISECONDS);
        tcpTransport.setReadTimeout(Long.parseLong(transportConfig.getReadTimeoutMillis()), TimeUnit.MILLISECONDS);
        tcpTransport.setServerConnectionBackLog(Integer.parseInt(transportConfig.getMaxConnectionsCount()));
        return tcpTransport;
    }

    protected NIOTransport configureUDPTransport() {
        return configureDefaultThreadPoolConfigs(
                UDPNIOTransportBuilder.newInstance().build());
    }

    protected <T extends NIOTransport> T configureDefaultThreadPoolConfigs(
            final T transport) {
        transport.setKernelThreadPoolConfig(ThreadPoolConfig.defaultConfig());
        transport.setWorkerThreadPoolConfig(ThreadPoolConfig.defaultConfig());
        
        return transport;
    }
    
    protected void configureProtocol(final ServiceLocator habitat,
            final NetworkListener networkListener,
            final Protocol protocol,
            final FilterChainBuilder filterChainBuilder) {
        
        if (Boolean.valueOf(protocol.getSecurityEnabled())) {
            configureSsl(habitat, 
                         getSsl(protocol),
                         filterChainBuilder);
        }
        configureSubProtocol(habitat, networkListener, protocol,
                filterChainBuilder);
    }

    protected void configureSubProtocol(final ServiceLocator habitat,
            final NetworkListener networkListener,
            final Protocol protocol,
            final FilterChainBuilder filterChainBuilder) {
        
        if (protocol.getHttp() != null) {
            final Http http = protocol.getHttp();
            configureHttpProtocol(habitat,
                                  networkListener,
                                  http,
                                  filterChainBuilder,
                                  Boolean.valueOf(protocol.getSecurityEnabled()));

        } else if (protocol.getPortUnification() != null) {
            // Port unification
            final PortUnification pu = protocol.getPortUnification();
            final String puFilterClassname = pu.getClassname();
            PUFilter puFilter = null;
            if (puFilterClassname != null) {
                try {
                    puFilter = Utils.newInstance(habitat,
                        PUFilter.class, puFilterClassname, puFilterClassname);
                    configureElement(habitat, networkListener, pu, puFilter);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING,
                        "Can not initialize port unification filter: "
                            + puFilterClassname + " default filter will be used instead", e);
                }
            }
            if (puFilter == null) {
                puFilter = new PUFilter();
            }
            List<org.glassfish.grizzly.config.dom.ProtocolFinder> findersConfig = pu.getProtocolFinder();
            for (org.glassfish.grizzly.config.dom.ProtocolFinder finderConfig : findersConfig) {
                final String finderClassname = finderConfig.getClassname();
                try {
                    final ProtocolFinder protocolFinder = Utils.newInstance(habitat,
                        ProtocolFinder.class, finderClassname, finderClassname);
                    configureElement(habitat, networkListener, finderConfig, protocolFinder);
                    final Protocol subProtocol = finderConfig.findProtocol();
                    final FilterChainBuilder subProtocolFilterChainBuilder =
                        puFilter.getPUFilterChainBuilder();
                    // If subprotocol is secured - we need to wrap it under SSLProtocolFinder
                    if (Boolean.valueOf(subProtocol.getSecurityEnabled())) {
                        final PUFilter extraSslPUFilter = new PUFilter();
                        
                        final Filter addedSSLFilter = configureSsl(
                                habitat, getSsl(subProtocol),
                                subProtocolFilterChainBuilder);
                        
                        subProtocolFilterChainBuilder.add(extraSslPUFilter);
                        final FilterChainBuilder extraSslPUFilterChainBuilder =
                            extraSslPUFilter.getPUFilterChainBuilder();
                        
                        try {
                            // temporary add SSL Filter, so subprotocol
                            // will see it
                            extraSslPUFilterChainBuilder.add(addedSSLFilter);
                            configureSubProtocol(habitat, networkListener,
                                    subProtocol, extraSslPUFilterChainBuilder);
                        } finally {
                            // remove SSL Filter
                            extraSslPUFilterChainBuilder.remove(addedSSLFilter);
                        }
                        
                        extraSslPUFilter.register(protocolFinder,
                                extraSslPUFilterChainBuilder.build());
                        
                        puFilter.register(new SSLProtocolFinder(
                            new SSLConfigurator(habitat, subProtocol.getSsl())),
                            subProtocolFilterChainBuilder.build());
                    } else {
                        configureSubProtocol(habitat, networkListener,
                                subProtocol, subProtocolFilterChainBuilder);
                        puFilter.register(protocolFinder, subProtocolFilterChainBuilder.build());
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Can not initialize sub protocol. Finder: "
                        + finderClassname, e);
                }
            }
            filterChainBuilder.add(puFilter);
        } else if (protocol.getHttpRedirect() != null) {
            filterChainBuilder.add(createHttpServerCodecFilter());
            final HttpRedirectFilter filter = new HttpRedirectFilter();
            filter.configure(habitat, networkListener, protocol.getHttpRedirect());
            filterChainBuilder.add(filter);
        } else {
            ProtocolChainInstanceHandler pcihConfig = protocol.getProtocolChainInstanceHandler();
            if (pcihConfig == null) {
                LOGGER.log(Level.WARNING, "Empty protocol declaration");
                return;
            }
            ProtocolChain filterChainConfig = pcihConfig.getProtocolChain();
            for (ProtocolFilter filterConfig : filterChainConfig.getProtocolFilter()) {
                final String filterClassname = filterConfig.getClassname();
                try {
                    final Filter filter = loadFilter(habitat,
                            filterConfig.getName(), filterClassname);
                    configureElement(habitat, networkListener, filterConfig, filter);
                    filterChainBuilder.add(filter);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Can not initialize protocol filter: "
                        + filterClassname, e);
                    throw new IllegalStateException("Can not initialize protocol filter: "
                        + filterClassname);
                }
            }
        }
    }

    protected static Filter configureSsl(final ServiceLocator habitat,
                                       final Ssl ssl,
                                       final FilterChainBuilder filterChainBuilder) {
        final SSLEngineConfigurator serverConfig = new SSLConfigurator(habitat, ssl);
//        final SSLEngineConfigurator clientConfig = new SSLConfigurator(habitat, ssl);
//        clientConfig.setClientMode(true);
        final SSLBaseFilter sslFilter = new SSLBaseFilter(serverConfig,
         //                                             clientConfig,
                                                      isRenegotiateOnClientAuthWant(ssl));
        sslFilter.setHandshakeTimeout(
                Long.parseLong(ssl.getHandshakeTimeoutMillis()), TimeUnit.MILLISECONDS);

        filterChainBuilder.add(sslFilter);
        return sslFilter;
    }

    private static boolean isRenegotiateOnClientAuthWant(final Ssl ssl) {
        return ssl == null || Boolean.parseBoolean(ssl.getRenegotiateOnClientAuthWant());
    }

    @SuppressWarnings({"unchecked"})
    private static boolean configureElement(ServiceLocator habitat,
            NetworkListener networkListener, ConfigBeanProxy configuration,
            Object instance) {
        
        if (instance instanceof ConfigAwareElement) {
            ((ConfigAwareElement) instance).configure(habitat, networkListener,
                    configuration);
            
            return true;
        }
        
        return false;
    }

    protected void configureThreadPool(final ServiceLocator habitat,
            final NetworkListener networkListener,
            final ThreadPool threadPool) {
        
        final String classname = threadPool.getClassname();
        if (classname != null &&
                !ThreadPool.DEFAULT_THREAD_POOL_CLASS_NAME.equals(classname)) {
            
            // Use custom thread pool
            try {
                final ExecutorService customThreadPool =
                        Utils.newInstance(habitat,
                        ExecutorService.class, classname, classname);
                
                if (customThreadPool != null) {
                    if (!configureElement(habitat, networkListener,
                            threadPool, customThreadPool)) {
                        LOGGER.log(Level.INFO,
                                "The ThreadPool configuration bean can not be "
                                + "passed to the custom thread-pool: {0}" +
                                " instance, because it's not {1}.",
                                new Object[] {
                                classname, ConfigAwareElement.class.getName()});
                    }
                    
                    workerExecutorService = customThreadPool;
                    transport.setWorkerThreadPool(customThreadPool);
                    return;
                }
                
                LOGGER.log(Level.WARNING,
                        "Can not initalize custom thread pool: {0}", classname);
                
            } catch (Throwable t) {
                LOGGER.log(Level.WARNING,
                        "Can not initalize custom thread pool: " + classname, t);
            }
        }
            
        try {
            // Use standard Grizzly thread pool
            workerExecutorService = GrizzlyExecutorService.createInstance(
                    configureThreadPoolConfig(networkListener, threadPool));
            transport.setWorkerThreadPool(workerExecutorService);
        } catch (NumberFormatException ex) {
            LOGGER.log(Level.WARNING, "Invalid thread-pool attribute", ex);
        }
    }

    protected ThreadPoolConfig configureThreadPoolConfig(final NetworkListener networkListener,
                                                         final ThreadPool threadPool) {

        final int maxQueueSize = threadPool.getMaxQueueSize() == null ? Integer.MAX_VALUE
            : Integer.parseInt(threadPool.getMaxQueueSize());
        final int minThreads = Integer.parseInt(threadPool.getMinThreadPoolSize());
        final int maxThreads = Integer.parseInt(threadPool.getMaxThreadPoolSize());
        final int timeout = Integer.parseInt(threadPool.getIdleThreadTimeoutSeconds());
        final ThreadPoolConfig poolConfig = ThreadPoolConfig.defaultConfig();
        poolConfig.setPoolName(networkListener.getName());
        poolConfig.setCorePoolSize(minThreads);
        poolConfig.setMaxPoolSize(maxThreads);
        poolConfig.setQueueLimit(maxQueueSize);

        // we specify the classloader that loaded this class to ensure
        // we present the same initial classloader no matter what mode
        // GlassFish is being run in.
        // See http://java.net/jira/browse/GLASSFISH-19639
        poolConfig.setInitialClassLoader(this.getClass().getClassLoader());

        poolConfig.setKeepAliveTime(timeout < 0 ? Long.MAX_VALUE : timeout, TimeUnit.SECONDS);
        if (transactionTimeoutMillis > 0 && !Utils.isDebugVM()) {
            poolConfig.setTransactionTimeout(obtainDelayedExecutor(),
                    transactionTimeoutMillis, TimeUnit.MILLISECONDS);
        }
        
        return poolConfig;
    }

    private DelayedExecutor obtainDelayedExecutor() {
        if (delayedExecutor != null) {
            return delayedExecutor;
        }
        
        final AtomicInteger threadCounter = new AtomicInteger();
        auxExecutorService = Executors.newCachedThreadPool(
            new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    final Thread newThread = new DefaultWorkerThread(
                        transport.getAttributeBuilder(),
                        getName() + "-expirer(" + threadCounter.incrementAndGet() + ")",
                        null,
                        r);
                    newThread.setDaemon(true);
                    return newThread;
                }
            });
        delayedExecutor = new DelayedExecutor(auxExecutorService);
        return delayedExecutor;
    }

    protected void startDelayedExecutor() {
        if (delayedExecutor != null) {
            delayedExecutor.start();
        }
    }
    
    protected void stopDelayedExecutor() {
        if (delayedExecutor != null) {
            final DelayedExecutor localDelayedExecutor = delayedExecutor;
            delayedExecutor = null;
            if (localDelayedExecutor != null) {
                localDelayedExecutor.stop();
                localDelayedExecutor.destroy();
            }
            final ExecutorService localThreadPool = auxExecutorService;
            auxExecutorService = null;
            if (localThreadPool != null) {
                localThreadPool.shutdownNow();
            }
        }
    }

    @SuppressWarnings({"deprecation"})
    protected void configureHttpProtocol(final ServiceLocator habitat,
            final NetworkListener networkListener,
            final Http http, final FilterChainBuilder filterChainBuilder,
            boolean secure) {
        transactionTimeoutMillis = Long.parseLong(http.getRequestTimeoutSeconds()) * 1000;
        filterChainBuilder.add(new IdleTimeoutFilter(obtainDelayedExecutor(),
               getTimeoutSeconds(http), TimeUnit.SECONDS));
        final org.glassfish.grizzly.http.HttpServerFilter httpServerFilter =
            createHttpServerCodecFilter(http);
        final Set<ContentEncoding> contentEncodings =
            configureContentEncodings(http);
        for (ContentEncoding contentEncoding : contentEncodings) {
            httpServerFilter.addContentEncoding(contentEncoding);
        }
//        httpServerFilter.getMonitoringConfig().addProbes(
//                serverConfig.getMonitoringConfig().getHttpConfig().getProbes());
        filterChainBuilder.add(httpServerFilter);
        final FileCache fileCache = configureHttpFileCache(http.getFileCache());
        fileCache.initialize(obtainDelayedExecutor());
        final FileCacheFilter fileCacheFilter = new FileCacheFilter(fileCache);
//        fileCache.getMonitoringConfig().addProbes(
//                serverConfig.getMonitoringConfig().getFileCacheConfig().getProbes());
        filterChainBuilder.add(fileCacheFilter);
        final HttpServerFilter webServerFilter = new HttpServerFilter(
                getHttpServerFilterConfiguration(http),
                obtainDelayedExecutor());

        final HttpHandler httpHandler = getHttpHandler();
        httpHandler.setAllowEncodedSlash(GrizzlyConfig.toBoolean(http.getEncodedSlashEnabled()));
        webServerFilter.setHttpHandler(httpHandler);
//        webServerFilter.getMonitoringConfig().addProbes(
//                serverConfig.getMonitoringConfig().getWebServerConfig().getProbes());
        filterChainBuilder.add(webServerFilter);

        configureSpdySupport(habitat, networkListener, http.getSpdy(), filterChainBuilder, secure);
        configureHttp2Support(habitat, networkListener, http.getHttp2(), filterChainBuilder, secure);

        // TODO: evaluate comet/websocket support over SPDY.
        configureCometSupport(habitat, networkListener, http, filterChainBuilder);

        configureWebSocketSupport(habitat, networkListener, http, filterChainBuilder);

        configureAjpSupport(habitat, networkListener, http, filterChainBuilder);
    }

   private int getTimeoutSeconds(final Http http) {
        // fix for Glassfish-21009
        int timeoutSeconds = Integer.parseInt(http.getTimeoutSeconds());
        return timeoutSeconds == 0 ? -1 : timeoutSeconds;
    }

    protected void configureSpdySupport(final ServiceLocator locator,
                                        final NetworkListener listener,
                                        final Spdy spdyElement,
                                        final FilterChainBuilder builder,
                                        final boolean secure) {
        if (spdyElement != null && spdyElement.getEnabled()) {

            boolean isNpnMode = spdyElement.getMode() == null ||
                    "npn".equalsIgnoreCase(spdyElement.getMode());
            
            // Spdy without NPN is supported, but warn that there may
            // be consequences to this configuration.
            if (!secure && isNpnMode) {
                LOGGER.log(Level.WARNING,
                        "SSL is not enabled for listener {0}.  SPDY support will be enabled, but will not be secured.  Some clients may not be able to use SPDY in this configuration.",
                        listener.getName());
            }

            // first try to lookup a service appropriate for the mode
            // that has been configured.
            AddOn spdyAddon = locator.getService(AddOn.class, "spdy");

            // if no service was found, attempt to load via reflection.
            if (spdyAddon == null) {
                Class<?> spdyMode;
                try {
                    spdyMode = Utils.loadClass("org.glassfish.grizzly.spdy.SpdyMode");
                } catch (ClassNotFoundException cnfe) {
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.fine("Unable to load class org.glassfish.grizzly.spdy.SpdyMode.  SPDY support cannot be enabled");
                    }
                    return;
                }
                Object[] enumConstants = spdyMode.getEnumConstants();
                Object mode = ((isNpnMode) ? enumConstants[1] : enumConstants[0]);
                spdyAddon = loadAddOn("org.glassfish.grizzly.spdy.SpdyAddOn", new Class[]{spdyMode}, mode);
            }

            if (spdyAddon != null) {
                // Configure SpdyAddOn
                configureElement(locator, listener, spdyElement, spdyAddon);
                
                // Spdy requires access to more information compared to the other addons
                // that are currently leveraged.  As such, we'll need to mock out a
                // Grizzly NetworkListener to pass to the addon.  This mock object will
                // only provide the information necessary for the addon to operate.
                // It will be important to keep this mock in sync with the details the
                // addon requires.
                spdyAddon.setup(createMockListener(secure), builder);
                isSpdyEnabled = true;
            }
        }
    }

    protected void configureHttp2Support(final ServiceLocator locator,
                                        final NetworkListener listener,
                                        final Http2 http2Element,
                                        final FilterChainBuilder builder,
                                        final boolean secure) {
        if (http2Element == null || http2Element.getEnabled()) {

            // first try to lookup a service appropriate for the mode
            // that has been configured.
            AddOn http2Addon = locator.getService(AddOn.class, "http2");

            // if no service was found, attempt to load via reflection.
            if (http2Addon == null) {
                http2Addon = loadAddOn("org.glassfish.grizzly.http2.Http2AddOn", new Class[]{});
            }

            if (http2Addon != null) {
                // Configure SpdyAddOn
                configureElement(locator, listener, http2Element, http2Addon);
                
                // Spdy requires access to more information compared to the other addons
                // that are currently leveraged.  As such, we'll need to mock out a
                // Grizzly NetworkListener to pass to the addon.  This mock object will
                // only provide the information necessary for the addon to operate.
                // It will be important to keep this mock in sync with the details the
                // addon requires.
                http2Addon.setup(createMockListener(secure), builder);
                isHttp2Enabled = true;
            }
        }
    }
    
    protected org.glassfish.grizzly.http.server.NetworkListener createMockListener(final boolean isSecure) {
        final TCPNIOTransport transportLocal = (TCPNIOTransport) transport;
        return new org.glassfish.grizzly.http.server.NetworkListener("mock") {
            @Override
            public TCPNIOTransport getTransport() {
                return transportLocal;
            }

            @Override
            public boolean isSecure() {
                return isSecure;
            }
        };
    }

    protected void configureCometSupport(final ServiceLocator habitat,
            final NetworkListener networkListener,
            final Http http, final FilterChainBuilder filterChainBuilder) {

        if(GrizzlyConfig.toBoolean(http.getCometSupportEnabled())) {
            final AddOn cometAddOn = loadAddOn(habitat, "comet",
                    "org.glassfish.grizzly.comet.CometAddOn");
            if (cometAddOn != null) {
                configureElement(habitat, networkListener, http, cometAddOn);
                cometAddOn.setup(null, filterChainBuilder);
                isCometEnabled = true;
            }
        }
    }

    protected void configureWebSocketSupport(final ServiceLocator habitat,
                                             final NetworkListener listener,
                                             final Http http,
                                             final FilterChainBuilder filterChainBuilder) {
        final boolean websocketsSupportEnabled = Boolean.parseBoolean(http.getWebsocketsSupportEnabled());
        if (websocketsSupportEnabled) {
            AddOn wsAddOn = loadAddOn(habitat,
                                      "websocket",
                                      "org.glassfish.grizzly.websockets.WebSocketAddOn");
            if (wsAddOn != null) {
                if (!configureElement(habitat, listener, http, wsAddOn)) {
                    // Dealing with a WebSocketAddOn created by reflection vs
                    // an HK2 service.  We need to pass the configuration data
                    // manually via reflection.
                    try {
                        Method m = wsAddOn.getClass().getDeclaredMethod("setTimeoutInSeconds", Long.TYPE);
                        m.invoke(wsAddOn, Long.parseLong(http.getWebsocketsTimeoutSeconds()));
                    } catch (Exception e) {
                        if (LOGGER.isLoggable(Level.WARNING)) {
                            LOGGER.log(Level.WARNING, e.toString(), e);
                        }
                    }
                }
                wsAddOn.setup(null, filterChainBuilder);
                isWebSocketEnabled = true;
            }
        }
    }

    protected void configureAjpSupport(final ServiceLocator habitat,
            final NetworkListener networkListener,
            final Http http, final FilterChainBuilder filterChainBuilder) {

        final boolean jkSupportEnabled = http.getJkEnabled() != null ?
            Boolean.parseBoolean(http.getJkEnabled()) :
            Boolean.parseBoolean(networkListener.getJkEnabled());

        if (jkSupportEnabled) {
            final AddOn ajpAddOn = loadAddOn(habitat, "ajp",
                    "org.glassfish.grizzly.http.ajp.AjpAddOn");
            if (ajpAddOn != null) {
                configureElement(habitat, networkListener, http, ajpAddOn);
                ajpAddOn.setup(null, filterChainBuilder);
                isAjpEnabled = true;
            }
        }
    }

    
    /**
     * Load {@link AddOn} with the specific service name and classname.
     */
    private AddOn loadAddOn(ServiceLocator habitat, String name, String addOnClassName) {
        return Utils.newInstance(habitat, AddOn.class, name, addOnClassName);
    }

    /**
     * Load {@link AddOn} with the specific service name and classname.
     */
    private AddOn loadAddOn(String addOnClassName, Class[] argTypes, Object... args) {
        return Utils.newInstance(null, AddOn.class, name, addOnClassName, argTypes, args);
    }

    /**
     * Load {@link Filter} with the specific service name and classname.
     */
    private Filter loadFilter(ServiceLocator habitat, String name, String filterClassName) {
        return Utils.newInstance(habitat, Filter.class, name, filterClassName);
    }
    
//    private Filter loadFilter(ServiceLocator habitat,
//                              String name, 
//                              String filterClassName, 
//                              Class<?>[] ctorArgTypes, 
//                              Object[] ctorArgs) {
//        return Utils.newInstance(habitat, Filter.class, name, filterClassName, ctorArgTypes, ctorArgs);
//    }

    private org.glassfish.grizzly.http.HttpServerFilter createHttpServerCodecFilter() {
        return createHttpServerCodecFilter(null);
    }
    
    private org.glassfish.grizzly.http.HttpServerFilter createHttpServerCodecFilter(
            final Http http) {
        
        int maxRequestHeaders = MimeHeaders.MAX_NUM_HEADERS_DEFAULT;
        int maxResponseHeaders = MimeHeaders.MAX_NUM_HEADERS_DEFAULT;
        boolean isChunkedEnabled = true;
        int headerBufferLengthBytes = org.glassfish.grizzly.http.HttpServerFilter.DEFAULT_MAX_HTTP_PACKET_HEADER_SIZE;
        
        String defaultResponseType = null;
        
        if (http != null) {
            isChunkedEnabled = Boolean.parseBoolean(http.getChunkingEnabled());
            
            headerBufferLengthBytes = Integer.parseInt(http.getHeaderBufferLengthBytes());
            
            defaultResponseType = http.getDefaultResponseType();

            maxRequestHeaders = Integer.parseInt(http.getMaxRequestHeaders());

            maxResponseHeaders = Integer.parseInt(http.getMaxResponseHeaders());
        }
        
        return createHttpServerCodecFilter(http, isChunkedEnabled,
                headerBufferLengthBytes, defaultResponseType,
                configureKeepAlive(http), obtainDelayedExecutor(),
                maxRequestHeaders, maxResponseHeaders);
    }
    
    protected org.glassfish.grizzly.http.HttpServerFilter createHttpServerCodecFilter(
            final Http http,
            final boolean isChunkedEnabled, final int headerBufferLengthBytes,
            final String defaultResponseType, final KeepAlive keepAlive,
            final DelayedExecutor delayedExecutor,
            final int maxRequestHeaders, final int maxResponseHeaders) {
        final org.glassfish.grizzly.http.HttpServerFilter httpCodecFilter =
                new org.glassfish.grizzly.http.HttpServerFilter(
                isChunkedEnabled,
                headerBufferLengthBytes,
                defaultResponseType,
                keepAlive,
                delayedExecutor,
                maxRequestHeaders,
                maxResponseHeaders);
        
        if (http != null) { // could be null for HTTP redirect
            httpCodecFilter.setMaxPayloadRemainderToSkip(
                    Integer.parseInt(http.getMaxSwallowingInputBytes()));
            
            httpCodecFilter.setAllowPayloadForUndefinedHttpMethods(
                    Boolean.parseBoolean(http.getAllowPayloadForUndefinedHttpMethods()));
        }
        
        return httpCodecFilter;
    }
    
    protected ServerFilterConfiguration getHttpServerFilterConfiguration(Http http) {
        final ServerFilterConfiguration serverFilterConfiguration =
                new ServerFilterConfiguration();
        final String scheme = http.getScheme();
        final String schemeMapping = http.getSchemeMapping();
        final String remoteUserMapping = http.getRemoteUserMapping();
        if (scheme != null || schemeMapping != null || remoteUserMapping != null) {
            final BackendConfiguration backendConfiguration = new BackendConfiguration();
            if (schemeMapping == null) {
                backendConfiguration.setScheme(scheme);
            } else {
                backendConfiguration.setSchemeMapping(schemeMapping);
            }
            
            backendConfiguration.setRemoteUserMapping(remoteUserMapping);
            serverFilterConfiguration.setBackendConfiguration(backendConfiguration);
        }
        serverFilterConfiguration.setPassTraceRequest(true);
        serverFilterConfiguration.setTraceEnabled(Boolean.valueOf(http.getTraceEnabled()));
        int maxRequestParameters;
        try {
            maxRequestParameters = Integer.parseInt(http.getMaxRequestParameters());
        } catch (NumberFormatException nfe) {
            maxRequestParameters = Http.MAX_REQUEST_PARAMETERS;
        }
        serverFilterConfiguration.setMaxRequestParameters(maxRequestParameters);
        serverFilterConfiguration.setMaxPostSize(Integer.parseInt(http.getMaxPostSizeBytes()));
        serverFilterConfiguration.setMaxFormPostSize(Integer.parseInt(http.getMaxFormPostSizeBytes()));
        serverFilterConfiguration.setMaxBufferedPostSize(Integer.parseInt(http.getMaxSavePostSizeBytes()));
        return serverFilterConfiguration;
    }

    protected HttpHandler getHttpHandler() {
        return new StaticHttpHandler(".");
    }

    /**
     * Configure the Grizzly HTTP FileCache mechanism
     */
    protected FileCache configureHttpFileCache(org.glassfish.grizzly.config.dom.FileCache cache) {
        final FileCache fileCache = new FileCache();
        if (cache != null) {
            fileCache.setEnabled(GrizzlyConfig.toBoolean(cache.getEnabled()));
            fileCache.setSecondsMaxAge(Integer.parseInt(cache.getMaxAgeSeconds()));
            fileCache.setMaxCacheEntries(Integer.parseInt(cache.getMaxFilesCount()));
            fileCache.setMaxLargeFileCacheSize(Integer.parseInt(cache.getMaxCacheSizeBytes()));
        } else {
            fileCache.setEnabled(false);
        }
        return fileCache;
    }

    protected KeepAlive configureKeepAlive(final Http http) {
        int timeoutInSeconds = 60;
        int maxConnections = 256;
        if (http != null) {
            try {
                timeoutInSeconds = Integer.parseInt(http.getTimeoutSeconds());
            } catch (NumberFormatException ex) {
//                String msg = _rb.getString("pewebcontainer.invalidKeepAliveTimeout");
                String msg = "pewebcontainer.invalidKeepAliveTimeout";
                msg = MessageFormat.format(msg, http.getTimeoutSeconds(), Integer.toString(timeoutInSeconds));
                LOGGER.log(Level.WARNING, msg, ex);
            }
            try {
                maxConnections = Integer.parseInt(http.getMaxConnections());
            } catch (NumberFormatException ex) {
//                String msg = _rb.getString("pewebcontainer.invalidKeepAliveMaxConnections");
                String msg = "pewebcontainer.invalidKeepAliveMaxConnections";
                msg = MessageFormat.format(msg, http.getMaxConnections(), Integer.toString(maxConnections));
                LOGGER.log(Level.WARNING, msg, ex);
            }
        }
        final KeepAlive keepAlive = new KeepAlive();
        keepAlive.setIdleTimeoutInSeconds(timeoutInSeconds);
        keepAlive.setMaxRequestsCount(maxConnections);
        return keepAlive;
    }

    protected Set<ContentEncoding> configureContentEncodings(final Http http) {
        return configureCompressionEncodings(http);
    }

    protected Set<ContentEncoding> configureCompressionEncodings(Http http) {
        final String mode = http.getCompression();
        int compressionMinSize = Integer.parseInt(http.getCompressionMinSizeBytes());
        CompressionLevel compressionLevel;
        try {
            compressionLevel = CompressionLevel.getCompressionLevel(mode);
        } catch (IllegalArgumentException e) {
            try {
                // Try to parse compression as an int, which would give the
                // minimum compression size
                compressionLevel = CompressionLevel.ON;
                compressionMinSize = Integer.parseInt(mode);
            } catch (Exception ignore) {
                compressionLevel = CompressionLevel.OFF;
            }
        }
        final String compressableMimeTypesString = http.getCompressableMimeType();
        final String noCompressionUserAgentsString = http.getNoCompressionUserAgents();
        final String[] compressableMimeTypes = 
                ((compressableMimeTypesString != null) 
                        ? compressableMimeTypesString.split(",") 
                        : new String[0]);
        final String[] noCompressionUserAgents = 
                ((noCompressionUserAgentsString != null) 
                        ? noCompressionUserAgentsString.split(",") 
                        : new String[0]);
        final ContentEncoding gzipContentEncoding = new GZipContentEncoding(
            GZipContentEncoding.DEFAULT_IN_BUFFER_SIZE,
            GZipContentEncoding.DEFAULT_OUT_BUFFER_SIZE,
            new CompressionEncodingFilter(compressionLevel, compressionMinSize,
                compressableMimeTypes,
                noCompressionUserAgents,
                GZipContentEncoding.getGzipAliases()));
        final ContentEncoding lzmaEncoding = new LZMAContentEncoding(new CompressionEncodingFilter(compressionLevel, compressionMinSize,
                compressableMimeTypes,
                noCompressionUserAgents,
                LZMAContentEncoding.getLzmaAliases()));
        final Set<ContentEncoding> set = new HashSet<ContentEncoding>(2);
        set.add(gzipContentEncoding);
        set.add(lzmaEncoding);
        return set;
    }

    @SuppressWarnings("unchecked")
    private static IOStrategy loadIOStrategy(final String classname) {
        Class<? extends IOStrategy> strategy;
        if (classname == null) {
            strategy = WorkerThreadIOStrategy.class;
        } else {
            try {
                strategy = Utils.loadClass(classname);
            } catch (Exception e) {
                strategy = WorkerThreadIOStrategy.class;
            }
        }
        
        try {
            final Method m = strategy.getMethod("getInstance");
            return (IOStrategy) m.invoke(null);
        } catch (Exception e) {
            throw new IllegalStateException("Can not initialize IOStrategy: " + strategy + ". Error: " + e);
        }
    }

    private static Ssl getSsl(Protocol protocol) {
        Ssl ssl = protocol.getSsl();
        if (ssl == null) {
            ssl = (Ssl) DefaultProxy.createDummyProxy(protocol, Ssl.class);
        }
        return ssl;
    }

    private static SelectionKeyHandler getSelectionKeyHandlerByName(final String name,
                                                                    final Transport transportConfig) {
        Transports transports = transportConfig.getParent();
        List<SelectionKeyHandler> handlers = transports.getSelectionKeyHandler();
        if (!handlers.isEmpty()) {
            for (SelectionKeyHandler handler : handlers) {
                if (handler.getName().equals(name)) {
                    return handler;
                }
            }
        }
        return null;
    }
}

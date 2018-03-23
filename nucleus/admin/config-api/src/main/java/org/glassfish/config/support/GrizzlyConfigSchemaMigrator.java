/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.config.support;

import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Configs;
import com.sun.enterprise.config.serverbeans.JavaConfig;
import com.sun.enterprise.config.serverbeans.ThreadPools;
import com.sun.enterprise.config.serverbeans.VirtualServer;
import com.sun.enterprise.config.serverbeans.HttpService;
import com.sun.enterprise.config.util.ConfigApiLoggerInfo;

import org.glassfish.api.admin.config.ConfigurationUpgrade;
import org.glassfish.grizzly.config.dom.FileCache;
import org.glassfish.grizzly.config.dom.Http;
import org.glassfish.grizzly.config.dom.NetworkConfig;
import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.grizzly.config.dom.NetworkListeners;
import org.glassfish.grizzly.config.dom.Protocol;
import org.glassfish.grizzly.config.dom.Protocols;
import org.glassfish.grizzly.config.dom.Ssl;
import org.glassfish.grizzly.config.dom.ThreadPool;
import org.glassfish.grizzly.config.dom.Transports;
import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;
import org.jvnet.hk2.config.types.Property;

import javax.inject.Inject;

@SuppressWarnings({"deprecation"})
@Service(name="grizzlyconfigupgrade")
public class GrizzlyConfigSchemaMigrator implements ConfigurationUpgrade, PostConstruct {
    private final static String SSL_CONFIGURATION_WANTAUTH = "org.glassfish.grizzly.ssl.auth";
    private final static String SSL_CONFIGURATION_SSLIMPL = "org.glassfish.grizzly.ssl.sslImplementation";
    @Inject
    private Configs configs;
    private Config currentConfig = null;
    @Inject
    private ServiceLocator habitat;
    private static final String HTTP_THREAD_POOL = "http-thread-pool";
    private static final String ASADMIN_LISTENER = "admin-listener";
    private static final String ASADMIN_VIRTUAL_SERVER = "__asadmin";

    static final Logger logger = ConfigApiLoggerInfo.getLogger();
    
    public void postConstruct() {
        for (Config config : configs.getConfig()) {
            currentConfig = config;
            try {
                final NetworkConfig networkConfig = currentConfig.getNetworkConfig();
                if (networkConfig == null) {
                    createFromScratch();
                }
                normalizeThreadPools();
                if (currentConfig.getHttpService() != null) {
                    promoteHttpServiceProperties(
                        currentConfig.getHttpService());
                    promoteVirtualServerProperties(
                        currentConfig.getHttpService());
                } else {
                    // this only happens during some unit tests
                    logger.log(Level.WARNING, ConfigApiLoggerInfo.nullHttpService,
                            new String[] { currentConfig.getName() });
                }
                promoteSystemProperties();
                addAsadminProtocol(currentConfig.getNetworkConfig());
            } catch (TransactionFailure tf) {
                logger.log(Level.SEVERE, ConfigApiLoggerInfo.failUpgradeDomain, tf);
                throw new RuntimeException(tf);
            }
        }
    }

    private void addAsadminProtocol(NetworkConfig config) throws TransactionFailure {
        ensureAdminThreadPool();
        final Protocols protocols = getProtocols(config);
        Protocol adminProtocol = protocols.findProtocol(ASADMIN_LISTENER);
        if (adminProtocol == null) {
            adminProtocol = (Protocol) ConfigSupport.apply(new SingleConfigCode<Protocols>() {
                public Object run(Protocols param) throws TransactionFailure {
                    final Protocol protocol = param.createChild(Protocol.class);
                    param.getProtocol().add(protocol);
                    protocol.setName(ASADMIN_LISTENER);
                    Http http = protocol.createChild(Http.class);
                    http.setFileCache(http.createChild(FileCache.class));
                    protocol.setHttp(http);
                    http.setDefaultVirtualServer(ASADMIN_VIRTUAL_SERVER);
                    http.setMaxConnections("250");
                    return protocol;
                }
            }, protocols);
        }
        for (NetworkListener listener : adminProtocol.findNetworkListeners()) {
            ConfigSupport.apply(new SingleConfigCode<NetworkListener>() {
                @Override
                public Object run(NetworkListener param) {
                    param.setThreadPool("admin-thread-pool");
                    return null;
                }
            }, listener);
        }

    }

    private void ensureAdminThreadPool() throws TransactionFailure {
        final ThreadPools threadPools = currentConfig.getThreadPools();
        boolean adminThreadPoolFound = false;
        for (ThreadPool pool : threadPools.getThreadPool()) {
            adminThreadPoolFound |= "admin-thread-pool".equals(pool.getName());
        }
        if (!adminThreadPoolFound) {
            ConfigSupport.apply(new SingleConfigCode<ThreadPools>() {
                @Override
                public Object run(ThreadPools param) throws PropertyVetoException, TransactionFailure {
                    final ThreadPool pool = param.createChild(ThreadPool.class);
                    param.getThreadPool().add(pool);
                    pool.setName("admin-thread-pool");
                    pool.setMaxThreadPoolSize("50");
                    pool.setMaxQueueSize("256");
                    return null;
                }
            }, threadPools);
        }
    }

    private void createFromScratch() throws TransactionFailure {
        normalizeThreadPools();
        getNetworkConfig();
    }

    private ThreadPools createThreadPools() throws TransactionFailure {
        return (ThreadPools) ConfigSupport.apply(new SingleConfigCode<Config>() {
            public Object run(Config param) throws PropertyVetoException, TransactionFailure {
                final ThreadPools threadPools = param.createChild(ThreadPools.class);
                param.setThreadPools(threadPools);
                return threadPools;
            }
        }, currentConfig);
    }

    private NetworkConfig getNetworkConfig() throws TransactionFailure {
        NetworkConfig config = currentConfig.getNetworkConfig();
        if (config == null) {
            config = (NetworkConfig) ConfigSupport.apply(new SingleConfigCode<Config>() {
                public Object run(Config param) throws PropertyVetoException, TransactionFailure {
                    final NetworkConfig netConfig = param.createChild(NetworkConfig.class);
                    netConfig.setProtocols(netConfig.createChild(Protocols.class));
                    netConfig.setNetworkListeners(netConfig.createChild(NetworkListeners.class));
                    netConfig.setTransports(netConfig.createChild(Transports.class));
                    param.setNetworkConfig(netConfig);
                    return netConfig;
                }
            }, currentConfig);
        }
        return config;
    }

    public static Protocols getProtocols(NetworkConfig config) throws TransactionFailure {
        Protocols protocols = config.getProtocols();
        if (protocols == null) {
            protocols = (Protocols) ConfigSupport.apply(new SingleConfigCode<NetworkConfig>() {
                public Object run(NetworkConfig param) throws TransactionFailure {
                    final Protocols child = param.createChild(Protocols.class);
                    param.setProtocols(child);
                    return child;
                }
            }, config);
        }
        return protocols;
    }

    private void migrateThreadPools(ThreadPools threadPools) throws TransactionFailure {
        final Config config = threadPools.getParent(Config.class);
        final NetworkListeners networkListeners = config.getNetworkConfig().getNetworkListeners();
        threadPools.getThreadPool().addAll(networkListeners.getThreadPool());
        ConfigSupport.apply(new SingleConfigCode<NetworkListeners>() {
            public Object run(NetworkListeners param) {
                param.getThreadPool().clear();
                return null;
            }
        }, networkListeners);
    }

    private void normalizeThreadPools() throws TransactionFailure {
        ThreadPools threadPools = currentConfig.getThreadPools();
        if (threadPools == null) {
            threadPools = createThreadPools();
        } else {
            final List<ThreadPool> list = threadPools.getThreadPool();
            boolean httpListenerFound = false;
            for (ThreadPool pool : list) {
                httpListenerFound |=
                    HTTP_THREAD_POOL.equals(pool.getThreadPoolId()) || HTTP_THREAD_POOL.equals(pool.getName());
                if (pool.getName() == null) {
                    ConfigSupport.apply(new SingleConfigCode<ThreadPool>() {
                        public Object run(ThreadPool param) {
                            param.setName(param.getThreadPoolId());
                            param.setThreadPoolId(null);
                            if (param.getMinThreadPoolSize() == null
                                || Integer.parseInt(param.getMinThreadPoolSize()) < 2) {
                                param.setMinThreadPoolSize("2");
                            }
                            return null;
                        }
                    }, pool);
                }
            }
            if (!httpListenerFound) {
                ConfigSupport.apply(new SingleConfigCode<ThreadPools>() {
                    public Object run(ThreadPools param) throws TransactionFailure {
                        final ThreadPool pool = param.createChild(ThreadPool.class);
                        pool.setName(HTTP_THREAD_POOL);
                        param.getThreadPool().add(pool);
                        return null;
                    }
                }, threadPools);
            }
        }
        final NetworkConfig networkConfig = currentConfig.getNetworkConfig();
        if (networkConfig != null) {
            final NetworkListeners networkListeners = networkConfig.getNetworkListeners();
            if (networkListeners != null) {
                if (networkListeners.getThreadPool() != null && !networkListeners.getThreadPool().isEmpty()) {
                    ConfigSupport.apply(new SingleConfigCode<ThreadPools>() {
                        public Object run(ThreadPools param) throws TransactionFailure {
                            migrateThreadPools(param);
                            return null;
                        }
                    }, threadPools);
                }
            }
        }
    }

    private void promoteHttpServiceProperties(HttpService service) throws TransactionFailure {
        ConfigSupport.apply(new SingleConfigCode<HttpService>() {
            @Override
            public Object run(HttpService param) {
                final List<Property> propertyList = new ArrayList<Property>(param.getProperty());
                final Iterator<Property> it = propertyList.iterator();
                while (it.hasNext()) {
                    final Property property = it.next();
                    if ("accessLoggingEnabled".equals(property.getName())) {
                        param.setAccessLoggingEnabled(property.getValue());
                        it.remove();
                    } else if ("accessLogBufferSize".equals(property.getName())) {
                        param.getAccessLog().setBufferSizeBytes(property.getValue());
                        it.remove();
                    } else if ("accessLogWriterInterval".equals(property.getName())) {
                        param.getAccessLog().setWriteIntervalSeconds(property.getValue());
                        it.remove();
                    } else if ("sso-enabled".equals(property.getName())) {
                        param.setSsoEnabled(property.getValue());
                        it.remove();
                    }
                }
                param.getProperty().clear();
                param.getProperty().addAll(propertyList);
                return null;
            }
        }, service);

    }

    private void promoteSystemProperties() throws TransactionFailure {
        ConfigSupport.apply(new SingleConfigCode<JavaConfig>() {
            @Override
            public Object run(JavaConfig param) throws PropertyVetoException, TransactionFailure {
                final List<String> props = new ArrayList<String>(param.getJvmOptions());
                final Iterator<String> iterator = props.iterator();
                while (iterator.hasNext()) {
                    String prop = iterator.next();
                    if (prop.startsWith("-D")) {
                        final String[] parts = prop.split("=");
                        String name = parts[0].substring(2);
                        if (SSL_CONFIGURATION_WANTAUTH.equals(name) || SSL_CONFIGURATION_SSLIMPL.equals(name)) {
                            iterator.remove();
                            updateSsl(name, parts[1]);
                        }
                        if ("com.sun.grizzly.maxTransactionTimeout".equals(name)) {
                            iterator.remove();
                            updateHttp(parts[1]);
                        }
                    }
                }
                param.setJvmOptions(props);
                return param;
            }
        }, habitat.<JavaConfig>getService(JavaConfig.class));
    }

    private void promoteVirtualServerProperties(HttpService service) throws TransactionFailure {
        for (VirtualServer virtualServer : service.getVirtualServer()) {
            ConfigSupport.apply(new SingleConfigCode<VirtualServer>() {
                @Override
                public Object run(VirtualServer param) throws PropertyVetoException {
                    if (param.getHttpListeners() != null && !"".equals(param.getHttpListeners())) {
                        param.setNetworkListeners(param.getHttpListeners());
                    }
                    param.setHttpListeners(null);
                    final List<Property> propertyList = new ArrayList<Property>(param.getProperty());
                    final Iterator<Property> it = propertyList.iterator();
                    while (it.hasNext()) {
                        final Property property = it.next();
                        if ("docroot".equals(property.getName())) {
                            param.setDocroot(property.getValue());
                            it.remove();
                        } else if ("accesslog".equals(property.getName())) {
                            param.setAccessLog(property.getValue());
                            it.remove();
                        } else if ("sso-enabled".equals(property.getName())) {
                            param.setSsoEnabled(property.getValue());
                            it.remove();
                        }
                    }
                    param.getProperty().clear();
                    param.getProperty().addAll(propertyList);
                    return null;
                }
            }, virtualServer);
        }
    }

    private void updateHttp(final String maxTransactionTimeout) throws TransactionFailure {
        for (Protocol protocol : currentConfig.getNetworkConfig().getProtocols().getProtocol()) {
            final Http http = protocol.getHttp();
            if (http != null) {
                ConfigSupport.apply(new SingleConfigCode<Http>() {
                    @Override
                    public Object run(Http param) {
                        if (param != null) {
                            param.setRequestTimeoutSeconds(maxTransactionTimeout);
                        }
                        return null;
                    }
                }, http);
            }
        }
    }

    private void updateSsl(final String propName, final String value) throws TransactionFailure {
        final Collection<Protocol> protocols = habitat.getAllServices(Protocol.class);
        for (Protocol protocol : protocols) {
            final Ssl ssl = protocol.getSsl();
            if (ssl != null) {
                ConfigSupport.apply(new SingleConfigCode<Ssl>() {
                    @Override
                    public Object run(Ssl param) {
                        if (SSL_CONFIGURATION_WANTAUTH.equals(propName)) {
                            param.setClientAuth(value);
                        } else if (SSL_CONFIGURATION_SSLIMPL.equals(propName)) {
                            param.setClassname(value);
                        }
                        return param;
                    }
                }, ssl);
            }
        }
    }

}

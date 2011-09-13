/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.kernel.javaee;

import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.module.Module;
import com.sun.enterprise.module.ModulesRegistry;
import com.sun.enterprise.v3.server.ContainerStarter;
import org.glassfish.grizzly.config.dom.*;
import com.sun.logging.LogDomains;
import org.glassfish.api.Startup;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.container.Sniffer;
import org.glassfish.internal.data.ContainerRegistry;
import org.glassfish.internal.data.EngineInfo;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.component.PostConstruct;
import org.jvnet.hk2.component.Singleton;
import org.jvnet.hk2.config.*;
import org.jvnet.hk2.config.types.Property;

import java.beans.PropertyChangeEvent;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Startup service for the web container.
 *
 * This service checks if any domain.xml configuration, or changes in
 * such configuration, that can be handled only by the web container
 * (e.g., access logging) have been specified, and if so, starts the
 * web container (unless already started).
 *
 * @author jluehe
 */
@Service
@Scoped(Singleton.class)
public class WebContainerStarter
        implements Startup, PostConstruct, ConfigListener {

    private static final Logger logger = LogDomains.getLogger(
        WebContainerStarter.class, LogDomains.CORE_LOGGER);
    private static final ResourceBundle rb = logger.getResourceBundle();

    private static final String AUTH_PASSTHROUGH_ENABLED_PROP =
        "authPassthroughEnabled";

    private static final String PROXY_HANDLER_PROP = "proxyHandler";

    private static final String TRACE_ENABLED_PROP = "traceEnabled";

    private Domain domain;

    @Inject
    private ContainerRegistry containerRegistry;

    @Inject
    private ContainerStarter containerStarter;

    @Inject
    private ModulesRegistry modulesRegistry;

    private Config serverConfig;

    @Inject
    private Habitat habitat;


    /**
     * Scans the domain.xml to see if it specifies any configuration
     * that can be handled only by the web container, and if so, starts
     * the web container
     */ 
    public void postConstruct() {
        domain = habitat.getComponent(Domain.class);
        serverConfig = habitat.getComponent(Config.class, ServerEnvironment.DEFAULT_INSTANCE_NAME);

        boolean isStartNeeded = false;
        if (serverConfig != null) {
            if (isStartNeeded(serverConfig.getHttpService())) {
                isStartNeeded = true;
            }
            if (!isStartNeeded && isStartNeeded(serverConfig.getNetworkConfig())) {
                isStartNeeded = true;
            }
        }

        if (isStartNeeded) {
            startWebContainer();
        } else {
            ObservableBean bean = (ObservableBean) ConfigSupport.getImpl(serverConfig.getHttpService());
            bean.addListener(this);
            bean = (ObservableBean) ConfigSupport.getImpl(serverConfig.getNetworkConfig().getNetworkListeners());
            bean.addListener(this);
        }
    }

    public Lifecycle getLifecycle() {
        // This service stays running for the life of the app server,
        // hence SERVER
        return Lifecycle.SERVER;
    }

    public UnprocessedChangeEvents changed(PropertyChangeEvent[] events) {
        return ConfigSupport.sortAndDispatch(events, new Changed() {
            public <T extends ConfigBeanProxy> NotProcessed changed(
                    TYPE type, Class<T> tClass, T t) {
                if (tClass == HttpService.class) {
                    if (type == TYPE.CHANGE) {
                        if (isStartNeeded((HttpService) t)) {
                            startWebContainer();
                        }
                    }
                } else if (tClass == VirtualServer.class) {
                    if (type == TYPE.ADD || type == TYPE.CHANGE) {
                        if (isStartNeeded((VirtualServer) t)) {
                            startWebContainer();
                        }
                    }
                } else if (tClass == NetworkListener.class) {
                    if (type == TYPE.ADD || type == TYPE.CHANGE) {
                        if (isStartNeeded((NetworkListener) t)) {
                            startWebContainer();
                        }
                    }
                }
                return null;
            }
        }
        , logger);
    }

    /**
     * Starts the web container
     */
    private void startWebContainer() {
        Sniffer webSniffer = habitat.getComponent(Sniffer.class,"web");
        if (webSniffer==null) {
            if (logger.isLoggable(Level.INFO)) {
                logger.info("core.web_container_not_installed");
            }
            return;
        }
        
        if (containerRegistry.getContainer(
                    webSniffer.getContainersNames()[0]) != null) {
            containerRegistry.getContainer(
                    webSniffer.getContainersNames()[0]).getContainer();
        } else {
            Module snifferModule = modulesRegistry.find(webSniffer.getClass());
            try {
                Collection<EngineInfo> containersInfo =
                    containerStarter.startContainer(webSniffer, snifferModule);
                if (containersInfo != null && !containersInfo.isEmpty()) {
                    // Start each container
                    for (EngineInfo info : containersInfo) {
                        info.getContainer();
                        if (logger.isLoggable(Level.INFO)) {
                            logger.log(Level.INFO, "core.start_container_done", 
                                webSniffer.getModuleType());
                        }
                    }
                } else {
                    logger.severe("core.unable_start_container_no_exception");
                }
            } catch (Exception e) {
                String msg = MessageFormat.format(
                        rb.getString("core.unable_start_container"),
                        webSniffer.getContainersNames()[0]);
                logger.log(Level.SEVERE, msg, e);
            }
        }
    }

    /*
     * @return true if the given HttpService contains any configuration
     * that can be handled only by the web container and therefore requires
     * the web container to be started, false otherwise
     */
    private boolean isStartNeeded(HttpService httpService) {
        if (httpService == null) {
            return false;
        }

        if (ConfigBeansUtilities.toBoolean(
                    httpService.getAccessLoggingEnabled()) ||
                ConfigBeansUtilities.toBoolean(
                    httpService.getSsoEnabled())) {
            return true;
        }

        List<Property> props = httpService.getProperty();
        if (props != null) {
            for (Property prop : props) {
                String propName = prop.getName();
                String propValue = prop.getValue();
                if (AUTH_PASSTHROUGH_ENABLED_PROP.equals(propName)) {
                    if (ConfigBeansUtilities.toBoolean(propValue)) {
                        return true;
                    }
                } else if (PROXY_HANDLER_PROP.equals(propName)) {
                    return true;
                } else if (TRACE_ENABLED_PROP.equals(propName)) {
                    if (!ConfigBeansUtilities.toBoolean(propValue)) {
                        return true;
                    }
                }
            }
        }

        List<VirtualServer> hosts = httpService.getVirtualServer();
        if (hosts != null) {
            for (VirtualServer host : hosts) {
                if (isStartNeeded(host)) {
                    return true;
                }
            }
        }

        return false;
    }

    /*
     * @return true if the given VirtualServer contains any configuration
     * that can be handled only by the web container and therefore requires
     * the web container to be started, false otherwise
     */
    private boolean isStartNeeded(VirtualServer host) {
        if (host == null) {
            return false;
        }

        if (ConfigBeansUtilities.toBoolean(host.getAccessLoggingEnabled()) ||
                ConfigBeansUtilities.toBoolean(host.getSsoEnabled())) {
            return true;
        }

        String state = host.getState();
        if (state != null &&
                ("disabled".equals(state) ||
                    !ConfigBeansUtilities.toBoolean(state))) {
            return true;
        }
     
        List<Property> props = host.getProperty();
        if (props != null && !props.isEmpty()) {
            return true;
        }

        return false;
    }

    /*
     * @return true if the given NetworkConfig contains any configuration
     * that can be handled only by the web container and therefore requires
     * the web container to be started, false otherwise
     */
    private boolean isStartNeeded(NetworkConfig networkConfig) {
        if (networkConfig == null) {
            return false;
        }

        NetworkListeners networkListeners = networkConfig.getNetworkListeners();
        if (networkListeners == null) {
            return false;
        }

        for (NetworkListener networkListener : networkListeners.getNetworkListener()) {
            if (isStartNeeded(networkListener)) {
                return true;
            }
        }

        return false;
    }

    /*
     * @return true if the given NetworkListener contains any configuration
     * that can be handled only by the web container and therefore requires
     * the web container to be started, false otherwise
     */
    private boolean isStartNeeded(NetworkListener networkListener) {
        if (networkListener == null) {
            return false;
        }

        return ConfigBeansUtilities.toBoolean(networkListener.getJkEnabled());
    }
}

/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2012 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.ConfigBeansUtilities;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.HttpService;
import com.sun.enterprise.config.serverbeans.VirtualServer;
import com.sun.enterprise.module.ModulesRegistry;
import com.sun.enterprise.v3.server.ContainerStarter;
import java.beans.PropertyChangeEvent;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.container.Sniffer;
import org.glassfish.grizzly.config.dom.NetworkConfig;
import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.grizzly.config.dom.NetworkListeners;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.runlevel.RunLevel;
import org.glassfish.internal.data.ContainerRegistry;
import org.glassfish.internal.data.EngineInfo;
import org.glassfish.logging.annotation.LogMessageInfo;
import org.glassfish.logging.annotation.LogMessagesResourceBundle;
import org.glassfish.logging.annotation.LoggerInfo;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.Changed;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigListener;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.NotProcessed;
import org.jvnet.hk2.config.ObservableBean;
import org.jvnet.hk2.config.UnprocessedChangeEvents;
import org.jvnet.hk2.config.types.Property;

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
@RunLevel(StartupRunLevel.VAL)
public class WebContainerStarter
        implements PostConstruct, ConfigListener {
   
    private static final String LOGMSG_PREFIX = "AS-CORE-JAVAEE";
    
    @LogMessagesResourceBundle
    private static final String SHARED_LOGMESSAGE_RESOURCE = "org.glassfish.kernel.javaee.LogMessages";
    
    @LoggerInfo(subsystem = "AS-CORE", description = "Java EE Core Kernel", publish = true)
    private static final String ASCORE_LOGGER = "javax.enterprise.system.core.ee";
    private static final Logger logger = Logger.getLogger(
                ASCORE_LOGGER, SHARED_LOGMESSAGE_RESOURCE);
    private static final ResourceBundle rb = logger.getResourceBundle();

    @LogMessageInfo(
            message = "Web Container not installed",
            cause = "The web container does not install properly.",
            action = "Please check the web container libraries are installed properly.",
            level = "INFO")
    public static final String mWebContainerNotInstalled = LOGMSG_PREFIX + "-0001";

    @LogMessageInfo(
            message = "Done with starting {0} container.",
            level = "INFO")
    public static final String mStartContainerDone = LOGMSG_PREFIX + "-0002";
    
    @LogMessageInfo(
            message = "Unable to start container (no exception provided)",
            cause = "The web container does not start properly.",
            action = "Please check the web container libraries are installed properly.",
            level = "SEVERE")
    public static final String mUnableStartContainerNoException = LOGMSG_PREFIX + "-0003";

    @LogMessageInfo(
            message = "Unable to start container {0}",
            cause = "The web container does not start properly. Most probably, there is a class loading issue.",
            action = "Please resolve issues mentioned in the stack trace.",
            level = "SEVERE")
    public static final String mUnableStartContainer = LOGMSG_PREFIX + "-0004";

    private static final String AUTH_PASSTHROUGH_ENABLED_PROP =
        "authPassthroughEnabled";

    private static final String PROXY_HANDLER_PROP = "proxyHandler";

    private static final String TRACE_ENABLED_PROP = "traceEnabled";

    @Inject
    private Provider<Domain> domainProvider;

    @Inject
    private ContainerRegistry containerRegistry;

    @Inject
    private ContainerStarter containerStarter;

    @Inject
    private ModulesRegistry modulesRegistry;

    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    private Provider<Config> serverConfigProvider;

    @Inject @Named("web")
    private Provider<Sniffer> webSnifferProvider;

    /**
     * Scans the domain.xml to see if it specifies any configuration
     * that can be handled only by the web container, and if so, starts
     * the web container
     */ 
    public void postConstruct() {
        domainProvider.get();
        Config serverConfig = serverConfigProvider.get();

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
        Sniffer webSniffer = webSnifferProvider.get();
        if (webSniffer==null) {
            if (logger.isLoggable(Level.INFO)) {
                logger.info(mWebContainerNotInstalled);
            }
            return;
        }
        
        if (containerRegistry.getContainer(
                    webSniffer.getContainersNames()[0]) != null) {
            containerRegistry.getContainer(
                    webSniffer.getContainersNames()[0]).getContainer();
        } else {
            try {
                Collection<EngineInfo> containersInfo =
                    containerStarter.startContainer(webSniffer);
                if (containersInfo != null && !containersInfo.isEmpty()) {
                    // Start each container
                    for (EngineInfo info : containersInfo) {
                        info.getContainer();
                        if (logger.isLoggable(Level.INFO)) {
                            logger.log(Level.INFO, mStartContainerDone, 
                                webSniffer.getModuleType());
                        }
                    }
                } else {
                    logger.severe(mUnableStartContainerNoException);
                }
            } catch (Exception e) {
                String msg;
                if ( rb != null ) {
                    msg = MessageFormat.format( rb.getString(mUnableStartContainer), webSniffer.getContainersNames()[0]);
                } else {
                    msg = "Unable to start Web Container: " + webSniffer.getContainersNames()[0];
                }
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

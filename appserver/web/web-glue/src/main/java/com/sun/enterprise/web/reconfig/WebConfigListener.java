/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2016 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.web.reconfig;

import com.sun.enterprise.config.serverbeans.AccessLog;
import com.sun.enterprise.config.serverbeans.AvailabilityService;
import com.sun.enterprise.config.serverbeans.HttpService;
import com.sun.enterprise.config.serverbeans.JavaConfig;
import com.sun.enterprise.config.serverbeans.SystemProperty;
import com.sun.enterprise.config.serverbeans.VirtualServer;
import com.sun.enterprise.v3.services.impl.MapperUpdateListener;
import com.sun.enterprise.web.WebContainer;
import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.web.LogFacade;
import org.glassfish.web.config.serverbeans.ManagerProperties;
import org.glassfish.web.config.serverbeans.WebContainerAvailability;
import org.apache.catalina.LifecycleException;
import javax.inject.Inject;
import org.jvnet.hk2.annotations.Optional;
import javax.inject.Named;
import org.jvnet.hk2.config.*;
import org.jvnet.hk2.config.types.Property;

import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.grizzly.config.dom.NetworkConfig;
import org.glassfish.grizzly.config.dom.NetworkListeners;
import org.glassfish.grizzly.http.server.util.Mapper;


/**
 * Web container dynamic configuration handler
 *
 * @author amyroh
 */
public class WebConfigListener implements ConfigListener, MapperUpdateListener {

    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    public HttpService httpService;
    
    @Inject @Optional
    public ManagerProperties managerProperties;

    @Inject @Optional
    public List<Property> property;
    
    private WebContainer container;

    private Logger logger;

    private NetworkConfig networkConfig;
    
    /**
     * Set the Web Container for this ConfigListener.
     * Must be set in order to perform dynamic configuration
     * @param container the container to be set
     */
    public void setContainer(WebContainer container) {
        this.container = container;
    }

    public void setLogger(Logger logger) {
        synchronized (this) {
            this.logger = logger;
        }
    }

    public void setNetworkConfig(NetworkConfig config) {
        this.networkConfig = config;
    }

    /**
     * Handles HttpService change events
     * @param events the PropertyChangeEvent
     */
    @Override
    public synchronized UnprocessedChangeEvents changed(PropertyChangeEvent[] events) {
        return ConfigSupport.sortAndDispatch(events, new Changed() {         
            @Override
            public <T extends ConfigBeanProxy> NotProcessed changed(TYPE type, Class<T> tClass, T t) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, LogFacade.CHANGE_INVOKED, new Object[] {type, tClass, t});
                }
                try {
                    if (tClass == HttpService.class) {
                        container.updateHttpService((HttpService) t);
                    } else if (tClass == NetworkListener.class) {
                        if (type==TYPE.ADD) {
                            container.addConnector((NetworkListener) t, httpService, true);
                        } else if (type==TYPE.REMOVE) {
                            container.deleteConnector((NetworkListener) t);
                        } else if (type==TYPE.CHANGE) {
                            container.updateConnector((NetworkListener) t, httpService);
                        }
                    } else if (tClass == VirtualServer.class) {
                        if (type==TYPE.ADD) {
                            container.createHost((VirtualServer) t, httpService, null);
                            container.loadDefaultWebModule((VirtualServer) t);
                        } else if (type==TYPE.REMOVE) {
                            container.deleteHost(httpService);
                        } else if (type==TYPE.CHANGE) {
                            container.updateHost((VirtualServer)t);
                        }
                    } else if (tClass == AccessLog.class) {
                        container.updateAccessLog(httpService);
                    } else if (tClass == ManagerProperties.class) {
                        return new NotProcessed("ManagerProperties requires restart");
                    } else if (tClass == WebContainerAvailability.class ||
                            tClass == AvailabilityService.class) {
                        // container.updateHttpService handles SingleSignOn valve configuration
                        container.updateHttpService(httpService);
                    } else if (tClass == NetworkListeners.class) {
                        // skip updates
                    } else if (tClass == Property.class) {
                        ConfigBeanProxy config = ((Property)t).getParent();
                        if (config instanceof HttpService) {
                            container.updateHttpService((HttpService)config);
                        } else if (config instanceof VirtualServer) {
                            container.updateHost((VirtualServer)config);
                        } else if (config instanceof NetworkListener) {
                            container.updateConnector((NetworkListener)config, httpService);
                        } else {
                            container.updateHttpService(httpService);
                        }
                    } else if (tClass == SystemProperty.class) {
                        if (((SystemProperty)t).getName().endsWith("LISTENER_PORT")) {
                            for (NetworkListener listener : networkConfig.getNetworkListeners().getNetworkListener()) {
                                if (listener.getPort().equals(((SystemProperty)t).getValue())) {
                                    container.updateConnector(listener, httpService);
                                }
                            }
                        }
                    } else if (tClass == JavaConfig.class) {
                        JavaConfig jc = (JavaConfig) t;
                        final List<String> jvmOptions = new ArrayList<String>(jc.getJvmOptions());
                        for (String jvmOption : jvmOptions) {
                            if (jvmOption.startsWith("-DjvmRoute=")) {
                                container.updateJvmRoute(httpService, jvmOption);
                            }
                        }
                    } else {
                        // Ignore other unrelated events
                    }
                } catch (LifecycleException le) {
                    logger.log(Level.SEVERE, LogFacade.EXCEPTION_WEB_CONFIG, le);
                }
                return null;
            }
        }
        , logger);
    }

    @Override
    public void update(HttpService httpService, NetworkListener httpListener,
            Mapper mapper) {
        container.updateMapper(httpService, httpListener, mapper);
    }
}

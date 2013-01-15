/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.webservices.transport.tcp;

import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.istack.NotNull;
import com.sun.istack.Nullable;
import com.sun.enterprise.deployment.WebServiceEndpoint;
import com.sun.enterprise.web.WebApplication;
import com.sun.enterprise.web.WebModule;
import com.sun.xml.ws.transport.tcp.resources.MessagesMessages;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.data.ApplicationRegistry;
import org.glassfish.internal.data.EngineRef;
import org.glassfish.internal.data.ModuleInfo;
import org.glassfish.webservices.EjbRuntimeEndpointInfo;
import org.glassfish.webservices.monitoring.Endpoint;
import org.glassfish.webservices.monitoring.WebServiceEngine;
import org.glassfish.webservices.monitoring.WebServiceEngineImpl;

/**
 * @author Alexey Stashok
 */
public final class AppServRegistry {
    private static final Logger logger = Logger.getLogger(
            com.sun.xml.ws.transport.tcp.util.TCPConstants.LoggingDomain + ".server");

    private static final AppServRegistry instance = new AppServRegistry();
    
    public static AppServRegistry getInstance() {
        return instance;
    }

    private AppServRegistry() {
        final WSEndpointLifeCycleListener lifecycleListener = new WSEndpointLifeCycleListener();

        final WebServiceEngine engine = WebServiceEngineImpl.getInstance();
        engine.addLifecycleListener(lifecycleListener);

        populateEndpoints(engine);
    }

    /**
     * Populate currently registered WS Endpoints and register them
     */
    private void populateEndpoints(@NotNull final WebServiceEngine engine) {
        final Iterator<Endpoint> endpoints = engine.getEndpoints();
        while(endpoints.hasNext()) {
            registerEndpoint(endpoints.next());
        }
    }

    /**
     * Method is used by WS invoker to clear some EJB invoker state ???
     */
    public @NotNull EjbRuntimeEndpointInfo getEjbRuntimeEndpointInfo(@NotNull final String wsPath) {

        final WSEndpointDescriptor wsEndpointDescriptor =
                WSTCPAdapterRegistryImpl.getInstance().lookupEndpoint(wsPath);
        EjbRuntimeEndpointInfo endpointInfo = null;

        if (wsEndpointDescriptor.isEJB()) {
            endpointInfo = (EjbRuntimeEndpointInfo) V3Module.getWSEjbEndpointRegistry().
                    getEjbWebServiceEndpoint(wsEndpointDescriptor.getURI(), "POST", null);
        }

        return endpointInfo;
    }

    /**
     * Register new WS Endpoint
     */
    protected void registerEndpoint(@NotNull final Endpoint endpoint) {
        final WebServiceEndpoint wsServiceDescriptor = endpoint.getDescriptor();

        if(wsServiceDescriptor != null && isTCPEnabled(wsServiceDescriptor)) {
            final String contextRoot = getEndpointContextRoot(wsServiceDescriptor);
            final String urlPattern = getEndpointUrlPattern(wsServiceDescriptor);
            final String wsPath = getWebServiceEndpointPath(wsServiceDescriptor);

            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, MessagesMessages.WSTCP_1110_APP_SERV_REG_REGISTER_ENDPOINT(
                        wsServiceDescriptor.getServiceName(), wsPath, wsServiceDescriptor.implementedByEjbComponent()));
            }
            final WSEndpointDescriptor descriptor = new WSEndpointDescriptor(wsServiceDescriptor,
                    contextRoot,
                    urlPattern,
                    endpoint.getEndpointSelector());
            WSTCPAdapterRegistryImpl.getInstance().registerEndpoint(wsPath, descriptor);
        }
    }

    /**
     * Deregister WS Endpoint
     */
    protected void deregisterEndpoint(@NotNull final Endpoint endpoint) {
        final WebServiceEndpoint wsServiceDescriptor = endpoint.getDescriptor();
        final String wsPath = getWebServiceEndpointPath(wsServiceDescriptor);

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, MessagesMessages.WSTCP_1111_APP_SERV_REG_DEREGISTER_ENDPOINT(
                    wsServiceDescriptor.getWebService().getName(),
                    wsPath, wsServiceDescriptor.implementedByEjbComponent()));
        }
        WSTCPAdapterRegistryImpl.getInstance().deregisterEndpoint(wsPath);
    }

    private @NotNull String getWebServiceEndpointPath(@NotNull final WebServiceEndpoint wsServiceDescriptor) {
        String wsPath;
        if(!wsServiceDescriptor.implementedByEjbComponent()) {
            String contextRoot = wsServiceDescriptor.getWebComponentImpl().
                    getWebBundleDescriptor().getContextRoot();
            String urlPattern = wsServiceDescriptor.getEndpointAddressUri();
            wsPath = contextRoot + ensureSlash(urlPattern);
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, MessagesMessages.WSTCP_1116_APP_SERV_REG_GET_WS_ENDP_PATH_NON_EJB(wsPath));
            }
        } else {
            wsPath = wsServiceDescriptor.getEndpointAddressUri();
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, MessagesMessages.WSTCP_1117_APP_SERV_REG_GET_WS_ENDP_PATH_EJB(wsPath));
            }
        }

        return ensureSlash(wsPath);
    }

    private @NotNull String getEndpointContextRoot(@NotNull final WebServiceEndpoint wsServiceDescriptor) {
        String contextRoot;
        if(!wsServiceDescriptor.implementedByEjbComponent()) {
            contextRoot = wsServiceDescriptor.getWebComponentImpl().
                    getWebBundleDescriptor().getContextRoot();
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, MessagesMessages.WSTCP_1112_APP_SERV_REG_GET_ENDP_CR_NON_EJB(contextRoot));
            }
        } else {
            final String[] path = wsServiceDescriptor.getEndpointAddressUri().split("/");
            contextRoot = "/" + path[1];
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, MessagesMessages.WSTCP_1113_APP_SERV_REG_GET_ENDP_CR_EJB(contextRoot));
            }
        }

        return contextRoot;
    }

    private @NotNull String getEndpointUrlPattern(@NotNull final WebServiceEndpoint wsServiceDescriptor) {
        String urlPattern;
        if(!wsServiceDescriptor.implementedByEjbComponent()) {
            urlPattern = wsServiceDescriptor.getEndpointAddressUri();
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, MessagesMessages.WSTCP_1114_APP_SERV_REG_GET_ENDP_URL_PATTERN_NON_EJB(urlPattern));
            }
        } else {
            final String[] path = wsServiceDescriptor.getEndpointAddressUri().split("/");
            if (path.length < 3) {
                return "";
            }

            urlPattern = "/" + path[2];
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, MessagesMessages.WSTCP_1115_APP_SERV_REG_GET_ENDP_URL_PATTERN_EJB(urlPattern));
            }
        }

        return urlPattern;
    }

    private @Nullable String ensureSlash(@Nullable String s) {
        if (s != null && s.length() > 0 && s.charAt(0) != '/') {
            return "/" + s;
        }

        return s;
    }

    private boolean isTCPEnabled(final com.sun.enterprise.deployment.WebServiceEndpoint webServiceDesc) {
        return true;
    }

    /*
     * This function is called once for every endpoint registration.
     * and the WebModule corresponding to that endpoint is stored.
     */
    static WebModule getWebModule(WebServiceEndpoint wsep) {
        ApplicationRegistry appRegistry = org.glassfish.internal.api.Globals.getDefaultHabitat().getService(ApplicationRegistry.class);
        String appName = wsep.getBundleDescriptor().getApplication().getAppName();
        ApplicationInfo appInfo = appRegistry.get(appName);

        WebApplication webApp = null;
        if (appInfo != null) {
            Collection<ModuleInfo> moduleInfos = appInfo.getModuleInfos();
            Set<EngineRef> engineRefs = null;
            WebBundleDescriptor requiredWbd = (WebBundleDescriptor) wsep.getBundleDescriptor();
            for (ModuleInfo moduleInfo : moduleInfos) {
                engineRefs = moduleInfo.getEngineRefs();
                for (EngineRef engineRef : engineRefs) {
                    if (engineRef.getApplicationContainer() instanceof WebApplication) {
                        webApp = (WebApplication) engineRef.getApplicationContainer();
                        WebBundleDescriptor wbd = webApp.getDescriptor();
                        if (wbd.equals(requiredWbd)) {
                            break; //WebApp corresponding to wsep is found.
                        } else {
                            webApp = null;
                        }
                    }
                }
            }
        }
        //get the required WebModule from the webApp.
        if (webApp != null) {
            String requiredModule = ((WebBundleDescriptor) wsep.getBundleDescriptor()).getModuleName();
            Set<WebModule> webModules = webApp.getWebModules();
            for(WebModule wm : webModules) {
                if(wm.getModuleName().equalsIgnoreCase(requiredModule)) {
                    return wm;
                }
            }
        }

        return null;
    }
}

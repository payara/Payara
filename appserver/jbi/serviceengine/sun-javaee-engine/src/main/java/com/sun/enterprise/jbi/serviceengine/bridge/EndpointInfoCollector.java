/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.jbi.serviceengine.bridge;

import com.sun.enterprise.config.serverbeans.Application;
import com.sun.enterprise.deployment.WebServicesDescriptor;
import com.sun.enterprise.web.WebApplication;
import com.sun.enterprise.config.serverbeans.Applications;
import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.deployment.WebServiceEndpoint;
import com.sun.enterprise.v3.server.ApplicationLoaderService;
import com.sun.enterprise.web.WebModule;
import org.glassfish.internal.data.EngineRef;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.data.ApplicationRegistry;
import org.glassfish.internal.data.ModuleInfo;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;

import java.util.Collection;
import java.util.Set;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.component.Singleton;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.annotations.Inject;

/**
 * Gathers the endpoint information of the installed Applications.
 * It is used for creating endpoints, when the application has load-on-startup as false.
 *
 * @author Mohit Gupta
 */
@Service
@Scoped(Singleton.class)
public class EndpointInfoCollector {

    @Inject
    private Logger logger;
    @Inject
    private Applications allApplications;
    @Inject
    private Habitat habitat;

    //Injected so that during restart event, listener is started after the applications are loaded.
    @Inject
    private ApplicationLoaderService appLoaderService;

    /*
     * On startup of serviceengine gets the Endpoints
     * for the Applications already Installed, using Domain.xml
     */
    public void initialize() {

        Collection<Application> applications = allApplications.getApplications();
        if (applications != null) {
            for (Application application : applications) {
                updateEndpoints(application.getName());
            }
        }
    }

    /**
     * This method is called when load-on-startup is false and endpoint information
     * is not present in Endpoint Registry.
     */
    public List<WebServiceEndpoint> getEndpoints(String appName) {
        updateEndpoints(appName);
        return endpoints.get(appName);
    }

    /*
     * Gets the Application Reference from ApplicationRegistry, collects the WebServiceEndpoints
     * and updates the endpoints Map accordingly
     *
     */
    private void updateEndpoints(String appName) {

        ApplicationRegistry appRegistry = habitat.getComponent(ApplicationRegistry.class);
        ApplicationInfo appInfo = appRegistry.get(appName);

        if (appInfo != null) {
            Collection<ModuleInfo> moduleInfos = appInfo.getModuleInfos();
            Set<EngineRef> engineRefs = null;
            List<WebServiceEndpoint> list = new ArrayList<WebServiceEndpoint>();
            for (ModuleInfo moduleInfo : moduleInfos) {
                engineRefs = moduleInfo.getEngineRefs();
                for (EngineRef engineRef : engineRefs) {
                    if (engineRef.getApplicationContainer() instanceof WebApplication) {
                        WebApplication webApp = (WebApplication) engineRef.getApplicationContainer();
                        WebServicesDescriptor webServices = (webApp.getDescriptor()).getWebServices();
                        if (webServices != null && !list.containsAll(webServices.getEndpoints())) {
                            list.addAll(webServices.getEndpoints());
                        }
                    }
                }
            }
            if (!list.isEmpty()) {
                endpoints.put(appName, list);
                logger.log(Level.FINE, "serviceengine.websvc_endpoints_added", new Object[]{appName});
            }
        }
    }

    /*
     * This function is called once for every endpoint registration.
     * and the WebModule corresponding to that endpoint is stored.
     */
    public WebModule getWebModule(WebServiceEndpoint wsep) {
        ApplicationRegistry appRegistry = habitat.getComponent(ApplicationRegistry.class);
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

    /**
     * RemoveEndpoints from the map, after undeploy is complete.
     *
     */
    public void removeEndpoints(String appName) {
        if (endpoints.containsKey(appName)) {
            endpoints.remove(appName);
        }
    }

    /**
     * During appserver restart applications are loaded before Java EE service
     * engine is installed. This method will be called once during the
     * startup of Java EE service engine. At this time, we need to merge all
     * the endpoints collected so far with the map maintained in
     * EndpointRegistry.
     */
    public void mergeEndpointRegistry(Map<String, List<WebServiceEndpoint>> ws_endpoints) {
        ws_endpoints.putAll(endpoints);
        endpoints = ws_endpoints;
    }
    private static Map<String, List<WebServiceEndpoint>> endpoints =
            new HashMap<String, List<WebServiceEndpoint>>();
}

/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.paas.orchestrator;

import org.glassfish.hk2.scopes.Singleton;
import org.glassfish.paas.orchestrator.service.metadata.ServiceDescription;
import org.glassfish.paas.orchestrator.service.metadata.ServiceMetadata;
import org.glassfish.paas.orchestrator.service.metadata.ServiceReference;
import org.glassfish.paas.orchestrator.service.spi.ConfiguredService;
import org.glassfish.paas.orchestrator.service.spi.ProvisionedService;
import org.glassfish.paas.orchestrator.service.spi.ServicePlugin;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;

import java.util.*;

/**
 * @author Jagadish Ramu
 */
@Scoped(Singleton.class)
@Service
public class PaaSAppInfoRegistry {

    private Map<String, ServiceMetadata> serviceMetadata = new LinkedHashMap<String, ServiceMetadata>();
    private Map<String, Set<ProvisionedService>> provisionedServices = new LinkedHashMap<String, Set<ProvisionedService>>();
    private Map<String, Set<ConfiguredService>> configuredServices = new LinkedHashMap<String, Set<ConfiguredService>>();

    private Map<String, Map<ServiceReference,ServiceDescription>> serviceRefToSDMap=
            new LinkedHashMap<String, Map<ServiceReference,ServiceDescription>>();
    private Map<String, Map<ServiceDescription, ServicePlugin>> pluginsToHandleSDMap=
            new LinkedHashMap<String, Map<ServiceDescription, ServicePlugin>>();


    public ServiceMetadata removeServiceMetadata(String appName) {
        return serviceMetadata.remove(appName);
    }

    public Map<String, Set<ProvisionedService>> getAllProvisionedServices(){
        return provisionedServices;
    }

    public Set<ProvisionedService> removeProvisionedServices(String appName) {
        return provisionedServices.remove(appName);
    }

    public Map<ServiceReference, ServiceDescription> getSRToSDMap(String appName){
        Map<ServiceReference, ServiceDescription> srToSDMap;
        srToSDMap = serviceRefToSDMap.get(appName);
        if(srToSDMap == null){
            srToSDMap = new LinkedHashMap<ServiceReference, ServiceDescription>();
            serviceRefToSDMap.put(appName, srToSDMap);
        }
        return srToSDMap;
    }

    public Map<ServiceReference, ServiceDescription> removeSRToSDMap(String appName){
        return serviceRefToSDMap.remove(appName);
    }


    public Map<ServiceDescription, ServicePlugin> getPluginsToHandleSDs(String appName){
        Map<ServiceDescription, ServicePlugin> pluginToSDMap;
        pluginToSDMap = pluginsToHandleSDMap.get(appName);
        if(pluginToSDMap == null){
            pluginToSDMap = new LinkedHashMap<ServiceDescription, ServicePlugin>();
            pluginsToHandleSDMap.put(appName, pluginToSDMap);
        }
        return pluginToSDMap;
    }

    public Map<ServiceDescription, ServicePlugin> removePluginsToHandleSDs(String appName){
        return pluginsToHandleSDMap.remove(appName);
    }

    public ServiceMetadata getServiceMetadata(String appName) {
        return serviceMetadata.get(appName);
    }

     Set<ProvisionedService> getProvisionedServices(String appName) {
        Set<ProvisionedService> ps = provisionedServices.get(appName);
        if(ps == null){
            ps = new LinkedHashSet<ProvisionedService>();
            provisionedServices.put(appName, ps);
        }
        return provisionedServices.get(appName);
    }

    Set<ConfiguredService> getConfiguredServices(String appName) {
        Set<ConfiguredService> cs = configuredServices.get(appName);
        if(cs == null){
            cs = new LinkedHashSet<ConfiguredService>();
            configuredServices.put(appName, cs);
        }
        return configuredServices.get(appName);
    }

    public void addServiceMetadata(String appName, ServiceMetadata appServiceMetadata) {
        serviceMetadata.put(appName, appServiceMetadata);
    }

    public boolean unregisterProvisionedServices(String appName, Collection<ProvisionedService> provisionedServices) {
        return getProvisionedServices(appName).removeAll(provisionedServices);
    }

    public boolean unregisterConfiguredServices(String appName, Collection<ConfiguredService> configuredServices) {
        return getConfiguredServices(appName).removeAll(configuredServices);
    }

    public void registerProvisionedServices(String appName, Collection<ProvisionedService> provisionedServices) {
        getProvisionedServices(appName).addAll(provisionedServices);
    }

    public void registerConfiguredServices(String appName, Collection<ConfiguredService> configuredServices) {
        getConfiguredServices(appName).addAll(configuredServices);
    }
}

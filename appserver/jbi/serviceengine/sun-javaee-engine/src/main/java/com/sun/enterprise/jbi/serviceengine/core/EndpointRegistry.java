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

package com.sun.enterprise.jbi.serviceengine.core;
import com.sun.enterprise.deployment.WebServiceEndpoint;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.namespace.QName;

/**
 * Registry of all ServiceEndpoints, provides mapping between EndPointName and
 * ServiceEngineEndpoint.
 * @author Manisha Umbarje
 */
public class EndpointRegistry {
    
    /** Service QName to table of endpoints
     * The key being the service name and value being table of ServiceEngineEndpoints
     */
    private ConcurrentHashMap endpoints;
    private ConcurrentHashMap<String, DescriptorEndpointInfo> jbiProviders;
    private ConcurrentHashMap<String, DescriptorEndpointInfo> jbiConsumers;
    private ConcurrentHashMap<String, DescriptorEndpointInfo> wsdlEndpts;
    private ConcurrentHashMap<String, DescriptorEndpointInfo> jbiEndpts;
    
    private Set<String> compApps;
    private Map<String, List<WebServiceEndpoint>> ws_endpoints;
    
    private static EndpointRegistry store = new EndpointRegistry();
    
    /** Creates a new instance of ServiceEndPointRegistry */
    private EndpointRegistry() {
        endpoints = new ConcurrentHashMap(11,0.75f,4);
        jbiProviders = new ConcurrentHashMap<String,DescriptorEndpointInfo>(11,0.75f,4);
        jbiConsumers = new ConcurrentHashMap<String, DescriptorEndpointInfo>(11,0.75f,4);
        wsdlEndpts = new ConcurrentHashMap<String, DescriptorEndpointInfo>(11,0.75f,4);
        jbiEndpts = new ConcurrentHashMap<String, DescriptorEndpointInfo>(11,0.75f,4);
        compApps = new HashSet<String>();
        ws_endpoints = new HashMap<String, List<WebServiceEndpoint>>();
    }
    
    public static EndpointRegistry getInstance() {
        return store;
    }
    
    
    
    /**
     * Adds a ServiceEndpoint to the store
     */
    public void put(QName service, String endpointName, ServiceEngineEndpoint endpoint) {
        Map map=  (Map)endpoints.get(service);
        if(map == null) {
            map = new Hashtable();
            endpoints.put(service, map);
        }
        map.put(endpointName, endpoint);
        
    }
    
    /**
     *
     */
    public ServiceEngineEndpoint get(QName service, String endpointName) {
        Map map=  (Map)endpoints.get(service);
        if(map != null)
        return (ServiceEngineEndpoint)map.get(endpointName);
        else
            return null;
        
    }
    
    /**
     * Removes ServiceEndpoint from the store
     */
    public void delete(QName service, String endpointName) {
        
        Map map=  (Map)endpoints.get(service);
        map.remove(endpointName);
        
    }
    
    public List<ServiceEngineEndpoint> list() {
        List<ServiceEngineEndpoint> list = new LinkedList<ServiceEngineEndpoint>();
        for (Iterator itr = endpoints.values().iterator();itr.hasNext();) {
            Hashtable table = (Hashtable) itr.next();
            list.addAll(table.values());
        }
        return list;
    }

    /** The endpoints are populated by ServiceEngineAppListener */
    public Map<String, List<WebServiceEndpoint>> getWSEndpoints() {
        return ws_endpoints;
    }
    
    /**
     * Check whether this endpoint is provided by any composite application.
     */
    public boolean hasProviderEP(ServiceEngineEndpoint seEndpoint) {
        //if the javaee app is not deployed as part of comp app then return false
        if(!compApps.contains(seEndpoint.getApplicationName()))
            return false;
        
        QName service = seEndpoint.getServiceName();
        String endpointName = seEndpoint.getEndpointName();
        DescriptorEndpointInfo ep = 
                jbiProviders.get(service.getLocalPart() + endpointName);
        return ep !=null && ep.isStarted();
    }

    public boolean hasConsumerEP(QName service, String endpointName) {
        DescriptorEndpointInfo ep = 
                jbiConsumers.get(service.getLocalPart() + endpointName);
        return ep !=null && ep.isStarted();
    }

    /**
     * The APIs below are used by JBIEndpointManager.RegistryManager to populate
     * the entries
     */
    ConcurrentHashMap<String, DescriptorEndpointInfo> getProviders() {
        return jbiProviders;
    }
    
    ConcurrentHashMap<String, DescriptorEndpointInfo> getConsumers() {
        return jbiConsumers;
    }
    
    Set<String> getCompApps() {
        return compApps;
    }

    public ConcurrentHashMap<String, DescriptorEndpointInfo> getWSDLEndpts() {
        return wsdlEndpts;
    }

    public ConcurrentHashMap<String, DescriptorEndpointInfo> getJBIEndpts() {
        return jbiEndpts;
    }
}

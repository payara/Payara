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

import com.sun.enterprise.deployment.WebServiceEndpoint;
import com.sun.enterprise.jbi.serviceengine.core.ServiceEngineEndpoint;
import com.sun.enterprise.jbi.serviceengine.core.EndpointRegistry;
import com.sun.enterprise.jbi.serviceengine.core.JavaEEServiceEngineContext;
import com.sun.enterprise.jbi.serviceengine.core.DescriptorEndpointInfo;
import com.sun.enterprise.jbi.serviceengine.util.JBIConstants;
import org.glassfish.webservices.monitoring.WebServiceEngine;
import org.glassfish.webservices.monitoring.Endpoint;
import org.glassfish.webservices.monitoring.EndpointLifecycleListener;
import org.glassfish.webservices.monitoring.WebServiceEngineImpl;
import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.jbi.serviceengine.core.ServiceEngineRuntimeHelper;
import com.sun.logging.LogDomains;
import javax.jbi.component.ComponentContext;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.Iterator;
import java.util.logging.Logger;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.jbi.JBIException;
import javax.xml.namespace.QName;
import java.util.List;

/**
 * A utillity class which keeps track of JBI enabled end points
 * @author Manisha Umbarje
 */
public class EndpointHelper {
    
    /** A web service engine */
    private WebServiceEngine engine ;
    private ComponentContext context ;
    private EndpointRegistry registry;
    private Map uriToDetailsMap ;
    private static EndpointHelper helper = new EndpointHelper();
    private EndpointLifecycleListener epLifecycleListener;
    private static final String auto_enabled =
            System.getProperty(JBIConstants.AUTO_ENDPOINT_ENABLING);
    /**
     * Internal handle to the logger instance
     */
    protected static final Logger logger =
            LogDomains.getLogger(EndpointHelper.class, LogDomains.SERVER_LOGGER);
    
    /** Creates a new instance of EndpointHelper */
    private EndpointHelper() {
        engine = WebServiceEngineImpl.getInstance();
        epLifecycleListener = new EndpointLifecycleListenerImpl();
        engine.addLifecycleListener(epLifecycleListener);
        context = JavaEEServiceEngineContext.getInstance().getJBIContext();
        registry = EndpointRegistry.getInstance();
        uriToDetailsMap = new HashMap();
    }
    
    public static EndpointHelper getInstance() {
        return helper;
    }
    
    public void initialize() {
        Iterator<Endpoint> endpoints = engine.getEndpoints();
        while(endpoints.hasNext()) {
            registerEndpoint(endpoints.next());
        }
    }
    
    public void enableEndpoint(QName service, String endpointName) {
        if(endpointName != null) {
            ServiceEngineEndpoint endpoint = registry.get(service, endpointName);
            if(endpoint != null && (!endpoint.isEnabled())) {
                try {
                    ServiceEndpoint jbiEndpoint = activateEndpoint(
                            endpoint.getServiceName(),
                            endpoint.getEndpointName());
                    endpoint.setServiceEndpoint(jbiEndpoint);
                    endpoint.setEnabled(true);
                    debug(Level.INFO,"serviceengine.enable_endpoint",
                            new Object[]{service.getLocalPart(), endpointName});
                } catch(Exception e) {
                    debug(Level.SEVERE,"serviceengine.error_enable",
                            new Object[]{service.getLocalPart(), endpointName});
                            
                }
            }
        }
    }

    /**
     * Activates the end point in JBI
     * @param endpoint endPoint to be activated in JBI
     */
    public void registerEndpoint(Endpoint endpoint) {
        registerEndpoint(endpoint.getDescriptor());
    }
    
    public void registerEndpoint(WebServiceEndpoint webServiceDesc) {
        if(webServiceDesc != null) {
            // activate the end point in JBI
            String endpointName = webServiceDesc.hasWsdlPort() ? webServiceDesc.getWsdlPort().getLocalPart() : webServiceDesc.getEndpointName();
         
            debug(Level.FINE,"serviceengine.start_registration",
                    new Object[]{webServiceDesc.getServiceName(),
                            endpointName});
                
            try {
           
                boolean ejbType = webServiceDesc.implementedByEjbComponent() ;
                String relativeURI = webServiceDesc.getEndpointAddressUri();
                String implClass = (ejbType)?webServiceDesc.getTieClassName():
                                            webServiceDesc.getServletImplClass();
                String contextRoot = null;
                com.sun.enterprise.web.WebModule webModule = null;
                if(!ejbType) {
                    contextRoot =
                            webServiceDesc.getWebComponentImpl().
                            getWebBundleDescriptor().getContextRoot();
                    relativeURI = contextRoot + relativeURI;
                    EndpointInfoCollector epInfoCollector =
                            ServiceEngineRuntimeHelper.getRuntime().getEndpointInfoCollector();
                    webModule = epInfoCollector.getWebModule(webServiceDesc);
                }

                ServiceEngineEndpoint seEndpoint =
                        new ServiceEngineEndpoint(webServiceDesc,
                            webModule,
                            webServiceDesc.getServiceName(),
                            endpointName,
                            implClass,
                            contextRoot, 
                            true);
                if(isJBIEnabled(webServiceDesc) ||
                        registry.hasProviderEP(seEndpoint)) {
                    ServiceEndpoint endpoint = activateEndpoint(
                            webServiceDesc.getServiceName(),
                            endpointName);
                    seEndpoint.setServiceEndpoint(endpoint);
                     debug(Level.FINE,"serviceengine.success_registration",
                             new Object[]{webServiceDesc.getServiceName(),
                            endpointName});
                } else {
                    seEndpoint.setEnabled(false);
                }
                    
                // even if jbi-enabled flag is off, internal registries keep track 
                // of deployed web services in SJSAS 
                registry.put(webServiceDesc.getServiceName(),
                        endpointName, seEndpoint);
                
                uriToDetailsMap.put(relativeURI,
                        new Object[]{webServiceDesc.getServiceName(),
                                endpointName});
            } catch (Exception e) {
                debug(Level.SEVERE,"serviceengine.error_registration",
                        new Object[]{webServiceDesc.getServiceName(),
                                endpointName});
                
            }
                            
        }
    }
    
    /**
     * Deactivates the end point in JBI
     */
    public void disableEndpoint(QName service, String endpointName) {
        // deactivates the end point in JBI
        ServiceEngineEndpoint endpoint = registry.get(service, endpointName);
        
        if (endpoint != null) {
            try {
                ServiceEndpoint endpt = endpoint.getServiceEndpoint();
                // It's assumed that ServiceEndpoint is priorly activated in JBI
                if(endpt != null ) {
                    context.deactivateEndpoint(endpt);
                    endpoint.setEnabled(false);
                    debug(Level.INFO,"serviceengine.disable_endpoint",
                            new Object[]{service.getLocalPart(), endpointName});
                }
            } catch(Exception e) {
                debug(Level.SEVERE,"serviceengine.error_disable",
                        new Object[]{service.getLocalPart(), endpointName});
                        
            }
        }
    }
    
    public void unregisterEndpoint(QName service, String endpointName) {
        if(endpointName != null) {
            ServiceEngineEndpoint endpoint = registry.get(service, endpointName);
            
            if(endpoint != null) {
                String endpointURI = endpoint.getURI();
                disableEndpoint(service, endpointName);
                registry.delete(service, endpointName);
                uriToDetailsMap.remove(endpointURI);
                debug(Level.INFO,"serviceengine.success_removal",
                        new Object[]{service.getLocalPart(), endpointName});
            }
        }
    }
    
    public void toggleEndpointStatus(String uri, boolean flag) {
        Object[] endpointInfo = (Object[])uriToDetailsMap.get(uri);
        if(endpointInfo != null) {
            if(flag)
                enableEndpoint((QName)endpointInfo[0], (String)endpointInfo[1]);
            else
                disableEndpoint((QName)endpointInfo[0], (String)endpointInfo[1]);
        }
        
    }
    
    public void destroy() {
        engine.removeLifecycleListener(epLifecycleListener);
    }

    private ServiceEndpoint activateEndpoint(QName serviceName, String endpointName)
            throws JBIException {
        String key = DescriptorEndpointInfo.getDEIKey(serviceName, endpointName);
        DescriptorEndpointInfo dei = registry.getJBIEndpts().get(key);
        if(dei != null) {
            serviceName = dei.getServiceName();
            endpointName = dei.getEndpointName();
        }
        return context.activateEndpoint(serviceName, endpointName);
    }
    
    private void debug(Level logLevel, String msgID, Object[] params) {
        logger.log(logLevel, msgID, params);
    }

    private boolean isJBIEnabled(WebServiceEndpoint endpoint) {
        try {
            String applicationName =
                    endpoint.getWebService().getBundleDescriptor().getApplication().getRegistrationName();
            String endpointName = endpoint.getEndpointName();
            List webServiceEndpoints = null;

            ServiceEngineRuntimeHelper runtimeHelper = ServiceEngineRuntimeHelper.getRuntime();
            Applications apps = runtimeHelper.getApplications();

            //Another way for getting the application reference.
            //String serverInstance = serverContext.getInstanceName();
            //Application app = configBeansUtility.getSystemApplicationReferencedFrom(serverInstance, applicationName);
            
            J2eeApplication app = apps.getModule(J2eeApplication.class, applicationName);
            if(app != null)
                webServiceEndpoints = app.getWebServiceEndpoint();

            EjbModule ejbApp = apps.getModule(EjbModule.class, applicationName);
            if(ejbApp != null)
                webServiceEndpoints = ejbApp.getWebServiceEndpoint();

            WebModule webApp = apps.getModule(WebModule.class, applicationName);
            if(webApp != null)
                webServiceEndpoints = webApp.getWebServiceEndpoint();

            if(webServiceEndpoints != null){
                Iterator<com.sun.enterprise.config.serverbeans.WebServiceEndpoint> endpoints = webServiceEndpoints.iterator();
                com.sun.enterprise.config.serverbeans.WebServiceEndpoint endpointBean = null;
                while(endpoints.hasNext()){
                    endpointBean = endpoints.next();
                    if((endpointBean.getName()).equals(endpointName)){
                        return Boolean.parseBoolean(endpointBean.getJbiEnabled());
                    }
                }
            }
        } catch(Throwable ce) {
            debug(Level.SEVERE,"serviceengine.config_not_found",
                    new Object[]{endpoint.getServiceName(),
                            endpoint.getEndpointName()});
        }
        // By default endpoints are disabled
        return "true".equalsIgnoreCase(auto_enabled);
    }

}

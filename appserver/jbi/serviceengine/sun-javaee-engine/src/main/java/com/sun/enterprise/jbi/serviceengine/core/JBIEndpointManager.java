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

import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.LinkedList;
import java.util.Collection;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.concurrent.ConcurrentHashMap;

import com.sun.enterprise.jbi.serviceengine.bridge.EndpointHelper;
import com.sun.enterprise.jbi.serviceengine.util.soap.StringTranslator;
import com.sun.enterprise.deployment.WebServiceEndpoint;
import com.sun.enterprise.deployment.BundleDescriptor;
//import com.sun.enterprise.deployment.backend.DeployableObjectType;
//import com.sun.enterprise.deployment.phasing.DeploymentServiceUtils;
//import com.sun.enterprise.instance.BaseManager;
import com.sun.enterprise.jbi.serviceengine.bridge.EndpointInfoCollector;
import org.glassfish.webservices.WsUtil;
import com.sun.enterprise.util.Utility;
import com.sun.logging.LogDomains;
import com.sun.xml.ws.api.server.SDDocumentSource;
import com.sun.xml.ws.api.server.WSEndpoint;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.BindingID;

/**
 * Class to manage enabling and disabling of endpoints in NMR. It uses 
 * EndpointHelper to manage the endpoints.
 * 
 * @author Vikas Awasthi
 * @author Mohit Gupta
 */
public class JBIEndpointManager {

    private RegistryManager registryManager;
    private final Logger logger = LogDomains.getLogger(JBIEndpointManager.class, LogDomains.SERVER_LOGGER);
    private StringTranslator translator;
    
    JBIEndpointManager() {
        registryManager = new RegistryManager();
        translator = new StringTranslator(this.getClass().getPackage().getName(), 
                                        this.getClass().getClassLoader());
    }

    /**
     * Parse the jbi.xml file and read enpoint information. Store this 
     * information in the EndpointRegistry. JBIDescriptorReader is used to 
     * read jbi.xml. It creates a list of DescriptorEndpointInfo objects 
     * corresponding to the endpoints specified in the jbi.xml
     * 
     * If there is any exception while parsing jbi.xml then deployment of the 
     * service unit will be stopped.
     */
    void storeAllEndpoints(String appLocation, String su_Name) throws Exception {
        InputStream jbi_xml = null;
        try {
            if(new File(appLocation).isDirectory()) {
                jbi_xml = new FileInputStream(appLocation +
                        File.separator +
                        "META-INF" +
                        File.separator +
                        "jbi.xml");
            } else {
                ZipFile zipFile = new ZipFile(appLocation);
                for (Enumeration e = zipFile.entries(); e.hasMoreElements();) {
                    ZipEntry zipEntry = (ZipEntry) e.nextElement();
                    if("META-INF/jbi.xml".equals(zipEntry.getName())) {
                        jbi_xml = zipFile.getInputStream(zipEntry);
                        break;
                    }
                }
            }
 
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setValidating(false);
            Document doc = factory.newDocumentBuilder().parse(jbi_xml);
            JBIDescriptorReader reader = new JBIDescriptorReader(su_Name);
            reader.init(doc);
            registryManager.addAllEndpointInfo(reader.getEndpoints());
            registryManager.addServiceUnit(su_Name);
        } catch (Exception e) {
            String exceptionText = 
                    translator.getString("serviceengine.jbixml_readerror",su_Name);
            logger.log(Level.SEVERE,exceptionText,e);
            throw new Exception(exceptionText, e);
        } finally {
            if(jbi_xml!=null)
                jbi_xml.close();
        }
    }

    /**
     * Clear the registry. This will be called during service unit undeployment.
     */
    void removeAllEndpoints(String su_Name) {
        registryManager.removeAllConsumerEP(su_Name);
        registryManager.removeAllProviderEP(su_Name);
        registryManager.removeServiceUnit(su_Name);
    }

    /**
     * Start all the endpoints specified in jbi.xml. In case of consumer just 
     * the state variable is set to started. For providers all the endpoints are
     * activated in NMR.  
     */
    void startAllEndpoints(String su_Name) throws Exception {
        EndpointHelper epHelper = EndpointHelper.getInstance();
        for (DescriptorEndpointInfo ep : registryManager.getAllConsumerEP(su_Name))
            ep.setStarted(true);

        for (DescriptorEndpointInfo ep : registryManager.getAllProviderEP(su_Name)) {
            if(registryManager.getSEEndpoint(ep)==null) {
                //Get the Endpoints from EndpointRegistry.
                WebServiceEndpoint ws_endpoint = registryManager.getWSEndpoint(ep, su_Name);

                if (ws_endpoint == null) {
                    //Get the Endpoints form AppRegistry.
                    EndpointInfoCollector endpointInfoCollector = ServiceEngineRuntimeHelper.getRuntime().getEndpointInfoCollector();
                    List<WebServiceEndpoint> endpoints = endpointInfoCollector.getEndpoints(su_Name);
                    for (WebServiceEndpoint endpoint : endpoints) {
                        String endpointName = endpoint.hasWsdlPort() ? endpoint.getWsdlPort().getLocalPart() : endpoint.getEndpointName();
                        if (endpoint.getServiceName().equals(ep.getServiceName()) &&
                                endpointName.equals(ep.getEndpointName())) {
                            ws_endpoint = endpoint;
                        }
                    }
                }
                //No endpoint registered.
                if(ws_endpoint == null)
                throw new Exception(
                        translator.getString("serviceengine.endpoint_mismatch", 
                                            ep.getServiceName().getLocalPart(), 
                                            su_Name));
                createEndpoint(ws_endpoint, ep);
            }
           /* TODO: JBI Private is not being set in WebServices.
            if (ep.isPrivate()) {
                WebServiceEndpoint endpoint =
                        registryManager.getSEEndpoint(ep).getEndpointDesc();
                endpoint.setJBIPrivate(true);
            }*/
            ep.setStarted(true);
            // now activate the endpoint in NMR
            epHelper.enableEndpoint(ep.getServiceName(), ep.getEndpointName());
        }
    }

    /**
     * Disable the endpoints enabled in during start 
     */
    void stopAllEndpoints(String su_Name) {
        EndpointHelper epHelper = EndpointHelper.getInstance();
        for (DescriptorEndpointInfo ep : registryManager.getAllConsumerEP(su_Name))
            ep.setStarted(false);

        for (DescriptorEndpointInfo ep : registryManager.getAllProviderEP(su_Name)) {
            ep.setStarted(false);
            // now de-activate the endpoint in NMR
            epHelper.disableEndpoint(ep.getServiceName(), ep.getEndpointName());
        }
    }

    /**
     * Create a WSEndpoint and add it to the EndpointRegistry. The
     * JBIAdapterBuilder will later use this endpoint to create a JBIAdapter.
     * This code is copied from JAXWSServlet.registerEndpoint() with the
     * following changes-
     *  --> Container is null as there is no need for security and
     *      monitoring pipes
     *  --> No ServletAdapter is created and only WSEndpoints are created
     *  --> No updates are made to JAXWSAdapterRegistry. A different registry
     *      specific to Java EE service engine is used. 
     * Related Issue: 6519371.
     */
    private void createEndpoint(WebServiceEndpoint endpoint,
                                DescriptorEndpointInfo ep) throws Exception {
        
        BundleDescriptor bundledesc = endpoint.getBundleDescriptor();
        ClassLoader cl = bundledesc.getClassLoader();
        // set the app classloader to be used while creating the WSEndpoint
        ClassLoader origCl = Utility.setContextClassLoader(cl);
        try {
            WsUtil wsu = new WsUtil();
            Class serviceEndpointClass =
                    Class.forName(endpoint.getServletImplClass(), true, cl);
            // Get the proper binding using BindingID
            String givenBinding = endpoint.getProtocolBinding();

            // Get list of all wsdls and schema
            SDDocumentSource primaryWsdl = null;
            Collection docs = null;
            if(endpoint.getWebService().hasWsdlFile()) {
                //TODO : Refer to JAXWSServlet.registerEndpoint function
               /* BaseManager mgr;
                if(bundledesc.getApplication().isVirtual()) {
                    mgr = DeploymentServiceUtils.getInstanceManager(DeployableObjectType.WEB);
                } else {
                    mgr = DeploymentServiceUtils.getInstanceManager(DeployableObjectType.APP);
                }
               */
                String deployedDir = null;
               //     mgr.getLocation(bundledesc.getApplication().getRegistrationName());
                File pkgedWsdl;
                if(deployedDir != null) {
                    if(bundledesc.getApplication().isVirtual()) {
                        pkgedWsdl = new File(deployedDir+File.separator+
                                    endpoint.getWebService().getWsdlFileUri());
                    } else {
                        pkgedWsdl = new File(deployedDir+File.separator+
                                bundledesc.getModuleDescriptor().getArchiveUri().replaceAll("\\.", "_") +
                                File.separator + endpoint.getWebService().getWsdlFileUri());
                    }
                } else {
                    pkgedWsdl = new File(endpoint.getWebService().getWsdlFileUrl().getFile());
                }
                if(pkgedWsdl.exists()) {
                    primaryWsdl = SDDocumentSource.create(pkgedWsdl.toURL());
                    docs = wsu.getWsdlsAndSchemas(pkgedWsdl);
                }
            }

            // Get catalog info
            java.net.URL catalogURL = null;
            File catalogFile = new File(bundledesc.getDeploymentDescriptorDir() +
                    File.separator + "jax-ws-catalog.xml");
            if(catalogFile.exists()) {
                catalogURL = catalogFile.toURL();
            }

            // Create Binding and set service side handlers on this binding
//            MTOMFeature mtom = new MTOMFeature(wsu.setMtom(endpoint));
            WSBinding binding = BindingID.parse(givenBinding).createBinding();
            wsu.configureJAXWSServiceHandlers(endpoint, givenBinding, binding);

            WSEndpoint wsep = WSEndpoint.create(
                    serviceEndpointClass, // The endpoint class
                    false, // we do not want JAXWS to process @HandlerChain
                    null, // the invoker interface
                    endpoint.getServiceName(), // the service QName
                    endpoint.getWsdlPort(), // the port
                    null, // Container is used to set custom security/monitoring pipe
                    binding, // Derive binding
                    primaryWsdl, // primary WSDL
                    docs, // Collection of imported WSDLs and schema
                    catalogURL
                    );
            EndpointHelper epHelper = EndpointHelper.getInstance();
            epHelper.registerEndpoint(endpoint);
            registryManager.getSEEndpoint(ep).setWsep(wsep);

        } finally {
            Utility.setContextClassLoader(origCl);
        }
    }

    /** 
     * Inner class to manage jbi endpoints in the registry. As this class is 
     * used within JBIEndpoint manager this is made Inner rather than a 
     * separate class. 
     */
    class RegistryManager {

        RegistryManager() {
            provider_endpoints = epRegistry.getProviders();
            consumer_endpoints = epRegistry.getConsumers();
        }


        void addAllEndpointInfo(DescriptorEndpointInfo[] endpoints) {
            for (DescriptorEndpointInfo ep : endpoints) {
                if(ep.isProvider())
                    provider_endpoints.put(ep.getKey(), ep);
                else
                    consumer_endpoints.put(ep.getKey(), ep);
            }
        }
        
        void addServiceUnit(String su_Name) {
            epRegistry.getCompApps().add(su_Name);
        }

        void removeServiceUnit(String su_Name) {
            epRegistry.getCompApps().remove(su_Name);
        }

        void removeAllProviderEP(String su_Name) {
            for (String key : provider_endpoints.keySet()) {
                DescriptorEndpointInfo ep = provider_endpoints.get(key);
                if(ep!=null && ep.getSu_Name().equals(su_Name))
                    provider_endpoints.remove(key);
            }
        }

        void removeAllConsumerEP(String su_Name) {
            for (String key : consumer_endpoints.keySet()) {
                DescriptorEndpointInfo ep = consumer_endpoints.get(key);
                if(ep!=null && ep.getSu_Name().equals(su_Name))
                    consumer_endpoints.remove(key);
            }
        }

        List<DescriptorEndpointInfo> getAllProviderEP(String su_Name) {
            List<DescriptorEndpointInfo> list = 
                    new LinkedList<DescriptorEndpointInfo>();
            for (String key : provider_endpoints.keySet()) {
                DescriptorEndpointInfo ep = provider_endpoints.get(key);
                if(ep!=null && ep.getSu_Name().equals(su_Name))
                    list.add(ep);
            }
            return list;
        }

        List<DescriptorEndpointInfo> getAllConsumerEP(String su_Name) {
            List<DescriptorEndpointInfo> list = 
                    new LinkedList<DescriptorEndpointInfo>();
            
            for (String key : consumer_endpoints.keySet()) {
                DescriptorEndpointInfo ep = consumer_endpoints.get(key);
                if(ep!=null && ep.getSu_Name().equals(su_Name))
                    list.add(ep);
            }
            return list;
        }

        /** Return endpoint from the registry */
        ServiceEngineEndpoint getSEEndpoint(DescriptorEndpointInfo ep) {
            return epRegistry.get(ep.getServiceName(),ep.getEndpointName());
        }

        /** Get a WebServiceEndpoint descriptor that matches with the given
         *  DescriptorEndpointInfo */
        private WebServiceEndpoint getWSEndpoint(DescriptorEndpointInfo ep, String appName) {
            List<WebServiceEndpoint> endpoints = epRegistry.getWSEndpoints().get(appName);
            if(endpoints == null)
                return null;
            for (WebServiceEndpoint endpoint : endpoints) {
                String endpointName = endpoint.hasWsdlPort() ?
                        endpoint.getWsdlPort().getLocalPart() : endpoint.getEndpointName();
                if(endpoint.getServiceName().equals(ep.getServiceName()) &&
                        endpointName.equals(ep.getEndpointName())) {
                    return endpoint;
                }
            }
            return null;
        }

        // Using vectors here might be easier while updating but searching would
        // be costlier. Searching in HashMap is faster.
        // Since DescriptorEndpointInfo has serviceUnitName there is no need to
        // have a data structure that maintains endpointInfo Vs su_Name
        private ConcurrentHashMap<String, DescriptorEndpointInfo> provider_endpoints;
        private ConcurrentHashMap<String, DescriptorEndpointInfo> consumer_endpoints;
        private EndpointRegistry epRegistry = EndpointRegistry.getInstance();
    }
}

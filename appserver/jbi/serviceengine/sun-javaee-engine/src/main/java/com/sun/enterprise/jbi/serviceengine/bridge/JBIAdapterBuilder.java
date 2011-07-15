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

import com.sun.enterprise.jbi.serviceengine.ServiceEngineException;
import com.sun.enterprise.jbi.serviceengine.bridge.transport.JBIAdapter;
import com.sun.enterprise.jbi.serviceengine.core.EndpointRegistry;
import com.sun.enterprise.jbi.serviceengine.core.ServiceEngineEndpoint;
import com.sun.enterprise.jbi.serviceengine.core.ServiceEngineRuntimeHelper;
import com.sun.enterprise.web.WebComponentInvocation;
import org.glassfish.webservices.JAXWSAdapterRegistry;
import org.glassfish.webservices.EjbRuntimeEndpointInfo;
import org.glassfish.webservices.WebServiceEjbEndpointRegistry;
import com.sun.logging.LogDomains;
import com.sun.xml.ws.api.server.Adapter;
import com.sun.xml.ws.api.server.SDDocument;
import com.sun.xml.ws.api.server.DocumentAddressResolver;
import javax.jbi.messaging.MessageExchange;
import com.sun.xml.ws.transport.http.servlet.ServletAdapter;

import org.glassfish.webservices.AdapterInvocationInfo;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.glassfish.api.invocation.InvocationManager;
import org.w3c.dom.Document;

/**
 * A Builder class to create JBIAdapters. JBIAdapters are created from 
 * WSEndpoints using the webservice container code.
 *
 * @author Vikas Awasthi
 */
public class JBIAdapterBuilder {
    
    private EndpointRegistry endpointRegistry = EndpointRegistry.getInstance();
    private final Logger logger = LogDomains.getLogger(JBIAdapterBuilder.class, LogDomains.SERVER_LOGGER);
    
    JBIAdapterBuilder() {
    }
    
    public JBIAdapter createAdapter(QName service, String endpointName, MessageExchange me)
    throws ServiceEngineException {
        ServiceEngineEndpoint endpt = endpointRegistry.get(service,  endpointName);
        if(endpt != null) {
            return createWSAdapter(endpt, me);
        }
        throw new ServiceEngineException("Endpoint " +  endpointName + "not deployed in JBI");
    }
    
    private JBIAdapter createWSAdapter(ServiceEngineEndpoint endpt,
            MessageExchange me) {
        try {
            AdapterInvocationInfo adapterInfo = null;
            ServletAdapter sadapter;
            EjbRuntimeEndpointInfo ejbEndPtInfo=null;
            WebComponentInvocation inv = null;
            if(endpt.isImplementedByEJB()) {
                WebServiceEjbEndpointRegistry registry = ServiceEngineRuntimeHelper.getRuntime().getWebServiceEjbEndpointRegistry();
                ejbEndPtInfo = registry.getEjbWebServiceEndpoint(endpt.getURI(), "POST", null);
                 adapterInfo =
                        (AdapterInvocationInfo) ejbEndPtInfo.prepareInvocation(true);
                sadapter = adapterInfo.getAdapter();
                endpt.setWsep(sadapter.getEndpoint());
            }else {
                
                String url = endpt.getURI();
                String contextRoot = endpt.getContextRoot();
                Adapter adapter =
                        JAXWSAdapterRegistry.getInstance().getAdapter(contextRoot, url, url);
                if (endpt.getWebModule() == null) {
                    //This is a hack as webModule should have been initialized in EndointHelper
                    //during the creation of ServiceEngineEndpoint. But, sometimes webApp.getWebModules()
                    //returns null on accessing it just after the deployment.
                    EndpointInfoCollector infoCollector
                            = ServiceEngineRuntimeHelper.getRuntime().getEndpointInfoCollector();
                    endpt.setWebModule(infoCollector.getWebModule(endpt.getEndpointDesc()));
                }
                InvocationManager invocationMgr =
                        ServiceEngineRuntimeHelper.getRuntime().getInvocationManager();
                inv = new WebComponentInvocation(endpt.getWebModule());
                invocationMgr.preInvoke(inv);
            
                if(adapter != null)
                    endpt.setWsep(adapter.getEndpoint());
                //for null adapter endpt is already set during comp app deployment
            }
            
            logger.log(Level.FINE,"Successfully created JBIAdapter for endpoint:"
                    + endpt.getWsep());
            if(endpt.getWsdlDocument() == null) {
                SDDocument sdDocument = endpt.getWsep().getServiceDefinition().getPrimary();
                synchronized(sdDocument) {
                    if(endpt.getWsdlDocument() == null) {
                        Document doc = getWSDLDocument(sdDocument);
                        endpt.setWsdlDocument(doc);
                    }
                }
            }
            return new JBIAdapter(endpt.getWsep(), endpt, me, endpt.getClassLoader(), ejbEndPtInfo, adapterInfo, inv);
            
        } catch (Exception e) {
            logger.log(Level.SEVERE,"Exception in creating JBIAdapter:"+e.getMessage(), e);
        }
        return null;
    }
    
    /**
     * Create a Dom document from the output stream provided by the given
     * JAX-WS SDDocument
     */
    private Document getWSDLDocument(SDDocument document) throws Exception {
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        document.writeTo(null, getResolver(), baos);
        baos.flush();
        InputStream in  = new ByteArrayInputStream(baos.toByteArray());
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setValidating(false);
            
            DocumentBuilder db = factory.newDocumentBuilder();
            return db.parse(in, document.getURL().toString());
        } finally {
            in.close();
            baos.close();
        }
    }
    
    private DocumentAddressResolver getResolver() {
        return new DocumentAddressResolver() {
            public String getRelativeAddressFor(SDDocument current,
                    SDDocument referenced) {
                // FIX for IT 2781
                logger.log(Level.FINE, "Current SDDocument URL = " + 
                        current.getURL() + ", isWSDL = " + current.isWSDL());
                logger.log(Level.FINE, "Referenced SDDocument URL = " + 
                        referenced.getURL() + ", isWSDL = " + referenced.isWSDL());
                if(referenced.isWSDL()) {
                    logger.log(Level.FINE, "Relative address for referenced " +
                            "SDDocument = " + referenced.getURL().toString());
                    return referenced.getURL().toString();
                }
                // END OF FIX for IT 2781.
                return "";
            }
        };
    }
}

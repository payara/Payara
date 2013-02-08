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

package com.sun.enterprise.deployment.node.runtime;

import com.sun.enterprise.deployment.*;
import com.sun.enterprise.deployment.node.DeploymentDescriptorNode;
import com.sun.enterprise.deployment.node.XMLElement;
import com.sun.enterprise.deployment.node.runtime.common.MessageSecurityBindingNode;
import com.sun.enterprise.deployment.node.runtime.common.RuntimeNameValuePairNode;
import com.sun.enterprise.deployment.runtime.common.MessageSecurityBindingDescriptor;
import com.sun.enterprise.deployment.util.DOLUtils;
import com.sun.enterprise.deployment.xml.RuntimeTagNames;
import com.sun.enterprise.deployment.xml.WebServicesTagNames;
import org.glassfish.deployment.common.Descriptor;
import org.w3c.dom.Node;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.logging.annotation.LogMessageInfo;

/**
 * This node is responsible for handling runtime info for
 * web service endpoints.
 *
 * @author  Kenneth Saks
 * @version 
 */
public class WebServiceEndpointRuntimeNode extends DeploymentDescriptorNode {

    private static final Logger logger = DOLUtils.getDefaultLogger();

    @LogMessageInfo(message = "Unknown port-component-name {0} port, all sub elements will be ignored.", level="SEVERE",
            cause = "Used port-component-name does not exists.",
            action = "Use the name of existing web service endpoint")
      private static final String WS_PORT_UNKNOWN = "AS-DEPLOYMENT-00016";

    private Descriptor descriptor;

    public WebServiceEndpointRuntimeNode() {
        registerElementHandler(
            new XMLElement(WebServicesTagNames.MESSAGE_SECURITY_BINDING),
            MessageSecurityBindingNode.class, "setMessageSecurityBinding");

        registerElementHandler(new XMLElement(RuntimeTagNames.PROPERTY), 
                               RuntimeNameValuePairNode.class, "addProperty");
    }

    @Override
    public Object getDescriptor() {
        return descriptor;
    }

    /**
     * all sub-implementation of this class can use a dispatch table to map xml element to
     * method name on the descriptor class for setting the element value. 
     *  
     * @return the map with the element name as a key, the setter method as a value
     */    
    @Override
    protected Map getDispatchTable() {    
        Map table = super.getDispatchTable();
        table.put(WebServicesTagNames.ENDPOINT_ADDRESS_URI, 
                  "setEndpointAddressUri");
        table.put(WebServicesTagNames.TIE_CLASS, "setTieClassName");
        table.put(WebServicesTagNames.SERVLET_IMPL_CLASS,
                  "setServletImplClass");
        table.put(WebServicesTagNames.DEBUGGING_ENABLED, "setDebugging");
        table.put(WebServicesTagNames.NAMESPACE_URI, "setServiceNamespaceUri");
        table.put(WebServicesTagNames.LOCAL_PART, "setServiceLocalPart");
        table.put(RuntimeTagNames.AUTH_METHOD, "setAuthMethod");
        table.put(RuntimeTagNames.REALM, "setRealm");
        table.put(WebServicesTagNames.TRANSPORT_GUARANTEE, 
                  "setTransportGuarantee");

        return table;
    }
    
    /**
     * receives notiification of the value for a particular tag
     * 
     * @param element the xml element
     * @param value it's associated value
     */

    @Override
    public void setElementValue(XMLElement element, String value) {
        if (WebServicesTagNames.PORT_COMPONENT_NAME.equals
            (element.getQName())) {
            Object parentDesc = getParentNode().getDescriptor();
            if (parentDesc instanceof EjbDescriptor) {
                EjbBundleDescriptor bundle = 
                    ((EjbDescriptor) parentDesc).getEjbBundleDescriptor();
                if (bundle != null) {
                    WebServicesDescriptor webServices = bundle.getWebServices();
                    descriptor = webServices.getEndpointByName(value);
                }
            } else if( parentDesc instanceof WebComponentDescriptor) {
                WebBundleDescriptor bundle = ((WebComponentDescriptor) parentDesc).getWebBundleDescriptor();
                if (bundle != null) {
                    WebServicesDescriptor webServices = bundle.getWebServices();
                    descriptor = webServices.getEndpointByName(value);
                }
            }
            if (descriptor==null) {
                logger.log(Level.SEVERE, WS_PORT_UNKNOWN, value);
            }
        } else super.setElementValue(element, value);
    }
    
    /**
     * write the descriptor class to a DOM tree and return it
     *
     * @param parent node for the DOM tree
     * @param node name for the descriptor
     * @param the descriptor to write
     * @return the DOM tree top node
     */    
    public Node writeDescriptor(Node parent, String nodeName, 
                                WebServiceEndpoint endpoint) {
        Node endpointNode = 
            super.writeDescriptor(parent, nodeName, endpoint);

        appendTextChild(endpointNode, WebServicesTagNames.PORT_COMPONENT_NAME,
                        endpoint.getEndpointName());
        appendTextChild(endpointNode, WebServicesTagNames.ENDPOINT_ADDRESS_URI,
                        endpoint.getEndpointAddressUri());
        
        // login config only makes sense for ejbs.  For web components,
        // this info is described in web application itself.
        if( endpoint.implementedByEjbComponent() &&
            endpoint.hasAuthMethod() ) {
            Node loginConfigNode = appendChild(endpointNode, 
                                               RuntimeTagNames.LOGIN_CONFIG);
            
            appendTextChild(loginConfigNode, RuntimeTagNames.AUTH_METHOD,
                            endpoint.getAuthMethod());
            appendTextChild(loginConfigNode, RuntimeTagNames.REALM,
                            endpoint.getRealm());
        }

        // message-security-binding
        MessageSecurityBindingDescriptor messageSecBindingDesc = 
            endpoint.getMessageSecurityBinding();
        if (messageSecBindingDesc != null) {
            MessageSecurityBindingNode messageSecBindingNode = 
                new MessageSecurityBindingNode();
            messageSecBindingNode.writeDescriptor(endpointNode, WebServicesTagNames.MESSAGE_SECURITY_BINDING, messageSecBindingDesc);
        }

        appendTextChild(endpointNode, WebServicesTagNames.TRANSPORT_GUARANTEE,
                        endpoint.getTransportGuarantee());

        QName serviceName = endpoint.getServiceName();
        if( serviceName != null ) {
            Node serviceQnameNode = appendChild
                (endpointNode, WebServicesTagNames.SERVICE_QNAME);
                                                
            appendTextChild(serviceQnameNode, WebServicesTagNames.NAMESPACE_URI,
                            serviceName.getNamespaceURI());
            appendTextChild(serviceQnameNode, WebServicesTagNames.LOCAL_PART,
                            serviceName.getLocalPart());
        }

        appendTextChild(endpointNode, WebServicesTagNames.TIE_CLASS,
                        endpoint.getTieClassName());
        
        if( endpoint.implementedByWebComponent() &&
            (endpoint.getServletImplClass() != null) ) {
            appendTextChild(endpointNode, 
                            WebServicesTagNames.SERVLET_IMPL_CLASS,
                            endpoint.getServletImplClass());
        }
        
        //debugging-enabled?
        appendTextChild(endpointNode, WebServicesTagNames.DEBUGGING_ENABLED,
                        endpoint.getDebugging());

        //property*
        Iterator properties = endpoint.getProperties();
        if (properties!=null) {
            RuntimeNameValuePairNode propNode = new RuntimeNameValuePairNode();        
            while (properties.hasNext()) {
                NameValuePairDescriptor aProp = (NameValuePairDescriptor) properties.next();
                propNode.writeDescriptor(endpointNode, RuntimeTagNames.PROPERTY, aProp);
            }
	    }
                            
        return endpointNode;
    }
    
    /**
     * writes all the runtime information for web service endpoints for
     * a given ejb
     * 
     * @param parent node to add the runtime xml info
     * @param the ejb endpoint
     */        
    public void writeWebServiceEndpointInfo(Node parent, EjbDescriptor ejb) {
                                            
        EjbBundleDescriptor bundle = ejb.getEjbBundleDescriptor();
        WebServicesDescriptor webServices = bundle.getWebServices();
        Collection endpoints = webServices.getEndpointsImplementedBy(ejb);
        for(Iterator iter = endpoints.iterator(); iter.hasNext();) {
            WebServiceEndpoint next = (WebServiceEndpoint) iter.next();
            writeDescriptor(parent, WebServicesTagNames.WEB_SERVICE_ENDPOINT, 
                            next);
        }
    }

    /**
     * writes all the runtime information for web service endpoints for
     * a given web component
     * 
     * @param parent node to add the runtime xml info
     * @param the web component
     */        
    public void writeWebServiceEndpointInfo
        (Node parent, WebComponentDescriptor webComp) {
        
        WebBundleDescriptor bundle = webComp.getWebBundleDescriptor();
        WebServicesDescriptor webServices = bundle.getWebServices();
        Collection endpoints = webServices.getEndpointsImplementedBy(webComp);
        for(Iterator iter = endpoints.iterator(); iter.hasNext();) {
            WebServiceEndpoint next = (WebServiceEndpoint) iter.next();
            writeDescriptor(parent, WebServicesTagNames.WEB_SERVICE_ENDPOINT, 
                            next);
        }
    }
    
}

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

/*
 * WebServiceEndpointNode.java
 *
 * Created on March 21, 2002, 4:16 PM
 */

package org.glassfish.webservices.node;

import com.sun.enterprise.deployment.WebServiceEndpoint;
import com.sun.enterprise.deployment.node.DisplayableComponentNode;
import com.sun.enterprise.deployment.node.XMLElement;
import com.sun.enterprise.deployment.xml.WebServicesTagNames;
import org.w3c.dom.Node;

import javax.xml.namespace.QName;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.webservices.connector.LogUtils;

/**
 * This node handles the web service endpoint definition
 *
 * @author Jerome Dochez
 */
public class WebServiceEndpointNode extends DisplayableComponentNode {

    private static final Logger logger = LogUtils.getLogger();

    private final static XMLElement tag =
        new XMLElement(WebServicesTagNames.PORT_COMPONENT);

    /** Creates a new instance of WebServiceEndpointNode */
    public WebServiceEndpointNode() {
        super();
        registerElementHandler
            (new XMLElement(WebServicesTagNames.HANDLER),
             WebServiceHandlerNode.class, "addHandler");
        registerElementHandler
            (new XMLElement(WebServicesTagNames.ADDRESSING),
             AddressingNode.class, "setAddressing");
        registerElementHandler
            (new XMLElement(WebServicesTagNames.RESPECT_BINDING),
             RespectBindingNode.class, "setRespectBinding");
        registerElementHandler
            (new XMLElement(WebServicesTagNames.HANDLER_CHAIN),
             WebServiceHandlerChainNode.class, "addHandlerChain");
    }

    /**
     * @return the XML tag associated with this XMLNode
     */
    protected XMLElement getXMLRootTag() {
        return tag;
    }

    @Override
    protected WebServiceEndpoint createDescriptor() {
        return new WebServiceEndpoint();
    }
    
    /**
     * all sub-implementation of this class can use a dispatch table to map xml element to
     * method name on the descriptor class for setting the element value. 
     *  
     * @return the map with the element name as a key, the setter method as a value
     */
    protected Map getDispatchTable() {
        Map table = super.getDispatchTable();
        table.put(WebServicesTagNames.PORT_COMPONENT_NAME, "setEndpointName");
        table.put(WebServicesTagNames.SERVICE_ENDPOINT_INTERFACE, 
                  "setServiceEndpointInterface");
        table.put(WebServicesTagNames.PROTOCOL_BINDING, "setProtocolBinding");
        table.put(WebServicesTagNames.ENABLE_MTOM, "setMtomEnabled");
        table.put(WebServicesTagNames.MTOM_THRESHOLD, "setMtomThreshold");
        return table;
    }
    
    /**
     * receives notification of the value for a particular tag
     * 
     * @param element the xml element
     * @param value it's associated value
     */
    public void setElementValue(XMLElement element, String value) {
        String elementName = element.getQName();
        WebServiceEndpoint endpoint = (WebServiceEndpoint) getDescriptor();
        if (WebServicesTagNames.EJB_LINK.equals(elementName)) {
            endpoint.setEjbLink(value);
        } else if (WebServicesTagNames.SERVLET_LINK.equals(elementName)) {
            endpoint.setWebComponentLink(value);
        } else if (WebServicesTagNames.WSDL_PORT.equals(elementName)) {
            String prefix = getPrefixFromQName(value);
            String localPart = getLocalPartFromQName(value);
            String namespaceUri = resolvePrefix(element, prefix);
            if( namespaceUri == null) {
                logger.log(Level.SEVERE, LogUtils.INVALID_DESC_MAPPING_FAILURE,
                    new Object[] {prefix , value });
            } else {
                QName wsdlPort = new QName(namespaceUri, localPart);
                endpoint.setWsdlPort(wsdlPort, prefix);
            }
        } else if(WebServicesTagNames.WSDL_SERVICE.equals(elementName)) {
            String prefix = getPrefixFromQName(value);
            String localPart = getLocalPartFromQName(value);
            String namespaceUri = resolvePrefix(element, prefix);
            if( namespaceUri == null) {
                logger.log(Level.SEVERE, LogUtils.INVALID_DESC_MAPPING_FAILURE,
                    new Object[] {prefix , value });
            } else {
                QName wsdlSvc = new QName(namespaceUri, localPart);
                endpoint.setWsdlService(wsdlSvc, prefix);
            }
        } else super.setElementValue(element, value);
    }
    
    /**
     * write the method descriptor class to a query-method DOM tree 
     * and return it
     *
     * @param parent node in the DOM tree 
     * @param nodeName name for the root element of this xml fragment
     * @param descriptor the descriptor to write
     * @return the DOM tree top node
     */
    public Node writeDescriptor(Node parent, String nodeName, 
                                WebServiceEndpoint descriptor) {        
        Node wseNode = super.writeDescriptor(parent, nodeName, descriptor);

        writeDisplayableComponentInfo(wseNode, descriptor);

        appendTextChild(wseNode, 
                        WebServicesTagNames.PORT_COMPONENT_NAME,
                        descriptor.getEndpointName());
        
        QName wsdlService = descriptor.getWsdlService();
        if((wsdlService!=null) &&
            (wsdlService.getLocalPart().length() != 0)) {
            appendQNameChild(WebServicesTagNames.WSDL_SERVICE, wseNode, 
                         wsdlService.getNamespaceURI(), wsdlService.getLocalPart(),
                         descriptor.getWsdlServiceNamespacePrefix());
        }
        
        QName wsdlPort = descriptor.getWsdlPort();
        if((wsdlPort!=null) &&
            (wsdlPort.getLocalPart().length() != 0)) {
            appendQNameChild(WebServicesTagNames.WSDL_PORT, wseNode, 
                         wsdlPort.getNamespaceURI(), wsdlPort.getLocalPart(),
                         descriptor.getWsdlPortNamespacePrefix());
        }

        appendTextChild(wseNode,
                        WebServicesTagNames.ENABLE_MTOM,
                        descriptor.getMtomEnabled());
        appendTextChild(wseNode,
                        WebServicesTagNames.MTOM_THRESHOLD,
                        descriptor.getMtomThreshold());
        //TODO add addressing etc here
        if(descriptor.hasUserSpecifiedProtocolBinding()) {
            appendTextChild(wseNode, 
                        WebServicesTagNames.PROTOCOL_BINDING,
                        descriptor.getProtocolBinding());            
        }
        
        appendTextChild(wseNode, 
                        WebServicesTagNames.SERVICE_ENDPOINT_INTERFACE, 
                        descriptor.getServiceEndpointInterface());
        
        if( descriptor.implementedByWebComponent() ) {
            Node linkNode = 
                appendChild(wseNode, WebServicesTagNames.SERVICE_IMPL_BEAN);
            appendTextChild(linkNode, WebServicesTagNames.SERVLET_LINK,
                            descriptor.getWebComponentLink());
        } else if( descriptor.implementedByEjbComponent() ) {
            Node linkNode = 
                appendChild(wseNode, WebServicesTagNames.SERVICE_IMPL_BEAN);
            appendTextChild(linkNode, WebServicesTagNames.EJB_LINK,
                            descriptor.getEjbLink());
        } else {
            logger.log(Level.INFO, LogUtils.WS_NOT_TIED_TO_COMPONENT,
                    descriptor.getEndpointName());
        }

        WebServiceHandlerNode handlerNode = new WebServiceHandlerNode();
        handlerNode.writeWebServiceHandlers(wseNode, 
                                            descriptor.getHandlers());

        WebServiceHandlerChainNode handlerChainNode = new WebServiceHandlerChainNode();
        handlerChainNode.writeWebServiceHandlerChains(wseNode, descriptor.getHandlerChain());
        return wseNode;
    }    
}

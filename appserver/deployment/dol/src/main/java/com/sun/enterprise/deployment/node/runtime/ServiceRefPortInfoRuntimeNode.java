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

package com.sun.enterprise.deployment.node.runtime;

import com.sun.enterprise.deployment.NameValuePairDescriptor;
import com.sun.enterprise.deployment.ServiceRefPortInfo;
import com.sun.enterprise.deployment.node.DeploymentDescriptorNode;
import com.sun.enterprise.deployment.node.NameValuePairNode;
import com.sun.enterprise.deployment.node.XMLElement;
import com.sun.enterprise.deployment.node.runtime.common.MessageSecurityBindingNode;
import com.sun.enterprise.deployment.runtime.common.MessageSecurityBindingDescriptor;
import com.sun.enterprise.deployment.xml.WebServicesTagNames;
import org.w3c.dom.Node;

import javax.xml.namespace.QName;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * This node is responsible for handling runtime info for
 * a service reference wsdl port.
 *
 * @author  Kenneth Saks
 * @version 
 */
public class ServiceRefPortInfoRuntimeNode extends DeploymentDescriptorNode {

    private String namespaceUri;

    public ServiceRefPortInfoRuntimeNode() {
        super();
        registerElementHandler
            (new XMLElement(WebServicesTagNames.STUB_PROPERTY),
             NameValuePairNode.class, "addStubProperty");
        registerElementHandler
            (new XMLElement(WebServicesTagNames.CALL_PROPERTY),
             NameValuePairNode.class, "addCallProperty");
        registerElementHandler(new XMLElement(WebServicesTagNames.MESSAGE_SECURITY_BINDING), MessageSecurityBindingNode.class, "setMessageSecurityBinding");
    }

    /**
     * all sub-implementation of this class can use a dispatch table to map xml element to
     * method name on the descriptor class for setting the element value. 
     *  
     * @return the map with the element name as a key, the setter method as a value
     */    
    protected Map getDispatchTable() {    
        Map table = super.getDispatchTable();
        table.put(WebServicesTagNames.SERVICE_ENDPOINT_INTERFACE, 
                  "setServiceEndpointInterface");
        return table;
    }
    
    /**
     * receives notiification of the value for a particular tag
     * 
     * @param element the xml element
     * @param value it's associated value
     */

    public void setElementValue(XMLElement element, String value) {
        String name = element.getQName();
        if (WebServicesTagNames.NAMESPACE_URI.equals(name)) {
            namespaceUri = value;
        } else if (WebServicesTagNames.LOCAL_PART.equals(name)) {
            ServiceRefPortInfo desc = (ServiceRefPortInfo)
                getDescriptor();
            QName wsdlPort = new QName(namespaceUri, value);
            desc.setWsdlPort(wsdlPort);
            namespaceUri = null;
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
                                ServiceRefPortInfo desc) {
        Node serviceRefPortInfoRuntimeNode = 
            super.writeDescriptor(parent, nodeName, desc);

        appendTextChild(serviceRefPortInfoRuntimeNode,
                        WebServicesTagNames.SERVICE_ENDPOINT_INTERFACE,
                        desc.getServiceEndpointInterface());

        QName port = desc.getWsdlPort();

        if( port != null ) {
            Node wsdlPortNode = appendChild(serviceRefPortInfoRuntimeNode,
                                            WebServicesTagNames.WSDL_PORT);
            appendTextChild(wsdlPortNode, 
                            WebServicesTagNames.NAMESPACE_URI,
                            port.getNamespaceURI());
            appendTextChild(wsdlPortNode,
                            WebServicesTagNames.LOCAL_PART,
                            port.getLocalPart());
        }

        // stub-property*

        NameValuePairNode nameValueNode = new NameValuePairNode();

        Set stubProperties = desc.getStubProperties();
        for(Iterator iter = stubProperties.iterator(); iter.hasNext();) {
            NameValuePairDescriptor next = (NameValuePairDescriptor)iter.next();
            nameValueNode.writeDescriptor
                (serviceRefPortInfoRuntimeNode,
                 WebServicesTagNames.STUB_PROPERTY, next);
        }

        // call-property*
        for(Iterator iter = desc.getCallProperties().iterator();
            iter.hasNext();) {
            NameValuePairDescriptor next = (NameValuePairDescriptor)iter.next();
            nameValueNode.writeDescriptor
                (serviceRefPortInfoRuntimeNode, 
                 WebServicesTagNames.CALL_PROPERTY, next);
        }

        // message-security-binding
        MessageSecurityBindingDescriptor messageSecBindingDesc =
            desc.getMessageSecurityBinding();
        if (messageSecBindingDesc != null) {
            MessageSecurityBindingNode messageSecBindingNode =
                new MessageSecurityBindingNode();
            messageSecBindingNode.writeDescriptor(serviceRefPortInfoRuntimeNode, WebServicesTagNames.MESSAGE_SECURITY_BINDING, messageSecBindingDesc);
        }

        return serviceRefPortInfoRuntimeNode;
    }  
    
}

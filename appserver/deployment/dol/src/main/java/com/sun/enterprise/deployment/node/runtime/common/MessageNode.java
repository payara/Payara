/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.deployment.node.runtime.common;

import com.sun.enterprise.deployment.*;
import com.sun.enterprise.deployment.node.DeploymentDescriptorNode;
import com.sun.enterprise.deployment.node.MethodNode;
import com.sun.enterprise.deployment.node.XMLElement;
import com.sun.enterprise.deployment.node.XMLNode;
import com.sun.enterprise.deployment.runtime.common.MessageDescriptor;
import com.sun.enterprise.deployment.xml.RuntimeTagNames;
import com.sun.enterprise.deployment.xml.WebServicesTagNames;
import org.w3c.dom.Node;

/**
 * This node handles message element
 *
 */
public class MessageNode extends DeploymentDescriptorNode {

    MessageDescriptor descriptor = null;
    private static final String ALL_METHODS = "*";

    public MessageNode() {
        registerElementHandler(new XMLElement(
            RuntimeTagNames.JAVA_METHOD), MethodNode.class, 
            "setMethodDescriptor");
    }
    
    /**
    * @return the descriptor instance to associate with this XMLNode
    */    
    public Object getDescriptor() {
        if (descriptor == null) {
            descriptor = new MessageDescriptor();
            setMiscDescriptors();
        }
        return descriptor;
    }     

        
    /**
     * receives notification of the value for a particular tag
     *
     * @param element the xml element
     * @param value it's associated value
     */
    public void setElementValue(XMLElement element, String value) {
        if (WebServicesTagNames.OPERATION_NAME.equals(element.getQName())) {
            descriptor.setOperationName(value);
        } else {
            super.setElementValue(element, value);
        }
    }    
    
    /**
     * write the descriptor class to a DOM tree and return it
     *
     * @param parent node for the DOM tree
     * @param node name for 
     * @param the descriptor to write
     * @return the DOM tree top node
     */    
    public Node writeDescriptor(Node parent, String nodeName, 
        MessageDescriptor messageDesc) {    
        Node messageNode = super.writeDescriptor(parent, nodeName,
           messageDesc);

        // for empty message case, set the method descriptor 
        // to a method descriptor with "*" as name
        if (messageDesc.getOperationName() == null && 
            messageDesc.getMethodDescriptor() == null) {
            MethodDescriptor allMethodDesc = new MethodDescriptor();
            allMethodDesc.setName(ALL_METHODS);
            messageDesc.setMethodDescriptor(allMethodDesc);
        }

        // java-method
        MethodDescriptor methodDesc = messageDesc.getMethodDescriptor();
        if (methodDesc != null) {
            MethodNode methodNode = new MethodNode();
            methodNode.writeJavaMethodDescriptor(messageNode,
                RuntimeTagNames.JAVA_METHOD, methodDesc);
        }

        // operation-name
        appendTextChild(messageNode, WebServicesTagNames.OPERATION_NAME, 
            messageDesc.getOperationName());

        return messageNode;
    }

    private void setMiscDescriptors() {
        XMLNode parentNode = 
            getParentNode().getParentNode().getParentNode();

        // get the endpoint or portinfo descriptor
        Object parentDesc = parentNode.getDescriptor();

        if (parentDesc instanceof ServiceRefPortInfo) {
            descriptor.setServiceRefPortInfo((ServiceRefPortInfo)parentDesc);
        } else if(parentDesc instanceof WebServiceEndpoint) {
            descriptor.setWebServiceEndpoint((WebServiceEndpoint)parentDesc);
        }

        // Get the bundle descriptor of which this belongs
        BundleDescriptor bundleDesc = null;
        parentNode = parentNode.getParentNode().getParentNode();
        if (parentNode.getDescriptor() instanceof WebBundleDescriptor) {
            // In the cases of used in
            // 1. webservice-endpoint for web component
            // 2. port-info for web component
            bundleDesc = 
                (WebBundleDescriptor)parentNode.getDescriptor(); 
        } else if (parentNode.getDescriptor() instanceof BundleDescriptor) {
            // In the cases of used in port-info for app client
            bundleDesc = (BundleDescriptor)parentNode.getDescriptor(); 
        } else {
            // In the case of used in webservice-endpoint for ejb component
            if (parentNode.getDescriptor() instanceof EjbDescriptor) {
                EjbDescriptor ejbDesc = 
                    (EjbDescriptor)parentNode.getDescriptor();
                bundleDesc = ejbDesc.getEjbBundleDescriptor();
            } else {
                // In the case of used in port-info for ejb component
                parentNode = parentNode.getParentNode();
                if (parentNode.getDescriptor() instanceof EjbDescriptor) {
                    EjbDescriptor ejbDesc = 
                        (EjbDescriptor)parentNode.getDescriptor();
                    bundleDesc = ejbDesc.getEjbBundleDescriptor();
                }
            }
        }
        descriptor.setBundleDescriptor(bundleDesc);
    }
}

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

package com.sun.enterprise.deployment.node.runtime.common;

import com.sun.enterprise.deployment.node.DeploymentDescriptorNode;
import com.sun.enterprise.deployment.node.XMLElement;
import com.sun.enterprise.deployment.runtime.common.MessageSecurityBindingDescriptor;
import com.sun.enterprise.deployment.runtime.common.MessageSecurityDescriptor;
import com.sun.enterprise.deployment.xml.WebServicesTagNames;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * This node handles message-security-binding element
 *
 */
public class MessageSecurityBindingNode extends DeploymentDescriptorNode {

    MessageSecurityBindingDescriptor descriptor = null;

    public MessageSecurityBindingNode() {
        registerElementHandler(new XMLElement(
            WebServicesTagNames.MESSAGE_SECURITY), MessageSecurityNode.class, 
            "addMessageSecurityDescriptor");
    }
    
    /**
    * @return the descriptor instance to associate with this XMLNode
    */    
    @Override
    public Object getDescriptor() {
       if (descriptor == null) {
            descriptor = new MessageSecurityBindingDescriptor();
        }
        return descriptor;
    }     

    /**
     * parsed an attribute of an element
     *
     * @param the element name
     * @param the attribute name
     * @param the attribute value
     * @return true if the attribute was processed
     */
    @Override
    protected boolean setAttributeValue(XMLElement elementName,
        XMLElement attributeName, String value) {
        if (attributeName.getQName().equals(WebServicesTagNames.AUTH_LAYER)) {
            descriptor.setAttributeValue(descriptor.AUTH_LAYER, value);
            return true;
        } else if (attributeName.getQName().equals(
            WebServicesTagNames.PROVIDER_ID)) {
            descriptor.setAttributeValue(descriptor.PROVIDER_ID, value);
            return true;
        }
        return false;
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
        MessageSecurityBindingDescriptor messageSecurityBindingDesc) {    
        Element messageSecurityBindingNode = (Element)super.writeDescriptor(parent, nodeName, messageSecurityBindingDesc);

        // message-security
        ArrayList messageSecDescs = 
            messageSecurityBindingDesc.getMessageSecurityDescriptors();
        if (!messageSecDescs.isEmpty()) {
            MessageSecurityNode messageSecurityNode = 
                new MessageSecurityNode();
            for (Iterator messageSecIterator = messageSecDescs.iterator();
                messageSecIterator.hasNext();) {
                MessageSecurityDescriptor messageSecDesc =
                    (MessageSecurityDescriptor) messageSecIterator.next();
                messageSecurityNode.writeDescriptor(messageSecurityBindingNode, WebServicesTagNames.MESSAGE_SECURITY, messageSecDesc);
            }
        }

        // auth-layer
        if (messageSecurityBindingDesc.getAttributeValue(
            messageSecurityBindingDesc.AUTH_LAYER) != null) {
            setAttribute(messageSecurityBindingNode, WebServicesTagNames.AUTH_LAYER, messageSecurityBindingDesc.getAttributeValue(messageSecurityBindingDesc.AUTH_LAYER));
        }
    
        // provider-id
        if (messageSecurityBindingDesc.getAttributeValue(
            messageSecurityBindingDesc.PROVIDER_ID) != null) {
            setAttribute(messageSecurityBindingNode, WebServicesTagNames.PROVIDER_ID, messageSecurityBindingDesc.getAttributeValue(messageSecurityBindingDesc.PROVIDER_ID)); 
        }   
            
        return messageSecurityBindingNode;
    }
}

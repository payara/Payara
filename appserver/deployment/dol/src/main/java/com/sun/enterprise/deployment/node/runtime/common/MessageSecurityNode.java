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
import com.sun.enterprise.deployment.runtime.common.MessageDescriptor;
import com.sun.enterprise.deployment.runtime.common.MessageSecurityDescriptor;
import com.sun.enterprise.deployment.runtime.common.ProtectionDescriptor;
import com.sun.enterprise.deployment.xml.WebServicesTagNames;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * This node handles message-security element
 *
 */
public class MessageSecurityNode extends DeploymentDescriptorNode {

    MessageSecurityDescriptor descriptor = null;

    public MessageSecurityNode() {
        registerElementHandler(new XMLElement(
            WebServicesTagNames.MESSAGE), MessageNode.class, 
            "addMessageDescriptor");
        registerElementHandler(new XMLElement(
            WebServicesTagNames.REQUEST_PROTECTION), ProtectionNode.class, 
            "setRequestProtectionDescriptor");
        registerElementHandler(new XMLElement(
            WebServicesTagNames.RESPONSE_PROTECTION), ProtectionNode.class, 
            "setResponseProtectionDescriptor");
    }
    
    /**
    * @return the descriptor instance to associate with this XMLNode
    */    
    public Object getDescriptor() {
       if (descriptor == null) {
            descriptor = new MessageSecurityDescriptor();
        }
        return descriptor;
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
        MessageSecurityDescriptor messageSecurityDesc) {    
        Node messageSecurityNode = super.writeDescriptor(parent, nodeName,
           messageSecurityDesc);

        ArrayList messageDescs =
            messageSecurityDesc.getMessageDescriptors();
        if (!messageDescs.isEmpty()) {
            MessageNode messageNode = new MessageNode();
            for (Iterator messageIterator = messageDescs.iterator();
                messageIterator.hasNext();) {
                MessageDescriptor messageDesc =
                    (MessageDescriptor) messageIterator.next();
                messageNode.writeDescriptor(messageSecurityNode,
                    WebServicesTagNames.MESSAGE, messageDesc);
            }
        }

        // request-protection
        ProtectionDescriptor requestProtectionDesc = 
            messageSecurityDesc.getRequestProtectionDescriptor();
        if (requestProtectionDesc != null) {
            ProtectionNode requestProtectionNode = new ProtectionNode();
            requestProtectionNode.writeDescriptor(messageSecurityNode,
                WebServicesTagNames.REQUEST_PROTECTION, requestProtectionDesc);
        }

        // response-protection
        ProtectionDescriptor responseProtectionDesc = 
            messageSecurityDesc.getResponseProtectionDescriptor();
        if (responseProtectionDesc != null) {
            ProtectionNode responseProtectionNode = new ProtectionNode();
            responseProtectionNode.writeDescriptor(messageSecurityNode,
                WebServicesTagNames.RESPONSE_PROTECTION, 
                    responseProtectionDesc);
        }

        return messageSecurityNode;
    }
}

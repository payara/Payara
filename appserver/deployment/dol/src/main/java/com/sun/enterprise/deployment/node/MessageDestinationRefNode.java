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

package com.sun.enterprise.deployment.node;

import java.util.Map;

import com.sun.enterprise.deployment.InjectionTarget;
import com.sun.enterprise.deployment.MessageDestinationReferenceDescriptor;
import com.sun.enterprise.deployment.xml.TagNames;
import org.w3c.dom.Node;

/**
 * This class handles all information related to the message-destination-ref 
 * xml tag
 *
 * @author  Kenneth Saks
 * @version 
 */
public class MessageDestinationRefNode extends DeploymentDescriptorNode<MessageDestinationReferenceDescriptor> {

    private MessageDestinationReferenceDescriptor descriptor;

    public MessageDestinationRefNode() {
        super();
        registerElementHandler(new XMLElement(TagNames.INJECTION_TARGET), 
                                InjectionTargetNode.class, "addInjectionTarget");
    }

    @Override
    protected Map getDispatchTable() {
        Map table = super.getDispatchTable();
        table.put(TagNames.MESSAGE_DESTINATION_REFERENCE_NAME, "setName");    
        table.put(TagNames.MESSAGE_DESTINATION_TYPE, "setDestinationType"); 
        table.put(TagNames.MESSAGE_DESTINATION_USAGE, "setUsage");
        table.put(TagNames.MESSAGE_DESTINATION_LINK, 
                  "setMessageDestinationLinkName");
        table.put(TagNames.MAPPED_NAME, "setMappedName");
        table.put(TagNames.LOOKUP_NAME, "setLookupName");
        return table;
    }

    @Override
    public MessageDestinationReferenceDescriptor getDescriptor() {
        if (descriptor == null) descriptor = new MessageDestinationReferenceDescriptor();
        return descriptor;
    }

    @Override
    public Node writeDescriptor(Node parent, String nodeName, 
                                MessageDestinationReferenceDescriptor desc) {
    
        Node msgDestRefNode = appendChild(parent, nodeName);

        writeLocalizedDescriptions(msgDestRefNode, desc);

        appendTextChild(msgDestRefNode,
                        TagNames.MESSAGE_DESTINATION_REFERENCE_NAME,
                        desc.getName()); 
        appendTextChild(msgDestRefNode, TagNames.MESSAGE_DESTINATION_TYPE, 
                        desc.getDestinationType());         
        appendTextChild(msgDestRefNode, TagNames.MESSAGE_DESTINATION_USAGE, 
                        desc.getUsage());         
        appendTextChild(msgDestRefNode, TagNames.MESSAGE_DESTINATION_LINK,
                            desc.getMessageDestinationLinkName());
        appendTextChild(msgDestRefNode, TagNames.MAPPED_NAME, 
            desc.getMappedName());

        if( desc.isInjectable() ) {
            InjectionTargetNode ijNode = new InjectionTargetNode();
            for (InjectionTarget target : desc.getInjectionTargets()) {
                ijNode.writeDescriptor(msgDestRefNode, TagNames.INJECTION_TARGET, target);
            }
        }

        appendTextChild(msgDestRefNode, TagNames.LOOKUP_NAME, 
            desc.getLookupName());

        return msgDestRefNode;
    }    
}

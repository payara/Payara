/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.deployment.node.connector;

import org.glassfish.deployment.common.Descriptor;
import com.sun.enterprise.deployment.InboundResourceAdapter;
import com.sun.enterprise.deployment.MessageListener;
import com.sun.enterprise.deployment.node.DeploymentDescriptorNode;
import com.sun.enterprise.deployment.node.DescriptorFactory;
import com.sun.enterprise.deployment.node.XMLElement;
import com.sun.enterprise.deployment.xml.ConnectorTagNames;
import org.w3c.dom.Node;

import java.util.Iterator;
import java.util.Map;

/**
 * This node is responsible for handling the Connector DTD related message-listener XML tag
 *
 * @author  Sheetal Vartak
 * @version 
 */
public class MessageListenerNode extends DeploymentDescriptorNode {
    
    private MessageListener msgListener = null;
    
    public MessageListenerNode() {
	registerElementHandler(new XMLElement(ConnectorTagNames.ACTIVATION_SPEC),
			       ActivationSpecNode.class); 
	
    }

   /**
     * all sub-implementation of this class can use a dispatch table to map xml element to
     * method name on the descriptor class for setting the element value. 
     *  
     * @return the map with the element name as a key, the setter method as a value
     */    

    protected Map getDispatchTable() {    
        Map table = super.getDispatchTable();
        table.put(ConnectorTagNames.MSG_LISTENER_TYPE, "setMessageListenerType");
	return table;
    }  

    /**
    * @return the descriptor instance to associate with this XMLNode
    */    
    public Object getDescriptor() {
        if (msgListener == null) {
            msgListener = (MessageListener) DescriptorFactory.getDescriptor(getXMLPath());
        } 
        return msgListener;
    } 

    /**
     * write the descriptor class to a DOM tree and return it
     *
     * @param parent node for the DOM tree
     * @param the descriptor to write
     * @return the DOM tree top node
     */    
    public Node writeDescriptor(Node parent, Descriptor descriptor) {

        if (! (descriptor instanceof InboundResourceAdapter)) {
            throw new IllegalArgumentException(getClass() + " cannot handle descriptors of type " + descriptor.getClass());
        }
	Iterator msgListeners = ((InboundResourceAdapter)descriptor).getMessageListeners().iterator();
	if (!msgListeners.hasNext()) {
	    throw new RuntimeException("There must be at least one messagelistener for this inbound resource adapter");
	}
	//message listeners
	for (;msgListeners.hasNext();) {
	    MessageListener msgListener = (MessageListener) msgListeners.next();
	    Node msgListenerNode = appendChild(parent, ConnectorTagNames.MSG_LISTENER);
	    appendTextChild(msgListenerNode, ConnectorTagNames.MSG_LISTENER_TYPE, msgListener.getMessageListenerType());  
	
	    //activation spec node
	    ActivationSpecNode actSpecNode = new ActivationSpecNode();
	    msgListenerNode = actSpecNode.writeDescriptor(msgListenerNode, msgListener);
	}
	return parent;
    }
}

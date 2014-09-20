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

/*
 * ConnectorNode.java. This class is responsible for encapsulating all information specific to the Connector DTD
 *
 * Created on April 18th, 2002, 4.34 PM
 */

package com.sun.enterprise.deployment.node.connector;

import com.sun.enterprise.deployment.ConnectorDescriptor;
import org.glassfish.deployment.common.Descriptor;
import com.sun.enterprise.deployment.node.DeploymentDescriptorNode;
import com.sun.enterprise.deployment.node.XMLElement;
import com.sun.enterprise.deployment.xml.ConnectorTagNames;
import org.w3c.dom.Node;

import java.util.Map;


/**
 *
 * @author Sheetal Vartak
 * @version 
 */
public  final class RANode extends DeploymentDescriptorNode {

    // Descriptor class we are using   
    private ConnectorDescriptor descriptor = null; 
    public static final String VERSION_10 = "1.0";
    public static final String VERSION_15 = "1.5";

    //default constructor
    public RANode() {
	register();
    }

    public RANode(XMLElement element) {
	this.setXMLRootTag(element);	
	register();
    }

    private void register() {
	//check for the version of DTD
	registerElementHandler(new XMLElement(ConnectorTagNames.OUTBOUND_RESOURCE_ADAPTER),
			       OutBoundRANode.class);       
	registerElementHandler(new XMLElement(ConnectorTagNames.INBOUND_RESOURCE_ADAPTER),
			       InBoundRANode.class); 
	registerElementHandler(new XMLElement(ConnectorTagNames.CONFIG_PROPERTY),
			       ConfigPropertyNode.class, "addConfigProperty"); 
	registerElementHandler(new XMLElement(ConnectorTagNames.ADMIN_OBJECT),
			       AdminObjectNode.class, "addAdminObject"); 
	registerElementHandler(new XMLElement(ConnectorTagNames.SECURITY_PERMISSION),
			       SecurityPermissionNode.class, "addSecurityPermission");         
    }

       
   /**
    * @return the descriptor instance to associate with this XMLNode
    */    
    public Object getDescriptor() {
        if (descriptor == null) {
	    descriptor = (ConnectorDescriptor)getParentNode().getDescriptor();
        } 
	return descriptor;
    } 
    
    /**
     * all sub-implementation of this class can use a dispatch table to map xml element to
     * method name on the descriptor class for setting the element value. 
     *  
     * @return the map with the element name as a key, the setter method as a value
     */    
    protected Map getDispatchTable() {
        // no need to be synchronized for now
        Map table = super.getDispatchTable();
     	table.put(ConnectorTagNames.RESOURCE_ADAPTER_CLASS, "setResourceAdapterClass");	
	return table;
    }   

    /**
     * write the descriptor class to a DOM tree and return it
     *
     * @param parent node for the DOM tree
     * @param the descriptor to write
     * @return the DOM tree top node
     */    
    public Node writeDescriptor(Node connectorNode, Descriptor descriptor) {

        if (! (descriptor instanceof ConnectorDescriptor)) {
            throw new IllegalArgumentException(getClass() + " cannot handle descriptors of type " + descriptor.getClass());
        }
        ConnectorDescriptor conDesc = (ConnectorDescriptor) descriptor;
      	Node raNode = appendChild(connectorNode, ConnectorTagNames.RESOURCE_ADAPTER);

	appendTextChild(raNode, ConnectorTagNames.RESOURCE_ADAPTER_CLASS, conDesc.getResourceAdapterClass());  
	
	//config-property
	ConfigPropertyNode config = new ConfigPropertyNode();
	raNode = config.writeDescriptor(raNode, conDesc);
	
	if (conDesc.getOutBoundDefined() == true) {
	    //outbound RA info	
	    OutBoundRANode obNode = new OutBoundRANode();
	    raNode = obNode.writeDescriptor(raNode, conDesc);
	}
	
	if (conDesc.getInBoundDefined() == true) {
	    //inbound RA info
	    InBoundRANode inNode = new InBoundRANode();
	    raNode = inNode.writeDescriptor(raNode, conDesc);	    
	}
	
	//adminobject
	AdminObjectNode admin = new AdminObjectNode();
	raNode = admin.writeDescriptor(raNode, conDesc);
	//}
        
        // security-permission*
	SecurityPermissionNode secPerm = new SecurityPermissionNode();
	raNode = secPerm.writeDescriptor(raNode, conDesc);        
			     
	return connectorNode;
    }   
}

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

package com.sun.enterprise.deployment.node.runtime.connector;

import com.sun.enterprise.deployment.ConnectorDescriptor;
import com.sun.enterprise.deployment.node.XMLElement;
import com.sun.enterprise.deployment.node.runtime.RuntimeBundleNode;
import com.sun.enterprise.deployment.runtime.connector.ResourceAdapter;
import com.sun.enterprise.deployment.runtime.connector.RoleMap;
import com.sun.enterprise.deployment.runtime.connector.SunConnector;
import com.sun.enterprise.deployment.xml.DTDRegistry;
import com.sun.enterprise.deployment.xml.RuntimeTagNames;
import org.w3c.dom.Node;

import java.util.List;
import java.util.Map;

/**
 * This node handles the sun-connector runtime deployment descriptors 
 *
 * @author  Jerome Dochez
 * @version 
 */
public class ConnectorNode extends RuntimeBundleNode<ConnectorDescriptor> {

    protected SunConnector connector=null;
    /**
     * Initialize the child handlers
     */
    public ConnectorNode(ConnectorDescriptor descriptor) {
        
	super(descriptor);

        // we do not care about our standard DDS handles
        handlers = null;
        
        registerElementHandler(new XMLElement(RuntimeTagNames.RESOURCE_ADAPTER), 
                               ResourceAdapterNode.class);
        registerElementHandler(new XMLElement(RuntimeTagNames.ROLE_MAP), 
                               RoleMapNode.class); 
    }
    
   /**
     * Adds  a new DOL descriptor instance to the descriptor instance 
     * associated with this XMLNode
     *
     * @param descriptor the new descriptor
     */
    public void addDescriptor(Object newDescriptor) {
        if (newDescriptor instanceof ResourceAdapter) {
            getSunConnectorDescriptor().setResourceAdapter(
                (ResourceAdapter)newDescriptor);
        } else if (newDescriptor instanceof RoleMap) {
            getSunConnectorDescriptor().setRoleMap(
                (RoleMap)newDescriptor);
        } else {
            super.addDescriptor(newDescriptor);
        }
    }


    /** 
     * @return the DOCTYPE that should be written to the XML file
     */
    public String getDocType() {
	return DTDRegistry.SUN_CONNECTOR_100_DTD_PUBLIC_ID;
    }
    
    /**
     * @return the SystemID of the XML file
     */
    public String getSystemID() {
	return DTDRegistry.SUN_CONNECTOR_100_DTD_SYSTEM_ID;
    }

    /**
     * @return NULL for all runtime nodes.
     */
    public List<String> getSystemIDs() {
        return null;
    }
    
    protected XMLElement getXMLRootTag() {
        return new XMLElement(RuntimeTagNames.S1AS_CONNECTOR_RUNTIME_TAG);
    }      
    
   /**
    * register this node as a root node capable of loading entire DD files
    * 
    * @param publicIDToDTD is a mapping between xml Public-ID to DTD 
    * @return the doctype tag name
    */
   public static String registerBundle(Map publicIDToDTD) {    
       publicIDToDTD.put(DTDRegistry.SUN_CONNECTOR_100_DTD_PUBLIC_ID, DTDRegistry.SUN_CONNECTOR_100_DTD_SYSTEM_ID);
       return RuntimeTagNames.S1AS_CONNECTOR_RUNTIME_TAG;
   } 
   
   /**
    * @return the descriptor instance to associate with this XMLNode
    */    
    public SunConnector getSunConnectorDescriptor() {
        if (connector==null) {
	    connector = new SunConnector();
            descriptor.setSunDescriptor(connector);
	}
        return connector;
    }

    public ConnectorDescriptor getDescriptor() {
        return descriptor;
    }

    /**
     * write the descriptor class to a DOM tree and return it
     *
     * @param parent node for the DOM tree
     * @param node name for the descriptor
     * @param the descriptor to write
     * @return the DOM tree top node
     */    
    public Node writeDescriptor(Node parent, String nodeName, ConnectorDescriptor connector) {
	Node connectorNode = super.writeDescriptor(parent, nodeName, connector);
	
	// resource-adapter
        SunConnector sunDesc = connector.getSunDescriptor();
        if (sunDesc!=null) {
            ResourceAdapterNode ran = new ResourceAdapterNode();
            ran.writeDescriptor(connectorNode, RuntimeTagNames.RESOURCE_ADAPTER, sunDesc.getResourceAdapter());
	
            // role-map ?
            if (sunDesc.getRoleMap()!=null) {
                RoleMapNode rmn = new RoleMapNode();
                rmn.writeDescriptor(connectorNode, RuntimeTagNames.ROLE_MAP, sunDesc.getRoleMap());
            }
        }
	return connectorNode;
    }
}

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
 * EjbRelationRole.java
 *
 * Created on February 1, 2002, 3:07 PM
 */

package com.sun.enterprise.deployment.node.connector;

import com.sun.enterprise.deployment.*;
import com.sun.enterprise.deployment.node.DeploymentDescriptorNode;
import com.sun.enterprise.deployment.node.DescriptorFactory;
import com.sun.enterprise.deployment.node.XMLElement;
import com.sun.enterprise.deployment.xml.ConnectorTagNames;
import org.glassfish.deployment.common.Descriptor;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;

import java.util.Iterator;
import java.util.Map;

/**
 * This node signifies the connection-definition tag in Connector DTD
 * 
 * @author Sheetal Vartak
 * @version 
 */
public class ConnectionDefNode extends DeploymentDescriptorNode {

    ConnectionDefDescriptor descriptor = null; 
   
    public final static XMLElement tag = new XMLElement(ConnectorTagNames.CONNECTION_DEFINITION);
    
    //default constructor...for normal operation in case of 1.5 DTD
    public ConnectionDefNode() {
	register();
    }

    public ConnectionDefNode(XMLElement element) {
	this.setXMLRootTag(element);
	register();
    }
    
    /**
     * method for registering the handlers with the various tags
     */
    private void register() {
	registerElementHandler(new XMLElement(ConnectorTagNames.CONFIG_PROPERTY),
			       ConfigPropertyNode.class); 
    }
        
   /**
    * @return the descriptor instance to associate with this XMLNode
    */    
    public Object getDescriptor() {
        if (descriptor==null) {
	    // the descriptor associated with the ConnectionDefNode is a ConnectionDefDescriptor 
	    // This descriptor is available with the parent node of the ConnectionDefNode

	    descriptor = (ConnectionDefDescriptor)DescriptorFactory.getDescriptor(getXMLPath());
	    ((OutboundResourceAdapter)(getParentNode().getDescriptor())).addConnectionDefDescriptor(descriptor);

	} 
        return descriptor;
    }  

    /**
     * Adds  a new DOL descriptor instance to the descriptor instance associated with 
     * this XMLNode
     *
     * @param descriptor the new descriptor
     */
    public void addDescriptor(Object obj) {
        if (obj instanceof ConnectorConfigProperty) {
            descriptor.addConfigProperty((ConnectorConfigProperty)obj);
        }
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
        	
	table.put(ConnectorTagNames.MANAGED_CONNECTION_FACTORY, "setManagedConnectionFactoryImpl");
	table.put(ConnectorTagNames.CONNECTION_FACTORY_INTF, "setConnectionFactoryIntf");
	table.put(ConnectorTagNames.CONNECTION_FACTORY_IMPL, "setConnectionFactoryImpl");
	table.put(ConnectorTagNames.CONNECTION_INTF, "setConnectionIntf");
	table.put(ConnectorTagNames.CONNECTION_IMPL, "setConnectionImpl");

        return table;
    }  

    
    
    /**
     * SAX Parser API implementation, we don't really care for now.
     */
    public void startElement(XMLElement element, Attributes attributes) {
	//FIXME : remove the foll line once connector stuff works properly
	//((ConnectionDefDescriptor)getDescriptor()).setOutBoundDefined(true);
	super.startElement(element, attributes);
    }

/**
     * write the descriptor class to a DOM tree and return it
     *
     * @param parent node for the DOM tree
     * @param the descriptor to write
     * @return the DOM tree top node
     */    
    public Node writeDescriptor(Node parent, Descriptor desc) {
	//connection definition info
	
	if (!(desc instanceof OutboundResourceAdapter)) {
            throw new IllegalArgumentException(getClass() + " cannot handle descriptors of type " + descriptor.getClass());
        }
	Iterator connectionDefs = null;
	connectionDefs = ((OutboundResourceAdapter)desc).getConnectionDefs().iterator();
	
	//connection-definitions
	for (;connectionDefs.hasNext();) {
	    ConnectionDefDescriptor con = (ConnectionDefDescriptor) connectionDefs.next();
	    Node conNode = appendChild(parent, ConnectorTagNames.CONNECTION_DEFINITION);
	    appendTextChild(conNode, ConnectorTagNames.MANAGED_CONNECTION_FACTORY, con.getManagedConnectionFactoryImpl());
	    
	    ConfigPropertyNode config = new ConfigPropertyNode();
	    conNode = config.writeDescriptor(conNode, con);
	    
	    appendTextChild(conNode, ConnectorTagNames.CONNECTION_FACTORY_INTF, con.getConnectionFactoryIntf());  
	    appendTextChild(conNode, ConnectorTagNames.CONNECTION_FACTORY_IMPL, con.getConnectionFactoryImpl());
	    appendTextChild(conNode, ConnectorTagNames.CONNECTION_INTF, con.getConnectionIntf());
	    appendTextChild(conNode, ConnectorTagNames.CONNECTION_IMPL, con.getConnectionImpl());
	}
	return parent;
    }	
}

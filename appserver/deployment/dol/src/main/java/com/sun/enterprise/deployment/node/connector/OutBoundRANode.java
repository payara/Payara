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
 * OutBoundRANode.java
 *
 * Created on February 1, 2002, 3:07 PM
 */

package com.sun.enterprise.deployment.node.connector;

import com.sun.enterprise.deployment.*;
import com.sun.enterprise.deployment.node.DeploymentDescriptorNode;
import com.sun.enterprise.deployment.node.DescriptorFactory;
import com.sun.enterprise.deployment.node.XMLElement;
import com.sun.enterprise.deployment.util.DOLUtils;
import com.sun.enterprise.deployment.xml.ConnectorTagNames;
import org.glassfish.deployment.common.Descriptor;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;

import java.util.Map;

/**
 * This node signifies the outbound-resourceadapter tag in Connector DTD
 * 
 * @author Sheetal Vartak
 * @version 
 */
public class OutBoundRANode extends DeploymentDescriptorNode {

    OutboundResourceAdapter descriptor = null; 
   
    public final static XMLElement tag = new XMLElement(ConnectorTagNames.OUTBOUND_RESOURCE_ADAPTER);
    
    //default constructor...for normal operation in case of 1.5 DTD
    public OutBoundRANode() {
	register();
    }

    public OutBoundRANode(XMLElement element) {
	this.setXMLRootTag(element);
	register();
    }

    /**
     * This method is required for 1.0 DTD so that there will be 1 instance of 
     * ConnectionDefDescriptor available
     * I know that this constructor will be called only when it is a 1.0 DD
     * dont want to rely on whether 1.0 or 1.5 spec version
     * So this method is called when the ConnectorNode knows that it is for 1.0 DTD
     */
    public void createConDefDescriptorFor10() {
	ConnectionDefDescriptor conDef = new ConnectionDefDescriptor();
	((OutboundResourceAdapter)getDescriptor()).addConnectionDefDescriptor(conDef);
    }

    /**
     * method for registering the handlers with the various tags
     */
    private void register() {
	registerElementHandler(new XMLElement(ConnectorTagNames.AUTH_MECHANISM),
			       AuthMechNode.class); 
	registerElementHandler(new XMLElement(ConnectorTagNames.CONNECTION_DEFINITION),
			       ConnectionDefNode.class);
	registerElementHandler(new XMLElement(ConnectorTagNames.CONFIG_PROPERTY),
			       ConfigPropertyNode.class); 
        registerElementHandler(new XMLElement(ConnectorTagNames.SECURITY_PERMISSION),
                               SecurityPermissionNode.class);
    }

    /**
    * @return the descriptor instance to associate with this XMLNode
    */    
    public Object getDescriptor() {
        if (descriptor==null) {
	    // the descriptor associated with the OutBoundRANode is a OutboundResourceAdapter 
	    // This descriptor is available with the parent node of the OutBoundRANode
	    descriptor = (OutboundResourceAdapter)DescriptorFactory.getDescriptor(getXMLPath());
	    ((ConnectorDescriptor)(getParentNode().getDescriptor())).setOutboundResourceAdapter(descriptor);
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
	if (obj instanceof AuthMechanism) {
	    boolean flag = descriptor.addAuthMechanism((AuthMechanism)obj);
	    if (flag == false)
		DOLUtils.getDefaultLogger().finer("The AuthMechanism object already exists in the Descriptor");
	} else if (obj instanceof ConnectionDefDescriptor) {
	    descriptor.addConnectionDefDescriptor((ConnectionDefDescriptor)obj);
	} else if (obj instanceof ConnectorConfigProperty) {
	    descriptor.addConfigProperty((ConnectorConfigProperty)obj);
	} else if (obj instanceof SecurityPermission) {
            // security-permission element is a direct sub element of 
            // resourceadapter, so set the value in ConnectorDescriptor
            ConnectorDescriptor connDesc = 
                (ConnectorDescriptor)getParentNode().getDescriptor();
	    connDesc.addSecurityPermission((SecurityPermission)obj);
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
	
	table.put(ConnectorTagNames.TRANSACTION_SUPPORT, "setTransactionSupport");
	table.put(ConnectorTagNames.REAUTHENTICATION_SUPPORT, "setReauthenticationSupport");
	
	/** The following setXXX methods are required for 1.0 DTD. For 1.5 DTD, These methods
	 * will never be used since the control will be transferred to ConnectionDefNode
	 * classes.
	 */
	table.put(ConnectorTagNames.MANAGED_CONNECTION_FACTORY, "setManagedConnectionFactoryImpl");
	
	table.put(ConnectorTagNames.CONNECTION_FACTORY_INTF, "setConnectionFactoryIntf");
	table.put(ConnectorTagNames.CONNECTION_FACTORY_IMPL, "setConnectionFactoryImpl");
	table.put(ConnectorTagNames.CONNECTION_INTF, "setConnectionIntf");
	table.put(ConnectorTagNames.CONNECTION_IMPL, "setConnectionImpl");
	
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
	//outbound RA info
	
	Node raNode = appendChild(connectorNode, ConnectorTagNames.OUTBOUND_RESOURCE_ADAPTER);
	append(raNode, (OutboundResourceAdapter)((ConnectorDescriptor)descriptor).getOutboundResourceAdapter());
	return connectorNode;	
    }
    
    /**
     * SAX Parser API implementation, we don't really care for now.
     */
    public void startElement(XMLElement element, Attributes attributes) {
    }

    /**
     * method to add the child nodes of RESOURCE_ADAPTER and OUTBOUND_RESOURCE_ADAPTER
     */
    private void append (Node raNode, OutboundResourceAdapter conDesc) {

	ConnectionDefNode conDef = new ConnectionDefNode();
	raNode = conDef.writeDescriptor(raNode, conDesc);

	appendTextChild(raNode, ConnectorTagNames.TRANSACTION_SUPPORT, conDesc.getTransSupport());

	AuthMechNode auth = new AuthMechNode();
	raNode = auth.writeDescriptor(raNode, conDesc);

	appendTextChild(raNode, ConnectorTagNames.REAUTHENTICATION_SUPPORT, conDesc.getReauthenticationSupport());

    }
}

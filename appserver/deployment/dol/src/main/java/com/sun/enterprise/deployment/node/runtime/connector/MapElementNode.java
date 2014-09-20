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

package com.sun.enterprise.deployment.node.runtime.connector;

import com.sun.enterprise.deployment.node.XMLElement;
import com.sun.enterprise.deployment.node.runtime.RuntimeDescriptorNode;
import com.sun.enterprise.deployment.runtime.connector.MapElement;
import com.sun.enterprise.deployment.runtime.connector.Principal;
import com.sun.enterprise.deployment.xml.RuntimeTagNames;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * This node handles the role-map runtime deployment descriptors 
 *
 * @author  Jerome Dochez
 * @version 
 */
public class MapElementNode extends RuntimeDescriptorNode {
    
    public MapElementNode() {
        registerElementHandler(new XMLElement(RuntimeTagNames.PRINCIPAL), 
                               PrincipalNode.class); 
        registerElementHandler(new XMLElement(RuntimeTagNames.BACKEND_PRINCIPAL), 
                               PrincipalNode.class);			       
    }
    
    
    /**
     * Adds  a new DOL descriptor instance to the descriptor instance associated with 
     * this XMLNode
     *
     * @param descriptor the new descriptor
     */
    public void addDescriptor(Object newDescriptor) {
	MapElement descriptor = (MapElement) getDescriptor();
	if (descriptor==null) {
	    throw new RuntimeException("Cannot set info on null descriptor");
	}
	if (newDescriptor instanceof Principal) {
	    Principal principal = (Principal) newDescriptor;
	    if (principal.getValue(Principal.CREDENTIAL)==null) {
		descriptor.addPrincipal(principal);
	    } else {
		descriptor.setBackendPrincipal(true);
		descriptor.setAttributeValue(MapElement.BACKEND_PRINCIPAL, Principal.USER_NAME, principal.getValue(Principal.USER_NAME));
		descriptor.setAttributeValue(MapElement.BACKEND_PRINCIPAL, Principal.PASSWORD, principal.getValue(Principal.PASSWORD));
		descriptor.setAttributeValue(MapElement.BACKEND_PRINCIPAL, Principal.CREDENTIAL, principal.getValue(Principal.CREDENTIAL));
		
	    }
	}
    }
    
    /**
     * write the descriptor class to a DOM tree and return it
     *
     * @param parent node for the DOM tree
     * @param node name for the descriptor
     * @param the descriptor to write
     * @return the DOM tree top node
     */    
    public Node writeDescriptor(Node parent, String nodeName, MapElement descriptor) {
	Node mapElementNode = super.writeDescriptor(parent, nodeName, descriptor);
	PrincipalNode pn = new PrincipalNode();
	Principal[] principals = descriptor.getPrincipal();
	for (int i=0;i<principals.length;i++) {
	    pn.writeDescriptor(mapElementNode, RuntimeTagNames.PRINCIPAL, principals[i]);
	}
	// backend-principal
	if (descriptor.isBackendPrincipal()) {
	    Element backend = (Element) appendChild(mapElementNode, RuntimeTagNames.BACKEND_PRINCIPAL);
	    setAttribute(backend, RuntimeTagNames.USER_NAME, 
	    	(String) descriptor.getAttributeValue(MapElement.BACKEND_PRINCIPAL, Principal.USER_NAME));
	    setAttribute(backend, RuntimeTagNames.PASSWORD, 
	    	(String) descriptor.getAttributeValue(MapElement.BACKEND_PRINCIPAL, Principal.PASSWORD));
	    setAttribute(backend, RuntimeTagNames.CREDENTIAL, 
	    	(String) descriptor.getAttributeValue(MapElement.BACKEND_PRINCIPAL, Principal.CREDENTIAL));
	}
		
	return mapElementNode;
    }
}

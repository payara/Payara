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

package com.sun.enterprise.deployment.node.runtime;

import com.sun.enterprise.deployment.ResourcePrincipal;
import com.sun.enterprise.deployment.ResourceReferenceDescriptor;
import com.sun.enterprise.deployment.node.DeploymentDescriptorNode;
import com.sun.enterprise.deployment.node.XMLElement;
import com.sun.enterprise.deployment.xml.RuntimeTagNames;
import org.w3c.dom.Node;

/**
 * This node handles the runtime deployment descriptor tag 
 * default-resource-principal
 *
 * @author  Jerome Dochez
 * @version 
 */
public class DefaultResourcePrincipalNode extends DeploymentDescriptorNode {

    private String name = null;
    private String passwd = null;

   /**
    * @return the descriptor instance to associate with this XMLNode
    */    
    public Object getDescriptor() {
        return null;
    }
    
    /**
     * receives notification of the value for a particular tag
     * 
     * @param element the xml element
     * @param value it's associated value
     */
    public void setElementValue(XMLElement element, String value) {
        if (RuntimeTagNames.NAME.equals(element.getQName())) {
            name = value;
        } else  if (RuntimeTagNames.PASSWORD.equals(element.getQName())) {
            passwd = value;
        } else super.setElementValue(element, value);
    }
    
    /**
     * notification of the end of XML parsing for this node
     */
    public void postParsing() {   
        if (getParentNode().getDescriptor() instanceof ResourceReferenceDescriptor) {
            ((ResourceReferenceDescriptor) getParentNode().getDescriptor()).setResourcePrincipal(new ResourcePrincipal(name, passwd));
        } else {
            getParentNode().addDescriptor(new ResourcePrincipal(name, passwd));
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
    public Node writeDescriptor(Node parent, String nodeName, ResourcePrincipal rpDescriptor) {     
        Node principalNode = super.writeDescriptor(parent, nodeName, null);
        appendTextChild(principalNode, RuntimeTagNames.NAME, rpDescriptor.getName());
	appendTextChild(principalNode, RuntimeTagNames.PASSWORD, rpDescriptor.getPassword()); 
        return principalNode;
    }    
}

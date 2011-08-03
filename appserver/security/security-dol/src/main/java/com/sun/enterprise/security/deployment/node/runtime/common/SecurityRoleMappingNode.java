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

package org.glassfish.security.deployment.node.runtime.common;

import com.sun.enterprise.deployment.node.XMLElement;
import com.sun.enterprise.deployment.node.runtime.RuntimeDescriptorNode;
import com.sun.enterprise.deployment.runtime.common.PrincipalNameDescriptor;
import com.sun.enterprise.deployment.runtime.common.SecurityRoleMapping;
import com.sun.enterprise.deployment.xml.RuntimeTagNames;
import org.w3c.dom.Node;

import java.util.List;

/**
 * This node handles all the role mapping information
 *
 * @author  Jerome Dochez
 * @version 
 */
public class SecurityRoleMappingNode extends RuntimeDescriptorNode {

    public SecurityRoleMappingNode() {
        registerElementHandler(
            new XMLElement(RuntimeTagNames.PRINCIPAL_NAME),
            PrincipalNameNode.class, "addPrincipalName");
    }
    
    /**
     * receives notiification of the value for a particular tag
     *                                               
     * @param element the xml element                
     * @param value it's associated value
     */    
    public void setElementValue(XMLElement element, String value) {   
        SecurityRoleMapping srm = (SecurityRoleMapping) getDescriptor();
        if (RuntimeTagNames.ROLE_NAME.equals(element.getQName())) {
            srm.setRoleName(value);
        } else if (RuntimeTagNames.GROUP_NAME.equals(element.getQName())) {
            srm.addGroupName(value);
        } else super.setElementValue(element, value);
    }

    /**
     * write the descriptor class to a DOM tree and return it
     *
     * @param parent node for the DOM tree
     * @param node name 
     * @param the descriptor to write
     * @return the DOM tree top node
     */    
    public Node writeDescriptor(Node parent, String nodeName, SecurityRoleMapping descriptor) {  
        Node roleMapping = appendChild(parent, nodeName);

        //role-name
        appendTextChild(roleMapping, RuntimeTagNames.ROLE_NAME, descriptor.getRoleName());

        //principal-name+
        PrincipalNameNode principal = new PrincipalNameNode();
        List<PrincipalNameDescriptor> principals = descriptor.getPrincipalNames();
	    for (int i = 0; i < principals.size(); i++) {
            principal.writeDescriptor(
                roleMapping, RuntimeTagNames.PRINCIPAL_NAME, principals.get(i));
	    }

        //group+
        List<String> groups = descriptor.getGroupNames();
        for (int i = 0; i < groups.size(); i++) {
	        appendTextChild(roleMapping, RuntimeTagNames.GROUP_NAME, groups.get(i));
        }
        return roleMapping;
    }
}

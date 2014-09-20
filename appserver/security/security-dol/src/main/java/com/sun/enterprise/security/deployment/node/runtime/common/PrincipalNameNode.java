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

package org.glassfish.security.deployment.node.runtime.common;

import com.sun.enterprise.deployment.node.XMLElement;
import com.sun.enterprise.deployment.node.runtime.RuntimeDescriptorNode;
import org.glassfish.deployment.common.RootDeploymentDescriptor;
import com.sun.enterprise.deployment.runtime.common.PrincipalNameDescriptor;
import com.sun.enterprise.deployment.xml.RuntimeTagNames;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * This node handles principal-name information
 *
 * @author deployment dev team
 */
public class PrincipalNameNode  extends RuntimeDescriptorNode {

    /**
     * receives notiification of the value for a particular tag
     *                                               
     * @param element the xml element                
     * @param value it's associated value
     */    
    public void setElementValue(XMLElement element, String value) {   
        PrincipalNameDescriptor principal = 
                (PrincipalNameDescriptor) getDescriptor();
        if (RuntimeTagNames.PRINCIPAL_NAME.equals(element.getQName())) {
            principal.setName(value);
            Object rootDesc = getParentNode().getParentNode().getDescriptor();
            if (rootDesc instanceof RootDeploymentDescriptor) {
                principal.setClassLoader(((RootDeploymentDescriptor)rootDesc).getClassLoader());
            }
        } else super.setElementValue(element, value);
    }

    /**
     * parsed an attribute of an element
     *  
     * @param the element name
     * @param the attribute name
     * @param the attribute value
     * @return true if the attribute was processed
     */ 
    protected boolean setAttributeValue(
            XMLElement element, XMLElement attribute, String value) {

        PrincipalNameDescriptor principal = 
                            (PrincipalNameDescriptor) getDescriptor();
        if (attribute.getQName().equals(RuntimeTagNames.CLASS_NAME)) {
            principal.setClassName(value);
            return true;
        }
        return false;
    }

    /**
     * write the descriptor class to a DOM tree and return it
     *
     * @param parent node for the DOM tree
     * @param node name for 
     * @param the descriptor to write
     * @return the DOM tree top node
     */    
    public Node writeDescriptor(
            Node parent, String nodeName, PrincipalNameDescriptor descriptor) {

        //principal-name
        Element principal = (Element) appendTextChild(
                parent, RuntimeTagNames.PRINCIPAL_NAME, descriptor.getName());

        // class-name
        setAttribute(principal, RuntimeTagNames.CLASS_NAME, descriptor.getClassName());

        return principal;
    }
}

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

import com.sun.enterprise.deployment.MailConfiguration;
import com.sun.enterprise.deployment.ResourcePrincipal;
import com.sun.enterprise.deployment.ResourceReferenceDescriptor;
import com.sun.enterprise.deployment.node.DeploymentDescriptorNode;
import com.sun.enterprise.deployment.node.XMLElement;
import com.sun.enterprise.deployment.types.ResourceReferenceContainer;
import com.sun.enterprise.deployment.util.DOLUtils;
import com.sun.enterprise.deployment.xml.RuntimeTagNames;
import org.w3c.dom.Node;

import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;

/**
 * This node handles the runtime deployment descriptors for resource-ref tag
 *
 * @author  Jerome Dochez
 * @version 
 */
public class ResourceRefNode extends DeploymentDescriptorNode {

    ResourceReferenceDescriptor descriptor=null;
    
    /** Creates new ResourceRefNode */
    public ResourceRefNode() {        
        registerElementHandler(new XMLElement(RuntimeTagNames.DEFAULT_RESOURCE_PRINCIPAL), 
                               DefaultResourcePrincipalNode.class, "setResourcePrincipal");                 
    }

   /**
    * @return the descriptor instance to associate with this XMLNode
    */    
    public Object getDescriptor() {
        return descriptor;
    }            
    
    /**
     * all sub-implementation of this class can use a dispatch table to map xml element to
     * method name on the descriptor class for setting the element value. 
     *  
     * @return the map with the element name as a key, the setter method as a value
     */    
    protected Map getDispatchTable() {    
        Map table = super.getDispatchTable();
        table.put(RuntimeTagNames.JNDI_NAME, "setJndiName");
        return table;
    }
    
    /**
     * receives notiification of the value for a particular tag
     * 
     * @param element the xml element
     * @param value it's associated value
     */
    public void setElementValue(XMLElement element, String value) {
        if (RuntimeTagNames.RESOURCE_REFERENCE_NAME.equals(element.getQName())) {
            Object parentDesc = getParentNode().getDescriptor();
            if (parentDesc instanceof ResourceReferenceContainer) {
                try {
                    descriptor = ((ResourceReferenceContainer) parentDesc).getResourceReferenceByName(value);
                    DOLUtils.getDefaultLogger().fine("Applying res-ref " + value + " runtime settings to " + descriptor);
                } catch (IllegalArgumentException iae) {
                    DOLUtils.getDefaultLogger().warning(iae.getMessage());
                }
            }
        } else super.setElementValue(element, value);
    }
    
    /**
     * Adds  a new DOL descriptor instance to the descriptor instance associated with 
     * this XMLNode
     *
     * @param descriptor the new descriptor
     */    
    public void addDescriptor(Object newDescriptor) {    
        if (descriptor == null) {
            DOLUtils.getDefaultLogger().log(Level.WARNING, "enterprise.deployment.backend.addDescriptorFailure",
                new Object[] {newDescriptor, this});
            return;
        }
        if (newDescriptor instanceof ResourcePrincipal) {
            descriptor.setResourcePrincipal((ResourcePrincipal) newDescriptor);
        } else if (newDescriptor instanceof MailConfiguration) {
            descriptor.setMailConfiguration((MailConfiguration) newDescriptor);
        } else {
            DOLUtils.getDefaultLogger().log(Level.SEVERE, "enterprise.deployment.backend.addDescriptorFailure",
                    new Object[]{"In " + this + " do not know what to do with " + newDescriptor});
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
    public Node writeDescriptor(Node parent, String nodeName, ResourceReferenceDescriptor rrDescriptor) {        
        Node rrNode = super.writeDescriptor(parent, nodeName, descriptor);
        appendTextChild(rrNode, RuntimeTagNames.RESOURCE_REFERENCE_NAME, rrDescriptor.getName());
        appendTextChild(rrNode, RuntimeTagNames.JNDI_NAME, rrDescriptor.getJndiName());
        if (rrDescriptor.getResourcePrincipal() != null) {
            DefaultResourcePrincipalNode drpNode = new DefaultResourcePrincipalNode();
            drpNode.writeDescriptor(rrNode, RuntimeTagNames.DEFAULT_RESOURCE_PRINCIPAL, 
                                    rrDescriptor.getResourcePrincipal());
        }
        return rrNode;
    }
    
    /**
     * writes all the runtime information for resources references
     * 
     * @param parent node to add the runtime xml info
     * @param the J2EE component containing ejb references
     */    
    public static void writeResourceReferences(Node parent, ResourceReferenceContainer descriptor) {
        
        // resource-ref*
        Iterator rrs = descriptor.getResourceReferenceDescriptors().iterator();
        if (rrs.hasNext()) {
            
            ResourceRefNode rrNode = new ResourceRefNode();                
            while (rrs.hasNext()) {
                rrNode.writeDescriptor(parent, RuntimeTagNames.RESOURCE_REFERENCE,
                    (ResourceReferenceDescriptor) rrs.next());
            }
        }  
    }    
}

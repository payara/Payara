/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.deployment.node.DeploymentDescriptorNode;
import com.sun.enterprise.deployment.node.XMLElement;
import com.sun.enterprise.deployment.types.EjbReference;
import com.sun.enterprise.deployment.types.EjbReferenceContainer;
import com.sun.enterprise.deployment.util.DOLUtils;
import com.sun.enterprise.deployment.xml.RuntimeTagNames;
import com.sun.enterprise.deployment.EjbSessionDescriptor;
import com.sun.enterprise.deployment.EjbDescriptor;
import org.w3c.dom.Node;

import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;

/**
 * This node class is responsible for handling runtime deployment descriptors
 * for ejb-ref
 *
 * @author Jerome Dochez
 * @version 
 */
public class EjbRefNode extends DeploymentDescriptorNode<EjbReference> {

    EjbReference descriptor=null;
    
   /**
    * @return the descriptor instance to associate with this XMLNode
    */    
   public EjbReference getDescriptor() {
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
        if (RuntimeTagNames.EJB_REFERENCE_NAME.equals(element.getQName())) {
            Object parentDesc = getParentNode().getDescriptor();
            if (parentDesc instanceof EjbReferenceContainer) {
                try {
                    descriptor = ((EjbReferenceContainer) parentDesc).getEjbReference(value);
                    DOLUtils.getDefaultLogger().finer("Applying ref runtime to " + descriptor);
                } catch (IllegalArgumentException iae) {
                    DOLUtils.getDefaultLogger().warning(iae.getMessage());
                }
            }
            if (descriptor==null) {
                DOLUtils.getDefaultLogger().log(Level.SEVERE, "enterprise.deployment.backend.addDescriptorFailure",
                        new Object[]{"ejb-ref" , value });
            }
        } else super.setElementValue(element, value);
    }
    
    /**
     * write the descriptor class to a DOM tree and return it
     *
     * @param parent node for the DOM tree
     * @param node name for the descriptor
     * @param the descriptor to write
     * @return the DOM tree top node
     */    
    public Node writeDescriptor(Node parent, String nodeName, EjbReference ejbRef) {        
        Node ejbRefNode = appendChild(parent, nodeName);
        appendTextChild(ejbRefNode, RuntimeTagNames.EJB_REFERENCE_NAME, ejbRef.getName());

        String jndiName = ejbRef.getJndiName();

        EjbDescriptor ejbReferee = ejbRef.getEjbDescriptor();

        // If this is an intra-app remote ejb dependency, write out the portable jndi name
        // of the target ejb.
        if( ejbReferee != null ) {
            if( !ejbRef.isLocal() && ejbRef.getType().equals(EjbSessionDescriptor.TYPE) ) {
               EjbSessionDescriptor sessionDesc = (EjbSessionDescriptor) ejbReferee;
               String intf = ejbRef.isEJB30ClientView() ?
                        ejbRef.getEjbInterface() : ejbRef.getEjbHomeInterface();
               jndiName = sessionDesc.getPortableJndiName(intf);
            }
        }
        appendTextChild(ejbRefNode, RuntimeTagNames.JNDI_NAME, jndiName);

        return ejbRefNode;
    }
    
    /**
     * writes all the runtime information for ejb references
     * 
     * @param parent node to add the runtime xml info
     * @param the J2EE component containing ejb references
     */
    public static void writeEjbReferences(Node parent, EjbReferenceContainer descriptor) {
        
        // ejb-ref*
        Iterator ejbRefs = descriptor.getEjbReferenceDescriptors().iterator();
        if (ejbRefs.hasNext()) {
            EjbRefNode refNode = new EjbRefNode();
            while (ejbRefs.hasNext()) {
                EjbReference ejbRef = (EjbReference) ejbRefs.next();
                if (!ejbRef.isLocal()) {
                    refNode.writeDescriptor(parent, RuntimeTagNames.EJB_REFERENCE, ejbRef );
                }
            }
        }          
    }    
}

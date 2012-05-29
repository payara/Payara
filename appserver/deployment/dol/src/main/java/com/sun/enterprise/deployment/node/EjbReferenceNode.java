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

package com.sun.enterprise.deployment.node;

import org.glassfish.deployment.common.Descriptor;
import com.sun.enterprise.deployment.EnvironmentProperty;
import com.sun.enterprise.deployment.InjectionTarget;
import com.sun.enterprise.deployment.types.EjbReference;
import com.sun.enterprise.deployment.xml.EjbTagNames;
import com.sun.enterprise.deployment.xml.TagNames;
import org.w3c.dom.Node;

import java.util.Map;

/**
 * This class handles all information in the ejb-reference xml node
 *
 * @author  Jerome Dochez
 * @version 
 */
public class EjbReferenceNode extends DeploymentDescriptorNode {

    protected EjbReference descriptor;
    
    public EjbReferenceNode() {
        super();
        registerElementHandler(new XMLElement(TagNames.INJECTION_TARGET), 
                                InjectionTargetNode.class, "addInjectionTarget");                          
    }
        
    /**
    * @return the descriptor instance to associate with this XMLNode
    */    
    public Object getDescriptor() {
        if (descriptor==null) {
            descriptor = (EjbReference) DescriptorFactory.getDescriptor(getXMLPath());
            descriptor.setLocal(false);   
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
        Map table = super.getDispatchTable();
        table.put(EjbTagNames.EJB_REFERENCE_NAME, "setName");
        table.put(EjbTagNames.EJB_REFERENCE_TYPE, "setType");
        table.put(EjbTagNames.HOME, "setEjbHomeInterface");
        table.put(EjbTagNames.REMOTE, "setEjbInterface");
        table.put(EjbTagNames.LOCAL_HOME, "setEjbHomeInterface");
        table.put(EjbTagNames.LOCAL, "setEjbInterface");        
        table.put(EjbTagNames.EJB_LINK, "setLinkName");
        table.put(TagNames.MAPPED_NAME, "setMappedName");
        table.put(TagNames.LOOKUP_NAME, "setLookupName");
        return table;
    }        

    /**
     * write the descriptor class to a DOM tree and return it
     *
     * @param parent node in the DOM tree 
     * @param node name for the root element of this xml fragment      
     * @param the descriptor to write
     * @return the DOM tree top node
     */
    public Node writeDescriptor(Node parent, String nodeName, EjbReference descriptor) {    
        Node ejbRefNode = appendChild(parent, nodeName);
        if (descriptor instanceof Descriptor) {
            Descriptor ejbRefDesc = (Descriptor)descriptor;
            writeLocalizedDescriptions(ejbRefNode, ejbRefDesc);
        }
        appendTextChild(ejbRefNode, EjbTagNames.EJB_REFERENCE_NAME, descriptor.getName());
        appendTextChild(ejbRefNode, EjbTagNames.EJB_REFERENCE_TYPE, descriptor.getType());
        if (descriptor.isLocal()) {
            appendTextChild(ejbRefNode, EjbTagNames.LOCAL_HOME, descriptor.getEjbHomeInterface());
            appendTextChild(ejbRefNode, EjbTagNames.LOCAL, descriptor.getEjbInterface());
        } else {
            appendTextChild(ejbRefNode, EjbTagNames.HOME, descriptor.getEjbHomeInterface());                
            appendTextChild(ejbRefNode, EjbTagNames.REMOTE, descriptor.getEjbInterface());
        }
        appendTextChild(ejbRefNode, EjbTagNames.EJB_LINK, descriptor.getLinkName());        

        if( descriptor instanceof EnvironmentProperty) {
            EnvironmentProperty envProp = (EnvironmentProperty)descriptor;
            appendTextChild(ejbRefNode, TagNames.MAPPED_NAME, envProp.getMappedName()); 
        }
        if( descriptor.isInjectable() ) {
            InjectionTargetNode ijNode = new InjectionTargetNode();
            for (InjectionTarget target : descriptor.getInjectionTargets()) {
                ijNode.writeDescriptor(ejbRefNode, TagNames.INJECTION_TARGET, target);
            }
        }

        if( descriptor.hasLookupName() ) {
            appendTextChild(ejbRefNode, TagNames.LOOKUP_NAME, descriptor.getLookupName());
        }

        return ejbRefNode;
    }        
        
}

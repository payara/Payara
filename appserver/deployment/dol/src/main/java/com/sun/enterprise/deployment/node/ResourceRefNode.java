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

package com.sun.enterprise.deployment.node;

import java.util.Map;

import com.sun.enterprise.deployment.InjectionTarget;
import com.sun.enterprise.deployment.ResourceReferenceDescriptor;
import com.sun.enterprise.deployment.xml.TagNames;
import org.w3c.dom.Node;

/**
 * This node handles all resource-ref xml tag elements
 * 
 * @author  Jerome Dochez
 * @version 
 */
public class ResourceRefNode  extends DeploymentDescriptorNode<ResourceReferenceDescriptor> {

    private ResourceReferenceDescriptor descriptor;

    public ResourceRefNode() {
        super();
        registerElementHandler(new XMLElement(TagNames.INJECTION_TARGET), 
                                InjectionTargetNode.class, "addInjectionTarget");
    }

    @Override
    public ResourceReferenceDescriptor getDescriptor() {
        if (descriptor == null) descriptor = new ResourceReferenceDescriptor();
        return descriptor;
    }

    @Override
    protected Map getDispatchTable() {
        // no need to be synchronized for now
        Map table = super.getDispatchTable();
        table.put(TagNames.RESOURCE_REFERENCE_NAME, "setName");
        table.put(TagNames.RESOURCE_TYPE, "setType");        
        table.put(TagNames.RESOURCE_AUTHORIZATION, "setAuthorization");
        table.put(TagNames.RESOURCE_SHARING_SCOPE, "setSharingScope");
        table.put(TagNames.MAPPED_NAME, "setMappedName");
        table.put(TagNames.LOOKUP_NAME, "setLookupName");
        return table;
    }

    @Override
    public Node writeDescriptor(Node parent, String nodeName, ResourceReferenceDescriptor descriptor) {    
        Node ejbResNode = appendChild(parent, nodeName);
        writeLocalizedDescriptions(ejbResNode, descriptor);
        
        appendTextChild(ejbResNode, TagNames.RESOURCE_REFERENCE_NAME, descriptor.getName());      
        appendTextChild(ejbResNode, TagNames.RESOURCE_TYPE, descriptor.getType());      
        appendTextChild(ejbResNode, TagNames.RESOURCE_AUTHORIZATION, descriptor.getAuthorization());               
        appendTextChild(ejbResNode, TagNames.RESOURCE_SHARING_SCOPE, descriptor.getSharingScope());  
        appendTextChild(ejbResNode, TagNames.MAPPED_NAME, descriptor.getMappedName());
        if( descriptor.isInjectable() ) {
            InjectionTargetNode ijNode = new InjectionTargetNode();
            for (InjectionTarget target : descriptor.getInjectionTargets()) {
                ijNode.writeDescriptor(ejbResNode, TagNames.INJECTION_TARGET, target);
            }
        }
        appendTextChild(ejbResNode, TagNames.LOOKUP_NAME, descriptor.getLookupName());
            
        return ejbResNode;
    }
}

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

package org.glassfish.ejb.deployment.node;

import org.glassfish.deployment.common.Descriptor;
import com.sun.enterprise.deployment.EjbDescriptor;
import com.sun.enterprise.deployment.EjbInterceptor;
import com.sun.enterprise.deployment.LifecycleCallbackDescriptor;
import com.sun.enterprise.deployment.node.DeploymentDescriptorNode;
import com.sun.enterprise.deployment.xml.EjbTagNames;
import org.w3c.dom.Node;

import java.util.Map;


/**
 * This node handles all information relative to injection-complete xml tag
 */
public class AroundInvokeNode extends DeploymentDescriptorNode {

    private LifecycleCallbackDescriptor descriptor;
    
   /**
    * @return the descriptor instance to associate with this XMLNode
    */       
    public Object getDescriptor() {
        
       if (descriptor==null) {
            descriptor = new LifecycleCallbackDescriptor();
            Descriptor parentDesc = 
                (Descriptor)getParentNode().getDescriptor();
            if (parentDesc instanceof EjbDescriptor) {
                EjbDescriptor ejbDesc = (EjbDescriptor)parentDesc;
                descriptor.setDefaultLifecycleCallbackClass(
                    ejbDesc.getEjbClassName());
            } else if (parentDesc instanceof EjbInterceptor) {
                EjbInterceptor ejbInterceptor = 
                    (EjbInterceptor)parentDesc;
                descriptor.setDefaultLifecycleCallbackClass(
                    ejbInterceptor.getInterceptorClassName());
            } 
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
        table.put(EjbTagNames.AROUND_INVOKE_CLASS_NAME, 
            "setLifecycleCallbackClass");
        table.put(EjbTagNames.AROUND_INVOKE_METHOD_NAME, 
            "setLifecycleCallbackMethod");
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
    public Node writeDescriptor(Node parent, String nodeName, LifecycleCallbackDescriptor descriptor) {
        Node myNode = appendChild(parent, nodeName);
        appendTextChild(myNode, EjbTagNames.AROUND_INVOKE_CLASS_NAME, 
            descriptor.getLifecycleCallbackClass());                        
        appendTextChild(myNode, EjbTagNames.AROUND_INVOKE_METHOD_NAME, 
            descriptor.getLifecycleCallbackMethod());                        
        return myNode;
    }
}

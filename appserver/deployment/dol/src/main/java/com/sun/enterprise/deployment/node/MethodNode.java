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

import com.sun.enterprise.deployment.MethodDescriptor;
import com.sun.enterprise.deployment.xml.RuntimeTagNames;
import com.sun.enterprise.deployment.xml.TagNames;
import org.w3c.dom.Node;

/**
 * This class handle the method element 
 *
 * @author  Jerome Dochez
 * @version 
 */
public class MethodNode extends DeploymentDescriptorNode<MethodDescriptor> {

    private MethodDescriptor descriptor;

    @Override
    public MethodDescriptor getDescriptor() {
        if (descriptor == null) descriptor = new MethodDescriptor();
        return descriptor;
    }

    @Override
    protected Map getDispatchTable() {
        Map table = super.getDispatchTable();
        table.put(TagNames.EJB_NAME, "setEjbName");
        table.put(TagNames.METHOD_INTF, "setEjbClassSymbol");
        table.put(TagNames.METHOD_NAME, "setName");
        table.put(TagNames.METHOD_PARAM, "addParameterClass");        
        return table;
    }

    @Override
    public boolean endElement(XMLElement element) {
        String qname = element.getQName();
        if (TagNames.METHOD_PARAMS.equals(qname)) {
            MethodDescriptor desc = getDescriptor();
            // this means we have an empty method-params element
            // which means this method has no input parameter
            if (desc.getParameterClassNames() == null) {
                desc.setEmptyParameterClassNames();
            }
        }
        return super.endElement(element);
    }


    public Node writeDescriptor(Node parent, String nodeName, MethodDescriptor descriptor, String ejbName) {        
        Node methodNode = super.writeDescriptor(parent, nodeName, descriptor);    
        writeLocalizedDescriptions(methodNode, descriptor);
        if (ejbName != null && ejbName.length() > 0)  {
            appendTextChild(methodNode, TagNames.EJB_NAME, ejbName);        
        }
        String methodIntfSymbol = descriptor.getEjbClassSymbol();
        if( (methodIntfSymbol != null) &&
            !methodIntfSymbol.equals(MethodDescriptor.EJB_BEAN) ) {
            appendTextChild(methodNode, TagNames.METHOD_INTF, 
                            methodIntfSymbol);
        }
        appendTextChild(methodNode, TagNames.METHOD_NAME, descriptor.getName());
        if (descriptor.getParameterClassNames()!=null) {
            Node paramsNode = appendChild(methodNode, TagNames.METHOD_PARAMS);            
            writeMethodParams(paramsNode, descriptor);                
        }
        return methodNode;
    }
    
    /**
     * write the method descriptor class to a query-method DOM tree and return it
     *
     * @param parent node in the DOM tree 
     * @param node name for the root element of this xml fragment      
     * @param the descriptor to write
     * @return the DOM tree top node
     */
    public Node writeQueryMethodDescriptor(Node parent, String nodeName, MethodDescriptor descriptor) {        
        Node methodNode = super.writeDescriptor(parent, nodeName, descriptor);
        appendTextChild(methodNode, TagNames.METHOD_NAME, descriptor.getName());
        Node paramsNode = appendChild(methodNode, TagNames.METHOD_PARAMS);        
        writeMethodParams(paramsNode, descriptor);           
        return methodNode;
    }
    
    /**
     * write the method descriptor class to a java-method DOM tree and return it
     *
     * @param parent node in the DOM tree
     * @param node name for the root element of this xml fragment
     * @param the descriptor to write
     * @return the DOM tree top node
     */
    public Node writeJavaMethodDescriptor(Node parent, String nodeName, 
        MethodDescriptor descriptor) {
        // Write out the java method description.  In the case of a void
        // method, a <method-params> element will *not* be written out.
        return writeJavaMethodDescriptor(parent, nodeName, descriptor, false);
    }   

    /**
     * write the method descriptor class to a java-method DOM tree and return it
     *
     * @param parent node in the DOM tree
     * @param node name for the root element of this xml fragment
     * @param the descriptor to write
     * @return the DOM tree top node
     */
    public Node writeJavaMethodDescriptor(Node parent, String nodeName, 
        MethodDescriptor descriptor, 
        boolean writeEmptyMethodParamsElementForVoidMethods) {
        Node methodNode = super.writeDescriptor(parent, nodeName, descriptor);
        appendTextChild(methodNode, RuntimeTagNames.METHOD_NAME, 
            descriptor.getName());    
        if (descriptor.getParameterClassNames() != null) {
            Node paramsNode = 
                appendChild(methodNode, RuntimeTagNames.METHOD_PARAMS); 
            writeMethodParams(paramsNode, descriptor);            
        } else {
            if( writeEmptyMethodParamsElementForVoidMethods ) {
                appendChild(methodNode, RuntimeTagNames.METHOD_PARAMS); 
            }
        }
        return methodNode;
    }   


    /**
     * writes the method parameters to the DOM Tree
     * 
     * @param the parent node for the parameters
     * @param the method descriptor
     */
    private void writeMethodParams(Node paramsNode, MethodDescriptor descriptor) {
        String[] params = descriptor.getParameterClassNames();
        if (params==null) 
            return;
        for (int i=0; i<params.length;i++) {
            appendTextChild(paramsNode, RuntimeTagNames.METHOD_PARAM, params[i]);
        }
    }
}
   

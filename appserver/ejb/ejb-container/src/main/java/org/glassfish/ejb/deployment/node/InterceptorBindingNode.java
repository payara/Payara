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

import java.util.List;
import java.util.Map;

import com.sun.enterprise.deployment.EjbInterceptor;
import com.sun.enterprise.deployment.MethodDescriptor;
import com.sun.enterprise.deployment.node.DeploymentDescriptorNode;
import com.sun.enterprise.deployment.node.MethodNode;
import com.sun.enterprise.deployment.node.XMLElement;
import org.glassfish.ejb.deployment.EjbTagNames;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.InterceptorBindingDescriptor;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;

public class InterceptorBindingNode extends DeploymentDescriptorNode<InterceptorBindingDescriptor> {

    private MethodDescriptor businessMethod = null;
    private boolean needsOverloadResolution = false;
    private InterceptorBindingDescriptor descriptor;

    public InterceptorBindingNode() {
        super();
    }

    @Override
    public InterceptorBindingDescriptor getDescriptor() {
        if (descriptor == null) descriptor = new InterceptorBindingDescriptor();
        return descriptor;
    }

    @Override
    public void startElement(XMLElement element, Attributes attributes) {

        if( EjbTagNames.METHOD.equals(element.getQName()) ) {

            businessMethod = new MethodDescriptor();
            // Assume we need overloaded method resolution until we
            // encounter at least one method-param element.
            needsOverloadResolution = true;

        } else if( EjbTagNames.METHOD_PARAMS.equals(element.getQName()) ) {

            // If there's a method-params element, regardless of whether there
            // are any <method-param> sub-elements, there's no overload
            // resolution needed.
            needsOverloadResolution = false;

        }
          
        super.startElement(element, attributes);
    }

    @Override
    public void setElementValue(XMLElement element, String value) {
        if( EjbTagNames.METHOD_NAME.equals(element.getQName()) ) {
            businessMethod.setName(value);
        } else if( EjbTagNames.METHOD_PARAM.equals(element.getQName()) ) {
            if( (value != null) && (value.trim().length() > 0) ) {
                businessMethod.addParameterClass(value.trim());
            }
        } else {
            super.setElementValue(element, value);
        }

    }                        



    /** 
     * receives notification of the end of an XML element by the Parser
     * 
     * @param element the xml tag identification
     * @return true if this node is done processing the XML sub tree
     */
    @Override
    public boolean endElement(XMLElement element) {

        if (EjbTagNames.INTERCEPTOR_ORDER.equals(element.getQName())) {

            InterceptorBindingDescriptor desc = getDescriptor();
            desc.setIsTotalOrdering(true);

        } else if (EjbTagNames.METHOD_PARAMS.equals(element.getQName())) {
            // this means we have an empty method-params element
            // which means this method has no input parameter
            if (businessMethod.getParameterClassNames() == null) {
                businessMethod.setEmptyParameterClassNames();
            }
        }else if( EjbTagNames.METHOD.equals(element.getQName()) ) {

            InterceptorBindingDescriptor bindingDesc = getDescriptor();
            businessMethod.setEjbClassSymbol(MethodDescriptor.EJB_BEAN);
            bindingDesc.setBusinessMethod(businessMethod);
            
            if( needsOverloadResolution ) {
                bindingDesc.setNeedsOverloadResolution(true);
            }

            businessMethod = null;
            needsOverloadResolution = false;

        }

        return super.endElement(element);
    } 

    /**
     * all sub-implementation of this class can use a dispatch table to map xml element to
     * method name on the descriptor class for setting the element value. 
     *  
     * @return the map with the element name as a key, the setter method as a value
     */
    @Override
    protected Map getDispatchTable() {
        Map table =  super.getDispatchTable();

        table.put(EjbTagNames.EJB_NAME, "setEjbName");
        table.put(EjbTagNames.INTERCEPTOR_CLASS, "appendInterceptorClass");
        table.put(EjbTagNames.EXCLUDE_DEFAULT_INTERCEPTORS, 
                  "setExcludeDefaultInterceptors");
        table.put(EjbTagNames.EXCLUDE_CLASS_INTERCEPTORS, 
                  "setExcludeClassInterceptors");

        return table;
    }    
    
    /**
     * Write interceptor bindings for this ejb.
     *
     * @param parent node in the DOM tree 
     * @param the descriptor to write
     */
    public void writeBindings(Node parent, EjbDescriptor ejbDesc) {

        List<EjbInterceptor> classInterceptors = ejbDesc.getInterceptorChain();
        if( classInterceptors.size() > 0 ) {
            writeTotalOrdering(parent, classInterceptors, ejbDesc, null);
        }

        Map<MethodDescriptor, List<EjbInterceptor>> methodInterceptorsMap =
            ejbDesc.getMethodInterceptorsMap();

        for(Map.Entry<MethodDescriptor, List<EjbInterceptor>> mapEntry:
            methodInterceptorsMap.entrySet()) {
            List<EjbInterceptor> interceptors = mapEntry.getValue();
            
            if(interceptors.isEmpty()) {
                writeExclusionBinding(parent, ejbDesc, mapEntry.getKey());
            } else {
                writeTotalOrdering(parent, interceptors, ejbDesc, mapEntry.getKey());
            }
        }

        return;
    }

    private void writeTotalOrdering(Node parent, 
                                    List<EjbInterceptor> interceptors,
                                    EjbDescriptor ejbDesc,
                                    MethodDescriptor method) {

        Node bindingNode = appendChild(parent, 
                                       EjbTagNames.INTERCEPTOR_BINDING);

        appendTextChild(bindingNode, EjbTagNames.EJB_NAME, 
                        ejbDesc.getName());

        Node totalOrderingNode = appendChild(bindingNode,
                                             EjbTagNames.INTERCEPTOR_ORDER);
        for(EjbInterceptor next : interceptors) {

            appendTextChild(totalOrderingNode, EjbTagNames.INTERCEPTOR_CLASS,
                            next.getInterceptorClassName());
        }

        if( method != null ) {
            
            MethodNode methodNode = new MethodNode();

            // Write out method description. void methods will be written
            // out using an empty method-params element so they will not
            // be interpreted as overloaded when processed.
            methodNode.writeJavaMethodDescriptor
                (bindingNode, EjbTagNames.INTERCEPTOR_BUSINESS_METHOD, method,
                 true);

        }

    }

    private void writeExclusionBinding(Node parent, EjbDescriptor ejbDesc, 
                                       MethodDescriptor method) {

        Node bindingNode = appendChild(parent, 
                                       EjbTagNames.INTERCEPTOR_BINDING);

        appendTextChild(bindingNode, EjbTagNames.EJB_NAME, 
                        ejbDesc.getName());

        appendTextChild(bindingNode, EjbTagNames.EXCLUDE_CLASS_INTERCEPTORS,
                        "true");

        MethodNode methodNode = new MethodNode();

        // Write out method description. void methods will be written
        // out using an empty method-params element so they will not
        // be interpreted as overloaded when processed.
        methodNode.writeJavaMethodDescriptor
            (bindingNode, EjbTagNames.INTERCEPTOR_BUSINESS_METHOD, method,
             true);
                                             
    }
}

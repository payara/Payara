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

import com.sun.enterprise.deployment.ApplicationClientDescriptor;
import com.sun.enterprise.deployment.node.DeploymentDescriptorNode;
import com.sun.enterprise.deployment.node.XMLElement;
import com.sun.enterprise.deployment.node.XMLNode;
import com.sun.enterprise.deployment.runtime.JavaWebStartAccessDescriptor;
import com.sun.enterprise.deployment.xml.RuntimeTagNames;
import org.w3c.dom.Node;

import java.util.Map;

/**
 *
 * @author tjquinn
 */
public class JavaWebStartAccessNode extends DeploymentDescriptorNode<JavaWebStartAccessDescriptor> {
    
    protected JavaWebStartAccessDescriptor descriptor;
    
    /** Creates a new instance of JavaWebStartAccessNode */
    public JavaWebStartAccessNode() {
        handlers = null;
        registerElementHandler(new XMLElement(RuntimeTagNames.JNLP_DOC),
                               JnlpDocNode.class);
    }

//    public JavaWebStartAccessNode(XMLElement element) {
//        register();
//        setXMLRootTag(element);
//    }

   /**
    * @return the descriptor instance to associate with this XMLNode
    */    
    @Override
    public JavaWebStartAccessDescriptor getDescriptor() {
	if (descriptor==null) {
	    descriptor = new JavaWebStartAccessDescriptor();
            XMLNode parentNode = getParentNode();
            if (parentNode != null && (parentNode instanceof AppClientRuntimeNode)) {
                Object parentDescriptor = parentNode.getDescriptor();
                if (parentDescriptor != null && (parentDescriptor instanceof ApplicationClientDescriptor) ) {
                    ApplicationClientDescriptor acDescriptor = (ApplicationClientDescriptor) parentDescriptor;
                    acDescriptor.setJavaWebStartAccessDescriptor(descriptor);
                }
                
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
    @Override
    protected Map getDispatchTable() {
        Map table =  super.getDispatchTable();
        table.put(RuntimeTagNames.CONTEXT_ROOT, "setContextRoot");
        table.put(RuntimeTagNames.ELIGIBLE, "setEligible");
        table.put(RuntimeTagNames.VENDOR, "setVendor");
        table.put(RuntimeTagNames.JNLP_DOC, "setJnlpDocument");
        return table;
    }

    /**
     * write the descriptor class to a DOM tree and return it
     *
     * @param parent node for the DOM tree
     * @param node name for the descriptor
     * @param the descriptor to write
     * @return the DOM tree top node
     */    
    public Node writeDescriptor(Node parent, String nodeName, JavaWebStartAccessDescriptor descr) {    
	Node accessNode = super.writeDescriptor(parent, nodeName, descr);
        appendTextChild(accessNode, RuntimeTagNames.CONTEXT_ROOT, descr.getContextRoot());
        appendTextChild(accessNode, RuntimeTagNames.ELIGIBLE, Boolean.toString(descr.isEligible()));
        appendTextChild(accessNode, RuntimeTagNames.VENDOR, descr.getVendor());
        appendTextChild(accessNode, RuntimeTagNames.JNLP_DOC, descr.getJnlpDocument());
	return accessNode;
    }    
    
    public static void writeJavaWebStartInfo(Node parent, JavaWebStartAccessDescriptor descr) {
        if (descr != null) {
            JavaWebStartAccessNode newNode = new JavaWebStartAccessNode();
            newNode.writeDescriptor(parent, RuntimeTagNames.JAVA_WEB_START_ACCESS, descr);
        }
    }
}

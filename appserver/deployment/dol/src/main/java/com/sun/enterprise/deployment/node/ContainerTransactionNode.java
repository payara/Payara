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

package com.sun.enterprise.deployment.node;

import com.sun.enterprise.deployment.ContainerTransaction;
import com.sun.enterprise.deployment.EjbBundleDescriptor;
import com.sun.enterprise.deployment.EjbDescriptor;
import com.sun.enterprise.deployment.MethodDescriptor;
import com.sun.enterprise.deployment.xml.EjbTagNames;
import org.w3c.dom.Node;

import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
/**
 * This node is responsible for handling the container-transaction XML node
 *
 * @author  Jerome Dochez
 * @version 
 */
public class ContainerTransactionNode extends DeploymentDescriptorNode {

    private String trans_attribute;
    private String description;    
    private Vector methods = new Vector();
    
    /** Creates new ContainerTransactionNode */
    public ContainerTransactionNode() {
       registerElementHandler(new XMLElement(EjbTagNames.METHOD), MethodNode.class);                         
    }
    
    /**
     * Adds  a new DOL descriptor instance to the descriptor instance associated with 
     * this XMLNode
     *
     * @param descriptor the new descriptor
     */    
    public void addDescriptor(Object newDescriptor) {
        if (newDescriptor instanceof MethodDescriptor) {
            methods.add(newDescriptor);
        }
    }

    /**
     * @return the Descriptor subclass that was populated  by reading
     * the source XML file
     */
    public Object getDescriptor() {
        return null;
    }
    
    /** 
     * receives notification of the end of an XML element by the Parser
     * 
     * @param element the xml tag identification
     * @return true if this node is done processing the XML sub tree
     */    
    public boolean endElement(XMLElement element) {
        boolean doneWithNode = super.endElement(element);
        
        if (doneWithNode) {
            ContainerTransaction ct =  new ContainerTransaction(trans_attribute, description);
            for (Iterator methodsIterator = methods.iterator();methodsIterator.hasNext();) {
                MethodDescriptor md = (MethodDescriptor) methodsIterator.next();
                EjbBundleDescriptor bundle = (EjbBundleDescriptor) getParentNode().getDescriptor();
                EjbDescriptor ejb = bundle.getEjbByName(md.getEjbName(), true);
                ejb.getMethodContainerTransactions().put(md, ct);
            }
        }        
        return doneWithNode;
    }
    
    /**
     * receives notiification of the value for a particular tag
     * 
     * @param element the xml element
     * @param value it's associated value
     */    
    public void setElementValue(XMLElement element, String value) {
        if (EjbTagNames.DESCRIPTION.equals(element.getQName())) {
            description = value;
        } 
        if (EjbTagNames.TRANSACTION_ATTRIBUTE.equals(element.getQName())) {
            trans_attribute = value;
        }
    }        
    
    /**
     * write the descriptor class to a DOM tree and return it
     *
     * @param parent node in the DOM tree 
     * @param node name for the root element of this xml fragment      
     * @param the descriptor to write
     * @return the DOM tree top node
     */
    public Node writeDescriptor(Node parent, String nodeName, EjbDescriptor ejb) {    
        
        Map methodToTransactions = ejb.getMethodContainerTransactions();
        MethodNode mn = new MethodNode();
        for (Iterator e=methodToTransactions.keySet().iterator();e.hasNext();) {
            MethodDescriptor md = (MethodDescriptor) e.next();
            Node ctNode = super.writeDescriptor(parent, nodeName, ejb);            
            ContainerTransaction ct = (ContainerTransaction) methodToTransactions.get(md);
            appendTextChild(ctNode, EjbTagNames.DESCRIPTION, ct.getDescription());
            mn.writeDescriptor(ctNode, EjbTagNames.METHOD, md, ejb.getName());
            appendTextChild(ctNode, EjbTagNames.TRANSACTION_ATTRIBUTE, ct.getTransactionAttribute());
        }
        return null;
    }
}

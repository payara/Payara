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

import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import com.sun.enterprise.deployment.MethodDescriptor;
import com.sun.enterprise.deployment.node.DeploymentDescriptorNode;
import com.sun.enterprise.deployment.node.MethodNode;
import com.sun.enterprise.deployment.node.XMLElement;
import com.sun.enterprise.deployment.xml.TagNames;
import org.glassfish.ejb.deployment.EjbTagNames;
import org.glassfish.ejb.deployment.descriptor.ContainerTransaction;
import org.glassfish.ejb.deployment.descriptor.EjbBundleDescriptorImpl;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.w3c.dom.Node;
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

    public ContainerTransactionNode() {
       registerElementHandler(new XMLElement(EjbTagNames.METHOD), MethodNode.class);
    }

    @Override
    public void addDescriptor(Object newDescriptor) {
        if (newDescriptor instanceof MethodDescriptor) {
            methods.add(newDescriptor);
        }
    }

    @Override
    public Object getDescriptor() {
        return null;
    }

    @Override
    public boolean endElement(XMLElement element) {
        boolean doneWithNode = super.endElement(element);
        
        if (doneWithNode) {
            ContainerTransaction ct =  new ContainerTransaction(trans_attribute, description);
            for (Iterator methodsIterator = methods.iterator();methodsIterator.hasNext();) {
                MethodDescriptor md = (MethodDescriptor) methodsIterator.next();
                EjbBundleDescriptorImpl bundle = (EjbBundleDescriptorImpl) getParentNode().getDescriptor();
                EjbDescriptor ejb = bundle.getEjbByName(md.getEjbName(), true);
                ejb.getMethodContainerTransactions().put(md, ct);
            }
        }        
        return doneWithNode;
    }

    @Override
    public void setElementValue(XMLElement element, String value) {
        if (TagNames.DESCRIPTION.equals(element.getQName())) {
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
     * @param nodeName name for the root element of this xml fragment
     * @param ejb the descriptor to write
     * @return the DOM tree top node
     */
    public Node writeDescriptor(Node parent, String nodeName, EjbDescriptor ejb) {    
        
        Map methodToTransactions = ejb.getMethodContainerTransactions();
        MethodNode mn = new MethodNode();
        for (Object o : methodToTransactions.entrySet()) {
            Map.Entry entry = (Map.Entry) o;
            MethodDescriptor md = (MethodDescriptor) entry.getKey();
            Node ctNode = super.writeDescriptor(parent, nodeName, ejb);            
            ContainerTransaction ct = (ContainerTransaction) entry.getValue();
            appendTextChild(ctNode, EjbTagNames.DESCRIPTION, ct.getDescription());
            mn.writeDescriptor(ctNode, EjbTagNames.METHOD, md, ejb.getName());
            appendTextChild(ctNode, EjbTagNames.TRANSACTION_ATTRIBUTE, ct.getTransactionAttribute());
        }
        return null;
    }
}

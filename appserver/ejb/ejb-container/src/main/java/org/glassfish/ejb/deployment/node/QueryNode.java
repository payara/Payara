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
import com.sun.enterprise.deployment.QueryDescriptor;
import com.sun.enterprise.deployment.node.DeploymentDescriptorNode;
import com.sun.enterprise.deployment.node.MethodNode;
import com.sun.enterprise.deployment.node.XMLElement;
import com.sun.enterprise.deployment.util.DOLUtils;
import com.sun.enterprise.deployment.xml.EjbTagNames;
import org.w3c.dom.Node;

import java.util.Map;
import java.util.logging.Level;

/**
 * This class is responsible for hanlding the query element
 *
 * @author  Jerome Dochez
 * @version 
 */
public class QueryNode extends DeploymentDescriptorNode {

    private QueryDescriptor descriptor = null;
    
    /** Creates new QueryNode */
    public QueryNode() {
        super();
        registerElementHandler(new XMLElement(EjbTagNames.QUERY_METHOD), 
                                                                MethodNode.class, "setQueryMethodDescriptor");                 
    }

    /**
     * @return the Descriptor subclass that was populated  by reading
     * the source XML file
     */
    public Object getDescriptor() {
        if (descriptor == null) {
            descriptor = (QueryDescriptor) super.getDescriptor();
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
        // no need to be synchronized for now
        Map table = super.getDispatchTable();
        table.put(EjbTagNames.EJB_QL, "setQuery");    
        return table;
    }
    
    
    /**
     * receives notiification of the value for a particular tag
     * 
     * @param element the xml element
     * @param value it's associated value
     */
    public void setElementValue(XMLElement element, String value) {    
        if (EjbTagNames.QUERY_RESULT_TYPE_MAPPING.equals(element.getQName())) {
            if (EjbTagNames.QUERY_REMOTE_TYPE_MAPPING.equals(value)) {
                descriptor.setHasRemoteReturnTypeMapping();
            } else if (EjbTagNames.QUERY_LOCAL_TYPE_MAPPING.equals(value)) {            
                descriptor.setHasLocalReturnTypeMapping();
            } else {
                DOLUtils.getDefaultLogger().log(Level.SEVERE, "enterprise.deployment.backend.addDescriptorFailure",
                                new Object[] {((Descriptor) getParentNode().getDescriptor()).getName() , value});
            }
        } else {
            super.setElementValue(element, value);
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
    public Node writeDescriptor(Node parent, String nodeName, QueryDescriptor descriptor) {        
        Node queryNode = super.writeDescriptor(parent, nodeName, descriptor);

        writeLocalizedDescriptions(queryNode, descriptor);
                
        // query-method
        MethodNode methodNode = new MethodNode();
        methodNode.writeQueryMethodDescriptor(queryNode, EjbTagNames.QUERY_METHOD, 
                                                                         descriptor.getQueryMethodDescriptor());
        
        if (descriptor.getHasRemoteReturnTypeMapping()) {            
            appendTextChild(queryNode, EjbTagNames.QUERY_RESULT_TYPE_MAPPING, 
                                                    EjbTagNames.QUERY_REMOTE_TYPE_MAPPING);     
        } else {
	    if (descriptor.getHasLocalReturnTypeMapping()) {
                appendTextChild(queryNode, EjbTagNames.QUERY_RESULT_TYPE_MAPPING,
                                                    EjbTagNames.QUERY_LOCAL_TYPE_MAPPING);
            }
	}
        // ejbql element is mandatory.  If no EJB QL query has been
        // specified for the method, the xml element will be empty
        String ejbqlText = descriptor.getIsEjbQl() ? descriptor.getQuery() : "";        
        Node child = appendChild(queryNode, EjbTagNames.EJB_QL);
        child.appendChild(getOwnerDocument(child).createTextNode(ejbqlText));          
        
        return queryNode;
    }
}

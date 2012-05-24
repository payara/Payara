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

import java.util.HashMap;
import java.util.Map;

import com.sun.enterprise.deployment.EjbBundleDescriptor;
import com.sun.enterprise.deployment.node.ConfigurableNode;
import com.sun.enterprise.deployment.node.DeploymentDescriptorNode;
import com.sun.enterprise.deployment.node.LocalizedInfoNode;
import com.sun.enterprise.deployment.node.XMLElement;
import com.sun.enterprise.deployment.node.XMLNode;
import com.sun.enterprise.deployment.xml.TagNames;
import org.glassfish.ejb.deployment.EjbTagNames;
import org.glassfish.ejb.deployment.descriptor.EjbCMPEntityDescriptor;
import org.glassfish.ejb.deployment.descriptor.RelationRoleDescriptor;
import org.w3c.dom.Node;

/**
 * This class is responsible for handling the ejb-relationship-role xml elements
 *
 * @author  Jerome Dochez
 * @version 
 */
public class EjbRelationshipRoleNode extends DeploymentDescriptorNode<RelationRoleDescriptor> {

    private RelationRoleDescriptor descriptor;

    public EjbRelationshipRoleNode() {
       super();
       registerElementHandler(new XMLElement(TagNames.DESCRIPTION), LocalizedInfoNode.class);           
    }

    @Override
    public  XMLNode getHandlerFor(XMLElement element) {
        if (EjbTagNames.RELATIONSHIP_ROLE_SOURCE.equals(element.getQName())) {
            Map dispatchTable = new HashMap();
            dispatchTable.put(EjbTagNames.DESCRIPTION, "setRoleSourceDescription");
            ConfigurableNode newNode = new ConfigurableNode(getDescriptor(), dispatchTable, element);
            return newNode;
        } if (EjbTagNames.CMR_FIELD.equals(element.getQName())) {
            Map dispatchTable = new HashMap();
            dispatchTable.put(EjbTagNames.DESCRIPTION, "setCMRFieldDescription");
            ConfigurableNode newNode = new ConfigurableNode(getDescriptor(), dispatchTable, element);
            return newNode;            
        } else {
            return super.getHandlerFor(element);
        }       
    }  

    @Override
    public boolean handlesElement(XMLElement element) {
        if (EjbTagNames.RELATIONSHIP_ROLE_SOURCE.equals(element.getQName())) {
            return true;
        } 
        if (EjbTagNames.CMR_FIELD.equals(element.getQName())) {    
            return true;
        } 
        return super.handlesElement(element);
    }

    @Override
    public RelationRoleDescriptor getDescriptor() {
        if (descriptor==null) descriptor = new RelationRoleDescriptor();
        return descriptor;
    }

    @Override
    protected Map getDispatchTable() {
        // no need to be synchronized for now
        Map table = super.getDispatchTable();
        table.put(EjbTagNames.EJB_RELATIONSHIP_ROLE_NAME, "setName");
        table.put(EjbTagNames.CMR_FIELD_NAME, "setCMRField");
        table.put(EjbTagNames.CMR_FIELD_TYPE, "setCMRFieldType");
        return table;
    }

    @Override
    public boolean endElement(XMLElement element) {
        if (EjbTagNames.CASCADE_DELETE.equals(element.getQName())) {
                descriptor.setCascadeDelete(true);
        }
        return super.endElement(element);
    }

    @Override
    public void setElementValue(XMLElement element, String value) {    
        if (EjbTagNames.MULTIPLICITY.equals(element.getQName())) {
            if ( value.equals("Many") )
                descriptor.setIsMany(true);
            else if ( value.equals("One") )
                descriptor.setIsMany(false);
            else if ( value.equals("many") ) // for backward compat with 1.3 FCS
                descriptor.setIsMany(true);
            else if ( value.equals("one") ) // for backward compat with 1.3 FCS
                descriptor.setIsMany(false);
            else 
                throw new IllegalArgumentException("Error in value of multiplicity element in EJB deployment descriptor XML: the value must be One or Many");
        } else if (EjbTagNames.EJB_NAME.equals(element.getQName())) {
            // let's get our bunlde descriptor...
                EjbBundleDescriptor bundleDesc = getEjbBundleDescriptor();
                EjbCMPEntityDescriptor desc = (EjbCMPEntityDescriptor)bundleDesc.getEjbByName(value);
                if (desc!=null){
                    descriptor.setPersistenceDescriptor(desc.getPersistenceDescriptor());            
                } else {
                    throw new IllegalArgumentException("Cannot find ejb " + value + " in bundle for relationship " + descriptor.getName());
                }                
        } else super.setElementValue(element, value);
    }
    
    private EjbBundleDescriptor getEjbBundleDescriptor() {
        XMLNode parent = getParentNode();
        Object parentDesc = parent.getDescriptor();
        while (parent!=null && !(parentDesc instanceof EjbBundleDescriptor)) {
            parent = parent.getParentNode();
            if (parent !=null)
                parentDesc = parent.getDescriptor();
        }
        if (parent!=null) {
            return (EjbBundleDescriptor) parentDesc;
        }  else {
            throw new IllegalArgumentException("Cannot find bundle descriptor");
        }
    } 

    @Override
    public Node writeDescriptor(Node parent, String nodeName, RelationRoleDescriptor descriptor) {   
        Node roleNode = super.writeDescriptor(parent, nodeName, descriptor);
        LocalizedInfoNode localizedNode = new LocalizedInfoNode();
        localizedNode.writeLocalizedMap(roleNode, TagNames.DESCRIPTION, descriptor.getLocalizedDescriptions());        
        if (descriptor.getRelationRoleName() != null) {
            appendTextChild(roleNode, EjbTagNames.EJB_RELATIONSHIP_ROLE_NAME, 
                descriptor.getRelationRoleName());        
        }
        
        // multiplicity
        if (descriptor.getIsMany()) {
            appendTextChild(roleNode, EjbTagNames.MULTIPLICITY, "Many");        
        } else {
            appendTextChild(roleNode, EjbTagNames.MULTIPLICITY, "One");        
        }
        
        // cascade-delete
        if (descriptor.getCascadeDelete()) {
            appendChild(roleNode, EjbTagNames.CASCADE_DELETE);        
        }
        
        Node roleSourceNode = appendChild(roleNode, EjbTagNames.RELATIONSHIP_ROLE_SOURCE);
        appendTextChild(roleSourceNode, TagNames.DESCRIPTION, descriptor.getRoleSourceDescription());
        appendTextChild(roleSourceNode, EjbTagNames.EJB_NAME, descriptor.getOwner().getName());
        
	// cmr-field
	if (descriptor.getCMRField() != null ) {
            Node cmrFieldNode = appendChild(roleNode, EjbTagNames.CMR_FIELD);
            
	    // description
            appendTextChild(cmrFieldNode, TagNames.DESCRIPTION, 
                                        descriptor.getCMRFieldDescription());
	    // cmr-field-name
	    appendTextChild(cmrFieldNode, EjbTagNames.CMR_FIELD_NAME, 
                                        descriptor.getCMRField());
	    // cmr-field-type
            appendTextChild(cmrFieldNode, EjbTagNames.CMR_FIELD_TYPE,
                                        descriptor.getCMRFieldType());
	}              
        return roleNode;
    }
}

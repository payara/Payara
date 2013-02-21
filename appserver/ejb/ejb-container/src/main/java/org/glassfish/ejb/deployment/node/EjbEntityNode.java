/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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
import java.util.Set;

import com.sun.enterprise.deployment.MethodDescriptor;
import com.sun.enterprise.deployment.node.XMLElement;
import com.sun.enterprise.deployment.xml.TagNames;
import org.glassfish.deployment.common.JavaEEResourceType;
import org.glassfish.ejb.deployment.EjbTagNames;
import org.glassfish.ejb.deployment.descriptor.EjbBundleDescriptorImpl;
import org.glassfish.ejb.deployment.descriptor.EjbCMPEntityDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbEntityDescriptor;
import org.glassfish.ejb.deployment.descriptor.FieldDescriptor;
import org.glassfish.ejb.deployment.descriptor.IASEjbCMPEntityDescriptor;
import org.glassfish.ejb.deployment.descriptor.QueryDescriptor;
import org.w3c.dom.Node;

/**
 *  This class handles all information pertinent to CMP and BMP entity beans
 *
 * @author  Jerome Dochez
 * @version 
 */
public class EjbEntityNode  extends InterfaceBasedEjbNode<EjbEntityDescriptor> {

    private EjbEntityDescriptor descriptor;

    public EjbEntityNode() { 
        super();      
        registerElementHandler(new XMLElement(EjbTagNames.CMP_FIELD), CmpFieldNode.class);          
        registerElementHandler(new XMLElement(EjbTagNames.QUERY), QueryNode.class);
    }

    @Override
    public EjbEntityDescriptor getEjbDescriptor() {
        if (descriptor == null) {
            descriptor = new EjbEntityDescriptor();
            descriptor.setEjbBundleDescriptor((EjbBundleDescriptorImpl) getParentNode().getDescriptor());
        }
        return descriptor;
    }

    /**
     * @return an instance of an EjbCMPEntityDescriptor initialized with all the 
     * fields already parsed.
     */
    private EjbCMPEntityDescriptor getCMPEntityDescriptor() {
        EjbDescriptor current = getEjbDescriptor();
        if (!(current instanceof EjbCMPEntityDescriptor)) {
            descriptor = new IASEjbCMPEntityDescriptor(current);
        }
        return (EjbCMPEntityDescriptor) descriptor;
    }

    @Override
    public void addDescriptor(Object  newDescriptor) {
        if (newDescriptor instanceof FieldDescriptor) {
           getCMPEntityDescriptor().getPersistenceDescriptor().addCMPField((FieldDescriptor) newDescriptor);           
        } else  if (newDescriptor instanceof QueryDescriptor) {
            QueryDescriptor newQuery = (QueryDescriptor) newDescriptor;
           getCMPEntityDescriptor().getPersistenceDescriptor().setQueryFor(
                        newQuery.getQueryMethodDescriptor(), newQuery);           
        } else {
            super.addDescriptor(newDescriptor);
        }
    }

    @Override
    protected Map getDispatchTable() {
        // no need to be synchronized for now
        Map table = super.getDispatchTable();
        table.put(EjbTagNames.PERSISTENCE_TYPE, "setPersistenceType");    
        table.put(EjbTagNames.PRIMARY_KEY_CLASS, "setPrimaryKeyClassName");              
        table.put(EjbTagNames.REENTRANT, "setReentrant");    
        return table;
    }

    @Override
    public void setElementValue(XMLElement element, String value) {
        if (EjbTagNames.CMP_VERSION.equals(element.getQName())) {
            if (EjbTagNames.CMP_1_VERSION.equals(value)) {
                getCMPEntityDescriptor().setCMPVersion(EjbCMPEntityDescriptor.CMP_1_1);
            } else if (EjbTagNames.CMP_2_VERSION.equals(value)) {
                getCMPEntityDescriptor().setCMPVersion(EjbCMPEntityDescriptor.CMP_2_x);                
            }
        } else if (EjbTagNames.ABSTRACT_SCHEMA_NAME.equals(element.getQName())) {
            getCMPEntityDescriptor().setAbstractSchemaName(value);
        } else  if (EjbTagNames.PRIMARY_KEY_FIELD.equals(element.getQName())) {
            getCMPEntityDescriptor().setPrimaryKeyFieldDesc(new FieldDescriptor(value));
        } else {
            super.setElementValue(element, value);
        }
    }

    @Override
    public Node writeDescriptor(Node parent, String nodeName, EjbEntityDescriptor ejbDesc) {
        Node ejbNode = super.writeDescriptor(parent, nodeName, ejbDesc);
        writeDisplayableComponentInfo(ejbNode, ejbDesc);
        writeCommonHeaderEjbDescriptor(ejbNode, ejbDesc);
        appendTextChild(ejbNode, EjbTagNames.PERSISTENCE_TYPE, ejbDesc.getPersistenceType());                   
        appendTextChild(ejbNode, EjbTagNames.PRIMARY_KEY_CLASS, ejbDesc.getPrimaryKeyClassName());                  
        appendTextChild(ejbNode, EjbTagNames.REENTRANT, ejbDesc.getReentrant());                  
        
        // cmp entity beans related tags
        if (ejbDesc instanceof EjbCMPEntityDescriptor) {
            EjbCMPEntityDescriptor cmpDesc = (EjbCMPEntityDescriptor) ejbDesc;
            if (cmpDesc.getCMPVersion()==EjbCMPEntityDescriptor.CMP_1_1) {
                appendTextChild(ejbNode, EjbTagNames.CMP_VERSION, EjbTagNames.CMP_1_VERSION);                   
            } else {
                appendTextChild(ejbNode, EjbTagNames.CMP_VERSION, EjbTagNames.CMP_2_VERSION);                   
            }
            
            appendTextChild(ejbNode, EjbTagNames.ABSTRACT_SCHEMA_NAME, cmpDesc.getAbstractSchemaName());                  
            // cmp-field*
            CmpFieldNode cmpNode = new CmpFieldNode();
            for (Iterator fields = cmpDesc.getPersistenceDescriptor().getCMPFields().iterator();fields.hasNext();) {
                FieldDescriptor aField = (FieldDescriptor) fields.next();
                cmpNode.writeDescriptor(ejbNode, EjbTagNames.CMP_FIELD, aField);
            }
            if ( cmpDesc.getPrimaryKeyFieldDesc()!=null) {
                appendTextChild(ejbNode, EjbTagNames.PRIMARY_KEY_FIELD, cmpDesc.getPrimaryKeyFieldDesc().getName());                
            }
        }
        
        // env-entry*
        writeEnvEntryDescriptors(ejbNode, ejbDesc.getEnvironmentProperties().iterator());
        
        // ejb-ref * and ejb-local-ref*
        writeEjbReferenceDescriptors(ejbNode, ejbDesc.getEjbReferenceDescriptors().iterator());

        // service-ref*
        writeServiceReferenceDescriptors(ejbNode, ejbDesc.getServiceReferenceDescriptors().iterator());
        
        // resource-ref*
        writeResourceRefDescriptors(ejbNode, ejbDesc.getResourceReferenceDescriptors().iterator());
        
        // resource-env-ref*
        writeResourceEnvRefDescriptors(ejbNode, ejbDesc.getResourceEnvReferenceDescriptors().iterator());        
        
        // message-destination-ref*
        writeMessageDestinationRefDescriptors(ejbNode, ejbDesc.getMessageDestinationReferenceDescriptors().iterator());

        // persistence-context-ref*
        writeEntityManagerReferenceDescriptors(ejbNode, ejbDesc.getEntityManagerReferenceDescriptors().iterator());
        
        // persistence-unit-ref*
        writeEntityManagerFactoryReferenceDescriptors(ejbNode, ejbDesc.getEntityManagerFactoryReferenceDescriptors().iterator());
        
        // post-construct
        writeLifeCycleCallbackDescriptors(ejbNode, TagNames.POST_CONSTRUCT, ejbDesc.getPostConstructDescriptors());

        // pre-destroy
        writeLifeCycleCallbackDescriptors(ejbNode, TagNames.PRE_DESTROY, ejbDesc.getPreDestroyDescriptors());

        // all descriptors (includes DSD, MSD, JMSCFD, JMSDD,AOD, CFD)*
        writeResourceDescriptors(ejbNode, ejbDesc.getAllResourcesDescriptors().iterator());

        // security-role-ref*
        writeRoleReferenceDescriptors(ejbNode, ejbDesc.getRoleReferences().iterator());
        
        // security-identity
        writeSecurityIdentityDescriptor(ejbNode, ejbDesc);

        // query
        if (ejbDesc instanceof EjbCMPEntityDescriptor) {
            EjbCMPEntityDescriptor cmpDesc = (EjbCMPEntityDescriptor) ejbDesc;
            Set queriedMethods = cmpDesc.getPersistenceDescriptor().getQueriedMethods();
            if (queriedMethods.size()>0) {
                QueryNode queryNode = new QueryNode();
                for (Iterator e=queriedMethods.iterator();e.hasNext();) {
                    queryNode.writeDescriptor(ejbNode, EjbTagNames.QUERY,
                        cmpDesc.getPersistenceDescriptor().getQueryFor((MethodDescriptor) e.next()));
                }                            
            }
        }            
        return ejbNode;
    }    
}

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
import java.util.logging.Level;

import com.sun.enterprise.deployment.LifecycleCallbackDescriptor;
import com.sun.enterprise.deployment.MessageDestinationReferenceDescriptor;
import com.sun.enterprise.deployment.RoleReference;
import com.sun.enterprise.deployment.RunAsIdentityDescriptor;
import com.sun.enterprise.deployment.node.AdministeredObjectDefinitionNode;
import com.sun.enterprise.deployment.node.ConnectionFactoryDefinitionNode;
import com.sun.enterprise.deployment.node.DataSourceDefinitionNode;
import com.sun.enterprise.deployment.node.JMSConnectionFactoryDefinitionNode;
import com.sun.enterprise.deployment.node.JMSDestinationDefinitionNode;
import com.sun.enterprise.deployment.node.MailSessionNode;
import com.sun.enterprise.deployment.node.DisplayableComponentNode;
import com.sun.enterprise.deployment.node.EjbLocalReferenceNode;
import com.sun.enterprise.deployment.node.EjbReferenceNode;
import com.sun.enterprise.deployment.node.EntityManagerFactoryReferenceNode;
import com.sun.enterprise.deployment.node.EntityManagerReferenceNode;
import com.sun.enterprise.deployment.node.EnvEntryNode;
import com.sun.enterprise.deployment.node.JndiEnvRefNode;
import com.sun.enterprise.deployment.node.MessageDestinationRefNode;
import com.sun.enterprise.deployment.node.ResourceEnvRefNode;
import com.sun.enterprise.deployment.node.ResourceRefNode;
import com.sun.enterprise.deployment.node.SecurityRoleRefNode;
import com.sun.enterprise.deployment.node.XMLElement;
import com.sun.enterprise.deployment.types.EjbReference;
import com.sun.enterprise.deployment.util.DOLUtils;
import com.sun.enterprise.deployment.xml.TagNames;
import com.sun.enterprise.deployment.xml.WebServicesTagNames;
import org.glassfish.ejb.deployment.EjbTagNames;
import org.glassfish.ejb.deployment.descriptor.EjbBundleDescriptorImpl;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.w3c.dom.Node;

/**
 * This class is responsible for handling all common information
 * shared by all types of enterprise beans (MDB, session, entity)
 *
 * @author  Jerome Dochez
 * @version
 */
public abstract class EjbNode<S extends EjbDescriptor> extends DisplayableComponentNode<S> {

    /** Creates new EjbNode */
    public EjbNode() {
        super();
        registerElementHandler(new XMLElement(TagNames.ENVIRONMENT_PROPERTY), 
                                                             EnvEntryNode.class, "addEnvironmentProperty");                          
        registerElementHandler(new XMLElement(TagNames.EJB_REFERENCE), EjbReferenceNode.class);     
        registerElementHandler(new XMLElement(TagNames.EJB_LOCAL_REFERENCE), EjbLocalReferenceNode.class);     
        JndiEnvRefNode serviceRefNode = habitat.getService(JndiEnvRefNode.class, WebServicesTagNames.SERVICE_REF);
        if (serviceRefNode != null) {
            registerElementHandler(new XMLElement(WebServicesTagNames.SERVICE_REF), serviceRefNode.getClass(),"addServiceReferenceDescriptor");
        }
        registerElementHandler(new XMLElement(TagNames.RESOURCE_REFERENCE), 
                                                             ResourceRefNode.class, "addResourceReferenceDescriptor");
        registerElementHandler(new XMLElement(TagNames.DATA_SOURCE), DataSourceDefinitionNode.class, "addResourceDescriptor");
        registerElementHandler(new XMLElement(TagNames.MAIL_SESSION), MailSessionNode.class, "addResourceDescriptor");
        registerElementHandler(new XMLElement(TagNames.CONNECTION_FACTORY), ConnectionFactoryDefinitionNode.class, "addResourceDescriptor");
        registerElementHandler(new XMLElement(TagNames.ADMINISTERED_OBJECT), AdministeredObjectDefinitionNode.class, "addResourceDescriptor");
        registerElementHandler(new XMLElement(TagNames.JMS_CONNECTION_FACTORY), JMSConnectionFactoryDefinitionNode.class, "addResourceDescriptor");
        registerElementHandler(new XMLElement(TagNames.JMS_DESTINATION), JMSDestinationDefinitionNode.class, "addResourceDescriptor");

        registerElementHandler(new XMLElement(EjbTagNames.SECURITY_IDENTITY),
                                                            SecurityIdentityNode.class);             
        registerElementHandler(new XMLElement(TagNames.RESOURCE_ENV_REFERENCE), 
                                                            ResourceEnvRefNode.class, "addResourceEnvReferenceDescriptor");               
        registerElementHandler(new XMLElement(TagNames.MESSAGE_DESTINATION_REFERENCE), MessageDestinationRefNode.class);
        registerElementHandler(new XMLElement(TagNames.PERSISTENCE_CONTEXT_REF), EntityManagerReferenceNode.class, "addEntityManagerReferenceDescriptor");
        registerElementHandler(new XMLElement(TagNames.PERSISTENCE_UNIT_REF), EntityManagerFactoryReferenceNode.class, "addEntityManagerFactoryReferenceDescriptor");

        // Use special method for overrides because more than one schedule can be specified on a single method
        registerElementHandler(new XMLElement(EjbTagNames.TIMER), ScheduledTimerNode.class, "addScheduledTimerDescriptorFromDD");
    }

    @Override
    public void addDescriptor(Object  newDescriptor) {       
        if (newDescriptor instanceof EjbReference) {            
            if (DOLUtils.getDefaultLogger().isLoggable(Level.FINE)) {
                DOLUtils.getDefaultLogger().fine("Adding ejb ref " + newDescriptor);
            }
            getEjbDescriptor().addEjbReferenceDescriptor(
                        (EjbReference) newDescriptor);
        } else  if (newDescriptor instanceof RunAsIdentityDescriptor) {
            if (DOLUtils.getDefaultLogger().isLoggable(Level.FINE)) {
                DOLUtils.getDefaultLogger().fine("Adding security-identity" + newDescriptor);                   
            }
            getEjbDescriptor().setUsesCallerIdentity(false);
	    getEjbDescriptor().setRunAsIdentity((RunAsIdentityDescriptor) newDescriptor);
        } else if( newDescriptor instanceof 
                   MessageDestinationReferenceDescriptor ) {
            MessageDestinationReferenceDescriptor msgDestRef =
                (MessageDestinationReferenceDescriptor) newDescriptor;
            EjbBundleDescriptorImpl ejbBundle = (EjbBundleDescriptorImpl)
                getParentNode().getDescriptor();
            // EjbBundle might not be set yet on EjbDescriptor, so set it
            // explicitly here.
            msgDestRef.setReferringBundleDescriptor(ejbBundle);
            getEjbDescriptor().addMessageDestinationReferenceDescriptor
                (msgDestRef);
        } else {
            super.addDescriptor(newDescriptor);
        }
    }

    @Override
    public S getDescriptor() {
        return getEjbDescriptor();
    }

    public abstract S getEjbDescriptor();

    @Override
    protected Map getDispatchTable() {
        // no need to be synchronized for now
        Map table = super.getDispatchTable();
        table.put(EjbTagNames.EJB_NAME, "setName");
        table.put(EjbTagNames.EJB_CLASS, "setEjbClassName");        
        table.put(TagNames.MAPPED_NAME, "setMappedName");        
        return table;
    }    
        
    /**
     * write the common descriptor info to a DOM tree and return it
     *
     * @param parent node for the DOM tree
     * @param the descriptor to write
     */    
    protected void writeCommonHeaderEjbDescriptor(Node ejbNode, EjbDescriptor descriptor) {
        appendTextChild(ejbNode, EjbTagNames.EJB_NAME, descriptor.getName());                       
        appendTextChild(ejbNode, TagNames.MAPPED_NAME, descriptor.getMappedName());                       
    }    
    
    /**
     * write the security identity information about an EJB
     *
     * @param parent node for the DOM tree
     * @param the EJB descriptor the security information to be retrieved
     */        
    protected void writeSecurityIdentityDescriptor(Node parent,  EjbDescriptor descriptor) {
        if (!descriptor.getUsesCallerIdentity() && descriptor.getRunAsIdentity()==null) 
            return;
        
        SecurityIdentityNode node = new SecurityIdentityNode();
        node.writeDescriptor(parent, EjbTagNames.SECURITY_IDENTITY,  descriptor);
    }

    /**
     * write  the security role references to the DOM Tree
     *
     * @param parentNode for the DOM tree
     * @param refs iterator over the RoleReference descriptors to write
     */
    protected void writeRoleReferenceDescriptors(Node parentNode, Iterator refs) {
        SecurityRoleRefNode node = new SecurityRoleRefNode();
        for (;refs.hasNext();) {
            RoleReference roleRef = (RoleReference) refs.next();
            node.writeDescriptor(parentNode, TagNames.ROLE_REFERENCE, roleRef);
        }
    }

    protected static void writeAroundInvokeDescriptors
            (Node parentNode, Iterator aroundInvokeDescs) {
        if (aroundInvokeDescs == null || !aroundInvokeDescs.hasNext())
            return;

        AroundInvokeNode subNode = new AroundInvokeNode();
        for(; aroundInvokeDescs.hasNext();) {
            LifecycleCallbackDescriptor next =
                    (LifecycleCallbackDescriptor) aroundInvokeDescs.next();
            subNode.writeDescriptor(parentNode,
                    EjbTagNames.AROUND_INVOKE_METHOD, next);
        }

    }

    protected static void writeAroundTimeoutDescriptors
            (Node parentNode, Iterator aroundTimeoutDescs) {
        if (aroundTimeoutDescs == null || !aroundTimeoutDescs.hasNext())
            return;

        AroundTimeoutNode subNode = new AroundTimeoutNode();
        for(; aroundTimeoutDescs.hasNext();) {
            LifecycleCallbackDescriptor next =
                    (LifecycleCallbackDescriptor) aroundTimeoutDescs.next();
            subNode.writeDescriptor(parentNode,
                    EjbTagNames.AROUND_TIMEOUT_METHOD, next);
        }

    }
}

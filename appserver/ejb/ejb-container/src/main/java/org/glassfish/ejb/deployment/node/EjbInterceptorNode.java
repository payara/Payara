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

import java.util.Map;
import java.util.logging.Level;

import com.sun.enterprise.deployment.EjbBundleDescriptor;
import com.sun.enterprise.deployment.EjbInterceptor;
import com.sun.enterprise.deployment.MessageDestinationReferenceDescriptor;
import com.sun.enterprise.deployment.node.AdministeredObjectDefinitionNode;
import com.sun.enterprise.deployment.node.ConnectionFactoryDefinitionNode;
import com.sun.enterprise.deployment.node.DataSourceDefinitionNode;
import com.sun.enterprise.deployment.node.JMSConnectionFactoryDefinitionNode;
import com.sun.enterprise.deployment.node.JMSDestinationDefinitionNode;
import com.sun.enterprise.deployment.node.MailSessionNode;
import com.sun.enterprise.deployment.node.DeploymentDescriptorNode;
import com.sun.enterprise.deployment.node.EjbLocalReferenceNode;
import com.sun.enterprise.deployment.node.EjbReferenceNode;
import com.sun.enterprise.deployment.node.EntityManagerFactoryReferenceNode;
import com.sun.enterprise.deployment.node.EntityManagerReferenceNode;
import com.sun.enterprise.deployment.node.EnvEntryNode;
import com.sun.enterprise.deployment.node.JndiEnvRefNode;
import com.sun.enterprise.deployment.node.LifecycleCallbackNode;
import com.sun.enterprise.deployment.node.MessageDestinationRefNode;
import com.sun.enterprise.deployment.node.ResourceEnvRefNode;
import com.sun.enterprise.deployment.node.ResourceRefNode;
import com.sun.enterprise.deployment.node.XMLElement;
import com.sun.enterprise.deployment.types.EjbReference;
import com.sun.enterprise.deployment.util.DOLUtils;
import com.sun.enterprise.deployment.xml.TagNames;
import com.sun.enterprise.deployment.xml.WebServicesTagNames;
import org.glassfish.ejb.deployment.EjbTagNames;
import org.w3c.dom.Node;

import static com.sun.enterprise.deployment.LifecycleCallbackDescriptor.CallbackType;

public class EjbInterceptorNode extends DeploymentDescriptorNode<EjbInterceptor> {
    private EjbInterceptor descriptor;

    public EjbInterceptorNode() {
        super();

        registerElementHandler(new XMLElement(EjbTagNames.AROUND_INVOKE_METHOD), AroundInvokeNode.class, "addAroundInvokeDescriptor");       
        registerElementHandler(new XMLElement(EjbTagNames.AROUND_TIMEOUT_METHOD), AroundTimeoutNode.class, "addAroundTimeoutDescriptor");       
        registerElementHandler(new XMLElement(EjbTagNames.POST_ACTIVATE_METHOD), LifecycleCallbackNode.class, "addPostActivateDescriptor");       
        registerElementHandler(new XMLElement(EjbTagNames.PRE_PASSIVATE_METHOD), LifecycleCallbackNode.class, "addPrePassivateDescriptor");       
        registerElementHandler(new XMLElement(EjbTagNames.AROUND_CONSTRUCT), LifecycleCallbackNode.class, "addAroundConstructDescriptor");       

        //jndiEnvironmentRefsGroup
        registerElementHandler(new XMLElement(TagNames.POST_CONSTRUCT), LifecycleCallbackNode.class, "addPostConstructDescriptor");       
        registerElementHandler(new XMLElement(TagNames.PRE_DESTROY), LifecycleCallbackNode.class, "addPreDestroyDescriptor");
        registerElementHandler(new XMLElement(TagNames.DATA_SOURCE), DataSourceDefinitionNode.class, "addResourceDescriptor");
        registerElementHandler(new XMLElement(TagNames.MAIL_SESSION), MailSessionNode.class, "addResourceDescriptor");
        registerElementHandler(new XMLElement(TagNames.CONNECTION_FACTORY), ConnectionFactoryDefinitionNode.class, "addResourceDescriptor");
        registerElementHandler(new XMLElement(TagNames.ADMINISTERED_OBJECT), AdministeredObjectDefinitionNode.class, "addResourceDescriptor");
        registerElementHandler(new XMLElement(TagNames.JMS_CONNECTION_FACTORY), JMSConnectionFactoryDefinitionNode.class, "addResourceDescriptor");
        registerElementHandler(new XMLElement(TagNames.JMS_DESTINATION), JMSDestinationDefinitionNode.class, "addResourceDescriptor");

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
        registerElementHandler(new XMLElement(TagNames.RESOURCE_ENV_REFERENCE),
               ResourceEnvRefNode.class, "addResourceEnvReferenceDescriptor");               
        registerElementHandler(new XMLElement(TagNames.MESSAGE_DESTINATION_REFERENCE), MessageDestinationRefNode.class);
        registerElementHandler(new XMLElement(TagNames.PERSISTENCE_CONTEXT_REF), EntityManagerReferenceNode.class, "addEntityManagerReferenceDescriptor");
        registerElementHandler(new XMLElement(TagNames.PERSISTENCE_UNIT_REF), EntityManagerFactoryReferenceNode.class, "addEntityManagerFactoryReferenceDescriptor");
    }

    @Override
    protected Map getDispatchTable() {
        // no need to be synchronized for now
        Map table = super.getDispatchTable();
        table.put(EjbTagNames.INTERCEPTOR_CLASS, "setInterceptorClassName");
        return table;
    }

    @Override
    public EjbInterceptor getDescriptor() {
        if (descriptor==null) {
            descriptor = new EjbInterceptor();
            descriptor.setEjbBundleDescriptor((EjbBundleDescriptor)getParentNode().getDescriptor());
        }
        return descriptor;
    }

    @Override
    public void addDescriptor(Object  newDescriptor) {       
        if (newDescriptor instanceof EjbReference) {            
            if (DOLUtils.getDefaultLogger().isLoggable(Level.FINE)) {
                DOLUtils.getDefaultLogger().fine("Adding ejb ref " + newDescriptor);
            }
            getDescriptor().addEjbReferenceDescriptor(
                        (EjbReference) newDescriptor);
        } else if( newDescriptor instanceof 
                   MessageDestinationReferenceDescriptor ) {
            MessageDestinationReferenceDescriptor msgDestRef =
                (MessageDestinationReferenceDescriptor) newDescriptor;
            EjbBundleDescriptor ejbBundle = (EjbBundleDescriptor) 
                getParentNode().getDescriptor();
            // EjbBundle might not be set yet on EjbInterceptor, so set it
            // explicitly here.
            msgDestRef.setReferringBundleDescriptor(ejbBundle);
            getDescriptor().addMessageDestinationReferenceDescriptor
                (msgDestRef);
        } else {
            super.addDescriptor(newDescriptor);
        }
    }

    @Override
    public Node writeDescriptor(Node parent, String nodeName, EjbInterceptor descriptor) {
        Node interceptorNode = appendChild(parent, nodeName);

        writeLocalizedDescriptions(interceptorNode, descriptor);
       
        appendTextChild(interceptorNode, EjbTagNames.INTERCEPTOR_CLASS, descriptor.getInterceptorClassName());     

        if (descriptor.hasAroundInvokeDescriptor()) {
            EjbNode.writeAroundInvokeDescriptors(interceptorNode,
                descriptor.getAroundInvokeDescriptors().iterator());
        }
        if (descriptor.hasAroundTimeoutDescriptor()) {
            EjbNode.writeAroundTimeoutDescriptors(interceptorNode,
                descriptor.getAroundTimeoutDescriptors().iterator());
        }
        if (descriptor.hasCallbackDescriptor(CallbackType.AROUND_CONSTRUCT)) {
            writeLifeCycleCallbackDescriptors(interceptorNode, EjbTagNames.AROUND_CONSTRUCT,
                descriptor.getCallbackDescriptors(CallbackType.AROUND_CONSTRUCT));
        }
        if (descriptor.hasCallbackDescriptor(CallbackType.POST_CONSTRUCT)) {
            writeLifeCycleCallbackDescriptors(interceptorNode, TagNames.POST_CONSTRUCT,
                descriptor.getCallbackDescriptors(CallbackType.POST_CONSTRUCT));
        }
        if (descriptor.hasCallbackDescriptor(CallbackType.PRE_DESTROY)) {
            writeLifeCycleCallbackDescriptors(interceptorNode, TagNames.PRE_DESTROY,
                descriptor.getCallbackDescriptors(CallbackType.PRE_DESTROY));
        }
        if (descriptor.hasCallbackDescriptor(CallbackType.POST_ACTIVATE)) {
            writeLifeCycleCallbackDescriptors(interceptorNode, EjbTagNames.POST_ACTIVATE_METHOD,
                descriptor.getCallbackDescriptors(CallbackType.POST_ACTIVATE));
        }
        if (descriptor.hasCallbackDescriptor(CallbackType.PRE_PASSIVATE)) {
            writeLifeCycleCallbackDescriptors(interceptorNode, EjbTagNames.PRE_PASSIVATE_METHOD,
                descriptor.getCallbackDescriptors(CallbackType.PRE_PASSIVATE));
        }

        // all descriptors (includes DSD, MSD, JMSCFD, JMSDD,AOD, CRD)*
        writeResourceDescriptors(interceptorNode, descriptor.getAllResourcesDescriptors().iterator());

        return interceptorNode;
    }
}

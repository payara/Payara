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

import com.sun.enterprise.deployment.node.*;
import com.sun.enterprise.deployment.xml.TagNames;
import org.glassfish.deployment.common.JavaEEResourceType;
import org.glassfish.ejb.deployment.EjbTagNames;
import org.glassfish.ejb.deployment.descriptor.EjbBundleDescriptorImpl;
import org.glassfish.ejb.deployment.descriptor.EjbMessageBeanDescriptor;
import org.glassfish.ejb.deployment.descriptor.ScheduledTimerDescriptor;
import org.w3c.dom.Node;

import java.util.Map;

/**
 * This class handles message-driven related xml information
 *
 * @author  Jerome Dochez
 * @version
 */
public class MessageDrivenBeanNode extends EjbNode<EjbMessageBeanDescriptor> {

    private EjbMessageBeanDescriptor descriptor;

    public MessageDrivenBeanNode() {
        super();
        registerElementHandler(new XMLElement(EjbTagNames.ACTIVATION_CONFIG),
                               ActivationConfigNode.class,
                               "setActivationConfigDescriptor");

        registerElementHandler(new XMLElement(EjbTagNames.AROUND_INVOKE_METHOD), AroundInvokeNode.class, "addAroundInvokeDescriptor"); 

        registerElementHandler(new XMLElement(EjbTagNames.AROUND_TIMEOUT_METHOD), AroundTimeoutNode.class, "addAroundTimeoutDescriptor"); 

        registerElementHandler(new XMLElement(TagNames.POST_CONSTRUCT), LifecycleCallbackNode.class, "addPostConstructDescriptor");

        registerElementHandler(new XMLElement(TagNames.PRE_DESTROY), LifecycleCallbackNode.class, "addPreDestroyDescriptor");

        registerElementHandler(new XMLElement(TagNames.DATA_SOURCE), DataSourceDefinitionNode.class, "addResourceDescriptor");

        registerElementHandler(new XMLElement(EjbTagNames.TIMEOUT_METHOD), MethodNode.class, "setEjbTimeoutMethod");

        registerElementHandler(new XMLElement(TagNames.MAIL_SESSION), MailSessionNode.class, "addResourceDescriptor");

        registerElementHandler(new XMLElement(TagNames.ROLE_REFERENCE), SecurityRoleRefNode.class, "addRoleReference");
    }

    @Override
    public EjbMessageBeanDescriptor getEjbDescriptor() {
        if (descriptor == null) {
            descriptor = new EjbMessageBeanDescriptor();
            descriptor.setEjbBundleDescriptor((EjbBundleDescriptorImpl) getParentNode().getDescriptor());
        }
        return descriptor;
    }

    @Override
    protected Map getDispatchTable() {
        // no need to be synchronized for now
        Map table = super.getDispatchTable();

        table.put(EjbTagNames.MESSAGING_TYPE, "setMessageListenerType"); 
        table.put(EjbTagNames.TRANSACTION_TYPE, "setTransactionType");
        table.put(EjbTagNames.MESSAGE_DESTINATION_TYPE, "setDestinationType");
        table.put(TagNames.MESSAGE_DESTINATION_LINK, 
                  "setMessageDestinationLinkName");

        // These are the EJB 2.0 elements that no longer exist in the EJB 2.1
        // schema.  We still set them on the descriptor but they will be
        // written out as activation config properties.
        table.put(EjbTagNames.MSG_SELECTOR, "setJmsMessageSelector");
        table.put(EjbTagNames.JMS_ACKNOWLEDGE_MODE, "setJmsAcknowledgeMode");
        table.put(EjbTagNames.JMS_DEST_TYPE, "setDestinationType");        
        table.put(EjbTagNames.JMS_SUBSCRIPTION_DURABILITY, 
                  "setSubscriptionDurability");        
        return table;
    }

    @Override
    public Node writeDescriptor(Node parent, String nodeName, EjbMessageBeanDescriptor ejbDesc) {
        Node ejbNode = super.writeDescriptor(parent, nodeName, ejbDesc);
        writeDisplayableComponentInfo(ejbNode, ejbDesc);
        writeCommonHeaderEjbDescriptor(ejbNode, ejbDesc);
        appendTextChild(ejbNode, EjbTagNames.EJB_CLASS, ejbDesc.getEjbClassName());             
        appendTextChild(ejbNode, EjbTagNames.MESSAGING_TYPE, ejbDesc.getMessageListenerType());


        MethodNode methodNode = new MethodNode();
        
        if( ejbDesc.isTimedObject() ) {
            if (ejbDesc.getEjbTimeoutMethod() != null) {
                methodNode.writeJavaMethodDescriptor
                        (ejbNode, EjbTagNames.TIMEOUT_METHOD,
                         ejbDesc.getEjbTimeoutMethod());
            }

            for ( ScheduledTimerDescriptor timerDesc : ejbDesc.getScheduledTimerDescriptors()) {
                ScheduledTimerNode timerNode = new ScheduledTimerNode();
                timerNode.writeDescriptor(ejbNode, EjbTagNames.TIMER, timerDesc);
            }
        }

        appendTextChild(ejbNode, EjbTagNames.TRANSACTION_TYPE, ejbDesc.getTransactionType());                   

        // message-destination-type
        appendTextChild(ejbNode, TagNames.MESSAGE_DESTINATION_TYPE,
                        ejbDesc.getDestinationType());

        // message-destination-link
        String link = ejbDesc.getMessageDestinationLinkName();
        appendTextChild(ejbNode, TagNames.MESSAGE_DESTINATION_LINK, link);
        
        ActivationConfigNode activationConfigNode = new ActivationConfigNode();
        activationConfigNode.writeDescriptor
            (ejbNode, EjbTagNames.ACTIVATION_CONFIG,
             ejbDesc.getActivationConfigDescriptor());

        // around-invoke-method
        writeAroundInvokeDescriptors(ejbNode, ejbDesc.getAroundInvokeDescriptors().iterator());

        // around-timeout-method
        writeAroundTimeoutDescriptors(ejbNode, ejbDesc.getAroundTimeoutDescriptors().iterator());

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
        
        return ejbNode;
    }
}

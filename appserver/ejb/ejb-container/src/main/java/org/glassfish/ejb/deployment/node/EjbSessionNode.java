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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.sun.enterprise.deployment.MethodDescriptor;
import com.sun.enterprise.deployment.node.DataSourceDefinitionNode;
import com.sun.enterprise.deployment.node.MailSessionNode;
import com.sun.enterprise.deployment.node.LifecycleCallbackNode;
import com.sun.enterprise.deployment.node.MethodNode;
import com.sun.enterprise.deployment.node.XMLElement;
import com.sun.enterprise.deployment.xml.TagNames;
import org.glassfish.deployment.common.JavaEEResourceType;
import org.glassfish.ejb.deployment.EjbTagNames;
import org.glassfish.ejb.deployment.descriptor.ConcurrentMethodDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbBundleDescriptorImpl;
import org.glassfish.ejb.deployment.descriptor.EjbInitInfo;
import org.glassfish.ejb.deployment.descriptor.EjbRemovalInfo;
import org.glassfish.ejb.deployment.descriptor.EjbSessionDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbSessionDescriptor.ConcurrencyManagementType;
import org.glassfish.ejb.deployment.descriptor.ScheduledTimerDescriptor;
import org.glassfish.ejb.deployment.descriptor.TimeoutValueDescriptor;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;

/**
 * This class handles all information pertinent to session beans
 *
 * @author Jerome Dochez
 * @version
 */
public class EjbSessionNode extends InterfaceBasedEjbNode<EjbSessionDescriptor> {

    private EjbSessionDescriptor descriptor;

    private boolean inDependsOn = false;
    private List<String> dependsOn = null;

    public EjbSessionNode() {
        super();
        // register sub XMLNodes

        registerElementHandler(new XMLElement(EjbTagNames.AROUND_INVOKE_METHOD), AroundInvokeNode.class, "addAroundInvokeDescriptor");

        registerElementHandler(new XMLElement(EjbTagNames.AROUND_TIMEOUT_METHOD), AroundTimeoutNode.class, "addAroundTimeoutDescriptor");

        registerElementHandler(new XMLElement(TagNames.POST_CONSTRUCT), LifecycleCallbackNode.class, "addPostConstructDescriptor");

        registerElementHandler(new XMLElement(TagNames.PRE_DESTROY), LifecycleCallbackNode.class, "addPreDestroyDescriptor");

        registerElementHandler(new XMLElement(TagNames.DATA_SOURCE), DataSourceDefinitionNode.class, "addResourceDescriptor");

        registerElementHandler(new XMLElement(EjbTagNames.MAIL_SESSION), MailSessionNode.class, "addResourceDescriptor");

        registerElementHandler(new XMLElement(EjbTagNames.POST_ACTIVATE_METHOD), LifecycleCallbackNode.class, "addPostActivateDescriptor");

        registerElementHandler(new XMLElement(EjbTagNames.PRE_PASSIVATE_METHOD), LifecycleCallbackNode.class, "addPrePassivateDescriptor");

        registerElementHandler(new XMLElement(EjbTagNames.TIMEOUT_METHOD), MethodNode.class, "setEjbTimeoutMethod");

        registerElementHandler(new XMLElement(EjbTagNames.INIT_METHOD), EjbInitNode.class, "addInitMethod");

        registerElementHandler(new XMLElement(EjbTagNames.REMOVE_METHOD), EjbRemoveNode.class, "addRemoveMethod");

        registerElementHandler(new XMLElement(EjbTagNames.STATEFUL_TIMEOUT), TimeoutValueNode.class, "addStatefulTimeoutDescriptor");

        registerElementHandler(new XMLElement(EjbTagNames.AFTER_BEGIN_METHOD), MethodNode.class, "addAfterBeginDescriptor");

        registerElementHandler(new XMLElement(EjbTagNames.BEFORE_COMPLETION_METHOD), MethodNode.class, "addBeforeCompletionDescriptor");

        registerElementHandler(new XMLElement(EjbTagNames.AFTER_COMPLETION_METHOD), MethodNode.class, "addAfterCompletionDescriptor");

        registerElementHandler(new XMLElement(EjbTagNames.ASYNC_METHOD), MethodNode.class, "addAsynchronousMethod");

        registerElementHandler(new XMLElement(EjbTagNames.CONCURRENT_METHOD), ConcurrentMethodNode.class,
                "addConcurrentMethodFromXml");
    }

    @Override
    public EjbSessionDescriptor getEjbDescriptor() {
        if (descriptor == null) {
            descriptor = new EjbSessionDescriptor();
            descriptor.setEjbBundleDescriptor((EjbBundleDescriptorImpl) getParentNode().getDescriptor());
        }
        return descriptor;
    }

    @Override
    protected Map getDispatchTable() {
        // no need to be synchronized for now
        Map table = super.getDispatchTable();
        table.put(EjbTagNames.SESSION_TYPE, "setSessionType");
        table.put(EjbTagNames.TRANSACTION_TYPE, "setTransactionType");
        table.put(EjbTagNames.INIT_ON_STARTUP, "setInitOnStartup");
        table.put(EjbTagNames.PASSIVATION_CAPABLE, "setPassivationCapable");        
        return table;
    }

    @Override
    public void startElement(XMLElement element, Attributes attributes) {
        if (EjbTagNames.LOCAL_BEAN.equals(element.getQName())) {
            descriptor.setLocalBean(true);
        } else if (EjbTagNames.DEPENDS_ON.equals(element.getQName())) {
            inDependsOn = true;
            dependsOn = new ArrayList<String>();
        }
        super.startElement(element, attributes);
    }

    @Override
    public void setElementValue(XMLElement element, String value) {
        if (inDependsOn && EjbTagNames.EJB_NAME.equals(element.getQName())) {
            dependsOn.add(value);
        } else if (EjbTagNames.CONCURRENCY_MANAGEMENT_TYPE.equals(element.getQName())) {
            descriptor.setConcurrencyManagementType(ConcurrencyManagementType.valueOf(value));
        } else {
            super.setElementValue(element, value);
        }
    }

    @Override
    public boolean endElement(XMLElement element) {
        if (EjbTagNames.DEPENDS_ON.equals(element.getQName())) {
            inDependsOn = false;
            descriptor.setDependsOn(dependsOn.toArray(new String[dependsOn.size()]));
            dependsOn = null;
        }

        return super.endElement(element);
    }

    @Override
    public Node writeDescriptor(Node parent, String nodeName, EjbSessionDescriptor ejbDesc) {
        Node ejbNode = super.writeDescriptor(parent, nodeName, ejbDesc);
        writeDisplayableComponentInfo(ejbNode, ejbDesc);
        writeCommonHeaderEjbDescriptor(ejbNode, ejbDesc);
        appendTextChild(ejbNode, EjbTagNames.SESSION_TYPE, ejbDesc.getSessionType());

        if (ejbDesc.hasStatefulTimeout()) {

            TimeoutValueNode timeoutValueNode = new TimeoutValueNode();
            TimeoutValueDescriptor timeoutDesc = new TimeoutValueDescriptor();
            timeoutDesc.setValue(ejbDesc.getStatefulTimeoutValue());
            timeoutDesc.setUnit(ejbDesc.getStatefulTimeoutUnit());
            timeoutValueNode.writeDescriptor(ejbNode, EjbTagNames.STATEFUL_TIMEOUT,
                    timeoutDesc);

        }

        MethodNode methodNode = new MethodNode();

        if (ejbDesc.isTimedObject()) {
            if (ejbDesc.getEjbTimeoutMethod() != null) {

                methodNode.writeJavaMethodDescriptor
                        (ejbNode, EjbTagNames.TIMEOUT_METHOD,
                                ejbDesc.getEjbTimeoutMethod());
            }

            for (ScheduledTimerDescriptor timerDesc : ejbDesc.getScheduledTimerDescriptors()) {
                ScheduledTimerNode timerNode = new ScheduledTimerNode();
                timerNode.writeDescriptor(ejbNode, EjbTagNames.TIMER, timerDesc);
            }
        }

        if (ejbDesc.isSingleton()) {
            appendTextChild(ejbNode, EjbTagNames.INIT_ON_STARTUP,
                    Boolean.toString(ejbDesc.getInitOnStartup()));
        }

        if (!ejbDesc.isStateless()) {
            appendTextChild(ejbNode, EjbTagNames.CONCURRENCY_MANAGEMENT_TYPE,
                    ejbDesc.getConcurrencyManagementType().toString());
        }

        for (EjbSessionDescriptor.AccessTimeoutHolder next : ejbDesc.getAccessTimeoutInfo()) {
            ConcurrentMethodDescriptor cDesc = new ConcurrentMethodDescriptor();
            cDesc.setConcurrentMethod(next.method);
            TimeoutValueDescriptor timeoutDesc = new TimeoutValueDescriptor();
            timeoutDesc.setValue(next.value);
            timeoutDesc.setUnit(next.unit);
            cDesc.setAccessTimeout(timeoutDesc);

            ConcurrentMethodNode cNode = new ConcurrentMethodNode();
            cNode.writeDescriptor(ejbNode, EjbTagNames.CONCURRENT_METHOD, cDesc);
        }

        for (MethodDescriptor nextReadLock : ejbDesc.getReadLockMethods()) {
            ConcurrentMethodDescriptor cDesc = new ConcurrentMethodDescriptor();
            cDesc.setConcurrentMethod(nextReadLock);
            cDesc.setWriteLock(false);
            ConcurrentMethodNode cNode = new ConcurrentMethodNode();
            cNode.writeDescriptor(ejbNode, EjbTagNames.CONCURRENT_METHOD, cDesc);
        }

        if (ejbDesc.hasDependsOn()) {
            Node dependsOnNode = appendChild(ejbNode, EjbTagNames.DEPENDS_ON);
            for (String depend : ejbDesc.getDependsOn()) {
                appendTextChild(dependsOnNode, EjbTagNames.EJB_NAME, depend);
            }
        }

        if (ejbDesc.hasInitMethods()) {
            EjbInitNode initNode = new EjbInitNode();
            for (EjbInitInfo next : ejbDesc.getInitMethods()) {
                initNode.writeDescriptor(ejbNode,
                        EjbTagNames.INIT_METHOD, next);
            }
        }

        if (ejbDesc.hasRemoveMethods()) {
            EjbRemoveNode removeNode = new EjbRemoveNode();
            for (EjbRemovalInfo next : ejbDesc.getAllRemovalInfo()) {
                removeNode.writeDescriptor(ejbNode,
                        EjbTagNames.REMOVE_METHOD, next);
            }
        }

        for (MethodDescriptor nextDesc : ejbDesc.getAsynchronousMethods()) {

            methodNode.writeDescriptor(ejbNode, EjbTagNames.ASYNC_METHOD, nextDesc,
                    ejbDesc.getName());

        }

        appendTextChild(ejbNode, EjbTagNames.TRANSACTION_TYPE, ejbDesc.getTransactionType());

        MethodDescriptor afterBeginMethod = ejbDesc.getAfterBeginMethod();
        if (afterBeginMethod != null) {
            methodNode.writeJavaMethodDescriptor(ejbNode, EjbTagNames.AFTER_BEGIN_METHOD,
                    afterBeginMethod);
        }

        MethodDescriptor beforeCompletionMethod = ejbDesc.getBeforeCompletionMethod();
        if (beforeCompletionMethod != null) {
            methodNode.writeJavaMethodDescriptor(ejbNode, EjbTagNames.BEFORE_COMPLETION_METHOD,
                    beforeCompletionMethod);
        }

        MethodDescriptor afterCompletionMethod = ejbDesc.getAfterCompletionMethod();
        if (afterCompletionMethod != null) {
            methodNode.writeJavaMethodDescriptor(ejbNode, EjbTagNames.AFTER_COMPLETION_METHOD,
                    afterCompletionMethod);
        }


        //around-invoke-method
        writeAroundInvokeDescriptors(ejbNode, ejbDesc.getAroundInvokeDescriptors().iterator());

        //around-timeout-method
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

        // post-activate-method
        writeLifeCycleCallbackDescriptors(ejbNode, EjbTagNames.POST_ACTIVATE_METHOD, ejbDesc.getPostActivateDescriptors());

        // pre-passivate-method
        writeLifeCycleCallbackDescriptors(ejbNode, EjbTagNames.PRE_PASSIVATE_METHOD, ejbDesc.getPrePassivateDescriptors());

        // security-role-ref*
        writeRoleReferenceDescriptors(ejbNode, ejbDesc.getRoleReferences().iterator());

        // security-identity
        writeSecurityIdentityDescriptor(ejbNode, ejbDesc);

        // passivation-capable
        if (ejbDesc.isStateful()) {
            appendTextChild(ejbNode, EjbTagNames.PASSIVATION_CAPABLE,
                    Boolean.toString(ejbDesc.isPassivationCapable()));
        }
        
        return ejbNode;
    }
}

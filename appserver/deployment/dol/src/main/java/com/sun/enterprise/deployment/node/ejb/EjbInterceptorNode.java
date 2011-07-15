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

package com.sun.enterprise.deployment.node.ejb;

import com.sun.enterprise.deployment.EjbBundleDescriptor;
import com.sun.enterprise.deployment.EjbInterceptor;
import static com.sun.enterprise.deployment.LifecycleCallbackDescriptor.CallbackType;
import com.sun.enterprise.deployment.MessageDestinationReferenceDescriptor;
import com.sun.enterprise.deployment.node.*;
import com.sun.enterprise.deployment.types.EjbReference;
import com.sun.enterprise.deployment.util.DOLUtils;
import com.sun.enterprise.deployment.xml.EjbTagNames;
import com.sun.enterprise.deployment.xml.TagNames;
import com.sun.enterprise.deployment.xml.WebServicesTagNames;
import org.w3c.dom.Node;

import java.util.Map;
import java.util.logging.Level;

public class EjbInterceptorNode extends DeploymentDescriptorNode {
    private EjbInterceptor descriptor;
    private CallbackType tempType;

    public EjbInterceptorNode() {
        super();

        registerElementHandler(new XMLElement(EjbTagNames.AROUND_INVOKE_METHOD), AroundInvokeNode.class, "addAroundInvokeDescriptor");       
        registerElementHandler(new XMLElement(EjbTagNames.AROUND_TIMEOUT_METHOD), AroundTimeoutNode.class, "addAroundTimeoutDescriptor");       
        registerElementHandler(new XMLElement(EjbTagNames.POST_ACTIVATE_METHOD), LifecycleCallbackNode.class, "addPostActivateDescriptor");       
        registerElementHandler(new XMLElement(EjbTagNames.PRE_PASSIVATE_METHOD), LifecycleCallbackNode.class, "addPrePassivateDescriptor");       

        //jndiEnvironmentRefsGroup
        registerElementHandler(new XMLElement(TagNames.POST_CONSTRUCT), LifecycleCallbackNode.class, "addPostConstructDescriptor");       
        registerElementHandler(new XMLElement(TagNames.PRE_DESTROY), LifecycleCallbackNode.class, "addPreDestroyDescriptor");
        registerElementHandler(new XMLElement(TagNames.DATA_SOURCE), DataSourceDefinitionNode.class, "addDataSourceDefinitionDescriptor");

        registerElementHandler(new XMLElement(TagNames.ENVIRONMENT_PROPERTY), 
               EnvEntryNode.class, "addEnvironmentProperty");
        registerElementHandler(new XMLElement(EjbTagNames.EJB_REFERENCE), EjbReferenceNode.class);     
        registerElementHandler(new XMLElement(EjbTagNames.EJB_LOCAL_REFERENCE), EjbLocalReferenceNode.class);     
        registerElementHandler(new XMLElement(WebServicesTagNames.SERVICE_REF), ServiceReferenceNode.class, "addServiceReferenceDescriptor");
        registerElementHandler(new XMLElement(EjbTagNames.RESOURCE_REFERENCE), 
               ResourceRefNode.class, "addResourceReferenceDescriptor");   
        registerElementHandler(new XMLElement(TagNames.RESOURCE_ENV_REFERENCE),
               ResourceEnvRefNode.class, "addJmsDestinationReferenceDescriptor");               
        registerElementHandler(new XMLElement(TagNames.MESSAGE_DESTINATION_REFERENCE), MessageDestinationRefNode.class);
        registerElementHandler(new XMLElement(TagNames.PERSISTENCE_CONTEXT_REF), EntityManagerReferenceNode.class, "addEntityManagerReferenceDescriptor");
        registerElementHandler(new XMLElement(TagNames.PERSISTENCE_UNIT_REF), EntityManagerFactoryReferenceNode.class, "addEntityManagerFactoryReferenceDescriptor");
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
        table.put(EjbTagNames.INTERCEPTOR_CLASS, "setInterceptorClassName");
        return table;
    }    

   /**
    * @return the descriptor instance to associate with this XMLNode
    */
    public Object getDescriptor() {
        
        if (descriptor==null) {
            descriptor = (EjbInterceptor)DescriptorFactory.getDescriptor(getXMLPath());
            descriptor.setEjbBundleDescriptor((EjbBundleDescriptor)getParentNode().getDescriptor());
        }
        return descriptor;
    }

    private EjbInterceptor getInterceptor() {
        return (EjbInterceptor)getDescriptor();
    }

    /**
     * Adds  a new DOL descriptor instance to the descriptor instance associated with 
     * this XMLNode
     *
     * @param descriptor the new descriptor
     */    
    public void addDescriptor(Object  newDescriptor) {       
        if (newDescriptor instanceof EjbReference) {            
            if (DOLUtils.getDefaultLogger().isLoggable(Level.FINE)) {
                DOLUtils.getDefaultLogger().fine("Adding ejb ref " + newDescriptor);
            }
            getInterceptor().addEjbReferenceDescriptor(
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
            getInterceptor().addMessageDestinationReferenceDescriptor
                (msgDestRef);
        } else {
            super.addDescriptor(newDescriptor);
        }
    }      

   /**
     * write the relationships descriptor class to a DOM tree and return it
     *
     * @param parent node in the DOM tree 
     * @param node name for the root element of this xml fragment      
     * @param the descriptor to write
     * @return the DOM tree top node
     */
    public Node writeDescriptor(Node parent, String nodeName, EjbInterceptor descriptor) {
        Node interceptorNode = appendChild(parent, nodeName);

        writeLocalizedDescriptions(interceptorNode, descriptor);
       
        appendTextChild(interceptorNode, EjbTagNames.INTERCEPTOR_CLASS, descriptor.getInterceptorClassName());     

        if (descriptor.hasAroundInvokeDescriptor()) {
            writeAroundInvokeDescriptors(interceptorNode,
                descriptor.getAroundInvokeDescriptors().iterator());
        }
        if (descriptor.hasAroundTimeoutDescriptor()) {
            writeAroundTimeoutDescriptors(interceptorNode,
                descriptor.getAroundTimeoutDescriptors().iterator());
        }
        if (descriptor.hasCallbackDescriptor(CallbackType.POST_CONSTRUCT)) {
            writePostConstructDescriptors(interceptorNode,
                descriptor.getCallbackDescriptors(CallbackType.POST_CONSTRUCT).iterator());
        }
        if (descriptor.hasCallbackDescriptor(CallbackType.PRE_DESTROY)) {
            writePreDestroyDescriptors(interceptorNode,
                descriptor.getCallbackDescriptors(CallbackType.PRE_DESTROY).iterator());
        }
        if (descriptor.hasCallbackDescriptor(CallbackType.POST_ACTIVATE)) {
            writePostActivateDescriptors(interceptorNode,
                descriptor.getCallbackDescriptors(CallbackType.POST_ACTIVATE).iterator());
        }
        if (descriptor.hasCallbackDescriptor(CallbackType.PRE_PASSIVATE)) {
            writePrePassivateDescriptors(interceptorNode,
                descriptor.getCallbackDescriptors(CallbackType.PRE_PASSIVATE).iterator());
        }

        //TODO V3 should we check for the availability of datasource-definition similar to above ? (hasCallbackDescriptor)
       // datasource-definition*
       writeDataSourceDefinitionDescriptors(interceptorNode, descriptor.getDataSourceDefinitionDescriptors().iterator());


        return interceptorNode;
    }
}

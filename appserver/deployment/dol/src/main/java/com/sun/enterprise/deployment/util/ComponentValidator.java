/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

/*
 * ComponentValidator.java
 *
 * Created on August 15, 2002, 5:51 PM
 */

package com.sun.enterprise.deployment.util;

import com.sun.enterprise.deployment.*;
import com.sun.enterprise.deployment.types.MessageDestinationReferencer;
import org.glassfish.deployment.common.XModuleType;

import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Level;

/**
 *
 * @author  dochez
 */
public class ComponentValidator extends DefaultDOLVisitor implements ComponentVisitor {
    
    /**
     * Visits a message destination referencer for the last J2EE 
     * component visited
     * @param the message destination referencer
     */
    public void accept(MessageDestinationReferencer msgDestReferencer) {

        // if it is linked to a logical destination
        if( msgDestReferencer.isLinkedToMessageDestination() ) {
            return;
        // if it is referred to a physical destination
        } else if (msgDestReferencer.ownedByMessageDestinationRef() && 
            msgDestReferencer.getMessageDestinationRefOwner(
            ).getJndiName() != null) {
            return;
        } else {
            MessageDestinationDescriptor msgDest = 
                msgDestReferencer.resolveLinkName();
            if( msgDest == null ) {
                String linkName = 
                    msgDestReferencer.getMessageDestinationLinkName();
                DOLUtils.getDefaultLogger().log(Level.WARNING, "enterprise.deployment.backend.invalidDescriptorMappingFailure",
                    new Object[] {"message-destination", linkName});
            } else {
                if (msgDestReferencer instanceof MessageDestinationReferenceDescriptor) {
                    ((MessageDestinationReferenceDescriptor)msgDestReferencer).setJndiName(msgDest.getJndiName());
                }
            }                                                 
        }
    }    

    /**
     * Visits a service reference for the last J2EE component visited
     * 
     * @param the service reference
     */
    public void accept(ServiceReferenceDescriptor serviceRef) {

        Set portsInfo = serviceRef.getPortsInfo();

        for(Iterator iter = portsInfo.iterator(); iter.hasNext();) {
            ServiceRefPortInfo next = (ServiceRefPortInfo) iter.next();

            if( next.hasPortComponentLinkName() &&
                !next.isLinkedToPortComponent() ) {
                WebServiceEndpoint portComponentLink = next.resolveLinkName();
                if( portComponentLink == null ) {
                    String linkName = next.getPortComponentLinkName(); 
                    DOLUtils.getDefaultLogger().log(Level.WARNING, "enterprise.deployment.backend.invalidDescriptorMappingFailure",
                        new Object[] {"port-component" , linkName});
                }                                                   
            }

        }
    }    

    public void accept(ResourceReferenceDescriptor resRef) {
        computeRuntimeDefault(resRef);
    }

    public void accept(JmsDestinationReferenceDescriptor jmsDestRef) {

        if (jmsDestRef.getJndiName() == null ||
                jmsDestRef.getJndiName().length() == 0) {
            Map<String, ManagedBeanDescriptor> managedBeanMap = getManagedBeanMap();
            String refType = jmsDestRef.getRefType();
            if( managedBeanMap.containsKey(refType) ) {
                ManagedBeanDescriptor desc = managedBeanMap.get(refType);

                // In app-client, keep lookup local to JVM so it doesn't need to access
                // server's global JNDI namespace for managed bean.
                String jndiName = ( bundleDescriptor.getModuleType() == XModuleType.CAR )
                        ?  desc.getAppJndiName() : desc.getGlobalJndiName();

                jmsDestRef.setJndiName(jndiName);
                jmsDestRef.setIsManagedBean(true);
                jmsDestRef.setManagedBeanDescriptor(desc);
            }
        }

        computeRuntimeDefault(jmsDestRef);
    }

    public void accept(MessageDestinationReferenceDescriptor msgDestRef) {
        computeRuntimeDefault(msgDestRef);
    }

    public void accept(MessageDestinationDescriptor msgDest) {
        computeRuntimeDefault(msgDest);
    }

    /**
     * Get a map of bean class to managed bean descriptor for the managed beans
     * defined within the current module.
     */
    private Map<String, ManagedBeanDescriptor> getManagedBeanMap() {
        BundleDescriptor thisBundle = getBundleDescriptor();

        Set<ManagedBeanDescriptor> managedBeans = new HashSet<ManagedBeanDescriptor>();

        // Make sure we're dealing with the top-level bundle descriptor when looking
        // for managed beans
        if( thisBundle != null ) {
            Object desc = thisBundle.getModuleDescriptor().getDescriptor();
            if( desc instanceof BundleDescriptor ) {
                managedBeans = ((BundleDescriptor)desc).getManagedBeans();
            }
        }

        Map<String, ManagedBeanDescriptor> managedBeanMap = new HashMap<String, ManagedBeanDescriptor>();

        for(ManagedBeanDescriptor managedBean : managedBeans ) {

            String beanClassName = managedBean.getBeanClassName();
            managedBeanMap.put(beanClassName, managedBean);
        }

        return managedBeanMap;

    }

    /**
     * Set runtime default value for ResourceReferenceDescriptor.
     */
    private void computeRuntimeDefault(ResourceReferenceDescriptor resRef) {
        if (resRef.getType() != null && resRef.getType().equals("org.omg.CORBA.ORB")) {
            resRef.setJndiName("java:comp/ORB");
        }

        else if (resRef.getJndiName() == null ||
                resRef.getJndiName().length() == 0) {
            resRef.setJndiName(getDefaultResourceJndiName(resRef.getName()));
        }
    }

    /**
     * Set runtime default value for JmsDestinationReferenceDescriptor.
     */
    private void computeRuntimeDefault(JmsDestinationReferenceDescriptor jmsDestRef) {
        if (jmsDestRef.getRefType() != null && jmsDestRef.getRefType().equals(
            "javax.transaction.UserTransaction")) {
            jmsDestRef.setJndiName("java:comp/UserTransaction");
        }

        else if (jmsDestRef.getRefType() != null && jmsDestRef.getRefType().equals("javax.transaction.TransactionSynchronizationRegistry")) {
            jmsDestRef.setJndiName(
                "java:comp/TransactionSynchronizationRegistry");
        }

        else if (jmsDestRef.getJndiName() == null ||
                jmsDestRef.getJndiName().length() == 0) {
            jmsDestRef.setJndiName(getDefaultResourceJndiName(jmsDestRef.getName()));
        }
    }

    /**
     * Set runtime default value for MessageDestinationReferenceDescriptor.
     */
    private void computeRuntimeDefault(MessageDestinationReferenceDescriptor msgDestRef) {
        if (msgDestRef.getJndiName() == null ||
                msgDestRef.getJndiName().length() == 0) {
            msgDestRef.setJndiName(getDefaultResourceJndiName(msgDestRef.getName()));
        }
    }

    /**
     * Set runtime default value for MessageDestinationDescriptor.
     */
    private void computeRuntimeDefault(MessageDestinationDescriptor msgDest) {
        if (msgDest.getJndiName() == null ||
                msgDest.getJndiName().length() == 0) {
            msgDest.setJndiName(getDefaultResourceJndiName(msgDest.getName()));
        }
    }

    /**
     * @param resName
     * @return default jndi name for a given interface resource name
     */
    private String getDefaultResourceJndiName(String resName) {
        return resName;
    }
}

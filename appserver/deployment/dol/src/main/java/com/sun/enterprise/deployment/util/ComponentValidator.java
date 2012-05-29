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

/*
 * ComponentValidator.java
 *
 * Created on August 15, 2002, 5:51 PM
 */

package com.sun.enterprise.deployment.util;

import com.sun.enterprise.deployment.*;
import com.sun.enterprise.deployment.types.*;

import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Level;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import javax.security.auth.Subject;
import java.security.AccessController;
import java.security.Principal;
import java.security.PrivilegedAction;


/**
 *
 * @author  dochez
 */
public class ComponentValidator extends DefaultDOLVisitor implements ComponentVisitor {
    
    protected BundleDescriptor bundleDescriptor = null;

    protected Application application;

    /**
     * @return the Application object if any
     */
    protected Application getApplication() {
        return application;
    }

    /**
     * @return the bundleDescriptor we are visiting
     */
    protected BundleDescriptor getBundleDescriptor() {
        return bundleDescriptor;
    }

    public void accept(BundleDescriptor bundleDescriptor) {
        this.bundleDescriptor = bundleDescriptor;

        super.accept(bundleDescriptor);
    }

    /**
     * Visits a message destination referencer for the last J2EE 
     * component visited
     * @param the message destination referencer
     */
    protected void accept(MessageDestinationReferencer msgDestReferencer) {

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
    protected void accept(ServiceReferenceDescriptor serviceRef) {

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

    protected void accept(ResourceReferenceDescriptor resRef) {
        computeRuntimeDefault(resRef);
    }

    protected void accept(ResourceEnvReferenceDescriptor resourceEnvRef) {

        if (resourceEnvRef.getJndiName() == null ||
                resourceEnvRef.getJndiName().length() == 0) {
            Map<String, ManagedBeanDescriptor> managedBeanMap = getManagedBeanMap();
            String refType = resourceEnvRef.getRefType();
            if( managedBeanMap.containsKey(refType) ) {
                ManagedBeanDescriptor desc = managedBeanMap.get(refType);

                // In app-client, keep lookup local to JVM so it doesn't need to access
                // server's global JNDI namespace for managed bean.
                String jndiName = ( bundleDescriptor.getModuleType() == DOLUtils.carType() )
                        ?  desc.getAppJndiName() : desc.getGlobalJndiName();

                resourceEnvRef.setJndiName(jndiName);
                resourceEnvRef.setIsManagedBean(true);
                resourceEnvRef.setManagedBeanDescriptor(desc);
            }
        }

        computeRuntimeDefault(resourceEnvRef);
    }

    protected void accept(MessageDestinationReferenceDescriptor msgDestRef) {
        computeRuntimeDefault(msgDestRef);
    }

    protected void accept(MessageDestinationDescriptor msgDest) {
        computeRuntimeDefault(msgDest);
    }

    /**
     * visits all entries within the component environment for which
     * isInjectable() == true.
     * @param injectable InjectionCapable environment dependency
     */
    protected void accept(InjectionCapable injectable) {
        acceptWithCL(injectable);
        acceptWithoutCL(injectable);
    }

    // we need to split the accept(InjectionCapable) into two parts:
    // one needs classloader and one doesn't. This is needed because
    // in the standalone war case, the classloader is not created
    // untill the web module is being started.

    protected void acceptWithCL(InjectionCapable injectable) {
        // If parsed from deployment descriptor, we need to determine whether
        // the inject target name refers to an injection field or an
        // injection method for each injection target
        for (InjectionTarget target : injectable.getInjectionTargets()) {
            if( (target.getFieldName() == null) &&
                    (target.getMethodName() == null) ) {

                String injectTargetName = target.getTargetName();
                String targetClassName  = target.getClassName();
                ClassLoader classLoader = getBundleDescriptor().getClassLoader();

                Class targetClazz = null;

                try {

                    targetClazz = classLoader.loadClass(targetClassName);

                } catch(ClassNotFoundException cnfe) {
                    // @@@
                    // Don't treat this as a fatal error for now.  One known issue
                    // is that all .xml, even web.xml, is processed within the
                    // appclient container during startup.  In that case, there
                    // are issues with finding .classes in .wars due to the
                    // structure of the returned client .jar and the way the
                    // classloader is formed.
                    DOLUtils.getDefaultLogger().fine
                            ("Injection class " + targetClassName + " not found for " +
                            injectable);
                    return;
                }

                // Spec requires that we attempt to match on method before field.
                boolean matched = false;

                // The only information we have is method name, so iterate
                // through the methods find a match.  There is no overloading
                // allowed for injection methods, so any match is considered
                // the only possible match.

                String setterMethodName = TypeUtil.
                        propertyNameToSetterMethod(injectTargetName);

                // method can have any access type so use getDeclaredMethods()
                for(Method next : targetClazz.getDeclaredMethods()) {
                    // only when the method name matches and the method
                    // has exactly one parameter, we find a match
                    if( next.getName().equals(setterMethodName) &&
                        next.getParameterTypes().length == 1) {
                        target.setMethodName(next.getName());
                        if( injectable.getInjectResourceType() == null ) {
                            Class[] paramTypes = next.getParameterTypes();
                            if (paramTypes.length == 1) {
                                String resourceType = paramTypes[0].getName();
                                injectable.setInjectResourceType(resourceType);
                            }
                        }
                        matched = true;
                        break;
                    }
                }

               if( !matched ) {

                    // In the case of injection fields, inject target name ==
                    // field name.  Field can have any access type.
                    try {
                        Field f = targetClazz.getDeclaredField(injectTargetName);
                        target.setFieldName(injectTargetName);
                        if( injectable.getInjectResourceType() == null ) {
                            String resourceType = f.getType().getName();
                            injectable.setInjectResourceType(resourceType);
                        }
                        matched = true;
                    } catch(NoSuchFieldException nsfe) {
                        String msg = "No matching injection setter method or " +
                                "injection field found for injection property " +
                                injectTargetName + " on class " + targetClassName +
                                " for component dependency " + injectable;

                        throw new RuntimeException(msg, nsfe);
                    }
                }
            }
        }
    }

    protected void acceptWithoutCL(InjectionCapable injectable) {
    }

    /**
     * Set a default RunAs principal to given RunAsIdentityDescriptor
     * if necessary.
     * @param runAs
     * @param application
     * @exception RuntimeException
     */
    protected void computeRunAsPrincipalDefault(RunAsIdentityDescriptor runAs,
            Application application) {

        // for backward compatibile
        if (runAs != null &&
                (runAs.getRoleName() == null ||
                    runAs.getRoleName().length() == 0)) {
            DOLUtils.getDefaultLogger().log(Level.WARNING,
            "enterprise.deployment.backend.emptyRoleName");
            return;
        }

        if (runAs != null &&
                (runAs.getPrincipal() == null ||
                    runAs.getPrincipal().length() == 0) &&
                application != null && application.getRoleMapper() != null) {

            String principalName = null;
            String roleName = runAs.getRoleName();

            final Subject fs = (Subject)application.getRoleMapper().getRoleToSubjectMapping().get(roleName);
            if (fs != null) {
                principalName = (String)AccessController.doPrivileged(new PrivilegedAction() {
                    public Object run() {
                        Set<Principal> pset = fs.getPrincipals();
                        Principal prin = null;
                        if (pset.size() > 0) {
                            prin = (Principal)pset.iterator().next();
                            DOLUtils.getDefaultLogger().log(Level.WARNING,
                            "enterprise.deployment.backend.computeRunAsPrincipal",
                            new Object[] { prin.getName() });
                        }
                        return (prin != null) ? prin.getName() : null;
                    }
                });
            }

            if (principalName == null || principalName.length() == 0) {
                throw new RuntimeException("The RunAs role " + "\"" + roleName + "\"" +
                    " is not mapped to a principal.");
            }
            runAs.setPrincipal(principalName);
        }
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
     * Set runtime default value for ResourceEnvReferenceDescriptor.
     */
    private void computeRuntimeDefault(ResourceEnvReferenceDescriptor resourceEnvRef) {
        if (resourceEnvRef.getRefType() != null && resourceEnvRef.getRefType().equals(
            "javax.transaction.UserTransaction")) {
            resourceEnvRef.setJndiName("java:comp/UserTransaction");
        }

        else if (resourceEnvRef.getRefType() != null && resourceEnvRef.getRefType().equals("javax.transaction.TransactionSynchronizationRegistry")) {
            resourceEnvRef.setJndiName(
                "java:comp/TransactionSynchronizationRegistry");
        }

        else if (resourceEnvRef.getJndiName() == null ||
                resourceEnvRef.getJndiName().length() == 0) {
            resourceEnvRef.setJndiName(getDefaultResourceJndiName(resourceEnvRef.getName()));
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

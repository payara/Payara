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

package com.sun.enterprise.deployment.util;

import com.sun.enterprise.deployment.*;
import com.sun.enterprise.deployment.types.EjbReference;
import com.sun.enterprise.deployment.types.MessageDestinationReferencer;
import org.glassfish.deployment.common.Descriptor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Iterator;

/**
 * Default implementation of all the DOL visitor interface for convenience
 *
 * @author  Jerome Dochez
 * @version 
 */

public class DefaultDOLVisitor implements ApplicationVisitor, EjbBundleVisitor, EjbVisitor, ManagedBeanVisitor, WebBundleVisitor {
   protected BundleDescriptor bundleDescriptor = null;

    /** Creates new DefaultDOLVisitor */
    public DefaultDOLVisitor() {
    }
    
    /**
     * visit an application object
     * @param the application descriptor
     */
    public void accept(Application application) {
    }
    
    /**
     * visits an ejb bundle descriptor
     * @param an ejb bundle descriptor
     */
    public void accept(EjbBundleDescriptor bundleDescriptor) {
        this.bundleDescriptor = bundleDescriptor;
    }

    /**
     * visits an ejb descriptor
     * @param ejb descriptor
     */
    public void accept(EjbDescriptor ejb) {
    }

    public void accept(InjectionCapable injectable) {
    }
    
    /**
     * visits an ejb reference for the last J2EE component visited
     * @param the ejb reference
     */
    public void accept(EjbReference ejbRef) {
    }

    public void accept(MessageDestinationReferencer msgDestReferencer) {
    }

    /**
     * visits a web service reference descriptor
     * @param serviceRef 
     */
    public void accept(ServiceReferenceDescriptor serviceRef) {
    }

    /**
     * visits a web service definition
     * @param web service
     */
    public void accept(WebService webService) {
    }

    /**
     * visits a method permission and permitted methods  for the last J2EE component visited
     * @param method permission 
     * @param the methods associated with the above permission
     */
    public void accept(MethodPermission pm, Iterator methods) {
    }
    
    /**
     * visits a role reference  for the last J2EE component visited
     * @param role reference
     */
    public void accept(RoleReference roleRef) {
    }
    
    /**
     * visists a method transaction  for the last J2EE component visited
     * @param method descriptor the method
     * @param container transaction
     */
    public void accept(MethodDescriptor method, ContainerTransaction ct) {
    }
    
    /**
     * visits an environment property  for the last J2EE component visited
     * @param the environment property
     */
    public void accept(EnvironmentProperty envEntry) {
    }

    /**
     * visits an resource reference for the last J2EE component visited
     * @param the resource reference
     */
    public void accept(ResourceReferenceDescriptor resRef) {
    }

    /**
     * visits an jms destination reference for the last J2EE component visited
     * @param the jms destination reference
     */
    public void accept(JmsDestinationReferenceDescriptor jmsDestRef) {
    }

    /**
     * visits an message destination reference for the last J2EE component visited
     * @param the message destination reference
     */
    public void accept(MessageDestinationReferenceDescriptor msgDestRef) {
    }

    /**
     * @return a EjbVisitor (if ejbs should be visited)
     */
    public EjbVisitor getEjbVisitor() {
        return this;
    }

    /**
     * visits an message destination for the last J2EE component visited
     * @param the message destination
     */
    public void accept(MessageDestinationDescriptor msgDest) {
    }

    /**
     * visits a CMP field definition (for CMP entity beans)
     * @param field descriptor for the CMP field
     */
    public void accept(FieldDescriptor fd) {
    }
    
    /**
     * visits a query method
     * @param method descriptor for the method
     * @param query descriptor
     */
    public void accept(MethodDescriptor method, QueryDescriptor qd) {
    }
    
    /**
     * visits an ejb relationship descriptor
     * @param the relationship descriptor
     */
    public void accept(RelationshipDescriptor descriptor) {
    }
    
    /**
     * visits a J2EE descriptor
     * @param the descriptor
     */
    public void accept(Descriptor descriptor) {
    }
    
    /**
     * visit a web bundle descriptor
     *
     * @param the web bundle descriptor
     */
    public void accept(WebBundleDescriptor descriptor) {
        this.bundleDescriptor = descriptor;
    }

    public void accept(ManagedBeanDescriptor descriptor) {
        this.bundleDescriptor = descriptor.getBundle();
    }
    
    /**
     * visit a web component descriptor
     *
     * @param the web component
     */
    public void accept(WebComponentDescriptor descriptor) {
    }

    /**
     * visit a servlet filter descriptor
     *
     * @param the servlet filter
     */
    public void accept(ServletFilterDescriptor descriptor) {
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
     * @return the bundleDescriptor we are visiting
     */
    protected BundleDescriptor getBundleDescriptor() {
        return bundleDescriptor;
    }
}

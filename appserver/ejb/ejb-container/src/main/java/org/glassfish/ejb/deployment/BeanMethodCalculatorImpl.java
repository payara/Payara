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
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2026] Payara Foundation and/or its affiliates

package org.glassfish.ejb.deployment;

import com.sun.enterprise.deployment.LifecycleCallbackDescriptor;
import com.sun.enterprise.deployment.MethodDescriptor;
import com.sun.logging.LogDomains;
import jakarta.ejb.EJBHome;
import jakarta.ejb.EJBLocalHome;
import jakarta.ejb.EJBLocalObject;
import jakarta.ejb.EJBObject;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.deployment.common.DeploymentUtils;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbSessionDescriptor;
import org.glassfish.ejb.deployment.descriptor.ScheduledTimerDescriptor;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.deployment.ClassHierarchy;
import org.glassfish.internal.deployment.ClassLoaderIndex;
import org.glassfish.internal.deployment.JandexIndexer;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class to calculate the list of methods required to have transaction
 * attributes
 *
 * @author Jerome Dochez
 */
public final class BeanMethodCalculatorImpl {

    private BeanMethodCalculatorImpl() {
        throw new UnsupportedOperationException("util");
    }

    // TODO - change logger if/when other EJB deployment classes are changed
    static final private Logger _logger = LogDomains.getLogger(BeanMethodCalculatorImpl.class, LogDomains.DPL_LOGGER);

    // from EJB 2.0 spec section 17.4.1: the methods of the EJB API interfaces that cannot carry a
    // transaction attribute, "*" standing for all of them
    private static final String[] HOME_METHODS_DISALLOWED = { "getEJBMetaData", "getHomeHandle" };
    private static final String[] OBJECT_METHODS_DISALLOWED = { "getEJBHome", "getHandle", "getPrimaryKey", "isIdentical" };
    private static final String[] LOCAL_HOME_METHODS_DISALLOWED = {};
    private static final String[] LOCAL_OBJECT_METHODS_DISALLOWED = { "getEJBLocalHome", "getPrimaryKey", "isIdentical" };
    private static final String[] SESSION_BEAN_METHODS_DISALLOWED = { "*" };

    /**
     * @return the index in which the bean's classes resolve without being loaded: the indexes of the
     * deployment in progress completed by the class loader, or the class files the class loader can see
     */
    public static IndexView indexFor(ClassLoader classLoader) {
        DeploymentContext deploymentContext = DeploymentUtils.getCurrentDeploymentContext();
        if (deploymentContext != null) {
            JandexIndexer indexer = Globals.getDefaultHabitat().getService(JandexIndexer.class);
            return JandexIndexer.compositeIndex(indexer.getAllIndexes(deploymentContext).values(), classLoader);
        }
        return new ClassLoaderIndex(classLoader);
    }

    /**
     * @return the class in the index
     * @throws ClassNotFoundException when the index does not know the class
     */
    public static ClassInfo classInfo(IndexView index, String className) throws ClassNotFoundException {
        ClassInfo classInfo = index.getClassByName(DotName.createSimple(className));
        if (classInfo == null) {
            throw new ClassNotFoundException(className);
        }
        return classInfo;
    }

    /**
     * The methods of the bean's client views, as {@code java.lang.reflect.Method} objects for the
     * container: this loads the classes and is meant for the running application.
     */
    public static Vector<Method> getMethodsFor(com.sun.enterprise.deployment.EjbDescriptor ejbDescriptor, ClassLoader classLoader)
            throws ClassNotFoundException
    {
        Vector<Method> methods = new Vector<>();

        if (ejbDescriptor.isRemoteInterfacesSupported()) {
            addAllInterfaceMethodsIn(methods, classLoader.loadClass(ejbDescriptor.getHomeClassName()));
            addAllInterfaceMethodsIn(methods, classLoader.loadClass(ejbDescriptor.getRemoteClassName()));
        }

        if (ejbDescriptor.isRemoteBusinessInterfacesSupported()) {
            for(String intf : ejbDescriptor.getRemoteBusinessClassNames()) {
                addAllInterfaceMethodsIn(methods, classLoader.loadClass(intf));
            }
        }

        if (ejbDescriptor.isLocalInterfacesSupported()) {
            addAllInterfaceMethodsIn(methods, classLoader.loadClass(ejbDescriptor.getLocalHomeClassName()));
            addAllInterfaceMethodsIn(methods, classLoader.loadClass(ejbDescriptor.getLocalClassName()));
        }

        if (ejbDescriptor.isLocalBusinessInterfacesSupported()) {
            for(String intf : ejbDescriptor.getLocalBusinessClassNames()) {
                addAllInterfaceMethodsIn(methods, classLoader.loadClass(intf));
            }
        }

        if (ejbDescriptor.isLocalBean()) {
            addAllInterfaceMethodsIn(methods, classLoader.loadClass(ejbDescriptor.getEjbClassName()));
        }

        if (ejbDescriptor.hasWebServiceEndpointInterface()) {
            addAllInterfaceMethodsIn(methods, classLoader.loadClass(ejbDescriptor.getWebServiceEndpointInterfaceName()));
        }
        return methods;
    }

    private static void addAllInterfaceMethodsIn(Collection<Method> methods, Class<?> c) {
        methods.addAll(Arrays.asList(c.getMethods()));
    }

    /**
     * The methods of the bean that can carry a transaction attribute, read from the index without
     * loading any class, see {@link #indexFor(ClassLoader)}.
     */
    public static Collection<MethodDescriptor> getTransactionalMethodsFor(com.sun.enterprise.deployment.EjbDescriptor desc, IndexView index)
            throws ClassNotFoundException
    {
        EjbDescriptor ejbDescriptor = (EjbDescriptor) desc;
        // only set if desc is a stateful session bean.  NOTE that
        // !statefulSessionBean does not imply stateless session bean
        boolean statefulSessionBean = false;

        Vector<MethodDescriptor> methods = new Vector<>();
        if (ejbDescriptor instanceof EjbSessionDescriptor) {
            statefulSessionBean = ((EjbSessionDescriptor) ejbDescriptor).isStateful();
            boolean singletonSessionBean = ((EjbSessionDescriptor) ejbDescriptor).isSingleton();

            // Session Beans
            if (ejbDescriptor.isRemoteInterfacesSupported()) {
                transformAndAdd(getTransactionMethodsFor(index, ejbDescriptor.getRemoteClassName(),
                        disallowedMethodsFor(index, EJBObject.class, SESSION_BEAN_METHODS_DISALLOWED)),
                        MethodDescriptor.EJB_REMOTE, methods);
            }

            if( ejbDescriptor.isRemoteBusinessInterfacesSupported() ) {
                for(String intfName : ejbDescriptor.getRemoteBusinessClassNames() ) {
                    transformAndAdd(ClassHierarchy.publicMethods(index, classInfo(index, intfName)), MethodDescriptor.EJB_REMOTE, methods);
                }
            }

            if (ejbDescriptor.isLocalInterfacesSupported()) {
                transformAndAdd(getTransactionMethodsFor(index, ejbDescriptor.getLocalClassName(),
                        disallowedMethodsFor(index, EJBLocalObject.class, SESSION_BEAN_METHODS_DISALLOWED)),
                        MethodDescriptor.EJB_LOCAL, methods);
            }

            if( ejbDescriptor.isLocalBusinessInterfacesSupported() ) {
                for(String intfName : ejbDescriptor.getLocalBusinessClassNames() ) {
                    transformAndAdd(ClassHierarchy.publicMethods(index, classInfo(index, intfName)), MethodDescriptor.EJB_LOCAL, methods);
                }
            }

            if( ejbDescriptor.isLocalBean() ) {
                // the no-interface view is the bean class itself, java.lang.Object's methods included
                transformAndAdd(ClassHierarchy.publicMethods(index, classInfo(index, ejbDescriptor.getEjbClassName()), true),
                        MethodDescriptor.EJB_LOCAL, methods);
            }

            if (ejbDescriptor.hasWebServiceEndpointInterface()) {
                transformAndAdd(ClassHierarchy.publicMethods(index, classInfo(index, ejbDescriptor.getWebServiceEndpointInterfaceName())),
                        MethodDescriptor.EJB_WEB_SERVICE, methods);
            }

            // SFSB and Singleton can have lifecycle callbacks transactional
            if (statefulSessionBean || singletonSessionBean) {
                Set<LifecycleCallbackDescriptor> lcds = ejbDescriptor.getLifecycleCallbackDescriptors();
                for(LifecycleCallbackDescriptor lcd : lcds) {
                    try {
                        MethodInfo m = lcd.getLifecycleCallbackMethod(index);
                        methods.add(new MethodDescriptor(m, MethodDescriptor.LIFECYCLE_CALLBACK));
                    } catch(Exception e) {
                        if (_logger.isLoggable(Level.FINE)) {
                            _logger.log(Level.FINE, "Lifecycle callback processing error", e);
                        }
                    }
                }
            }

        } else {
            // entity beans remote interfaces
            String homeIntf = ejbDescriptor.getHomeClassName();
            if (homeIntf!=null) {
                transformAndAdd(getTransactionMethodsFor(index, homeIntf,
                        disallowedMethodsFor(index, EJBHome.class, HOME_METHODS_DISALLOWED)),
                        MethodDescriptor.EJB_HOME, methods);
                transformAndAdd(getTransactionMethodsFor(index, ejbDescriptor.getRemoteClassName(),
                        disallowedMethodsFor(index, EJBObject.class, OBJECT_METHODS_DISALLOWED)),
                        MethodDescriptor.EJB_REMOTE, methods);
            }

            // entity beans local interfaces
            String localHomeIntf = ejbDescriptor.getLocalHomeClassName();
            if (localHomeIntf!=null) {
                transformAndAdd(getTransactionMethodsFor(index, localHomeIntf,
                        disallowedMethodsFor(index, EJBLocalHome.class, LOCAL_HOME_METHODS_DISALLOWED)),
                        MethodDescriptor.EJB_LOCALHOME, methods);
                transformAndAdd(getTransactionMethodsFor(index, ejbDescriptor.getLocalClassName(),
                        disallowedMethodsFor(index, EJBLocalObject.class, LOCAL_OBJECT_METHODS_DISALLOWED)),
                        MethodDescriptor.EJB_LOCAL, methods);
            }
        }

        if( !statefulSessionBean ) {
            if( ejbDescriptor.isTimedObject() ) {
                if( ejbDescriptor.getEjbTimeoutMethod() != null) {
                    methods.add(ejbDescriptor.getEjbTimeoutMethod());
                }
                for (ScheduledTimerDescriptor schd : ejbDescriptor.getScheduledTimerDescriptors()) {
                    methods.add(schd.getTimeoutMethod());
                }
            }
        }

        return methods;
    }

    /**
     * @return the public methods of the interface minus the disallowed EJB API methods it inherits
     */
    private static Collection<MethodInfo> getTransactionMethodsFor(IndexView index, String interfaceName,
                                                                   Collection<MethodInfo> disallowedMethods)
            throws ClassNotFoundException
    {
        List<MethodInfo> methods = new ArrayList<>(ClassHierarchy.publicMethods(index, classInfo(index, interfaceName)));
        methods.removeIf(method -> disallowedMethods.stream().anyMatch(disallowed -> sameMethod(method, disallowed)));
        return methods;
    }

    private static boolean sameMethod(MethodInfo method, MethodInfo other) {
        return method.declaringClass().name().equals(other.declaringClass().name())
                && ClassHierarchy.matches(method, other.name(), ClassHierarchy.javaParameterNames(other));
    }

    /**
     * @return the methods of the EJB API interface with one of the names, all of them for "*"
     */
    private static Collection<MethodInfo> disallowedMethodsFor(IndexView index, Class<?> interfaceType, String[] methodNames) {
        if (methodNames.length == 0) {
            return Collections.emptyList();
        }
        ClassInfo interfaceInfo = index.getClassByName(DotName.createSimple(interfaceType));
        if (interfaceInfo == null) {
            // the EJB API is not resolvable in this index, so nothing in it can be disallowed
            return Collections.emptyList();
        }
        List<MethodInfo> disallowed = new ArrayList<>();
        List<String> names = Arrays.asList(methodNames);
        for (MethodInfo method : ClassHierarchy.publicMethods(index, interfaceInfo)) {
            if (names.contains("*") || names.contains(method.name())) {
                disallowed.add(method);
            }
        }
        return disallowed;
    }

    private static void transformAndAdd(Collection<MethodInfo> methods, String methodIntf, Vector<MethodDescriptor> globalList) {
        for (MethodInfo m : methods) {
            globalList.add(new MethodDescriptor(m, methodIntf));
        }
    }
}

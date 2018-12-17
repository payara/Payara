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

package org.glassfish.ejb.deployment;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.enterprise.deployment.LifecycleCallbackDescriptor;
import com.sun.enterprise.deployment.MethodDescriptor;
import com.sun.logging.LogDomains;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbSessionDescriptor;
import org.glassfish.ejb.deployment.descriptor.FieldDescriptor;
import org.glassfish.ejb.deployment.descriptor.ScheduledTimerDescriptor;

/**
 * Utility class to calculate the list of methods required  to have transaction
 * attributes
 *
 * @author  Jerome Dochez
 */
final public class BeanMethodCalculatorImpl {

    // TODO - change logger if/when other EJB deployment classes are changed
    static final private Logger _logger = LogDomains.getLogger(BeanMethodCalculatorImpl.class, LogDomains.DPL_LOGGER);

    private static final String[] entityBeanHomeMethodsDisallowed = {
       "getEJBMetaData", "getHomeHandle"
    };
    private static final String[] entityBeanRemoteMethodsDisallowed = {
       "getEJBHome", "getHandle", "getPrimaryKey", "isIdentical"
    };
    private static final String[] entityBeanLocalHomeMethodsDisallowed = {};
    private static final String[] entityBeanLocalInterfaceMethodsDisallowed = {
       "getEJBLocalHome", "getPrimaryKey", "isIdentical"
    };

    private static final String[] sessionBeanMethodsDisallowed = {
       "*"
    };

    private static final String[] sessionLocalBeanMethodsDisallowed = {
       "*"
    };

    private static final Map<Class, String[]> disallowedMethodsPerInterface;

    static {
        Map<Class, String[]> methodsByClass = new HashMap<>();
        methodsByClass.put(javax.ejb.EJBHome.class, entityBeanHomeMethodsDisallowed);
        methodsByClass.put(javax.ejb.EJBObject.class, entityBeanRemoteMethodsDisallowed);
        methodsByClass.put(javax.ejb.EJBLocalHome.class, entityBeanLocalHomeMethodsDisallowed);
        methodsByClass.put(javax.ejb.EJBLocalObject.class, entityBeanLocalInterfaceMethodsDisallowed);
        disallowedMethodsPerInterface = Collections.unmodifiableMap(methodsByClass);
    }

    public List<FieldDescriptor> getPossibleCmpCmrFields(ClassLoader cl,
                                                 String className)
        throws ClassNotFoundException {

        List<FieldDescriptor> fieldDescriptors = new ArrayList<>();
        Class theClass = cl.loadClass(className);

        // Start with all *public* methods
        Method[] methods = theClass.getMethods();

        // Find all accessors that could be cmp fields. This list
        // will contain all cmr field accessors as well, since there
        // is no good way to distinguish between the two purely based
        // on method signature.
        for (Method next : methods) {
            String nextName = next.getName();
            int nextModifiers = next.getModifiers();
            if (Modifier.isAbstract(nextModifiers)) {
                if (nextName.startsWith("get") &&
                        nextName.length() > 3) {
                    String field =
                            nextName.substring(3, 4).toLowerCase(Locale.US) +
                                    nextName.substring(4);
                    fieldDescriptors.add(new FieldDescriptor(field));
                }
            }
        }
        return fieldDescriptors;
    }

    public List<Method> getMethodsFor(com.sun.enterprise.deployment.EjbDescriptor ejbDescriptor, ClassLoader classLoader)
            throws ClassNotFoundException
    {
        List<Method> methods = new ArrayList<>();

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

    private static void addAllInterfaceMethodsIn(Collection<Method> methods, Class c) {
        methods.addAll(Arrays.asList(c.getMethods()));
    }

    /**
     * @return a collection of MethodDescriptor for all the methods of my
     * ejb which are eligible to have a particular transaction setting.
     */
    public List<MethodDescriptor> getTransactionalMethodsFor(com.sun.enterprise.deployment.EjbDescriptor desc, ClassLoader loader)
        throws ClassNotFoundException
    {
        EjbDescriptor ejbDescriptor = (EjbDescriptor) desc;
        // only set if desc is a stateful session bean.  NOTE that
        // !statefulSessionBean does not imply stateless session bean
        boolean statefulSessionBean = false;

        List<MethodDescriptor> methods = new ArrayList<>();
        if (ejbDescriptor instanceof EjbSessionDescriptor) {
            statefulSessionBean =
                ((EjbSessionDescriptor) ejbDescriptor).isStateful();

            boolean singletonSessionBean =
                ((EjbSessionDescriptor) ejbDescriptor).isSingleton();

	    // Session Beans
            if (ejbDescriptor.isRemoteInterfacesSupported()) {
                Collection<Method> disallowedMethods = extractDisallowedMethodsFor(javax.ejb.EJBObject.class, sessionBeanMethodsDisallowed);
                Collection<Method> potentials = getTransactionMethodsFor(loader, ejbDescriptor.getRemoteClassName() , disallowedMethods);
                transformAndAdd(potentials, MethodDescriptor.EJB_REMOTE, methods);
            }

            if( ejbDescriptor.isRemoteBusinessInterfacesSupported() ) {
                for(String intfName : ejbDescriptor.getRemoteBusinessClassNames() ) {
                    readMethodsFromClass(intfName, methods, loader, MethodDescriptor.EJB_REMOTE);
                }
            }

            if (ejbDescriptor.isLocalInterfacesSupported()) {
                Collection<Method> disallowedMethods = extractDisallowedMethodsFor(javax.ejb.EJBLocalObject.class, sessionLocalBeanMethodsDisallowed);
                Collection<Method> potentials = getTransactionMethodsFor(loader, ejbDescriptor.getLocalClassName() , disallowedMethods);
                transformAndAdd(potentials, MethodDescriptor.EJB_LOCAL, methods);

            }

            if( ejbDescriptor.isLocalBusinessInterfacesSupported() ) {
                for(String intfName : ejbDescriptor.getLocalBusinessClassNames() ) {
                    readMethodsFromClass(intfName, methods, loader, MethodDescriptor.EJB_LOCAL);
                }
            }

            if( ejbDescriptor.isLocalBean() ) {
                String clazzName = ejbDescriptor.getEjbClassName();
                readMethodsFromClass(clazzName, methods, loader, MethodDescriptor.EJB_LOCAL);
            }

            if (ejbDescriptor.hasWebServiceEndpointInterface()) {
                String clazzName = ejbDescriptor.getWebServiceEndpointInterfaceName();
                readMethodsFromClass(clazzName, methods, loader, MethodDescriptor.EJB_WEB_SERVICE);
            }

            // SFSB and Singleton can have lifecycle callbacks transactional
            if (statefulSessionBean || singletonSessionBean) {
                Set<LifecycleCallbackDescriptor> lcds = ejbDescriptor.getLifecycleCallbackDescriptors();
                for(LifecycleCallbackDescriptor lcd : lcds) {
                    try {
                        Method m = lcd.getLifecycleCallbackMethodObject(loader);
                        MethodDescriptor md = new MethodDescriptor(m, MethodDescriptor.LIFECYCLE_CALLBACK);
                        methods.add(md);
                    } catch(Exception e) {
                        if (_logger.isLoggable(Level.FINE)) {
                            _logger.log(Level.FINE,
                            "Lifecycle callback processing error", e);
                        }
                    }
                }
            }

        } else {
            // entity beans local interfaces
            String homeIntf = ejbDescriptor.getHomeClassName();
            if (homeIntf!=null) {

                Class home = loader.loadClass(homeIntf);
                Collection<Method> potentials = getTransactionMethodsFor(javax.ejb.EJBHome.class, home);
                transformAndAdd(potentials, MethodDescriptor.EJB_HOME, methods);

                String remoteIntf = ejbDescriptor.getRemoteClassName();
                Class remote = loader.loadClass(remoteIntf);
                potentials = getTransactionMethodsFor(javax.ejb.EJBObject.class, remote);
                transformAndAdd(potentials, MethodDescriptor.EJB_REMOTE, methods);
            }

            // enity beans remote interfaces
            String localHomeIntf = ejbDescriptor.getLocalHomeClassName();
            if (localHomeIntf!=null) {
                Class home = loader.loadClass(localHomeIntf);
                Collection<Method> potentials = getTransactionMethodsFor(javax.ejb.EJBLocalHome.class, home);
                transformAndAdd(potentials, MethodDescriptor.EJB_LOCALHOME, methods);

                String remoteIntf = ejbDescriptor.getLocalClassName();
                Class remote = loader.loadClass(remoteIntf);
                potentials = getTransactionMethodsFor(javax.ejb.EJBLocalObject.class, remote);
                transformAndAdd(potentials, MethodDescriptor.EJB_LOCAL, methods);
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

    private void readMethodsFromClass(String className, List<MethodDescriptor> methods, ClassLoader loader, String ejbType) throws ClassNotFoundException {
        Class clazz = loader.loadClass(className);
        Method[] clazzMethods = clazz.getMethods();
        for (Method next : clazzMethods ) {
            methods.add(new MethodDescriptor(next, ejbType));
        }
    }

    private Collection<Method> getTransactionMethodsFor(ClassLoader loader, String interfaceName, Collection<Method> disallowedMethods)
        throws ClassNotFoundException {
         Class clazz = loader.loadClass(interfaceName);
         return getTransactionMethodsFor(clazz, disallowedMethods);
     }

     private Collection<Method> getTransactionMethodsFor(Class interfaceImpl, Collection<Method> disallowedMethods) {
         List<Method> v = new ArrayList<>(Arrays.asList(interfaceImpl.getMethods()));
         v.removeAll(disallowedMethods);
         return v;
     }

     private Collection<Method> getTransactionMethodsFor(Class interfaceType, Class interfaceImpl) {
         Collection<Method> disallowedTransactionMethods = getDisallowedTransactionMethodsFor(interfaceType);
         return getTransactionMethodsFor(interfaceImpl, disallowedTransactionMethods);
     }

     private Collection<Method> getDisallowedTransactionMethodsFor(Class interfaceType) {
         return extractDisallowedMethodsFor(interfaceType, getDisallowedMethodsNamesFor(interfaceType));
     }

     // from EJB 2.0 spec section 17.4.1
     private Collection<Method> extractDisallowedMethodsFor(Class interfaceType, String[] methodNames) {

         List<Method> v = new ArrayList<>();
         // no disallowed methods for this interface
         if (methodNames.length==0)
             return v;

         Method[] methods = interfaceType.getMethods();

         for (Method method : methods) {
             // all methods of the interface are disallowed
             if (methodNames[0].equals("*"))
                 v.add(method);
             else if (Arrays.binarySearch(methodNames, method.getName()) >= 0)
                 v.add(method);
         }
         return v;
     }

     /**
      * Utility method to transform our collection of Method objects into
      * MethodDescriptor objects and add them to our global list of
      * eligible methods
      * @param methods collection of acceptable method objects
      * @param methodIntf method-intf identifier for those methods
      * @param globalList global list of MethodDescriptors objects
      */
     private void transformAndAdd(Collection<Method> methods, String methodIntf, List<MethodDescriptor> globalList) {
         for (Object method : methods) {
             Method m = (Method) method;
             MethodDescriptor md = new MethodDescriptor(m, methodIntf);
             globalList.add(md);
         }
     }

     /**
      * @return the list of disallowed methods for a particular interface
      */
     private String[] getDisallowedMethodsNamesFor(Class interfaceType) {
         return getDisallowedMethodsNames().get(interfaceType);
     }

     /**
      * @return a Map of disallowed methods per interface type. The key to the
      * map is the interface type (e.g. EJBHome, EJBObject), the value
      * is an array of methods names disallowed to have transaction attributes
      */
     private Map<Class, String[]> getDisallowedMethodsNames() {
         return disallowedMethodsPerInterface;
     }

}

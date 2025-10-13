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

package org.glassfish.ejb.deployment;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.enterprise.deployment.LifecycleCallbackDescriptor;
import com.sun.enterprise.deployment.MethodDescriptor;
import com.sun.logging.LogDomains;
import java.util.Locale;
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
public final class BeanMethodCalculatorImpl {

    private BeanMethodCalculatorImpl() {
        throw new UnsupportedOperationException("util");
    }

    // TODO - change logger if/when other EJB deployment classes are changed
    static final private Logger _logger = LogDomains.getLogger(BeanMethodCalculatorImpl.class, LogDomains.DPL_LOGGER);

    public static Vector<FieldDescriptor> getPossibleCmpCmrFields(ClassLoader cl, String className)
            throws ClassNotFoundException {

        Vector<FieldDescriptor> fieldDescriptors = new Vector<>();
        Class<?> theClass = cl.loadClass(className);

        // Start with all *public* methods
        Method[] methods = theClass.getMethods();

        // Find all accessors that could be cmp fields. This list 
        // will contain all cmr field accessors as well, since there
        // is no good way to distinguish between the two purely based
        // on method signature.
        for(int mIndex = 0; mIndex < methods.length; mIndex++) {
            Method next = methods[mIndex];
            String nextName = next.getName();
            int nextModifiers = next.getModifiers();
            if( Modifier.isAbstract(nextModifiers) ) {
                if( nextName.startsWith("get") &&
                        nextName.length() > 3 ) {
                    String field = 
                            nextName.substring(3,4).toLowerCase(Locale.US) +
                            nextName.substring(4);
                    fieldDescriptors.add(new FieldDescriptor(field));
                }
            }
        }
        return fieldDescriptors;
    }

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
     * @return a collection of MethodDescriptor for all the methods of my 
     * ejb which are elligible to have a particular transaction setting.
     */
    public static Collection<MethodDescriptor> getTransactionalMethodsFor(com.sun.enterprise.deployment.EjbDescriptor desc, ClassLoader loader)
            throws ClassNotFoundException
    {
        EjbDescriptor ejbDescriptor = (EjbDescriptor) desc;
        // only set if desc is a stateful session bean.  NOTE that 
        // !statefulSessionBean does not imply stateless session bean
        boolean statefulSessionBean = false;

        Vector<MethodDescriptor> methods = new Vector<>();
        if (ejbDescriptor instanceof EjbSessionDescriptor) {
            statefulSessionBean = 
                    ((EjbSessionDescriptor) ejbDescriptor).isStateful();

            boolean singletonSessionBean = 
                    ((EjbSessionDescriptor) ejbDescriptor).isSingleton();

            // Session Beans
            if (ejbDescriptor.isRemoteInterfacesSupported()) {                
                Collection<Method> disallowedMethods = extractDisallowedMethodsFor(jakarta.ejb.EJBObject.class, sessionBeanMethodsDisallowed);
                Collection<Method> potentials = getTransactionMethodsFor(loader, ejbDescriptor.getRemoteClassName() , disallowedMethods);
                transformAndAdd(potentials, MethodDescriptor.EJB_REMOTE, methods);
            }

            if( ejbDescriptor.isRemoteBusinessInterfacesSupported() ) {

                for(String intfName : 
                    ejbDescriptor.getRemoteBusinessClassNames() ) {

                    Class<?> businessIntf = loader.loadClass(intfName);
                    Method[] busIntfMethods = businessIntf.getMethods();
                    for (Method next : busIntfMethods ) {
                        methods.add(new MethodDescriptor
                                (next, MethodDescriptor.EJB_REMOTE));
                    }
                }
            }

            if (ejbDescriptor.isLocalInterfacesSupported()) {
                Collection<Method> disallowedMethods = extractDisallowedMethodsFor(jakarta.ejb.EJBLocalObject.class, sessionLocalBeanMethodsDisallowed);
                Collection<Method> potentials = getTransactionMethodsFor(loader, ejbDescriptor.getLocalClassName() , disallowedMethods);
                transformAndAdd(potentials, MethodDescriptor.EJB_LOCAL, methods);

            }

            if( ejbDescriptor.isLocalBusinessInterfacesSupported() ) {

                for(String intfName :
                    ejbDescriptor.getLocalBusinessClassNames() ) {

                    Class<?> businessIntf = loader.loadClass(intfName);
                    Method[] busIntfMethods = businessIntf.getMethods();
                    for (Method next : busIntfMethods ) {
                        methods.add(new MethodDescriptor
                                (next, MethodDescriptor.EJB_LOCAL));
                    }
                }
            }

            if( ejbDescriptor.isLocalBean() ) {
                String intfName = ejbDescriptor.getEjbClassName();
                Class<?> businessIntf = loader.loadClass(intfName);
                Method[] busIntfMethods = businessIntf.getMethods();
                for (Method next : busIntfMethods ) {
                    methods.add(new MethodDescriptor
                            (next, MethodDescriptor.EJB_LOCAL));
                }
            }

            if (ejbDescriptor.hasWebServiceEndpointInterface()) {
                Class<?> webServiceClass = loader.loadClass
                        (ejbDescriptor.getWebServiceEndpointInterfaceName());

                Method[] webMethods = webServiceClass.getMethods();                
                for (int i=0;i<webMethods.length;i++) {
                    methods.add(new MethodDescriptor(webMethods[i],  
                            MethodDescriptor.EJB_WEB_SERVICE));

                }
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

                Class<?> home = loader.loadClass(homeIntf);
                Collection<Method> potentials = getTransactionMethodsFor(jakarta.ejb.EJBHome.class, home);
                transformAndAdd(potentials, MethodDescriptor.EJB_HOME, methods);

                String remoteIntf = ejbDescriptor.getRemoteClassName();                
                Class<?> remote = loader.loadClass(remoteIntf);
                potentials = getTransactionMethodsFor(jakarta.ejb.EJBObject.class, remote);
                transformAndAdd(potentials, MethodDescriptor.EJB_REMOTE, methods);
            } 

            // enity beans remote interfaces
            String localHomeIntf = ejbDescriptor.getLocalHomeClassName();
            if (localHomeIntf!=null) { 
                Class<?> home = loader.loadClass(localHomeIntf);
                Collection<Method> potentials = getTransactionMethodsFor(jakarta.ejb.EJBLocalHome.class, home);
                transformAndAdd(potentials, MethodDescriptor.EJB_LOCALHOME, methods);

                String remoteIntf = ejbDescriptor.getLocalClassName();                
                Class<?> remote = loader.loadClass(remoteIntf);
                potentials = getTransactionMethodsFor(jakarta.ejb.EJBLocalObject.class, remote);
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

    private static Collection<Method> getTransactionMethodsFor(ClassLoader loader, String interfaceName, Collection<Method> disallowedMethods)
            throws ClassNotFoundException
    {
        Class<?> clazz = loader.loadClass(interfaceName);
        return getTransactionMethodsFor(clazz, disallowedMethods);
    }

    private static Collection<Method> getTransactionMethodsFor(Class<?> interfaceImpl, Collection<Method> disallowedMethods) {
        Vector<Method> v = new Vector<>(Arrays.asList(interfaceImpl.getMethods()));
        v.removeAll(disallowedMethods);
        return v;
    }

    private static Collection<Method> getTransactionMethodsFor(Class<?> interfaceType, Class<?> interfaceImpl) {
        Collection<Method> disallowedTransactionMethods = getDisallowedTransactionMethodsFor(interfaceType);
        return getTransactionMethodsFor(interfaceImpl, disallowedTransactionMethods);
    }

    private static Collection<Method> getDisallowedTransactionMethodsFor(Class<?> interfaceType) {
        return extractDisallowedMethodsFor(interfaceType, getDisallowedMethodsNamesFor(interfaceType));
    }

    // from EJB 2.0 spec section 17.4.1
    private static Collection<Method> extractDisallowedMethodsFor(Class<?> interfaceType, String[] methodNames) {

        Vector<Method> v = new Vector<>();
        // no disallowed methods for this interface 
        if (methodNames.length==0)
            return v;

        Method methods[] = interfaceType.getMethods();

        for (int i=0; i<methods.length; i++) {
            // all methods of the interface are disallowed
            if (methodNames[0].equals("*"))
                v.addElement(methods[i]);             
            else if (Arrays.binarySearch(methodNames, methods[i].getName())>=0) 
                v.addElement(methods[i]);
        }
        return v;
    }

    /**
     * utiliy method to transform our collection of Method objects into 
     * MethodDescriptor objects and add them to our global list of 
     * elligible methods
     * @param the collection of acceptable method objects
     * @param the method-intf identifier for those methods
     * @param the global list of MethodDescriptors objects 
     */
    private static void transformAndAdd(Collection<Method> methods, String methodIntf, Vector<MethodDescriptor> globalList) {
        for (Method m : methods) {
            globalList.add(new MethodDescriptor(m, methodIntf));
        }
    }

    /**
     * @return the list of disallowed methods for a particular interface
     */
    private static String[] getDisallowedMethodsNamesFor(Class<?> interfaceType) {
        return getDisallowedMethodsNames().get(interfaceType);
    }

    /**
     * @return a Map of disallowed methods per interface type. The key to the 
     * map is the interface type (e.g. EJBHome, EJBObject), the value
     * is an array of methods names disallowed to have transaction attributes
     */
    private static Map<Class<?>, String[]> getDisallowedMethodsNames() {
        if (disallowedMethodsPerInterface==null) {
            disallowedMethodsPerInterface = new Hashtable<>();
            disallowedMethodsPerInterface.put(jakarta.ejb.EJBHome.class, new String[] {
                    "getEJBMetaData", "getHomeHandle" 
            });
            disallowedMethodsPerInterface.put(jakarta.ejb.EJBObject.class, new String[] {
                    "getEJBHome", "getHandle", "getPrimaryKey", "isIdentical"
            });
            disallowedMethodsPerInterface.put(jakarta.ejb.EJBLocalHome.class, new String[0]);
            disallowedMethodsPerInterface.put(jakarta.ejb.EJBLocalObject.class, new String[] {
                    "getEJBLocalHome", "getPrimaryKey", "isIdentical"
            });
        }
        return disallowedMethodsPerInterface;
    }

    private static final String[] sessionBeanMethodsDisallowed = { "*" };

    private static final String[] sessionLocalBeanMethodsDisallowed = { "*" };

    private static Map<Class<?>, String[]> disallowedMethodsPerInterface;

}

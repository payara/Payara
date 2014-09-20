/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2014 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.container.common.impl.util;

import com.sun.enterprise.container.common.spi.util.ComponentEnvManager;
import com.sun.enterprise.container.common.spi.util.InjectionException;
import com.sun.enterprise.container.common.spi.util.InjectionManager;
import com.sun.enterprise.container.common.spi.ManagedBeanManager;
import com.sun.enterprise.container.common.spi.JCDIService;
import com.sun.enterprise.deployment.InjectionCapable;
import com.sun.enterprise.deployment.InjectionInfo;
import com.sun.enterprise.deployment.InjectionTarget;
import com.sun.enterprise.deployment.JndiNameEnvironment;
import com.sun.enterprise.util.LocalStringManagerImpl;

import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.api.naming.GlassfishNamingManager;
import javax.inject.Inject;
import org.jvnet.hk2.annotations.Service;

import javax.naming.NamingException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.ManagedBean;
import java.lang.reflect.Constructor;
import org.glassfish.api.admin.ProcessEnvironment;

import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.api.ServiceLocator;

/**
 * Implementation of InjectionManager.
 *
 * @author Kenneth Saks
 */
@Service
public class InjectionManagerImpl implements InjectionManager, PostConstruct {

    @Inject
    private Logger _logger;

    static private LocalStringManagerImpl localStrings =
        new LocalStringManagerImpl(InjectionManagerImpl.class);

    @Inject
    private ComponentEnvManager compEnvManager;

    @Inject
    private InvocationManager invocationMgr;

    @Inject
    private GlassfishNamingManager glassfishNamingManager;

    @Inject
    private ServiceLocator habitat;

    @Inject
    private ProcessEnvironment processEnv;

    public void postConstruct() {

        // When in the server, register in JNDI to allow container code without
        // compile-time dependency on GlassFish to use injection services.
        // We know GlassFishNaming manager is available because it's an injected field.

        if( processEnv.getProcessType().isServer() ) {
            try {
                glassfishNamingManager.publishObject("com.sun.enterprise.container.common.spi.util.InjectionManager",
                    this, true);
            } catch(NamingException ne) {
                throw new RuntimeException(ne);
            }
        }

    }


    public void injectInstance(Object instance)
        throws InjectionException {

        injectInstance(instance, true);

    }

    public void injectInstance(Object instance, boolean invokePostConstruct)
        throws InjectionException {

        ComponentInvocation inv = invocationMgr.getCurrentInvocation();

        if( inv != null ) {

            JndiNameEnvironment componentEnv = compEnvManager.getJndiNameEnvironment(inv.getComponentId());

            if( componentEnv != null ) {
                inject(instance.getClass(), instance, componentEnv, null, invokePostConstruct);
            } else {
                throw new InjectionException(localStrings.getLocalString(
                        "injection-manager.no-descriptor-registered-for-invocation",
                        "No descriptor registered for current invocation: {0}", inv));
            }

        } else {
            throw new InjectionException(localStrings.getLocalString(
                    "injection-manager.null-invocation-context", "Null invocation context"));
        }

    }

    public void injectInstance(Object instance, 
                               JndiNameEnvironment componentEnv) 
        throws InjectionException 
    {

        inject(instance.getClass(), instance, componentEnv, null, true);

    }

    public void injectInstance(Object instance,
                               JndiNameEnvironment componentEnv,
                               boolean invokePostConstruct)
      throws InjectionException
    {

        inject(instance.getClass(), instance, componentEnv, null,
               invokePostConstruct);

    }

    public void injectInstance(Object instance,
                               String componentId,
                               boolean invokePostConstruct)
        throws InjectionException {

        ComponentInvocation inv = invocationMgr.getCurrentInvocation();

        if( inv != null ) {

            JndiNameEnvironment componentEnv =
                compEnvManager.getJndiNameEnvironment(componentId);

            if( componentEnv != null ) {
                inject(instance.getClass(), instance, componentEnv, componentId, invokePostConstruct);
            } else {
                throw new InjectionException(localStrings.getLocalString(
                        "injection-manager.no-descriptor-registered-for-component", 
                        "No descriptor registered for componentId: {0}", componentId));
            }
        } else {
            throw new InjectionException(localStrings.getLocalString(
                    "injection-manager.null-invocation-context", "Null invocation context"));
        }
    }

    public void injectClass(Class clazz,
                            String componentId,
                            boolean invokePostConstruct ) throws InjectionException {

      ComponentInvocation inv = invocationMgr.getCurrentInvocation();

      if( inv != null ) {
        JndiNameEnvironment componentEnv =
          compEnvManager.getJndiNameEnvironment(componentId);

        if( componentEnv != null ) {
          injectClass(clazz, componentEnv, invokePostConstruct);
        } else {
          throw new InjectionException(localStrings.getLocalString(
            "injection-manager.no-descriptor-registered-for-component",
            "No descriptor registered for componentId: {0}", componentId));
        }
      } else {
        throw new InjectionException(localStrings.getLocalString(
          "injection-manager.null-invocation-context", "Null invocation context"));
      }
    }

    public void injectClass(Class clazz,
                            JndiNameEnvironment componentEnv) 
        throws InjectionException
    {
        injectClass(clazz, componentEnv, true);
    }

    public void injectClass(Class clazz, 
                            JndiNameEnvironment componentEnv,
                            boolean invokePostConstruct) 
        throws InjectionException
    {
        inject(clazz, null, componentEnv, null, invokePostConstruct);
    }

    public void invokeInstancePreDestroy(Object instance,
                                         JndiNameEnvironment componentEnv)
        throws InjectionException
    {
        invokePreDestroy(instance.getClass(), instance, componentEnv);
    }

    public void invokeInstancePostConstruct(Object instance,
                                            JndiNameEnvironment componentEnv)
        throws InjectionException
    {
        invokePostConstruct(instance.getClass(), instance, componentEnv);
    }
    
    public void invokeInstancePreDestroy(Object instance)
            throws InjectionException {
        invokeInstancePreDestroy(instance, true);
    }

    public void invokeInstancePreDestroy(Object instance, boolean validate)
        throws InjectionException {
        ComponentInvocation inv = invocationMgr.getCurrentInvocation();
        
        //if ComponentInv is null and validate is true, throw InjectionException;
        //if component JndiNameEnvironment is null and validate is true, throw InjectionException;
        //if validate is false, the above 2 null conditions are basically ignored,
        //except that when fine logging is enabled, fine-log a message.
        if( inv != null ) {
            JndiNameEnvironment componentEnv = compEnvManager.getJndiNameEnvironment(inv.getComponentId());

            if (componentEnv != null) {
                invokePreDestroy(instance.getClass(), instance, componentEnv);
            } else if (validate || _logger.isLoggable(Level.FINE)) {
                String msg1 = localStrings.getLocalString(
                        "injection-manager.no-descriptor-registered-for-invocation",
                        "No descriptor registered for current invocation: {0}", inv);
                if (validate) {
                    throw new InjectionException(msg1);
                }
                _logger.log(Level.FINE, msg1);
            }
        } else if (validate || _logger.isLoggable(Level.FINE)) {
            String msg2 = localStrings.getLocalString(
                    "injection-manager.null-invocation-context", "Null invocation context");
            if (validate) {
                throw new InjectionException(msg2);
            }
            _logger.log(Level.FINE, msg2);
        }
    }

    public void invokeClassPreDestroy(Class clazz,
                                      JndiNameEnvironment componentEnv)
        throws InjectionException
    {
        invokePreDestroy(clazz, null, componentEnv);
    }


    /**
     * Create a managed object for the given class.  The object will be
     * injected and any PostConstruct methods will be called.  The returned
     * object can be cast to the clazz type but is not necessarily a direct
     * reference to the managed instance.  All invocations on the returned
     * object should be on its public methods.
     *
     * It is the responsibility of the caller to destroy the returned object
     * by calling destroyManagedObject(Object managedObject).
     *
     * @param clazz  Class to be instantiated
     * @return managed object
     * @throws InjectionException
     */
    public <T> T createManagedObject(Class<T> clazz)
        throws InjectionException {

        T managedObject = null;

        try {

            ManagedBean managedBeanAnn = clazz.getAnnotation(ManagedBean.class);

            ManagedBeanManager managedBeanMgr = habitat.getService(ManagedBeanManager.class);

            if( managedBeanAnn != null ) {

                // EE style @ManagedBean

                // Create , inject, and call PostConstruct via managed bean manager
                managedObject = managedBeanMgr.createManagedBean(clazz);

            } else {

                JCDIService jcdiService = habitat.getService(JCDIService.class);

                if( (jcdiService != null) && jcdiService.isCurrentModuleJCDIEnabled() ) {

                    // Create , inject, and call PostConstruct via managed bean manager
                    managedObject = managedBeanMgr.createManagedBean(clazz);
                        
                } else {

                    // Not in a 299-enabled module and not annoated with @ManagedBean, so
                    // just instantiate using new and perform injection
                    Constructor<T> noArgCtor = clazz.getConstructor();

                    managedObject = noArgCtor.newInstance();

                    // Inject and call PostConstruct
                    injectInstance(managedObject);

                }
            }
        } catch(Exception e) {
            throw new InjectionException(localStrings.getLocalString(
                    "injection-manager.error-creating-managed-object", 
                    "Error creating managed object for class: {0}", clazz), e);
        }

        return managedObject;
    }

     /**
     * Create a managed object for the given class.  The object will be
     * injected and if invokePostConstruct is true, any @PostConstruct
     * methods on the instance's class(and super-classes) will be invoked
     * after injection.  The returned
     * object can be cast to the clazz type but is not necessarily a direct
     * reference to the managed instance.  All invocations on the returned
     * object should be on its public methods.
     *
     * It is the responsibility of the caller to destroy the returned object
     * by calling destroyManagedObject(Object managedObject).
     *
     * @param clazz  Class to be instantiated
     * @param invokePostConstruct if true, invoke any @PostConstruct methods
     * on the instance's class(and super-classes) after injection.
     * @return managed object
     * @throws InjectionException
     */
    public <T> T createManagedObject(Class<T> clazz, boolean invokePostConstruct)
        throws InjectionException {

        T managedObject = null;

        try {

            ManagedBean managedBeanAnn = clazz.getAnnotation(ManagedBean.class);

            ManagedBeanManager managedBeanMgr = habitat.getService(ManagedBeanManager.class);

            if( managedBeanAnn != null ) {

                // EE style @ManagedBean

                // Create , inject, and call PostConstruct (if necessary) via managed bean manager
                managedObject = managedBeanMgr.createManagedBean(clazz, invokePostConstruct);

            } else {

                JCDIService jcdiService = habitat.getService(JCDIService.class);

                if( (jcdiService != null) && jcdiService.isCurrentModuleJCDIEnabled() ) {

                    // Create , inject, and call PostConstruct (if necessary)  via managed bean manager
                    managedObject = managedBeanMgr.createManagedBean(clazz, invokePostConstruct);

                } else {
                    // Not in a 299-enabled module and not annoated with @ManagedBean, so
                    // just instantiate using new and perform injection
                    Constructor<T> noArgCtor = clazz.getConstructor();

                    managedObject = noArgCtor.newInstance();

                    // Inject and call PostConstruct if necessary
                    injectInstance(managedObject, invokePostConstruct);

                }
            }

        } catch(Exception e) {
            throw new InjectionException(localStrings.getLocalString(
                    "injection-manager.error-creating-managed-object",
                    "Error creating managed object for class: {0}", clazz), e);
        }

        return managedObject;
    }

    /**
     * Destroy a managed object that was created via createManagedObject.  Any
     * PreDestroy methods will be called.
     *
     * @param managedObject
     * @throws InjectionException
     */
    public void destroyManagedObject(Object managedObject)
        throws InjectionException {

        destroyManagedObject(managedObject, true);
    }

    /**
     * Destroy a managed object that was created via createManagedObject.  Any
     * PreDestroy methods will be called.
     *
     * @param managedObject
     * @param validate if false, do nothing if the instance is not registered
     * @throws InjectionException
     */
    public void destroyManagedObject(Object managedObject, boolean validate)
        throws InjectionException {
                      
        Class managedObjectClass = managedObject.getClass();

        ManagedBean managedBeanAnn = (ManagedBean) managedObjectClass.getAnnotation(ManagedBean.class);

        ManagedBeanManager managedBeanMgr = habitat.getService(ManagedBeanManager.class);

        JCDIService jcdiService = habitat.getService(JCDIService.class);

        if( (jcdiService != null) && jcdiService.isCurrentModuleJCDIEnabled() ) {

            // If 299-enabled always delegate to managed bean manager
            managedBeanMgr.destroyManagedBean(managedObject, validate);

        } else {

            // If the object's class has @ManagedBean it's a managed bean.  Otherwise, ask
            // managed bean manager.
            boolean isManagedBean = (managedBeanAnn != null) ||
                    managedBeanMgr.isManagedBean(managedObject);

            if( isManagedBean ) {

                managedBeanMgr.destroyManagedBean(managedObject, validate);

            } else {

                this.invokeInstancePreDestroy(managedObject, validate);
            }
        }

    }

    /**
     * @param instance Target instance for injection, or null if injection
     *                 is class-based.  Any error encountered during any
     * portion of injection is propagated immediately.
     */
    public void inject(final Class clazz, final Object instance,
                        JndiNameEnvironment envDescriptor,
                        String componentId,
                        boolean invokePostConstruct) 
        throws InjectionException 
    {
        
        LinkedList<Method> postConstructMethods = new LinkedList<Method>();
            
        Class nextClass = clazz;

        // Process each class in the inheritance hierarchy, starting with
        // the most derived class and ignoring java.lang.Object.
        while((nextClass != Object.class) && (nextClass != null)) {

            InjectionInfo injInfo = 
                envDescriptor.getInjectionInfoByClass(nextClass);

            if( injInfo.getInjectionResources().size() > 0 ) {
                _inject(nextClass, instance, componentId, injInfo.getInjectionResources());
            }

            if( invokePostConstruct ) {
                
                if( injInfo.getPostConstructMethodName() != null ) {
                    
                    Method postConstructMethod = getPostConstructMethod
                        (injInfo, nextClass);
                    
                    // Delay calling post construct methods until all
                    // dependency injection within the hierarchy has been
                    // completed.  Then, invoke the methods starting from
                    // the least-derived class downward.  
                    postConstructMethods.addFirst(postConstructMethod);
                }
            }

            nextClass = nextClass.getSuperclass();
        }


        for(Method postConstructMethod : postConstructMethods) {

            invokeLifecycleMethod(postConstructMethod, instance);

        }

    }

    /**
     * @param instance Target instance for preDestroy, or null if 
     *                 class-based. 
     */
    private void invokePreDestroy(final Class clazz, final Object instance, 
                                  JndiNameEnvironment envDescriptor)
        throws InjectionException 
    {
        
        LinkedList<Method> preDestroyMethods = new LinkedList<Method>();
            
        Class nextClass = clazz;

        // Process each class in the inheritance hierarchy, starting with
        // the most derived class and ignoring java.lang.Object.
        while((nextClass != Object.class) && (nextClass != null)) {

            InjectionInfo injInfo = 
                envDescriptor.getInjectionInfoByClass(nextClass);

            if( injInfo.getPreDestroyMethodName() != null ) {
                
                Method preDestroyMethod = getPreDestroyMethod
                    (injInfo, nextClass);
                
                // Invoke the preDestroy methods starting from
                // the least-derived class downward.  
                preDestroyMethods.addFirst(preDestroyMethod);
            }

            nextClass = nextClass.getSuperclass();
        }

        for(Method preDestroyMethod : preDestroyMethods) {

            invokeLifecycleMethod(preDestroyMethod, instance);

        }

    }

    /**
     * @param instance Target instance for postConstruct, or null if 
     * class-based. 
     */
    private void invokePostConstruct(final Class clazz,
                                     final Object instance, 
                                     JndiNameEnvironment envDescriptor)
        throws InjectionException 
    {     
        LinkedList<Method> postConstructMethods = new LinkedList<Method>();
            
        Class nextClass = clazz;

        // Process each class in the inheritance hierarchy, starting with
        // the most derived class and ignoring java.lang.Object.
        while ((nextClass != Object.class) && (nextClass != null)) {

            InjectionInfo injInfo = 
                envDescriptor.getInjectionInfoByClass(nextClass);

            if (injInfo.getPostConstructMethodName() != null) {
                
                Method postConstructMethod = getPostConstructMethod
                    (injInfo, nextClass);
                
                // Invoke the postConstruct methods starting from
                // the least-derived class downward.  
                postConstructMethods.addFirst(postConstructMethod);
            }

            nextClass = nextClass.getSuperclass();
        }

        for (Method postConstructMethod : postConstructMethods) {
            invokeLifecycleMethod(postConstructMethod, instance);
        }
    }

    /**
     *
     * Internal injection operation.  componentId is only specified if
     * componentId-specific lookup operation should be used. 
     */
    private void _inject(final Class clazz, final Object instance,
                         String componentId,
                        List<InjectionCapable> injectableResources) 
        throws InjectionException 
    {

	for (InjectionCapable next : injectableResources ) {

            try {

                String lookupName = next.getComponentEnvName();
                if( !lookupName.startsWith("java:") ) {
                    lookupName = "java:comp/env/" + lookupName;
                }
                
                final Object value =  (componentId != null) ?
                        glassfishNamingManager.lookup(componentId, lookupName) :
                        glassfishNamingManager.getInitialContext().lookup(lookupName);

                // there still could be 2 injection on the same class, better
                // do a loop here
                for (InjectionTarget target : next.getInjectionTargets()) {
                    
                    // if target class is not the class we are injecting
                    // we can just jump to the next target
                    if (!clazz.getName().equals(target.getClassName()))
                        continue;
                    
                    if( target.isFieldInjectable() ) {
                        
                        final Field f = getField(target, clazz);
                        
                        if( Modifier.isStatic(f.getModifiers()) &&
                            (instance != null) ) {
                            throw new InjectionException(localStrings.getLocalString(
                                "injection-manager.illegal-use-of-static-field",
                                "Illegal use of static field on class that only supports instance-based injection: {0}",
                                f));
                        }

                        if( (instance == null) &&
                            !Modifier.isStatic(f.getModifiers()) ) {
                            throw new InjectionException(localStrings.getLocalString(
                                "injection-manager.appclient-injected-field-must-be-static",
                                "Injected field: {0} on Application Client class: {1} must be declared static",
                                f, clazz));
                        }


                        if(_logger.isLoggable(Level.FINE)) {
                            _logger.fine(localStrings.getLocalString(
                                "injection-manager.injecting-dependency-field",
                                "Injecting dependency with logical name: {0} into field: {1} on class: {2}", 
                                next.getComponentEnvName(), f, clazz));
                        }
                        
                        // Wrap actual value insertion in doPrivileged to
                        // allow for private/protected field access.
                        if( System.getSecurityManager() != null ) {
                            java.security.AccessController.doPrivileged(
                             new java.security.PrivilegedExceptionAction() {
                               public java.lang.Object run() throws Exception {
                                 f.set(instance, value);
                                 return null;
                               }
                             });
                        } else {
                            f.set(instance, value);
                        }
                    } else if( target.isMethodInjectable() ) {
                        
                        final Method m = getMethod(next, target, clazz);

                        if( Modifier.isStatic(m.getModifiers()) &&
                            (instance != null) ) {
                            throw new InjectionException(localStrings.getLocalString(
                                "injection-manager.illegal-use-of-static-method", 
                                "Illegal use of static method on class that only supports instance-based injection: {0}", 
                                m));
                        }

                        if( (instance == null) &&
                            !Modifier.isStatic(m.getModifiers()) ) {
                            throw new InjectionException(localStrings.getLocalString(
                                "injection-manager.appclient-injected-method-must-be-static", 
                                "Injected method: {0} on Application Client class: {1} must be declared static", 
                                m, clazz));
                        }
                        
                        if(_logger.isLoggable(Level.FINE)) {
                            _logger.fine(localStrings.getLocalString(
                                "injection-manager.injecting-dependency-method", 
                                "Injecting dependency with logical name: {0} into method: {1} on class: {2}", 
                                next.getComponentEnvName(), m, clazz));
                        }

                        if( System.getSecurityManager() != null ) {
                          // Wrap actual value insertion in doPrivileged to
                          // allow for private/protected field access.
                          java.security.AccessController.doPrivileged(
                                new java.security.PrivilegedExceptionAction() {
                            public java.lang.Object run() throws Exception {
                                m.invoke(instance, new Object[] { value });
                                return null;
                            }});
                        } else {
                            m.invoke(instance, new Object[] { value });
                        }
                        
                    }
                }
            } catch(Throwable t) {
                Throwable cause = (t instanceof InvocationTargetException) ?
                    ((InvocationTargetException)t).getCause() : t;
                String msg = localStrings.getLocalString(
                        "injection-manager.exception-to-inject",
                        "Exception attempting to inject {0} into {1}: {2}",
                        next, clazz, cause.getMessage());
                _logger.log(Level.FINE, msg, t);
                throw new InjectionException(msg, cause);
            }
        }
    }

    private void invokeLifecycleMethod(final Method lifecycleMethod,
                                       final Object instance) 
        throws InjectionException {

        try {

            if(_logger.isLoggable(Level.FINE)) {
                _logger.fine(localStrings.getLocalString(
                    "injection-manager.calling-lifecycle-method",
                    "Calling lifecycle method: {0} on class: {1}",
                    lifecycleMethod, lifecycleMethod.getDeclaringClass()));
            }

            // Wrap actual value insertion in doPrivileged to
            // allow for private/protected field access.
            java.security.AccessController.doPrivileged(
                new java.security.PrivilegedExceptionAction() {
                    public java.lang.Object run() throws Exception {
                        if( !lifecycleMethod.isAccessible() ) {
                            lifecycleMethod.setAccessible(true);
                        }
                        lifecycleMethod.invoke(instance);
                        return null;
                    }
                });
        } catch( Throwable t) {
            String msg = localStrings.getLocalString(
                    "injection-manager.exception-invoke-lifecycle-method",
                    "Exception attempting invoke lifecycle method: {0}", lifecycleMethod);
            _logger.log(Level.FINE, msg, t);
            InjectionException ie = new InjectionException(msg);
            Throwable cause = (t instanceof InvocationTargetException) ?
                ((InvocationTargetException)t).getCause() : t;
            ie.initCause( cause );
            throw ie;
        }
        
        return;

    }

    private Field getField(InjectionTarget target,
                           Class resourceClass) throws Exception {

        Field f = target.getField();

        if( f == null ) {
            try {
                // Check for the given field within the resourceClass only.
                // This does not include super-classes of this class.
                f = resourceClass.getDeclaredField
                        (target.getFieldName());

                final Field finalF = f;
                java.security.AccessController.doPrivileged(
                     new java.security.PrivilegedExceptionAction() {
                            public java.lang.Object run() throws Exception {
                                if( !finalF.isAccessible() ) {
                                    finalF.setAccessible(true);
                                }
                                return null;
                            }
                        });

            } catch(java.lang.NoSuchFieldException nsfe) {}

            if( f != null ) {
                target.setField(f);
            }
        }

        if( f == null ) {
            throw new Exception(localStrings.getLocalString(
                    "injection-manager.field-not-found",
                    "InjectionManager exception.  Field: {0} not found in class: {1}",
                    target.getFieldName(), resourceClass));
        }

        return f;
    }                                               

    private Method getMethod(InjectionCapable resource, InjectionTarget target,
                             Class resourceClass) throws Exception {

        Method m = target.getMethod();

        if( m == null ) {
            // Check for the method within the resourceClass only.
            // This does not include super-classses.
            for(Method next : resourceClass.getDeclaredMethods()) {
                // Overloading is not supported for setter injection 
                // methods, so matching on method-name is sufficient.  
                if(next.getName().equals(target.getMethodName())) {
                    m = next;
                    target.setMethod(m);
                    
                    final Method finalM = m;
                    java.security.AccessController.doPrivileged(
                       new java.security.PrivilegedExceptionAction() {
                          public java.lang.Object run() throws Exception {
                             if( !finalM.isAccessible() ) {
                                 finalM.setAccessible(true);
                             }
                             return null;
                         }});

                    break;
                }
            }
        }

        if( m == null ) {
            throw new Exception(localStrings.getLocalString(
                "injection-manager.method-not-found", 
                "InjectionManager exception.  Method: void {0} ({1}) not found in class: {2}",
                target.getMethodName(), resource.getInjectResourceType(), resourceClass));
        }

        return m;
    }

    private Method getPostConstructMethod(InjectionInfo injInfo,
                                          Class resourceClass)
        throws InjectionException {

        Method m = injInfo.getPostConstructMethod();

        if( m == null ) {
            String postConstructMethodName = 
                injInfo.getPostConstructMethodName();

            // Check for the method within the resourceClass only.
            // This does not include super-classes.
            for(Method next : resourceClass.getDeclaredMethods()) {
                // InjectionManager only handles injection into PostConstruct
                // methods with no arguments. 
                if( next.getName().equals(postConstructMethodName) &&
                    (next.getParameterTypes().length == 0) ) {
                    m = next;
                    injInfo.setPostConstructMethod(m);
                    break;
                }
            }
        }

        if( m == null ) {
            throw new InjectionException(localStrings.getLocalString(
                "injection-manager.postconstruct-not-found",
                "InjectionManager exception. PostConstruct method: {0} not found in class: {1}",
                injInfo.getPostConstructMethodName(), injInfo.getClassName()));
        }

        return m;
    }

    private Method getPreDestroyMethod(InjectionInfo injInfo,
                                       Class resourceClass)
        throws InjectionException {

        Method m = injInfo.getPreDestroyMethod();

        if( m == null ) {
            String preDestroyMethodName = injInfo.getPreDestroyMethodName();

            // Check for the method within the resourceClass only.
            // This does not include super-classses.
            for(Method next : resourceClass.getDeclaredMethods()) {
                // InjectionManager only handles injection into PreDestroy
                // methods with no arguments. 
                if( next.getName().equals(preDestroyMethodName) &&
                    (next.getParameterTypes().length == 0) ) {
                    m = next;
                    injInfo.setPreDestroyMethod(m);
                    break;
                }
            }
        }

        if( m == null ) {
            throw new InjectionException(localStrings.getLocalString(
                "injection-manager.predestroy-not-found",
                "InjectionManager exception. PreDestroy method: {0} not found in class: {1}",
                injInfo.getPreDestroyMethodName(), injInfo.getClassName()));
        }

        return m;
    }

}

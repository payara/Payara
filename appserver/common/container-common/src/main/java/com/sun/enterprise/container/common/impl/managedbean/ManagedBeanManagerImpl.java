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

package com.sun.enterprise.container.common.impl.managedbean;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.naming.InitialContext;

import org.glassfish.api.admin.ProcessEnvironment;
import org.glassfish.api.admin.ProcessEnvironment.ProcessType;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.Events;
import org.glassfish.api.naming.GlassfishNamingManager;
import org.glassfish.api.naming.NamingObjectProxy;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.runlevel.RunLevel;
import org.glassfish.internal.api.PostStartupRunLevel;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.deployment.Deployment;
import org.jvnet.hk2.annotations.Service;

import com.sun.enterprise.container.common.spi.InterceptorInvoker;
import com.sun.enterprise.container.common.spi.JCDIService;
import com.sun.enterprise.container.common.spi.JavaEEInterceptorBuilder;
import com.sun.enterprise.container.common.spi.JavaEEInterceptorBuilderFactory;
import com.sun.enterprise.container.common.spi.ManagedBeanManager;
import com.sun.enterprise.container.common.spi.util.ComponentEnvManager;
import com.sun.enterprise.container.common.spi.util.InjectionManager;
import com.sun.enterprise.container.common.spi.util.InterceptorInfo;
import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.ApplicationClientDescriptor;
import com.sun.enterprise.deployment.BundleDescriptor;
import com.sun.enterprise.deployment.EjbBundleDescriptor;
import com.sun.enterprise.deployment.EjbDescriptor;
import com.sun.enterprise.deployment.JndiNameEnvironment;
import com.sun.enterprise.deployment.LifecycleCallbackDescriptor;
import com.sun.enterprise.deployment.ManagedBeanDescriptor;
import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.logging.LogDomains;

/**
 */
@Service(name="ManagedBeanManagerImpl")
@RunLevel(value=PostStartupRunLevel.VAL, mode=RunLevel.RUNLEVEL_MODE_NON_VALIDATING)
public class ManagedBeanManagerImpl implements ManagedBeanManager, PostConstruct, EventListener {

     private static final Logger _logger = LogDomains.getLogger(ManagedBeanManagerImpl.class,
            LogDomains.CORE_LOGGER);

    @Inject
    private ComponentEnvManager compEnvManager;

    @Inject
    private InjectionManager injectionMgr;

    @Inject
    private GlassfishNamingManager namingManager;

    @Inject
    private ServiceLocator habitat;

    @Inject
    private Events events;

    @Inject
    private ProcessEnvironment processEnv;

    private ProcessType processType;

    // Keep track of contexts for managed bean instances instantiated via JCDI
    private Map<BundleDescriptor, Map<Object, JCDIService.JCDIInjectionContext>> jcdiManagedBeanInstanceMap =
            new HashMap<BundleDescriptor, Map<Object, JCDIService.JCDIInjectionContext>>();

    // Used to hold managed beans in app client container
    private Map<String, NamingObjectProxy> appClientManagedBeans = new HashMap<String, NamingObjectProxy>();

    public void postConstruct() {
        events.register(this);
        processType = processEnv.getProcessType();
    }

    public void event(Event event) {
        
         if (event.is(Deployment.APPLICATION_LOADED) ) {
             ApplicationInfo info =  Deployment.APPLICATION_LOADED.getHook(event);

             loadManagedBeans(info);

             registerAppLevelDependencies(info);

         } else if( event.is(Deployment.APPLICATION_UNLOADED) ) {
             
             ApplicationInfo info =  Deployment.APPLICATION_UNLOADED.getHook(event);
             Application app = info.getMetaData(Application.class);

             doCleanup(app);

         } else if( event.is(Deployment.DEPLOYMENT_FAILURE) ) {

             Application app = Deployment.DEPLOYMENT_FAILURE.getHook(event).getModuleMetaData(Application.class);

             doCleanup(app);

         }
    }

    private void doCleanup(Application app) {

        if( app != null ) {

            unloadManagedBeans(app);

            unregisterAppLevelDependencies(app);
        }

    }

    private void registerAppLevelDependencies(ApplicationInfo appInfo) {

        Application app = appInfo.getMetaData(Application.class);

        if( app == null ) {
            return;
        }

        try {
            compEnvManager.bindToComponentNamespace(app);
        } catch(Exception e) {
            throw new RuntimeException("Error binding app-level env dependencies " +
                    app.getAppName(), e);
        }

    }


    private void unregisterAppLevelDependencies(Application app) {

         if( app != null ) {
            try {
                compEnvManager.unbindFromComponentNamespace(app);
            } catch(Exception e) {
                _logger.log(Level.FINE, "Exception unbinding app objects", e);
            }
         }
     }



    private void loadManagedBeans(ApplicationInfo appInfo) {

        Application app = appInfo.getMetaData(Application.class);

        if( app == null ) {
            return;
        }

        loadManagedBeans(app);
    }
    
    public void loadManagedBeans(Application app) {

        JCDIService jcdiService = habitat.getService(JCDIService.class);

        for(BundleDescriptor bundle : app.getBundleDescriptors()) {

            if (!bundleEligible(bundle)) {
                continue;
            }

            boolean validationRequired  = (bundle instanceof EjbBundleDescriptor)
                    || (bundle instanceof ApplicationClientDescriptor);

            boolean isCDIBundle = (jcdiService != null && jcdiService.isJCDIEnabled(bundle));

            for(ManagedBeanDescriptor next : bundle.getManagedBeans()) {

                try {

                    // TODO Should move this to regular DOL processing stage                                      
                    if( validationRequired ) {
                        next.validate();
                    }

                    Set<String> interceptorClasses = next.getAllInterceptorClasses();


                    Class targetClass = bundle.getClassLoader().loadClass(next.getBeanClassName());
                    InterceptorInfo interceptorInfo = new InterceptorInfo();
                    interceptorInfo.setTargetClass(targetClass);
                    interceptorInfo.setInterceptorClassNames(interceptorClasses);
                    interceptorInfo.setAroundConstructInterceptors
                            (next.getAroundConstructCallbackInterceptors(targetClass, 
                                     getConstructor(targetClass, isCDIBundle)));
                    interceptorInfo.setPostConstructInterceptors
                            (next.getCallbackInterceptors(LifecycleCallbackDescriptor.CallbackType.POST_CONSTRUCT));
                      interceptorInfo.setPreDestroyInterceptors
                            (next.getCallbackInterceptors(LifecycleCallbackDescriptor.CallbackType.PRE_DESTROY));
                    if( next.hasAroundInvokeMethod() ) {
                        interceptorInfo.setHasTargetClassAroundInvoke(true);
                    }

                    Map<Method, List> interceptorChains = new HashMap<Method, List>();
                    for(Method m : targetClass.getMethods()) {
                        interceptorChains.put(m, next.getAroundInvokeInterceptors(m) );
                    }

                    interceptorInfo.setAroundInvokeInterceptorChains(interceptorChains);

                    // TODO can optimize this out for the non-JAXRS, non-application specified interceptor case
                    interceptorInfo.setSupportRuntimeDelegate(true);

                    JavaEEInterceptorBuilderFactory interceptorBuilderFactory =
                            habitat.getService(JavaEEInterceptorBuilderFactory.class);

                    JavaEEInterceptorBuilder builder = interceptorBuilderFactory.createBuilder(interceptorInfo);

                    next.setInterceptorBuilder(builder);


                    compEnvManager.bindToComponentNamespace(next);

                    String jndiName = next.getGlobalJndiName();
                    ManagedBeanNamingProxy namingProxy =
                        new ManagedBeanNamingProxy(next, habitat);

                    if( processType.isServer() ) {
                        namingManager.publishObject(jndiName, namingProxy, true);
                    } else {
                        // Can't store them in server's global naming service so keep
                        // them in local map.
                        appClientManagedBeans.put(jndiName, namingProxy);
                    }


                } catch(Exception e) {
                    throw new RuntimeException("Error binding ManagedBean " + next.getBeanClassName() +
                    " with name = " + next.getName(), e);
                }
            }

            jcdiManagedBeanInstanceMap.put(bundle,
                Collections.synchronizedMap(new HashMap<Object, JCDIService.JCDIInjectionContext>()));
        }

    }

    private Constructor getConstructor(Class clz, boolean isCDIBundle) throws Exception {
        if (isCDIBundle) {
            Constructor<?>[] ctors = clz.getDeclaredConstructors();
            for(Constructor<?> ctor : ctors) {
                if (ctor.getAnnotation(javax.inject.Inject.class) != null) {
                    // @Inject constructor
                    return ctor;
                }
            }
        }
        // Not a CDI bundle or no @Inject constructor - use no-arg constructor
        return clz.getConstructor();
    }

    public Object getManagedBean(String globalJndiName) throws Exception {

        NamingObjectProxy proxy = appClientManagedBeans.get(globalJndiName);

        Object managedBean = null;

        if( proxy != null ) {

            managedBean = proxy.create(new InitialContext());

        }

        return managedBean;      
    }

    /**
     * Apply a runtime interceptor instance to all managed beans in the given module
     * @param interceptorInstance
     * @param bundle bundle descripto
     *
     */
    public void registerRuntimeInterceptor(Object interceptorInstance, BundleDescriptor bundle) {


        for(ManagedBeanDescriptor next :  bundle.getManagedBeans()) {

            JavaEEInterceptorBuilder interceptorBuilder = (JavaEEInterceptorBuilder)
                       next.getInterceptorBuilder();

            interceptorBuilder.addRuntimeInterceptor(interceptorInstance);

        }

    }

    public void unloadManagedBeans(Application app) {

        for(BundleDescriptor bundle : app.getBundleDescriptors()) {

            if (!bundleEligible(bundle)) {
                continue;
            }

            Map<Object, JCDIService.JCDIInjectionContext> jcdiInstances =
                    jcdiManagedBeanInstanceMap.remove(bundle);

            if( jcdiInstances != null ) {
                for(JCDIService.JCDIInjectionContext next : jcdiInstances.values()) {
                    try {
                        next.cleanup(true);
                    } catch(Exception e) {
                        _logger.log(Level.FINE, "Exception during JCDI cleanup for " + next, e);
                    }
                }
            }

            for(ManagedBeanDescriptor next : bundle.getManagedBeans()) {

                for(Object instance : next.getBeanInstances()) {

                    InterceptorInvoker invoker = (InterceptorInvoker)
                        next.getSupportingInfoForBeanInstance(instance);

                    try {
                        invoker.invokePreDestroy();
                    } catch(Exception e) {
                        _logger.log(Level.FINE, "Managed bean " + next.getBeanClassName() + " PreDestroy", e);
                    }

                }                                 

                com.sun.enterprise.container.common.spi.util.ComponentEnvManager compEnvManager =
                    habitat.getService(com.sun.enterprise.container.common.spi.util.ComponentEnvManager.class);

                try {
                    compEnvManager.unbindFromComponentNamespace(next);
                } catch(javax.naming.NamingException ne) {
                    _logger.log(Level.FINE, "Managed bean " + next.getBeanClassName() + " unbind", ne);
                }

                org.glassfish.api.naming.GlassfishNamingManager namingManager =
                        habitat.getService(org.glassfish.api.naming.GlassfishNamingManager.class);
                String jndiName = next.getGlobalJndiName();

                if( processType.isServer() ) {
                    try {
                        namingManager.unpublishObject(jndiName);
                    } catch(javax.naming.NamingException ne) {
                        _logger.log(Level.FINE, "Error unpubishing managed bean " +
                               next.getBeanClassName() + " with jndi name " + jndiName, ne);
                    }
                } else {
                    appClientManagedBeans.remove(jndiName);
                }

                next.clearAllBeanInstanceInfo();

            }
        }

    }

    private boolean bundleEligible(BundleDescriptor bundle) {

        boolean eligible = false;

        if( processType.isServer() ) {

            eligible = (bundle instanceof WebBundleDescriptor) ||
                (bundle instanceof EjbBundleDescriptor);

        } else if( processType == ProcessType.ACC ) {
            eligible = (bundle instanceof ApplicationClientDescriptor);
        }

        return eligible;
    }

    public <T> T createManagedBean(Class<T> managedBeanClass) throws Exception {
        ManagedBeanDescriptor managedBeanDesc = null;

        try {
            BundleDescriptor bundle = getBundle();
            managedBeanDesc = bundle.getManagedBeanByBeanClass(managedBeanClass.getName());
        } catch(Exception e) {
            // OK.  Can mean that it's not annotated with @ManagedBean but 299 can handle it.
        }

        return createManagedBean(managedBeanDesc, managedBeanClass);
    }

    public <T> T createManagedBean(Class<T> managedBeanClass, boolean invokePostConstruct) throws Exception {
        ManagedBeanDescriptor managedBeanDesc = null;

        try {
            BundleDescriptor bundle = getBundle();
            managedBeanDesc = bundle.getManagedBeanByBeanClass(managedBeanClass.getName());
        } catch(Exception e) {
            // OK.  Can mean that it's not annotated with @ManagedBean but 299 can handle it.
        }

        return createManagedBean(managedBeanDesc, managedBeanClass, invokePostConstruct);
    }

    /**
     *
     * @param desc can be null if JCDI enabled bundle.
     * @param managedBeanClass
     * @return
     * @throws Exception
     */
    public <T> T createManagedBean(ManagedBeanDescriptor desc, Class<T> managedBeanClass) throws Exception {

        JCDIService jcdiService = habitat.getService(JCDIService.class);

        BundleDescriptor bundleDescriptor = null;

        if( desc == null ) {
            bundleDescriptor = getBundle();
        } else {
            bundleDescriptor = desc.getBundleDescriptor();
        }

        if( bundleDescriptor == null ) {
            throw new IllegalStateException
                ("Class " + managedBeanClass + " is not a valid EE ManagedBean class");
        }

        T callerObject = null;
        
        if( (jcdiService != null) && jcdiService.isJCDIEnabled(bundleDescriptor)) {

            // Have 299 create, inject, and call PostConstruct on managed bean

            JCDIService.JCDIInjectionContext jcdiContext =
                jcdiService.createManagedObject(managedBeanClass, bundleDescriptor);
            callerObject = (T) jcdiContext.getInstance();

            // Need to keep track of context in order to destroy properly
            Map<Object, JCDIService.JCDIInjectionContext> bundleNonManagedObjs =
                jcdiManagedBeanInstanceMap.get(bundleDescriptor);
            bundleNonManagedObjs.put(callerObject, jcdiContext);

        } else {


            JavaEEInterceptorBuilder interceptorBuilder = (JavaEEInterceptorBuilder)
                desc.getInterceptorBuilder();

            InterceptorInvoker interceptorInvoker =
                    interceptorBuilder.createInvoker(null);

            // This is the object passed back to the caller.
            callerObject = (T) interceptorInvoker.getProxy();

            Object[] interceptorInstances = interceptorInvoker.getInterceptorInstances();

            // Inject interceptor instances
            for(int i = 0; i < interceptorInstances.length; i++) {
                inject(interceptorInstances[i], desc);
            }

            interceptorInvoker.invokeAroundConstruct();

            // This is the managed bean class instance
            Object managedBean = interceptorInvoker.getTargetInstance();

            inject(managedBean, desc);

            interceptorInvoker.invokePostConstruct();

            desc.addBeanInstanceInfo(managedBean, interceptorInvoker);

        }

        return callerObject;

    }

    /**
     *
     * @param desc can be null if JCDI enabled bundle.
     * @param managedBeanClass
     * @param invokePostConstruct
     * @return
     * @throws Exception
     */
    public <T> T createManagedBean(ManagedBeanDescriptor desc, Class<T> managedBeanClass,
        boolean invokePostConstruct) throws Exception {

        JCDIService jcdiService = habitat.getService(JCDIService.class);

        BundleDescriptor bundleDescriptor = null;

        if( desc == null ) {
            bundleDescriptor = getBundle();
        } else {
            bundleDescriptor = desc.getBundleDescriptor();
        }

        if( bundleDescriptor == null ) {
            throw new IllegalStateException
                ("Class " + managedBeanClass + " is not a valid EE ManagedBean class");
        }

        T callerObject = null;

        if( (jcdiService != null) && jcdiService.isJCDIEnabled(bundleDescriptor)) {

            // Have 299 create, inject, and call PostConstruct (if desired) on managed bean

            JCDIService.JCDIInjectionContext jcdiContext =
                jcdiService.createManagedObject(managedBeanClass, bundleDescriptor, invokePostConstruct);
            callerObject = (T) jcdiContext.getInstance();
            // Need to keep track of context in order to destroy properly
            Map<Object, JCDIService.JCDIInjectionContext> bundleNonManagedObjs =
                jcdiManagedBeanInstanceMap.get(bundleDescriptor);
            bundleNonManagedObjs.put(callerObject, jcdiContext);

        } else {


            JavaEEInterceptorBuilder interceptorBuilder = (JavaEEInterceptorBuilder)
                desc.getInterceptorBuilder();

            // This is the managed bean class instance
            T managedBean = managedBeanClass.newInstance();

            InterceptorInvoker interceptorInvoker =
                    interceptorBuilder.createInvoker(managedBean);

            // This is the object passed back to the caller.
            callerObject = (T) interceptorInvoker.getProxy();

            Object[] interceptorInstances = interceptorInvoker.getInterceptorInstances();

            inject(managedBean, desc);

            // Inject interceptor instances
            for(int i = 0; i < interceptorInstances.length; i++) {
                inject(interceptorInstances[i], desc);
            }

            interceptorInvoker.invokePostConstruct();

            desc.addBeanInstanceInfo(managedBean, interceptorInvoker);

        }

        return callerObject;

    }

    public boolean isManagedBean(Object object) {

        JavaEEInterceptorBuilderFactory interceptorBuilderFactory =
                            habitat.getService(JavaEEInterceptorBuilderFactory.class);

        if (interceptorBuilderFactory != null) {
            return interceptorBuilderFactory.isClientProxy(object);
        } else {
            return false;
        }

    }

    private void inject(Object instance, ManagedBeanDescriptor managedBeanDesc)
        throws Exception {
        BundleDescriptor bundle = managedBeanDesc.getBundle();
        if( (bundle instanceof EjbBundleDescriptor) || (bundle instanceof ApplicationClientDescriptor) ) {
            injectionMgr.injectInstance(instance, managedBeanDesc.getGlobalJndiName(), false);
        } else {
            //  Inject instances, but use injection invoker for PostConstruct
            injectionMgr.injectInstance(instance, (JndiNameEnvironment) bundle, false);
        }
    }

    public void destroyManagedBean(Object managedBean)  {
        destroyManagedBean(managedBean, true);
    }

    public void destroyManagedBean(Object managedBean, boolean validate)  {

        BundleDescriptor bundle = getBundle();

        JCDIService jcdiService = habitat.getService(JCDIService.class);

        if( (jcdiService != null) && jcdiService.isJCDIEnabled(bundle)) {
            Map<Object, JCDIService.JCDIInjectionContext> bundleNonManagedObjs = jcdiManagedBeanInstanceMap.get(bundle);
            // in a failure scenario it's possible that bundleNonManagedObjs is null
            if ( bundleNonManagedObjs == null ) {
                if (validate) {
                    throw new IllegalStateException("Unknown JCDI-enabled managed bean " +
                                                     managedBean + " of class " + managedBean.getClass());
                }
                _logger.log(Level.FINE, "Unknown JCDI-enabled managed bean " +
                            managedBean + " of class " + managedBean.getClass());
            } else {
                JCDIService.JCDIInjectionContext context = bundleNonManagedObjs.remove(managedBean);
                if( context == null ) {
                    if (validate) {
                        throw new IllegalStateException("Unknown JCDI-enabled managed bean " +
                                                            managedBean + " of class " + managedBean.getClass());
                    }
                    _logger.log(Level.FINE, "Unknown JCDI-enabled managed bean " +
                        managedBean + " of class " + managedBean.getClass());
                    return;
                }

                // Call PreDestroy and cleanup
                context.cleanup(true);
            }
        } else {

            Object managedBeanInstance = null;

            try {

                Field proxyField = managedBean.getClass().getDeclaredField("__ejb31_delegate");

                final Field finalF = proxyField;
                    java.security.AccessController.doPrivileged(
                    new java.security.PrivilegedExceptionAction() {
                        public java.lang.Object run() throws Exception {
                            if( !finalF.isAccessible() ) {
                                finalF.setAccessible(true);
                            }
                            return null;
                        }
                    });

                Proxy proxy = (Proxy) proxyField.get(managedBean);

                InterceptorInvoker invoker = (InterceptorInvoker) Proxy.getInvocationHandler(proxy);

                managedBeanInstance = invoker.getTargetInstance();

            } catch(Exception e) {
                throw new IllegalArgumentException("invalid managed bean " + managedBean, e);
            }

            ManagedBeanDescriptor desc = bundle.getManagedBeanByBeanClass(
                    managedBeanInstance.getClass().getName());
            
            if( desc == null ) {
                throw new IllegalStateException("Could not retrieve managed bean descriptor for " +
                    managedBean + " of class " + managedBean.getClass());
            }
            
            InterceptorInvoker invoker = (InterceptorInvoker)
                desc.getSupportingInfoForBeanInstance(managedBeanInstance);

            try {
                invoker.invokePreDestroy();
            } catch(Exception e) {
                _logger.log(Level.FINE, "Managed bean " + desc.getBeanClassName() + " PreDestroy", e);
            }

            desc.clearBeanInstanceInfo(managedBeanInstance);
        }                      
    }

    private BundleDescriptor getBundle() {
        ComponentEnvManager compEnvManager = habitat.getService(ComponentEnvManager.class);

        JndiNameEnvironment env = compEnvManager.getCurrentJndiNameEnvironment();

        BundleDescriptor bundle = null;

        if( env instanceof BundleDescriptor) {

           bundle = (BundleDescriptor) env;

        } else if( env instanceof EjbDescriptor ) {

           bundle = (BundleDescriptor)
                   ((EjbDescriptor)env).getEjbBundleDescriptor().getModuleDescriptor().getDescriptor();
        }

        if( bundle == null ) {
           throw new IllegalStateException("Invalid context for managed bean creation");
        }

        return bundle;
    }
}

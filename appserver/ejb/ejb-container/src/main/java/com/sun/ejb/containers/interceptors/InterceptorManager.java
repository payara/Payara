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

package com.sun.ejb.containers.interceptors;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJBException;
import javax.interceptor.InvocationContext;

import com.sun.ejb.EJBUtils;
import com.sun.ejb.containers.BaseContainer;
import com.sun.ejb.containers.EJBContextImpl;
import com.sun.enterprise.container.common.spi.util.InterceptorInfo;
import com.sun.enterprise.deployment.EjbInterceptor;
import com.sun.enterprise.deployment.InterceptorDescriptor;
import com.sun.enterprise.deployment.LifecycleCallbackDescriptor;
import com.sun.enterprise.deployment.LifecycleCallbackDescriptor.CallbackType;
import com.sun.enterprise.deployment.MethodDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbSessionDescriptor;


/**
 * UserInterceptorsManager manages UserInterceptors. There
 * is one instance of InterceptorManager per container.
 *
 * @author Mahesh Kannan
 */
public class InterceptorManager {


    // The following are set when initializing EJB interceptors
    private BaseContainer container;
    private EjbDescriptor ejbDesc;

    // Set when initializing interceptors for a non-ejb
    private InterceptorInfo interceptorInfo;

    private ClassLoader loader;

    private Class beanClass;

    private String beanClassName;

    private Logger _logger;

    private Class[] interceptorClasses;
    
    private Class[] serializableInterceptorClasses;

    private Map<String, Integer> instanceIndexMap
            = new HashMap<String, Integer>();

    private boolean interceptorsExists;

    private String[] pre30LCMethodNames;

    private Class[] lcAnnotationClasses;

    private CallbackChainImpl[] callbackChain;

    // Optionally specified delegate to be set on SystemInterceptorProxy
    private Object runtimeInterceptor;

    List<InterceptorDescriptor> frameworkInterceptors = new LinkedList<InterceptorDescriptor>();


    public InterceptorManager(Logger _logger, BaseContainer container,
                              Class[] lcAnnotationClasses, String[] pre30LCMethodNames)
            throws Exception {
        this._logger = _logger;
        this.container = container;
        this.lcAnnotationClasses = lcAnnotationClasses;
        this.pre30LCMethodNames = pre30LCMethodNames;

        ejbDesc = container.getEjbDescriptor();
        loader = container.getClassLoader();
        beanClassName = ejbDesc.getEjbImplClassName();

        this.beanClass = loader.loadClass(beanClassName);
        frameworkInterceptors = ejbDesc.getFrameworkInterceptors();

        buildEjbInterceptorChain();

        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, "InterceptorManager: " + toString());
        }
    }

    public InterceptorManager(Logger _logger, ClassLoader classLoader, String className,
                              InterceptorInfo interceptorInfo)
            throws Exception {
        this._logger = _logger;


        loader = classLoader;
        beanClassName = className;

        this.beanClass = loader.loadClass(beanClassName);

        this.interceptorInfo = interceptorInfo;
        if( interceptorInfo.getSupportRuntimeDelegate() ) {
            frameworkInterceptors.add(SystemInterceptorProxy.createInterceptorDesc());
        }

        buildInterceptorChain(interceptorInfo);

    }

    // Used when InterceptorManager should instantiate interceptor instances
    // Alternatively, if the caller needs to instantiate the instances, call
    // getInterceptorClasses() and then initializeInterceptorInstances(Object[])
    public Object[] createInterceptorInstances() {
        int size = serializableInterceptorClasses.length;
        Object[] interceptors = new Object[size];

        int newInstanceSize = serializableInterceptorClasses.length;

        for (int index = 0; index < newInstanceSize; index++) {

            Class clazz = serializableInterceptorClasses[index];
            try {
                interceptors[index] = clazz.newInstance();
            } catch (IllegalAccessException illEx) {
                throw new RuntimeException(illEx);
            } catch (InstantiationException instEx) {
                throw new RuntimeException(instEx);
            }

        }

        initializeInterceptorInstances(interceptors);

        return interceptors;
    }

    public Class[] getInterceptorClasses() {
        return serializableInterceptorClasses;
    }

    public void initializeInterceptorInstances(Object[] interceptorInstances) {

        for (int index = 0; index < interceptorInstances.length; index++) {
            Class clazz = serializableInterceptorClasses[index];

            if( SystemInterceptorProxy.class.isAssignableFrom(clazz) && (runtimeInterceptor != null) ) {
                ((SystemInterceptorProxy)interceptorInstances[index]).setDelegate(runtimeInterceptor);
            }
        }
    }


    /**
     * Called sometime after original interceptor initialization.
     * Install the given interceptor class instance before any application
     * level interceptors.
     * param o  instance of an interceptor class
     */
    public void registerRuntimeInterceptor(Object o) {

        runtimeInterceptor = o;

    }

    public InterceptorManager.InterceptorChain getAroundInvokeChain(
            MethodDescriptor mDesc, Method beanMethod) {

        ArrayList<AroundInvokeInterceptor> interceptors =
                new ArrayList<AroundInvokeInterceptor>();

        for(InterceptorDescriptor interceptor : frameworkInterceptors) {
            Set<LifecycleCallbackDescriptor> aroundInvokeDescs =
                interceptor.getAroundInvokeDescriptors();
            if(aroundInvokeDescs.isEmpty() ) {
                continue;
            }

            List<LifecycleCallbackDescriptor> orderedAIInterceptors = null;
            Class interceptorClass = interceptor.getInterceptorClass();
            ClassLoader classLoaderToUse = (interceptorClass != null) ?
                        interceptorClass.getClassLoader() : loader;
            try {
                orderedAIInterceptors = interceptor.getOrderedAroundInvokeDescriptors(classLoaderToUse);
            } catch (Exception e) {
                throw new IllegalStateException("No AroundInvokeIntercetpors found "
                + " on class " + interceptor.getInterceptorClassName(), e);
            }
            addAroundInvokeInterceptors(interceptors, interceptor,
                orderedAIInterceptors, interceptor.getInterceptorClassName(),
                    classLoaderToUse);
        }


        List<InterceptorDescriptor> list = (ejbDesc != null) ?
                ejbDesc.getAroundInvokeInterceptors(mDesc) :
                interceptorInfo.getAroundInvokeInterceptors(beanMethod);


        for (InterceptorDescriptor interceptor : list) {
            String className = interceptor.getInterceptorClassName();
            Set<LifecycleCallbackDescriptor> aroundInvokeDescs = 
                interceptor.getAroundInvokeDescriptors();
            if(aroundInvokeDescs.isEmpty() ) {
                continue;
            }

            List<LifecycleCallbackDescriptor> orderedAIInterceptors = null;
            try {
                orderedAIInterceptors = interceptor.getOrderedAroundInvokeDescriptors(loader);
            } catch (Exception e) {
               throw new IllegalStateException("No AroundInvokeIntercetpors found "
                   + " on class " + className, e); 
            }
            addAroundInvokeInterceptors(interceptors, interceptor,
                    orderedAIInterceptors, className, loader);
        }

        AroundInvokeInterceptor[] inter = interceptors.toArray(
                new AroundInvokeInterceptor[interceptors.size()]);
        return new AroundInvokeChainImpl(inter);
    }

    public InterceptorManager.InterceptorChain getAroundTimeoutChain(
            MethodDescriptor mDesc, Method beanMethod) {


        ArrayList<AroundInvokeInterceptor> interceptors =
                new ArrayList<AroundInvokeInterceptor>();


        for(InterceptorDescriptor interceptor : frameworkInterceptors) {
            Set<LifecycleCallbackDescriptor> aroundTimeoutDescs =
                interceptor.getAroundTimeoutDescriptors();
            if(aroundTimeoutDescs.isEmpty() ) {
                continue;
            }

            List<LifecycleCallbackDescriptor> orderedAIInterceptors = null;
            Class interceptorClass = interceptor.getInterceptorClass();
            ClassLoader classLoaderToUse = (interceptorClass != null) ?
                        interceptorClass.getClassLoader() : loader;
            try {
                orderedAIInterceptors = interceptor.getOrderedAroundTimeoutDescriptors(classLoaderToUse);
            } catch (Exception e) {
                throw new IllegalStateException("No AroundTimeoutIntercetpors found "
                + " on class " + interceptor.getInterceptorClassName(), e);
            }
            addAroundInvokeInterceptors(interceptors, interceptor,
                orderedAIInterceptors, interceptor.getInterceptorClassName(),
                    classLoaderToUse);
        }

        List<EjbInterceptor> list = (ejbDesc != null) ?
                ejbDesc.getAroundTimeoutInterceptors(mDesc) :
                new LinkedList<EjbInterceptor>();

        for (EjbInterceptor interceptor : list) {
            String className = interceptor.getInterceptorClassName();
            Set<LifecycleCallbackDescriptor> aroundTimeoutDescs = 
                interceptor.getAroundTimeoutDescriptors();
            if(aroundTimeoutDescs.isEmpty() ) {
                continue;
            }

            List<LifecycleCallbackDescriptor> orderedATInterceptors;
            try {
                orderedATInterceptors = interceptor.getOrderedAroundTimeoutDescriptors(loader);
            } catch (Exception e) {
               throw new IllegalStateException("No AroundTimeoutIntercetpors found "
                   + " on class " + className, e); 
            }
            addAroundInvokeInterceptors(interceptors, interceptor, 
                    orderedATInterceptors, className, loader);
        }

        AroundInvokeInterceptor[] inter = interceptors.toArray(
                new AroundInvokeInterceptor[interceptors.size()]);
        return new AroundInvokeChainImpl(inter);
    }

    private void addAroundInvokeInterceptors(
            List<AroundInvokeInterceptor> interceptors,
            InterceptorDescriptor interceptor,
            List<LifecycleCallbackDescriptor> orderedInterceptors, 
            String className,
            ClassLoader classLoaderToUse) {

        for(LifecycleCallbackDescriptor desc : orderedInterceptors) {
            Method method = null;
            try {
                method = desc.getLifecycleCallbackMethodObject(classLoaderToUse);
            } catch(Exception e) {
               throw new IllegalStateException("No callback method of name " +
                       desc.getLifecycleCallbackMethod() 
               + " found on class " + className, e); 
            }

            if (interceptor.getFromBeanClass()) {
                interceptors.add(new BeanAroundInvokeInterceptor(method));
            } else {
                Integer bigInt = instanceIndexMap.get(className);
                int index = (bigInt == null) ? -1 : bigInt;
                if (index == -1) {
                    throw new IllegalStateException(getInternalErrorString(className));
                }
                Class clazz = interceptorClasses[index];
                _logger.log(Level.FINE, "*[md.getDeclaredMethod() => " 
                            + method + " FOR CLAZZ: " + clazz);  
                interceptors.add(new AroundInvokeInterceptor(index, method));
            }
        }
    }

    public boolean hasInterceptors() {
        return this.interceptorsExists;
    }

    public Object intercept(InterceptorManager.InterceptorChain chain,
                            AroundInvokeContext ctx)
            throws Throwable {
        return chain.invokeNext(0, ctx);
    }

    public boolean intercept(CallbackType eventType, EJBContextImpl ctx)
            throws Throwable {

        return intercept(eventType, ctx.getEJB(), ctx.getInterceptorInstances(), ctx);
    }

    public boolean intercept(CallbackType eventType, Object targetObject,
                             Object[] interceptorInstances)
            throws Throwable {
        return intercept(eventType, targetObject, interceptorInstances, null);
    }

    public boolean intercept(CallbackType eventType, Object targetObject,
                             Object[] interceptorInstances, EJBContextImpl ctx)
            throws Throwable {

        CallbackChainImpl chain = null;
        CallbackInvocationContext invContext = null;
        switch (eventType) {
            case AROUND_CONSTRUCT:
                chain = callbackChain[eventType.ordinal()];
                if (container != null) {
                    invContext = new CallbackInvocationContext(beanClass, 
                            interceptorInstances, chain, eventType, container, ctx);
                } else {
                    invContext = new CallbackInvocationContext(beanClass, 
                            interceptorInstances, chain, eventType, interceptorInfo);
                }
                if (chain != null) {
                    chain.invokeNext(0, invContext);
                }
                break;
            case POST_CONSTRUCT:
            case PRE_PASSIVATE:
            case POST_ACTIVATE:
            case PRE_DESTROY:
                chain = callbackChain[eventType.ordinal()];
                invContext = new CallbackInvocationContext(targetObject, 
                        interceptorInstances, chain, eventType);
                if (chain != null) {
                    chain.invokeNext(0, invContext);
                }
                break;
            default:
                throw new IllegalStateException("Invalid event type");
        }

        return true;
    }

    Object getTargetInstance() {
        return interceptorInfo.getTargetObjectInstance();
    }

    private void buildEjbInterceptorChain()
            throws Exception {

        Set<Class> listOfClasses = new HashSet<Class>();
        for(String name : ejbDesc.getInterceptorClassNames()) {
            listOfClasses.add( loader.loadClass(name));
        }

        // Add framework interceptors to list, but check for existence of
        // class before attempting to load it via application class loader
        for(InterceptorDescriptor frameworkInterceptor : frameworkInterceptors) {
            Class clazz = frameworkInterceptor.getInterceptorClass();
            if( clazz == null ) {
                clazz = loader.loadClass(frameworkInterceptor.getInterceptorClassName());
            }
            listOfClasses.add(clazz);

        }

        initInterceptorClasses(listOfClasses);

        interceptorsExists = (listOfClasses.size() > 0) ||
            ejbDesc.hasAroundInvokeMethod() || ejbDesc.hasAroundTimeoutMethod();

        initEjbCallbackIndices();
    }

    private void buildInterceptorChain(InterceptorInfo interceptorInfo)
            throws Exception {


        Set<Class> listOfClasses = new HashSet<Class>();
        for(String name : interceptorInfo.getInterceptorClassNames()) {
            listOfClasses.add( loader.loadClass(name));
        }

        // Add framework interceptors to list, but check for existence of
        // class before attempting to load it via application class loader
        for(InterceptorDescriptor frameworkInterceptor : frameworkInterceptors) {
            Class clazz = frameworkInterceptor.getInterceptorClass();
            if( clazz == null ) {
                clazz = loader.loadClass(frameworkInterceptor.getInterceptorClassName());
            }
            listOfClasses.add(clazz);
        }

        initInterceptorClasses(listOfClasses);

        interceptorsExists = (listOfClasses.size() > 0) ||
            interceptorInfo.getHasTargetClassAroundInvoke();

        int size = CallbackType.values().length;
        callbackChain = new CallbackChainImpl[size];

        initCallbackIndices(interceptorInfo.getAroundConstructInterceptors(), CallbackType.AROUND_CONSTRUCT);
        initCallbackIndices(interceptorInfo.getPostConstructInterceptors(), CallbackType.POST_CONSTRUCT);
        initCallbackIndices(interceptorInfo.getPreDestroyInterceptors(), CallbackType.PRE_DESTROY);

    }


    private void initInterceptorClasses(Set<Class> classes) throws  Exception {

        int size = classes.size();
        interceptorClasses = new Class[size];
        serializableInterceptorClasses = new Class[size];
        int index = 0;
        for (Class interClass : classes) {

            interceptorClasses[index] = interClass;
            serializableInterceptorClasses[index] = interClass;
            instanceIndexMap.put(interClass.getName(), index);
            if (!Serializable.class.isAssignableFrom(interClass)) {

                serializableInterceptorClasses[index] = 
                    EJBUtils.loadGeneratedSerializableClass(interClass.getClassLoader(),
                            interClass.getName());

            }
            index++;
        }


        // bean class is never accessed from instanceIndexMap so it's
        // never added.
    }

    private void initEjbCallbackIndices()
            throws ClassNotFoundException, Exception {

        int size = CallbackType.values().length;
        ArrayList[] callbacks = new ArrayList[size];
        boolean scanFor2xLifecycleMethods = true;
        int numPostConstructFrameworkCallbacks = 0;
        for (CallbackType eventType : CallbackType.values()) {
            int index = eventType.ordinal();
            callbacks[index] = new ArrayList<CallbackInterceptor>();
            boolean scanForCallbacks = true;
            if (! (ejbDesc instanceof EjbSessionDescriptor)) {
                if ((eventType == CallbackType.PRE_PASSIVATE) ||
                        (eventType == CallbackType.POST_ACTIVATE)) {
                    scanForCallbacks = false;
                }
            }

            if (scanForCallbacks) {

                // Make sure any framework interceptors are first
                for(InterceptorDescriptor callback : frameworkInterceptors) {
                    if( callback.hasCallbackDescriptor(eventType)) {
                        Class interceptorClass = callback.getInterceptorClass();
                        ClassLoader classLoaderToUse = (interceptorClass != null) ?
                                interceptorClass.getClassLoader() : loader;
                        List<CallbackInterceptor> inters = createCallbackInterceptors(eventType, callback,
                                classLoaderToUse);
                        for (CallbackInterceptor inter : inters) {
                            callbacks[index].add(inter);
                            if( eventType == CallbackType.POST_CONSTRUCT) {
                                numPostConstructFrameworkCallbacks++;
                            }
                        }
                    }
                }

                List<EjbInterceptor> callbackList = ejbDesc.getCallbackInterceptors(eventType);

                // If there are any application-specified callbacks in metadata, there's no need
                // to scan for the old 2.x style lifecycle methods
                if( callbackList.size() > 0 ) {
                    scanFor2xLifecycleMethods = false;
                }

                for (EjbInterceptor callback : callbackList) {
                    List<CallbackInterceptor> inters = createCallbackInterceptors(eventType, callback);
                    for (CallbackInterceptor inter : inters) {
                        callbacks[index].add(inter);
                    }
                }
            }

        }

        if (scanFor2xLifecycleMethods) {
            load2xLifecycleMethods(callbacks);
        }
        
        //The next set of lines are to handle the case where
        //  the app doesn't have a @PostConstruct or it 
        //  doesn't implement the EntrerpriseBean interface
        //  In this case we scan for ejbCreate() for MDBs and SLSBs
        boolean lookForEjbCreateMethod = container.scanForEjbCreateMethod();
        
        if (lookForEjbCreateMethod) {
            loadOnlyEjbCreateMethod(callbacks, numPostConstructFrameworkCallbacks);
        }
        
        callbackChain = new CallbackChainImpl[size];
        for (CallbackType eventType : CallbackType.values()) {
            int index = eventType.ordinal();
            CallbackInterceptor[] interceptors = (CallbackInterceptor[])
                    callbacks[index].toArray(new CallbackInterceptor[callbacks[index].size()]);
            callbackChain[index] = new CallbackChainImpl(interceptors);
        }

    }

    private void initCallbackIndices(List<InterceptorDescriptor> callbackList,
                                     CallbackType callbackType) throws Exception {

        ArrayList<CallbackInterceptor> callbacks = new ArrayList<CallbackInterceptor>();

        int index = callbackType.ordinal();

        for(InterceptorDescriptor callback : frameworkInterceptors) {
            if( callback.hasCallbackDescriptor(callbackType)) {
                Class interceptorClass = callback.getInterceptorClass();
                ClassLoader classLoaderToUse = (interceptorClass != null) ?
                        interceptorClass.getClassLoader() : loader;
                List<CallbackInterceptor> inters = createCallbackInterceptors(callbackType, callback,
                        classLoaderToUse);
                for (CallbackInterceptor inter : inters) {
                    callbacks.add(inter);
                }
            }
        }

        for (InterceptorDescriptor callback : callbackList) {
            List<CallbackInterceptor> inters = createCallbackInterceptors(callbackType, callback);
            for (CallbackInterceptor inter : inters) {
                callbacks.add(inter);
            }
        }

        // move above callbackChain = new CallbackChainImpl[size];

        CallbackInterceptor[] interceptors = (CallbackInterceptor[])
            callbacks.toArray(new CallbackInterceptor[callbacks.size()]);
        callbackChain[index] = new CallbackChainImpl(interceptors);

    }

    private List<CallbackInterceptor> createCallbackInterceptors(CallbackType eventType,
                                                          InterceptorDescriptor inter) throws Exception {
        // Create callback interceptors using loader provided at initialization time
        return createCallbackInterceptors(eventType, inter, loader);

    }

    private List<CallbackInterceptor> createCallbackInterceptors(CallbackType eventType,
                                                          InterceptorDescriptor inter,
                                                          ClassLoader classLoaderToUse) throws Exception {
        List<CallbackInterceptor> callbackList = new ArrayList<CallbackInterceptor>();
        
        List<LifecycleCallbackDescriptor> orderedCallbackMethods = 
            inter.getOrderedCallbackDescriptors(eventType, classLoaderToUse);

        String className = inter.getInterceptorClassName();

        for (LifecycleCallbackDescriptor callbackDesc : orderedCallbackMethods) {
            Method method = null;
            try {
                method = callbackDesc.getLifecycleCallbackMethodObject(classLoaderToUse);
            } catch(Exception e) {
                throw new IllegalStateException("No callback method of name " +
                   callbackDesc.getLifecycleCallbackMethod() 
                   + " found on class " + className, e);
	        }

            CallbackInterceptor interceptor = null;
            if (inter.getFromBeanClass()) {
                interceptor = new BeanCallbackInterceptor(method);
            } else {
                Integer bigInt = instanceIndexMap.get(className);
                int index = (bigInt == null) ? -1 : bigInt;
                if (index == -1) {
                    throw new IllegalStateException(getInternalErrorString(className));
                }
                interceptor = new CallbackInterceptor(index, method);
            }

            callbackList.add(interceptor);
        }

        return callbackList;
    }


    private void load2xLifecycleMethods(ArrayList<CallbackInterceptor>[] metaArray) {

        if (javax.ejb.EnterpriseBean.class.isAssignableFrom(beanClass)) {
            int sz = lcAnnotationClasses.length;
            for (int i = 0; i < sz; i++) {
                if (pre30LCMethodNames[i] == null) {
                    continue;
                }
                try {
                    Method method = beanClass.getMethod(
                            pre30LCMethodNames[i], (Class[]) null);
                    if (method != null) {    
                        CallbackInterceptor meta =
                                new BeanCallbackInterceptor(method);
                        metaArray[i].add(meta);
                        _logger.log(Level.FINE, "**## bean has 2.x LM: " + meta);
                    }
                } catch (NoSuchMethodException nsmEx) {
                    //TODO: Log exception
                    //Error for a 2.x bean????
                }
            }
        }
    }
    
    //TODO: load2xLifecycleMethods and loadOnlyEjbCreateMethod can be 
    //  refactored to use a common method.
    private void loadOnlyEjbCreateMethod(
            ArrayList<CallbackInterceptor>[] metaArray,
            int numPostConstructFrameworkCallbacks) {
        int sz = lcAnnotationClasses.length;
        for (int i = 0; i < sz; i++) {
            if (lcAnnotationClasses[i] != PostConstruct.class) {
                continue;
            }

            boolean needToScan = true;
            if (metaArray[i] != null) {
                ArrayList<CallbackInterceptor> al = metaArray[i];
                needToScan =  (al.size() == numPostConstructFrameworkCallbacks);
            }
            
            if (! needToScan) {
                // We already have found a @PostConstruct method
                // So just ignore any ejbCreate() method
                break;
            } else {
                try {
                    Method method = beanClass.getMethod(pre30LCMethodNames[i],
                            (Class[]) null);
                    if (method != null) {
                        CallbackInterceptor meta = new BeanCallbackInterceptor(
                                method);
                        metaArray[i].add(meta);
                        _logger.log(Level.FINE,
                                "**##[ejbCreate] bean has 2.x style ejbCreate: " + meta);
                    }
                } catch (NoSuchMethodException nsmEx) {
                    // TODO: Log exception
                    //Error for a 2.x bean????
                }
            }
        }
    }
    

    public String toString() {
        StringBuilder sbldr = new StringBuilder();
        sbldr.append("##########################################################\n");
        sbldr.append("InterceptorManager<").append(beanClassName).append("> has ")
                .append(interceptorClasses.length).append(" interceptors");
        sbldr.append("\n\tbeanClassName: ").append(beanClassName);
        sbldr.append("\n\tInterceptors: ");
        for (Class clazz : interceptorClasses) {
            sbldr.append("\n\t\t").append(clazz.getName());
        }
        if (lcAnnotationClasses != null) {
            // lcAnnotationClasses are available for EJBs only
            sbldr.append("\n\tCallback Interceptors: ");
            for (int i = 0; i < lcAnnotationClasses.length; i++) {
                CallbackChainImpl chain = callbackChain[i];
                sbldr.append("\n\t").append(i)
                        .append(": ").append(lcAnnotationClasses[i]);
                sbldr.append("\n\t\t").append(chain.toString());
            }
        }
        sbldr.append("\n");
        sbldr.append("##########################################################\n");
        return sbldr.toString();
    }

    private String getInternalErrorString(String className) {
        StringBuilder sbldr = new StringBuilder("Internal error: ");
        sbldr.append(" className: ").append(className)
                .append(" is neither a bean class (")
                .append(beanClassName).append(") nor an ")
                .append("interceptor class (");
        for (Class cn : interceptorClasses) {
            sbldr.append(cn.getName()).append("; ");
        }
        sbldr.append(")");
        _logger.log(Level.INFO, "++ : " + sbldr.toString());
        return sbldr.toString();
    }

    public interface AroundInvokeContext extends InvocationContext {

        public Object[] getInterceptorInstances();

       /**
        * Called from Interceptor Chain to invoke the actual bean method.
        * This method must throw any exception from the bean method *as is*,
        * without being wrapped in an InvocationTargetException.  The exception
        * thrown from this method will be propagated through the application's
        * interceptor code, so it must not be changed in order for any exception
        * handling logic in that code to function properly.
        */
        public  Object invokeBeanMethod()
            throws Throwable;

    }

    public interface InterceptorChain {
	public Object invokeNext(int index, AroundInvokeContext invCtx)
	    throws Throwable;
    }



}

class AroundInvokeChainImpl
        implements InterceptorManager.InterceptorChain {
    enum ChainType {
        METHOD, CALLBACK}


    protected AroundInvokeInterceptor[] interceptors;
    protected int size;


    protected AroundInvokeChainImpl(AroundInvokeInterceptor[] interceptors) {

        this.interceptors = interceptors;
        this.size = (interceptors == null) ? 0 : interceptors.length;
    }

    public Object invokeNext(int index, InterceptorManager.AroundInvokeContext inv)
            throws Throwable {
        return ( index < size ) ?
            interceptors[index].intercept(inv) :
            inv.invokeBeanMethod();
    }

    public String toString() {
        StringBuilder bldr = new StringBuilder();
        for (AroundInvokeInterceptor inter : interceptors) {
            bldr.append("\n\t").append(inter);
        }

        return bldr.toString();
    }
}


class AroundInvokeInterceptor {
    protected int index;
    protected Method method;

    AroundInvokeInterceptor(int index, Method method) {
        this.index = index;
        this.method = method;

        try {
            final Method finalM = method;
            if(System.getSecurityManager() == null) {
                if (!finalM.isAccessible()) {
                    finalM.setAccessible(true);
                }
            } else {
                java.security.AccessController
                        .doPrivileged(new java.security.PrivilegedExceptionAction() {
                    public java.lang.Object run() throws Exception {
                        if (!finalM.isAccessible()) {
                            finalM.setAccessible(true);
                        }
                        return null;
                    }});
            }
        } catch(Exception e) {
            throw new EJBException(e);
        }

    }

    Object intercept(final InterceptorManager.AroundInvokeContext invCtx) throws Throwable {
        try {
            final Object[] interceptors = invCtx.getInterceptorInstances();

            if( System.getSecurityManager() != null ) {
            // Wrap actual value insertion in doPrivileged to
            // allow for private/protected field access.
            return java.security.AccessController
                    .doPrivileged(new java.security.PrivilegedExceptionAction() {
                        public java.lang.Object run() throws Exception {
                            return method.invoke(interceptors[index], invCtx);
                        }
                    });
            } else {

                 return method.invoke(interceptors[index], invCtx);

            }
        } catch (java.lang.reflect.InvocationTargetException invEx) {
            throw invEx.getCause();
        } catch (java.security.PrivilegedActionException paEx) {
            Throwable th = paEx.getCause();
            if (th.getCause() != null) {
                throw th.getCause();
            }
            throw th;
        }
    }

    public String toString() {
        return "[" + index + "]: " + method;
    }

}

class BeanAroundInvokeInterceptor
        extends AroundInvokeInterceptor {
    private static final Object[] NULL_ARGS = null;

    BeanAroundInvokeInterceptor(Method method) {
        super(-1, method);
    }

    Object intercept(final InterceptorManager.AroundInvokeContext invCtx) throws Throwable {
        try {

            if( System.getSecurityManager() != null ) {
            // Wrap actual value insertion in doPrivileged to
            // allow for private/protected field access.
            return java.security.AccessController
                    .doPrivileged(new java.security.PrivilegedExceptionAction() {
                        public java.lang.Object run() throws Exception {
                            return method.invoke(invCtx.getTarget(), invCtx);
                        }
                    });
            } else {
                return method.invoke(invCtx.getTarget(), invCtx);
            }
        } catch (java.lang.reflect.InvocationTargetException invEx) {
            throw invEx.getCause();
        } catch (java.security.PrivilegedActionException paEx) {
            Throwable th = paEx.getCause();
            if (th.getCause() != null) {
                throw th.getCause();
            }
            throw th;
        }
    }
}

class CallbackInterceptor {
    protected int index;
    protected Method method;

    CallbackInterceptor(int index, Method method) {
        this.index = index;
        this.method = method;

        try {
        final Method finalM = method;
        if(System.getSecurityManager() == null) {
            if (!finalM.isAccessible()) {
                finalM.setAccessible(true);
            }
        } else {
            java.security.AccessController
                    .doPrivileged(new java.security.PrivilegedExceptionAction() {
                public java.lang.Object run() throws Exception {
                    if (!finalM.isAccessible()) {
                        finalM.setAccessible(true);
                    }
                    return null;
                }});
        }
        } catch(Exception e) {
            throw new EJBException(e);
        }

    }

    Object intercept(final CallbackInvocationContext invContext) 
        throws Throwable {
        try {

            final Object[] interceptors = invContext
                    .getInterceptorInstances();

            if( System.getSecurityManager() != null ) {
            // Wrap actual value insertion in doPrivileged to
            // allow for private/protected field access.
            return java.security.AccessController
                    .doPrivileged(new java.security.PrivilegedExceptionAction() {
                        public java.lang.Object run() throws Exception {
                            return method.invoke(interceptors[index],
                                                 invContext);
                        }
                    });
            } else {
                return method.invoke(interceptors[index], invContext);
                                     
            }
        } catch (java.lang.reflect.InvocationTargetException invEx) {
            throw invEx.getCause();
        } catch (java.security.PrivilegedActionException paEx) {
            Throwable th = paEx.getCause();
            if (th.getCause() != null) {
                throw th.getCause();
            }
            throw th;
        }
    }

    boolean isBeanCallback() {
        return false;
    }

    public String toString() {
        return "callback[" + index + "]: " + method;
    }
}

class BeanCallbackInterceptor
        extends CallbackInterceptor {
    private static final Object[] NULL_ARGS = null;

    BeanCallbackInterceptor(Method method) {
        super(-1, method);
    }

    Object intercept(final CallbackInvocationContext invContext) 
        throws Throwable {
        try {

            if( System.getSecurityManager() != null ) {
            // Wrap actual value insertion in doPrivileged to
            // allow for private/protected field access.
               java.security.AccessController
                    .doPrivileged(new java.security.PrivilegedExceptionAction() {
                        public java.lang.Object run() throws Exception {

                            method.invoke(invContext.getTarget(),
                                          NULL_ARGS);
                            return null;

                        }
                    });
            } else {
                method.invoke(invContext.getTarget(), NULL_ARGS);
            }

            return invContext.proceed();

        } catch (java.lang.reflect.InvocationTargetException invEx) {
            throw invEx.getCause();
        } catch (java.security.PrivilegedActionException paEx) {
            Throwable th = paEx.getCause();
            if (th.getCause() != null) {
                throw th.getCause();
            }
            throw th;
        }
    }

    boolean isBeanCallback() {
        return true;
    }

    public String toString() {
        return "beancallback[" + index + "]: " + method;
    }
}

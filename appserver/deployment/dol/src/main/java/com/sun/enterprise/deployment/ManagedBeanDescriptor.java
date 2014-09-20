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

 package com.sun.enterprise.deployment;

import com.sun.enterprise.deployment.util.ManagedBeanVisitor;
import com.sun.enterprise.deployment.util.ApplicationValidator;
import com.sun.enterprise.deployment.types.EjbReference;
import com.sun.enterprise.deployment.types.MessageDestinationReferencer;
import com.sun.enterprise.deployment.LifecycleCallbackDescriptor.CallbackType;

import java.util.*;
import java.lang.reflect.Constructor;

import java.lang.reflect.Method;

/**
 * Descriptor representing a Java EE Managed Bean.
 *
 * @author Kenneth Saks
 */

public class ManagedBeanDescriptor extends JndiEnvironmentRefsGroupDescriptor {

    // *Optional* managed bean name.  Only non-null if the
    // bean has been assigned a name by the developer.
    // (E.g., via the @ManagedBean name() attribute)
    private String name;

    // fully-qualified class name of managed bean class
    private String beanClassName;

    // Module in which managed bean is defined
    private BundleDescriptor enclosingBundle;

    private Object interceptorBuilder = null;
    private Collection beanInstances = new HashSet();
    private Map<Object, Object> beanSupportingInfo = new HashMap<Object, Object>();

    private List<InterceptorDescriptor> classInterceptorChain = new LinkedList<InterceptorDescriptor>();

    private Set<LifecycleCallbackDescriptor> aroundInvokeDescs = new HashSet<LifecycleCallbackDescriptor>();

    //
    // Interceptor info per business method.  If the map does not
    // contain an entry for the business method, there is no method-specific
    // interceptor information for that method.  In that case the standard
    // class-level interceptor information applies.
    //
    // If there is an entry for the business method, the corresponding list
    // represents the *complete* ordered list of interceptor classes for that
    // method.  An empty list would mean all the interceptors have been
    // disabled for that particular business method.
    //
    private Map<MethodDescriptor, List<InterceptorDescriptor>> methodInterceptorsMap =
            new HashMap<MethodDescriptor, List<InterceptorDescriptor>>();

	/** 
	* Default constructor. 
	*/
    public ManagedBeanDescriptor() {}

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean isNamed() {
        return (name != null);
    }

    public void setBeanClassName(String className) {
        beanClassName = className;
    }

    public String getBeanClassName() {
        return beanClassName;
    }

    public void setBundle(BundleDescriptor bundle) {
        enclosingBundle = bundle;
        super.setBundleDescriptor(bundle);
    }

    public BundleDescriptor getBundle() {
        return enclosingBundle;
    }

    public void setInterceptorBuilder(Object b) {
        interceptorBuilder = b;
    }

    public Object getInterceptorBuilder() {
        return interceptorBuilder;
    }

    public boolean hasInterceptorBuilder() {
        return (interceptorBuilder != null);
    }


    public void addBeanInstanceInfo(Object o) {
         addBeanInstanceInfo(o, null);
     }

    // InterceptorInfo can be null
    public void addBeanInstanceInfo(Object o, Object supportingInfo) {
        beanInstances.add(o);
        if( supportingInfo != null ) {
            beanSupportingInfo.put(o, supportingInfo);
        }
    }

    public Collection getBeanInstances() {
        return new HashSet(beanInstances);
    }

    public Object getSupportingInfoForBeanInstance(Object o) {
        return beanSupportingInfo.get(o);
    }

    public void clearBeanInstanceInfo(Object beanInstance) {

        beanInstances.remove(beanInstance);
        beanSupportingInfo.remove(beanInstance);

    }

    public void clearAllBeanInstanceInfo() {
        beanInstances.clear();
        beanSupportingInfo.clear();
        interceptorBuilder = null;
    }

    public Set<String> getAllInterceptorClasses() {
        Set<String> classes = new HashSet<String>();
        
        for(InterceptorDescriptor desc : classInterceptorChain) {
            classes.add(desc.getInterceptorClassName());
        }

        for(List intList : methodInterceptorsMap.values()) {
            for(Object o : intList) {
                InterceptorDescriptor interceptor = (InterceptorDescriptor) o;
                classes.add(interceptor.getInterceptorClassName());
            }
        }

        return classes;
    }

    public void setClassInterceptorChain(List<InterceptorDescriptor> chain) {
        classInterceptorChain = new LinkedList<InterceptorDescriptor>(chain);
    }

    public void setMethodLevelInterceptorChain(MethodDescriptor m, List<InterceptorDescriptor> chain) {

        methodInterceptorsMap.put(m, chain);

    }

     /**
     * Return the ordered list of AroundConstruct interceptors 
     */
    public List<InterceptorDescriptor> getAroundConstructCallbackInterceptors(Class clz, Constructor ctor) {
        LinkedList<InterceptorDescriptor> callbackInterceptors =
                new LinkedList<InterceptorDescriptor>();

        Class[] ctorParamTypes = ctor.getParameterTypes();
        String[] parameterClassNames = (new MethodDescriptor()).getParameterClassNamesFor(null, ctorParamTypes);
        MethodDescriptor mDesc = new MethodDescriptor(clz.getSimpleName(), null, 
                parameterClassNames, MethodDescriptor.EJB_BEAN);

        List<InterceptorDescriptor> interceptors = methodInterceptorsMap.get(mDesc);
        if (interceptors == null) {
            interceptors = classInterceptorChain;
        }

        for (InterceptorDescriptor next : interceptors) {
            if (next.getCallbackDescriptors(CallbackType.AROUND_CONSTRUCT).size() > 0) {
                callbackInterceptors.add(next);
            }
        }

        // There are no bean-level AroundConstruct interceptors

        return callbackInterceptors;
    }

     /**
     * Return the ordered list of interceptor info for a particular
     * callback event type.  This list *does* include the info
     * on any bean class callback.  If present, this would always be the
     * last element in the list because of the precedence defined by the spec.
     */
    public List<InterceptorDescriptor> getCallbackInterceptors(CallbackType type) {

        LinkedList<InterceptorDescriptor> callbackInterceptors =
                new LinkedList<InterceptorDescriptor>();

        for (InterceptorDescriptor next : classInterceptorChain) {
            if (next.getCallbackDescriptors(type).size() > 0) {
                callbackInterceptors.add(next);
            }
        }

        if (this.hasCallbackDescriptor(type)) {
            InterceptorDescriptor beanClassCallbackInfo = new InterceptorDescriptor();
            beanClassCallbackInfo.setFromBeanClass(true);
            beanClassCallbackInfo.addCallbackDescriptors(type,
                 this.getCallbackDescriptors(type));


            beanClassCallbackInfo.setInterceptorClassName(getBeanClassName());
            callbackInterceptors.add(beanClassCallbackInfo);
        }
      
        return callbackInterceptors;
    }

   public Set<LifecycleCallbackDescriptor> getAroundInvokeDescriptors() {
        return aroundInvokeDescs;
    }

    public void addAroundInvokeDescriptor(LifecycleCallbackDescriptor
            aroundInvokeDesc) {
        String className = aroundInvokeDesc.getLifecycleCallbackClass();
        boolean found = false;
        for (LifecycleCallbackDescriptor next :
                getAroundInvokeDescriptors()) {
            if (next.getLifecycleCallbackClass().equals(className)) {
                found = true;
                break;
            }
        }
        if (!found) {
            getAroundInvokeDescriptors().add(aroundInvokeDesc);
        }
    }

    public LifecycleCallbackDescriptor
    getAroundInvokeDescriptorByClass(String className) {

        for (LifecycleCallbackDescriptor next :
                getAroundInvokeDescriptors()) {
            if (next.getLifecycleCallbackClass().equals(className)) {
                return next;
            }
        }
        return null;
    }

    public boolean hasAroundInvokeMethod() {
        return (getAroundInvokeDescriptors().size() > 0);
    }

    /**
     * Return the ordered list of interceptor info for AroundInvoke behavior
     * of a particular business method.  This list *does* include the info
     * on any bean class interceptor.  If present, this would always be the
     * last element in the list because of the precedence defined by the spec.
     */
    public List<InterceptorDescriptor> getAroundInvokeInterceptors
            (Method m) {

        MethodDescriptor mDesc = new MethodDescriptor(m);

        // See if there's any method-level setting (either a chain
        // or a empty list ).  If not, use class-level chain
        List<InterceptorDescriptor> aroundInvokeInterceptors =
                methodInterceptorsMap.get(mDesc);

        if( aroundInvokeInterceptors == null ) {
            aroundInvokeInterceptors = new LinkedList<InterceptorDescriptor>();
            for(InterceptorDescriptor desc : classInterceptorChain) {
                if( desc.hasAroundInvokeDescriptor() ) {
                    aroundInvokeInterceptors.add(desc);
                }
            }
        }

        // Add any managed bean around invokes
        if (hasAroundInvokeMethod()) {

            EjbInterceptor interceptorInfo = new EjbInterceptor();
            interceptorInfo.setFromBeanClass(true);
            interceptorInfo.addAroundInvokeDescriptors(getAroundInvokeDescriptors());
            interceptorInfo.setInterceptorClassName(beanClassName);

            aroundInvokeInterceptors.add(interceptorInfo);
        }

        return aroundInvokeInterceptors;
    }


    public String getGlobalJndiName() {

        String appName = null;

        if (enclosingBundle == null)
          return null;

        Application app = enclosingBundle.getApplication();
        if ( !app.isVirtual()  ) {
            appName = enclosingBundle.getApplication().getAppName();
        }

        String modName = enclosingBundle.getModuleDescriptor().getModuleName();

        StringBuffer javaGlobalPrefix = new StringBuffer("java:global/");

        if (appName != null) {
            javaGlobalPrefix.append(appName);
            javaGlobalPrefix.append("/");
        }

        javaGlobalPrefix.append(modName);
        javaGlobalPrefix.append("/");


        // If the managed bean is named, use the name for the final component
        // of the managed bean global name.  Otherwise, use a derived internal
        // name since we'll still need a way to register and lookup the bean
        // from within the container.

        String componentName = isNamed() ? name :
                "___internal_managed_bean_" + beanClassName;
        javaGlobalPrefix.append(componentName);


        return javaGlobalPrefix.toString();        
    }

    public String getAppJndiName() {

        if (enclosingBundle == null)
            return null;

        String modName = enclosingBundle.getModuleDescriptor().getModuleName();

        StringBuffer javaAppPrefix = new StringBuffer("java:app/");

        javaAppPrefix.append(modName);
        javaAppPrefix.append("/");


        // If the managed bean is named, use the name for the final component
        // of the managed bean global name.  Otherwise, use a derived internal
        // name since we'll still need a way to register and lookup the bean
        // from within the container.

        String componentName = isNamed() ? name :
                "___internal_managed_bean_" + beanClassName;
        javaAppPrefix.append(componentName);


        return javaAppPrefix.toString();
               
    }


	/**
	* Returns a formatted String of the attributes of this object.
	*/
    public void print(StringBuffer toStringBuffer) {

	// toStringBuffer.append("\n homeClassName ").append(homeClassName);

    }

    public void validate() {

        visit((ManagedBeanVisitor)new ApplicationValidator());
           
    }

    public void visit(ManagedBeanVisitor aVisitor) {
        aVisitor.accept(this);
    }

    @Override
    public List<InjectionCapable> getInjectableResourcesByClass(String className) {

        List<InjectionCapable> injectables = new LinkedList<InjectionCapable>();

        for (Iterator envEntryItr = getEnvironmentProperties().iterator();
             envEntryItr.hasNext();) {
            EnvironmentProperty envEntry = (EnvironmentProperty)
                    envEntryItr.next();
            // Only env-entries that have been assigned a value are
            // eligible for injection.
            if (envEntry.hasAValue()) {
                injectables.add(envEntry);
            }
        }

        injectables.addAll(getEjbReferenceDescriptors());
        injectables.addAll(getServiceReferenceDescriptors());
        injectables.addAll(getResourceReferenceDescriptors());
        injectables.addAll(getResourceEnvReferenceDescriptors());
        injectables.addAll(getMessageDestinationReferenceDescriptors());

        injectables.addAll(getEntityManagerFactoryReferenceDescriptors());
        injectables.addAll(getEntityManagerReferenceDescriptors());

        List<InjectionCapable> injectablesByClass =
                new LinkedList<InjectionCapable>();

        for (InjectionCapable next : injectables ) {
            if (next.isInjectable()) {
                for (InjectionTarget target : next.getInjectionTargets()) {
                    if (target.getClassName().equals(className)) {
                        injectablesByClass.add(next);
                    }
                }
            }
        }

        return injectablesByClass;
    }

    @Override
    public InjectionInfo getInjectionInfoByClass(Class clazz) {

        // TODO This is invariant data so we could cache it

        String className = clazz.getName();

        LifecycleCallbackDescriptor postConstructDesc =
                getPostConstructDescriptorByClass(className);
        String postConstructMethodName = (postConstructDesc != null) ?
                postConstructDesc.getLifecycleCallbackMethod() : null;
        LifecycleCallbackDescriptor preDestroyDesc =
                getPreDestroyDescriptorByClass(className);
        String preDestroyMethodName = (preDestroyDesc != null) ?
                preDestroyDesc.getLifecycleCallbackMethod() : null;
        InjectionInfo injectionInfo = new InjectionInfo(className,
                postConstructMethodName, preDestroyMethodName,
                getInjectableResourcesByClass(className));

        return injectionInfo;
    }

    @Override
    public LifecycleCallbackDescriptor getPostConstructDescriptorByClass(String className) {
        for (LifecycleCallbackDescriptor next : getPostConstructDescriptors()) {
            if (next.getLifecycleCallbackClass().equals(className)) {
                return next;
            }
        }
        return null;
    }

    @Override
    public LifecycleCallbackDescriptor getPreDestroyDescriptorByClass(String className) {
        for (LifecycleCallbackDescriptor next : getPreDestroyDescriptors()) {
            if (next.getLifecycleCallbackClass().equals(className)) {
                return next;
            }
        }
        return null;
    }


}
    

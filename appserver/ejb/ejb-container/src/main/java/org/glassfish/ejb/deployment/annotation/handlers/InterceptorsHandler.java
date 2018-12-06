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
// Portions Copyright [2018] [Payara Foundation and/or its affiliates]
package org.glassfish.ejb.deployment.annotation.handlers;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;
import javax.interceptor.AroundInvoke;
import javax.interceptor.AroundTimeout;
import javax.interceptor.Interceptors;

import com.sun.enterprise.deployment.EjbInterceptor;
import com.sun.enterprise.deployment.LifecycleCallbackDescriptor;
import com.sun.enterprise.deployment.MethodDescriptor;
import com.sun.enterprise.deployment.annotation.context.EjbContext;
import com.sun.enterprise.deployment.annotation.context.EjbInterceptorContext;
import org.glassfish.apf.AnnotationHandlerFor;
import org.glassfish.apf.AnnotationInfo;
import org.glassfish.apf.AnnotationProcessorException;
import org.glassfish.apf.HandlerProcessingResult;
import org.glassfish.apf.ProcessingContext;
import org.glassfish.apf.impl.ComponentDefinition;
import org.glassfish.ejb.deployment.descriptor.EjbBundleDescriptorImpl;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.InterceptorBindingDescriptor;
import org.jvnet.hk2.annotations.Service;

import static com.sun.enterprise.deployment.LifecycleCallbackDescriptor.CallbackType;

/**
 * This handler is responsible for handling javax.ejb.Interceptors
 *
 */
@Service
@AnnotationHandlerFor(Interceptors.class)
public class InterceptorsHandler extends AbstractAttributeHandler {
    
    public InterceptorsHandler() {
    }

    protected boolean supportTypeInheritance() {
        return true;
    }

    protected HandlerProcessingResult processAnnotation(AnnotationInfo ainfo,
            EjbContext[] ejbContexts) throws AnnotationProcessorException {

        Interceptors interceptors = (Interceptors) ainfo.getAnnotation();

        
        EjbBundleDescriptorImpl ejbBundle =
            ((EjbDescriptor)ejbContexts[0].getDescriptor()).
                getEjbBundleDescriptor();
        
        // Process each of the interceptor classes.
        for(Class interceptor : interceptors.value()) {
            processInterceptorClass(interceptor, ejbBundle, ainfo);
        }

        for(EjbContext next : ejbContexts) {

            EjbDescriptor ejbDescriptor = (EjbDescriptor) next.getDescriptor();

            // Create binding information.  
            InterceptorBindingDescriptor binding = 
                new InterceptorBindingDescriptor();

            binding.setEjbName(ejbDescriptor.getName());

            for(Class interceptor : interceptors.value()) {
                binding.appendInterceptorClass(interceptor.getName());
            }
            
            if(ElementType.METHOD.equals(ainfo.getElementType())) {
                Method m = (Method) ainfo.getAnnotatedElement();
                MethodDescriptor md = 
                    new MethodDescriptor(m, MethodDescriptor.EJB_BEAN);
                binding.setBusinessMethod(md);
            } else if(ElementType.CONSTRUCTOR.equals(ainfo.getElementType())) {
                                Constructor c = (Constructor) ainfo.getAnnotatedElement();
                Class cl = c.getDeclaringClass();
                Class[] ctorParamTypes = c.getParameterTypes();
                String[] parameterClassNames = (new MethodDescriptor()).getParameterClassNamesFor(null, ctorParamTypes);
                MethodDescriptor md = new MethodDescriptor(cl.getSimpleName(), null,
                        parameterClassNames, MethodDescriptor.EJB_BEAN);
                binding.setBusinessMethod(md);
            }

            // All binding information processed from annotations should go
            // before the binding information processed from the descriptors.
            // Since descriptors are processed first, always place the binding
            // info at the front.  The binding information from the descriptor
            // is ordered, but there is no prescribed order in which the 
            // annotations are processed, so all that matters is that it's
            // before the descriptor bindings and that the descriptor binding
            // order is preserved.
            ejbBundle.prependInterceptorBinding(binding);
        }

        return getDefaultProcessedResult();
    }

    private void processInterceptorClass(Class interceptorClass,
            EjbBundleDescriptorImpl ejbBundle, AnnotationInfo ainfo)
        throws AnnotationProcessorException {
                
        Set<LifecycleCallbackDescriptor> aroundInvokeDescriptors =
            new HashSet<LifecycleCallbackDescriptor>();
        Set<LifecycleCallbackDescriptor> aroundTimeoutDescriptors =
            new HashSet<LifecycleCallbackDescriptor>();
        Set<LifecycleCallbackDescriptor> postActivateDescriptors =
            new HashSet<LifecycleCallbackDescriptor>();
        Set<LifecycleCallbackDescriptor> prePassivateDescriptors =
            new HashSet<LifecycleCallbackDescriptor>();
        
        ComponentDefinition cdef = new ComponentDefinition(interceptorClass);
        for(Method m : cdef.getMethods()) {
            if( m.getAnnotation(AroundInvoke.class) != null ) {
                aroundInvokeDescriptors.add(getLifecycleCallbackDescriptor(m, true));
            }
            if( m.getAnnotation(AroundTimeout.class) != null ) {
                aroundTimeoutDescriptors.add(getLifecycleCallbackDescriptor(m, true));
            }
            if( m.getAnnotation(PostActivate.class) != null ) {
                postActivateDescriptors.add(getLifecycleCallbackDescriptor(m, false));
            }
            if( m.getAnnotation(PrePassivate.class) != null ) {
                prePassivateDescriptors.add(getLifecycleCallbackDescriptor(m, false));
            }
        }
        
        EjbInterceptor interceptor =
            ejbBundle.getInterceptorByClassName(interceptorClass.getName());
        if (interceptor == null) {
            interceptor = new EjbInterceptor();
            interceptor.setInterceptorClassName(interceptorClass.getName());
            // Add interceptor to the set of all interceptors in the ejb-jar
            ejbBundle.addInterceptor(interceptor);
        }
        
        if (aroundInvokeDescriptors.size() > 0) {
            interceptor.addAroundInvokeDescriptors(aroundInvokeDescriptors);
        }
        
        if (aroundTimeoutDescriptors.size() > 0) {
            interceptor.addAroundTimeoutDescriptors(aroundTimeoutDescriptors);
        }
        
        if (postActivateDescriptors.size() > 0) {
            interceptor.addCallbackDescriptors(CallbackType.POST_ACTIVATE,
                postActivateDescriptors);
        }
        
        if (prePassivateDescriptors.size() > 0) {
            interceptor.addCallbackDescriptors(CallbackType.PRE_PASSIVATE,
                prePassivateDescriptors);
        }

        // process resource related annotations
        EjbInterceptorContext ejbInterceptorContext =
            new EjbInterceptorContext(interceptor);
        ProcessingContext procContext = ainfo.getProcessingContext();
        procContext.pushHandler(ejbInterceptorContext);
        procContext.getProcessor().process(
            procContext, new Class[] { interceptorClass });
        return;
    }

    /**
     * @return an array of annotation types this annotation handler would 
     * require to be processed (if present) before it processes it's own 
     * annotation type.
     */
    public Class<? extends Annotation>[] getTypeDependencies() {
        return getEjbAnnotationTypes();
    }

    private LifecycleCallbackDescriptor getLifecycleCallbackDescriptor(Method m, boolean requiresInvocationContextArgument) {
        LifecycleCallbackDescriptor lccDesc = new LifecycleCallbackDescriptor();
        lccDesc.setLifecycleCallbackClass(m.getDeclaringClass().getName());
        lccDesc.setLifecycleCallbackMethod(m.getName());
        lccDesc.setRequiresInvocationContextArgument(requiresInvocationContextArgument);
        return lccDesc;
    }
}

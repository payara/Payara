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
// Portions Copyright [2018-2026] Payara Foundation and/or its affiliates
package org.glassfish.ejb.deployment.annotation.handlers;

import com.sun.enterprise.deployment.EjbInterceptor;
import com.sun.enterprise.deployment.LifecycleCallbackDescriptor;
import com.sun.enterprise.deployment.MethodDescriptor;
import com.sun.enterprise.deployment.annotation.context.EjbContext;
import com.sun.enterprise.deployment.annotation.context.EjbInterceptorContext;
import jakarta.ejb.PostActivate;
import jakarta.ejb.PrePassivate;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.AroundTimeout;
import jakarta.interceptor.Interceptors;
import org.glassfish.apf.AnnotationHandlerFor;
import org.glassfish.apf.AnnotationInfo;
import org.glassfish.apf.AnnotationProcessorException;
import org.glassfish.apf.HandlerProcessingResult;
import org.glassfish.apf.ProcessingContext;
import org.glassfish.apf.impl.ComponentDefinition;
import org.glassfish.ejb.deployment.descriptor.EjbBundleDescriptorImpl;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.InterceptorBindingDescriptor;
import org.glassfish.internal.deployment.ClassHierarchy;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jvnet.hk2.annotations.Service;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.sun.enterprise.deployment.LifecycleCallbackDescriptor.CallbackType;

/**
 * This handler is responsible for handling the jakarta.interceptor.Interceptors annotation.
 */
@Service
@AnnotationHandlerFor(Interceptors.class)
public class InterceptorsHandler extends AbstractAttributeHandler {

    private static final DotName AROUND_INVOKE = DotName.createSimple(AroundInvoke.class);
    private static final DotName AROUND_TIMEOUT = DotName.createSimple(AroundTimeout.class);
    private static final DotName POST_ACTIVATE = DotName.createSimple(PostActivate.class);
    private static final DotName PRE_PASSIVATE = DotName.createSimple(PrePassivate.class);

    public InterceptorsHandler() {
    }

    @Override
    protected boolean supportTypeInheritance() {
        return true;
    }

    @Override
    protected HandlerProcessingResult processAnnotation(AnnotationInfo ainfo,
            EjbContext[] ejbContexts) throws AnnotationProcessorException {

        IndexView index = ainfo.getIndex();
        List<ClassInfo> interceptors = new ArrayList<>();
        for (String interceptorName : ainfo.classArrayValue("value")) {
            ClassInfo interceptor = index.getClassByName(DotName.createSimple(interceptorName));
            if (interceptor == null) {
                throw new AnnotationProcessorException("Interceptor class " + interceptorName
                        + " is neither in the application nor visible to its class loader", ainfo);
            }
            interceptors.add(interceptor);
        }

        EjbBundleDescriptorImpl ejbBundle =
            ((EjbDescriptor)ejbContexts[0].getDescriptor()).getEjbBundleDescriptor();

        // Process each of the interceptor classes.
        for(ClassInfo interceptor : interceptors) {
            processInterceptorClass(interceptor, ejbBundle, ainfo);
        }

        for(EjbContext next : ejbContexts) {

            EjbDescriptor ejbDescriptor = (EjbDescriptor) next.getDescriptor();

            // Create binding information.
            InterceptorBindingDescriptor binding = new InterceptorBindingDescriptor();

            binding.setEjbName(ejbDescriptor.getName());

            for(ClassInfo interceptor : interceptors) {
                binding.appendInterceptorClass(interceptor.name().toString());
            }

            if(ElementType.METHOD.equals(ainfo.getElementType())) {
                MethodInfo m = ainfo.getAnnotatedElement().asMethod();
                MethodDescriptor md = new MethodDescriptor(m, MethodDescriptor.EJB_BEAN);
                binding.setBusinessMethod(md);
            } else if(ElementType.CONSTRUCTOR.equals(ainfo.getElementType())) {
                MethodInfo c = ainfo.getAnnotatedElement().asMethod();
                MethodDescriptor md = new MethodDescriptor(ClassHierarchy.simpleName(c.declaringClass()), null,
                        ClassHierarchy.sourceParameterNames(c), MethodDescriptor.EJB_BEAN);
                binding.setBusinessMethod(md);
            }

            // All binding information processed from annotations should go
            // before the binding information processed from the descriptors
            // since descriptors override annotations.
            ejbBundle.prependInterceptorBinding(binding);
        }

        return getDefaultProcessedResult();
    }

    private void processInterceptorClass(ClassInfo interceptorClass, EjbBundleDescriptorImpl ejbBundle,
                                         AnnotationInfo ainfo) throws AnnotationProcessorException {

        Set<LifecycleCallbackDescriptor> aroundInvokeDescriptors = new HashSet<>();
        Set<LifecycleCallbackDescriptor> aroundTimeoutDescriptors = new HashSet<>();
        Set<LifecycleCallbackDescriptor> postActivateDescriptors = new HashSet<>();
        Set<LifecycleCallbackDescriptor> prePassivateDescriptors = new HashSet<>();

        ComponentDefinition cdef = new ComponentDefinition(interceptorClass, ainfo.getIndex());
        for(MethodInfo m : cdef.getMethods()) {
            if( m.hasDeclaredAnnotation(AROUND_INVOKE) ) {
                aroundInvokeDescriptors.add(getLifecycleCallbackDescriptor(m, true));
            }
            if( m.hasDeclaredAnnotation(AROUND_TIMEOUT) ) {
                aroundTimeoutDescriptors.add(getLifecycleCallbackDescriptor(m, true));
            }
            if( m.hasDeclaredAnnotation(POST_ACTIVATE) ) {
                postActivateDescriptors.add(getLifecycleCallbackDescriptor(m, false));
            }
            if( m.hasDeclaredAnnotation(PRE_PASSIVATE) ) {
                prePassivateDescriptors.add(getLifecycleCallbackDescriptor(m, false));
            }
        }

        String interceptorClassName = interceptorClass.name().toString();
        EjbInterceptor interceptor = ejbBundle.getInterceptorByClassName(interceptorClassName);
        if (interceptor == null) {
            interceptor = new EjbInterceptor();
            interceptor.setInterceptorClassName(interceptorClassName);
            // Add interceptor to the set of all interceptors in the ejb-jar.
            ejbBundle.addInterceptor(interceptor);
        }

        if (aroundInvokeDescriptors.size() > 0) {
            interceptor.addAroundInvokeDescriptors(aroundInvokeDescriptors);
        }

        if (aroundTimeoutDescriptors.size() > 0) {
            interceptor.addAroundTimeoutDescriptors(aroundTimeoutDescriptors);
        }

        if (postActivateDescriptors.size() > 0) {
            interceptor.addCallbackDescriptors(CallbackType.POST_ACTIVATE, postActivateDescriptors);
        }

        if (prePassivateDescriptors.size() > 0) {
            interceptor.addCallbackDescriptors(CallbackType.PRE_PASSIVATE, prePassivateDescriptors);
        }

        // process resource related annotations
        EjbInterceptorContext ejbInterceptorContext = new EjbInterceptorContext(interceptor);
        ProcessingContext procContext = ainfo.getProcessingContext();
        procContext.pushHandler(ejbInterceptorContext);
        procContext.getProcessor().process(procContext, List.of(interceptorClass));
    }

    /**
     * @return an array of annotation types this annotation handler would
     * require to be processed (if present) before it processes it's own
     * annotation type.
     */
    @Override
    public Class<? extends Annotation>[] getTypeDependencies() {
        return getEjbAnnotationTypes();
    }

    private LifecycleCallbackDescriptor getLifecycleCallbackDescriptor(MethodInfo m, boolean requiresInvocationContextArgument) {
        LifecycleCallbackDescriptor lccDesc = new LifecycleCallbackDescriptor();
        lccDesc.setLifecycleCallbackClass(m.declaringClass().name().toString());
        lccDesc.setLifecycleCallbackMethod(m.name());
        lccDesc.setRequiresInvocationContextArgument(requiresInvocationContextArgument);
        return lccDesc;
    }
}

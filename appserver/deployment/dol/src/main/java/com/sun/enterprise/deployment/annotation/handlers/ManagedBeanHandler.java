/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.deployment.annotation.handlers;

import com.sun.enterprise.deployment.*;
import com.sun.enterprise.deployment.annotation.context.ResourceContainerContext;
import com.sun.enterprise.deployment.annotation.context.ManagedBeanContext;
import com.sun.enterprise.deployment.util.TypeUtil;
import org.glassfish.apf.*;
import org.glassfish.internal.api.Globals;
import org.jvnet.hk2.annotations.Service;

import javax.annotation.ManagedBean;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Method;

import java.util.List;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.Map;
import java.util.HashMap;

@Service
public class ManagedBeanHandler extends AbstractHandler {


    public ManagedBeanHandler() {
    }

    /**
     * @return the annoation type this annotation handler is handling
     */
    public Class<? extends Annotation> getAnnotationType() {
        return ManagedBean.class;
    }

    public HandlerProcessingResult processAnnotation(AnnotationInfo element)
        throws AnnotationProcessorException {


        AnnotatedElementHandler aeHandler = element.getProcessingContext().getHandler();
        if( aeHandler instanceof ManagedBeanContext ) {

            // Ignore @ManagedBean processing during ManagedBean class processing itself
            return getDefaultProcessedResult();
        }

        ManagedBeanDescriptor managedBeanDesc = new ManagedBeanDescriptor();

        ManagedBean resourceAn = (ManagedBean) element.getAnnotation();

        // name() is optional
        String logicalName = resourceAn.value();
        if( !logicalName.equals("")) {
            managedBeanDesc.setName(logicalName);
        }

        Class managedBeanClass = (Class) element.getAnnotatedElement();

        managedBeanDesc.setBeanClassName(managedBeanClass.getName());


        Class[] classInterceptors = null;
        Map<Method, Class[]> methodLevelInterceptors = new HashMap<Method, Class[]>();
        Map<String, InterceptorDescriptor> interceptorDescs = new HashMap<String, InterceptorDescriptor>();


        // For now, just process the javax.interceptor related annotations directly instead
        // of relying on the annotation framework.   All the existing javax.interceptor
        // handlers are very tightly coupled to ejb so it would be more work to abstract those
        // than to just process the annotations directly.  Also, do javax.interceptor
        // annotation processing reflectively to avoid dependency on javax.interceptor from
        // DOL module.

        // TODO refactor javax.interceptor annotation handlers to support both ejb and non-ejb
        // related interceptors

        Annotation interceptorsAnn = getClassAnnotation(managedBeanClass, "javax.interceptor.Interceptors");
        if( interceptorsAnn != null ) {
            try {
                Method m = interceptorsAnn.annotationType().getDeclaredMethod("value");
                classInterceptors = (Class[]) m.invoke(interceptorsAnn);
            } catch(Exception e) {
                AnnotationProcessorException ape = new AnnotationProcessorException(e.getMessage(), element);
                ape.initCause(e);
                throw ape;
            }
        }

        Class nextIntClass = managedBeanClass;
        while(nextIntClass != Object.class) {
            Method managedBeanAroundInvoke =
                getMethodForMethodAnnotation(nextIntClass, "javax.interceptor.AroundInvoke");

            if( (managedBeanAroundInvoke != null) && !(methodOverridden(managedBeanAroundInvoke,
                    nextIntClass, managedBeanClass)) ) {

                LifecycleCallbackDescriptor desc = new LifecycleCallbackDescriptor();
                desc.setLifecycleCallbackClass(nextIntClass.getName());
                desc.setLifecycleCallbackMethod(managedBeanAroundInvoke.getName());
                managedBeanDesc.addAroundInvokeDescriptor(desc);
            }

            nextIntClass = nextIntClass.getSuperclass();
        }


        for(Method m : managedBeanClass.getMethods()) {
            Annotation ann = getMethodAnnotation(m, "javax.interceptor.Interceptors");
            if(ann != null) {
                try {
                    Method valueM = ann.annotationType().getDeclaredMethod("value");
                    methodLevelInterceptors.put(m, (Class[]) valueM.invoke(ann));
                } catch(Exception e) {
                    AnnotationProcessorException ape = new AnnotationProcessorException(e.getMessage(), element);
                    ape.initCause(e);
                    throw ape;
                }
            } else {
                // If the method doesn't declare any method-level interceptors but it excludes
                // class-level interceptors, explicitly set method-level to an empty list.
                boolean excludeClassInterceptors =
                        ( getMethodAnnotation(m, "javax.interceptor.ExcludeClassInterceptors")
                            != null );
                if( excludeClassInterceptors ) {
                    MethodDescriptor mDesc = new MethodDescriptor(m);
                    managedBeanDesc.setMethodLevelInterceptorChain(mDesc,
                            new LinkedList<InterceptorDescriptor>());
                }

            }

        }


        if( aeHandler instanceof ResourceContainerContext ) {
            ((ResourceContainerContext) aeHandler).addManagedBean(managedBeanDesc);            


            // process managed bean class annotations
            ManagedBeanContext managedBeanContext =
                new ManagedBeanContext(managedBeanDesc);
            ProcessingContext procContext = element.getProcessingContext();
            procContext.pushHandler(managedBeanContext);

            procContext.getProcessor().process(
                procContext, new Class[] { managedBeanClass });

            List<InterceptorDescriptor> classInterceptorChain = new LinkedList<InterceptorDescriptor>();

            if( classInterceptors != null ) {

                for(Class i : classInterceptors) {

                    InterceptorDescriptor nextInterceptor = processInterceptor(i, managedBeanContext,
                            procContext);

                    // Add interceptor to class-level chain
                    classInterceptorChain.add(nextInterceptor);

                    interceptorDescs.put(i.getName(), nextInterceptor);

                }
                
                managedBeanDesc.setClassInterceptorChain(classInterceptorChain);

            }

            for(Map.Entry<Method, Class[]> next : methodLevelInterceptors.entrySet()) {

                Method m = next.getKey();
                Class[] interceptors = next.getValue();

                boolean excludeClassInterceptors =
                        ( getMethodAnnotation(m, "javax.interceptor.ExcludeClassInterceptors")
                            != null );

                List<InterceptorDescriptor> methodInterceptorChain = excludeClassInterceptors ?
                        new LinkedList<InterceptorDescriptor>() :
                        new LinkedList<InterceptorDescriptor>(classInterceptorChain);


                for(Class nextInterceptor : interceptors) {
                    InterceptorDescriptor interceptorDesc = interceptorDescs.get(nextInterceptor.getName());
                    if( interceptorDesc == null ) {
                        interceptorDesc = processInterceptor(nextInterceptor, managedBeanContext,
                                procContext);
                        interceptorDescs.put(nextInterceptor.getName(), interceptorDesc);
                    }
                    methodInterceptorChain.add(interceptorDesc);
                }

                MethodDescriptor mDesc = new MethodDescriptor(m);
                managedBeanDesc.setMethodLevelInterceptorChain(mDesc, methodInterceptorChain);
            }
           
        }

        return getDefaultProcessedResult();
    }

    private InterceptorDescriptor processInterceptor(Class interceptorClass, ManagedBeanContext managedBeanCtx,
                                                     ProcessingContext procCtx)
        throws AnnotationProcessorException {

        InterceptorDescriptor interceptorDesc = new InterceptorDescriptor();
        interceptorDesc.setInterceptorClassName(interceptorClass.getName());

        // Redirect PostConstruct / PreDestroy methods to InterceptorDescriptor
        // during annotation processing
        managedBeanCtx.setInterceptorMode(interceptorDesc);

        // Process annotations on interceptor
        procCtx.pushHandler(managedBeanCtx);
        procCtx.getProcessor().process(procCtx, new Class[] {interceptorClass});


        managedBeanCtx.unsetInterceptorMode();

        Class nextIntClass = interceptorClass;
        while(nextIntClass != Object.class) {
            Method interceptorAroundInvoke =
                getMethodForMethodAnnotation(nextIntClass, "javax.interceptor.AroundInvoke");
            if( (interceptorAroundInvoke != null) && !(methodOverridden(interceptorAroundInvoke,
                    nextIntClass, interceptorClass)) ) {

                LifecycleCallbackDescriptor desc = new LifecycleCallbackDescriptor();
                desc.setLifecycleCallbackClass(nextIntClass.getName());
                desc.setLifecycleCallbackMethod(interceptorAroundInvoke.getName());
                interceptorDesc.addAroundInvokeDescriptor(desc);
            }

            nextIntClass = nextIntClass.getSuperclass();
        }


        return interceptorDesc;
    }

    private Annotation getClassAnnotation(Class c, String annotationClassName) {
        for(Annotation next : c.getDeclaredAnnotations()) {

            if( next.annotationType().getName().equals(annotationClassName)) {
                return next;
            }
        }
        return null;
    }

    private Method getMethodForMethodAnnotation(Class c, String annotationClassName) {
        for(Method m : c.getDeclaredMethods()) {
            for(Annotation next : m.getDeclaredAnnotations()) {

                if( next.annotationType().getName().equals(annotationClassName)) {
                    return m;
                }
            }
        }
        return null;
    }

    private Annotation getMethodAnnotation(Method m, String annotationClassName) {

        for(Annotation next : m.getDeclaredAnnotations()) {

            if( next.annotationType().getName().equals(annotationClassName)) {
                return next;
            }
        }

        return null;
    }


    private boolean methodOverridden(Method m, Class declaringSuperClass, Class leafClass) {

        Class nextClass = leafClass;
        boolean overridden = false;

        if( Modifier.isPrivate(m.getModifiers())) {
            return false;
        }

        while((nextClass != declaringSuperClass) && (nextClass != Object.class)) {

            for(Method nextMethod : nextClass.getDeclaredMethods()) {

                if( !Modifier.isPrivate(nextMethod.getModifiers()) &&
                    TypeUtil.sameMethodSignature(m, nextMethod) ) {
                    overridden = true;
                    break;
                }
            }

            if( overridden ) {
                break;
            }

            nextClass = nextClass.getSuperclass();
        }

        return overridden;
    }

}

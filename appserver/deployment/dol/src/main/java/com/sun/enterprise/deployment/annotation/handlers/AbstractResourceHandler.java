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
package com.sun.enterprise.deployment.annotation.handlers;

import com.sun.enterprise.deployment.EjbBundleDescriptor;
import com.sun.enterprise.deployment.annotation.context.EjbBundleContext;
import com.sun.enterprise.deployment.annotation.context.EjbsContext;
import com.sun.enterprise.deployment.annotation.context.ResourceContainerContext;
import com.sun.enterprise.deployment.annotation.context.WebComponentsContext;
import org.glassfish.apf.AnnotatedElementHandler;
import org.glassfish.apf.AnnotationInfo;
import org.glassfish.apf.AnnotationProcessorException;
import org.glassfish.apf.HandlerProcessingResult;
import org.glassfish.deployment.common.RootDeploymentDescriptor;
import org.glassfish.internal.deployment.ClassHierarchy;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import java.lang.annotation.Annotation;
import java.util.Collection;

/**
 * This is an abstract class encapsulate generic behaviour of resource
 * annotation.
 * Concrete subclass handlers need to implement the following:
 *     public Class&lt;? extends Annotation&gt; getAnnotationType();
 *     protected HandlerProcessingResult processAnnotation(
 *             AnnotationInfo ainfo,
 *             ResourceContainerContext[] rcContexts)
 *             throws AnnotationProcessorException;
 * It may also need to override the following if other annotations
 * need to be processed prior to given annotation:
 *     public Class&lt;? extends Annotation&gt;[] getTypeDependencies();
 *
 * @author Shing Wai Chan
 */
public abstract class AbstractResourceHandler extends AbstractHandler {
    /**
     * Process Annotation with given ResourceContainerContexts.
     * @param ainfo
     * @param rcContexts
     * @return HandlerProcessingResult
     */
    protected abstract HandlerProcessingResult processAnnotation(
            AnnotationInfo ainfo,
            ResourceContainerContext[] rcContexts)
            throws AnnotationProcessorException;

    /**
     * Process a particular annotation which type is the same as the
     * one returned by @see getAnnotationType(). All information
     * pertinent to the annotation and its context is encapsulated
     * in the passed AnnotationInfo instance.
     *
     * @param ainfo the annotation information
     */
    @Override
    public HandlerProcessingResult processAnnotation(AnnotationInfo ainfo)
            throws AnnotationProcessorException {

        AnnotatedElementHandler aeHandler = ainfo.getProcessingContext().getHandler();
        if (aeHandler instanceof EjbBundleContext) {
            EjbBundleContext ejbBundleContext = (EjbBundleContext)aeHandler;
            aeHandler = ejbBundleContext.createContextForEjb();
            if (aeHandler == null) {
                aeHandler = ejbBundleContext.createContextForEjbInterceptor();
            }

            // If it's still null and we're in an ejb-jar, use the EjbBundleContext.
            // This way we process dependencies on any classes (other than ejbs ,
            // interceptors, and their super-classes) that have annotations in case
            // we need the info for managed classes we wouldn't normally know about
            // (e.g. 299 classes).  In a .war, those are already processed during
            // the .war annotation scanning.

            EjbBundleDescriptor bundleDesc = ejbBundleContext.getDescriptor();
            RootDeploymentDescriptor enclosingBundle = bundleDesc.getModuleDescriptor().getDescriptor();

            boolean ejbJar = enclosingBundle instanceof EjbBundleDescriptor;

            if( (aeHandler == null) && ejbJar ) {
                aeHandler = ejbBundleContext;
            }
        }

        if (aeHandler == null) {
            // no such ejb
            return getInvalidAnnotatedElementHandlerResult(
                ainfo.getProcessingContext().getHandler(), ainfo);
        }
        ResourceContainerContext[] rcContexts = null;
        if (aeHandler instanceof EjbsContext) {
            EjbsContext ejbsContext = (EjbsContext)aeHandler;
            rcContexts = (ResourceContainerContext[])ejbsContext.getEjbContexts();
        } else if (aeHandler instanceof WebComponentsContext) {
            WebComponentsContext webCompsContext = (WebComponentsContext)aeHandler;
            rcContexts = (ResourceContainerContext[])webCompsContext.getWebComponentContexts();
        } else if (aeHandler instanceof ResourceContainerContext) {
            rcContexts = new ResourceContainerContext[] {
                    (ResourceContainerContext)aeHandler };
        } else {
            return getInvalidAnnotatedElementHandlerResult(aeHandler, ainfo);
        }

        return processAnnotation(ainfo, rcContexts);
    }

    /**
     * @return an array of annotation types this annotation handler would
     * require to be processed (if present) before it processes it's own
     * annotation type.
     */
    @Override
    public Class<? extends Annotation>[] getTypeDependencies() {
        return getEjbAndWebAnnotationTypes();
    }

    protected boolean isAEjbComponentClass(Collection<AnnotationInstance> annotations) {
        return hasAnyOf(annotations, getEjbAnnotationTypes());
    }

    protected boolean isAWebComponentClass(Collection<AnnotationInstance> annotations) {
        return hasAnyOf(annotations, getWebAnnotationTypes());
    }

    private static boolean hasAnyOf(Collection<AnnotationInstance> annotations, Class<? extends Annotation>[] types) {
        for (AnnotationInstance annotation : annotations) {
            for (Class<? extends Annotation> type : types) {
                if (annotation.name().toString().equals(type.getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks a lifecycle callback method against the rules of the specification: at most one
     * parameter, and when it has none, no checked exception and a void return type.
     *
     * @param index resolves the exception types
     */
    protected void validateAnnotatedLifecycleMethod(MethodInfo method, IndexView index) {
        if (method.parametersCount() > 1) {
            throw new IllegalArgumentException(localStrings.getLocalString("lifecycle_method_invalid_param_size",
                    "The lifecycle method [{0}] must not have more than one parameter", method.name()));
        }

        if (method.parametersCount() == 0) {
            for (Type exception : method.exceptions()) {
                if (!ClassHierarchy.isSubtype(index, exception, RuntimeException.class)) {
                    throw new IllegalArgumentException(localStrings.getLocalString("lifecycle_method_no_checked_exception",
                            "The lifecycle method [{0}] must not throw a checked exception", method.name()));
                }
            }
            if (method.returnType().kind() != Type.Kind.VOID) {
                throw new IllegalArgumentException(localStrings.getLocalString("lifecycle_method_return_type_void",
                        "The return type of the lifecycle method [{0}] must be void", method.name()));
            }
        }
    }
}

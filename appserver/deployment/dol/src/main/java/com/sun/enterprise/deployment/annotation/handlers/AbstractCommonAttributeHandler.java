/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.deployment.EjbDescriptor;
import com.sun.enterprise.deployment.MethodDescriptor;
import com.sun.enterprise.deployment.WebComponentDescriptor;
import com.sun.enterprise.deployment.annotation.context.*;
import com.sun.enterprise.deployment.web.SecurityConstraint;
import org.glassfish.apf.AnnotatedElementHandler;
import org.glassfish.apf.AnnotationInfo;
import org.glassfish.apf.AnnotationProcessorException;
import org.glassfish.apf.HandlerProcessingResult;

import java.lang.annotation.ElementType;
import java.util.logging.Level;

/**
 * This is an abstract class encapsulate generic behaviour of annotation
 * handler applying on Ejb and WebComponent Class. It will get the corresponding
 * EjbDescriptors or WebComponentDescriptor associated to the annotation on
 * the given Class and then pass it to underlying processAnnotation method.
 * Concrete subclass handlers need to implement the following:
 *     public Class&lt;? extends Annotation&gt; getAnnotationType();
 *     protected HandlerProcessingResult processAnnotation(AnnotationInfo ainfo,
 *             EjbContext[] ejbContexts) throws AnnotationProcessorException;
 *     protected HandlerProcessingResult processAnnotation(AnnotationInfo ainfo,
 *             WebComponentContext[] webCompContexts)
 *             throws AnnotationProcessorException;
 *     protected HandlerProcessingResult processAnnotation(AnnotationInfo ainfo,
 *             WebBundleContext webBundleContext)
 *             throws AnnotationProcessorException;
 * It may also need to override the following if other annotations
 * need to be processed prior to given annotation:
 *     public Class&lt;? extends Annotation&gt;[] getTypeDependencies();
 *
 * @author Shing Wai Chan
 */
public abstract class AbstractCommonAttributeHandler extends AbstractHandler {
    /**
     * Process Annotation with given EjbContexts.
     * @param ainfo
     * @param ejbContexts
     * @return HandlerProcessingResult
     */
    protected abstract HandlerProcessingResult processAnnotation(
            AnnotationInfo ainfo, EjbContext[] ejbContexts)
            throws AnnotationProcessorException;

    /**
     * Process Annotation with given WebCompContexts.
     * @param ainfo
     * @param webCompContexts
     * @return HandlerProcessingResult
     */
    protected abstract HandlerProcessingResult processAnnotation(
            AnnotationInfo ainfo, WebComponentContext[] webCompContexts)
            throws AnnotationProcessorException;

    /**
     * Process Annotation with given WebBundleContext.
     * @param ainfo
     * @param webBundleContext
     * @return HandlerProcessingResult
     */
    protected abstract HandlerProcessingResult processAnnotation(
            AnnotationInfo ainfo, WebBundleContext webBundleContext)
            throws AnnotationProcessorException;

    /**
     * Process a particular annotation which type is the same as the
     * one returned by @see getAnnotationType(). All information
     * pertinent to the annotation and its context is encapsulated
     * in the passed AnnotationInfo instance.
     *
     * @param ainfo the annotation information
     */
    public HandlerProcessingResult processAnnotation(AnnotationInfo ainfo)
            throws AnnotationProcessorException {

        AnnotatedElementHandler aeHandler = ainfo.getProcessingContext().getHandler();
        if (aeHandler instanceof EjbBundleContext) {
            EjbBundleContext ejbBundleContext = (EjbBundleContext)aeHandler;
            aeHandler = ejbBundleContext.createContextForEjb();
        } else if (aeHandler instanceof WebBundleContext) {
            WebBundleContext webBundleContext = (WebBundleContext)aeHandler;
            aeHandler = webBundleContext.createContextForWeb();
            if (aeHandler == null) {
                // no such web comp, use webBundleContext
                aeHandler = ainfo.getProcessingContext().getHandler();
            }
        }

        if (aeHandler == null) {
            // no such ejb
            return getInvalidAnnotatedElementHandlerResult(
                ainfo.getProcessingContext().getHandler(), ainfo);
        }

        if (!supportTypeInheritance() &&
                ElementType.TYPE.equals(ainfo.getElementType()) &&
                aeHandler instanceof ComponentContext) {
            ComponentContext context = (ComponentContext)aeHandler;
            Class clazz = (Class)ainfo.getAnnotatedElement();
            if (!clazz.getName().equals(context.getComponentClassName())) {
                if (logger.isLoggable(Level.WARNING)) {
                    log(Level.WARNING, ainfo, 
                        localStrings.getLocalString(
                        "enterprise.deployment.annotation.handlers.typeinhernotsupp",
                        "The annotation symbol inheritance is not supported."));
                }
                return getDefaultProcessedResult();
            }
        }

        HandlerProcessingResult procResult = null;
        if (aeHandler instanceof EjbContext) {
            procResult = processAnnotation(ainfo, new EjbContext[] { (EjbContext)aeHandler });
        } else if (aeHandler instanceof EjbsContext) {
            EjbsContext ejbsContext = (EjbsContext)aeHandler;
            procResult = processAnnotation(ainfo, ejbsContext.getEjbContexts());
        } else if (aeHandler instanceof WebComponentContext) {
            procResult = processAnnotation(ainfo,
                new WebComponentContext[] { (WebComponentContext)aeHandler });
        } else if (aeHandler instanceof WebComponentsContext) {
            WebComponentsContext webCompsContext = (WebComponentsContext)aeHandler;
            procResult = processAnnotation(ainfo, webCompsContext.getWebComponentContexts());
        } else if (aeHandler instanceof WebBundleContext) {
            WebBundleContext webBundleContext = (WebBundleContext)aeHandler;
            procResult = processAnnotation(ainfo, webBundleContext);
        } else {
            return getInvalidAnnotatedElementHandlerResult(aeHandler, ainfo);
        }

        return procResult;
    }

    /**
     * This indicates whether the annotation type should be processed for
     * type level in super-class.
     */
    protected boolean supportTypeInheritance() {
        return false;
    }
}

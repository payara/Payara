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
package org.glassfish.ejb.deployment.annotation.handlers;

import com.sun.enterprise.deployment.annotation.context.ComponentContext;
import com.sun.enterprise.deployment.annotation.context.EjbBundleContext;
import com.sun.enterprise.deployment.annotation.context.EjbContext;
import com.sun.enterprise.deployment.annotation.context.EjbInterceptorContext;
import com.sun.enterprise.deployment.annotation.context.EjbsContext;
import com.sun.enterprise.deployment.annotation.handlers.AbstractHandler;
import com.sun.enterprise.util.LocalStringManagerImpl;
import org.glassfish.apf.AnnotatedElementHandler;
import org.glassfish.apf.AnnotationInfo;
import org.glassfish.apf.AnnotationProcessorException;
import org.glassfish.apf.HandlerProcessingResult;
import org.jboss.jandex.ClassInfo;

import java.lang.annotation.ElementType;
import java.util.logging.Level;

/**
 * This is an abstract class encapsulate generic behaviour of annotation
 * handler applying on Ejb Class. It will get the corresponding EjbDescriptors
 * associated to the annotation on the given Ejb Class and then pass it to
 * underlying subclass to process. We will only process
 * annotations that are not on the business interface,
 * not on the super class of the component and not on
 * the dummy ejb descriptor.
 *
 * @author Shing Wai Chan
 */
public abstract class AbstractAttributeHandler extends AbstractHandler {

    protected final static LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(AbstractAttributeHandler.class);

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
     * Process Annotation with given EjbInterceptorContext.
     * @param ainfo
     * @param ejbInterceptorContext
     * @return HandlerProcessingResult
     */
    protected HandlerProcessingResult processAnnotation(
            AnnotationInfo ainfo, EjbInterceptorContext ejbInterceptorContext)
            throws AnnotationProcessorException {
        if (!isDelegatee()) {
            throw new UnsupportedOperationException();
        }
        return getDefaultProcessedResult();
    }

    /**
     * Process a particular annotation which type is the same as the
     * one returned by @see getAnnotationType(). All information
     * pertinent to the annotation and its context is encapsulated
     * in the passed AnnotationInfo instance.
     *
     * @param ainfo the annotation information
     */
    @Override
    public HandlerProcessingResult processAnnotation(AnnotationInfo ainfo) throws AnnotationProcessorException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer("@process annotation " + ainfo.getAnnotation() + " in " + ainfo.getAnnotatedElement());
        }

        AnnotatedElementHandler aeHandler = ainfo.getProcessingContext().getHandler();

        if (aeHandler instanceof EjbBundleContext) {
            EjbBundleContext ejbBundleContext = (EjbBundleContext)aeHandler;
            AnnotatedElementHandler aeh = ejbBundleContext.createContextForEjb();
            if (aeh != null) {
                aeHandler = aeh;
            } else {
                if (isDelegatee()) {
                    aeHandler = ejbBundleContext.createContextForEjbInterceptor();
                }
                if (aeHandler == null) {
                    return getInvalidAnnotatedElementHandlerResult(null, ainfo);
                }
            }
        }

        if (!supportTypeInheritance() &&
                ElementType.TYPE.equals(ainfo.getElementType()) &&
                aeHandler instanceof ComponentContext) {
            ComponentContext context = (ComponentContext)aeHandler;
            ClassInfo clazz = ainfo.getAnnotatedElement().asClass();
            if (!clazz.name().toString().equals(context.getComponentClassName())) {
                if (logger.isLoggable(Level.WARNING)) {
                    log(Level.WARNING, ainfo,
                        localStrings.getLocalString(
                        "enterprise.deployment.annotation.handlers.typeinhernotsupp",
                        "The annotation symbol inheritance is not supported."));
                }
                return getDefaultProcessedResult();
            }
        }

        EjbContext[] ejbContexts = null;
        EjbInterceptorContext ejbInterceptorContext = null;
        if (aeHandler instanceof EjbContext) {
            EjbContext ejbContext = (EjbContext)aeHandler;
            ejbContexts = new EjbContext[] { ejbContext };
        } else if (aeHandler instanceof EjbsContext) {
            ejbContexts = ((EjbsContext)aeHandler).getEjbContexts();
        } else if (isDelegatee() && aeHandler instanceof EjbInterceptorContext) {
            ejbInterceptorContext = (EjbInterceptorContext)aeHandler;
        } else {
            return getInvalidAnnotatedElementHandlerResult(aeHandler, ainfo);
        }

        HandlerProcessingResult procResult;
        if (ejbInterceptorContext != null) {
            procResult = processAnnotation(ainfo, ejbInterceptorContext);
        } else {
            procResult = processAnnotation(ainfo, ejbContexts);
        }

        if (logger.isLoggable(Level.FINER)) {
            logger.finer("New annotation for " + ainfo.getAnnotation());
        }
        return procResult;
    }

    /**
     * This indicates whether the annotation type should be processed for
     * type level in super-class.
     */
    protected boolean isDelegatee() {
        return false;
    }

    /**
     * This indicates whether the annotation type should be processed for
     * type level in super-class.
     */
    protected boolean supportTypeInheritance() {
        return false;
    }
}

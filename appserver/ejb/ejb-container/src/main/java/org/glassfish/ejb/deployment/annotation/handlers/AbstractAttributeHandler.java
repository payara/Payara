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

package org.glassfish.ejb.deployment.annotation.handlers;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.reflect.AnnotatedElement;
import java.util.logging.Level;

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

/**
 * This is an abstract class encapsulate generic behaviour of annotation
 * handler applying on Ejb Class.  It will get the corresponding
 * EjbDescriptors associated to the annotation on the given Ejb Class
 * and then pass it to underlying processAnnotation method.
 * Concrete subclass handlers need to implement the following:
 *     public Class&lt;? extends Annotation&gt; getAnnotationType();
 *     protected HandlerProcessingResult processAnnotation(AnnotationInfo ainfo,
 *             EjbContext[] ejbContexts) throws AnnotationProcessorException;
 * It may also need to override the following:
 * a) if other annotations need to be processed prior to given annotation:
 *     public Class&lt;? extends Annotation&gt;[] getTypeDependencies();
 * b) if the given annotation can be processed while processing another
 *    annotation
 *     protected boolean isDelegatee();
 * c) if we need to process for interceptor
 *     protected HandlerProcessingResult processAnnotation(AnnotationInfo ainfo,
 *             EjbInterceptorContext ejbInterceptorContext)
 *             throws AnnotationProcessorException;
 * d) indicate the annotation support type inheritance
 *     protected boolean supportTypeIneritance();
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
     * @exception AnnotationProcessorException
     */
    protected abstract HandlerProcessingResult processAnnotation(
            AnnotationInfo ainfo, EjbContext[] ejbContexts)
            throws AnnotationProcessorException;

    /**
     * Process Annotation with given InteceptorContext.
     * @param ainfo
     * @param ejbInterceptorContext
     * @return HandlerProcessingResult
     * @exception AnnotationProcessorException
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
     * This is a method in interface AnnotationHandler.
     *
     * @param ainfo the annotation information
     */
    public HandlerProcessingResult processAnnotation(AnnotationInfo ainfo) 
            throws AnnotationProcessorException {
        
        AnnotatedElement ae = ainfo.getAnnotatedElement();
        Annotation annotation = ainfo.getAnnotation();

        if (logger.isLoggable(Level.FINER)) {
            logger.finer("@process annotation " + annotation + " in " + ae);
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

        HandlerProcessingResult procResult = null;

        if (ejbInterceptorContext != null) {
            procResult = processAnnotation(ainfo, ejbInterceptorContext);
        } else {
            procResult = processAnnotation(ainfo, ejbContexts);
        }

        if (logger.isLoggable(Level.FINER)) {
            logger.finer("New annotation for " + annotation);
        }
        return procResult;
    }

    /**
     * This indicates whether the annotation can be processed by delegation
     * from the another annotation.
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

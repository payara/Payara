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

import com.sun.enterprise.deployment.MethodDescriptor;
import com.sun.enterprise.deployment.annotation.context.EjbContext;
import com.sun.enterprise.deployment.annotation.handlers.PostProcessor;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.Local;
import jakarta.ejb.Remote;
import jakarta.ejb.Singleton;
import jakarta.ejb.Stateful;
import jakarta.ejb.Stateless;
import org.glassfish.apf.AnnotationHandlerFor;
import org.glassfish.apf.AnnotationInfo;
import org.glassfish.apf.AnnotationProcessorException;
import org.glassfish.apf.HandlerProcessingResult;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbSessionDescriptor;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.MethodInfo;
import org.jvnet.hk2.annotations.Service;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.util.logging.Level;

/**
 * This handler is responsible for handling the jakarta.ejb.Asynchronous annotation on
 * a Bean class or method.
 */
@Service
@AnnotationHandlerFor(Asynchronous.class)
public class AsynchronousHandler extends AbstractAttributeHandler
        implements PostProcessor<EjbContext> {

    public AsynchronousHandler() {
    }

    @Override
    protected HandlerProcessingResult processAnnotation(AnnotationInfo ainfo,
            EjbContext[] ejbContexts) throws AnnotationProcessorException {

        for (EjbContext ejbContext : ejbContexts) {
            EjbDescriptor ejbDesc = (EjbDescriptor) ejbContext.getDescriptor();

            if (ElementType.TYPE.equals(ainfo.getElementType())) {
                // delay processing Class-level default until after methods are processed
                ejbContext.addPostProcessInfo(ainfo, this);
            } else {
                MethodInfo annMethod = ainfo.getAnnotatedElement().asMethod();
                setAsynchronous(annMethod, ejbDesc);
            }
        }

        return getDefaultProcessedResult();
    }

    /**
     * @return an array of annotation types this annotation handler would
     * require to be processed (if present) before it processes it's own
     * annotation type.
     */
    @Override
    public Class<? extends Annotation>[] getTypeDependencies() {
        return new Class[] {
            Local.class, Remote.class, Stateful.class, Stateless.class, Singleton.class};
    }

    @Override
    protected boolean supportTypeInheritance() {
        return true;
    }

    /**
     * Set the default value (from class type annotation) on all
     * methods that don't have a value.
     */
    @Override
    public void postProcessAnnotation(AnnotationInfo ainfo, EjbContext ejbContext)
            throws AnnotationProcessorException {
        EjbDescriptor ejbDesc = (EjbDescriptor) ejbContext.getDescriptor();
        ClassInfo classAn = ainfo.getAnnotatedElement().asClass();

        for (MethodInfo m0 : classAn.methods()) {
            if (!m0.name().startsWith("<")) {
                setAsynchronous(m0, ejbDesc);
            }
        }
    }

    private void setAsynchronous(MethodInfo m0, EjbDescriptor ejbDesc) throws AnnotationProcessorException {
        setAsynchronous(m0, ejbDesc, null);
    }

    /**
     * Designate a method as asynchronous.
     */
    private void setAsynchronous(MethodInfo m0, EjbDescriptor ejbDesc, String methodIntf)
            throws AnnotationProcessorException {

        if( !ejbDesc.getType().equals(EjbSessionDescriptor.TYPE)) {
            throw new AnnotationProcessorException("Invalid asynchronous method " + m0 +
                 "@Asynchronous is only permitted for session beans");
        }

        EjbSessionDescriptor sessionDesc = (EjbSessionDescriptor) ejbDesc;

        MethodDescriptor methodDesc = (methodIntf == null) ?
                new MethodDescriptor(m0) : new MethodDescriptor(m0, methodIntf);

        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Adding asynchronous method " + methodDesc);
        }

        // There is no way to "turn off" the asynchronous designation in the
        // deployment descriptor, so we don't need to do any override checks
        // here.  Just always add any async methods.
        sessionDesc.addAsynchronousMethod(methodDesc);

    }

}

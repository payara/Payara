/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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
import java.lang.reflect.Method;
import java.util.Set;
import java.util.logging.Level;
import javax.ejb.MessageDriven;
import javax.ejb.Singleton;
import javax.ejb.Stateful;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagement;

import com.sun.enterprise.deployment.LifecycleCallbackDescriptor;
import com.sun.enterprise.deployment.MethodDescriptor;
import com.sun.enterprise.deployment.annotation.context.EjbContext;
import com.sun.enterprise.deployment.annotation.handlers.PostProcessor;
import com.sun.enterprise.deployment.util.TypeUtil;
import org.glassfish.apf.AnnotationHandlerFor;
import org.glassfish.apf.AnnotationInfo;
import org.glassfish.apf.AnnotationProcessorException;
import org.glassfish.apf.HandlerProcessingResult;
import org.glassfish.ejb.deployment.descriptor.ContainerTransaction;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbSessionDescriptor;
import org.jvnet.hk2.annotations.Service;

/**
 * This handler is responsible for handling the javax.ejb.TransactionAttribute.
 *
 * @author Shing Wai Chan
 */
@Service
@AnnotationHandlerFor(TransactionAttribute.class)
public class TransactionAttributeHandler extends AbstractAttributeHandler
        implements PostProcessor<EjbContext> {

    public TransactionAttributeHandler() {
    }

    protected HandlerProcessingResult processAnnotation(AnnotationInfo ainfo,
            EjbContext[] ejbContexts) throws AnnotationProcessorException {
        
        TransactionAttribute taAn = 
            (TransactionAttribute) ainfo.getAnnotation();

        for (EjbContext ejbContext : ejbContexts) {
            EjbDescriptor ejbDesc = (EjbDescriptor) ejbContext.getDescriptor();
            ContainerTransaction containerTransaction =
                getContainerTransaction(taAn.value());

            if (ElementType.TYPE.equals(ainfo.getElementType())) {
                ejbContext.addPostProcessInfo(ainfo, this);
            } else {
                Method annMethod = (Method) ainfo.getAnnotatedElement();
                
                Set txBusMethods = ejbDesc.getTxBusinessMethodDescriptors();
                for (Object next : txBusMethods) {
                    MethodDescriptor nextDesc = (MethodDescriptor) next;
                    Method m = nextDesc.getMethod(ejbDesc);
                    if( TypeUtil.sameMethodSignature(m, annMethod) &&
                            ejbDesc.getContainerTransactionFor(nextDesc) == null ) {
                        // override by xml
                        ejbDesc.setContainerTransactionFor
                            (nextDesc, containerTransaction);
                    }
                }

                if (ejbDesc instanceof EjbSessionDescriptor) {
                    EjbSessionDescriptor sd = (EjbSessionDescriptor)ejbDesc;
                    if ( sd.isStateful() || sd.isSingleton() ) {
                        ClassLoader loader = ejbDesc.getEjbBundleDescriptor().getClassLoader();
                        Set<LifecycleCallbackDescriptor> lcds = ejbDesc.getLifecycleCallbackDescriptors();
                        for(LifecycleCallbackDescriptor lcd : lcds) {
                            if( lcd.getLifecycleCallbackClass().equals(ejbDesc.getEjbClassName())
                                    && lcd.getLifecycleCallbackMethod().equals(annMethod.getName()) ) {
                                try {
                                    Method m = lcd.getLifecycleCallbackMethodObject(loader);
                                    MethodDescriptor md = new MethodDescriptor(m, MethodDescriptor.LIFECYCLE_CALLBACK);
                                    if( TypeUtil.sameMethodSignature(m, annMethod) &&
                                            ejbDesc.getContainerTransactionFor(md) == null ) {
                                        // override by xml
                                        ejbDesc.setContainerTransactionFor(md, containerTransaction);
                                        if (logger.isLoggable(Level.FINE)) {
                                             logger.log(Level.FINE, "Found matching callback method " + ejbDesc.getEjbClassName() 
                                                     + "<>" + md + " : " + containerTransaction);
                                        }
                                    }
                                } catch(Exception e) {
                                    if (logger.isLoggable(Level.FINE)) {
                                        logger.log(Level.FINE,
                                        "Transaction attribute for a lifecycle callback annotation processing error", e);
                                    }
                                }

                            }
                        }

                    }
                }
            }
        }

        return getDefaultProcessedResult();
    }

    private ContainerTransaction getContainerTransaction(
            TransactionAttributeType taType) {
        switch(taType) {
            case MANDATORY:
                return new ContainerTransaction(
                        ContainerTransaction.MANDATORY,
                        ContainerTransaction.MANDATORY);
            case REQUIRED:
                return new ContainerTransaction(
                        ContainerTransaction.REQUIRED,
                        ContainerTransaction.REQUIRED);
            case REQUIRES_NEW:
                return new ContainerTransaction(
                        ContainerTransaction.REQUIRES_NEW,
                        ContainerTransaction.REQUIRES_NEW);
            case SUPPORTS:
                return new ContainerTransaction(
                        ContainerTransaction.SUPPORTS,
                        ContainerTransaction.SUPPORTS);
            case NOT_SUPPORTED:
                return new ContainerTransaction(
                        ContainerTransaction.NOT_SUPPORTED,
                        ContainerTransaction.NOT_SUPPORTED);
            default: // NEVER
                return new ContainerTransaction(
                        ContainerTransaction.NEVER,
                        ContainerTransaction.NEVER);
        }
    }

    /**
     * @return an array of annotation types this annotation handler would 
     * require to be processed (if present) before it processes it's own 
     * annotation type.
     */
    public Class<? extends Annotation>[] getTypeDependencies() {
        
        return new Class[] {
            MessageDriven.class, Stateful.class, Stateless.class, Singleton.class,
                Timeout.class, TransactionManagement.class};
                
    }

    protected boolean supportTypeInheritance() {
        return true;
    }

    /**
     * Set the default value (from class type annotation) on all
     * methods that don't have a value.
     */
    public void postProcessAnnotation(AnnotationInfo ainfo, EjbContext ejbContext)
            throws AnnotationProcessorException {
        EjbDescriptor ejbDesc = (EjbDescriptor) ejbContext.getDescriptor();
        TransactionAttribute taAn = 
            (TransactionAttribute) ainfo.getAnnotation();
        ContainerTransaction containerTransaction =
            getContainerTransaction(taAn.value());
        Class classAn = (Class)ainfo.getAnnotatedElement();

        Set txBusMethods = ejbDesc.getTxBusinessMethodDescriptors();
        for (Object mdObj : txBusMethods) {
            MethodDescriptor md = (MethodDescriptor)mdObj;
            // override by xml
            if (classAn.equals(ejbContext.getDeclaringClass(md)) &&
                    ejbDesc.getContainerTransactionFor(md) == null) {
                ejbDesc.setContainerTransactionFor(
                    md, containerTransaction);
            }
        }
    }
}

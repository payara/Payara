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
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;

import com.sun.enterprise.deployment.MethodDescriptor;
import com.sun.enterprise.deployment.annotation.context.EjbContext;
import com.sun.enterprise.deployment.annotation.handlers.PostProcessor;
import com.sun.enterprise.deployment.util.TypeUtil;
import org.glassfish.apf.AnnotationHandlerFor;
import org.glassfish.apf.AnnotationInfo;
import org.glassfish.apf.AnnotationProcessorException;
import org.glassfish.apf.HandlerProcessingResult;
import org.glassfish.ejb.deployment.descriptor.EjbSessionDescriptor;
import org.jvnet.hk2.annotations.Service;

/**
 * This handler is responsible for handling the javax.ejb.Lock.
 *
 * @author Mahesh Kannan
 * @author Marina Vatkina
 */
@Service
@AnnotationHandlerFor(Lock.class)
public class LockHandler extends AbstractAttributeHandler 
        implements PostProcessor<EjbContext> {

    public LockHandler() {
    }

    protected HandlerProcessingResult processAnnotation(AnnotationInfo ainfo,
            EjbContext[] ejbContexts) throws AnnotationProcessorException {

        Lock lockAnn = (Lock) ainfo.getAnnotation();

        for (EjbContext ejbContext : ejbContexts) {
            if (ejbContext.getDescriptor() instanceof EjbSessionDescriptor) {
                EjbSessionDescriptor singletonDesc =
                        (EjbSessionDescriptor) ejbContext.getDescriptor();

                if( !singletonDesc.isSingleton() ) {
                    throw new AnnotationProcessorException("@Lock is only permitted for " +
                            "singleton session beans");
                }

                if (ElementType.TYPE.equals(ainfo.getElementType())) {
                    // Delay processing Class-level default until after methods are processed
                    ejbContext.addPostProcessInfo(ainfo, this);
                } else {
                    Method annMethod = (Method) ainfo.getAnnotatedElement();

                    // Only assign lock type if the method hasn't already been processed.
                    // This correctly ignores overridden superclass methods and
                    // applies the correct .xml overriding semantics.
                    if(!matchesExistingReadOrWriteLockMethod(annMethod, singletonDesc)) {
                        MethodDescriptor newMethodDesc = new MethodDescriptor(annMethod);
                        if( lockAnn.value() == LockType.WRITE) {
                            singletonDesc.addWriteLockMethod(newMethodDesc);
                        } else {
                            singletonDesc.addReadLockMethod(newMethodDesc);
                        }
                    }
                }
            }
        }

        return getDefaultProcessedResult();
    }

    /**
     * @return an array of annotation types this annotation handler would
     *         require to be processed (if present) before it processes it's own
     *         annotation type.
     */
    public Class<? extends Annotation>[] getTypeDependencies() {

        return new Class[]{Singleton.class, ConcurrencyManagement.class};

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
        EjbSessionDescriptor ejbDesc = (EjbSessionDescriptor) ejbContext.getDescriptor();

        Class classAn = (Class)ainfo.getAnnotatedElement();
        Lock lockAnn = (Lock) ainfo.getAnnotation();

        // At this point, all method-level specific annotations have been processed.
        // For non-private methods, find the ones from the EjbContext's
        // component definition view that are declared on this class.  This will correctly
        // eliminate any overridden methods and provide the most-derived version of each.
        // Use the Class's declared methods list to get the private methods.

        List<Method> toProcess = new ArrayList<Method>();
        for(Method m : ejbContext.getComponentDefinitionMethods()) {
            if( classAn.equals(m.getDeclaringClass())) {
                toProcess.add(m);
            }
        }
        for(Method m : classAn.getDeclaredMethods()) {
            if( Modifier.isPrivate(m.getModifiers()) ) {
                toProcess.add(m);
            }
        }

        for( Method m : toProcess ) {

            // If the method is declared on the same class as the TYPE-level default
            // and it hasn't already been assigned lock information from the deployment
            // descriptor, set it.
            if( !matchesExistingReadOrWriteLockMethod(m, ejbDesc) ) {
                MethodDescriptor newMethodDesc = new MethodDescriptor(m);
                if( lockAnn.value() == LockType.WRITE) {
                    ejbDesc.addWriteLockMethod(newMethodDesc);
                } else {
                    ejbDesc.addReadLockMethod(newMethodDesc);
                }
            }

        }
    }

    private boolean matchesExistingReadOrWriteLockMethod(Method methodToMatch,
                                                         EjbSessionDescriptor desc) {

        List<MethodDescriptor> lockMethods = desc.getReadAndWriteLockMethods();

        boolean match = false;
        for (MethodDescriptor next : lockMethods) {

            Method m = next.getMethod(desc);
            if( ( m.getDeclaringClass().equals(methodToMatch.getDeclaringClass()) ) &&
                TypeUtil.sameMethodSignature(m, methodToMatch) ) {
                match = true;
                break;
            }
        }

        return match;
    }


}

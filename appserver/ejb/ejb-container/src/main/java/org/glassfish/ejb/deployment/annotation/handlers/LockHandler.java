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
import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.Lock;
import jakarta.ejb.LockType;
import jakarta.ejb.Singleton;
import org.glassfish.apf.AnnotationHandlerFor;
import org.glassfish.apf.AnnotationInfo;
import org.glassfish.apf.AnnotationProcessorException;
import org.glassfish.apf.HandlerProcessingResult;
import org.glassfish.ejb.deployment.descriptor.EjbSessionDescriptor;
import org.glassfish.internal.deployment.ClassHierarchy;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.MethodInfo;
import org.jvnet.hk2.annotations.Service;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * This handler is responsible for handling the jakarta.ejb.Lock annotation on
 * a Bean class or method.
 */
@Service
@AnnotationHandlerFor(Lock.class)
public class LockHandler extends AbstractAttributeHandler
        implements PostProcessor<EjbContext> {

    public LockHandler() {
    }

    @Override
    protected HandlerProcessingResult processAnnotation(AnnotationInfo ainfo,
            EjbContext[] ejbContexts) throws AnnotationProcessorException {

        LockType lockType = ainfo.enumValue("value", LockType.class);

        for (EjbContext ejbContext : ejbContexts) {
            if (ejbContext.getDescriptor() instanceof EjbSessionDescriptor) {
                EjbSessionDescriptor singletonDesc = (EjbSessionDescriptor) ejbContext.getDescriptor();

                if( !singletonDesc.isSingleton() ) {
                    throw new AnnotationProcessorException("@Lock is only permitted for " +
                            "singleton session beans");
                }

                if (ElementType.TYPE.equals(ainfo.getElementType())) {
                    // delay processing Class-level default until after methods are processed
                    ejbContext.addPostProcessInfo(ainfo, this);
                } else {
                    MethodInfo annMethod = ainfo.getAnnotatedElement().asMethod();

                    // Only add the method-level Lock if the method doesn't already
                    // have a lock type from the deployment descriptor.
                    if(!matchesExistingReadOrWriteLockMethod(annMethod, singletonDesc, ejbContext)) {
                        MethodDescriptor newMethodDesc = new MethodDescriptor(annMethod);
                        if( lockType == LockType.WRITE) {
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
     * require to be processed (if present) before it processes it's own
     * annotation type.
     */
    @Override
    public Class<? extends Annotation>[] getTypeDependencies() {
        return new Class[]{Singleton.class, ConcurrencyManagement.class};
    }

    @Override
    protected boolean supportTypeInheritance() {
        return true;
    }

    /**
     * Set the default value (from class type annotation) on all
     * methods that don't have a value.
     * Class type annotation applies to all methods not having an explicit method-level
     * annotation, including methods of its superclasses it does not override and the
     * class's own private methods.
     */
    @Override
    public void postProcessAnnotation(AnnotationInfo ainfo, EjbContext ejbContext)
            throws AnnotationProcessorException {
        EjbSessionDescriptor ejbDesc = (EjbSessionDescriptor) ejbContext.getDescriptor();

        ClassInfo classAn = ainfo.getAnnotatedElement().asClass();
        String className = classAn.name().toString();
        LockType lockType = ainfo.enumValue("value", LockType.class);

        List<MethodInfo> toProcess = new ArrayList<>();
        for(MethodInfo m : ejbContext.getComponentDefinitionMethods()) {
            if( className.equals(m.declaringClass().name().toString())) {
                toProcess.add(m);
            }
        }
        for(MethodInfo m : classAn.methods()) {
            if( Modifier.isPrivate(m.flags()) && !m.name().startsWith("<") ) {
                toProcess.add(m);
            }
        }

        for( MethodInfo m : toProcess ) {

            if( !matchesExistingReadOrWriteLockMethod(m, ejbDesc, ejbContext) ) {
                MethodDescriptor newMethodDesc = new MethodDescriptor(m);
                if( lockType == LockType.WRITE) {
                    ejbDesc.addWriteLockMethod(newMethodDesc);
                } else {
                    ejbDesc.addReadLockMethod(newMethodDesc);
                }
            }

        }
    }

    private boolean matchesExistingReadOrWriteLockMethod(MethodInfo methodToMatch, EjbSessionDescriptor desc,
                                                         EjbContext ejbContext) {

        List<MethodDescriptor> lockMethods = desc.getReadAndWriteLockMethods();
        String declaringClass = methodToMatch.declaringClass().name().toString();

        for (MethodDescriptor next : lockMethods) {
            if (declaringClass.equals(ejbContext.getDeclaringClass(next))
                    && ClassHierarchy.matches(methodToMatch, next.getName(), next.getJavaParameterClassNames())) {
                return true;
            }
        }

        return false;
    }

}

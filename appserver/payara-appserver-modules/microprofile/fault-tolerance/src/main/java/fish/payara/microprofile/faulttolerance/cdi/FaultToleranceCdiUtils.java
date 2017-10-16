/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2017] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/master/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 * 
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at glassfish/legal/LICENSE.txt.
 * 
 *     GPL Classpath Exception:
 *     The Payara Foundation designates this particular file as subject to the "Classpath"
 *     exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *     file that accompanied this code.
 * 
 *     Modifications:
 *     If applicable, add the following below the License Header, with the fields
 *     enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyright [year] [name of copyright owner]"
 * 
 *     Contributor(s):
 *     If you wish your version of this file to be governed by only the CDDL or
 *     only the GPL Version 2, indicate your decision by adding "[Contributor]
 *     elects to include this software in this distribution under the [CDDL or GPL
 *     Version 2] license."  If you don't indicate a single choice of license, a
 *     recipient has the option to distribute your version of this file under
 *     either the CDDL, the GPL Version 2 or to extend the choice of license to
 *     its licensees as provided above.  However, if you add GPL Version 2 code
 *     and therefore, elected the GPL Version 2 license, then the option applies
 *     only if the new code is made subject to such option by the copyright
 *     holder.
 */
package fish.payara.microprofile.faulttolerance.cdi;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import javax.enterprise.inject.spi.BeanManager;
import javax.interceptor.InvocationContext;

/**
 *
 * @author Andrew Pielage
 */
public class FaultToleranceCdiUtils {

//    private static final String CDI_BINDINGS_CLASS = "org.jboss.weld.interceptor.bindings";
    
    public static <A extends Annotation> A getAnnotation(BeanManager beanManager, Class<A> annotationClass, 
            InvocationContext invocationContext) {
        A annotation = null;
        Class<?> annotatedClass = invocationContext.getMethod().getDeclaringClass();
        
        // Try to get the annotation from the method, otherwise attempt to get it from the class
        if (invocationContext.getMethod().isAnnotationPresent(annotationClass)) {
            annotation = invocationContext.getMethod().getAnnotation(annotationClass);
        } else {
            if (annotatedClass.isAnnotationPresent(annotationClass)) {
                annotation = annotatedClass.getAnnotation(annotationClass);
            } else {
                // Account for Stereotypes
                Queue<Annotation> annotations = new LinkedList<>(Arrays.asList(annotatedClass.getAnnotations()));

                while (!annotations.isEmpty()) {
                    Annotation a = annotations.remove();

                    if (a.annotationType().equals(annotationClass)) {
                        annotation = annotationClass.cast(a);
                        break;
                    }

                    if (beanManager.isStereotype(a.annotationType())) {
                        annotations.addAll(beanManager.getStereotypeDefinition(a.annotationType()));
                    }
                }
            }
        }
        
        // ************ WELD 3 ONLY ************
//        // If we still haven't found the annotation yet, try to get it from the CDI bindings
//        if (annotation == null) {
//            Set<Annotation> bindings = (Set<Annotation>) invocationContext.getContextData().get(CDI_BINDINGS_CLASS);
//        
//            if (bindings != null) {
//                for (Annotation binding : bindings) {
//                    if (binding.annotationType().equals(annotationClass)) {
//                        annotation = annotationClass.cast(binding);
//
//                        if (annotation != null) {
//                            break;
//                        }
//                    }
//                }
//            }
//        }    
        // ************ END OF WELD 3 ONLY ************
        
        return annotation;
    }
}

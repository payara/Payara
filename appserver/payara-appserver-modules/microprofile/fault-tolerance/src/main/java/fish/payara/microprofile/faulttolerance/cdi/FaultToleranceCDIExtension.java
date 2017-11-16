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

import fish.payara.microprofile.faulttolerance.interceptors.AsynchronousInterceptor;
import fish.payara.microprofile.faulttolerance.interceptors.BulkheadInterceptor;
import fish.payara.microprofile.faulttolerance.interceptors.CircuitBreakerInterceptor;
import fish.payara.microprofile.faulttolerance.interceptors.RetryInterceptor;
import fish.payara.microprofile.faulttolerance.interceptors.TimeoutInterceptor;
import fish.payara.microprofile.faulttolerance.validators.AsynchronousValidator;
import fish.payara.microprofile.faulttolerance.validators.BulkheadValidator;
import fish.payara.microprofile.faulttolerance.validators.CircuitBreakerValidator;
import fish.payara.microprofile.faulttolerance.validators.FallbackValidator;
import fish.payara.microprofile.faulttolerance.validators.RetryValidator;
import fish.payara.microprofile.faulttolerance.validators.TimeoutValidator;
import java.lang.annotation.Annotation;
import java.util.Set;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.WithAnnotations;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

/**
 *
 * @author Andrew Pielage
 */
public class FaultToleranceCDIExtension implements Extension {
    
    void beforeBeanDiscovery(@Observes BeforeBeanDiscovery beforeBeanDiscovery, BeanManager beanManager) {
        // Add each of the Fault Tolerance interceptors
        beforeBeanDiscovery.addInterceptorBinding(Asynchronous.class);
        AnnotatedType<AsynchronousInterceptor> asynchronousInterceptor = 
                beanManager.createAnnotatedType(AsynchronousInterceptor.class);
        beforeBeanDiscovery.addAnnotatedType(asynchronousInterceptor, AsynchronousInterceptor.class.getName());
        
        beforeBeanDiscovery.addInterceptorBinding(Bulkhead.class);
        AnnotatedType<BulkheadInterceptor> bulkheadInterceptor 
                = beanManager.createAnnotatedType(BulkheadInterceptor.class);
        beforeBeanDiscovery.addAnnotatedType(bulkheadInterceptor, BulkheadInterceptor.class.getName());
        
        beforeBeanDiscovery.addInterceptorBinding(CircuitBreaker.class);
        AnnotatedType<CircuitBreakerInterceptor> circuitBreakerInterceptor = 
                beanManager.createAnnotatedType(CircuitBreakerInterceptor.class);
        beforeBeanDiscovery.addAnnotatedType(circuitBreakerInterceptor, CircuitBreakerInterceptor.class.getName());
        
        beforeBeanDiscovery.addInterceptorBinding(Retry.class);
        AnnotatedType<RetryInterceptor> retryInterceptor = beanManager.createAnnotatedType(RetryInterceptor.class);
        beforeBeanDiscovery.addAnnotatedType(retryInterceptor, RetryInterceptor.class.getName());
        
        beforeBeanDiscovery.addInterceptorBinding(Timeout.class);
        AnnotatedType<TimeoutInterceptor> timeoutInterceptor = beanManager.createAnnotatedType(TimeoutInterceptor.class);
        beforeBeanDiscovery.addAnnotatedType(timeoutInterceptor, TimeoutInterceptor.class.getName());
    }
    
    <T> void processAnnotatedType(@Observes @WithAnnotations({ Asynchronous.class, Bulkhead.class, CircuitBreaker.class,
            Fallback.class, Retry.class, Timeout.class }) ProcessAnnotatedType<T> processAnnotatedType, 
            BeanManager beanManager) throws Exception {
        AnnotatedType<T> annotatedType = processAnnotatedType.getAnnotatedType();
        
        // Validate the Fault Tolerance annotations for each annotated method
        Set<AnnotatedMethod<? super T>> annotatedMethods = annotatedType.getMethods();
        for (AnnotatedMethod<?> annotatedMethod : annotatedMethods) {
            validateMethodAnnotations(annotatedMethod);
        }
    }
    
    /**
     * Helper method that validates the Fault Tolerance annotations for a given method.
     * @param <T> The annotated method type
     * @param annotatedMethod The annotated method to validate
     * @throws Exception 
     */
    private <T> void validateMethodAnnotations(AnnotatedMethod<T> annotatedMethod) 
            throws ClassNotFoundException, NoSuchMethodException {      
        Config config = ConfigProvider.getConfig();
        
        for (Annotation annotation : annotatedMethod.getAnnotations()) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            
            if (annotationType == Asynchronous.class) {
                AsynchronousValidator.validateAnnotation((Asynchronous) annotation, annotatedMethod);
            } else if (annotationType == Bulkhead.class) {
                BulkheadValidator.validateAnnotation((Bulkhead) annotation, annotatedMethod, config);
            } else if (annotationType == CircuitBreaker.class) {
                CircuitBreakerValidator.validateAnnotation((CircuitBreaker) annotation, annotatedMethod, config);
            } else if (annotationType == Fallback.class) {
                FallbackValidator.validateAnnotation((Fallback) annotation, annotatedMethod, config);
            } else if (annotationType == Retry.class) {
                RetryValidator.validateAnnotation((Retry) annotation, annotatedMethod, config);
            } else if (annotationType == Timeout.class) {
                TimeoutValidator.validateAnnotation((Timeout) annotation, annotatedMethod, config);
            }
        }
    }
}

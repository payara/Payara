/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2017-2019] Payara Foundation and/or its affiliates. All rights reserved.
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

import fish.payara.microprofile.faulttolerance.FaultToleranceConfig;
import fish.payara.microprofile.faulttolerance.policy.FaultTolerancePolicy;
import fish.payara.microprofile.faulttolerance.service.FaultToleranceUtils;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

import javax.annotation.Priority;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.WithAnnotations;
import javax.enterprise.inject.spi.configurator.AnnotatedMethodConfigurator;
import javax.enterprise.util.AnnotationLiteral;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Optional;

/**
 * CDI Extension that does the setup for FT interceptor handling.
 * 
 * @author Andrew Pielage
 * @author Jan Bernitt (2.0)
 */
public class FaultToleranceExtension implements Extension {

    private static final String INTERCEPTOR_PRIORITY_PROPERTY = "mp.fault.tolerance.interceptor.priority";

    /**
     * The {@link FaultTolerance} "instance" we use to dynamically mark methods at runtime that should be
     * handled by the {@link FaultToleranceInterceptor} that handles all of the FT annotations.
     */
    private static final Annotation MARKER = () -> FaultTolerance.class;

    void beforeBeanDiscovery(@Observes BeforeBeanDiscovery beforeBeanDiscovery, BeanManager beanManager) {
        beforeBeanDiscovery.addAnnotatedType(beanManager.createAnnotatedType(AsynchronousInterceptor.class), "MP-FT-Asynchronous");
        beforeBeanDiscovery.addAnnotatedType(beanManager.createAnnotatedType(BulkheadInterceptor.class), "MP-FT-Bulkhead");
        beforeBeanDiscovery.addAnnotatedType(beanManager.createAnnotatedType(CircuitBreakerInterceptor.class), "MP-FT-CircuitBreaker");
        beforeBeanDiscovery.addAnnotatedType(beanManager.createAnnotatedType(RetryInterceptor.class), "MP-FT-Retry");
        beforeBeanDiscovery.addAnnotatedType(beanManager.createAnnotatedType(TimeoutInterceptor.class), "MP-FT-Timeout");
//        beforeBeanDiscovery.addAnnotatedType(beanManager.createAnnotatedType(FallbackInterceptor.class), "MP-FT-Fallback");
//        beforeBeanDiscovery.addAnnotatedType(beanManager.createAnnotatedType(FaultToleranceInterceptor.class), "MP-FT");
    }

    /**
     * Marks all {@link Method}s *affected* by FT annotation with the {@link FaultTolerance} annotation which
     * is handled by the {@link FaultToleranceInterceptor} which processes the FT annotations.
     * 
     * @param processAnnotatedType type currently processed
     */
    <T> void processAnnotatedType(@Observes @WithAnnotations({ Asynchronous.class, Bulkhead.class, CircuitBreaker.class,
        Fallback.class, Retry.class, Timeout.class }) ProcessAnnotatedType<T> processAnnotatedType) throws Exception {
        // Ignore our own interceptor classes
        Class<T> annotatedClass = processAnnotatedType.getAnnotatedType().getJavaClass();
        if (annotatedClass.equals(AsynchronousInterceptor.class)
                || annotatedClass.equals(BulkheadInterceptor.class)
                || annotatedClass.equals(CircuitBreakerInterceptor.class)
                || annotatedClass.equals(RetryInterceptor.class)
                || annotatedClass.equals(TimeoutInterceptor.class)) {
            return;
        }

        Class<? extends Annotation>[] alternativeAsynchronousAnnotations = getAlternativeAsynchronousAnnotations();
        AnnotatedType<T> type = processAnnotatedType.getAnnotatedType();
        boolean markAllMethods = FaultToleranceUtils.isAnnotatedWithFaultToleranceAnnotations(type)
                || isAnyAnnotationPresent(type, alternativeAsynchronousAnnotations);
        Class<?> targetClass = type.getJavaClass();
        for (AnnotatedMethodConfigurator<?> methodConfigurator : processAnnotatedType.configureAnnotatedType().methods()) {
            AnnotatedMethod<?> method = methodConfigurator.getAnnotated();
            if (markAllMethods || FaultToleranceUtils.isAnnotatedWithFaultToleranceAnnotations(method)
                    || isAnyAnnotationPresent(method, alternativeAsynchronousAnnotations)) {
                FaultTolerancePolicy.asAnnotated(targetClass, method.getJavaMember());
//                methodConfigurator.add(MARKER);
            }
        }
    }

    private static boolean isAnyAnnotationPresent(Annotated element, Class<? extends Annotation>[] annotationTypes) {
        for (Class<? extends Annotation> annotationType : annotationTypes) {
            if (element.isAnnotationPresent(annotationType)) {
                return true;
            }
        }
        return false;
    }

    private static Class<? extends Annotation>[] getAlternativeAsynchronousAnnotations() {
        Optional<String> alternativeAsynchronousAnnotationNames = ConfigProvider.getConfig().getOptionalValue(
                FaultToleranceConfig.ALTERNATIVE_ASYNCHRONOUS_ANNNOTATIONS_PROPERTY, String.class);
        return alternativeAsynchronousAnnotationNames.isPresent()
                ? FaultToleranceUtils.toClassArray(alternativeAsynchronousAnnotationNames.get(),
                        FaultToleranceConfig.ALTERNATIVE_ASYNCHRONOUS_ANNNOTATIONS_PROPERTY,
                        FaultToleranceConfig.NO_ALTERNATIVE_ANNOTATIONS)
                : FaultToleranceConfig.NO_ALTERNATIVE_ANNOTATIONS;
    }

    void changeInterceptorPriority(@Observes ProcessAnnotatedType<FaultToleranceInterceptor> interceptorType) {
        Optional<Integer> priorityOverride = ConfigProvider.getConfig().getOptionalValue(INTERCEPTOR_PRIORITY_PROPERTY,
                Integer.class);
        if (priorityOverride.isPresent()) {
            interceptorType.configureAnnotatedType()
                    .remove(annotation -> annotation instanceof Priority)
                    .add(new PriorityLiteral(priorityOverride.get()));
        }
    }

    public static final class PriorityLiteral extends AnnotationLiteral<Priority> implements Priority {
        private static final long serialVersionUID = 1L;

        private final int value;

        public PriorityLiteral(int value) {
            this.value = value;
        }

        @Override
        public int value() {
            return value;
        }
    }
}

/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2018-2025] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/main/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 *
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
 *
 * *****************************************************************************
 * Copyright (c) 2013 Antonin Stefanutti (antonin.stefanutti@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fish.payara.microprofile.metrics.cdi.interceptor;

import fish.payara.microprofile.metrics.MetricsService;
import fish.payara.microprofile.metrics.cdi.AnnotationReader;
import fish.payara.microprofile.metrics.cdi.MetricsAnnotationBinding;
import fish.payara.microprofile.metrics.impl.GaugeImpl;
import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Intercepted;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.function.BiConsumer;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Gauge;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.glassfish.internal.api.Globals;

@Interceptor
@MetricsAnnotationBinding
@Priority(Interceptor.Priority.LIBRARY_BEFORE)
public class MetricsInterceptor {

    private MetricRegistry registry;

    @Inject
    @Intercepted
    private Bean<?> bean;

    public MetricsInterceptor() {
    }

    @Inject
    public MetricsInterceptor(MetricRegistry registry, @Intercepted Bean<?> bean) {
        this.registry = registry;
        this.bean = bean;
    }

    @AroundConstruct
    private Object constructorInvocation(InvocationContext context) throws Exception {
        Object target;
        MetricsService metricsService = Globals.getDefaultBaseServiceLocator().getService(MetricsService.class);
        if (metricsService.isEnabled()) {
            Class<?> beanClass = bean.getBeanClass();
            registerMetrics(beanClass, context.getConstructor(), context.getTarget(), metricsService);
            //review metrics from stereotypes
            registerFromStereotypes(bean.getStereotypes(), beanClass, context.getConstructor(),
                    context.getTarget(), metricsService);
            //review metrics from parent type
            registerFromParentType(beanClass, beanClass.getSuperclass(), context.getConstructor(),
                    context.getTarget(), metricsService);

            target = context.proceed();

            Class<?> type = beanClass;
            do {
                for (Method method : type.getDeclaredMethods()) {
                    if (!method.isSynthetic() && !Modifier.isPrivate(method.getModifiers())) {
                        registerMetrics(beanClass, method, context.getTarget(), metricsService);
                        //review metrics from stereotypes applied to methods
                        registerFromStereotypes(bean.getStereotypes(), beanClass, method,
                                context.getTarget(), metricsService);
                        //review metrics from parent type applied to methods
                        registerFromParentType(beanClass, beanClass.getSuperclass(), method,
                                context.getTarget(), metricsService);
                    }
                }
                type = type.getSuperclass();
            } while (!Object.class.equals(type));
        } else {
            target = context.proceed();
        }
        return target;
    }

    private <E extends Member & AnnotatedElement> void registerFromParentType(Class<?> bean,
                                                                              Class<?> superClassBean, E element,
                                                                              Object target, MetricsService metricsService) {
        for (Annotation annotation : superClassBean.getAnnotations()) {
            if (annotation.annotationType().isAssignableFrom(Timed.class)) {
                registerTimedMetric((Timed) annotation, bean, element, metricsService);
                //review methods
                for (Method method : superClassBean.getDeclaredMethods()) {
                    if (!method.isSynthetic() && !Modifier.isPrivate(method.getModifiers())) {
                        registerTimedMetric((Timed) annotation, bean, element, metricsService);
                    }
                }
            }

            if (annotation.annotationType().isAssignableFrom(Counted.class)) {
                registerCountedMetric((Counted) annotation, bean, element, metricsService);
                for (Method method : superClassBean.getDeclaredMethods()) {
                    if (!method.isSynthetic() && !Modifier.isPrivate(method.getModifiers())) {
                        registerCountedMetric((Counted) annotation, bean, element, metricsService);
                    }
                }
            }

            if (annotation.annotationType().isAssignableFrom(Gauge.class)) {
                registerGaugeMetric((Gauge) annotation, bean, element, target, metricsService);
                for (Method method : superClassBean.getDeclaredMethods()) {
                    if (!method.isSynthetic()) {
                        registerGaugeMetric((Gauge) annotation, bean, element, target, metricsService);
                    }
                }
            }
        }
    }

    private <E extends Member & AnnotatedElement> void registerFromStereotypes(Set<Class<? extends Annotation>> stereotypes,
                                                                               Class<?> bean, E element, Object target,
                                                                               MetricsService metricsService) {
        for (Class<? extends Annotation> a : stereotypes) {
            Annotation[] array = a.getAnnotations();
            for (Annotation annotation : array) {
                registerFromAnnotation(annotation, bean, element, target, metricsService);
            }
        }
    }

    private <E extends Member & AnnotatedElement> void registerMetrics(Class<?> bean, E element, Object target, MetricsService metricsService) {
        Timed timedAnnotation = element.getAnnotation(Timed.class);
        Counted countedAnnotation = element.getAnnotation(Counted.class);
        Gauge gaugeAnnotation = element.getAnnotation(Gauge.class);

        if (timedAnnotation != null && !isMethodPrivate(element)) {
            registerTimedMetric(timedAnnotation, bean, element, metricsService);
        }

        if (countedAnnotation != null && !isMethodPrivate(element)) {
            registerCountedMetric(countedAnnotation, bean, element, metricsService);
        }

        if (gaugeAnnotation != null) {
            registerGaugeMetric(gaugeAnnotation, bean, element, target, metricsService);
        }

        //we need to check if current class is annotated, then apply metrics to all public methods
        if (timedAnnotation == null && countedAnnotation == null && gaugeAnnotation == null) {
            for (Annotation annotation : bean.getAnnotations()) {
                registerFromAnnotation(annotation, bean, element, target, metricsService);
            }
        }
    }

    /**
     * Method to identify if the method is private from given element
     *
     * @param element method to evaluate
     * @return boolean indicating if this is a private method
     */
    public static <E extends Member & AnnotatedElement> boolean isMethodPrivate(E element) {
        if (element instanceof Method && Modifier.isPrivate(((Method) element).getModifiers())) {
            return true;
        }
        return false;
    }

    /**
     * Method to evaluate which is the annotation used for the metric, options can be: Timed, Counted and Gauge
     *
     * @param annotation     The annotation used to register the metric
     * @param bean           source class where the annotation is used
     * @param element        constructor or method element where the annotation is going to be applied
     * @param target         context target managed by the interceptor
     * @param metricsService reference of the metrics service used to register the metrics
     */
    private <E extends Member & AnnotatedElement> void registerFromAnnotation(Annotation annotation, Class<?> bean,
                                                                              E element, Object target,
                                                                              MetricsService metricsService) {
        if (annotation.annotationType().isAssignableFrom(Timed.class) && !isMethodPrivate(element)) {
            registerTimedMetric((Timed) annotation, bean, element, metricsService);
        }

        if (annotation.annotationType().isAssignableFrom(Counted.class) && !isMethodPrivate(element)) {
            registerCountedMetric((Counted) annotation, bean, element, metricsService);
        }

        if (annotation.annotationType().isAssignableFrom(Gauge.class)) {
            registerGaugeMetric((Gauge) annotation, bean, element, target, metricsService);
        }
    }

    /**
     * Method to register Timed metric
     *
     * @param timedAnnotation the Timed annotation used to register the metric
     * @param bean            source class where the annotation is used
     * @param element         element to apply like constructor or method
     * @param metricsService  reference of the metrics service used to register the metrics
     */
    private <E extends Member & AnnotatedElement> void registerTimedMetric(Timed timedAnnotation, Class<?> bean,
                                                                           E element, MetricsService metricsService) {
        String scope = timedAnnotation.scope();
        if (scope != null) {
            final MetricRegistry metricRegistry = metricsService.getContext(true).getOrCreateRegistry(scope);
            register(bean, element, AnnotationReader.TIMED, metricRegistry::timer);
        } else {
            register(bean, element, AnnotationReader.TIMED, registry::timer);
        }
    }

    /**
     * Method to register Counted metric
     *
     * @param countedAnnotation the Counted annotation used to register the metric
     * @param bean              source class where the annotation is used
     * @param element           element to apply like constructor or method
     * @param metricsService    reference of the metrics service used to register the metrics
     */
    private <E extends Member & AnnotatedElement> void registerCountedMetric(Counted countedAnnotation, Class<?> bean,
                                                                             E element, MetricsService metricsService) {
        String scope = countedAnnotation.scope();
        if (scope != null) {
            final MetricRegistry metricRegistry = metricsService.getContext(true).getOrCreateRegistry(scope);
            register(bean, element, AnnotationReader.COUNTED, metricRegistry::counter);
        } else {
            register(bean, element, AnnotationReader.COUNTED, registry::counter);
        }
    }

    /**
     * Method to register a Gauge Metric
     *
     * @param gaugeAnnotation the Gauge annotation used to register the metric
     * @param bean            source class where the annotation is used
     * @param element         element to apply like constructor or method
     * @param target          context target managed by the interceptor
     * @param metricsService  reference of the metrics service used to register the metrics
     */
    private <E extends Member & AnnotatedElement> void registerGaugeMetric(Gauge gaugeAnnotation,
                                                                           Class<?> bean, E element,
                                                                           Object target, MetricsService metricsService) {
        String scope = gaugeAnnotation.scope();

        if (scope != null) {
            final MetricRegistry metricRegistry = metricsService.getContext(true).getOrCreateRegistry(scope);
            register(bean, element, AnnotationReader.GAUGE, (metadata, tags) ->
                    metricRegistry.gauge(metadata, new GaugeImpl<>((Method) element, target), tags));
        } else {
            register(bean, element, AnnotationReader.GAUGE, (metadata, tags) ->
                    registry.gauge(metadata, new GaugeImpl<>((Method) element, target), tags));
        }
    }

    private static <E extends Member & AnnotatedElement, T extends Annotation> void register(Class<?> bean, E element,
                                                                                             AnnotationReader<T> reader, BiConsumer<Metadata, Tag[]> register) {
        if (reader.isPresent(bean, element)) {
            register.accept(reader.metadata(bean, element), reader.tags(reader.annotation(bean, element)));
        }
    }

}

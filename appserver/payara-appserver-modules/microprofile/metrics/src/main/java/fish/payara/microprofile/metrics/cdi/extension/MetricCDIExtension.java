/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2018] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.metrics.cdi.extension;

import fish.payara.microprofile.metrics.MetricsService;
import fish.payara.microprofile.metrics.cdi.MetricsAnnotationBinding;
import fish.payara.microprofile.metrics.cdi.MetricsHelper;
import fish.payara.microprofile.metrics.cdi.MetricsResolver;
import fish.payara.microprofile.metrics.cdi.interceptor.CountedInterceptor;
import fish.payara.microprofile.metrics.cdi.interceptor.MeteredInterceptor;
import fish.payara.microprofile.metrics.cdi.interceptor.MetricsInterceptor;
import fish.payara.microprofile.metrics.cdi.interceptor.TimedInterceptor;
import fish.payara.microprofile.metrics.cdi.producer.MetricProducer;
import fish.payara.microprofile.metrics.cdi.producer.MetricRegistryProducer;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Logger;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.AnnotatedCallable;
import javax.enterprise.inject.spi.AnnotatedMember;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessBeanAttributes;
import javax.enterprise.inject.spi.ProcessProducer;
import javax.enterprise.inject.spi.Producer;
import javax.enterprise.inject.spi.WithAnnotations;
import javax.enterprise.util.AnnotationLiteral;
import javax.enterprise.util.Nonbinding;
import javax.interceptor.Interceptor;
import javax.interceptor.InterceptorBinding;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Gauge;
import org.eclipse.microprofile.metrics.annotation.Metered;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.glassfish.internal.api.Globals;

public class MetricCDIExtension<E extends Member & AnnotatedElement> implements Extension {

    private static final AnnotationLiteral<Nonbinding> NON_BINDING = new AnnotationLiteral<Nonbinding>() {
    };
    private static final AnnotationLiteral<InterceptorBinding> INTERCEPTOR_BINDING = new AnnotationLiteral<InterceptorBinding>() {
    };
    private static final AnnotationLiteral<MetricsAnnotationBinding> METRICS_ANNOTATION_BINDING = new AnnotationLiteral<MetricsAnnotationBinding>() {
    };
    private final Map<Producer<?>, AnnotatedMember<?>> producerMetrics = new HashMap<>();

    private final Map<String, E> annotatedElements = new HashMap<>();

    private final Map<String, Metadata> metadataMap = new HashMap<>();

    private final List<String> validationMessages = new ArrayList<>();
    
    private static final Logger LOGGER = Logger.getLogger(MetricCDIExtension.class.getName());

    private void beforeBeanDiscovery(@Observes BeforeBeanDiscovery beforeBeanDiscovery, BeanManager manager) {
        beforeBeanDiscovery.addQualifier(org.eclipse.microprofile.metrics.annotation.Metric.class);
        addInterceptorBinding(Counted.class, manager, beforeBeanDiscovery);
        addInterceptorBinding(Metered.class, manager, beforeBeanDiscovery);
        addInterceptorBinding(Timed.class, manager, beforeBeanDiscovery);

        addAnnotatedType(CountedInterceptor.class, manager, beforeBeanDiscovery);
        addAnnotatedType(MeteredInterceptor.class, manager, beforeBeanDiscovery);
        addAnnotatedType(TimedInterceptor.class, manager, beforeBeanDiscovery);
        addAnnotatedType(MetricsInterceptor.class, manager, beforeBeanDiscovery);

        addAnnotatedType(MetricProducer.class, manager, beforeBeanDiscovery);
        addAnnotatedType(MetricRegistryProducer.class, manager, beforeBeanDiscovery);

        addAnnotatedType(MetricsResolver.class, manager, beforeBeanDiscovery);
        addAnnotatedType(MetricsHelper.class, manager, beforeBeanDiscovery);
    }

    private <T> void metricsAnnotations(@Observes @WithAnnotations({Counted.class, Gauge.class, Metered.class, Timed.class}) ProcessAnnotatedType<T> processAnnotatedType) {
        processAnnotatedType.setAnnotatedType(new AnnotatedTypeDecorator<>(processAnnotatedType.getAnnotatedType(), METRICS_ANNOTATION_BINDING));
    }

    private <T> void validateMetrics(@Observes @WithAnnotations({Counted.class, Gauge.class, Metered.class, Timed.class}) ProcessAnnotatedType<T> processAnnotatedType) {
        AnnotatedType<?> annotatedType = processAnnotatedType.getAnnotatedType();
        List<AnnotatedCallable<?>> annotatedCallables = new ArrayList<>(annotatedType.getConstructors());
        annotatedCallables.addAll(annotatedType.getMethods());

        for (AnnotatedCallable<?> annotatedElement : annotatedCallables) {
            MetricsResolver resolver = new MetricsResolver();
            Class<?> bean = annotatedType.getJavaClass();
            E element = (E) annotatedElement.getJavaMember();
            if(bean.isAnnotationPresent(Interceptor.class)) {
                continue;
            }

            MetricsResolver.Of counted = resolver.counted(bean, element);
            if (counted.isPresent()) {
                validateMetricsElement(counted, element);
            }

            MetricsResolver.Of<Metered> metered = resolver.metered(bean, element);
            if (metered.isPresent()) {
                validateMetricsElement(metered, element);
            }

            MetricsResolver.Of<Timed> timed = resolver.timed(bean, element);
            if (timed.isPresent()) {
                validateMetricsElement(timed, element);
            }

            if (element instanceof Method
                    && element.isAnnotationPresent(org.eclipse.microprofile.metrics.annotation.Gauge.class)) {
                MetricsResolver.Of<Gauge> gauge = resolver.gauge(bean, (Method) element);
                if (gauge.isPresent()) {
                    validateMetricsElement(gauge, element);
                }
            }
        }
    }
    
    private void validateMetricsElement(MetricsResolver.Of metrics, E element) {
        Metadata metadata = metrics.metadata();
        E existingElement = annotatedElements.putIfAbsent(metrics.metricName(), element);
        Metadata existingMetadata = metadataMap.putIfAbsent(metrics.metricName(), metadata);
        if (null != existingElement && null != existingMetadata
                && metadata.getTypeRaw() != existingMetadata.getTypeRaw()) {
            String errorMessage;
            if (element instanceof Constructor) {
                errorMessage = String.format("Duplicate metric name[%s] found on elements [%s#%s] and [%s#%s]",
                        metrics.metricName(),
                        existingElement, existingMetadata.getType(),
                        element, metadata.getType()
                );
            } else {
                errorMessage = String.format("Duplicate metric name[%s] found on elements [%s.%s#%s] and [%s.%s#%s]",
                        metrics.metricName(),
                        existingElement.getDeclaringClass().getName(), existingElement.getName(), existingMetadata.getType(),
                        element.getDeclaringClass().getName(), element.getName(), metadata.getType()
                );
            }
            validationMessages.add(errorMessage);
        }
    }

    private <T extends Metric> void filterMetricsProducer(@Observes ProcessProducer<?, T> processProducer) {
        Type type = processProducer.getAnnotatedMember().getDeclaringType().getBaseType();
        if (!type.equals(MetricProducer.class)) {
            org.eclipse.microprofile.metrics.annotation.Metric metric
                    = processProducer.getAnnotatedMember().getAnnotation(org.eclipse.microprofile.metrics.annotation.Metric.class);

            if (metric != null) {
                producerMetrics.put(processProducer.getProducer(), processProducer.getAnnotatedMember());
            }
        }
    }

    public void vetoMetricsProducer(@Observes ProcessBeanAttributes<?> processBeanAttributes, BeanManager manager) {
        Type declaringType;
        if (processBeanAttributes.getAnnotated() instanceof AnnotatedMember) {
            AnnotatedMember annotatedMember = (AnnotatedMember) processBeanAttributes.getAnnotated();
            declaringType = annotatedMember.getDeclaringType().getBaseType();
        } else {
            declaringType = processBeanAttributes.getAnnotated().getBaseType();
        }

        if (declaringType != MetricProducer.class
                && processBeanAttributes.getAnnotated().isAnnotationPresent(org.eclipse.microprofile.metrics.annotation.Metric.class)
                && processBeanAttributes.getAnnotated().isAnnotationPresent(Produces.class)
                && processBeanAttributes.getBeanAttributes().getTypes().contains(Metric.class)) {
            processBeanAttributes.veto();
        }
    }

    private void validationError(@Observes AfterBeanDiscovery afterBeanDiscovery){
         for(String validationMessage : validationMessages) {
            LOGGER.severe(validationMessage);
        }
        annotatedElements.clear();
        metadataMap.clear();
        validationMessages.clear();
    }
    
    private void registerCustomMetrics(@Observes AfterDeploymentValidation afterDeploymentValidation, BeanManager manager) {
        MetricsService metricsService = Globals.getDefaultBaseServiceLocator().getService(MetricsService.class);
        MetricRegistry registry = metricsService.getOrAddRegistry(metricsService.getApplicationName());
        MetricsHelper helper = getReference(manager, MetricsHelper.class);
        for (Map.Entry<Producer<?>, AnnotatedMember<?>> entry : producerMetrics.entrySet()) {
            AnnotatedMember<?> annotatedMember = entry.getValue();
            Producer<?> prod = entry.getKey();
            if (hasInjectionPoints(annotatedMember)) {
                continue;
            }
            Metadata metadata = helper.metadataOf(annotatedMember);
            registry.register(metadata, (Metric) prod.produce(manager.createCreationalContext(null)));
        }
        producerMetrics.clear();
    }

    private <T extends Annotation> void addInterceptorBinding(Class<T> annotation, BeanManager manager, BeforeBeanDiscovery beforeBeanDiscovery) {
        AnnotatedType<T> annotated = manager.createAnnotatedType(annotation);
        Set<AnnotatedMethod<? super T>> methods = new HashSet<>();
        for (AnnotatedMethod<? super T> method : annotated.getMethods()) {
            methods.add(new AnnotatedMethodDecorator<>(method, NON_BINDING));
        }
        beforeBeanDiscovery.addInterceptorBinding(new AnnotatedTypeDecorator<>(annotated, INTERCEPTOR_BINDING, methods));
    }

    private <T extends Object> void addAnnotatedType(Class<T> type, BeanManager manager, BeforeBeanDiscovery beforeBeanDiscovery) {
        beforeBeanDiscovery.addAnnotatedType(manager.createAnnotatedType(type), type.getName());
    }

    private <T> T getReference(BeanManager manager, Class<T> type) {
        return getReference(manager, type, manager.resolve(manager.getBeans(type)));
    }

    private <T> T getReference(BeanManager manager, Type type, Bean<?> bean) {
        
        return (T) manager.getReference(bean, type, manager.createCreationalContext(bean));
    }

    private boolean hasInjectionPoints(AnnotatedMember<?> member) {
        if (!(member instanceof AnnotatedMethod)) {
            return false;
        }
        AnnotatedMethod<?> method = (AnnotatedMethod<?>) member;
        for (AnnotatedParameter<?> parameter : method.getParameters()) {
            if (parameter.getBaseType().equals(InjectionPoint.class)) {
                return true;
            }
        }
        return false;
    }

}

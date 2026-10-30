/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2018-2023] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.metrics.cdi.extension;

import fish.payara.microprofile.metrics.MetricsService;
import fish.payara.microprofile.metrics.cdi.AnnotationReader;
import fish.payara.microprofile.metrics.cdi.MetricsAnnotationBinding;
import fish.payara.microprofile.metrics.cdi.interceptor.CountedInterceptor;
import fish.payara.microprofile.metrics.cdi.interceptor.MetricsInterceptor;
import fish.payara.microprofile.metrics.cdi.interceptor.TimedInterceptor;
import fish.payara.microprofile.metrics.cdi.producer.MetricProducer;
import fish.payara.microprofile.metrics.cdi.producer.MetricRegistryProducer;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.AnnotatedCallable;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;
import jakarta.enterprise.inject.spi.WithAnnotations;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.enterprise.util.Nonbinding;
import jakarta.interceptor.Interceptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Gauge;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.glassfish.internal.api.Globals;

public class MetricCDIExtension<E extends Member & AnnotatedElement> implements Extension {

    private static final AnnotationLiteral<Nonbinding> NON_BINDING = new AnnotationLiteral<Nonbinding>() {
    };
    private static final AnnotationLiteral<MetricsAnnotationBinding> METRICS_ANNOTATION_BINDING = new AnnotationLiteral<MetricsAnnotationBinding>() {
    };
    private final Map<String, E> annotatedElements = new HashMap<>();

    private final Map<String, Metadata> metadataMap = new HashMap<>();

    private final List<String> validationMessages = new ArrayList<>();

    private MetricsService metricsService;

    private MetricsService.MetricsContext metricsContext;

    void beforeBeanDiscovery(@Observes BeforeBeanDiscovery beforeBeanDiscovery, BeanManager manager) {
        addNonbindingAnnotation(Counted.class, beforeBeanDiscovery);
        addNonbindingAnnotation(Timed.class, beforeBeanDiscovery);
        addNonbindingAnnotation(Gauge.class, beforeBeanDiscovery);

        addAnnotatedType(CountedInterceptor.class, manager, beforeBeanDiscovery);
        addAnnotatedType(TimedInterceptor.class, manager, beforeBeanDiscovery);
        addAnnotatedType(MetricsInterceptor.class, manager, beforeBeanDiscovery);

        addAnnotatedType(MetricProducer.class, manager, beforeBeanDiscovery);
        addAnnotatedType(MetricRegistryProducer.class, manager, beforeBeanDiscovery);
    }

    <T> void metricsAnnotations(@Observes @WithAnnotations({ Counted.class, Gauge.class,
            Timed.class}) ProcessAnnotatedType<T> processAnnotatedType) {
        processAnnotatedType.configureAnnotatedType().add(METRICS_ANNOTATION_BINDING);
    }

    <T> void validateMetrics(@Observes @WithAnnotations({ Counted.class, Gauge.class,
            Timed.class }) ProcessAnnotatedType<T> processAnnotatedType) {
        AnnotatedType<?> annotatedType = processAnnotatedType.getAnnotatedType();
        List<AnnotatedCallable<?>> annotatedCallables = new ArrayList<>(annotatedType.getConstructors());
        annotatedCallables.addAll(annotatedType.getMethods());

        Class<?> bean = annotatedType.getJavaClass();
        for (AnnotatedCallable<?> annotatedElement : annotatedCallables) {
            if(!bean.isAnnotationPresent(Interceptor.class)) {
                @SuppressWarnings("unchecked")
                E element = (E) annotatedElement.getJavaMember();
                validateAnnotated(element, bean);
            }
        }
    }

    private void validateAnnotated(E element, Class<?> bean) {
        for (AnnotationReader<?> reader : AnnotationReader.readers()) {
            validateAnnotated(element, bean, reader);
        }
    }

    private <T extends Annotation> void validateAnnotated(E element, Class<?> bean, AnnotationReader<T> reader) {
        if (!reader.isPresent(bean, element)) {
            return;
        }
        Metadata metadata = reader.metadata(bean, element);
        String name = metadata.getName();
        annotatedElements.putIfAbsent(name, element);
        metadataMap.putIfAbsent(name, metadata);
        initService();
        if (reader.annotationType().getName().equals(Timed.class.getName()) && 
                !MetricsInterceptor.isMethodPrivate(element)) {
            String availableScope = reader.scope(reader.annotation(bean, element));
            Tag[] tags = reader.tags(reader.annotation(bean, element));
            metricsService.getContext(true).
                    getOrCreateRegistry((availableScope != null) ? availableScope : "application").timer(metadata, tags);
        }
    }

    void validationError(@Observes AfterBeanDiscovery afterBeanDiscovery){
        validationMessages.forEach(message -> afterBeanDiscovery.addDefinitionError(new IllegalStateException(message)));
        annotatedElements.clear();
        metadataMap.clear();
        validationMessages.clear();
    }

    private static <T extends Annotation> void addNonbindingAnnotation(Class<T> annotation, BeforeBeanDiscovery beforeBeanDiscovery) {
        beforeBeanDiscovery.configureInterceptorBinding(annotation)
                .filterMethods(method -> !method.isAnnotationPresent(Nonbinding.class))
                .forEach(method -> method.add(NON_BINDING));
    }

    private static <T extends Object> void addAnnotatedType(Class<T> type, BeanManager manager, BeforeBeanDiscovery beforeBeanDiscovery) {
        beforeBeanDiscovery.addAnnotatedType(manager.createAnnotatedType(type), type.getName());
    }


    private void initService() {
        if (metricsService == null) {
            metricsService = Globals.getDefaultBaseServiceLocator().getService(MetricsService.class);
            if (metricsService.isEnabled()) {
                metricsContext = metricsService.getContext(true);
            }
        }
    }
    
}

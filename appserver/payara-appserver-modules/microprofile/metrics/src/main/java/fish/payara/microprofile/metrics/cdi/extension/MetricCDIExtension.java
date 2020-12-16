/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2018-2020] Payara Foundation and/or its affiliates. All rights reserved.
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

import fish.payara.microprofile.metrics.cdi.MetricsAnnotationBinding;
import fish.payara.microprofile.metrics.cdi.AnnotationReader;
import fish.payara.microprofile.metrics.cdi.interceptor.ConcurrentGaugeInterceptor;
import fish.payara.microprofile.metrics.cdi.interceptor.CountedInterceptor;
import fish.payara.microprofile.metrics.cdi.interceptor.MeteredInterceptor;
import fish.payara.microprofile.metrics.cdi.interceptor.MetricsInterceptor;
import fish.payara.microprofile.metrics.cdi.interceptor.SimplyTimedInterceptor;
import fish.payara.microprofile.metrics.cdi.interceptor.TimedInterceptor;
import fish.payara.microprofile.metrics.cdi.producer.MetricProducer;
import fish.payara.microprofile.metrics.cdi.producer.MetricRegistryProducer;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AnnotatedCallable;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.WithAnnotations;
import javax.enterprise.util.AnnotationLiteral;
import javax.enterprise.util.Nonbinding;
import javax.interceptor.Interceptor;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.annotation.ConcurrentGauge;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Gauge;
import org.eclipse.microprofile.metrics.annotation.Metered;
import org.eclipse.microprofile.metrics.annotation.SimplyTimed;
import org.eclipse.microprofile.metrics.annotation.Timed;

public class MetricCDIExtension<E extends Member & AnnotatedElement> implements Extension {

    private static final AnnotationLiteral<Nonbinding> NON_BINDING = new AnnotationLiteral<Nonbinding>() {
    };
    private static final AnnotationLiteral<MetricsAnnotationBinding> METRICS_ANNOTATION_BINDING = new AnnotationLiteral<MetricsAnnotationBinding>() {
    };
    private final Map<String, E> annotatedElements = new HashMap<>();

    private final Map<String, Metadata> metadataMap = new HashMap<>();

    private final List<String> validationMessages = new ArrayList<>();

    void beforeBeanDiscovery(@Observes BeforeBeanDiscovery beforeBeanDiscovery, BeanManager manager) {
        addNonbindingAnnotation(Counted.class, beforeBeanDiscovery);
        addNonbindingAnnotation(ConcurrentGauge.class, beforeBeanDiscovery);
        addNonbindingAnnotation(Metered.class, beforeBeanDiscovery);
        addNonbindingAnnotation(Timed.class, beforeBeanDiscovery);
        addNonbindingAnnotation(SimplyTimed.class, beforeBeanDiscovery);
        addNonbindingAnnotation(Gauge.class, beforeBeanDiscovery);
//
        addAnnotatedType(CountedInterceptor.class, manager, beforeBeanDiscovery);
        addAnnotatedType(ConcurrentGaugeInterceptor.class, manager, beforeBeanDiscovery);
        addAnnotatedType(MeteredInterceptor.class, manager, beforeBeanDiscovery);
        addAnnotatedType(TimedInterceptor.class, manager, beforeBeanDiscovery);
        addAnnotatedType(MetricsInterceptor.class, manager, beforeBeanDiscovery);
        addAnnotatedType(SimplyTimedInterceptor.class, manager, beforeBeanDiscovery);

        addAnnotatedType(MetricProducer.class, manager, beforeBeanDiscovery);
        addAnnotatedType(MetricRegistryProducer.class, manager, beforeBeanDiscovery);
    }

    <T> void metricsAnnotations(@Observes @WithAnnotations({ Counted.class, ConcurrentGauge.class, Gauge.class,
            Metered.class, Timed.class, SimplyTimed.class }) ProcessAnnotatedType<T> processAnnotatedType) {
        processAnnotatedType.configureAnnotatedType().add(METRICS_ANNOTATION_BINDING);
    }

    <T> void validateMetrics(@Observes @WithAnnotations({ Counted.class, ConcurrentGauge.class, Gauge.class,
            Metered.class, Timed.class, SimplyTimed.class }) ProcessAnnotatedType<T> processAnnotatedType) {
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
            if (reader.type() != MetricType.INVALID) {
                validateAnnotated(element, bean, reader);
            }
        }
    }

    private <T extends Annotation> void validateAnnotated(E element, Class<?> bean, AnnotationReader<T> reader) {
        if (!reader.isPresent(bean, element)) {
            return;
        }
        Metadata metadata = reader.metadata(bean, element);
        String name = metadata.getName();
        E existingElement = annotatedElements.putIfAbsent(name, element);
        Metadata existingMetadata = metadataMap.putIfAbsent(name, metadata);
        if (null != existingElement && null != existingMetadata
                && metadata.getTypeRaw() != existingMetadata.getTypeRaw()) {
            String errorMessage;
            if (element instanceof Constructor) {
                errorMessage = String.format("Duplicate metric name[%s] found on elements [%s#%s] and [%s#%s]",
                        name,
                        existingElement, existingMetadata.getType(),
                        element, metadata.getType()
                );
            } else {
                errorMessage = String.format("Duplicate metric name[%s] found on elements [%s.%s#%s] and [%s.%s#%s]",
                        name,
                        existingElement.getDeclaringClass().getName(), existingElement.getName(), existingMetadata.getType(),
                        element.getDeclaringClass().getName(), element.getName(), metadata.getType()
                );
            }
            validationMessages.add(errorMessage);
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

}

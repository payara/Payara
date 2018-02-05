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
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
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
import javax.interceptor.InterceptorBinding;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Gauge;
import org.eclipse.microprofile.metrics.annotation.Metered;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.glassfish.internal.api.Globals;

public class MetricCDIExtension implements Extension {

    private static final AnnotationLiteral<Nonbinding> NON_BINDING = new AnnotationLiteral<Nonbinding>() {
    };
    private static final AnnotationLiteral<InterceptorBinding> INTERCEPTOR_BINDING = new AnnotationLiteral<InterceptorBinding>() {
    };
    private static final AnnotationLiteral<MetricsAnnotationBinding> METRICS_ANNOTATION_BINDING = new AnnotationLiteral<MetricsAnnotationBinding>() {
    };
    private final Map<Producer<?>, AnnotatedMember<?>> metrics = new HashMap<>();

    private void beforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd, BeanManager manager) {
        bbd.addQualifier(org.eclipse.microprofile.metrics.annotation.Metric.class);
        addInterceptorBinding(Counted.class, manager, bbd);
        addInterceptorBinding(Metered.class, manager, bbd);
        addInterceptorBinding(Timed.class, manager, bbd);

        addAnnotatedType(CountedInterceptor.class, manager, bbd);
        addAnnotatedType(MeteredInterceptor.class, manager, bbd);
        addAnnotatedType(TimedInterceptor.class, manager, bbd);
        addAnnotatedType(MetricsInterceptor.class, manager, bbd);

        addAnnotatedType(MetricProducer.class, manager, bbd);
        addAnnotatedType(MetricRegistryProducer.class, manager, bbd);

        addAnnotatedType(MetricsResolver.class, manager, bbd);
        addAnnotatedType(MetricsHelper.class, manager, bbd);
    }

    private <T> void metricsAnnotations(@Observes @WithAnnotations({Counted.class, Gauge.class, Metered.class, Timed.class}) ProcessAnnotatedType<T> pat) {
        pat.setAnnotatedType(new AnnotatedTypeDecorator<>(pat.getAnnotatedType(), METRICS_ANNOTATION_BINDING));
    }

    private <T extends Metric> void filterMetricsProducer(@Observes ProcessProducer<?, T> pp) {
        Type type = pp.getAnnotatedMember().getDeclaringType().getBaseType();
        if (!type.equals(MetricProducer.class)) {
            org.eclipse.microprofile.metrics.annotation.Metric m
                    = pp.getAnnotatedMember().getAnnotation(org.eclipse.microprofile.metrics.annotation.Metric.class);

            if (m != null) {
                metrics.put(pp.getProducer(), pp.getAnnotatedMember());
            }
        }
    }

    public void vetoMetricsProducer(@Observes ProcessBeanAttributes<?> pba, BeanManager manager) {
        Type declaringType;
        if (pba.getAnnotated() instanceof AnnotatedMember) {
            AnnotatedMember annotatedMember = (AnnotatedMember) pba.getAnnotated();
            declaringType = annotatedMember.getDeclaringType().getBaseType();
        } else {
            declaringType = pba.getAnnotated().getBaseType();
        }

        if (declaringType != MetricProducer.class
                && pba.getAnnotated().isAnnotationPresent(org.eclipse.microprofile.metrics.annotation.Metric.class)
                && pba.getAnnotated().isAnnotationPresent(Produces.class)
                && pba.getBeanAttributes().getTypes().contains(Metric.class)) {
            pba.veto();
        }
    }

    private void registerCustomMetrics(@Observes AfterDeploymentValidation adv, BeanManager manager) {
        MetricsService metricsService = Globals.getDefaultBaseServiceLocator().getService(MetricsService.class);
        MetricRegistry registry = metricsService.getOrAddRegistry(metricsService.getApplicationName());
        MetricsHelper helper = getReference(manager, MetricsHelper.class);
        for (Map.Entry<Producer<?>, AnnotatedMember<?>> entry : metrics.entrySet()) {
            AnnotatedMember<?> annotatedMember = entry.getValue();
            Producer<?> prod = entry.getKey();
            if (hasInjectionPoints(annotatedMember)) {
                continue;
            }
            Metadata metadata = helper.metadataOf(annotatedMember);
            registry.register(metadata.getName(),
                    (Metric) prod.produce(manager.createCreationalContext(null)), metadata);
        }
        metrics.clear();
    }

    private <T extends Annotation> void addInterceptorBinding(Class<T> annotation, BeanManager manager, BeforeBeanDiscovery bbd) {
        AnnotatedType<T> annotated = manager.createAnnotatedType(annotation);
        Set<AnnotatedMethod<? super T>> methods = new HashSet<>();
        for (AnnotatedMethod<? super T> method : annotated.getMethods()) {
            methods.add(new AnnotatedMethodDecorator<>(method, NON_BINDING));
        }
        bbd.addInterceptorBinding(new AnnotatedTypeDecorator<>(annotated, INTERCEPTOR_BINDING, methods));
    }

    private <T extends Object> void addAnnotatedType(Class<T> type, BeanManager manager, BeforeBeanDiscovery bbd) {
        bbd.addAnnotatedType(manager.createAnnotatedType(type), type.getName());
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

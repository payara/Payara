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

package fish.payara.microprofile.metrics.cdi.interceptor;

import fish.payara.microprofile.metrics.MetricsService;
import fish.payara.microprofile.metrics.cdi.MetricsAnnotationBinding;
import fish.payara.microprofile.metrics.cdi.AnnotationReader;
import fish.payara.microprofile.metrics.impl.GaugeImpl;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.BiConsumer;

import javax.annotation.Priority;
import javax.enterprise.inject.Intercepted;
import javax.enterprise.inject.spi.Bean;
import javax.inject.Inject;
import javax.interceptor.AroundConstruct;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;
import org.glassfish.internal.api.Globals;

@Interceptor
@MetricsAnnotationBinding
@Priority(Interceptor.Priority.LIBRARY_BEFORE)
public class MetricsInterceptor {

    private MetricRegistry registry;

    private Bean<?> bean;

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
            registerMetrics(beanClass, context.getConstructor(), context.getTarget());

            target = context.proceed();

            Class<?> type = beanClass;
            do {
                for (Method method : type.getDeclaredMethods()) {
                    if (!method.isSynthetic() && !Modifier.isPrivate(method.getModifiers())) {
                        registerMetrics(beanClass, method, context.getTarget());
                    }
                }
                type = type.getSuperclass();
            } while (!Object.class.equals(type));
        } else {
            target = context.proceed();
        }
        return target;
    }

    private <E extends Member & AnnotatedElement> void registerMetrics(Class<?> bean, E element, Object target) {
        register(bean, element, AnnotationReader.COUNTED, registry::counter);
        register(bean, element, AnnotationReader.CONCURRENT_GAUGE, registry::concurrentGauge);
        register(bean, element, AnnotationReader.METERED, registry::meter);
        register(bean, element, AnnotationReader.TIMED, registry::timer);
        register(bean, element, AnnotationReader.SIMPLY_TIMED, registry::simpleTimer);
        register(bean, element, AnnotationReader.GAUGE, (metadata, tags) ->
            registry.gauge(metadata, new GaugeImpl<>((Method) element, target), tags));
    }

    private static <E extends Member & AnnotatedElement, T extends Annotation> void register(Class<?> bean, E element,
            AnnotationReader<T> reader, BiConsumer<Metadata, Tag[]> register) {
        if (reader.isPresent(bean, element)) {
            register.accept(reader.metadata(bean, element), reader.tags(reader.annotation(bean, element)));
        }
    }

}

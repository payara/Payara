/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2018-2019] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.metrics.cdi;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedMember;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.InjectionPoint;
import org.eclipse.microprofile.metrics.ConcurrentGauge;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetadataBuilder;
import org.eclipse.microprofile.metrics.Meter;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.Timer;
import org.eclipse.microprofile.metrics.annotation.Metric;

@ApplicationScoped
public class MetricsHelper {

    public String metricNameOf(InjectionPoint ip) {
        Annotated annotated = ip.getAnnotated();
        if (annotated instanceof AnnotatedMember) {
            return metricNameOf((AnnotatedMember<?>) annotated);
        } else if (annotated instanceof AnnotatedParameter) {
            return metricNameOf((AnnotatedParameter<?>) annotated);
        } else {
            throw new IllegalArgumentException("Unable to retrieve metric name for injection point [" + ip + "], only members and parameters are supported");
        }
    }

    private String metricNameOf(AnnotatedMember<?> member) {
        if (member.isAnnotationPresent(Metric.class)) {
            Metric metric = member.getAnnotation(Metric.class);
            String name = metric.name().isEmpty() ? member.getJavaMember().getName() : metric.name();
            return metric.absolute() ? name : MetricRegistry.name(member.getJavaMember().getDeclaringClass(), name);
        } else {
            String name = member.getJavaMember().getName();
            return MetricRegistry.name(member.getJavaMember().getDeclaringClass(), name);
        }
    }

    private String metricNameOf(AnnotatedParameter<?> parameter) {
        if (parameter.isAnnotationPresent(Metric.class)) {
            Metric metric = parameter.getAnnotation(Metric.class);
            String name = metric.name().isEmpty() ? getParameterName(parameter) : metric.name();
            return metric.absolute() ? name : MetricRegistry.name(parameter.getDeclaringCallable().getJavaMember().getDeclaringClass(), name);
        } else {
            String name = getParameterName(parameter);
            return MetricRegistry.name(parameter.getDeclaringCallable().getJavaMember().getDeclaringClass(), name);
        }
    }

    private String getParameterName(AnnotatedParameter<?> parameter) {
        try {
            Method method = Method.class.getMethod("getParameters");
            Object[] parameters = (Object[]) method.invoke(parameter.getDeclaringCallable().getJavaMember());
            Object param = parameters[parameter.getPosition()];
            Class<?> Parameter = Class.forName("java.lang.reflect.Parameter");
            if ((Boolean) Parameter.getMethod("isNamePresent").invoke(param)) {
                return (String) Parameter.getMethod("getName").invoke(param);
            } else {
                throw new UnsupportedOperationException("Unable to retrieve name for parameter [" + parameter + "], activate the -parameters compiler argument or annotate the injected parameter with the @Metric annotation");
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassNotFoundException cause) {
            throw new UnsupportedOperationException("Unable to retrieve name for parameter [" + parameter + "], @Metric annotation on injected parameter is required before Java 8");
        }
    }

    public Metadata metadataOf(InjectionPoint ip) {
        Annotated annotated = ip.getAnnotated();
        return metadataOf(annotated, ip.getMember().getDeclaringClass().getCanonicalName(), ip.getMember().getName());
    }

    public Metadata metadataOf(AnnotatedMember<?> member) {
        return metadataOf(member, member.getJavaMember().getDeclaringClass().getCanonicalName(), member.getJavaMember().getName());
    }

    private Metadata metadataOf(Annotated annotated, String enclosingClass, String memberName) {
        MetadataBuilder metadataBuilder = Metadata.builder();
        Metric metric = annotated.getAnnotation(Metric.class);
        metadataBuilder = metadataBuilder.withDescription(metric.description())
            .withDisplayName(metric.displayName())
            .withUnit(metric.unit());
        if (metric.absolute()) {
            if (metric.name().isEmpty()) {
                metadataBuilder = metadataBuilder.withName(memberName);
            } else {
                metadataBuilder = metadataBuilder.withName(metric.name());
            }
        } else {
            if (metric.name().isEmpty()) {
                metadataBuilder = metadataBuilder.withName(enclosingClass + '.' + memberName);
            } else {
                metadataBuilder = metadataBuilder.withName(enclosingClass + '.' + metric.name());
            }
        }
        setMetricType(metadataBuilder, annotated);
        metadataBuilder = metadataBuilder.notReusable();
        return metadataBuilder.build();
    }
    
    private MetadataBuilder setMetricType(MetadataBuilder builder, Annotated annotated) {
        String typeName = annotated.getBaseType().getTypeName();
        if (typeName.startsWith(Gauge.class.getName())) {
            builder.withType(MetricType.GAUGE);
        } else if (typeName.startsWith(ConcurrentGauge.class.getName())) {
            builder.withType(MetricType.CONCURRENT_GAUGE);
        } else if (typeName.startsWith(Counter.class.getName())) {
            builder.withType(MetricType.COUNTER);
        } else if (typeName.startsWith(Meter.class.getName())) {
            builder.withType(MetricType.METERED);
        } else if (typeName.startsWith(Histogram.class.getName())) {
            builder.withType(MetricType.HISTOGRAM);
        } else if (typeName.startsWith(Timer.class.getName())) {
            builder.withType(MetricType.TIMER);
        }
        return builder;
    }
    
    public MetricID metricIDOf(InjectionPoint ip) {
        Annotated annotated = ip.getAnnotated();
        if (annotated instanceof AnnotatedMember) {
            return metricIDOf((AnnotatedMember<?>) annotated, ip.getMember().getDeclaringClass());
        } else if (annotated instanceof AnnotatedParameter) {
            return metricIDOf((AnnotatedParameter<?>) annotated, ip.getMember().getDeclaringClass());
        } else {
            throw new IllegalArgumentException("Unable to retrieve metric name for injection point [" + ip + "], only members and parameters are supported");
        }
    }
    
    public MetricID metricIDOf(Annotated member, Class baseClass) {
        if (member.isAnnotationPresent(Metric.class)) {
            Metric metric = member.getAnnotation(Metric.class);
            if (metric.absolute()) {
                return new MetricID(metric.name(), tagsFromString(metric.tags()));
            } else {
                return new MetricID(baseClass + "." + metric.name(), tagsFromString(metric.tags()));
            }
        } 
        return null;
    }
    
    public static Tag[] tagsFromString(String[] stringtags) {
        Tag[] tags = new Tag[stringtags.length];
        for (int i = 0; i < stringtags.length; i++) {
            int splitIndex = stringtags[i].indexOf('=');
            if (splitIndex == -1) {
                throw new IllegalArgumentException("invalid tag: " + stringtags[i] + ", tags must be in the form key=value");
            } else {
                tags[i] = new Tag(stringtags[i].substring(0, splitIndex), stringtags[i].substring(splitIndex + 1));
            }
        }
        return tags;
    }

}

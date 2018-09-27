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
package fish.payara.microprofile.metrics.cdi;

import fish.payara.microprofile.metrics.Tag;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import java.util.stream.Stream;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedMember;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.InjectionPoint;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Metadata;
import static org.eclipse.microprofile.metrics.Metadata.GLOBAL_TAGS_VARIABLE;
import org.eclipse.microprofile.metrics.Meter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.Timer;
import org.eclipse.microprofile.metrics.annotation.Metric;

@ApplicationScoped
public class MetricsHelper {

    public static String getGlobalTagsString() {
        return System.getenv(GLOBAL_TAGS_VARIABLE);
    }

    public static List<Tag> getGlobalTags() {
        return convertToTags(getGlobalTagsString());
    }

    public static Map<String, String> getGlobalTagsMap() {
        return convertToMap(getGlobalTagsString());
    }

    private static List<Tag> convertToTags(String tagsString) {
        List<Tag> tags = Collections.emptyList();
        if (tagsString != null) {
            String[] singleTags = tagsString.split(",");
            tags = Stream.of(singleTags)
                    .map(tag -> tag.trim())
                    .map(tag -> new Tag(tag))
                    .collect(toList());
        }
        return tags;
    }

    private static Map<String, String> convertToMap(String tagsString) {
        Map<String, String> tags = Collections.EMPTY_MAP;
        if (tagsString != null) {
            String[] singleTags = tagsString.split(",");
            tags = Arrays.stream(singleTags)
                    .map(Tag::of)
                    .collect(toMap(Entry::getKey, Entry::getValue));
        }
        return tags;
    }

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

    public Metadata metadataOf(InjectionPoint ip, Class<?> type) {
        Annotated annotated = ip.getAnnotated();
        String name = metricNameOf(ip);
        return metadataOf(annotated, type, name);
    }

    public Metadata metadataOf(AnnotatedMember<?> member) {
        String typeName = member.getBaseType().getTypeName();
        if (typeName.startsWith(Gauge.class.getName())) {
            return metadataOf(member, Gauge.class);
        } else if (typeName.startsWith(Counter.class.getName())) {
            return metadataOf(member, Counter.class);
        } else if (typeName.startsWith(Meter.class.getName())) {
            return metadataOf(member, Meter.class);
        } else if (typeName.startsWith(Histogram.class.getName())) {
            return metadataOf(member, Histogram.class);
        } else if (typeName.startsWith(Timer.class.getName())) {
            return metadataOf(member, Timer.class);
        }
        return null;
    }

    private Metadata metadataOf(AnnotatedMember<?> member, Class<?> type) {
        return metadataOf(member, type, metricNameOf(member));
    }

    private Metadata metadataOf(Annotated annotated, Class<?> type, String name) {
        Metadata metadata = new Metadata(name, MetricType.from(type));
        if (annotated.isAnnotationPresent(Metric.class)) {
            Metric metric = annotated.getAnnotation(Metric.class);
            metadata.setDescription(metric.description() == null || metric.description().trim().isEmpty() ? null
                    : metric.description());
            metadata.setDisplayName(metric.displayName() == null || metric.displayName().trim().isEmpty() ? null
                    : metric.displayName());
            metadata.setUnit(metric.unit() == null || metric.unit().trim().isEmpty() ? null
                    : metric.unit());
            for (String tag : metric.tags()) {
                metadata.addTag(tag);
            }
        }
        return metadata;
    }

}

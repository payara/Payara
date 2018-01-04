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

package fish.payara.microprofile.metrics.cdi;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Vetoed;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Gauge;
import org.eclipse.microprofile.metrics.annotation.Metered;
import org.eclipse.microprofile.metrics.annotation.Timed;

@ApplicationScoped
public class MetricResolver {

    public <E extends Member & AnnotatedElement> Of<Counted> counted(Class<?> bean, E element) {
        return resolverOf(bean, element, Counted.class);
    }

    public Of<Gauge> gauge(Class<?> bean, Method method) {
        return resolverOf(bean, method, Gauge.class);
    }

    public <E extends Member & AnnotatedElement> Of<Metered> metered(Class<?> bean, E element) {
        return resolverOf(bean, element, Metered.class);
    }

    public <E extends Member & AnnotatedElement> Of<Timed> timed(Class<?> bean, E element) {
        return resolverOf(bean, element, Timed.class);
    }

    private <E extends Member & AnnotatedElement, T extends Annotation> Of<T> resolverOf(Class<?> bean, E element, Class<T> metric) {
        if (element.isAnnotationPresent(metric)) {
            return elementResolverOf(element, metric);
        } else {
            return beanResolverOf(bean, element, metric);
        }
    }

    private <E extends Member & AnnotatedElement, T extends Annotation> Of<T> elementResolverOf(E element, Class<T> metric) {
        T annotation = element.getAnnotation(metric);
        String name = metricName(element, annotation);
        return new ValidMetric<>(annotation, name, getMetadata(name, annotation));
    }

    private <E extends Member & AnnotatedElement, T extends Annotation> Of<T> beanResolverOf(Class<?> bean, E element, Class<T> metric) {
        if (bean.isAnnotationPresent(metric)) {
            T annotation = bean.getAnnotation(metric);
            String name = metricName(bean, element, annotation);
            return new ValidMetric<>(annotation, name, getMetadata(name, annotation));
        } else if (bean.getSuperclass() != null) {
            return beanResolverOf(bean.getSuperclass(), element, metric);
        }
        return new InvalidMetric<>();
    }

    private <E extends Member & AnnotatedElement> String metricName(E element, Annotation annotation) {
        String name = metricName(annotation);
        String metric = name.isEmpty() ? defaultName(element) : name;
        return isMetricAbsolute(annotation) ? metric 
                : MetricRegistry.name(element.getDeclaringClass(), metric);
    }

    private <E extends Member & AnnotatedElement> String metricName(Class<?> bean, E element, Annotation annotation) {
        String name = metricName(annotation);
        String metric = name.isEmpty() ? bean.getSimpleName() : name;
        return isMetricAbsolute(annotation) ? MetricRegistry.name(metric, defaultName(element)) 
                : MetricRegistry.name(bean.getPackage().getName(), metric, defaultName(element));
    }

    private <E extends Member & AnnotatedElement> String defaultName(E element) {
      if (element instanceof Constructor) {
            return element.getDeclaringClass().getSimpleName();
        } else {
            return element.getName();
        } 
    }

    private String metricName(Annotation annotation) {
        if (Counted.class.isInstance(annotation)) {
            return ((Counted) annotation).name();
        } else if (Gauge.class.isInstance(annotation)) {
            return ((Gauge) annotation).name();
        } else if (Metered.class.isInstance(annotation)) {
            return ((Metered) annotation).name();
        } else if (Timed.class.isInstance(annotation)) {
            return ((Timed) annotation).name();
        } else {
            throw new IllegalArgumentException("Unsupported Metrics forMethod [" + annotation.getClass().getName() + "]");
        }
    }

    private boolean isMetricAbsolute(Annotation annotation) {
        if (Counted.class.isInstance(annotation)) {
            return ((Counted) annotation).absolute();
        } else if (Gauge.class.isInstance(annotation)) {
            return ((Gauge) annotation).absolute();
        } else if (Metered.class.isInstance(annotation)) {
            return ((Metered) annotation).absolute();
        } else if (Timed.class.isInstance(annotation)) {
            return ((Timed) annotation).absolute();
        } else {
            throw new IllegalArgumentException("Unsupported Metrics forMethod [" + annotation.getClass().getName() + "]");
        }
    }
    
    private <T extends Annotation> Metadata getMetadata(String name, T annotation) {
        Metadata metadata = new Metadata(name, this.getType(annotation), this.getUnit(annotation));
        metadata.setDescription(this.getDescription(annotation));
        metadata.setDisplayName(this.getDisplayname(annotation));
        for (String tag : this.getTags(annotation)) {
            metadata.addTag(tag);
        }
        return metadata;
    }
    
    private String[] getTags(Annotation annotation) {
        if (Counted.class.isInstance(annotation))
            return ((Counted) annotation).tags();
        else if (Gauge.class.isInstance(annotation))
            return ((Gauge) annotation).tags();
        else if (Metered.class.isInstance(annotation))
            return ((Metered) annotation).tags();
        else if (Timed.class.isInstance(annotation))
            return ((Timed) annotation).tags();
        else
            throw new IllegalArgumentException("Unsupported Metrics forMethod [" + annotation.getClass().getName() + "]");

    }

    private String getDisplayname(Annotation annotation) {
        if (Counted.class.isInstance(annotation))
            return ((Counted) annotation).displayName();
        else if (Gauge.class.isInstance(annotation))
            return ((Gauge) annotation).displayName();
        else if (Metered.class.isInstance(annotation))
            return ((Metered) annotation).displayName();
        else if (Timed.class.isInstance(annotation))
            return ((Timed) annotation).displayName();
        else
            throw new IllegalArgumentException("Unsupported Metrics forMethod [" + annotation.getClass().getName() + "]");

    }

    private String getDescription(Annotation annotation) {
        if (Counted.class.isInstance(annotation))
            return ((Counted) annotation).description();
        else if (Gauge.class.isInstance(annotation))
            return ((Gauge) annotation).description();
        else if (Metered.class.isInstance(annotation))
            return ((Metered) annotation).description();
        else if (Timed.class.isInstance(annotation))
            return ((Timed) annotation).description();
        else
            throw new IllegalArgumentException("Unsupported Metrics forMethod [" + annotation.getClass().getName() + "]");
    }

    private MetricType getType(Annotation annotation) {
        if (Counted.class.isInstance(annotation))
            return MetricType.COUNTER;
        else if (Gauge.class.isInstance(annotation))
            return MetricType.GAUGE;
        else if (Metered.class.isInstance(annotation))
            return MetricType.METERED;
        else if (Timed.class.isInstance(annotation))
            return MetricType.TIMER;
        else
            throw new IllegalArgumentException("Unsupported Metrics forMethod [" + annotation.getClass().getName() + "]");
    }

    private String getUnit(Annotation annotation) {
        if (Counted.class.isInstance(annotation))
            return ((Counted) annotation).unit();
        else if (Gauge.class.isInstance(annotation))
            return ((Gauge) annotation).unit();
        else if (Metered.class.isInstance(annotation))
            return ((Metered) annotation).unit();
        else if (Timed.class.isInstance(annotation))
            return ((Timed) annotation).unit();
        else
            throw new IllegalArgumentException("Unsupported Metrics forMethod [" + annotation.getClass().getName() + "]");
    }


    @Vetoed
    private static final class ValidMetric<T extends Annotation> implements Of<T> {

        private final T annotation;

        private final String name;
        
        private final Metadata metadata;

        private ValidMetric(T annotation, String name, Metadata metadata) {
            this.annotation = annotation;
            this.name = name;
            this.metadata = metadata;
        }

        @Override
        public boolean isPresent() {
            return true;
        }

        @Override
        public String metricName() {
            return name;
        }

        @Override
        public T metricAnnotation() {
            return annotation;
        }
        
        @Override
        public Metadata metadata() {
            return metadata;
        }
    }

    @Vetoed
    private static final class InvalidMetric<T extends Annotation> implements Of<T> {

        private InvalidMetric() {
        }

        @Override
        public boolean isPresent() {
            return false;
        }

        @Override
        public String metricName() {
            throw new UnsupportedOperationException();
        }

        @Override
        public T metricAnnotation() {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public Metadata metadata() {
            return null;
        }
        
    }
    
    public interface Of<T extends Annotation> {
        
        boolean isPresent();
        
        String metricName();
        
        T metricAnnotation();
        
        Metadata metadata();
    }
    
}

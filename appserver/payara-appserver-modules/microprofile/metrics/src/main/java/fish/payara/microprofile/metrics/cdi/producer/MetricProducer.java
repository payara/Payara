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
 */

package fish.payara.microprofile.metrics.cdi.producer;

import fish.payara.microprofile.metrics.*;
import fish.payara.microprofile.metrics.cdi.AnnotationReader;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.InjectionPoint;
import java.lang.annotation.Annotation;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Timer;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Metric;
import org.glassfish.internal.api.Globals;

@Dependent
public class MetricProducer {

    private static final AnnotationReader<Metric> COUNTER = AnnotationReader.METRIC.asType(Counter.class.getTypeName());
    private static final AnnotationReader<Metric> GAUGE = AnnotationReader.METRIC.asType(Gauge.class.getTypeName());
    private static final AnnotationReader<Metric> HISTOGRAM = AnnotationReader.METRIC.asType(Histogram.class.getTypeName());
    private static final AnnotationReader<Metric> TIMER = AnnotationReader.METRIC.asType(Timer.class.getTypeName());

    @Produces
    private Counter counter(InjectionPoint ip) {
        return COUNTER.getOrRegister(ip, Counter.class, getMetricRegistry(ip));
    }

    @SuppressWarnings("unchecked")
    @Produces
    private <T extends Number> Gauge<T> gauge(InjectionPoint ip) {
        return GAUGE.getOrRegister(ip, Gauge.class, getMetricRegistry(ip));
    }

    @Produces
    private Histogram histogram(InjectionPoint ip) {
        return HISTOGRAM.getOrRegister(ip, Histogram.class, getMetricRegistry(ip));
    }

    @Produces
    private Timer timer(InjectionPoint ip) {
        return TIMER.getOrRegister(ip, Timer.class, getMetricRegistry(ip));
    }

    private MetricRegistry getMetricRegistry(InjectionPoint ip) {
        MetricsService metricsService =
                Globals.getDefaultBaseServiceLocator().getService(MetricsService.class);
        for(Annotation an : ip.getAnnotated().getAnnotations()) {
            if(an.annotationType().getName().equals(Metric.class.getTypeName())){
                Metric a = (Metric) an;
                return metricsService.getContext(true).getOrCreateRegistry(a.scope());
            }
            if(an.annotationType().getName().equals(org.eclipse.microprofile.metrics.annotation.Counted.class.getTypeName())){
                Counted a = (Counted) an;
                return metricsService.getContext(true).getOrCreateRegistry(a.scope());
            }

            if(an.annotationType().getName().equals(org.eclipse.microprofile.metrics.annotation.Gauge.class.getTypeName())){
                org.eclipse.microprofile.metrics.annotation.Gauge a = (org.eclipse.microprofile.metrics.annotation.Gauge) an;
                return metricsService.getContext(true).getOrCreateRegistry(a.scope());
            }

            if(an.annotationType().getName().equals(org.eclipse.microprofile.metrics.annotation.Timed.class.getTypeName())){
                org.eclipse.microprofile.metrics.annotation.Timed a = (org.eclipse.microprofile.metrics.annotation.Timed) an;
                return metricsService.getContext(true).getOrCreateRegistry(a.scope());
            }
        }
        return metricsService.getContext(true).getApplicationRegistry();
    }

}

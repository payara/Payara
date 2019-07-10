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
 */

package fish.payara.microprofile.metrics.cdi.producer;

import fish.payara.microprofile.metrics.cdi.MetricsHelper;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Priority;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import javax.interceptor.Interceptor;
import org.eclipse.microprofile.metrics.ConcurrentGauge;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Meter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.Timer;
import org.eclipse.microprofile.metrics.annotation.Metric;

@Dependent
@Priority(Interceptor.Priority.LIBRARY_BEFORE + 1)
public class MetricProducer {

    @Inject
    private MetricRegistry registry;

    @Inject
    private MetricsHelper helper;
    
    private static final Logger LOGGER = Logger.getLogger("MP-METRICS");

    @Produces
    private Counter counter(InjectionPoint ip) {
        Metric annotation = ip.getAnnotated().getAnnotation(Metric.class);
        if (annotation != null) {
            if (!(annotation.unit().equals(MetricUnits.NONE) &&
                    annotation.absolute() && annotation.description().isEmpty() && annotation.displayName().isEmpty())) {
                return registry.counter(helper.metadataOf(ip), MetricsHelper.tagsFromString(ip.getAnnotated().getAnnotation(Metric.class).tags()));
            } else {
                return registry.counter(helper.metricNameOf(ip), MetricsHelper.tagsFromString(ip.getAnnotated().getAnnotation(Metric.class).tags()));
            }
        } else {
            return registry.counter(ip.getMember().getDeclaringClass().getCanonicalName() + "." + ip.getMember().getName());
        }
    }
    
    @Produces
    private ConcurrentGauge concurrentGauge(InjectionPoint ip) {
        Metric annotation = ip.getAnnotated().getAnnotation(Metric.class);
        if (annotation != null) {
            if (!(annotation.unit().equals(MetricUnits.NONE) ||
                    annotation.absolute() || annotation.description().isEmpty() || annotation.displayName().isEmpty())) {
                return registry.concurrentGauge(helper.metadataOf(ip), MetricsHelper.tagsFromString(ip.getAnnotated().getAnnotation(Metric.class).tags()));
            } else {
                return registry.concurrentGauge(helper.metricNameOf(ip), MetricsHelper.tagsFromString(ip.getAnnotated().getAnnotation(Metric.class).tags()));
            }
        } else {
            return registry.concurrentGauge(ip.getMember().getDeclaringClass().getCanonicalName() + "." + ip.getMember().getName());
        }
    }

    @Produces
    private <T> Gauge<T> gauge(InjectionPoint ip) {
        return () -> (T) registry.getGauges().get(helper.metricIDof(ip)).getValue();
    }

    @Produces
    private Histogram histogram(InjectionPoint ip) {
        Metric annotation = ip.getAnnotated().getAnnotation(Metric.class);
        if (annotation != null) {
            if (!(annotation.unit().equals(MetricUnits.NONE) || annotation.description().isEmpty() || annotation.displayName().isEmpty())
                    || annotation.absolute()) {
                return registry.histogram(helper.metadataOf(ip), MetricsHelper.tagsFromString(ip.getAnnotated().getAnnotation(Metric.class).tags()));
            } else {
                return registry.histogram(helper.metricNameOf(ip), MetricsHelper.tagsFromString(ip.getAnnotated().getAnnotation(Metric.class).tags()));
            }
        } else {
            return registry.histogram(ip.getMember().getDeclaringClass().getCanonicalName() + "." + ip.getMember().getName());
        }
    }

    @Produces
    private Meter meter(InjectionPoint ip) {
        Metric annotation = ip.getAnnotated().getAnnotation(Metric.class);
        if (annotation != null) {
            if (!(annotation.unit().equals(MetricUnits.NONE) ||
                    annotation.absolute() || annotation.description().isEmpty() || annotation.displayName().isEmpty())) {
                return registry.meter(helper.metadataOf(ip), MetricsHelper.tagsFromString(ip.getAnnotated().getAnnotation(Metric.class).tags()));
            } else {
                return registry.meter(helper.metricNameOf(ip), MetricsHelper.tagsFromString(ip.getAnnotated().getAnnotation(Metric.class).tags()));
            }
        } else {
            return registry.meter(ip.getMember().getDeclaringClass().getCanonicalName() + "." + ip.getMember().getName());
        }
    }

    @Produces
    private Timer timer(InjectionPoint ip) {
        Metric annotation = ip.getAnnotated().getAnnotation(Metric.class);
        if (annotation != null) {
            if (!(annotation.unit().equals(MetricUnits.NONE) ||
                    annotation.absolute() || annotation.description().isEmpty() || annotation.displayName().isEmpty())) {
                return registry.timer(helper.metadataOf(ip), MetricsHelper.tagsFromString(ip.getAnnotated().getAnnotation(Metric.class).tags()));
            } else {
                return registry.timer(helper.metricNameOf(ip), MetricsHelper.tagsFromString(ip.getAnnotated().getAnnotation(Metric.class).tags()));
            }
        } else {
            return registry.timer(ip.getMember().getDeclaringClass().getCanonicalName() + "." + ip.getMember().getName());
        }
    }

    @Produces
    @Metric
    private Counter counterMetric(InjectionPoint ip) {
        return counter(ip);
    }
    
    @Produces
    @Metric
    private ConcurrentGauge concurrentGaugeMetric(InjectionPoint ip) {
        return concurrentGauge(ip);
    }

    @Produces
    @Metric
    private <T> Gauge<T> gaugeMetric(InjectionPoint ip) {
        return gauge(ip);
    }

    @Produces
    @Metric
    private Histogram histogramMetric(InjectionPoint ip) {
        return histogram(ip);
    }

    @Produces
    @Metric
    private Meter meterMetric(InjectionPoint ip) {
        return meter(ip);
    }

    @Produces
    @Metric
    private Timer timerMetric(InjectionPoint ip) {
        return timer(ip);
    }
}

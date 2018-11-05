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
 */

package fish.payara.microprofile.metrics.cdi.producer;

import fish.payara.microprofile.metrics.cdi.MetricsHelper;
import javax.annotation.Priority;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import javax.interceptor.Interceptor;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Meter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Timer;
import org.eclipse.microprofile.metrics.annotation.Metric;

@Dependent
@Priority(Interceptor.Priority.LIBRARY_BEFORE + 1)
public class MetricProducer {

    @Inject
    private MetricRegistry registry;

    @Inject
    private MetricsHelper helper;

    @Produces
    private Counter counter(InjectionPoint ip) {
        return registry.counter(helper.metadataOf(ip, Counter.class));
    }

    @Produces
    private <T> Gauge<T> gauge(InjectionPoint ip) {
        return () -> (T) registry.getGauges().get(helper.metricNameOf(ip)).getValue();
    }

    @Produces
    private Histogram histogram(InjectionPoint ip) {
        return registry.histogram(helper.metadataOf(ip, Histogram.class));
    }

    @Produces
    private Meter meter(InjectionPoint ip) {
        return registry.meter(helper.metadataOf(ip, Meter.class));
    }

    @Produces
    private Timer timer(InjectionPoint ip) {
        return registry.timer(helper.metadataOf(ip, Timer.class));
    }

    @Produces
    @Metric
    private Counter counterMetric(InjectionPoint ip) {
        return counter(ip);
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

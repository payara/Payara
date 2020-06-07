/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2020 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package fish.payara.microprofile.metrics.writer;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.microprofile.metrics.ConcurrentGauge;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Meter;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.SimpleTimer;
import org.eclipse.microprofile.metrics.Timer;

/**
 * The {@link MetricExporter} is an abstraction for writing individual {@link Metric}s to an output.
 *
 * The {@link MetricExporter} will expect that metrics of same name are exported together before metrics of a different
 * name are exported.
 *
 * @author Jan Bernitt
 * @since 5.202
 */
public interface MetricExporter {

    Logger LOGGER = Logger.getLogger(MetricExporter.class.getName());

    /**
     * Creates a new {@link MetricExporter} with the provided scope.
     *
     * @param scope the scope to use in the export, most likely on of the {@link MetricRegistry.Type}s
     * @return A new instance of this {@link MetricExporter} with the provided scope set, this instance is kept
     *         unchanged and will continue to use its current scope. Both, this {@link MetricExporter} and the returned
     *         one will however share other internal state that is related to the output written so far.
     */
    MetricExporter in(MetricRegistry.Type scope, boolean asNode);

    default MetricExporter in(MetricRegistry.Type scope) {
        return in(scope, true);
    }

    void export(MetricID metricID, Counter counter, Metadata metadata);

    void export(MetricID metricID, ConcurrentGauge gauge, Metadata metadata);

    void export(MetricID metricID, Gauge<?> gauge, Metadata metadata);

    void export(MetricID metricID, Histogram histogram, Metadata metadata);

    void export(MetricID metricID, Meter meter, Metadata metadata);

    void export(MetricID metricID, SimpleTimer timer, Metadata metadata);

    void export(MetricID metricID, Timer timer, Metadata metadata);

    default void export(MetricID metricID, Metric metric, Metadata metadata) {
        switch (MetricType.from(metric.getClass())) {
        case COUNTER: export(metricID, (Counter) metric, metadata); break;
        case CONCURRENT_GAUGE: export(metricID, (ConcurrentGauge) metric, metadata); break;
        case GAUGE: export(metricID, (Gauge<?>) metric, metadata); break;
        case HISTOGRAM: export(metricID, (Histogram) metric, metadata); break;
        case METERED: export(metricID, (Meter) metric, metadata); break;
        case SIMPLE_TIMER: export(metricID, (SimpleTimer) metric, metadata); break;
        case TIMER: export(metricID, (Timer) metric, metadata); break;
        case INVALID:
        default:
            LOGGER.log(Level.WARNING, "Metric type {0} for {1} is not supported",
                    new Object[] { metric.getClass(), metricID });
        }
    }

    void exportComplete();
}

/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright (c) [2020] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/master/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 * 
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License.
 * 
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at glassfish/legal/LICENSE.txt.
 * 
 *  GPL Classpath Exception:
 *  The Payara Foundation designates this particular file as subject to the "Classpath"
 *  exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *  file that accompanied this code.
 * 
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 * 
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 */
package fish.payara.healthcheck.microprofile.metrics;

import fish.payara.microprofile.metrics.MetricsService;
import fish.payara.nucleus.healthcheck.configuration.MicroProfileMetricsChecker;
import fish.payara.notification.healthcheck.HealthCheckResultEntry;
import static fish.payara.notification.healthcheck.HealthCheckResultStatus.CRITICAL;
import static fish.payara.notification.healthcheck.HealthCheckResultStatus.GOOD;
import static fish.payara.notification.healthcheck.HealthCheckResultStatus.WARNING;
import fish.payara.nucleus.healthcheck.HealthCheckResult;
import fish.payara.nucleus.healthcheck.configuration.MonitoredMetric;
import fish.payara.nucleus.healthcheck.preliminary.BaseHealthCheck;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import org.eclipse.microprofile.metrics.ConcurrentGauge;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Meter;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.SimpleTimer;
import org.eclipse.microprofile.metrics.Timer;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

@Service(name = "healthcheck-mpmetrics")
@RunLevel(StartupRunLevel.VAL)
public class MicroProfileMetricsCheck
        extends BaseHealthCheck<HealthCheckMicroProfileMetricstExecutionOptions, MicroProfileMetricsChecker> {

    @Inject
    private MetricsService metricsService;

    @PostConstruct
    public void postConstruct() {
        postConstruct(this, MicroProfileMetricsChecker.class);
    }

    @Override
    public HealthCheckMicroProfileMetricstExecutionOptions constructOptions(MicroProfileMetricsChecker checker) {
        return new HealthCheckMicroProfileMetricstExecutionOptions(Boolean.valueOf(
                checker.getEnabled()),
                Long.parseLong(checker.getTime()),
                asTimeUnit(checker.getUnit()),
                checker.getMonitoredMetrics());
    }

    @Override
    protected String getDescription() {
        return "healthcheck.description.MPmetrics";
    }

    @Override
    protected HealthCheckResult doCheckInternal() {
        List<MonitoredMetric> monitoredMetrics = options.getMonitoredMetrics();
        HealthCheckResult result = new HealthCheckResult();

        if (monitoredMetrics != null && !monitoredMetrics.isEmpty()) {
            List<String> metrics = collectMetrics(monitoredMetrics);
            StringBuilder metricsToDisplay = new StringBuilder();
            metrics.forEach(metricsToDisplay::append);
            result.add(new HealthCheckResultEntry(metrics.isEmpty() ? WARNING : GOOD,
                    metrics.isEmpty() ? "The metrics you have added for monitoring doesn't exist" : metricsToDisplay.toString()));
        } else {
            result.add(new HealthCheckResultEntry(CRITICAL, "No metric has been added for monitoring."));
        }
        return result;
    }

    private List<String> collectMetrics(List<MonitoredMetric> monitoredMetrics) {
        List<String> metrics = new ArrayList<>();
        String metricsInfos;
        for (MonitoredMetric metric : monitoredMetrics) {
            for (MetricRegistry metricRegistry : metricsService.getAllRegistry()) {
                String metricName = metric.getMetricName();
                metricsInfos = getMetricInfos(metricName, metricRegistry.getMetrics().get(new MetricID(metricName)));
                if (metricsInfos != null) {
                    metrics.add(metricsInfos);
                    break;
                }
            }
        }
        return metrics;
    }

    private String getMetricInfos(String metricName, Metric metric) {
        if (metric instanceof Counter) {
            return toName(metricName, "Count") + "=" + ((Counter) metric).getCount();
        }
        if (metric instanceof ConcurrentGauge) {
            ConcurrentGauge concurrentGauge = ((ConcurrentGauge) metric);
            return toName(metricName, "Count") + "=" + concurrentGauge.getCount()
                    + toName(metricName, "Max") + "=" + concurrentGauge.getMax()
                    + toName(metricName, "Min") + "=" + concurrentGauge.getMin();
        }
        if (metric instanceof Gauge) {
            Object value = ((Gauge<?>) metric).getValue();
            if (value instanceof Number) {
                return toName(metricName, "") + " = " + ((Number) value);
            }
        }
        if (metric instanceof Histogram) {
            Histogram histogram = ((Histogram) metric);
            return toName(metricName, "Count") + "=" + histogram.getCount()
                    + toName(metricName, "Max") + "=" + histogram.getSnapshot().getMax()
                    + toName(metricName, "Mean") + "=" + histogram.getSnapshot().getMean()
                    + toName(metricName, "Median") + "=" + histogram.getSnapshot().getMedian()
                    + toName(metricName, "Min") + "=" + histogram.getSnapshot().getMin()
                    + toName(metricName, "StdDev") + "=" + histogram.getSnapshot().getStdDev()
                    + toName(metricName, "75thPercentile") + "=" + histogram.getSnapshot().get75thPercentile()
                    + toName(metricName, "95thPercentile") + "=" + histogram.getSnapshot().get95thPercentile()
                    + toName(metricName, "98thPercentile") + "=" + histogram.getSnapshot().get98thPercentile()
                    + toName(metricName, "99thPercentile") + "=" + histogram.getSnapshot().get99thPercentile()
                    + toName(metricName, "999thPercentile") + "=" + histogram.getSnapshot().get999thPercentile();
        }

        if (metric instanceof Meter) {
            Meter meter = ((Meter) metric);
            return toName(metricName, "Count") + "=" + meter.getCount()
                    + toName(metricName, "FifteenMinuteRate") + "=" + meter.getFifteenMinuteRate()
                    + toName(metricName, "FiveMinuteRate") + "=" + meter.getFiveMinuteRate()
                    + toName(metricName, "OneMinuteRate") + "=" + meter.getOneMinuteRate()
                    + toName(metricName, "MeanRate") + "=" + meter.getMeanRate();
        }

        if (metric instanceof SimpleTimer) {
            SimpleTimer simpleTimer = ((SimpleTimer) metric);
            return toName(metricName, "Count") + "=" + simpleTimer.getCount()
                    + toName(metricName, "ElapsedTime") + "=" + simpleTimer.getElapsedTime().toMillis();
        }
        if (metric instanceof Timer) {
            Timer timer = ((Timer) metric);
            return toName(metricName, "Count") + "=" + timer.getCount()
                    + toName(metricName, "FifteenMinuteRate") + "=" + timer.getFifteenMinuteRate()
                    + toName(metricName, "FiveMinuteRate") + "=" + timer.getFiveMinuteRate()
                    + toName(metricName, "OneMinuteRate") + "=" + timer.getOneMinuteRate()
                    + toName(metricName, "MeanRate") + "=" + timer.getMeanRate()
                    + toName(metricName, "Max") + "=" + timer.getSnapshot().getMax()
                    + toName(metricName, "Mean") + "=" + timer.getSnapshot().getMean()
                    + toName(metricName, "Median") + "=" + timer.getSnapshot().getMedian()
                    + toName(metricName, "Min") + "=" + timer.getSnapshot().getMin()
                    + toName(metricName, "StdDev") + "=" + timer.getSnapshot().getStdDev()
                    + toName(metricName, "75thPercentile") + "=" + timer.getSnapshot().get75thPercentile()
                    + toName(metricName, "95thPercentile") + "=" + timer.getSnapshot().get95thPercentile()
                    + toName(metricName, "98thPercentile") + "=" + timer.getSnapshot().get98thPercentile()
                    + toName(metricName, "99thPercentile") + "=" + timer.getSnapshot().get99thPercentile()
                    + toName(metricName, "999thPercentile") + "=" + timer.getSnapshot().get999thPercentile();
        }

        return null;
    }

    private static String toName(String name, String suffix) {
        if (name.indexOf(' ') >= 0) { // trying to avoid replace
            name = name.replace(' ', '_');
        } else {
            if (name.indexOf('.') > 0) {
                String[] words = name.split("\\.");
                name = "";
                for (String word : words) {
                    name += toFirstLetterUpperCase(word);
                }
            }
        }
        name = toFirstLetterUpperCase(name);
        return " " + (name.endsWith(suffix) || suffix.isEmpty() ? name : name + suffix);
    }

    private static String toFirstLetterUpperCase(String value) {
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}

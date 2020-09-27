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
import static fish.payara.notification.healthcheck.HealthCheckResultStatus.GOOD;
import static fish.payara.notification.healthcheck.HealthCheckResultStatus.WARNING;
import fish.payara.nucleus.healthcheck.HealthCheckResult;
import fish.payara.nucleus.healthcheck.preliminary.BaseHealthCheck;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.eclipse.microprofile.metrics.ConcurrentGauge;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Meter;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.SimpleTimer;
import org.eclipse.microprofile.metrics.Timer;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.hk2.runlevel.RunLevel;
import org.glassfish.internal.api.Globals;
import org.jvnet.hk2.annotations.Service;

@Service(name = "healthcheck-mpmetrics")
@RunLevel(StartupRunLevel.VAL)
public class MicroProfileMetricsCheck
        extends BaseHealthCheck<HealthCheckMicroProfileMetricstExecutionOptions, MicroProfileMetricsChecker> {

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
                checker.getMetricsScope(),
                checker.getMetricApplicationName(),
                checker.getMetricName());
    }

    @Override
    protected String getDescription() {
        return "healthcheck.description.MPmetrics";
    }

    @Override
    protected HealthCheckResult doCheckInternal() {
        String registryName = options.getMetricsScope();
        List<String> metricNames = new ArrayList<>(Arrays.asList(options.getMetricName().split(",")));
        metricNames.removeAll(Arrays.asList("", null));
        MetricsService metricsService = Globals.getDefaultBaseServiceLocator().getService(MetricsService.class);
        if (registryName.equalsIgnoreCase("application")) {
            registryName = options.getMetricsApplicationName();
        }

        HealthCheckResult result = new HealthCheckResult();
        List<String> metrics = collectMetrics(metricsService.getRegistry(registryName), metricNames);

        result.add(new HealthCheckResultEntry(metrics.isEmpty() ? WARNING : GOOD,
                metrics.isEmpty() ? "The metric you entered doesn't exist under " + registryName : metrics.stream().map(Object::toString).collect(Collectors.joining())));

        return result;

    }

    private List<String> collectMetrics(MetricRegistry state, List<String> metricNames) {
        List<String> array = new ArrayList<>();
        String metricsInfos;
        if (metricNames == null || metricNames.isEmpty()) {
            for (String name : state.getNames()) {
                metricsInfos = getMetricInfos(name, state);
                if (metricsInfos != null) {
                    array.add(metricsInfos);
                }

            }
        } else {
            for (String metricName : metricNames) {
                metricsInfos = getMetricInfos(metricName.trim(), state);
                if (metricsInfos != null) {
                    array.add(metricsInfos);
                }
            }
        }
        return array;
    }

    private String getMetricInfos(String metricName, MetricRegistry state) {
        Map<MetricID, Metric> metricInfos = state.getMetrics().entrySet().stream()
                .filter(entry -> entry.getKey().getName().equals(metricName))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        for (Entry<MetricID, Metric> entry : metricInfos.entrySet()) {
            MetricID metricID = entry.getKey();
            Metric metric = entry.getValue();

            if (metric instanceof Counter) {
                return toName(metricID.getName(), "Count") + "=" + ((Counter) metric).getCount();
            }
            if (metric instanceof ConcurrentGauge) {
                ConcurrentGauge concurrentGauge = ((ConcurrentGauge) metric);
                return toName(metricID.getName(), "Count") + "=" + concurrentGauge.getCount()
                        + toName(metricID.getName(), "Max") + "=" + concurrentGauge.getMax()
                        + toName(metricID.getName(), "Min") + "=" + concurrentGauge.getMin();
            }
            if (metric instanceof Gauge) {
                Object value = ((Gauge<?>) metric).getValue();
                if (value instanceof Number) {
                    return toName(metricID.getName(),
                            getMetricUnitSuffix(state.getMetadata().get(metricID.getName()).getUnit())) + " = " + ((Number) value);
                }
            }
            if (metric instanceof Histogram) {
                Histogram histogram = ((Histogram) metric);
                return toName(metricID.getName(), "Count") + "=" + histogram.getCount()
                        + toName(metricID.getName(), "Max") + "=" + histogram.getSnapshot().getMax()
                        + toName(metricID.getName(), "Mean") + "=" + histogram.getSnapshot().getMean()
                        + toName(metricID.getName(), "Median") + "=" + histogram.getSnapshot().getMedian()
                        + toName(metricID.getName(), "Min") + "=" + histogram.getSnapshot().getMin()
                        + toName(metricID.getName(), "StdDev") + "=" + histogram.getSnapshot().getStdDev()
                        + toName(metricID.getName(), "75thPercentile") + "=" + histogram.getSnapshot().get75thPercentile()
                        + toName(metricID.getName(), "95thPercentile") + "=" + histogram.getSnapshot().get95thPercentile()
                        + toName(metricID.getName(), "98thPercentile") + "=" + histogram.getSnapshot().get98thPercentile()
                        + toName(metricID.getName(), "99thPercentile") + "=" + histogram.getSnapshot().get99thPercentile()
                        + toName(metricID.getName(), "999thPercentile") + "=" + histogram.getSnapshot().get999thPercentile();
            }

            if (metric instanceof Meter) {
                Meter meter = ((Meter) metric);
                return toName(metricID.getName(), "Count") + "=" + meter.getCount()
                        + toName(metricID.getName(), "FifteenMinuteRate") + "=" + meter.getFifteenMinuteRate()
                        + toName(metricID.getName(), "FiveMinuteRate") + "=" + meter.getFiveMinuteRate()
                        + toName(metricID.getName(), "OneMinuteRate") + "=" + meter.getOneMinuteRate()
                        + toName(metricID.getName(), "MeanRate") + "=" + meter.getMeanRate();
            }

            if (metric instanceof SimpleTimer) {
                SimpleTimer simpleTimer = ((SimpleTimer) metric);
                return toName(metricID.getName(), "Count") + "=" + simpleTimer.getCount()
                        + toName(metricID.getName(), "ElapsedTime") + "=" + simpleTimer.getElapsedTime().toMillis();
            }
            if (metric instanceof Timer) {
                Timer timer = ((Timer) metric);
                return toName(metricID.getName(), "Count") + "=" + timer.getCount()
                        + toName(metricID.getName(), "FifteenMinuteRate") + "=" + timer.getFifteenMinuteRate()
                        + toName(metricID.getName(), "FiveMinuteRate") + "=" + timer.getFiveMinuteRate()
                        + toName(metricID.getName(), "OneMinuteRate") + "=" + timer.getOneMinuteRate()
                        + toName(metricID.getName(), "MeanRate") + "=" + timer.getMeanRate()
                        + toName(metricID.getName(), "Max") + "=" + timer.getSnapshot().getMax()
                        + toName(metricID.getName(), "Mean") + "=" + timer.getSnapshot().getMean()
                        + toName(metricID.getName(), "Median") + "=" + timer.getSnapshot().getMedian()
                        + toName(metricID.getName(), "Min") + "=" + timer.getSnapshot().getMin()
                        + toName(metricID.getName(), "StdDev") + "=" + timer.getSnapshot().getStdDev()
                        + toName(metricID.getName(), "75thPercentile") + "=" + timer.getSnapshot().get75thPercentile()
                        + toName(metricID.getName(), "95thPercentile") + "=" + timer.getSnapshot().get95thPercentile()
                        + toName(metricID.getName(), "98thPercentile") + "=" + timer.getSnapshot().get98thPercentile()
                        + toName(metricID.getName(), "99thPercentile") + "=" + timer.getSnapshot().get99thPercentile()
                        + toName(metricID.getName(), "999thPercentile") + "=" + timer.getSnapshot().get999thPercentile();
            }

        }
        return null;
    }

    private static String getMetricUnitSuffix(Optional<String> unit) {
        if (!unit.isPresent()) {
            return "";
        }
        String value = unit.get();
        if (MetricUnits.NONE.equalsIgnoreCase(value) || value.isEmpty()) {
            return "";
        }
        return toFirstLetterUpperCase(value);
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

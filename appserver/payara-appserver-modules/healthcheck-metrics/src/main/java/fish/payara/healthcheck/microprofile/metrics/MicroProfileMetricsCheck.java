/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) [2020-2024] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/main/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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

import static fish.payara.notification.healthcheck.HealthCheckResultStatus.CRITICAL;
import static fish.payara.notification.healthcheck.HealthCheckResultStatus.GOOD;
import static fish.payara.notification.healthcheck.HealthCheckResultStatus.WARNING;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import fish.payara.internal.notification.EventLevel;
import fish.payara.notification.healthcheck.HealthCheckResultStatus;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.glassfish.api.StartupRunLevel;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import fish.payara.microprofile.metrics.MetricsService;
import fish.payara.microprofile.metrics.writer.FilteredMetricsExporter;
import fish.payara.microprofile.metrics.writer.MetricsWriter;
import fish.payara.microprofile.metrics.writer.MetricsWriterImpl;
import fish.payara.notification.healthcheck.HealthCheckResultEntry;
import fish.payara.nucleus.healthcheck.HealthCheckResult;
import fish.payara.nucleus.healthcheck.configuration.MicroProfileMetricsChecker;
import fish.payara.nucleus.healthcheck.configuration.MonitoredMetric;
import fish.payara.nucleus.healthcheck.preliminary.BaseHealthCheck;

@Service(name = "healthcheck-mpmetrics")
@RunLevel(StartupRunLevel.VAL)
public class MicroProfileMetricsCheck
        extends BaseHealthCheck<HealthCheckMicroProfileMetricstExecutionOptions, MicroProfileMetricsChecker> {

    @Inject
    private MetricsService metricsService;

    private StringWriterProxy buffer;
    private MetricsWriter writer;

    @PostConstruct
    public void postConstruct() {
        postConstruct(this, MicroProfileMetricsChecker.class);
    }

    @Override
    public synchronized HealthCheckMicroProfileMetricstExecutionOptions constructOptions(MicroProfileMetricsChecker checker) {
        Set<String> metricNames = new HashSet<>();
        for (MonitoredMetric metric : checker.getMonitoredMetrics()) {
            metricNames.add(metric.getMetricName());
        }
        this.buffer = new StringWriterProxy();
        this.writer = new MetricsWriterImpl(new FilteredMetricsExporter(buffer, metricNames),
                metricsService.getContextNames(), metricsService::getContext);

        return new HealthCheckMicroProfileMetricstExecutionOptions(Boolean.valueOf(checker.getEnabled()),
                Long.parseLong(checker.getTime()), asTimeUnit(checker.getUnit()),
                Boolean.valueOf(checker.getAddToMicroProfileHealth()), checker.getMonitoredMetrics());
    }

    @Override
    protected String getDescription() {
        return "healthcheck.description.MPmetrics";
    }

    @Override
    protected HealthCheckResult doCheckInternal() {
        List<MonitoredMetric> monitoredMetrics = options.getMonitoredMetrics();
        HealthCheckResult result = new HealthCheckResult();

        try {
            if (monitoredMetrics != null && !monitoredMetrics.isEmpty()) {
                final String data = write();
                result.add(new HealthCheckResultEntry(data.isEmpty() ? WARNING : GOOD,
                        data.isEmpty() ? "The metrics you have added for monitoring doesn't exist"
                                : data));
            } else {
                result.add(new HealthCheckResultEntry(CRITICAL, "No metric has been added for monitoring."));
            }
        } catch (IOException ex) {
            result.add(new HealthCheckResultEntry(CRITICAL, "Failed to write metrics to stream."));
        }
        return result;
    }

    @Override
    protected EventLevel createNotificationEventLevel (HealthCheckResultStatus checkResult) {
        return EventLevel.INFO;
    }

    private synchronized String write() throws IOException {
        writer.write();
        // Remove any trailing whitespace or commas
        final String result = buffer.toString().trim().replaceAll(",$", "");
        this.buffer.clear();
        return result;
    }

}

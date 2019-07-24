/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016-2019 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.nucleus.healthcheck.stuck;

import fish.payara.nucleus.healthcheck.HealthCheckResult;
import fish.payara.monitoring.collect.MonitoringDataCollector;
import fish.payara.monitoring.collect.MonitoringDataSource;
import fish.payara.notification.healthcheck.HealthCheckResultEntry;
import fish.payara.notification.healthcheck.HealthCheckResultStatus;
import fish.payara.nucleus.healthcheck.HealthCheckStuckThreadExecutionOptions;
import fish.payara.nucleus.healthcheck.preliminary.BaseHealthCheck;
import fish.payara.nucleus.healthcheck.configuration.StuckThreadsChecker;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

/**
 * @since 4.1.2.173
 * @author jonathan coustick
 */
@Service(name = "healthcheck-stuck")
@RunLevel(StartupRunLevel.VAL)
public class StuckThreadsHealthCheck extends
        BaseHealthCheck<HealthCheckStuckThreadExecutionOptions, StuckThreadsChecker> implements MonitoringDataSource {

    @Inject
    StuckThreadsStore stuckThreadsStore;

    @Inject
    StuckThreadsChecker checker;

    private final Map<ThreadInfo, Long> stuckThreads = new ConcurrentHashMap<>();

    @Override
    public void collect(MonitoringDataCollector collector) {
        if (isReady()) {
            MonitoringDataCollector stuckCollector = collector.in("health-check").type("checker").entity("STUCK")
                    .collect("checksDone", getChecksDone())
                    .collectNonZero("checksFailed", getChecksFailed())
                    .collectNonZero("size", stuckThreads.size());
            for (Entry<ThreadInfo, Long> stuckThread : stuckThreads.entrySet()) {
                stuckCollector.tag("thread", stuckThread.getKey().getThreadName())
                    .collect("timeHeld", stuckThread.getValue());
            }
        }
    }

    @PostConstruct
    void postConstruct() {
        postConstruct(this, StuckThreadsChecker.class);
    }

    @Override
    protected HealthCheckResult doCheckInternal() {
        stuckThreads.clear();
        HealthCheckResult result = new HealthCheckResult();
        ThreadMXBean bean = ManagementFactory.getThreadMXBean();

        Long thresholdNanos = TimeUnit.NANOSECONDS.convert(options.getTimeStuck(), options.getUnitStuck());

        ConcurrentHashMap<Long, Long> threads = stuckThreadsStore.getThreads();
        for (Long thread : threads.keySet()){
            Long timeHeld = threads.get(thread);
            if (timeHeld > thresholdNanos){
                ThreadInfo info = bean.getThreadInfo(thread, Integer.MAX_VALUE);
                if (info != null){//check thread hasn't died already
                    stuckThreads.put(info, timeHeld);
                    result.add(new HealthCheckResultEntry(HealthCheckResultStatus.WARNING, "Stuck Thread: " + info.toString()));
                }
            }
        }

        return result;
    }

    @Override
    public HealthCheckStuckThreadExecutionOptions constructOptions(StuckThreadsChecker checker) {
        return new HealthCheckStuckThreadExecutionOptions(Boolean.valueOf(checker.getEnabled()), Long.parseLong(checker.getTime()), asTimeUnit(checker.getUnit()),
                Long.parseLong(checker.getThreshold()), asTimeUnit(checker.getThresholdTimeUnit()));
    }    

    @Override
    protected String getDescription() {
        return "healthcheck.description.stuckThreads";
    }
    
}

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
package fish.payara.nucleus.healthcheck.cpool;

import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.config.serverbeans.Module;
import com.sun.enterprise.connectors.util.ResourcesUtil;
import com.sun.enterprise.resource.pool.PoolManager;
import com.sun.enterprise.resource.pool.PoolStatus;

import fish.payara.monitoring.collect.MonitoringDataCollector;
import fish.payara.monitoring.collect.MonitoringDataSource;
import fish.payara.notification.healthcheck.HealthCheckResultEntry;
import fish.payara.nucleus.healthcheck.HealthCheckResult;
import fish.payara.nucleus.healthcheck.cpool.configuration.ConnectionPoolChecker;
import fish.payara.nucleus.healthcheck.preliminary.BaseThresholdHealthCheck;
import org.glassfish.hk2.runlevel.RunLevel;
import org.glassfish.jdbc.config.JdbcConnectionPool;
import org.glassfish.jdbc.config.JdbcResource;
import org.glassfish.jdbc.util.JdbcResourcesUtil;
import org.glassfish.resourcebase.resources.api.PoolInfo;
import org.glassfish.resourcebase.resources.api.ResourceInfo;
import org.glassfish.resourcebase.resources.util.ResourceUtil;
import org.jvnet.hk2.annotations.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author mertcaliskan
 */
@Service(name = "healthcheck-cpool")
@RunLevel(10)
public class ConnectionPoolHealthCheck
        extends BaseThresholdHealthCheck<HealthCheckConnectionPoolExecutionOptions, ConnectionPoolChecker>
        implements MonitoringDataSource {

    @Inject
    private Domain domain;

    @Inject
    private Applications applications;

    @Inject
    private PoolManager poolManager;

    private final Map<String, PoolStatus> status = new ConcurrentHashMap<>();

    @Override
    public void collect(MonitoringDataCollector collector) {
        if (isReady()) {
            collector.in("health-check").type("checker").entity("CONP")
                .collect("checksDone", getChecksDone())
                .collectNonZero("checksFailed", getChecksFailed())
                .collectObjects(status.values(), ConnectionPoolHealthCheck::collectPoolStatus);
        }
    }

    private static void collectPoolStatus(MonitoringDataCollector collector, PoolStatus status) {
        PoolInfo info = status.getPoolInfo();
        collector.tag("app", info.getApplicationName()).tag("pool", info.getName())
            .collect("freeConnections", status.getNumConnFree())
            .collect("usedConnections", status.getNumConnUsed());
    }

    @PostConstruct
    void postConstruct() {
        postConstruct(this, ConnectionPoolChecker.class);
    }

    public HealthCheckConnectionPoolExecutionOptions constructOptions(ConnectionPoolChecker checker) {
        return new HealthCheckConnectionPoolExecutionOptions(Boolean.valueOf(checker.getEnabled()),
                Long.parseLong(checker.getTime()),
                asTimeUnit(checker.getUnit()),
                checker.getPropertyValue(THRESHOLD_CRITICAL, THRESHOLD_DEFAULTVAL_CRITICAL),
                checker.getPropertyValue(THRESHOLD_WARNING, THRESHOLD_DEFAULTVAL_WARNING),
                checker.getPropertyValue(THRESHOLD_GOOD, THRESHOLD_DEFAULTVAL_GOOD),
                checker.getPoolName());
    }

    @Override
    protected String getDescription() {
        return "healthcheck.description.connectionPool";
    }

    @Override
    protected HealthCheckResult doCheckInternal() {
        status.clear();
        HealthCheckResult result = new HealthCheckResult();
        Collection<JdbcResource> allJdbcResources = getAllJdbcResources();
        for (JdbcResource resource : allJdbcResources) {
            ResourceInfo resourceInfo = ResourceUtil.getResourceInfo(resource);
            JdbcConnectionPool pool = JdbcResourcesUtil.createInstance().getJdbcConnectionPoolOfResource(resourceInfo);
            PoolInfo poolInfo = ResourceUtil.getPoolInfo(pool);
            if (getOptions().getPoolName() != null) {
                if (getOptions().getPoolName().equals(poolInfo.getName())) {
                    evaluatePoolUsage(result, poolInfo);
                }
            }
            else {
                evaluatePoolUsage(result, poolInfo);
            }

        }
        return result;
    }

    private void evaluatePoolUsage(HealthCheckResult result, PoolInfo poolInfo) {
        PoolStatus poolStatus = poolManager.getPoolStatus(poolInfo);
        if (poolStatus != null) {
            status.put(poolInfo.getName(), poolStatus);
            long usedConnection = poolStatus.getNumConnUsed();
            long freeConnection = poolStatus.getNumConnFree();
            long totalConnection = usedConnection + freeConnection;

            if (totalConnection > 0) {
                double usedPercentage = ((double)usedConnection / totalConnection) * 100;

                result.add(new HealthCheckResultEntry(decideOnStatusWithRatio(usedPercentage),
                        poolInfo.getName() + " Usage (%): " + new DecimalFormat("#.00").format(usedPercentage)));
            }
        }
    }

    private Collection<JdbcResource> getAllJdbcResources() {
        Collection<JdbcResource> allResources = new ArrayList<>();
        Collection<JdbcResource> jdbcResources = domain.getResources().getResources(JdbcResource.class);
        allResources.addAll(jdbcResources);
        for (Application app : applications.getApplications()) {
            if (ResourcesUtil.createInstance().isEnabled(app)) {
                Resources appScopedResources = app.getResources();
                if (appScopedResources != null && appScopedResources.getResources() != null) {
                    allResources.addAll(appScopedResources.getResources(JdbcResource.class));
                }
                List<Module> modules = app.getModule();
                if (modules != null) {
                    for (Module module : modules) {
                        Resources msr = module.getResources();
                        if (msr != null && msr.getResources() != null) {
                            allResources.addAll(msr.getResources(JdbcResource.class));
                        }
                    }
                }
            }
        }
        return allResources;
    }
}

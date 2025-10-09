/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016-2021 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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

import fish.payara.monitoring.collect.MonitoringData;
import fish.payara.monitoring.collect.MonitoringDataCollector;
import fish.payara.monitoring.collect.MonitoringDataSource;
import fish.payara.monitoring.collect.MonitoringWatchCollector;
import fish.payara.monitoring.collect.MonitoringWatchSource;
import fish.payara.notification.healthcheck.HealthCheckResultEntry;
import fish.payara.nucleus.healthcheck.HealthCheckResult;
import fish.payara.nucleus.healthcheck.HealthCheckStatsProvider;
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

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import static java.util.stream.Collectors.toSet;
import com.sun.enterprise.config.serverbeans.Module;

/**
 * @author mertcaliskan
 * @author Jan Bernitt (consumer based version)
 */
@Service(name = "healthcheck-cpool")
@RunLevel(10)
public class ConnectionPoolHealthCheck
    extends BaseThresholdHealthCheck<HealthCheckConnectionPoolExecutionOptions, ConnectionPoolChecker>
    implements MonitoringDataSource, MonitoringWatchSource, HealthCheckStatsProvider {

    @Inject
    private Domain domain;

    @Inject
    private Applications applications;

    @Inject
    private PoolManager poolManager;

    private final Map<String, Long> usedConnections = new ConcurrentHashMap<>();
    private final Map<String, Long> freeConnections = new ConcurrentHashMap<>();
    private static final String USED_CONNECTION = "usedConnection";
    private static final String FREE_CONNECTION = "freeConnection";
    private static final String TOTAL_CONNECTION = "totalConnection";
    private static final Set<String> VALID_SUB_ATTRIBUTES = Set.of(USED_CONNECTION, FREE_CONNECTION, TOTAL_CONNECTION);

    @PostConstruct
    void postConstruct() {
        postConstruct(this, ConnectionPoolChecker.class);
    }

    @Override
    public Object getValue(Class type, String attributeName, String subAttributeName) {
        if (subAttributeName == null) {
            throw new IllegalArgumentException("sub-attribute name is required");
        }
        if (!VALID_SUB_ATTRIBUTES.contains(subAttributeName)) {
            throw new IllegalArgumentException("Invalid sub-attribute name: " + subAttributeName + ", supported sub-attributes are " + VALID_SUB_ATTRIBUTES);
        }
        if (!Number.class.isAssignableFrom(type)) {
            throw new IllegalArgumentException("attribute type must be number");
        }
        switch (subAttributeName) {
            case USED_CONNECTION:
                return usedConnections.getOrDefault(attributeName, 0L);
            case FREE_CONNECTION:
                return freeConnections.getOrDefault(attributeName, 0L);
            case TOTAL_CONNECTION:
                return usedConnections.getOrDefault(attributeName, 0L) + freeConnections.getOrDefault(attributeName, 0L);
        }
        return 0L;
    }

    @Override
    public Set<String> getAttributes() {
       return getAllJdbcResourcesName().stream().map(PoolInfo::getName).collect(toSet());
    }

    @Override
    public Set<String> getSubAttributes() {
        return VALID_SUB_ATTRIBUTES;
    }

    @Override
    public boolean isEnabled() {
        return this.getOptions() != null ? this.getOptions().isEnabled() : false;
    }

    @Override
    public HealthCheckConnectionPoolExecutionOptions constructOptions(ConnectionPoolChecker checker) {
        return new HealthCheckConnectionPoolExecutionOptions(Boolean.valueOf(checker.getEnabled()),
                Long.parseLong(checker.getTime()),
                asTimeUnit(checker.getUnit()),
                Boolean.valueOf(checker.getAddToMicroProfileHealth()),
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
        HealthCheckResult result = new HealthCheckResult();
        freeConnections.clear();
        usedConnections.clear();
        consumeAllJdbcResources(createConsumer((info, usedPercentage) ->
            result.add(new HealthCheckResultEntry(decideOnStatusWithRatio(usedPercentage),
                    info.getName() + " Usage (%): " + new DecimalFormat("#.00").format(usedPercentage)))
        ));
        return result;
    }

    @Override
    public void collect(MonitoringWatchCollector collector) {
        collectUsage(collector, "ns:health @:* PoolUsage", "Connection Pool Usage", 5, false);
    }

    @Override
    @MonitoringData(ns = "health", intervalSeconds = 8)
    public void collect(MonitoringDataCollector collector) {
        if (options != null && options.isEnabled()) {
            freeConnections.clear();
            usedConnections.clear();
            consumeAllJdbcResources(createConsumer((info, usedPercentage)
                    -> collector.group(info.getName()).collect("PoolUsage", usedPercentage.longValue())
            ));
        }
    }

    private Consumer<JdbcResource> createConsumer(BiConsumer<PoolInfo, Double> poolUsageConsumer) {
        return resource -> {
            ResourceInfo resourceInfo = ResourceUtil.getResourceInfo(resource);
            JdbcConnectionPool pool = JdbcResourcesUtil.createInstance().getJdbcConnectionPoolOfResource(resourceInfo);
            PoolInfo poolInfo = ResourceUtil.getPoolInfo(pool);
            String name = getOptions().getPoolName();
            if (name == null || name.equals(poolInfo.getName())) {
                PoolStatus poolStatus = poolManager.getPoolStatus(poolInfo);
                if (poolStatus != null) {
                    long usedConnection = poolStatus.getNumConnUsed();
                    long freeConnection = poolStatus.getNumConnFree();
                    long totalConnection = usedConnection + freeConnection;

                    if (totalConnection > 0) {
                        double usedPercentage = 100d * usedConnection / totalConnection;
                        poolUsageConsumer.accept(poolInfo, usedPercentage);
                    }
                    freeConnections.put(poolInfo.getName(), freeConnection);
                    usedConnections.put(poolInfo.getName(), usedConnection);
                }
            }
        };
    }

    private void consumeAllJdbcResources(Consumer<JdbcResource> consumer) {
        consumeJdbcResources(domain.getResources(), consumer);
        for (Application app : applications.getApplications()) {
            if (ResourcesUtil.createInstance().isEnabled(app)) {
                consumeJdbcResources(app.getResources(), consumer);
                List<Module> modules = app.getModule();
                if (modules != null) {
                    for (Module module : modules) {
                        consumeJdbcResources(module.getResources(), consumer);
                    }
                }
            }
        }
    }

    private static void consumeJdbcResources(Resources resources, Consumer<JdbcResource> consumer) {
        if (resources != null) {
            List<Resource> list = resources.getResources();
            if (list != null) {
                for (Resource r : list) {
                    if (JdbcResource.class.isInstance(r)) {
                        consumer.accept((JdbcResource) r);
                    }
                }
            }
        }
    }
    
    private List<PoolInfo> getAllJdbcResourcesName() {
        List<PoolInfo> poolInfos = new ArrayList<>();
        poolInfos.addAll(getJdbcResourcesInfo(domain.getResources()));
        for (Application app : applications.getApplications()) {
            if (ResourcesUtil.createInstance().isEnabled(app)) {
                poolInfos.addAll(getJdbcResourcesInfo(app.getResources()));
                List<Module> modules = app.getModule();
                if (modules != null) {
                    for (Module module : modules) {
                        poolInfos.addAll(getJdbcResourcesInfo(module.getResources()));
                    }
                }
            }
        }
        return poolInfos; 
    }

    private List<PoolInfo> getJdbcResourcesInfo(Resources resources) {
        List<PoolInfo> poolInfos = new ArrayList<>();
        if (resources != null) {
            List<Resource> list = resources.getResources();
            if (list != null) {
                for (Resource resource : list) {
                    if (JdbcResource.class.isInstance(resource)) {
                        ResourceInfo resourceInfo = ResourceUtil.getResourceInfo((JdbcResource)resource);
                        JdbcConnectionPool pool = JdbcResourcesUtil.createInstance().getJdbcConnectionPoolOfResource(resourceInfo);
                        poolInfos.add(ResourceUtil.getPoolInfo(pool));
                    }
                }
            }
        }
        return poolInfos;   
    }
}

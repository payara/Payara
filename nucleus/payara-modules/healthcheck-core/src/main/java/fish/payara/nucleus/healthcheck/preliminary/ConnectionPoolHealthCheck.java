/*
 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 Copyright (c) 2016 Payara Foundation. All rights reserved.
 The contents of this file are subject to the terms of the Common Development
 and Distribution License("CDDL") (collectively, the "License").  You
 may not use this file except in compliance with the License.  You can
 obtain a copy of the License at
 https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 or packager/legal/LICENSE.txt.  See the License for the specific
 language governing permissions and limitations under the License.
 When distributing the software, include this License Header Notice in each
 file and include the License file at packager/legal/LICENSE.txt.
 */
package fish.payara.nucleus.healthcheck.preliminary;

import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.connectors.util.ResourcesUtil;
import com.sun.enterprise.resource.pool.PoolManager;
import com.sun.enterprise.resource.pool.PoolStatus;
import fish.payara.nucleus.healthcheck.HealthCheckConnectionPoolExecutionOptions;
import fish.payara.nucleus.healthcheck.HealthCheckResult;
import fish.payara.nucleus.healthcheck.HealthCheckResultEntry;
import fish.payara.nucleus.healthcheck.configuration.ConnectionPoolChecker;
import org.glassfish.api.StartupRunLevel;
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

/**
 * @author mertcaliskan
 */
@Service(name = "healthcheck-cpool")
@RunLevel(value = StartupRunLevel.VAL)
public class ConnectionPoolHealthCheck extends BaseThresholdHealthCheck<HealthCheckConnectionPoolExecutionOptions,
        ConnectionPoolChecker> {

    @Inject
    private Domain domain;

    @Inject
    private Applications applications;

    @Inject
    private PoolManager poolManager;

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
    public HealthCheckResult doCheck() {
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
        Collection<JdbcResource> allResources = new ArrayList<JdbcResource>();
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

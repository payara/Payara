/*
 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 Copyright (c) 2015 C2B2 Consulting Limited. All rights reserved.
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

import fish.payara.nucleus.healthcheck.*;
import fish.payara.nucleus.healthcheck.configuration.Checker;
import fish.payara.nucleus.healthcheck.configuration.HealthCheckServiceConfiguration;
import fish.payara.nucleus.healthcheck.configuration.ThresholdDiagnosticsChecker;
import org.glassfish.api.admin.ServerEnvironment;
import org.jvnet.hk2.annotations.Contract;
import org.jvnet.hk2.annotations.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.concurrent.TimeUnit;

/**
 * @author mertcaliskan
 */
@Contract
public abstract class BaseThresholdHealthCheck extends BaseHealthCheck {

    private HealthCheckWithThresholdExecutionOptions options;

    protected <T extends BaseThresholdHealthCheck> void postConstruct(T t, Class checkerType) {
        if (configuration == null) {
            return;
        }
        this.checkerType = checkerType;

        ThresholdDiagnosticsChecker diagnosticsChecker = (ThresholdDiagnosticsChecker)
                configuration.getCheckerByType(checkerType);

        if (diagnosticsChecker != null) {
            options = new HealthCheckWithThresholdExecutionOptions(
                    Boolean.valueOf(diagnosticsChecker.getEnabled()),
                    diagnosticsChecker.getTime(),
                    asTimeUnit(diagnosticsChecker.getUnit()),
                    diagnosticsChecker.getPropertyValue(THRESHOLD_CRITICAL, THRESHOLD_DEFAULTVAL_CRITICAL),
                    diagnosticsChecker.getPropertyValue(THRESHOLD_WARNING, THRESHOLD_DEFAULTVAL_WARNING),
                    diagnosticsChecker.getPropertyValue(THRESHOLD_GOOD, THRESHOLD_DEFAULTVAL_GOOD));

            healthCheckService.registerCheck(diagnosticsChecker.getName(), t);
        }
    }

    protected HealthCheckResultStatus decideOnStatusWithRatio(Double percentage) {
        if (percentage > options.getThresholdCritical()) {
            return HealthCheckResultStatus.CRITICAL;
        }
        else if (percentage > options.getThresholdWarning()) {
            return HealthCheckResultStatus.WARNING;
        }
        else if (percentage > options.getThresholdGood()) {
            return HealthCheckResultStatus.GOOD;
        }
        else {
            return HealthCheckResultStatus.CHECK_ERROR;
        }
    }

    @Override
    public HealthCheckWithThresholdExecutionOptions getOptions() {
        return options;
    }
}

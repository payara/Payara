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

import fish.payara.nucleus.healthcheck.HealthCheckResultStatus;
import fish.payara.nucleus.healthcheck.HealthCheckWithThresholdExecutionOptions;
import fish.payara.nucleus.healthcheck.configuration.ThresholdDiagnosticsChecker;
import org.jvnet.hk2.annotations.Contract;

/**
 * @author mertcaliskan
 */
@Contract
public abstract class BaseThresholdHealthCheck<O extends HealthCheckWithThresholdExecutionOptions,
        C extends ThresholdDiagnosticsChecker> extends BaseHealthCheck<O, C> {

    public HealthCheckWithThresholdExecutionOptions constructThresholdOptions(ThresholdDiagnosticsChecker checker) {
        return new HealthCheckWithThresholdExecutionOptions(
                Boolean.valueOf(checker.getEnabled()),
                Long.parseLong(checker.getTime()),
                asTimeUnit(checker.getUnit()),
                checker.getPropertyValue(THRESHOLD_CRITICAL, THRESHOLD_DEFAULTVAL_CRITICAL),
                checker.getPropertyValue(THRESHOLD_WARNING, THRESHOLD_DEFAULTVAL_WARNING),
                checker.getPropertyValue(THRESHOLD_GOOD, THRESHOLD_DEFAULTVAL_GOOD));
    }

    protected HealthCheckResultStatus decideOnStatusWithRatio(Double percentage) {
        if (percentage > options.getThresholdCritical()) {
            return HealthCheckResultStatus.CRITICAL;
        } else if (percentage > options.getThresholdWarning()) {
            return HealthCheckResultStatus.WARNING;
        } else if (percentage >= options.getThresholdGood()) {
            return HealthCheckResultStatus.GOOD;
        } else {
            return HealthCheckResultStatus.FINE;
        }
    }

    public O getOptions() {
        return options;
    }
}

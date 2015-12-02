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

import fish.payara.nucleus.healthcheck.HealthCheckExecutionOptions;
import fish.payara.nucleus.healthcheck.HealthCheckResult;
import fish.payara.nucleus.healthcheck.HealthCheckResultStatus;
import fish.payara.nucleus.healthcheck.HealthCheckService;
import fish.payara.nucleus.healthcheck.configuration.Checker;

import java.util.concurrent.TimeUnit;

/**
 * @author mertcaliskan
 */
public abstract class BaseHealthCheck {

    protected final long ONE_KB = 1024;
    protected final long ONE_MB = ONE_KB * ONE_KB;
    protected final long ONE_GB = ONE_KB * ONE_MB;

    protected final long ONE_SEC = 1000;
    protected final long ONE_MIN = 60 * ONE_SEC;
    protected final long FIVE_MIN = 5 * ONE_MIN;

    public static final String THRESHOLD_CRITICAL = "threshold-critical";
    public static final String THRESHOLD_WARNING = "threshold-warning";
    public static final String THRESHOLD_GOOD = "threshold-good";
    public static final String THRESHOLD_DEFAULTVAL_CRITICAL = "80";
    public static final String THRESHOLD_DEFAULTVAL_WARNING = "50";
    public static final String THRESHOLD_DEFAULTVAL_GOOD = "0";

    protected HealthCheckExecutionOptions options;

    public abstract HealthCheckResult doCheck();
    protected abstract HealthCheckService getService();

    protected TimeUnit asTimeUnit(String unit) {
        return TimeUnit.valueOf(unit);
    }

    protected <T extends BaseHealthCheck> void postConstruct(Checker checker, T t) {
        HealthCheckExecutionOptions options = new HealthCheckExecutionOptions(checker.getTime(), asTimeUnit(checker.getUnit()));
        postConstruct(checker, t, options);
    }

    protected <T extends BaseHealthCheck> void postConstruct(Checker checker, T t, HealthCheckExecutionOptions options) {
        if (Boolean.valueOf(checker.getEnabled())) {
            this.options = options;
            getService().registerCheck(checker.getName(), t);
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

    protected HealthCheckResultStatus decideOnStatusWithDuration(long duration) {
        if (duration > FIVE_MIN) {
            return HealthCheckResultStatus.CRITICAL;
        }
        else if (duration > ONE_MIN) {
            return HealthCheckResultStatus.WARNING;
        }
        else if (duration > 0) {
            return HealthCheckResultStatus.GOOD;
        }
        else {
            return HealthCheckResultStatus.CHECK_ERROR;
        }
    }

    protected String prettyPrintBytes(long value) {
        String result;

        if (value / ONE_GB > 0) {
            result = (value / ONE_GB) + " Gb";
        }
        else if (value / ONE_MB > 0) {
            result = (value / ONE_MB) + " Mb";
        }
        else if (value / ONE_KB > 0) {
            result = (value / ONE_KB) + " Kb";
        }
        else {
            result = (value) + " bytes";
        }

        return result;
    }

    protected String prettyPrintDuration(long value) {
        long minutes = 0;
        long seconds = 0;
        StringBuilder sb = new StringBuilder();

        if (value > ONE_MIN) {
            minutes = TimeUnit.MILLISECONDS.toMinutes(value);
            value -= TimeUnit.MINUTES.toMillis(minutes);
        }
        if (value > ONE_SEC) {
            seconds = TimeUnit.MILLISECONDS.toSeconds(value);
            value -= TimeUnit.SECONDS.toMillis(seconds);
        }
        if (value >= 0) {
            if (minutes > 0) {
                sb.append(minutes + " minutes ");
            }
            if (seconds > 0) {
                sb.append(seconds + " seconds ");
            }
            if (value > 0) {
                sb.append(value);
                sb.append(" milliseconds");
            }
            return sb.toString();
        }
        else {
            return null;
        }
    }

    public HealthCheckExecutionOptions getOptions() {
        return options;
    }
}

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

    protected HealthCheckExecutionOptions options;
    public abstract HealthCheckResult doCheck();
    public HealthCheckExecutionOptions getOptions() {
        return options;
    }

    protected HealthCheckResultStatus decideOnStatusWithRatio(Double percentage) {
        if (percentage > 80) {
            return HealthCheckResultStatus.CRITICAL;
        }
        else if (percentage > 50) {
            return HealthCheckResultStatus.WARNING;
        }
        else if (percentage > 0) {
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
}

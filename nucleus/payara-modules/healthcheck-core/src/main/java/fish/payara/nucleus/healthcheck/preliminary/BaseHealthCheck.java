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

/**
 * @author mertcaliskan
 */
public abstract class BaseHealthCheck {

    protected final long ONE_KB = 1024;
    protected final long ONE_MB = ONE_KB * ONE_KB;
    protected final long ONE_GB = ONE_KB * ONE_MB;

    protected HealthCheckExecutionOptions options;
    public abstract HealthCheckResult doCheck();
    public HealthCheckExecutionOptions getOptions() {
        return options;
    }

    protected HealthCheckResultStatus decideOnStatus(Double percentage) {
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
}

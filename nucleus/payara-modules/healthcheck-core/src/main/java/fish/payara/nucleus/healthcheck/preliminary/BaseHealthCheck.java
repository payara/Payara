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
import org.glassfish.api.admin.config.ConfigExtension;
import org.jvnet.hk2.annotations.Contract;
import org.jvnet.hk2.annotations.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.concurrent.TimeUnit;

/**
 * @author mertcaliskan
 */
@Contract
public abstract class BaseHealthCheck implements HealthCheckConstants {

    @Inject
    protected HealthCheckService healthCheckService;

    @Inject
    @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    @Optional
    HealthCheckServiceConfiguration configuration;

    private HealthCheckExecutionOptions options;
    protected Class checkerType;

    public abstract HealthCheckResult doCheck();

    protected <T extends BaseHealthCheck> void postConstruct(T t, Class checkerType) {
        if (configuration == null) {
            return;
        }

        this.checkerType = checkerType;
        Checker checker = configuration.getCheckerByType(checkerType);
        if (checker != null) {
            options = new HealthCheckExecutionOptions(Boolean.valueOf(checker.getEnabled()), checker.getTime(), asTimeUnit(checker.getUnit()));
            healthCheckService.registerCheck(checker.getName(), t);
        }
    }

    protected TimeUnit asTimeUnit(String unit) {
        return TimeUnit.valueOf(unit);
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

    public <T extends ConfigExtension> Class<T> getCheckerType() {
        return checkerType;
    }
}

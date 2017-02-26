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

import fish.payara.nucleus.healthcheck.*;
import fish.payara.nucleus.healthcheck.configuration.Checker;
import fish.payara.nucleus.healthcheck.configuration.HealthCheckServiceConfiguration;
import fish.payara.nucleus.notification.NotificationService;
import fish.payara.nucleus.notification.domain.EventSource;
import fish.payara.nucleus.notification.domain.NotificationEvent;
import fish.payara.nucleus.notification.domain.NotificationEventFactory;
import fish.payara.nucleus.notification.domain.NotifierExecutionOptions;
import fish.payara.nucleus.notification.service.NotificationEventFactoryStore;
import org.glassfish.api.admin.ServerEnvironment;
import org.jvnet.hk2.annotations.Contract;
import org.jvnet.hk2.annotations.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * @author mertcaliskan
 */
@Contract
public abstract class BaseHealthCheck<O extends HealthCheckExecutionOptions, C extends Checker> implements HealthCheckConstants {

    @Inject
    protected HealthCheckService healthCheckService;

    @Inject
    @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    @Optional
    HealthCheckServiceConfiguration configuration;

    @Inject
    NotificationService notificationService;

    @Inject
    private NotificationEventFactoryStore eventFactoryStore;

    @Inject
    private HistoricHealthCheckEventStore healthCheckEventStore;

    protected O options;
    protected Class<C> checkerType;

    public abstract HealthCheckResult doCheck();
    public abstract O constructOptions(C c);

    protected <T extends BaseHealthCheck> O postConstruct(T t, Class<C> checkerType) {
        this.checkerType = checkerType;
        if (configuration == null) {
            return null;
        }

        C checker = configuration.getCheckerByType(this.checkerType);
        if (checker != null) {
            options = constructOptions(checker);
            healthCheckService.registerCheck(checker.getName(), t);
        }

        return options;
    }

    protected HealthCheckExecutionOptions constructBaseOptions(Checker checker) {
        return new HealthCheckExecutionOptions(
                Boolean.valueOf(checker.getEnabled()),
                Long.parseLong(checker.getTime()),
                asTimeUnit(checker.getUnit()));
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

    protected String prettyPrintStackTrace(StackTraceElement[] elements) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement traceElement : elements) {
            sb.append("\tat ").append(traceElement);
        }
        return sb.toString();
    }

    public O getOptions() {
        return options;
    }

    public void setOptions(O options) {
        this.options = options;
    }

    public Class<C> getCheckerType() {
        return checkerType;
    }

    public void sendNotification(Level level, String message, Object[] parameters) {
        String subject = "Health Check notification with severity level: " + level.getName();

        for (NotifierExecutionOptions notifierExecutionOptions : healthCheckService.getNotifierExecutionOptionsList()) {
            if (notifierExecutionOptions.isEnabled()) {
                NotificationEventFactory notificationEventFactory = eventFactoryStore.get(notifierExecutionOptions.getNotifierType());
                NotificationEvent notificationEvent = notificationEventFactory.buildNotificationEvent(level, subject, message, parameters);
                notificationService.notify(EventSource.HEALTHCHECK, notificationEvent);
            }
        }

        if (healthCheckService.isHistoricalTraceEnabled()) {
            healthCheckEventStore.addTrace(new Date().getTime(), level, subject, message, parameters);
        }
    }
}

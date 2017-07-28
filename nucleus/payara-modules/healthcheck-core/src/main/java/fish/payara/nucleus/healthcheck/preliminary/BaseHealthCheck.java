/*
 *
 * Copyright (c) 2016-2017 Payara Foundation and/or its affiliates. All rights reserved.
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

        if (healthCheckService.getNotifierExecutionOptionsList() != null) {

            for (int i = 0; i < healthCheckService.getNotifierExecutionOptionsList().size(); i++) {
                NotifierExecutionOptions notifierExecutionOptions = healthCheckService.getNotifierExecutionOptionsList().get(i);

                if (notifierExecutionOptions.isEnabled()) {
                    NotificationEventFactory notificationEventFactory = eventFactoryStore.get(notifierExecutionOptions.getNotifierType());
                    NotificationEvent notificationEvent = notificationEventFactory.buildNotificationEvent(level, subject, message, parameters);
                    notificationService.notify(EventSource.HEALTHCHECK, notificationEvent);
                }
            }
        }

        if (healthCheckService.isHistoricalTraceEnabled()) {
            healthCheckEventStore.addTrace(new Date().getTime(), level, subject, message, parameters);
        }
    }
}
/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2016-2025] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.nucleus.healthcheck.preliminary;

import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import fish.payara.internal.notification.EventLevel;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sun.enterprise.util.LocalStringManagerImpl;

import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.hk2.api.messaging.Topic;
import org.jvnet.hk2.annotations.Contract;
import org.jvnet.hk2.annotations.Optional;

import fish.payara.internal.notification.PayaraNotification;
import fish.payara.internal.notification.PayaraNotificationFactory;
import fish.payara.notification.healthcheck.HealthCheckNotificationData;
import fish.payara.notification.healthcheck.HealthCheckResultEntry;
import fish.payara.notification.healthcheck.HealthCheckResultStatus;
import fish.payara.nucleus.healthcheck.HealthCheckConstants;
import fish.payara.nucleus.healthcheck.HealthCheckExecutionOptions;
import fish.payara.nucleus.healthcheck.HealthCheckResult;
import fish.payara.nucleus.healthcheck.HealthCheckService;
import fish.payara.nucleus.healthcheck.HistoricHealthCheckEventStore;
import fish.payara.nucleus.healthcheck.configuration.Checker;
import fish.payara.nucleus.healthcheck.configuration.HealthCheckServiceConfiguration;

/**
 * Base class for all health check services
 * @author mertcaliskan
 * @since 4.1.1.161
 */
@Contract
public abstract class BaseHealthCheck<O extends HealthCheckExecutionOptions, C extends Checker> implements HealthCheckConstants {

    final private static LocalStringManagerImpl strings = new LocalStringManagerImpl(BaseHealthCheck.class);

    @Inject
    protected HealthCheckService healthCheckService;

    @Inject
    @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    @Optional
    HealthCheckServiceConfiguration configuration;

    @Inject
    private Topic<PayaraNotification> notificationEventBus;

    @Inject
    private PayaraNotificationFactory notificationFactory;

    @Inject
    private HistoricHealthCheckEventStore healthCheckEventStore;

    protected O options;
    protected Class<C> checkerType;

    private final AtomicInteger checksDone = new AtomicInteger();
    private final AtomicInteger checksFailed = new AtomicInteger();
    private final AtomicBoolean inProcess = new AtomicBoolean(false);
    private volatile HealthCheckResult mostRecentResult;

    public final HealthCheckResult doCheck() {
        if (!getOptions().isEnabled() || inProcess.compareAndSet(false, true)) {
            return null;
        }
        try {
            HealthCheckResult result = doCheckInternal();
            mostRecentResult = result;
            return result;
        } catch (Exception ex) {
            checksFailed.incrementAndGet();
            throw ex;
        } finally {
            inProcess.set(false);
            checksDone.incrementAndGet();
        }
    }

    protected abstract HealthCheckResult doCheckInternal();

    protected abstract EventLevel createNotificationEventLevel (HealthCheckResultStatus checkResult);
    public abstract O constructOptions(C c);

    public HealthCheckResultStatus getMostRecentCumulativeStatus() {
        return mostRecentResult.getCumulativeStatus();
    }
    
     public HealthCheckResult getMostRecentResult() {
        return mostRecentResult;
    }

    public boolean isInProgress() {
        return inProcess.get();
    }

    public int getChecksDone() {
        return checksDone.get();
    }

    public int getChecksFailed() {
        return checksFailed.get();
    }

    public boolean isReady() {
        O options = getOptions();
        return !isInProgress() && options != null && options.isEnabled();
    }
    
    public boolean isEnabled(){ 
        return getOptions().isEnabled();
    }

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
                Boolean.parseBoolean(checker.getEnabled()),
                Long.parseLong(checker.getTime()),
                asTimeUnit(checker.getUnit()),
                Boolean.parseBoolean(checker.getAddToMicroProfileHealth()));
    }

    /**
     * Converts a string representing a timeunit to the value
     * i.e. {@code "MILLISECONDS"} to {@link TimeUnit#MILLISECONDS}
     * @param unit A string representing a TimeUnit.
     * @return The TimeUnit.
     */
    protected TimeUnit asTimeUnit(String unit) {
        return TimeUnit.valueOf(unit);
    }

    /**
     * Determines the level of the healthcheck based on a time
     * @param duration length of time taken in milliseconds
     * @return {@link HealthCheckResultStatus.CRITICAL} if duration > 5 minutes;
     * {@link HealthCheckResultStatus.WARNING} if duration > 1 minute;
     * {@link HealthCheckResultStatus.GOOD} if duration > 0;
     * otherwise {@link HealthCheckResultStatus.CHECK_ERROR}
     */
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

    /**
     * Returns the amount in a human-friendly string <p>
     * i.e. with an input of 1024 the result will be "1 Kb";
     * for an input of 20000000 the result would be "19 Mb"
     * <p>
     * Result is always rounded down number of the largest unit of which there is at least one
     * of that unit.
     * <p>
     * Unit can be Gb, Mb, Kb or bytes.
     * @param value The amount of bytes.
     * @return Value as a human-friendly string.
     */
    protected String prettyPrintBytes(long value) {
        String result;
        DecimalFormat format = new DecimalFormat("#.00");
        if (value / ONE_GB > 0) {
            result = format.format((double)value / ONE_GB) + " Gb";
        }
        else if (value / ONE_MB > 0) {
            result = format.format((double)value / ONE_MB) + " Mb";
        }
        else if (value / ONE_KB > 0) {
            result = format.format((double)value / ONE_KB) + " Kb";
        }
        else {
            result = (value) + " bytes";
        }

        return result;
    }

    /**
     * @param elements Array of stack trace elements.
     * @return A string of tab-separated stack trace elements.
     */
    protected String prettyPrintStackTrace(StackTraceElement[] elements) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement traceElement : elements) {
            sb.append("\tat ").append(traceElement);
        }
        return sb.toString();
    }

    /**
     * @return A human-friendly description of the healthcheck
     * @since 4.1.2.173
     */
    public String resolveDescription() {
        return strings.getLocalString(getDescription(), "");
    }

    /**
     * @return The key for a human-friendly description of the healthcheck
     */
    protected abstract String getDescription();

    public O getOptions() {
        return options;
    }

    public void setOptions(O options) {
        this.options = options;
    }

    public Class<C> getCheckerType() {
        return checkerType;
    }

    /**
     * Sends a notification to all notifier enabled with the healthcheck service.
     * <p>
     * @param checkResult information collected by the regarding health check service
     * @param level Level of the message to send
     * @param name Name of the checker executed
     */
    public void sendNotification(String name, HealthCheckResult checkResult, Level level) {
        String message = "{0}:{1}";
        String subject = "Health Check notification with severity level: " + level.getName();
        String messageFormatted = getMessageFormatted(new Object[]{name, getCumulativeMessages(checkResult.getEntries())});

        Collection<String> enabledNotifiers = healthCheckService.getEnabledNotifiers();
        PayaraNotification notification = notificationFactory.newBuilder()
            .whitelist(enabledNotifiers.toArray(new String[0]))
            .subject(name)
            .message(messageFormatted)
            .data(new HealthCheckNotificationData(checkResult.getEntries()))
            .eventType(level.getName())
            .level(this.createNotificationEventLevel(checkResult.getCumulativeStatus()))
            .build();
        notificationEventBus.publish(notification);

        if (healthCheckService.isHistoricalTraceEnabled()) {
            healthCheckEventStore.addTrace(new Date().getTime(), level, subject, message,
                    new Object[]{name, checkResult.getEntries().toString()});
        }
    }

    private String getMessageFormatted(Object[] parameters) {
        String formattedMessage = null;
        if (parameters != null && parameters.length > 0) {
            formattedMessage = MessageFormat.format("{0}:{1}", parameters);
        }
        return formattedMessage;
    }

    private String getCumulativeMessages(List<HealthCheckResultEntry> entries) {
        return "Health Check Result:" + entries.toString();
    }

}
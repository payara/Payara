/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016-2018 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.nucleus.requesttracing;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Server;
import fish.payara.notification.requesttracing.RequestTrace;
import fish.payara.nucleus.eventbus.ClusterMessage;
import fish.payara.nucleus.eventbus.EventBus;
import fish.payara.nucleus.events.HazelcastEvents;
import fish.payara.nucleus.hazelcast.HazelcastCore;
import fish.payara.nucleus.notification.NotificationService;
import fish.payara.nucleus.notification.TimeUtil;
import fish.payara.nucleus.notification.configuration.Notifier;
import fish.payara.nucleus.notification.configuration.NotifierConfigurationType;
import fish.payara.nucleus.notification.domain.*;
import fish.payara.nucleus.notification.log.LogNotifier;
import fish.payara.nucleus.notification.log.LogNotifierExecutionOptions;
import fish.payara.nucleus.notification.service.NotificationEventFactoryStore;
import fish.payara.nucleus.requesttracing.configuration.RequestTracingServiceConfiguration;
import fish.payara.nucleus.requesttracing.store.RequestTraceStoreFactory;
import fish.payara.nucleus.requesttracing.store.RequestTraceStoreInterface;
import fish.payara.notification.requesttracing.EventType;
import fish.payara.notification.requesttracing.RequestTraceSpan;
import fish.payara.notification.requesttracing.RequestTraceSpanLog;
import fish.payara.nucleus.requesttracing.domain.execoptions.RequestTracingExecutionOptions;
import fish.payara.nucleus.requesttracing.events.RequestTracingEvents;
import fish.payara.nucleus.requesttracing.sampling.AdaptiveSampleFilter;
import fish.payara.nucleus.requesttracing.sampling.SampleFilter;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.EventTypes;
import org.glassfish.api.event.Events;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.*;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main service class that provides methods used by interceptors for tracing requests.
 *
 * @author mertcaliskan
 * @since 4.1.1.163
 */
@Service(name = "requesttracing-service")
@RunLevel(StartupRunLevel.VAL)
public class RequestTracingService implements EventListener, ConfigListener {

    private static final Logger logger = Logger.getLogger(RequestTracingService.class.getCanonicalName());

    public static final String EVENT_BUS_LISTENER_NAME = "RequestTracingEvents";

    private static final int SECOND = 1;
    private static final int MINUTE = 60 * SECOND;
    private static final int HOUR = 60 * MINUTE;
    private static final int DAY = 24 * HOUR;

    @Inject
    @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    @Optional
    RequestTracingServiceConfiguration configuration;

    @Inject
    private Events events;

    @Inject
    private EventBus eventBus;

    @Inject
    private Domain domain;

    @Inject
    private Server server;

    @Inject
    ServerEnvironment env;

    @Inject
    Transactions transactions;

    @Inject
    private ServiceLocator habitat;

    @Inject
    NotificationService notificationService;

    @Inject
    RequestTraceSpanStore requestEventStore;

    @Inject
    NotificationEventFactoryStore eventFactoryStore;

    @Inject
    private NotifierExecutionOptionsFactoryStore executionOptionsFactoryStore;

    @Inject
    private HazelcastCore hazelcast;

    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

    private RequestTracingExecutionOptions executionOptions = new RequestTracingExecutionOptions();

    private RequestTraceStoreInterface historicRequestTraceStore;
    private RequestTraceStoreInterface requestTraceStore;

    /**
     * The filter which determines whether to sample a given request
     */
    private SampleFilter sampleFilter;

    @PostConstruct
    void postConstruct() {
        events.register(this);
        configuration = habitat.getService(RequestTracingServiceConfiguration.class);
        if (configuration != null && configuration.getNotifierList() != null && configuration.getNotifierList().isEmpty()) {
            try {
                ConfigSupport.apply(new SingleConfigCode<RequestTracingServiceConfiguration>() {
                    @Override
                    public Object run(final RequestTracingServiceConfiguration configurationProxy)
                            throws PropertyVetoException, TransactionFailure {
                        LogNotifier notifier = configurationProxy.createChild(LogNotifier.class);
                        configurationProxy.getNotifierList().add(notifier);
                        return configurationProxy;
                    }
                }, configuration);
            } catch (TransactionFailure e) {
                logger.log(Level.SEVERE, "Error occurred while setting initial log notifier", e);
            }
        }
    }

    @Override
    public void event(Event event) {
        // If Hazelcast is enabled, wait for it, otherwise just bootstrap when the server is ready
        if (hazelcast.isEnabled()) {
            if (event.is(HazelcastEvents.HAZELCAST_BOOTSTRAP_COMPLETE)) {
                bootstrapRequestTracingService();
            }
        } else {
            if (event.is(EventTypes.SERVER_READY)) {
                bootstrapRequestTracingService();
            }
        }

        // If Hazelcast has shutdown, reinitialise request tracing
        if (event.is(HazelcastEvents.HAZELCAST_SHUTDOWN_COMPLETE)) {
            bootstrapRequestTracingService();
        }

        transactions.addListenerForType(RequestTracingServiceConfiguration.class, this);
    }

    /**
     * Starts the request tracing service
     *
     * @since 4.1.1.171
     */
    public void bootstrapRequestTracingService() {
        if (configuration != null) {
            executionOptions.setEnabled(Boolean.parseBoolean(configuration.getEnabled()));

            executionOptions.setSampleRate(Double.valueOf(configuration.getSampleRate()));
            executionOptions.setAdaptiveSamplingEnabled(Boolean.parseBoolean(configuration.getAdaptiveSamplingEnabled()));
            executionOptions.setAdaptiveSamplingTargetCount(Integer.valueOf(configuration.getAdaptiveSamplingTargetCount()));
            executionOptions.setAdaptiveSamplingTimeValue(Integer.valueOf(configuration.getAdaptiveSamplingTimeValue()));
            executionOptions.setAdaptiveSamplingTimeUnit(TimeUnit.valueOf(configuration.getAdaptiveSamplingTimeUnit()));

            executionOptions.setApplicationsOnlyEnabled(Boolean.parseBoolean(configuration.getApplicationsOnlyEnabled()));
            executionOptions.setThresholdUnit(TimeUnit.valueOf(configuration.getThresholdUnit()));
            executionOptions.setThresholdValue(Long.parseLong(configuration.getThresholdValue()));
            executionOptions.setSampleRateFirstEnabled(Boolean.parseBoolean(configuration.getSampleRateFirstEnabled()));

            executionOptions.setTraceStoreSize(Integer.parseInt(configuration.getTraceStoreSize()));
            executionOptions.setTraceStoreTimeout(TimeUtil.setStoreTimeLimit(configuration.getTraceStoreTimeout()));
            executionOptions.setReservoirSamplingEnabled(Boolean.parseBoolean(configuration.getReservoirSamplingEnabled()));

            executionOptions.setHistoricTraceStoreEnabled(Boolean.parseBoolean(configuration.getHistoricTraceStoreEnabled()));
            executionOptions.setHistoricTraceStoreSize(Integer.parseInt(configuration.getHistoricTraceStoreSize()));
            executionOptions.setHistoricTraceStoreTimeout(TimeUtil.setStoreTimeLimit(configuration.getHistoricTraceStoreTimeout()));

            bootstrapNotifierList();
        }

        if (executionOptions != null && executionOptions.isEnabled()) {
            // Set up the historic request trace store if enabled
            if (executionOptions.isHistoricTraceStoreEnabled()) {
                historicRequestTraceStore = RequestTraceStoreFactory.getStore(events, executionOptions.getReservoirSamplingEnabled(), true);
                historicRequestTraceStore.setSize(executionOptions.getHistoricTraceStoreSize());

                // Disable cleanup task if it's null, less than 0, or reservoir sampling is enabled
                if (executionOptions.getTraceStoreTimeout() != null
                        && executionOptions.getTraceStoreTimeout() > 0
                        && !executionOptions.getReservoirSamplingEnabled()) {
                    // if timeout is bigger than 5 minutes execute the cleaner task in 5 minutes periods,
                    // if not use timeout value as period
                    long period = executionOptions.getTraceStoreTimeout() > TimeUtil.CLEANUP_TASK_FIVE_MIN_PERIOD
                            ? TimeUtil.CLEANUP_TASK_FIVE_MIN_PERIOD : executionOptions.getTraceStoreTimeout();
                    scheduledExecutorService.scheduleAtFixedRate(new RequestTraceStoreCleanupTask(
                            executionOptions.getTraceStoreTimeout(), historicRequestTraceStore),
                            0, period, TimeUnit.SECONDS);
                }
            }

            // Set up the general request trace store
            requestTraceStore = RequestTraceStoreFactory.getStore(events, executionOptions.getReservoirSamplingEnabled(), false);
            requestTraceStore.setSize(executionOptions.getTraceStoreSize());

            // Disable cleanup task if it's null, less than 0, or reservoir sampling is enabled
            if (executionOptions.getTraceStoreTimeout() != null && executionOptions.getTraceStoreTimeout() > 0
                    && !executionOptions.getReservoirSamplingEnabled()) {
                // if timeout is bigger than 5 minutes execute the cleaner task in 5 minutes periods,
                // if not use timeout value as period
                long period = executionOptions.getTraceStoreTimeout() > TimeUtil.CLEANUP_TASK_FIVE_MIN_PERIOD
                        ? TimeUtil.CLEANUP_TASK_FIVE_MIN_PERIOD : executionOptions.getTraceStoreTimeout();
                scheduledExecutorService.scheduleAtFixedRate(new RequestTraceStoreCleanupTask(
                        executionOptions.getTraceStoreTimeout(), requestTraceStore),
                        0, period, TimeUnit.SECONDS);
            }

            if (executionOptions.getAdaptiveSamplingEnabled()) {
                sampleFilter = new AdaptiveSampleFilter(executionOptions.getSampleRate(), executionOptions.getAdaptiveSamplingTargetCount(),
                        executionOptions.getAdaptiveSamplingTimeValue(), executionOptions.getAdaptiveSamplingTimeUnit());
            } else {
                sampleFilter = new SampleFilter(executionOptions.getSampleRate());
            }

            logger.log(Level.INFO, "Payara Request Tracing Service Started with configuration: {0}", executionOptions);
        }
    }

    /**
     * Configures notifiers with request tracing and starts any enabled ones. If no options are set then the log
     * notifier is automatically turned on.
     *
     * @since 4.1.2.173
     */
    public void bootstrapNotifierList() {
        executionOptions.resetNotifierExecutionOptions();
        if (configuration.getNotifierList() != null) {
            for (Notifier notifier : configuration.getNotifierList()) {
                ConfigView view = ConfigSupport.getImpl(notifier);
                NotifierConfigurationType annotation = view.getProxyType().getAnnotation(NotifierConfigurationType.class);
                executionOptions.addNotifierExecutionOption(executionOptionsFactoryStore.get(annotation.type()).build(notifier));
            }
        }
        if (executionOptions.getNotifierExecutionOptionsList().isEmpty()) {
            // Add logging execution options by default
            LogNotifierExecutionOptions logNotifierExecutionOptions = new LogNotifierExecutionOptions();
            logNotifierExecutionOptions.setEnabled(true);
            executionOptions.addNotifierExecutionOption(logNotifierExecutionOptions);
        }
    }

    /**
     * Retrieves the current Conversation ID
     *
     * @return
     */
    public UUID getConversationID() {
        return requestEventStore.getTraceID();
    }

    public UUID getStartingTraceID() {
        return requestEventStore.getTrace().getTraceSpans().getFirst().getId();
    }

    /**
     * Reset the conversation ID This is especially useful for trace propagation across threads when the event tracer
     * can receive the conversation ID propagated to it
     *
     * @param newID
     */
    public void setTraceId(UUID newID) {
        requestEventStore.setTraceId(newID);
    }

    /**
     * Returns true if a trace has started and not yet completed. NOTE: This only applies to traces started using the
     * request tracing service; traces started using OpenTracing *MAY* not be picked up by this (for example, 
     * if you're using the OpenTracing MockTracer instead of the in-built one).
     *
     * @return
     */
    public boolean isTraceInProgress() {
        return requestEventStore.isTraceInProgress();
    }

    /**
     * Starts a new request trace
     *
     * @return a unique identifier for the request trace
     */
    public RequestTraceSpan startTrace(String traceName) {
        return startTrace(new RequestTraceSpan(EventType.TRACE_START, traceName));
    }

    public RequestTraceSpan startTrace(RequestTraceSpan span) {
        if (shouldStartTrace()) {
            span.addSpanTag("Server", server.getName());
            span.addSpanTag("Domain", domain.getName());
            requestEventStore.storeEvent(span);
            return span;
        } else {
            return null;
        }
    }

    public RequestTraceSpan startTrace(RequestTraceSpan span, long timestampMillis) {
        if (shouldStartTrace()) {
            span.addSpanTag("Server", server.getName());
            span.addSpanTag("Domain", domain.getName());
            requestEventStore.storeEvent(span, timestampMillis);
            return span;
        } else {
            return null;
        }
    }

    public RequestTraceSpan startTrace(UUID propagatedTraceId, UUID propagatedParentId,
            RequestTraceSpan.SpanContextRelationshipType propagatedRelationshipType, String traceName) {
        if (!isRequestTracingEnabled()) {
            return null;
        }
        RequestTraceSpan span = new RequestTraceSpan(EventType.PROPAGATED_TRACE, traceName,
                propagatedTraceId, propagatedParentId, propagatedRelationshipType);
        span.addSpanTag("Server", server.getName());
        span.addSpanTag("Domain", domain.getName());
        requestEventStore.storeEvent(span);
        return span;
    }

    /**
     * Adds a new event to the request trace currently in progress
     *
     * @param requestEvent
     */
    public void traceSpan(RequestTraceSpan requestEvent) {
        if (isRequestTracingEnabled() && isTraceInProgress()) {
            requestEventStore.storeEvent(requestEvent);
        }
    }

    public void traceSpan(RequestTraceSpan requestEvent, long timestampMillis) {
        if (isRequestTracingEnabled() && isTraceInProgress()) {
            requestEventStore.storeEvent(requestEvent, timestampMillis);
        }
    }

    private boolean shouldStartTrace() {
        if (!isRequestTracingEnabled()) {
            return false;
        }

        // Check if the trace came from an admin listener. If it did, and 'applications only' is enabled, ignore the trace.
        if (executionOptions.getApplicationsOnlyEnabled() == true
                && Thread.currentThread().getName().matches("admin-thread-pool::admin-listener\\([0-9]+\\)")) {
            return false;
        }

        // Determine whether to sample the request, if sampleRateFirstEnabled is true
        if (executionOptions.getSampleRateFirstEnabled() && !sampleFilter.sample()) {
            return false;
        }

        return true;
    }
    
    /**
     *
     */
    public void endTrace() {
        if (!isRequestTracingEnabled() || !isTraceInProgress()) {
            return;
        }
        requestEventStore.endTrace();
        Long thresholdValueInNanos = getThresholdValueInNanos();

        long elapsedTime = requestEventStore.getElapsedTime();
        long elapsedTimeInNanos = TimeUnit.NANOSECONDS.convert(elapsedTime, TimeUnit.MILLISECONDS);
        if (elapsedTimeInNanos - thresholdValueInNanos > 0) {
            // Determine whether to sample the request, if sampleRateFirstEnabled is false
            if (!executionOptions.getSampleRateFirstEnabled()) {
                if (!sampleFilter.sample()) {
                    requestEventStore.flushStore();
                    return;
                }
            }
            
            final RequestTrace requestTrace = requestEventStore.getTrace();

            Runnable addTask = new Runnable() {

                @Override
                public void run() {
                    RequestTrace removedTrace = requestTraceStore.addTrace(requestTrace);

                    // Store the trace in the historic trace store if it's enabled, avoiding recalculation
                    if (executionOptions.isHistoricTraceStoreEnabled()) {
                        historicRequestTraceStore.addTrace(requestTrace, removedTrace);
                    }

                    if (removedTrace != null) {
                        if (hazelcast.isEnabled()) {
                            eventBus.publish(EVENT_BUS_LISTENER_NAME, new ClusterMessage(
                                    RequestTracingEvents.STORE_FULL.toString()));
                        } else {
                            events.send(new EventListener.Event(RequestTracingEvents.STORE_FULL));
                        }
                    }
                }
            };

            scheduledExecutorService.submit(addTask);

            for (NotifierExecutionOptions notifierExecutionOptions : executionOptions.getNotifierExecutionOptionsList().values()) {
                if (notifierExecutionOptions.isEnabled()) {
                    NotificationEventFactory notificationEventFactory = eventFactoryStore.get(
                            notifierExecutionOptions.getNotifierType());
                    String subject = "Request execution time: " + elapsedTime + "(ms) exceeded the acceptable threshold";
                    NotificationEvent notificationEvent = notificationEventFactory.buildNotificationEvent(
                            subject, requestTrace);
                    notificationService.notify(EventSource.REQUESTTRACING, notificationEvent);
                }
            }
        }
        requestEventStore.flushStore();
    }

    public void addSpanLog(RequestTraceSpanLog spanLog) {
        if (!isRequestTracingEnabled() || !isTraceInProgress()) {
            return;
        }

        requestEventStore.getTrace().addSpanLog(spanLog);
    }

    /**
     *
     * @return @since 4.1.1.164
     */
    public Long getThresholdValueInNanos() {
        if (executionOptions != null) {
            return TimeUnit.NANOSECONDS.convert(executionOptions.getThresholdValue(),
                    executionOptions.getThresholdUnit());
        }
        return null;
    }

    public boolean isRequestTracingEnabled() {
        return executionOptions != null && executionOptions.isEnabled();
    }

    public RequestTracingExecutionOptions getExecutionOptions() {
        return executionOptions;
    }

    @Override
    public UnprocessedChangeEvents changed(PropertyChangeEvent[] events) {
        boolean isCurrentInstanceMatchTarget = false;
        if (env.isInstance()) {
            isCurrentInstanceMatchTarget = true;
        } else {
            for (PropertyChangeEvent pe : events) {
                ConfigBeanProxy proxy = (ConfigBeanProxy) pe.getSource();
                while (proxy != null && !(proxy instanceof Config)) {
                    proxy = proxy.getParent();
                }

                if (proxy != null && ((Config) proxy).isDas()) {
                    isCurrentInstanceMatchTarget = true;
                    break;
                }
            }
        }

        if (isCurrentInstanceMatchTarget) {
            return ConfigSupport.sortAndDispatch(events, new Changed() {

                @Override
                public <T extends ConfigBeanProxy> NotProcessed changed(TYPE type, Class<T> changedType, T changedInstance) {

                    if (changedType.equals(RequestTracingServiceConfiguration.class)) {
                        configuration = (RequestTracingServiceConfiguration) changedInstance;
                    }
                    return null;
                }
            }, logger);
        }
        return null;
    }

    /**
     * Returns the RequestTraceStore used for storing historical traces
     *
     * @return
     */
    public RequestTraceStoreInterface getHistoricRequestTraceStore() {
        return historicRequestTraceStore;
    }

    /**
     * Returns the RequestTraceStore used for storing traces
     *
     * @return
     */
    public RequestTraceStoreInterface getRequestTraceStore() {
        return requestTraceStore;
    }
    
}

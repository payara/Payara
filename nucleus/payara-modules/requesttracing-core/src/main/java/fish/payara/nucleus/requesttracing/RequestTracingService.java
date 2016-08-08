/*
 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 Copyright (c) 2016 C2B2 Consulting Limited. All rights reserved.
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
package fish.payara.nucleus.requesttracing;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Server;
import fish.payara.nucleus.notification.NotificationService;
import fish.payara.nucleus.notification.configuration.Notifier;
import fish.payara.nucleus.requesttracing.configuration.RequestTracingServiceConfiguration;
import fish.payara.nucleus.requesttracing.domain.EventType;
import fish.payara.nucleus.requesttracing.domain.RequestEvent;
import fish.payara.nucleus.requesttracing.domain.execoptions.NotifierExecutionOptions;
import fish.payara.nucleus.requesttracing.domain.execoptions.NotifierExecutionOptionsFactory;
import fish.payara.nucleus.requesttracing.domain.execoptions.RequestTracingExecutionOptions;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.EventTypes;
import org.glassfish.api.event.Events;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * @author mertcaliskan
 * Main service class that provides methods used by interceptors for tracing requests.
 */
@Service(name = "requesttracing-service")
@RunLevel(StartupRunLevel.VAL)
public class RequestTracingService implements EventListener {

    private static final Logger logger = Logger.getLogger(RequestTracingService.class.getCanonicalName());

    @Inject
    @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    @Optional
    RequestTracingServiceConfiguration configuration;

    @Inject
    private Events events;

    @Inject
    private Domain domain;

    @Inject
    private Server server;

    @Inject
    NotificationService notificationService;

    @Inject
    RequestEventStore requestEventStore;

    @Inject
    private RequestTracingNotificationEventFactory eventFactory;

    @Inject
    NotifierExecutionOptionsFactory executionOptionsFactory;

    private RequestTracingExecutionOptions executionOptions = new RequestTracingExecutionOptions();

    @PostConstruct
    void postConstruct() {
        if (configuration != null) {
            executionOptions.setEnabled(configuration.getEnabled());
            executionOptions.setThresholdValue(configuration.getThresholdValue());
            executionOptions.setThresholdUnit(TimeUnit.valueOf(configuration.getThresholdUnit()));

            for (Notifier notifier : configuration.getNotifierList()) {
                executionOptions.addNotifierExecutionOption(executionOptionsFactory.build(notifier));
            }
        }
        events.register(this);
    }

    @Override
    public void event(Event event) {
        if (event.is(EventTypes.SERVER_READY)) {
            bootstrapRequestTracingService();
        }
    }

    private void bootstrapRequestTracingService() {
        if (executionOptions != null && executionOptions.isEnabled()) {
            logger.info("Payara Request Tracing Service Started with configuration: " + executionOptions);
        }
    }

    public UUID startTrace() {
        if (!isRequestTracingEnabled()) {
            return null;
        }
        RequestEvent requestEvent = new RequestEvent(EventType.TRACE_START, "StartTrace");
        requestEvent.addProperty("Server", server.getName());
        requestEvent.addProperty("Domain", domain.getName());
        requestEventStore.storeEvent(requestEvent);
        return requestEvent.getId();
    }

    public void traceRequestEvent(RequestEvent requestEvent) {
        if (isRequestTracingEnabled()) {
            requestEventStore.storeEvent(requestEvent);
        }
    }

    public void endTrace() {
        if (!isRequestTracingEnabled()) {
            return;
        }
        requestEventStore.storeEvent(new RequestEvent(EventType.TRACE_END, "TraceEnd"));
        Long thresholdValueInMillis = getThresholdValueInMillis();

        long elapsedTime = requestEventStore.getElapsedTime();
        if ( elapsedTime > thresholdValueInMillis) {
            for (NotifierExecutionOptions notifierExecutionOptions : executionOptions.getNotifierExecutionOptionsList().values()) {
                if (notifierExecutionOptions.isEnabled()) {
                    notificationService.notify(eventFactory.build(elapsedTime, notifierExecutionOptions.getNotifierType()));
                }
            }
        }
        requestEventStore.flushStore();
    }

    public Long getThresholdValueInMillis() {
        if (executionOptions != null) {
            return TimeUnit.MILLISECONDS.convert(executionOptions.getThresholdValue(), executionOptions.getThresholdUnit());
        }
        return null;
    }

    public boolean isRequestTracingEnabled() {
        return executionOptions != null && executionOptions.isEnabled();
    }

    public RequestTracingExecutionOptions getExecutionOptions() {
        return executionOptions;
    }
}
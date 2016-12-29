/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016 Payara Foundation and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Server;
import fish.payara.nucleus.notification.NotificationService;
import fish.payara.nucleus.notification.configuration.Notifier;
import fish.payara.nucleus.notification.configuration.NotifierConfigurationType;
import fish.payara.nucleus.notification.domain.NotificationEvent;
import fish.payara.nucleus.notification.domain.NotificationEventFactory;
import fish.payara.nucleus.notification.service.NotificationEventFactoryStore;
import fish.payara.nucleus.requesttracing.configuration.RequestTracingServiceConfiguration;
import fish.payara.nucleus.requesttracing.domain.EventType;
import fish.payara.nucleus.requesttracing.domain.RequestEvent;
import fish.payara.nucleus.requesttracing.domain.execoptions.NotifierExecutionOptions;
import fish.payara.nucleus.requesttracing.domain.execoptions.NotifierExecutionOptionsFactoryStore;
import fish.payara.nucleus.requesttracing.domain.execoptions.RequestTracingExecutionOptions;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.EventTypes;
import org.glassfish.api.event.Events;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.ConfigView;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * @author mertcaliskan
 *         Main service class that provides methods used by interceptors for tracing requests.
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
    NotificationEventFactoryStore eventFactoryStore;

    @Inject
    NotifierExecutionOptionsFactoryStore execOptionsFactoryStore;

    private RequestTracingExecutionOptions executionOptions = new RequestTracingExecutionOptions();

    @PostConstruct
    void postConstruct() {
        events.register(this);
    }

    @Override
    public void event(Event event) {
        if (event.is(EventTypes.SERVER_READY)) {
            bootstrapRequestTracingService();
        }
    }

    private void bootstrapRequestTracingService() {
        if (configuration != null) {
            executionOptions.setEnabled(Boolean.parseBoolean(configuration.getEnabled()));
            executionOptions.setThresholdValue(Long.parseLong(configuration.getThresholdValue()));
            executionOptions.setThresholdUnit(TimeUnit.valueOf(configuration.getThresholdUnit()));

            for (Notifier notifier : configuration.getNotifierList()) {
                ConfigView view = ConfigSupport.getImpl(notifier);
                NotifierConfigurationType annotation = view.getProxyType().getAnnotation(NotifierConfigurationType.class);
                executionOptions.addNotifierExecutionOption(execOptionsFactoryStore.get(annotation.type()).build(notifier));
            }
        }

        if (executionOptions != null && executionOptions.isEnabled()) {
            logger.info("Payara Request Tracing Service Started with configuration: " + executionOptions);
        }
    }

    /**
     * Retrieves the current Conversation ID
     *
     * @return
     */
    public UUID getConversationID() {
        return requestEventStore.getConversationID();
    }

    /**
     * Reset the conversation ID
     * This is especially useful for trace propagation across threads when
     * the event tracer can receive the conversation ID propagated to it
     *
     * @param newID
     */
    public void setConversationID(UUID newID) {
        requestEventStore.setConverstationID(newID);
    }

    public boolean isTraceInProgress() {
        return requestEventStore.isTraceInProgress();
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
        Long thresholdValueInNanos = getThresholdValueInNanos();

        long elapsedTime = requestEventStore.getElapsedTime();
        long elapsedTimeInNanos = TimeUnit.NANOSECONDS.convert(elapsedTime, TimeUnit.MILLISECONDS);
        if (elapsedTimeInNanos - thresholdValueInNanos > 0) {
            for (NotifierExecutionOptions notifierExecutionOptions : getExecutionOptions().getNotifierExecutionOptionsList().values()) {
                if (notifierExecutionOptions.isEnabled()) {
                    NotificationEventFactory notificationEventFactory =
                            eventFactoryStore.get(notifierExecutionOptions.getNotifierType());
                    NotificationEvent notificationEvent = notificationEventFactory.
                            buildNotificationEvent(elapsedTime, requestEventStore.getTraceAsString());
                    notificationService.notify(notificationEvent);
                }
            }
        }
        requestEventStore.flushStore();
    }

    public Long getThresholdValueInNanos() {
        if (getExecutionOptions() != null) {
            return TimeUnit.NANOSECONDS.convert(getExecutionOptions().getThresholdValue(),
                    getExecutionOptions().getThresholdUnit());
        }
        return null;
    }

    public boolean isRequestTracingEnabled() {
        return getExecutionOptions() != null && getExecutionOptions().isEnabled();
    }

    public RequestTracingExecutionOptions getExecutionOptions() {
        return executionOptions;
    }
}
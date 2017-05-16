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

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Server;
import fish.payara.nucleus.notification.NotificationService;
import fish.payara.nucleus.notification.configuration.Notifier;
import fish.payara.nucleus.notification.configuration.NotifierConfigurationType;
import fish.payara.nucleus.notification.domain.*;
import fish.payara.nucleus.notification.log.LogNotifier;
import fish.payara.nucleus.notification.log.LogNotifierExecutionOptions;
import fish.payara.nucleus.notification.service.NotificationEventFactoryStore;
import fish.payara.nucleus.requesttracing.configuration.RequestTracingServiceConfiguration;
import fish.payara.nucleus.requesttracing.domain.EventType;
import fish.payara.nucleus.requesttracing.domain.RequestEvent;
import fish.payara.nucleus.requesttracing.domain.execoptions.RequestTracingExecutionOptions;
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
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main service class that provides methods used by interceptors for tracing requests.
 *
 * @author mertcaliskan
 */
@Service(name = "requesttracing-service")
@RunLevel(StartupRunLevel.VAL)
public class RequestTracingService implements EventListener, ConfigListener {

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
    ServerEnvironment env;

    @Inject
    Transactions transactions;

    @Inject
    private ServiceLocator habitat;

    @Inject
    NotificationService notificationService;

    @Inject
    RequestEventStore requestEventStore;

    @Inject
    HistoricRequestEventStore historicRequestEventStore;

    @Inject
    NotificationEventFactoryStore eventFactoryStore;

    @Inject
    private NotifierExecutionOptionsFactoryStore executionOptionsFactoryStore;

    private RequestTracingExecutionOptions executionOptions = new RequestTracingExecutionOptions();

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
            }
            catch (TransactionFailure e) {
                logger.log(Level.SEVERE, "Error occurred while setting initial log notifier", e);
            }
        }
    }

    @Override
    public void event(Event event) {
        if (event.is(EventTypes.SERVER_READY)) {
            bootstrapRequestTracingService();
        }
        transactions.addListenerForType(RequestTracingServiceConfiguration.class, this);
    }

    public void bootstrapRequestTracingService() {
        if (configuration != null) {
            executionOptions.setEnabled(Boolean.parseBoolean(configuration.getEnabled()));
            executionOptions.setThresholdUnit(TimeUnit.valueOf(configuration.getThresholdUnit()));
            executionOptions.setThresholdValue(Long.parseLong(configuration.getThresholdValue()));
            executionOptions.setHistoricalTraceEnabled(Boolean.parseBoolean(configuration.getHistoricalTraceEnabled()));
            executionOptions.setHistoricalTraceStoreSize(Integer.parseInt(configuration.getHistoricalTraceStoreSize()));

            bootstrapNotifierList();
        }

        if (executionOptions != null && executionOptions.isEnabled()) {
            if (executionOptions.isHistoricalTraceEnabled()) {
                historicRequestEventStore.initialize(executionOptions.getHistoricalTraceStoreSize());
            }

            logger.info("Payara Request Tracing Service Started with configuration: " + executionOptions);
        }
    }

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
            String traceAsString = requestEventStore.getTraceAsString();

            if (executionOptions.isHistoricalTraceEnabled()) {
                historicRequestEventStore.addTrace(elapsedTime, traceAsString);
            }

            for (NotifierExecutionOptions notifierExecutionOptions : getExecutionOptions().getNotifierExecutionOptionsList().values()) {
                if (notifierExecutionOptions.isEnabled()) {
                    NotificationEventFactory notificationEventFactory = eventFactoryStore.get(notifierExecutionOptions.getNotifierType());
                    String subject = "Request execution time: " + elapsedTime + "(ms) exceeded the acceptable threshold";
                    NotificationEvent notificationEvent = notificationEventFactory.buildNotificationEvent(subject, traceAsString);
                    notificationService.notify(EventSource.REQUESTTRACING, notificationEvent);
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

    @Override
    public UnprocessedChangeEvents changed(PropertyChangeEvent[] events) {
        boolean isCurrentInstanceMatchTarget = false;
        if (env.isInstance()) {
            isCurrentInstanceMatchTarget = true;
        }
        else {
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

                    if(changedType.equals(RequestTracingServiceConfiguration.class)) {
                        configuration = (RequestTracingServiceConfiguration) changedInstance;
                    }
                    return null;
                }
            }, logger);
        }
        return null;
    }
}
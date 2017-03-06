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
package fish.payara.nucleus.notification;

import com.sun.enterprise.config.serverbeans.Config;
import fish.payara.nucleus.notification.configuration.NotificationServiceConfiguration;
import fish.payara.nucleus.notification.configuration.NotifierConfiguration;
import fish.payara.nucleus.notification.configuration.NotifierConfigurationType;
import fish.payara.nucleus.notification.configuration.NotifierType;
import fish.payara.nucleus.notification.domain.EventSource;
import fish.payara.nucleus.notification.domain.NotificationEvent;
import fish.payara.nucleus.notification.domain.NotificationExecutionOptions;
import fish.payara.nucleus.notification.log.LogNotifier;
import fish.payara.nucleus.notification.log.LogNotifierConfiguration;
import fish.payara.nucleus.notification.service.NotifierConfigurationExecutionOptionsFactoryStore;
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
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main service class that provides {@link #notify(NotificationEvent)} method used by services, which needs disseminating notifications.
 *
 * @author mertcaliskan
 */
@Service(name = "notification-service")
@RunLevel(StartupRunLevel.VAL)
public class NotificationService implements EventListener, ConfigListener {

    private static final Logger logger = Logger.getLogger(NotificationService.class.getCanonicalName());

    @Inject
    @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    @Optional
    NotificationServiceConfiguration configuration;

    @Inject
    private Events events;

    @Inject
    ServiceLocator habitat;

    @Inject
    Transactions transactions;

    @Inject
    private NotificationEventBus notificationEventBus;

    @Inject
    private NotifierConfigurationExecutionOptionsFactoryStore factoryStore;

    @Inject
    ServerEnvironment env;

    private NotificationExecutionOptions executionOptions;

    @PostConstruct
    void postConstruct() {
        events.register(this);
        configuration = habitat.getService(NotificationServiceConfiguration.class);

        if (configuration != null && configuration.getNotifierConfigurationList() != null && configuration.getNotifierConfigurationList().isEmpty()) {
            try {
                ConfigSupport.apply(new SingleConfigCode<NotificationServiceConfiguration>() {
                    @Override
                    public Object run(final NotificationServiceConfiguration configurationProxy)
                            throws PropertyVetoException, TransactionFailure {
                        LogNotifierConfiguration notifierConfiguration = configurationProxy.createChild(LogNotifierConfiguration.class);
                        configurationProxy.getNotifierConfigurationList().add(notifierConfiguration);
                        return configurationProxy;
                    }
                }, configuration);
            }
            catch (TransactionFailure e) {
                logger.log(Level.SEVERE, "Error occurred while setting initial log notifier configuration", e);
            }
        }
    }

    public void event(Event event) {
        if (event.is(EventTypes.SERVER_READY)) {
            bootstrapNotificationService();
        }
        transactions.addListenerForType(NotificationServiceConfiguration.class, this);
    }

    public void bootstrapNotificationService() {
        if (configuration != null) {
            executionOptions = new NotificationExecutionOptions();
            executionOptions.setEnabled(Boolean.parseBoolean(configuration.getEnabled()));

            for (NotifierConfiguration notifierConfiguration : configuration.getNotifierConfigurationList()) {
                NotifierType type = null;
                try {
                    ConfigView view = ConfigSupport.getImpl(notifierConfiguration);
                    NotifierConfigurationType annotation = view.getProxyType().getAnnotation(NotifierConfigurationType.class);
                    type = annotation.type();
                    executionOptions.addNotifierConfigurationExecutionOption(factoryStore.get(type).build(notifierConfiguration));
                } catch (UnsupportedEncodingException e) {
                    logger.log(Level.SEVERE, "Notifier configuration with type " + type
                            + " cannot be configured due to encoding problems in configuration parameters", e);
                }
            }
            if (executionOptions.isEnabled()) {
                logger.info("Payara Notification Service bootstrapped with configuration: " + executionOptions);
            }
        }
    }

    public void notify(EventSource source, NotificationEvent notificationEvent) {
        notificationEvent.setEventType(source.getValue());
        notificationEventBus.postEvent(notificationEvent);
    }

    public NotificationExecutionOptions getExecutionOptions() {
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

                    if(changedType.equals(NotificationServiceConfiguration.class)) {
                        configuration = (NotificationServiceConfiguration) changedInstance;
                    }
                    return null;
                }
            }, logger);
        }
        return null;
    }
}
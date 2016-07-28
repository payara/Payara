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

import fish.payara.nucleus.notification.configuration.NotificationServiceConfiguration;
import fish.payara.nucleus.notification.configuration.NotifierConfiguration;
import fish.payara.nucleus.notification.configuration.NotifierType;
import fish.payara.nucleus.notification.domain.NotificationEvent;
import fish.payara.nucleus.notification.domain.execoptions.NotificationExecutionOptions;
import fish.payara.nucleus.notification.domain.execoptions.NotifierConfigurationExecutionOptions;
import fish.payara.nucleus.notification.domain.execoptions.NotifierConfigurationExecutionOptionsFactory;
import fish.payara.nucleus.notification.service.LogNotifierService;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.EventTypes;
import org.glassfish.api.event.Events;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.logging.Logger;

/**
 * @author mertcaliskan
 *
 * Main service class that provides {@link #notify(NotificationEvent)} method used by services, which needs disseminating notifications.
 */
@Service(name = "notification-service")
@RunLevel(StartupRunLevel.VAL)
public class NotificationService implements EventListener {

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
    private NotificationEventBus notificationEventBus;

    @Inject
    private NotifierConfigurationExecutionOptionsFactory executionOptionsFactory;

    private NotificationExecutionOptions executionOptions = new NotificationExecutionOptions();

    @PostConstruct
    void postConstruct() {
        if (configuration != null) {
            executionOptions.setEnabled(Boolean.parseBoolean(configuration.getEnabled()));
            for (NotifierConfiguration notifierConfiguration : configuration.getNotifierConfigurationList()) {
                executionOptions.addNotifierConfigurationExecutionOption(executionOptionsFactory.build(notifierConfiguration));
            }
        }
        events.register(this);
    }

    public void event(Event event) {
        if (event.is(EventTypes.SERVER_READY)) {
            bootstrapNotificationService();
        }
    }

    private void bootstrapNotificationService() {
        if (executionOptions.isEnabled()) {
            logger.info("Payara Notification Service Started with configuration: " + executionOptions);
        }
    }

    public void notify(NotificationEvent notificationEvent) {
        notificationEventBus.postEvent(notificationEvent);
    }

    public NotificationExecutionOptions getExecutionOptions() {
        return executionOptions;
    }
}
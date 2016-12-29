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
package fish.payara.nucleus.notification.service.log;

import com.google.common.eventbus.Subscribe;
import fish.payara.nucleus.notification.configuration.LogNotifier;
import fish.payara.nucleus.notification.configuration.LogNotifierConfiguration;
import fish.payara.nucleus.notification.configuration.NotifierType;
import fish.payara.nucleus.notification.domain.LogNotificationEvent;
import fish.payara.nucleus.notification.service.BaseNotifierService;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.event.EventTypes;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import java.text.MessageFormat;
import java.util.logging.Logger;

/**
 * @author mertcaliskan
 */
@Service(name = "service-log")
@RunLevel(StartupRunLevel.VAL)
public class LogNotifierService extends BaseNotifierService<LogNotificationEvent, LogNotifier, LogNotifierConfiguration> {

    private Logger logger = Logger.getLogger(LogNotifierService.class.getCanonicalName());

    public void event(Event event) {
        if (event.is(EventTypes.SERVER_READY)) {
            register(NotifierType.LOG, LogNotifier.class, LogNotifierConfiguration.class, this);
        }
    }

    @Override
    @Subscribe
    public void handleNotification(LogNotificationEvent event) {
        if (event.getParameters() != null && event.getParameters().length > 0) {
            String formattedText = MessageFormat.format(event.getMessage(), event.getParameters());
            logger.log(event.getLevel(), event.getUserMessage() != null ?
                    event.getUserMessage() + " - " + formattedText : formattedText);
        } else {
            logger.log(event.getLevel(), event.getUserMessage() != null ?
                    event.getUserMessage() + " - " + event.getMessage() : event.getMessage());
        }
    }
}
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

import fish.payara.nucleus.notification.configuration.NotifierType;
import fish.payara.nucleus.notification.domain.LogNotificationEvent;
import fish.payara.nucleus.notification.domain.NotificationEvent;
import fish.payara.nucleus.requesttracing.domain.RequestEvent;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.logging.Level;


/**
 * @author mertcaliskan
 *
 * Factory class that translates stored request events to notifiation events based on specified {@link NotifierType}.
 */
@Service
@Singleton
class RequestTracingNotificationEventFactory {

    @Inject
    private RequestEventStore store;

    NotificationEvent build(long elapsedTime, NotifierType notifierType) {
        if (NotifierType.LOG.equals(notifierType)) {
            LogNotificationEvent event = new LogNotificationEvent();
            event.setUserMessage("Request execution time: " + elapsedTime + "(ms) exceeded the acceptable threshold");
            event.setLevel(Level.INFO);
            event.setMessage(getRequestEventsAsStr());
            return event;
        }
        return null;
    }

    private String getRequestEventsAsStr() {
        return store.getTraceAsString();
    }
}
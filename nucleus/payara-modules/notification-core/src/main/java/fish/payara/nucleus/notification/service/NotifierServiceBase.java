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
package fish.payara.nucleus.notification.service;


import fish.payara.nucleus.notification.NotificationEventBus;
import fish.payara.nucleus.notification.NotificationService;
import fish.payara.nucleus.notification.configuration.NotifierType;
import fish.payara.nucleus.notification.domain.NotificationEvent;
import org.jvnet.hk2.annotations.Contract;

import javax.inject.Inject;

/**
 * @author mertcaliskan
 *
 * Base contract for all notifier services.
 */
@Contract
public abstract class NotifierServiceBase<E extends NotificationEvent> {

    @Inject
    private NotificationEventBus eventBus;

    @Inject
    private NotificationService notificationService;

    void register(NotifierType type, NotifierServiceBase service) {
        if (notificationService.getExecutionOptions().isNotifierServiceEnabled(type)){
            eventBus.register(service);
        }
    }

    public abstract void handleNotification(E event);
}

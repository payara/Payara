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

import com.google.common.eventbus.EventBus;
import fish.payara.nucleus.notification.domain.NotificationEvent;
import fish.payara.nucleus.notification.service.BaseNotifierService;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

/**
 * @author mertcaliskan
 *
 * Service class that holds an instance of guava pub-sub event bus where notifier services register themselves in order to be
 * notified according to a given notification event post.
 */
@Service(name = "notification-eventbus")
@RunLevel(StartupRunLevel.VAL)
public class NotificationEventBus {

    private EventBus eventBus = new EventBus();

    public void register(BaseNotifierService notifier) {
        eventBus.register(notifier);
    }

    public void unregister(BaseNotifierService notifier) {
        eventBus.unregister(notifier);
    }

    void postEvent(NotificationEvent event) {
        eventBus.post(event);
    }
}
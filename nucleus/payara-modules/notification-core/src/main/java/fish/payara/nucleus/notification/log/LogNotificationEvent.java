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
package fish.payara.nucleus.notification.log;

import fish.payara.nucleus.notification.domain.NotificationEvent;

import java.util.Arrays;
import java.util.logging.Level;

/**
 * @author mertcaliskan
 *
 * Stores log notifications with a defined severity level, a message and parameters to be formatted into the given message.
 */
public class LogNotificationEvent extends NotificationEvent {

    private Level level;
    private Object[] parameters;

    public Level getLevel() {
        return level;
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    public Object[] getParameters() {
        return parameters;
    }

    public void setParameters(Object[] parameters) {
        this.parameters = parameters;
    }

    @Override
    public String toString() {
        return "LogNotificationEvent{" +
                "level=" + level +
                ", message='" + getMessage() + '\'' +
                ", parameters=" + Arrays.toString(parameters) +
                '}';
    }
}

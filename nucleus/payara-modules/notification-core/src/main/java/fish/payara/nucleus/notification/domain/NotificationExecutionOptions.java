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
package fish.payara.nucleus.notification.domain;

import fish.payara.nucleus.notification.configuration.NotifierType;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author mertcaliskan
 *
 * Configuration class that holds enable/disable of notification services and refers to a list of notifier configurations.
 */
public class NotificationExecutionOptions {

    private boolean enabled;
    private Map<NotifierType, NotifierConfigurationExecutionOptions> notifierConfigurationExecutionOptionsList =
            new ConcurrentHashMap<>();

    public void addNotifierConfigurationExecutionOption(NotifierConfigurationExecutionOptions executionOptions) {
        notifierConfigurationExecutionOptionsList.put(executionOptions.getNotifierType(), executionOptions);
    }

    public void removeNotifierConfigurationExecutionOption(NotifierConfigurationExecutionOptions executionOptions) {
        notifierConfigurationExecutionOptionsList.remove(executionOptions.getNotifierType());
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Map<NotifierType, NotifierConfigurationExecutionOptions> getNotifierConfigurationExecutionOptionsList() {
        return notifierConfigurationExecutionOptionsList;
    }

    public boolean isNotifierServiceEnabled(NotifierType notifierType) {
        NotifierConfigurationExecutionOptions executionOptions = notifierConfigurationExecutionOptionsList.get(notifierType);
        return isNotifierServiceDefined(notifierType) && executionOptions.isEnabled();
    }

    public boolean isNotifierServiceDefined(NotifierType notifierType) {
        NotifierConfigurationExecutionOptions executionOptions = notifierConfigurationExecutionOptionsList.get(notifierType);
        return executionOptions != null;
    }


    @Override
    public String toString() {
        return "NotificationExecutionOptions{" +
                "enabled=" + enabled +
                ", notifierConfigurationExecutionOptionsList=" + notifierConfigurationExecutionOptionsList +
                '}';
    }
}

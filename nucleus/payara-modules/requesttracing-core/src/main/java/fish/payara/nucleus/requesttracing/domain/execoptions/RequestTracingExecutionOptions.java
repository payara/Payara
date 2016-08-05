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
package fish.payara.nucleus.requesttracing.domain.execoptions;

import fish.payara.nucleus.notification.configuration.NotifierType;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author mertcaliskan
 *
 * Configuration class that holds enable/disable value of request tracing, the threshold value with its timeunit that will
 * trigger request tracing mechainsm, and a list of notifier configurations.
 */
public class RequestTracingExecutionOptions {

    private boolean enabled;
    private Long thresholdValue;
    private TimeUnit thresholdUnit;
    private Map<NotifierType, NotifierExecutionOptions> notifierExecutionOptionsList = new HashMap<NotifierType, NotifierExecutionOptions>();

    public void addNotifierExecutionOption(NotifierExecutionOptions notifierExecutionOptions) {
        getNotifierExecutionOptionsList().put(notifierExecutionOptions.getNotifierType(), notifierExecutionOptions);
    }
    public void removeNotifierExecutionOption(NotifierExecutionOptions notifierExecutionOptions) {
        getNotifierExecutionOptionsList().remove(notifierExecutionOptions.getNotifierType());
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Long getThresholdValue() {
        return thresholdValue;
    }

    public void setThresholdValue(Long thresholdValue) {
        this.thresholdValue = thresholdValue;
    }

    public TimeUnit getThresholdUnit() {
        return thresholdUnit;
    }

    public void setThresholdUnit(TimeUnit thresholdUnit) {
        this.thresholdUnit = thresholdUnit;
    }

    public Map<NotifierType, NotifierExecutionOptions> getNotifierExecutionOptionsList() {
        return notifierExecutionOptionsList;
    }

    @Override
    public String toString() {
        return "RequestTracingExecutionOptions{" +
                "enabled=" + enabled +
                ", thresholdValue=" + thresholdValue +
                ", thresholdUnit=" + thresholdUnit +
                ", notifierExecutionOptionsList=" + notifierExecutionOptionsList +
                '}';
    }
}

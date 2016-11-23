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

/**
 * Base class for all notifier execution options.
 *
 * @author mertcaliskan
 */
public abstract class NotifierExecutionOptions {

    private NotifierType notifierType;
    private boolean enabled;

    protected NotifierExecutionOptions(NotifierType notifierType) {
        this.notifierType = notifierType;
    }

    public NotifierType getNotifierType() {
        return notifierType;
    }

    public void setNotifierType(NotifierType notifierType) {
        this.notifierType = notifierType;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String toString() {
        return "NotificationExecutionOptions{" +
                "notifierType=" + notifierType +
                ", enabled=" + enabled +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NotifierExecutionOptions that = (NotifierExecutionOptions) o;

        return notifierType == that.notifierType;

    }

    @Override
    public int hashCode() {
        return notifierType != null ? notifierType.hashCode() : 0;
    }
}

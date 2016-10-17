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
package fish.payara.nucleus.notification.configuration;

import org.glassfish.api.admin.config.ConfigExtension;
import org.jvnet.hk2.config.*;

import java.beans.PropertyVetoException;
import java.util.List;

/**
 * @author mertcaliskan
 *
 */
@Configured
public interface NotificationServiceConfiguration extends ConfigBeanProxy, ConfigExtension {

    @Attribute(defaultValue = "false", dataType = Boolean.class)
    String getEnabled();
    void enabled(String value) throws PropertyVetoException;

    @Element("*")
    List<NotifierConfiguration> getNotifierConfigurationList();

    @DuckTyped
    <T extends NotifierConfiguration> T getNotifierConfigurationByType(Class type);

    class Duck {
        public static <T extends NotifierConfiguration> T getNotifierConfigurationByType(NotificationServiceConfiguration config, Class<T> type) {
            for (NotifierConfiguration notifierConfiguration : config.getNotifierConfigurationList()) {
                try {
                    return type.cast(notifierConfiguration);
                } catch (Exception e) {
                    // ignore, not the right type.
                }
            }
            return null;
        }
    }
}

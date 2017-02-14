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
package fish.payara.nucleus.healthcheck.configuration;

import fish.payara.nucleus.notification.configuration.Notifier;
import org.glassfish.api.admin.config.ConfigExtension;
import org.jvnet.hk2.config.*;

import javax.validation.constraints.Min;
import java.beans.PropertyVetoException;
import java.util.List;

/**
 * @author mertcaliskan
 *
 */
@Configured
public interface HealthCheckServiceConfiguration extends ConfigBeanProxy, ConfigExtension {

    @Attribute(defaultValue="false",dataType=Boolean.class)
    String getEnabled();
    void enabled(String value) throws PropertyVetoException;

    @Attribute(defaultValue = "false", dataType = Boolean.class)
    String getHistoricalTraceEnabled();
    void setHistoricalTraceEnabled(String value) throws PropertyVetoException;

    @Attribute(defaultValue = "20", dataType = Integer.class)
    @Min(value = 0)
    String getHistoricalTraceStoreSize();
    void setHistoricalTraceStoreSize(String value) throws PropertyVetoException;

    @Element("*")
    List<Checker> getCheckerList();

    @DuckTyped
    <T extends Checker> T getCheckerByType(Class<T> type);

    @Element("*")
    List<Notifier> getNotifierList();

    @DuckTyped
    <T extends Notifier> T getNotifierByType(Class type);

    class Duck {
        public static <T extends Checker> T getCheckerByType(HealthCheckServiceConfiguration config, Class<T> type) {
            for (Checker checker : config.getCheckerList()) {
                try {
                    return type.cast(checker);
                } catch (Exception e) {
                    // ignore, not the right type.
                }
            }
            return null;
        }

        public static <T extends Notifier> T getNotifierByType(HealthCheckServiceConfiguration config, Class<T> type) {
            for (Notifier notifier : config.getNotifierList()) {
                try {
                    return type.cast(notifier);
                } catch (Exception e) {
                    // ignore, not the right type.
                }
            }
            return null;
        }

    }
}
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
package fish.payara.jmx.monitoring.configuration;

import fish.payara.jmx.monitoring.MonitoringJob;
import java.beans.PropertyVetoException;
import java.util.List;
import org.glassfish.api.admin.config.ConfigExtension;
import org.jvnet.hk2.config.*;

/**
 *
 * @author savage
 */
@Configured
public interface JMXMonitoringServiceConfiguration extends ConfigBeanProxy, ConfigExtension {

    @Attribute(defaultValue="false",dataType=Boolean.class)
    Boolean getEnabled();
    void enabled(String value) throws PropertyVetoException;
   
    @Attribute(defaultValue="log",dataType=String.class)
    String getLogType();
    void logtype(String value) throws PropertyVetoException;

    @Attribute(defaultValue="localhost",dataType=String.class)
    String getHost();
    void host(String value) throws PropertyVetoException;

    @Attribute(defaultValue="1099",dataType=String.class)
    String getPort();
    void port(String value) throws PropertyVetoException;

    @Attribute(defaultValue="seconds",dataType=Long.class)
    Long getLogFrequency();
    void logfrequency(String value) throws PropertyVetoException;

    @Element("*")
    List<MonitoringJob> getMonitoringJobList();

}

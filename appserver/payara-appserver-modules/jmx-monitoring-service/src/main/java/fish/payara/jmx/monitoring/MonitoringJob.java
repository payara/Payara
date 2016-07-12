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
package fish.payara.jmx.monitoring;

import fish.payara.jmx.monitoring.configuration.MonitoringJobConfiguration;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.inject.Named;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import org.glassfish.api.admin.ServerEnvironment;
import org.jvnet.hk2.annotations.Optional;

/**
 *
 * @author savage
 */
public class MonitoringJob {

    @Inject
    @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    @Optional
    MonitoringJobConfiguration configuration;

    private final ObjectName mBean;
    private final List<MonitoringAttribute> attributes;
    
    private boolean enabled;

    public MonitoringJob(ObjectName mbean, List<MonitoringAttribute> attributes) throws MalformedObjectNameException {
        this.mBean = new ObjectName(configuration.getMBean());
        this.enabled = configuration.getEnabled(); 
        this.attributes = configuration.getMonitoringAttributeList();
    }

    public String getMonitoringInfo(MBeanServerConnection connection) {
        StringBuilder monitoringString = new StringBuilder();
        monitoringString.append(mBean.getDomain());

        for (MonitoringAttribute attribute : attributes) {
            if (attribute.getEnabled()) {
                try {
                    String value = getAttributeValue(attribute, connection);
                    monitoringString.append(value);
                } catch (MBeanException | AttributeNotFoundException | InstanceNotFoundException | ReflectionException | IOException ex) {
                    Logger.getLogger(MonitoringJob.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        return monitoringString.toString();
    }

    public boolean getEnabled() {
        return enabled;
    }

    public ObjectName getMBean() {
        return mBean;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    private String getAttributeValue(MonitoringAttribute attribute,
                                MBeanServerConnection connection) throws 
                                MBeanException, AttributeNotFoundException, 
                                InstanceNotFoundException, ReflectionException, 
                                IOException {
        String name = attribute.getAttributeName();
        return connection.getAttribute(mBean, name).toString();
    }
}

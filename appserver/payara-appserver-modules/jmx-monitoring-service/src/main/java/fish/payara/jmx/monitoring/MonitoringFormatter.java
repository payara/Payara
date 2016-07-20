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

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

/**
 *
 * @author savage
 */
public class MonitoringFormatter implements Runnable {

    private final List<MonitoringJob> jobs;
    
    private String serviceURL;
    private MBeanServerConnection connection;

    public MonitoringFormatter(String serviceURLString, List<MonitoringJob> jobs) {
        this.serviceURL = serviceURLString;
        this.jobs = jobs;
    }
    
    @Override
    public void run() {

        if (null == connection) {
            try {
                createNewConnection();
            } catch (IOException ex) {
                Logger.getLogger(MonitoringFormatter.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        StringBuilder monitoringString = new StringBuilder();

        for (MonitoringJob job : jobs) {
                monitoringString.append(job.getMonitoringInfo(connection));
        }
        // Replace this with the relevant formatting when ready
        System.out.println(monitoringString.toString());
    }

    public void createNewConnection() throws IOException {
        JMXServiceURL serviceUrl = new JMXServiceURL(serviceURL);
        JMXConnector jmxConnector = JMXConnectorFactory.connect(serviceUrl, null);
        this.connection = jmxConnector.getMBeanServerConnection();
    }
    
    public void setServiceURL(String value) {
        this.serviceURL = value;
    }
}

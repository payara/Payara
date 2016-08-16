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
package fish.payara.jmx.monitoring;

import java.util.List;
import java.util.logging.Logger;
import javax.management.MBeanServer;

/**
 * The runnable class which gathers monitoring info from a list of MonitoringJob objects and builds a log string from it.
 *
 * @author savage
 */
public class MonitoringFormatter implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(MonitoringFormatter.class.getCanonicalName());
    private final String LOGMESSAGE_PREFIX = "PAYARA-MONITORING: ";

    private final MBeanServer mBeanServer;
    private final List<MonitoringJob> jobs;

    /**
     * Constructor for the MonitoringFormatter class.
     * 
     * @param mBeanServer The MBeanServer to monitor.
     * @param jobs List of monitoring jobs to perform.
     */
    public MonitoringFormatter(MBeanServer mBeanServer,List<MonitoringJob> jobs) {
        this.mBeanServer = mBeanServer;
        this.jobs = jobs;
    }
   
    /**
     * Class runnable method.
     *  Calls getMonitoringInfo on all MonitoringJobs passing the MBeanServer.
     *  Uses the results to build a String for the log message.
     */
    @Override
    public void run() {
        StringBuilder monitoringString = new StringBuilder();

        monitoringString.append(LOGMESSAGE_PREFIX);

        for (MonitoringJob job : jobs) {
                monitoringString.append(job.getMonitoringInfo(mBeanServer));
        }
        
        LOGGER.info(monitoringString.toString());
    }

}

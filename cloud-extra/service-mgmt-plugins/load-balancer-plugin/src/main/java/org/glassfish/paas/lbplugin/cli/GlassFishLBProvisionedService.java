/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package org.glassfish.paas.lbplugin.cli;

import org.glassfish.paas.orchestrator.service.HTTPLoadBalancerServiceType;
import org.glassfish.paas.orchestrator.service.ServiceStatus;
import org.glassfish.paas.orchestrator.service.ServiceType;
import org.glassfish.paas.orchestrator.service.metadata.ServiceDescription;
import org.glassfish.paas.orchestrator.service.spi.ProvisionedService;
import org.glassfish.paas.orchestrator.service.spi.Service;
import org.glassfish.paas.orchestrator.service.spi.ServiceLogRecord;
import org.glassfish.paas.orchestrator.service.spi.ServiceLogType;
import org.glassfish.paas.orchestrator.service.spi.ServiceLogRecordBuilder;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.logging.Level;


/**
 * @author Jagadish Ramu
 */
public class GlassFishLBProvisionedService implements ProvisionedService {

    private ServiceDescription serviceDescription;
    private Properties serviceProperties;
    private ServiceStatus status;

    public GlassFishLBProvisionedService(ServiceDescription serviceDescription,
                                         Properties serviceProperties) {
        this.serviceDescription = serviceDescription;
        this.serviceProperties = serviceProperties;
    }

    public ServiceType getServiceType() {
        return new HTTPLoadBalancerServiceType();
    }

    public ServiceDescription getServiceDescription() {
        return serviceDescription;
    }

    public Properties getServiceProperties() {
        return serviceProperties;
    }

    public ServiceStatus getStatus() {
        return status;
    }

    public void setStatus(ServiceStatus status) {
        this.status = status;
    }

    public String getName(){
        return serviceDescription.getName();
    }

    public Set<Service> getChildServices() {
        return new HashSet<Service>();
    }

    public Properties getProperties() {
        //TODO return the co-ordinates (eg: hostname, port, etc.,)
        return new Properties();
    }

    public Map<Service, List<ServiceLogRecord>> collectLogs(ServiceLogType type, Level level, Date since) {

        throw new UnsupportedOperationException("Not yet implemented");

        /*Map<Service, List<ServiceLogRecord>> serviceListMap = new HashMap<Service, List<ServiceLogRecord>>();
        ServiceLogRecordBuilder serviceLogRecordBuilder = new ServiceLogRecordBuilder();
        serviceLogRecordBuilder.setLogFile(new File("/home/naman/Desktop/error_log"));
        serviceLogRecordBuilder.setStartSequence("[");
        serviceLogRecordBuilder.setEndSequence("]");
        serviceLogRecordBuilder.setLevel(Level.INFO);
        serviceLogRecordBuilder.setDateFormatter("EEE MMM d HH:mm:ss yyyy");
        serviceLogRecordBuilder.setLB(true);
        String[] myPara = {serviceLogRecordBuilder.DATETIME, serviceLogRecordBuilder.LEVEL, serviceLogRecordBuilder.MESSAGE};
        serviceLogRecordBuilder.setParameters(myPara);

        try {
            List<ServiceLogRecord> records = serviceLogRecordBuilder.build();
            serviceListMap.put(this,records);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.UnsupportedOperationException("Not yet implemented");
        }

        return  serviceListMap;*/

    }

    public Map<Service, List<ServiceLogRecord>> collectLogs(ServiceLogType type, Level level, long count) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public Set<ServiceLogType> getLogTypes() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public ServiceLogType getDefaultLogType() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

}

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

package org.glassfish.paas.gfplugin;

import org.glassfish.embeddable.GlassFish;
import org.glassfish.paas.orchestrator.service.ServiceStatus;
import org.glassfish.paas.orchestrator.service.metadata.ServiceDescription;
import org.glassfish.paas.orchestrator.service.spi.Service;
import org.glassfish.paas.orchestrator.service.spi.ServiceLogRecord;
import org.glassfish.paas.orchestrator.service.spi.ServiceLogRecordBuilder;
import org.glassfish.paas.orchestrator.service.spi.ServiceLogType;
import org.glassfish.paas.spe.common.BasicProvisionedService;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.logging.Level;

/**
 * @author bhavanishankar@java.net
 */

public class GlassFishProvisionedService extends BasicProvisionedService {

    // TODO :: should contain DAS information and Cluster information.
    
    private GlassFish provisionedGlassFish;

    public GlassFishProvisionedService(ServiceDescription serviceDesription,
                                       Properties serviceProperties,
                                       ServiceStatus status,
                                       GlassFish provisionedGlassFish) {
        super(serviceDesription, serviceProperties, status);
        this.provisionedGlassFish = provisionedGlassFish;
    }

/*
    @Override
    public ServiceStatus getStatus() {
        // TODO :: make sure the cluster is running.
        return ServiceStatus.RUNNING;
    }
*/

    public GlassFish getProvisionedGlassFish() {
        return provisionedGlassFish;
    }

    public void setProvisionedGlassFish(GlassFish glassFish) {
        this.provisionedGlassFish = glassFish;
    }

    // Map GlassFish status to Service status.
    private static Map<GlassFish.Status, ServiceStatus> statusMapping = new HashMap();

    static {
        statusMapping.put(GlassFish.Status.STARTING, ServiceStatus.STARTING);
        statusMapping.put(GlassFish.Status.STARTED, ServiceStatus.STARTED);
        statusMapping.put(GlassFish.Status.STOPPING, ServiceStatus.STOPPING);
        statusMapping.put(GlassFish.Status.STOPPED, ServiceStatus.STOPPED);
        statusMapping.put(GlassFish.Status.INIT, ServiceStatus.STOPPED);
        statusMapping.put(GlassFish.Status.DISPOSED, ServiceStatus.UNKNOWN);
     }

    /*@Override
    public Map<org.glassfish.paas.orchestrator.service.spi.Service, List<ServiceLogRecord>> collectLogs(ServiceLogType type, Level level, Date since) {

        Map<Service, List<ServiceLogRecord>> serviceListMap = new HashMap<Service, List<ServiceLogRecord>>();
        File logFile = new File("/home/naman/Desktop/server.log");

        ServiceLogRecordBuilder serviceLogRecordBuilder = new ServiceLogRecordBuilder();
        serviceLogRecordBuilder.setLogFile(logFile);
        serviceLogRecordBuilder.setStartSequence("[#|");
        serviceLogRecordBuilder.setEndSequence("|#]");
        serviceLogRecordBuilder.setDelimiter("|");
        serviceLogRecordBuilder.setLevel(Level.INFO);
        serviceLogRecordBuilder.setDateFormatter("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

        String[] myPara = {serviceLogRecordBuilder.DATETIME, serviceLogRecordBuilder.LEVEL, serviceLogRecordBuilder.OTHER,
                serviceLogRecordBuilder.LOGGERNAME, serviceLogRecordBuilder.OTHER, serviceLogRecordBuilder.MESSAGE};

        serviceLogRecordBuilder.setParameters(myPara);

        try {
            List<ServiceLogRecord> records = serviceLogRecordBuilder.build();
            serviceListMap.put(this,records);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        return  serviceListMap;
    }*/
}

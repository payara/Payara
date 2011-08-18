/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.paas.orchestrator.service.JavaEEServiceType;
import org.glassfish.paas.orchestrator.service.ServiceStatus;
import org.glassfish.paas.orchestrator.service.ServiceType;
import org.glassfish.paas.orchestrator.service.metadata.ServiceDescription;
import org.glassfish.paas.orchestrator.service.spi.ProvisionedService;
import org.glassfish.paas.orchestrator.service.spi.ServiceDefinition;
import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFishException;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author bhavanishankar@java.net
 */

public class GlassFishProvisionedService implements ProvisionedService {

    private ServiceDescription serviceDesription; // serviceDescription.getName() is like domain.cluster
    private Properties serviceProperties; // contains host, port, domainName.
    private GlassFish provisionedGlassFish;

    public GlassFishProvisionedService(ServiceDescription serviceDesription,
                                       Properties serviceProperties,
                                       GlassFish provisionedGlassFish) {
        this.serviceDesription = serviceDesription;
        this.serviceProperties = serviceProperties;
        this.provisionedGlassFish = provisionedGlassFish;
    }

    public ServiceType getServiceType() {
        return new JavaEEServiceType();
    }

    public ServiceDescription getServiceDescription() {
        return serviceDesription;
    }

    public String getName(){
        return serviceDesription.getName();
    }

    public Properties getProperties() {
        //TODO return the co-ordinates (eg: hostname, port, etc.,)
        return new Properties();
    }

    public ServiceStatus getStatus() {
        ServiceStatus status = ServiceStatus.UNKNOWN;
        try {
            status = provisionedGlassFish != null ?
                    statusMapping.get(provisionedGlassFish.getStatus()) : status;
        } catch (GlassFishException e) {
            e.printStackTrace();
        }
        return status;
    }

    public Properties getServiceProperties() {
        return serviceProperties;
    }

    public void setServiceDesription(ServiceDescription definition) {
        this.serviceDesription = definition;
    }

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

}

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
package org.glassfish.paas.javadbplugin;

import org.glassfish.api.deployment.ApplicationContainer;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.paas.orchestrator.provisioning.CloudRegistryEntry;
import org.glassfish.paas.orchestrator.provisioning.CloudRegistryService;
import org.glassfish.paas.orchestrator.provisioning.DatabaseProvisioner;
import org.glassfish.paas.orchestrator.provisioning.cli.ServiceUtil;
import org.glassfish.paas.orchestrator.service.RDBMSServiceType;
import org.glassfish.paas.orchestrator.service.ServiceReference;
import org.glassfish.paas.orchestrator.service.ServiceStatus;
import org.glassfish.paas.orchestrator.service.SimpleServiceDefinition;
import org.glassfish.paas.orchestrator.service.spi.Plugin;
import org.glassfish.paas.orchestrator.service.spi.ProvisionedService;
import org.glassfish.paas.orchestrator.service.spi.ServiceDefinition;
import org.glassfish.embeddable.CommandResult;
import org.glassfish.embeddable.CommandRunner;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * @author Jagadish Ramu
 */
@Scoped(PerLookup.class)
@Service
public class DerbyPlugin implements Plugin<RDBMSServiceType> {

    @Inject
    private CloudRegistryService registryService;

    @Inject
    private CommandRunner commandRunner;

    @Inject
    private ServiceUtil serviceUtil;

    private static final String DATASOURCE = "javax.sql.DataSource";

    public static final String GLASSFISH_DERBY = "GLASSFISH_DERBY";
    public static final String RDBMS_SERVICE_TYPE = "RDBMS";

    public RDBMSServiceType getServiceType() {
        return new RDBMSServiceType();
    }

    public boolean handles(ReadableArchive cloudArchive) {
        //For prototype, DB Plugin has no role here.
        return true;
    }

    public boolean isReferenceTypeSupported(String referenceType) {
        return DATASOURCE.equalsIgnoreCase(referenceType);
    }

    public Set<ServiceReference> getServiceReferences(ReadableArchive cloudArchive) {
        //DB plugin does not scan anything for prototype
        return new HashSet<ServiceReference>();
    }

    public ServiceDefinition getDefaultServiceDefinition(ServiceReference svcRef) {

        if (DATASOURCE.equals(svcRef.getServiceRefType())) {
            DatabaseProvisioner dbProvisioner = registryService.getDatabaseProvisioner(GLASSFISH_DERBY);
            Properties connectionProperties = dbProvisioner.getDefaultConnectionProperties();
            String defaultDBServiceName = dbProvisioner.getDefaultServiceName();
            return new SimpleServiceDefinition(defaultDBServiceName, RDBMS_SERVICE_TYPE, connectionProperties);
        } else {
            return null;
        }
    }

    public ProvisionedService provisionService(ServiceDefinition svcDefn) {
        String serviceName = svcDefn.getName();

        ArrayList<String> params;
        String[] parameters;

        CommandResult result = commandRunner.run("list-database-services");
        if (!result.getOutput().contains(serviceName)) {
            //create-database-service
            params = new ArrayList<String>();
            params.add(serviceName);
            parameters = new String[params.size()];
            parameters = params.toArray(parameters);

            result = commandRunner.run("create-database-service", parameters);
            if (result.getExitStatus().equals(CommandResult.ExitStatus.FAILURE)) {
                System.out.println("create-database-service [" + serviceName + "] failed");
            }
        }

        CloudRegistryEntry entry = serviceUtil.retrieveCloudEntry(serviceName, ServiceUtil.SERVICE_TYPE.DATABASE);
        if (entry == null) {
            throw new RuntimeException("unable to get DB service : " + serviceName);
        }

        params = new ArrayList<String>();
        params.add(serviceName);
        parameters = new String[params.size()];
        parameters = params.toArray(parameters);

        result = commandRunner.run("start-database-service", parameters);
        if (result.getExitStatus().equals(CommandResult.ExitStatus.FAILURE)) {
            System.out.println("start-database-service [" + serviceName + "] failed");
        }


        Properties connectionProperties = ((SimpleServiceDefinition) svcDefn).getProperties();
        //TODO HACK as we use serviceUtil to get DB's IP Address.
        String ipAddress = serviceUtil.getIPAddress(serviceName, ServiceUtil.SERVICE_TYPE.DATABASE);
        connectionProperties.put("serverName", ipAddress);

        return new DerbyProvisionedService(svcDefn, ServiceStatus.STARTED);
    }

    public void associateServices(ProvisionedService provisionedSvc, ServiceReference svcRef, boolean beforeDeployment) {
        //no-op
    }

    public ApplicationContainer deploy(ReadableArchive cloudArchive) {
        return null;
    }

    public boolean isRunning(ProvisionedService provisionedSvc) {
        return provisionedSvc.getStatus().equals(ServiceStatus.STARTED);
    }

    public ProvisionedService match(ServiceReference svcRef) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public boolean reconfigureServices(ProvisionedService oldPS, ProvisionedService newPS) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public Set<ServiceDefinition> getImplicitServiceDefinitions(
            ReadableArchive cloudArchive) {
        //no-op. Just by looking at a orchestration archive
        //the db plugin cannot say that a DB needs to be provisioned. 
        return new HashSet<ServiceDefinition>();
    }
}

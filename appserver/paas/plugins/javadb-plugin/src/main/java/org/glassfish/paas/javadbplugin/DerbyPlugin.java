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
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.embeddable.CommandResult;
import org.glassfish.embeddable.CommandRunner;
import org.glassfish.paas.javadbplugin.cli.DatabaseServiceUtil;
import org.glassfish.paas.orchestrator.ServiceOrchestrator;
import org.glassfish.paas.orchestrator.ServiceOrchestrator.ReconfigAction;
import org.glassfish.paas.orchestrator.provisioning.ServiceInfo;
import org.glassfish.paas.orchestrator.provisioning.DatabaseProvisioner;
import org.glassfish.paas.orchestrator.provisioning.cli.ServiceType;
import org.glassfish.paas.orchestrator.service.RDBMSServiceType;
import org.glassfish.paas.orchestrator.service.ServiceStatus;
import org.glassfish.paas.orchestrator.service.metadata.Property;
import org.glassfish.paas.orchestrator.service.metadata.ServiceCharacteristics;
import org.glassfish.paas.orchestrator.service.metadata.ServiceDescription;
import org.glassfish.paas.orchestrator.service.metadata.ServiceReference;
import org.glassfish.paas.orchestrator.service.spi.Plugin;
import org.glassfish.paas.orchestrator.service.spi.ProvisionedService;
import org.glassfish.virtualization.spi.AllocationStrategy;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

/**
 * @author Jagadish Ramu
 */
@Scoped(PerLookup.class)
@Service
public class DerbyPlugin implements Plugin<RDBMSServiceType> {

    @Inject
    private CommandRunner commandRunner;

    @Inject
    private DatabaseServiceUtil dbServiceUtil;

    private static final String DATASOURCE = "javax.sql.DataSource";
public static final String RDBMS_ServiceType = "Database";

    private static Logger logger = Logger.getLogger(DerbyPlugin.class.getName());

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

    public ServiceDescription getDefaultServiceDescription(String appName, ServiceReference svcRef) {

        if (DATASOURCE.equals(svcRef.getServiceRefType())) {

            DatabaseProvisioner dbProvisioner = new DerbyProvisioner();

            // create default service description.
            String defaultServiceName = dbProvisioner.getDefaultServiceName();
            List<Property> properties = new ArrayList<Property>();
            properties.add(new Property("service-type", RDBMS_ServiceType));
            properties.add(new Property("os-name", System.getProperty("os.name"))); // default OS will be same as that of what Orchestrator is running on.
            ServiceDescription sd = new ServiceDescription(defaultServiceName, appName,
                    "lazy", new ServiceCharacteristics(properties), null);

            // Fill the required details in service reference.
            Properties defaultConnPoolProperties = dbProvisioner.getDefaultConnectionProperties();
            defaultConnPoolProperties.setProperty("serviceName", defaultServiceName);
            svcRef.setProperties(defaultConnPoolProperties);
            
            return sd;
        } else {
            return null;
        }
    }

    private String formatArgument(List<Property> properties) {
        StringBuilder sb = new StringBuilder();
        if (properties != null) {
            for (Property p : properties) {
                sb.append(p.getName() + "=" + p.getValue() + ":");
            }
        }
        // remove the last ':'
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    public ProvisionedService provisionService(ServiceDescription serviceDescription, DeploymentContext dc) {
        String serviceName = serviceDescription.getName();
        logger.entering(getClass().getName(), "provisionService");

        ArrayList<String> params;
        String[] parameters;

        CommandResult result = commandRunner.run("_list-derby-services");
        if (!result.getOutput().contains(serviceName)) {
            //create-derby-service
            String serviceConfigurations = formatArgument(serviceDescription.getConfigurations());
            String appNameParam = "";
            if (serviceDescription.getAppName() != null) {
                appNameParam = "--appname=" + serviceDescription.getAppName();
            }

        // either template identifier or service characteristics are specified, not both.
        if (serviceDescription.getTemplateIdentifier() != null) {
            String templateId = serviceDescription.getTemplateIdentifier().getId();
            result = commandRunner.run("_create-derby-service",
                    "--templateid=" + templateId,
                    "--serviceconfigurations", serviceConfigurations,
                    "--waitforcompletion=true", appNameParam, serviceName);
        } else if (serviceDescription.getServiceCharacteristics() != null) {
            String serviceCharacteristics = formatArgument(serviceDescription.
                    getServiceCharacteristics().getServiceCharacteristics());
            result = commandRunner.run("_create-derby-service",
                    "--servicecharacteristics=" + serviceCharacteristics,
                    "--serviceconfigurations", serviceConfigurations,
                    "--waitforcompletion=true", appNameParam, serviceName);
        }
            if (result.getExitStatus().equals(CommandResult.ExitStatus.FAILURE)) {
                System.out.println("_create-derby-service [" + serviceName + "] failed");
            }
        }

        ServiceInfo entry = dbServiceUtil.retrieveCloudEntry(serviceName,
                serviceDescription.getAppName(), ServiceType.DATABASE);
        if (entry == null) {
            throw new RuntimeException("unable to get DB service : " + serviceName);
        }

        params = new ArrayList<String>();
        if(serviceDescription.getAppName() != null){
            params.add("--appname="+serviceDescription.getAppName());
        }
        params.add("servicename="+serviceName);
        parameters = new String[params.size()];
        parameters = params.toArray(parameters);

        result = commandRunner.run("_start-derby-service", parameters);
        if (result.getExitStatus().equals(CommandResult.ExitStatus.FAILURE)) {
            System.out.println("_start-derby-service [" + serviceName + "] failed");
        }

        Properties serviceProperties = new Properties();
        String ipAddress = entry.getIpAddress();
        serviceProperties.put("host", ipAddress);
        serviceProperties.put("port", "1527"); // TODO :: grab the actual port.

        DerbyProvisionedService dps = new DerbyProvisionedService(serviceDescription, serviceProperties);
        dps.setStatus(ServiceStatus.STARTED);
        return dps;
    }

    public ProvisionedService getProvisionedService(ServiceDescription serviceDescription, ServiceInfo serviceInfo){
        Properties serviceProperties = new Properties();
        String ipAddress = serviceInfo.getIpAddress();
        serviceProperties.put("host", ipAddress);
        serviceProperties.put("port", "1527"); // TODO :: grab the actual port.
        DerbyProvisionedService dps = new DerbyProvisionedService(serviceDescription, serviceProperties);
        dps.setStatus(dbServiceUtil.getServiceStatus(serviceInfo));
        return dps;
    }

    public void associateServices(ProvisionedService serviceConsumer, ServiceReference svcRef,
                                  ProvisionedService serviceProvider, boolean beforeDeployment, DeploymentContext dc) {
        //no-op
    }

    public ApplicationContainer deploy(ReadableArchive cloudArchive) {
        return null;
    }

    public ProvisionedService startService(ServiceDescription serviceDescription, ServiceInfo serviceInfo) {
        String serviceName = serviceDescription.getName();
        logger.entering(getClass().getName(), "startService");

        ArrayList<String> params;
        String[] parameters;

        CommandResult result = commandRunner.run("_list-derby-services");
        if (result.getOutput().contains(serviceName)) {
            params = new ArrayList<String>();

            if(serviceDescription.getAppName() != null){
                params.add("--appname="+serviceDescription.getAppName());
            }
            params.add(serviceName);
            parameters = new String[params.size()];
            parameters = params.toArray(parameters);

            result = commandRunner.run("_start-derby-service", parameters);
            if (result.getExitStatus().equals(CommandResult.ExitStatus.FAILURE)) {
                System.out.println("_start-derby-service [" + serviceName + "] failed");
            }
        }

        ServiceInfo entry = dbServiceUtil.retrieveCloudEntry(serviceName, serviceDescription.getAppName(), ServiceType.DATABASE);
        if (entry == null) {
            throw new RuntimeException("unable to get DB service : " + serviceName);
        }

        Properties serviceProperties = new Properties();
        String ipAddress = entry.getIpAddress();
        serviceProperties.put("host", ipAddress);
        serviceProperties.put("port", "1527"); // TODO :: grab the actual port.

        DerbyProvisionedService dps = new DerbyProvisionedService(serviceDescription, serviceProperties);
        dps.setStatus(ServiceStatus.STARTED);
        return dps;
    }

    public boolean stopService(ServiceDescription serviceDescription, ServiceInfo serviceInfo) {
        String appNameParam="";
        if(serviceDescription.getAppName() != null){
            appNameParam="--appname="+serviceDescription.getAppName();
        }
        CommandResult result = commandRunner.run("_stop-derby-service",
                appNameParam, serviceDescription.getName());
        System.out.println("_stop-derby-service command output [" + result.getOutput() + "]");
        if (result.getExitStatus() == CommandResult.ExitStatus.SUCCESS) {
            return true;
        } else {
            //TODO throw exception ?
            result.getFailureCause().printStackTrace();
            return false;
        }
    }

    public boolean isRunning(ProvisionedService provisionedSvc) {
        return provisionedSvc.getStatus().equals(ServiceStatus.STARTED);
    }

    public ProvisionedService match(ServiceReference svcRef) {
        throw new UnsupportedOperationException("Not implemented yet");
    }


    public Set<ServiceDescription> getImplicitServiceDescriptions(
            ReadableArchive cloudArchive, String appName) {
        //no-op. Just by looking at a orchestration archive
        //the db plugin cannot say that a DB needs to be provisioned. 
        return new HashSet<ServiceDescription>();
    }

    public boolean unprovisionService(ServiceDescription serviceDescription, DeploymentContext dc){
        String appNameParam="";
        if(serviceDescription.getAppName() != null){
            appNameParam="--appname="+serviceDescription.getAppName();
        }
        CommandResult result = commandRunner.run("_delete-derby-service",
                "--waitforcompletion=true", appNameParam, serviceDescription.getName());
        System.out.println("_delete-derby-service command output [" + result.getOutput() + "]");
        if (result.getExitStatus() == CommandResult.ExitStatus.SUCCESS) {
            return true;
        } else {
            //TODO throw exception ?
            result.getFailureCause().printStackTrace();
            return false;
        }
    }

    public void dissociateServices(ProvisionedService serviceConsumer, ServiceReference svcRef,
                                   ProvisionedService serviceProvider, boolean beforeUndeploy, DeploymentContext dc){
        //no-op
    }

    @Override
    public ProvisionedService scaleService(ServiceDescription serviceDesc,
            int scaleCount, AllocationStrategy allocStrategy) {
        //no-op
        throw new UnsupportedOperationException("Scaling of Derby Service " +
        		"not supported in this release");
    }

    @Override
    public boolean reconfigureServices(ProvisionedService oldPS,
            ProvisionedService newPS) {
        //no-op
        throw new UnsupportedOperationException("Reconfiguration of Service " +
                "not supported in this release");
    }

    @Override
    public boolean reassociateServices(ProvisionedService oldPS,
            ProvisionedService newPS, ReconfigAction reason) {
        //no-op
        throw new UnsupportedOperationException("Reassociation of Service " +
                "not supported in this release");
    }

}

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
package org.glassfish.paas.dbspecommon;

import org.glassfish.api.deployment.ApplicationContainer;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.internal.api.ClassLoaderHierarchy;
import org.glassfish.paas.orchestrator.PaaSDeploymentContext;
import org.glassfish.paas.orchestrator.ServiceOrchestrator;
import org.glassfish.paas.orchestrator.provisioning.ServiceInfo;
import org.glassfish.paas.orchestrator.service.RDBMSServiceType;
import org.glassfish.paas.orchestrator.service.ServiceStatus;
import org.glassfish.paas.orchestrator.service.metadata.Property;
import org.glassfish.paas.orchestrator.service.metadata.ServiceCharacteristics;
import org.glassfish.paas.orchestrator.service.metadata.ServiceDescription;
import org.glassfish.paas.orchestrator.service.metadata.ServiceReference;
import org.glassfish.paas.orchestrator.service.spi.ServicePlugin;
import org.glassfish.paas.orchestrator.service.spi.ProvisionedService;
import org.glassfish.paas.orchestrator.service.spi.Service;
import org.glassfish.paas.spe.common.ServiceProvisioningEngineBase;
import org.glassfish.virtualization.spi.AllocationStrategy;
import org.glassfish.virtualization.spi.VirtualMachine;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.SQLExec;
import org.apache.tools.ant.types.Path;
import org.jvnet.hk2.annotations.Inject;

import java.io.File;
import java.util.*;

/**
 * Common Service Provisioning Engine for Databases.
 *
 * @author Shalini M
 */
public abstract class DatabaseSPEBase extends ServiceProvisioningEngineBase<RDBMSServiceType>
        implements DatabaseSPEConstants {

    @Inject
    private ClassLoaderHierarchy clh;

    /**
     * {@inheritDoc}
     */
    public boolean handles(ReadableArchive cloudArchive) {
        //For prototype, DB Plugin has no role here.
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public boolean handles(ServiceDescription serviceDescription) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public RDBMSServiceType getServiceType() {
        return new RDBMSServiceType();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isReferenceTypeSupported(String referenceType) {
        return DATASOURCE.equalsIgnoreCase(referenceType);
    }

    /**
     * {@inheritDoc}
     */
    public Set<ServiceReference> getServiceReferences(String appName,
                                                      ReadableArchive cloudArchive,
                                                      PaaSDeploymentContext context) {
        return new HashSet<ServiceReference>();
    }

    /**
     * {@inheritDoc}
     */
    public ServiceDescription getDefaultServiceDescription(
            String appName, ServiceReference svcRef) {
        if (DATASOURCE.equals(svcRef.getType())) {

            List<Property> properties = new ArrayList<Property>();
            properties.add(new Property(SERVICE_TYPE, RDBMS_ServiceType));

            String initSqlFile = "";
            String databaseName = "";
            List<Property> configurations = new ArrayList<Property>();
            configurations.add(new Property(INIT_SQL_SVC_CONFIG, initSqlFile));
            configurations.add(new Property(DATABASE_NAME_SVC_CONFIG, databaseName));

            ServiceDescription sd = new ServiceDescription(getDefaultServiceName(), appName,
                    SERVICE_INIT_TYPE_LAZY, new ServiceCharacteristics(properties), configurations);

/*
            // Fill the required details in service reference.
            Properties defaultConnPoolProperties = dbProvisioner.getDefaultConnectionProperties();
            defaultConnPoolProperties.setProperty("serviceName", defaultServiceName);
            svcRef.setProperties(defaultConnPoolProperties);
*/

            return sd;
        } else {
            return null;
        }
    }

    /**
     * Get the default service name for the database service.
     *
     * @return service name
     */
    protected abstract String getDefaultServiceName();

    /**
     * {@inheritDoc}
     */
    public Set<ServiceDescription> getImplicitServiceDescriptions(
            ReadableArchive cloudArchive, String appName) {
        //no-op. Just by looking at a orchestration archive
        //the db plugin cannot say that a DB needs to be provisioned.
        return new HashSet<ServiceDescription>();
    }

    /**
     * {@inheritDoc}
     */
    public ProvisionedService provisionService(
            ServiceDescription serviceDescription, PaaSDeploymentContext dc) {
        ProvisionedService provisionedService = createService(serviceDescription).join();
        //Start database
        Properties properties = provisionedService.getProperties();
        VirtualMachine vm = getVmByID(serviceDescription.getVirtualClusterName(),
                properties.getProperty(VIRTUAL_MACHINE_ID));
        String ipAddress = properties.getProperty(VIRTUAL_MACHINE_IP_ADDRESS);
        startDatabase(vm);

        //Create custom database
        String databaseName = serviceDescription.getConfiguration(DATABASE_NAME_SVC_CONFIG);
        if (databaseName != null && databaseName.trim().length() > 0) {
            setDatabaseName(databaseName);
            createDatabase(getServiceProperties(ipAddress));
        }

        //Execute Init SQL
        String initSqlFile = serviceDescription.getConfiguration(INIT_SQL_SVC_CONFIG);
        if (initSqlFile != null && initSqlFile.trim().length() > 0) {
            executeInitSql(getServiceProperties(ipAddress), initSqlFile);
        }

        //Add database name to provisioned service properties
        properties.putAll(getServiceProperties(
                properties.getProperty(VIRTUAL_MACHINE_IP_ADDRESS)));
        return provisionedService;
    }

    /**
     * {@inheritDoc}
     */
    public void associateServices(Service serviceConsumer,
                                  ServiceReference svcRef, Service serviceProvider,
                                  boolean beforeDeployment, PaaSDeploymentContext dc) {
        //no-op
    }

    /**
     * {@inheritDoc}
     */
    public ProvisionedService getProvisionedService(ServiceDescription serviceDescription,
                                                    ServiceInfo serviceInfo) {
        ProvisionedService provisionedService = getProvisionedService(serviceDescription);
        String databaseName = serviceDescription.getConfiguration(DATABASE_NAME_SVC_CONFIG);
        Properties properties = provisionedService.getProperties();
        properties.putAll(getServiceProperties(
                properties.getProperty(VIRTUAL_MACHINE_IP_ADDRESS)));
        return provisionedService;
    }

    /**
     * {@inheritDoc}
     */
    public boolean unprovisionService(ServiceDescription serviceDescription,
                                      PaaSDeploymentContext dc) {
        Properties properties = getProvisionedService(serviceDescription).getProperties();
        VirtualMachine vm = getVmByID(serviceDescription.getVirtualClusterName(),
                properties.getProperty(VIRTUAL_MACHINE_ID));
        String ipAddress = properties.getProperty(VIRTUAL_MACHINE_IP_ADDRESS);
        stopDatabase(vm);
        return deleteService(serviceDescription);
    }

    /**
     * {@inheritDoc}
     */
    public ProvisionedService scaleService(ServiceDescription serviceDesc,
                                           int scaleCount, AllocationStrategy allocStrategy) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public void dissociateServices(Service serviceConsumer,
                                   ServiceReference svcRef, Service serviceProvider,
                                   boolean beforeUndeploy, PaaSDeploymentContext dc) {
        //no-op
    }

    /**
     * {@inheritDoc}
     */
    public ApplicationContainer deploy(ReadableArchive cloudArchive) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public ProvisionedService startService(ServiceDescription serviceDescription,
                                           ServiceInfo serviceInfo) {
        ProvisionedService provisionedService = startService(serviceDescription);

        //Start database
        Properties properties = provisionedService.getProperties();
        VirtualMachine vm = getVmByID(serviceDescription.getVirtualClusterName(),
                properties.getProperty(VIRTUAL_MACHINE_ID));
        startDatabase(vm);

        String databaseName = serviceDescription.getConfiguration(DATABASE_NAME_SVC_CONFIG);
        //Add database name to provisioned service properties
        properties.putAll(getServiceProperties(
                properties.getProperty(VIRTUAL_MACHINE_IP_ADDRESS)));
        return provisionedService;
    }

    /**
     * {@inheritDoc}
     */
    public boolean stopService(ServiceDescription serviceDescription, ServiceInfo serviceInfo) {
        //Stop database
        Properties properties = getProvisionedService(serviceDescription).getProperties();
        VirtualMachine vm = getVmByID(serviceDescription.getVirtualClusterName(),
                properties.getProperty(VIRTUAL_MACHINE_ID));
        stopDatabase(vm);
        return stopService(serviceDescription);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isRunning(ProvisionedService provisionedSvc) {
        return provisionedSvc.getStatus().equals(ServiceStatus.STARTED);
    }

    /**
     * {@inheritDoc}
     */
    public ProvisionedService match(ServiceReference svcRef) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * {@inheritDoc}
     */
    public boolean reconfigureServices(ProvisionedService oldPS, ProvisionedService newPS) {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public boolean reassociateServices(org.glassfish.paas.orchestrator.service.spi.Service svcConsumer,
                                       org.glassfish.paas.orchestrator.service.spi.Service oldSvcProvider,
                                       org.glassfish.paas.orchestrator.service.spi.Service newSvcProvider,
                                       ServiceOrchestrator.ReconfigAction reason) {
        return true;
    }

    protected abstract Properties getServiceProperties(String ipAddress);

    /**
     * Start the database instance
     *
     * @param virtualMachine the virtual machine containing the instantiated
     * template.
     */
    protected abstract void startDatabase(VirtualMachine virtualMachine);

    /**
     * Stop the database instance
     *
     * @param virtualMachine the virtual machine containing the instantiated
     * template.
     */
    protected abstract void stopDatabase(VirtualMachine virtualMachine);

    /**
     * Create database if it does not exist
     *
     * @param dbProps Database connection properties
     */
    public abstract void createDatabase(Properties dbProps);

    /**
     * Set Database Name.
     *
     * @param databaseName
     */
    protected abstract void setDatabaseName(String databaseName);

    /**
     * Get Database Name
     *
     * @return database name
     */
    protected abstract String getDatabaseName();

    /**
     * Execute Initialization SQL file provided.
     *
     * @param dbProps Database connection properties
     * @param initSqlFile initialization SQL file
     */
    public abstract void executeInitSql(Properties dbProps, String initSqlFile);

    /**
     * Execute ant task using the driverName and url supplied. The task of a sql
     * execution is done here.
     *
     * @param dbProps Database connection properties
     * @param driverName JDBC driver classname
     * @param url Database URL
     * @param sql SQL file or string
     * @param isSqlFile indicates if sql is a file or sql string
     */
    protected void executeAntTask(Properties dbProps, String driverName, String url,
                                  String sql, boolean isSqlFile) {
        Project project = new Project();
        project.init();
        SQLExec task = new SQLExec();
        SQLExec.OnError error = new SQLExec.OnError();
        error.setValue("continue");
        task.setDriver(driverName);
        task.setUrl(url);
        task.setUserid(dbProps.getProperty(USER));
        task.setPassword(dbProps.getProperty(PASSWORD));
        if (!isSqlFile) {
            task.addText(sql);
        } else {
            task.setSrc(new File(sql));
        }
        task.setOnerror(error);
        Path path = new Path(project, clh.getCommonClassPath());
        path.addJavaRuntime();
        task.setClasspath(path);
        task.setProject(project);
        task.setAutocommit(true);
        task.execute();
    }
}

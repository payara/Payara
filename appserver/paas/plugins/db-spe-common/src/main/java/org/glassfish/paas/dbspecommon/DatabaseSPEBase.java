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
package org.glassfish.paas.dbspecommon;

import org.apache.tools.ant.AntClassLoader;
import org.glassfish.api.deployment.ApplicationContainer;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.deployment.common.DeploymentUtils;
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
import java.io.IOException;
import java.io.InputStream;
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
                                                      PaaSDeploymentContext dc) {
        Set<ServiceReference> servicesReferences = new LinkedHashSet<ServiceReference>();
        if(dc.getDeploymentContext() != null){
            //TODO : Jagadish : we are returning service-reference on own service, which can be
            //avoided when new contract on ServicePlugin is introduced.
            String initSqlFile = null;
            initSqlFile = getInitSQLFileName(cloudArchive, dc);
            if (new File(initSqlFile).exists()) {
                servicesReferences.add(new ServiceReference("init-sql-ref","javax.sql.DataSource", null));
                return servicesReferences; //no need to check service properties as one service-ref is sufficient.
            }
            String servicePropertiesFile = null;
            servicePropertiesFile = getServicePropertiesFileName(dc, cloudArchive);

            if (new File(servicePropertiesFile).exists()) {
                servicesReferences.add(new ServiceReference("service-properties-ref","javax.sql.DataSource", null));
                return servicesReferences;
            }
        }
        return servicesReferences;
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
        ProvisionedService provisionedService = createService(serviceDescription).get();
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
            createDatabase(getServiceProperties(ipAddress),vm);
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

 	// with associateServices/dissocateServices being called bi-directionally,
        // make sure that the service-consumer is RDBMS type.
        if(!RDBMS_ServiceType.equals(serviceConsumer.getServiceType().toString())){
            return;
        }

        //Skip during post deploy phase
        if(!beforeDeployment) {
            return;
        }
        final DeploymentContext context = dc.getDeploymentContext();
        try {
            Boolean isDatabaseInitialized = context.getTransientAppMetaData(
                    getClass().getName() + DB_INITIALIZED, Boolean.class);
            if (isDatabaseInitialized == null || !isDatabaseInitialized) {

                final ReadableArchive readableArchive = context.getSource();
                String initSqlFile = null;
                String databaseName = null;
                String servicePropertiesFile = null;
                String ipAddress = serviceConsumer.getServiceProperties().getProperty(VIRTUAL_MACHINE_IP_ADDRESS);
                //Create Custom database
                servicePropertiesFile = getServicePropertiesFileName(dc, readableArchive);

                if (new File(servicePropertiesFile).exists()) {
                    //Get the database name from this file
                    Properties properties = new Properties();
                    try {
                        InputStream inputStream = readableArchive.getEntry(servicePropertiesFile);
                        if (inputStream != null) {
                            properties.load(readableArchive.getEntry(servicePropertiesFile));
                        }
                    } catch (IOException e) {

                    }
                    databaseName = properties.getProperty(DATABASE_NAME_SVC_CONFIG);
                    if (databaseName != null && databaseName.trim().length() > 0) {
                        setDatabaseName(databaseName);
                        createDatabase(getServiceProperties(ipAddress));
                    }
                }

                //Execute Init SQL
                initSqlFile = getInitSQLFileName(readableArchive, dc);
                if (new File(initSqlFile).exists()) {
                    setDatabaseName(serviceConsumer.getServiceProperties().getProperty(DATABASENAME));
                    executeInitSql(getServiceProperties(ipAddress), initSqlFile);
                }
            }
        } finally {
            //Since associateServices are called multiple times, this ensures that
            //the custom db name creation and init sql execution are executed just once.
            context.addTransientAppMetaData(getClass().getName()+DB_INITIALIZED, true);
        }
    }

    private String getServicePropertiesFileName(PaaSDeploymentContext dc, ReadableArchive readableArchive) {
        String servicePropertiesFile;
        if (DeploymentUtils.isWebArchive(readableArchive)) {
            servicePropertiesFile = dc.getDeploymentContext().getSource().getURI().getPath() +
                    "WEB-INF" + File.separator + "service.properties";
        } else {
            servicePropertiesFile = dc.getDeploymentContext().getSource().getURI().getPath() +
                    "META-INF" + File.separator + "service.properties";
        }
        return servicePropertiesFile;
    }

    private String getInitSQLFileName(ReadableArchive cloudArchive, PaaSDeploymentContext dc) {
        String initSqlFile;
        if (DeploymentUtils.isWebArchive(cloudArchive)) {
            initSqlFile = dc.getDeploymentContext().getSource().getURI().getPath() +
                    "WEB-INF" + File.separator + "init.sql";
        } else {
            initSqlFile = dc.getDeploymentContext().getSource().getURI().getPath() +
                    "META-INF" + File.separator + "init.sql";
        }
        return initSqlFile;
    }

    /**
     * {@inheritDoc}
     */
    public ProvisionedService getProvisionedService(ServiceDescription serviceDescription,
                                                    ServiceInfo serviceInfo) {
        ProvisionedService provisionedService = getProvisionedService(serviceDescription);
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
        stopDatabase(vm);
        return deleteService(serviceDescription);
    }

    /**
     * {@inheritDoc}
     */
    public ProvisionedService scaleService(ProvisionedService provisionedService,
                                           int scaleCount, AllocationStrategy allocStrategy) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public void dissociateServices(Service serviceConsumer,
                                   ServiceReference svcRef, Service serviceProvider,
                                   boolean beforeUndeploy, PaaSDeploymentContext dc) {

        // with associateServices/dissocateServices being called bi-directionally,
        // make sure that the service-consumer is RDBMS type.
        if(!RDBMS_ServiceType.equals(serviceConsumer.getServiceType().toString())){
            return;
        }

        //post undeployment phase - skip it
        if(!beforeUndeploy) {
            return;
        }
        final DeploymentContext context = dc.getDeploymentContext();
        try {
            Boolean isDatabaseUnInitialized = context.getTransientAppMetaData(
                    getClass().getName() + DB_UNINITIALIZED, Boolean.class);
            if (isDatabaseUnInitialized == null || !isDatabaseUnInitialized) {

                final ReadableArchive readableArchive = context.getSource();
                String tearDownSqlFile = null;
                String ipAddress = serviceConsumer.getProperties().getProperty(VIRTUAL_MACHINE_IP_ADDRESS);

                //Execute Tear down SQL
                if (DeploymentUtils.isWebArchive(readableArchive)) {
                    tearDownSqlFile = dc.getDeploymentContext().getSource().getURI().getPath() +
                            "WEB-INF" + File.separator + "teardown.sql";
                } else {
                    tearDownSqlFile = dc.getDeploymentContext().getSource().getURI().getPath() +
                            "META-INF" + File.separator + "teardown.sql";
                }
                if (new File(tearDownSqlFile).exists()) {
                    executeTearDownSql(getServiceProperties(ipAddress), tearDownSqlFile);
                }
            }
        } finally {
            //Since dissociateServices are called multiple times, this ensures that
            //the tear down sql execution is executed just once.
            context.addTransientAppMetaData(getClass().getName()+DB_UNINITIALIZED, true);
        }
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

        properties.putAll(getServiceProperties(
                properties.getProperty(VIRTUAL_MACHINE_IP_ADDRESS)));
        return provisionedService;
    }

    /**
     * {@inheritDoc}
     */
    public boolean stopService(ProvisionedService provisionedService, ServiceInfo serviceInfo) {
        //Stop database
        ServiceDescription serviceDescription = provisionedService.getServiceDescription();
        Properties properties = provisionedService.getProperties();
        VirtualMachine vm = getVmByID(serviceDescription.getVirtualClusterName(),
                properties.getProperty(VIRTUAL_MACHINE_ID));
        stopDatabase(vm);

        boolean stopped = stopService(serviceDescription);
        if(stopped){
            provisionedService.setStatus(ServiceStatus.STOPPED);
        }
        return stopped;
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
    public abstract void createDatabase(Properties dbProps,VirtualMachine vm);

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
     * Execute Tear down SQL file provided
     *
     * @param dbProps Database connection properties
     * @param teardownSqlFile tear down SQL file
     */
    public abstract void executeTearDownSql(Properties dbProps, String teardownSqlFile);

    protected void executeSql(Properties dbProps, String driverClassName, String sqlFile) {
        String url = dbProps.getProperty(URL);
        executeAntTask(dbProps, driverClassName, url, sqlFile, true);
    }

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
        Project project = new Project() {

            // JDBCTask loads the driver using the classloader returned by this method.
            @Override
            public AntClassLoader createClassLoader(Path path) {
                // Default implementation always uses getClass().getClassLoader()
                // as the base classloader while creating the new AntClassLoader
                // While running inside GlassFish, the base classloader will be
                // db-spe-common module's OSGi classloader,
                // Since it is OSGi a classloader, it makes some of the classes
                // viz., sun.reflect.Generated* cause IllegalAccessError during runtime.

                // Hence create the classloader with common classloader (CCL)
                // as the base classloader with parentFirst=true
                // CCL has jdbc drivers placed in domain1/lib.
                // Hence, SQL stmt execution will happen under the CCL's context.
                return new AntClassLoader(clh.getCommonClassLoader(), this, path, true);
            }
        };

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

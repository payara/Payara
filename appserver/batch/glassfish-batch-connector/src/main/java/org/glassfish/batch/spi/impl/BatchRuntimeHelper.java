/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2014] [C2B2 Consulting Limited] 
package org.glassfish.batch.spi.impl;

import com.hazelcast.core.HazelcastInstance;
import com.ibm.jbatch.container.servicesmanager.ServiceTypes;
import com.ibm.jbatch.spi.*;
import com.sun.enterprise.config.serverbeans.Config;

import fish.payara.jbatch.persistence.rdbms.DB2PersistenceManager;
import fish.payara.jbatch.persistence.rdbms.JBatchJDBCPersistenceManager;
import fish.payara.jbatch.persistence.rdbms.MySqlPersistenceManager;
import fish.payara.jbatch.persistence.rdbms.OraclePersistenceManager;
import fish.payara.jbatch.persistence.rdbms.PostgresPersistenceManager;

import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.EventTypes;
import org.glassfish.api.event.Events;
import org.glassfish.deployment.common.DeploymentContextImpl;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.runlevel.RunLevel;
import org.glassfish.internal.api.ServerContext;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.data.ApplicationRegistry;
import org.glassfish.internal.data.ModuleInfo;
import org.glassfish.internal.deployment.Deployment;
import org.jvnet.hk2.annotations.Service;

import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.inject.Inject;
import javax.inject.Named;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sql.DataSource;

/**
 * Helper class to get values for Batch Runtime. Follows zero-config rules by
 * using default values when the batch-runtime config object is not present in
 * domain.xml
 *
 * @author Mahesh Kannan
 */
@Service
@RunLevel(StartupRunLevel.VAL)
public class BatchRuntimeHelper
        implements PostConstruct, EventListener {

    public static final String PAYARA_TABLE_PREFIX_PROPERTY = "payara.jbatch.table.prefix";

    public static final String PAYARA_TABLE_SUFFIX_PROPERTY = "payara.jbatch.table.suffix";

    @Inject
    ServiceLocator serviceLocator;

    @Inject
    @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    private BatchRuntimeConfiguration batchRuntimeConfiguration;

    @Inject
    private ServerContext serverContext;

    private volatile ManagedServiceActivator activator;

    @Inject
    private GlassFishBatchSecurityHelper glassFishBatchSecurityHelper;

    @Inject
    private Logger logger;

    @Inject
    Events events;

    @Inject
    Config config;

    @Inject
    ApplicationRegistry applicationRegistry;

    private GlassFishBatchExecutorServiceProvider glassFishBatchExecutorServiceProvider
            = new GlassFishBatchExecutorServiceProvider();

    private AtomicBoolean initialized = new AtomicBoolean(false);

    private static final String CREATE_TABLE_DDL_NAME = "/jsr352-";

    public void checkAndInitializeBatchRuntime() {
        if (!initialized.get()) {
            synchronized (this) {
                if (!initialized.get()) {
                    initialized.set(true);
//                    try {
//                        //Temporary fix till batch_{db_vendor}.sql is part of the distribution
//                        File sqlFile = new File(ddlDir, "batch_derby.sql");
//                        if (sqlFile.exists()) {
//                            java2DBProcessorHelper.executeDDLStatement(ddlDir.getCanonicalPath() + CREATE_TABLE_DDL_NAME, getDataSourceLookupName());
////                            java2DBProcessorHelper.executeDDLStatement(sqlFile, getDataSourceLookupName());
//                        } else {
//                            logger.log(Level.WARNING, sqlFile.getAbsolutePath() + " does NOT exist");
//                        }

//                        Java2DBProcessorHelper java2DBProcessorHelper = new Java2DBProcessorHelper(this.getClass().getSimpleName());
//                        File ddlDir = new File(serverContext.getInstallRoot(), "/lib/install/databases/");
//                        logger.log(Level.INFO, "**[1]Executing DDL for: " + ddlDir.getCanonicalPath() + CREATE_TABLE_DDL_NAME);
//                        java2DBProcessorHelper.executeDDLStatement(
//                                ddlDir.getCanonicalPath() + CREATE_TABLE_DDL_NAME, getDataSourceLookupName());
//                        initialized.set(true);
//
//                    } catch (Exception ex) {
//                        logger.log(Level.FINE, "Exception during table creation ", ex);
//                    }
                }
            }
        }
    }

    @Override
    public void postConstruct() {
        events.register(this);

        BatchSPIManager batchSPIManager = BatchSPIManager.getInstance();
        batchSPIManager.registerExecutorServiceProvider(glassFishBatchExecutorServiceProvider);
        batchSPIManager.registerBatchSecurityHelper(glassFishBatchSecurityHelper);
        // setting this puts JBatch into SE mode which is a JBatch bug.
        batchSPIManager.registerPlatformMode(BatchSPIManager.PlatformMode.EE);

        Properties overrideProperties = new Properties();
        overrideProperties.put(PAYARA_TABLE_PREFIX_PROPERTY, batchRuntimeConfiguration.getTablePrefix());
        overrideProperties.put(PAYARA_TABLE_SUFFIX_PROPERTY, batchRuntimeConfiguration.getTableSuffix());
        overrideProperties.put(ServiceTypes.PERSISTENCE_MANAGEMENT_SERVICE, determinePersistenceManagerClass());
        overrideProperties.put(ServiceTypes.CONTAINER_ARTIFACT_FACTORY_SERVICE,"com.ibm.jbatch.container.services.impl.CDIBatchArtifactFactoryImpl" );
        overrideProperties.put(ServiceTypes.BATCH_THREADPOOL_SERVICE, "com.ibm.jbatch.container.services.impl.SPIDelegatingThreadPoolServiceImpl");
        overrideProperties.put(ServiceTypes.Name.JAVA_EDITION_IS_SE_DUMMY_SERVICE, "false");
        batchSPIManager.registerBatchContainerOverrideProperties(overrideProperties);

        try {
            DatabaseConfigurationBean databaseConfigurationBean = new GlassFishDatabaseConfigurationBean();
            databaseConfigurationBean.setSchema(getSchemaName());
            batchSPIManager.registerDatabaseConfigurationBean(databaseConfigurationBean);
        } catch (DatabaseAlreadyInitializedException daiEx) {
            daiEx.printStackTrace();
        }
    }

    public void setExecutorService(ExecutorService executorService) {
        glassFishBatchExecutorServiceProvider.setExecutorService(executorService);
    }

    private Set<String> tagNamesRequiringCleanup = new HashSet<>();

    private void registerIfBatchJobsDirExists(ApplicationInfo applicationInfo) {
        if (applicationInfo != null && applicationInfo.isLoaded()) {
            ClassLoader appClassLoader = applicationInfo.getAppClassLoader();
            if (appClassLoader != null && appClassLoader.getResource("META-INF/batch-jobs") != null) {
                tagNamesRequiringCleanup.add(config.getName() + ":" + applicationInfo.getName());
                return;
            }
            for (ModuleInfo moduleInfo : applicationInfo.getModuleInfos()) {
                ClassLoader moduleClassLoader = moduleInfo.getModuleClassLoader();
                if (moduleClassLoader != null && moduleClassLoader.getResource("META-INF/batch-jobs") != null) {
                    tagNamesRequiringCleanup.add(config.getName() + ":" + applicationInfo.getName());
                }
            }
        }
    }

    @Override
    public void event(Event event) {
        try {
            if (event.is(EventTypes.SERVER_READY)) {
                for (String appName : applicationRegistry.getAllApplicationNames()) {
                    ApplicationInfo applicationInfo = applicationRegistry.get(appName);
                    registerIfBatchJobsDirExists(applicationInfo);
                }
            } else if (event.is(Deployment.APPLICATION_LOADED)) {
                if (event.hook() != null && event.hook() instanceof ApplicationInfo) {
                    ApplicationInfo applicationInfo = (ApplicationInfo) event.hook();
                    registerIfBatchJobsDirExists(applicationInfo);
                }
            }
            if (event.is(Deployment.UNDEPLOYMENT_SUCCESS)) {
                if (event.hook() != null && event.hook() instanceof DeploymentContextImpl) {
                    DeploymentContextImpl deploymentContext = (DeploymentContextImpl) event.hook();
                    Properties props = deploymentContext.getAppProps();
                    String appName = props.getProperty("defaultAppName");
                    if (!Boolean.parseBoolean(props.getProperty("retain-batch-jobs"))) {
                        String tagName = config.getName() + ":" + appName;
                        try {
                            BatchSPIManager batchSPIManager = BatchSPIManager.getInstance();
                            if (batchSPIManager != null && batchSPIManager.getBatchJobUtil() != null) {
                                batchSPIManager.getBatchJobUtil().purgeOwnedRepositoryData(tagName);
                                tagNamesRequiringCleanup.remove(tagName);
                            } else if (tagNamesRequiringCleanup.contains(tagName)) {
                                //Force initialization of BatchRuntime
                                JobOperator jobOperator = BatchRuntime.getJobOperator();

                                if (batchSPIManager.getBatchJobUtil() != null) {
                                    batchSPIManager.getBatchJobUtil().purgeOwnedRepositoryData(tagName);
                                    tagNamesRequiringCleanup.remove(tagName);
                                }
                            }
                        } catch (Exception ex) {
                            logger.log(Level.FINE, "Error while purging jobs", ex);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            logger.log(Level.FINE, "Exception while handling event: " + event, ex);
        }
    }

    public String getDataSourceLookupName() {
        String val = batchRuntimeConfiguration.getDataSourceLookupName();
        if (val == null || val.trim().length() == 0) {
            val = serverContext.getInstanceName().equals("server")
                    ? "jdbc/__TimerPool" : "jdbc/__default";
        }

        return val;
    }

    public static String getDefaultDataSourceLookupNameForTarget(String targetName) {
        return targetName.equals("server") ? "jdbc/__TimerPool" : "jdbc/__default";
    }

    private String getSchemaName() {
        return batchRuntimeConfiguration.getSchemaName();
    }

    public String getExecutorServiceLookupName() {
        return batchRuntimeConfiguration.getExecutorServiceLookupName();
    }

    private String determinePersistenceManagerClass() {
        String result = JBatchJDBCPersistenceManager.class.getName();
        try {
            // this is the default
            String dataSourceName = getDataSourceLookupName();
            InitialContext ctx = new InitialContext();
            Object object = ctx.lookup(dataSourceName);

            //check whether the referenced JNDI entry is a DataSource
            if (object instanceof DataSource) {
                Connection conn = null;
                try {
                    DataSource ds = DataSource.class.cast(object);
                    conn = ds.getConnection();
                    String database = conn.getMetaData().getDatabaseProductName();
                    if (database.contains("Derby")) {
                        result = JBatchJDBCPersistenceManager.class.getName();
                    } else if (database.contains("MySQL")) {
                        result = MySqlPersistenceManager.class.getName();
                    } else if (database.contains("Oracle")) {
                        result = OraclePersistenceManager.class.getName();
                    } else if (database.contains("PostgreSQL")) {
                        result = PostgresPersistenceManager.class.getName();
                    } else if (database.contains("DB2")) {
                        result = DB2PersistenceManager.class.getName();
                    }
                } catch (SQLException ex) {
                    Logger.getLogger(BatchRuntimeHelper.class.getName()).log(Level.SEVERE, "Failed to get connecion to determine database type", ex);
                } finally {
                    if (conn != null) {
                        try {
                            conn.close();
                        } catch (SQLException ex) {
                            Logger.getLogger(BatchRuntimeHelper.class.getName()).log(Level.SEVERE, "Failed to close connection", ex);
                        }
                    }
                }
            } else if (object instanceof HazelcastInstance) {
                result = "fish.payara.jbatch.persistence.hazelcast.HazelcastPersistenceService";
            }
        } catch (NamingException ex) {
            Logger.getLogger(BatchRuntimeHelper.class.getName()).log(Level.WARNING, "Unable to find JBatch configured DataSource", ex);
        }
        return result;
    }

    private class GlassFishDatabaseConfigurationBean
            extends DatabaseConfigurationBean {

        @Override
        public String getJndiName() {
            checkAndInitializeBatchRuntime();
            return getDataSourceLookupName();
        }
    }

    private class GlassFishBatchExecutorServiceProvider
            implements ExecutorServiceProvider {

        private volatile ExecutorService executorService;

        void setExecutorService(ExecutorService executorService) {
            this.executorService = executorService;
        }

        @Override
        public ExecutorService getExecutorService() {
            checkAndInitializeBatchRuntime();
            if (executorService == null) {
                synchronized (this) {
                    if (executorService == null) {
                        if (System.getSecurityManager() == null) {
                            executorService = lookupExecutorService();
                        } else {
                            java.security.AccessController.doPrivileged(
                                    new java.security.PrivilegedAction() {
                                        public java.lang.Object run() {
                                            executorService = lookupExecutorService();
                                            return null;
                                        }
                                    }
                            );
                        }
                    }
                }
            }
            return executorService;
        }
    }

    public ExecutorService lookupExecutorService() {
        return lookupExecutorService(getExecutorServiceLookupName());
    }

    public ExecutorService lookupExecutorService(String exeLookupName) {
        try {
            if (activator == null) {
                activator = serviceLocator.getService(ManagedServiceActivator.class);
            }
            InitialContext initialContext = new InitialContext();
            Object obj = initialContext.lookup(exeLookupName);
            if (!(obj instanceof ExecutorService)) {
                throw new GlassFishBatchValidationException(exeLookupName + " is not mapped to an ExecutorService. Batch operations may not work correctly.");
            }
            return (ExecutorService) obj;
        } catch (NamingException nEx) {
            throw new GlassFishBatchValidationException("No ExecutorService bound to name = " + exeLookupName, nEx);
        }
    }

}

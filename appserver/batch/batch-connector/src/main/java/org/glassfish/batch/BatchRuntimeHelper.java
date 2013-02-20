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
package org.glassfish.batch;

import com.ibm.batch.container.config.DatabaseConfigurationBean;
import com.ibm.batch.container.config.GlassfishThreadPoolConfigurationBean;
import com.ibm.batch.container.config.IBatchConfig;
import com.ibm.batch.container.services.ServicesManager;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;

import javax.ejb.Singleton;
import javax.inject.Inject;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Helper class to get values for Batch Runtime. Follows
 * zero-config rules by using default values when the
 * batch-runtime config object is not present in
 * domain.xml
 *
 * @author Mahesh Kannan
 */
@Service
@Singleton
@RunLevel(value = 15)
public class BatchRuntimeHelper
    implements PostConstruct {

    @Inject
    @Optional
    BatchRuntime batchRuntimeConfiguration;

    private DatabaseConfigurationBeanProxy databaseConfigurationBeanProxy;

    private GlassFishThreadPoolConfigurationBeanProxy glassFishThreadPoolConfigurationBeanProxy;

    private JobExecutorService jobExecutorService;

    private PersistenceStore persistenceStore;

    private AtomicBoolean initialized = new AtomicBoolean(false);

    public void checkAndInitializeBatchRuntime() {
        if (!initialized.get()) {
            synchronized (this) {
                if (!initialized.get()) {
                     /*
                    Java2DBProcessorHelper java2DBProcessorHelper = new  Java2DBProcessorHelper(this.getClass().getSimpleName());
                    File ddlDir = new File(serverContext.getInstallRoot(), "/lib");

                    //Temporary fix till batch_{db_vendor}.sql is part of the distribution
                    File sqlFile = new File(ddlDir, "batch_derby.sql");
                    if (sqlFile.exists()) {
                    java2DBProcessorHelper.executeDDLStatement(ddlDir.getCanonicalPath() + CREATE_TABLE_DDL_NAME, dsName);
                    } else {
                    logger.log(Level.WARNING, sqlFile.getAbsolutePath() + " does NOT exist");
                    }
                    */
                    initialized.set(true);
                }
            }
        }
    }

    @Override
    public void postConstruct() {
        registerProxies();
    }

    public void registerProxies() {
        try {
            if (batchRuntimeConfiguration != null) {
                jobExecutorService = batchRuntimeConfiguration.getJobExecutorService();
                persistenceStore = batchRuntimeConfiguration.getPersistenceStore();
            }

            ServicesManager servicesManager = ServicesManager.getInstance();
            IBatchConfig batchConfig = servicesManager.getBatchRuntimeConfiguration();

            databaseConfigurationBeanProxy = new DatabaseConfigurationBeanProxy(this);
            batchConfig.setDatabaseConfigurationBean(databaseConfigurationBeanProxy);

            glassFishThreadPoolConfigurationBeanProxy = new GlassFishThreadPoolConfigurationBeanProxy(this);
            batchConfig.setGlassfishThreadPoolConfigurationBean(glassFishThreadPoolConfigurationBeanProxy);

        } catch (Throwable th) {
            //TODO: Log
        }
    }

    /*package*/
    int getMaxThreadPoolSize() {
        return jobExecutorService != null
                ? jobExecutorService.getMaxThreadPoolSize()
                : Integer.valueOf(JobExecutorService.MAX_THREAD_POOL_SIZE);
    }

    /*package*/
    int getMinThreadPoolSize() {
        return jobExecutorService != null
                ? jobExecutorService.getMinThreadPoolSize()
                : Integer.valueOf(JobExecutorService.MIN_THREAD_POOL_SIZE);
    }

    /*package*/
    int getMaxIdleThreadTimeout() {
        return jobExecutorService != null
                ? jobExecutorService.getMaxIdleThreadTimeoutInSeconds()
                : Integer.valueOf(JobExecutorService.MAX_IDLE_THREAD_TIMEOUT_IN_SECONDS);
    }

    /*package*/
    int getMaxQueueSize() {
        return jobExecutorService != null
                ? jobExecutorService.getMaxQueueSize()
                : Integer.valueOf(JobExecutorService.MAX_QUEUE_SIZE);
    }

    /*package*/
    String getDataSourceJndiName() {
        return persistenceStore != null
                ? persistenceStore.getDataSourceJndiName()
                : PersistenceStore.DEFAULT_DATA_SOURCE_NAME;
    }

    /*package*/
    int getMaxRetentionTime() {
        return persistenceStore != null
                ? persistenceStore.getMaxRetentionTimeInSeconds()
                : PersistenceStore.MAX_DATA_RETENTION_TIME_IN_SECONDS;
    }

    private static class DatabaseConfigurationBeanProxy
        extends DatabaseConfigurationBean {

        private BatchRuntimeHelper helper;

        DatabaseConfigurationBeanProxy(BatchRuntimeHelper helper) {
            this.helper = helper;
        }

        @Override
        public String getSchema() {
            helper.checkAndInitializeBatchRuntime();
            return "APP";
        }

        @Override
        public String getJndiName() {
            helper.checkAndInitializeBatchRuntime();
            return helper.getDataSourceJndiName();
        }
    }

    private static class GlassFishThreadPoolConfigurationBeanProxy
        extends GlassfishThreadPoolConfigurationBean {

        private BatchRuntimeHelper helper;

        GlassFishThreadPoolConfigurationBeanProxy(BatchRuntimeHelper helper) {
            this.helper = helper;
        }

        @Override
        public int getMaxQueueSize() {
            helper.checkAndInitializeBatchRuntime();
            return helper.getMaxQueueSize();
        }

        @Override
        public int getMinThreadPoolSize() {
            helper.checkAndInitializeBatchRuntime();
            return helper.getMinThreadPoolSize();
        }

        @Override
        public int getMaxThreadPoolSize() {
            helper.checkAndInitializeBatchRuntime();
            return helper.getMaxThreadPoolSize();
        }

        @Override
        public int getIdleThreadTimeout() {
            helper.checkAndInitializeBatchRuntime();
            return helper.getMaxIdleThreadTimeout();
        }
    }
}

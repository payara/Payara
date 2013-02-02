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
import org.glassfish.internal.api.ServerContext;
import org.glassfish.persistence.common.Java2DBProcessorHelper;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;

/**
 * @author Mahesh Kannan
 *
 */

@Service
@Singleton
public class GlassFishBatchRuntimeConfigurator {

    private boolean dbInitialized = false;

    private boolean threadPoolInitialized = false;


    private static final String DEFAULT_DATA_SOURCE_NAME = "jdbc/__TimerPool";

    private static final String CREATE_TABLE_DDL_NAME = "/batch_";

    @Inject
    private ServerContext serverContext;     //getInstallRoot()

    public boolean isInitialized() {
        return dbInitialized && threadPoolInitialized;
    }

    public void initializeBatchRuntime() {
        if (!dbInitialized)
            setDataSourceName(DEFAULT_DATA_SOURCE_NAME);

        if (!threadPoolInitialized)
            setThreadPoolConfiguration();
    }

    public boolean setDataSourceName(String dsName) {
        dbInitialized = false;
        try {

            ServicesManager servicesManager = ServicesManager.getInstance();
            IBatchConfig batchConfig = servicesManager.getBatchRuntimeConfiguration();
            DatabaseConfigurationBean dbBean = new DatabaseConfigurationBean();
            dbBean.setJndiName(dsName);
            batchConfig.setDatabaseConfigurationBean(dbBean);


            Java2DBProcessorHelper java2DBProcessorHelper = new  Java2DBProcessorHelper(this.getClass().getSimpleName());
            File ddlDir = new File(serverContext.getInstallRoot(), "/lib");
            java2DBProcessorHelper.executeDDLStatement(ddlDir.getCanonicalPath() + CREATE_TABLE_DDL_NAME, dsName);

            dbInitialized = true;
        } catch (Throwable th) {
            th.printStackTrace();
        }

        return dbInitialized;
    }

    public boolean setThreadPoolConfiguration() {
        threadPoolInitialized = false;
        try {
            ServicesManager servicesManager = ServicesManager.getInstance();
            IBatchConfig batchConfig = servicesManager.getBatchRuntimeConfiguration();
            GlassfishThreadPoolConfigurationBean threadPoolBean = new GlassfishThreadPoolConfigurationBean();
            threadPoolBean.setIdleThreadTimeout(600);
            threadPoolBean.setMaxQueueSize(4096);
            threadPoolBean.setMaxThreadPoolSize(16);
            threadPoolBean.setMinThreadPoolSize(2);
            batchConfig.setGlassfishThreadPoolConfigurationBean(threadPoolBean);
            threadPoolInitialized = true;
        } catch (Throwable th) {
            th.printStackTrace();
        }

        return threadPoolInitialized;
    }
}

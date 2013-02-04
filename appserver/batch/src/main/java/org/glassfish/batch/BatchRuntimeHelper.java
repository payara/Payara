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

import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.PostConstruct;
import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;

/**
 * Helper class to get values for Batch Runtime. Follows
 *  zero-config rules by using default values when the
 *  batch-runtime config object is not present in
 *  domain.xml
 *
 * @author Mahesh Kannan
 *
 */
@Service
@PerLookup
public class BatchRuntimeHelper
    implements PostConstruct {

    @Inject
    @Optional
    BatchRuntime batchRuntimeConfiguration;

    private JobExecutorService jobExecutorService;

    private PersistenceStore persistenceStore;

    @Override
    public void postConstruct() {
        if (batchRuntimeConfiguration != null) {
            jobExecutorService = batchRuntimeConfiguration.getJobExecutorService();
            persistenceStore = batchRuntimeConfiguration.getPersistenceStore();
        }
    }
    public int getVersion() {
        return batchRuntimeConfiguration != null
                ? batchRuntimeConfiguration.getVersion() : 0;
    }

    public int getMaxThreadPoolSize() {
        return jobExecutorService != null
                ? jobExecutorService.getMaxThreadPoolSize()
                : Integer.valueOf(JobExecutorService.MAX_THREAD_POOL_SIZE);
    }

    public int getMinThreadPoolSize() {
        return jobExecutorService != null
                ? jobExecutorService.getMinThreadPoolSize()
                : Integer.valueOf(JobExecutorService.MIN_THREAD_POOL_SIZE);
    }

    public int getMaxIdleThreadTimeout() {
        return jobExecutorService != null
                ? jobExecutorService.getMaxIdleThreadTimeout()
                : Integer.valueOf(JobExecutorService.MAX_IDLE_THREAD_TIMEOUT);
    }

    public int getMaxQueueSize() {
        return jobExecutorService != null
                ? jobExecutorService.getMaxQueueSize()
                : Integer.valueOf(JobExecutorService.MAX_QUEUE_SIZE);
    }

    public String getDataSourceName() {
        return persistenceStore != null
                ? persistenceStore.getDataSourceName()
                : PersistenceStore.DEFAULT_DATA_SOURCE_NAME;
    }

    public int getMaxRetentionTime() {
        return persistenceStore != null
                ? persistenceStore.getMaxRetentionTime()
                : PersistenceStore.MAX_DATA_RETENTION_TIME;
    }

}

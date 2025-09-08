/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2025 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package fish.payara.admin.cluster;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.v3.admin.adapter.AdminEndpointDecider;
import com.sun.enterprise.v3.admin.cluster.ClusterCommandHelper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.Math.min;

/**
 * ExecutorServiceFactory refactored from {@link ClusterCommandHelper}
 * to provide access to ExecutorService and its calculated fixed thread pool size
 *
 * @author Jason Tarry
 */
public class ExecutorServiceFactory {
    private static final int ADMIN_DEFAULT_POOL_SIZE = 5;
    private static final String SERVER_CONFIG = "server-config";

    private ExecutorServiceFactory() {
        // private constructor
    }

    // Holder for both executor and size
    public static class ExecutorServiceHolder {
        private final ExecutorService executorService;
        private final int poolSize;

        public ExecutorServiceHolder(ExecutorService executorService, int poolSize) {
            this.executorService = executorService;
            this.poolSize = poolSize;
        }

        public ExecutorService getExecutorService() {
            return executorService;
        }

        public int getThreadPoolSize() {
            return poolSize;
        }
    }

    private static int getAdminThreadPoolSize(final Domain domain) {
        // Get the DAS configuration
        Config config = domain.getConfigNamed(SERVER_CONFIG);
        if (config == null) {
            return ADMIN_DEFAULT_POOL_SIZE;
        }

        return new AdminEndpointDecider(config).getMaxThreadPoolSize();
    }

    private static int getThreadPoolSize(final Domain domain, int nInstances, boolean rolling) {
        int threadPoolSize = 1;
        if (!rolling) {
            // Make the thread pool use the smaller of the number of instances
            // or half the admin thread pool size
            threadPoolSize = min(nInstances, getAdminThreadPoolSize(domain) / 2);

            if (threadPoolSize < 1) {
                threadPoolSize = 1;
            }
        }
        return threadPoolSize;
    }

    public static ExecutorServiceHolder newFixedThreadPool(final Domain domain, int nInstances, boolean rolling) {
        int poolSize = getThreadPoolSize(domain, nInstances, rolling);
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);
        return new ExecutorServiceHolder(executor, poolSize);
    }
}

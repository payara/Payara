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

import com.sun.enterprise.util.ColumnFormatter;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.*;

/**
 * Command to list batch jobs info
 *
 * @author Mahesh Kannan
 *
 */
@Service(name="list-batch-runtime-configuration")
@PerLookup
public class ListBatchRuntimeConfiguration
    extends AbstractListCommand {

    private static final String MAX_THREAD_POOL_SIZE = "max-thread-pool-size";

    private static final String MIN_THREAD_POOL_SIZE = "min-thread-pool-size";

    private static final String MAX_IDLE_THREAD_TIMEOUT = "max-idle-thread-timeout";

    private static final String MAX_QUEUE_SIZE = "max-queue-size";

    private static final String DATA_SOURCE_NAME = "data-source-name";

    private static final String MAX_DATA_RETENTION_TIME = "max-data-retention-time";

    @Inject
    BatchRuntimeHelper helper;

    @Override
    protected void executeCommand(AdminCommandContext context, Properties extraProps) {

        Map<String, Object> map = new HashMap<String, Object>();

        map.put(MAX_THREAD_POOL_SIZE, helper.getMaxThreadPoolSize());
        map.put(MIN_THREAD_POOL_SIZE, helper.getMinThreadPoolSize());
        map.put(MAX_IDLE_THREAD_TIMEOUT, helper.getMaxIdleThreadTimeout());
        map.put(MAX_QUEUE_SIZE, helper.getMaxQueueSize());
        map.put(DATA_SOURCE_NAME, helper.getDataSourceName());
        map.put(MAX_DATA_RETENTION_TIME, helper.getMaxRetentionTime());
        extraProps.put("list-batch-runtime-configuration", map);

        ColumnFormatter columnFormatter = new ColumnFormatter(getDisplayHeaders());
        Object[] data = new Object[getOutputHeaders().length];
        for (int index=0; index<getOutputHeaders().length; index++) {
            switch (getOutputHeaders()[index]) {
                case MAX_THREAD_POOL_SIZE:
                    data[index] = helper.getMaxThreadPoolSize();
                    break;
                case MIN_THREAD_POOL_SIZE:
                    data[index] = helper.getMinThreadPoolSize();
                    break;
                case MAX_IDLE_THREAD_TIMEOUT:
                    data[index] = helper.getMaxIdleThreadTimeout();
                    break;
                case MAX_QUEUE_SIZE:
                    data[index] = helper.getMaxQueueSize();
                    break;
                case DATA_SOURCE_NAME:
                    data[index] = helper.getDataSourceName();
                    break;
                case MAX_DATA_RETENTION_TIME:
                    data[index] = helper.getMaxRetentionTime();
                    break;
                default:
                    throw new IllegalArgumentException("Unknown header: " + getOutputHeaders()[index]);
            }
        }
        columnFormatter.addRow(data);
        context.getActionReport().setMessage(columnFormatter.toString());
    }

    @Override
    protected final String[] getSupportedHeaders() {
        return new String[] {
                MAX_THREAD_POOL_SIZE, MIN_THREAD_POOL_SIZE,
                MAX_IDLE_THREAD_TIMEOUT, MAX_QUEUE_SIZE,
                DATA_SOURCE_NAME, MAX_DATA_RETENTION_TIME
        };
    }

    @Override
    protected final String[] getTerseHeaders() {
        return getSupportedHeaders();
    }

}

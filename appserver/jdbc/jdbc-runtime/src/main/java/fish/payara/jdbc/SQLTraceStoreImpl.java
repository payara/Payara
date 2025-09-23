/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2020-2025 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.jdbc;

import static java.lang.Double.parseDouble;
import static java.lang.Math.round;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.glassfish.api.jdbc.SQLTraceRecord;
import org.glassfish.api.jdbc.SQLTraceStore;
import org.glassfish.jdbc.config.JdbcConnectionPool;
import org.jvnet.hk2.annotations.Service;

import com.sun.appserv.connectors.internal.api.ConnectorsUtil;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.ResourcePool;

@Service
@Singleton
public class SQLTraceStoreImpl implements SQLTraceStore {

    private static final class SQLTraceEntry {
        final long thresholdMillis;
        final SQLTraceRecord trace;
        final String sql;

        SQLTraceEntry(long thresholdMillis, SQLTraceRecord trace, String sql) {
            this.thresholdMillis = thresholdMillis;
            this.trace = trace;
            this.sql = sql;
        }
    }

    @Inject
    private Domain domain;

    private final Map<String, JdbcConnectionPool> connectionPoolByName = new ConcurrentHashMap<>();
    private final Map<String, Queue<SQLTraceEntry>> uncollectedTracesByPoolName = new ConcurrentHashMap<>();

    @Override
    public void trace(SQLTraceRecord record, String sql) {
        JdbcConnectionPool connectionPool = getJdbcConnectionPool(record.getPoolName());
        if (connectionPool == null) {
            return;
        }
        long threshold = thresholdInMillis(connectionPool);
        Queue<SQLTraceEntry> queue = uncollectedTracesByPoolName.computeIfAbsent(record.getPoolName(), 
                key -> new ConcurrentLinkedQueue<>());
        if (queue.size() >= 50) {
            queue.poll(); // avoid queue creating a memory leak by accumulating entries in case no consumer polls them
        }
        queue.add(new SQLTraceEntry(threshold, record, sql));
    }

    private static long thresholdInMillis(JdbcConnectionPool pool) {
        String thresholdInSeconds = pool.getSlowQueryThresholdInSeconds();
        if (thresholdInSeconds == null || thresholdInSeconds.isEmpty()) {
            return -1;
        }
        return round(parseDouble(pool.getSlowQueryThresholdInSeconds()) * 1000);
    }

    private JdbcConnectionPool getJdbcConnectionPool(String poolName) {
        return connectionPoolByName.computeIfAbsent(poolName, name -> {
            ResourcePool pool = (ResourcePool) ConnectorsUtil.getResourceByName(
                    domain.getResources(), ResourcePool.class, poolName);
            return pool instanceof JdbcConnectionPool ? (JdbcConnectionPool) pool : null;
        });
    }
}

/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016-2020 Payara Foundation and/or its affiliates. All rights reserved.
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

import org.glassfish.api.jdbc.SQLTraceListener;
import org.glassfish.api.jdbc.SQLTraceRecord;
import org.glassfish.jdbc.config.JdbcConnectionPool;

/**
 * Adapter to {@link SQLTraceListener} abstraction that maps the calls to {@link #sqlTrace(SQLTraceRecord)} during SQL
 * processing to a single call to {@link #onThresholdExceeded(long, long, SQLTraceRecord, SQLQuery)} in case a query
 * exceeds the pool threshold.
 *
 * @author steve (initial)
 * @author Jan Bernitt (extracted)
 */
abstract class SlowSQLTraceListener implements SQLTraceListener {

    private static ThreadLocal<SQLQuery> currentQuery = new ThreadLocal<>();

    private final JdbcConnectionPool connectionPool;

    public SlowSQLTraceListener(JdbcConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    /**
     * Called once for a SQL query when it's execution time exceeds the threshold.
     * 
     * @param thresholdTime
     * @param record
     * @param sql
     */
    abstract void onSlowExecutionTime(long thresholdTime, SQLTraceRecord record, String sql);

    private long thresholdMillis() {
        return Math.round(Double.parseDouble(connectionPool.getSlowQueryThresholdInSeconds()) * 1000d);
    }

    @Override
    public void sqlTrace(SQLTraceRecord record) {
        if (record != null) {    
            switch (record.getMethodName()) {

            // these calls capture a query string
            case "nativeSQL":
            case "prepareCall":
            case "prepareStatement":
            case "addBatch":
            {
                // acquire the SQL
                SQLQuery query = currentQuery.get();
                if (query == null) {
                    query = new SQLQuery();
                    currentQuery.set(query);
                }  
                if (record.getParams() != null && record.getParams().length > 0)
                    query.addSQL((String)record.getParams()[0]);
                break;
            }
            case "execute":
            case "executeQuery":
            case "executeUpdate":
            {
                // acquire the SQL
                SQLQuery query = currentQuery.get();
                if (query == null) {
                    query = new SQLQuery();
                    currentQuery.set(query);
                }                      // these can all run the SQL and contain SQL
                long executionTime = record.getExecutionTime();
                // see if we have more SQL
                if (record.getParams() != null && record.getParams().length > 0) {
                    // gather the SQL
                    query.addSQL((String) record.getParams()[0]);
                }

                // check the execution time
                long threshold = thresholdMillis();
                if (threshold > 0 && executionTime > threshold) {
                    onSlowExecutionTime(threshold, record, query.getSQL());
                }
                // clean the thread local
                currentQuery.set(null);
                break;
            }
            }
        }
    }

}

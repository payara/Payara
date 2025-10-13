/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2020 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
import org.glassfish.api.jdbc.SQLTraceStore;
import org.glassfish.internal.api.Globals;

import com.sun.gjc.util.SQLTraceLogger;

/**
 * An adapter between the {@link SQLTraceListener} abstraction that is registered with implementation class as key and a
 * managed instance of the {@link SQLTraceStore}.
 * 
 * Even though this class is very similar to {@link SQLTraceLogger} the classes cannot share code as each has to have
 * its own {@link ThreadLocal}. Would they inherit from the same base class the invocations from both classes would get
 * mixed up in the same {@link ThreadLocal}.
 * 
 * @author Jan Bernitt
 */
public class SQLTraceStoreAdapter implements SQLTraceListener {

    private static ThreadLocal<SQLQuery> currentQuery = new ThreadLocal<>();

    private final SQLTraceStore store;

    public SQLTraceStoreAdapter() {
        this.store = Globals.getDefaultHabitat().getService(SQLTraceStore.class);
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
                aquireSQL(record);
                break;
            // these can all run the SQL and contain SQL
            case "execute":
            case "executeQuery":
            case "executeUpdate":
                trace(record);
                break;
            default:
                // nothing
            }
        }
    }

    private void trace(SQLTraceRecord record) {
        SQLQuery query = aquireSQL(record);
        if (store != null) {
            store.trace(record, query.getSQL());
        }
        // clean the thread local
        currentQuery.set(null);
    }

    private static SQLQuery aquireSQL(SQLTraceRecord record) {
        // acquire the SQL
        SQLQuery query = currentQuery.get();
        if (query == null) {
            query = new SQLQuery();
            currentQuery.set(query);
        }
        // see if we have more SQL
        if (record.getParams() != null && record.getParams().length > 0) {
            // gather the SQL
            query.addSQL((String)record.getParams()[0]);
        }
        return query;
    }

}

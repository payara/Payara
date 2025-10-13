/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016-2018 Payara Foundation and/or its affiliates. All rights reserved.
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

import com.sun.gjc.util.SQLTraceLogger;
import com.sun.logging.LogDomains;
import fish.payara.nucleus.requesttracing.RequestTracingService;
import fish.payara.notification.requesttracing.RequestTraceSpanLog;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.api.jdbc.SQLTraceListener;
import org.glassfish.api.jdbc.SQLTraceRecord;
import org.glassfish.internal.api.Globals;

/**
 * A SQLTraceListener for the Request Tracing service.
 * @author Andrew Pielage
 */
public class RequestTracingListener implements SQLTraceListener {

    private RequestTracingService requestTracing;
    
    private static final Logger logger = 
            LogDomains.getLogger(SQLTraceLogger.class, 
                    LogDomains.SQL_TRACE_LOGGER);
    
    public RequestTracingListener() {
        try {
            requestTracing = Globals.getDefaultHabitat().getService(
                    RequestTracingService.class);
        } catch (NullPointerException ex) {
            logger.log(Level.INFO, "Error retrieving Request Tracing service "
                    + "during initialisation of RequestTracingListener - "
                    + "NullPointerException");
        }
    }
    
    @Override
    public void sqlTrace(SQLTraceRecord record) {
        // Construct request event and trace
        RequestTraceSpanLog spanLog = constructJDBCSpanLog(record);
        if (requestTracing != null) {
            requestTracing.addSpanLog(spanLog);
        } 
    }
    
    /**
     * Constructs a Request tracing event for the JDBC event
     * @param record The SQL record to log
     * @return RequestEvent to be traced
     */
    private RequestTraceSpanLog constructJDBCSpanLog(SQLTraceRecord record) {
        RequestTraceSpanLog spanLog = new RequestTraceSpanLog("jdbcContextEvent");
        
        spanLog.addLogEntry("Method Name", record.getMethodName());
        spanLog.addLogEntry("Parameters", Arrays.toString(record.getParams()));
        spanLog.addLogEntry("Pool Name", record.getPoolName());
        spanLog.addLogEntry("Thread ID", Long.toString(record.getThreadID()));
        spanLog.addLogEntry("Thread Name", record.getThreadName());
        spanLog.addLogEntry("Execution Time", Long.toString(record.getExecutionTime()));
             
        return spanLog;
    }
}

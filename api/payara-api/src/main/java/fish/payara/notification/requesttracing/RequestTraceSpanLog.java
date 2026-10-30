/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017-2018 Payara Foundation and/or its affiliates. All rights reserved.
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
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
package fish.payara.notification.requesttracing;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author Andrew Pielage
 */
public class RequestTraceSpanLog implements Serializable {
    
    private final long timeMillis;
    private final Map<String, String> logEntries;
    
    public RequestTraceSpanLog() {
        timeMillis = System.currentTimeMillis();
        logEntries = new LinkedHashMap<>();
    }
    
    public RequestTraceSpanLog(long timestampMillis) {
        timeMillis = timestampMillis;
        logEntries = new LinkedHashMap<>();
    }
    
    public RequestTraceSpanLog(String logName) {
        timeMillis = System.currentTimeMillis();
        logEntries = new LinkedHashMap<>();
        logEntries.put("logEvent", logName);
    }
    
    public RequestTraceSpanLog(long timestampMillis, String logName) {
        timeMillis = timestampMillis;
        logEntries = new LinkedHashMap<>();
        logEntries.put("logEvent", logName);
    }
    
    public long getTimeMillis() {
        return timeMillis;
    }
    
    public Map<String, String> getLogEntries() {
        return logEntries;
    }
    
    public void addLogEntry(String entry, String value) {
        if (value != null) {
            logEntries.put(entry, value.replaceAll("\"", "\\\""));
        } else {
            logEntries.put(entry, value);
        }       
    }
}

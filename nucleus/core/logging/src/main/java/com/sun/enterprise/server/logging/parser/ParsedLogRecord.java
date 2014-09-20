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
package com.sun.enterprise.server.logging.parser;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.sun.enterprise.server.logging.LogEvent;

/**
 * 
 * @author sanshriv
 *
 */
public final class ParsedLogRecord implements LogEvent {
        
    public static final String DATE_TIME = "timestamp";
    public static final String LOG_LEVEL_NAME = "level";
    public static final String PRODUCT_ID = "productId";
    public static final String LOGGER_NAME = "logger";
    public static final String THREAD_ID = "threadId";
    public static final String THREAD_NAME = "threadName";
    public static final String USER_ID = "user";
    public static final String EC_ID = "ecid";
    public static final String TIME_MILLIS = "timeMillis";
    public static final String LOG_LEVEL_VALUE = "levelValue";
    public static final String LOG_MESSAGE = "message";
    public static final String SUPP_ATTRS = "suppAttrs";
    public static final String MESSAGE_ID = "msgId";
    
    public static final Set<String> FIELD_NAMES = new HashSet<String>() {
        
        private static final long serialVersionUID = 1L;
        
        {
           add(DATE_TIME);
           add(LOG_LEVEL_NAME);
           add(PRODUCT_ID);
           add(LOGGER_NAME);
           add(THREAD_ID);
           add(THREAD_NAME);
           add(USER_ID);
           add(EC_ID);
           add(TIME_MILLIS);
           add(LOG_LEVEL_VALUE);
           add(SUPP_ATTRS);
           add(LOG_MESSAGE);
           add(MESSAGE_ID);
        }
        
    };
    
    private String formattedLogRecord;
    
    private boolean matchedLogQuery;
    
    private Map<String, Object> fields = new HashMap<String,Object>();
    
    public ParsedLogRecord() {
        fields.put(SUPP_ATTRS, new Properties());
    }
    
    public ParsedLogRecord(String formattedContent) {
        formattedLogRecord = formattedContent;
        fields.put(SUPP_ATTRS, new Properties());
    }

    public String getTimestamp() {
        return (String) fields.get(DATE_TIME);
    }

    public String getMessage() {
        return (String) fields.get(LOG_MESSAGE);
    }
    
    public String getLevel() {
        return (String) fields.get(LOG_LEVEL_NAME);
    }    

    public String getLogger() {
        return (String) fields.get(LOGGER_NAME);
    }    

    public int getLevelValue() {
        String val = (String) fields.get(LOG_LEVEL_VALUE); 
        return Integer.parseInt(val) ;
    }

    public String getComponentId() {
        return (String) fields.get(PRODUCT_ID);
    }

    public long getTimeMillis() {
        String val = (String) fields.get(TIME_MILLIS);
        if (val == null || val.isEmpty()) {
            return 0L;
        } else {
            return Long.parseLong(val) ;
        }
    }
    
    public String getMessageId() {
        return (String) fields.get(MESSAGE_ID);
    }

    public long getThreadId() {
        String val = (String) fields.get(THREAD_ID);
        return Long.parseLong(val) ;
    }

    public String getThreadName() {
        return (String) fields.get(THREAD_NAME);
    }

    public String getUser() {
        return (String) fields.get(USER_ID);
    }

    public String getECId() {
        return (String) fields.get(EC_ID);
    }

    public Map<String,Object> getSupplementalAttributes() {
        return (Map<String,Object>) fields.get(SUPP_ATTRS);
    }

    public String getFormattedLogRecord() {
        return formattedLogRecord;
    }

    /**
     * @return the matchedLogQuery
     */
    public boolean isMatchedLogQuery() {
        return matchedLogQuery;
    }
    
    /**
     * 
     * @param name
     * @return
     */
    public Object getFieldValue(String name) {
        return fields.get(name);
    }
    
    /**
     * @param formattedLogRecord the formattedLogRecord to set
     */
    void setFormattedLogRecord(String formattedLogRecord) {
        this.formattedLogRecord = formattedLogRecord;
    }

    /**
     * @param matchedLogQuery the matchedLogQuery to set
     */
    void setMatchedLogQuery(boolean matchedLogQuery) {
        this.matchedLogQuery = matchedLogQuery;
    }
    
    /**
     * 
     * @param name
     * @param value
     */
    void setFieldValue(String name, Object value) {
        fields.put(name, value);
    }    
    
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("Log record: <"+fields + ">" + LogParserFactory.NEWLINE);
        return buffer.toString();
    }

}

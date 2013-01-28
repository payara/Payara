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

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;

import com.sun.enterprise.server.logging.LogFacade;
import com.sun.enterprise.util.LocalStringManagerImpl;

final class UniformLogParser implements LogParser {

    final private static LocalStringManagerImpl LOCAL_STRINGS = 
        new LocalStringManagerImpl(UniformLogParser.class);

    static final String FIELD_SEPARATOR = "\\|";

    static final String LOG_RECORD_BEGIN_MARKER = "[#|";
    
    static final String LOG_RECORD_END_MARKER = "|#]";

    private static final int ULF_FIELD_COUNT = 6;
    
    private static final Map<String,String> FIELD_NAME_ALIASES = 
        new HashMap<String,String>() 
    {
        private static final long serialVersionUID = -2041470292369513712L;
        {
            put("_ThreadID", ParsedLogRecord.THREAD_ID);
            // put("_ThreadName", "threadName");
            put("_TimeMillis", ParsedLogRecord.TIME_MILLIS);
            put("_LevelValue", ParsedLogRecord.LOG_LEVEL_VALUE);
            put("_UserID", ParsedLogRecord.USER_ID);
            put("_ECID", ParsedLogRecord.EC_ID);
            put("_MessageID",ParsedLogRecord.MESSAGE_ID);
            // put("ClassName", "className");
            // put("MethodName", "methodName");
        }
    };

    private String streamName;
    
    public UniformLogParser(String name) {
        streamName = name;
    }
    
    @Override
    public void parseLog(BufferedReader reader, LogParserListener listener)
            throws LogParserException
    {
        
        try {
            String line = null;
            StringBuffer buffer = new StringBuffer();
            long position = 0L;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(LOG_RECORD_BEGIN_MARKER)) {
                    // Construct a parsed log record from the prior content
                    String logRecord = buffer.toString();
                    parseLogRecord(position, logRecord, listener);
                    position += logRecord.length();
                    buffer = new StringBuffer();                    
                }
                buffer.append(line);
                buffer.append(LogParserFactory.NEWLINE);
            }
            // Last record
            String logRecord = buffer.toString();
            parseLogRecord(position, logRecord, listener);            
        } catch(IOException e){
            throw new LogParserException(e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    LogFacade.LOGGING_LOGGER.log(Level.FINE, "Got exception while clsoing reader "+ streamName, e); 
                }
            }
        }                
    }
    
    private void parseLogRecord(long position, String logRecord, LogParserListener listener) {
        ParsedLogRecord parsedLogRecord = new ParsedLogRecord();
        if (initializeUniformFormatLogRecord(parsedLogRecord, logRecord)) {
            listener.foundLogRecord(position, parsedLogRecord);
        }        
    }

    private boolean initializeUniformFormatLogRecord(
            ParsedLogRecord parsedLogRecord, 
            String logRecord) 
    {
        parsedLogRecord.setFormattedLogRecord(logRecord);
                
        int beginIndex = logRecord.indexOf(LOG_RECORD_BEGIN_MARKER);
        if (beginIndex < 0) {
            return false;    
        }
        int endIndex = logRecord.lastIndexOf(LOG_RECORD_END_MARKER);
        if (endIndex < 0) {
            return false;
        }
        
        if (logRecord.length() < 
                (LOG_RECORD_BEGIN_MARKER.length() + LOG_RECORD_END_MARKER.length())) 
        {
            return false;
        }
        
        String logData = logRecord.substring(
                beginIndex + LOG_RECORD_BEGIN_MARKER.length(), endIndex);
        
        String[] fieldValues = logData.split(FIELD_SEPARATOR);        
        if (fieldValues.length < ULF_FIELD_COUNT) {
            String msg = LOCAL_STRINGS.getLocalString(
                    "parser.illegal.ulf.record", "Illegal Uniform format log record {0} found", logRecord);
            throw new IllegalArgumentException(msg);
        }
        
        for (int i=0; i < ULF_FIELD_COUNT; i++) {
           populateLogRecordFields(i, fieldValues[i], parsedLogRecord);    
        }
        
        if (fieldValues.length > ULF_FIELD_COUNT) {
            StringBuffer buf = new StringBuffer();
            buf.append(parsedLogRecord.getFieldValue(ParsedLogRecord.LOG_MESSAGE));
            for (int i=ULF_FIELD_COUNT; i<fieldValues.length; i++) {
                buf.append("|");
                buf.append(fieldValues[i]);
            }
            parsedLogRecord.setFieldValue(ParsedLogRecord.LOG_MESSAGE, buf.toString());
        }
        return true;
    }

    private void populateLogRecordFields(int index, String fieldData, 
            ParsedLogRecord parsedLogRecord) 
    {
        switch(index) {
        case 0:
            parsedLogRecord.setFieldValue(ParsedLogRecord.DATE_TIME, fieldData);
            break;
        case 1:
            parsedLogRecord.setFieldValue(ParsedLogRecord.LOG_LEVEL_NAME, fieldData);
            break;
        case 2:
            parsedLogRecord.setFieldValue(ParsedLogRecord.PRODUCT_ID, fieldData);
            break;
        case 3:
            parsedLogRecord.setFieldValue(ParsedLogRecord.LOGGER_NAME, fieldData);
            break;            
        case 4:
            String[] nv_pairs = fieldData.split(";");
            for (String pair : nv_pairs) {
                String[] nameValue = pair.split("=");
                if (nameValue.length == 2) {
                    String name = nameValue[0];
                    String value = nameValue[1];
                    if (FIELD_NAME_ALIASES.containsKey(name)) {
                        name = FIELD_NAME_ALIASES.get(name);
                        parsedLogRecord.setFieldValue(name, value);
                    } else {
                        Properties props = (Properties) parsedLogRecord.getFieldValue(ParsedLogRecord.SUPP_ATTRS);
                        props.put(name, value);
                    }
                }
            }            
            break;
        case 5:
            parsedLogRecord.setFieldValue(ParsedLogRecord.LOG_MESSAGE, fieldData);
            break;
        default:
            break;
        }        
    }
    
 }

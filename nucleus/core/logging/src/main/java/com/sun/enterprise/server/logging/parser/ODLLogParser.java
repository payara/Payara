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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.enterprise.server.logging.LogFacade;

/**
 * @author sanshriv
 *
 */
final class ODLLogParser implements LogParser {

    private static final int ODL_FIXED_FIELD_COUNT = 5;
    
    private static final String ODL_FIELD_REGEX = "(\\[[^\\[\\]]*?\\])+?";
        
    private static final class ODLFieldPatternHolder {
        static final Pattern ODL_FIELD_PATTERN = Pattern.compile(ODL_FIELD_REGEX);
    }
    
    private static final Map<String, String> ODL_STANDARD_FIELDS = new HashMap<String, String>(){
        
        private static final long serialVersionUID = -6870456038890663569L;

        {
            put("tid", ParsedLogRecord.THREAD_ID);
            put(ParsedLogRecord.EC_ID, ParsedLogRecord.EC_ID);
            put(ParsedLogRecord.USER_ID, ParsedLogRecord.USER_ID);
        }
    }; 
    
    private String streamName;
    
    public ODLLogParser(String name) {
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
                Matcher m = LogParserFactory.getInstance().getODLDateFormatPattern().matcher(line);
                if (m.matches()) {
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
        Matcher matcher = ODLFieldPatternHolder.ODL_FIELD_PATTERN.matcher(logRecord);
        int start=0;
        int end=0;
        int fieldIndex=0;        
        while (matcher.find()) {
            fieldIndex++;
            start = matcher.start();
            if (end != 0 && start != end+1) {
                break;
            }
            end = matcher.end();
            String text = matcher.group();
            text = text.substring(1, text.length()-1);
            if (fieldIndex <= ODL_FIXED_FIELD_COUNT) {
                populateLogRecordFields(fieldIndex, text, parsedLogRecord);
            } else {
                populateLogRecordSuppAttrs(text, parsedLogRecord);
            }
        }
        String msg = logRecord.substring(end);
        // System.out.println("Indexof=" + msg.indexOf("[["));
        msg = msg.trim();
        boolean multiLineBegin = false;
        if (msg.startsWith("[[")) {
            msg = msg.replaceFirst("\\[\\[", "").trim();
            multiLineBegin = true;
            multiLineBegin = true;
        }
        if (multiLineBegin && msg.endsWith("]]")) {
            int endIndex = msg.length() - 2;
            if (endIndex > 0) {
                msg = msg.substring(0, endIndex);
            }
        }
        parsedLogRecord.setFieldValue(ParsedLogRecord.LOG_MESSAGE, msg);
        if (fieldIndex < ODL_FIXED_FIELD_COUNT) {
            return false;
        }        
        return true;
    }

    private void populateLogRecordSuppAttrs(String text,
            ParsedLogRecord parsedLogRecord) {
        int index = text.indexOf(':');
        if (index > 0) {
            String key = text.substring(0, index);
            String value = text.substring(index+1);
            value = value.trim();
            if (ODL_STANDARD_FIELDS.containsKey(key)) {
                parsedLogRecord.setFieldValue(ODL_STANDARD_FIELDS.get(key), value);
            } else {
                Properties props = (Properties) parsedLogRecord.getFieldValue(ParsedLogRecord.SUPP_ATTRS);
                props.put(key, value);               
                if (key.equals(ParsedLogRecord.TIME_MILLIS)) {
                    parsedLogRecord.setFieldValue(ParsedLogRecord.TIME_MILLIS, value);
                }
            }
        }
    }

    private void populateLogRecordFields(int index, String fieldData, 
            ParsedLogRecord parsedLogRecord) 
    {
        switch(index) {
        case 1:
            parsedLogRecord.setFieldValue(ParsedLogRecord.DATE_TIME, fieldData);
            break;
        case 2:
            parsedLogRecord.setFieldValue(ParsedLogRecord.PRODUCT_ID, fieldData);
            break;
        case 3:
            parsedLogRecord.setFieldValue(ParsedLogRecord.LOG_LEVEL_NAME, fieldData);
            break;            
        case 4:
            parsedLogRecord.setFieldValue(ParsedLogRecord.MESSAGE_ID, fieldData);
            break;            
        case 5:
            parsedLogRecord.setFieldValue(ParsedLogRecord.LOGGER_NAME, fieldData);
            break;
        default:
            break;
        }        
    }
    
 }

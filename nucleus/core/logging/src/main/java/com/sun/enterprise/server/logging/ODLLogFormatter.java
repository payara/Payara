/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.server.logging;


import com.sun.appserv.server.util.Version;
import org.jvnet.hk2.annotations.ContractsProvided;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.*;
import java.util.logging.Formatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ODLLogFormatter conforms to the logging format defined by the
 * Log Working Group in Oracle.
 * The specified format is
 * "[[timestamp] [organization ID] [Message Type/Level] [Message ID] [Logger
 * Name] [Thread ID] [User ID] [ECID] [Extra Attributes] [Message]]\n"
 *
 * @author Naman Mehta
 */
@Service()
@ContractsProvided({ODLLogFormatter.class, Formatter.class})
@PerLookup
public class ODLLogFormatter extends Formatter {

    private static final String AS_COMPONENT_NAME = "AS";

    // loggerResourceBundleTable caches references to all the ResourceBundle
    // and can be searched using the LoggerName as the key 
    private HashMap loggerResourceBundleTable;

    private LogManager logManager;

    private static boolean LOG_SOURCE_IN_KEY_VALUE = false;

    private static boolean RECORD_NUMBER_IN_KEY_VALUE = false;

    private static String userID = "";

    private static String ecID = "";

    private FormatterDelegate _delegate = null;

    private static final String LINE_SEPARATOR =
        (String) java.security.AccessController.doPrivileged(
                new sun.security.action.GetPropertyAction("line.separator"));

    static {
        String logSource = System.getProperty(
                "com.sun.aas.logging.keyvalue.logsource");
        if ((logSource != null)
                && (logSource.equals("true"))) {
            LOG_SOURCE_IN_KEY_VALUE = true;
        }

        String recordCount = System.getProperty(
                "com.sun.aas.logging.keyvalue.recordnumber");
        if ((recordCount != null)
                && (recordCount.equals("true"))) {
            RECORD_NUMBER_IN_KEY_VALUE = true;
        }

        userID = System.getProperty("com.sun.aas.logging.userID");

        ecID = System.getProperty("com.sun.aas.logging.ecID");        
    }

    private long recordNumber = 0;

    private String recordFieldSeparator;
    private String recordDateFormat;

    private BitSet includeSuppAttrsBits = new BitSet();

    private static final String FIELD_BEGIN_MARKER = "[";
    private static final String FIELD_END_MARKER = "]";

    private static final char FIELD_SEPARATOR = ' ';

    private static final String RFC_3339_DATE_FORMAT =
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    private static enum SupplementalAttribute {TID, USERID, ECID, TIME_MILLIS, LEVEL_VALUE, MAX_VALUE};
    
    public ODLLogFormatter() {
        super();
        loggerResourceBundleTable = new HashMap();
        logManager = LogManager.getLogManager();
    }

    public ODLLogFormatter(FormatterDelegate delegate) {
        this();
        _delegate = delegate;
    }


    public void setDelegate(FormatterDelegate delegate) {
        _delegate = delegate;
    }

    /**
     * _REVISIT_: Replace the String Array with an HashMap and do some
     * benchmark to determine whether StringCat is faster or Hashlookup for
     * the template is faster.
     */


    @Override
    public String format(LogRecord record) {
        return odlLogFormat(record);
    }

    @Override
    public String formatMessage(LogRecord record) {
        return odlLogFormat(record);
    }


    /**
     * GlassFish can override to specify their product version
     */
    protected String getProductId() {

        String version = Version.getAbbreviatedVersion() + Version.getVersionPrefix() +
                Version.getMajorVersion() + "." + Version.getMinorVersion();
        return (version);
    }


    /**
     * Note: This method is not synchronized, we are assuming that the
     * synchronization will happen at the Log Handler.publish( ) method.
     */
    private String odlLogFormat(LogRecord record) {

        try {

            // creating message from log record using resource bundle and appending parameters
            String message = getLogMessage(record);
            boolean multiLine = isMultiLine(message);

            // Starting formatting message
            // Adding record begin marker
            StringBuilder recordBuffer = new StringBuilder();

            // A Dummy Container Date Object is used to format the date
            Date date = new Date();

            // Adding timestamp
            SimpleDateFormat dateFormatter = new SimpleDateFormat(getRecordDateFormat() != null ? getRecordDateFormat() : RFC_3339_DATE_FORMAT);
            date.setTime(record.getMillis());
            recordBuffer.append(FIELD_BEGIN_MARKER);
            recordBuffer.append(dateFormatter.format(date));
            recordBuffer.append(FIELD_END_MARKER);
            recordBuffer.append(getRecordFieldSeparator() != null ? getRecordFieldSeparator() : FIELD_SEPARATOR);

            // Adding organization ID
            recordBuffer.append(FIELD_BEGIN_MARKER);
            recordBuffer.append(AS_COMPONENT_NAME);
            recordBuffer.append(FIELD_END_MARKER);
            recordBuffer.append(getRecordFieldSeparator() != null ? getRecordFieldSeparator() : FIELD_SEPARATOR);

            // Adding messageType
            Level logRecord = record.getLevel();
            recordBuffer.append(FIELD_BEGIN_MARKER);
            recordBuffer.append(getMapplingLogRecord(logRecord));
            recordBuffer.append(FIELD_END_MARKER);
            recordBuffer.append(getRecordFieldSeparator() != null ? getRecordFieldSeparator() : FIELD_SEPARATOR);

            // Adding message ID
            recordBuffer.append(FIELD_BEGIN_MARKER);
            String msgId  = UniformLogFormatter.getMessageId(record);            
            recordBuffer.append((msgId == null) ? "" : msgId);
            recordBuffer.append(FIELD_END_MARKER);
            recordBuffer.append(getRecordFieldSeparator() != null ? getRecordFieldSeparator() : FIELD_SEPARATOR);

            // Adding logger Name / module Name
            recordBuffer.append(FIELD_BEGIN_MARKER);
            recordBuffer.append(record.getLoggerName());
            recordBuffer.append(FIELD_END_MARKER);
            recordBuffer.append(getRecordFieldSeparator() != null ? getRecordFieldSeparator() : FIELD_SEPARATOR);

            // Adding thread ID
            if (includeSuppAttrsBits.get(SupplementalAttribute.TID.ordinal())) {
                recordBuffer.append(FIELD_BEGIN_MARKER);
                recordBuffer.append("tid: _ThreadID=");
                recordBuffer.append(record.getThreadID());
                String threadName;
                if (record instanceof GFLogRecord) {
                    threadName = ((GFLogRecord)record).getThreadName();
                } else {
                    threadName = Thread.currentThread().getName();
                }
                recordBuffer.append(" _ThreadName=");
                recordBuffer.append(threadName);
                recordBuffer.append(FIELD_END_MARKER);
                recordBuffer.append(getRecordFieldSeparator() != null ? getRecordFieldSeparator() : FIELD_SEPARATOR);                
            }

            // Adding user ID
            if (includeSuppAttrsBits.get(SupplementalAttribute.USERID.ordinal()) && 
                    userID != null && !("").equals(userID.trim())) 
            {
                recordBuffer.append(FIELD_BEGIN_MARKER);
                recordBuffer.append("userId: ");
                recordBuffer.append(userID);
                recordBuffer.append(FIELD_END_MARKER);
                recordBuffer.append(getRecordFieldSeparator() != null ? getRecordFieldSeparator() : FIELD_SEPARATOR);
            }

            // Adding ec ID
            if (includeSuppAttrsBits.get(SupplementalAttribute.ECID.ordinal()) && 
                    ecID != null && !("").equals(ecID.trim())) 
            {
                recordBuffer.append(FIELD_BEGIN_MARKER);
                recordBuffer.append("ecid: ");
                recordBuffer.append(ecID);
                recordBuffer.append(FIELD_END_MARKER);
                recordBuffer.append(getRecordFieldSeparator() != null ? getRecordFieldSeparator() : FIELD_SEPARATOR);
            }
            
            // Include the raw time stamp   
            if (includeSuppAttrsBits.get(SupplementalAttribute.TIME_MILLIS.ordinal())) {
                recordBuffer.append(FIELD_BEGIN_MARKER);
                recordBuffer.append("timeMillis: ");
                recordBuffer.append(record.getMillis());
                recordBuffer.append(FIELD_END_MARKER);
                recordBuffer.append(getRecordFieldSeparator() != null ? getRecordFieldSeparator() : FIELD_SEPARATOR);                
            }

            // Include the level value
            if (includeSuppAttrsBits.get(SupplementalAttribute.LEVEL_VALUE.ordinal())) {
                recordBuffer.append(FIELD_BEGIN_MARKER);
                recordBuffer.append("levelValue: ");
                recordBuffer.append(record.getLevel().intValue());
                recordBuffer.append(FIELD_END_MARKER);
                recordBuffer.append(getRecordFieldSeparator() != null ? getRecordFieldSeparator() : FIELD_SEPARATOR);                
            }

            // Adding extra Attributes - record number
            if (RECORD_NUMBER_IN_KEY_VALUE) {
                recordBuffer.append(FIELD_BEGIN_MARKER);
                recordBuffer.append("RECORDNUMBER: ");
                recordBuffer.append(recordNumber++);
                recordBuffer.append(FIELD_END_MARKER);
                recordBuffer.append(getRecordFieldSeparator() != null ? getRecordFieldSeparator() : FIELD_SEPARATOR);
            }

            // Adding extra Attributes - produce ID
            /*
            if (getProductId()!=null) {
                recordBuffer.append(FIELD_BEGIN_MARKER);
                recordBuffer.append("PRODUCTID: ");
                recordBuffer.append(getProductId());
                recordBuffer.append(FIELD_END_MARKER);
                recordBuffer.append(getRecordFieldSeparator() != null ? getRecordFieldSeparator() : FIELD_SEPARATOR);
            }
            */

            // Adding extra Attributes - class name and method name for FINE and higher level messages
            Level level = record.getLevel();
            if (LOG_SOURCE_IN_KEY_VALUE ||
                    (level.intValue() <= Level.FINE.intValue())) {
                recordBuffer.append(FIELD_BEGIN_MARKER);
                recordBuffer.append("CLASSNAME: ");
                recordBuffer.append(record.getSourceClassName());
                recordBuffer.append(FIELD_END_MARKER);
                recordBuffer.append(getRecordFieldSeparator() != null ? getRecordFieldSeparator() : FIELD_SEPARATOR);
                recordBuffer.append(FIELD_BEGIN_MARKER);
                recordBuffer.append("METHODNAME: ");
                recordBuffer.append(record.getSourceMethodName());
                recordBuffer.append(FIELD_END_MARKER);
                recordBuffer.append(getRecordFieldSeparator() != null ? getRecordFieldSeparator() : FIELD_SEPARATOR);
            }

            if (_delegate != null) {
                _delegate.format(recordBuffer, level);
            }

            if (multiLine) {
                recordBuffer.append(FIELD_BEGIN_MARKER).append(FIELD_BEGIN_MARKER);    
            }
            recordBuffer.append(message);
            if (multiLine) {
                recordBuffer.append(FIELD_END_MARKER).append(FIELD_END_MARKER);    
            }
            recordBuffer.append(LINE_SEPARATOR);
            return recordBuffer.toString();
        } catch (Exception ex) {
            new ErrorManager().error(
                    "Error in formatting Logrecord", ex,
                    ErrorManager.FORMAT_FAILURE);
            // We've already notified the exception, the following
            // return is to keep javac happy
            return "";
        }
    }

    private boolean isMultiLine(String message) {
        if (message == null || message.isEmpty()) {
            return false;
        }
        String[] lines = message.split(LINE_SEPARATOR);
        if (lines == null) {
            return false;
        }
        return (lines.length > 1);
    }

    private String getLogMessage(LogRecord record) throws IOException {
        String logMessage = record.getMessage();
        if (logMessage == null) {
            logMessage = "";
        }        
        if (logMessage.indexOf("{0") >= 0 && logMessage.contains("}") && record.getParameters() != null) {
            // If we find {0} or {1} etc., in the message, then it's most
            // likely finer level messages for Method Entry, Exit etc.,
            logMessage = java.text.MessageFormat.format(
                    logMessage, record.getParameters());
        } else {
            ResourceBundle rb = getResourceBundle(record.getLoggerName());
            if (rb != null && rb.containsKey(logMessage)) {
                try {
                    logMessage = MessageFormat.format(
                            rb.getString(logMessage),
                            record.getParameters());
                } catch (java.util.MissingResourceException e) {
                    // If we don't find an entry, then we are covered
                    // because the logMessage is initialized already
                }
            }
        }        
        if (record.getThrown() != null) {
            StringBuffer buffer = new StringBuffer();
            buffer.append(logMessage);
            buffer.append(LINE_SEPARATOR);
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            record.getThrown().printStackTrace(pw);
            pw.close();
            buffer.append(sw.toString());
            logMessage = buffer.toString();
            sw.close();
        } 
        return logMessage;
    }

    private synchronized ResourceBundle getResourceBundle(String loggerName) {
        if (loggerName == null) {
            return null;
        }
        ResourceBundle rb = (ResourceBundle) loggerResourceBundleTable.get(
                loggerName);
        /*
                * Note that logManager.getLogger(loggerName) untrusted code may create loggers with
                * any arbitrary names this method should not be relied on so added code for checking null.
                */
        if (rb == null && logManager.getLogger(loggerName) != null) {
            rb = logManager.getLogger(loggerName).getResourceBundle();
            loggerResourceBundleTable.put(loggerName, rb);
        }
        return rb;
    }

    public String getRecordFieldSeparator() {
        return recordFieldSeparator;
    }

    public void setRecordFieldSeparator(String recordFieldSeparator) {
        this.recordFieldSeparator = recordFieldSeparator;
    }

    public String getRecordDateFormat() {
        return recordDateFormat;
    }

    public void setRecordDateFormat(String recordDateFormat) {
        this.recordDateFormat = recordDateFormat;
    }

    public String getMessageWithoutMessageID(String message) {
        String messageID = "";
        if (message.contains(": ")) {
            StringTokenizer st = new StringTokenizer(message, ":");
            messageID = st.nextToken();
            if (messageID.contains(" ")) {
                // here message ID is not proper value so returning original message only
                return message;
            } else {
                // removing message Id and returning message
                message = st.nextToken();
                return message.substring(1, message.length());
            }
        }
        return message;
    }

    public String getMapplingLogRecord(Level levelInRecord) {
        String level = levelInRecord.toString();
        if (level.equals("OFF")) {
            return "OFF";
        } else if (level.equals("SEVERE")) {
            return "ERROR";
        } else if (level.equals("WARNING")) {
            return "WARNING";
        } else if (level.equals("INFO")) {
            return "NOTIFICATION";
        } else if (level.equals("CONFIG")) {
            return "NOTIFICATION";
        } else if (level.equals("FINE")) {
            return "TRACE";
        } else if (level.equals("FINER")) {
            return "TRACE";
        } else if (level.equals("FINEST")) {
            return "TRACE";
        }
        return "";
    }

    public void setIncludeFields(String includeFields) {
        includeSuppAttrsBits.clear();
        if (includeFields != null) {
            String[] fields = includeFields.split(",");
            for (String field : fields) {
                if (field.equals("tid")) {
                    includeSuppAttrsBits.set(SupplementalAttribute.TID.ordinal());
                } else if (field.equals("userId")) {
                    includeSuppAttrsBits.set(SupplementalAttribute.USERID.ordinal());
                } else if (field.equals("ecid")) {
                    includeSuppAttrsBits.set(SupplementalAttribute.ECID.ordinal());
                } else if (field.equals("timeMillis")) {
                    includeSuppAttrsBits.set(SupplementalAttribute.TIME_MILLIS.ordinal());
                } else if (field.equals("levelValue")) {
                    includeSuppAttrsBits.set(SupplementalAttribute.LEVEL_VALUE.ordinal());
                } 
            }
        } else {
            includeSuppAttrsBits.set(SupplementalAttribute.TID.ordinal(), 
                    SupplementalAttribute.MAX_VALUE.ordinal());
        }
    }
}

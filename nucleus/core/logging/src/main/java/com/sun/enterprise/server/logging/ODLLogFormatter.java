/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 Oracle and/or its affiliates. All rights reserved.
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

// Portions Copyright [2017-2020] [Payara Foundation and/or affiliates]

package com.sun.enterprise.server.logging;

import com.sun.enterprise.server.logging.i18n.MessageResolver;

import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.ErrorManager;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;

/**
 * ODLLogFormatter conforms to the logging format defined by the
 * Log Working Group in Oracle.
 * The specified format is
 * "[[timestamp] [organization ID] [Message Type/Level] [Message ID] [Logger
 * Name] [Thread ID] [User ID] [ECID] [Extra Attributes] [Message]]\n"
 *
 * @author Naman Mehta
 * @author David Matejcek - refactoring
 */
@Service()
@ContractsProvided({ODLLogFormatter.class, Formatter.class})
@PerLookup
public class ODLLogFormatter extends AnsiColorFormatter implements LogEventBroadcaster {

    private static final int REC_BUFFER_CAPACITY = 512;
    private static final String FIELD_BEGIN_MARKER = "[";
    private static final String FIELD_END_MARKER = "]";
    private static final char FIELD_SEPARATOR = ' ';
    private static final String INDENT = "  ";
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private static final MessageResolver MSG_RESOLVER = new MessageResolver();

    private static final boolean LOG_SOURCE_IN_KEY_VALUE;
    private static final boolean RECORD_NUMBER_IN_KEY_VALUE;
    private static final String USER_ID;
    private static final String EC_ID;
    static {
        String logSource = System.getProperty("com.sun.aas.logging.keyvalue.logsource");
        LOG_SOURCE_IN_KEY_VALUE = "true".equals(logSource);
        String recordCount = System.getProperty("com.sun.aas.logging.keyvalue.recordnumber");
        RECORD_NUMBER_IN_KEY_VALUE = "true".equals(recordCount);
        USER_ID = System.getProperty("com.sun.aas.logging.userID", "").trim();
        EC_ID = System.getProperty("com.sun.aas.logging.ecID", "").trim();
    }

    private final UniformLogFormatter uniformLogFormatter;
    private final ExcludeFieldsSupport excludeFieldsSupport;
    private final FormatterDelegate delegate;
    private final AtomicLong recordNumber;

    private String recordFieldSeparator;
    private DateTimeFormatter recordDateFormat;
    private LogEventBroadcaster logEventBroadcasterDelegate;
    private boolean multiLineMode;

    public ODLLogFormatter() {
        this(null);
    }

    public ODLLogFormatter(FormatterDelegate delegate) {
        this.delegate = delegate;
        recordDateFormat = ISO_OFFSET_DATE_TIME;
        recordNumber = new AtomicLong();
        excludeFieldsSupport = new ExcludeFieldsSupport();
        uniformLogFormatter = new UniformLogFormatter();
    }

    @Override
    public String format(LogRecord record) {
        return formatLogRecord(record);
    }

    @Override
    public String formatMessage(LogRecord record) {
        return formatLogRecord(record);
    }


    private String formatLogRecord(LogRecord record) {
        return formatEnhancedLogRecord(MSG_RESOLVER.resolve(record));
    }

    private String formatEnhancedLogRecord(final EnhancedLogRecord record) {

        try {
            final LogEventImpl logEvent = new LogEventImpl();
            final StringBuilder output = new StringBuilder(REC_BUFFER_CAPACITY);

            String message = getMessage(record.getMessage(), record.getThrownStackTrace());
            if (message == null || message.isEmpty()) {
                return "";
            }
            boolean multiLine = multiLineMode || isMultiLine(message);

            output.append(FIELD_BEGIN_MARKER);
            final String timestamp = recordDateFormat.format(record.getTime());
            logEvent.setTimestamp(timestamp);
            output.append(timestamp);
            output.append(FIELD_END_MARKER);
            append(output, getRecordFieldSeparator(), FIELD_SEPARATOR);

            // Adding organization ID
            output.append(FIELD_BEGIN_MARKER);
            logEvent.setComponentId(uniformLogFormatter.getProductId());
            output.append(uniformLogFormatter.getProductId());
            output.append(FIELD_END_MARKER);
            append(output, getRecordFieldSeparator(), FIELD_SEPARATOR);

            // Adding messageType
            Level logLevel = record.getLevel();
            output.append(FIELD_BEGIN_MARKER);
            if (color()) {
                output.append(getColor(logLevel));
            }
            String odlLevel = logLevel.getLocalizedName();
            logEvent.setLevel(odlLevel);
            output.append(odlLevel);
            if (color()) {
                output.append(getReset());
            }
            output.append(FIELD_END_MARKER);
            append(output, getRecordFieldSeparator(), FIELD_SEPARATOR);

            // Adding message ID
            output.append(FIELD_BEGIN_MARKER);
            String msgId  = record.getMessageKey();
            output.append(msgId == null ? "" : msgId);
            logEvent.setMessageId(msgId);
            output.append(FIELD_END_MARKER);
            append(output, getRecordFieldSeparator(), FIELD_SEPARATOR);

            // Adding logger Name / module Name
            output.append(FIELD_BEGIN_MARKER);
            String loggerName = record.getLoggerName();
            loggerName = (loggerName == null) ? "" : loggerName;
            if (color()) {
                output.append(getLoggerColor());
            }
            output.append(loggerName);
            if (color()) {
                output.append(getReset());
            }
            logEvent.setLogger(loggerName);
            output.append(FIELD_END_MARKER);
            append(output, getRecordFieldSeparator(), FIELD_SEPARATOR);

            // Adding thread ID
            if (!excludeFieldsSupport.isSet(ExcludeFieldsSupport.SupplementalAttribute.TID)) {
                output.append(FIELD_BEGIN_MARKER);
                output.append("tid: _ThreadID=");
                output.append(record.getThreadID());
                logEvent.setThreadId(record.getThreadID());
                final String threadName = record.getThreadName();
                output.append(" _ThreadName=");
                logEvent.setThreadName(threadName);
                output.append(threadName);
                output.append(FIELD_END_MARKER);
                append(output, getRecordFieldSeparator(), FIELD_SEPARATOR);
            }

            // Adding user ID
            if (!excludeFieldsSupport.isSet(ExcludeFieldsSupport.SupplementalAttribute.USERID) && !USER_ID.isEmpty()) {
                output.append(FIELD_BEGIN_MARKER);
                output.append("userId: ");
                logEvent.setUser(USER_ID);
                output.append(USER_ID);
                output.append(FIELD_END_MARKER);
                append(output, getRecordFieldSeparator(), FIELD_SEPARATOR);
            }

            // Adding ec ID
            if (!excludeFieldsSupport.isSet(ExcludeFieldsSupport.SupplementalAttribute.ECID) && !EC_ID.isEmpty()) {
                output.append(FIELD_BEGIN_MARKER);
                output.append("ecid: ");
                logEvent.setECId(EC_ID);
                output.append(EC_ID);
                output.append(FIELD_END_MARKER);
                append(output, getRecordFieldSeparator(), FIELD_SEPARATOR);
            }

            // Include the raw time stamp
            if (!excludeFieldsSupport.isSet(ExcludeFieldsSupport.SupplementalAttribute.TIME_MILLIS)) {
                output.append(FIELD_BEGIN_MARKER);
                output.append("timeMillis: ");
                logEvent.setTimeMillis(record.getMillis());
                output.append(record.getMillis());
                output.append(FIELD_END_MARKER);
                append(output, getRecordFieldSeparator(), FIELD_SEPARATOR);
            }

            // Include the level value
            if (!excludeFieldsSupport.isSet(ExcludeFieldsSupport.SupplementalAttribute.LEVEL_VALUE)) {
                output.append(FIELD_BEGIN_MARKER);
                output.append("levelValue: ");
                logEvent.setLevelValue(logLevel.intValue());
                output.append(logLevel.intValue());
                output.append(FIELD_END_MARKER);
                append(output, getRecordFieldSeparator(), FIELD_SEPARATOR);
            }

            // Adding extra Attributes - record number
            if (RECORD_NUMBER_IN_KEY_VALUE) {
                output.append(FIELD_BEGIN_MARKER);
                long recNumber = recordNumber.incrementAndGet();
                output.append("RECORDNUMBER: ");
                logEvent.getSupplementalAttributes().put("RECORDNUMBER", recNumber);
                output.append(recNumber);
                output.append(FIELD_END_MARKER);
                append(output, getRecordFieldSeparator(), FIELD_SEPARATOR);
            }

            // Adding extra Attributes - class name and method name for FINE and higher level messages
            Level level = record.getLevel();
            if (LOG_SOURCE_IN_KEY_VALUE ||
                    (level.intValue() <= Level.FINE.intValue())) {
                String sourceClassName = record.getSourceClassName();
                if (sourceClassName != null && !sourceClassName.isEmpty()) {
                    output.append(FIELD_BEGIN_MARKER);
                    output.append("CLASSNAME: ");
                    logEvent.getSupplementalAttributes().put("CLASSNAME", sourceClassName);
                    output.append(sourceClassName);
                    output.append(FIELD_END_MARKER);
                    append(output, getRecordFieldSeparator(), FIELD_SEPARATOR);
                }
                String sourceMethodName = record.getSourceMethodName();
                if (sourceMethodName != null && !sourceMethodName.isEmpty()) {
                    output.append(FIELD_BEGIN_MARKER);
                    output.append("METHODNAME: ");
                    logEvent.getSupplementalAttributes().put("METHODNAME", sourceMethodName);
                    output.append(sourceMethodName);
                    output.append(FIELD_END_MARKER);
                    append(output, getRecordFieldSeparator(), FIELD_SEPARATOR);
                }
            }

            if (delegate != null) {
                delegate.format(output, level);
            }

            if (multiLine) {
                output.append(FIELD_BEGIN_MARKER).append(FIELD_BEGIN_MARKER);
                output.append(LINE_SEPARATOR);
                output.append(INDENT);
            }
            output.append(message);
            logEvent.setMessage(message);
            if (multiLine) {
                output.append(FIELD_END_MARKER).append(FIELD_END_MARKER);
            }
            output.append(LINE_SEPARATOR).append(LINE_SEPARATOR);
            informLogEventListeners(logEvent);
            return output.toString();
        } catch (Exception ex) {
            new ErrorManager().error("Error in formatting Logrecord", ex, ErrorManager.FORMAT_FAILURE);
            // We've already notified the exception, the following
            // return is to keep javac happy
            return "";
        }
    }

    private StringBuilder append(final StringBuilder output, final Object value, final Object defaultValue) {
        if (value == null) {
            return output.append(defaultValue);
        }
        return output.append(value);
    }

    private boolean isMultiLine(String message) {
        if (message == null || message.isEmpty()) {
            return false;
        }
        return message.contains(LINE_SEPARATOR);
    }

    private String getMessage(final String message, final String stackTrace) {
        if (message == null || message.isEmpty()) {
            return stackTrace == null ? "" : stackTrace;
        }
        if (stackTrace == null) {
            return message;
        }
        return message + LINE_SEPARATOR + stackTrace;
    }

    public String getRecordFieldSeparator() {
        return recordFieldSeparator;
    }

    public void setRecordFieldSeparator(String recordFieldSeparator) {
        this.recordFieldSeparator = recordFieldSeparator;
    }

    public String getRecordDateFormat() {
        return recordDateFormat.toString();
    }

    public void setRecordDateFormat(String format) {
        this.recordDateFormat = format == null ? ISO_OFFSET_DATE_TIME : DateTimeFormatter.ofPattern(format);
    }

    void setLogEventBroadcaster(LogEventBroadcaster logEventBroadcaster) {
        logEventBroadcasterDelegate = logEventBroadcaster;
    }

    @Override
    public void informLogEventListeners(LogEvent logEvent) {
        if (logEventBroadcasterDelegate != null) {
            logEventBroadcasterDelegate.informLogEventListeners(logEvent);
        }
    }

    void setMultiLineMode(boolean value) {
        this.multiLineMode = value;
    }

    void setExcludeFields(String excludeFields) {
        excludeFieldsSupport.setExcludeFields(excludeFields);
    }
}

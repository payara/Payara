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

package fish.payara.logging.jul.formatter;

import fish.payara.logging.jul.event.LogEventImpl;
import fish.payara.logging.jul.i18n.MessageResolver;
import fish.payara.logging.jul.internal.EnhancedLogRecord;
import fish.payara.logging.jul.internal.ExcludeFieldsSupport;
import fish.payara.logging.jul.internal.ExcludeFieldsSupport.SupplementalAttribute;

import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.ErrorManager;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * ODLLogFormatter conforms to the logging format defined by the
 * Log Working Group in Oracle.
 * The specified format is
 * <pre>[[timestamp] [organization ID] [Message Type/Level] [Message ID] [Logger Name] [Thread ID] [User ID] [ECID] [Extra Attributes] [Message]]\n</pre>
 *
 * @author Naman Mehta
 * @author David Matejcek - refactoring
 */
public class ODLLogFormatter extends AnsiColorFormatter {

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

    private final ExcludeFieldsSupport excludeFieldsSupport;
    private final AtomicLong recordNumber;

    private String recordFieldSeparator;
    private boolean multiLineMode;

    public ODLLogFormatter() {
        recordNumber = new AtomicLong();
        excludeFieldsSupport = new ExcludeFieldsSupport();
        multiLineMode = true;
    }

    @Override
    public BroadcastingFormatterOutput formatRecord(final LogRecord record) {
        return formatLogRecord(record);
    }

    private BroadcastingFormatterOutput formatLogRecord(LogRecord record) {
        return formatEnhancedLogRecord(MSG_RESOLVER.resolve(record));
    }

    private BroadcastingFormatterOutput formatEnhancedLogRecord(final EnhancedLogRecord record) {

        try {
            final LogEventImpl logEvent = new LogEventImpl();
            final StringBuilder output = new StringBuilder(REC_BUFFER_CAPACITY);

            String message = getMessage(record.getMessage(), record.getThrownStackTrace());
            if (message == null || message.isEmpty()) {
                return new BroadcastingFormatterOutput("", null);
            }
            boolean multiLine = multiLineMode || isMultiLine(message);

            output.append(FIELD_BEGIN_MARKER);
            final String timestamp = getDateTimeFormatter().format(record.getTime());
            logEvent.setTimestamp(timestamp);
            output.append(timestamp);
            output.append(FIELD_END_MARKER);
            append(output, recordFieldSeparator, FIELD_SEPARATOR);

            // Adding organization ID
            output.append(FIELD_BEGIN_MARKER);
            logEvent.setComponentId(getProductId());
            output.append(getProductId());
            output.append(FIELD_END_MARKER);
            append(output, recordFieldSeparator, FIELD_SEPARATOR);

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
            append(output, recordFieldSeparator, FIELD_SEPARATOR);

            // Adding message ID
            output.append(FIELD_BEGIN_MARKER);
            String msgId  = record.getMessageKey();
            output.append(msgId == null ? "" : msgId);
            logEvent.setMessageId(msgId);
            output.append(FIELD_END_MARKER);
            append(output, recordFieldSeparator, FIELD_SEPARATOR);

            // Adding logger Name / module Name
            output.append(FIELD_BEGIN_MARKER);
            String loggerName = record.getLoggerName();
            loggerName = loggerName == null ? "" : loggerName;
            if (color()) {
                output.append(getLoggerColor());
            }
            output.append(loggerName);
            if (color()) {
                output.append(getReset());
            }
            logEvent.setLogger(loggerName);
            output.append(FIELD_END_MARKER);
            append(output, recordFieldSeparator, FIELD_SEPARATOR);

            // Adding thread ID
            if (!excludeFieldsSupport.isSet(SupplementalAttribute.TID)) {
                output.append(FIELD_BEGIN_MARKER);
                output.append("tid: _ThreadID=");
                output.append(record.getThreadID());
                logEvent.setThreadId(record.getThreadID());
                final String threadName = record.getThreadName();
                output.append(" _ThreadName=");
                logEvent.setThreadName(threadName);
                output.append(threadName);
                output.append(FIELD_END_MARKER);
                append(output, recordFieldSeparator, FIELD_SEPARATOR);
            }

            // Adding user ID
            if (!excludeFieldsSupport.isSet(SupplementalAttribute.USERID) && !USER_ID.isEmpty()) {
                output.append(FIELD_BEGIN_MARKER);
                output.append("userId: ");
                logEvent.setUser(USER_ID);
                output.append(USER_ID);
                output.append(FIELD_END_MARKER);
                append(output, recordFieldSeparator, FIELD_SEPARATOR);
            }

            // Adding ec ID
            if (!excludeFieldsSupport.isSet(SupplementalAttribute.ECID) && !EC_ID.isEmpty()) {
                output.append(FIELD_BEGIN_MARKER);
                output.append("ecid: ");
                logEvent.setECId(EC_ID);
                output.append(EC_ID);
                output.append(FIELD_END_MARKER);
                append(output, recordFieldSeparator, FIELD_SEPARATOR);
            }

            // Include the raw time stamp
            if (!excludeFieldsSupport.isSet(SupplementalAttribute.TIME_MILLIS)) {
                output.append(FIELD_BEGIN_MARKER);
                output.append("timeMillis: ");
                logEvent.setTimeMillis(record.getMillis());
                output.append(record.getMillis());
                output.append(FIELD_END_MARKER);
                append(output, recordFieldSeparator, FIELD_SEPARATOR);
            }

            // Include the level value
            if (!excludeFieldsSupport.isSet(SupplementalAttribute.LEVEL_VALUE)) {
                output.append(FIELD_BEGIN_MARKER);
                output.append("levelValue: ");
                logEvent.setLevelValue(logLevel.intValue());
                output.append(logLevel.intValue());
                output.append(FIELD_END_MARKER);
                append(output, recordFieldSeparator, FIELD_SEPARATOR);
            }

            // Adding extra Attributes - record number
            if (RECORD_NUMBER_IN_KEY_VALUE) {
                output.append(FIELD_BEGIN_MARKER);
                long recNumber = recordNumber.incrementAndGet();
                output.append("RECORDNUMBER: ");
                logEvent.getSupplementalAttributes().put("RECORDNUMBER", recNumber);
                output.append(recNumber);
                output.append(FIELD_END_MARKER);
                append(output, recordFieldSeparator, FIELD_SEPARATOR);
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
                    append(output, recordFieldSeparator, FIELD_SEPARATOR);
                }
                String sourceMethodName = record.getSourceMethodName();
                if (sourceMethodName != null && !sourceMethodName.isEmpty()) {
                    output.append(FIELD_BEGIN_MARKER);
                    output.append("METHODNAME: ");
                    logEvent.getSupplementalAttributes().put("METHODNAME", sourceMethodName);
                    output.append(sourceMethodName);
                    output.append(FIELD_END_MARKER);
                    append(output, recordFieldSeparator, FIELD_SEPARATOR);
                }
            }

            formatDelegatePart(output, level);

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
            return new BroadcastingFormatterOutput(output.toString(), logEvent);
        } catch (Exception ex) {
            new ErrorManager().error("Error in formatting Logrecord", ex, ErrorManager.FORMAT_FAILURE);
            return new BroadcastingFormatterOutput("", null);
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

    public void setMultiLineMode(boolean value) {
        this.multiLineMode = value;
    }

    public void setExcludeFields(String excludeFields) {
        this.excludeFieldsSupport.setExcludeFields(excludeFields);
    }
}

/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2013 Oracle and/or its affiliates. All rights reserved.
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

// Portions Copyright [2016-2020] [Payara Foundation]

package fish.payara.logging.jul.formatter;

import fish.payara.logging.jul.event.LogEventImpl;
import fish.payara.logging.jul.i18n.MessageResolver;
import fish.payara.logging.jul.internal.EnhancedLogRecord;
import fish.payara.logging.jul.internal.ExcludeFieldsSupport;
import fish.payara.logging.jul.internal.ExcludeFieldsSupport.SupplementalAttribute;

import java.time.format.DateTimeFormatter;
import java.time.format.DecimalStyle;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.ErrorManager;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;

/**
 * UniformLogFormatter conforms to the logging format defined by the
 * Log Working Group in Java Webservices Org.
 * The specified format is
 * "[#|DATETIME|LOG_LEVEL|PRODUCT_ID|LOGGER NAME|OPTIONAL KEY VALUE PAIRS|MESSAGE|#]\n"
 *
 * @author Hemanth Puttaswamy
 * @author David Matejcek - refactoring
 */
public class UniformLogFormatter extends AnsiColorFormatter {

    private static final int REC_BUFFER_CAPACITY = 512;
    private static final String RECORD_NUMBER = "RecordNumber";
    private static final String METHOD_NAME = "MethodName";
    private static final String CLASS_NAME = "ClassName";

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    private static final String RECORD_BEGIN_MARKER = "[#|";
    private static final String RECORD_END_MARKER = "|#]" + LINE_SEPARATOR;
    private static final char FIELD_SEPARATOR = '|';
    public static final char NVPAIR_SEPARATOR = ';';
    public static final char NV_SEPARATOR = '=';
    private static final String INDENT = "  ";

    private static final MessageResolver MSG_RESOLVER = new MessageResolver();

    private static final boolean LOG_SOURCE_IN_KEY_VALUE;
    private static final boolean RECORD_NUMBER_IN_KEY_VALUE;
    static {
        String logSource = System.getProperty("com.sun.aas.logging.keyvalue.logsource");
        LOG_SOURCE_IN_KEY_VALUE = "true".equals(logSource);
        String recordCount = System.getProperty("com.sun.aas.logging.keyvalue.recordnumber");
        RECORD_NUMBER_IN_KEY_VALUE = "true".equals(recordCount);
    }

    private final AtomicLong recordNumber;
    private final ExcludeFieldsSupport excludeFieldsSupport;

    private String recordBeginMarker;
    private String recordEndMarker;
    private String recordFieldSeparator;
    private DateTimeFormatter recordDateFormat;
    private boolean multiLineMode;

    public UniformLogFormatter() {
        recordDateFormat = ISO_OFFSET_DATE_TIME.withDecimalStyle(DecimalStyle.STANDARD.withZeroDigit('0'));
        excludeFieldsSupport = new ExcludeFieldsSupport();
        recordNumber = new AtomicLong();
    }


    @Override
    public BroadcastingFormatterOutput formatRecord(final LogRecord record) {
        return formatLogRecord(record);
    }

    public String getRecordBeginMarker() {
        return recordBeginMarker;
    }

    public void setRecordBeginMarker(String recordBeginMarker) {
        this.recordBeginMarker = recordBeginMarker;
    }

    public String getRecordEndMarker() {
        return recordEndMarker;
    }

    public void setRecordEndMarker(String recordEndMarker) {
        this.recordEndMarker = recordEndMarker;
    }

    public String getRecordFieldSeparator() {
        return recordFieldSeparator;
    }

    public void setRecordFieldSeparator(String recordFieldSeparator) {
        this.recordFieldSeparator = recordFieldSeparator;
    }

    /**
     * @return The date format for the record.
     */
    public final String getRecordDateFormat() {
        return recordDateFormat.toString();
    }

    /**
     * @param format The date format to set for records.
     */
    public final void setRecordDateFormat(String format) {
        this.recordDateFormat = format == null ? ISO_OFFSET_DATE_TIME : DateTimeFormatter.ofPattern(format);
    }

    /**
     * @param multiLineMode the multiLineMode to set
     */
    public void setMultiLineMode(boolean value) {
        multiLineMode = value;
    }

    /**
     * @param excludeFields the excludeFields to set
     */
    public void setExcludeFields(String excludeFields) {
        this.excludeFieldsSupport.setExcludeFields(excludeFields);
    }

    /**
     * Sun One Appserver SE/EE? can override to specify their product specific
     * key value pairs.
     */
    protected void getNameValuePairs(StringBuilder buf, LogRecord record) {

        Object[] parameters = record.getParameters();
        if (parameters == null || parameters.length == 0) {
            return;
        }

        try {
            for (Object obj : parameters) {
                if (obj == null) {
                    continue;
                }
                if (obj instanceof Map) {
                    for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) obj).entrySet()) {
                        // there are implementations that allow <null> keys...
                        if (entry.getKey() != null) {
                            buf.append(entry.getKey().toString());
                        } else {
                            buf.append("null");
                        }

                        buf.append(NV_SEPARATOR);

                        // also handle <null> values...
                        if (entry.getValue() != null) {
                            buf.append(entry.getValue().toString());
                        } else {
                            buf.append("null");
                        }
                        buf.append(NVPAIR_SEPARATOR);

                    }
                } else if (obj instanceof java.util.Collection) {
                    for (Object entry : ((Collection) obj)) {
                        // handle null values (remember the specs)...
                        if (entry != null) {
                            buf.append(entry.toString());
                        } else {
                            buf.append("null");
                        }
                        buf.append(NVPAIR_SEPARATOR);

                    }
                }
            }
        } catch (Exception e) {
            new ErrorManager().error("Error in extracting Name Value Pairs", e, ErrorManager.FORMAT_FAILURE);
        }
    }

    private BroadcastingFormatterOutput formatLogRecord(LogRecord record) {
        return formatEnhancedLogRecord(MSG_RESOLVER.resolve(record));
    }

    private BroadcastingFormatterOutput formatEnhancedLogRecord(final EnhancedLogRecord record) {
        try {
            final LogEventImpl logEvent = new LogEventImpl();
            final StringBuilder output = new StringBuilder(REC_BUFFER_CAPACITY);

            append(output, getRecordBeginMarker(), RECORD_BEGIN_MARKER);
            String timestamp = recordDateFormat.format(record.getTime());
            logEvent.setTimestamp(timestamp);
            output.append(timestamp);
            if (color()) {
                output.append(getColor(record.getLevel()));
            }
            append(output, getRecordFieldSeparator(), FIELD_SEPARATOR);

            logEvent.setLevel(record.getLevel().getName());
            output.append(record.getLevel().getLocalizedName());
            append(output, getRecordFieldSeparator(), FIELD_SEPARATOR);
            if (color()) {
                output.append(getReset());
            }
            logEvent.setComponentId(getProductId());
            append(output, getProductId(), "");
            append(output, getRecordFieldSeparator(), FIELD_SEPARATOR);

            String loggerName = record.getLoggerName();
            loggerName = loggerName == null ? "" : loggerName;
            logEvent.setLogger(loggerName);
            if (color()) {
                output.append(getLoggerColor());
            }
            output.append(loggerName);
            append(output, getRecordFieldSeparator(), FIELD_SEPARATOR);
            if (color()) {
                output.append(getReset());
            }

            if (!excludeFieldsSupport.isSet(SupplementalAttribute.TID)) {
                output.append("_ThreadID").append(NV_SEPARATOR);
                logEvent.setThreadId(record.getThreadID());
                output.append(record.getThreadID()).append(NVPAIR_SEPARATOR);
                output.append("_ThreadName").append(NV_SEPARATOR);
                final String threadName = record.getThreadName();
                logEvent.setThreadName(threadName);
                output.append(threadName);
                output.append(NVPAIR_SEPARATOR);
            }

            // Include the raw long time stamp value in the log
            if (!excludeFieldsSupport.isSet(SupplementalAttribute.TIME_MILLIS)) {
                output.append("_TimeMillis").append(NV_SEPARATOR);
                logEvent.setTimeMillis(record.getMillis());
                output.append(record.getMillis()).append(NVPAIR_SEPARATOR);
            }

            // Include the integer level value in the log
            Level level = record.getLevel();
            if (!excludeFieldsSupport.isSet(SupplementalAttribute.LEVEL_VALUE)) {
                output.append("_LevelValue").append(NV_SEPARATOR);
                int levelValue = level.intValue();
                logEvent.setLevelValue(levelValue);
                output.append(levelValue).append(NVPAIR_SEPARATOR);
            }

            String msgId = record.getMessageKey();
            if (msgId != null && !msgId.isEmpty()) {
                logEvent.setMessageId(msgId);
                output.append("_MessageID").append(NV_SEPARATOR);
                output.append(msgId).append(NVPAIR_SEPARATOR);
            }

            // ClassName and MethodName information should be included for FINE log level.
            if (LOG_SOURCE_IN_KEY_VALUE || level.intValue() <= Level.FINE.intValue()) {
                String sourceClassName = record.getSourceClassName();
                if (sourceClassName != null && !sourceClassName.isEmpty()) {
                    output.append(CLASS_NAME).append(NV_SEPARATOR);
                    logEvent.getSupplementalAttributes().put(CLASS_NAME, sourceClassName);
                    output.append(sourceClassName);
                    output.append(NVPAIR_SEPARATOR);
                }

                String sourceMethodName = record.getSourceMethodName();
                if (sourceMethodName != null && !sourceMethodName.isEmpty()) {
                    output.append(METHOD_NAME).append(NV_SEPARATOR);
                    logEvent.getSupplementalAttributes().put(METHOD_NAME, sourceMethodName);
                    output.append(sourceMethodName);
                    output.append(NVPAIR_SEPARATOR);
                }
            }

            if (RECORD_NUMBER_IN_KEY_VALUE) {
                final long recNumber = recordNumber.getAndIncrement();
                output.append(RECORD_NUMBER).append(NV_SEPARATOR);
                logEvent.getSupplementalAttributes().put(RECORD_NUMBER, recNumber);
                output.append(recNumber).append(NVPAIR_SEPARATOR);
            }

            formatDelegatePart(output, level);

            append(output, getRecordFieldSeparator(), FIELD_SEPARATOR);

            if (multiLineMode) {
                output.append(LINE_SEPARATOR);
                output.append(INDENT);
            }
            final String logMessage = getMessage(record.getMessage(), record.getThrownStackTrace());
            logEvent.setMessage(logMessage);
            output.append(logMessage);
            append(output, getRecordEndMarker(), RECORD_END_MARKER);
            output.append(LINE_SEPARATOR).append(LINE_SEPARATOR);
            informLogEventListeners(logEvent);
            return new BroadcastingFormatterOutput(output.toString(), logEvent);

        } catch (Exception ex) {
            new ErrorManager().error("Error in formatting Logrecord", ex, ErrorManager.FORMAT_FAILURE);
            return new BroadcastingFormatterOutput(record.getMessage(), null);
        }
    }

    private StringBuilder append(final StringBuilder output, final Object value, final Object defaultValue) {
        if (value == null) {
            return output.append(defaultValue);
        }
        return output.append(value);
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
}

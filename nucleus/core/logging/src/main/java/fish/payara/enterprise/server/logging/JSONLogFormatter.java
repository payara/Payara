/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016-2021 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.enterprise.server.logging;

import com.sun.common.util.logging.GFLogRecord;
import com.sun.enterprise.server.logging.CommonFormatter;
import com.sun.enterprise.server.logging.ExcludeFieldsSupport;
import com.sun.enterprise.server.logging.FormatterDelegate;
import com.sun.enterprise.server.logging.LogEvent;
import com.sun.enterprise.server.logging.LogEventBroadcaster;
import com.sun.enterprise.server.logging.LogEventImpl;
import com.sun.enterprise.server.logging.UniformLogFormatter;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.ErrorManager;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Class for converting a {@link LogRecord} to Json format
 * @since 4.1.1.164
 * @author savage
 */
// Removed HK2 service annotations as never used as such, only plain Java instantiation.
public class JSONLogFormatter extends CommonFormatter implements LogEventBroadcaster {

    private static final String RECORD_NUMBER = "RecordNumber";
    private static final String METHOD_NAME = "MethodName";
    private static final String CLASS_NAME = "ClassName";

    private Map<String, ResourceBundle> loggerResourceBundleTable;
    private LogManager logManager;

    private final Date date = new Date();

    private static boolean LOG_SOURCE_IN_KEY_VALUE = false;

    private static boolean RECORD_NUMBER_IN_KEY_VALUE = false;

    private FormatterDelegate _delegate = null;

    // Static Initialiser Block
    static {
        String logSource = System.getProperty(
                "com.sun.aas.logging.keyvalue.logsource");
        if ((logSource != null)
            && (logSource.equals("true"))) {
            LOG_SOURCE_IN_KEY_VALUE = true;
        }

        String recordCount = System.getProperty("com.sun.aas.logging.keyvalue.recordnumber");
        if ((recordCount != null)
                && (recordCount.equals("true"))) {
            RECORD_NUMBER_IN_KEY_VALUE = true;
        }
    }

    private long recordNumber = 0;
    private String recordDateFormat;

    // Event separator
    private static final String LINE_SEPARATOR = System.lineSeparator();

    // String values for field keys
    private String TIMESTAMP_KEY = "Timestamp";
    private String LOG_LEVEL_KEY = "Level";
    private String PRODUCT_ID_KEY = "Version";
    private String LOGGER_NAME_KEY = "LoggerName";
    // String values for exception keys
    private String EXCEPTION_KEY = "Exception";
    private String STACK_TRACE_KEY = "StackTrace";
    // String values for thread excludable keys
    private String THREAD_ID_KEY = "ThreadID";
    private String THREAD_NAME_KEY = "ThreadName";
    private String LEVEL_VALUE_KEY = "LevelValue";
    private String TIME_MILLIS_KEY = "TimeMillis";
    private String MESSAGE_ID_KEY = "MessageID";
    private String LOG_MESSAGE_KEY = "LogMessage";
    private String THROWABLE_KEY = "Throwable";

    private static final String RFC3339_DATE_FORMAT =
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    private LogEventBroadcaster logEventBroadcasterDelegate;

    /**
     * For backwards compatibility with log format for pre-182
     * @deprecated
     */
    @Deprecated
    private static final String PAYARA_JSONLOGFORMATTER_UNDERSCORE="fish.payara.deprecated.jsonlogformatter.underscoreprefix";

    // Account for instances of (Formatter) Class.forName(formatter).newInstance();
    public JSONLogFormatter() {
        this(null);
    }

    public JSONLogFormatter(String excludeFields) {
        super(excludeFields);
        loggerResourceBundleTable = new HashMap<>();
        logManager = LogManager.getLogManager();

        String underscorePrefix = logManager.getProperty(PAYARA_JSONLOGFORMATTER_UNDERSCORE);
        if (Boolean.parseBoolean(underscorePrefix)) {
            TIMESTAMP_KEY = "_" + TIMESTAMP_KEY;
            LOG_LEVEL_KEY = "_" + LOG_LEVEL_KEY;
            PRODUCT_ID_KEY = "_" + PRODUCT_ID_KEY;
            LOGGER_NAME_KEY = "_" + LOGGER_NAME_KEY;
            EXCEPTION_KEY = "_" + EXCEPTION_KEY;
            STACK_TRACE_KEY = "_" + STACK_TRACE_KEY;
            // String values for thread excludable keys
            THREAD_ID_KEY = "_" + THREAD_ID_KEY;
            THREAD_NAME_KEY = "_" + THREAD_NAME_KEY;
            LEVEL_VALUE_KEY = "_" + LEVEL_VALUE_KEY;
            TIME_MILLIS_KEY = "_" + TIME_MILLIS_KEY;
            MESSAGE_ID_KEY = "_" + MESSAGE_ID_KEY;
            LOG_MESSAGE_KEY = "_" + LOG_MESSAGE_KEY;
            THROWABLE_KEY = "_" + THROWABLE_KEY;
        }
    }

    public JSONLogFormatter(FormatterDelegate delegate, String excludeFields) {
        this(excludeFields);
        _delegate = delegate;
    }

    public void setDelegate(FormatterDelegate delegate) {
        _delegate = delegate;
    }

    @Override
    public String format(LogRecord record) {
        return jsonLogFormat(record);
    }

    @Override
    public String formatMessage(LogRecord record) {
        return jsonLogFormat(record);
    }

    /**
     * @param record The record to format.
     * @return The JSON formatted record.
     */
    private String jsonLogFormat(LogRecord record) {
        try {
            LogEventImpl logEvent = new LogEventImpl();
            JsonObjectBuilder eventObject = Json.createObjectBuilder();

            /*
             * Create the timestamp field and append to object.
             */
            SimpleDateFormat dateFormatter;

            if (null != getRecordDateFormat()) {
                dateFormatter = new SimpleDateFormat(getRecordDateFormat());
            } else {
                dateFormatter = new SimpleDateFormat(RFC3339_DATE_FORMAT);
            }

            date.setTime(record.getMillis());
            String timestampValue = dateFormatter.format(date);
            logEvent.setTimestamp(timestampValue);
            eventObject.add(TIMESTAMP_KEY, timestampValue);

            /*
             * Create the event level field and append to object.
             */
            Level eventLevel = record.getLevel();
            logEvent.setLevel(eventLevel.getName());
            eventObject.add(LOG_LEVEL_KEY, eventLevel.getLocalizedName());

            /*
             * Get the product id and append to object.
             */
            if (!isFieldExcluded(ExcludeFieldsSupport
                    .SupplementalAttribute.VERSION)) {

                String productId = getProductId();
                logEvent.setComponentId(productId);
                eventObject.add(PRODUCT_ID_KEY, productId);
            }
            /*
             * Get the logger name and append to object.
             */
            String loggerName = record.getLoggerName();

            if (null == loggerName) {
                loggerName = "";
            }

            logEvent.setLogger(loggerName);
            eventObject.add(LOGGER_NAME_KEY, loggerName);

            /*
             * Get thread information and append to object if not excluded.
             */
            if (!isFieldExcluded(ExcludeFieldsSupport
                    .SupplementalAttribute.TID)) {
                // Thread ID
                int threadId = record.getThreadID();
                logEvent.setThreadId(threadId);
                eventObject.add(THREAD_ID_KEY, String.valueOf(threadId));

                // Thread Name
                String threadName;

                if (record instanceof GFLogRecord) {
                    threadName = ((GFLogRecord)record).getThreadName();
                } else {
                    threadName = Thread.currentThread().getName();
                }

                logEvent.setThreadName(threadName);
                eventObject.add(THREAD_NAME_KEY, threadName);
            }

            /*
             * Get millis time for log entry timestamp
             */
            if (!isFieldExcluded(ExcludeFieldsSupport
                    .SupplementalAttribute.TIME_MILLIS)) {
                long timestamp = record.getMillis();
                logEvent.setTimeMillis(timestamp);
                eventObject.add(TIME_MILLIS_KEY, String.valueOf(timestamp));
            }

            /*
             * Include the integer value for log level
             */
            Level level = record.getLevel();
            if (!isFieldExcluded(ExcludeFieldsSupport
                    .SupplementalAttribute.LEVEL_VALUE)) {
                int levelValue = level.intValue();
                logEvent.setLevelValue(levelValue);
                eventObject.add(LEVEL_VALUE_KEY, String.valueOf(levelValue));
            }

            /*
             * Stick the message id on the entry
             */
            String messageId = getMessageId(record);
            if (messageId != null && !messageId.isEmpty()) {
                logEvent.setMessageId(messageId);
                eventObject.add(MESSAGE_ID_KEY, messageId);
            }

            /*
             * Include ClassName and MethodName for FINER and FINEST log levels.
             */
            if (LOG_SOURCE_IN_KEY_VALUE ||
                    level.intValue() <= Level.FINE.intValue()) {
                String sourceClassName = record.getSourceClassName();

                if (null != sourceClassName && !sourceClassName.isEmpty()) {
                    logEvent.getSupplementalAttributes()
                            .put(CLASS_NAME, sourceClassName);
                    eventObject.add(CLASS_NAME, sourceClassName);
                }

                String sourceMethodName = record.getSourceMethodName();

                if (null != sourceMethodName && !sourceMethodName.isEmpty()) {
                    logEvent.getSupplementalAttributes()
                            .put(METHOD_NAME, sourceMethodName);
                    eventObject.add(METHOD_NAME, sourceMethodName);
                }
            }

            /*
             * Add the record number to the entry.
             */
            if (RECORD_NUMBER_IN_KEY_VALUE) {
                recordNumber++;
                logEvent.getSupplementalAttributes()
                        .put(RECORD_NUMBER, recordNumber);
                eventObject.add(RECORD_NUMBER, String.valueOf(recordNumber));
            }

            if (null != _delegate) {
                _delegate.format(new StringBuilder()
                        .append(eventObject.toString()), level);
            }

            Object[] parameters = record.getParameters();
            if (parameters != null) {
                for (Object parameter : parameters) {
                    if (parameter instanceof Map) {
                        for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) parameter).entrySet()) {
                            // there are implementations that allow <null> keys...
                            String key;
                            if (entry.getKey() != null) {
                                key = entry.getKey().toString();
                            } else {
                                key = "null";
                            }

                            // also handle <null> values...
                            if (entry.getValue() != null) {
                                eventObject.add(key, entry.getValue().toString());
                            } else {
                                eventObject.add(key, "null");
                            }
                        }
                    }
                }
            }

            String logMessage = record.getMessage();

            if (null == logMessage || logMessage.trim().equals("")) {
                Throwable throwable = record.getThrown();
                if (null != throwable) {
                    try (StringWriter stringWriter = new StringWriter();
                         PrintWriter printWriter = new PrintWriter(stringWriter)) {
                        JsonObjectBuilder traceObject = Json.createObjectBuilder();
                        throwable.printStackTrace(printWriter);
                        if (throwable.getMessage() != null) {
                            traceObject.add(EXCEPTION_KEY, throwable.getMessage());
                        }
                        logMessage = stringWriter.toString();
                        traceObject.add(STACK_TRACE_KEY, logMessage);
                        logEvent.setMessage(logMessage);
                        eventObject.add(THROWABLE_KEY, traceObject.build());
                    }
                }
            } else {
                logMessage = UniformLogFormatter.formatLogMessage(logMessage, record, this::getResourceBundle);
                StringBuilder logMessageBuilder = new StringBuilder();
                logMessageBuilder.append(logMessage);

                Throwable throwable = getThrowable(record);
                if (null != throwable) {
                    try (StringWriter stringWriter = new StringWriter();
                         PrintWriter printWriter = new PrintWriter(stringWriter)) {
                        JsonObjectBuilder traceObject =Json.createObjectBuilder();
                        throwable.printStackTrace(printWriter);
                        logMessage = stringWriter.toString();
                        traceObject.add(EXCEPTION_KEY, logMessageBuilder.toString());
                        traceObject.add(STACK_TRACE_KEY, logMessage);
                        logEvent.setMessage(logMessage);
                        eventObject.add(THROWABLE_KEY, traceObject.build());
                    }
                } else {
                    logMessage = logMessageBuilder.toString();
                    logEvent.setMessage(logMessage);
                    eventObject.add(LOG_MESSAGE_KEY, logMessage);
                }
            }

            informLogEventListeners(logEvent);
            return eventObject.build().toString() + LINE_SEPARATOR;

        } catch (Exception ex) {
            new ErrorManager().error(
                    "Error in formatting Logrecord", ex,
                    ErrorManager.FORMAT_FAILURE);
            return "";
        }
    }

    /**
     * @param record
     * @return
     */
    static String getMessageId(LogRecord record) {
        String message = record.getMessage();
        if (null != message && !message.isEmpty()) {
            ResourceBundle bundle = record.getResourceBundle();
            if (null != bundle && bundle.containsKey(message)) {
                if (!bundle.getString(message).isEmpty()) {
                    return message;
                }
            }
        }
        return null;
    }

    /**
     * @param record
     * @return
     */
    static Throwable getThrowable(LogRecord record) {
        return record.getThrown();
    }

    /**
     * @param loggerName Name of logger to get the ResourceBundle of.
     * @return The ResourceBundle for the logger name given.
     */
    private synchronized ResourceBundle getResourceBundle(String loggerName) {
        if (loggerName == null) {
            return null;
        }

        ResourceBundle bundle = loggerResourceBundleTable.get(loggerName);

        /*
         *  logManager.getLogger should not be relied upon.
         *  To deal with this check if bundle is null and logger is not.
         *  Put a new logger and bundle in the resource bundle table if so.
         */
        Logger logger = logManager.getLogger(loggerName);
        if (null == bundle && null != logger) {
            bundle = logger.getResourceBundle();
            loggerResourceBundleTable.put(loggerName, bundle);
        }

        return bundle;
    }

    /**
     * @return The date format for the record.
     */
    public String getRecordDateFormat() {
        return recordDateFormat;
    }

    /**
     * @param recordDateFormat The date format to set for records.
     */
    public void setRecordDateFormat(String recordDateFormat) {
        this.recordDateFormat = recordDateFormat;
    }

    /**
     * @return The current LogEventBroadcaster.
     */
    LogEventBroadcaster getLogEventBroadcaster() {
        return logEventBroadcasterDelegate;
    }

    /**
     * @param logEventBroadcaster The LogEventBroadcaster to be set.
     */
    public void setLogEventBroadcaster(LogEventBroadcaster logEventBroadcaster) {
        this.logEventBroadcasterDelegate = logEventBroadcaster;
    }

    /**
     * @param logEvent LogEvent to inform the listeners of.
     */
    @Override
    public void informLogEventListeners(LogEvent logEvent) {
        if (logEventBroadcasterDelegate != null) {
            logEventBroadcasterDelegate.informLogEventListeners(logEvent);
        }
    }

}

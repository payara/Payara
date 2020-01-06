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
 * file and include the License file at glassfish/legal/LICENSE.txt.
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
 // Portions Copyright [2018-2020] [Payara Foundation and/or its affiliates]
package fish.payara.enterprise.server.logging;

import com.sun.enterprise.server.logging.EnhancedLogRecord;
import com.sun.enterprise.server.logging.ExcludeFieldsSupport;
import com.sun.enterprise.server.logging.FormatterDelegate;
import com.sun.enterprise.server.logging.LogEvent;
import com.sun.enterprise.server.logging.LogEventBroadcaster;
import com.sun.enterprise.server.logging.LogEventImpl;
import com.sun.enterprise.server.logging.i18n.MessageResolver;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.logging.ErrorManager;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import javax.json.Json;
import javax.json.JsonObjectBuilder;

import org.glassfish.api.VersionInfo;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;

/**
 * Class for converting a {@link LogRecord} to Json format
 * @since 4.1.1.164
 * @author savage
 */
@Service()
@ContractsProvided({JSONLogFormatter.class, Formatter.class})
@PerLookup
public class JSONLogFormatter extends Formatter implements LogEventBroadcaster {

    private static final String RECORD_NUMBER = "RecordNumber";
    private static final String METHOD_NAME = "MethodName";
    private static final String CLASS_NAME = "ClassName";

    private static final boolean LOG_SOURCE_IN_KEY_VALUE;
    private static final boolean RECORD_NUMBER_IN_KEY_VALUE;

    private final ServiceLocator habitat = Globals.getDefaultBaseServiceLocator();

    private final MessageResolver messageResolver;

    private FormatterDelegate _delegate = null;

    static {
        String logSource = System.getProperty("com.sun.aas.logging.keyvalue.logsource");
        LOG_SOURCE_IN_KEY_VALUE = "true".equals(logSource);
        String recordCount = System.getProperty("com.sun.aas.logging.keyvalue.recordnumber");
        RECORD_NUMBER_IN_KEY_VALUE = "true".equals(recordCount);
    }

    private long recordNumber = 0;
    private DateTimeFormatter recordDateFormat;

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
    private String USER_ID_KEY = "UserId";
    private String ECID_KEY = "ECId";
    private String LEVEL_VALUE_KEY = "LevelValue";
    private String TIME_MILLIS_KEY = "TimeMillis";
    private String MESSAGE_ID_KEY = "MessageID";
    private String LOG_MESSAGE_KEY = "LogMessage";
    private String THROWABLE_KEY = "Throwable";

    private final ExcludeFieldsSupport excludeFieldsSupport = new ExcludeFieldsSupport();
    private LogEventBroadcaster logEventBroadcasterDelegate;
    private String productId = "";

    /**
     * For backwards compatibility with log format for pre-182
     * @deprecated
     */
    @Deprecated
    private static final String PAYARA_JSONLOGFORMATTER_UNDERSCORE="fish.payara.deprecated.jsonlogformatter.underscoreprefix";

    public JSONLogFormatter() {
        super();
        messageResolver = new MessageResolver();

        LogManager logManager = LogManager.getLogManager();
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
            USER_ID_KEY = "_" + USER_ID_KEY;
            ECID_KEY = "_" + ECID_KEY;
            LEVEL_VALUE_KEY = "_" + LEVEL_VALUE_KEY;
            TIME_MILLIS_KEY = "_" + TIME_MILLIS_KEY;
            MESSAGE_ID_KEY = "_" + MESSAGE_ID_KEY;
            LOG_MESSAGE_KEY = "_" + LOG_MESSAGE_KEY;
            THROWABLE_KEY = "_" + THROWABLE_KEY;
        }
    }

    public JSONLogFormatter(FormatterDelegate delegate) {
        this();
        _delegate = delegate;
    }

    public void setDelegate(FormatterDelegate delegate) {
        _delegate = delegate;
    }

    @Override
    public String format(LogRecord record) {
        return formatLogRecord(record);
    }

    @Override
    public String formatMessage(LogRecord record) {
        return formatLogRecord(record);
    }

    /**
     * Payara can override this to specify product version.
     * @return The string value of the product id.
     */
    protected String getProductId() {
        if (habitat != null) {
            VersionInfo version = habitat.getService(VersionInfo.class);
            if (productId.isEmpty() && version != null) {
                StringBuilder builder = new StringBuilder();
                builder.append(version.getAbbreviatedProductName());
                builder.append(' ');
                builder.append(version.getVersionPrefix());
                builder.append(version.getMajorVersion());
                builder.append('.');
                builder.append(version.getMinorVersion());
                productId = builder.toString();
            }
        }
        return productId;
    }

    /**
     * @param record The record to format.
     * @return The JSON formatted record.
     */
    private String formatLogRecord(LogRecord record) {
        return formatEnhancedLogRecord(messageResolver.resolve(record));
    }

    private String formatEnhancedLogRecord(final EnhancedLogRecord record) {
        try {
            LogEventImpl logEvent = new LogEventImpl();
            JsonObjectBuilder eventObject = Json.createObjectBuilder();

            final Instant time = Instant.ofEpochMilli(record.getMillis());
            String timestampValue = recordDateFormat.format(time);
            logEvent.setTimestamp(timestampValue);
            eventObject.add(TIMESTAMP_KEY, timestampValue);

            Level eventLevel = record.getLevel();
            logEvent.setLevel(eventLevel.getName());
            eventObject.add(LOG_LEVEL_KEY, eventLevel.getLocalizedName());

            productId = getProductId();
            logEvent.setComponentId(productId);
            eventObject.add(PRODUCT_ID_KEY, productId);

            String loggerName = record.getLoggerName();
            if (null == loggerName) {
                loggerName = "";
            }

            logEvent.setLogger(loggerName);
            eventObject.add(LOGGER_NAME_KEY, loggerName);

            if (!excludeFieldsSupport.isSet(ExcludeFieldsSupport.SupplementalAttribute.TID)) {
                // Thread ID
                int threadId = record.getThreadID();
                logEvent.setThreadId(threadId);
                eventObject.add(THREAD_ID_KEY, String.valueOf(threadId));

                String threadName = record.getThreadName();
                logEvent.setThreadName(threadName);
                eventObject.add(THREAD_NAME_KEY, threadName);
            }

            if (!excludeFieldsSupport.isSet(ExcludeFieldsSupport.SupplementalAttribute.USERID)) {
                String userId = logEvent.getUser();
                if (null != userId && !userId.isEmpty()) {
                    eventObject.add(USER_ID_KEY, userId);
                }
            }

            if (!excludeFieldsSupport.isSet(ExcludeFieldsSupport.SupplementalAttribute.ECID)) {
                String ecid = logEvent.getECId();
                if (null != ecid && !ecid.isEmpty()) {
                    eventObject.add(ECID_KEY, ecid);
                }
            }

            if (!excludeFieldsSupport.isSet(ExcludeFieldsSupport.SupplementalAttribute.TIME_MILLIS)) {
                long timestamp = record.getMillis();
                logEvent.setTimeMillis(timestamp);
                eventObject.add(TIME_MILLIS_KEY, String.valueOf(timestamp));
            }

            Level level = record.getLevel();
            if (!excludeFieldsSupport.isSet(ExcludeFieldsSupport.SupplementalAttribute.LEVEL_VALUE)) {
                int levelValue = level.intValue();
                logEvent.setLevelValue(levelValue);
                eventObject.add(LEVEL_VALUE_KEY, String.valueOf(levelValue));
            }

            String messageId = record.getMessageKey();
            if (messageId != null && !messageId.isEmpty()) {
                logEvent.setMessageId(messageId);
                eventObject.add(MESSAGE_ID_KEY, messageId);
            }

            if (LOG_SOURCE_IN_KEY_VALUE || level.intValue() <= Level.FINE.intValue()) {
                String sourceClassName = record.getSourceClassName();

                if (sourceClassName != null && !sourceClassName.isEmpty()) {
                    logEvent.getSupplementalAttributes().put(CLASS_NAME, sourceClassName);
                    eventObject.add(CLASS_NAME, sourceClassName);
                }

                String sourceMethodName = record.getSourceMethodName();
                if (sourceMethodName != null && !sourceMethodName.isEmpty()) {
                    logEvent.getSupplementalAttributes().put(METHOD_NAME, sourceMethodName);
                    eventObject.add(METHOD_NAME, sourceMethodName);
                }
            }

            /*
             * Add the record number to the entry.
             */
            if (RECORD_NUMBER_IN_KEY_VALUE) {
                recordNumber++;
                logEvent.getSupplementalAttributes().put(RECORD_NUMBER, recordNumber);
                eventObject.add(RECORD_NUMBER, String.valueOf(recordNumber));
            }

            if (_delegate != null) {
                _delegate.format(new StringBuilder(eventObject.toString()), level);
            }

            final String logMessage = record.getMessage();
            if (logMessage == null || logMessage.isEmpty()) {
                final Throwable throwable = record.getThrown();
                if (throwable != null) {
                    JsonObjectBuilder traceObject = Json.createObjectBuilder();
                    if (throwable.getMessage() != null) {
                        traceObject.add(EXCEPTION_KEY, throwable.getMessage());
                    }
                    final String stackTrace = record.getThrownStackTrace();
                    traceObject.add(STACK_TRACE_KEY, stackTrace);
                    logEvent.setMessage(stackTrace);
                    eventObject.add(THROWABLE_KEY, traceObject.build());
                }
            } else {
                final String stackTrace = record.getThrownStackTrace();
                if (stackTrace == null) {
                    logEvent.setMessage(logMessage);
                    eventObject.add(LOG_MESSAGE_KEY, logMessage);
                } else {
                    JsonObjectBuilder traceObject =Json.createObjectBuilder();
                    traceObject.add(EXCEPTION_KEY, logMessage);
                    traceObject.add(STACK_TRACE_KEY, stackTrace);
                    logEvent.setMessage(stackTrace);
                    eventObject.add(THROWABLE_KEY, traceObject.build());
                }
            }

            informLogEventListeners(logEvent);
            return eventObject.build().toString() + LINE_SEPARATOR;

        } catch (Exception ex) {
            new ErrorManager().error("Error in formatting Logrecord", ex, ErrorManager.FORMAT_FAILURE);
            return "";
        }
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
    public void setRecordDateFormat(String format) {
        this.recordDateFormat = format == null ? ISO_OFFSET_DATE_TIME : DateTimeFormatter.ofPattern(format);
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

    /**
     * @param excludeFields Fields to exclude.
     */
    public void setExcludeFields(String excludeFields) {
        this.excludeFieldsSupport.setExcludeFields(excludeFields);
    }
}

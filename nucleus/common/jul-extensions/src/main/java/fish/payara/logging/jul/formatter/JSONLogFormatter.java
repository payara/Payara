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
package fish.payara.logging.jul.formatter;

import fish.payara.logging.jul.cfg.LoggingSystemEnvironment;
import fish.payara.logging.jul.i18n.MessageResolver;
import fish.payara.logging.jul.record.EnhancedLogRecord;
import fish.payara.logging.jul.tracing.PayaraLoggingTracer;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.NANO_OF_SECOND;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;

/**
 * Class for converting a {@link LogRecord} to Json format
 *
 * @author savage
 * @author David Matejcek
 */
public class JSONLogFormatter extends PayaraLogFormatter {

    private static final String RECORD_NUMBER = "RecordNumber";
    private static final String METHOD_NAME = "MethodName";
    private static final String CLASS_NAME = "ClassName";

    private final MessageResolver messageResolver;
    private DateTimeFormatter dateTimeFormatter;

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

    /**
     * For backwards compatibility with log format for pre-182
     * @deprecated remove in Payara 6
     */
    @Deprecated
    private static final String PAYARA_JSONLOGFORMATTER_UNDERSCORE="fish.payara.deprecated.jsonlogformatter.underscoreprefix";

    // This was required, because we need 3 decimal numbers of the second fraction
    // DateTimeFormatter.ISO_LOCAL_DATE_TIME prints just nonzero values
    private static final DateTimeFormatter iSO_LOCAL_TIME = new DateTimeFormatterBuilder()
        .appendValue(HOUR_OF_DAY, 2).appendLiteral(':')
        .appendValue(MINUTE_OF_HOUR, 2).optionalStart().appendLiteral(':')
        .appendValue(SECOND_OF_MINUTE, 2).optionalStart()
        .appendFraction(NANO_OF_SECOND, 3, 3, true)
        .toFormatter(Locale.ROOT);

    private static final DateTimeFormatter ISO_LOCAL_DATE_TIME = new DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .append(DateTimeFormatter.ISO_LOCAL_DATE)
        .appendLiteral('T')
        .append(iSO_LOCAL_TIME)
        .toFormatter(Locale.ROOT);

    private static final DateTimeFormatter DEFAULT_DATETIME_FORMATTER = new DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .append(ISO_LOCAL_DATE_TIME)
        .appendOffsetId()
        .toFormatter(Locale.ROOT);

    public JSONLogFormatter() {
        this.dateTimeFormatter = DEFAULT_DATETIME_FORMATTER;
        this.messageResolver = new MessageResolver();

        final LogManager logManager = LogManager.getLogManager();
        final String underscorePrefix = logManager.getProperty(PAYARA_JSONLOGFORMATTER_UNDERSCORE);
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

        // to validate it can work, especially depends on JSON support
        format(new EnhancedLogRecord(Level.ALL, "msg", false));
    }


    /**
     * @param record The record to format.
     * @return JSON formatted record.
     */
    @Override
    public String formatRecord(final LogRecord record) {
        return formatLogRecord(record);
    }

    private String formatLogRecord(final LogRecord record) {
        return formatEnhancedLogRecord(messageResolver.resolve(record));
    }

    private String formatEnhancedLogRecord(final EnhancedLogRecord record) {
        if (record == null || (record.getMessage() == null && record.getThrown() == null)) {
            // nothing useful in the record.
            return "";
        }
        try {
            final JsonBuilderWrapper json = new JsonBuilderWrapper();
            final String timestampValue = dateTimeFormatter.format(record.getTime());
            json.add(TIMESTAMP_KEY, timestampValue);

            final Level level = record.getLevel();
            json.add(LOG_LEVEL_KEY, level.getLocalizedName());

            final String productId = LoggingSystemEnvironment.getProductId();
            json.add(PRODUCT_ID_KEY, productId);

            json.add(LOGGER_NAME_KEY, record.getLoggerName());

            if (!excludeFieldsSupport.isSet(ExcludeFieldsSupport.SupplementalAttribute.TID)) {
                final int threadId = record.getThreadID();
                json.add(THREAD_ID_KEY, String.valueOf(threadId));

                final String threadName = record.getThreadName();
                json.add(THREAD_NAME_KEY, threadName);
            }

            if (!excludeFieldsSupport.isSet(ExcludeFieldsSupport.SupplementalAttribute.TIME_MILLIS)) {
                final long timestamp = record.getMillis();
                json.add(TIME_MILLIS_KEY, timestamp);
            }

            if (!excludeFieldsSupport.isSet(ExcludeFieldsSupport.SupplementalAttribute.LEVEL_VALUE)) {
                final int levelValue = level.intValue();
                json.add(LEVEL_VALUE_KEY, levelValue);
            }

            final String messageId = record.getMessageKey();
            json.add(MESSAGE_ID_KEY, messageId);

            if (isPrintSource()) {
                final String sourceClassName = record.getSourceClassName();
                if (sourceClassName != null && !sourceClassName.isEmpty()) {
                    json.add(CLASS_NAME, sourceClassName);
                }

                final String sourceMethodName = record.getSourceMethodName();
                if (sourceMethodName != null && !sourceMethodName.isEmpty()) {
                    json.add(METHOD_NAME, sourceMethodName);
                }
            }

            if (isPrintSequenceNumber()) {
                json.add(RECORD_NUMBER, String.valueOf(record.getSequenceNumber()));
            }

            final Object[] parameters = record.getParameters();
            if (parameters != null) {
                for (final Object parameter : parameters) {
                    // FIXME: parameter map: conflicts with same keys in other maps are not resolved!
                    // FIXME: parameters cannot be mapped here, they were already resolved in EnhancedLogRecord.
                    //        possible solution: cfg parameter - release parameters after resolving (default) OR not
                    //        release -> no maps, just strings
                    //        not -> still possible to use original parameters in formatter's thread may be affected by concurrent
                    //        changes of states of objects in the map
                    if (parameter instanceof Map) {
                        for (final Map.Entry<Object, Object> entry : ((Map<Object, Object>) parameter).entrySet()) {
                            // there are implementations that allow <null> keys...
                            final String key = entry.getKey() == null ? "null" : entry.getKey().toString();
                            json.add(key, entry.getValue());
                        }
                    }
                }
            }

            json.add(LOG_MESSAGE_KEY, record.getMessage());
            if (record.getThrown() != null) {
                final JsonBuilderWrapper traceObject = new JsonBuilderWrapper();
                final String exceptionMessage = record.getThrown().getMessage();
                if (exceptionMessage != null) {
                    traceObject.add(EXCEPTION_KEY, exceptionMessage);
                }
                final String stackTrace = record.getThrownStackTrace();
                if (stackTrace != null) {
                    traceObject.add(STACK_TRACE_KEY, stackTrace);
                }
                json.add(THROWABLE_KEY, traceObject.toJsonObject());
            }

            final String string = json.toString();
            return string == null || string.isEmpty() ? "" : string + LINE_SEPARATOR;
        } catch (final Exception e) {
            PayaraLoggingTracer.error(getClass(), "Error in formatting Logrecord", e);
            return record.getMessage();
        }
    }

    /**
     * @return The date format for the record.
     */
    public final String getDateTimeFormatter() {
        return dateTimeFormatter.toString();
    }

    /**
     * @param format The date format to set for records.
     */
    public void setDateTimeFormatter(final String format) {
        this.dateTimeFormatter = format == null ? ISO_OFFSET_DATE_TIME : DateTimeFormatter.ofPattern(format);
    }


    /**
     * @param excludeFields Fields to exclude.
     */
    public void setExcludeFields(final String excludeFields) {
        this.excludeFieldsSupport.setExcludeFields(excludeFields);
    }


    private static final class JsonBuilderWrapper {
        private final JsonObjectBuilder builder;

        public JsonBuilderWrapper() {
            this.builder = Json.createObjectBuilder();
        }

        JsonBuilderWrapper add(final String key, final JsonObject value) {
            if (key == null) {
                return this;
            }
            this.builder.add(key, value);
            return this;
        }
        JsonBuilderWrapper add(final String key, final Object value) {
            if (key == null || value == null) {
                return this;
            }
            final String stringValue = value.toString();
            if (stringValue != null) {
                this.builder.add(key, stringValue);
            }
            return this;
        }

        public JsonObject toJsonObject() {
            return this.builder.build();
        }

        @Override
        public String toString() {
            return toJsonObject().toString();
        }
    }
}

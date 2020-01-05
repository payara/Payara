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

package com.sun.enterprise.server.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.ErrorManager;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.glassfish.api.VersionInfo;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;

/**
 * UniformLogFormatter conforms to the logging format defined by the
 * Log Working Group in Java Webservices Org.
 * The specified format is
 * "[#|DATETIME|LOG_LEVEL|PRODUCT_ID|LOGGER NAME|OPTIONAL KEY VALUE PAIRS|MESSAGE|#]\n"
 *
 * @author Hemanth Puttaswamy
 */
@Service()
@ContractsProvided({UniformLogFormatter.class, Formatter.class})
@PerLookup
public class UniformLogFormatter extends AnsiColorFormatter implements LogEventBroadcaster {

    private static final ZoneId TIME_ZONE = ZoneId.systemDefault();
    private static final String RECORD_NUMBER = "RecordNumber";
    private static final String METHOD_NAME = "MethodName";
    private static final String CLASS_NAME = "ClassName";

    private final ServiceLocator habitat = Globals.getDefaultBaseServiceLocator();

    private final LogManager logManager;

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    private static final boolean LOG_SOURCE_IN_KEY_VALUE;
    private static final boolean RECORD_NUMBER_IN_KEY_VALUE;

    private FormatterDelegate _delegate = null;

    static {
        String logSource = System.getProperty("com.sun.aas.logging.keyvalue.logsource");
        LOG_SOURCE_IN_KEY_VALUE = "true".equals(logSource);
        String recordCount = System.getProperty("com.sun.aas.logging.keyvalue.recordnumber");
        RECORD_NUMBER_IN_KEY_VALUE = "true".equals(recordCount);
    }

    private final AtomicLong recordNumber = new AtomicLong();
    private String recordBeginMarker;
    private String recordEndMarker;
    private String recordFieldSeparator;
    private DateTimeFormatter recordDateFormat;

    private static final String RECORD_BEGIN_MARKER = "[#|";
    private static final String RECORD_END_MARKER = "|#]" + LINE_SEPARATOR;
    private static final char FIELD_SEPARATOR = '|';
    public static final char NVPAIR_SEPARATOR = ';';
    public static final char NV_SEPARATOR = '=';
    private static final String INDENT = "  ";

    private LogEventBroadcaster logEventBroadcasterDelegate;
    private boolean multiLineMode;
    private final ExcludeFieldsSupport excludeFieldsSupport = new ExcludeFieldsSupport();
    private String productId = "";

    public UniformLogFormatter() {
        logManager = LogManager.getLogManager();
        recordDateFormat = ISO_OFFSET_DATE_TIME;
    }

    @Override
    public String format(LogRecord record) {
        return uniformLogFormat(record);
    }

    @Override
    public String formatMessage(LogRecord record) {
        return uniformLogFormat(record);
    }

    /**
     * GlassFish can override to specify their product version
     */
    protected String getProductId() {
        if (habitat != null) {
            VersionInfo versionInfo = habitat.getService(VersionInfo.class);
            if (productId.isEmpty() && versionInfo != null) {
                StringBuilder sb = new StringBuilder();
                sb.append(versionInfo.getAbbreviatedProductName());
                sb.append(' ');
                sb.append(versionInfo.getVersionPrefix());
                sb.append(versionInfo.getMajorVersion());
                sb.append('.');
                sb.append(versionInfo.getMinorVersion());
                sb.append('.');
                sb.append(versionInfo.getUpdateVersion());
                productId = sb.toString();
            }
        }
        return productId;
    }

    /**
     * Sun One Appserver SE/EE? can override to specify their product specific
     * key value pairs.
     */
    protected void getNameValuePairs(StringBuilder buf, LogRecord record) {

        Object[] parameters = record.getParameters();
        if ((parameters == null) || (parameters.length == 0)) {
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
            new ErrorManager().error(
                    "Error in extracting Name Value Pairs", e,
                    ErrorManager.FORMAT_FAILURE);
        }
    }

    /**
     * Note: This method is not synchronized, we are assuming that the
     * synchronization will happen at the Log Handler.publish( ) method.
     */
    private String uniformLogFormat(LogRecord record) {
        try {
            LogEventImpl logEvent = new LogEventImpl();
            final OffsetDateTime time = OffsetDateTime.ofInstant(Instant.ofEpochMilli(record.getMillis()), TIME_ZONE);
            final StringBuilder recordBuffer = new StringBuilder(256);
            recordBuffer.append(getRecordBeginMarker() != null ? getRecordBeginMarker() : RECORD_BEGIN_MARKER);
            String timestamp = recordDateFormat.format(time);
            logEvent.setTimestamp(timestamp);
            recordBuffer.append(timestamp);
            if (color()) {
                recordBuffer.append(getColor(record.getLevel()));
            }
            recordBuffer.append(getRecordFieldSeparator() != null ? getRecordFieldSeparator() : FIELD_SEPARATOR);

            logEvent.setLevel(record.getLevel().getName());
            recordBuffer.append(record.getLevel().getLocalizedName()).append(getRecordFieldSeparator() != null ? getRecordFieldSeparator() : FIELD_SEPARATOR);
            if (color()) {
                recordBuffer.append(getReset());
            }
            String compId = getProductId();
            logEvent.setComponentId(compId);
            recordBuffer.append(compId).append(getRecordFieldSeparator() != null ? getRecordFieldSeparator() : FIELD_SEPARATOR);

            String loggerName = record.getLoggerName();
            loggerName = loggerName == null ? "" : loggerName;
            logEvent.setLogger(loggerName);
            if (color()) {
                recordBuffer.append(getLoggerColor());
            }
            recordBuffer.append(loggerName).append(getRecordFieldSeparator() != null ? getRecordFieldSeparator() : FIELD_SEPARATOR);
            if (color()) {
                recordBuffer.append(getReset());
            }
            if (!excludeFieldsSupport.isSet(ExcludeFieldsSupport.SupplementalAttribute.TID)) {
                recordBuffer.append("_ThreadID").append(NV_SEPARATOR);
                logEvent.setThreadId(record.getThreadID());
                recordBuffer.append(record.getThreadID()).append(NVPAIR_SEPARATOR);
                recordBuffer.append("_ThreadName").append(NV_SEPARATOR);
                String threadName;
                if (record instanceof EnhancedLogRecord) {
                    threadName = ((EnhancedLogRecord) record).getThreadName();
                } else {
                    threadName = Thread.currentThread().getName();
                }
                logEvent.setThreadName(threadName);
                recordBuffer.append(threadName);
                recordBuffer.append(NVPAIR_SEPARATOR);
            }

            if (!excludeFieldsSupport.isSet(ExcludeFieldsSupport.SupplementalAttribute.USERID)) {
                String user = logEvent.getUser();
                if (user != null && !user.isEmpty()) {
                    recordBuffer.append("_UserId").append(NV_SEPARATOR);
                    recordBuffer.append(user);
                    recordBuffer.append(NVPAIR_SEPARATOR);
                }
            }

            if (!excludeFieldsSupport.isSet(ExcludeFieldsSupport.SupplementalAttribute.ECID)) {
                String ecid = logEvent.getECId();
                if (ecid != null && !ecid.isEmpty()) {
                    recordBuffer.append("_ECId").append(NV_SEPARATOR);
                    recordBuffer.append(ecid);
                    recordBuffer.append(NVPAIR_SEPARATOR);
                }
            }

            // Include the raw long time stamp value in the log
            if (!excludeFieldsSupport.isSet(ExcludeFieldsSupport.SupplementalAttribute.TIME_MILLIS)) {
                recordBuffer.append("_TimeMillis").append(NV_SEPARATOR);
                logEvent.setTimeMillis(record.getMillis());
                recordBuffer.append(record.getMillis()).append(NVPAIR_SEPARATOR);
            }

            // Include the integer level value in the log
            Level level = record.getLevel();
            if (!excludeFieldsSupport.isSet(ExcludeFieldsSupport.SupplementalAttribute.LEVEL_VALUE)) {
                recordBuffer.append("_LevelValue").append(NV_SEPARATOR);
                int levelValue = level.intValue();
                logEvent.setLevelValue(levelValue);
                recordBuffer.append(levelValue).append(NVPAIR_SEPARATOR);
            }

            String msgId = getMessageId(record);
            if (msgId != null && !msgId.isEmpty()) {
                logEvent.setMessageId(msgId);
                recordBuffer.append("_MessageID").append(NV_SEPARATOR);
                recordBuffer.append(msgId).append(NVPAIR_SEPARATOR);
            }

            // See 6316018. ClassName and MethodName information should be
            // included for FINER and FINEST log levels.
            if (LOG_SOURCE_IN_KEY_VALUE ||
                    (level.intValue() <= Level.FINE.intValue())) {
                String sourceClassName = record.getSourceClassName();
                // sourceClassName = (sourceClassName == null) ? "" : sourceClassName;
                if (sourceClassName != null && !sourceClassName.isEmpty()) {
                    recordBuffer.append(CLASS_NAME).append(NV_SEPARATOR);
                    logEvent.getSupplementalAttributes().put(CLASS_NAME, sourceClassName);
                    recordBuffer.append(sourceClassName);
                    recordBuffer.append(NVPAIR_SEPARATOR);
                }

                String sourceMethodName = record.getSourceMethodName();
                // sourceMethodName = (sourceMethodName == null) ? "" : sourceMethodName;
                if (sourceMethodName != null && !sourceMethodName.isEmpty()) {
                    recordBuffer.append(METHOD_NAME).append(NV_SEPARATOR);
                    logEvent.getSupplementalAttributes().put(METHOD_NAME, sourceMethodName);
                    recordBuffer.append(sourceMethodName);
                    recordBuffer.append(NVPAIR_SEPARATOR);
                }
            }

            if (RECORD_NUMBER_IN_KEY_VALUE) {
                final long recNumber = recordNumber.getAndIncrement();
                recordBuffer.append(RECORD_NUMBER).append(NV_SEPARATOR);
                logEvent.getSupplementalAttributes().put(RECORD_NUMBER, recNumber);
                recordBuffer.append(recNumber).append(NVPAIR_SEPARATOR);
            }

            // Not needed as per the current logging message format. Fixing bug 16849.
            // getNameValuePairs(recordBuffer, record);

            if (_delegate != null) {
                _delegate.format(recordBuffer, level);
            }

            recordBuffer.append(getRecordFieldSeparator() != null ? getRecordFieldSeparator() : FIELD_SEPARATOR);

            if (multiLineMode) {
                recordBuffer.append(LINE_SEPARATOR);
                recordBuffer.append(INDENT);
            }
            String logMessage = record.getMessage();
            // in some case no msg is passed to the logger API. We assume that either:
            // 1. A message was logged in a previous logger call and now just the exception is logged.
            // 2. There is a bug in the calling code causing the message to be missing.
            if (logMessage == null || logMessage.trim().equals("")) {

                if (record.getThrown() != null) {
                    // case 1: Just log the exception instead of a message
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    record.getThrown().printStackTrace(pw);
                    pw.close();
                    logMessage = sw.toString();
                    sw.close();
                } else {
                    // GLASSFISH-18816: Suppress noise.
                    logMessage = "";
                }
                logEvent.setMessage(logMessage);
                recordBuffer.append(logMessage);
            } else {
                if (logMessage.indexOf("{0") >= 0 && logMessage.contains("}") && record.getParameters() != null) {
                    // If we find {0} or {1} etc., in the message, then it's most
                    // likely finer level messages for Method Entry, Exit etc.,
                    logMessage = MessageFormat.format(logMessage, record.getParameters());
                } else {
                    ResourceBundle rb = getResourceBundle(record.getLoggerName());
                    if (rb != null) {
                        try {
                            logMessage = MessageFormat.format(rb.getString(logMessage), record.getParameters());
                        } catch (MissingResourceException e) {
                            // If we don't find an entry, then we are covered
                            // because the logMessage is initialized already
                        }
                    }
                }

                StringBuilder logMessageBuffer = new StringBuilder();
                logMessageBuffer.append(logMessage);

                Throwable throwable = getThrowable(record);
                if (throwable != null) {
                    logMessageBuffer.append(LINE_SEPARATOR);
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    throwable.printStackTrace(pw);
                    pw.close();
                    logMessageBuffer.append(sw.toString());
                    sw.close();
                }
                logMessage = logMessageBuffer.toString();
                logEvent.setMessage(logMessage);
                recordBuffer.append(logMessage);
            }
            recordBuffer.append(getRecordEndMarker() == null ? RECORD_END_MARKER : getRecordEndMarker());
            recordBuffer.append(LINE_SEPARATOR).append(LINE_SEPARATOR);
            informLogEventListeners(logEvent);
            return recordBuffer.toString();

        } catch (Exception ex) {
            new ErrorManager().error("Error in formatting Logrecord", ex, ErrorManager.FORMAT_FAILURE);
            // We've already notified the exception, the following
            // return is to keep javac happy
            return "";
        }
    }

    static String getMessageId(LogRecord lr) {
        if (lr instanceof EnhancedLogRecord) {
            return EnhancedLogRecord.class.cast(lr).getMessageKey();
        }
        final String msg = lr.getMessage();
        if (msg == null || msg.isEmpty()) {
            return null;
        }
        final ResourceBundle rb = lr.getResourceBundle();
        if (rb == null) {
            return null;
        }
        try {
            final String msgBody = rb.getString(msg);
            return msgBody.isEmpty() ? null : msgBody;
        } catch (MissingResourceException e) {
            return msg;
        }
    }

    static Throwable getThrowable(LogRecord record) {
        return record.getThrown();
    }

    private synchronized ResourceBundle getResourceBundle(String loggerName) {
        if (loggerName == null) {
            return null;
        }
        final Logger logger = logManager.getLogger(loggerName);
        if (logger == null) {
            return null;
        }
        return logger.getResourceBundle();
    }

    public UniformLogFormatter(FormatterDelegate delegate) {
        this();
        _delegate = delegate;
    }

    public void setDelegate(FormatterDelegate delegate) {
        _delegate = delegate;
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
     * @return the logEventBroadcaster
     */
    LogEventBroadcaster getLogEventBroadcaster() {
        return logEventBroadcasterDelegate;
    }

    /**
     * @param logEventBroadcaster the logEventBroadcaster to set
     */
    void setLogEventBroadcaster(LogEventBroadcaster logEventBroadcaster) {
        this.logEventBroadcasterDelegate = logEventBroadcaster;
    }

    @Override
    public void informLogEventListeners(LogEvent logEvent) {
        if (logEventBroadcasterDelegate != null) {
            logEventBroadcasterDelegate.informLogEventListeners(logEvent);
        }
    }

    /**
     * @param multiLineMode the multiLineMode to set
     */
    void setMultiLineMode(boolean value) {
        multiLineMode = value;
    }

    /**
     * @param excludeFields the excludeFields to set
     */
    void setExcludeFields(String excludeFields) {
        this.excludeFieldsSupport.setExcludeFields(excludeFields);
    }
}

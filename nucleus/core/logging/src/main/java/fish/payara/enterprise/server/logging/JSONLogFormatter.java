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
package fish.payara.enterprise.server.logging;

import com.sun.common.util.logging.GFLogRecord;
import com.sun.enterprise.server.logging.AnsiColorFormatter;
import com.sun.enterprise.server.logging.ExcludeFieldsSupport;
import com.sun.enterprise.server.logging.FormatterDelegate;
import com.sun.enterprise.server.logging.LogEvent;
import com.sun.enterprise.server.logging.LogEventBroadcaster;
import com.sun.enterprise.server.logging.LogEventImpl;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.ErrorManager;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import org.glassfish.api.VersionInfo;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

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

    private final ServiceLocator habitat = Globals.getDefaultBaseServiceLocator();

    private HashMap loggerResourceBundleTable;
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

        String recordCount = System.getProperty(
                "com.sun.aas.logging.keyvalue.recordnumber");
        if ((recordCount != null) 
                && (recordCount.equals("true"))) {
            RECORD_NUMBER_IN_KEY_VALUE = true;
        }
    }

    private long recordNumber = 0;
    private String recordDateFormat;

    // Event separator
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    // String values for field keys
    private static final String TIMESTAMP_KEY = "_Timestamp";
    private static final String LOG_LEVEL_KEY = "_Level";
    private static final String PRODUCT_ID_KEY = "_Version";
    private static final String LOGGER_NAME_KEY = "_LoggerName";
    // String values for exception keys
    private static final String EXCEPTION_KEY = "_Exception";
    private static final String STACK_TRACE_KEY = "_StackTrace";
    // String values for thread excludable keys
    private static final String THREAD_ID_KEY = "_ThreadID";
    private static final String THREAD_NAME_KEY = "_ThreadName";
    private static final String USER_ID_KEY = "_UserId";
    private static final String ECID_KEY = "_ECId";
    private static final String LEVEL_VALUE_KEY = "_LevelValue";
    private static final String TIME_MILLIS_KEY = "_TimeMillis";
    private static final String MESSAGE_ID_KEY = "_MessageID";
    private static final String LOG_MESSAGE_KEY = "_LogMessage";

    private static final String RFC3339_DATE_FORMAT = 
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    
    private final ExcludeFieldsSupport excludeFieldsSupport = new ExcludeFieldsSupport();
    private LogEventBroadcaster logEventBroadcasterDelegate;
    private String productId = "";

    public JSONLogFormatter() {
        super();
        loggerResourceBundleTable = new HashMap();
        logManager = LogManager.getLogManager();
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
        return jsonLogFormat(record);
    }

    @Override
    public String formatMessage(LogRecord record) {
        return jsonLogFormat(record);
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
            StringBuilder levelBuilder = new StringBuilder();
            levelBuilder.append(eventLevel.getLocalizedName());
            eventObject.add(LOG_LEVEL_KEY, levelBuilder.toString());

            /*
             * Get the product id and append to object.
             */
            productId = getProductId();
            logEvent.setComponentId(productId);
            eventObject.add(PRODUCT_ID_KEY, productId);

            /*
             * Get the logger name and append to object.
             */ 
            String loggerName = record.getLoggerName();
            
            if (null == loggerName) {
                loggerName = "";
            }

            logEvent.setLogger(loggerName);            
            StringBuilder loggerBuilder = new StringBuilder();
            loggerBuilder.append(loggerName);           
            eventObject.add(LOGGER_NAME_KEY, loggerBuilder.toString());

            /*
             * Get thread information and append to object if not excluded.
             */
            if (!excludeFieldsSupport.isSet(ExcludeFieldsSupport
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
             * Get user id and append if not excluded and exists with value.
             */
            if (!excludeFieldsSupport.isSet(ExcludeFieldsSupport
                    .SupplementalAttribute.USERID)) {
                String userId = logEvent.getUser();
                if (null != userId && !userId.isEmpty()) {
                    eventObject.add(USER_ID_KEY, userId);
                }
            }

            /*
             * Get ec id and append if not excluded and exists with value.
             */ 
            if (!excludeFieldsSupport.isSet(ExcludeFieldsSupport
                    .SupplementalAttribute.ECID)) {
                String ecid = logEvent.getECId();
                if (null != ecid && !ecid.isEmpty()) {
                    eventObject.add(ECID_KEY, ecid);
                }
            }

            /*
             * Get millis time for log entry timestamp
             */
            if (!excludeFieldsSupport.isSet(ExcludeFieldsSupport
                    .SupplementalAttribute.TIME_MILLIS)) {
                Long timestamp = record.getMillis();
                logEvent.setTimeMillis(timestamp);
                eventObject.add(TIME_MILLIS_KEY, String.valueOf(timestamp));
            }

            /*
             * Include the integer value for log level 
             */
            Level level = record.getLevel();
            if (!excludeFieldsSupport.isSet(ExcludeFieldsSupport
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

            String logMessage = record.getMessage();

            if (null == logMessage || logMessage.trim().equals("")) {
                Throwable throwable = record.getThrown();
                if (null != throwable) {
                    try (StringWriter stringWriter = new StringWriter(); 
                         PrintWriter printWriter = new PrintWriter(stringWriter)) {
                        JsonObjectBuilder traceObject = Json.createObjectBuilder();
                        throwable.printStackTrace(printWriter);
                        logMessage = stringWriter.toString();
                        traceObject.add(EXCEPTION_KEY, throwable.getMessage());
                        traceObject.add(STACK_TRACE_KEY, logMessage);
                        logEvent.setMessage(logMessage);
                        eventObject.add(LOG_MESSAGE_KEY, traceObject.build());
                    }
                } 
            } else {
                if (logMessage.contains("{0") && logMessage.contains("}") 
                        && null != record.getParameters()) {
                    logMessage = MessageFormat
                            .format(logMessage, record.getParameters());
                } else {
                    ResourceBundle bundle = getResourceBundle(record.getLoggerName());
                    if (null != bundle) {
                        try {
                            logMessage = MessageFormat.format(bundle
                                .getString(logMessage),
                                record.getParameters());
                        } catch (MissingResourceException ex) {
                            // Leave logMessage as it is because it already has 
                            // an exception message
                        }
                    }
                }
                
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
                        eventObject.add(LOG_MESSAGE_KEY, traceObject.build());
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

        ResourceBundle bundle = (ResourceBundle) 
                loggerResourceBundleTable.get(loggerName);

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

    /**
     * @param excludeFields Fields to exclude.
     */
    public void setExcludeFields(String excludeFields) {
        this.excludeFieldsSupport.setExcludeFields(excludeFields);
    }
}

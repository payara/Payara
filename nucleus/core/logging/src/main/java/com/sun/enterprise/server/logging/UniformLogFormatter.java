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

package com.sun.enterprise.server.logging;


import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.ErrorManager;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

import org.glassfish.api.VersionInfo;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

/**
 * UniformLogFormatter conforms to the logging format defined by the
 * Log Working Group in Java Webservices Org.
 * The specified format is
 * "[#|DATETIME|LOG_LEVEL|PRODUCT_ID|LOGGER NAME|OPTIONAL KEY VALUE PAIRS|
 * MESSAGE|#]\n"
 *
 * @author Hemanth Puttaswamy
 *         <p/>
 *         TODO:
 *         1. Performance improvement. We can Cache the LOG_LEVEL|PRODUCT_ID strings
 *         and minimize the concatenations and revisit for more performance
 *         improvements
 *         2. Need to use Product Name and Version based on the version string
 *         that is part of the product.
 *         3. Stress testing
 *         4. If there is a Map as the last element, need to scan the message to
 *         distinguish key values with the message argument.
 */
@Service()
@ContractsProvided({UniformLogFormatter.class, Formatter.class})
@PerLookup
public class UniformLogFormatter extends Formatter implements LogEventBroadcaster {
    
    private static final String RECORD_NUMBER = "RecordNumber";
    private static final String METHOD_NAME = "MethodName";
    private static final String CLASS_NAME = "ClassName";
    
    private ServiceLocator habitat = Globals.getDefaultBaseServiceLocator();        

    // loggerResourceBundleTable caches references to all the ResourceBundle
    // and can be searched using the LoggerName as the key 
    private HashMap loggerResourceBundleTable;
    private LogManager logManager;
    // A Dummy Container Date Object is used to format the date
    private Date date = new Date();

    private static boolean LOG_SOURCE_IN_KEY_VALUE = false;

    private static boolean RECORD_NUMBER_IN_KEY_VALUE = false;

    private FormatterDelegate _delegate = null;
    
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

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private String recordBeginMarker;
    private String recordEndMarker;
    private String recordFieldSeparator;
    private String recordDateFormat;

    private static final String RECORD_BEGIN_MARKER = "[#|";
    private static final String RECORD_END_MARKER = "|#]" + LINE_SEPARATOR;
    private static final char FIELD_SEPARATOR = '|';
    public static final char NVPAIR_SEPARATOR = ';';
    public static final char NV_SEPARATOR = '=';

    private static final String RFC_3339_DATE_FORMAT =
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    private LogEventBroadcaster logEventBroadcasterDelegate;
    
    private boolean multiLineMode;
    
    private static final String INDENT = "  ";
    
    private ExcludeFieldsSupport excludeFieldsSupport = new ExcludeFieldsSupport();
    
    private String productId = "";
    
    public UniformLogFormatter() {
        super();
        loggerResourceBundleTable = new HashMap();
        logManager = LogManager.getLogManager();
    }

    public UniformLogFormatter(FormatterDelegate delegate) {
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
//                } else {
//                    buf.append(obj.toString()).append(NVPAIR_SEPARATOR);
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
            
            SimpleDateFormat dateFormatter = new SimpleDateFormat(getRecordDateFormat() != null ? getRecordDateFormat() : RFC_3339_DATE_FORMAT);

            StringBuilder recordBuffer = new StringBuilder(getRecordBeginMarker() != null ? getRecordBeginMarker() : RECORD_BEGIN_MARKER);
            // The following operations are to format the date and time in a
            // human readable  format.
            // _REVISIT_: Use HiResolution timer to analyze the number of
            // Microseconds spent on formatting date object
            date.setTime(record.getMillis());
            String timestamp = dateFormatter.format(date);
            logEvent.setTimestamp(timestamp);
            recordBuffer.append(timestamp);
            recordBuffer.append(getRecordFieldSeparator() != null ? getRecordFieldSeparator() : FIELD_SEPARATOR);

            logEvent.setLevel(record.getLevel().getName());
            recordBuffer.append(record.getLevel()).append(getRecordFieldSeparator() != null ? getRecordFieldSeparator() : FIELD_SEPARATOR);
            
            String compId = getProductId();
            logEvent.setComponentId(compId);
            recordBuffer.append(compId).append(getRecordFieldSeparator() != null ? getRecordFieldSeparator() : FIELD_SEPARATOR);
            
            String loggerName = record.getLoggerName();
            loggerName = (loggerName == null) ? "" : loggerName;
            logEvent.setLogger(loggerName);
            recordBuffer.append(loggerName).append(getRecordFieldSeparator() != null ? getRecordFieldSeparator() : FIELD_SEPARATOR);

            if (!excludeFieldsSupport.isSet(ExcludeFieldsSupport.SupplementalAttribute.TID)) {
                recordBuffer.append("_ThreadID").append(NV_SEPARATOR);
                logEvent.setThreadId(record.getThreadID());
                recordBuffer.append(record.getThreadID()).append(NVPAIR_SEPARATOR);
                recordBuffer.append("_ThreadName").append(NV_SEPARATOR);
                String threadName;
                if (record instanceof GFLogRecord) {
                  threadName = ((GFLogRecord)record).getThreadName();
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
                recordNumber++;
                recordBuffer.append(RECORD_NUMBER).append(NV_SEPARATOR);
                logEvent.getSupplementalAttributes().put(RECORD_NUMBER, recordNumber);
                recordBuffer.append(recordNumber).append(NVPAIR_SEPARATOR);
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
                    logMessage = java.text.MessageFormat.format(
                            logMessage, record.getParameters());
                } else {
                    ResourceBundle rb = getResourceBundle(record.getLoggerName());
                    if (rb != null) {
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
                
                StringBuffer logMessageBuffer = new StringBuffer();
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
            recordBuffer.append(getRecordEndMarker() != null ? getRecordEndMarker() : RECORD_END_MARKER).append(LINE_SEPARATOR).append(LINE_SEPARATOR);
            informLogEventListeners(logEvent);
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

    static String getMessageId(LogRecord lr) {
        String msg = lr.getMessage();
        if (msg != null && !msg.isEmpty()) {
          ResourceBundle rb = lr.getResourceBundle();
          if (rb != null) {        
            if (rb.containsKey(msg)) {
              String msgBody = lr.getResourceBundle().getString(msg);
              if (!msgBody.isEmpty()) {
                return msg;
              }
            }
          }
        }
        return null;
    }
    
    static Throwable getThrowable(LogRecord record) {
        return record.getThrown();
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

    public String getRecordDateFormat() {
        return recordDateFormat;
    }

    public void setRecordDateFormat(String recordDateFormat) {
        this.recordDateFormat = recordDateFormat;
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

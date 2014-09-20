/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.admin.amx.impl.mbean;

import com.sun.common.util.logging.LoggingConfigImpl;
import com.sun.enterprise.server.logging.GFFileHandler;
import com.sun.enterprise.server.logging.diagnostics.MessageIdCatalog;
import com.sun.enterprise.server.logging.logviewer.backend.LogFilter;
import java.io.Serializable;
import java.util.*;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import javax.management.*;
import org.glassfish.admin.amx.core.Util;
import org.glassfish.admin.amx.impl.util.InjectedValues;
import static org.glassfish.admin.amx.logging.LogAnalyzer.*;
import static org.glassfish.admin.amx.logging.LogFileAccess.*;
import static org.glassfish.admin.amx.logging.LogRecordEmitter.*;
import org.glassfish.admin.amx.logging.Logging;
import org.glassfish.admin.amx.util.*;
import org.glassfish.admin.amx.util.jmx.JMXUtil;
import org.glassfish.admin.amx.util.jmx.NotificationBuilder;
import org.glassfish.external.amx.AMXGlassfish;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.server.ServerEnvironmentImpl;
import org.jvnet.hk2.annotations.Service;

//import com.sun.enterprise.server.logging.LoggingImplHook;
/**
 * Implementation of {@link Logging}. <p> The following is a GlassFish V2
 * comment, and needs work for v3:<br> AMX Logging MBean is hooked directly into
 * the logging subsystem via
 * com.sun.enterprise.server.logging.FileandSyslogHandler which uses
 * com.sun.enterprise.server.logging.AMXLoggingHook to instantiate and call an
 * instance of LoggingImpl.
 */
public final class LoggingImpl extends AMXImplBase //implements /*Logging,*/ LoggingImplHook
{

    private final Map<Level, String> mLevelToNotificationTypeMap;
    private final Map<String, NotificationBuilder> mNotificationTypeToNotificationBuilderMap;
    private static final String SERVER_LOG_NAME = "server.log";
    private static final String ACCESS_LOG_NAME = "access.log";
    private final LoggingConfigImpl loggingConfig;
    private final GFFileHandler gfFileHandler;
    private final LogFilter logFilter;
    private final MessageIdCatalog msgIdCatalog;
    private final Logger logger;
    private final ServiceLocator mHabitat;
    final String FILE_SEP;
    private final String mServerName;

    /**
     * Used internally to get the Logging ObjectName for a particular server
     * Logging MBean is a special-case because it needs to load as early as
     * possible.
     */
    public static ObjectName getObjectName(final String serverName) {
        final String requiredProps = Util.makeRequiredProps(Util.deduceType(Logging.class), serverName);
        final String ServerRootMonitorType = "ServerRootMonitor";
        final String parentProp = Util.makeProp(ServerRootMonitorType, serverName);
        final String props = Util.concatenateProps(requiredProps, parentProp);

        return Util.newObjectName(AMXGlassfish.DEFAULT.amxJMXDomain(), props);
    }

    /**
     */
    public LoggingImpl(final ObjectName parent, final String serverName) {
        super(parent, Logging.class);

        mServerName = serverName;
        FILE_SEP = System.getProperty("file.separator");

        mLevelToNotificationTypeMap = initLevelToNotificationTypeMap();
        mNotificationTypeToNotificationBuilderMap = new HashMap<String, NotificationBuilder>();
        final ServerEnvironmentImpl env = InjectedValues.getInstance().getServerEnvironment();
        loggingConfig = new LoggingConfigImpl();
        loggingConfig.setupConfigDir(env.getConfigDirPath(), env.getLibPath());
        msgIdCatalog = new MessageIdCatalog();
        mHabitat = InjectedValues.getInstance().getHabitat();
        gfFileHandler = mHabitat.getService(GFFileHandler.class);
        logFilter = mHabitat.getService(LogFilter.class);
        logger = Logger.getAnonymousLogger();

    }
    /**
     * Hook for subclass to modify anything in MBeanInfo.
     *
     * @Override
     */
    /*
     @Override
     protected MBeanInfo
     postRegisterModifyMBeanInfo( final MBeanInfo info )
     {
     final MBeanOperationInfo[]  ops = info.getOperations();
	    
     final int   idx = JMXUtil.findMBeanOperationInfo( info, "queryServerLog", null);
	    
     final MBeanOperationInfo    op  = ops[idx];
     ops[idx]    = new MBeanOperationInfo( op.getName(), op.getDescription(),
     op.getSignature(), Map.class.getName(),
     MBeanOperationInfo.INFO );
	    
     return JMXUtil.newMBeanInfo( info, ops );
     }
     */
    private static MBeanNotificationInfo[] SELF_NOTIFICATION_INFOS = null;

    /**
     * getMBeanInfo() can be called frequently. By making this static, we avoid
     * needlessly creating new Objects.
     */
    private static synchronized MBeanNotificationInfo[] getSelfNotificationInfos() {
        if (SELF_NOTIFICATION_INFOS == null) {
            final String[] types = SetUtil.toStringArray(ALL_LOG_RECORD_NOTIFICATION_TYPES);
            final MBeanNotificationInfo selfInfo = new MBeanNotificationInfo(
                    types, Notification.class.getName(), "LogRecord notifications");

            SELF_NOTIFICATION_INFOS = new MBeanNotificationInfo[]{selfInfo};
        }
        return (SELF_NOTIFICATION_INFOS);
    }

    public MBeanNotificationInfo[] getNotificationInfo() {
        final MBeanNotificationInfo[] superInfos = super.getNotificationInfo();

        final MBeanNotificationInfo[] all =
                JMXUtil.mergeMBeanNotificationInfos(superInfos, getSelfNotificationInfos());

        return all;
    }

    /**
     * FIXME
     */
    private void unimplemented() {
        throw new RuntimeException("Not implemented.");
    }

    public void setModuleLogLevel(
            final String module,
            final String level) {

        try {
            loggingConfig.setLoggingProperty(module + ".level", level);
        } catch (java.io.IOException e) {
            logger.log(Level.SEVERE, "Can not set module log level");
        }
    }

    public String getModuleLogLevel(final String module) {
        try {
            Map<String, String> props = loggingConfig.getLoggingProperties();
            if (props != null) {
                return props.get(module + ".level");
            }
            return null;
        } catch (java.io.IOException e) {
            logger.log(Level.SEVERE, "Can not get module log level");
            return null;
        }
    }

    public Map<String, String> getLoggingProperties() {
        try {
            Map<String, String> props = loggingConfig.getLoggingProperties();
            return props;
        } catch (java.io.IOException e) {
            logger.log(Level.WARNING, "Can not get module log level");
            return null;
        }
    }

    public void updateLoggingProperties(final Map<String, String> properties) {
        try {
            loggingConfig.updateLoggingProperties(properties);
        } catch (java.io.IOException e) {
            logger.log(Level.WARNING, "Can not get module log level");
        }
    }

    public int getLogLevelListenerCount(final Level logLevel) {
        final String notifType = logLevelToNotificationType(logLevel);

        final int count = getNotificationEmitter().getNotificationTypeListenerCount(notifType);
        return (count);
    }

    public String[] getLogFileKeys() {
        unimplemented();
        return new String[]{SERVER_KEY, ACCESS_KEY};
    }

    public synchronized String[] getLogFileNames(final String key) {
        String[] result = null;

        if (SERVER_KEY.equals(key)) {
        } else {
            throw new IllegalArgumentException(key);
        }

        return result;
    }

    public Map<String, String> getLoggingAttributes() {
        String gfHandler = "com.sun.enterprise.server.logging.GFFileHandler";
        String sysHandler = "com.sun.enterprise.server.logging.SyslogHandler";
        try {
            Map<String, String> props = loggingConfig.getLoggingProperties();
            if (props == null) {
                return null;
            }
            Map<String, String> attributes = new HashMap<String, String>();
            attributes.put(gfHandler + ".file", props.get(gfHandler + ".file"));
            attributes.put(gfHandler + ".rotationTimelimitInMinutes", props.get(gfHandler + ".rotationTimelimitInMinutes"));
            attributes.put(gfHandler + ".rotationLimitInBytes", props.get(gfHandler + ".rotationLimitInBytes"));
            attributes.put(gfHandler + ".logtoConsole", props.get(gfHandler + ".logtoConsole"));
            attributes.put(gfHandler + ".flushFrequency", props.get(gfHandler + ".flushFrequency"));
            attributes.put("handlers", props.get("handlers"));
            attributes.put(sysHandler + ".useSystemLogging", props.get(sysHandler + ".useSystemLogging"));
            return attributes;
        } catch (java.io.IOException e) {
            logger.log(Level.WARNING, "Can not get logging attributes");
            return null;
        }
    }

    public void updateLoggingAttributes(final Map<String, String> properties) {
        try {
            loggingConfig.updateLoggingProperties(properties);
        } catch (java.io.IOException e) {
            logger.log(Level.WARNING, "Can not set logging attributes");
        }
    }

    public synchronized void rotateAllLogFiles() {
        gfFileHandler.rotate();
    }

    public synchronized void rotateLogFile(final String key) {

        if (ACCESS_KEY.equals(key)) {
            throw new IllegalArgumentException("not supported: " + key);
            //getLogMBean().rotateAccessLog();
        } else if (SERVER_KEY.equals(key)) {
            gfFileHandler.rotate();
        } else {
            throw new IllegalArgumentException("" + key);
        }
    }

    private Properties attributesToProps(List<Attribute> attrs) {
        final Properties props = new Properties();

        if (attrs != null) {
            for (Attribute attr : attrs) {
                final Object value = attr.getValue();
                if (value == null) {
                    throw new IllegalArgumentException(attr.getName() + "=" + null);
                }

                props.put(attr.getName(), value.toString());
            }
        }

        return (props);
    }

    private List<Serializable[]> convertQueryResult(final AttributeList queryResult) {
        // extract field descriptions into a String[]
        final AttributeList fieldAttrs = (AttributeList) ((Attribute) queryResult.get(0)).getValue();
        final String[] fieldHeaders = new String[fieldAttrs.size()];
        for (int i = 0; i < fieldHeaders.length; ++i) {
            final Attribute attr = (Attribute) fieldAttrs.get(i);
            fieldHeaders[ i] = (String) attr.getValue();
        }

        final List<List<Serializable>> srcRecords = TypeCast.asList(
                ((Attribute) queryResult.get(1)).getValue());

        // create the new results, making the first Object[] be the field headers
        final List<Serializable[]> results = new ArrayList<Serializable[]>(srcRecords.size());
        results.add(fieldHeaders);

        // extract every record
        for (int recordIdx = 0; recordIdx < srcRecords.size(); ++recordIdx) {
            final List<Serializable> record = srcRecords.get(recordIdx);

            assert (record.size() == fieldHeaders.length);
            final Serializable[] fieldValues = new Serializable[fieldHeaders.length];
            for (int fieldIdx = 0; fieldIdx < fieldValues.length; ++fieldIdx) {
                fieldValues[ fieldIdx] = record.get(fieldIdx);
            }

            results.add(fieldValues);
        }

        return results;
    }

    // code in LogBean.java code in v2
    public List<Serializable[]> queryServerLog(
            String name,
            long startIndex,
            boolean searchForward,
            int maximumNumberOfResults,
            Long fromTime,
            Long toTime,
            String logLevel,
            Set<String> modules,
            List<Attribute> nameValuePairs,
            String anySearch) {
        final List<Serializable[]> result = queryServerLogInternal(
                name, startIndex, searchForward, maximumNumberOfResults,
                fromTime, toTime, logLevel, modules, nameValuePairs, anySearch);
        return result;
    }

    private List<Serializable[]> queryServerLogInternal(
            final String name,
            final long startIndex,
            final boolean searchForward,
            final int maximumNumberOfResults,
            final Long fromTime,
            final Long toTime,
            final String logLevel,
            final Set<String> modules,
            final List<Attribute> nameValuePairs,
            final String anySearch) {

        if (name == null) {
            throw new IllegalArgumentException("use MOST_RECENT_NAME, not null");
        }

        boolean sortAscending = true;
        List<String> moduleList = null;
        if (modules != null) {
            moduleList = ListUtil.newListFromCollection(modules);
        }
        final Properties props = attributesToProps(nameValuePairs);

        String actualName;
        if (MOST_RECENT_NAME.equals(name)) {
            actualName = null;
        } else {
            actualName = name;
        }
        if (!searchForward) {
            sortAscending = false;
        }

        final AttributeList result = logFilter.getLogRecordsUsingQuery(actualName,
                Long.valueOf(startIndex),
                searchForward, sortAscending,
                maximumNumberOfResults,
                fromTime == null ? null
                : new Date(fromTime),
                toTime == null ? null
                : new Date(toTime),
                logLevel, false, moduleList, props, anySearch);


        return convertQueryResult(result);
    }

    public Map<String, Number>[] getErrorInfo() {
        unimplemented();

        final List<Map<String, Object>> infos = null; //getLogMBean().getErrorInformation();

        final Map<String, Number>[] results = TypeCast.asArray(new HashMap[infos.size()]);

        for (int i = 0; i < results.length; ++i) {
            final Map<String, Object> info = infos.get(i);

            assert (info.keySet().size() == 3);

            final Long timestamp = Long.parseLong(info.get(TIMESTAMP_KEY).toString());
            final Integer severeCount = Integer.parseInt(info.get(SEVERE_COUNT_KEY).toString());
            final Integer warningCount = Integer.parseInt(info.get(WARNING_COUNT_KEY).toString());

            final Map<String, Number> item = new HashMap<String, Number>(info.size());
            item.put(TIMESTAMP_KEY, timestamp);
            item.put(SEVERE_COUNT_KEY, severeCount);
            item.put(WARNING_COUNT_KEY, warningCount);

            results[ i] = item;
        }

        return results;
    }
    private static final Integer INTEGER_0 = Integer.valueOf(0);
    private static final Map<String, Integer> EMPTY_ERROR_DISTRIBUTION_MAP =
            Collections.emptyMap();
    private static final Set<String> LEGAL_DISTRIBUTION_LEVELS =
            SetUtil.newUnmodifiableStringSet(
            Level.SEVERE.toString(), Level.WARNING.toString());

    public Map<String, Integer> getErrorDistribution(long timestamp, String level) {
        if (!LEGAL_DISTRIBUTION_LEVELS.contains(level)) {
            throw new IllegalArgumentException(level);
        }

        unimplemented();

        Map<String, Integer> result =
                null; //getLogMBean().getErrorDistribution( timestamp, Level.parse( level ) );

        // query may return null instead of an empty Map
        if (result != null) {
            // Ensure that no module has a null count
            for (final Map.Entry<String, Integer> me : result.entrySet()) {
                if (me.getValue() == null) {
                    result.put(me.getKey(), INTEGER_0);
                }
            }
        } else {
            // never return a null Map, only an empty one
            result = EMPTY_ERROR_DISTRIBUTION_MAP;
        }

        return result;
    }

    public void setKeepErrorStatisticsForIntervals(final int num) {
        unimplemented();
        //getLogMBean().setKeepErrorStatisticsForIntervals( num );
    }

    public int getKeepErrorStatisticsForIntervals() {
        return 0;
        /*
         unimplemented();
         return getLogMBean().getKeepErrorStatisticsForIntervals();
         */
    }

    public void setErrorStatisticsIntervalMinutes(final long minutes) {
        unimplemented();
        //getLogMBean().setErrorStatisticsIntervalDuration( minutes );
    }

    public long getErrorStatisticsIntervalMinutes() {
        return 0;
        /*
         unimplemented();
         return getLogMBean().getErrorStatisticsIntervalDuration();
         */
    }

    public String[] getLoggerNames() {
        return EMPTY_STRING_ARRAY;
        /*unimplemented();
         final List<String>  names   =
         TypeCast.checkList( getLogMBean().getLoggerNames(), String.class );
	    
         return names.toArray( EMPTY_STRING_ARRAY ); */
    }

    public String[] getLoggerNamesUnder(final String loggerName) {
        unimplemented();
        /*
         final List<String>  names   = TypeCast.checkList(
         getLogMBean().getLoggerNamesUnder( loggerName ), String.class );
	    
         return names.toArray( EMPTY_STRING_ARRAY );
         */
        return null;
    }

    public String[] getDiagnosticCauses(final String messageID, final String moduleName) {

        final List<String> causes = msgIdCatalog.getDiagnosticCausesForMessageId(messageID, moduleName);

        String[] result = null;
        if (causes != null) {
            result = CollectionUtil.toArray(causes, String.class);
        }

        return result;


    }

    public String[] getDiagnosticChecks(String messageID, String moduleName) {

        final List<String> checks = msgIdCatalog.getDiagnosticChecksForMessageId(messageID, moduleName);

        String[] result = null;
        if (checks != null) {
            result = CollectionUtil.toArray(checks, String.class);
        }

        return result;
    }

    public String getDiagnosticURI(final String messageID) {
        unimplemented();
        return null; //getLogMBean().getDiagnosticURIForMessageId( messageID );
    }
    private static final Object[] LEVELS_AND_NOTIF_TYPES = new Object[]{
        Level.SEVERE, LOG_RECORD_SEVERE_NOTIFICATION_TYPE,
        Level.WARNING, LOG_RECORD_WARNING_NOTIFICATION_TYPE,
        Level.INFO, LOG_RECORD_INFO_NOTIFICATION_TYPE,
        Level.CONFIG, LOG_RECORD_CONFIG_NOTIFICATION_TYPE,
        Level.FINE, LOG_RECORD_FINE_NOTIFICATION_TYPE,
        Level.FINER, LOG_RECORD_FINER_NOTIFICATION_TYPE,
        Level.FINEST, LOG_RECORD_FINEST_NOTIFICATION_TYPE,};

    private static Map<Level, String> initLevelToNotificationTypeMap() {
        final Map<Level, String> m = new HashMap<Level, String>();

        for (int i = 0; i < LEVELS_AND_NOTIF_TYPES.length; i += 2) {
            final Level level = (Level) LEVELS_AND_NOTIF_TYPES[ i];
            final String notifType = (String) LEVELS_AND_NOTIF_TYPES[ i + 1];
            m.put(level, notifType);
        }

        return (Collections.unmodifiableMap(m));
    }

    private String logLevelToNotificationType(final Level level) {
        return mLevelToNotificationTypeMap.get(level);
    }

    protected void preRegisterDone()
            throws Exception {
        initNotificationTypeToNotificationBuilderMap(getObjectName());
    }

    private void initNotificationTypeToNotificationBuilderMap(final ObjectName objectName) {
        mNotificationTypeToNotificationBuilderMap.clear();
        for (final String notifType : ALL_LOG_RECORD_NOTIFICATION_TYPES) {
            mNotificationTypeToNotificationBuilderMap.put(
                    notifType,
                    new NotificationBuilder(notifType, objectName));
        }
    }

    private NotificationBuilder notificationTypeToNotificationBuilder(final String notificationType) {
        NotificationBuilder builder =
                mNotificationTypeToNotificationBuilderMap.get(notificationType);

        assert (builder != null);

        return builder;
    }

    private Map<String, Serializable> logRecordToMap(
            final LogRecord record,
            final String recordAsString) {
        final Map<String, Serializable> m = new HashMap<String, Serializable>();

        m.put(LOG_RECORD_AS_STRING_KEY, recordAsString);
        m.put(LOG_RECORD_LEVEL_KEY, record.getLevel());
        m.put(LOG_RECORD_LOGGER_NAME_KEY, record.getLoggerName());
        m.put(LOG_RECORD_MESSAGE_KEY, record.getMessage());
        m.put(LOG_RECORD_MILLIS_KEY, record.getMillis());
        m.put(LOG_RECORD_SEQUENCE_NUMBER_KEY, record.getSequenceNumber());
        m.put(LOG_RECORD_SOURCE_CLASS_NAME_KEY, record.getSourceClassName());
        m.put(LOG_RECORD_SOURCE_METHOD_NAME_KEY, record.getSourceMethodName());
        m.put(LOG_RECORD_THREAD_ID_KEY, record.getThreadID());
        final Throwable thrown = record.getThrown();
        if (thrown != null) {
            final Throwable mapped = new ThrowableMapper(thrown).map();
            m.put(LOG_RECORD_THROWN_KEY, mapped);

            final Throwable rootCause = ExceptionUtil.getRootCause(thrown);
            if (rootCause != thrown) {
                final Throwable mappedRootCause = new ThrowableMapper(rootCause).map();
                m.put(LOG_RECORD_ROOT_CAUSE_KEY, mappedRootCause);
            }
        }
        return m;
    }
    private long mMyThreadID = -1;

    /**
     * Internal use only, called by
     * com.sun.enterprise.server.logging.AMXLoggingHook.
     */
    public void privateLoggingHook(
            final LogRecord logRecord,
            final Formatter formatter) {
        //debug( "LoggingImpl.privateLoggingHook: " + formatter.format( logRecord ) );

        if (logRecord.getThreadID() == mMyThreadID) {
            debug("privateLoggingHook: recusive call!!!");
            throw new RuntimeException("recursive call");
        }
        synchronized (this) {
            mMyThreadID = Thread.currentThread().getId();

            final Level level = logRecord.getLevel();

            try {
                // don't construct a Notification if there are no listeners.
                if (getLogLevelListenerCount(level) != 0) {
                    final String notifType = logLevelToNotificationType(level);

                    final NotificationBuilder builder =
                            notificationTypeToNotificationBuilder(notifType);

                    // Notification.getMessage() will be the formatted log record
                    final String logRecordAsString = formatter.format(logRecord);

                    final Map<String, Serializable> userData =
                            logRecordToMap(logRecord, logRecordAsString);

                    final Notification notif =
                            builder.buildNewWithMap(logRecordAsString, userData);

                    debug("privateLoggingHook: sending: " + notif);
                    sendNotification(notif);
                } else {
                    // debug( "privateLogHook: no listeners for level " + level );
                }
            } finally {
                mMyThreadID = -1;
            }
        }
    }
    
    public void
    testEmitLogMessage( final String level, final String message )
    {
        setMBeanLogLevel( level );
        debug( "testEmitLogMessage: logging: message = " + message );
        getLogger().log( Level.parse( level ), message );
    }
}

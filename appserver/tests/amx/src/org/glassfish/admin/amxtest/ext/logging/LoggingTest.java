/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admin.amxtest.ext.logging;

import com.sun.appserv.management.base.Util;
import com.sun.appserv.management.base.XTypes;
import com.sun.appserv.management.ext.logging.LogModuleNames;
import com.sun.appserv.management.ext.logging.LogQueryEntry;
import com.sun.appserv.management.ext.logging.LogQueryResult;
import com.sun.appserv.management.ext.logging.Logging;
import static com.sun.appserv.management.ext.logging.Logging.*;
import com.sun.appserv.management.j2ee.J2EEServer;
import com.sun.appserv.management.util.misc.CollectionUtil;
import com.sun.appserv.management.util.misc.GSetUtil;
import com.sun.appserv.management.util.misc.StringUtil;
import com.sun.appserv.management.util.stringifier.CollectionStringifier;
import org.glassfish.admin.amxtest.AMXTestBase;
import org.glassfish.admin.amxtest.Capabilities;

import javax.management.Attribute;
import javax.management.ListenerNotFoundException;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;


/**
 Test the {@link Logging}.
 */
public final class LoggingTest
        extends AMXTestBase {
    public LoggingTest() {
    }

    public static Capabilities
    getCapabilities() {
        return getOfflineCapableCapabilities(false);
    }

    final Set<Logging>
    getAllLogging() {
        return getQueryMgr().queryJ2EETypeSet(XTypes.LOGGING);
    }


    public void
    testGetModuleLogLevel() {
        final long start = now();
        final Set<Logging> loggings = getAllLogging();

        for (final Logging logging : loggings) {
            for (final String moduleName : LogModuleNames.ALL_NAMES) {
                final String level = logging.getModuleLogLevel(moduleName);
            }
        }
        printElapsed("testGetModuleLogLevel", loggings.size(), start);
    }

    public void
    testSetModuleLogLevel() {
        final long start = now();
        final Set<Logging> loggings = getAllLogging();

        for (final Logging logging : loggings) {
            for (final String moduleName : LogModuleNames.ALL_NAMES) {
                final String level = logging.getModuleLogLevel(moduleName);
                logging.setModuleLogLevel(moduleName, "FINEST");
                logging.setModuleLogLevel(moduleName, level);
            }
        }
        printElapsed("testSetModuleLogLevel", loggings.size(), start);
    }

    public void
    testGetLogFileNames() {
        final long start = now();
        final Set<Logging> loggings = getAllLogging();

        for (final Logging logging : loggings) {
            final String[] serverLogs = logging.getLogFileNames(SERVER_KEY);
            assert (serverLogs.length != 0);

            //final String[]  accessLogs   = logging.getLogFileNames( ACCESS_KEY );
            //assert( accessLogs.length != 0 );
        }
        printElapsed("testGetLogFileNames", loggings.size(), start);
    }

    public void
    testGetLogFileKeys() {
        final long start = now();
        final Set<Logging> loggings = getAllLogging();

        for (final Logging logging : loggings) {
            final String[] keys = logging.getLogFileKeys();
            final Set<String> keysSet = GSetUtil.newSet(keys);
            assert (keysSet.contains(SERVER_KEY));
            assert (keysSet.contains(ACCESS_KEY));
            assertEquals(2, keysSet.size());
        }
        printElapsed("testGetLogFileKeys", loggings.size(), start);
    }

    /*
     !!!
         Testing these is problematic because if they run
         before the other tests, they create nearly-empty log files

         public void
     testRotateAllLogFiles()
     {
        logging.rotateAllLogFiles();
         how to verify?
     }

         public void
     testRotateLogFile()
     {
         warning( "testRotateLogFile: SKIPPING TEST" );
         logging.rotateLogFile( SERVER_KEY );
         logging.rotateLogFile( ACCESS_KEY );
     }
      */

    public void
    testGetLogFile() {
        final long start = now();
        final Set<Logging> loggings = getAllLogging();

        for (final Logging logging : loggings) {
            final String[] serverLogs = logging.getLogFileNames(SERVER_KEY);
            assert (serverLogs.length != 0);

            for (final String name : serverLogs) {
                final long startGet = now();
                final String contents = logging.getLogFile(SERVER_KEY, name);
                assert (contents != null) : "contents of log file are null: " + name;
                assert (contents.length() != 0) : "contents of log file are empty: " + name;
                printVerbose("Log file " + StringUtil.quote(name) +
                        " has size " + (contents.length() / 1024) + "K, ms = " + (now() - startGet));
            }
        }
        printElapsed("testGetLogFile", loggings.size(), start);
    }


    public void
    testGetErrorInfo() {
        final long start = now();
        final Set<Logging> loggings = getAllLogging();

        for (final Logging logging : loggings) {
            final Map<String, Number>[] infos = logging.getErrorInfo();
            assert (infos != null);
            for (final Map<String, Number> info : infos) {
                assert (info != null && info.keySet().size() >= 3);
                assert (info.containsKey(TIMESTAMP_KEY));
                assert (info.containsKey(SEVERE_COUNT_KEY));
                assert (info.containsKey(WARNING_COUNT_KEY));
            }
        }
        printElapsed("testGetErrorInfo", loggings.size(), start);
    }


    public void
    testGetErrorDistribution() {
        final long start = now();
        final Set<Logging> loggings = getAllLogging();

        for (final Logging logging : loggings) {
            final Map<String, Number>[] infos = logging.getErrorInfo();
            assert (infos != null);
            for (final Map<String, Number> info : infos) {
                final Long timeStamp = (Long) info.get(TIMESTAMP_KEY);
                assert (timeStamp != null);
                final Integer severeCount = (Integer) info.get(SEVERE_COUNT_KEY);
                assert (severeCount != null);
                final Integer warningCount = (Integer) info.get(WARNING_COUNT_KEY);
                assert (warningCount != null);

                final Map<String, Integer> warnings =
                        logging.getErrorDistribution(timeStamp.longValue(), "WARNING");
                assert (warnings != null);
                for (final String moduleID : warnings.keySet()) {
                    assert (warnings.get(moduleID) != null);
                }

                final Map<String, Integer> severes =
                        logging.getErrorDistribution(timeStamp, "SEVERE");
                assert (severes != null);
                for (final String moduleID : severes.keySet()) {
                    assert (severes.get(moduleID) != null);
                }
                // verify that there are no illegal keys.
                final String[] loggerNames = logging.getLoggerNames();
                final Set<String> illegal = GSetUtil.copySet(severes.keySet());
                illegal.removeAll(GSetUtil.newSet(loggerNames));
                assert (illegal.size() == 0) :
                        "Illegal logger names found in Map returned by getErrorDistribution: {" +
                                CollectionUtil.toString(illegal, ",") + "}";


                try {
                    logging.getErrorDistribution(timeStamp, "INFO");
                    fail("expecting failure from getErrorDistribution( timeStamp, 'INFO' )");
                }
                catch (Exception e) {
                }
            }
        }
        printElapsed("testGetErrorDistribution", loggings.size(), start);
    }


    public void
    testGetLoggerNamesAndGetLoggerNamesUnder() {
        final Set<Logging> loggings = getAllLogging();

        for (final Logging logging : loggings) {
            final String[] names = logging.getLoggerNames();
            assert (names != null && names.length != 0);

            for (final String loggerName : names) {
                final String[] under = logging.getLoggerNamesUnder(loggerName);
                assert (under != null);
            }
        }

    }

    public void
    testQuery() {
        final long start = now();
        final Set<Logging> loggings = getAllLogging();

        for (final Logging logging : loggings) {
            final String filename = Logging.MOST_RECENT_NAME;
            final int startRecord = FIRST_RECORD;
            final boolean searchForward = true;
            final int requestedCount = ALL_RECORDS;
            final Long fromTime = now() - (24 * 60 * 60 * 1000);
            final Long toTime = now();
            final Set<String> modules = LogModuleNames.ALL_NAMES; // all
            final List<Attribute> attrs = null;

            final LogQueryResult result = logging.queryServerLog(
                    filename,
                    startRecord,
                    searchForward,
                    requestedCount,
                    fromTime,
                    toTime,
                    Level.WARNING.toString(),
                    modules,
                    attrs);

            final String[] fieldNames = result.getFieldNames();

            for (final LogQueryEntry entry : result.getEntries()) {
                final String messageID = entry.getMessageID();

                final String[] causes = logging.getDiagnosticCauses(messageID);

                final String[] checks = logging.getDiagnosticChecks(messageID);

                final String uri = logging.getDiagnosticURI(messageID);
            }
        }
        printElapsed("testQuery", loggings.size(), start);
    }


    private Map<String, J2EEServer>
    getAllJ2EEServer() {
        final Map<String, J2EEServer> servers =
                getDomainRoot().getJ2EEDomain().getJ2EEServerMap();

        return servers;
    }

    public void
    testHaveLoggingForEachRunningServer() {
        final long start = now();

        final Map<String, J2EEServer> servers = getAllJ2EEServer();
        for (final J2EEServer server : servers.values()) {
            final int state = server.getstate();
            final String serverName = server.getName();
            if (state != J2EEServer.STATE_RUNNING) {
                warning("testHaveLoggingForEachRunningServer: server " +
                        serverName + " is not running");
            } else {
                final Set<Logging> loggings = getAllLogging();
                final Set<ObjectName> loggingsObjectNames = Util.toObjectNames(loggings);
                final Map<String, Logging> all =
                        Util.createNameMap(loggings);
                assert (all.containsKey(serverName)) :
                        "Can't find Logging for server " + serverName +
                                ", have: {" +
                                CollectionStringifier.toString(loggingsObjectNames, ", ") + "}";
            }
        }
        printElapsed("testHaveLoggingForEachRunningServer", servers.size(), start);
    }

    private final class MyListener
            implements NotificationListener {
        private final String mID;

        public MyListener(final String id) {
            mNotifs = Collections.synchronizedList(new ArrayList<Notification>());
            mID = id;
        }

        private final List<Notification> mNotifs;

        public void
        handleNotification(
                final Notification notif,
                final Object handback) {
            //printVerbose( "Received notif: " + notif );

            if (notif.getMessage().indexOf(mID) >= 0) {
                mNotifs.add(notif);
            }
        }

        public List<Notification>
        getNotifications() {
            return Collections.unmodifiableList(mNotifs);
        }

        public int
        getNotificationCount() {
            return mNotifs.size();
        }
    }


    private void
    testListening(final Logging logging)
            throws ListenerNotFoundException {
        final String message = "testListening:" + now();

        final MyListener listener = new MyListener(message);

        printVerbose("Testing Logging=" + logging.getName());

        logging.addNotificationListener(listener, null, null);
        try {
            logging.testEmitLogMessage(Level.SEVERE.toString(), message);
            logging.testEmitLogMessage(Level.WARNING.toString(), message);
            logging.testEmitLogMessage(Level.CONFIG.toString(), message);
            logging.testEmitLogMessage(Level.INFO.toString(), message);
            logging.testEmitLogMessage(Level.FINE.toString(), message);
            logging.testEmitLogMessage(Level.FINER.toString(), message);
            logging.testEmitLogMessage(Level.FINEST.toString(), message);

            int count = 0;
            mySleep(200);
            while ((count = listener.getNotificationCount()) != 7) {
                mySleep(500);
                trace("testListening: waiting for 7 Notifications, have: " + count);
            }
        }
        finally {
            logging.removeNotificationListener(listener);
        }
    }

    public void
    testListeningForAllLogging()
            throws ListenerNotFoundException {
        final long start = now();
        final Set<Logging> loggings = getAllLogging();

        for (final Logging logging : loggings) {
            testListening(logging);
        }
        printElapsed("testListeningForAllLogging", loggings.size(), start);
    }


    public void
    testNoStrayLogging()
            throws ListenerNotFoundException {
        final Set<String> validNames = getAllJ2EEServer().keySet();
        final Set<Logging> loggings = getAllLogging();

        assert (validNames.size() >= loggings.size());

        for (final Logging logging : loggings) {
            assert validNames.contains(logging.getName()) :
                    "Logging MBean name doesn't match any J2EEServer: " + logging.getName();
        }
    }

}






















/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2013 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.admin.launcher;

import com.sun.enterprise.server.logging.ODLLogFormatter;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;

import java.io.IOException;
import java.util.logging.*;
import org.glassfish.logging.annotation.LogMessageInfo;
import org.glassfish.logging.annotation.LoggerInfo;
import org.glassfish.logging.annotation.LogMessagesResourceBundle;

/**
 * A POL (plain old logger).
 *
 * @author bnevins
 */
public class GFLauncherLogger {
    // use LocalStrings for < INFO level...

    public static void warning(String msg, Object... objs) {
        logger.log(Level.WARNING, msg, objs);
    }

    public static void info(String msg, Object... objs) {
        logger.log(Level.INFO, msg, objs);
    }

    public static void severe(String msg, Object... objs) {
        logger.log(Level.SEVERE, msg, objs);
    }

    public static void fine(String msg, Object... objs) {
        if(logger.isLoggable(Level.FINE))
            logger.fine(strings.get(msg, objs));
    }

    /////////////////////////  non-public below  //////////////////////////////
    static synchronized void setConsoleLevel(Level level) {
        for (Handler h : logger.getHandlers()) {
            if (ConsoleHandler.class.isAssignableFrom(h.getClass())) {
                h.setLevel(level);
            }
        }
    }

    /**
     * IMPORTANT! The server's logfile is added to the *local* logger. But it is
     * never removed. The files are kept open by the logger. One really bad
     * result is that Windows will not be able to delete that server after
     * stopping it. Solution: remove the file handler when done.
     *
     * @param logFile The logfile
     * @throws GFLauncherException if the info object has not been setup
     */
    static synchronized void addLogFileHandler(String logFile, GFLauncherInfo info) throws GFLauncherException {
        try {
            if (logFile == null || logfileHandler != null) {
                return;
            }
            logfileHandler = new FileHandler(logFile, true);
            logfileHandler.setFormatter(new ODLLogFormatter());
            logfileHandler.setLevel(Level.INFO);
            logger.addHandler(logfileHandler);
        }
        catch (IOException e) {
            // should be seen in verbose and watchdog modes for debugging
            e.printStackTrace();
        }

    }

    static synchronized void removeLogFileHandler() {
        if (logfileHandler != null) {
            logger.removeHandler(logfileHandler);
            logfileHandler.close();
            logfileHandler = null;
        }
    }

    private GFLauncherLogger() {
    }
    // The resourceBundle name to be used for the module's log messages
    @LogMessagesResourceBundle
    public static final String SHARED_LOGMESSAGE_RESOURCE = "com.sun.enterprise.admin.launcher.LogMessages";
    @LoggerInfo(subsystem = "Launcher", description = "Launcher Logger", publish = true)
    public static final String LOGGER_NAME = "javax.enterprise.launcher";
    private final static Logger logger;
    private final static LocalStringsImpl strings = new LocalStringsImpl(GFLauncherLogger.class);
    private static FileHandler logfileHandler;

    static {
        /*
         * Create a Logger just for the launcher that only uses
         * the Handlers we set up.  This makes sure that when
         * we change the log level for the Handler, we don't
         * interfere with subsequent use of the Logger by
         * asadmin.
         */
        logger = Logger.getLogger(LOGGER_NAME, SHARED_LOGMESSAGE_RESOURCE);
        logger.setLevel(Level.INFO);
        logger.setUseParentHandlers(false);
        logger.addHandler(new ConsoleHandler());
    }

    @LogMessageInfo(
            message =
    "Single and double quote characters are not allowed in the CLASSPATH environmental variable.  "
    + "They were stripped out for you.\nBefore: {0}\nAfter: {1}",
    comment = "CLASSPATH is illegal.",
       cause = "see message",
    action = "see message",

    level = "SEVERE")
    public static final String NO_QUOTES_ALLOWED = "NCLS-GFLAUNCHER-00001";
    @LogMessageInfo(
            message =
    "Error Launching: {0}",
    comment = "Launcher Error",
    cause = "see message",
    action = "fix the CLASSPATH",
    level = "SEVERE")
    public static final String LAUNCH_FAILURE = "NCLS-GFLAUNCHER-00002";

    @LogMessageInfo(
            message =
    "Could not locate the flashlight agent here: {0}",
    comment = "catastrophic error",
   cause = "see message",
    action = "Find the agent file.",
    level = "SEVERE")
    public static final String NO_FLASHLIGHT_AGENT = "NCLS-GFLAUNCHER-00003";

    @LogMessageInfo(
            message =
    "Will copy glassfish/lib/templates/server.policy file to domain before upgrading.",
    comment = "Upgrade Information",
    level = "INFO")

    public static final String copy_server_policy = "NCLS-GFLAUNCHER-00004";

    @LogMessageInfo(
            message =
    "JVM invocation command line:{0}",
    comment = "Routine Information",
   cause = "NA",
    action = "NA",
    level = "INFO")
    public static final String COMMAND_LINE = "NCLS-GFLAUNCHER-00005";
}

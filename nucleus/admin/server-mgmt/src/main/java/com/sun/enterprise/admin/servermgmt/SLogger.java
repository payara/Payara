/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.admin.servermgmt;

import java.util.logging.Logger;
import org.glassfish.logging.annotation.LogMessageInfo;
import org.glassfish.logging.annotation.LogMessagesResourceBundle;
import org.glassfish.logging.annotation.LoggerInfo;

/**
 *
 * @author Byron Nevins
 */
public class SLogger {

    public static Logger getLogger() {
        return logger;
    }

    private SLogger() {
    }
    @LogMessagesResourceBundle
    public static final String SHARED_LOGMESSAGE_RESOURCE = "com.sun.enterprise.admin.servermgmt.LogMessages";
    @LoggerInfo(subsystem = "ServerManagement", description = "Server Management Logger", publish = true)
    public static final String LOGGER_NAME = "javax.enterprise.servermgmt";
    private final static Logger logger = Logger.getLogger(LOGGER_NAME, SHARED_LOGMESSAGE_RESOURCE);
    // these messages are historical.  We've transitioned to this latest Logging API
    @LogMessageInfo(message =
    "Caught an Exception: {0}",
    comment = "Unhandled Exception",
    cause = "see Exception message",
    action = "see Exception message",
    level = "SEVERE")
    public static final String UNHANDLED_EXCEPTION = "NCLS-SERVERMGMT-00000";
    @LogMessageInfo(message =
    "Error attemping to delete temporary certificate file: {0}",
    comment = "Delete error",
    cause = "see message",
    action = "delete the file manually",
    level = "WARNING")
    public static final String BAD_DELETE_TEMP_CERT_FILE = "NCLS-SERVERMGMT-00001";
    @LogMessageInfo(message =
    "Renaming {0} to {1}",
    comment = "No error",
    //cause = "No error",
    //action = "delete the file manually",
    level = "INFO")
    public static final String RENAME_CERT_FILE = "NCLS-SERVERMGMT-00002";
    @LogMessageInfo(message =
    "Failed to rename {0} to {1}",
    comment = "File rename error",
    cause = "see message",
    action = "Check the file system",
    level = "SEVERE")
    public static final String BAD_RENAME_CERT_FILE = "NCLS-SERVERMGMT-00003";
    @LogMessageInfo(message =
    "Failure while upgrading jvm-options from V2 to V3",
    comment = "V2 to V3 Upgrade Failure",
    cause = "see message",
    action = "Check documentation.",
    level = "SEVERE")
    public static final String JVM_OPTION_UPGRADE_FAILURE = "NCLS-SERVERMGMT-00004";
    @LogMessageInfo(message =
    "JVM Monitoring",
    comment = "Just a title",
    level = "INFO")
    public static final String MONITOR_TITLE = "NCLS-SERVERMGMT-00005";
    @LogMessageInfo(message =
    "UpTime(ms)",
    comment = "Just a title",
    level = "INFO")
    public static final String MONITOR_UPTIME_TITLE = "NCLS-SERVERMGMT-00006";
    @LogMessageInfo(
            message = "Heap and NonHeap Memory(bytes)",
    comment = "Just a title",
    level = "INFO")
    public static final String MONITOR_MEMORY_TITLE = "NCLS-SERVERMGMT-00007";
    @LogMessageInfo(message =
    "Failure while upgrading log-service. Could not create logging.properties file. ",
    comment = "see message",
    cause = "see message",
    action = "Check documentation.",
    level = "SEVERE")
    public static final String FAIL_CREATE_LOG_PROPS = "NCLS-SERVERMGMT-00008";
    @LogMessageInfo(message =
    "Failure while upgrading log-service. Could not update logging.properties file. ",
    comment = "see message",
    cause = "see message",
    action = "Check documentation.",
    level = "SEVERE")
    public static final String FAIL_UPDATE_LOG_PROPS = "NCLS-SERVERMGMT-00009";
    @LogMessageInfo(message =
    "Failure while upgrading log-service ",
    comment = "see message",
    cause = "see message",
    action = "Check documentation.",
    level = "SEVERE")
    public static final String FAIL_UPGRADE_LOG_SERVICE = "NCLS-SERVERMGMT-00010";
}

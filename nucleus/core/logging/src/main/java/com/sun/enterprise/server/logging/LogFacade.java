/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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

import java.util.logging.Logger;

import org.glassfish.logging.annotation.LogMessageInfo;
import org.glassfish.logging.annotation.LogMessagesResourceBundle;
import org.glassfish.logging.annotation.LoggerInfo;

public class LogFacade {
    
    @LoggerInfo(subsystem = "Logging", description="Main logger for core logging component.")
    public static final String LOGGING_LOGGER_NAME = "javax.enterprise.logging";

    @LoggerInfo(subsystem = "Logging", description="Stdout logger.", publish=false)
    private static final String STDOUT_LOGGER_NAME = "javax.enterprise.logging.stdout";

    @LoggerInfo(subsystem = "Logging", description="Stderr logger.", publish=false)
    private static final String STDERR_LOGGER_NAME = "javax.enterprise.logging.stderr";

    @LogMessagesResourceBundle()
    public static final String LOGGING_RB_NAME = "com.sun.enterprise.server.logging.LogMessages";
    
    public static final Logger LOGGING_LOGGER = 
        Logger.getLogger(LOGGING_LOGGER_NAME, LOGGING_RB_NAME);
    
    public static final Logger STDOUT_LOGGER = Logger.getLogger(STDOUT_LOGGER_NAME);
    
    public static final Logger STDERR_LOGGER = Logger.getLogger(STDERR_LOGGER_NAME);
    
    @LogMessageInfo(message = "Cannot read logging configuration file.", level="SEVERE",
            cause="An exception has occurred while reading the logging configuration file.",
            action="Take appropriate action based on the exception message.")
    public static final String ERROR_READING_CONF_FILE = "NCLS-LOGGING-00001";

    @LogMessageInfo(message = "Could not apply the logging configuration changes.", level="SEVERE",
            cause="There was an exception thrown while applying the logging configuration changes.",
            action="Take appropriate action based on the exception message.")
    public static final String ERROR_APPLYING_CONF = "NCLS-LOGGING-00002";

    @LogMessageInfo(message = "Updated logger levels successfully.", level="INFO")
    public static final String UPDATED_LOG_LEVELS = "NCLS-LOGGING-00003";

    @LogMessageInfo(message = "The logging configuration file {0} has been deleted.", level="WARNING")
    public static final String CONF_FILE_DELETED = "NCLS-LOGGING-00004";

    @LogMessageInfo(message = "Error executing query to fetch log records.", level="SEVERE",
            cause="There was an exception thrown while executing log query.",
            action="Take appropriate action based on the exception message.")
    public static final String ERROR_EXECUTING_LOG_QUERY = "NCLS-LOGGING-00005";
    
    @LogMessageInfo(message = "The syslog handler could not be initialized.", level="SEVERE",
            cause="There was an exception thrown while initializing the syslog handler.",
            action="Take appropriate action based on the exception message.")
    public static final String ERROR_INIT_SYSLOG = "NCLS-LOGGING-00006";

    @LogMessageInfo(message = "There was an error sending a log message to syslog.", level="SEVERE",
            cause="There was an exception thrown while sending a log message to the syslog.",
            action="Take appropriate action based on the exception message.")
    public static final String ERROR_SENDING_SYSLOG_MSG = "NCLS-LOGGING-00007";

    @LogMessageInfo(message = "The log file {0} for the instance does not exist.", level="WARNING")
    public static final String INSTANCE_LOG_FILE_NOT_FOUND = "NCLS-LOGGING-00008";

    @LogMessageInfo(message = "Running GlassFish Version: {0}", level="INFO")
    public static final String GF_VERSION_INFO = "NCLS-LOGGING-00009";
    
    @LogMessageInfo(message = "Server log file is using Formatter class: {0}", level="INFO")
    public static final String LOG_FORMATTER_INFO = "NCLS-LOGGING-00010";

    @LogMessageInfo(message = "Failed to parse the date: {0}", level="WARNING")
    public static final String DATE_PARSING_FAILED = "NCLS-LOGGING-00011";
    
    @LogMessageInfo(message = "An invalid value {0} has been specified for the {1} attribute in the logging configuration.", level="WARNING")
    public static final String INVALID_ATTRIBUTE_VALUE = "NCLS-LOGGING-00012";    

    @LogMessageInfo(message = "The formatter class {0} could not be instantiated.", level="WARNING")
    public static final String INVALID_FORMATTER_CLASS_NAME = "NCLS-LOGGING-00013";    

}

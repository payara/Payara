/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.java.net/public/CDDL+GPL_1_1.html
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

package com.sun.enterprise.connectors.jms;

import java.util.logging.Logger;
import org.glassfish.logging.annotation.LogMessageInfo;
import org.glassfish.logging.annotation.LogMessagesResourceBundle;
import org.glassfish.logging.annotation.LoggerInfo;

/**
 * Logger information for the jms-core module.
 * @author Krishna Deepak
 */
public class JMSLoggerInfo {
    public static final String LOGMSG_PREFIX = "AS-JMS-CORE";

    @LogMessagesResourceBundle
    public static final String SHARED_LOGMESSAGE_RESOURCE = "com.sun.enterprise.connectors.jms.LogMessages";

    @LoggerInfo(subsystem="JMS", description="Main JMS Logger", publish=true)
    public static final String JMS_MAIN_LOGGER = "javax.enterprise.resource.jms";

    private static final Logger jmsLogger =
            Logger.getLogger(JMS_MAIN_LOGGER, SHARED_LOGMESSAGE_RESOURCE);

    public static Logger getLogger() {
        return jmsLogger;
    }

    @LogMessageInfo(message = "JMS Service Connection URL is : {0}")
    public static final String JMS_CONNECTION_URL = LOGMSG_PREFIX + "-00001";

    @LogMessageInfo(message = "ADDRESSLIST in setJmsServiceProvider : {0}")
    public static final String ADDRESSLIST_JMSPROVIDER = LOGMSG_PREFIX + "-00002";

    @LogMessageInfo(message = "Addresslist : {0}")
    public static final String ADDRESSLIST = LOGMSG_PREFIX + "-00003";
    
    @LogMessageInfo(message = "End point determines destination name, Res name: {0}, JNDI name: {1} descriptor name : {2}")
    public static final String ENDPOINT_DEST_NAME = LOGMSG_PREFIX + "-00004";
    
    @LogMessageInfo(message = "Successfully set Master broker on JMSRA to {0}")
    public static final String MASTER_BROKER_SUCCESS = LOGMSG_PREFIX + "-00005";

    @LogMessageInfo(message = "Failed to set Master broker on JMSRA to {0} cause {1}")
    public static final String MASTER_BROKER_FAILURE = LOGMSG_PREFIX + "-00006";

    @LogMessageInfo(message = "Successfully set Cluster brokerlist to {0}")
    public static final String CLUSTER_BROKER_SUCCESS = LOGMSG_PREFIX + "-00007";

    @LogMessageInfo(
            message = "Failed to set Cluster brokerlist to {0} cause {1}",
            level = "WARNING",
            cause = "unknown",
            action = "unknown"
    )
    public static final String CLUSTER_BROKER_FAILURE = LOGMSG_PREFIX + "-00008";
    
    @LogMessageInfo(
            message = "Failed to shut down Grizzly NetworkListener : {0}",
            level = "WARNING",
            cause = "unknown",
            action = "unknown"
    )
    public static final String SHUTDOWN_FAIL_GRIZZLY = LOGMSG_PREFIX + "-00009";

    @LogMessageInfo(
            message = "Error occurs when shutting down JMSRA : {0}",
            level = "WARNING",
            cause = "unknown",
            action = "unknown"
    )
    public static final String SHUTDOWN_FAIL_JMSRA = LOGMSG_PREFIX + "-00010";

    @LogMessageInfo(
            message = "Invalid RMI registry port",
            level = "WARNING",
            cause = "unknown",
            action = "unknown"
    )
    public static final String INVALID_RMI_PORT = LOGMSG_PREFIX + "-00011";

    @LogMessageInfo(
            message = "No such method {0} in the class {1}",
            level = "WARNING",
            cause = "The method setProperty is not defined in the class",
            action = "Define the appropriate method"
    )
    public static final String NO_SUCH_METHOD = LOGMSG_PREFIX + "-00012";

    @LogMessageInfo(
            message = "Connector Resource could not be closed",
            level = "WARNING",
            cause = "unknown",
            action = "unknown"
    )
    public static final String CLOSE_CONNECTION_FAILED = LOGMSG_PREFIX + "-00013";
    
    @LogMessageInfo(
            message = "rardeployment.mcfcreation_error {0}",
            level = "WARNING",
            cause = "unknown",
            action = "unknown"
    )
    public static final String RARDEPLOYMENT_MCF_ERROR = LOGMSG_PREFIX + "-00014";
    
    @LogMessageInfo(
            message = "Exception while getting configured RMI port : {0}",
            level = "WARNING",
            cause = "unknown",
            action = "unknown"
    )
    public static final String GET_RMIPORT_FAIL = LOGMSG_PREFIX + "-00015";

    @LogMessageInfo(
            message = "Failed to start Grizlly proxy for MQ broker",
            level = "WARNING",
            cause = "unknown",
            action = "unknown"
    )
    public static final String GRIZZLY_START_FAILURE = LOGMSG_PREFIX + "-00016";

    @LogMessageInfo(
            message = "Failed to create addresslist due to the exception : {0}",
            level = "WARNING",
            cause = "unknown",
            action = "unknown"
    )
    public static final String ADDRESSLIST_SETUP_FAIL = LOGMSG_PREFIX + "-00017";

    @LogMessageInfo(
            message = "Error executing method {0} of the class {1}",
            level = "SEVERE",
            cause = "unknown",
            action = "unknown"
    )
    public static final String ERROR_EXECUTE_METHOD = LOGMSG_PREFIX + "-00018";

    @LogMessageInfo(
            message = "MDB destination not specified",
            level = "SEVERE",
            cause = "Missing destination JNDI name",
            action = "unknown"
    )
    public static final String ERROR_IN_DD = LOGMSG_PREFIX + "-00019";

    @LogMessageInfo(
            message = "Failed to validate endpoint",
            level = "SEVERE",
            cause = "unknown",
            action = "unknown"
    )
    public static final String ENDPOINT_VALIDATE_FAILED = LOGMSG_PREFIX + "-00020";

    @LogMessageInfo(
            message = "Cannot obtain master broker",
            level = "SEVERE",
            cause = "unknown",
            action = "unknown"
    )
    public static final String GET_MASTER_FAILED = LOGMSG_PREFIX + "-00021";

    @LogMessageInfo(
            message = "Error while loading connector resources during recovery : {0}",
            level = "SEVERE",
            cause = "unknown",
            action = "unknown"
    )
    public static final String LOAD_RESOURCES_ERROR = LOGMSG_PREFIX + "-00022";
    
    @LogMessageInfo(
            message = "Exception in reading mdb-container configuration : [{0}]",
            level = "WARNING",
            cause = "unknown",
            action = "unknown"
    )
    public static final String MDB_CONFIG_EXCEPTION = LOGMSG_PREFIX + "-00023";

    @LogMessageInfo(message = "MQ Resource adapter upgrade started.")
    public static final String JMSRA_UPGRADE_STARTED = LOGMSG_PREFIX + "-00024";

    @LogMessageInfo(message = "MQ Resource adapter upgrade completed.")
    public static final String JMSRA_UPGRADE_COMPLETED = LOGMSG_PREFIX + "-00025";

    @LogMessageInfo(
            message = "Upgrading a MQ resource adapter failed : {0}",
            level = "WARNING",
            cause = "unknown",
            action = "unknown"
    )
    public static final String JMSRA_UPGRADE_FAILED = LOGMSG_PREFIX + "-00026";

    @LogMessageInfo(
            message = "Check for a new version of MQ installation failed : {0}",
            level = "WARNING",
            cause = "unknown",
            action = "unknown"
    )
    public static final String JMSRA_UPGRADE_CHECK_FAILED = LOGMSG_PREFIX + "-00027";
}
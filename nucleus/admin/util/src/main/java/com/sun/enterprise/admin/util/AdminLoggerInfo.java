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
package com.sun.enterprise.admin.util;

import java.util.logging.Logger;
import org.glassfish.logging.annotation.LogMessageInfo;
import org.glassfish.logging.annotation.LogMessagesResourceBundle;
import org.glassfish.logging.annotation.LoggerInfo;

/**
 * Logger information for the admin-util module.
 * @author Tom Mueller
 */
/* Module private */
public class AdminLoggerInfo {
    public static final String LOGMSG_PREFIX = "NCLS-ADMIN";
    
    @LogMessagesResourceBundle
    public static final String SHARED_LOGMESSAGE_RESOURCE = "com.sun.enterprise.admin.util.LogMessages";
    
    @LoggerInfo(subsystem = "ADMIN", description = "Administration Services", publish = true)
    public static final String ADMIN_LOGGER = "javax.enterprise.system.tools.admin";
    private static final Logger adminLogger = Logger.getLogger(
                ADMIN_LOGGER, SHARED_LOGMESSAGE_RESOURCE);

    public static Logger getLogger() {
        return adminLogger;
    }
    
    @LogMessageInfo(
            message = "Could not find state of instance registered in the state service",
            cause = "unknown",
            action = "unknown",
            level = "SEVERE")
    static final String stateNotFound = LOGMSG_PREFIX + "-00001";
    
    @LogMessageInfo(
            message = "Error during command replication: {0}",
            cause = "unknown",
            action = "unknown",
            level = "SEVERE")
    static final String replicationError = LOGMSG_PREFIX + "-00002";
    
    @LogMessageInfo(
            message = "unable to read instance state file {0}, recreating",
            level = "FINE")
    final static String mISScannotread = LOGMSG_PREFIX + "-00003";
    
    @LogMessageInfo(
            message = "unable to create instance state file: {0}, exception: {1}",
            cause = "The instance state file is missing and the system is trying to" + 
                    "recreated it but and exception was raised.",
            action = "Check the server logs and contact Oracle support",
            publish = true,
            level = "SEVERE")
    final static String mISScannotcreate = LOGMSG_PREFIX + "-00004";
    
    @LogMessageInfo(
            message = "error while adding new server state to instance state: {0}",
            cause = "An attempt to add a new server to the instance state file failed.", 
            action = "Check the server logs and contact Oracle support",
            publish = true,
            level = "SEVERE")
    final static String mISSaddstateerror = LOGMSG_PREFIX + "-00005";
    
    @LogMessageInfo(
            message = "error while adding failed command to instance state: {0}",
            cause = "An attempt to add a failed command to the instance state file failed.", 
            action = "Check the server logs and contact Oracle support",
            publish = true,
            level = "SEVERE")
    final static String mISSaddcmderror = LOGMSG_PREFIX + "-00006";
    
    @LogMessageInfo(
            message = "error while removing failed commands from instance state: {0}",
            cause = "An attempt to remove a failed command from the instance state file failed.", 
            action = "Check the server logs and contact Oracle support",
            publish = true,
            level = "SEVERE")
    final static String mISSremcmderror = LOGMSG_PREFIX + "-00007";
    
    @LogMessageInfo(
            message = "error while setting instance state: {0}",
            cause = "An attempt to set the state of a server in the instance state file failed.", 
            action = "Check the server logs and contact Oracle support",
            publish = true,
            level = "SEVERE")
    final static String mISSsetstateerror = LOGMSG_PREFIX + "-00008";
    
    @LogMessageInfo(
            message = "error while removing instance: {0}",
            cause = "An attempt to remove a server from the instance state file failed.", 
            action = "Check the server logs and contact Oracle support",
            publish = true,
            level = "SEVERE")
    final static String mISSremstateerror = LOGMSG_PREFIX + "-00009";

    @LogMessageInfo(
            message = "It appears that server [{0}:{1}] does not accept secure connections. Retry with --secure=false.",
            cause = "An attempt to invoke a command on another server failed.", 
            action = "Check that the server is configured to accept secure connections.",
            publish = true,
            level = "SEVERE")
    public final static String mServerIsNotSecure = LOGMSG_PREFIX + "-00010";
    
    @LogMessageInfo(
            message = "An unexpected exception occurred.",
            cause = "An unexpected exception occurred.", 
            action = "Check the server logs and contact Oracle support",
            publish = true,
            level = "SEVERE")
    public final static String mUnexpectedException = LOGMSG_PREFIX + "-00011";
    
    @LogMessageInfo(
            message = "The server requires a valid admin password to be set before it can start. Please set a password using the change-admin-password command.",
            cause = "For security reason, the server requires a valid admin password before it can start.", 
            action = "Set a password using the change-admin-password command.",
            publish = true,
            level = "SEVERE")
    public final static String mSecureAdminEmptyPassword = LOGMSG_PREFIX + "-00012";
   
    @LogMessageInfo(
            message = "Can not put data to cache under key {0}",
            cause = "While invoking a command on another server, this server is unable"
                + " to cache the meta data related to the command.", 
            action = "Check the server logs and contact Oracle support",
            publish = true,
            level = "WARNING")
    public final static String mCantPutToCache = LOGMSG_PREFIX + "-00013";
     
    @LogMessageInfo(
            message = "An admin request arrived from {0} with the domain identifier {1} "
                + "which does not match the domain identifier {2} configured for this "
                + "server's domain; rejecting the request",
            cause = "There is a error in the cluster or network configuration.",
            action = "Check the server logs and contact Oracle support",
            publish = true,
            level = "WARNING")
    public final static String mForeignDomainID = LOGMSG_PREFIX + "-00014";

    @LogMessageInfo(
            message = "Error searching for a default admin user",
            cause = "An unexpected exception occurred wihle searching for the default admin user.",
            action = "Check the server logs and contact Oracle support",
            publish = true,
            level = "WARNING")
    public final static String mAdminUserSearchError = LOGMSG_PREFIX + "-00015";
    
    @LogMessageInfo(
            message = "Cannot read admin cache file for {0}",
            cause = "An error occured while reading the admin command model cache file.",
            action = "Check the server logs and contact Oracle support",
            publish = true,
            level = "WARNING")
    public final static String mCannotReadCache = LOGMSG_PREFIX + "-00016";
    
    @LogMessageInfo(
            message = "Cannot write data to cache file for {0}",
            cause = "An error occured while writing the admin command model cache file.",
            action = "Check the server logs and contact Oracle support",
            publish = true,
            level = "WARNING")
    public final static String mCannotWriteCache = LOGMSG_PREFIX + "-00017";
    
    @LogMessageInfo(
            message = "Unexpected exception from command event listener.",
            cause = "An error occured while calling registered listener.",
            action = "Check the server logs and contact Oracle support",
            publish = true,
            level = "WARNING")
    public final static String mExceptionFromEventListener = LOGMSG_PREFIX + "-00018";

}

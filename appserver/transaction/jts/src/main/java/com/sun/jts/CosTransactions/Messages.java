/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

//----------------------------------------------------------------------------
//
// Module:      Messages.java
//
// Description: JTS messages, default.
//
// Product:     com.sun.jts.CosTransactions
//
// Author:      Simon Holdsworth
//
// Date:        March, 1997
//
// Copyright (c):   1995-1997 IBM Corp.
//
//   The source code for this program is not published or otherwise divested
//   of its trade secrets, irrespective of what has been deposited with the
//   U.S. Copyright Office.
//
//   This software contains confidential and proprietary information of
//   IBM Corp.
//----------------------------------------------------------------------------

package com.sun.jts.CosTransactions;

import java.util.*;
import java.text.*;

/**
 * This class provides a ListResourceBundle which contains the message formats
 * for messages produced by the JTS.
 *
 * @version 0.01
 *
 * @author Simon Holdsworth, IBM Corporation
 *
 * @see
 */

//----------------------------------------------------------------------------
// CHANGE HISTORY
//
// Version By     Change Description
//   0.01  SAJH   Initial implementation.
//----------------------------------------------------------------------------

public class Messages extends ListResourceBundle  {

    // These message numbers are needed for the error messages
    // produced by code in this file.

    private static final int UNKNOWN_MESSAGE = 0;
    private static final int INVALID_FORMAT = 1;

    // Application-specific message numbers.

    /**
     * The ORB is not running.
     */
    public static final int ORBD_NOT_RUNNING = 2;

    /**
     * The JTS instance is not persistent.
     */
    public static final int NON_PERSISTENT = 3;

    /**
     * The Naming service failed.
     */
    public static final int NAME_SERVICE_FAILED = 4;

    /**
     * Log initialisation failed.
     */
    public static final int LOG_INIT_FAILED = 5;

    /**
     * Log open failed.
     */
    public static final int LOG_OPEN_FAILED = 6;

    /**
     * Object reference creation failed.
     */
    public static final int OBJREF_CREATE_FAILED = 7;

    /**
     * Object reference destruction failed.
     */
    public static final int OBJREF_DESTROY_FAILED = 8;

    /**
     * JTS is already identified to the Comms Manager.
     */
    public static final int ALREADY_IDENTIFIED = 9;

    /**
     * JTS cannot identify itself to the Comms Manager.
     */
    public static final int IDENTIFY_UNAVAILABLE = 10;

    /**
     * Could not create a subordinate Coordinator.
     */
    public static final int SUBORDINATE_CREATE_FAILED = 11;

    /**
     * Could not recover a Coordinator from the log.
     */
    public static final int RECOVER_COORD_FAILED = 12;

    /**
     * Exception occurred while performing resync.
     */
    public static final int RESYNC_OP_EXCEPTION = 13;

    /**
     * Exception occurred while calling a Resource object.
     */
    public static final int RESOURCE_OP_EXCEPTION = 14;

    /**
     * Commit/rollback/forget operation retry limit exceeded.
     */
    public static final int RETRY_LIMIT_EXCEEDED = 15;

    /**
     * Exception occurred while calling a Synchronization object.
     */
    public static final int SYNC_OP_EXCEPTION = 16;

    /**
     * Timeout thread stopped abnormally.
     */
    public static final int TIMEOUT_STOPPED = 17;

    /**
     * Invalid log path specification.
     */
    public static final int INVALID_LOG_PATH = 18;

    /**
     * Default log path does not exist.
     */
    public static final int INVALID_DEFAULT_LOG_PATH = 19;

    /**
     * Error accessing JTS repository.
     */
    public static final int SERVER_INFO_ERROR = 20;

    /**
     * Error accessing JTS repository.
     */
    public static final int GLOBAL_INFO_ERROR = 21;

    /**
     * Log error occurred.
     */
    public static final int LOG_ERROR = 23;

    /**
     * Trace using current directory.
     */
    public static final int TRACE_CURRENT = 24;

    /**
     * Invalid repository path specification.
     */
    public static final int INVALID_REPOSITORY_PATH = 25;

    /**
     * Default repository path does not exist.
     */
    public static final int INVALID_DEFAULT_REPOSITORY_PATH = 26;

    /**
     * Failed to access repository file.
     */
    public static final int REPOSITORY_FAILED = 27;

    /**
     * Could not locate object servant.
     */
    public static final int SERVANT_LOCATE_FAILED = 28;

    /**
     * Heuristic exception caught in resync could not be reported.
     */
    public static final int UNREPORTABLE_HEURISTIC = 29;

    /**
     * Wait for resync complete interrupted.
     */
    public static final int RESYNC_WAIT_INTERRUPTED = 30;

    /**
     * Transaction in wrong state for operation.
     */
    public static final int WRONG_STATE = 31;

    /**
     * Failed to store information in repository.
     */
    public static final int RESTART_WRITE_FAILED = 32;

    /**
     * Failed to close database during recovery.
     */
    public static final int RECOVERY_CLOSE_FAILED = 33;

    /**
     * Failed to reconstruct XA information.
     */
    public static final int XA_RECONSTRUCT_FAILED = 34;

    /**
     * XA resource manager string invalid.
     */
    public static final int INVALID_XA_RM_STRING = 35;

    /**
     * XA server initialisation failed.
     */
    public static final int XA_SERVER_INIT_FAILED = 36;

    /**
     * XA open/close operation failed.
     */
    public static final int XA_OPENCLOSE_ERROR = 37;

    /**
     * XA operation failed.
     */
    public static final int XA_ERROR = 38;

    /**
     * Database context is incorrect for the operation.
     */
    public static final int INCORRECT_CONTEXT = 39;

    /**
     * SQL error.
     */
    public static final int SQL_ERROR = 40;

    /**
     * XA switch load file load failed.
     */
    public static final int XA_SWITCH_LOAD_FAILED = 41;

    /**
     * SQL ALLOCATE failed.
     */
    public static final int SQL_ALLOCATE_FAILED = 42;

    /**
     * Could not create JDBC driver.
     */
    public static final int JDBC_DRIVER_CREATE_FAILED = 43;

    /**
     * JDBC security check failed.
     */
    public static final int JDBC_SECURITY_CHECK_FAILED = 44;

    /**
     * JDBC instance creation failed.
     */
    public static final int JDBC_INSTANCE_CREATE_FAILED = 45;

    /**
     * JDBC Driver Manager registration failed.
     */
    public static final int JDBC_DM_REGISTER_FAILED = 46;

    /**
     * JDBC driver load failed.
     */
    public static final int JDBC_DRIVER_LOAD_FAILED = 47;

    /**
     * Error loading native XA class.
     */
    public static final int NATIVEXA_CLASS_LOAD_ERROR = 48;

    /**
     * Log exists for transient process.
     */
    public static final int LOG_EXISTS_FOR_TRANSIENT = 49;

    /**
     * Invalid data in log record.
     */
    public static final int INVALID_LOG_RECORD_DATA = 50;

    /**
     * ODBC database definition does not exist.
     */
    public static final int NO_ODBC_DATABASE_DEFINITION = 51;

    /**
     * ODBC driver not supported for database.
     */
    public static final int UNSUPPORTED_ODBC_DRIVER_FOR_DATABASE = 52;

    /**
     * Recovery of object reference failed.
     */
    public static final int RECOVER_OBJECT_REF = 53;

    /**
     * Superior Top Coordinator not reachable on reply completion.
     */
    public static final int REPLAY_COMP_UNKNOWN = 54;

    /**
     * Transaction id is already in use.
     */
    public static final int ERR_TID_ALREADY_USED = 55;

    /**
     * Invalid state change.
     */
    public static final int ERR_INVALID_STATE_CHANGE = 56;

    /**
     * No coordinator available.
     */
    public static final int ERR_NO_COORDINATOR = 57;

    /**
     * XAException during recovery.
     */
    public static final int ERR_XA_RECOVERY = 58;

    /**
     * Standard message during startup regarding server id and mode.
     */
    public static final int MSG_STARTUP = 59;

    /**
     * No server name.
     */
    public static final int MSG_NO_SERVERNAME = 60;

    /**
     * Invalid default log path.
     */
    public static final int LOG_FILE_WRITE_ERROR = 61;

    /**
     * JTS error message.
     */
    public static final int MSG_JTS_ERROR = 62;

    /**
     * JTS warning message.
     */
    public static final int MSG_JTS_WARNING = 63;

    /**
     * JTS info message.
     */
    public static final int MSG_JTS_INFO = 64;

    /**
     * JTS log message.
     */
    public static final int LOG_MESSAGE = 65;

    /**
     * JTS error message.
     */
    public static final int INVALID_TIMEOUT = 66;

    /**
     * Return the contents of the ResourceBundle.
     */
    protected Object[][] getContents() { return contents; }

    /**
     * Return a formatted message.
     */
    final String getMessage(int messageNum, Object[] inserts) {
        Object[][] contents = getContents();
        if (messageNum > contents.length) {
            messageNum = UNKNOWN_MESSAGE;
            inserts = new Object[] { messageNum };
        } else if (!(contents[messageNum][1] instanceof String)) {
            messageNum = INVALID_FORMAT;
            inserts = new Object[] { messageNum };
        }

        return MessageFormat.format((String) contents[messageNum][1], inserts);
    }

    /**
     * Get a message number.
     */
    final String getMessageNumber(int messageNum) {

        Object[][] contents = getContents();

        if (messageNum > contents.length) {
            return null;
        }

        return contents[messageNum][0].toString();
    }

    /**
     * Get an unformatted message.
     *
     * @param messageNum the message number.
     * @return unformatted message (value part of the resource bundle).
     */
    final String getMessage(int messageNum) {

        Object[][] contents = getContents();

        if (messageNum > contents.length) {
            return null;
        }

        return contents[messageNum][1].toString();
    }

    /**
     * The message formats.
     */
    private static final Object[][] contents = {

        // Required messages.

        { "", "Unknown message number {0}." },
        { "", "Invalid message format for message number {0}." },

        // Application messages.

        { "000", "The ORB daemon, ORBD, is not running." },
        { "001", "This is a non-persistent server. Transactions will not " +
                 "be recoverable." },
        { "002", "Cannot register {0} instance with the ORB." },
        { "003", "Cannot initialise log." },
        { "004", "Cannot open log file for server {0}." },
        { "005", "Cannot create {0} object reference." },
        { "006", "Cannot destroy {0} object reference." },
        { "007", "Already identified to communications manager." },
        { "008", "Unable to identify to communications manager." },
        { "009", "Unable to create a subordinate Coordinator." },
        { "010", "Exception {0} recovering an in-doubt Coordinator." },
        { "011", "Exception {0} on {1} operation during resync." },
        { "012", "Exception {0} on Resource {1} operation." },
        { "013", "Retry limit of {0} {1} operations exceeded." },
        { "014", "Exception {0} on {1} synchronization operation." },
        { "015", "Timeout thread stopped." },
        { "016", "Error: Invalid log path.  Using {0}." },
        { "017", "Error: Invalid default log path.  Using current directory." },
        { "018", "Cannot access server information for server {0}." },
        { "019", "Cannot access global information." },
        { "020", "" },
        { "021", "Unexpected exception {0} from log." },
        { "022", "Defaulting to current directory for trace." },
        { "023", "Invalid repository path.  Using {0}." },
        { "024", "Invalid default repository path. Using current directory." },
        { "025", "Cannot read repository file." },
        { "026", "Cannot locate {0} servant." },
        { "027", "Heuristic exception {0} cannot be reported to superior " +
                 "in resync." },
        { "028", "Wait for resync complete interrupted." },
        { "029", "Transaction in the wrong state for {0} operation." },
        { "030", "Unable to write restart record." },
        { "031", "xa_close operation failed during recovery." },
        { "032", "Could not reconstruct XA information during recovery." },
        { "033", "XA Resource Manager string {0} is invalid." },
        { "034", "XA Resource Manager {1} initialisation failed." },
        { "035", "{0} with string {1} returned {2} for " +
                 "Resource Manager {3}." },
        { "036", "{0} operation returned {1} for Resource Manager {2}." },
        { "037", "Incorrect {0} context during transaction start " +
                 "association." },
        { "038", "SQL error: {0} returned rc {1}, SQLCODE {2}." },
        { "039", "Unexpected exception ''{0}'' while loading XA switch " +
                 "class {1}." },
        { "040", "Cannot allocate SQL environment." },
        { "041", "Unable to create new JDBC-ODBC Driver." },
        { "042", "Security check failed, reason string is {0}." },
        { "043", "Unable to create new JdbcOdbc instance." },
        { "044", "Unable to register with the JDBC Driver Manager." },
        { "045", "Unable to load JDBC-ODBC Driver class." },
        { "046", "Unexpected exception ''{0}'' while loading class {1}." },
        { "047", "Log file exists for transient server {0}." },
        { "048", "Invalid log record data in section {0}." },
        { "049", "No entry found for database ''{0}'' in the ODBC " +
                 "configuration." },
        { "050", "ODBC Driver ''{0}'' for database ''{1}'' not supported " +
                 "for transactional connection." },
        { "051", "Unable to convert object reference to string in recovery." },
        { "052", "Transaction resynchronization from originator failed, " +
                 "retrying...." },
        { "053", "Transaction id is already in use." },
        { "054", "Invalid transaction state change." },
        { "055", "No coordinator available." },
        { "056", "XAException occured during recovery of XAResource objects." },
        { "057", "Recoverable JTS instance, serverId = {0}." },
        { "058", "No server name." },
        { "059", "Error: Unable to write to error log file." },
        { "060", "JTS Error: {0}." },
        { "061", "JTS Warning: {0}." },
        { "062", "JTS Info: {0}." },
        { "063", "{0} : {1} : JTS{2}{3} {4}\n" },
        { "064", "Invalid timeout value. Negative values are illegal." },
    };
}

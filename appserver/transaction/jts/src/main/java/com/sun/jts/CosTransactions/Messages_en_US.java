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

//----------------------------------------------------------------------------
//
// Module:      Messages_en_US.java
//
// Description: JTS messages, US English (for testing purposes).
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

/**
 * This class provides a ListResourceBundle which contains the message formats
 * for messages produced by the JTS.  It is an example which can be copied for
 * other languages.
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

public class Messages_en_US extends Messages {
    /**
     * Return the contents of the bundle.
     */
    protected Object[][] getContents() { return contents; }

    /**
     * The message formats.
     */
    private static final Object[][] contents = {

        // Required messages.

        { "", "Unknown message number {0}." },
        { "", "Invalid message format for message number {0}." },

        // Application messages.

        { "000", "The ORB daemon, ORBD, is not running." },
        { "001", "(US) This is a non-persistent server. " +
                 "Transactions will not be recoverable." },
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
        { "012", "Exception {0} on {1} resource operation." },
        { "013", "Retry limit of {0} {1} operations exceeded." },
        { "014", "Exception {0} on {1} synchronization operation." },
        { "015", "Timeout thread stopped." },
        { "016", "Invalid log path.  Using {0}." },
        { "017", "Invalid default log path.  Using current directory." },
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
        { "030", "Unable to store information in repository." },
        { "031", "xa_close operation failed during recovery." },
        { "032", "Could not reconstruct XA information during recovery." },
        { "033", "XA Resource Manager string {0} is invalid." },
        { "034", "XA Resource Manager {1} initialisation failed." },
        { "035", "{0} with string {1} returned {2} for " +
                 "Resource Manager {3}." },
        { "036", "{0} operation returned {1} for Resource Manager {2}." },
        { "037", "Incorrect {0} context during transaction start "+
                 "association." },
        { "038", "SQL error: {0} returned rc {1}, SQLCODE {2}." },
        { "039", "Unexpected exception ''{0}'' while loading XA switch " +
                 "class {1}." },
        { "040", "Connot allocate SQL environment." },
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

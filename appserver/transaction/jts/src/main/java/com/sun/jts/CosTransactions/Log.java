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
// Module:      Log.java
//
// Description: Transaction state logger.
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

// Import required classes.

import java.util.logging.Logger;
import java.util.logging.Level;
import com.sun.logging.LogDomains;
import com.sun.jts.utils.LogFormatter;


/**The Log class provides operations that control the physical log
 * as an entity versus the individual LogFiles that form the log. It supports
 * the initialisation, opening and termination of the log. Different physical
 * logs can be placed on the system with only minor changes to the methods
 * contained in this class.
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
//-----------------------------------------------------------------------------

class Log {

    /**A reference to the LogControl object.
     */
    private LogControl logControl = null;

    /**The log path.
     */
    // private static String logPath = null;
    private String logPath = null;
	/*
		Logger to log transaction messages
	*/
	static Logger _logger = LogDomains.getLogger(Log.class, LogDomains.TRANSACTION_LOGGER);

    /**Default Log constructor.
     *
     * @param
     *
     * @return
     *
     * @see
     */
    Log() {
        // We need to ensure that messaging is initialised as this may be called
        // prior to SOMTR_Init.

        // Initialise the instance variables.
        logPath = LogControl.getLogPath();

    }


    Log(String logPath) {
        this.logPath = logPath;
    }

    /**Initialises the log.
     *
     * @param
     *
     * @return
     *
     * @see
     */
    boolean initialise() {
        boolean result = true;

        // Call the initialize operation for the log

        logControl = new LogControl();
        logControl.initLog(false,false,logPath);

        return result;
    }


    /**Opens the log for the given server.
     * <p>
     * The given LogSOS object, if any, will be called in the event of the log
     * going short-on-storage. A LogFile object reference is returned that is used
     * for operations on the specific portion of the log.
     *
     * @param serverName  The name of the server whose log file is being opened.
     * @param upcall      The object which will handle upcalls from the log.
     *
     * @return  The object representing the physical log file.
     *
     * @see
     */
    LogFile open( String          serverName,
                  LogUpcallTarget upcall ) {

        LogFile logFile = null;

        boolean[] newLog = new boolean[1];  newLog[0] = true;

        // Open the log using the server name.

        try {
            LogHandle handle = logControl.openFile(serverName,upcall,null,newLog);

            // Create a new LogFile object with the handle to represent the open log.

            logFile = new LogFile(handle);
        }

        // If the log open failed, report the error.

        catch( LogException le ) {
			_logger.log(Level.SEVERE,"jts.log_error",le);
			 String msg = LogFormatter.getLocalizedMessage(_logger,"jts.log_error",
			 			new java.lang.Object[] {le.toString()});
			 throw  (org.omg.CORBA.INTERNAL) (new org.omg.CORBA.INTERNAL(msg)).initCause(le);
        }

        return logFile;
    }

    /**Terminates the log.
     *
     * @param
     *
     * @return
     *
     * @see
     */
    boolean terminate() {

        boolean result = true;

        // No special action needed after the close for the logger

        return result;
    }

    /**Determines whether a log file exists for the given server.
     * <p>
     * This method may be used without initialising the Log object to determine
     * whether recovery should be performed, without initialising the log or the OTS.
     *
     * @param String
     *
     * @return
     *
     * @see
     */
    static boolean checkFileExists( String serverName ) {
        // Check whether the file exists.
        boolean exists = false;

        if( serverName != null ) {
            String logPath = LogControl.getLogPath();
            exists = LogControl.checkFileExists(serverName,logPath);
        }

        return exists;
    }

    /**Dumps the state of the object.
     *
     * @param
     *
     * @return
     *
     * @see
     */
    void dump() {
        //! somtrDUMP_OBJECT_HEADER;

        // Dump all of the instance variables in the LogFile object, without going
        // any further down object references.

        logControl.dump();
    }
}

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
// Module:      LogFile.java
//
// Description: Log File interface.
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

import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import com.sun.logging.LogDomains;
import com.sun.jts.utils.LogFormatter;

/**The LogFile interface provides operations that control the
 * individual log entries that make up the physical log. It allows writing to
 * the log and reading from the log, along with the capability to close a
 * portion of the log. Different physical logs can be placed on the system
 * with only minor changes to the methods contained in this class.
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

class LogFile {

    /**Constants for write types.
     */
    final static int UNFORCED = 0;
    final static int FORCED   = 1;

    /**Constants for log record types.
     */
    final static int NORMAL         = 0;
    final static int KEYPOINT_START = 1;
    final static int KEYPOINT_END   = 2;
    final static int REWRITE        = 3;
	
	/*
		Logger to log transaction messages
	*/ 
	
	static Logger _logger = LogDomains.getLogger(LogFile.class, LogDomains.TRANSACTION_LOGGER);

    /**The handle of the log file.
     */
    LogHandle handle = null;

    /**LogFile constructor.
     *
     * @param LogHandle
     *
     * @return
     *
     * @see
     */
    LogFile( LogHandle handle ) {

        // Set up the instance variables to those values passed in.

        this.handle = handle;

    }

    /**Writes a log record to the physical log.
     * <p>
     * Supports either a force or unforced option with force requiring an immediate
     * write to the log and unforced keeping the data until a force is done somewhere
     * else in the log service.
     * <p>
     * The LSN of the written log record is an output parameter.
     * <p>
     * Returns true if the write completed successfully and false if the write
     * did not complete.
     *
     * @param writeType   Forced/non-forced write indicator.
     * @param record      Log record data.
     * @param recordType  Log record type.
     * @param recordLSN   LSN of the written record.
     *
     * @return
     *
     * @see
     */
    synchronized boolean write( int    writeType,
                                byte[] record,
                                int    recordType,
                                LogLSN recordLSN ) {

        boolean result = true;

        // Write the record.
        // Set the result based on return code from log write.

        try {
            LogLSN resultLSN = handle.writeRecord(record,recordType,
                                                  (writeType==LogFile.FORCED ? LogHandle.FORCE : LogHandle.BUFFER));
            if( recordLSN != null )
                recordLSN.copy(resultLSN);
        } catch( LogException le ) {
			_logger.log(Level.SEVERE,"jts.log_error",le.toString());
			 String msg = LogFormatter.getLocalizedMessage(_logger,"jts.log_error",
                                        new java.lang.Object[] {le.toString()});
			 throw (org.omg.CORBA.INTERNAL) (new org.omg.CORBA.INTERNAL(msg)).initCause(le);
            //if( recordLSN != null )
            //recordLSN.copy(LogLSN.NULL_LSN);
            //result = false;
        }

        return result;
    }

    /**Informs the log that all log records older than the one with the given LSN
     * are no longer required.
     * <p>
     * The checkpoint marks the point where log processing will begin in the event
     * of recovery processing. This will generally correspond to the last record
     * before a successful keypoint.
     *
     * @param firstLSN
     *
     * @return
     *
     * @see
     */
    synchronized boolean checkpoint( LogLSN firstLSN ) {

        boolean result = true;
        LogLSN checkLSN;

        // If the LSN passed in is NULL, assume it means the head.

        if( firstLSN.isNULL() )
            checkLSN = new LogLSN(LogLSN.HEAD_LSN);
        else
            checkLSN = new LogLSN(firstLSN);

        // Checkpoint the log.

        try {
            handle.checkLSN(checkLSN);
            handle.truncate(checkLSN,LogHandle.TAIL_NOT_INCLUSIVE);
        } catch( LogException le ) {
            result = false;
        }

        return result;
    }

    /**Writes the given information in the restart record for the log.
     * <p>
     *
     * @param record  The information to be written.
     *
     * @return  Indicates success of the operation.
     *
     * @see
     */
    synchronized boolean writeRestart( byte[] record ) {

        boolean result = false;

        // Write the restart information.

        try {
            handle.writeRestart(record);
            result = true;
        } catch( LogException le ) {
            result = false;
        }

        return result;
    }

    /**Reads the restart record from the log.
     * <p>
     *
     * @param
     *
     * @return  The restart record.
     *
     * @see
     */
    synchronized byte[] readRestart() {
        byte[] result = null;

        // Write the restart information.

        try {
            result = handle.readRestart();
        } catch( LogException le ) {
        }

        return result;
    }

    /**Closes the portion of the log defined by the LogFile object reference.
     * <p>
     * Deletes the associated logfile if requested.
     *
     * @param deleteFile
     *
     * @return
     *
     * @see
     */
    synchronized boolean close( boolean deleteFile ) {

        boolean result = true;

        // Call to close the physical log.

        try {
            handle.closeFile(deleteFile);
        } catch( LogException le ) {
            result = false;
        }

        return result;
    }

    /**Returns all of the log records written to the log since the last checkpoint.
     * <p>
     * The caller is responsible for freeing the sequence storage.
     * <p>
     * If the log is empty, an empty sequence is returned.
     * <p>
     * The result is returned in a Vector as we do not know ahead of time how
     * many log records there are.
     *
     * @param
     *
     * @return  The log records.
     *
     * @see
     */
    synchronized Vector getLogRecords() {
        Vector logRecords = new Vector();
        boolean keypointEndFound = false;
        LogCursor logCursor;

        // Open a cursor for use with the log.

        try {
            logCursor = handle.openCursor(LogLSN.HEAD_LSN,LogLSN.TAIL_LSN);
        } catch( LogException le ) {

            return new Vector();
        }

        // Read each log record from the physical log and place in temporary queue.

        try {
            LogLSN lsn = new LogLSN();
            int[] recordType = new int[1];

            for(;;) {
                byte[] logRecord = logCursor.readCursor(recordType,lsn);

                // Process the log record depending on its type.

                switch( recordType[0] ) {

                    // If the record is a keypoint start, and we have found the end of the
                    // keypoint, then we can stop processing the log.  If the end has not been
                    // found then a failure must have occurred during the keypoint operation,
                    // so we must continue to process the log.
                    // We do not do anything with the contents of the keypoint start record.

                case LogFile.KEYPOINT_START :
                    if( keypointEndFound )
                        throw new LogException(null,LogException.LOG_END_OF_CURSOR,2);
                    break;

                    // If the record is a keypoint end, remember this so that we can stop when
                    // we find the start of the keypoint.
                    // We do not do anything with the contents of the keypoint end record.

                case LogFile.KEYPOINT_END :
                    keypointEndFound = true;
                    break;

                    // For a normal log record, add the records to the list.
                    // For a rewritten record, only add the record to the list if the
                    // keypoint end record has been found.

                case LogFile.NORMAL :
                case LogFile.REWRITE :
                    if( (recordType[0] == LogFile.NORMAL) || keypointEndFound )
                        logRecords.addElement(logRecord);
                    break;

                    // Any other type of log record is ignored.

                default :
                    break;
                }
            }
        } catch( LogException le ) {

            // If any exception other that END_OF_CURSOR was thrown, then return an empty
            // list.

            if( le.errorCode != LogException.LOG_END_OF_CURSOR ) {
                return new Vector();
            }
        }

        // Close the cursor.

        try {
            handle.closeCursor(logCursor); 
        } catch( LogException le ) {
        }

        return logRecords;
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
        //! somtrDump_OBJECT_HEADER;

        // Dump all of the instance variables in the LogFile object, without going
        // any further down object references.

    }
}

/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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
// Module:      LogControl.java
//
// Description: Log control file interface.
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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.*;

import com.sun.enterprise.util.i18n.StringManager;
import com.sun.logging.LogDomains;

/**This class holds the top level information for an instance of the log,
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
//------------------------------------------------------------------------------

public class LogControl {
    private static final StringManager sm = StringManager.getManager(LogControl.class);

    private static Logger _logger = LogDomains.getLogger(LogControl.class, LogDomains.TRANSACTION_LOGGER);

    /**Constants for file name extensions.
     */
    private final static String CUSHION_NAME = "cushion"/*#Frozen*/;
    private final static String EXTENT_NAME  = "extent."/*#Frozen*/;
    private final static String CONTROL_NAME = "control"/*#Frozen*/;
    public final static String RECOVERY_STRING_FILE_NAME = "recoveryfile"/*#Frozen*/;
    public final static String RECOVERY_LOCK_FILE_NAME = "recoverylockfile"/*#Frozen*/;
	//START IASRI 4721336
    //private final static String LOG_EXTENSION = ".ld"/*#Frozen*/;
    private final static String LOG_EXTENSION = ""/*#Frozen*/;
	//END IASRI 4721336
    private final static char[] EXTENT_CHARS = { 'e','x','t','e','n','t','.','0','0','0' };

    /**Internal instance members
     */
    boolean logInitialised = false;
    boolean logReadOnly = false;
    Vector  logHandles = null;
    String  directoryPath = null;
    File    controlFile = null;
    File    cushionFile = null;

    //static processSharedLog = GlobLock();

    /**Initialises the log in the given directory.
     *
     * @param coldStart     Cold start indicator.
     * @param readOnly      Read only log indicator.
     * @param logDirectory  Directory for log files.
     *
     * @return
     *
     * @see
     */
    synchronized void initLog( boolean coldStart,
                               boolean readOnly,
                               String  logDirectory ) {

        // Trace the global state of the log.

        // IF logInitialised
        //    Unlock the LogGlobalMutex
        //    Return LOG_SUCCESS

        if( logInitialised ) {
            return;
        }

        logReadOnly = readOnly;

        // Store the log directory name for use by subsequent log functions.

        directoryPath = logDirectory;

        // If this is a cold start, then remove all files in the log directory

        if( coldStart && !readOnly )
            clearDirectory(logDirectory);

        // Create the Vector which will hold the LogHandles.

        logHandles = new Vector();

        // Set the logInitialised flag to TRUE
        // Unlock the logGlobalMutex
        // Return LOG_SUCCESS

        logInitialised = true;

    }

    /**Opens a log file with the given name and upcall.
     *
     * @param logName       Name of log.
     * @param upcallTarget  Upcall for log file.
     * @param baseNewName   Base of new name for log file.
     * @param newlyCreated  A one-element array to hold the newly created indicator.
     *
     * @return  Handle for the log file.
     *
     * @exception LogException  The open failed.
     *
     * @see
     */
    synchronized LogHandle openFile( String          logFileName,
                                     LogUpcallTarget upcallTarget,
                                     String          baseNewName,
                                     boolean[]       newlyCreated )
        throws LogException {

        // IF not LogInitialised
        //   Return LOG_NOT_INITIALISED

        if( !logInitialised )
            throw new LogException(null,LogException.LOG_NOT_INITIALISED,1);

        // If the logid provided is an Alias name, it could be more than 8
        // characters. If so, to support the FAT file system, a unique
        // 8 character must be generated
        // If the name is an Alias name, then the base new name to use will have
        // been passed in as a parameter.

        String logName = null;

        /*  if( baseNewName != null )
            {
            int i = 0;

            // Determine the index to start allocating from based on the base

            if( baseNewName.length() != 0 )
            {
            try
            { i = Integer.parseInt(baseNewName.substring(LogHandle.FILENAME_PREFIX_LEN+1,LogHandle.FILENAME_PREFIX_LEN+3),16); }
            catch( Throwable e ) {}
            i++;
            }

            // Generate a new name from the base.

            boolean logExists;
            for( logExists = true;
            i <= LogHandle.MAX_NAMES && logExists;
            i++)
            {
            logName = new String(FILENAME_PREFIX);
            if( i < 100 ) logName += "0";
            if( i < 10  ) logName += "0";
            logName += Integer.toString(i);
            logExists = checkFileExists(logName,directoryPath);
            }
            }
            else
        */    {

            // If the log Id is provided is not an Alias make make sure it
            // meets the FAT file system requirements

            /*    if( logFileName.length() != LogHandle.NAME_LENGTH )
                  throw new LogException(null,LogException.LOG_INVALID_FILE_DESCRIPTOR,19);
            */
            logName = logFileName;
            File logDir = directory(logName,directoryPath);
            if( !logDir.exists() ) {
                boolean created = logDir.mkdirs();
                if (!created)
                     _logger.log(Level.WARNING,"jts.exception_creating_log_directory",logDir);
            }
        }

        // Build name of log's control file, e.g. (<logname>.control)
        // Issue OPEN request for control file, specifying Read/Write, Create,
        // No-share and 'guaranteed write to disk' options
        // IF OPEN is not successful
        //   Return LOG_OPEN_FAILURE

        controlFile = controlFile(logName,directoryPath);
        cushionFile = cushionFile(logName);

        int openOptions = LogFileHandle.OPEN_RDWR  |
            LogFileHandle.OPEN_CREAT |
            LogFileHandle.OPEN_SYNC; // Default open options
        if( logReadOnly )
            openOptions = LogFileHandle.OPEN_RDONLY;

        LogFileHandle controlFH;
        try {
            controlFH = new LogFileHandle(controlFile,openOptions);
        } catch( LogException le ) {
            throw new LogException(LogException.LOG_OPEN_FAILURE, 3,
                sm.getString("jts.log_create_LogFileHandle_failed", controlFile), le);
        }

        // Allocate a Log_FileDescriptor block and initialise it
        // IF allocate fails
        //   Close the control file
        //   Return LOG_INSUFFICIENT_MEMORY

        LogHandle logHandle = null;
        try {
            logHandle = new LogHandle(this,logName,controlFH,upcallTarget); }
        catch( LogException le ) {
            controlFH.destroy();
            throw new LogException(LogException.LOG_INSUFFICIENT_MEMORY,4,
                    sm.getString("jts.log_create_LogHandle_failed", logName), le);
        }


        // Call Log_RestoreCushion to create/check the cushion file exists
        // for this logfile. If this fails the log cannot be opened.

        try {
            logHandle.restoreCushion(false); 
        } catch( LogException le ) {
            controlFH.destroy();
            throw new LogException(LogException.LOG_INSUFFICIENT_MEMORY,9,
                    sm.getString("jts.log_cushion_file_failed"), le);
        }

        // Issue a READ request for the control file, specifying the
        // Log_ControlDescriptor structure within the Log_FileDescriptor
        // block as the input buffer
        // IF not successful (rc == -1)
        //   Close the control file
        //   Deallocate the Log_FileDescriptor block
        //   Return LOG_READ_FAILURE

        byte[] controlBytes = new byte[LogControlDescriptor.SIZEOF];
        int bytesRead = 0;
        try {
            bytesRead = controlFH.fileRead(controlBytes); 
        } catch( LogException le ) {
            controlFH.destroy();
            throw new LogException(LogException.LOG_READ_FAILURE,5,
                    sm.getString("jts.log_control_file_read_failed"), le);
        }

        if( bytesRead == 0 ) {

            // IF the READ returned EOF (rc == 0), continue with initialisation of
            // the Log_ControlDescriptor structure within the
            // Log_FileDescriptor block
            // - Initialise the log head LSN to LOG_NULL_LSN
            // - Initialise the log tail LSN to LOG_FIRST_LSN
            // - Initialise the next free LSN to LOG_FIRST_LSN
            // - Initialise the RestartDataLength in the Log_FileDescriptor
            //   block to 0
            //
            // Set NewlyCreated parameter to TRUE
            //
            // Pre-allocate all the file storage for the control file
            //
            // Set RecordsWritten to LOG_CONTROL_FORCE_INTERVAL so on
            // the first write to the log the newly created control data
            // is written to disk. If this is not done then data could be lost.
            //
            // Write the control data to file
            //
            // Open the first extent file and allocate the storage it requires.
            // If either step fails close and erase the control file and
            // return LOG_INSUFFICIENT_STORAGE.

            logHandle.logControlDescriptor.headLSN.copy(LogLSN.NULL_LSN);
            logHandle.logControlDescriptor.tailLSN.copy(LogLSN.FIRST_LSN);
            logHandle.logControlDescriptor.nextLSN.copy(LogLSN.FIRST_LSN);
            logHandle.restartDataLength = 0;
            logHandle.recordsWritten = LogHandle.CONTROL_FORCE_INTERVAL;
            newlyCreated[0] = true;

            if( !logReadOnly ) {
                int bytesWritten;

                try {
                    controlFH.allocFileStorage(LogHandle.CONTROL_FILE_SIZE); 
                } catch( LogException le ) {
                    controlFH.destroy();
                    throw new LogException(LogException.LOG_WRITE_FAILURE,6,
                            sm.getString("jts.log_allocate_failed"), le);
                }

                logHandle.logControlDescriptor.toBytes(controlBytes,0);
                try {
                    bytesWritten = controlFH.fileWrite(controlBytes);
                } catch( LogException le ) {
                    controlFH.destroy();
                    throw new LogException(LogException.LOG_WRITE_FAILURE,7,
                            sm.getString("jts.log_control_file_write_failed"), le);
                }

                LogExtent logEDP = null;
                try {
                    logEDP = logHandle.openExtent(logHandle.logControlDescriptor.nextLSN.extent);
                } catch( LogException le ) {
                    controlFH.destroy();
                    throw new LogException(LogException.LOG_NO_SPACE,10,
                            sm.getString("jts.log_open_extend_failed"), le);
                }

                try {
                    logEDP.fileHandle.allocFileStorage(LogHandle.ALLOCATE_SIZE); 
                } catch( LogException le ) {
                    controlFH.destroy();
                    throw new LogException(LogException.LOG_NO_SPACE,11,
                            sm.getString("jts.log_allocate_failed"), le);
                }

                logHandle.chunkRemaining = LogHandle.ALLOCATE_SIZE;
            }
        }

        // Otherwise the log already exists.

        else {
            int timeStampRec1;                    // The time stamp in restart rec 1
            int timeStampRec2;                    // The time stamp in restart rec 2
            int lengthRec1;                       // The length of restart record 1
            int lengthRec2;                       // The length of restart record 1

            // Set NewlyCreated parameter to FALSE

            newlyCreated[0] = false;

            // Open all of the active extents and
            // rebuild the extent table, starting form the extent
            // containing the Tail LSN and finishing with the extent
            // containing the Head LSN

            logHandle.logControlDescriptor = new LogControlDescriptor(controlBytes,0);

            LogExtent logEDP = null;
            for( int currentExtent = logHandle.logControlDescriptor.tailLSN.extent;
                 currentExtent <= logHandle.logControlDescriptor.headLSN.extent ||
                     currentExtent <= logHandle.logControlDescriptor.nextLSN.extent;
                 currentExtent++)
                try {
                    logEDP = logHandle.openExtent(currentExtent); 
                } catch( LogException le ) {
                    controlFH.destroy();
                    throw new LogException(LogException.LOG_OPEN_EXTENT_FAILURE,19,
                            sm.getString("jts.log_open_extend_failed"), le);
                }

            // Read the restart data for restart record one
            // If the read failed then
            //   Close the control file
            //   Deallocate the Log_FileDescriptor block
            //   Return LOG_READ_FAILURE

            int[] restartValues1 = new int[2];
            int[] restartValues2 = new int[2];

            try {
                logHandle.checkRestart(controlFH,1,restartValues1); 
            } catch( LogException le ) {
                controlFH.destroy();
                throw new LogException(LogException.LOG_READ_FAILURE,8,
                        sm.getString("jts.log_read_restart_data_failed"), le);
            }

            // Check that the record length was not equal to zero
            // IF the record length was not equal to zero
            //   READ the next restart record
            //   IF ( (Length2 !=0) && (Time2 > Time1)
            //   THEN CurrentValid is 2
            //   ELSE CurrentValid is 1

            // BUGFIX(Ram Jeyaraman) Always check both the restart records,
            // even though the first record might have zero data length.
            // It is possible, that the second record might 
            // have non-zero data length with a later
            // timestamp, even though the first record has zero data length.
            // Fix is to comment out the check below.
            //if (restartValues1[0] != 0)
            {

                // If the read failed then
                //   Close the control file
                //   Deallocate the Log_FileDescriptor block
                //   Return LOG_READ_FAILURE

                try {
                    logHandle.checkRestart(controlFH,2,restartValues2);
                } catch( LogException le ) {
                    controlFH.destroy();
                    throw new LogException(LogException.LOG_READ_FAILURE,9,
                            sm.getString("jts.log_check_restart_failed"), le);
                }

                if( restartValues2[0] != 0 &&
                    restartValues2[1] > restartValues1[1] ) {
                    logHandle.activeRestartVersion = 2;
                    logHandle.restartDataLength    = restartValues2[0];
                } else {
                    logHandle.activeRestartVersion = 1;
                    logHandle.restartDataLength    = restartValues1[0];
                }
            }

            if( logHandle.logControlDescriptor.headLSN.isNULL() )
                logHandle.recordsWritten = LogHandle.CONTROL_FORCE_INTERVAL;
        }

        // Add the Log_FileDescriptor block to the head of the
        // (LogFileDescriptorHead) chain hung off the RCA and update the backward
        // chain pointer of the block which was previously at the head of the chain.

        logHandles.addElement(logHandle);

        // IF the head LSN in Log_ControlDescriptor != LOG_NULL_LSN
        // Build the extent file name from the head LSN
        // Issue an OPEN request for the file

        if( !logHandle.logControlDescriptor.headLSN.isNULL() ) {
            int offset;                           // Present offset in the open extent
            LogRecordHeader extentRec,            // An extent record header
                headRec,              // An extent record header
                linkRec;              // An extent record header
            boolean lastValidRead = false;        // Has the last valid record be read
            LogExtent logEDP = null;

            // IF not successful (rc == -1)
            //   Close the control file
            //   Unchain the LogFDP and free up the storage
            //   Return LOG_OPEN_FAILURE

            // Allocate a Log_ExtentDescriptor block and initialise it
            // Hash the extent number to find the anchor for this extent in the
            // hash table and add it to the head of the chain
            // Move the file pointer to the head LSN record (using LSEEK)

            try {
                logEDP = logHandle.positionFilePointer(logHandle.logControlDescriptor.headLSN,
                                                       0,LogExtent.ACCESSTYPE_READ); }
            catch( LogException le ) {
                controlFH.destroy();
                removeFile(logHandle);
                throw new LogException(LogException.LOG_OPEN_FAILURE,10,
                        sm.getString("jts.log_position_file_pointer_failed"), le);
            }

            // Issue a READ for the record header
            // IF the read is not successful (rc <= 0)
            //   Close the control file
            //   Deallocate newly acquired control blocks
            //   Return LOG_READ_FAILURE
            //
            // Check that the LSN in the record header matches the head LSN
            // IF there is a mismatch
            //   Deallocate newly acquired control blocks
            //   Return LOG_CORRUPTED

            byte[] headerBytes = new byte[LogRecordHeader.SIZEOF];

            try {
                bytesRead = logEDP.fileHandle.fileRead(headerBytes);
            } catch( LogException le ) {
                controlFH.destroy();
                removeFile(logHandle);
                throw new LogException(le.errorCode,11,
                        sm.getString("jts.log_read_header_failed"), le);
            }
            extentRec = new LogRecordHeader(headerBytes,0);

            if( !extentRec.currentLSN.equals(logHandle.logControlDescriptor.headLSN) ) {
                controlFH.destroy();
                removeFile(logHandle);
                throw new LogException(null,LogException.LOG_CORRUPTED,12);
            }

            // Copy the record header to HEADERCOPY
            // Move the file pointer to the 'next LSN' in record header

            logEDP.cursorPosition += bytesRead;
            headRec = new LogRecordHeader();
            headRec.copy(extentRec);
            offset = headRec.nextLSN.offset;

            try {
                logEDP = logHandle.positionFilePointer(extentRec.nextLSN,0,LogExtent.ACCESSTYPE_READ);
            } catch( LogException le ) {
                controlFH.destroy();
                removeFile(logHandle);
                throw new LogException(LogException.LOG_OPEN_FAILURE,13,
                        sm.getString("jts.log_position_file_pointer_failed"), le);
            }

            linkRec = new LogRecordHeader();

            // LOOP until last valid log record has been read

            lastValidRead = false;
            do {
                // Issue a read for the record header

                offset = extentRec.nextLSN.offset;

                try {
                    bytesRead = logEDP.fileHandle.fileRead(headerBytes);
                } catch( LogException le ) {
                    controlFH.destroy();
                    removeFile(logHandle);
                    throw new LogException(LogException.LOG_READ_FAILURE,14,
                            sm.getString("jts.log_read_header_failed"), le);
                }
                if( bytesRead == -1 ) {
                    controlFH.destroy();
                    removeFile(logHandle);
                    throw new LogException(null,LogException.LOG_READ_FAILURE,14);
                }

                extentRec = new LogRecordHeader(headerBytes,0);
                logEDP.cursorPosition += bytesRead;

                // IF the LSN in the record header matches the LSN of the
                // current position in the extent file

                if( extentRec.currentLSN.offset == offset ) {
                    // IF its a link record

                    if( extentRec.recordType == LogHandle.LINK ) {
                        // Copy it to LINKCOPY
                        // 'Move' the file pointer to the NextRecord value (in the next extent file)
                        // Iterate.

                        linkRec.copy(extentRec);
                        try {
                            logEDP = logHandle.positionFilePointer(extentRec.nextLSN,0,LogExtent.ACCESSTYPE_READ);
                        } catch( LogException le ) {
                            controlFH.destroy();
                            removeFile(logHandle);
                            throw new LogException(le.errorCode,15,
                                    sm.getString("jts.log_position_file_pointer_failed"), le);
                        }
                        continue;
                    } else

                        // IF LINKCOPY is not null AND LINKCOPY.NextRecord LSN &lnot.= ThisRecord LSN
                        //   Set LINKCOPY to null

                        if( !linkRec.currentLSN.isNULL() &&
                            !linkRec.nextLSN.equals(extentRec.currentLSN) )
                            linkRec = new LogRecordHeader();

                    // Copy the record header to HEADERCOPY
                    // Use the NextRecord value to move the file pointer to the next record header

                    headRec.copy(extentRec);
                    try {
                        logEDP = logHandle.positionFilePointer(extentRec.nextLSN,0,LogExtent.ACCESSTYPE_READ);
                    } catch( Throwable e ) {}
                } else {
                    LogRecordEnding endRec;           // An ending record type

                    lastValidRead = true;

                    // Use the ThisRecord value from HEADERCOPY together with the
                    // RecordLength value to calculate the position of the previous
                    // Log_RecordEnding structure
                    //
                    // LSEEK to this position and read it

                    try {
                        logEDP = logHandle.positionFilePointer(headRec.currentLSN,
                                                               LogRecordHeader.SIZEOF+headRec.recordLength,
                                                               LogExtent.ACCESSTYPE_READ); 
                    } catch( LogException le ) {
                        controlFH.destroy();
                        removeFile(logHandle);
                        throw new LogException(le.errorCode,16,
                                sm.getString("jts.log_position_file_pointer_failed"), le);
                    }

                    byte[] endingBytes = new byte[LogRecordEnding.SIZEOF];

                    try {
                        bytesRead = logEDP.fileHandle.fileRead(endingBytes);
                    } catch( LogException le ) {
                        controlFH.destroy();
                        removeFile(logHandle);
                        throw new LogException(le.errorCode,17, null, le);
                    }

                    endRec = new LogRecordEnding(endingBytes,0);

                    logEDP.cursorPosition += bytesRead;

                    // IF its value is the same as ThisRecord LSN This is the last valid record
                    // Update head LSN in control data with ThisRecord LSN
                    // Update next LSN in control data with the NextRecord LSN

                    if( endRec.currentLSN.equals(headRec.currentLSN) ) {
                        logHandle.logControlDescriptor.headLSN.copy(headRec.currentLSN);
                        logHandle.logControlDescriptor.nextLSN.copy(headRec.nextLSN);
                    }

                    // This is an invalid record, so record details of previous valid record
                    // IF LINKCOPY is null
                    // Update head LSN in control data with the PreviousRecord LSN
                    // Update next LSN in control data with ThisRecord LSN

                    else {
                        if( linkRec.currentLSN.isNULL() ) {
                            logHandle.logControlDescriptor.headLSN.copy(headRec.previousLSN);
                            logHandle.logControlDescriptor.nextLSN.copy(headRec.currentLSN);
                        }

                        // Otherwise update head LSN in control data with value from LINKCOPY

                        else { 
                            logHandle.logControlDescriptor.headLSN.copy(linkRec.previousLSN);
                            logHandle.logControlDescriptor.nextLSN.copy(linkRec.currentLSN);
                        }
                    }
                }

                // Increase the number of records written since the last force

                logHandle.recordsWritten++;
            }
            while( !lastValidRead );
        }

        // Store the address of the Log_FileDescriptor block into its BlockValid field

        logHandle.blockValid = logHandle;

        return logHandle;
    }

    /**Cleans up the given log file.
     *
     * @param logHandle  The log to clean up.
     *
     * @return
     *
     * @exception LogException  The operation failed.
     *
     * @see
     */
    synchronized void cleanUp( LogHandle logHandle ) 
        throws LogException {

        // IF not LogInitialised Return LOG_NOT_INITIALISED

        if( !logInitialised )
            throw new LogException(null,LogException.LOG_NOT_INITIALISED,1);

        // Check BlockValid field in Log_FileDescriptor block and
        // ensure it is valid
        // IF not valid Log_FileDescriptor
        //   Return LOG_INVALID_FILE_DESCRIPTOR

        if( logHandle == null || logHandle.blockValid != logHandle )
            throw new LogException(LogException.LOG_INVALID_FILE_DESCRIPTOR,2,
                sm.getString("jts.log_invalid_file_descriptor", logHandle), (Throwable) null);

        // Set the block valid to NULL

        logHandle.blockValid = null;

        // Remove all extent entries from the log file's extent hash table

        logHandle.cleanUpExtents();

        // Unchain the Log_FileDescriptor block from the RCA chain.  The
        // mutices contained in the FD latch structure will already have been
        // freed up when the AS process which owned them died.  We indicate this
        // by setting the ProcessOwnsLatch parameter to FALSE.

        removeFile(logHandle);

    }

    /**Determines whether the given named log exists in the given directory.
     *
     * @param logId         The log identifier.
     * @param logDirectory  The log directory.
     *
     * @return  Indicates whether the file exists.
     *
     * @see
     */

    static boolean checkFileExists( String logId,
                                    String logDirectory ) {

		//START IASRI 4730519
		if(logDirectory==null)
		    return false;
		//END IASRI 4730519
        boolean exists = controlFile(logId,logDirectory).exists();

        return exists;
    }

    /**Removes the log file from the chain.
     *
     * @param logHandle
     *
     * @return
     *
     * @see
     */
    synchronized void removeFile( LogHandle logHandle ) {

        // Unchain the Log_FileDescriptor block from the RCA chain

        logHandles.removeElement(logHandle);

        // Clear the BlockValid field in the Log_FileDescriptor block

        logHandle.blockValid = null;

    }

    /**Dumps the state of the object.
     *
     * @param
     *
     * @return
     *
     * @see
     */
    //----------------------------------------------------------------------------
    void dump() {
        // If the log has been initialised the pointer to the global
        // data will have been initialised. So, if the pointer to the
        // global data is not NULL, dump out the its contents.
    }

    /**Clears out all log files from the given directory.
     *
     * @param logDir
     *
     * @return
     *
     * @see
     */
    static void clearDirectory( String logDir ) {
        // Find each control file in turn and then delete all files that
        // begin with the same name. This will delete all files associated
        // with that log (including the control file so that it is not found
        // again next time through the loop)

        File directory = new File(logDir);
        String[] allFiles = directory.list();

        for( int i = 0; i < allFiles.length; i++ ) {

            // Determine if the file is actually a log subdirectory.  If so, use it's name
            // to delete all associated log files.
            if( allFiles[i].endsWith(LOG_EXTENSION) ) {
                //Start IASRI 4720539
                final File logFileDir = new File(directory,allFiles[i]);
                if( logFileDir.isDirectory() ) {
                    final String[] logFiles = logFileDir.list();
                    /*
                    for( int j = 0; j < logFiles.length; j++ )
                        new File(logFileDir,logFiles[j]).delete();
                    logFileDir.delete();
                    */                                        
                    java.security.AccessController.doPrivileged(
                        new java.security.PrivilegedAction() {
                            public Object run(){
                                for( int j = 0; j < logFiles.length; j++ ){
                                    new File(logFileDir,logFiles[j]).delete();                                    
                                }
                                return null;
                            }
                        }
                    );                    
                    java.security.AccessController.doPrivileged(
                        new java.security.PrivilegedAction() {
                            public Object run(){
                                logFileDir.delete();
                                return null;
                            }
                        }
                    );                    
                }
                //End IASRI 4720539
            }            
        }

    }

    /**Builds a log extent file.
     *
     * @param logId   Log identifier.
     * @param extent  Extent number.
     *
     * @return  A File object representing the extent file.
     *
     * @see
     */
    File extentFile( String logId,
                     int    extent ) {

        char[] buff = (char[])EXTENT_CHARS.clone();

        int tmpExtent   = extent / LogExtent.EXTENT_RADIX;
        int extentLow  = extent % LogExtent.EXTENT_RADIX;
        int extentMid  = tmpExtent % LogExtent.EXTENT_RADIX;
        int extentHigh = tmpExtent / LogExtent.EXTENT_RADIX;

        buff[7]  = (char)(extentHigh + (extentHigh > 9 ? 'A'-10 : '0'));
        buff[8]  = (char)(extentMid  + (extentMid  > 9 ? 'A'-10 : '0'));
        buff[9] = (char)(extentLow  + (extentLow  > 9 ? 'A'-10 : '0'));

        String fileName = new String(buff);
        File result = new File(directory(logId,directoryPath),fileName);

        return result;
    }

    /**Builds a log control file.
     *
     * @param logId   Log identifier.
     * @param logDir  Log directory.
     *
     * @return  A File object representing the control file.
     *
     * @see
     */
    final static File controlFile( String logId, String logDir ) {
        File result = new File(directory(logId,logDir),CONTROL_NAME);
        return result;
    }

    /**Builds a log cushion file.
     *
     * @param logId  Log identifier.
     *
     * @return  A File object representing the cushion file.
     *
     * @see
     */
    final File cushionFile( String logId ) {

        File result = new File(directory(logId,directoryPath),CUSHION_NAME);

        return result;
    }

    /**Builds a log directory file.
     *
     * @param logId   Log identifier.
     * @param logDir  Base log directory.
     *
     * @return  A File object representing the directory.
     *
     * @see
     */
    final static File directory( String logId, String logDir ) {

	//START IASRI 4721336
	//START IASRI 4730519
	if(logDir==null) //It should not be null
       	return new File( "." + File.separator + logId + LOG_EXTENSION);
	//END IASRI 4730519
    return new File(logDir);
	//END IASRI 4721336
    }

    final static File recoveryIdentifierFile(String logId, String logDir) {
        File result = new File(directory(logId,logDir),RECOVERY_STRING_FILE_NAME);
        return result;
    }

    public final static File recoveryLockFile(String logId, String logDir) {
        File dir = directory(logId,logDir);
        if( !dir.exists() ) {
            boolean created = dir.mkdirs();
            if (!created)
                 _logger.log(Level.WARNING,"jts.exception_creating_log_directory",dir);
        }
        File result = new File(dir,RECOVERY_LOCK_FILE_NAME);
        return result;
    }

    /**
     * Returns the log directory name
     */
    public static String getLogPath() {
        String logPath = null;
        int[] result = new int[1];
        logPath = Configuration.getDirectory(Configuration.LOG_DIRECTORY,
                                                 Configuration.JTS_SUBDIRECTORY,
                                                 result);

        // If a default was used, display a message.

        if( result[0] == Configuration.DEFAULT_USED ||
                result[0] == Configuration.DEFAULT_INVALID ) {

            // In the case where the SOMBASE default is used, only display a message
            // if an invalid value was specified in the environment value.

            if( logPath.length() > 0 ) {
                 _logger.log(Level.WARNING,"jts.invalid_log_path",logPath);
            }

            // In the case where the SOMBASE default is invalid, the value returned is
            // the invalid default. We then default to the current directory.

            if( result[0] == Configuration.DEFAULT_INVALID ) {
                _logger.log(Level.WARNING,"jts.invalid_default_log_path");
                logPath = "."/*#Frozen*/;
            }
        }
        return logPath;
    }

}

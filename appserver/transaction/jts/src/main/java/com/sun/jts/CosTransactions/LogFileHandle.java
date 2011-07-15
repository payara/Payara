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
// Module:      LogFileHandle.java
//
// Description: Physical log file operations.
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

import com.sun.enterprise.util.i18n.StringManager;
import java.io.*;

/**This class encapsulates file I/O operations and the file handle.
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

class LogFileHandle {
    private static final StringManager sm = StringManager.getManager(LogFileHandle.class);

    /**The log file should be accessed in read only mode.
     */
    final static int OPEN_RDONLY = 0x00000001;

    /**The log file should be accessed in read/write mode.
     */
    final static int OPEN_RDWR   = 0x00000002;

    /**The log file should be opened as a new file if necessary.
     */
    final static int OPEN_CREAT  = 0x00000004;

    /**The log file should be synchronized with the file system.
     */
    final static int OPEN_SYNC   = 0x00000008;

    /**Seek relative location to the current position.
     */
    final static int SEEK_RELATIVE = 0;

    /**Seek absolute location in the file.
     */
    final static int SEEK_ABSOLUTE = 1;

    /**Open options string for file read only.
     */
    final static String MODE_READONLY     = "r"/*#Frozen*/;     // rb

    /**Open options string for file read/write, old file.
     */
    static String MODE_READWRITEOLD = "rw"/*#Frozen*/;    // wb-

    /**open options string for file read/write, new file.
     */
    static String MODE_READWRITENEW = "rw"/*#Frozen*/;    // wb+

    static String dsyncProp = null;

    final static String DSYNC_PROPERTY = "com.sun.appserv.transaction.nofdsync";

    /**The maximum length of a file name.
     */
    //!final static int LOG_FNAME_MAX = FILENAME_MAX;
    final static int LOG_FNAME_MAX = 252;

    /**The size of a block in the file system.
     */
    final static int FILESYSTEM_BLOCKSIZE = 4096;

    /**Instance information.
     */
    private RandomAccessFile fhandle = null;
    private FileDescriptor   fd      = null;
    private byte[] bufferData = null;
    boolean buffered = false;
    int bufferUpdateStart = -1;
    int bufferUpdateEnd   = -1;
    int buffPos = 0;

    static {
        dsyncProp = System.getProperty(DSYNC_PROPERTY);
        if (dsyncProp != null) {
            MODE_READWRITEOLD = "rwd";   
            MODE_READWRITENEW = "rwd"; 
        }
    }

    /**Default LogFileHandle constructor.
     *
     * @param
     *
     * @return
     *
     * @see
     */
    LogFileHandle() {
        fhandle = null;
        fd      = null;                                                       //@MA

    }

    /**Creates a new file handle for the given file.
     *
     * @param file  The File to be opened.
     * @param int   Open options
     *
     * @return
     *
     * @exception LogException Opening the file failed.
     *
     * @see
     */
    LogFileHandle( File  file,
                   int   openOptions ) 
        throws LogException {

        // Perform buffering depending on the flag.

        if (dsyncProp == null) {
            if( (openOptions & OPEN_SYNC) == 0 )
                buffered = true;
        }

        // Change the OpenOptions to the format expected by CLOSE

        if( (openOptions & OPEN_RDONLY) != 0 )
            fileOpen(file,MODE_READONLY);
        else
            try {
                fileOpen(file,MODE_READWRITEOLD);
            } catch( LogException e ) {
                if( (openOptions & OPEN_CREAT) != 0 )
                    fileOpen(file,MODE_READWRITENEW);
            }
    }

    /**Destroys the FileHandle, closing the file, if open.
     *
     * @param
     *
     * @return
     *
     * @exception LogException The close of the file failed.
     *
     * @see
     */
    void destroy()
        throws LogException {

        // Ensure that the file is closed.
        // If the file is buffered, this ensures that the buffer is written out
        // if necessary.

        if( fhandle != null )
            fileClose();
    }

    protected void finalize()
        throws LogException {

        destroy();
    }

    /**Reads from the file.
     *
     * @param buffer  Buffer for file read.
     *
     * @return  Number of bytes read.
     *
     * @exception LogException  The read failed.
     *
     * @see
     */
    int fileRead( byte[] buffer )
        throws LogException{

        int bytesRead = 0;

        if( buffer.length > 0 )
            try {

                // If buffered, then copy the file buffer into the required array.

                if( buffered ) {

                    // If the current position is beyond the end of the buffer then the read fails.

                    if( buffPos >= bufferData.length )
                        bytesRead = -1;

                    // Otherwise if the buffer is not big enough for all the bytes, return those that
                    // it does contain, else return all the bytes asked for.

                    else {
                        if( buffPos + buffer.length >= bufferData.length )
                            bytesRead = bufferData.length - buffPos;
                        else
                            bytesRead = buffer.length;

                        System.arraycopy(bufferData,buffPos,buffer,0,bytesRead);
                        buffPos += bytesRead;
                    }
                }

                // Otherwise read the data from the file.

                else {
                    bytesRead = fhandle.read(buffer);
                    if( bytesRead == -1 ) bytesRead = 0;
                }
            } catch( Throwable exc ) {
                throw new LogException(LogException.LOG_READ_FAILURE, 1, 
                         sm.getString("jts.log_read_failed_bytes", bytesRead), exc);
            }

        return bytesRead;
    }

    /**Writes to the file.
     *
     * @param buffer  The bytes to write.
     *
     * @return  The number of bytes written.
     *
     * @exception LogException  The write failed.
     *
     * @see
     */
    int fileWrite( byte[] buffer )
        throws LogException {

        if( buffer.length > 0 )
            try {

                // If buffered, then copy the array into the file buffer.

                if( buffered ) {

                    // If the array copy requires more bytes than exist in the buffer, then the
                    // buffer must be extended to the required size.

                    if( buffPos + buffer.length >= bufferData.length ) {
                        byte[] newBufferData = new byte[buffPos+buffer.length];
                        if( bufferData.length > 0 )
                            System.arraycopy(bufferData,0,newBufferData,0,bufferData.length);
                        bufferData = newBufferData;
                    }

                    // Copy the data.

                    System.arraycopy(buffer,0,bufferData,buffPos,buffer.length);

                    // Remember how much of the buffer has been updated, and increase the current
                    // buffer position.

                    if( bufferUpdateStart == -1 ||
                        buffPos < bufferUpdateStart )
                        bufferUpdateStart = buffPos;

                    buffPos += buffer.length;

                    if( buffPos > bufferUpdateEnd )
                        bufferUpdateEnd = buffPos;
                }

                // Otherwise write the data to the file.
                // For non-buffered writes, we always sync to the file system.

                else {
                    fhandle.write(buffer);
                    if (dsyncProp == null)
                        fd.sync();
                }
            } catch( Throwable e ) {
                int errCode = LogException.LOG_WRITE_FAILURE;
                //$     if( errno == ENOSPC )
                //$       retCode = LogControl.LOG_NO_SPACE;
                throw new LogException(errCode, 1, sm.getString("jts.log_write_failed"), e);
            }

        return buffer.length;
    }

    /**Opens the given file.
     *
     * @param file      The name of the file.
     * @param fileMode  The mode to open in.
     *
     * @return
     *
     * @exception LogException The open failed.
     *
     * @see
     */
    void fileOpen( File   file,
                   String fileMode ) 
        throws LogException {
        fhandle = null;
        try {
            fhandle = new RandomAccessFile(file,fileMode);
            fd = fhandle.getFD();

            // If buffering, and the opened file has contents, then allocate the buffer
            // and read the file contents in.  Otherwise make the buffer an empty array.

            if( buffered )
                if( fhandle.length() > 0 ) {
                    bufferData = new byte[(int)fhandle.length()];
                    fhandle.readFully(bufferData);
                }
                else
                    bufferData = new byte[0];
        } catch( Throwable e ) {
            throw new LogException(LogException.LOG_OPEN_FAILURE,1,
                sm.getString("jts.log_open_failed", file), e);
        }

    }

    /**Closes the file.
     *
     * @param
     *
     * @return
     *
     * @exception LogException The close failed
     *
     * @see
     */
    void fileClose()
        throws LogException {

        try {

            // If buffered, then ensure that the buffer is stored and synced with the
            // file system.

            if( bufferUpdateStart != -1 )
                fileSync();

            // Close the file.

            fhandle.close();
        } catch( Throwable e ) {
            throw new LogException(LogException.LOG_CLOSE_FAILURE,1,
                    sm.getString("jts.log_close_failed"), e);
        }

        // Reset the file handle and descriptor values.

        fhandle = null;
        fd = null;                                                            //@MA

    }

    /**Seeks the given position in the file.
     *
     * @param position  Position to seek.
     * @param seekMode  Mode of seek.
     *
     * @return
     *
     * @exception LogException The seek failed.
     *
     * @see
     */

    void fileSeek( long position,
                   int  seekMode )
        throws LogException {

        // Adjust the position being sought if it is relative to the current position.

        long absPos = position;
        try {

            // If buffered, then simply set the buffer position.
            // If the position is beyond the end of the buffer, then the buffer will be
            // extended when the next write occurs.

            if( buffered ) {
                if( seekMode == SEEK_RELATIVE )
                    absPos = buffPos + position;
                buffPos = (int)absPos;
            }

            // Otherwise seek the position in the file.

            else {
                if( seekMode == SEEK_RELATIVE )
                    absPos = fhandle.getFilePointer() + position;
                fhandle.seek(absPos);
            }
        } catch( Throwable e ) {
            throw new LogException(LogException.LOG_READ_FAILURE,1,
                    sm.getString("jts.log_file_seek_failed"), e);
        }
    }

    /**Synchronises (flushes) the file to the file system.
     *
     * @param
     *
     * @return
     *
     * @exception LogException The sync failed
     *
     * @see
     */
    void fileSync() throws LogException {

        // Synchronization is only done for buffered files which have been updated.
        // Non-buffered files have every write synchronized with the file system.

        if( bufferUpdateStart != -1 )
            try {
                fhandle.seek(bufferUpdateStart);
                fhandle.write(bufferData,bufferUpdateStart,bufferUpdateEnd-bufferUpdateStart);
                if (dsyncProp == null)
                    fd.sync();

                bufferUpdateStart = -1;
                bufferUpdateEnd   = -1;
            } catch (Throwable e) {
                throw new LogException(LogException.LOG_READ_FAILURE,1,
                        sm.getString("jts.log_file_sync_failed"), e);
            }

    }

    /**Reads a vector of records from the file.
     *
     * @param vector  The vector to contain the records to be read.
     *
     * @return  The total number of bytes read.
     *
     * @exception LogException The read failed.
     *
     * @see
     */
    int readVector( byte[][] vector )
        throws LogException {
        int bytesRead = 0;
        for( int i = 0; i < vector.length; i++ )
            bytesRead += fileRead(vector[i]);

        return bytesRead;
    }

    /**Allocates more storage for the file.
     *
     * @param bytesToClear Number of bytes to allocate for the file.
     *
     * @return
     *
     * @exception LogException The allocation failed.
     *
     * @see
     */

    void allocFileStorage( int bytesToClear )
        throws LogException {
        int numberOfBlocks;                     // Number of blocks to write
        int bytesRemaining;                     // Remaining bytes
        //byte[] singleChar1 = new byte[1];
        byte[] singleChar2 = new byte[1];
        long bytesWritten;


        if( bytesToClear == 0 ) {
            return;
        }
        /* Don't bother with the compilcated version.  Just write out a byte at the
           appropriate place.

           // Calculate the number of blocks and remaining bytes

           numberOfBlocks = bytesToClear / FILESYSTEM_BLOCKSIZE;
           bytesRemaining = bytesToClear % FILESYSTEM_BLOCKSIZE;

           // Initialise the single characters to be written to force allocation.

           singleChar1[0] = (byte)0x01;
           singleChar2[0] = (byte)0xff;

           // For each block, write a single char and seek to the next block
           // multiple from the current position

           for( int i = 1; i <= numberOfBlocks; i++ )
           {

           // Write the single char at start of the file block

           fileWrite(singleChar1);

           // Now seek to the end of block

           fileSeek(FILESYSTEM_BLOCKSIZE-2,SEEK_RELATIVE);

           // Write the single char at end of the file block

           fileWrite(singleChar2);
           }

           // If there are still bytes remaining, get them allocated too.

           if( bytesRemaining > 0 )
           {

           // Now write out a byte at the beginning and end of the remaining
           // area to be allocated

           fileWrite(singleChar1);
           if( --bytesRemaining > 0 )
           {

           // Seek to end of area and write the last char

           fileSeek(bytesRemaining-1,SEEK_RELATIVE);

           // Now write a byte at the end of the remaining area to be allocated.

           fileWrite(singleChar2);
           }
           }

           This is the quick version which only does one write.
        */

        fileSeek(bytesToClear-1,SEEK_RELATIVE);
        fileWrite(singleChar2);

        // Move the file pointer back to its original location on the file
        // by seeking -BytesToClear

        fileSeek(-bytesToClear,SEEK_RELATIVE);

        // If the file is buffered, make sue the space is really allocated.

        if( buffered )
            fileSync();

    }
}

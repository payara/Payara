/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2016 Oracle and/or its affiliates. All rights reserved.
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
// Module:      CoordinatorLog.java
//
// Description: Coordinator state logging.
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
import java.io.*;

import org.omg.CORBA.*;

import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.util.logging.Logger;
import java.util.logging.Level;
import com.sun.logging.LogDomains;
import com.sun.jts.utils.LogFormatter;

/**The CoordinatorLog interface provides operations to record transaction-
 * specific information that needs to be persistently stored at a particular
 * point in time, and subsequently restored.
 * <p>
 * The CoordinatorLog contains an attribute value which is the local transaction
 * identifier associated with the transaction operating on the log. The
 * CoordinatorLog maintains the LSN of the last log record written for the
 * transaction, and a flag indicating whether rewrite is required for a
 * keypoint.
 * <p>
 * As an instance of this class may be accessed from multiple threads within
 * a process, serialisation for thread-safety and locking during keypoint is
 * necessary.
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

class CoordinatorLog extends java.lang.Object implements LogUpcallTarget {
    private static final int LOG_DEF_KEY_TRIGGER = 100;
    private static final int LOG_THRESHOLD = 10000;
    private static final int STRING_TO_REF_RETRIES = 20;
    private static final String defaultstring = "DEFAULT_LOG";

    /**
    // Since muliple logs have to coexist as part of delegated recovery 
    // support, static data can not be maintained. Now this data is stored
    // per log location
    private static LogFile logFile        = null;
    private static Log log                = null;
    private static Hashtable activeLogs   = new Hashtable();
    private static Hashtable keypointLogs = new Hashtable();
    private static int tranCount          = 0;
    private static int keypointTrigger    = 100;
    private static boolean keypointInProgress = false;
    private static java.lang.Object keypointLock = new java.lang.Object();
    private static java.lang.Object keypointStateLock = new java.lang.Object();
   **/
    private static int keypointTrigger    = 100;
    private static Hashtable logStateHoldertable = new Hashtable();
    private static final java.lang.Object NULL_ENTRY = new java.lang.Object();

    private Hashtable sectionMapping = null;
    private boolean rewriteRequired  = false;
    private boolean writeDone = false;

    private String logPath = null;

	/*
		Logger to log transaction messages
	*/
	static Logger _logger = LogDomains.getLogger(CoordinatorLog.class, LogDomains.TRANSACTION_LOGGER);
    
	/**The local transaction identifier for the transaction this object is logging.
     */
    Long localTID = null;
    CoordinatorLogStateHolder logStateHolder = null;
    private static CoordinatorLogStateHolder defaultLogStateHolder = getStateHolder(defaultstring);

    // Static variables to handle byte array formatting.

    private ByteArrayOutputStream byteOutput = new ByteArrayOutputStream(2000);
    private DataOutputStream      dataOutput = new DataOutputStream(byteOutput);

   
    // All the methods which take "String logPath" as parameter are same as the
    // ones with out that parameter. These methods are added for delegated
    // recovery support
    /**
     * Get the state for the given log location.
     * If the state does not exists, creates the state and retuns, otherwise existing
     * state is returned.
     *
     * @param str  log location.
     *
     * @return  state for the given log location.
     *
     * @see
     */
    static private CoordinatorLogStateHolder getStateHolder(String str) {
        synchronized (logStateHoldertable) {
            CoordinatorLogStateHolder logStateHolder = (CoordinatorLogStateHolder)logStateHoldertable.get(str);
            if (logStateHolder == null) {
                logStateHolder =  new CoordinatorLogStateHolder();
                 logStateHolder.logFile        = null;
                 logStateHolder.log            = null;
                 logStateHolder.activeLogs   = new Hashtable();
                 logStateHolder.keypointLogs = new Hashtable();
                 logStateHolder.tranCount          = 0;
                 logStateHolder.keypointInProgress = false;
                 // logStateHolder.keypointLock = new java.lang.Object();
                 logStateHolder.keypointLock = new RWLock();
                 logStateHolder.keypointStateLock = new java.lang.Object();
                 logStateHoldertable.put(str,logStateHolder);
            }
            return logStateHolder;
        }
    }

    /**Default CoordinatorLog constructor.
     *
     * @param
     *
     * @return
     *
     * @see
     */
    CoordinatorLog() {

        sectionMapping = new Hashtable();
        logStateHolder = defaultLogStateHolder;
        

        // Do not inform the metaclass about the existence of this object yet, as it
        // does not have a transaction identifier.

    }

    CoordinatorLog(String logPath) {

        sectionMapping = new Hashtable();
        logStateHolder = getStateHolder(logPath);
        this.logPath = logPath;
        

        // Do not inform the metaclass about the existence of this object yet, as it
        // does not have a transaction identifier.

    }

    /**Default CoordinatorLog destructor.
     *
     * @param
     *
     * @return
     *
     * @see
     */
    synchronized public void doFinalize() {

        // Clear up the section mapping.

        if( sectionMapping != null ) {
            Enumeration sections = sectionMapping.elements();
	    // the traditional way of iterating through the enumeration
	    // using sections.hasMoreElements was showing up as a
	    // hot spot during performance tests.	Arun 9/27/99
	    int sz = sectionMapping.size();
	    while (sz-- > 0) {
                CoordinatorLogSection section = (CoordinatorLogSection)sections.nextElement();
                section.reUse();
            }
            sectionMapping.clear();
            sectionMapping = null;
        }
    }

    /**
     * reUse method is called explicitly to clean up
     * and return this instance to the pool
     *
     * Note: the implementation of the cache does not ensure
     *       that when an object is re-used there are no
     *	     outstanding references to that object. However, the
     *	     risk involved is minimal since reUse() replaces the
     *	     existing call to finalize(). The existing call to
     *	     finalize also does not ensure that there are no
     *	     outstanding references to the object being finalized.
     *
     *
     * @param
     *
     * @return
     *
     * @see
     */
    synchronized private void reUse() {			// Arun 9/27/99

        // Clear up the section mapping.

        if( sectionMapping != null ) {
            Enumeration sections = sectionMapping.elements();
	    int sz = sectionMapping.size();
	    while (sz-- > 0) {
                CoordinatorLogSection section = (CoordinatorLogSection)sections.nextElement();
                section.reUse();
            }
            sectionMapping.clear();
        }
        rewriteRequired  = false;
        writeDone = false;
        localTID = null;

        byteOutput.reset();

	// cache the coordinator log in the coordinator log pool
	CoordinatorLogPool.putCoordinatorLog(this);

    }

    synchronized private void reUse(String logPath) {	

        // Clear up the section mapping.

        if( sectionMapping != null ) {
            Enumeration sections = sectionMapping.elements();
	    int sz = sectionMapping.size();
	    while (sz-- > 0) {
                CoordinatorLogSection section = (CoordinatorLogSection)sections.nextElement();
                section.reUse();
            }
            sectionMapping.clear();
        }
        rewriteRequired  = false;
        writeDone = false;
        localTID = null;

        byteOutput.reset();

	// cache the coordinator log in the coordinator log pool
	CoordinatorLogPool.putCoordinatorLog(this, logPath);

    }

    /**Creates and initialises a new CoordinatorLog object, with the given local
     * transaction identifier.
     * <p>
     * If the local transaction identifier is non-NULL,
     * the CoordinatorLog adds itself to the static list of instances.
     *
     * @param localTID  The local transaction identifier.
     *
     * @return
     *
     * @see
     */
    CoordinatorLog( Long localTID ) {

        // Set up the local transaction identifier; if it is not NULL, inform the
        // metaclass of the object's existence.

        this.localTID = localTID;
        if( localTID.longValue() != 0 )
            addLog(localTID,this);

    }

    /**
     * Creates a subsection in the CoordinatorLog in which to store related
     * objects and data.
     * <p>
     * The object that is returned is used to identify the section on subsequent
     * calls.
     * <p>
     * If the section has already been created, the object for the existing
     * section is returned.
     *
     * @param sectionName  The name of the section.
     *
     * @return  An object representing the section.
     *
     * @see
     */
    synchronized java.lang.Object createSection( String sectionName ) {

        CoordinatorLogSection result = null;

        // Check whether the given name already has a corresponding section.

        result = (CoordinatorLogSection) sectionMapping.get(sectionName);
        if (result == null) {
            // Create a new section.
            // If a section info structure cannot be allocated, return.
            // Note that the section name is added to the end of the section
            // info structure to reduce the number of SOMMalloc calls.

	    // get a new section object from the cache Arun 9/27/99
            result = SectionPool.getCoordinatorLogSection(sectionName);
            if( result == null ) {
            }

            // Copy in the name and set initial values of the other variables.

            else {

                // Add the new section information to the map.

                sectionMapping.put(sectionName,result);
            }
        }

        return result;
    }

    /**Adds the given object to the sequence of those in the given section.
     * <p>
     * The objects are stored in the order that they are added to the sequence.
     * No checking is done for duplicates.
     *
     * @param sectionObj  The object representing the section.
     * @param obj         The object to be added.
     *
     * @return  Indicates success of the operation.
     *
     * @see
     */

    synchronized boolean addObject( java.lang.Object sectionObj,
                                    org.omg.CORBA.Object obj ) {

        boolean result = true;

        // Determine if section is valid

        if( sectionObj != null ) {
            CoordinatorLogSection section = (CoordinatorLogSection)sectionObj;

            // Add header length to unwritten data length if section has currently has no
            // unwritten information.

            section.unwrittenEmpty = false;		// Arun 9/27/99

            if( section.unwrittenObjects == null )
                section.unwrittenObjects = new Vector(10,10);

            // Convert the object reference to string value

            String objRefStr = null;
            try {
                objRefStr = Configuration.getORB().object_to_string(obj);

                // Add object reference to section and update counts

                section.unwrittenObjects.addElement(objRefStr);

                //$Write logrecord if threshold is exceeded
                //$
                //$     if( unwrittenLength >= LOG_THRESHOLD )
                //$       try
                //$         { formatLogRecords(false); }
                //$       catch( IOException exc )
                //$         {
                //$         if( trc != null ) trc.error(ERR_WRITE).data(exc).write();
                //$         result = false;
                //$         }
            } catch( Throwable exc ) {
                result = false;
            }
        } else {
            result = false;
        }

        return result;
    }

    /**Adds the given opaque data structure to the sequence of those in the
     * given section.
     * <p>
     * The data structures are stored in the order that they are added to the
     * sequence. No checking is done for duplicates.
     *
     * @param sectionObj  The object representing the section.
     * @param data        The data to be added.
     *
     * @return  Indicates success of the operation.
     *
     * @see
     */

    synchronized boolean addData( java.lang.Object sectionObj,
                                  byte[] data ) {

        boolean result = true;
        byte[] dataCopy;

        // Determine if section is valid

        if( sectionObj != null ) {
            CoordinatorLogSection section = (CoordinatorLogSection)sectionObj;

            // Add header length to unwritten data length if section has currently has no
            // unwritten information.

            section.unwrittenEmpty = false;		// Arun 9/27/99

            if( section.unwrittenData == null )
                section.unwrittenData = new Vector(4,4);

            // Make a copy of the data to add to the unwritten data queue.

            dataCopy = new byte[data.length];
            System.arraycopy(data,0,dataCopy,0,data.length);

            // Add data item (sequence of octets) to section and update counts

            section.unwrittenData.addElement(dataCopy);

            //$Write logrecord if threshold is exceeded

            //$   if( unwrittenLength >= LOG_THRESHOLD )
            //$     try
            //$       { formatLogRecords(false); }
            //$     catch( IOException exc )
            //$       {
            //$       if( trc != null ) trc.error(ERR_WRITE).data(exc).write();
            //$       result = false;
            //$       }
        } else {
            result = false;
        }

        return result;
    }

    /**Write the contents of the CoordinatorLog to persistent storage.
     * <p>
     * If the force parameter is set, this requires that all information defined
     * to the CoordinatorLog that has not already been written be recorded before
     * the operation returns.
     * <p>
     * If rewrite is required, all information whether previously written or not
     * is recorded.
     * <p>
     * The log record should include the LSN of the previous record
     * written for the same transaction, if any, otherwise it is NULL. Further
     * information may be added to the CoordinatorLog after it has been forced,
     * and will be separately written in a subsequent log record, whose LSN will
     * point to the current one.
     * <p>
     * This operation discharges the CoordinatorLog's requirement to rewrite. The
     * keypoint lock must be obtained from the metaclass before checking whether
     * a rewrite is required, and released after the write is complete.
     *
     * @param force  Indicates whether the log data should be forced before this
     *               method returns.
     *
     * @return  Indicates success of the operation.
     *
     * @see
     */
    boolean write( boolean force ) {

        // Format the log records with a forced write.

        boolean result = true;
        try {
            result = formatLogRecords(force);
        } catch( IOException exc ) {
            result = false;
        }

        return result;
    }

    /**Informs the CoordinatorLog object that it must rewrite its entire state
     * the next time it writes a log record.
     * <p>
     * If the CoordinatorLog has state that has previously been written, it records
     * the requirement to rewrite, otherwise it does not record the requirement.
     *
     * @param
     *
     * @return  Indicates success of the operation.
     *
     * @see
     */
    private synchronized boolean requireRewrite() {
        boolean result = true;

        // Record the fact that a rewrite is required if a write has been done.

        if( writeDone )
            rewriteRequired = true;

        return result;
    }

    /**Rewrites the contents of the CoordinatorLog to persistent storage.
     * <p>
     * This requires that all information defined to the CoordinatorLog that has
     * already been written be re-written (unforced) to the log.
     * <p>
     * The CoordinatorLog also writes any unwritten state at this point.
     * <p>
     * The log record will contain a NULL LSN to indicate that no previous records
     * for this transaction should be used for recovery. If no state has previously
     * been written, the CoordinatorLog does nothing at this point and waits for
     * a subsequent force operation.
     * <p>
     * This operation discharges the CoordinatorLog's requirement to rewrite.
     *
     * @param
     *
     * @return  Indicates success of the operation.
     *
     * @see
     */

    private boolean rewrite() {

        boolean result = true;

        // If a rewrite is required, format a log record with all the CoordinatorLog
        // data, with a non-forced write.

        if( rewriteRequired )
            try {
                result = formatLogRecords(false);
            } catch( IOException exc ) {
                result = false;
            }

        return result;
    }

    /**
     * Requests that the object reconstructs its state from the given stream.
     * <p>
     * There may be more than one if the CoordinatorLog elects to write to the
     * log before it is asked to force the transaction state.
     * <p>
     * This operation is invoked when there are log records that need to
     * be recovered. The CoordinatorLog should reconstruct the sequences of
     * objects and data from each of the sections so that they can be queried by
     * the callers that set them up.
     *
     * @param data  The data to be used to create the CoordinatorLog object.
     *
     * @return  Indicates success of the operation.
     *
     * @see
     */
    private boolean reconstruct( DataInputStream dataInput )
        throws IOException {

        boolean result = true;

        // Read in the number of sections.

        int numSections = dataInput.readUnsignedShort();


        // Reconstruct each of the sections in the log record

        while( --numSections >= 0 ) {

            // Get the section name, number of objects and number of data items from the
            // log record passed in.

            int length = dataInput.readUnsignedShort();

            // If the section name length is zero, then it contains no data, so skip it.

            if( length > 0 ) {

                int numObjects = dataInput.readUnsignedShort();
                int numData = dataInput.readUnsignedShort();

                // Make a copy of the section name.

                byte[] stringData = new byte[length];
                dataInput.read(stringData);
                String sectionName = new String(stringData);

                // Create a section in the CoordinatorLog

                CoordinatorLogSection section = (CoordinatorLogSection) createSection(sectionName);

                // Add each object reference from the log record to the section.

                // BUGFIX(Ram J) added (writtenObject == null) check, so that
                // the previously collected objects are not discarded.
                if (numObjects > 0 && section.writtenObjects == null) {
                    section.writtenObjects = new Vector(numObjects, 10);
                }

                for( int i = 0; i < numObjects; i++ ) {

                    // Get the size of the object reference and allocate a buffer to make a copy
                    // of it.

                    length = dataInput.readUnsignedShort();
                    stringData = new byte[length];
                    dataInput.read(stringData);
                    String objRefStr = new String(stringData);

                    // Add the object reference to the list of written objects.

                    section.writtenObjects.addElement(objRefStr);
                }

                // Add each data item from the log record to the section.

                // BUGFIX(Ram J) added (writtenData == null) check, so that
                // the previously collected data are not discarded.
                if (numData > 0 && section.writtenData == null) {
                    section.writtenData = new Vector(numData, 4);
                }

                for( int i = 0; i < numData; i++ ) {

                    // Get the size of the data item and allocate a buffer to make a copy of it.

                    length = dataInput.readUnsignedShort();
                    byte[] dataItem = new byte[length];

                    // Copy the data item into the storage allocated, and add that to the list
                    // of written data items.

                    dataInput.read(dataItem);
                    section.writtenData.addElement(dataItem);
                }
            }
        }

        return result;
    }

    /**Returns a sequence containing all of the objects in the given section.
     *
     * @param sectionObj  The object representing the section.
     *
     * @return  The objects.
     *
     * @see
     */
    java.lang.Object[] getObjects( java.lang.Object sectionObj ) {
        java.lang.Object[] result = null;

        // Check that the section identifier is valid.
        // Browse through the Queue of stringified object references, converting each
        // to an actual object reference and adding it to the sequence returned from
        // this method.

        if( sectionObj != null ) {

            CoordinatorLogSection section = (CoordinatorLogSection)sectionObj;

            int unwrittenSize = 0;
            if( section.unwrittenObjects != null )
                unwrittenSize = section.unwrittenObjects.size();

            int writtenSize = 0;
            if( section.writtenObjects != null )
                writtenSize = section.writtenObjects.size();

            result = new java.lang.Object[unwrittenSize + writtenSize];
            int currObject = 0;

            // Obtain the reference of the ORB.

            ORB orb = Configuration.getORB();

            // Go through the written objects.

            for( int i = 0; i < writtenSize; i++ ) {

                org.omg.CORBA.Object obj = null;
                String refStr = (String)section.writtenObjects.elementAt(i);

                // Try ten times to convert the reference to a string.

                int retries = STRING_TO_REF_RETRIES;
                boolean discard = false;
                while( obj == null && retries-- > 0 && !discard ) {
                    try {
                        obj = orb.string_to_object(refStr);
                    } catch( MARSHAL exc ) {
                        // The MARSHAL exception indicates that the ImplHelper for the object has not been
                        // started, so try again after two seconds.

                        try {
                            Thread.sleep(2000);
                        } catch( InterruptedException ex2 ) {
							_logger.log(Level.WARNING,
									"jts.wait_for_resync_complete_interrupted");
							String msg = LogFormatter.getLocalizedMessage(_logger,
						    			"jts.wait_for_resync_complete_interrupted");
							throw  new org.omg.CORBA.INTERNAL(msg);
                        }
                    } catch( Throwable exc ) {
                        // Any other exception indicates that the reference is invalid, so just discard it.

                        discard = true;
                    }
                }

                // Add the valid object to the list.

                if( !discard ){
                    if( obj != null ){
                        result[currObject++] = obj;
					}
					else {
						_logger.log(Level.SEVERE,
						"jts.unable_to_convert_object_reference_to_string_in_recovery");
						 
						  String msg = LogFormatter.getLocalizedMessage(_logger,
			  			"jts.unable_to_convert_object_reference_to_string_in_recovery");
						
						throw  new org.omg.CORBA.INTERNAL(msg);
					}
				}
            }

            // Now get the unwritten objects.  We do not need to do all the above error
            // checking as these objects have not been recovered from the log.

            for( int i = 0; i < unwrittenSize; i++ ) {
                try {

                    // Add the valid object to the list.

                    org.omg.CORBA.Object obj = orb.string_to_object((String)section.unwrittenObjects.elementAt(i));
                    result[currObject++] = obj;
                } catch( Throwable exc ) {
                    // If the object resulting from the string is invalid, then don't add it to
                    // the list.
                }
            }
        }

        return result;
    }

    /**Returns a sequence containing all of the opaque data in the given section.
     *
     * @param sectionObj  The object representing the section.
     *
     * @return  The data.
     *
     * @see
     */

    byte[][] getData( java.lang.Object sectionObj ) {

        byte[][] result = null;

        // Check that the section identifier is valid.
        // Browse through the Queues of data items, adding each to the sequence
        // returned from this method.

        if( sectionObj != null ) {
            CoordinatorLogSection section = (CoordinatorLogSection)sectionObj;

            int unwrittenSize = 0;
            if( section.unwrittenData != null )
                unwrittenSize = section.unwrittenData.size();

            int writtenSize = 0;
            if( section.writtenData != null )
                writtenSize = section.writtenData.size();

            result = new byte[unwrittenSize+writtenSize][];

            if( unwrittenSize > 0 )
                section.unwrittenData.copyInto(result);

            for( int i = 0; i < writtenSize; i++ )
                result[unwrittenSize++] = (byte[])section.writtenData.elementAt(i);
        }

        return result;
    }


    /**Sets the local identifier for the CoordinatorLog object.
     * <p>
     * If the local identifier was previously 0, the CoordinatorLog object is
     * added to the static list.
     *
     * @param localTID  The new local identifier.
     *
     * @return
     *
     * @see
     */
    synchronized void setLocalTID( Long localTID ) {

        // Check whether the local identifier is currently NULL.

        boolean addToMetaclass = (localTID.longValue() != 0 && (this.localTID == null || this.localTID.longValue() == 0));

        // Set the local identifier, and add the object to the metaclass if required.

        this.localTID = localTID;
        if( addToMetaclass )
            addLog(localTID,this);

    }

    synchronized void setLocalTID( Long localTID, String logPath ) {

        // Check whether the local identifier is currently NULL.

        boolean addToMetaclass = (localTID.longValue() != 0 && (this.localTID == null || this.localTID.longValue() == 0));

        // Set the local identifier, and add the object to the metaclass if required.

        this.localTID = localTID;
        if( addToMetaclass )
            addLog(localTID,this, logPath);

    }

    /**Formats the information in a single section of the Coordinatorlog into a
     * stream.
     * <p>
     * This internal method does not need to be synchronized.
     * If the rewrite flag is not set, only information that has not already been
     * written is formatted, otherwise all information is formatted.
     *
     * @param section     The section.
     * @param rewrite     Indicates if the record is being rewritten.
     * @param dataOutput  The stream to which to data is output.
     *
     * @return
     *
     * @exception IOException  The format failed.
     *
     * @see
     */
    private void formatSection( CoordinatorLogSection section,
                                boolean               rewrite,
                                DataOutputStream      dataOutput )
        throws IOException {
        // No formatting is done if the section is empty, and if rewrite is required,
        // the written section is also empty.
        // Note that we still need to write something out to satisfy the number of
        // sections originally written, so we write out a name length of zero.

        if( section.unwrittenEmpty &&
            (!rewrite || section.writtenEmpty) ) {
            dataOutput.writeShort(0);
            return;
        }

        // Place length of section name into buffer.

        dataOutput.writeShort(section.sectionName.length());

        // Place count of number of object references into buffer, including written
        // object references if rewrite is required.

        int unwrittenObjectsSize = 0;
        int writtenObjectsSize = 0;
        if( section.unwrittenObjects != null )
            unwrittenObjectsSize = section.unwrittenObjects.size();
        if( rewrite &&
            section.writtenObjects != null )
            writtenObjectsSize = section.writtenObjects.size();

        dataOutput.writeShort(unwrittenObjectsSize + writtenObjectsSize);

        // Place count of number of data items into buffer, including written data
        // items if rewrite is required.

        int unwrittenDataSize = 0;
        int writtenDataSize = 0;
        if( section.unwrittenData != null )
            unwrittenDataSize = section.unwrittenData.size();
        if( rewrite &&
            section.writtenData != null )
            writtenDataSize = section.writtenData.size();

        dataOutput.writeShort(unwrittenDataSize + writtenDataSize);

        // Copy the section name into the buffer.

        dataOutput.writeBytes(section.sectionName);

        // If rewrite is required, first write the already-written object references

        for( int i = 0; i < writtenObjectsSize; i++ ) {
            String objRefStr = (String)section.writtenObjects.elementAt(i);
            dataOutput.writeShort(objRefStr.length());
            dataOutput.writeBytes(objRefStr);
        }

        // Next place length of each stringified object reference and the stringified
        // object reference into the buffer. Move each from unwritten to written queue

        for( int i = 0; i < unwrittenObjectsSize; i++ ) {
            String objRefStr = (String)section.unwrittenObjects.elementAt(i);
            dataOutput.writeShort(objRefStr.length());
            dataOutput.writeBytes(objRefStr);

            if( section.writtenObjects == null )
                section.writtenObjects = new Vector(unwrittenObjectsSize,10);

            section.writtenObjects.addElement(objRefStr);
        }

        if( unwrittenObjectsSize > 0 )
            section.unwrittenObjects.removeAllElements();

        // Now we process the data items.
        // If rewrite is required, first write the already-written data items.

        for( int i = 0; i < writtenDataSize; i++ ) {
            byte[] dataItem = (byte[])section.writtenData.elementAt(i);
            dataOutput.writeShort(dataItem.length);
            dataOutput.write(dataItem);
        }

        // Next place length of each stringified object reference and the stringified
        // object reference into the buffer. Move each from unwritten to written queue

        for( int i = 0; i < unwrittenDataSize; i++ ) {
            byte[] dataItem = (byte[])section.unwrittenData.elementAt(i);
            dataOutput.writeShort(dataItem.length);
            dataOutput.write(dataItem);

            if( section.writtenData == null )
                section.writtenData = new Vector(unwrittenDataSize,4);

            section.writtenData.addElement(dataItem);
        }
        if( unwrittenDataSize > 0 )
            section.unwrittenData.removeAllElements();

        // Set unwritten_empty to TRUE and written_empty to FALSE since everything    //
        // has moved from the unwritten to the written queues.                        //

        section.unwrittenEmpty = true;
        section.writtenEmpty = false;

    }

    /**Formats the information in all sections of the CoordinatorLog.
     * <p>
     * The formatted information is written to the log.
     * <p>
     * This internal method does not need to be synchronized.
     * If the rewrite flag is not set, only information that has not already been
     * written is formatted, otherwise all information is formatted.
     *
     * @param forced  Forced/unforced write indicator.
     *
     * @return  Indicates success of the operation.
     *
     * @exception IOException  The format failed.
     *
     * @see
     */
    private boolean formatLogRecords( boolean forced )
	throws IOException {

        // If there is no LogFile for this transaction, and one cannot be obtained
        // from the metaclass, then no formatting can be done.

        if (logPath == null)
            openLog();
        else
            openLog(logPath);
        if( logStateHolder.logFile == null ) {
            return false;
        }

        // In order to check whether rewrite is required, we must first obtain the
        // keypoint lock to ensure that the metaclass is not in the process of
        // informing us that a rewrite is required.
        //$We must not wait for the keypoint lock while holding our own lock so
        //$release it now.

        boolean result = false;
        try {
            logStateHolder.keypointLock.acquireReadLock();
            // Once we have the keypoint lock, it is OK to obtain our own.

            synchronized( this ) {

                // Place the tid in the buffer.

                byteOutput.reset();
                dataOutput.writeLong(localTID.longValue());

                // Write out the number of sections.

                dataOutput.writeShort(sectionMapping.size());

                // Format log section within map and add the information to buffer. Browse
                // through the CoordinatorLog filling in the buffer for each entry.

                Enumeration sections = sectionMapping.elements();
		int sz = sectionMapping.size();		// Arun 9/27/99
		while (sz-- > 0) {			// Arun 9/27/99
                    formatSection((CoordinatorLogSection)sections.nextElement(),
						rewriteRequired,dataOutput);
		}

                // Write the buffer to the LogFile.

                result = logStateHolder.logFile.write( forced ? LogFile.FORCED : LogFile.UNFORCED,
                                        byteOutput.toByteArray(),
                                        rewriteRequired ? LogFile.REWRITE : LogFile.NORMAL,
                                        null );

                rewriteRequired = false;
                writeDone = true;
            }
        } finally {
            logStateHolder.keypointLock.releaseReadLock();
        }

        return result;
    }

    /**Provides static initialisation of the CoordinatorLog class.
     *
     * @param
     *
     * @return
     *
     * @see
     */
    static {

        // Get the value of the keypoint trigger from the environment.

        String keypointCountEnv = Configuration.getPropertyValue(Configuration.KEYPOINT_COUNT);
        keypointTrigger = LOG_DEF_KEY_TRIGGER;
        if( keypointCountEnv != null )
            try {
                keypointTrigger = Integer.parseInt(keypointCountEnv);
            } catch( Throwable e ) {}

    }

    /**Opens the log file for all CoordinatorLogs in this process.
     * <p>
     * If the log has already been opened, the operation uses the opened LogFile.
     *
     * @param
     *
     * @return  Indicates success of the operation.
     *
     * @see
     */
    private static boolean openLog() {
        boolean result = false;
        String logName;
        CoordinatorLogStateHolder logStateHolder = defaultLogStateHolder;

        // If the log has been opened, there is nothing to do.

        if( logStateHolder.log == null ) {
            logStateHolder.log = new Log();
            if( !logStateHolder.log.initialise() ) {
                logStateHolder.log = null;
				_logger.log(Level.SEVERE,"jts.cannot_initialise_log");
				String msg = LogFormatter.getLocalizedMessage(_logger,
							"jts.cannot_initialise_log");
				throw  new org.omg.CORBA.INTERNAL(msg);
            }
        }


        // Open the Log and set the logfile object reference.  If there is no
        // ImplementationDef object available, then we cannot determine the log file


        // name, so the log cannot be opened.
        // Note that this does not preclude the log file being opened at some later
        // time.

        String serverName = null;
        if( logStateHolder.log != null &&
            logStateHolder.logFile == null &&
            (serverName = Configuration.getServerName()) != null ) {

	    // get a coordinator log object from cache instead
	    // of instantiating a new one		Arun 9/27/99
            logStateHolder.logFile = logStateHolder.log.open(serverName,
			       CoordinatorLogPool.getCoordinatorLog());

            if( logStateHolder.logFile == null ) {
				_logger.log(Level.SEVERE,"jts.cannot_open_log_file",serverName);
				 String msg = LogFormatter.getLocalizedMessage(_logger,
				 						"jts.cannot_open_log_file");
				 throw  new org.omg.CORBA.INTERNAL(msg);
            } else 
                Configuration.setLogFile(logStateHolder.logFile);
        }

        result = (logStateHolder.logFile != null);

        return result;
    }

    /**Opens the log file for all CoordinatorLogs in this process.
     * <p>
     * If the log has already been opened, the operation uses the opened LogFile.
     *
     * @param
     *
     * @return  Indicates success of the operation.
     *
     * @see
     */
    private static boolean openLog(String logPath) {
        boolean result = false;
        String logName;
        CoordinatorLogStateHolder logStateHolder = getStateHolder(logPath);

        // If the log has been opened, there is nothing to do.

        if( logStateHolder.log == null ) {
            logStateHolder.log = new Log(logPath);
            if( !logStateHolder.log.initialise() ) {
                logStateHolder.log = null;
				_logger.log(Level.SEVERE,"jts.cannot_initialise_log");
				String msg = LogFormatter.getLocalizedMessage(_logger,
							"jts.cannot_initialise_log");
				throw  new org.omg.CORBA.INTERNAL(msg);
            }
        }


        // Open the Log and set the logfile object reference.  If there is no
        // ImplementationDef object available, then we cannot determine the log file


        // name, so the log cannot be opened.
        // Note that this does not preclude the log file being opened at some later
        // time.

        String serverName = null;
        // Always reopen logFile for a delegated recovery: do not check for logStateHolder.logFile == null
        if( logStateHolder.log != null &&
            (serverName = Configuration.getServerName(logPath)) != null ) {

	    // get a coordinator log object from cache instead
	    // of instantiating a new one		Arun 9/27/99
            logStateHolder.logFile = logStateHolder.log.open(serverName,
			       CoordinatorLogPool.getCoordinatorLog(logPath));

            if( logStateHolder.logFile == null ) {
				_logger.log(Level.SEVERE,"jts.cannot_open_log_file",serverName);
				 String msg = LogFormatter.getLocalizedMessage(_logger,
				 						"jts.cannot_open_log_file");
				 throw  new org.omg.CORBA.INTERNAL(msg);
            } else 
                Configuration.setLogFile(logPath,logStateHolder.logFile);
        }

        result = (logStateHolder.logFile != null);

        return result;
    }

    /**Process the log to build a sequence of CoordinatorLog objects which
     * represent all logged transactions.
     *
     * @param
     *
     * @return  The CoordinatorLog objects, or null if there are none.
     *
     * @see
     */
    synchronized static Enumeration getLogged() {

        Vector logRecords = null;
        Enumeration coordLogs = null;

        // Initialise the Log.  If the log cannot be opened, return an empty
        // sequence, with whatever exception the open returned.

        if( openLog() ) {
             CoordinatorLogStateHolder logStateHolder = defaultLogStateHolder;

            // Get the log records returned from the log and browse through them.  Take
            // Take the sequence of log records returned from the LogFile and convert
            // them into the sequence of CoordinatorLog objects that are returned from
            // this method.

            logRecords = logStateHolder.logFile.getLogRecords();
            for( int i = 0; i < logRecords.size(); i++ ) {

                // Get tid value from the log record. Get the CoordinatorLog reference if
                // it exists in map, else create a new CoordinatorLog object; it will
                // added to the map when we set the transaction id.

                byte[] buffer = (byte[])logRecords.elementAt(i);
                ByteArrayInputStream byteInput = new ByteArrayInputStream(buffer);
                DataInputStream dataInput = new DataInputStream(byteInput);

                try {
                    Long localTID = dataInput.readLong();
                    CoordinatorLog coordLog = (CoordinatorLog)logStateHolder.activeLogs.get(localTID);
                    if( coordLog == null ) {

	                // get a coordinator log object from cache instead
	                // of instantiating a new one		Arun 9/27/99
                        coordLog = CoordinatorLogPool.getCoordinatorLog();

                        coordLog.setLocalTID(localTID);
                    }

                    // Reconstruct the CoordinatorLog information from the log record.

                    coordLog.reconstruct(dataInput);
                } catch( IOException exc ) {
                }
            }

            // Return a copy of the list of active CoordinatorLog objects.

            coordLogs = logStateHolder.activeLogs.elements();
        }

        // If the log could not be opened, return an empty Enumeration.

        else
            coordLogs = new Hashtable().elements();


        return coordLogs;
    }

    /**Process the log to build a sequence of CoordinatorLog objects which
     * represent all logged transactions.
     *
     * @param
     *
     * @return  The CoordinatorLog objects, or null if there are none.
     *
     * @see
     */
    synchronized static Enumeration getLogged(String logPath) {

        Vector logRecords = null;
        Enumeration coordLogs = null;

        // Initialise the Log.  If the log cannot be opened, return an empty
        // sequence, with whatever exception the open returned.

        if( openLog(logPath) ) {
             CoordinatorLogStateHolder logStateHolder = getStateHolder(logPath);

            // Get the log records returned from the log and browse through them.  Take
            // Take the sequence of log records returned from the LogFile and convert
            // them into the sequence of CoordinatorLog objects that are returned from
            // this method.

            logRecords = logStateHolder.logFile.getLogRecords();
            for( int i = 0; i < logRecords.size(); i++ ) {

                // Get tid value from the log record. Get the CoordinatorLog reference if
                // it exists in map, else create a new CoordinatorLog object; it will
                // added to the map when we set the transaction id.

                byte[] buffer = (byte[])logRecords.elementAt(i);
                ByteArrayInputStream byteInput = new ByteArrayInputStream(buffer);
                DataInputStream dataInput = new DataInputStream(byteInput);

                try {
                    Long localTID = dataInput.readLong();
                    CoordinatorLog coordLog = (CoordinatorLog)logStateHolder.activeLogs.get(localTID);
                    if( coordLog == null ) {

	                // get a coordinator log object from cache instead
	                // of instantiating a new one		Arun 9/27/99
                        coordLog = CoordinatorLogPool.getCoordinatorLog(logPath);

                        coordLog.setLocalTID(localTID, logPath);
                    }

                    // Reconstruct the CoordinatorLog information from the log record.

                    coordLog.reconstruct(dataInput);
                } catch( IOException exc ) {
                }
            }

            // Return a copy of the list of active CoordinatorLog objects.

            coordLogs = logStateHolder.activeLogs.elements();
        }

        // If the log could not be opened, return an empty Enumeration.

        else
            coordLogs = new Hashtable().elements();


        return coordLogs;
    }


    /**Remembers the mapping between the local transaction identifier and the
     * CoordinatorLog object.
     *
     * @param localTID  The local transaction identifier.
     * @param clog      The CoordinatorLog object.
     *
     * @return  Indicates success of the operation.
     *
     * @see
     */
    private static boolean addLog(Long localTID,
                                               CoordinatorLog clog ) {
       CoordinatorLogStateHolder logStateHolder = defaultLogStateHolder;

        boolean result = true;

        logStateHolder.activeLogs.put(localTID,clog);

        return result;
    }

    private static boolean addLog(Long localTID,
                                               CoordinatorLog clog, String logPath ) {
       CoordinatorLogStateHolder logStateHolder = getStateHolder(logPath);

        boolean result = true;

        logStateHolder.activeLogs.put(localTID,clog);

        return result;
    }

    /**Removes the CoordinatorLog object from the map, and destroys it.
     *
     * @param localTID  The local transaction identifier.
     *
     * @return  Indicates success of the operation.
     *
     * @see
     */
    synchronized static boolean removeLog( Long localTID ) {

        boolean result = true;
        CoordinatorLogStateHolder logStateHolder = defaultLogStateHolder;

        // Remove the given CoordinatorLog and local identifier from the map.
        // If the CoordinatorLog could be removed, we need to check whether a
        // keypoint is in progress, and if so, prevent the CoordinatorLog from being
        // called during the keypoint.

        CoordinatorLog clog = (CoordinatorLog)logStateHolder.activeLogs.remove(localTID);
        if( clog != null ) {

            // Obtaining the keypoint state lock prevents us from doing this while the
            // keypoint method is using the map.

            synchronized( logStateHolder.keypointStateLock ) {
                // If a keypoint is in progress, look up the entry for the transaction in the
                // map and replace the value with a NULL entry.

                if( logStateHolder.keypointInProgress && logStateHolder.keypointLogs != null )
                    logStateHolder.keypointLogs.put(localTID,NULL_ENTRY);
            }

            // If the transaction is read-only, then do not increment the transaction count.

            if( clog.writeDone )
                logStateHolder.tranCount++;

            // return the CoordinatorLog object to the pool to be reused.
	    //					Arun 9/27/99
            clog.reUse();


            // Check whether a keypoint is required.  This is based solely on the number
            // of (non-readonly) transactions since the last keypoint.

            if( logStateHolder.tranCount >= keypointTrigger ) {
                logStateHolder.tranCount = 0;
                keypoint();
            }
        }


        return result;
    }

    synchronized static boolean removeLog( Long localTID , String logPath) {

        boolean result = true;
        CoordinatorLogStateHolder logStateHolder = getStateHolder(logPath);

        // Remove the given CoordinatorLog and local identifier from the map.
        // If the CoordinatorLog could be removed, we need to check whether a
        // keypoint is in progress, and if so, prevent the CoordinatorLog from being
        // called during the keypoint.

        CoordinatorLog clog = (CoordinatorLog)logStateHolder.activeLogs.remove(localTID);
        if( clog != null ) {

            // Obtaining the keypoint state lock prevents us from doing this while the
            // keypoint method is using the map.

            synchronized( logStateHolder.keypointStateLock ) {
                // If a keypoint is in progress, look up the entry for the transaction in the
                // map and replace the value with a NULL entry.

                if( logStateHolder.keypointInProgress && logStateHolder.keypointLogs != null )
                    logStateHolder.keypointLogs.put(localTID,NULL_ENTRY);
            }

            // If the transaction is read-only, then do not increment the transaction count.

            if( clog.writeDone )
                logStateHolder.tranCount++;

            // return the CoordinatorLog object to the pool to be reused.
	    //					Arun 9/27/99
            clog.reUse(logPath);


            // Check whether a keypoint is required.  This is based solely on the number
            // of (non-readonly) transactions since the last keypoint.

            if( logStateHolder.tranCount >= keypointTrigger ) {
                logStateHolder.tranCount = 0;
                keypoint(logPath);
            }
        }


        return result;
     }

    /**Performs a keypoint operation to allow old information in the log to be
     * discarded.
     * <p>
     * This operation is not synchronized as we do not want the latter part of the
     * operation to block other logging operations.  The start of the keypoint is
     * in a separate method which is synchronized.
     *
     * @param
     *
     * @return
     *
     * @see
     */
    static void keypoint() {
        CoordinatorLogStateHolder logStateHolder = defaultLogStateHolder;

        byte[] keypointEndRecord = {
            (byte) 'K',
            (byte) 'E',
            (byte) 'Y',
            (byte) 'E',
            (byte) 'N',
            (byte) 'D'};

        LogLSN previousLSN = new LogLSN();
        LogLSN keypointStartLSN = new LogLSN();
        boolean keypointRequired = false;

        // Obtain the global keypoint lock to prevent any activity until the keypoint
        // start has been recorded.
        // Once the keypoint start has been completed, we can release the
        // keypoint lock.  This will allow waiting CoordinatorLog writes to complete.

        try {
            logStateHolder.keypointLock.acquireWriteLock();
            keypointRequired = startKeypoint(keypointStartLSN);
        } finally {
            logStateHolder.keypointLock.releaseWriteLock();
        }

        // If no keypoint start record was written, then just return.

        if( keypointStartLSN.isNULL() ) {
            return;
        }

        // Once all of the CoordinatorLog objects have been unlocked, we must make
        // sure each of them has been rewritten before the keypoint end record is
        // written.  Note that it is possible that one or more of the CoordinatorLog
        // objects in this list has already been deleted.  We must be careful
        // to make sure that we do not invoke a method on a deleted object.

        if( keypointRequired ) {
            Enumeration keypointLocalTIDs = logStateHolder.keypointLogs.keys();
            while( keypointLocalTIDs.hasMoreElements() )

                // Obtain the keypoint state lock before obtaining the value from the map, as the
                // remove operation might be changing the value to NULL.  Note that the
                // remove operation only changes the value of an entry in this map, it does
                // not change the number of entries in the map, so we do not need to hold the
                // mutex for the browse.

                synchronized( logStateHolder.keypointStateLock ) {
                    CoordinatorLog currentLog = (CoordinatorLog)logStateHolder.keypointLogs.get(keypointLocalTIDs.nextElement());

                    // Get the value out of the map, and if not NULL entry, tell it to rewrite itself.

                    if( currentLog != NULL_ENTRY )
                        currentLog.rewrite();
                }
        }

        // Now we know all CoordinatorLog objects have either independently rewritten
        // themselves, or we have done it explicitly.  A keypoint end record is
        // written to indicate that the keypoint is complete.

        logStateHolder.logFile.write(LogFile.UNFORCED,
                      keypointEndRecord,
                      LogFile.KEYPOINT_END,
                      previousLSN);

        // All that is left to do is to inform the LogFile that the records before
        // the keypoint start record are no longer required.
        // Checkpoint the log.  This allows the log to discard previous entries that
        // are no longer required.

        logStateHolder.logFile.checkpoint(keypointStartLSN);

        // Clear the keypoint in progress flag, empty the map of CoordinatorLog
        // objects being keypointed, release muteces and return.

        logStateHolder.keypointInProgress = false;
        logStateHolder.keypointLogs.clear();

    }
    static void keypoint(String logPath) {
        CoordinatorLogStateHolder logStateHolder = getStateHolder(logPath);

        byte[] keypointEndRecord = {
            (byte) 'K',
            (byte) 'E',
            (byte) 'Y',
            (byte) 'E',
            (byte) 'N',
            (byte) 'D'};

        LogLSN previousLSN = new LogLSN();
        LogLSN keypointStartLSN = new LogLSN();
        boolean keypointRequired = false;

        // Obtain the global keypoint lock to prevent any activity until the keypoint
        // start has been recorded.
        // Once the keypoint start has been completed, we can release the
        // keypoint lock.  This will allow waiting CoordinatorLog writes to complete.

        try {
            logStateHolder.keypointLock.acquireWriteLock();
            keypointRequired = startKeypoint(keypointStartLSN, logPath);
        } finally {
           logStateHolder.keypointLock.releaseWriteLock();
        }

        // If no keypoint start record was written, then just return.

        if( keypointStartLSN.isNULL() ) {
            return;
        }

        // Once all of the CoordinatorLog objects have been unlocked, we must make
        // sure each of them has been rewritten before the keypoint end record is
        // written.  Note that it is possible that one or more of the CoordinatorLog
        // objects in this list has already been deleted.  We must be careful
        // to make sure that we do not invoke a method on a deleted object.

        if( keypointRequired ) {
            Enumeration keypointLocalTIDs = logStateHolder.keypointLogs.keys();
            while( keypointLocalTIDs.hasMoreElements() )

                // Obtain the keypoint state lock before obtaining the value from the map, as the
                // remove operation might be changing the value to NULL.  Note that the
                // remove operation only changes the value of an entry in this map, it does
                // not change the number of entries in the map, so we do not need to hold the
                // mutex for the browse.

                synchronized( logStateHolder.keypointStateLock ) {
                    CoordinatorLog currentLog = (CoordinatorLog)logStateHolder.keypointLogs.get(keypointLocalTIDs.nextElement());

                    // Get the value out of the map, and if not NULL entry, tell it to rewrite itself.

                    if( currentLog != NULL_ENTRY )
                        currentLog.rewrite();
                }
        }

        // Now we know all CoordinatorLog objects have either independently rewritten
        // themselves, or we have done it explicitly.  A keypoint end record is
        // written to indicate that the keypoint is complete.

        logStateHolder.logFile.write(LogFile.UNFORCED,
                      keypointEndRecord,
                      LogFile.KEYPOINT_END,
                      previousLSN);

        // All that is left to do is to inform the LogFile that the records before
        // the keypoint start record are no longer required.
        // Checkpoint the log.  This allows the log to discard previous entries that
        // are no longer required.

        logStateHolder.logFile.checkpoint(keypointStartLSN);

        // Clear the keypoint in progress flag, empty the map of CoordinatorLog
        // objects being keypointed, release muteces and return.

        logStateHolder.keypointInProgress = false;
        logStateHolder.keypointLogs.clear();

    }

    /**Handles a short-on-storage situation in the log by taking a keypoint.

    /**Handles a short-on-storage situation in the log by taking a keypoint.
     *
     * @param reason  The reason for the upcall.
     *
     * @return
     *
     * @see
     */

    public void upcall( int reason ){

        // Just perform a keypoint.
       if (logPath == null)
           CoordinatorLog.keypoint();
       else
           CoordinatorLog.keypoint(logPath);

    }

    /**Destroys the state of the CoordinatorLog class.
     *
     * @param
     *
     * @return
     *
     * @see
     */

    synchronized static void finalizeAll(){
       CoordinatorLogStateHolder logStateHolder = defaultLogStateHolder;

        boolean deleteFile = false;

        // Obtain the keypoint state lock for this operation.

        synchronized( logStateHolder.keypointStateLock ) {

            // Close the LogFile.

            if( logStateHolder.activeLogs != null ) {

                // If there are no active log records sete delete_file to TRUE so that
                // LogFile_close will cause the logfile to be deleted

                if( logStateHolder.activeLogs.size() == 0 )
                    deleteFile = true;
                logStateHolder.activeLogs.clear();
                logStateHolder.activeLogs = null;
            }

            if( logStateHolder.logFile != null ) logStateHolder.logFile.close(deleteFile);
            logStateHolder.logFile = null;

            // Discard the CoordinatorLog mappings.

            if( logStateHolder.keypointLogs != null )
                logStateHolder.keypointLogs.clear();
            logStateHolder.keypointLogs = null;
        }

        // Discard the locks.

        logStateHolder.keypointStateLock = null;
        logStateHolder.keypointLock = null;

    }

    synchronized static void finalizeAll(String logPath){
       CoordinatorLogStateHolder logStateHolder = getStateHolder(logPath);

        boolean deleteFile = false;

        // Obtain the keypoint state lock for this operation.

        synchronized( logStateHolder.keypointStateLock ) {

            // Close the LogFile.

            if( logStateHolder.activeLogs != null ) {

                // If there are no active log records sete delete_file to TRUE so that
                // LogFile_close will cause the logfile to be deleted

                if( logStateHolder.activeLogs.size() == 0 )
                    deleteFile = true;
                logStateHolder.activeLogs.clear();
                logStateHolder.activeLogs = null;
            }

            if( logStateHolder.logFile != null ) logStateHolder.logFile.close(deleteFile);
            logStateHolder.logFile = null;

            // Discard the CoordinatorLog mappings.

            if( logStateHolder.keypointLogs != null )
                logStateHolder.keypointLogs.clear();
            logStateHolder.keypointLogs = null;
        }

        // Discard the locks.

        logStateHolder.keypointStateLock = null;
        logStateHolder.keypointLock = null;

    }

    /**Starts a keypoint.
     *
     * @param keypointStartLSN  The LSN to hold the keypoint start LSN.
     *
     * @return  Indicates whether keypoint is required.
     *
     * @see
     */

    synchronized static boolean startKeypoint( LogLSN keypointStartLSN ) {
        CoordinatorLogStateHolder logStateHolder = defaultLogStateHolder;

        boolean keypointRequired = false;

        // If a keypoint is in progress, return and do nothing.

        if( logStateHolder.keypointInProgress ) {
            return false;
        }
        logStateHolder.keypointInProgress = true;

        // Initialise the Log.  If this fails, then return whatever exception the
        // open raised.

        if( !openLog() ) {
            logStateHolder.keypointInProgress = false;
            return false;
        }

        // If there are no known CoordinatorLog objects, then all that the keypoint
        // operation does is checkpoint the log at the head.

        if( logStateHolder.activeLogs.size() == 0 )
            keypointRequired = false;

        // Else go round all currently known CoordinatorLog objects and build a list
        // of them.  New CoordinatorLog objects that are created during this time
        // will be suspended when they try to do an CoordinatorLog.addLog operation as
        // this thread has the lock.

        else {

            // Go through all current CoordinatorLog objects, telling them that they
            // must rewrite their state if necessary.
            // Each CoordinatorLog that exists at this time is copied to a separate list.

            Enumeration clogs = logStateHolder.activeLogs.elements();
            while( clogs.hasMoreElements() ) {
                CoordinatorLog currentLog = (CoordinatorLog)clogs.nextElement();
                Long localTID = currentLog.localTID;

                currentLog.requireRewrite();
                logStateHolder.keypointLogs.put(localTID,currentLog);
            }
            keypointRequired = logStateHolder.keypointLogs.size() > 0;
        }

        // Write a keypoint start record now that we know no logging activity is
        // taking place.

        byte[] keypointStartRecord = {(byte) 'K',
                                      (byte) 'E',
                                      (byte) 'Y',
                                      (byte) 'S',
                                      (byte) 'T',
                                      (byte) 'A',
                                      (byte) 'R',
                                      (byte) 'T'};
        logStateHolder.logFile.write(LogFile.UNFORCED,
                      keypointStartRecord,
                      LogFile.KEYPOINT_START,
                      keypointStartLSN);

        return keypointRequired;
    }

    synchronized static boolean startKeypoint( LogLSN keypointStartLSN, String logPath ) {
        CoordinatorLogStateHolder logStateHolder = getStateHolder(logPath);

        boolean keypointRequired = false;

        // If a keypoint is in progress, return and do nothing.

        if( logStateHolder.keypointInProgress ) {
            return false;
        }
        logStateHolder.keypointInProgress = true;

        // Initialise the Log.  If this fails, then return whatever exception the
        // open raised.

        if( !openLog(logPath) ) {
            logStateHolder.keypointInProgress = false;
            return false;
        }

        // If there are no known CoordinatorLog objects, then all that the keypoint
        // operation does is checkpoint the log at the head.

        if( logStateHolder.activeLogs.size() == 0 )
            keypointRequired = false;

        // Else go round all currently known CoordinatorLog objects and build a list
        // of them.  New CoordinatorLog objects that are created during this time
        // will be suspended when they try to do an CoordinatorLog.addLog operation as
        // this thread has the lock.

        else {

            // Go through all current CoordinatorLog objects, telling them that they
            // must rewrite their state if necessary.
            // Each CoordinatorLog that exists at this time is copied to a separate list.

            Enumeration clogs = logStateHolder.activeLogs.elements();
            while( clogs.hasMoreElements() ) {
                CoordinatorLog currentLog = (CoordinatorLog)clogs.nextElement();
                Long localTID = currentLog.localTID;

                currentLog.requireRewrite();
                logStateHolder.keypointLogs.put(localTID,currentLog);
            }
            keypointRequired = logStateHolder.keypointLogs.size() > 0;
        }

        // Write a keypoint start record now that we know no logging activity is
        // taking place.

        byte[] keypointStartRecord = {(byte) 'K',
                                      (byte) 'E',
                                      (byte) 'Y',
                                      (byte) 'S',
                                      (byte) 'T',
                                      (byte) 'A',
                                      (byte) 'R',
                                      (byte) 'T'};
        logStateHolder.logFile.write(LogFile.UNFORCED,
                      keypointStartRecord,
                      LogFile.KEYPOINT_START,
                      keypointStartLSN);

        return keypointRequired;
    }

    /**Dumps the state of the class.
     *
     * @param
     *
     * @return
     *
     * @see
     */

    static void dumpClass() {
        // Dump the contained objects.
        CoordinatorLogStateHolder logStateHolder = defaultLogStateHolder;

        logStateHolder.log.dump();
        logStateHolder.logFile.dump();
    }

    // START IASRI 4662745
    public static void setKeypointTrigger(int keypoint)
    {
        keypointTrigger = keypoint;
    }
    // END IASRI 4662745

}

/**The CoordinatorLogSection class stores information relevant to a section.
 *
 * @version 0.1
 *
 * @author Simon Holdsworth, IBM Corporation
 *
 * @see
*/
// CHANGE HISTORY
//
// Version By     Change Description
//   0.1   SAJH   Initial implementation.
//------------------------------------------------------------------------------

class CoordinatorLogSection extends java.lang.Object {
    String  sectionName      = null;
    boolean unwrittenEmpty   = true;
    boolean writtenEmpty     = true;
    Vector  unwrittenObjects = null;
    Vector  unwrittenData    = null;
    Vector  writtenObjects   = null;
    Vector  writtenData      = null;

    /**Creates a CoordinatorLogSection with the given name.
     *
     * @param sectionName  The name of the section.
     *
     * @return
     *
     * @see
     */
    CoordinatorLogSection( String sectionName ) {
        this.sectionName = sectionName;
    }

    /**Destroys the contents of a CoordinatorLogSection.
     *
     * @param
     *
     * @return
     *
     * @see
     */
    public void doFinalize() {
        if( unwrittenObjects != null )
            unwrittenObjects.removeAllElements();

        if( unwrittenData != null )
            unwrittenData.removeAllElements();

        if( writtenObjects != null )
            writtenObjects.removeAllElements();

        if( writtenData != null )
            writtenData.removeAllElements();

        sectionName = null;

        unwrittenObjects = null;
        unwrittenData    = null;
        writtenObjects   = null;
        writtenData      = null;
    }

    /**Cleans up the CoordinatorLogSection and
     * returns it to the pool for re-use
     *
     * Note: the implementation of the cache does not ensure
     *       that when an object is re-used there are no
     *	     outstanding references to that object. However, the
     *	     risk involved is minimal since reUse() replaces the
     *	     existing call to finalize(). The existing call to
     *	     finalize also does not ensure that there are no
     *	     outstanding references to the object being finalized.
     *
     * @param
     *
     * @return
     *
     * @see
     */
    synchronized void reUse() {			// Arun 9/27/99

        if( unwrittenObjects != null )
            unwrittenObjects.removeAllElements();

        if( unwrittenData != null )
            unwrittenData.removeAllElements();

        if( writtenObjects != null )
            writtenObjects.removeAllElements();

        if( writtenData != null )
            writtenData.removeAllElements();

        sectionName = null;
        unwrittenEmpty   = true;
        writtenEmpty     = true;

	SectionPool.putCoordinatorLogSection(this);
    }
}

/**
 * The SectionPool is used as a cache for CoordinatorLogSection objects.
 * This pool allows the re-use of these objects which are very expensive
 * to instantiate.
 *
 * The pool was added to improve performance of trnasaction logging
 *
 * @version 0.01
 *
 * @author Arun Krishnan
 *
 * @see
*/

class SectionPool {

    private Stack pool;
    private static final int MAXSTACKSIZE = 15;

    static SectionPool SPool = new SectionPool();


    public SectionPool() {
	pool = new Stack();
    }

    /**
     * Fetch a CoordinatorLogSection object from the cache. This method
     * should be called instead of "new CoordinatorLogSection()". If the
     * cache is empty this method will instantiate a new
     * CoordinatorLogSection object.
     *
     * @param String name the section name
     *
     * @return CoordinatorLogSection
     *
     */
    public static synchronized
	   CoordinatorLogSection getCoordinatorLogSection(String name) {

	CoordinatorLogSection cls;
	if (SPool.pool.empty()) {
	    return new CoordinatorLogSection(name);
	}
	else {
	    cls = (CoordinatorLogSection) SPool.pool.pop();
	    cls.sectionName = name;
	    return cls;
	}
    }

    /**
     * Return a CoordinatorLogSection object to cache. Cache size is
     * limited to MAXSTACKSIZE by discarding objects when the cache
     * is already at max size.
     *
     * @param CoordinatorLogSection object to be returned to cache
     *
     */
    public static void putCoordinatorLogSection(CoordinatorLogSection cls) {
	if (SPool.pool.size() <= MAXSTACKSIZE) {
	    SPool.pool.push(cls);
	}
    }

}

class CoordinatorLogStateHolder {
   LogFile logFile        = null;
   Log log                = null;
   Hashtable activeLogs   = null;
   Hashtable keypointLogs = null;
   int tranCount          = 0;
   boolean keypointInProgress = false;
   // java.lang.Object keypointLock = new java.lang.Object();
   RWLock keypointLock = null;
   java.lang.Object keypointStateLock = null;
}

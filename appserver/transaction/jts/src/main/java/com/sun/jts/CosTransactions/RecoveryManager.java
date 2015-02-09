/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2015 Oracle and/or its affiliates. All rights reserved.
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
// Module:      RecoveryManager.java
//
// Description: Process transaction management.
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
import java.io.*;
import java.util.concurrent.*;

import org.omg.CORBA.*;
import org.omg.CosTransactions.*;

import com.sun.jts.jtsxa.*;
import com.sun.jts.codegen.jtsxa.*;

import javax.transaction.xa.*;
import com.sun.jts.jta.TransactionManagerImpl;

import com.sun.jts.trace.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import com.sun.logging.LogDomains;
import com.sun.jts.utils.LogFormatter;
import com.sun.enterprise.transaction.jts.api.TransactionRecoveryFence;
/**
 * This class manages information required for recovery, and also general
 * state regarding transactions in a process.
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

public class RecoveryManager {

    /**
     * list of XA Resources to be recovered.
     */
    private static Enumeration uniqueRMSet = null;

    /**
     * This attribute indicates whether initialisation has been started.
     */
    private static boolean initialised = false;

    /**
     * This attribute indicates the number of Coordinator objects which require
     * resync.  This is set to the number of in-doubt transactions recovered
     * from the log, then decreased as transactions are resolved.
     */
    private static int resyncCoords = 0;

    /**
     * This attribute records the thread which is used to perform resync during
     * restart
     */
    private static ResyncThread resyncThread = null;

    /**
     * This attribute is used to block new requests while there are
     * Coordinators which still require resync.
     */
    private static volatile EventSemaphore resyncInProgress = new EventSemaphore();

    /**
     * This attribute is used to block requests against RecoveryCoordinators or
     * CoordinatorResources before recovery has completed.
     */
    private static volatile EventSemaphore recoveryInProgress = new EventSemaphore();

    /**
     * This attribute is used by the Recovery Thread to know if the
     * xaResource list is ready in case manual recovery is attempted.
     */
    private static volatile EventSemaphore uniqueRMSetReady = new EventSemaphore();

    private static Hashtable coordsByGlobalTID = new Hashtable();
    private static Hashtable coordsByLocalTID = new Hashtable();

    /**
     * Mapping between transactionIds and threads. This is used to ensure
     * there is at most one thread doing work in a transaction.
     */
    private static Hashtable transactionIds = new Hashtable();

    /**
     * Mapping between incompleteTxIds and their commit decisions.
     */
    private static Hashtable inCompleteTxMap = new Hashtable();

    // This will start TransactionRecoveryFence service as soon as all resources are available.
    private static TransactionRecoveryFence txRecoveryFence = new TransactionRecoveryFenceSimple();

    
    
    /**
     * This is intented to be used as a lock object.
     */
    private static java.lang.Object lockObject = new java.lang.Object();
	/*
		Logger to log transaction messages
	*/  
	    static Logger _logger = LogDomains.getLogger(RecoveryManager.class, LogDomains.TRANSACTION_LOGGER);
    /**
     * Initialises the static state of the RecoveryManager class.
     *
     * @param
     *
     * @return
     *
     * @see
     */
    static void initialise() {

        // If already initialised, return immediately.

        if (initialised) {
            return;
        }

        initialised = true;

        // Perform recovery/resync if necessary.

        if (Configuration.isRecoverable()) {
            resyncThread = new ResyncThread();
	    	if(_logger.isLoggable(Level.FINE))
	    	{
				_logger.logp(Level.FINE,"RecoveryManager","initialise()",
						"Before starting ResyncThread ");
	    	}
            //resyncThread.start();
        } else {

            // If the process is non-recoverable, but there is a valid server
            // name,then check for a log file and issue a warning message
            // if one exists.  Also ensure that restart required is set to no.

            if (!Configuration.isAppClientContainer())  {
                String serverName = Configuration.getServerName();
                if (serverName != null && Log.checkFileExists(serverName)) {
					_logger.log(Level.INFO,"jts.log_file_transient_server",serverName);

                }
            }

            // Modify the restart requirement in the repository, and
            // post the event semaphore as there will be no Coordinators
            // requiring resync.

            try {
                recoveryInProgress.post(); // BUGFIX (Ram Jeyaraman)
                resyncComplete(false, false);
            } catch (Throwable exc) {exc.printStackTrace();}
        }
    }

    /**
     * Sets up the local and global identifier to Coordinator mapping as given.
     * <p>
     * If the global identifier has already got associated information,
     * the operation returns false.
     * <p>
     * The timeout value, if non-zero, is used to establish a time-out for the
     * transaction; if the local identifier to Coordinator association
     * exists after the time-out period, then the TimeoutManager will
     * attempt to roll the transaction back.
     *
     * @param globalTID  The global identifier for the transaction.
     * @param localTID   The local identifier for the transaction.
     * @param coord      The Coordinator for the transaction.
     * @param timeout    The timeout for the transaction.
     * @param log        The log object for the transaction.
     *
     * @return  Indicates success of the operation.
     *
     * @see
     */
    static boolean addCoordinator(GlobalTID globalTID,
            Long localTID, CoordinatorImpl coord, int timeout) {

        boolean result = true;

        // Attempt to add the global and local indentifier to
        // Coordinator associations to the maps.

        coordsByGlobalTID.put(globalTID,coord);
        coordsByLocalTID.put(localTID,coord);

        // Set up the timeout for the transaction.  When active, the
        // timeout thread will periodically examine the map and abort
        // any active transactions on it that have gone beyond their
        // allocated time.

        if (timeout != 0) {
            TimeoutManager.setTimeout(localTID, TimeoutManager.ACTIVE_TIMEOUT,
                                      timeout);
        }

        return result;
    }

    /**
     * Removes the Coordinator associations for the given identifiers.
     * <p>
     * If there was no association the operation returns false.
     * <p>
     * Any timeout that was established for the Coordinator is cancelled,
     * and any active thread associations for the transaction are removed
     * and the corresponding Control objects destroyed.
     *
     * @param globalTID  The global identifier for the transaction.
     * @param localTID   The local identifier for the transaction.
     * @param aborted    The transaction aborted indicator.
     *
     * @return  Indicates success of the operation.
     *
     * @see
     */
    static boolean removeCoordinator(GlobalTID globalTID,
            Long localTID, boolean aborted) {

        boolean result = false;

        // Remove the global identifier to Coordinator mapping if possible.

        CoordinatorImpl coord  = null;
        result = (coordsByGlobalTID.remove(globalTID) != null);

        // Remove the InternalTid to Coordinator mapping if possible.

        if (result) {
            coord = (CoordinatorImpl) coordsByLocalTID.remove(localTID);
            result = (coord != null);
        }

        // If that succeeded, forget the CoordinatorLog object, if the
        // transaction is not a subtransaction.  The following may return
        // FALSE if there are no log records available
        // (i.e. non-recoverable OTS).

        if (coord != null) {
            try {
                if (coord.is_top_level_transaction()) {
                    if (inCompleteTxMap.get(coord) == null) {
                        if (Configuration.isDBLoggingEnabled())
                            LogDBHelper.getInstance().deleteRecord(localTID.longValue());
                        else
                            CoordinatorLog.removeLog(localTID);
                    } else {
                        if(_logger.isLoggable(Level.FINE)) {
                            _logger.logp(Level.FINE,"RecoveryManager","removeCoordinator()",
                                         "Transaction hasn't completed, let it stay in active logs");
                        }
                    }
                }
            } catch(SystemException exc) {
                result = false;
            }
        }

        // Clear the timeout for the transaction, if any.
        // Perform the removal under the timer mutex.

        TimeoutManager.setTimeout(localTID, TimeoutManager.CANCEL_TIMEOUT, 0);

        // Modify any thread associations there may be for the transaction, to
        // indicate that the transaction has ended.



        // COMMENT(Ram J) 09/19/2001 This below line is commented out since in
        // the J2EE controlled environment, all threads are associated and
        // dissociated in an orderly fashion, as well as there is no possibility
        // of concurrent threads active in a given transaction.
        //CurrentTransaction.endAll(globalTID, aborted);

        // If the count of resyncing Coordinators is greater than zero,
        // this means we are still in resync.  Decrease the count.

        if (resyncCoords > 0) {

            resyncCoords--;

            // If the number of resyncing Coordinators is now zero,
            // we may allow new work.

            if (resyncCoords == 0) {
                try {
                    resyncComplete(true, true);
                } catch (Throwable exc) {}
            }
        }

        return result;
    }

    /**
     * Returns a reference to the Coordinator object that corresponds to the
     * global identifier passed as a parameter.
     *
     * @param globalTID  The global identifier for the transaction.
     *
     * @return  The Coordinator for the transaction.
     *
     * @see
     */
    static CoordinatorImpl getCoordinator(GlobalTID globalTID) {

        CoordinatorImpl result = (CoordinatorImpl)
            coordsByGlobalTID.get(globalTID);

        return result;
    }

    /**
     * Read and update the transaction ID map atomically with the current 
     * thread, if and only if there is no concurrent activity for the 
     * specified transaction id.
     * 
     * @param tid transaction id.
     * 
     * @return true if there is no concurrent activity and the map has been 
     * updated.
     */
    static boolean readAndUpdateTxMap(GlobalTID tid) {
        synchronized (transactionIds) {
            Thread thread = (Thread) transactionIds.get(tid);
            if (thread != null) { // concurrent activity
                return false;            
            } 
            // register the thread for the transaction id
            transactionIds.put(tid, Thread.currentThread());                
            return true;
        }        
    }

    /**
     * Get the value (thread) for the specified transaction id from the 
     * transaction ID map.
     * 
     * @return the value for the transaction id key from the 
     * transaction ID map.
     */
    static Thread getThreadFromTxMap(GlobalTID tid) {           
        return (Thread) transactionIds.get(tid);
    }

    /**
     * Remove the specified transaction id from the transaction ID map.
     * 
     * @return the value for the transaction id key from the 
     * transaction ID map.
     */
    static Thread removeFromTxMap(GlobalTID tid) {           
        return (Thread) transactionIds.remove(tid);
    }
    
    /**
     * Requests that the RecoveryManager proceed with recovery.
     * <p>
     * The log is read and a list of TopCoordinators is reconstructed that
     * corresponds to those transactions that were in-doubt at the time of the
     * previous failure.
     * <p>
     * The method returns true if any transactions require resync.
     *
     * @param
     *
     * @return  Indicates that there are Coordinators requiring resync.
     *
     * @see
     */
    static boolean recover() {

        boolean result = false;

        if (skipRecoveryOnStartup()) {
            _logger.fine("========== no recovery ==========");
            // Quickly release all locks

            // Post the recovery in progress event so that requests
            // waiting for recovery to complete may proceed.
            recoveryInProgress.post();

            // And finish resync
            try {
                resyncComplete(false, false);
            } catch (Throwable ex) { }

            return result;
        }

        // Check the log for transactions.  If there are any outstanding
        // transactions, recover the Coordinator objects and set up the
        // OMGtid to Coordinator map.

        boolean keypointRequired = false;
        Enumeration logRecords = CoordinatorLog.getLogged();

        while (logRecords.hasMoreElements()) {
            keypointRequired = true;
            try {
                new TopCoordinator().
                    reconstruct((CoordinatorLog) logRecords.nextElement());
            } catch(Exception exc) {
				_logger.log(Level.SEVERE,"jts.recovery_in_doubt_exception",exc);
           		_logger.log(Level.SEVERE,"jts.recovery_in_doubt",exc.toString());
				 String msg = LogFormatter.getLocalizedMessage(_logger,
				 							"jts.recovery_in_doubt",
											new java.lang.Object[] {exc.toString()});
				 throw  new org.omg.CORBA.INTERNAL(msg);
            }
        }

        // Perform recovery of XA resources.

        //recoverXA();
		if(_logger.isLoggable(Level.FINE))
		{
			_logger.logp(Level.FINE,"RecoveryManager","recover()",
					"Before invoking proceedWithXARecovery()");
		}
        proceedWithXARecovery();

        // Post the recovery in progress event so that requests
        // waiting for recovery to complete may proceed.

        recoveryInProgress.post();

        // If resync is not needed, then perform after-resync
        // tasks immediately.

        result = coordsByGlobalTID.size() > 0;
        if (!result) {
            try {
                resyncComplete(false,keypointRequired);
            } catch(Throwable exc) {}
        }

        return result;
    }

    /**
     * Performs resync processing.
     * <p>
     * The RecoveryManager gets recovery information from each TopCoordinator
     * (while holding the transaction lock) and proceeds with resync.
     * <p>
     * Once resync is complete, a keypoint is taken to indicate that the log
     * information is no longer required.
     *
     * @param
     *
     * @return
     *
     * @see
     */
    static void resync() {

        // If there are any transactions, proceed with resync.  The map of
        // coordinators by global identifier is created during the
        // TopCoordinator reconstruct method when the coordinators are added
        // via addCoordinator. We copy the contents to another map as
        // Coordinators will remove themselves from the map during resync.

        // Now that the Coordinators have been reconstructed, record
        // the number of transactions requiring resync,
        // and make an event trace point. We must clone the Hashtable
        // here so that the Enumeration does not get
        // changed when any subsequent transaction is created (this can happen
        // when the last Coordinator is removed).

        resyncCoords = coordsByGlobalTID.size();
        Enumeration resyncList =
            ((Hashtable) coordsByGlobalTID.clone()).elements();

        boolean isRoot[] = new boolean[1];

        // Go through and resync each transaction.  The transaction lock
        // for each transaction is obtained to avoid deadlocks during recovery.

        while (resyncList.hasMoreElements()) {

            TopCoordinator coord = (TopCoordinator)resyncList.nextElement();

            try {

                // Before performing recovery, lock the coordinator.

                synchronized (coord) {

                    Status state = coord.recover(isRoot);

                    if (state == Status.StatusUnknown) {

                        // If the coordinator can be locked, then perform
                        // recovery on it. If the outcome is not currently
                        // known, we do nothing with the transaction,
                        // as we expect to eventually get an outcome
                        // from the parent. In this case an in-doubt timeout
                        // is established for the
                        // transaction so that it will continue to retry.
                        // For subordinates, the Coordinator will compl-ete the
                        // transaction itself as it will have no
                        // Synchronization objects.

                        TimeoutManager.setTimeout(
                            coord.getLocalTID(),
                            TimeoutManager.IN_DOUBT_TIMEOUT,
                            60);

                    } else if (state == Status.StatusCommitted) {

                        // For committed or rolled back, proceed with
                        // completion of the transaction, regardless of
                        // whether it is the root or a subordinate.
                        // If the transaction represents a root, it would
                        // normally wait for the CoordinatorTerm object to
                        // call before completing the transaction.  As there is
                        // no CoordinatorTerm in recovery, we must do it here.
					if(_logger.isLoggable(Level.FINE))
                    {
						_logger.logp(Level.FINE,"RecoveryManager","resync()",
								"Before invoking commit on the reconstructed coordinator"+
								"GTID is: "+
								((TopCoordinator)coord).superInfo.globalTID.toString());
						
                   	}


                        try {
                            coord.commit();
                        } catch (Throwable exc) {
							_logger.log(Level.WARNING,"jts.exception_during_resync",
	                                new java.lang.Object[] {exc.toString(),"commit"});
                        }

                        if (isRoot[0]) {
                            try {
                                coord.afterCompletion(state);
                            } catch (Throwable exc) {
								_logger.log(Level.WARNING,"jts.exception_during_resync",
                               		     new java.lang.Object[] {exc.toString(),
										 "after_completion"});
                            }
                        }

                    } else {

                        // By default, roll the transaction back.

                        try {
							if(_logger.isLoggable(Level.FINE))
                    		{
								_logger.logp(Level.FINE,"RecoveryManager","resync()",
										"Before invoking rollback on the"+
										"reconstructed coordinator :"+
										"GTID is : "+
										((TopCoordinator)coord).superInfo.globalTID.toString());
							
                    		}
                            coord.rollback(true);
                        } catch (Throwable exc) {
							_logger.log(Level.WARNING,"jts.resync_failed",
									new java.lang.Object [] {exc.toString(),"rollback"});
                        }

                        if (isRoot[0]) {
                            try {
                                coord.afterCompletion(Status.StatusRolledBack);
                            } catch (Throwable exc) {
								_logger.log(Level.WARNING,"jts.resync_failed",
										new java.lang.Object[]
                                        { exc.toString(), "after_completion"});
                            }
                        }
                    }
                }
            } catch (Throwable exc) {}
        }

        // Note that resyncComplete will be called by the
        // last TopCoordinator to complete resync (in removeCoordinator)
        // so we do not need to do it here.
    }

    /**
     * Called to indicate that resync is complete.
     * <p>
     * Indicates that all in-doubt Coordinators recovered from the log have
     * obtained global outcomes are corresponding transactions are complete.
     * <p>
     * The parameters indicate whether there were Coordinators
     * requiring resync, and whether a keypoint is required.
     *
     * @param resynced          Indicates whether any resync was done.
     * @param keypointRequired  Indicates whether the log needs keypointing.
     *
     * @return
     *
     * @exception LogicErrorException  An internal logic error occurred.
     *
     * @see
     */
    static void resyncComplete(boolean resynced,
            boolean keypointRequired) throws LogicErrorException {

        // Inform JTSXA that resync is complete, and trace the fact
        // that resync has completed.

        // COMMENT(Ram J) not needed anymore
        //JTSXA.resyncComplete();

        // Perform a keypoint of the log if required.

        if (keypointRequired) {
            CoordinatorLog.keypoint();
        }

        // Post the resync in progress event semaphore.

        if (resyncInProgress != null) {
            resyncInProgress.post();
            resyncInProgress = null;
        }
    }

    /**
     * Returns a reference to the Coordinator object that corresponds to the
     * local identifier passed as a parameter.
     *
     * @param localTID  The local identifier for the transaction.
     *
     * @return  The Coordinator object.
     *
     * @see
     */
    static CoordinatorImpl getLocalCoordinator(Long localTID) {

        CoordinatorImpl result = (CoordinatorImpl)
            coordsByLocalTID.get(localTID);

        return result;
    }

    /**
     * Determines whether the local transaction identifier represents a valid
     * transaction.
     *
     * @param localTID  The local transaction identifier to check.
     *
     * @return  Indicates the local transaction identifier is valid.
     *
     * @see
     */
    static boolean validLocalTID(Long localTID) {

        boolean result = coordsByLocalTID.containsKey(localTID);

        return result;
    }

    /**
     * Informs the RecoveryManager that the transaction service is being shut
     * down.
     *
     * For immediate shutdown,
     *
     * For quiesce,
     *
     * @param immediate  Indicates whether to stop immediately.
     *
     * @return
     *
     * @see
     */
    static void shutdown(boolean immediate) {


        /**
        if (immediate) {

            // If immediate, stop the resync thread if any.

            if (resyncThread != null) {
                resyncThread.stop();
            }
        } else {
        **/

            // Otherwise ensure that resync has completed.

            if (resyncInProgress != null) {
                try {
                    resyncInProgress.waitEvent();
                    if (resyncThread != null) {
                        resyncThread.join();
                    }
                } catch (InterruptedException exc) {}
            }
        /**
        }
        **/

        // COMMENT(Ram J) not needed anymore.
        //JTSXA.shutdown(immediate);

        // If not immediate shutdown, keypoint and close the log.
        // Only do this if the process is recoverable!

        if (!immediate && Configuration.isRecoverable()) {
            CoordinatorLog.keypoint();
            CoordinatorLog.finalizeAll();
        }

        //$Continue with shutdown/quiesce.
    }

    /**
     * Reduce the set of XAResource objects into a unique set such that there 
     * is at most one XAResource object per RM.
     */
    private static Enumeration getUniqueRMSet(Enumeration xaResourceList){
 
        Vector uniqueRMList = new Vector();
        
        while (xaResourceList.hasMoreElements()) {
            XAResource xaRes = (XAResource) xaResourceList.nextElement();
            int size = uniqueRMList.size();
            boolean match = false;
            for (int i = 0; i < size; i++) { // compare and eliminate duplicates
                XAResource uniqueXaRes = (XAResource) uniqueRMList.elementAt(i);
                try {
                    if (xaRes.isSameRM(uniqueXaRes)) {
                        match = true; 
                        break;
                    }
                } catch (XAException xe) {}
            }
            if (!match) { 
                uniqueRMList.add(xaRes); 
            }
        }
        
        return uniqueRMList.elements();
    }
    
    /**
     * Recovers the in doubt transactions from the provided list of
     * XAResource objects. This method is never called by the recovery
     * thread, and its the application threads which wants to pass in
     * the XA resources that call this.
     *
     * @param xaResources enumerated list of XA Resources to be recovered
     *
     */
    public static void recoverXAResources(Enumeration xaResources) {

        /*  This method has been newly added - Ram Jeyaraman */

        String manualRecovery =
            Configuration.getPropertyValue(Configuration.MANUAL_RECOVERY);

        // if ManualRecovery property is not set, do not attempt XA recovery.
        if (manualRecovery == null  ||
                !(manualRecovery.equalsIgnoreCase("true"/*#Frozen*/))) {
            return;
        }

        synchronized (lockObject) {

            if (uniqueRMSetReady.isPosted() == false) {
                RecoveryManager.uniqueRMSet = getUniqueRMSet(xaResources);
                uniqueRMSetReady.post();
                waitForResync();
                return;
            } else {
                RecoveryManager.waitForResync();
                RecoveryManager.uniqueRMSet = getUniqueRMSet(xaResources);
                // the following call is meant to induce recovery. But
                // currently it will not work as intended, if it is called
                // during regular TP processing. Currently, this call deals
                // only with XA recovery. There needs to be some support
                // from the coordinator to be able to support recovery
                // during TP processing.
                proceedWithXARecovery();
            }
        }
    }

    /**
     * This method returns InDoubt Xids for a given XAResource
     */

     static Xid[] getInDoubtXids(XAResource xaResource) {
	 if(_logger.isLoggable(Level.FINE))
	 {
	     _logger.logp(Level.FINE,"RecoveryManager", "getInDoubtXids()",
	      "Before receiving inDoubtXids from xaresource = " +
	      xaResource);
         }
         Xid[] inDoubtXids = null;
         ArrayList<Xid> inDoubtXidList = null;
         int flags;
         String recoveryScanFlags = System.getProperty("RECOVERSCANFLAGS");
         if (recoveryScanFlags != null && recoveryScanFlags.equals("TMNOFLAGS"))
             flags = XAResource.TMSTARTRSCAN;
         else
             flags = XAResource.TMSTARTRSCAN | XAResource.TMENDRSCAN;
         boolean continueLoop = true;
         while (continueLoop) {
	     try {
                 inDoubtXids = xaResource.recover(flags);
                 if (inDoubtXids == null || inDoubtXids.length == 0)
                     break;
                 if (flags == XAResource.TMSTARTRSCAN || flags ==  XAResource.TMNOFLAGS) {
                      flags = XAResource.TMNOFLAGS;
                      if (inDoubtXidList == null) {
                          inDoubtXidList = new ArrayList<Xid>();
                      }
                      for (int i = 0; i < inDoubtXids.length; i++)
                          inDoubtXidList.add(inDoubtXids[i]);
                 }
                 else {
                     break;
                 }
             } catch (XAException e) {
	        _logger.log(Level.WARNING,"jts.xaexception_in_recovery", e.errorCode);
            _logger.log(Level.WARNING, com.sun.jts.trace.TraceUtil.getXAExceptionInfo(e, _logger), e);
                break;
             }

         }
         if (inDoubtXidList != null)
             inDoubtXids = inDoubtXidList.toArray(new Xid[]{});
	 if(_logger.isLoggable(Level.FINE) && (inDoubtXids != null))
	 {
	     String xidList = LogFormatter.convertXidArrayToString(inDoubtXids);
	     _logger.logp(Level.FINE,"RecoveryManager",
	     "getInDoubtXid()",
	     "InDoubtXids returned from xaresource = "+
	      xaResource + "are: " +xidList);
	 }
         return inDoubtXids;
     }

    /**
     * This method is used to recontruct and register the Resource objects
     * corresponding to in-doubt transactions in the RMs. It is assumed
     * that the XAResource list has already been provided to the
     * Recovery Manager. This method can be called by Recovery Thread as
     * well as any other thread driving recovery of XA Resources.
     */
    private static void proceedWithXARecovery() {

        /*  This method has been newly added - Ram Jeyaraman */

        Enumeration xaResources = RecoveryManager.uniqueRMSet;
/**
        if (xaResources == null) {
            // TODO - check that automatic recovery works in a clustered instance
            return;
        }
**/

        String manualRecovery =
            Configuration.getPropertyValue(Configuration.MANUAL_RECOVERY);

        // if ManualRecovery property is not set, do not attempt XA recovery.
        if (manualRecovery == null  ||
                !(manualRecovery.equalsIgnoreCase("true"/*#Frozen*/))) {
            return;
        }

        if (Thread.currentThread().getName().equals("JTS Resync Thread"/*#Frozen*/)) {

            if (uniqueRMSetReady != null) {
                try {
                    uniqueRMSetReady.waitEvent();
                    txRecoveryFence.raiseFence();
                    xaResources = RecoveryManager.uniqueRMSet;
                } catch (InterruptedException exc) {
					_logger.log(Level.SEVERE,"jts.wait_for_resync_complete_interrupted");
					 String msg = LogFormatter.getLocalizedMessage(_logger,
					 							"jts.wait_for_resync_complete_interrupted");
					  throw  new org.omg.CORBA.INTERNAL(msg);
                }
            }
        }

        // sanity check
        if (xaResources == null) {
            return;
        }

        Vector otsResources = new Vector();
        // Map uniqueXids = new Hashtable();
        Set uniqueXids = new HashSet();

        while (xaResources.hasMoreElements()) {

            XAResource xaResource = (XAResource) xaResources.nextElement();

            // Get the list of XIDs which represent in-doubt transactions
            // for the database.

            Xid[] inDoubtXids = getInDoubtXids(xaResource);
            // uniqueXids.clear();
            if (inDoubtXids == null || inDoubtXids.length == 0) {
                    continue; // No in-doubt xids for this resource.
            }

            for (int i = 0; i < inDoubtXids.length; i++) {

                    // check to see if the xid belongs to this server.

                    String branchQualifier =
                        new String(inDoubtXids[i].getBranchQualifier());
                    String serverName = Configuration.getServerName();
                    
                    if (branchQualifier.startsWith(serverName)) {

                        // check if the xid is a duplicate. i.e., Xids
                        // which have same globalId and branchId are
                        // considered duplicates. Note that the
                        // branchId format is (serverId, rmId). This is
                        // to make sure that at most one OTSResource object
                        // is registered with the coordinator per transaction
                        // per RM.
                        
                        if (!uniqueXids.contains(inDoubtXids[i])) { // unique xid
                            if(_logger.isLoggable(Level.FINE))
                            {
                                _logger.logp(Level.FINE,"RecoveryManager",
                                        "proceedWithXARecovery",
                                        " This xid is UNIQUE " +
                                        inDoubtXids[i]);
                            }

                            uniqueXids.add(inDoubtXids[i]);// add to uniqueList

                            // Create an OTSResource for the in-doubt
                            // transaction and add it to the list. Each
                            // OTSResource represents a RM per transaction.
                            otsResources.addElement(
                                new OTSResourceImpl(inDoubtXids[i],
                                                    xaResource, null
                                                   ).getCORBAObjReference());
                        } else {
                            if(_logger.isLoggable(Level.FINE))
                            {
                                _logger.logp(Level.FINE,"RecoveryManager",
                                            "proceedWithXARecovery",
                                            " This xid is NOTUNIQUE " +
                                            inDoubtXids[i]);
                            }

                        }
                    } else {
                        if(_logger.isLoggable(Level.FINE))
                        {
                                _logger.logp(Level.FINE,"RecoveryManager",
                                        "proceedWithXARecovery",
                                        " This xid doesn't belong to me " +
                                        inDoubtXids[i]);
                            }

                    }
                }
        }


        // For each OTSResource, determine whether the transaction is known,
        // and if so, register it, otherwise roll it back.

        for (int i = 0; i < otsResources.size(); i++) {

            OTSResource otsResource = (OTSResource) otsResources.elementAt(i);
            GlobalTID globalTID = new GlobalTID(otsResource.getGlobalTID());
            TopCoordinator coord =
                (TopCoordinator) coordsByGlobalTID.get(globalTID);

            if (coord == null) {
                // Roll the OTSResource back if the transaction is not
                // recognised. This happens when the RM has recorded its
                // prepare vote, but the JTS has not recorded its prepare vote.
		    	if(_logger.isLoggable(Level.FINE))
		    	{
					_logger.logp(Level.FINE,"RecoveryManager","proceedWithXARecovery()",
							"Could  not recognize OTSResource: "+otsResource + 
							" with tid: " + 
							LogFormatter.convertToString(globalTID.realTID.tid)+
							";Hence rolling this resource back...");
		    	}
		
               boolean infiniteRetry = true;
               int commitRetries = Configuration.getRetries();
               if (commitRetries >= 0)
                   infiniteRetry = false;
               int commitRetriesLeft = commitRetries;
                boolean exceptionisThrown = true;
                while (exceptionisThrown) {
                    try {
                        otsResource.rollback();
                        exceptionisThrown = false;
                    } catch (Throwable exc) {
                        if ((exc instanceof COMM_FAILURE) || (exc instanceof TRANSIENT)) {
                            if (commitRetriesLeft > 0 || infiniteRetry) {
                                // For TRANSIENT or COMM_FAILURE, wait
                                // for a while, then retry the commit.
                                if (!infiniteRetry) {
                                    commitRetriesLeft--;
                                }

                                try {
                                    Thread.sleep(Configuration.COMMIT_RETRY_WAIT);
                                } catch( Throwable e ) {}
                            }
                            else {
					           _logger.log(Level.WARNING,"jts.exception_during_resync",
							            new java.lang.Object[] {exc.toString(),"OTSResource rollback"});
                               exceptionisThrown = false;
                            }
                        }
                        else {
				            _logger.log(Level.WARNING,"jts.exception_during_resync",
						            new java.lang.Object[] {exc.toString(),"OTSResource rollback"});
                            exceptionisThrown = false;
                        }
                    }
                }
            } else {
                // NOTE: Currently unimplemented. The coordinator needs to
                // check if duplicate resources are being registered for the
                // same RM for the same xid. Also the coordinator should
                // not go away, until all its resources have been sent
                // completion notification. The keypointing should not
                // be done *as is* in the removeCoordinator() method.
                // waitForResync semaphore needs to be flagged when the
                // recovery thread goes away.

                // Register the OTSResource with the Coordinator.
                // It will be called for commit or rollback during resync.
			    	if(_logger.isLoggable(Level.FINE))
	    			{
						_logger.logp(Level.FINE,"RecoveryManager",
								"proceedWithXARecovery()",
								"Recognized OTSResource: " + otsResource + 
								" with tid: " +
								LogFormatter.convertToString(globalTID.realTID.tid) +
								";Hence registering this resource with coordinator...");
	    			}
               		 coord.directRegisterResource(otsResource);
            }
        }
	}

    static void dbXARecovery() {
        Enumeration xaResources = RecoveryManager.uniqueRMSet;

        if (skipRecoveryOnStartup()) {
            _logger.fine("========== no recovery ==========");
            try {
            resyncComplete(false, false);
            } catch (Throwable ex) { }
            return;
        }

        if (Thread.currentThread().getName().equals("JTS Resync Thread"/*#Frozen*/)) {
            if (uniqueRMSetReady != null) {
                try {
                	_logger.fine("dbXArecovery()");
                    uniqueRMSetReady.waitEvent();
                    xaResources = RecoveryManager.uniqueRMSet;
                } catch (InterruptedException exc) {
		    _logger.log(Level.SEVERE,"jts.wait_for_resync_complete_interrupted");
		    String msg = LogFormatter.getLocalizedMessage(_logger,
		              "jts.wait_for_resync_complete_interrupted");
		    throw  new org.omg.CORBA.INTERNAL(msg);
                }
            }
        }

        // sanity check
        if (xaResources == null) {
            try {
            resyncComplete(false, false);
            } catch (Throwable ex) { }
            return;
        }

        //dbXARecovery(Configuration.getServerName(), xaResources);
        // Configuration.getServerName() might be not quite right at auto-recovery
        String sname = LogDBHelper.getInstance().getServerNameForInstanceName(Configuration.getPropertyValue(Configuration.INSTANCE_NAME));
        if (sname != null) {
            dbXARecovery(sname, xaResources);
        }

        try {
        resyncComplete(false, false);
        } catch (Throwable ex) { ex.printStackTrace(); }

	}

    static void dbXARecovery(String serverName, Enumeration xaResources) {
        // Get global TIDs
        Map gtidMap = LogDBHelper.getInstance().getGlobalTIDMap(serverName);

        Set uniqueXids = new HashSet();
        if(_logger.isLoggable(Level.INFO)) {
            _logger.log(Level.INFO, "RecoveryManager.dbXARecovery recovering for serverName: " + serverName);
        }

        // if flag is set use commit_one_phase (old style), otherwise use commit
        boolean one_phase = getCommitOnePhaseDuringRecovery();
        while (xaResources.hasMoreElements()) {

            XAResource xaResource = (XAResource) xaResources.nextElement();
            if(_logger.isLoggable(Level.INFO)) {
                _logger.log(Level.INFO, "RecoveryManager.dbXARecovery processing  xaResource: " + xaResource);
            }

            // Get the list of XIDs which represent in-doubt transactions
            // for the database.

            Xid[] inDoubtXids = getInDoubtXids(xaResource);
            // uniqueXids.clear();
            if (inDoubtXids == null || inDoubtXids.length == 0) {
                    continue; // No in-doubt xids for this resource.
            }
            for (int i = 0; i < inDoubtXids.length; i++) {

                    // check to see if the xid belongs to this server.

                    String branchQualifier =
                        new String(inDoubtXids[i].getBranchQualifier());
                    //String serverName = Configuration.getServerName();
                    if(_logger.isLoggable(Level.INFO)) {
                        _logger.log(Level.INFO, "RecoveryManager.dbXARecovery inDoubtXid: " + 
                                inDoubtXids[i] + " branchQualifier: " + branchQualifier);
                    }
                    
                    if (branchQualifier.startsWith(serverName)) {

                        // check if the xid is a duplicate. i.e., Xids
                        // which have same globalId and branchId are
                        // considered duplicates. Note that the
                        // branchId format is (serverId, rmId). This is
                        // to make sure that at most one OTSResource object
                        // is registered with the coordinator per transaction
                        // per RM.
                        
                        if (!uniqueXids.contains(inDoubtXids[i])) { // unique xid
                            if(_logger.isLoggable(Level.FINE))
                            {
                                _logger.logp(Level.FINE,"RecoveryManager",
                                        "dbXARecovery",
                                        " This xid is UNIQUE " +
                                        inDoubtXids[i]);
                            }

                            uniqueXids.add(inDoubtXids[i]); // add to uniqueList

                            try {
                            byte[] gtrid = inDoubtXids[i].getGlobalTransactionId();
                            GlobalTID gtid = GlobalTID.fromTIDBytes(gtrid);
                            Long localTID = (Long)gtidMap.get(gtid);
                            if(_logger.isLoggable(Level.INFO)) {
                                _logger.log(Level.INFO, "RecoveryManager.dbXARecovery completing transaction for localTID: " + localTID);
                            }
                            if (localTID == null) {
                                 xaResource.rollback(inDoubtXids[i]);
                            } else {
                                 xaResource.commit(inDoubtXids[i], one_phase);
                                 LogDBHelper.getInstance().deleteRecord(localTID.longValue(), serverName);
                            }
                            } catch (Exception ex) { ex.printStackTrace(); }
                        } else {
                            if(_logger.isLoggable(Level.INFO))
                            {
                                _logger.logp(Level.INFO,"RecoveryManager",
                                            "dbXARecovery",
                                            " This xid is NOTUNIQUE " +
                                            inDoubtXids[i]);
                            }
                        }
                    } else {
                        if(_logger.isLoggable(Level.INFO))
                        {
                            _logger.logp(Level.INFO,"RecoveryManager",
                                        "dbXARecovery",
                                        " This xid doesn't belong to me " +
                                        inDoubtXids[i]);
                        }

                    }
                }
        }
/**
        try {
        resyncComplete(false, false);
        } catch (Throwable ex) { ex.printStackTrace(); }
**/
    }


    /**
     * Requests that the RecoveryManager proceed with recovery of XA resources
     * via JTSXA.
     * <p>
     * JTSXA returns a list of OTSResource objects which require
     * outcomes.  These are registered with appropriate Coordinators or rolled
     * back as appropriate.
     *


    /**
     * Requests that the RecoveryManager proceed with recovery of XA resources
     * via JTSXA.
     * <p>
     * JTSXA returns a list of OTSResource objects which require
     * outcomes.  These are registered with appropriate Coordinators or rolled
     * back as appropriate.
     *
     * @param
     *
     * @return
     *
     * @see
     */
    /*
     * DISCARD(Ram J) - this method is not needed anymore. This has been
     * replaced by proceedWithXARecovery method.
     */
    /*
    private static void recoverXA() {

        boolean result = false;

        // Get a list of OTSResource objects from JTSXA.

        Vector resources = new Vector();
        JTSXA.recover(resources);
        Enumeration res = resources.elements();

        // For each OTSResource, determine whether the transaction is known,
        // and if so, register it, otherwise roll it back.

        while (res.hasMoreElements()) {

            TxOTSResource xares = (TxOTSResource) res.nextElement();
            GlobalTID globalTID = new GlobalTID(xares.getGlobalTID());
            TopCoordinator coord =
                (TopCoordinator) coordsByGlobalTID.get(globalTID);

            //    report();

            if (coord == null) {

                // Roll the OTSResource back if the transaction is not
                // recognised. This happens when the RM has recorded its
                // prepare vote, but the JTS has not recorded its prepare vote.

                try {
                    xares.rollback();
                } catch (Throwable exc) {
					_logger.log(Level.WARNING,"jts.exception_during_resync",
	                        new java.lang.Object[] { exc.toString(), "xa_rollback"});


                }
            } else {

                // Register the OTSResource with the Coordinator.
                // It will be called for commit or rollback during resync.
                coord.directRegisterResource(xares);
            }
        }
    }
    */

    /**
     * Returns an array of Coordinator objects currently active.
     *
     * @param
     *
     * @return  The array of Coordinators.
     *
     * @see
     */
    static CoordinatorImpl[] getCoordinators() {

        int size = coordsByGlobalTID.size();
        CoordinatorImpl[] result = new CoordinatorImpl[size];

        Enumeration coords = coordsByGlobalTID.elements();

		for(int pos = 0;pos<size;){
            result[pos++] = (CoordinatorImpl) coords.nextElement();
        }

        return result;
    }

	static Hashtable/*<GlobalTID,Coordinator>*/ getCoordsByGlobalTID()
	{
		return coordsByGlobalTID;
	}



    /**
     * Gets the restart data for the process.
     *
     * @param
     *
     * @return  The restart data.
     *
     * @see
     */
    public static byte[] getRestart() {

        byte[] result = null;
        LogFile logFile = Configuration.getLogFile();
        if (logFile != null)
          result = logFile.readRestart();

        return result;
    }

    /**
     * Sets the restart data for the process.
     *
     * @param bytes  The restart data.
     *
     * @return
     *
     * @see
     */
    public static void setRestart(byte[] bytes) {

        LogFile logFile = Configuration.getLogFile();

        if (logFile != null) {
            if (!logFile.writeRestart(bytes)) {
				_logger.log(Level.WARNING,"jts.restart_write_failed");
            }
        }
    }

    /**
     * Waits for recovery to complete.
     *
     * @param
     *
     * @return
     *
     * @see
     */
    public static void waitForRecovery() {

        if (recoveryInProgress != null) {
            try {
                recoveryInProgress.waitEvent();
            } catch (InterruptedException exc) {
				_logger.log(Level.SEVERE,"jts.wait_for_resync_complete_interrupted");
				 String msg = LogFormatter.getLocalizedMessage(_logger,
											"jts.wait_for_resync_complete_interrupted");
				  throw  new org.omg.CORBA.INTERNAL(msg);
            }
        }
    }

    /**
     * Waits for resync to complete with timeout.
     *
     * @param cmtTimeout Container managed transaction timeout
     *
     * @return
     *
     * @see
     */
    public static void waitForResync(int cmtTimeOut) {

        if (resyncInProgress != null) {
            try {
                resyncInProgress.waitTimeoutEvent(cmtTimeOut);
            } catch (InterruptedException exc) {
		_logger.log(Level.SEVERE,"jts.wait_for_resync_complete_interrupted");
		 String msg = LogFormatter.getLocalizedMessage(_logger,
			"jts.wait_for_resync_complete_interrupted");
                 throw  new org.omg.CORBA.INTERNAL(msg);
            }
        }
    }
    
    /**
     * Waits for resync to complete.
     *
     * @param
     *
     * @return
     *
     * @see
     */
    public static void waitForResync() {

        if (resyncInProgress != null) {
            try {
                resyncInProgress.waitEvent();
            } catch (InterruptedException exc) {
		_logger.log(Level.SEVERE,"jts.wait_for_resync_complete_interrupted");
		 String msg = LogFormatter.getLocalizedMessage(_logger,
			"jts.wait_for_resync_complete_interrupted");
                 throw  new org.omg.CORBA.INTERNAL(msg);
            }
        }
    }

    static void addToIncompleTx(CoordinatorImpl coord, boolean commit) {
        inCompleteTxMap.put(coord, commit);
    }

    public static Boolean isIncompleteTxRecoveryRequired() {
        String logdir = Configuration.getPropertyValue(Configuration.LOG_DIRECTORY);
	    if (inCompleteTxMap.isEmpty() ||
                    logdir == null || !(new File(logdir)).exists()) {
                return Boolean.FALSE;
	    }
            else {
                return Boolean.TRUE;
	    }
    } 

    public static int sizeOfInCompleteTx() {
        return inCompleteTxMap.size();
    }
   
    public static void recoverIncompleteTx(XAResource[] xaresArray) {
        if ((xaresArray == null) || (xaresArray.length == 0))
            return;
        int size = xaresArray.length;
        Vector v = new Vector();
        for (int i=0; i<size; i++) {
            v.addElement(xaresArray[i]);
        }
        Enumeration resourceList = getUniqueRMSet(v.elements());
        Set uniqueXids = new HashSet();
        Vector otsResources = new Vector();
        while (resourceList.hasMoreElements()) {
            XAResource xaResource = (XAResource) resourceList.nextElement();
            // Get the list of XIDs which represent in-doubt transactions
            // for the database.
            Xid[] inDoubtXids = getInDoubtXids(xaResource);
            // uniqueXids.clear();
            if (inDoubtXids == null || inDoubtXids.length == 0) {
                continue; // No in-doubt xids for this resource.
            }

            for (int i = 0; i < inDoubtXids.length; i++) {

                    // check to see if the xid belongs to this server.

                    String branchQualifier =
                        new String(inDoubtXids[i].getBranchQualifier());
                    String serverName = Configuration.getServerName();

                    if (branchQualifier.startsWith(serverName)) {

                        // check if the xid is a duplicate. i.e., Xids
                        // which have same globalId and branchId are
                        // considered duplicates. Note that the
                        // branchId format is (serverId, rmId). This is
                                 // to make sure that at most one OTSResource object
                        // is registered with the coordinator per transaction
                        // per RM.

                        if (!uniqueXids.contains(inDoubtXids[i])) { // unique xid
                            if(_logger.isLoggable(Level.FINE))
                            {
                                _logger.logp(Level.FINE,"RecoveryManager",
                                        "recoverIncompleteTx",
                                        " This xid is UNIQUE " +
                                        inDoubtXids[i]);
                            }
                            uniqueXids.add(inDoubtXids[i]); // add to uniqueList
                            otsResources.addElement(
                                new OTSResourceImpl(inDoubtXids[i],
                                                    xaResource, null
                                                   ));
                        } else {
                            if(_logger.isLoggable(Level.FINE))
                            {
                                _logger.logp(Level.FINE,"RecoveryManager",
                                            "recoverIncompleteTx",
                                            " This xid is NOTUNIQUE " +
                                            inDoubtXids[i]);
                            }
                        }
                    } else {
                        if(_logger.isLoggable(Level.FINE))
                        {
                            _logger.logp(Level.FINE,"RecoveryManager",
                                        "recoverIncompleteTx",
                                        " This xid doesn't belong to me " +
                                        inDoubtXids[i]);
                        }
                    }
                }
            } // while (true)

            // if flag is set use commit_one_phase (old style), otherwise use commit
            boolean commit_one_phase = getCommitOnePhaseDuringRecovery();
            for (int i = 0; i < otsResources.size(); i++) {
                OTSResourceImpl otsResource = (OTSResourceImpl) otsResources.elementAt(i);
                GlobalTID globalTID = new GlobalTID(otsResource.getGlobalTID());
                synchronized (inCompleteTxMap) {
                    Enumeration e = inCompleteTxMap.keys();
                    while (e.hasMoreElements()) {
                        CoordinatorImpl cImpl = (CoordinatorImpl)e.nextElement();
                        GlobalTID gTID = new GlobalTID(cImpl.getGlobalTID());
                        if (gTID.equals(globalTID)) {
                            Boolean commit = (Boolean) inCompleteTxMap.get(cImpl);
                            boolean infiniteRetry = true;
                            int commitRetries = Configuration.getRetries();
                            if (commitRetries >= 0)
                                infiniteRetry = false;
                            int commitRetriesLeft = commitRetries;
                            boolean exceptionisThrown = true;
                            while (exceptionisThrown) {
                                try {
                                    if (commit.booleanValue()) {
                                        if (commit_one_phase) {
                                            otsResource.commit_one_phase();
                                        } else {
                                            otsResource.commit();
                                        }
                                        if(_logger.isLoggable(Level.FINE)) {
                                            _logger.logp(Level.FINE,"RecoveryManager",
                                                         "recoverIncompleteTx",
                                                         " committed  " +
                                                         otsResource);
                                        }

                                    }
                                    else { 
                                        otsResource.rollback();
                                        if(_logger.isLoggable(Level.FINE)) {
                                            _logger.logp(Level.FINE,"RecoveryManager",
                                                         "recoverIncompleteTx",
                                                         " rolled back  " +
                                                         otsResource);
                                        }

                                    }
                                    exceptionisThrown = false;
                                } catch (Throwable exc) {
                                    if ((exc instanceof COMM_FAILURE) || (exc instanceof TRANSIENT)) {
                                        if (commitRetriesLeft > 0 || infiniteRetry) {
                                            // For TRANSIENT or COMM_FAILURE, wait
                                            // for a while, then retry the commit.
                                            if (!infiniteRetry) {
                                                commitRetriesLeft--;
                                            }
                                            try {
                                                Thread.sleep(Configuration.COMMIT_RETRY_WAIT);
                                            } catch( Throwable iex ) {}
                                        }
                                        else {
                                             _logger.log(Level.WARNING,"jts.exception_during_resync",
			                            new java.lang.Object[] {exc.toString(),"OTSResource " + 
                                                    ((commit.booleanValue())? "commit" : "rollback")});
                                            exceptionisThrown = false;
                                        }
                                    }
                                    else {
		                        _logger.log(Level.WARNING,"jts.exception_during_resync",
		                                new java.lang.Object[] {exc.toString(),"OTSResource " + 
                                                    ((commit.booleanValue())? "commit" : "rollback")});
                                        exceptionisThrown = false;
                                    }
                                }
                            }
                        }
                    }
                }
            }
    }

    static void createRecoveryFile(String serverName) {
        try {
            String logPath = LogControl.getLogPath();
            if (new File(logPath).exists()) {
                File recoveryFile = LogControl.recoveryIdentifierFile(serverName,logPath);
                RandomAccessFile raf = new RandomAccessFile(recoveryFile,"rw");
                raf.writeBytes(serverName);
                raf.setLength(serverName.length());
                raf.close();
            }
        } catch (Exception ex) {
            _logger.log(Level.WARNING,"jts.exception_in_recovery_file_handling",ex);
        }
    }

    /**
     * Register the implementation of Transaction recovery fence.
     * This service is started as soon as all the resources are available.
     */
    public static void registerTransactionRecoveryFence(TransactionRecoveryFence fence) {
        txRecoveryFence = fence;
    }

    /**
     * return the TxRecoveryFence Object
     */
    static TransactionRecoveryFence getTransactionRecoveryFence() {
        return txRecoveryFence;
    }

    /**
     * Start Transaction recovery fence.
     */
    public static void startTransactionRecoveryFence() {
        if (txRecoveryFence != null) {
            // Perform any extra steps (like finish delegated recovery if necessary
            txRecoveryFence.start();
        } else {
            _logger.log(Level.WARNING,"", new IllegalStateException());
        }
    }

    /**
     * return true if commit_one_phase should be used during recovery
     */
    private static boolean getCommitOnePhaseDuringRecovery() {
        String propValue = Configuration.getPropertyValue(Configuration.COMMIT_ONE_PHASE_DURING_RECOVERY);
        if (propValue != null && propValue.equalsIgnoreCase("true"/*#Frozen*/)) {
            return true;
        }
        return false;
    }

    /**
     * return true recovery on startup should be skipped
     */
    private static boolean skipRecoveryOnStartup() {
        // if ManualRecovery property is not set, or the logdir does not exist
        // do not attempt XA recovery.
        String logdir = Configuration.getPropertyValue(Configuration.LOG_DIRECTORY);
        if(_logger.isLoggable(Level.FINE)) {
            _logger.fine("========== logdir ========== to recover ========= " + logdir);
            if (logdir != null)
                _logger.fine("========== logdir ========== exists ========= " + (new File(logdir)).exists());
        }

        String manualRecovery =
            Configuration.getPropertyValue(Configuration.MANUAL_RECOVERY);

        return (manualRecovery == null  || !(manualRecovery.equalsIgnoreCase("true"/*#Frozen*/)) ||
                logdir == null || !(new File(logdir)).exists());
     }

    /**
     * Start resync thread
     */
    public static void startResyncThread() {
        if (resyncThread == null) {
            initialise();
        }
	if(_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE,"RecoveryManager.startResyncThread Configuration.isRecoverable? "
                    + Configuration.isRecoverable());
	}
        if (Configuration.isRecoverable()) {
            resyncThread.start();
        }
    }

    /**
     * Reports the contents of the RecoveryManager tables.
     * $Only required for debug.
     *
     * @param immediate  Indicates whether to stop immediately.
     *
     * @return
     *
     * @see
     */
    /*
    static void report() {

        // Report on coordsByGlobalTID.

        if (coordsByGlobalTID.size() > 0) {
			if(_logger.isLoggable(Level.FINE))
			{
				 _logger.logp(Level.FINE,"RecoveryManager","report()",
				 		"RecoveryManager.coordsByGlobalTID non-empty");
			}
            Enumeration keys = coordsByGlobalTID.keys();

            while (keys.hasMoreElements()) {
                GlobalTID globalTID = (GlobalTID) keys.nextElement();
                CoordinatorImpl coordImpl =
                    (CoordinatorImpl) coordsByGlobalTID.get(globalTID);
				if(_logger.isLoggable(Level.FINE))
				{
					_logger.logp(Level.FINE,"RecoveryManager","report()",
							"GlobalTid :"+globalTID+" -> "+coordImpl);
				}
            }
        } else {
				if(_logger.isLoggable(Level.FINE))
				{
					_logger.logp(Level.FINE,"RecoveryManager","report()", 
		          			"RecoveryManager.coordsByGlobalTID empty");
				}
        }

        // Report on coordsByLocalTID.

        if (coordsByLocalTID.size() > 0) {
			if(_logger.isLoggable(Level.FINE))
			{
				_logger.logp(Level.FINE,"RecoveryManager","report()", 
            			"RecoveryManager.coordsByLocalTID non-empty");
			}
            Enumeration keys = coordsByLocalTID.keys();
            while (keys.hasMoreElements()) {
                Long localTID = (Long)keys.nextElement();
                CoordinatorImpl coordImpl =
                    (CoordinatorImpl) coordsByLocalTID.get(localTID);
				if(_logger.isLoggable(Level.FINE))
				{
					_logger.logp(Level.FINE,"RecoveryManager","report()",
							"LocalTid:"+localTID+" -> " + coordImpl);
				}
            }
        } else {
			if(_logger.isLoggable(Level.FINE))
			{
				 _logger.logp(Level.FINE,"RecoveryManager","report()",
          		  		"RecoveryManager.coordsByLocalTID empty");
			}
        }
    }
    */

   /**
    * A no-op class
    */
   static class TransactionRecoveryFenceSimple implements TransactionRecoveryFence {

        private final Semaphore semaphore = new Semaphore(1, true);

        public void start() {
        }

        /**
         * {@inheritDoc}
         */
        public void raiseFence() {
            try {
                semaphore.acquire();
            } catch(InterruptedException ie) {
                _logger.log(Level.FINE,"Error in acquireReadLock",ie);
            }
        }

        /**
         * {@inheritDoc}
         */
        public void lowerFence() {
            semaphore.release();
        }

    }
}

/**
 * This class represents a thread on which the RecoveryManager can perform
 * resync operations.
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

class ResyncThread extends Thread  {

    /**
     * ResyncThread constructor.
     *
     * @param
     *
     * @return
     *
     * @see
     */
    static Logger _logger = LogDomains.getLogger(ResyncThread.class, LogDomains.TRANSACTION_LOGGER);

    ResyncThread() {
        setName("JTS Resync Thread"/*#Frozen*/);
        setDaemon(true);
    }

    /**
     * Performs resync.
     *
     * @param
     *
     * @return
     *
     * @see
     */
    public void run() {
        yield();

	if(_logger.isLoggable(Level.FINE))
	{
		_logger.logp(Level.FINE,"ResyncThread","run()","Before invoking RecoveryManager.recover()"); 
	}
        try {
            if (Configuration.isDBLoggingEnabled()) {
                RecoveryManager.dbXARecovery();
            } else {
                if (RecoveryManager.recover()) {
                    RecoveryManager.resync();
                }
            }
        } catch (Throwable ex) {
            _logger.log(Level.SEVERE,"jts.log_exception_at_recovery",ex);
        } finally {
            try {
                RecoveryManager.resyncComplete(false,false);
            } catch (Throwable tex) {tex.printStackTrace();} // forget any exeception in resyncComplete
        }
        if(RecoveryManager.getTransactionRecoveryFence() != null)
            RecoveryManager.getTransactionRecoveryFence().lowerFence();

    }


}

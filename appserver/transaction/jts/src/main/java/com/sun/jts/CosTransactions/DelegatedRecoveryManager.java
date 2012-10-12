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

package com.sun.jts.CosTransactions;

import java.util.*;
import java.io.*;

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
import com.sun.jts.utils.RecoveryHooks.FailureInducer;

/**
 * This class manages information required for Delegated recovery. This class supports
 * multiple delegated recoveries at the same time. Functionality is alsomost same as
 * RecoveryManager.java. This class maintains the map between state and log location
 * instead of static data incase of Recovery Manager.
 *
 * @version 0.01
 *
 * @author Sankara Rao Bhogi
 *
 * @see
 */

public class DelegatedRecoveryManager {
    
    private static Hashtable recoveryStatetable = new Hashtable();
    private static Hashtable tmoutMgrtable = new Hashtable();
    
    synchronized static DelegatedTimeoutManager getTimeoutManager(String logPath) {
        DelegatedTimeoutManager tmoutMgr = (DelegatedTimeoutManager)tmoutMgrtable.get(logPath);
        if (tmoutMgr != null)
            return tmoutMgr;
        tmoutMgr =  new DelegatedTimeoutManager(logPath);
        tmoutMgrtable.put(logPath,tmoutMgr);
        return tmoutMgr;
    }
    
    static Logger _logger = LogDomains.getLogger(DelegatedRecoveryManager.class, LogDomains.TRANSACTION_LOGGER);
    
    static boolean addCoordinator(GlobalTID globalTID,
    Long localTID, CoordinatorImpl coord, int timeout, String logPath) {
        
        
        boolean result = true;
        RecoveryStateHolder state = (RecoveryStateHolder)recoveryStatetable.get(logPath);
        
        // Attempt to add the global and local indentifier to
        // Coordinator associations to the maps.
        
        state.coordsByGlobalTID.put(globalTID,coord);
        state.coordsByLocalTID.put(localTID,coord);
        
        // Set up the timeout for the transaction.  When active, the
        // timeout thread will periodically examine the map and abort
        // any active transactions on it that have gone beyond their
        // allocated time.
        
        if (timeout != 0) {
            DelegatedTimeoutManager tmoutMgr = getTimeoutManager(logPath);
            tmoutMgr.setTimeout(localTID, DelegatedTimeoutManager.ACTIVE_TIMEOUT,
            timeout);
        }
        
        return result;
    }
    
    static boolean removeCoordinator(GlobalTID globalTID,
    Long localTID, boolean aborted, String logPath) {
        
        boolean result = false;
        RecoveryStateHolder state = (RecoveryStateHolder)recoveryStatetable.get(logPath);
        
        // Remove the global identifier to Coordinator mapping if possible.
        
        CoordinatorImpl coord  = null;
        result = (state.coordsByGlobalTID.remove(globalTID) != null);
        
        // Remove the InternalTid to Coordinator mapping if possible.
        
        if (result) {
            coord = (CoordinatorImpl) state.coordsByLocalTID.remove(localTID);
            result = (coord != null);
        }
        
        // If that succeeded, forget the CoordinatorLog object, if the
        // transaction is not a subtransaction.  The following may return
        // FALSE if there are no log records available
        // (i.e. non-recoverable OTS).
        
        if (coord != null) {
            try {
                if (coord.is_top_level_transaction()) {
                    CoordinatorLog.removeLog(localTID, logPath);
                }
            } catch(SystemException exc) {
                result = false;
            }
        }
        
        // Clear the timeout for the transaction, if any.
        // Perform the removal under the timer mutex.
        
        DelegatedTimeoutManager tmoutMgr = getTimeoutManager(logPath);
        tmoutMgr.setTimeout(localTID, DelegatedTimeoutManager.CANCEL_TIMEOUT, 0);
        
        
        
        // Modify any thread associations there may be for the transaction, to
        // indicate that the transaction has ended.
        
        
        
        // COMMENT(Ram J) 09/19/2001 This below line is commented out since in
        // the J2EE controlled environment, all threads are associated and
        // dissociated in an orderly fashion, as well as there is no possibility
        // of concurrent threads active in a given transaction.
        //CurrentTransaction.endAll(globalTID, aborted);
        
        // If the count of resyncing Coordinators is greater than zero,
        // this means we are still in resync.  Decrease the count.
        
        if (state.resyncCoords > 0) {
            
            state.resyncCoords--;
            
            // If the number of resyncing Coordinators is now zero,
            // we may allow new work.
            
            if (state.resyncCoords == 0) {
                try {
                    resyncComplete(true, true, logPath);
                } catch (Throwable exc) {
                }
            }
        }
        
        return result;
    }
    
    
    static CoordinatorImpl getCoordinator(GlobalTID globalTID, String logPath) {
        
        RecoveryStateHolder state = (RecoveryStateHolder)recoveryStatetable.get(logPath);
        CoordinatorImpl result = (CoordinatorImpl)
        state.coordsByGlobalTID.get(globalTID);
        
        return result;
    }
    
    
    public static boolean delegated_recover(String logPath, XAResource[] resources) throws Exception  {
       try {
            String serverName = null;
            if (Configuration.isDBLoggingEnabled()) {
                serverName = LogDBHelper.getInstance().getServerNameForInstanceName(logPath);
                if (serverName == null) {
                    // No XA transaction had been logged on that instance
                    return true;
                }
            } else {
                File recoveryFile = new File(logPath,LogControl.RECOVERY_STRING_FILE_NAME);
                RandomAccessFile raf = new RandomAccessFile(recoveryFile,"r");
                long length = raf.length();
                byte b1[] = new byte[(int)length]; // length is very small
                raf.readFully(b1);
                serverName = new String(b1);
                raf.close();
            }
            return delegated_recover(serverName,logPath,resources);
       } catch (IOException ex) {
           _logger.log(Level.WARNING,"jts.exception_in_recovery_file_handling",ex);
           throw ex;
           // return false;
       }
    }
    public static boolean delegated_recover(String serverName, String logPath, XAResource[] resources) throws Exception {
        if (logPath == null || serverName == null) {
            return false;
        }
        Configuration.setServerName(logPath,serverName);
        if (Configuration.isDBLoggingEnabled()) {
            RecoveryManager.dbXARecovery(serverName, Collections.enumeration(Arrays.asList(resources)));
            return true;
        }

        boolean result = false;
        boolean keypointRequired = false;
        RecoveryStateHolder state = new RecoveryStateHolder();
        recoveryStatetable.put(logPath,state);
        Enumeration logRecords = CoordinatorLog.getLogged(logPath);
        while (logRecords.hasMoreElements()) {
            keypointRequired = true;
            try {
                (new TopCoordinator()).delegated_reconstruct((CoordinatorLog) logRecords.nextElement(), logPath);
            } catch(Exception exc) {
                _logger.log(Level.SEVERE,"jts.recovery_in_doubt_exception",exc);
                String msg = LogFormatter.getLocalizedMessage(_logger, "jts.recovery_in_doubt",
                new java.lang.Object[] {exc.toString()});
                throw  new org.omg.CORBA.INTERNAL(msg);
            }
        }
        
        
        int size = resources.length;
        Vector v = new Vector();
        for (int i=0; i<size; i++) {
            v.addElement(resources[i]);
        }
        state.uniqueRMSet = getUniqueRMSet(v.elements());
        proceedWithXARecovery(logPath);
        state.recoveryInProgress.post();
        
        // If resync is not needed, then perform after-resync
        // tasks immediately.
        
        result = state.coordsByGlobalTID.size() > 0;
        if (!result) {
            try {
                resyncComplete(false,keypointRequired,logPath);
            } catch(Throwable exc) {}
        }
        
        if (result)
            resync(logPath);
        return true;
    }
    
    static void resync(String logPath) {
        
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
        
        RecoveryStateHolder recoveryState = (RecoveryStateHolder)recoveryStatetable.get(logPath);
        
        recoveryState.resyncCoords = recoveryState.coordsByGlobalTID.size();
        Enumeration resyncList =
        ((Hashtable) recoveryState.coordsByGlobalTID.clone()).elements();
        
        boolean isRoot[] = new boolean[1];
        
        // Go through and resync each transaction.  The transaction lock
        // for each transaction is obtained to avoid deadlocks during recovery.
        
        FailureInducer.waitInRecovery();
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
                        
                        DelegatedTimeoutManager tmoutMgr = getTimeoutManager(logPath);
                        tmoutMgr.setTimeout(
                                coord.getLocalTID(),
                                DelegatedTimeoutManager.IN_DOUBT_TIMEOUT,
                                60);
                        
                    } else if (state == Status.StatusCommitted) {
                        
                        // For committed or rolled back, proceed with
                        // completion of the transaction, regardless of
                        // whether it is the root or a subordinate.
                        // If the transaction represents a root, it would
                        // normally wait for the CoordinatorTerm object to
                        // call before completing the transaction.  As there is
                        // no CoordinatorTerm in recovery, we must do it here.
                        if(_logger.isLoggable(Level.FINE)) {
                            _logger.logp(Level.FINE,"DelegatedRecoveryManager","resync()",
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
                            if(_logger.isLoggable(Level.FINE)) {
                                _logger.logp(Level.FINE,"DelegatedRecoveryManager","resync()",
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
    
    
    private static void resyncComplete(boolean resynced,
    boolean keypointRequired, String logPath) throws LogicErrorException {
        
        RecoveryStateHolder state = (RecoveryStateHolder)recoveryStatetable.get(logPath);
        // Inform JTSXA that resync is complete, and trace the fact
        // that resync has completed.
        
        // COMMENT(Ram J) not needed anymore
        //JTSXA.resyncComplete();
        
        // Perform a keypoint of the log if required.
        
        if (keypointRequired) {
            CoordinatorLog.keypoint(logPath);
        }
        
        // Post the resync in progress event semaphore.
        
        state.resyncInProgress.post();
        state.resyncInProgress = null;
    }
    
    /**
     * Returns a reference to the Coordinator object that corresponds to the
     * local identifier passed as a parameter.
     *
     * @param localTID  The local identifier for the transaction.
     *
     * @param logPath  log location for which the delegated recovery is done
     *
     * @return  The Coordinator object.
     *
     * @see
     */
    
    static CoordinatorImpl getLocalCoordinator(Long localTID, String logPath) {
        
        RecoveryStateHolder state = (RecoveryStateHolder)recoveryStatetable.get(logPath);
        CoordinatorImpl result = (CoordinatorImpl)
        state.coordsByLocalTID.get(localTID);
        
        return result;
    }
    
    /**
     * Determines whether the local transaction identifier represents a valid
     * transaction.
     *
     * @param localTID  The local transaction identifier to check.
     *
     * @param logPath  log location for which the delegated recovery is done
     *
     * @return  Indicates the local transaction identifier is valid.
     *
     * @see
     */
    
    static boolean validLocalTID(Long localTID, String logPath) {
        RecoveryStateHolder state = (RecoveryStateHolder)recoveryStatetable.get(logPath);
        
        boolean result = state.coordsByLocalTID.containsKey(localTID);
        
        return result;
    }
    
    /**
     * Informs the DelegatedRecoveryManager that the transaction service is being shut
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
        
        
        Enumeration keys = recoveryStatetable.keys();
        if (keys.hasMoreElements()) {
            String logPath = (String)keys.nextElement();
            RecoveryStateHolder state = (RecoveryStateHolder)recoveryStatetable.get(logPath);
            if (immediate) {
                // If immediate, stop the resync thread if any.
                
            } else {
                
                // Otherwise ensure that resync has completed.
                
                if (state.resyncInProgress != null) {
                    try {
                        state.resyncInProgress.waitEvent();
                    } catch (InterruptedException exc) {}
                }
            }
            
            // COMMENT(Ram J) not needed anymore.
            //JTSXA.shutdown(immediate);
            
            // If not immediate shutdown, keypoint and close the log.
            // Only do this if the process is recoverable!
            
            if (!immediate) {
                CoordinatorLog.keypoint(logPath);
                CoordinatorLog.finalizeAll(logPath);
            }
            
            //$Continue with shutdown/quiesce.
        }
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
     * This method is used to recontruct and register the Resource objects
     * corresponding to in-doubt transactions in the RMs. It is assumed
     * that the XAResource list has already been provided to the
     * Recovery Manager. This method can be called by Recovery Thread as
     * well as any other thread driving recovery of XA Resources.
     */
    private static void proceedWithXARecovery(String logPath) {
        
        RecoveryStateHolder state = (RecoveryStateHolder)recoveryStatetable.get(logPath);
        
        /*  This method has been newly added - Ram Jeyaraman */
        
        Enumeration xaResources = state.uniqueRMSet;
        
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
            
            Xid[] inDoubtXids = RecoveryManager.getInDoubtXids(xaResource);
            // uniqueXids.clear();
                if (inDoubtXids == null || inDoubtXids.length == 0) {
                    continue; // go to the next resource
                }
                
                for (int i = 0; i < inDoubtXids.length; i++) {
                    
                    // check to see if the xid belongs to this server.
                    
                    String branchQualifier =
                    new String(inDoubtXids[i].getBranchQualifier());
                    String serverName = Configuration.getServerName(logPath);
                    
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
                                _logger.logp(Level.FINE,"DelegatedRecoveryManager",
                                        "proceedWithXARecovery",
                                        " This xid is UNIQUE " +
                                        inDoubtXids[i]);
                            }
                            
                            uniqueXids.add(inDoubtXids[i]); // add to uniqueList
                            
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
                                    _logger.logp(Level.FINE,"DelegatedRecoveryManager",
                                            "proceedWithXARecovery",
                                            " This xid is NOTUNIQUE " +
                                            inDoubtXids[i]);
                                }
                          }
                    } else {
                            if(_logger.isLoggable(Level.FINE))
                            {
                                _logger.logp(Level.FINE,"DelegatedRecoveryManager",
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
            (TopCoordinator) state.coordsByGlobalTID.get(globalTID);
            
            if (coord == null) {
                // Roll the OTSResource back if the transaction is not
                // recognised. This happens when the RM has recorded its
                // prepare vote, but the JTS has not recorded its prepare vote.
                if(_logger.isLoggable(Level.FINE)) {
                    _logger.logp(Level.FINE,"DelegatedRecoveryManager","proceedWithXARecovery()",
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
                if(_logger.isLoggable(Level.FINE)) {
                    _logger.logp(Level.FINE,"DelegatedRecoveryManager",
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
    
    
    /**
     * Returns an array of Coordinator objects currently active.
     *
     * @param logPath  log location for which the delegated recovery is done
     *
     * @return  The array of Coordinators.
     *
     * @see
     */
    static CoordinatorImpl[] getCoordinators(String logPath) {
        
        RecoveryStateHolder state = (RecoveryStateHolder)recoveryStatetable.get(logPath);
        int size = state.coordsByGlobalTID.size();
        CoordinatorImpl[] result = new CoordinatorImpl[size];
        
        Enumeration coords = state.coordsByGlobalTID.elements();
        
        for(int pos = 0;pos<size;){
            result[pos++] = (CoordinatorImpl) coords.nextElement();
        }
        
        return result;
    }
    
    
    static Hashtable/*<GlobalTID,Coordinator>*/ getCoordsByGlobalTID(String logPath) {
        RecoveryStateHolder state = (RecoveryStateHolder)recoveryStatetable.get(logPath);
        return state.coordsByGlobalTID;
    }
    
    
    
    public static void waitForRecovery(String logPath) {
        RecoveryStateHolder state = (RecoveryStateHolder)recoveryStatetable.get(logPath);
        
        if (state.recoveryInProgress != null) {
            try {
                state.recoveryInProgress.waitEvent();
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
     * @param logPath  log location for which the delegated recovery is done
     *
     * @return
     *
     * @see
     */
    public static void waitForResync(String logPath) {
        RecoveryStateHolder state = (RecoveryStateHolder)recoveryStatetable.get(logPath);
        if (state.resyncInProgress != null) {
            try {
                state.resyncInProgress.waitEvent();
            } catch (InterruptedException exc) {
                _logger.log(Level.SEVERE,"jts.wait_for_resync_complete_interrupted");
                String msg = LogFormatter.getLocalizedMessage(_logger,
                "jts.wait_for_resync_complete_interrupted");
                throw  new org.omg.CORBA.INTERNAL(msg);
            }
        }
    }
    
}

class RecoveryStateHolder {
    
    /**
     * list of XA Resources to be recovered.
     */
    Enumeration uniqueRMSet = null;
    
    /**
     * This attribute indicates the number of Coordinator objects which require
     * resync.  This is set to the number of in-doubt transactions recovered
     * from the log, then decreased as transactions are resolved.
     */
    int resyncCoords = 0;
    
    /**
     * This attribute is used to block new requests while there are
     * Coordinators which still require resync.
     */
    EventSemaphore resyncInProgress = new EventSemaphore();
    
    /**
     * This attribute is used to block requests against RecoveryCoordinators or
     * CoordinatorResources before recovery has completed.
     */
    EventSemaphore recoveryInProgress = new EventSemaphore();
    
    Hashtable coordsByGlobalTID = new Hashtable();
    Hashtable coordsByLocalTID = new Hashtable();
    //Hashtable transactionIds = new Hashtable();
}

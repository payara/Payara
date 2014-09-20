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

package com.sun.jts.CosTransactions;

import java.util.*;

import org.omg.CosTransactions.*;
import com.sun.jts.jtsxa.XID;

import com.sun.jts.trace.*;

import java.util.logging.Logger;
import java.util.logging.Level;
import com.sun.logging.LogDomains;
import com.sun.jts.utils.LogFormatter;

/**
 * This class records state for timing out transactions, and runs a thread
 * which performs occasional checks to time out transactions. For each log
 * location, which requires delegated recovery, an instance of this will be
 * created. Done as part of delegated recovery support.
 * th
 *
 * @version 0.01
 *
 *
 * @see
 */

class DelegatedTimeoutManager {
    /**
     * Constants which define the types of timeout possible.
     */
    static final int CANCEL_TIMEOUT   = 0;
    static final int NO_TIMEOUT       = 0;
    static final int ACTIVE_TIMEOUT   = 1;
    static final int IN_DOUBT_TIMEOUT = 2;
    
    /**
     * this attribute indicates whether initialisation has been started.
     */
    private static boolean initialised = false;
    
    private Hashtable     pendingTimeouts = new Hashtable();
    private Hashtable     indoubtTimeouts = new Hashtable();
    private DelegatedTimeoutThread timeoutThread = null;
    private boolean       timeoutActive = false;
    private boolean       quiescing = false;
    private boolean       isSetTimeout = false;
    private String        logPath = null;
    
        /*
                Logger to log transaction messages
         */
    static Logger _logger = LogDomains.getLogger(DelegatedTimeoutManager.class, LogDomains.TRANSACTION_LOGGER);
    /**
     * Initialises the static state of the TimeoutManager class.
     *
     * @param
     *
     * @return
     *
     * @see
     */
    /**
     * synchronized static void initialise() {
     *
     * // If already initialised, return immediately.
     *
     * if (initialised) {
     * return;
     * }
     *
     * initialised = true;
     *
     * // Start the timeout thread.
     *
     * if (!timeoutActive && timeoutThread == null) {
     * //  timeoutThread = new TimeoutThread();
     * //  timeoutThread.start();
     * timeoutActive = true;
     * }
     * }
     **/
    
    DelegatedTimeoutManager() {
    }
    
    DelegatedTimeoutManager(String logPath) {
        this.logPath = logPath;
    }
    
    
    synchronized void initSetTimeout() {
        if (isSetTimeout)
            return;
        isSetTimeout = true;
        timeoutThread = new DelegatedTimeoutThread(this);
        timeoutThread.start();
    }
    
    /**
     * Sets the timeout for the transaction to the specified type and time in
     * seconds.
     * <p>
     * If the type is none, the timeout for the transaction is
     * cancelled, otherwise the current timeout for the transaction is modified
     * to be of the new type and duration.
     *
     * @param localTID     The local identifier for the transaction.
     * @param timeoutType  The type of timeout to establish.
     * @param seconds      The length of the timeout.
     *
     * @return  Indicates success of the operation.
     *
     * @see
     */
    boolean setTimeout(Long localTID, int timeoutType,
    int seconds) {
        
        boolean result = true;
        
        // Modify the timeout to the required type and value.
        
        DelegatedTimeoutInfo timeoutInfo = null;
        
        switch (timeoutType) {
            
            // If the new type is active or in_doubt, then create a
            // new TimeoutInfo if necessary, and set up the type and interval.
            
            case DelegatedTimeoutManager.ACTIVE_TIMEOUT :
                if (!isSetTimeout) {
                    initSetTimeout();
                }
                timeoutInfo = new DelegatedTimeoutInfo();
                timeoutInfo.expireTime  =
                new Date().getTime() + seconds * 1000L;
                timeoutInfo.localTID    = localTID;
                timeoutInfo.timeoutType = timeoutType;
                pendingTimeouts.put(localTID,timeoutInfo);
                break;
            case TimeoutManager.IN_DOUBT_TIMEOUT :
                if (!isSetTimeout) {
                    initSetTimeout();
                    // isSetTimeout = true;
                }
                timeoutInfo = new DelegatedTimeoutInfo();
                timeoutInfo.expireTime  =
                new Date().getTime() + seconds * 1000L;
                timeoutInfo.localTID    = localTID;
                timeoutInfo.timeoutType = timeoutType;
                indoubtTimeouts.put(localTID,timeoutInfo);
                break;
                
                // For any other type, remove the timeout if there is one.
                
            default:
                if (!isSetTimeout)
                    break;
                result = (pendingTimeouts.remove(localTID) != null);
                if (!result)
                    result = (indoubtTimeouts.remove(localTID) != null);
                
                // If the transaction service is quiescing and
                // there are no more pending timeouts,
                // deactivate timeout and stop the timeout thread.
                
                if (quiescing && pendingTimeouts.isEmpty() && indoubtTimeouts.isEmpty()) {
                    timeoutThread.stop();
                    timeoutActive = false;
                    // pendingTimeouts = null;
                }
                break;
        }
        return result;
    }
    
    /**
     * Takes appropriate action for a timeout.
     * <p>
     * The type fo timeout is given, and the transaction represented by the
     * Coordinator and its local identifier.
     * <p>
     * This method does not reference the TimeoutManager's state directly
     * and so does not need to be synchronized.
     *
     * @param localTID     The local identifier for the transaction.
     * @param timeoutType  The type of timeout.
     *
     * @return
     *
     * @see
     */
    void timeoutCoordinator(Long localTID, int  timeoutType) {
        
        // Look up the Coordinator for the transaction.
        // If there is none, then the transaction has already gone.
        // Otherwise do something with the transaction.
        
        
        CoordinatorImpl coord = DelegatedRecoveryManager.getLocalCoordinator(localTID, logPath);
        if (coord == null) {
            if(_logger.isLoggable(Level.FINER)) {
                _logger.logp(Level.FINER,"DelegatedTimeoutManager","timeoutCoordinator()",
                "DelegatedRecoveryManager.getLocalCoordinator() returned null,"+
                "which means txn is done. Setting timeout type to CANCEL_TIMEOUT");
            }
            setTimeout(localTID, TimeoutManager.CANCEL_TIMEOUT, 0);
        } else {
            synchronized (coord) {
                boolean[] isRoot = new boolean[1];
                
                switch (timeoutType) {
                    
                    // If active, then attempt to roll the transaction back.
                    
                    case DelegatedTimeoutManager.ACTIVE_TIMEOUT :
                        if(_logger.isLoggable(Level.FINER)) {
                            _logger.logp(Level.FINER,"DelegatedTimeoutManager","timeoutCoordinator()",
                            "DelegatedTimeoutManager.timeoutCoordinator():case ACTIVE_TIMEOUT"+
                            "DelegatedRecoveryManager.getLocalCoordinator() returned non-null,"+
                            "which means txn is still around. Rolling back the"+
                            "transaction...: GTID is : " +
                            ((TopCoordinator)coord).superInfo.globalTID.toString());
                        }
                        try {
                            // coord.rollback(true);
                            coord.rollback_only();
                        } catch (Throwable exc) {}
                        break;
                        
                        // If in doubt, it must be a TopCoordinator.
                        // In that case replay_completion needs to be driven.
                        // This is done by telling the TopCoordinator to act as
                        // if in recovery.  The result is then used to
                        // determine what to do with the Coordinator.
                        
                    case DelegatedTimeoutManager.IN_DOUBT_TIMEOUT :
                        if(_logger.isLoggable(Level.FINER)) {
                            _logger.logp(Level.FINER,"DelegatedTimeoutManager","timeoutCoordinator()",
                            "DelegatedTimeoutManager.timeoutCoordinator():case IN_DOUBT_TIMEOUT"+
                            "DelegatedRecoveryManager.getLocalCoordinator() returned non-null,"+
                            "which means txn is still around. Invoking recover(boolean)"+
                            "on TopCoordinator...: GTID is: "+
                            ((TopCoordinator)coord).superInfo.globalTID.toString());
                        }
                        Status state = ((TopCoordinator) coord).recover(isRoot);
                        
                        if (state == Status.StatusUnknown) {
                            
                            // If the outcome is not currently known, we do
                            // nothing with the transaction, as we expect to
                            // eventually get an outcome from the parent.
                            
                            // GDH put out warning in case this state
                            // continues for a long time.
                            _logger.log(Level.WARNING, "jts.transaction_resync_from_orginator_failed");
                            
                        } else if (state == Status.StatusCommitted) {
                            
                            // For committed or rolled back, proceed with
                            // completion of the transaction, regardless of whether
                            // it is the root or a subordinate. This will
                            // result in the removal of the in-doubt timeout.
                            
                            try {
                                ((TopCoordinator)coord).commit();
                                if (isRoot[0]) {
                                    ((TopCoordinator) coord).
                                    afterCompletion(state);
                                }
                            } catch (Throwable exc) {}
                        } else {
                            // By default, roll the transaction back.
                            try {
                                ((TopCoordinator) coord).rollback(true);
                                if (isRoot[0]) {
                                    ((TopCoordinator) coord).
                                    afterCompletion(Status.StatusRolledBack);
                                }
                            } catch (Throwable exc) {}
                        }
                        
                        break;
                        
                    default:
                        // Otherwise do nothing.
                        break;
                }
            }
        }
    }
    
    /**
     * Periodically checks the existing timeouts.
     * <p>
     * This is done to discover if any transactions have overrun their allotted
     * time.  Those which have are returned as an Enumeration.
     * <p>
     * Note that this method should not do anything that will cause a
     * synchronized method in the RecoveryManager to be called, as this could
     * cause a deadlock when RecoveryManager methods on other threads call
     * setTimeout.
     *
     * @param
     *
     * @return  The information for transactions which have timed out.
     *
     * @see
     */
    Enumeration checkTimeouts() {
        if (!isSetTimeout)
            return null;
        
        Enumeration result = null;
        
        // When woken up, go through all current timeouts and identify those
        // which have expired.
        
        if (timeoutActive && ((pendingTimeouts.size() != 0) || (indoubtTimeouts.size() != 0))) {
            Vector timedOut = null;
            
            Enumeration timeouts = null;
            
            synchronized (pendingTimeouts) {
                timeouts = pendingTimeouts.elements();
                
                while (timeouts.hasMoreElements()) {
                    
                    DelegatedTimeoutInfo timeoutInfo = (DelegatedTimeoutInfo)timeouts.nextElement();
                    
                    // For each timeout in the list, check whether it has expired.
                    // If so, look up the Coordinator and roll it back.
                    
                    if (new Date().getTime() > timeoutInfo.expireTime) {
                        
                        // Add the TimeoutInfo to the queue of
                        //those that have timed out.
                        
                        if (timedOut == null) {
                            timedOut = new Vector();
                        }
                        
                        timedOut.addElement(timeoutInfo);
                    }
                }
            }
            
            synchronized (indoubtTimeouts) {
                
                timeouts = indoubtTimeouts.elements();
                
                while (timeouts.hasMoreElements()) {
                    
                    DelegatedTimeoutInfo timeoutInfo = (DelegatedTimeoutInfo)timeouts.nextElement();
                    
                    // For each timeout in the list, check whether it has expired.
                    // If so, look up the Coordinator and roll it back.
                    
                    if (new Date().getTime() > timeoutInfo.expireTime) {
                        
                        // Add the TimeoutInfo to the queue of
                        //those that have timed out.
                        
                        if (timedOut == null) {
                            timedOut = new Vector();
                        }
                        
                        timedOut.addElement(timeoutInfo);
                    }
                }
                
            }
            // Enumerate the transactions which have timed out.
            
            if (timedOut != null) {
                result = timedOut.elements();
            }
        }
        
        // The remainder of the timeout processing is not carried out here
        // because we would get deadlocked with addCoordinator or
        // removeCoordinator that also update the timeout list.  Hence the
        // returned enumeration, which may be processed with
        // no concurrency control.
        
        return result;
    }
    
    /**
     * @return a set of in-doubt transaction ids.
     */
    XID[] getInDoubtXids() {
        
        synchronized (indoubtTimeouts) {
            Vector inDoubtList = new Vector();
            
            Enumeration timeouts = indoubtTimeouts.elements();
            
            while (timeouts.hasMoreElements()) {
                
                DelegatedTimeoutInfo timeoutInfo = (DelegatedTimeoutInfo) timeouts.nextElement();
                
                // Look up the Coordinator for the transaction.
                // If there is none, then the transaction has already gone.
                // Otherwise do something with the transaction.
                
                CoordinatorImpl coord =
                DelegatedRecoveryManager.getLocalCoordinator(timeoutInfo.localTID, logPath);
                
                if (coord != null) {
                    XID xid = new XID();
                    xid.copy(coord.getGlobalTID());
                    inDoubtList.addElement(xid);
                }
            }
            
            return (XID[]) inDoubtList.toArray(new XID[] {});
        }
    }
    
    /**
     * Returns the amount of time left before the given transaction times out.
     *
     * @param localTID  The local identifier for the transaction.
     *
     * @return  The time left.  If there is no timeout for the transaction,
     *          this value will be negative.  If the timeout period has been
     *          exceeded, this value will be zero.
     *
     * @see
     */
    long timeLeft(Long localTID) {
        
        DelegatedTimeoutInfo timeoutInfo = (DelegatedTimeoutInfo) pendingTimeouts.get(localTID);
        if (timeoutInfo == null)
            timeoutInfo = (DelegatedTimeoutInfo) indoubtTimeouts.get(localTID);
        long result = -1;
        if (timeoutInfo != null) {
            result = timeoutInfo.expireTime - new Date().getTime();
            if (result < 0) {
                result = 0;
            }
        }
        
        return result;
    }
    
    /**
     * Informs the TimeoutManager that the transaction service
     * is being shut down. For immediate shutdown, the timeout thread is
     * stopped and all timeout information discarded.
     *
     * For quiesce, the timeout thread is stopped when there are no running
     * transactions left.
     *
     * @param immediate  Indicates whether to stop immediately.
     *
     * @return
     *
     * @see
     */
    void shutdown(boolean immediate) {
        
        // For immediate, kill the timeout thread and throw
        // away all information. Also, if there are no pending
        // timeouts, there is nothing to quiesce so
        // shutdown immediately regardless.
        
        if (immediate ||
        pendingTimeouts == null || pendingTimeouts.isEmpty()) {
            if (timeoutThread != null) {
                timeoutThread.stop();
            }
            
            if (pendingTimeouts != null) {
                pendingTimeouts.clear();
            }
            
            pendingTimeouts = null;
            timeoutThread = null;
            timeoutActive = false;
        } else {
            quiescing = true;
        }
    }
    
}

/**
 * This class records information for a timeout for a transaction.
 *
 * @version 0.1
 *
 * @author Simon Holdsworth, IBM Corporation
 *
 * @see
 */

//----------------------------------------------------------------------------
// CHANGE HISTORY
//
// Version By     Change Description
//   0.1   SAJH   Initial implementation.
//----------------------------------------------------------------------------

class DelegatedTimeoutInfo extends Object {
    Long localTID = null;
    long expireTime = 0;
    int  timeoutType = TimeoutManager.NO_TIMEOUT;
}

/**
 * This class represents a thread on which the TimeoutManager can perform
 * timeout checking.
 *
 * @version 0.01
 *
 *
 * @see
 */


class DelegatedTimeoutThread extends Thread {
    
    private int TIMEOUT_INTERVAL ;
    private DelegatedTimeoutManager tmoutMgr = null;
    
    static Logger _logger = LogDomains.getLogger(DelegatedTimeoutThread.class, LogDomains.TRANSACTION_LOGGER);
    /**
     * TimeoutThread constructor.
     * <p>
     * This sets the thread name, and sets the thread to be a daemon thread so
     * that it does not prevent the process from terminating.
     *
     * @param
     *
     * @return
     *
     * @see
     */
    DelegatedTimeoutThread(DelegatedTimeoutManager timeoutMgr) {
        setName("Delegated JTS Timeout Thread"/*#Frozen*/);
        setDaemon(true);
        tmoutMgr = timeoutMgr;
        try{
            String timeout_interval = Configuration.getPropertyValue(Configuration.TIMEOUT_INTERVAL);
            if(timeout_interval!=null){
                TIMEOUT_INTERVAL= Integer.parseInt(timeout_interval);
                TIMEOUT_INTERVAL*=1000;
                if(TIMEOUT_INTERVAL<10000)
                    TIMEOUT_INTERVAL=10000;
            }
            else{
                TIMEOUT_INTERVAL=10000;
            }
        }catch(Exception e){
            TIMEOUT_INTERVAL=10000;
        }
    }
    
    /**
     * Performs timeout checking on a regular basis (every ten seconds or so).
     *
     * @param
     *
     * @return
     *
     * @see
     */
    public void run() {
        try {
            while (true) {
                
                // Sleep for a while between checks.
                
                Thread.sleep(TIMEOUT_INTERVAL);
                
                // Perform timeout checks, getting a list of timed-out
                // transactions.
                
                Enumeration timedOut = tmoutMgr.checkTimeouts();
                
                // Now we must go through the list, telling each
                // timed-out Coordinator to do something appropriate.
                
                if (timedOut != null) {
                    while (timedOut.hasMoreElements()) {
                        DelegatedTimeoutInfo timeoutInfo =
                        (DelegatedTimeoutInfo) timedOut.nextElement();
                        
                        // Look up the Coordinator and tell it to roll back
                        // if it still exists. Note that we rely on the
                        // Coordinator calling removeCoordinator when it
                        // has finished, which will remove the timeout from
                        // the list, and remove other associations as well.
                        
                        tmoutMgr.
                        timeoutCoordinator(timeoutInfo.localTID,
                        timeoutInfo.timeoutType);
                    }
                }
            }
        } catch (InterruptedException exc) {
            _logger.log(Level.INFO,"jts.time_out_thread_stopped");
        }
    }
}

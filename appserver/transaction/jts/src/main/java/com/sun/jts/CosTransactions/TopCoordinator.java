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
// Module:      TopCoordinator.java
//
// Description: Top-level transaction Coordinator object implementation.
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

import org.omg.CORBA.*;
import org.omg.CosTransactions.*;

import com.sun.jts.codegen.otsidl.*;
import com.sun.jts.jtsxa.OTSResourceImpl;
//import com.sun.jts.codegen.otsidl.JCoordinatorHelper;
//import com.sun.jts.codegen.otsidl.JCoordinatorOperations;
//import java.io.PrintStream;
//import java.util.Vector;

//import com.sun.enterprise.transaction.OTSResourceImpl;
//import com.sun.enterprise.transaction.SynchronizationImpl;

import com.sun.jts.trace.*;

import java.util.logging.Logger;
import java.util.logging.Level;
import com.sun.logging.LogDomains;
import com.sun.jts.utils.LogFormatter;

/**
 * The TopCoordinator interface is our implementation of the standard
 * Coordinator interface that is used for top-level transactions. It allows
 * Resources to be registered for participation in a top-level transaction.
 * In addition the TopCoordinator recovery interface can be used if the
 * connection to a superior Coordinator is lost after a transaction is
 * prepared. As an instance of this class may be accessed from multiple
 * threads within a process, serialisation for thread-safety is necessary in
 * the implementation. The information managed should be reconstructible in
 * the case of a failure.
 *
 * @version 0.02
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
//   0.02  GDH    Gordon Hutchison April 1998
//                Some improvements to the way the additional tran states in
//                OTS 1.1 (vs 1.0) are handled and also improvements to logic
//                used in recovery.
//   0.03  GDH    Gordon 27th Jan 1999
//                Changes marked by COPDEF1 to fix two defects in COP work.
//   0.04  GDH    Gordon 28th Jan 1999
//                Change marked by COPDEF2, in COP if resource voted rb
//                our code set state to COB_RB twice which is invalid:
//                first state change removed as unnecessary.
//-----------------------------------------------------------------------------

public class TopCoordinator extends CoordinatorImpl {
    String              name = null;
    RegisteredResources participants = null;
    RegisteredSyncs     synchronizations = null;
    SuperiorInfo        superInfo = null;
    NestingInfo         nestingInfo = null;
    TransactionState    tranState = null;
    CoordinatorLog      logRecord = null;
    CompletionHandler   terminator = null;
    boolean             registered = false;
    boolean             registeredSync = false;
    boolean             root = true;
    boolean             rollbackOnly = false;
    boolean             dying = false;
    boolean             temporary = false;
    int                 hash = 0;

	/*
		Logger to log transaction messages
	*/ 
	  static Logger _logger = LogDomains.getLogger(TopCoordinator.class,LogDomains.TRANSACTION_LOGGER);
    // added (Ram J) for memory Leak fix.
    Vector recoveryCoordinatorList = null;
    CoordinatorSynchronizationImpl coordSyncImpl = null;

   // added (sankar) for delegated recovery support
   boolean delegated = false;
   String logPath = null;

    /**
     * Default TopCoordinator constructor.
     *
     * @param
     *
     * @return
     *
     * @see
     */
    TopCoordinator() {
        // No persistent reference is created in this case.
    }

    /**
     * Creates and initialises a new root TopCoordinator,
     * and returns the global identifier for the transaction.
     * The timeout value, if non-zero, is used
     * to establish a time-out for the transaction. A CoordinatorLog object is
     * created at this time if the log is available.
     *
     * @param timeOut   The time-out value for the transaction.
     *
     * @return
     *
     * @exception LogicErrorException  An internal logic error occurred.
     *
     * @see
     */
    TopCoordinator(int timeOut) throws LogicErrorException {

        // If this execution of the process is recoverable, then create a
        // CoordinatorLog object for the top-level transaction. Each of the
        // implementation classes that use the CoordinatorLog have been written
        // to be able to work with or without a CoordinatorLog reference.

        if (Configuration.isRecoverable()) {
	    // get a CoordinatorLog object from the cache
	    // instead of instantiating a new one    Arun 9/27/99
	    logRecord = CoordinatorLogPool.getCoordinatorLog();
        } else {
            logRecord = null;
        }

        // Allocate a new global identifier for the transaction.

        tranState = new TransactionState(logRecord);

        // Store information about the superior, ancestors and
        // participants of the new transaction.

        superInfo = new SuperiorInfo(tranState.localTID, tranState.globalTID,
                                     null, logRecord);

        // Cache the name  - create a buffer and print the global XID into it.

        // name = superInfo.globalTID.toString();

        // Cache the hash value of the Coordinator.

        hash = superInfo.globalTID.hashCode();

        // Zero out the RegisteredResources, NestingInfo and RegisteredSyncs
        // references. These will be created when they are required.

        nestingInfo = null;
        participants = null;
        synchronizations = null;

        // Set other instance variables.

        root = true;
        registered = true;
        registeredSync = true;
        rollbackOnly = false;
        dying = false;
        temporary = false;
        terminator = null;

        if (!tranState.setState(TransactionState.STATE_ACTIVE)) {

            // Set the state of the transaction to active before making
            // it visible to the TransactionManager.

            LogicErrorException exc = new LogicErrorException(
					LogFormatter.getLocalizedMessage(_logger,
					"jts.invalid_state_change"));
            throw exc;

        } else {

            // Inform the RecoveryManager of the existence of this transaction.
            if (!RecoveryManager.addCoordinator(tranState.globalTID,
                                                tranState.localTID,
                                                this,
                                                timeOut)) {
                LogicErrorException exc = new LogicErrorException(
						LogFormatter.getLocalizedMessage(_logger,
						"jts.transaction_id_already_in_use"));
                throw exc;
            }
        }
    }

    /**
     * Creates and initialises a subordinate TopCoordinator, given the global
     * identifier and superior Coordinator reference, and returns the local
     * identifier for the transaction. The timeout value, if non-zero, is used
     * to establish a time-out for the subordinate transaction. The temporary
     * flag indicates whether the TopCoordinator was created as a temporary
     * ancestor.
     *
     * @param timeOut    The timeout value for the transaction.
     * @param globalTID  The global identifier for the transaction.
     * @param superior   The superior Coordinator.
     * @param temporary  The temporary indicator.
     *
     * @return
     *
     * @exception LogicErrorException  An internal logic error occurred.
     *
     * @see
     */
    TopCoordinator(int timeOut, GlobalTID globalTID, Coordinator superior,
            boolean temporary) throws LogicErrorException {

        // If this execution of the process is recoverable, then create a
        // CoordinatorLog object for the top-level transaction. Each of the
        // implementation classes that use the CoordinatorLog have been written
        // to be able to work with or without a CoordinatorLog reference.

        if (Configuration.isRecoverable()) {
	    // get a CoordinatorLog object from the cache
	    // instead of instantiating a new one    Arun 9/27/99
	    logRecord = CoordinatorLogPool.getCoordinatorLog();
        } else {
            logRecord = null;
        }

        // Allocate a new local identifier for the transaction.
        // If one cannot be allocated, raise an exception as the
        // transaction cannot be started.

        tranState = new TransactionState(globalTID,logRecord);

        // Store information about the superior, ancestors and participants
        // of the new subordinate transaction.

        superInfo = new SuperiorInfo(tranState.localTID, tranState.globalTID,
                                     superior, logRecord);

        // Cache the name  - create a buffer and print the global XID into it.

        // name = superInfo.globalTID.toString();

        // Cache the hash value of the Coordinator.

        hash = superInfo.globalTID.hashCode();

        // Zero out the RegisteredResources, NestingInfo and RegisteredSyncs
        // references. These will be created when they are required.

        nestingInfo = null;
        participants = null;
        synchronizations = null;

        // Set other instance variables.

        root = false;
        registered = false;
        registeredSync = false;
        rollbackOnly = false;
        dying = false;
        this.temporary = temporary;
        terminator = null;

        // Set the state of the transaction to active before making it
        // visible to the RecoveryManager.

        if (!tranState.setState(TransactionState.STATE_ACTIVE)) {
            LogicErrorException exc = new LogicErrorException(
					LogFormatter.getLocalizedMessage(_logger,
					"jts.invalid_state_change"));
            throw exc;
        } else {
            if (!RecoveryManager.addCoordinator(globalTID, tranState.localTID,
                                                this, timeOut)) {
                LogicErrorException exc = new LogicErrorException(
						LogFormatter.getLocalizedMessage(_logger,
						"jts.transaction_id_already_in_use"));
                throw exc;
            }
        }
    }

    /**
     * Cleans up the objects state.
     *
     * @param
     *
     * @return
     *
     * @see
     */
    synchronized public void doFinalize() {

        // Set the flag to indicate that the coordinator is being destroyed.

        dying = true;

        // What we do when destroyed depends on the transaction's state.
        // We assume that temporary Coordinators have rolled bak at this point.

        int state = TransactionState.STATE_ROLLED_BACK;
        if (tranState != null && !temporary) {
            state = tranState.state;
        }

        switch (state) {

        // If the transaction is active it should be rolled back.  This
        // will result in the TopCoordinator self-destructing at the
        // end of two-phase commit.

        case TransactionState.STATE_ACTIVE :
            try {
                rollback(true);
            } catch (Throwable exc) {
                _logger.log(Level.FINE, "", exc);
            }

            break;

        // For committed or rolled-back, we really need to destroy the
        // object. Also for prepared_readonly.

        case TransactionState.STATE_PREPARED_READONLY :
        case TransactionState.STATE_COMMITTED :
        case TransactionState.STATE_ROLLED_BACK :
        case TransactionState.STATE_COMMITTED_ONE_PHASE_OK :
        case TransactionState.STATE_COMMIT_ONE_PHASE_ROLLED_BACK :

            if (superInfo != null) {
                superInfo.doFinalize();
            }

            tranState = null;
            superInfo = null;
            nestingInfo = null;
            participants = null;
            synchronizations = null;
            logRecord = null;
            terminator = null;
            name = null;
            break;

        // For any other state, the transaction is completing, so the
        // TopCoordinator will eventually self-destruct.  We do nothing here.

        default :
            break;
        }
    }

    /**
     * Directs the TopCoordinator to recover its state after a failure,
     * based on the given CoordinatorLog object.
     * If the TopCoordinator has already been defined or recovered,
     *  the operation returns immediately. Otherwise the
     * TopCoordinator restores the state of its internal objects using their
     * recovery operations, which in turn recover their state from the
     * CoordinatorLog object.
     *
     * @param log  The CoordinatorLog object which contains the Coordinators
     *             state.
     * @return
     *
     * @see
     */
    synchronized void reconstruct(CoordinatorLog log) {

        // Set up instance variables.

        rollbackOnly = false;
        registered = false;
        registeredSync = false;
        root = false;
        dying = false;
        temporary = false;
        terminator = null;
        logRecord = log;
        name = null;

        // Zero out NestingInfo and Synchronizations references. These won't be
        // needed for a recovered transaction.

        nestingInfo = null;
        synchronizations = null;

        // Use the result of the TransactionState reconstruction to
        // decide whether to continue with recovery of this transaction.

        tranState = new TransactionState();
        int state = tranState.reconstruct(log);
        if (state == TransactionState.STATE_NONE ||
                state == TransactionState.STATE_COMMITTED ||
                // state == TransactionState.STATE_COMMITTED_ONE_PHASE_OK ||
                // state == TransactionState.STATE_COMMIT_ONE_PHASE_ROLLED_BACK ||
                state == TransactionState.STATE_ROLLED_BACK) {

            // If the transaction is discarded, then ensure that
            // the log record is discarded.

            CoordinatorLog.removeLog(log.localTID);
            destroy();

        } else {

            // Otherwise continue with reconstruction.
            participants = new RegisteredResources(this);
            participants.reconstruct(log);

            // Reconstruct the SuperiorInfo object.  This will result in a
            // call to RecoveryManager.addCoordinator (which is done
            // because reconstruction of the object references in the
            // SuperiorInfo requires the Coordinator to
            // already be known to the RecoveryManager).

            superInfo = new SuperiorInfo();
            superInfo.reconstruct(log, this);

            // Cache the name  - create a buffer and print the
            // global XID into it.

            name = superInfo.globalTID.toString();

            // Cache the hash value of the Coordinator.

            hash = superInfo.globalTID.hashCode();
        }
    }

    /**
     * Directs the TopCoordinator to recover its state after a failure,
     * based on the given CoordinatorLog object for the given logpath.
     * If the TopCoordinator has already been defined or recovered,
     *  the operation returns immediately. Otherwise the
     * TopCoordinator restores the state of its internal objects using their
     * recovery operations, which in turn recover their state from the
     * CoordinatorLog object.
     *
     * @param log  The CoordinatorLog object which contains the Coordinators
     *             state.
     *
     * @param logPath  Location of the log file
     * @return
     *
     * @see
     */
    synchronized void delegated_reconstruct(CoordinatorLog log, String logPath ) {

        // Set up instance variables.

        rollbackOnly = false;
        registered = false;
        registeredSync = false;
        root = false;
        dying = false;
        temporary = false;
        terminator = null;
        logRecord = log;
        name = null;

        // Zero out NestingInfo and Synchronizations references. These won't be
        // needed for a recovered transaction.

        nestingInfo = null;
        synchronizations = null;

        delegated = true;
        this.logPath = logPath;

        // Use the result of the TransactionState reconstruction to
        // decide whether to continue with recovery of this transaction.

        tranState = new TransactionState();
        // int state = tranState.delegated_reconstruct(log);
        int state = tranState.reconstruct(log);
        if (state == TransactionState.STATE_NONE ||
                state == TransactionState.STATE_COMMITTED ||
                // state == TransactionState.STATE_COMMITTED_ONE_PHASE_OK ||
                // state == TransactionState.STATE_COMMIT_ONE_PHASE_ROLLED_BACK ||
                state == TransactionState.STATE_ROLLED_BACK) {

            // If the transaction is discarded, then ensure that
            // the log record is discarded.

            CoordinatorLog.removeLog(log.localTID, logPath);
            destroy();

        } else {

            // Otherwise continue with reconstruction.
            participants = new RegisteredResources(this);
            participants.reconstruct(log);

            // Reconstruct the SuperiorInfo object.  This will result in a
            // call to RecoveryManager.addCoordinator (which is done
            // because reconstruction of the object references in the
            // SuperiorInfo requires the Coordinator to
            // already be known to the RecoveryManager).

            superInfo = new SuperiorInfo();
            superInfo.delegated_reconstruct(log, this, logPath);

            // Cache the name  - create a buffer and print the
            // global XID into it.

            name = superInfo.globalTID.toString();

            // Cache the hash value of the Coordinator.

            hash = superInfo.globalTID.hashCode();
        }
    }

    /**
     * Directs the TopCoordinator to perform recovery actions based on its
     * reconstructed state after a failure, or after an in-doubt timeout has
     * occurred.
     * This method is called by the RecoveryManager during recovery, in
     * which case there is no terminator object, or during normal operation
     * if the transaction  commit retry interval has been
     * exceeded for the transaction.
     * If this method is called more times than the retry limit specified in
     * COMMITRETRY, then the global outcome of the transaction is taken from
     * the value of HEURISTICDIRECTION.
     *
     * @param isRoot  A 1-element array which will be filled in with the
     *                root flag.
     *
     * @return  The state of the recovered transaction.
     *
     * @see
     */
    synchronized Status recover(boolean[/*1*/] isRoot) {

        Status result;

        // Determine the global outcome using the transactions state for a root
        // Coordinator, or the RecoveryCoordinator for a subordinate.

        if (superInfo.recovery != null) {

            // For a subordinate, first check whether the global
            // outcome is known locally.


            // GDH COP For the commit_one_phase operations we need to do the
            //         following ultimately. However for all c-o-p operations
            //         We know that the CLIENT/Superior chose to COMMIT.
            //         Also for all c-o-p operations that are  'past tense'
            //         the direction (commit or rolled back) is not really
            //         important as we are using c-o-p for single resources
            //         not last agent in CORBA CosTransactions.
            //
            // For clarity, all c-o-p states return a commited direction,
            // This is counter intuative but logicaly correct (unimportant)
            // even for COMMIT_ONE_PHASE_ROLLED_BACK.
            // A well behaved resource will not contact us in any of the
            // 'past tense' c-o-p states anyway as they have already returned
            // from a c-o-p op and can expect no further flows
            // (apart from forget perhaps).
            // When it comes to real resource flows we must be careful to
            // cause the following actions based on state:
            //
            // STATE_COMMITTING_ONE_PHASE
            // (We only ever enter this state if we have one resource
            // even if the c-o-p method was called on our CoordinatorResource)
            // The transaction was partway through a commit_one_phase
            // operation when the server failed.
            // So the commit_one_phase needs to be called again.
            // STATE COMMITTED_ONE_PHASE
            // STATE COMMITTED_ONE_PHASE_ROLLEDBACK
            // The transaction had just completed a commit_one_phase operation.
            // Therefore all of the work for the downstream part of the
            // transaction is over.  The only work to do is to possibly report
            // outcome to superior.
            // STATE COMMIT_ONE_PHASE_HEURISTIC_MIXED
            // STATE COMMIT_ONE_PHASE_HEURISTIC_HAZARD
            // Part of the tree has made a heuristic decision.  The forget
            // message must flow to all subordinate coordinators to allow them
            // to end.

            switch (tranState.state) {

            // GDH Due to the possibility of recovery being attempted
            // on more than one thread we must cover the case where
            // the transaction has actually COMMITTED already.

            case TransactionState.STATE_COMMITTED :
                // GDH (added)
            case TransactionState.STATE_COMMITTED_ONE_PHASE_OK :
                // GDH (added)
            case TransactionState.STATE_COMMITTING_ONE_PHASE :
                // GDH (added)
            case TransactionState.STATE_COMMIT_ONE_PHASE_ROLLED_BACK :
                // GDH (added)
            case TransactionState.STATE_COMMITTING :
                result = Status.StatusCommitted;
                break;

            // GDH Due to the possibility of recovery being attempted
            // on more than one thread we must cover the case where
            // the transaction has actually ROLLED_BACK already.

            case TransactionState.STATE_ROLLED_BACK :  // GDH (added)
            case TransactionState.STATE_ROLLING_BACK :
                // GDH Note we do not need C-O-P_ROLLED_BACK Here as the actual
                // resource rolling back will be done already so it's academic.
                result = Status.StatusRolledBack;
                break;

            // For a subordinate, the replay_completion method is invoked on
            // the superior's RecoveryCoordinator.  We may need to create a
            // CoordinatorResource object to give to the superior in the case
            // where we are in recovery. If the number of times
            // the replay_completion has bee retried is greater than the value
            // specified by COMMITRETRY, then HEURISTICDIRECTION is used
            // to determine the transaction outcome.

            default :

                boolean attemptRetry = true;
                // String commitRetryVar;
                // int commitRetries = 0;

                // If COMMITRETRY is not set, then retry is infinite.
                // Otherwise check that
                // the current number of retries is less than the limit.

               /**
                commitRetryVar = Configuration.
                    getPropertyValue(Configuration.COMMIT_RETRY);
                if (commitRetryVar != null) {
                    try {
                        commitRetries = Integer.parseInt(commitRetryVar);
                    } catch( NumberFormatException exc ) {}

                    if (superInfo.resyncRetries() >= commitRetries) {
                        attemptRetry = false;
                    }
                }
                **/
                int commitRetries = Configuration.getRetries();
                if (commitRetries >= 0 && (superInfo.resyncRetries() >= commitRetries))
                    attemptRetry = false;


                if (!attemptRetry) {

                    // If we are not to attempt a retry of the
                    // replay_completion method, then the HEURISTICDIRECTION
                    // environment variable  is used to get the global outcome.

                    String heuristicVar;
                    boolean commitTransaction = false;
                    result = Status.StatusRolledBack;

                    heuristicVar =
                        Configuration.getPropertyValue(
                            Configuration.HEURISTIC_DIRECTION);

                    if (heuristicVar != null) {
                        commitTransaction = (heuristicVar.charAt(0) == '1');
                    }

                    if (commitTransaction) {
                        result = Status.StatusCommitted;
                    }

                } else {

                    // Otherwise, use the RecoveryCoordinator to get
                    // the global outcome. Get the global outcome
                    // from the superior's RecoveryCoordinator.

                    try {
						if(_logger.isLoggable(Level.FINE))
                    	{
							_logger.logp(Level.FINE,"TopCoordinator","recover",
									"Before invoking replay_completion on Superior Coordinator");
                    	}
                        if (!delegated) {
                            result = superInfo.recovery.
                                        replay_completion(superInfo.resource);
                        } else {
                            result = ((RecoveryCoordinatorImpl)(superInfo.recovery)).
                                        replay_completion(superInfo.resource, logPath);
                        }

                        // GDH
                        // If the global result is returned as COMMITTING we
                        // know the outcome of the global transaction
                        // is COMMITTED.

                        if (result == Status.StatusCommitting) {
                            result = Status.StatusCommitted;
                        }
                    } catch (Throwable exc) {
                        _logger.log(Level.FINE, "", exc);
                        // If the exception is neither TRANSIENT or
                        // COMM_FAILURE, it isunexpected, so display a message
                        // and assume that the transaction has rolled back.

                        if (!(exc instanceof COMM_FAILURE) &&
                                !(exc instanceof TRANSIENT)) {
                            result = Status.StatusRolledBack;
                        } else {
                            // For TRANSIENT or COMM_FAILURE, the outcome
                            // is unknown.
                            result = Status.StatusUnknown;
                        }
                    }
                }

                break;
            }

            // Clear the root Coordinator flag to indicate that
            // this is not the root.

            root = false;

        } else {

            // For a top-level Coordinator, we will generally only
            // recover in the case where we have successfully prepared.
            // If the state is not prepared_success,
            // then assume it is rollback.

            if (tranState.state == TransactionState.STATE_PREPARED_SUCCESS) {
                result = Status.StatusCommitted;
            } else {
                result = Status.StatusRolledBack;
            }

            // Set the root Coordinator flag to indicate that this is the root.

            root = true;
        }

        isRoot[0] = root;

        return result;
    }

    /**
     * Returns the local status of the target transaction.
     *
     * @param
     *
     * @return  The status of the transaction.
     *
     * @see
     */
    public Status get_status() {

        Status result = Status.StatusUnknown;

        if (tranState != null) {

            switch (tranState.state) {

            // If active, return active or marked rollback-only
            // if the flag is set.

            case TransactionState.STATE_ACTIVE :
                if( rollbackOnly )
                    result = Status.StatusMarkedRollback;
                else
                    result = Status.StatusActive;
                break;

            // If prepared, (successfully or otherwise), return prepared.
            // If committing return prepared (may want to block in this case).

            case TransactionState.STATE_PREPARED_SUCCESS :
            case TransactionState.STATE_PREPARED_FAIL :
            case TransactionState.STATE_PREPARED_READONLY :
                result = Status.StatusPrepared;
                break;

            // If we have no internal state, return that fact.
            // All of these states map directly to the OMG values.

            case TransactionState.STATE_NONE :
                result = Status.StatusNoTransaction;
                break;
            case TransactionState.STATE_PREPARING :
                result = Status.StatusPreparing;
                break;
            case TransactionState.STATE_COMMITTING :
            case TransactionState.STATE_COMMITTING_ONE_PHASE :
                result = Status.StatusCommitting;
                break;
            case TransactionState.STATE_COMMITTED :
            case TransactionState.STATE_COMMITTED_ONE_PHASE_OK :
            case TransactionState.STATE_COMMIT_ONE_PHASE_HEURISTIC_HAZARD :
            case TransactionState.STATE_COMMIT_ONE_PHASE_HEURISTIC_MIXED :
                result = Status.StatusCommitted;
                break;
            case TransactionState.STATE_ROLLING_BACK :
            case TransactionState.STATE_COMMIT_ONE_PHASE_ROLLED_BACK :
                result = Status.StatusRollingBack;
                break;
            case TransactionState.STATE_ROLLED_BACK :
                result = Status.StatusRolledBack;
                break;

            // Any other state, return unknown.
            // GDH Including c-o-p heuristic states

            default :
                result = Status.StatusUnknown;
                break;
            }
        } else {
            INVALID_TRANSACTION exc = new INVALID_TRANSACTION(
                                            MinorCode.Completed,
                                            CompletionStatus.COMPLETED_NO);
          throw exc;
        }

        return result;
    }

    /**
     * Gets the local state of the transaction.
     * For a top-level transaction this operation is equivalent
     * to the get_status method.
     * This operation references no instance variables and so can be
     * implemented locally in the proxy class.
     *
     * @param
     *
     * @return  The status of the transaction.
     *
     * @see
     */
    public Status get_parent_status() {
        Status result = get_status();
        return result;
    }

    /**
     * Gets the local state of the transaction.
     * For a top-level transaction this operation is equivalent
     * to the get_status method.
     * This operation references no instance variables and so can be
     * implemented locally in a proxy class.
     *
     * @param
     *
     * @return  The status of the transaction.
     *
     * @see
     */
    public Status get_top_level_status() {

        Status result = get_status();
        return result;
    }

    /**
     * Compares the given Coordinator object with the target,
     * and returns TRUE if they represent the same transaction.
     * This operation needs to be implemented in an efficient manner,
     * without any cross-process calls. This could be achieved by
     * including the global identifier in the Coordinator references
     * and comparing them.
     * This operation references no instance variables and so can be
     * implemented locally in a proxy class.
     *
     * @param other  The other Coordinator to be compared.
     *
     * @return  Indicates equality of the transactions the objects
     *   represent.
     *
     * @exception SystemException  The other Coordinator could not be reached.
     *
     * @see
     */
/**
	removed synchronization at method level since only tranState requires
	locking
*/
    public boolean is_same_transaction(Coordinator other)
            throws SystemException {

        boolean result = false;

        // Get the names of the two transactions and compare them.

        if (tranState != null) {
            if (name == null)
                name = superInfo.globalTID.toString();
            result = name.equals(other.get_transaction_name());
        } else {
            INVALID_TRANSACTION exc = new INVALID_TRANSACTION(
                                            MinorCode.Completed,
                                            CompletionStatus.COMPLETED_NO);
            throw exc;
        }

        return result;
    }

    /**
     * Determines whether the target TopCoordinator is related to
     * the given Coordinator (i.e. is a member of the same transaction family).
     * For a top-level transaction returns TRUE if and only if
     * the transaction associated with the parameter object is a
     * descendant of the transaction associated with the target object.
     * This operation references no instance variables and so can be
     * implemented locally in a proxy class.
     *
     * @param other  The other Coordinator.
     *
     * @return  Indicates the relationship.
     *
     * @exception SystemException  The other Coordinator could not be reached.
     *
     * @see
     */
    public boolean is_related_transaction(Coordinator other)
            throws SystemException {

        boolean result = false;

        if (tranState != null) {
            result = other.is_descendant_transaction(this.object());
        } else {
            INVALID_TRANSACTION exc = new INVALID_TRANSACTION(
                                            MinorCode.Completed,
                                            CompletionStatus.COMPLETED_NO);
            throw exc;
        }

        return result;
    }

    /**
     * Determines whether this TopCoordinator is the root TopCoordinator.
     * the given Coordinator (i.e. is a member of the same transaction family).
     * For a root transaction, this method returns TRUE. Otherwise it
     * returns FALSE.
     *
     * @return  Indicates if this is the root TopCoordinator.
     *
     * @see
     */
    public boolean is_root_transaction() {

        boolean result = root;

        return result;
    }

    /**
     * Determines whether the target TopCoordinator is an ancestor
     * of the given Coordinator.
     * For a top-level transaction returns TRUE if and only if
     * the transaction associated with the target object is an ancestor
     * of the transaction associated with the parameter object.
     * This operation references no instance variables and so can be
     * implemented locally in a proxy class.
     *
     * @param other  The other Coordinator.
     *
     * @return  Indicates the relationship.
     *
     * @exception SystemException  The other Coordinator could not be reached.
     *
     * @see
     */
    public boolean is_ancestor_transaction(Coordinator other)
            throws SystemException {

        boolean result = false;
        if (tranState != null) {
          result = other.is_descendant_transaction(this.object());
        } else {
            INVALID_TRANSACTION exc = new INVALID_TRANSACTION(
                                            MinorCode.Completed,
                                            CompletionStatus.COMPLETED_NO);
            throw exc;
        }

        return result;
    }

    /**
     * Determines whether the target TopCoordinator is a descendant
     * of the given Coordinator.
     * For a top-level transaction returns TRUE if and only if
     * the transaction associated with the target object is the same as
     * the transaction associated with the parameter object.
     * This operation references no instance variables and so can be
     * implemented locally in a proxy class.
     *
     * @param other  The other Coordinator.
     *
     * @return  Indicates the relationship.
     *
     * @exception SystemException  The other Coordinator could not be reached.
     *
     * @see
     */
    public boolean is_descendant_transaction(Coordinator other)
            throws SystemException {

        boolean result = false;
        if (tranState != null) {
            result = is_same_transaction(other);
        } else {
            INVALID_TRANSACTION exc = new INVALID_TRANSACTION(
                                            MinorCode.Completed,
                                            CompletionStatus.COMPLETED_NO);
            throw exc;
        }

        return result;
    }

    /**
     * Determines whether the target TopCoordinator represents a top-level
     * (non-nested) transaction.
     * <p>
     * For a top-level transaction returns TRUE.
     * <p>
     * This operation references no instance variables and so can be
     * implemented locally in a proxy class.
     *
     * @param
     *
     * @return  Indicates this is a top-level transaction.
     *
     * @see
     */
    public boolean is_top_level_transaction() {

        boolean result = true;
        return result;
    }

    /**
     * Returns a hash value based on the transaction associated with the target
     * object.
     * This operation references only the global TID, and so can be
     * implemented locally in a proxy class.
     *
     * @param
     *
     * @return  The hash value for the transaction.
     *
     * @see
     */
/**
	removed synchronization at method level since only tranState requires
	locking
*/
    public int hash_transaction() {

        int result = hash;

        if (tranState == null) {
            INVALID_TRANSACTION exc = new INVALID_TRANSACTION(
                                            MinorCode.Completed,
                                            CompletionStatus.COMPLETED_NO);
            throw exc;
        }

        return result;
    }

    /**
     * Returns a hash value based on the top-level ancestor of the transaction
     * associated with the target object.
     * This operation references only the global TID, and so can be
     * implemented locally in a proxy class.
     *
     * @param
     *
     * @return  The hash value for the transaction.
     *
     * @see
     */
    synchronized public int hash_top_level_tran() {

        int result = hash;

        if (tranState == null) {
            INVALID_TRANSACTION exc = new INVALID_TRANSACTION(
                                            MinorCode.Completed,
                                            CompletionStatus.COMPLETED_NO);

            throw exc;
        }

        return result;
    }

    /**
     * Enables a Resource to be registered as a participant in the completion
     * of the top-level transaction represented by the TopCoordinator.
     * If the TopCoordinator is a subordinate, and has not registered with its
     * superior, it creates a CoordinatorResource and registers it. The
     * RecoveryCoordinator that is returned is stored in the SuperiorInfo.
     *
     * @param res  The Resource to be registered.
     *
     * @return  The RecoveryCoordinator object from the
     *   registration with the top-level ancestor.
     *
     * @exception Inactive  The Coordinator is completing the transaction and
     *   cannot accept this registration.
     * @exception TRANSACTION_ROLLEDBACK  The transaction which the Coordinator
     *   represents has already been rolled back, or has been marked
     *   rollback-only.
     *
     * @see
     */
    synchronized public RecoveryCoordinator register_resource(Resource res)
            throws Inactive, TRANSACTION_ROLLEDBACK {

        RecoveryCoordinator result = null;

        // First check the state of the transaction. If it is not active,
        // do not allow the registration.

        if (tranState == null || tranState.state !=
                TransactionState.STATE_ACTIVE) {
          Inactive exc = new Inactive();
          throw exc;
        }

        // Check whether the transaction has been marked rollback-only.

        if (rollbackOnly) {
            TRANSACTION_ROLLEDBACK exc =
                new TRANSACTION_ROLLEDBACK(0, CompletionStatus.COMPLETED_NO);
            throw exc;
        }

        // If not previously registered, a CoordinatorResource object must be
        // registered with our superior.  Note that root TopCoordinators are
        // created with the registration flag set, so we do not need to
        // check whether we are the root TopCoordinator here.

        if (!registered && DefaultTransactionService.isORBAvailable()) {

            // Initialise the CoordinatorResource with the local id,
            // our reference, and a flag to indicate that it does not
            // represent a subtransaction.

            CoordinatorResourceImpl cImpl =
                new CoordinatorResourceImpl(superInfo.globalTID, this, false);

            try {

                // Register the CoordinatorResource with the superior
                // Coordinator, and store the resulting RecoveryCoordinator
                // reference.

                CoordinatorResource cRes = cImpl.object();
                RecoveryCoordinator superRecovery =
                    superInfo.superior.register_resource(cRes);
		if (!(superRecovery instanceof TxInflowRecoveryCoordinator))
                    superInfo.setRecovery(superRecovery);
                superInfo.setResource(cRes);
                registered = true;
				if(_logger.isLoggable(Level.FINEST))
                {
                    _logger.logp(Level.FINEST,"TopCoordinator","register_resource()",
							"CoordinatorResource " + cImpl + 
							" has been registered with (Root)TopCoordinator"+
							superInfo.globalTID.toString());
                }

            } catch (Exception exc) {

                // If an exception was raised, do not store the
                // RecoveryCoordinator or set the registration flag.
                // Throw an internal exception.

                cImpl.destroy();

                if (exc instanceof OBJECT_NOT_EXIST) {

                    // If the exception is a system exception, then allow it
                    // to percolate to the caller.
                    TRANSACTION_ROLLEDBACK ex2 =
                        new TRANSACTION_ROLLEDBACK(
                            0, CompletionStatus.COMPLETED_NO);
                    ex2.initCause(exc);
                    throw ex2;
                }

                if (exc instanceof Inactive)  {
                    throw (Inactive)exc;
                }

                if (exc instanceof SystemException) {
                    throw (SystemException)exc;
                }

                // Otherwise throw an internal exception.

                INTERNAL ex2 = new INTERNAL(MinorCode.NotRegistered,
                                            CompletionStatus.COMPLETED_NO);
                ex2.initCause(exc);
                throw ex2;
            }
        }

        // If the set has not already been created, create it now.

        if (participants == null) {
            participants = new RegisteredResources(logRecord, this);
        }

        // Add a duplicate of the reference to the set.  This is done
        // because if the registration is for a remote object, the proxy
        // will be freed when the registration request returns.

        // COMMENT(Ram J) if the res object is a local servant, there is
        // no proxy involved. Also, the instanceof operator could be replaced
        // by a is_local() method if this class implements the CORBA local
        // object contract.
        int numRes = 0;
        if (res instanceof OTSResourceImpl) {
            numRes = participants.addRes(res);
		    if(_logger.isLoggable(Level.FINEST))
            {
				_logger.logp(Level.FINEST,"TopCoordinator","register_resource()",
						"OTSResource " + res +" has been registered"+"GTID is:"+
						superInfo.globalTID.toString());
            }

        } else {
            numRes = participants.addRes((Resource)res._duplicate());
        }

        temporary = false;

        // Create, initialise and return a RecoveryCoordinator
        // object to the caller.

        // COMMENT(Ram J) a RecoveryCoordinator object need not be
        // created for local resources.
        if (!(res instanceof OTSResourceImpl)) {
            RecoveryCoordinatorImpl rcImpl = null;
            try {
                rcImpl = new RecoveryCoordinatorImpl(
                                            superInfo.globalTID, numRes);
                result = rcImpl.object();
            } catch (Exception exc) {

                // If the RecoveryCoordinator could not be created,
                // report the exception.

                INTERNAL ex2 = new INTERNAL(MinorCode.RecCoordCreateFailed,
                                            CompletionStatus.COMPLETED_NO);
                ex2.initCause(exc);
                throw ex2;
            }

            // ADD(Ram J) memory leak fix. All Recovery Coordinators need
            // to be cleanedup when the transaction completes.
            if (recoveryCoordinatorList == null) {
                recoveryCoordinatorList = new Vector();
            }
            recoveryCoordinatorList.add(rcImpl);
        }

        return result;
    }

    /**
     * Enables a SubtransactionAwareResource to be registered as a
     * participant in the completion of a subtransaction.
     * For a top-level transaction this raises the NotSubtransaction exception.
     *
     * @param sares  The SubtransactionAwareResource to be registered.
     *
     * @return
     *
     * @exception NotSubtransaction  The Coordinator represents a top-level
     *   transaction and cannot accept the registration.
     *
     * @see
     */
    synchronized public void register_subtran_aware(
            SubtransactionAwareResource sares) throws NotSubtransaction {

        NotSubtransaction exc = new NotSubtransaction();
        throw exc;
    }

    /**
     * Ensures that the transaction represented by the target TopCoordinator
     * cannot be committed.
     *
     * @param
     *
     * @return
     *
     * @exception Inactive  The Coordinator is already completing the
     *                      transaction.
     * @see
     */
     public void rollback_only() throws Inactive {

        if (tranState == null ||
                tranState.state != TransactionState.STATE_ACTIVE) {
            Inactive exc = new Inactive();
            throw exc;
        } else {
            // Set the rollback-only flag.
            rollbackOnly = true;
        }
    }

    /**
     * Returns a printable string that represents the TopCoordinator.
     * This operation references only the global TID, and so can be
     * implemented locally in a proxy class.
     *
     * @param
     *
     * @return  The transaction name.
     *
     * @see
     */
/**
	removed synchronization at method level since only tranState requires
	locking
*/
    public String get_transaction_name() {

        String result = null;
        if (tranState != null) {
            if (name == null)
                name = superInfo.globalTID.toString();
            result = name;
        } else {
            INVALID_TRANSACTION exc = new INVALID_TRANSACTION(
                                            MinorCode.Completed,
                                            CompletionStatus.COMPLETED_NO);
            throw exc;
        }

        return result;
    }

    /**
     * Creates a subtransaction and returns a Control object that represents
     * the child transaction.
     *
     * @param
     *
     * @return  The Control object for the new child transaction.
     *
     * @exception Inactive  The Coordinator is completing the subtransaction
     *                      and cannot create a new child.
     *
     * @see
     */
    synchronized public Control create_subtransaction() throws Inactive {

        Control result = null;

        // First check the state of the transaction. If it is not active,
        // do not allow the subtransaction to be created.

        if (tranState == null ||
                tranState.state != TransactionState.STATE_ACTIVE) {
            Inactive exc = new Inactive();
            throw exc;
        }

        // Set up the sequence of ancestors to hold the single reference
        // and global identifier of the top-level
        // TopCoordinator as there are no ancestors.
        // We do not need to make a copy of the global TID as this is done
        // by the factory when it creates the child.

        CoordinatorImpl[] ancestors = new CoordinatorImpl[1];
        ancestors[0] = this;

        // Create a new SubCoordinator, and initialise it with the given
        // identifiers and ancestry.  If the operation fails,
        // return a NULL Control object, and
        // the SubtransactionsUnavailable exception. Note that the
        // ancestor sequence is not copied by the creation operation.

        SubCoordinator child = null;
        TerminatorImpl terminator = null;
        try {
            child = new SubCoordinator(superInfo.globalTID,
                                       superInfo.localTID,
                                       ancestors);

            // Create a Terminator object, and initialise it with the
            // SubCoordinator reference and a flag to indicate that it
            // represents a subtransaction.

            terminator = new TerminatorImpl(child,true);

            // Create a Control object, and initialise it with the Terminator,
            // SubCoordinator and global OMGtid.

            result = new ControlImpl(terminator, child,
                                     new GlobalTID(child.getGlobalTID()),
                                     child.getLocalTID()
                                    ).object();
        } catch (Throwable exc) {
            Inactive ex2 = new Inactive();
            ex2.initCause(exc);
            throw ex2;
        }

        // If the operation succeeded, add the new child to the set
        // of children. Ensure that the NestingInfo object is set up.

        if (nestingInfo == null) {
            nestingInfo = new NestingInfo();
        }

        nestingInfo.addChild(child);

        return result;
    }

    /**
     * Returns a global identifier that represents the TopCoordinator's
     * transaction. <p>
     * This operation references only the global identifier, and so can be
     * implemented locally in a proxy class.
     * <p>
     * This method is currently not synchronized because that causes a deadlock
     * in resync.  I don't think this is a problem as the global identifier is
     * allocated in the constructor and then never changes.
     *
     * @param
     *
     * @return  The global transaction identifier.
     *
     * @see
     */
    public otid_t getGlobalTID() {

        otid_t result = superInfo.globalTID.realTID;
        return result;
    }

    public GlobalTID getGlobalTid() {
        return superInfo.globalTID;
    }

    public int getParticipantCount() {
        if (participants == null) {
            return 0;
        }
        return participants.numRegistered();
    }
    
    /**
     * Returns the internal identifier for the transaction.
     * This method is currently not synchronized because that causes a deadlock
     * in resync.
     *
     * @param
     *
     * @return  The local identifier.
     *
     * @see
     */
    public long getLocalTID() {

        long result = superInfo.localTID.longValue();
        return result;
    }

    /**
     * Indicates that a method reply is being sent and requests
     * the TopCoordinator's action.
     * If the Coordinator has active children, which are not
     * registered with their superior (includes root Coordinators)
     *  then this method returns activeChildren.
     * If it has already been registered, the method returns doNothing.
     * Otherwise the TopCoordinator returns forgetMe.
     *
     * @param action  A 1-element array to hold the reply action.
     *
     * @return  The parent coordinator if any.
     *
     * @exception SystemException  An error occurred. The minor code indicates
     *                             the reason for the exception.
     *
     * @see
     */
    synchronized CoordinatorImpl replyAction(int[/*1*/] action)
            throws SystemException {

        CoordinatorImpl result = null;
        action[0] = CoordinatorImpl.doNothing;

        // If this Coordinator is not a root, and there are active children,
        // report that fact to the caller. If the NestingInfo instance variable
        // has not been set up, there are no children.

        if (!root && nestingInfo != null && nestingInfo.replyCheck()) {

            action[0] = CoordinatorImpl.activeChildren;

            // If there are no active children, then check whether this
            // transaction needs to be destroyed, or registered on reply.

        } else {

            // If there are participants, and we have not registered,
            // raise an exception.

            if (!registered) {
                if (participants != null && participants.involved()) {

                    INTERNAL ex2 = new INTERNAL(MinorCode.NotRegistered,
                                                CompletionStatus.COMPLETED_NO);
                    throw ex2;
                } else if (!registeredSync) {
                    action[0] = forgetMe;
                }
            }

            // If there are synchronization objects, and we have not
            // registered, raise an exception.

            if (!registeredSync) {
                if (synchronizations != null && synchronizations.involved()) {

                    INTERNAL ex2 = new INTERNAL(MinorCode.NotRegistered,
                                                CompletionStatus.COMPLETED_NO);
                    throw ex2;
                } else if (action[0] == doNothing && !registered) {
                    // If we are not registered, and have no participants,
                    // we have no reason to exist, so tell the caller to
                    // forget about us. The TransactionManager will take
                    // care of cleaning everything else up when
                    // it receives the forgetMe response.
                    action[0] = forgetMe;
                }
            }
        }

        // Default action is do nothing when we are registered.

        result = null;

        return result;
    }

    /**
     * Marks the TopCoordinator as permanent.
     *
     * @param
     *
     * @return  The local transaction identifier.
     *
     * @see
     */
    synchronized Long setPermanent() {

        Long result = superInfo.localTID;
        temporary = false;
        return result;
    }

    /**
     * Checks whether the TopCoordinator is marked rollback-only.
     *
     * @param
     *
     * @return  Indicates whether the transaction is rollback-only.
     *
     * @see
     */
    synchronized public boolean isRollbackOnly() {

        boolean result = rollbackOnly;
        return result;
    }

    /**
     * Checks whether the TopCoordinator is active.
     *
     * @param
     *
     * @return  Indicates the transaction is active.
     *
     * @see
     */
    synchronized boolean isActive() {

        boolean result = (tranState.state == TransactionState.STATE_ACTIVE);
        return result;
    }

    /**
     * Checks whether the TopCoordinator has registered with its superior.
     *
     * @param
     *
     * @return  Indicates the registration status.
     *
     * @see
     */
    synchronized boolean hasRegistered() {

        boolean result = registered || registeredSync;
        return result;
    }

    /**
     * Returns the sequence of ancestors of the transaction.
     *
     * @param
     *
     * @return  The sequence of ancestors.
     *
     * @see
     */
    synchronized public TransIdentity[] getAncestors() {
        return null;
    }

    /**
     * Adds the given Coordinator reference to the set of children of the
     * target TopCoordinator.
     *
     * @param child  The child Coordinator.
     *
     * @return  Indicates success of the operation.
     *
     * @see
     */
    synchronized boolean addChild(CoordinatorImpl child) {

        boolean result;

        // Make sure the NestingInfo instance variables is set up
        // before adding the child.

        if (nestingInfo == null) {
            nestingInfo = new NestingInfo();
        }

        result = nestingInfo.addChild(child);

        return result;
    }

    /**
     * Removes the given Coordinator from the set of children of the
     * target TopCoordinator.
     * If the TopCoordinator is a temporary ancestor, and has no
     * recoverable state after the child is removed, it destroys itself.
     *
     * @param child  The child Coordinator.
     *
     * @return  Indicates success of the operation.
     *
     * @see
     */
    synchronized boolean removeChild(CoordinatorImpl child) {

        boolean result = false;

        // Remove the child from the set of children.  If the NestingInfo
        // instance variable has not been set up, then the child cannot
        // be removed.

        if (nestingInfo != null) {
            result = nestingInfo.removeChild(child);
        }

        // If the removal results in an empty, temporary Coordinator, then this
        // Coordinator must be cleaned up.  The RecoveryManager is called to
        // clean up the transaction.

        if (temporary && !registered &&
                !(participants != null && participants.involved()) &&
                !(synchronizations != null && synchronizations.involved()) &&
                !(nestingInfo != null && nestingInfo.numChildren() > 0)) {
            cleanUpEmpty(null);
        }

        return result;
    }

    static String[] resultName = { "Commit"/*#Frozen*/, "Rollback"/*#Frozen*/, "Read-only"/*#Frozen*/ };

    /**
     * Directs the TopCoordinator to prepare to commit.
     * The TopCoordinator directs all registered Resources to prepare, and
     * returns the result to the caller. The TopCoordinator must
     * guarantee that each Resource object registered with it receives
     * at most one prepare request (This includes the case where the
     * Recoverable Server registers the same Resource twice).
     *
     * @param
     *
     * @return  The consolidated vote.
     *
     * @exception INVALID_TRANSACTION  The transaction is not in a state to
     *   commit, due to outstanding work.
     * @exception HeuristicMixed  Indicates that a participant voted
     *   to roll the transaction back, but one or more others
     *   have already heuristically committed.
     * @exception HeuristicHazard  Indicates that a participant voted to roll
     *   the transaction back, but one or more others may have already
     *   heuristically committed.
     *
     * @see
     */
    Vote prepare()
            throws INVALID_TRANSACTION, HeuristicMixed, HeuristicHazard {

        // Until we actually distribute prepare flows, synchronize the method.

        synchronized(this) {

            // First check for active children, before getting too far into
            // the prepare. This is only done for the root Coordinator as for
            // any others it is too late.

            if (root && nestingInfo != null &&
                    nestingInfo.numChildren() != 0) {
                INVALID_TRANSACTION exc =
                    new INVALID_TRANSACTION(
                        MinorCode.UnfinishedSubtransactions,
                        CompletionStatus.COMPLETED_NO);
                throw exc;
            }

            // If the TopCoordinator is in the wrong state, return immediately.

            if (!tranState.setState(TransactionState.STATE_PREPARING)) {
                return Vote.VoteRollback;
            }

            // Check for marked rollback-only.

            if (rollbackOnly) {

                // Try to set the state to prepared fail.

                if (!tranState.
                        setState(TransactionState.STATE_PREPARED_FAIL)) {
		     if(_logger.isLoggable(Level.FINE)) {
		         _logger.log(Level.FINE,
                                "TopCoordinator - setState(TransactionState.STATE_PREPARED_FAIL) returned false");
                     }
                }

                return Vote.VoteRollback;
            }

            // Release the lock prior to distributing the prepare operations.
            // This is to allow the transaction to be marked rollback-only
            // (by resources)

        }  // synchronised bit

        // Get the RegisteredResources to distribute prepare operations.
        // If a heuristic exception is thrown, then set the state
        // to rolled back.

        Vote overallResult = Vote.VoteReadOnly;
        Throwable heuristicExc = null;

        if (participants != null) {

            try {

                overallResult = participants.distributePrepare();

		if (overallResult == Vote.VoteCommit || 
				overallResult == Vote.VoteReadOnly) {
		    
		    //if (participants.getLAOResource() != null) {
	                if (logRecord == null && Configuration.isDBLoggingEnabled()) {
                            if (!(LogDBHelper.getInstance().addRecord(
				  tranState.localTID.longValue(),
                                  tranState.globalTID.toTidBytes()))) {
                                overallResult = Vote.VoteRollback;
                            }
			}	
		    if (participants.getLAOResource() != null) {
			if (overallResult != Vote.VoteRollback) {
                            participants.getLAOResource().commit();
		        }
		    }
		}
            } catch (Throwable exc) {

                // If a heuristic exception was thrown, change the state of
                // the Coordinator to rolled back and clean up before throwing
                // the exception to the caller.

                if (exc instanceof HeuristicMixed ||
                        exc instanceof HeuristicHazard ||
                        exc instanceof INTERNAL) {

                    heuristicExc = exc;
                    if (!tranState.
                            setState(TransactionState.STATE_ROLLED_BACK)) {
		        if(_logger.isLoggable(Level.FINE)) {
			    _logger.log(Level.FINE,
                                "TopCoordinator - setState(TransactionState.STATE_ROLLED_BACK) returned false");
                        }
                    }

                    /* comented out (Ram J) for memory leak fix.
                    // Discard the Coordinator if there is no after completion.
                    // Root Coordinators and those with registered
                    // Synchronization objects always have after completion
                    // flows. Otherwise remove the RecoveryManager associations
                    // and destroy the Coordinator.

                    if (!root &&
                            (synchronizations == null ||
                             !synchronizations.involved())
                            ) {
                        RecoveryManager.removeCoordinator(superInfo.globalTID,
                                                          superInfo.localTID,
                                                          false);
                        destroy();
                    }
                    */

                    // added (Ram J) for memory leak fix
                    // if subordinate, send out afterCompletion. This will
                    // destroy the CoordinatorSynchronization and coordinator.
                    if (!root) {
                        afterCompletion(Status.StatusRolledBack);
                    }

                    /*!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!*/
                    /* NO INSTANCE VARIABLES MAY BE                      */
                    /*                       ACCESSED FROM THIS POINT ON.*/
                    /*!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!*/

                    if (heuristicExc instanceof HeuristicMixed) {
                        throw (HeuristicMixed)heuristicExc;
                    } else if (heuristicExc instanceof INTERNAL) {
                        throw (INTERNAL)heuristicExc;
                    } else {
                        throw (HeuristicHazard)heuristicExc;
                    }
                } else {
                    _logger.log(Level.FINE, "", exc);
                }

                // For any other exception, change the vote to rollback

                overallResult = Vote.VoteRollback;

            }  // catch for except

        } // if block

        // The remainder of the method needs to be synchronized.

        synchronized(this) {

            // If the transaction has been marked rollback-only during
            // this process, change the vote.

            if (rollbackOnly) {
                overallResult = Vote.VoteRollback;
            }


            // Set the state depending on the result of the prepare operation.
            // For read-only, we can throw away the Coordinator if there are no
            // synchronization objects, otherwise the Coordinator will
            // be destroyed after synchronization.
            // Set the state to prepared, read-only.

            if (overallResult == Vote.VoteReadOnly) {

                if (!tranState.
                        setState(TransactionState.STATE_PREPARED_READONLY)) {
                    overallResult = Vote.VoteRollback;
                }

                /* commented out (Ram J) for memory leak fix.
                // When voting readonly, discard the Coordinator if there is
                // no after completion. Root Coordinators and those with
                // registered Synchronization objects always have after
                // completion flows. Otherwise remove the
                // RecoveryManager associations and destroy the Coordinator.

                if (!root &&
                        (synchronizations == null ||
                         !synchronizations.involved())
                        ) {
                    RecoveryManager.removeCoordinator(superInfo.globalTID,
                                                      superInfo.localTID,
                                                      false);
                    destroy();
                }
                */

                // added (Ram J) for memory leak fix
                // if subordinate, send out afterCompletion. This will
                // destroy the CoordinatorSynchronization and coordinator.
                if (!root) {
                    afterCompletion(Status.StatusCommitted);
                }
            } else if (overallResult == Vote.VoteCommit) {

                // For commit, change any active timeout and change the state.

                int timeoutType = TimeoutManager.NO_TIMEOUT;

                // In the root, there is no need for an in-doubt timeout,
                // so cancel the timeout so that the transaction is
                // not rolled back.  Otherwise set an
                // in-doubt timeout of 60 seconds.

                if (!root) {
                    timeoutType = TimeoutManager.IN_DOUBT_TIMEOUT;
                }

                TimeoutManager.setTimeout(superInfo.localTID, timeoutType, 60);

                // Set the state to prepared_success.

                if (!tranState.
                        setState(TransactionState.STATE_PREPARED_SUCCESS)) {
                    overallResult = Vote.VoteRollback;
                }
            } else {

                // By default, assume rollback.  We do not need to cancel
                // the timeout as it  does not matter
                // if the transaction is subsequently rolled back.

                if (!tranState.
                        setState(TransactionState.STATE_PREPARED_FAIL)) {
                    overallResult = Vote.VoteRollback;
                }
            }
        }

        return overallResult;
    }

    /**
     * Directs the TopCoordinator to commit the transaction.
     * The TopCoordinator directs all registered Resources to commit. If any
     * Resources raise Heuristic exceptions, the information is recorded,
     * and the Resources are directed to forget the transaction before the
     * Coordinator returns a heuristic exception to its caller.
     *
     * @param
     *
     * @return
     *
     * @exception HeuristicMixed  A Resource has taken an heuristic decision
     *   which has resulted in part of the transaction being rolled back.
     * @exception HeuristicHazard  Indicates that heuristic decisions may have
     *   been taken which have resulted in part of the transaction
     *   being rolled back.
     * @exception NotPrepared  The transaction has not been prepared.
     *
     * @see
     */
    void commit() throws HeuristicMixed, HeuristicHazard, NotPrepared {

        // Until we actually distribute prepare flows, synchronize the method.


        synchronized(this) {
			if(_logger.isLoggable(Level.FINE))
        	{
				_logger.logp(Level.FINE,"TopCoordinator","commit()",
						"Within TopCoordinator.commit()"+"GTID is :"+
						superInfo.globalTID.toString());
        	}

            // If the TopCoordinator voted readonly,
            // produce a warning and return.

            if (tranState.state == TransactionState.STATE_PREPARED_READONLY) {
                return;
            }

            // GDH
            // If the TopCoordinator has already completed due to recovery
            // resync thread, return. (Note there is no
            // need to deal with state ROLLED_BACK here as nothing should have
            // caused us to enter that state and subsequently receive a commit.
            // However the opposite cannot be said to be true as presumed abort
            // can cause a rollback to occur when
            // replay_completion is called on a transaction that
            // has gone away already.

            if (tranState.state == TransactionState.STATE_COMMITTED) {
                return;
            }

            // If the TopCoordinator is in the wrong state, return immediately.

            if (!tranState.setState(TransactionState.STATE_COMMITTING)) {
                _logger.log(Level.SEVERE,"jts.transaction_wrong_state","commit");
				 String msg = LogFormatter.getLocalizedMessage(_logger,
				 						"jts.transaction_wrong_state",
										new java.lang.Object[] { "commit"});
				 throw  new org.omg.CORBA.INTERNAL(msg);
                //NotPrepared exc = new NotPrepared();
				//Commented out as code is never executed
                //throw exc;
            }

            // Release the lock before proceeding with commit.

        }

        // Commit all participants.  If a fatal error occurs during
        // this method, then the process must be ended with a fatal error.

        Throwable heuristicExc = null;
        Throwable internalExc = null;
        if (participants != null) {
            try {
                participants.distributeCommit();
            } catch (Throwable exc) {
                if (exc instanceof HeuristicMixed ||
                        exc instanceof HeuristicHazard) {
                    heuristicExc = exc;
                } else if (exc instanceof INTERNAL) {

                    // ADDED(Ram J) percolate any system exception
                    // back to the caller.
                    internalExc = exc; // throw (INTERNAL) exc;
                } else {
                    _logger.log(Level.WARNING, "", exc);
                }
            }
        }

        // The remainder of the method needs to be synchronized.

        synchronized(this) {

            // Record that objects have been told to commit.

            // Set the state

            if (!tranState.setState(TransactionState.STATE_COMMITTED)) {
                _logger.log(Level.SEVERE,"jts.transaction_wrong_state","commit");
				 String msg = LogFormatter.getLocalizedMessage(_logger,
				 						"jts.transaction_wrong_state",
										new java.lang.Object[] { "commit"});
				 throw  new org.omg.CORBA.INTERNAL(msg);
            }

            // Clean up the TopCoordinator after a commit. In the case where
            // the TopCoordinator is a root, the CoordinatorTerm object must be
            // informed that the transaction has completed so that if another
            // caller has committed the transaction the object normally
            // responsible for terminating the transaction can take the
            // appropriate action. NOTE: This may DESTROY the TopCoordinator
            // object so NO INSTANCE VARIABLES should be referenced after the
            // call. In the case where the TopCoordinator is a subordinate, the
            // CoordinatorResource object must be informed that the transaction
            // has been completed so that it can handle any subsequent requests
            // for the transaction.

            if (terminator != null) {
                terminator.setCompleted(false, (heuristicExc != null || internalExc != null));
            }

            /*  commented out (Ram J) for memory leak fix.
            // If there are no registered Synchronization objects,
            // there is nothing left to do, so get the RecoveryManager
            // to forget about us, then self-destruct.

            if (!root && (synchronizations == null ||
                          !synchronizations.involved())
                         ) {
                RecoveryManager.removeCoordinator(superInfo.globalTID,
                                                  superInfo.localTID,
                                                  false);
                destroy();
            }
            */

             // added (Ram J) for memory leak fix
             // if subordinate, send out afterCompletion. This will
             // destroy the CoordinatorSynchronization and coordinator.
             if (!root) {
                afterCompletion(Status.StatusCommitted);
             }


            /*!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!*/
            /* NO INSTANCE VARIABLES MAY BE ACCESSED FROM THIS POINT ON.     */
            /*!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!*/

            // If there was heuristic damage, report it.

            if (heuristicExc != null) {
                if (heuristicExc instanceof HeuristicMixed) {
                    throw (HeuristicMixed) heuristicExc;
                } else {
                    throw (HeuristicHazard) heuristicExc;
                }
            } else if (internalExc != null) {
                throw (INTERNAL) internalExc;
            }
        }
    }

    /**
     * Directs the TopCoordinator to roll back the transaction.
     * The TopCoordinator directs all registered Resources to rollback.
     * If any Resources raise Heuristic exceptions,
     * the information is recorded, and the Resources are directed
     * to forget the transaction before the
     * Coordinator returns a heuristic exception to its caller.
     *
     * @param force  Indicates that the transaction must rollback regardless.
     *
     * @return
     *
     * @exception HeuristicMixed  A Resource has taken an heuristic decision
     *   which has resulted in part of the transaction being committed.
     * @exception HeuristicHazard  Indicates that heuristic decisions may
     *   have been taken which have resulted in part of the transaction
     *   being rolled back.
     * @see
     */
    void rollback(boolean force) throws HeuristicMixed, HeuristicHazard {

        // Until we actually distribute prepare flows, synchronize the method.

        synchronized(this){
		if(_logger.isLoggable(Level.FINE))
		{
			_logger.logp(Level.FINE,"TopCoordinator","rollback()",
					"Within TopCoordinator.rollback() :"+"GTID is : "+
					superInfo.globalTID.toString());
        }

            // If the transaction has already been rolled back, just return.

            if (tranState == null) {
                return;
            }

            // GDH
            // If the TopCoordinator has already completed (eg due to
            // recovery resync thread and this is now running on
            // the 'main' one) we can safely ignore the error

            if (tranState.state == TransactionState.STATE_ROLLED_BACK) {
                return;
            }

            // GDH
            // The state could even be commited, which can be OK if it was
            // committed, and thus completed, when the recovery thread asked
            // the superior about the txn. The superior would
            // no longer had any knowledge of it. In this case, due to presumed
            // abort, the recovery manager would then
            // now default to aborting it.
            // In this case if the TopCoordinator has committed already
            // we should also just return ignoring the error.

            if (tranState.state == TransactionState.STATE_COMMITTED) {
                return;
            }

            // If this is not a forced rollback and the coordinator
            // has prepared or is in an inappropriate state, do not continue
            // and return FALSE.

            if (!force && ((tranState.state ==
                                TransactionState.STATE_PREPARED_SUCCESS) ||
                           (!tranState.setState(
                                TransactionState.STATE_ROLLING_BACK))
                          )) {
                return;
            }

            // We do not care about invalid state changes as we are
            // rolling back anyway. If the TopCoordinator is
            //  temporary, we do not change state as this would
            // cause a log force in a subordinate, which is not required.

            if( !temporary &&
                    !tranState.setState(TransactionState.STATE_ROLLING_BACK)) {
                if(_logger.isLoggable(Level.FINE)) {
		    _logger.log(Level.FINE,
                           "TopCoordinator - setState(TransactionState.STATE_ROLLED_BACK) returned false");
                }
            }

            // Rollback outstanding children.  If the NestingInfo instance
            // variable has not been created, there are no
            // children to rollback.

            if (nestingInfo != null) {
                nestingInfo.rollbackFamily();
            }

            // Release the lock before proceeding with rollback.

        }

        // Roll back all participants.  If a fatal error occurs during
        // this method, then the process must be ended with a fatal error.

        Throwable heuristicExc = null;
        if (participants != null) {
            try {
                participants.distributeRollback(false);
            } catch(Throwable exc) {

                if (exc instanceof HeuristicMixed ||
                        exc instanceof HeuristicHazard) {
                    heuristicExc = exc;
                } else if (exc instanceof INTERNAL) {
                    // ADDED (Ram J) percolate up any system exception.
                    throw (INTERNAL) exc;
                } else {
                    _logger.log(Level.WARNING, "", exc);
                }
            }
        }

        // The remainder of the method needs to be synchronized.

        synchronized(this) {

            // Set the state.  Only bother doing this if the coordinator
            // is not temporary.

            if (!temporary &&
                    !tranState.setState(TransactionState.STATE_ROLLED_BACK)) {
	        if(_logger.isLoggable(Level.FINE)) {
		    _logger.log(Level.FINE,
                          "TopCoordinator - setState(TransactionState.STATE_ROLLED_BACK) returned false");
                }
            }

            // Clean up the TopCoordinator after a rollback.
            // In the case where the TopCoordinator is a root,
            // the CoordinatorTerm object must be informed that the transaction
            // has completed so that if another caller has rolled back
            // the transaction (time-out for example) the object normally
            // responsible for terminating the transaction can take the
            // appropriate action. NOTE: This may DESTROY
            // the TopCoordinator object so NO INSTANCE VARIABLES
            // should be referenced after the call. In the case where
            // the TopCoordinator is a subordinate, the CoordinatorResource
            // object must be informed that the transaction has been
            // completed so that it can handle any subsequent requests for the
            // transaction.

            if (terminator != null) {
                terminator.setCompleted(true, heuristicExc != null);
            }

            /* commented out (Ram J) for memory leak fix.
            // If there are no registered Synchronization objects, there is
            // nothing left to do, so get the RecoveryManager to forget
            // about us, then self-destruct.

            if (!root && (synchronizations == null ||
                          !synchronizations.involved())
                         ) {
                RecoveryManager.removeCoordinator(superInfo.globalTID,
                                                  superInfo.localTID,
                                                  true);

                if (!dying) {
                    destroy();
                }
            }
            */
            // added (Ram J) for memory leak fix
            // if subordinate, send out afterCompletion. This will
            // destroy the CoordinatorSynchronization and coordinator.
            if (!root) {
                afterCompletion(Status.StatusRolledBack);
            }

            /*!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!*/
            /* NO INSTANCE VARIABLES MAY BE ACCESSED FROM THIS POINT ON.     */
            /*!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!*/

            // If there was heuristic damage, report it.

            if (heuristicExc != null) {
                if (heuristicExc instanceof HeuristicMixed) {
                    throw (HeuristicMixed) heuristicExc;
                } else {
                    throw (HeuristicHazard) heuristicExc;
                }
            }
        }

        // Otherwise return normally.
    }

    /**
     * Informs the TopCoordinator that the given object requires
     * synchronization before and after completion of the transaction.
     * If possible, a CoordinatorSync object is registered
     * with the superior Coordinator.  Otherwise this
     * Coordinator becomes the root of a sub-tree for
     * synchronization.
     *
     * @param sync  The Synchronization object to be registered.
     *
     * @return
     *
     * @exception Inactive  The Coordinator is in the process of completing the
     *   transaction and cannot accept this registration.
     * @exception SynchronizationUnavailable  The transaction service
     *   cannot support synchronization.
     * @exception SystemException  The operation failed.
     *
     * @see
     */
    synchronized public void register_synchronization(Synchronization sync)
            throws SystemException, Inactive, SynchronizationUnavailable {

        // First check the state of the transaction. If it is not active,
        // do not allow the registration.

        if (tranState == null ||
                tranState.state != TransactionState.STATE_ACTIVE) {
              Inactive exc = new Inactive();
              throw exc;
        }

        // If not previously registered, a CoordinatorSync object must be
        // registered with our superior.  Note that root TopCoordinators
        // are created with the registration flag set, so we do not need to
        // check whether we are the root TopCoordinator here.

        if (!registeredSync && DefaultTransactionService.isORBAvailable()) {

            // Initialise the CoordinatorSync with the local id, our reference,
            // and a flag to indicate that does not represent a subtransaction.

            CoordinatorSynchronizationImpl sImpl =
                new CoordinatorSynchronizationImpl(this);

            // Register the CoordinatorSync with the superior CoordinatorImpl.

            try {
                Synchronization subSync = sImpl.object();
                superInfo.superior.register_synchronization(subSync);
                registeredSync = true;

                // added (Ram J) for memory leak fix.
                this.coordSyncImpl = sImpl;
				if(_logger.isLoggable(Level.FINER))
                {
					_logger.logp(Level.FINER,"TopCoordinator",
							"register_synchronization()", 
							"CoordinatorSynchronizationImpl :" + sImpl + 
							" has been registered with (Root)TopCoordinator"+
							"GTID is: "+ superInfo.globalTID.toString());
                }

            } catch (Exception exc) {
                // If an exception was raised, dont set the registration flag.
                sImpl.destroy();

                // If the exception is a system exception, then allow it
                // to percolate to the caller.

                if (exc instanceof OBJECT_NOT_EXIST) {
                    TRANSACTION_ROLLEDBACK ex2 =
                        new TRANSACTION_ROLLEDBACK(
                            0, CompletionStatus.COMPLETED_NO);
                    ex2.initCause(exc);
                    throw ex2;
                }

                if (exc instanceof Inactive) {
                    throw (Inactive)exc;
                }

                if (exc instanceof SystemException) {
                    throw (SystemException) exc;
                }

                // Otherwise throw an internal exception.

                INTERNAL ex2 = new INTERNAL(MinorCode.NotRegistered,
                                            CompletionStatus.COMPLETED_NO);
                ex2.initCause(exc);
                throw ex2;
            }
        }

        // Make sure the RegisteredSyncs instance variable has been set up.

        if (synchronizations == null) {
            synchronizations = new RegisteredSyncs();
        }

        // Add a duplicate of the reference to the set.  This is done
        // because if the registration is for a remote object,
        // the proxy will be freed
        // when the registration request returns.

        // COMMENT(Ram J) if the sync object is a local servant, there is
        // no proxy involved. Also the instanceof operator could be replaced
        // by a is_local() method if this class implements the CORBA local
        // object contract.
        if (sync instanceof com.sun.jts.jta.SynchronizationImpl) {
            synchronizations.addSync(sync);

		    if(_logger.isLoggable(Level.FINER))
            {
				_logger.logp(Level.FINER,"TopCoordinator",
						"register_synchronization()",
						"SynchronizationImpl :" + sync +
						" has been registeredwith TopCoordinator :"+
						"GTID is : "+ superInfo.globalTID.toString());
            }

        } else {
            synchronizations.addSync((Synchronization) sync._duplicate());
        }

        temporary = false;
    }

    /**
     * Informs the TopCoordinator that the transaction is about to complete.
     * The TopCoordinator informs all Synchronization objects registered with
     * it that the transaction is about to complete and waits for all of the
     * replies before this operation completes.
     *
     * @param
     *
     * @return
     *
     * @exception INVALID_TRANSACTION  The transaction is not in a state to
     *   commit, due to outstanding work.
     *
     * @see
     */
    synchronized void beforeCompletion() throws INVALID_TRANSACTION {

        // First check for active children, before getting too far in.
        // This is only done for the root Coordinator as for any
        // others its too late.

        if (root && nestingInfo != null && nestingInfo.numChildren() != 0) {
            INVALID_TRANSACTION exc = new INVALID_TRANSACTION(
                                        MinorCode.UnfinishedSubtransactions,
                                        CompletionStatus.COMPLETED_NO);
            throw exc;
        }

        // If there are registered Synchronization objects, tell them
        // the transaction is about to prepare.

        if (synchronizations != null) {

            // Tell the RegisteredSyncs to distribute the before completion
            // messages. If an exception is raised, then mark the transaction
            // rollback-only.

            try {
                if (!synchronizations.distributeBefore()) {
                    rollbackOnly = true;
                }
            } catch (RuntimeException ex) {
                rollbackOnly = true;
                throw ex;
            }
        }
    }

    /**
     * Informs the TopCoordinator that the transaction has completed.
     * The TopCoordinator informs all Synchronization objects registered with
     * it that the transaction has completed. It does not need to wait for all
     * responses before returning.
     *
     * @param status  Indicates whether the transaction committed or aborted.
     *
     * @return
     *
     * @see
     */
    synchronized void afterCompletion(Status status) {

        // If the Coordinator is still active, set it to read only to prevent
        // the Coordinator from actually rolling back when it is destroyed.

        if (tranState.state == TransactionState.STATE_ACTIVE) {
            tranState.setState(TransactionState.STATE_PREPARING);
            tranState.setState(TransactionState.STATE_PREPARED_READONLY);
        }

        // If there are registered Synchronization objects,
        // tell them the transaction has completed.

        if (synchronizations != null) {

            // Tell the RegisteredSyncs to distribute the after completion
            // messages. If an exception occurs, just report it.

            // synchronizations.distributeAfter(get_status());
            synchronizations.distributeAfter(status);
        }

        // At this point, there is nothing left to do, so destroy ourselves
        // before returning.
        boolean aborted = true;
        if (status == Status.StatusCommitted) {
            aborted = false;
        }
        if (!delegated) {
            RecoveryManager.removeCoordinator(superInfo.globalTID,
                                       superInfo.localTID,
                                       aborted);
        } else {
            DelegatedRecoveryManager.removeCoordinator(superInfo.globalTID,
                                       superInfo.localTID,
                                       aborted, logPath);
        }

        // memory leak fix (Ram J) - cleanup the Recovery Coordinator objs.
        if (recoveryCoordinatorList != null) {
            for (int i = 0; i < recoveryCoordinatorList.size(); i++) {
                RecoveryCoordinatorImpl rcImpl = (RecoveryCoordinatorImpl)
                    recoveryCoordinatorList.elementAt(i);
                rcImpl.destroy();
            }
            recoveryCoordinatorList = null;
        }

        // memory leak fix (Ram J)
        // destroy the CoordinatorSynchronization object.
        if (this.coordSyncImpl != null) {
            this.coordSyncImpl.destroy();
        }
        this.synchronizations = null;

        // destroy the coordinator object.
        destroy();
    }

    /**
     * Informs the TopCoordinator of the identity of the
     * object that is normally responsible for directing
     * it through termination. The CoordinatorTerm /
     * CoordinatorResource object is informed by the Coordinator when the
     * transaction aborts so that they can cope with asynchronous aborts.
     *
     * @param term  The object normally responsible for terminating the
     *              Coordinator.
     * @return
     *
     * @see
     */
    synchronized void setTerminator(CompletionHandler term) {
        terminator = term;
    }

    /**
     * Gets the parent coordinator of the transaction.  As this is a top level
     * coordinator, a parent does not exist so NULL is returned.
     *
     * @param
     *
     * @return  The parent Coordinator, null.
     *
     * @see
     */
    Coordinator getParent() {

        Coordinator result = null;
        return result;
    }

    /**
     * Gets the superior Coordinator for this transaction.
     *
     * @param
     *
     * @return  The superior Coordinator
     *
     * @see
     */
    Coordinator getSuperior() {

        Coordinator result = superInfo.superior;
        return result;
    }

    /**
     * Returns the Resource objects and their states.
     *
     * @param resources  The object which will contain the Resources
     * @param states     The object which will contain the states.
     *
     * @return
     *
     * @see
     */
    /* COMMENT(Ram J) only Admin package needs this.
    public void getResources(ResourceSequenceHolder resources,
                             ResourceStatusSequenceHolder states) {
        if (participants != null) {
            participants.getResources(resources,states);

            // Validate each of the Resource objects in the list
            // before returning it.

            for (int i = 0; i < resources.value.length; i++) {
                if (resources.value[i]._non_existent()) {
                    resources.value[i] = null;
                }
            }
        } else {
            resources.value = new Resource[0];
            states.value = new ResourceStatus[0];
        }
    }
    */

    /**
     * Gets the object normally responsible for terminating this Coordinator.
     *
     * @param
     *
     * @return  The object normally responsible for terminating
     *   the Coordinator.
     *
     * @see
     */
    CompletionHandler getTerminator()  {

        CompletionHandler result = terminator;
        return result;
    }

    /**
     * Registers the given Resource object with the Coordinator with no regard
     * for the state of the transaction or registration with the superior.
     * <p>
     * This is intended to be used during recovery to enable XA Resource
     * Managers to participate in resync without needing the XA Resource
     * objects to have persistent references.
     * <p>
     * The Resource object parameter should only refer to a local object.
     *
     * @param res  The Resource to be directly registered.
     *
     * @return
     *
     * @see
     */
    void directRegisterResource(Resource res) {

        // If the set has not already been created, create it now.
        // Note that we do notpass the CoordinatorLog object to the
        // RegisteredResources as we do not want to do anything
        // with it here.  Generally participants will not be null
        // as this method will be called diring recovery.

        if (participants == null) {
            participants = new RegisteredResources(null, this);
        }

        // Add the reference to the set.  The reference is not duplicated,
        // as this operation should only be called for local Resource objects.

        participants.addRes(res);
		if(_logger.isLoggable(Level.FINE))
        {
			_logger.logp(Level.FINE,"TopCoordinator","directRegisterResource()",
					"Registered resource :" + res );
        }

    }

    private static Any emptyData = null;

    /**
     * Creates a PropagationContext which contains the information which would
     * normally be passed implicitly via the CosTSPropagation interfaces.
     *
     * @param
     *
     * @return  The transaction context.
     *
     * @exception Inactive  The Coordinator is in the process of completing the
     *   transaction and cannot return the information.
     *
     * @see
     */
    synchronized public PropagationContext get_txcontext() throws Unavailable {

        // First check the state of the transaction. If it is not active,
        // do not allow the registration.

        if (tranState == null ||
                tranState.state != TransactionState.STATE_ACTIVE ||
                rollbackOnly ) {
            Unavailable exc = new Unavailable();
            throw exc;
        }

        // Work out the timeout value to pass, if any.
        // Note that only top-level transactions have timeouts.
        // We do not check for timeouts if the Coordinator is remote.
        // If the Coordinator does not have a timeout defined, the
        // TimeoutManager will return a negative value.
        // If the transaction has timed out, the value will be  zero.

        long timeLeft = TimeoutManager.timeLeft(superInfo.localTID);
        int timeout = 0;
        if (timeLeft > 0) {

            timeout = (int)timeLeft/1000;

        } else if (timeLeft == 0) {

            // If the timeout has expired, then do not return a context,
            // but roll the transaction back and
            // throw the TRANSACTION_ROLLEDBACK exception.

            TimeoutManager.timeoutCoordinator(superInfo.localTID,
                                              TimeoutManager.ACTIVE_TIMEOUT);
            TRANSACTION_ROLLEDBACK exc = new TRANSACTION_ROLLEDBACK(
                                            0, CompletionStatus.COMPLETED_NO);
            throw exc;
        }

        // Fill in the context with the current transaction information,
        // and the ancestor information.

        TransIdentity current = new TransIdentity(this.object(),
                                                  null,
                                                  superInfo.globalTID.realTID);

        // Ensure that the implementation specific data is filled with a value.

        if (emptyData == null){
            emptyData = Configuration.getORB().create_any();
            emptyData.insert_boolean(false);
        }

        PropagationContext result = new PropagationContext(
                                            timeout, current,
                                            new TransIdentity[0], emptyData);
		if(_logger.isLoggable(Level.FINEST))
        {
			_logger.logp(Level.FINEST,"TopCoordinator","get_txcontext()", 
					"Obtained PropagationContext"+"GTID is: "+
					superInfo.globalTID.toString());
        }

        return result;
    }

    /**
     * Cleans up an empty Coordinator.
     *
     * @param parent  The parent Coordinator
     *                (always null for a TopCoordinator).
     * @return
     *
     * @see
     */
    void cleanUpEmpty(CoordinatorImpl parent) {

        // Roll the transaction back, ignoring any exceptions.

        try {
            rollback(true);
        } catch( Throwable exc ) {
            _logger.log(Level.FINE, "", exc);
        }
    }

    //-------------------------------------------------------------------------
    // Method: TopCoordinator.commmitOnePhase
    //
    // Comments: This method is called by CoordinatorResource when
    //           this Coordinator is a subordinate coordinator. If any
    //           Resources raise Heuristic exceptions, the information is
    //           recorded, and the Resources are directed to forget the
    //           transaction before the Coordinator returns a heuristic
    //           exception to its caller.
    //-------------------------------------------------------------------------

    /**
     * commitOnePhase
     *
     * @param none
     * @return boolean indicating success or whether two phase commit
     *                 should be tried.
     * @see
     */
    boolean commitOnePhase() throws HeuristicMixed, HeuristicHazard  {

        synchronized (this) {

            // First check for active children, before getting too far
            // into the prepare. This is only done for
            // the root Coordinator as for any others it is too  late.

            if (root && nestingInfo != null &&
                    nestingInfo.numChildren() != 0) {
                INVALID_TRANSACTION exc =
                    new INVALID_TRANSACTION(
                        MinorCode.UnfinishedSubtransactions,
                        CompletionStatus.COMPLETED_NO);
                throw exc;
            }


            // If the Coordinator has > 1 resource return smoothly

            if (participants != null && participants.numRegistered() > 1) {
                // GDH COPDEF1
                return false;
            }

            // If the TopCoordinator is in the wrong state, return immediately.

            if (!tranState.
                setState(TransactionState.STATE_COMMITTING_ONE_PHASE)) {
                return false;
            }

            // Check for marked rollback-only, if we are then we can drive
            // the rollback directly from the 2PC process so return false
            // this will cause use to enter prepare (which will do nothing as
            // it checks the same flag
            // thence directly into rollback

            if (rollbackOnly) {
                return false;
            }

            int timeoutType = TimeoutManager.NO_TIMEOUT;

            // In the root, there is no need for an in-doubt timeout,
            // so cancel the timeout so that the transaction is
            // not rolled back. Otherwise set anin-doubt timeout of 60 seconds.

            if (root) {
                TimeoutManager.setTimeout(superInfo.localTID,
                                          TimeoutManager.NO_TIMEOUT,
                                          60);
            }
            else {
                TimeoutManager.setTimeout(superInfo.localTID,
                                          TimeoutManager.IN_DOUBT_TIMEOUT,
                                          60);
            }


            //
            // Contact the resource (note: participants can exist with
            //                             no resourcesafter recovery)
            //

        }  // first synchronised bit ends now to allow possible callbacks.


        if ((participants != null)  && (participants.numRegistered() == 1)) {
            Throwable heuristicExc = null;
            Throwable internalExc = null;
            boolean rolled_back = false;
            try {
                participants.commitOnePhase();
            } catch (Throwable exc) {

                if (exc instanceof HeuristicMixed) {
		    // revert IASRI START 4722886
                    heuristicExc = exc;
		    // revert IASRI END 4722886
                    if (!tranState.setState(TransactionState.
                                STATE_COMMIT_ONE_PHASE_HEURISTIC_MIXED)) {
			_logger.log(Level.SEVERE,"jts.transaction_wrong_state",
                               "COMMIT_ONE_PHASE (1)");
			 String msg = LogFormatter.getLocalizedMessage(_logger,
				"jts.transaction_wrong_state",
				new java.lang.Object[]
				{ "COMMIT_ONE_PHASE (1)"});
			 throw  new org.omg.CORBA.INTERNAL(msg);
                    }
		    // revert IASRI START 4722886
		    // throw (HeuristicMixed)exc;
		    // revert IASRI END 4722886
                } else if (exc instanceof HeuristicHazard) {
		    // revert IASRI START 4722886
                    heuristicExc = exc;
		    // revert IASRI END 4722886
                    if (!tranState.setState(TransactionState.
                             STATE_COMMIT_ONE_PHASE_HEURISTIC_HAZARD)) {
			_logger.log(Level.SEVERE,"jts.transaction_wrong_state",
                                 "COMMIT_ONE_PHASE (2)");
			 String msg = LogFormatter.getLocalizedMessage(_logger,
			        "jts.transaction_wrong_state",
			        new java.lang.Object[]
			        { "COMMIT_ONE_PHASE (2)"});
		         throw  new org.omg.CORBA.INTERNAL(msg);
                    }
		    // revert IASRI START 4722886
		    // throw (HeuristicHazard)exc;
		    // revert IASRI END 4722886
                } else if (exc instanceof TRANSACTION_ROLLEDBACK) {
                    rolled_back = true;

                    // GDH COPDEF2 Removed code below that changesd state to
                    // COMMIT_ONE_PHASE_ROLLED_BACK this was unnecessary
                    // as setting the rolled_back flag is picked up just below
                    // where the state is changed. Prior to this change we
                    // tried to set the state to COP_RB twice
                    // in a row which is an error.
                } else if (exc instanceof INTERNAL) {
                    // ADDED (Ram J) percolate up any system exception.
                    internalExc = exc;
                } else {
                    _logger.log(Level.WARNING, "", exc);
                } // end else if cascade on the exception types

                // (Other exceptions are not passed back
                // by RegisteredResources)

            } // end of catch block for exceptions

            // (GDH COPDEF1 was after if-else block below)

            // Set the final state now
            if (rolled_back) {

                // GDH COPDEF1 Changed state movement to be via COP_RB
                // even though possible to go direct to RB

                // Change state in two steps - this is traced and only the
                // first change would need a forced log write traditionaly
                if (!tranState.setState(
                        TransactionState.
                            STATE_COMMIT_ONE_PHASE_ROLLED_BACK)) {
			_logger.log(Level.SEVERE,"jts.transaction_wrong_state",
                            "COMMIT_ONE_PHASE (4)");
			 String msg = LogFormatter.getLocalizedMessage(_logger,
				"jts.transaction_wrong_state",
				new java.lang.Object[]
				{ "COMMIT_ONE_PHASE (4)"});
		         throw  new org.omg.CORBA.INTERNAL(msg);
                    }
                  /**

                if (!tranState.setState(TransactionState.STATE_ROLLED_BACK)) {
					_logger.log(Level.SEVERE,"jts.transaction_wrong_state",
                            "COMMIT_ONE_PHASE (5)");
					 String msg = LogFormatter.getLocalizedMessage(_logger,
				 						"jts.transaction_wrong_state",
										new java.lang.Object[] { "COMMIT_ONE_PHASE (5)"});
					  throw  new org.omg.CORBA.INTERNAL(msg);
                }
                   **/
            } else if (heuristicExc == null) { // we commited without a Heuristic exception

                // GDH COPDEF1 Changed state movement to be via COP_OK
                // this is needed by the state tables as the first
                // state change should ideall be a forced log write.

                // We do the state change in two jumps for better trace
                // and to log the fact that a COP_OK represents a successful
                // prepare - only the fist state changed needs a flushed write.
                if (!tranState.setState(TransactionState.
                                            STATE_COMMITTED_ONE_PHASE_OK)) {
					_logger.log(Level.SEVERE,"jts.transaction_wrong_state",
                            "COMMIT_ONE_PHASE (6)");
					 String msg = LogFormatter.getLocalizedMessage(_logger,
				 						"jts.transaction_wrong_state",
										new java.lang.Object[] { "COMMIT_ONE_PHASE (6)"});
					  throw  new org.omg.CORBA.INTERNAL(msg);
                }
                /**

                // Now set this coord to commited finally.
                if (!tranState.setState(TransactionState.STATE_COMMITTED)) {
					_logger.log(Level.SEVERE,"jts.transaction_wrong_state",
                            "COMMIT_ONE_PHASE (7)");
					 String msg = LogFormatter.getLocalizedMessage(_logger,
				 						"jts.transaction_wrong_state",
										new java.lang.Object[] { "COMMIT_ONE_PHASE (7)"});
					  throw  new org.omg.CORBA.INTERNAL(msg);
                }
                 **/
            }  // else we did not rollback


            // The remainder of the method needs to be synchronized too!

            synchronized (this) {

                // Clean up the TopCoordinator after a commit. In the case
                // where the  TopCoordinator is a root,
                // the CoordinatorTerm object must be informed that
                // the transaction has completed so that if another
                // caller has committed the transaction the object
                // normally responsible for terminating the
                // transaction can take the appropriate action. NOTE: This may
                // DESTROY the TopCoordinator object so NO INSTANCE VARIABLES
                // should be referenced after the call.
                // In the case where the TopCoordinator is a subordinate, the
                // CoordinatorResource object must be informed that the
                // transaction has been completed so that it can
                // handle any subsequent requests for the
                // transaction.

                if (terminator != null) {
                    terminator.setCompleted(false, heuristicExc != null || internalExc != null);
                }

                /* commented out (Ram J) for memory leak fix.
                // If there are no registered Synchronization objects,
                // there is nothing left to do, so get the RecoveryManager
                // to forget about us, then self-destruct.

                if (!root && (synchronizations == null ||
                              !synchronizations.involved())
                             ) {
                    RecoveryManager.removeCoordinator(superInfo.globalTID,
                                                      superInfo.localTID,
                                                      false);
                    destroy();
                }
                */

                // added (Ram J) for memory leak fix
                // if subordinate, send out afterCompletion. This will
                // destroy the CoordinatorSynchronization and coordinator.
                if (!root) {
                    afterCompletion(Status.StatusCommitted);
                }

                /*!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!*/
                /* NO INSTANCE VARIABLES MAY BE ACCESSED FROM THIS POINT ON. */
                /*!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!*/

                // If there was heuristic damage, report it.

                if (heuristicExc != null) {
                    if (heuristicExc instanceof HeuristicMixed) {
                        throw (HeuristicMixed)heuristicExc;
                    } else {
                        throw (HeuristicHazard)heuristicExc;
                    }
                } else if (internalExc != null) {
                    throw (INTERNAL) internalExc;
                }

                // If the resource rolled back throw TRANSACTION_ROLLEDBACK

                if (rolled_back) {
                    TRANSACTION_ROLLEDBACK exc =
                        new TRANSACTION_ROLLEDBACK(
                            0, CompletionStatus.COMPLETED_YES);
                    throw exc;
                }

            } //end of any synchronised work

            // Otherwise return normally.

            return true;

        } else {
            // GDH COPDEF1
            // No resources at all - just complete state as for commited

            // We do the state change in two jumps for better trace and to
            // log the fact that a COP_OK represents a successful prepare,
            // only the fist state changed needs a flushed write.
            // (Can't turn of NTFS file caching in Java anyway but the
            // intention is in that direction)
            if (!tranState.
                    setState(TransactionState.STATE_COMMITTED_ONE_PHASE_OK)) {
					_logger.log(Level.SEVERE,"jts.transaction_wrong_state",
                            "COMMIT_ONE_PHASE (8)");
					 String msg = LogFormatter.getLocalizedMessage(_logger,
				 						"jts.transaction_wrong_state",
										new java.lang.Object[] { "COMMIT_ONE_PHASE (8)"});
					  throw  new org.omg.CORBA.INTERNAL(msg);
            }

            // Now set this coord to commited finally.
            /*
            if (!tranState.setState(TransactionState.STATE_COMMITTED)) {
				_logger.log(Level.SEVERE,"jts.transaction_wrong_state",
                        "COMMIT_ONE_PHASE (9)");
				 String msg = LogFormatter.getLocalizedMessage(_logger,
			 						"jts.transaction_wrong_state",
									new java.lang.Object[] { "COMMIT_ONE_PHASE (9)"});
				  throw  new org.omg.CORBA.INTERNAL(msg);
            }
            */
        } // end of else clause if no resources

        return true;
    }

    /**
     * Returns a hash code for the object.
     * <p>
     * This very basic method is used by the trace facility and
     * should not call any method which is traced.
     *
     * @param
     *
     * @return  The hash code for the object.
     *
     * @see
     */
    public int hashCode() {
        if (hash == 0 && superInfo != null && superInfo.globalTID != null) {
            hash = superInfo.globalTID.hashCode();
        }

        return hash;
    }

    /**
     * Determines equality of the object with the parameter.
     *
     * @param other  The other object.
     *
     * @return  Indicates equality.
     *
     * @see
     */
    public boolean equals(java.lang.Object other) {

        // Do a quick check on the object references.

        if( this == other ) return true;

        // Obtain the global identifier for the other Coordinator.

        otid_t otherTID = null;

        // For local Coordinator objects which are really instances of the
        // CoordinatorImpl class, get the global TID via a private method call.

        if (other instanceof CoordinatorImpl) {
            if (other instanceof TopCoordinator) {
                otherTID = ((TopCoordinator) other).
                                superInfo.globalTID.realTID;
            }
        } else if (other instanceof org.omg.CORBA.Object) {

            // For remote Coordinator objects which are instances of
            // the JCoordinator class, use the getGlobalTID method remotely.

            try {
                JCoordinator jcoord = JCoordinatorHelper.
                                        narrow((org.omg.CORBA.Object) other);
                otherTID = jcoord.getGlobalTID();
            } catch (BAD_PARAM exc) {

                // For remote Coordinator objects which are not
                // instances of the JCoordinator class, use the propagation
                // context to compare the Coordinators. This relies on the
                // availability of the propagation
                // context from the target Coordinator.

                try {
                    Coordinator coord =
                        CoordinatorHelper.narrow((org.omg.CORBA.Object) other);
                    PropagationContext pc = coord.get_txcontext();
                    otherTID = pc.current.otid;
                } catch (BAD_PARAM ex2) {
                    // If the other object is not actually a Coordinator,
                    // then the objects are not the same.
                } catch (Unavailable ex2) {
                    // If the other Coordinator is inactive, then there is
                    // nothing we can do to get the global identifier for the
                    // transaction, so we cannot compare the
                    // Coordinator objects.
                    INVALID_TRANSACTION ex3 = new INVALID_TRANSACTION(
                                                MinorCode.CompareFailed,
                                                CompletionStatus.COMPLETED_NO);
                    ex3.initCause(exc);
                    throw ex3;
                }
            }
        }

        // Compare the global identifiers.

        if (otherTID != null) {
            return superInfo.globalTID.isSameTID(otherTID);
        }

        return false;
    }
}

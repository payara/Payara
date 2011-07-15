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
// Module:      TransactionState.java
//
// Description: Transaction state manager.
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

import java.io.*;
import java.util.*;
import com.sun.jts.trace.*;
import org.omg.CosTransactions.*;

import com.sun.jts.utils.RecoveryHooks.FailureInducer;

import java.util.logging.Logger;
import java.util.logging.Level;
import com.sun.logging.LogDomains;
import com.sun.jts.utils.LogFormatter;
/**
 * The TransactionState interface provides operations that maintain the
 * relative commitment state of a Coordinator object, and is responsible for
 * allocating new global and local transaction identifiers. This class is
 * contained in the TopCoordinator and SubCoordinator classes.
 *
 * @version 0.7
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
//   0.2   SAJH   GlobalTID interface changes.
//   0.3   SAJH   Repository synchronization.
//   0.4   SAJH   Conversion to new bindings.
//   0.5   SAJH   Modified log record writing.
//   0.6   SAJH   Allow preparing->rolled back for prepare heuristics.
//   0.7   SAJH   Renamed repository.
//   0.8   GDH    Added  commit_one_phase related states
//----------------------------------------------------------------------------

class TransactionState {

    /**
     * A state value indicating that the transaction has not yet been started.
     */
    final static int STATE_NONE = 0;

    /**
     * A state value indicating that the transaction has been started,
     * and not yet completed.
     */
    final static int STATE_ACTIVE = 1;

    /**
     * A state value indicating that the transaction is in the process of being
     * prepared.
     */
    final static int STATE_PREPARING = 2;

    /**
     * A state value indicating that the transaction has been
     * successfully prepared, but commit or rollback has not yet started.
     */
    final static int STATE_PREPARED_SUCCESS = 3;

    /**
     * A state value indicating that the transaction has failed to be prepared,
     * but rollback has not yet started.
     */
    final static int STATE_PREPARED_FAIL = 4;

    /**
     * A state value indicating that the transaction has been prepared
     * and is read-only, but rollback has not yet started.
     */
    final static int STATE_PREPARED_READONLY = 5;

    /**
     * A state value indicating that the transaction is in the process of being
     * committed.
     */
    final static int STATE_COMMITTING = 6;

    /**
     * A state value indicating that the transaction has been committed.
     */
    final static int STATE_COMMITTED = 7;

    /**
     * A state value indicating that the transaction is in the process of being
     * rolled back.
     */
    final static int STATE_ROLLING_BACK = 8;

    /**
     * A state value indicating that the transaction has been rolled back.
     */
    final static int STATE_ROLLED_BACK = 9;

    // GDH: New COP States
    /**
     * A state value indicating that the transaction is being commited
     * to a downstream resource using one phase commit
     */
    final static int STATE_COMMITTING_ONE_PHASE = 10;


    /**
     * A state value indicating that the transaction has been successfully
     * commited using commit one phase
     */
    final static int STATE_COMMITTED_ONE_PHASE_OK = 11;


    /**
     * A state value indicating that the transaction has been rolled back
     * after a commit one phase flow.
     */
    final static int STATE_COMMIT_ONE_PHASE_ROLLED_BACK = 12;

    /**
     * A state value indicating that the transaction has heuristic
     * hazard after a commit one phase flow.
     */
    final static int STATE_COMMIT_ONE_PHASE_HEURISTIC_HAZARD = 13;

    /**
     * A state value indicating that the resources are heuristic mixed
     * after a commit one phase flow.
     */
    final static int STATE_COMMIT_ONE_PHASE_HEURISTIC_MIXED = 14;

	/*
		Logger to log transaction messages
	*/  
    static Logger _logger = LogDomains.getLogger(TransactionState.class, LogDomains.TRANSACTION_LOGGER);

    static RWLock freezeLock = new RWLock();
    GlobalTID globalTID = null;
    Long      localTID = null;
    int       state =  STATE_NONE;

    boolean        subordinate = false;
    CoordinatorLog logRecord = null;
    Object         logSection = null;

    //static long epochNumber    = new Date().getTime();
    static long sequenceNumber = 1;

    static boolean inDoubt = false;

    //get server name only once
    //static String serverName = Configuration.getServerName();

    //half built cached TID - used as template for any globalTIDs generations
    static byte [] TIDTemplate=null;

    // GDH State table added to for one phase commit of single resource
    // could have extended further to split out:
    //    heuristic rollback
    //    rolling back huristic hazard
    //    rolling back heuristic mixed
    // but these are factored into rolled back and rolling back respectively


    // GDH: Added this column onwards
    //
    final static boolean[][] validStateChange = {
    /*  from      to         none   actve  ping    pds    pdf    pdr   cing    cd    ring    rd    c1p    1p_ok  1p_rb  1p_hh  1p_hm  */
    /*  none            */ { false, true,  false, false, false, false, false, false, true,  false, false, false, false, false, false  },
    /*  active          */ { false, false, true,  false, false, false, false, false, true,  false, true,  false, false, false, false  },
    /*  preparing       */ { false, false, false, true,  true,  true,  false, false, true,  true,  false, false, false, false, false  },
    /*  prepared_success*/ { false, false, false, false, false, false, true,  false, true,  false, false, false, false, false, false  },
    /*  prepared_fail   */ { false, false, false, false, false, false, false, false, true,  false, false, false, false, false, false  },
    /*  prepared_ro     */ { false, false, false, false, false, false, false, false, false, false, false, false, false, false, false  },
    /*  committing      */ { false, false, false, false, false, false, true,  true,  false, false, false, false, false, false, false  },
    /*  committed       */ { true,  false, false, false, false, false, false, false, false, false, false, false, false, false, false  },
    /*  rolling back    */ { false, false, false, false, false, false, false, false, true,  true,  false, false, false, false, false  },
    /*  rolled back     */ { false, false, false, false, false, false, false, false, false, false, false, false, false, false, false  },
    // GDH: Added this row onwards
    /* commit_one_phas  */ { false, false, false, false , false, false, false, false, true,  false, true,  true,  true,  true,  true   },
    /* cmt_one_phse_ok  */ { false, false, false, false, false, false, false, true,  false, false, false, false, false, false, false  },
    /* cmt_one_phse_rb  */ { false, false, false, false, false, false, false, false, false, true,  false, false, false, false, false  },
    /* cmt_one_phse_hh  */ { false, false, false, false, false, false, false, true,  false, false, false, false, false, false, false  },
    /* cmt_one_phse_hm  */ { false, false, false, false, false, false, false, true,  false, false, false, false, false, false, false  }};

    // XID format identifier.

    final static int XID_FORMAT_ID = ('J'<<16) + ('T'<<8) + 'S';

    private final static String LOG_SECTION_NAME = "TS"/*#Frozen*/;

    /**
     * Default TransactionState constructor.
     *
     * @param
     *
     * @return
     *
     * @see
     */
    TransactionState() {}

    /**
     * This constructor is used for a root Coordinator.
     * It allocates a global identifier for the transaction, and a local
     * identifier for the local Transaction Service. The global ID is used to
     * propagate to other processes to identify the transaction globally. The
     * local ID is used for local comparisons. The CoordinatorLog object is
     * used to recover the TransactionState at restart. Use of this operation
     * indicates that the transaction is a top-level transaction. A section is
     * created in the given CoordinatorLog object to maintain the transaction
     * state persistently.
     *
     * @param log  The CoordinatorLog object for the transaction.
     *
     * @return
     *
     * @see
     */
    TransactionState(CoordinatorLog log) {

        // Get the sequence number for the transaction identifier.

        localTID = getSequenceNumber();

        // Get the epoch number for this execution of the server.

        //int epoch = getEpochNumber();

        // Create a global identifier.

        globalTID = new GlobalTID(XID_FORMAT_ID, 0,
                                  generateTID(localTID.longValue()));

        // Store the values in instance variables.
        // This is a top-level transaction.

        state = STATE_NONE;
        subordinate = false;

        // Set the CoordinatorLog id to the InternalTid.

        if (log != null) {
            logRecord = log;
            logRecord.setLocalTID(localTID);

            // Create a section in the CoordinatorLog for TransactionState

            logSection = logRecord.createSection(LOG_SECTION_NAME);
        }
    }

    /**
     * This constructor is used for a subordinate Coordinator.
     * It allocates a local identifier for a subordinate transaction that is
     * represented by the given global identifier, and returns it.
     * The presence of a CoordinatorLog object indicates
     * whether the transaction is a top-level
     * transaction. A section is created in the given CoordinatorLog object to
     * maintain the transaction state persistently.
     *
     * @param globalTID  The global identifier for the transaction.
     * @param log        The CoordinatorLog for a top-level transaction.
     *
     * @return
     *
     * @see
     */
    TransactionState(GlobalTID globalTID, CoordinatorLog log) {

        // Get the sequence number for the transaction identifier.

        this.globalTID = globalTID;
        localTID = getSequenceNumber();

        // Store the values in instance variables.

        state = STATE_NONE;
        subordinate = true;

        // Create a section in the CoordinatorLog.

        if (log != null) {
            logRecord = log;

            // Set the CoordinatorLog id to the InternalTid.

            logRecord.setLocalTID(localTID);

            // Create a section in the CoordinatorLog for TransactionState

            logSection = logRecord.createSection(LOG_SECTION_NAME);
        }
    }

    /**
     * This constructor is used for a root nested transaction.
     * It allocates a global identifier and local identifier
     * for a child transaction based on the local
     * and global identifiers given for the parent, and returns
     * them.  The use of this operation indicates that the transaction is a
     * subtransaction.
     *
     * @param parentLocalTID   The parent's local identifier.
     * @param parentGlobalTID  The parent's global identifier.
     *
     * @return
     *
     * @see
     */
    TransactionState(Long parentLocalTID, GlobalTID parentGlobalTID) {

        // Get the sequence number for the transaction identifier.

        localTID = getSequenceNumber();

        // Get the epoch number for this execution of the server.

        //int epoch = getEpochNumber();

        // Create a global identifier.

        globalTID = new GlobalTID(XID_FORMAT_ID, 0,
                                  generateTID(localTID.longValue()));

        // Store the values in instance variables. This is a subtransaction.

        state = STATE_NONE;
        subordinate = false;
    }

    /**
     * Directs the TransactionState to recover its state
     * after a failure, based on the given CoordinatorLog object.
     * If the TransactionState has already been defined or
     * recovered, the operation returns the current state of the transaction.
     * If the state cannot be recovered, the operation returns none.
     * If the CoordinatorLog records information prior to a log record being
     * forced, this may result in recovery of an in-flight transaction. The
     * TransactionState returns active in this case.
     *
     * @param log  The CoordinatorLog for the transaction.
     *
     * @return  The current state of the transaction.
     *
     * @see
     */
    int reconstruct(CoordinatorLog log) {

        int result = STATE_NONE;

        // Get a section id in the CoordinatorLog for TransactionState

        logSection = log.createSection(LOG_SECTION_NAME);
        byte[][] logData = log.getData(logSection);

        // Go through the sequence to get the overall status

        int logState = 0;
        for (int i = 0; i < logData.length; i++) {
            if (logData[i].length > 1) {
                logState |= (((logData[i][0] & 255) << 8) +
                             (logData[i][1] & 255));
            } else {
                // If the log record data is invalid, then exit immediately.
				_logger.log(Level.SEVERE,"jts.invalid_log_record_data",
                        LOG_SECTION_NAME);
				String msg = LogFormatter.getLocalizedMessage(_logger,
							"jts.invalid_log_record_data",
							new java.lang.Object[] { LOG_SECTION_NAME });
 				throw  new org.omg.CORBA.INTERNAL(msg);
            }
        }

        // Set the state value returned from the reconstruct method

        if ((logState & (1 << STATE_ROLLED_BACK)) != 0)
            result = STATE_ROLLED_BACK;
        else if ((logState & (1 << STATE_COMMITTED)) != 0)
            result = STATE_COMMITTED;
        else if ((logState & (1 << STATE_COMMITTING)) != 0)
            result = STATE_COMMITTING;
        else if ((logState & (1 << STATE_ROLLING_BACK)) != 0)
            result = STATE_ROLLING_BACK;
        else if ((logState & (1 << STATE_PREPARED_READONLY)) != 0)
            result = STATE_PREPARED_READONLY;
        else if ((logState & (1 << STATE_PREPARED_FAIL)) != 0)
            result = STATE_PREPARED_FAIL;
        else if ((logState & (1 << STATE_PREPARED_SUCCESS)) != 0)
            result = STATE_PREPARED_SUCCESS;
        // GDH new states-->
        else if ((logState & (1 << STATE_COMMITTING_ONE_PHASE)) != 0)
            result = STATE_COMMITTING_ONE_PHASE;
        else if ((logState & (1 << STATE_COMMITTED_ONE_PHASE_OK)) != 0)
            result = STATE_COMMITTED_ONE_PHASE_OK;
        else if ((logState & (1 << STATE_COMMIT_ONE_PHASE_ROLLED_BACK)) != 0)
            result = STATE_COMMIT_ONE_PHASE_ROLLED_BACK;
        else if ((logState &
                 (1 << STATE_COMMIT_ONE_PHASE_HEURISTIC_HAZARD)) != 0)
            result = STATE_COMMIT_ONE_PHASE_HEURISTIC_HAZARD;
        else if ((logState &
                 (1 << STATE_COMMIT_ONE_PHASE_HEURISTIC_MIXED)) != 0)
            result = STATE_COMMIT_ONE_PHASE_HEURISTIC_MIXED;

        state = result;
        subordinate = false;
        logRecord = log;

        return result;
    }

    /**
     * Sets the state to the given value and returns true.
     * If the state change is invalid, the state is not
     * changed and the operation returns false.  When a top-level
     * transaction has its state changed to prepared, the information
     * is stored in the CoordinatorLog object. When prepared_success, the log
     * record for the transaction is explicitly forced. Otherwise the
     * transaction will be treated as in-flight upon
     * recovery (i.e. will be rolled back). When
     * a subordinate transaction has its state changed to committing or
     * rolling_back, the state is added to the CoordinatorLog object, which is
     * then forced.
     *
     * @param newState  The new state of the transaction.
     *
     * @return  Indicates if the state change is possible.
     *
     * @see
     */
    boolean setState(int newState) {

        boolean result = false;

        // Check that the state change is valid

        if (validStateChange[state][newState]) {

            //Added code for counting and blocking.
            if(AdminUtil.bSampling)
            {
                switch ( newState )
                {
                    case STATE_PREPARED_SUCCESS :
                        AdminUtil.incrementPendingTransactionCount();
                        break ;
                    case STATE_PREPARED_READONLY :
                        AdminUtil.incrementPendingTransactionCount();
                        break ;
                    case STATE_COMMITTED : 
                        AdminUtil.incrementCommitedTransactionCount(); 
                        break ;
                    case STATE_ROLLED_BACK :
                        AdminUtil.incrementAbortedTransactionCount();
                        break ;
                    /*
		    case STATE_COMMITTED_ONE_PHASE_OK :
                        AdminUtil.incrementCommitedTransactionCount();
                        break ;
                    case STATE_COMMIT_ONE_PHASE_ROLLED_BACK :
                        AdminUtil.incrementCommitedTransactionCount();
                        break ;
		    */
		    case STATE_ROLLING_BACK :
                        AdminUtil.incrementUnpreparedAbortedTransactionCount();
                        break;
                }
            }


            //release the readlocks.
            switch ( state )
            {
                case STATE_PREPARING :
                case STATE_COMMITTING :

                case STATE_COMMITTING_ONE_PHASE : 
                    if(_logger.isLoggable(Level.FINEST)){
						String statestr=null;
						switch(newState ) {
						case STATE_PREPARING :
							statestr="PREPARING";
							break;
						case STATE_COMMITTING :
							statestr="COMMITTING";
							break;
						case STATE_COMMITTING_ONE_PHASE :
							statestr="COMMITTING_ONE_PHASE";
							break;
						default :
							statestr="Illegal state ";
							break;
						}
                        _logger.logp(Level.FINEST,"TransactionState","setState()",
								"Releasing read lock on freeze : state "+statestr);
                    } 
                    freezeLock.releaseReadLock();
                    if(_logger.isLoggable(Level.FINEST)){
						String statestr=null;
						_logger.logp(Level.FINEST,"TransactionState","setState()",
                         		"Released read lock on freeze");
					switch(newState ) {
						case STATE_PREPARING :
							statestr="PREPARING";
							break;
						case STATE_COMMITTING :
							statestr="COMMITTING";
							break;
						case STATE_COMMITTING_ONE_PHASE :
							statestr="COMMITTING_ONE_PHASE";
							break;
						default :
							statestr="Illegal state ";
							break;
						}
                        _logger.logp(Level.FINEST,"TransactionState","setState()",
								"Released read lock on freeze : state "+statestr);
                    } 
                    break;
            }

            //acquire read locks
            switch ( newState )
            {
                case STATE_PREPARING :
                case STATE_COMMITTING :
                //case STATE_ROLLING_BACK :
                case STATE_COMMITTING_ONE_PHASE :
                    if(_logger.isLoggable(Level.FINEST)){
						String statestr=null;
						switch(newState ) {
						case STATE_PREPARING :
							statestr="PREPARING";
							break;
						case STATE_COMMITTING :
							statestr="COMMITTING";
							break;
						case STATE_COMMITTING_ONE_PHASE :
							statestr="COMMITTING_ONE_PHASE";
							break;
						default :
							statestr="Illegal state ";
							break;
						}
                        _logger.logp(Level.FINEST,"TransactionState","setState()",
								"Acquiring read lock on freeze : state "+statestr);
                    }
                    freezeLock.acquireReadLock(); 
                    if(_logger.isLoggable(Level.FINEST)){
						String statestr=null;
						switch(newState ) {
						case STATE_PREPARING :
							statestr="PREPARING";
							break;
						case STATE_COMMITTING :
							statestr="COMMITTING";
							break;
						case STATE_COMMITTING_ONE_PHASE :
							statestr="COMMITTING_ONE_PHASE";
							break;
						default :
							statestr="Illegal state ";
							break;
						}
                        _logger.logp(Level.FINEST,"TransactionState","setState()",
								"Acquired read lock on freeze : state "+statestr);
                    }
                    break;
            }

            // RecoveryHook (for induced crashes and waits)  (Ram Jeyaraman)
            if (FailureInducer.isFailureInducerActive() &&
                    (!(Thread.currentThread().getName().
                        equals("JTS Resync Thread"/*#Frozen*/)))) {
                Integer failurePoint = null;
                switch (newState) {
                    case STATE_PREPARING :
                        failurePoint = FailureInducer.ACTIVE; break;
                    case STATE_PREPARED_SUCCESS :
                        failurePoint = FailureInducer.PREPARING; break;
                    case STATE_PREPARED_FAIL :
                        failurePoint = FailureInducer.PREPARING; break;
                    case STATE_PREPARED_READONLY :
                        failurePoint = FailureInducer.PREPARING; break;                        
                    case STATE_COMMITTING_ONE_PHASE :
                        failurePoint = FailureInducer.ACTIVE; break;
                    case STATE_COMMITTED_ONE_PHASE_OK :
                        failurePoint = FailureInducer.COMPLETING; break;
                    case STATE_COMMIT_ONE_PHASE_ROLLED_BACK :
                        failurePoint = FailureInducer.COMPLETING; break;
                    case STATE_COMMITTING :
                        failurePoint = FailureInducer.PREPARED; break;
                    case STATE_COMMITTED :
                        failurePoint = FailureInducer.COMPLETING; break;
                    case STATE_ROLLING_BACK :
                        if (state == STATE_PREPARED_SUCCESS) {
                            failurePoint = FailureInducer.PREPARED;
                        } else if (state == STATE_PREPARED_FAIL) {
                            failurePoint = FailureInducer.PREPARED;
                        } else if (state == STATE_PREPARED_READONLY) {
                            failurePoint = FailureInducer.PREPARED;
                        } else if (state == STATE_ACTIVE) {
                            failurePoint = FailureInducer.ACTIVE;
                        }
                        break;
                    case STATE_ROLLED_BACK :
                        failurePoint = FailureInducer.COMPLETING;
                }
                FailureInducer.waitForFailure(this.globalTID, failurePoint);
            }

            // Change state.  This is the point at which some
            // log records may be written

            state = newState;
            result = true;

            // Add state information to CoordinatorLog for various states.

            if (logRecord != null &&
                    (newState == STATE_PREPARED_SUCCESS ||
                     newState == STATE_PREPARED_FAIL ||
                     (newState == STATE_COMMITTING && subordinate) ||
                     (newState == STATE_ROLLING_BACK && subordinate) ||
                     newState == STATE_COMMITTED ||
                     newState == STATE_ROLLED_BACK ||
                     // GDH
                     // newState == STATE_COMMITTING_ONE_PHASE   ||
                     // newState == STATE_COMMITTED_ONE_PHASE_OK   ||
                     // newState == STATE_COMMIT_ONE_PHASE_ROLLED_BACK ||
                     newState == STATE_COMMIT_ONE_PHASE_HEURISTIC_HAZARD ||
                     newState == STATE_COMMIT_ONE_PHASE_HEURISTIC_MIXED
                     // GDH
                    )) {
                byte[] byteData = new byte[2];
                byteData[0] = (byte)(((1 << state) & 0xff00) >> 8);
                byteData[1] = (byte)( (1 << state) & 0x00ff);
                result = logRecord.addData(logSection, byteData);
            }

            // If the new state represents successful preparation,
            // and the transaction is a top-level transaction,
            //  or the new state indicates the beginning of commit or
            // rollback and the Coordinator is a subordinate, we want to make
            // sure that the log information is permanent.

            if (logRecord != null &&
                    (newState == STATE_PREPARED_SUCCESS ||
                     // GDH:  All the new states should be flushed to the log
                     //        cop as it represents begining of commit and
                     //        the others as they represent end of phase one
                     // (at least)
                      
                     newState == STATE_COMMIT_ONE_PHASE_HEURISTIC_HAZARD ||
                     newState == STATE_COMMIT_ONE_PHASE_HEURISTIC_MIXED ||
                     // \GDH
                     (newState == STATE_COMMITTING && subordinate) ||
                     (newState == STATE_ROLLING_BACK && subordinate))) {

                // If this is the first time a transaction has
                // gone in-doubt in this process, then ensure
                // that the restart required flag is set in the repository.

                setInDoubt(true);

                // Force the log record for the transaction.
                // How much 'force' can we use in Java?

                result = logRecord.write(true);
                /**
                if (!result) {
                    // empty
                }
                **/
            } else {
                if (newState == STATE_PREPARED_SUCCESS ||
                     newState == STATE_COMMIT_ONE_PHASE_HEURISTIC_HAZARD ||
                     newState == STATE_COMMIT_ONE_PHASE_HEURISTIC_MIXED ||
                     (newState == STATE_COMMITTING && subordinate) ||
                     (newState == STATE_ROLLING_BACK && subordinate)) {
                     setInDoubt(true);
                     // LogDBHelper.getInstance().addRecord(localTID.longValue(), globalTID.toTidBytes());
                }
	    }
               

            // If the new state represents completion of a
            // top-level transaction, a top-level transaction,
            // then write an unforced record to the log.

            if (logRecord != null && (newState == STATE_COMMITTED ||
                                      // newState == STATE_COMMITTING_ONE_PHASE   ||
                                      // newState == STATE_COMMITTED_ONE_PHASE_OK   ||
                                      // newState == STATE_COMMIT_ONE_PHASE_ROLLED_BACK ||
                                      newState == STATE_ROLLED_BACK)) {

                // Write the log record for the transaction (unforced).

                result = logRecord.write(false);
                /**
                if (!result) {
                    // empty
                }
                **/
            }

            // RecoveryHook (for induced crashes and waits)  (Ram Jeyaraman)
            if (FailureInducer.isFailureInducerActive() &&
                    (!(Thread.currentThread().getName().
                        equals("JTS Resync Thread"/*#Frozen*/)))) {
                Integer failurePoint = null;
                switch (newState) {
                    case STATE_COMMITTED_ONE_PHASE_OK :
                        failurePoint = FailureInducer.COMPLETED; break;
                    case STATE_COMMITTED :
                        failurePoint = FailureInducer.COMPLETED; break;
                    case STATE_ROLLED_BACK :
                        failurePoint = FailureInducer.COMPLETED;
                }
                FailureInducer.waitForFailure(this.globalTID, failurePoint);
            }
        }

        return result;
    }

    /**
     * Returns the current transaction sequence number and increments it.
     *
     * @param
     *
     * @return  The current transaction sequence number.
     *
     * @see
     */
    private static synchronized long getSequenceNumber() {

        return ++sequenceNumber;
    }

    /**
     * Returns the current epoch number.
     *
     * @param
     *
     * @return  The current epoch number.
     *
     * @see
     */
    /*private static int getEpochNumber() {

	
        int result = (int)epochNumber;
        return result;
    }*/

    /**
     * Returns a flag indicating whether any transactions may be in doubt.
     *
     * @param
     *
     * @return  The in doubt indicator.
     *
     * @see
     */
    static boolean inDoubt() {
        return inDoubt;
    }

    /**
     * Sets the in doubt indicator.
     *
     * @param value  The new value of the indicator.
     *
     * @return
     *
     * @see
     */
    static void setInDoubt(boolean value) {
        inDoubt = value;
    }

    /**
     * Generates the body of a transaction identifier.
     *
     * @param localTID    The local transaction identifier.
     * @param epoch       The epoch number.
     * @param serverName  The server name.
     *
     * @return
     *
     * @see
     */
    private static final byte[] generateTID(long localTID) {
        if(TIDTemplate==null){
	    synchronized(TransactionState.class){
                if(TIDTemplate==null){
                    String serverName = Configuration.getServerName();
                    int nameLength = (serverName == null ? 0 : serverName.length());
	            TIDTemplate = new byte[nameLength+8];
        
                    long epochNumber    = new Date().getTime();
                    TIDTemplate[4] = (byte) epochNumber;
                    TIDTemplate[5] = (byte)(epochNumber >> 8);
                    TIDTemplate[6] = (byte)(epochNumber >> 16);
                    TIDTemplate[7] = (byte)(epochNumber >> 24);

                    for( int i = 0; i < nameLength; i++ )
                        TIDTemplate[i+8] = (byte) serverName.charAt(i);
		}
	    }
	}
        byte[] result = new byte[TIDTemplate.length];
	System.arraycopy(TIDTemplate, 4, result, 4, TIDTemplate.length-4);

        result[0] = (byte) localTID;
        result[1] = (byte)(localTID >> 8);
        result[2] = (byte)(localTID >> 16);
        result[3] = (byte)(localTID >> 24);

        return result;
    }
}

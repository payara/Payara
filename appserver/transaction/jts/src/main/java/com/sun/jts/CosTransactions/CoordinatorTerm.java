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
// Module:      CoordinatorTerm.java
//
// Description: Client Coordinator termination.
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

import org.omg.CORBA.*;
import org.omg.CosTransactions.*;
import com.sun.jts.trace.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import com.sun.logging.LogDomains;
import com.sun.jts.utils.LogFormatter;

/**The CoordinatorTerm interface provides operations that allow an
 * Terminator to direct completion of a transaction without any dependency
 * on the Coordinator interface.
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

class CoordinatorTerm implements CompletionHandler {
    private CoordinatorImpl coordinator = null;
    private boolean         subtransaction = false;
    private boolean         aborted = false;
    private boolean         heuristicDamage = false;
    private boolean         completed = false;
    private boolean         completing = false;

	/*
		Logger to log transaction messages
	*/ 
	
	static Logger _logger = LogDomains.getLogger(CoordinatorTerm.class, LogDomains.TRANSACTION_LOGGER);

    /**Default CoordinatorTerm constructor.
     *
     * @param
     *
     * @return
     *
     * @see
     */
    CoordinatorTerm() {
    }

    /**Normal constructor.
     * <p>
     * Sets up the CoordinatorTerm with the Coordinator reference so that the
     * CoordinatorTerm can find the Coordinator to pass on the two-phase commit
     * messages.
     * <p>
     * A flag is passed to indicate whether the CoordinatorTerm
     * represents a subtransaction.
     *
     * @param coord    The Coordinator for the transaction.
     * @param subtran  The subtransaction indicator.
     *
     * @return
     *
     * @see
     */
    CoordinatorTerm( CoordinatorImpl coord,
                     boolean         subtran ) {

        // Set up the instance variables from the values passed.

        coordinator = coord;
        subtransaction = subtran;

        // Inform the Coordinator that this is the object that normally terminates
        // the transaction.

        if( coordinator != null )
            coordinator.setTerminator(this);

    }

    /**Informs the object that the transaction is to be committed.
     * <p>
     * Uses a private interface to pass the Terminator's commit request on to the
     * Coordinator.
     * <p>
     * This operation does not check for outstanding requests, that is done by
     * the Current operations (when the client is using direct interaction we
     * cannot provide checked behavior).
     * <p>
     * Before telling the Coordinator to commit, it is informed that the
     * transaction is about to complete, and afterwards it is told that the
     * transaction has completed, with an indication of commit or rollback.
     *
     * @param promptReturn  Indicates whether to return promptly.
     *
     * @return
     *
     * @exception TRANSACTION_ROLLEDBACK  The transaction could not be committed and
     *                                   has been rolled back.
     * @exception HeuristicHazard  Heuristic action may have been taken by a
     *                             participant in the transaction.
     * @exception HeuristicMixed  Heuristic action has been taken by a participant
     *                            in the transaction so part of the transaction has
     *                            been rolled back.
     * @exception SystemException  An error occurred calling another object.
     * @exception LogicErrorException  An internal logic error occurred.
     *
     * @see
     */
    void commit( boolean promptReturn )
        throws HeuristicMixed, HeuristicHazard, TRANSACTION_ROLLEDBACK,
        SystemException, LogicErrorException {

        // BUGFIX(Ram J) (12/16/2000) This was previously set to true, which  the
        // caused commit_one_phase exceptions not to be reported.
        boolean commit_one_phase_worked_ok = false;

        // If the transaction that this object represents has already been rolled
        // back, raise the TRANSACTION_ROLLEDBACK exception and return.
        // If the transaction completed with heuristic damage, report that.

        if( aborted ) {
            throw new TRANSACTION_ROLLEDBACK(0,CompletionStatus.COMPLETED_NO);
        }

        // If there is no Coordinator reference, raise an exception.

        if( coordinator == null ) {
			String msg = LogFormatter.getLocalizedMessage(_logger,
									  "jts.no_coordinator_available");
            LogicErrorException exc = new LogicErrorException(msg);
            throw exc;
        }

        // Remember that it is us that have initiated the completion so that we can
        // ignore the aborted operation if it happens subsequently.

        completing = true;

        // Determine the current Control object and Coordinator.  Determine whether
        // the current transaction is the same as that being committed.

        ControlImpl current = CurrentTransaction.getCurrent();
        ControlImpl establishedControl = null;
        Coordinator currentCoord = null;
        boolean sameCoordinator = false;

        if( current != null )
            try {
                if (Configuration.isLocalFactory()) {
                  currentCoord = current.get_localCoordinator();
                } else {
                  currentCoord = current.get_coordinator();
                }

                sameCoordinator = coordinator.is_same_transaction(currentCoord);
            } catch( Unavailable exc ) {}

        // We must ensure that there is a current transaction at this point if it is a
        // top-level transaction because the Synchronization objects expect to have
        // transaction context.
        // If this CoordinatorTerm was created by a Factory, then this commit will
        // not have context because the Terminator is not a transactional object.

        RuntimeException rte = null;
        if( !subtransaction ) {
            try {
                if( current == null ||
                    !sameCoordinator ) {
                    establishedControl = new ControlImpl(null,coordinator,
                                                         new GlobalTID(coordinator.getGlobalTID()),
                                                         coordinator.getLocalTID());
                    CurrentTransaction.setCurrent(establishedControl,true);
                }
            } catch( Throwable exc ) {
            }

            // Tell the Coordinator that the transaction is about to complete.

            try {
                ((TopCoordinator)coordinator).beforeCompletion();
            }

            // If the Coordinator raised an exception, return it to the caller.

            catch( SystemException exc ) {
                completing = false;
                throw exc;
            } catch (RuntimeException exc) {
                rte = exc;
            }

            // If a temporary context was established, end it now.

            finally {
                if( establishedControl != null ) {
                    CurrentTransaction.endCurrent(true);
                    establishedControl.doFinalize();
                }
            }
        }

        // End the association of the current Control object with the current thread
        // if it is the same transaction as the one being committed.
        // This is done here rather than in Current because the before completion
        // method must be invoked while the transaction is still active.

        Status status = Status.StatusCommitted;
        if( sameCoordinator )
            CurrentTransaction.endCurrent(true);


        // Now, process the actual 2PC

        Throwable heuristicExc = null;
        Throwable otherExc = null;

        // GDH Now see if we can legally call commit one phase
        // the commitOnePhase method will return false if this is
        // not possible.

        if (rte == null) {
            try {
                commit_one_phase_worked_ok = coordinator.commitOnePhase();
            } catch( Throwable exc ) {

                if( exc instanceof HeuristicHazard ||
                        exc instanceof HeuristicMixed ) {
                    heuristicExc = exc;
                } else if( exc instanceof TRANSACTION_ROLLEDBACK ) {
                    status = Status.StatusRolledBack;
                } else if( exc instanceof INVALID_TRANSACTION ) {
                    // Why have we driven before completion before now?
                    throw (INVALID_TRANSACTION)exc;
                } if (exc instanceof INTERNAL) {
                    // ADDED(Ram J) percolate any system exception
                    // back to the caller.
                    otherExc = exc; //throw (INTERNAL) exc;
                }else {
                }
            }
        }

        if( commit_one_phase_worked_ok ) {
            // Then we have done the commit already above
            // Set the status for the after completion call
            // even though currently the parm is not used
            // inside the call - it get's the state from transtate
            // the status variable here is a local flag.
            status = Status.StatusCommitted;  // GDH COPDEF1 Added this for neatness

        } else if (status != Status.StatusRolledBack) { // Added (Ram J) (12/06/2000)
            // commit two phase now

            // Get the prepare vote from the root Coordinator.
            Vote prepareResult = Vote.VoteRollback;

            if (rte == null) {
                try {
                    prepareResult = coordinator.prepare();
                } catch( HeuristicHazard exc ) {
                    heuristicExc = exc;
                } catch( HeuristicMixed exc ) {
                    heuristicExc = exc;
                } catch( INVALID_TRANSACTION exc ) {
                    otherExc = exc; //throw exc;
                } catch( INTERNAL exc ) {
                    otherExc = exc; //throw exc;
                } catch( Throwable exc ) {
                }
            }

            if( subtransaction ) {
                // Depending on the prepare result, commit or abort the transaction.

                if( prepareResult == Vote.VoteCommit )
                    try {
			
			if(_logger.isLoggable(Level.FINE))
			{
				_logger.logp(Level.FINE,"CoordinatorTerm","commit()",
						"Before invoking coordinator.commit() :"+"GTID is: "+
						((TopCoordinator)coordinator).superInfo.globalTID.toString());
				
			}
                        coordinator.commit();
                    } catch( NotPrepared exc ) {
                        prepareResult = Vote.VoteRollback;
                    }

                if( prepareResult == Vote.VoteRollback ) {
					if(_logger.isLoggable(Level.FINE))
					{
						 _logger.logp(Level.FINE,"CoordinatorTerm","commit()",
						 		"Before invoking coordinator.rollback :"+
								"GTID is : "+
								((TopCoordinator)coordinator).superInfo.globalTID.toString());
					}
                    coordinator.rollback(true);
                    status = Status.StatusRolledBack;
                }
            }

            // End of dealing with top-level transactions.

            else {

                // Depending on the prepare result, commit or abort the transaction.

                //$ DO SOMETHING ABOUT PROMPT RETURN  */

                try {
                    if( prepareResult == Vote.VoteCommit ) {
                        try {
                            coordinator.commit();
                        } catch( NotPrepared exc ) {
                            prepareResult = Vote.VoteRollback;
                        }
                    }

                    if( prepareResult == Vote.VoteRollback && heuristicExc == null ) {
                        status = Status.StatusRolledBack; // GDH COPDEF1 Swapped these two lines
                        coordinator.rollback(true);       // (not stricly necessary for problem fix)
                    }
                } catch( Throwable exc ) {
                    if (exc instanceof HeuristicHazard ||
                        exc instanceof HeuristicMixed) {
                        heuristicExc = exc;
                    }

                    // ADDED(Ram J) percolate any system exception
                    // back to the caller.
                    if (exc instanceof INTERNAL) {
                        otherExc = exc; //throw (INTERNAL) exc;
                    }
                }


            }   // end else was top level txn

        } // end else used two phase else

        // Tell the Coordinator that the transaction has completed (top-level
        // transactions only).
        if( !subtransaction ) {
            ((TopCoordinator)coordinator).afterCompletion(status);
        }

        // Inform the Control object that the transaction has completed.

        completed = true;
        if( current != null && sameCoordinator )
            current.setTranState(status);

        // If a heuristic was thrown, throw the exception.

        if( heuristicExc != null ) {
            if( heuristicExc instanceof HeuristicMixed )
                throw (HeuristicMixed)heuristicExc;
            else
                throw (HeuristicHazard)heuristicExc;
        } else if( otherExc != null ) {
            if (otherExc instanceof INVALID_TRANSACTION)
                throw (INVALID_TRANSACTION) otherExc;
            else if (otherExc instanceof INTERNAL)
                throw (INTERNAL) otherExc;
        }

        // If the transaction was rolled back, raise an exception.

        if( status == Status.StatusRolledBack ) {
            TRANSACTION_ROLLEDBACK exc = new TRANSACTION_ROLLEDBACK(0,CompletionStatus.COMPLETED_YES);
            if (rte != null) {
                exc.initCause(rte);
            }
            throw exc;
        }

    }

    /**Informs the object that the transaction is to be rolled back.
     * <p>
     * Uses a private interface to pass the Terminator's rollback request on to
     * the Coordinator.
     *
     * @param
     *
     * @return
     *
     * @exception HeuristicHazard  Heuristic action may have been taken by a
     *                             participant in the transaction.
     * @exception HeuristicMixed  Heuristic action has been taken by a participant
     *                            in the transaction so part of the transaction has
     *                            been committed.
     * @exception SystemException  An error occurred calling another object.
     * @exception LogicErrorException  An internal logic error occurred.
     *
     * @see
     */
    void rollback()
        throws HeuristicMixed, HeuristicHazard, SystemException,
        LogicErrorException {

        // If the transaction that this object represents has already been rolled
        // back, raise an exception.  Note that we cannot raise a heuristic exception
        // here as it is not in the OMG interface.

        if( aborted ) {
            TRANSACTION_ROLLEDBACK exc = new TRANSACTION_ROLLEDBACK(0,CompletionStatus.COMPLETED_NO);
            throw exc;
        }

        // If there is no Coordinator reference, raise an exception.

        if( coordinator == null ) {
			 String msg = LogFormatter.getLocalizedMessage(_logger,
			 							"jts.no_coordinator_available");
			 						   
            LogicErrorException exc = new LogicErrorException(msg);
            throw exc;
        }

        // Remember that it is us that have initiated the completion so that we can
        // ignore the aborted operation if it happens subsequently.

        Coordinator currentCoord = null;
        ControlImpl current = null;

        completing = true;

        // End the association of the current Control object with the current thread
        // if it is the same transaction as the one being committed.

        try {
            current = CurrentTransaction.getCurrent();
        } catch( Throwable exc ) {}

        if( current != null )
            try {
                if (Configuration.isLocalFactory()) {
                  currentCoord = current.get_localCoordinator();
                } else {
                  currentCoord = current.get_coordinator();
                }

                if( coordinator.is_same_transaction(currentCoord) )
                    CurrentTransaction.endCurrent(true);
            }

        // If an exception was raised, it must be because the transaction that the
        // current Control object represents was rolled back.  In that case, always
        // end the current thread association.

        catch( Throwable exc ) {
            CurrentTransaction.endCurrent(true);
        }

        // Rollback the transaction and inform synchronisation objects.

        Throwable heuristicExc = null;
        try {
            coordinator.rollback(true);
        }
        catch (Throwable exc) {
            if (exc instanceof HeuristicHazard ||
                exc instanceof HeuristicMixed) {
                   heuristicExc = exc;
            }

            // ADDED (Ram J) percolate any system exception to the caller
            if (exc instanceof INTERNAL) {
                throw (INTERNAL) exc;
            }
        } finally {
            if( !subtransaction )
                ((TopCoordinator)coordinator).afterCompletion(Status.StatusRolledBack);
        }

        // Inform the Control object that the transaction has completed.

        completed = true;
        if( current != null )
            current.setTranState(Status.StatusRolledBack);

        // If a heuristic was thrown, throw the exception.

        if( heuristicExc != null ) {
            if( heuristicExc instanceof HeuristicMixed )
                throw (HeuristicMixed)heuristicExc;
            else
                throw (HeuristicHazard)heuristicExc;
        }

        // Otherwise return normally.

    }

    /**Informs the CoordinatorTerm object that the transaction it represents
     * has completed.
     * <p>
     * Flags indicate whether the transaction aborted, and
     * whether there was heuristic damage.
     * <p>
     * This operation is invoked by a Coordinator when it is rolled back,
     * potentially by a caller other than the CoordinatorTerm itself.  In the
     * event that it is some other caller, the CoordinatorTerm object performs the
     * after rollback synchronisation, and remembers the fact that it has aborted
     * for later.
     *
     * @param aborted          Indicates whether the transaction locally aborted.
     * @param heuristicDamage  Indicates local heuristic damage.
     *
     * @return
     *
     * @see
     */

    public void setCompleted( boolean aborted,
                              boolean heuristicDamage ) {

        // If the transaction that this object represents has already been rolled
        // back, or is being rolled back by this object, just return.

        if( !completing ) {

            // If there is no Coordinator reference, we cannot do anything.  Note that
            // CompletionHandler does not permit an exception through this method so we
            // cannot throw one.

            // Set the flags and distribute after completion operations for the
            // transaction.

            completed = true;
            this.aborted = aborted;
            this.heuristicDamage = heuristicDamage;

            if( coordinator == null ) {
            } else if( !subtransaction )
                ((TopCoordinator)coordinator).afterCompletion(Status.StatusRolledBack);
        }

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
    }

}


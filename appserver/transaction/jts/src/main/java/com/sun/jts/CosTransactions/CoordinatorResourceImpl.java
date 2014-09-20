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
// Module:      CoordinatorResourceImpl.java
//
// Description: Subordinate Coordinator participation interface.
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
import org.omg.PortableServer.*;
import org.omg.PortableServer.POAPackage.ServantAlreadyActive;
import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.CosTransactions.*;

import com.sun.jts.codegen.otsidl.*;

import com.sun.jts.trace.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import com.sun.logging.LogDomains;
import com.sun.jts.utils.LogFormatter;

/**The CoordinatorResourceImpl interface provides operations that allow a
 * Coordinator to be represented among the registered Resources of a
 * superior Coordinator, without requiring that the Coordinator support the
 * Resource interface.
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
//                Improved behaviour post recovery occurring
//-----------------------------------------------------------------------------

class CoordinatorResourceImpl extends CoordinatorResourcePOA
        implements CompletionHandler {
    private static POA poa = null;
    private static boolean recoverable = false;
    private CoordinatorResource thisRef = null;

    /**This flag may be set to indicate that the transaction is being forced.
     */
    boolean beingForced = false;

    private GlobalTID globalTID = null;
    private boolean   subtransaction = false;
    private boolean   aborted = false;
    private boolean   heuristicDamage = false;
    private boolean   completed = false;
    private boolean   setAsTerminator = false;

	/*
		Logger to log transaction messages
	*/
	
	static Logger _logger = LogDomains.getLogger(CoordinatorResourceImpl.class, LogDomains.TRANSACTION_LOGGER);
    /**Normal constructor.
     * <p>
     * Sets up the CoordinatorResourceImpl with the Coordinator reference and the
     * local transaction identifier so that the Resource can always find the
     * Coordinator to pass on the two-phase commit messages.
     * <p>
     * A flag is passed to indicate whether the CoordinatorResourceImpl represents
     * a subtransaction.
     *
     * @param globalTID  The global transaction identifier.
     * @param coord      The Coordinator for the transaction.
     * @param subtran    Subtransaction indicator.
     *
     * @return
     *
     * @see
     */
    CoordinatorResourceImpl( GlobalTID       globalTID,
                             CoordinatorImpl coord,
                             boolean         subtran ) {


        // Set up the instance variables to those values passed in.

        subtransaction = subtran;
        this.globalTID = globalTID;

        // Inform the Coordinator that this is the object that normally terminates
        // the transaction.

        if( coord != null )
            coord.setTerminator(this);

    }

    /**Informs the CoordinatorResourceImpl object that the transaction it
     * represents has completed.
     * <p>
     * Flags indicate whether the transaction aborted, and whether there was
     * heuristic damage.
     *
     * @param aborted          Indicates the transaction aborted.
     * @param heuristicDamage  Indicates heuristic damage occurred.
     *
     * @return
     *
     * @see CompletionHandler
     */
    public void setCompleted( boolean aborted,
                              boolean heuristicDamage ) {

        // Record the information.

        completed = true;
        this.aborted = aborted;
        this.heuristicDamage = heuristicDamage;

    }

    /**Requests the prepare phase vote from the object.
     * <p>
     * This uses a private interface to pass the superior Coordinator's prepare request.
     * on to the Coordinator that registered the CoordinatorResourceImpl.
     * <p>
     * The result from the Coordinator is returned to the caller.
     *
     * @param
     *
     * @return  The Coordinators vote.
     *
     * @exception SystemException  The operation failed.
     * @exception HeuristicMixed  Indicates that a participant voted to roll the
     *   transaction back, but one or more others have already heuristically committed.
     * @exception HeuristicHazard  Indicates that a participant voted to roll the
     *   transaction back, but one or more others may have already heuristically
     *   committed.
     *
     * @see Resource
     */
    public Vote prepare()
        throws SystemException, HeuristicMixed, HeuristicHazard {


        Vote result = Vote.VoteRollback;

        // If no global identifier has been set up, we can do nothing but vote for
        // the transaction to be rolled back.

        if( globalTID == null ) {
            INTERNAL exc = new INTERNAL(MinorCode.NoGlobalTID,
                                        CompletionStatus.COMPLETED_NO);
            throw exc;
        }

        // Prepare operations should only come in for top-level transactions.

        if( subtransaction ) {

            INTERNAL exc = new INTERNAL(MinorCode.TopForSub,
                                        CompletionStatus.COMPLETED_NO);
            throw exc;
        }

        // If the transaction that this object represents has already been rolled
        // back, return a rollback vote.

        if (completed) {
            if (aborted) {
                result = Vote.VoteRollback;
            } else {
                result = Vote.VoteCommit;
            }
        } else {

            // Look up the Coordinator for the transaction.

            TopCoordinator coord = (TopCoordinator)RecoveryManager.getCoordinator(globalTID);

            // If there is a Coordinator, lock it for the duration of this operation.
            // If there is no Coordinator, return a rollback vote.

            if( coord != null )
                synchronized( coord ) {

                    // Get the Coordinator's vote.
                    // If the Coordinator throws HeuristicMixed or HeuristicHazard,
                    // allow them to percolate to the caller.

                    result = coord.prepare();

                    // If the Coordinator has voted to roll the transaction back, then this
                    // CoordinatorResourceImpl will not be called again.  Ensure that the
                    // Coordinator has rolled back.
                    // If the Coordinator throws HeuristicMixed or HeuristicHazard,
                    // allow them to percolate to the caller.

                    if( result == Vote.VoteRollback )
                        coord.rollback(false);
                }
        }

        // If the Coordinator has voted to roll the transaction back, then this
        // CoordinatorResourceImpl will not be called again.
        // If a heuristic exception was thrown, this will be done in forget.

        if (result == Vote.VoteRollback) {
            destroy();
        }

        return result;
    }

    /**Informs the object that the transaction is to be committed.
     * <p>
     * Passes the superior Coordinator's commit request on to the Coordinator that
     * registered the CoordinatorResourceImpl, using a private interface.
     * <p>
     * If the Coordinator does not raise any heuristic exception, the
     * CoordinatorResourceImpl destroys itself.
     *
     * @param
     *
     * @return
     *
     * @exception HeuristicRollback  The transaction has already been rolled back.
     * @exception HeuristicMixed  At least one participant in the transaction has
     *                            rolled back its changes.
     * @exception HeuristicHazard  At least one participant in the transaction may
     *                             have rolled back its changes.
     * @exception NotPrepared  The transaction has not yet been prepared.
     * @exception SystemException  An error occurred.  The minor code indicates
     *                             the reason for the exception.
     *
     * @see Resource
     */
    public void commit()
        throws HeuristicRollback, HeuristicMixed, HeuristicHazard, NotPrepared,
        SystemException {

        // If no global identifier has been set up, we can do nothing.

        if( globalTID == null ) {
            INTERNAL exc = new INTERNAL(MinorCode.NoGlobalTID,
                                        CompletionStatus.COMPLETED_NO);
            throw exc;
        }

        // Commit operations should only come in for top-level transactions.

        if( subtransaction ) {
            INTERNAL exc = new INTERNAL(MinorCode.TopForSub,
                                        CompletionStatus.COMPLETED_NO);
            throw exc;
        }

        // If the transaction that this object represents has already been completed,
        // raise a heuristic exception if necessary.  This object must wait for a
        // forget before destroying itself if it returns a heuristic exception.

        if( completed ) {
            if( aborted ) {
                heuristicDamage = true;
                HeuristicRollback exc = new HeuristicRollback();
                throw exc;
            } else if( heuristicDamage ) {
                HeuristicMixed exc = new HeuristicMixed();
                throw exc;
            }
        } else {

            // Look up the Coordinator for the transaction.

            // GDH: First of all make sure it has been recovered if necessary
	    if(_logger.isLoggable(Level.FINE))
	    {
			_logger.logp(Level.FINE,"CoordinatorResourceImpl","commit()",
					"Before invoking RecoveryManager.waitForRecovery():"+
					"GTID is: "+ globalTID.toString());

	    }
            RecoveryManager.waitForRecovery();

            TopCoordinator coord = (TopCoordinator)RecoveryManager.getCoordinator(globalTID);

            // If there is a Coordinator, lock it for the duration of this operation.
            // Tell the Coordinator to commit.
            // If the Coordinator throws HeuristicMixed or HeuristicHazard,
            // allow them to percolate to the caller.

            if( coord != null )
                synchronized( coord ) {
                    // GDH:
                    // Make sure the coordinator knows we are it's terminator
                    // (this is done here in case we are in a recovery situation)
                    // (the operation has been moved from the constructor of the
                    // this object to here as the constructor is now called
                    // to early to ensure the coordinator is present in all
                    // cases

                    // GDH:
                    // If the transaction has completed at this point the
                    // makeSureSetAsTerminator method will be unable to
                    // set the coordinators terminator and will throw
                    // an object not exist exception: This is what we want in this case.

                    makeSureSetAsTerminator();
                    coord.commit();
                }
        }

        // If and we are not being forced, we can destroy ourselves before returning.
        // Otherwise, the TopCoordinator will have called setCompleted to set up
        // the information we need should a subsequent commit or rollback request
        // arrive.

        if( !beingForced )
            destroy();

    }

    /**Informs the object that the transaction is to be committed in one phase.
     * <p>
     * Passes the superior Coordinator's single-phase commit request on to the
     * Coordinator that registered the CoordinatorResourceImpl, using a private
     * interface.
     * <p>
     * The result from the Coordinator is returned to the caller. If the
     * Coordinator did not raise any heuristic exception, the CoordinatorResourceImpl
     * destroys itself.
     *
     * @param
     *
     * @return
     *
     * @exception TRANSACTION_ROLLEDBACK  The transaction could not be committed and
     *                                   has been rolled back.
     * @exception HeuristicHazard  One or more resources in the transaction may have
     *                             rolled back.
     * @exception SystemException  An error occurred.  The minor code indicates
     *                             the reason for the exception.
     *
     * @see Resource
     */
    public void commit_one_phase()
        throws TRANSACTION_ROLLEDBACK, HeuristicHazard, SystemException {

        boolean rolledBack;


        // If no global_identifier has been set up, we can do nothing.

        if( globalTID == null ) {
            INTERNAL exc = new INTERNAL(MinorCode.NoGlobalTID,
                                        CompletionStatus.COMPLETED_NO);
            throw exc;
        }

        // Commit operations should only come in for top-level transactions.

        if( subtransaction ) {
            INTERNAL exc = new INTERNAL(MinorCode.TopForSub,
                                        CompletionStatus.COMPLETED_NO);
            throw exc;
        }

        // If the transaction that this object represents has already been completed,
        // raise an exception if necessary.

        if( completed ) {
            if( aborted ) {
                TRANSACTION_ROLLEDBACK exc = new TRANSACTION_ROLLEDBACK(0,CompletionStatus.COMPLETED_NO);
                throw exc;
            }
        } else {

            // Look up the Coordinator for the transaction.

            // GDH: First of all make sure it has been recovered if necessary
	    if(_logger.isLoggable(Level.FINE))
	    {
			_logger.logp(Level.FINE,"CoordinatorResourceImpl","commit_one_phase()",
					"Before invoking RecoveryManager.waitForRecovery(): "+
					"GTID is: " + globalTID.toString());
	    }
            RecoveryManager.waitForRecovery();

            TopCoordinator coord = (TopCoordinator)RecoveryManager.getCoordinator(globalTID);

            // If there is a Coordinator, lock it for the duration of this operation.
            // Tell the Coordinator to commit, by first doing a prepare.
            // If the Coordinator throws HeuristicMixed or HeuristicHazard,
            // allow them to percolate to the caller.

            if( coord != null ) {
                rolledBack = false;

                synchronized( coord ) {

                    // GDH:
                    // Make sure the coordinator knows we are it's terminator
                    // (this is done here in case we are in a recovery situation)
                    // (the operation has been moved from the constructor of the
                    // this object to here as the constructor is now called
                    // to early to ensure the coordinator is present in all
                    // cases
                    makeSureSetAsTerminator();

                    // The prepare operation may throw the HeuristicHazard exception.  In this case
                    // allow it to go back to the caller.

                    Vote vote;
                    try {
                        vote = coord.prepare();
                    } catch( HeuristicMixed exc ) {
                        throw new HeuristicHazard();
                    }
                    try {
                        if( vote == Vote.VoteCommit )
                            coord.commit();
                        else if (vote == Vote.VoteRollback) {
                            // COMMENT (Ram J) 12/11/2000 The above if check
                            // for rollback was added to avoid throwing a
                            // rollback exception for readonly votes.
                            coord.rollback(true);
                            rolledBack = true;
                        }
                    } catch (Throwable exc) {
                        // ADDED(Ram J) percolate any system exception
                        // back to the caller.
                        if (exc instanceof INTERNAL) {
                            destroy();
                            throw (INTERNAL) exc;
                        }
                    }
                }

            } else {
                // coord is null
                // We could still find no coord that matched even after recovery
                // If there is still no coord then the process failed between
                // deleting the subordinate coordinator and the result being received
                // in the superior.   As all of the recoverable work occurred in this
                // server or in its subordinate servers (we are in cop), from a data integrity point of
                // view, it does not matter what result the CoordinatorResource returns
                // to the superior.  However, the superior may still have a client attached
                // and it would look strange if the transaction actually rolled back and
                // the TRANSACTION_ROLLEDBACK exception was not reported to the client.
                // Therefore we jump through hoops to give the best answer as possible.
                // By delaying the call to forget_superior until CoordinatorResource's
                // destructor we ensure the Coordinator and the CoordinatorResource are
                // deleted together (as much as is possible in the JAVA/CORBA environment) and
                // this occurs after the response has been sent to the superior (because
                // the ORB is holding a reference count to the CoordinatorResource).
                // Therefore, the most likely reason for this to occur is that the
                // Coordinator was timed out (and removed) just before the server
                // terminated.  In this case, the transaction rolled back, which is the
                // response that will be returned.

                rolledBack = true;

            }

            if( rolledBack ) {
                destroy();
                TRANSACTION_ROLLEDBACK exc = new TRANSACTION_ROLLEDBACK(0,CompletionStatus.COMPLETED_YES);
                throw exc;
            }
        }

        // Self-destruct before returning.

        destroy();

    }

    /**Informs the object that the transaction is to be rolled back.
     * <p>
     * Passes the superior Coordinator's rollback request on to the Coordinator
     * that registered the CoordinatorResourceImpl, using a private interface.
     * <p>
     * If the Coordinator does not raise any heuristic exception, the
     * CoordinatorResourceImpl destroys itself.
     *
     * @param
     *
     * @return
     *
     * @exception HeuristicCommit  The transaction has already been committed.
     * @exception HeuristicMixed  At least one participant in the transaction has
     *                            committed its changes.
     * @exception HeuristicHazard  At least one participant in the transaction may
     *                             have rolled back its changes.
     * @exception SystemException  An error occurred.  The minor code indicates
     *                             the reason for the exception.
     *
     * @see Resource
     *///----------------------------------------------------------------------------
         public void rollback()
             throws HeuristicCommit, HeuristicMixed,
             HeuristicHazard, SystemException {

             // If no global identifier has been set up, we can do nothing.

             if( globalTID == null ) {
                 INTERNAL exc = new INTERNAL(MinorCode.NoGlobalTID,
                                             CompletionStatus.COMPLETED_NO);
                 throw exc;
             }

             // Rollback operations should only come in for top-level transactions.

             if( subtransaction ) {
                 INTERNAL exc = new INTERNAL(MinorCode.TopForSub,
                                             CompletionStatus.COMPLETED_NO);
                 throw exc;
             }

             // If the transaction that this object represents has already been completed,
             // raise a heuristic exception if necessary.  This object must wait for a
             // forget before destroying itself if it returns a heuristic exception.

             if( completed ) {
                 if( !aborted ) {
                     heuristicDamage = true;
                     HeuristicCommit exc = new HeuristicCommit();
                     throw exc;
                 }
                 else if( heuristicDamage ) {
                     HeuristicMixed exc = new HeuristicMixed();
                     throw exc;
                 }
             } else {

                 // Look up the Coordinator for the transaction.

                 // GDH: First of all make sure it has been recovered if necessary
	    	if(_logger.isLoggable(Level.FINE))
	    	{
				_logger.logp(Level.FINE,"CoordinatorResourceImpl","rollback()",
						"Before invoking RecoveryManager.waitForRecovery(): "+
						"GTID is : "+ globalTID.toString()); 
			
	    	}
                 RecoveryManager.waitForRecovery();

                 TopCoordinator coord = (TopCoordinator)RecoveryManager.getCoordinator(globalTID);

                 // If there is a Coordinator, lock it for the duration of this operation.
                 // Tell the Coordinator to rollback.
                 // If the Coordinator throws HeuristicMixed or HeuristicHazard,
                 // allow them to percolate to the caller.

                 if( coord != null )
                     synchronized( coord ) {

                         // GDH:
                         // Make sure the coordinator knows we are it's terminator
                         // (this is done here in case we are in a recovery situation)
                         // (the operation has been moved from the constructor of the
                         // this object to here as the constructor is now called
                         // to early to ensure the coordinator is present in all
                         // cases
                         makeSureSetAsTerminator();
                         coord.rollback(true);
                     }
             }

             // If we are not being forced, we can destroy ourselves before returning.
             // Otherwise, the TopCoordinator will have called set_completed to set up
             // the information we need should a subsequent commit or rollback request
             // arrive.

             if( !beingForced )
                 destroy();

         }

    /**Informs the object that the transaction is to be forgotten.
     * <p>
     * Informs the CoordinatorResourceImpl that it does not need to retain heuristic
     * information any longer.
     *
     * @param
     *
     * @return
     *
     * @exception  SystemException  An error occurred.  The minor code indicates
     *                              the reason for the exception.
     *
     * @see
     */
    public void forget() throws SystemException {

        // Forget operations should only come in for top-level transactions.

        if( subtransaction ) {
            INTERNAL exc = new INTERNAL(MinorCode.TopForSub,
                                        CompletionStatus.COMPLETED_NO);
            throw exc;
        }

        // If no exception is raised, we can destroy ourselves before returning.

        destroy();
    }

    /**Informs the object that the subtransaction is to be committed.
     * <p>
     * Passes the superior Coordinator's commit request on to the SubCoordinator
     * that registered the CoordinatorResourceImpl, using a private interface.
     * <p>
     * The result from the SubCoordinator is returned to the caller. The
     * CoordinatorResourceImpl destroys itself.
     *
     * @param parent  The parent Coordinator reference.
     *
     * @return
     *
     * @exception TRANSACTION_ROLLEDBACK  The transaction could not be committed
     *                                   and some parts may have rolled back.
     * @exception SystemException  An error occurred.  The minor code indicates
     *                             the reason for the exception.
     *
     * @see
     */
    public void commit_subtransaction( Coordinator parent )
        throws TRANSACTION_ROLLEDBACK, SystemException {

        // If no global identifier has been set up, we can do nothing.

        if( globalTID == null ) {
            INTERNAL exc = new INTERNAL(MinorCode.NoGlobalTID,
                                        CompletionStatus.COMPLETED_NO);
            throw exc;
        }

        // Commit_subtransaction operations should only come in for subtransactions.

        if( !subtransaction ) {
            INTERNAL exc = new INTERNAL(MinorCode.SubForTop,
                                        CompletionStatus.COMPLETED_NO);
            throw exc;
        }

        // If the transaction that this object represents has already been rolled
        // back, raise the TRANSACTION_ROLLEDBACK exception.

        if( completed ) {
            if( aborted ) {
                destroy();
                TRANSACTION_ROLLEDBACK exc = new TRANSACTION_ROLLEDBACK(0,CompletionStatus.COMPLETED_YES);
                throw exc;
            }
        } else {

            // Look up the Coordinator for the subtransaction.

            SubCoordinator coord = (SubCoordinator)RecoveryManager.getCoordinator(globalTID);

            // If there is a Coordinator, lock it for the duration of this operation.
            // Tell the Coordinator to prepare then commit.
            // Record then ignore any exceptions - we do not expect heuristics during
            // subtransaction completion.

            if( coord != null ) {
                boolean rolledBack = false;

                synchronized( coord ) {
                    try {
                        if( coord.prepare() == Vote.VoteCommit )
                            coord.commit();
                        else {
                            coord.rollback(true);
                            rolledBack = true;
                        }
                    }
                    catch( Throwable ex ) {
                    }
                }

                if( rolledBack ) {
                    destroy();
                    TRANSACTION_ROLLEDBACK exc = new TRANSACTION_ROLLEDBACK(0,CompletionStatus.COMPLETED_YES);
                    throw exc;
                }
            }
        }

        // Destroy ourselves before returning (there is no heuristic information for
        // subtransactions).

        destroy();

    }

    /**Informs the object that the subtransaction is to be rolled back.
     * <p>
     * Passes the superior Coordinator's rollback request on to the SubCoordinator
     * that registered the CoordinatorResourceImpl, using a private interface.
     * <p>
     * The result from the SubCoordinator is returned to the caller. The
     * CoordinatorResourceImpl destroys itself.
     *
     * @param
     *
     * @return
     *
     * @exception SystemException  An error occurred.  The minor code indicates
     *                             the reason for the exception.
     *
     * @see
     */

    public void rollback_subtransaction() throws SystemException {

        // If no global identifier has been set up, we can do nothing.

        if( globalTID == null ) {
            INTERNAL exc = new INTERNAL(MinorCode.NoGlobalTID,
                                        CompletionStatus.COMPLETED_NO);
            throw exc;
        }

        // Commit_subtransaction operations should only come in for subtransactions.

        if( !subtransaction ) {
            INTERNAL exc = new INTERNAL(MinorCode.SubForTop,
                                        CompletionStatus.COMPLETED_NO);
            throw exc;
        }

        // If the transaction that this object represents has already been completed,
        // do nothing.

        if( !completed ) {

            // Look up the Coordinator for the transaction.

            SubCoordinator coord = (SubCoordinator)RecoveryManager.getCoordinator(globalTID);

            // If there is a Coordinator, lock it for the duration of this operation.
            // Tell the Coordinator to rollback.

            if( coord != null )
                synchronized( coord ) {
                    coord.rollback(true);
                }
        }

        // Destroy ourselves before returning (there is no heuristic information for
        // subtransactions).

        destroy();

    }

    /**Creates the CoordinatorResourceImpl with the given key.  This is done when
     * the CoordinatorResource object is recreated after the server has been
     * restarted.
     *
     * @param key  The key for the object.
     *
     * @return
     *
     * @see
     */
    CoordinatorResourceImpl( byte[] key ) {

        // Get the global transaction identifier from the key.

        globalTID = new GlobalTID(key);

        // GDH
        // Ensure that recovery has completed so that we can get the Coordinator.
        // This is now delayed until the 2PC methods for normal resync and is
        // done as part of the superinfo.reconst

        //    RecoveryManager.waitForRecovery();

        // Check that the global transaction identifier represents a known transaction.
        // If it does not, then throw the OBJECT_NOT_EXIST exception.

        //    CoordinatorImpl coord = RecoveryManager.getCoordinator(globalTID);
        //    if( coord == null )
        //      {
        //      OBJECT_NOT_EXIST exc = new OBJECT_NOT_EXIST();
        //      if( trc != null ) trc.event(EVT_THROW).data(exc).write();
        //      throw exc;
        //      }

        // If the transaction is known, then inform the Coordinator that this is the
        // terminator for it.

        //    else
        //      coord.setTerminator(this);

        // Leave other members at the default values.

    }

    /**Returns the CORBA Object which represents this object.
     *
     * @param
     *
     * @return  The CORBA object.
     *
     * @see
     */
    CoordinatorResource object() {
        if( thisRef == null ) {
            if( poa == null ) {
                poa = Configuration.getPOA("CoordinatorResource"/*#Frozen*/);
                recoverable = Configuration.isRecoverable();
            }

            try {
                byte[] id = null;
                if (recoverable && globalTID != null) {
                    id = globalTID.toBytes();
                    poa.activate_object_with_id(id, this);
                    org.omg.CORBA.Object obj = poa.create_reference_with_id(id, CoordinatorResourceHelper.id());
                    thisRef = CoordinatorResourceHelper.narrow(obj);
                    //thisRef = (CoordinatorResource) this;
                } else {
                    poa.activate_object(this);
                    org.omg.CORBA.Object obj = poa.servant_to_reference(this);
                    thisRef = CoordinatorResourceHelper.narrow(obj);
                    //thisRef = (CoordinatorResource) this;
                }
            } catch( ServantAlreadyActive saexc ) {
				_logger.log(Level.SEVERE,
						"jts.create_CoordinatorResource_object_error",saexc);
				String msg = LogFormatter.getLocalizedMessage(_logger,
										 "jts.create_CoordinatorResource_object_error");
				throw  new org.omg.CORBA.INTERNAL(msg);
						
            } catch( ServantNotActive snexc ) {
				_logger.log(Level.SEVERE,
						"jts.create_CoordinatorResource_object_error",snexc);
				String msg = LogFormatter.getLocalizedMessage(_logger,
										 "jts.create_CoordinatorResource_object_error");
				throw  new org.omg.CORBA.INTERNAL(msg);
						
            } catch( Exception exc ) {
				_logger.log(Level.SEVERE,
						"jts.create_CoordinatorResource_object_error",exc);
				String msg = LogFormatter.getLocalizedMessage(_logger,
										 "jts.create_CoordinatorResource_object_error");
				throw  new org.omg.CORBA.INTERNAL(msg);
						
            }
        }

        return thisRef;
    }

    /**Destroys the CoordinatorResourceImpl object.
     *
     * @param
     *
     * @return
     *
     * @see
     */
    void destroy() {

        try {
            if (poa != null && thisRef != null) {
                poa.deactivate_object(poa.reference_to_id(thisRef));
                thisRef = null;
            } else {
                // BUGFIX(Ram J) It is possible that the
                // CoordinatorResource object was activated via the activation
                // daemon. In that case, there is no guarantee
                // that poa and thisRef are set to a meaningful value.
                // So, try to deactivate the CoordinatorResource object anyway.

                POA crPoa = null;
                if (poa == null) {
                    crPoa = Configuration.getPOA("CoordinatorResource"/*#Frozen*/);
                } else {
                    crPoa = poa;
                }
                if (thisRef == null) {
                    crPoa.deactivate_object(crPoa.servant_to_id(this));
                } else {
                    crPoa.deactivate_object(crPoa.reference_to_id(thisRef));
                    thisRef = null;
                }
            }
        } catch( Exception exc ) {
			 _logger.log(Level.WARNING,"jts.object_destroy_error","CoordinatorResource");
				
        }

        globalTID = null;
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

    /**Makes sure this object is set as the Coordinator Terminator
     *
     * @param
     *
     * @return
     *
     * @see
     */
    void makeSureSetAsTerminator() {

        if( !setAsTerminator ) {

            CoordinatorImpl coord = RecoveryManager.getCoordinator(globalTID);

            // If the transaction is not known then throw an exception,
            // otherwise inform the Coordinator that this is the
            // terminator for it.

            if( coord == null ) {
                OBJECT_NOT_EXIST exc = new OBJECT_NOT_EXIST();
                throw exc;
            } else {
                coord.setTerminator(this);
                setAsTerminator = true;
            }
        }

    }  // MakeSureSetAsTerminator method

    } // class


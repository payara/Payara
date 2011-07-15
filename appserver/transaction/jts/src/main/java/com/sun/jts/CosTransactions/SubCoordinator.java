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
// Module:      SubCoordinator.java
//
// Description: Nested transaction Coordinator object implementation.
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
import java.util.logging.Logger;
import java.util.logging.Level;
import com.sun.logging.LogDomains;
import com.sun.jts.utils.LogFormatter;

/**
 * The SubCoordinator interface is our implementation of the standard
 * Coordinator interface that is used for subtransactions. It allows
 * SubtransactionAwareResources to be registered for participation in a
 * subtransaction. As an instance of this class may be accessed from multiple
 * threads within a process, serialisation for thread-safety is necessary in
 * the implementation. The information managed does not need to be
 * reconstructible in the case of a failure as subtransactions are not
 * durable.
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

class SubCoordinator extends CoordinatorImpl {
    String              name = null;
    RegisteredResources participants = null;
    SuperiorInfo        superInfo = null;
    NestingInfo         nestingInfo = null;
    TransactionState    tranState = null;
    CompletionHandler   terminator = null;
    boolean             registered = false;
    boolean             root = true;
    boolean             rollbackOnly = false;
    boolean             dying = false;
    boolean             temporary = false;
    int                 hash = 0;
	/*
		Logger to log transaction messages
	*/  
    static Logger _logger = LogDomains.getLogger(SubCoordinator.class,LogDomains.TRANSACTION_LOGGER);


    /**
     * Creates and initialises a SubCoordinator, given the parent's local
     * and global identifiers and the sequence of ancestors.
     *
     * @param parentGlobalTID  The parent's global transaction identifier.
     * @param parentLocalTID   The parent's local transaction identifier.
     * @param ancestors        This transactions's ancestors (includes parent).
     *
     * @return
     *
     * @exception LogicErrorException  An internal logic error occurred.
     *
     * @see
     */
    SubCoordinator(GlobalTID parentGlobalTID, Long parentLocalTID,
            CoordinatorImpl[] ancestors) throws LogicErrorException {

        // Allocate a new global identifier for the subtransaction.
        // If one cannot be allocated, raise an exception as the
        // subtransaction cannot be started.

        tranState = new TransactionState(parentLocalTID, parentGlobalTID);

        // Store information about the superior, ancestors and participants
        // of the new subtransaction.

        superInfo = new SuperiorInfo(tranState.localTID,
                                     tranState.globalTID, null, null);

        // Cache the name  - create a buffer and print the global XID into it.

        name = superInfo.globalTID.toString();

        // Cache the hash code.

        hash = superInfo.globalTID.hashCode();

        // Create the nesting info object to record the ancestors.

        nestingInfo = new NestingInfo(ancestors);

        // Zero out the RegisteredResources reference as it will be
        // created when needed.

        participants = null;

        // Set other instance variables.

        root = true;
        registered = true;
        rollbackOnly = false;
        dying = false;
        temporary = false;
        terminator = null;

        // Set the state of the subtransaction to active before making it
        // visible to the RecoveryManager.

        if (!tranState.setState(TransactionState.STATE_ACTIVE)) {
            LogicErrorException exc = new LogicErrorException(
					 LogFormatter.getLocalizedMessage(_logger,
					 "jts.invalid_state_change"));
            throw exc;
        } else {
            // Inform the RecoveryManager of the existence of this transaction.
            RecoveryManager.addCoordinator(tranState.globalTID,
                                           tranState.localTID, this, 0);
        }
    }

    /**
     * Creates and initialises a subordinate SubCoordinator, given the global
     * identifier, a reference to the superior SubCoordinator,
     * and the ancestors of the transaction.
     * The temporary subordinate indicator is used for the case where a parent
     * Coordinator is created when a subtransaction enters a process for the
     * first time. If the request returns and the subtransaction has no
     * participants, it is destroyed, along with any temporary ancestors.
     *
     * @param globalTID  The global identifier for the transaction.
     * @param superior   The superior Coordinator.
     * @param temporary  The temporary flag.
     * @param ancestors  The ancestors of the transaction.
     *
     * @return
     *
     * @exception LogicErrorException  An internal logic error occurred.
     *
     * @see
     */
    SubCoordinator(GlobalTID globalTID, Coordinator superior,
            boolean temporary, CoordinatorImpl[] ancestors)
            throws LogicErrorException {

        // Allocate a new local identifier for the transaction.  If one cannot
        // be allocated, raise an exception as the transaction
        // cannot be started.

        tranState = new TransactionState(globalTID,null);

        // Store information about the superior, ancestors and participants of
        // the new subordinate transaction.

        superInfo = new SuperiorInfo(tranState.localTID,
                                     globalTID, superior, null);

        // Cache the name  - create a buffer and print the global XID into it.

        name = superInfo.globalTID.toString();

        // Cache the hash code.

        hash = superInfo.globalTID.hashCode();

        // Create the nesting info object to record the ancestors.

        nestingInfo = new NestingInfo(ancestors);

        // Zero out the RegisteredResources reference,
        // as it will be created when needed.

        participants = null;

        // Set other instance variables.

        root = false;
        registered = false;
        rollbackOnly = false;
        dying = false;
        this.temporary = temporary;
        terminator = null;

        // Set the state of the transaction to active before making it visible
        // to the TransactionManager.

        if (!tranState.setState(TransactionState.STATE_ACTIVE)) {
            LogicErrorException exc = new LogicErrorException(
					LogFormatter.getLocalizedMessage(_logger,
					"jts.invalid_state_change"));
            throw exc;
        } else if (!RecoveryManager.addCoordinator(globalTID,
                                                   tranState.localTID,
                                                   this, 0)) {
            LogicErrorException exc = new LogicErrorException(
					LogFormatter.getLocalizedMessage(_logger,
					"jts.transaction_id_already_in_use"));
            throw exc;
        }
    }

    /**
     * Cleans up the state of the object.
     *
     * @param
     *
     * @return
     *
     * @see
     */
    public void doFinalize() {

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
            rollback(true);
            break;

        // For committed or rolled-back, we really need to destroy the object

        case TransactionState.STATE_COMMITTED :
        case TransactionState.STATE_ROLLED_BACK :
            if( superInfo != null ) superInfo.doFinalize();

            tranState = null;
            superInfo = null;
            nestingInfo = null;
            participants = null;
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
     * Returns the local status of the target transaction.
     *
     * @param
     *
     * @return  The status of the transaction.
     *
     * @see
     */
    synchronized public Status get_status() {

        Status result = Status.StatusUnknown;

        if (tranState != null) {

            switch (tranState.state) {

            // If active, return active or marked rollback-only
            // if the flag is set.

            case TransactionState.STATE_ACTIVE :
                if (rollbackOnly) {
                    result = Status.StatusMarkedRollback;
                } else {
                    result = Status.StatusActive;
                }
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
                result = Status.StatusCommitting;
                break;
            case TransactionState.STATE_COMMITTED :
                result = Status.StatusCommitted;
                break;
            case TransactionState.STATE_ROLLING_BACK :
                result = Status.StatusRollingBack;
                break;
            case TransactionState.STATE_ROLLED_BACK :
                result = Status.StatusRolledBack;
                break;

            // Any other state, return unknown.

            default :
                result = Status.StatusUnknown;
                break;
            }
        } else {
            INVALID_TRANSACTION exc = new INVALID_TRANSACTION(
                MinorCode.Completed, CompletionStatus.COMPLETED_NO);
            throw exc;
        }

        return result;
    }

    /**
     * Gets the local state of the parent transaction.
     * This operation references no instance variables and so can be
     * implemented locally in the proxy class.
     *
     * @param
     *
     * @return  The parent transaction's status.
     *
     * @exception SystemException  The parent could not be reached.
     *
     * @see
     */
    synchronized public Status get_parent_status() throws SystemException {

        Status result = Status.StatusNoTransaction;

        // Return the parents status.  If there is none, this is an error;
        // return no transaction status (may want to raise a LogicError here).

		if (tranState != null) {
			CoordinatorImpl parent = nestingInfo.getParent(false); 
			if (parent != null) { 
				result = parent.get_status();
			}
		} else {
			INVALID_TRANSACTION exc = new INVALID_TRANSACTION(
				MinorCode.Completed, CompletionStatus.COMPLETED_NO); 
			throw exc; 
		}

        return result;
    }

    /**
     * Gets the local state of the top-level transaction.
     * This operation references no instance variables and so can be
     * implemented locally in a proxy class.
     *
     * @param
     *
     * @return  The top-level transaction status.
     *
     * @exception SystemException  The top-level ancestor could not be reached.
     *
     * @see
     */
    synchronized public Status get_top_level_status() throws SystemException {

        // Return the top-level status.  If there is none, this is an error;
        // return no transaction status (may want to raise a LogicError here).

        Status result = Status.StatusNoTransaction;

        if (tranState != null) {
            CoordinatorImpl topLevel = nestingInfo.getTopLevel();
            if (topLevel != null) {
                result = topLevel.get_status();
            }
        } else {
            INVALID_TRANSACTION exc = new INVALID_TRANSACTION(
                MinorCode.Completed, CompletionStatus.COMPLETED_NO);
            throw exc;
        }

        return result;
    }

    /**
     * Compares the given Coordinator object with the target,
     * and returns TRUE if they represent the same transaction.
     * This operation needs to be implemented in an efficient manner, without
     * any cross-process calls. This could be achieved by including the
     * global identifier in the Coordinator references and comparing them.
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
    synchronized public boolean is_same_transaction(Coordinator other)
            throws SystemException {

        boolean result = false;

        // Get the names of the two transactions and compare them.

        if (name != null) {
          result = name.equals(other.get_transaction_name());
        } else {
            INVALID_TRANSACTION exc = new INVALID_TRANSACTION(
                MinorCode.Completed, CompletionStatus.COMPLETED_NO);
            throw exc;
        }

        return result;
    }

    /**
     * Determines whether the target SubCoordinator is related to
     * the given Coordinator (i.e. is a member of the same transaction family).
     * For a subtransaction this is equivalent to saying that the transaction
     * associated with the parameter object is a descendant of the top-level
     * ancestor of the transaction associated with the target object.
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
    synchronized public boolean is_related_transaction(Coordinator other)
            throws SystemException {

        // Check whether the given transaction is a descendant of our top-level
        // transaction.

        boolean result = false;
        if (tranState != null) {
            CoordinatorImpl topLevel = nestingInfo.getTopLevel();
            if (topLevel != null) {
                result = other.is_descendant_transaction(topLevel.object());
            }
        } else {
            INVALID_TRANSACTION exc = new INVALID_TRANSACTION(
                MinorCode.Completed, CompletionStatus.COMPLETED_NO);
            throw exc;
        }

        return result;
    }

    /**
     * Determines whether the target SubCoordinator is an ancestor
     * of the given Coordinator.
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
                MinorCode.Completed, CompletionStatus.COMPLETED_NO);
            throw exc;
        }

        return result;
    }

    /**
     * Determines whether the target SubCoordinator is a descendant
     * of the given Coordinator.
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
    synchronized public boolean is_descendant_transaction(Coordinator other)
            throws SystemException {

        // A transaction is considered to be a descendant of itself, so if the
        // two transactions are the same, return TRUE.

        boolean result = false;
        if (tranState != null) {
            if (is_same_transaction(other)) {
                result = true;
            } else {
            // Otherwise, go through our ancestors, comparing
            // them with the given transaction.
            result = nestingInfo.isDescendant(other);
            }
        } else {
            INVALID_TRANSACTION exc = new INVALID_TRANSACTION(
                MinorCode.Completed, CompletionStatus.COMPLETED_NO);
            throw exc;
        }

        return result;
    }

    /**
     * Determines whether the target SubCoordinator represents a top-level
     * (non-nested) transaction.
     * <p>
     * For a subtransaction returns FALSE.
     * <p>
     * This operation references no instance variables and so can be
     * implemented locally in a proxy class.
     *
     * @param
     *
     * @return  Indicates the transaction is top-level.
     *
     * @see
     */
    public boolean is_top_level_transaction() {

        boolean result = false;
        if (tranState == null) {
            INVALID_TRANSACTION exc = new INVALID_TRANSACTION(
                MinorCode.Completed, CompletionStatus.COMPLETED_NO);
            throw exc;
        }

        return result;
    }

    /**
     * Returns a hash value based on the transaction associated with the target
     * object.
     *
     * @param
     *
     * @return  The hash value for the transaction.
     *
     * @see
     */
    synchronized public int hash_transaction() {

        int result =  hash;

        if (tranState == null) {
            INVALID_TRANSACTION exc = new INVALID_TRANSACTION(
                MinorCode.Completed, CompletionStatus.COMPLETED_NO);
            throw exc;
        }

        return result;
    }

    /**
     * Returns a hash value based on the top-level ancestor of the transaction
     * associated with the target object.
     *
     * @param
     *
     * @return  The hash value for the top-level ancestor.
     *
     * @exception SystemException  The other Coordinator could not be reached.
     *
     * @see
     */
    synchronized public int hash_top_level_tran() throws SystemException {

        int result = 0;
        if (tranState != null) {
            CoordinatorImpl topLevel = nestingInfo.getTopLevel();
            if (topLevel != null) {
                result = topLevel.hash_transaction();
            }
        } else {
            INVALID_TRANSACTION exc = new INVALID_TRANSACTION(
                MinorCode.Completed, CompletionStatus.COMPLETED_NO);
            throw exc;
        }

        return result;
    }

    /**
     * Enables a Resource to be registered as a participant in the completion
     * of the subtransaction represented by the SubCoordinator.
     * If the Resource is a SubtransactionAwareResource, it is registered
     * with the SubCoordinator; if the SubCoordinator has not registered
     * with is superior, it creates a CoordinatorResource and registers it
     * with the superior. The registration is passed on to the top-level
     * Coordinator in any case.
     *
     * @param res  The Resource to be registered.
     *
     * @return  The RecoveryCoordinator object from the
     *   registration with the top-level ancestor.
     *
     * @exception Inactive  The Coordinator is completing the transaction and
     *   cannot accept this registration.
     * @exception TRANSACTION_ROLLEDBACK  The transaction which the Coordinator
     *   represents has already been rolled back, or  been marked
     *   rollback-only.
     * @exception SystemException  The operation failed.
     *
     *  @see
     */
    synchronized public RecoveryCoordinator register_resource(Resource res)
            throws SystemException, Inactive, TRANSACTION_ROLLEDBACK {

        RecoveryCoordinator result = null;

        // First check the state of the transaction. If it is not active,
        // do not allow the registration.

        if (tranState == null ||
                tranState.state != TransactionState.STATE_ACTIVE) {
            Inactive exc = new Inactive();
            throw exc;
        }

        // Check whether the transaction has been marked rollback-only.

        if (rollbackOnly) {
            TRANSACTION_ROLLEDBACK exc =
                new TRANSACTION_ROLLEDBACK(0,CompletionStatus.COMPLETED_NO);
            throw exc;
        }

        // Register the given Resource with the top-level
        // Coordinator first, and remember the RecoveryCoordinator object
        // that is returned by the top-level Coordinator.

        CoordinatorImpl topLevel = nestingInfo.getTopLevel();

        // If the top-level Coordinator raises an exception, then do not
        // proceed with the registration, and return the exception.

        try {
            result = topLevel.register_resource(res);
        } catch (SystemException exc) {
            throw (SystemException) exc.fillInStackTrace();
        } catch (Inactive exc) {
            throw (Inactive) exc.fillInStackTrace();
        }

        // Find out whether the Resource is actually a
        // SubtransactionAwareResource.

        boolean subAwareRes =
            res._is_a(SubtransactionAwareResourceHelper.id());

        // If the Resource is actually a SubtransactionAwareResource,
        // then it needs to be registered for participation in completion
        // of the subtransaction as well as the top level transaction.

        if (subAwareRes) {

            // If not previously registered, a CoordinatorResource object
            // must be registered with our superior.  Note that root
            // SubCoordinators are created with the registration flag set,
            // so we do not need to check
            // whether we are the root SubCoordinator here.

            if (!registered) {

                // Initialise the CoordinatorResource with the local id,
                // our reference, and a flag to indicate that it does
                //  not represent a subtransaction.

                CoordinatorResourceImpl cImpl =
                    new CoordinatorResourceImpl(superInfo.globalTID,
                                                this, true);

                // Register the CoordinatorResource with superior Coordinator,
                // and store the resulting RecoveryCoordinator reference.

                try {
                    CoordinatorResource cRes = cImpl.object();
                    superInfo.superior.register_subtran_aware(cRes);
                    superInfo.setResource(cRes);
                    registered = true;
                } catch (Throwable exc) {
                    // If an exception was raised, do not set the
                    // registration flag, and destroy the object.
                    cImpl.destroy();

                    // If the exception is a system exception,
                    // then allow it to percolate to the caller.

                    if (exc instanceof OBJECT_NOT_EXIST) {
                        TRANSACTION_ROLLEDBACK ex2 =
                            new TRANSACTION_ROLLEDBACK(
                                0, CompletionStatus.COMPLETED_NO);
                        throw ex2;
                    }

                    if (exc instanceof Inactive) {
                        throw (Inactive) exc;
                    }

                    if (exc instanceof SystemException) {
                        throw (SystemException) exc;
                    }

                    // Otherwise throw an internal exception.

                    INTERNAL ex2 = new INTERNAL(MinorCode.NotRegistered,
                                                CompletionStatus.COMPLETED_NO);
                    throw ex2;
                }
            }

            // Add the SubtransactionAwareResource to the set of participants.
            // Make sure the RegisteredResources instance variable has been
            // set up.

            if (participants == null) {
                participants = new RegisteredResources(null, this);
            }

            // Add a duplicate of the reference to the set.  This is done
            // because if the registration is for a remote object, the proxy
            // will be freed when the registration request returns.

            participants.addRes((Resource)res._duplicate());
            temporary = false;
        }

        return result;
    }

    /**
     * Enables a SubtransactionAwareResource to be registered as a participant
     * in the completion of a subtransaction.
     * <p>
     * If the SubCoordinator has not registered with is superior, it creates a
     * CoordinatorResource and registers it with the superior.
     *
     * @param sares  The SubtransactionAwareResource to be registered.
     *
     * @return
     *
     * @exception Inactive  The Coordinator is completing the transaction and
     *   cannot accept this registration.
     * @exception TRANSACTION_ROLLEDBACK  The transaction which the Coordinator
     *   represents has already been rolled back, or has been marked
     *   rollback-only.
     * @exception SystemException  The operation failed.
     *
     * @see
     */
    synchronized public void register_subtran_aware(
            SubtransactionAwareResource sares)
            throws SystemException, Inactive, TRANSACTION_ROLLEDBACK {

        // First check the state of the transaction. If it is not active,
        // do not allow the registration.

        if (tranState == null ||
                tranState.state != TransactionState.STATE_ACTIVE) {
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
        // registered with our superior.  Note that root SubCoordinators
        // are created with the registration flag set, so we do not need
        // to check whether we are the root SubCoordinator here.

        if (!registered) {

            // Initialise the CoordinatorResource with the local id,
            // our reference, and a
            // flag to indicate that it does not represent a subtransaction.

            CoordinatorResourceImpl cImpl =
                new CoordinatorResourceImpl(superInfo.globalTID, this, true);

            // Register the CoordinatorResource with the superior Coordinator,
            // and store the resulting RecoveryCoordinator reference.

            try {
                CoordinatorResource cRes = cImpl.object();
                superInfo.superior.register_subtran_aware(cRes);
                superInfo.setResource(cRes);
                registered = true;
            } catch(Throwable exc) {
                // If an exception was raised, do not set the registration flag.
                cImpl.destroy();

                // If the exception is a system exception, then allow
                // it to percolate to the caller.

                if (exc instanceof OBJECT_NOT_EXIST) {
                    TRANSACTION_ROLLEDBACK ex2 =
                        new TRANSACTION_ROLLEDBACK(
                            0, CompletionStatus.COMPLETED_NO);
                    throw ex2;
                }

                if (exc instanceof Inactive) {
                    throw (Inactive) exc;
                }

                if (exc instanceof SystemException) {
                    throw (SystemException) exc;
                }

                // Otherwise throw an internal exception.

                INTERNAL ex2 = new INTERNAL(MinorCode.NotRegistered,
                                            CompletionStatus.COMPLETED_NO);
                throw ex2;
            }
        }

        // Add the SubtransactionAwareResource to the set of participants.
        // Make sure the RegisteredResources instance variable has been set up.

        if (participants == null) {
            participants = new RegisteredResources(null, this);
        }

        // Add a duplicate of the reference to the set.
        // This is done because if the  registration is for a remote
        // object, the proxy will be freed when the
        // registration request returns.

        participants.addRes((Resource)sares._duplicate());
        temporary = false;
    }

    /**
     * Ensures that the transaction represented by the target SubCoordinator
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
    synchronized public void rollback_only() throws Inactive {

        if (tranState.state != TransactionState.STATE_ACTIVE) {
            Inactive exc = new Inactive();
            throw exc;
        } else {
            // Set the rollback-only flag.
            rollbackOnly = true;
        }
    }

    /**
     * Returns a printable string that represents the SubCoordinator.
     * This operation references only the global TID, and so can be
     * implemented locally in a proxy class.
     *
     * @param
     *
     * @return  The transaction name.
     *
     * @see
     */
    synchronized public String get_transaction_name() {

        String result = null;
        if (tranState != null) {
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
     * Creates a subtransaction and returns a Control object that
     * represents the child transaction.
     *
     * @param
     *
     * @return  The Control object for the new child transaction.
     *
     * @exception Inactive  The Coordinator is completing the
     *   subtransaction and cannot create a new child.
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
        // and global identifier of the top-level SubCoordinator
        // as there are no ancestors. We do not need to make a copy of the
        // global TID as this is done by the factory when it creates the child.

        CoordinatorImpl[] thisAncestors = nestingInfo.getAncestors();
        CoordinatorImpl[] ancestors =
            new CoordinatorImpl[thisAncestors.length + 1];
        System.arraycopy(thisAncestors, 0, ancestors, 1, thisAncestors.length);
        ancestors[0] = this;

        // Create a new SubCoordinator, and initialise it with the given
        // identifiers and ancestry.  If the operation fails, return a
        // NULL Control object, and the SubtransactionsUnavailable exception.
        // Note that the ancestor sequence is not copied by the creation
        // operation.

        SubCoordinator child = null;
        TerminatorImpl terminator = null;
        try {
            child = new SubCoordinator(superInfo.globalTID,
                                       superInfo.localTID, ancestors);

            // Create a Terminator object, and initialise it with
            // the SubCoordinator reference and a flag to indicate that
            // it represents a subtransaction.

            terminator = new TerminatorImpl(child, true);

            // Create a Control object, and initialise it with the Terminator,
            // SubCoordinator and global OMGtid.

            result = new ControlImpl(terminator, child,
                                     new GlobalTID(child.getGlobalTID()),
                                     child.getLocalTID()).object();
        } catch (Throwable exc) {
            Inactive ex2 = new Inactive();
            throw ex2;
        }

        // If the operation succeeded, add the new child
        // to the set of children.

        nestingInfo.addChild(child);

        return result;
    }

    /**
     * Returns a global identifier that represents the SubCoordinator's
     * transaction. <p>
     * This operation references only the global identifier, and so can be
     * implemented locally in a proxy class.
     * <p>
     * This method is currently not synchronized because that causes a
     * deadlock in resync.  I don't think this is a problem as the global
     * identifier is allocated in the constructor and then never changes.
     *
     * @param
     *
     * @return  The global identifier for the transaction.
     *
     * @see
     */
    public otid_t getGlobalTID() {

        otid_t result = null;
        result = superInfo.globalTID.realTID;

        return result;
    }

    /**
     * Returns the internal identifier for the transaction.
     * This method is currently not synchronized because that causes a deadlock
     * in resync.
     *
     * @param
     *
     * @return  The local transaction identifier.
     *
     * @see
     */
    public long getLocalTID() {

        long result = superInfo.localTID.longValue();
        return result;
    }

    /**
     * Indicates that a method reply is being sent and requests the
     * SubCoordinator's  action.
     * If the Coordinator has active children, which are not registered with
     * their superior (includes root Coordinators) then this method returns
     * activeChildren.
     * If it has already been registered, the method returns doNothing.
     * Otherwise the SubCoordinator returns forgetMe.
     *
     * @param action  A 1-element array to hold the reply action.
     *
     * @return  The parent coordinator if any.
     *
     * @exception SystemException  An error occurred.  The minor code indicates
     *                             the reason for the exception.
     *
     * @see
     */
    synchronized CoordinatorImpl replyAction(int[/*1*/] action)
            throws SystemException {

        CoordinatorImpl result = null;
        action[0] = CoordinatorImpl.doNothing;

        // If this Coordinator is not a root, and there are active children,
        // report that fact to the caller. If the NestingInfo instance
        // variable has not been set up, there are no children.

        if (!root && nestingInfo.replyCheck()) {
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
                } else {
                    action[0] = forgetMe;
                }
            }

            // If we are not registered, and have no participants,
            // we have no reason to exist, so tell the caller to
            // forget about us. The TransactionManager will take care of
            // cleaning everything else up whenit receives the forgetMe
            // response.

            if (action[0] == doNothing && !registered)
                action[0] = forgetMe;
        }

        // Default action is do nothing when we are registered.

        result = null;

        return result;
    }

    /**
     * Marks the SubCoordinator as permanent.
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
     * Checks whether the SubCoordinator is marked rollback-only.
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
     * Checks whether the SubCoordinator is active.
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
     * Checks whether the SubCoordinator has registered with its superior.
     *
     * @param
     *
     * @return  Indicates the registration status.
     *
     * @see
     */
    synchronized boolean hasRegistered() {

        boolean result = registered;
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

        CoordinatorImpl[] coords = nestingInfo.getAncestors();

        TransIdentity[] result = new TransIdentity[coords.length];
        for (int i = 0; i < coords.length; i++) {
            try {
                result[i] = new TransIdentity(coords[i].object(), null,
                                              coords[i].getGlobalTID());
            } catch (Throwable exc) {}
        }

        return result;
    }

    /**
     * Adds the given Coordinator reference to the set of children
     * of the target SubCoordinator.
     *
     * @param child  The child Coordinator.
     *
     * @return  Indicates success of the operation.
     *
     * @see
     */
    synchronized boolean addChild(CoordinatorImpl child) {

        boolean result = nestingInfo.addChild(child);
        return result;
    }

    /**
     * Removes the given Coordinator from the set of children of the target
     * SubCoordinator.
     * If the SubCoordinator is a temporary ancestor, and has no
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
        // instance variable has not been set up, then the child
        // cannot be removed.

        if (nestingInfo != null) {
            result = nestingInfo.removeChild(child);
        }

        // If the removal results in an empty, temporary Coordinator, then this
        // Coordinator must be cleaned up.  The RecoveryManager is called to
        // clean up the transaction.

        if (temporary && !registered &&
                !(participants != null && participants.involved()) &&
                !(nestingInfo != null && nestingInfo.numChildren() > 0)) {

            // We pass the parent Coordinator to the RecoveryManager
            // so that it can remove the child from the parent's set of
            // children after the child is rolled back.

            CoordinatorImpl parent = nestingInfo.getParent(true);
            cleanUpEmpty(parent);
        }

        return result;
    }

    /**
     * Directs the SubCoordinator to prepare to commit.
     * The SubCoordinator checks that the subtransaction can be committed.
     * It does not distribute prepare operations to the participants.
     *
     * @param
     *
     * @return  The consolidated vote.
     *
     * @exception INVALID_TRANSACTION  The transaction is not in a
     *   state to commit, due to outstanding work.
     *
     * @see
     */
    static String[] resultName = { "Commit"/*#Frozen*/, "Rollback"/*#Frozen*/, "Read-only"/*#Frozen*/ };

    synchronized Vote prepare() throws INVALID_TRANSACTION {

        Vote result = Vote.VoteRollback;
        int newState = TransactionState.STATE_PREPARED_FAIL;

        // Record that the Coordinator is about to prepare.

        // First check for active children, before getting too far
        // into the prepare. This is only done for the root Coordinator
        // as for any others it is too late.

        if (root && nestingInfo.numChildren() != 0) {
            INVALID_TRANSACTION exc =
                new INVALID_TRANSACTION(MinorCode.UnfinishedSubtransactions,
                                        CompletionStatus.COMPLETED_NO);
                throw exc;
        }

        // If the SubCoordinator is in the wrong state, return immediately.

        if (!tranState.setState(TransactionState.STATE_PREPARING)) {
            return Vote.VoteRollback;
        }

        // Check for marked rollback-only.

        if (rollbackOnly) {

            // Record that the Coordinator is deciding to rollback.
            // Try to set the state to prepared fail.

            if (!tranState.setState(TransactionState.STATE_PREPARED_FAIL)) {
                return Vote.VoteRollback;
            }
        } else {
            newState = TransactionState.STATE_PREPARED_SUCCESS;
            result = Vote.VoteCommit;
        }

        // Record that prepare is complete.
        // Set the state.

        if (!tranState.setState(newState)) {
            result = Vote.VoteRollback;
        }

        return result;
    }

    /**
     * Directs the SubCoordinator to commit the transaction.
     * The SubCoordinator directs all registered Resources to commit.
     *
     * @param
     *
     * @return
     *
     * @see
     */
    void commit() {

        Coordinator parent = null;

        // Record that the Coordinator is about to commit.
        // Until we actually distribute commit flows, synchronize the method.

        synchronized (this) {

            // If the SubCoordinator is in the wrong state, return immediately.

            if (!tranState.setState(TransactionState.STATE_COMMITTING)) {
					_logger.log(Level.SEVERE,"jts.transaction_wrong_state",
							"commit");
					 String msg = LogFormatter.getLocalizedMessage(_logger,
				 							"jts.transaction_wrong_state",
											new java.lang.Object[] {
											"commit"});
					 throw  new org.omg.CORBA.INTERNAL(msg);
            }

            // Get the reference of the parent Coordinator.

            parent = nestingInfo.getParent(false).object();

            // Release the lock before proceeding with commit.

        }

        // Commit all participants.  If a fatal error occurs during this
        // method, then the process must be ended with a fatal error.

        if (participants != null) {
            try {
                participants.distributeSubcommit(parent);
            } catch (Throwable exc) {
				_logger.log(Level.SEVERE,"jts.exception_on_resource_operation",
                        new java.lang.Object[] { exc.toString(),
						"commit"});
				 String msg = LogFormatter.getLocalizedMessage(_logger,
				 							"jts.exception_on_resource_operation",
											new java.lang.Object[]
											{exc.toString(),
											"commit"});
				 throw  new org.omg.CORBA.INTERNAL(msg);
            }
        }

        // The remainder of the method needs to be synchronized.

        synchronized (this) {

            // Record that objects have been told to commit.
            // Set the state

            if (!tranState.setState(TransactionState.STATE_COMMITTED)) {
					_logger.log(Level.SEVERE,"jts.transaction_wrong_state",
							"commit");
					 String msg = LogFormatter.getLocalizedMessage(_logger,
				 							"jts.transaction_wrong_state",
											new java.lang.Object[] {
											"commit"});
					 throw  new org.omg.CORBA.INTERNAL(msg);
            }

            // Remove our reference from the parents set of children

            nestingInfo.removeFromParent(this);

            // Clean up the SubCoordinator after a commit.
            // In the case where the SubCoordinator is a root,
            // the CoordinatorTerm object must be informed that the
            // transaction has completed so that if another
            // caller has committed the transaction the object
            // normally responsible for terminating the transaction
            // can take the appropriate action. NOTE: This may DESTROY
            // the SubCoordinator object so NO INSTANCE VARIABLES
            // should be referenced after the call.
            // In the case where the SubCoordinator is a subordinate, the
            // CoordinatorResource object must be informed that the transaction
            // has been completed so that it can handle any subsequent requests
            // for the transaction.

            if (terminator != null) {
                terminator.setCompleted(false, false);
            }

            // As subtransactions do not have synchronization,
            // there is nothing left to  do, so get the
            //RecoveryManager to forget about us, then self-destruct.

            RecoveryManager.removeCoordinator(superInfo.globalTID,
                                              superInfo.localTID, false);
            destroy();

            /*!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!*/
            /* NO INSTANCE VARIABLES MAY BE ACCESSED FROM THIS POINT ON.     */
            /*!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!*/

        }
    }

    /**
     * Directs the SubCoordinator to roll back the transaction.
     * The SubCoordinator directs all registered Resources to rollback.
     *
     * @param force
     *
     * @return
     *
     * @see
     */
    void rollback(boolean force) {

        // Until we actually distribute rollback flows, synchronize the method.

        synchronized (this) {

            // If the transaction has already been rolled back, just return.

            if (tranState == null) {
                return;
            }

            // If this is not a forced rollback and the
            // coordinator has prepared or is in an
            // inappropriate state, do not continue and return FALSE.

            if (!force &&
                    ((tranState.state ==
                        TransactionState.STATE_PREPARED_SUCCESS) ||
                     (!tranState.setState(TransactionState.STATE_ROLLING_BACK))
                    )) {
                return;
            }

            // We do not care about invalid state changes as we are
            // rolling back anyway. If the SubCoordinator is temporary,
            // we do not change state as this would
            // cause a log force in a subordinate, which is not required.

            if (!temporary &&
                    !tranState.setState(TransactionState.STATE_ROLLING_BACK)) {
	        if(_logger.isLoggable(Level.FINE)) {
		    _logger.log(Level.FINE,
                         "SubCoordinator - setState(TransactionState.STATE_ROLLED_BACK) returned false");
                }

            }

            // Rollback outstanding children.  If the NestingInfo instance
            // variable has not been created, there are no children
            // to roll back.

            if (nestingInfo != null) {
                nestingInfo.rollbackFamily();
            }

            // Release the lock before proceeding with rollback.

        }

        // Roll back all participants.  If a fatal error occurs during
        // this method, then the process must be ended with a fatal error.

        if (participants != null) {
            participants.distributeSubrollback();
        }

        // The remainder of the method needs to be synchronized.

        synchronized(this) {

            // Set the state to rolled back.

            // Remove our reference from the parents set of children
            if (!temporary &&
                    !tranState.setState(TransactionState.STATE_ROLLED_BACK)) {
	        if(_logger.isLoggable(Level.FINE)) {
		    _logger.log(Level.FINE,
                         "SubCoordinator - setState(TransactionState.STATE_ROLLED_BACK) returned false");
                }
            }

            nestingInfo.removeFromParent(this);

            // Clean up the SubCoordinator after a rollback.
            // In the case where the SubCoordinator is a root,
            // the CoordinatorTerm object must be informed that
            // the transaction has completed so that if another caller has
            // rolled back the transaction (time-out for example) the object
            // normally responsible for terminating the transaction can
            // take the appropriate action.
            // NOTE: This may DESTROY the SubCoordinator object
            // so NO INSTANCE VARIABLES should be referenced after the call.
            // In the case where the SubCoordinator is a subordinate, the
            // CoordinatorResource object must be informed that the transaction
            // has been completed so that it can handle any subsequent
            // requests for the transaction.

            if (terminator != null) {
                terminator.setCompleted(true, false);
            }

            // As subtransactions do not have synchronization,
            // there is nothing left to do, so get the RecoveryManager
            // to forget about us, then self-destruct.

            RecoveryManager.removeCoordinator(superInfo.globalTID,
                                              superInfo.localTID, false);

            if (!dying) {
                destroy();
            }

            /*!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!*/
            /* NO INSTANCE VARIABLES MAY BE ACCESSED FROM THIS POINT ON.     */
            /*!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!*/
        }
    }

    /**
     * Informs the SubCoordinator that the given object
     * requires synchronization before and after completion
     * of the top-level ancestor transaction.
     * The registration is passed directly to the top-level ancestor.
     *
     * @param sync  The Synchronization object to be registered.
     *
     * @return
     *
     * @exception Inactive  The Coordinator is in the process of completing the
     *   transaction and cannot accept this registration.
     * @exception SynchronizationUnavailable  The transaction service
     *   cannot support synchronization.
     *
     * @see
     */
    synchronized public void register_synchronization(Synchronization sync)
            throws Inactive, SynchronizationUnavailable {

        // First check the state of the transaction. If it is not active,
        // do not allow the registration.

        if (tranState == null ||
                tranState.state != TransactionState.STATE_ACTIVE) {
              Inactive exc = new Inactive();
              throw exc;
          }

        // Register the Synchronization object with the top-level Coordinator.
        // Allow any exception to percolate to the caller.

        CoordinatorImpl topLevel = nestingInfo.getTopLevel();
        topLevel.register_synchronization(sync);
    }

    /**
     * Informs the SubCoordinator of the identity of the
     * object that is normally responsible for directing
     * it through termination.  The CoordinatorTerm/
     * CoordinatorResource object is informed by the Coordinator when the
     * transaction aborts so that they can cope with asynchronous aborts.
     *
     * @param term  The object normally responsible for terminating the
     *              Coordinator.
     *
     * @return
     *
     * @see
     */
    synchronized void setTerminator(CompletionHandler term) {
        terminator = term;
    }

    /**
     * Gets the parent coordinator of the transaction.
     *
     * @param
     *
     * @return  The parent Coordinator
     *
     * @see
     */
    Coordinator getParent() {

        Coordinator result = nestingInfo.getParent(false).object();
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
            participants.getResources(resources, states);

            // Validate each of the Resource objects in
            // the list before returning it.

            for (int i = 0; i < resources.value.length; i++) {
                if (resources.value[i]._non_existent()) {
                    resources.value[i] = null;
                }
            }
        } else {
            resources.value = null;
            states.value = null;
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
    CompletionHandler getTerminator() {

        CompletionHandler result = terminator;
        return result;
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
        // do not allow the operation.

        if (tranState == null ||
                tranState.state != TransactionState.STATE_ACTIVE ||
                rollbackOnly) {
            Unavailable exc = new Unavailable();
            throw exc;
        }

        // Work out the timeout value to pass, if any.
        // Note that only top-level transactions have timeouts.
        // We do not check for timeouts if the Coordinator is remote.
        // If the Coordinator does not have a timeout defined, the
        // TimeoutManager will return a negative value.
        // If the transaction has timed out, the value will be
        // zero.

        long timeLeft = TimeoutManager.timeLeft(superInfo.localTID);
        int timeout = 0;
        if (timeLeft > 0) {
          timeout = (int) timeLeft / 1000;
        } else if (timeLeft == 0) {
            // If the timeout has expired, then do not return a context,
            // but roll the transaction back and throw
            // the TRANSACTION_ROLLEDBACK exception.

            TimeoutManager.timeoutCoordinator(superInfo.localTID,
                                              TimeoutManager.ACTIVE_TIMEOUT);
            TRANSACTION_ROLLEDBACK exc =
                new TRANSACTION_ROLLEDBACK(0,CompletionStatus.COMPLETED_NO);
            throw exc;
        }

        // Fill in the context with the current transaction information,
        // and the ancestor information.

        TransIdentity current = new TransIdentity(this.object(),
                                                  null,
                                                  superInfo.globalTID.realTID);
        TransIdentity[] parents = getAncestors();

        // Ensure that the implementation specific data is filled with a value.

        if (emptyData == null) {
            emptyData = Configuration.getORB().create_any();
            emptyData.insert_boolean(false);
        }

        PropagationContext result = new PropagationContext(timeout, current,
                                                           parents, emptyData);

        return result;
    }

    /**
     * Cleans up an empty Coordinator.
     *
     * @param parent  The parent Coordinator, if any.
     *
     * @return
     *
     * @see
     */
    void cleanUpEmpty(CoordinatorImpl parent) {

        // Roll the transaction back, ignoring any exceptions.

        try {
            rollback(true);
        } catch (Throwable exc) {}

        // If the transaction is a subtransaction, remove the
        // child from the parent's  set of children.
        // If the parent is temporary, this will cause the parent
        // to call cleanup_empty_coordinator, and so-on until all
        // empty ancestors are cleaned up.

        if (parent != null) {
            parent.removeChild(this);
        }
    }

    /**
     * Directs the SubCoordinator to commit the transaction in one phase
     * The SubCoordinator directs all registered Resources to commit.
     *
     * @param
     *
     * @return
     *
     * @see
     */
    boolean commitOnePhase() {

        // The commit of a subtransaction is always a one phase commit.
        // The implementation of the prepare/commit methods simply
        // split this up into two parts: the prepare checks the state and
        // the commit calls the resources.  Therefore commit_one_phase can
        // simply call these methods directly.
        // Also we let any exception pass up through.

        Vote v = this.prepare();

        if (v == Vote.VoteCommit) {
           this.commit();
        } else if (v == Vote.VoteReadOnly) {
            // Nothing to do
        } else {
            this.rollback(true);
        }

        return true;
    }

    /**
     * Returns a hash code for the object.
     * This very basic method is used by the trace facility and should
     * not call any method which is traced.
     *
     * @param
     *
     * @return  The hash code for the object.
     *
     * @see
     */
    public int hashCode() {
        return hash;
    }

    /**
     * Determines equality of the object with the parameter.
     * <p>
     * This relies on the availability of the propagation context from the
     * target Coordinator.
     * <p>
     * If the other Coordinator is remote, and not a JCoordinator,
     * and is in the process of ending the transaction,
     * then this operation will fail; in this
     * case we throw the INVALID_TRANSACTION exception with a minor code that
     * indicates the cause of the failure.
     * <p>
     * Unfortunately this is the best we can do with the OMG interfaces when
     * interoperating with a different OTS implementation.
     *
     * @param other  The other object.
     *
     * @return  Indicates equality.
     *
     * @see
     */
    public boolean equals(java.lang.Object other) throws INVALID_TRANSACTION {

        // Do a quick check on the object references.

        if (this == other) {
            return true;
        }

        // Obtain the global identifier for the other Coordinator.

        otid_t otherTID = null;

        if (other instanceof CoordinatorImpl) {
            // For local Coordinator objects which are really instances of the
            // CoordinatorImpl class, get the global TID via a private
            // method call.
            if (other instanceof SubCoordinator) {
                otherTID = ((SubCoordinator)other).superInfo.globalTID.realTID;
            }
        } else if (other instanceof org.omg.CORBA.Object) {

            // For remote Coordinator objects which are instances of the
            // JCoordinator class, use the getGlobalTID method remotely.

            try {
                JCoordinator jcoord =
                    JCoordinatorHelper.narrow((org.omg.CORBA.Object) other);
                otherTID = jcoord.getGlobalTID();
            } catch (BAD_PARAM exc) {

                // For remote Coordinator objects which are not instances of
                // the JCoordinator class, use the
                // propagation context to compare the Coordinators.
                // This relies on the availability of the propagation context
                // from the target Coordinator.

                try {
                    Coordinator coord =
                        CoordinatorHelper.narrow((org.omg.CORBA.Object)other);
                    PropagationContext pc = coord.get_txcontext();
                    otherTID = pc.current.otid;
                } catch (BAD_PARAM ex2) {
                    // If the other object is not actually a Coordinator,
                    // then the objects are not the same.
                } catch (Unavailable ex2) {
                    // If the other Coordinator is inactive, then there is
                    // nothing we can do to get the global identifier for the
                    // transaction, so we cannot compare the Coordinator
                    // objects.

                    INVALID_TRANSACTION ex3 =
                        new INVALID_TRANSACTION(MinorCode.CompareFailed,
                                                CompletionStatus.COMPLETED_NO);
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

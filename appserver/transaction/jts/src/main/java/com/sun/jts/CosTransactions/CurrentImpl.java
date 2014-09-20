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
// Module:      CurrentImpl.java
//
// Description: Transaction Current pseudo-object implementation.
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
import org.omg.CosTransactions.*;

import com.sun.jts.codegen.otsidl.*;
import com.sun.jts.trace.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import com.sun.logging.LogDomains;
import com.sun.jts.utils.LogFormatter;

/**The CurrentImpl class is our implementation of the standard Current
 * interface. It provides operations that enable the demarcation of a
 * transaction's scope. These operations are specified in pseudo-IDL. The
 * CurrentImpl ensures that the CurrentTransaction class is set up when the
 * CurrentImpl is created. As an instance of this class may be accessed from
 * multiple threads within a process, serialisation for thread-safety is
 * necessary in the implementation.
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

public class CurrentImpl extends org.omg.CORBA.LocalObject
    implements org.omg.CosTransactions.Current {
    private int timeOut = 0;
    private static boolean active = true;
    private TransactionFactory factory = null;
	/*
		Logger to log transaction messages
	*/ 
	static Logger _logger = LogDomains.getLogger(CurrentImpl.class, LogDomains.TRANSACTION_LOGGER);

    /**Default CurrentImpl constructor.
     *
     * @param
     *
     * @return
     *
     * @see
     */
    CurrentImpl() {

        // Ensure the CurrentTransaction state has been initialised.

        CurrentTransaction.initialise();
    }

    /**Creates a new Control object, containing new Terminator and Coordinator
     * objects.
     * <p>
     * The current timeout value is passed to the Coordinator.
     * <p>
     * The Control object is made the current one for the thread on which this
     * method was invoked. If there is already a current Control the existing
     * Control that represents the parent transaction is stacked behind the new one,
     * which becomes the current one.
     *
     * @param
     *
     * @return
     *
     * @exception SubtransactionsUnavailable  A subtransaction cannot be begun,
     *   either because the Transaction Service does not support nested transactions
     *   or because the current transaction is completing.
     * @exception INVALID_TRANSACTION  The transaction could not be begun as the
     *   current thread of control has pending non-transactional work.
     * @exception SystemException  The operation failed.
     *
     * @see
     */
    public void begin()
        throws INVALID_TRANSACTION, SystemException, SubtransactionsUnavailable {

        ControlImpl controlImpl = null;

        // Until we need to check for resync in progress, synchronize the method.

        synchronized(this) {

            // If the transaction service is not active, throw an exception.

            if( !active ) {
                NO_PERMISSION exc = new NO_PERMISSION(0,CompletionStatus.COMPLETED_NO);
                throw exc;
            }

            if (Configuration.isDBLoggingEnabled()) {
                //Put a marker record into the log table
                LogDBHelper.getInstance().setServerName();
            }

            // Get a reference to the current ControlImpl object.

            try {
                controlImpl = CurrentTransaction.getCurrent();
            } catch( SystemException exc ) {
            }

            // Release the lock now so that if the TransactionFactoryImpl call has to block
            // (during resync), the lock does not prevent other threads from running.

        }

        // If there is a current Control object, then we should try to begin a
        // subtransaction.

        if( controlImpl != null )
            try {

                // Get the Coordinator reference, and use it to create a subtransaction.
                // If an exception was raised, return it to the caller.

                Coordinator coord = controlImpl.get_coordinator();

                // Create a new Control object to represent the subtransaction.

                Control control = coord.create_subtransaction();

                // This control object may be remote, if the original Control object was.
                // In this case we need to deal with it in the same was as we do in the
                // resume method.  Any exception which is thrown during this process will
                // result in SubtransactionsUnavailable thrown to the caller.

                JControl jcontrol = JControlHelper.narrow(control);
                if( jcontrol != null )
                    controlImpl = ControlImpl.servant(jcontrol);

                // If there is no local ControlImpl object for the transaction, we create one
                // now.

                if( controlImpl == null )
                    controlImpl = new ControlImpl(control);
            } catch( Throwable exc ) {
                SubtransactionsUnavailable ex2 = new SubtransactionsUnavailable();
                throw ex2;
            }

        // If there is no current ControlImpl object, create a new top-level transaction

        else {
            //$   CurrentTransaction.report();
            //$   RecoveryManager.report();
            //$   ControlImpl.report();
            //$   CoordinatorLog.report();
            //$   TimeoutManager.report();
            //
            try {

                // Get the TransactionFactory which should be used from this application.
                // This block must be synchronized so that different threads do not try
                // this concurrently.

                synchronized(this) {
                    if( factory == null )
                        factory = Configuration.getFactory();
                }

                // Create the new transaction.

                if (factory != null) {
		    		if(_logger.isLoggable(Level.FINEST))
                    {
						_logger.logp(Level.FINEST,"CurrentImpl","begin()",
								"Before invoking create() on TxFactory");
                    }
 
                    if (Configuration.isLocalFactory()) {
                      controlImpl = ((TransactionFactoryImpl) factory).localCreate(timeOut);
                    } else {
                      Control control = factory.create(timeOut);

                      // This control object is remote.
                      // In this case we need to deal with it in the same was as we do in the
                      // resume method.  Any exception which is thrown during this process will
                      // result in SubtransactionsUnavailable thrown to the caller.

                      JControl jcontrol = JControlHelper.narrow(control);
                      if (jcontrol != null) {
                          controlImpl = ControlImpl.servant(jcontrol);
                      }

                      // If there is no local ControlImpl object for the transaction, we create one
                      // now.

                      if (controlImpl == null) {
                          controlImpl = new ControlImpl(control);
                      }
                    }
                }
            } catch( Throwable exc ) {
				_logger.log(Level.WARNING,
						"jts.unexpected_error_in_begin",exc);
						
            }
        }

        // If the new Control reference is NULL, raise an error at this point.

        if( controlImpl == null ){
            INVALID_TRANSACTION exc = new INVALID_TRANSACTION(MinorCode.FactoryFailed,
                                                              CompletionStatus.COMPLETED_NO);
            throw exc;
        }

        // Make the new Control reference the current one, indicating that the
        // existing one (if any) should be stacked.

        else
            try {
				if(_logger.isLoggable(Level.FINEST))
                    {
						_logger.logp(Level.FINEST,"CurrentImpl","begin()",
								"Before invoking CurrentTransaction.setCurrent(control,true)");
                    }
                CurrentTransaction.setCurrent(controlImpl,true);
            }

        // The INVALID_TRANSACTION exception at this point indicates that the transaction
        // may not be started.  Clean up the state of the objects in the transaction.

        catch( INVALID_TRANSACTION exc ) {
            controlImpl.destroy();
            throw (INVALID_TRANSACTION)exc.fillInStackTrace();
        }

    }

	//START RI PERFIMPROVEMENT
	// This method is introduced to improve the performance. without this method external
	// synchronization is required to associate the timeout with this transaction
	// This method is not part of the standard Current interface
    /**Creates a new Control object, containing new Terminator and Coordinator
     * objects.
     * <p>
     * Input parameter timeout value is passed to the Coordinator.
     * <p>
     * The Control object is made the current one for the thread on which this
     * method was invoked. If there is already a current Control the existing
     * Control that represents the parent transaction is stacked behind the new one,
     * which becomes the current one.
     *
     * @param
     *
     * @return
     *
     * @exception SubtransactionsUnavailable  A subtransaction cannot be begun,
     *   either because the Transaction Service does not support nested transactions
     *   or because the current transaction is completing.
     * @exception INVALID_TRANSACTION  The transaction could not be begun as the
     *   current thread of control has pending non-transactional work.
     * @exception SystemException  The operation failed.
     *
     * @see
     */
    public void begin(int time_out)
        throws INVALID_TRANSACTION, SystemException, SubtransactionsUnavailable {

        ControlImpl controlImpl = null;

        // Until we need to check for resync in progress, synchronize the method.

        synchronized(this) {

            // If the transaction service is not active, throw an exception.

            if( !active ) {
                NO_PERMISSION exc = new NO_PERMISSION(0,CompletionStatus.COMPLETED_NO);
                throw exc;
            }

            if (Configuration.isDBLoggingEnabled()) {
                //Put a marker record into the log table
                LogDBHelper.getInstance().setServerName();
            }

            // Get a reference to the current ControlImpl object.

            try {
                controlImpl = CurrentTransaction.getCurrent();
            } catch( SystemException exc ) {
            }

            // Release the lock now so that if the TransactionFactoryImpl call has to block
            // (during resync), the lock does not prevent other threads from running.

        }

        // If there is a current Control object, then we should try to begin a
        // subtransaction.

        if( controlImpl != null )
            try {

                // Get the Coordinator reference, and use it to create a subtransaction.
                // If an exception was raised, return it to the caller.

                Coordinator coord = controlImpl.get_coordinator();

                // Create a new Control object to represent the subtransaction.

                Control control = coord.create_subtransaction();

                // This control object may be remote, if the original Control object was.
                // In this case we need to deal with it in the same was as we do in the
                // resume method.  Any exception which is thrown during this process will
                // result in SubtransactionsUnavailable thrown to the caller.

                JControl jcontrol = JControlHelper.narrow(control);
                if( jcontrol != null )
                    controlImpl = ControlImpl.servant(jcontrol);

                // If there is no local ControlImpl object for the transaction, we create one
                // now.

                if( controlImpl == null )
                    controlImpl = new ControlImpl(control);
            } catch( Throwable exc ) {
                SubtransactionsUnavailable ex2 = new SubtransactionsUnavailable();
                throw ex2;
            }

        // If there is no current ControlImpl object, create a new top-level transaction

        else {
            //$   CurrentTransaction.report();
            //$   RecoveryManager.report();
            //$   ControlImpl.report();
            //$   CoordinatorLog.report();
            //$   TimeoutManager.report();
            //
            try {

                // Get the TransactionFactory which should be used from this application.
                // This block must be synchronized so that different threads do not try
                // this concurrently.

                synchronized(this) {
                    if( factory == null )
                        factory = Configuration.getFactory();
                }

                // Create the new transaction.

                if (factory != null) {
		    		if(_logger.isLoggable(Level.FINEST))
                    {
						_logger.logp(Level.FINEST,"CurrentImpl","begin()",
								"Before invoking create() on TxFactory");
                    }
 
                    if (Configuration.isLocalFactory()) {
                      controlImpl = ((TransactionFactoryImpl) factory).localCreate(time_out);
                    } else {
                      Control control = factory.create(time_out);

                      // This control object is remote.
                      // In this case we need to deal with it in the same was as we do in the
                      // resume method.  Any exception which is thrown during this process will
                      // result in SubtransactionsUnavailable thrown to the caller.

                      JControl jcontrol = JControlHelper.narrow(control);
                      if (jcontrol != null) {
                          controlImpl = ControlImpl.servant(jcontrol);
                      }

                      // If there is no local ControlImpl object for the transaction, we create one
                      // now.

                      if (controlImpl == null) {
                          controlImpl = new ControlImpl(control);
                      }
                    }
                }
            } catch( Throwable exc ) {
				_logger.log(Level.WARNING,
						"jts.unexpected_error_in_begin",exc);
						
            }
        }

        // If the new Control reference is NULL, raise an error at this point.

        if( controlImpl == null ){
            INVALID_TRANSACTION exc = new INVALID_TRANSACTION(MinorCode.FactoryFailed,
                                                              CompletionStatus.COMPLETED_NO);
            throw exc;
        }

        // Make the new Control reference the current one, indicating that the
        // existing one (if any) should be stacked.

        else
            try {
				if(_logger.isLoggable(Level.FINEST))
                    {
						_logger.logp(Level.FINEST,"CurrentImpl","begin()",
								"Before invoking CurrentTransaction.setCurrent(control,true)");
                    }
                CurrentTransaction.setCurrent(controlImpl,true);
            }

        // The INVALID_TRANSACTION exception at this point indicates that the transaction
        // may not be started.  Clean up the state of the objects in the transaction.

        catch( INVALID_TRANSACTION exc ) {
            controlImpl.destroy();
            throw (INVALID_TRANSACTION)exc.fillInStackTrace();
        }

    }
	//END RI PERFIMPROVEMENT


    /**Completes the current transaction.
     * <p>
     * This operation can only be called if there is a Terminator object available.
     *
     * @param reportHeuristics  Indicates that heuristic exceptions should be
     *   passed back to the caller.
     *
     * @return
     *
     * @exception NoTransaction  There is no current transaction to commit.
     * @exception INVALID_TRANSACTION  The current transaction has outstanding
     *   work and the Transaction Service is "checked".
     * @exception NO_PERMISSION  The caller is not allowed to commit the
     *   transaction.
     * @exception TRANSACTION_ROLLEDBACK  The transaction could not be committed,
     *   and has been rolled back.
     * @exception HeuristicHazard  Heuristic action may have been taken by a
     *   participant in the transaction.
     * @exception HeuristicMixed  Heuristic action has been taken by a participant
     *   in the transaction.
     * @exception SystemException  The operation failed.
     *
     * @see
     */
    public void commit( boolean reportHeuristics )
        throws NO_PERMISSION, INVALID_TRANSACTION, TRANSACTION_ROLLEDBACK,
        NoTransaction, HeuristicHazard, HeuristicMixed, SystemException {

        // Get the current Control object.  If there is none, raise the
        // NoTransaction exception and return.

        ControlImpl controlImpl = CurrentTransaction.getCurrent();
        if( controlImpl == null ) {
            NoTransaction exc = new NoTransaction();
            throw exc;
        }

        // Get the local identifier from the Control object. Raise an exception if
        // the transaction the Control represents has been completed.

        int active = 1;

        if( !controlImpl.representsRemoteControl() ) {

            StatusHolder status = new StatusHolder();
            controlImpl.getLocalTID(status);

            if( status.value != Status.StatusActive ) {

                // added (Ram J) to handle asynchronous aborts. If a
                // thread calls commit after an asynchronous abort happens,
                // end the thread-tx association before throwing
                // TRANSACTION_ROLLEDBACK exception.
                // In the case where someone had called terminator.commit
                // directly successfully, then end the thread association,
                // before throwing INVALID_TRANSACTION exception.
                CurrentTransaction.endCurrent(true);

                if( status.value == Status.StatusRolledBack ) {
                    TRANSACTION_ROLLEDBACK exc =
                        new TRANSACTION_ROLLEDBACK(0, CompletionStatus.COMPLETED_NO);
                    throw exc;
                }

                INVALID_TRANSACTION exc = new INVALID_TRANSACTION(MinorCode.Completed,
                                                                  CompletionStatus.COMPLETED_NO);
                throw exc;
            }

            // Check whether there are outstanding requests for this transaction, or
            // other active threads.

            active = controlImpl.numAssociated();
        }

        // If this is not the only active thread or there are outstanding requests,
        // raise an exception and return.

        if( (active != 1) || (controlImpl.isOutgoing()) ) {
            /**
            if( active != 1 ) {
            }

            if( controlImpl.isOutgoing() ) {
            }
            **/

            INVALID_TRANSACTION exc = new INVALID_TRANSACTION(MinorCode.DeferredActivities,
                                                              CompletionStatus.COMPLETED_NO);
            throw exc;
        }

        // Get the Terminator from the current Control object.  If this fails, then
        // return whatever exception was raised to the caller.

        else {
            Terminator term = null;

            if (Configuration.isLocalFactory()) {
              try {
                term = controlImpl.get_localTerminator();
              } catch (Throwable exc) {
                NO_PERMISSION ex2 = new NO_PERMISSION(0,CompletionStatus.COMPLETED_NO);
                throw ex2;
              }
            } else {
              try {
                  term = controlImpl.get_terminator();
              } catch( Throwable exc ) {
                NO_PERMISSION ex2 = new NO_PERMISSION(0,CompletionStatus.COMPLETED_NO);
                throw ex2;
              }

              // Tell the Terminator to commit the transaction.
              // This will end the current association of the transaction, if the
              // Terminator is local.  If the Terminator is remote, we end the association
              // ourselves, and do not request unstacking as there will be no stacked
              // Control.

              // End the association if the Terminator is remote.  This is done under a
              // local environment to ignore any exception.  This must be done before
              // the Terminator is called otherwise the ControlImpl object will get
              // confused.

              try {
                if( Configuration.getProxyChecker().isProxy(term) )
                    CurrentTransaction.endCurrent(true);
              } catch( Throwable exc ) {}
            }
            // Commit the transaction.

	    try{
            	term.commit(reportHeuristics);
            } catch (TRANSACTION_ROLLEDBACK e) {
                // ADDED (Ram J) (10/15/01) To handle asynchronous aborts. End
                // thread-tx association before re-throwing exception. This is
                // because commit/rollback operation by a different thread
                // does not set all other thread's control's status to INACTIVE 
                // anymore, for performance purposes.
                CurrentTransaction.endCurrent(true);
                throw e;
            }

            // Free the ControlImpl object.

            controlImpl.destroy();
        }

    }

    /**Rolls back the changes performed under the current transaction.
     * <p>
     * This operation can only be called if there is a Terminator object available.
     *
     * @param
     *
     * @return
     *
     * @exception NoTransaction  There is no current transaction to rollback.
     * @exception INVALID_TRANSACTION  The current transaction has outstanding
     *   work and the Transaction Service is "checked".
     * @exception NO_PERMISSION  The caller is not allowed to roll the
     *   transaction back.
     * @exception TRANSACTION_ROLLEDBACK  The transaction has already been rolled
     *   back.
     * @exception SystemException  The operation failed.
     *
     * @see
     */
    public void rollback()
        throws NoTransaction, INVALID_TRANSACTION, NO_PERMISSION,
        TRANSACTION_ROLLEDBACK, SystemException {

        ControlImpl controlImpl = CurrentTransaction.getCurrent();

        if( controlImpl == null ) {
            NoTransaction exc = new NoTransaction();
            throw exc;
        }

        // Get the local identifier from the Control object. Raise an exception if
        // the transaction the Control represents has been completed.

        int active = 1;

        if( !controlImpl.representsRemoteControl() ) {

            StatusHolder status = new StatusHolder();
            controlImpl.getLocalTID(status);

            if( status.value != Status.StatusActive ) {

                // added (Ram J) to handle asynchronous aborts. If a
                // thread calls rollback after an asynchronous abort happens,
                // end the thread-tx association before throwing
                // TRANSACTION_ROLLEDBACK exception.
                // In the case where someone had called terminator.commit
                // directly successfully, then end the thread association,
                // before throwing INVALID_TRANSACTION exception.
                CurrentTransaction.endCurrent(true);

                if( status.value == Status.StatusRolledBack ) {
                    /* TN - do not throw rollback exception
                    TRANSACTION_ROLLEDBACK exc = new TRANSACTION_ROLLEDBACK(0,CompletionStatus.COMPLETED_NO);
                    throw exc;
                    */
                    return;
                }

                INVALID_TRANSACTION exc = new INVALID_TRANSACTION(MinorCode.Completed,
                                                                  CompletionStatus.COMPLETED_NO);
                throw exc;
            }

            // Check whether there are outstanding requests for this transaction, or
            // other active threads.

            active = controlImpl.numAssociated();
        }

        // If this is not the only active thread or there are outstanding requests,
        // raise an exception and return.

        if( (active != 1) || (controlImpl.isOutgoing()) ) {
            /**
            if( active != 1 ) {
            }

            if( controlImpl.isOutgoing() ) {
            }
            **/

            INVALID_TRANSACTION exc = new INVALID_TRANSACTION(MinorCode.DeferredActivities,
                                                              CompletionStatus.COMPLETED_NO);
            throw exc;
        }

        // Get the Terminator from the current Control object.  If this fails, then
        // return whatever exception was raised to the caller.

        else {
            Terminator term = null;

            if (Configuration.isLocalFactory()) {
              try {
                  term = controlImpl.get_localTerminator();
              } catch( Unavailable exc ) {
                NO_PERMISSION ex2 = new NO_PERMISSION(0,CompletionStatus.COMPLETED_NO);
                throw ex2;
              }
            } else {
              try {
                term = controlImpl.get_terminator();
              } catch( Unavailable exc ) {
                NO_PERMISSION ex2 = new NO_PERMISSION(0,CompletionStatus.COMPLETED_NO);
                throw ex2;
              }

              // Tell the Terminator to roll the transaction back.
              // This will end the current association of the transaction, if the
              // Terminator is local.  If the Terminator is remote, we end the association
              // ourselves, and do not request unstacking as there will be no stacked
              // Control.

              // End the association if the Terminator is remote.  This is done under a
              // local environment to ignore any exception.  This must be done before
              // the Terminator is called otherwise the ControlImpl object will get
              // confused.

              try {
                if( Configuration.getProxyChecker().isProxy(term) )
                    CurrentTransaction.endCurrent(true);
              } catch( Throwable exc ) {}
            }
            // Roll the transaction back.

            try{
                term.rollback();
            } catch (TRANSACTION_ROLLEDBACK e) {
                // ADDED (Ram J) (10/15/01) To handle asynchronous aborts. End
                // thread-tx association before re-throwing exception. This is
                // because commit/rollback operation by a different thread
                // does not set all other thread's control's status to INACTIVE 
                // anymore, for performance purposes.
                CurrentTransaction.endCurrent(true);
                //throw e; // no need to throw this for rollback operation.
            }

            // Free the ControlImpl object.

            controlImpl.destroy();
        }

    }

    /**Marks the current transaction such that is cannot be committed and only
     * rolled back.
     *
     * @param
     *
     * @return
     *
     * @exception NoTransaction  There is no current transaction to mark rollback-
     *   only.
     *
     * @see
     */
    public void rollback_only()
        throws NoTransaction {

        // Get the reference of the current Coordinator.  If there is none, raise the
        // NoTransaction exception.

        try {
            Coordinator coord = CurrentTransaction.getCurrentCoordinator();

            // Tell the Coordinator to mark itself rollback-only.

            coord.rollback_only();
        } catch( Throwable exc ) {
            throw new NoTransaction();
        }

    }

    /**Returns the status of the current transaction.
     *
     * @param
     *
     * @return  The current status of the transaction.  If there is no
     *   current transaction, the value StatusNoTransaction is returned.
     *
     * @see
     */
    public Status get_status() {

        Status result = Status.StatusNoTransaction;
        try {
            Coordinator coord = CurrentTransaction.getCurrentCoordinator();

            // Ask the Coordinator object for its status, and return the value.

            if( coord != null )
                result = coord.get_status();
        } catch( Unavailable exc ) {
        } catch( TRANSACTION_ROLLEDBACK exc ) {
            result = Status.StatusRolledBack;
        } catch( SystemException exc ) {
            result = Status.StatusUnknown;
        }

        return result;
    }

    /**Returns a printable string representing the current transaction.
     *
     * @param
     *
     * @return  The transaction name.  If there is no current transaction,
     *   null is returned.
     *
     * @see
     */
    public String get_transaction_name() {

        String result = null;
        try {
            Coordinator coord = CurrentTransaction.getCurrentCoordinator();

            // Ask the Coordinator object for its name, and return the value.

            if( coord != null )
                result = coord.get_transaction_name();
        }

        // Ignore Unavailable (return null in this case), but allow other exceptions
        // to percolate up.

        catch( Unavailable exc ) {}

        return result;
    }

    /**Sets the timeout value to be used for all subsequent transactions.
     *
     * @param timeout  The timeout value in seconds.
     *
     * @return
     *
     * @see
     */
    public void set_timeout( int timeout ) {
        // timeout < 0 will be rejected (no op).
        // timeout = 0 implies tx has no timeout or a default timeout is used.
        // timeout > 0 implies tx will timeout at the end of the duration.
        if (timeout >= 0) {
            timeOut = timeout;
        }
    }

    public int get_timeout() {
	return timeOut;
    }

    /**Returns the current ControlImpl object.
     *
     * @param
     *
     * @return  The Control object for the current transaction, or null
     *   if there is no current transaction.
     *
     * @exception TRANSACTION_ROLLEDBACK  The current transaction has been rolled
     *   back.
     *
     * @see
     */
    public Control get_control()
        throws TRANSACTION_ROLLEDBACK {
        Control result = null;

        // Get the current Control reference from the TransactionManager. If there
        // is none, return a NULL pointer.
        // If the current Control object indicates that the transaction has ended,
        // then the current association will be ended and the TRANSACTION_ROLLEDBACK
        // exception will be raised.

        ControlImpl control = CurrentTransaction.getCurrent();
        if (control != null) {
          if (Configuration.isLocalFactory()) {
            result = (Control) control;
          } else {
            result = control.object();
          }
        }

        return result;
    }

    /**Disassociates the current ControlImpl from the calling thread.
     *
     * @param
     *
     * @return  The Control object for the suspended transaction.  If
     *   there was no transaction, this will be null.
     *
     * @see
     */
    public Control suspend() {
        Control result = null;
        ControlImpl cImpl = CurrentTransaction.endCurrent(false);
        if(_logger.isLoggable(Level.FINEST))
        {
			_logger.logp(Level.FINEST,"CurrentImpl","suspend()",
					"Current thread has been disassociated from control :"
					+cImpl);
        }

        if (Configuration.isLocalFactory()) {
            result = (Control) cImpl;
        } else {
          if (cImpl != null) {
            result = cImpl.object();
          }
        }

        return result;
    }

    /**Re-associates the given Control to the calling thread.
     * <p>
     * If there is already a current ControlImpl, it is replaced as the current one.
     *
     * @param control  The Control object to be made current.
     *
     * @return
     *
     * @exception InvalidControl  The Control object passed as a parameter is
     *   not valid.  This may be because the transaction it represents has
     *   already completed, or because the object is of the wrong type.
     * @exception INVALID_TRANSACTION  The transaction could not be begun as the
     *   current thread of control has pending non-transactional work.
     *
     * @see
     */
    public void resume( Control control )
        throws InvalidControl, INVALID_TRANSACTION {

        ControlImpl contImpl = null;

        // If the Control object is NULL, then this operation is actually a suspend
        // operation, so end the current association with the thread.

        if( control == null )
            CurrentTransaction.endCurrent(false);
        else {

            if (Configuration.isLocalFactory()) {
                contImpl = (ControlImpl) control;
            } else {
              // Check the ControlImpl object is valid.
              JControl jcontrol = JControlHelper.narrow(control);

              // Try to locate the local ControlImpl object for the transaction.

              if( jcontrol != null )
                contImpl = ControlImpl.servant(jcontrol);

              // If there is no local ControlImpl object for the transaction, we create one
              // now.

              if( contImpl == null )
                try {
                    contImpl = new ControlImpl(control);
                } catch( Exception exc ) {

                    InvalidControl ex2 = new InvalidControl();
                    throw ex2;
                }
             }

            // End the current association regardless of whether there is one.
            // Attempt to make the given ControlImpl object the current one.

            try {
                CurrentTransaction.endCurrent(false);
                CurrentTransaction.setCurrent(contImpl,false);
                if(_logger.isLoggable(Level.FINEST))
                {
					_logger.logp(Level.FINEST,"CurrentImpl","resume(control)",
							"Current thread has been associated with control :"
							+contImpl);
                }
            }

            // The INVALID_TRANSACTION exception at this point indicates that the transaction
            // may not be resumed.

            catch( INVALID_TRANSACTION exc ) {
                throw (INVALID_TRANSACTION)exc.fillInStackTrace();
            }
        }

    }

    /**Shuts down all services.
     *
     * @param immediate  Indicates whether to ignore running transactions.
     *
     * @return
     *
     * @see
     */

    synchronized void shutdown( boolean immediate ) {

        // Inform the basic transaction services to shutdown.

        CurrentTransaction.shutdown(immediate);

    }

    /**Prevents any further transactional activity in the process.
     *
     * @param
     *
     * @return
     *
     * @see
     */
    static void deactivate() {
        active = false;
    }

    //
    // Provide a dummy routine to satisfy the abstract method inherited from org.omg.CosTransactions.Current
    //
    public String[] _ids() {
        return null;
    }
}

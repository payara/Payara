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
// Module:      CurrentTransaction.java
//
// Description: Transaction to thread association management.
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

import org.omg.CORBA.*;
import org.omg.CosTransactions.*;

import com.sun.jts.codegen.otsidl.*;
import com.sun.jts.trace.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import com.sun.logging.LogDomains;
import com.sun.jts.utils.LogFormatter;

import com.sun.enterprise.transaction.jts.JavaEETransactionManagerJTSDelegate;

/**This class manages association of transactions with threads in a process,
 * and associated state/operations.
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

public class CurrentTransaction {
    private static Hashtable threadContexts = new Hashtable(Configuration.EXPECTED_CONCURRENT_THREADS);
    private static Vector suspended = new Vector(Configuration.EXPECTED_CONCURRENT_TRANSACTIONS);

    //store the suspended and associated transactions support only if stats are required
    static boolean statsOn=false;

    private static Hashtable importedTransactions = new Hashtable();
    private static RegisteredStatics statics = null;
    //private static ORB orb = null;
    //private static TransactionFactory localFactory = null;

    // Static arrays for output parameters.

    private static ThreadLocal m_tid=new ThreadLocal();
    //private static boolean[]    outBoolean = new boolean[1];
    //private static int[]        outInt     = new int[1];
    //private static StatusHolder outStatus  = new StatusHolder();

    //$ This constant is used to represent the empty transaction context.  It
    //$ should not be required when the TRANSACTION_REQUIRED exception is
    //$ supported.
	/*
		Logger to log transaction messages
	*/ 
	static Logger _logger = LogDomains.getLogger(CurrentTransaction.class, LogDomains.TRANSACTION_LOGGER);
   
	private static PropagationContext emptyContext =
        new PropagationContext(0,new TransIdentity(null,null,new otid_t(-1,0,new byte[0])),
                               new TransIdentity[0],null);

    /**Initialises the static state of the CurrentTransaction class.
     *
     * @param
     *
     * @return
     *
     * @see
     */
    static  void initialise() {

        // Initialise the static state for the class.

    }

    /**Sets up the thread association for a Control object.
     * <p>
     * If the thread association already exists for the thread under which the
     * operation was invoked and the stacking flag is set, the existing Control
     * is stacked behind the given one, which is made the current one.
     * <p>
     * If the association already exists and the stacking flag is not set, no
     * association is changed and the operation returns false.
     * <p>
     * For XA support, when an association is started or ended, all
     * registered StaticResource objects are informed of the association change.
     *
     * @param control  The Control object to be made the current one.
     * @param stack    Indicates whether the current Control should be stacked.
     *
     * @return  Indicates success of the operation.
     *
     * @see
     */
    static boolean setCurrent( ControlImpl control,
                                            boolean     stack ) {

        boolean result = false;

        // Ensure that the current thread association is valid.
        //boolean[] outBoolean = new boolean[1];

        ControlImpl current = (ControlImpl)m_tid.get();
        /*if( outBoolean[0] ) {
        }*/

        // If there is a current Control object, and we have not been asked to stack
        // it, return FALSE to indicate that we cannot replace it.
        // Otherwise stack the current Control object behind the new one, which
        // becomes the current Control for the thread.

        if( current != null ) {
            if( stack ) {

                // XA support: If the remove operation was successful, inform all registered
                // StaticResource objects of the end of the thread association.
                // Allow any exception to percolate to the caller.
                // This is done first so that if there is an error, we don't leave the
                // transaction associated.

                if( statics != null )
                    statics.distributeEnd(current,false);

                // Push the given Control object onto the current one, and remove the
                // current association.

                StatusHolder outStatus = new StatusHolder();
                control.pushControl(current,outStatus);
		if(statsOn){
        		Thread thread = Thread.currentThread();
                	result = (threadContexts.remove(thread) != null);
		}
		else
			result=true;
	        m_tid.set(null);

                // The parent transaction has effectively been suspended - add it to the
                // set of suspended transactions.

		if(statsOn)
                	suspended.addElement(current);
            }
        } else
            result = true;

// If there is no current Control, then just make the new one current.

        if( result ) {

            // XA support: If the set_current operation was successful, inform all
            // registered StaticResource objects of the new thread association.
            // Allow any exception to percolate to the caller.
            // This is done first so that if there is an error, we don't leave the
            // transaction associated.

            if( statics != null )
                statics.distributeStart(control,stack);

            // Update the thread to Control mapping for the new Control.

	    if(statsOn){
        	Thread thread = Thread.currentThread();
            	threadContexts.put(thread,control);
	    }
	    m_tid.set(control);

            // Remove the Control from the set of suspended Control objects.

	    if(statsOn)
            	suspended.removeElement(control);

            // Increment the association count for the control object

            control.incrementAssociation();
        }

        return result;
    }

    /**Removes the association for the thread under which the operation was
     * invoked.
     * <p>
     * The (previously) associated Control object is returned.
     * <p>
     * If there was no association, the operation returns a NULL reference.
     * <p>
     * If the stacking flag is set, and there is an associated Control, the stacked
     * context (if any) becomes the current context when the operation completes.
     * <p>
     * For XA support, when an association is started or ended, all
     * registered StaticResource objects are informed of the change.
     *
     * @param unstack  Indicates whether the stacked Control object should be made
     *   the current one.
     *
     * @return  The current Control object.
     *
     * @see
     */
    static ControlImpl endCurrent( boolean unstack ) {

        // Ensure that the current thread association is valid.

        //boolean[] outBoolean = new boolean[1];
        ControlImpl result = (ControlImpl)m_tid.get();
        /*if( outBoolean[0] ) {
        }*/

        // If there is a current Control, remove its association.  If we were asked
        // to unstack, get the stacked Control, if any.  If there is one, set up the
        // thread association.

        if( result != null ){
	    if(statsOn){
        	Thread thread = Thread.currentThread();
            	threadContexts.remove(thread);
	    }
	    m_tid.set(null);

            // Decrement the count of associations for the Control object.

            result.decrementAssociation();

            // Add the Control to the set of suspended Control objects, if this is
            // a suspend and not an end.

            if( !unstack && statsOn) suspended.addElement(result);

                // XA support: If there was a current Control, inform all registered
                // StaticResource objects of the end of the thread association.
                // Allow any exception to percolate to the caller.

            if( statics != null )
                statics.distributeEnd(result,unstack);

                // If we were asked to unstack, get the stacked Control, if any.  If there
                // is one, set up the thread association.
                // Now that we have identified the first active ancestor, proceed to unstack
                // its parent.

            if( unstack ) {
                StatusHolder outStatus = new StatusHolder();
                ControlImpl stacked = result.popControl(outStatus);
                if( stacked != null &&
                    outStatus.value == Status.StatusActive ) {

                    // XA support: If there is a stacked context, inform all registered
                    // StaticResource objects of the new thread association.
                    // Allow any exception to percolate to the caller.
                    // This is done first so that if there is an error, we don't leave the
                    // transaction associated.

                    if( statics != null )
                        statics.distributeStart(stacked,false);

                    // The stacked Control is no longer suspended so is removed from the
                    // set of suspended transactions.

		    if(statsOn){
        		Thread thread = Thread.currentThread();
                    	threadContexts.put(thread,stacked);
                    	suspended.removeElement(stacked);
		    }
		    m_tid.set(stacked);
                }
            }
        }

        // If there is no current Control, just return NULL.

        else
            result = null;

        return result;
    }

    // COMMENT (Ram J) 12/18/2000
    // This is being accessed from OTS interceptors package to
    // check to see if there is a current transaction or not.
    public static boolean isTxAssociated() {
        //Thread thread = Thread.currentThread();
        //ControlImpl result = (ControlImpl) threadContexts.get(thread);
        //return (result != null);
	return (m_tid.get()!=null);
    }

    /**Ensures that an association with an aborted transaction is dealt with cleanly.
     *
     *
     * TN - do not dissociate thread even if it's aborted!!
     *
     * If the current Control object represents a transaction that has been
     * aborted, this method replaces the association by one with the first
     * ancestor that has not been aborted, if any, or no association, and the
     * method returns true as the output parameter. Otherwise the method returns
     * false as the output parameter.
     * <p>
     * If there is a current Control object in either case it is returned,
     * otherwise null is returned.
     *
     * @param aborted  A 1-element array which will hold the aborted indicator.
     *
     * @return  The current Control object.
     *
     * @see
     */
    private static ControlImpl
        endAborted( boolean[/*1*/] aborted, boolean endAssociation) {

        // Get the current thread identifier, and the corresponding Control object
        // if there is one.

        boolean completed = true;
        aborted[0] = false;

        ControlImpl result = (ControlImpl)m_tid.get();

        // If there is a current Control object, and it represents a transaction that
        // has been aborted, then we need to end its association with the current
        // thread of control.

        if( result != null )
            try {
                completed = (result.getTranState() != Status.StatusActive);
            } catch( Throwable exc ) {
		_logger.log(Level.FINE,"", exc);
            }

        if( result != null && completed ) {
            if (endAssociation) {
	        synchronized(CurrentTransaction.class){
			if(statsOn){
        			Thread thread = Thread.currentThread();
                		threadContexts.remove(thread);
			}
			m_tid.set(null);

                	// XA support: If there was a current IControl, inform all registered
                	// StaticResource objects of the end of the thread association.
                	// Allow any exception to percolate to the caller.

                	if( statics != null )
                    		statics.distributeEnd(result,false);

                	// Discard all stacked controls that represent aborted or unrecognised
                	// transactions.

                	result = result.popAborted();

                	// If there is a valid ancestor, make it the current one.

                	if( result != null ) {
				m_tid.set(result);
				if(statsOn){
        				Thread thread = Thread.currentThread();
                    			threadContexts.put(thread,result);
                    			suspended.removeElement(result);
				}
                	}

                	// XA support: If there is a stacked context, inform all registered
                	// StaticResource objects of the new thread association.
                	// Allow any exception to percolate to the caller.

                	if( statics != null )
                    		statics.distributeStart(result,false);
		}
            }
            aborted[0] = true;
        }

		if(_logger.isLoggable(Level.FINEST))
		{
			Thread thread = Thread.currentThread();
			_logger.logp(Level.FINEST,"CurrentTransaction","endAborted()",
					"threadContexts.get(thread) returned " + 
					result + " for current thread " + thread);
		}

        return result;
    }

    /**Adds the given Control object to the set of Control objects suspended in
     * the process.
     *
     * @param control  The Control object which has been suspended.
     *
     * @return
     *
     * @see
     */
    static void addSuspended( ControlImpl control ) {
    	if(statsOn)
        	suspended.addElement(control);
    }

    /**Removes the given Control object from the set of those suspended in the
     * process. The operation returns FALSE if the Control object has not been
     * suspended.
     *
     * @param control  The Control object which has been resumed/destroyed.
     *
     * @return  Indicates success of the operation.
     *
     * @see
     */
    static boolean removeSuspended( ControlImpl control ) {
        boolean result = true;
	if(statsOn)
		result=suspended.removeElement(control);
        return result;
    }

    /**Returns the current Control object.
     * <p>
     * That is, the Control object that corresponds to the thread
     * under which the operation was invoked. If there is no such association the
     * null value is returned.
     *
     * @param
     *
     * @return  The current Control object.
     *
     *
     * @see
     */
    public static ControlImpl getCurrent()
        throws TRANSACTION_ROLLEDBACK {

        //boolean[] outBoolean = new boolean[1];
        ControlImpl result = (ControlImpl)m_tid.get();

        return result;
    }

    /**Returns a reference to the current Coordinator.
     * <p>
     * That is, the Coordinator object that corresponds to the
     * thread under which the operation was invoked.
     * If there is no such association the null value is returned.
     * <p>
     * Note that this operation can be optimised so that the Coordinator reference is
     * stored along with the Control reference when the thread association is set up.
     *
     * @param
     *
     * @return  The current Coordinator.
     *
     * @exception TRANSACTION_ROLLEDBACK  The Coordinator has already been rolled
     *   back.
     * @exception Unavailable  The Coordinator object is not available.
     *
     * @see
     */
    static Coordinator getCurrentCoordinator()
        throws TRANSACTION_ROLLEDBACK, Unavailable {

        /* This method has been rewritten (Ram J)
         * in order to enable current.get_status() to be called
         * on a completed transaction, and get the completed status.
         * Previously, the first call to get_status() will return
         * the right completion status, and the second call to get_status
         * would return StatusNoTransaction, since the first call would end
         * the thread - tx association.
         */

        // Get the current thread identifier, and the corresponding
        // Control object if there is one.

        ControlImpl control = (ControlImpl)m_tid.get();
        Coordinator result = null;

        if (control != null) {

          if( Configuration.isLocalFactory()) {
            result = (Coordinator) ((ControlImpl) control).get_localCoordinator();
          } else {
            // this call may throw TRANSACTION_ROLLEDBACK
            // or INVALID_TRANSACTION
            result = control.get_coordinator();
          }
        }

        return result;

    }

    /**Returns the number of thread associations currently active for the given
     * transaction identifier.
     * <p>
     * A boolean value indicating whether there are outstanding requests is returned
     * as an output parameter.
     *
     * @param localTID     The local transaction identifier.
     * @param outstanding  A 1-element array which will indicate outstanding requests.
     *
     * @return  The number of active thread associations.
     *
     * @see
     */
    static int numActive( Long           localTID,
                                       boolean[/*1*/] outstanding ) {
	if(!statsOn){
		throw new NO_IMPLEMENT("statistics not on");
	}

        int result = 0;
        outstanding[0] = false;
        StatusHolder outStatus = new StatusHolder();

        // First check whether there are any outstanding requests.
        // Count all of the Control objects that have the same local TID as that given.

        Enumeration controls = threadContexts.elements();
        while( controls.hasMoreElements() ) {
            ControlImpl current = (ControlImpl)controls.nextElement();

            // If the Control object represents a transaction that has been completed,
            // don't count it.

            outStatus.value = Status.StatusRolledBack;
            try {
                Long currentLocalTID = current.getLocalTID(outStatus);
                if( outStatus.value == Status.StatusActive )
                    if( currentLocalTID.equals(localTID) ) {
                        outstanding[0] |= current.isOutgoing();
                        result++;
                    }
            } catch( Throwable exc ) {
		_logger.log(Level.FINE,"", exc);
            }
        }

        return result;
    }

    /**Registers the given StaticResource object.
     * <p>
     * The StaticResource object will be informed whenever any association of
     * a transaction with a thread is started or ended.
     *
     * @param obj  The StaticResource being registered.
     *
     * @return
     *
     * @see
     */
    synchronized static void registerStatic( StaticResource obj ) {

        // If the RegisteredStatics instance variable has not been created at this
        // point, create it.

        if( statics == null )
            statics = new RegisteredStatics();

        // Attempt to add the StaticResource reference to those already registered.

        statics.addStatic(obj);
    }

    /**Returns all the transactions in the system that are currently suspended
     * in the form of a sequence of Control objects.
     *
     * @param
     *
     * @return  The list of suspended Control objects.
     *
     * @see
     */
    static Control[] getSuspendedTransactions() {

	if(!statsOn){
		throw new NO_IMPLEMENT("statistics not on");
	}

        Control[] result = null;

        // Copy the contents of the suspended set into the array.

        int suspNum = suspended != null ? suspended.size() : 0;
        if( suspNum > 0 ) {
            result = new Control[suspNum];

            Enumeration controls = suspended.elements();
            int pos = 0;
            while( controls.hasMoreElements() )
                result[pos++] = ((ControlImpl)controls.nextElement()).object();
        }
        else
            result = new Control[0];

        return result;
    }

    /**Returns all the transactions in the system that are currently running
     * (i.e. not suspended) in the form of a sequence of Control objects.
     *
     * @param
     *
     * @return  The list of running Control objects.
     *
     * @see
     */
    static Control[] getRunningTransactions() {
	if(!statsOn){
		throw new NO_IMPLEMENT("statistics not on");
	}

        Control[] result = null;

        // Copy the Control objects which have thread associations into the result.

        int runNum = threadContexts != null ? threadContexts.size() : 0;
        if( runNum > 0 ) {
            result = new Control[runNum];

            Enumeration controls = threadContexts.elements();
            int pos = 0;
            while( controls.hasMoreElements() )
                result[pos++] = ((ControlImpl)controls.nextElement()).object();
        }
        else
            result = new Control[0];

        return result;
    }

    /**Returns all the transactions in the system that are currently running
     * or suspended in the form of a sequence of Control objects.
     *
     * @param
     *
     * @return  The list of all Control objects.
     *
     * @see
     */
    static Control[] getAllTransactions() {

	if(!statsOn){
		throw new NO_IMPLEMENT("statistics not on");
	}
        Control[] result = null;

        int allNum = threadContexts != null ? threadContexts.size()+suspended.size() : 0;
        if( allNum > 0 ) {
            result = new Control[allNum];

            // Copy the contents of the suspended set into the array.

            Enumeration controls = suspended.elements();
            int pos = 0;
            while( controls.hasMoreElements() )
                result[pos++] = ((ControlImpl)controls.nextElement()).object();

            // Copy the Control objects which have thread associations into the result.

            controls = threadContexts.elements();
            while( controls.hasMoreElements() )
                result[pos++] = ((ControlImpl)controls.nextElement()).object();
        }
        else
            result = new Control[0];

        return result;
    }

    /**Informs the CurrentTransaction that a request is being sent.
     * <p>
     * Returns the transaction context that should be established for the object in
     * the remote process.
     *
     * @param id      The request identifier.
     * @param holder  The completed context object.
     *
     * @return
     *
     * @exception TRANSACTION_ROLLEDBACK  The current transaction has been rolled
     *   back.  The message should not be sent and TRANSACTION_ROLLEDBACK should
     *   be returned to the caller.
     * @exception TRANSACTION_REQUIRED  There is no current transaction.
     *
     * @see
     */
    static void sendingRequest( int id,
                                             PropagationContextHolder holder )
        throws TRANSACTION_ROLLEDBACK, TRANSACTION_REQUIRED {

        // Empty out the context.
        // Ensure that the cached reference to the ORB is set up, and that the Any
        // value in the context is initialised.
        //$ The following is necessary for the context to be marshallable.  It is a
        //$ waste of time when there is no transaction, in which case we should be
        //$ throwing the TRANSACTION_REQUIRED exception.

        // COMMENT(Ram J) 11/19/2000 This is taken care of by the PI OTS
        // interceptors, so this has been commented out. If no current
        // transaction is available simply return. The PI OTS interceptor will
        // either raise a TRANSACTION_REQUIRED exception if the target policy
        // requires a transaction, else it will not provide a tx context.
        /*
        if( emptyContext.implementation_specific_data == null ) {
            if( orb == null )
                orb = Configuration.getORB();
            emptyContext.implementation_specific_data = orb.create_any();
            emptyContext.implementation_specific_data.insert_boolean(false);
        }
        holder.value = emptyContext;
        */

        // Ensure that the current Control object is valid.  Return immediately if
        // not.

        boolean[] outBoolean = new boolean[1];
        ControlImpl current = endAborted(outBoolean, false);
        if( outBoolean[0] ) {
            TRANSACTION_ROLLEDBACK exc = new TRANSACTION_ROLLEDBACK(0,CompletionStatus.COMPLETED_NO);
            throw exc;
        }

        // Throw the TRANSACTION_REQUIRED exception if there is no current transaction.

        if( current == null ) {
            //$   TRANSACTION_REQUIRED exc = new TRANSACTION_REQUIRED();
            //$   if( trc != null ) trc.event(EVT_THROW).data(exc).write();
            //$   throw exc;
            return;
        }

        // Get the the context from the Control object.
        // If the context is not available, then indicate that there is no transaction.

        try {
            holder.value = current.getTXContext();
        }

        // If the Coordinator is inactive, throw the INVALID_TRANSACTION exception,
        // as this will be because the transaction is not able to do transactional
        // work.

        catch (Unavailable exc) {
            INVALID_TRANSACTION ex2 = new INVALID_TRANSACTION(0,CompletionStatus.COMPLETED_NO);
            ex2.initCause(exc);
            throw ex2;
        }

        // If the Coordinator has rolled back, allow the TRANSACTION_ROLLEDBACK exception,
        // to pass to the caller.

        catch( TRANSACTION_ROLLEDBACK exc ) {
            endCurrent(true);
            current.destroy();
            throw (TRANSACTION_ROLLEDBACK)exc.fillInStackTrace();
        }

        // Any other exception is unexpected.  Assume there is no transaction.

        catch( Throwable exc ) {
	    _logger.log(Level.FINE,"", exc);
        }

        // Increase the count of outgoing requests for this transaction, if the
        // Control object is not a proxy.

        // COMMENT(Ram J) 11/25/2000 With the PI based OTS 1.2 implementation,
        // exception replies | location forwarded responses may not carry back
        // a tx svc context (since the server OTS interceptor send point may
        // not have been called. In such a case, it is impossible to enforce
        // checked behaviour. The next revision of OTS 1.2 should address this,
        // and provide a solution to the checked behaviour in a PI based OTS
        // implementation. Then, these checks shall be enabled.
        //current.incrementOutgoing();
    }

    /**Informs the CurrentTransaction that a reply has been received.
     *
     * @param id       The request identifier.
     * @param context  The PropagationContext from the message.
     * @param ex       The exception on the message.
     *
     * @return
     *
     * @exception WrongTransaction  The context returned on the reply is for a
     *   different transaction from the current one on the thread.
     *
     * @see
     */
    static void receivedReply( int id,
                                            PropagationContext context,
                                            org.omg.CORBA.Environment ex )
        throws org.omg.CORBA.WrongTransaction {

        // Look up the current Control object.

        //Thread thread = Thread.currentThread();
        ControlImpl current = (ControlImpl)m_tid.get();

        // If there is no current transaction, or an exception was raised, then just
        // return.

        if( current == null ) {
            return;
        }

        //$ If there is an active exception, report it.

        // OMG OTS issue 1819, if there is a system exception mark the
        // transaction for rollback
        java.lang.Exception ctxExc = ex.exception();
        if (ctxExc instanceof SystemException) {

            Coordinator currentCoord = null;
            try {
                if (Configuration.isLocalFactory()) {
                    currentCoord = current.get_localCoordinator();
                } else {
                    currentCoord = current.get_coordinator();
                }
            } catch (Unavailable exc) {
	        _logger.log(Level.FINE,"", exc);
            }

            if (currentCoord == null) {
                return; // no coord, cannot mark tx for rollback
            }

            try {
                currentCoord.rollback_only();
            } catch (Inactive exc) {
	        _logger.log(Level.FINE,"", exc);
            }

            // COMMENT (Ram J) (11/24/2000) This has been commented out since
            // the exception reply could have a tx context. Do further checks.
            //return;
        }

        // Return if there is no context on the message.

        if( context == null ||
            context.current == null ||
            context.current.coord == null ||
            context.current.otid.formatID == -1 ) {
            return;
        }

        // Get the global id from the current context. If the transaction is not
        // active, then end the current association.

        StatusHolder outStatus = new StatusHolder();
        outStatus.value = Status.StatusRolledBack;
        GlobalTID globalTID = null;
        try {
            globalTID = new GlobalTID(current.getGlobalTID(outStatus));
        } catch( Throwable exc ) {
            _logger.log(Level.FINE,"", exc);
        }

        // If the global identifier is NULL, then the Control object is unable to provide
        // us with checking behaviour.  We do not check in this case.

        if( globalTID != null ) {
            if( outStatus.value != Status.StatusActive ) {
                endCurrent(true);
                current.destroy();

                //      org.omg.CORBA.WrongTransaction exc = new org.omg.CORBA.WrongTransaction(0,CompletionStatus.COMPLETED_YES);
                org.omg.CORBA.WrongTransaction exc = new org.omg.CORBA.WrongTransaction();
                throw exc;
            }

            // If the global id is different from the one in the context, then raise the
            // org.omg.CORBA.WrongTransaction exception.

            if( !globalTID.isSameTID(context.current.otid) ) {
                //      org.omg.CORBA.WrongTransaction exc = new org.omg.CORBA.WrongTransaction(0,CompletionStatus.COMPLETED_YES);
                org.omg.CORBA.WrongTransaction exc = new org.omg.CORBA.WrongTransaction();
                throw exc;
            }
        }

        // If the Control object is not a proxy, then decrement the outgoing count.

        // COMMENT(Ram J) 11/25/2000 With the PI based OTS 1.2 implementation,
        // exception replies | location forwarded responses may not carry back
        // a tx svc context (since the server OTS interceptor send point may
        // not have been called. In such a case, it is impossible to enforce
        // checked behaviour. The next revision of OTS 1.2 should address this,
        // and provide a solution to the checked behaviour in a PI based OTS
        // implementation. Then, these checks shall be enabled.
        //current.decrementOutgoing();
    }

    /**Informs the CurrentTransaction that a request has been received.
     * <p>
     * The request contains the transaction context that should be established
     * for the object.
     *
     * @param id       The request identifier.
     * @param context  The PropagationContext from the message.
     *
     * @return
     *
     * @see
     */
    static void receivedRequest( int id,
                                              PropagationContext context ) {

        // Return if there is no context on the message.
        // If the transaction identifier in the context is NULL, just return.

        if( context == null ||
            context.current == null ||
            context.current.otid.formatID == -1 ) {
            return;
        }

        // Init TransactionManagerImpl in the JTS delegate, so that all calls from
        // this point forward use it correctly.
        JavaEETransactionManagerJTSDelegate.getInstance().initXA();

        // Use a local factory to recreate the transaction locally.

        //if( localFactory == null )
            //localFactory = Configuration.getFactory();
        //Control current = localFactory.recreate(context);
        Control current = Configuration.getFactory().recreate(context);

        // Record the imported transaction.

        importedTransactions.put(Thread.currentThread(),new GlobalTID(context.current.otid));

        // Create a new Control and associate it with the thread

        try {
            ControlImpl contImpl = null;
            if (Configuration.isLocalFactory()) {
                contImpl = (ControlImpl) current;
            } else {
                contImpl = ControlImpl.servant(JControlHelper.narrow(current));
            }
            setCurrent(contImpl,false);
        }

        // If any exception was thrown during that lot, then we have failed to
        // create a subordinate.  Do something drastic.

        catch( Throwable exc ) {
			_logger.log(Level.WARNING,"jts.unable_to_create_subordinate_coordinator", exc);
			 String msg = LogFormatter.getLocalizedMessage(_logger,
			 							"jts.unable_to_create_subordinate_coordinator");
			  throw  new org.omg.CORBA.INTERNAL(msg);
        }
    }

    /**Informs the object's Coordinator that a reply is being sent to the client.
     *
     * @param id      The request identifier.
     * @param holder  The context to be returned on the reply.
     *
     * @exception INVALID_TRANSACTION  The current transaction has outstanding work
     *   on this reply, and has been marked rollback-only, or the reply is returning
     *   when a different transaction is active from the one active when the request
     *   was imported.
     * @exception TRANSACTION_ROLLEDBACK  The current transaction has already been
     *   rolled back.
     *
     * @see
     */
    static void sendingReply( int id,
                                           PropagationContextHolder holder )
        throws INVALID_TRANSACTION, TRANSACTION_ROLLEDBACK {

        // Zero out context information.
        // Ensure that the cached reference to the ORB is set up, and that the Any
        // value in the context is initialised.
        //$ The following is necessary for the context to be marshallable.  It is a
        //$ waste of time when there is no transaction, in which case we should be
        //$ throwing the TRANSACTION_REQUIRED exception (?).

        if( emptyContext.implementation_specific_data == null ) {
            ORB orb = Configuration.getORB();
            emptyContext.implementation_specific_data = orb.create_any();
            emptyContext.implementation_specific_data.insert_boolean(false);
        }

        // COMMENT(Ram J) There is no need to send an empty context, if a tx
        // is not available. The PI based OTS hooks will not send a tx context
        // in the reply.
        /*
        holder.value = emptyContext;
        */

        // Ensure that the current Control object is valid.  Return immediately if not.

        boolean[] outBoolean = new boolean[1];
        ControlImpl current = endAborted(outBoolean, true);  // end association
        if( outBoolean[0] ) {
            importedTransactions.remove(Thread.currentThread());
            TRANSACTION_ROLLEDBACK exc = new TRANSACTION_ROLLEDBACK(0,CompletionStatus.COMPLETED_YES);
            throw exc;
        }

        // Get the global identifier of the transaction that was imported into this
        // thread.  If there is none, that is an error.

        Thread thread = Thread.currentThread();
        GlobalTID importedTID = (GlobalTID)importedTransactions.remove(thread);

        // If there is no import information, and no current transaction, then return
        // the empty context.

        if( importedTID == null && current == null ) {
            return;
        }

        // Check that the current transaction matches the one that was imported.

        StatusHolder outStatus = new StatusHolder();
        try {
            if( importedTID == null ||
                current == null ||
                !importedTID.isSameTID(current.getGlobalTID(outStatus)) ||
                outStatus.value != Status.StatusActive ) {
                INVALID_TRANSACTION exc = new INVALID_TRANSACTION(MinorCode.WrongContextOnReply,CompletionStatus.COMPLETED_YES);
                throw exc;
            }
        } catch( SystemException ex ) {
            _logger.log(Level.FINE,"", ex);
            INVALID_TRANSACTION exc = new INVALID_TRANSACTION(MinorCode.WrongContextOnReply,CompletionStatus.COMPLETED_YES);
            throw exc;
        }

        //$Get the Coordinator reference.

        CoordinatorImpl coord = null;
        Coordinator coordRef = null;
        try {
            if (Configuration.isLocalFactory()) {
                coord = (CoordinatorImpl) current.get_localCoordinator();
            } else {
                coordRef = current.get_coordinator();
                coord = CoordinatorImpl.servant(coordRef);
            }

            //    _logger.log(Level.FINE,"Servant = "+coord);

            // Check the Coordinator before sending the reply.
            // We must do this before ending the thread association to allow the
            // Coordinator to take advantage of registration on reply if available.
            // Note that if the Coordinator returns forgetMe, the global identifier
            // will have been destroyed at this point.

            CoordinatorImpl forgetParent = null;
            int[] outInt = new int[1];
            //StatusHolder outStatus = new StatusHolder();                                        
            try {
                forgetParent = coord.replyAction(outInt);
            } catch( Throwable exc ) {
                _logger.log(Level.FINE,"", exc);
            }
    
            int replyAction = outInt[0];
            if( replyAction == CoordinatorImpl.activeChildren ) {
                try {
                    coord.rollback_only();
                } catch( Throwable ex ) {
                    _logger.log(Level.FINE,"", ex);
                }

                INVALID_TRANSACTION exc = new INVALID_TRANSACTION(MinorCode.UnfinishedSubtransactions,
                                                              CompletionStatus.COMPLETED_YES);
                throw exc;
            }

            // End the current thread association.

            endCurrent(false);

            // If the transaction needs to be cleaned up, do so now.
            // We ignore any exception the end_current may have raised in this case.
            // The Control object is destroyed before the Coordinator so that it is not
            // in the suspended set when the Coordinator is rolled back.

            if( replyAction == CoordinatorImpl.forgetMe ) {
                current.destroy();
                coord.cleanUpEmpty(forgetParent);
            }

            // Otherwise, we have to check this reply.

            else {
                if( current.isAssociated() ||
                        current.isOutgoing() ) {
                    try {
                        coord.rollback_only();
                    } catch( Throwable exc ) {
                        _logger.log(Level.FINE,"", exc);
                    }

                    INVALID_TRANSACTION exc = new INVALID_TRANSACTION(MinorCode.DeferredActivities,
                                                                  CompletionStatus.COMPLETED_YES);
                    throw exc;
                }

                current.destroy();
            }

        } catch( INVALID_TRANSACTION exc ) {
            throw exc;
        } catch( Unavailable exc ) {
            _logger.log(Level.FINE,"", exc);
            // Ignore
        } catch( SystemException exc ) {
            _logger.log(Level.FINE,"", exc);
            // Ignore
        }

        // Create a context with the necessary information.
        // All we propagate back is the transaction id and implementation specific data.
    
        holder.value = new PropagationContext(0,new TransIdentity(null,null,importedTID.realTID),
                                              new TransIdentity[0],emptyContext.implementation_specific_data);

    }

    /**
     * Recreates a transaction based on the information contained in the 
     * transaction id (tid) and associates the current thread of control with
     * the recreated transaction.
     * 
     * @param tid  the transaction id.
     */
    public static void recreate(GlobalTID tid, int timeout) {              
        
        // check if there is any concurrent activity
        if (RecoveryManager.readAndUpdateTxMap(tid) == false) {          
            throw new INVALID_TRANSACTION(
                MinorCode.TX_CONCURRENT_WORK_DISALLOWED,
                CompletionStatus.COMPLETED_NO);            
        }

        // recreate the transaction
        
        try {                
            
            // Use a local factory to recreate the transaction locally.
            TransactionFactoryImpl factory = 
                (TransactionFactoryImpl) Configuration.getFactory();
            Control current = factory.recreate(tid, timeout);
            
            // Record the imported transaction.
            importedTransactions.put(Thread.currentThread(), tid);

            // Create a new Control and associate it with the thread   
            ControlImpl contImpl = null;
            if (Configuration.isLocalFactory()) {
                contImpl = (ControlImpl) current;
            } else {
                contImpl = ControlImpl.servant(JControlHelper.narrow(current));
            }
            setCurrent(contImpl,false);
            
        } catch (Throwable exc) {
            RecoveryManager.removeFromTxMap(tid); // remove tx id from map
			_logger.log(Level.WARNING,"jts.unable_to_create_subordinate_coordinator", exc);
			 String msg = LogFormatter.getLocalizedMessage(_logger,
			 							"jts.unable_to_create_subordinate_coordinator");
            throw new INVALID_TRANSACTION(msg,
                MinorCode.TX_RECREATE_FAILED, CompletionStatus.COMPLETED_MAYBE);
        }
    }
    
    /**
     * Disassociates the current thread of control from the specified 
     * transaction.
     * 
     * @param tid  the transaction id.
     */
    public static void release(GlobalTID tid) {    

        Thread thread = (Thread) RecoveryManager.getThreadFromTxMap(tid);

	if (thread == null || (thread != Thread.currentThread())) {
	    // the current thread is not in tx, so simply return.
	    return;
        } else {
	    RecoveryManager.removeFromTxMap(tid);
	}
        
        // Ensure that the current Control object is valid.
        boolean[] outBoolean = new boolean[1];
        ControlImpl control = endAborted(outBoolean, true);  // end association
        if (outBoolean[0]) {
            importedTransactions.remove(Thread.currentThread());
	    return; // thread is not associated with tx, simply return
        }

        // Get the global identifier of the transaction that was imported into
        // this thread.  If there is none, that is an error.
        GlobalTID importedTID = (GlobalTID) importedTransactions.remove(thread);

        // Check that the current transaction matches the one that was imported.
        StatusHolder outStatus = new StatusHolder();
        try {
            if (importedTID == null || control == null ||
                    !importedTID.isSameTID(control.getGlobalTID(outStatus)) ||
                    outStatus.value != Status.StatusActive) {
                INVALID_TRANSACTION exc = 
                    new INVALID_TRANSACTION(MinorCode.WrongContextOnReply,
                                            CompletionStatus.COMPLETED_YES);
                throw exc;
            }
        } catch (SystemException ex) {
            _logger.log(Level.FINE,"", ex);
            INVALID_TRANSACTION exc = 
                new INVALID_TRANSACTION(MinorCode.WrongContextOnReply,
                                        CompletionStatus.COMPLETED_YES);
            throw exc;
        }

        // End the current thread association.
        endCurrent(false);
        control.destroy();
    }

    /**Ends all thread associations for the given transaction.
     *
     * @param globalTID  The global transaction identifier.
     * @param aborted    Indicates whether the transaction has aborted.
     *
     * @return
     *
     * @see
     */
    //not used anywhere
    synchronized static void endAll( GlobalTID globalTID,
                                     boolean   aborted ) {
	throw new NO_IMPLEMENT("not implemented");
        // Modify any thread associations there may be for the transaction, to
        // indicate that the transaction has ended.
        /*StatusHolder outStatus = new StatusHolder();                                        

        Enumeration controls = threadContexts.elements();
	      int cz = threadContexts.size();			// Arun 9/27/99
	      while (cz-- > 0) {
            ControlImpl control = (ControlImpl)controls.nextElement();

            // If the Control object corresponds to the transaction being removed, then
            // inform it that the transaction has completed.

            try {
                if( globalTID.equals(control.getGlobalTID(outStatus)) &&
                    outStatus.value == Status.StatusActive )
                    control.setTranState(aborted ? Status.StatusRolledBack : Status.StatusCommitted);
            } catch( Throwable exc ) {
            }
        }

        // Modify any suspended Control objects there may be for the transaction, to
        // indicate that the transaction has ended.

        controls = suspended.elements();
	      cz = suspended.size();			// Arun 9/27/99
	      while(cz-- > 0) {
            try {
                ControlImpl control = (ControlImpl)controls.nextElement();

                // If the Control object corresponds to the transaction being removed, then
                // inform it that the transaction has completed.

                if( globalTID.equals(control.getGlobalTID(outStatus)) &&
                    outStatus.value == Status.StatusActive )
                    control.setTranState(aborted ? Status.StatusRolledBack : Status.StatusCommitted);
            } catch( Throwable exc ) {
            }
        }*/
    }

    /**Informs the CurrentTransaction that the transaction service is being shut
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
    static void shutdown( boolean immediate ) {

        //$Continue with shutdown/quiesce.
    }

    /**Dumps the static state of the class.
     *
     * @param
     *
     * @return
     *
     * @see
     */
    static void dump() {
    }

    /**Reports the contents of the CurrentTransaction tables.
     *$Only required for debug.
     *
     * @param immediate  Indicates whether to stop immediately.
     *
     * @return
     *
     * @see
     */
    /*
      static

      void report()
      {

      // Report on threadContexts.

      if( threadContexts.size() > 0 )
      {
	  _logger.log(Level.FINE,"CurrentTransaction.threadContexts non-empty");
      Enumeration keys = threadContexts.keys();
      while( keys.hasMoreElements() )
      {
      Thread thread = (Thread)keys.nextElement();
      ControlImpl contImpl = (ControlImpl)threadContexts.get(thread);
	  if(_logger.isLoggable(Level.FINE))
	  _logger.log(Level.FINE,"Thread :"+thread+" -> "+contImpl)
      }
      }
      else
	  _logger.log(Level.FINE,"CurrentTransaction.threadContexts empty");

      // Report on importedTransactions.

      if( importedTransactions.size() > 0 )
      {
		  _logger.log(Level.FINE,"CurrentTransaction.importedTransactions non-empty");
      Enumeration keys = importedTransactions.keys();
      while( keys.hasMoreElements() )
      {
      Thread thread = (Thread)keys.nextElement();
      GlobalTID tid = (GlobalTID)importedTransactions.get(thread);
	  if(_logger.isLoggable(Level.FINE))
	  _logger.log(Level.FINE,"Thread :"+thread+" -> "+tid)
      }
      }
      else
		_logger.log(Level.FINE,"CurrentTransaction.importedTransactions empty");
      // Report on suspended

      if( suspended.size() > 0 )
      {
	  	 _logger.log(Level.FINE,"CurrentTransaction.suspended non-empty");
      Enumeration keys = suspended.elements();
      while( keys.hasMoreElements() )
      {
	      ControlImpl contImpl = (ControlImpl)keys.nextElement();
		  _logger.log(Level.FINE,"ControlImpl:"+contImpl);
      }
      }
      else
	  	_logger.log(Level.FINE,"CurrentTransaction.suspended empty");
      } */
}

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
// Module:      ControlImpl.java
//
// Description: Transaction Control object implementation.
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
import org.omg.PortableServer.*;
import org.omg.CosTransactions.*;
import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.ServantAlreadyActive;

import com.sun.jts.codegen.otsidl.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import com.sun.logging.LogDomains;
import com.sun.jts.utils.LogFormatter;
/**The ControlImpl interface is our implementation of the standard Control
 * interface. It provides operations to set and subsequently obtain the
 * Terminator and Coordinator objects from the given context. Our
 * implementation also provides a method to obtain the corresponding
 * transaction identifiers, and stacking methods.
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

public class ControlImpl extends JControlPOA implements Control {
    
    private static POA poa = null;
    private Control thisRef = null;
    
    protected boolean         temporary = false;
    protected boolean         inSuspended = false;
    protected Status          tranState = Status.StatusActive;
    protected CoordinatorImpl coord = null;
    protected Coordinator     coordRef = null;
    protected TerminatorImpl  term = null;
    protected Terminator      termRef = null;
    protected ControlImpl     stacked = null;
    protected GlobalTID       globalTID = null;
    protected Long            localTID = null;
    protected boolean         representsRemote = false;
    protected PropagationContext cachedContext = null;
    
    // Transaction checking values
    
    protected int outgoing = 0;
    protected int association = 0;
    
    /**
     * Logger to log transaction messages
     */
    static Logger _logger = LogDomains.getLogger(Configuration.class, LogDomains.TRANSACTION_LOGGER);
    
    /**Default ControlImpl constructor.
     *
     * @param
     *
     * @return
     *
     * @see
     */
    ControlImpl() {
        
        tranState = Status.StatusActive;
        
        // Add the Control object to the set of
        // suspended ones for this process.
        inSuspended = true;
        CurrentTransaction.addSuspended(this);
        
    }
    
    /**Creates and initialises a new ControlImpl, given the TerminatorImpl and
     * CoordinatorImpl objects, and the corresponding global and local identifiers.
     *
     * @param term       The Terminator for the transaction.
     * @param coord      The Coordinator for the transaction.
     * @param globalTID  The global identifier for the transaction.
     * @param localTID   The local identifier for the transaction.
     *
     * @return
     *
     * @see
     */
    ControlImpl( TerminatorImpl  term,
    CoordinatorImpl coord,
    GlobalTID       globalTID,
    Long            localTID ) {
        
        
        // Set up the instance variables.
        
        this.term = term;
        this.coord = coord;
        this.globalTID = globalTID;
        this.localTID = localTID;
        tranState = Status.StatusActive;
        
        // Add the Control object to the set of suspended ones for this process.
        
        inSuspended = true;
        CurrentTransaction.addSuspended(this);
        
        // pass this control obj to the terminator to cleanup properly (Ram J)
        if (term != null) {
            term.setControl(this);
        }
    }
    
    /**Creates and initialises a new ControlImpl, given a Control object.
     * This constructor is used to create a local ControlImpl when a remote factory
     * has been used to create the Control object.
     *
     * @param ref  The Control object for the transaction.
     *
     * @return
     *
     * @exception Unavailable  The required information to set up the Control object
     *   is not available.
     *
     * @see
     */
    ControlImpl( Control ref )
    throws Unavailable {
        
        // Set up the instance variables.
        
        thisRef = ref;
        representsRemote = true;
        coordRef = ref.get_coordinator();
        termRef = ref.get_terminator();
        
        // Get a PropagationContext from the Coordinator, which will contain the
        // global TID for the transaction.
        
        try {
            cachedContext = coordRef.get_txcontext();
            globalTID = new GlobalTID(cachedContext.current.otid);
        } catch( Throwable exc ) {
        }
        
        tranState = Status.StatusActive;
        
        // Don't add the Control object to the set of suspended ones for this process,
        // as we may never get the opportunity to remove it.
        
        //$ CurrentTransaction.addSuspended(this);
        
    }
    
    /**Cleans up the state of the object.
     *
     * @param
     *
     * @return
     *
     * @see
     */
    synchronized public void doFinalize() {
        
        // Ensure that this object does not appear in the suspended set.
        
        if( inSuspended )
            CurrentTransaction.removeSuspended(this);
        inSuspended = false;
        
        // If there is a Terminator reference, destroy the Terminator.
        
        if( term != null ) term.destroy();
        
        // Remove the reference from the map.
        
        /* TN - do not nullify the references yet
        thisRef = null;
        coord = null;
        coordRef = null;
        term = null;
        termRef = null;
        stacked = null;
        globalTID = null;
        localTID = null;
         */
        
    }
    
    /**Returns the identifier that globally represents the transaction
     *
     * @param
     *
     * @return  The global identifier.
     *
     * @see
     */
    public GlobalTID getGlobalTID() {
        return globalTID;
    }
    
    /**Returns the identifier that globally represents the transaction, and a
     * value that indicates the state of the transaction.
     *
     * @param status  An object to hold the status value, or null.
     *
     * @return  The global identifier.
     *
     * @exception SystemException  An error occurred.  The minor code indicates
     *                             the reason for the exception.
     *
     * @see
     */
    synchronized public otid_t getGlobalTID( StatusHolder status )
    throws SystemException {
        
        otid_t result = null;
        
        // Return the transaction state.
        
        if( status != null )
            status.value = tranState;
        
        // If the object is asked for its OMGtid and has none, raise an exception.
        
        if( globalTID == null ) {
            
            INTERNAL exc = new INTERNAL(MinorCode.NoGlobalTID,
            CompletionStatus.COMPLETED_NO);
            throw exc;
        }
        else
            result = globalTID.realTID;
        
        return result;
    }
    
    /**Returns the identifier that locally represents the transaction, and a value
     * that indicates the state of the transaction.
     * <p>
     * If the transaction represented by the Control object has been completed,
     * the identifier is still returned if possible.
     *
     * @param status  An object to hold the status value, or null.
     *
     * @return  The local transaction identifier.
     *
     * @exception SystemException  An error occurred.  The minor code indicates
     *                             the reason for the exception.
     *
     * @see
     */
    synchronized public long getLocalTID( StatusHolder status )
    throws SystemException {
        
        long result = 0;
        
        // Return the transaction state.
        
        if( status != null )
            status.value = tranState;
        
        // If the internal id has not been defined, raise an exception.
        
        if( localTID == null ) {
            INTERNAL exc = new INTERNAL(MinorCode.NoGlobalTID,
            CompletionStatus.COMPLETED_NO);
            throw exc;
        } else {
            result = localTID.longValue();
        }
        
        return result;
    }
    
    
    /**Returns a reference to the stacked ControlImpl if there is one, otherwise
     * returns a NULL reference.
     * <p>
     * A value is returned that indicates the state of the transaction.
     * <p>
     * If the transaction represented by the Control object has been completed,
     * the reference is still returned if possible.
     * The stacked Control object is removed from the stack.
     *
     * @param status  An object to hold the status value, or null.
     *
     * @return  The stacked Control object.
     *
     * @see
     */
    synchronized ControlImpl popControl( StatusHolder status ) {
        
        ControlImpl result = null;
        
        // Return the transaction state.
        
        if( status != null )
            status.value = tranState;
        
        // Get the value of the stacked Control object.
        // Remove the stacked Control object (if any).
        
        result = stacked;
        stacked = null;
        
        return result;
    }
    
    /**Stacks the given ControlImpl on the target of the operation, so that it can
     * later be restored, and returns a value that indicates the state of the
     * transaction.
     * If there is already a stacked ControlImpl object, the operation throws an
     * exception.  If the transaction has already completed, no stacking is done.
     *
     * @param control  The Control object to be stacked.
     * @param status   An object to hold the status value, or null.
     *
     * @return
     *
     * @exception SystemException  An error occurred.  The minor code indicates
     *                             the reason for the exception.
     *
     * @see
     */
    synchronized void pushControl( ControlImpl  control,
    StatusHolder status )
    throws SystemException {
        
        if( tranState == Status.StatusActive ) {
            
            // If a Control object is already stacked on this one, raise an exception.
            
            if( stacked != null ) {
                INTERNAL exc = new INTERNAL(MinorCode.AlreadyStacked,
                CompletionStatus.COMPLETED_NO);
                throw exc;
            }
            
            // Make the stacked Control object the given one.
            
            else
                stacked = control;
        }
        
        // Return the transaction state.
        
        if( status != null )
            status.value = tranState;
    }
    
    /**Returns the Terminator object for the transaction.
     * We raise the Unavailable exception when there is no Terminator.
     * If the transaction has been completed, an appropriate exception is raised.
     *
     * This operation is part of the OMG interface and must not return any
     * exceptions other than those defined in the OMG interface.
     *
     * @param
     *
     * @return  The Terminator for the transaction.
     *
     * @exception Unavailable  The Terminator object is not available.
     * @exception SystemException  The operation failed.
     *
     * @see
     */
    
    synchronized public Terminator get_terminator()
    throws Unavailable, SystemException {
        
        Terminator result = termRef;
        
        // If the transaction has been completed, then raise an exception.
        // Raise either TRANSACTION_ROLLEDBACK or INVALID_TRANSACTION depending on
        // whether the transaction has aborted.
        
        if( tranState == Status.StatusCommitted ) {
            INVALID_TRANSACTION exc = new INVALID_TRANSACTION(MinorCode.Completed,
            CompletionStatus.COMPLETED_NO);
            throw exc;
        }
        
        if( tranState == Status.StatusRolledBack ) {
            TRANSACTION_ROLLEDBACK exc = new TRANSACTION_ROLLEDBACK(0,CompletionStatus.COMPLETED_NO);
            throw exc;
        }
        
        // If there is no Terminator reference, but a local Terminator, then get its
        // reference and remember it.
        
        if( termRef == null && term != null ) {
            termRef = term.object();
            result = termRef;
        }
        
        // If there is no reference available, throw the Unavailable exception.
        
        if( result == null ) {
            Unavailable exc = new Unavailable();
            throw exc;
        }
        
        return result;
    }
    
    synchronized public Terminator get_localTerminator()
    throws Unavailable, SystemException {
        
        Terminator result = (Terminator) term;
        
        // If the transaction has been completed, then raise an exception.
        // Raise either TRANSACTION_ROLLEDBACK or INVALID_TRANSACTION depending on
        // whether the transaction has aborted.
        
        if( tranState == Status.StatusCommitted ) {
            INVALID_TRANSACTION exc = new INVALID_TRANSACTION(MinorCode.Completed,
            CompletionStatus.COMPLETED_NO);
            throw exc;
        }
        
        if( tranState == Status.StatusRolledBack ) {
            TRANSACTION_ROLLEDBACK exc = new TRANSACTION_ROLLEDBACK(0,CompletionStatus.COMPLETED_NO);
            throw exc;
        }
        
        // If there is no reference available, throw the Unavailable exception.
        
        if (result == null) {
            Unavailable exc = new Unavailable();
            throw exc;
        }
        
        return result;
    }
    
    /**
     * Returns the Coordinator for the transaction.
     * If the transaction has been completed, an appropriate exception is raised.
     *
     * This operation is part of the OMG interface and must not return
     * any exceptions other than those defined in the OMG interface.
     *
     * @param
     *
     * @return  The Coordinator for the transaction.
     *
     * @exception Unavailable  The Coordinator is not available.
     * @exception SystemException  The operation failed.
     *
     * @see
     */
    synchronized public Coordinator get_coordinator()
    throws Unavailable, SystemException {
        
        Coordinator result = coordRef;
        
        // If the transaction has been completed, then raise an exception.
        // Raise either TRANSACTION_ROLLEDBACK or INVALID_TRANSACTION depending on
        // whether the transaction has aborted.
        
        if( tranState == Status.StatusCommitted ) {
            INVALID_TRANSACTION exc = new INVALID_TRANSACTION(MinorCode.Completed,
            CompletionStatus.COMPLETED_NO);
            throw exc;
        }
        
        if( tranState == Status.StatusRolledBack ) {
            TRANSACTION_ROLLEDBACK exc = new TRANSACTION_ROLLEDBACK(0,CompletionStatus.COMPLETED_NO);
            throw exc;
        }
        
        // If there is no Coordinator reference, but a local Coordinator, then get its
        // reference and remember it.
        
        if( coordRef == null && coord != null ) {
            coordRef = coord.object();
            result = coordRef;
        }
        
        // If there is no reference available, throw the Unavailable exception.
        
        if( result == null ) {
            Unavailable exc = new Unavailable();
            throw exc;
        }
        
        return result;
    }
    
    /**
     * Returns the Coordinator for the transaction.
     * If the transaction has been completed, an appropriate exception is raised.
     *
     * This operation is part of the OMG interface and must not return
     * any exceptions other than those defined in the OMG interface.
     *
     * @param
     *
     * @return  The Coordinator for the transaction.
     *
     * @exception Unavailable  The Coordinator is not available.
     * @exception SystemException  The operation failed.
     *
     * @see
     */
    synchronized public Coordinator get_localCoordinator()
    throws Unavailable, SystemException {
        
        Coordinator result = (Coordinator) coord;
        
        // If the transaction has been completed, then raise an exception.
        // Raise either TRANSACTION_ROLLEDBACK or INVALID_TRANSACTION depending on
        // whether the transaction has aborted.
        
        if( tranState == Status.StatusCommitted ) {
            INVALID_TRANSACTION exc = new INVALID_TRANSACTION(MinorCode.Completed,
            CompletionStatus.COMPLETED_NO);
            throw exc;
        }
        
        if( tranState == Status.StatusRolledBack ) {
            TRANSACTION_ROLLEDBACK exc = new TRANSACTION_ROLLEDBACK(0,CompletionStatus.COMPLETED_NO);
            throw exc;
        }
        
        // If there is no reference available, throw the Unavailable exception.
        
        if( result == null ) {
            Unavailable exc = new Unavailable();
            throw exc;
        }
        
        return result;
    }
    
    /**This operation returns a value indicating that asynchonrous requests issued
     * within the context of the current ControlImpl instance have not yet
     * completed.
     *
     * @param
     *
     * @return  Indicates there are outgoing requests.
     *
     * @see
     */
    
    synchronized boolean isOutgoing() {
        boolean result = (outgoing != 0);
        return result;
    }
    
    
    /**This operation returns a value which indicates that this ControlImpl instance
     * is associated with one or more threads.
     *
     * @param
     *
     * @return  Indicates an exisiting association.
     *
     * @see
     */
    synchronized boolean isAssociated() {
        boolean result = (association != 0);
        return result;
    }
    
    /**This operation returns the number of thread associations
     *
     * @param
     *
     * @return  Indicates the number of thread associations.
     *
     * @see
     */
    synchronized int numAssociated() {
        int result = association;
        return result;
    }
    
    /**Increment the thread association count.
     *
     * @param
     *
     * @return
     *
     * @see
     */
    synchronized void incrementAssociation() {
        association++;
    }
    
    /**Decrement the thread association count.
     *
     * @param
     *
     * @return  Indicates the association count was above zero.
     *
     * @see
     */
    synchronized boolean decrementAssociation() {
        boolean result = (association > 0);
        if( result ) association--;
        return result;
    }
    
    /**Increment the incomplete asynchronous request counter.
     *
     * @param
     *
     * @return
     *
     * @see
     */
    synchronized void incrementOutgoing() {
        outgoing++;
    }
    
    /**Decrement the incomplete asynchronous request counter.
     *
     * @param
     *
     * @return  Indicates the request counter was above zero.
     *
     * @see
     */
    synchronized boolean decrementOutgoing() {
        boolean result = (outgoing > 0);
        if( result ) outgoing--;
        return result;
    }
    
    /**Returns the state of the transaction as the Control object knows it.
     *
     * @param
     *
     * @return  The transaction state.
     *
     * @see
     */
    synchronized public Status getTranState(){
        Status result = tranState;
        return result;
    }
    
    /**Sets the state of the transaction as the Control object knows it.
     * No checking is done to verify the state change is valid.
     *
     * @param int  The new state.
     *
     * @return
     *
     * @see
     */
    synchronized public void setTranState( Status newState ) {
        tranState = newState;
    }
    
    
    /**Locates the first stacked ancestor which has not aborted.  If there is no
     * such ancestor the operation returns null.
     *
     * @param
     *
     * @return  The first stacked Control which does not represent an aborted
     *          transaction.
     *
     * @see
     */
    synchronized ControlImpl popAborted() {
        
        // Start with this object's stacked Control.
        
        ControlImpl result = stacked;
        boolean validTID = false;
        StatusHolder outStatus = new StatusHolder();
        while( result != null && !validTID ) {
            
            // Get the local transaction identifier for the stacked Control object,
            // and ask the RecoveryManager if it represents a valid transaction.
            
            Long localTID = null;
            try {
                localTID = result.getLocalTID(outStatus); 
                validTID = RecoveryManager.validLocalTID(localTID);
            } catch( Throwable exc ) {}
            
            // If the transaction identifier is not valid, then the transaction must have
            // rolled back, so discard it and get its stacked Control object, if any.
            
            if( !validTID ) {
                
                // Get the stacked Control object from the one that represents a rolled
                // back transaction, and try to resume that one instead.
                
                ControlImpl stacked = result.popControl(outStatus);
                result.destroy();
                
                // Discard the rolled-back Control object and continue until a valid one, or
                // no stacked Control is found.
                
                result = stacked;
            }
        }
        
        return result;
    }
    
    /**Determines whether the ControlImpl object represents a remote Control object
     * or a local one.
     *
     * @param
     *
     * @return  Indicates whether the ControlImpl represents a remote Control.
     *
     * @see
     */
    
    synchronized boolean representsRemoteControl() {
        boolean result = representsRemote;
        return result;
    }
    
    /**Returns the transaction context for the Coordinator.
     *
     * @param
     *
     * @return  The transaction context.
     *
     * @exception Unavailable No transaction context is available.
     *
     * @see
     */
    synchronized PropagationContext getTXContext()
    throws Unavailable {
        PropagationContext result = null;
        
        // If the ControlImpl represents a remote transaction, then use the cached
        // context, or get one from the Coordinator if there is no cached context.
        
        if( representsRemote ) {
            if( cachedContext == null )
                try {
                    cachedContext = coordRef.get_txcontext();
                } catch( OBJECT_NOT_EXIST exc ) {
                    TRANSACTION_ROLLEDBACK ex2 = new TRANSACTION_ROLLEDBACK(0,CompletionStatus.COMPLETED_NO);
                    throw ex2;
                }
            result = cachedContext;
        }
        
        // For local transactions, get the context from the Coordinator.
        
        else
            result = coord.get_txcontext();
        
        return result;
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
    
    /**Returns the CORBA Object which represents this object.
     *
     * @param
     *
     * @return  The CORBA object.
     *
     * @see
     */
    synchronized final Control object() {
        if( thisRef == null ) {
            if( poa == null )
                poa = Configuration.getPOA("transient"/*#Frozen*/);
            
            try {
                poa.activate_object(this);
                thisRef = ControlHelper.narrow(poa.servant_to_reference(this));
            } catch( ServantAlreadyActive sexc ) {
                _logger.log(Level.SEVERE,"jts.create_control_object_error",sexc);
                String msg = LogFormatter.getLocalizedMessage(_logger,
                "jts.create_control_object_error");
                throw  new org.omg.CORBA.INTERNAL(msg);
            } catch( ServantNotActive snexc ) {
                _logger.log(Level.SEVERE,"jts.create_control_object_error",snexc);
                String msg = LogFormatter.getLocalizedMessage(_logger,
                "jts.create_control_object_error");
                throw  new org.omg.CORBA.INTERNAL(msg);
            } catch( Exception exc ) {
                _logger.log(Level.SEVERE,"jts.create_control_object_error",exc);
                String msg = LogFormatter.getLocalizedMessage(_logger,
                "jts.create_control_object_error");
                throw  new org.omg.CORBA.INTERNAL(msg);
            }
        }
        
        return thisRef;
    }
    
    /**Returns the ControlImpl which serves the given object.
     *
     * @param  The CORBA Object.
     *
     * @return  The ControlImpl object which serves it.
     *
     * @see
     */
    synchronized public static final ControlImpl servant( JControl control ) {
        ControlImpl result = null;
        
        // GDH we will not be able to obtain the
        // servant from our local POA for a proxy control object.
        // so return null
        if ( control != null && Configuration.getProxyChecker().isProxy(control)) {
            return result;
        }
        
        if( control instanceof ControlImpl ) {
            result = (ControlImpl)control;
        } else if( poa != null )
           try {
                result = (ControlImpl)poa.reference_to_servant(control);
                if( result.thisRef == null )
                    result.thisRef = control;
            } catch( Exception exc ) {
                _logger.log(Level.WARNING,"jts.cannot_locate_servant","Control");
                
            }
        
        return result;
    }
    
    /**Destroys the ControlImpl object.
     *
     * @param
     *
     * @return
     *
     * @see
     */
    synchronized final void destroy() {
        // GDH: We have no desire to destroy an underlying remote control object, instead we
        // release it. We will finalise the local control wrapper below
        if ( thisRef != null && Configuration.getProxyChecker().isProxy(thisRef)) {
            thisRef._release();
        } else {
            if( poa != null &&
            thisRef != null )
                try {
                    poa.deactivate_object(poa.reference_to_id(thisRef));
                    thisRef = null;
                } catch( Exception exc ) {
                    _logger.log(Level.WARNING,"jts.object_destroy_error","Control");
                    
                    
                }
        }
        
        doFinalize();
        
    }
    
    /**Added to prevent null delegate problem.
     *
     * @param
     *
     * @return
     *
     * @see
     */
    public boolean equals(java.lang.Object o) {
        return this == o;
    }

    /**Added because this class overrides equals() method.
     *
     */
    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }
    
    /*
     * These methods are there to satisy the compiler. At some point
     * when we move towards a tie based model, the org.omg.Corba.Object
     * interface method implementation below shall be discarded.
     */
    
    public org.omg.CORBA.Object _duplicate() {
        throw new org.omg.CORBA.NO_IMPLEMENT("This is a locally constrained object.");
        
    }
    
    public void _release() {
        throw new org.omg.CORBA.NO_IMPLEMENT("This is a locally constrained object.");
        
    }
    
    public boolean _is_a(String repository_id) {
        throw new org.omg.CORBA.NO_IMPLEMENT("This is a locally constrained object.");
        
    }
    
    public boolean _is_equivalent(org.omg.CORBA.Object that) {
        throw new org.omg.CORBA.NO_IMPLEMENT("This is a locally constrained object.");
        
    }
    
    public boolean _non_existent() {
        throw new org.omg.CORBA.NO_IMPLEMENT("This is a locally constrained object.");
        
    }
    
    public int _hash(int maximum) {
        throw new org.omg.CORBA.NO_IMPLEMENT("This is a locally constrained object.");
        
    }
    
    public Request _request(String operation) {
        throw new org.omg.CORBA.NO_IMPLEMENT("This is a locally constrained object.");
        
    }
    
    public Request _create_request(Context ctx,
    String operation,
    NVList arg_list,
    NamedValue result) {
        throw new org.omg.CORBA.NO_IMPLEMENT("This is a locally constrained object.");
        
    }
    
    public Request _create_request(Context ctx,
    String operation,
    NVList arg_list,
    NamedValue result,
    ExceptionList exceptions,
    ContextList contexts) {
        throw new org.omg.CORBA.NO_IMPLEMENT("This is a locally constrained object.");
        
    }
    
    public org.omg.CORBA.Object _get_interface_def() {
        throw new org.omg.CORBA.NO_IMPLEMENT("This is a locally constrained object.");
        
    }
    
    public org.omg.CORBA.Policy _get_policy(int policy_type) {
        throw new org.omg.CORBA.NO_IMPLEMENT("This is a locally constrained object.");
        
    }
    
    public org.omg.CORBA.DomainManager[] _get_domain_managers() {
        throw new org.omg.CORBA.NO_IMPLEMENT("This is a locally constrained object.");
        
    }
    
    public org.omg.CORBA.Object _set_policy_override(
    org.omg.CORBA.Policy[] policies,
    org.omg.CORBA.SetOverrideType set_add) {
        throw new org.omg.CORBA.NO_IMPLEMENT("This is a locally constrained object.");
        
    }
}

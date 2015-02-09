/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2015 Oracle and/or its affiliates. All rights reserved.
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
// Module:      TransactionFactoryImpl.java
//
// Description: TransactionFactory object implementation.
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

import org.omg.CORBA.*;
import org.omg.PortableServer.*;
import org.omg.CosTransactions.*;

import org.omg.CosNaming.*;
import org.omg.CosNaming.NamingContextPackage.*;

import com.sun.jts.trace.*;

import java.util.logging.Logger;
import java.util.logging.Level;
import com.sun.logging.LogDomains;
import com.sun.jts.utils.LogFormatter;

/**
 * The TransactionFactoryImpl interface is our implementation of the standard
 * TransactionFactory interface. It provides operations to create the objects
 * required to process a transaction.
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

class TransactionFactoryImpl extends TransactionFactoryPOA implements TransactionFactory {

    private static POA poa = null;
    private TransactionFactory thisRef = null;

    static boolean active = true;

    /*Logger to log transaction messages*/  
    static Logger _logger = LogDomains.getLogger(TransactionFactoryImpl.class, LogDomains.TRANSACTION_LOGGER);
    /**
     * Constructor for the TransactionFactoryImpl.  Passes through
     * to the parent constructor.
     *
     * @param
     *
     * @return
     *
     * @see
     */
    TransactionFactoryImpl() {

        // Initialise the TimeoutManager and RecoveryManager.

        TimeoutManager.initialise();
        RecoveryManager.initialise();
    }

    /**
     * Creates the Coordinator, Control and Terminator objects for a
     * new top-level transaction.
     *
     * @param timeOut  The timeout value for the transaction.
     *
     * @return  The Control object for the new transaction.
     *
     * @exception SystemException  An error occurred.
     *
     * @see
     */
    public Control create(int timeOut) throws SystemException {

        Control result = null;
    
        ControlImpl cimpl = localCreate(timeOut);

        if (cimpl == null) {
            return result;
        }

        if (Configuration.isLocalFactory()) {
            result = (Control) cimpl;
        } else {
            result = cimpl.object();
        }

        return result;
    }

    /**
     * Creates the Coordinator, Control and Terminator objects for a
     * new top-level transaction.
     *
     * @param timeOut  The timeout value for the transaction.
     *
     * @return  The local Control object for the new transaction.
     *
     * @exception SystemException  An error occurred.
     *
     * @see
     */
    public ControlImpl localCreate(int timeOut) throws SystemException {

        // If the transaction service is not active, throw an exception.

        if (!active) {
            NO_PERMISSION exc =
                new NO_PERMISSION(0, CompletionStatus.COMPLETED_NO);
            throw exc;
        }

        // Ensure that resync has completed.

        RecoveryManager.waitForResync(timeOut);

        // Create a new top-level Coordinator, and initialise it
        // with the given time-out value.  If the operation fails,
        // return a NULL Control
        // object, and whatever exception was raised.

        TopCoordinator coordinator = null;
        TerminatorImpl terminator = null;
        ControlImpl result = null;

        try {
            coordinator = new TopCoordinator(timeOut);

            // Create a Terminator object, and initialise it with
            // the Coordinator reference and a flag
            // to indicate that it does not represent a
            // subtransaction.  If the operation fails,
            // return a NULL Control object, and whatever exception was raised.

            terminator = new TerminatorImpl(coordinator,false);

            // Create a Control object, and initialise it with the Terminator,
            // Coordinator and global OMGtid. If the operation fails,
            // return a NULL Control object, and whatever exception was raised.

            // result = new ControlImpl(terminator, coordinator,
            //                         new GlobalTID(coordinator.getGlobalTID()),
            //                          coordinator.getLocalTID());
            result = new ControlImpl(terminator, coordinator,
                                     coordinator.getGlobalTid(),
                                     coordinator.getLocalTID());
            if(_logger.isLoggable(Level.FINE))
            {
                _logger.logp(Level.FINE,"TransactionFactoryImpl","localCreate()",
                        "Control object :" + result + 
                        " corresponding to this transaction has been created"+
                        "GTID is : "+
                        ((TopCoordinator)coordinator).superInfo.globalTID.toString());
            }
    
        } catch (Throwable exc) {

            // If an error occurred, free up the objects.

            if (coordinator != null) {
                coordinator.doFinalize();
            }

            if (result != null) {
                ((ControlImpl) result).doFinalize();
            }

            result = null;
        }

        return result;
    }

    /**
     * Creates the Coordinator, Control and Terminator objects
     * for a transaction based on the context passed in.
     *
     * @param context  The context for the transaction.
     *
     * @return  The Control object for the new transaction.
     *
     * @exception SystemException  An error occurred.
     *
     * @see
     */
    public Control recreate(PropagationContext context)
            throws SystemException {

        // If the transaction service is not active, throw an exception.

        if (!active) {
            NO_PERMISSION exc =
                new NO_PERMISSION(0,CompletionStatus.COMPLETED_NO);
            throw exc;
        }

        // If the transaction identifier in the context is NULL, just return.

        if (context.current == null || context.current.otid.formatID == -1) {
            return null;
        }

        // First check whether there is already a local subordinate
        // for the given transaction.

        GlobalTID globalTID = new GlobalTID(context.current.otid);
        CoordinatorImpl subordinate =
            RecoveryManager.getCoordinator(globalTID);
        Control result = null;

        // If there is no local subordinate, but the Coordinator
        // in the context represents a local object,
        // then the Coordinator must have been created by the client using
        // this TransactionFactory, and the transaction it represents
        // must have been rolled back - invoking a method on the Coordinator
        // at this point should throw OBJECT_NOT_EXIST.  This means that we
        // cannot import the transaction, and should throw
        // TRANSACTION_ROLLEDBACK.

        ProxyChecker checker = Configuration.getProxyChecker();
        if (subordinate == null &&
                !checker.isProxy(context.current.coord)) {

            TRANSACTION_ROLLEDBACK exc =
                new TRANSACTION_ROLLEDBACK(0, CompletionStatus.COMPLETED_NO);
            throw exc;
        }

        // If there is no subordinate, then we must import the transaction.
        // We first must make sure that resync is not in progress.

        if (subordinate == null) {
            RecoveryManager.waitForResync();
            subordinate = RecoveryManager.getCoordinator(globalTID);
        }

        // If there is no local subordinate, then for each OMGtid
        // in the context, make sure there is a local
        // Coordinator set up in this process - required for
        // AIX, where we need to ensure every  ancestor has a locally created
        // transaction.  When we have threading on AIX this will not
        // be absolutely necessary, but will still be desirable.

        try {

            if (subordinate == null) {

                if (context.parents.length > 0) {

                    CoordinatorImpl[] newAncestors =
                        new CoordinatorImpl[context.parents.length];

                    // If there are indeed ancestors, go through them
                    // starting at the top-level
                    // ancestor, which is the last in the sequence.

                    for (int i = context.parents.length - 1; i >= 0; i--) {

                        // If a subordinate does not exist, create one.

                        GlobalTID subGlobalTID =
                            new GlobalTID(context.parents[i].otid);
                        subordinate =
                            RecoveryManager.getCoordinator(subGlobalTID);

                        if (subordinate == null) {

                            if (i == context.parents.length - 1) {

                                // If it is the last ancestor, create a
                                // temporary top-level subordinate. Use a
                                // duplicate of the proxy to allow the context
                                // to be fully destroyed.

                                subordinate = new TopCoordinator(
                                    context.timeout,
                                    subGlobalTID,
                                    (Coordinator) context.parents[i].
                                        coord._duplicate(),
                                    true);

                            } else {

                                // If it is not the last ancestor, create a
                                // temporary child subordinate. Use a duplicate
                                // of the proxy to allow the context to be
                                // fully destroyed.

                                CoordinatorImpl[] subAncestors =
                                    new CoordinatorImpl[context.parents.length
                                                        - i - 1];
                                System.arraycopy(newAncestors, i + 1,
                                                 subAncestors, 0,
                                                 context.
                                                    parents.length - i - 1);

                                subordinate =
                                    new SubCoordinator(
                                        subGlobalTID,
                                        (Coordinator)context.parents[i].
                                            coord._duplicate(),
                                        true,
                                        subAncestors);

                                // Make sure that the parent knows about
                                // its new child.

                                newAncestors[i+1].addChild(subordinate);
                            }
                         }

                        // Replace the Coordinator reference in the sequence
                        // of ancestors with that of the local subordinate,
                        // first releasing the proxy in the context.

                        context.parents[i].coord._release();
                        newAncestors[i] = subordinate;
                    }

                    // Now the ancestors have been 'converted' create a
                    // subordinate of the given subtransaction.
                    // Use a duplicate of the proxy to allow
                    // the context to be fully destroyed.

                    subordinate =
                        new SubCoordinator(globalTID,
                                           (Coordinator) context.current.
                                                coord._duplicate(),
                                           true,
                                           newAncestors);

                    //$ This flag was set to false.  Surely this subordinate
                    // is temporary?

                    // Make sure the parent knows about its new child.

                    newAncestors[0].addChild(subordinate);
                } else {

                    // If there are no ancestors, but the subordinate
                    // does not already exist, create a top-level subordinate.
                    // Use a duplicate of the proxy to allow the context
                    // to be fully destroyed.

                    subordinate = new TopCoordinator(
                                    context.timeout,
                                    globalTID,
                                    (Coordinator) context.
                                        current.coord._duplicate(),
                                    true);
                }

                //$ This flag was set to false.
                // Surely this subordinate is temporary?

            } else {

                // If there already is a subordinate, then ensure
                // that it knows it is not a temporary Coordinator.
                subordinate.setPermanent();

            }

            // Create a new Control object for the transaction.
            // We do not create a local Terminator

            if (Configuration.isLocalFactory()) {
                result = (Control) new ControlImpl(null, subordinate, globalTID,
                                     subordinate.getLocalTID()
                                    );
            } else {
                result = new ControlImpl(null, subordinate, globalTID,
                                     subordinate.getLocalTID()
                                    ).object();
            }

        } catch (Throwable exc) {

            // If any exception was thrown during that lot, then we
            // have failed to create a subordinate.  Do something drastic.
            _logger.log(Level.SEVERE,"jts.unable_to_create_subordinate_coordinator");
             String msg = LogFormatter.getLocalizedMessage(_logger,
                                       "jts.unable_to_create_subordinate_coordinator");
             throw  new org.omg.CORBA.INTERNAL(msg);
        }

        return result;
    }

    /**
     * Creates the Coordinator, Control and Terminator objects
     * for a transaction based on the Xid object passed in.
     *
     * @param xid  Xid object containing a transaction context.
     *
     * @return  The Control object for the new transaction.
     *
     * @exception SystemException  An error occurred.
     */
    public Control recreate(GlobalTID tid, int timeout)
            throws SystemException {

        // If the transaction service is not active, throw an exception.
        if (!active) {
            NO_PERMISSION exc =
                new NO_PERMISSION(0, CompletionStatus.COMPLETED_NO);
            throw exc;
        }

        // First check whether there is already a local subordinate
        // for the given transaction.
        CoordinatorImpl subordinate = RecoveryManager.getCoordinator(tid);
        Control result = null;

        // If there is no subordinate, then we must import the transaction.
        // We first must make sure that resync is not in progress.
        if (subordinate == null) {
            RecoveryManager.waitForResync();
            subordinate = RecoveryManager.getCoordinator(tid);
        }

        // If there is no local subordinate, then for each OMGtid
        // in the context, make sure there is a local
        // Coordinator set up in this process - required for
        // AIX, where we need to ensure every  ancestor has a locally created
        // transaction.  When we have threading on AIX this will not
        // be absolutely necessary, but will still be desirable.

        try {
            if (subordinate == null) {
                // If there are no ancestors, but the subordinate
                // does not already exist, create a top-level subordinate.
                // Use a duplicate of the proxy to allow the context
                // to be fully destroyed.
                subordinate = new TopCoordinator(
                                timeout, tid, new TxInflowCoordinator(),
                                true);
            } else {
                Status status = subordinate.get_status();
                if ((status != Status.StatusMarkedRollback) &&
                        (status != Status.StatusActive)) {
                    throw new INVALID_TRANSACTION("tx completion in-progress");           
                }
                // If there already is a subordinate, then ensure
                // that it knows it is not a temporary Coordinator.
                subordinate.setPermanent();
            }

            // Create a new Control object for the transaction.
            // We do not create a local Terminator.
            if (Configuration.isLocalFactory()) {
                result = (Control) new ControlImpl(null, subordinate, tid,
                                     subordinate.getLocalTID()
                                    );
            } else {
                result = new ControlImpl(null, subordinate, tid,
                                     subordinate.getLocalTID()
                                    ).object();
            }
        } catch (Throwable exc) {
            // If any exception was thrown during that lot, then we
            // have failed to create a subordinate.
             _logger.log(Level.WARNING,"jts.unable_to_create_subordinate_coordinator");
             String msg = LogFormatter.getLocalizedMessage(_logger,
                                        "jts.unable_to_create_subordinate_coordinator");
            INTERNAL intExc = new INTERNAL(msg);
            intExc.initCause(exc);
            throw intExc;
        }

        return result;
    }

    /**
     * Prevents any further transactional activity in the process.
     *
     * @param
     *
     * @return
     *
     * @see
     */
    static void deactivate() {

        active = false;

        // Shut down the TimeoutManager and RecoveryManager.

        TimeoutManager.shutdown(false);
        RecoveryManager.shutdown(false);
        DelegatedRecoveryManager.shutdown(false);
    }

    /**Returns the CORBA Object which represents this object.
     *
     * @param
     *
     * @return  The CORBA object.
     *
     * @see
     */
    synchronized TransactionFactory object() {
        if (thisRef == null) {
            if (poa == null) {
                poa = Configuration.getPOA("transient"/*#Frozen*/);
            }

            try {
                poa.activate_object(this);
                thisRef = TransactionFactoryHelper.
                            narrow(poa.servant_to_reference(this));
                //thisRef = (TransactionFactory)this;

                /*
                 * ADDED(Ram J) Whenever a TransactionFactory corba object is
                 * created, put it in the cosnaming service.
                 */

                NamingContext namingContext = null;
                try {
                    namingContext = NamingContextHelper.narrow(
                        Configuration.getORB().resolve_initial_references("NameService"/*#Frozen*/));
                } catch (Exception exc) {
                    _logger.log(Level.WARNING,"jts.orb_not_running");
                    // Return - otherwise it'll just be an NPE reported in the next block
                    return thisRef;
                }

                try {
                    NameComponent nc = new NameComponent(TransactionFactoryHelper.id(), "");
                    NameComponent path[] = { nc };
                    namingContext.rebind(path, thisRef);
                } catch (Exception exc) {
                    _logger.log(Level.WARNING,"jts.cannot_register_with_orb",
                            "TransactionFactory");
                }
            } catch (Exception exc) {
                _logger.log(Level.SEVERE,"jts.create_transactionfactory_object_error");
                 String msg = LogFormatter.getLocalizedMessage(_logger,
                                         "jts.create_transactionfactory_object_error");
                 throw  new org.omg.CORBA.INTERNAL(msg);
            }
        }

        return thisRef;
    }

    /**Returns the TransactionFactoryImpl which serves the given object.
     *
     * @param  The CORBA Object.
     *
     * @return  The TransactionFactoryImpl object which serves it.
     *
     * @see
     */
    synchronized static final TransactionFactoryImpl servant(TransactionFactory factory) {
        TransactionFactoryImpl result = null;

        // we will not be able to obtain the
        // servant from our local POA for a proxy factory object.
        // so return null
        if (factory != null && Configuration.getProxyChecker().isProxy(factory)) {
            return result;
        }

        if (factory instanceof TransactionFactoryImpl ) {
            result = (TransactionFactoryImpl) factory;
        } else if (poa != null) {
            try {
                result = (TransactionFactoryImpl) poa.reference_to_servant(factory);
                if( result.thisRef == null )
                    result.thisRef = factory;
            } catch( Exception exc ) {
                _logger.log(Level.WARNING,"jts.cannot_locate_servant",
                        "TransactionFactory");
            }
        }

        return result;
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

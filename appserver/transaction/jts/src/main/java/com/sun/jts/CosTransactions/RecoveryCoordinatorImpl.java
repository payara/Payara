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
// Module:      RecoveryCoordinatorImpl.java
//
// Description: Transaction RecoveryCoordinator object implementation.
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
import com.sun.jts.trace.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import com.sun.logging.LogDomains;
import com.sun.jts.utils.LogFormatter;

/**
 * The RecoveryCoordinatorImpl interface is our implementation of the standard
 * RecoveryCoordinator interface. It allows recoverable objects to drive the
 * recovery process in certain situations. Each instance of this class is
 * implicitly associated with a single resource registration, and may only be
 * used by that resource in that particular transaction for which it is
 * registered. An instance of this class should be accessed from only one
 * thread within a process.
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
//                Added in COMMITTING to replay_completion
//-----------------------------------------------------------------------------

class RecoveryCoordinatorImpl extends RecoveryCoordinatorPOA {

    private static boolean recoverable = false;
    private static POA poa = null;
    private RecoveryCoordinator thisRef = null;
    private int internalSeq = 0;
	/*
		Logger to log transaction messages
	*/  
    static Logger _logger = LogDomains.getLogger(RecoveryCoordinatorImpl.class, LogDomains.TRANSACTION_LOGGER);
    GlobalTID globalTID = null;

	RecoveryCoordinatorImpl() {}

    /**
     * Sets up the RecoveryCoordinator with the global identifier.
     * <p>
     * This is so that it can always find the Coordinator to inform it of the
     * requirement for recovery of the given Resource.
     * <p>
     * An internal sequence number is used to differentiate RecoveryCoordinator
     * objects used for the same Coordinator object within a process.  This is
     * so that they each get a unique object identifier.
     *
     * @param globalTID  The global transaction identifier.
     * @param sequence   An internal sequence number to differentiate objects.
     *
     * @return
     *
     * @see
     */
    RecoveryCoordinatorImpl(GlobalTID globalTID, int sequence) {

        this.globalTID = globalTID;
        internalSeq = sequence;

        // MODIFICATION (Ram Jeyaraman) comment out the code
        // below, as it does nothing.
        /*
        byte[] tidBytes = globalTID.toBytes();
        byte[] id = new byte[tidBytes.length + 4];
        System.arraycopy(tidBytes, 0, id, 4, tidBytes.length);
        id[0] = (byte) internalSeq;
        id[1] = (byte)(internalSeq >> 8);
        id[2] = (byte)(internalSeq >> 16);
        id[3] = (byte)(internalSeq >> 24);
        */
    }

    /**
     * Informs the Transaction Service that the given Resource object has been
     * prepared but has not received a commit or rollback operation.
     * <p>
     * If the transaction outcome is unknown, the Resource object passed
     * on this operation will be called at some later time for
     * commit or rollback.
     *
     * @param res  The Resource to be recovered.
     *
     * @return  The state of the transaction.
     *
     * @exception NotPrepared  The transaction for which the
     *   RecoveryCoordinator was created has not prepared.
     *
     * @see
     */
    public Status replay_completion(Resource res) throws NotPrepared {
	
		if(_logger.isLoggable(Level.FINE))
        {
			 _logger.logp(Level.FINE,"RecoveryCoordinatorImpl",
			 		"replay_completion()","replay_completion on Resource:"+
					res);
        }


        Status result = Status.StatusRolledBack;

        CoordinatorImpl coord = RecoveryManager.getCoordinator(globalTID);
        if (coord != null) {
            try {
                result = coord.get_status();
            } catch (SystemException exc) {}
        }

        switch (result.value()) {

        /*
         * If the transaction is still active, raise the NotPrepared
         * exception. The Coordinator must be marked rollback-only at
         * this point because we cannot allow the transaction to
         * complete if a participant has failed.
         */

        case Status._StatusActive :
        case Status._StatusMarkedRollback :
            try {
                coord.rollback_only();
            } catch (Throwable exc) {}

            throw new NotPrepared();

        /*
         * If the transaction is prepared, the caller must wait for the
         * Coordinator to tell it what to do, so return an unknown status, and
         * do nothing.  Note that if this Coordinator is sitting waiting for
         * its superior, this could take a int time.
         */

        case Status._StatusPrepared :
            result = Status.StatusUnknown;
            break;

        /*
         * If the transaction has been committed, the caller will receive
         * a commit.
         *
         * GDH If the transaction is commiting then we pass this on
         * to the caller. This state (added in OTS 1.1 means that
         * TopCoordinator.recover must now accept the COMMITTING state.
         */

        case Status._StatusCommitting :
            // MODIFICATION (Ram Jeyaraman) commented out the code below,
            // since a StatusCommitting will be upgraded to Committed in
            // the subordinate.
            /*
            // (Ram Jeyaraman) let the subordinate wait, and allow the root
            // finish driving the commit.
            result = Status.StatusUnknown;
            */
            break;

        case Status._StatusCommitted :
            break;

        case Status._StatusRolledBack :

            // If the transaction has been rolled back, and there is
            // no Coordinator for the transaction, we must invoke rollback
            // directly, as it will not be done otherwise.  However for
            // proxies, this rollback cannot be done from this thread as
            // it would cause deadlock in the server requesting resync.

            if (coord == null) {

                if (!Configuration.getProxyChecker().isProxy(res)) {
                    rollbackOrphan(res);
                } else {

                    // We must pass a duplicate of the proxy to the
                    // rollback thread because this proxy will be destroyed
                    // when the replay_completion request returns
                    // to the remote server.

                    try {
                        OrphanRollbackThread rollbackThread =
                            new OrphanRollbackThread(
                                this, (Resource) res._duplicate());
                        rollbackThread.start();
                    } catch (SystemException exc) {}
                }
            }

            break;

        /*
         * In any other situation, assume that the transaction has been rolled
         * back. As there is a Coordinator, it will direct the Resource to roll
         * back.
         */

        default :
            result = Status.StatusRolledBack;
        }

        return result;
    }

    // same as replay_completion(res) : added for delegated recovery support
    public Status replay_completion(Resource res, String logPath) throws NotPrepared {
	
        if(_logger.isLoggable(Level.FINE))
        {
	     _logger.logp(Level.FINE,"RecoveryCoordinatorImpl",
			"replay_completion()","replay_completion on Resource:"+ res);
        }

        Status result = Status.StatusRolledBack;

        CoordinatorImpl coord = DelegatedRecoveryManager.getCoordinator(globalTID, logPath);
        if (coord != null) {
            try {
                result = coord.get_status();
            } catch (SystemException exc) {}
        }

        switch (result.value()) {

        /*
         * If the transaction is still active, raise the NotPrepared
         * exception. The Coordinator must be marked rollback-only at
         * this point because we cannot allow the transaction to
         * complete if a participant has failed.
         */

        case Status._StatusActive :
        case Status._StatusMarkedRollback :
            try {
                coord.rollback_only();
            } catch (Throwable exc) {}

            throw new NotPrepared();

        /*
         * If the transaction is prepared, the caller must wait for the
         * Coordinator to tell it what to do, so return an unknown status, and
         * do nothing.  Note that if this Coordinator is sitting waiting for
         * its superior, this could take a int time.
         */

        case Status._StatusPrepared :
            result = Status.StatusUnknown;
            break;

        /*
         * If the transaction has been committed, the caller will receive
         * a commit.
         *
         * GDH If the transaction is commiting then we pass this on
         * to the caller. This state (added in OTS 1.1 means that
         * TopCoordinator.recover must now accept the COMMITTING state.
         */

        case Status._StatusCommitting :
            // MODIFICATION (Ram Jeyaraman) commented out the code below,
            // since a StatusCommitting will be upgraded to Committed in
            // the subordinate.
            /*
            // (Ram Jeyaraman) let the subordinate wait, and allow the root
            // finish driving the commit.
            result = Status.StatusUnknown;
            */
            break;

        case Status._StatusCommitted :
            break;

        case Status._StatusRolledBack :

            // If the transaction has been rolled back, and there is
            // no Coordinator for the transaction, we must invoke rollback
            // directly, as it will not be done otherwise.  However for
            // proxies, this rollback cannot be done from this thread as
            // it would cause deadlock in the server requesting resync.

            if (coord == null) {

                if (!Configuration.getProxyChecker().isProxy(res)) {
                    rollbackOrphan(res);
                } else {

                    // We must pass a duplicate of the proxy to the
                    // rollback thread because this proxy will be destroyed
                    // when the replay_completion request returns
                    // to the remote server.

                    try {
                        OrphanRollbackThread rollbackThread =
                            new OrphanRollbackThread(
                                this, (Resource) res._duplicate());
                        rollbackThread.start();
                    } catch (SystemException exc) {}
                }
            }

            break;

        /*
         * In any other situation, assume that the transaction has been rolled
         * back. As there is a Coordinator, it will direct the Resource to roll
         * back.
         */

        default :
            result = Status.StatusRolledBack;
        }

        return result;
    }

    /**
     * This method invoked rollback on the Resource that is passed as a
     * parameter.
     * <p>
     * This procedure may be called as the main procedure of a new thread,
     * which must be done for remote Resource objects
     * during resync to avoid the possibility of deadlock during resync.
     *
     * <p>
     * It is called directly when the Resource is not a proxy.
     *
     * @param res  The Resource to be rolled back.
     *
     * @return
     *
     * @see
     */
    void rollbackOrphan(Resource res) {

        try {
            res.rollback();
        } catch(Throwable exc) {

            // If the rollback raised a heuristic exception, it can
            // only be reported in a message as it will never reach
            // the Coordinator.

            if (exc instanceof HeuristicCommit ||
                    exc instanceof HeuristicMixed ||
                    exc instanceof HeuristicHazard) {
				_logger.log(Level.WARNING,"jts.heuristic_exception",exc.toString());
            } else {}
        }

        // We must release the proxy now.

        res._release();
    }

    /**
     * Creates the RecoveryCoordinatorImpl with the given key.
     * <p>
     * This is done when the RecoveryCoordinator object is recreated after the
     * server has been restarted.
     * <p>
     * The first four bytes of the key are an internal sequence number used to
     * differentiate RecoveryCoordinator objects created in the
     * same process for the same transaction.
     * <p>
     * The rest of the key is the global transaction identifier.
     *
     * @param key  The key for the object.
     *
     * @return
     *
     * @see
     */
    RecoveryCoordinatorImpl(byte[] key) {

        // Get the global transaction identifier from the key.

        byte[] tidBytes = new byte[key.length - 4];

        // BUGFIX (Ram Jeyaraman) changed the order of array copy.
        // previously, an source and destination array was wrong.
        //System.arraycopy(tidBytes, 0, key, 4, tidBytes.length);
        System.arraycopy(key, 4, tidBytes, 0, tidBytes.length);
        
        globalTID = new GlobalTID(tidBytes);

        // Ensure that recovery has completed so that
        // we can get the Coordinator.

        RecoveryManager.waitForRecovery();

        // Leave other members at the default values.
    }

    /**
     * Returns the CORBA Object which represents this object.
     *
     * @param
     *
     * @return  The CORBA object.
     *
     * @see
     */
    synchronized final RecoveryCoordinator object() {

        if (thisRef == null) {
            if (poa == null) {
                poa = Configuration.getPOA("RecoveryCoordinator"/*#Frozen*/);
                recoverable = Configuration.isRecoverable();
            }

            try {

                if (recoverable && globalTID != null) {
                    // Create the object id from the global transaction
                    // identifier and the internal sequence number.

                    byte[] tidBytes = globalTID.toBytes();
                    byte[] id = new byte[tidBytes.length + 4];
                    System.arraycopy(tidBytes, 0, id, 4, tidBytes.length);
                    id[0] = (byte) internalSeq;
                    id[1] = (byte)(internalSeq >> 8);
                    id[2] = (byte)(internalSeq >> 16);
                    id[3] = (byte)(internalSeq >> 24);

                    // Activate the object and create the reference.

                    poa.activate_object_with_id(id, this);

                    org.omg.CORBA.Object obj =
                        poa.create_reference_with_id(
                            id, RecoveryCoordinatorHelper.id());
                    thisRef = RecoveryCoordinatorHelper.narrow(obj);
                    //thisRef = (RecoveryCoordinator) this;
                } else {
                    poa.activate_object(this);
                    org.omg.CORBA.Object obj = poa.servant_to_reference(this);
                    thisRef = RecoveryCoordinatorHelper.narrow(obj);
                    //thisRef = (RecoveryCoordinator)this;
                }
            } catch(Exception exc) {
				_logger.log(Level.SEVERE,"jts.create_recoverycoordinator_error");
				 String msg = LogFormatter.getLocalizedMessage(_logger,
				 						"jts.create_recoverycoordinator_error");
				  throw  new org.omg.CORBA.INTERNAL(msg);
            }
        }

        return thisRef;
    }

    /**
     * Destroys the RecoveryCoordinatorImpl object.
     *
     * @param
     *
     * @return
     *
     * @see
     */
    synchronized final void destroy() {

        try {
            if (poa != null && thisRef != null) {
                poa.deactivate_object(poa.reference_to_id(thisRef));
                thisRef = null;
            } else {
                // BUGFIX(Ram J) It is possible that the
                // RecoveryCoordinator object was activated via the activation
                // daemon. In that case, there is no guarantee
                // that poa and thisRef are set to a meaningful value.
                // So, try to deactivate the RecoveryCoordinator object anyway.

                POA rcPoa = null;
                if (poa == null) {
                    rcPoa = Configuration.getPOA("RecoveryCoordinator"/*#Frozen*/);
                } else {
                    rcPoa = poa;
                }

                if (thisRef == null) {
                    rcPoa.deactivate_object(rcPoa.servant_to_id(this));
                } else {
                    rcPoa.deactivate_object(rcPoa.reference_to_id(thisRef));
                    thisRef = null;
                }
            }
        } catch( Exception exc ) {
				_logger.log(Level.WARNING,"jts.object_destroy_error","RecoveryCoordinator");
        }

        globalTID = null;
        internalSeq = 0;
    }
}

/**
 * This class is provided to allow a Resource to be rolled back by the
 * RecoveryCoordinator on a new thread.  This is required for
 * Resource objects which are proxies because deadlock would occur
 * if the rollback was called from the same thread as the one on
 * which the replay_completion was executing.
 *
 * @version 0.01
 *
 * @author Simon Holdsworth, IBM Corporation
 *
 * @see
 */
class OrphanRollbackThread extends Thread {
    Resource resource = null;
    RecoveryCoordinatorImpl recovery = null;

    /**
     * OrphanRollbackThread constructor.
     *
     * @param recovery
     * @param resource
     *
     * @return
     *
     * @see
     */
    OrphanRollbackThread(RecoveryCoordinatorImpl recovery,
            Resource resource) {
        this.resource = resource;
        this.recovery = recovery;
        setName("JTS Orphan Rollback Thread"/*#Frozen*/);
    }

    /**
     * Calls the RecoveryCoordinator to rollback the Resource.
     *
     * @param
     *
     * @return
     *
     * @see
     */
    public void run() {
        recovery.rollbackOrphan(resource);
    }
}

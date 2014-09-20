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
// Module:      CoordinatorSynchronizationImpl.java
//
// Description: Subordinate Coordinator synchronization interface.
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

import java.util.logging.Logger;
import java.util.logging.Level;
import com.sun.logging.LogDomains;
import com.sun.jts.utils.LogFormatter;

/**The CoordinatorSynchronizationImpl interface allows a subordinate Coordinator
 * to be informed of the completion of a transaction, both before the transaction
 * is prepared, and after it is committed or rolled back. Every
 * Synchronization object registered with the subordinate should be called
 * before the operation returns to the superior. An instance of this class
 * should be accessed from only one thread within a process.
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

class CoordinatorSynchronizationImpl extends SynchronizationPOA {

    private static POA poa = null;
    private Synchronization thisRef = null;

    private Long           localTID = null;
    private TopCoordinator coordinator = null;
	/*
		Logger to log transaction messages
	*/ 
	static Logger _logger = LogDomains.getLogger(CoordinatorSynchronizationImpl.class, LogDomains.TRANSACTION_LOGGER);

    /**Default CoordinatorSynchronizationImpl constructor.
     *
     * @param
     *
     * @return
     *
     * @see
     */
    CoordinatorSynchronizationImpl() {
    }

    /**Sets up a new CoordinatorSynchronizationImpl object with the Coordinator reference so
     * that it can pass on synchronization requests.
     *
     * @param coord  The Coordinator for the transaction.
     *
     * @return
     *
     * @see
     */
    CoordinatorSynchronizationImpl( TopCoordinator coord ) {

        // Set the instance variables to the values passed in.

        coordinator = coord;
        try {
            localTID = coord.getLocalTID();
        } catch( SystemException exc ) {}

    }

    /**Passes on the before completion operation to the Coordinator.
     *
     * @param
     *
     * @return
     *
     * @exception SystemException  The operation failed.  The minor code provides
     *                             a reason for the failure.
     *
     * @see
     */
    public void before_completion() 
        throws SystemException {

        // If there is no Coordinator reference, raise an exception.

        if( coordinator == null ) {
            INTERNAL exc = new INTERNAL(MinorCode.NoCoordinator,
                                        CompletionStatus.COMPLETED_NO);
            throw exc;
        }

        // Pass the before completion operation on to the coordinator.

        coordinator.beforeCompletion();

    }

    /**Passes on the after completion operation to the Coordinator.
     *
     * @param status  The state of the transaction.
     *
     * @return
     *
     * @exception SystemException  The operation failed.  The minor code provides
     *                             a reason for the failure.
     *
     * @see
     */
    public void after_completion( Status status )
        throws SystemException {

        // If there is no Coordinator reference, raise an exception.

        if( coordinator == null ) {
            INTERNAL exc = new INTERNAL(MinorCode.NoCoordinator,
                                        CompletionStatus.COMPLETED_NO);
            throw exc;
        }

        // Pass the after completion operation on to the coordinator.
        // Destroy myself.

        coordinator.afterCompletion(status);
        destroy();

    }

    /**Returns the CORBA Object which represents this object.
     *
     * @param
     *
     * @return  The CORBA object.
     *
     * @see
     */
    Synchronization object() {
        if( poa == null ) poa = Configuration.getPOA("transient"/*#Frozen*/);
        if( thisRef == null ) {
            if( poa == null )
                poa = Configuration.getPOA("transient"/*#Frozen*/);

            try {
                poa.activate_object(this);
                thisRef = SynchronizationHelper.
                            narrow(poa.servant_to_reference(this));
                //thisRef = (Synchronization)this;
            } catch( ServantAlreadyActive saexc ) {
				_logger.log(Level.SEVERE,
						"jts.create_CoordinatorSynchronization_object_error");
				 String msg = LogFormatter.getLocalizedMessage(_logger,
	 						  "jts.create_CoordinatorSynchronization_object_error");
				  throw  new org.omg.CORBA.INTERNAL(msg);
            } catch( ServantNotActive snexc ) {
				_logger.log(Level.SEVERE,
						"jts.create_CoordinatorSynchronization_object_error");
				 String msg = LogFormatter.getLocalizedMessage(_logger,
	 						  "jts.create_CoordinatorSynchronization_object_error");
				  throw  new org.omg.CORBA.INTERNAL(msg);
            } catch( Exception exc ) {
				_logger.log(Level.SEVERE,
						"jts.create_CoordinatorSynchronization_object_error");
				 String msg = LogFormatter.getLocalizedMessage(_logger,
	 						  "jts.create_CoordinatorSynchronization_object_error");
				  throw  new org.omg.CORBA.INTERNAL(msg);
            }
        }

        return thisRef;
    }

    /**Destroys the CoordinatorSynchronizationImpl object.
     *
     * @param
     *
     * @return
     *
     * @see
     */
    void destroy() {
        if( poa != null &&
            thisRef != null )
            try {
                poa.deactivate_object(poa.reference_to_id(thisRef));
                thisRef = null;
            } catch( Exception exc ) {
				 _logger.log(Level.WARNING,"jts.object_destroy_error",
				 		"CoordinatorResource");
            }

        coordinator = null;
    }

    /**
     * Returns the CoordinatorSynchronizationImpl which serves the given object.
     *
     * @param  The CORBA Object.
     *
     * @return  The CoordinatorSynchronizationImpl object which serves it.
     *
     * @see
     */
    synchronized static final CoordinatorSynchronizationImpl servant(Synchronization sync) {
        CoordinatorSynchronizationImpl result = null;

        // we will not be able to obtain the
        // servant from our local POA for a proxy sync object.
        // so return null
        if (sync != null && Configuration.getProxyChecker().isProxy(sync)) {
            return result;
        }

        if (sync instanceof CoordinatorSynchronizationImpl ) {
            result = (CoordinatorSynchronizationImpl) sync;
        } else if (poa != null) {
            try {
                result = (CoordinatorSynchronizationImpl) poa.reference_to_servant(sync);
                if( result.thisRef == null )
                    result.thisRef = sync;
            } catch( Exception exc ) {
			    _logger.log(Level.WARNING,"jts.cannot_locate_servant",
							"CoordinatorSynchronization");
            }
        }

        return result;
    }
}

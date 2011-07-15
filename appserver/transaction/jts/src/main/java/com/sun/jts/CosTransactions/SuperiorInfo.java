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
// Module:      SuperiorInfo.java
//
// Description: Global transaction information.
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

import org.omg.CORBA.*;
import org.omg.CosTransactions.*;
import com.sun.corba.ee.spi.presentation.rmi.StubAdapter;

/**
 * The SuperiorInfo interface provides operations that record the local
 * transaction ID, global transaction ID for the superior (client) and the
 * superior Coordinator.
 * As an instance of this class may be accessed from multiple threads within
 * a process, serialisation for thread-safety is necessary in the
 * implementation. The information recorded in an instance of this class
 * needs to be reconstructible in the case of a system failure.
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

class SuperiorInfo {
    /**
     * The local identifier for the transaction.
     */
    Long localTID = null;

    /**
     * The global identifier for the transaction.
     */
    GlobalTID globalTID = null;

    /**
     * The reference of the superior Coordinator.
     */
    Coordinator superior = null;

    /**
     * The reference of the RecoveryCoordinator returned on the
     * register_resource call to the superior Coordinator,
     * if any.  Note that this member may be directly read,
     *  but must be set using the setRecovery method in this class.
     */
    RecoveryCoordinator recovery = null;

    /**
     * The reference of the Resource registered on the
     * register_resource call to the superior Coordinator, if any.
     * Note that this member may be directly
     * read, but must be set using the setResource method in this class.
     */
    SubtransactionAwareResource resource = null;

    private CoordinatorLog   logRecord = null;
    private java.lang.Object logSection = null;
    private int              resyncRetries = 0;

    private final static String LOG_SECTION_NAME = "SI"/*#Frozen*/;

    /**
     * Default SuperiorInfo constructor.
     *
     * @param
     *
     * @return
     *
     * @see
     */
    SuperiorInfo() { }

    /**
     * Defines the local transaction ID, global ID and superior Coordinator
     * reference.
     * <p>
     * The CoordinatorLog is used as the basis for recovering the
     * SuperiorInfo at restart. The SuperiorInfo section in the CoordinatorLog
     * is created, with the global identifier added to the section.
     *
     * @param localTID   The local identifier for the transaction.
     * @paran globalTID  The global identifier for the transaction.
     * @param superior   The superior Coordinator reference (may be null).
     * @param log        The CoordinatorLog object (may be null).
     *
     * @return
     *
     * @see
     */
    SuperiorInfo(Long localTID, GlobalTID globalTID,
                 Coordinator superior, CoordinatorLog log) {

        this.localTID = localTID;
        this.globalTID = globalTID;
        this.superior = superior;
        recovery = null;
        resource = null;
        logRecord = log;
        resyncRetries = 0;

        // Set up CoordinatorLog section for this class

        if (log != null) {

            // Create a section in the CoordinatorLog for SuperiorInfo

            logSection = log.createSection(LOG_SECTION_NAME);

            // Add the Global Id to the SuperiorInfo section of the log

            log.addData(logSection,globalTID.toBytes());
        }
    }

    /**
     * Cleans up the objects state.
     *
     * @param
     *
     * @return
     *
     * @see
     */
    public void doFinalize() {

        // Release superior Coordinator references.

        if (superior != null &&
                !(superior instanceof org.omg.CORBA.LocalObject)) {
            superior._release();
        }

        //$#ifndef OTS_DEFERRED_REGISTRATION
        //$For deferred registration, the CurrentTransaction
        //$will release the proxy, so there is no need to do it here.
        //$For non-deferred registration, the RecoveryCoordinator
        //$must be destroyed by the subordinate as no-one else will.

        if (recovery != null && 
                !(recovery instanceof org.omg.CORBA.LocalObject)) {
            recovery._release();
        }

        //$#endif /* OTS_DEFERRED_REGISTRATION */

        localTID = null;
        globalTID = null;
        superior = null;
        recovery = null;
        resource = null;
    }

    /**
     * Directs the SuperiorInfo to recover its state after a failure, based on
     * the given CoordinatorLog object.
     * <p>
     * The SuperiorInfo then adds the given Coordinator to the
     * RecoveryManager mappings.  If the SuperiorInfo has already been defined
     * or reconstructed, the operation does nothing. 
     * If the state cannot be reconstructed, the
     * Coordinator is not added.
     * <p>
     * The global identifier, local ID, RecoveryCoordinator
     * and CoordinatorResource object references are restored.
     *
     * @param log    The CoordinatorLog object for the transaction.
     * @param coord  The Coordinator object recreated after recovery.
     *
     * @return
     *
     * @see
     */
    void reconstruct(CoordinatorLog log, CoordinatorImpl coord) {

        superior = null;

        // Recover our state from the CoordinatorLog object.
        // Get the section id in the CoordinatorLog for SuperiorInfo

        logSection = log.createSection(LOG_SECTION_NAME);
        byte[][] logData = log.getData(logSection);

        // Construct the global identifier.

        globalTID = new GlobalTID(logData[0]);

        // Get the local transaction identifier from the CoordinatorLog
        // attribute rather than from the log record itself.

        localTID = log.localTID;
        logRecord = log;
        resyncRetries = 0;

        // Add the Coordinator to the RecoveryManager map.  This is
        // to allow the recovery of the CoordinatorResource reference,
        // which results in a call to getCoordinator.

        RecoveryManager.addCoordinator(globalTID, localTID, coord, 0);

        // Get all objects that were logged.  If this SuperiorInfo represents
        // a root TopCoordinator object, then there will be no objects logged.

        java.lang.Object[] logObjects = log.getObjects(logSection);

        try {
            if (logObjects.length > 1) {
                if (((org.omg.CORBA.Object) logObjects[0]).
                        _is_a(RecoveryCoordinatorHelper.id())) {
                    // TN - used to be com.sun.CORBA.iiop.CORBAObjectImpl
                    java.lang.Object rcimpl = logObjects[0];
                        
                    /*
                    String[] ids = StubAdapter.getTypeIds(rcimpl);
                    for (int i = 0; i < ids.length; i++)
                        if( trc != null )
                            trc.exit(998).data(i).data(ids[i]).write();
                    */

                    // TN - used to be com.sun.CORBA.iiop.CORBAObjectImpl
                    java.lang.Object crimpl = logObjects[1];
                        
                    /*
                    ids = StubAdapter.getTypeIds(crimpl);
                    for( int i = 0; i < ids.length; i++ )
                        if( trc != null )
                            trc.exit(998).data(i).data(ids[i]).write();
                    */
                    recovery = RecoveryCoordinatorHelper.
                                narrow((org.omg.CORBA.Object) logObjects[0]);

                    resource = SubtransactionAwareResourceHelper.
                                narrow((org.omg.CORBA.Object) logObjects[1]);
                } else {
                    recovery = RecoveryCoordinatorHelper.
                                narrow((org.omg.CORBA.Object) logObjects[1]);
                    resource = SubtransactionAwareResourceHelper.
                                narrow((org.omg.CORBA.Object) logObjects[0]);
                }
            } else {
                recovery = null;
                resource = null;
            }
        } catch (Throwable exc) {}
    }

    // same as reconstruct method: added for delegated recovery support
    void delegated_reconstruct(CoordinatorLog log, CoordinatorImpl coord, String logPath) {

        superior = null;

        // Recover our state from the CoordinatorLog object.
        // Get the section id in the CoordinatorLog for SuperiorInfo

        logSection = log.createSection(LOG_SECTION_NAME);
        byte[][] logData = log.getData(logSection);

        // Construct the global identifier.

        globalTID = new GlobalTID(logData[0]);

        // Get the local transaction identifier from the CoordinatorLog
        // attribute rather than from the log record itself.

        localTID = log.localTID;
        logRecord = log;
        resyncRetries = 0;

        // Add the Coordinator to the RecoveryManager map.  This is
        // to allow the recovery of the CoordinatorResource reference,
        // which results in a call to getCoordinator.

        DelegatedRecoveryManager.addCoordinator(globalTID, localTID, coord, 0, logPath);

        // Get all objects that were logged.  If this SuperiorInfo represents
        // a root TopCoordinator object, then there will be no objects logged.

        java.lang.Object[] logObjects = log.getObjects(logSection);

        try {
            if (logObjects.length > 1) {
                if (((org.omg.CORBA.Object) logObjects[0]).
                        _is_a(RecoveryCoordinatorHelper.id())) {
                    // TN - used to be com.sun.CORBA.iiop.CORBAObjectImpl
                    java.lang.Object rcimpl = logObjects[0];
                        
                    /*
                    String[] ids = StubAdapter.getTypeIds(rcimpl);
                    for (int i = 0; i < ids.length; i++)
                        if( trc != null )
                            trc.exit(998).data(i).data(ids[i]).write();
                    */

                    // TN - used to be com.sun.CORBA.iiop.CORBAObjectImpl
                    java.lang.Object crimpl = logObjects[1];
                        
                    /*
                    ids = StubAdapter.getTypeIds(crimpl);
                    for( int i = 0; i < ids.length; i++ )
                        if( trc != null )
                            trc.exit(998).data(i).data(ids[i]).write();
                    */
                    recovery = RecoveryCoordinatorHelper.
                                narrow((org.omg.CORBA.Object) logObjects[0]);

                    resource = SubtransactionAwareResourceHelper.
                                narrow((org.omg.CORBA.Object) logObjects[1]);
                } else {
                    recovery = RecoveryCoordinatorHelper.
                                narrow((org.omg.CORBA.Object) logObjects[1]);
                    resource = SubtransactionAwareResourceHelper.
                                narrow((org.omg.CORBA.Object) logObjects[0]);
                }
            } else {
                recovery = null;
                resource = null;
            }
        } catch (Throwable exc) {}
    }


    /**
     * Records the RecoveryCoordinator for the transaction.
     *
     * @param rec  The RecoveryCoordinator from the superior.
     *
     * @return
     *
     * @see
     */
    void setRecovery(RecoveryCoordinator rec) {

        if (recovery == null) {
            recovery = rec;

            // Add the RecoveryCoordinator to the CoordinatorLog object

            if (logRecord != null) {
                logRecord.addObject(logSection, rec);
            }
        }
    }

    /**
     * Records the CoordinatorResource for the transaction.
     *
     * @param res  The CoordinatorResource registered with the superior.
     *
     * @return
     *
     * @see
     */
    void setResource(SubtransactionAwareResource res) {

        if (resource == null) {
            resource = res;

            // Add the CoordinatorResource to the CoordinatorLog object

            if (logRecord != null) {
                logRecord.addObject(logSection, res);
            }
        }
    }

    /**
     * Returns the number of retries so far, and increments the count.
     *
     * @param
     *
     * @return  The number of retries so far.
     *
     * @see
     */
    int resyncRetries() {

        int result = resyncRetries++;

        return result;
    }
}

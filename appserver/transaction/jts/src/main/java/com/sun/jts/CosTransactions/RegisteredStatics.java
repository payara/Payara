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
// Module:      RegisteredStatics.java
//
// Description: Static Resource management.
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

/**
 * The RegisteredStatics class provides operations that manage the set of
 * StaticResource objects and distributes association operations to those
 * registered objects.
 *
 * @version 0.01
 *
 * @author Simon Holdsworth, IBM Corporation
 *
 * @see
 */

//---------------------------------------------------------------------------
// CHANGE HISTORY
//
// Version By     Change Description
//   0.01  SAJH   Initial implementation.
//---------------------------------------------------------------------------

class RegisteredStatics {

    private Vector registered = new Vector();

    /**
     * Default RegisteredStatics constructor.
     *
     * @param
     *
     * @return
     *
     * @see
     */
    RegisteredStatics() {}

    /**
     * Informs all registered objects an association has started.
     * <p>
     * The Control object which represents the transaction is given.
     * <p>
     * A flag is passed indicating whether this association
     * is as a result of a Current.begin operation.
     *
     * @param control The transaction whose association has started.
     * @param begin   Indicates if this is a begin rather than a resume.
     *
     * @return
     *
     * @see
     */
    void distributeStart(ControlImpl control, boolean begin) {

        // Determine the Coordinator for the transaction.

        org.omg.CosTransactions.Coordinator coord = null;

        try {
            coord = control.get_coordinator();
        } catch (Unavailable exc) {}

        // Browse through the set, telling each that association is starting.

        if (coord != null) {

            for (int i = 0; i < registered.size(); i++) {

                StaticResource resource =
                    (StaticResource) registered.elementAt(i);

                try {
                    resource.startAssociation(coord, begin);
                } catch (INVALID_TRANSACTION exc) {
                    // Catch INVALID_TRANSACTION exception, and allow it to
                    // percolate. We need to inform all previously called
                    // StaticResources that the association has ended
                    // immediately.

                    for (int j = i - 1; j >= 0; j--) {
                        ((StaticResource) registered.elementAt(j)).
                            endAssociation(coord, begin);
                    }

                    throw (INVALID_TRANSACTION)exc.fillInStackTrace();
                } catch (Throwable exc) {
                    // discard any other exception
                }
            }
        }
    }

    /**
     * Informs all registered StaticResource objects that a thread association
     * has ended.
     * <p>
     * The Control object representing the transaction is given.
     * <p>
     * A flag is passed indicating whether this association
     * is as a result of the transaction completing.
     *
     * @param control   The transaction whose association has ended.
     * @param complete  Indicates that this is a commit/rollback rather than a
     *                  suspend.
     *
     * @return
     *
     * @see
     */
    void distributeEnd(ControlImpl control, boolean complete) {

        // Determine the Coordinator for the transaction.

        org.omg.CosTransactions.Coordinator coord = null;

        try {
            coord = control.get_coordinator();
        } catch (Unavailable exc) {}

        // Browse through the set, telling each that the association is ending.

        if (coord != null) {
            for (int i = 0; i < registered.size(); i++) {
                StaticResource resource =
                    (StaticResource)registered.elementAt(i);
                try {
                    resource.endAssociation(coord, complete);
                } catch (Throwable e) {
                    // Discard any exception.
                }
            }
        }
    }

    /**
     * Adds the given StaticResource object to the set of those informed of
     * thread association changes.
     * <p>
     * If there is a current thread association, then the added StaticResource
     * is called immediately so that it is aware of the association.
     *
     * @param obj  The StaticResource to be added.
     *
     * @return
     *
     * @see
     */

    void addStatic(StaticResource obj) {

        registered.addElement(obj);

        // Determine whether there is a current association.

        try {
            org.omg.CosTransactions.Coordinator coord =
                CurrentTransaction.getCurrentCoordinator();

            // Tell the StaticResource that the association has started.
            // Pretend that it is a begin association, as the
            // StaticResource has not seen the transaction before.

            if (coord != null) {
                obj.startAssociation(coord, true);
            }
        } catch(Throwable exc) {
            // Discard any exception.
        }
    }

    /**
     * Removes the given StaticResource object from the set of those
     * informed of thread association changes.
     *
     * @param obj  The StaticResource to be removed.
     *
     * @return  Indicates success of the operation.
     *
     * @see
     */
    boolean removeStatic(StaticResource obj) {

        boolean result = registered.removeElement(obj);
        return result;
    }
}

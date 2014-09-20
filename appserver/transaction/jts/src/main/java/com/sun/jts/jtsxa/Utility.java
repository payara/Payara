/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jts.jtsxa;

import java.io.*;
import java.util.*;

import org.omg.CosTransactions.*;
import com.sun.jts.codegen.otsidl.*;
import com.sun.jts.CosTransactions.*;

/**
 * This is an Utility class containing helper functions.
 */
public class Utility {

    // static variables

    private static org.omg.CosTransactions.Current current = null;

    /*
     * All Utility methods are static.
     * It is not possible to create a JTSXA instance variable.
     */
    private Utility() {}

    /**
     * Obtain the current Control object.
     *
     * @return the current control object, or null if the Control cannot be obtained.
     * @see org.omg.CosTransactions.Control
     */
    public static Control getControl() {
        Control control = null;

        try {
            if (current == null) {
                current = (org.omg.CosTransactions.Current) Configuration.
                    getORB().resolve_initial_references("TransactionCurrent"/*#Frozen*/);
            }
            control = current.get_control();
        } catch(Exception e) {
            // empty
        }

        return control;
    }

    /**
     * Obtain the coordinator object from the supplied control.
     * <p>If a null control is supplied, an null coordinator will be returned.
     *
     * @param control the control object for which the coordinator
     *        will be returned
     *
     * @return the coordinator, or null if no coordinator can be obtained.
     *
     * @see org.omg.CosTransactions.Control
     * @see org.omg.CosTransactions.Coordinator
     */
    public static Coordinator getCoordinator(Control control) {
        Coordinator coordinator = null;

        if (control == null) {
            return null;
        }

        try {
            coordinator = control.get_coordinator();
        } catch(Exception e) {
            coordinator = null;
        }

        return coordinator;
    }

    /**
     * Obtain the global transaction identifier for the supplied coordinator.
     *
     * @param coordinator the coordinator representing the transaction for which
     *                    the global transaction identifier is required
     *
     * @return the global transaction identifier.
     *
     * @see com.sun.jts.jtsxa.XID
     */
    public static XID getXID(Coordinator coordinator) {
        otid_t tid = null;
        XID xid = new XID();

        if (coordinator == null) {
            return null;
        }

        try {
            tid = JCoordinatorHelper.narrow(coordinator).getGlobalTID();
            xid.copy(tid);
        } catch(Exception e) {
            return null;
        }

        return xid;
    }

    /**
     * Obtain the global transaction identifier for the current transaction.
     *
     * @return the global transaction identifier.
     *
     * @see com.sun.jts.jtsxa.XID
     */
    public static XID getXID() {
        Control control = null;
        Coordinator coordinator = null;

        control = getControl();
        coordinator = getCoordinator(control);

        XID xid = getXID(coordinator);

        return xid;
    }
}

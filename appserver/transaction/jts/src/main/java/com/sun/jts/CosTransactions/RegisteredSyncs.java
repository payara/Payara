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
// Module:      RegisteredSyncs.java
//
// Description: Synchronization participant management.
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

import com.sun.jts.trace.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import com.sun.logging.LogDomains;
import com.sun.jts.utils.LogFormatter;

/**
 * The RegisteredSyncs class provides operations that manage a set of
 * Synchronization objects involved in a transaction. In order to avoid
 * sending multiple synchronization requests to the same resource we require
 * some way to perform Synchronization reference comparisons.
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

class RegisteredSyncs {

    private Vector registered = new Vector();

	/*
		Logger to log transaction messages
	*/  
    static Logger _logger = LogDomains.getLogger(RegisteredSyncs.class, LogDomains.TRANSACTION_LOGGER);

    /**
     * Default RegisteredSyncs constructor.
     *
     * @param
     *
     * @return
     *
     * @see
     */
    RegisteredSyncs() {}

    /**
     * Distributes before completion operations to all registered
     * Synchronization objects.
     * <p>
     * Returns a boolean to indicate success/failure.
     *
     * @param
     *
     * @return  Indicates success of the operation.
     *
     * @see
     */
    boolean distributeBefore() {

        boolean result = true;

        for (int i = 0; i < registered.size() && result == true; i++) {
            Synchronization sync = (Synchronization) registered.elementAt(i);
            try {
		 		if(_logger.isLoggable(Level.FINEST))
                {
					_logger.logp(Level.FINEST,"RegisterdSyncs","distributeBefore()",
							"Before invoking before_completion() on synchronization object " + sync);
                }

                sync.before_completion();

		 		if(_logger.isLoggable(Level.FINEST))
                {
					_logger.logp(Level.FINEST,"RegisterdSyncs","distributeBefore()", 
					 		 "After invoking before_completion() on synchronization object " + sync);
                }
            } catch (RuntimeException rex) {
                // Exception was logged in SynchronizationImpl
                throw rex;
            } catch (Throwable exc) {
				_logger.log(Level.WARNING, "jts.exception_in_synchronization_operation",
		                new java.lang.Object[] { exc.toString(),"before_completion"});
                result = false;
            }
        }

        return result;
    }

    /**
     * Distributes after completion operations to all registered
     * Synchronization objects.
     *
     * @param status  Indicates whether the transaction committed.
     *
     * @return
     *
     * @see
     */
    void distributeAfter(Status status) {

        for (int i = 0; i < registered.size(); i++) {
            boolean isProxy = false;
            Synchronization sync = (Synchronization) registered.elementAt(i);

            // COMMENT(Ram J) the instanceof operation should be replaced
            // by a is_local() call, once the local object contract is
            // implemented.
            if (!(sync instanceof com.sun.jts.jta.SynchronizationImpl)) {
                isProxy = Configuration.getProxyChecker().isProxy(sync);
            }

            try {
 				if(_logger.isLoggable(Level.FINEST))
                {
					_logger.logp(Level.FINEST,"RegisterdSyncs","distributeAfter()",
							"Before invoking after_completion() on synchronization object " + sync);
                }

                sync.after_completion(status);

		 		if(_logger.isLoggable(Level.FINEST))
                {
					_logger.logp(Level.FINEST,"RegisterdSyncs","distributeAfter()",
							"After invoking after_completion() on"+
							"synchronization object"+ sync);
                }
            } catch (Throwable exc) {
                // Discard any exceptions at this point.
                if (exc instanceof OBJECT_NOT_EXIST ||
                        exc instanceof COMM_FAILURE) {
                    // ignore i.e., no need to log this error (Ram J)
                    // this can happen normally during after_completion flow,
                    // since remote sync objects would go away when the
                    // subordinate cleans up (i.e, the subordinate would have
                    // called afterCompletions locally before going away).
                } else {
					_logger.log(Level.WARNING,
							"jts.exception_in_synchronization_operation",
	                        new java.lang.Object[] { exc.toString(),
							"after_completion"});
                }
            }

            // Release the object if it is a proxy.
            if (isProxy) {
                sync._release();
            }
        }
    }

    /**
     * Adds a reference to a Synchronization object to the set.
     * <p>
     * If there is no such set then a new one is created with the single
     * Synchronization reference.
     *
     * @param obj  The Synchronization object to be added.
     *
     * @return
     *
     * @see
     */
    void addSync(Synchronization obj) {
        registered.addElement(obj);
    }

    /**
     * Empties the set of registered Synchronization objects.
     *
     * @param
     *
     * @return
     *
     * @see
     */
    void empty() {
        registered.removeAllElements();
    }

    /**
     * Checks whether there are any Synchronization objects registered.
     * <p>
     * If there are, the operation returns true, otherwise false.
     *
     * @param
     *
     * @return  Indicates whether any objects are registered.
     *
     * @see
     */
    boolean involved() {

        boolean result = (registered.size() != 0);
        return result;
    }
}

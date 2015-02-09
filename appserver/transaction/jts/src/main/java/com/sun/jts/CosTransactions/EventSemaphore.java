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
// Module:      EventSemaphore.java
//
// Description: Event semaphore implementation.
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

/**The EventSemaphore interface provides operations that wait for and post an
 * event semaphore.
 * <p>
 * This is specifically to handle the situation where the event may have been
 * posted before the wait method is called.  This behaviour is not supported by
 * the existing wait and notify methods.
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

public class EventSemaphore {
    boolean posted = false;

    /**Default EventSemaphore constructor.
     *
     * @param
     *
     * @return
     *
     * @see
     */
    EventSemaphore() {
    }

    /**Creates the event semaphore in the given posted state.
     *
     * @param posted  Indicates whether the semaphore should be posted.
     *
     * @return
     *
     * @see
     */
    EventSemaphore( boolean posted ) {
        this.posted = posted;
    }

    /**
     * @return true if semaphore has already been posted.
     */
    synchronized public boolean isPosted() {
        return posted;
    }

    /**Waits for the event to be posted.
     * <p>
     * If the event has already been posted, then the operation returns immediately.
     *
     * @param
     *
     * @return
     *
     * @exception InterruptedException  The wait was interrupted.
     *
     * @see
     */
    synchronized public void waitEvent()
            throws InterruptedException {
        if( !posted )
            wait();
    }

   /*Waits for the event to be posted. Release the thread waiting after the CMT
     * Timeout period if no event has been posted during this timeout interval.
     * <p>
     * If the event has already been posted, then the operation returns immediately.
     *
     * @param cmtTimeout - container managed transaction timeout
     *
     * @return
     *
     * @exception InterruptedException  The wait was interrupted.
     *
     * @see
     */

    synchronized public void waitTimeoutEvent(int cmtTimeout)
            throws InterruptedException {

        if (!posted) {
            long timeout = (System.currentTimeMillis() / 1000) + cmtTimeout;
            while (!posted && timeout - (System.currentTimeMillis() / 1000) > 0) {
                wait(timeout - (System.currentTimeMillis() / 1000));
            }
        }
    }

    /**Posts the event semaphore.
     * <p>
     * All waiters are notified.
     *
     * @param
     *
     * @return
     *
     * @see
     */
    synchronized void post() {
        if( !posted )
            notifyAll();
        posted = true;
    }

    /**Clears a posted event semaphore.
     *
     * @param
     *
     * @return
     *
     * @see
     */
    synchronized void clear() {
        posted = false;
    }
}
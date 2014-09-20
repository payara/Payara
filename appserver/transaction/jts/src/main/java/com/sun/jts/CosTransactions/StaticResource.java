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

//----------------------------------------------------------------------------
//
// Module:      StaticResource.java
//
// Description: Statically-registered Resource interface.
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

import org.omg.CosTransactions.*;

/**
 * The StaticResource interface provides operations that allow an object to
 * be informed about changes in transaction associations with threads. The
 * operations are guaranteed to be invoked on the thread on which the
 * association is started or ended. This class is an abstract base class so
 * the behavior here is that which is expected from any subclass.
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

public abstract class StaticResource {

    /**
     * Informs the object that an association has started.
     * <p>
     * That is, a thread association has begun on the calling thread for the
     * transaction represented by the given Coordinator object.
     * A flag is passed indicating whether this association is
     * as a result of a begin operation.
     *
     * @param coord  The transaction whose association is starting.
     * @param begin  Indicates a begin rather than a resume.
     *
     * @return
     *
     * @see
     */
    public abstract void startAssociation(Coordinator coord, boolean begin);

    /**
     * Informs the object that an association has ended.
     * <p>
     * That is, a thread association has ended on the calling thread for the
     * transaction represented by the given Coordinator object.
     * A flag is passed indicating whether this
     * association is as a result of the transaction completing.
     *
     * @param coord     The transaction whose association is starting.
     * @param complete  Indicates a commit/rollback rather than a suspend.
     *
     * @return
     *
     * @see
     */
    public abstract void endAssociation(Coordinator coord, boolean complete);

    /**
     * Registers the StaticResource object.
     * <p>
     * Until this method is called, the StaticResource object will not receive
     * calls to start/endAssociation.
     *
     * @param
     *
     * @return
     *
     * @see
     */
    protected void register() {
        CurrentTransaction.registerStatic(this);
    }
}

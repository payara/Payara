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
// Module:      LogRecordEnding.java
//
// Description: Log record ending.
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

// Import required classes

import java.io.*;

/**A class containing ending information for a log record.
 *
 * @version 0.01
 *
 * @author Simon Holdsworth, IBM Corporation
 *
 * @see LogHandle
*/
//----------------------------------------------------------------------------
// CHANGE HISTORY
//
// Version By     Change Description
//   0.01  SAJH   Initial implementation.
//-----------------------------------------------------------------------------

class LogRecordEnding implements Serializable {
    /**This constant holds the size of the LogRecordEnding object.
     */
    static final int SIZEOF = LogLSN.SIZEOF;

    /**The log record ending contains the current LSN.
     */
    LogLSN currentLSN = null;

    /**Default LogRecordEnding constructor.
     *
     * @param
     *
     * @return
     *
     * @see
     */
    LogRecordEnding() {
    }

    /**Constructs a LogReocrdEnding from the given byte array.
     *
     * @param bytes The array of bytes from which the object is to be constructed.
     * @param index The index in the array where copy is to start.
     *
     * @return
     *
     * @see
     */
    LogRecordEnding( byte[] bytes,
                     int  index ) {
        currentLSN = new LogLSN(bytes,index);
    }

    /**Makes a byte representation of the LogRecordEnding.
     *
     * @param bytes The array of bytes into which the object is to be copied.
     * @param index The index in the array where copy is to start.
     *
     * @return  Number of bytes copied.
     *
     * @see
     */
    final int toBytes( byte[] bytes,
                       int  index ) {
        currentLSN.toBytes(bytes,index);

        return SIZEOF;
    }

    /**This method is called to direct the object to format its state to a String.
     *
     * @param
     *
     * @return  The formatted representation of the object.
     *
     * @see
     */
    @Override
    public final String toString() {
        return "LRE(curr="/*#Frozen*/+currentLSN+")"/*#Frozen*/;
    }
}

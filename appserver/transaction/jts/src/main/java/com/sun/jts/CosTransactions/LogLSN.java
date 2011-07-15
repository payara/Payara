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
// Module:      LogLSN.java
//
// Description: Log sequence number.
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

/**A structure containing 2 unsigned integers.
 * extent: the extent file number
 * offset: the offset within the extent file
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

class LogLSN implements Serializable {

    /**Constants for particular LSN values.
     */
    static final LogLSN HEAD_LSN  = new LogLSN(0xFFFFFFFF, 0xFFFFFFFF);
    static final LogLSN TAIL_LSN  = new LogLSN(0xFFFFFFFF, 0xFFFFFFFE);
    static final LogLSN NULL_LSN  = new LogLSN(0x00000000, 0x00000000);
    static final LogLSN FIRST_LSN = new LogLSN(0x00000001, 0x00000000);

    /**This constant holds the size of the LogRecordEnding object.
     */
    static final int SIZEOF = 8;

    /**Internal instance members.
     */
    int offset = 0;
    int extent = 0;

    /**Default LogLSN constructor
     *
     * @param
     *
     * @return
     *
     * @see
     */
    LogLSN() {
        offset = 0;
        extent = 0;
    }

    /**LogLSN constructor
     *
     * @param ext Extent for new LSN.
     * @param off Offset for new LSN.
     *
     * @return
     *
     * @see
     */
    LogLSN( int ext,
            int off ) {
        offset = off;
        extent = ext;
    }

    /**LogLSN constructor
     *
     * @param lsn Other LSN to be copied.
     *
     * @return
     *
     * @see
     */
    LogLSN( LogLSN lsn ) {
        offset = lsn.offset;
        extent = lsn.extent;
    }

    /**Constructs a LogLSN from the given byte array.
     *
     * @param bytes The array of bytes from which the LogLSN is to be constructed.
     * @param index The index in the array where copy is to start.
     *
     * @return
     *
     * @see
     */
    LogLSN( byte[] bytes,
            int  index ) {
        offset =  (bytes[index++]&255) +
            ((bytes[index++]&255) << 8) +
            ((bytes[index++]&255) << 16) +
            ((bytes[index++]&255) << 24);
        extent =  (bytes[index++]&255) +
            ((bytes[index++]&255) << 8) +
            ((bytes[index++]&255) << 16) +
            ((bytes[index++]&255) << 24);
    }

    /**Determines whether the target LSN is NULL.
     *
     * @param
     *
     * @return
     *
     * @see
     */
    final boolean isNULL() {
        return offset == 0 && extent == 0;
    }

    /**Determines whether the given LSN is equal to the target.
     *
     * @param other The other LogLSN to be compared.
     *
     * @return
     *
     * @see
     */
    final boolean equals( LogLSN other ) {
        return offset == other.offset && extent == other.extent;
    }

    /**Determines whether the target LSN is less than the parameter.
     *
     * @param other The other LogLSN to be compared.
     *
     * @return
     *
     * @see
     */
    final boolean lessThan( LogLSN other ) {
        return ( (offset < other.offset && extent == other.extent) ||
                 extent < other.extent);
    }

    /**Determines whether the target LSN is greater than the parameter.
     *
     * @param other The other LogLSN to be compared.
     *
     * @return
     *
     * @see
     */
    final boolean greaterThan( LogLSN other ) {
        return ( (offset > other.offset && extent == other.extent) ||
                 extent > other.extent);
    }

    /**makes the target LSN a copy of the parameter.
     *
     * @param LogLSN  The LSN to be copied.
     *
     * @return
     *
     * @see
     */
    final void copy( LogLSN other ) {
        extent = other.extent;
        offset = other.offset;
    }

    /**Makes a byte representation of the LogLSN.
     *
     * @param bytes The array of bytes into which the LogLSN is to be copied.
     * @param index The index in the array where copy is to start.
     *
     * @return  Number of bytes copied.
     *
     * @see
     */
    final int toBytes( byte[] bytes,
                       int  index ) {
        bytes[index++] = (byte) offset;
        bytes[index++] = (byte)(offset >> 8);
        bytes[index++] = (byte)(offset >> 16);
        bytes[index++] = (byte)(offset >> 24);
        bytes[index++] = (byte) extent;
        bytes[index++] = (byte)(extent >> 8);
        bytes[index++] = (byte)(extent >> 16);
        bytes[index++] = (byte)(extent >> 24);

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
    public final String toString() {
        return "LSN(ext="/*#Frozen*/+extent+",off="/*#Frozen*/+offset+")"/*#Frozen*/;
    }
}

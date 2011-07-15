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
// Module:      LogRecordHeader.java
//
// Description: Log record header.
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

/**
 * A class containing header information for a log record.
 *
 * @version 0.01
 *
 * @author Simon Holdsworth, IBM Corporation
 *
 * @see LogHandle
 */
class LogRecordHeader implements Serializable {

    /**
     * This constant holds the size of the LogRecordHeader object.
     */
    final static int SIZEOF = 3 * LogLSN.SIZEOF + 8;

    int    recordType   = 0;
    LogLSN currentLSN   = null;
    LogLSN previousLSN  = null;
    LogLSN nextLSN      = null;
    int    recordLength = 0;

    LogRecordHeader() {
        currentLSN = new LogLSN();
        previousLSN = new LogLSN();
        nextLSN = new LogLSN();
    }

    /**
     * Constructs a LogRecordHeader from the given byte array.
     *
     * @param bytes The array of bytes from which the object is to
     *              be constructed.
     * @param index The index in the array where copy is to start.
     *
     * @return
     *
     * @see
     */
    LogRecordHeader(byte[] bytes, int index) {
        recordType = (bytes[index++]&255) +
                     ((bytes[index++]&255) << 8) +
                     ((bytes[index++]&255) << 16) +
                     ((bytes[index++]&255) << 24);

        currentLSN  = new LogLSN(bytes,index);  index += LogLSN.SIZEOF;
        previousLSN = new LogLSN(bytes,index);  index += LogLSN.SIZEOF;
        nextLSN     = new LogLSN(bytes,index);  index += LogLSN.SIZEOF;

        recordLength = (bytes[index++]&255) +
                       ((bytes[index++]&255) << 8) +
                       ((bytes[index++]&255) << 16) +
                       ((bytes[index++]&255) << 24);
    }

    /**
     * Makes the target object a copy of the parameter.
     *
     * @param other  The object to be copied.
     *
     * @return
     *
     * @see
     */
    void copy( LogRecordHeader other) {
        recordType  = other.recordType;
        currentLSN.copy(other.currentLSN);
        previousLSN.copy(other.previousLSN);
        nextLSN.copy(other.nextLSN);
        recordLength = other.recordLength;
    }

    /**
     * Makes a byte representation of the LogRecordHeader.
     *
     * @param bytes The array of bytes into which the object is to be copied.
     * @param index The index in the array where copy is to start.
     *
     * @return  Number of bytes copied.
     *
     * @see
     */
    final int toBytes(byte[] bytes, int  index) {
        bytes[index++] = (byte) recordType;
        bytes[index++] = (byte)(recordType >> 8);
        bytes[index++] = (byte)(recordType >> 16);
        bytes[index++] = (byte)(recordType >> 24);
        index += currentLSN.toBytes(bytes,index);
        index += previousLSN.toBytes(bytes,index);
        index += nextLSN.toBytes(bytes,index);
        bytes[index++] = (byte) recordLength;
        bytes[index++] = (byte)(recordLength >> 8);
        bytes[index++] = (byte)(recordLength >> 16);
        bytes[index++] = (byte)(recordLength >> 24);

        return SIZEOF;
    }

    /**
     * This method is called to direct the object to format its state
     * to a String.
     *
     * @param
     *
     * @return  The formatted representation of the object.
     *
     * @see
     */
    public final String toString() {
        return "LRH(type="/*#Frozen*/ + recordType + ",curr="/*#Frozen*/ + currentLSN +
               ",prev="/*#Frozen*/ + previousLSN + ",next="/*#Frozen*/ + nextLSN +
               ",len="/*#Frozen*/ + recordLength + ")"/*#Frozen*/;
    }
}

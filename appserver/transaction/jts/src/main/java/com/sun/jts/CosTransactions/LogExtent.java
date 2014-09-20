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
// Module:      LogExtent.java
//
// Description: Log extent file interface.
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

// Import required definitions.

import java.io.*;

//------------------------------------------------------------------------------
// LogExtent class
//------------------------------------------------------------------------------
/**A structure containing information for an open log file extent.
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

class LogExtent extends Object {

    // The type of access last made to an extent file is stored in the extent
    // descriptor block. This is used to save doing uneccessary fseeks.

    /**Type of last access is unknown (forces fseek to required cursor pos'n)
     */
    final static int ACCESSTYPE_UNKNOWN = 0;

    /**Last access was for reading
     */
    final static int ACCESSTYPE_READ = 1;

    /**Last access was for writing
     */
    final static int ACCESSTYPE_WRITE = 2;

    /**The radix used to convert extent numbers to strings.
     */
    final static int EXTENT_RADIX      = 36;

    /**The maximum number of extent files that can be allocated to a single
     * log at any one time. Extent names are made up of <logfilename>.nnn
     * Hence this value is restricted by the .nnn extension (3 characters
     * only, to support the FAT file system.
     */
    final static int MAX_NO_OF_EXTENTS = EXTENT_RADIX*EXTENT_RADIX*EXTENT_RADIX;

    /**This value is used to validate the LogExtent object.
     */
    LogExtent blockValid = null;

    /**The extent number.
     */
    int extentNumber = -1;

    /**The file handle for the log extent file.
     */
    LogFileHandle  fileHandle = null;

    /**The file for the log extent file.
     */
    File file = null;

    /**Indicates whether any information has been written since the last force.
     */
    boolean writtenSinceLastForce = false;

    /**The cursor position in the log extent.
     */
    int cursorPosition = 0;

    /**The last type of access to the extent.
     */
    int lastAccess = ACCESSTYPE_UNKNOWN;

    /**LogExtent constructor
     *
     * @param extent   The number of the extent.
     * @param extentFH The handle of the extent file.
     *
     * @return
     *
     * @see
     */
    LogExtent( int           extent,
               LogFileHandle extentFH,
               File          extentFile ) {
        extentNumber = extent;
        fileHandle = extentFH;
        file = extentFile;
    }

    /**Default LogExtent destructor.
     *
     * @param
     *
     * @return
     *
     * @see
     */
    public void doFinalize() {
        try {
            fileHandle.destroy(); 
        } catch( Throwable e ) {};

        blockValid = null;
        file = null;
    }

    /**Modulates the extent number using the maximum extent number.
     *
     * @param ext  The extent number
     *
     * @return  The modulated extent number.
     *
     * @see
     */
    final static int modExtent( int ext ) {
        return (ext % MAX_NO_OF_EXTENTS);
    }
}

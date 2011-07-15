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
// Module:      LogException.java
//
// Description: Log exception.
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

import com.sun.enterprise.util.i18n.StringManager;

/**A class which contains exception information for errors in the log.
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
//------------------------------------------------------------------------------

class LogException extends Exception {
    private static final StringManager sm = StringManager.getManager(LogException.class);

    /**Constants which define error codes from the logger classes.
     */
    static final int LOG_SUCCESS = 0;
    static final int LOG_NOT_INITIALISED = 1;
    static final int LOG_OPEN_FAILURE = 2;
    static final int LOG_READ_FAILURE = 3;
    static final int LOG_CORRUPTED = 4;
    static final int LOG_INVALID_FILE_DESCRIPTOR = 5;
    static final int LOG_LOCK_FAILURE = 6;
    static final int LOG_WRITE_FAILURE = 7;
    static final int LOG_CLOSE_FAILURE = 8;
    static final int LOG_TOO_MANY_INPUT_BUFFERS = 9;
    static final int LOG_RECORD_TOO_LARGE = 10;
    static final int LOG_NO_SPACE = 11;
    static final int LOG_INSUFFICIENT_MEMORY = 12;
    static final int LOG_ERROR_FORCING_LOG = 13;
    static final int LOG_INVALID_LSN = 14;
    static final int LOG_NEW_TAIL_TOO_HIGH = 15;
    static final int LOG_NEW_TAIL_TOO_LOW = 16;
    static final int LOG_INVALID_TAIL = 17;
    static final int LOG_INTERNAL_ERROR = 18;
    static final int LOG_NO_RESTART_RECORD = 19;
    static final int LOG_INVALID_CURSOR = 20;
    static final int LOG_END_OF_CURSOR = 21;
    static final int LOG_ACCESS_FAILURE = 22;
    static final int LOG_INVALID_PROCESS = 23;
    static final int LOG_INVALID_RECORDTYPE = 24;
    static final int LOG_INVALID_WRITEMODE = 25;
    static final int LOG_OPEN_EXTENT_FAILURE = 26;
    static final int LOG_READ_ONLY_ACCESS = 27;
    static final int MAX_RESPONSE_VALUE = LOG_READ_ONLY_ACCESS;

    /**Strings which contain error messages from the log.
     */
    private static final String[] statusStrings = 
    { "jts.LOG_000_Operation_successful"/*#Frozen*/,
      "jts.LOG_001_Log_not_initialised"/*#Frozen*/,
      "jts.LOG_002_Open_failure"/*#Frozen*/,
      "jts.LOG_003_Read_failure"/*#Frozen*/,
      "jts.LOG_004_Data_corrupted"/*#Frozen*/,
      "jts.LOG_005_Invalid_file_descriptor"/*#Frozen*/,
      "jts.LOG_006_Lock_failure"/*#Frozen*/,
      "jts.LOG_007_Write_failure"/*#Frozen*/,
      "jts.LOG_008_Close_failure"/*#Frozen*/,
      "jts.LOG_009_Too_many_input_buffers"/*#Frozen*/,
      "jts.LOG_010_Record_too_large"/*#Frozen*/,
      "jts.LOG_011_No_space_in_filesystem"/*#Frozen*/,
      "jts.LOG_012_Insufficient_memory"/*#Frozen*/,
      "jts.LOG_013_Force_failure"/*#Frozen*/,
      "jts.LOG_014_Invalid_LSN_value"/*#Frozen*/,
      "jts.LOG_015_New_tail_LSN_too_high"/*#Frozen*/,
      "jts.LOG_016_New_tail_LSN_too_low"/*#Frozen*/,
      "jts.LOG_017_Invalid_tail_LSN_value"/*#Frozen*/,
      "jts.LOG_018_Internal_error"/*#Frozen*/,
      "jts.LOG_019_No_restart_record_present"/*#Frozen*/,
      "jts.LOG_020_Invalid_cursor_value"/*#Frozen*/,
      "jts.LOG_021_End_of_cursor_reached"/*#Frozen*/,
      "jts.LOG_022_Filesystem_access_failure"/*#Frozen*/,
      "jts.LOG_023_Invalid_process"/*#Frozen*/,
      "jts.LOG_024_Log_is_read_only"/*#Frozen*/,
      "jts.LOG_025_Invalid_record_type_specified"/*#Frozen*/,
      "jts.LOG_026_Extent_file_open_failure"/*#Frozen*/,
      "jts.LOG_027_Invalid_write_mode_specified"/*#Frozen*/,
      "jts.LOG_028_Invalid_status_specified"/*#Frozen*/ };

    /**Instance members
     */
    int errorCode;
    private int throwPoint;
    private Object extraInfo;

    /**LogException constructor
     *
     * @param trc   The current trace object to allow exception trace to be made.
     * @param err   The error code.
     * @param point The throw point.
     *
     * @return
     *
     * @see
     */
    LogException(Object dummy /* COMMENT(Ram J) - used to be trace object */,
                  int   err,
                  int   point ) {
        super(getMessageFromErrorCode(null, err, point));
        errorCode = err;
        throwPoint = point;
    }

    /**LogException constructor
     *
     * @param trc    The current trace object to allow exception trace to be made.
     * @param err    The error code.
     * @param point  The throw point.
     * @param extra  Extra information.
     *
     * @return
     *
     * @see
     */
    LogException(Object dummy /* COMMENT(Ram J) - used to be trace object */,
                  int    err,
                  int    point,
                  Object extra ) {
        super(getMessageFromErrorCode(null, err, point));
        errorCode = err;
        throwPoint = point;
        extraInfo = extra;
    }

    LogException(int err, int point, String msg, Throwable cause) {
        super(getMessageFromErrorCode(msg, err, point), cause);
        this.errorCode = err;
        this.throwPoint = point;
    }

    private static String getMessageFromErrorCode(String extraMsg, int err, int point) {
        String key = statusStrings[err > MAX_RESPONSE_VALUE ? MAX_RESPONSE_VALUE + 1 : err];
        return sm.getString("jts.log_exception", sm.getString(key), point,
                 (extraMsg == null) ? "" : extraMsg);
    }
}

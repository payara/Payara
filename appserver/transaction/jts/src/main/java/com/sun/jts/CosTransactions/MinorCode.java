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
// Module:      MinorCode.java
//
// Description: JTS standard exception minor codes.
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

/**
 * This class simply contains minor code values for standard exceptions thrown
 * by the JTS.
 *
 * @version 0.01
 *
 * @author Simon Holdsworth, IBM Corporation
 *
 * @see
 */
public interface MinorCode  {

    /**
     * This minor code is used on standard exceptions.
     * <p> It indicates that there is no
     * further information for the exception.
     */
    public static int Undefined = 0x0000;

    /**
     * This minor code is used on the INVALID_TRANSACTION exception.
     * <p> It indicates that the transaction is invalid because
     * it has unfinished subtransactions.
     */
    public static int UnfinishedSubtransactions = 0x0001;

    /**
     * This minor code is used on the INVALID_TRANSACTION exception.
     * <p> It indicates
     * that the transaction is invalid because it has outstanding work
     * (other threads either in the same process or other processes
     * which are still associated with the transaction).
     */
    public static int DeferredActivities = 0x0002;

    /**
     * This minor code is used on the INVALID_TRANSACTION exception.
     * <p> It indicates
     * that the transaction has completed and the operation is not valid.
     */
    public static int Completed = 0x0003;

    /**
     * This minor code is used on the INVALID_TRANSACTION exception.
     * <p> It indicates
     * that the TransactionFactory was unable to create the transaction.
     */
    public static int FactoryFailed = 0x0004;

    /**
     * This minor code is used on the INVALID_TRANSACTION exception.
     * <p> It indicates
     * that an XA Resource Manager is doing work outside of a transaction
     * on the current thread and cannot allow the begin or resume operation.
     */
    public static int XAOutside = 0x0005;

    /**
     * This minor code is used on the INVALID_TRANSACTION exception.
     * <p> It indicates that a reply is returning when a different transaction
     * is active from the one active when the request was imported.
     */
    public static int WrongContextOnReply = 0x0006;

    /**
     * This minor code is used on the INVALID_TRANSACTION exception.
     * <p> It indicates
     * that the is_same_transaction operation has been invoked with a parameter
     * which represents a Coordinator object from a different implementation
     * of the OTS interfaces, and that Coordinator object is in the process
     * of ending the transaction.  In this case, the JTS cannot obtain the
     * necessary information to determine equality of the Coordinator objects.
     */
    public static int CompareFailed = 0x0007;

    /**
     * This minor code is used on the INTERNAL exception.
     * <p> It indicates
     * that the object could not locate the Coordinator for its transaction.
     */
    public static int NoCoordinator = 0x0101;

    /**
     * This minor code is used on the INTERNAL exception.
     * <p>It indicates
     * that the object did not have access to the global identifier for the
     * transaction which it represents.
     */
    public static int NoGlobalTID = 0x0102;

    /**
     * This minor code is used on the INTERNAL exception.
     * <p> It indicates that the object represents a subtransaction
     * and was called for a top-level transaction operation.
     */
    public static int TopForSub = 0x0103;

    /**
     * This minor code is used on the INTERNAL exception.
     * <p> It indicates
     * that the object represents a top-level transaction and was called for a
     * subtransaction operation.
     */
    public static int SubForTop = 0x0104;

    /**
     * This minor code is used on the INTERNAL exception.
     * <p> It indicates that a stacked Control object already exists
     * when beginning a subtransaction.
     */
    public static int AlreadyStacked = 0x0105;

    /**
     * This minor code is used on the INTERNAL exception.
     * <p> It indicates
     * that an internal logic error was detected.
     */
    public static int LogicError = 0x0106;

    /**
     * This minor code is used on the INTERNAL exception.
     * <p> It indicates
     * that a Resource could not be registered by a subordinate.
     */
    public static int NotRegistered = 0x0107;

    /**
     * This minor code is used on the INTERNAL exception.
     * <p> It indicates
     * that a RecoveryCoordinator could not be created.
     */
    public static int RecCoordCreateFailed = 0x0108;

    /**
     * This minor code is used on the INTERNAL exception.
     * <p> It indicates
     * that the TransactionService could not be created.
     */
    public static int TSCreateFailed = 0x0109;
	
    /**
     * This minor code is used on the INVALID_TRANSACTION exception.
     * <p> It indicates 
     * that recreating a imported transaction has failed.
     */
    public static int TX_RECREATE_FAILED = 0x010A;

    /**
     * This minor code is used on the INVALID_TRANSACTION exception.
     * <p> It indicates
     * that concurrent activity within a transaction is disallowed.
     */
    public static int TX_CONCURRENT_WORK_DISALLOWED = 0x010B;        
}

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

package javax.transaction.xa;

/**
 * The XAException is thrown by the Resource Manager (RM) to inform the 
 * Transaction Manager of an error encountered by the involved 
 * transaction.
 */
public class XAException extends java.lang.Exception {

    /**
    * The error code with which to create the SystemException.
    *
    * @serial The error code for the exception.
     */

    public int errorCode;

    /**
     * Create an XAException.
     */
    public XAException()
    {
    	super();
    }    
    
    /**
     * Create an XAException with a given string.
     *
     * @param s The <code>String</code> object containing the exception
     *		message.
     */
    public XAException(String s)
    {
    	super(s);
    }
    
    /**
     * Create an XAException with a given error code.
     *
     * @param errcode The error code identifying the exception.
     */
    public XAException(int errcode)
    {
    	super();
    	errorCode = errcode;
    }

    /**
     * The inclusive lower bound of the rollback codes.
     */
    public final static int XA_RBBASE = 100;

    /**
     * Indicates that the rollback was caused by an unspecified reason.
     */
    public final static int XA_RBROLLBACK = XA_RBBASE;

    /**
     * Indicates that the rollback was caused by a communication failure.
     */
    public final static int XA_RBCOMMFAIL = XA_RBBASE + 1;

    /**
     * A deadlock was detected.
     */
    public final static int XA_RBDEADLOCK = XA_RBBASE + 2;

    /**
     * A condition that violates the integrity of the resource was detected.
     */
    public final static int XA_RBINTEGRITY = XA_RBBASE + 3;

    /**
     * The resource manager rolled back the transaction branch for a reason
     * not on this list.
     */
    public final static int XA_RBOTHER = XA_RBBASE + 4;

    /**
     * A protocol error occurred in the resource manager.
     */
    public final static int XA_RBPROTO = XA_RBBASE + 5;

    /**
     * A transaction branch took too long.
     */
    public final static int XA_RBTIMEOUT = XA_RBBASE + 6;

    /**
     * May retry the transaction branch.
     */
    public final static int XA_RBTRANSIENT = XA_RBBASE + 7;

    /**
     * The inclusive upper bound of the rollback error code.
     */
    public final static int XA_RBEND = XA_RBTRANSIENT;

    /**
     * Resumption must occur where the suspension occurred.
     */
    public final static int XA_NOMIGRATE = 9;

    /**
     * The transaction branch may have been heuristically completed.
     */
    public final static int XA_HEURHAZ = 8;

    /**
     * The transaction branch has been heuristically committed.
     */
    public final static int XA_HEURCOM = 7;

    /**
     * The transaction branch has been heuristically rolled back.
     */
    public final static int XA_HEURRB = 6;

    /**
     * The transaction branch has been heuristically committed and 
     * rolled back.
     */
    public final static int XA_HEURMIX = 5;

    /**
     * Routine returned with no effect and may be reissued.
     */
    public final static int XA_RETRY = 4;

    /**
     * The transaction branch was read-only and has been committed.
     */
    public final static int XA_RDONLY = 3;

    /**
     * There is an asynchronous operation already outstanding.
     */
    public final static int XAER_ASYNC = -2;

    /**
     * A resource manager error has occurred in the transaction branch.
     */
    public final static int XAER_RMERR = -3;

    /**
     * The XID is not valid.
     */
    public final static int XAER_NOTA = -4;

    /**
     * Invalid arguments were given.
     */
    public final static int XAER_INVAL = -5;

    /**
     * Routine was invoked in an improper context.
     */
    public final static int XAER_PROTO = -6;

    /**
     * Resource manager is unavailable.
     */
    public final static int XAER_RMFAIL = -7;

    /**
     * The XID already exists.
     */
    public final static int XAER_DUPID = -8;

    /**
     * The resource manager is doing work outside a global transaction.
     */
    public final static int XAER_OUTSIDE = -9;

   
}

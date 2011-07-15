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

package javax.transaction;

/**
 * The Status interface defines static variables used for transaction 
 * status codes.
 */

public interface Status {
    /**
     * A transaction is associated with the target object and it is in the
     * active state. An implementation returns this status after a
     * transaction has been started and prior to a Coordinator issuing
     * any prepares, unless the transaction has been marked for rollback.
     */  
    public final static int STATUS_ACTIVE = 0;

    /**
     * A transaction is associated with the target object and it has been
     * marked for rollback, perhaps as a result of a setRollbackOnly operation.
     */  
    public final static int STATUS_MARKED_ROLLBACK = 1;

    /**
     * A transaction is associated with the target object and it has been
     * prepared. That is, all subordinates have agreed to commit. The
     * target object may be waiting for instructions from a superior as to how
     * to proceed.
     */  
    public final static int STATUS_PREPARED = 2;
 
    /**
     * A transaction is associated with the target object and it has been
     * committed. It is likely that heuristics exist; otherwise, the
     * transaction would have been destroyed and NoTransaction returned.
     */  
    public final static int STATUS_COMMITTED = 3;

    /**
     * A transaction is associated with the target object and the outcome
     * has been determined to be rollback. It is likely that heuristics exist;
     * otherwise, the transaction would have been destroyed and NoTransaction
     * returned.
     */  
    public final static int STATUS_ROLLEDBACK = 4;

    /**
     * A transaction is associated with the target object but its
     * current status cannot be determined. This is a transient condition
     * and a subsequent invocation will ultimately return a different status.
     */  
    public final static int STATUS_UNKNOWN = 5;

    /**
     * No transaction is currently associated with the target object. This
     * will occur after a transaction has completed.
     */  
    public final static int STATUS_NO_TRANSACTION = 6;

    /**
     * A transaction is associated with the target object and it is in the
     * process of preparing. An implementation returns this status if it
     * has started preparing, but has not yet completed the process. The
     * likely reason for this is that the implementation is probably
     * waiting for responses to prepare from one or more
     * Resources.
     */  
    public final static int STATUS_PREPARING = 7;

    /**
     * A transaction is associated with the target object and it is in the
     * process of committing. An implementation returns this status if it
     * has decided to commit but has not yet completed the committing process. 
     * This occurs because the implementation is probably waiting for 
     * responses from one or more Resources.
     */  
    public final static int STATUS_COMMITTING = 8;

    /**
     * A transaction is associated with the target object and it is in the
     * process of rolling back. An implementation returns this status if
     * it has decided to rollback but has not yet completed the process.
     * The implementation is probably waiting for responses from one or more
     * Resources.
     */  
    public final static int STATUS_ROLLING_BACK = 9;
}

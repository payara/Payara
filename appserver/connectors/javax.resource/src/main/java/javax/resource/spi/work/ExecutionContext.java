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

package javax.resource.spi.work;

import javax.resource.NotSupportedException;
import javax.transaction.xa.Xid;

/**
 * This class models an execution context (transaction, security, etc) 
 * with which the <code>Work</code> instance must be executed.  
 * This class is provided as a convenience for easily creating 
 * <code>ExecutionContext</code> instances by extending this class
 * and overriding only those methods of interest.
 *
 * <p>Some reasons why it is better for <code>ExecutionContext</code> 
 * to be a class rather than an interface: 
 * <ul><li>There is no need for a resource adapter to implement this class. 
 * It only needs to implement the context information like 
 * transaction, etc.
 * <li>The resource adapter code does not have to change when the 
 * <code>ExecutionContext</code> class evolves. For example, more context 
 * types could be added to the <code>ExecutionContext</code> class 
 * (in the future) without forcing resource adapter implementations 
 * to change.</ul>
 *
 * Note: Resource adapters that are developed for Connectors 1.6 specification
 * compliant application servers and above, are recommended to use
 * the <code>TransactionContext</code> interface instead of this 
 * class. See Chapter.11 Generic Work Context in the Connectors 1.6
 * specification for more details.
 *
 * @version 1.0
 * @author  Ram Jeyaraman
 */
public class ExecutionContext {

    /**
     * transaction context.
     */
    private Xid xid;

    /**
     * transaction timeout value.
     */
    private long transactionTimeout = WorkManager.UNKNOWN;


    /**
     * set a transaction context.
     *
     * @param xid transaction context.
     */
    public void setXid(Xid xid) { this.xid = xid; }

    /**
     * @return an Xid object carrying a transaction context, 
     * if any.
     */
    public Xid getXid() { return this.xid; }

    /**
     * Set the transaction timeout value for a imported transaction.
     *
     * @param timeout transaction timeout value in seconds. Only positive
     * non-zero values are accepted. Other values are illegal and are 
     * rejected with a <code>NotSupportedException</code>.
     *
     * @throws NotSupportedException thrown to indicate an illegal timeout 
     * value.
     */
    public void setTransactionTimeout(long timeout) 
	throws NotSupportedException {
	    if (timeout > 0) {
	        this.transactionTimeout = timeout;
	    } else {
	        throw new NotSupportedException("Illegal timeout value");
	    }
    }

    /** 
     * Get the transaction timeout value for a imported transaction.
     *
     * @return the specified transaction timeout value in seconds. When no
     * timeout value or an illegal timeout value had been specified, a value of
     * -1 (<code>WorkManager.UNKNOWN</code>) is returned; the timeout 
     * processing of such a transaction depends on the application server 
     * implementation.
     */
    public long getTransactionTimeout() {
	    return this.transactionTimeout;
    }
}

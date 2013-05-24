/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.cdi.transaction;

import javax.transaction.*;



public class TransactionManager implements javax.transaction.TransactionManager {
    ThreadLocal transactionThreadLocal = new ThreadLocal();

    public void begin() throws NotSupportedException, SystemException {
        if (getTransaction()!=null) throw new NotSupportedException("attempt to start tx when one already exists");
        transactionThreadLocal.set(new Transaction());
    }

    public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException,
            SecurityException, IllegalStateException, SystemException {
        if(((Transaction)getTransaction()).isMarkedRollback) {
            suspend();
            throw new RollbackException("test tx was marked for rollback");
        }
        suspend();
    }

    public int getStatus() throws SystemException {
        return 0;  
    }

    public javax.transaction.Transaction getTransaction() throws SystemException {
        return (javax.transaction.Transaction) transactionThreadLocal.get();
    }

    public void resume(javax.transaction.Transaction transaction) throws InvalidTransactionException, IllegalStateException, SystemException {
        transactionThreadLocal.set(transaction);
    }

    public void rollback() throws IllegalStateException, SecurityException, SystemException {
        suspend();
    }

    public void setRollbackOnly() throws IllegalStateException, SystemException {
        Transaction transaction = (Transaction) getTransaction();
        if(transaction!=null) transaction.isMarkedRollback = true;
    }

    public void setTransactionTimeout(int seconds) throws SystemException {
        
    }

    public javax.transaction.Transaction suspend() throws SystemException {
        javax.transaction.Transaction transaction = (javax.transaction.Transaction)transactionThreadLocal.get();
        transactionThreadLocal.set(null);
        return transaction;
    }
}

/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.weld.services;

import static javax.transaction.Status.STATUS_ACTIVE;
import static javax.transaction.Status.STATUS_COMMITTING;
import static javax.transaction.Status.STATUS_MARKED_ROLLBACK;
import static javax.transaction.Status.STATUS_PREPARED;
import static javax.transaction.Status.STATUS_PREPARING;
import static javax.transaction.Status.STATUS_ROLLING_BACK;
import static javax.transaction.Status.STATUS_UNKNOWN;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import org.glassfish.hk2.api.ServiceLocator;
import org.jboss.weld.transaction.spi.TransactionServices;

import com.sun.enterprise.transaction.api.JavaEETransactionManager;

public class TransactionServicesImpl implements TransactionServices {

    private JavaEETransactionManager transactionManager;

    public TransactionServicesImpl(ServiceLocator services) {
        transactionManager = services.getService(JavaEETransactionManager.class);
        if (transactionManager == null) {
            throw new RuntimeException("Unable to retrieve transaction mgr.");
        }
    }

    public boolean isTransactionActive() {
        try {
            switch (transactionManager.getStatus()) {
                case STATUS_ACTIVE:
                case STATUS_MARKED_ROLLBACK:
                case STATUS_PREPARED:
                case STATUS_UNKNOWN:
                case STATUS_PREPARING:
                case STATUS_COMMITTING:
                case STATUS_ROLLING_BACK:
                    return true;
                default:
                    return false;
            }
        } catch (SystemException e) {
            throw new RuntimeException("Unable to determine transaction status", e);
        }
    }

    public void registerSynchronization(Synchronization observer) {
        try {
            transactionManager.registerSynchronization(observer);
        } catch (Exception e) {
            throw new RuntimeException("Unable to register synchronization " + observer + " for current transaction", e);
        }
    }

    public UserTransaction getUserTransaction() {
        try {
            return (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");
        } catch (NamingException e) {
            return null;
        }
    }

    public void cleanup() {}
}


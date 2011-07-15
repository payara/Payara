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

package com.sun.enterprise.jbi.serviceengine.handlers;

//import com.sun.enterprise.Switch;
import com.sun.enterprise.jbi.serviceengine.comm.MessageExchangeTransport;

import com.sun.enterprise.jbi.serviceengine.core.ServiceEngineRuntimeHelper;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOnly;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import javax.transaction.Transaction;
import javax.transaction.InvalidTransactionException;

/**
 * Transaction handling for JBI message exchanges is done in this class. It has
 * a singleton instance.
 * 
 * @author Vikas Awasthi
 */
public class JBITransactionHandler implements JBIHandler {

//    @Inject
//    JavaEETransactionManager tm;

    public void handleInbound(MessageExchangeTransport meTransport) throws Exception {
        MessageExchange me = meTransport.getMessageExchange();
        if(!isTxEnabled(me))
            return;

        suspendTx(me);
    }

    public void handleOutbound(MessageExchangeTransport meTransport) throws Exception {
        MessageExchange me = meTransport.getMessageExchange();
        if(!isTxEnabled(me))
            return;

        resumeTx(me);
    }

    protected boolean isTxEnabled(MessageExchange me) {
        return ((tx_enable==null || tx_enable.equalsIgnoreCase("true")));
    }
    
    private void suspendTx(MessageExchange me) throws SystemException {

        //TransactionManager tm = getTM();
        ServiceEngineRuntimeHelper runtimeHelper = ServiceEngineRuntimeHelper.getRuntime();
        TransactionManager tm = runtimeHelper.getTransactionManager();
        Transaction tx = tm.getTransaction();

        if (tx != null) {
            if(me.getStatus().equals(ExchangeStatus.ERROR))
                tx.setRollbackOnly();
            tx = tm.suspend();
            me.setProperty(MessageExchange.JTA_TRANSACTION_PROPERTY_NAME, tx);
        }
    }

    private void resumeTx(MessageExchange me) throws
            SystemException,
            InvalidTransactionException {

        //TransactionManager tm = getTM();
        ServiceEngineRuntimeHelper runtimeHelper = ServiceEngineRuntimeHelper.getRuntime();
        TransactionManager tm = runtimeHelper.getTransactionManager();
        Transaction tx = (Transaction)me.getProperty(MessageExchange.JTA_TRANSACTION_PROPERTY_NAME);

        if(tx != null) {
            if(me.getStatus().equals(ExchangeStatus.ERROR))
                tx.setRollbackOnly();
            tm.resume(tx);
        }
    }

    private TransactionManager getTM() {
        //return Switch.getSwitch().getTransactionManager();
        return null;
    }
    
    private String tx_enable = System.getProperty("com.sun.enterprise.jbi.tx.enable");
}

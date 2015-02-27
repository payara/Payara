/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2015 Oracle and/or its affiliates. All rights reserved.
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

import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.PassivationCapable;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.Status;
import javax.transaction.TransactionScoped;
import javax.transaction.TransactionSynchronizationRegistry;
import java.lang.annotation.Annotation;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Context implementation for obtaining contextual instances of {@link TransactionScoped} beans.
 * <p/>
 * The contextual instances are destroyed when the transaction completes.
 * <p/>
 * Any attempt to call a method on a {@link TransactionScoped} bean when a transaction is not active will result in a
 * {@Link javax.enterprise.context.ContextNotActiveException}.
 *
 * A CDI Event: @Initialized(TransactionScoped.class) is fired with {@link TransactionScopedCDIEventPayload}, when the context is initialized for
 * the first time and @Destroyed(TransactionScoped.class) is fired with {@link TransactionScopedCDIEventPayload}, when the context is destroyed at
 * the end. Currently this payload is empty i.e. it doesn't contain any information.
 *
 * @author <a href="mailto:j.j.snyder@oracle.com">JJ Snyder</a>
 * @author <a href="mailto:arjav.desai@oracle.com">Arjav Desai</a>
 */
public class TransactionScopedContextImpl implements Context {
    public static final String TRANSACTION_SYNCHRONIZATION_REGISTRY_JNDI_NAME = "java:comp/TransactionSynchronizationRegistry";

    ConcurrentHashMap<TransactionSynchronizationRegistry,Set<TransactionScopedBean>> beansPerTransaction;

    public TransactionScopedContextImpl(){
        beansPerTransaction = new ConcurrentHashMap<>();
    }

    public ConcurrentHashMap<TransactionSynchronizationRegistry, Set<TransactionScopedBean>> getBeansPerTransaction() {
        return beansPerTransaction;
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return TransactionScoped.class;
    }

    @Override
    public <T> T get(Contextual<T> contextual, CreationalContext<T> creationalContext) {
        TransactionSynchronizationRegistry transactionSynchronizationRegistry = getTransactionSynchronizationRegistry();
        Object beanId = getContextualId(contextual);
        T contextualInstance = getContextualInstance(beanId, transactionSynchronizationRegistry);
        if (contextualInstance == null) {
            contextualInstance = createContextualInstance(contextual, beanId, creationalContext, transactionSynchronizationRegistry);
        }

        return contextualInstance;
    }

    @Override
    public <T> T get(Contextual<T> contextual) {
        TransactionSynchronizationRegistry transactionSynchronizationRegistry = getTransactionSynchronizationRegistry();
        Object beanKey = getContextualId(contextual);
        return getContextualInstance(beanKey, transactionSynchronizationRegistry);
    }

    @Override
    /**
     * Determines if this context object is active.
     *
     * @return true if there is a current global transaction and its status is
     *              {@Link javax.transaction.Status.STATUS_ACTIVE}
     *         false otherwise
     */
    public boolean isActive() {
        try {
            //Just calling it but not checking for != null on return value as its already done inside method
            getTransactionSynchronizationRegistry();
            return true;
        } catch (ContextNotActiveException ignore) {
        }
        return false;
    }

    private Object getContextualId(Contextual<?> contextual) {
        if (contextual instanceof PassivationCapable) {
            PassivationCapable passivationCapable = (PassivationCapable) contextual;
            return passivationCapable.getId();
        } else {
            return contextual;
        }
    }

    private <T> T createContextualInstance(Contextual<T> contextual,
                                           Object contextualId,
                                           CreationalContext<T> creationalContext,
                                           TransactionSynchronizationRegistry transactionSynchronizationRegistry) {
        TransactionScopedBean<T> transactionScopedBean = new TransactionScopedBean<T>(contextual, creationalContext,this);
        transactionSynchronizationRegistry.putResource(contextualId, transactionScopedBean);
        transactionSynchronizationRegistry.registerInterposedSynchronization(transactionScopedBean);
        //Adding TransactionScopedBean as Set, per transactionSynchronizationRegistry, which is unique per transaction
        //Setting synchronizedSet so that even is there are multiple transaction for an app its safe
        Set<TransactionScopedBean> transactionScopedBeanSet = beansPerTransaction.get(transactionSynchronizationRegistry);
        if (transactionScopedBeanSet != null){
            transactionScopedBeanSet = Collections.synchronizedSet(transactionScopedBeanSet);
        } else {
            transactionScopedBeanSet = Collections.synchronizedSet(new HashSet());
            //Fire this event only for the first initialization of context and not for every TransactionScopedBean in a Transaction
            TransactionScopedCDIUtil.fireEvent(TransactionScopedCDIUtil.INITIALIZED_EVENT);
            //Adding transactionScopedBeanSet in Map for the first time for this transactionSynchronizationRegistry key
            beansPerTransaction.put(transactionSynchronizationRegistry,transactionScopedBeanSet);
        }
        transactionScopedBeanSet.add(transactionScopedBean);
        //Not updating entry in main Map with new TransactionScopedBeans as it should happen by reference
        return transactionScopedBean.getContextualInstance();
    }

    private <T> T getContextualInstance(Object beanId, TransactionSynchronizationRegistry transactionSynchronizationRegistry) {
        Object obj = transactionSynchronizationRegistry.getResource(beanId);
        TransactionScopedBean<T> transactionScopedBean = (TransactionScopedBean<T>) obj;
        if (transactionScopedBean != null) {
            return transactionScopedBean.getContextualInstance();
        }
        return null;
    }

    private TransactionSynchronizationRegistry getTransactionSynchronizationRegistry() {
        TransactionSynchronizationRegistry transactionSynchronizationRegistry;
        try {
            InitialContext initialContext = new InitialContext();
            transactionSynchronizationRegistry =
                    (TransactionSynchronizationRegistry) initialContext.lookup(TRANSACTION_SYNCHRONIZATION_REGISTRY_JNDI_NAME);
        } catch (NamingException ne) {
            throw new ContextNotActiveException("Could not get TransactionSynchronizationRegistry", ne);
        }

        int status = transactionSynchronizationRegistry.getTransactionStatus();
        if (status == Status.STATUS_ACTIVE ||
                status == Status.STATUS_MARKED_ROLLBACK ||
                status == Status.STATUS_PREPARED ||
                status == Status.STATUS_UNKNOWN ||
                status == Status.STATUS_PREPARING ||
                status == Status.STATUS_COMMITTING ||
                status == Status.STATUS_ROLLING_BACK) {
            return transactionSynchronizationRegistry;
        }

        throw new ContextNotActiveException("TransactionSynchronizationRegistry status is not active.");
    }

}

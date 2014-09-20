/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2014 Oracle and/or its affiliates. All rights reserved.
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

import org.easymock.EasyMockSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.PassivationCapable;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.Status;
import javax.transaction.TransactionScoped;
import javax.transaction.TransactionSynchronizationRegistry;

import static junit.framework.Assert.*;
import static org.easymock.EasyMock.*;

/**
 * @author <a href="mailto:j.j.snyder@oracle.com">JJ Snyder</a>
 */
public class TransactionScopedContextImplTest {
    private static final String TRANSACTION_SYNCHRONIZATION_REGISTRY_JNDI_NAME = "java:comp/TransactionSynchronizationRegistry";

    private String initialContextFactoryProperty = null;
    private String urlPkgPrefixes = null;
    private EasyMockSupport mockSupport = null;
    private InitialContext initialContext = null;

    @Before
    public void beforeTest() throws Exception {
        mockSupport = new EasyMockSupport();
        initialContext = mockSupport.createMock( InitialContext.class );
        MyInitialContextFactory.setInitialContext(initialContext);

        initialContextFactoryProperty = System.getProperty( Context.INITIAL_CONTEXT_FACTORY );
        urlPkgPrefixes = System.getProperty(Context.URL_PKG_PREFIXES );
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, MyInitialContextFactory.class.getName());
        System.setProperty(Context.URL_PKG_PREFIXES, MyInitialContext.class.getPackage().getName() );
    }

    @After
    public void afterTest() throws Exception {
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, initialContextFactoryProperty == null ? "" : initialContextFactoryProperty);
        System.setProperty(Context.URL_PKG_PREFIXES, urlPkgPrefixes == null ? "" :  urlPkgPrefixes );
    }

    @Test
    public void testgetScope() {
        TransactionScopedContextImpl transactionScopedContext = new TransactionScopedContextImpl();
        assertEquals(transactionScopedContext.getScope(), TransactionScoped.class);
    }

    @Test
    public void testisActive() throws Exception {
        TransactionSynchronizationRegistry transactionSynchronizationRegistry = mockSupport.createMock(TransactionSynchronizationRegistry.class);

        expect( initialContext.lookup(TRANSACTION_SYNCHRONIZATION_REGISTRY_JNDI_NAME)).andThrow( new NamingException() );
        mockSupport.replayAll();

        TransactionScopedContextImpl transactionScopedContext = new TransactionScopedContextImpl();
        assertFalse( transactionScopedContext.isActive() );

        mockSupport.verifyAll();
        mockSupport.resetAll();

        setupMocksForInactiveTransaction(transactionSynchronizationRegistry);
        mockSupport.replayAll();

        assertFalse( transactionScopedContext.isActive() );

        mockSupport.verifyAll();
        mockSupport.resetAll();

        setupMocksForActiveTransaction( transactionSynchronizationRegistry );
        mockSupport.replayAll();

        assertTrue(transactionScopedContext.isActive());

        mockSupport.verifyAll();
        mockSupport.resetAll();

        setupMocksForActiveTransaction( transactionSynchronizationRegistry, Status.STATUS_ACTIVE );
        mockSupport.replayAll();

        assertTrue(transactionScopedContext.isActive());

        mockSupport.verifyAll();
        mockSupport.resetAll();

        setupMocksForActiveTransaction( transactionSynchronizationRegistry, Status.STATUS_MARKED_ROLLBACK );
        mockSupport.replayAll();

        assertTrue(transactionScopedContext.isActive());

        mockSupport.verifyAll();
        mockSupport.resetAll();

        setupMocksForActiveTransaction( transactionSynchronizationRegistry, Status.STATUS_PREPARED );
        mockSupport.replayAll();

        assertTrue(transactionScopedContext.isActive());

        mockSupport.verifyAll();
        mockSupport.resetAll();

        setupMocksForActiveTransaction( transactionSynchronizationRegistry, Status.STATUS_UNKNOWN );
        mockSupport.replayAll();

        assertTrue(transactionScopedContext.isActive());

        mockSupport.verifyAll();
        mockSupport.resetAll();

        setupMocksForActiveTransaction( transactionSynchronizationRegistry, Status.STATUS_PREPARING );
        mockSupport.replayAll();

        assertTrue(transactionScopedContext.isActive());

        mockSupport.verifyAll();
        mockSupport.resetAll();

        setupMocksForActiveTransaction( transactionSynchronizationRegistry, Status.STATUS_COMMITTING );
        mockSupport.replayAll();

        assertTrue(transactionScopedContext.isActive());

        mockSupport.verifyAll();
        mockSupport.resetAll();

        setupMocksForActiveTransaction( transactionSynchronizationRegistry, Status.STATUS_ROLLING_BACK );
        mockSupport.replayAll();

        assertTrue(transactionScopedContext.isActive());

        mockSupport.verifyAll();
        mockSupport.resetAll();

    }

    @Test
    public void testget() throws Exception {
        TransactionSynchronizationRegistry transactionSynchronizationRegistry = mockSupport.createMock(TransactionSynchronizationRegistry.class);
        Contextual<LocalBean> contextual = mockSupport.createMock(Contextual.class);
        CreationalContext<LocalBean> creationalContext = mockSupport.createMock(CreationalContext.class);
        ContextualPassivationCapable<LocalPassivationCapableBean> passivationCapableContextual = mockSupport.createMock(ContextualPassivationCapable.class);
        CreationalContext<LocalPassivationCapableBean> passivationCapableCreationalContext = mockSupport.createMock(CreationalContext.class);

        // test transaction not active
        setupMocksForInactiveTransaction(transactionSynchronizationRegistry);
        mockSupport.replayAll();

        TransactionScopedContextImpl transactionScopedContext = new TransactionScopedContextImpl();

        try {
            transactionScopedContext.get(contextual, creationalContext);
            fail("Should have gotten a ContextNotActiveException.");
        } catch (ContextNotActiveException ignore) {}

        mockSupport.verifyAll();
        mockSupport.resetAll();

        // test active transaction.  Create new contextual instance
        LocalBean localBean = new LocalBean();
        setupMocksForActiveTransaction(transactionSynchronizationRegistry);
        setupMocksForGetContextualInstance(transactionSynchronizationRegistry,
                                           contextual,
                                           null,
                                           null);
        setupMocksForCreateContextualInstance(transactionSynchronizationRegistry,
                                              contextual,
                                              creationalContext,
                                              contextual,
                                              localBean );
        mockSupport.replayAll();

        LocalBean retrievedLocalBean = transactionScopedContext.get(contextual, creationalContext);
        assertSame( localBean, retrievedLocalBean );

        mockSupport.verifyAll();
        mockSupport.resetAll();

        // test active transaction.  Get existing contextual instance
        TransactionScopedBean<LocalBean> transactionScopedBean =
            TransactionScopedBeanTest.getTransactionScopedBean( mockSupport,
                                                                localBean,
                                                                contextual,
                                                                creationalContext,
                    transactionScopedContext);
        setupMocksForActiveTransaction(transactionSynchronizationRegistry);
        setupMocksForGetContextualInstance(transactionSynchronizationRegistry,
                                           contextual,
                                           transactionScopedBean,
                                           localBean );
        mockSupport.replayAll();

        retrievedLocalBean = transactionScopedContext.get(contextual, creationalContext);
        assertSame( localBean, retrievedLocalBean );

        mockSupport.verifyAll();
        mockSupport.resetAll();

        // test active transaction with PassivationCapable
        String beanId = "PCCId";
        LocalPassivationCapableBean localPassivationCapableBean = new LocalPassivationCapableBean();
        TransactionScopedBean<LocalPassivationCapableBean> transactionScopedPassivationCapableBean =
            TransactionScopedBeanTest.getTransactionScopedBean( mockSupport,
                                                                localPassivationCapableBean,
                                                                passivationCapableContextual,
                                                                passivationCapableCreationalContext,
                    transactionScopedContext);
        setupMocksForActiveTransaction(transactionSynchronizationRegistry);
        expect( passivationCapableContextual.getId() ).andReturn(beanId);
        setupMocksForGetContextualInstance(transactionSynchronizationRegistry,
                                           beanId,
                                           transactionScopedPassivationCapableBean,
                                           localPassivationCapableBean );
        mockSupport.replayAll();

        LocalPassivationCapableBean retrievedLocalPassivationCapableBean =
            transactionScopedContext.get(passivationCapableContextual, passivationCapableCreationalContext);
        assertSame(localPassivationCapableBean, retrievedLocalPassivationCapableBean);

        mockSupport.verifyAll();
        mockSupport.resetAll();

        // test the get(Contextual<T> contextual) method...transaction not active
        setupMocksForInactiveTransaction(transactionSynchronizationRegistry);
        mockSupport.replayAll();

        transactionScopedContext = new TransactionScopedContextImpl();

        try {
            transactionScopedContext.get(contextual);
            fail("Should have gotten a ContextNotActiveException.");
        } catch (ContextNotActiveException ignore) {}

        mockSupport.verifyAll();
        mockSupport.resetAll();

        // test the get(Contextual<T> contextual) method...transaction active
        setupMocksForActiveTransaction(transactionSynchronizationRegistry);
        setupMocksForGetContextualInstance(transactionSynchronizationRegistry,
                                           contextual,
                                           transactionScopedBean,
                                           localBean);
        mockSupport.replayAll();

        retrievedLocalBean = transactionScopedContext.get(contextual);
        assertSame( localBean, retrievedLocalBean );

        mockSupport.verifyAll();
        mockSupport.resetAll();
    }

    private void setupMocksForActiveTransaction(TransactionSynchronizationRegistry transactionSynchronizationRegistry,
                                                int status) throws Exception {
        expect( initialContext.lookup(TRANSACTION_SYNCHRONIZATION_REGISTRY_JNDI_NAME)).andReturn( transactionSynchronizationRegistry );
        expect( transactionSynchronizationRegistry.getTransactionStatus() ).andReturn( status );
    }

    private void setupMocksForActiveTransaction(TransactionSynchronizationRegistry transactionSynchronizationRegistry) throws Exception {
        setupMocksForActiveTransaction( transactionSynchronizationRegistry, Status.STATUS_ACTIVE );
    }

    private void setupMocksForInactiveTransaction(TransactionSynchronizationRegistry transactionSynchronizationRegistry) throws Exception {
        expect( initialContext.lookup(TRANSACTION_SYNCHRONIZATION_REGISTRY_JNDI_NAME)).andReturn( transactionSynchronizationRegistry );
        expect( transactionSynchronizationRegistry.getTransactionStatus() ).andReturn( Status.STATUS_NO_TRANSACTION );
    }

    private <T> void setupMocksForGetContextualInstance(TransactionSynchronizationRegistry transactionSynchronizationRegistry,
                                                        Object beanId,
                                                        TransactionScopedBean<T> transactionScopedBean,
                                                        T contextualInstance ) throws Exception {
        expect( transactionSynchronizationRegistry.getResource(beanId) ).andReturn( transactionScopedBean );
    }

    private <T> void setupMocksForCreateContextualInstance(TransactionSynchronizationRegistry transactionSynchronizationRegistry,
                                                           Contextual<T> contextual,
                                                           CreationalContext<T> creationalContext,
                                                           Object beanId,
                                                           T beanInstance ) throws Exception {
        expect( contextual.create( creationalContext ) ).andReturn( beanInstance );
        transactionSynchronizationRegistry.putResource( same(beanId), isA( TransactionScopedBean.class) );
        transactionSynchronizationRegistry.registerInterposedSynchronization( isA( TransactionScopedBean.class) );
    }

    private interface ContextualPassivationCapable<T> extends Contextual<T>, PassivationCapable {}

    private class LocalBean {}
    private class LocalPassivationCapableBean {}
}


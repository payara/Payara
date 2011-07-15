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

package com.sun.enterprise.container.common.impl;

import com.sun.enterprise.container.common.spi.JavaEEContainer;
import com.sun.enterprise.container.common.spi.util.CallFlowAgent;
import com.sun.enterprise.container.common.spi.util.ComponentEnvManager;
import com.sun.enterprise.container.common.spi.util.EntityManagerMethod;
import com.sun.enterprise.transaction.api.JavaEETransaction;
import com.sun.logging.LogDomains;
import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.internal.api.Globals;
import org.jvnet.hk2.component.Habitat;

import javax.persistence.criteria.*;
import javax.persistence.*;
import javax.persistence.metamodel.Metamodel;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of a container-managed entity manager.  
 * A new instance of this class will be created for each injected
 * EntityManager reference or each lookup of an EntityManager
 * reference within the component jndi environment. 
 * The underlying EntityManager object does not support concurrent access.
 * Likewise, this wrapper does not support concurrent access.  
 *
 * @author Kenneth Saks
 */
public class EntityManagerWrapper implements EntityManager, Serializable {

    static Logger _logger=LogDomains.getLogger(EntityManagerWrapper.class, LogDomains.UTIL_LOGGER);

    // Serializable state

    private String unitName;
    private PersistenceContextType contextType;
    private Map emProperties;

    // transient state

    transient private EntityManagerFactory entityManagerFactory;

    transient private TransactionManager txManager;

    transient private InvocationManager invMgr;
    
    // Only used to cache entity manager with EXTENDED persistence context
    transient private EntityManager extendedEntityManager;

    // set and cleared after each non-tx, non EXTENDED call to _getDelegate()
    transient private EntityManager nonTxEntityManager;

    transient private ComponentEnvManager compEnvMgr;

    transient private CallFlowAgent callFlowAgent;

    public EntityManagerWrapper(TransactionManager txManager, InvocationManager invMgr,
                                ComponentEnvManager compEnvMgr, CallFlowAgent callFlowAgent) {
        this.txManager = txManager;
        this.invMgr = invMgr;
        this.compEnvMgr = compEnvMgr;
        this.callFlowAgent = callFlowAgent;
    }

    public void initializeEMWrapper(String unitName,
        PersistenceContextType contextType, Map emProperties) {
        this.unitName = unitName;
        this.contextType = contextType;
        this.emProperties = emProperties;
        if(contextType == PersistenceContextType.EXTENDED) {
            // We are initializing an extended EM. The physical em is already created and stored in SessionContext to
            // enable persistence context propagation.
            // Initialize the delegate eagerly to support use cases like issue 11805
            _getDelegate();
        }
    }

    private void init() {

        entityManagerFactory = EntityManagerFactoryWrapper.
            lookupEntityManagerFactory(invMgr, compEnvMgr, unitName);
        
        if( entityManagerFactory == null ) {
            throw new IllegalStateException
                ("Unable to retrieve EntityManagerFactory for unitName "
                 + unitName);
        }

    }

    private void doTransactionScopedTxCheck() {
        
        if( contextType != PersistenceContextType.TRANSACTION) {
            return;
        }
        
        doTxRequiredCheck();

    }

    private void doTxRequiredCheck() {

        if( entityManagerFactory == null ) {
            init();
        }

        Transaction tx = null;
        try {
            tx = txManager.getTransaction();
        } catch(Exception e) {
            throw new IllegalStateException("exception retrieving tx", e);
        }
            
        if( tx == null ) {
            throw new TransactionRequiredException();
        }

    }
    private EntityManager _getDelegate() {

        // Populate any transient objects the first time 
        // this method is called.

        if( entityManagerFactory == null ) {
            init();
        }

        EntityManager delegate = null;

        if( nonTxEntityManager != null ) {
            cleanupNonTxEntityManager();
        }

        if( contextType == PersistenceContextType.TRANSACTION ) {

            JavaEETransaction tx = null;
            try {
                tx = (JavaEETransaction) txManager.getTransaction();
            } catch(Exception e) {
                throw new IllegalStateException("exception retrieving tx", e);
            }
            
            if( tx != null ) {

                // If there is an active extended persistence context
                // for the same entity manager factory and the same tx,
                // it takes precendence.
                delegate = tx.getExtendedEntityManager(entityManagerFactory);

                if( delegate == null ) {

                    delegate = tx.getTxEntityManager(entityManagerFactory);

                    if( delegate == null ) {

                        // If there is a transaction and this is the first
                        // access of the wrapped entity manager, create an
                        // actual entity manager and associate it with the
                        // entity manager factory.
                        delegate = entityManagerFactory.
                            createEntityManager(emProperties);

                        tx.addTxEntityManagerMapping(entityManagerFactory, 
                                                     delegate);
                    }
                }

            } else {

                nonTxEntityManager = entityManagerFactory.createEntityManager
                    (emProperties);

                // Return a new non-transactional entity manager.
                delegate = nonTxEntityManager;
                    
            }

        } else {

            // EXTENDED Persitence Context

            if( extendedEntityManager == null ) {
                ComponentInvocation ci = invMgr.getCurrentInvocation();
                if (ci != null) {
                    Object cc = ci.getContainer();
                    if (cc instanceof JavaEEContainer) {
                        extendedEntityManager = ((JavaEEContainer) cc).lookupExtendedEntityManager(
                                entityManagerFactory);
                    }
                }   
            }          
      
            delegate = extendedEntityManager;

        }
        
        if( _logger.isLoggable(Level.FINE) ) {
            _logger.fine("In EntityManagerWrapper::_getDelegate(). " +
                         "Logical entity manager  = " + this);
            _logger.fine("Physical entity manager = " + delegate);
        }

        return delegate;

    }
    
    private void cleanupNonTxEntityManager() {
        if( nonTxEntityManager != null ) {
            nonTxEntityManager.close();
            nonTxEntityManager = null;
        }
    }

    public void persist(Object entity) {
        doTransactionScopedTxCheck();

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.PERSIST);
            }
            _getDelegate().persist(entity);
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }

        // tx is required so there's no need to do any non-tx cleanup
    }

    public <T> T merge(T entity) {
        doTransactionScopedTxCheck();

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.MERGE);
            }
            return _getDelegate().merge(entity);
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }

        // tx is required so there's no need to do any non-tx cleanup
    }

    public void remove(Object entity) {
        doTransactionScopedTxCheck();

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.REMOVE);
            }
            _getDelegate().remove(entity);
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
        
        // tx is required so there's no need to do any non-tx cleanup
    }

    public <T> T find(Class<T> entityClass, Object primaryKey) {
        T returnValue = null;

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.FIND);
            }
            returnValue = _getDelegate().find(entityClass, primaryKey);
        } finally {
            if( nonTxEntityManager != null ) {
                cleanupNonTxEntityManager();
            }
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
        return returnValue;
    }

    public <T> T find(Class<T> entityClass, Object primaryKey, Map<String, Object> properties) {
        T returnValue = null;

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.FIND);
            }
            returnValue = _getDelegate().find(entityClass, primaryKey, properties);
        } finally {
            if( nonTxEntityManager != null ) {
                cleanupNonTxEntityManager();
            }
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
        return returnValue;
    }


    public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode) {
        T returnValue = null;

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.FIND_CLASS_OBJECT_LOCKMODETYPE);
            }
            returnValue = _getDelegate().find(entityClass, primaryKey, lockMode);
        } finally {
            if( nonTxEntityManager != null ) {
                cleanupNonTxEntityManager();
            }
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
        return returnValue;
    }

    public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode, Map<String, Object> properties) {
        T returnValue = null;

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.FIND_CLASS_OBJECT_LOCKMODETYPE_PROPERTIES);
            }
            returnValue = _getDelegate().find(entityClass, primaryKey, lockMode, properties);
        } finally {
            if( nonTxEntityManager != null ) {
                cleanupNonTxEntityManager();
            }
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
        return returnValue;
    }

    public <T> T getReference(Class<T> entityClass, Object primaryKey) {
        T returnValue = null;

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.GET_REFERENCE);
            }
            returnValue = _getDelegate().getReference(entityClass, primaryKey);
        } finally {
            if( nonTxEntityManager != null ) {
                cleanupNonTxEntityManager();
            }
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
        return returnValue;
    }

    public void flush() {
        // tx is ALWAYS required, regardless of persistence context type.
        doTxRequiredCheck();
        

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.FLUSH);
            }
            _getDelegate().flush();
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
        
        // tx is required so there's no need to do any non-tx cleanup
    }

    public Query createQuery(String ejbqlString) {
        Query returnValue = null;

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.CREATE_QUERY);
            }
            EntityManager delegate = _getDelegate();
            returnValue = delegate.createQuery(ejbqlString);

            if( nonTxEntityManager != null ) {
                Query queryDelegate = returnValue;
                returnValue = QueryWrapper.createQueryWrapper
                    (entityManagerFactory, emProperties, delegate,
                     queryDelegate, ejbqlString);
                // It's now the responsibility of the QueryWrapper to 
                // close the non-tx EM delegate
                nonTxEntityManager = null;
            }
        } catch(RuntimeException re) {
            if( nonTxEntityManager != null ) {
                cleanupNonTxEntityManager();
            }
            throw re;
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
        return returnValue;
    }

    public <T> TypedQuery<T> createQuery(String ejbqlString, Class<T> resultClass) {
        TypedQuery<T> returnValue = null;

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.CREATE_QUERY_STRING_CLASS);
            }
            EntityManager delegate = _getDelegate();
            returnValue = delegate.createQuery(ejbqlString, resultClass);

            if( nonTxEntityManager != null ) {
                TypedQuery<T> queryDelegate = returnValue;
                returnValue = TypedQueryWrapper.createQueryWrapper
                    (entityManagerFactory, emProperties, delegate,
                     queryDelegate, ejbqlString, resultClass);
                // It's now the responsibility of the QueryWrapper to
                // close the non-tx EM delegate
                nonTxEntityManager = null;
            }
        } catch(RuntimeException re) {
            if( nonTxEntityManager != null ) {
                cleanupNonTxEntityManager();
            }
            throw re;
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
        return returnValue;
    }

    public <T> TypedQuery<T> createQuery(CriteriaQuery<T> criteriaQuery) {
        TypedQuery<T> returnValue = null;

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.CREATE_QUERY_CRITERIA_QUERY);
            }
            EntityManager delegate = _getDelegate();
            returnValue = delegate.createQuery(criteriaQuery);

            if( nonTxEntityManager != null ) {
                TypedQuery<T> queryDelegate = returnValue;

                returnValue = TypedQueryWrapper.createQueryWrapper
                    (entityManagerFactory, emProperties, delegate,
                     queryDelegate, criteriaQuery);
                // It's now the responsibility of the QueryWrapper to
                // close the non-tx EM delegate
                nonTxEntityManager = null;
            }
        } catch(RuntimeException re) {
            if( nonTxEntityManager != null ) {
                cleanupNonTxEntityManager();
            }
            throw re;
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
        return returnValue;
    }

    public Query createNamedQuery(String name) {
        Query returnValue = null;

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.CREATE_NAMED_QUERY);
            }
            EntityManager delegate = _getDelegate();
            returnValue = delegate.createNamedQuery(name);

            if( nonTxEntityManager != null ) {
                Query queryDelegate = returnValue;
                returnValue = QueryWrapper.createNamedQueryWrapper
                    (entityManagerFactory, emProperties, delegate,
                     queryDelegate, name);
                // It's now the responsibility of the QueryWrapper to 
                // close the non-tx EM delegate
                nonTxEntityManager = null;
            }
        } catch(RuntimeException re) {
            if( nonTxEntityManager != null ) {
                cleanupNonTxEntityManager();
            }
            throw re;
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }

        return returnValue;
    }

    public <T> TypedQuery<T> createNamedQuery(String name, Class<T> resultClass) {
        TypedQuery<T> returnValue = null;

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.CREATE_NAMED_QUERY);
            }
            EntityManager delegate = _getDelegate();
            returnValue = delegate.createNamedQuery(name, resultClass);

            if( nonTxEntityManager != null ) {
                TypedQuery<T> queryDelegate = returnValue;
                returnValue = TypedQueryWrapper.createNamedQueryWrapper
                    (entityManagerFactory, emProperties, delegate,
                     queryDelegate, name, resultClass);
                // It's now the responsibility of the QueryWrapper to
                // close the non-tx EM delegate
                nonTxEntityManager = null;
            }
        } catch(RuntimeException re) {
            if( nonTxEntityManager != null ) {
                cleanupNonTxEntityManager();
            }
            throw re;
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }

        return returnValue;
    }

    public Query createNativeQuery(String sqlString) {
        Query returnValue = null;

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.CREATE_NATIVE_QUERY_STRING);
            }
            EntityManager delegate = _getDelegate();
            returnValue = delegate.createNativeQuery(sqlString);

            if( nonTxEntityManager != null ) {
                Query queryDelegate = returnValue;
                returnValue = QueryWrapper.createNativeQueryWrapper
                    (entityManagerFactory, emProperties, delegate,
                     queryDelegate, sqlString);
                // It's now the responsibility of the QueryWrapper to 
                // close the non-tx EM delegate
                nonTxEntityManager = null;
            }
        } catch(RuntimeException re) {
            if( nonTxEntityManager != null ) {
                cleanupNonTxEntityManager();
            }
            throw re;
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
        return returnValue;
    }

    public Query createNativeQuery(String sqlString, Class resultClass) {
        Query returnValue = null;

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.CREATE_NATIVE_QUERY_STRING_CLASS);
            }
            EntityManager delegate = _getDelegate();
            returnValue = delegate.createNativeQuery(sqlString, resultClass);

            if( nonTxEntityManager != null ) {
                Query queryDelegate = returnValue;
                returnValue = QueryWrapper.createNativeQueryWrapper
                    (entityManagerFactory, emProperties, delegate,
                     queryDelegate, sqlString, resultClass);
                // It's now the responsibility of the QueryWrapper to 
                // close the non-tx EM delegate
                nonTxEntityManager = null;
            }
        } catch(RuntimeException re) {
            if( nonTxEntityManager != null ) {
                cleanupNonTxEntityManager();
            }
            throw re;
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
        return returnValue;
    }

    public Query createNativeQuery(String sqlString, String resultSetMapping) {
        Query returnValue = null;

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.CREATE_NATIVE_QUERY_STRING_STRING);
            }
            EntityManager delegate = _getDelegate();
            returnValue = delegate.createNativeQuery
                (sqlString, resultSetMapping);

            if( nonTxEntityManager != null ) {
                Query queryDelegate = returnValue;
                returnValue = QueryWrapper.createNativeQueryWrapper
                    (entityManagerFactory, emProperties, delegate,
                     queryDelegate, sqlString, resultSetMapping);
                // It's now the responsibility of the QueryWrapper to 
                // close the non-tx EM delegate
                nonTxEntityManager = null;
            }
        } catch(RuntimeException re) {
            if( nonTxEntityManager != null ) {
                cleanupNonTxEntityManager();
            }
            throw re;
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
        return returnValue;
    }

    public void refresh(Object entity) {
        doTransactionScopedTxCheck();


        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.REFRESH);
            }
            _getDelegate().refresh(entity);
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
        
        // tx is required so there's no need to do any non-tx cleanup
    }

    public void refresh(Object entity, Map<String, Object> properties) {
        doTransactionScopedTxCheck();


        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.REFRESH_OBJECT_PROPERTIES);
            }
            _getDelegate().refresh(entity, properties);
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }

        // tx is required so there's no need to do any non-tx cleanup
    }

    public void refresh(Object entity, LockModeType lockMode) {
        doTransactionScopedTxCheck();

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.REFRESH_OBJECT_LOCKMODETYPE);
            }
            _getDelegate().refresh(entity, lockMode);
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }

        // tx is required so there's no need to do any non-tx cleanup
    }

    public void refresh(Object entity, LockModeType lockMode, Map<String, Object> properties) {
        doTransactionScopedTxCheck();

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.REFRESH_OBJECT_LOCKMODETYPE_MAP);
            }
            _getDelegate().refresh(entity, lockMode, properties);
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }

        // tx is required so there's no need to do any non-tx cleanup
    }

    public boolean contains(Object entity) {

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.CONTAINS);
            }
            EntityManager delegate = _getDelegate();
            return delegate.contains(entity);
        } finally {
            if( nonTxEntityManager != null ) {
                cleanupNonTxEntityManager();
            }
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
    }

    public LockModeType getLockMode(Object o) {

        doTxRequiredCheck();

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.GET_LOCK_MODE);
            }
            return _getDelegate().getLockMode(o);
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }

        // tx is required so there's no need to do any non-tx cleanup
    }

    public void setProperty(String propertyName, Object value) {

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.SET_PROPERTY);
            }
            _getDelegate().setProperty(propertyName, value);
        } finally {
            if( nonTxEntityManager != null ) {
                cleanupNonTxEntityManager();
            }
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }

        // tx is required so there's no need to do any non-tx cleanup
    }


    public Map<String, Object> getProperties() {

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.GET_PROPERTIES);
            }
            return _getDelegate().getProperties();
        } finally {
            if( nonTxEntityManager != null ) {
                cleanupNonTxEntityManager();
            }
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
    }

    public void close() {

        if(callFlowAgent.isEnabled()) {
            callFlowAgent.entityManagerMethodStart(EntityManagerMethod.CLOSE);
            callFlowAgent.entityManagerMethodEnd();
        }
        // close() not allowed on container-managed EMs.
        throw new IllegalStateException();
    }

    public boolean isOpen() {

        if(callFlowAgent.isEnabled()) {
            callFlowAgent.entityManagerMethodStart(EntityManagerMethod.IS_OPEN);
            callFlowAgent.entityManagerMethodEnd();
        }
        // Not relevant for container-managed EMs.  Just return true.
        return true;
    }

    public EntityTransaction getTransaction() {

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.GET_TRANSACTION);
            }
            return _getDelegate().getTransaction();
        } finally {
            if( nonTxEntityManager != null ) {
                cleanupNonTxEntityManager();
            }
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
    }

    public EntityManagerFactory getEntityManagerFactory() {

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.GET_ENTITY_MANAGER_FACTORY);
            }
            if( entityManagerFactory == null ) {
                init();
            }

            // Spec requires to throw IllegalStateException if this em has been closed.
            // No need to perform the check here as this can not happen for managed em.
            // No need to go to delegate for this. 
            return entityManagerFactory;
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
    }

    public CriteriaBuilder getCriteriaBuilder() {

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.GET_CRITERIA_BUILDER);
            }
            return _getDelegate().getCriteriaBuilder();
        } finally {
            if( nonTxEntityManager != null ) {
                cleanupNonTxEntityManager();
            }
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }

    }

    public Metamodel getMetamodel() {

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.GET_METAMODEL);
            }
            return _getDelegate().getMetamodel();
        } finally {
            if( nonTxEntityManager != null ) {
                cleanupNonTxEntityManager();
            }
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }

    }

    public void lock(Object entity, LockModeType lockMode) {

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.LOCK);
            }
            _getDelegate().lock(entity, lockMode);
        } finally {
            if( nonTxEntityManager != null ) {
                cleanupNonTxEntityManager();
            }
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
    }

    public void lock(Object entity, LockModeType lockMode, Map<String, Object> properties) {

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.LOCK_LOCKMODETYPE_MAP);
            }
            _getDelegate().lock(entity, lockMode, properties);
        } finally {
            if( nonTxEntityManager != null ) {
                cleanupNonTxEntityManager();
            }
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
    }

    public void clear() {

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.CLEAR);
            }
            _getDelegate().clear();
        } finally {
            if( nonTxEntityManager != null ) {
                cleanupNonTxEntityManager();
            }
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
    }

    public void detach(Object o) {

        //TODO revisit this check once Linda confirms whether it is required or not.
        doTransactionScopedTxCheck();

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.DETATCH);
            }
            _getDelegate().detach(o);
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }

        // tx is required so there's no need to do any non-tx cleanup
    }

    public Object getDelegate() {

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.GET_DELEGATE);
            }
            return _getDelegate();
        } finally {
            if( nonTxEntityManager != null ) {
                // In this case we can't close the physical EntityManager
                // before returning it to the application, so we just clear
                // the EM wrapper's reference to it.  
                nonTxEntityManager = null;
            }
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }

    }

    public FlushModeType getFlushMode() {

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.GET_FLUSH_MODE);
            }
            return _getDelegate().getFlushMode();
        } finally {
            if( nonTxEntityManager != null ) {
                cleanupNonTxEntityManager();
            }
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
    }

    public void setFlushMode(FlushModeType flushMode) {

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.SET_FLUSH_MODE);
            }
            _getDelegate().setFlushMode(flushMode);
        } finally {
            if( nonTxEntityManager != null ) {
                cleanupNonTxEntityManager();
            }
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
    }

    public void joinTransaction() {
        // Doesn't apply to the container-managed case, but all the
        // spec says is that an exception should be thrown if called
        // without a tx.
        doTxRequiredCheck();

        if(callFlowAgent.isEnabled()) {
            callFlowAgent.entityManagerMethodStart(EntityManagerMethod.JOIN_TRANSACTION);
            callFlowAgent.entityManagerMethodEnd();
        }

        // There's no point in calling anything on the physical 
        // entity manager since in all tx cases it will be
        // correctly associated with a tx already.
    }

    public <T> T unwrap(Class<T> tClass) {

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.UNWRAP);
            }
            return _getDelegate().unwrap(tClass);
        } finally {
            if( nonTxEntityManager != null ) {
                cleanupNonTxEntityManager();
            }
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();

        //Initialize the transients that were passed at ctor.
        Habitat defaultHabitat = Globals.getDefaultHabitat();
        txManager     = defaultHabitat.getByContract(TransactionManager.class);
        invMgr        = defaultHabitat.getByContract(InvocationManager.class);
        compEnvMgr    = defaultHabitat.getByContract(ComponentEnvManager.class);
        callFlowAgent = defaultHabitat.getByContract(CallFlowAgent.class);
    }


}

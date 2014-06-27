/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2014 Oracle and/or its affiliates. All rights reserved.
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
import org.glassfish.api.invocation.ComponentInvocationHandler;
import org.glassfish.api.invocation.InvocationException;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;
import org.jvnet.hk2.annotations.Service;

import javax.ejb.EJBException;
import static javax.persistence.SynchronizationType.SYNCHRONIZED;
import static javax.persistence.SynchronizationType.UNSYNCHRONIZED;
import javax.persistence.criteria.*;
import javax.persistence.*;
import javax.persistence.metamodel.Metamodel;
import javax.transaction.TransactionManager;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
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
    private SynchronizationType synchronizationType;

    // transient state

    transient private EntityManagerFactory entityManagerFactory;

    transient private TransactionManager txManager;

    transient private InvocationManager invMgr;

    // Only used to cache entity manager with EXTENDED persistence context
    transient private EntityManager extendedEntityManager;

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
        PersistenceContextType contextType, SynchronizationType synchronizationType, Map emProperties) {
        this.unitName = unitName;
        this.contextType = contextType;
        this.synchronizationType = synchronizationType;
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
        if( getCurrentTransaction() == null ) {
            throw new TransactionRequiredException();
        }

    }
    private EntityManager _getDelegate() {

        // Populate any transient objects the first time
        // this method is called.

        if( entityManagerFactory == null ) {
            init();
        }

        EntityManager delegate;

        if( contextType == PersistenceContextType.TRANSACTION ) {

            JavaEETransaction tx = getCurrentTransaction();

            if( tx != null ) {

                // If there is an active extended persistence context
                // for the same entity manager factory and the same tx,
                // it takes precedence.
                PhysicalEntityManagerWrapper propagatedPersistenceContext = getExtendedEntityManager(tx, entityManagerFactory);
                if(propagatedPersistenceContext == null) {

                    propagatedPersistenceContext = getTxEntityManager(tx, entityManagerFactory);


                    if( propagatedPersistenceContext == null ) {

                        // If there is a transaction and this is the first
                        // access of the wrapped entity manager, create an
                        // actual entity manager and associate it with the
                        // entity manager factory.
                        EntityManager em = entityManagerFactory.createEntityManager(synchronizationType, emProperties);
                        propagatedPersistenceContext = new PhysicalEntityManagerWrapper(em, synchronizationType);
                        tx.addTxEntityManagerMapping(entityManagerFactory, propagatedPersistenceContext);
                    } else {

                        //Check if sync type of current persistence context is compatible with persistence context being propagated
                        if(synchronizationType == SYNCHRONIZED && propagatedPersistenceContext.getSynchronizationType() == UNSYNCHRONIZED) {
                            throw new IllegalStateException("Detected an UNSYNCHRONIZED  persistence context being propagated to SYNCHRONIZED persistence context.");
                        }

                    }
                }
                delegate = propagatedPersistenceContext.getEM();

            } else {
                //Get non transactional entity manager corresponding to this wrapper from current invocation
                delegate  = getNonTxEMFromCurrentInvocation();
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

    private JavaEETransaction getCurrentTransaction() {
        try {
            return (JavaEETransaction) txManager.getTransaction();
        } catch(Exception e) {
            throw new IllegalStateException("exception retrieving tx", e);
        }
    }

    private static final Class INVOCATION_PAYLOAD_KEY = EntityManagerWrapper.class; //Key used to look up payload from current invocation

    private EntityManager getNonTxEMFromCurrentInvocation() {
        // We store nonTxEM as a payload in a map from EMF to EM inside current invocation.
        // It will be closed during  NonTxEntityManagerCleaner.beforePostInvoke() below
        
        ComponentInvocation currentInvocation = invMgr.getCurrentInvocation();
        Map<EntityManagerFactory, EntityManager> nonTxEMs = getNonTxEMsFromCurrentInvocation(currentInvocation);
        if(nonTxEMs == null) {
            nonTxEMs = new HashMap<>();
            currentInvocation.setRegistryFor(INVOCATION_PAYLOAD_KEY, nonTxEMs);
        }
        EntityManager nonTxEM = nonTxEMs.get(entityManagerFactory);
        if(nonTxEM == null) {
            // Could not find one, create new one and store it within current invocation for cleanup
            nonTxEM = entityManagerFactory.createEntityManager(synchronizationType, emProperties);
            nonTxEMs.put(entityManagerFactory, nonTxEM);
        }
        return nonTxEM;
    }

    @SuppressWarnings("unchecked")
    private static Map<EntityManagerFactory, EntityManager> getNonTxEMsFromCurrentInvocation(ComponentInvocation currentInvocation) {
        return (Map<EntityManagerFactory, EntityManager>)currentInvocation.getRegistryFor(INVOCATION_PAYLOAD_KEY);
    }

    @Override
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

    @Override
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

    @Override
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

    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey) {
        T returnValue = null;

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.FIND);
            }
            EntityManager delegate = _getDelegate();
            returnValue = delegate.find(entityClass, primaryKey);
            clearPersistenceContextPerhaps(delegate);
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
        return returnValue;
    }

    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey, Map<String, Object> properties) {
        T returnValue = null;

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.FIND);
            }
            EntityManager delegate = _getDelegate();
            returnValue = _getDelegate().find(entityClass, primaryKey, properties);
            clearPersistenceContextPerhaps(delegate);
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
        return returnValue;
    }


    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode) {
        T returnValue = null;

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.FIND_CLASS_OBJECT_LOCKMODETYPE);
            }
            EntityManager delegate = _getDelegate();
            returnValue = _getDelegate().find(entityClass, primaryKey, lockMode);
            clearPersistenceContextPerhaps(delegate);
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
        return returnValue;
    }

    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode, Map<String, Object> properties) {
        T returnValue = null;

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.FIND_CLASS_OBJECT_LOCKMODETYPE_PROPERTIES);
            }
            EntityManager delegate = _getDelegate();
            returnValue = _getDelegate().find(entityClass, primaryKey, lockMode, properties);
            clearPersistenceContextPerhaps(delegate);
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
        return returnValue;
    }

    @Override
    public <T> T getReference(Class<T> entityClass, Object primaryKey) {
        T returnValue = null;

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.GET_REFERENCE);
            }
            returnValue = _getDelegate().getReference(entityClass, primaryKey);
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
        return returnValue;
    }

    @Override
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

    @Override
    public Query createQuery(String ejbqlString) {
        Query returnValue = null;

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.CREATE_QUERY);
            }
            EntityManager delegate = _getDelegate();
            returnValue = delegate.createQuery(ejbqlString);

            if( getCurrentTransaction() == null ) {
                returnValue = QueryWrapper.createQueryWrapper(returnValue, delegate);
            }
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
        return returnValue;
    }

    @Override
    public <T> TypedQuery<T> createQuery(String ejbqlString, Class<T> resultClass) {
        TypedQuery<T> returnValue = null;

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.CREATE_QUERY_STRING_CLASS);
            }
            EntityManager delegate = _getDelegate();
            returnValue = delegate.createQuery(ejbqlString, resultClass);

            if( getCurrentTransaction() == null ) {
                returnValue = TypedQueryWrapper.createQueryWrapper(returnValue, delegate);
            }
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
        return returnValue;
    }

    @Override
    public <T> TypedQuery<T> createQuery(CriteriaQuery<T> criteriaQuery) {
        TypedQuery<T> returnValue = null;

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.CREATE_QUERY_CRITERIA_QUERY);
            }
            EntityManager delegate = _getDelegate();
            returnValue = delegate.createQuery(criteriaQuery);

            if( getCurrentTransaction() == null) {
                returnValue = TypedQueryWrapper.createQueryWrapper(returnValue, delegate);
            }
        }finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
        return returnValue;
    }

    @Override
    public Query createNamedQuery(String name) {
        Query returnValue = null;

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.CREATE_NAMED_QUERY);
            }
            EntityManager delegate = _getDelegate();
            returnValue = delegate.createNamedQuery(name);

            if( getCurrentTransaction() == null) {
                returnValue = QueryWrapper.createQueryWrapper(returnValue, delegate);
            }
        }finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }

        return returnValue;
    }

    @Override
    public <T> TypedQuery<T> createNamedQuery(String name, Class<T> resultClass) {
        TypedQuery<T> returnValue = null;

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.CREATE_NAMED_QUERY);
            }
            EntityManager delegate = _getDelegate();
            returnValue = delegate.createNamedQuery(name, resultClass);

            if( getCurrentTransaction() == null) {
                returnValue = TypedQueryWrapper.createQueryWrapper(returnValue, delegate);
            }
        }finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }

        return returnValue;
    }

    @Override
    public Query createNativeQuery(String sqlString) {
        Query returnValue = null;

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.CREATE_NATIVE_QUERY_STRING);
            }
            EntityManager delegate = _getDelegate();
            returnValue = delegate.createNativeQuery(sqlString);

            if( getCurrentTransaction() == null) {
                returnValue = QueryWrapper.createQueryWrapper(returnValue, delegate);
            }
        }finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
        return returnValue;
    }

    @Override
    public Query createNativeQuery(String sqlString, Class resultClass) {
        Query returnValue = null;

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.CREATE_NATIVE_QUERY_STRING_CLASS);
            }
            EntityManager delegate = _getDelegate();
            returnValue = delegate.createNativeQuery(sqlString, resultClass);

            if( getCurrentTransaction() == null) {
                returnValue = QueryWrapper.createQueryWrapper(returnValue, delegate);
            }
        }finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
        return returnValue;
    }

    @Override
    public Query createNativeQuery(String sqlString, String resultSetMapping) {
        Query returnValue = null;

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.CREATE_NATIVE_QUERY_STRING_STRING);
            }
            EntityManager delegate = _getDelegate();
            returnValue = delegate.createNativeQuery
                (sqlString, resultSetMapping);

            if( getCurrentTransaction() == null) {
                returnValue = QueryWrapper.createQueryWrapper(returnValue, delegate);
            }
        }finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
        return returnValue;
    }

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
    public boolean contains(Object entity) {

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.CONTAINS);
            }
            EntityManager delegate = _getDelegate();
            return delegate.contains(entity);
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
    }

    @Override
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

    @Override
    public void setProperty(String propertyName, Object value) {

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.SET_PROPERTY);
            }
            _getDelegate().setProperty(propertyName, value);
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }

        // tx is required so there's no need to do any non-tx cleanup
    }


    @Override
    public Map<String, Object> getProperties() {

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.GET_PROPERTIES);
            }
            return _getDelegate().getProperties();
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
    }

    @Override
    public void close() {

        if(callFlowAgent.isEnabled()) {
            callFlowAgent.entityManagerMethodStart(EntityManagerMethod.CLOSE);
            callFlowAgent.entityManagerMethodEnd();
        }
        // close() not allowed on container-managed EMs.
        throw new IllegalStateException();
    }

    @Override
    public boolean isOpen() {

        if(callFlowAgent.isEnabled()) {
            callFlowAgent.entityManagerMethodStart(EntityManagerMethod.IS_OPEN);
            callFlowAgent.entityManagerMethodEnd();
        }
        // Not relevant for container-managed EMs.  Just return true.
        return true;
    }

    @Override
    public EntityTransaction getTransaction() {

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.GET_TRANSACTION);
            }
            return _getDelegate().getTransaction();
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
    }

    @Override
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

    @Override
    public CriteriaBuilder getCriteriaBuilder() {

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.GET_CRITERIA_BUILDER);
            }
            return _getDelegate().getCriteriaBuilder();
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }

    }

    @Override
    public Metamodel getMetamodel() {

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.GET_METAMODEL);
            }
            return _getDelegate().getMetamodel();
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }

    }

    @Override
    public void lock(Object entity, LockModeType lockMode) {

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.LOCK);
            }
            _getDelegate().lock(entity, lockMode);
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
    }

    @Override
    public void lock(Object entity, LockModeType lockMode, Map<String, Object> properties) {

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.LOCK_LOCKMODETYPE_MAP);
            }
            _getDelegate().lock(entity, lockMode, properties);
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
    }

    @Override
    public void clear() {

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.CLEAR);
            }
            _getDelegate().clear();
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
    }

    @Override
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

    @Override
    public Object getDelegate() {

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.GET_DELEGATE);
            }
            return _getDelegate();
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }

    }

    @Override
    public FlushModeType getFlushMode() {

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.GET_FLUSH_MODE);
            }
            return _getDelegate().getFlushMode();
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
    }

    @Override
    public void setFlushMode(FlushModeType flushMode) {

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.SET_FLUSH_MODE);
            }
            _getDelegate().setFlushMode(flushMode);
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
    }

    @Override
    public void joinTransaction() {
        // Doesn't apply to the container-managed case, but all the
        // spec says is that an exception should be thrown if called
        // without a tx.
        doTxRequiredCheck();

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.JOIN_TRANSACTION);
            }
            _getDelegate().joinTransaction();
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }

    }

    @Override
    public <T> T unwrap(Class<T> tClass) {

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.UNWRAP);
            }
            return _getDelegate().unwrap(tClass);
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
    }

    @Override
    public StoredProcedureQuery createNamedStoredProcedureQuery(String name)  {
        StoredProcedureQuery returnValue = null;

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.CREATE_NAMED_STORED_PROCEDURE_QUERY);
            }
            EntityManager delegate = _getDelegate();
            returnValue = delegate.createNamedStoredProcedureQuery(name);

            if( getCurrentTransaction() == null ) {
                returnValue = StoreProcedureQueryWrapper.createQueryWrapper(returnValue, delegate);
            }
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
        return returnValue;
    }

    @Override
    public StoredProcedureQuery createStoredProcedureQuery(String procedureName) {
        StoredProcedureQuery returnValue = null;

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.CREATE_STORED_PROCEDURE_QUERY);
            }
            EntityManager delegate = _getDelegate();
            returnValue = delegate.createStoredProcedureQuery(procedureName);

            if( getCurrentTransaction() == null ) {
                returnValue = StoreProcedureQueryWrapper.createQueryWrapper(returnValue, delegate);
            }
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
        return returnValue;
    }

    @Override
    public StoredProcedureQuery createStoredProcedureQuery(String procedureName, Class... resultClasses) {
        StoredProcedureQuery returnValue = null;

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.CREATE_STORED_PROCEDURE_QUERY_STRING_CLASS);
            }
            EntityManager delegate = _getDelegate();
            returnValue = delegate.createStoredProcedureQuery(procedureName, resultClasses);

            if( getCurrentTransaction() == null ) {
                returnValue = StoreProcedureQueryWrapper.createQueryWrapper(returnValue, delegate);
            }
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
        return returnValue;
    }

    @Override
    public StoredProcedureQuery createStoredProcedureQuery(String procedureName, String... resultSetMappings) {
        StoredProcedureQuery returnValue = null;

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.CREATE_STORED_PROCEDURE_QUERY_STRING_STRING);
            }
            EntityManager delegate = _getDelegate();
            returnValue = delegate.createStoredProcedureQuery(procedureName, resultSetMappings);

            if( getCurrentTransaction() == null ) {
                returnValue = StoreProcedureQueryWrapper.createQueryWrapper(returnValue, delegate);
            }
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
        return returnValue;
    }

    public Query createQuery(CriteriaUpdate updateQuery) {
        // Unlike other create*Query() methods, there is not need to create a QueryWrapper for non trnsactional case here.
        // QueryWrappers are created to ensure that entities returned by query are detatched. This is an update query and thus will not return any entities.
        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.CREATE_QUERY_CRITERIA_UPDATE);
            }
            return _getDelegate().createQuery(updateQuery);
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
    }

    @Override
    public Query createQuery(CriteriaDelete deleteQuery) {
        // Please refer to comments in method createQuery(CriteriaUpdate) for the reason we do not create a QueryWrapper here.
        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.CREATE_QUERY_CRITERIA_DELETE);
            }
            return _getDelegate().createQuery(deleteQuery);
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
    }

    @Override
    public boolean isJoinedToTransaction() {
        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.IS_JOINED_TO_TRANSACTION);
            }
            return _getDelegate().isJoinedToTransaction();
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
    }

    @Override
    public <T> EntityGraph<T> createEntityGraph(Class<T> rootType) {
        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.CREATE_ENTITY_GRAPH_CLASS);
            }
            return _getDelegate().createEntityGraph(rootType);
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
    }

    @Override
    public EntityGraph<?> createEntityGraph(String graphName) {
        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.CREATE_ENTITY_GRAPH_STRING);
            }
            return _getDelegate().createEntityGraph(graphName);
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
    }

    @Override
    public EntityGraph<?> getEntityGraph(String graphName) {
        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.GET_ENTITY_GRAPH);
            }
            return _getDelegate().getEntityGraph(graphName);
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
    }

    @Override
    public <T> List<EntityGraph<? super T>> getEntityGraphs(Class<T> entityClass) {
        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodStart(EntityManagerMethod.GET_ENTITY_GRAPHS);
            }
            return _getDelegate().getEntityGraphs(entityClass);
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerMethodEnd();
            }
        }
     }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();

        //Initialize the transients that were passed at ctor.
        ServiceLocator defaultServiceLocator = Globals.getDefaultHabitat();
        txManager     = defaultServiceLocator.getService(TransactionManager.class);
        invMgr        = defaultServiceLocator.getService(InvocationManager.class);
        compEnvMgr    = defaultServiceLocator.getService(ComponentEnvManager.class);
        callFlowAgent = defaultServiceLocator.getService(CallFlowAgent.class);
    }

    private void clearPersistenceContextPerhaps(EntityManager em) {
        if(getCurrentTransaction() == null && contextType != PersistenceContextType.EXTENDED) {
            em.clear();
        }
    }


    public static PhysicalEntityManagerWrapper getExtendedEntityManager(JavaEETransaction transaction, EntityManagerFactory factory) {
        return (PhysicalEntityManagerWrapper)transaction.getExtendedEntityManagerResource(factory);
    }

    public static PhysicalEntityManagerWrapper  getTxEntityManager(JavaEETransaction transaction, EntityManagerFactory factory) {
        return (PhysicalEntityManagerWrapper) transaction.getTxEntityManagerResource(factory);

    }

    @Service
    /**
     * NonTxEMs are saved as payload in current invocation. Clean them up at end of any component invocation
     */
    public static class NonTxEMCleaner implements ComponentInvocationHandler {

        @Override
        public void beforePreInvoke(ComponentInvocation.ComponentInvocationType invType, ComponentInvocation prevInv, ComponentInvocation newInv) throws InvocationException { }

        @Override
        public void afterPreInvoke(ComponentInvocation.ComponentInvocationType invType, ComponentInvocation prevInv, ComponentInvocation curInv) throws InvocationException { }

        @Override
        public void beforePostInvoke(ComponentInvocation.ComponentInvocationType invType, ComponentInvocation prevInv, ComponentInvocation curInv) throws InvocationException {
            // Close all the NonTxEMs for current invocation
            Map<EntityManagerFactory, EntityManager> nonTxEMs = getNonTxEMsFromCurrentInvocation(curInv);

            if(nonTxEMs != null) {
                for (EntityManager nonTxEM : nonTxEMs.values()) {
                    nonTxEM.close();
                }
            }
        }

        @Override
        public void afterPostInvoke(ComponentInvocation.ComponentInvocationType invType, ComponentInvocation prevInv, ComponentInvocation curInv) throws InvocationException { }
    }

}

/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.container.common.spi.util.CallFlowAgent;
import com.sun.enterprise.container.common.spi.util.EntityManagerQueryMethod;
import com.sun.enterprise.container.common.impl.util.DummyCallFlowAgentImpl;

import javax.persistence.*;
import java.util.*;

/**
 * Wrapper class for javax.persistence.Query objects returned from
 * non-transactional access of a container-managed transactional
 * EntityManager.  Proxying the Query object prevents the 
 * EntityManagerWrapper from having to keep a physical EntityManager
 * open when returning Query objects for non-transactional access.
 *
 * This results in a cleaner implementation of the non-transactional
 * EntityManager behavior and minimizes the amount of time 
 * non-transactional EntityManager objects are left open.  It is likely
 * that physical EntityManager objects will have heavy-weight resources
 * such as DB connections open even after clear() is called.  This is
 * one of the main reasons to minimize the number of open non-transactional
 * EntityManager objects held internally within injected/looked-up
 * container-managed EntityManagers.  
 *
 * The EntityManager and Query delegate objects are provided at
 * QueryWrapper creation time.  These objects must exist in order
 * for the EntityManagerWrapper to provide the correct exception
 * behavior to the application when a Query is requested.   
 * Likewise, the actual delegates must be available
 * to handle the majority of the Query API operations such as
 * performing validation on the various setter parameters.  
 * 
 * The Query/EntityManager delegates
 * are closed/discarded after each call to getSingleResult/getResultList.
 * A new Query/EntityManager delegate pair is then created lazily
 * the next time the Query delegate is needed.  The QueryWrapper
 * maintains a list of all setter operations invoked by the application.
 * These are re-applied in the same order whenever a new Query delegate
 * is created to ensure that the state of the Query delegate object matches
 * what it would have been if there wasn't any QueryWrapper.   
 * 
 */
public class QueryWrapper implements Query {

    private transient CallFlowAgent callFlowAgent;

    // Holds current query/em delegates.  These are cleared out after
    // query execution to minimize potential entity manager resource leakage.
    private Query queryDelegate;
    private EntityManager entityManagerDelegate;

    // Used if new query/em delegates need to be created.
    private EntityManagerFactory entityMgrFactory;
    private Map entityMgrProperties;

    // State used to construct query delegate object itself.
    private QueryType queryType;
    private String queryString;
    private Class queryResultClass;
    private String queryResultSetMapping;

    // State used to recreate sequence of setter methods applied to the
    // QueryWrapper when a new Query delegate is created.
    private List<SetterData> setterInvocations;


    public static Query createQueryWrapper(EntityManagerFactory emf, 
                                           Map emProperties, 
                                           EntityManager emDelegate,
                                           Query queryDelegate,
                                           String ejbqlString) {

        return new QueryWrapper(emf, emProperties, emDelegate,
                                queryDelegate, QueryType.EJBQL,
                                ejbqlString, null, null);
    }

    public static Query createNamedQueryWrapper(EntityManagerFactory emf, 
                                                Map emProperties, 
                                                EntityManager emDelegate,
                                                Query queryDelegate,
                                                String name) {
        return new QueryWrapper(emf, emProperties, emDelegate,
                                queryDelegate, QueryType.NAMED,
                                name, null, null);
    }

    public static Query createNativeQueryWrapper(EntityManagerFactory emf, 
                                                 Map emProperties, 
                                                 EntityManager emDelegate,
                                                 Query queryDelegate,
                                                 String sqlString) {

        return new QueryWrapper(emf, emProperties, emDelegate,
                                queryDelegate, QueryType.NATIVE,
                                sqlString, null, null);
        
    }

    public static Query createNativeQueryWrapper(EntityManagerFactory emf, 
                                                 Map emProperties, 
                                                 EntityManager emDelegate,
                                                 Query queryDelegate,
                                                 String sqlString,
                                                 Class resultClass) {

        return new QueryWrapper(emf, emProperties, emDelegate,
                                queryDelegate, QueryType.NATIVE,
                                sqlString, resultClass, null);
        
    }

    public static Query createNativeQueryWrapper(EntityManagerFactory emf, 
                                                 Map emProperties, 
                                                 EntityManager emDelegate,
                                                 Query queryDelegate,
                                                 String sqlString,
                                                 String resultSetMapping) {

        return new QueryWrapper(emf, emProperties, emDelegate,
                                queryDelegate, QueryType.NATIVE,
                                sqlString,  null, resultSetMapping);
        
    }

    protected QueryWrapper(EntityManagerFactory emf, Map emProperties,
                         EntityManager emDelegate, Query qDelegate,
                         QueryType type, String query,
                         Class resultClass, String resultSetMapping)
    {
        entityMgrFactory = emf;
        entityMgrProperties = emProperties;

        entityManagerDelegate = emDelegate;
        queryDelegate = qDelegate;

        queryType = type;
        queryString = query;
        queryResultClass = resultClass;
        queryResultSetMapping = resultSetMapping;

        setterInvocations = new LinkedList<SetterData>();

        callFlowAgent = new DummyCallFlowAgentImpl();    //TODO get it from ContainerUtil
    }


    public List getResultList() {
        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerQueryStart(EntityManagerQueryMethod.GET_RESULT_LIST);
            }
            Query delegate = getQueryDelegate();
            return delegate.getResultList();
        } finally {
            clearDelegates();
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerQueryEnd();
            }
        }
    }

    public Object getSingleResult() {
        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerQueryStart(EntityManagerQueryMethod.GET_SINGLE_RESULT);
            }
            Query delegate = getQueryDelegate();
            return delegate.getSingleResult();

        } finally {
            clearDelegates();
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerQueryEnd();
            }
        }
    }

    public int executeUpdate() {
        if(callFlowAgent.isEnabled()) {
            callFlowAgent.entityManagerQueryStart(EntityManagerQueryMethod.EXECUTE_UPDATE);
            callFlowAgent.entityManagerQueryEnd();
        }
        throw new TransactionRequiredException("executeUpdate is not supported for a Query object obtained through non-transactional access of a container-managed transactional EntityManager");
    }

    public Query setMaxResults(int maxResults) {
        
        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerQueryStart(EntityManagerQueryMethod.SET_MAX_RESULTS);
            }
            if( maxResults < 0 ) {
                throw new IllegalArgumentException("maxResult cannot be negative");
            }
            
            Query delegate = getQueryDelegate();
            delegate.setMaxResults(maxResults);
            
            SetterData setterData = SetterData.createMaxResults(maxResults);
            setterInvocations.add(setterData);
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerQueryEnd();
            }
        }
        
        return this;
    }

    public int getMaxResults() {

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerQueryStart(EntityManagerQueryMethod.GET_MAX_RESULTS);
            }

            Query delegate = getQueryDelegate();
            return delegate.getMaxResults();

        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerQueryEnd();
            }
        }
    }

    public Query setFirstResult(int startPosition) {
        
        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerQueryStart(EntityManagerQueryMethod.SET_FIRST_RESULT);
            }
            if( startPosition < 0 ) {
                throw new IllegalArgumentException
                        ("startPosition cannot be negative");
            }
            
            Query delegate = getQueryDelegate();
            delegate.setFirstResult(startPosition);
            
            SetterData setterData = SetterData.createFirstResult(startPosition);
            setterInvocations.add(setterData);
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerQueryEnd();
            }
        }
        
        return this;
    }

    public int getFirstResult() {

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerQueryStart(EntityManagerQueryMethod.GET_FIRST_RESULT);
            }

            Query delegate = getQueryDelegate();
            return delegate.getFirstResult();

        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerQueryEnd();
            }
        }
    }

    public Query setHint(String hintName, Object value) {
        
        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerQueryStart(EntityManagerQueryMethod.SET_HINT);
            }
            Query delegate = getQueryDelegate();
            delegate.setHint(hintName, value);
            
            SetterData setterData = SetterData.createHint(hintName, value);
            setterInvocations.add(setterData);
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerQueryEnd();
            }
        }
        
        return this;
    }

    public Map<String, Object> getHints() {

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerQueryStart(EntityManagerQueryMethod.GET_HINTS);
            }

            Query delegate = getQueryDelegate();
            return delegate.getHints();

        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerQueryEnd();
            }
        }
    }

    public <T> Query setParameter(Parameter<T> param, T value) {

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerQueryStart(EntityManagerQueryMethod.SET_PARAMETER_PARAMETER_OBJECT);
            }
            Query delegate = getQueryDelegate();
            delegate.setParameter(param, value);

            SetterData setterData = SetterData.createParameter(param, value);
            setterInvocations.add(setterData);
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerQueryEnd();
            }
        }

        return this;
    }

    public Query setParameter(Parameter<Date> param, Date value,  TemporalType temporalType) {

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerQueryStart(EntityManagerQueryMethod.SET_PARAMETER_PARAMETER_DATE_TEMPORAL_TYPE);
            }
            Query delegate = getQueryDelegate();
            delegate.setParameter(param, value, temporalType);

            SetterData setterData = SetterData.createParameter(param, value, temporalType);
            setterInvocations.add(setterData);
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerQueryEnd();
            }
        }

        return this;
    }

    public Query setParameter(Parameter<Calendar> param, Calendar value,  TemporalType temporalType) {

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerQueryStart(EntityManagerQueryMethod.SET_PARAMETER_PARAMETER_CALENDAR_TEMPORAL_TYPE);
            }
            Query delegate = getQueryDelegate();
            delegate.setParameter(param, value, temporalType);

            SetterData setterData = SetterData.createParameter(param, value, temporalType);
            setterInvocations.add(setterData);
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerQueryEnd();
            }
        }

        return this;
    }



    public Query setParameter(String name, Object value) {
        
        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerQueryStart(EntityManagerQueryMethod.SET_PARAMETER_STRING_OBJECT);
            }
            Query delegate = getQueryDelegate();
            delegate.setParameter(name, value);
            
            SetterData setterData = SetterData.createParameter(name, value);
            setterInvocations.add(setterData);
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerQueryEnd();
            }
        }
        
        return this;
    }

    public Query setParameter(String name, Date value, 
                              TemporalType temporalType) {
        
        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerQueryStart(EntityManagerQueryMethod.SET_PARAMETER_STRING_DATE_TEMPORAL_TYPE);
            }
            Query delegate = getQueryDelegate();
            delegate.setParameter(name, value, temporalType);
            
            SetterData setterData = SetterData.createParameter(name, value,
                    temporalType);
            setterInvocations.add(setterData);
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerQueryEnd();
            }
        }
        return this;
    }

    public Query setParameter(String name, Calendar value, 
                              TemporalType temporalType) {
        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerQueryStart(EntityManagerQueryMethod.SET_PARAMETER_STRING_CALENDAR_TEMPORAL_TYPE);
            }
            Query delegate = getQueryDelegate();
            delegate.setParameter(name, value, temporalType);
            
            SetterData setterData = SetterData.createParameter(name, value,
                    temporalType);
            setterInvocations.add(setterData);
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerQueryEnd();
            }
        }
        return this;
    }

    public Query setParameter(int position, Object value) {
        
        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerQueryStart(EntityManagerQueryMethod.SET_PARAMETER_INT_OBJECT);
            }
        Query delegate = getQueryDelegate();
        delegate.setParameter(position, value);

        SetterData setterData = SetterData.createParameter(position, value);
        setterInvocations.add(setterData);
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerQueryEnd();
            }
        }

        return this;
    }

    public Query setParameter(int position, Date value, 
                              TemporalType temporalType) {
        
        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerQueryStart(EntityManagerQueryMethod.SET_PARAMETER_INT_DATE_TEMPORAL_TYPE);
            }
            Query delegate = getQueryDelegate();
            delegate.setParameter(position, value, temporalType);
            
            SetterData setterData = SetterData.createParameter(position, value,
                    temporalType);
            setterInvocations.add(setterData);
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerQueryEnd();
            }
        }
        
        return this;
    }

    public Query setParameter(int position, Calendar value, 
                              TemporalType temporalType) {
        
        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerQueryStart(EntityManagerQueryMethod.SET_PARAMETER_INT_CALENDAR_TEMPORAL_TYPE);
            }
            Query delegate = getQueryDelegate();
            delegate.setParameter(position, value, temporalType);
            
            SetterData setterData = SetterData.createParameter(position, value,
                    temporalType);
            setterInvocations.add(setterData);
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerQueryEnd();
            }
        }
        
        return this;
    }

    public Set<Parameter<?>> getParameters() {

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerQueryStart(EntityManagerQueryMethod.GET_PARAMETERS);
            }

            Query delegate = getQueryDelegate();
            return delegate.getParameters();

        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerQueryEnd();
            }
        }
    }

    public Parameter<?> getParameter(String name) {

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerQueryStart(EntityManagerQueryMethod.GET_PARAMETER_NAME);
            }

            Query delegate = getQueryDelegate();
            return delegate.getParameter(name);

        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerQueryEnd();
            }
        }
    }

    public <T> Parameter<T> getParameter(String name, Class<T> type) {

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerQueryStart(EntityManagerQueryMethod.GET_PARAMETER_NAME_TYPE);
            }

            Query delegate = getQueryDelegate();
            return delegate.getParameter(name, type);

        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerQueryEnd();
            }
        }
    }

    public Parameter<?> getParameter(int position) {

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerQueryStart(EntityManagerQueryMethod.GET_PARAMETER_POSITION);
            }

            Query delegate = getQueryDelegate();
            return delegate.getParameter(position);

        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerQueryEnd();
            }
        }
    }

    public <T> Parameter<T> getParameter(int position, Class<T> type)  {

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerQueryStart(EntityManagerQueryMethod.GET_PARAMETER_POSITION_CLASS);
            }

            Query delegate = getQueryDelegate();
            return delegate.getParameter(position, type);

        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerQueryEnd();
            }
        }
    }

    public boolean isBound(Parameter<?> param) {

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerQueryStart(EntityManagerQueryMethod.IS_BOUND_PARAMETER);
            }

            Query delegate = getQueryDelegate();
            return delegate.isBound(param);

        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerQueryEnd();
            }
        }
    }

    public <T> T getParameterValue(Parameter<T> param) {

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerQueryStart(EntityManagerQueryMethod.GET_PARAMETER_VALUE_PARAMETER);
            }

            Query delegate = getQueryDelegate();
            return delegate.getParameterValue(param);

        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerQueryEnd();
            }
        }
    }

    public Object getParameterValue(String name) {

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerQueryStart(EntityManagerQueryMethod.GET_PARAMETER_VALUE_STRING);
            }

            Query delegate = getQueryDelegate();
            return delegate.getParameterValue(name);

        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerQueryEnd();
            }
        }
    }

    public Object getParameterValue(int position) {

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerQueryStart(EntityManagerQueryMethod.GET_PARAMETER_VALUE_INT);
            }

            Query delegate = getQueryDelegate();
            return delegate.getParameterValue(position);

        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerQueryEnd();
            }
        }
    }

    public Query setFlushMode(FlushModeType flushMode) {
        
        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerQueryStart(EntityManagerQueryMethod.SET_FLUSH_MODE);
            }
            Query delegate = getQueryDelegate();
            delegate.setFlushMode(flushMode);
            
            SetterData setterData = SetterData.createFlushMode(flushMode);
            setterInvocations.add(setterData);
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerQueryEnd();
            }
        }
        
        return this;
    }

    public FlushModeType getFlushMode() {

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerQueryStart(EntityManagerQueryMethod.GET_FLUSH_MODE);
            }

            Query delegate = getQueryDelegate();
            return delegate.getFlushMode();

        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerQueryEnd();
            }
        }
    }

    public Query setLockMode(LockModeType lockModeType) {
        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerQueryStart(EntityManagerQueryMethod.SET_LOCK_MODE);
            }
            Query delegate = getQueryDelegate();
            delegate.setLockMode(lockModeType);

            SetterData setterData = SetterData.createLockMode(lockModeType);
            setterInvocations.add(setterData);
        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerQueryEnd();
            }
        }

        return this;
    }

    public LockModeType getLockMode() {

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerQueryStart(EntityManagerQueryMethod.GET_LOCK_MODE);
            }

            Query delegate = getQueryDelegate();
            return delegate.getLockMode();

        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerQueryEnd();
            }
        }
    }

    public <T> T unwrap(Class<T> tClass) {

        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerQueryStart(EntityManagerQueryMethod.UNWRAP);
            }

            Query delegate = getQueryDelegate();
            return delegate.unwrap(tClass);

        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerQueryEnd();
            }
        }
    }

    private void clearDelegates() {

        queryDelegate = null;

        if( entityManagerDelegate != null ) {
            entityManagerDelegate.close();
            entityManagerDelegate = null;
        }
        
    }

    protected Query getQueryDelegate() {

        if( queryDelegate == null ) {

            entityManagerDelegate = 
                entityMgrFactory.createEntityManager(entityMgrProperties);

            queryDelegate = createQueryDelegate(queryType, entityManagerDelegate, queryString);

            // Now recreate the sequence of valid setter invocations applied 
            // to this query.
            for(SetterData setterData : setterInvocations) {
                setterData.apply(queryDelegate);
            }

        }


        return queryDelegate;

    }

    protected Query createQueryDelegate(QueryType queryType, EntityManager entityManager, String queryString) {
        Query retVal;
        switch(queryType) {

          case EJBQL :
              retVal = entityManager.createQuery(queryString);
              break;

          case NAMED :
              retVal = entityManager.createNamedQuery(queryString);
              break;

          case NATIVE :
              if( queryResultClass != null ) {
                  retVal = entityManager.createNativeQuery(queryString, queryResultClass);
              } else if( queryResultSetMapping != null ) {
                  retVal = entityManager.createNativeQuery(queryString, queryResultSetMapping);
              } else {
                  retVal = entityManager.createNativeQuery(queryString);
              }
              break;

          default:
              assert false : "The method is called with unexpected queryType."; 
              retVal = null;
        }
        return retVal;
    }


    protected enum QueryType {

        EJBQL,
        TYPED_EJBQL,
        NAMED,
        TYPED_NAMED,
        TYPED_CRITERIA,
        NATIVE

    }

    private enum SetterType {

        MAX_RESULTS,
        FIRST_RESULT,
        HINT,
        PARAM_PARAMETER_OBJECT,
        PARAM_PARAMETER_DATE_TEMPORAL_TYPE,
        PARAM_PARAMETER_CALENDAR_TEMPORAL_TYPE,
        PARAM_NAME_OBJECT,
        PARAM_NAME_DATE_TEMPORAL,
        PARAM_NAME_CAL_TEMPORAL,
        PARAM_POSITION_OBJECT,
        PARAM_POSITION_DATE_TEMPORAL,
        PARAM_POSITION_CAL_TEMPORAL,
        FLUSH_MODE,
        LOCK_MODE

    }

    private  static class  SetterData <T> {

        SetterType type;

        int int1;
        String string1;
        T object1;
        Parameter<T> parameter;

        Date date;
        Calendar calendar;
        TemporalType temporalType;

        FlushModeType flushMode;

        LockModeType lockMode;

        static SetterData createMaxResults(int maxResults) {
            SetterData data = new SetterData();
            data.type = SetterType.MAX_RESULTS;
            data.int1 = maxResults;
            return data;
        }

        static SetterData createFirstResult(int firstResult) {
            SetterData data = new SetterData();
            data.type = SetterType.FIRST_RESULT;
            data.int1 = firstResult;
            return data;
        }

        static SetterData createHint(String hintName, Object value) {
            SetterData data = new SetterData();
            data.type = SetterType.HINT;
            data.string1 = hintName;
            data.object1 = value;
            return data;
        }

        static <T> SetterData createParameter(Parameter<T> param, T value) {
            SetterData data = new SetterData<T>();
            data.type = SetterType.PARAM_PARAMETER_OBJECT;
            data.parameter = param;
            data.object1 = value;
            return data;
        }

        static SetterData<Date> createParameter(Parameter<Date> param, Date value, TemporalType temporalType) {
            SetterData<Date> data = new SetterData<Date>();
            data.type = SetterType.PARAM_PARAMETER_DATE_TEMPORAL_TYPE;
            data.parameter = param;
            data.object1 = value;
            data.temporalType = temporalType;
            return data;
        }

        static SetterData<Calendar> createParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType) {
            SetterData<Calendar> data = new SetterData<Calendar>();
            data.type = SetterType.PARAM_PARAMETER_CALENDAR_TEMPORAL_TYPE;
            data.parameter = param;
            data.object1 = value;
            data.temporalType = temporalType;
            return data;
        }

        static SetterData createParameter(String name, Object value) {

            SetterData data = new SetterData();
            data.type = SetterType.PARAM_NAME_OBJECT;
            data.string1 = name;
            data.object1 = value;
            return data;
        }

        static SetterData createParameter(String name, Date value,
                                          TemporalType temporalType) {

            SetterData data = new SetterData();
            data.type = SetterType.PARAM_NAME_DATE_TEMPORAL;
            data.string1 = name;
            data.date = value;
            data.temporalType = temporalType;
            return data;
        }

        static SetterData createParameter(String name, Calendar value,
                                          TemporalType temporalType) {

            SetterData data = new SetterData();
            data.type = SetterType.PARAM_NAME_CAL_TEMPORAL;
            data.string1 = name;
            data.calendar = value;
            data.temporalType = temporalType;
            return data;
        }
        
        static SetterData createParameter(int position, Object value) {

            SetterData data = new SetterData();
            data.type = SetterType.PARAM_POSITION_OBJECT;
            data.int1 = position;
            data.object1 = value;
            return data;
        }

        static SetterData createParameter(int position, Date value, 
                                          TemporalType temporalType) {
            SetterData data = new SetterData();
            data.type = SetterType.PARAM_POSITION_DATE_TEMPORAL;
            data.int1 = position;
            data.date = value;
            data.temporalType = temporalType;
            return data;
        }

       static SetterData createParameter(int position, Calendar value, 
                                          TemporalType temporalType) {
            SetterData data = new SetterData();
            data.type = SetterType.PARAM_POSITION_CAL_TEMPORAL;
            data.int1 = position;
            data.calendar = value;
            data.temporalType = temporalType;
            return data;
        } 
        
        static SetterData createFlushMode(FlushModeType flushMode) {

            SetterData data = new SetterData();
            data.type = SetterType.FLUSH_MODE;
            data.flushMode = flushMode;
            return data;

        }
        
        static SetterData createLockMode(LockModeType lockMode) {

            SetterData data = new SetterData();
            data.type = SetterType.LOCK_MODE;
            data.lockMode = lockMode;
            return data;

        }

        void apply(Query query) {

            switch(type) {

            case MAX_RESULTS :
                
                query.setMaxResults(int1);
                break;

            case FIRST_RESULT :
                
                query.setFirstResult(int1);
                break;

            case HINT :
                
                query.setHint(string1, object1);
                break;

            case PARAM_PARAMETER_OBJECT :

                query.setParameter(parameter, object1);
                break;

            case PARAM_PARAMETER_DATE_TEMPORAL_TYPE:

                query.setParameter((Parameter<Date>) parameter, (Date)object1, temporalType);
                break;

            case PARAM_PARAMETER_CALENDAR_TEMPORAL_TYPE:

                query.setParameter((Parameter<Calendar>) parameter, (Calendar) object1, temporalType);
                break;

            case PARAM_NAME_OBJECT :
                
                query.setParameter(string1, object1);
                break;

            case PARAM_NAME_DATE_TEMPORAL :
                
                query.setParameter(string1, date, temporalType);
                break;

            case PARAM_NAME_CAL_TEMPORAL :
                
                query.setParameter(string1, calendar, temporalType);
                break;

            case PARAM_POSITION_OBJECT :
                
                query.setParameter(int1, object1);
                break;

            case PARAM_POSITION_DATE_TEMPORAL :
                
                query.setParameter(int1, date, temporalType);
                break;

            case PARAM_POSITION_CAL_TEMPORAL :
                
                query.setParameter(int1, calendar, temporalType);
                break;

            case FLUSH_MODE :
                
                query.setFlushMode(flushMode);
                break;

            case LOCK_MODE :

                    query.setLockMode(lockMode);
                    break;

             default :

                 assert (false) : "A new value has been added to enum SetterType. Please add a case clause in this method to handle it ";
                 throw new RuntimeException("A new value has been added to enum SetterType without being coded in apply.");
            }
        }

    }
    

}

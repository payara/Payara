/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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
 * EntityManager.  Proxying the Query object allows us to clear persistence
 * context after execution to allow for returned objects to be detached
 *
 */
public class QueryWrapper <T extends Query> implements Query {

    private transient CallFlowAgent callFlowAgent;

    // Holds current query/em delegates.
    protected T queryDelegate;
    private EntityManager entityManagerDelegate;


    public static Query createQueryWrapper(Query queryDelegate, EntityManager emDelegate) {
        return new QueryWrapper<Query>(queryDelegate, emDelegate);
    }


    protected QueryWrapper(T qDelegate, EntityManager emDelegate)
    {
        queryDelegate = qDelegate;
        entityManagerDelegate = emDelegate;
        callFlowAgent = new DummyCallFlowAgentImpl();    //TODO get it from ContainerUtil
    }


    public List getResultList() {
        try {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerQueryStart(EntityManagerQueryMethod.GET_RESULT_LIST);
            }
            List retVal =  queryDelegate.getResultList();
            entityManagerDelegate.clear();
            return retVal;
        } finally {
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
            Object retVal =  queryDelegate.getSingleResult();
            entityManagerDelegate.clear();
            return retVal;
        } finally {
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
            
            queryDelegate.setMaxResults(maxResults);
            
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

            return queryDelegate.getMaxResults();

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
            
            queryDelegate.setFirstResult(startPosition);
            
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

            return queryDelegate.getFirstResult();

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
            queryDelegate.setHint(hintName, value);
            
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

            return queryDelegate.getHints();

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
            queryDelegate.setParameter(param, value);

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
            queryDelegate.setParameter(param, value, temporalType);

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
            queryDelegate.setParameter(param, value, temporalType);

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
            queryDelegate.setParameter(name, value);
            
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
            queryDelegate.setParameter(name, value, temporalType);
            
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
            queryDelegate.setParameter(name, value, temporalType);
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
        queryDelegate.setParameter(position, value);
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
            queryDelegate.setParameter(position, value, temporalType);
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
            queryDelegate.setParameter(position, value, temporalType);
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

            return queryDelegate.getParameters();

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

            return queryDelegate.getParameter(name);

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

            return queryDelegate.getParameter(name, type);

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

            return queryDelegate.getParameter(position);

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

            return queryDelegate.getParameter(position, type);

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

            return queryDelegate.isBound(param);

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

            return queryDelegate.getParameterValue(param);

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

            return queryDelegate.getParameterValue(name);

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

            return queryDelegate.getParameterValue(position);

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
            queryDelegate.setFlushMode(flushMode);
            
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

            return queryDelegate.getFlushMode();

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
            queryDelegate.setLockMode(lockModeType);

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

            return queryDelegate.getLockMode();

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

            return queryDelegate.unwrap(tClass);

        } finally {
            if(callFlowAgent.isEnabled()) {
                callFlowAgent.entityManagerQueryEnd();
            }
        }
    }


}

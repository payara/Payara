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

/*
 * EntityManagerMethod.java
 * $Id: EntityManagerMethod.java,v 1.2 2007/05/05 05:31:16 tcfujii Exp $
 * $Date: 2007/05/05 05:31:16 $
 * $Revision: 1.2 $
 */

package com.sun.enterprise.container.common.spi.util;

public enum EntityManagerMethod {

    PERSIST {
        @Override
        public String toString() {
            return "persist(Object entity)";
        }
    },
    
    MERGE {
        @Override
        public String toString() {
            return "merge(<T> entity)";
        }
    },
    
    REMOVE {
        @Override
        public String toString() {
            return "remove(Object entity)";
        }
    },
    
    FIND_CLASS_OBJECT_MAP {
        @Override
        public String toString() {
            return "find(Class<T> entityClass, Object primaryKey, Map<String, Object> properties)";
        }
    },

    FIND {
        @Override
        public String toString() {
            return "find(Class<T> entityClass, Object primaryKey)";
        }
    },

    FIND_CLASS_OBJECT_LOCKMODETYPE {
        @Override
        public String toString() {
            return "find(Class<T> entityClass, Object primaryKey, LockModeType lockMode)";
        }
    },

    FIND_CLASS_OBJECT_LOCKMODETYPE_PROPERTIES {
        @Override
        public String toString() {
            return "find(Class<T> entityClass, Object primaryKey, LockModeType lockMode, Map properties)";
        }
    },

    GET_REFERENCE {
        @Override
        public String toString() {
            return "getReference(Class<T> entityClass, Object primaryKey)";
        }
    },
    
    FLUSH {
        @Override
        public String toString() {
            return "flush()";
        }
    },
  
    SET_FLUSH_MODE {
        @Override
        public String toString() {
            return "setFlushMode(FlushModeType flushMode)";
        }
    },
    
    GET_FLUSH_MODE {
        @Override
        public String toString() {
            return "getFlushMode()";
        }
    },

    LOCK {
        @Override
        public String toString() {
            return "lock(Object entity, LockModeType lockMode)";
        }
    },

    LOCK_LOCKMODETYPE_MAP {
        @Override
        public String toString() {
            return "Object entity, LockModeType lockMode, Map properties";
        }
    },

    REFRESH {
        @Override
        public String toString() {
            return "refresh(Object entity)";
        }
    },

    REFRESH_OBJECT_PROPERTIES {
        @Override
        public String toString() {
            return "refresh(Object entity, Map<String, Object> properties)";
        }
    },

    REFRESH_OBJECT_LOCKMODETYPE {
        @Override
        public String toString() {
            return "refresh(Object entity, LockModeType lockMode)";
        }
    },

    REFRESH_OBJECT_LOCKMODETYPE_MAP {
        @Override
        public String toString() {
            return "refresh(Object entity, LockModeType lockMode, Map properties)";
        }
    },

    CLEAR {
        @Override
        public String toString() {
            return "clear()";
        }
    },

    CONTAINS {
        @Override
        public String toString() {
            return "contains(Object entity)";
        }
    },
    
    GET_LOCK_MODE {
        @Override
        public String toString() {
            return "getLockMode()";
        }
    },

    SET_PROPERTY {
        @Override
        public String toString() {
            return "settProperties())";
        }
    },

    GET_PROPERTIES {
        @Override
        public String toString() {
            return "getProperties())";
        }
    },

    CREATE_QUERY {
        @Override
        public String toString() {
            return "createQuery(String qlString)";
        }
    },
    
    CREATE_QUERY_STRING_CLASS {
        @Override
        public String toString() {
            return "createQuery(String qlString, Class<T> resultClass)";
        }
    },

    CREATE_QUERY_CRITERIA_QUERY {
        @Override
        public String toString() {
            return "createQuery(CriteriaQuery criteriaQuery)";
        }
    },

    CREATE_NAMED_QUERY {
        @Override
        public String toString() {
            return "createNamedQuery(String name)";
        }
    },
    
    CREATE_NAMED_QUERY_STRING_CLASS {
        @Override
        public String toString() {
            return "createNamedQuery(String name, Class<T> resultClass)";
        }
    },

    CREATE_NATIVE_QUERY_STRING {
        @Override
        public String toString() {
            return "createNativeQuery(String sqlString)";
        }
    },
    
    CREATE_NATIVE_QUERY_STRING_CLASS {
        @Override
        public String toString() {
            return "createNativeQuery(String sqlString, Class resultClass)";
        }
    },
    
    CREATE_NATIVE_QUERY_STRING_STRING {
        @Override
        public String toString() {
            return "createNativeQuery(String sqlString, String resultSetMapping)";
        }
    },
    
    JOIN_TRANSACTION {
        @Override
        public String toString() {
            return "joinTransaction()";
        }
    },

    GET_DELEGATE {
        @Override
        public String toString() {
            return "getDelegate()";
        }
    },

    CLOSE {
        @Override
        public String toString() {
            return "close()";
        }
    },
    
    IS_OPEN {
        @Override
        public String toString() {
            return "isOpen()";
        }
    },
    
    GET_TRANSACTION {
        @Override
        public String toString() {
            return "getTransaction()";
        }
    },

    GET_ENTITY_MANAGER_FACTORY {
        @Override
        public String toString() {
            return "getEntityManagerFactory()";
        }
    },

    GET_CRITERIA_BUILDER {
        @Override
        public String toString() {
            return "getCriteriaBuilder()";
        }
    },

    GET_METAMODEL {
        @Override
        public String toString() {
            return "getMetamodel()";
        }
    },

    DETATCH {
        @Override
        public String toString() {
            return "detatch()";
        }
    },

    UNWRAP {
        @Override
        public String toString() {
            return "unwrap()";
        }
    },

    CREATE_NAMED_STORED_PROCEDURE_QUERY  {
        @Override
        public String toString() {
            return "createNamedStoredProcedureQuery(String name)";
        }
    },

    CREATE_STORED_PROCEDURE_QUERY  {
        @Override
        public String toString() {
            return "createStoredProcedureQuery(String procedureName)";
        }
    },

    CREATE_STORED_PROCEDURE_QUERY_STRING_CLASS  {
        @Override
        public String toString() {
            return "createStoredProcedureQuery(String procedureName, Class... resultClasses)";
        }
    },

    CREATE_STORED_PROCEDURE_QUERY_STRING_STRING  {
        @Override
        public String toString() {
            return "createStoredProcedureQuery(String procedureName, String... resultSetMappings)";
        }
    },

    CREATE_QUERY_CRITERIA_UPDATE {
        @Override
        public String toString() {
            return "createQuery(CriteriaUpdate updateQuery)";
        }
    },

    CREATE_QUERY_CRITERIA_DELETE {
        @Override
        public String toString() {
            return "createQuery(CriteriaDelete deleteQuery)";
        }
    },

    IS_JOINED_TO_TRANSACTION  {
        @Override
        public String toString() {
            return "isJoinedToTransaction()";
        }
    },

    CREATE_ENTITY_GRAPH_CLASS  {
        @Override
        public String toString() {
            return "createEntityGraph(Class<T> rootType)";
        }
    },

    CREATE_ENTITY_GRAPH_STRING  {
        @Override
        public String toString() {
            return "createEntityGraph(String graphName)";
        }
    },

    GET_ENTITY_GRAPH  {
        @Override
        public String toString() {
            return "getEntityGraph(String graphName)";
        }
    },

    GET_ENTITY_GRAPHS  {
        @Override
        public String toString() {
            return "getEntityGraphs(Class<T> entityClass)";
        }
    }
}

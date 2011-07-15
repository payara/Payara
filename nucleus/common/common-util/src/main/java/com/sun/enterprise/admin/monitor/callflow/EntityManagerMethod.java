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

/*
 * EntityManagerMethod.java
 * $Id: EntityManagerMethod.java,v 1.1 2006/11/08 20:55:16 harpreet Exp $
 * $Date: 2006/11/08 20:55:16 $
 * $Revision: 1.1 $
 */

package	com.sun.enterprise.admin.monitor.callflow;

public enum EntityManagerMethod {

    PERSIST {
        public String toString() {
            return "persist(Object entity)";
        }
    },
    
    MERGE {
        public String toString() {
            return "merge(<T> entity)";
        }
    },
    
    REMOVE {
        public String toString() {
            return "remove(Object entity)";
        }
    },
    
    FIND {
        public String toString() {
            return "find(Class<T> entityClass, Object primaryKey)";
        }
    },

    GET_REFERENCE {
        public String toString() {
            return "getReference(Class<T> entityClass, Object primaryKey)";
        }
    },
    
    FLUSH {
        public String toString() {
            return "flush()";
        }
    },
  
    SET_FLUSH_MODE {
        public String toString() {
            return "setFlushMode(FlushModeType flushMode)";
        }
    },
    
    GET_FLUSH_MODE {
        public String toString() {
            return "getFlushMode()";
        }
    },

    LOCK {
        public String toString() {
            return "lock(Object entity, LockModeType lockMode)";
        }
    },

    REFRESH {
        public String toString() {
            return "refresh(Object entity)";
        }
    },
    
    CLEAR {
        public String toString() {
            return "clear()";
        }
    },

    CONTAINS {
        public String toString() {
            return "contains(Object entity)";
        }
    },
    
    CREATE_QUERY {
        public String toString() {
            return "createQuery(String qlString)";
        }
    },
    
    CREATE_NAMED_QUERY {
        public String toString() {
            return "createNamedQuery(String name)";
        }
    },
    
    CREATE_NATIVE_QUERY_STRING {
        public String toString() {
            return "createNativeQuery(String sqlString)";
        }
    },
    
    CREATE_NATIVE_QUERY_STRING_CLASS {
        public String toString() {
            return "createNativeQuery(String sqlString, Class resultClass)";
        }
    },
    
    CREATE_NATIVE_QUERY_STRING_STRING {
        public String toString() {
            return "createNativeQuery(String sqlString, String resultSetMapping)";
        }
    },
    
    JOIN_TRANSACTION {
        public String toString() {
            return "joinTransaction()";
        }
    },

    GET_DELEGATE {
        public String toString() {
            return "getDelegate()";
        }
    },

    CLOSE {
        public String toString() {
            return "close()";
        }
    },
    
    IS_OPEN {
        public String toString() {
            return "isOpen()";
        }
    },
    
    GET_TRANSACTION {
        public String toString() {
            return "getTransaction()";
        }
    }
    
}

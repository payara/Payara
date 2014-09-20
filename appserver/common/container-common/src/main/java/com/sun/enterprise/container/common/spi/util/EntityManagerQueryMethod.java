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
 * EntityManagerQueryMethod.java
 * $Id: EntityManagerQueryMethod.java,v 1.2 2007/05/05 05:31:16 tcfujii Exp $
 * $Date: 2007/05/05 05:31:16 $
 * $Revision: 1.2 $
 */

package com.sun.enterprise.container.common.spi.util;

public enum EntityManagerQueryMethod {
    
    GET_RESULT_LIST {
        public String toString() {
            return "getResultList()";
        }
    },
    
    GET_SINGLE_RESULT {
        public String toString() {
            return "getSingleResult()";
        }
    },
    
    EXECUTE_UPDATE {
        public String toString() {
            return "executeUpdate()";
        }
    },
    
    SET_MAX_RESULTS {
        public String toString() {
            return "setMaxResults(int maxResult)";
        }
    },
    
    GET_MAX_RESULTS {
        public String toString() {
            return "getMaxResults()";
        }
    },

    SET_FIRST_RESULT {
        public String toString() {
            return "setFirstResult(int startPosition)";
        }
    },

    GET_FIRST_RESULT {
        public String toString() {
            return "getFirstResult()";
        }
    },

    SET_HINT {
        public String toString() {
            return "setHint(String hintName, Object value)";
        }
    },
    
    GET_HINTS {
        public String toString() {
            return "getHints()";
        }
    },

    SET_PARAMETER_PARAMETER_OBJECT {
        public String toString() {
            return "setParameter(Parameter<T> param, T value)";
        }
    },

    SET_PARAMETER_PARAMETER_DATE_TEMPORAL_TYPE {
        public String toString() {
            return "setParameter(Parameter<Date> param, Date value,  TemporalType temporalType)";
        }
    },

    SET_PARAMETER_PARAMETER_CALENDAR_TEMPORAL_TYPE {
        public String toString() {
            return "setParameter(Parameter<Calendar> param, Calendar value,  TemporalType temporalType)";
        }
    },

    SET_PARAMETER_STRING_OBJECT {
        public String toString() {
            return "setParameter(String name, Object value)";
        }
    },
    
    SET_PARAMETER_STRING_DATE_TEMPORAL_TYPE {
        public String toString() {
            return "setParameter(String name, Date value, TemporalType temporalType)";
        }
    },
    
    SET_PARAMETER_STRING_CALENDAR_TEMPORAL_TYPE {
        public String toString() {
            return "setParameter(String name, Calendar value, TemporalType temporalType)";
        }
    },
    
    SET_PARAMETER_INT_OBJECT {
        public String toString() {
            return "setParameter(int position, Object value)";
        }
    },
    
    SET_PARAMETER_INT_DATE_TEMPORAL_TYPE {
        public String toString() {
            return "setParameter(int position, Date value, TemporalType temporalType)";
        }
    },
    
    SET_PARAMETER_INT_CALENDAR_TEMPORAL_TYPE {
        public String toString() {
            return "setParameter(int position, Calendar value, TemporalType temporalType)";
        }
    },
    
    GET_PARAMETERS {
        public String toString() {
            return "getParameter()";
        }
    },

    GET_PARAMETER_NAME {
        public String toString() {
            return "getParameter(String name)";
        }
    },

    GET_PARAMETER_NAME_TYPE {
        public String toString() {
            return "getParameter(String name, Class<T> type)";
        }
    },

    GET_PARAMETER_NAME_CLASS {
        public String toString() {
            return "getParameter(String name, Class<T> type)";
        }
    },

    GET_PARAMETER_POSITION {
        public String toString() {
            return "getParameter(int position)";
        }
    },

    GET_PARAMETER_POSITION_CLASS {
        public String toString() {
            return "getParameter(int position, Class<T> type)";
        }
    },

    IS_BOUND_PARAMETER {
        public String toString() {
            return "isBound(Parameter)";
        }
    },

    GET_PARAMETER_VALUE_PARAMETER {
        public String toString() {
            return "getParameterValue(Parameter)";
        }
    },

    GET_PARAMETER_VALUE_STRING {
        public String toString() {
            return "getParameterValue(String)";
        }
    },

    GET_PARAMETER_VALUE_INT {
        public String toString() {
            return "getParameterValue(int)";
        }
    },

    GET_FLUSH_MODE {
        public String toString() {
            return "getFlushMode()";
        }
    },

    SET_FLUSH_MODE {
        public String toString() {
            return "setFlushMode(FlushModeType flushMode)";
        }
    },

    SET_LOCK_MODE {
        public String toString() {
            return "setLock(LockModeType lockMode)";
        }
    },

    GET_LOCK_MODE {
        public String toString() {
            return "getLockMode()";
        }
    },

    UNWRAP {
        public String toString() {
            return "unwrap()";
        }
    }

}

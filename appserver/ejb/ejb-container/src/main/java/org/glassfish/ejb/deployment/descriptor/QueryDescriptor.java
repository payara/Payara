/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.ejb.deployment.descriptor;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.enterprise.deployment.MethodDescriptor;
import com.sun.enterprise.deployment.util.DOLUtils;
import org.glassfish.deployment.common.Descriptor;

/** 
 * This class contains information about EJB-QL queries for
 * finder/selector methods of EJB2.0 CMP EntityBeans.
 * It represents the <query> XML element.
 *
 * @author Sanjeev Krishnan
 */

public final class QueryDescriptor extends Descriptor {

    // For EJB2.0: the query is either a string in EJB-QL or is empty.
    // For EJB1.1: the query empty (only sql is available)
    private String query; 

    // SQL query corresponding to EJB-QL or English
    private String sql;   

    private MethodDescriptor methodDescriptor;
    private transient Method method;

    
    // Deployment information used to specify whether ejbs
    // returned by a select query should be materialized as
    // EJBLocalObject or EJBObject.  This property is optional
    // and is only applicable for ejbSelect methods that 
    // select ejbs.  

    private static final int NO_RETURN_TYPE_MAPPING = 0;
    private static final int RETURN_LOCAL_TYPES     = 1;
    private static final int RETURN_REMOTE_TYPES    = 2;

    private int returnTypeMapping;
    
    // Create logger object per Java SDK 1.4 to log messages
    // introduced Santanu De, Sun Microsystems, March 2002

    static Logger _logger = DOLUtils.getDefaultLogger();
	
    public QueryDescriptor()
    {
        this.query = null;
        this.sql = null;
        this.returnTypeMapping = NO_RETURN_TYPE_MAPPING;
    }

    public QueryDescriptor(QueryDescriptor otherQuery, Method m) {
        this.query = otherQuery.query;
        this.sql   = otherQuery.sql;
        this.method = m;
        this.returnTypeMapping = otherQuery.returnTypeMapping;
    }

/**
    public void setQueryMethod(MethodDescriptor md)
    {
	this.methodDescriptor = md;
    } 

    public MethodDescriptor getQueryMethod()
    {
	return methodDescriptor;
    } 
**/

    public void setQueryMethod(Method m)
    {
	this.method = m;
    } 

    public Method getQueryMethod()
    {
	return method;
    } 
    
    public void setQueryMethodDescriptor(MethodDescriptor m) {
        methodDescriptor = m;
    }
    
    public MethodDescriptor getQueryMethodDescriptor() {
        return methodDescriptor;
    }

    public boolean getIsEjbQl()
    {
        return (query != null);
    }

    /**
     * Set the EJB-QL query (ejb-ql XML element).  If query parameter
     * is null, or has no content, getIsEjbQl will return false.  
     * Otherwise, getIsEjbQl will return true.
     */
    public void setQuery(String query)
    {
         _logger.log(Level.FINE,"input query = '" + query + "'");
 
        String newQuery = (query != null) ? query.trim() : null;
        if( (newQuery != null) && newQuery.equals("") ) {
            newQuery = null;
        }
		if( newQuery == null ) {
			_logger.log(Level.FINE,"query has no content -- setting to NULL");
		} else {
			_logger.log(Level.FINE,"setting query to '" + newQuery + "'");
        }
        this.query = newQuery;
    }

    /**
     * Get the EJB-QL query (ejb-ql XML element)
     */
    public String getQuery()
    {
	return query;
    }

    public boolean getHasSQL() {
        return (this.sql != null);
    }

    public void setSQL(String sql)
    {
	this.sql = sql;
    }

    public String getSQL()
    {
	return sql;
    }


    // Returns true if no return type mapping has been specified
    public boolean getHasNoReturnTypeMapping() {
        return (returnTypeMapping == NO_RETURN_TYPE_MAPPING);
    }

    // Returns true only if a local return type has been specified.
    public boolean getHasLocalReturnTypeMapping() {
        return (returnTypeMapping == RETURN_LOCAL_TYPES);
    }

    // Returns true only if a remote return type has been specified.
    public boolean getHasRemoteReturnTypeMapping() {
        return (returnTypeMapping == RETURN_REMOTE_TYPES);
    }

    public void setHasNoReturnTypeMapping() {
        returnTypeMapping = NO_RETURN_TYPE_MAPPING;
    }

    public void setHasLocalReturnTypeMapping() {
        returnTypeMapping = RETURN_LOCAL_TYPES;
    }

    public void setHasRemoteReturnTypeMapping() {
        returnTypeMapping = RETURN_REMOTE_TYPES;
    }

    public int getReturnTypeMapping() {
        return returnTypeMapping;
    }
    
    public void print(StringBuffer toStringBuffer) {
        toStringBuffer.append("Query ");
        if(getQueryMethodDescriptor()  != null)
            getQueryMethodDescriptor().print(toStringBuffer);
        toStringBuffer.append("\n");
        if (getHasSQL()) {
            toStringBuffer.append("SQL : ").append(getSQL());
            return;
        } 
        if (getIsEjbQl()) {
            toStringBuffer.append("EJB QL: ").append(query);
            return;
        }
        toStringBuffer.append(" No query associated");
    }

}


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
 * JDOQLElements.java
 *
 * Created on November 12, 2001
 */

package com.sun.jdo.spi.persistence.support.ejb.ejbqlc;

import java.util.Properties;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.io.File;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;

/**
 * An JDOQLElements instance represents the result of the EJBQLC compile step.
 *
 * @author  Michael Bouschen
 * @author  Shing Wai Chan
 */
public class JDOQLElements
{
    /** The name of the candidate class */
    private String candidateClassName;

    /** The parameter declarations string. */
    private String parameters;

    /** The variable declarations string. */
    private String variables;

    /** The filter expression string. */
    private String filter;

    /** The ordering expression string. */
    private String ordering;

    /** The result expression. */
    private String result;

    /** The result type. */
    private String resultType;

    /** Flag indicating whether the result element is of a pc class. */
    private boolean isPCResult;

    /**
     *  Flag indicating whether the result element is associated to an
     *  aggregate function. 
     */
    private boolean isAggregate;

    /** String array contains ejb names corresponding to parameters */
    private String[] parameterEjbNames;

    /**
     * Constructor taking JDOQL elements.
     */
    public JDOQLElements(String candidateClassName,
                         String parameters,
                         String variables,
                         String filter,
                         String ordering,
                         String result,
                         String resultType,
                         boolean isPCResult,
                         boolean isAggregate,
                         String[] parameterEjbNames)
    {
        setCandidateClassName(candidateClassName);
        setParameters(parameters);
        setVariables(variables);
        setFilter(filter);
        setOrdering(ordering);
        setResult(result);
        setResultType(resultType);
        setPCResult(isPCResult);
        setAggregate(isAggregate);
        setParameterEjbNames(parameterEjbNames);
    }

    /** Returns the fully qulified name of the candidate class. */
    public String getCandidateClassName()
    {
        return this.candidateClassName;
    }

    /** Sets the fully qulified name of the candidate class. */
    public void setCandidateClassName(String candidateClassName)
    {
        // TBD: check non empty candidateClassName
        this.candidateClassName = candidateClassName;
    }

    /** Returns the parameter declaration string. */
    public String getParameters()
    {
        return parameters;
    }

    /** Sets the parameter declarations string. */
    public void setParameters(String parameters)
    {
        this.parameters = (parameters == null) ? "" : parameters; //NOI18N
    }

    /** Returns the variable declarations string. */
    public String getVariables()
    {
        return variables;
    }

    /** Sets the variable declarations string. */
    public void setVariables(String variables)
    {
        this.variables = (variables == null) ? "" : variables; //NOI18N
    }

    /** Returns the filter expression. */
    public String getFilter()
    {
        return filter;
    }

    /** Sets the filter expression. */
    public void setFilter(String filter)
    {
        this.filter = (filter == null) ? "" : filter; //NOI18N
    }

    /** Returns the ordering expression. */
    public String getOrdering()
    {
        return ordering;
    }

    /** Sets the ordering expression. */
    public void setOrdering(String ordering)
    {
        this.ordering = (ordering == null) ? "" : ordering; //NOI18N
    }

    /** Returns the result expression. */
    public String getResult()
    {
        return result;
    }

    /** Sets the result expression. */
    public void setResult(String result)
    {
        this.result = (result == null) ? "" : result; //NOI18N
    }

    /** 
     * Returns the result type. The result type is the name of the element type 
     * of the JDO query result set. 
     */
    public String getResultType()
    {
        return resultType;
    }

    /** 
     * Sets the result type. The result type is the name of the element type 
     * of the JDO query result set.
     */
    public void setResultType(String resultType)
    {
        this.resultType = resultType;
    }

    /**
     * Returns whether the result of the JDOQL query is a collection of pc
     * instances or not.
     */
    public boolean isPCResult()
    {
        return isPCResult;
    }

    /**
     * Sets whether the result of the JDOQL query is a collection of pc
     * instances or not.
     */
    public void setPCResult(boolean isPCResult)
    {
        this.isPCResult = isPCResult;
    }

    /**
     * Returns whether the result of the JDOQL query is associated to
     * an aggregate function.
     */
    public boolean isAggregate()
    {
        return isAggregate;
    }

    /**
     * Sets whether the result of the JDOQL query is a associated to
     * an aggregate function. 
     */
    public void setAggregate(boolean isAggregate)
    {
        this.isAggregate = isAggregate;
    }

    /**
     * Returns parameterEjbNames array
     */
    public String[] getParameterEjbNames()
    {
        return parameterEjbNames;
    }

    /**
     * set parameterEjbNames array
     */
    public void setParameterEjbNames(String[] parameterEjbNames)
    {
        this.parameterEjbNames = parameterEjbNames;
    }

    /** Returns a string representation of this JDOQLElements instance. */
    public String toString()
    {
        StringBuffer repr = new StringBuffer();
        repr.append("JDOQLElements("); //NOI18N
        repr.append("candidateClass: "); //NOI18N
        repr.append(candidateClassName);
        if (parameters != null && parameters.length() > 0) {
            repr.append(", parameters: "); //NOI18N 
            repr.append(parameters);
        }
        if (variables != null && variables.length() > 0) {
            repr.append(", variables: "); //NOI18N 
            repr.append(variables);
        }
        if (filter != null && filter.length() > 0) {
            repr.append(", filter: "); //NOI18N 
            repr.append(filter);
        }
        if (ordering != null && ordering.length() > 0) {
            repr.append(", ordering: "); //NOI18N 
            repr.append(ordering);
        }
        if (result != null && result.length() > 0) {
            repr.append(", result: "); //NOI18N 
            repr.append(result);
            repr.append(", resultType: "); //NOI18N 
            repr.append(resultType);
            repr.append(", isPCResult: "); //NOI18N 
            repr.append(isPCResult);
        }
        repr.append(", isAggregate: ");
        repr.append(isAggregate);
        if (parameterEjbNames != null && parameterEjbNames.length > 0) {
            repr.append(", parameterEjbNames: "); //NOI18N
            for (int i = 0; i < parameterEjbNames.length; i++) {
                repr.append(i);
                repr.append(": ");
                repr.append(parameterEjbNames[i]);
                repr.append(", ");
            }
        }
        repr.append(")"); //NOI18N
        return repr.toString();
    }
}

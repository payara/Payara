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

package org.glassfish.api.jdbc;

import java.io.Serializable;

/**
 * Information related to SQL operations executed by the applications are 
 * stored in this object. 
 * 
 * This trace record is used to log all the sql statements in a particular 
 * format.
 * 
 * @author Shalini M
 */
public class SQLTraceRecord implements Serializable {
    /**
     * Thread ID from which SQL statement originated.
     */
    private long threadID;
    
    /**
     * Thread Name from which SQL statement originated.
     */
    private String threadName;

    /**
     * Pool Name in which the SQL statement is executed.
     */
    private String poolName;
    
    /**
     * Type of SQL query. Could be PreparedStatement, CallableStatement or
     * other object types.
     */
    private String className;
    
    /**
     * Method that executed the query.
     */
    private String methodName;
    
    /**
     * Time of execution of query.
     */
    private long timeStamp;
    
    /**
     * Parameters of the method that executed the SQL query. Includes information
     * like SQL query, arguments and so on.
     */    
    private Object[] params;

    /**
     * Gets the class name of the SQL query expressed as a String.
     * 
     * @return The class name of the SQL query expressed as a String.
     */
    public String getClassName() {
        return className;
    }

    /**
     * Sets the class name of the SQL query expressed as a String.
     * 
     * @param className class name of the SQL query.
     */
    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * Gets the method name that executed the SQL query.
     * 
     * @return methodName that executed the SQL query.
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     * Sets the method name that executes the SQL query.
     * 
     * @param methodName that executes the SQL query.
     */
    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    /**
     * Gets the pool name in which the SQL statement is executed.
     * 
     * @return poolName in which the SQL statement is executed.
     */
    public String getPoolName() {
        return poolName;
    }

    /**
     * Sets the poolName in which the SQL statement is executed.
     * 
     * @param poolName in which the SQL statement is executed.
     */
    public void setPoolName(String poolName) {
        this.poolName = poolName;
    }

    /**
     * Gets the thread ID from which the SQL statement originated.
     * 
     * @return long threadID from which the SQL statement originated.
     */
    public long getThreadID() {
        return threadID;
    }

    /**
     * Sets the thread ID from which the SQL statement originated.
     * 
     * @param threadID from which the SQL statement originated.
     */    
    public void setThreadID(long threadID) {
        this.threadID = threadID;
    }

    /**
     * Gets the thread Name from which the SQL statement originated.
     * 
     * @return String threadName from which the SQL statement originated.
     */
    public String getThreadName() {
        return threadName;
    }

    /**
     * Sets the thread Name from which the SQL statement originated.
     * 
     * @param threadName from which the SQL statement originated.
     */    
    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    /**
     * Gets the time of execution of query.
     * 
     * @return long timeStamp of execution of query.
     */    
    public long getTimeStamp() {
        return timeStamp;
    }

    /**
     * Sets the time of execution of query.
     * 
     * @param timeStamp of execution of query.
     */        
    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    /**
     * Gets the parameters of the method that executed the SQL query. 
     * Includes information like SQL query, arguments and so on.
     * 
     * @return Object[] params method parameters that execute SQL query.
     */    
    public Object[] getParams() {
        return params;
    }
    
    /**
     * Sets the parameters of the method that executed the SQL query. 
     * Includes information like SQL query, arguments and so on.
     * 
     * @param params method parameters that execute SQL query.
     */    
    public void setParams(Object[] params) {
        this.params = params;
    }
    
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("ThreadID=" + getThreadID() + " | ");
        sb.append("ThreadName=" + getThreadName() + " | ");
        sb.append("TimeStamp=" + getTimeStamp() + " | ");
        sb.append("ClassName=" + getClassName() + " | ");
        sb.append("MethodName=" + getMethodName() + " | ");
        if(params != null && params.length > 0) {
            int index = 0;
            for(Object param : params) {
                sb.append("arg[" + index++ + "]=" + param.toString() + " | ");
            }
        }
        //TODO add poolNames and other fields of this record.
        return sb.toString();
    }
}

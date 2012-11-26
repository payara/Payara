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

package com.sun.enterprise.deployment;

import com.sun.enterprise.deployment.util.DOLUtils;
import java.util.Properties;
import java.util.logging.Level;
import java.sql.Connection;

import static org.glassfish.deployment.common.JavaEEResourceType.*;

/**
 * @author Jagadish Ramu
 */
public class DataSourceDefinitionDescriptor extends ResourceDescriptor {

    private String name ;
    private String description;
    private String className ;
    private int portNumber = -1;
    private String databaseName ;
    private String serverName = "localhost";
    private String url;
    private String user;
    private String password;
    private long loginTimeout =0 ;
    private boolean transactional = true;
    private int isolationLevel = -1;
    private int initialPoolSize =-1;
    private int maxPoolSize = -1;
    private int minPoolSize =-1;
    private long maxIdleTime=-1 ; //seconds / milliseconds ?
    private int maxStatements =-1;
    private Properties properties = new Properties();

    private boolean transactionSet = false;
    private boolean loginTimeoutSet = false; 
    private boolean serverNameSet = false;



    private boolean deployed = false;

    private static final String JAVA_URL = "java:";
    private static final String JAVA_COMP_URL = "java:comp/";

    //represents the valid values for isolation-level in Deployment Descriptors
    private static final String TRANSACTION_NONE = "TRANSACTION_NONE";
    private static final String TRANSACTION_READ_UNCOMMITTED = "TRANSACTION_READ_UNCOMMITTED";
    private static final String TRANSACTION_READ_COMMITTED = "TRANSACTION_READ_COMMITTED";
    private static final String TRANSACTION_REPEATABLE_READ = "TRANSACTION_REPEATABLE_READ";
    private static final String TRANSACTION_SERIALIZABLE = "TRANSACTION_SERIALIZABLE";

    public DataSourceDefinitionDescriptor(){
        super.setResourceType(DSD);
    }

    public void setDeployed(boolean deployed){
        this.deployed = deployed;
    }

    public boolean isDeployed(){
        return deployed;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUrl(String Url){
        this.url = Url;
    }

    public String getUrl(){
        return url;
    }
    public String getDescription() {
        return description;
    }

    public void setTransactionSet(boolean value){
        this.transactionSet = value;
    }

    public boolean isTransactionSet(){
        return transactionSet;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public int getPortNumber() {
        return portNumber;
    }

    public void setPortNumber(int portNumber) {
        this.portNumber = portNumber;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
        setServerNameSet(true);
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public long getLoginTimeout() {
        return loginTimeout;
    }

    public void setLoginTimeout(String loginTimeout) {
        try{
            this.loginTimeout = Long.parseLong(loginTimeout);
            setLoginTimeoutSet(true);
        }catch(NumberFormatException nfe){
            DOLUtils.getDefaultLogger().log(Level.WARNING, "invalid loginTimeout value [ " + loginTimeout+ " ]," +
                    " required long");
        }
    }

    private void setLoginTimeoutSet(boolean b) {
        loginTimeoutSet = b;
    }

    public boolean isLoginTimeoutSet(){
        return loginTimeoutSet;
    }

    public boolean isServerNameSet(){
        return serverNameSet;
    }

    private void setServerNameSet(boolean b){
        this.serverNameSet = b;
    }

    public boolean isTransactional() {
        return transactional;
    }

    public void setTransactional(boolean transactional) {
        this.transactional = transactional;
        setTransactionSet(true);
    }

    //DD specified Enumeration values are String
    //Annotation uses integer values and hence this mapping is needed
    public String getIsolationLevelString(){
        String isolationLevelString = null;
        if(isolationLevel == Connection.TRANSACTION_READ_COMMITTED){
            isolationLevelString = TRANSACTION_READ_COMMITTED;
        }else if (isolationLevel == Connection.TRANSACTION_READ_UNCOMMITTED){
            isolationLevelString = TRANSACTION_READ_UNCOMMITTED;
        }else if (isolationLevel == Connection.TRANSACTION_REPEATABLE_READ){
            isolationLevelString = TRANSACTION_REPEATABLE_READ;
        }else if (isolationLevel == Connection.TRANSACTION_SERIALIZABLE){
            isolationLevelString = TRANSACTION_SERIALIZABLE;
        }
        return isolationLevelString;
    }
    public int getIsolationLevel() {
        return isolationLevel;
    }

    //DD specified Enumeration values are String
    //Annotation uses integer values and hence this mapping is needed
    public void setIsolationLevel(String isolationLevelString) {

        if (!isIntegerIsolationLevelSet(isolationLevelString)) {

            if (isolationLevelString.equals(TRANSACTION_READ_COMMITTED)) {
                this.isolationLevel = Connection.TRANSACTION_READ_COMMITTED;
            } else if (isolationLevelString.equals(TRANSACTION_READ_UNCOMMITTED)) {
                this.isolationLevel = Connection.TRANSACTION_READ_UNCOMMITTED;
            } else if (isolationLevelString.equals(TRANSACTION_REPEATABLE_READ)) {
                this.isolationLevel = Connection.TRANSACTION_REPEATABLE_READ;
            } else if (isolationLevelString.equals(TRANSACTION_SERIALIZABLE)) {
                this.isolationLevel = Connection.TRANSACTION_SERIALIZABLE;
            } else {
                throw new IllegalStateException
                        ("Isolation level [ " + isolationLevelString + " ] not of of standard isolation levels.");
            }
        }
    }

    public boolean isIntegerIsolationLevelSet(String isolationLevelString) {

        int isolationLevel;
        try {
            isolationLevel = Integer.parseInt(isolationLevelString);
        } catch (NumberFormatException nfe) {
            return false;
        }

        if(isolationLevel == -1){
            //do nothing as no value is specified.
            return true;
        }

        switch (isolationLevel) {
            case Connection.TRANSACTION_READ_COMMITTED:
            case Connection.TRANSACTION_READ_UNCOMMITTED:
            case Connection.TRANSACTION_REPEATABLE_READ:
            case Connection.TRANSACTION_SERIALIZABLE:
                this.isolationLevel = isolationLevel;
                break;
            default:
                throw new IllegalStateException
                        ("Isolation level [ " + isolationLevel + " ] not of of standard isolation levels.");
        }
        return true;
    }

    public int getInitialPoolSize() {
        return initialPoolSize;
    }

    public void setInitialPoolSize(int initialPoolSize) {
        this.initialPoolSize = initialPoolSize;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public int getMinPoolSize() {
        return minPoolSize;
    }

    public void setMinPoolSize(int minPoolSize) {
        this.minPoolSize = minPoolSize;
    }

    public long getMaxIdleTime() {
        return maxIdleTime;
    }

    public void setMaxIdleTime(String maxIdleTime) {
        try{
            this.maxIdleTime = Long.parseLong(maxIdleTime);
        }catch(NumberFormatException nfe){
            DOLUtils.getDefaultLogger().log(Level.WARNING, "invalid maxIdleTime value [ " + maxIdleTime + " ]," +
                    " required long");
        }
    }

    public int getMaxStatements() {
        return maxStatements;
    }

    public void setMaxStatements(int maxStatements) {
        this.maxStatements = maxStatements;
    }

    public void addProperty(String key, String value){
        properties.put(key, value);
    }
    public String getProperty(String key){
        return (String)properties.get(key);
    }

    public Properties getProperties(){
        return properties;
    }

    public boolean equals(Object object) {
        if (object instanceof DataSourceDefinitionDescriptor) {
            DataSourceDefinitionDescriptor reference = (DataSourceDefinitionDescriptor) object;
            return getJavaName(this.getName()).equals(getJavaName(reference.getName()));
        }
        return false;
    }

    public int hashCode() {
        int result = 17;
        result = 37*result + getName().hashCode();
        return result;
    }

    public static String getJavaName(String thisName) {
        if(!thisName.contains(JAVA_URL)){
                thisName = JAVA_COMP_URL + thisName;
        }
        return thisName;
    }

    public void addDataSourcePropertyDescriptor(ResourcePropertyDescriptor propertyDescriptor){
        properties.put(propertyDescriptor.getName(), propertyDescriptor.getValue());
    }

    public boolean isConflict(DataSourceDefinitionDescriptor other) {
        return (getName().equals(other.getName())) &&
            !(
                DOLUtils.equals(getClassName(), other.getClassName()) &&
                DOLUtils.equals(getServerName(), other.getServerName()) &&
                getPortNumber() == other.getPortNumber() &&
                DOLUtils.equals(getDatabaseName(), other.getDatabaseName()) &&
                DOLUtils.equals(getUrl(), other.getUrl()) &&
                DOLUtils.equals(getUser(), other.getUser()) &&
                DOLUtils.equals(getPassword(), other.getPassword()) &&
                properties.equals(other.properties) &&
                getLoginTimeout() == other.getLoginTimeout() &&
                isTransactional() == other.isTransactional() &&
                getIsolationLevel() == other.getIsolationLevel() &&
                getInitialPoolSize() == other.getInitialPoolSize() &&
                getMinPoolSize() == other.getMinPoolSize() &&
                getMaxPoolSize() == other.getMaxPoolSize() &&
                getMaxIdleTime() == other.getMaxIdleTime() &&
                getMaxStatements() == other.getMaxStatements()
            );
    }
}

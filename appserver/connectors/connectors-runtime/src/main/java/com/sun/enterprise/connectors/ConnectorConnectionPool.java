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

package com.sun.enterprise.connectors;

import com.sun.enterprise.connectors.authentication.ConnectorSecurityMap;
import com.sun.enterprise.deployment.ConnectorConfigProperty;
import com.sun.logging.LogDomains;
import org.glassfish.resourcebase.resources.api.PoolInfo;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class abstracts a connection connection pool. It contains
 * two parts
 * 1) Connector Connection Pool properties.
 * 2) ConnectorDescriptorInfo which contains some of the values of ra.xml
 * pertaining to managed connection factory class
 *
 * @author Srikanth Padakandla
 */

public class ConnectorConnectionPool implements Serializable {

    protected ConnectorDescriptorInfo connectorDescriptorInfo_;

    protected String steadyPoolSize_;
    protected String maxPoolSize_;
    protected String maxWaitTimeInMillis_;
    protected String poolResizeQuantity_;
    protected String idleTimeoutInSeconds_;
    protected boolean failAllConnections_;
    //This property will *always* initially be set to:
    // true - by ConnectorConnectionPoolDeployer
    // false - by JdbcConnectionPoolDeployer
    protected boolean matchConnections_ = false;

    protected int transactionSupport_;
    protected boolean isConnectionValidationRequired_ = false;


    private boolean lazyConnectionAssoc_ = false;
    private boolean lazyConnectionEnlist_ = false;
    private boolean associateWithThread_ = false;
    private boolean partitionedPool = false;
    private boolean poolingOn = true;
    private boolean pingDuringPoolCreation = false;
    private String poolDataStructureType;
    private String poolWaitQueue;
    private String dataStructureParameters;
    private String resourceGatewayClass;
    private String resourceSelectionStrategyClass;
    private boolean nonTransactional_ = false;
    private boolean nonComponent_ = false;

    private long dynamicReconfigWaitTimeout = 0;

    private ConnectorSecurityMap[] securityMaps = null;
    private boolean isAuthCredentialsDefinedInPool_ = false;

    private String maxConnectionUsage;

    //To validate a Sun RA Pool Connection if it hasnot been
    //validated in the past x sec. (x=idle-timeout)
    //The property will be set from system property :
    //com.sun.enterprise.connectors.ValidateAtmostEveryIdleSecs=true
    private boolean validateAtmostEveryIdleSecs = false;
    //This property will be set by ConnectorConnectionPoolDeployer or
    //JdbcConnectionPoolDeployer.
    private boolean preferValidateOverRecreate_ = false;
    
    private String validateAtmostOncePeriod_ = null;

    private String conCreationRetryAttempts_ = null;
    private String conCreationRetryInterval_ = null;

    private String connectionLeakTracingTimeout_ = null;
    private boolean connectionReclaim_ = false;

    public static final String DEFAULT_MAX_CONNECTION_USAGE = "0";
    public static final String DEFAULT_CON_CREATION_RETRY_ATTEMPTS = "0";
    public static final String DEFAULT_CON_CREATION_RETRY_INTERVAL = "10";
    public static final String DEFAULT_VALIDATE_ATMOST_ONCE_PERIOD = "0";
    public static final String DEFAULT_LEAK_TIMEOUT = "0";

    private static Logger _logger = LogDomains.getLogger(ConnectorConnectionPool.class, LogDomains.RSR_LOGGER);
    private String name;
    private String applicationName;
    private String moduleName;


    /**
     * Constructor
     *
     * @param name Name of the connector connection pool
     */
    public ConnectorConnectionPool(String name, String applicationName) {
        this.name = name;
        this.applicationName = applicationName;
    }

    /**
     * Constructor
     *
     * @param name Name of the connector connection pool
     */
    public ConnectorConnectionPool(String name, String applicationName, String moduleName) {
        this.name = name;
        this.applicationName = applicationName;
        this.moduleName = moduleName;
    }

    /**
     * Constructor
     *
     * @param name Name of the connector connection pool
     */
    public ConnectorConnectionPool(String name) {
        this.name = name;
    }

    public ConnectorConnectionPool(PoolInfo poolInfo){
        this.name = poolInfo.getName();
        this.applicationName = poolInfo.getApplicationName();
        this.moduleName = poolInfo.getModuleName();
    }

    public String getApplicationName(){
        return applicationName;
    }

    public void setApplicationName(String applicationName){
        this.applicationName = applicationName;
    }

    public String getModuleName(){
        return moduleName;
    }

    public void setModuleName(String moduleName){
        this.moduleName = moduleName;
    }

    public boolean isApplicationScopedResource(){
        return applicationName != null;
    }

    public boolean getPingDuringPoolCreation() {
        return pingDuringPoolCreation;
    }

    /**
     * Setter method of Ping pool during creation attribute.
     * 
     * @param enabled enables/disables ping during creation.
     */
    public void setPingDuringPoolCreation(boolean enabled) {
        pingDuringPoolCreation = enabled;    
    }
    
    public boolean isPoolingOn() {
        return poolingOn;
    }
    
    /**
     * Setter method of pooling attribute
     *
     * @param enabled enables/disables pooling
     */
    public void setPooling(boolean enabled) {
        poolingOn = enabled;
    }
    /**
     * Clone method.
     */

    protected ConnectorConnectionPool doClone(String name) {

        ConnectorConnectionPool clone = new ConnectorConnectionPool(name);
        ConnectorDescriptorInfo cdi = connectorDescriptorInfo_.doClone();
        clone.setSecurityMaps(this.securityMaps);

        clone.setSteadyPoolSize(getSteadyPoolSize());
        clone.setMaxPoolSize(getMaxPoolSize());
        clone.setMaxWaitTimeInMillis(getMaxWaitTimeInMillis());
        clone.setPoolResizeQuantity(getPoolResizeQuantity());
        clone.setIdleTimeoutInSeconds(getIdleTimeoutInSeconds());

        clone.setConnectionValidationRequired(isConnectionValidationRequired_);
        clone.setFailAllConnections(isFailAllConnections());

        clone.setTransactionSupport(getTransactionSupport());
        clone.setConnectorDescriptorInfo(cdi);
        clone.setNonComponent(isNonComponent());
        clone.setNonTransactional(isNonTransactional());

        clone.setMatchConnections(matchConnections());
        clone.setLazyConnectionAssoc(isLazyConnectionAssoc());
        clone.setAssociateWithThread(isAssociateWithThread());
        clone.setPartitionedPool(isPartitionedPool());
        clone.setDataStructureParameters(getDataStructureParameters());
        clone.setPoolDataStructureType(getPoolDataStructureType());
        clone.setPoolWaitQueue(getPoolWaitQueue());
        clone.setLazyConnectionEnlist(isLazyConnectionEnlist());

        clone.setMaxConnectionUsage(getMaxConnectionUsage());
        clone.setValidateAtmostOncePeriod(getValidateAtmostOncePeriod());

        clone.setConnectionLeakTracingTimeout(
                getConnectionLeakTracingTimeout());
        clone.setConCreationRetryInterval
                (getConCreationRetryInterval());
        clone.setConCreationRetryAttempts(getConCreationRetryAttempts());
        clone.setPreferValidateOverRecreate(isPreferValidateOverRecreate());
        clone.setPooling(isPoolingOn());
        clone.setPingDuringPoolCreation(getPingDuringPoolCreation());
        return clone;
    }

    public String getName() {
        return name;
    }

    public void setAuthCredentialsDefinedInPool(boolean authCred) {
        this.isAuthCredentialsDefinedInPool_ = authCred;
    }

    public boolean getAuthCredentialsDefinedInPool() {
        return this.isAuthCredentialsDefinedInPool_;
    }

    /**
     * Getter method of ConnectorDescriptorInfo which contains some the ra.xml
     * values pertainining to managed connection factory
     *
     * @return ConnectorDescriptorInfo which contains ra.xml values
     *         pertaining to managed connection factory
     */

    public ConnectorDescriptorInfo getConnectorDescriptorInfo() {
        return connectorDescriptorInfo_;
    }

    /**
     * Setter method of ConnectorDescriptorInfo which contains some the ra.xml
     * values pertainining to managed connection factory
     *
     * @param connectorDescriptorInfo which contains ra.xml values
     *                                pertaining to managed connection factory
     */

    public void setConnectorDescriptorInfo(
            ConnectorDescriptorInfo connectorDescriptorInfo) {
        connectorDescriptorInfo_ = connectorDescriptorInfo;
    }


    /**
     * Getter method of SteadyPoolSize property
     *
     * @return Steady Pool Size value
     */

    public String getSteadyPoolSize() {
        return steadyPoolSize_;
    }

    /**
     * Setter method of SteadyPoolSize property
     *
     * @param steadyPoolSize Steady pool size value
     */

    public void setSteadyPoolSize(String steadyPoolSize) {
        steadyPoolSize_ = steadyPoolSize;
    }

    /**
     * Getter method of MaxPoolSize property
     *
     * @return maximum Pool Size value
     */

    public String getMaxPoolSize() {
        return maxPoolSize_;
    }

    /**
     * Setter method of MaxPoolSize property
     *
     * @param maxPoolSize maximum pool size value
     */

    public void setMaxPoolSize(String maxPoolSize) {
        maxPoolSize_ = maxPoolSize;
    }

    /**
     * Getter method of MaxWaitTimeInMillis property
     *
     * @return maximum wait time in milli value
     */

    public String getMaxWaitTimeInMillis() {
        return maxWaitTimeInMillis_;
    }

    /**
     * Setter method of MaxWaitTimeInMillis property
     *
     * @param maxWaitTimeInMillis maximum wait time in millis value
     */

    public void setMaxWaitTimeInMillis(String maxWaitTimeInMillis) {
        maxWaitTimeInMillis_ = maxWaitTimeInMillis;
    }

    /**
     * Getter method of PoolResizeQuantity property
     *
     * @return pool resize quantity value
     */

    public String getPoolResizeQuantity() {
        return poolResizeQuantity_;
    }

    /**
     * Setter method of PoolResizeQuantity property
     *
     * @param poolResizeQuantity pool resize quantity value
     */

    public void setPoolResizeQuantity(String poolResizeQuantity) {
        poolResizeQuantity_ = poolResizeQuantity;
    }

    /**
     * Getter method of IdleTimeoutInSeconds property
     *
     * @return idle Timeout in seconds value
     */

    public String getIdleTimeoutInSeconds() {
        return idleTimeoutInSeconds_;
    }

    /**
     * Setter method of IdleTimeoutInSeconds property
     *
     * @param idleTimeoutInSeconds Idle timeout in seconds value
     */

    public void setIdleTimeoutInSeconds(String idleTimeoutInSeconds) {
        idleTimeoutInSeconds_ = idleTimeoutInSeconds;
    }

    /**
     * Getter method of FailAllConnections property
     *
     * @return whether to fail all connections or not
     */

    public boolean isFailAllConnections() {
        return failAllConnections_;
    }

    /**
     * Setter method of FailAllConnections property
     *
     * @param failAllConnections fail all connections value
     */

    public void setFailAllConnections(boolean failAllConnections) {
        failAllConnections_ = failAllConnections;
    }

    /**
     * Getter method of matchConnections property
     *
     * @return whether to match connections always with resource adapter
     *         or not
     */

    public boolean matchConnections() {
        return matchConnections_;
    }

    /**
     * Setter method of matchConnections property
     *
     * @param matchConnections fail all connections value
     */

    public void setMatchConnections(boolean matchConnections) {
        matchConnections_ = matchConnections;
    }

    /**
     * Returns the transaction support level for this pool
     * The valid values are<br>
     * <ul>
     * <li>ConnectorConstants.NO_TRANSACTION</li>
     * <li>ConnectorConstants.LOCAL_TRANSACTION</li>
     * <li>ConnectorConstants.XA_TRANSACTION</li>
     * </ul>
     *
     * @return the transaction support level for this pool
     */
    public int getTransactionSupport() {
        return transactionSupport_;
    }

    /**
     * Sets the transaction support level for this pool
     * The valid values are<br>
     *
     * @param transactionSupport int representing transaction support<br>
     *                           <ul>
     *                           <li>ConnectorConstants.NO_TRANSACTION</li>
     *                           <li>ConnectorConstants.LOCAL_TRANSACTION</li>
     *                           <li>ConnectorConstants.XA_TRANSACTION</li>
     *                           </ul>
     */
    public void setTransactionSupport(int transactionSupport) {
        transactionSupport_ = transactionSupport;
    }

    /**
     * Sets the connection-validation-required pool attribute
     *
     * @param validation boolean representing validation requirement
     */
    public void setConnectionValidationRequired(boolean validation) {
        isConnectionValidationRequired_ = validation;
    }

    /**
     * Queries the connection-validation-required pool attribute
     *
     * @return boolean representing validation requirement
     */
    public boolean isIsConnectionValidationRequired() {
        return isConnectionValidationRequired_;
    }

    /**
     * Queries the lazy-connection-association pool attribute
     *
     * @return boolean representing lazy-connection-association status
     */
    public boolean isLazyConnectionAssoc() {
        return lazyConnectionAssoc_;
    }

    /**
     * Setter method of lazyConnectionAssociation attribute
     *
     * @param enabled enables/disables lazy-connection-association
     */
    public void setLazyConnectionAssoc(boolean enabled) {
        lazyConnectionAssoc_ = enabled;
    }

    /**
     * Queries the lazy-connection-enlistment pool attribute
     *
     * @return boolean representing lazy-connection-enlistment status
     */
    public boolean isLazyConnectionEnlist() {
        return lazyConnectionEnlist_;
    }

    /**
     * Setter method of lazy-connection-enlistment attribute
     *
     * @param enabled enables/disables lazy-connection-enlistment
     */
    public void setLazyConnectionEnlist(boolean enabled) {
        lazyConnectionEnlist_ = enabled;
    }

    /**
     * Queries the associate-with-thread pool attribute
     *
     * @return boolean representing associate-with-thread status
     */
    public boolean isAssociateWithThread() {
        return associateWithThread_;
    }

    /**
     * Setter method of associate-with-thread attribute
     *
     * @param enabled enables/disables associate-with-thread
     */
    public void setAssociateWithThread(boolean enabled) {
        associateWithThread_ = enabled;
    }

    /**
     * Queries the non-transactional pool attribute
     *
     * @return boolean representing non-transactional status
     */
    public boolean isNonTransactional() {
        return nonTransactional_;
    }

    /**
     * Setter method of non-transactional attribute
     *
     * @param enabled enables/disables non-transactional status
     */
    public void setNonTransactional(boolean enabled) {
        nonTransactional_ = enabled;
    }

    /**
     * Queries the non-component pool attribute
     *
     * @return boolean representing non-component status
     */
    public boolean isNonComponent() {
        return nonComponent_;
    }

    /**
     * Setter method of non-component attribute
     *
     * @param enabled enables/disables non-component status
     */
    public void setNonComponent(boolean enabled) {
        nonComponent_ = enabled;
    }

    /**
     * Queries the connection-leak-tracing-timeout pool attribute
     *
     * @return boolean representing connection-leak-tracing-timeout status
     */
    public String getConnectionLeakTracingTimeout() {
        return connectionLeakTracingTimeout_;
    }

    /**
     * Setter method of connection-leak-tracing-timeout attribute
     *
     * @param timeout value after which connection is assumed to be leaked.
     */
    public void setConnectionLeakTracingTimeout(String timeout) {
        connectionLeakTracingTimeout_ = timeout;
    }


    /**
     * Setter method for Security Maps
     *
     * @param securityMapArray SecurityMap[]
     */

    public void setSecurityMaps(ConnectorSecurityMap[] securityMapArray) {
        this.securityMaps = securityMapArray;
    }

    /**
     * Getter method for Security Maps
     *
     * @return SecurityMap[]
     */

    public ConnectorSecurityMap[] getSecurityMaps() {
        return this.securityMaps;
    }


    /**
     * Queries the validate-atmost-every-idle-seconds pool attribute
     *
     * @return boolean representing validate-atmost-every-idle-seconds
     *         status
     */
    public boolean isValidateAtmostEveryIdleSecs() {
        return validateAtmostEveryIdleSecs;
    }

    /**
     * Setter method of validate-atmost-every-idle-seconds pool attribute
     *
     * @param enabled enables/disables validate-atmost-every-idle-seconds
     *                property
     */
    public void setValidateAtmostEveryIdleSecs(boolean enabled) {
        this.validateAtmostEveryIdleSecs = enabled;
    }

    /**
     * Setter method of max-connection-usage pool attribute
     *
     * @param count max-connection-usage count
     */
    public void setMaxConnectionUsage(String count) {
        maxConnectionUsage = count;
    }

    /**
     * Queries the max-connection-usage pool attribute
     *
     * @return boolean representing max-connection-usage count
     */
    public String getMaxConnectionUsage() {
        return maxConnectionUsage;
    }

    /**
     * Queries the connection-creation-retry-interval pool attribute
     *
     * @return boolean representing connection-creation-retry-interval
     *         duration
     */
    public String getConCreationRetryInterval() {
        return conCreationRetryInterval_;
    }

    /**
     * Setter method of connection-creation-retry-interval attribute
     *
     * @param retryInterval connection-creation-retry-interval  duration
     */
    public void setConCreationRetryInterval(String retryInterval) {
        this.conCreationRetryInterval_ = retryInterval;
    }

    /**
     * Queries the connection-creation-retry-attempt pool attribute
     *
     * @return boolean representing connection-creation-retry-attempt count
     */
    public String getConCreationRetryAttempts() {
        return conCreationRetryAttempts_;
    }

    /**
     * Setter method of connection-creation-retry-attempt attribute
     *
     * @param retryAttempts connection-creation-retry-attempt interval
     *                      duration
     */
    public void setConCreationRetryAttempts(String retryAttempts) {
        this.conCreationRetryAttempts_ = retryAttempts;
    }

    /**
     * Queries the validate-atmost-period pool attribute
     *
     * @return boolean representing validate-atmost-period duration
     */
    public String getValidateAtmostOncePeriod() {
        return validateAtmostOncePeriod_;
    }

    /**
     * Setter method of validate-atmost-period attribute
     *
     * @param validateAtmostOncePeriod validate-atmost-period duration
     */
    public void setValidateAtmostOncePeriod(String validateAtmostOncePeriod) {
        this.validateAtmostOncePeriod_ = validateAtmostOncePeriod;
    }

    /**
     * Queries the connection-reclaim attribute
     *
     * @return boolean representing connection-reclaim status
     */
    public boolean isConnectionReclaim() {
        return connectionReclaim_;
    }

    /**
     * Setter method of connection-reclaim attribute
     *
     * @param connectionReclaim onnection-reclaim status
     */
    public void setConnectionReclaim(boolean connectionReclaim) {
        this.connectionReclaim_ = connectionReclaim;
    }

    /**
     * return the String representation of the pool.
     *
     * @return String representation of pool
     */
    public String toString() {
        String returnVal = "";
        StringBuffer sb = new StringBuffer("ConnectorConnectionPool :: ");
        try {
            sb.append(getName());
            sb.append("\nsteady size: ");
            sb.append(getSteadyPoolSize());
            sb.append("\nmax pool size: ");
            sb.append(getMaxPoolSize());
            sb.append("\nmax wait time: ");
            sb.append(getMaxWaitTimeInMillis());
            sb.append("\npool resize qty: ");
            sb.append(getPoolResizeQuantity());
            sb.append("\nIdle timeout: ");
            sb.append(getIdleTimeoutInSeconds());
            sb.append("\nfailAllConnections: ");
            sb.append(isFailAllConnections());
            sb.append("\nTransaction Support Level: ");
            sb.append(transactionSupport_);
            sb.append("\nisConnectionValidationRequired_ ");
            sb.append(isConnectionValidationRequired_);
            sb.append("\npreferValidateOverRecreate_ ");
            sb.append(preferValidateOverRecreate_);

            sb.append("\nmatchConnections_ ");
            sb.append(matchConnections_);
            sb.append("\nassociateWithThread_ ");
            sb.append(associateWithThread_);
            sb.append("\nlazyConnectionAssoc_ ");
            sb.append(lazyConnectionAssoc_);
            sb.append("\nlazyConnectionEnlist_ ");
            sb.append(lazyConnectionEnlist_);
            sb.append("\nmaxConnectionUsage_ ");
            sb.append(maxConnectionUsage);

            sb.append("\npingPoolDuringCreation_ ");
            sb.append(pingDuringPoolCreation);

            sb.append("\npoolingOn_ ");
            sb.append(poolingOn);

            sb.append("\nvalidateAtmostOncePeriod_ ");
            sb.append(validateAtmostOncePeriod_);

            sb.append("\nconnectionLeakTracingTimeout_");
            sb.append(connectionLeakTracingTimeout_);
            sb.append("\nconnectionReclaim_");
            sb.append(connectionReclaim_);

            sb.append("\nconnectionCreationRetryAttempts_");
            sb.append(conCreationRetryAttempts_);
            sb.append("\nconnectionCreationRetryIntervalInMilliSeconds_");
            sb.append(conCreationRetryInterval_);

            sb.append("\nnonTransactional_ ");
            sb.append(nonTransactional_);
            sb.append("\nnonComponent_ ");
            sb.append(nonComponent_);

            sb.append("\nConnectorDescriptorInfo -> ");
            sb.append("\nrarName: ");
            if (connectorDescriptorInfo_ != null) {
                sb.append(connectorDescriptorInfo_.getRarName());
                sb.append("\nresource adapter class: ");
                sb.append(connectorDescriptorInfo_.getResourceAdapterClassName());
                sb.append("\nconnection def name: ");
                sb.append(connectorDescriptorInfo_.getConnectionDefinitionName());
                sb.append("\nMCF Config properties-> ");
                for (Object o : connectorDescriptorInfo_.getMCFConfigProperties()) {
                    ConnectorConfigProperty  ep = (ConnectorConfigProperty) o;
                    sb.append(ep.getName());
                    sb.append(":");
                    sb.append(("password".equalsIgnoreCase(ep.getName()) ?
                            "****" : ep.getValue()));
                    sb.append("\n");
                }
            }
            if (securityMaps != null) {
                sb.append("SecurityMaps -> {");
                for (ConnectorSecurityMap securityMap : securityMaps) {
                    if (securityMap != null &&
                            securityMap.getName() != null) {
                        sb.append(securityMap.getName());
                        sb.append(" ");
                    }
                }
                sb.append("}");
            }
            returnVal = sb.toString();
        } catch (Exception e) {
            _logger.log(Level.WARNING, "Exception while computing toString() of connection pool [ "+name+" ]", e);
        }
        return returnVal;
    }

    public boolean isPartitionedPool() {
        return partitionedPool;
    }

    public void setPartitionedPool(boolean partitionedPool) {
        this.partitionedPool = partitionedPool;
    }

    public String getPoolDataStructureType() {
        return poolDataStructureType;
    }

    public void setPoolDataStructureType(String poolDataStructureType) {
        this.poolDataStructureType = poolDataStructureType;
    }

    public String getPoolWaitQueue() {
        return poolWaitQueue;
    }

    public void setPoolWaitQueue(String poolWaitQueue) {
        this.poolWaitQueue = poolWaitQueue;
    }

    public String getDataStructureParameters() {
        return dataStructureParameters;
    }

    public void setDataStructureParameters(String dataStructureParameters) {
        this.dataStructureParameters = dataStructureParameters;
    }

    public String getResourceGatewayClass() {
        return resourceGatewayClass;
    }

    public void setResourceGatewayClass(String resourceGatewayClass) {
        this.resourceGatewayClass = resourceGatewayClass;
    }

    public String getResourceSelectionStrategyClass() {
        return resourceSelectionStrategyClass;
    }

    public void setResourceSelectionStrategyClass(String resourceSelectionStrategyClass) {
        this.resourceSelectionStrategyClass = resourceSelectionStrategyClass;
    }

    public boolean isPreferValidateOverRecreate() {
        return preferValidateOverRecreate_;
    }

    public void setPreferValidateOverRecreate(boolean preferValidateOverRecreate) {
        preferValidateOverRecreate_ = preferValidateOverRecreate;
    }

    public long getDynamicReconfigWaitTimeout() {
        return dynamicReconfigWaitTimeout;
    }

    public void setDynamicReconfigWaitTimeout(long dynamicReconfigWaitTimeout) {
        this.dynamicReconfigWaitTimeout = dynamicReconfigWaitTimeout;
    }

    public PoolInfo getPoolInfo(){
        if(applicationName != null && moduleName != null){
            return new PoolInfo(name, applicationName, moduleName);
        }else if(applicationName != null){
            return new PoolInfo(name, applicationName);
        }else{
            return new PoolInfo(name);
        }
    }

}

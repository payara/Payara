/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2017] [Payara Foundation and/or its affiliates]

package com.sun.enterprise.resource.deployer;

import com.sun.appserv.connectors.internal.api.ConnectorsUtil;
import com.sun.enterprise.config.serverbeans.Resource;
import com.sun.enterprise.config.serverbeans.Resources;
import com.sun.enterprise.deployment.ConnectionFactoryDefinitionDescriptor;
import com.sun.logging.LogDomains;
import org.glassfish.connectors.config.ConnectorConnectionPool;
import org.glassfish.connectors.config.ConnectorResource;
import org.glassfish.connectors.config.SecurityMap;
import org.glassfish.resourcebase.resources.api.ResourceConflictException;
import org.glassfish.resourcebase.resources.api.ResourceDeployer;
import org.glassfish.resourcebase.resources.api.ResourceDeployerInfo;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.TransactionFailure;
import org.jvnet.hk2.config.types.Property;

import javax.inject.Inject;
import javax.inject.Provider;
import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.config.support.TranslatedConfigView;

import static org.glassfish.deployment.common.JavaEEResourceType.*;

/**
 * @author Dapeng Hu
 */
@Service
@ResourceDeployerInfo(ConnectionFactoryDefinitionDescriptor.class)
public class ConnectionFactoryDefinitionDeployer implements ResourceDeployer {

    @Inject
    private Provider<org.glassfish.resourcebase.resources.util.ResourceManagerFactory> resourceManagerFactoryProvider;

    private static Logger _logger = LogDomains.getLogger(ConnectionFactoryDefinitionDeployer.class, LogDomains.RSR_LOGGER);
    final static String PROPERTY_PREFIX = "org.glassfish.connector-connection-pool.";

    public void deployResource(Object resource, String applicationName, String moduleName) throws Exception {
        //TODO ASR
    }
    
    public void deployResource(Object resource) throws Exception {

        final ConnectionFactoryDefinitionDescriptor desc = (ConnectionFactoryDefinitionDescriptor) resource;
        String poolName = ConnectorsUtil.deriveResourceName(desc.getResourceId(), desc.getName(), CFDPOOL);
        String resourceName = ConnectorsUtil.deriveResourceName(desc.getResourceId(), desc.getName(),desc.getResourceType());

        if(_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, "ConnectionFactoryDefinitionDeployer.deployResource() : pool-name ["+poolName+"], " +
                    " resource-name ["+resourceName+"]");
        }

        ConnectorConnectionPool connectorCp = new MyConnectorConnectionPool(desc, poolName);

        //deploy pool
        getDeployer(connectorCp).deployResource(connectorCp);

        //deploy resource
        ConnectorResource connectorResource = new MyConnectorResource(poolName, resourceName);
        getDeployer(connectorResource).deployResource(connectorResource);
        
    }

    /**
     * {@inheritDoc}
     */
    public boolean canDeploy(boolean postApplicationDeployment, Collection<Resource> allResources, Resource resource){
        if(handles(resource)){
            if(!postApplicationDeployment){
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public void validatePreservedResource(com.sun.enterprise.config.serverbeans.Application oldApp,
                                          com.sun.enterprise.config.serverbeans.Application newApp, 
                                          Resource resource,
                                          Resources allResources)
    throws ResourceConflictException {
        //do nothing.
    }


    private ResourceDeployer getDeployer(Object resource) {
        return resourceManagerFactoryProvider.get().getResourceDeployer(resource);
    }

    private ConnectionFactoryProperty convertProperty(String name, String value) {
        return new ConnectionFactoryProperty(name, value);
    }

    public void undeployResource(Object resource, String applicationName, String moduleName) throws Exception {
        //TODO ASR
    }

    public void undeployResource(Object resource) throws Exception {

        final ConnectionFactoryDefinitionDescriptor desc = (ConnectionFactoryDefinitionDescriptor) resource;

        String poolName = ConnectorsUtil.deriveResourceName(desc.getResourceId(), desc.getName(), CFDPOOL);
        String resourceName = ConnectorsUtil.deriveResourceName(desc.getResourceId(), desc.getName(),desc.getResourceType());

        if(_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, "ConnectionFactoryDefinitionDeployer.undeployResource() : pool-name ["+poolName+"], " +
                    " resource-name ["+resourceName+"]");
        }

        //undeploy resource
        ConnectorResource connectorResource = new MyConnectorResource(poolName, resourceName);
        getDeployer(connectorResource).undeployResource(connectorResource);

        //undeploy pool
        ConnectorConnectionPool connectorCp = new MyConnectorConnectionPool(desc, poolName);
        getDeployer(connectorCp).undeployResource(connectorCp);

    }

    public void redeployResource(Object resource) throws Exception {
        throw new UnsupportedOperationException("redeploy() not supported for connection-factory-definition type");
    }

    public void enableResource(Object resource) throws Exception {
        throw new UnsupportedOperationException("enable() not supported for connection-factory-definition type");
    }

    public void disableResource(Object resource) throws Exception {
        throw new UnsupportedOperationException("disable() not supported for connection-factory-definition type");
    }

    public boolean handles(Object resource) {
        return resource instanceof ConnectionFactoryDefinitionDescriptor;
    }

    /**
     * @inheritDoc
     */
    public boolean supportsDynamicReconfiguration() {
        return false;
    }

    /**
     * @inheritDoc
     */
    public Class[] getProxyClassesForDynamicReconfiguration() {
        return new Class[0];
    }

    abstract class FakeConfigBean implements ConfigBeanProxy {
 
        public ConfigBeanProxy deepCopy(ConfigBeanProxy parent) {
            throw new UnsupportedOperationException();
        }

        public ConfigBeanProxy getParent() {
            return null;
        }

        public <T extends ConfigBeanProxy> T getParent(Class<T> tClass) {
            return null;
        }

        public <T extends ConfigBeanProxy> T createChild(Class<T> tClass) throws TransactionFailure {
            return null;
        }        
    }

    class ConnectionFactoryProperty extends FakeConfigBean implements Property {

        private String name;
        private String value;
        private String description;

        ConnectionFactoryProperty(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public void setName(String value) throws PropertyVetoException {
            this.name = value;
        }

        public String getValue() {
            return TranslatedConfigView.expandValue(value);
        }

        public void setValue(String value) throws PropertyVetoException {
            this.value = value;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String value) throws PropertyVetoException {
            this.description = value;
        }

        public void injectedInto(Object o) {
            //do nothing
        }
    }

    class MyConnectorResource extends FakeConfigBean implements ConnectorResource {

        private String poolName;
        private String jndiName;

        MyConnectorResource(String poolName, String jndiName) {
            this.poolName = poolName;
            this.jndiName = jndiName;
        }

        public String getPoolName() {
            return poolName;
        }

        public void setPoolName(String value) throws PropertyVetoException {
            this.poolName = value;
        }

        public String getObjectType() {
            return null;
        }

        public void setObjectType(String value) throws PropertyVetoException {
        }

        public String getIdentity() {
            return jndiName;
        }

        public String getEnabled() {
            return String.valueOf(true);
        }

        public void setEnabled(String value) throws PropertyVetoException {
        }

        public String getDescription() {
            return null;
        }

        public void setDescription(String value) throws PropertyVetoException {
        }

        public List<Property> getProperty() {
            return null;
        }

        @Override
        public Property addProperty(Property property) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Property lookupProperty(String s) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Property removeProperty(String s) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Property removeProperty(Property property) {
            throw new UnsupportedOperationException();
        }

        public Property getProperty(String name) {
            return null;
        }

        public String getPropertyValue(String name) {
            return null;
        }

        public String getPropertyValue(String name, String defaultValue) {
            return null;
        }

        public void injectedInto(Object o) {
        }

        public String getJndiName() {
            return jndiName;
        }

        public void setJndiName(String value) throws PropertyVetoException {
            this.jndiName = value;
        }

        public String getDeploymentOrder() {
            return null;
        }

        public void setDeploymentOrder(String value) {
            //do nothing
        }
    }
    class MyConnectorConnectionPool extends FakeConfigBean implements ConnectorConnectionPool {

        private ConnectionFactoryDefinitionDescriptor desc;
        private String name;

        public MyConnectorConnectionPool(ConnectionFactoryDefinitionDescriptor desc, String name) {
            this.desc = desc;
            this.name = name;
        }

        @Override
        public String getObjectType() {
            return "user";  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public void setObjectType(String value) throws PropertyVetoException {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public String getIdentity() {
            return name;
        }

        public String getSteadyPoolSize() {
            int minPoolSize = desc.getMinPoolSize();
            if(minPoolSize >= 0){
                return Integer.toString(minPoolSize);
            }else{
                return "8";
            }
        }

        public void setSteadyPoolSize(String value) throws PropertyVetoException {
            //do nothing
        }

        public String getMaxPoolSize() {
            int maxPoolSize = desc.getMaxPoolSize();
            if (maxPoolSize >= 0) {
                return Integer.toString(maxPoolSize);
            }else{
                return "32";
            }
        }

        public void setMaxPoolSize(String value) throws PropertyVetoException {
            //do nothing
        }

        public String getMaxWaitTimeInMillis() {
            String maxWaitTimeInMillis = desc.getProperty(PROPERTY_PREFIX+"max-wait-time-in-millis");
            if (maxWaitTimeInMillis != null && !maxWaitTimeInMillis.equals("")) {
                return maxWaitTimeInMillis;
            }else{
                return "60000";
            }
        }

        public void setMaxWaitTimeInMillis(String value) throws PropertyVetoException {
            //do nothing
        }

        public String getPoolResizeQuantity() {
            String poolResizeQuantity = desc.getProperty(PROPERTY_PREFIX+"pool-resize-quantity");
            if (poolResizeQuantity != null && !poolResizeQuantity.equals("")) {
                return poolResizeQuantity;
            }else{
                return "2";
            }
        }

        public void setPoolResizeQuantity(String value) throws PropertyVetoException {
            //do nothing
        }
        
        public String getIdleTimeoutInSeconds() {
            String idleTimeoutInSeconds = desc.getProperty(PROPERTY_PREFIX+"idle-timeout-in-seconds");
            if (idleTimeoutInSeconds != null && !idleTimeoutInSeconds.equals("")) {
                return idleTimeoutInSeconds;
            }else{
                return "300";
            }
        }

        public void setIdleTimeoutInSeconds(String value) throws PropertyVetoException {
            //do nothing
        }

        public String getIsConnectionValidationRequired() {
            String isConnectionValidationRequired = desc.getProperty(PROPERTY_PREFIX+"is-connection-validation-required");
            if (isConnectionValidationRequired != null && !isConnectionValidationRequired.equals("")) {
                return isConnectionValidationRequired;
            }else{
                return "false";
            }
        }

        public void setIsConnectionValidationRequired(String value) throws PropertyVetoException {
            //do nothing
        }

        public String getResourceAdapterName() {
            return desc.getResourceAdapter();
        }

        public void setResourceAdapterName(String value) throws PropertyVetoException {
            //do nothing
        }

        public String getConnectionDefinitionName() {
            return desc.getInterfaceName();
        }

        public void setConnectionDefinitionName(String value)  throws PropertyVetoException {
            //do nothing
        }

        public String getFailAllConnections() {
            String failAllConnections = desc.getProperty(PROPERTY_PREFIX+"fail-all-connections");
            if (failAllConnections != null && !failAllConnections.equals("")) {
                return failAllConnections;
            }else{
                return "false";
            }
        }

        public void setFailAllConnections(String value) throws PropertyVetoException {
            //do nothing
        }

        public String getTransactionSupport() {
            return desc.getTransactionSupport();
        }

        public void setTransactionSupport(String value) throws PropertyVetoException {
            //do nothing
        }

        public String getValidateAtmostOncePeriodInSeconds() {
            String validateAtmostOncePeriodInSeconds = desc.getProperty(PROPERTY_PREFIX+"validate-at-most-once-period-in-seconds");
            if (validateAtmostOncePeriodInSeconds != null && !validateAtmostOncePeriodInSeconds.equals("")) {
                return validateAtmostOncePeriodInSeconds;
            }else{
                return "0";
            }
        }

        public void setValidateAtmostOncePeriodInSeconds(String value) throws PropertyVetoException {
            //do nothing
        }

        public String getConnectionLeakTimeoutInSeconds() {
            String connectionLeakTimeoutInSeconds = desc.getProperty(PROPERTY_PREFIX+"connection-leak-timeout-in-seconds");
            if (connectionLeakTimeoutInSeconds != null && !connectionLeakTimeoutInSeconds.equals("")) {
                return connectionLeakTimeoutInSeconds;
            }else{
                return "0";
            }
        }

        public void setConnectionLeakTimeoutInSeconds(String value) throws PropertyVetoException {
            //do nothing
        }

        public String getConnectionLeakReclaim() {
            String connectionLeakReclaim = desc.getProperty(PROPERTY_PREFIX+"connection-leak-reclaim");
            if (connectionLeakReclaim != null && !connectionLeakReclaim.equals("")) {
                return connectionLeakReclaim;
            }else{
                return "0";
            }
        }

        public void setConnectionLeakReclaim(String value) throws PropertyVetoException {
            //do nothing
        }

        public String getConnectionCreationRetryAttempts() {
            String connectionCreationRetryAttempts = desc.getProperty(PROPERTY_PREFIX+"connection-creation-retry-attempts");
            if (connectionCreationRetryAttempts != null && !connectionCreationRetryAttempts.equals("")) {
                return connectionCreationRetryAttempts;
            }else{
                return "0";
            }
        }

        public void setConnectionCreationRetryAttempts(String value) throws PropertyVetoException {
            //do nothing
        }

        public String getConnectionCreationRetryIntervalInSeconds() {
            String connectionCreationRetryIntervalInSeconds = desc.getProperty(PROPERTY_PREFIX+"connection-creation-retry-interval-in-seconds");
            if (connectionCreationRetryIntervalInSeconds != null && !connectionCreationRetryIntervalInSeconds.equals("")) {
                return connectionCreationRetryIntervalInSeconds;
            }else{
                return "0";
            }
        }

        public void setConnectionCreationRetryIntervalInSeconds(String value) throws PropertyVetoException {
            //do nothing
        }

        public String getLazyConnectionEnlistment() {
            String lazyConnectionEnlistment = desc.getProperty(PROPERTY_PREFIX+"lazy-connection-enlistment");
            if (lazyConnectionEnlistment != null && !lazyConnectionEnlistment.equals("")) {
                return lazyConnectionEnlistment;
            }else{
                return "false";
            }
        }

        public void setLazyConnectionEnlistment(String value) throws PropertyVetoException {
            //do nothing
        }

        public String getLazyConnectionAssociation() {
            String lazyConnectionAssociation = desc.getProperty(PROPERTY_PREFIX+"lazy-connection-association");
            if (lazyConnectionAssociation != null && !lazyConnectionAssociation.equals("")) {
                return lazyConnectionAssociation;
            }else{
                return "false";
            }
        }

        public void setLazyConnectionAssociation(String value) throws PropertyVetoException {
            //do nothing
        }

        public String getAssociateWithThread() {
            String associateWithThread = desc.getProperty(PROPERTY_PREFIX+"associate-with-thread");
            if (associateWithThread != null && !associateWithThread.equals("")) {
                return associateWithThread;
            }else{
                return "false";
            }
        }

        public void setAssociateWithThread(String value) throws PropertyVetoException {
            //do nothing
        }

        public String getPooling() {
            String pooling = desc.getProperty(PROPERTY_PREFIX+"pooling");
            if (pooling != null && !pooling.equals("")) {
                return pooling;
            }else{
                return "true";
            }
        }

        public void setPooling(String value) throws PropertyVetoException {
            //do nothing
        }

        public String getMatchConnections() {
            String matchConnections = desc.getProperty(PROPERTY_PREFIX+"match-connections");
            if (matchConnections != null && !matchConnections.equals("")) {
                return matchConnections;
            }else{
                return "true";
            }
        }

        public void setMatchConnections(String value) throws PropertyVetoException {
            //do nothing
        }

        public String getMaxConnectionUsageCount() {
            String maxConnectionUsageCount = desc.getProperty(PROPERTY_PREFIX+"max-connection-usage-count");
            if (maxConnectionUsageCount != null && !maxConnectionUsageCount.equals("")) {
                return maxConnectionUsageCount;
            }else{
                return "0";
            }
        }

        public void setMaxConnectionUsageCount(String value) throws PropertyVetoException {
            //do nothing
        }

        public String getDescription() {
            return desc.getDescription();
        }

        public void setDescription(String value) throws PropertyVetoException {
            //do nothing
        }

        public List<Property> getProperty() {
            Properties p = desc.getProperties();
            List<Property> connectionFactoryProperties = new ArrayList<Property>();
            for (Entry<Object, Object> entry : p.entrySet()) {
                String key = (String) entry.getKey();
                if(key.startsWith(PROPERTY_PREFIX)){
                    continue;
                }
                String value = (String) entry.getValue();
                ConnectionFactoryProperty dp = convertProperty(key, value);
                connectionFactoryProperties.add(dp);
            }

            return connectionFactoryProperties;
        }

        @Override
        public Property addProperty(Property property) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Property lookupProperty(String s) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Property removeProperty(String s) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Property removeProperty(Property property) {
            throw new UnsupportedOperationException();
        }

        public Property getProperty(String name) {
            String value = desc.getProperty(name);
            return new ConnectionFactoryProperty(name, value);
        }

        public String getPropertyValue(String name) {
            return desc.getProperty(name);
        }

        public String getPropertyValue(String name, String defaultValue) {
            String value = null;
            value = desc.getProperty(name);
            if (value != null) {
                return value;
            } else {
                return defaultValue;
            }
        }

        public void injectedInto(Object o) {
            //do nothing
        }

        public String getName() {
            return name;
        }

        public void setName(String value) throws PropertyVetoException {
            //do nothing
        }

        public String getPing() {
            String ping = desc.getProperty(PROPERTY_PREFIX+"ping");
            if (ping != null && !ping.equals("")) {
                return ping;
            }else{
                return "false";
            }
        }

        public void setPing(String value) throws PropertyVetoException {
            //do nothing
        }

        public List<SecurityMap> getSecurityMap() {
            return new ArrayList<SecurityMap>(0);
        }

        public String getDeploymentOrder() {
            return null;
        }

        public void setDeploymentOrder(String value) {
            //do nothing
        }

    }
}

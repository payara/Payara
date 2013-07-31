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

package com.sun.enterprise.resource.deployer;


import com.sun.appserv.connectors.internal.api.ConnectorRuntimeException;
import com.sun.appserv.connectors.internal.api.ConnectorsUtil;
import com.sun.enterprise.connectors.ConnectorConnectionPool;
import com.sun.enterprise.connectors.ConnectorDescriptorInfo;
import com.sun.enterprise.connectors.ConnectorRuntime;
import com.sun.enterprise.connectors.util.ConnectionPoolObjectsUtils;
import com.sun.enterprise.connectors.util.ConnectorDDTransformUtils;
import com.sun.enterprise.connectors.util.SecurityMapUtils;
import com.sun.enterprise.deployment.ConnectionDefDescriptor;
import com.sun.enterprise.deployment.ConnectorConfigProperty;
import com.sun.enterprise.deployment.ConnectorDescriptor;
import com.sun.enterprise.util.i18n.StringManager;
import com.sun.logging.LogDomains;
import org.glassfish.connectors.config.SecurityMap;
import org.glassfish.resourcebase.resources.api.PoolInfo;
import org.glassfish.resourcebase.resources.api.ResourceDeployer;
import org.glassfish.resourcebase.resources.api.ResourceDeployerInfo;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.types.Property;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * @author Srikanth P, Sivakumar Thyagarajan
 */

@Service
@ResourceDeployerInfo(org.glassfish.connectors.config.ConnectorConnectionPool.class)
@Singleton
public class ConnectorConnectionPoolDeployer extends AbstractConnectorResourceDeployer{

    @Inject
    private ConnectorRuntime runtime;

    private static Logger _logger = LogDomains.getLogger(ConnectorConnectionPoolDeployer.class, LogDomains.RSR_LOGGER);

    private static StringManager localStrings =
            StringManager.getManager(ConnectorConnectionPoolDeployer.class);

    private static final Locale locale = Locale.getDefault();

    /**
     * {@inheritDoc}
     */
    public void deployResource(Object resource, String applicationName, String moduleName) throws Exception {
        //deployResource is not synchronized as there is only one caller
        //ResourceProxy which is synchronized

        if(_logger.isLoggable(Level.FINE)) {
            _logger.fine("ConnectorConnectionPoolDeployer : deployResource ");
        }

        final org.glassfish.connectors.config.ConnectorConnectionPool
                domainCcp =
                (org.glassfish.connectors.config.ConnectorConnectionPool) resource;

        // If the user is trying to modify the default pool,
        // redirect call to redeployResource
        if (ConnectionPoolObjectsUtils.isPoolSystemPool(domainCcp)) {
            this.redeployResource(resource);
            return;
        }

        PoolInfo poolInfo = new PoolInfo(domainCcp.getName(), applicationName, moduleName);
        final ConnectorConnectionPool ccp = getConnectorConnectionPool(domainCcp, poolInfo);
        String rarName = domainCcp.getResourceAdapterName();
        String connDefName = domainCcp.getConnectionDefinitionName();
        List<Property> props = domainCcp.getProperty();
        List<SecurityMap> securityMaps = domainCcp.getSecurityMap();

        populateConnectorConnectionPool(ccp, connDefName, rarName, props, securityMaps);
        final String defName = domainCcp.getConnectionDefinitionName();

        /*if (domainCcp.isEnabled()) {
            if (UNIVERSAL_CF.equals(defName) || QUEUE_CF.equals(defName) || TOPIC_CF.equals(defName)) {
            //registers the jsr77 object for the mail resource deployed
            final ManagementObjectManager mgr =
                getAppServerSwitchObject().getManagementObjectManager();
            mgr.registerJMSResource(domainCcp.getName(), defName, null, null,
                    getPropNamesAsStrArr(domainCcp.getElementProperty()),
                    getPropValuesAsStrArr(domainCcp.getElementProperty()));
            }

        } else {
                _logger.log(Level.INFO, "core.resource_disabled",
                        new Object[] {domainCcp.getName(),
                        IASJ2EEResourceFactoryImpl.CONNECTOR_CONN_POOL_TYPE});
        }*/


        if(_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, "Calling backend to add connectorConnectionPool", domainCcp.getResourceAdapterName());
        }
        runtime.createConnectorConnectionPool(ccp, defName, domainCcp.getResourceAdapterName(),
                domainCcp.getProperty(), domainCcp.getSecurityMap());
        if(_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, "Added connectorConnectionPool in backend",
                domainCcp.getResourceAdapterName());
        }

    }
    
    /**
     * {@inheritDoc}
     */
    public void deployResource(Object resource) throws Exception {
        org.glassfish.connectors.config.ConnectorConnectionPool ccp =
                (org.glassfish.connectors.config.ConnectorConnectionPool)resource;
        PoolInfo poolInfo = ConnectorsUtil.getPoolInfo(ccp);
        deployResource(resource, poolInfo.getApplicationName(), poolInfo.getModuleName());
    }

    /**
     * {@inheritDoc}
     */
    public void undeployResource(Object resource, String applicationName, String moduleName) throws Exception{
        if(_logger.isLoggable(Level.FINE)) {
            _logger.fine("ConnectorConnectionPoolDeployer : undeployResource : ");
        }
        final org.glassfish.connectors.config.ConnectorConnectionPool
                domainCcp =
                (org.glassfish.connectors.config.ConnectorConnectionPool) resource;
        PoolInfo poolInfo = new PoolInfo(domainCcp.getName(), applicationName, moduleName);
        actualUndeployResource(domainCcp, poolInfo);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void undeployResource(Object resource)
            throws Exception {
        if(_logger.isLoggable(Level.FINE)) {
            _logger.fine("ConnectorConnectionPoolDeployer : undeployResource : ");
        }
        final org.glassfish.connectors.config.ConnectorConnectionPool
                domainCcp =
                (org.glassfish.connectors.config.ConnectorConnectionPool) resource;
        PoolInfo poolInfo = ConnectorsUtil.getPoolInfo(domainCcp);

        actualUndeployResource(domainCcp, poolInfo);
    }

    private void actualUndeployResource(org.glassfish.connectors.config.ConnectorConnectionPool domainCcp,
                                        PoolInfo poolInfo) throws ConnectorRuntimeException {
        if(_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, "Calling backend to delete ConnectorConnectionPool", domainCcp);
        }
        runtime.deleteConnectorConnectionPool(poolInfo);
        if(_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, "Deleted ConnectorConnectionPool in backend", domainCcp);
        }

        /*//unregister the managed object
        if (QUEUE_CF.equals(defName) || TOPIC_CF.equals(defName)) {
            //registers the jsr77 object for the mail resource deployed
            final ManagementObjectManager mgr =
                getAppServerSwitchObject().getManagementObjectManager();
            mgr.unregisterJMSResource(domainCcp.getName());
        }*/
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void redeployResource(Object resource)
            throws Exception {
        //Connector connection pool reconfiguration or
        //change in security maps 
        org.glassfish.connectors.config.ConnectorConnectionPool
                domainCcp =
                (org.glassfish.connectors.config.ConnectorConnectionPool) resource;
        List<SecurityMap> securityMaps = domainCcp.getSecurityMap();

        //Since 8.1 PE/SE/EE, only if pool has already been deployed in this 
        //server-instance earlier, reconfig this pool
        PoolInfo poolInfo = ConnectorsUtil.getPoolInfo(domainCcp);
        if (!runtime.isConnectorConnectionPoolDeployed(poolInfo)) {
            if(_logger.isLoggable(Level.FINE)) {
                _logger.fine("The connector connection pool " + poolInfo
                    + " is either not referred or not yet created in "
                    + "this server instance and pool and hence "
                    + "redeployment is ignored");
            }
            return;
        }


        String rarName = domainCcp.getResourceAdapterName();
        String connDefName = domainCcp.getConnectionDefinitionName();
        List<Property> props = domainCcp.getProperty();
        ConnectorConnectionPool ccp = getConnectorConnectionPool(domainCcp, poolInfo);
        populateConnectorConnectionPool(ccp, connDefName, rarName, props, securityMaps);

        boolean poolRecreateRequired = false;
        try {
            if(_logger.isLoggable(Level.FINE)) {
                _logger.fine("Calling reconfigure pool");
            }
            poolRecreateRequired = runtime.reconfigureConnectorConnectionPool(ccp,
                    new HashSet());
        } catch (ConnectorRuntimeException cre) {
            Object params[] = new Object[]{poolInfo, cre};
            _logger.log(Level.WARNING,"error.reconfiguring.pool", params);
        }

        if (poolRecreateRequired) {
            if(_logger.isLoggable(Level.FINE)) {
                _logger.fine("Pool recreation required");
            }
            runtime.recreateConnectorConnectionPool(ccp);
            if(_logger.isLoggable(Level.FINE)) {
                _logger.fine("Pool recreation done");
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean handles(Object resource){
        return resource instanceof org.glassfish.connectors.config.ConnectorConnectionPool;
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

    /**
     * {@inheritDoc}
     */
    public synchronized void disableResource(Object resource)
            throws Exception {
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void enableResource(Object resource)
            throws Exception {
    }

    private ConnectorConnectionPool getConnectorConnectionPool(
            org.glassfish.connectors.config.ConnectorConnectionPool domainCcp, PoolInfo poolInfo)
            throws Exception {
        ConnectorConnectionPool ccp ;
        ccp = new ConnectorConnectionPool(poolInfo);
        ccp.setSteadyPoolSize(domainCcp.getSteadyPoolSize());
        ccp.setMaxPoolSize(domainCcp.getMaxPoolSize());
        ccp.setMaxWaitTimeInMillis(domainCcp.getMaxWaitTimeInMillis());
        ccp.setPoolResizeQuantity(domainCcp.getPoolResizeQuantity());
        ccp.setIdleTimeoutInSeconds(domainCcp.getIdleTimeoutInSeconds());
        ccp.setFailAllConnections(Boolean.valueOf(domainCcp.getFailAllConnections()));
        ccp.setAuthCredentialsDefinedInPool(
                isAuthCredentialsDefinedInPool(domainCcp));
        //The line below will change for 9.0. We will get this from
        //the domain.xml
        ccp.setConnectionValidationRequired(Boolean.valueOf(domainCcp.getIsConnectionValidationRequired()));

        String txSupport = domainCcp.getTransactionSupport();
        int txSupportIntVal = parseTransactionSupportString(txSupport);

        if (txSupportIntVal == -1) {
            //if transaction-support attribute is null load the value
            //from the ra.xml
            if (_logger.isLoggable(Level.FINE)) {
                _logger.fine("Got transaction-support attr null from domain.xml");
            }
            txSupportIntVal = ConnectionPoolObjectsUtils.getTransactionSupportFromRaXml(
                    domainCcp.getResourceAdapterName());

        } else {
            //We got some valid transaction-support attribute value
            //so go figure if it is valid.
            //The tx support is valid if it is less-than/equal-to
            //the value specified in the ra.xml
            if (!ConnectionPoolObjectsUtils.isTxSupportConfigurationSane(txSupportIntVal,
                    domainCcp.getResourceAdapterName())) {

                String i18nMsg = localStrings.getString("ccp_deployer.incorrect_tx_support");
                ConnectorRuntimeException cre = new
                        ConnectorRuntimeException(i18nMsg);

                _logger.log(Level.SEVERE, "rardeployment.incorrect_tx_support",
                        ccp.getName());
                throw cre;
            }
        }
        if (_logger.isLoggable(Level.FINE)) {
            _logger.fine("setting txSupportVal to " + txSupportIntVal +
                    " in pool " + domainCcp.getName());
        }
        ccp.setTransactionSupport(txSupportIntVal);

        //Always for ccp	
        ccp.setNonComponent(false);
        ccp.setNonTransactional(false);
        ccp.setConnectionLeakTracingTimeout(domainCcp.getConnectionLeakTimeoutInSeconds());
        ccp.setConnectionReclaim(Boolean.valueOf(domainCcp.getConnectionLeakReclaim()));

        ccp.setMatchConnections(Boolean.valueOf(domainCcp.getMatchConnections()));
        ccp.setAssociateWithThread(Boolean.valueOf(domainCcp.getAssociateWithThread()));
        ccp.setPooling(Boolean.valueOf(domainCcp.getPooling()));
        ccp.setPingDuringPoolCreation(Boolean.valueOf(domainCcp.getPing()));
        
        boolean lazyConnectionEnlistment = Boolean.valueOf(domainCcp.getLazyConnectionEnlistment());
        boolean lazyConnectionAssociation = Boolean.valueOf(domainCcp.getLazyConnectionAssociation());

        if (lazyConnectionAssociation) {
            if (lazyConnectionEnlistment) {
                ccp.setLazyConnectionAssoc(true);
                ccp.setLazyConnectionEnlist(true);
            } else {
                _logger.log(Level.SEVERE,
                        "conn_pool_obj_utils.lazy_enlist-lazy_assoc-invalid-combination",
                        domainCcp.getName());
                String i18nMsg = localStrings.getString(
                        "cpou.lazy_enlist-lazy_assoc-invalid-combination",  domainCcp.getName());
                throw new RuntimeException(i18nMsg);
            }
        } else {
            ccp.setLazyConnectionAssoc(lazyConnectionAssociation);
            ccp.setLazyConnectionEnlist(lazyConnectionEnlistment);
        }
        boolean pooling = Boolean.valueOf(domainCcp.getPooling());
        
        //TODO: should this be added to the beginning of this method?
        if(!pooling) {
            //Throw exception if assoc with thread is set to true.
            if(Boolean.valueOf(domainCcp.getAssociateWithThread())) {
                _logger.log(Level.SEVERE, "conn_pool_obj_utils.pooling_disabled_assocwiththread_invalid_combination",
                        domainCcp.getName());
                String i18nMsg = localStrings.getString(
                        "cpou.pooling_disabled_assocwiththread_invalid_combination", domainCcp.getName());
                throw new RuntimeException(i18nMsg);
            }
            
            //Below are useful in pooled environment only.
            //Throw warning for connection validation/validate-atmost-once/
            //match-connections/max-connection-usage-count/idele-timeout
            if(Boolean.valueOf(domainCcp.getIsConnectionValidationRequired())) {
                _logger.log(Level.WARNING, "conn_pool_obj_utils.pooling_disabled_conn_validation_invalid_combination",
                        domainCcp.getName());                
            }
            if(Integer.parseInt(domainCcp.getValidateAtmostOncePeriodInSeconds()) > 0) {
                _logger.log(Level.WARNING, "conn_pool_obj_utils.pooling_disabled_validate_atmost_once_invalid_combination",
                        domainCcp.getName());                                
            }
            if(Boolean.valueOf(domainCcp.getMatchConnections())) {
                _logger.log(Level.WARNING, "conn_pool_obj_utils.pooling_disabled_match_connections_invalid_combination",
                        domainCcp.getName());                                                
            }
            if(Integer.parseInt(domainCcp.getMaxConnectionUsageCount()) > 0) {
                _logger.log(Level.WARNING, "conn_pool_obj_utils.pooling_disabled_max_conn_usage_invalid_combination",
                        domainCcp.getName());                                                                
            }
            if(Integer.parseInt(domainCcp.getIdleTimeoutInSeconds()) > 0) {
                _logger.log(Level.WARNING, "conn_pool_obj_utils.pooling_disabled_idle_timeout_invalid_combination",
                        domainCcp.getName());                
            }
        }
        ccp.setPooling(pooling);

        ccp.setMaxConnectionUsage(domainCcp.getMaxConnectionUsageCount());
        ccp.setValidateAtmostOncePeriod(
                domainCcp.getValidateAtmostOncePeriodInSeconds());

        ccp.setConCreationRetryAttempts(
                domainCcp.getConnectionCreationRetryAttempts());
        ccp.setConCreationRetryInterval(
                domainCcp.getConnectionCreationRetryIntervalInSeconds());

        //IMPORTANT
        //Here all properties that will be checked by the
        //convertElementPropertyToPoolProperty method need to be set to
        //their default values
        convertElementPropertyToPoolProperty(ccp, domainCcp);
        return ccp;
    }

    private void populateConnectorConnectionPool(ConnectorConnectionPool ccp,
                                                 String connectionDefinitionName, String rarName,
                                                 List<Property> props, List<SecurityMap> securityMaps)
            throws ConnectorRuntimeException {

        ConnectorDescriptor connectorDescriptor = runtime.getConnectorDescriptor(rarName);
        if (connectorDescriptor == null) {
            ConnectorRuntimeException cre = new ConnectorRuntimeException("Failed to get connection pool object");
            _logger.log(Level.SEVERE, "rardeployment.connector_descriptor_notfound_registry", rarName);
            _logger.log(Level.SEVERE, "", cre);
            throw cre;
        }
        Set connectionDefs =
                connectorDescriptor.getOutboundResourceAdapter().getConnectionDefs();
        ConnectionDefDescriptor cdd = null;
        Iterator it = connectionDefs.iterator();
        while (it.hasNext()) {
            cdd = (ConnectionDefDescriptor) it.next();
            if (connectionDefinitionName.equals(cdd.getConnectionFactoryIntf()))
                break;

        }
        ConnectorDescriptorInfo cdi = new ConnectorDescriptorInfo();

        cdi.setRarName(rarName);
        cdi.setResourceAdapterClassName(connectorDescriptor.getResourceAdapterClass());
        cdi.setConnectionDefinitionName(cdd.getConnectionFactoryIntf());
        cdi.setManagedConnectionFactoryClass(cdd.getManagedConnectionFactoryImpl());
        cdi.setConnectionFactoryClass(cdd.getConnectionFactoryImpl());
        cdi.setConnectionFactoryInterface(cdd.getConnectionFactoryIntf());
        cdi.setConnectionClass(cdd.getConnectionImpl());
        cdi.setConnectionInterface(cdd.getConnectionIntf());
        Properties properties = new Properties();
        //skip the AddressList in case of JMS RA.
        //Refer Sun Bug :6579154 - Equivalent Oracle Bug :12206278
        if(rarName.trim().equals(ConnectorRuntime.DEFAULT_JMS_ADAPTER)){
            properties.put("AddressList","localhost");
        }
        Set mergedProps = ConnectorDDTransformUtils.mergeProps(props, cdd.getConfigProperties(),properties);
        cdi.setMCFConfigProperties(mergedProps);
        cdi.setResourceAdapterConfigProperties(connectorDescriptor.getConfigProperties());
        ccp.setConnectorDescriptorInfo(cdi);
        ccp.setSecurityMaps(SecurityMapUtils.getConnectorSecurityMaps(securityMaps));

    }

    private int parseTransactionSupportString(String txSupport) {
        return ConnectionPoolObjectsUtils.parseTransactionSupportString(txSupport);
    }

    /**
     * The idea is to convert the ElementProperty values coming from the admin
     * connection pool to standard pool attributes thereby making it
     * easy in case of a reconfig
     */
    public void convertElementPropertyToPoolProperty(ConnectorConnectionPool ccp,
                                                     org.glassfish.connectors.config.ConnectorConnectionPool domainCcp) {
        List<Property> elemProps = domainCcp.getProperty();
        if (elemProps == null) {
            return;
        }
        for (Property ep : elemProps) {
            if (ep != null) {
                if ("MATCHCONNECTIONS".equals(ep.getName().toUpperCase(locale))) {
                    if (_logger.isLoggable(Level.FINE)) {
                        _logger.fine(" ConnectorConnectionPoolDeployer::  Setting matchConnections");
                    }
                    ccp.setMatchConnections(toBoolean(ep.getValue(), true));
                } else if ("LAZYCONNECTIONASSOCIATION".equals(ep.getName().toUpperCase(locale))) {
                    ConnectionPoolObjectsUtils.setLazyEnlistAndLazyAssocProperties(ep.getValue(),
                            domainCcp.getProperty(), ccp);
                    if(_logger.isLoggable(Level.FINE)) {
                        _logger.fine("LAZYCONNECTIONASSOCIATION");
                    }

                } else if ("LAZYCONNECTIONENLISTMENT".equals(ep.getName().toUpperCase(locale))) {
                    ccp.setLazyConnectionEnlist(toBoolean(ep.getValue(), false));
                    if(_logger.isLoggable(Level.FINE)) {
                        _logger.fine("LAZYCONNECTIONENLISTMENT");
                    }

                } else if ("ASSOCIATEWITHTHREAD".equals(ep.getName().toUpperCase(locale))) {
                    ccp.setAssociateWithThread(toBoolean(ep.getValue(), false));
                    if(_logger.isLoggable(Level.FINE)) {
                        _logger.fine("ASSOCIATEWITHTHREAD");
                    }
                } else if ("POOLDATASTRUCTURE".equals(ep.getName().toUpperCase(locale))) {
                    ccp.setPoolDataStructureType(ep.getValue());
                    if(_logger.isLoggable(Level.FINE)) {
                        _logger.fine("POOLDATASTRUCTURE");
                    }

                } else if ("POOLWAITQUEUE".equals(ep.getName().toUpperCase(locale))) {
                    ccp.setPoolWaitQueue(ep.getValue());
                    if(_logger.isLoggable(Level.FINE)) {
                        _logger.fine("POOLWAITQUEUE");
                    }

                } else if ("DATASTRUCTUREPARAMETERS".equals(ep.getName().toUpperCase(locale))) {
                    ccp.setDataStructureParameters(ep.getValue());
                    if(_logger.isLoggable(Level.FINE)) {
                        _logger.fine("DATASTRUCTUREPARAMETERS");
                    }
                } else if ("PREFER-VALIDATE-OVER-RECREATE".equals(ep.getName().toUpperCase(locale))) {
                    String value = ep.getValue();
                    if (_logger.isLoggable(Level.FINE)) {
                        _logger.fine(" ConnectorConnectionPoolDeployer::  " +
                                "Setting PREFER-VALIDATE-OVER-RECREATE to " +
                                value);
                    }
                    ccp.setPreferValidateOverRecreate(toBoolean(value, false));
                }
            }
        }
    }

    private boolean toBoolean(Object prop, boolean defaultVal) {
        if (prop == null) {
            return defaultVal;
        }

        return Boolean.valueOf((String) prop);
    }

    private boolean isAuthCredentialsDefinedInPool(
            org.glassfish.connectors.config.ConnectorConnectionPool domainCcp) {
        List<Property> elemProps = domainCcp.getProperty();
        if (elemProps == null) {
            return false;
        }

        for (Property ep : elemProps) {

            if (ep.getName().equalsIgnoreCase("UserName") ||
                    ep.getName().equalsIgnoreCase("User") ||
                    ep.getName().equalsIgnoreCase("Password")) {
                return true;
            }
        }
        return false;
    }
}

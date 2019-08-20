/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2014 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2016-2017] [Payara Foundation]

package org.glassfish.jdbc.deployer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.glassfish.jdbc.config.JdbcConnectionPool;
import org.glassfish.jdbc.util.JdbcResourcesUtil;
import org.glassfish.jdbc.util.LoggerFactory;
import org.glassfish.resourcebase.resources.api.PoolInfo;
import org.glassfish.resourcebase.resources.api.ResourceConflictException;
import org.glassfish.resourcebase.resources.api.ResourceDeployer;
import org.glassfish.resourcebase.resources.api.ResourceDeployerInfo;
import org.glassfish.resourcebase.resources.api.ResourceInfo;
import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.types.Property;

import com.sun.appserv.connectors.internal.api.ConnectorConstants;
import com.sun.appserv.connectors.internal.api.ConnectorRuntimeException;
import com.sun.appserv.connectors.internal.api.ConnectorsUtil;
import com.sun.enterprise.config.serverbeans.Application;
import com.sun.enterprise.config.serverbeans.BindableResource;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Resource;
import com.sun.enterprise.config.serverbeans.Resources;
import com.sun.enterprise.connectors.ConnectorConnectionPool;
import com.sun.enterprise.connectors.ConnectorDescriptorInfo;
import com.sun.enterprise.connectors.ConnectorRegistry;
import com.sun.enterprise.connectors.ConnectorRuntime;
import com.sun.enterprise.connectors.util.ConnectionPoolObjectsUtils;
import com.sun.enterprise.connectors.util.ResourcesUtil;
import com.sun.enterprise.deployment.ConnectionDefDescriptor;
import com.sun.enterprise.deployment.ConnectorConfigProperty;
import com.sun.enterprise.deployment.ConnectorDescriptor;
import com.sun.enterprise.resource.pool.ResourcePool;
import com.sun.enterprise.resource.pool.waitqueue.PoolWaitQueue;
import com.sun.enterprise.util.i18n.StringManager;
import org.glassfish.config.support.TranslatedConfigView;

/**
 * Handles Jdbc connection pool events in the server instance. When user adds a
 * jdbc connection pool , the admin instance emits resource event. The jdbc
 * connection pool events are propagated to this object.
 * <p/>
 * The methods can potentially be called concurrently, therefore implementation
 * need to be synchronized.
 *
 * @author Tamil Vengan
 */

// This class was created to fix the bug # 4650787

@Service
@ResourceDeployerInfo(JdbcConnectionPool.class)
@Singleton
public class JdbcConnectionPoolDeployer implements ResourceDeployer {

    @Inject
    private ConnectorRuntime runtime;

    @Inject
    @Optional //we need it only in server mode
    private Domain domain;

    static private StringManager sm = StringManager.getManager(
            JdbcConnectionPoolDeployer.class);
    static private String msg = sm.getString("resource.restart_needed");

    static private Logger _logger = LoggerFactory.getLogger(JdbcConnectionPoolDeployer.class);

    private static final Locale locale = Locale.getDefault();

    private ExecutorService execService =
    Executors.newSingleThreadExecutor(new ThreadFactory() {
           @Override
        public Thread newThread(Runnable r) {
             return new Thread(r);
           }
    });

    /**
     * {@inheritDoc}
     */
    @Override
    public void deployResource(Object resource, String applicationName, String moduleName) throws Exception {
        //deployResource is not synchronized as there is only one caller
        //ResourceProxy which is synchronized

        //intentional no-op
        //From 8.1 PE/SE/EE, JDBC connection pools are no more resources and
        //they would be available only to server instances that have a resoruce-ref
        //that maps to a pool. So deploy resource would not be called during
        //JDBC connection pool creation. The actualDeployResource method
        //below is invoked by JdbcResourceDeployer when a resource-ref for a
        //resource that is pointed to this pool is added to a server instance
        JdbcConnectionPool jcp = (JdbcConnectionPool)resource;
        PoolInfo poolInfo = new PoolInfo(jcp.getName(), applicationName, moduleName);
        if(_logger.isLoggable(Level.FINE)){
            _logger.fine(" JdbcConnectionPoolDeployer - deployResource : " + poolInfo + " calling actualDeploy");
        }
        actualDeployResource(resource, poolInfo);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deployResource(Object resource) throws Exception {

        JdbcConnectionPool jcp = (JdbcConnectionPool)resource;
        PoolInfo poolInfo = ConnectorsUtil.getPoolInfo(jcp);
        actualDeployResource(resource, poolInfo);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canDeploy(boolean postApplicationDeployment, Collection<Resource> allResources, Resource resource){
        if(handles(resource)){
            if(!postApplicationDeployment){
                return true;
            }
        }
        return false;
    }

    /**
     * Deploy the resource into the server's runtime naming context
     *
     * @param resource a resource object
     * @throws Exception thrown if fail
     */
    public void actualDeployResource(Object resource, PoolInfo poolInfo) {
        if(_logger.isLoggable(Level.FINE)){
            _logger.fine(" JdbcConnectionPoolDeployer - actualDeployResource : " + poolInfo);
        }
        JdbcConnectionPool adminPool = (JdbcConnectionPool) resource;
        try {
            ConnectorConnectionPool connConnPool = createConnectorConnectionPool(adminPool, poolInfo);
            registerTransparentDynamicReconfigPool(poolInfo, adminPool);
            //now do internal book keeping
            runtime.createConnectorConnectionPool(connConnPool);
        } catch (Exception e) {
            Object params[] = new Object[]{poolInfo, e};
            _logger.log(Level.WARNING, "error.creating.jdbc.pool", params);
        }
    }

    //performance issue related fix : IT 15784
    private void registerTransparentDynamicReconfigPool(PoolInfo poolInfo, JdbcConnectionPool resourcePool) {
        ConnectorRegistry registry = ConnectorRegistry.getInstance();
        if(ConnectorsUtil.isDynamicReconfigurationEnabled(resourcePool)){
            registry.addTransparentDynamicReconfigPool(poolInfo);
        }else{
            registry.removeTransparentDynamicReconfigPool(poolInfo);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void undeployResource(Object resource, String applicationName, String moduleName) throws Exception{
        JdbcConnectionPool jdbcConnPool = (JdbcConnectionPool) resource;
        PoolInfo poolInfo = new PoolInfo(jdbcConnPool.getName(), applicationName, moduleName);
        if(_logger.isLoggable(Level.FINE)) {
            _logger.fine(" JdbcConnectionPoolDeployer - unDeployResource : " +
                "calling actualUndeploy of " + poolInfo);
        }
        actualUndeployResource(poolInfo);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void undeployResource(Object resource) throws Exception {
        JdbcConnectionPool jdbcConnPool = (JdbcConnectionPool) resource;
        PoolInfo poolInfo = ConnectorsUtil.getPoolInfo(jdbcConnPool);
        if(_logger.isLoggable(Level.FINE)) {
            _logger.fine(" JdbcConnectionPoolDeployer - unDeployResource : " +
                "calling actualUndeploy of " + poolInfo);
        }
        actualUndeployResource(poolInfo);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean handles(Object resource){
        return resource instanceof JdbcConnectionPool;
    }

    /**
     * @inheritDoc
     */
    @Override
    public boolean supportsDynamicReconfiguration() {
        return true;
    }

    /**
     * @inheritDoc
     */
    @Override
    public Class[] getProxyClassesForDynamicReconfiguration() {
        return new Class[]{com.sun.appserv.jdbc.DataSource.class};
    }


    /**
     * Undeploy the resource from the server's runtime naming context
     *
     * @param poolInfo a resource object
     * @throws UnsupportedOperationException Currently we are not supporting this method.
     */

    private synchronized void actualUndeployResource(PoolInfo poolInfo) throws Exception {
        runtime.deleteConnectorConnectionPool(poolInfo);
        //performance issue related fix : IT 15784
        ConnectorRegistry.getInstance().removeTransparentDynamicReconfigPool(poolInfo);
        if (_logger.isLoggable(Level.FINEST)) {
            _logger.finest("Pool Undeployed");
        }
    }

    /**
     * Pull out the MCF configuration properties and return them as an array
     * of ConnectorConfigProperty
     *
     * @param adminPool   - The JdbcConnectionPool to pull out properties from
     * @param conConnPool - ConnectorConnectionPool which will be used by Resource Pool
     * @param connDesc    - The ConnectorDescriptor for this JDBC RA
     * @return ConnectorConfigProperty [] array of MCF Config properties specified
     *         in this JDBC RA
     */
    private ConnectorConfigProperty [] getMCFConfigProperties(
            JdbcConnectionPool adminPool,
            ConnectorConnectionPool conConnPool, ConnectorDescriptor connDesc) {

        ArrayList propList = new ArrayList();

        if (adminPool.getResType() != null) {
            if (ConnectorConstants.JAVA_SQL_DRIVER.equals(adminPool.getResType())) {
                propList.add(new ConnectorConfigProperty("ClassName",
                        adminPool.getDriverClassname() == null ? "" : adminPool.getDriverClassname(),
                        "The driver class name", "java.lang.String"));
            } else {
                propList.add(new ConnectorConfigProperty("ClassName",
                        adminPool.getDatasourceClassname() == null ? "" : adminPool.getDatasourceClassname(),
                        "The datasource class name", "java.lang.String"));
            }
        } else {
            //When resType is null, one of these classnames would be specified
            if (adminPool.getDriverClassname() != null) {
                propList.add(new ConnectorConfigProperty("ClassName",
                        adminPool.getDriverClassname() == null ? "" : adminPool.getDriverClassname(),
                        "The driver class name", "java.lang.String"));
            } else if (adminPool.getDatasourceClassname() != null) {
                propList.add(new ConnectorConfigProperty("ClassName",
                        adminPool.getDatasourceClassname() == null ? "" : adminPool.getDatasourceClassname(),
                        "The datasource class name", "java.lang.String"));
            }
        }
        propList.add(new ConnectorConfigProperty("ConnectionValidationRequired",
                adminPool.getIsConnectionValidationRequired() + "",
                "Is connection validation required",
                "java.lang.String"));

        propList.add(new ConnectorConfigProperty("ValidationMethod",
                adminPool.getConnectionValidationMethod() == null ? ""
                : adminPool.getConnectionValidationMethod(),
                "How the connection is validated",
                "java.lang.String"));

        propList.add(new ConnectorConfigProperty("ValidationTableName",
                adminPool.getValidationTableName() == null
                ? "" : adminPool.getValidationTableName(),
                "Validation Table name",
                "java.lang.String"));

        propList.add(new ConnectorConfigProperty("ValidationClassName",
                adminPool.getValidationClassname() == null
                ? "" : adminPool.getValidationClassname(),
                "Validation Class name",
                "java.lang.String"));

        propList.add(new ConnectorConfigProperty("TransactionIsolation",
                adminPool.getTransactionIsolationLevel() == null ? ""
                : adminPool.getTransactionIsolationLevel(),
                "Transaction Isolatin Level",
                "java.lang.String"));

        propList.add(new ConnectorConfigProperty("GuaranteeIsolationLevel",
                adminPool.getIsIsolationLevelGuaranteed() + "",
                "Transaction Isolation Guarantee",
                "java.lang.String"));

        propList.add(new ConnectorConfigProperty("StatementWrapping",
                adminPool.getWrapJdbcObjects() + "",
                "Statement Wrapping",
                "java.lang.String"));
        
        propList.add(new ConnectorConfigProperty("LogJdbcCalls",
                adminPool.getLogJdbcCalls() + "",
                "Log JDBC Calls",
                "java.lang.String"));
                
        propList.add(new ConnectorConfigProperty("SlowQueryThresholdInSeconds",
                adminPool.getSlowQueryThresholdInSeconds() + "",
                "Slow Query Threshold In Seconds",
                "java.lang.String"));

        propList.add(new ConnectorConfigProperty("StatementTimeout",
                adminPool.getStatementTimeoutInSeconds() + "",
                "Statement Timeout",
                "java.lang.String"));

        PoolInfo poolInfo = conConnPool.getPoolInfo();

        propList.add(new ConnectorConfigProperty("PoolMonitoringSubTreeRoot",
                ConnectorsUtil.getPoolMonitoringSubTreeRoot(poolInfo, true) + "",
                "Pool Monitoring Sub Tree Root",
                "java.lang.String"));

        propList.add(new ConnectorConfigProperty("PoolName",
                poolInfo.getName() + "",
                "Pool Name",
                "java.lang.String"));

        if (poolInfo.getApplicationName() != null) {
            propList.add(new ConnectorConfigProperty("ApplicationName",
                    poolInfo.getApplicationName() + "",
                    "Application Name",
                    "java.lang.String"));
        }

        if (poolInfo.getModuleName() != null) {
            propList.add(new ConnectorConfigProperty("ModuleName",
                    poolInfo.getModuleName() + "",
                    "Module name",
                    "java.lang.String"));
        }

        propList.add(new ConnectorConfigProperty("StatementCacheSize",
                adminPool.getStatementCacheSize() + "",
                "Statement Cache Size",
                "java.lang.String"));

        propList.add(new ConnectorConfigProperty("StatementCacheType",
                adminPool.getStatementCacheType() + "",
                "Statement Cache Type",
                "java.lang.String"));

        propList.add(new ConnectorConfigProperty("InitSql",
                adminPool.getInitSql() + "",
                "InitSql",
                "java.lang.String"));

        propList.add(new ConnectorConfigProperty("SqlTraceListeners",
                adminPool.getSqlTraceListeners() + "",
                "Sql Trace Listeners",
                "java.lang.String"));

        propList.add(new ConnectorConfigProperty("StatementLeakTimeoutInSeconds",
                adminPool.getStatementLeakTimeoutInSeconds() + "",
                "Statement Leak Timeout in seconds",
                "java.lang.String"));

        propList.add(new ConnectorConfigProperty("StatementLeakReclaim",
                adminPool.getStatementLeakReclaim() + "",
                "Statement Leak Reclaim",
                "java.lang.String"));

        //dump user defined poperties into the list
        Set connDefDescSet = connDesc.getOutboundResourceAdapter().
                getConnectionDefs();
        //since this a 1.0 RAR, we will have only 1 connDefDesc
        if (connDefDescSet.size() != 1) {
            throw new MissingResourceException("Only one connDefDesc present",
                    null, null);
        }

        Iterator iter = connDefDescSet.iterator();

        //Now get the set of MCF config properties associated with each
        //connection-definition . Each element here is an EnviromnentProperty
        Set mcfConfigProps = null;
        while (iter.hasNext()) {
            mcfConfigProps = ((ConnectionDefDescriptor) iter.next()).getConfigProperties();
        }
        if (mcfConfigProps != null) {

            Map mcfConPropKeys = new HashMap();
            Iterator mcfConfigPropsIter = mcfConfigProps.iterator();
            while (mcfConfigPropsIter.hasNext()) {
                String key = ((ConnectorConfigProperty) mcfConfigPropsIter.next()).getName();
                mcfConPropKeys.put(key.toUpperCase(locale), key);
            }

            String driverProperties = "";
            for (Property rp : adminPool.getProperty()) {
                if (rp == null) {
                    continue;
                }
                String name = rp.getName();

                //The idea here is to convert the Environment Properties coming from
                //the admin connection pool to standard pool properties thereby
                //making it easy to compare in the event of a reconfig
                if ("MATCHCONNECTIONS".equals(name.toUpperCase(locale))) {
                    //JDBC - matchConnections if not set is decided by the ConnectionManager
                    //so default is false
                    conConnPool.setMatchConnections(toBoolean(rp.getValue(), false));
                    logFine("MATCHCONNECTIONS");

                } else if ("ASSOCIATEWITHTHREAD".equals(name.toUpperCase(locale))) {
                    conConnPool.setAssociateWithThread(toBoolean(rp.getValue(), false));
                    logFine("ASSOCIATEWITHTHREAD");

                } else if ("LAZYCONNECTIONASSOCIATION".equals(name.toUpperCase(locale))) {
                    ConnectionPoolObjectsUtils.setLazyEnlistAndLazyAssocProperties(rp.getValue(),
                            adminPool.getProperty(), conConnPool);
                    logFine("LAZYCONNECTIONASSOCIATION");

                } else if ("LAZYCONNECTIONENLISTMENT".equals(name.toUpperCase(Locale.getDefault()))) {
                    conConnPool.setLazyConnectionEnlist(toBoolean(rp.getValue(), false));
                    logFine("LAZYCONNECTIONENLISTMENT");

                } else if ("POOLDATASTRUCTURE".equals(name.toUpperCase(Locale.getDefault()))) {
                    conConnPool.setPoolDataStructureType(rp.getValue());
                    logFine("POOLDATASTRUCTURE");

                } else if (ConnectorConstants.DYNAMIC_RECONFIGURATION_FLAG.equals(name.toLowerCase(locale))) {
                    String value = rp.getValue();
                    try {
                        conConnPool.setDynamicReconfigWaitTimeout(Long.parseLong(rp.getValue()) * 1000L);
                        logFine(ConnectorConstants.DYNAMIC_RECONFIGURATION_FLAG);
                    } catch (NumberFormatException nfe) {
                        _logger.log(Level.WARNING, "Invalid value for "
                                + "'" + ConnectorConstants.DYNAMIC_RECONFIGURATION_FLAG + "' : " + value);
                    }
                } else if ("POOLWAITQUEUE".equals(name.toUpperCase(locale))) {
                    conConnPool.setPoolWaitQueue(rp.getValue());
                    logFine("POOLWAITQUEUE");

                } else if ("DATASTRUCTUREPARAMETERS".equals(name.toUpperCase(locale))) {
                    conConnPool.setDataStructureParameters(rp.getValue());
                    logFine("DATASTRUCTUREPARAMETERS");

                } else if ("USERNAME".equals(name.toUpperCase(Locale.getDefault()))
                        || "USER".equals(name.toUpperCase(locale))) {

                    propList.add(new ConnectorConfigProperty("User", TranslatedConfigView.expandValue(rp.getValue()), "user name", "java.lang.String"));

                } else if ("PASSWORD".equals(name.toUpperCase(locale))) {

                    propList.add(new ConnectorConfigProperty("Password",
                            TranslatedConfigView.expandValue(rp.getValue()), "Password", "java.lang.String"));

                } else if ("JDBC30DATASOURCE".equals(name.toUpperCase(locale))) {

                    propList.add(new ConnectorConfigProperty("JDBC30DataSource",
                            rp.getValue(), "JDBC30DataSource", "java.lang.String"));

                } else if ("PREFER-VALIDATE-OVER-RECREATE".equals(name.toUpperCase(Locale.getDefault()))) {
                    String value = rp.getValue();
                    conConnPool.setPreferValidateOverRecreate(toBoolean(value, false));
                    logFine("PREFER-VALIDATE-OVER-RECREATE : " + value);

                } else if ("STATEMENT-CACHE-TYPE".equals(name.toUpperCase(Locale.getDefault()))) {

                    if(adminPool.getStatementCacheType() != null) {
                                propList.add(new ConnectorConfigProperty("StatementCacheType",
                                        rp.getValue(), "StatementCacheType", "java.lang.String"));
                    }

                } else if ("NUMBER-OF-TOP-QUERIES-TO-REPORT".equals(name.toUpperCase(Locale.getDefault()))) {

                    propList.add(new ConnectorConfigProperty("NumberOfTopQueriesToReport",
                            rp.getValue(), "NumberOfTopQueriesToReport", "java.lang.String"));

                } else if ("TIME-TO-KEEP-QUERIES-IN-MINUTES".equals(name.toUpperCase(Locale.getDefault()))) {

                    propList.add(new ConnectorConfigProperty("TimeToKeepQueriesInMinutes",
                            rp.getValue(), "TimeToKeepQueriesInMinutes", "java.lang.String"));

                } else if ("MAXCACHESIZE".equals(name.toUpperCase(Locale.getDefault())) || "MAX-CACHE-SIZE".equals(name.toUpperCase(Locale.getDefault()))){
                    
                    propList.add(new ConnectorConfigProperty("MaxCacheSize",
                            rp.getValue(), "MaxCacheSize", "java.lang.String"));
                    
                } else if (mcfConPropKeys.containsKey(name.toUpperCase(Locale.getDefault()))) {

                    propList.add(new ConnectorConfigProperty(
                            (String) mcfConPropKeys.get(name.toUpperCase(Locale.getDefault())),
                            rp.getValue() == null ? "" : TranslatedConfigView.expandValue(rp.getValue()),
                            "Some property",
                            "java.lang.String"));
                } else {
                    driverProperties = driverProperties + "set" + escape(name)
                            + "#" + escape(TranslatedConfigView.expandValue(rp.getValue())) + "##";
                }
            }

            if (!driverProperties.equals("")) {
                propList.add(new ConnectorConfigProperty("DriverProperties",
                        driverProperties,
                        "some proprietarty properties",
                        "java.lang.String"));
            }
        }


        propList.add(new ConnectorConfigProperty("Delimiter",
                "#", "delim", "java.lang.String"));

        propList.add(new ConnectorConfigProperty("EscapeCharacter",
                "\\", "escapeCharacter", "java.lang.String"));

        //create an array of EnvironmentProperties from above list
        ConnectorConfigProperty[] eProps = new ConnectorConfigProperty[propList.size()];
        ListIterator propListIter = propList.listIterator();

        for (int i = 0; propListIter.hasNext(); i++) {
            eProps[i] = (ConnectorConfigProperty) propListIter.next();
        }

        return eProps;

    }

    /**
     * To escape the "delimiter" characters that are internally used by Connector & JDBCRA.
     *
     * @param value String that need to be escaped
     * @return Escaped value
     */
    private String escape(String value) {
        CharSequence seq = "\\";
        CharSequence replacement = "\\\\";
        value = value.replace(seq, replacement);

        seq = "#";
        replacement = "\\#";
        value = value.replace(seq, replacement);
        return value;
    }


    private boolean toBoolean(Object prop, boolean defaultVal) {
        if (prop == null) {
            return defaultVal;
        }
        return Boolean.valueOf(((String) prop).toLowerCase(locale));
    }

    /**
     * Use this method if the string being passed does not <br>
     * involve multiple concatenations<br>
     * Avoid using this method in exception-catch blocks as they
     * are not frequently executed <br>
     *
     * @param msg
     */
    private void logFine(String msg) {
        if (_logger.isLoggable(Level.FINE) && msg != null) {
            _logger.fine(msg);
        }
    }

    public ConnectorConnectionPool createConnectorConnectionPool(JdbcConnectionPool adminPool, PoolInfo poolInfo)
            throws ConnectorRuntimeException {

        String moduleName = JdbcResourcesUtil.createInstance().getRANameofJdbcConnectionPool(adminPool);
        int txSupport = getTxSupport(moduleName);

        ConnectorDescriptor connDesc = runtime.getConnectorDescriptor(moduleName);

        //Create the connector Connection Pool object from the configbean object
        ConnectorConnectionPool conConnPool = new ConnectorConnectionPool(poolInfo);

        conConnPool.setTransactionSupport(txSupport);
        setConnectorConnectionPoolAttributes(conConnPool, adminPool);

        //Initially create the ConnectorDescriptor
        ConnectorDescriptorInfo connDescInfo =
                createConnectorDescriptorInfo(connDesc, moduleName);


        connDescInfo.setMCFConfigProperties(
                getMCFConfigProperties(adminPool, conConnPool, connDesc));

        //since we are deploying a 1.0 RAR, this is null
        connDescInfo.setResourceAdapterConfigProperties((Set) null);

        conConnPool.setConnectorDescriptorInfo(connDescInfo);

        return conConnPool;
    }


    private int getTxSupport(String moduleName) {
        if (ConnectorConstants.JDBCXA_RA_NAME.equals(moduleName)) {
           return ConnectionPoolObjectsUtils.parseTransactionSupportString(
               ConnectorConstants.XA_TRANSACTION_TX_SUPPORT_STRING );
        }
        return ConnectionPoolObjectsUtils.parseTransactionSupportString(
                ConnectorConstants.LOCAL_TRANSACTION_TX_SUPPORT_STRING);
    }

    private ConnectorDescriptorInfo createConnectorDescriptorInfo(
            ConnectorDescriptor connDesc, String moduleName) {
        ConnectorDescriptorInfo connDescInfo = new ConnectorDescriptorInfo();

        connDescInfo.setManagedConnectionFactoryClass(
                connDesc.getOutboundResourceAdapter().
                        getManagedConnectionFactoryImpl());

        connDescInfo.setRarName(moduleName);

        connDescInfo.setResourceAdapterClassName(connDesc.
                getResourceAdapterClass());

        connDescInfo.setConnectionDefinitionName(
                connDesc.getOutboundResourceAdapter().getConnectionFactoryIntf());

        connDescInfo.setConnectionFactoryClass(
                connDesc.getOutboundResourceAdapter().getConnectionFactoryImpl());

        connDescInfo.setConnectionFactoryInterface(
                connDesc.getOutboundResourceAdapter().getConnectionFactoryIntf());

        connDescInfo.setConnectionClass(
                connDesc.getOutboundResourceAdapter().getConnectionImpl());

        connDescInfo.setConnectionInterface(
                connDesc.getOutboundResourceAdapter().getConnectionIntf());

        return connDescInfo;
    }

    private void setConnectorConnectionPoolAttributes(ConnectorConnectionPool ccp, JdbcConnectionPool adminPool) {
        String poolName = ccp.getName();
        ccp.setMaxPoolSize(adminPool.getMaxPoolSize());
        ccp.setSteadyPoolSize(adminPool.getSteadyPoolSize());
        ccp.setMaxWaitTimeInMillis(adminPool.getMaxWaitTimeInMillis());

        ccp.setPoolResizeQuantity(adminPool.getPoolResizeQuantity());

        ccp.setIdleTimeoutInSeconds(adminPool.getIdleTimeoutInSeconds());

        ccp.setFailAllConnections(Boolean.valueOf(adminPool.getFailAllConnections()));

        ccp.setConnectionValidationRequired(Boolean.valueOf(adminPool.getIsConnectionValidationRequired()));

        ccp.setNonTransactional(Boolean.valueOf(adminPool.getNonTransactionalConnections()));
        ccp.setNonComponent(Boolean.valueOf(adminPool.getAllowNonComponentCallers()));

        ccp.setPingDuringPoolCreation(Boolean.valueOf(adminPool.getPing()));
        //These are default properties of all Jdbc pools
        //So set them here first and then figure out from the parsing routine
        //if they need to be reset
        ccp.setMatchConnections(Boolean.valueOf(adminPool.getMatchConnections()));
        ccp.setAssociateWithThread(Boolean.valueOf(adminPool.getAssociateWithThread()));
        ccp.setConnectionLeakTracingTimeout(adminPool.getConnectionLeakTimeoutInSeconds());
        ccp.setConnectionReclaim(Boolean.valueOf(adminPool.getConnectionLeakReclaim()));

        boolean lazyConnectionEnlistment = Boolean.valueOf(adminPool.getLazyConnectionEnlistment());
        boolean lazyConnectionAssociation = Boolean.valueOf(adminPool.getLazyConnectionAssociation());

        //lazy-connection-enlistment need to be ON for lazy-connection-association to work.
        if (lazyConnectionAssociation) {
            if (lazyConnectionEnlistment) {
                ccp.setLazyConnectionAssoc(true);
                ccp.setLazyConnectionEnlist(true);
            } else {
                _logger.log(Level.SEVERE, "conn_pool_obj_utils.lazy_enlist-lazy_assoc-invalid-combination",
                        poolName);
                String i18nMsg = sm.getString(
                        "cpou.lazy_enlist-lazy_assoc-invalid-combination", poolName);
                throw new RuntimeException(i18nMsg);
            }
        } else {
            ccp.setLazyConnectionAssoc(lazyConnectionAssociation);
            ccp.setLazyConnectionEnlist(lazyConnectionEnlistment);
        }

        boolean pooling = Boolean.valueOf(adminPool.getPooling());

        if(!pooling) {
            //Throw exception if assoc with thread is set to true.
            if(Boolean.valueOf(adminPool.getAssociateWithThread())) {
                _logger.log(Level.SEVERE, "conn_pool_obj_utils.pooling_disabled_assocwiththread_invalid_combination",
                        poolName);
                String i18nMsg = sm.getString(
                        "cpou.pooling_disabled_assocwiththread_invalid_combination", poolName);
                throw new RuntimeException(i18nMsg);
            }

            //Below are useful in pooled environment only.
            //Throw warning for connection validation/validate-atmost-once/
            //match-connections/max-connection-usage-count/idele-timeout
            if(Boolean.valueOf(adminPool.getIsConnectionValidationRequired())) {
                _logger.log(Level.WARNING, "conn_pool_obj_utils.pooling_disabled_conn_validation_invalid_combination",
                        poolName);
            }
            if(Integer.parseInt(adminPool.getValidateAtmostOncePeriodInSeconds()) > 0) {
                _logger.log(Level.WARNING, "conn_pool_obj_utils.pooling_disabled_validate_atmost_once_invalid_combination",
                        poolName);
            }
            if(Boolean.valueOf(adminPool.getMatchConnections())) {
                _logger.log(Level.WARNING, "conn_pool_obj_utils.pooling_disabled_match_connections_invalid_combination",
                        poolName);
            }
            if(Integer.parseInt(adminPool.getMaxConnectionUsageCount()) > 0) {
                _logger.log(Level.WARNING, "conn_pool_obj_utils.pooling_disabled_max_conn_usage_invalid_combination",
                        poolName);
            }
            if(Integer.parseInt(adminPool.getIdleTimeoutInSeconds()) > 0) {
                _logger.log(Level.WARNING, "conn_pool_obj_utils.pooling_disabled_idle_timeout_invalid_combination",
                        poolName);
            }
        }
        ccp.setPooling(pooling);
        ccp.setMaxConnectionUsage(adminPool.getMaxConnectionUsageCount());

        ccp.setConCreationRetryAttempts(adminPool.getConnectionCreationRetryAttempts());
        ccp.setConCreationRetryInterval(
                adminPool.getConnectionCreationRetryIntervalInSeconds());

        ccp.setValidateAtmostOncePeriod(adminPool.getValidateAtmostOncePeriodInSeconds());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void redeployResource(Object resource) throws Exception {

        final JdbcConnectionPool adminPool = (JdbcConnectionPool) resource;
        PoolInfo poolInfo = ConnectorsUtil.getPoolInfo(adminPool);

        //Only if pool has already been deployed in this server-instance
        //reconfig this pool

        if (!runtime.isConnectorConnectionPoolDeployed(poolInfo)) {
            if(_logger.isLoggable(Level.FINE)) {
                _logger.fine("The JDBC connection pool " + poolInfo
                    + " is not referred or not yet created in this server "
                    + "instance and hence pool redeployment is ignored");
            }
            return;
        }
        final ConnectorConnectionPool connConnPool = createConnectorConnectionPool(adminPool, poolInfo);


        if (connConnPool == null) {
            throw new ConnectorRuntimeException("Unable to create ConnectorConnectionPool" +
                    "from JDBC connection pool");
        }

        //now do internal book keeping
        HashSet excludes = new HashSet();
        //add MCF config props to the set that need to be excluded
        //in checking for the equality of the props with old pool
        excludes.add("TransactionIsolation");
        excludes.add("GuaranteeIsolationLevel");
        excludes.add("ValidationTableName");
        excludes.add("ConnectionValidationRequired");
        excludes.add("ValidationMethod");
        excludes.add("StatementWrapping");
        excludes.add("StatementTimeout");
        excludes.add("ValidationClassName");
        excludes.add("StatementCacheSize");
        excludes.add("StatementCacheType");
        excludes.add("StatementLeakTimeoutInSeconds");
        excludes.add("StatementLeakReclaim");


        try {
            if(_logger.isLoggable(Level.FINEST)) {
                _logger.finest("Calling reconfigure pool");
            }
            boolean requirePoolRecreation = runtime.reconfigureConnectorConnectionPool(connConnPool, excludes);
            if (requirePoolRecreation) {
                if (runtime.isServer() || runtime.isEmbedded()) {
                    handlePoolRecreation(connConnPool);
                }else{
                    recreatePool(connConnPool);
                }
            }
        } catch (ConnectorRuntimeException cre) {
            Object params[] = new Object[]{poolInfo, cre};
            _logger.log(Level.WARNING, "error.redeploying.jdbc.pool", params);
            throw cre;
        }
    }

    private void handlePoolRecreation(final ConnectorConnectionPool connConnPool) throws ConnectorRuntimeException {
        debug("[DRC] Pool recreation required");

        final long reconfigWaitTimeout = connConnPool.getDynamicReconfigWaitTimeout();
        PoolInfo poolInfo = new PoolInfo(connConnPool.getName(), connConnPool.getApplicationName(),
                connConnPool.getModuleName());
        final ResourcePool oldPool = runtime.getPoolManager().getPool(poolInfo);

        if (reconfigWaitTimeout > 0) {

            oldPool.blockRequests(reconfigWaitTimeout);

            if (oldPool.getPoolWaitQueue().getQueueLength() > 0 || oldPool.getPoolStatus().getNumConnUsed() > 0) {

                Runnable thread = new Runnable() {
                    @Override
                    public void run() {
                        try {

                            long numSeconds = 5000L ; //poll every 5 seconds
                            long steps = reconfigWaitTimeout/numSeconds;
                            if(steps == 0){
                                waitForCompletion(steps, oldPool, reconfigWaitTimeout);
                            }else{
                                for (long i = 0; i < steps; i++) {
                                    waitForCompletion(steps, oldPool, reconfigWaitTimeout);
                                    if (oldPool.getPoolWaitQueue().getQueueLength() == 0 &&
                                            oldPool.getPoolStatus().getNumConnUsed() == 0) {
                                        debug("wait-queue is empty and num-con-used is 0");
                                        break;
                                    }
                                }
                            }

                            handlePoolRecreationForExistingProxies(connConnPool);

                            PoolWaitQueue reconfigWaitQueue = oldPool.getReconfigWaitQueue();
                            debug("checking reconfig-wait-queue for notification");
                            if (reconfigWaitQueue.getQueueContents().size() > 0) {
                                for (Object o : reconfigWaitQueue.getQueueContents()) {
                                    debug("notifying reconfig-wait-queue object [ " + o + " ]");
                                    synchronized (o) {
                                        o.notify();
                                    }
                                }
                            }
                        } catch (InterruptedException ie) {
                            if(_logger.isLoggable(Level.FINEST)) {
                                _logger.log(Level.FINEST,
                                    "Interrupted while waiting for all existing clients to return connections to pool", ie);
                            }
                        }

                        if(_logger.isLoggable(Level.FINEST)){
                            _logger.finest("woke-up after giving time for in-use connections to return, " +
                                "WaitQueue-Length : ["+oldPool.getPoolWaitQueue().getQueueContents()+"], " +
                                "Num-Conn-Used : ["+oldPool.getPoolStatus().getNumConnUsed()+"]");
                        }
                    }
                };

                Callable c = Executors.callable(thread);
                ArrayList list = new ArrayList();
                list.add(c);
                try{
                    execService.invokeAll(list);
                }catch(Exception e){
                    Object[] params = new Object[]{connConnPool.getName(), e};
                    _logger.log(Level.WARNING,"exception.redeploying.pool.transparently", params);
                }

            }else{
                handlePoolRecreationForExistingProxies(connConnPool);
            }
        } else if(oldPool.getReconfigWaitTime() > 0){
            //reconfig is being switched off, invalidate proxies
            Collection<BindableResource> resources =
                    JdbcResourcesUtil.getResourcesOfPool(runtime.getResources(oldPool.getPoolInfo()), oldPool.getPoolInfo().getName());
            ConnectorRegistry registry = ConnectorRegistry.getInstance();
            for(BindableResource resource : resources){
                ResourceInfo resourceInfo = ConnectorsUtil.getResourceInfo(resource);
                registry.removeResourceFactories(resourceInfo);
            }
            //recreate the pool now.
            recreatePool(connConnPool);
        }else {
            recreatePool(connConnPool);
        }
    }

    private void waitForCompletion(long steps, ResourcePool oldPool, long totalWaitTime) throws InterruptedException {
        debug("waiting for in-use connections to return to pool or waiting requests to complete");
        try{
            Object poolWaitQueue = oldPool.getPoolWaitQueue();
            synchronized(poolWaitQueue){
                long waitTime = totalWaitTime/steps;
                if(waitTime > 0) {
                    poolWaitQueue.wait(waitTime);
                }
            }
        }catch(InterruptedException ie){
            //ignore
        }
        debug("woke-up to verify in-use / waiting requests list");
    }


    private void handlePoolRecreationForExistingProxies(ConnectorConnectionPool connConnPool) {

        recreatePool(connConnPool);

        Collection<BindableResource> resourcesList ;
        if(!connConnPool.isApplicationScopedResource()){
            resourcesList = JdbcResourcesUtil.getResourcesOfPool(domain.getResources(), connConnPool.getName());
        }else{
            PoolInfo poolInfo = connConnPool.getPoolInfo();
            Resources resources = ResourcesUtil.createInstance().getResources(poolInfo);
            resourcesList = JdbcResourcesUtil.getResourcesOfPool(resources, connConnPool.getName());
        }
        for (BindableResource bindableResource : resourcesList) {

            ResourceInfo resourceInfo = ConnectorsUtil.getResourceInfo(bindableResource);
            ConnectorRegistry.getInstance().updateResourceInfoVersion(resourceInfo);
        }
    }

    private void recreatePool(ConnectorConnectionPool connConnPool) {
        try {
            runtime.recreateConnectorConnectionPool(connConnPool);
            debug("Pool ["+connConnPool.getName()+"] recreation done");
        } catch (ConnectorRuntimeException cre) {
            Object params[] = new Object[]{connConnPool.getName(), cre};
            _logger.log(Level.WARNING, "error.redeploying.jdbc.pool", params);
        }
    }

    /**
     * Enable the resource in the server's runtime naming context
     *
     * @param resource a resource object
     * @exception UnsupportedOperationException Currently we are not supporting this method.
     *
     */
    @Override
    public synchronized void enableResource(Object resource) throws Exception {
        throw new UnsupportedOperationException(msg);
    }

    /**
     * Disable the resource in the server's runtime naming context
     *
     * @param resource a resource object
     * @exception UnsupportedOperationException Currently we are not supporting this method.
     *
     */
    @Override
    public synchronized void disableResource(Object resource) throws Exception {
        throw new UnsupportedOperationException(msg);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validatePreservedResource(Application oldApp, Application newApp, Resource resource,
                                  Resources allResources)
    throws ResourceConflictException {
        //do nothing.
    }

    private void debug(String message){
        if(_logger.isLoggable(Level.FINEST)) {
            _logger.finest("[DRC] : "+ message);
        }
    }
}

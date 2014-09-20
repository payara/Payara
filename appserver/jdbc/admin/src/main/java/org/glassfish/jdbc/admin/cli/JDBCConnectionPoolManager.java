/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jdbc.admin.cli;

import com.sun.appserv.connectors.internal.api.ConnectorsUtil;
import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.util.LocalStringManagerImpl;
import org.glassfish.api.I18n;
import org.glassfish.jdbc.config.JdbcConnectionPool;
import org.glassfish.jdbc.util.JdbcResourcesUtil;
import org.glassfish.resources.admin.cli.ResourceManager;
import org.glassfish.resourcebase.resources.api.ResourceStatus;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;
import org.jvnet.hk2.config.types.Property;

import javax.resource.ResourceException;
import java.beans.PropertyVetoException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.glassfish.resources.admin.cli.ResourceConstants.*;


/**
 * @author Prashanth Abbagani
 */
@Service(name = ServerTags.JDBC_CONNECTION_POOL)
@I18n("add.resources")
public class JDBCConnectionPoolManager implements ResourceManager {

    private static final String DESCRIPTION = ServerTags.DESCRIPTION;

    final private static LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(JDBCConnectionPoolManager.class);

    private String datasourceclassname = null;
    private String restype = null;
    private String steadypoolsize = "8";
    private String maxpoolsize = "32";
    private String maxwait = "60000";
    private String poolresize = "2";
    private String idletimeout = "300";
    private String initsql = null;
    private String isolationlevel = null;
    private String isisolationguaranteed = Boolean.TRUE.toString();
    private String isconnectvalidatereq = Boolean.FALSE.toString();
    private String validationmethod = "table";
    private String validationtable = null;
    private String failconnection = Boolean.FALSE.toString();
    private String allownoncomponentcallers = Boolean.FALSE.toString();
    private String nontransactionalconnections = Boolean.FALSE.toString();
    private String validateAtmostOncePeriod = "0";
    private String connectionLeakTimeout = "0";
    private String connectionLeakReclaim = Boolean.FALSE.toString();
    private String connectionCreationRetryAttempts = "0";
    private String connectionCreationRetryInterval = "10";
    private String driverclassname = null;
    private String sqltracelisteners = null;
    private String statementTimeout = "-1";
    private String statementcachesize = "0";
    private String lazyConnectionEnlistment = Boolean.FALSE.toString();
    private String lazyConnectionAssociation = Boolean.FALSE.toString();
    private String associateWithThread = Boolean.FALSE.toString();
    private String matchConnections = Boolean.FALSE.toString();
    private String maxConnectionUsageCount = "0";
    private String ping = Boolean.FALSE.toString();
    private String pooling = Boolean.TRUE.toString();
    private String validationclassname = null;
    private String wrapJDBCObjects = Boolean.TRUE.toString();

    private String description = null;
    private String jdbcconnectionpoolid = null;

    public JDBCConnectionPoolManager() {
    }

    public String getResourceType() {
        return ServerTags.JDBC_CONNECTION_POOL;
    }

    public ResourceStatus create(Resources resources, HashMap attributes, final Properties properties,
                                 String target) throws Exception {
        setAttributes(attributes);

        ResourceStatus validationStatus = isValid(resources);
        if(validationStatus.getStatus() == ResourceStatus.FAILURE){
            return validationStatus;
        }

        try {
            ConfigSupport.apply(new SingleConfigCode<Resources>() {

                public Object run(Resources param) throws PropertyVetoException, TransactionFailure {
                    return createResource(param, properties);
                }
            }, resources);

        } catch (TransactionFailure tfe) {
            String msg = localStrings.getLocalString(
                    "create.jdbc.connection.pool.fail", "JDBC connection pool {0} create failed: {1}",
                    jdbcconnectionpoolid, tfe.getMessage());
            return new ResourceStatus(ResourceStatus.FAILURE, msg);
        }
        String msg = localStrings.getLocalString(
                "create.jdbc.connection.pool.success", "JDBC connection pool {0} created successfully",
                jdbcconnectionpoolid);
        return new ResourceStatus(ResourceStatus.SUCCESS, msg);
    }

    private ResourceStatus isValid(Resources resources){
        ResourceStatus status = new ResourceStatus(ResourceStatus.SUCCESS, "Validation Successful");
        if (jdbcconnectionpoolid == null) {
            String msg = localStrings.getLocalString("add.resources.noJdbcConnectionPoolId",
                    "No pool name defined for JDBC Connection pool.");
            return new ResourceStatus(ResourceStatus.FAILURE, msg);
        }
        // ensure we don't already have one of this name
        for (ResourcePool pool : resources.getResources(ResourcePool.class)) {
            if (pool.getName().equals(jdbcconnectionpoolid)) {
                String msg = localStrings.getLocalString("create.jdbc.connection.pool.duplicate",
                        "A resource {0} already exists.", jdbcconnectionpoolid);
                return new ResourceStatus(ResourceStatus.FAILURE, msg, true);
            }
        }

        if ("table".equals(this.validationmethod)
                && Boolean.TRUE.toString().equals(this.isconnectvalidatereq)
                && this.validationtable == null) {
            String msg = localStrings.getLocalString("create.jdbc.connection.pool.validationtable_required",
                    "--validationtable is required if --validationmethod=table " +
                            "and --isconnectvalidatereq=true.");
            return new ResourceStatus(ResourceStatus.FAILURE, msg, true);
        }
        return status;
    }

    private JdbcConnectionPool createResource(Resources param, Properties properties) throws PropertyVetoException,
            TransactionFailure {
        JdbcConnectionPool newResource = createConfigBean(param, properties);
        param.getResources().add(newResource);
        return newResource;
    }

    private JdbcConnectionPool createConfigBean(Resources param, Properties properties) throws PropertyVetoException,
            TransactionFailure {
        JdbcConnectionPool newResource = param.createChild(JdbcConnectionPool.class);
        newResource.setWrapJdbcObjects(wrapJDBCObjects);
        if (validationtable != null)
            newResource.setValidationTableName(validationtable);
        newResource.setValidateAtmostOncePeriodInSeconds(validateAtmostOncePeriod);
        if (isolationlevel != null) {
            newResource.setTransactionIsolationLevel(isolationlevel);
        }
        newResource.setSteadyPoolSize(steadypoolsize);
        if(statementTimeout != null){
            newResource.setStatementTimeoutInSeconds(statementTimeout);
        }
        if (restype != null) {
            newResource.setResType(restype);
        }
        newResource.setPoolResizeQuantity(poolresize);
        newResource.setNonTransactionalConnections(nontransactionalconnections);
        newResource.setMaxWaitTimeInMillis(maxwait);
        newResource.setMaxPoolSize(maxpoolsize);
        if(maxConnectionUsageCount != null){
            newResource.setMaxConnectionUsageCount(maxConnectionUsageCount);
        }
        if(matchConnections != null){
            newResource.setMatchConnections(matchConnections);
        }
        if(lazyConnectionEnlistment != null){
            newResource.setLazyConnectionEnlistment(lazyConnectionEnlistment);
        }
        if(lazyConnectionAssociation != null){
            newResource.setLazyConnectionAssociation(lazyConnectionAssociation);
        }
        newResource.setIsIsolationLevelGuaranteed(isisolationguaranteed);
        newResource.setIsConnectionValidationRequired(isconnectvalidatereq);
        newResource.setIdleTimeoutInSeconds(idletimeout);
        newResource.setFailAllConnections(failconnection);
        if (datasourceclassname != null) {
            newResource.setDatasourceClassname(datasourceclassname);
        }
        newResource.setConnectionValidationMethod(validationmethod);
        if(connectionLeakTimeout != null){
            newResource.setConnectionLeakTimeoutInSeconds(connectionLeakTimeout);
        }
        if(connectionLeakReclaim != null){
            newResource.setConnectionLeakReclaim(connectionLeakReclaim);
        }
        if(connectionCreationRetryInterval != null){
            newResource.setConnectionCreationRetryIntervalInSeconds(connectionCreationRetryInterval);
        }
        if(connectionCreationRetryAttempts != null){
            newResource.setConnectionCreationRetryAttempts(connectionCreationRetryAttempts);
        }
        if(associateWithThread != null){
            newResource.setAssociateWithThread(associateWithThread);
        }
        if(allownoncomponentcallers != null){
            newResource.setAllowNonComponentCallers(allownoncomponentcallers);
        }
        if(statementcachesize != null){
            newResource.setStatementCacheSize(statementcachesize);
        }
        if (validationclassname != null) {
            newResource.setValidationClassname(validationclassname);
        }
        if(initsql != null){
            newResource.setInitSql(initsql);
        }
        if (sqltracelisteners != null) {
            newResource.setSqlTraceListeners(sqltracelisteners);
        }
        if(pooling != null){
            newResource.setPooling(pooling);
        }
        if(ping != null){
            newResource.setPing(ping);
        }
        if (driverclassname != null) {
            newResource.setDriverClassname(driverclassname);
        }
        if (description != null) {
            newResource.setDescription(description);
        }
        newResource.setName(jdbcconnectionpoolid);
        if (properties != null) {
            for (Map.Entry e : properties.entrySet()) {
                Property prop = newResource.createChild(Property.class);
                prop.setName((String) e.getKey());
                prop.setValue((String) e.getValue());
                newResource.getProperty().add(prop);
            }
        }
        return newResource;
    }

    public void setAttributes(HashMap attrList) {
        datasourceclassname = (String) attrList.get(DATASOURCE_CLASS);
        restype = (String) attrList.get(RES_TYPE);
        steadypoolsize = (String) attrList.get(STEADY_POOL_SIZE);
        maxpoolsize = (String) attrList.get(MAX_POOL_SIZE);
        maxwait = (String) attrList.get(MAX_WAIT_TIME_IN_MILLIS);
        poolresize = (String) attrList.get(POOL_SIZE_QUANTITY);
        idletimeout = (String) attrList.get(IDLE_TIME_OUT_IN_SECONDS);
        isolationlevel = (String) attrList.get(TRANS_ISOLATION_LEVEL);
        isisolationguaranteed = (String) attrList.get(IS_ISOLATION_LEVEL_GUARANTEED);
        isconnectvalidatereq = (String) attrList.get(IS_CONNECTION_VALIDATION_REQUIRED);
        validationmethod = (String) attrList.get(CONNECTION_VALIDATION_METHOD);
        validationtable = (String) attrList.get(VALIDATION_TABLE_NAME);
        failconnection = (String) attrList.get(FAIL_ALL_CONNECTIONS);
        allownoncomponentcallers = (String) attrList.get(ALLOW_NON_COMPONENT_CALLERS);
        nontransactionalconnections = (String) attrList.get(NON_TRANSACTIONAL_CONNECTIONS);
        validateAtmostOncePeriod = (String) attrList.get(VALIDATE_ATMOST_ONCE_PERIOD_IN_SECONDS);
        connectionLeakTimeout = (String) attrList.get(CONNECTION_LEAK_TIMEOUT_IN_SECONDS);
        connectionLeakReclaim = (String) attrList.get(CONNECTION_LEAK_RECLAIM);
        connectionCreationRetryAttempts = (String) attrList.get(CONNECTION_CREATION_RETRY_ATTEMPTS);
        connectionCreationRetryInterval = (String) attrList.get(CONNECTION_CREATION_RETRY_INTERVAL_IN_SECONDS);
        statementTimeout = (String) attrList.get(STATEMENT_TIMEOUT_IN_SECONDS);
        lazyConnectionEnlistment = (String) attrList.get(LAZY_CONNECTION_ENLISTMENT);
        lazyConnectionAssociation = (String) attrList.get(LAZY_CONNECTION_ASSOCIATION);
        associateWithThread = (String) attrList.get(ASSOCIATE_WITH_THREAD);
        matchConnections = (String) attrList.get(MATCH_CONNECTIONS);
        maxConnectionUsageCount = (String) attrList.get(MAX_CONNECTION_USAGE_COUNT);
        wrapJDBCObjects = (String) attrList.get(WRAP_JDBC_OBJECTS);
        description = (String) attrList.get(DESCRIPTION);
        jdbcconnectionpoolid = (String) attrList.get(CONNECTION_POOL_NAME);
        statementcachesize = (String) attrList.get(STATEMENT_CACHE_SIZE);
        validationclassname = (String) attrList.get(VALIDATION_CLASSNAME);
        initsql = (String) attrList.get(INIT_SQL);
        sqltracelisteners = (String) attrList.get(SQL_TRACE_LISTENERS);
        pooling = (String) attrList.get(POOLING);
        ping = (String) attrList.get(PING);
        driverclassname = (String) attrList.get(DRIVER_CLASSNAME);
    }

    public ResourceStatus delete(Iterable<Server> servers, Iterable<Cluster> clusters, final Resources resources, final String cascade,
                                 final String poolName) throws Exception {

        if (poolName == null) {
            String msg = localStrings.getLocalString("jdbcConnPool.resource.noJndiName",
                    "No id defined for JDBC Connection pool.");
            return new ResourceStatus(ResourceStatus.FAILURE, msg);
        }

        // ensure we already have this resource
        if (!isResourceExists(resources, poolName)) {
            String msg = localStrings.getLocalString("delete.jdbc.connection.pool.notfound",
                    "A JDBC connection pool named {0} does not exist.", poolName);
            return new ResourceStatus(ResourceStatus.FAILURE, msg);
        }

        try {

            // if cascade=true delete all the resources associated with this pool 
            // if cascade=false don't delete this connection pool if a resource is referencing it
            Object obj = deleteAssociatedResources(servers, clusters, resources,
                    Boolean.parseBoolean(cascade), poolName);
            if (obj instanceof Integer &&
                    (Integer) obj == ResourceStatus.FAILURE) {
                String msg = localStrings.getLocalString(
                        "delete.jdbc.connection.pool.pool_in_use",
                        "JDBC Connection pool {0} delete failed ", poolName);
                return new ResourceStatus(ResourceStatus.FAILURE, msg);
            }

            // delete jdbc connection pool
            if (ConfigSupport.apply(new SingleConfigCode<Resources>() {
                public Object run(Resources param) throws PropertyVetoException, TransactionFailure {
                    JdbcConnectionPool cp = (JdbcConnectionPool)
                            ConnectorsUtil.getResourceByName(resources, JdbcConnectionPool.class, poolName);
                    return param.getResources().remove(cp);
                }
            }, resources) == null) {
                String msg = localStrings.getLocalString("delete.jdbc.connection.pool.notfound",
                        "A JDBC connection pool named {0} does not exist.", poolName);
                return new ResourceStatus(ResourceStatus.FAILURE, msg);
            }

        } catch (TransactionFailure tfe) {
            String msg = tfe.getMessage() != null ? tfe.getMessage() :
                    localStrings.getLocalString("jdbcConnPool.resource.deletionFailed",
                            "JDBC Connection pool {0} delete failed ", poolName);
            ResourceStatus status = new ResourceStatus(ResourceStatus.FAILURE, msg);
            status.setException(tfe);
            return status;
        }

        String msg = localStrings.getLocalString("jdbcConnPool.resource.deleteSuccess",
                "JDBC Connection pool {0} deleted successfully", poolName);
        return new ResourceStatus(ResourceStatus.SUCCESS, msg);
    }

    private boolean isResourceExists(Resources resources, String poolName) {
        return ConnectorsUtil.getResourceByName(resources, JdbcConnectionPool.class, poolName) != null;
    }

    private Object deleteAssociatedResources(final Iterable<Server> servers, final Iterable<Cluster> clusters, Resources resources,
                                           final boolean cascade, final String poolName) throws TransactionFailure {
        if (cascade) {
            ConfigSupport.apply(new SingleConfigCode<Resources>() {
                public Object run(Resources param) throws PropertyVetoException, TransactionFailure {
                    Collection<BindableResource> referringResources = JdbcResourcesUtil.getResourcesOfPool(param, poolName);
                    for (BindableResource referringResource : referringResources) {
                        // delete resource-refs
                        deleteServerResourceRefs(servers, referringResource.getJndiName());
                        deleteClusterResourceRefs(clusters, referringResource.getJndiName());
                        // remove the resource
                        param.getResources().remove(referringResource);
                    }
                    return true; //no-op
                }
            }, resources);
        }else{
            Collection<BindableResource> referringResources = JdbcResourcesUtil.getResourcesOfPool(resources, poolName);
            if(referringResources.size() > 0){
                return ResourceStatus.FAILURE;
            }
        }
        return true; //no-op
    }

    private void deleteServerResourceRefs(Iterable<Server> servers, final String refName)
            throws TransactionFailure {
        if(servers != null){
            for (Server server : servers) {
                server.deleteResourceRef(refName);
            }
        }
    }

    private void deleteClusterResourceRefs(Iterable<Cluster> clusters, final String refName)
            throws TransactionFailure {
        if(clusters != null){
            for (Cluster cluster : clusters) {
                cluster.deleteResourceRef(refName);
            }
        }
    }

    public Resource createConfigBean(Resources resources, HashMap attributes, Properties properties, boolean validate)
            throws Exception {
        setAttributes(attributes);
        ResourceStatus status = null;
        if(!validate){
            status = new ResourceStatus(ResourceStatus.SUCCESS,"");
        }else{
            status = isValid(resources);
        }
        if(status.getStatus() == ResourceStatus.SUCCESS){
            return createConfigBean(resources, properties);
        }else{
            throw new ResourceException(status.getMessage());
        }
    }
}

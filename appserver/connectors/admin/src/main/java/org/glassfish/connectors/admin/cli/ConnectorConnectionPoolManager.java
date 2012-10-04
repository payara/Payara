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

package org.glassfish.connectors.admin.cli;

import com.sun.appserv.connectors.internal.api.ConnectorRuntime;
import com.sun.appserv.connectors.internal.api.ConnectorRuntimeException;
import com.sun.appserv.connectors.internal.api.ConnectorsUtil;
import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.util.LocalStringManagerImpl;
import org.glassfish.api.I18n;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.connectors.config.ConnectorConnectionPool;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.resources.admin.cli.ResourceManager;
import org.glassfish.resourcebase.resources.api.ResourceStatus;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;
import org.jvnet.hk2.config.types.Property;

import javax.inject.Inject;
import javax.resource.ResourceException;
import java.beans.PropertyVetoException;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.sun.appserv.connectors.internal.api.ConnectorConstants.EMBEDDEDRAR_NAME_DELIMITER;
import static org.glassfish.resources.admin.cli.ResourceConstants.*;


/**
 *
 * @author Jennifer Chou, Jagadish Ramu
 */
@Service (name=ServerTags.CONNECTOR_CONNECTION_POOL)
@PerLookup
@I18n("create.connector.connection.pool")
public class ConnectorConnectionPoolManager implements ResourceManager {

    @Inject
    private Applications applications;

    @Inject
    private ConnectorRuntime connectorRuntime;

    @Inject
    private ServerEnvironment environment;

    private static final String DESCRIPTION = ServerTags.DESCRIPTION;

    final private static LocalStringManagerImpl localStrings = 
        new LocalStringManagerImpl(ConnectorConnectionPoolManager.class);

    private String raname = null;
    private String connectiondefinition = null;
    private String steadypoolsize = "8";
    private String maxpoolsize = "32";
    private String maxwait = "60000";
    private String poolresize = "2";
    private String idletimeout = "300";
    private String isconnectvalidatereq = Boolean.FALSE.toString();
    private String failconnection = Boolean.FALSE.toString();
    private String validateAtmostOncePeriod = "0";
    private String connectionLeakTimeout = "0";
    private String connectionLeakReclaim = Boolean.FALSE.toString();
    private String connectionCreationRetryAttempts = "0";
    private String connectionCreationRetryInterval = "10";
    private String lazyConnectionEnlistment = Boolean.FALSE.toString();
    private String lazyConnectionAssociation = Boolean.FALSE.toString();
    private String associateWithThread = Boolean.FALSE.toString();
    private String matchConnections = Boolean.FALSE.toString();
    private String maxConnectionUsageCount = "0";
    private String ping = Boolean.FALSE.toString();
    private String pooling = Boolean.TRUE.toString();
    private String transactionSupport = null;

    private String description = null;
    private String poolname = null;

    public ConnectorConnectionPoolManager() {
    }

    public String getResourceType() {
        return ServerTags.CONNECTOR_CONNECTION_POOL;
    }

    public ResourceStatus create(Resources resources, HashMap attributes, final Properties properties,
                                 String target) throws Exception {
        setParams(attributes);

        ResourceStatus validationStatus = isValid(resources, true);
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
            Logger.getLogger(ConnectorConnectionPoolManager.class.getName()).log(Level.SEVERE,
                    "create-connector-connection-pool failed", tfe);
            String msg = localStrings.getLocalString(
                    "create.connector.connection.pool.fail", "Connector connection pool {0} create failed: {1}",
                    poolname) + " " + tfe.getLocalizedMessage();
            return new ResourceStatus(ResourceStatus.FAILURE, msg);
        }

        String msg = localStrings.getLocalString(
                "create.connector.connection.pool.success", "Connector connection pool {0} created successfully",
                poolname);
        return new ResourceStatus(ResourceStatus.SUCCESS, msg);

    }

    private ResourceStatus isValid(Resources resources, boolean requiresNewTransaction){
        ResourceStatus status = new ResourceStatus(ResourceStatus.SUCCESS, "Validation Successful");
        if (poolname == null) {
            String msg = localStrings.getLocalString("create.connector.connection.pool.noJndiName",
                            "No pool name defined for connector connection pool.");
            return new ResourceStatus(ResourceStatus.FAILURE, msg);
        }
        // ensure we don't already have one of this name
        if(ConnectorsUtil.getResourceByName(resources, ConnectorConnectionPool.class, poolname) != null){
            String errMsg = localStrings.getLocalString("create.connector.connection.pool.duplicate",
                    "A resource named {0} already exists.", poolname);
            return new ResourceStatus(ResourceStatus.FAILURE, errMsg);
        }

        //no need to validate in remote instance as the validation would have happened in DAS.
        if(environment.isDas() && requiresNewTransaction){

            if (applications == null) {
                String msg = localStrings.getLocalString("noApplications",
                        "No applications found.");
                return new ResourceStatus(ResourceStatus.FAILURE, msg);
            }

            try {
                status = validateConnectorConnPoolAttributes(raname, connectiondefinition);
                if (status.getStatus() == ResourceStatus.FAILURE) {
                    return status;
                }
            } catch(ConnectorRuntimeException cre) {
                Logger.getLogger(ConnectorConnectionPoolManager.class.getName()).log(Level.SEVERE,
                        "Could not find connection definitions from ConnectorRuntime for resource adapter "+ raname, cre);
                String msg = localStrings.getLocalString(
                      "create.connector.connection.pool.noConnDefs",
                      "Could not find connection definitions for resource adapter {0}",
                      raname) + " " + cre.getLocalizedMessage();
                return new ResourceStatus(ResourceStatus.FAILURE, msg);
            }
        }
        return status;
    }

    private ConnectorConnectionPool createResource(Resources param, Properties properties) throws PropertyVetoException,
            TransactionFailure {
        ConnectorConnectionPool newResource = createConfigBean(param, properties);
        param.getResources().add(newResource);
        return newResource;
    }


    private ConnectorConnectionPool createConfigBean(Resources param, Properties properties) throws PropertyVetoException,
            TransactionFailure {
        ConnectorConnectionPool newResource = param.createChild(ConnectorConnectionPool.class);

        newResource.setResourceAdapterName(raname);
        newResource.setConnectionDefinitionName(connectiondefinition);
        if(validateAtmostOncePeriod != null){
            newResource.setValidateAtmostOncePeriodInSeconds(validateAtmostOncePeriod);
        }
        newResource.setSteadyPoolSize(steadypoolsize);
        newResource.setPoolResizeQuantity(poolresize);
        newResource.setMaxWaitTimeInMillis(maxwait);
        newResource.setMaxPoolSize(maxpoolsize);
        if(maxConnectionUsageCount != null){
            newResource.setMaxConnectionUsageCount(maxConnectionUsageCount);
        }
        newResource.setMatchConnections(matchConnections);
        if(lazyConnectionEnlistment != null){
            newResource.setLazyConnectionEnlistment(lazyConnectionEnlistment);
        }
        if(lazyConnectionAssociation != null){
            newResource.setLazyConnectionAssociation(lazyConnectionAssociation);
        }
        if(isconnectvalidatereq != null){
            newResource.setIsConnectionValidationRequired(isconnectvalidatereq);
        }
        newResource.setIdleTimeoutInSeconds(idletimeout);
        newResource.setFailAllConnections(failconnection);
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
        if(pooling != null){
            newResource.setPooling(pooling);
        }
        if(ping != null){
            newResource.setPing(ping);
        }
        if (transactionSupport != null) {
            newResource.setTransactionSupport(transactionSupport);
        }
        if (description != null) {
            newResource.setDescription(description);
        }
        newResource.setName(poolname);
        if (properties != null) {
            for ( java.util.Map.Entry e : properties.entrySet()) {
                Property prop = newResource.createChild(Property.class);
                prop.setName((String)e.getKey());
                prop.setValue((String)e.getValue());
                newResource.getProperty().add(prop);
            }
        }
        return newResource;
    }

    public void setParams(HashMap attrList) {
        raname = (String) attrList.get(RES_ADAPTER_NAME);
        connectiondefinition = (String) attrList.get(CONN_DEF_NAME);
        steadypoolsize = (String) attrList.get(STEADY_POOL_SIZE);
        maxpoolsize = (String) attrList.get(MAX_POOL_SIZE);
        maxwait = (String) attrList.get(MAX_WAIT_TIME_IN_MILLIS);
        poolresize = (String) attrList.get(POOL_SIZE_QUANTITY);
        idletimeout = (String) attrList.get(IDLE_TIME_OUT_IN_SECONDS);
        isconnectvalidatereq = (String) attrList.get(IS_CONNECTION_VALIDATION_REQUIRED);
        failconnection = (String) attrList.get(FAIL_ALL_CONNECTIONS);
        validateAtmostOncePeriod = (String) attrList.get(VALIDATE_ATMOST_ONCE_PERIOD_IN_SECONDS);
        connectionLeakTimeout = (String) attrList.get(CONNECTION_LEAK_TIMEOUT_IN_SECONDS);
        connectionLeakReclaim = (String) attrList.get(CONNECTION_LEAK_RECLAIM);
        connectionCreationRetryAttempts = (String) attrList.get(CONNECTION_CREATION_RETRY_ATTEMPTS);
        connectionCreationRetryInterval = (String) attrList.get(CONNECTION_CREATION_RETRY_INTERVAL_IN_SECONDS);
        lazyConnectionEnlistment = (String) attrList.get(LAZY_CONNECTION_ENLISTMENT);
        lazyConnectionAssociation = (String) attrList.get(LAZY_CONNECTION_ASSOCIATION);
        associateWithThread = (String) attrList.get(ASSOCIATE_WITH_THREAD);
        matchConnections = (String) attrList.get(MATCH_CONNECTIONS);
        maxConnectionUsageCount = (String) attrList.get(MAX_CONNECTION_USAGE_COUNT);
        description = (String) attrList.get(DESCRIPTION);
        poolname = (String) attrList.get(CONNECTOR_CONNECTION_POOL_NAME);
        pooling = (String) attrList.get(POOLING);
        ping = (String) attrList.get(PING);
        transactionSupport = (String) attrList.get(CONN_TRANSACTION_SUPPORT);
    }
    
    private ResourceStatus validateConnectorConnPoolAttributes(String raName, String connDef)
            throws ConnectorRuntimeException {
        ResourceStatus status = isValidRAName(raName);
        if(status.getStatus() == ResourceStatus.SUCCESS) {
            if(!isValidConnectionDefinition(connDef,raName)) {

                String msg = localStrings.getLocalString("admin.mbeans.rmb.invalid_ra_connectdef_not_found",
                            "Invalid connection definition. Connector Module with connection definition {0} not found.", connDef);
                status = new ResourceStatus(ResourceStatus.FAILURE, msg);
            }
        }
        return status;
    }

    private ResourceStatus isValidRAName(String raName) {
        //TODO turn on validation.  For now, turn validation off until connector modules ready
        //boolean retVal = false;
        ResourceStatus status = new ResourceStatus(ResourceStatus.SUCCESS, "");

        if ((raName == null) || (raName.equals(""))) {
            String msg = localStrings.getLocalString("admin.mbeans.rmb.null_res_adapter",
                    "Resource Adapter Name is null.");
            status = new ResourceStatus(ResourceStatus.FAILURE, msg);
        } else {
            // To check for embedded connector module
            // System RA, so don't validate
            if (!ConnectorsUtil.getNonJdbcSystemRars().contains(raName)) {
                // Check if the raName contains double underscore or hash.
                // If that is the case then this is the case of an embedded rar,
                // hence look for the application which embeds this rar,
                // otherwise look for the webconnector module with this raName.

                int indx = raName.indexOf(EMBEDDEDRAR_NAME_DELIMITER);
                if (indx != -1) {
                    String appName = raName.substring(0, indx);
                    Application app = applications.getModule(Application.class, appName);
                    if (app == null) {
                        String msg = localStrings.getLocalString("admin.mbeans.rmb.invalid_ra_app_not_found",
                                "Invalid raname. Application with name {0} not found.", appName);
                        status = new ResourceStatus(ResourceStatus.FAILURE, msg);
                    }
                } else {
                    Application app = applications.getModule(Application.class, raName);
                    if (app == null) {
                        String msg = localStrings.getLocalString("admin.mbeans.rmb.invalid_ra_cm_not_found",
                                "Invalid raname. Connector Module with name {0} not found.", raName);
                        status = new ResourceStatus(ResourceStatus.FAILURE, msg);
                    }
                }
            }
        }

        return status;
    }

    private boolean isValidConnectionDefinition(String connectionDef,String raName)
            throws ConnectorRuntimeException {
        String[] names = connectorRuntime.getConnectionDefinitionNames(raName);
        for(int i = 0; i < names.length; i++) {
            if(names[i].equals(connectionDef)) {
                return true;
            }
        }
        return false;
    }
    public Resource createConfigBean(Resources resources, HashMap attributes, Properties properties, boolean validate)
            throws Exception{
        setParams(attributes);
        ResourceStatus status = null;
        if(!validate){
            status = new ResourceStatus(ResourceStatus.SUCCESS,"");
        }else{
            status = isValid(resources, false);
        }
        if(status.getStatus() == ResourceStatus.SUCCESS){
            return createConfigBean(resources, properties);
        }else{
            throw new ResourceException(status.getMessage());
        }
    }
}

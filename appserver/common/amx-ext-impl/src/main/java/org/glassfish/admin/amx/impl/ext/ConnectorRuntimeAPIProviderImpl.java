/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admin.amx.impl.ext;

import java.util.Map;
import javax.management.ObjectName;

import java.util.HashMap;
import java.util.Set;

import org.glassfish.resource.common.PoolInfo;
import org.glassfish.admin.amx.base.ConnectorRuntimeAPIProvider;
import org.glassfish.admin.amx.impl.mbean.AMXImplBase;
import org.glassfish.admin.amx.util.ExceptionUtil;
import org.jvnet.hk2.component.ComponentException;
import org.jvnet.hk2.component.Habitat;

import com.sun.appserv.connectors.internal.api.ConnectorRuntime;
import com.sun.appserv.connectors.internal.api.ConnectorRuntimeException;
import com.sun.appserv.connectors.internal.api.ConnectorConstants;

import javax.resource.ResourceException;

/**
 * ConnectorRuntime exposed APIs via AMX
 */
public final class ConnectorRuntimeAPIProviderImpl extends AMXImplBase
// implements Runtime
{
    private final Habitat mHabitat;

    public ConnectorRuntimeAPIProviderImpl(final ObjectName parent, Habitat habitat)
    {
        super(parent, ConnectorRuntimeAPIProvider.class);

        if (habitat != null)
        {
            mHabitat = habitat;
        }
        else
        {
            throw new IllegalStateException("Habitat is null");
        }
    }

    /**
     * Obtain properties of the JDBC DataSource/Driver. This is used by the 
     * administration console to list all the additional properties for the
     * particular resource type.
     * @param datasourceClassName
     * @param resType one of javax.sql.DataSource, javax.sql.XADataSource,
     * javax.sql.ConnectionPoolDataSource, java.sql.Driver.
     * @return a map containing a CONN_DEFINITION_PROPS_KEY with a map of
     * connection definition properties with the default values. 
     * If CONN_DEFINITION_PROPS_KEY is null, an exception has occured and 
     * REASON_FAILED_KEY would give the reason
     * why getting connection definition properties and its defaults failed. 
     */
    public Map<String, Object> getConnectionDefinitionPropertiesAndDefaults(
            final String datasourceClassName, final String resType) {
        
        final Map<String, Object> result = new HashMap<String, Object>();

        // get connector runtime
        try
        {
            final Map<String, Object> connProps = getConnectorRuntime().
                    getConnectionDefinitionPropertiesAndDefaults(datasourceClassName, resType);
            result.put(ConnectorRuntimeAPIProvider.CONN_DEFINITION_PROPS_KEY, connProps);
        }
        catch (ComponentException e)
        {
            result.put(ConnectorRuntimeAPIProvider.CONN_DEFINITION_PROPS_KEY, null);
            result.put(ConnectorRuntimeAPIProvider.REASON_FAILED_KEY, ExceptionUtil.toString(e));
        }

        // got everything, now get properties
        return result;
    }

    private ConnectorRuntime getConnectorRuntime()
    {
        return mHabitat.getComponent(ConnectorRuntime.class, null);
    }

    public Map<String, Object> getSystemConnectorsAllowingPoolCreation(){
        final Map<String, Object> result = new HashMap<String, Object>();

        try
        {
            final String[] systemRars = getConnectorRuntime().getSystemConnectorsAllowingPoolCreation();
            result.put(ConnectorRuntimeAPIProvider.SYSTEM_CONNECTORS_KEY, systemRars);
        }
        catch (ComponentException e)
        {
            result.put(ConnectorRuntimeAPIProvider.SYSTEM_CONNECTORS_KEY, null);
            result.put(ConnectorRuntimeAPIProvider.REASON_FAILED_KEY, ExceptionUtil.toString(e));
        }
        return result;
    }

    public Map<String, Object> getBuiltInCustomResources()
    {
        final Map<String, Object> result = new HashMap<String, Object>();

        try
        {
            final Map<String, String> customResources = getConnectorRuntime().getBuiltInCustomResources();
            result.put(ConnectorRuntimeAPIProvider.BUILT_IN_CUSTOM_RESOURCES_KEY, customResources);
        }
        catch (ComponentException e)
        {
            result.put(ConnectorRuntimeAPIProvider.BUILT_IN_CUSTOM_RESOURCES_KEY, null);
            result.put(ConnectorRuntimeAPIProvider.REASON_FAILED_KEY, ExceptionUtil.toString(e));
        }
        return result;
    }

    public Map<String, Object> getConnectionDefinitionNames(String rarName)
    {
        rarName = getActualRAName(rarName);
        final Map<String, Object> result = new HashMap<String, Object>();

        final String[] conDefnNames;
        try
        {
            conDefnNames = getConnectorRuntime().getConnectionDefinitionNames(rarName);
            result.put(ConnectorRuntimeAPIProvider.CONNECTION_DEFINITION_NAMES_KEY, conDefnNames);
        }
        catch (ConnectorRuntimeException e)
        {
            result.put(ConnectorRuntimeAPIProvider.CONNECTION_DEFINITION_NAMES_KEY, null);
            result.put(ConnectorRuntimeAPIProvider.REASON_FAILED_KEY, ExceptionUtil.toString(e));
        }
        catch (ComponentException e)
        {
            result.put(ConnectorRuntimeAPIProvider.CONNECTION_DEFINITION_NAMES_KEY, null);
            result.put(ConnectorRuntimeAPIProvider.REASON_FAILED_KEY, ExceptionUtil.toString(e));
        }
        return result;
    }

    public Map<String, Object> getMCFConfigProps(String rarName, String connectionDefName)
    {
        rarName = getActualRAName(rarName);
        final Map<String, Object> result = new HashMap<String, Object>();

        try
        {
            Map<String, String> configProperties = getConnectorRuntime().getMCFConfigProps(rarName, connectionDefName);
            result.put(ConnectorRuntimeAPIProvider.MCF_CONFIG_PROPS_KEY, configProperties);
        }
        catch (ConnectorRuntimeException e)
        {
            result.put(ConnectorRuntimeAPIProvider.MCF_CONFIG_PROPS_KEY, null);
            result.put(ConnectorRuntimeAPIProvider.REASON_FAILED_KEY, ExceptionUtil.toString(e));
        }
        catch (ComponentException e)
        {
            result.put(ConnectorRuntimeAPIProvider.MCF_CONFIG_PROPS_KEY, null);
            result.put(ConnectorRuntimeAPIProvider.REASON_FAILED_KEY, ExceptionUtil.toString(e));
        }
        return result;
    }

    public Map<String, Object> getAdminObjectInterfaceNames(String rarName)
    {
        rarName = getActualRAName(rarName);
        final Map<String, Object> result = new HashMap<String, Object>();

        try
        {
            final String[] adminObjectInterfaceNames = getConnectorRuntime().getAdminObjectInterfaceNames(rarName);
            result.put(ConnectorRuntimeAPIProvider.ADMIN_OBJECT_INTERFACES_KEY, adminObjectInterfaceNames);
        }
        catch (ConnectorRuntimeException e)
        {
            result.put(ConnectorRuntimeAPIProvider.ADMIN_OBJECT_INTERFACES_KEY, null);
            result.put(ConnectorRuntimeAPIProvider.REASON_FAILED_KEY, ExceptionUtil.toString(e));
        }
        catch (ComponentException e)
        {
            result.put(ConnectorRuntimeAPIProvider.ADMIN_OBJECT_INTERFACES_KEY, null);
            result.put(ConnectorRuntimeAPIProvider.REASON_FAILED_KEY, ExceptionUtil.toString(e));
        }
        return result;
    }

    public Map<String, Object> getAdminObjectClassNames(String rarName, String intfName)
    {
        rarName = getActualRAName(rarName);

        final Map<String, Object> result = new HashMap<String, Object>();

        try
        {
            final String[] adminObjectClassNames = getConnectorRuntime().getAdminObjectClassNames(rarName, intfName);
            result.put(ConnectorRuntimeAPIProvider.ADMIN_OBJECT_CLASSES_KEY, adminObjectClassNames);
        }
        catch (ConnectorRuntimeException e)
        {
            result.put(ConnectorRuntimeAPIProvider.ADMIN_OBJECT_CLASSES_KEY, null);
            result.put(ConnectorRuntimeAPIProvider.REASON_FAILED_KEY, ExceptionUtil.toString(e));
        }
        catch (ComponentException e)
        {
            result.put(ConnectorRuntimeAPIProvider.ADMIN_OBJECT_CLASSES_KEY, null);
            result.put(ConnectorRuntimeAPIProvider.REASON_FAILED_KEY, ExceptionUtil.toString(e));
        }
        return result;
    }

    public Map<String, Object> getResourceAdapterConfigProps(String rarName)
    {
        rarName = getActualRAName(rarName);
        final Map<String, Object> result = new HashMap<String, Object>();

        try
        {
            Map<String, String> configProperties = getConnectorRuntime().getResourceAdapterConfigProps(rarName);
            result.put(ConnectorRuntimeAPIProvider.RESOURCE_ADAPTER_CONFIG_PROPS_KEY, configProperties);
        }
        catch (ConnectorRuntimeException e)
        {
            result.put(ConnectorRuntimeAPIProvider.RESOURCE_ADAPTER_CONFIG_PROPS_KEY, null);
            result.put(ConnectorRuntimeAPIProvider.REASON_FAILED_KEY, ExceptionUtil.toString(e));
        }
        catch (ComponentException e)
        {
            result.put(ConnectorRuntimeAPIProvider.RESOURCE_ADAPTER_CONFIG_PROPS_KEY, null);
            result.put(ConnectorRuntimeAPIProvider.REASON_FAILED_KEY, ExceptionUtil.toString(e));
        }
        return result;
    }

    public Map<String, Object> getAdminObjectConfigProps(String rarName, String adminObjectIntf, String adminObjectClass)
    {
        rarName = getActualRAName(rarName);
        final Map<String, Object> result = new HashMap<String, Object>();

        try
        {
            Map<String, String> configProperties = getConnectorRuntime().getAdminObjectConfigProps(
                    rarName, adminObjectIntf, adminObjectClass);
            result.put(ConnectorRuntimeAPIProvider.ADMIN_OBJECT_CONFIG_PROPS_KEY, configProperties);
        }
        catch (ConnectorRuntimeException e)
        {
            result.put(ConnectorRuntimeAPIProvider.ADMIN_OBJECT_CONFIG_PROPS_KEY, null);
            result.put(ConnectorRuntimeAPIProvider.REASON_FAILED_KEY, ExceptionUtil.toString(e));
        }
        catch (ComponentException e)
        {
            result.put(ConnectorRuntimeAPIProvider.ADMIN_OBJECT_CONFIG_PROPS_KEY, null);
            result.put(ConnectorRuntimeAPIProvider.REASON_FAILED_KEY, ExceptionUtil.toString(e));
        }
        return result;
    }

    public Map<String, Object> getAdminObjectConfigProps(String rarName, String adminObjectIntf)
    {
        rarName = getActualRAName(rarName);
        final Map<String, Object> result = new HashMap<String, Object>();

        try
        {
            Map<String, String> configProperties = getConnectorRuntime().getAdminObjectConfigProps(rarName, adminObjectIntf);
            result.put(ConnectorRuntimeAPIProvider.ADMIN_OBJECT_CONFIG_PROPS_KEY, configProperties);
        }
        catch (ConnectorRuntimeException e)
        {
            result.put(ConnectorRuntimeAPIProvider.ADMIN_OBJECT_CONFIG_PROPS_KEY, null);
            result.put(ConnectorRuntimeAPIProvider.REASON_FAILED_KEY, ExceptionUtil.toString(e));
        }
        catch (ComponentException e)
        {
            result.put(ConnectorRuntimeAPIProvider.ADMIN_OBJECT_CONFIG_PROPS_KEY, null);
            result.put(ConnectorRuntimeAPIProvider.REASON_FAILED_KEY, ExceptionUtil.toString(e));
        }
        return result;
    }

    public Map<String, Object> getConnectorConfigJavaBeans(String rarName, String connectionDefName, String type)
    {
        rarName = getActualRAName(rarName);
        final Map<String, Object> result = new HashMap<String, Object>();

        try
        {
            Map<String, String> configProperties = getConnectorRuntime().getConnectorConfigJavaBeans(rarName, connectionDefName, type);
            result.put(ConnectorRuntimeAPIProvider.CONNECTOR_CONFIG_JAVA_BEANS_KEY, configProperties);
        }
        catch (ConnectorRuntimeException e)
        {
            result.put(ConnectorRuntimeAPIProvider.CONNECTOR_CONFIG_JAVA_BEANS_KEY, null);
            result.put(ConnectorRuntimeAPIProvider.REASON_FAILED_KEY, ExceptionUtil.toString(e));
        }
        catch (ComponentException e)
        {
            result.put(ConnectorRuntimeAPIProvider.CONNECTOR_CONFIG_JAVA_BEANS_KEY, null);
            result.put(ConnectorRuntimeAPIProvider.REASON_FAILED_KEY, ExceptionUtil.toString(e));
        }
        return result;
    }

    public Map<String, Object> getActivationSpecClass(String rarName,
                                                      String messageListenerType)
    {
        rarName = getActualRAName(rarName);
        final Map<String, Object> result = new HashMap<String, Object>();

        try
        {
            String activationSpec = getConnectorRuntime().getActivationSpecClass(rarName, messageListenerType);
            result.put(ConnectorRuntimeAPIProvider.ACTIVATION_SPEC_CLASS_KEY, activationSpec);
        }
        catch (ConnectorRuntimeException e)
        {
            result.put(ConnectorRuntimeAPIProvider.ACTIVATION_SPEC_CLASS_KEY, null);
            result.put(ConnectorRuntimeAPIProvider.REASON_FAILED_KEY, ExceptionUtil.toString(e));
        }
        catch (ComponentException e)
        {
            result.put(ConnectorRuntimeAPIProvider.ACTIVATION_SPEC_CLASS_KEY, null);
            result.put(ConnectorRuntimeAPIProvider.REASON_FAILED_KEY, ExceptionUtil.toString(e));
        }
        return result;
    }

    public Map<String, Object> getMessageListenerTypes(String rarName)
    {
        rarName = getActualRAName(rarName);
        final Map<String, Object> result = new HashMap<String, Object>();

        try
        {
            final String[] messageListenerTypes = getConnectorRuntime().getMessageListenerTypes(rarName);
            result.put(ConnectorRuntimeAPIProvider.MESSAGE_LISTENER_TYPES_KEY, messageListenerTypes);
        }
        catch (ConnectorRuntimeException e)
        {
            result.put(ConnectorRuntimeAPIProvider.MESSAGE_LISTENER_TYPES_KEY, null);
            result.put(ConnectorRuntimeAPIProvider.REASON_FAILED_KEY, ExceptionUtil.toString(e));
        }
        catch (ComponentException e)
        {
            result.put(ConnectorRuntimeAPIProvider.MESSAGE_LISTENER_TYPES_KEY, null);
            result.put(ConnectorRuntimeAPIProvider.REASON_FAILED_KEY, ExceptionUtil.toString(e));
        }
        return result;
    }

    public Map<String, Object> getMessageListenerConfigProps(String rarName,
                                                             String messageListenerType)
    {
        rarName = getActualRAName(rarName);
        final Map<String, Object> result = new HashMap<String, Object>();

        try
        {
            Map<String, String> configProperties = getConnectorRuntime().getMessageListenerConfigProps(
                    rarName, messageListenerType);
            result.put(ConnectorRuntimeAPIProvider.MESSAGE_LISTENER_CONFIG_PROPS_KEY, configProperties);
        }
        catch (ConnectorRuntimeException e)
        {
            result.put(ConnectorRuntimeAPIProvider.MESSAGE_LISTENER_CONFIG_PROPS_KEY, null);
            result.put(ConnectorRuntimeAPIProvider.REASON_FAILED_KEY, ExceptionUtil.toString(e));
        }
        catch (ComponentException e)
        {
            result.put(ConnectorRuntimeAPIProvider.MESSAGE_LISTENER_CONFIG_PROPS_KEY, null);
            result.put(ConnectorRuntimeAPIProvider.REASON_FAILED_KEY, ExceptionUtil.toString(e));
        }
        return result;
    }

    public Map<String, Object> getMessageListenerConfigPropTypes(String rarName,
                                                                 String messageListenerType)
    {
        rarName = getActualRAName(rarName);
        final Map<String, Object> result = new HashMap<String, Object>();

        try
        {
            Map<String, String> configProperties = getConnectorRuntime().getMessageListenerConfigPropTypes(
                    rarName, messageListenerType);
            result.put(ConnectorRuntimeAPIProvider.MESSAGE_LISTENER_CONFIG_PROP_TYPES_KEY, configProperties);
        }
        catch (ConnectorRuntimeException e)
        {
            result.put(ConnectorRuntimeAPIProvider.MESSAGE_LISTENER_CONFIG_PROP_TYPES_KEY, null);
            result.put(ConnectorRuntimeAPIProvider.REASON_FAILED_KEY, ExceptionUtil.toString(e));
        }
        catch (ComponentException e)
        {
            result.put(ConnectorRuntimeAPIProvider.MESSAGE_LISTENER_CONFIG_PROP_TYPES_KEY, null);
            result.put(ConnectorRuntimeAPIProvider.REASON_FAILED_KEY, ExceptionUtil.toString(e));
        }
        return result;
    }

    /**
     * Flush Connection pool. This API is used by administration console for the
     * Flush button the admin GUI. The connections in connection pool are 
     * re-initialized when flush is executed.
     * @param poolName
     * @return a map containing a FLUSH_CONNECTION_POOL_KEY with a boolean value
     * indicating pass/fail. If FLUSH_CONNECTION_POOL_KEY is false, an 
     * exception has occured and REASON_FAILED_KEY would give the reason
     * why getting flush connection pool failed. 
     */
    public Map<String, Object> flushConnectionPool(final String poolName)
    {
        final Map<String, Object> result = new HashMap<String, Object>();

        if (mHabitat == null)
        {
            result.put(ConnectorRuntimeAPIProvider.FLUSH_CONNECTION_POOL_KEY, false);
            result.put(ConnectorRuntimeAPIProvider.REASON_FAILED_KEY, "Habitat is null");
            return result;
        }
        try
        {
            final ConnectorRuntime connRuntime = mHabitat.getComponent(ConnectorRuntime.class, null);
            //as per the method parameters, this is not applicable for "application-scoped" pools
            PoolInfo poolInfo = new PoolInfo(poolName);
            boolean flushStatus = connRuntime.flushConnectionPool(poolInfo);

            result.put(ConnectorRuntimeAPIProvider.FLUSH_CONNECTION_POOL_KEY, flushStatus);    
        }            
        catch (ConnectorRuntimeException ex)
        {
            result.put(ConnectorRuntimeAPIProvider.FLUSH_CONNECTION_POOL_KEY, false);
            result.put(ConnectorRuntimeAPIProvider.REASON_FAILED_KEY, ExceptionUtil.toString(ex));
        }
        catch (ComponentException e)
        {
            result.put(ConnectorRuntimeAPIProvider.FLUSH_CONNECTION_POOL_KEY, false);
            result.put(ConnectorRuntimeAPIProvider.REASON_FAILED_KEY, ExceptionUtil.toString(e));
        }
        return result;
    }

    /**
     * Obtain connection validation table names for the database that the jdbc 
     * connection pool refers to. This is used by administration console to list
     * the validation table names when connection validation is enabled and/or 
     * table is chosen for validation method.
     * @param poolName
     * @return a map containing a VALIDATION_TABLE_NAMES_KEY with a set of
     * validation table names. If VALIDATION_TABLE_NAMES_KEY is null, an 
     * exception has occured and REASON_FAILED_KEY would give the reason
     * why getting connection validation table names failed. 
     */
    public Map<String, Object> getValidationTableNames(final String poolName)
    {
        final Map<String, Object> result = new HashMap<String, Object>();

        try
        {
            final ConnectorRuntime connRuntime = mHabitat.getComponent(ConnectorRuntime.class, null);
            //as per the method parameters, this is not applicable for "application-scoped" pools
            PoolInfo poolInfo = new PoolInfo(poolName);
            final Set<String> tableNames = connRuntime.getValidationTableNames(poolInfo);
            result.put(ConnectorRuntimeAPIProvider.VALIDATION_TABLE_NAMES_KEY, tableNames);
        }
        catch (ComponentException e)
        {
            result.put(ConnectorRuntimeAPIProvider.VALIDATION_TABLE_NAMES_KEY, null);
            result.put(ConnectorRuntimeAPIProvider.REASON_FAILED_KEY, ExceptionUtil.toString(e));
        }
        catch (Exception e)
        {
            result.put(ConnectorRuntimeAPIProvider.VALIDATION_TABLE_NAMES_KEY, null);
            result.put(ConnectorRuntimeAPIProvider.REASON_FAILED_KEY, ExceptionUtil.toString(e));
        }
        return result;
    }

    /**
     * Obtain Jdbc driver implementation class names for a particular 
     * dbVendor and resource type. This is used by the administration console
     * to list the driver-classname or datasource-classname fields based on the
     * resType provided.
     * @param dbVendor
     * @param resType one of javax.sql.DataSource, javax.sql.ConnectionPoolDataSource,
     * javax.sql.XADataSource, java.sql.Driver.
     * @return a map containing a JDBC_DRIVER_CLASS_NAMES_KEY with a set of
     * datasource/driver class names. If JDBC_DRIVER_CLASS_NAMES_KEY is null, an 
     * exception has occured and REASON_FAILED_KEY would give the reason
     * why getting datasource/driver classnames failed. 
     */
    public Map<String, Object> getJdbcDriverClassNames(final String dbVendor,
                                                       final String resType)
    {
        final Map<String, Object> result = new HashMap<String, Object>();

        try
        {
            final ConnectorRuntime connRuntime = mHabitat.getComponent(ConnectorRuntime.class, null);
            final Set<String> implClassNames = connRuntime.getJdbcDriverClassNames(dbVendor, resType);
            result.put(ConnectorRuntimeAPIProvider.JDBC_DRIVER_CLASS_NAMES_KEY, implClassNames);
        }
        catch (ComponentException e)
        {
            result.put(ConnectorRuntimeAPIProvider.JDBC_DRIVER_CLASS_NAMES_KEY, null);
            result.put(ConnectorRuntimeAPIProvider.REASON_FAILED_KEY, ExceptionUtil.toString(e));
        }
        catch (Exception e)
        {
            result.put(ConnectorRuntimeAPIProvider.JDBC_DRIVER_CLASS_NAMES_KEY, null);
            result.put(ConnectorRuntimeAPIProvider.REASON_FAILED_KEY, ExceptionUtil.toString(e));
        }
        return result;
    }

    /**
     * Obtain Jdbc driver implementation class names for a particular
     * dbVendor and resource type. This is used by the administration console
     * to list the driver-classname or datasource-classname fields based on the
     * resType provided.
     * The classnames retrieved are introspected from jdbc driver jar or
     * got from the pre-defined list based on the introspect flag.
     * @param dbVendor
     * @param resType one of javax.sql.DataSource, javax.sql.ConnectionPoolDataSource,
     * javax.sql.XADataSource, java.sql.Driver.
     * @param introspect introspect or quick select from list
     * @return a map containing a JDBC_DRIVER_CLASS_NAMES_KEY with a set of
     * datasource/driver class names. If JDBC_DRIVER_CLASS_NAMES_KEY is null, an
     * exception has occured and REASON_FAILED_KEY would give the reason
     * why getting datasource/driver classnames failed.
     */
    public Map<String, Object> getJdbcDriverClassNames(final String dbVendor,
                                                       final String resType,
                                                       final boolean introspect)
    {
        final Map<String, Object> result = new HashMap<String, Object>();

        try
        {
            final ConnectorRuntime connRuntime = mHabitat.getComponent(ConnectorRuntime.class, null);
            final Set<String> implClassNames = connRuntime.getJdbcDriverClassNames(dbVendor, resType, introspect);
            result.put(ConnectorRuntimeAPIProvider.JDBC_DRIVER_CLASS_NAMES_KEY, implClassNames);
        }
        catch (ComponentException e)
        {
            result.put(ConnectorRuntimeAPIProvider.JDBC_DRIVER_CLASS_NAMES_KEY, null);
            result.put(ConnectorRuntimeAPIProvider.REASON_FAILED_KEY, ExceptionUtil.toString(e));
        }
        catch (Exception e)
        {
            result.put(ConnectorRuntimeAPIProvider.JDBC_DRIVER_CLASS_NAMES_KEY, null);
            result.put(ConnectorRuntimeAPIProvider.REASON_FAILED_KEY, ExceptionUtil.toString(e));
        }
        return result;
    }

    /**
     * Ping JDBC Connection Pool and return status.
     * 
     * This API is used for the Ping button in the administration console. Ping
     * could be executed for a connection pool after its creation to verify if
     * the connection pool is usable.
     * @param poolName 
     * @return a map containing a PING_CONNECTION_POOL_KEY with a boolean value 
     * indicating a pass/fail. If PING_CONNECTION_POOL_KEY is false, an 
     * exception has occured and REASON_FAILED_KEY would give the reason
     * why ping connection pool failed.
     */
    public Map<String, Object> pingJDBCConnectionPool(final String poolName)
    {
        final Map<String, Object> result = new HashMap<String, Object>();

        if (mHabitat == null)
        {
            result.put(ConnectorRuntimeAPIProvider.PING_CONNECTION_POOL_KEY, false);
            result.put(ConnectorRuntimeAPIProvider.REASON_FAILED_KEY, "Habitat is null");
            return result;
        }
        try
        {
            final ConnectorRuntime connRuntime = mHabitat.getComponent(ConnectorRuntime.class, null);
            //as per the method parameters, this is not applicable for "application-scoped" pools
            PoolInfo poolInfo = new PoolInfo(poolName);
            final boolean pingStatus = connRuntime.pingConnectionPool(poolInfo);
            result.put(ConnectorRuntimeAPIProvider.PING_CONNECTION_POOL_KEY, pingStatus);
        }
        catch (ResourceException ex)
        {
            result.put(ConnectorRuntimeAPIProvider.PING_CONNECTION_POOL_KEY, false);
            result.put(ConnectorRuntimeAPIProvider.REASON_FAILED_KEY, ex.getMessage());
        }
        catch (ComponentException e)
        {
            result.put(ConnectorRuntimeAPIProvider.PING_CONNECTION_POOL_KEY, false);
            result.put(ConnectorRuntimeAPIProvider.REASON_FAILED_KEY, ExceptionUtil.toString(e));
        }
        catch (Exception e)
        {
            result.put(ConnectorRuntimeAPIProvider.PING_CONNECTION_POOL_KEY, false);
            result.put(ConnectorRuntimeAPIProvider.REASON_FAILED_KEY, ExceptionUtil.toString(e));
        }
        return result;
    }

    /**
     * Obtain a set of connection validation class names for the datasource/driver class
     * name that the jdbc connection pool refers to. This API is used when
     * custom-validation is chosen as the connection validation method, to list
     * the various custom validation implementations available.

     * @param className
     * @return a map containing a VALIDATION_CLASS_NAMES_KEY with a set of
     * validation class names. If VALIDATION_CLASS_NAMES_KEY is null, an 
     * exception has occured and REASON_FAILED_KEY would give the reason
     * why getting connection validation classnames failed.
     */
    public Map<String, Object> getValidationClassNames(final String className) {
        final Map<String, Object> result = new HashMap<String, Object>();

        if (mHabitat == null)
        {
            result.put(ConnectorRuntimeAPIProvider.VALIDATION_CLASS_NAMES_KEY, false);
            result.put(ConnectorRuntimeAPIProvider.REASON_FAILED_KEY, "Habitat is null");
            return result;
        }

        try {
            final ConnectorRuntime connRuntime = mHabitat.getComponent(ConnectorRuntime.class, null);
            final Set<String> valClassNames = connRuntime.getValidationClassNames(className);
            result.put(ConnectorRuntimeAPIProvider.VALIDATION_CLASS_NAMES_KEY, valClassNames);
        } catch (ComponentException e) {
            result.put(ConnectorRuntimeAPIProvider.VALIDATION_CLASS_NAMES_KEY, null);
            result.put(ConnectorRuntimeAPIProvider.REASON_FAILED_KEY, ExceptionUtil.toString(e));
        } catch (Exception e) {
            result.put(ConnectorRuntimeAPIProvider.VALIDATION_CLASS_NAMES_KEY, null);
            result.put(ConnectorRuntimeAPIProvider.REASON_FAILED_KEY, ExceptionUtil.toString(e));
        }
        return result;
    }

    /**
     * Obtain a set of database vendor names. This API is used to list
     * the various common database vendor names.

     * @return a map containing a DATABASE_VENDOR_NAMES_KEY with a set of
     * database vendor names. If DATABASE_VENDOR_NAMES_KEY is null, an 
     * exception has occured and REASON_FAILED_KEY would give the reason
     * why getting database vendor names failed.
     */
    public Map<String, Object> getDatabaseVendorNames() {
        final Map<String, Object> result = new HashMap<String, Object>();

        if (mHabitat == null)
        {
            result.put(ConnectorRuntimeAPIProvider.DATABASE_VENDOR_NAMES_KEY, false);
            result.put(ConnectorRuntimeAPIProvider.REASON_FAILED_KEY, "Habitat is null");
            return result;
        }

        try {
            final ConnectorRuntime connRuntime = mHabitat.getComponent(ConnectorRuntime.class, null);
            final Set<String> dbVendorNames = connRuntime.getDatabaseVendorNames();
            result.put(ConnectorRuntimeAPIProvider.DATABASE_VENDOR_NAMES_KEY, dbVendorNames);
        } catch (ComponentException e) {
            result.put(ConnectorRuntimeAPIProvider.DATABASE_VENDOR_NAMES_KEY, null);
            result.put(ConnectorRuntimeAPIProvider.REASON_FAILED_KEY, ExceptionUtil.toString(e));
        } catch (Exception e) {
            result.put(ConnectorRuntimeAPIProvider.DATABASE_VENDOR_NAMES_KEY, null);
            result.put(ConnectorRuntimeAPIProvider.REASON_FAILED_KEY, ExceptionUtil.toString(e));
        }
        return result;
    }

    private static String getActualRAName(String rarName){
        //handle only embedded rars
        if(rarName != null && rarName.contains(ConnectorConstants.EMBEDDEDRAR_NAME_DELIMITER)){
            if(rarName.endsWith(ConnectorConstants.RAR_EXTENSION)){
                int index = rarName.lastIndexOf(ConnectorConstants.RAR_EXTENSION);
                return rarName.substring(0,index);
            }
        }
        return rarName;
    }
}

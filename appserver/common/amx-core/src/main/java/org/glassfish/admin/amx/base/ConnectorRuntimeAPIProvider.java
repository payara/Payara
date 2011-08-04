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

package org.glassfish.admin.amx.base;

import org.glassfish.admin.amx.annotation.ManagedOperation;
import org.glassfish.admin.amx.annotation.Description;
import org.glassfish.admin.amx.annotation.Param;
import org.glassfish.admin.amx.core.AMXProxy;

import java.util.Map;
import javax.management.MBeanOperationInfo;
import org.glassfish.admin.amx.annotation.ManagedAttribute;
import org.glassfish.external.arc.Stability;
import org.glassfish.external.arc.Taxonomy;
import org.glassfish.admin.amx.core.AMXMBeanMetadata;

/**
@since GlassFish V3
 */
@Taxonomy(stability = Stability.UNCOMMITTED)
@AMXMBeanMetadata(singleton = true, globalSingleton = true, leaf = true)
public interface ConnectorRuntimeAPIProvider extends AMXProxy, Utility, Singleton
{
    /**Value of type Map from {@link #getConnectionDefinitionPropertiesAndDefaults} */
    public static final String CONN_DEFINITION_PROPS_KEY = "ConnDefinitionPropsKey";

    /**Value of type boolean for Map from {@link #pingJdbcConnectionPool} */
    public static final String PING_CONNECTION_POOL_KEY = "PingConnectionPoolKey";
    
    /**Value of type boolean for Map from {@link #flushConnectionPool} */
    public static final String FLUSH_CONNECTION_POOL_KEY = "FlushConnectionPoolKey";
    
    /**Value of type Set for Map from {@link #getValidationClassNames}*/
    public static final String VALIDATION_CLASS_NAMES_KEY = "ValidationClassNamesKey";
    
    /**Value of type Set for Map from {@link #getJdbcDriverClassNames}*/
    public static final String JDBC_DRIVER_CLASS_NAMES_KEY = "JdbcDriverClassNamesKey";
    
    /**Value of type Set for Map from {@link #getValidationTableNames}*/
    public static final String VALIDATION_TABLE_NAMES_KEY = "ValidationTableNamesKey";
    
    /**Value of type Set for Map from {@link #getDatabaseVendorNames}*/
    public static final String DATABASE_VENDOR_NAMES_KEY = "DatabaseVendorNamesKey";
    
    /** Key into Map returned by various methods including {@link #getConnectionDefinitionPropertiesAndDefaults}
     * {@link #getConnectionDefinitionNames}
     * {@link #getAdminObjectInterfaceNames}
     * {@link #getMessageListenerTypes}
     * {@link #getMessageListenerTypes}
     * {@link #getBuiltInCustomResources}
     * {@link #getValidationTableNames}
     * {@link #getJdbcDriverClassNames}
     * {@link #flushConnectionPool}
     * {@link #getMCFConfigProps}
     * {@link #getResourceAdapterConfigProps}
     * {@link #getAdminObjectConfigProps}
     * {@link #getConnectorConfigJavaBeans}
     * {@link #getMessageListenerConfigProps}
     * {@link #getMessageListenerConfigPropTypes}
     * **/
    public static final String REASON_FAILED_KEY = "ReasonFailedKey";
    
    /**Value of type String[] for Map from {@link #getSystemConnectorsAllowingPoolCreation}*/
    public static final String SYSTEM_CONNECTORS_KEY = "SystemConnectorsKey";
    
    /**Value of type Map for Map from {@link #getBuiltInCustomResources}*/
    public static final String BUILT_IN_CUSTOM_RESOURCES_KEY = "BuiltInCustomResourcesKey";
    
    /**Value of type String[] for Map from {@link #getConnectionDefinitionNames}*/
    public static final String CONNECTION_DEFINITION_NAMES_KEY = "ConnectionDefinitionNamesKey";
    
    /**Value of type Map for Map from {@link #getMCFConfigProps}*/
    public static final String MCF_CONFIG_PROPS_KEY = "McfConfigPropsKey";
    
    /**Value of type String[] for Map from {@link #getAdminObjectInterfaceNames}*/
    public static final String ADMIN_OBJECT_INTERFACES_KEY = "AdminObjectInterfacesKey";

    /**Value of type String[] for Map from {@link #getAdminObjectClassNames}*/
    public static final String ADMIN_OBJECT_CLASSES_KEY = "AdminObjectClassesKey";

    /**Value of type Map for Map from {@link #getResourceAdapterConfigProps}*/
    public static final String RESOURCE_ADAPTER_CONFIG_PROPS_KEY = "ResourceAdapterConfigPropsKey";
    
    /**Value of type Map for Map from {@link #getAdminObjectConfigProps}*/
    public static final String ADMIN_OBJECT_CONFIG_PROPS_KEY = "AdminObjectConfigPropsKey";
    
    /**Value of type Map for Map from {@link #getConnectorConfigJavaBeans}*/
    public static final String CONNECTOR_CONFIG_JAVA_BEANS_KEY = "ConnectorConfigJavaBeansKey";
    
    /**Value of type String for Map from {@link #getActivationSpecClass}*/
    public static final String ACTIVATION_SPEC_CLASS_KEY = "ActivationSpecClassKey";
    
    /**Value of type String[] for Map from {@link #getMessageListenerTypes}*/
    public static final String MESSAGE_LISTENER_TYPES_KEY = "MessageListenerTypesKey";
    
    /**Value of type Map for Map from {@link #getMessageListenerConfigProps}*/
    public static final String MESSAGE_LISTENER_CONFIG_PROPS_KEY = "MessageListenerConfigPropsKey";
    
    /**Value of type Map for Map from {@link #getMessageListenerConfigPropTypes}*/
    public static final String MESSAGE_LISTENER_CONFIG_PROP_TYPES_KEY = "MessageListenerConfigPropTypesKey";

    /**
     * Get properties of JDBC Data Source along with its default values.
     * @see #CONN_DEFINITION_PROPS_KEY
     * @see #REASON_FAILED_KEY
     */
    @ManagedOperation(impact = MBeanOperationInfo.INFO)
    @Description("Returns the connection definition properties and their default values of a datasource class")
    public Map<String, Object> getConnectionDefinitionPropertiesAndDefaults(
            @Param(name = "datasourceClassName") String datasourceClassName,
            @Param(name = "resType") String resType);

    @ManagedAttribute
    @Description("List of built in custom resource factory classes")
    public Map<String, Object> getBuiltInCustomResources();

    @ManagedAttribute
    @Description("List of system resource-adapters that allow connector-connection-pool creation")
    public Map<String, Object> getSystemConnectorsAllowingPoolCreation();

    @ManagedOperation(impact = MBeanOperationInfo.INFO)
    @Description("List of connection definition names for the given resource-adapter")
    public Map<String, Object> getConnectionDefinitionNames(@Param(name = "rarName") String rarName);

    @ManagedOperation(impact = MBeanOperationInfo.INFO)
    @Description("get the MCF config properties of the connection definition")
    public Map<String, Object> getMCFConfigProps(
            @Param(name = "rarName") String rarName,
            @Param(name = "connectionDefName") String connectionDefName);

    @ManagedOperation(impact = MBeanOperationInfo.INFO)
    @Description("List of administered object interfaces for the given resource-adapter")
    public Map<String, Object> getAdminObjectInterfaceNames(@Param(name = "rarName") String rarName);

    @ManagedOperation(impact = MBeanOperationInfo.INFO)
    @Description("List of administered object class names for the given resource-adapter and administered object interface name")
    public Map<String, Object> getAdminObjectClassNames(@Param(name = "rarName") String rarName, @Param(name="intfName") String intfName);

    @ManagedOperation(impact = MBeanOperationInfo.INFO)
    @Description("List of resource adapter configuration properties of a resource-adapter")
    public Map<String, Object> getResourceAdapterConfigProps(@Param(name = "rarName") String rarName);

    @ManagedOperation(impact = MBeanOperationInfo.INFO)
    @Description("List of administered object configuration properties")
    public Map<String, Object> getAdminObjectConfigProps(
            @Param(name = "rarName") String rarName,
            @Param(name = "adminObjectIntf") String adminObjectIntf);

    @ManagedOperation(impact = MBeanOperationInfo.INFO)
    @Description("List of administered object configuration properties")
    public Map<String, Object> getAdminObjectConfigProps(
            @Param(name = "rarName") String rarName,
            @Param(name = "adminObjectIntf") String adminObjectIntf,
            @Param(name = "adminObjectClass") String adminObjectClass);

    @ManagedOperation(impact = MBeanOperationInfo.INFO)
    @Description("List of java bean properties and their default values for a connection definition")
    public Map<String, Object> getConnectorConfigJavaBeans(
            @Param(name = "rarName") String rarName,
            @Param(name = "connectionDefName") String connectionDefName,
            @Param(name = "type") String type);

    @ManagedOperation(impact = MBeanOperationInfo.INFO)
    @Description("get the activation spec class for the given message-listener type of a resource-adapter")
    public Map<String, Object> getActivationSpecClass(
            @Param(name = "rarName") String rarName,
            @Param(name = "messageListenerType") String messageListenerType);

    @ManagedOperation(impact = MBeanOperationInfo.INFO)
    @Description("get message listener types of a resource-adapter")
    public Map<String, Object> getMessageListenerTypes(@Param(name = "rarName") String rarName);

    @ManagedOperation(impact = MBeanOperationInfo.INFO)
    @Description("get message listener config properties for the given message-listener-type of a resource-adapter")
    public Map<String, Object> getMessageListenerConfigProps(@Param(name = "rarName") String rarName,
                                                             @Param(name = "messageListenerType") String messageListenerType);

    @ManagedOperation(impact = MBeanOperationInfo.INFO)
    @Description("get message listener config property types for the given message-listener-type of a resource-adapter")
    public Map<String, Object> getMessageListenerConfigPropTypes(
            @Param(name = "rarName") String rarName,
            @Param(name = "messageListenerType") String messageListenerType);

    /**
     * Flush Connection pool.
     * @see #FLUSH_CONNECTION_POOL_KEY
     * @see #REASON_FAILED_KEY
     */
    @ManagedOperation(impact = MBeanOperationInfo.INFO)
    @Description("Flush connection pool by reinitializing all connections established in the pool")
    public Map<String, Object> flushConnectionPool(@Param(name = "poolName") String poolName);

    /**
     * Obtain connection validation table names.
     * @see #VALIDATION_TABLE_NAMES_KEY
     * @see #REASON_FAILED_KEY
     */
    @ManagedOperation(impact = MBeanOperationInfo.INFO)
    @Description("Get Connection validation table names for display in GUI")
    public Map<String, Object> getValidationTableNames(@Param(name = "poolName") String poolName);

    /**
     * Obtain Jdbc driver implementation class names.
     * @see #JDBC_DRIVER_CLASS_NAMES_KEY
     * @see #REASON_FAILED_KEY
     */
    @ManagedOperation(impact = MBeanOperationInfo.INFO)
    @Description("Get Jdbc driver implementation class names")
    public Map<String, Object> getJdbcDriverClassNames(@Param(name = "dbVendor") String dbVendor, 
                                                       @Param(name = "resType") String resType);

    /**
     * Obtain Jdbc driver implementation class names.
     * @see #JDBC_DRIVER_CLASS_NAMES_KEY
     * @see #REASON_FAILED_KEY
     */
    @ManagedOperation(impact = MBeanOperationInfo.INFO)
    @Description("Get Jdbc driver implementation class names")
    public Map<String, Object> getJdbcDriverClassNames(@Param(name = "dbVendor") String dbVendor,
                                                       @Param(name = "resType") String resType,
                                                       @Param(name = "introspect") boolean introspect);

    /**
     * Ping the ConnectionPool and return status.
     * @see #PING_CONNECTION_POOL_KEY
     * @see #REASON_FAILED_KEY
     */
    @ManagedOperation(impact = MBeanOperationInfo.INFO)
    @Description("Ping Connection Pool and return status")
    public Map<String, Object> pingJDBCConnectionPool(final String poolName);

    /**
     * Obtain connection validation class names.
     * @see #VALIDATION_CLASS_NAMES_KEY
     * @see #REASON_FAILED_KEY
     */
    @ManagedOperation(impact=MBeanOperationInfo.INFO)
    @Description("Get connection validation class names for custom validation")
    public Map<String, Object> getValidationClassNames( final String className);

    /**
     * Obtain database vendor names.
     * @see #DATABASE_VENDOR_NAMES_KEY
     * @see #REASON_FAILED_KEY
     */
    @ManagedAttribute
    @Description("Get database vendor names")
    public Map<String, Object> getDatabaseVendorNames();   
    
}

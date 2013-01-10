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

package com.sun.appserv.connectors.internal.api;

import com.sun.appserv.connectors.internal.spi.ConnectorNamingEventListener;
import com.sun.enterprise.config.serverbeans.ResourcePool;
import com.sun.enterprise.deployment.ConnectorDescriptor;
import com.sun.enterprise.transaction.api.JavaEETransactionManager;
import org.glassfish.api.admin.ProcessEnvironment;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.connectors.config.WorkSecurityMap;
import org.glassfish.resourcebase.resources.api.PoolInfo;
import org.glassfish.resourcebase.resources.api.ResourceInfo;
import org.jvnet.hk2.annotations.Contract;

import javax.naming.NamingException;
import javax.resource.ResourceException;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.ResourceAdapterAssociation;
import javax.security.auth.callback.CallbackHandler;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * This class is the entry point to connector backend module.
 * It exposes different API's called by external entities like JPA, admin
 * to perform various connector backend related  operations.
 * It delegates calls to various connetcor admin services and other
 * connector services which actually implement the functionality.
 * This is a delegating class.
 *
 * @author Binod P.G, Srikanth P, Aditya Gore, Jagadish Ramu
 */

@Contract
public interface ConnectorRuntime extends ConnectorConstants{

    /**
     * Creates Active resource Adapter which abstracts the rar module.
     * During the creation of ActiveResourceAdapter, default pools and
     * resources also are created.
     *
     * @param sourcePath  Directory where rar module is exploded.
     * @param moduleName Name of the module
     * @param loader Classloader used to load the .rar
     * @throws ConnectorRuntimeException if creation fails.
     */
    public void createActiveResourceAdapter(String sourcePath, String moduleName, ClassLoader loader)
            throws ConnectorRuntimeException;

    /**
     * Creates Active resource Adapter which abstracts the rar module.
     * During the creation of ActiveResourceAdapter, default pools and
     * resources also are created.
     *
     * @param moduleName Name of the module
     * @throws ConnectorRuntimeException if creation fails.
     */
    public void createActiveResourceAdapterForEmbeddedRar(String moduleName) throws ConnectorRuntimeException;

    /**
     * Destroys/deletes the Active resource adapter object from the
     * connector container. Active resource adapter abstracts the rar
     * deployed.
     *
     * @param moduleName Name of the rarModule to destroy/delete
     * @throws ConnectorRuntimeException if the deletion fails
     */
    public void destroyActiveResourceAdapter(String moduleName) throws ConnectorRuntimeException;

    /**
     * Shut down all pools and active resource-adapters
     */
    public void cleanUpResourcesAndShutdownAllActiveRAs();

    /**
     * Shut down all active resource adapters
     */
    public void shutdownAllActiveResourceAdapters();

    /**
     * Given the module directory, creates a connector-class-finder (class-loader) for the module
     * @param moduleDirectory rar module directory for which classloader is needed
     * @param parent parent classloader<br>
     * For standalone rars, pass null, as the parent should be common-class-loader
     * that will be automatically taken care by ConnectorClassLoaderService.<br>
     * For embedded rars, parent is necessary<br>
     * @return classloader created for the module
     * @throws ConnectorRuntimeException when unable to create classloader
     */
    public ClassLoader createConnectorClassLoader(String moduleDirectory, ClassLoader parent, String rarModuleName)
            throws ConnectorRuntimeException;

    /**
     * Does lookup of non-tx-datasource. If found, it will be returned.<br><br>
     * <p/>
     * If not found and <b>force</b> is true,  this api will try to get a wrapper datasource specified
     * by the jdbcjndi name. The motivation for having this
     * API is to provide the CMP backend/ JPA-Java2DB a means of acquiring a connection during
     * the codegen phase. If a user is trying to deploy an JPA-Java2DB app on a remote server,
     * without this API, a resource reference has to be present both in the DAS
     * and the server instance. This makes the deployment more complex for the
     * user since a resource needs to be forcibly created in the DAS Too.
     * This API will mitigate this need.
     *
     * @param jndiName jndi name of the resource
     * @param force    provide the resource (in DAS)  even if it is not enabled in DAS
     * @return DataSource representing the resource.
     * @throws javax.naming.NamingException when not able to get the datasource.
     */
    public Object lookupNonTxResource(String jndiName, boolean force) throws NamingException;

    /**
     * Does lookup of "__pm" datasource. If found, it will be returned.<br><br>
     * <p/>
     * If not found and <b>force</b> is true, this api will try to get a wrapper datasource specified
     * by the jdbcjndi name. The motivation for having this
     * API is to provide the CMP backend/ JPA-Java2DB a means of acquiring a connection during
     * the codegen phase. If a user is trying to deploy an JPA-Java2DB app on a remote server,
     * without this API, a resource reference has to be present both in the DAS
     * and the server instance. This makes the deployment more complex for the
     * user since a resource needs to be forcibly created in the DAS Too.
     * This API will mitigate this need.
     * When the resource is not enabled, datasource wrapper provided will not be of
     * type "__pm"
     *
     * @param jndiName jndi name of the resource
     * @param force    provide the resource (in DAS)  even if it is not enabled in DAS
     * @return DataSource representing the resource.
     * @throws javax.naming.NamingException when not able to get the datasource.
     */
    public Object lookupPMResource(String jndiName, boolean force) throws NamingException;

    /**
     * Does lookup of non-tx-datasource. If found, it will be returned.<br><br>
     * <p/>
     * If not found and <b>force</b> is true,  this api will try to get a wrapper datasource specified
     * by the jdbcjndi name. The motivation for having this
     * API is to provide the CMP backend/ JPA-Java2DB a means of acquiring a connection during
     * the codegen phase. If a user is trying to deploy an JPA-Java2DB app on a remote server,
     * without this API, a resource reference has to be present both in the DAS
     * and the server instance. This makes the deployment more complex for the
     * user since a resource needs to be forcibly created in the DAS Too.
     * This API will mitigate this need.
     *
     * @param resourceInfo jndi name of the resource
     * @param force    provide the resource (in DAS)  even if it is not enabled in DAS
     * @return DataSource representing the resource.
     * @throws javax.naming.NamingException when not able to get the datasource.
     */
    public Object lookupNonTxResource(ResourceInfo resourceInfo, boolean force) throws NamingException;

    /**
     * Does lookup of "__pm" datasource. If found, it will be returned.<br><br>
     * <p/>
     * If not found and <b>force</b> is true, this api will try to get a wrapper datasource specified
     * by the jdbcjndi name. The motivation for having this
     * API is to provide the CMP backend/ JPA-Java2DB a means of acquiring a connection during
     * the codegen phase. If a user is trying to deploy an JPA-Java2DB app on a remote server,
     * without this API, a resource reference has to be present both in the DAS
     * and the server instance. This makes the deployment more complex for the
     * user since a resource needs to be forcibly created in the DAS Too.
     * This API will mitigate this need.
     * When the resource is not enabled, datasource wrapper provided will not be of
     * type "__pm"
     *
     * @param resourceInfo jndi name of the resource
     * @param force    provide the resource (in DAS)  even if it is not enabled in DAS
     * @return DataSource representing the resource.
     * @throws javax.naming.NamingException when not able to get the datasource.
     */
    public Object lookupPMResource(ResourceInfo resourceInfo, boolean force) throws NamingException;

    /**
     * register the connector naming event listener
     * @param listener connector-naming-event-listener
     */
    public void registerConnectorNamingEventListener(ConnectorNamingEventListener listener);

    /**
     * unregister the connector naming event listner
     * @param listener connector-naming-event-listener
     */
    public void unregisterConnectorNamingEventListener(ConnectorNamingEventListener listener);

    /**
     * Provide the configuration of the pool
     * @param PoolInfo connection pool info
     * @return ResourcePool connection pool configuration
     */
    public ResourcePool getConnectionPoolConfig(PoolInfo poolInfo);

    /**
     * Tests whether the configuration for the pool is valid by making a connection.
     * @param PoolInfo connection pool info.
     * @return boolean indicating ping status
     * @throws ResourceException when unable to ping
     */
    public boolean pingConnectionPool(PoolInfo poolInfo) throws ResourceException;

    /**
     * provides the transactionManager
     *
     * @return TransactionManager
     */
    public JavaEETransactionManager getTransactionManager();

    /**
     * provides the invocationManager
     *
     * @return InvocationManager
     */
    public InvocationManager getInvocationManager();

    /**
     * get resource reference descriptors from current component's jndi environment
     * @return set of resource-refs
     */
    public Set getResourceReferenceDescriptor();

    /** Returns the MCF instance. If the MCF is already created and
     *  present in connectorRegistry that instance is returned. Otherwise it
     *  is created explicitly and added to ConnectorRegistry.
     *  @param PoolInfo Name of the pool.MCF pertaining to this pool is
     *         created/returned.
     *  @return created/already present MCF instance
     *  @throws ConnectorRuntimeException if creation/retrieval of MCF fails
     */
    public ManagedConnectionFactory obtainManagedConnectionFactory( PoolInfo poolInfo)
            throws ConnectorRuntimeException ;

    /**
     * provide the MCF of the pool (either retrieve or create)
     * @param poolName connection pool name
     * @param env Environment entries to use for lookup
     * @return ManagedConnectionFactory mcf of the pool
     * @throws ConnectorRuntimeException when unable to provide the MCF
     */
    public ManagedConnectionFactory obtainManagedConnectionFactory(PoolInfo poolInfo, Hashtable env)
            throws ConnectorRuntimeException;

    /**
     * Indicates whether the execution environment is server or client
     * @return ConnectorConstants.SERVER or ConnectorConstants.CLIENT
     */
    public ProcessEnvironment.ProcessType getEnvironment();

    /**
     * provides container's (application server's) callback handler
     * @return container callback handler
     */
    public CallbackHandler getCallbackHandler();

    /**
     * Checks whether the executing environment is application server
     * @return true if execution environment is server
     *         false if it is not server
     */
    boolean isServer();

    /**
     * Checks whether the executing environment is embedded runtime 
     * @return true if execution environment is embedded mode
     *         false if it non-embedded
     */
    boolean isEmbedded() ;

    /**
     * Initializes the execution environment. If the execution environment
     * is appserv runtime it is set to ConnectorConstants.SERVER else
     * it is set ConnectorConstants.CLIENT
     *
     * @param environment set to ConnectorConstants.SERVER if execution
     *                    environment is appserv runtime else set to
     *                    ConnectorConstants.CLIENT
     */
/*
    void initialize(int environment);
*/

    /**
     * provides connector class loader
     * @return ClassLoader
     */
    ClassLoader getConnectorClassLoader();

    /**
      * Gets the properties of the Java bean connection definition class that
      * have setter methods defined and the default values as provided by the
      * Connection Definition java bean developer.
      * This method is used to get properties of jdbc-data-source<br>
      * To get Connection definition properties for Connector Connection Pool,
      * use ConnectorRuntime.getMCFConfigProperties()<br>
      * When the connection definition class is not found, standard JDBC
      * properties (of JDBC 3.0 Specification) will be returned.<br>
      *
      * @param connectionDefinitionClassName
      *                     The Connection Definition Java bean class for which
      *                     overrideable properties are required.
      * @param resType resource-type
     * @return Map<String, Object> String represents property name
      * and Object is the defaultValue that is a primitive type or String
      */
    public Map<String, Object> getConnectionDefinitionPropertiesAndDefaults(String connectionDefinitionClassName, 
            String resType);

    /**
     * Provides the list of built in custom resources by
     * resource-type and factory-class-name pair.
     * @return map of resource-type & factory-class-name
     */
    public Map<String,String> getBuiltInCustomResources();

    /**
     * Returns the system RAR names that allow pool creation
     * @return String array representing list of system-rars
     */
    public String[] getSystemConnectorsAllowingPoolCreation();


    /** Obtains all the Connection definition names of a rar
     *  @param rarName rar moduleName
     *  @return Array of connection definition names.
     *  @throws ConnectorRuntimeException when unable to obtain connection definition from descriptor.
     */
    public String[] getConnectionDefinitionNames(String rarName)
               throws ConnectorRuntimeException ;

    /**
     *  Obtains the Permission string that needs to be added to the
     *  to the security policy files. These are the security permissions needed
     *  by the resource adapter implementation classes.
     *  These strings are obtained by parsing the ra.xml and by processing annotations if any
     *  @param moduleName rar module Name
     *  @throws ConnectorRuntimeException If rar.xml parsing or annotation processing fails.
     *  @return security permission spec
     */
    public String getSecurityPermissionSpec(String moduleName)
                         throws ConnectorRuntimeException ;

    /**
     * Obtains all the Admin object interface names of a rar
     * @param rarName rar moduleName
     * @return Array of admin object interface names.
     * @throws ConnectorRuntimeException when unable to obtain admin object interface names
     */
    public String[] getAdminObjectInterfaceNames(String rarName)
               throws ConnectorRuntimeException ;

    /**
     * Obtains all the Admin object implementation class names of the admin-object-interface for a rar
     * @param rarName rar moduleName
     * @param intfName admin-object-interface-name
     * @return Array of admin object interface names.
     * @throws ConnectorRuntimeException when unable to obtain admin object interface names
     */
    public String[] getAdminObjectClassNames(String rarName, String intfName)
        throws ConnectorRuntimeException;

    /**
     * checks whether the specified intfName, className has presence in
     * admin objects of the RAR
     * @param rarName resource-adapter name
     * @param intfName admin object interface name
     * @param className admin object class name
     * @return boolean indicating the presence of admin object
     * @throws ConnectorRuntimeException when unable to determine the presence
     */
    public boolean hasAdminObject(String rarName, String intfName, String className)
                throws ConnectorRuntimeException;

    /**
     *  Retrieves the Resource adapter javabean properties with default values.
     *  The default values will the values present in the ra.xml. If the
     *  value is not present in ra.xxml, javabean is introspected to obtain
     *  the default value present, if any. If intrspection fails or null is the
     *  default value, empty string is returned.
     *  If ra.xml has only the property and no value, empty string is the value
     *  returned.
     *  If the Resource Adapter Java bean is annotated, properties will be the result of merging
     *  annotated config property and config-property of Resource Adapter bean in ra.xml 
     *  @param rarName rar module name
     *  @return Resource adapter javabean properties with default values.
     *  @throws ConnectorRuntimeException if property retrieval fails.
     */
    public Map<String,String> getResourceAdapterConfigProps(String rarName)
                throws ConnectorRuntimeException ;

    /**
     *  Retrieves the MCF javabean properties with default values.
     *  The default values will the values present in the ra.xml. If the
     *  value is not present in ra.xxml, javabean is introspected to obtain
     *  the default value present, if any. If intrspection fails or null is the
     *  default value, empty string is returned.
     *  If ra.xml has only the property and no value, empty string is the value
     *  returned.
     *  If the ManagedConnectionFactory Java bean is annotated, properties will be the result of merging
     *  annotated config property and config-property of MCF in ra.xml

     *  @param rarName rar module name
     *  @param connectionDefName connection-definition-name
     *  @return managed connection factory javabean properties with
     *          default values.
     *  @throws ConnectorRuntimeException if property retrieval fails.
     */
    public Map<String,String> getMCFConfigProps(
     String rarName,String connectionDefName) throws ConnectorRuntimeException ;

    /**
     *  Retrieves the admin object javabean properties with default values.
     *  The default values will the values present in the ra.xml. If the
     *  value is not present in ra.xxml, javabean is introspected to obtain
     *  the default value present, if any. If intrspection fails or null is the
     *  default value, empty string is returned.
     *  If ra.xml has only the property and no value, empty string is the value
     *  returned.
     *  If the AdministeredObject Java bean is annotated, properties will be the result of merging
     *  annotated config property and config-property of AdministeredObject in ra.xml

     *  @param rarName rar module name
     *  @param adminObjectIntf admin-object-interface name
     * @return admin object javabean properties with
     *          default values.
     *  @throws ConnectorRuntimeException if property retrieval fails.
     */
    public Map<String,String> getAdminObjectConfigProps(
      String rarName,String adminObjectIntf) throws ConnectorRuntimeException ;

    /**
     *  Retrieves the admin object javabean properties with default values.
     *  The default values will the values present in the ra.xml. If the
     *  value is not present in ra.xxml, javabean is introspected to obtain
     *  the default value present, if any. If intrspection fails or null is the
     *  default value, empty string is returned.
     *  If ra.xml has only the property and no value, empty string is the value
     *  returned.
     *  If the AdministeredObject Java bean is annotated, properties will be the result of merging
     *  annotated config property and config-property of AdministeredObject in ra.xml

     *  @param rarName rar module name
     *  @param adminObjectIntf admin-object-interface name
     *  @param adminObjectClass admin-object-class name
     *  @return admin object javabean properties with
     *          default values.
     *  @throws ConnectorRuntimeException if property retrieval fails.
     */
    public Map<String,String> getAdminObjectConfigProps(
      String rarName,String adminObjectIntf, String adminObjectClass) throws ConnectorRuntimeException ;

    /**
     *  Retrieves the XXX javabean properties with default values.
     *  The javabean to introspect/retrieve is specified by the type.
     *  The default values will be the values present in the ra.xml. If the
     *  value is not present in ra.xxml, javabean is introspected to obtain
     *  the default value present, if any. If intrspection fails or null is the
     *  default value, empty string is returned.
     *  If ra.xml has only the property and no value, empty string is the value
     *  returned.
     *  @param rarName rar module name
     *  @param connectionDefName connection definition name
     *  @param type JavaBean type to introspect
     * @return admin object javabean properties with
     *          default values.
     *  @throws ConnectorRuntimeException if property retrieval fails.
     */
    public Map<String,String> getConnectorConfigJavaBeans(String rarName,
        String connectionDefName,String type) throws ConnectorRuntimeException ;

    /**
     * Return the ActivationSpecClass name for given rar and messageListenerType
     * @param rarName name of the rar module
     * @param messageListenerType MessageListener type
     * @throws  ConnectorRuntimeException If moduleDir is null.
     *          If corresponding rar is not deployed.
     * @return activation-spec class
     */
    public String getActivationSpecClass( String rarName,
             String messageListenerType) throws ConnectorRuntimeException ;

    /**
     *  Parses the ra.xml, processes the annotated rar artificats if any
     *  and returns all the Message listener types.
     *
     * @param  rarName name of the rar module.
     * @return Array of message listener types as strings.
     * @throws  ConnectorRuntimeException If moduleDir is null.
     *          If corresponding rar is not deployed.
     *
     */
    public String[] getMessageListenerTypes(String rarName)
               throws ConnectorRuntimeException  ;

    /** Parses the ra.xml for the ActivationSpec javabean
     *  properties and processes annotations if any. The ActivationSpec to be parsed is
     *  identified by the moduleDir where ra.xml is present and the
     *  message listener type.
     *
     *  message listener type will be unique in a given ra.xml.
     *
     *  It throws ConnectorRuntimeException if either or both the
     *  parameters are null, if corresponding rar is not deployed,
     *  if message listener type mentioned as parameter is not found in ra.xml.
     *  If rar is deployed and message listener (type mentioned) is present
     *  but no properties are present for the corresponding message listener,
     *  null is returned.
     *
     *  @param  rarName name of the rar module.
     *  @param  messageListenerType message listener type.It is uniqie
     *          across all <messagelistener> sub-elements in <messageadapter>
     *          element in a given rar.
     *  @return Javabean properties with the property names and values
     *          of properties. The property values will be the values
     *          mentioned in ra.xml if present. Otherwise it will be the
     *          default values obtained by introspecting the javabean.
     *          In both the case if no value is present, empty String is
     *          returned as the value.
     *  @throws  ConnectorRuntimeException if either of the parameters are null.
     *           If corresponding rar is not deployed i.e moduleDir is invalid.
     *           If messagelistener type is not found in ra.xml or could not be
     *           found in annotations if any
     */
    public Map<String,String> getMessageListenerConfigProps(String rarName,
         String messageListenerType)throws ConnectorRuntimeException ;

    /** Returns the Properties object consisting of propertyname as the
     *  key and datatype as the value.
     *  @param  rarName name of the rar module.
     *  @param  messageListenerType message listener type.It is uniqie
     *          across all <messagelistener> sub-elements in <messageadapter>
     *          element in a given rar.
     *  @return Properties object with the property names(key) and datatype
     *          of property(as value).
     *  @throws  ConnectorRuntimeException if either of the parameters are null.
     *           If corresponding rar is not deployed i.e moduleDir is invalid.
     *           If messagelistener type is not found in ra.xmlor could not be found
     *           in annotations if any
     */
    public Map<String,String> getMessageListenerConfigPropTypes(String rarName,
               String messageListenerType) throws ConnectorRuntimeException ;

    /**
     * get work security maps for a resource-adapter-name
     * @param raName resource-adapter name
     * @return all work security maps of a resource-adapter
     */
    public List<WorkSecurityMap> getWorkSecurityMap(String raName);

    /**
     * Used to register data-source-definitions at an earlier stage of deployment (prepare phase).
     * This is used to register "java:global" and "java:app" scoped DataSourceDefinitions
     * which can be referred by JPA in persistence.xml
     * @param application Application being deployed.
     */
    public void registerDataSourceDefinitions(com.sun.enterprise.deployment.Application application);

    /**
     * Used to unRegister data-source-definitions at an later stage of undeploy operation.
     * This is used to unRegister "java:global" and "java:app" scoped DataSourceDefinitions
     * which can be referred by JPA in persistence.xml
     * @param application Application being undeployed.
     */
    public void unRegisterDataSourceDefinitions(com.sun.enterprise.deployment.Application application);
    
    /**
     * Flush Connection pool by reinitializing the connections 
     * established in the pool.
     * @param PoolInfo connection pool info
     * @throws ConnectorRuntimeException
     */
    public boolean flushConnectionPool(PoolInfo poolInfo) throws ConnectorRuntimeException;

    /**
     * Flush Connection pool by reinitializing the connections
     * established in the pool.
     * @param PoolInfo connection pool info
     * @throws ConnectorRuntimeException
     */
    public boolean flushConnectionPool(String poolName) throws ConnectorRuntimeException;

     /**
     * Fetch the DataSource/Driver implementation class names for a particular 
     * dbVendor and resource type. Sometimes an already stored datasource <br>
     * classname is used in this method.
     * @param dbVendor database vendor name
     * @param resType resource-type <br>
      * (javax.sql.DataSource/javax.sql.ConnectionPoolDataSource/javax.sql.XADataSource/java.sql.Driver)
     * @return set of implementation class names for the dbvendor.
     */
    public Set<String> getJdbcDriverClassNames(String dbVendor, String resType);

    /**
     *
     * Fetch the DataSource/Driver implementation class names for a particular
     * dbVendor and resource type. A pre-defined datasource or driver <br>
     * classname is returned by this method by default for common database
     * vendors. When introspect is true, classnames
     * are got by introspection of the jdbc driver jar.
     * @param dbVendor database vendor name
     * @param resType resource-type
     * @param introspect
     * (javax.sql.DataSource/javax.sql.ConnectionPoolDataSource/javax.sql.XADataSource/java.sql.Driver)
     * @return set of implementation class names for the dbvendor.
     */
    public Set<String> getJdbcDriverClassNames(String dbVendor, String resType,
            boolean introspect);

    /**
     * Check if Ping attribute is on during pool creation. This is used for
     * pinging the pool for erroneous values during pool creation.
     * 
     * @param PoolInfo connection pool info
     * @return true if ping is on
     */
    public boolean getPingDuringPoolCreation(PoolInfo poolInfo);

    /**
     * given a resource-adapter name, retrieves the connector-descriptor
     * either from runtime's registry or by reading the descriptor from
     * deployment location.
     * @param rarName resource-adapter-name
     * @return ConnectorDescriptor of the .rar
     * @throws ConnectorRuntimeException when unable to provide the descriptor
     */
    public ConnectorDescriptor getConnectorDescriptor(String rarName)
            throws ConnectorRuntimeException ;
    
    /**
     * Get jdbc database vendor names list. This is used for getting a list
     * of all common database vendor names.
     * @return set of common database vendor names
     */
    public Set<String> getDatabaseVendorNames();

    /**
     * associates the given instance of ResourceAdapterAssociation with
     * the ResourceAdapter java-bean of the specified RAR
     * @param rarName resource-adapter-name
     * @param raa Object that is an instance of ResourceAdapterAssociation
     * @throws ResourceException when unable to associate the RA Bean with RAA instance.
     */
    public void associateResourceAdapter(String rarName, ResourceAdapterAssociation raa)
            throws ResourceException;

    /**
     * Gets the shutdown-timeout attribute configured in <code>connector-service</code>
     * @return long shutdown timeout (in milli-seconds)
     */
    public long getShutdownTimeout() ;


    /**
     * Retrieve the "confidential" config properties of specified artifact from a resource-adapter</br>
     * @param rarName resource-adapter name
     * @param type indicates the artifact type. Valid values are : ConnectorConstants.AOR, RA, MCF, MSL
     * @param keyFields var-args list of key fields.
     *                  eg: connection-definition-name when
     * @return list of confidential properties
     * @throws ConnectorRuntimeException
     */
    public List<String> getConfidentialProperties(String rarName,
                                           String type, String... keyFields) throws ConnectorRuntimeException ;

}

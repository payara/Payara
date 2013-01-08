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

package org.glassfish.jdbc.config;

import com.sun.enterprise.config.serverbeans.Resource;
import com.sun.enterprise.config.serverbeans.ResourcePool;
import org.glassfish.api.admin.RestRedirect;
import org.glassfish.api.admin.RestRedirects;
import org.glassfish.api.admin.config.PropertiesDesc;
import org.glassfish.api.admin.config.PropertyDesc;
import org.glassfish.config.support.datatypes.Port;
import org.glassfish.connectors.config.validators.ConnectionPoolErrorMessages;
import org.glassfish.jdbc.config.validators.JdbcConnectionPoolConstraint;
import org.glassfish.jdbc.config.validators.JdbcConnectionPoolConstraints;
import org.glassfish.admin.cli.resources.UniqueResourceNameConstraint;
import org.glassfish.resourcebase.resources.ResourceDeploymentOrder;
import org.glassfish.resourcebase.resources.ResourceTypeOrder;
import org.jvnet.hk2.config.*;
import org.jvnet.hk2.config.types.Property;
import org.jvnet.hk2.config.types.PropertyBag;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import java.beans.PropertyVetoException;
import java.util.List;

/**
 * Defines configuration used to create and manage a pool physical database
 * connections. Pool definition is named, and can be referred to by multiple
 * jdbc-resource elements (See <jdbc-resource>).
 * Each named pool definition results in a pool instantiated at server start-up.
 * Pool is populated when accessed for the first time. If two or more
 * jdbc-resource elements point to the same jdbc-connection-pool element,
 * they are using the same pool of connections, at run time.         
 */

/* @XmlType(name = "", propOrder = {
    "description",
    "property"
}) */

@Configured
@JdbcConnectionPoolConstraints ({
    @JdbcConnectionPoolConstraint(value = ConnectionPoolErrorMessages.MAX_STEADY_INVALID),
    @JdbcConnectionPoolConstraint(value = ConnectionPoolErrorMessages.STMT_WRAPPING_DISABLED),
    @JdbcConnectionPoolConstraint(value = ConnectionPoolErrorMessages.RES_TYPE_MANDATORY),
    @JdbcConnectionPoolConstraint(value = ConnectionPoolErrorMessages.TABLE_NAME_MANDATORY),
    @JdbcConnectionPoolConstraint(value = ConnectionPoolErrorMessages.CUSTOM_VALIDATION_CLASS_NAME_MANDATORY)
})
@RestRedirects({
 @RestRedirect(opType = RestRedirect.OpType.POST, commandName = "create-jdbc-connection-pool"),
 @RestRedirect(opType = RestRedirect.OpType.DELETE, commandName = "delete-jdbc-connection-pool")
})
@ResourceTypeOrder(deploymentOrder=ResourceDeploymentOrder.JDBC_POOL)
@UniqueResourceNameConstraint(message="{resourcename.isnot.unique}", payload=JdbcConnectionPool.class)
public interface JdbcConnectionPool extends ConfigBeanProxy, Resource, ResourcePool,
    PropertyBag {

    /**
     *
     * Gets the value of the datasourceClassname property.
     * 
     * Name of the vendor supplied JDBC datasource resource manager.
     * An XA or global transactions capable datasource class will implement
     * javax.sql.XADatasource interface. Non XA or Local transactions only
     * datasources will implement javax.sql.Datasource interface.
     * 
     @return possible object is
     *         {@link String }
     */
    @Attribute
    String getDatasourceClassname();

    /**
     * Sets the value of the datasourceClassname property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setDatasourceClassname(String value) throws PropertyVetoException;

    /**
     *
     * Gets the value of the driverClassname property.
     * 
     * Name of the vendor supplied JDBC driver resource manager.
     * Get classnames that implement java.sql.Driver.
     * 
     @return possible object is
     *         {@link String }
     */
    @Attribute
    String getDriverClassname();

    /**
     * Sets the value of the driverClassname property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setDriverClassname(String value) throws PropertyVetoException;

    /**
     * Gets the value of the resType property.
     *
     * DataSource implementation class could implement one of
     * javax.sql.DataSource, javax.sql.XADataSource or
     * javax.sql.ConnectionPoolDataSource interfaces. This optional attribute
     * must be specified to disambiguate when a Datasource class implements two
     * or more of these interfaces. An error is produced when this attribute has
     * a legal value and the indicated interface is not implemented by the
     * datasource class. This attribute has no default value.
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    @Pattern(regexp="(java.sql.Driver|javax.sql.DataSource|javax.sql.XADataSource|javax.sql.ConnectionPoolDataSource)")
    String getResType();

    /**
     * Sets the value of the resType property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setResType(String value) throws PropertyVetoException;

    /**
     * Gets the value of the steadyPoolSize property.
     *
     * Minimum and initial number of connections maintained in the pool
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="8")
    @Min(value=0)
    @Max(value=Integer.MAX_VALUE)
    String getSteadyPoolSize();

    /**
     * Sets the value of the steadyPoolSize property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setSteadyPoolSize(String value) throws PropertyVetoException;

    /**
     * Gets the value of the maxPoolSize property.
     *
     * Maximum number of conections that can be created
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="32")
    @Min(value=1)
    @Max(value=Integer.MAX_VALUE)
    String getMaxPoolSize();

    /**
     * Sets the value of the maxPoolSize property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setMaxPoolSize(String value) throws PropertyVetoException;

    /**
     * Gets the value of the maxWaitTimeInMillis property.
     *
     * Amount of time the caller will wait before getting a connection timeout.
     * Default is 60 sec. A value of 0 will force caller to wait indefinitely.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="60000")
    @Min(value=0)
    @Max(value=Integer.MAX_VALUE)
    String getMaxWaitTimeInMillis();

    /**
     * Sets the value of the maxWaitTimeInMillis property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setMaxWaitTimeInMillis(String value) throws PropertyVetoException;

    /**
     * Gets the value of the poolResizeQuantity property.
     *
     * Number of connections to be removed when dle-timeout-in-seconds timer
     * expires. Connections that have idled for longer than the timeout are
     * candidates for removal. When the pool size reaches steady-pool-size,
     * the connection removal stops.
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="2")
    @Min(value=1)
    @Max(value=Integer.MAX_VALUE)
    String getPoolResizeQuantity();

    /**
     * Sets the value of the poolResizeQuantity property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setPoolResizeQuantity(String value) throws PropertyVetoException;

    /**
     * Gets the value of the idleTimeoutInSeconds property.
     *
     * maximum time in seconds, that a connection can remain idle in the pool.
     * After this time, the pool implementation can close this connection.
     * Note that this does not control connection timeouts enforced at the
     * database server side. Adminsitrators are advised to keep this timeout
     * shorter than the database server side timeout (if such timeouts are
     * configured on the specific vendor's database), to prevent accumulation of
     * unusable connection in Application Server.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="300")
    @Min(value=0)
    @Max(value=Integer.MAX_VALUE)
    String getIdleTimeoutInSeconds();

    /**
     * Sets the value of the idleTimeoutInSeconds property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setIdleTimeoutInSeconds(String value) throws PropertyVetoException;

    /**
     * Gets the value of the transactionIsolationLevel property.
     *
     * Specifies the Transaction Isolation Level on pooled database connections.
     * Optional. Has no default. If left unspecified the pool operates with
     * default isolation level provided by the JDBC Driver. A desired isolation
     * level can be set using one of the standard transaction isolation levels,
     * which see.
     *
     * Applications that change the Isolation level on a pooled connection
     * programmatically, risk polluting the pool and this could lead to program
     * errors. Also see: is-isolation-level-guaranteed
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    @Pattern(regexp="(read-uncommitted||read-committed|repeatable-read|serializable)")
    String getTransactionIsolationLevel();

    /**
     * Sets the value of the transactionIsolationLevel property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setTransactionIsolationLevel(String value) throws PropertyVetoException;

    /**
     * Gets the value of the isIsolationLevelGuaranteed property.
     *
     * Applicable only when a particular isolation level is specified for
     * transaction-isolation-level. The default value is true.
     * This assures that every time a connection is obtained from the pool,
     * it is guaranteed to have the isolation set to the desired value.
     * This could have some performance impact on some JDBC drivers. Can be set
     * to false by that administrator when they are certain that the application
     * does not change the isolation level before returning the connection.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="true", dataType=Boolean.class)
    String getIsIsolationLevelGuaranteed();

    /**
     * Sets the value of the isIsolationLevelGuaranteed property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setIsIsolationLevelGuaranteed(String value) throws PropertyVetoException;

    /**
     * Gets the value of the isConnectionValidationRequired property.
     *
     * if true, connections are validated (checked to find out if they are
     * usable) before giving out to the application. The default is false.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="false", dataType=Boolean.class)
    String getIsConnectionValidationRequired();

    /**
     * Sets the value of the isConnectionValidationRequired property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setIsConnectionValidationRequired(String value) throws PropertyVetoException;

    /**
     * Gets the value of the connectionValidationMethod property.
     *
     * specifies the type of validation to be performed when
     * is-connection-validation-required is true. The following types of
     * validation are supported:
     * 
     * auto-commit
     *   using connection.autoCommit()
     * meta-data
     *   using connection.getMetaData()
     * table
     *   performing a query on a user specified table(see validation-table-name)
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="table")
    @Pattern(regexp="(auto-commit|meta-data|custom-validation|table)")
    String getConnectionValidationMethod();

    /**
     * Sets the value of the connectionValidationMethod property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setConnectionValidationMethod(String value) throws PropertyVetoException;

    /**
     * Gets the value of the validationTableName property.
     *
     * Specifies the table name to be used to perform a query to validate a
     * connection. This parameter is mandatory, if connection-validation-type is
     * set to table. Verification by accessing a user specified table may become
     * necessary for connection validation, particularly if database driver 
     * caches calls to setAutoCommit() and getMetaData().
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    String getValidationTableName();

    /**
     * Sets the value of the validationTableName property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setValidationTableName(String value) throws PropertyVetoException;

    /**
     * Gets the value of the validationClassName property.
     *
     * Specifies the custom validation class name to be used to perform 
     * connection validation. This parameter is mandatory, if connection-validation-type is
     * set to custom-validation. 
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    String getValidationClassname();

    /**
     * Sets the value of the validationClassName property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setValidationClassname(String value) throws PropertyVetoException;

    /**
     * Gets the value of the failAllConnections property.
     *
     * Indicates if all connections in the pool must be closed should a single
     * validation check fail. The default is false. One attempt will be made to
     * re-establish failed connections.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="false", dataType=Boolean.class)
    String getFailAllConnections();

    /**
     * Sets the value of the failAllConnections property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setFailAllConnections(String value) throws PropertyVetoException;

    /**
     * Gets the value of the nonTransactionalConnections property.
     *
     * A pool with this property set to true returns non-transactional
     * connections. This connection does not get automatically enlisted
     * with the transaction manager.
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="false", dataType=Boolean.class)
    String getNonTransactionalConnections();

    /**
     * Sets the value of the nonTransactionalConnections property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setNonTransactionalConnections(String value) throws PropertyVetoException;

    /**
     * Gets the value of the allowNonComponentCallers property.
     *
     * A pool with this property set to true, can be used by non-J2EE components
     * (i.e components other than EJBs or Servlets). The returned connection is
     * enlisted automatically with the transaction context obtained from the
     * transaction manager. This property is to enable the pool to be used by
     * non-component callers such as ServletFilters, Lifecycle modules, and
     * 3rd party persistence managers. Standard J2EE components can continue to
     * use such pools. Connections obtained by non-component callers are not
     * automatically cleaned at the end of a transaction by the container. They
     * need to be explicitly closed by the the caller.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="false", dataType=Boolean.class)
    String getAllowNonComponentCallers();

    /**
     * Sets the value of the allowNonComponentCallers property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setAllowNonComponentCallers(String value) throws PropertyVetoException;

    /**
     * Gets the value of the validateAtmostOncePeriodInSeconds property.
     *
     * Used to set the time-interval within which a connection is validated
     * atmost once. Default is 0 which implies that it is  not enabled.
     * TBD: Documentation is to be corrected.
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="0")
    @Min(value=0)
    @Max(value=Integer.MAX_VALUE)
    String getValidateAtmostOncePeriodInSeconds();

    /**
     * Sets the value of the validateAtmostOncePeriodInSeconds property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setValidateAtmostOncePeriodInSeconds(String value) throws PropertyVetoException;

    /**
     * Gets the value of the connectionLeakTimeoutInSeconds property.
     *
     * To aid user in detecting potential connection leaks by the application.
     * When a connection is not returned back to the pool by the application
     * within the specified period, it is assumed to be a potential leak and
     * stack trace of the caller will be logged. Default is 0, which implies
     * there is no leak detection, by default. A positive non-zero value turns
     * on leak detection. Note however that, this attribute only detects if
     * there is a connection leak. The connection can be reclaimed only if
     * connection-leak-reclaim is set to true.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="0")
    @Min(value=0)
    @Max(value=Integer.MAX_VALUE)
    String getConnectionLeakTimeoutInSeconds();

    /**
     * Sets the value of the connectionLeakTimeoutInSeconds property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setConnectionLeakTimeoutInSeconds(String value) throws PropertyVetoException;

    /**
     * Gets the value of the connectionLeakReclaim property.
     *
     * If enabled, connection will be reusable (put back into pool) after
     * connection-leak-timeout-in-seconds occurs. Default value is false.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="false", dataType=Boolean.class)
    String getConnectionLeakReclaim();

    /**
     * Sets the value of the connectionLeakReclaim property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setConnectionLeakReclaim(String value) throws PropertyVetoException;

    /**
     * Gets the value of the connectionCreationRetryAttempts property.
     *
     * The number of attempts to create a new connection.
     * Default is 0, which implies no retries.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="0")
    @Min(value=0)
    @Max(value=Integer.MAX_VALUE)
    String getConnectionCreationRetryAttempts();

    /**
     * Sets the value of the connectionCreationRetryAttempts property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setConnectionCreationRetryAttempts(String value) throws PropertyVetoException;

    /**
     * Gets the value of the connectionCreationRetryIntervalInSeconds property.
     *
     * The time interval between retries while attempting to create a connection
     * Default is 10 seconds. Effective when connection-creation-retry-attempts
     * is greater than 0.
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="10")
    @Min(value=0)
    @Max(value=Integer.MAX_VALUE)
    String getConnectionCreationRetryIntervalInSeconds();

    /**
     * Sets the value of the connectionCreationRetryIntervalInSeconds property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setConnectionCreationRetryIntervalInSeconds(String value) throws PropertyVetoException;

    /**
     * Gets the value of the statementTimeoutInSeconds property.
     *
     * Sets the timeout property of a connection to enable termination of
     * abnormally long running queries. Default value of -1 implies that it is
     * not enabled.
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="-1", dataType=Integer.class)
    @Min(value=-1)
    String getStatementTimeoutInSeconds();

    /**
     * Sets the value of the statementTimeoutInSeconds property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setStatementTimeoutInSeconds(String value) throws PropertyVetoException;

    /**
     * Gets the value of the lazyConnectionEnlistment property.
     *
     * Enlist a resource to the transaction only when it is actually used in a
     * method, which avoids enlistment of connections that are not used in a
     * transaction. This also prevents unnecessary enlistment of connections
     * cached in the calling components. Default value is false
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="false", dataType=Boolean.class)
    String getLazyConnectionEnlistment();

    /**
     * Sets the value of the lazyConnectionEnlistment property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setLazyConnectionEnlistment(String value) throws PropertyVetoException;

    /**
     * Gets the value of the lazyConnectionAssociation property.
     *
     * Connections are lazily associated when an operation is performed on them.
     * Also, they are disassociated when the transaction is completed and a
     * component method ends, which helps reuse of the physical connections.
     * Default value is false.
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="false", dataType=Boolean.class)
    String getLazyConnectionAssociation();

    /**
     * Sets the value of the lazyConnectionAssociation property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setLazyConnectionAssociation(String value) throws PropertyVetoException;

    /**
     * Gets the value of the associateWithThread property.
     *
     * Associate a connection with the thread such that when the same thread is
     * in need of a connection, it can reuse the connection already associated
     * with that thread, thereby not incurring the overhead of getting a
     * connection from the pool. Default value is false
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="false", dataType=Boolean.class)
    String getAssociateWithThread();

    /**
     * Sets the value of the associateWithThread property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setAssociateWithThread(String value) throws PropertyVetoException;

    /**
     * Gets the value of the pooling property.
     *
     * Property to disable pooling for the pool.
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="true", dataType=Boolean.class)
    String getPooling();

    /**
     * Sets the value of the pooling property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setPooling(String value) throws PropertyVetoException;


    /**
     * Gets the value of the statementCacheSize property.
     *
     * When specified, statement caching is turned on to cache statements, 
     * prepared statements, callable statements that are repeatedly executed by 
     * applications. Default value is 0, which implies the
     * feature is not enabled.
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="0")
    @Min(value=0)
    @Max(value=Integer.MAX_VALUE)
    String getStatementCacheSize();

    /**
     * Sets the value of the statementCacheSize property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setStatementCacheSize(String value) throws PropertyVetoException;

    /**
     * Gets the value of the statementCacheType property.
     *
     * When specified, statement caching type is set to cache statements, 
     * prepared statements, callable statements that are repeatedly executed by 
     * applications. 
     *  
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="")
    String getStatementCacheType();

    /**
     * Sets the value of the statementCacheType property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setStatementCacheType(String value) throws PropertyVetoException;

    /**
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="0")
    @Min(value=0)
    @Max(value=Integer.MAX_VALUE)
    String getStatementLeakTimeoutInSeconds();

    /**
     * Sets the value of the statementLeakTimeoutInSeconds property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setStatementLeakTimeoutInSeconds(String value) throws PropertyVetoException;

    @Attribute (defaultValue="false", dataType=Boolean.class)
    String getStatementLeakReclaim();

    /**
     * Sets the value of the statementLeakReclaim property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setStatementLeakReclaim(String value) throws PropertyVetoException;


    /**
     * Gets the value of the initSql property.
     *
     * Init sql is executed whenever a connection created from the pool. 
     * This is mostly useful when the state of a
     * connection is to be initialized
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    String getInitSql();

    /**
     * Sets the value of the initSql property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setInitSql(String value) throws PropertyVetoException;

    /**
     * Gets the value of the matchConnections property.
     *
     * To switch on/off connection matching for the pool. It can be set to false
     * if the administrator knows that the connections in the pool will always
     * be homogeneous and hence a connection picked from the pool need not be
     * matched by the resource adapter. Default value is false.
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="false", dataType=Boolean.class)
    String getMatchConnections();

    /**
     * Sets the value of the matchConnections property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setMatchConnections(String value) throws PropertyVetoException;

    /**
     * Gets the value of the maxConnectionUsageCount property.
     *
     * When specified, connections will be re-used by the pool for the specified
     * number of times after which it will be closed. This is useful for
     * instance, to avoid statement-leaks. Default value is 0, which implies the
     * feature is not enabled.
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="0")
    @Min(value=0)
    @Max(value=Integer.MAX_VALUE)
    String getMaxConnectionUsageCount();

    /**
     * Sets the value of the maxConnectionUsageCount property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setMaxConnectionUsageCount(String value) throws PropertyVetoException;

    /**
     * Gets the value of the wrapJdbcObjects property.
     *
     * When set to true, application will get wrapped jdbc objects for
     * Statement, PreparedStatement, CallableStatement, ResultSet,
     * DatabaseMetaData. Defaults to false.
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="true", dataType=Boolean.class)
    String getWrapJdbcObjects();

    /**
     * Sets the value of the wrapJdbcObjects property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setWrapJdbcObjects(String value) throws PropertyVetoException;
    
    /**
     * Gets the value of the SqlTraceListeners property.
     *
     * Comma separated list of SQL trace listener implementations to be used to 
     * trace the SQL statements executed by the applications. The default 
     * logger used by the system logs the SQL statements based on a set of 
     * values stored in SQLTraceRecord object.
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    String getSqlTraceListeners();

    /**
     * Sets the value of the sqltracelisteners property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setSqlTraceListeners(String value) throws PropertyVetoException;

    /**
     * Gets the value of the description property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    String getDescription();

    /**
     * Sets the value of the description property.
     *
     * @param value allowed object is
     *
     *              {@link String }
     */
    void setDescription(String value) throws PropertyVetoException;

    /**
     *   Properties.  This list is likely incomplete as of 21 October 2008.
     *
     * Most JDBC 2.0 drivers permit use of standard property lists, to specify
     * User, Password and other resource configuration. While these are optional
     * properties, according to the specification, several of these properties
     * may be necessary for most databases. See Section 5.3 of JDBC 2.0 Standard
     * Extension API.
     * 
     * The following are the names and corresponding values for these properties
     * databaseName
     *      Name of the Database
     * serverName
     *      Database Server name.
     * port
     *      Port where a Database server is listening for requests.
     * networkProtocol
     *      Communication Protocol used.
     * user
     *      default name of the database user with which connections
     *      will be stablished. Programmatic database authentication
     *      or default-resource-principal specified in vendor
     *      specific web and ejb deployment descriptors will take
     *      precedence, over this default. The details and caveats
     *      are described in detail in the Administrator's guide.
     * password
     *      password for default database user
     *  roleName
     *      The initial SQL role name.
     * datasourceName
     *      used to name an underlying XADataSource, or ConnectionPoolDataSource
     *      when pooling of connections is done 
     * description
     *      Textual Description
     *
     * When one or more of these properties are specified, they are passed as
     * is using set<Name>(<Value>) methods to the vendors Datasource class
     * (specified in datasource-classname). User and Password properties are
     * used as default principal, if Container Managed authentication is
     * specified and a default-resource-principal is not found in application
     * deployment descriptors.
     *
     */
@PropertiesDesc(
    props={
        @PropertyDesc(name="PortNumber", defaultValue="1527", dataType=Port.class,
            description="Port on which the database server listens for requests"),

        @PropertyDesc(name="Password", defaultValue="APP",
            description="Password for connecting to the database"),

        @PropertyDesc(name="User", defaultValue="APP",
            description="User name for connecting to the database"),

        @PropertyDesc(name="serverName", defaultValue="localhost",
            description="Database server for this connection pool"),

        @PropertyDesc(name="DatabaseName", defaultValue="sun-appserv-samples",
            description="Database for this connection pool."),

        @PropertyDesc(name="connectionAttributes", defaultValue=";create=true",
            description="connection attributes")
    }
    )
    @Element
    List<Property> getProperty();

    @DuckTyped
    String getIdentity();

    class Duck {
        public static String getIdentity(JdbcConnectionPool resource){
            return resource.getName();
        }
    }

}

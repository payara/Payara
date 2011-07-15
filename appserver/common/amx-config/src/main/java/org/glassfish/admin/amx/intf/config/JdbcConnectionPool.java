/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admin.amx.intf.config;


/**
 * @deprecated Configuration for the &lt;jdbc-connection-pool&gt; element.
 * <p/>
 * NOTE: some getters/setters use java.lang.String. This is a problem; these
 * methods cannot use the AppServer template facility, whereby an Attribute value can be of
 * the form attr-name=${ATTR_VALUE}.  For an example of where/how this facility is used, see
 * the &lt;http-listener> element, which looks like this:<br/>
 * <pre>
 * &lt;http-listener id="http-listener-1" address="0.0.0.0" port="${HTTP_LISTENER_PORT}" acceptor-threads="1" security-enabled="false" default-virtual server="server" server-name="" xpowered-by="true" enabled="true">
 * </pre>
 * The 'port' attribute above is set to the value "${HTTP_LISTENER_PORT}", which is a system
 * property.  Obviously no method that uses 'String' could get or set a String.
 */
@Deprecated
public interface JdbcConnectionPool
        extends Description, NamedConfigElement, PropertiesAccess, ResourceRefReferent {
    /**
     * Key for use with {@link ResourcesConfig#createJDBCConnectionPoolConfig(String, String, Map)}
     * <p/>
     * See {@link ConnectionValidationMethodValues}.
     */
    public final static String CONNECTION_VALIDATION_METHOD_KEY = "connection-validation-method";
    /**
     * Key for use with @link { ResourcesConfig#createJDBCConnectionPoolConfig(String, String, Map)}
     */
    public final static String VALIDATION_TABLE_NAME_KEY = "validation-table-name";
    /**
     * Key for use with @link { ResourcesConfig#createJDBCConnectionPoolConfig(String, String, Map)}
     */
    public final static String DATASOURCE_CLASSNAME_KEY = "datasource-classname";
    /**
     * Key for use with {@link ResourcesConfig#createJDBCConnectionPoolConfig(String, String, Map)}
     */
    public final static String FAIL_ALL_CONNECTIONS_KEY = "fail-all-connections";
    /**
     * Key for use with @link { ResourcesConfig#createJDBCConnectionPoolConfig(String, String, Map)}
     */
    public final static String IDLE_TIMEOUT_IN_SECONDS_KEY = "idle-timeout-in-seconds";
    /**
     * Key for use with @link { ResourcesConfig#createJDBCConnectionPoolConfig(String, String, Map)}
     */
    public final static String IS_CONNECTION_VALIDATION_REQUIRED_KEY = "is-connection-validation-required";
    /**
     * Key for use with @link { ResourcesConfig#createJDBCConnectionPoolConfig(String, String, Map)}
     */
    public final static String IS_ISOLATION_LEVEL_GUARANTEED_KEY = "is-isolation-level-guaranteed";
    /**
     * Key for use with @link { ResourcesConfig#createJDBCConnectionPoolConfig(String, String, Map)}
     * See {@link IsolationValues}.
     */
    public final static String TRANSACTION_ISOLATION_LEVEL_KEY = "transaction-isolation-level";
    /**
     * Key for use with @link { ResourcesConfig#createJDBCConnectionPoolConfig(String, String, Map)}
     */
    public final static String MAX_POOL_SIZE_KEY = "max-pool-size";
    /**
     * Key for use with @link { ResourcesConfig#createJDBCConnectionPoolConfig(String, String, Map)}
     */
    public final static String MAX_WAIT_TIME_MILLIS_KEY = "max-wait-time-in-millis";
    /**
     * Key for use with @link { ResourcesConfig#createJDBCConnectionPoolConfig(String, String, Map)}
     */
    public final static String POOL_RESIZE_QUANTITY_KEY = "pool-resize-quantity";
    /**
     * Key for use with @link { ResourcesConfig#createJDBCConnectionPoolConfig(String, String, Map)}
     */
    public final static String NON_TRANSACTIONAL_CONNECTIONS_KEY = "non-transactional-connections";
    /**
     * Key for use with @link { ResourcesConfig#createJDBCConnectionPoolConfig(String, String, Map)}
     */
    public final static String ALLOW_NON_COMPONENT_CALLERS_KEY = "allow-non-component-callers";
    /**
     * Key for use with @link { ResourcesConfig#createJDBCConnectionPoolConfig(String, String, Map)}
     * Possible values:
     * <ul>
     * <li>javax.sql.DataSource</li>
     * <li>javax.sql.XADataSource</li>
     * <li>javax.sql.ConnectionPoolDataSource</li>
     * </ul>
     */
    public final static String RES_TYPE_KEY = "res-type";
    /**
     * Key for use with @link { ResourcesConfig#createJDBCConnectionPoolConfig(String, String, Map)}
     */
    public final static String STEADY_POOL_SIZE_KEY = "steady-pool-size";
    /**
     * Key for use with @link { ResourcesConfig#createJDBCConnectionPoolConfig(String, String, Map)}
     */
    public final static String DATABASE_NAME_KEY = "property.DatabaseName";
    /**
     * Key for use with @link { ResourcesConfig#createJDBCConnectionPoolConfig(String, String, Map)}
     */
    public final static String DATABASE_USER_KEY = "property.User";
    /**
     * Key for use with @link { ResourcesConfig#createJDBCConnectionPoolConfig(String, String, Map)}
     */
    public final static String DATABASE_PASSWORD_KEY = "property.Password";

    public String getDescription();

    public void setDescription(String param1);

    public String getResType();

    public void setResType(String param1);

    public String getIsConnectionValidationRequired();

    public void setIsConnectionValidationRequired(String param1);

    public String getFailAllConnections();

    public void setFailAllConnections(String param1);

    public String getIdleTimeoutInSeconds();

    public void setIdleTimeoutInSeconds(String param1);

    public String getMaxPoolSize();

    public void setMaxPoolSize(String param1);

    public String getMaxWaitTimeInMillis();

    public void setMaxWaitTimeInMillis(String param1);

    public String getPoolResizeQuantity();

    public void setPoolResizeQuantity(String param1);

    public String getSteadyPoolSize();

    public void setSteadyPoolSize(String param1);

    public String getConnectionLeakTimeoutInSeconds();

    public void setConnectionLeakTimeoutInSeconds(String param1);

    public String getConnectionLeakReclaim();

    public void setConnectionLeakReclaim(String param1);

    public String getConnectionCreationRetryAttempts();

    public void setConnectionCreationRetryAttempts(String param1);

    public String getConnectionCreationRetryIntervalInSeconds();

    public void setConnectionCreationRetryIntervalInSeconds(String param1);

    public String getValidateAtmostOncePeriodInSeconds();

    public void setValidateAtmostOncePeriodInSeconds(String param1);

    public String getLazyConnectionEnlistment();

    public void setLazyConnectionEnlistment(String param1);

    public String getLazyConnectionAssociation();

    public void setLazyConnectionAssociation(String param1);

    public String getAssociateWithThread();

    public void setAssociateWithThread(String param1);

    public String getMatchConnections();

    public void setMatchConnections(String param1);

    public String getMaxConnectionUsageCount();

    public void setMaxConnectionUsageCount(String param1);

    public String getConnectionValidationMethod();

    public void setConnectionValidationMethod(String param1);

    public String getDatasourceClassname();

    public void setDatasourceClassname(String param1);

    public String getIsIsolationLevelGuaranteed();

    public void setIsIsolationLevelGuaranteed(String param1);

    public String getTransactionIsolationLevel();

    public void setTransactionIsolationLevel(String param1);

    public String getValidationTableName();

    public void setValidationTableName(String param1);

    public String getNonTransactionalConnections();

    public void setNonTransactionalConnections(String param1);

    public String getAllowNonComponentCallers();

    public void setAllowNonComponentCallers(String param1);

    public String getWrapJdbcObjects();

    public void setWrapJdbcObjects(String param1);

    public String getStatementTimeoutInSeconds();

    public void setStatementTimeoutInSeconds(String param1);

    public String getStatementCacheSize();

    public void setStatementCacheSize(String param1);

    public String getSqlTraceListeners();

    public void setSqlTraceListeners(String param1);

    public String getValidationClassname();

    public void setValidationClassname(String param1);

    public String getPing();

    public void setPing(String param1);

    public String getPooling();

    public void setPooling(String param1);

    public String getInitSql();

    public void setInitSql(String param1);

    public String getDriverClassname();

    public void setDriverClassname(String param1);

    public String getStatementLeakTimeoutInSeconds();

    public void setStatementLeakTimeoutInSeconds(String param1);

    public String getStatementLeakReclaim();

    public void setStatementLeakReclaim(String param1);

}

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

package org.glassfish.resources.admin.cli;

/**
 * A constants class housing all the resource related constants
 * @author PRASHANTH ABBAGANI
 */
public final class ResourceConstants {

    //Attribute names constants
    // JDBC Resource
    public static final String JNDI_NAME = "jndi-name";

    public static final String POOL_NAME = "pool-name";

    // JMS Resource                                        
    public static final String RES_TYPE = "res-type";

    public static final String FACTORY_CLASS = "factory-class";

    public static final String ENABLED = "enabled";

    // External JNDI Resource
    public static final String JNDI_LOOKUP = "jndi-lookup-name";

    // JDBC Connection pool
    public static final String CONNECTION_POOL_NAME = "name";

    public static final String STEADY_POOL_SIZE = "steady-pool-size";

    public static final String MAX_POOL_SIZE = "max-pool-size";

    public static final String MAX_WAIT_TIME_IN_MILLIS = "max-wait-time-in-millis";

    public static final String POOL_SIZE_QUANTITY = "pool-resize-quantity";

    public static final String IDLE_TIME_OUT_IN_SECONDS = "idle-timeout-in-seconds";

    public static final String INIT_SQL = "init-sql";

    public static final String IS_CONNECTION_VALIDATION_REQUIRED = "is-connection-validation-required";

    public static final String CONNECTION_VALIDATION_METHOD = "connection-validation-method";

    public static final String CUSTOM_VALIDATION = "custom-validation";

    public static final String FAIL_ALL_CONNECTIONS = "fail-all-connections";

    public static final String VALIDATION_TABLE_NAME = "validation-table-name";

    public static final String DATASOURCE_CLASS = "datasource-classname";

    public static final String TRANS_ISOLATION_LEVEL = "transaction-isolation-level";

    public static final String IS_ISOLATION_LEVEL_GUARANTEED = "is-isolation-level-guaranteed";

    public static final String NON_TRANSACTIONAL_CONNECTIONS = "non-transactional-connections";

    public static final String ALLOW_NON_COMPONENT_CALLERS = "allow-non-component-callers";

    public static final String VALIDATE_ATMOST_ONCE_PERIOD_IN_SECONDS = "validate-atmost-once-period-in-seconds";

    public static final String CONNECTION_LEAK_TIMEOUT_IN_SECONDS = "connection-leak-timeout-in-seconds";

    public static final String CONNECTION_LEAK_RECLAIM = "connection-leak-reclaim";

    public static final String CONNECTION_CREATION_RETRY_ATTEMPTS = "connection-creation-retry-attempts";

    public static final String CONNECTION_CREATION_RETRY_INTERVAL_IN_SECONDS = "connection-creation-retry-interval-in-seconds";

    public static final String STATEMENT_TIMEOUT_IN_SECONDS = "statement-timeout-in-seconds";

    public static final String DRIVER_CLASSNAME = "driver-classname";

    public static final String LAZY_CONNECTION_ENLISTMENT = "lazy-connection-enlistment";

    public static final String LAZY_CONNECTION_ASSOCIATION = "lazy-connection-association";

    public static final String ASSOCIATE_WITH_THREAD = "associate-with-thread";

    public static final String ASSOCIATE_WITH_THREAD_CONNECTIONS_COUNT = "associate-with-thread-connections-count";

    public static final String MATCH_CONNECTIONS = "match-connections";

    public static final String MAX_CONNECTION_USAGE_COUNT = "max-connection-usage-count";

    public static final String PING = "ping";

    public static final String POOLING = "pooling";

    public static final String SQL_TRACE_LISTENERS = "sql-trace-listeners";

    public static final String STATEMENT_CACHE_SIZE = "statement-cache-size";

    public static final String VALIDATION_CLASSNAME = "validation-classname";

    public static final String WRAP_JDBC_OBJECTS = "wrap-jdbc-objects";
    
    public static final String CASCADE = "cascade";

    public static final String STATEMENT_LEAK_TIMEOUT_IN_SECONDS = "statement-leak-timeout-in-seconds";

    public static final String STATEMENT_LEAK_RECLAIM = "statement-leak-reclaim";

    //Mail resource
    public static final String MAIL_HOST = "host";

    public static final String MAIL_USER = "user";

    public static final String MAIL_FROM_ADDRESS = "from";

    public static final String MAIL_STORE_PROTO = "store-protocol";

    public static final String MAIL_STORE_PROTO_CLASS = "store-protocol-class";

    public static final String MAIL_TRANS_PROTO = "transport-protocol";

    public static final String MAIL_TRANS_PROTO_CLASS = "transport-protocol-class";

    public static final String MAIL_DEBUG = "debug";

    //Persistence Manager Factory resource
    public static final String JDBC_RESOURCE_JNDI_NAME = "jdbc-resource-jndi-name";

    //Admin Object resource
    public static final String RES_ADAPTER = "res-adapter";

    public static final String ADMIN_OBJECT_CLASS_NAME = "class-name";

    //Connector resource
    public static final String RESOURCE_TYPE = "resource-type";

    // ConnectorConnection Pool resource ...
    // child elements
    public static final String CONNECTOR_CONN_DESCRIPTION = "description";

    public static final String CONNECTOR_SECURITY_MAP = "security-map";

    public static final String CONNECTOR_PROPERTY = "property";

    //attributes....
    public static final String CONNECTOR_CONNECTION_POOL_NAME = "name";

    public static final String RESOURCE_ADAPTER_CONFIG_NAME = "resource-adapter-name";

    public static final String CONN_DEF_NAME = "connection-definition-name";

    public static final String CONN_STEADY_POOL_SIZE = "steady-pool-size";

    public static final String CONN_MAX_POOL_SIZE = "max-pool-size";

    public static final String CONN_POOL_RESIZE_QUANTITY = "pool-resize-quantity";

    public static final String CONN_IDLE_TIME_OUT = "idle-timeout-in-seconds";

    public static final String CONN_FAIL_ALL_CONNECTIONS = "fail-all-connections";

    public static final String CONN_TRANSACTION_SUPPORT = "transaction-support";

    //Security Map elements...
    public static final String SECURITY_MAP = "security-map";

    public static final String SECURITY_MAP_NAME = "name";

    public static final String SECURITY_MAP_PRINCIPAL = "principal";

    public static final String SECURITY_MAP_USER_GROUP = "user-group";

    public static final String SECURITY_MAP_BACKEND_PRINCIPAL = "backend-principal";

    //Resource -Adapter config attributes.
    public static final String RES_ADAPTER_CONFIG = "resource-adapter-config";

    public static final String THREAD_POOL_IDS = "thread-pool-ids";

    public static final String RES_ADAPTER_NAME = "resource-adapter-name";

    //Backend Principal elements....
    public static final String USER_NAME = "user-name";

    public static final String PASSWORD = "password";

    //work security map elements.
    public static final String WORK_SECURITY_MAP = "work-security-map";

    public static final String WORK_SECURITY_MAP_NAME = "name";

    public static final String WORK_SECURITY_MAP_RA_NAME = "resource-adapter-name";

    public static final String WORK_SECURITY_MAP_GROUP_MAP = "group-map";

    public static final String WORK_SECURITY_MAP_PRINCIPAL_MAP = "principal-map";

    //work security map - group-map elements ..
    public static final String WORK_SECURITY_MAP_EIS_GROUP = "eis-group";

    public static final String WORK_SECURITY_MAP_MAPPED_GROUP = "mapped-group";

    //work security map - eis-map elements ..
    public static final String WORK_SECURITY_MAP_EIS_PRINCIPAL = "eis-principal";

    public static final String WORK_SECURITY_MAP_MAPPED_PRINCIPAL = "mapped-principal";

    // concurrent resource objects
    public static final String CONTEXT_INFO = "context-info";
    public static final String CONTEXT_INFO_DEFAULT_VALUE = "Classloader,JNDI,Security,WorkArea";
    public static final String CONTEXT_INFO_ENABLED = "context-info-enabled";
    public static final String THREAD_PRIORITY = "thread-priority";
    public static final String LONG_RUNNING_TASKS = "long-runnings-tasks";
    public static final String HUNG_AFTER_SECONDS = "hung-after-seconds";
    public static final String CORE_POOL_SIZE = "core-pool-size";
    public static final String MAXIMUM_POOL_SIZE = "maximum-pool-size";
    public static final String KEEP_ALIVE_SECONDS = "keep-alive-seconds";
    public static final String THREAD_LIFETIME_SECONDS = "thread-lifetime-seconds";
    public static final String TASK_QUEUE_CAPACITY = "task-queue-capacity";

    public static final String SYSTEM_ALL_REQ = "system-all-req";
}

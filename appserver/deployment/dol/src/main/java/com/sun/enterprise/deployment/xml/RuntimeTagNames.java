/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.deployment.xml;

/** 
 * I hold the tag names of the runtime binding information of a J2EE 
 * application.
 * @author Danny Coward
 */

public interface RuntimeTagNames extends TagNames {
	
    public final static String S1AS_EJB_RUNTIME_TAG = "sun-ejb-jar";
    public final static String S1AS_APPCLIENT_RUNTIME_TAG = "sun-application-client";    
    public final static String S1AS_APPLICATION_RUNTIME_TAG = "sun-application";
    public final static String S1AS_WEB_RUNTIME_TAG = "sun-web-app";
    public final static String S1AS_CONNECTOR_RUNTIME_TAG = "sun-connector";

    public final static String WLS_EJB_RUNTIME_TAG = "weblogic-ejb-jar";
    public final static String WLS_APPCLIENT_RUNTIME_TAG = "weblogic-application-client";
    public final static String WLS_APPLICATION_RUNTIME_TAG = "weblogic-application";
    public final static String WLS_WEB_RUNTIME_TAG = "weblogic-web-app";
    public final static String WLS_CONNECTOR_RUNTIME_TAG = "weblogic-connector";

    public final static String GF_EJB_RUNTIME_TAG = "glassfish-ejb-jar";
    public final static String GF_APPCLIENT_RUNTIME_TAG = "glassfish-application-client";
    public final static String GF_APPLICATION_RUNTIME_TAG = "glassfish-application";
    public final static String GF_WEB_RUNTIME_TAG = "glassfish-web-app";

    String AS_CONTEXT = "as-context";
    String AUTH_METHOD = "auth-method";
    String CALLER_PROPAGATION = "caller-propagation";
    String CONFIDENTIALITY = "confidentiality";
    public static final String DURABLE_SUBSCRIPTION = "jms-durable-subscription-name";
    String ESTABLISH_TRUST_IN_CLIENT = "establish-trust-in-client";
    String ESTABLISH_TRUST_IN_TARGET = "establish-trust-in-target";
    String INTEGRITY = "integrity";
    String IOR_CONFIG = "ior-security-config";
    public static final String MDB_CONNECTION_FACTORY = "mdb-connection-factory";
    String MESSAGE_DESTINATION = "message-destination";
    String MESSAGE_DESTINATION_NAME = "message-destination-name";
    String REALM = "realm";
    String LOGIN_CONFIG = "login-config";
    String REQUIRED = "required";
    String RESOURCE_ADAPTER_MID = "resource-adapter-mid";
    String SAS_CONTEXT = "sas-context";
    String TRANSPORT_CONFIG = "transport-config";
    String MDB_RESOURCE_ADAPTER = "mdb-resource-adapter";
    String ACTIVATION_CONFIG = "activation-config";
    String ACTIVATION_CONFIG_PROPERTY = "activation-config-property";
    String ACTIVATION_CONFIG_PROPERTY_NAME = "activation-config-property-name";
    String ACTIVATION_CONFIG_PROPERTY_VALUE = "activation-config-property-value";
    
    public static final String APPLICATION_CLIENT = "app-client";    
    public static final String CMP = "cmp";
    public static final String CMPRESOURCE = "cmpresource";
    public static final String DEFAULT_RESOURCE_PRINCIPAL = "default-resource-principal";
    public static final String DISPLAY_NAME = "display-name";
    public static final String EJB = "ejb";
    public static final String EJB_NAME = "ejb-name";
    public static final String EJB20_CMP = "ejb20-cmp";
    public static final String EJBS = "enterprise-beans";
    public static final String FIELD = "field";

    public static final String GROUP = "group";
    public static final String GROUPS = "groups";
    public static final String JOIN_OBJECT = "join-object";
    public static final String JNDI_NAME = "jndi-name";
    public static final String LOCAL_PART = "localpart";
    public static final String MAIL_CONFIG = "mail-configuration";
    public static final String MAIL_FROM = "mail-from";
    public static final String MAIL_HOST = "mail-host";
    public static final String METHOD = "method";
    public static final String NAME = "name";
    public static final String NAMESPACE_URI = "namespace-uri";
    public static final String OPERATION = "operation";

    public static final String PASSWORD = "password";
    public static final String PRINCIPALS = "principals";
    public static final String PRINCIPAL = "principal";
    public static final String REMOTE_ENTITY = "remote-entity";
    public static final String ROLE = "role";    
    public static final String ROLE_MAPPING = "rolemapping";
    public static final String ROLE_ENTRY = "role";
    public static final String SERVER_NAME = "server-name";
    
    public static final String SERVLET = "servlet";
    public static final String SERVLET_NAME = "servlet-name";
    public static final String SOURCE = "source";
    public static final String SINK = "sink";
    public static final String SQL = "sql";
    public static final String SQL_STATEMENT = "sql-statement";
    public static final String TABLE_CREATE = "table-create-sql";
    public static final String TABLE_REMOVE = "table-remove-sql";


    public static final String UNIQUE_ID = "unique-id";
    public static final String WEB = "web";
    public static final String WEB_SERVICE_ENDPOINT = "web-service-endpoint";


    public static final String EJB_IMPL = "ejb-impl";
    public static final String REMOTE_IMPL = "remote-impl";
    public static final String LOCAL_IMPL = "local-impl";
    public static final String REMOTE_HOME_IMPL = "remote-home-impl";
    public static final String LOCAL_HOME_IMPL = "local-home-impl";
    public static final String STATE_IMPL = "state-impl";
    public static final String GEN_CLASSES = "gen-classes";
    
    // acceptable values
    public static final String TRUE = "true";
    public static final String FALSE = "false";
    
    // SECURITY related
    public static final String SECURITY_ROLE_MAPPING = "security-role-mapping";
    public static final String SECURITY_ROLE_ASSIGNMENT = "security-role-assignment";
    public static final String ROLE_NAME = "role-name";
    public static final String PRINCIPAL_NAME = "principal-name";
    public static final String GROUP_NAME = "group-name";
    public static final String EXTERNALLY_DEFINED = "externally-defined";
    
    // common
    public static final String EJB_REF = "ejb-ref";
    public static final String RESOURCE_REF = "resource-ref";
    public static final String RESOURCE_ENV_REF = "resource-env-ref";
    
    // S1AS specific
    public static final String PASS_BY_REFERENCE = "pass-by-reference";
    public static final String JMS_MAX_MESSAGES_LOAD = "jms-max-messages-load";
    public static final String IS_READ_ONLY_BEAN = "is-read-only-bean";
    public static final String REFRESH_PERIOD_IN_SECONDS = "refresh-period-in-seconds";
    public static final String COMMIT_OPTION = "commit-option";    
    public static final String CMT_TIMEOUT_IN_SECONDS = "cmt-timeout-in-seconds";    
    public static final String USE_THREAD_POOL_ID = "use-thread-pool-id";    
    public static final String AVAILABILITY_ENABLED = "availability-enabled";
    public static final String DISABLE_NONPORTABLE_JNDI_NAMES = "disable-nonportable-jndi-names";
    public static final String PER_REQUEST_LOAD_BALANCING = "per-request-load-balancing";

    // CMP related
    public static final String CMP_RESOURCE = "cmp-resource";    
    public static final String MAPPING_PROPERTIES = "mapping-properties";
    public static final String IS_ONE_ONE_CMP = "is-one-one-cmp";
    public static final String ONE_ONE_FINDERS = "one-one-finders";
    public static final String METHOD_NAME = "method-name";
    public static final String QUERY_PARAMS = "query-params";
    public static final String QUERY_FILTER = "query-filter";
    public static final String QUERY_VARIABLES = "query-variables";
    public static final String QUERY_ORDERING = "query-ordering";
    public static final String FINDER = "finder";
    public static final String CREATE_TABLES_AT_DEPLOY = "create-tables-at-deploy";
    public static final String DROP_TABLES_AT_UNDEPLOY = "drop-tables-at-undeploy";
    public static final String DATABASE_VENDOR_NAME = "database-vendor-name";
    public static final String SCHEMA_GENERATOR_PROPERTIES = "schema-generator-properties";
    
    
    // PM-DESCRIPTORS related
    public static final String PM_DESCRIPTORS = "pm-descriptors";
    public static final String PM_DESCRIPTOR = "pm-descriptor";
    public static final String PM_IDENTIFIER = "pm-identifier";
    public static final String PM_VERSION = "pm-version";
    public static final String PM_CONFIG = "pm-config";
    public static final String PM_CLASS_GENERATOR = "pm-class-generator";
    public static final String PM_MAPPING_FACTORY = "pm-mapping-factory";
    public static final String PM_INUSE = "pm-inuse";
    
    // BEAN-POOL related
    public static final String BEAN_POOL = "bean-pool";
    public static final String STEADY_POOL_SIZE = "steady-pool-size";
    public static final String POOL_RESIZE_QUANTITY = "resize-quantity";
    public static final String MAX_POOL_SIZE = "max-pool-size";
    public static final String POOL_IDLE_TIMEOUT_IN_SECONDS = "pool-idle-timeout-in-seconds";
    public static final String MAX_WAIT_TIME_IN_MILLIS = "max-wait-time-in-millis";
    
    // BEAN-CACHE related
    public static final String BEAN_CACHE = "bean-cache";
    public static final String MAX_CACHE_SIZE = "max-cache-size";
    public static final String RESIZE_QUANTITY = "resize-quantity";
    public static final String IS_CACHE_OVERFLOW_ALLOWED = "is-cache-overflow-allowed";
    public static final String CACHE_IDLE_TIMEOUT_IN_SECONDS = "cache-idle-timeout-in-seconds";
    public static final String REMOVAL_TIMEOUT_IN_SECONDS = "removal-timeout-in-seconds";
    public static final String VICTIM_SELECTION_POLICY = "victim-selection-policy";
    
    // thread-pool related
    public static final String THREAD_CORE_POOL_SIZE = "thread-core-pool-size";
    public static final String THREAD_MAX_POOL_SIZE  = "thread-max-pool-size";
    public static final String THREAD_KEEP_ALIVE_SECONDS = "thread-keep-alive-seconds";
    public static final String THREAD_QUEUE_CAPACITY = "thread-queue-capacity";
    public static final String ALLOW_CORE_THREAD_TIMEOUT = "allow-core-thread-timeout";
    public static final String PRESTART_ALL_CORE_THREADS = "prestart-all-core-threads";
    
    // flush-at-end-of-method
    public static final String FLUSH_AT_END_OF_METHOD =
        "flush-at-end-of-method";
    // checkpointed-methods, support backward compatibility with 7.1
    public static final String CHECKPOINTED_METHODS =
        "checkpointed-methods";
    // checkpoint-at-end-of-method, equivalent element of 
    // checkpointed-methods in 8.1 and later releases
    public static final String CHECKPOINT_AT_END_OF_METHOD =
        "checkpoint-at-end-of-method";
    // prefetch-disabled 
    public static final String PREFETCH_DISABLED =
        "prefetch-disabled";

    public static final String QUERY_METHOD = "query-method";

    // Connector related
    public static final String RESOURCE_ADAPTER = "resource-adapter";
    public static final String ROLE_MAP = "role-map";
    public static final String IDLE_TIMEOUT_IN_SECONDS = "idle-timeout-in-seconds"; 
    public static final String PROPERTY = "property";
    public static final String MAP_ELEMENT = "map-element";
    public static final String MAP_ID = "map-id";
    public static final String BACKEND_PRINCIPAL = "backend-principal";
    public static final String USER_NAME = "user-name";
    public static final String CREDENTIAL = "credential";
    
    // application related
    public static final String WEB_URI = "web-uri";
    public static final String CONTEXT_ROOT = "context-root"; // also used in java web start support
    public final static String ARCHIVE_NAME = "archive-name";
    public final static String COMPATIBILITY = "compatibility";
    public final static String KEEP_STATE = "keep-state";
    public static final String VERSION_IDENTIFIER = "version-identifier";
    public final static String APPLICATION_PARAM = "application-param";
    public static final String PARAM_NAME = "param-name";
    public static final String PARAM_VALUE = "param-value";
    public final static String MODULE = "module";
    public final static String TYPE = "type";
    public final static String PATH = "path";

    // Web
    public static final String CACHE_MAPPING = "cache-mapping";
    public static final String CACHE_HELPER = "cache-helper";
    public static final String CACHE_HELPER_REF = "cache-helper-ref";
    public static final String CLASS_NAME = "class-name";
    public static final String COOKIE_PROPERTIES = "cookie-properties";
    public static final String CONSTRAINT_FIELD = "constraint-field";
    public static final String CONSTRAINT_FIELD_VALUE = "constraint-field-value";
    public static final String LOCALE_CHARSET_INFO = "locale-charset-info";
    public static final String DEFAULT_LOCALE = "default-locale";
    public static final String DEFAULT_HELPER = "default-helper";
    public static final String LOCALE = "locale";
    public static final String MAX_ENTRIES = "max-entries";
    public static final String TIMEOUT_IN_SECONDS = "timeout-in-seconds";
    public static final String ENABLED = "enabled";
    public static final String AGENT = "agent";
    public static final String CHARSET = "charset";
    public static final String LOCALE_CHARSET_MAP = "locale-charset-map";
    public static final String PARAMETER_ENCODING = "parameter-encoding";
    public static final String FORM_HINT_FIELD = "form-hint-field";
    public static final String DEFAULT_CHARSET = "default-charset";
    public static final String STORE_PROPERTIES = "store-properties";
    public static final String MANAGER_PROPERTIES = "manager-properties";
    public static final String REFRESH_FIELD = "refresh-field";
    public static final String SESSION_MANAGER = "session-manager";
    public static final String SESSION_PROPERTIES = "session-properties";
    public static final String SESSION_CONFIG = "session-config";
    public static final String TIMEOUT = "timeout";
    public static final String PERSISTENCE_TYPE = "persistence-type";
    public static final String JSP_CONFIG = "jsp-config";
    public static final String CLASS_LOADER = "class-loader";
    public static final String EXTRA_CLASS_PATH = "extra-class-path";
    public static final String DELEGATE = "delegate";
    public static final String DYNAMIC_RELOAD_INTERVAL =
        "dynamic-reload-interval";
    public static final String CACHE = "cache";
    public static final String KEY_FIELD = "key-field";
    public static final String URL_PATTERN = "url-pattern";
    public static final String HTTP_METHOD = "http-method";
    public static final String DISPATCHER = "dispatcher";
    public static final String SCOPE = "scope";
    public static final String CACHE_ON_MATCH = "cache-on-match";
    public static final String CACHE_ON_MATCH_FAILURE = "cache-on-match-failure";
    public static final String MATCH_EXPR = "match-expr";
    public static final String VALUE = "value";   
    public static final String IDEMPOTENT_URL_PATTERN = "idempotent-url-pattern";
    public static final String ERROR_URL = "error-url";
    public static final String HTTPSERVLET_SECURITY_PROVIDER = "httpservlet-security-provider";
    public static final String NUM_OF_RETRIES = "num-of-retries";   

    public static final String JAVA_METHOD = "java-method";
    public final static String METHOD_PARAMS = "method-params";
    public final static String METHOD_PARAM = "method-param";

    public final static String VALVE = "valve";
    
    // Java Web Start-support related
    public final static String JAVA_WEB_START_ACCESS = "java-web-start-access";
    public final static String ELIGIBLE = "eligible";
    public final static String VENDOR = "vendor";
    public final static String JNLP_DOC = "jnlp-doc";
    // also uses CONTEXT_ROOT defined above in the application-related section


    // Weblogic specific
    public static final String RESOURCE_DESCRIPTION = "resource-description";
    public static final String RESOURCE_ENV_DESCRIPTION = "resource-env-description";
    public static final String EJB_REFERENCE_DESCRIPTION = "ejb-reference-description";
}

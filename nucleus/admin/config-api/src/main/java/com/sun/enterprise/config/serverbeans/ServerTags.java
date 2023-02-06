/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2013 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2017-2019] [Payara Foundation and/or its affiliates]

package com.sun.enterprise.config.serverbeans;

public class ServerTags  {
    
    /** Tags for Element domain */
    public static final String DOMAIN = "domain";
    public static final String APPLICATION_ROOT = "application-root";
    public static final String LOG_ROOT = "log-root";
    public static final String LOCALE = "locale";
    /** Tags for Element applications */
    public static final String APPLICATIONS = "applications";
    public static final String APPLICATION = "application";
    /** Tags for Element resources */
    public static final String RESOURCES = "resources";
    /** Tags for Element configs */
    public static final String CONFIGS = "configs";
    /** Tags for Element servers */
    public static final String SERVERS = "servers";
    /** Tags for Element clusters */
    public static final String CLUSTERS = "clusters";
    /** Tags for Element node-agents */
    public static final String NODE_AGENTS = "node-agents";
    /**  Tags for Element lb-configs */
    public static final String LB_CONFIGS = "lb-configs";
    /** Tags for Element load-balancers */
    public static final String LOAD_BALANCERS = "load-balancers";
    /** Tags for Element system-property */
    public static final String SYSTEM_PROPERTY = "system-property";
    public static final String DESCRIPTION = "description";
    public static final String NAME = "name";
    public static final String VALUE = "value";
    /** Tags for Element element-property */
    public static final String ELEMENT_PROPERTY = "element-property";
    //public static final String DESCRIPTION = "description";
    //public static final String NAME = "name";
    //public static final String VALUE = "value";
    /** Tags for Element load-balancer */
    public static final String LOAD_BALANCER = "load-balancer";
    //public static final String NAME = "name"; */
    public static final String LB_CONFIG_NAME = "lb-config-name";
    public static final String AUTO_APPLY_ENABLED = "auto-apply-enabled";
    /** Tags for Element lb-config */
    public static final String LB_CONFIG = "lb-config";
    //public static final String NAME = "name";
    public static final String RESPONSE_TIMEOUT_IN_SECONDS = "response-timeout-in-seconds";
    public static final String HTTPS_ROUTING = "https-routing";
    public static final String RELOAD_POLL_INTERVAL_IN_SECONDS = "reload-poll-interval-in-seconds";
    public static final String MONITORING_ENABLED = "monitoring-enabled";
    public static final String ROUTE_COOKIE_ENABLED = "route-cookie-enabled";
    /** Tags for Element cluster-ref */
    public static final String CLUSTER_REF = "cluster-ref";
    public static final String REF = "ref";
    public static final String LB_POLICY = "lb-policy";
    public static final String LB_POLICY_MODULE = "lb-policy-module";
    /** Tags for Element server-ref */
    public static final String SERVER_REF = "server-ref";
    //public static final String REF = "ref";
    public static final String DISABLE_TIMEOUT_IN_MINUTES = "disable-timeout-in-minutes";
    public static final String LB_ENABLED = "lb-enabled";
    public static final String ENABLED = "enabled";
    /** Tags for Element health-checker */
    public static final String HEALTH_CHECKER = "health-checker";
    public static final String URL = "url";
    public static final String INTERVAL_IN_SECONDS = "interval-in-seconds";
    public static final String TIMEOUT_IN_SECONDS = "timeout-in-seconds";
    /** Tags for Element node-agent */
    public static final String NODE_AGENT = "node-agent";
    //public static final String NAME = "name";
    public static final String SYSTEM_JMX_CONNECTOR_NAME = "system-jmx-connector-name";
    public static final String START_SERVERS_IN_STARTUP = "start-servers-in-startup";
    /** Tags for Element jmx-connector */
    public static final String JMX_CONNECTOR = "jmx-connector";
    //public static final String NAME = "name";
    //public static final String ENABLED = "enabled";
    public static final String PROTOCOL = "protocol";
    public static final String ADDRESS = "address";
    public static final String PORT = "port";
    public static final String ACCEPT_ALL = "accept-all";
    public static final String AUTH_REALM_NAME = "auth-realm-name";
    public static final String SECURITY_ENABLED = "security-enabled";
    /** Tags for Element auth-realm */
    public static final String AUTH_REALM = "auth-realm";
    //public static final String NAME = "name";
    public static final String CLASSNAME = "classname";
    /** Tags for Element log-service */
    public static final String LOG_SERVICE = "log-service";
    public static final String FILE = "file";
    public static final String USE_SYSTEM_LOGGING = "use-system-logging";
    public static final String LOG_HANDLER = "log-handler";
    public static final String LOG_FILTER = "log-filter";
    public static final String LOG_TO_CONSOLE = "log-to-console";
    public static final String LOG_TO_FILE = "log-to-file";
    public static final String PAYARA_NOTIFICATION_LOG_TO_CONSOLE = "payara-notification-log-to-file";
    public static final String LOG_ROTATION_LIMIT_IN_BYTES = "log-rotation-limit-in-bytes";
    public static final String PAYARA_NOTIFICATIONLOG_ROTATION_LIMIT_IN_BYTES = "payara-notification-log-rotation-limit-in-bytes";
    public static final String LOG_ROTATION_TIMELIMIT_IN_MINUTES = "log-rotation-timelimit-in-minutes";
    public static final String PAYARA_NOTIFICATIONLOG_ROTATION_TIMELIMIT_IN_MINUTES = "payara-notification-log-rotation-timelimit-in-minutes";
    public static final String ALARMS = "alarms";
    public static final String RETAIN_ERROR_STATISTICS_FOR_HOURS = "retain-error-statistics-for-hours";
    public static final String LOG_STANDARD_STREAMS = "log-standard-streams";
    /** Tags for Element module-log-levels */
    public static final String MODULE_LOG_LEVELS = "module-log-levels";
    public static final String ROOT = "root";
    public static final String SERVER = "server";
    public static final String EJB_CONTAINER = "ejb-container";
    public static final String CMP_CONTAINER = "cmp-container";
    public static final String MDB_CONTAINER = "mdb-container";
    public static final String WEB_CONTAINER = "web-container";
    public static final String CLASSLOADER = "classloader";
    public static final String CONFIGURATION = "configuration";
    public static final String NAMING = "naming";
    public static final String SECURITY = "security";
    public static final String JTS = "jts";
    public static final String JTA = "jta";
    public static final String ADMIN = "admin";
    public static final String DEPLOYMENT = "deployment";
    public static final String VERIFIER = "verifier";
    public static final String JAXR = "jaxr";
    public static final String JAXRPC = "jaxrpc";
    public static final String SAAJ = "saaj";
    public static final String CORBA = "corba";
    public static final String JAVAMAIL = "javamail";
    public static final String JMS = "jms";
    public static final String CONNECTOR = "connector";
    public static final String JDO = "jdo";
    public static final String CMP = "cmp";
    public static final String UTIL = "util";
    public static final String RESOURCE_ADAPTER = "resource-adapter";
    public static final String SYNCHRONIZATION = "synchronization";
    //public static final String NODE_AGENT = "node-agent";
    public static final String NODES ="nodes";
    
    public static final String SELF_MANAGEMENT = "self-management";
    public static final String GROUP_MANAGEMENT_SERVICE = "group-management-service";
    public static final String MANAGEMENT_EVENT = "management-event";
    /** Tags for Element ssl */
    public static final String SSL = "ssl";
    public static final String CERT_NICKNAME = "cert-nickname";
    public static final String SSL2_ENABLED = "ssl2-enabled";
    public static final String SSL2_CIPHERS = "ssl2-ciphers";
    public static final String SSL3_ENABLED = "ssl3-enabled";
    public static final String SSL3_TLS_CIPHERS = "ssl3-tls-ciphers";
    public static final String TLS_ENABLED = "tls-enabled";
    public static final String TLS_ROLLBACK_ENABLED = "tls-rollback-enabled";
    public static final String CLIENT_AUTH_ENABLED = "client-auth-enabled";
    /** Tags for Element cluster */
    public static final String CLUSTER = "cluster";
    //public static final String NAME = "name";
    public static final String CONFIG_REF = "config-ref";
    public static final String HEARTBEAT_ENABLED = "heartbeat-enabled";
    public static final String HEARTBEAT_PORT = "heartbeat-port";
    public static final String HEARTBEAT_ADDRESS = "heartbeat-address";
    /** Tags for Element resource-ref */
    public static final String RESOURCE_REF = "resource-ref";
    //public static final String ENABLED = "enabled";
    //public static final String REF = "ref";
    /** Tags for Element application-ref */
    public static final String APPLICATION_REF = "application-ref";
    //public static final String ENABLED = "enabled";
    public static final String VIRTUAL_SERVERS = "virtual-servers";
    public static final String CDI_DEV_MODE_ENABLED_PROP = "cdiDevModeEnabled";
    
    //public static final String LB_ENABLED = "lb-enabled";
    //public static final String DISABLE_TIMEOUT_IN_MINUTES = "disable-timeout-in-minutes";
    //public static final String REF = "ref";
    /** Tags for Element server
    //public static final String SERVER = "server";
    //public static final String NAME = "name";
    //public static final String CONFIG_REF = "config-ref";*/
    public static final String NODE_AGENT_REF = "node-agent-ref";
    public static final String LB_WEIGHT = "lb-weight";
    /** Tags for Element config */
    public static final String CONFIG = "config";
    //public static final String NAME = "name";
    public static final String DYNAMIC_RECONFIGURATION_ENABLED = "dynamic-reconfiguration-enabled";
    /** Tags for Element http-service */
    public static final String HTTP_SERVICE = "http-service";
    /** Tags for Element iiop-service */
    public static final String IIOP_SERVICE = "iiop-service";
    public static final String CLIENT_AUTHENTICATION_REQUIRED = "client-authentication-required";
    /** Tags for Element admin-service */
    public static final String ADMIN_SERVICE = "admin-service";
    public static final String TYPE = "type";
    //public static final String SYSTEM_JMX_CONNECTOR_NAME = "system-jmx-connector-name";
    /** Tags for Element connector-service */
    public static final String CONNECTOR_SERVICE = "connector-service";
    public static final String SHUTDOWN_TIMEOUT_IN_SECONDS = "shutdown-timeout-in-seconds";
/** Tags for Element web-container */
//public static final String WEB_CONTAINER = "web-container";
    /** Tags for Element ejb-container */
    //public static final String EJB_CONTAINER = "ejb-container";
    public static final String STEADY_POOL_SIZE = "steady-pool-size";
    public static final String POOL_RESIZE_QUANTITY = "pool-resize-quantity";
    public static final String MAX_POOL_SIZE = "max-pool-size";
    public static final String CACHE_RESIZE_QUANTITY = "cache-resize-quantity";
    public static final String MAX_CACHE_SIZE = "max-cache-size";
    public static final String POOL_IDLE_TIMEOUT_IN_SECONDS = "pool-idle-timeout-in-seconds";
    public static final String CACHE_IDLE_TIMEOUT_IN_SECONDS = "cache-idle-timeout-in-seconds";
    public static final String REMOVAL_TIMEOUT_IN_SECONDS = "removal-timeout-in-seconds";
    public static final String VICTIM_SELECTION_POLICY = "victim-selection-policy";
    public static final String COMMIT_OPTION = "commit-option";
    public static final String SESSION_STORE = "session-store";
    /** Tags for Element mdb-container */
    //public static final String MDB_CONTAINER = "mdb-container";
    //public static final String STEADY_POOL_SIZE = "steady-pool-size";
    //public static final String POOL_RESIZE_QUANTITY = "pool-resize-quantity";
    //public static final String MAX_POOL_SIZE = "max-pool-size";
    public static final String IDLE_TIMEOUT_IN_SECONDS = "idle-timeout-in-seconds";
    /** Tags for Element jms-service */
    public static final String JMS_SERVICE = "jms-service";
    public static final String INIT_TIMEOUT_IN_SECONDS = "init-timeout-in-seconds";
    public static final String MASTER_BROKER = "master-broker";
    //public static final String TYPE = "type";
    public static final String START_ARGS = "start-args";
    public static final String DEFAULT_JMS_HOST = "default-jms-host";
    public static final String RECONNECT_INTERVAL_IN_SECONDS = "reconnect-interval-in-seconds";
    public static final String RECONNECT_ATTEMPTS = "reconnect-attempts";
    public static final String RECONNECT_ENABLED = "reconnect-enabled";
    public static final String ADDRESSLIST_BEHAVIOR = "addresslist-behavior";
    public static final String ADDRESSLIST_ITERATIONS = "addresslist-iterations";
    public static final String MQ_SCHEME = "mq-scheme";
    public static final String MQ_SERVICE = "mq-service";
    /** Tags for Element security-service */
    public static final String SECURITY_SERVICE = "security-service";
    public static final String DEFAULT_REALM = "default-realm";
    public static final String DEFAULT_PRINCIPAL = "default-principal";
    public static final String DEFAULT_PRINCIPAL_PASSWORD = "default-principal-password";
    public static final String ANONYMOUS_ROLE = "anonymous-role";
    public static final String AUDIT_ENABLED = "audit-enabled";
    public static final String JACC = "jacc";
    public static final String AUDIT_MODULES = "audit-modules";
    public static final String ACTIVATE_DEFAULT_PRINCIPAL_TO_ROLE_MAPPING = "activate-default-principal-to-role-mapping";
    public static final String MAPPED_PRINCIPAL_CLASS = "mapped-principal-class";
    /** Tags for Element transaction-service */
    public static final String TRANSACTION_SERVICE = "transaction-service";
    public static final String AUTOMATIC_RECOVERY = "automatic-recovery";
    //public static final String TIMEOUT_IN_SECONDS = "timeout-in-seconds";
    public static final String TX_LOG_DIR = "tx-log-dir";
    public static final String HEURISTIC_DECISION = "heuristic-decision";
    public static final String RETRY_TIMEOUT_IN_SECONDS = "retry-timeout-in-seconds";
    public static final String KEYPOINT_INTERVAL = "keypoint-interval";
    /** Tags for Element monitoring-service */
    public static final String MONITORING_SERVICE = "monitoring-service";
    /** Tags for Element diagnostic-service */
    public static final String DIAGNOSTIC_SERVICE = "diagnostic-service";
    public static final String COMPUTE_CHECKSUM = "compute-checksum";
    public static final String VERIFY_CONFIG = "verify-config";
    public static final String CAPTURE_INSTALL_LOG = "capture-install-log";
    public static final String CAPTURE_SYSTEM_INFO = "capture-system-info";
    public static final String CAPTURE_HADB_INFO = "capture-hadb-info";
    public static final String CAPTURE_APP_DD = "capture-app-dd";
    public static final String MIN_LOG_LEVEL = "min-log-level";
    public static final String MAX_LOG_ENTRIES = "max-log-entries";
    /** Tags for Element java-config */
    public static final String JAVA_CONFIG = "java-config";
    public static final String JVM_OPTIONS = "jvm-options";
    public static final String JAVA_HOME = "java-home";
    public static final String DEBUG_ENABLED = "debug-enabled";
    public static final String DEBUG_OPTIONS = "debug-options";
    public static final String RMIC_OPTIONS = "rmic-options";
    public static final String JAVAC_OPTIONS = "javac-options";
    public static final String CLASSPATH_PREFIX = "classpath-prefix";
    public static final String CLASSPATH_SUFFIX = "classpath-suffix";
    public static final String SERVER_CLASSPATH = "server-classpath";
    public static final String SYSTEM_CLASSPATH = "system-classpath";
    public static final String NATIVE_LIBRARY_PATH_PREFIX = "native-library-path-prefix";
    public static final String NATIVE_LIBRARY_PATH_SUFFIX = "native-library-path-suffix";
    public static final String BYTECODE_PREPROCESSORS = "bytecode-preprocessors";
    public static final String ENV_CLASSPATH_IGNORED = "env-classpath-ignored";
    /** Tags for Element availability-service */
    public static final String AVAILABILITY_SERVICE = "availability-service";
    public static final String AVAILABILITY_ENABLED = "availability-enabled";
    public static final String HA_AGENT_HOSTS = "ha-agent-hosts";
    public static final String HA_AGENT_PORT = "ha-agent-port";
    public static final String HA_AGENT_PASSWORD = "ha-agent-password";
    public static final String HA_STORE_NAME = "ha-store-name";
    public static final String AUTO_MANAGE_HA_STORE = "auto-manage-ha-store";
    public static final String STORE_POOL_NAME = "store-pool-name";
    public static final String HA_STORE_HEALTHCHECK_ENABLED = "ha-store-healthcheck-enabled";
    public static final String HA_STORE_HEALTHCHECK_INTERVAL_IN_SECONDS = "ha-store-healthcheck-interval-in-seconds";
    /** Tags for Element thread-pools */
    public static final String THREAD_POOLS = "thread-pools";
    /** Tags for Element alert-service */
    public static final String ALERT_SERVICE = "alert-service";
    /** Tags for Element group-management-service */
    //public static final String GROUP_MANAGEMENT_SERVICE = "group-management-service";
    public static final String FD_PROTOCOL_MAX_TRIES = "fd-protocol-max-tries";
    public static final String FD_PROTOCOL_TIMEOUT_IN_MILLIS = "fd-protocol-timeout-in-millis";
    public static final String MERGE_PROTOCOL_MAX_INTERVAL_IN_MILLIS = "merge-protocol-max-interval-in-millis";
    public static final String MERGE_PROTOCOL_MIN_INTERVAL_IN_MILLIS = "merge-protocol-min-interval-in-millis";
    public static final String PING_PROTOCOL_TIMEOUT_IN_MILLIS = "ping-protocol-timeout-in-millis";
    public static final String VS_PROTOCOL_TIMEOUT_IN_MILLIS = "vs-protocol-timeout-in-millis";
    /** Tags for Element management-rules */
    public static final String MANAGEMENT_RULES = "management-rules";
    //public static final String ENABLED = "enabled";
    /** Tags for Element management-rule */
    public static final String MANAGEMENT_RULE = "management-rule";
    //public static final String DESCRIPTION = "description";
    //public static final String NAME = "name";
    //public static final String ENABLED = "enabled";
    /** Tags for Element event */
    public static final String EVENT = "event";
    //public static final String DESCRIPTION = "description";
    //public static final String TYPE = "type";
    public static final String RECORD_EVENT = "record-event";
    public static final String LEVEL = "level";
    /** Tags for Element action */
    public static final String ACTION = "action";
    public static final String ACTION_MBEAN_NAME = "action-mbean-name";
    /** Tags for Element alert-subscription */
    public static final String ALERT_SUBSCRIPTION = "alert-subscription";
    //public static final String NAME = "name";
    /** Tags for Element listener-config */
    public static final String LISTENER_CONFIG = "listener-config";
    public static final String LISTENER_CLASS_NAME = "listener-class-name";
    public static final String SUBSCRIBE_LISTENER_WITH = "subscribe-listener-with";
    /** Tags for Element filter-config */
    public static final String FILTER_CONFIG = "filter-config";
    public static final String FILTER_CLASS_NAME = "filter-class-name";
    /** Tags for Element thread-pool */
    public static final String THREAD_POOL = "thread-pool";
    public static final String THREAD_POOL_ID = "thread-pool-id";
    public static final String MIN_THREAD_POOL_SIZE = "min-thread-pool-size";
    public static final String MAX_THREAD_POOL_SIZE = "max-thread-pool-size";
    public static final String IDLE_THREAD_TIMEOUT_IN_SECONDS = "idle-thread-timeout-in-seconds";
    public static final String NUM_WORK_QUEUES = "num-work-queues";
    /** Tags for Element web-container-availability */
    public static final String WEB_CONTAINER_AVAILABILITY = "web-container-availability";
    //public static final String AVAILABILITY_ENABLED = "availability-enabled";
    public static final String PERSISTENCE_TYPE = "persistence-type";
    public static final String PERSISTENCE_FREQUENCY = "persistence-frequency";
    public static final String PERSISTENCE_SCOPE = "persistence-scope";
    public static final String PERSISTENCE_STORE_HEALTH_CHECK_ENABLED = "persistence-store-health-check-enabled";
    public static final String SSO_FAILOVER_ENABLED = "sso-failover-enabled";
    public static final String HTTP_SESSION_STORE_POOL_NAME = "http-session-store-pool-name";
    /** Tags for Element ejb-container-availability */
    public static final String EJB_CONTAINER_AVAILABILITY = "ejb-container-availability";
    //public static final String AVAILABILITY_ENABLED = "availability-enabled";
    public static final String SFSB_HA_PERSISTENCE_TYPE = "sfsb-ha-persistence-type";
    public static final String SFSB_PERSISTENCE_TYPE = "sfsb-persistence-type";
    public static final String SFSB_CHECKPOINT_ENABLED = "sfsb-checkpoint-enabled";
    public static final String SFSB_QUICK_CHECKPOINT_ENABLED = "sfsb-quick-checkpoint-enabled";
    public static final String SFSB_STORE_POOL_NAME = "sfsb-store-pool-name";
    /** Tags for Element jms-availability */
    public static final String JMS_AVAILABILITY = "jms-availability";
    //public static final String AVAILABILITY_ENABLED = "availability-enabled";
    public static final String MQ_STORE_POOL_NAME = "mq-store-pool-name";
    /** Tags for Element profiler */
    public static final String PROFILER = "profiler";
    //public static final String JVM_OPTIONS = "jvm-options";
    //public static final String NAME = "name";
    public static final String CLASSPATH = "classpath";
    public static final String NATIVE_LIBRARY_PATH = "native-library-path";
    //public static final String ENABLED = "enabled";
    /** Tags for Element module-monitoring-levels */
    public static final String MODULE_MONITORING_LEVELS = "module-monitoring-levels";
    //public static final String THREAD_POOL = "thread-pool";
    public static final String ORB = "orb";
    //public static final String EJB_CONTAINER = "ejb-container";
    //public static final String WEB_CONTAINER = "web-container";
    //public static final String TRANSACTION_SERVICE = "transaction-service";
    //public static final String HTTP_SERVICE = "http-service";
    public static final String JDBC_CONNECTION_POOL = "jdbc-connection-pool";
    public static final String CONNECTOR_CONNECTION_POOL = "connector-connection-pool";
    //public static final String CONNECTOR_SERVICE = "connector-service";
    //public static final String JMS_SERVICE = "jms-service";
    public static final String JVM = "jvm";
    /** Tags for Element jacc-provider */
    public static final String JACC_PROVIDER = "jacc-provider";
    //public static final String NAME = "name";
    public static final String POLICY_PROVIDER = "policy-provider";
    public static final String POLICY_CONFIGURATION_FACTORY_PROVIDER = "policy-configuration-factory-provider";
    /** Tags for Element audit-module */
    public static final String AUDIT_MODULE = "audit-module";
    //public static final String NAME = "name";
    //public static final String CLASSNAME = "classname";
    /** Tags for Element message-security-config */
    public static final String MESSAGE_SECURITY_CONFIG = "message-security-config";
    public static final String AUTH_LAYER = "auth-layer";
    public static final String DEFAULT_PROVIDER = "default-provider";
    public static final String DEFAULT_CLIENT_PROVIDER = "default-client-provider";
    /** Tags for Element provider-config */
    public static final String PROVIDER_CONFIG = "provider-config";
    public static final String PROVIDER_ID = "provider-id";
    public static final String PROVIDER_TYPE = "provider-type";
    public static final String CLASS_NAME = "class-name";
    /** Tags for Element request-policy */
    public static final String REQUEST_POLICY = "request-policy";
    public static final String AUTH_SOURCE = "auth-source";
    public static final String AUTH_RECIPIENT = "auth-recipient";
    /** Tags for Element response-policy */
    public static final String RESPONSE_POLICY = "response-policy";
    //public static final String AUTH_SOURCE = "auth-source";
    //public static final String AUTH_RECIPIENT = "auth-recipient";
    /** Tags for Element jms-host */
    public static final String JMS_HOST = "jms-host";
    //public static final String NAME = "name";
    public static final String HOST = "host";
    //public static final String PORT = "port";
    public static final String ADMIN_USER_NAME = "admin-user-name";
    public static final String ADMIN_PASSWORD = "admin-password";
    /** Tags for Element ejb-timer-service
    public static final String EJB_TIMER_SERVICE = "ejb-timer-service";
    public static final String MINIMUM_DELIVERY_INTERVAL_IN_MILLIS = "minimum-delivery-interval-in-millis";
    public static final String MAX_REDELIVERIES = "max-redeliveries";
    public static final String TIMER_DATASOURCE = "timer-datasource";
    public static final String REDELIVERY_INTERVAL_INTERNAL_IN_MILLIS = "redelivery-interval-internal-in-millis";
    /** Tags for Element session-config */
    public static final String SESSION_CONFIG = "session-config";
    /** Tags for Element session-manager */
    public static final String SESSION_MANAGER = "session-manager";
    /** Tags for Element session-properties */
    public static final String SESSION_PROPERTIES = "session-properties";
    //public static final String TIMEOUT_IN_SECONDS = "timeout-in-seconds";
    /** Tags for Element manager-properties */
    public static final String MANAGER_PROPERTIES = "manager-properties";
    public static final String SESSION_FILE_NAME = "session-file-name";
    public static final String REAP_INTERVAL_IN_SECONDS = "reap-interval-in-seconds";
    public static final String MAX_SESSIONS = "max-sessions";
    public static final String SESSION_ID_GENERATOR_CLASSNAME = "session-id-generator-classname";
    /** Tags for Element store-properties */
    public static final String STORE_PROPERTIES = "store-properties";
    public static final String DIRECTORY = "directory";
    //public static final String REAP_INTERVAL_IN_SECONDS = "reap-interval-in-seconds";
    /** Tags for Element das-config */
    public static final String DAS_CONFIG = "das-config";
    public static final String DYNAMIC_RELOAD_ENABLED = "dynamic-reload-enabled";
    public static final String DYNAMIC_RELOAD_POLL_INTERVAL_IN_SECONDS = "dynamic-reload-poll-interval-in-seconds";
    public static final String AUTODEPLOY_ENABLED = "autodeploy-enabled";
    public static final String AUTODEPLOY_POLLING_INTERVAL_IN_SECONDS = "autodeploy-polling-interval-in-seconds";
    public static final String AUTODEPLOY_DIR = "autodeploy-dir";
    public static final String AUTODEPLOY_VERIFIER_ENABLED = "autodeploy-verifier-enabled";
    public static final String AUTODEPLOY_JSP_PRECOMPILATION_ENABLED = "autodeploy-jsp-precompilation-enabled";
    public static final String DEPLOY_XML_VALIDATION = "deploy-xml-validation";
    public static final String ADMIN_SESSION_TIMEOUT_IN_MINUTES = "admin-session-timeout-in-minutes";
    /** Tags for Element orb */
    //public static final String ORB = "orb";
    public static final String USE_THREAD_POOL_IDS = "use-thread-pool-ids";
    public static final String MESSAGE_FRAGMENT_SIZE = "message-fragment-size";
    public static final String MAX_CONNECTIONS = "max-connections";
    /** Tags for Element ssl-client-config */
    public static final String SSL_CLIENT_CONFIG = "ssl-client-config";
    /** Tags for Element iiop-listener */
    public static final String IIOP_LISTENER = "iiop-listener";
    public static final String ID = "id";
    //public static final String ADDRESS = "address";
    //public static final String PORT = "port";
    //public static final String SECURITY_ENABLED = "security-enabled";
    //public static final String ENABLED = "enabled";
    /** Tags for Element access-log */
    public static final String ACCESS_LOG = "access-log";
    public static final String FORMAT = "format";
    public static final String ROTATION_POLICY = "rotation-policy";
    public static final String ROTATION_INTERVAL_IN_MINUTES = "rotation-interval-in-minutes";
    public static final String ROTATION_SUFFIX = "rotation-suffix";
    public static final String ROTATION_ENABLED = "rotation-enabled";
    /** Tags for Element http-listener */
    public static final String HTTP_LISTENER = "http-listener";
    //public static final String ID = "id";
    //public static final String ADDRESS = "address";
    //public static final String PORT = "port";
    public static final String EXTERNAL_PORT = "external-port";
    public static final String FAMILY = "family";
    public static final String BLOCKING_ENABLED = "blocking-enabled";
    public static final String ACCEPTOR_THREADS = "acceptor-threads";
    //public static final String SECURITY_ENABLED = "security-enabled";
    public static final String DEFAULT_VIRTUAL_SERVER = "default-virtual-server";
    public static final String SERVER_NAME = "server-name";
    public static final String REDIRECT_PORT = "redirect-port";
    public static final String XPOWERED_BY = "xpowered-by";
    //public static final String ENABLED = "enabled";
    /** Tags for Element virtual-server */
    public static final String VIRTUAL_SERVER = "virtual-server";
    //public static final String ID = "id";
    public static final String HTTP_LISTENERS = "http-listeners";
    public static final String DEFAULT_WEB_MODULE = "default-web-module";
    public static final String HOSTS = "hosts";
    public static final String STATE = "state";
    public static final String DOCROOT = "docroot";
    public static final String LOG_FILE = "log-file";
    /** Tags for Element request-processing */
    public static final String REQUEST_PROCESSING = "request-processing";
    public static final String THREAD_COUNT = "thread-count";
    public static final String INITIAL_THREAD_COUNT = "initial-thread-count";
    public static final String THREAD_INCREMENT = "thread-increment";
    public static final String REQUEST_TIMEOUT_IN_SECONDS = "request-timeout-in-seconds";
    public static final String HEADER_BUFFER_LENGTH_IN_BYTES = "header-buffer-length-in-bytes";
    /** Tags for Element keep-alive */
    public static final String KEEP_ALIVE = "keep-alive";
    //public static final String THREAD_COUNT = "thread-count";
    //public static final String MAX_CONNECTIONS = "max-connections";
    //public static final String TIMEOUT_IN_SECONDS = "timeout-in-seconds";
    /** Tags for Element connection-pool */
    public static final String CONNECTION_POOL = "connection-pool";
    public static final String QUEUE_SIZE_IN_BYTES = "queue-size-in-bytes";
    public static final String MAX_PENDING_COUNT = "max-pending-count";
    public static final String RECEIVE_BUFFER_SIZE_IN_BYTES = "receive-buffer-size-in-bytes";
    public static final String SEND_BUFFER_SIZE_IN_BYTES = "send-buffer-size-in-bytes";
    /** Tags for Element http-protocol */
    public static final String HTTP_PROTOCOL = "http-protocol";
    public static final String VERSION = "version";
    public static final String DNS_LOOKUP_ENABLED = "dns-lookup-enabled";
    public static final String FORCED_RESPONSE_TYPE = "forced-response-type";
    public static final String DEFAULT_RESPONSE_TYPE = "default-response-type";
    public static final String SSL_ENABLED = "ssl-enabled";
    /** Tags for Element http-file-cache */
    public static final String HTTP_FILE_CACHE = "http-file-cache";
    public static final String GLOBALLY_ENABLED = "globally-enabled";
    public static final String FILE_CACHING_ENABLED = "file-caching-enabled";
    public static final String MAX_AGE_IN_SECONDS = "max-age-in-seconds";
    public static final String MEDIUM_FILE_SIZE_LIMIT_IN_BYTES = "medium-file-size-limit-in-bytes";
    public static final String MEDIUM_FILE_SPACE_IN_BYTES = "medium-file-space-in-bytes";
    public static final String SMALL_FILE_SIZE_LIMIT_IN_BYTES = "small-file-size-limit-in-bytes";
    public static final String SMALL_FILE_SPACE_IN_BYTES = "small-file-space-in-bytes";
    public static final String FILE_TRANSMISSION_ENABLED = "file-transmission-enabled";
    public static final String MAX_FILES_COUNT = "max-files-count";
    public static final String HASH_INIT_SIZE = "hash-init-size";
    /** Tags for Element http-access-log */
    public static final String HTTP_ACCESS_LOG = "http-access-log";
    public static final String LOG_DIRECTORY = "log-directory";
    public static final String IPONLY = "iponly";
    /** Tags for Element custom-resource */
    public static final String CUSTOM_RESOURCE = "custom-resource";
    //public static final String DESCRIPTION = "description";
    public static final String JNDI_NAME = "jndi-name";
    public static final String RES_TYPE = "res-type";
    public static final String FACTORY_CLASS = "factory-class";
    public static final String OBJECT_TYPE = "object-type";
    //public static final String ENABLED = "enabled";
    /** Tags for Element external-jndi-resource */
    public static final String EXTERNAL_JNDI_RESOURCE = "external-jndi-resource";
    //public static final String DESCRIPTION = "description";
    //public static final String JNDI_NAME = "jndi-name";
    public static final String JNDI_LOOKUP_NAME = "jndi-lookup-name";
    //public static final String RES_TYPE = "res-type";
    //public static final String FACTORY_CLASS = "factory-class";
    //public static final String OBJECT_TYPE = "object-type";
    //public static final String ENABLED = "enabled";
    public static final String DEPLOYMENT_ORDER = "deployment-order";
    /** Tags for Element jdbc-resource */
    public static final String JDBC_RESOURCE = "jdbc-resource";
    //public static final String DESCRIPTION = "description";
    //public static final String JNDI_NAME = "jndi-name";
    public static final String POOL_NAME = "pool-name";
    //public static final String OBJECT_TYPE = "object-type";
    //public static final String ENABLED = "enabled";
    /** Tags for Element mail-resource */
    public static final String MAIL_RESOURCE = "mail-resource";
    //public static final String DESCRIPTION = "description";
    //public static final String JNDI_NAME = "jndi-name";
    public static final String STORE_PROTOCOL = "store-protocol";
    public static final String STORE_PROTOCOL_CLASS = "store-protocol-class";
    public static final String TRANSPORT_PROTOCOL = "transport-protocol";
    public static final String TRANSPORT_PROTOCOL_CLASS = "transport-protocol-class";
    //public static final String HOST = "host";
    public static final String USER = "user";
    public static final String FROM = "from";
    public static final String DEBUG = "debug";
    //public static final String OBJECT_TYPE = "object-type";
    //public static final String ENABLED = "enabled";
    /** Tags for Element persistence-manager-factory-resource  */
    public static final String PERSISTENCE_MANAGER_FACTORY_RESOURCE = "persistence-manager-factory-resource";
    //public static final String DESCRIPTION = "description";
    //public static final String JNDI_NAME = "jndi-name";
    //public static final String FACTORY_CLASS = "factory-class";
    public static final String JDBC_RESOURCE_JNDI_NAME = "jdbc-resource-jndi-name";
    //public static final String OBJECT_TYPE = "object-type";
    //public static final String ENABLED = "enabled";
    /** Tags for Element admin-object-resource */
    public static final String ADMIN_OBJECT_RESOURCE = "admin-object-resource";
    //public static final String DESCRIPTION = "description";
    //public static final String JNDI_NAME = "jndi-name";
    //public static final String RES_TYPE = "res-type";
    public static final String RES_ADAPTER = "res-adapter";
    //public static final String OBJECT_TYPE = "object-type";
    //public static final String ENABLED = "enabled";
    /** Tags for Element connector-resource */
    public static final String CONNECTOR_RESOURCE = "connector-resource";
    //public static final String DESCRIPTION = "description";
    //public static final String JNDI_NAME = "jndi-name";
    //public static final String POOL_NAME = "pool-name";
    //public static final String OBJECT_TYPE = "object-type";
    //public static final String ENABLED = "enabled";
    /** Tags for Element resource-adapter-config */
    public static final String RESOURCE_ADAPTER_CONFIG = "resource-adapter-config";
    //public static final String NAME = "name";
    public static final String THREAD_POOL_IDS = "thread-pool-ids";
    //public static final String OBJECT_TYPE = "object-type";
    public static final String RESOURCE_ADAPTER_NAME = "resource-adapter-name";
    /** Tags for Element jdbc-connection-pool */
    //public static final String JDBC_CONNECTION_POOL = "jdbc-connection-pool";
    //public static final String DESCRIPTION = "description";
    //public static final String NAME = "name";
    public static final String DATASOURCE_CLASSNAME = "datasource-classname";
    //public static final String RES_TYPE = "res-type";
    //public static final String STEADY_POOL_SIZE = "steady-pool-size";
    //public static final String MAX_POOL_SIZE = "max-pool-size";
    public static final String MAX_WAIT_TIME_IN_MILLIS = "max-wait-time-in-millis";
    //public static final String POOL_RESIZE_QUANTITY = "pool-resize-quantity";
    //public static final String IDLE_TIMEOUT_IN_SECONDS = "idle-timeout-in-seconds";
    public static final String TRANSACTION_ISOLATION_LEVEL = "transaction-isolation-level";
    public static final String IS_ISOLATION_LEVEL_GUARANTEED = "is-isolation-level-guaranteed";
    public static final String IS_CONNECTION_VALIDATION_REQUIRED = "is-connection-validation-required";
    public static final String CONNECTION_VALIDATION_METHOD = "connection-validation-method";
    public static final String VALIDATION_TABLE_NAME = "validation-table-name";
    public static final String FAIL_ALL_CONNECTIONS = "fail-all-connections";
    public static final String NON_TRANSACTIONAL_CONNECTIONS = "non-transactional-connections";
    public static final String ALLOW_NON_COMPONENT_CALLERS = "allow-non-component-callers";
    public static final String VALIDATE_ATMOST_ONCE_PERIOD_IN_SECONDS = "validate-atmost-once-period-in-seconds";
    public static final String CONNECTION_LEAK_TIMEOUT_IN_SECONDS = "connection-leak-timeout-in-seconds";
    public static final String CONNECTION_LEAK_RECLAIM = "connection-leak-reclaim";
    public static final String CONNECTION_CREATION_RETRY_ATTEMPTS = "connection-creation-retry-attempts";
    public static final String CONNECTION_CREATION_RETRY_INTERVAL_IN_SECONDS = "connection-creation-retry-interval-in-seconds";
    public static final String STATEMENT_TIMEOUT_IN_SECONDS = "statement-timeout-in-seconds";
    public static final String LAZY_CONNECTION_ENLISTMENT = "lazy-connection-enlistment";
    public static final String LAZY_CONNECTION_ASSOCIATION = "lazy-connection-association";
    public static final String ASSOCIATE_WITH_THREAD = "associate-with-thread";
    public static final String MATCH_CONNECTIONS = "match-connections";
    public static final String MAX_CONNECTION_USAGE_COUNT = "max-connection-usage-count";
    public static final String WRAP_JDBC_OBJECTS = "wrap-jdbc-objects";
    /** Tags for Element connector-connection-pool */
    //public static final String CONNECTOR_CONNECTION_POOL = "connector-connection-pool";
    //public static final String DESCRIPTION = "description";
    //public static final String NAME = "name";
    //public static final String RESOURCE_ADAPTER_NAME = "resource-adapter-name";
    public static final String CONNECTION_DEFINITION_NAME = "connection-definition-name";
    //public static final String STEADY_POOL_SIZE = "steady-pool-size";
    //public static final String MAX_POOL_SIZE = "max-pool-size";
    //public static final String MAX_WAIT_TIME_IN_MILLIS = "max-wait-time-in-millis";
    //public static final String POOL_RESIZE_QUANTITY = "pool-resize-quantity";
    //public static final String IDLE_TIMEOUT_IN_SECONDS = "idle-timeout-in-seconds";
    //public static final String FAIL_ALL_CONNECTIONS = "fail-all-connections";
    public static final String TRANSACTION_SUPPORT = "transaction-support";
    //public static final String IS_CONNECTION_VALIDATION_REQUIRED = "is-connection-validation-required";
    //public static final String VALIDATE_ATMOST_ONCE_PERIOD_IN_SECONDS = "validate-atmost-once-period-in-seconds";
    //public static final String CONNECTION_LEAK_TIMEOUT_IN_SECONDS = "connection-leak-timeout-in-seconds";
    //public static final String CONNECTION_LEAK_RECLAIM = "connection-leak-reclaim";
    //public static final String CONNECTION_CREATION_RETRY_ATTEMPTS = "connection-creation-retry-attempts";
    //public static final String CONNECTION_CREATION_RETRY_INTERVAL_IN_SECONDS = "connection-creation-retry-interval-in-seconds";
    //public static final String LAZY_CONNECTION_ENLISTMENT = "lazy-connection-enlistment";
    //public static final String LAZY_CONNECTION_ASSOCIATION = "lazy-connection-association";
    //public static final String ASSOCIATE_WITH_THREAD = "associate-with-thread";
    //public static final String MATCH_CONNECTIONS = "match-connections";
    //public static final String MAX_CONNECTION_USAGE_COUNT = "max-connection-usage-count";
    /** Tags for concurrent resources elements */
    public static final String CONTEXT_SERVICE = "context-service";
    public static final String MANAGED_THREAD_FACTORY = "managed-thread-factory";
    public static final String MANAGED_EXECUTOR_SERVICE = "managed-executor-service";
    public static final String MANAGED_SCHEDULED_EXECUTOR_SERVICE = "managed-scheduled-executor-service";
    /** Tags for Element security-map */
    public static final String SECURITY_MAP = "security-map";
    public static final String PRINCIPAL = "principal";
    public static final String USER_GROUP = "user-group";
    //public static final String NAME = "name"; */
    /** Tags for Element backend-principal
    public static final String BACKEND_PRINCIPAL = "backend-principal";
    public static final String USER_NAME = "user-name";
    public static final String PASSWORD = "password";
    /** Tags for Element lifecycle-module */
    public static final String LIFECYCLE_MODULE = "lifecycle-module";
    //public static final String DESCRIPTION = "description";
    //public static final String NAME = "name";
    //public static final String CLASS_NAME = "class-name";
    //public static final String CLASSPATH = "classpath";
    public static final String LOAD_ORDER = "load-order";
    public static final String IS_FAILURE_FATAL = "is-failure-fatal";
    //public static final String OBJECT_TYPE = "object-type";
    //public static final String ENABLED = "enabled";
    public static final String IS_LIFECYCLE = "isLifecycle";
    public static final String LOAD_SYSTEM_APP_ON_STARTUP = 
        "load-system-app-on-startup";
    /** Tags for Element j2ee-application */
    public static final String DEFAULT_APP_NAME = "defaultAppName";
    public static final String IS_COMPOSITE = "isComposite";
    public static final String APP_CONFIG = "appConfig";
    public static final String J2EE_APPLICATION = "j2ee-application";
    //public static final String DESCRIPTION = "description";
    //public static final String NAME = "name";
    public static final String LOCATION = "location";
    //public static final String OBJECT_TYPE = "object-type";
    //public static final String ENABLED = "enabled";
    public static final String LIBRARIES = "libraries";
    //public static final String AVAILABILITY_ENABLED = "availability-enabled";
    public static final String DIRECTORY_DEPLOYED = "directory-deployed";
    public static final String JAVA_WEB_START_ENABLED = "java-web-start-enabled";
    /** Tags for Element ejb-module */
    public static final String EJB_MODULE = "ejb-module";
    //public static final String DESCRIPTION = "description";
    //public static final String NAME = "name";
    //public static final String LOCATION = "location";
    //public static final String OBJECT_TYPE = "object-type";
    //public static final String ENABLED = "enabled";
    //public static final String LIBRARIES = "libraries";
    //public static final String AVAILABILITY_ENABLED = "availability-enabled";
    //public static final String DIRECTORY_DEPLOYED = "directory-deployed";
    /** Tags for Element web-module */
    public static final String WEB_MODULE = "web-module";
    //public static final String DESCRIPTION = "description";
    //public static final String NAME = "name";
    public static final String CONTEXT_ROOT = "context-root";
    //public static final String LOCATION = "location";
    //public static final String OBJECT_TYPE = "object-type";
    //public static final String ENABLED = "enabled";
    //public static final String LIBRARIES = "libraries";
    //public static final String AVAILABILITY_ENABLED = "availability-enabled";
    //public static final String DIRECTORY_DEPLOYED = "directory-deployed";
    /** Tags for Element connector-module */
    public static final String CONNECTOR_MODULE = "connector-module";
    //public static final String DESCRIPTION = "description";
    //public static final String NAME = "name";
    //public static final String LOCATION = "location";
    //public static final String OBJECT_TYPE = "object-type";
    //public static final String ENABLED = "enabled";
    //public static final String DIRECTORY_DEPLOYED = "directory-deployed";
    /** Tags for Element appclient-module */
    public static final String APPCLIENT_MODULE = "appclient-module";
    //public static final String DESCRIPTION = "description";
    //public static final String NAME = "name";
    //public static final String LOCATION = "location";
    //public static final String DIRECTORY_DEPLOYED = "directory-deployed";
    //public static final String JAVA_WEB_START_ENABLED = "java-web-start-enabled";
    /** Tags for Element mbean */
    public static final String MBEAN = "mbean";
    //public static final String DESCRIPTION = "description";
    //public static final String NAME = "name";
    //public static final String OBJECT_TYPE = "object-type";
    public static final String IMPL_CLASS_NAME = "impl-class-name";
    public static final String OBJECT_NAME = "object-name";
    //public static final String ENABLED = "enabled";
    /** Tags for Element extension-module */
    public static final String EXTENSION_MODULE = "extension-module";
    //public static final String DESCRIPTION = "description";
    //public static final String NAME = "name";
    //public static final String LOCATION = "location";
    public static final String MODULE_TYPE = "module-type";
    //public static final String OBJECT_TYPE = "object-type";
    //public static final String ENABLED = "enabled";
    //public static final String LIBRARIES = "libraries";
    //public static final String AVAILABILITY_ENABLED = "availability-enabled";
    //public static final String DIRECTORY_DEPLOYED = "directory-deployed";
    /** Tags for Element web-service-endpoint */
    public static final String WEB_SERVICE_ENDPOINT = "web-service-endpoint";
    //public static final String NAME = "name";
    public static final String MONITORING = "monitoring";
    public static final String MAX_HISTORY_SIZE = "max-history-size";
    public static final String JBI_ENABLED = "jbi-enabled";
    /** Tags for Element registry-location */
    public static final String REGISTRY_LOCATION = "registry-location";
    public static final String CONNECTOR_RESOURCE_JNDI_NAME = "connector-resource-jndi-name";
    /** Tags for Element transformation-rule */
    public static final String TRANSFORMATION_RULE = "transformation-rule";
    //public static final String NAME = "name";
    //public static final String ENABLED = "enabled";
    public static final String APPLY_TO = "apply-to";
    public static final String RULE_FILE_LOCATION = "rule-file-location";
    //public static final String DESCRIPTION = "description";
    //public static final String NODE_AGENT_REF = "node-agent-ref";
    //public static final String CONFIG_REF = "config-ref";
    
    public static final String ADMIN_CONSOLE_CONTEXT_ROOT	= "adminConsoleContextRoot";
    public static final String ADMIN_CONSOLE_DOWNLOAD_LOCATION	= "adminConsoleDownloadLocation";
    public static final String IPS_ROOT				= "ipsRoot";
    public static final String ADMIN_CONSOLE_VERSION		= "adminConsoleVersion";
    public static final String ADMIN_LISTENER_ID = "admin-listener";
    public final static String SEC_ADMIN_LISTENER_PROTOCOL_NAME = "sec-admin-listener";
    public final static String REDIRECT_PROTOCOL_NAME = "admin-http-redirect";
    public final static String PORT_UNIF_PROTOCOL_NAME = "pu-protocol";
}

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

package com.sun.enterprise.config.serverbeans;

import org.jvnet.hk2.config.ConfigBeanProxy;

public class ServerTags  {
    // Tags for Element domain
    static public final String DOMAIN = "domain";
    static public final String APPLICATION_ROOT = "application-root";
    static public final String LOG_ROOT = "log-root";
    static public final String LOCALE = "locale";
    // Tags for Element applications
    static public final String APPLICATIONS = "applications";
    static public final String APPLICATION = "application";
    // Tags for Element resources
    static public final String RESOURCES = "resources";
    // Tags for Element configs
    static public final String CONFIGS = "configs";
    // Tags for Element servers
    static public final String SERVERS = "servers";
    // Tags for Element clusters
    static public final String CLUSTERS = "clusters";
    // Tags for Element node-agents
    static public final String NODE_AGENTS = "node-agents";
    // Tags for Element lb-configs
    static public final String LB_CONFIGS = "lb-configs";
    // Tags for Element load-balancers
    static public final String LOAD_BALANCERS = "load-balancers";
    // Tags for Element system-property
    static public final String SYSTEM_PROPERTY = "system-property";
    static public final String DESCRIPTION = "description";
    static public final String NAME = "name";
    static public final String VALUE = "value";
    // Tags for Element element-property
    static public final String ELEMENT_PROPERTY = "element-property";
    //static public final String DESCRIPTION = "description";
    //static public final String NAME = "name";
    //static public final String VALUE = "value";
    // Tags for Element load-balancer
    static public final String LOAD_BALANCER = "load-balancer";
    //static public final String NAME = "name";
    static public final String LB_CONFIG_NAME = "lb-config-name";
    static public final String AUTO_APPLY_ENABLED = "auto-apply-enabled";
    // Tags for Element lb-config
    static public final String LB_CONFIG = "lb-config";
    //static public final String NAME = "name";
    static public final String RESPONSE_TIMEOUT_IN_SECONDS = "response-timeout-in-seconds";
    static public final String HTTPS_ROUTING = "https-routing";
    static public final String RELOAD_POLL_INTERVAL_IN_SECONDS = "reload-poll-interval-in-seconds";
    static public final String MONITORING_ENABLED = "monitoring-enabled";
    static public final String ROUTE_COOKIE_ENABLED = "route-cookie-enabled";
    // Tags for Element cluster-ref
    static public final String CLUSTER_REF = "cluster-ref";
    static public final String REF = "ref";
    static public final String LB_POLICY = "lb-policy";
    static public final String LB_POLICY_MODULE = "lb-policy-module";
    // Tags for Element server-ref
    static public final String SERVER_REF = "server-ref";
    //static public final String REF = "ref";
    static public final String DISABLE_TIMEOUT_IN_MINUTES = "disable-timeout-in-minutes";
    static public final String LB_ENABLED = "lb-enabled";
    static public final String ENABLED = "enabled";
    // Tags for Element health-checker
    static public final String HEALTH_CHECKER = "health-checker";
    static public final String URL = "url";
    static public final String INTERVAL_IN_SECONDS = "interval-in-seconds";
    static public final String TIMEOUT_IN_SECONDS = "timeout-in-seconds";
    // Tags for Element node-agent
    static public final String NODE_AGENT = "node-agent";
    //static public final String NAME = "name";
    static public final String SYSTEM_JMX_CONNECTOR_NAME = "system-jmx-connector-name";
    static public final String START_SERVERS_IN_STARTUP = "start-servers-in-startup";
    // Tags for Element jmx-connector
    static public final String JMX_CONNECTOR = "jmx-connector";
    //static public final String NAME = "name";
    //static public final String ENABLED = "enabled";
    static public final String PROTOCOL = "protocol";
    static public final String ADDRESS = "address";
    static public final String PORT = "port";
    static public final String ACCEPT_ALL = "accept-all";
    static public final String AUTH_REALM_NAME = "auth-realm-name";
    static public final String SECURITY_ENABLED = "security-enabled";
    // Tags for Element auth-realm
    static public final String AUTH_REALM = "auth-realm";
    //static public final String NAME = "name";
    static public final String CLASSNAME = "classname";
    // Tags for Element log-service
    static public final String LOG_SERVICE = "log-service";
    static public final String FILE = "file";
    static public final String USE_SYSTEM_LOGGING = "use-system-logging";
    static public final String LOG_HANDLER = "log-handler";
    static public final String LOG_FILTER = "log-filter";
    static public final String LOG_TO_CONSOLE = "log-to-console";
    static public final String LOG_ROTATION_LIMIT_IN_BYTES = "log-rotation-limit-in-bytes";
    static public final String LOG_ROTATION_TIMELIMIT_IN_MINUTES = "log-rotation-timelimit-in-minutes";
    static public final String ALARMS = "alarms";
    static public final String RETAIN_ERROR_STATISTICS_FOR_HOURS = "retain-error-statistics-for-hours";
    // Tags for Element module-log-levels
    static public final String MODULE_LOG_LEVELS = "module-log-levels";
    static public final String ROOT = "root";
    static public final String SERVER = "server";
    static public final String EJB_CONTAINER = "ejb-container";
    static public final String CMP_CONTAINER = "cmp-container";
    static public final String MDB_CONTAINER = "mdb-container";
    static public final String WEB_CONTAINER = "web-container";
    static public final String CLASSLOADER = "classloader";
    static public final String CONFIGURATION = "configuration";
    static public final String NAMING = "naming";
    static public final String SECURITY = "security";
    static public final String JTS = "jts";
    static public final String JTA = "jta";
    static public final String ADMIN = "admin";
    static public final String DEPLOYMENT = "deployment";
    static public final String VERIFIER = "verifier";
    static public final String JAXR = "jaxr";
    static public final String JAXRPC = "jaxrpc";
    static public final String SAAJ = "saaj";
    static public final String CORBA = "corba";
    static public final String JAVAMAIL = "javamail";
    static public final String JMS = "jms";
    static public final String CONNECTOR = "connector";
    static public final String JDO = "jdo";
    static public final String CMP = "cmp";
    static public final String UTIL = "util";
    static public final String RESOURCE_ADAPTER = "resource-adapter";
    static public final String SYNCHRONIZATION = "synchronization";
    //static public final String NODE_AGENT = "node-agent";
    static public final String NODES ="nodes";
    
    static public final String SELF_MANAGEMENT = "self-management";
    static public final String GROUP_MANAGEMENT_SERVICE = "group-management-service";
    static public final String MANAGEMENT_EVENT = "management-event";
    // Tags for Element ssl
    static public final String SSL = "ssl";
    static public final String CERT_NICKNAME = "cert-nickname";
    static public final String SSL2_ENABLED = "ssl2-enabled";
    static public final String SSL2_CIPHERS = "ssl2-ciphers";
    static public final String SSL3_ENABLED = "ssl3-enabled";
    static public final String SSL3_TLS_CIPHERS = "ssl3-tls-ciphers";
    static public final String TLS_ENABLED = "tls-enabled";
    static public final String TLS_ROLLBACK_ENABLED = "tls-rollback-enabled";
    static public final String CLIENT_AUTH_ENABLED = "client-auth-enabled";
    // Tags for Element cluster
    static public final String CLUSTER = "cluster";
    //static public final String NAME = "name";
    static public final String CONFIG_REF = "config-ref";
    static public final String HEARTBEAT_ENABLED = "heartbeat-enabled";
    static public final String HEARTBEAT_PORT = "heartbeat-port";
    static public final String HEARTBEAT_ADDRESS = "heartbeat-address";
    // Tags for Element resource-ref
    static public final String RESOURCE_REF = "resource-ref";
    //static public final String ENABLED = "enabled";
    //static public final String REF = "ref";
    // Tags for Element application-ref
    static public final String APPLICATION_REF = "application-ref";
    //static public final String ENABLED = "enabled";
    static public final String VIRTUAL_SERVERS = "virtual-servers";
    //static public final String LB_ENABLED = "lb-enabled";
    //static public final String DISABLE_TIMEOUT_IN_MINUTES = "disable-timeout-in-minutes";
    //static public final String REF = "ref";
    // Tags for Element server
    //static public final String SERVER = "server";
    //static public final String NAME = "name";
    //static public final String CONFIG_REF = "config-ref";
    static public final String NODE_AGENT_REF = "node-agent-ref";
    static public final String LB_WEIGHT = "lb-weight";
    // Tags for Element config
    static public final String CONFIG = "config";
    //static public final String NAME = "name";
    static public final String DYNAMIC_RECONFIGURATION_ENABLED = "dynamic-reconfiguration-enabled";
    // Tags for Element http-service
    static public final String HTTP_SERVICE = "http-service";
    // Tags for Element iiop-service
    static public final String IIOP_SERVICE = "iiop-service";
    static public final String CLIENT_AUTHENTICATION_REQUIRED = "client-authentication-required";
    // Tags for Element admin-service
    static public final String ADMIN_SERVICE = "admin-service";
    static public final String TYPE = "type";
    //static public final String SYSTEM_JMX_CONNECTOR_NAME = "system-jmx-connector-name";
    // Tags for Element connector-service
    static public final String CONNECTOR_SERVICE = "connector-service";
    static public final String SHUTDOWN_TIMEOUT_IN_SECONDS = "shutdown-timeout-in-seconds";
// Tags for Element web-container
//static public final String WEB_CONTAINER = "web-container";
    // Tags for Element ejb-container
    //static public final String EJB_CONTAINER = "ejb-container";
    static public final String STEADY_POOL_SIZE = "steady-pool-size";
    static public final String POOL_RESIZE_QUANTITY = "pool-resize-quantity";
    static public final String MAX_POOL_SIZE = "max-pool-size";
    static public final String CACHE_RESIZE_QUANTITY = "cache-resize-quantity";
    static public final String MAX_CACHE_SIZE = "max-cache-size";
    static public final String POOL_IDLE_TIMEOUT_IN_SECONDS = "pool-idle-timeout-in-seconds";
    static public final String CACHE_IDLE_TIMEOUT_IN_SECONDS = "cache-idle-timeout-in-seconds";
    static public final String REMOVAL_TIMEOUT_IN_SECONDS = "removal-timeout-in-seconds";
    static public final String VICTIM_SELECTION_POLICY = "victim-selection-policy";
    static public final String COMMIT_OPTION = "commit-option";
    static public final String SESSION_STORE = "session-store";
    // Tags for Element mdb-container
    //static public final String MDB_CONTAINER = "mdb-container";
    //static public final String STEADY_POOL_SIZE = "steady-pool-size";
    //static public final String POOL_RESIZE_QUANTITY = "pool-resize-quantity";
    //static public final String MAX_POOL_SIZE = "max-pool-size";
    static public final String IDLE_TIMEOUT_IN_SECONDS = "idle-timeout-in-seconds";
    // Tags for Element jms-service
    static public final String JMS_SERVICE = "jms-service";
    static public final String INIT_TIMEOUT_IN_SECONDS = "init-timeout-in-seconds";
    static public final String MASTER_BROKER = "master-broker";
    //static public final String TYPE = "type";
    static public final String START_ARGS = "start-args";
    static public final String DEFAULT_JMS_HOST = "default-jms-host";
    static public final String RECONNECT_INTERVAL_IN_SECONDS = "reconnect-interval-in-seconds";
    static public final String RECONNECT_ATTEMPTS = "reconnect-attempts";
    static public final String RECONNECT_ENABLED = "reconnect-enabled";
    static public final String ADDRESSLIST_BEHAVIOR = "addresslist-behavior";
    static public final String ADDRESSLIST_ITERATIONS = "addresslist-iterations";
    static public final String MQ_SCHEME = "mq-scheme";
    static public final String MQ_SERVICE = "mq-service";
    // Tags for Element security-service
    static public final String SECURITY_SERVICE = "security-service";
    static public final String DEFAULT_REALM = "default-realm";
    static public final String DEFAULT_PRINCIPAL = "default-principal";
    static public final String DEFAULT_PRINCIPAL_PASSWORD = "default-principal-password";
    static public final String ANONYMOUS_ROLE = "anonymous-role";
    static public final String AUDIT_ENABLED = "audit-enabled";
    static public final String JACC = "jacc";
    static public final String AUDIT_MODULES = "audit-modules";
    static public final String ACTIVATE_DEFAULT_PRINCIPAL_TO_ROLE_MAPPING = "activate-default-principal-to-role-mapping";
    static public final String MAPPED_PRINCIPAL_CLASS = "mapped-principal-class";
    // Tags for Element transaction-service
    static public final String TRANSACTION_SERVICE = "transaction-service";
    static public final String AUTOMATIC_RECOVERY = "automatic-recovery";
    //static public final String TIMEOUT_IN_SECONDS = "timeout-in-seconds";
    static public final String TX_LOG_DIR = "tx-log-dir";
    static public final String HEURISTIC_DECISION = "heuristic-decision";
    static public final String RETRY_TIMEOUT_IN_SECONDS = "retry-timeout-in-seconds";
    static public final String KEYPOINT_INTERVAL = "keypoint-interval";
    // Tags for Element monitoring-service
    static public final String MONITORING_SERVICE = "monitoring-service";
    // Tags for Element diagnostic-service
    static public final String DIAGNOSTIC_SERVICE = "diagnostic-service";
    static public final String COMPUTE_CHECKSUM = "compute-checksum";
    static public final String VERIFY_CONFIG = "verify-config";
    static public final String CAPTURE_INSTALL_LOG = "capture-install-log";
    static public final String CAPTURE_SYSTEM_INFO = "capture-system-info";
    static public final String CAPTURE_HADB_INFO = "capture-hadb-info";
    static public final String CAPTURE_APP_DD = "capture-app-dd";
    static public final String MIN_LOG_LEVEL = "min-log-level";
    static public final String MAX_LOG_ENTRIES = "max-log-entries";
    // Tags for Element java-config
    static public final String JAVA_CONFIG = "java-config";
    static public final String JVM_OPTIONS = "jvm-options";
    static public final String JAVA_HOME = "java-home";
    static public final String DEBUG_ENABLED = "debug-enabled";
    static public final String DEBUG_OPTIONS = "debug-options";
    static public final String RMIC_OPTIONS = "rmic-options";
    static public final String JAVAC_OPTIONS = "javac-options";
    static public final String CLASSPATH_PREFIX = "classpath-prefix";
    static public final String CLASSPATH_SUFFIX = "classpath-suffix";
    static public final String SERVER_CLASSPATH = "server-classpath";
    static public final String SYSTEM_CLASSPATH = "system-classpath";
    static public final String NATIVE_LIBRARY_PATH_PREFIX = "native-library-path-prefix";
    static public final String NATIVE_LIBRARY_PATH_SUFFIX = "native-library-path-suffix";
    static public final String BYTECODE_PREPROCESSORS = "bytecode-preprocessors";
    static public final String ENV_CLASSPATH_IGNORED = "env-classpath-ignored";
    // Tags for Element availability-service
    static public final String AVAILABILITY_SERVICE = "availability-service";
    static public final String AVAILABILITY_ENABLED = "availability-enabled";
    static public final String HA_AGENT_HOSTS = "ha-agent-hosts";
    static public final String HA_AGENT_PORT = "ha-agent-port";
    static public final String HA_AGENT_PASSWORD = "ha-agent-password";
    static public final String HA_STORE_NAME = "ha-store-name";
    static public final String AUTO_MANAGE_HA_STORE = "auto-manage-ha-store";
    static public final String STORE_POOL_NAME = "store-pool-name";
    static public final String HA_STORE_HEALTHCHECK_ENABLED = "ha-store-healthcheck-enabled";
    static public final String HA_STORE_HEALTHCHECK_INTERVAL_IN_SECONDS = "ha-store-healthcheck-interval-in-seconds";
    // Tags for Element thread-pools
    static public final String THREAD_POOLS = "thread-pools";
    // Tags for Element alert-service
    static public final String ALERT_SERVICE = "alert-service";
    // Tags for Element group-management-service
    //static public final String GROUP_MANAGEMENT_SERVICE = "group-management-service";
    static public final String FD_PROTOCOL_MAX_TRIES = "fd-protocol-max-tries";
    static public final String FD_PROTOCOL_TIMEOUT_IN_MILLIS = "fd-protocol-timeout-in-millis";
    static public final String MERGE_PROTOCOL_MAX_INTERVAL_IN_MILLIS = "merge-protocol-max-interval-in-millis";
    static public final String MERGE_PROTOCOL_MIN_INTERVAL_IN_MILLIS = "merge-protocol-min-interval-in-millis";
    static public final String PING_PROTOCOL_TIMEOUT_IN_MILLIS = "ping-protocol-timeout-in-millis";
    static public final String VS_PROTOCOL_TIMEOUT_IN_MILLIS = "vs-protocol-timeout-in-millis";
    // Tags for Element management-rules
    static public final String MANAGEMENT_RULES = "management-rules";
    //static public final String ENABLED = "enabled";
    // Tags for Element management-rule
    static public final String MANAGEMENT_RULE = "management-rule";
    //static public final String DESCRIPTION = "description";
    //static public final String NAME = "name";
    //static public final String ENABLED = "enabled";
    // Tags for Element event
    static public final String EVENT = "event";
    //static public final String DESCRIPTION = "description";
    //static public final String TYPE = "type";
    static public final String RECORD_EVENT = "record-event";
    static public final String LEVEL = "level";
    // Tags for Element action
    static public final String ACTION = "action";
    static public final String ACTION_MBEAN_NAME = "action-mbean-name";
    // Tags for Element alert-subscription
    static public final String ALERT_SUBSCRIPTION = "alert-subscription";
    //static public final String NAME = "name";
    // Tags for Element listener-config
    static public final String LISTENER_CONFIG = "listener-config";
    static public final String LISTENER_CLASS_NAME = "listener-class-name";
    static public final String SUBSCRIBE_LISTENER_WITH = "subscribe-listener-with";
    // Tags for Element filter-config
    static public final String FILTER_CONFIG = "filter-config";
    static public final String FILTER_CLASS_NAME = "filter-class-name";
    // Tags for Element thread-pool
    static public final String THREAD_POOL = "thread-pool";
    static public final String THREAD_POOL_ID = "thread-pool-id";
    static public final String MIN_THREAD_POOL_SIZE = "min-thread-pool-size";
    static public final String MAX_THREAD_POOL_SIZE = "max-thread-pool-size";
    static public final String IDLE_THREAD_TIMEOUT_IN_SECONDS = "idle-thread-timeout-in-seconds";
    static public final String NUM_WORK_QUEUES = "num-work-queues";
    // Tags for Element web-container-availability
    static public final String WEB_CONTAINER_AVAILABILITY = "web-container-availability";
    //static public final String AVAILABILITY_ENABLED = "availability-enabled";
    static public final String PERSISTENCE_TYPE = "persistence-type";
    static public final String PERSISTENCE_FREQUENCY = "persistence-frequency";
    static public final String PERSISTENCE_SCOPE = "persistence-scope";
    static public final String PERSISTENCE_STORE_HEALTH_CHECK_ENABLED = "persistence-store-health-check-enabled";
    static public final String SSO_FAILOVER_ENABLED = "sso-failover-enabled";
    static public final String HTTP_SESSION_STORE_POOL_NAME = "http-session-store-pool-name";
    // Tags for Element ejb-container-availability
    static public final String EJB_CONTAINER_AVAILABILITY = "ejb-container-availability";
    //static public final String AVAILABILITY_ENABLED = "availability-enabled";
    static public final String SFSB_HA_PERSISTENCE_TYPE = "sfsb-ha-persistence-type";
    static public final String SFSB_PERSISTENCE_TYPE = "sfsb-persistence-type";
    static public final String SFSB_CHECKPOINT_ENABLED = "sfsb-checkpoint-enabled";
    static public final String SFSB_QUICK_CHECKPOINT_ENABLED = "sfsb-quick-checkpoint-enabled";
    static public final String SFSB_STORE_POOL_NAME = "sfsb-store-pool-name";
    // Tags for Element jms-availability
    static public final String JMS_AVAILABILITY = "jms-availability";
    //static public final String AVAILABILITY_ENABLED = "availability-enabled";
    static public final String MQ_STORE_POOL_NAME = "mq-store-pool-name";
    // Tags for Element profiler
    static public final String PROFILER = "profiler";
    //static public final String JVM_OPTIONS = "jvm-options";
    //static public final String NAME = "name";
    static public final String CLASSPATH = "classpath";
    static public final String NATIVE_LIBRARY_PATH = "native-library-path";
    //static public final String ENABLED = "enabled";
    // Tags for Element module-monitoring-levels
    static public final String MODULE_MONITORING_LEVELS = "module-monitoring-levels";
    //static public final String THREAD_POOL = "thread-pool";
    static public final String ORB = "orb";
    //static public final String EJB_CONTAINER = "ejb-container";
    //static public final String WEB_CONTAINER = "web-container";
    //static public final String TRANSACTION_SERVICE = "transaction-service";
    //static public final String HTTP_SERVICE = "http-service";
    static public final String JDBC_CONNECTION_POOL = "jdbc-connection-pool";
    static public final String CONNECTOR_CONNECTION_POOL = "connector-connection-pool";
    //static public final String CONNECTOR_SERVICE = "connector-service";
    //static public final String JMS_SERVICE = "jms-service";
    static public final String JVM = "jvm";
    // Tags for Element jacc-provider
    static public final String JACC_PROVIDER = "jacc-provider";
    //static public final String NAME = "name";
    static public final String POLICY_PROVIDER = "policy-provider";
    static public final String POLICY_CONFIGURATION_FACTORY_PROVIDER = "policy-configuration-factory-provider";
    // Tags for Element audit-module
    static public final String AUDIT_MODULE = "audit-module";
    //static public final String NAME = "name";
    //static public final String CLASSNAME = "classname";
    // Tags for Element message-security-config
    static public final String MESSAGE_SECURITY_CONFIG = "message-security-config";
    static public final String AUTH_LAYER = "auth-layer";
    static public final String DEFAULT_PROVIDER = "default-provider";
    static public final String DEFAULT_CLIENT_PROVIDER = "default-client-provider";
    // Tags for Element provider-config
    static public final String PROVIDER_CONFIG = "provider-config";
    static public final String PROVIDER_ID = "provider-id";
    static public final String PROVIDER_TYPE = "provider-type";
    static public final String CLASS_NAME = "class-name";
    // Tags for Element request-policy
    static public final String REQUEST_POLICY = "request-policy";
    static public final String AUTH_SOURCE = "auth-source";
    static public final String AUTH_RECIPIENT = "auth-recipient";
    // Tags for Element response-policy
    static public final String RESPONSE_POLICY = "response-policy";
    //static public final String AUTH_SOURCE = "auth-source";
    //static public final String AUTH_RECIPIENT = "auth-recipient";
    // Tags for Element jms-host
    static public final String JMS_HOST = "jms-host";
    //static public final String NAME = "name";
    static public final String HOST = "host";
    //static public final String PORT = "port";
    static public final String ADMIN_USER_NAME = "admin-user-name";
    static public final String ADMIN_PASSWORD = "admin-password";
    // Tags for Element ejb-timer-service
    static public final String EJB_TIMER_SERVICE = "ejb-timer-service";
    static public final String MINIMUM_DELIVERY_INTERVAL_IN_MILLIS = "minimum-delivery-interval-in-millis";
    static public final String MAX_REDELIVERIES = "max-redeliveries";
    static public final String TIMER_DATASOURCE = "timer-datasource";
    static public final String REDELIVERY_INTERVAL_INTERNAL_IN_MILLIS = "redelivery-interval-internal-in-millis";
    // Tags for Element session-config
    static public final String SESSION_CONFIG = "session-config";
    // Tags for Element session-manager
    static public final String SESSION_MANAGER = "session-manager";
    // Tags for Element session-properties
    static public final String SESSION_PROPERTIES = "session-properties";
    //static public final String TIMEOUT_IN_SECONDS = "timeout-in-seconds";
    // Tags for Element manager-properties
    static public final String MANAGER_PROPERTIES = "manager-properties";
    static public final String SESSION_FILE_NAME = "session-file-name";
    static public final String REAP_INTERVAL_IN_SECONDS = "reap-interval-in-seconds";
    static public final String MAX_SESSIONS = "max-sessions";
    static public final String SESSION_ID_GENERATOR_CLASSNAME = "session-id-generator-classname";
    // Tags for Element store-properties
    static public final String STORE_PROPERTIES = "store-properties";
    static public final String DIRECTORY = "directory";
    //static public final String REAP_INTERVAL_IN_SECONDS = "reap-interval-in-seconds";
    // Tags for Element das-config
    static public final String DAS_CONFIG = "das-config";
    static public final String DYNAMIC_RELOAD_ENABLED = "dynamic-reload-enabled";
    static public final String DYNAMIC_RELOAD_POLL_INTERVAL_IN_SECONDS = "dynamic-reload-poll-interval-in-seconds";
    static public final String AUTODEPLOY_ENABLED = "autodeploy-enabled";
    static public final String AUTODEPLOY_POLLING_INTERVAL_IN_SECONDS = "autodeploy-polling-interval-in-seconds";
    static public final String AUTODEPLOY_DIR = "autodeploy-dir";
    static public final String AUTODEPLOY_VERIFIER_ENABLED = "autodeploy-verifier-enabled";
    static public final String AUTODEPLOY_JSP_PRECOMPILATION_ENABLED = "autodeploy-jsp-precompilation-enabled";
    static public final String DEPLOY_XML_VALIDATION = "deploy-xml-validation";
    static public final String ADMIN_SESSION_TIMEOUT_IN_MINUTES = "admin-session-timeout-in-minutes";
    // Tags for Element orb
    //static public final String ORB = "orb";
    static public final String USE_THREAD_POOL_IDS = "use-thread-pool-ids";
    static public final String MESSAGE_FRAGMENT_SIZE = "message-fragment-size";
    static public final String MAX_CONNECTIONS = "max-connections";
    // Tags for Element ssl-client-config
    static public final String SSL_CLIENT_CONFIG = "ssl-client-config";
    // Tags for Element iiop-listener
    static public final String IIOP_LISTENER = "iiop-listener";
    static public final String ID = "id";
    //static public final String ADDRESS = "address";
    //static public final String PORT = "port";
    //static public final String SECURITY_ENABLED = "security-enabled";
    //static public final String ENABLED = "enabled";
    // Tags for Element access-log
    static public final String ACCESS_LOG = "access-log";
    static public final String FORMAT = "format";
    static public final String ROTATION_POLICY = "rotation-policy";
    static public final String ROTATION_INTERVAL_IN_MINUTES = "rotation-interval-in-minutes";
    static public final String ROTATION_SUFFIX = "rotation-suffix";
    static public final String ROTATION_ENABLED = "rotation-enabled";
    // Tags for Element http-listener
    static public final String HTTP_LISTENER = "http-listener";
    //static public final String ID = "id";
    //static public final String ADDRESS = "address";
    //static public final String PORT = "port";
    static public final String EXTERNAL_PORT = "external-port";
    static public final String FAMILY = "family";
    static public final String BLOCKING_ENABLED = "blocking-enabled";
    static public final String ACCEPTOR_THREADS = "acceptor-threads";
    //static public final String SECURITY_ENABLED = "security-enabled";
    static public final String DEFAULT_VIRTUAL_SERVER = "default-virtual-server";
    static public final String SERVER_NAME = "server-name";
    static public final String REDIRECT_PORT = "redirect-port";
    static public final String XPOWERED_BY = "xpowered-by";
    //static public final String ENABLED = "enabled";
    // Tags for Element virtual-server
    static public final String VIRTUAL_SERVER = "virtual-server";
    //static public final String ID = "id";
    static public final String HTTP_LISTENERS = "http-listeners";
    static public final String DEFAULT_WEB_MODULE = "default-web-module";
    static public final String HOSTS = "hosts";
    static public final String STATE = "state";
    static public final String DOCROOT = "docroot";
    static public final String LOG_FILE = "log-file";
    // Tags for Element request-processing
    static public final String REQUEST_PROCESSING = "request-processing";
    static public final String THREAD_COUNT = "thread-count";
    static public final String INITIAL_THREAD_COUNT = "initial-thread-count";
    static public final String THREAD_INCREMENT = "thread-increment";
    static public final String REQUEST_TIMEOUT_IN_SECONDS = "request-timeout-in-seconds";
    static public final String HEADER_BUFFER_LENGTH_IN_BYTES = "header-buffer-length-in-bytes";
    // Tags for Element keep-alive
    static public final String KEEP_ALIVE = "keep-alive";
    //static public final String THREAD_COUNT = "thread-count";
    //static public final String MAX_CONNECTIONS = "max-connections";
    //static public final String TIMEOUT_IN_SECONDS = "timeout-in-seconds";
    // Tags for Element connection-pool
    static public final String CONNECTION_POOL = "connection-pool";
    static public final String QUEUE_SIZE_IN_BYTES = "queue-size-in-bytes";
    static public final String MAX_PENDING_COUNT = "max-pending-count";
    static public final String RECEIVE_BUFFER_SIZE_IN_BYTES = "receive-buffer-size-in-bytes";
    static public final String SEND_BUFFER_SIZE_IN_BYTES = "send-buffer-size-in-bytes";
    // Tags for Element http-protocol
    static public final String HTTP_PROTOCOL = "http-protocol";
    static public final String VERSION = "version";
    static public final String DNS_LOOKUP_ENABLED = "dns-lookup-enabled";
    static public final String FORCED_RESPONSE_TYPE = "forced-response-type";
    static public final String DEFAULT_RESPONSE_TYPE = "default-response-type";
    static public final String SSL_ENABLED = "ssl-enabled";
    // Tags for Element http-file-cache
    static public final String HTTP_FILE_CACHE = "http-file-cache";
    static public final String GLOBALLY_ENABLED = "globally-enabled";
    static public final String FILE_CACHING_ENABLED = "file-caching-enabled";
    static public final String MAX_AGE_IN_SECONDS = "max-age-in-seconds";
    static public final String MEDIUM_FILE_SIZE_LIMIT_IN_BYTES = "medium-file-size-limit-in-bytes";
    static public final String MEDIUM_FILE_SPACE_IN_BYTES = "medium-file-space-in-bytes";
    static public final String SMALL_FILE_SIZE_LIMIT_IN_BYTES = "small-file-size-limit-in-bytes";
    static public final String SMALL_FILE_SPACE_IN_BYTES = "small-file-space-in-bytes";
    static public final String FILE_TRANSMISSION_ENABLED = "file-transmission-enabled";
    static public final String MAX_FILES_COUNT = "max-files-count";
    static public final String HASH_INIT_SIZE = "hash-init-size";
    // Tags for Element http-access-log
    static public final String HTTP_ACCESS_LOG = "http-access-log";
    static public final String LOG_DIRECTORY = "log-directory";
    static public final String IPONLY = "iponly";
    // Tags for Element custom-resource
    static public final String CUSTOM_RESOURCE = "custom-resource";
    //static public final String DESCRIPTION = "description";
    static public final String JNDI_NAME = "jndi-name";
    static public final String RES_TYPE = "res-type";
    static public final String FACTORY_CLASS = "factory-class";
    static public final String OBJECT_TYPE = "object-type";
    //static public final String ENABLED = "enabled";
    // Tags for Element external-jndi-resource
    static public final String EXTERNAL_JNDI_RESOURCE = "external-jndi-resource";
    //static public final String DESCRIPTION = "description";
    //static public final String JNDI_NAME = "jndi-name";
    static public final String JNDI_LOOKUP_NAME = "jndi-lookup-name";
    //static public final String RES_TYPE = "res-type";
    //static public final String FACTORY_CLASS = "factory-class";
    //static public final String OBJECT_TYPE = "object-type";
    //static public final String ENABLED = "enabled";
    static public final String DEPLOYMENT_ORDER = "deployment-order";
    // Tags for Element jdbc-resource
    static public final String JDBC_RESOURCE = "jdbc-resource";
    //static public final String DESCRIPTION = "description";
    //static public final String JNDI_NAME = "jndi-name";
    static public final String POOL_NAME = "pool-name";
    //static public final String OBJECT_TYPE = "object-type";
    //static public final String ENABLED = "enabled";
    // Tags for Element mail-resource
    static public final String MAIL_RESOURCE = "mail-resource";
    //static public final String DESCRIPTION = "description";
    //static public final String JNDI_NAME = "jndi-name";
    static public final String STORE_PROTOCOL = "store-protocol";
    static public final String STORE_PROTOCOL_CLASS = "store-protocol-class";
    static public final String TRANSPORT_PROTOCOL = "transport-protocol";
    static public final String TRANSPORT_PROTOCOL_CLASS = "transport-protocol-class";
    //static public final String HOST = "host";
    static public final String USER = "user";
    static public final String FROM = "from";
    static public final String DEBUG = "debug";
    //static public final String OBJECT_TYPE = "object-type";
    //static public final String ENABLED = "enabled";
    // Tags for Element persistence-manager-factory-resource
    static public final String PERSISTENCE_MANAGER_FACTORY_RESOURCE = "persistence-manager-factory-resource";
    //static public final String DESCRIPTION = "description";
    //static public final String JNDI_NAME = "jndi-name";
    //static public final String FACTORY_CLASS = "factory-class";
    static public final String JDBC_RESOURCE_JNDI_NAME = "jdbc-resource-jndi-name";
    //static public final String OBJECT_TYPE = "object-type";
    //static public final String ENABLED = "enabled";
    // Tags for Element admin-object-resource
    static public final String ADMIN_OBJECT_RESOURCE = "admin-object-resource";
    //static public final String DESCRIPTION = "description";
    //static public final String JNDI_NAME = "jndi-name";
    //static public final String RES_TYPE = "res-type";
    static public final String RES_ADAPTER = "res-adapter";
    //static public final String OBJECT_TYPE = "object-type";
    //static public final String ENABLED = "enabled";
    // Tags for Element connector-resource
    static public final String CONNECTOR_RESOURCE = "connector-resource";
    //static public final String DESCRIPTION = "description";
    //static public final String JNDI_NAME = "jndi-name";
    //static public final String POOL_NAME = "pool-name";
    //static public final String OBJECT_TYPE = "object-type";
    //static public final String ENABLED = "enabled";
    // Tags for Element resource-adapter-config
    static public final String RESOURCE_ADAPTER_CONFIG = "resource-adapter-config";
    //static public final String NAME = "name";
    static public final String THREAD_POOL_IDS = "thread-pool-ids";
    //static public final String OBJECT_TYPE = "object-type";
    static public final String RESOURCE_ADAPTER_NAME = "resource-adapter-name";
    // Tags for Element jdbc-connection-pool
    //static public final String JDBC_CONNECTION_POOL = "jdbc-connection-pool";
    //static public final String DESCRIPTION = "description";
    //static public final String NAME = "name";
    static public final String DATASOURCE_CLASSNAME = "datasource-classname";
    //static public final String RES_TYPE = "res-type";
    //static public final String STEADY_POOL_SIZE = "steady-pool-size";
    //static public final String MAX_POOL_SIZE = "max-pool-size";
    static public final String MAX_WAIT_TIME_IN_MILLIS = "max-wait-time-in-millis";
    //static public final String POOL_RESIZE_QUANTITY = "pool-resize-quantity";
    //static public final String IDLE_TIMEOUT_IN_SECONDS = "idle-timeout-in-seconds";
    static public final String TRANSACTION_ISOLATION_LEVEL = "transaction-isolation-level";
    static public final String IS_ISOLATION_LEVEL_GUARANTEED = "is-isolation-level-guaranteed";
    static public final String IS_CONNECTION_VALIDATION_REQUIRED = "is-connection-validation-required";
    static public final String CONNECTION_VALIDATION_METHOD = "connection-validation-method";
    static public final String VALIDATION_TABLE_NAME = "validation-table-name";
    static public final String FAIL_ALL_CONNECTIONS = "fail-all-connections";
    static public final String NON_TRANSACTIONAL_CONNECTIONS = "non-transactional-connections";
    static public final String ALLOW_NON_COMPONENT_CALLERS = "allow-non-component-callers";
    static public final String VALIDATE_ATMOST_ONCE_PERIOD_IN_SECONDS = "validate-atmost-once-period-in-seconds";
    static public final String CONNECTION_LEAK_TIMEOUT_IN_SECONDS = "connection-leak-timeout-in-seconds";
    static public final String CONNECTION_LEAK_RECLAIM = "connection-leak-reclaim";
    static public final String CONNECTION_CREATION_RETRY_ATTEMPTS = "connection-creation-retry-attempts";
    static public final String CONNECTION_CREATION_RETRY_INTERVAL_IN_SECONDS = "connection-creation-retry-interval-in-seconds";
    static public final String STATEMENT_TIMEOUT_IN_SECONDS = "statement-timeout-in-seconds";
    static public final String LAZY_CONNECTION_ENLISTMENT = "lazy-connection-enlistment";
    static public final String LAZY_CONNECTION_ASSOCIATION = "lazy-connection-association";
    static public final String ASSOCIATE_WITH_THREAD = "associate-with-thread";
    static public final String MATCH_CONNECTIONS = "match-connections";
    static public final String MAX_CONNECTION_USAGE_COUNT = "max-connection-usage-count";
    static public final String WRAP_JDBC_OBJECTS = "wrap-jdbc-objects";
    // Tags for Element connector-connection-pool
    //static public final String CONNECTOR_CONNECTION_POOL = "connector-connection-pool";
    //static public final String DESCRIPTION = "description";
    //static public final String NAME = "name";
    //static public final String RESOURCE_ADAPTER_NAME = "resource-adapter-name";
    static public final String CONNECTION_DEFINITION_NAME = "connection-definition-name";
    //static public final String STEADY_POOL_SIZE = "steady-pool-size";
    //static public final String MAX_POOL_SIZE = "max-pool-size";
    //static public final String MAX_WAIT_TIME_IN_MILLIS = "max-wait-time-in-millis";
    //static public final String POOL_RESIZE_QUANTITY = "pool-resize-quantity";
    //static public final String IDLE_TIMEOUT_IN_SECONDS = "idle-timeout-in-seconds";
    //static public final String FAIL_ALL_CONNECTIONS = "fail-all-connections";
    static public final String TRANSACTION_SUPPORT = "transaction-support";
    //static public final String IS_CONNECTION_VALIDATION_REQUIRED = "is-connection-validation-required";
    //static public final String VALIDATE_ATMOST_ONCE_PERIOD_IN_SECONDS = "validate-atmost-once-period-in-seconds";
    //static public final String CONNECTION_LEAK_TIMEOUT_IN_SECONDS = "connection-leak-timeout-in-seconds";
    //static public final String CONNECTION_LEAK_RECLAIM = "connection-leak-reclaim";
    //static public final String CONNECTION_CREATION_RETRY_ATTEMPTS = "connection-creation-retry-attempts";
    //static public final String CONNECTION_CREATION_RETRY_INTERVAL_IN_SECONDS = "connection-creation-retry-interval-in-seconds";
    //static public final String LAZY_CONNECTION_ENLISTMENT = "lazy-connection-enlistment";
    //static public final String LAZY_CONNECTION_ASSOCIATION = "lazy-connection-association";
    //static public final String ASSOCIATE_WITH_THREAD = "associate-with-thread";
    //static public final String MATCH_CONNECTIONS = "match-connections";
    //static public final String MAX_CONNECTION_USAGE_COUNT = "max-connection-usage-count";
    // Tags for concurrent resources elements
    static public final String CONTEXT_SERVICE = "context-service";
    static public final String MANAGED_THREAD_FACTORY = "managed-thread-factory";
    static public final String MANAGED_EXECUTOR_SERVICE = "managed-executor-service";
    static public final String MANAGED_SCHEDULED_EXECUTOR_SERVICE = "managed-scheduled-executor-service";
    // Tags for Element security-map
    static public final String SECURITY_MAP = "security-map";
    static public final String PRINCIPAL = "principal";
    static public final String USER_GROUP = "user-group";
    //static public final String NAME = "name";
    // Tags for Element backend-principal
    static public final String BACKEND_PRINCIPAL = "backend-principal";
    static public final String USER_NAME = "user-name";
    static public final String PASSWORD = "password";
    // Tags for Element lifecycle-module
    static public final String LIFECYCLE_MODULE = "lifecycle-module";
    //static public final String DESCRIPTION = "description";
    //static public final String NAME = "name";
    //static public final String CLASS_NAME = "class-name";
    //static public final String CLASSPATH = "classpath";
    static public final String LOAD_ORDER = "load-order";
    static public final String IS_FAILURE_FATAL = "is-failure-fatal";
    //static public final String OBJECT_TYPE = "object-type";
    //static public final String ENABLED = "enabled";
    public static final String IS_LIFECYCLE = "isLifecycle";
    public static final String LOAD_SYSTEM_APP_ON_STARTUP = 
        "load-system-app-on-startup";
    // Tags for Element j2ee-application
    public static final String DEFAULT_APP_NAME = "defaultAppName";
    public static final String IS_COMPOSITE = "isComposite";
    public static final String APP_CONFIG = "appConfig";
    static public final String J2EE_APPLICATION = "j2ee-application";
    //static public final String DESCRIPTION = "description";
    //static public final String NAME = "name";
    static public final String LOCATION = "location";
    //static public final String OBJECT_TYPE = "object-type";
    //static public final String ENABLED = "enabled";
    static public final String LIBRARIES = "libraries";
    //static public final String AVAILABILITY_ENABLED = "availability-enabled";
    static public final String DIRECTORY_DEPLOYED = "directory-deployed";
    static public final String JAVA_WEB_START_ENABLED = "java-web-start-enabled";
    // Tags for Element ejb-module
    static public final String EJB_MODULE = "ejb-module";
    //static public final String DESCRIPTION = "description";
    //static public final String NAME = "name";
    //static public final String LOCATION = "location";
    //static public final String OBJECT_TYPE = "object-type";
    //static public final String ENABLED = "enabled";
    //static public final String LIBRARIES = "libraries";
    //static public final String AVAILABILITY_ENABLED = "availability-enabled";
    //static public final String DIRECTORY_DEPLOYED = "directory-deployed";
    // Tags for Element web-module
    static public final String WEB_MODULE = "web-module";
    //static public final String DESCRIPTION = "description";
    //static public final String NAME = "name";
    static public final String CONTEXT_ROOT = "context-root";
    //static public final String LOCATION = "location";
    //static public final String OBJECT_TYPE = "object-type";
    //static public final String ENABLED = "enabled";
    //static public final String LIBRARIES = "libraries";
    //static public final String AVAILABILITY_ENABLED = "availability-enabled";
    //static public final String DIRECTORY_DEPLOYED = "directory-deployed";
    // Tags for Element connector-module
    static public final String CONNECTOR_MODULE = "connector-module";
    //static public final String DESCRIPTION = "description";
    //static public final String NAME = "name";
    //static public final String LOCATION = "location";
    //static public final String OBJECT_TYPE = "object-type";
    //static public final String ENABLED = "enabled";
    //static public final String DIRECTORY_DEPLOYED = "directory-deployed";
    // Tags for Element appclient-module
    static public final String APPCLIENT_MODULE = "appclient-module";
    //static public final String DESCRIPTION = "description";
    //static public final String NAME = "name";
    //static public final String LOCATION = "location";
    //static public final String DIRECTORY_DEPLOYED = "directory-deployed";
    //static public final String JAVA_WEB_START_ENABLED = "java-web-start-enabled";
    // Tags for Element mbean
    static public final String MBEAN = "mbean";
    //static public final String DESCRIPTION = "description";
    //static public final String NAME = "name";
    //static public final String OBJECT_TYPE = "object-type";
    static public final String IMPL_CLASS_NAME = "impl-class-name";
    static public final String OBJECT_NAME = "object-name";
    //static public final String ENABLED = "enabled";
    // Tags for Element extension-module
    static public final String EXTENSION_MODULE = "extension-module";
    //static public final String DESCRIPTION = "description";
    //static public final String NAME = "name";
    //static public final String LOCATION = "location";
    static public final String MODULE_TYPE = "module-type";
    //static public final String OBJECT_TYPE = "object-type";
    //static public final String ENABLED = "enabled";
    //static public final String LIBRARIES = "libraries";
    //static public final String AVAILABILITY_ENABLED = "availability-enabled";
    //static public final String DIRECTORY_DEPLOYED = "directory-deployed";
    // Tags for Element web-service-endpoint
    static public final String WEB_SERVICE_ENDPOINT = "web-service-endpoint";
    //static public final String NAME = "name";
    static public final String MONITORING = "monitoring";
    static public final String MAX_HISTORY_SIZE = "max-history-size";
    static public final String JBI_ENABLED = "jbi-enabled";
    // Tags for Element registry-location
    static public final String REGISTRY_LOCATION = "registry-location";
    static public final String CONNECTOR_RESOURCE_JNDI_NAME = "connector-resource-jndi-name";
    // Tags for Element transformation-rule
    static public final String TRANSFORMATION_RULE = "transformation-rule";
    //static public final String NAME = "name";
    //static public final String ENABLED = "enabled";
    static public final String APPLY_TO = "apply-to";
    static public final String RULE_FILE_LOCATION = "rule-file-location";
    //static public final String DESCRIPTION = "description";
    //static public final String NODE_AGENT_REF = "node-agent-ref";
    //static public final String CONFIG_REF = "config-ref";
    
    public static final String ADMIN_CONSOLE_CONTEXT_ROOT	= "adminConsoleContextRoot";
    public static final String ADMIN_CONSOLE_DOWNLOAD_LOCATION	= "adminConsoleDownloadLocation";
    public static final String IPS_ROOT				= "ipsRoot";
    public static final String ADMIN_CONSOLE_VERSION		= "adminConsoleVersion";
    public static final String ADMIN_LISTENER_ID = "admin-listener";
    public final static String SEC_ADMIN_LISTENER_PROTOCOL_NAME = "sec-admin-listener";
    public final static String REDIRECT_PROTOCOL_NAME = "admin-http-redirect";
    public final static String PORT_UNIF_PROTOCOL_NAME = "pu-protocol";
}

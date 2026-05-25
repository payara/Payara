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
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright 2016-2026 Payara Foundation and/or its affiliates

package com.sun.enterprise.deployment.xml;

/**
 * I hold the tag names of the runtime binding information of a J2EE
 * application.
 * @author Danny Coward
 */

public interface RuntimeTagNames extends TagNames {

    String S1AS_EJB_RUNTIME_TAG = "sun-ejb-jar";
    String S1AS_APPCLIENT_RUNTIME_TAG = "sun-application-client";
    String S1AS_APPLICATION_RUNTIME_TAG = "sun-application";
    String S1AS_WEB_RUNTIME_TAG = "sun-web-app";
    String S1AS_CONNECTOR_RUNTIME_TAG = "sun-connector";

    String GF_EJB_RUNTIME_TAG = "glassfish-ejb-jar";
    String GF_APPCLIENT_RUNTIME_TAG = "glassfish-application-client";
    String GF_APPLICATION_RUNTIME_TAG = "glassfish-application";
    String GF_WEB_RUNTIME_TAG = "glassfish-web-app";

    String PAYARA_CLASSLOADING_DELEGATE = "classloading-delegate";
    String PAYARA_ENABLE_IMPLICIT_CDI = "enable-implicit-cdi";
    String PAYARA_SCANNING_EXCLUDE = "scanning-exclude";
    String PAYARA_SCANNING_INCLUDE = "scanning-include";
    String PAYARA_WHITELIST_PACKAGE = "whitelist-package";
    String PAYARA_JAXRS_ROLES_ALLOWED_ENABLED = "jaxrs-roles-allowed-enabled";
    String PAYARA_APPLICATION_RUNTIME_TAG = "payara-application";
    String PAYARA_WEB_RUNTIME_TAG = "payara-web-app";
    String PAYARA_EJB_RUNTIME_TAG = "payara-ejb-jar";
    String PAYARA_APPCLIENT_RUNTIME_TAG = "payara-application-client";

    // The name of the deployment context property used to disable implicit bean discovery for a
    // particular application deployment.
    String IMPLICIT_CDI_ENABLED_PROP = "implicitCdiEnabled";
    String PAYARA_CLUSTERED_BEAN = "clustered-bean";
    String PAYARA_CLUSTERED_KEY_NAME = "clustered-key-name";
    String PAYARA_CLUSTERED_LOCK_TYPE = "clustered-lock-type";
    String PAYARA_CLUSTERED_POSTCONSTRUCT_ON_ATTACH = "clustered-attach-postconstruct";
    String PAYARA_CLUSTERED_PREDESTROY_ON_DETTACH = "clustered-detach-predestroy";

    String AS_CONTEXT = "as-context";
    String AUTH_METHOD = "auth-method";
    String CALLER_PROPAGATION = "caller-propagation";
    String CONFIDENTIALITY = "confidentiality";
    String DURABLE_SUBSCRIPTION = "jms-durable-subscription-name";
    String ESTABLISH_TRUST_IN_CLIENT = "establish-trust-in-client";
    String ESTABLISH_TRUST_IN_TARGET = "establish-trust-in-target";
    String INTEGRITY = "integrity";
    String IOR_CONFIG = "ior-security-config";
    String MDB_CONNECTION_FACTORY = "mdb-connection-factory";
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

    String DEFAULT_RESOURCE_PRINCIPAL = "default-resource-principal";
    String EJB = "ejb";
    String EJB_NAME = "ejb-name";
    String EJBS = "enterprise-beans";
    String WEBSERVICE_DEFAULT_LOGIN_CONFIG = "webservice-default-login-config";
    String FIELD = "field";

    String GROUP = "group";
    String JNDI_NAME = "jndi-name";
    String MAIL_FROM = "mail-from";
    String MAIL_HOST = "mail-host";
    String METHOD = "method";
    String NAME = "name";

    String PASSWORD = "password";
    String PRINCIPAL = "principal";

    String SERVLET = "servlet";
    String SERVLET_NAME = "servlet-name";
    String SOURCE = "source";
    String SQL = "sql";


    String UNIQUE_ID = "unique-id";
    String WEB = "web";


    String REMOTE_IMPL = "remote-impl";
    String LOCAL_IMPL = "local-impl";
    String REMOTE_HOME_IMPL = "remote-home-impl";
    String LOCAL_HOME_IMPL = "local-home-impl";
    String GEN_CLASSES = "gen-classes";

    // acceptable values
    String TRUE = "true";
    String FALSE = "false";

    // SECURITY related
    String SECURITY_ROLE_MAPPING = "security-role-mapping";
    String ROLE_NAME = "role-name";
    String PRINCIPAL_NAME = "principal-name";
    String GROUP_NAME = "group-name";

    // common
    String EJB_REF = "ejb-ref";
    String RESOURCE_REF = "resource-ref";
    String RESOURCE_ENV_REF = "resource-env-ref";

    // S1AS specific
    String PASS_BY_REFERENCE = "pass-by-reference";
    String JMS_MAX_MESSAGES_LOAD = "jms-max-messages-load";
    String IS_READ_ONLY_BEAN = "is-read-only-bean";
    String REFRESH_PERIOD_IN_SECONDS = "refresh-period-in-seconds";
    String COMMIT_OPTION = "commit-option";
    String CMT_TIMEOUT_IN_SECONDS = "cmt-timeout-in-seconds";
    String USE_THREAD_POOL_ID = "use-thread-pool-id";
    String AVAILABILITY_ENABLED = "availability-enabled";
    String DISABLE_NONPORTABLE_JNDI_NAMES = "disable-nonportable-jndi-names";
    String PER_REQUEST_LOAD_BALANCING = "per-request-load-balancing";

    // BEAN-POOL related
    String BEAN_POOL = "bean-pool";
    String STEADY_POOL_SIZE = "steady-pool-size";
    String POOL_RESIZE_QUANTITY = "resize-quantity";
    String MAX_POOL_SIZE = "max-pool-size";
    String POOL_IDLE_TIMEOUT_IN_SECONDS = "pool-idle-timeout-in-seconds";

    // BEAN-CACHE related
    String BEAN_CACHE = "bean-cache";
    String MAX_CACHE_SIZE = "max-cache-size";
    String RESIZE_QUANTITY = "resize-quantity";
    String CACHE_IDLE_TIMEOUT_IN_SECONDS = "cache-idle-timeout-in-seconds";
    String REMOVAL_TIMEOUT_IN_SECONDS = "removal-timeout-in-seconds";
    String VICTIM_SELECTION_POLICY = "victim-selection-policy";

    // thread-pool related
    String THREAD_CORE_POOL_SIZE = "thread-core-pool-size";
    String THREAD_MAX_POOL_SIZE  = "thread-max-pool-size";
    String THREAD_KEEP_ALIVE_SECONDS = "thread-keep-alive-seconds";
    String THREAD_QUEUE_CAPACITY = "thread-queue-capacity";
    String ALLOW_CORE_THREAD_TIMEOUT = "allow-core-thread-timeout";
    String PRESTART_ALL_CORE_THREADS = "prestart-all-core-threads";

    // flush-at-end-of-method
    String FLUSH_AT_END_OF_METHOD =
        "flush-at-end-of-method";
    // checkpoint-at-end-of-method, equivalent element of
    // checkpointed-methods in 8.1 and later releases
    String CHECKPOINT_AT_END_OF_METHOD =
        "checkpoint-at-end-of-method";

    String QUERY_METHOD = "query-method";

    // Connector related
    String RESOURCE_ADAPTER = "resource-adapter";
    String ROLE_MAP = "role-map";
    String IDLE_TIMEOUT_IN_SECONDS = "idle-timeout-in-seconds";
    String PROPERTY = "property";
    String MAP_ELEMENT = "map-element";
    String MAP_ID = "map-id";
    String BACKEND_PRINCIPAL = "backend-principal";
    String USER_NAME = "user-name";
    String CREDENTIAL = "credential";

    // application related
    String WEB_URI = "web-uri";
    String CONTEXT_ROOT = "context-root"; // also used in java web start support
    String ARCHIVE_NAME = "archive-name";
    String COMPATIBILITY = "compatibility";
    String KEEP_STATE = "keep-state";
    String VERSION_IDENTIFIER = "version-identifier";
    String APPLICATION_PARAM = "application-param";
    String MODULE = "module";
    String TYPE = "type";
    String PATH = "path";

    // Web
    String CACHE_MAPPING = "cache-mapping";
    String CACHE_HELPER = "cache-helper";
    String CACHE_HELPER_REF = "cache-helper-ref";
    String CLASS_NAME = "class-name";
    String COOKIE_PROPERTIES = "cookie-properties";
    String CONSTRAINT_FIELD = "constraint-field";
    String CONSTRAINT_FIELD_VALUE = "constraint-field-value";
    String DEFAULT_HELPER = "default-helper";
    String MAX_ENTRIES = "max-entries";
    String TIMEOUT_IN_SECONDS = "timeout-in-seconds";
    String ENABLED = "enabled";
    String PARAMETER_ENCODING = "parameter-encoding";
    String FORM_HINT_FIELD = "form-hint-field";
    String DEFAULT_CHARSET = "default-charset";
    String STORE_PROPERTIES = "store-properties";
    String MANAGER_PROPERTIES = "manager-properties";
    String REFRESH_FIELD = "refresh-field";
    String SESSION_MANAGER = "session-manager";
    String SESSION_PROPERTIES = "session-properties";
    String SESSION_CONFIG = "session-config";
    String TIMEOUT = "timeout";
    String PERSISTENCE_TYPE = "persistence-type";
    String JSP_CONFIG = "jsp-config";
    String CLASS_LOADER = "class-loader";
    String EXTRA_CLASS_PATH = "extra-class-path";
    String DELEGATE = "delegate";
    String DYNAMIC_RELOAD_INTERVAL =
        "dynamic-reload-interval";
    String CACHE = "cache";
    String KEY_FIELD = "key-field";
    String URL_PATTERN = "url-pattern";
    String HTTP_METHOD = "http-method";
    String DISPATCHER = "dispatcher";
    String SCOPE = "scope";
    String CACHE_ON_MATCH = "cache-on-match";
    String CACHE_ON_MATCH_FAILURE = "cache-on-match-failure";
    String MATCH_EXPR = "match-expr";
    String VALUE = "value";
    String IDEMPOTENT_URL_PATTERN = "idempotent-url-pattern";
    String ERROR_URL = "error-url";
    String HTTPSERVLET_SECURITY_PROVIDER = "httpservlet-security-provider";
    String NUM_OF_RETRIES = "num-of-retries";

    String JAVA_METHOD = "java-method";
    String METHOD_PARAMS = "method-params";
    String METHOD_PARAM = "method-param";

    String VALVE = "valve";

    // Java Web Start-support related
    String JAVA_WEB_START_ACCESS = "java-web-start-access";
    String ELIGIBLE = "eligible";
    String VENDOR = "vendor";
    String JNLP_DOC = "jnlp-doc";
    // also uses CONTEXT_ROOT defined above in the application-related section
}

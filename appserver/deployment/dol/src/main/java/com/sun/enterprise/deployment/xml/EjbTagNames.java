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

package com.sun.enterprise.deployment.xml;
 
/** 
 * The XML tag names for the ejb-jar dtd
 * @author Danny Coward
 */

public interface EjbTagNames extends TagNames {

    public final static String SESSION = "session";
    public final static String ENTITY = "entity";
    public final static String SESSION_TYPE = "session-type";
    public final static String MESSAGE_DRIVEN = "message-driven";

    public final static String EJB_NAME = "ejb-name";
    public final static String HOME = "home";
    public final static String REMOTE = "remote";
    public final static String LOCAL_HOME = "local-home";
    public final static String LOCAL = "local";
    public final static String LOCAL_BEAN = "local-bean";
    public final static String BUSINESS_LOCAL = "business-local";
    public final static String BUSINESS_REMOTE = "business-remote";
    public final static String EJB_CLASS = "ejb-class";
    public final static String SERVICE_ENDPOINT_INTERFACE = "service-endpoint";
    public final static String ROLE_REFERENCES = "security-role-refs";
    
    // entity
    public final static String PERSISTENCE_TYPE = "persistence-type";
    public final static String PRIMARY_KEY_CLASS = "prim-key-class";
    public final static String PRIMARY_KEY_FIELD = "primkey-field";
    public final static String REENTRANT = "reentrant";
    public final static String PERSISTENT_FIELDS = "persistent-fields";
    public final static String CMP_FIELD = "cmp-field";
    public final static String CMP_VERSION = "cmp-version";
    public final static String CMP_2_VERSION = "2.x";
    public final static String CMP_1_VERSION = "1.x";
    public final static String FIELD_NAME = "field-name";
    public final static String ABSTRACT_SCHEMA_NAME = "abstract-schema-name";

    // relationships
    public final static String RELATIONSHIPS = "relationships";
    public final static String EJB_RELATION = "ejb-relation";
    public final static String EJB_RELATION_NAME = "ejb-relation-name";
    public final static String EJB_RELATIONSHIP_ROLE = "ejb-relationship-role";
    public final static String EJB_RELATIONSHIP_ROLE_NAME = "ejb-relationship-role-name";
    public final static String MULTIPLICITY = "multiplicity";
    public final static String RELATIONSHIP_ROLE_SOURCE = "relationship-role-source";
    public final static String CMR_FIELD = "cmr-field";
    public final static String CMR_FIELD_NAME = "cmr-field-name";
    public final static String CMR_FIELD_TYPE = "cmr-field-type";
    public final static String CASCADE_DELETE = "cascade-delete";

    // application exceptions
    public final static String APPLICATION_EXCEPTION = "application-exception";
    public final static String APP_EXCEPTION_CLASS = "exception-class";
    public final static String APP_EXCEPTION_ROLLBACK = "rollback";
    public final static String APP_EXCEPTION_INHERITED = "inherited";

    // ejb-entity-ref
    public final static String REMOTE_EJB_NAME = "remote-ejb-name";

    // query
    public final static String QUERY = "query";
    public final static String QUERY_METHOD = "query-method";
    public final static String EJB_QL = "ejb-ql";
    public final static String QUERY_RESULT_TYPE_MAPPING = "result-type-mapping";
    public final static String QUERY_REMOTE_TYPE_MAPPING = "Remote";
    public final static String QUERY_LOCAL_TYPE_MAPPING = "Local";

    // session
    public final static String TRANSACTION_TYPE = "transaction-type";
    public final static String TRANSACTION_SCOPE = "transaction-scope";
    public final static String CONCURRENCY_MANAGEMENT_TYPE = "concurrency-management-type";
    public final static String ASYNC_METHOD = "async-method";
    public final static String CONCURRENT_METHOD = "concurrent-method";
    public final static String CONCURRENT_LOCK = "lock";
    public final static String CONCURRENT_ACCESS_TIMEOUT = "access-timeout";
    
    

    // message-driven 
    public final static String ACTIVATION_CONFIG = "activation-config";
    public final static String ACTIVATION_CONFIG_PROPERTY = 
        "activation-config-property";
    public final static String ACTIVATION_CONFIG_PROPERTY_NAME = 
        "activation-config-property-name";
    public final static String ACTIVATION_CONFIG_PROPERTY_VALUE = 
        "activation-config-property-value";
    public final static String MESSAGING_TYPE = "messaging-type";
    public final static String MSG_SELECTOR = "message-selector";
    public final static String JMS_ACKNOWLEDGE_MODE = "acknowledge-mode";
    public final static String MESSAGE_DRIVEN_DEST = "message-driven-destination";
    public final static String JMS_SUBSCRIPTION_DURABILITY = "subscription-durability";
    public final static String JMS_SUBSCRIPTION_IS_DURABLE = "Durable";
    public final static String JMS_SUBSCRIPTION_NOT_DURABLE = "NonDurable";
    public final static String JMS_AUTO_ACK_MODE = "Auto-acknowledge";
    public final static String JMS_DUPS_OK_ACK_MODE  = "Dups-ok-acknowledge";
    public final static String JMS_DEST_TYPE = "destination-type";

    public final static String JNDI_NAME = "jndi-name";
    
    public final static String EJB_BUNDLE_TAG = "ejb-jar";
    public final static String EJBS = "enterprise-beans";
    public final static String ASSEMBLY_DESCRIPTOR = "assembly-descriptor";
    public final static String METHOD_PERMISSION = "method-permission";
    public final static String UNCHECKED = "unchecked";
    public final static String EXCLUDE_LIST = "exclude-list";
    public final static String METHOD = "method";
    public final static String METHOD_NAME = "method-name";
    public final static String METHOD_INTF = "method-intf";
    public final static String METHOD_PARAMS = "method-params";
    public final static String METHOD_PARAM = "method-param";
    public final static String CONTAINER_TRANSACTION = "container-transaction";
    public final static String TRANSACTION_ATTRIBUTE = "trans-attribute";
    
    public final static String EJB_CLIENT_JAR = "ejb-client-jar";

    // security-identity
    public final static String SECURITY_IDENTITY = "security-identity";
    public final static String USE_CALLER_IDENTITY = "use-caller-identity";

    // interceptors
    public final static String INTERCEPTOR = "interceptor";
    public final static String INTERCEPTORS = "interceptors";
    public final static String INTERCEPTOR_BINDING = "interceptor-binding";
    public final static String INTERCEPTOR_CLASS = "interceptor-class";
    public final static String INTERCEPTOR_ORDER = "interceptor-order";
    public final static String INTERCEPTOR_BUSINESS_METHOD = "method";
    public final static String EXCLUDE_DEFAULT_INTERCEPTORS = 
        "exclude-default-interceptors";
    public final static String EXCLUDE_CLASS_INTERCEPTORS = 
        "exclude-class-interceptors";
    public final static String AROUND_INVOKE_METHOD = "around-invoke";

    // around-invoke
    public static final String AROUND_INVOKE_CLASS_NAME = "class";
    public static final String AROUND_INVOKE_METHOD_NAME = "method-name";

    // around-invoke
    public final static String AROUND_TIMEOUT_METHOD = "around-timeout";

    // stateful session
    public final static String POST_ACTIVATE_METHOD = "post-activate";
    public final static String PRE_PASSIVATE_METHOD = "pre-passivate";
    public final static String INIT_METHOD = "init-method";
    public final static String INIT_CREATE_METHOD = "create-method";
    public final static String INIT_BEAN_METHOD = "bean-method";
    public final static String REMOVE_METHOD = "remove-method";
    public final static String REMOVE_BEAN_METHOD = "bean-method";
    public final static String REMOVE_RETAIN_IF_EXCEPTION = "retain-if-exception";
    public final static String STATEFUL_TIMEOUT = "stateful-timeout";
    public final static String AFTER_BEGIN_METHOD = "after-begin-method";
    public final static String BEFORE_COMPLETION_METHOD = "before-completion-method";
    public final static String AFTER_COMPLETION_METHOD = "after-completion-method";


    // singleton
    public final static String INIT_ON_STARTUP = "init-on-startup";
    public final static String DEPENDS_ON = "depends-on";

    // Common timeout value elements for STATEFUL_TIMEOUT, ACCESS_TIMEOUT
    public final static String TIMEOUT_VALUE = "timeout";
    public final static String TIMEOUT_UNIT = "unit";


    // timeout method
    public final static String TIMEOUT_METHOD = "timeout-method";
    public final static String TIMER_SCHEDULE = "schedule";

    public final static String TIMER = "timer";
    public final static String TIMER_START = "start";
    public final static String TIMER_END = "end";
    public final static String TIMER_PERSISTENT = "persistent";
    public final static String TIMER_TIMEZONE = "timezone";
    public final static String TIMER_INFO = "info";
    public final static String TIMER_SECOND = "second";
    public final static String TIMER_MINUTE = "minute";
    public final static String TIMER_HOUR = "hour";
    public final static String TIMER_DAY_OF_MONTH = "day-of-month";
    public final static String TIMER_MONTH = "month";
    public final static String TIMER_DAY_OF_WEEK = "day-of-week";
    public final static String TIMER_YEAR = "year";




}


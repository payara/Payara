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

package org.glassfish.resources.api;

/**
 * @author Jagadish Ramu
 */
public interface ResourceConstants {

    /**
     * Constant to denote external jndi resource type.
     */
    public static final String RES_TYPE_EXTERNAL_JNDI = "external-jndi";

    public static final String RES_TYPE_JDBC = "jdbc";

    /**
     * Constant to denote jdbc connection pool resource type.
     */
    public static final String RES_TYPE_JCP = "jcp";

    /**
     * Constant to denote connector connection pool  resource type.
     */
    public static final String RES_TYPE_CCP = "ccp";

    /**
     * Constant to denote connector resource type.
     */
    public static final String RES_TYPE_CR = "cr";

    /**
     * Constant to denote custom resource type.
     */
    public static final String RES_TYPE_CUSTOM = "custom";

    /**
     * Constant to denote admin object resource type.
     */
    public static final String RES_TYPE_AOR = "aor";

    /**
     * Constant to denote resource adapter config type.
     */
    public static final String RES_TYPE_RAC = "rac";

    /**
     * Constant to denote connector-work-security-map type.
     */
    public static final String RES_TYPE_CWSM = "cwsm";

    /**
     * Constant to denote mail resource type.
     */
    public static final String RES_TYPE_MAIL = "mail";

    /**
     * Represents the glassfish-resources.xml handling module name / type for .ear
     */
    public static final String GF_RESOURCES_MODULE_EAR = "resources_ear";

    /**
     * Represents the glassfish-resources.xml handling module name / type for standalone application
     */
    public static final String GF_RESOURCES_MODULE = "resources";

    /**
     * Represents the location where glassfish-resources.xml will be present in an archive
     */
    public static final String GF_RESOURCES_LOCATION ="META-INF/glassfish-resources.xml";

    /** resource type residing in an external JNDI repository */
    public static final String EXT_JNDI_RES_TYPE = "external-jndi-resource";

    public static final String JMS_QUEUE = "javax.jms.Queue";
    public static final String JMS_TOPIC = "javax.jms.Topic";
    public static final String JMS_QUEUE_CONNECTION_FACTORY = "javax.jms.QueueConnectionFactory";
    public static final String JMS_TOPIC_CONNECTION_FACTORY = "javax.jms.TopicConnectionFactory";
    public static final String JMS_MESSAGE_LISTENER = "javax.jms.MessageListener";

    //TODO should be refactored to non-resources module
    /**
     *  Reserved sub-context where datasource-definition objets (resource and pool) are bound with generated names.
     */
    public static String DATASOURCE_DEFINITION_JNDINAME_PREFIX="__datasource_definition/";
    public static String MAILSESSION_DEFINITION_JNDINAME_PREFIX="__mailsession_definition/";
    public static String CONNECTION_FACTORY_DEFINITION_JNDINAME_PREFIX="__connection_factory_definition/";
    public static String JMS_CONNECTION_FACTORY_DEFINITION_JNDINAME_PREFIX = "__jms_connection_factory_definition/";
    public static String JMS_DESTINATION_DEFINITION_JNDINAME_PREFIX = "__jms_destination_definition/";
    public static String ADMINISTERED_OBJECT_DEFINITION_JNDINAME_PREFIX="__administered_object_definition/";

    public static final String JAVA_SCOPE_PREFIX = "java:";
    public static final String JAVA_APP_SCOPE_PREFIX = "java:app/";
    public static final String JAVA_COMP_SCOPE_PREFIX = "java:comp/";
    public static final String JAVA_MODULE_SCOPE_PREFIX = "java:module/";
    public static final String JAVA_GLOBAL_SCOPE_PREFIX = "java:global/";

    public static enum TriState {
        TRUE, FALSE, UNKNOWN
    }

    public final static String CONNECTOR_RESOURCES = "CONNECTOR";
    public final static String NON_CONNECTOR_RESOURCES = "NON-CONNECTOR";

    /**
     * Token used for generating the name to refer to the embedded rars.
     * It will be AppName+EMBEDDEDRAR_NAME_DELIMITER+embeddedRarName.
     */

    public static String EMBEDDEDRAR_NAME_DELIMITER="#";

    public final static String APP_META_DATA_RESOURCES = "app-level-resources-config";
    public final static String APP_SCOPED_RESOURCES_MAP = "app-scoped-resources-map";

}

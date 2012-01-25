/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.paas.gfplugin;

import com.sun.enterprise.util.OS;

import java.io.File;
import java.text.MessageFormat;

/**
 * @author Bhavanishankar S
 */

public interface GlassFishPluginConstants {

    /**
     * Service configuration related.
     */
    public String SERVICE_TYPE = "service-type";

    public String SERVICE_NAME = "service-name";

    public String JAVAEE_SERVICE_TYPE = "JavaEE";

    public String INIT_TYPE_LAZY = "lazy";

    public String MIN_CLUSTERSIZE = "min.clustersize";

    public String MAX_CLUSTERSIZE = "max.clustersize";

    public String DEFAULT_MIN_CLUSTERSIZE = "2";

    public String DEFAULT_MAX_CLUSTERSIZE = "4";

    /**
     * JDBC related
     */
    public String DATABASE_SERVICE_TYPE = "Database";

    public String JDBC_RESOURCE = "jdbc-resource";

    public String JDBC_CONNECTION_POOL = "jdbc-connection-pool";

    public String JDBC_SERVERNAME = "serverName";

    public String JDBC_URL = "URL";

    public String POOL_NAME = "pool-name";

    public String JNDI_NAME = "jndi-name";

    public String JDBC_DATASOURCE = "javax.sql.DataSource";
    
    public String JDBC_DS_CLASSNAME = "datasource-classname";

    public String JDBC_DS_RESTYPE = "res-type";

    /**
     * Temporary constants
     */
    public String FS = File.separator;

    public String RESOURCE_XML_PARSERS = "resourceXmlParsers";

    public String NON_CONNECTOR_RESOURCES = "nonConnectorResources";

    public String TMR_DIR = "java.io.tmpdir";

    public String DEPLOYMENT_PLAN_DIR = FS + "deployment_plan";

    public String JAR_EXTN = ".jar"; // used for deployment plan generation.

    public String HOST = "host";

    public String LOCALHOST = "localhost";

    /**
     * asadmin commands
     */
    public String START_CLUSTER = "start-cluster";

    public String STOP_CLUSTER = "stop-cluster";

    public String CREATE_ELASTIC_SERVICE = "_create-elastic-service";

    public String DELETE_ELASTIC_SERVICE = "_delete-elastic-service";

    public String ENABLE_AUTO_SCALING = "enable-auto-scaling";

    public String DISABLE_AUTO_SCALING = "disable-auto-scaling";

    /**
     * asadmin commands used in customizer
     */
    public String CREATE_NODE_SSH = "create-node-ssh";

    public String DELETE_NODE_SSH = "delete-node-ssh";

    public String CREATE_INSTANCE = "create-instance";

    public String DELETE_INSTANCE = "delete-instance";

    public String START_INSTANCE = "start-instance";

    public String STOP_INSTANCE = "stop-instance";

    public String CREATE_LOCAL_INSTANCE = "create-local-instance";

    public String START_LOCAL_INSTANCE = "start-local-instance";

    /**
     * asadmin command arguments used in customizer.
     */
    public String NODE_HOST_ARG = "nodehost";

    public String NODE_ARG = "node";

    public String SSH_USER_ARG = "sshUser";

    public String INSTALL_DIR_ARG = "installdir";

    public String CLUSTER_ARG = "cluster";

    public String VM_SHUTDOWN_ARG = "_vmShutdown";

    /**
     * Other constants used in customizer.
     */
    public String PLAIN_ACTION_REPORT = "plain";

    public String NODE_TYPE_SSH = "SSH";

    public MessageFormat NODE_NAME_FORMAT =
            new MessageFormat("{0}_{1}_{2}"); // poolName_machineName_vmName

    public MessageFormat INSTANCE_NAME_FORMAT = new MessageFormat(
            NODE_NAME_FORMAT.toPattern() + "Instance"); // poolName_machineName_vmNameInstance

    public MessageFormat ASADMIN_COMMAND = new MessageFormat(
            "{0}" + FS + "lib" + FS + "nadmin" + (OS.isWindows() ? ".bat" : "")); // {0} must be install root.

    public String RESOURCE_TYPE = "resourcetype";

    public String CLASSNAME = "classname";
}



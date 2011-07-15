/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.connectors.admin.cli;


//TODO java-doc

/**
 * @author Jagadish Ramu
 */
public interface CLIConstants {

    String TARGET = "target";
    String PROPERTY = "property";
    String OPERAND = "DEFAULT";
    String ENABLED = "enabled";
    String DESCRIPTION = "description";
    String IGNORE_DUPLICATE = "ignore-duplicate";
    String DO_NOT_CREATE_RESOURCE_REF = "do-not-create-resource-ref";
    String OBJECT_TYPE = "objecttype";


    public interface WSM {
        String WSM_RA_NAME = "raname";
        String WSM_PRINCIPALS_MAP = "principalsmap";
        String WSM_GROUPS_MAP = "groupsmap";
        String WSM_MAP_NAME = "mapname";
        String WSM_CREATE_WSM_COMMAND = "create-connector-work-security-map";
    }

    public interface RAC {
        String RAC_RA_NAME = "raname";
        String RAC_THREAD_POOL_ID = "threadpoolid";
        String RAC_CREATE_RAC_COMMAND = "create-resource-adapter-config";
    }

    public interface AOR {
        String AOR_CREATE_COMMAND_NAME = "create-admin-object";
        String AOR_RES_TYPE="restype";
        String AOR_CLASS_NAME = "classname";
        String AOR_RA_NAME = "raname";
        String AOR_JNDI_NAME = "jndi_name";
    }

    public interface SM {
        String SM_CREATE_COMMAND_NAME="create-connector-security-map";
        String SM_POOL_NAME = "poolname";
        String SM_PRINCIPALS = "principals";
        String SM_USER_GROUPS="usergroups";
        String SM_MAPPED_NAME = "mappedusername";
        String SM_MAPPED_PASSWORD = "mappedpassword";
        String SM_MAP_NAME = "mapname";
    }

    public interface CCP {
        String CCP_RA_NAME = "raname";
        String CCP_CON_DEFN_NAME = "connectiondefinition";
        String CCP_STEADY_POOL_SIZE = "steadypoolsize";
        String CCP_MAX_POOL_SIZE = "maxpoolsize";
        String CCP_MAX_WAIT_TIME = "maxwait";
        String CCP_POOL_RESIZE_QTY = "poolresize";
        String CCP_IDLE_TIMEOUT = "idletimeout";
        String CCP_IS_VALIDATION_REQUIRED = "isconnectvalidatereq";
        String CCP_FAIL_ALL_CONNS = "failconnection";
        String CCP_LEAK_TIMEOUT = "leaktimeout";
        String CCP_LEAK_RECLAIM = "leakreclaim";
        String CCP_CON_CREATION_RETRY_ATTEMPTS = "creationretryattempts";
        String CCP_CON_CREATION_RETRY_INTERVAL = "creationretryinterval";
        String CCP_LAZY_CON_ENLISTMENT = "lazyconnectionenlistment";
        String CCP_LAZY_CON_ASSOC = "lazyconnectionassociation";
        String CCP_ASSOC_WITH_THREAD = "associatewiththread";
        String CCP_MATCH_CONNECTIONS = "matchconnections";
        String CCP_MAX_CON_USAGE_COUNT = "maxconnectionusagecount";
        String CCP_PING = "ping";
        String CCP_POOLING = "pooling";
        String CCP_VALIDATE_ATMOST_PERIOD = "validateatmostonceperiod";
        String CCP_TXN_SUPPORT = "transactionsupport";
        String CCP_POOL_NAME = "poolname";
        String CCP_CREATE_COMMAND_NAME="create-connector-connection-pool";
    }

    public interface CR {
        String CR_POOL_NAME = "poolname";
        String CR_OBJECT_TYPE = "objecttype";
        String CR_JNDI_NAME = "jndi_name";
        String CR_CREATE_COMMAND_NAME="create-connector-resource";
    }

}

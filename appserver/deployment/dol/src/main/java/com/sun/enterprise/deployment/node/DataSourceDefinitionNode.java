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

package com.sun.enterprise.deployment.node;

import org.w3c.dom.Node;


import com.sun.enterprise.deployment.xml.TagNames;
import com.sun.enterprise.deployment.DataSourceDefinitionDescriptor;

import java.util.Map;

public class DataSourceDefinitionNode extends DeploymentDescriptorNode<DataSourceDefinitionDescriptor> {

    public final static XMLElement tag = new XMLElement(TagNames.DATA_SOURCE);
    private DataSourceDefinitionDescriptor descriptor = null;
    public DataSourceDefinitionNode() {
        registerElementHandler(new XMLElement(TagNames.RESOURCE_PROPERTY), ResourcePropertyNode.class,
                "addDataSourcePropertyDescriptor");
    }

    protected Map getDispatchTable() {
        // no need to be synchronized for now
        Map table = super.getDispatchTable();

        table.put(TagNames.DATA_SOURCE_DESCRIPTION, "setDescription");
        table.put(TagNames.DATA_SOURCE_NAME, "setName");
        table.put(TagNames.DATA_SOURCE_CLASS_NAME, "setClassName");
        table.put(TagNames.DATA_SOURCE_SERVER_NAME, "setServerName");
        table.put(TagNames.DATA_SOURCE_PORT_NUMBER, "setPortNumber");
        table.put(TagNames.DATA_SOURCE_DATABASE_NAME, "setDatabaseName");
        table.put(TagNames.DATA_SOURCE_URL, "setUrl");
        table.put(TagNames.DATA_SOURCE_USER, "setUser");
        table.put(TagNames.DATA_SOURCE_PASSWORD, "setPassword");
        //
        table.put(TagNames.DATA_SOURCE_LOGIN_TIMEOUT, "setLoginTimeout");
        table.put(TagNames.DATA_SOURCE_TRANSACTIONAL, "setTransactional");
        table.put(TagNames.DATA_SOURCE_ISOLATION_LEVEL, "setIsolationLevel");
        table.put(TagNames.DATA_SOURCE_INITIAL_POOL_SIZE, "setInitialPoolSize");
        table.put(TagNames.DATA_SOURCE_MAX_POOL_SIZE, "setMaxPoolSize");
        table.put(TagNames.DATA_SOURCE_MIN_POOL_SIZE, "setMinPoolSize");
        table.put(TagNames.DATA_SOURCE_MAX_IDLE_TIME, "setMaxIdleTime");
        table.put(TagNames.DATA_SOURCE_MAX_STATEMENTS, "setMaxStatements");

        return table;
    }


    public Node writeDescriptor(Node parent, String nodeName, DataSourceDefinitionDescriptor dataSourceDesc) {

        Node node = appendChild(parent, nodeName);
        appendTextChild(node, TagNames.DATA_SOURCE_DESCRIPTION, dataSourceDesc.getDescription());
        appendTextChild(node, TagNames.DATA_SOURCE_NAME, dataSourceDesc.getName());
        appendTextChild(node, TagNames.DATA_SOURCE_CLASS_NAME, dataSourceDesc.getClassName());
        appendTextChild(node, TagNames.DATA_SOURCE_SERVER_NAME, dataSourceDesc.getServerName());
        appendTextChild(node, TagNames.DATA_SOURCE_PORT_NUMBER, dataSourceDesc.getPortNumber());
        appendTextChild(node, TagNames.DATA_SOURCE_DATABASE_NAME, dataSourceDesc.getDatabaseName());
        appendTextChild(node, TagNames.DATA_SOURCE_URL, dataSourceDesc.getUrl());
        appendTextChild(node, TagNames.DATA_SOURCE_USER, dataSourceDesc.getUser());
        appendTextChild(node, TagNames.DATA_SOURCE_PASSWORD, dataSourceDesc.getPassword());

        ResourcePropertyNode propertyNode = new ResourcePropertyNode();
        propertyNode.writeDescriptor(node, dataSourceDesc);

        appendTextChild(node, TagNames.DATA_SOURCE_LOGIN_TIMEOUT, String.valueOf(dataSourceDesc.getLoginTimeout()));
        appendTextChild(node, TagNames.DATA_SOURCE_TRANSACTIONAL, String.valueOf(dataSourceDesc.isTransactional()));
        //DD specified Enumeration values are String
        //Annotation uses integer values and hence this mapping is needed
        String isolationLevelString = dataSourceDesc.getIsolationLevelString();
        if(isolationLevelString != null){
            appendTextChild(node, TagNames.DATA_SOURCE_ISOLATION_LEVEL, isolationLevelString);
        }
        appendTextChild(node, TagNames.DATA_SOURCE_INITIAL_POOL_SIZE, dataSourceDesc.getInitialPoolSize());
        appendTextChild(node, TagNames.DATA_SOURCE_MAX_POOL_SIZE, dataSourceDesc.getMaxPoolSize());
        appendTextChild(node, TagNames.DATA_SOURCE_MIN_POOL_SIZE, dataSourceDesc.getMinPoolSize());
        appendTextChild(node, TagNames.DATA_SOURCE_MAX_IDLE_TIME, String.valueOf(dataSourceDesc.getMaxIdleTime()));
        appendTextChild(node, TagNames.DATA_SOURCE_MAX_STATEMENTS, dataSourceDesc.getMaxStatements());

        return node;
    }

    public DataSourceDefinitionDescriptor getDescriptor() {
        if(descriptor == null){
            descriptor = new DataSourceDefinitionDescriptor();
        }
        return descriptor;
    }
}

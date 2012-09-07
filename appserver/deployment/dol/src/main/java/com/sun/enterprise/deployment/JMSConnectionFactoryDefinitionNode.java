/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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

import java.util.Map;

import org.w3c.dom.Node;

import com.sun.enterprise.deployment.JMSConnectionFactoryDefinitionDescriptor;
import com.sun.enterprise.deployment.xml.TagNames;

public class JMSConnectionFactoryDefinitionNode extends DeploymentDescriptorNode<JMSConnectionFactoryDefinitionDescriptor> {

    public final static XMLElement tag = new XMLElement(TagNames.JMS_CONNECTION_FACTORY);

    private JMSConnectionFactoryDefinitionDescriptor descriptor = null;

    public JMSConnectionFactoryDefinitionNode() {
        registerElementHandler(new XMLElement(TagNames.JMS_CONNECTION_FACTORY_PROPERTY), JMSConnectionFactoryPropertyNode.class,
                "addJMSConnectionFactoryPropertyDescriptor");
    }

    protected Map<String, String> getDispatchTable() {
        // no need to be synchronized for now
        Map<String, String> table = super.getDispatchTable();

        table.put(TagNames.JMS_CONNECTION_FACTORY_DESCRIPTION, "setDescription");
        table.put(TagNames.JMS_CONNECTION_FACTORY_NAME, "setName");
        table.put(TagNames.JMS_CONNECTION_FACTORY_CLASS_NAME, "setClassName");
        table.put(TagNames.JMS_CONNECTION_FACTORY_RESOURCE_ADAPTER_NAME, "setResourceAdapterName");
        table.put(TagNames.JMS_CONNECTION_FACTORY_USER, "setUser");
        table.put(TagNames.JMS_CONNECTION_FACTORY_PASSWORD, "setPassword");
        table.put(TagNames.JMS_CONNECTION_FACTORY_CLIENT_ID, "setClientId");

        table.put(TagNames.JMS_CONNECTION_FACTORY_CONNECTION_TIMEOUT, "setConnectionTimeout");
        table.put(TagNames.JMS_CONNECTION_FACTORY_TRANSACTIONAL, "setTransactional");
        table.put(TagNames.JMS_CONNECTION_FACTORY_INITIAL_POOL_SIZE, "setInitialPoolSize");
        table.put(TagNames.JMS_CONNECTION_FACTORY_MAX_POOL_SIZE, "setMaxPoolSize");
        table.put(TagNames.JMS_CONNECTION_FACTORY_MIN_POOL_SIZE, "setMinPoolSize");
        table.put(TagNames.JMS_CONNECTION_FACTORY_MAX_IDLE_TIME, "setMaxIdleTime");

        return table;
    }

    public Node writeDescriptor(Node parent, String nodeName, JMSConnectionFactoryDefinitionDescriptor desc) {
        Node node = appendChild(parent, nodeName);
        appendTextChild(node, TagNames.JMS_CONNECTION_FACTORY_DESCRIPTION, desc.getDescription());
        appendTextChild(node, TagNames.JMS_CONNECTION_FACTORY_NAME, desc.getName());
        appendTextChild(node, TagNames.JMS_CONNECTION_FACTORY_CLASS_NAME, desc.getClassName());
        appendTextChild(node, TagNames.JMS_CONNECTION_FACTORY_RESOURCE_ADAPTER_NAME, desc.getResourceAdapterName());
        appendTextChild(node, TagNames.JMS_CONNECTION_FACTORY_USER, desc.getUser());
        appendTextChild(node, TagNames.JMS_CONNECTION_FACTORY_PASSWORD, desc.getPassword());
        appendTextChild(node, TagNames.JMS_CONNECTION_FACTORY_CLIENT_ID, desc.getClientId());

        JMSConnectionFactoryPropertyNode propertyNode = new JMSConnectionFactoryPropertyNode();
        propertyNode.writeDescriptor(node, desc);

        appendTextChild(node, TagNames.JMS_CONNECTION_FACTORY_CONNECTION_TIMEOUT, desc.getConnectionTimeout());
        appendTextChild(node, TagNames.JMS_CONNECTION_FACTORY_TRANSACTIONAL, String.valueOf(desc.isTransactional()));
        appendTextChild(node, TagNames.JMS_CONNECTION_FACTORY_INITIAL_POOL_SIZE, desc.getInitialPoolSize());
        appendTextChild(node, TagNames.JMS_CONNECTION_FACTORY_MAX_POOL_SIZE, desc.getMaxPoolSize());
        appendTextChild(node, TagNames.JMS_CONNECTION_FACTORY_MIN_POOL_SIZE, desc.getMinPoolSize());
        appendTextChild(node, TagNames.JMS_CONNECTION_FACTORY_MAX_IDLE_TIME, desc.getMaxIdleTime());

        return node;
    }

    public JMSConnectionFactoryDefinitionDescriptor getDescriptor() {
        if (descriptor == null) {
            descriptor = new JMSConnectionFactoryDefinitionDescriptor();
        }
        return descriptor;
    }
}


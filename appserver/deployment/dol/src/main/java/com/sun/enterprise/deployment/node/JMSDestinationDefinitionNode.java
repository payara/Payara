/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.deployment.JMSDestinationDefinitionDescriptor;
import com.sun.enterprise.deployment.xml.TagNames;
import org.w3c.dom.Node;

import java.util.Map;

public class JMSDestinationDefinitionNode extends DeploymentDescriptorNode<JMSDestinationDefinitionDescriptor> {

    public final static XMLElement tag = new XMLElement(TagNames.JMS_DESTINATION);

    private JMSDestinationDefinitionDescriptor descriptor = null;

    public JMSDestinationDefinitionNode() {
        registerElementHandler(new XMLElement(TagNames.JMS_DESTINATION_PROPERTY), ResourcePropertyNode.class,
                "addJMSDestinationPropertyDescriptor");
    }

    protected Map<String, String> getDispatchTable() {
        // no need to be synchronized for now
        Map<String, String> table = super.getDispatchTable();

        table.put(TagNames.JMS_DESTINATION_DESCRIPTION, "setDescription");
        table.put(TagNames.JMS_DESTINATION_NAME, "setName");
        table.put(TagNames.JMS_DESTINATION_CLASS_NAME, "setClassName");
        table.put(TagNames.JMS_DESTINATION_RESOURCE_ADAPTER, "setResourceAdapter");
        table.put(TagNames.JMS_DESTINATION_DESTINATION_NAME, "setDestinationName");

        return table;
    }

    public Node writeDescriptor(Node parent, String nodeName, JMSDestinationDefinitionDescriptor desc) {
        Node node = appendChild(parent, nodeName);
        appendTextChild(node, TagNames.JMS_DESTINATION_DESCRIPTION, desc.getDescription());
        appendTextChild(node, TagNames.JMS_DESTINATION_NAME, desc.getName());
        appendTextChild(node, TagNames.JMS_DESTINATION_CLASS_NAME, desc.getClassName());
        appendTextChild(node, TagNames.JMS_DESTINATION_RESOURCE_ADAPTER, desc.getResourceAdapter());
        appendTextChild(node, TagNames.JMS_DESTINATION_DESTINATION_NAME, desc.getDestinationName());

        ResourcePropertyNode propertyNode = new ResourcePropertyNode();
        propertyNode.writeDescriptor(node, desc);

        return node;
    }

    public JMSDestinationDefinitionDescriptor getDescriptor() {
        if (descriptor == null) {
            descriptor = new JMSDestinationDefinitionDescriptor();
        }
        return descriptor;
    }
}


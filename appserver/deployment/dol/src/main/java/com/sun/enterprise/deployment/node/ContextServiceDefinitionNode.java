/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2022] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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

import com.sun.enterprise.deployment.ContextServiceDefinitionDescriptor;
import com.sun.enterprise.deployment.xml.TagNames;

public class ContextServiceDefinitionNode extends DeploymentDescriptorNode<ContextServiceDefinitionDescriptor> {
    public final static XMLElement tag = new XMLElement(TagNames.CONTEXT_SERVICE);

    ContextServiceDefinitionDescriptor descriptor = null;

    public ContextServiceDefinitionNode() {
        registerElementHandler(new XMLElement(com.sun.enterprise.deployment.xml.TagNames.RESOURCE_PROPERTY), ResourcePropertyNode.class,
                "addContextServiceExecutorDescriptor");
    }

    protected java.util.Map getDispatchTable() {
        java.util.Map table = super.getDispatchTable();
        table.put(TagNames.CONTEXT_SERVICE_NAME, "setName");
        table.put(TagNames.CONTEXT_SERVICE_PROPAGATED, "addPropagated");
        table.put(TagNames.CONTEXT_SERVICE_CLEARED, "addCleared");
        table.put(TagNames.CONTEXT_SERVICE_UNCHANGED, "addUnchanged");
        return table;
    }

    public org.w3c.dom.Node writeDescriptor(org.w3c.dom.Node parent, String nodeName,
                                            ContextServiceDefinitionDescriptor contextServiceDefinitionDescriptor) {
        org.w3c.dom.Node node = appendChild(parent, nodeName);
        appendTextChild(node, TagNames.CONTEXT_SERVICE_NAME, contextServiceDefinitionDescriptor.getName());
        for(String s : contextServiceDefinitionDescriptor.getCleared()) {
            appendTextChild(node, TagNames.CONTEXT_SERVICE_CLEARED, s);
        }

        for(String s : contextServiceDefinitionDescriptor.getPropagated()) {
            appendTextChild(node, TagNames.CONTEXT_SERVICE_PROPAGATED, s);
        }

        for(String s : contextServiceDefinitionDescriptor.getUnchanged()) {
            appendTextChild(node, TagNames.CONTEXT_SERVICE_UNCHANGED, s);
        }

        ResourcePropertyNode propertyNode = new ResourcePropertyNode();
        propertyNode.writeDescriptor(node, contextServiceDefinitionDescriptor);
        return node;
    }

    public ContextServiceDefinitionDescriptor getDescriptor() {
        if (descriptor == null) {
            descriptor = new ContextServiceDefinitionDescriptor();
        }
        return descriptor;
    }
}
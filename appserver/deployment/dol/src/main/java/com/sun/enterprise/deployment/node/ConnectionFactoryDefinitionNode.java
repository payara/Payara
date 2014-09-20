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

import com.sun.enterprise.deployment.ConnectionFactoryDefinitionDescriptor;
import com.sun.enterprise.deployment.util.DOLUtils;
import com.sun.enterprise.deployment.xml.TagNames;

import org.glassfish.logging.annotation.LogMessageInfo;
import org.w3c.dom.Node;

import java.util.Map;
import java.util.logging.Level;

public class ConnectionFactoryDefinitionNode extends DeploymentDescriptorNode<ConnectionFactoryDefinitionDescriptor> {
    public final static XMLElement tag = new XMLElement(TagNames.CONNECTION_FACTORY);
    
    private ConnectionFactoryDefinitionDescriptor descriptor = null;
    
    public ConnectionFactoryDefinitionNode() {
        registerElementHandler(new XMLElement(TagNames.RESOURCE_PROPERTY), ResourcePropertyNode.class,
                "addConnectionFactoryPropertyDescriptor");
    }

    protected Map<String, String> getDispatchTable() {
        // no need to be synchronized for now
        Map<String, String> table = super.getDispatchTable();
        table.put(TagNames.CONNECTION_FACTORY_NAME, "setName");
        table.put(TagNames.CONNECTION_FACTORY_INTERFACE_NAME, "setInterfaceName");
        table.put(TagNames.CONNECTION_FACTORY_ADAPTER, "setResourceAdapter");
        table.put(TagNames.CONNECTION_FACTORY_TRANSACTION_SUPPORT, "setTransactionSupport");
        table.put(TagNames.CONNECTION_FACTORY_MAX_POOL_SIZE, "setMaxPoolSize");
        table.put(TagNames.CONNECTION_FACTORY_MIN_POOL_SIZE, "setMinPoolSize");

        return table;
    }
    
    @LogMessageInfo(
            message = "For connection-factory resource: {0}, there is no application part in its resource adapter name: {1}.",
            level="WARNING",
            cause = "For embedded resource adapter, its internal format of resource adapter name should contains application name.",
            comment = "For the method writeDescriptor of com.sun.enterprise.deployment.node.ConnectionFactoryDefinitionNode."
            )
    private static final String RESOURCE_ADAPTER_NAME_INVALID = "AS-DEPLOYMENT-00023";

    public Node writeDescriptor(Node parent, String nodeName, ConnectionFactoryDefinitionDescriptor desc) {
        Node node = appendChild(parent, nodeName);
        appendTextChild(node, TagNames.CONNECTION_FACTORY_DESCRIPTION, desc.getDescription());
        appendTextChild(node, TagNames.CONNECTION_FACTORY_NAME, desc.getName());
        appendTextChild(node, TagNames.CONNECTION_FACTORY_INTERFACE_NAME, desc.getInterfaceName());

        // change the resource adapter name from internal format to standard format
        String resourceAdapterName = desc.getResourceAdapter();
        int poundIndex = resourceAdapterName.indexOf("#");
        if(poundIndex > 0){
            // the internal format of resource adapter name is "appName#raName", remove the appName part
            resourceAdapterName =  resourceAdapterName.substring(poundIndex);
            
        }else if(poundIndex == 0){
            // the resource adapter name should not be the standard format "#raName" here
            DOLUtils.getDefaultLogger().log(Level.WARNING, RESOURCE_ADAPTER_NAME_INVALID,
                    new Object[] { desc.getName(), desc.getResourceAdapter() });
        }else{
            // the resource adapter name represent the standalone RA in this case.
        }
        appendTextChild(node, TagNames.CONNECTION_FACTORY_ADAPTER, resourceAdapterName);
        
        appendTextChild(node, TagNames.CONNECTION_FACTORY_MAX_POOL_SIZE, desc.getMaxPoolSize());
        appendTextChild(node, TagNames.CONNECTION_FACTORY_MIN_POOL_SIZE, desc.getMinPoolSize());
        appendTextChild(node, TagNames.CONNECTION_FACTORY_TRANSACTION_SUPPORT, desc.getTransactionSupport());
        
        ResourcePropertyNode propertyNode = new ResourcePropertyNode();
        propertyNode.writeDescriptor(node, desc);

        return node;
    }
    
    public ConnectionFactoryDefinitionDescriptor getDescriptor() {
        if(descriptor == null){
            descriptor = new ConnectionFactoryDefinitionDescriptor();
        }
        return descriptor;
    }

}

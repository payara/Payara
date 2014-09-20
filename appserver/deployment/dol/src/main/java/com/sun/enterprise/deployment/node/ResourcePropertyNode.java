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

import com.sun.enterprise.deployment.*;
import com.sun.enterprise.deployment.xml.TagNames;
import org.glassfish.deployment.common.Descriptor;
import org.w3c.dom.Node;

import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: naman
 * Date: 7/9/12
 * Time: 12:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class ResourcePropertyNode extends DeploymentDescriptorNode<ResourcePropertyDescriptor> {

    private ResourcePropertyDescriptor descriptor = null;

    protected Map getDispatchTable() {
        // no need to be synchronized for now
        Map table = super.getDispatchTable();
        table.put(TagNames.RESOURCE_PROPERTY_NAME, "setName");
        table.put(TagNames.RESOURCE_PROPERTY_VALUE, "setValue");
        return table;
    }

    public Node writeDescriptor(Node node, Descriptor desc) {

        Properties properties = null;

        if (desc instanceof MailSessionDescriptor) {
            properties = ((MailSessionDescriptor) desc).getProperties();
        } else if (desc instanceof ConnectionFactoryDefinitionDescriptor) {
            properties = ((ConnectionFactoryDefinitionDescriptor) desc).getProperties();
        } else if (desc instanceof DataSourceDefinitionDescriptor) {
            properties = ((DataSourceDefinitionDescriptor) desc).getProperties();
        } else if (desc instanceof JMSConnectionFactoryDefinitionDescriptor) {
            properties = ((JMSConnectionFactoryDefinitionDescriptor) desc).getProperties();
        } else if (desc instanceof JMSDestinationDefinitionDescriptor) {
            properties = ((JMSDestinationDefinitionDescriptor) desc).getProperties();
        }


        Set keys = properties.keySet();

        for (Object key : keys) {
            String name = (String) key;
            String value = (String) properties.get(name);
            Node propertyNode = appendChild(node, TagNames.RESOURCE_PROPERTY);
            appendTextChild(propertyNode, TagNames.RESOURCE_PROPERTY_NAME, name);
            appendTextChild(propertyNode, TagNames.RESOURCE_PROPERTY_VALUE, value);
        }
        return node;
    }


    public ResourcePropertyDescriptor getDescriptor() {
        if (descriptor == null) {
            descriptor = new ResourcePropertyDescriptor();
        }
        return descriptor;
    }
}

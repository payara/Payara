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

import java.util.Map;

import com.sun.enterprise.deployment.EnvironmentProperty;
import com.sun.enterprise.deployment.InjectionTarget;
import com.sun.enterprise.deployment.xml.TagNames;
import org.w3c.dom.Node;

/**
 * This node is responsible for handling all env-entry related xml tags
 *
 * @author  Jerome Dochez
 * @version 
 */
public class EnvEntryNode extends DeploymentDescriptorNode<EnvironmentProperty> {

    private EnvironmentProperty envProp;
    private boolean setValueCalled = false;

    public EnvEntryNode() {
        super();
        registerElementHandler(new XMLElement(TagNames.INJECTION_TARGET), 
                                InjectionTargetNode.class, "addInjectionTarget");                          
    }

    @Override
    public EnvironmentProperty getDescriptor() {
        if (envProp == null) envProp = new EnvironmentProperty();
        return envProp;
    }

    @Override
    protected Map getDispatchTable() {    
        Map table = super.getDispatchTable();
        table.put(TagNames.ENVIRONMENT_PROPERTY_NAME, "setName");
        table.put(TagNames.ENVIRONMENT_PROPERTY_VALUE, "setValue");
        table.put(TagNames.ENVIRONMENT_PROPERTY_TYPE, "setType");        
        table.put(TagNames.MAPPED_NAME, "setMappedName");
        table.put(TagNames.LOOKUP_NAME, "setLookupName");
        return table;
    }

    @Override
    public boolean endElement(XMLElement element) {
        if (TagNames.ENVIRONMENT_PROPERTY_NAME.equals(element.getQName())) {
            // name element is always right before value, so initialize
            // setValueCalled to false when it is processed.
            setValueCalled = false;
        } else if( TagNames.ENVIRONMENT_PROPERTY_VALUE.equals
                   (element.getQName()) ) {
            setValueCalled = true;
        } else if (TagNames.LOOKUP_NAME.equals(element.getQName())) {
            if (setValueCalled) {
                throw new IllegalArgumentException(localStrings.getLocalString( "enterprise.deployment.node.invalidenventry", "Cannot specify both the env-entry-value and lookup-name elements for env-entry element {0}", new Object[] {envProp.getName()}));
            }
        }
        return super.endElement(element);
    } 

    @Override
    public void addDescriptor(Object newDescriptor) {
        if( setValueCalled ) {
            super.addDescriptor(newDescriptor);
        } else {
            // Don't add it to DOL.  The env-entry only exists
            // at runtime if it has been assigned a value.
        }
    }

    @Override
    public Node writeDescriptor(Node parent, String nodeName, EnvironmentProperty envProp) {
        Node envEntryNode = super.writeDescriptor(parent, nodeName, envProp);
        writeLocalizedDescriptions(envEntryNode, envProp);
        appendTextChild(envEntryNode, TagNames.ENVIRONMENT_PROPERTY_NAME, envProp.getName());
        appendTextChild(envEntryNode, TagNames.ENVIRONMENT_PROPERTY_TYPE, envProp.getType());
        appendTextChild(envEntryNode, TagNames.ENVIRONMENT_PROPERTY_VALUE, envProp.getValue());
        appendTextChild(envEntryNode, TagNames.MAPPED_NAME, envProp.getMappedName());
        if( envProp.isInjectable() ) {
            InjectionTargetNode ijNode = new InjectionTargetNode();
            for (InjectionTarget target : envProp.getInjectionTargets()) {
                ijNode.writeDescriptor(envEntryNode, TagNames.INJECTION_TARGET, target);
            }
        }
        appendTextChild(envEntryNode, TagNames.LOOKUP_NAME, envProp.getLookupName());
        return envEntryNode;
    }
}

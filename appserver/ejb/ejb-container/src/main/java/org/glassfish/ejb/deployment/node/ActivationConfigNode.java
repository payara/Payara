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

package org.glassfish.ejb.deployment.node;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.sun.enterprise.deployment.EnvironmentProperty;
import com.sun.enterprise.deployment.node.DeploymentDescriptorNode;
import com.sun.enterprise.deployment.node.XMLElement;
import org.glassfish.ejb.deployment.EjbTagNames;
import org.glassfish.ejb.deployment.descriptor.ActivationConfigDescriptor;
import org.w3c.dom.Node;

/**
 * This class is responsible for handling the activation config elements.
 *
 * @author Kenneth Saks
 * @version 
 */
public class ActivationConfigNode extends DeploymentDescriptorNode<ActivationConfigDescriptor> {

    private ActivationConfigDescriptor descriptor;
    private String propertyName = null;

    public ActivationConfigNode() {
        super();
        registerElementHandler(new XMLElement(EjbTagNames.ACTIVATION_CONFIG),
                               ActivationConfigNode.class, 
                               "setActivationConfigDescriptor");
    }

    @Override
    public ActivationConfigDescriptor getDescriptor() {
        if (descriptor == null) descriptor = new ActivationConfigDescriptor();
        return descriptor;
    }

    @Override
    public void setElementValue(XMLElement element, String value) {    
        if (EjbTagNames.ACTIVATION_CONFIG_PROPERTY_NAME.equals
            (element.getQName())) {
            propertyName = value;
        } else if(EjbTagNames.ACTIVATION_CONFIG_PROPERTY_VALUE.equals
                  (element.getQName())) {
            EnvironmentProperty prop = 
                new EnvironmentProperty(propertyName, value, "");
            descriptor.getActivationConfig().add(prop);
            propertyName = null;
        }
    }

    @Override
    public Node writeDescriptor(Node parent, String nodeName, 
                                ActivationConfigDescriptor descriptor) {        

        Node activationConfigNode = null;
        Set activationConfig = descriptor.getActivationConfig();

        // ActionConfig must have at least one ActionConfigProperty
        // and ActionConfigProperty must have a pair of name/value
        // so filter out the entries with blank name or value  
        Set activationConfig2 = new HashSet(); 
        for(Iterator iter = activationConfig.iterator(); iter.hasNext();) {
            EnvironmentProperty next = (EnvironmentProperty) iter.next();
            if ( ! next.getName().trim().equals("") && 
                 ! next.getValue().trim().equals("")) {
                activationConfig2.add(next);
            }
        }

        if( activationConfig2.size() > 0 ) {
            activationConfigNode = 
                appendChild(parent, nodeName);
            writeLocalizedDescriptions(activationConfigNode, descriptor);
            
            for(Iterator iter = activationConfig2.iterator(); iter.hasNext();) {
                Node activationConfigPropertyNode = 
                    appendChild(activationConfigNode, 
                                EjbTagNames.ACTIVATION_CONFIG_PROPERTY);
                EnvironmentProperty next = (EnvironmentProperty) iter.next();
                appendTextChild(activationConfigPropertyNode, 
                                EjbTagNames.ACTIVATION_CONFIG_PROPERTY_NAME, 
                                next.getName());
                appendTextChild(activationConfigPropertyNode,
                                EjbTagNames.ACTIVATION_CONFIG_PROPERTY_VALUE, 
                                next.getValue());
            }
        } 
        return activationConfigNode;
    }
}

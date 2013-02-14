/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.deployment.node.connector;

import com.sun.enterprise.deployment.*;
import com.sun.enterprise.deployment.node.DeploymentDescriptorNode;
import com.sun.enterprise.deployment.node.DescriptorFactory;
import com.sun.enterprise.deployment.xml.ConnectorTagNames;
import org.glassfish.deployment.common.Descriptor;
import org.w3c.dom.Node;

import java.util.Iterator;
import java.util.Map;

/**
 * This node is responsible for handling the Connector DTD related config-property XML tag
 *
 * @author  Sheetal Vartak
 * @version 
 */
public class ConfigPropertyNode extends DeploymentDescriptorNode {

    private ConnectorConfigProperty config = null;

   /**
     * all sub-implementation of this class can use a dispatch table to map xml element to
     * method name on the descriptor class for setting the element value. 
     *  
     * @return the map with the element name as a key, the setter method as a value
     */    
    protected Map getDispatchTable() {    
        Map table = super.getDispatchTable();
        table.put(ConnectorTagNames.CONFIG_PROPERTY_NAME, "setName");
        table.put(ConnectorTagNames.CONFIG_PROPERTY_VALUE, "setValue");
        table.put(ConnectorTagNames.CONFIG_PROPERTY_TYPE, "setType");        
        table.put(ConnectorTagNames.CONFIG_PROPERTY_SUPPORTS_DYNAMIC_UPDATES, "setSupportsDynamicUpdates");        
        table.put(ConnectorTagNames.CONFIG_PROPERTY_IGNORE, "setIgnore");
        table.put(ConnectorTagNames.CONFIG_PROPERTY_CONFIDENTIAL, "setConfidential");
        return table;
    }    

    /**
    * @return the descriptor instance to associate with this XMLNode
    */    
    public Object getDescriptor() {
        if (config == null) {
            config = (ConnectorConfigProperty) DescriptorFactory.getDescriptor(getXMLPath());
        } 
        return config;
    } 

    /**
     * write the descriptor class to a DOM tree and return it
     *
     * @param parent node for the DOM tree
     * @param the descriptor to write
     * @return the DOM tree top node
     */    
    public Node writeDescriptor(Node parent, Descriptor descriptor) {

        if (! (descriptor instanceof ConnectorDescriptor) && 
	    ! (descriptor instanceof AdminObject) && 
	    ! (descriptor instanceof ConnectionDefDescriptor) &&
	    ! (descriptor instanceof OutboundResourceAdapter) &&
        ! (descriptor instanceof MessageListener) )  {
            throw new IllegalArgumentException(getClass() + " cannot handle descriptors of type " + descriptor.getClass());
        }
	Iterator configProps = null;
	if (descriptor instanceof ConnectorDescriptor) {
	    configProps = ((ConnectorDescriptor)descriptor).getConfigProperties().iterator();
	} else if (descriptor instanceof ConnectionDefDescriptor) {
	    configProps = ((ConnectionDefDescriptor)descriptor).getConfigProperties().iterator();
	} else if (descriptor instanceof AdminObject) {
	    configProps = ((AdminObject)descriptor).getConfigProperties().iterator();
	} else if (descriptor instanceof OutboundResourceAdapter) {
	    configProps = ((OutboundResourceAdapter)descriptor).getConfigProperties().iterator();
	} else if (descriptor instanceof MessageListener){
        configProps = ((MessageListener)descriptor).getConfigProperties().iterator();
    }
	//config property info
        if (configProps != null) {
          for (;configProps.hasNext();) {
	    ConnectorConfigProperty config = (ConnectorConfigProperty) configProps.next();
	    Node configNode = appendChild(parent, ConnectorTagNames.CONFIG_PROPERTY);
	    writeLocalizedDescriptions(configNode, config);  
	    appendTextChild(configNode, ConnectorTagNames.CONFIG_PROPERTY_NAME, config.getName());  
	    appendTextChild(configNode, ConnectorTagNames.CONFIG_PROPERTY_TYPE, config.getType());  
	    appendTextChild(configNode, ConnectorTagNames.CONFIG_PROPERTY_VALUE, config.getValue());  
	    appendTextChild(configNode, ConnectorTagNames.CONFIG_PROPERTY_IGNORE, String.valueOf(config.isIgnore()));
	    appendTextChild(configNode, ConnectorTagNames.CONFIG_PROPERTY_SUPPORTS_DYNAMIC_UPDATES,
                            String.valueOf(config.isSupportsDynamicUpdates()));
	    appendTextChild(configNode, ConnectorTagNames.CONFIG_PROPERTY_CONFIDENTIAL,
                            String.valueOf(config.isConfidential()));
          }
        }
	return parent;
    }
}

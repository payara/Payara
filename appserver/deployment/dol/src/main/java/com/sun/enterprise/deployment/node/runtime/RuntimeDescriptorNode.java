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

package com.sun.enterprise.deployment.node.runtime;

import com.sun.enterprise.deployment.BundleDescriptor;
import org.glassfish.deployment.common.Descriptor;
import com.sun.enterprise.deployment.JndiNameEnvironment;
import com.sun.enterprise.deployment.MessageDestinationDescriptor;
import com.sun.enterprise.deployment.node.DeploymentDescriptorNode;
import com.sun.enterprise.deployment.node.XMLElement;
import com.sun.enterprise.deployment.runtime.RuntimeDescriptor;
import com.sun.enterprise.deployment.types.EjbReferenceContainer;
import com.sun.enterprise.deployment.types.MessageDestinationReferenceContainer;
import com.sun.enterprise.deployment.types.ResourceEnvReferenceContainer;
import com.sun.enterprise.deployment.types.ResourceReferenceContainer;
import com.sun.enterprise.deployment.util.DOLUtils;
import com.sun.enterprise.deployment.xml.RuntimeTagNames;
import org.w3c.dom.Node;

import java.util.Iterator;
import java.util.logging.Level;

/**
 * Superclass for all the runtime descriptor nodes
 *
 * @author  Jerome Dochez
 * @version 
 */
public class RuntimeDescriptorNode<T> extends DeploymentDescriptorNode<T>
{
    /**
     * @return the descriptor instance to associate with this XMLNode
     */
    @Override
    public T getDescriptor() {
        
        if (abstractDescriptor==null) {
	    abstractDescriptor = createDescriptor();
            if (abstractDescriptor ==null) {
                return (T)getParentNode().getDescriptor();
            }
        }
        return (T)abstractDescriptor;
    }     
    
    @SuppressWarnings("unchecked")
    protected Object createDescriptor() {
        return RuntimeDescriptorFactory.getDescriptor(getXMLPath());
    }
    
    /**
     * receives notification of the value for a particular tag
     * 
     * @param element the xml element
     * @param value it's associated value
     */
    public void setElementValue(XMLElement element, String value) {
	if (getDispatchTable().containsKey(element.getQName())) {
	    super.setElementValue(element, value);
	} else {
	    Object o = getDescriptor();
	    if (o instanceof RuntimeDescriptor) {
		RuntimeDescriptor rd = (RuntimeDescriptor) o;
		rd.setValue(element.getQName(), value);
	    } else {
                DOLUtils.getDefaultLogger().log(Level.SEVERE, "enterprise.deployment.backend.addDescriptorFailure",
                    new Object[]{element.getQName() , value });
            }
	}
    }
    
    /**
     * writes all information common to all J2EE components
     *
     * @param parent xml node parent to add the info to
     * @param descriptor the descriptor
     */
    public static void writeCommonComponentInfo(Node parent, Descriptor descriptor) {
        if (descriptor instanceof EjbReferenceContainer) {
            EjbRefNode.writeEjbReferences(parent, (EjbReferenceContainer) descriptor);
        }	
        if (descriptor instanceof ResourceReferenceContainer) {
            ResourceRefNode.writeResourceReferences(parent, (ResourceReferenceContainer) descriptor);
        }
        if (descriptor instanceof ResourceEnvReferenceContainer) {
            ResourceEnvRefNode.writeResoureEnvReferences(parent, (ResourceEnvReferenceContainer) descriptor);
        }
        if( descriptor instanceof JndiNameEnvironment ) {
            ServiceRefNode.writeServiceReferences
                (parent, (JndiNameEnvironment) descriptor);
        }
        if (descriptor instanceof MessageDestinationReferenceContainer) {
            MessageDestinationRefNode.writeMessageDestinationReferences(parent, 
                (MessageDestinationReferenceContainer) descriptor);
        }
    }                

    public static void writeMessageDestinationInfo(Node parent, 
                                               BundleDescriptor descriptor) {
        for(Iterator iter = descriptor.getMessageDestinations().iterator();
            iter.hasNext();) {
            MessageDestinationRuntimeNode node = 
                new MessageDestinationRuntimeNode();
            node.writeDescriptor(parent, RuntimeTagNames.MESSAGE_DESTINATION,
                                 (MessageDestinationDescriptor) iter.next());
        }
    }

}

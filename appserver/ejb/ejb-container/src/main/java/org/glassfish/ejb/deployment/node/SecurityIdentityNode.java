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

import java.util.Map;

import org.glassfish.ejb.deployment.EjbTagNames;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;

import com.sun.enterprise.deployment.node.DeploymentDescriptorNode;
import com.sun.enterprise.deployment.node.RunAsNode;
import com.sun.enterprise.deployment.node.XMLElement;
import com.sun.enterprise.deployment.xml.TagNames;

/**
 * This node handles all information relative to security-indentity tag
 *
 * @author  Jerome Dochez
 * @version 
 */
public class SecurityIdentityNode extends DeploymentDescriptorNode {

    public SecurityIdentityNode() {
        super();        
        registerElementHandler(new XMLElement(TagNames.RUNAS_SPECIFIED_IDENTITY), RunAsNode.class);
    }

    @Override
    public Object getDescriptor() {
        return null;
    }

    @Override
    protected Map getDispatchTable() {
        return  null;
    }

    @Override
    public void startElement(XMLElement element, Attributes attributes) {
        if( EjbTagNames.USE_CALLER_IDENTITY.equals(element.getQName()) ) {
            ((EjbDescriptor) getParentNode().getDescriptor()).
                setUsesCallerIdentity(true);
        } else {
            super.startElement(element, attributes);
        }
        return;
    }

    @Override
    public void setElementValue(XMLElement element, String value) {    
        if (TagNames.DESCRIPTION.equals(element.getQName())) {
            ((EjbDescriptor) getParentNode().getDescriptor()).setSecurityIdentityDescription(value);
        } else {
            super.setElementValue(element, value);
        }
    }

    public Node writeDescriptor(Node parent, String nodeName, EjbDescriptor descriptor) {    
        Node subNode = appendChild(parent, nodeName);
        appendTextChild(subNode, TagNames.DESCRIPTION, descriptor.getSecurityIdentityDescription());
        if (descriptor.getUsesCallerIdentity()) {
            Node useCaller = subNode.getOwnerDocument().createElement(EjbTagNames.USE_CALLER_IDENTITY);
            subNode.appendChild(useCaller);
        } else {
            RunAsNode runAs = new RunAsNode();
            runAs.writeDescriptor(subNode, TagNames.RUNAS_SPECIFIED_IDENTITY, descriptor.getRunAsIdentity());
        }
    return subNode;
    }
}

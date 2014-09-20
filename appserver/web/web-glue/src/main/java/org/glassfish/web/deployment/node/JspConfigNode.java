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

package org.glassfish.web.deployment.node;

import com.sun.enterprise.deployment.node.DeploymentDescriptorNode;
import com.sun.enterprise.deployment.node.XMLElement;
import org.glassfish.web.deployment.descriptor.JspConfigDescriptorImpl;
import org.glassfish.web.deployment.descriptor.JspGroupDescriptor;
import org.glassfish.web.deployment.descriptor.TagLibConfigurationDescriptor;
import org.glassfish.web.deployment.xml.WebTagNames;
import org.w3c.dom.Node;

import javax.servlet.descriptor.*;

/**
 * This node represents the <jsp-config> element in a web application.
 */
public class JspConfigNode extends DeploymentDescriptorNode<JspConfigDescriptorImpl> {
    public JspConfigNode() {
	super();
	registerElementHandler(new XMLElement(WebTagNames.TAGLIB), TagLibNode.class, "addTagLib");
	registerElementHandler(new XMLElement(WebTagNames.JSP_GROUP), JspGroupNode.class, "addJspGroup");
    }

    protected JspConfigDescriptorImpl descriptor = null;

    /**
     * @return the descriptor instance to associate with this XMLNode
     */
    @Override
    public JspConfigDescriptorImpl getDescriptor() {
        if (descriptor==null) {
            descriptor = new JspConfigDescriptorImpl();
        }
        return descriptor;
    }
    
    /**
     * write the descriptor class to a DOM tree and return it
     *
     * @param parent node in the DOM tree 
     * @param node name for the root element of this xml fragment      
     * @param the descriptor to write
     * @return the DOM tree top node
     */
    public Node writeDescriptor(Node parent, String nodeName, JspConfigDescriptorImpl descriptor) {
	Node myNode = appendChild(parent, nodeName);
	TagLibNode lNode = new TagLibNode();
	for (TaglibDescriptor desc : descriptor.getTaglibs()) {
            lNode.writeDescriptor(myNode, WebTagNames.TAGLIB, 
                (TagLibConfigurationDescriptor) desc);
	}
	JspGroupNode jspGroup = new JspGroupNode();
	for(JspPropertyGroupDescriptor desc : descriptor.getJspPropertyGroups()) {
            jspGroup.writeDescriptor(myNode, WebTagNames.JSP_GROUP, 
                (JspGroupDescriptor) desc);
	}

        return myNode;
    }
}

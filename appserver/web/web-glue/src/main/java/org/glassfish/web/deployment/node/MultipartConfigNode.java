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

import java.util.Map;

import com.sun.enterprise.deployment.node.DeploymentDescriptorNode;
import com.sun.enterprise.deployment.node.XMLElement;
import org.glassfish.web.deployment.descriptor.MultipartConfigDescriptor;
import org.glassfish.web.deployment.xml.WebTagNames;
import org.w3c.dom.Node;

/**
 * This class is responsible for handling multipart-config xml node.
 * 
 * @author Shing Wai Chan
 */
public class MultipartConfigNode extends DeploymentDescriptorNode<MultipartConfigDescriptor> {
    private MultipartConfigDescriptor descriptor;

    public MultipartConfigNode() {
        super();
    }

    /**
     * @return the descriptor instance to associate with this XMLNode
     */
    @Override
    public MultipartConfigDescriptor getDescriptor() {
        if (descriptor == null) {
            descriptor = new MultipartConfigDescriptor();
        }
        return descriptor;
    }

    /**
     * all sub-implementation of this class can use a dispatch table to map xml element to
     * method name on the descriptor class for setting the element value. 
     *  
     * @return the map with the element name as a key, the setter method as a value
     */
    @Override
    protected Map<String, String> getDispatchTable() {
        Map<String, String> table = super.getDispatchTable();
        table.put(WebTagNames.LOCATION, "setLocation");
        return table;
    }

    /**
     * receives notiification of the value for a particular tag
     * 
     * @param element the xml element
     * @param value it's associated value
     */
    @Override
    public void setElementValue(XMLElement element, String value) {
        if (WebTagNames.MAX_FILE_SIZE.equals(element.getQName())) {
            descriptor.setMaxFileSize(Long.valueOf(value));
        } else if (WebTagNames.MAX_REQUEST_SIZE.equals(element.getQName())) {
            descriptor.setMaxRequestSize(Long.valueOf(value));
        } else if (WebTagNames.FILE_SIZE_THRESHOLD.equals(element.getQName())) {
            descriptor.setFileSizeThreshold(Integer.valueOf(value));
        } else {
            super.setElementValue(element, value);
        }
    }

    /**
     * write the descriptor class to a DOM tree and return it
     *
     * @param parent node in the DOM tree 
     * @param nodeName node name for the root element of this xml fragment
     * @param descriptor the descriptor to write
     * @return the DOM tree top node
     */
    @Override
    public Node writeDescriptor(Node parent, String nodeName, MultipartConfigDescriptor descriptor) {       
        Node myNode = appendChild(parent, nodeName);
        appendTextChild(myNode, WebTagNames.LOCATION, descriptor.getLocation());
        if (descriptor.getMaxFileSize() != null) {
            appendTextChild(myNode, WebTagNames.MAX_FILE_SIZE, descriptor.getMaxFileSize().toString());
        }
        if (descriptor.getMaxRequestSize() != null) {
            appendTextChild(myNode, WebTagNames.MAX_REQUEST_SIZE, descriptor.getMaxRequestSize().toString());
        }
        if (descriptor.getFileSizeThreshold() != null) {
            appendTextChild(myNode, WebTagNames.FILE_SIZE_THRESHOLD, descriptor.getFileSizeThreshold().toString());
        }
        
        return myNode;
    }   
}

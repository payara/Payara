/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2016 Oracle and/or its affiliates. All rights reserved.
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
import org.glassfish.web.deployment.descriptor.CookieConfigDescriptor;
import org.glassfish.web.deployment.descriptor.SessionConfigDescriptor;
import org.glassfish.web.deployment.xml.WebTagNames;

import org.w3c.dom.Node;

/**
 * This class is responsible for handling session-config xml node.
 * 
 * @author Shing Wai Chan
 */
public class SessionConfigNode extends DeploymentDescriptorNode {
    private SessionConfigDescriptor descriptor;

    public SessionConfigNode() {
        super();
        registerElementHandler(new XMLElement(WebTagNames.COOKIE_CONFIG),
                CookieConfigNode.class, "setCookieConfig");
    }

   /**
    * @return the descriptor instance to associate with this XMLNode
    */
   @Override
    public SessionConfigDescriptor getDescriptor() {
        if (descriptor == null) {
            descriptor = new SessionConfigDescriptor();
        }
        return descriptor;
    }

    /**
     * receives notiification of the value for a particular tag
     * 
     * @param element the xml element
     * @param value it's associated value
     */    
    public void setElementValue(XMLElement element, String value) {    
        if (WebTagNames.SESSION_TIMEOUT.equals(element.getQName())) {
            // if the session out value is already set
            // which means there are multiple session-config elements
            // throw an exception
            if (descriptor.getSessionTimeout() != 
                SessionConfigDescriptor.SESSION_TIMEOUT_DEFAULT) {
                throw new RuntimeException(
                    "Has more than one session-config element!");
            } 
            descriptor.setSessionTimeout(Integer.parseInt(value.trim()));
        } else if (WebTagNames.TRACKING_MODE.equals(element.getQName())) {
            descriptor.addTrackingMode(value);
        } else {
            super.setElementValue(element, value);
        }
    }      

    /**
     * write the descriptor class to a DOM tree and return it
     *
     * @param parent node in the DOM tree 
     * @param node name for the root element of this xml fragment      
     * @param the descriptor to write
     * @return the DOM tree top node
     */
    public Node writeDescriptor(Node parent, String nodeName, SessionConfigDescriptor descriptor) {
        Node myNode = appendChild(parent, nodeName);
        if (descriptor.getSessionTimeout() != descriptor.SESSION_TIMEOUT_DEFAULT) {
            appendTextChild(myNode, WebTagNames.SESSION_TIMEOUT, 
                    String.valueOf(descriptor.getSessionTimeout()));
        }
        CookieConfigDescriptor cookieConfigDesc = (CookieConfigDescriptor)descriptor.getCookieConfig();
        if (cookieConfigDesc != null) {
            CookieConfigNode cookieConfigNode = new CookieConfigNode();
            cookieConfigNode.writeDescriptor(myNode, WebTagNames.COOKIE_CONFIG,
                cookieConfigDesc);
        }

        if (descriptor.getTrackingModes().size() > 0) {
            for (Enum tmEnum : descriptor.getTrackingModes()) {
                appendTextChild(myNode, WebTagNames.TRACKING_MODE, tmEnum.name());
            }
        }
        return myNode;
    }
}

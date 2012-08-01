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

package org.glassfish.web.deployment.node.runtime.gf;

import com.sun.enterprise.deployment.node.XMLElement;
import com.sun.enterprise.deployment.node.runtime.RuntimeDescriptorNode;
import com.sun.enterprise.deployment.xml.RuntimeTagNames;
import org.glassfish.web.deployment.runtime.SessionConfig;
import org.w3c.dom.Node;

/**
* superclass node for WebProperty container 
*
* @author Jerome Dochez
*/

public class SessionConfigNode extends RuntimeDescriptorNode<SessionConfig> {

    protected SessionConfig descriptor = null;

    /**
     * @return the descriptor instance to associate with this XMLNode
     */
    @Override
    public SessionConfig getDescriptor() {
        if (descriptor==null) {
            descriptor = new SessionConfig();
        }
        return descriptor;
    }

    /**
     * Initialize the child handlers
     */
    public SessionConfigNode() {
        registerElementHandler(new XMLElement(RuntimeTagNames.SESSION_MANAGER), 
                               SessionManagerNode.class, "setSessionManager");	
        registerElementHandler(new XMLElement(RuntimeTagNames.SESSION_PROPERTIES), 
                               SessionPropertiesNode.class, "setSessionProperties");
        registerElementHandler(new XMLElement(RuntimeTagNames.COOKIE_PROPERTIES), 
                               CookiePropertiesNode.class, "setCookieProperties");
    }
    
    /**
     * write the descriptor class to a DOM tree and return it
     *
     * @param parent node for the DOM tree
     * @param node name 
     * @param the descriptor to write
     * @return the DOM tree top node
     */    
    public Node writeDescriptor(Node parent, String nodeName, SessionConfig descriptor) {
	Node sessionConfig = super.writeDescriptor(parent, nodeName, descriptor);

	// session-manager?
	if (descriptor.getSessionManager()!=null) {
	    SessionManagerNode smn = new SessionManagerNode();
	    smn.writeDescriptor(sessionConfig, RuntimeTagNames.SESSION_MANAGER, descriptor.getSessionManager());
	}
	
	// session-properties?
	if (descriptor.getSessionProperties()!=null) {
	    WebPropertyNode wpn = new WebPropertyNode();
	    Node sessionProps = appendChild(sessionConfig, RuntimeTagNames.SESSION_PROPERTIES);
	    wpn.writeDescriptor(sessionProps, RuntimeTagNames.PROPERTY, descriptor.getSessionProperties().getWebProperty());
	}
	
	// cookie-properties?
	if (descriptor.getCookieProperties()!=null) {
	    WebPropertyNode wpn = new WebPropertyNode();
	    Node cookieProps = appendChild(sessionConfig, RuntimeTagNames.COOKIE_PROPERTIES);
	    wpn.writeDescriptor(cookieProps, RuntimeTagNames.PROPERTY, descriptor.getCookieProperties().getWebProperty());
	}
	
	return sessionConfig;
    }
}

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

/*
 * ServletNode.java
 *
 * Created on March 7, 2002, 2:30 PM
 */

package org.glassfish.web.deployment.node.runtime.gf;

import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.deployment.WebComponentDescriptor;
import com.sun.enterprise.deployment.WebServicesDescriptor;
import com.sun.enterprise.deployment.node.DeploymentDescriptorNode;
import com.sun.enterprise.deployment.node.XMLElement;
import com.sun.enterprise.deployment.node.runtime.WebServiceEndpointRuntimeNode;
import com.sun.enterprise.deployment.xml.RuntimeTagNames;
import com.sun.enterprise.deployment.xml.WebServicesTagNames;
import org.glassfish.web.deployment.descriptor.WebComponentDescriptorImpl;
import org.w3c.dom.Node;

/**
 * This node is handling all runtime deployment descriptors 
 * relative to servlets
 *
 * @author  Jerome Dochez
 * @version 
 */
public class ServletNode extends DeploymentDescriptorNode<WebComponentDescriptor> {

    protected WebComponentDescriptor descriptor;

    public ServletNode() {
        registerElementHandler(new XMLElement
            (WebServicesTagNames.WEB_SERVICE_ENDPOINT), 
                               WebServiceEndpointRuntimeNode.class);
    }

    /**
     * @return the descriptor instance to associate with this XMLNode
     */
    @Override
    public WebComponentDescriptor getDescriptor() {
        if (descriptor==null) {
            descriptor = new WebComponentDescriptorImpl();
        }
        return descriptor;
    }
    
    /**
     * receives notiification of the value for a particular tag
     * 
     * @param element the xml element
     * @param value it's associated value
     */
    @Override
    public void setElementValue(XMLElement element, String value) {
        if (RuntimeTagNames.SERVLET_NAME.equals(element.getQName())) {
            Object parentDesc = getParentNode().getDescriptor();
            if (parentDesc instanceof WebBundleDescriptor) {
                descriptor = ((WebBundleDescriptor) parentDesc).getWebComponentByCanonicalName(value);
            }
        } else if (RuntimeTagNames.PRINCIPAL_NAME.equals(element.getQName())) {
            if (descriptor!=null && descriptor.getRunAsIdentity()!=null) {
                descriptor.getRunAsIdentity().setPrincipal(value);
            }
        } else super.setElementValue(element, value);
    }
    

    /**
     * write the descriptor class to a DOM tree and return it
     *
     * @param parent node for the DOM tree
     * @param nodeName node name
     * @param descriptor the descriptor to write
     * @return the DOM tree top node
     */
    @Override
    public Node writeDescriptor(Node parent, String nodeName, WebComponentDescriptor descriptor) {
        WebServicesDescriptor webServices = 
            descriptor.getWebBundleDescriptor().getWebServices();

        // only write servlet runtime elements if there is a runas identity
        // or the servlet is exposed as a web service
        if ( (descriptor.getRunAsIdentity() != null) ||
             webServices.hasEndpointsImplementedBy(descriptor) ) {
            Node servletNode =  appendChild(parent, nodeName);
            appendTextChild(servletNode, RuntimeTagNames.SERVLET_NAME, descriptor.getCanonicalName());

            if( descriptor.getRunAsIdentity() != null ) {
                appendTextChild(servletNode, RuntimeTagNames.PRINCIPAL_NAME, 
                                descriptor.getRunAsIdentity().getPrincipal());
            }

            WebServiceEndpointRuntimeNode wsRuntime = 
                new WebServiceEndpointRuntimeNode();
            wsRuntime.writeWebServiceEndpointInfo(servletNode, descriptor);

            return servletNode;
        }
        return null;
    }
}

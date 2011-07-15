/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.deployment.node.ws;

import com.sun.enterprise.deployment.NameValuePairDescriptor;
import com.sun.enterprise.deployment.ServiceRefPortInfo;
import com.sun.enterprise.deployment.ServiceReferenceDescriptor;
import com.sun.enterprise.deployment.node.DeploymentDescriptorNode;
import com.sun.enterprise.deployment.node.NameValuePairNode;
import com.sun.enterprise.deployment.node.XMLElement;
import com.sun.enterprise.deployment.node.runtime.common.MessageSecurityBindingNode;
import com.sun.enterprise.deployment.runtime.common.MessageSecurityBindingDescriptor;
import com.sun.enterprise.deployment.xml.WebServicesTagNames;
import org.w3c.dom.Node;

import javax.xml.namespace.QName;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * This node is responsible for handling runtime info for a service reference wsdl port from weblogic DD.
 *
 * @author Rama Pulavarthi
 */
public class WLServiceRefPortInfoRuntimeNode extends DeploymentDescriptorNode {
    ServiceRefPortInfo descriptor = null;

    public WLServiceRefPortInfoRuntimeNode() {
        super();
        registerElementHandler
                (new XMLElement(WebServicesTagNames.STUB_PROPERTY),
                        NameValuePairNode.class, "addStubProperty");
        registerElementHandler
                (new XMLElement(WebServicesTagNames.CALL_PROPERTY),
                        NameValuePairNode.class, "addCallProperty");
    }

    @Override
    public Object getDescriptor() {
        return descriptor;
    }

    /**
     * receives notiification of the value for a particular tag
     *
     * @param element the xml element
     * @param value   it's associated value
     */

    public void setElementValue(XMLElement element, String value) {
        String name = element.getQName();
        if (WLWebServicesTagNames.SERVICE_REFERENCE_PORT_NAME.equals(name)) {
            ServiceReferenceDescriptor serviceRef = ((ServiceReferenceDescriptor) getParentNode().getDescriptor());
            //WLS-DD does not provide a way to specify ns uri of the port, so use the service ns uri
            String namespaceUri = serviceRef.getServiceNamespaceUri();
            QName wsdlPort = new QName(namespaceUri, value);
            descriptor = serviceRef.getPortInfoByPort(wsdlPort);
        } else super.setElementValue(element, value);

    }

    /**
     * write the descriptor class to a DOM tree and return it
     *
     * @param parent   node for the DOM tree
     * @param nodeName node name for the descriptor
     * @param desc     the descriptor to write
     * @return the DOM tree top node
     */
    public Node writeDescriptor(Node parent, String nodeName,
                                ServiceRefPortInfo desc) {
        Node serviceRefPortInfoRuntimeNode =
                super.writeDescriptor(parent, nodeName, desc);

        QName port = desc.getWsdlPort();

        if (port != null) {
            appendTextChild(serviceRefPortInfoRuntimeNode,
                    WLWebServicesTagNames.SERVICE_REFERENCE_PORT_NAME,
                    port.getLocalPart());


            // stub-property*
            NameValuePairNode nameValueNode = new NameValuePairNode();

            Set stubProperties = desc.getStubProperties();
            for (Iterator iter = stubProperties.iterator(); iter.hasNext();) {
                NameValuePairDescriptor next = (NameValuePairDescriptor) iter.next();
                nameValueNode.writeDescriptor
                        (serviceRefPortInfoRuntimeNode,
                                WebServicesTagNames.STUB_PROPERTY, next);
            }

            // call-property*
            for (Iterator iter = desc.getCallProperties().iterator();
                 iter.hasNext();) {
                NameValuePairDescriptor next = (NameValuePairDescriptor) iter.next();
                nameValueNode.writeDescriptor
                        (serviceRefPortInfoRuntimeNode,
                                WebServicesTagNames.CALL_PROPERTY, next);
            }

        }
        return serviceRefPortInfoRuntimeNode;
    }

}

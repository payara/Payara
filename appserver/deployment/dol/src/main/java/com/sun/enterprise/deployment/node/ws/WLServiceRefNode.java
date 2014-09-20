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

package com.sun.enterprise.deployment.node.ws;

import com.sun.enterprise.deployment.*;
import com.sun.enterprise.deployment.node.DeploymentDescriptorNode;
import com.sun.enterprise.deployment.node.NameValuePairNode;
import com.sun.enterprise.deployment.node.XMLElement;
import com.sun.enterprise.deployment.node.runtime.ServiceRefPortInfoRuntimeNode;
import com.sun.enterprise.deployment.runtime.web.SunWebApp;
import com.sun.enterprise.deployment.util.DOLUtils;
import com.sun.enterprise.deployment.xml.WebServicesTagNames;
import org.w3c.dom.Node;

import javax.xml.namespace.QName;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * This node is responsible for handling runtime info for service references from weblogic DD
 *
 * @author Rama Pulavarthi
 */
public class WLServiceRefNode extends DeploymentDescriptorNode {

    private ServiceReferenceDescriptor descriptor;

    public WLServiceRefNode() {
        super();
        registerElementHandler
                (new XMLElement(WLWebServicesTagNames.SERVICE_REFERENCE_PORT_INFO),
                        WLServiceRefPortInfoRuntimeNode.class);
        registerElementHandler
                (new XMLElement(WebServicesTagNames.CALL_PROPERTY),
                        NameValuePairNode.class, "addCallProperty");
    }

    public void addDescriptor(Object desc) {
        if (desc instanceof ServiceRefPortInfo) {
            ServiceRefPortInfo newPortInfo = (ServiceRefPortInfo) desc;
            ServiceReferenceDescriptor serviceRef =
                    (ServiceReferenceDescriptor) getDescriptor();
            serviceRef.addRuntimePortInfo(newPortInfo);
        }
    }

    /**
     * @return the descriptor instance to associate with this XMLNode
     */
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
        if (WebServicesTagNames.SERVICE_REF_NAME.equals(name)) {
            Object parentDesc = getParentNode().getDescriptor();
            if (parentDesc instanceof JndiNameEnvironment) {
                descriptor = ((JndiNameEnvironment) parentDesc).
                        getServiceReferenceByName(value);
            } else if (parentDesc instanceof WebBundleDescriptor) {
                WebBundleDescriptor desc = (WebBundleDescriptor)parentDesc;
                descriptor = desc.getServiceReferenceByName(value);
            }
        } else if (WLWebServicesTagNames.SERVICE_REFERENCE_WSDL_URL.equals(name)) {
            try {
                URL url = new URL(value);
                descriptor.setWsdlOverride(url);
            } catch (MalformedURLException mue) {
                DOLUtils.getDefaultLogger().log(Level.INFO,
                        "Warning : Invalid wsdl override url=" + value, mue);
            }
        } else {
            super.setElementValue(element, value);
        }

    }

    /**
     * write the descriptor class to a DOM tree and return it
     *
     * @param parent     node for the DOM tree
     * @param nodeName   for the descriptor
     * @param serviceRef the descriptor to write
     * @return the DOM tree top node
     */
    public Node writeDescriptor(Node parent, String nodeName,
                                ServiceReferenceDescriptor serviceRef) {
        Node serviceRefNode =
                super.writeDescriptor(parent, nodeName, serviceRef);

        appendTextChild(serviceRefNode, WebServicesTagNames.SERVICE_REF_NAME,
                serviceRef.getName());

        if (serviceRef.hasWsdlOverride()) {
            URL wsdlOverride = serviceRef.getWsdlOverride();
            appendTextChild(serviceRefNode, WLWebServicesTagNames.SERVICE_REFERENCE_WSDL_URL,
                    wsdlOverride.toExternalForm());
        }

        NameValuePairNode nameValueNode = new NameValuePairNode();
        for (Iterator iter = serviceRef.getCallProperties().iterator();
             iter.hasNext();) {
            NameValuePairDescriptor next = (NameValuePairDescriptor) iter.next();
            nameValueNode.writeDescriptor
                    (serviceRefNode, WebServicesTagNames.CALL_PROPERTY, next);
        }

        WLServiceRefPortInfoRuntimeNode portInfoRuntimeNode =
                new WLServiceRefPortInfoRuntimeNode();

        Set portsInfo = serviceRef.getPortsInfo();
        for (Iterator iter = portsInfo.iterator(); iter.hasNext();) {
            ServiceRefPortInfo next = (ServiceRefPortInfo) iter.next();
            portInfoRuntimeNode.writeDescriptor
                    (serviceRefNode, WLWebServicesTagNames.SERVICE_REFERENCE_PORT_NAME, next);
        }

        return serviceRefNode;
    }

    /**
     * writes all the runtime information for service references
     *
     * @param parent     node to add the runtime xml info
     * @param descriptor the J2EE component containing service references
     */
    public static void writeServiceReferences(Node parent,
                                              JndiNameEnvironment descriptor) {
        Iterator serviceRefs =
                descriptor.getServiceReferenceDescriptors().iterator();
        if (serviceRefs.hasNext()) {
            WLServiceRefNode serviceRefNode = new WLServiceRefNode();
            while (serviceRefs.hasNext()) {
                ServiceReferenceDescriptor next =
                        (ServiceReferenceDescriptor) serviceRefs.next();
                // Only write runtime service-ref entry if there IS 
                // some runtime info...
                if (!next.getPortsInfo().isEmpty() ||
                        !next.getCallProperties().isEmpty() ||
                        next.hasWsdlOverride()) {
                    serviceRefNode.writeDescriptor
                            (parent, WLWebServicesTagNames.SERVICE_REFERENCE_DESCRIPTION, next);
                }
            }
        }
    }

}

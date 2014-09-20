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
import com.sun.enterprise.deployment.node.*;
import com.sun.enterprise.deployment.util.DOLUtils;
import com.sun.enterprise.deployment.xml.WebServicesTagNames;
import org.w3c.dom.Node;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.logging.Level;

/**
 * This node represents webservice-description node in weblogic-webservices.xml
 *
 * @author Rama Pulavarthi
 */
public class WLWebServiceNode extends DisplayableComponentNode {

    private WebService descriptor = null;
    private String serviceDescriptionName;
    private final static XMLElement tag =
            new XMLElement(WLWebServicesTagNames.WEB_SERVICE);

    public WLWebServiceNode() {
        registerElementHandler(new XMLElement(WLWebServicesTagNames.PORT_COMPONENT),
                WLWebServiceEndpointNode.class);

    }

    /**
     * all sub-implementation of this class can use a dispatch table
     * to map xml element to method name on the descriptor class for
     * setting the element value.
     *
     * @return map with the element name as a key, the setter method as a value
     */
    @Override
    protected Map getDispatchTable() {
        Map table = super.getDispatchTable();
        //table.put(WebServicesTagNames.WEB_SERVICE_DESCRIPTION_NAME,"setName");
        table.put(WLWebServicesTagNames.WEBSERVICE_TYPE, "setType");
        return table;
    }


    /**
     * receives notiification of the value for a particular tag
     *
     * @param element the xml element
     * @param value   it's associated value
     */
    @Override
    public void setElementValue(XMLElement element, String value) {
        if (WLWebServicesTagNames.WEB_SERVICE_DESCRIPTION_NAME.equals
                (element.getQName())) {
            WebServicesDescriptor webServices = (WebServicesDescriptor) getParentNode().getDescriptor();
            descriptor = webServices.getWebServiceByName(value);
            serviceDescriptionName = value;
        } else {
            if (descriptor == null) {
                    DOLUtils.getDefaultLogger().severe
                            ("Warning : WebService descriptor cannot be found webservice-description-name "
                                    + serviceDescriptionName);
                    throw new RuntimeException("DeploymentException: WebService descriptor cannot be found for webservice-description-name:" +
                            serviceDescriptionName +" specified in weblogic-webservices.xml");
            }

            if (WLWebServicesTagNames.WSDL_PUBLISH_FILE.equals
                    (element.getQName())) {
                URL url = null;
                try {
                    url = new URL(value);
                } catch (MalformedURLException e) {
                    try {
                        //try file
                        url = new File(value).toURI().toURL();
                    } catch (MalformedURLException mue) {
                        DOLUtils.getDefaultLogger().log(Level.INFO,
                                "Warning : Invalid final wsdl url=" + value, mue);
                    }
                }
                if (url != null) {
                    descriptor.setClientPublishUrl(url);
                }
            } else {
                super.setElementValue(element, value);
            }
        }
    }

    @Override
    public Object getDescriptor() {
        return descriptor;
    }

    /**
     * @return the XML tag associated with this XMLNode
     */
    @Override
    protected XMLElement getXMLRootTag() {
        return tag;
    }

    /**
     * Adds  a new DOL descriptor instance to the descriptor
     * instance associated with this XMLNode
     *
     * @param descriptor the new descriptor
     */
    @Override
    public void addDescriptor(Object descriptor) {
        WebServiceEndpoint endpoint = (WebServiceEndpoint) descriptor;
        WebService webService = (WebService) getDescriptor();
        webService.addEndpoint(endpoint);
    }

    public Node writeDescriptor(Node parent, String nodeName,
                                WebService descriptor) {
        Node topNode =
                super.writeDescriptor(parent, nodeName, descriptor);
        
        //TODO is this needed?
        //writeDisplayableComponentInfo(topNode, descriptor);

        appendTextChild(topNode,
                WebServicesTagNames.WEB_SERVICE_DESCRIPTION_NAME,
                descriptor.getName());
        appendTextChild(topNode, WLWebServicesTagNames.WEBSERVICE_TYPE,
                descriptor.getType());
        if (descriptor.getClientPublishUrl() != null) {
            appendTextChild(topNode, WLWebServicesTagNames.WSDL_PUBLISH_FILE,
                descriptor.getClientPublishUrl().toString());
        }

        WLWebServiceEndpointNode endpointNode = new WLWebServiceEndpointNode();
        for (WebServiceEndpoint next : descriptor.getEndpoints()) {
            endpointNode.writeDescriptor
                    (topNode, WebServicesTagNames.PORT_COMPONENT, next);
        }

        return topNode;
    }

}

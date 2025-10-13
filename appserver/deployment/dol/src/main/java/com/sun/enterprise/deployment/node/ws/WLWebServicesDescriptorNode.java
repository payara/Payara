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
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2018-2019] [Payara Foundation and/or its affiliates]
package com.sun.enterprise.deployment.node.ws;

import static com.sun.enterprise.deployment.node.ws.WLDescriptorConstants.WL_WEBSERVICES_SCHEMA_LOCATION;
import static com.sun.enterprise.deployment.node.ws.WLDescriptorConstants.WL_WEBSERVICES_XML_SCHEMA;
import static com.sun.enterprise.deployment.node.ws.WLWebServicesTagNames.WEBSERVICE_SECURITY;
import static com.sun.enterprise.deployment.node.ws.WLWebServicesTagNames.WEB_SERVICE;
import static com.sun.enterprise.deployment.node.ws.WLWebServicesTagNames.WEB_SERVICES;
import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.glassfish.deployment.common.RootDeploymentDescriptor;
import org.jvnet.hk2.annotations.Service;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.sun.enterprise.deployment.WebService;
import com.sun.enterprise.deployment.WebServicesDescriptor;
import com.sun.enterprise.deployment.node.AbstractBundleNode;
import com.sun.enterprise.deployment.node.SaxParserHandler;
import com.sun.enterprise.deployment.node.XMLElement;
import com.sun.enterprise.deployment.node.XMLNode;
import com.sun.enterprise.deployment.xml.TagNames;
import com.sun.enterprise.deployment.xml.WebServicesTagNames;

/**
 * Node representing weblogic-webservices root element in weblogic-webservices.xml
 *
 * @author Rama Pulavarthi
 */
@Service
public class WLWebServicesDescriptorNode extends AbstractBundleNode {

    public WLWebServicesDescriptorNode(WebServicesDescriptor descriptor) {
        this();
        parentDescriptor = descriptor;
    }

    public WLWebServicesDescriptorNode() {
        registerElementHandler(new XMLElement(WEB_SERVICE), WLWebServiceNode.class);
        registerElementHandler(new XMLElement(WEBSERVICE_SECURITY), WLUnSupportedNode.class);
        SaxParserHandler.registerBundleNode(this, WEB_SERVICES);
    }

    private final static XMLElement ROOT_ELEMENT = new XMLElement(WEB_SERVICES);

    private final static String SCHEMA_ID = WL_WEBSERVICES_XML_SCHEMA;
    private final static String SPEC_VERSION = "1.0";
    private final static List<String> systemIDs = initSystemIDs();

    private static List<String> initSystemIDs() {
        List<String> systemIDs = new ArrayList<>();
        systemIDs.add(SCHEMA_ID);
        
        return unmodifiableList(systemIDs);
    }

    private WebServicesDescriptor parentDescriptor;

    /**
     * @return the DOCTYPE of the XML file
     */
    public String getDocType() {
        return null;
    }

    /**
     * @return the SystemID of the XML file
     */
    public String getSystemID() {
        return SCHEMA_ID;
    }

    /**
     * @return the list of SystemID of the XML schema supported
     */
    public List<String> getSystemIDs() {
        return systemIDs;
    }

    @Override
    public String registerBundle(Map<String, String> publicIDToSystemIDMapping) {
        return ROOT_ELEMENT.getQName();
    }

    @Override
    public Map<String, Class<?>> registerRuntimeBundle(Map<String, String> publicIDToDTD, Map<String, List<Class<?>>> versionUpgrades) {
        return emptyMap();
    }

    /**
     * @return the complete URL for J2EE schemas
     */
    protected String getSchemaURL() {
        return WL_WEBSERVICES_SCHEMA_LOCATION;
    }

    /**
     * @return the XML tag associated with this XMLNode
     */
    protected XMLElement getXMLRootTag() {
        return ROOT_ELEMENT;
    }

    @Override
    protected boolean setAttributeValue(XMLElement elementName, XMLElement attributeName, String value) {
        // We do not support id attribute for the moment
        if (attributeName.getQName().equals(TagNames.ID)) {
            return true;
        }

        if (TagNames.VERSION.equals(attributeName.getQName()) && SPEC_VERSION.equals(value)) {
            return true;
        }

        return false;
    }

    @Override
    public XMLNode getHandlerFor(XMLElement element) {
        if (WEBSERVICE_SECURITY.equals(element.getQName())) {
            throw new UnsupportedConfigurationException(element + " configuration in weblogic-webservices.xml is not supported.");
        }
        
        return super.getHandlerFor(element);
    }

    @Override
    public void addDescriptor(Object descriptor) {
        // None of the sub nodes should call addDescriptor() on this node.
        // as this configuration only supplements webservices.xml configuration and
        // does not create new web services.
        // DOLUtils.getDefaultLogger().info("Warning: WLWebServiceDescriptorNode.addDescriptor() should not have been called by"
        // + descriptor.toString());

    }

    /**
     * @return the descriptor instance to associate with this XMLNode
     */
    public WebServicesDescriptor getDescriptor() {
        return parentDescriptor;
    }

    public Node writeDescriptor(Node parent, RootDeploymentDescriptor descriptor) {
        Node bundleNode;
        if (getDocType() == null) {
            // we are using schemas for this DDs
            bundleNode = appendChildNS(parent, getXMLRootTag().getQName(), WLDescriptorConstants.WL_WEBSERVICES_XML_NS);

            addBundleNodeAttributes((Element) bundleNode, descriptor);
        } else {
            bundleNode = appendChild(parent, getXMLRootTag().getQName());
        }

        // Description, display-name, icons...
        writeDisplayableComponentInfo(bundleNode, descriptor);

        if (descriptor instanceof WebServicesDescriptor) {
            WLWebServiceNode wsNode = new WLWebServiceNode();
            for (WebService next : ((WebServicesDescriptor) descriptor).getWebServices()) {
                wsNode.writeDescriptor(bundleNode, WebServicesTagNames.WEB_SERVICE, next);
            }
        }
        return bundleNode;
    }

    @Override
    protected void addBundleNodeAttributes(Element bundleNode, RootDeploymentDescriptor descriptor) {
        String schemaLocation;
        // the latest connector schema still use j2ee namespace
        bundleNode.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns", WLDescriptorConstants.WL_WEBSERVICES_XML_NS);
        bundleNode.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:j2ee", TagNames.J2EE_NAMESPACE);

        schemaLocation = WLDescriptorConstants.WL_WEBSERVICES_XML_NS + " " + getSchemaURL();
        schemaLocation = schemaLocation + " " + TagNames.J2EE_NAMESPACE + " " + "http://java.sun.com/xml/ns/j2ee/web-app_2_4.xsd";
        bundleNode.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:xsi", W3C_XML_SCHEMA_INSTANCE);

        // add all custom global namespaces
        addNamespaceDeclaration(bundleNode, descriptor);
        String clientSchemaLocation = descriptor.getSchemaLocation();
        if (clientSchemaLocation != null) {
            schemaLocation = schemaLocation + " " + clientSchemaLocation;
        }
        bundleNode.setAttributeNS(W3C_XML_SCHEMA_INSTANCE, SCHEMA_LOCATION_TAG, schemaLocation);
        bundleNode.setAttribute(TagNames.VERSION, getSpecVersion());
    }

    /**
     * @return the default spec version level this node complies to
     */
    public String getSpecVersion() {
        return SPEC_VERSION;
    }

}

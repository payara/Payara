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
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
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
// Portions Copyright [2018-2020] [Payara Foundation and/or its affiliates]

package com.sun.enterprise.deployment.node;

import com.sun.enterprise.deployment.JaxrpcMappingDescriptor;
import com.sun.enterprise.deployment.xml.WebServicesTagNames;
import org.jvnet.hk2.annotations.Service;
import org.xml.sax.Attributes;

import static java.util.Collections.emptyMap;

import java.util.*;

/**
 * Root node for jaxrpc mapping deployment descriptor
 *
 * @author Kenneth Saks
 * @version
 */
@Service
public class JaxrpcMappingDescriptorNode extends AbstractBundleNode {

    public final static XMLElement ROOT_ELEMENT = new XMLElement(WebServicesTagNames.JAXRPC_MAPPING_FILE_ROOT);

    public final static String SCHEMA_ID = "j2ee_jaxrpc_mapping_1_1.xsd";
    private final static List<String> systemIDs = initSystemIDs();

    private static final Set<String> complexElements = initComplexElements();
    private JaxrpcMappingDescriptor descriptor = null;
    private String javaPackage = null;

    // true if mapping file contains more than just package->namespace mappings.
    private boolean complexMapping = false;

    private static Set<String> initComplexElements() {
        Set<String> complexElements = new HashSet<>();
        complexElements.add(WebServicesTagNames.JAVA_XML_TYPE_MAPPING);
        complexElements.add(WebServicesTagNames.EXCEPTION_MAPPING);
        complexElements.add(WebServicesTagNames.SERVICE_INTERFACE_MAPPING);
        complexElements.add(WebServicesTagNames.SERVICE_ENDPOINT_INTERFACE_MAPPING);
        return Collections.unmodifiableSet(complexElements);
    }

    private static List<String> initSystemIDs() {
        ArrayList<String> systemIDs = new ArrayList<>();
        systemIDs.add(SCHEMA_ID);
        return Collections.unmodifiableList(systemIDs);
    }

    public JaxrpcMappingDescriptorNode() {
        descriptor = new JaxrpcMappingDescriptor();
        SaxParserHandler.registerBundleNode(this, WebServicesTagNames.JAXRPC_MAPPING_FILE_ROOT);
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
     * @return the XML tag associated with this XMLNode
     */
    @Override
    protected XMLElement getXMLRootTag() {
        return ROOT_ELEMENT;
    }

    /**
     * @return the DOCTYPE of the XML file
     */
    @Override
    public String getDocType() {
        return null;
    }

    /**
     * @return the SystemID of the XML file
     */
    @Override
    public String getSystemID() {
        return SCHEMA_ID;
    }

    /**
     * @return the list of SystemID of the XML schema supported
     */
    @Override
    public List<String> getSystemIDs() {
        return systemIDs;
    }

    /**
     * @return the complete URL for Jakarta EE schemas
     */
    @Override
    protected String getSchemaURL() {
        return WebServicesTagNames.IBM_NAMESPACE + "/" + getSystemID();
    }

    /**
     * @return the descriptor instance to associate with this XMLNode
     */
    @Override
    public Object getDescriptor() {
        return descriptor;
    }

    @Override
    public void startElement(XMLElement element, Attributes attributes) {
        if (complexMapping) {
            // NOTE : we don't call super.startElement in this case because
            // we don't need to process any of the attributes
            return;
        } else if (complexElements.contains(element.getQName())) {
            complexMapping = true;
            descriptor.setIsSimpleMapping(false);
            // NOTE : we don't call super.startElement in this case because
            // we don't need to process any of the attributes
        } else {
            super.startElement(element, attributes);
        }
    }

    /**
     * receives notiification of the value for a particular tag
     * 
     * @param element the xml element
     * @param value it's associated value
     */
    @Override
    public void setElementValue(XMLElement element, String value) {
        if (complexMapping) {
            // We only gather namespace->package mapping. In exhaustive(complex)
            // mapping case, it's enough to just capture the fact that we
            // have complex mapping info. The actual processing of the elements
            // will be done by mapping file modeler during deployment
            return;
        } else if (WebServicesTagNames.PACKAGE_TYPE.equals(element.getQName())) {
            javaPackage = value;
        } else if (WebServicesTagNames.NAMESPACE_URI.equals(element.getQName())) {
            descriptor.addMapping(javaPackage, value);
            javaPackage = null;
        } else {
            super.setElementValue(element, value);
        }
    }

    /**
     * @return the default spec version level this node complies to
     */
    @Override
    public String getSpecVersion() {
        return "1.1";
    }

}

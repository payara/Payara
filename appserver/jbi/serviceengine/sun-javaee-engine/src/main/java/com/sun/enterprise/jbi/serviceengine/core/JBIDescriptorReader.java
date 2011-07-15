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

package com.sun.enterprise.jbi.serviceengine.core;

import org.w3c.dom.*;

import javax.xml.namespace.QName;
import java.util.StringTokenizer;
import java.util.Map;


/**
 * This class reads the SU jbi.xml and loads up the values.
 *
 * @author Sun Microsystems
 */
public class JBIDescriptorReader {
    /**
     * Document object of the XML config file.
     */
    private Document mDoc;

    /**
     * List of endpoints in the SU jbi.xml.
     */
    private DescriptorEndpointInfo [] mEPList;

    /**
     * Number of consumers.
     */
    private int mNoOfConsumers;

    /**
     * Number of providers.
     */
    private int mNoOfProviders;

    /**
     * Type of deployment, WSDL, XML or WSDL11
     */
    private String mType;
    private String mSu_Name;
    private EndpointRegistry registry = EndpointRegistry.getInstance();
    private static final String prefix_NS =
            "http://javaee.serviceengine.sun.com/endpoint/naming/extension";
    private static final String ept_Mappings = "ept-mappings";
    private static final String private_Endpoints = "private-endpoints";
//    private static final String ept_Mapping = "ept-mapping";
    private Map<String, DescriptorEndpointInfo> wsdlEndpts = registry.getWSDLEndpts();
    private Map<String, DescriptorEndpointInfo> jbiEndpts = registry.getJBIEndpts();

    /**
     * Creates a new ConfigReader object.
     */
    JBIDescriptorReader(String su_Name) {
        mSu_Name = su_Name;
    }


    DescriptorEndpointInfo[] getEndpoints() {
        return mEPList;
    }

    /**
     * Returns the number of consumer endpoints.
     *
     * @return consumer endpoint count.
     */
    int getConsumerCount() {
        return mNoOfConsumers;
    }

    /**
     * Sets the endpoitn attributes.
     *
     * @param node provider/consumer node.
     * @param ep   endpoint information.
     */
    private void setEndpoint(Node node, DescriptorEndpointInfo ep) {
        NamedNodeMap map = node.getAttributes();

        String epname = map.getNamedItem("endpoint-name").getNodeValue();
        String sername = map.getNamedItem("service-name").getNodeValue();
        String intername = map.getNamedItem("interface-name").getNodeValue();
        ep.setServiceName(new QName(getNamespace(sername), getLocalName(sername)));
        ep.setInterfaceName(new QName(getNamespace(intername), getLocalName(intername)));
        ep.setEndpointName(epname);
    }

    /**
     * Sets the type of artifact.
     */
    private void setType(String type) {
        mType = type;
    }

    /**
     * Gets the type of artifact
     */

    String getType() {
        return mType;
    }

    private void setArtifacts() {
        NodeList namelist = mDoc.getElementsByTagNameNS("*", "artifactstype");

        if (namelist == null) {
            /* This means the tag is not present. default type is WSDL20
             */
            setType("WSDL20");
            return;
        }

        Element name = (Element) namelist.item(0);
        String sValue;

        try {
            sValue = (name.getChildNodes().item(0)).getNodeValue().trim();
        } catch (NullPointerException ne) {
            setType("WSDL20");
            return;
        }

        setType(sValue);
    }

    /**
     * Returns the number of provider endpoints.
     *
     * @return provider endpoint count.
     */
    int getProviderCount() {
        return mNoOfProviders;
    }

    /**
     * Initializes the config file and loads services.
     *
     * @param doc Name of the config file.
     */
    void init(Document doc) {
        mDoc = doc;
        mDoc.getDocumentElement().normalize();
        load();
    }

    /**
     * Loads the data.
     */
    void load() {
        NodeList providers = mDoc.getElementsByTagName("provides");
        mNoOfProviders = providers.getLength();

        NodeList consumers = mDoc.getElementsByTagName("consumes");
        mNoOfConsumers = consumers.getLength();
        mEPList = new DescriptorEndpointInfo[mNoOfConsumers + mNoOfProviders];
        setArtifacts();
        for (int i = 0; i < mNoOfProviders; i++) {
            Node node = providers.item(i);
            DescriptorEndpointInfo sb = new DescriptorEndpointInfo(mSu_Name);
            setEndpoint(node, sb);
            sb.setProvider();
            mEPList[i] = sb;
        }

        for (int i = 0; i < mNoOfConsumers; i++) {
            Node node = consumers.item(i);
            DescriptorEndpointInfo sb = new DescriptorEndpointInfo(mSu_Name);
            setEndpoint(node, sb);
            mEPList[i + mNoOfProviders] = sb;
        }

        NodeList eptMappings = mDoc.getElementsByTagNameNS(prefix_NS, ept_Mappings);
        if(eptMappings.getLength() > 0)
            eptMappings = eptMappings.item(0).getChildNodes();
        for(int i = 0; i < eptMappings.getLength(); i++) {
            if(!eptMappings.item(i).getNodeName().contains("ept-mapping"))
                continue;
            NodeList nList = eptMappings.item(i).getChildNodes();
            DescriptorEndpointInfo dei = new DescriptorEndpointInfo(mSu_Name);
            DescriptorEndpointInfo dei1 = new DescriptorEndpointInfo(mSu_Name);
            for (int j = 0; j < nList.getLength(); j++) {
                Node node = nList.item(j);
                if(node.getNodeName().contains("java-ept")) {
                    setEndpoint(node, dei);
                    String type =
                            node.getAttributes().getNamedItem("type").getNodeValue();
                    if(type.equals("provider")) {
                        dei.setProvider();
                        dei1.setProvider();
                    }
                } else if(node.getNodeName().contains("wsdl-ept"))
                    setEndpoint(node, dei1);
            }
            wsdlEndpts.put(dei.getKey(), dei1);
            jbiEndpts.put(dei1.getKey(), dei);
        }

        if (!wsdlEndpts.isEmpty()) {
            for (int i = 0; i < mEPList.length; i++) {
                DescriptorEndpointInfo dei = mEPList[i];
                if(wsdlEndpts.get(dei.getKey())!=null)
                    mEPList[i] = wsdlEndpts.get(dei.getKey());
            }
        }
        processPrivateEndpoints();
    }

    private void processPrivateEndpoints() {
        NodeList privateEndpoints = mDoc.getElementsByTagNameNS(
                prefix_NS, private_Endpoints);
        if(privateEndpoints.getLength() > 0) {
            privateEndpoints = privateEndpoints.item(0).getChildNodes();
        }
        for(int i = 0; i < privateEndpoints.getLength(); i++) {
            Node privateEndpoint = privateEndpoints.item(i);
            if(!privateEndpoint.getNodeName().contains("private-endpoint")) {
                continue;
            }
            DescriptorEndpointInfo dei = new DescriptorEndpointInfo(mSu_Name);
            setEndpoint(privateEndpoint, dei);
            for(int j=0; j<mEPList.length; j++) {
                if(mEPList[j].equals(dei) && mEPList[j].isProvider()) {
                    mEPList[j].setPrivate(true);
                }
            }
        }
    }
    
    /**
     * Gets the local name from the quname.
     *
     * @param qname Qualified name of service.
     * @return String local name
     */
    private String getLocalName(String qname) {
        StringTokenizer tok = new StringTokenizer(qname, ":");

        if (tok.countTokens() == 1) {
            return qname;
        }
        tok.nextToken();

        return tok.nextToken();
    }

    /**
     * Gets the namespace from the qname.
     *
     * @param qname Qname of service
     * @return namespace namespace of service
     */
    private String getNamespace(String qname) {
        StringTokenizer tok = new StringTokenizer(qname, ":");
        String prefix;

        if (tok.countTokens() == 1) {
            return "";
        }
        prefix = tok.nextToken();

        NamedNodeMap map = mDoc.getDocumentElement().getAttributes();
        for (int j = 0; j < map.getLength(); j++) {
            Node n = map.item(j);

            if (n.getLocalName().trim().equals(prefix.trim())) {
                return n.getNodeValue();
            }
        }

        return "";
    }

}

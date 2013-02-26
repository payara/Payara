/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.webservices.node;

import com.sun.enterprise.deployment.*;
import com.sun.enterprise.deployment.node.AbstractBundleNode;
import com.sun.enterprise.deployment.node.SaxParserHandler;
import com.sun.enterprise.deployment.node.XMLElement;
import com.sun.enterprise.deployment.xml.TagNames;
import com.sun.enterprise.deployment.xml.WebServicesTagNames;
import java.util.Map;
import org.jvnet.hk2.annotations.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.webservices.connector.LogUtils;

/**
 * Root node for web services deployment descriptor
 *
 * @author  Kenneth Saks
 * @version 
 */
@Service
public class WebServicesDescriptorNode extends AbstractBundleNode<BundleDescriptor> {    
    public final static XMLElement ROOT_ELEMENT =
        new XMLElement(WebServicesTagNames.WEB_SERVICES);
    
    public final static String SCHEMA_ID = "javaee_web_services_1_4.xsd";
    public final static String SCHEMA_ID_12 = "javaee_web_services_1_2.xsd";
    public final static String SCHEMA_ID_13 = "javaee_web_services_1_3.xsd";
    public final static String SPEC_VERSION = "1.3";
    private final static List<String> systemIDs = initSystemIDs();
    private static final Logger logger = LogUtils.getLogger();

    private static List<String> initSystemIDs() {
        List<String> sysIDs = new ArrayList<String>();
        sysIDs.add(SCHEMA_ID);
        sysIDs.add(SCHEMA_ID_12);
        sysIDs.add(SCHEMA_ID_13);
        return Collections.unmodifiableList(sysIDs);

    }
    
    private BundleDescriptor bundleDescriptor;

    public WebServicesDescriptorNode(BundleDescriptor descriptor) {
        bundleDescriptor = descriptor;
        registerElementHandler(new XMLElement(WebServicesTagNames.WEB_SERVICE),
                               WebServiceNode.class);
        SaxParserHandler.registerBundleNode(this, WebServicesTagNames.WEB_SERVICES);
    }   

    public WebServicesDescriptorNode() {
        this(null);
    }

    @Override
    public String registerBundle(Map<String, String> publicIDToSystemIDMapping) {
        return ROOT_ELEMENT.getQName();
    }
    
    @Override
    public Map<String, Class> registerRuntimeBundle(Map<String, String> publicIDToSystemIDMapping, Map<String, List<Class>> versionUpgrades) {
        return Collections.EMPTY_MAP;
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
     * @return the XML tag associated with this XMLNode
     */
    @Override
    protected XMLElement getXMLRootTag() {
        return ROOT_ELEMENT;
    }
    
    /**
     * receives notiification of the value for a particular tag
     * 
     * @param element the xml element
     * @param value it's associated value
     */    
    @Override
    public void setElementValue(XMLElement element, String value) {    
        if (TagNames.VERSION.equals(element.getQName())) {    
            bundleDescriptor.getWebServices().setSpecVersion(value);
        } else super.setElementValue(element, value);
    }
        
    /**
     * Adds  a new DOL descriptor instance to the descriptor 
     * instance associated with this XMLNode
     *
     * @param descriptor the new descriptor
     */
    @Override
    public void addDescriptor(Object descriptor) {    
        WebServicesDescriptor webServicesDesc = 
            bundleDescriptor.getWebServices();
        WebService webService = (WebService) descriptor;
        webServicesDesc.addWebService(webService);
        
        for(Iterator iter = webService.getEndpoints().iterator(); 
            iter.hasNext();) {
            WebServiceEndpoint next = (WebServiceEndpoint) iter.next();
            if( !next.resolveComponentLink() ) {
                logger.log(Level.INFO, LogUtils.WS_COMP_LINK_NOT_VALID,
                        new Object[]{next.getEndpointName(), next.getLinkName()});
            }
        }
        
    }
    
   /**
    * @return the descriptor instance to associate with this XMLNode
    */    
    @Override
    public BundleDescriptor getDescriptor() {
        return bundleDescriptor;
    }     

    /**
     * write the descriptor class to a DOM tree and return it
     *
     * @param parent node for the DOM tree
     * @param descriptor to write
     * @return the DOM tree top node
     */    
    @Override
    public Node writeDescriptor(Node parent, BundleDescriptor descriptor) {
        if (parent instanceof Document) {
            Node topNode = super.writeDescriptor(parent, descriptor);
            WebServicesDescriptor webServicesDesc = descriptor.getWebServices();
            WebServiceNode wsNode = new WebServiceNode();
            for(WebService next : webServicesDesc.getWebServices()) {
                wsNode.writeDescriptor(topNode, WebServicesTagNames.WEB_SERVICE,
                                       next);
            }
        }
        return parent;
    }

    /**
     * @return the default spec version level this node complies to
     */
    @Override
    public String getSpecVersion() {
        return SPEC_VERSION;
    }
    
}


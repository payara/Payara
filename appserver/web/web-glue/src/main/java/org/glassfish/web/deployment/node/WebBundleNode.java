/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2017 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.deployment.*;
import com.sun.enterprise.deployment.io.ConfigurationDeploymentDescriptorFile;
import com.sun.enterprise.deployment.node.*;
import com.sun.enterprise.deployment.util.DOLUtils;
import com.sun.enterprise.deployment.xml.TagNames;
import org.glassfish.web.WarType;
import org.glassfish.web.deployment.descriptor.WebBundleDescriptorImpl;
import org.glassfish.web.deployment.xml.WebTagNames;
import org.w3c.dom.Node;

import java.util.*;

/**
 * This node is responsible for handling the web-app xml tree
 *
 * @author  Jerome Dochez
 * @version 
 */
public class WebBundleNode extends WebCommonNode<WebBundleDescriptorImpl> {

    public final static XMLElement tag = new XMLElement(WebTagNames.WEB_BUNDLE);

    /** 
     * The public ID for my documents.
     */
    public final static String PUBLIC_DTD_ID = "-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN";
    public final static String PUBLIC_DTD_ID_12 = "-//Sun Microsystems, Inc.//DTD Web Application 2.2//EN";
    /** 
     * The system ID of my documents. 
     */
    public final static String SYSTEM_ID = "http://java.sun.com/dtd/web-app_2_3.dtd";
    public final static String SYSTEM_ID_12 = "http://java.sun.com/dtd/web-app_2_2.dtd";   
    
    public final static String SCHEMA_ID_24 = "web-app_2_4.xsd";
    public final static String SCHEMA_ID_25 = "web-app_2_5.xsd";
    public final static String SCHEMA_ID_30 = "web-app_3_0.xsd";
    public final static String SCHEMA_ID_31 = "web-app_3_1.xsd";
    public final static String SCHEMA_ID = "web-app_4_0.xsd";
    private final static List<String> systemIDs = initSystemIDs();


    private static List<String> initSystemIDs() {
        List<String> systemIDs = new ArrayList<String>();
        systemIDs.add(SCHEMA_ID);
        systemIDs.add(SCHEMA_ID_24);
        systemIDs.add(SCHEMA_ID_25);
        systemIDs.add(SCHEMA_ID_30);
        systemIDs.add(SCHEMA_ID_31);
        return Collections.unmodifiableList(systemIDs);
    }
    
   /**
    * register this node as a root node capable of loading entire DD files
    * 
    * @param publicIDToDTD is a mapping between xml Public-ID to DTD 
    * @return the doctype tag name
    */
    @Override
    public String registerBundle(Map<String, String> publicIDToDTD) {
        publicIDToDTD.put(PUBLIC_DTD_ID, SYSTEM_ID);
        publicIDToDTD.put(PUBLIC_DTD_ID_12, SYSTEM_ID_12);
        return tag.getQName();
    }
    
    @Override
     public Map<String,Class> registerRuntimeBundle(final Map<String,String> publicIDToDTD, Map<String, List<Class>> versionUpgrades) {
        final Map<String,Class> result = new HashMap<String,Class>();
        for (ConfigurationDeploymentDescriptorFile wddFile :
                DOLUtils.getConfigurationDeploymentDescriptorFiles(
                        habitat, WarType.ARCHIVE_TYPE)) {

            wddFile.registerBundle(result, publicIDToDTD, versionUpgrades);
        }

        return result;
    }

    @Override
    public Collection<String> elementsAllowingEmptyValue() {
        final Set<String> result = new HashSet<String>();
        result.add(WebTagNames.LOAD_ON_STARTUP);
        return result;
    }

    @Override
    public Collection<String> elementsPreservingWhiteSpace() {
        final Set<String> result = new HashSet<String>();
        result.add(WebTagNames.URL_PATTERN);
        return result;
    }
    
    
    
    /** Creates new WebBundleNode */
    public WebBundleNode()  {
        super();
        registerElementHandler(new XMLElement(WebTagNames.ABSOLUTE_ORDERING),
               AbsoluteOrderingNode.class, "setAbsoluteOrderingDescriptor");
        SaxParserHandler.registerBundleNode(this, WebTagNames.WEB_BUNDLE);
    }

    @Override
    public void setElementValue(XMLElement element, String value) {
        if (TagNames.MODULE_NAME.equals(element.getQName())) {
            WebBundleDescriptor bundleDesc = getDescriptor();
            bundleDesc.getModuleDescriptor().setModuleName(value);
        } else if (WebTagNames.DEFAULT_CONTEXT_PATH.equals(element.getQName())) {
            WebBundleDescriptor bundleDesc = getDescriptor();
            bundleDesc.setContextRoot(value);
        } else {
            super.setElementValue(element, value);
        }
    }

    @Override
    public boolean endElement(XMLElement element) {
        if (WebTagNames.DENY_UNCOVERED_HTTP_METHODS.equals(element.getQName())) {
            descriptor.setDenyUncoveredHttpMethods(true);
            return false;
        } else {
            return super.endElement(element);
        }
    }
    
   /**
    * @return the descriptor instance to associate with this XMLNode
    */
    @Override
    public WebBundleDescriptorImpl getDescriptor() {
        if (descriptor==null) {
            descriptor = new WebBundleDescriptorImpl();
        }
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
     * write the descriptor class to a DOM tree and return it
     *
     * @param parent node for the DOM tree
     * @param the descriptor to write
     * @return the DOM tree top node
     */
    @Override
    public Node writeDescriptor(Node parent, 
        WebBundleDescriptorImpl webBundleDesc) {

        Node jarNode = super.writeDescriptor(parent, webBundleDesc);
        if (webBundleDesc.isDenyUncoveredHttpMethods()) {
            appendChild(jarNode, WebTagNames.DENY_UNCOVERED_HTTP_METHODS);
        }
        if (webBundleDesc.getAbsoluteOrderingDescriptor() != null) {
            AbsoluteOrderingNode absOrderingNode = new AbsoluteOrderingNode();
            absOrderingNode.writeDescriptor(jarNode, WebTagNames.ABSOLUTE_ORDERING,
                    webBundleDesc.getAbsoluteOrderingDescriptor());
        }
        return jarNode;
    }

}

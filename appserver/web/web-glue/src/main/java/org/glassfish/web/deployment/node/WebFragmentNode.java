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

package org.glassfish.web.deployment.node;

import com.sun.enterprise.deployment.node.*;
import org.glassfish.web.deployment.descriptor.WebFragmentDescriptor;
import org.glassfish.web.deployment.xml.WebTagNames;
import org.jvnet.hk2.annotations.Service;
import org.w3c.dom.Node;

import java.util.*;

/**
 * This node is responsible for handling the web-fragment xml tree
 *
 * @author  Shing Wai Chan
 * @version 
 */
@Service
public class WebFragmentNode extends WebCommonNode<WebFragmentDescriptor> {

   public final static XMLElement tag = new XMLElement(WebTagNames.WEB_FRAGMENT);

    /** 
     * The system ID of my documents. 
     */
    public final static String SCHEMA_ID = "web-fragment_3_0.xsd";
    private final static List<String> systemIDs = initSystemIDs();

    private static List<String> initSystemIDs() {
        List<String> systemIDs = new ArrayList<String>();
        systemIDs.add(SCHEMA_ID);
        return Collections.unmodifiableList(systemIDs);
    }
    
    /**
     * register this node as a root node capable of loading entire DD files
     *
     * @param publicIDToDTD is a mapping between xml Public-ID to DTD
     * @return the doctype tag name
     */
    @Override
    public String registerBundle(Map publicIDToDTD) {
        return tag.getQName();
    }


    @Override
    public Map<String,Class> registerRuntimeBundle(final Map<String,String> publicIDToDTD, Map<String, List<Class>> versionUpgrades) {
        return Collections.emptyMap();
    }
    
    /** Creates new WebBundleNode */
    public WebFragmentNode()  {
        super();
        registerElementHandler(new XMLElement(WebTagNames.ORDERING),
                OrderingNode.class, "setOrderingDescriptor");
        SaxParserHandler.registerBundleNode(this, WebTagNames.WEB_FRAGMENT);
    }

    /**
     * all sub-implementation of this class can use a dispatch table to map xml element to
     * method name on the descriptor class for setting the element value. 
     *  
     * @return the map with the element name as a key, the setter method as a value
     */
    @Override
    protected Map<String, String> getDispatchTable() {
        Map<String, String> table = super.getDispatchTable();
        table.put(WebTagNames.COMMON_NAME, "setName");
        return table;
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

    /**
     * @return the descriptor instance to associate with this XMLNode
     */
    @Override
    public WebFragmentDescriptor getDescriptor() {
        // no default bundle for web-fragment
        if (descriptor==null) {
            descriptor = new WebFragmentDescriptor();
        }
        return descriptor;
    }

    /**
     * write the descriptor class to a DOM tree and return it
     *
     * @param parent node for the DOM tree
     * @param webFragmentDesc the descriptor to write
     * @return the DOM tree top node
     */
    @Override
    public Node writeDescriptor(Node parent,
        WebFragmentDescriptor webFragmentDesc) {
        Node jarNode = super.writeDescriptor(parent, webFragmentDesc);
        if (webFragmentDesc.getOrderingDescriptor() != null) {
            OrderingNode orderingNode = new OrderingNode();
            orderingNode.writeDescriptor(jarNode, WebTagNames.ORDERING,
                    webFragmentDesc.getOrderingDescriptor());
        }
        return jarNode;
    }


}

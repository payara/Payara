/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.deployment.WebServiceHandlerChain;
import com.sun.enterprise.deployment.node.DisplayableComponentNode;
import com.sun.enterprise.deployment.node.XMLElement;
import com.sun.enterprise.deployment.xml.WebServicesTagNames;
import org.w3c.dom.Node;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class WebServiceHandlerChainNode extends DisplayableComponentNode {

    private final static XMLElement tag =
        new XMLElement(WebServicesTagNames.HANDLER_CHAIN);

    public WebServiceHandlerChainNode() {
        super();
        registerElementHandler
            (new XMLElement(WebServicesTagNames.HANDLER),
             WebServiceHandlerNode.class, "addHandler");
    }

    @Override
    protected WebServiceHandlerChain createDescriptor() {
        return new WebServiceHandlerChain();
    }

    /**
     * @return the XML tag associated with this XMLNode
     */
    protected XMLElement getXMLRootTag() {
        return tag;
    }
    
    /**
     * all sub-implementation of this class can use a dispatch table to map xml element to
     * method name on the descriptor class for setting the element value. 
     *  
     * @return the map with the element name as a key, the setter method as a value
     */
    protected Map getDispatchTable() {
        Map table = super.getDispatchTable();
        table.put(WebServicesTagNames.SERVICE_NAME_PATTERN, "setServiceNamePattern");
        table.put(WebServicesTagNames.PORT_NAME_PATTERN, "setPortNamePattern");
        table.put(WebServicesTagNames.PROTOCOL_BINDINGS, "setProtocolBindings");
        return table;
    }

    /**
     * receives notification of the value for a particular tag
     * 
     * @param element the xml element
     * @param value it's associated value
     */
    public void setElementValue(XMLElement element, String value) {
        super.setElementValue(element, value);
    }
    
    /**
     * write the method descriptor class to a query-method DOM tree and 
     * return it
     *
     * @param parent node in the DOM tree 
     * @param nodeName name for the root element of this xml fragment
     * @param handler the descriptor to write
     * @return the DOM tree top node
     */
    public Node writeDescriptor(Node parent, String nodeName, 
                                WebServiceHandlerChain handler) {        
        Node wshNode = super.writeDescriptor(parent, nodeName, handler);

        if(handler.getServiceNamePattern() != null) {
            appendTextChild(wshNode, 
                        WebServicesTagNames.SERVICE_NAME_PATTERN,
                        handler.getServiceNamePattern());
        }
        if(handler.getPortNamePattern() != null) {
            appendTextChild(wshNode, 
                        WebServicesTagNames.PORT_NAME_PATTERN,
                        handler.getPortNamePattern());
        }
        if(handler.getProtocolBindings() != null) {
            appendTextChild(wshNode, 
                        WebServicesTagNames.PROTOCOL_BINDINGS,
                        handler.getProtocolBindings());
        }
        WebServiceHandlerNode handlerNode = new WebServiceHandlerNode();
        handlerNode.writeWebServiceHandlers(wshNode, handler.getHandlers());
        return wshNode;
    }    

    public void writeWebServiceHandlerChains(Node parent, List handlerChain) {
        // If there are HanderChains, add the <handler-chains> node before adding
        // individual <handler-chain> nodes
        if(handlerChain.size() != 0) {
            parent = super.writeDescriptor(parent, WebServicesTagNames.HANDLER_CHAINS, null);
        }
        for(Iterator iter = handlerChain.iterator(); iter.hasNext();) {
            WebServiceHandlerChain next = (WebServiceHandlerChain) iter.next();
            writeDescriptor(parent, WebServicesTagNames.HANDLER_CHAIN, next);
        }
    }

}

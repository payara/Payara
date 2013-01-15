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

import com.sun.enterprise.deployment.NameValuePairDescriptor;
import com.sun.enterprise.deployment.WebServiceHandler;
import com.sun.enterprise.deployment.node.DisplayableComponentNode;
import com.sun.enterprise.deployment.node.XMLElement;
import com.sun.enterprise.deployment.xml.TagNames;
import com.sun.enterprise.deployment.xml.WebServicesTagNames;
import org.w3c.dom.Node;

import javax.xml.namespace.QName;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.webservices.connector.LogUtils;

/**
 * This node does xml marshalling to/from web service handlers.
 *
 * @author Kenneth Saks
 */
public class WebServiceHandlerNode extends DisplayableComponentNode {

    private static final Logger logger = LogUtils.getLogger();

    private final static XMLElement tag =
        new XMLElement(WebServicesTagNames.HANDLER);

    private NameValuePairDescriptor initParam = null;

    public WebServiceHandlerNode() {
        super();
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
        table.put(WebServicesTagNames.SOAP_ROLE, "addSoapRole");
        table.put(WebServicesTagNames.HANDLER_NAME, "setHandlerName");
        table.put(WebServicesTagNames.HANDLER_CLASS, "setHandlerClass");
        table.put(WebServicesTagNames.HANDLER_PORT_NAME, "addPortName");
        return table;
    }

    protected WebServiceHandler createDescriptor() {
       return new WebServiceHandler();
   }

    /**
     * receives notification of the value for a particular tag
     * 
     * @param element the xml element
     * @param value it's associated value
     */
    public void setElementValue(XMLElement element, String value) {
        String qname = element.getQName();
        WebServiceHandler handler = (WebServiceHandler) getDescriptor();
        if (WebServicesTagNames.INIT_PARAM_NAME.equals(qname)) {
            initParam = new NameValuePairDescriptor();
            initParam.setName(value);
        } else if (WebServicesTagNames.INIT_PARAM_VALUE.equals(qname)) {
            initParam.setValue(value);
            handler.addInitParam(initParam);
        } else if (TagNames.DESCRIPTION.equals(qname)) {
            if( initParam != null ) {
                // description for the init-param
                initParam.setDescription(value);
                initParam = null;
            } else {
                // must be the description element of the handler itself.
                super.setElementValue(element, value);
            }
        } else if (WebServicesTagNames.SOAP_HEADER.equals(qname) ) {
            String prefix = getPrefixFromQName(value);
            String localPart = getLocalPartFromQName(value);
            String namespaceUri = resolvePrefix(element, prefix);

            if( namespaceUri == null) {
                logger.log(Level.SEVERE, LogUtils.INVALID_DESC_MAPPING_FAILURE,
                    new Object[] {prefix , handler.getHandlerName()});
            } else {
                QName soapHeader = new QName(namespaceUri, localPart);
                handler.addSoapHeader(soapHeader);
            }

        } else super.setElementValue(element, value);
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
                                WebServiceHandler handler) {        
        Node wshNode = super.writeDescriptor(parent, nodeName, handler);

        writeDisplayableComponentInfo(wshNode, handler);
        appendTextChild(wshNode, 
                        WebServicesTagNames.HANDLER_NAME,
                        handler.getHandlerName());

        appendTextChild(wshNode, 
                        WebServicesTagNames.HANDLER_CLASS,
                        handler.getHandlerClass());
        
        for(Iterator iter = handler.getInitParams().iterator();iter.hasNext();){
            NameValuePairDescriptor next = (NameValuePairDescriptor)iter.next();
            Node initParamNode = 
                appendChild(wshNode, WebServicesTagNames.INIT_PARAM);
            appendTextChild(initParamNode, WebServicesTagNames.INIT_PARAM_NAME,
                            next.getName());
            appendTextChild(initParamNode, WebServicesTagNames.INIT_PARAM_VALUE,
                            next.getValue());
        }
        
        for(Iterator iter = handler.getSoapHeaders().iterator(); 
            iter.hasNext();) {
            QName next = (QName) iter.next();
            // Append soap header QName.  NOTE : descriptor does not contain
            // a prefix so always generate one.
            appendQNameChild(WebServicesTagNames.SOAP_HEADER, wshNode,
                             next.getNamespaceURI(), next.getLocalPart(), null);
        }

        for(Iterator iter = handler.getSoapRoles().iterator(); iter.hasNext();){
            String next = (String) iter.next();
            appendTextChild(wshNode, WebServicesTagNames.SOAP_ROLE, next);
        }

        for(Iterator iter = handler.getPortNames().iterator(); iter.hasNext();){
            String next = (String) iter.next();
            appendTextChild(wshNode, WebServicesTagNames.HANDLER_PORT_NAME, 
                            next);
        }

        return wshNode;
    }    

    public void writeWebServiceHandlers(Node parent, List handlerChain) {
        for(Iterator iter = handlerChain.iterator(); iter.hasNext();) {
            WebServiceHandler next = (WebServiceHandler) iter.next();
            writeDescriptor(parent, WebServicesTagNames.HANDLER, next);
        }
    }

}

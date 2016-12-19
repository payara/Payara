/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2016 Oracle and/or its affiliates. All rights reserved.
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

/*
 * FilterMappingNode.java
 *
 * Created on February 26, 2002, 9:21 PM
 */

package org.glassfish.web.deployment.node;

import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.deployment.node.DeploymentDescriptorNode;
import com.sun.enterprise.deployment.node.XMLElement;
import com.sun.enterprise.deployment.util.DOLUtils;
import com.sun.enterprise.util.net.URLPattern;
import org.glassfish.web.LogFacade;
import org.glassfish.web.deployment.descriptor.ServletFilterMappingDescriptor;
import org.glassfish.web.deployment.xml.WebTagNames;
import org.w3c.dom.Node;

import java.text.MessageFormat;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;

import javax.servlet.DispatcherType;


/**
 * This node handles all information relative to servlet-mapping xml tag
 *
 * @author  Jerome Dochez
 * @version 
 */
public class FilterMappingNode extends DeploymentDescriptorNode<ServletFilterMappingDescriptor> {

    private static final ResourceBundle rb = LogFacade.getLogger().getResourceBundle();

    private ServletFilterMappingDescriptor descriptor;

    /**
     * @return the descriptor instance to associate with this XMLNode
     */
    @Override
    public ServletFilterMappingDescriptor getDescriptor() {
        
       if (descriptor==null) {
            descriptor = new ServletFilterMappingDescriptor();
        }
        return descriptor;
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
        table.put(WebTagNames.FILTER_NAME, "setName");
        return table;
    }    
    
    /**
     * receives notiification of the value for a particular tag
     * 
     * @param element the xml element
     * @param value it's associated value
     */
    @Override
    public void setElementValue(XMLElement element, String value) {
        if (WebTagNames.SERVLET_NAME.equals(element.getQName())) {
            descriptor.addServletName(value);
        } else if (WebTagNames.URL_PATTERN.equals(element.getQName())) {
            if (!URLPattern.isValid(value)) {
                // try trimming url (in case DD uses extra
                // whitespace for aligning)
                String trimmedUrl = value.trim();

                // If URL Pattern does not start with "/" then
                // prepend it (for Servlet2.2 Web apps)
                Object parent = getParentNode().getDescriptor();
                if (parent instanceof WebBundleDescriptor &&
                        ((WebBundleDescriptor) parent).getSpecVersion().equals("2.2")) {
                    if(!trimmedUrl.startsWith("/") &&
                            !trimmedUrl.startsWith("*.")) {
                        trimmedUrl = "/" + trimmedUrl;
                    }
                }

                if (URLPattern.isValid(trimmedUrl)) {
                    // warn user if url included \r or \n
                    if (URLPattern.containsCRorLF(value)) {
                        DOLUtils.getDefaultLogger().log(Level.WARNING,
                                "enterprise.deployment.backend.urlcontainscrlf",
                                new Object[] { value });
                    }
                    value = trimmedUrl;
                } else {
                    throw new IllegalArgumentException(
                            rb.getString(
                                    MessageFormat.format(
                                            LogFacade.ENTERPRISE_DEPLOYMENT_INVALID_URL_PATTERN, value)));
                }
            }
            descriptor.addURLPattern(value);
        } else if (WebTagNames.DISPATCHER.equals(element.getQName())) {
            descriptor.addDispatcher(value);
        } else super.setElementValue(element, value);
    }   
    
    /**
     * write the descriptor class to a DOM tree and return it
     *
     * @param parent node in the DOM tree 
     * @param nodeName node name for the root element of this xml fragment
     * @param descriptor the descriptor to write
     * @return the DOM tree top node
     */
    @Override
    public Node writeDescriptor(Node parent, String nodeName, ServletFilterMappingDescriptor descriptor) {
        Node myNode = appendChild(parent, nodeName);
        appendTextChild(myNode, WebTagNames.FILTER_NAME, descriptor.getName());                        
        for (String servletName : descriptor.getServletNames()) {
            appendTextChild(myNode, WebTagNames.SERVLET_NAME, servletName);
        }

        for (String urlPattern : descriptor.getUrlPatterns()) {
            appendTextChild(myNode, WebTagNames.URL_PATTERN, urlPattern);
        }

        for (DispatcherType dispatcherType : descriptor.getDispatchers()) {
            appendTextChild(myNode, WebTagNames.DISPATCHER, dispatcherType.name());
        }
        return myNode;
    }
}

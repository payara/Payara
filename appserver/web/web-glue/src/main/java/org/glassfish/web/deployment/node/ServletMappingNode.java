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

package org.glassfish.web.deployment.node;

import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.deployment.node.DeploymentDescriptorNode;
import com.sun.enterprise.deployment.node.XMLElement;
import com.sun.enterprise.deployment.node.XMLNode;
import com.sun.enterprise.deployment.util.DOLUtils;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.net.URLPattern;
import org.glassfish.web.LogFacade;
import org.glassfish.web.deployment.xml.WebTagNames;

import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.logging.Level;

/**
 * This node is responsible for handling servlet-mapping subtree node
 *
 * @author  Jerome Dochez
 * @version 
 */
public class ServletMappingNode extends DeploymentDescriptorNode {

    private static final ResourceBundle rb = LogFacade.getLogger().getResourceBundle();

    private String servletName;
    private String urlPattern;

    /**
     * @return the descriptor instance to associate with this XMLNode
     */
    @Override
    public Object getDescriptor() {
        return null;
    }

    /**
     * receives notiification of the value for a particular tag
     * 
     * @param element the xml element
     * @param value it's associated value
     */    
    public void setElementValue(XMLElement element, String value) {
        if (WebTagNames.SERVLET_NAME.equals(element.getQName())) {
            servletName = value;
        } 
        if (WebTagNames.URL_PATTERN.equals(element.getQName())) {
            if (!URLPattern.isValid(value)) {
                // try trimming url (in case DD uses extra
                // whitespace for aligning)
                String trimmedUrl = value.trim();
                if ("\"\"".equals(trimmedUrl)) {
                    trimmedUrl = "";
                }

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

            urlPattern = value;

            XMLNode  parentNode = getParentNode();
            if (parentNode instanceof WebCommonNode) {
                ((WebCommonNode) parentNode).addServletMapping(servletName, 
                urlPattern);
            } else {
                DOLUtils.getDefaultLogger().log(Level.SEVERE, "enterprise.deployment.backend.addDescriptorFailure",
                    new Object[]{getXMLRootTag() , "servlet-mapping"});
            }

        } 
    }

}

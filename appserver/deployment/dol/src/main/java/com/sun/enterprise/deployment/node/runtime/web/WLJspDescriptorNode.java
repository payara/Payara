/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.deployment.node.runtime.web;

import java.util.ArrayList;
import java.util.Enumeration;

import org.w3c.dom.Node;

import com.sun.enterprise.deployment.EnvironmentProperty;
import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.deployment.WebComponentDescriptor;
import com.sun.enterprise.deployment.node.XMLElement;
import com.sun.enterprise.deployment.xml.RuntimeTagNames;
import com.sun.enterprise.deployment.web.InitializationParameter;


/**
 * This node is responsible for handling weblogic.xml jsp-descriptor.
 *
 * @author Kin-man Chung
 */

public class WLJspDescriptorNode extends WebRuntimeNode {

    /**
     * @return the descriptor instance to associate with this XMLNode
     */
    public Object getDescriptor() {
        return null;
    }

    /**
     * receives notification of the value for a particular tag
     *
     * @param element the xml element
     * @param value it's associated value
     */
    public void setElementValue(XMLElement element, String value) {

        String name = element.getQName();
        if (name.equals(RuntimeTagNames.KEEPGENERATED)) {
            setJspInitParameter("keepgenerated", value);
        } else if (name.equals(RuntimeTagNames.WORKING_DIR)) {
            setJspInitParameter("scratchdir", value);
        } else if (name.equals(RuntimeTagNames.COMPRESS_HTML_TEMPLATE)) {
            setJspInitParameter("trimSpaces", value);
        }
    }

    private WebComponentDescriptor getJspDescriptor(WebBundleDescriptor wbd) {

        for (WebComponentDescriptor wcd: wbd.getWebComponentDescriptors()) {
            if (!wcd.isEnabled()) {
                continue;
            }
            String servletName = wcd.getWebComponentImplementation();
            if ("org.apache.jasper.servlet.JspServlet".equals(servletName)) {
                return wcd;
            }
        }
        return null;
    }
            
    private static final String JSP_DESC = "glassfish.weblogic.jsp";
    private void setJspInitParameter(String property, String value) {

        WebComponentDescriptor jspDescriptor = getJspDescriptor(
                    (WebBundleDescriptor)getParentNode().getDescriptor());
        if (jspDescriptor != null) {
            // The description in the envior property is used as special
            // marker to indicate this jsp init is from weblogic.xml
            jspDescriptor.addInitializationParameter(
                new EnvironmentProperty(property, value, JSP_DESC));
        }
    }

    /**
     * write the descriptor class to a DOM tree and return it
     *
     * @param parent node for the DOM tree
     * @param the descriptor to write
     * @return the DOM tree top node
     */
    public Node writeDescriptor(Node parent, WebBundleDescriptor wbd) {

        ArrayList<InitializationParameter> jspInits =
                new ArrayList<InitializationParameter>();

        WebComponentDescriptor jspDescriptor = getJspDescriptor(wbd);
        if (jspDescriptor == null) {
            return null;
        }
        Enumeration e = jspDescriptor.getInitializationParameters();
        while (e.hasMoreElements()) {
            InitializationParameter initp =
                    (InitializationParameter)e.nextElement();
            if (JSP_DESC.equals(initp.getDescription())) {
                jspInits.add(initp);
            }
        }

        Node jspNode = null;
        if (jspInits.size() > 0) {
            // Reconstruct the weblogic tags
            jspNode = appendChild(parent, RuntimeTagNames.JSP_DESCRIPTOR);
            for (InitializationParameter ip: jspInits) {
                String tagName = null;
                if ("keepgenerated".equals(ip.getName())) {
                    tagName = RuntimeTagNames.KEEPGENERATED;
                } else if ("scratchdir".equals(ip.getName())) {
                    tagName = RuntimeTagNames.WORKING_DIR;
                } else if ("trimSpaces".equals(ip.getName())) {
                    tagName = RuntimeTagNames.COMPRESS_HTML_TEMPLATE;
                }
                if (tagName != null) {
                    appendTextChild(jspNode, tagName, ip.getValue());
                }
            }
        }
        return jspNode;
    }
}

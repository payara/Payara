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

package com.sun.enterprise.deployment.node.runtime.web;

import com.sun.enterprise.deployment.EnvironmentProperty;
import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.deployment.WebComponentDescriptor;
import com.sun.enterprise.deployment.web.InitializationParameter;
import com.sun.enterprise.deployment.node.XMLElement;
import com.sun.enterprise.deployment.node.runtime.RuntimeDescriptorNode;
import com.sun.enterprise.deployment.runtime.web.ClassLoader;
import com.sun.enterprise.deployment.xml.RuntimeTagNames;

import org.glassfish.deployment.common.DeploymentProperties;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * This node is responsible for handling weblogic.xml container-descriptor.
 *
 * @author  Shing Wai Chan
 */
public class WLContainerDescriptorNode extends RuntimeDescriptorNode {
    /**
     * receives notification of the value for a particular tag
     * 
     * @param element the xml element
     * @param value it's associated value
     */
    public void setElementValue(XMLElement element, String value) {
        String name = element.getQName();
        if (name.equals(RuntimeTagNames.INDEX_DIRECTORY_ENALBED)) {
            setDefaultServletInitParam("listings", value);
        } else if (name.equals(RuntimeTagNames.INDEX_DIRECTORY_SORT_BY)) {
            setDefaultServletInitParam("sortedBy", value);
        } else if (name.equals(RuntimeTagNames.SAVE_SESSIONS_ENABLED)) {
            WebBundleDescriptor descriptor = (WebBundleDescriptor)getParentNode().getDescriptor();
            descriptor.setKeepState(value);
        } else if (name.equals(RuntimeTagNames.PREFER_WEB_INF_CLASSES)) {
            WebBundleDescriptor descriptor = (WebBundleDescriptor)getParentNode().getDescriptor();
            ClassLoader clBean = descriptor.getSunDescriptor().getClassLoader();
            if (clBean == null) {
                clBean = new ClassLoader();
                descriptor.getSunDescriptor().setClassLoader(clBean);
            }
            clBean.setAttributeValue(ClassLoader.DELEGATE,
                    Boolean.toString(!Boolean.parseBoolean(value)));
        } else {
            super.setElementValue(element, value);
        }
    }

    /**
     * @return the descriptor instance to associate with this XMLNode
     */    
    public Object getDescriptor() {
        return null;
    }

    public Node writeDescriptor(Element root, WebBundleDescriptor webBundleDescriptor) {
        Node containerDescriptorNode = null;
        WebComponentDescriptor defaultServletDesc =
                webBundleDescriptor.getWebComponentByCanonicalName("default");
        InitializationParameter listingsParam = getDefaultServletInitParam(
                defaultServletDesc, "listings", false);
        InitializationParameter sortedByParam = getDefaultServletInitParam(
                defaultServletDesc, "sortedBy", false);
        ClassLoader clBean = webBundleDescriptor.getSunDescriptor().getClassLoader();

        containerDescriptorNode = appendChild(root, RuntimeTagNames.CONTAINER_DESCRIPTOR);

        if (listingsParam != null) {
            appendTextChild(containerDescriptorNode,
                    RuntimeTagNames.INDEX_DIRECTORY_ENALBED, listingsParam.getValue());
        }
        
        if (sortedByParam != null) {
            appendTextChild(containerDescriptorNode,
                    RuntimeTagNames.INDEX_DIRECTORY_SORT_BY, sortedByParam.getValue());
        }

        appendTextChild(containerDescriptorNode, RuntimeTagNames.SAVE_SESSIONS_ENABLED,
                Boolean.toString(webBundleDescriptor.getKeepState()));

        if (clBean != null) {
            appendTextChild(containerDescriptorNode,
                    RuntimeTagNames.PREFER_WEB_INF_CLASSES,
                    clBean.getAttributeValue(ClassLoader.DELEGATE));
        }

        return containerDescriptorNode;
    }


    private void setDefaultServletInitParam(String name, String value) {
        WebBundleDescriptor descriptor = (WebBundleDescriptor)getParentNode().getDescriptor();
        WebComponentDescriptor defaultServletDesc =
                descriptor.getWebComponentByCanonicalName("default");
        InitializationParameter initParam =
                getDefaultServletInitParam(defaultServletDesc, name, true);
        initParam.setValue(value);
    }

    private InitializationParameter getDefaultServletInitParam(
            WebComponentDescriptor defaultServletDesc, String name, boolean create) {
        if (defaultServletDesc == null) {
            throw new RuntimeException("Default servlet is missing in web descriptors.");
        }
        InitializationParameter initParam = defaultServletDesc.getInitializationParameterByName(name);
        if (initParam == null && create) {
            initParam = new EnvironmentProperty();
            defaultServletDesc.addInitializationParameter(initParam);
            initParam.setName(name);
        }
        return initParam;
    }
}

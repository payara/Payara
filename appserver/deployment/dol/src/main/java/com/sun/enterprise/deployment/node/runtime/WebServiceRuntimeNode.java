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

package com.sun.enterprise.deployment.node.runtime;

import com.sun.enterprise.deployment.BundleDescriptor;
import com.sun.enterprise.deployment.WebService;
import com.sun.enterprise.deployment.WebServicesDescriptor;
import com.sun.enterprise.deployment.node.DeploymentDescriptorNode;
import com.sun.enterprise.deployment.node.XMLElement;
import com.sun.enterprise.deployment.util.DOLUtils;
import com.sun.enterprise.deployment.xml.WebServicesTagNames;
import org.w3c.dom.Node;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.logging.Level;

/**
 * This node is responsible for handling WebService runtime info 
 *
 * @author  Kenneth Saks
 * @version 
 */
public class WebServiceRuntimeNode extends DeploymentDescriptorNode {

    private WebService descriptor;

    public Object getDescriptor() {
        return descriptor;
    }

    /**
     * receives notiification of the value for a particular tag
     * 
     * @param element the xml element
     * @param value it's associated value
     */

    public void setElementValue(XMLElement element, String value) {
        if (WebServicesTagNames.WEB_SERVICE_DESCRIPTION_NAME.equals
            (element.getQName())) {
            BundleDescriptor parent = (BundleDescriptor)getParentNode().getDescriptor();
            WebServicesDescriptor webServices = parent.getWebServices();
            descriptor = webServices.getWebServiceByName(value);
        } else if( WebServicesTagNames.CLIENT_WSDL_PUBLISH_URL.equals
                   (element.getQName()) ) {
            if( descriptor == null ) {
                DOLUtils.getDefaultLogger().info
                    ("Warning : WebService descriptor is null for "
                     + "final wsdl url=" + value);
                return;
            }
            try {
                URL url = new URL(value);
                descriptor.setClientPublishUrl(url);
            } catch(MalformedURLException mue) {
                DOLUtils.getDefaultLogger().log(Level.INFO,
                  "Warning : Invalid final wsdl url=" + value, mue);
            }
        } else {
            super.setElementValue(element, value);
        }
    }
    
    /**
     * write the descriptor class to a DOM tree and return it
     *
     * @param parent node for the DOM tree
     * @param node name for the descriptor
     * @param the descriptor to write
     * @return the DOM tree top node
     */    
    public Node writeDescriptor(Node parent, String nodeName, 
                                WebService webService) {
        Node webServiceNode = 
            super.writeDescriptor(parent, nodeName, webService);

        appendTextChild(webServiceNode, 
                        WebServicesTagNames.WEB_SERVICE_DESCRIPTION_NAME,
                        webService.getName());

        if( webService.hasClientPublishUrl() ) {
            URL url = webService.getClientPublishUrl();
            appendTextChild(webServiceNode, 
                            WebServicesTagNames.CLIENT_WSDL_PUBLISH_URL,
                            url.toExternalForm());
        }

        return webServiceNode;
    }  
    
    /**
     * writes all the runtime information for the web services for a given
     * bundle descriptor
     * 
     * @param parent node to add the runtime xml info
     * @param the bundle descriptor
     */        
    public void writeWebServiceRuntimeInfo(Node parent,
                                           BundleDescriptor bundle) {
        WebServicesDescriptor webServices = bundle.getWebServices();
        if( webServices != null ) {
            for(Iterator iter = webServices.getWebServices().iterator(); 
                iter.hasNext();) {
                WebService next = (WebService) iter.next();
                if( next.hasClientPublishUrl() ) {
                    writeDescriptor
                        (parent, WebServicesTagNames.WEB_SERVICE, next);
                }
            }
        }
    }
    
}

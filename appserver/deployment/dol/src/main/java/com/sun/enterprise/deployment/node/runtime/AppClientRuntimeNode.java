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

import com.sun.enterprise.deployment.ApplicationClientDescriptor;
import com.sun.enterprise.deployment.node.XMLElement;
import com.sun.enterprise.deployment.node.appclient.AppClientNode;
import com.sun.enterprise.deployment.xml.DTDRegistry;
import com.sun.enterprise.deployment.xml.RuntimeTagNames;
import com.sun.enterprise.deployment.xml.WebServicesTagNames;
import org.w3c.dom.Node;

import java.util.List;
import java.util.Map;

/**
 * This node is responsible for saving all J2EE RI runtime
 * information for app clients
 *
 * @author  Jerome Dochez
 * @version 
 */
public class AppClientRuntimeNode extends 
        RuntimeBundleNode<ApplicationClientDescriptor> {   
    
    public AppClientRuntimeNode(ApplicationClientDescriptor descriptor) {
        super(descriptor);
        //trigger registration in standard node, if it hasn't happened
        habitat.getService(AppClientNode.class);
    }
    
    public AppClientRuntimeNode() {
        this(null);
    }      
    
    /**
     * Initialize the child handlers
     */
    protected void init() {
        
        // we do not care about our standard DDS handles
        handlers = null;        
        
        registerElementHandler(new XMLElement(RuntimeTagNames.RESOURCE_REFERENCE), 
                               ResourceRefNode.class);     
        registerElementHandler(new XMLElement(RuntimeTagNames.EJB_REFERENCE), 
                               EjbRefNode.class);
        registerElementHandler(new XMLElement(RuntimeTagNames.RESOURCE_ENV_REFERENCE), 
                               ResourceEnvRefNode.class);                     
        registerElementHandler(new XMLElement(RuntimeTagNames.MESSAGE_DESTINATION_REFERENCE),
                               MessageDestinationRefNode.class);
        registerElementHandler(new XMLElement(RuntimeTagNames.MESSAGE_DESTINATION), 
             MessageDestinationRuntimeNode.class);
	registerElementHandler(new XMLElement(WebServicesTagNames.SERVICE_REF),
                               ServiceRefNode.class);
        registerElementHandler(new XMLElement(RuntimeTagNames.JAVA_WEB_START_ACCESS),
                               JavaWebStartAccessNode.class);


    }
    
   /**
    * register this node as a root node capable of loading entire DD files
    * 
    * @param publicIDToDTD is a mapping between xml Public-ID to DTD 
    * @return the doctype tag name
    */
   public static String registerBundle(Map publicIDToDTD) {    
       publicIDToDTD.put(DTDRegistry.SUN_APPCLIENT_130_DTD_PUBLIC_ID, DTDRegistry.SUN_APPCLIENT_130_DTD_SYSTEM_ID);
       publicIDToDTD.put(DTDRegistry.SUN_APPCLIENT_140_DTD_PUBLIC_ID, DTDRegistry.SUN_APPCLIENT_140_DTD_SYSTEM_ID);       
       publicIDToDTD.put(DTDRegistry.SUN_APPCLIENT_141_DTD_PUBLIC_ID, DTDRegistry.SUN_APPCLIENT_141_DTD_SYSTEM_ID);       
       publicIDToDTD.put(DTDRegistry.SUN_APPCLIENT_500_DTD_PUBLIC_ID, DTDRegistry.SUN_APPCLIENT_500_DTD_SYSTEM_ID);
       publicIDToDTD.put(DTDRegistry.SUN_APPCLIENT_600_DTD_PUBLIC_ID, DTDRegistry.SUN_APPCLIENT_600_DTD_SYSTEM_ID);
       if (!restrictDTDDeclarations()) {           
           publicIDToDTD.put(DTDRegistry.SUN_APPCLIENT_140beta_DTD_PUBLIC_ID, DTDRegistry.SUN_APPCLIENT_140beta_DTD_SYSTEM_ID);       
       }
       return RuntimeTagNames.S1AS_APPCLIENT_RUNTIME_TAG;
   }    
    
    /**
     * @return the XML tag associated with this XMLNode
     */
    protected XMLElement getXMLRootTag() {
        return new XMLElement(RuntimeTagNames.S1AS_APPCLIENT_RUNTIME_TAG);
    } 
    
    /** 
     * @return the DOCTYPE that should be written to the XML file
     */
    public String getDocType() {
	return DTDRegistry.SUN_APPCLIENT_600_DTD_PUBLIC_ID;
    }
    
    /**
     * @return the SystemID of the XML file
     */
    public String getSystemID() {
	return DTDRegistry.SUN_APPCLIENT_600_DTD_SYSTEM_ID;
    }

    /**
     * @return NULL for all runtime nodes.
     */
    public List<String> getSystemIDs() {
        return null;
    }
    
    /**
     * write the descriptor class to a DOM tree and return it
     *
     * @param parent node for the DOM tree
     * @param bundleDescriptor the descriptor to write
     * @return the DOM tree top node
     */    
    public Node writeDescriptor(Node parent, ApplicationClientDescriptor bundleDescriptor) {       
        Node appClient = super.writeDescriptor(parent, bundleDescriptor);
        RuntimeDescriptorNode.writeCommonComponentInfo(appClient, bundleDescriptor);
        RuntimeDescriptorNode.writeMessageDestinationInfo(appClient, bundleDescriptor);
        JavaWebStartAccessNode.writeJavaWebStartInfo(appClient, bundleDescriptor.getJavaWebStartAccessDescriptor());
        return appClient;
    }

   /**
     * receives notification of the value for a particular tag
     *
     * @param element the xml element
     * @param value it's associated value
     */
    public void setElementValue(XMLElement element, String value) {
        if (element.getQName().equals(RuntimeTagNames.VERSION_IDENTIFIER)) {
        } else {
            super.setElementValue(element, value);
        }
    }
}

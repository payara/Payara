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
import com.sun.enterprise.deployment.MessageDestinationDescriptor;
import org.glassfish.deployment.common.RootDeploymentDescriptor;
import com.sun.enterprise.deployment.node.DeploymentDescriptorNode;
import com.sun.enterprise.deployment.node.RootXMLNode;
import com.sun.enterprise.deployment.node.XMLElement;
import com.sun.enterprise.deployment.util.DOLUtils;
import com.sun.enterprise.deployment.xml.RuntimeTagNames;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

import java.util.Iterator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class RuntimeBundleNode<T extends RootDeploymentDescriptor>
        extends DeploymentDescriptorNode<T> implements RootXMLNode<T> {

    private static Boolean restrictDTDDeclarations=null;

    protected T descriptor=null;

    // we record the XML element to node class mapping when parsing and 
    // retrieve it when writing out
    // The first level map is indexed by the parent element name, and the 
    // second level of the map is indexed by the sub element name and the
    // corresponding handler node class name
    protected HashMap<String, LinkedHashMap<String, Class>> elementToNodeMappings = new HashMap<String, LinkedHashMap<String, Class>>();
    
    public RuntimeBundleNode(T descriptor) {
        this.descriptor = descriptor;
	init();
    }   

    public RuntimeBundleNode() {
        this(null);
    } 
    
    /**
     * Initializes the child handler;
     */
    protected void init() {
	// we do not care about standard DDs common tags
	handlers=null;
    }
    
    /**
     * Adds  a new DOL descriptor instance to the descriptor instance associated with 
     * this XMLNode
     *
     * @param descriptor the new descriptor
     */
    public void addDescriptor(Object descriptor) {    
        return;
    }
    
   /**
    * @return the descriptor instance to associate with this XMLNode
    */    
    public T getDescriptor() {
        return descriptor;
    } 
    
    /**
     * @return the default spec version level this node complies to
     */
    public String getSpecVersion() {
        return "1.5";
    }
    
    /**
     * set the DOCTYPE as read in the input XML File
     * @param DOCTYPE
     */
    public void setDocType(String docType) {
        // I do not care about the version of the runtime descriptors
    }
    
    /**
     * Sets the specVersion for this descriptor depending on the docType
     */
    protected void setSpecVersion() {
        // I do not care about the version of the runtime descriptors
    }  

    /**
     * writes the message destination references runtime information
     */
    protected void writeMessageDestinationInfo(Node parent, 
                                               BundleDescriptor descriptor) {
        for(Iterator iter = descriptor.getMessageDestinations().iterator();
            iter.hasNext();) {
            MessageDestinationRuntimeNode node = 
                new MessageDestinationRuntimeNode();
            node.writeDescriptor(parent, RuntimeTagNames.MESSAGE_DESTINATION,
                                 (MessageDestinationDescriptor) iter.next());
        }
    }    
    
    /** 
     * @return true if the runtime bundle node should only process
     * the product FCS DTD declarations
     */
    protected static final synchronized boolean restrictDTDDeclarations() {
        if (restrictDTDDeclarations==null) {
            restrictDTDDeclarations = Boolean.valueOf(Boolean.getBoolean("com.sun.aas.deployment.restrictdtddeclarations"));
        }
        return restrictDTDDeclarations.booleanValue();
    }
    
    public static Element appendChildNS(Node parent, String elementName,
        String nameSpace) {
        Element child = getOwnerDocument(parent).createElementNS(nameSpace, elementName);
        parent.appendChild(child);
        return child;
    }

    /**
     * record mapping of sub element to node class for the current element
     */
    public void recordNodeMapping(String currentElementName, String subElementName, Class subElementHandler) {
        LinkedHashMap<String, Class> subElementMappings = elementToNodeMappings.get(currentElementName);
        if (subElementMappings == null) {
            subElementMappings = new LinkedHashMap<String, Class>();
            elementToNodeMappings.put(currentElementName, subElementMappings);
        }
        subElementMappings.put(subElementName, subElementHandler);
    }

    public LinkedHashMap<String, Class> getNodeMappings(String currentElementName) {
        return elementToNodeMappings.get(currentElementName);
    }

    /**
     * receives notiification of the value for a particular tag
     * 
     * @param element the xml element
     * @param value it's associated value
     */
    public void setElementValue(XMLElement element, String value) {
      if (! DOLUtils.setElementValue(element, value, getDescriptor())) {
        super.setElementValue(element, value);
      }
    }
    
    /**
     * all sub-implementation of this class can use a dispatch table to map xml element to
     * method name on the descriptor class for setting the element value. 
     *  
     * @return the map with the element name as a key, the setter method as a value
     */
    protected Map<String, String> getDispatchTable() {
      Map<String,String> dispatchTable = super.getDispatchTable();
      dispatchTable.put("version", "setSpecVersion");
      return dispatchTable;
    }

}

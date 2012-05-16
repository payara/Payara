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

package com.sun.enterprise.deployment.node;

import org.jvnet.hk2.annotations.Contract;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;

/**
 * This interface defines the protocol associated with all the nodes. An 
 * XML node is responsible for reading the XML file  into a object
 * representation 
 *
 * @author  Jerome Dochez
 * @version 
 */
@Contract
public interface XMLNode<T> {

    /**
     * notification of the start of an XML element tag in the processed
     * XML source file. 
     * 
     * @param element the XML element type name
     * @param attributes the specified or defaultted attritutes
     */
    public void startElement(XMLElement element, Attributes attributes);
     
    /**
     * sets the value of an XML element
     * 
     * @param element the XML element type name
     * @param value the element value
     */
    public void setElementValue(XMLElement element, String value);
    
    /** 
     * notification of the end of an XML element in the source XML 
     * file. 
     * 
     * @param element the XML element type name
     * @return true if this node is done with the processing of elements 
     * in the processing
     */
    public boolean endElement(XMLElement element); 
    
    /**
     * Return true if the XMLNode is responisble for handling the 
     * XML element
     * 
     * @param element the XML element type name
     * @return true if the node processes this element name
     */
    public boolean handlesElement(XMLElement element);
    
    /**
     * Return the XMLNode implementation respionsible for
     * handling the sub-element of the current node
     * 
     * @param element the XML element type name
     * @return XMLNode implementation responsible for handling
     * the XML tag
     */
    public XMLNode getHandlerFor(XMLElement element);    
    
    /** 
     * @return the parent node for this XMLNode
     */
    public XMLNode getParentNode();     

    /**
     * @return the root node for this XMLNode
     */
    public XMLNode getRootNode();

    /**
     * @return the XMLPath for the element name this node 
     * is handling. The XML path can be a absolute or a 
     * relative XMLPath.
     */
    public String getXMLPath();
    
    /** 
     * @return the Descriptor subclass that was populated  by reading
     * the source XML file
     */
    public T getDescriptor();
    
    /**
     * Add a new descriptor to the current descriptor associated with 
     * this node. This method is usually called by sub XMLNodes 
     * (Returned by getHandlerFor) to add the result of their parsing 
     * to the main descriptor. 
     *
     * @param descriptor the new descriptor to be added to the current
     * descriptor.
     */
    public void addDescriptor(Object descriptor);
    
    /**
     * write the descriptor to an JAXP DOM node and return it
     * 
     * @param parent node in the DOM tree
     * @param descriptor the descriptor to be written
     * @return the JAXP DOM node for this descriptor
     */
    public Node writeDescriptor(Node parent, T descriptor);

    /**
     * notify of a new prefix mapping used from this node
     */
    public void addPrefixMapping(String prefix, String uri);
    
    /**
     * Resolve a QName prefix to its corresponding Namespace URI by
     * searching up node chain starting with the child.
     */
    public String resolvePrefix(XMLElement element, String prefix);
}


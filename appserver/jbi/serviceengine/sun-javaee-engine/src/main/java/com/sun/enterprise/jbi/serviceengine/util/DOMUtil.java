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

package com.sun.enterprise.jbi.serviceengine.util;

import java.util.HashMap;
import javax.xml.stream.XMLStreamWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import java.io.Writer;

import java.util.ArrayList;
import java.util.Map;
import javax.xml.stream.XMLStreamReader;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Attr;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.ProcessingInstruction;


/**
 * This object provides utility methods to manipulate DOM tree.
 *
 * @author bhavanishankar@dev.java.net.
 */
public class DOMUtil {
    /**
     * static object to access the methods of this object.
     */
    public static final DOMUtil UTIL = new DOMUtil();
    
    /**
     * Creates a new instance of DOMUtil.
     */
    public DOMUtil() {
    }
    
    /**
     * Writes namespace attributes and other attributes from the current node
     * of the reader to the current node of the writer.
     *
     * @param reader XMLStreamReader pointing to a START_ELEMENT.
     * @param writer XMLStreamWriter pointing to a START_ELEMENT.
     */
    void writeAttributes(XMLStreamReader reader, XMLStreamWriter writer)
    throws Exception {
        int namespaceCount = reader.getNamespaceCount();
        int attributeCount = reader.getAttributeCount();
        for(int i=0; i<namespaceCount; i++) {
            writer.writeNamespace(reader.getNamespacePrefix(i),
                    reader.getNamespaceURI(i));
        }
        for(int i=0; i<attributeCount; i++) {
            String attrNs = reader.getAttributeNamespace(i);
            if(attrNs != null) {
                writer.writeAttribute(attrNs,
                        reader.getAttributeLocalName(i),
                        reader.getAttributeValue(i));
                
            } else {
                writer.writeAttribute(reader.getAttributeLocalName(i),
                        reader.getAttributeValue(i));
            }
        }
    }
    
    private void writeAncestorNamespaceAttrs(
            XMLStreamReader reader,
            XMLStreamWriter writer,
            Map<String,String> ancestorNamespaceAttrs) {
        try {
            String prefix = reader.getPrefix();
            String namespace = ancestorNamespaceAttrs.remove(prefix);
            if(namespace != null) {
                writer.writeNamespace(prefix, namespace);
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }
    
    private String fixNull(String s) {
        return s == null ? "" : s;
    }
    
    /**
     * Inherit the namespace attributes from e's parents and set it
     * in e, if the prefix is not already resolved.
     */
    private String resolvePrefix(
            Element e,
            Map<String,String> resolvedPrefixes) {
        String nodePrefix = fixNull(e.getPrefix());
        String nodeNamespace = e.getNamespaceURI();
        if(nodeNamespace != null) {
            /**
             * Set the namespace attribute if it is not already set
             */
            if(!resolvedPrefixes.containsKey(nodePrefix)) {
                String attrName = (nodePrefix.trim().length() == 0)
                ? "xmlns" : "xmlns:" + nodePrefix;
                e.setAttribute(attrName, nodeNamespace);
                resolvedPrefixes.put(nodePrefix, nodeNamespace);
            }
        }
        return nodePrefix;
    }
    
    
    /**
     * writes the node pointed by XMLStreamReader to the XMLStreamWriter
     * Strictly speaking, this method should not be in this class.
     *
     * @param reader XMLStreamReader from where the contents should be read.
     * @param writer XMLStreamWriter to where the contents should be written.
     */
    // TODO : handle attributes.
    public void writeNode(XMLStreamReader reader, XMLStreamWriter writer) throws Exception {
        writeNode(reader, writer, new HashMap<String,String>());
    }
    
    public void writeChildren(XMLStreamReader reader, XMLStreamWriter writer) throws Exception {
        writeChildren(reader, writer, new HashMap<String,String>());
    }
    
    public void writeNode(
            XMLStreamReader reader,
            XMLStreamWriter writer,
            Map<String,String> ancestorNamespaceAttrs) throws Exception {
        /**
         * CR 6652680.
         * ancestorNamespaceAttrs may be required by the other elements,
         * so don't remove stuff from it.
         */
        Map<String, String> copyOfANSA = 
                new HashMap<String,String>(ancestorNamespaceAttrs.size());
        copyOfANSA.putAll(ancestorNamespaceAttrs);
        int depth = 0;
        int event = reader.getEventType();
        do {
            switch(event) {
                case XMLStreamReader.START_DOCUMENT:
                    reader.next();
                    writeNode(reader, writer, copyOfANSA);
                    break;
                case XMLStreamReader.START_ELEMENT:
                    String tagName =
                            (reader.getPrefix() != null && reader.getPrefix().length() != 0)
                            ? reader.getPrefix() + ":" + reader.getLocalName()
                            : reader.getLocalName();
                    writer.writeStartElement(tagName);
                    writeAttributes(reader, writer);
                    writeAncestorNamespaceAttrs(reader, writer, copyOfANSA);
                    ++depth;
                    break;
                case XMLStreamReader.CHARACTERS:
                    writer.writeCharacters(reader.getText());
                    break;
                case XMLStreamReader.END_ELEMENT:
                    writer.writeEndElement();
                    --depth;
            }
            event = reader.next();
        } while(depth > 0);
    }
    
    public void writeChildren(
            XMLStreamReader reader,
            XMLStreamWriter writer,
            Map<String,String> ancestorNamespaceAttrs) throws Exception {
        reader.next();
        while(reader.getEventType() != XMLStreamReader.END_ELEMENT) {
            writeNode(reader, writer, ancestorNamespaceAttrs);
        }
        reader.next();
    }
    
    /**
     * Writes the node to the XMLStreamWriter as text.
     *
     * @param node Node from where the contents should be read.
     * @param writer XMLStreamWriter to where the contents should be written.
     */
    public void writeNode(Node node, XMLStreamWriter writer) throws Exception {
        writeNode(node, writer, new HashMap<String,String>());
    }
    
    public void writeChildren(XMLStreamWriter writer, Node node) throws Exception {
        NodeList l = node.getChildNodes();
        int size = l.getLength();
        for ( int i = 0; i < size; i++ ) {
            writeNode(l.item(i), writer, new HashMap<String,String>());
        }
    }
    
    /**
     * namespaceMap keeps track of the namespace attributes set
     * in the the ancestor node.
     */
    public void writeNode(Node node, XMLStreamWriter writer, Map<String,String> resolvedPrefixes) throws Exception {
        
        short type = node.getNodeType();
        switch ( type ) {
            
            case Node.DOCUMENT_NODE: {
                writer.writeStartDocument();
                writeChildren(writer, node, resolvedPrefixes);
                break;
            }
            
            case Node.DOCUMENT_FRAGMENT_NODE: {
                writeChildren(writer, node, resolvedPrefixes);
                break;
            }
            
            case Node.ELEMENT_NODE: {
                Element e = (Element)node;
                String n = e.getTagName();
                
                String resolvedPrefix = resolvePrefix(e, resolvedPrefixes);                
                writer.writeStartElement(n);
                
                NamedNodeMap a = e.getAttributes();
                int size = a.getLength();
                for ( int i = 0; i < size; i++ ) {
                    Attr att = (Attr)a.item(i);
                    writeNode(att, writer, resolvedPrefixes);
                }
                
                if ( e.hasChildNodes() ) {
                    writeChildren(writer, node, resolvedPrefixes);
                    writer.writeEndElement();
                    resolvedPrefixes.remove(resolvedPrefix);
                } else {
                    writer.writeEndElement();
                    resolvedPrefixes.remove(resolvedPrefix);
                }
                break;
            }
            
            case Node.ATTRIBUTE_NODE:
                Attr a = (Attr)node;
                writer.writeAttribute(a.getName(), a.getValue());
                break;
                
            case Node.PROCESSING_INSTRUCTION_NODE: {
                ProcessingInstruction pi = (ProcessingInstruction)node;
                writer.writeProcessingInstruction(pi.getTarget(), pi.getData());
                break;
            }
            
            case Node.TEXT_NODE: {
                writer.writeCharacters(node.getNodeValue());
                break;
            }
            
            case Node.CDATA_SECTION_NODE: {
                writer.writeCData(node.getNodeValue());
                break;
            }
            
            case Node.COMMENT_NODE: {
                writer.writeComment(node.getNodeValue());
                break;
            }
        }
    }
    
    /**
     * This method should be used only from
     * writeNode(node,writer,resolvedPrefixes) method.
     */
    private void writeChildren(XMLStreamWriter writer,
            Node node,
            Map<String,String> resolvedPrefixes) throws Exception {
        NodeList l = node.getChildNodes();
        int size = l.getLength();
        for ( int i = 0; i < size; i++ ) {
            writeNode(l.item(i), writer, resolvedPrefixes);
        }
    }
    
    
    /**
     * gets the element.
     *
     * @param aParentDocument Document for parent node
     * @param aTagName        String for tagname
     * @return Element with tagname
     */
    public Element getElement(
            Document aParentDocument,
            String aTagName) {
        NodeList nodeList = aParentDocument.getElementsByTagName(aTagName);
        
        return (nodeList != null) ? (Element) nodeList.item(0) : null;
    }
    
    /**
     * gets the element.
     *
     * @param elem    Element for parent node
     * @param tagName String for tagname
     * @return Element with tagname
     */
    public Element getElement(
            Element elem,
            String tagName
            ) {
        NodeList nl = elem.getElementsByTagName(tagName);
        Element childElem = (Element) nl.item(0);
        
        return childElem;
    }
    
    /**
     * gets the element value.
     *
     * @param doc      Document for parent node
     * @param elemName String for element name
     * @return Element value
     */
    public String getElementValue(
            Document doc,
            String elemName
            ) {
        String elemVal = null;
        
        Element elem = getElement(doc, elemName);
        elemVal = getTextData(elem);
        
        return elemVal;
    }
    
    /**
     * use this util method to create/retrieve a Text Node in a element.
     *
     * @param aElement Element node
     * @return Text node for text data
     */
    private Text getText(Element aElement) {
        Node node = null;
        aElement.normalize();
        node = aElement.getFirstChild();
        
        if ((node == null) || !(node instanceof Text)) {
            node = aElement.getOwnerDocument().createTextNode("");
            aElement.appendChild(node);
        }
        
        return (Text) node;
    }
    
    /**
     * use this util method to set a Text Data in a element.
     *
     * @param aElement Element for text node
     * @param aData    String contains text
     */
    public void setTextData(
            Element aElement,
            String aData
            ) {
        getText(aElement).setData(aData);
    }
    
    /**
     * use this util method to retrieve a Text Data in a element.
     *
     * @param aElement Element for text node
     * @return String contains text
     */
    public String getTextData(Element aElement) {
        return getText(aElement).getData();
    }
    
    /**
     * save document to stream.
     *
     * @param aDocument Document
     * @param aWriter   is what the aDocument is serialized to.
     * @return String representation of Document
     * @throws Exception If fails to construct a string.
     */
    public String DOM2String(Document aDocument,
            Writer aWriter) throws Exception {
        TransformerFactory transformerFactory =
                TransformerFactory.newInstance();
        Transformer transformer;
        
        try {
            transformer = transformerFactory.newTransformer();
        } catch (javax.xml.transform.TransformerConfigurationException e) {
            e.printStackTrace();
            transformer = null;
            throw e;
        }
        
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
        transformer.setOutputProperty(OutputKeys.CDATA_SECTION_ELEMENTS, "");
        transformer.setOutputProperty(OutputKeys.INDENT, "no");
        
        try {
            transformer.transform(new DOMSource(aDocument),
                    new StreamResult(aWriter));
        } catch (javax.xml.transform.TransformerException e) {
            e.printStackTrace();
            throw e;
        }
        
        return aWriter.toString();
    }
    
    /**
     * gets list of elements.
     *
     * @param aParentElement Element for parent
     * @param aTagName       String for tagname
     * @return NodeList with tagname
     */
    public NodeList getElements(
            Element aParentElement,
            String aTagName
            ) {
        return aParentElement.getElementsByTagNameNS(
                aParentElement.getNamespaceURI(),
                aTagName
                );
    }
    
    /**
     * gets set of elements.
     *
     * @param aParentDocument Document for parent node
     * @param aTagName        String for tagname
     * @return NodeList with tagname
     */
    public NodeList getElements(
            Document aParentDocument,
            String aTagName
            ) {
        return aParentDocument.getElementsByTagNameNS("*", aTagName);
    }
    
    /**
     * get the children of the same type element tag name.
     *
     * @param aElement        Element for parent node
     * @param aElementTagName String for tagname
     * @return NodeList for list of children with the tagname
     */
    public NodeList getChildElements(
            Element aElement,
            String aElementTagName
            ) {
        NodeList nodeList = aElement.getChildNodes();
        NodeListImpl list = new NodeListImpl();
        int count = nodeList.getLength();
        
        for (int i = 0; i < count; ++i) {
            Node node = nodeList.item(i);
            
            if (node instanceof Element) {
                String tagName = getElementName((Element) node);
                
                if (tagName.equals(aElementTagName)) {
                    list.add(node);
                }
            }
        }
        
        return list;
    }
    
    /**
     * get Element Tag Name with striped prefix.
     *
     * @param aElement Element object
     * @return String with stripped prefix
     */
    public String getElementName(Element aElement) {
        String tagName = aElement.getTagName();
        
        return getName(tagName);
    }
    
    /**
     * strips the prefix of the name if present.
     *
     * @param aName String value of Name with prefix
     * @return String for name after striping prefix
     */
    public String getName(String aName) {
        int lastIdx = aName.lastIndexOf(':');
        
        if (lastIdx >= 0) {
            return aName.substring(lastIdx + 1);
        }
        
        return aName;
    }
    
    /**
     * NodeListImpl.
     */
    public static class NodeListImpl extends ArrayList implements NodeList {
        /**
         * Default Constructor.
         */
        public NodeListImpl() {
            super();
        }
        
        /**
         * nodelist length.
         *
         * @return int for number of nodes in nodelist
         */
        public int getLength() {
            return this.size();
        }
        
        /**
         * return a node.
         *
         * @param aIndex int for the index of the node
         * @return Node at the index
         */
        public Node item(int aIndex) {
            return (Node) this.get(aIndex);
        }
    }
    
}

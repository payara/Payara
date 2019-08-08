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
// Portions Copyright [2016-2019] [Payara Foundation and/or its affiliates]
package com.sun.enterprise.deployment.node;

import com.sun.enterprise.deployment.*;
import com.sun.enterprise.deployment.node.runtime.RuntimeBundleNode;
import com.sun.enterprise.deployment.types.EjbReference;
import com.sun.enterprise.deployment.util.DOLUtils;
import com.sun.enterprise.deployment.xml.TagNames;
import com.sun.enterprise.deployment.xml.WebServicesTagNames;
import com.sun.enterprise.util.LocalStringManagerImpl;
import org.glassfish.deployment.common.Descriptor;
import org.glassfish.deployment.common.JavaEEResourceType;
import org.glassfish.internal.api.Globals;
import org.glassfish.hk2.api.ServiceLocator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;

import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;
import org.glassfish.config.support.TranslatedConfigView;

/**
 * Superclass of all Nodes implementation XMLNode implementation represents all the DOL classes responsible for handling
 * the XML deployment descriptors. These nodes are called by the SAX parser when reading and are constructed to build
 * the DOM tree for saving the XML files.
 *
 * XMLNode are organized like a tree with one root XMLNode (which implement the RootXMLNode interface) and sub XMLNodes
 * responsible for handling subparts of the XML documents. Sub XMLNodes register themselves to their parent XMLNode as
 * handlers for a particular XML subtag of the tag handled by the parent XMLNode
 *
 * Each XMLNode is therefore associated with a xml tag (located anywhere in the tree of tags as defined by the DTD). It
 * owns the responsibility for reading and writing the tag, its attributes and all subtags (by using delegation to sub
 * XMLNode if necessary).
 *
 * @author Jerome Dochez
 * @version
 */
public abstract class DeploymentDescriptorNode<T> implements XMLNode<T> {

    private static final String QNAME_SEPARATOR = ":";

    protected ServiceLocator serviceLocator = Globals.getDefaultHabitat();

    // Handlers is the list of XMLNodes registered for handling sub xml tags of the current
    // XMLNode
    protected Hashtable<String, Class> handlers;

    // List of add methods declared on the descriptor class to add sub descriptors extracted
    // by the handlers registered above. The key for the table is the xml root tag for the
    // descriptor to be added, the value is the method name to add such descriptor to the
    // current descriptor.
    private Hashtable addMethods;

    // Each node is associated with a XML tag it is handling
    private XMLElement xmlTag;

    // Parent node in the XML Nodes implementation tree we create to map to the XML
    // tags of the XML document
    protected XMLNode parentNode;

    // The root xml node in the XML Node implementation tree
    protected XMLNode rootNode;

    // default descriptor associated with this node, some sub nodes which
    // relies on the dispatch table don't really need to know the actual
    // type of the descriptor they deal with since they are populated through
    // reflection method calls
    protected Object abstractDescriptor;

    // for i18N
    protected static final LocalStringManagerImpl localStrings = new LocalStringManagerImpl(DeploymentDescriptorNode.class);

    /** Creates new DeploymentDescriptorNode */
    public DeploymentDescriptorNode() {
        registerElementHandler(new XMLElement(TagNames.DESCRIPTION), LocalizedInfoNode.class);
    }

    /**
     * @return the descriptor instance to associate with this XMLNode
     */
    @SuppressWarnings("unchecked")
    public T getDescriptor() {
        if (abstractDescriptor == null) {
            abstractDescriptor = createDescriptor();
        }
        
        return (T) abstractDescriptor;
    }

    protected Object createDescriptor() {
        return DescriptorFactory.getDescriptor(getXMLPath());
    }

    /**
     * Adds a new DOL descriptor instance to the descriptor instance associated with this XMLNode
     *
     * @param descriptor the new descriptor
     */
    public void addDescriptor(Object descriptor) {
        if (getParentNode() == null) {
            DOLUtils.getDefaultLogger().log(SEVERE, "enterprise.deployment.backend.addDescriptorFailure", new Object[] { descriptor, toString() });
            throw new RuntimeException("Cannot add " + descriptor + " to " + toString());
        }
        
        getParentNode().addDescriptor(descriptor);
    }

    /**
     * Adds a new DOL descriptor instance to the descriptor associated with this XMLNode
     * 
     * @param node the sub-node adding the descriptor;
     */
    protected void addNodeDescriptor(DeploymentDescriptorNode node) {

        // If there is no descriptor associated with this class, the addDescriptor should implement
        // the fate of this new descriptor.
        if (getDescriptor() == null) {
            addDescriptor(node.getDescriptor());
            return;
        }
        
        String xmlRootTag = node.getXMLRootTag().getQName();
        if (addMethods != null && addMethods.containsKey(xmlRootTag)) {
            try {

                Method toInvoke = null;
                if ((node.getDescriptor() instanceof ResourceDescriptor) && ((ResourceDescriptor) node.getDescriptor()).getResourceType() != null) {
                    toInvoke = getDescriptor().getClass().getMethod((String) addMethods.get(xmlRootTag), new Class[] { ResourceDescriptor.class });

                } else {
                    toInvoke = getDescriptor().getClass().getMethod((String) addMethods.get(xmlRootTag), new Class[] { node.getDescriptor().getClass() });
                }
                toInvoke.invoke(getDescriptor(), new Object[] { node.getDescriptor() });

            } catch (InvocationTargetException e) {
                Throwable t = e.getTargetException();
                if (t instanceof IllegalArgumentException) {
                    // We report the error but we continue loading, this will allow the verifier to catch these errors or to register
                    // an error handler for notification
                    DOLUtils.getDefaultLogger().log(SEVERE, "enterprise.deployment.backend.addDescriptorFailure",
                            new Object[] { node.getDescriptor().getClass(), getDescriptor().getClass() });
                } else {
                    DOLUtils.getDefaultLogger().log(WARNING, "Error occurred", t);
                    DOLUtils.getDefaultLogger().log(SEVERE, "enterprise.deployment.backend.addDescriptorFailure", new Object[] { t.toString(), null });
                }
            } catch (Throwable t) {
                DOLUtils.getDefaultLogger().log(SEVERE, "enterprise.deployment.backend.addDescriptorFailure",
                        new Object[] { node.getDescriptor().getClass(), getDescriptor().getClass() });
                DOLUtils.getDefaultLogger().log(SEVERE, "enterprise.deployment.backend.addDescriptorFailure", new Object[] { t.toString(), null });
                DOLUtils.getDefaultLogger().log(WARNING, "Error occurred", t);
            }
        } else {
            addDescriptor(node.getDescriptor());
        }
    }

    /**
     * set the parent node for the current instance.
     */
    public void setParentNode(XMLNode parentNode) {
        this.parentNode = parentNode;
    }

    /**
     * @return the parent node of the current instance
     */
    public XMLNode getParentNode() {
        return parentNode;
    }

    /**
     * @return the root node of the current instance
     */
    public XMLNode getRootNode() {
        XMLNode parent = this;

        while (parent.getParentNode() != null) {
            parent = parent.getParentNode();
        }

        return parent;
    }

    /**
     * register a new XMLNode handler for a particular XML tag.
     * 
     * @param element XMLElement is the XML tag this XMLNode will handle
     * @param handler the class implemenenting the XMLNode interface
     */
    protected void registerElementHandler(XMLElement element, Class handler) {
        if (handlers == null) {
            handlers = new Hashtable<String, Class>();
        }
        
        handlers.put(element.getQName(), handler);
    }

    /**
     * register a new XMLNode handler for a particular XML tag.
     * 
     * @param element XMLElement is the XML tag this XMLNode will handle
     * @param handler the class implemenenting the XMLNode interface
     * @param addMethodName is the method name for adding the descriptor extracted by the handler node to the current
     * descriptor
     */
    public void registerElementHandler(XMLElement element, Class handler, String addMethodName) {
        registerElementHandler(element, handler);
        
        if (addMethods == null) {
            addMethods = new Hashtable();
        }
        addMethods.put(element.getQName(), addMethodName);
    }

    /**
     * @return the XML tag associated with this XMLNode
     */
    protected XMLElement getXMLRootTag() {
        return xmlTag;
    }

    /**
     * sets the XML tag associated with this XMLNode
     */
    protected void setXMLRootTag(XMLElement element) {
        xmlTag = element;
    }

    /**
     * @return the handler registered for the subtag element of the curent XMLNode
     */
    public XMLNode getHandlerFor(XMLElement element) {
        if (handlers == null) {
            DOLUtils.getDefaultLogger().log(Level.WARNING, DOLUtils.INVALID_DESC_MAPPING, new Object[] { this, "No handler registered" });
            return null;
        }
        
        Class c = handlers.get(element.getQName());
        if (c == null) {
            DOLUtils.getDefaultLogger().log(Level.WARNING, DOLUtils.INVALID_DESC_MAPPING, new Object[] { element.getQName(), "No handler registered" });
            return null;
        }
        
        if (DOLUtils.getDefaultLogger().isLoggable(Level.FINER)) {
            DOLUtils.getDefaultLogger().finer("New Handler requested for " + c);
        }
        
        DeploymentDescriptorNode node;
        try {
            node = (DeploymentDescriptorNode) c.newInstance();
            node.setParentNode(this);
            node.setXMLRootTag(element);
            node.getDescriptor();
        } catch (Exception e) {
            DOLUtils.getDefaultLogger().log(Level.WARNING, "Error occurred", e);
            return null;
        }
        
        return node;
    
    }

    private Class getExtensionHandler(final XMLElement element) {
        DeploymentDescriptorNode extNode = (DeploymentDescriptorNode) serviceLocator.getService(XMLNode.class, element.getQName());
        if (extNode == null) {
            return null;
        }
        return extNode.getClass();
    }

    /**
     * SAX Parser API implementation, we don't really care for now.
     */
    public void startElement(XMLElement element, Attributes attributes) {
        // DOLUtils.getDefaultLogger().finer("STARTELEMENT : " + "in " + getXMLRootTag() + " Node, startElement " +
        // element.getQName());
        if (!this.getXMLRootTag().equals(element))
            return;

        if (attributes.getLength() > 0) {
            for (int i = 0; i < attributes.getLength(); i++) {
                if (DOLUtils.getDefaultLogger().isLoggable(Level.FINER)) {
                    DOLUtils.getDefaultLogger().finer("With attribute " + attributes.getQName(i));
                    DOLUtils.getDefaultLogger().finer("With value " + attributes.getValue(i));
                }
                // we try the setAttributeValue first, if not processed then the setElement
                String attrValue = TranslatedConfigView.expandValue(attributes.getValue(i));
                if (!setAttributeValue(element, new XMLElement(attributes.getQName(i)), attrValue)) {
                    setElementValue(new XMLElement(attributes.getQName(i)), attrValue);
                }

            }
        }
    }

    /**
     * parsed an attribute of an element
     *
     * @param elementName the element name
     * @param attributeName the attribute name
     * @param value the attribute value
     * @return true if the attribute was processed
     */
    protected boolean setAttributeValue(XMLElement elementName, XMLElement attributeName, String value) {
        // we do not support id attribute for the moment
        if (attributeName.getQName().equals(TagNames.ID)) {
            return true;
        }

        return false;
    }

    /**
     * receives notification of the end of an XML element by the Parser
     * 
     * @param element the xml tag identification
     * @return true if this node is done processing the XML sub tree
     */
    public boolean endElement(XMLElement element) {
        // DOLUtils.getDefaultLogger().finer("ENDELEMENT : " + "in " + getXMLRootTag() + " Node, endElement " +
        // element.getQName());
        boolean allDone = element.equals(getXMLRootTag());
        if (allDone) {
            postParsing();
            if (getParentNode() != null && getDescriptor() != null) {
                ((DeploymentDescriptorNode) getParentNode()).addNodeDescriptor(this);
            }
        }
        return allDone;
    }

    /**
     * notification of the end of XML parsing for this node
     */
    public void postParsing() {
    }

    /**
     * @return true if the element tag can be handled by any registered sub nodes of the current XMLNode
     */
    public boolean handlesElement(XMLElement element) {

        // Let's iterator over all the statically registered handlers to
        // find one which is responsible for handling the XML tag.
        if (handlers != null) {
            for (Enumeration handlersIterator = handlers.keys(); handlersIterator.hasMoreElements();) {
                String subElement = (String) handlersIterator.nextElement();
                if (element.getQName().equals(subElement)) {
                    // record element to node mapping for runtime nodes
                    recordNodeMapping(element.getQName(), handlers.get(subElement));
                    return false;
                }
            }
        }

        // let's now find if there is any dynamically registered handler
        // to handle this XML tag
        Class extHandler = getExtensionHandler(element);
        if (extHandler != null) {
            // if yes, we should add this handler to the table so
            // we don't need to look it up again later and also return
            // false
            registerElementHandler(new XMLElement(element.getQName()), extHandler);
            // record element to node mapping for runtime nodes
            recordNodeMapping(element.getQName(), extHandler);
            return false;
        }

        recordNodeMapping(element.getQName(), this.getClass());

        return true;
    }

    // record element to node mapping
    private void recordNodeMapping(String subElementName, Class handler) {
        XMLNode rootNode = getRootNode();
        if (rootNode instanceof RuntimeBundleNode) {
            ((RuntimeBundleNode) rootNode).recordNodeMapping(getXMLRootTag().getQName(), subElementName, handler);
        }
    }

    /**
     * receives notification of the value for a particular tag
     * 
     * @param element the xml element
     * @param value it's associated value
     */
    @Override
    public void setElementValue(XMLElement element, String value) {
        // DOLUtils.getDefaultLogger().finer("SETELEMENTVALUE : " + "in " + getXMLRootTag() + " Node, startElement " +
        // element.getQName());
        Map dispatchTable = getDispatchTable();

        if (dispatchTable != null) {
            if (dispatchTable.containsKey(element.getQName())) {
                if (dispatchTable.get(element.getQName()) == null) {
                    // we just ignore these values from the DDs
                    if (DOLUtils.getDefaultLogger().isLoggable(Level.FINER)) {
                        DOLUtils.getDefaultLogger().finer("Deprecated element " + element.getQName() + " with value " + value + " is ignored");
                    }
                    return;
                }
                try {
                    Object descriptor = getDescriptor();
                    if (descriptor != null) {
                        setDescriptorInfo(descriptor, (String) dispatchTable.get(element.getQName()), value);
                    } else {
                        DOLUtils.getDefaultLogger().log(Level.WARNING, DOLUtils.INVALID_DESC_MAPPING, new Object[] { element.getQName(), value });
                    }

                    return;
                } catch (InvocationTargetException e) {
                    DOLUtils.getDefaultLogger().log(Level.WARNING, DOLUtils.INVALID_DESC_MAPPING,
                            new Object[] { dispatchTable.get(element.getQName()), getDescriptor().getClass() });
                    Throwable t = e.getTargetException();
                    if (t instanceof IllegalArgumentException) {
                        // We report the error but we continue loading, this will allow the verifier to catch these errors or to register
                        // an error handler for notification
                        DOLUtils.getDefaultLogger().log(Level.WARNING, DOLUtils.INVALID_DESC_MAPPING, new Object[] { element, value });
                    } else {
                        DOLUtils.getDefaultLogger().log(Level.WARNING, DOLUtils.INVALID_DESC_MAPPING, new Object[] { t.toString(), null });
                        DOLUtils.getDefaultLogger().log(Level.WARNING, "Error occurred", t);
                    }
                } catch (Throwable t) {
                    DOLUtils.getDefaultLogger().log(Level.WARNING, DOLUtils.INVALID_DESC_MAPPING, new Object[] { t.toString(), null });
                    DOLUtils.getDefaultLogger().log(Level.WARNING, "Error occurred", t);
                }
            }
        }
        if (value.trim().length() != 0) {
            DOLUtils.getDefaultLogger().log(Level.WARNING, DOLUtils.INVALID_DESC_MAPPING, new Object[] { element.getQName(), value });
        }
        return;
    }

    /**
     * all sub-implementation of this class can use a dispatch table to map xml element to method name on the descriptor
     * class for setting the element value.
     * 
     * @return the map with the element name as a key, the setter method as a value
     */
    protected Map<String, String> getDispatchTable() {
        // no need to be synchronized for now
        Map<String, String> table = new HashMap<String, String>();
        table.put(TagNames.DESCRIPTION, "setDescription");
        return table;
    }

    /**
     * call a setter method on a descriptor with a new value
     * 
     * @param target the descriptor to use
     * @param methodName the setter method to invoke
     * @param value the new value for the field in the descriptor
     */
    protected void setDescriptorInfo(Object target, String methodName, String value)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {

        if (DOLUtils.getDefaultLogger().isLoggable(Level.FINE)) {
            DOLUtils.getDefaultLogger().fine("in " + target.getClass() + "  method  " + methodName + " with  " + value);
        }

        try {
            Method toInvoke = target.getClass().getMethod(methodName, new Class[] { String.class });
            toInvoke.invoke(target, new Object[] { value });
        } catch (NoSuchMethodException e1) {
            try {
                // try with int as a parameter
                Method toInvoke = target.getClass().getMethod(methodName, new Class[] { int.class });
                toInvoke.invoke(target, new Object[] { Integer.valueOf(value) });
            } catch (NumberFormatException nfe) {
                DOLUtils.getDefaultLogger().log(Level.WARNING, DOLUtils.INVALID_DESC_MAPPING, new Object[] { getXMLPath(), nfe.toString() });
            } catch (NoSuchMethodException e2) {
                // try with boolean as a parameter
                Method toInvoke = target.getClass().getMethod(methodName, new Class[] { boolean.class });
                toInvoke.invoke(target, new Object[] { Boolean.valueOf(value) });
            }
        }
    }

    /**
     * @return the XPath this XML Node is handling
     */
    public String getXMLPath() {
        if (getParentNode() != null) {
            return getParentNode().getXMLPath() + "/" + getXMLRootTag().getQName();
        } else {
            return getXMLRootTag().getQName();
        }
    }

    /**
     * write the descriptor class to a DOM tree and return it
     *
     * @param parent node in the DOM tree
     * @param descriptor the descriptor to write
     * @return the DOM tree top node
     */
    public Node writeDescriptor(Node parent, T descriptor) {
        return writeDescriptor(parent, getXMLRootTag().getQName(), descriptor);
    }

    /**
     * write the descriptor class to a DOM tree and return it
     *
     * @param parent node in the DOM tree
     * @param nodeName name for the root element for this DOM tree fragment
     * @param descriptor the descriptor to write
     * @return the DOM tree top node
     */
    public Node writeDescriptor(Node parent, String nodeName, T descriptor) {
        Node node = appendChild(parent, nodeName);
        return node;
    }

    /**
     * write all occurrences of the descriptor corresponding to the current node from the parent descriptor to an JAXP DOM
     * node and return it
     *
     * This API will be invoked by the parent node when the parent node writes out a mix of statically and dynamically
     * registered sub nodes.
     *
     * This method should be overriden by the sub classes if it needs to be called by the parent node.
     *
     * @param parent node in the DOM tree
     * @param nodeName the name of the node
     * @param parentDesc parent descriptor of the descriptor to be written
     * @return the JAXP DOM node
     */
    public Node writeDescriptors(Node parent, String nodeName, Descriptor parentDesc) {
        return parent;
    }

    /**
     * write out simple text element based on the node name to an JAXP DOM node and return it
     *
     * This API will be invoked by the parent node when the parent node writes out a mix of statically and dynamically
     * registered sub nodes. And this API is to write out the simple text sub element that the parent node handles itself.
     *
     * This method should be overriden by the sub classes if it needs to be called by the parent node.
     *
     * @param parent node in the DOM tree
     * @param nodeName node name of the node
     * @param parentDesc parent descriptor of the descriptor to be written
     * @return the JAXP DOM node
     */
    public Node writeSimpleTextDescriptor(Node parent, String nodeName, Descriptor parentDesc) {
        return parent;
    }

    /**
     * write out descriptor in a generic way to an JAXP DOM node and return it
     *
     * This API will generally be invoked when the parent node needs to write out a mix of statically and dynamically
     * registered sub nodes.
     *
     * @param node current node in the DOM tree
     * @param nodeName node name of the node
     * @param descriptor the descriptor to be written
     * @return the JAXP DOM node for this descriptor
     */
    public Node writeSubDescriptors(Node node, String nodeName, Descriptor descriptor) {
        XMLNode rootNode = getRootNode();
        if (rootNode instanceof RuntimeBundleNode) {
            // we only support this for runtime xml
            LinkedHashMap<String, Class> elementToNodeMappings = ((RuntimeBundleNode) rootNode).getNodeMappings(nodeName);

            if (elementToNodeMappings != null) {
                Set<Map.Entry<String, Class>> entrySet = elementToNodeMappings.entrySet();
                Iterator<Map.Entry<String, Class>> entryIt = entrySet.iterator();
                while (entryIt.hasNext()) {
                    Map.Entry<String, Class> entry = entryIt.next();
                    String subElementName = entry.getKey();
                    // skip if it's the element itself and not the subelement
                    if (subElementName.equals(nodeName)) {
                        continue;
                    }
                    Class handlerClass = entry.getValue();
                    if (handlerClass.getName().equals(this.getClass().getName())) {
                        // if this sublement is handled by the current node
                        // it is a simple text element, just append the text
                        // element based on the node name
                        writeSimpleTextDescriptor(node, subElementName, descriptor);
                    } else {
                        // if this sublement is handled by a sub node
                        // write all occurences of this sub node under
                        // the parent node
                        try {
                            DeploymentDescriptorNode subNode = (DeploymentDescriptorNode) handlerClass.newInstance();
                            subNode.setParentNode(this);
                            subNode.writeDescriptors(node, subElementName, descriptor);
                        } catch (Exception e) {
                            DOLUtils.getDefaultLogger().log(Level.WARNING, e.getMessage(), e);
                        }
                    }
                }
            }
        }
        return node;
    }

    /**
     * <p>
     * 
     * @return the Document for the given node
     * </p>
     */
    static protected Document getOwnerDocument(Node node) {

        if (node instanceof Document) {
            return (Document) node;
        }
        return node.getOwnerDocument();
    }

    /**
     * <p>
     * Append a new element child to the current node
     * </p>
     * 
     * @param parent is the parent node for the new child element
     * @param elementName is new element tag name
     * @return the newly created child node
     */
    public static Element appendChild(Node parent, String elementName) {
        Element child = getOwnerDocument(parent).createElement(elementName);
        parent.appendChild(child);
        return child;
    }

    /**
     * <p>
     * Append a new text child
     * </p>
     * 
     * @param parent for the new child element
     * @param elementName is the new element tag name
     * @param text the text for the new element
     * @result the newly create child node
     */
    public static Node appendTextChild(Node parent, String elementName, String text) {

        if (text == null || text.length() == 0)
            return null;

        Node child = appendChild(parent, elementName);
        child.appendChild(getOwnerDocument(child).createTextNode(text));
        return child;
    }

    /**
     * <p>
     * Append a new text child
     * </p>
     * 
     * @param parent for the new child element
     * @param elementName is the new element tag name
     * @param value the int value for the new element
     * @result the newly create child node
     */
    public static Node appendTextChild(Node parent, String elementName, int value) {
        return appendTextChild(parent, elementName, String.valueOf(value));
    }

    /**
     * <p>
     * Append a new text child even if text is empty
     * </p>
     * 
     * @param parent for the new child element
     * @param elementName is the new element tag name
     * @param text the text for the new element
     * @result the newly create child node
     */
    public static Node forceAppendTextChild(Node parent, String elementName, String text) {

        Node child = appendChild(parent, elementName);
        if (text != null && text.length() != 0) {
            child.appendChild(getOwnerDocument(child).createTextNode(text));
        }
        return child;
    }

    /**
     * <p>
     * Append a new attribute to an element
     * </p>
     * 
     * @param parent for the new child element
     * @param elementName is the new element tag name
     * @param text the text for the new element
     * @result the newly create child node
     */
    public static void setAttribute(Element parent, String elementName, String text) {

        if (text == null || text.length() == 0)
            return;
        parent.setAttribute(elementName, text);
    }

    /**
     * Set a namespace attribute on an element.
     * 
     * @param element on which to set attribute
     * @param prefix raw prefix (without "xmlns:")
     * @param namespaceURI namespace URI to which prefix is mapped.
     */
    public static void setAttributeNS(Element element, String prefix, String namespaceURI) {

        String nsPrefix = prefix.equals("") ? "xmlns" : "xmlns" + QNAME_SEPARATOR + prefix;

        element.setAttributeNS("http://www.w3.org/2000/xmlns/", nsPrefix, namespaceURI);
    }

    /**
     * write a list of env entry descriptors to a DOM Tree
     *
     * @param parentNode parent node for the DOM tree
     * @param envEntries the iterator over the descriptors to write
     */
    protected void writeEnvEntryDescriptors(Node parentNode, Iterator envEntries) {

        if (envEntries == null || !envEntries.hasNext())
            return;

        EnvEntryNode subNode = new EnvEntryNode();
        for (; envEntries.hasNext();) {
            EnvironmentProperty envProp = (EnvironmentProperty) envEntries.next();
            subNode.writeDescriptor(parentNode, TagNames.ENVIRONMENT_PROPERTY, envProp);
        }
    }

    /**
     * write the ejb references (local or remote) to the DOM tree
     *
     * @param parentNode parent node for the DOM tree
     * @param refs the set of EjbReferenceDescriptor to write
     */
    protected void writeEjbReferenceDescriptors(Node parentNode, Iterator refs) {

        if (refs == null || !refs.hasNext())
            return;

        EjbReferenceNode subNode = new EjbReferenceNode();
        // ejb-ref*
        Set localRefDescs = new HashSet();
        for (; refs.hasNext();) {
            EjbReference ejbRef = (EjbReference) refs.next();
            if (ejbRef.isLocal()) {
                localRefDescs.add(ejbRef);
            } else {
                subNode.writeDescriptor(parentNode, TagNames.EJB_REFERENCE, ejbRef);
            }
        }
        // ejb-local-ref*
        for (Iterator e = localRefDescs.iterator(); e.hasNext();) {
            EjbReference ejbRef = (EjbReference) e.next();
            subNode.writeDescriptor(parentNode, TagNames.EJB_LOCAL_REFERENCE, ejbRef);
        }
    }

    protected void writeServiceReferenceDescriptors(Node parentNode, Iterator refs) {
        if ((refs == null) || !refs.hasNext()) {
            return;
        }

        JndiEnvRefNode serviceRefNode = serviceLocator.getService(JndiEnvRefNode.class, WebServicesTagNames.SERVICE_REF);
        if (serviceRefNode != null) {
            while (refs.hasNext()) {
                ServiceReferenceDescriptor next = (ServiceReferenceDescriptor) refs.next();
                serviceRefNode.writeDeploymentDescriptor(parentNode, next);
            }
        }
    }

    /**
     * write a list of resource reference descriptors to a DOM Tree
     *
     * @param parentNode parent node for the DOM tree
     * @param resRefs the iterator over the descriptors to write
     */
    protected void writeResourceRefDescriptors(Node parentNode, Iterator resRefs) {

        if (resRefs == null || !resRefs.hasNext())
            return;

        ResourceRefNode subNode = new ResourceRefNode();
        for (; resRefs.hasNext();) {
            ResourceReferenceDescriptor aResRef = (ResourceReferenceDescriptor) resRefs.next();
            subNode.writeDescriptor(parentNode, TagNames.RESOURCE_REFERENCE, aResRef);
        }
    }

    /**
     * write a list of resource env reference descriptors to a DOM Tree
     *
     * @param parentNode parent node for the DOM tree
     * @param resRefs the iterator over the descriptors to write
     */
    protected void writeResourceEnvRefDescriptors(Node parentNode, Iterator resRefs) {

        if (resRefs == null || !resRefs.hasNext())
            return;

        ResourceEnvRefNode subNode = new ResourceEnvRefNode();
        for (; resRefs.hasNext();) {
            ResourceEnvReferenceDescriptor aResRef = (ResourceEnvReferenceDescriptor) resRefs.next();
            subNode.writeDescriptor(parentNode, TagNames.RESOURCE_ENV_REFERENCE, aResRef);
        }
    }

    /**
     * write a list of message destination reference descriptors to a DOM Tree
     *
     * @param parentNode parent node for the DOM tree
     * @param msgDestRefs the iterator over the descriptors to write
     */
    protected void writeMessageDestinationRefDescriptors(Node parentNode, Iterator msgDestRefs) {

        if (msgDestRefs == null || !msgDestRefs.hasNext())
            return;

        MessageDestinationRefNode subNode = new MessageDestinationRefNode();
        for (; msgDestRefs.hasNext();) {
            MessageDestinationReferenceDescriptor next = (MessageDestinationReferenceDescriptor) msgDestRefs.next();
            subNode.writeDescriptor(parentNode, TagNames.MESSAGE_DESTINATION_REFERENCE, next);
        }
    }

    /**
     * write a list of entity manager reference descriptors to a DOM Tree
     *
     * @param parentNode parent node for the DOM tree
     * @param entityMgrRefs the iterator over the descriptors to write
     */
    protected void writeEntityManagerReferenceDescriptors(Node parentNode, Iterator entityMgrRefs) {

        if (entityMgrRefs == null || !entityMgrRefs.hasNext())
            return;

        EntityManagerReferenceNode subNode = new EntityManagerReferenceNode();
        for (; entityMgrRefs.hasNext();) {
            EntityManagerReferenceDescriptor aEntityMgrRef = (EntityManagerReferenceDescriptor) entityMgrRefs.next();
            subNode.writeDescriptor(parentNode, TagNames.PERSISTENCE_CONTEXT_REF, aEntityMgrRef);
        }
    }

    /**
     * write a list of entity manager factory reference descriptors to a DOM Tree
     *
     * @param parentNode parent node for the DOM tree
     * @param entityMgrFactoryRefs the iterator over the descriptors to write
     */
    protected void writeEntityManagerFactoryReferenceDescriptors(Node parentNode, Iterator entityMgrFactoryRefs) {

        if (entityMgrFactoryRefs == null || !entityMgrFactoryRefs.hasNext())
            return;

        EntityManagerFactoryReferenceNode subNode = new EntityManagerFactoryReferenceNode();
        for (; entityMgrFactoryRefs.hasNext();) {
            EntityManagerFactoryReferenceDescriptor aEntityMgrFactoryRef = (EntityManagerFactoryReferenceDescriptor) entityMgrFactoryRefs.next();
            subNode.writeDescriptor(parentNode, TagNames.PERSISTENCE_UNIT_REF, aEntityMgrFactoryRef);
        }
    }

    /**
     * write a list of life-cycle-callback descriptors to a DOM Tree
     *
     * @param parentNode parent node for the DOM tree
     * @param tagName the tag name for the descriptors
     * @param lifecycleCallbacks the iterator over the descriptors to write
     */
    protected void writeLifeCycleCallbackDescriptors(Node parentNode, String tagName, Collection<LifecycleCallbackDescriptor> lifecycleCallbacks) {
        if (lifecycleCallbacks == null || lifecycleCallbacks.isEmpty())
            return;

        LifecycleCallbackNode subNode = new LifecycleCallbackNode();
        for (LifecycleCallbackDescriptor lcd : lifecycleCallbacks) {
            subNode.writeDescriptor(parentNode, tagName, lcd);
        }
    }

    /**
     * write a list of all descriptors to a DOM Tree
     *
     * @param parentNode parent node for the DOM tree
     * @param descriptorIterator the iterator over the descriptors to write
     */
    protected void writeResourceDescriptors(Node parentNode, Iterator<ResourceDescriptor> descriptorIterator) {
        if (descriptorIterator == null || !descriptorIterator.hasNext()) {
            return;
        }

        DataSourceDefinitionNode dataSourceDefinitionNode = new DataSourceDefinitionNode();
        MailSessionNode mailSessionNode = new MailSessionNode();
        ConnectionFactoryDefinitionNode connectionFactoryDefinitionNode = new ConnectionFactoryDefinitionNode();
        AdministeredObjectDefinitionNode administeredObjectDefinitionNode = new AdministeredObjectDefinitionNode();
        JMSConnectionFactoryDefinitionNode jmsConnectionFactoryDefinitionNode = new JMSConnectionFactoryDefinitionNode();
        JMSDestinationDefinitionNode jmsDestinationDefinitionNode = new JMSDestinationDefinitionNode();

        for (; descriptorIterator.hasNext();) {
            ResourceDescriptor descriptor = descriptorIterator.next();

            if (descriptor.getResourceType().equals(JavaEEResourceType.DSD)) {
                DataSourceDefinitionDescriptor next = (DataSourceDefinitionDescriptor) descriptor;
                dataSourceDefinitionNode.writeDescriptor(parentNode, TagNames.DATA_SOURCE, next);
            } else if (descriptor.getResourceType().equals(JavaEEResourceType.MSD)) {
                MailSessionDescriptor next = (MailSessionDescriptor) descriptor;
                mailSessionNode.writeDescriptor(parentNode, TagNames.MAIL_SESSION, next);
            } else if (descriptor.getResourceType().equals(JavaEEResourceType.CFD)) {
                ConnectionFactoryDefinitionDescriptor next = (ConnectionFactoryDefinitionDescriptor) descriptor;
                connectionFactoryDefinitionNode.writeDescriptor(parentNode, TagNames.CONNECTION_FACTORY, next);
            } else if (descriptor.getResourceType().equals(JavaEEResourceType.AODD)) {
                AdministeredObjectDefinitionDescriptor next = (AdministeredObjectDefinitionDescriptor) descriptor;
                administeredObjectDefinitionNode.writeDescriptor(parentNode, TagNames.ADMINISTERED_OBJECT, next);
            } else if (descriptor.getResourceType().equals(JavaEEResourceType.JMSCFDD)) {
                JMSConnectionFactoryDefinitionDescriptor next = (JMSConnectionFactoryDefinitionDescriptor) descriptor;
                jmsConnectionFactoryDefinitionNode.writeDescriptor(parentNode, TagNames.JMS_CONNECTION_FACTORY, next);
            } else if (descriptor.getResourceType().equals(JavaEEResourceType.JMSDD)) {
                JMSDestinationDefinitionDescriptor next = (JMSDestinationDefinitionDescriptor) descriptor;
                jmsDestinationDefinitionNode.writeDescriptor(parentNode, TagNames.JMS_DESTINATION, next);
            }
        }
    }

    /**
     * writes iocalized descriptions (if any) to the DOM node
     */
    protected void writeLocalizedDescriptions(Node node, Descriptor desc) {

        LocalizedInfoNode localizedNode = new LocalizedInfoNode();
        localizedNode.writeLocalizedMap(node, TagNames.DESCRIPTION, desc.getLocalizedDescriptions());
    }

    /**
     * writes jndi environment references group nodes
     */
    protected void writeJNDIEnvironmentRefs(Node node, JndiNameEnvironment descriptor) {

        /*
         * <xsd:element name="env-entry" type="javaee:env-entryType" minOccurs="0" maxOccurs="unbounded"/>
         */
        writeEnvEntryDescriptors(node, descriptor.getEnvironmentProperties().iterator());

        /*
         * <xsd:element name="ejb-ref" type="javaee:ejb-refType" minOccurs="0" maxOccurs="unbounded"/>
         */
        /*
         * <xsd:element name="ejb-local-ref" type="javaee:ejb-local-refType" minOccurs="0" maxOccurs="unbounded"/>
         */
        writeEjbReferenceDescriptors(node, descriptor.getEjbReferenceDescriptors().iterator());

        /*
         * <xsd:group ref="javaee:service-refGroup"/>
         */
        writeServiceReferenceDescriptors(node, descriptor.getServiceReferenceDescriptors().iterator());

        /*
         * <xsd:element name="resource-ref" type="javaee:resource-refType" minOccurs="0" maxOccurs="unbounded"/>
         */
        writeResourceRefDescriptors(node, descriptor.getResourceReferenceDescriptors().iterator());

        /*
         * <xsd:element name="resource-env-ref" type="javaee:resource-env-refType" minOccurs="0" maxOccurs="unbounded"/>
         */
        writeResourceEnvRefDescriptors(node, descriptor.getResourceEnvReferenceDescriptors().iterator());

        /*
         * <xsd:element name="message-destination-ref" type="javaee:message-destination-refType" minOccurs="0"
         * maxOccurs="unbounded"/>
         */
        writeMessageDestinationRefDescriptors(node, descriptor.getMessageDestinationReferenceDescriptors().iterator());

        /*
         * <xsd:element name="persistence-context-ref" type="javaee:persistence-context-refType" minOccurs="0"
         * maxOccurs="unbounded"/>
         */
        writeEntityManagerReferenceDescriptors(node, descriptor.getEntityManagerReferenceDescriptors().iterator());

        /*
         * <xsd:element name="persistence-unit-ref" type="javaee:persistence-unit-refType" minOccurs="0" maxOccurs="unbounded"/>
         */
        writeEntityManagerFactoryReferenceDescriptors(node, descriptor.getEntityManagerFactoryReferenceDescriptors().iterator());

        /*
         * <xsd:element name="post-construct" type="javaee:lifecycle-callbackType" minOccurs="0" maxOccurs="unbounded"/>
         */
        writeLifeCycleCallbackDescriptors(node, TagNames.POST_CONSTRUCT, descriptor.getPostConstructDescriptors());

        /*
         * <xsd:element name="pre-destroy" type="javaee:lifecycle-callbackType" minOccurs="0" maxOccurs="unbounded"/>
         */
        writeLifeCycleCallbackDescriptors(node, TagNames.PRE_DESTROY, descriptor.getPreDestroyDescriptors());
    }

    /**
     * Any node can now declare its own namespace. this apply to DDs only when dealing with deployment extensions. Write any
     * declared namespace declaration
     *
     * @param node from which this namespace is declared
     * @param descriptor containing the namespace declaration if any
     */
    protected void addNamespaceDeclaration(Element node, Descriptor descriptor) {

        // declare now all remaining namepace...
        Map<String, String> prefixMapping = (descriptor != null) ? descriptor.getPrefixMapping() : null;
        if (prefixMapping != null) {
            Set<Map.Entry<String, String>> entrySet = prefixMapping.entrySet();
            Iterator<Map.Entry<String, String>> entryIt = entrySet.iterator();
            while (entryIt.hasNext()) {
                Map.Entry<String, String> entry = entryIt.next();
                String prefix = entry.getKey();
                String namespaceURI = entry.getValue();
                setAttributeNS(node, prefix, namespaceURI);
            }
        }
    }

    /**
     * notify of a new prefix mapping used in this document
     */
    public void addPrefixMapping(String prefix, String uri) {
        Object o = getDescriptor();
        if (o instanceof Descriptor) {
            Descriptor descriptor = (Descriptor) o;
            descriptor.addPrefixMapping(prefix, uri);
        }
    }

    /**
     * Resolve a QName prefix to its corresponding Namespace URI by searching up node chain starting with child.
     */
    public String resolvePrefix(XMLElement element, String prefix) {
        // If prefix is empty string, returned namespace URI
        // is the default namespace.
        return element.getPrefixURIMapping(prefix);
    }

    /**
     * @return namespace URI prefix from qname, where qname is an xsd:QName, or the empty string if there is no prefix.
     *
     * QName ::= (Prefix ':')? LocalPart
     */
    public String getPrefixFromQName(String qname) {
        StringTokenizer tokenizer = new StringTokenizer(qname, QNAME_SEPARATOR);
        return (tokenizer.countTokens() == 2) ? tokenizer.nextToken() : "";
    }

    /**
     * Return local part from qname, where qname is an xsd:QName.
     *
     * QName ::= (Prefix ':')? LocalPart
     */
    public String getLocalPartFromQName(String qname) {
        StringTokenizer tokenizer = new StringTokenizer(qname, QNAME_SEPARATOR);
        String localPart = qname;
        if (tokenizer.countTokens() == 2) {
            // skip namespace prefix.
            tokenizer.nextToken();
            localPart = tokenizer.nextToken();
        }
        return localPart;
    }

    public String composeQNameValue(String prefix, String localPart) {
        return ((prefix != null) && !(prefix.equals(""))) ? prefix + QNAME_SEPARATOR + localPart : localPart;
    }

    public void appendQNameChild(String elementName, Node parent, String namespaceUri, String localPart, String prefix) {
        if (prefix == null) {
            // @@@ make configurable??
            prefix = elementName + "_ns__";
        }

        String elementValue = composeQNameValue(prefix, localPart);
        Element element = (Element) appendTextChild(parent, elementName, elementValue);
        // Always set prefix mapping on leaf node. If the DOL was
        // populated from an existing deployment descriptor it does
        // not preserve the original node structure of the XML document,
        // so we can't reliably know what level to place mapping.
        // Alternatively, if we're writing out a descriptor that was created
        // by the deploytool, there is no prefix->namespace information in
        // the first place.
        if (element != null) {
            setAttributeNS(element, prefix, namespaceUri);
        }

    }
}

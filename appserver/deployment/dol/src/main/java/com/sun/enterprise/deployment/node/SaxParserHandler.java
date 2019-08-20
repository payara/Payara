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

import com.sun.enterprise.deployment.EnvironmentProperty;
import com.sun.enterprise.deployment.util.DOLUtils;
import com.sun.enterprise.deployment.xml.DTDRegistry;
import com.sun.enterprise.deployment.xml.TagNames;
import com.sun.enterprise.util.LocalStringManagerImpl;
import org.glassfish.config.support.TranslatedConfigView;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.NamespaceSupport;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;

import static java.util.Collections.unmodifiableSet;
import static java.util.logging.Level.FINE;

/**
 * This class implements all the callbacks for the SAX Parser in JAXP 1.1
 *
 * @author Jerome Dochez
 * @version
 */
@Service
@PerLookup
public class SaxParserHandler extends DefaultHandler {

    public static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
    public static final String JAXP_SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";
    public static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";

    private static final String TRUE_STR = "true";
    private static final String FALSE_STR = "false";

    private static final class MappingStuff {
        public final ConcurrentMap<String, Boolean> bundleRegistrationStatus = new ConcurrentHashMap<String, Boolean>();
        public final ConcurrentMap<String, String> mapping = new ConcurrentHashMap<String, String>();

        private final ConcurrentMap<String, Class<?>> rootNodesMutable;
        public final Map<String, Class<?>> rootNodes;

        private final CopyOnWriteArraySet<String> elementsAllowingEmptyValuesMutable;
        public final Collection<String> elementsAllowingEmptyValues;

        private final CopyOnWriteArraySet<String> elementsPreservingWhiteSpaceMutable;
        public final Collection<String> elementsPreservingWhiteSpace;
        private final Map<String, List<Class<?>>> versionUpgradeClasses;
        private final Map<String, List<VersionUpgrade>> versionUpgrades;

        MappingStuff() {
            rootNodesMutable = new ConcurrentHashMap<>();
            rootNodes = Collections.unmodifiableMap(rootNodesMutable);

            elementsAllowingEmptyValuesMutable = new CopyOnWriteArraySet<>();
            elementsAllowingEmptyValues = Collections.unmodifiableSet(elementsAllowingEmptyValuesMutable);

            elementsPreservingWhiteSpaceMutable = new CopyOnWriteArraySet<>();
            elementsPreservingWhiteSpace = unmodifiableSet(elementsPreservingWhiteSpaceMutable);
            versionUpgradeClasses = new ConcurrentHashMap<>();
            versionUpgrades = new ConcurrentHashMap<>();
        }
    }

    private static final MappingStuff MAPPING_STUFF = new MappingStuff();

    private final List<XMLNode> nodes = new ArrayList<XMLNode>();
    public XMLNode topNode = null;
    protected String publicID = null;
    private StringBuilder elementData = null;
    private Map<String, String> prefixMapping = null;

    private boolean stopOnXMLErrors = false;

    private boolean pushedNamespaceContext = false;
    private NamespaceSupport namespaces = new NamespaceSupport();

    private Stack elementStack = new Stack();

    private static final LocalStringManagerImpl localStrings = new LocalStringManagerImpl(SaxParserHandler.class);

    protected static Map<String, String> getMapping() {
        return MAPPING_STUFF.mapping;
    }

    protected static List<VersionUpgrade> getVersionUpgrades(String key) {
        List<VersionUpgrade> versionUpgradeList = MAPPING_STUFF.versionUpgrades.get(key);
        if (versionUpgradeList != null) {
            return versionUpgradeList;
        }

        List<Class<?>> classList = MAPPING_STUFF.versionUpgradeClasses.get(key);
        if (classList == null) {
            return null;
        }

        versionUpgradeList = new ArrayList<VersionUpgrade>();
        for (int n = 0; n < classList.size(); ++n) {
            VersionUpgrade versionUpgrade = null;
            try {
                versionUpgrade = (VersionUpgrade) classList.get(n).newInstance();
            } catch (Exception ex) {
            }
            if (versionUpgrade != null) {
                versionUpgradeList.add(versionUpgrade);
            }
        }
        MAPPING_STUFF.versionUpgrades.put(key, versionUpgradeList);

        return versionUpgradeList;
    }

    protected static Collection<String> getElementsAllowingEmptyValues() {
        return MAPPING_STUFF.elementsAllowingEmptyValues;
    }

    protected static Collection<String> getElementsPreservingWhiteSpace() {
        return MAPPING_STUFF.elementsPreservingWhiteSpace;
    }

    private String rootElement = null;

    private List<VersionUpgrade> versionUpgradeList = null;

    private boolean doDelete = false;

    public static void registerBundleNode(BundleNode bn, String bundleTagName) {
        /*
         * There is exactly one standard node object for each descriptor type. The node's registerBundle method itself adds the
         * publicID-to-DTD entry to the mapping. This method needs to add the tag-to-node class entry to the rootNodes map.
         */
        if (MAPPING_STUFF.bundleRegistrationStatus.containsKey(bundleTagName)) {
            return;
        }

        HashMap<String, String> dtdMapping = new HashMap<String, String>();
        Map<String, List<Class<?>>> versionUpgrades = new HashMap<>();

        String rootNodeKey = bn.registerBundle(dtdMapping);
        MAPPING_STUFF.rootNodesMutable.putIfAbsent(rootNodeKey, bn.getClass());

        /*
         * There can be multiple runtime nodes (for example, sun-xxx and glassfish-xxx). So the BundleNode's
         * registerRuntimeBundle updates the publicID-to-DTD map and returns a map of tags to runtime node classes.
         */
        MAPPING_STUFF.rootNodesMutable.putAll(bn.registerRuntimeBundle(dtdMapping, versionUpgrades));

        MAPPING_STUFF.versionUpgradeClasses.putAll(versionUpgrades);

        // Let's remove the URL from the DTD so we use local copies...
        for (Map.Entry<String, String> entry : dtdMapping.entrySet()) {
            final String publicID = entry.getKey();
            final String dtd = entry.getValue();
            String systemIDResolution = resolvePublicID(publicID, dtd);
            if (systemIDResolution == null) {
                MAPPING_STUFF.mapping.put(publicID, dtd.substring(dtd.lastIndexOf('/') + 1));
            } else {
                MAPPING_STUFF.mapping.put(publicID, systemIDResolution);
            }
        }

        /*
         * This node might know of elements which should permit empty values, or elements for which we should preserve white
         * space. Track them.
         */
        Collection<String> c = bn.elementsAllowingEmptyValue();
        if (c.size() > 0) {
            MAPPING_STUFF.elementsAllowingEmptyValuesMutable.addAll(c);
        }

        c = bn.elementsPreservingWhiteSpace();
        if (c.size() > 0) {
            MAPPING_STUFF.elementsPreservingWhiteSpaceMutable.addAll(c);
        }

        MAPPING_STUFF.bundleRegistrationStatus.put(rootNodeKey, Boolean.TRUE);
    }

    public InputSource resolveEntity(String publicID, String systemID) throws SAXException {
        try {
            if (DOLUtils.getDefaultLogger().isLoggable(Level.FINE)) {
                DOLUtils.getDefaultLogger().fine("Asked to resolve  " + publicID + " system id = " + systemID);
            }
            if (publicID == null) {
                // unspecified schema
                if (systemID == null || systemID.lastIndexOf('/') == systemID.length()) {
                    return null;
                }

                String fileName = null;
                String namespaceResolution = resolveSchemaNamespace(systemID);
                if (namespaceResolution != null) {
                    fileName = getSchemaURLFor(namespaceResolution);
                } else {
                    fileName = getSchemaURLFor(systemID.substring(systemID.lastIndexOf('/') + 1));
                }
                // if this is not a request for a schema located in our repository,
                // let's hope that the hint provided by schemaLocation is correct
                if (fileName == null) {
                    fileName = systemID;
                }
                if (DOLUtils.getDefaultLogger().isLoggable(FINE)) {
                    DOLUtils.getDefaultLogger().fine("Resolved to " + fileName);
                }

                return new InputSource(fileName);
            }

            if (getMapping().containsKey(publicID)) {
                this.publicID = publicID;
                return new InputSource(new BufferedInputStream(getDTDUrlFor(getMapping().get(publicID))));
            }
        } catch (Exception ioe) {
            DOLUtils.getDefaultLogger().log(Level.SEVERE, ioe.getMessage(), ioe);
            throw new SAXException(ioe);
        }

        return null;
    }

    /**
     * Sets if the parser should stop parsing and generate an SAXPArseException when the xml parsed contains errors in
     * regards to validation
     */
    public void setStopOnError(boolean stop) {
        stopOnXMLErrors = stop;
    }

    public void error(SAXParseException spe) throws SAXParseException {
        DOLUtils.getDefaultLogger().log(Level.SEVERE, "enterprise.deployment.backend.invalidDescriptorFailure",
                new Object[] { errorReportingString, String.valueOf(spe.getLineNumber()), String.valueOf(spe.getColumnNumber()), spe.getLocalizedMessage() });

        if (stopOnXMLErrors) {
            throw spe;
        }
    }

    public void fatalError(SAXParseException spe) throws SAXParseException {
        DOLUtils.getDefaultLogger().log(Level.SEVERE, "enterprise.deployment.backend.invalidDescriptorFailure",
                new Object[] { errorReportingString, String.valueOf(spe.getLineNumber()), String.valueOf(spe.getColumnNumber()), spe.getLocalizedMessage() });

        if (stopOnXMLErrors) {
            throw spe;
        }
    }

    /**
     * @return the input stream for a DTD public ID
     */
    protected InputStream getDTDUrlFor(String dtdFileName) {
        String dtdLoc = DTDRegistry.DTD_LOCATION.replace('/', File.separatorChar);
        File f = new File(dtdLoc + File.separatorChar + dtdFileName);

        try {
            return new BufferedInputStream(new FileInputStream(f));
        } catch (FileNotFoundException fnfe) {
            DOLUtils.getDefaultLogger().fine("Cannot find DTD " + dtdFileName);
            return null;
        }
    }

    /**
     * @return an URL for the schema location for a schema indentified by the passed parameter
     * @param schemaSystemID the system id for the schema
     */
    public static String getSchemaURLFor(String schemaSystemID) throws IOException {
        File f = getSchemaFileFor(schemaSystemID);
        if (f != null) {
            return f.toURI().toURL().toString();
        } else {
            return null;
        }
    }

    /**
     * @return a File pointer to the localtion of the schema indentified by the passed parameter
     * @param schemaSystemID the system id for the schema
     */
    public static File getSchemaFileFor(String schemaSystemID) throws IOException {
        if (DOLUtils.getDefaultLogger().isLoggable(Level.FINE)) {
            DOLUtils.getDefaultLogger().fine("Getting Schema " + schemaSystemID);
        }
        String schemaLoc = DTDRegistry.SCHEMA_LOCATION.replace('/', File.separatorChar);
        File f = new File(schemaLoc + File.separatorChar + schemaSystemID);
        if (!f.exists()) {
            DOLUtils.getDefaultLogger().fine("Cannot find schema " + schemaSystemID);
            return null;
        }
        return f;
    }

    /**
     * Determine whether the syatemID starts with a known namespace. If so, strip off that namespace and return the rest.
     * Otherwise, return null
     *
     * @param systemID The systemID to examine
     * @return the part if the namespace to find in the file system or null if the systemID does not start with a known
     * namespace
     */
    public static String resolveSchemaNamespace(String systemID) {
        List<String> namespaces = DOLUtils.getProprietarySchemaNamespaces();
        for (int n = 0; n < namespaces.size(); ++n) {
            String namespace = namespaces.get(n);
            if (systemID.startsWith(namespace)) {
                return systemID.substring(namespace.length());
            }
        }
        return null;
    }

    /**
     * Determine whether the publicID starts with a known proprietary value. If so, strip off that value and return the
     * rest. Otherwise, return null
     *
     * @param publicID The publicID to examine
     * @return the part if the namespace to find in the file system or null if the publicID does not start with a known
     * namespace
     */
    public static String resolvePublicID(String publicID, String dtd) {
        List<String> dtdStarts = DOLUtils.getProprietaryDTDStart();
        for (int n = 0; n < dtdStarts.size(); ++n) {
            String dtdStart = dtdStarts.get(n);
            if (dtd.startsWith(dtdStart)) {
                return dtd.substring(dtdStart.length());
            }
        }
        return null;
    }

    public void notationDecl(java.lang.String name, java.lang.String publicId, java.lang.String systemId) throws SAXException {
        if (DOLUtils.getDefaultLogger().isLoggable(Level.FINE)) {
            DOLUtils.getDefaultLogger().fine("Received notation " + name + " :=: " + publicId + " :=: " + systemId);
        }
    }

    public void startPrefixMapping(String prefix, String uri) throws SAXException {

        if (prefixMapping == null) {
            prefixMapping = new HashMap<String, String>();
        }

        // We need one namespace context per element, but any prefix mapping
        // callbacks occur *before* startElement is called. So, push a
        // context on the first startPrefixMapping callback per element.
        if (!pushedNamespaceContext) {
            namespaces.pushContext();
            pushedNamespaceContext = true;
        }
        namespaces.declarePrefix(prefix, uri);
        prefixMapping.put(prefix, uri);
    }

    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        if (!pushedNamespaceContext) {
            // We need one namespae context per element, so push a context
            // if there weren't any prefix mappings defined.
            namespaces.pushContext();
        }
        // Always reset flag since next callback could be startPrefixMapping
        // OR another startElement.
        pushedNamespaceContext = false;

        doDelete = false;
        String lastElement = null;
        try {
            lastElement = (String) elementStack.pop();
        } catch (EmptyStackException ex) {
        }
        if (lastElement == null) {
            rootElement = localName;
            versionUpgradeList = getVersionUpgrades(rootElement);
            if (versionUpgradeList != null) {
                for (int n = 0; n < versionUpgradeList.size(); ++n) {
                    VersionUpgrade versionUpgrade = versionUpgradeList.get(n);
                    versionUpgrade.init();
                }
            }
            elementStack.push(localName);
        } else {
            lastElement += "/" + localName;
            elementStack.push(lastElement);
        }

        if (versionUpgradeList != null) {
            for (int n = 0; n < versionUpgradeList.size(); ++n) {
                VersionUpgrade versionUpgrade = versionUpgradeList.get(n);
                if (VersionUpgrade.UpgradeType.REMOVE_ELEMENT == versionUpgrade.getUpgradeType()) {
                    Map<String, String> matchXPath = versionUpgrade.getMatchXPath();
                    int entriesMatched = 0;
                    for (Map.Entry<String, String> entry : matchXPath.entrySet()) {
                        if (entry.getKey().equals(lastElement)) {
                            entry.setValue(elementData.toString());
                            ++entriesMatched;
                        }
                    }
                    if (entriesMatched == matchXPath.size()) {
                        doDelete = true;
                        break;
                    }
                }
            }
        }

        if (DOLUtils.getDefaultLogger().isLoggable(Level.FINER)) {
            DOLUtils.getDefaultLogger().finer("start of element " + uri + " with local name " + localName + " and " + qName);
        }
        XMLNode node = null;
        elementData = new StringBuilder();

        if (nodes.isEmpty()) {
            // this must be a root element...
            Class rootNodeClass = MAPPING_STUFF.rootNodes.get(localName);
            if (rootNodeClass == null) {
                DOLUtils.getDefaultLogger().log(Level.SEVERE, DOLUtils.INVALID_DESC_MAPPING, new Object[] { localName, " not supported !" });
                if (stopOnXMLErrors) {
                    throw new IllegalArgumentException(localStrings.getLocalString("invalid.root.element", "{0} Element [{1}] is not a valid root element",
                            new Object[] { errorReportingString, localName }));
                }
            } else {
                try {
                    node = (XMLNode) rootNodeClass.newInstance();
                    if (DOLUtils.getDefaultLogger().isLoggable(Level.FINE)) {
                        DOLUtils.getDefaultLogger().fine("Instanciating " + node);
                    }
                    if (node instanceof RootXMLNode) {
                        if (publicID != null) {
                            ((RootXMLNode) node).setDocType(publicID);
                        }
                        addPrefixMapping(node);
                    }
                    nodes.add(node);
                    topNode = node;
                    node.getDescriptor();
                } catch (Exception e) {
                    DOLUtils.getDefaultLogger().log(Level.WARNING, "Error occurred", e);
                    return;
                }
            }
        } else {
            node = nodes.get(nodes.size() - 1);
        }
        if (node != null) {
            XMLElement element = new XMLElement(qName, namespaces);
            if (node.handlesElement(element)) {
                node.startElement(element, attributes);
            } else {
                if (DOLUtils.getDefaultLogger().isLoggable(Level.FINE)) {
                    DOLUtils.getDefaultLogger().fine("Asking for new handler for " + element + " to " + node);
                }
                XMLNode newNode = node.getHandlerFor(element);
                if (DOLUtils.getDefaultLogger().isLoggable(Level.FINE)) {
                    DOLUtils.getDefaultLogger().fine("Got " + newNode);
                }
                nodes.add(newNode);
                addPrefixMapping(newNode);
                newNode.startElement(element, attributes);
            }
        }
    }

    public void endElement(String uri, String localName, String qName) {

        String lastElement = null;
        try {
            lastElement = (String) elementStack.peek();
        } catch (EmptyStackException ex) {
        }

        if (DOLUtils.getDefaultLogger().isLoggable(Level.FINER)) {
            DOLUtils.getDefaultLogger().finer("End of element " + uri + " local name " + localName + " and " + qName + " value " + elementData);
        }
        if (nodes.size() == 0) {
            // no more nodes to pop
            elementData = null;
            return;
        }
        XMLElement element = new XMLElement(qName, namespaces);
        XMLNode topNode = nodes.get(nodes.size() - 1);
        if (elementData != null && (elementData.length() != 0 || allowsEmptyValue(element.getQName()))) {
            if (DOLUtils.getDefaultLogger().isLoggable(Level.FINER)) {
                DOLUtils.getDefaultLogger().finer("For element " + element.getQName() + " And value " + elementData);
            }
            boolean doReplace = false;
            String replacementName = null;
            String replacementValue = null;
            if (versionUpgradeList != null) {
                for (int n = 0; n < versionUpgradeList.size(); ++n) {
                    VersionUpgrade versionUpgrade = versionUpgradeList.get(n);
                    if (VersionUpgrade.UpgradeType.REPLACE_ELEMENT == versionUpgrade.getUpgradeType()) {
                        Map<String, String> matchXPath = versionUpgrade.getMatchXPath();
                        int entriesMatched = 0;
                        for (Map.Entry<String, String> entry : matchXPath.entrySet()) {
                            if (entry.getKey().equals(lastElement)) {
                                entry.setValue(elementData.toString());
                                ++entriesMatched;
                            }
                        }
                        if (entriesMatched == matchXPath.size()) {
                            if (versionUpgrade.isValid()) {
                                doReplace = true;
                                replacementName = versionUpgrade.getReplacementElementName();
                                replacementValue = versionUpgrade.getReplacementElementValue();
                            } else {
                                StringBuilder buf = new StringBuilder();
                                String errorString = "Invalid upgrade from <";
                                buf.append(errorString);
                                for (Map.Entry<String, String> entry : matchXPath.entrySet()) {
                                    buf.append(entry.getKey() + "  " + entry.getValue() + " >");
                                }
                                errorString = buf.toString();
                                DOLUtils.getDefaultLogger().log(Level.SEVERE, errorString);
                                // Since the elements are not replaced,
                                // there should be a parsing error
                            }
                            break;
                        }
                    }
                }
            }
            if (doReplace) {
                element = new XMLElement(replacementName, namespaces);
                topNode.setElementValue(element, TranslatedConfigView.expandValue(replacementValue));
            } else if (doDelete) {
                // don't set a value so that the element is not written out
            } else if (getElementsPreservingWhiteSpace().contains(element.getQName())) {
                topNode.setElementValue(element, TranslatedConfigView.expandValue(elementData.toString()));
            } else if (element.getQName().equals(TagNames.ENVIRONMENT_PROPERTY_VALUE)) {
                Object envEntryDesc = topNode.getDescriptor();
                if (envEntryDesc != null && envEntryDesc instanceof EnvironmentProperty) {
                    EnvironmentProperty envProp = (EnvironmentProperty) envEntryDesc;
                    // we need to preserve white space for env-entry-value
                    // if the env-entry-type is java.lang.String or
                    // java.lang.Character
                    if (envProp.getType() != null && (envProp.getType().equals("java.lang.String") || envProp.getType().equals("java.lang.Character"))) {
                        topNode.setElementValue(element, TranslatedConfigView.expandValue(elementData.toString()));
                    } else {
                        topNode.setElementValue(element, TranslatedConfigView.expandValue(elementData.toString().trim()));
                    }
                } else {
                    topNode.setElementValue(element, TranslatedConfigView.expandValue(elementData.toString().trim()));
                }
            } else {
                /*
                 * Allow any case for true/false & convert to lower case
                 */
                String val = TranslatedConfigView.expandValue(elementData.toString().trim());
                if (TRUE_STR.equalsIgnoreCase(val)) {
                    topNode.setElementValue(element, val.toLowerCase(Locale.US));
                } else if (FALSE_STR.equalsIgnoreCase(val)) {
                    topNode.setElementValue(element, val.toLowerCase(Locale.US));
                } else {
                    topNode.setElementValue(element, val);
                }
            }
            elementData = null;
        }
        if (topNode.endElement(element)) {
            if (DOLUtils.getDefaultLogger().isLoggable(Level.FINE)) {
                DOLUtils.getDefaultLogger().fine("Removing top node " + topNode);
            }
            nodes.remove(nodes.size() - 1);
        }

        namespaces.popContext();
        pushedNamespaceContext = false;

        try {
            lastElement = (String) elementStack.pop();
        } catch (EmptyStackException ex) {
        }
        if (lastElement != null) {
            if (lastElement.lastIndexOf('/') >= 0) {
                lastElement = lastElement.substring(0, lastElement.lastIndexOf('/'));
                elementStack.push(lastElement);
            }
        }
    }

    public void characters(char[] ch, int start, int stop) {
        if (elementData != null) {
            elementData = elementData.append(ch, start, stop);
        }
    }

    public XMLNode getTopNode() {
        return topNode;
    }

    public void setTopNode(XMLNode node) {
        topNode = node;
        nodes.add(node);
    }

    private void addPrefixMapping(XMLNode node) {
        if (prefixMapping != null) {
            for (Map.Entry<String, String> entry : prefixMapping.entrySet()) {
                node.addPrefixMapping(entry.getKey(), entry.getValue());
            }
            prefixMapping = null;
        }
    }

    private String errorReportingString = "";

    /**
     * Sets the error reporting context string
     */
    public void setErrorReportingString(String s) {
        errorReportingString = s;
    }

    /**
     * Indicates whether the element name is one for which empty values should be recorded.
     * <p>
     * If there were many tags that support empty values, it might make sense to have a constant list that contains all
     * those tag names. Then this method would search the list for the target elementName. Because this code is potentially
     * invoked for many elements that do not support empty values, and because the list is very small at the moment, the
     * current implementation uses an inelegant but fast equals test.
     * <p>
     * If the set of tags that should support empty values grows a little, extending the expression to
     *
     * elementName.equals(TAG_1) || elementName.equals(TAG_2) || ...
     *
     * might make sense. If the set of such tags grows sufficiently large, then a list-based approach might make more sense
     * even though it might prove to be slower.
     *
     * @param elementName the name of the element
     * @return boolean indicating whether empty values should be recorded for this element
     */
    private boolean allowsEmptyValue(String elementName) {
        return getElementsAllowingEmptyValues().contains(elementName);
    }
}

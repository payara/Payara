/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.admin.util;

import javax.xml.parsers.ParserConfigurationException;
import org.glassfish.api.admin.InstanceState;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.HashMap;
import org.xml.sax.SAXException;

/**
 * This parses the instance state file and sets up the InstanceState singleton object for use by
 * various parts of the system
 */
class InstanceStateFileProcessor {
    private Document xmlDoc = null;
    private File stateFile;
    private HashMap<String, InstanceState> instanceStates;

    public InstanceStateFileProcessor(HashMap<String, InstanceState> st, File xmlFile)
            throws IOException {
        instanceStates = st;
        stateFile = xmlFile;
        parse();
    }

    private void parse() throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setIgnoringElementContentWhitespace(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            this.xmlDoc = builder.parse(stateFile);
            parseInstanceStateFile();
        } catch (SAXException se) {
            throw new IOException("Unable to parse instance state file", se);
        } catch (ParserConfigurationException pce) {
            throw new IOException("Unable to parse instance state file", pce);
        }
    }

    static public InstanceStateFileProcessor createNew(HashMap<String, InstanceState> st, File xmlFileObject)
            throws IOException {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(xmlFileObject));

            writer.write("<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"no\"?>");
            writer.newLine();
            writer.write("<instance-state version=\"1.0\">");
            writer.newLine();
            writer.write("<gms-enabled>false</gms-enabled>");
            writer.newLine();
            for (String s : st.keySet()) {
                writer.write("<instance name=\"" + s + "\" state=\""
                        + InstanceState.StateType.NO_RESPONSE.getDescription() + "\" />");
                writer.newLine();
            }
            writer.write("</instance-state>");
            writer.newLine();
            writer.flush();
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
        return new InstanceStateFileProcessor(st, xmlFileObject);
    }

    public void addNewServer(String name) throws Exception {
        String newState = InstanceState.StateType.NEVER_STARTED.getDescription();
        Node i = findNode(name);
        if (i != null) {
            // deal with the case where an entry for this server was already
            // in the list. This can happen the first time the file is created.
            // In this case, remove the old information.
            removeInstanceNode(name);
        }
        addInstanceNode(name, newState);
        writeDoc();
    }

    private void parseInstanceStateFile() {
        NodeList list = xmlDoc.getElementsByTagName("instance");
        if(list==null) {
            return;
        }
        for(int i=0; i<list.getLength(); i++) {
            parseInstanceElement(list.item(i));
        }
    }

    private void parseInstanceElement(Node n) {
        String name = null, state = null;
        NamedNodeMap attrs = n.getAttributes();
        if(attrs != null) {
            name = getNodeValue(attrs.getNamedItem("name"));
            state = getNodeValue(attrs.getNamedItem("state"));
        }
        if(name == null)
            return;
        InstanceState newInstanceState = null;
        if(state == null)
            newInstanceState = new InstanceState(InstanceState.StateType.NO_RESPONSE);
        else
            newInstanceState = new InstanceState(InstanceState.StateType.makeStateType(state));
        NodeList list = n.getChildNodes();
        if(list == null)
            return;
        for(int i=0; i<list.getLength(); i++) {
            //TODO : Why we need this ? check
            String t = list.item(i).getTextContent();
            if("\n".equals(t))
                continue;
            newInstanceState.addFailedCommands(t);
        }
        instanceStates.put(name, newInstanceState);
    }

    private String getNodeValue(Node x) {
        return (x==null) ? null : x.getNodeValue();
    }

    private void writeDoc() throws Exception {
        OutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(stateFile);
            TransformerFactory transformerfactory = TransformerFactory.newInstance();
            Transformer transformer = transformerfactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.ENCODING, "utf-8");
            //transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            DOMSource domSource = new DOMSource(this.xmlDoc);
            transformer.transform(domSource, new StreamResult(outputStream));
        } finally {
            if (outputStream != null) outputStream.close();
        }
    }

    public void updateState(String instanceName, String newState) throws Exception {
        Node instance = findNode(instanceName);
        if (instance == null) {
            addInstanceNode(instanceName, newState);
        } else {
            instance.getAttributes().getNamedItem("state").setNodeValue(newState);
        }
        writeDoc();
    }

    private Node findNode(String instanceName) {
        NodeList list = xmlDoc.getElementsByTagName("instance");
        if (list==null) {
            return null;
        }
        for (int i=0; i<list.getLength(); i++) {
            Node instance = list.item(i);
            NamedNodeMap attrs = instance.getAttributes();
            if (attrs == null)
                continue;
            String name = getNodeValue(attrs.getNamedItem("name"));
            if (instanceName.equals(name))
                return instance;
        }
        return null;
    }

    public void addFailedCommand(String instanceName, String failedCmd) throws Exception {
        Node instance = findNode(instanceName);
        if (instance == null) {
            instance = addInstanceNode(instanceName, InstanceState.StateType.NO_RESPONSE.getDescription());
        }
        Text tNode = xmlDoc.createTextNode(failedCmd);
        Element fcNode = xmlDoc.createElement("failed-command");
        fcNode.appendChild(tNode);
        instance.appendChild(fcNode);
        writeDoc();
    }

    public void removeFailedCommands(String instanceName) throws Exception {
        Node instance = findNode(instanceName);
        if (instance == null)
            return;
        NodeList clist = instance.getChildNodes();
        for(int j=0; j<clist.getLength(); j++) {
            instance.removeChild(clist.item(j));
        }
        writeDoc();
    }

    public void removeInstanceNode(String instanceName) throws Exception {
        Node instance = findNode(instanceName);
        if (instance == null)
            return;
        NodeList clist = instance.getChildNodes();
        for(int j=0; j<clist.getLength(); j++) {
            instance.removeChild(clist.item(j));
        }
        Node parent = instance.getParentNode();
        parent.removeChild(instance);
        writeDoc();
    }

    private Node addInstanceNode(String name, String state) {
        Node parentNode = xmlDoc.getElementsByTagName("instance-state").item(0);
        Element insNode = xmlDoc.createElement("instance");
        insNode.setAttribute("name", name);
        insNode.setAttribute("state", state);
        parentNode.appendChild(insNode);
        return insNode;
    }
}

/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2012 Oracle and/or its affiliates. All rights reserved.
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

// Portions Copyright [2019] [Payara Foundation and/or its affiliates]

package com.sun.enterprise.admin.report;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.jvnet.hk2.annotations.Service;

import org.glassfish.hk2.api.PerLookup;

/**
 * Represents the action report as XML like this:
 * <br>
 * <!-- 
 *     Apologies for the formatting - it's necessary for the JavaDoc to be readable 
 *     If you are using NetBeans, for example, click anywhere in this comment area to see
 *     the document example clearly in the JavaDoc preview
 * -->
 * <code> 
 * <br>&lt;action-report description="xxx" exit-code="xxx" [failure-cause="xxx"]>
 * <br>&nbsp;&nbsp;&lt;message-part message="xxx">
 * <br>&nbsp;&nbsp;&nbsp;&nbsp;&lt;property name="xxx" value="xxx"/>
 * <br>&nbsp;&nbsp;&nbsp;&nbsp;...
 * <br>&nbsp;&nbsp;&nbsp;&nbsp;&lt;message-part message="xxx" type="xxx">
 * <br>&nbsp;&nbsp;&nbsp;&nbsp;...
 * <br>&nbsp;&nbsp;&nbsp;&nbsp;&lt;/message-part>
 * <br>&nbsp;&nbsp;&lt/message-part>
 * <br>&nbsp;&nbsp;&lt;action-report ...> [for subactions]
 * <br>&nbsp;&nbsp;...
 * <br>&nbsp;&nbsp;&lt;/action-report>
 * <br>&lt;/action-report>
 * </code>
 * 
 * @author tjquinn
 */
@Service(name="xml")
@PerLookup
public class XMLActionReporter extends ActionReporter {

    @Override
    public void writeReport(OutputStream os)  {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document d = db.newDocument();

            d.appendChild(writeActionReport(d, this));
            writeXML(d, os);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
    }
    
    /**
     * Creates a new Element representing the XML content describing an
     * action report.  Invokes itself recursively to capture information
     * about any subactions.
     * @param owningDocument Document which will own all generated XML content
     * @param report the ActionReporter to convert to XML content
     * @return Element for the specified ActionReporter (and any sub-reports)
     */
    private Element writeActionReport(Document owningDocument, ActionReporter report) {
        Element result = owningDocument.createElement("action-report");
        result.setAttribute("description", report.actionDescription);
        result.setAttribute("exit-code", report.getActionExitCode().name());
        if (exception != null) {
            result.setAttribute("failure-cause", exception.getLocalizedMessage());
        }

        writePart(result, report.getTopMessagePart(), null);
        for (ActionReporter subReport : report.subActions) {
            result.appendChild(writeActionReport(owningDocument, subReport));
        }
        return result;
    }

    @Override
    public String getContentType() {
        return "text/xml"; 
    }
    
    private void writePart(Element actionReport, MessagePart part, String childType) {
        Document d = actionReport.getOwnerDocument();
        Element messagePart = d.createElement("message-part");
        actionReport.appendChild(messagePart);
        if (childType != null) {
            messagePart.setAttribute("type", childType);
        }
        
        for (Map.Entry prop : part.getProps().entrySet()) {
            Element p = d.createElement("property");
            messagePart.appendChild(p);
            p.setAttribute("name", prop.getKey().toString());
            Object value = prop.getValue();
            if (value instanceof List) {
                addListElement(p, (List)value);
            } else if (value instanceof Map) {
                addMapElement(p, (Map)value);
            } else {
                p.setAttribute("value", prop.getValue().toString());
            }
        }
        messagePart.setAttribute("message", part.getMessage());
        for (MessagePart subPart : part.getChildren()) {
            writePart(messagePart, subPart, subPart.getChildrenType());
        }
    }

    private void addListElement(Element parent, List list) {
        Document d = parent.getOwnerDocument();
        Element listElement = d.createElement("list");
        parent.appendChild(listElement);

        for (Object entry : list) {
            Element entryElement = d.createElement("entry");
            listElement.appendChild(entryElement);
            if (entry instanceof List) {
                addListElement(entryElement, (List) entry);
            } else if (entry instanceof Map) {
                addMapElement(entryElement, (Map) entry);
            } else {
                entryElement.setAttribute("value", entry.toString());
            }
        }
    }
    
    private void addMapElement(Element parent, Map map) {
        Document d = parent.getOwnerDocument();
        Element mapElement = d.createElement("map");
        parent.appendChild(mapElement);

        for (Map.Entry entry : (Set<Map.Entry>)map.entrySet()) {
            Element entryElement = d.createElement("entry");
            String key = entry.getKey().toString();
            Object value = entry.getValue();
            mapElement.appendChild(entryElement);
            entryElement.setAttribute("key", key);
            
            if (value instanceof List) {
                addListElement(entryElement, (List) value);
            } else if (value instanceof Map) {
                addMapElement(entryElement, (Map) value);
            } else {
                entryElement.setAttribute("value", value.toString());
            }
        }
    }

    private void writeXML(Document doc, OutputStream os) throws TransformerConfigurationException, TransformerException {
        Source source = new DOMSource(doc);

        Result result = new StreamResult(os);

        Transformer xformer = TransformerFactory.newInstance().newTransformer();
        xformer.transform(source, result);
    }
}

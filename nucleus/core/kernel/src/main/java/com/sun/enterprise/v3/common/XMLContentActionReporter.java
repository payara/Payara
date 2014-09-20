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

package com.sun.enterprise.v3.common;

import java.io.OutputStream;
import java.util.Map;
import javax.xml.parsers.*;
import javax.xml.transform.*;
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
 * <br>&nbsp;&nbsp;&lt;message-part message="">
 * <br>&nbsp;&nbsp;&nbsp;&nbsp;&lt;property name="xxx" value="xxx"/>
 * <br>&nbsp;&nbsp;&nbsp;&nbsp;...
 * <br>&nbsp;&nbsp;&nbsp;&nbsp;&lt;<i>child-type</i> <i>property</i>="value"/>
 * <br>&nbsp;&nbsp;&nbsp;&nbsp;...
 * <br>&nbsp;&nbsp;&nbsp;&nbsp;&lt;/<i>child-type</i>>
 * <br>&nbsp;&nbsp;&nbsp;&nbsp;&lt;/message-part>
 * <br>&nbsp;&nbsp;&lt/message-part>
 * <br>&lt;/action-report>
 * </code>
 *
 * Currently this is used to return the metadata for a command, although
 * it could be used more generally to return XML content.  In the general
 * case the action-report and message-part elements ought to be removed.
 * 
 * @author tjquinn
 * @author Bill Shannon
 */
@Service(name="metadata")   // XXX - need a better mapping
@PerLookup
public class XMLContentActionReporter extends ActionReporter {

    public void writeReport(OutputStream os)  {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();

            Document d = db.newDocument();

            d.appendChild(writeActionReport(d, this));
            writeXML(d, os);
        } catch (ParserConfigurationException pex) {
            throw new RuntimeException(pex);
        } catch (TransformerException e) {
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
    private Element writeActionReport(Document owningDocument,
	    ActionReporter report) {
        Element result = owningDocument.createElement("action-report");
        result.setAttribute("description", report.actionDescription);
        result.setAttribute("exit-code", report.getActionExitCode().name());
        if (exception != null) {
            result.setAttribute("failure-cause",
		exception.getLocalizedMessage());
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

    private void writePart(Element actionReport, MessagePart part,
	    String childType) {
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
            p.setAttribute("value", prop.getValue().toString());
        }
        messagePart.setAttribute("message", part.getMessage());

        for (MessagePart subPart : part.getChildren())
            writeSubPart(messagePart, subPart, subPart.getChildrenType());
    }

    /**
     * Write out all the sub-parts as XML elements where the
     * "childType" is the name of the XML element and the properties
     * are attributes of the element.  Recurse for any subparts.
     */
    private void writeSubPart(Element actionReport, MessagePart part,
	    String childType) {
        Document d = actionReport.getOwnerDocument();
        Element messagePart = d.createElement(childType);
        actionReport.appendChild(messagePart);

        for (Map.Entry prop : part.getProps().entrySet()) {
            messagePart.setAttribute(prop.getKey().toString(),
		prop.getValue().toString());
        }
        for (MessagePart subPart : part.getChildren())
            writeSubPart(messagePart, subPart, subPart.getChildrenType());
    }

    /**
     * Write the XML document to the output stream.
     *
     * @param doc   the XML document
     * @param os    the output stream
     * @throws TransformerException if anything goes wrong
     */
    private void writeXML(Document doc, OutputStream os)
	    throws TransformerException {
        Source source = new DOMSource(doc);

        Result result = new StreamResult(os);

        Transformer xformer = TransformerFactory.newInstance().newTransformer();
        xformer.transform(source, result);
    }
}

/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admin.rest.logviewer;

import java.io.StringWriter;
import java.util.Date;
import java.util.logging.Level;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.glassfish.admin.rest.RestLogging;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * internal REST wrapper for a log record
 * will be used to emit JSON easily with Jackson framework
 *
 * @author ludo
 */
public class LogRecord {

    long recordNumber;
    Date loggedDateTime;
    String loggedLevel;
    String productName;
    String loggerName;
    String nameValuePairs;
    String messageID;
    String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String Message) {
        this.message = Message;
    }

    public Date getLoggedDateTime() {
        return loggedDateTime;
    }

    public void setLoggedDateTime(Date loggedDateTime) {
        this.loggedDateTime = loggedDateTime;
    }

    public String getLoggedLevel() {
        return loggedLevel;
    }

    public void setLoggedLevel(String loggedLevel) {
        this.loggedLevel = loggedLevel;
    }

    public String getLoggerName() {
        return loggerName;
    }

    public void setLoggerName(String loggerName) {
        this.loggerName = loggerName;
    }

    public String getMessageID() {
        return messageID;
    }

    public void setMessageID(String messageID) {
        this.messageID = messageID;
    }

    public String getNameValuePairs() {
        return nameValuePairs;
    }

    public void setNameValuePairs(String nameValuePairs) {
        this.nameValuePairs = nameValuePairs;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public long getRecordNumber() {
        return recordNumber;
    }

    public void setRecordNumber(long recordNumber) {
        this.recordNumber = recordNumber;
    }

    public String toJSON() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("recordNumber", recordNumber);
            obj.put("loggedDateTimeInMS", (loggedDateTime != null) ? loggedDateTime.getTime() : null);
            obj.put("loggedLevel", loggedLevel);
            obj.put("productName", productName);
            obj.put("loggerName", loggerName);
            obj.put("nameValuePairs", nameValuePairs);
            obj.put("messageID", messageID);
            obj.put("Message", message); //.replaceAll("\n", Matcher.quoteReplacement("\\\n")).replaceAll("\"", Matcher.quoteReplacement("\\\"")));
        } catch (JSONException ex) {
            throw new RuntimeException(ex);
        }
        return obj.toString();
    }

    public String toXML() {

        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();

            Document d = db.newDocument();

            Element result = d.createElement("record");
            result.setAttribute("recordNumber", "" + recordNumber);
            result.setAttribute("loggedDateTimeInMS", (loggedDateTime != null) ? ("" + loggedDateTime.getTime()) : "");
            result.setAttribute("loggedLevel", loggedLevel);
            result.setAttribute("productName", productName);
            result.setAttribute("loggerName", loggerName);
            result.setAttribute("nameValuePairs", nameValuePairs);
            result.setAttribute("messageID", messageID);
            result.setNodeValue(message);
            d.appendChild(result);
            return xmlToString(d);

        } catch (ParserConfigurationException pex) {
            throw new RuntimeException(pex);
        }
    }

    private String xmlToString(Node node) {
        try {
            Source source = new DOMSource(node);
            StringWriter stringWriter = new StringWriter();
            Result result = new StreamResult(stringWriter);
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

            transformer.transform(source, result);
            return stringWriter.getBuffer().toString();
        } catch (TransformerConfigurationException e) {
            //  e.printStackTrace();
        } catch (TransformerException e) {
            //  e.printStackTrace();
        }
        return null;
    }
}

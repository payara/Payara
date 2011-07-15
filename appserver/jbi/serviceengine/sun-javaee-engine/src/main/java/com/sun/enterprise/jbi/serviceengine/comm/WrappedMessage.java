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

/*
 * WrappedMessage.java
 *
 * Created on November 20, 2006, 6:03 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.sun.enterprise.jbi.serviceengine.comm;

import com.sun.enterprise.jbi.serviceengine.core.JavaEEServiceEngineContext;
import com.sun.enterprise.jbi.serviceengine.util.DOMUtil;
import com.sun.enterprise.jbi.serviceengine.util.JBIConstants;
import com.sun.enterprise.jbi.serviceengine.util.soap.EndpointMetaData;
import com.sun.enterprise.jbi.serviceengine.util.soap.SOAPConstants;
import com.sun.logging.LogDomains;
import com.sun.xml.bind.api.Bridge;
import com.sun.xml.ws.api.message.AttachmentSet;
import com.sun.xml.ws.api.message.HeaderList;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.message.DataHandlerAttachment;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.wsdl.Part;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

/**
 *
 * @author bhavani
 */
public class WrappedMessage extends Message
        implements JBIConstants, SOAPConstants {
    
    private static Logger logger =
            LogDomains.getLogger(WrappedMessage.class, LogDomains.SERVER_LOGGER);
    
    private static DocumentBuilderFactory dbf;
    
    /**
     * Holds the contents of the wrapped message.
     */
    Source wrappedMessage;
    
    /**
     * Variables used for wrapping the message.
     */
    Message abstractMessage;
    QName wsdlMessageType;
    String wsdlBindingStyle;
    String wsdlMessageName;
    List<Part> wsdlOrderedParts;
    int[] wsdlPartBindings; // represents whether the part is bound to 'header', 'body', or 'attachment'.
    
    public void setAbstractMessage(Message abstractMessage) {
        this.abstractMessage = abstractMessage;
    }
    
    public void setWSDLMessageType(QName wsdlMessageType) {
        this.wsdlMessageType = wsdlMessageType;
    }
    
    public void setWSDLBindingStyle(String wsdlBindingStyle) {
        this.wsdlBindingStyle = wsdlBindingStyle;
    }
    
    public void setWSDLMessageName(String wsdlMessageName) {
        this.wsdlMessageName = wsdlMessageName;
    }
    
    public void setWSDLOrderedParts(List<Part> wsdlOrderedParts) {
        this.wsdlOrderedParts = wsdlOrderedParts;
    }
    
    public void setWSDLPartBindings(int[] wsdlPartBindings) {
        this.wsdlPartBindings = wsdlPartBindings;
    }
    
    private boolean log = false;
    
    private void setLog() {
        if(logger.isLoggable(Level.FINE)) {
            log = true;
        }
    }
    
    public WrappedMessage() {
        if(dbf == null) {
            dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
        }
        setLog();
    }
    
    public void wrap() throws Exception {
        
        if(JavaEEServiceEngineContext.getInstance().isServiceMix()) {
            if(log) {
                logger.log(Level.FINE, "Skipping the wrapping...");
            }
            wrappedMessage = abstractMessage.readPayloadAsSource();
        } else {
            if(RPC_STYLE.equalsIgnoreCase(wsdlBindingStyle)) {
                new RPCStyleWrapper().wrap();
            } else {
                new DocumentStyleWrapper().wrap();
            }
        }
    }
    
    private void writeJBIHeader(XMLStreamWriter writer) throws Exception {
        writer.writeStartDocument();
        writer.writeStartElement(WRAPPER_MESSAGE_QNAME);
        writeMessageAttributes(writer, wsdlMessageName);
    }
    
    private void writeJBIFooter(XMLStreamWriter writer) throws Exception {
        writer.writeEndElement();
        writer.writeEndDocument();
        writer.flush();
    }
    
    private void setMessage(byte[] data) throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        String usedWith = System.getProperty(USED_WITH);
        if(usedWith != null && usedWith.indexOf(USED_WITH_HTTP_SOAP_BC) != -1) {
            DocumentBuilder db = dbf.newDocumentBuilder();
            wrappedMessage = new DOMSource(db.parse(bais));
        } else {
            wrappedMessage = new StreamSource(bais);
        }
    }
    
    class DocumentStyleWrapper {
        void wrap() {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                XMLStreamWriter writer = XOF.createXMLStreamWriter(baos, "UTF-8");
                
                writeJBIHeader(writer);
                
                for(int i=0, headerIndex=0; i<wsdlPartBindings.length; i++) {
                    switch(wsdlPartBindings[i]) {
                        case SOAP_BODY_BINDING :
                            if(log) {
                                logger.log(Level.FINE, "Adding part number " + i + " from payLoad");
                            }
                            if(abstractMessage.hasPayload()) {
                                writer.writeStartElement(WRAPPER_PART_QNAME); // assuming only one part in the response.
                                writer.writeCharacters(""); // Force completion of open elems
                                writePayloadTo(writer);
                                writer.writeEndElement();
                            }
                            break;
                        case SOAP_HEADER_BINDING :
                            if(log) {
                                logger.log(Level.FINE, "Adding part number " + i + " from header");
                            }
                            writer.writeStartElement(WRAPPER_PART_QNAME); // assuming only one part in the response.
                            writer.writeCharacters(""); // Force completion of open elems
                            writer.flush();
                            abstractMessage.getHeaders().get(headerIndex++).writeTo(writer);
                            writer.writeEndElement();
                            break;
                        case SOAP_ATTACHMENT_BINDING :
                            if(log) {
                                logger.log(Level.FINE, "Part number " + i + " is an attachment");
                            }
                            break;
                    }
                }
                
                writeJBIFooter(writer);
                
                setMessage(baos.toByteArray());
                
                if(log) {
                    logger.log(Level.FINE, "\n\nWrapped message = " + baos.toString() + "\n\n");
                }
            } catch(Exception ex) {
                logger.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
        
    }
    
    class RPCStyleWrapper {
        
        void wrap() throws Exception {
            
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                XMLStreamWriter writer = XOF.createXMLStreamWriter(baos, "UTF-8");
                
                writeJBIHeader(writer);
                
                XMLStreamReader payLoad = abstractMessage.readPayload();
                if(payLoad.getEventType() == XMLStreamReader.START_DOCUMENT) {
                    payLoad.next();
                }
                Map<String,String> payLoadNSAttrs = new HashMap<String,String>();
                copyNamespaceAttributes(payLoad, payLoadNSAttrs);
                payLoad.next(); // skip the operation node.
                
                copyNamespaceAttributes(payLoad, payLoadNSAttrs); // CR 6652680
                if(log) {
                    logger.log(Level.FINE, "Payload namespace attributes = " + payLoadNSAttrs);
                }
                writeJBIParts(payLoad, writer, payLoadNSAttrs);
                
                writeJBIFooter(writer);
                
                setMessage(baos.toByteArray());
                
                if(log) {
                    logger.log(Level.FINE, "\n\nWrapped message = " + baos.toString() + "\n\n");
                }
            } catch(Exception ex) {
                logger.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
        
        private void copyNamespaceAttributes(
                XMLStreamReader reader,
                Map<String,String> m) throws Exception {
            for(int i=0; i<reader.getNamespaceCount(); i++) {
                m.put(reader.getNamespacePrefix(i),
                        reader.getNamespaceURI(i));
            }
        }
        
        private void writeJBIParts(
                XMLStreamReader payLoad,
                XMLStreamWriter writer,
                Map<String,String> ancestorNamespaceAttrs) throws Exception {
            if(log) {
                logger.log(Level.FINE, "Number of parts = " + wsdlPartBindings.length);
            }
            for(int partIndex = 0, headerIndex = 0
                    ; partIndex < wsdlPartBindings.length && payLoad.hasNext()
                    ; partIndex++) {
                switch(wsdlPartBindings[partIndex]) {
                    case SOAP_HEADER_BINDING :
                        if(log) {
                            logger.log(Level.FINE, "Adding part number " + partIndex + " from header");
                        }
                        writer.writeStartElement(WRAPPER_PART_QNAME); // assuming only one part in the response.
                        writer.writeCharacters(""); // Force completion of open elems
                        writer.flush();
                        abstractMessage.getHeaders().get(headerIndex++).writeTo(writer);
                        writer.writeEndElement();
                        break;
                        
                    case SOAP_BODY_BINDING :
                        if(log) {
                            logger.log(Level.FINE, "Adding part number " + partIndex + " from payLoad");
                        }
                        writer.writeStartElement(WRAPPER_PART_QNAME);
                        DOMUtil.UTIL.writeChildren(payLoad, writer, ancestorNamespaceAttrs);
                        writer.writeEndElement();
                        //payLoad.next();
                        break;
                    case SOAP_ATTACHMENT_BINDING :
                        if(log) {
                            logger.log(Level.FINE, "Part number " + partIndex + " is an attachment");
                        }
                        break;
                }
            }
        }
        
        private boolean isSimpleType(Part part) {
            QName parttype = part.getTypeName();
            if (parttype != null) {
                String s = parttype.getNamespaceURI();
                if ( s != null && s.trim().equals("http://www.w3.org/2001/XMLSchema")) {
                    return true;
                }
            }
            return false;
        }
        
    }
    
    private void writeMessageAttributes(XMLStreamWriter sw,
            String wsdlMessageName) throws XMLStreamException {
        String prefix = wsdlMessageType.getPrefix();
        if (prefix == null || prefix.trim().length() == 0) {
            prefix = "msgns";
        }
        sw.writeAttribute(WRAPPER_ATTRIBUTE_VERSION, WRAPPER_ATTRIBUTE_VERSION_VALUE);
        if(wsdlMessageName != null) {
            sw.writeAttribute(WRAPPER_ATTRIBUTE_NAME, wsdlMessageName);
        }
        sw.writeAttribute(WRAPPER_ATTRIBUTE_TYPE, prefix + ":" + wsdlMessageType.getLocalPart());
        sw.writeAttribute("xmlns:" + prefix, wsdlMessageType.getNamespaceURI());
        sw.writeAttribute("xmlns:" + WRAPPER_DEFAULT_NAMESPACE_PREFIX, WRAPPER_DEFAULT_NAMESPACE);
    }
    
    public Source readPayloadAsSource() {
        return wrappedMessage;
    }
    
    public boolean isFault() {
        return abstractMessage.isFault();
    }
    
    public String getPayloadLocalPart() {
        return abstractMessage.getPayloadLocalPart();
    }
    
    public String getPayloadNamespaceURI() {
        return abstractMessage.getPayloadNamespaceURI();
    }
    
    public XMLStreamReader readPayload() throws XMLStreamException {
        return abstractMessage.readPayload();
    }
    
    public boolean hasPayload() {
        return abstractMessage.hasPayload();
    }
    
    public boolean hasHeaders() {
        return abstractMessage.hasHeaders();
    }
    
    public HeaderList getHeaders() {
        return abstractMessage.getHeaders();
    }
    
    public Message copy() {
        return null;
    }
    
    public Source readEnvelopeAsSource() {
        return abstractMessage.readEnvelopeAsSource();
    }
    
    public SOAPMessage readAsSOAPMessage() throws SOAPException {
        return abstractMessage.readAsSOAPMessage();
    }
    
    public <T> T readPayloadAsJAXB(Unmarshaller unmarshaller) throws JAXBException {
        return (T) abstractMessage.readPayloadAsJAXB(unmarshaller);
    }
    
    public <T> T readPayloadAsJAXB(Bridge<T> bridge) throws JAXBException {
        return abstractMessage.readPayloadAsJAXB(bridge);
    }
    
    public void writePayloadTo(XMLStreamWriter sw) throws XMLStreamException {
        try {
            sw.flush();
            abstractMessage.writePayloadTo(sw);
        } catch(Exception ex) {
            ex.printStackTrace();
            throw new XMLStreamException(ex);
        }
    }
    
    public void writeTo(ContentHandler contentHandler, ErrorHandler errorHandler) throws SAXException {
        abstractMessage.writeTo(contentHandler, errorHandler);
    }
    
    /**
     * Writes the wrapped abstractMessage into the stream writer.
     *
     * The fault is not wrapped to compensate a bug in HTTP SOAP BC.
     * It is assumed that the normal response abstractMessage has only one part.
     */
    public void writeTo(XMLStreamWriter sw) throws XMLStreamException {
        writePayloadTo(sw);
    }

    private Node getNodeValue(Document d, String localName) {
        NodeList nl = d.getElementsByTagNameNS("", localName);
        Node n = (nl != null && nl.getLength() > 0)
        ? nl.item(0).getFirstChild()
        : null;
        return n;
    }
    
    public void wrapFault(String operationName, EndpointMetaData emd) {
        
        if(JavaEEServiceEngineContext.getInstance().isServiceMix()) {
            if(log) {
                logger.log(Level.FINE, "Skipping the wrapping...");
            }
            wrappedMessage = abstractMessage.readPayloadAsSource();
            return;
        }
        
        DOMResult result = new DOMResult();
        try {
            Transformer t = TF.newTransformer();
            t.transform(abstractMessage.readPayloadAsSource(), result);
        } catch(Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
        Document d = (Document)result.getNode();
        
        if(log) {
            logger.log(Level.FINE, "Fault received from JAX-WS : " +
                    UnWrappedMessage.toString(d));
        }
        
        Node faultDetail = getNodeValue(d, FAULT_DETAIL_ELEMENT);
        
        /*
         * Caused by RuntimeException thrown from JAX-WS.
         */
        if(faultDetail == null) {
            logger.log(Level.WARNING, 
                    "RuntimeException thrown from the JAX-WS. No <detail> found.");
            RuntimeException rt_ex = new RuntimeException(
                    "RuntimeException thrown from the JAX-WS. No details found.");
            // don't send service engine stack trace.
            rt_ex.setStackTrace(new StackTraceElement[]{}); 
            throw rt_ex;
        }
        
        javax.wsdl.Message wsdlMsg = null;
        try {
            wsdlMsg = emd.getFaultMessage(operationName,
                    faultDetail.getLocalName(), faultDetail.getNamespaceURI());
        } catch(Exception ex) {
            logger.log(Level.WARNING, ex.getMessage(), ex);
        }

        /*
         * Caused by RuntimeException thrown from the Application.
         */
        if(wsdlMsg == null) {
            logger.log(Level.WARNING, "RuntimeException thrown from the " +
                    "Application. Fault is not defined in the WSDL");
            
            Node n = getNodeValue(d, FAULT_STRING_ELEMENT);
            Node m = getNodeValue(d, FAULT_MESSAGE_ELEMENT);
            String exMsg = (m != null) // if there is exception message, we can use that.
                    ? m.getTextContent()
                    : n.getTextContent();
            RuntimeException rt_ex = new RuntimeException(exMsg);
            // don't send service engine stack trace.
            rt_ex.setStackTrace(new StackTraceElement[]{}); 
            throw rt_ex;
        }
        
        /*
         * Caused by the Checked exception thrown from the Application.
         */
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            XMLStreamWriter writer = XOF.createXMLStreamWriter(baos, "UTF-8");
            writer.writeStartDocument();
            String wsdlMessageName = faultDetail.getLocalName();
            this.wsdlMessageType = wsdlMsg.getQName();
            writer.writeStartElement(WRAPPER_MESSAGE_QNAME);
            writeMessageAttributes(writer, wsdlMessageName);
            writer.writeStartElement(WRAPPER_PART_QNAME);
            writer.writeCharacters(""); // Force completion of open elems
            DOMUtil.UTIL.writeNode(faultDetail, writer);
            writer.writeEndElement();
            writer.writeEndElement();
            writer.writeEndDocument();
            writer.flush();
            setMessage(baos.toByteArray());
            
            if(log) {
                logger.log(Level.FINE, "\n\nWrapped fault = " + 
                        baos.toString() + "\n\n");
            }
        } catch(Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }
    
    /*
    void setMessage(byte[] data) throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        DocumentBuilder db = dbf.newDocumentBuilder();
        wrappedMessage = new DOMSource(db.parse(bais));
    }
     */
}

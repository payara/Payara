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

package com.sun.enterprise.jbi.serviceengine.comm;

import com.sun.enterprise.jbi.serviceengine.core.JavaEEServiceEngineContext;
import com.sun.enterprise.jbi.serviceengine.util.DOMUtil;
import com.sun.enterprise.jbi.serviceengine.util.JBIConstants;
import com.sun.enterprise.jbi.serviceengine.util.DOMStreamReader;
import com.sun.enterprise.jbi.serviceengine.util.soap.SOAPConstants;
import com.sun.enterprise.jbi.serviceengine.util.soap.StringTranslator;
import com.sun.logging.LogDomains;
import com.sun.xml.bind.api.Bridge;
import com.sun.xml.ws.api.message.HeaderList;
import com.sun.xml.ws.api.message.Message;
import com.sun.enterprise.jbi.serviceengine.util.StAXSource;
import com.sun.xml.ws.message.DOMHeader;
import com.sun.xml.ws.message.DataHandlerAttachment;
import com.sun.xml.ws.message.stream.StreamHeader11;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jbi.messaging.NormalizedMessage;
import javax.wsdl.Part;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.InputSource;

/**
 *
 * This class is used to unwrap the incoming JBI request message when the
 * Java EE Service Engine is the Provider.
 *
 * This class unwraps and extracts the 'payload' from the incomoming
 * request message which is in normalized form.
 *
 * The current implentation supports the following WSDL1.1 SOAP binding styles :
 *
 *      1. Wrapped Document/literal
 *      2. RPC/Literal
 *
 * The characterstics of the Wrapped Document/literal style are
 *
 *  1. The input message has a single part.
 *  2. The part is an element.
 *  3. The element has the same name as the operation.
 *  4. The element's complex type has no attributes.
 *
 * For wrapped document/literal, the incoming request message looks like :
 *
 * <?xml version="1.0" encoding="UTF-8"?>
 * <jbi:message
 *          xmlns:msgns="http://example.web.service/Calculator"
 *          name="add"
 *          type="msgns:add"
 *          version="1.0"
 *          xmlns:jbi="http://java.sun.com/xml/ns/jbi/wsdl-11-wrapper">
 *	<jbi:part>
 *		<ns2:add xmlns:ns2="http://example.web.service/Calculator">
 *			<arg0>1.0</arg0>
 *			<arg1>2.0</arg1>
 *		</ns2:add>
 *	</jbi:part>
 * </jbi:message>
 *
 * and the payLoad node for this is :
 *
 * <ns2:add xmlns:ns2="http://example.web.service/Calculator">
 *      <arg0>1.0</arg0>
 *      <arg1>2.0</arg1>
 * </ns2:add>
 *
 * For RPC/literal, the incoming request message looks like this :
 *
 * <?xml version="1.0" encoding="UTF-8"?>
 * <jbi:message
 *          xmlns:msgns="http://example.web.service/Calculator"
 *          name="add"
 *          type="msgns:add"
 *          version="1.0"
 *          xmlns:jbi="http://java.sun.com/xml/ns/jbi/wsdl-11-wrapper">
 *	<jbi:part><int_1>1</int_1></jbi:part>
 *	<jbi:part><int_2>2</int_2></jbi:part>
 * </jbi:message>
 *
 * and the payLoad node for this is:
 *
 * <ns2:add xmlns:ns2="http://example.web.service/Calculator">
 *      <int_1>1</int_1>
 *      <int_2>2</int_2>
 * </ns2:add>
 *
 * The styles which are not supported by JAX-WS 2.0 are:
 *      RPC/encoded
 *      Document/encoded
 *
 * The style(s) which are treated/converted as wrapped document/literal by JAX-WS tools are
 *      document/literal
 *
 * @author bhavanishankar@dev.java.net
 */

public final class UnWrappedMessage extends Message
        implements JBIConstants, SOAPConstants {
    
    private static Logger logger =
            LogDomains.getLogger(UnWrappedMessage.class, LogDomains.SERVER_LOGGER);
    
    private String payloadLocalName;
    private String payloadNamespaceURI;
    private Source payLoadAsSource;
    private XMLStreamReader payLoadAsStreamReader;
    private ByteArrayOutputStream payLoadAsBaos;
    private HeaderList headers = new HeaderList();
    private boolean isFault;
    private boolean log = false;
    
    private void setLog() {
        if(logger.isLoggable(Level.FINE)) {
            log = true;
        }
    }
    
    public UnWrappedMessage() {
        setLog();
    }
    
    /**
     * Variables used for unwrapping the message.
     */
    Source nmContent;
    NormalizedMessage normalizedMessage;
    
    QName wsdlMessageType;
    String wsdlBindingStyle;
    List<Part> wsdlOrderedParts;
    int[] wsdlPartBindings; // represents whether the part is bound to 'header', 'body', or 'attachment'.
    
    public void setNormalizedMessage(NormalizedMessage normalizedMessage) {
        this.normalizedMessage = normalizedMessage;
        this.nmContent = normalizedMessage.getContent();
    }
    
    public void setWSDLMessageType(QName wsdlMessageType) {
        this.wsdlMessageType = wsdlMessageType;
    }
    
    public void setWSDLBindingStyle(String wsdlBindingStyle) {
        this.wsdlBindingStyle = wsdlBindingStyle;
    }
    
    public void setWSDLOrderedParts(List<Part> wsdlOrderedParts) {
        this.wsdlOrderedParts = wsdlOrderedParts;
    }
    
    public void setWSDLPartBindings(int[] wsdlPartBindings) {
        this.wsdlPartBindings = wsdlPartBindings;
    }
    
    public void unwrap() throws Exception {
        
        if(log) {
            String s = (nmContent instanceof DOMSource) ? toString(nmContent) : "StreamSource";
            logger.log(Level.FINE, "bindingStyle = " + wsdlBindingStyle + ", received message = " + s);
        }
        
        if(JavaEEServiceEngineContext.getInstance().isServiceMix()) {
            if(log) {
                logger.log(Level.FINE, "Skipping the unwrapping...");
            }
            setPayLoad(nmContent);
        } else {
            if(RPC_STYLE.equalsIgnoreCase(wsdlBindingStyle)) {
                new RPCStyleUnWrapper().unwrap();
            } else {
                new DocumentStyleUnWrapper().unwrap();
            }
        }
        
        processAttachments();
        
        if(payLoadAsStreamReader == null) {
            throw new Exception(StringTranslator.getDefaultInstance().getString(
                    "serviceengine.unwrapping_failed"));
        }
        
    }
    
    public void unwrapFault() throws Exception {
        
        if(log) {
            String s = (nmContent instanceof DOMSource) ? toString(nmContent) : "StreamSource";
            logger.log(Level.FINE, "bindingStyle = " + wsdlBindingStyle + ", received fault = " + s);
        }
        
        isFault = true;
        if(JavaEEServiceEngineContext.getInstance().isServiceMix()) {
            if(log) {
                logger.log(Level.FINE, "Skipping the unwrapping...");
            }
            setPayLoad(nmContent);
        } else {
            unWrapFault();
        }
        
        if(payLoadAsStreamReader == null) {
            throw new Exception(StringTranslator.getDefaultInstance().getString(
                    "serviceengine.unwrapping_failed"));
        }
    }
    
    public boolean isFault() {
        return isFault;
    }
    
    public String getPayloadLocalPart() {
        return payloadLocalName;
    }
    
    public String getPayloadNamespaceURI() {
        return payloadNamespaceURI;
    }
    
    public boolean hasPayload() {
        return payLoadAsStreamReader == null;
    }
    
    public boolean hasHeaders() {
        return (headers.size() != 0);
    }
    
    public HeaderList getHeaders() {
        if(log) {
            logger.log(Level.FINE, "Headers = " + headers);
        }
        return headers;
    }
    
    public Message copy() {
        return null;
    }
    
    public Source readEnvelopeAsSource() {
        return null;
    }
    
    public XMLStreamReader readPayload() throws XMLStreamException {
        if(log) {
            logger.log(Level.FINE, "UnWrappedMessage :: readPayLoad()");
        }
        return payLoadAsStreamReader;
    }
    
    public Source readPayloadAsSource() {
        if(log) {
            logger.log(Level.FINE, "UnWrappedMessage :: readPayLoadAsSource()");
        }
        return payLoadAsSource;
    }
    
    public <T> T readPayloadAsJAXB(Unmarshaller unmarshaller) throws JAXBException {
        if(System.getSecurityManager() == null) {
            return (T)unmarshaller.unmarshal(payLoadAsSource);
        } else {
            try {
                final Unmarshaller funmarshaller = unmarshaller;
                final Source fpayLoad = payLoadAsSource;
                return (T)  java.security.AccessController.doPrivileged
                (new java.security.PrivilegedExceptionAction() {
                public java.lang.Object run() throws JAXBException{
                    return funmarshaller.unmarshal(fpayLoad);
                }});
            } catch (java.security.PrivilegedActionException e) {
                throw (JAXBException) e.getException();
            }
        }
    }
    
    public <T> T readPayloadAsJAXB(Bridge<T> bridge) throws JAXBException {
        if(System.getSecurityManager() == null) {
            return bridge.unmarshal(payLoadAsSource);
        } else {
            try {
                final Bridge<T> fbridge = bridge;
                final Source fpayLoad = payLoadAsSource;
                                              
                return (T)  java.security.AccessController.doPrivileged
                (new java.security.PrivilegedExceptionAction() {
                public java.lang.Object run() throws JAXBException{
                    return fbridge.unmarshal(fpayLoad);
                }});
            } catch (java.security.PrivilegedActionException e) {
                throw (JAXBException) e.getException();
            }
        }
    }
    
    public void writePayloadTo(XMLStreamWriter sw) throws XMLStreamException {
        throw new XMLStreamException("Operaion is not supported.");
    }
    
    public void writeTo(XMLStreamWriter sw) throws XMLStreamException {
        throw new XMLStreamException("Operaion is not supported.");
    }
    
    public void writeTo(ContentHandler contentHandler, ErrorHandler errorHandler) throws SAXException {
        throw new SAXException("Operaion is not supported.");
    }
    
    public SOAPMessage readAsSOAPMessage() throws SOAPException {
        String methodSig =
                "\ncom.sun.enterprise.jbi.serviceengine.comm.UnWrappedMessage" +
                "::readAsSOAPMessage()";
        String usedWith = System.getProperty(USED_WITH);
        if(usedWith == null || usedWith.indexOf(USED_WITH_JMAC_PROVIDER) == -1) {
            throw new SOAPException(
                    methodSig + " operation is not supported." +
                    "\nSet this system property to workaround this issue : " +
                    "com.sun.enterprise.jbi.se.usedwith=jmacprovider");
        }
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            XMLStreamWriter writer = XOF.createXMLStreamWriter(baos, "UTF-8");
            writer.writeStartDocument();
            writer.writeStartElement(SOAP_ENVELOPE);
            writer.writeAttribute("xmlns:" + SOAP_PREFIX, SOAP_NAMESPACE);
            writer.writeEmptyElement(SOAP_HEADER);
            writer.writeStartElement(SOAP_BODY);
            if(payLoadAsBaos == null) {
                if(payLoadAsSource instanceof DOMSource) {
                    Node payLoadNode = ((DOMSource)payLoadAsSource).getNode();
                    DOMUtil.UTIL.writeNode(payLoadNode, writer);
                } else {
                    DOMUtil.UTIL.writeNode(payLoadAsStreamReader, writer);
                }
            } else {
                baos.write(">".getBytes());
                baos.write(payLoadAsBaos.toByteArray());
            }
            writer.writeEndElement();
            writer.writeEndElement();
            writer.writeEndDocument();
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            
            SOAPMessage message = MessageFactory.newInstance().createMessage(null, bais);
            if(log) {
                logger.log(Level.FINE, methodSig + " :: SOAPMessage = " + toString(message));
            }
            return message;
        } catch(Exception ex) {
            throw new SOAPException(methodSig + ex.getMessage());
        }
    }
    
    
    public void setPayLoad(Source s) throws Exception {
        if(s instanceof DOMSource) {
            setPayLoad(((DOMSource)s).getNode());
        } else if(s instanceof StreamSource) {
            XMLStreamReader reader = XIF.
                    createXMLStreamReader(((StreamSource)s).getInputStream());
            setPayLoad(reader);
        }  else if(s instanceof SAXSource) {
            InputSource source = ((SAXSource) s).getInputSource();
            XMLStreamReader reader = (source.getCharacterStream() != null)?
                    XIF.createXMLStreamReader(source.getCharacterStream()):
                    XIF.createXMLStreamReader(source.getByteStream());
            setPayLoad(reader);
        } else {
            logger.log(Level.WARNING, "UnWrappedMessage :: Transforming the input message to DOM");
            Transformer t = TF.newTransformer();
            DOMResult result = new DOMResult();
            t.transform(s, result);
            setPayLoad(result.getNode());
        }
    }
    
    public void setPayLoad(Node n) {
        if(n.getNodeType() == Node.DOCUMENT_NODE) {
            n = n.getFirstChild();
        }
        payloadLocalName = n.getLocalName();
        payloadNamespaceURI = n.getNamespaceURI();
        payLoadAsSource = new DOMSource(n);
        payLoadAsStreamReader = new DOMStreamReader(n);
        printPayLoad("");
    }
    
    public void setPayLoad(XMLStreamReader reader) throws Exception {
        if(reader.getEventType() == XMLStreamReader.START_DOCUMENT) {
            reader.next();
        }
        payloadLocalName =  reader.getLocalName();
        payloadNamespaceURI = reader.getNamespaceURI();
        payLoadAsSource = new StAXSource(reader, true); // StAXSource will be available in JDK6.
        payLoadAsStreamReader = reader;
        XMLStreamReader r = reader;
        printPayLoad("");
    }
    
    public void copyPayLoad(XMLStreamReader r) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XMLStreamWriter writer = XOF.createXMLStreamWriter(baos, "UTF-8");
        DOMUtil.UTIL.writeNode(r, writer);
        writer.flush();
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        XMLStreamReader reader = XIF.createXMLStreamReader(bais);
        if(log) {
            logger.log(Level.FINE, "Payload = " + baos.toString());
        }
        setPayLoad(reader);
    }
    
    // Methods for debugging purposes.
    
    public static String toString(SOAPMessage soapMessage) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            soapMessage.writeTo(baos);
            return baos.toString();
        } catch(Exception e) {
            return e.getMessage();
        }
    }
    
    
    public static String toString(Node n) {
        return toString(new DOMSource(n));
    }
    
    public static String toString(XMLStreamReader reader) {
        return toString(new StAXSource(reader, true));
    }
    
    public static String toString(Source s) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            StreamResult sr = new StreamResult(baos);
            TF.newTransformer().transform(s, sr);
            return baos.toString();
        } catch(Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
    
    public StreamSource getStreamSource(Source src) {
        try {
            if(src instanceof StreamSource) {
                return (StreamSource)src;
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            StreamResult result = new StreamResult(baos);
            TF.newTransformer().transform(src, result);
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            StreamSource ss = new StreamSource(bais);
            return ss;
        } catch(Exception ex) {
            ex.printStackTrace();
            return null;
        }
        
    }
    
    public void printPayLoad(String message) {
        if(!log) {
            return;
        }
        StringBuffer msg = new StringBuffer("\n\n");
        if(payLoadAsSource instanceof DOMSource) {
            Node n = ((DOMSource)payLoadAsSource).getNode();
            msg.append("Unwrapped message " + UnWrappedMessage.toString(n));
        }
        msg
                .append(message)
                .append("\n\npayLoadLocalName = ")
                .append(payloadLocalName)
                .append("\npayLoadNamespaceURI = ")
                .append(payloadNamespaceURI)
                .append("\n\n");
        
        logger.log(Level.FINE, msg.toString());
    }
    
    /**
     * Adds the header node to the header list.
     */
    private void addHeader(Node n) {
        if(log) {
            logger.log(Level.FINE, "Header = " + toString(n));
        }
        DOMHeader header = new DOMHeader((Element)n);
        headers.add(header);
    }
    
    /**
     * Adds the header node pointed by XMLStreamReader to the header list.
     */
    private void addHeader(XMLStreamReader r) {
        try {
            if(log) {
                logger.log(Level.FINE, "Header = " + r.getName());
            }
            StreamHeader11 header = new StreamHeader11(r);
            headers.add(header);
        } catch(Exception ex) {
            logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }
    
    private void processAttachments() {
        for(String aName : (Set<String>)normalizedMessage.getAttachmentNames()) {
            logger.log(Level.FINE, "Adding attachment with ID = " + aName);
            getAttachments().add(new DataHandlerAttachment(
                    aName, normalizedMessage.getAttachment(aName)));
        }
    }
    
    
// END :: Methods for debugging purposes.
    
    class DocumentStyleUnWrapper {
        
        void unwrap() throws Exception {
            if(nmContent instanceof DOMSource) { // most cases it will be DOMSource
                Node n = ((DOMSource)nmContent).getNode();
                unwrap(getOwnerDocument(n));
            } else if(nmContent instanceof StreamSource) {
                XMLStreamReader reader = XIF.
                        createXMLStreamReader(((StreamSource)nmContent).getInputStream());
                unwrap(reader);
            } else if(nmContent instanceof SAXSource) {
                InputSource source = ((SAXSource) nmContent).getInputSource();
                XMLStreamReader reader = (source.getCharacterStream() != null)?
                                XIF.createXMLStreamReader(source.getCharacterStream()):
                                XIF.createXMLStreamReader(source.getByteStream());
                unwrap(reader);
            } else {
                logger.log(Level.WARNING, "UnWrappedMessage :: Transforming the input message to DOM");
                Transformer t = TF.newTransformer();
                DOMResult result = new DOMResult();
                t.transform(nmContent, result);
                unwrap(getOwnerDocument(result.getNode()));
            }
        }
        
        /**
         * Searches for the payLoad node in the given node and makes
         * payLoadAsSource and payLoadAsStreamReader point to the 'payload' node.
         *
         * If the payLode node is not found in the given node, then payLoadAsSource
         * and payLoadAsStreamReader will remain null.
         *
         * @param n node representing a JBI inbound request message
         * @param operation operation to be invoked on the endpoint
         */
        private void unwrap(Node n) {
            try {
                if(n.getNodeType() == Node.DOCUMENT_NODE) {
                    n = n.getFirstChild();
                }
                if(isJBINode(n, WRAPPER_MESSAGE_LOCALNAME)) {
                    NodeList nl = n.getChildNodes();
                    for(int i=0, partIndex=0; i<wsdlPartBindings.length; i++, partIndex++) {
                        Node partValue = nl.item(partIndex).getFirstChild();
                        switch (wsdlPartBindings[i]) {
                            case SOAP_BODY_BINDING :
                                if(log) {
                                    logger.log(Level.FINE, "Setting part number " + i + " as payload");
                                }
                                setPayLoad(partValue);
                                break;
                            case SOAP_HEADER_BINDING :
                                if(log) {
                                    logger.log(Level.FINE, "Adding part number " + i + " to header");
                                }
                                addHeader(partValue);
                                break;
                            case SOAP_ATTACHMENT_BINDING :
                                if(log) {
                                    logger.log(Level.FINE, "Part number " + i + " is an attachment");
                                }
                                --partIndex; // Stay on the same node.
                                break;
                        }
                    }
                } else {
                    logger.log(Level.SEVERE, "Received message is not <jbi:message>");
                }
            } catch(Exception ex) {
                logger.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
        
        /**
         * Searches for the payLoad in the given reader and makes
         * payLoadAsSource and payLoadAsStreamReader point to the 'payload' node.
         *
         * If the payLode is not found in the given reader, then payLoadAsSource
         * and payLoadAsStreamReader will remain null.
         *
         * @param reader reader pointing to a JBI inbound request message
         * @param operation operation to be invoked on the endpoint
         */
        private void unwrap(XMLStreamReader reader) {
            String methodSig = "UnWrappedMessage$DocumentStyleUnwrapper :: " +
                    "unwrap(XMLStreamReader, QName) : ";
            try {
                if(!findJBINode(reader,WRAPPER_MESSAGE_LOCALNAME)) {
                    logger.log(Level.SEVERE, "Received message is not <jbi:message>");
                    return;
                }
                for(int i=0; i<wsdlPartBindings.length; i++) {
                    switch (wsdlPartBindings[i]) {
                        case SOAP_BODY_BINDING :
                            
                            if(!findJBINode(reader, WRAPPER_PART_LOCALNAME)) {
                                logger.log(Level.SEVERE, "Required <jbi:part> is not found in the received message");
                                continue;
                            }
                            reader.next(); // point reader to <jbi:part>'s value
                            
                            if(wsdlPartBindings.length == 1) {
                                if(log) {
                                    logger.log(Level.FINE, "Setting part number " + i + " as payload");
                                }
                                setPayLoad(reader);
                            } else {
                                if(log) {
                                    logger.log(Level.FINE, "Copying part number " + i + " to payload");
                                }
                                copyPayLoad(reader);
                            }
                            break;
                        case SOAP_HEADER_BINDING :
                            if(!findJBINode(reader, WRAPPER_PART_LOCALNAME)) {
                                logger.log(Level.SEVERE, "Required <jbi:part> is not found in the received message");
                                continue;
                            }
                            reader.next(); // point reader to <jbi:part>'s value
                            
                            if(log) {
                                logger.log(Level.FINE, "Adding part number " + i + " to header");
                            }
                            addHeader(reader);
                            break;
                        case SOAP_ATTACHMENT_BINDING :
                            if(log) {
                                logger.log(Level.FINE, "Part number " + i + " is an attachment");
                            }
                            break;
                    }
                }
            } catch(Exception ex) {
                logger.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
    }
    
    class RPCStyleUnWrapper {
        
        void unwrap() throws Exception {
            if(nmContent instanceof DOMSource) { // most cases it will be DOMSource
                Node n = ((DOMSource)nmContent).getNode();
                unwrap(getOwnerDocument(n));
            } else if(nmContent instanceof StreamSource) {
                XMLStreamReader reader = XIF.
                        createXMLStreamReader(((StreamSource)nmContent).getInputStream());
                unwrap(reader);
            } else if(nmContent instanceof SAXSource) {
                InputSource source = ((SAXSource) nmContent).getInputSource();
                XMLStreamReader reader = (source.getCharacterStream() != null)?
                                XIF.createXMLStreamReader(source.getCharacterStream()):
                                XIF.createXMLStreamReader(source.getByteStream());
                unwrap(reader);
            } else {
                logger.log(Level.WARNING, "UnWrappedMessage :: Transforming the input message to DOM");
                Transformer t = TF.newTransformer();
                DOMResult result = new DOMResult();
                t.transform(nmContent, result);
                unwrap(getOwnerDocument(result.getNode()));
            }
        }
        
        /**
         *
         * Unwraps the incoming JBI request message and sets
         * payLoadAsSource and payLoadAsStreamReader to hold the 'payload'.
         *
         * @param wrappedDocument XMLStreamReader or Node representing the JBI
         * request message which is in the following form:
         *
         * <jbi:message
         *          xmlns:msgns="http://example.web.service/Calculator"
         *          name="add"
         *          type="msgns:add"
         *          version="1.0"
         *          xmlns:jbi="http://java.sun.com/xml/ns/jbi/wsdl-11-wrapper">
         * 	<jbi:part><int_1>1</int_1></jbi:part>
         * 	<jbi:part><int_2>2</int_2></jbi:part>
         * </jbi:message>
         * @param payLoadName Name of the operation to be invoked on the endpoint
         * @param payLoadNsUri Target name space URI which is unique for a webservice.
         * @return Sets payLoadAsSource and payLoadAsStreamReader to the payload which is
         *
         * <ns2:add xmlns:ns2="http://example.web.service/Calculator">
         *      <int_1>1</int_1>
         *      <int_2>2</int_2>
         * </ns2:add>
         */
        private void unwrap(Object wrappedDocument) {
            String methodSig = "UnWrappedMessage$RPCStyleUnwrapper :: " +
                    "unwrap(Object, String, String, ServiceEngineEndpoint) : ";
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                XMLStreamWriter writer = XOF.createXMLStreamWriter(baos, "UTF-8");
                /**
                 * Extract the payLoad content from wrappedDocument and write it
                 * to writer. This does not involve creating or importing a DOM node.
                 */
                writer.writeStartElement(DEFAULT_OPERATION_PREFIX + ":" + wsdlMessageType.getLocalPart());
                writer.writeAttribute(DEFAULT_XML_NS_SCHEME + ":" + DEFAULT_OPERATION_PREFIX,
                        wsdlMessageType.getNamespaceURI());
                if(wrappedDocument instanceof Node) {
                    writeJBIParts((Node)wrappedDocument, writer);
                } else if(wrappedDocument instanceof XMLStreamReader) {
                    writeJBIParts((XMLStreamReader)wrappedDocument, writer);
                }
                writer.writeEndElement();
                writer.flush();
                /**
                 * Create payLoadAsStreamReader and payLoadAsSource from the
                 * content available in ByteArrayOutputStream.
                 */
                ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
                payLoadAsBaos = baos;
                payloadLocalName = wsdlMessageType.getLocalPart();
                payloadNamespaceURI = wsdlMessageType.getNamespaceURI();
                payLoadAsStreamReader = XIF.createXMLStreamReader(bais);
                payLoadAsStreamReader.next(); // skip the <?xml ...?> node.
                payLoadAsSource = new StAXSource(payLoadAsStreamReader, true);
                
                printPayLoad("Unwrapped message = " + baos.toString());
            } catch(Exception ex) {
                logger.log(Level.SEVERE, ex.getMessage(), ex);
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
        
        private void writeJBIParts(XMLStreamReader reader,
                XMLStreamWriter writer) throws Exception {
            long startTime = System.currentTimeMillis();
            /**
             * The following loop is expected to iterate only once always,
             * because there could be only one <jbi:message> in the request.
             */
            
            if(!findJBINode(reader, WRAPPER_MESSAGE_LOCALNAME)) {
                logger.log(Level.SEVERE, "Received message is not <jbi:message>");
                return;
            }
            
            for(int k=0; k<wsdlPartBindings.length; k++) {
                switch(wsdlPartBindings[k]) {
                    case SOAP_HEADER_BINDING :
                        if(!findJBINode(reader, WRAPPER_PART_LOCALNAME)) {
                            logger.log(Level.SEVERE, "Required <jbi:part> is not found in the received message");
                            continue;
                        }
                        /**
                         * <jbi:part>'s value will always be an element, so
                         * point reader to <jbi:part>'s value.
                         */
                        reader.next();
                        if(log) {
                            logger.log(Level.FINE, "Adding part number " + k + " to header");
                        }
                        addHeader(reader);
                        break;
                    case SOAP_BODY_BINDING :
                        if(!findJBINode(reader, WRAPPER_PART_LOCALNAME)) {
                            logger.log(Level.SEVERE, "Required <jbi:part> is not found in the received message");
                            continue;
                        }
                        if(log) {
                            logger.log(Level.FINE, "Adding part number " + k + " to payload");
                        }
                        Part part = wsdlOrderedParts.get(k);
                        writer.writeStartElement(part.getName());
                        DOMUtil.UTIL.writeChildren(reader, writer);
                        writer.writeEndElement();
                        break;
                    case SOAP_ATTACHMENT_BINDING :
                        if(log) {
                            logger.log(Level.FINE, "Part number " + k + " is an attachment");
                        }
                        break;
                }
            }
            
            long timeTaken = System.currentTimeMillis() - startTime;
            if(log) {
                logger.log(Level.FINE, "TimeTaken to write JBI parts to payload = " + timeTaken);
            }
        }
        
        private void writeJBIParts(Node wrappedDocument,
                XMLStreamWriter writer) throws Exception {
            NodeList jbiMessages = wrappedDocument.getChildNodes();
            /**
             * The following loop is expected to iterate only once always,
             * because there could be only one <jbi:message> in the request.
             */
            long startTime = System.currentTimeMillis();
            for(int i=0; i<jbiMessages.getLength(); i++) {
                Node jbiMessage = jbiMessages.item(i);
                if(!isJBINode(jbiMessage, WRAPPER_MESSAGE_LOCALNAME)) continue;
                NodeList jbiParts = jbiMessage.getChildNodes();
                /**
                 * The following loop is expected to iterate as much as
                 * there are <jbi:part> elements.
                 */
                for(int j = 0, k = 0; j< jbiParts.getLength(); j++) {
                    Node jbiPart = jbiParts.item(j);
                    if(!isJBINode(jbiPart, WRAPPER_PART_LOCALNAME)) continue;
                    
                    switch(wsdlPartBindings[k]) {
                        
                        case SOAP_HEADER_BINDING :
                            /**
                             * <jbi:part>'s value will always be an element, so
                             * add <jbi:part>'s value as header.
                             */
                            if(log) {
                                logger.log(Level.FINE, "Adding part number " + k + " as header");
                            }
                            addHeader(jbiPart.getFirstChild());
                            break;
                            
                        case SOAP_BODY_BINDING :
                            if(log) {
                                logger.log(Level.FINE, "Adding part number " + k + " to payload");
                            }
                            Part part = wsdlOrderedParts.get(k);
                            writer.writeStartElement(part.getName());
                            DOMUtil.UTIL.writeChildren(writer, jbiPart);
                            writer.writeEndElement();
                            break;
                        case SOAP_ATTACHMENT_BINDING :
                            if(log) {
                                logger.log(Level.FINE, "Part number " + k + " is an attachment");
                            }
                            --j; // stay on the same node.
                            break;
                    }
                    ++k;
                }
            }
            long timeTaken = System.currentTimeMillis() - startTime;
            if(log) {
                logger.log(Level.FINE, "TimeTaken to write JBI parts to payload = " + timeTaken);
            }
        }
        
    }
    
    private boolean findJBINode(XMLStreamReader reader, String localName) throws Exception {
        while(reader.hasNext()) {
            if(isJBINode(reader,localName)) {
                return true;
            }
            reader.next();
        }
        return false;
    }
    
    private boolean isJBINode(Node node, String localName) {
        if(WRAPPER_DEFAULT_NAMESPACE.equalsIgnoreCase(node.getNamespaceURI()) &&
                localName.equalsIgnoreCase(node.getLocalName())) {
            return true;
        }
        return false;
    }
    
    private boolean isJBINode(XMLStreamReader reader, String localName) {
        if(reader.getEventType() != XMLStreamReader.START_ELEMENT) {
            return false;
        }
        if(WRAPPER_DEFAULT_NAMESPACE.equalsIgnoreCase(reader.getNamespaceURI()) &&
                localName.equalsIgnoreCase(reader.getLocalName())) {
            return true;
        }
        return false;
    }
    
    public Node getOwnerDocument(Node n) {
        Node ownerDocument = n.getOwnerDocument();
        return ownerDocument != null
                ? ownerDocument
                : n;
    }
    
    private void unWrapFault() throws Exception {
        Source s = this.nmContent;
        Node n;
        if(s instanceof DOMSource) {
            n = ((DOMSource)s).getNode();
        } else {
            Transformer t = TF.newTransformer();
            DOMResult result = new DOMResult();
            t.transform(s, result);
            n = result.getNode();
        }
        
        Document d = (n instanceof Document) ? (Document) n : n.getOwnerDocument();
        
        NodeList nl = d.getElementsByTagNameNS(WRAPPER_DEFAULT_NAMESPACE, WRAPPER_PART_LOCALNAME);
        Node jbiPart = (nl != null && nl.getLength() > 0)
        ? nl.item(0).getFirstChild()
        : d.getFirstChild();
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XMLStreamWriter writer = XOF.createXMLStreamWriter(baos);
        
        writer.writeStartDocument();
        writer.writeStartElement(SOAP_FAULT);
        writer.writeAttribute("xmlns:" + SOAP_PREFIX, SOAP_NAMESPACE);
        
        String faultCode = jbiPart == null ? SERVER_FAULT_CODE : CLIENT_FAULT_CODE;
        String faultString = FAULT_STRING;
        
        writer.writeStartElement(FAULT_CODE_ELEMENT);
        writer.writeCharacters(SOAP_PREFIX + ":" + faultCode);
        writer.writeEndElement();
        
        writer.writeStartElement(FAULT_STRING_ELEMENT);
        writer.writeCharacters(faultString);
        writer.writeEndElement();
        
        writer.writeStartElement(FAULT_DETAIL_ELEMENT);
        if(jbiPart != null)
            DOMUtil.UTIL.writeNode(jbiPart, writer);
        
        writer.writeEndElement();
        writer.writeEndElement();
        writer.writeEndDocument();
        writer.flush();
        
        setPayLoad(baos.toByteArray());
        
        printPayLoad("\n\nUnwrapped fault = " + baos.toString());
    }
    
    public void setPayLoad(byte[] data) throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        payLoadAsStreamReader = XIF.createXMLStreamReader(bais);
        payLoadAsStreamReader.next(); // skip the <?xml ...?> node.
        payLoadAsSource = new StAXSource(payLoadAsStreamReader, true);
        payloadLocalName = payLoadAsStreamReader.getLocalName();
        payloadNamespaceURI = payLoadAsStreamReader.getNamespaceURI();
    }
    
}

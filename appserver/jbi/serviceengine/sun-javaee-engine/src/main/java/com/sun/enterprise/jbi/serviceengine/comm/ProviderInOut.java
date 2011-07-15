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
 * InOutMEPHandler.java
 *
 * Created on November 16, 2006, 12:16 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.sun.enterprise.jbi.serviceengine.comm;

import com.sun.enterprise.jbi.serviceengine.util.soap.EndpointMetaData;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.Packet;
import java.util.logging.Level;
import javax.jbi.messaging.Fault;
import javax.jbi.messaging.InOut;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.namespace.QName;

/**
 *
 * This class handles sending message to & receiving message from NMR when
 *
 *  1) Role of the component is Provider.
 *  2) Message exchange pattern is In-Out.
 *
 * @author bhavanishankar@dev.java.net
 */
public class ProviderInOut extends MessageExchangeTransportImpl {
    
    private InOut me;
    
    public ProviderInOut(InOut me) {
        super(me);
        this.me = me;
    }
    
    public NormalizedMessage receiveNormalized() {
        msg = me.getInMessage();
        preReceive();
        return msg;
    }
    
    public UnWrappedMessage receive(EndpointMetaData emd) {
        NormalizedMessage normalizedMessage = receiveNormalized();
        UnWrappedMessage unwrappedMessage = null;
        if(normalizedMessage != null) {
            try {
                
                String operationName = me.getOperation().getLocalPart();
                
                unwrappedMessage = new UnWrappedMessage();
                unwrappedMessage.setNormalizedMessage(normalizedMessage);
                
                if(normalizedMessage instanceof Fault) {
                    unwrappedMessage.unwrapFault();
                } else {
                    unwrappedMessage.setWSDLMessageType(
                            new QName(
                            emd.getInputMessage(operationName).getQName().getNamespaceURI(),
                            operationName));
                    
                    unwrappedMessage.setWSDLBindingStyle(
                            emd.getBindingStyle(operationName));
                    
                    unwrappedMessage.setWSDLOrderedParts(
                            emd.getInputMessage(operationName).getOrderedParts(null));
                    
                    unwrappedMessage.setWSDLPartBindings(
                            emd.getInputPartBindings(operationName));
                    
                    unwrappedMessage.unwrap();
                }
            } catch(Exception ex) {
                logger.log(Level.SEVERE, ex.getMessage(), ex);
                // TODO : Should we send error/fault to client???
            }
        }
        return unwrappedMessage;
    }
    
    public void sendNormalized() throws Exception {
        if(msg instanceof Fault) {
            me.setFault((Fault)msg);
        } else {
            me.setOutMessage(msg);
        }
        send();
    }
    
    public void send(Packet packet, EndpointMetaData emd) throws Exception {
        Message message = packet.getMessage();
        String operationName = me.getOperation().getLocalPart();
        WrappedMessage wrappedMessage = new WrappedMessage();
        wrappedMessage.setAbstractMessage(message);
        
        if(message.isFault()) {
            wrappedMessage.wrapFault(operationName, emd);
            this.msg = me.createFault();
        } else {
            wrappedMessage.setWSDLBindingStyle(emd.getBindingStyle(operationName));
            wrappedMessage.setWSDLMessageType(emd.getOutputMessage(operationName).getQName());
            wrappedMessage.setWSDLMessageName(emd.getOperationOutputName(operationName));
            wrappedMessage.setWSDLOrderedParts(emd.getOutputMessage(operationName).getOrderedParts(null));
            wrappedMessage.setWSDLPartBindings(emd.getOutputPartBindings(operationName));
            wrappedMessage.wrap();
            this.msg = me.createMessage();
        }
        msg.setContent(wrappedMessage.readPayloadAsSource());
        setMessageProperties(packet.invocationProperties);
        sendNormalized();
    }
    
    /*
     
    public void send(ByteArrayOutputStream output, int status) {
        if(output == null) {
            sendError((String)null);
        } else if(status == SOAPConstants.JBI_ERROR) {
            sendError(output.toString());
        } else {
            send(createMessage(output, status));
        }
    }
     
    private NormalizedMessage createMessage(ByteArrayOutputStream output, int status) {
        try {
            byte[] message = ((ByteArrayOutputStream)output).toByteArray();
            if(logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Response message = " + new String(message));
            }
            NormalizedMessage response = null;
            if(status == SOAPConstants.JBI_FAULT) {
                ByteArrayInputStream is = new ByteArrayInputStream(message);
                response = me.createFault();
                Document n = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
                response.setContent(new DOMSource(n));
                try {
                    String faultString = n.getElementsByTagName("faultstring").item(0).getTextContent();
                    String faultCode = n.getElementsByTagName("faultcode").item(0).getTextContent();
                    if(logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE, "FaultCode = " + faultCode + ", FaultString = " + faultString);
                    }
                    response.setProperty(SOAPConstants.FAULT_STRING_PROPERTY_NAME, faultString);
                    response.setProperty(SOAPConstants.FAULT_CODE_PROPERTY_NAME, extractFaultCode(faultCode));
                } catch(Exception ex) {
                    logger.log(Level.SEVERE, ex.getMessage(), ex);
                }
            } else {
                ByteArrayInputStream is = new ByteArrayInputStream(message);
                response = me.createMessage();
                response.setContent(new StreamSource(is));
            }
            return response;
        } catch(Exception ex) {
            return null;
        }
    }
     */
    
}

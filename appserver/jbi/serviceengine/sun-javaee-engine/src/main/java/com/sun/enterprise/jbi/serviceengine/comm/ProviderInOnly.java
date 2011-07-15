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
 * InOnlyMEPHandler.java
 *
 * Created on November 16, 2006, 12:18 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.sun.enterprise.jbi.serviceengine.comm;

import com.sun.enterprise.jbi.serviceengine.util.soap.EndpointMetaData;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.Packet;
import java.util.logging.Level;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.Fault;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

/**
 *
 * This class handles sending message to & receiving message from NMR when
 *
 *  1) Role of the component is Provider.
 *  2) Message exchange pattern is In-Only.
 *
 * @author bhavanishankar@dev.java.net
 */

public class ProviderInOnly extends MessageExchangeTransportImpl  {
    
    protected InOnly me;
    
    public ProviderInOnly(InOnly me) {
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
                logger.log(Level.INFO, ex.getMessage());
            }
        }
        return unwrappedMessage;
    }
    
    public void sendNormalized() throws Exception {
        me.setStatus(ExchangeStatus.DONE);
        send();
    }
    
    public void send(Packet packet, EndpointMetaData emd) throws Exception {
        Message abstractMessage = packet.getMessage();
        if(abstractMessage != null && abstractMessage.isFault()) {
            Exception exObj = null;
            try {
                String operationName = me.getOperation().getLocalPart();
                WrappedMessage wrappedMessage = new WrappedMessage();
                wrappedMessage.setAbstractMessage(abstractMessage);
                wrappedMessage.wrapFault(operationName, emd);
                /**
                 * wrapFault(..) will throw RuntimeException, if the RuntimeException 
                 * was thrown either by the Application or by the JAX-WS layer.
                 * 
                 * Since this is InOnly MEP, we should only send the status.
                 * So there is no need to send the wrapped fault.
                 */
            } catch (RuntimeException e) {
                exObj = e;
            }
            sendError(exObj);
        } else {
            sendNormalized();
        }
    }
    
}

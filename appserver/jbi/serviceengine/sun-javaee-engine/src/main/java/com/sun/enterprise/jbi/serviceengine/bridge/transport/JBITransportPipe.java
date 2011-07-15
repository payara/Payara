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

package com.sun.enterprise.jbi.serviceengine.bridge.transport;

import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.pipe.PipeCloner;
import com.sun.xml.ws.api.pipe.helper.AbstractPipeImpl;

import javax.xml.namespace.QName;
import java.net.URL;

/**
 * This is the main Pipe that is used by JAX-WS client runtime to sendRequest the 
 * request to the service and return the response back to the client.
 *  
 * 
 * @author Vikas Awasthi
 */
public class JBITransportPipe extends AbstractPipeImpl {

    //private Codec codec;
    private URL wsdlLocation;
    private QName service;
    private WSDLPort wsdlPort;

    public JBITransportPipe(WSBinding binding, 
                            URL wsdlLocation, 
                            QName service, 
                            WSDLPort wsdlPort) {
        /*
        StreamSOAPCodec xmlEnvCodec =
            Codecs.createSOAPEnvelopeXmlCodec(binding.getSOAPVersion());
        codec = Codecs.createSOAPBindingCodec(binding, xmlEnvCodec);
         */
        setWSDLLocation(wsdlLocation);
        setWSDLPort(wsdlPort);
        setServiceName(service);
    }

    private JBITransportPipe(JBITransportPipe that, PipeCloner cloner) {
        super(that, cloner);
        if(that != null) {
            setWSDLLocation(that.getWSDLLocation());
            setWSDLPort(that.getWSDLPort());
            setServiceName(that.getServiceName());
        }
    }

    public Packet process(Packet request) {
        try {
            // TODO get the oneway flag from the message
//            Boolean isOneway = request.getMessage().isOneWay(wSDLPort);
            boolean isOneWay = !request.expectReply;
            
            QName operation = request.getMessage().getOperation(wsdlPort).getName();
            String endpointName = wsdlPort.getName().getLocalPart();
            NMRClientConnection con =
                    new NMRClientConnection(wsdlLocation, service, endpointName, operation, isOneWay);

            con.initialize();
            con.sendRequest(request);

            Packet reply = null;
            if(System.getSecurityManager() == null) {
                reply = con.receiveResponse(request);
            } else {
                final NMRClientConnection fcon = con;
                final Packet frequest = request;

                reply = (Packet)  java.security.AccessController.doPrivileged
                (new java.security.PrivilegedAction() {
                public java.lang.Object run() {
                    return fcon.receiveResponse(frequest);
                }});

            }
            if(!isOneWay) {
                con.sendStatus();
            }
            return reply;
        } catch(Exception wex) {
            RuntimeException ex = new RuntimeException(wex.getMessage());
            ex.setStackTrace(wex.getStackTrace());
            throw ex;
        } 
    }

    public Pipe copy(PipeCloner cloner) {
        return new JBITransportPipe(this, cloner);
    }

    private void setWSDLLocation(URL wsdlLocation) {
        this.wsdlLocation = wsdlLocation;
    }

    private void setWSDLPort(WSDLPort wsdlPort) {
        this.wsdlPort = wsdlPort;
    }

    private void setServiceName(QName service) {
        this.service = service;
    }

    public URL getWSDLLocation() {
        return this.wsdlLocation;
    }

    public WSDLPort getWSDLPort() {
        return this.wsdlPort;
    }

    public QName getServiceName() {
        return this.service;
    }
}

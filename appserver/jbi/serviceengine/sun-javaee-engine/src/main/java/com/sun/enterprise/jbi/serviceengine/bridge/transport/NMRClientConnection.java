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

import com.sun.enterprise.jbi.serviceengine.comm.MessageExchangeTransport;
import com.sun.enterprise.jbi.serviceengine.comm.MessageExchangeTransportFactory;
import com.sun.enterprise.jbi.serviceengine.comm.UnWrappedMessage;
import com.sun.enterprise.jbi.serviceengine.core.JavaEEServiceEngineContext;
import com.sun.enterprise.jbi.serviceengine.core.DescriptorEndpointInfo;
import com.sun.enterprise.jbi.serviceengine.core.EndpointRegistry;
import com.sun.enterprise.jbi.serviceengine.util.JBIConstants;
import com.sun.enterprise.jbi.serviceengine.util.soap.EndpointMetaData;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.Packet;
import javax.jbi.component.ComponentContext;
import javax.jbi.messaging.ExchangeStatus;
import javax.xml.namespace.QName;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessageExchangeFactory;

/**
 *
 * @author Manisha Umbarje
 */
public class NMRClientConnection {
    
    //private QName svcQName;
    private boolean oneWay;
    private URL wsdlLocation;
    private QName service;
    private String endpointName;
    
    private QName operation;
    private MessageExchange me = null;
    private MessageExchangeTransport meTransport;
    
    private static ConcurrentHashMap<String, EndpointMetaData> emdCache =
            new ConcurrentHashMap<String, EndpointMetaData>(11,0.75f,4);
    
    public NMRClientConnection(URL wsdlLocation,
            QName service,
            String endpointName,
            QName operation,
            boolean oneWay) {
        this.wsdlLocation = wsdlLocation;
        this.service = service;
        this.endpointName = endpointName;
        this.operation = operation;
        setOneWay(oneWay);
    }
    
    public void setOneWay(boolean oneWay) {
        this.oneWay = oneWay;
    }
    
    public void initialize() {
        try {
            DeliveryChannel channel =
                    JavaEEServiceEngineContext.getInstance().getDeliveryChannel();
            String key = DescriptorEndpointInfo.getDEIKey(service, endpointName);
            DescriptorEndpointInfo dei = EndpointRegistry.getInstance().getJBIEndpts().get(key);
            QName service = (dei == null)? this.service:dei.getServiceName();
            String endpointName = (dei == null)? this.endpointName:dei.getEndpointName();

            // Create MessageExchange
            MessageExchangeFactory factory =
                    channel.createExchangeFactoryForService(service);
            me =  oneWay ? factory.createInOnlyExchange() : factory.createInOutExchange();
            me.setService(service);
            ComponentContext context = JavaEEServiceEngineContext.getInstance().getJBIContext();
            me.setEndpoint(context.getEndpoint(service, endpointName));
            me.setOperation(operation);
            meTransport = MessageExchangeTransportFactory.getHandler(me);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }
    
    /**
     * Get the EndpointMetaData from the cache if running in production clientCache.
     * Otherwise, return a new EndpointMetaData.
     */
    private EndpointMetaData getEndpointMetaData() {
        String clientCache = System.getProperty(JBIConstants.CLIENT_CACHE);
        if("false".equalsIgnoreCase(clientCache)) {
            return createEndpointMetaData();
        } else {
            String key = wsdlLocation.toString();
            EndpointMetaData emd = emdCache.get(key);
            if(emd == null) {
                emd = createEndpointMetaData();
                emdCache.put(key, emd);
            }
            return emd;
        }
    }
    
    private EndpointMetaData createEndpointMetaData() {
        EndpointMetaData emd = new EndpointMetaData(wsdlLocation, service, endpointName);
        emd.resolve();
        return emd;
    }
    
    public void sendRequest(Packet packet) throws Exception {
        meTransport.send(packet, getEndpointMetaData());
    }

    /**
     * @param request packet is used only for construction of response packet.
     * @return response packet.
     */
    public Packet receiveResponse(Packet request) {
        Message respMsg = meTransport.receive(getEndpointMetaData());
        Packet reply = request.createResponse(respMsg);
        if(reply.invocationProperties != null) {
            //TODO :: should we first clear the invocationProperties inherited from request packet? 
            reply.invocationProperties.putAll(meTransport.getMessageProperties());
        }
        return reply;
    }
    
    public void sendStatus() {
        if(me.getStatus().equals(ExchangeStatus.ACTIVE)) {
            meTransport.sendStatus(ExchangeStatus.DONE);
        }
    }
    
}

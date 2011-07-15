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

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPPart;
// import javax.xml.rpc.BindingProvider;
// import javax.xml.rpc.JAXRPCContext;

import javax.jbi.component.ComponentContext;
import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.messaging.InOut;
import javax.jbi.messaging.MessageExchangeFactory;
import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.servicedesc.ServiceEndpoint;

import com.sun.xml.rpc.client.ClientTransport;
import com.sun.xml.rpc.soap.message.SOAPMessageContext;

//import com.sun.enterprise.jbi.component.MessageFormatHandler;
import com.sun.enterprise.deployment.ServiceRefPortInfo;
import com.sun.enterprise.jbi.serviceengine.core.JavaEEServiceEngineContext;
import com.sun.enterprise.jbi.serviceengine.util.soap.MessageExchangeHelper;

/**
 *
 * @author Manisha Umbarje
 */
public class JAXRPCClientTransport implements ClientTransport {
    
    private DeliveryChannel channel;
    //private QName svcQName;
    private ServiceRefPortInfo portInfo;
   
    public JAXRPCClientTransport() {
        channel = JavaEEServiceEngineContext.getInstance().getDeliveryChannel();
    }

    /*public void setServiceQName(QName q) {
	svcQName = q;
    }*/


    public void invoke(String endpoint, SOAPMessageContext soapMsgContext) {

	try {
            
            MessageExchangeHelper meHelper = new MessageExchangeHelper();
            meHelper.initializeMessageExchange(portInfo,false);
            meHelper.normalizeMessage(soapMsgContext.getMessage(), true);
            meHelper.dispatchMessage();
            SOAPMessage responseMessage = meHelper.denormalizeMessage(false);
            soapMsgContext.setMessage(responseMessage);
	}
	catch ( Exception e ) {
	    throw new TransportFailedException(e.getMessage());
	}
	
    }
    
    public void invokeOneWay(String endpoint, SOAPMessageContext context) {
        invoke(endpoint, context);
    }
    
    public void setServicePortInfo(ServiceRefPortInfo portInfo) {
        this.portInfo = portInfo;
    }
}

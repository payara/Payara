/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.security.webservices;

import com.sun.enterprise.deployment.ServiceRefPortInfo;
import com.sun.enterprise.deployment.ServiceReferenceDescriptor;
import com.sun.enterprise.deployment.runtime.common.MessageSecurityBindingDescriptor;
import com.sun.enterprise.security.jauth.AuthParam;
import com.sun.enterprise.security.jmac.WebServicesDelegate;
import com.sun.enterprise.security.jmac.config.ConfigHelper.AuthConfigRegistrationWrapper;
import com.sun.enterprise.security.jmac.provider.PacketMessageInfo;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.model.wsdl.WSDLBoundOperation;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.enterprise.security.jmac.provider.SOAPAuthParam;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.auth.message.MessageInfo;
import javax.xml.namespace.QName;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.Name;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import org.glassfish.api.invocation.ComponentInvocation;

import org.jvnet.hk2.annotations.Service;
import javax.inject.Singleton;

/**
 *
 * @author kumar.jayanti
 */
@Service
@Singleton
public class WebServicesDelegateImpl implements WebServicesDelegate {

    protected static final Logger _logger = LogUtils.getLogger();

    private static final String DEFAULT_WEBSERVICES_PROVIDER=
            "com.sun.xml.wss.provider.wsit.WSITAuthConfigProvider";
    
     public MessageSecurityBindingDescriptor getBinding(ServiceReferenceDescriptor svcRef, Map properties) {
        MessageSecurityBindingDescriptor binding = null;
        WSDLPort p = (WSDLPort) properties.get("WSDL_MODEL");
        QName portName = null;
        if (p != null) {
            portName = p.getName();
        }
        if (portName != null) {
            ServiceRefPortInfo i = svcRef.getPortInfoByPort(portName);
            if (i != null) {
                binding = i.getMessageSecurityBinding();
            }
        }
        return binding;
    }

    public void removeListener(AuthConfigRegistrationWrapper listener) {
        //TODO:V3 convert the pipes to Tubes.
        ClientPipeCloser.getInstance().removeListenerWrapper(listener);
    }

    public String getDefaultWebServicesProvider() {
        return DEFAULT_WEBSERVICES_PROVIDER;
    }

    public String getAuthContextID(MessageInfo messageInfo) {

        // make this more efficient by operating on packet 
        String rvalue = null;
        if (messageInfo instanceof PacketMessageInfo) {
            PacketMessageInfo pmi = (PacketMessageInfo) messageInfo;
            Packet p = (Packet) pmi.getRequestPacket();
            if (p != null) {
                Message m = p.getMessage();
                if (m != null) {
                    WSDLPort port =
                            (WSDLPort) messageInfo.getMap().get("WSDL_MODEL");
                    if (port != null) {
                        WSDLBoundOperation w = m.getOperation(port);
                        if (w != null) {
                            QName n = w.getName();
                            if (n != null) {
                                rvalue = n.getLocalPart();
                            }
                        }
                    }
                }
            }
            return rvalue;
        } else {
            // make this more efficient by operating on packet 
            return getOpName((SOAPMessage) messageInfo.getRequestMessage());
        }

    }

    public AuthParam newSOAPAuthParam(MessageInfo messageInfo) {
        return new SOAPAuthParam((SOAPMessage)
                                  messageInfo.getRequestMessage(),
			         (SOAPMessage)
			          messageInfo.getResponseMessage());

    }

    private String getOpName(SOAPMessage message) {
        if (message == null) {
            return null;
        }

        String rvalue = null;

        // first look for a SOAPAction header. 
        // this is what .net uses to identify the operation

        MimeHeaders headers = message.getMimeHeaders();
        if (headers != null) {
            String[] actions = headers.getHeader("SOAPAction");
            if (actions != null && actions.length > 0) {
                rvalue = actions[0];
                if (rvalue != null && rvalue.equals("\"\"")) {
                    rvalue = null;
                }
            }
        }

        // if that doesn't work then we default to trying the name
        // of the first child element of the SOAP envelope.

        if (rvalue == null) {
            Name name = getName(message);
            if (name != null) {
                rvalue = name.getLocalName();
            }
        }

        return rvalue;
    }

    private Name getName(SOAPMessage message) {
        Name rvalue = null;
        SOAPPart soap = message.getSOAPPart();
        if (soap != null) {
            try {
                SOAPEnvelope envelope = soap.getEnvelope();
                if (envelope != null) {
                    SOAPBody body = envelope.getBody();
                    if (body != null) {
                        Iterator it = body.getChildElements();
                        while (it.hasNext()) {
                            Object o = it.next();
                            if (o instanceof SOAPElement) {
                                rvalue = ((SOAPElement) o).getElementName();
                                break;
                            }
                        }
                    }
                }
            } catch (SOAPException se) {
                if (_logger.isLoggable(Level.FINE)) {
                    _logger.log(Level.FINE, "WSS: Unable to get SOAP envelope",
                            se);
                }
            }
        }
        return rvalue;
    }

    public Object getSOAPMessage(ComponentInvocation inv) {
        /*V3 commented getting this from EJBPolicyContextDelegate instead
         * currently getting this from EjbPolicyContextDelegate which might be OK
        SOAPMessage soapMessage = null;
	    MessageContext msgContext = inv.messageContext;

            if (msgContext != null) {
                if (msgContext instanceof SOAPMessageContext) {
		    SOAPMessageContext smc =
                            (SOAPMessageContext) msgContext;
		    soapMessage = smc.getMessage();
                }
	    } else {
                soapMessage = inv.getSOAPMessage();
            }

	    return soapMessage;*/
        return null; 
    }
	
}

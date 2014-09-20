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


import com.sun.xml.ws.api.pipe.TubeCloner;
import com.sun.xml.ws.api.pipe.helper.AbstractTubeImpl;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.message.config.*;
import javax.security.auth.message.AuthStatus;
import javax.xml.ws.WebServiceException;

import com.sun.enterprise.security.jmac.provider.PacketMessageInfo;
import com.sun.enterprise.security.jmac.provider.PacketMapMessageInfo;
import com.sun.enterprise.security.jmac.provider.config.PipeHelper;
import com.sun.enterprise.util.LocalStringManagerImpl;

import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.message.Message;

import com.sun.xml.ws.api.pipe.NextAction;
import com.sun.xml.ws.api.pipe.Tube;
import com.sun.xml.ws.api.pipe.helper.AbstractFilterTubeImpl;
import com.sun.xml.ws.security.secconv.SecureConversationInitiator;
import com.sun.xml.ws.security.secconv.WSSecureConversationException;
import javax.xml.bind.JAXBElement;

/**
 * This pipe is used to do client side security for app server
 */
public class ClientSecurityTube extends AbstractFilterTubeImpl
    implements SecureConversationInitiator {

    protected PipeHelper helper;
   
    protected static final Logger _logger = LogUtils.getLogger();

    protected static final LocalStringManagerImpl localStrings = 
        new LocalStringManagerImpl(ClientSecurityTube.class);

    private static final String WSIT_CLIENT_AUTH_CONTEXT="com.sun.xml.wss.provider.wsit.WSITClientAuthContext";
    
    //Pipe to Tube Conversion
    private ClientAuthContext cAC = null;
    private PacketMessageInfo info = null;
    private Subject clientSubject = null;
    
    public ClientSecurityTube(Map props, Tube next) {
        
        super(next);
	props.put(PipeConstants.SECURITY_PIPE,this);
        WSDLPort wsdlModel = (WSDLPort)props.get(PipeConstants.WSDL_MODEL);
        if (wsdlModel != null) {
            props.put(PipeConstants.WSDL_SERVICE,
                wsdlModel.getOwner().getName());
        }
	this.helper = new PipeHelper(PipeConstants.SOAP_LAYER,props,null);

    }

    protected ClientSecurityTube(ClientSecurityTube that, TubeCloner cloner) {
        super(that, cloner);
        this.helper = that.helper;
    }
		       
    @Override
    public void preDestroy() {
        //Give the AuthContext a chance to cleanup 
        //create a dummy request packet
        try {
            Packet request = new Packet();
            PacketMessageInfo locInfo = new PacketMapMessageInfo(request, new Packet());
            Subject subj = getClientSubject(request);
            ClientAuthContext locCAC = helper.getClientAuthContext(locInfo, subj);
             if (locCAC != null && WSIT_CLIENT_AUTH_CONTEXT.equals(locCAC.getClass().getName())) {
                locCAC.cleanSubject(locInfo, subj);
            }
        } catch (Exception ex) {
        //ignore exceptions
        }
        helper.disable();
    }    
    
    @Override
    public NextAction processRequest(Packet request) {
        
        try {

            /*
             * XXX should there be code like the following?
            if(isHttpBinding) {
            return next.process(request);
            }
             */

            info = new PacketMapMessageInfo(request, new Packet());
            AuthStatus status = AuthStatus.SEND_SUCCESS;
            info.getMap().put(javax.xml.ws.Endpoint.WSDL_SERVICE,
                    helper.getProperty(PipeConstants.WSDL_SERVICE));

            Subject locClientSubject = getClientSubject(request);
            
            cAC = helper.getClientAuthContext(info, locClientSubject);

            if (cAC != null) {
                // proceed to process message sescurity
                status = cAC.secureRequest(info, locClientSubject);
            }
            if (status == AuthStatus.FAILURE) {
                if (_logger.isLoggable(Level.FINE)) {
                    _logger.log(Level.FINE, "ws.status_secure_request", status);
                }
                throw new WebServiceException(localStrings.getLocalString("enterprise.webservice.cantSecureRequst",
                        "Cannot secure request for {0}",
                        new Object[]{helper.getModelName()}), new Exception("An Error occured while Securing the Request"));
            } else {
                return doInvoke(super.next, info.getRequestPacket());
            }
        } catch (Exception e) {
            _logger.log(Level.SEVERE, LogUtils.ERROR_REQUEST_SECURING, e);
            throw new WebServiceException(localStrings.getLocalString("enterprise.webservice.cantSecureRequst",
                    "Cannot secure request for {0}",
                    new Object[]{helper.getModelName()}), e);
        }

    }
     
    @Override
    public NextAction processResponse(Packet response) {
        try {
            // check for response
            Message m = response.getMessage();
            if (m != null) {
                if (cAC != null) {
                    AuthStatus status;
                    info.setResponsePacket(response);
                    try {
                        status = cAC.validateResponse(info, clientSubject, null);
                    } catch (Exception e) {
                        return doThrow(new WebServiceException(
                                localStrings.getLocalString(
                                "enterprise.webservice.cantValidateResponse",
                                "Cannot validate response for {0}",
                                new Object[]{helper.getModelName()}), e));
                    }
                    if (status == AuthStatus.SEND_CONTINUE) {
                        //response = processSecureRequest(info, cAC, clientSubject);
                        return doInvoke(super.next, info.getRequestPacket());
                    } else {
                        response = info.getResponsePacket();
                    }
                }
            }
            return doReturnWith(response);
        } catch (Throwable t) {
            if (!(t instanceof WebServiceException)) {
                t = new WebServiceException(t);
            }
            return doThrow(t);
        }

    }
        
    private static Subject getClientSubject(Packet p) {

	Subject s = null;

	if (p != null) {
	    s = (Subject) 
		p.invocationProperties.get(PipeConstants.CLIENT_SUBJECT);
	}

	if (s == null) {

	    s = PipeHelper.getClientSubject();

            if (p != null) {
	        p.invocationProperties.put(PipeConstants.CLIENT_SUBJECT,s);
            }
	}
	
	return s;
    }
			
    @Override
    public JAXBElement startSecureConversation(Packet packet) 
            throws WSSecureConversationException {

	PacketMessageInfo locInfo = new PacketMapMessageInfo(packet,new Packet());
	JAXBElement token = null;

	try {

	    // gets the subject from the packet (puts one there if not found)
	    Subject locClientSubject = getClientSubject(packet);

	    // put MessageInfo in properties map, since MessageInfo 
	    // is not passed to getAuthContext, key idicates function
	    HashMap map = new HashMap();
	    map.put(PipeConstants.SECURITY_TOKEN,locInfo);

	    helper.getSessionToken(map,locInfo,locClientSubject);

	    // helper returns token in map of msgInfo, using same key
	    Object o = locInfo.getMap().get(PipeConstants.SECURITY_TOKEN);

	    if (o != null && o instanceof JAXBElement) {
		token = (JAXBElement) o;
	    }

	} catch(Exception e) {

	    if (e instanceof WSSecureConversationException) {
		throw (WSSecureConversationException) e;
	    } else {
		throw new WSSecureConversationException
		    ("Secure Conversation failure: ", e);
	    }
	} 

	return token;
    }

    @Override
    public AbstractTubeImpl copy(TubeCloner cloner) {
        return new ClientSecurityTube(this, cloner);
    }
}













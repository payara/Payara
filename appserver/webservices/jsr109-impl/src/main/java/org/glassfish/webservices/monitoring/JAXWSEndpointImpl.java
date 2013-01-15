/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.webservices.monitoring;

import java.util.logging.Level;
import javax.servlet.http.HttpServletRequest;
import javax.xml.ws.handler.MessageContext;

import org.glassfish.webservices.SOAPMessageContext;

import org.glassfish.webservices.LogUtils;


public class JAXWSEndpointImpl extends EndpointImpl {

    JAXWSEndpointImpl(String endpointSelector, EndpointType type) {
        super(endpointSelector, type);
    }
    
    public boolean processRequest(SOAPMessageContext messageContext)
                    throws Exception {

	boolean status = true;

        // let's get our thread local context
        WebServiceEngineImpl wsEngine = WebServiceEngineImpl.getInstance();
        try {            
            if (!listeners.isEmpty() || wsEngine.hasGlobalMessageListener()) {

                String messageID = (String) messageContext.get(EndpointImpl.MESSAGE_ID);
                
                // someone is listening ?
                if (messageID!=null) {
                    HttpServletRequest httpReq = (HttpServletRequest) messageContext.get(MessageContext.SERVLET_REQUEST);
                    HttpRequestInfoImpl info = new HttpRequestInfoImpl(httpReq);
                    wsEngine.processRequest(messageID, messageContext, info);
                } 
                
                // any local listeners ?
                if (!listeners.isEmpty()) {
                    // create the message trace and save it to our message context
                    MessageTraceImpl request = new MessageTraceImpl();
                    request.setEndpoint(this);
                    request.setMessageContext(messageContext);       
                    HttpServletRequest httpReq = (HttpServletRequest) messageContext.get(MessageContext.SERVLET_REQUEST);
                    request.setTransportInfo(new HttpRequestInfoImpl(httpReq));
                    messageContext.put(EndpointImpl.REQUEST_TRACE, request);                    
                }
            }
        } catch(Exception e) {
            WebServiceEngineImpl.sLogger.log(Level.WARNING, LogUtils.EXCEPTION_TRACING_REQUEST, e.getMessage());
	    throw e;
        }
        return status;
    }

    public void processResponse(SOAPMessageContext messageContext) throws Exception {   
        
        // let's get our thread local context
        WebServiceEngineImpl wsEngine = WebServiceEngineImpl.getInstance();
        try {
            
            if (wsEngine.hasGlobalMessageListener() || !listeners.isEmpty()) {
                
                String messageID = (String) messageContext.get(EndpointImpl.MESSAGE_ID);
                // do we have a global listener ?
                if (messageID!=null) {
                    wsEngine.processResponse(messageID,  messageContext);
                }
                
                // local listeners
                if (!listeners.isEmpty()) {
                    MessageTraceImpl response = new MessageTraceImpl();
                    response.setEndpoint(this);
                    response.setMessageContext(messageContext);
                    //TODO BM check regarding this method
                    for (org.glassfish.webservices.monitoring.MessageListener listener : listeners) {
                        listener.invocationProcessed((MessageTrace) messageContext.get(REQUEST_TRACE), response);
                    }
                }
            }
        } catch(Exception e) {
            WebServiceEngineImpl.sLogger.log(Level.WARNING, LogUtils.EXCEPTION_TRACING_RESPONSE, e.getMessage());
	    throw e;
        } 
    }
   
}

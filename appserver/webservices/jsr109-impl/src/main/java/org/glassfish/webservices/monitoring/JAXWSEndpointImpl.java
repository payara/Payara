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
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2019-2021] [Payara Foundation and/or its affiliates]
package org.glassfish.webservices.monitoring;

import static java.util.logging.Level.WARNING;
import static jakarta.xml.ws.handler.MessageContext.SERVLET_REQUEST;
import static org.glassfish.webservices.LogUtils.EXCEPTION_TRACING_REQUEST;
import static org.glassfish.webservices.LogUtils.EXCEPTION_TRACING_RESPONSE;

import jakarta.servlet.http.HttpServletRequest;

import org.glassfish.webservices.SOAPMessageContext;

public class JAXWSEndpointImpl extends EndpointImpl {

    JAXWSEndpointImpl(String endpointSelector, EndpointType type) {
        super(endpointSelector, type);
    }

    public boolean processRequest(SOAPMessageContext messageContext) throws Exception {

        boolean status = true;

        // Let's get our static context
        WebServiceEngineImpl wsMonitor = WebServiceEngineImpl.getInstance();
        try {
            if (!listeners.isEmpty() || wsMonitor.hasGlobalMessageListener()) {

                String messageID = (String) messageContext.get(MESSAGE_ID);

                // Someone is listening?
                if (messageID != null) {
                    HttpServletRequest httpReq = (HttpServletRequest) messageContext.get(SERVLET_REQUEST);
                    wsMonitor.processRequest(messageID, messageContext, new HttpRequestInfoImpl(httpReq));
                }

                // Any local listeners ?
                if (!listeners.isEmpty()) {
                    // Create the message trace and save it to our message context
                    MessageTraceImpl requestTrace = new MessageTraceImpl();
                    requestTrace.setEndpoint(this);
                    requestTrace.setMessageContext(messageContext);
                    HttpServletRequest httpReq = (HttpServletRequest) messageContext.get(SERVLET_REQUEST);
                    requestTrace.setTransportInfo(new HttpRequestInfoImpl(httpReq));
                    
                    messageContext.put(REQUEST_TRACE, requestTrace);
                }
            }
        } catch (Exception e) {
            WebServiceEngineImpl.sLogger.log(WARNING, EXCEPTION_TRACING_REQUEST, e.getMessage());
            throw e;
        }
        return status;
    }

    public void processResponse(SOAPMessageContext messageContext) throws Exception {

        // let's get our static context
        WebServiceEngineImpl wsEngine = WebServiceEngineImpl.getInstance();
        try {

            if (wsEngine.hasGlobalMessageListener() || !listeners.isEmpty()) {

                String messageID = (String) messageContext.get(MESSAGE_ID);
                
                // Do we have a global listener?
                if (messageID != null) {
                    wsEngine.processResponse(messageID, messageContext);
                }

                // Local listeners
                if (!listeners.isEmpty()) {
                    MessageTraceImpl responseTrace = new MessageTraceImpl();
                    responseTrace.setEndpoint(this);
                    responseTrace.setMessageContext(messageContext);
                    
                    // TODO BM check regarding this method
                    for (MessageListener listener : listeners) {
                        listener.invocationProcessed((MessageTrace) messageContext.get(REQUEST_TRACE), responseTrace);
                    }
                }
            }
        } catch (Exception e) {
            WebServiceEngineImpl.sLogger.log(WARNING, EXCEPTION_TRACING_RESPONSE, e.getMessage());
            throw e;
        }
    }

}

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

package org.glassfish.webservices;

import org.glassfish.webservices.monitoring.*;

import javax.xml.namespace.QName;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.http.HTTPBinding;

import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.pipe.PipeCloner;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.model.SEIModel;
import com.sun.xml.ws.api.model.JavaMethod;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.api.server.WSEndpoint;
import com.sun.xml.ws.api.pipe.ServerPipeAssemblerContext;

import com.sun.enterprise.deployment.WebServiceEndpoint;

import com.sun.xml.ws.api.pipe.helper.AbstractFilterPipeImpl;

/**
 * This pipe is used to do app server monitoring
 */
public class MonitoringPipe extends AbstractFilterPipeImpl {

    private final SEIModel seiModel;
    private final WSDLPort wsdlModel;
    private final WSEndpoint owner;
    private final WebServiceEndpoint endpoint;
    private final WebServiceEngineImpl wsEngine;

    public MonitoringPipe(ServerPipeAssemblerContext ctxt, Pipe tail,
                          WebServiceEndpoint ep) {
        super(tail);
        this.endpoint = ep;
        this.seiModel = ctxt.getSEIModel();
        this.wsdlModel = ctxt.getWsdlModel();
        this.owner = ctxt.getEndpoint();
        wsEngine = WebServiceEngineImpl.getInstance();
    }

    public MonitoringPipe(MonitoringPipe that, PipeCloner cloner) {
        super(that, cloner);
        this.endpoint = that.endpoint;
        this.seiModel = that.seiModel;
        this.wsdlModel = that.wsdlModel;
        this.owner = that.owner;
        wsEngine = WebServiceEngineImpl.getInstance();
    }

    public final Pipe copy(PipeCloner cloner) {
        return new MonitoringPipe(this, cloner);
    }

    public Packet process(Packet request) {
        // if it is a JBI request then skip the monitoring logic. This is done 
        // as HTTPServletRequest/Response is not available when the invocation 
        // is from JavaEE service engine.

        String delegateClassName = request.webServiceContextDelegate.getClass().getName();
        if (delegateClassName.equals("com.sun.enterprise.jbi.serviceengine." +
                "bridge.transport.NMRServerConnection")) {
            return next.process(request);
        }
  
        // No monitoring available for restful services
        if("http://www.w3.org/2004/08/wsdl/http".equals(endpoint.getProtocolBinding())) {
            return next.process(request);
        }
        SOAPMessageContext ctxt = new SOAPMessageContextImpl(request);
        HttpServletRequest httpRequest =
                (HttpServletRequest) request.get(javax.xml.ws.handler.MessageContext.SERVLET_REQUEST);
        HttpServletResponse httpResponse =
                (HttpServletResponse) request.get(javax.xml.ws.handler.MessageContext.SERVLET_RESPONSE);

        String messageId=null;

        JAXWSEndpointImpl endpt1;
        if(endpoint.implementedByWebComponent()) {
            endpt1 = (JAXWSEndpointImpl)wsEngine.getEndpoint(httpRequest.getServletPath());
        } else {
            endpt1 = (JAXWSEndpointImpl)wsEngine.getEndpoint(httpRequest.getRequestURI());
        }
        messageId = wsEngine.preProcessRequest(endpt1);
        if (messageId!=null) {
            ctxt.put(EndpointImpl.MESSAGE_ID, messageId);
            ThreadLocalInfo config = new ThreadLocalInfo(messageId, httpRequest);
            wsEngine.getThreadLocal().set(config);
        }

        try {

            endpt1.processRequest(ctxt);

        } catch (Exception e) {
            // temporary - need to send back SOAP fault message
        }

        Packet pipeResponse = next.process(request);

        //Make the response packet available in the MessageContext
        ((SOAPMessageContextImpl)ctxt).setPacket(pipeResponse);


        try {
            if (endpt1 != null) {
                endpt1.processResponse(ctxt);
            }

        } catch (Exception e) {
            // temporary - need to send back SOAP fault message
        }

        if (messageId!=null) {
            HttpResponseInfoImpl info = new HttpResponseInfoImpl(httpResponse);
            wsEngine.postProcessResponse(messageId, info);
        }
        return pipeResponse;
    }
}

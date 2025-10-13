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
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
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
package org.glassfish.webservices;

import static jakarta.xml.ws.handler.MessageContext.SERVLET_REQUEST;
import static jakarta.xml.ws.handler.MessageContext.SERVLET_RESPONSE;
import static org.glassfish.webservices.monitoring.EndpointImpl.MESSAGE_ID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.internal.api.Globals;
import org.glassfish.webservices.monitoring.HttpResponseInfoImpl;
import org.glassfish.webservices.monitoring.JAXWSEndpointImpl;
import org.glassfish.webservices.monitoring.MonitorContextImpl;
import org.glassfish.webservices.monitoring.MonitorFilter;
import org.glassfish.webservices.monitoring.ThreadLocalInfo;
import org.glassfish.webservices.monitoring.WebServiceEngineImpl;

import com.oracle.webservices.api.databinding.JavaCallInfo;
import com.sun.enterprise.deployment.WebServiceEndpoint;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.model.SEIModel;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.pipe.PipeCloner;
import com.sun.xml.ws.api.pipe.ServerPipeAssemblerContext;
import com.sun.xml.ws.api.pipe.helper.AbstractFilterPipeImpl;
import com.sun.xml.ws.api.server.WSEndpoint;
import com.sun.xml.ws.model.SOAPSEIModel;

/**
 * This pipe is used to do app server monitoring
 */
public class MonitoringPipe extends AbstractFilterPipeImpl {

    private final SEIModel seiModel;
    private final WSDLPort wsdlModel;
    private final WSEndpoint<?> ownerEndpoint;
    private final WebServiceEndpoint endpoint;
    private final WebServiceEngineImpl wsMonitor;

    public MonitoringPipe(ServerPipeAssemblerContext assemblerContext, Pipe tail, WebServiceEndpoint endpoint) {
        super(tail);
        
        this.endpoint = endpoint;
        
        seiModel = assemblerContext.getSEIModel();
        wsdlModel = assemblerContext.getWsdlModel();
        ownerEndpoint = assemblerContext.getEndpoint();
        wsMonitor = WebServiceEngineImpl.getInstance();
    }

    public MonitoringPipe(MonitoringPipe that, PipeCloner cloner) {
        super(that, cloner);
        
        this.endpoint = that.endpoint;
        this.seiModel = that.seiModel;
        this.wsdlModel = that.wsdlModel;
        this.ownerEndpoint = that.ownerEndpoint;
        
        wsMonitor = WebServiceEngineImpl.getInstance();
    }

    @Override
    public Packet process(Packet pipeRequest) {
        
        // If it is a JBI request then skip the monitoring logic. This is done
        // as HTTPServletRequest/Response is not available when the invocation
        // is from JavaEE service engine.

        String delegateClassName = pipeRequest.webServiceContextDelegate.getClass().getName();
        if (delegateClassName.equals("com.sun.enterprise.jbi.serviceengine." + "bridge.transport.NMRServerConnection")) {
            return next.process(pipeRequest);
        }

        // No monitoring available for restful services
        if ("http://www.w3.org/2004/08/wsdl/http".equals(endpoint.getProtocolBinding())) {
            return next.process(pipeRequest);
        }
       
        HttpServletRequest httpRequest = (HttpServletRequest) pipeRequest.get(SERVLET_REQUEST);
        HttpServletResponse httpResponse = (HttpServletResponse) pipeRequest.get(SERVLET_RESPONSE);
        

        JAXWSEndpointImpl endpointTracer = getEndpointTracer(httpRequest);
        SOAPMessageContextImpl soapMessageContext = new SOAPMessageContextImpl(pipeRequest);
        
        InvocationManager invocationManager = Globals.get(InvocationManager.class);
        JavaCallInfo javaCallInfo = getJavaCallInfo(pipeRequest);
        
        try {
            pushWebServiceMethod(invocationManager, javaCallInfo);

            firePreInvocation(httpRequest, pipeRequest, endpointTracer, soapMessageContext, javaCallInfo);
            
            // Copy pipe request, since when the endpoint is NOT an EJB, it's body will be emptied after the service invocation
            Packet originalPipeRequest = pipeRequest.copy(true);
    
            Packet pipeResponse = next.process(pipeRequest);
    
            firePostInvocation(httpResponse, pipeResponse, originalPipeRequest, endpointTracer, soapMessageContext);
            
            return pipeResponse;
        } finally {
            popWebServiceMethod(invocationManager, javaCallInfo);
        }
    }
    
    @Override
    public final Pipe copy(PipeCloner cloner) {
        return new MonitoringPipe(this, cloner);
    }
    
    private JAXWSEndpointImpl getEndpointTracer(HttpServletRequest httpRequest) {
        if (endpoint.implementedByWebComponent()) {
            return getJAXWSEndpointImpl(httpRequest.getServletPath());
        }
        
        return getJAXWSEndpointImpl(httpRequest.getRequestURI());
    }
    
    private JAXWSEndpointImpl getJAXWSEndpointImpl(String uri) {
        return (JAXWSEndpointImpl) wsMonitor.getEndpoint(uri);
    }
    
    private void firePreInvocation(HttpServletRequest httpRequest, Packet pipeRequest, JAXWSEndpointImpl endpointTracer, SOAPMessageContextImpl soapMessageContext, JavaCallInfo javaCallInfo) {
        
        if (seiModel instanceof SOAPSEIModel) {
            
            SOAPSEIModel soapSEIModel = (SOAPSEIModel) seiModel;
            
            Globals.get(MonitorFilter.class)
                   .filterRequest(
                       pipeRequest, 
                       new MonitorContextImpl(
                           javaCallInfo, 
                           soapSEIModel, wsdlModel, ownerEndpoint, endpoint));
            
        }
        
        // Invoke preProcessRequest on global listeners. If there's a global listener we get
        // a trace ID back to trace this message
        String messageTraceId = wsMonitor.preProcessRequest(endpointTracer);
        if (messageTraceId != null) {
            soapMessageContext.put(MESSAGE_ID, messageTraceId);
            wsMonitor.getThreadLocal().set(new ThreadLocalInfo(messageTraceId, httpRequest));
        }
        
        try {
            // Invoke processRequest on global listeners
            endpointTracer.processRequest(soapMessageContext);
        } catch (Exception e) {
            // temporary - need to send back SOAP fault message
        }
    }
    
    private JavaCallInfo getJavaCallInfo(Packet pipeRequest) {
        
        if (seiModel instanceof SOAPSEIModel) {
            
            SOAPSEIModel soapSEIModel = (SOAPSEIModel) seiModel;
        
            return soapSEIModel.getDatabinding().deserializeRequest(pipeRequest.copy(true));
        }
        
        return null;
    }
    
    private void pushWebServiceMethod(InvocationManager invocationManager, JavaCallInfo javaCallInfo) {
        if (javaCallInfo != null) {
            invocationManager.pushWebServiceMethod(javaCallInfo.getMethod());
        }
    }
    
    private void popWebServiceMethod(InvocationManager invocationManager, JavaCallInfo javaCallInfo) {
        if (javaCallInfo != null) {
            invocationManager.popWebServiceMethod();
        }
    }
    
    private void firePostInvocation(HttpServletResponse httpResponse, Packet pipeResponse, Packet pipeRequest, JAXWSEndpointImpl endpointTracer, SOAPMessageContextImpl soapMessageContext) {
        
        if (seiModel instanceof SOAPSEIModel) {
            
            SOAPSEIModel soapSEIModel = (SOAPSEIModel) seiModel;
            
            Globals.get(MonitorFilter.class)
                   .filterResponse(
                       pipeRequest,
                       pipeResponse, 
                       new MonitorContextImpl(
                           soapSEIModel.getDatabinding().deserializeRequest(pipeRequest), 
                           soapSEIModel, wsdlModel, ownerEndpoint, endpoint));
            
        }
        
        
        // Make the response packet available in the MessageContext
        soapMessageContext.setPacket(pipeResponse);
        
        try {
            // Invoke processRequest on global and local listeners
            endpointTracer.processResponse(soapMessageContext);
        } catch (Exception e) {
            // temporary - need to send back SOAP fault message
        }
        
        String messageTraceId = (String) soapMessageContext.get(MESSAGE_ID);
        
        if (messageTraceId != null) {
            wsMonitor.postProcessResponse(messageTraceId, new HttpResponseInfoImpl(httpResponse));
        }
    }
}

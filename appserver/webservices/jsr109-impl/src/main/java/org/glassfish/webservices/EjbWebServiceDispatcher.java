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

package org.glassfish.webservices;

import java.io.InputStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletContext;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPException;

import javax.xml.namespace.QName;
import com.sun.xml.messaging.saaj.util.ByteInputStream;
import com.sun.xml.rpc.spi.JaxRpcObjectFactory;
import com.sun.xml.rpc.spi.runtime.SOAPMessageContext;
import com.sun.xml.rpc.spi.runtime.SOAPConstants;
import com.sun.xml.rpc.spi.runtime.StreamingHandler;
import java.text.MessageFormat;

import org.glassfish.webservices.monitoring.*;

import java.util.logging.Logger;
import java.util.logging.Level;
import org.glassfish.internal.api.Globals;
import org.glassfish.ejb.api.EJBInvocation;

/**
 * Handles dispatching of ejb web service http invocations.
 *
 * @author Kenneth Saks
 */
public class EjbWebServiceDispatcher implements EjbMessageDispatcher {

    private static final Logger logger = LogUtils.getLogger();

    private JaxRpcObjectFactory rpcFactory;
    private final WsUtil wsUtil = new WsUtil();
    private WebServiceEngineImpl wsEngine;

    // @@@ This should be added to jaxrpc spi, probably within
    // SOAPConstants.
    private static final QName FAULT_CODE_CLIENT =
        new QName(SOAPConstants.URI_ENVELOPE, "Client");

    // @@@ Used to set http servlet response object so that TieBase
    // will correctly flush response code for one-way operations.
    // Should be added to SPI
    private static final String HTTP_SERVLET_RESPONSE = 
        "com.sun.xml.rpc.server.http.HttpServletResponse";

    //the security service
    org.glassfish.webservices.SecurityService  secServ;

    public EjbWebServiceDispatcher() {
        rpcFactory = JaxRpcObjectFactory.newInstance();
        wsEngine = WebServiceEngineImpl.getInstance();
        if (Globals.getDefaultHabitat() != null) {
            secServ = Globals.get(org.glassfish.webservices.SecurityService.class);
        }
    }           

    @Override
    public void invoke(HttpServletRequest req, 
                       HttpServletResponse resp,
                       ServletContext ctxt,
                       EjbRuntimeEndpointInfo endpointInfo) {

        String method = req.getMethod();
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, LogUtils.WEBSERVICE_DISPATCHER_INFO,
                    new Object[] {req.getMethod(), req.getRequestURI(), req.getQueryString()});
        }
        try {
            if( method.equals("POST") ) {
                handlePost(req, resp, endpointInfo);
            } else if( method.equals("GET") ) {
                handleGet(req, resp, ctxt, endpointInfo);
            } else {
                String errorMessage = MessageFormat.format(
                        logger.getResourceBundle().getString(LogUtils.UNSUPPORTED_METHOD_REQUEST),
                        new Object[] {method, endpointInfo.getEndpoint().getEndpointName(),
                            endpointInfo.getEndpointAddressUri()});
                logger.log(Level.WARNING, errorMessage);
                wsUtil.writeInvalidMethodType(resp, errorMessage);
            }
        } catch(Exception e) {
            logger.log(Level.WARNING, LogUtils.EJB_ENDPOINT_EXCEPTION, e);
        }
    }

    private void handlePost(HttpServletRequest req,
                            HttpServletResponse resp,
                            EjbRuntimeEndpointInfo endpointInfo)
        throws IOException, SOAPException {
        
        JAXRPCEndpointImpl endpoint = null;               
        String messageID = null;
        SOAPMessageContext msgContext = null;
        
        try {
            
            MimeHeaders headers = wsUtil.getHeaders(req);
            if (!wsUtil.hasTextXmlContentType(headers)) {
                wsUtil.writeInvalidContentType(resp);
                return;
            }
            
            msgContext = rpcFactory.createSOAPMessageContext();
            SOAPMessage message = createSOAPMessage(req, headers);
                        
    	    boolean wssSucceded = true;
            
            if (message != null) {                                
                
                msgContext.setMessage(message);

                // get the endpoint info
                endpoint = (JAXRPCEndpointImpl) endpointInfo.getEndpoint().getExtraAttribute(EndpointImpl.NAME);
                
                if (endpoint!=null) {
                    // first global notification
                    if (wsEngine.hasGlobalMessageListener()) {
                        messageID = wsEngine.preProcessRequest(endpoint);
                    }
                } else {
                    if (logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE, LogUtils.MISSING_MONITORING_INFO, req.getRequestURI());
                    }
                }                                   
                
                AdapterInvocationInfo aInfo = null;
                
                if (!(endpointInfo instanceof Ejb2RuntimeEndpointInfo)) {
                    throw new IllegalArgumentException(endpointInfo + "is not instance of Ejb2RuntimeEndpointInfo.");
                }

                try {
                    Ejb2RuntimeEndpointInfo endpointInfo2 = (Ejb2RuntimeEndpointInfo)endpointInfo;
                    // Do ejb container pre-invocation and pre-handler
                    // logic
                    aInfo = endpointInfo2.getHandlerImplementor();

                    // Set message context in invocation
                    EJBInvocation.class.cast(aInfo.getInv()).setMessageContext(msgContext);


                    // Set http response object so one-way operations will
                    // response before actual business method invocation.
                    msgContext.setProperty(HTTP_SERVLET_RESPONSE, resp);
                    if (secServ != null) {
                        wssSucceded = secServ.validateRequest(endpointInfo2.getServerAuthConfig(),
                                (StreamingHandler)aInfo.getHandler(), msgContext);
                    }
                    // Trace if necessary
                    if (messageID!=null || (endpoint!=null && endpoint.hasListeners())) {
                        // create the thread local
                        ThreadLocalInfo threadLocalInfo = new ThreadLocalInfo(messageID, req);
                        wsEngine.getThreadLocal().set(threadLocalInfo);
                        if (endpoint != null) {
                            endpoint.processRequest(msgContext);
                        } else {
                            if (logger.isLoggable(Level.FINE)) {
                                logger.log(Level.FINE, LogUtils.MISSING_MONITORING_INFO, req.getRequestURI());
                            }
                        }
                    }
                    
                    // Pass control back to jaxrpc runtime to invoke
                    // any handlers and call the webservice method itself,
                    // which will be flow back into the ejb container.
                    if (wssSucceded) {
                        aInfo.getHandler().handle(msgContext);
                    }
                } finally {
                    // Always call release, even if an error happened
                    // during getImplementor(), since some of the
                    // preInvoke steps might have occurred.  It's ok
                    // if implementor is null.
                    if (aInfo != null) {
                        endpointInfo.releaseImplementor(aInfo.getInv());
                    }
                }
            } else {
                String errorMsg = MessageFormat.format(
                        logger.getResourceBundle().getString(LogUtils.NULL_MESSAGE),
                        endpointInfo.getEndpoint().getEndpointName(), endpointInfo.getEndpointAddressUri());
                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine(errorMsg);
                    }
                    msgContext.writeSimpleErrorResponse
                    (FAULT_CODE_CLIENT, errorMsg);
            }
            if (messageID!=null || endpoint!=null) {
                endpoint.processResponse(msgContext);
            }
            SOAPMessage reply = msgContext.getMessage();
            if (secServ != null && wssSucceded) {
                if (!(endpointInfo instanceof Ejb2RuntimeEndpointInfo)) {
                    throw new IllegalArgumentException(endpointInfo + "is not instance of Ejb2RuntimeEndpointInfo.");
                }

                Ejb2RuntimeEndpointInfo endpointInfo2 = (Ejb2RuntimeEndpointInfo)endpointInfo;
                secServ.secureResponse(endpointInfo2.getServerAuthConfig(),(StreamingHandler)endpointInfo2.getHandlerImplementor().getHandler(),msgContext);
            }
            
            if (reply.saveRequired()) {
                reply.saveChanges();
            }
            wsUtil.writeReply(resp, msgContext);
        } catch (Throwable e) {
            String errorMessage = MessageFormat.format(
                    logger.getResourceBundle().getString(LogUtils.ERROR_ON_EJB),
                    new Object[] {endpointInfo.getEndpoint().getEndpointName(),
                        endpointInfo.getEndpointAddressUri(), e.getMessage()});
            logger.log(Level.WARNING, errorMessage, e);
            SOAPMessageContext errorMsgContext =
                rpcFactory.createSOAPMessageContext();
            errorMsgContext.writeSimpleErrorResponse
                (SOAPConstants.FAULT_CODE_SERVER, errorMessage);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            if (messageID!=null || endpoint!=null) {
                endpoint.processResponse(errorMsgContext);
            }
            wsUtil.writeReply(resp, errorMsgContext);
        }

        // final tracing notification
        if (messageID!=null) {
            HttpResponseInfoImpl response = new HttpResponseInfoImpl(resp);
            wsEngine.postProcessResponse(messageID, response);
        }
    }

    private void handleGet(HttpServletRequest req, 
                           HttpServletResponse resp,
                           ServletContext ctxt,
                           EjbRuntimeEndpointInfo endpointInfo)
        throws IOException
    {
       
        wsUtil.handleGet(req, resp, endpointInfo.getEndpoint());           

    }    

    protected SOAPMessage createSOAPMessage(HttpServletRequest request,
                                            MimeHeaders headers)
        throws IOException {

        InputStream is = request.getInputStream();
        
        byte[] bytes = readFully(is);
        int length = request.getContentLength() == -1 ? bytes.length 
            : request.getContentLength();
        ByteInputStream in = new ByteInputStream(bytes, length);

        SOAPMessageContext msgContext = rpcFactory.createSOAPMessageContext();
        SOAPMessage message = msgContext.createMessage(headers, in);

        return message;
    }

    protected byte[] readFully(InputStream istream) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int num = 0;
        while( (num = istream.read(buf)) != -1) {
            bout.write(buf, 0, num);
        }
        byte[] ret = bout.toByteArray();
        return ret;
    }
}

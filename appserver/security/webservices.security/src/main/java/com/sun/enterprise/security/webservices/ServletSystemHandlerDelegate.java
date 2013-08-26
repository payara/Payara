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

import java.util.logging.*;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;


import javax.xml.soap.SOAPMessage;

import javax.security.auth.Subject;

import com.sun.enterprise.security.jauth.*;

import com.sun.xml.rpc.spi.runtime.Implementor;
import com.sun.xml.rpc.spi.runtime.SOAPMessageContext;
import com.sun.xml.rpc.spi.runtime.StreamingHandler;
import com.sun.xml.rpc.spi.runtime.SystemHandlerDelegate;
import com.sun.xml.rpc.spi.runtime.Tie;

//import com.sun.xml.rpc.server.http.MessageContextProperties;

import com.sun.enterprise.security.SecurityContext;


import com.sun.enterprise.deployment.WebServiceEndpoint;

import com.sun.enterprise.security.jmac.provider.ServerAuthConfig;


/**
 * The methods of this interface are invoked by the JAXRPCServletDelegate
 * on the path to web sevice endpoints deployed as servlets.
 *
 * NOTE: The methods of this interface may also be called on the client side of
 * jaxrpc invocations, although at this time, we have not decided from
 * where such invocations would be made.
 *
 * @author Ron Monzillo
 */

public class ServletSystemHandlerDelegate implements SystemHandlerDelegate {

    protected static final Logger _logger = LogUtils.getLogger();

    private static final String IMPLEMENTOR = 
	"com.sun.xml.rpc.server.http.Implementor";
    private static final String SERVER_AUTH_CONTEXT = 
	"com.sun.enterprise.security.jauth.ServerAuthContext";

    ServerAuthConfig config_;
    WebServiceEndpoint endpoint_;

    public ServletSystemHandlerDelegate(ServerAuthConfig config, WebServiceEndpoint ep) {
	config_ = config;
        endpoint_ = ep;
    }

   /**
    * The processRequest method is invoked with an object that 
    * implements com.sun.xml.rpc.spi.runtime.SOAPMessageContext.
    * <p>
    * When this method is called by the JAXRPCServletDelegate
    * (on the server side of jaxrpc servlet container invocation processing)
    * it must be called just before the call to implementor.getTie().handle(),
    * and at the time of the request message and the following properties 
    * must have been set on the SOAPMessageContext.
    * <p>
    * com.sun.xml.rpc.server.http.MessageContextProperties.IMPLEMENTOR
    * <br>
    * This property must be set to the com.sun.xml.rpc.spi.runtime.Implementor 
    * object corresponding to the target endpoint.
    * <p>
    * com.sun.xml.rpc.server.http.MessageContextProperties.HTTP_SERVLET_REQUEST
    * <br>
    * This property must be
    * set to the javax.servlet.http.HttpServletRequest object containing the 
    * JAXRPC invocation.
    * <p>
    * com.sun.xml.rpc.server.http.MessageContextProperties.HTTP_SERVLET_RESPONSE
    * <br>
    * This property must be
    * set to the javax.servlet.http.HttpServletResponse object corresponding to
    * the JAXRPC invocation.
    * <p>
    * com.sun.xml.rpc.server.MessageContextProperties.HTTP_SERVLET_CONTEXT
    * <br>
    * This property must be
    * set to the javax.servlet.ServletContext object corresponding to web application
    * in which the JAXRPC servlet is running.
    * @param messageContext the SOAPMessageContext object containing the request
    * message and the properties described above.
    * @return true if processing by the delegate was such that the caller
    * should continue with its normal message processing. Returns false if the
    * processing by the delegate resulted in the messageContext containing a response
    * message that should be returned without the caller proceding to its normal
    * message processing. 
    * @throws java.lang.RuntimeException when the processing by the delegate failed,
    * without yielding a response message. In this case, the expectation is that
    * the caller will return a HTTP layer response code reporting that an internal
    * error occured.
    */
    public boolean processRequest(SOAPMessageContext messageContext) {

	if(_logger.isLoggable(Level.FINE)){
	    _logger.fine("ws.processRequest");
	}

        final SOAPMessageContext finalMC = messageContext;
	Implementor implementor = (Implementor) messageContext.getProperty( IMPLEMENTOR );
        final Tie tie = implementor.getTie();
	StreamingHandler handler = (StreamingHandler) implementor.getTie();
	SOAPMessage request = finalMC.getMessage();
	final ServerAuthContext sAC = config_.getAuthContext(handler,request);

        boolean status = true;
	try {
	    if (sAC != null) {
		status = false;
                // proceed to process message security
                status = WebServiceSecurity.validateRequest(finalMC,sAC);

		if (status) {
		    messageContext.setProperty(SERVER_AUTH_CONTEXT, sAC);
		}
            } 
	} catch (AuthException ae) {
	    _logger.log(Level.SEVERE, LogUtils.ERROR_REQUEST_VALIDATION, ae);
	    throw new RuntimeException(ae);
	} finally {
	    WebServiceSecurity.auditInvocation(messageContext, endpoint_, status); 
        }

        if (status) {

	    // only do doAsPriv if SecurityManager in effect.

	    if (System.getSecurityManager() != null) {

		// on this branch, the endpoint invocation and the 
		// processing of the response will be initiated from
		// within the system handler delegate. delegate returns
		// false so that dispatcher will not invoke the endpoint.

		status = false;

		try {

		    Subject.doAsPrivileged
			(SecurityContext.getCurrent().getSubject(),
			 new PrivilegedExceptionAction() {
			    public Object run() throws Exception {
				tie.handle(finalMC);
				processResponse(finalMC);
				return null;
			    }
                     }, null);

		} catch (PrivilegedActionException pae) {
		    Throwable cause = pae.getCause();
		    if (cause instanceof AuthException){
			_logger.log(Level.SEVERE, LogUtils.ERROR_RESPONSE_SECURING, cause);
		    }
		    RuntimeException re = null;
		    if (cause instanceof RuntimeException) {
			re = (RuntimeException) cause;
		    } else {
			re = new RuntimeException(cause);
		    }
		    throw re;
		}
	    }
        }
	return status;
    }

   /**
    * The processResponse method is invoked with an object that 
    * implements com.sun.xml.rpc.spi.runtime.SOAPMessageContext.
    * <p>
    * When this method is called by the JAXRPCServletDelegate
    * (on the server side of jaxrpc servlet container invocation processing)
    * it must be called just just after the call to implementor.getTie().handle().
    * In the special case where the handle method throws an exception, the
    * processResponse message must not be called.
    * <p>
    * The SOAPMessageContext passed to the processRequest and handle messages is
    * passed to the processResponse method.
    * @throws java.lang.RuntimeException when the processing by the delegate failed,
    * in which case the caller is expected to return an HTTP layer 
    * response code reporting that an internal error occured.
    */
    public void processResponse(SOAPMessageContext messageContext) {

	if(_logger.isLoggable(Level.FINE)){
	    _logger.fine("ws.processResponse");
	}

	ServerAuthContext sAC = 
	    (ServerAuthContext) messageContext.getProperty( SERVER_AUTH_CONTEXT );

	if (sAC == null) {
	    return;
	}

	try {
	    WebServiceSecurity.secureResponse(messageContext,sAC);
	} catch (AuthException ae) {
            _logger.log(Level.SEVERE, LogUtils.ERROR_RESPONSE_SECURING, ae);
	    throw new RuntimeException(ae);
	}
    }
}

    

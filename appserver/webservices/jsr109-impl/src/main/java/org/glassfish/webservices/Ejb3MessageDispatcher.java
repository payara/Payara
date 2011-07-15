/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;


import java.util.logging.Logger;
import java.util.logging.Level;
import com.sun.logging.LogDomains;



import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletContext;



import com.sun.xml.ws.transport.http.servlet.ServletAdapter;
import org.glassfish.webservices.monitoring.WebServiceEngineImpl;

/**
 * Implementation of the Ejb Message Dispatcher for EJB3 endpoints.
 *
 * @author Jerome Dochez
 */
public class Ejb3MessageDispatcher implements EjbMessageDispatcher {
    
    protected Logger logger = LogDomains.getLogger(this.getClass(),LogDomains.WEBSERVICES_LOGGER);
    
    private static WsUtil wsUtil = new WsUtil();
    

    
    
    public void invoke(HttpServletRequest req, 
                       HttpServletResponse resp,
                       ServletContext ctxt,
                       EjbRuntimeEndpointInfo endpointInfo) {
        
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "JAXWS WebServiceDispatcher " + req.getMethod() + 
                   " entering for " + req.getRequestURI() + " and query string " + req.getQueryString());
        }       
        String method = req.getMethod();
        try {
            if( method.equals("POST") ) {
                handlePost(req, resp, endpointInfo);
            } else if( method.equals("GET") ) {
                handleGet(req, resp, ctxt, endpointInfo);
            } else {
                String errorMessage =  "Unsupported method request = [" 
                    + method + "] for endpoint " + 
                    endpointInfo.getEndpoint().getEndpointName() + " at " + 
                    endpointInfo.getEndpointAddressUri();
                logger.warning(errorMessage);
                wsUtil.writeInvalidMethodType(resp, errorMessage);
            }
        } catch(Exception e) {
            logger.log(Level.WARNING, "ejb endpoint exception", e);
        }
    } 
    
    private void handlePost(HttpServletRequest req,
                            HttpServletResponse resp,
                            EjbRuntimeEndpointInfo endpointInfo)
        throws IOException    {
        AdapterInvocationInfo adapterInfo = null;
        ServletAdapter adapter;
        try {            
            try {

                 adapterInfo =
                        (AdapterInvocationInfo) endpointInfo.prepareInvocation(true);
                adapter = adapterInfo.getAdapter();
                if (adapter != null) {                    
                    adapter.handle(null, req, resp);
                } else {
                    logger.log(Level.SEVERE, 
                            "Unable to find adpater for endpoint " + endpointInfo.getEndpoint().getName());
                }
            } finally {
                // Always call release, even if an error happened
                // during getImplementor(), since some of the
                // preInvoke steps might have occurred.  It's ok
                // if implementor is null.
                endpointInfo.releaseImplementor(adapterInfo.getInv());
            }    
        } catch (Throwable e) {
            String errorMessage = "invocation error on ejb endpoint " +
            endpointInfo.getEndpoint().getEndpointName() + " at " +
            endpointInfo.getEndpointAddressUri() + " : " + e.getMessage();
            logger.log(Level.WARNING, errorMessage, e);
            String binding = endpointInfo.getEndpoint().getProtocolBinding();
            WsUtil.raiseException(resp, binding, errorMessage);
        }
    }    
    
    private void handleGet(HttpServletRequest req, 
                           HttpServletResponse resp,
                           ServletContext ctxt,
                           EjbRuntimeEndpointInfo endpointInfo)
                            throws IOException    {
        AdapterInvocationInfo adapterInfo = null;
        ServletAdapter adapter;
        try {
             adapterInfo =
                        (AdapterInvocationInfo) endpointInfo.prepareInvocation(true);
            adapter = adapterInfo.getAdapter();
            if (adapter != null) {
                adapter.publishWSDL(ctxt, req, resp);
            } else {
                String message = "Invalid wsdl request " +  req.getRequestURL();
                (new WsUtil()).writeInvalidMethodType(resp, message);
            }
        } catch (Throwable e) {
            String errorMessage = "invocation error on ejb endpoint " +
            endpointInfo.getEndpoint().getEndpointName() + " at " +
            endpointInfo.getEndpointAddressUri() + " : " + e.getMessage();
            logger.log(Level.WARNING, errorMessage, e);
            String binding = endpointInfo.getEndpoint().getProtocolBinding();
            WsUtil.raiseException(resp, binding, errorMessage);
        }
    }      
}

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

/*
 * JAXRPCEndpointImpl.java
 */

package org.glassfish.webservices.monitoring;

import com.sun.xml.rpc.spi.runtime.SOAPMessageContext;
import com.sun.xml.rpc.spi.runtime.SystemHandlerDelegate;
import java.util.logging.Level;
import org.glassfish.webservices.LogUtils;

/**
 * Implementation of the JAXRPC endpoint interface and JAXRPC System Handler Delegate
 *
 * @author Jerome Dochez
 */
public class JAXRPCEndpointImpl extends EndpointImpl implements SystemHandlerDelegate {
    
    SystemHandlerDelegate parent = null;
    
    /** Creates a new instance of EndpointImpl */
    JAXRPCEndpointImpl(String endpointSelector, EndpointType type) {
        super(endpointSelector, type);
    }
    
    public boolean processRequest(SOAPMessageContext messageContext) {

	boolean status = true;

        if (parent!=null) {
            status = parent.processRequest(messageContext);
        }

        // let's get our thread local context
        WebServiceEngineImpl wsEngine = WebServiceEngineImpl.getInstance();
        try {
            if (!listeners.isEmpty() || wsEngine.hasGlobalMessageListener()) {
                
                // someone is listening
                ThreadLocalInfo config = 
                        (ThreadLocalInfo) wsEngine.getThreadLocal().get();

                // do we have a global listener ?
                if (config!=null && config.getMessageId()!=null) {
                    HttpRequestInfoImpl info = new HttpRequestInfoImpl(config.getRequest());
                    wsEngine.processRequest(config.getMessageId(), messageContext, info);
                } 
                
                // any local listeners ?
                if (!listeners.isEmpty()) {
                    if (config==null) {
                        config = new ThreadLocalInfo(null, null);
                    }
                    // create the message trace and save it to our thread local
                    MessageTraceImpl request = new MessageTraceImpl();
                    request.setEndpoint(this);
                    request.setMessageContext(messageContext);
                    if (config.getRequest()!=null) {
                        request.setTransportInfo(new HttpRequestInfoImpl(config.getRequest()));
                    }
                    
                    config.setRequestMessageTrace(request);
                }
                
            }
	    } catch(Throwable t) {
                WebServiceEngineImpl.sLogger.log(Level.WARNING, LogUtils.EXCEPTION_TRACING_REQUEST, t.getMessage());
	        RuntimeException re;
            if (t instanceof RuntimeException) {
		        re = (RuntimeException) t;
	        } else {
		        re = new RuntimeException(t);
	        }
	        throw re;
        }        
        return status;
    }

    public void processResponse(SOAPMessageContext messageContext) {   

        // let's get our thread local context
        WebServiceEngineImpl wsEngine = WebServiceEngineImpl.getInstance();
        try {
            
            if (wsEngine.hasGlobalMessageListener() || !listeners.isEmpty()) {
                
                // someone is listening
                ThreadLocalInfo config = 
                        (ThreadLocalInfo) wsEngine.getThreadLocal().get();

                if (config!=null) {                    
                    // do we have a global listener ?
                    if (config.getMessageId()!=null) {
                        wsEngine.processResponse(config.getMessageId(),  messageContext);
                    }

                    // local listeners
                    if (!listeners.isEmpty()) {
                        MessageTraceImpl response = new MessageTraceImpl();
                        response.setEndpoint(this);
                        response.setMessageContext(messageContext);
                        for (MessageListener listener : listeners) {                    
                            listener.invocationProcessed(config.getRequestMessageTrace(), response);
                        }   
                    }
                }
            }
            // cleanup
            wsEngine.getThreadLocal().remove();
            
	        // do security after tracing
	        if (parent!=null) {
		    parent.processResponse(messageContext);
	            }
        
        } catch(Throwable t) {
            WebServiceEngineImpl.sLogger.log(Level.WARNING, LogUtils.EXCEPTION_TRACING_RESPONSE, t.getMessage());
	        RuntimeException re;
	        if (t instanceof RuntimeException) {
		        re = (RuntimeException) t;
	        } else {
		        re = new RuntimeException(t);
	        }
	        throw re;
        }                 
    }
    
    public void setParent(SystemHandlerDelegate parent) {
        this.parent = parent;
    }        
}

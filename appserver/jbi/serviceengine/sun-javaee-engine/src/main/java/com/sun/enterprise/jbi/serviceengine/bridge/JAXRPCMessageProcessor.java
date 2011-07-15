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

package com.sun.enterprise.jbi.serviceengine.bridge;
import com.sun.enterprise.jbi.serviceengine.ServiceEngineException;
import com.sun.enterprise.jbi.serviceengine.util.soap.MessageExchangeHelper;
import com.sun.enterprise.jbi.serviceengine.comm.MessageProcessor;
import com.sun.logging.LogDomains;
//import org.glassfish.webservices.Ejb2RuntimeEndpointInfo;
//import org.glassfish.webservices.EjbRuntimeEndpointInfo;
//import com.sun.xml.rpc.spi.JaxRpcObjectFactory;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jbi.messaging.MessageExchange;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPMessage;

/**
 * Process a MEP for a new incoming message from NMR.
 * Processing always happen in a new thread.
 *
 * @author Binod PG.
 */
public class JAXRPCMessageProcessor extends MessageProcessor {
    protected static final Logger logger =
            LogDomains.getLogger(JAXRPCMessageProcessor.class, LogDomains.EJB_LOGGER);
    //TODO: commented this and the ejb request.
    //JaxRpcObjectFactory rpcFactory = JaxRpcObjectFactory.newInstance();

    /**
     * MessageAcceptor uses this method to start processing the MEP.
     */
    public void process() {
        execute();
    }
    
    /**
     * Actual code that starts processing the MEP.
     */
    public void doWork() {
        try {
            // Add code here to process the received message.
            MessageExchange me = getMessageExchange();
            String endpoint = me.getEndpoint().getEndpointName();
            QName service = me.getEndpoint().getServiceName();
            SOAPMessage response = null;
            MessageExchangeHelper meHelper = new MessageExchangeHelper();
            meHelper.setMessageExchange(me);
            try {
                debug(Level.FINEST,"serviceengine.process_incoming_request",
                        new Object[]{service.getLocalPart(), endpoint});
                        
               /*
                 EjbRuntimeEndpointInfo runtimeEndpointInfo =
                    (EjbRuntimeEndpointInfo)RuntimeEndpointInfoRegistryImpl.getInstance().
                    getRuntimeEndpointInfo(service, endpoint);
                 
                 response = processEJBRequest(
                         meHelper.denormalizeMessage(true), runtimeEndpointInfo);
                */
            } catch(Throwable e) {
                logger.log(Level.SEVERE, "serviceengine.error_incoming_request", e);
                ServiceEngineException seException = new ServiceEngineException(e);
                meHelper.handleException(seException);
            }
            meHelper.handleResponse(response, false);
        } catch(Exception e) {
            logger.log(Level.SEVERE, "JavaEEServiceEngine : Error processing request" + e  , e);
        }
    }
    
    private void debug(Level logLevel, String msgID, Object[] params) {
        logger.log(logLevel, msgID, params);
    }

    /*
    private SOAPMessage processEJBRequest(SOAPMessage message,
            EjbRuntimeEndpointInfo endpointInfo) {
        
        if (message != null) {
            Container container = endpointInfo.getContainer();
            SOAPMessageContext msgContext = rpcFactory.createSOAPMessageContext();
            // Set context class loader to application class loader
            container.externalPreInvoke();
            msgContext.setMessage(message);
            
            try {
                // Do ejb container pre-invocation and pre-handler
                // logic
                Handler implementor = ((Ejb2RuntimeEndpointInfo) endpointInfo).getHandlerImplementor(msgContext);
                
                // Pass control back to jaxrpc runtime to invoke
                // any handlers and call the webservice method itself,
                // which will be flow back into the ejb container.
                implementor.handle(msgContext);
               
                SOAPMessage reply = msgContext.getMessage();
                
                if (reply.saveRequired()) {
                    reply.saveChanges();
                }
                return reply;
                
            } catch(Exception e) {
                logger.fine(e.getMessage());
            } finally {
                
                // Always call release, even if an error happened
                // during getImplementor(), since some of the
                // preInvoke steps might have occurred.  It's ok
                // if implementor is null.
                endpointInfo.releaseImplementor();
                // Restore context class loader
                container.externalPostInvoke();
            }
        } else {
            String errorMsg = "null message POSTed to ejb endpoint " +
                    endpointInfo.getEndpoint().getEndpointName() +
                    " at " + endpointInfo.getEndpointAddressUri();
            logger.severe(errorMsg);
        }
        
        return null;
    }*/
}


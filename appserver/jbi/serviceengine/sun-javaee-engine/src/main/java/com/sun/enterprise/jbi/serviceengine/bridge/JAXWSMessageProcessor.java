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
import com.sun.enterprise.jbi.serviceengine.core.EndpointRegistry;
import com.sun.enterprise.jbi.serviceengine.core.DescriptorEndpointInfo;
import com.sun.enterprise.jbi.serviceengine.bridge.transport.JBIAdapter;
import com.sun.enterprise.jbi.serviceengine.comm.MessageProcessor;

import javax.jbi.messaging.MessageExchange;
import javax.xml.namespace.QName;
import java.util.logging.Level;

/**
 * Process new incoming messages from NMR and pass it to the JAX-WS layer for
 * endpoint invocations.
 * Processing always happen in a new thread.
 *
 * @author Vikas Awasthi
 */
public class JAXWSMessageProcessor extends MessageProcessor {
    
    /**  MessageAcceptor uses this method to start processing the MEP. */
    public void process() {
        execute();
    }
    
    /** Actual code that starts processing the MEP. */
    public void doWork() {
        try {
            // Add code here to process the received message.
            MessageExchange me = getMessageExchange();
            String endpointName = me.getEndpoint().getEndpointName();
            QName service = me.getEndpoint().getServiceName();

            String key = DescriptorEndpointInfo.getDEIKey(service, endpointName);
            DescriptorEndpointInfo dei = EndpointRegistry.getInstance().getWSDLEndpts().get(key);
            if(dei != null) {
                service = dei.getServiceName();
                endpointName = dei.getEndpointName();
            }
            JBIAdapterBuilder builder = new JBIAdapterBuilder();
            JBIAdapter jbiAdapter = null;
            
            try {
                debug(Level.FINE,"serviceengine.process_incoming_request",
                        new Object[]{service.getLocalPart(), endpointName});
                
                jbiAdapter = builder.createAdapter(service,endpointName,me);
                
                Thread curThread = Thread.currentThread();
                if (jbiAdapter != null) {
                    ClassLoader currentLoader =
                            curThread.getContextClassLoader();
                    try {
                        // do we need to perform security checks here?
                        curThread.setContextClassLoader(jbiAdapter.getClassLoader());
                        // call to invoke the endpoint
                        jbiAdapter.handle();
                    } finally {
                        curThread.setContextClassLoader(currentLoader);
                    }
                } else
                    logger.log(Level.WARNING, "Endpoint ["+endpointName+"] not registered");
                
                debug(Level.FINE,"serviceengine.success_incoming_request",
                        new Object[]{service.getLocalPart(), endpointName});
                
            } catch(Throwable e) {
                logger.log(Level.SEVERE, "serviceengine.error_incoming_request", e);
                ServiceEngineException seException = new ServiceEngineException(e);
                if(jbiAdapter != null) {
                    jbiAdapter.handleException(seException);
                }
            }
        } catch(Exception e) {
            logger.log(Level.SEVERE, "JavaEEServiceEngine : Error processing request" + e  , e);
        }
    }
    
    private void debug(Level logLevel, String msgID, Object[] params) {
        logger.log(logLevel, msgID, params);
    }
}

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

package com.sun.enterprise.jbi.serviceengine.bridge.transport;

import com.sun.enterprise.jbi.serviceengine.core.ServiceEngineEndpoint;
//import org.glassfish.webservices.EjbRuntimeEndpointInfo;
import com.sun.enterprise.jbi.serviceengine.core.ServiceEngineRuntimeHelper;
import com.sun.enterprise.web.WebComponentInvocation;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.server.Adapter;
import com.sun.xml.ws.api.server.TransportBackChannel;
import com.sun.xml.ws.api.server.WSEndpoint;
import com.sun.logging.LogDomains;

import java.util.logging.Logger;
import java.util.logging.Level;
import javax.jbi.messaging.MessageExchange;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.webservices.AdapterInvocationInfo;
import org.glassfish.webservices.EjbRuntimeEndpointInfo;

/**
 * Adapter class that invokes the Endpoint. It uses an inner class to handle
 * the inbound request.
 *
 * @author Vikas Awasthi
 */
public class JBIAdapter extends Adapter<JBIAdapter.WSToolkit> {
    
    private final Logger logger = LogDomains.getLogger(JBIAdapter.class, LogDomains.SERVER_LOGGER);
    private NMRServerConnection con;
    private ClassLoader classLoader;
    private EjbRuntimeEndpointInfo ejbEndPtInfo;
    private AdapterInvocationInfo adapterInvocationInfo;
    private WebComponentInvocation inv;
    
    /**
     * Creates an {@link com.sun.xml.ws.api.server.Adapter} that delivers
     * messages to the given endpoint.
     */
    public JBIAdapter(WSEndpoint endpoint, 
                      ServiceEngineEndpoint endpt, 
                      MessageExchange me,
                      ClassLoader classLoader
                      ,EjbRuntimeEndpointInfo ejbEndPtInfo,
                      AdapterInvocationInfo adapterInvocationInfo,
                      WebComponentInvocation inv) {
        super(endpoint);
        con = new NMRServerConnection(me, endpt);
        this.classLoader = classLoader;
        this.ejbEndPtInfo = ejbEndPtInfo;
        this.adapterInvocationInfo = adapterInvocationInfo;
        this.inv = inv;
    }
    
    protected WSToolkit createToolkit() {
        return new WSToolkit();
    }

    public void handle() {
        WSToolkit tk = pool.take();
        try {
            tk.handle();
        }finally {
            pool.recycle(tk);
        }
    }

    public void handleException(Exception ex) {
        con.handleException(ex);
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    /** For every endpoint invocation a preInvoke and postInvoke must be
     * called. The preInvoke is called during the creation of JBIAdapter in  
     * JBIAdapterBuilder. The postInvoke method should be called before 
     * returning the response back to NMR. 
     * For non-EJB cases ejbEndPtInfo will be null. */
    private void postInvoke() {
        if(ejbEndPtInfo!=null && adapterInvocationInfo != null) {
            ejbEndPtInfo.releaseImplementor(adapterInvocationInfo.getInv());
        } else if(inv != null){
            //for non-EJB endpoints.
            InvocationManager invocationMgr =
                    ServiceEngineRuntimeHelper.getRuntime().getInvocationManager();
            invocationMgr.postInvoke(inv);
        }

    }
    
    final class WSToolkit extends Adapter.Toolkit implements TransportBackChannel {
        
        public void handle() {
            Packet packet = con.receiveRequest();
            try {
                packet = head.process(packet,con,this);
            } catch(Exception e) {
                logger.log(Level.SEVERE,"Exception in invoking the service:"
                        + e.getMessage());
                postInvoke();
                handleException(e);
                return;
            }
            
            postInvoke();
            
            try {
                con.sendResponse(packet);
            } catch(Exception e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
                handleException(e);
            }
        }

        /** This method is called during the execution of head.process() before 
         * the endpoint is invoked. This callback is used in HTTPAdapter to 
         * send back the response to the client immediately.
         * With JBI there is no need to use this method. */
        public void close() {
        }
        
    }

}

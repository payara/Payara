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

/*
 * EndpointImpl.java
 *
 * Created on March 14, 2005, 10:35 AM
 */

package org.glassfish.webservices.monitoring;

import java.util.List;
import java.util.ArrayList;

import com.sun.enterprise.deployment.WebServiceEndpoint;


/**
 * Implementation of the endpoint interface
 *
 * @author Jerome Dochez
 */
public class EndpointImpl implements Endpoint {
    
    public final static String NAME = "MONITORING_ENDPOINT";
    public final static String MESSAGE_ID = "MONITORING_MESSAGE_ID";
    public final static String REQUEST_TRACE = "MONITORING_REQUEST_MESSAGE_TRACE";
    
    final String endpointSelector;
    final EndpointType type;
    WebServiceEndpoint endpointDesc;
    List<MessageListener> listeners = new ArrayList<MessageListener>();
    
    /** Creates a new instance of EndpointImpl */
    EndpointImpl(String endpointSelector, EndpointType type) {
        this.endpointSelector = endpointSelector;
        this.type = type;
    }
    
    /** 
     * @return the endpoint URL as a string. This is the URL
     * web service clients use to invoke the endpoint.
     */
    public String getEndpointSelector() {        
        return endpointSelector;
    }
        
    /**
     * @return the endpoint type
     */
    public EndpointType getEndpointType() {
        return type;
    }
    
    /**
     * Returns the Transport type 
     */
    public TransportType getTransport() {
        return TransportType.HTTP;
    }
    
    /**
     * registers a new SOAPMessageListener for this endpoint
     * @param  newListener instance to register.
     */
    public void addListener(MessageListener newListener) {
        listeners.add(newListener);
    }
    
    /**
     * unregiters a SOAPMessageListener for this endpoint
     * @param  listener instance to unregister.
     */
    public void removeListener(MessageListener listener) {
        listeners.remove(listener);
    }
    
    /** 
     * Returns true if this endpoint has listeners registered
     * @return true if at least one listener is registered
     */
    public boolean hasListeners() {
        return !listeners.isEmpty();
    }
    
    /**
     * Return the deployment descriptors associated with this 
     * endpoint.
     */
    public WebServiceEndpoint getDescriptor() {
        return endpointDesc;
    }
    
    /**
     * Set the WebServiceEndpoint DOL descriptor
     */
    public void setDescriptor(WebServiceEndpoint endpointDesc) {
        
        if (endpointDesc!=null) {
            endpointDesc.addExtraAttribute(EndpointImpl.NAME, this);        
        }
        this.endpointDesc = endpointDesc;
    }    
}

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

package com.sun.enterprise.jbi.serviceengine.component;

import javax.jbi.component.*;
import javax.jbi.servicedesc.ServiceEndpoint;
import org.w3c.dom.Document;
import com.sun.enterprise.jbi.serviceengine.core.*;

/** 
 * Represents JSR 208 compliant service engine which provides beidge beteween
 * SJS AS containers and JBI environment.
 * @author Manisha Umbarje
 */
        
public class JavaEEServiceEngine implements Component {
  
    JavaEEServiceEngineLifeCycle lifeCycle = null;
    JavaEEServiceEngineSUManager serviceUnitManager = null;
    
    public JavaEEServiceEngine() {
    }

    /**
     * Get the required life cycle control implementation for this component.
     * @return the life cycle control implementation for this component.
     */
    public ComponentLifeCycle getLifeCycle(){
        
        if (lifeCycle == null) {
            lifeCycle = new JavaEEServiceEngineLifeCycle();
        }
        return lifeCycle;
    }

    /**
     * Get the Service Unit manager for this component. If this component
     * does not support deployments, return <code>null</code>.
     * @return the Service Unit manager for this component, or <code>null</code>
     * if there is none.
     */
    public ServiceUnitManager getServiceUnitManager(){
        if(serviceUnitManager == null)
            serviceUnitManager = new JavaEEServiceEngineSUManager();
        return serviceUnitManager;
    }

    /**
     * Retrieves a DOM representation containing metadata which describes the 
     * service provided by this component, through the given endpoint. The 
     * result can use WSDL 1.1 or WSDL 2.0.
     * @param endpoint the endpoint to be described.
     * @return the description for the specified endpoint.
     */
    public Document getServiceDescription(ServiceEndpoint endpoint){
        
        //return bridge.getServiceDescription(endpoint);
        return null;
    }
    
    /** This method is called by JBI to check if this component, in the role of
     *  provider of the service indicated by the given exchange, can actually 
     *  perform the operation desired. The consumer is described by the given 
     *  capabilities, and JBI has already ensured that a fit exists between the 
     *  set of required capabilities of the provider and the available 
     *  capabilities of the consumer, and vice versa. This matching consists of
     *  simple set matching based on capability names only. <br><br>
     *  Note that JBI assures matches on capability names only; it is the 
     *  responsibility of this method to examine capability values to ensure a 
     *  match with the consumer.
     *  @param endpoint the endpoint to be used by the consumer
     *  @param exchange the proposed message exchange to be performed
     *  @param consumerCapabilities the consumer?s capabilities and requirements
     *  @return true if this provider component can perform the the given 
     *   exchange with the described consumer
     */
    public boolean isExchangeWithConsumerOkay(
        javax.jbi.servicedesc.ServiceEndpoint endpoint,
        javax.jbi.messaging.MessageExchange exchange)
    {        
        return true;
    }
    
    /** This method is called by JBI to check if this component, in the role of
     *  consumer of the service indicated by the given exchange, can actually 
     *  interact with the the provider completely. Ths provider is described 
     *  by the given capabilities, and JBI has already ensure that a fit exists 
     *  between the set of required capabilities of the consumer and the 
     *  available capabilities of the provider, and vice versa. This matching 
     *  consists of simple set matching based on capability names only. <br><br>
     *  Note that JBI assures matches on capability names only; it is the 
     *  responsibility of this method to examine capability values to ensure a 
     *  match with the provider.
     *  @param exchange the proposed message exchange to be performed
     *  @param providerCapabilities the provider's capabilities and requirements
     *  @return true if this consurer component can interact with the described
     *   provider to perform the given exchange
     */
    public boolean isExchangeWithProviderOkay(
        javax.jbi.servicedesc.ServiceEndpoint endpoint,
        javax.jbi.messaging.MessageExchange exchange)
    {
        return true;
    }
    
    public ServiceEndpoint resolveEndpointReference(
            org.w3c.dom.DocumentFragment epr) {
	return null;
    }
    
}


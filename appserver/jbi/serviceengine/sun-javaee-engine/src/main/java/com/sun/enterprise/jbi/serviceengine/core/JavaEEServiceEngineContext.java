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

package com.sun.enterprise.jbi.serviceengine.core;
import com.sun.enterprise.jbi.serviceengine.ServiceEngineException;
import com.sun.enterprise.jbi.serviceengine.config.ComponentConfiguration;
import com.sun.enterprise.jbi.serviceengine.work.WorkManagerImpl;
import com.sun.logging.LogDomains;
import java.util.Iterator;
import javax.jbi.JBIException;
import javax.jbi.component.ComponentContext;
import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.MessagingException;
import javax.jbi.servicedesc.ServiceEndpoint;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Wrapper for a JBI Context object, provides utility methods
 * @author Manisha Umbarje
 */
public class JavaEEServiceEngineContext {
    
    private ComponentContext jbiContext;
    
    private EndpointRegistry endpointRegistry;
    
    private DeliveryChannel seDeliveryChannel;
    
    private WorkManagerImpl workManager;
    
    private ComponentConfiguration config ;
    
    private Bridge bridge;
    
    private static JavaEEServiceEngineContext serviceEngineContext = 
            new JavaEEServiceEngineContext();
    /**
     * Internal handle to the logger instance
     */
    protected static final Logger logger =
        LogDomains.getLogger(JavaEEServiceEngineContext.class, LogDomains.SERVER_LOGGER);

            
    /** Creates a new instance of JavaEEServiceEngineContext */
    private JavaEEServiceEngineContext()  {
    }
    
    public static JavaEEServiceEngineContext getInstance() {
        return serviceEngineContext;
    }
    
    public void initialize() throws ServiceEngineException {
        endpointRegistry = EndpointRegistry.getInstance();
        config = new ComponentConfiguration();
        workManager = new WorkManagerImpl(config);
        bridge = config.getBridge();
        bridge.initialize();
    }
    /**
     * Returns the context provided by the JBI environment  to the 
     * service engine
     */
    public ComponentContext getJBIContext() {
        return jbiContext;
    }
    
    public DeliveryChannel getDeliveryChannel() {
        return seDeliveryChannel;
    }
    
    public WorkManagerImpl getWorkManager() {
        return workManager;
    }
    
    public Bridge getBridge() {
        return bridge;
    }
    /**
     *
     */
    public void setJBIContext(ComponentContext context)
    throws MessagingException {
        jbiContext = context;
        seDeliveryChannel = jbiContext.getDeliveryChannel();
        debug(Level.FINE, "Delivery Channel is : " + seDeliveryChannel);
    }
    
    /**
     * Gets the ServiceEndpoint Registry
     */
    public EndpointRegistry getRegistry() {
        return endpointRegistry;
    }
    
    /**
     * Activates multiple end points in JBI
     * @param endpoints list of end points to be activated in JBI
     */
    public void activateEndpoints(Iterator endpoints) 
    throws  JBIException {
        if (endpoints != null) {
            while(endpoints.hasNext()) {
                ServiceEndpoint endpoint = (ServiceEndpoint)endpoints.next();
                jbiContext.activateEndpoint(
                        endpoint.getServiceName(), endpoint.getEndpointName());
            }
        }
        
    }
    
    private void debug(Level logLevel, String msgID) {
        logger.log(logLevel, msgID);
    }
    
    public boolean isSunESB() {
         /*
         if(jbiContext == null ||
             !"com.sun.jbi.framework.ComponentContext".equals(jbiContext.getClass().getName())) {
             return false;
         }
          */
        
        String ESBRuntimeProp = System.getProperty("com.sun.enterprise.jbi.se.esbruntime");
        if(ESBRuntimeProp != null && !ESBRuntimeProp.equalsIgnoreCase("sunesb")) {
            return false;
        }
        return true;
    }
    
    public boolean isServiceMix() {
        String ESBRuntimeProp = System.getProperty("com.sun.enterprise.jbi.se.esbruntime");
        if(ESBRuntimeProp != null && ESBRuntimeProp.equalsIgnoreCase("servicemix")) {
            return true;
        }
        return false;
    }
    
}

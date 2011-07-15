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

import com.sun.enterprise.deployment.ServiceRefPortInfo;
import com.sun.enterprise.deployment.ServiceReferenceDescriptor;
import com.sun.logging.LogDomains;

import javax.xml.namespace.QName;
import java.util.logging.Logger;
import java.util.logging.Level;
/**
 * If web service client is JBI enabled, transport factory is set to NMR, otherwise
 * ususal JAXWS stack is used.
 * <p><b>Not thread-safe</b>
 * @author Manisha Umbarje
 */
public class ServiceEngineUtil {
    
    public static final String JBI_ENABLED = "jbi-enabled";
    private static final String seDisablePropertyValue =
                        System.getProperty("com.sun.enterprise.jbi.se.disable");
    private static final boolean seEnabledFlag =
                                    !("true".equals(seDisablePropertyValue));
    private static final Logger logger =
            LogDomains.getLogger(ServiceEngineUtil.class, LogDomains.SERVER_LOGGER);

    public static boolean isServiceJBIEnabled(ServiceReferenceDescriptor desc) {
        if(isServiceEngineEnabled()) {
            java.util.Set portsInfo = desc.getPortsInfo();
            java.util.Iterator ports = portsInfo.iterator();
            while(ports.hasNext()) {
                ServiceRefPortInfo portDesc = (ServiceRefPortInfo)ports.next();
                if(isPortJbiEnabled(portDesc))
                    return true;
            }
        }
        return false;
    }
    
    public static void setJBITransportFactory(ServiceRefPortInfo portInfo,
            Object stubObj, boolean jbiEnabled) {
        if(isServiceEngineEnabled()) {
            ServiceReferenceDescriptor serviceRef = portInfo.getServiceReference();
            if(serviceRef != null && stubObj != null) {
                if (isServiceEngineEnabled() && 
                        jbiEnabled && 
                        serviceRef.getMappingFileUri() != null) {
                     setTransportFactory((com.sun.xml.rpc.spi.runtime.StubBase)stubObj, portInfo);
                }
                
            }
        } else {
            logger.log(Level.INFO, "Java EE Service Engine's functionality is disabled");
        }
        
    }
    
     public static boolean isServiceEngineEnabled() {
        return seEnabledFlag;
    }

    //Getting serviceref from sun-web.xml is not planned to be supported in v3
    /*
    public static ServiceRefPortInfo getPortInfo(WSClientContainer container, 
                                                 QName portName) {
        return container.svcRef.getPortInfoByPort(portName);
    }
    */

    public static boolean isJBIRequest(String delegateClassName) {
        return delegateClassName.equals("com.sun.enterprise.jbi.serviceengine." +
                                        "bridge.transport.NMRServerConnection");
    }
    
    private static boolean isPortJbiEnabled(ServiceRefPortInfo portInfo) {
        if(portInfo != null) {
            String value = portInfo.getStubPropertyValue(JBI_ENABLED);
            logger.log(Level.FINEST, "JBI_ENABLED flag value is : " + value);
            return "true".equals(value);
        } else {
            // This means the deployer did not resolve the port to
            // which this SEI is mapped, return false;
        }
        return false;
    }
    
    private static void setTransportFactory(
            com.sun.xml.rpc.spi.runtime.StubBase stubObj, ServiceRefPortInfo portInfo) {
        try {
            // This is done to avoide classloader issues.
            // Check out ServiceEngineRtObjectFactory for more details.
            com.sun.xml.rpc.spi.runtime.ClientTransportFactory factory =
                    (com.sun.xml.rpc.spi.runtime.ClientTransportFactory)
                    ServiceEngineRtObjectFactory.getInstance().
                    getFacade().getTransportFactory(portInfo, true);
            if (factory == null)
                return ;
            
            logger.log(Level.INFO, "Before setting setTransportFactory to NMR");
            // Set JBI transport factory
            stubObj._setTransportFactory(factory);
            
        } catch(Exception e) {
            // Do nothing.
            if (logger.isLoggable(Level.WARNING)) {
                logger.log(Level.WARNING,
                        "Error during setting of transport factory"+e.getMessage());
            }
        }
    }
}

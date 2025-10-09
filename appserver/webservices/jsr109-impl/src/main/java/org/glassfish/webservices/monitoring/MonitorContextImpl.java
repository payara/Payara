/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2019] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/main/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 * 
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 * 
 *     GPL Classpath Exception:
 *     The Payara Foundation designates this particular file as subject to the "Classpath"
 *     exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *     file that accompanied this code.
 * 
 *     Modifications:
 *     If applicable, add the following below the License Header, with the fields
 *     enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyright [year] [name of copyright owner]"
 * 
 *     Contributor(s):
 *     If you wish your version of this file to be governed by only the CDDL or
 *     only the GPL Version 2, indicate your decision by adding "[Contributor]
 *     elects to include this software in this distribution under the [CDDL or GPL
 *     Version 2] license."  If you don't indicate a single choice of license, a
 *     recipient has the option to distribute your version of this file under
 *     either the CDDL, the GPL Version 2 or to extend the choice of license to
 *     its licensees as provided above.  However, if you add GPL Version 2 code
 *     and therefore, elected the GPL Version 2 license, then the option applies
 *     only if the new code is made subject to such option by the copyright
 *     holder.
 */
package org.glassfish.webservices.monitoring;

import com.oracle.webservices.api.databinding.JavaCallInfo;
import com.sun.enterprise.deployment.WebServiceEndpoint;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.api.server.WSEndpoint;
import com.sun.xml.ws.model.SOAPSEIModel;

public class MonitorContextImpl implements MonitorContext {
   
    private final JavaCallInfo callInfo;
    private final SOAPSEIModel seiModel;
    private final WSDLPort wsdlModel;
    private final WSEndpoint<?> ownerEndpoint;
    private final WebServiceEndpoint endpoint;
    
    public MonitorContextImpl(JavaCallInfo callInfo, SOAPSEIModel seiModel, WSDLPort wsdlModel, WSEndpoint<?> ownerEndpoint, WebServiceEndpoint endpoint) {
        this.callInfo = callInfo;
        this.seiModel = seiModel;
        this.wsdlModel = wsdlModel;
        this.ownerEndpoint = ownerEndpoint;
        this.endpoint = endpoint;
    }
    
    @Override
    public Class<?> getImplementationClass() {
        String className;
        
        if (endpoint.getEjbComponentImpl() != null) {
           className = endpoint.getEjbComponentImpl().getEjbClassName();
        } else if (endpoint.hasServletImplClass()) {
            className = endpoint.getServletImplClass();
        } else {
            className = endpoint.getWebComponentImpl().getWebComponentImplementation();
        }
        
        try {
            return Thread.currentThread()
                         .getContextClassLoader()
                         .loadClass(className);
        } catch (Exception e) {
            // Ignore, calling code will have to handle null
        }
        
        return null;
    }

    @Override
    public JavaCallInfo getCallInfo() {
        return callInfo;
    }

    @Override
    public SOAPSEIModel getSeiModel() {
        return seiModel;
    }

    @Override
    public WSDLPort getWsdlModel() {
        return wsdlModel;
    }

    @Override
    public WSEndpoint<?> getOwnerEndpoint() {
        return ownerEndpoint;
    }

    @Override
    public WebServiceEndpoint getEndpoint() {
        return endpoint;
    }

}

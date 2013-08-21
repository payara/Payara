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

package org.glassfish.webservices;

import com.sun.enterprise.container.common.spi.util.ComponentEnvManager;
import com.sun.enterprise.deployment.JndiNameEnvironment;
import com.sun.enterprise.deployment.WebServicesDescriptor;
import com.sun.enterprise.deployment.WebServiceEndpoint;
import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.deployment.WebComponentDescriptor;

import org.glassfish.webservices.monitoring.JAXRPCEndpointImpl;

//TBD
//import com.sun.enterprise.security.jmac.provider.ServerAuthConfig;


// JAX-RPC SPI
import com.sun.xml.rpc.spi.JaxRpcObjectFactory;
import com.sun.xml.rpc.spi.runtime.ImplementorCache;
import com.sun.xml.rpc.spi.runtime.ImplementorCacheDelegate;
import com.sun.xml.rpc.spi.runtime.RuntimeEndpointInfo;
import com.sun.xml.rpc.spi.runtime.ServletDelegate;
import com.sun.xml.rpc.spi.runtime.ServletSecondDelegate;
import com.sun.xml.rpc.spi.runtime.SystemHandlerDelegate;
import java.text.MessageFormat;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.xml.ws.WebServiceException;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import org.glassfish.internal.api.Globals;
import org.glassfish.webservices.monitoring.WebServiceEngineImpl;

/**
 * This class is delegated to by the container-provided servlet-class 
 * that is written into the web.xml at deployment time.  It overrides
 * the JAXRPC servlet delegate to register endpoint information and
 * intercept certain events.
 */
public class ServletWebServiceDelegate extends ServletSecondDelegate {

    private static final Logger logger = LogUtils.getLogger();
    private WebServiceEndpoint endpoint_;

    private ServletDelegate rpcDelegate_;

    private JaxRpcObjectFactory rpcFactory_;

    private WebServiceEngineImpl wsEngine_;
    private JAXRPCEndpointImpl endpointImpl_;

    private org.glassfish.webservices.SecurityService  secServ;

    public ServletWebServiceDelegate (ServletDelegate firstDelegate) {
        rpcDelegate_ = firstDelegate;
        rpcFactory_ = JaxRpcObjectFactory.newInstance();
        wsEngine_ = WebServiceEngineImpl.getInstance();
        if (Globals.getDefaultHabitat() != null) {
            secServ = Globals.get(org.glassfish.webservices.SecurityService.class);
        }
    }

    @Override
    public void postInit(ServletConfig servletConfig) throws ServletException {

        String servletName = "unknown";

        try {
            WebServiceContractImpl wscImpl = WebServiceContractImpl.getInstance();
            ComponentEnvManager compEnvManager = wscImpl.getComponentEnvManager();
            JndiNameEnvironment jndiNameEnv = compEnvManager.getCurrentJndiNameEnvironment();
            WebBundleDescriptor webBundle = null;
            if (jndiNameEnv != null && jndiNameEnv instanceof WebBundleDescriptor){
                webBundle = ((WebBundleDescriptor)jndiNameEnv);
            } else {
               throw new WebServiceException("Cannot intialize the JAXRPCServlet for " + jndiNameEnv);
            }
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            servletName = servletConfig.getServletName();
            WebComponentDescriptor webComponent =
                    webBundle.getWebComponentByCanonicalName(servletName);
            if( webComponent != null ) {
                WebServicesDescriptor webServices = webBundle.getWebServices();
                Collection endpoints =
                        webServices.getEndpointsImplementedBy(webComponent);
                // Only 1 endpoint per servlet is supported, even though
                // data structure implies otherwise. 
                endpoint_ = (WebServiceEndpoint) endpoints.iterator().next();
                registerEndpoint(classLoader);

                // if a conventional authentication mechanism has NOT been configured
                // for the endpoint create and install system handler for web services
                // security
                if (secServ != null) {
                    SystemHandlerDelegate securityHandlerDelegate = secServ.getSecurityHandler(endpoint_);
                    if (securityHandlerDelegate != null) {
                        rpcDelegate_.setSystemHandlerDelegate(securityHandlerDelegate);

                        // need to invoke the endpoint lifecylcle
                        endpointImpl_ = JAXRPCEndpointImpl.class.cast(wsEngine_.createHandler(securityHandlerDelegate, endpoint_));
                        rpcDelegate_.setSystemHandlerDelegate(endpointImpl_);
                    }
                }

            } else {
                throw new ServletException(servletName + " not found");
            }
        } catch(Throwable t) {
            String msg = MessageFormat.format(
                    logger.getResourceBundle().getString(LogUtils.SERVLET_ENDPOINT_FAILURE),
                    servletName);
            logger.log(Level.WARNING, msg, t);
            throw new ServletException(t);
        }
    }

    public void destroy() {
        wsEngine_.removeHandler(endpoint_);
    }

    @Override
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response) throws ServletException {

        // normal WSDL retrieval invocation
        WsUtil wsUtil = new WsUtil();
        try {
            wsUtil.handleGet(request, response, endpoint_);
        } catch(Exception e) {
            String msg = MessageFormat.format(
                    logger.getResourceBundle().getString(LogUtils.SERVLET_ENDPOINT_GET_ERROR),
                    endpoint_.getEndpointName());
            logger.log(Level.WARNING, msg, e);
        }
    }

    public  void doPost(HttpServletRequest request,
                        HttpServletResponse response) throws ServletException {

        rpcDelegate_.doPost(request, response);

    }

    @Override
    public void warnMissingContextInformation() {
        // context info not used within j2ee integration, so override
        // this method to prevent warning message
    }

    @Override
    public ImplementorCache createImplementorCache(ServletConfig sc) {
        ImplementorCache ic = rpcFactory_.createImplementorCache(sc);
        ImplementorCacheDelegate delegate =
                new ImplementorCacheDelegateImpl(sc);
        ic.setDelegate(delegate);
        return ic;
    }

    private void registerEndpoint(ClassLoader loader)
            throws Exception {

        //
        // Convert J2EE deployment descriptor information into 
        // JAXRPC endpoint data structure
        //

        RuntimeEndpointInfo endpointInfo =
                rpcFactory_.createRuntimeEndpointInfo();

        Class serviceEndpointInterfaceClass =
                loader.loadClass(endpoint_.getServiceEndpointInterface());
        Class implementationClass =
                loader.loadClass(endpoint_.getServletImplClass());
        String tieClassName = endpoint_.getTieClassName();
        if(tieClassName != null) {
            Class tieClass = loader.loadClass(tieClassName);
            endpointInfo.setTieClass(tieClass);
        }

        endpointInfo.setRemoteInterface(serviceEndpointInterfaceClass);
        endpointInfo.setImplementationClass(implementationClass);

        endpointInfo.setName(endpoint_.getEndpointName());


        // No need to set model file name or wsdl file, since we override
        // the code that serves up the final WSDL.
        //endpointInfo.setModelFileName()
        //endpointInfo.setWSDLFileName()

        endpointInfo.setDeployed(true);
        endpointInfo.setPortName(endpoint_.getWsdlPort());

        endpointInfo.setServiceName(endpoint_.getServiceName());

        // For web components, this will be relative to the web app
        // context root.  Make sure there is a leading slash.
        String uri = endpoint_.getEndpointAddressUri();
        uri = uri.startsWith("/") ? uri : "/" + uri;
        endpointInfo.setUrlPattern(uri);

        rpcDelegate_.registerEndpointUrlPattern(endpointInfo);
    }
}

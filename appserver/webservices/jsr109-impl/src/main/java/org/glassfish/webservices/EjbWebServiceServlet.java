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


import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.WebServiceEndpoint;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.http.HTTPBinding;
import org.glassfish.api.logging.LogHelper;
import org.glassfish.webservices.monitoring.Endpoint;
import org.glassfish.webservices.monitoring.WebServiceEngineImpl;
import org.glassfish.webservices.monitoring.WebServiceTesterServlet;
import org.glassfish.ejb.api.EjbEndpointFacade;
import org.glassfish.ejb.spi.WSEjbEndpointRegistry;
import org.glassfish.internal.api.Globals;

/**
 * Servlet responsible for invoking EJB webservice endpoint.
 *
 * Most of this code used to be in
 * com.sun.enterprise.webservice.EjbWebServiceValve.
 *
 * @author	Qingqing Ouyang
 * @author	Kenneth Saks
 * @author	Jan Luehe
 */
public class EjbWebServiceServlet extends HttpServlet {

    private static final Logger logger = LogUtils.getLogger();

    private SecurityService secServ;

    public EjbWebServiceServlet() {
        super();
        if (Globals.getDefaultHabitat() != null) {
            secServ = Globals.get(SecurityService.class);
        }
    }

    @Override
    protected void service(HttpServletRequest hreq,
                           HttpServletResponse hresp)
            throws ServletException, IOException {

        String requestUriRaw = hreq.getRequestURI();
        String requestUri = (requestUriRaw.charAt(0) == '/') ?
                requestUriRaw.substring(1) : requestUriRaw;
        String query = hreq.getQueryString();
        
        WebServiceEjbEndpointRegistry wsejbEndpointRegistry =
                (WebServiceEjbEndpointRegistry) Globals.getDefaultHabitat()
                    .getService(WSEjbEndpointRegistry.class);
        EjbRuntimeEndpointInfo ejbEndpoint =
                wsejbEndpointRegistry.getEjbWebServiceEndpoint(requestUri, hreq.getMethod(), query);

        if (requestUri.contains(WebServiceEndpoint.PUBLISHING_SUBCONTEXT) && ejbEndpoint == null) {
            requestUri = requestUri.substring(0, requestUri.indexOf(WebServiceEndpoint.PUBLISHING_SUBCONTEXT) - 1);
            ejbEndpoint =
                wsejbEndpointRegistry.getEjbWebServiceEndpoint(requestUri, hreq.getMethod(), query);
        }

        if (ejbEndpoint != null) {
            /*
             * We can actually assert that ejbEndpoint is != null,
             * because this EjbWebServiceServlet would not have been
             * invoked otherwise
             */
            String scheme = hreq.getScheme();
            WebServiceEndpoint wse = ejbEndpoint.getEndpoint();
            if ("http".equals(scheme) && wse.isSecure()) {
                //redirect to correct protocol scheme if needed
                logger.log(Level.WARNING, LogUtils.INVALID_REQUEST_SCHEME,
                        new Object[]{wse.getEndpointName(), "https", scheme});
                URL url = wse.composeEndpointAddress(new WsUtil().getWebServerInfoForDAS().getWebServerRootURL(true));
                StringBuilder sb = new StringBuilder(url.toExternalForm());
                if (query != null && query.trim().length() > 0) {
                    sb.append("?");
                    sb.append(query);
                }
                hresp.sendRedirect(URLEncoder.encode(sb.toString(), "UTF-8"));
            } else {
                boolean dispatch = true;
                // check if it is a tester servlet invocation
                if ("Tester".equalsIgnoreCase(query) && (!(HTTPBinding.HTTP_BINDING.equals(wse.getProtocolBinding())))) {
                    Endpoint endpoint = WebServiceEngineImpl.getInstance().getEndpoint(hreq.getRequestURI());
                    if ((endpoint.getDescriptor().isSecure())
                            || (endpoint.getDescriptor().getMessageSecurityBinding() != null)) {
                        String message = endpoint.getDescriptor().getWebService().getName()
                                + "is a secured web service; Tester feature is not supported for secured services";
                        (new WsUtil()).writeInvalidMethodType(hresp, message);
                        return;
                    }
                    if (Boolean.parseBoolean(endpoint.getDescriptor().getDebugging())) {
                        dispatch = false;
                        WebServiceTesterServlet.invoke(hreq, hresp,
                                endpoint.getDescriptor());
                    }
                }
                if ("wsdl".equalsIgnoreCase(query) && (!(HTTPBinding.HTTP_BINDING.equals(wse.getProtocolBinding())))) {
                    if (wse.getWsdlExposed() != null && !Boolean.parseBoolean(wse.getWsdlExposed())) {
                        hresp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    }
                }
                if (dispatch) {
                    dispatchToEjbEndpoint(hreq, hresp, ejbEndpoint);
                }
            }
        } else {
            hresp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void dispatchToEjbEndpoint(HttpServletRequest hreq,
                                       HttpServletResponse hresp,
                                       EjbRuntimeEndpointInfo ejbEndpoint) {
        EjbEndpointFacade container = ejbEndpoint.getContainer();
        ClassLoader savedClassLoader = null;

        boolean authenticated = false;
        try {
            // Set context class loader to application class loader
            savedClassLoader = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(container.getEndpointClassLoader());

            // compute realmName
            String realmName = null;
            Application app = ejbEndpoint.getEndpoint().getBundleDescriptor().getApplication();
            if (app != null) {
                realmName = app.getRealm();
            }
            if (realmName == null) {
                realmName = ejbEndpoint.getEndpoint().getRealm();
            }

            if (realmName == null) {
                // use the same logic as BasicAuthenticator
                realmName = hreq.getServerName() + ":" + hreq.getServerPort();
            }

            try {
                if (secServ != null) {
                    WebServiceContextImpl context = (WebServiceContextImpl) ejbEndpoint.getWebServiceContext();
                    authenticated = secServ.doSecurity(hreq, ejbEndpoint, realmName, context);
                }

            } catch(Exception e) {
                //sendAuthenticationEvents(false, hreq.getRequestURI(), null);
                LogHelper.log(logger, Level.WARNING, LogUtils.AUTH_FAILED,
                        e, ejbEndpoint.getEndpoint().getEndpointName());
            }

            if (!authenticated) {
                hresp.setHeader("WWW-Authenticate",
                        "Basic realm=\"" + realmName + "\"");
                hresp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            // depending on the jaxrpc or jax-ws version, this will return the
            // right dispatcher.
            EjbMessageDispatcher msgDispatcher = ejbEndpoint.getMessageDispatcher();
            msgDispatcher.invoke(hreq, hresp, getServletContext(), ejbEndpoint);

        } catch(Throwable t) {
            logger.log(Level.WARNING, LogUtils.EXCEPTION_THROWN, t);
        } finally {
            // remove any security context from the thread local before returning
            if (secServ != null) {
                secServ.resetSecurityContext();
                secServ.resetPolicyContext();
            }
            // Restore context class loader
            Thread.currentThread().setContextClassLoader(savedClassLoader);
        }
    }
}

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
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2018-2022] [Payara Foundation and/or its affiliates]
package com.sun.enterprise.security.webservices;

import static com.sun.enterprise.security.webservices.LogUtils.BASIC_AUTH_ERROR;
import static com.sun.enterprise.security.webservices.LogUtils.CLIENT_CERT_ERROR;
import static com.sun.enterprise.security.webservices.LogUtils.EJB_SEC_CONFIG_FAILURE;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;
import static org.apache.catalina.Globals.CERTIFICATES_ATTR;
import static org.apache.catalina.Globals.SSL_CERTIFICATE_ATTR;

import java.lang.ref.WeakReference;
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.security.jacc.PolicyContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.xml.soap.SOAPMessage;

import org.apache.catalina.util.Base64;
import org.glassfish.webservices.EjbRuntimeEndpointInfo;
import org.glassfish.webservices.SecurityService;
import org.glassfish.webservices.WebServiceContextImpl;
import org.glassfish.webservices.monitoring.AuthenticationListener;
import org.glassfish.webservices.monitoring.Endpoint;
import org.glassfish.webservices.monitoring.WebServiceEngineImpl;
import org.jvnet.hk2.annotations.Service;

import com.sun.enterprise.deployment.ServiceReferenceDescriptor;
import com.sun.enterprise.deployment.WebServiceEndpoint;
import com.sun.enterprise.deployment.runtime.common.MessageSecurityBindingDescriptor;
import com.sun.enterprise.security.SecurityContext;
import com.sun.enterprise.security.ee.audit.AppServerAuditManager;
import com.sun.enterprise.security.jacc.context.PolicyContextHandlerImpl;
import com.sun.enterprise.security.jauth.AuthConfig;
import com.sun.enterprise.security.jauth.jaspic.provider.ServerAuthConfig;
import com.sun.enterprise.security.web.integration.WebPrincipal;
import com.sun.enterprise.web.WebModule;
import com.sun.web.security.RealmAdapter;
import com.sun.xml.ws.assembler.metro.dev.ClientPipelineHook;

/**
 *
 * @author Kumar
 */
@Service
@Singleton
public class SecurityServiceImpl implements SecurityService {
    
    protected static final Logger _logger = LogUtils.getLogger();

    private static final String AUTHORIZATION_HEADER = "authorization";

    private static ThreadLocal<WeakReference<SOAPMessage>> req = new ThreadLocal<WeakReference<SOAPMessage>>();

    @Inject
    private AppServerAuditManager auditManager;

    @Override
    public Object mergeSOAPMessageSecurityPolicies(MessageSecurityBindingDescriptor desc) {
        try {
            // Merge message security policy from domain.xml and sun-specific
            // deployment descriptor
            return ServerAuthConfig.getConfig(AuthConfig.SOAP, desc, null);
        } catch (Exception ae) {
            _logger.log(SEVERE, EJB_SEC_CONFIG_FAILURE, ae);
        }
        return null;
    }

    @Override
    public boolean doSecurity(HttpServletRequest hreq, EjbRuntimeEndpointInfo epInfo, String realmName, WebServiceContextImpl context) {
        // BUG2263 - Clear the value of UserPrincipal from previous request
        // If authentication succeeds, the proper value will be set later in
        // this method.
        boolean authenticated = false;
        try {
            // calling this for a GET request WSDL query etc can cause problems
            String method = hreq.getMethod();

            if (context != null) {
                context.setUserPrincipal(null);
            }

            WebServiceEndpoint endpoint = epInfo.getEndpoint();

            String rawAuthInfo = hreq.getHeader(AUTHORIZATION_HEADER);
            if (method.equals("GET") || !endpoint.hasAuthMethod()) {
                authenticated = true;
                return true;
            }

            WebPrincipal webPrincipal = null;
            String endpointName = endpoint.getEndpointName();
            if (endpoint.hasBasicAuth() || rawAuthInfo != null) {
                if (rawAuthInfo == null) {
                    sendAuthenticationEvents(false, hreq.getRequestURI(), null);
                    authenticated = false;
                    return false;
                }

                List<Object> usernamePassword = parseUsernameAndPassword(rawAuthInfo);
                if (usernamePassword != null) {
                    webPrincipal = new WebPrincipal((String) usernamePassword.get(0), (char[]) usernamePassword.get(1),
                            SecurityContext.init());
                } else {
                    _logger.log(WARNING, BASIC_AUTH_ERROR, endpointName);
                }
            } else {
                // org.apache.coyote.request.X509Certificate
                X509Certificate certs[] = (X509Certificate[]) hreq.getAttribute(CERTIFICATES_ATTR);
                if ((certs == null) || (certs.length < 1)) {
                    certs = (X509Certificate[]) hreq.getAttribute(SSL_CERTIFICATE_ATTR);
                }

                if (certs != null) {
                    webPrincipal = new WebPrincipal(certs, SecurityContext.init());
                } else {
                    _logger.log(WARNING, CLIENT_CERT_ERROR, endpointName);
                }

            }

            if (webPrincipal == null) {
                sendAuthenticationEvents(false, hreq.getRequestURI(), null);
                return authenticated;
            }

            RealmAdapter realmAdapter = new RealmAdapter(realmName, endpoint.getBundleDescriptor().getModuleID());
            authenticated = realmAdapter.authenticate(webPrincipal);

            if (authenticated == false) {
                sendAuthenticationEvents(false, hreq.getRequestURI(), webPrincipal);
                if (_logger.isLoggable(FINE)) {
                    _logger.fine("authentication failed for " + endpointName);
                }
            } else {
                sendAuthenticationEvents(true, hreq.getRequestURI(), webPrincipal);
            }

            // Setting userPrincipal in WSCtxt applies for JAXWS endpoints only
            epInfo.prepareInvocation(false);
            WebServiceContextImpl ctxt = (WebServiceContextImpl) epInfo.getWebServiceContext();
            ctxt.setUserPrincipal(webPrincipal);

        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (auditManager != null && auditManager.isAuditOn()) {
                auditManager.ejbAsWebServiceInvocation(epInfo.getEndpoint().getEndpointName(), authenticated);
            }
        }
        return authenticated;
    }

    @Override
    public Principal getUserPrincipal(boolean isWeb) {
        // This is a servlet endpoint
        SecurityContext securityContext = SecurityContext.getCurrent();
        if (securityContext == null) {
            return null;
        }
        
        if (securityContext.didServerGenerateCredentials()) {
            if (isWeb) {
                return null;
            }
        }
        
        return securityContext.getCallerPrincipal();
    }

    @Override
    public boolean isUserInRole(WebModule webModule, Principal principal, String servletName, String role) {
        if (webModule.getRealm() instanceof RealmAdapter) {
            RealmAdapter realmAdapter = (RealmAdapter) webModule.getRealm();
            return realmAdapter.hasRole(servletName, principal, role);
        }
        
        return false;
    }

    @Override
    public void resetSecurityContext() {
        SecurityContext.setUnauthenticatedContext();
    }

    @Override
    public void resetPolicyContext() {
        ((PolicyContextHandlerImpl) PolicyContextHandlerImpl.getInstance()).reset();
        PolicyContext.setContextID(null);
    }

    @Override
    public ClientPipelineHook getClientPipelineHook(ServiceReferenceDescriptor ref) {
        return new ClientPipeCreator(ref);
    }


    private List<Object> parseUsernameAndPassword(String rawAuthInfo) {

        List usernamePassword = null;
        if ((rawAuthInfo != null) && (rawAuthInfo.startsWith("Basic "))) {
            String authString = rawAuthInfo.substring(6).trim();
            // Decode and parse the authorization credentials
            String unencoded = new String(Base64.decode(authString.getBytes()));
            int colon = unencoded.indexOf(':');
            if (colon > 0) {
                usernamePassword = new ArrayList();
                usernamePassword.add(unencoded.substring(0, colon).trim());
                usernamePassword.add(unencoded.substring(colon + 1).trim().toCharArray());
            }
        }
        return usernamePassword;
    }

    private void sendAuthenticationEvents(boolean success, String url, Principal principal) {

        Endpoint endpoint = WebServiceEngineImpl.getInstance().getEndpoint(url);
        if (endpoint == null) {
            return;
        }
        for (AuthenticationListener listener : WebServiceEngineImpl.getInstance().getAuthListeners()) {
            if (success) {
                listener.authSucess(endpoint.getDescriptor().getBundleDescriptor(), endpoint, principal);
            } else {
                listener.authFailure(endpoint.getDescriptor().getBundleDescriptor(), endpoint, principal);
            }
        }
    }
}

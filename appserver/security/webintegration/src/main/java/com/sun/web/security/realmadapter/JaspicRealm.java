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
 * https://glassfish.dev.java.net/public/CDDLGPL_1_1.html
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
// Portions Copyright [2016-2021] [Payara Foundation and/or its affiliates]
package com.sun.web.security.realmadapter;

import static com.sun.enterprise.security.jaspic.config.HttpServletConstants.AUTH_TYPE;
import static com.sun.enterprise.security.jaspic.config.HttpServletConstants.IS_MANDATORY;
import static com.sun.enterprise.security.jaspic.config.HttpServletConstants.REGISTER_SESSION;
import static com.sun.enterprise.security.jaspic.config.HttpServletConstants.REGISTER_WITH_AUTHENTICATOR;
import static com.sun.enterprise.security.jaspic.config.HttpServletConstants.WEB_BUNDLE;
import static com.sun.logging.LogDomains.WEB_LOGGER;
import static java.lang.Boolean.TRUE;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;
import static jakarta.security.auth.message.AuthStatus.SUCCESS;
import static jakarta.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static org.apache.catalina.ContainerEvent.AFTER_AUTHENTICATION;
import static org.apache.catalina.ContainerEvent.AFTER_LOGOUT;
import static org.apache.catalina.ContainerEvent.AFTER_POST_AUTHENTICATION;
import static org.apache.catalina.ContainerEvent.BEFORE_AUTHENTICATION;
import static org.apache.catalina.ContainerEvent.BEFORE_LOGOUT;
import static org.apache.catalina.ContainerEvent.BEFORE_POST_AUTHENTICATION;
import static org.apache.catalina.Globals.WRAPPED_REQUEST;
import static org.apache.catalina.Globals.WRAPPED_RESPONSE;

import java.io.IOException;
import java.security.Principal;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.logging.Logger;

import javax.security.auth.Subject;
import jakarta.security.auth.message.AuthException;
import jakarta.security.auth.message.MessageInfo;
import jakarta.security.auth.message.config.ServerAuthConfig;
import jakarta.security.auth.message.config.ServerAuthContext;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.catalina.Authenticator;
import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.HttpRequest;
import org.apache.catalina.HttpResponse;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.RequestFacade;
import org.apache.catalina.deploy.LoginConfig;

import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.security.SecurityContext;
import com.sun.enterprise.security.jaspic.config.PayaraJaspicServletServices;
import com.sun.enterprise.security.web.integration.WebPrincipal;
import com.sun.jaspic.config.servlet.HttpMessageInfo;
import com.sun.logging.LogDomains;
import com.sun.web.security.HttpRequestWrapper;
import com.sun.web.security.HttpResponseWrapper;
import com.sun.web.security.RealmAdapter;
import com.sun.web.security.RealmAdapter.IOSupplier;

import fish.payara.notification.requesttracing.RequestTraceSpan;
import fish.payara.nucleus.requesttracing.RequestTracingService;

public class JaspicRealm {

    private static final Logger logger = LogDomains.getLogger(RealmAdapter.class, WEB_LOGGER);

    /**
     * name of system property that can be used to define corresponding default provider for system apps.
     */
    private static final String SYSTEM_HTTPSERVLET_SECURITY_PROVIDER = "system_httpservlet_security_provider";
    private static final String SERVER_AUTH_CONTEXT = "__jakarta.security.auth.message.ServerAuthContext";
    private static final String MESSAGE_INFO = "__jakarta.security.auth.message.MessageInfo";

    /**
     * The default JASPIC config provider for system apps if one has been set via a system property.
     * This JASPIC config provider is used to obtain references to the SAM (authentication mechanism).
     */
    private static String jaspicSystemConfigProviderID = getDefaultSystemProviderID();


    private String realmName;
    private boolean isSystemApp;
    private WebBundleDescriptor webDescriptor;
    private RequestTracingService requestTracing;

    private Container virtualServer;

    private PayaraJaspicServletServices jaspicServices;
    private AtomicBoolean initialised = new AtomicBoolean();

    public JaspicRealm(String realmName, boolean isSystemApp, WebBundleDescriptor webDescriptor, RequestTracingService requestTracing) {
        this.realmName = realmName;
        this.isSystemApp = isSystemApp;
        this.webDescriptor = webDescriptor;
        this.requestTracing = requestTracing;
    }

    public void setVirtualServer(Container virtualServer) {
        this.virtualServer = virtualServer;
    }

    // TODO: reexamine this after TP2
    public synchronized void initJaspicServices(ServletContext servletContext) {
        if (jaspicServices != null) {
            return;
        }

        jaspicServices = getConfigHelper(servletContext);
        initialised.set(true);
    }

    public boolean isInitialised() {
        return initialised.get();
    }

    public boolean isJaspicEnabled(ServletContext servletContext) {
        if (jaspicServices == null) {
            initJaspicServices(servletContext);
        }

        return isJaspicEnabled();
    }

    public boolean isJaspicEnabled() {
        return getServerAuthConfig() != null;
    }

    public boolean validateRequest(HttpRequest request, HttpResponse response, Context context, Authenticator authenticator, boolean calledFromAuthenticate, Function<HttpServletRequest, Boolean> isMandatoryFn) throws IOException {
        try {
            context.fireContainerEvent(BEFORE_AUTHENTICATION, null);

            // Get the WebPrincipal principal and add to the security context principals
            RequestFacade requestFacade = (RequestFacade) request.getRequest();
            setAdditionalPrincipalInContext(requestFacade);

            return validateRequest(getServerAuthConfig(), context, requestFacade, request, response, context.getLoginConfig(), authenticator, calledFromAuthenticate, isMandatoryFn);
        } finally {
            resetAdditionalPrincipalInContext();
            context.fireContainerEvent(AFTER_AUTHENTICATION, null);
        }
    }

    public boolean secureResponse(HttpRequest request, HttpResponse response, Context context) throws IOException {
        Entry<MessageInfo, ServerAuthContext> messageInfoEntry = null;

        try {
            messageInfoEntry = getMessageInfoFromRequest((HttpServletRequest) request.getRequest());
            if (messageInfoEntry != null) {
                try {
                    context.fireContainerEvent(BEFORE_POST_AUTHENTICATION, null);

                    ServerAuthContext serverAuthContext = messageInfoEntry.getValue();
                    MessageInfo messageInfo = messageInfoEntry.getKey();

                    return SUCCESS.equals(serverAuthContext.secureResponse(messageInfo, null));
                } finally {
                    context.fireContainerEvent(AFTER_POST_AUTHENTICATION, null);
                }
            }
        } catch (AuthException ex) {
            throw new IOException(ex);
        } finally {
            if (messageInfoEntry != null) {
                if (request instanceof HttpRequestWrapper) {
                    request.removeNote(WRAPPED_REQUEST);
                }
                if (response instanceof HttpResponseWrapper) {
                    request.removeNote(WRAPPED_RESPONSE);
                }
            }
        }

        return false;
    }

    public void cleanSubject(HttpRequest httpRequest) throws AuthException {
        MessageInfo messageInfo = (MessageInfo) httpRequest.getRequest().getAttribute(MESSAGE_INFO);

        if (messageInfo == null) {
            messageInfo = new HttpMessageInfo(
                    (HttpServletRequest) httpRequest.getRequest(),
                    (HttpServletResponse) httpRequest.getResponse().getResponse());
        }

        messageInfo.getMap().put(IS_MANDATORY, TRUE.toString());

        ServerAuthContext serverAuthContext = jaspicServices.getServerAuthContext(messageInfo, null);
        if (serverAuthContext != null) {

            // Check for the default/server-generated/unauthenticated security context.

            SecurityContext securityContext = SecurityContext.getCurrent();
            Subject subject = securityContext.didServerGenerateCredentials() ? new Subject() : securityContext.getSubject();

            if (subject == null) {
                subject = new Subject();
            }

            if (subject.isReadOnly()) {
                logger.log(WARNING, "Read-only subject found during logout processing");
            }

            try {
                httpRequest.getContext().fireContainerEvent(BEFORE_LOGOUT, null);

                serverAuthContext.cleanSubject(messageInfo, subject);
            } finally {
                httpRequest.getContext().fireContainerEvent(AFTER_LOGOUT, null);
            }
        }
    }

    public void destroy() {
        if (jaspicServices != null) {
            jaspicServices.disable();
        }
    }


    
    // ############################   Private methods ######################################

    
    private ServerAuthConfig getServerAuthConfig() {
        if (jaspicServices == null) {
            return null;
        }

        try {
            return jaspicServices.getServerAuthConfig();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private ServerAuthContext getServerAuthContext(MessageInfo messageInfo) throws AuthException {
        ServerAuthContext authContext = jaspicServices.getServerAuthContext(messageInfo, null); // null serviceSubject

        if (authContext == null) {
            throw new AuthException("null ServerAuthContext");
        }

        return authContext;
    }

    /**
     * This must be invoked after virtualServer is set.
     */
    private PayaraJaspicServletServices getConfigHelper(ServletContext servletContext) {
        Map<String, Object> map = new HashMap<>();
        map.put(WEB_BUNDLE, webDescriptor);

        return new PayaraJaspicServletServices(
                getAppContextID(servletContext), map, null, // null handler
                realmName, isSystemApp, jaspicSystemConfigProviderID);
    }

    /**
     * This must be invoked after virtualServer is set.
     */
    private String getAppContextID(ServletContext servletContext) {
        if (!servletContext.getVirtualServerName().equals(virtualServer.getName())) {
            // PAYARA-1261 downgrade log messages to INFO as users haven't got a problem
            logger.log(INFO, "Virtual server name from ServletContext: {0} differs from name from virtual.getName(): {1}",
                    new Object[] { servletContext.getVirtualServerName(), virtualServer.getName() });
        }

        if (!servletContext.getContextPath().equals(webDescriptor.getContextRoot())) {
            // PAYARA-1261 downgrade log messages to INFO as users haven't got a problem
            logger.log(INFO, "Context path from ServletContext: {0} differs from path from bundle: {1}",
                    new Object[] { servletContext.getContextPath(), webDescriptor.getContextRoot() });
        }

        return servletContext.getVirtualServerName() + " " + servletContext.getContextPath();
    }

    /**
     * get the default provider id for system apps if one has been established. the default provider for system apps is
     * established by defining a system property.
     *
     * @return the provider id or null.
     */
    private static String getDefaultSystemProviderID() {
        String p = System.getProperty(SYSTEM_HTTPSERVLET_SECURITY_PROVIDER);
        if (p != null) {
            p = p.trim();
            if (p.length() == 0) {
                p = null;
            }
        }
        return p;
    }

    private void setAdditionalPrincipalInContext(RequestFacade requestFacade) {
        if (requestFacade != null) {
            Principal wrapped = requestFacade.getPrincipal();
            if (wrapped != null) {
                SecurityContext.getCurrent().setAdditionalPrincipal(wrapped);
            }
        }
    }

    private void resetAdditionalPrincipalInContext() {
        SecurityContext.getCurrent().setAdditionalPrincipal(null);
    }

    private boolean validateRequest(ServerAuthConfig serverAuthConfig, Context context, RequestFacade requestFacade, HttpRequest request, HttpResponse response, LoginConfig loginConfig, Authenticator authenticator, boolean calledFromAuthenticate, Function<HttpServletRequest, Boolean> isMandatoryFn) throws IOException {
        if (isRequestTracingEnabled()) {
            return doTraced(serverAuthConfig, context, requestFacade,
                    () -> validateRequest(request, response, loginConfig, authenticator, calledFromAuthenticate, isMandatoryFn));
        }

        return validateRequest(request, response, loginConfig, authenticator, calledFromAuthenticate, isMandatoryFn);
    }

    private boolean validateRequest(HttpRequest request, HttpResponse response, LoginConfig config, Authenticator authenticator, boolean calledFromAuthenticate, Function<HttpServletRequest, Boolean> isMandatoryFn) throws IOException {

        HttpServletRequest servletRequest = (HttpServletRequest) request.getRequest();
        HttpServletResponse servletResponse = (HttpServletResponse) response.getResponse();

        Subject subject = new Subject();
        MessageInfo messageInfo = new HttpMessageInfo(servletRequest, servletResponse);

        boolean isValidateSuccess = false;
        boolean isMandatory = true;
        ServerAuthContext authContext = null;


        try {
            isMandatory = isMandatoryFn.apply(servletRequest);

            // Produce caller challenge if call originates from HttpServletRequest#authenticate
            if (isMandatory || calledFromAuthenticate) {
                setMandatory(messageInfo);
            }

            // Obtain the JASIC ServerAuthContext, which represents the authentication mechanism that interacts with the caller
            authContext = getServerAuthContext(messageInfo);

            // Call the JASPIC ServerAuthContext which should eventually call the ServerAuthModule (SAM)

            // Notice a null is passed in as the service subject
            // Additionally notice we only care about SUCCESS being returned or not and ignore
            // all other JASPIC AuthStatus values.

            isValidateSuccess = SUCCESS.equals(authContext.validateRequest(messageInfo, subject, null));

            if (!isValidateSuccess) {
                return false;
            }

        } catch (AuthException | RuntimeException e) {
            logger.log(WARNING, "JASPIC: http msg authentication fail", e);
            servletResponse.setStatus(SC_INTERNAL_SERVER_ERROR);
        }

        // When a SAM has returned SUCCESS, it can mean 3 different things:

        // 1. The SAM authenticated the caller and a new Principal has been set
        // 2. The SAM "did nothing" and a NULL has been set
        // 3. The SAM wants to use the session and the sets the (non null) Principal it obtained from the passed-in request

        // Store the messageInfo and ServerAuthContext so that the exact same ones can be used again when the SAM
        // needs to be called again later in this request (for example, when secureResponse is called).
        storeMessageInfoInRequest(servletRequest, messageInfo, authContext);

        // There must be at least one new principal to count as SAM having authenticated
        if (hasNewPrincipal(subject.getPrincipals())) {

            // Handle case 1: The SAM authenticated the caller and a new Principal has been set

            handleSamAuthenticated(subject, messageInfo, request, response, config, authenticator);
        } else {

            // Handle case 2: The SAM "did nothing" and a NULL has been set.

            isValidateSuccess = handleSamNotAuthenticated(messageInfo, isMandatory, isValidateSuccess, request, response);
        }

        if (isValidateSuccess) {
            // Check if the SAM instructed us to wrap the request and response, and if so do the wrapping
            checkRequestResponseWrappingNeeded(messageInfo, request, response, servletRequest, servletResponse);
        }

        return isValidateSuccess;
    }

    private void handleSamAuthenticated(Subject subject, MessageInfo messageInfo, HttpRequest request, HttpResponse response, LoginConfig config, Authenticator authenticator) throws IOException {
        SecurityContext securityContext = new SecurityContext(subject);

        // Assuming no null principal here
        WebPrincipal webPrincipal = new WebPrincipal(securityContext.getCallerPrincipal(), securityContext);

        // TODO: check Java SE SecurityManager access
        SecurityContext.setCurrent(securityContext);

        try {
            String authType = getAuthType(messageInfo, config);

            if (shouldRegisterSession(messageInfo)) {

                // Besides authenticating, the SAM has indicated that the new principal should
                // be stored in a session. This means that when the SAM is called again in a next request
                // it can opt to continue this session.

                new AuthenticatorProxy(authenticator, webPrincipal, authType)
                        .authenticate(request, response, config);
            } else {
                request.setAuthType(authType == null ? AuthenticatorProxy.PROXY_AUTH_TYPE : authType);
                // it is not completely sure the necessity of webPrincipal wrapping the custom principal while
                // HttpServletRequest.getUserPrincipal() return the custom principal instead of webPrincipal
                // therefore it is necessary to check and use custom principal if available
                request.setUserPrincipal(webPrincipal.getCustomPrincipal() == null ?
                        webPrincipal : webPrincipal.getCustomPrincipal());
            }
        } catch (LifecycleException le) {
            logger.log(SEVERE, "[Web-Security] unable to register session", le);
        }
    }

    private boolean handleSamNotAuthenticated(MessageInfo messageInfo, boolean isMandatory, boolean isValidateSuccess, HttpRequest request, HttpResponse response) {

        if (hasRequestPrincipal(messageInfo)) {

            // If there's a request principal, then it means a session exists with an existing principal.
            // But the SAM has specifically chosen not to join that session (use the session's principal)
            // for this request as there was no new principal present in the subject.

            // So, set request principal to null for this request (GLASSFISH-20930)
            request.setUserPrincipal(null);
            request.setAuthType(null);
        }

        // If authentication is mandatory, we must have a non-anonymous principal
        if (isMandatory) {
            return false;
        }

        return isValidateSuccess;
    }

    private boolean doTraced(ServerAuthConfig serverAuthConfig, Context context, RequestFacade requestFacade, IOSupplier<Boolean> supplier) throws IOException {
        RequestTraceSpan span = null;
        boolean result;

        try {
            span = new RequestTraceSpan("authenticateJaspic");
            span.addSpanTag("AppContext", serverAuthConfig.getAppContext());
            span.addSpanTag("Context", context.getPath());

            result = supplier.get();

            span.addSpanTag("AuthResult", Boolean.toString(result));

            Principal principal = requestFacade.getPrincipal();
            String principalName = "null";
            if (principal != null) {
                principalName = principal.getName();
            }
            span.addSpanTag("Principal", principalName);
        } finally {
            if (span != null) {
                requestTracing.traceSpan(span);
            }
        }

        return result;
    }

    private void checkRequestResponseWrappingNeeded(MessageInfo messageInfo, HttpRequest request, HttpResponse response, HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        HttpServletRequest wrappedServletRequest = (HttpServletRequest) messageInfo.getRequestMessage();
        if (wrappedServletRequest != servletRequest) {
            request.setNote(WRAPPED_REQUEST, new HttpRequestWrapper(request, wrappedServletRequest));
        }

        HttpServletResponse wrappedServletResponse = (HttpServletResponse) messageInfo.getResponseMessage();
        if (wrappedServletResponse != servletResponse) {
            request.setNote(WRAPPED_RESPONSE, new HttpResponseWrapper(response, wrappedServletResponse));
        }
    }

    private boolean isRequestTracingEnabled() {
        return requestTracing != null && requestTracing.isRequestTracingEnabled();
    }

    private boolean hasRequestPrincipal(MessageInfo messageInfo) {
        return ((HttpServletRequest) messageInfo.getRequestMessage()).getUserPrincipal() != null;
    }

    private boolean hasNewPrincipal(Set<Principal> principalSet) {
        return principalSet != null && !principalSet.isEmpty() && !principalSetContainsOnlyAnonymousPrincipal(principalSet);
    }

    /**
     * Used to detect when the principals in the subject correspond to the default or "ANONYMOUS" principal, and therefore a
     * null principal should be set in the HttpServletRequest.
     *
     * @param principalSet
     * @return true whe a null principal is to be set.
     */
    private boolean principalSetContainsOnlyAnonymousPrincipal(Set<Principal> principalSet) {
        boolean containsOnlyAnonymousPrincipal = false;

        Principal defaultPrincipal = SecurityContext.getDefaultCallerPrincipal();
        if (defaultPrincipal != null && principalSet != null) {
            containsOnlyAnonymousPrincipal = principalSet.contains(defaultPrincipal);
        }

        if (containsOnlyAnonymousPrincipal) {
            Iterator<Principal> it = principalSet.iterator();
            while (it.hasNext()) {
                if (!it.next().equals(defaultPrincipal)) {
                    return false;
                }
            }
        }

        return containsOnlyAnonymousPrincipal;
    }

    @SuppressWarnings("unchecked")
    private void setMandatory(MessageInfo messageInfo) {
        messageInfo.getMap().put(IS_MANDATORY, TRUE.toString());
    }

    private String getAuthType(MessageInfo messageInfo, LoginConfig config) {
        String authType = getAuthType(messageInfo);

        if (authType == null && config != null && config.getAuthMethod() != null) {
            authType = config.getAuthMethod();
        }

        return authType;
    }

    private String getAuthType(MessageInfo messageInfo) {
        return (String) messageInfo.getMap().get(AUTH_TYPE);
    }

    private boolean shouldRegisterSession(MessageInfo messageInfo) {
        @SuppressWarnings("rawtypes")
        Map map = messageInfo.getMap();

        // Detect both the proprietary property and the standard one.

        return map.containsKey(REGISTER_WITH_AUTHENTICATOR) || mapEntryToBoolean(REGISTER_SESSION, map);
    }

    @SuppressWarnings("unchecked")
    private void storeMessageInfoInRequest(HttpServletRequest servletRequest, MessageInfo messageInfo, ServerAuthContext authContext) {
        messageInfo.getMap().put(SERVER_AUTH_CONTEXT, authContext);
        servletRequest.setAttribute(MESSAGE_INFO, messageInfo);
    }

    private Entry<MessageInfo, ServerAuthContext> getMessageInfoFromRequest(HttpServletRequest servletRequest) {
        if (jaspicServices != null) {
            MessageInfo messageInfo = (MessageInfo) servletRequest.getAttribute(MESSAGE_INFO);
            if (messageInfo != null) {

                // JSR 196 is enabled for this application
                return new SimpleImmutableEntry<>(messageInfo, (ServerAuthContext) messageInfo.getMap().get(SERVER_AUTH_CONTEXT));
            }
        }

        return null;
    }

    private boolean mapEntryToBoolean(String propName, Map map) {
        if (map.containsKey(propName)) {
            Object value = map.get(propName);
            if (value != null && value instanceof String) {
                return Boolean.valueOf((String) value);
            }
        }

        return false;
    }

}

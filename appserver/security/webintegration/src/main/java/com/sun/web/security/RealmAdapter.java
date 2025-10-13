/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2016 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
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
// Portions Copyright [2016-2024] [Payara Foundation and/or its affiliates]
package com.sun.web.security;

import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.RunAsIdentityDescriptor;
import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.deployment.WebComponentDescriptor;
import com.sun.enterprise.deployment.web.LoginConfiguration;
import com.sun.enterprise.security.AppCNonceCacheMap;
import com.sun.enterprise.security.CNonceCacheFactory;
import com.sun.enterprise.security.SecurityContext;
import com.sun.enterprise.security.auth.WebAndEjbToJaasBridge;
import com.sun.enterprise.security.auth.digest.api.DigestAlgorithmParameter;
import com.sun.enterprise.security.auth.digest.api.Key;
import com.sun.enterprise.security.auth.digest.impl.CNonceValidator;
import com.sun.enterprise.security.auth.digest.impl.DigestParameterGenerator;
import com.sun.enterprise.security.auth.digest.impl.HttpAlgorithmParameterImpl;
import com.sun.enterprise.security.auth.login.DigestCredentials;
import com.sun.enterprise.security.auth.login.DistinguishedPrincipalCredential;
import com.sun.enterprise.security.auth.login.LoginContextDriver;
import com.sun.enterprise.security.auth.realm.certificate.CertificateRealm;
import com.sun.enterprise.security.ee.SecurityUtil;
import com.sun.enterprise.security.ee.authentication.jakarta.AuthMessagePolicy;
import com.sun.enterprise.security.ee.authentication.jakarta.ConfigDomainParser;
import com.sun.enterprise.security.ee.authentication.jakarta.callback.ServerContainerCallbackHandler;
import com.sun.enterprise.security.integration.RealmInitializer;
import com.sun.enterprise.security.ee.authorization.WebAuthorizationManagerService;
import com.sun.enterprise.security.web.integration.WebPrincipal;
import com.sun.enterprise.security.ee.web.integration.WebSecurityManagerFactory;
import com.sun.enterprise.util.net.NetUtils;
import com.sun.logging.LogDomains;
import fish.payara.nucleus.requesttracing.RequestTracingService;
import jakarta.security.auth.message.AuthStatus;
import jakarta.security.auth.message.MessageInfo;
import jakarta.security.auth.message.config.ServerAuthContext;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Set;
import javax.security.auth.x500.X500Principal;
import org.apache.catalina.*;
import org.apache.catalina.authenticator.AuthenticatorBase;
import org.apache.catalina.authenticator.Constants;
import org.apache.catalina.connector.RequestFacade;
import org.apache.catalina.deploy.LoginConfig;
import org.apache.catalina.deploy.SecurityConstraint;
import org.apache.catalina.realm.RealmBase;
import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.epicyro.config.helper.Caller;
import org.glassfish.epicyro.config.helper.CallerPrincipal;
import org.glassfish.epicyro.config.helper.HttpServletConstants;
import org.glassfish.epicyro.config.servlet.HttpMessageInfo;
import org.glassfish.epicyro.services.BaseAuthenticationService;
import org.glassfish.epicyro.services.DefaultAuthenticationService;
import org.glassfish.grizzly.config.dom.NetworkConfig;
import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.grizzly.config.dom.NetworkListeners;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.internal.api.ServerContext;
import org.glassfish.security.common.Group;
import org.glassfish.security.common.UserNameAndPassword;
import org.jvnet.hk2.annotations.Service;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import javax.security.auth.Subject;
import jakarta.security.auth.message.AuthException;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.security.InvalidAlgorithmParameterException;
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.sun.enterprise.security.SecurityContext.setUnauthenticatedContext;
import static com.sun.enterprise.security.auth.digest.api.Constants.A1;
import static com.sun.enterprise.security.auth.digest.impl.DigestParameterGenerator.HTTP_DIGEST;

import static com.sun.enterprise.util.Utility.isAllNull;
import static com.sun.logging.LogDomains.WEB_LOGGER;
import static java.lang.String.format;
import static java.net.URLEncoder.encode;
import static java.util.Arrays.asList;
import static java.util.logging.Level.*;
import static jakarta.servlet.http.HttpServletResponse.*;
import static org.apache.catalina.ContainerEvent.*;
import static org.apache.catalina.Globals.WRAPPED_REQUEST;
import static org.apache.catalina.Globals.WRAPPED_RESPONSE;
import static org.apache.catalina.realm.Constants.FORM_METHOD;
import static org.glassfish.api.admin.ServerEnvironment.DEFAULT_INSTANCE_NAME;


import static org.glassfish.epicyro.config.helper.HttpServletConstants.POLICY_CONTEXT;
import static org.glassfish.epicyro.config.helper.HttpServletConstants.REGISTER_SESSION;
import static com.sun.enterprise.security.ee.authentication.jakarta.AuthMessagePolicy.WEB_BUNDLE;

/**
 * This is the realm adapter used to authenticate users and authorize access to web resources. The authenticate method
 * is called by Tomcat to authenticate users. The hasRole method is called by Tomcat during the authorization process.
 *
 * @author Harpreet Singh
 * @author JeanFrancois Arcand
 */
@Service
@PerLookup
public class RealmAdapter extends RealmBase implements RealmInitializer, PostConstruct {

    public static final String SECURITY_CONTEXT = "SecurityContext";
    public static final String BASIC = "BASIC";
    public static final String FORM = "FORM";

    private static final Logger LOG = LogDomains.getLogger(RealmAdapter.class, WEB_LOGGER);
    private static final ResourceBundle resourceBundle = LOG.getResourceBundle();

    private static final String REGISTER_WITH_AUTHENTICATOR = "com.sun.web.RealmAdapter.register";
    public static final String SERVER_AUTH_CONTEXT = "__jakarta.security.auth.message.ServerAuthContext";
    private static final String MESSAGE_INFO = "__jakarta.security.auth.message.MessageInfo";

    private WebBundleDescriptor webDescriptor;
    private HashMap<String, String> runAsPrincipals;

    // required for realm-per-app login
    private String realmName;

    /**
     * Descriptive information about this Realm implementation.
     */
    protected static final String name = "J2EE-RI-RealmAdapter";

    /**
     * The context Id value needed for Jakarta Authorization.
     */
    private String contextId;

    private Container virtualServer;

    /**
     * A <code>WebAuthorizationManagerService</code> object associated with a contextId
     */
    protected volatile WebAuthorizationManagerService webAuthorizationManagerService;

    protected boolean isCurrentURIincluded;

    /*
     * The following fields are used to implement a bypass of FBL related targets
     */
    protected final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private boolean contextEvaluated;
    private String loginPage;
    private String errorPage;
    private final static SecurityConstraint[] emptyConstraints = new SecurityConstraint[] {};

    private String moduleID;

    private BaseAuthenticationService authenticationService;

    @Inject
    private ServerContext serverContext;

    @Inject
    private Provider<AppCNonceCacheMap> appCNonceCacheMapProvider;

    @Inject
    private Provider<CNonceCacheFactory> cNonceCacheFactoryProvider;

    @Inject
    @Named(DEFAULT_INSTANCE_NAME)
    private NetworkConfig networkConfig;

    /**
     * The factory used for creating <code>WebAuthorizationManagerService</code> object.
     */
    @Inject
    protected WebSecurityManagerFactory webSecurityManagerFactory;

    @Inject
    private RequestTracingService requestTracing;


    private NetworkListeners nwListeners;
    private CNonceValidator cNonceValidator;

    /**
     * ThreadLocal object to keep track of the reentrancy status of each thread. It contains a byte[] object whose single
     * element is either 0 (initial value or no reentrancy), or 1 (current thread is reentrant). When a thread exits the
     * implies method, byte[0] is always reset to 0.
     */
    private static ThreadLocal<byte[]> reentrancyStatus = ThreadLocal.withInitial(() ->  new byte[] { 0 });

    private static final String PROXY_AUTH_TYPE = "PLUGGABLE_PROVIDER";

    public RealmAdapter() {
        // used during Injection in WebContainer (glue code)
    }

    /**
     * Create for WS EJB endpoint authentication. Roles related data is not available here.
     */
    public RealmAdapter(String realmName, String moduleID) {
        this.realmName = realmName;
        this.moduleID = moduleID;
    }


    /**
     * {@inheritDoc}
     *
     * @param bundleDescriptor must be an instance of {@link WebBundleDescriptor}
     */
    @Override
    public void initializeRealm(Object bundleDescriptor, boolean isSystemApp, String defaultRealmName) {
        webDescriptor = (WebBundleDescriptor) bundleDescriptor;
        LOG.config(
            () -> format("initializeRealm(bundleDescriptor.appContextId=%s, isSystemApp=%s, defaultRealmName=%s)",
                webDescriptor.getAppContextId(), isSystemApp, defaultRealmName));
        realmName = computeRealmName(defaultRealmName);
        contextId = WebAuthorizationManagerService.getContextID(webDescriptor);
        runAsPrincipals = new HashMap<>();
        for (WebComponentDescriptor componentDescriptor : webDescriptor.getWebComponentDescriptors()) {
            RunAsIdentityDescriptor runAsDescriptor = componentDescriptor.getRunAsIdentity();

            if (runAsDescriptor != null) {
                String principal = runAsDescriptor.getPrincipal();
                String servlet = componentDescriptor.getCanonicalName();
                if (principal == null || servlet == null) {
                    LOG.warning("web.realmadapter.norunas");
                } else {
                    runAsPrincipals.put(servlet, principal);
                    LOG.fine(() -> "Servlet " + servlet + " will run-as: " + principal);
                }
            }
        }

        moduleID = webDescriptor.getModuleID();
        cNonceValidator = new CNonceValidator(webDescriptor, appCNonceCacheMapProvider, cNonceCacheFactoryProvider);
    }

    /**
     * Sets the virtual server on which the web module (with which this RealmAdapter is associated with) has been deployed.
     *
     * @param container The virtual server
     */
    @Override
    public void setVirtualServer(Object container) {
        this.virtualServer = (Container) container;
    }

    /**
     * Return <tt>true</tt> if JASPIC is available.
     *
     * @return <tt>true</tt> if JASPIC is available. 1171
     */
    @Override
    public boolean isSecurityExtensionEnabled(ServletContext context) {
        if (authenticationService == null) {
            initAuthenticationService(context);
        }

        try {
            return (authenticationService.getServerAuthConfig() != null);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public synchronized void initAuthenticationService(final ServletContext servletContext) {
        if (this.authenticationService != null) {
            return;
        }

        try {
            this.authenticationService = createAuthenticationService(servletContext);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private BaseAuthenticationService createAuthenticationService(final ServletContext servletContext) throws IOException {
        Map<String, Object> properties = new HashMap<>();

        String policyContextId = SecurityUtil.getContextID(webDescriptor);
        if (policyContextId != null) {
            properties.put(POLICY_CONTEXT, policyContextId);
        }

        // "authModuleId" (HttpServletSecurityProvider) is a GlassFish proprietary mechanism where a
        // Jakarta Authentication module gets assigned an ID in the proprietary config of GlassFish (domain.xml).
        // This ID is then used in glassfish-web.xml to indicate that a war wants to use that authentication module.
        String authModuleId =
                AuthMessagePolicy.getProviderID(
                        AuthMessagePolicy.getSunWebApp(Map.of(
                                WEB_BUNDLE, webDescriptor)));

        if (authModuleId != null) {
            properties.put("authModuleId", authModuleId);
        }
        
        String appContextId = getAppContextID(servletContext);

        return new DefaultAuthenticationService(
                appContextId,
                properties,
                new ConfigDomainParser(),
                new ServerContainerCallbackHandler(realmName));
    }

    /**
     * This must be invoked after virtualServer is set.
     */
    private String getAppContextID(final ServletContext servletContext) {
        if (!servletContext.getVirtualServerName().equals(this.virtualServer.getName())) {
            LOG.log(WARNING, "Virtual server name from ServletContext: {0} differs from name from virtual.getName(): {1}",
                    new Object[] { servletContext.getVirtualServerName(), virtualServer.getName() });
        }
        if (!servletContext.getContextPath().equals(webDescriptor.getContextRoot())) {
            LOG.log(WARNING, "Context path from ServletContext: {0} differs from path from bundle: {1}",
                    new Object[] { servletContext.getContextPath(), webDescriptor.getContextRoot() });
        }
        return servletContext.getVirtualServerName() + " " + servletContext.getContextPath();
    }
    
    /**
     * One of the initial operations being done to apply security to a request, is to find out if there are security constraints
     * for a request.
     *
     * Returns null 1. if there are no security constraints defined on any of the web resources within the context, or 2. if
     * the target is a form login related page or target.
     *
     * <p>
     * See SJSAS 6232464 6202703
     *
     * otherwise return an empty array of SecurityConstraint.
     */
    @Override
    public SecurityConstraint[] findSecurityConstraints(HttpRequest request, Context context) {
        return findSecurityConstraints(null, null, context);
    }

    /**
     * Returns null if there are no security constraints defined on any of the web resources within the context
     *
     * <p>
     * See SJSAS 6232464 6202703
     *
     * otherwise return an empty array of SecurityConstraint.
     */
    @Override
    public SecurityConstraint[] findSecurityConstraints(String requestPathMB, String httpMethod, Context context) {
        if (authenticationService == null) {
            initAuthenticationService(context.getServletContext());
        }

        WebAuthorizationManagerService webAuthorizationManagerService = getWebSecurityManager(false);
        if (webAuthorizationManagerService != null && webAuthorizationManagerService.hasNoConstrainedResources()
                && !isSecurityExtensionEnabled(context.getServletContext())) {
            return null;
        }

        return emptyConstraints;
    }

    /**
     * Utility method to get web security manager.
     * Will log warning if the manager is not found in the factory, and logNull is true.
     * <p>
     * Note: webSecurityManagerFactory can be null the very questionable SOAP code just
     * instantiates a RealmAdapter
     *
     * @param logNull
     * @return {@link WebAuthorizationManagerService} or null
     */
    public WebAuthorizationManagerService getWebSecurityManager(boolean logNull) {
        if (webAuthorizationManagerService == null && webSecurityManagerFactory != null) {
            synchronized (this) {
                webAuthorizationManagerService = webSecurityManagerFactory.getManager(contextId);
            }

            if (webAuthorizationManagerService == null && logNull) {
                LOG.log(WARNING, "realmAdapter.noWebSecMgr", contextId);
            }
        }

        return webAuthorizationManagerService;
    }

    /**
     * Enforce any user data constraint required by the security constraint guarding this request URI.
     *
     * @param request Request we are processing
     * @param response Response we are creating
     * @param constraints Security constraint being checked
     *
     * @exception IOException if an input/output error occurs
     *
     * @return <code>true</code> if this constraint was not violated and processing should continue, or <code>false</code>
     * if we have created a response already
     */
    @Override
    public boolean hasUserDataPermission(HttpRequest request, HttpResponse response, SecurityConstraint[] constraints) throws IOException {
        return hasUserDataPermission(request, response, constraints, null, null);
    }

    /**
     * Checks if the given request URI and method are the target of any user-data-constraint with a transport-guarantee of
     * CONFIDENTIAL, and whether any such constraint is already satisfied.
     *
     * <p>
     * If <tt>uri</tt> and <tt>method</tt> are null, then the URI and method of the given <tt>request</tt> are checked.
     *
     * <p>
     * If a user-data-constraint exists that is not satisfied, then the given <tt>request</tt> will be redirected to HTTPS.
     *
     * @param request the request that may be redirected
     * @param response the response that may be redirected
     * @param constraints the security constraints to check against
     * @param uri the request URI (minus the context path) to check
     * @param method the request method to check
     *
     * @return true if the request URI and method are not the target of any unsatisfied user-data-constraint with a
     * transport-guarantee of CONFIDENTIAL, and false if they are (in which case the given request will have been redirected
     * to HTTPS)
     */
    @Override
    public boolean hasUserDataPermission(HttpRequest request, HttpResponse response, SecurityConstraint[] constraints, String uri, String method) throws IOException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;

        if (httpServletRequest.getServletPath() == null) {
            request.setServletPath(getResourceName(httpServletRequest.getRequestURI(), httpServletRequest.getContextPath()));
        }

        logHasUserDataPermission(httpServletRequest);

        if (request.getRequest().isSecure()) {
            logRequestSecure(request);
            return true;
        }

        WebAuthorizationManagerService securityManager = getWebSecurityManager(true);
        if (securityManager == null) {
            return false;
        }

        int isGranted = 0;
        try {
            isGranted = securityManager.hasUserDataPermission(httpServletRequest, uri, method);
        } catch (IllegalArgumentException e) {
            // End the request after getting IllegalArgumentException while checking user data permission
            sendBadRequest(response, e);
            return false;
        }

        // Only redirect if we are sure the user will be granted.
        // See bug 4947698

        // -1 - if the current transport is not granted, but a redirection can occur so the grand will succeed.
        if (isGranted == -1) {
            logSSLRedirect();
            return redirect(request, response);
        }

        // 0 - if not granted
        if (isGranted == 0) {
            sendForbidden(response);
            return false;
        }

        // 1 - if granted
        return true;
    }

    /**
     * Checks whether or not authentication is needed. If JASPIC / JSR 196 is active, authentication is always done.
     *
     * <p>
     * Returns an int, one of:
     * <ul>
     *     <li>AUTHENTICATE_NOT_NEEDED,
     *     <li>AUTHENTICATE_NEEDED
     *     <li>AUTHENTICATED_NOT_AUTHORIZED
     * </ul>
     *
     * <p>
     * See SJSAS 6202703
     *
     * @param request Request we are processing
     * @param response Response we are creating
     * @param constraints Security constraint we are enforcing
     * @param disableProxyCaching whether or not to disable proxy caching for protected resources.
     * @param securePagesWithPragma true if we add headers which are incompatible with downloading office documents in IE
     * under SSL but which fix a caching problem in Mozilla.
     * @param ssoEnabled true if sso is enabled
     *
     * @exception IOException if an input/output error occurs
     */
    @Override
    public int preAuthenticateCheck(HttpRequest request, HttpResponse response, SecurityConstraint[] constraints, 
                                    boolean disableProxyCaching, boolean securePagesWithPragma, boolean ssoEnabled) throws IOException {
        boolean isGranted = false;

        try {
            if (!hasRequestPrincipal(request)) {
                setUnauthenticatedContext();
            }

            // JASPIC enabled; always give the SAM the opportunity to authenticate
            if (isJakartaAuthenticationEnabled()) {
                return AUTHENTICATE_NEEDED;
            }

            // JASPIC not enabled, check with the Servlet/Catalina mechanism
            isGranted = invokeWebSecurityManager(request, response, constraints);
        } catch (IOException iex) {
            throw iex;
        } catch (Throwable ex) {
            sendServiceUnavailable(response, ex);
            return AUTHENTICATED_NOT_AUTHORIZED;
        }

        if (isGranted) {
            if (hasRequestPrincipal(request)) {
                disableProxyCaching(request, response, disableProxyCaching, securePagesWithPragma);
                if (ssoEnabled) {
                    HttpServletRequest httpServletRequest = (HttpServletRequest) request.getRequest();
                    if (!getWebSecurityManager(true).permitAll(httpServletRequest)) {
                        // Create a session for protected SSO association
                        httpServletRequest.getSession(true);
                    }
                }
            }
            return AUTHENTICATE_NOT_NEEDED;
        }

        if (hasRequestPrincipal(request)) {
            sendForbidden(response);
            return AUTHENTICATED_NOT_AUTHORIZED;
        }

        disableProxyCaching(request, response, disableProxyCaching, securePagesWithPragma);

        return AUTHENTICATE_NEEDED;
    }

    @Override
    public boolean invokeAuthenticateDelegate(HttpRequest request, HttpResponse response, Context context, Authenticator authenticator, boolean calledFromAuthenticate) throws IOException {
        LoginConfig config = context.getLoginConfig();

        if (isJakartaAuthenticationEnabled()) {
            // Jakarta Authentication is enabled for this application
            try {
                context.fireContainerEvent(BEFORE_AUTHENTICATION, null);
                RequestFacade requestFacade = (RequestFacade) request.getRequest();
                SecurityContext.getCurrent().setSessionPrincipal(requestFacade.getRequestPrincipal());
                return validateRequest(request, response, config, authenticator, calledFromAuthenticate);
            } finally {
                SecurityContext.getCurrent().setSessionPrincipal(null);
                context.fireContainerEvent(AFTER_AUTHENTICATION, null);
            }
        }

        // Jakarta Authentication is not enabled. Use the current authenticator.
        return ((AuthenticatorBase) authenticator).authenticate(request, response, config);
    }

    private boolean validateRequest(HttpRequest request, HttpResponse response, LoginConfig config, Authenticator authenticator, boolean calledFromAuthenticate) throws IOException {

        HttpServletRequest servletRequest = (HttpServletRequest) request.getRequest();
        HttpServletResponse servletResponse = (HttpServletResponse) response.getResponse();

        Subject subject = new Subject();
        MessageInfo messageInfo = new HttpMessageInfo(servletRequest, servletResponse);

        boolean isRequestValidated = false;
        boolean isMandatory = true;
        ServerAuthContext authContext = null;


        try {
            isMandatory = !this.webAuthorizationManagerService.permitAll(servletRequest);
            // Produce caller challenge if call originates from HttpServletRequest#authenticate
            if (isMandatory || calledFromAuthenticate) {
                messageInfo.getMap().put(HttpServletConstants.IS_MANDATORY, Boolean.TRUE.toString());
            }

            // Obtain the JASIC ServerAuthContext, which represents the authentication mechanism that interacts with the caller
            authContext = authenticationService.getServerAuthContext(messageInfo, null);

            if (authContext == null) {
                throw new AuthException("null ServerAuthContext");
            }
            AuthStatus authStatus = authContext.validateRequest(messageInfo, subject, null); // null serviceSubject
            isRequestValidated = AuthStatus.SUCCESS.equals(authStatus);

            if (isRequestValidated) { // cache it only if validateRequest = true
                messageInfo.getMap().put(SERVER_AUTH_CONTEXT, authContext);
                servletRequest.setAttribute(MESSAGE_INFO, messageInfo);
            }

        } catch (AuthException | RuntimeException e) {
            LOG.log(WARNING, "JASPIC: http msg authentication fail", e);
            servletResponse.setStatus(SC_INTERNAL_SERVER_ERROR);
        }

        if (isRequestValidated) {
            Caller caller = getCaller(subject);

            // Must have a caller to establish non-default security context
            if (caller != null) {

                // Convert Epicyro representation of the Caller Principal / Groups to the existing
                // GlassFish one. A future version of this code may use the Epicyro one everywhere directly.
                subject = new Subject();

                // See if there's a Subject stored in the session that contain all relevant principals and
                // credentials for reuse, and the caller has indicated to take these.
                Subject sessionSubject = reuseSessionSubject(caller);
                if (sessionSubject != null) {
                    // Copy principals, public credentials and private credentials from the Subject that lives in
                    // the session to the receiving Subject.
                    copySubject(subject, sessionSubject);
                } else {
                    Principal glassFishCallerPrincipal = getGlassFishCallerPrincipal(caller);

                    toSubject(subject, glassFishCallerPrincipal);
                    DistinguishedPrincipalCredential distinguishedPrincipal = new DistinguishedPrincipalCredential(glassFishCallerPrincipal);

                    // Credentials don't serialize, so for now, also add to the subject principals
                    // For next version, see if we can only use principals
                    toSubject(subject, distinguishedPrincipal);
                    toSubjectCredential(subject, distinguishedPrincipal);

                    for (String group : caller.getGroups()) {
                        toSubject(subject, new Group(group));
                    }

                    if (!glassFishCallerPrincipal.equals(SecurityContext.getDefaultCallerPrincipal())) {

                        // Give native GlassFish (realms, mostly) opportunity to add groups
                        LoginContextDriver.jmacLogin(subject, glassFishCallerPrincipal, realmName);

                        SecurityContext ctx = new SecurityContext(subject);
                        SecurityContext.setCurrent(ctx);

                        // XXX assuming no null principal here
                        Principal principal = ctx.getCallerPrincipal();
                        WebPrincipal webPrincipal = new WebPrincipal(principal, ctx);
                        try {
                            String authType = (String) messageInfo.getMap().get(HttpServletConstants.AUTH_TYPE);
                            if (authType == null && config != null && config.getAuthMethod() != null) {
                                authType = config.getAuthMethod();
                            }

                            if (shouldRegister(messageInfo.getMap())) {
                                // Sets webPrincipal for the session and request
                                new AuthenticatorProxy(authenticator, webPrincipal, authType)
                                        .authenticate(request, response, config);
                            } else {
                                // Sets webPrincipal for the request only
                                request.setAuthType(authType == null ? PROXY_AUTH_TYPE : authType);
                                request.setUserPrincipal(webPrincipal);
                            }
                        } catch (LifecycleException le) {
                            LOG.log(SEVERE, "Unable to register session", le);

                        }

                    } else {
                        // GLASSFISH-20930.Set null for the case when SAM does not
                        // indicate that it needs the session
                        if (((HttpServletRequest) messageInfo.getRequestMessage()).getUserPrincipal() != null) {
                            request.setUserPrincipal(null);
                            request.setAuthType(null);
                        }

                        if (isMandatory) {
                            isRequestValidated = false;
                        }
                    }
                }
            }

            if (isRequestValidated) {
                HttpServletRequest newRequest = (HttpServletRequest) messageInfo.getRequestMessage();
                if (newRequest != servletRequest) {
                    request.setNote(WRAPPED_REQUEST, new HttpRequestWrapper(request, newRequest));
                }

                HttpServletResponse newResponse = (HttpServletResponse) messageInfo.getResponseMessage();
                if (newResponse != servletResponse) {
                    request.setNote(WRAPPED_RESPONSE, new HttpResponseWrapper(response, newResponse));
                }
            }

        }

        return isRequestValidated;
    }

    private boolean shouldRegister(Map map) {
        /*
         * Detect both the proprietary property and the standard one.
         */
        return map.containsKey(REGISTER_WITH_AUTHENTICATOR) || mapEntryToBoolean(REGISTER_SESSION, map);
    }

    private boolean mapEntryToBoolean(final String propName, final Map map) {
        if (map.containsKey(propName)) {
            Object value = map.get(propName);
            if (value != null && value instanceof String) {
                return Boolean.parseBoolean((String) value);
            }
        }

        return false;
    }

    public static void toSubjectCredential(Subject subject, Object credential) {
        subject.getPublicCredentials().add(credential);
    }

    public static void toSubject(Subject subject, Principal principal) {
        subject.getPrincipals().add(principal);
    }

    private Principal getGlassFishCallerPrincipal(Caller caller) {
        Principal callerPrincipal = caller.getCallerPrincipal();

        // Check custom principal
        if (callerPrincipal instanceof CallerPrincipal == false) {
            return callerPrincipal;
        }

        // Check anonymous principal
        if (callerPrincipal.getName() == null) {
            return SecurityContext.getDefaultCallerPrincipal();
        }

        // Check certificate / X500 principal (this is oddly specific)
        if (CertificateRealm.AUTH_TYPE.equals(realmName)) {
            return new X500Principal(callerPrincipal.getName());
        }

        return new UserNameAndPassword(callerPrincipal.getName());
    }

    public static void copySubject(Subject target, Subject source) {
        target.getPrincipals().addAll(source.getPrincipals());
        target.getPublicCredentials().addAll(source.getPublicCredentials());
        target.getPrivateCredentials().addAll(source.getPrivateCredentials());
    }

    private Caller getCaller(Subject subject) {
        Set<Caller> callers = subject.getPrincipals(Caller.class);
        if (callers.isEmpty()) {
            return null;
        }

        return callers.iterator().next();
    }

    private Subject reuseSessionSubject(final Caller caller) {
        Principal returnedPrincipal = findPrincipalWrapper(caller.getCallerPrincipal());

        if (returnedPrincipal instanceof WebPrincipal) {
            return reuseWebPrincipal((WebPrincipal) returnedPrincipal);
        }

        return null;
    }

    private Subject reuseWebPrincipal(final WebPrincipal webPrincipal) {

        SecurityContext securityContext = webPrincipal.getSecurityContext();
        final Subject securityContextSubject = securityContext != null ? securityContext.getSubject() : null;
        final Principal callerPrincipal = securityContext != null ? securityContext.getCallerPrincipal() : null;
        final Principal defaultPrincipal = SecurityContext.getDefaultCallerPrincipal();

        // This method uses 4 (numbered) criteria to determine if the argument WebPrincipal can be reused

        /**
         * 1. WebPrincipal must contain a SecurityContext and SC must have a non-null, non-default callerPrincipal and a Subject
         */
        if (callerPrincipal == null || callerPrincipal.equals(defaultPrincipal) || securityContextSubject == null) {
            return null;
        }

        boolean hasObject = false;
        Set<DistinguishedPrincipalCredential> distinguishedCreds = securityContextSubject.getPublicCredentials(DistinguishedPrincipalCredential.class);
        if (distinguishedCreds.size() == 1) {
            for (DistinguishedPrincipalCredential cred : distinguishedCreds) {
                if (cred.principal().equals(callerPrincipal)) {
                    hasObject = true;
                }

            }
        }

        if (!hasObject) {
            Set<DistinguishedPrincipalCredential> distinguishedPrincipals = securityContextSubject.getPrincipals(DistinguishedPrincipalCredential.class);
            if (distinguishedPrincipals.size() == 1) {
                for (DistinguishedPrincipalCredential cred : distinguishedPrincipals) {
                    if (cred.principal().equals(callerPrincipal)) {
                        hasObject = true;
                    }
                }
            }
        }

        /**
         * 2. Subject within SecurityContext must contain a single DistinguishedPrincipalCredential that identifies the Caller Principal
         */
        if (!hasObject) {
            return null;
        }

        hasObject = securityContextSubject.getPrincipals().contains(callerPrincipal);

        /**
         * 3. Subject within SecurityContext must contain the caller principal
         */
        if (!hasObject) {
            return null;
        }

        /**
         * 4. The webPrincipal must have a non null name that equals the name of the callerPrincipal.
         */
        if (webPrincipal.getName() == null || !webPrincipal.getName().equals(callerPrincipal.getName())) {
            return null;
        }

        return securityContextSubject;
    }

    private Principal findPrincipalWrapper(Principal principal) {
        if (principal != null && !(principal instanceof WebPrincipal)) {

            // Get the top level session principal
            Principal sessionPrincipal = SecurityContext.getCurrent().getSessionPrincipal();

            // If it's the wrapper we're looking for, it must be of type WebPrincipal
            if (sessionPrincipal instanceof WebPrincipal) {
                WebPrincipal webPrincipalFromSession = (WebPrincipal) sessionPrincipal;

                // Check if the top level session principal is indeed wrapping our current principal
                if (webPrincipalFromSession.getCustomPrincipal() == principal) {

                    // Custom principal from wrapper is the same as our current principal, so
                    // this is the wrapper we're looking for.
                    return webPrincipalFromSession;
                }
            }
        }

        // Not wrapped, or wrapper could not be found
        return principal;
    }

    @Override
    protected String getName() {
        return name;
    }

    @Override
    public String getRealmName() {
        return realmName;
    }
    

    @Override
    public void updateWebSecurityManager() {
        if (webAuthorizationManagerService == null) {
            webAuthorizationManagerService = this.getWebAuthorizationManager(true);
        }

        if (webAuthorizationManagerService != null) {
            try {
                webAuthorizationManagerService.release();
                webAuthorizationManagerService.destroy();
            } catch (Exception ex) {
                LOG.log(Level.SEVERE, "Failed to release and destroy the jaccWebAuthorizationManager", ex);
            }

            webAuthorizationManagerService = webSecurityManagerFactory.createManager(webDescriptor, true, serverContext);
            LOG.fine(() -> "WebAuthorizationManagerService for " + contextId + " has been updated");
        }
    }

    /**
     * Authenticates and sets the SecurityContext in the TLS.
     *
     * <p>
     * This username/password authenticate variant is primarily used by the Basic- and FormAuthenticator.
     *
     * @return the authenticated principal.
     * @param username the user name.
     * @param password the password.
     */
    @Override
    public Principal authenticate(String username, char[] password) {
        LOG.finest(() -> format("authenticate(username=%s, password)", username));
        if (authenticate(username, password, null, null)) {
            return new WebPrincipal(username, password, SecurityContext.getCurrent());
        }
        return null;
    }

    /**
     * Authenticates and sets the SecurityContext in the TLS.
     *
     * This HttpServletRequest authenticate variant is primarily used by the DigestAuthenticator
     */
    @Override
    public Principal authenticate(HttpServletRequest httpServletRequest) {
        DigestAlgorithmParameter[] params;
        String username;
        try {
            params = getDigestParameters(httpServletRequest);
            username = getDigestKey(params).getUsername();
        } catch (Exception le) {
            LOG.log(WARNING, "web.login.failed", (Object) le);
            return null;
        }

        if (authenticate(username, null, null, params)) {
            return new WebPrincipal(username, (char[]) null, SecurityContext.getCurrent());
        }

        return null;
    }

    /**
     * Authenticates and sets the SecurityContext in the TLS.
     *
     * This HttpServletRequest authenticate variant is primarily used by the SSLAuthenticator
     */
    @Override
    public Principal authenticate(X509Certificate certificates[]) {
        if (authenticate(null, null, certificates, null)) {
            return new WebPrincipal(certificates, SecurityContext.getCurrent(), true);
        }

        return null;
    }

    /**
     * Perform access control based on the specified authorization constraint. Return <code>true</code> if this constraint
     * is satisfied and processing should continue, or <code>false</code> otherwise.
     *
     * @param request Request we are processing
     * @param response Response we are creating
     * @param constraints Security constraint we are enforcing
     * @param context The Context to which client of this class is attached.
     *
     * @exception IOException if an input/output error occurs
     */
    @Override
    public boolean hasResourcePermission(HttpRequest request, HttpResponse response, SecurityConstraint[] constraints, Context context) throws IOException {
        boolean isGranted = false;

        try {
            isGranted = invokeWebSecurityManager(request, response, constraints);
        } catch (IOException iex) {
            throw iex;
        } catch (Throwable ex) {
            LOG.log(SEVERE, "web_server.excep_authenticate_realmadapter", ex);
            ((HttpServletResponse) response.getResponse()).sendError(SC_SERVICE_UNAVAILABLE);
            response.setDetailMessage(resourceBundle.getString("realmBase.forbidden"));

            return isGranted;
        }

        if (isGranted) {
            return isGranted;
        }

        ((HttpServletResponse) response.getResponse()).sendError(SC_FORBIDDEN);
        response.setDetailMessage(resourceBundle.getString("realmBase.forbidden"));

        // Invoking secureResponse
        invokePostAuthenticateDelegate(request, response, context);

        return isGranted;
    }

    /**
     * Post authentication for given request and response.
     *
     * @param request Request we are processing
     * @param response Response we are creating
     * @param context The Context to which client of this class is attached.
     * @exception IOException if an input/output error occurs
     */
    @Override
    public boolean invokePostAuthenticateDelegate(HttpRequest request, HttpResponse response, Context context) throws IOException {
        boolean result = false;
        ServerAuthContext serverAuthContext = null;
        try {
            if (authenticationService != null) {
                HttpServletRequest httpServletRequest = (HttpServletRequest) request.getRequest();
                MessageInfo messageInfo = (MessageInfo) httpServletRequest.getAttribute(MESSAGE_INFO);
                if (messageInfo != null) {
                    // Jakarta Authentication is enabled for this application
                    serverAuthContext = (ServerAuthContext) messageInfo.getMap().get(SERVER_AUTH_CONTEXT);
                    if (serverAuthContext != null) {
                        try {
                            context.fireContainerEvent(BEFORE_POST_AUTHENTICATION, null);
                            AuthStatus authStatus = serverAuthContext.secureResponse(messageInfo, null); // null serviceSubject
                            result = AuthStatus.SUCCESS.equals(authStatus);
                        } finally {
                            context.fireContainerEvent(AFTER_POST_AUTHENTICATION, null);
                        }
                    }
                }
            }
        } catch (AuthException ex) {
            throw new IOException(ex);
        } finally {
            if (authenticationService != null && serverAuthContext != null) {
                if (request instanceof HttpRequestWrapper) {
                    request.removeNote(WRAPPED_REQUEST);
                }
                if (response instanceof HttpResponseWrapper) {
                    request.removeNote(WRAPPED_RESPONSE);
                }
            }
        }

        return result;
    }

    /**
     * Check if the given principal has the provided role. Returns true if the principal has the specified role, false
     * otherwise.
     *
     * @param principal the principal
     * @param role the role
     * @return true if the principal has the specified role.
     * @param request Request we are processing
     * @param response Response we are creating
     */
    @Override
    public boolean hasRole(HttpRequest request, HttpResponse response, Principal principal, String role) {
        WebAuthorizationManagerService authorizationManager = getWebSecurityManager(true);
        if (authorizationManager == null) {
            return false;
        }
        String servletName = getCanonicalName(request);
        boolean isGranted = authorizationManager.hasRoleRefPermission(servletName, role, principal);
        LOG.fine(() -> "Checking if servlet " + servletName + " with principal " + principal +
            " has role " + role + " isGranted: " + isGranted);
        return isGranted;
    }

    @Override
    public void logout(HttpRequest httpRequest) {
        boolean securityExtensionEnabled = isSecurityExtensionEnabled(httpRequest.getRequest().getServletContext());
        byte[] alreadyCalled = reentrancyStatus.get();

        if (securityExtensionEnabled && authenticationService != null && alreadyCalled[0] == 0) {
            alreadyCalled[0] = 1;

            MessageInfo messageInfo = (MessageInfo) httpRequest.getRequest().getAttribute(MESSAGE_INFO);
            if (messageInfo == null) {
                messageInfo = new HttpMessageInfo((HttpServletRequest) httpRequest.getRequest(),
                        (HttpServletResponse) httpRequest.getResponse().getResponse());
            }

            messageInfo.getMap().put(HttpServletConstants.IS_MANDATORY, Boolean.TRUE.toString());
            try {
                ServerAuthContext serverAuthContext = authenticationService.getServerAuthContext(messageInfo, null);
                if (serverAuthContext != null) {
                    /*
                     * Check for the default/server-generated/unauthenticated security context.
                     */
                    SecurityContext securityContext = SecurityContext.getCurrent();
                    Subject subject = securityContext.didServerGenerateCredentials() ? new Subject() : securityContext.getSubject();

                    if (subject == null) {
                        subject = new Subject();
                    }
                    if (subject.isReadOnly()) {
                        LOG.log(WARNING, "Read-only subject found during logout processing");
                    }

                    try {
                        httpRequest.getContext().fireContainerEvent(BEFORE_LOGOUT, null);
                        serverAuthContext.cleanSubject(messageInfo, subject);
                    } finally {
                        httpRequest.getContext().fireContainerEvent(AFTER_LOGOUT, null);
                    }
                }
            } catch (AuthException ex) {
                throw new RuntimeException(ex);
            } finally {
                doLogout(httpRequest, true);
                alreadyCalled[0] = 0;
            }
        } else {
            doLogout(httpRequest, alreadyCalled[0] == 1);
        }
    }

    @Override
    public void logout() {
        setSecurityContext(null);

        // Sets the security context for Jakarta Authorization
        WebAuthorizationManagerService webAuthorizationManagerService = getWebSecurityManager(false);
        if (webAuthorizationManagerService != null) {
            webAuthorizationManagerService.onLogout();
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        if (authenticationService != null) {
            authenticationService.disable();
        }
    }

    /**
     * Used by SecurityServiceImpl
     */
    public boolean authenticate(WebPrincipal principal) {
        if (principal.isUsingCertificate()) {
            return authenticate(null, null, principal.getCertificates(), null);
        }

        return authenticate(principal.getName(), principal.getPassword(), null, null);
    }

    /**
     * Utility method to get the web security manager.
     *
     * <p>
     * This will log a warning if the manager is not found in the factory, and logNull is true.
     *
     */
    public WebAuthorizationManagerService getWebAuthorizationManager(boolean logNull) {
        if (webAuthorizationManagerService == null) {
            synchronized (this) {
                webAuthorizationManagerService = webSecurityManagerFactory.getManager(contextId, null,false);
            }
            if (webAuthorizationManagerService == null && logNull) {
                LOG.log(WARNING, "realmAdapter.noWebSecMgr", contextId);
            }
        }

        return webAuthorizationManagerService;
    }

    /**
     * This method is added to create a Principal based on the username only. Hercules stores the username as part of
     * authentication failover and needs to create a Principal based on username only <sridhar.satuloori@sun.com> See IASRI
     * 4809144
     *
     * @param username
     * @return Principal for the user username HERCULES:add
     */
    public Principal createFailOveredPrincipal(String username) {
        LOG.log(FINEST, "createFailOveredPrincipal ({0})", username);

        // Set the appropriate security context
        loginForRunAs(username);
        SecurityContext securityContext = SecurityContext.getCurrent();
        LOG.log(FINE, "Security context is {0}", securityContext);

        Principal principal = new WebPrincipal(username, (char[]) null, securityContext);
        LOG.log(INFO, "Principal created for FailOvered user {0}", principal);

        return principal;
    }

    public boolean hasRole(String servletName, Principal principal, String role) {
        WebAuthorizationManagerService webSecurityManager = getWebAuthorizationManager(true);
        if (webSecurityManager == null) {
            return false;
        }

        return webSecurityManager.hasRoleRefPermission(servletName, role, principal);
    }

    /**
     * Set the run-as principal into the SecurityContext when needed.
     *
     * <P>
     * This method will attempt to obtain the name of the servlet from the ComponentInvocation. Note that there may not be
     * one since this gets called also during internal processing (not clear..) not just part of servlet requests. However,
     * if it is not a servlet request there is no need (or possibility) to have a run-as setting so no further action is
     * taken.
     *
     * <P>
     * If the servlet name is present the runAsPrincipals cache is checked to find the run-as principal to use (if any). If
     * one is set, the SecurityContext is switched to this principal.
     *
     * <p>
     * See IASRI 4747594
     *
     * @param inv The invocation object to process.
     *
     */
    public void preSetRunAsIdentity(ComponentInvocation inv) {

        // Optimization to avoid the expensive call to getServletName
        // for cases with no run-as descriptors

        if (runAsPrincipals != null && runAsPrincipals.isEmpty()) {
            return;
        }

        String servletName = this.getServletName(inv);
        if (servletName == null) {
            return;
        }

        String runAs = runAsPrincipals.get(servletName);

        if (runAs != null) {
            // The existing SecurityContext is saved - however, this seems
            // meaningless - see bug 4757733. For now, keep it unchanged
            // in case there are some dependencies elsewhere in RI.
            SecurityContext old = getSecurityContext();
            inv.setOldSecurityContext(old);

            // Set the run-as principal into SecurityContext
            loginForRunAs(runAs);

            LOG.fine(() -> "run-as principal for " + servletName + " set to: " + runAs);
        }
    }

    /**
     * Attempts to restore old SecurityContext (but fails).
     *
     * <P>
     * In theory this method seems to attempt to check if a run-as principal was set by preSetRunAsIdentity() (based on the
     * indirect assumption that if the servlet in the given invocation has a run-as this must've been the case). If so, it
     * retrieves the oldSecurityContext from the invocation object and set it in the SecurityContext.
     *
     * <P>
     * The problem is that the invocation object is not the same object as was passed in to preSetRunAsIdentity() so it will
     * never contain the right info - see bug 4757733.
     *
     * <P>
     * In practice it means this method only ever sets the SecurityContext to null (if run-as matched) or does nothing. In
     * particular note the implication that it <i>will</i> be set to null after a run-as invocation completes. This behavior
     * will be retained for the time being for consistency with RI. It must be fixed later.
     *
     * @param invocation The invocation object to process.
     *
     */
    public void postSetRunAsIdentity(ComponentInvocation invocation) {

        // Optimization to avoid the expensive call to getServletName
        // for cases with no run-as descriptors

        if (runAsPrincipals != null && runAsPrincipals.isEmpty()) {
            return;
        }

        String servletName = getServletName(invocation);
        if (servletName == null) {
            return;
        }

        String runAs = runAsPrincipals.get(servletName);
        if (runAs != null) {
            setSecurityContext((SecurityContext) invocation.getOldSecurityContext()); // always null
        }
    }





    // ############################   Private methods ######################################

    /**
     * Authenticates and sets the SecurityContext in the TLS.
     *
     * <p>
     * This is the general authenticate method called by the other public authenticate methods.
     *
     * @return true if authentication succeeded, false otherwise.
     * @param username the username .
     * @param password the password.
     * @param certs Certificate Array.
     */
    private boolean authenticate(String username, char[] password, X509Certificate[] certs, DigestAlgorithmParameter[] digestParams) {
        try {
            if (certs != null) {
                // Certificate credential used to authenticate
                WebAndEjbToJaasBridge.doX500Login(createSubjectWithCerts(certs), moduleID);
            } else if (digestParams != null) {
                // Digest credential used to authenticate
                WebAndEjbToJaasBridge.login(new DigestCredentials(realmName, username, digestParams));
            } else {
                // Username/password credential used to authenticate
                WebAndEjbToJaasBridge.login(username, password, realmName);
            }
            LOG.log(FINE, () -> "Web login succeeded for: " + username);
            return true;
        } catch (Exception le) {
            LOG.log(WARNING, "web.login.failed", (Object) le);
            if (LOG.isLoggable(FINE)) {
                LOG.log(FINE, "Web login failed for user " + username, le);
            }
            return false;
        }
    }

    private String computeRealmName(String defaultRealmName) {
        Application application = webDescriptor.getApplication();
        LoginConfiguration loginConfig = webDescriptor.getLoginConfiguration();
        String computedRealmName = application.getRealm();
        if (computedRealmName == null && loginConfig != null) {
            computedRealmName = loginConfig.getRealmName();
        }
        if (defaultRealmName != null && (computedRealmName == null || computedRealmName.isEmpty())) {
            computedRealmName = defaultRealmName;
        }
        return computedRealmName;
    }

    private void doLogout(HttpRequest request, boolean extensionEnabled) {
        final Context context = request.getContext();
        final Authenticator authenticator = context == null ? null : context.getAuthenticator();
        Objects.requireNonNull(authenticator, "Context or Authenticator is null");
        try {
            if (extensionEnabled) {
                new AuthenticatorProxy(authenticator, null, null).logout(request);
            } else {
                authenticator.logout(request);
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        logout();
    }

    /**
     * Obtain servlet name from invocation.
     *
     * <P>
     * In order to obtain the servlet name one of the following must be true: 1. The instanceName of the ComponentInvocation
     * is not null 2. The ComponentInvocation contains a 'class' of type HttpServlet, which contains a valid ServletConfig
     * object. This method returns the value returned by getServletName() on the ServletConfig.
     *
     * <P>
     * If the above is not met, null is returned.
     *
     * @param invocation The invocation object to process.
     * @return Servlet name or null.
     *
     */
    private String getServletName(ComponentInvocation invocation) {

        String servletName = invocation.getInstanceName();
        if (servletName != null) {
            return servletName;
        }

        Object invocationInstance = invocation.getInstance();

        if (invocationInstance instanceof HttpServlet) {

            HttpServlet thisServlet = (HttpServlet) invocationInstance;
            ServletConfig servletConfig = thisServlet.getServletConfig();

            if (servletConfig != null) {
                return thisServlet.getServletName();
            }
        }

        return null;
    }

    private void loginForRunAs(String principal) {
        WebAndEjbToJaasBridge.loginPrincipal(principal, realmName);
    }

    private SecurityContext getSecurityContext() {
        return SecurityContext.getCurrent();
    }

    private void setSecurityContext(SecurityContext securityContext) {
        SecurityContext.setCurrent(securityContext);
    }

    @Override
    protected char[] getPassword(String username) {
        throw new IllegalStateException("Should not reach here");
    }

    @Override
    protected Principal getPrincipal(String username) {
        throw new IllegalStateException("Should not reach here");
    }




    // Private methods


    /**
     * Invokes WebSecurityManager to perform access control check. Return <code>true</code> if permission is granted, or
     * <code>false</code> otherwise.
     *
     * @param request Request we are processing
     * @param response Response we are creating
     * @param constraints Security constraint we are enforcing
     *
     * @exception IOException if an input/output error occurs
     */
    private boolean invokeWebSecurityManager(HttpRequest request, HttpResponse response, 
                                             SecurityConstraint[] constraints) throws IOException {
        if (isRequestFormPage(request)) {
            return true;
        }

        setServletPath(request);
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;

        LOG.log(FINE, () -> "[Web-Security] [ hasResourcePermission ]" +
                " Principal: " + httpServletRequest.getUserPrincipal() +
                " ContextPath: " + httpServletRequest.getContextPath());

        WebAuthorizationManagerService webAuthorizationManagerService = getWebSecurityManager(true);
        if (webAuthorizationManagerService == null) {
            return false;
        }

        return webAuthorizationManagerService.hasResourcePermission(httpServletRequest);
    }

    private void setServletPath(HttpRequest request) {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        if (httpServletRequest.getServletPath() == null) {
            request.setServletPath(getResourceName(httpServletRequest.getRequestURI(), httpServletRequest.getContextPath()));
        }
    }

    private boolean isRequestFormPage(HttpRequest request) {
        initFormPages();

        if (isAllNull(loginPage, errorPage)) {
            return false;
        }

        String requestURI = request.getRequestPathMB().toString();
        LOG.log(FINE, "requestURI: {0}, loginPage: {1}, errorPage: {2}",
                new Object[] {requestURI, loginPage, errorPage});

        if (loginPage != null && loginPage.equals(requestURI)) {
            LOG.log(FINE, "Allowed access to login page {0}", loginPage);
            return true;
        }

        if (errorPage != null && errorPage.equals(requestURI)) {
            LOG.log(FINE, "Allowed access to error page {0}", errorPage);
            return true;
        }

        if (requestURI.endsWith(Constants.FORM_ACTION)) {
            LOG.log(FINE, "Allowed access to username/password submission ({0})", Constants.FORM_ACTION);
            return true;
        }

        return false;
    }

    private void initFormPages() {
        // allow access to form login related pages and targets
        // and the "j_security_check" action
        boolean evaluated = false;
        try {
            rwLock.readLock().lock();
            evaluated = contextEvaluated;
        } finally {
            rwLock.readLock().unlock();
        }

        if (!evaluated) {
            try {
                rwLock.writeLock().lock();
                if (!contextEvaluated) {
                    // get Context here as preAuthenticateCheck does not have it
                    // and our Container is always a Context
                    Context context = (Context) getContainer();
                    LoginConfig config = context.getLoginConfig();
                    if (config != null && FORM_METHOD.equals(config.getAuthMethod())) {
                        loginPage = config.getLoginPage();
                        errorPage = config.getErrorPage();
                    }
                    contextEvaluated = true;
                }
            } finally {
                rwLock.writeLock().unlock();
            }
        }
    }

    private boolean redirect(HttpRequest request, HttpResponse response) throws IOException {
        // Initialize variables we need to determine the appropriate action
        HttpServletRequest httpServletRequest = (HttpServletRequest) request.getRequest();
        HttpServletResponse httpServletResponse = (HttpServletResponse) response.getResponse();

        int redirectPort = request.getConnector().getRedirectPort();

        // Is redirecting disabled?
        if (redirectPort <= 0) {
            LOG.fine("[Web-Security]  SSL redirect is disabled");
            httpServletResponse.sendError(SC_FORBIDDEN, encode(httpServletRequest.getRequestURI(), "UTF-8"));
            return false;
        }

        StringBuilder file = new StringBuilder(httpServletRequest.getRequestURI());
        String requestedSessionId = httpServletRequest.getRequestedSessionId();
        if (requestedSessionId != null && httpServletRequest.isRequestedSessionIdFromURL()) {
            file.append(";" + Globals.SESSION_PARAMETER_NAME + "=");
            file.append(requestedSessionId);
        }

        String queryString = httpServletRequest.getQueryString();
        if (queryString != null) {
            file.append('?');
            file.append(queryString);
        }

        List<String> hostAndPort = getHostAndPort(request);
        try {
            httpServletResponse
                .sendRedirect(
                    new URL("https", hostAndPort.get(0), Integer.parseInt((hostAndPort.get(1))), file.toString())
                        .toExternalForm());
            return false;
        } catch (MalformedURLException e) {
            httpServletResponse.sendError(SC_INTERNAL_SERVER_ERROR, encode(httpServletRequest.getRequestURI(), "UTF-8"));
            return false;
        }
    }

    private List<String> getHostAndPort(HttpRequest request) throws IOException {
        boolean isWebServerRequest = false;
        Enumeration<String> headerNames = ((HttpServletRequest) request.getRequest()).getHeaderNames();

        String[] hostPort = null;
        boolean isHeaderPresent = false;
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();

            if (headerName.equalsIgnoreCase("Host")) {
                String hostVal = ((HttpServletRequest) request.getRequest()).getHeader(headerName);
                isHeaderPresent = true;
                hostPort = hostVal.split(":");
            }
        }

        if (hostPort == null) {
            throw new ProtocolException(resourceBundle.getString("missing_http_header.host"));
        }

        // If the port in the Header is empty (it refers to the default port), which is
        // not one of the Payara listener ports -> Payara is front-ended by a proxy (LB plugin)

        boolean isHostPortNullOrEmpty = ((hostPort.length <= 1) || (hostPort[1] == null || hostPort[1].trim().isEmpty()));
        if (!isHeaderPresent) {
            isWebServerRequest = false;
        } else if (isHostPortNullOrEmpty) {
            isWebServerRequest = true;
        } else {
            boolean breakFromLoop = false;

            for (NetworkListener nwListener : nwListeners.getNetworkListener()) {
                // Loop through the network listeners
                String nwAddress = nwListener.getAddress();
                InetAddress[] localHostAdresses;
                if (nwAddress == null || nwAddress.equals("0.0.0.0")) {
                    nwAddress = NetUtils.getCanonicalHostName();
                    if (!nwAddress.equals(hostPort[0])) {

                        // compare the InetAddress objects
                        // only if the hostname in the header
                        // does not match with the hostname in the
                        // listener-To avoid performance overhead
                        localHostAdresses = NetUtils.getHostAddresses();

                        InetAddress hostAddress = InetAddress.getByName(hostPort[0]);
                        for (InetAddress inetAdress : localHostAdresses) {
                            if (inetAdress.equals(hostAddress)) {
                                // Hostname of the request in the listener and the hostname in the Host header match.
                                // Check the port
                                String nwPort = nwListener.getPort();
                                // If the listener port is different from the port
                                // in the Host header, then request is received by WS frontend
                                if (!nwPort.equals(hostPort[1])) {
                                    isWebServerRequest = true;

                                } else {
                                    isWebServerRequest = false;
                                    breakFromLoop = true;
                                    break;
                                }
                            }
                        }
                    } else {
                        // Host names are the same, compare the ports
                        String nwPort = nwListener.getPort();
                        // If the listener port is different from the port
                        // in the Host header, then request is received by WS frontend
                        if (!nwPort.equals(hostPort[1])) {
                            isWebServerRequest = true;

                        } else {
                            isWebServerRequest = false;
                            breakFromLoop = true;
                        }
                    }
                }

                if (breakFromLoop && !isWebServerRequest) {
                    break;
                }
            }
        }

        String serverHost = request.getRequest().getServerName();
        int redirectPort = request.getConnector().getRedirectPort();

        // If the request is a from a webserver frontend, redirect to the url
        // with the webserver frontend host and port
        if (isWebServerRequest) {
            serverHost = hostPort[0];
            if (isHostPortNullOrEmpty) {
                // Use the default port
                redirectPort = -1;
            } else {
                redirectPort = Integer.parseInt(hostPort[1]);
            }
        }

        return asList(serverHost, String.valueOf(redirectPort));
    }

    // START SJSAS 6232464
    // pass in HttpServletResponse instead of saving it as instance variable
    private String getCanonicalName(HttpRequest currentRequest) {
        return currentRequest.getWrapper().getServletName();
    }

    private String getResourceName(String uri, String contextPath) {
        if (contextPath.length() < uri.length()) {
            return uri.substring(contextPath.length());
        }

        return "";
    }

    private void logHasUserDataPermission(HttpServletRequest httpServletRequest) {
        LOG.fine(() -> "[Web-Security][ hasUserDataPermission ] Principal: " + httpServletRequest.getUserPrincipal()
            + " ContextPath: " + httpServletRequest.getContextPath());
    }

    private void logRequestSecure(HttpRequest request) {
        LOG.fine(() -> "[Web-Security] request.getRequest().isSecure(): " + request.getRequest().isSecure());
    }

    private void logSSLRedirect() {
        LOG.fine("[Web-Security] redirecting using SSL");
    }

    private void sendBadRequest(HttpResponse response, Exception e) throws IOException {
        LOG.log(WARNING, resourceBundle.getString("realmAdapter.badRequestWithId"), e);
        HttpServletResponse httpServletResponse  = (HttpServletResponse) response.getResponse();
        httpServletResponse.sendError(SC_BAD_REQUEST, resourceBundle.getString("realmAdapter.badRequest"));
    }

    private void sendForbidden(HttpResponse response) throws IOException {
        HttpServletResponse httpServletResponse  = (HttpServletResponse) response.getResponse();
        httpServletResponse.sendError(SC_FORBIDDEN, resourceBundle.getString("realmBase.forbidden"));
    }

    private void sendServiceUnavailable(HttpResponse response, Throwable e) throws IOException {
        LOG.log(SEVERE, "web_server.excep_authenticate_realmadapter", e);
        HttpServletResponse httpServletResponse  = (HttpServletResponse) response.getResponse();
        httpServletResponse.sendError(SC_SERVICE_UNAVAILABLE);
        response.setDetailMessage(resourceBundle.getString("realmBase.forbidden"));
    }

    private SecurityContext getSecurityContextForPrincipal(Principal principal) {
        if (principal == null) {
            return null;
        }

        if (principal instanceof WebPrincipal webPrincipal) {
            return webPrincipal.getSecurityContext();
        }

        Subject subject = new Subject();
        subject.getPrincipals().add(principal);
        return new SecurityContext(principal.getName(), subject);
    }

    public void setCurrentSecurityContextWithWebPrincipal(Principal principal) {
        if (principal instanceof WebPrincipal) {
            SecurityContext.setCurrent(getSecurityContextForPrincipal(principal));
        }
    }

    public void setCurrentSecurityContext(Principal principal) {
        SecurityContext.setCurrent(getSecurityContextForPrincipal(principal));
    }

    private Subject createSubjectWithCerts(X509Certificate[] certificates) {
        Subject subject = new Subject();
        // Specifically not using getName() as we aren't interested with the name here, we're interested in the X500Principal itself
        subject.getPublicCredentials().add(certificates[0].getSubjectX500Principal());
        subject.getPublicCredentials().add(asList(certificates));

        return subject;
    }

    @Override
    public void postConstruct() {
        nwListeners = networkConfig.getNetworkListeners();
    }

    private DigestAlgorithmParameter[] getDigestParameters(HttpServletRequest request) throws InvalidAlgorithmParameterException {
        return cNonceValidator.validateCnonce(
            DigestParameterGenerator
                .getInstance(HTTP_DIGEST)
                .generateParameters(new HttpAlgorithmParameterImpl(request)));
    }

    private Key getDigestKey(DigestAlgorithmParameter[] params) {
        for (DigestAlgorithmParameter dap : params) {
            if (A1.equals(dap.getName()) && dap instanceof Key) {
                return (Key) dap;
            }
        }

        throw new RuntimeException("No key found in parameters");
    }

    private boolean hasRequestPrincipal(HttpRequest request) {
        return ((HttpServletRequest) request).getUserPrincipal() != null;
    }

    private boolean isJakartaAuthenticationEnabled() throws IOException {
        try {
            return authenticationService != null && authenticationService.getServerAuthConfig() != null;
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    @FunctionalInterface
    public interface IOSupplier<T> {

        /**
         * Gets a result.
         *
         * @return a result
         */
        T get() throws IOException;
    }

    // inner class extends AuthenticatorBase such that session registration
    // of webtier can be invoked by RealmAdapter after authentication
    // by authentication module.
    static class AuthenticatorProxy extends AuthenticatorBase {

        private final AuthenticatorBase authBase;
        private final Principal principal;
        private final String authType;

        @Override
        public boolean getCache() {
            return authBase.getCache();
        }

        @Override
        public Container getContainer() {
            return authBase.getContainer();
        }

        AuthenticatorProxy(Authenticator authenticator, Principal p, String authType) throws LifecycleException {

            this.authBase = (AuthenticatorBase) authenticator;
            this.principal = p;
            this.authType = authType == null ? RealmAdapter.PROXY_AUTH_TYPE : authType;

            setCache(authBase.getCache());
            setContainer(authBase.getContainer());
            start(); // finds sso valve and sets its value in proxy
        }

        @Override
        public boolean authenticate(HttpRequest request, HttpResponse response, LoginConfig config) throws IOException {
            if (cache) {
                getSession(request, true);
            }

            register(request, response, this.principal, this.authType, this.principal.getName(), null);
            return true;
        }

        @Override
        public String getAuthMethod() {
            return authType;
        }
    }
}

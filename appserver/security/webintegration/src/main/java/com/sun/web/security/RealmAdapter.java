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
// Portions Copyright [2016-2020] [Payara Foundation and/or its affiliates]
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
import com.sun.enterprise.security.integration.RealmInitializer;
import com.sun.enterprise.security.jacc.JaccWebAuthorizationManager;
import com.sun.enterprise.security.jacc.context.PolicyContextHandlerImpl;
import com.sun.enterprise.security.web.integration.WebPrincipal;
import com.sun.enterprise.security.web.integration.WebSecurityManagerFactory;
import com.sun.enterprise.util.net.NetUtils;
import com.sun.logging.LogDomains;
import com.sun.web.security.realmadapter.AuthenticatorProxy;
import com.sun.web.security.realmadapter.JaspicRealm;
import fish.payara.nucleus.requesttracing.RequestTracingService;
import org.apache.catalina.*;
import org.apache.catalina.authenticator.AuthenticatorBase;
import org.apache.catalina.deploy.LoginConfig;
import org.apache.catalina.deploy.SecurityConstraint;
import org.apache.catalina.realm.RealmBase;
import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.grizzly.config.dom.NetworkConfig;
import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.grizzly.config.dom.NetworkListeners;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.internal.api.ServerContext;
import org.jvnet.hk2.annotations.Service;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import javax.security.auth.Subject;
import jakarta.security.auth.message.AuthException;
import jakarta.security.jacc.PolicyContext;
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
import java.security.AccessController;
import java.security.InvalidAlgorithmParameterException;
import java.security.Principal;
import java.security.PrivilegedAction;
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
import static com.sun.logging.LogDomains.WEB_LOGGER;
import static java.lang.String.format;
import static java.net.URLEncoder.encode;
import static java.security.AccessController.doPrivileged;
import static java.util.Arrays.asList;
import static java.util.logging.Level.*;
import static jakarta.servlet.http.HttpServletResponse.*;
import static org.apache.catalina.realm.Constants.FORM_ACTION;
import static org.apache.catalina.realm.Constants.FORM_METHOD;
import static org.glassfish.api.admin.ServerEnvironment.DEFAULT_INSTANCE_NAME;

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

    private WebBundleDescriptor webDescriptor;
    private HashMap<String, String> runAsPrincipals;

    // required for realm-per-app login
    private String realmName;

    /**
     * Descriptive information about this Realm implementation.
     */
    protected static final String name = "J2EE-RI-RealmAdapter";

    /**
     * The context Id value needed by JACC.
     */
    private String jaccContextId;

    /**
     * A <code>JaccWebAuthorizationManager</code> object associated with a jaccContextId
     */
    protected volatile JaccWebAuthorizationManager jaccWebAuthorizationManager;

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
     * The factory used for creating <code>JaccWebAuthorizationManager</code> object.
     */
    @Inject
    protected WebSecurityManagerFactory webSecurityManagerFactory;

    @Inject
    private RequestTracingService requestTracing;


    private NetworkListeners nwListeners;
    private JaspicRealm jaspicRealm;
    private CNonceValidator cNonceValidator;

    /**
     * ThreadLocal object to keep track of the reentrancy status of each thread. It contains a byte[] object whose single
     * element is either 0 (initial value or no reentrancy), or 1 (current thread is reentrant). When a thread exits the
     * implies method, byte[0] is always reset to 0.
     */
    private static ThreadLocal<byte[]> reentrancyStatus = ThreadLocal.withInitial(() ->  new byte[] { 0 });

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
        jaccContextId = JaccWebAuthorizationManager.getContextID(webDescriptor);
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
        jaspicRealm = new JaspicRealm(realmName, isSystemApp, webDescriptor, requestTracing);
        cNonceValidator = new CNonceValidator(webDescriptor, appCNonceCacheMapProvider, cNonceCacheFactoryProvider);
    }

    /**
     * Return <tt>true</tt> if JASPIC is available.
     *
     * @return <tt>true</tt> if JASPIC is available. 1171
     */
    @Override
    public boolean isSecurityExtensionEnabled(ServletContext context) {
        return jaspicRealm.isJaspicEnabled(context);
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
        if (!jaspicRealm.isInitialised()) {
            jaspicRealm.initJaspicServices(context.getServletContext());
        }

        JaccWebAuthorizationManager authorizationManager = getJaccWebAuthorizationManager(false);

        if (authorizationManager != null && authorizationManager.hasNoConstrainedResources() && !jaspicRealm.isJaspicEnabled(context.getServletContext())) {
            // No constraints
            return null;
        }

        // Constraints
        return emptyConstraints;
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

        JaccWebAuthorizationManager authorizationManager = getJaccWebAuthorizationManager(true);
        if (authorizationManager == null) {
            return false;
        }

        int isGranted = 0;
        try {
            isGranted = authorizationManager.hasUserDataPermission(httpServletRequest, uri, method);
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
    public int preAuthenticateCheck(HttpRequest request, HttpResponse response, SecurityConstraint[] constraints, boolean disableProxyCaching, boolean securePagesWithPragma, boolean ssoEnabled) throws IOException {
        boolean isGranted = false;

        try {
            if (!hasRequestPrincipal(request)) {
                setUnauthenticatedContext();
            }

            // JASPIC enabled; always give the SAM the opportunity to authenticate
            if (jaspicRealm.isJaspicEnabled()) {
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
                    if (!getJaccWebAuthorizationManager(true).isPermitAll(httpServletRequest)) {
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

    /**
     * Authenticates the user making this request, based on the specified authentication mechanism.
     *
     * Return <code>true</code> if any specified requirements have been satisfied, or <code>false</code>
     * if we have created a response challenge already.
     *
     * @param request Request we are processing
     * @param response Response we are creating
     * @param context The Context to which client of this class is attached.
     * @param authenticator the current authenticator.
     * @param calledFromAuthenticate if the calls to this method comes from a call to HttpServletRequest.authenticate
     * @return
     * @exception IOException if an input/output error occurs
     */
    @Override
    public boolean invokeAuthenticateDelegate(HttpRequest request, HttpResponse response, Context context, Authenticator authenticator, boolean calledFromAuthenticate) throws IOException {

        if (jaspicRealm.isJaspicEnabled()) {
            // JASPIC (JSR 196) is enabled for this application
            return jaspicRealm.validateRequest(request, response, context, authenticator, calledFromAuthenticate, e -> !getJaccWebAuthorizationManager(true).isPermitAll(e));
        }

        // JASPIC (JSR 196) is not enabled. Use the passed-in Catalina authenticator.
        return ((AuthenticatorBase) authenticator).authenticate(request, response, context.getLoginConfig());
    }

    @Override
    protected String getName() {
        return name;
    }

    @Override
    public String getRealmName() {
        return realmName;
    }


    /**
     * {@inheritDoc}
     *
     * @param container - must be an instance of {@link Container}
     */
    @Override
    public void setVirtualServer(Object container) {
        jaspicRealm.setVirtualServer((Container) container);
    }

    @Override
    public void updateWebSecurityManager() {
        if (jaccWebAuthorizationManager == null) {
            jaccWebAuthorizationManager = getJaccWebAuthorizationManager(true);
        }

        if (jaccWebAuthorizationManager != null) {
            try {
                jaccWebAuthorizationManager.release();
                jaccWebAuthorizationManager.destroy();
            } catch (Exception ex) {
                LOG.log(Level.SEVERE, "Failed to release and destroy the jaccWebAuthorizationManager", ex);
            }

            jaccWebAuthorizationManager = webSecurityManagerFactory.createManager(webDescriptor, true, serverContext);
            LOG.fine(() -> "JaccWebAuthorizationManager for " + jaccContextId + " has been updated");
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
        if (jaspicRealm.isJaspicEnabled()) {
            return jaspicRealm.secureResponse(request, response, context);
        }

        return false;
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
        JaccWebAuthorizationManager authorizationManager = getJaccWebAuthorizationManager(true);
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
        ServletContext servletContext = httpRequest.getRequest().getServletContext();

        byte[] alreadyCalled = reentrancyStatus.get();

        if (jaspicRealm.isJaspicEnabled(servletContext) && alreadyCalled[0] == 0) {
            alreadyCalled[0] = 1;

            try {
                jaspicRealm.cleanSubject(httpRequest);
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

        doPrivileged((PrivilegedAction<Void>) () -> {
            resetPolicyContext();
            return null;
        });
    }

    @Override
    public void destroy() {
        super.destroy();
        jaspicRealm.destroy();
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
    public JaccWebAuthorizationManager getJaccWebAuthorizationManager(boolean logNull) {
        if (jaccWebAuthorizationManager == null) {
            synchronized (this) {
                jaccWebAuthorizationManager = webSecurityManagerFactory.getManager(jaccContextId, null, false);
            }
            if (jaccWebAuthorizationManager == null && logNull) {
                LOG.log(WARNING, "realmAdapter.noWebSecMgr", jaccContextId);
            }
        }

        return jaccWebAuthorizationManager;
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
        JaccWebAuthorizationManager authorizationManager = getJaccWebAuthorizationManager(true);
        if (authorizationManager == null) {
            return false;
        }

        return authorizationManager.hasRoleRefPermission(servletName, role, principal);
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
    private boolean invokeWebSecurityManager(HttpRequest request, HttpResponse response, SecurityConstraint[] constraints) throws IOException {

        // Allow access to form login related pages and targets
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

                    // Get Context here as preAuthenticateCheck does not have it
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

        if (loginPage != null || errorPage != null) {
            String requestURI = request.getRequestPathMB().toString();
            LOG.fine(() -> "[Web-Security]  requestURI: " + requestURI + " loginPage: " + loginPage);
            if (loginPage != null && loginPage.equals(requestURI)) {
                LOG.fine(() -> " Allow access to login page " + loginPage);
                return true;
            } else if (errorPage != null && errorPage.equals(requestURI)) {
                LOG.fine(() -> " Allow access to error page " + errorPage);
                return true;
            } else if (requestURI.endsWith(FORM_ACTION)) {
                LOG.fine(" Allow access to username/password submission");
                return true;
            }
        }

        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        if (httpServletRequest.getServletPath() == null) {
            request.setServletPath(getResourceName(httpServletRequest.getRequestURI(), httpServletRequest.getContextPath()));
        }

        LOG.fine(() -> "[Web-Security] [ hasResourcePermission ] Principal: " + httpServletRequest.getUserPrincipal()
            + " ContextPath: " + httpServletRequest.getContextPath());

        JaccWebAuthorizationManager authorizationManager = getJaccWebAuthorizationManager(true);
        if (authorizationManager == null) {
            return false;
        }

        return authorizationManager.hasResourcePermission(httpServletRequest);
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

    private void resetPolicyContext() {
        ((PolicyContextHandlerImpl) PolicyContextHandlerImpl.getInstance()).reset();
        PolicyContext.setContextID(null);
    }

    private SecurityContext getSecurityContextForPrincipal(Principal principal) {
        if (principal == null) {
            return null;
        }

        if (principal instanceof WebPrincipal) {
            return ((WebPrincipal) principal).getSecurityContext();
        }

        return AccessController.doPrivileged(new PrivilegedAction<SecurityContext>() {

            @Override
            public SecurityContext run() {
                Subject subject = new Subject();
                subject.getPrincipals().add(principal);
                return new SecurityContext(principal.getName(), subject);
            }
        });

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

    @FunctionalInterface
    public interface IOSupplier<T> {

        /**
         * Gets a result.
         *
         * @return a result
         */
        T get() throws IOException;
    }
}

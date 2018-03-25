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
// Portions Copyright [2016-2018] [Payara Foundation and/or its affiliates]

package com.sun.web.security;

import static com.sun.enterprise.security.auth.digest.api.Constants.A1;
import static com.sun.enterprise.security.auth.digest.impl.DigestParameterGenerator.HTTP_DIGEST;
import static com.sun.enterprise.security.jmac.config.HttpServletConstants.AUTH_TYPE;
import static com.sun.enterprise.security.jmac.config.HttpServletConstants.IS_MANDATORY;
import static com.sun.enterprise.security.jmac.config.HttpServletConstants.REGISTER_SESSION;
import static com.sun.enterprise.security.jmac.config.HttpServletConstants.REGISTER_WITH_AUTHENTICATOR;
import static com.sun.enterprise.security.jmac.config.HttpServletConstants.WEB_BUNDLE;
import static com.sun.logging.LogDomains.WEB_LOGGER;
import static java.lang.Boolean.TRUE;
import static java.net.URLEncoder.encode;
import static java.security.AccessController.doPrivileged;
import static java.util.Arrays.asList;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.FINEST;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;
import static javax.security.auth.message.AuthStatus.SUCCESS;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_SERVICE_UNAVAILABLE;
import static org.apache.catalina.ContainerEvent.AFTER_AUTHENTICATION;
import static org.apache.catalina.ContainerEvent.AFTER_LOGOUT;
import static org.apache.catalina.ContainerEvent.AFTER_POST_AUTHENTICATION;
import static org.apache.catalina.ContainerEvent.BEFORE_AUTHENTICATION;
import static org.apache.catalina.ContainerEvent.BEFORE_LOGOUT;
import static org.apache.catalina.ContainerEvent.BEFORE_POST_AUTHENTICATION;
import static org.apache.catalina.Globals.WRAPPED_REQUEST;
import static org.apache.catalina.Globals.WRAPPED_RESPONSE;
import static org.apache.catalina.realm.Constants.FORM_ACTION;
import static org.apache.catalina.realm.Constants.FORM_METHOD;
import static org.glassfish.api.admin.ServerEnvironment.DEFAULT_INSTANCE_NAME;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
/*V3:Comment
import com.sun.enterprise.webservice.monitoring.WebServiceEngineImpl;
import com.sun.enterprise.webservice.monitoring.AuthenticationListener;
 */
import java.security.AccessController;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.security.auth.Subject;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.config.ServerAuthConfig;
import javax.security.auth.message.config.ServerAuthContext;
import javax.security.jacc.PolicyContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Authenticator;
import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Globals;
import org.apache.catalina.HttpRequest;
import org.apache.catalina.HttpResponse;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Realm;
import org.apache.catalina.authenticator.AuthenticatorBase;
import org.apache.catalina.connector.RequestFacade;
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
import org.glassfish.security.common.CNonceCache;
import org.glassfish.security.common.NonceInfo;
import org.jvnet.hk2.annotations.Service;

//import com.sun.enterprise.Switch;
import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.RunAsIdentityDescriptor;
import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.deployment.WebComponentDescriptor;
//import org.glassfish.deployment.common.SecurityRoleMapper;
import com.sun.enterprise.deployment.web.LoginConfiguration;
import com.sun.enterprise.security.AppCNonceCacheMap;
import com.sun.enterprise.security.CNonceCacheFactory;
import com.sun.enterprise.security.SecurityContext;
import com.sun.enterprise.security.WebSecurityDeployerProbeProvider;
import com.sun.enterprise.security.auth.digest.api.DigestAlgorithmParameter;
import com.sun.enterprise.security.auth.digest.api.Key;
import com.sun.enterprise.security.auth.digest.impl.DigestParameterGenerator;
import com.sun.enterprise.security.auth.digest.impl.HttpAlgorithmParameterImpl;
import com.sun.enterprise.security.auth.digest.impl.NestedDigestAlgoParamImpl;
import com.sun.enterprise.security.auth.login.DigestCredentials;
import com.sun.enterprise.security.auth.login.LoginContextDriver;
import com.sun.enterprise.security.authorize.PolicyContextHandlerImpl;
import com.sun.enterprise.security.ee.SecurityUtil;
import com.sun.enterprise.security.integration.RealmInitializer;
import com.sun.enterprise.security.jmac.config.HttpServletHelper;
import com.sun.enterprise.security.web.integration.WebPrincipal;
import com.sun.enterprise.security.web.integration.WebSecurityManager;
import com.sun.enterprise.security.web.integration.WebSecurityManagerFactory;
import com.sun.enterprise.util.net.NetUtils;
import com.sun.logging.LogDomains;

import fish.payara.nucleus.requesttracing.RequestTracingService;
import fish.payara.nucleus.requesttracing.domain.RequestTraceSpan;

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

    private static final String SERVER_AUTH_CONTEXT = "__javax.security.auth.message.ServerAuthContext";
    private static final String MESSAGE_INFO = "__javax.security.auth.message.MessageInfo";
    private static final String PROXY_AUTH_TYPE = "PLUGGABLE_PROVIDER";

    private static final Logger logger = LogDomains.getLogger(RealmAdapter.class, WEB_LOGGER);
    private static final ResourceBundle resourceBundle = logger.getResourceBundle();
    private static final WebSecurityDeployerProbeProvider websecurityProbeProvider = new WebSecurityDeployerProbeProvider();

    // name of system property that can be used to define
    // corresponding default provider for system apps.
    private static final String SYSTEM_HTTPSERVLET_SECURITY_PROVIDER = "system_httpservlet_security_provider";

    private WebBundleDescriptor webDescriptor;
    private HashMap<String, String> runAsPrincipals;

    // required for realm-per-app login
    private String realmName;

    /**
     * Descriptive information about this Realm implementation.
     */
    protected static final String name = "J2EE-RI-RealmAdapter";

    /**
     * The context Id value needed by the jacc architecture.
     */
    private String CONTEXT_ID;
    private Container virtualServer;

    /**
     * A <code>WebSecurityManager</code> object associated with a CONTEXT_ID
     */
    protected volatile WebSecurityManager webSecurityManager;

    protected boolean isCurrentURIincluded;

    /*
     * The following fields are used to implement a bypass of FBL related targets
     */
    protected final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private boolean contextEvaluated;
    private String loginPage;
    private String errorPage;
    private final static SecurityConstraint[] emptyConstraints = new SecurityConstraint[] {};

    /**
     * The default provider id for system apps if one has been established. the default provider for system apps is
     * established by defining a system property.
     */
    private static String defaultSystemProviderID = getDefaultSystemProviderID();

    private String moduleID;
    private boolean isSystemApp;
    private HttpServletHelper httpServletHelper;

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
     * The factory used for creating <code>WebSecurityManager</code> object.
     */
    @Inject
    protected WebSecurityManagerFactory webSecurityManagerFactory;

    @Inject
    RequestTracingService requestTracing;

    private CNonceCacheFactory cNonceCacheFactory;
    private CNonceCache cnonces;
    private AppCNonceCacheMap haCNonceCacheMap;

    private NetworkListeners nwListeners;

    /**
     * ThreadLocal object to keep track of the reentrancy status of each thread. It contains a byte[] object whose single
     * element is either 0 (initial value or no reentrancy), or 1 (current thread is reentrant). When a thread exits the
     * implies method, byte[0] is always reset to 0.
     */
    private static ThreadLocal<byte[]> reentrancyStatus;

    static {
        reentrancyStatus = new ThreadLocal<byte[]>() {

            @Override
            protected synchronized byte[] initialValue() {
                return new byte[] { 0 };
            }
        };
    }

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

    @Override
    public void initializeRealm(Object descriptor, boolean isSystemApp, String realmName) {
        this.isSystemApp = isSystemApp;
        webDescriptor = (WebBundleDescriptor) descriptor;

        computeRealmName(webDescriptor, realmName);

        CONTEXT_ID = WebSecurityManager.getContextID(webDescriptor);

        runAsPrincipals = new HashMap<String, String>();

        for (WebComponentDescriptor componentDescriptor : webDescriptor.getWebComponentDescriptors()) {
            RunAsIdentityDescriptor runAsDescriptor = componentDescriptor.getRunAsIdentity();

            if (runAsDescriptor != null) {
                String principal = runAsDescriptor.getPrincipal();
                String servlet = componentDescriptor.getCanonicalName();

                if (principal == null || servlet == null) {
                    logger.warning("web.realmadapter.norunas");
                } else {
                    runAsPrincipals.put(servlet, principal);
                    logger.fine("Servlet " + servlet + " will run-as: " + principal);
                }
            }
        }

        this.moduleID = webDescriptor.getModuleID();
    }

    /**
     * Return a short name for this Realm Adapter implementation.
     */
    @Override
    protected String getName() {
        return name;
    }

    /**
     * Return the name of the realm this RealmAdapter uses.
     *
     * @return realm name
     *
     */
    @Override
    public String getRealmName() {
        return realmName;
    }

    public void setRealmName(String realmName) {
        // do nothing since this is done when initializing the Realm.
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

    public WebBundleDescriptor getWebDescriptor() {
        return webDescriptor;
    }

    /**
     * Utility method to get the web security manager.
     * 
     * <p>
     * This will log a warning if the manager is not found in the factory, and logNull is true.
     * 
     */
    public WebSecurityManager getWebSecurityManager(boolean logNull) {
        if (webSecurityManager == null) {

            synchronized (this) {
                webSecurityManager = webSecurityManagerFactory.getManager(CONTEXT_ID, null, false);
            }

            if (webSecurityManager == null && logNull) {
                logger.log(WARNING, "realmAdapter.noWebSecMgr", CONTEXT_ID);
            }
        }

        return webSecurityManager;
    }

    @Override
    public void updateWebSecurityManager() {
        if (webSecurityManager == null) {
            webSecurityManager = getWebSecurityManager(true);
        }

        if (webSecurityManager != null) {
            try {
                webSecurityManager.release();
                webSecurityManager.destroy();
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            webSecurityManager = webSecurityManagerFactory.createManager(webDescriptor, true, serverContext);

            if (logger.isLoggable(FINE)) {
                logger.fine("WebSecurityManager for " + CONTEXT_ID + " has been updated");
            }
        }
    }

    /**
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
        if (httpServletHelper == null) {
            initConfigHelper(context.getServletContext());
        }

        WebSecurityManager securityManager = getWebSecurityManager(false);

        if (securityManager != null && securityManager.hasNoConstrainedResources()
                && !isSecurityExtensionEnabled(context.getServletContext())) {
            return null;
        }

        return emptyConstraints;
    }

    /**
     * Returns null 1. if there are no security constraints defined on any of the web resources within the context, or 2. if
     * the target is a form login related page or target.
     * 
     * <p>
     * See SJSAS 6232464 6202703
     *
     * otherwise return an empty array of SecurityConstraint.
     */
    @Override
    public SecurityConstraint[] findSecurityConstraints(String requestPathMB, String httpMethod, Context context) {
        if (httpServletHelper == null) {
            initConfigHelper(context.getServletContext());
        }

        WebSecurityManager securityManager = getWebSecurityManager(false);

        if (securityManager != null && securityManager.hasNoConstrainedResources()
                && !isSecurityExtensionEnabled(context.getServletContext())) {
            return null;
        }

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
     * If <tt>uri</tt> and <tt>method</tt> are null, then the URI and method of the given <tt>request</tt> are checked.
     *
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
    public boolean hasUserDataPermission(HttpRequest request, HttpResponse response, SecurityConstraint[] constraints, String uri,
            String method) throws IOException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        if (httpServletRequest.getServletPath() == null) {
            request.setServletPath(getResourceName(httpServletRequest.getRequestURI(), httpServletRequest.getContextPath()));
        }

        if (logger.isLoggable(FINE)) {
            logger.fine("[Web-Security][ hasUserDataPermission ] Principal: " + httpServletRequest.getUserPrincipal() + " ContextPath: "
                    + httpServletRequest.getContextPath());
        }

        if (request.getRequest().isSecure()) {
            if (logger.isLoggable(FINE)) {
                logger.fine("[Web-Security] request.getRequest().isSecure(): " + request.getRequest().isSecure());
            }
            return true;
        }

        WebSecurityManager securityManager = getWebSecurityManager(true);
        if (securityManager == null) {
            return false;
        }

        int isGranted = 0;
        try {
            isGranted = securityManager.hasUserDataPermission(httpServletRequest, uri, method);
        } catch (IllegalArgumentException e) {
            // End the request after getting IllegalArgumentException while checking user data permission
            logger.log(WARNING, resourceBundle.getString("realmAdapter.badRequestWithId"), e);
            ((HttpServletResponse) response.getResponse())
                    .sendError(SC_BAD_REQUEST, resourceBundle.getString("realmAdapter.badRequest"));

            return false;
        }

        // Only redirect if we are sure the user will be granted.
        // See bug 4947698

        // This method will return:
        // 1 - if granted
        // 0 - if not granted
        // -1 - if the current transport is not granted, but a redirection can occur
        // so the grand will succeed.
        if (isGranted == -1) {
            if (logger.isLoggable(FINE)) {
                logger.fine("[Web-Security] redirecting using SSL");
            }
            return redirect(request, response);
        }

        if (isGranted == 0) {
            ((HttpServletResponse) response.getResponse()).sendError(SC_FORBIDDEN, resourceBundle.getString("realmBase.forbidden"));
            return false;
        }

        return true;
    }

    /**
     * Checks whether or not authentication is needed. Returns an int, one of AUTHENTICATE_NOT_NEEDED, AUTHENTICATE_NEEDED,
     * or AUTHENTICATED_NOT_AUTHORIZED
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
            HttpServletRequest httpServletRequest = (HttpServletRequest) request.getRequest();
            if (httpServletRequest.getUserPrincipal() == null) {
                SecurityContext.setUnauthenticatedContext();
            }

            if (httpServletHelper != null && httpServletHelper.getServerAuthConfig() != null) {
                return AUTHENTICATE_NEEDED;
            }

            isGranted = invokeWebSecurityManager(request, response, constraints);
        } catch (IOException iex) {
            throw iex;
        } catch (Throwable ex) {
            logger.log(SEVERE, "web_server.excep_authenticate_realmadapter", ex);
            ((HttpServletResponse) response.getResponse()).sendError(SC_SERVICE_UNAVAILABLE);
            response.setDetailMessage(resourceBundle.getString("realmBase.forbidden"));

            return AUTHENTICATED_NOT_AUTHORIZED;
        }

        if (isGranted) {

            if (((HttpServletRequest) request).getUserPrincipal() != null) {
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

        if (((HttpServletRequest) request).getUserPrincipal() != null) {
            ((HttpServletResponse) response.getResponse()).sendError(SC_FORBIDDEN);
            response.setDetailMessage(resourceBundle.getString("realmBase.forbidden"));

            return AUTHENTICATED_NOT_AUTHORIZED;
        }

        disableProxyCaching(request, response, disableProxyCaching, securePagesWithPragma);

        return Realm.AUTHENTICATE_NEEDED;
    }

    /**
     * Authenticates the user making this request, based on the specified login configuration. Return <code>true</code> if
     * any specified requirements have been satisfied, or <code>false</code> if we have created a response challenge
     * already.
     *
     * @param request Request we are processing
     * @param response Response we are creating
     * @param context The Context to which client of this class is attached.
     * @param authenticator the current authenticator.
     * @param calledFromAuthenticate
     * @return
     * @exception IOException if an input/output error occurs
     */
    @Override
    public boolean invokeAuthenticateDelegate(HttpRequest request, HttpResponse response, Context context, Authenticator authenticator,
            boolean calledFromAuthenticate) throws IOException {
        boolean result = false;

        LoginConfig loginConfig = context.getLoginConfig();
        ServerAuthConfig serverAuthConfig = getServerAuthConfig();

        if (serverAuthConfig != null) {

            // JASPIC (JSR 196) is enabled for this application

            try {
                context.fireContainerEvent(BEFORE_AUTHENTICATION, null);

                // Get the WebPrincipal principal and add to the security context principals
                RequestFacade requestFacade = (RequestFacade) request.getRequest();
                setAdditionalPrincipalInContext(requestFacade);

                if (requestTracing != null && requestTracing.isRequestTracingEnabled()) {
                    result = doTraced(serverAuthConfig, context, requestFacade,
                            () -> validate(request, response, loginConfig, authenticator, calledFromAuthenticate));
                } else {
                    result = validate(request, response, loginConfig, authenticator, calledFromAuthenticate);
                }
            } finally {
                resetAdditionalPrincipalInContext();
                context.fireContainerEvent(AFTER_AUTHENTICATION, null);
            }
        } else {
            // JSR 196 is not enabled. Use the current authenticator.
            result = ((AuthenticatorBase) authenticator).authenticate(request, response, loginConfig);
        }

        return result;
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
        if (logger.isLoggable(FINE)) {
            logger.fine("Tomcat callback for authenticate user/password");
            logger.fine("usename = " + username);
        }

        if (authenticate(username, password, null)) {
            return new WebPrincipal(username, password, SecurityContext.getCurrent());
        }

        return null;
    }

    /**
     * This HttpServletRequest authenticate variant is primarily used by the DigestAuthenticator
     */
    @Override
    public Principal authenticate(HttpServletRequest httpServletRequest) {
        try {
            DigestAlgorithmParameter[] params = DigestParameterGenerator.getInstance(HTTP_DIGEST)
                    .generateParameters(
                            new HttpAlgorithmParameterImpl(httpServletRequest));
            Key key = null;

            if (cnonces == null) {
                String appName = webDescriptor.getApplication().getAppName();

                synchronized (this) {
                    if (haCNonceCacheMap == null) {
                        haCNonceCacheMap = appCNonceCacheMapProvider.get();
                    }

                    if (haCNonceCacheMap != null) {
                        // get the initialized HA CNonceCache
                        cnonces = haCNonceCacheMap.get(appName);
                    }

                    if (cnonces == null) {
                        if (cNonceCacheFactory == null) {
                            cNonceCacheFactory = cNonceCacheFactoryProvider.get();
                        }

                        // create a Non-HA CNonce Cache
                        cnonces = cNonceCacheFactory.createCNonceCache(webDescriptor.getApplication().getAppName(), null, null, null);
                    }
                }
            }

            String nc = null;
            String cnonce = null;

            for (DigestAlgorithmParameter p : params) {
                if (p instanceof NestedDigestAlgoParamImpl) {
                    NestedDigestAlgoParamImpl np = (NestedDigestAlgoParamImpl) p;
                    DigestAlgorithmParameter[] nps = (DigestAlgorithmParameter[]) np.getNestedParams();
                    for (DigestAlgorithmParameter p1 : nps) {
                        if ("cnonce".equals(p1.getName())) {
                            cnonce = new String(p1.getValue());
                        } else if ("nc".equals(p1.getName())) {
                            nc = new String(p1.getValue());
                        }
                        if (cnonce != null && nc != null) {
                            break;
                        }
                    }
                    if (cnonce != null && nc != null) {
                        break;
                    }
                }
                if ("cnonce".equals(p.getName())) {
                    cnonce = new String(p.getValue());
                } else if ("nc".equals(p.getName())) {
                    nc = new String(p.getValue());
                }
            }

            long count;
            long currentTime = System.currentTimeMillis();
            try {
                count = Long.parseLong(nc, 16);
            } catch (NumberFormatException nfe) {
                throw new RuntimeException(nfe);
            }

            NonceInfo info;
            synchronized (cnonces) {
                info = cnonces.get(cnonce);
            }

            if (info == null) {
                info = new NonceInfo();
            } else {
                if (count <= info.getCount()) {
                    throw new RuntimeException("Invalid Request : Possible Replay Attack detected ?");
                }
            }

            info.setCount(count);
            info.setTimestamp(currentTime);
            synchronized (cnonces) {
                cnonces.put(cnonce, info);
            }

            for (int i = 0; i < params.length; i++) {
                DigestAlgorithmParameter dap = params[i];
                if (A1.equals(dap.getName()) && (dap instanceof Key)) {
                    key = (Key) dap;
                    break;
                }
            }

            if (key != null) {
                DigestCredentials creds = new DigestCredentials(realmName, key.getUsername(), params);
                LoginContextDriver.login(creds);

                return new WebPrincipal(creds.getUserName(), (char[]) null, SecurityContext.getCurrent());
            }

            throw new RuntimeException("No key found in parameters");

        } catch (Exception le) {
            if (logger.isLoggable(WARNING)) {
                logger.log(WARNING, "web.login.failed", le.toString());
            }
        }

        return null;
    }

    /**
     * This HttpServletRequest authenticate variant is primarily used by the SSLAuthenticator
     */
    @Override
    public Principal authenticate(X509Certificate certs[]) {
        if (authenticate(null, null, certs)) {
            return new WebPrincipal(certs, SecurityContext.getCurrent());
        }

        return null;
    }

    /**
     * Used by SecurityServiceImpl
     */
    public boolean authenticate(WebPrincipal principal) {
        if (principal.isUsingCertificate()) {
            return authenticate(null, null, principal.getCertificates());
        }

        return authenticate(principal.getName(), principal.getPassword(), null);
    }

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
    private boolean authenticate(String username, char[] password, X509Certificate[] certs) {
        try {
            if (certs != null) {

                // Certificate credential used to authenticate

                LoginContextDriver.doX500Login(createSubjectWithCerts(certs), moduleID);
            } else {

                // Username/password credential used to authenticate

                LoginContextDriver.login(username, password, realmName);
            }

            if (logger.isLoggable(FINE)) {
                logger.log(FINE, "Web login succeeded for: " + username);
            }

            return true;

        } catch (Exception le) {
            if (logger.isLoggable(WARNING)) {
                logger.log(WARNING, "web.login.failed", le.toString());
                if (logger.isLoggable(FINE)) {
                    logger.log(FINE, "Exception", le);
                }
            }
        }

        return false;
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
        logger.log(FINEST, "IN createFailOveredPrincipal ({0})", username);

        // Set the appropriate security context
        loginForRunAs(username);
        SecurityContext securityContext = SecurityContext.getCurrent();
        logger.log(FINE, "Security context is {0}", securityContext);

        Principal principal = new WebPrincipal(username, (char[]) null, securityContext);
        logger.log(INFO, "Principal created for FailOvered user {0}", principal);

        return principal;
    }

    /**
     * Perform access control based on the specified authorization constraint. Return <code>true</code> if this constraint
     * is satisfied and processing should continue, or <code>false</code> otherwise.
     *
     * @param request Request we are processing
     * @param response Response we are creating
     * @param constraint Security constraint we are enforcing
     * @param The Context to which client of this class is attached.
     *
     * @exception IOException if an input/output error occurs
     */
    @Override
    public boolean hasResourcePermission(HttpRequest request, HttpResponse response, SecurityConstraint[] constraints, Context context)
            throws IOException {
        boolean isGranted = false;

        try {
            isGranted = invokeWebSecurityManager(request, response, constraints);
        } catch (IOException iex) {
            throw iex;
        } catch (Throwable ex) {
            logger.log(SEVERE, "web_server.excep_authenticate_realmadapter", ex);
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
            if (httpServletHelper != null) {
                HttpServletRequest req = (HttpServletRequest) request.getRequest();
                MessageInfo messageInfo = (MessageInfo) req.getAttribute(MESSAGE_INFO);
                if (messageInfo != null) {

                    // JSR 196 is enabled for this application
                    serverAuthContext = (ServerAuthContext) messageInfo.getMap().get(SERVER_AUTH_CONTEXT);
                    if (serverAuthContext != null) {
                        try {
                            context.fireContainerEvent(BEFORE_POST_AUTHENTICATION, null);
                            result = SUCCESS.equals(serverAuthContext.secureResponse(messageInfo, null));
                        } finally {
                            context.fireContainerEvent(AFTER_POST_AUTHENTICATION, null);
                        }
                    }
                }
            }
        } catch (AuthException ex) {
            throw new IOException(ex);
        } finally {
            if (httpServletHelper != null && serverAuthContext != null) {
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
    // START OF SJSAS 6232464
    @Override
    public boolean hasRole(HttpRequest request, HttpResponse response, Principal principal, String role) {
        WebSecurityManager securityManager = getWebSecurityManager(true);
        if (securityManager == null) {
            return false;
        }

        // add HttpResponse and HttpResponse to the parameters, and remove
        // instance variable currentRequest from this class. References to
        // this.currentRequest are also removed from other methods.
        // String servletName = getResourceName( currentRequest.getRequestURI(),
        // currentRequest.getContextPath());
        String servletName = getCanonicalName(request);

        // END S1AS8PE 4966609
        boolean isGranted = securityManager.hasRoleRefPermission(servletName, role, principal);

        if (logger.isLoggable(FINE)) {
            logger.fine(
                    "Checking if servlet " + servletName + " with principal " + principal +
                            " has role " + role + " isGranted: " + isGranted);
        }

        return isGranted;
    }

    public boolean hasRole(String servletName, Principal principal, String role) {
        WebSecurityManager securityManager = getWebSecurityManager(true);
        if (securityManager == null) {
            return false;
        }

        return securityManager.hasRoleRefPermission(servletName, role, principal);
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

            if (logger.isLoggable(Level.FINE)) {
                logger.fine("run-as principal for " + servletName + " set to: " + runAs);
            }
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

    @Override
    @SuppressWarnings("unchecked")
    public void logout(HttpRequest httpRequest) {
        boolean securityExtensionEnabled = isSecurityExtensionEnabled(httpRequest.getRequest().getServletContext());
        byte[] alreadyCalled = reentrancyStatus.get();

        if (securityExtensionEnabled && httpServletHelper != null && alreadyCalled[0] == 0) {
            alreadyCalled[0] = 1;

            MessageInfo messageInfo = (MessageInfo) httpRequest.getRequest().getAttribute(MESSAGE_INFO);
            if (messageInfo == null) {
                messageInfo = new HttpMessageInfo(
                        (HttpServletRequest) httpRequest.getRequest(),
                        (HttpServletResponse) httpRequest.getResponse().getResponse());
            }

            messageInfo.getMap().put(IS_MANDATORY, TRUE.toString());

            try {
                ServerAuthContext serverAuthContext = httpServletHelper.getServerAuthContext(messageInfo, null);
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

        if (httpServletHelper != null) {
            httpServletHelper.disable();
        }
    }

    // ### Private methods

    private void computeRealmName(WebBundleDescriptor webDescriptor, String realmName) {
        Application application = webDescriptor.getApplication();
        LoginConfiguration loginConfig = webDescriptor.getLoginConfiguration();
        this.realmName = application.getRealm();
        if (this.realmName == null && loginConfig != null) {
            this.realmName = loginConfig.getRealmName();
        }
        if (realmName != null && (this.realmName == null || this.realmName.equals(""))) {
            this.realmName = realmName;
        }
    }

    private void doLogout(HttpRequest request, boolean extensionEnabled) {
        Context context = request.getContext();

        Authenticator authenticator = null;
        if (context != null) {
            authenticator = context.getAuthenticator();
        }

        if (authenticator == null) {
            throw new RuntimeException("Context or Authenticator is null");
        }

        try {
            if (extensionEnabled) {
                AuthenticatorProxy proxy = new AuthenticatorProxy(authenticator, null, null);
                proxy.logout(request);
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
        LoginContextDriver.loginPrincipal(principal, realmName);
    }

    private SecurityContext getSecurityContext() {
        return SecurityContext.getCurrent();
    }

    private void setSecurityContext(SecurityContext securityContext) {
        SecurityContext.setCurrent(securityContext);
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

    @Override
    protected char[] getPassword(String username) {
        throw new IllegalStateException("Should not reach here");
    }

    @Override
    protected Principal getPrincipal(String username) {
        throw new IllegalStateException("Should not reach here");
    }

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
    private boolean invokeWebSecurityManager(HttpRequest request, HttpResponse response, SecurityConstraint[] constraints)
            throws IOException {

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

            if (logger.isLoggable(FINE)) {
                logger.fine("[Web-Security]  requestURI: " + requestURI + " loginPage: " + loginPage);
            }

            if (loginPage != null && loginPage.equals(requestURI)) {
                if (logger.isLoggable(FINE)) {
                    logger.fine(" Allow access to login page " + loginPage);
                }

                return true;
            } else if (errorPage != null && errorPage.equals(requestURI)) {
                if (logger.isLoggable(FINE)) {
                    logger.fine(" Allow access to error page " + errorPage);
                }

                return true;
            } else if (requestURI.endsWith(FORM_ACTION)) {
                if (logger.isLoggable(FINE)) {
                    logger.fine(" Allow access to username/password submission");
                }

                return true;
            }
        }

        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        if (httpServletRequest.getServletPath() == null) {
            request.setServletPath(getResourceName(httpServletRequest.getRequestURI(), httpServletRequest.getContextPath()));
        }

        if (logger.isLoggable(FINE)) {
            logger.fine("[Web-Security] [ hasResourcePermission ] Principal: " + httpServletRequest.getUserPrincipal() + " ContextPath: "
                    + httpServletRequest.getContextPath());
        }

        WebSecurityManager securityManager = getWebSecurityManager(true);

        if (securityManager == null) {
            return false;
        }

        return securityManager.hasResourcePermission(httpServletRequest);
    }

    private boolean redirect(HttpRequest request, HttpResponse response) throws IOException {
        // Initialize variables we need to determine the appropriate action
        HttpServletRequest httpServletRequest = (HttpServletRequest) request.getRequest();
        HttpServletResponse httpServletResponse = (HttpServletResponse) response.getResponse();

        int redirectPort = request.getConnector().getRedirectPort();

        // Is redirecting disabled?
        if (redirectPort <= 0) {
            if (logger.isLoggable(INFO)) {
                logger.fine("[Web-Security]  SSL redirect is disabled");
            }

            httpServletResponse.sendError(SC_FORBIDDEN, encode(httpServletRequest.getRequestURI(), "UTF-8"));

            return false;
        }

        StringBuffer file = new StringBuffer(httpServletRequest.getRequestURI());

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
                    .sendRedirect(new URL("https", hostAndPort.get(0), Integer.parseInt((hostAndPort.get(1))), file.toString()).toString());
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

    private boolean doTraced(ServerAuthConfig serverAuthConfig, Context context, RequestFacade requestFacade, IOSupplier<Boolean> supplier)
            throws IOException {
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

    private ServerAuthConfig getServerAuthConfig() throws IOException {
        ServerAuthConfig serverAuthConfig = null;
        try {
            if (httpServletHelper != null) {
                serverAuthConfig = httpServletHelper.getServerAuthConfig();
            }
        } catch (Exception ex) {
            throw new IOException(ex);
        }

        return serverAuthConfig;
    }

    private ServerAuthContext getServerAuthContext(MessageInfo messageInfo) throws AuthException {
        ServerAuthContext authContext = httpServletHelper.getServerAuthContext(messageInfo, null); // null serviceSubject

        if (authContext == null) {
            throw new AuthException("null ServerAuthContext");
        }

        return authContext;
    }

    /**
     * Return <tt>true</tt> if a Security Extension is available.
     * 
     * @return <tt>true</tt> if a Security Extension is available. 1171
     */
    @Override
    public boolean isSecurityExtensionEnabled(final ServletContext context) {
        if (httpServletHelper == null) {
            initConfigHelper(context);
        }

        try {
            return httpServletHelper.getServerAuthConfig() != null;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * This must be invoked after virtualServer is set.
     */
    private HttpServletHelper getConfigHelper(final ServletContext servletContext) {
        Map<String, WebBundleDescriptor> map = new HashMap<>();
        map.put(WEB_BUNDLE, webDescriptor);

        return new HttpServletHelper(
                getAppContextID(servletContext), map, null, // null handler
                realmName, isSystemApp, defaultSystemProviderID);
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

    private boolean validate(HttpRequest request, HttpResponse response, LoginConfig config, Authenticator authenticator,
            boolean calledFromAuthenticate) throws IOException {

        HttpServletRequest servletRequest = (HttpServletRequest) request.getRequest();
        HttpServletResponse servletResponse = (HttpServletResponse) response.getResponse();

        Subject subject = new Subject();

        MessageInfo messageInfo = new HttpMessageInfo(servletRequest, servletResponse);

        boolean isValidateSuccess = false;
        boolean isMandatory = true;
        try {
            isMandatory = !getWebSecurityManager(true).permitAll(servletRequest);

            // Issue - 9578 - produce user challenge if call originates from HttpServletRequest.authenticate
            if (isMandatory || calledFromAuthenticate) {
                setMandatory(messageInfo);
            }

            ServerAuthContext authContext = getServerAuthContext(messageInfo);

            // Call the JASPIC ServerAuthContext which should eventually call the ServerAuthModule (SAM)

            // Notice a null is passed in as the service subject
            // Additionally notice we only care about SUCCESS being returned or not and ignore
            // all other JASPIC AuthStatus values.

            isValidateSuccess = SUCCESS.equals(authContext.validateRequest(messageInfo, subject, null));

            if (isValidateSuccess) { // store it only if validateRequest = true
                storeInRequest(servletRequest, messageInfo, authContext);
            }

        } catch (AuthException ae) {
            logger.log(WARNING, "JMAC: http msg authentication fail", ae);
            servletResponse.setStatus(SC_INTERNAL_SERVER_ERROR);
        } catch (RuntimeException e) {
            logger.log(WARNING, "JMAC: Exception during validateRequest", e);
            servletResponse.sendError(SC_INTERNAL_SERVER_ERROR);
        }

        if (isValidateSuccess) {
            Set<Principal> principalSet = subject.getPrincipals();

            // Must be at least one new principal to establish non-default security context
            if (hasNewPrincipal(principalSet)) {

                SecurityContext securityContext = new SecurityContext(subject);

                // Assuming no null principal here
                Principal callerPrincipal = securityContext.getCallerPrincipal();
                WebPrincipal webPrincipal = new WebPrincipal(callerPrincipal, securityContext);

                // TODO: check Java SE SecurityManager access
                SecurityContext.setCurrent(securityContext);

                try {
                    String authType = getAuthType(messageInfo, config);

                    if (shouldRegisterSession(messageInfo)) {
                        new AuthenticatorProxy(authenticator, webPrincipal, authType)
                                .authenticate(request, response, config);
                    } else {
                        request.setAuthType(authType == null ? PROXY_AUTH_TYPE : authType);
                        request.setUserPrincipal(webPrincipal);
                    }
                } catch (LifecycleException le) {
                    logger.log(SEVERE, "[Web-Security] unable to register session", le);
                }
            } else {
                // GLASSFISH-20930. Set null for the case when SAM does not indicate that it needs the session
                if (hasRequestPrincipal(messageInfo)) {
                    request.setUserPrincipal(null);
                    request.setAuthType(null);
                }

                // If authentication is mandatory, we must have a non-anonymous principal
                if (isMandatory) {
                    isValidateSuccess = false;
                }
            }

            if (isValidateSuccess) {

                // Check if the SAM instructed us to wrap the request and response

                HttpServletRequest wrappedServletRequest = (HttpServletRequest) messageInfo.getRequestMessage();
                if (wrappedServletRequest != servletRequest) {
                    request.setNote(WRAPPED_REQUEST, new HttpRequestWrapper(request, wrappedServletRequest));
                }

                HttpServletResponse wrappedServletResponse = (HttpServletResponse) messageInfo.getResponseMessage();
                if (wrappedServletResponse != servletResponse) {
                    request.setNote(WRAPPED_RESPONSE, new HttpResponseWrapper(response, wrappedServletResponse));
                }
            }

        }

        return isValidateSuccess;
    }

    @SuppressWarnings("unchecked")
    private void storeInRequest(HttpServletRequest servletRequest, MessageInfo messageInfo, ServerAuthContext authContext) {
        messageInfo.getMap().put(SERVER_AUTH_CONTEXT, authContext);
        servletRequest.setAttribute(MESSAGE_INFO, messageInfo);
    }

    private boolean hasRequestPrincipal(MessageInfo messageInfo) {
        return ((HttpServletRequest) messageInfo.getRequestMessage()).getUserPrincipal() != null;
    }

    private boolean hasNewPrincipal(Set<Principal> principalSet) {
        return principalSet != null && !principalSet.isEmpty() && !principalSetContainsOnlyAnonymousPrincipal(principalSet);
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

    private boolean mapEntryToBoolean(String propName, Map map) {
        if (map.containsKey(propName)) {
            Object value = map.get(propName);
            if (value != null && value instanceof String) {
                return Boolean.valueOf((String) value);
            }
        }

        return false;
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

    private void resetPolicyContext() {
        ((PolicyContextHandlerImpl) PolicyContextHandlerImpl.getInstance()).reset();
        PolicyContext.setContextID(null);
    }

    // inner class extends AuthenticatorBase such that session registration
    // of webtier can be invoked by RealmAdapter after authentication
    // by authentication module.
    static class AuthenticatorProxy extends AuthenticatorBase {

        private AuthenticatorBase authBase;
        private Principal principal;
        private String authType;

        AuthenticatorProxy(Authenticator authenticator, Principal principal, String authType) throws LifecycleException {
            this.authBase = (AuthenticatorBase) authenticator;
            this.principal = principal;
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

        @Override
        public boolean getCache() {
            return authBase.getCache();
        }

        @Override
        public Container getContainer() {
            return authBase.getContainer();
        }
    }

    private static class HttpMessageInfo implements MessageInfo {

        private Object request = null;
        private Object response = null;
        private Map<String, Object> map = new HashMap<>();

        HttpMessageInfo(HttpServletRequest request, HttpServletResponse response) {
            this.request = request;
            this.response = response;
        }

        @Override
        public Object getRequestMessage() {
            return request;
        }

        @Override
        public Object getResponseMessage() {
            return response;
        }

        @Override
        public void setRequestMessage(Object request) {
            this.request = request;
        }

        @Override
        public void setResponseMessage(Object response) {
            this.response = response;
        }

        @Override
        @SuppressWarnings("rawtypes")
        public Map getMap() {
            return map;
        }
    }

    /**
     * Generate the JSR 115 policy file for a web application, bundled within a ear or deployed as a standalone war file.
     *
     * Implementation note: If the generated file doesn't contains all the permission, the role mapper is probably broken.
     */
    protected void configureSecurity(WebBundleDescriptor wbd, boolean isSystem) {
        try {
            webSecurityManagerFactory.createManager(wbd, true, serverContext);
            String context = WebSecurityManager.getContextID(wbd);
            SecurityUtil.generatePolicyFile(context);
            if (isSystem && context.equals("__admingui/__admingui")) {
                websecurityProbeProvider.policyCreationEvent(context);
            }
        } catch (Exception ce) {
            logger.log(Level.SEVERE, "policy.configure", ce);
            throw new RuntimeException(ce);
        }
    }

    // Moved from J2EEInstanceListener.java
    private SecurityContext getSecurityContextForPrincipal(final Principal p) {
        if (p == null) {
            return null;
        } else if (p instanceof WebPrincipal) {
            return ((WebPrincipal) p).getSecurityContext();
        } else {
            return AccessController.doPrivileged(new PrivilegedAction<SecurityContext>() {

                @Override
                public SecurityContext run() {
                    Subject s = new Subject();
                    s.getPrincipals().add(p);
                    return new SecurityContext(p.getName(), s);
                }
            });
        }
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

        subject.getPublicCredentials().add(certificates[0].getSubjectDN());
        subject.getPublicCredentials().add(asList(certificates));

        return subject;
    }

    // TODO: reexamine this after TP2
    public synchronized void initConfigHelper(ServletContext servletContext) {
        if (httpServletHelper != null) {
            return;
        }

        httpServletHelper = getConfigHelper(servletContext);
    }

    @Override
    public void postConstruct() {
        nwListeners = networkConfig.getNetworkListeners();
    }

    @FunctionalInterface
    public static interface IOSupplier<T> {

        /**
         * Gets a result.
         *
         * @return a result
         */
        T get() throws IOException;
    }
}

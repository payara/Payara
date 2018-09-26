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
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// Portions Copyright [2016-2018] [Payara Foundation and/or its affiliates]
package org.apache.catalina.authenticator;

import static java.util.logging.Level.FINE;
import static javax.servlet.http.HttpServletResponse.SC_SERVICE_UNAVAILABLE;
import static org.apache.catalina.Realm.AUTHENTICATED_NOT_AUTHORIZED;
import static org.apache.catalina.Realm.AUTHENTICATE_NEEDED;
import static org.apache.catalina.Realm.AUTHENTICATE_NOT_NEEDED;
import static org.apache.catalina.authenticator.Constants.SESS_PASSWORD_NOTE;
import static org.apache.catalina.authenticator.Constants.SESS_USERNAME_NOTE;
import static org.apache.catalina.authenticator.Constants.SINGLE_SIGN_ON_COOKIE;

import java.io.IOException;
import java.lang.reflect.Method;
import java.security.Principal;
import java.security.SecureRandom;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Auditor;
import org.apache.catalina.Authenticator;
import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.HttpRequest;
import org.apache.catalina.HttpResponse;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LogFacade;
import org.apache.catalina.Pipeline;
import org.apache.catalina.Realm;
import org.apache.catalina.Request;
import org.apache.catalina.Response;
import org.apache.catalina.Session;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.deploy.LoginConfig;
import org.apache.catalina.deploy.SecurityConstraint;
import org.apache.catalina.valves.ValveBase;
import org.glassfish.web.valve.GlassFishValve;

/**
 * Basic implementation of the <b>Valve</b> interface that enforces the <code>&lt;security-constraint&gt;</code>
 * elements in the web application deployment descriptor. This functionality is implemented as a Valve so that it can be
 * ommitted in environments that do not require these features. Individual implementations of each supported
 * authentication method can subclass this base class as required.
 * <p>
 * <b>USAGE CONSTRAINT</b>: When this class is utilized, the Context to which it is attached (or a parent Container in a
 * hierarchy) must have an associated Realm that can be used for authenticating users and enumerating the roles to which
 * they have been assigned.
 * <p>
 * <b>USAGE CONSTRAINT</b>: This Valve is only useful when processing HTTP requests. Requests of any other type will
 * simply be passed through.
 *
 * @author Craig R. McClanahan
 * @version $Revision: 1.17.6.3 $ $Date: 2008/04/17 18:37:04 $
 */

public abstract class AuthenticatorBase extends ValveBase
        /**
         * CR 6411114 (Lifecycle implementation moved to ValveBase) implements Authenticator, Lifecycle {
         */
        // START CR 6411114
        implements Authenticator {
    // END CR 6411114

    // ----------------------------------------------------- Static Variables

    protected static final Logger log = LogFacade.getLogger();
    protected static final ResourceBundle rb = log.getResourceBundle();

    /**
     * Descriptive information about this implementation.
     */
    protected static final String info = "org.apache.catalina.authenticator.AuthenticatorBase/1.0";

    /**
     * The number of random bytes to include when generating a session identifier.
     */
    protected static final int SESSION_ID_BYTES = 16;

    /**
     * Authentication header
     */
    protected static final String AUTH_HEADER_NAME = "WWW-Authenticate";

    /**
     * Default authentication realm name.
     */
    protected static final String REALM_NAME = "Authentication required";

    // ----------------------------------------------------- Instance Variables

    /**
     * Should a session always be used once a user is authenticated? This may offer some performance benefits since the
     * session can then be used to cache the authenticated Principal, hence removing the need to authenticate the user via
     * the Realm on every request. This may be of help for combinations such as BASIC authentication used with the JNDIRealm
     * or DataSourceRealms. However there will also be the performance cost of creating and GC'ing the session. By default,
     * a session will not be created.
     */
    protected boolean alwaysUseSession = false;

    /**
     * Should we cache authenticated Principals if the request is part of an HTTP session?
     */
    protected boolean cache = true;

    /**
     * Should the session ID, if any, be changed upon a successful authentication to prevent a session fixation attack?
     */
    protected boolean changeSessionIdOnAuthentication = true;

    /**
     * The Context to which this Valve is attached.
     */
    protected Context context;

    /**
     * A String initialization parameter used to increase the entropy of the initialization of our random number generator.
     */
    protected String entropy;

    /**
     * Flag to determine if we disable proxy caching, or leave the issue up to the webapp developer.
     */
    protected boolean disableProxyCaching = true;

    /**
     * The lifecycle event support for this component.
     */
    /**
     * CR 6411114 (Lifecycle implementation moved to ValveBase) protected LifecycleSupport lifecycle = new
     * LifecycleSupport(this);
     */

    /**
     * A random number generator to use when generating session identifiers.
     */
    protected SecureRandom random = null;

    /**
     * The Java class name of the random number generator class to be used when generating session identifiers.
     */
    protected String randomClass = SecureRandom.class.getName();

    /**
     * The SingleSignOn implementation in our request processing chain, if there is one.
     */
    protected SingleSignOn sso;

    /**
     * Flag to determine if we disable proxy caching with headers incompatible with IE
     */
    protected boolean securePagesWithPragma = true;

    // ------------------------------------------------------------- Properties

    public boolean getAlwaysUseSession() {
        return alwaysUseSession;
    }

    public void setAlwaysUseSession(boolean alwaysUseSession) {
        this.alwaysUseSession = alwaysUseSession;
    }

    /**
     * Return the cache authenticated Principals flag.
     */
    public boolean getCache() {
        return cache;
    }

    /**
     * Set the cache authenticated Principals flag.
     *
     * @param cache The new cache flag
     */
    public void setCache(boolean cache) {
        this.cache = cache;
    }

    /**
     * Return the Container to which this Valve is attached.
     */
    public Container getContainer() {
        return context;
    }

    /**
     * Set the Container to which this Valve is attached.
     *
     * @param container The container to which we are attached
     */
    public void setContainer(Container container) {
        if (!(container instanceof Context)) {
            throw new IllegalArgumentException(rb.getString(LogFacade.CONFIG_ERROR_MUST_ATTACH_TO_CONTEXT));
        }

        super.setContainer(container);
        this.context = (Context) container;
        this.securePagesWithPragma = context.isSecurePagesWithPragma();
    }

    /**
     * Return the debugging detail level for this component.
     */
    public int getDebug() {
        return debug;
    }

    /**
     * Set the debugging detail level for this component.
     *
     * @param debug The new debugging detail level
     */
    public void setDebug(int debug) {
        this.debug = debug;
    }

    /**
     * Return the entropy increaser value, or compute a semi-useful value if this String has not yet been set.
     */
    public String getEntropy() {
        // Calculate a semi-useful value if this has not been set
        if (entropy == null) {
            setEntropy(this.toString());
        }

        return entropy;
    }

    /**
     * Set the entropy increaser value.
     *
     * @param entropy The new entropy increaser value
     */
    public void setEntropy(String entropy) {
        this.entropy = entropy;
    }

    /**
     * Return descriptive information about this Valve implementation.
     */
    @Override
    public String getInfo() {
        return (this.info);
    }

    /**
     * Return the random number generator class name.
     */
    public String getRandomClass() {
        return randomClass;
    }

    /**
     * Set the random number generator class name.
     *
     * @param randomClass The new random number generator class name
     */
    public void setRandomClass(String randomClass) {
        this.randomClass = randomClass;
    }

    /**
     * Return the flag that states if we add headers to disable caching by proxies.
     */
    public boolean getDisableProxyCaching() {
        return disableProxyCaching;
    }

    /**
     * Set the value of the flag that states if we add headers to disable caching by proxies.
     *
     * @param nocache <code>true</code> if we add headers to disable proxy caching, <code>false</code> if we leave the
     * headers alone.
     */
    public void setDisableProxyCaching(boolean nocache) {
        disableProxyCaching = nocache;
    }

    /**
     * Return the flag that states, if proxy caching is disabled, what headers we add to disable the caching.
     */
    public boolean isSecurePagesWithPragma() {
        return securePagesWithPragma;
    }

    /**
     * Set the value of the flag that states what headers we add to disable proxy caching.
     *
     * @param securePagesWithPragma <code>true</code> if we add headers which are incompatible with downloading office
     * documents in IE under SSL but which fix a caching problem in Mozilla.
     */
    public void setSecurePagesWithPragma(boolean securePagesWithPragma) {
        this.securePagesWithPragma = securePagesWithPragma;
    }

    /**
     * Return the flag that states if we should change the session ID of an existing session upon successful authentication.
     *
     * @return <code>true</code> to change session ID upon successful authentication, <code>false</code> to do not perform
     * the change.
     */
    public boolean isChangeSessionIdOnAuthentication() {
        return changeSessionIdOnAuthentication;
    }

    /**
     * Set the value of the flag that states if we should change the session ID of an existing session upon successful
     * authentication.
     *
     * @param changeSessionIdOnAuthentication <code>true</code> to change session ID upon successful authentication,
     * <code>false</code> to do not perform the change.
     */
    public void setChangeSessionIdOnAuthentication(boolean changeSessionIdOnAuthentication) {
        this.changeSessionIdOnAuthentication = changeSessionIdOnAuthentication;
    }

    public SingleSignOn getSingleSignOn() {
        return sso;
    }

    public void setSingleSignOn(SingleSignOn sso) {
        this.sso = sso;
    }

    // --------------------------------------------------------- Public Methods

    /**
     * Enforce the security restrictions in the web application deployment descriptor of our associated Context.
     *
     * @param request Request to be processed
     * @param response Response to be processed
     *
     * @exception IOException if an input/output error occurs
     * @exception ServletException if thrown by a processing element
     */
    @Override
    public int invoke(Request request, Response response) throws IOException, ServletException {

        // START GlassFish 247
        if (!context.getAvailable()) {
            try {
                ((HttpServletResponse) response.getResponse()).sendError(SC_SERVICE_UNAVAILABLE);
            } catch (IllegalStateException | IOException e) {
                ;
            }

            return END_PIPELINE;
        }
        // END GlassFish 247

        HttpRequest hrequest = (HttpRequest) request;
        HttpResponse hresponse = (HttpResponse) response;
        if (log.isLoggable(FINE)) {
            log.fine("Security checking request " + ((HttpServletRequest) request.getRequest()).getMethod() + " "
                    + ((HttpServletRequest) request.getRequest()).getRequestURI());
        }
        LoginConfig config = this.context.getLoginConfig();

        // Have we got a cached authenticated Principal to record?
        if (cache) {
            Principal principal = ((HttpServletRequest) request.getRequest()).getUserPrincipal();
            if (principal == null) {
                Session session = getSession(hrequest);
                if (session != null) {
                    principal = session.getPrincipal();
                    if (principal != null) {
                        if (log.isLoggable(FINE)) {
                            log.fine("We have cached auth type " + session.getAuthType() + " for principal " + session.getPrincipal());
                        }

                        hrequest.setAuthType(session.getAuthType());
                        hrequest.setUserPrincipal(principal);
                    }
                }
            }
        }

        Realm realm = this.context.getRealm();
        // Is this request URI subject to a security constraint?
        SecurityConstraint[] constraints = realm.findSecurityConstraints(hrequest, this.context);

        if (constraints == null) {
            log.fine(" Not subject to any constraint");
            return processSecurityCheck(hrequest, hresponse, config);
        }

        log.fine(" Calling hasUserDataPermission()");

        if (!realm.hasUserDataPermission(hrequest, hresponse, constraints)) {
            log.fine(" Failed hasUserDataPermission() test");
            // ASSERT: Authenticator already set the appropriate
            // HTTP status code, so we do not have to do anything special
            return END_PIPELINE;
        }

        int preAuthenticateCheckResult = realm.preAuthenticateCheck(hrequest, hresponse, constraints, disableProxyCaching,
                securePagesWithPragma, (sso != null));

        if (preAuthenticateCheckResult == AUTHENTICATE_NOT_NEEDED) {
            return processSecurityCheck(hrequest, hresponse, config);
        }

        if (preAuthenticateCheckResult == AUTHENTICATE_NEEDED) {
            log.fine(" Calling authenticate()");

            boolean authenticateResult = realm.invokeAuthenticateDelegate(hrequest, hresponse, context, this, false);

            if (!authenticateResult) {
                log.fine(" Failed authenticate() test");
                return END_PIPELINE;
            }
        } else if (preAuthenticateCheckResult == AUTHENTICATED_NOT_AUTHORIZED) {
            return END_PIPELINE;
        }

        log.log(FINE, " Calling accessControl()");

        if (!realm.hasResourcePermission(hrequest, hresponse, constraints, this.context)) {
            log.log(Level.FINE, " Failed accessControl() test");

            Auditor[] auditors = context.getAuditors();
            if (auditors != null) {
                for (int j = 0; j < auditors.length; j++) {
                    auditors[j].webInvocation(hrequest, false);
                }
            }

            /*
             * ASSERT: AccessControl method has already set the appropriate HTTP status code, so we do not have to do anything
             * special
             */
            return END_PIPELINE;
        }

        Auditor[] auditors = this.context.getAuditors();
        if (auditors != null) {
            boolean success = true;
            for (int j = 0; j < auditors.length; j++) {
                try {
                    auditors[j].webInvocation(hrequest, true);
                } catch (Exception e) {
                    success = false;
                }
            }

            if (!success) { // fail authorization if auditor blew up
                return END_PIPELINE;
            }
        }

        // Any and all specified constraints have been satisfied
        log.fine("Successfully passed all security constraints");

        return INVOKE_NEXT;
    }

    /**
     * A post-request processing implementation that does nothing.
     *
     * Very few Valves override this behaviour as most Valve logic is used for request processing.
     */
    @Override
    public void postInvoke(Request request, Response response) throws IOException, ServletException {
        Realm realm = this.context.getRealm();
        HttpRequest hrequest = (HttpRequest) request;
        HttpResponse hresponse = (HttpResponse) response;
        /*
         * Check realm for null since app may have been undeployed by the time its pipeline is invoked on the way out, in which
         * case its realm will have been set to null. See IT 6801
         */
        if (realm != null) {
            realm.invokePostAuthenticateDelegate(hrequest, hresponse, context);
        }
    }

    // ------------------------------------------------------ Protected Methods

    /**
     * Associate the specified single sign on identifier with the specified Session.
     *
     * @param ssoId Single sign on identifier
     * @param ssoVersion Single sign on version
     * @param session Session to be associated
     */
    protected void associate(String ssoId, long ssoVersion, Session session) {

        if (sso == null)
            return;
        sso.associate(ssoId, ssoVersion, session);

    }

    /**
     * Authenticate the user making this request, based on the specified login configuration. Return <code>true</code> if
     * any specified constraint has been satisfied, or <code>false</code> if we have created a response challenge already.
     *
     * @param request Request we are processing
     * @param response Response we are creating
     * @param config Login configuration describing how authentication should be performed
     *
     * @exception IOException if an input/output error occurs
     */
    // START SJSAS 6202703
    /*
     * protected abstract boolean authenticate(HttpRequest request, HttpResponse response, LoginConfig config) throws
     * IOException;
     */
    public abstract boolean authenticate(HttpRequest request, HttpResponse response, LoginConfig config) throws IOException;
    // END SJSAS 6202703

    /**
     * Generate and return a new session identifier for the cookie that identifies an SSO principal.
     */
    protected synchronized String generateSessionId() {

        // Generate a byte array containing a session identifier
        byte bytes[] = new byte[SESSION_ID_BYTES];
        getRandom().nextBytes(bytes);

        // Render the result as a String of hexadecimal digits
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            byte b1 = (byte) ((bytes[i] & 0xf0) >> 4);
            byte b2 = (byte) (bytes[i] & 0x0f);
            if (b1 < 10)
                result.append((char) ('0' + b1));
            else
                result.append((char) ('A' + (b1 - 10)));
            if (b2 < 10)
                result.append((char) ('0' + b2));
            else
                result.append((char) ('A' + (b2 - 10)));
        }
        return (result.toString());

    }

    /**
     * Return the random number generator instance we should use for generating session identifiers. If there is no such
     * generator currently defined, construct and seed a new one.
     */
    protected synchronized SecureRandom getRandom() {

        if (this.random == null) {
            try {
                Class clazz = Class.forName(randomClass);
                this.random = (SecureRandom) clazz.newInstance();
                long seed = System.currentTimeMillis();
                char entropy[] = getEntropy().toCharArray();
                for (int i = 0; i < entropy.length; i++) {
                    long update = ((byte) entropy[i]) << ((i % 8) * 8);
                    seed ^= update;
                }
                this.random.setSeed(seed);
            } catch (Exception e) {
                this.random = new SecureRandom();
            }
        }

        return (this.random);

    }

    /**
     * Return the internal Session that is associated with this HttpRequest, or <code>null</code> if there is no such
     * Session.
     *
     * @param request The HttpRequest we are processing
     */
    protected Session getSession(HttpRequest request) {

        return (getSession(request, false));

    }

    /**
     * Return the internal Session that is associated with this HttpRequest, possibly creating a new one if necessary, or
     * <code>null</code> if there is no such session and we did not create one.
     *
     * @param request The HttpRequest we are processing
     * @param create Should we create a session if needed?
     */
    protected Session getSession(HttpRequest request, boolean create) {

        return request.getSessionInternal(create);

    }

    /**
     * Log a message on the Logger associated with our Container (if any).
     *
     * @param message Message to be logged
     */
    protected void log(String message) {
        org.apache.catalina.Logger logger = context.getLogger();
        if (logger != null) {
            logger.log("Authenticator[" + context.getPath() + "]: " + message);
        } else {
            if (log.isLoggable(Level.INFO)) {
                log.log(Level.INFO, LogFacade.AUTHENTICATOR_INFO, new Object[] { context.getPath(), message });
            }
        }
    }

    /**
     * Log a message on the Logger associated with our Container (if any).
     *
     * @param message Message to be logged
     * @param t Associated exception
     */
    protected void log(String message, Throwable t) {
        org.apache.catalina.Logger logger = context.getLogger();
        if (logger != null) {
            logger.log("Authenticator[" + context.getPath() + "]: " + message, t, org.apache.catalina.Logger.WARNING);
        } else {
            String msg = MessageFormat.format(rb.getString(LogFacade.AUTHENTICATOR_INFO), new Object[] { context.getPath(), message });
            log.log(Level.WARNING, msg, t);
        }
    }

    /**
     * Register an authenticated Principal and authentication type in our request, in the current session (if there is one),
     * and with our SingleSignOn valve, if there is one. Set the appropriate cookie to be returned.
     *
     * @param request The servlet request we are processing
     * @param response The servlet response we are generating
     * @param principal The authenticated Principal to be registered
     * @param authType The authentication type to be registered
     * @param username Username used to authenticate (if any)
     * @param password Password used to authenticate (if any)
     */
    protected void register(HttpRequest request, HttpResponse response, Principal principal, String authType, String username,
            char[] password) {

        if (log.isLoggable(FINE)) {
            String pname = ((principal != null) ? principal.getName() : "[null principal]");
            log.log(FINE, "Authenticated '" + pname + "' with type '" + authType + "'");
        }
        // Cache the authentication information in our request
        request.setAuthType(authType);
        request.setUserPrincipal(principal);

        Session session = getSession(request, false);
        if (session != null && changeSessionIdOnAuthentication) {
            request.changeSessionId();
        } else if (alwaysUseSession) {
            session = getSession(request, true);
        }

        // Cache the authentication information in our session, if any
        if (cache) {
            if (session != null) {
                session.setAuthType(authType);
                session.setPrincipal(principal);

                if (username != null) {
                    session.setNote(SESS_USERNAME_NOTE, username);
                } else {
                    session.removeNote(SESS_USERNAME_NOTE);
                }

                if (password != null) {
                    session.setNote(SESS_PASSWORD_NOTE, password);
                } else {
                    session.removeNote(SESS_PASSWORD_NOTE);
                }
            }
        }

        // Construct a cookie to be returned to the client
        if (sso == null) {
            return;
        }

        HttpServletRequest hreq = (HttpServletRequest) request.getRequest();
        HttpServletResponse hres = (HttpServletResponse) response.getResponse();

        // Use the connector's random number generator (if any) for
        // generating the session ID. If none, then fall back to the default
        // session ID generator.
        String value = request.generateSessionId();
        if (value == null) {
            value = generateSessionId();
        }

        Cookie cookie = new Cookie(SINGLE_SIGN_ON_COOKIE, value);
        cookie.setMaxAge(-1);
        cookie.setPath("/");

        StandardHost host = (StandardHost) context.getParent();
        if (host != null) {
            host.configureSingleSignOnCookieSecure(cookie, hreq);
            host.configureSingleSignOnCookieHttpOnly(cookie);
        } else {
            cookie.setSecure(hreq.isSecure());
        }
        hres.addCookie(cookie);

        // Register this principal with our SSO valve
        /*
         * BEGIN S1AS8 PE 4856080,4918627 sso.register(value, principal, authType, username, password);
         */
        // BEGIN S1AS8 PE 4856080,4918627
        String realm = context.getRealm().getRealmName();
        // being here, an authentication just occurred using the realm
        assert (realm != null);
        sso.register(value, principal, authType, username, password, realm);
        // END S1AS8 PE 4856080,4918627

        request.setNote(Constants.REQ_SSOID_NOTE, value);
        if (sso.isVersioningSupported()) {
            request.setNote(Constants.REQ_SSO_VERSION_NOTE, Long.valueOf(0));
        }

    }

    @Override
    public void login(String username, char[] password, HttpRequest request) throws ServletException {
        Principal principal = doLogin(request, username, password);
        register(request, (HttpResponse) request.getResponse(), principal, getAuthMethod(), username, password);
    }

    protected abstract String getAuthMethod();

    /**
     * Process the login request.
     *
     * @param request Associated request
     * @param username The user
     * @param password The password
     * @return The authenticated Principal
     * @throws ServletException
     */
    protected Principal doLogin(HttpRequest request, String username, char[] password) throws ServletException {
        Principal p = context.getRealm().authenticate(username, password);
        if (p == null) {
            throw new ServletException(rb.getString(LogFacade.LOGIN_FAIL));
        }
        return p;
    }

    @Override
    public void logout(HttpRequest request) throws ServletException {
        Session session = getSession(request);
        if (session != null) {
            session.setPrincipal(null);
            session.setAuthType(null);
        }

        // principal and authType set to null in the following
        register(request, (HttpResponse) request.getResponse(), null, null, null, null);
    }

    // ------------------------------------------------------ Private Methods

    private int processSecurityCheck(HttpRequest hrequest, HttpResponse hresponse, LoginConfig config) throws IOException {

        // Special handling for form-based logins to deal with the case
        // where the login form (and therefore the "j_security_check" URI
        // to which it submits) might be outside the secured area
        String contextPath = this.context.getPath();
        String requestURI = hrequest.getDecodedRequestURI();
        if (requestURI.startsWith(contextPath) && requestURI.endsWith(Constants.FORM_ACTION)) {
            if (!authenticate(hrequest, hresponse, config)) {
                if (log.isLoggable(Level.FINE)) {
                    String msg = " Failed authenticate() test ??" + requestURI;
                    log.log(Level.FINE, msg);
                }
                return END_PIPELINE;
            }
        }
        return INVOKE_NEXT;
    }

    // ------------------------------------------------------ Lifecycle Methods

    /**
     * Add a lifecycle event listener to this component.
     *
     * @param listener The listener to add
     */
    /**
     * CR 6411114 (Lifecycle implementation moved to ValveBase) public void addLifecycleListener(LifecycleListener listener)
     * {
     *
     * lifecycle.addLifecycleListener(listener);
     *
     * }
     */

    /**
     * Get the lifecycle listeners associated with this lifecycle. If this Lifecycle has no listeners registered, a
     * zero-length array is returned.
     */
    /**
     * CR 6411114 (Lifecycle implementation moved to ValveBase) public LifecycleListener[] findLifecycleListeners() {
     *
     * return lifecycle.findLifecycleListeners();
     *
     * }
     */

    /**
     * Remove a lifecycle event listener from this component.
     *
     * @param listener The listener to remove
     */
    /**
     * CR 6411114 (Lifecycle implementation moved to ValveBase) public void removeLifecycleListener(LifecycleListener
     * listener) {
     *
     * lifecycle.removeLifecycleListener(listener);
     *
     * }
     */

    /**
     * Prepare for the beginning of active use of the public methods of this component. This method should be called after
     * <code>configure()</code>, and before any of the public methods of the component are utilized.
     *
     * @exception LifecycleException if this component detects a fatal error that prevents this component from being used
     */
    public void start() throws LifecycleException {
        // START CR 6411114
        if (started) // Ignore multiple starts
            return;
        super.start();
        // END CR 6411114
        if (context instanceof org.apache.catalina.core.StandardContext) {
            try {
                // XXX What is this ???
                Class paramTypes[] = new Class[0];
                Object paramValues[] = new Object[0];
                Method method = context.getClass().getMethod("getDebug", paramTypes);
                Integer result = (Integer) method.invoke(context, paramValues);
                setDebug(result);
            } catch (Exception e) {
                log.log(Level.SEVERE, LogFacade.GETTING_DEBUG_VALUE_EXCEPTION, e);
            }
        }
        /**
         * CR 6411114 (Lifecycle implementation moved to ValveBase) started = true;
         */

        // Look up the SingleSignOn implementation in our request processing
        // path, if there is one
        Container parent = context.getParent();
        while ((sso == null) && (parent != null)) {
            if (!(parent instanceof Pipeline)) {
                parent = parent.getParent();
                continue;
            }
            GlassFishValve valves[] = ((Pipeline) parent).getValves();
            for (int i = 0; i < valves.length; i++) {
                if (valves[i] instanceof SingleSignOn) {
                    sso = (SingleSignOn) valves[i];
                    break;
                }
            }
            if (sso == null)
                parent = parent.getParent();
        }
        if (log.isLoggable(Level.FINE)) {
            if (sso != null)
                log.log(Level.FINE, "Found SingleSignOn Valve at " + sso);
            else
                log.log(Level.FINE, "No SingleSignOn Valve is present");
        }

    }

    /**
     * Gracefully terminate the active use of the public methods of this component. This method should be the last one
     * called on a given instance of this component.
     *
     * @exception LifecycleException if this component detects a fatal error that needs to be reported
     */
    @Override
    public void stop() throws LifecycleException {
        // START CR 6411114
        if (!started) // Ignore stop if not started
            return;
        // END CR 6411114

        sso = null;
        // START CR 6411114
        super.stop();
        // END CR 6411114

    }

    // BEGIN S1AS8 PE 4856062,4918627
    /**
     * Set the name of the associated realm. This method does nothing by default.
     *
     * @param name the name of the realm.
     */
    public void setRealmName(String name) {

    }

    /**
     * Returns the name of the associated realm. Always returns null unless subclass overrides behavior.
     *
     * @return realm name or null if not set.
     */
    public String getRealmName() {
        return null;
    }
    // END S1AS8 PE 4856062,4918627

}

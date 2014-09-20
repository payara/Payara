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

package org.apache.catalina.authenticator;


import org.apache.catalina.*;
import org.apache.catalina.Logger;
import org.apache.catalina.core.StandardServer;
import org.apache.catalina.valves.ValveBase;
import org.glassfish.logging.annotation.LogMessageInfo;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;


/**
 * A <strong>Valve</strong> that supports a "single sign on" user experience,
 * where the security identity of a user who successfully authenticates to one
 * web application is propagated to other web applications in the same
 * security domain.  For successful use, the following requirements must
 * be met:
 * <ul>
 * <li>This Valve must be configured on the Container that represents a
 *     virtual host (typically an implementation of <code>Host</code>).</li>
 * <li>The <code>Realm</code> that contains the shared user and role
 *     information must be configured on the same Container (or a higher
 *     one), and not overridden at the web application level.</li>
 * <li>The web applications themselves must use one of the standard
 *     Authenticators found in the
 *     <code>org.apache.catalina.authenticator</code> package.</li>
 * </ul>
 *
 * @author Craig R. McClanahan
 * @version $Revision: 1.7 $ $Date: 2007/05/05 05:31:53 $
 */

public class SingleSignOn
    extends ValveBase
    /** CR 6411114 (Lifecycle implementation moved to ValveBase)
    implements Lifecycle, SessionListener {
    */
    // START CR 6411114
    implements SessionListener {
    // END CR 6411114


    // ----------------------------------------------------- Static Variables

    private static final java.util.logging.Logger log = StandardServer.log;
    private static final ResourceBundle rb = log.getResourceBundle();

    @LogMessageInfo(
            message = "Started",
            level = "INFO"
    )
    public static final String START_COMPONENT_INFO = "AS-WEB-CORE-00005";

    @LogMessageInfo(
            message = "Stopped",
            level = "INFO"
    )
    public static final String STOP_COMPONENT_INFO = "AS-WEB-CORE-00006";

    @LogMessageInfo(
            message = "Process session destroyed on {0}",
            level = "INFO"
    )
    public static final String PROCESS_SESSION_DESTROYED_INFO = "AS-WEB-CORE-00007";

    @LogMessageInfo(
            message = "Process request for '{0}'",
            level = "INFO"
    )
    public static final String PROCESS_REQUEST_INFO = "AS-WEB-CORE-00008";

    @LogMessageInfo(
            message = "Principal {0} has already been authenticated",
            level = "INFO"
    )
    public static final String PRINCIPAL_BEEN_AUTHENTICATED_INFO = "AS-WEB-CORE-00009";

    @LogMessageInfo(
            message = "Checking for SSO cookie",
            level = "INFO"
    )
    public static final String CHECK_SSO_COOKIE_INFO = "AS-WEB-CORE-00010";

    @LogMessageInfo(
            message = "SSO cookie is not present",
            level = "INFO"
    )
    public static final String SSO_COOKIE_NOT_PRESENT_INFO = "AS-WEB-CORE-00011";

    @LogMessageInfo(
            message = "Checking for cached principal for {0}",
            level = "INFO"
    )
    public static final String CHECK_CACHED_PRINCIPAL_INFO = "AS-WEB-CORE-00012";

    @LogMessageInfo(
            message = "Found cached principal {0} with auth type {1}",
            level = "INFO"
    )
    public static final String FOUND_CACHED_PRINCIPAL_AUTH_TYPE_INFO = "AS-WEB-CORE-00013";

    @LogMessageInfo(
            message = "No cached principal found, erasing SSO cookie",
            level = "INFO"
    )
    public static final String NO_CACHED_PRINCIPAL_FOUND_INFO = "AS-WEB-CORE-00014";

    @LogMessageInfo(
            message = "Associate sso id {0} with session {1}",
            level = "INFO"
    )
    public static final String ASSOCIATE_SSO_WITH_SESSION_INFO = "AS-WEB-CORE-00015";

    @LogMessageInfo(
            message = "Registering sso id {0} for user {1} with auth type {2}",
            level = "INFO"
    )
    public static final String REGISTERING_SSO_INFO = "AS-WEB-CORE-00016";

    /**
     * Descriptive information about this Valve implementation.
     */
    protected static final String info =
        "org.apache.catalina.authenticator.SingleSignOn";


    // ----------------------------------------------------- Instance Variables

    /**
     * The cache of SingleSignOnEntry instances for authenticated Principals,
     * keyed by the cookie value that is used to select them.
     */
    protected Map<String, SingleSignOnEntry> cache = new HashMap<String, SingleSignOnEntry>();

    /**
     * The lifecycle event support for this component.
     */
    /** CR 6411114 (Lifecycle implementation moved to ValveBase)
    protected LifecycleSupport lifecycle = new LifecycleSupport(this);
    */

    /**
     * Component started flag.
     */
    /** CR 6411114 (Lifecycle implementation moved to ValveBase)
    protected boolean started = false;
    */


    // ------------------------------------------------------------- Properties

    /**
     * Return the debugging detail level.
     */
    public int getDebug() {
        return (this.debug);
    }

    /**
     * Set the debugging detail level.
     *
     * @param debug The new debugging detail level
     */
    public void setDebug(int debug) {
        this.debug = debug;
    }


    // ------------------------------------------------------ Lifecycle Methods

    /**
     * Add a lifecycle event listener to this component.
     *
     * @param listener The listener to add
     */
    /** CR 6411114 (Lifecycle implementation moved to ValveBase)
    public void addLifecycleListener(LifecycleListener listener) {

        lifecycle.addLifecycleListener(listener);

    }
    */


    /**
     * Get the lifecycle listeners associated with this lifecycle. If this 
     * Lifecycle has no listeners registered, a zero-length array is returned.
     */
    /** CR 6411114 (Lifecycle implementation moved to ValveBase)
    public LifecycleListener[] findLifecycleListeners() {

        return lifecycle.findLifecycleListeners();

    }
    */


    /**
     * Remove a lifecycle event listener from this component.
     *
     * @param listener The listener to remove
     */
    /** CR 6411114 (Lifecycle implementation moved to ValveBase)
    public void removeLifecycleListener(LifecycleListener listener) {

        lifecycle.removeLifecycleListener(listener);

    }
    */


    /**
     * Prepare for the beginning of active use of the public methods of this
     * component.  This method should be called after <code>configure()</code>,
     * and before any of the public methods of the component are utilized.
     *
     * @exception LifecycleException if this component detects a fatal error
     *  that prevents this component from being used
     */
    public void start() throws LifecycleException {
        // START CR 6411114
        if (started)            // Ignore multiple starts
            return;
        super.start();
        // END CR 6411114

        if (debug >= 1)
            log(rb.getString(START_COMPONENT_INFO));

    }


    /**
     * Gracefully terminate the active use of the public methods of this
     * component.  This method should be the last one called on a given
     * instance of this component.
     *
     * @exception LifecycleException if this component detects a fatal error
     *  that needs to be reported
     */
    public void stop() throws LifecycleException {
        // START CR 6411114
        if (!started)       // Ignore stop if not started
            return;
        // END CR 6411114

        if (debug >= 1)
            log(rb.getString(STOP_COMPONENT_INFO));
        // START CR 6411114
        super.stop();
        // END CR 6411114

    }


    // ------------------------------------------------ SessionListener Methods


    /**
     * Acknowledge the occurrence of the specified event.
     *
     * @param event SessionEvent that has occurred
     */
    public void sessionEvent(SessionEvent event) {

        // We only care about session destroyed events
        if (!Session.SESSION_DESTROYED_EVENT.equals(event.getType()))
            return;

        // Look up the single session id associated with this session (if any)
        Session session = event.getSession();
        if (debug >= 1) {
            String msg = MessageFormat.format(rb.getString(PROCESS_SESSION_DESTROYED_INFO), session);
            log(msg);
        }
        String ssoId = session.getSsoId();
        if (ssoId == null) {
            return;
        }

        deregister(ssoId, session);
    }


    // ---------------------------------------------------------- Valve Methods


    /**
     * Return descriptive information about this Valve implementation.
     */
    public String getInfo() {

        return (info);

    }


    /**
     * Perform single-sign-on support processing for this request.
     *
     * @param request The servlet request we are processing
     * @param response The servlet response we are creating
     *
     * @exception IOException if an input/output error occurs
     * @exception ServletException if a servlet error occurs
     */
    public int invoke(Request request, Response response)
        throws IOException, ServletException {

        // If this is not an HTTP request and response, just pass them on
        /* GlassFish 6386229
        if (!(request instanceof HttpRequest) ||
            !(response instanceof HttpResponse)) {
            return INVOKE_NEXT;
        }
        */
        HttpServletRequest hreq =
            (HttpServletRequest) request.getRequest();
        HttpServletResponse hres =
            (HttpServletResponse) response.getResponse();
        request.removeNote(Constants.REQ_SSOID_NOTE);
        request.removeNote(Constants.REQ_SSO_VERSION_NOTE);

        // Has a valid user already been authenticated?
        if (debug >= 1) {
            String msg = MessageFormat.format(rb.getString(PROCESS_REQUEST_INFO), hreq.getRequestURI());
            log(msg);
        }
        if (hreq.getUserPrincipal() != null) {
            if (debug >= 1) {
                String msg = MessageFormat.format(rb.getString(PRINCIPAL_BEEN_AUTHENTICATED_INFO),
                                                  hreq.getUserPrincipal());
                log(msg);
            }
            return END_PIPELINE;
        }

        // Check for the single sign on cookie
        if (debug >= 1)
            log(rb.getString(CHECK_SSO_COOKIE_INFO));
        Cookie cookie = null;
        Cookie versionCookie = null;
        Cookie cookies[] = hreq.getCookies();
        if (cookies == null)
            cookies = new Cookie[0];
        for (int i = 0; i < cookies.length; i++) {
            if (Constants.SINGLE_SIGN_ON_COOKIE.equals(cookies[i].getName())) {
                cookie = cookies[i];
            } else if (Constants.SINGLE_SIGN_ON_VERSION_COOKIE.equals(cookies[i].getName())) {
                versionCookie = cookies[i];
            }
            
            if (cookie != null && versionCookie != null) {
                break;
            }
        }
        if (cookie == null) {
            if (debug >= 1)
                log(rb.getString(SSO_COOKIE_NOT_PRESENT_INFO));
            return INVOKE_NEXT;
        }

        // Look up the cached Principal associated with this cookie value
        if (debug >= 1) {
            String msg = MessageFormat.format(rb.getString(CHECK_CACHED_PRINCIPAL_INFO), cookie.getValue());
            log(msg);
        }
        long version = 0;
        if (isVersioningSupported() && versionCookie != null) {
            version = Long.parseLong(versionCookie.getValue());
        }
        SingleSignOnEntry entry = lookup(cookie.getValue(), version);
        if (entry != null) {
            if (debug >= 1) {
                String msg = MessageFormat.format(rb.getString(FOUND_CACHED_PRINCIPAL_AUTH_TYPE_INFO),
                                                  new Object[] {entry.getPrincipal().getName(), entry.getAuthType()});
                log(msg);
            }
            request.setNote(Constants.REQ_SSOID_NOTE, cookie.getValue());
            if (isVersioningSupported()) {
                long ver = entry.incrementAndGetVersion();
                request.setNote(Constants.REQ_SSO_VERSION_NOTE,
                        Long.valueOf(ver));
            }

            ((HttpRequest) request).setAuthType(entry.getAuthType());
            ((HttpRequest) request).setUserPrincipal(entry.getPrincipal());
        } else {
            if (debug >= 1)
                log(rb.getString(NO_CACHED_PRINCIPAL_FOUND_INFO));
            cookie.setMaxAge(0);
            hres.addCookie(cookie);
        }

        // Invoke the next Valve in our pipeline
        return INVOKE_NEXT;
    }


    // --------------------------------------------------------- Public Methods


    /**
     * Return a String rendering of this object.
     */
    public String toString() {

        StringBuilder sb = new StringBuilder("SingleSignOn[");
        if (container == null )
            sb.append("Container is null");
        else
            sb.append(container.getName());
        sb.append("]");
        return (sb.toString());

    }


    // -------------------------------------------------------- Package Methods


    /**
     * Associate the specified single sign on identifier with the
     * specified Session.
     *
     * @param ssoId Single sign on identifier
     * @param ssoVersion Single sign on version
     * @param session Session to be associated
     */
    public void associate(String ssoId, long ssoVersion, Session session) {

        if (!started) {
            return;
        }

        if (debug >= 1)
            log(rb.getString(ASSOCIATE_SSO_WITH_SESSION_INFO));

        SingleSignOnEntry sso = lookup(ssoId, ssoVersion);
        if (sso != null) {
            session.setSsoId(ssoId);
            session.setSsoVersion(ssoVersion);
            sso.addSession(this, session);
        }
    }

    /**
     * Deregister the specified session.  If it is the last session,
     * then also get rid of the single sign on identifier
     *
     * @param ssoId Single sign on identifier
     * @param session Session to be deregistered
     */
    protected void deregister(String ssoId, Session session) {

        SingleSignOnEntry sso = lookup(ssoId);
        if ( sso == null )
            return;

        session.setSsoId(null);
        session.setSsoVersion(0L);
        sso.removeSession( session );

        // see if we are the last session, if so blow away ssoId
        if (sso.isEmpty()) {
            synchronized (cache) {
                cache.remove(ssoId);
            }
        }
    }


    /**
     * Register the specified Principal as being associated with the specified
     * value for the single sign on identifier.
     *
     * @param ssoId Single sign on identifier to register
     * @param principal Associated user principal that is identified
     * @param authType Authentication type used to authenticate this
     *  user principal
     * @param username Username used to authenticate this user
     * @param password Password used to authenticate this user
     */
    protected void register(String ssoId, Principal principal, String authType,
                  String username, char[] password, String realmName) {

        if (debug >= 1) {
            String msg = MessageFormat.format(rb.getString(REGISTERING_SSO_INFO),
                                              new Object[] {ssoId, principal.getName(), authType});
            log(msg);
        }
        synchronized (cache) {
            cache.put(ssoId, new SingleSignOnEntry(ssoId, 0L, principal, authType,
                                                   username, realmName));
        }

    }


    // ------------------------------------------------------ Protected Methods


    /**
     * Log a message on the Logger associated with our Container (if any).
     *
     * @param message Message to be logged
     */
    protected void log(String message) {
        Logger logger = container.getLogger();
        if (logger != null) {
            logger.log(this.toString() + ": " + message);
        } else {
            if (log.isLoggable(Level.INFO)) {
                log.log(Level.INFO, this.toString() + ": " + message);
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
        Logger logger = container.getLogger();
        if (logger != null) {
            logger.log(this.toString() + ": " + message, t,
                Logger.WARNING);
        } else {
            log.log(Level.WARNING,
                this.toString() + ": " + message, t);
        }
    }


    /**
     * Look up and return the cached SingleSignOn entry associated with this
     * sso id value, if there is one; otherwise return <code>null</code>.
     *
     * @param ssoId Single sign on identifier to look up
     */
    protected SingleSignOnEntry lookup(String ssoId) {

        synchronized (cache) {
            return cache.get(ssoId);
        }

    }

    /**
     * Look up and return the cached SingleSignOn entry associated with this
     * sso id value, if there is one; otherwise return <code>null</code>.
     *
     * @param ssoId Single sign on identifier to look up
     * @param ssoVersion Single sign on version to look up
     */
    protected SingleSignOnEntry lookup(String ssoId, long ssoVersion) {

        return lookup(ssoId);

    }

    /**
     * Return a boolean to indicate whether the sso id version is
     * supported or not.
     */
    public boolean isVersioningSupported() {
        return false;
    }
}

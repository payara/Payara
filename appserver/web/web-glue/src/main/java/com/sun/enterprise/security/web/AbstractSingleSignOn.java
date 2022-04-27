/*
 *
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) 2022 Payara Foundation and/or its affiliates. All rights reserved.
 *
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/master/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at glassfish/legal/LICENSE.txt.
 *
 *  GPL Classpath Exception:
 *  The Payara Foundation designates this particular file as subject to the "Classpath"
 *  exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *  file that accompanied this code.
 *
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 *
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 *
 */

package com.sun.enterprise.security.web;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.web.security.RealmAdapter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Realm;
import org.apache.catalina.authenticator.Constants;
import org.apache.catalina.authenticator.SingleSignOn;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.glassfish.web.LogFacade;

public abstract class AbstractSingleSignOn<E extends PayaraSingleSignOnEntry> extends SingleSignOn implements Runnable, SingleSignOnMBean {
    /**
     * The log used by this class.
     */
    protected static final Logger logger = LogFacade.getLogger();

    /**
     * Store current realm, so that it can be registered in SSOEntry
     */
    protected final ThreadLocal<String> currentRealm = new ThreadLocal<>();

    /**
     * Store current version, so this can be picked up during registration
     */
    protected final ThreadLocal<Long> currentVersion = ThreadLocal.withInitial(() -> 0L);

    /**
     * Number of cache hits
     */
    private final AtomicInteger hitCount = new AtomicInteger(0);

    /**
     * Number of cache misses
     */
    private final AtomicInteger missCount = new AtomicInteger(0);

    /**
     * The background thread.
     */
    private Thread thread = null;

    /**
     * The background thread completion semaphore.
     */
    private boolean threadDone = false;

    /**
     * The interval (in seconds) between checks for expired sessions.
     */
    private int ssoReapInterval = 60;

    /**
     * Max idle time (in seconds) for SSO entries before being elegible for purging. A value less than zero indicates that
     * SSO entries are supposed to never expire.
     */
    private int ssoMaxInactive = 300;

    /**
     * Return expire thread interval (seconds)
     */
    public int getReapInterval() {

        return this.ssoReapInterval;

    }

    /**
     * Set expire thread interval (seconds)
     */
    public void setReapInterval(int t) {

        this.ssoReapInterval = t;

    }

    /**
     * Return max idle time for SSO entries (seconds)
     */
    public int getMaxInactive() {

        return this.ssoMaxInactive;

    }

    /**
     * Set max idle time for SSO entries (seconds)
     */
    public void setMaxInactive(int t) {

        this.ssoMaxInactive = t;

    }

    /**
     * Prepare for the beginning of active use of the public methods of this component. This method should be called after
     * <code>configure()</code>, and before any of the public methods of the component are utilized.
     *
     * @throws LifecycleException if this component detects a fatal error that prevents this component from being used
     */
    @Override
    protected void startInternal() throws LifecycleException {
        super.startInternal();
        // Start the background reaper thread
        threadStart();
    }

    /**
     * Gracefully terminate the active use of the public methods of this component. This method should be the last one
     * called on a given instance of this component.
     *
     * @throws LifecycleException if this component detects a fatal error that needs to be reported
     */
    @Override
    protected void stopInternal() throws LifecycleException {
        // Stop the background reaper thread
        threadStop();
        super.stopInternal();
    }

    /**
     * Perform single-sign-on support processing for this request.
     *
     * @param request  The servlet request we are processing
     * @param response The servlet response we are creating
     * @return the valve flag
     */
    @Override
    public void invoke(final Request request, final Response response) throws ServletException, IOException {
        HttpServletRequest hreq = request.getRequest();
        HttpServletResponse hres = response.getResponse();
        request.removeNote(Constants.REQ_SSOID_NOTE);
        request.removeNote(SsoConstants.REQ_SSO_VERSION_NOTE);

        // Has a valid user already been authenticated?
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, LogFacade.REQUEST_PROCESSED, hreq.getRequestURI());
        }
        if (hreq.getUserPrincipal() != null) {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, LogFacade.PRINCIPAL_ALREADY_AUTHENTICATED, hreq.getUserPrincipal().getName());
            }
            getNext().invoke(request, response);
            return;
        }

        // Get the realm associated with the app of this request.
        // If there is no realm available, do not process SSO.
        Realm realm = request.getContext().getRealm();
        if (!(realm instanceof RealmAdapter)) {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, LogFacade.NO_REALM_CONFIGURED);
            }
            getNext().invoke(request, response);
            return;
        }

        String realmName = ((RealmAdapter) realm).getRealmName();
        if (realmName == null) {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, LogFacade.NO_REALM_CONFIGURED);
            }
            getNext().invoke(request, response);
            return;
        }
        currentRealm.set(realmName);

        // Check for the single sign on cookie
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, LogFacade.CHECKING_SSO_COOKIE);
        }

        final Cookie[] cookies = hreq.getCookies();
        if (cookies == null) {
            getNext().invoke(request, response);
            currentRealm.set(null);
            return;
        }
        Cookie cookie = null;
        Cookie versionCookie = null;
        for (Cookie c : cookies) {
            if (Constants.SINGLE_SIGN_ON_COOKIE.equals(c.getName())) {
                cookie = c;
            } else if (SsoConstants.SINGLE_SIGN_ON_VERSION_COOKIE.equals(c.getName())) {
                versionCookie = c;
            }

            if (cookie != null && versionCookie != null) {
                break;
            }
        }
        if (cookie == null) {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, LogFacade.SSO_COOKIE_NOT_PRESENT);
            }
            getNext().invoke(request, response);
            currentRealm.set(null);
            return;
        }


        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, LogFacade.APP_REALM);
        }

        // Look up the cached Principal associated with this cookie value
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, LogFacade.CHECKING_CACHED_PRINCIPAL);
        }

        long version = 0;
        if (isVersioningSupported() && versionCookie != null) {
            version = Long.parseLong(versionCookie.getValue());
        }
        currentVersion.set(version);
        PayaraSingleSignOnEntry entry = lookup(cookie.getValue(), version);
        if (entry != null) {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, LogFacade.FOUND_CACHED_PRINCIPAL,
                        new Object[]{entry.getPrincipal().getName(), entry.getAuthType(), entry.getRealmName()});
            }
            // S1AS8 6155481 END

            // only use this SSO identity if it was set in the same realm
            if (entry.getRealmName().equals(realmName)) {
                request.setNote(Constants.REQ_SSOID_NOTE, cookie.getValue());
                request.setAuthType(entry.getAuthType());
                request.setUserPrincipal(entry.getPrincipal());
                // Touch the SSO entry access time
                entry.setLastAccessTime(System.currentTimeMillis());
                if (isVersioningSupported()) {
                    long ver = entry.incrementAndGetVersion();
                    request.setNote(SsoConstants.REQ_SSO_VERSION_NOTE, Long.valueOf(ver));
                }
                // update hit atomic counter
                hitCount.incrementAndGet();
            } else {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, LogFacade.IGNORING_SSO, realmName);
                }
                // consider this a cache miss, update atomic counter
                missCount.incrementAndGet();
            }
        } else {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, LogFacade.NO_CACHED_PRINCIPAL_FOUND);
            }
            cookie.setMaxAge(0);
            hres.addCookie(cookie);
            // update miss atomic counter
            missCount.incrementAndGet();
        }
        getNext().invoke(request, response);
        currentRealm.set(null);
        currentVersion.set(0L);
    }

    protected E lookup(String ssoId) {
        return (E) cache.get(ssoId);
    }

    protected E lookup(String ssoId, long version) {
        return lookup(ssoId);
    }

    protected boolean isVersioningSupported() {
        // overriden in subclasses
        return false;
    }

    // we need to override any write access to cache as well, as we depend on our SSO entries
    @Override
    protected void register(String ssoId, Principal principal, String authType, String username, String password) {

        if (logger.isLoggable(Level.FINE)) {
            // Resource bundles for org.apache.catalina are gone.
            logger.fine(() -> String.format("Registering sso id {0} for user {1} with auth type {2}", ssoId,
                    principal != null ? principal.getName() : "", authType));
        }

        var entry = createEntry(ssoId, principal, authType, username);
        cache.put(ssoId, entry);
        entryAdded(entry);
    }

    protected void entryAdded(E entry) {

    }

    protected abstract E createEntry(String ssoId, Principal principal, String authType, String username);

    /**
     * Invalidate all SSO cache entries that have expired.
     */
    private void processExpires() {

        if (ssoMaxInactive < 0) {
            // SSO entries are supposed to never expire
            return;
        }

        long tooOld = System.currentTimeMillis() - ssoMaxInactive * 1000L;
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, LogFacade.SSO_EXPIRATION_STARTED, this.cache.size());
        }
        final ArrayList<String> removals = new ArrayList<>(this.cache.size() / 2);

        // build list of removal targets

        // Note that only those SSO entries which are NOT associated with
        // any session are eligible for removal here.
        // Currently no session association ever happens so this covers all
        // SSO entries. However, this should be addressed separately.

        try {
            for (var e : cache.entrySet()) {
                var sso = (E) e.getValue();
                if (sso.isEmpty() && sso.getLastAccessTime() < tooOld) {
                    removals.add(e.getKey());
                }
            }

            int removalCount = removals.size();
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, LogFacade.SSO_CACHE_EXPIRE, removalCount);
            }
            // deregister any eligible sso entries
            for (final String removal : removals) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, LogFacade.SSO_EXPRIRATION_REMOVING_ENTRY, removal);
                }
                deregister(removal);
            }
        } catch (Throwable e) { // don't let thread die
            logger.log(Level.WARNING, LogFacade.EXCEPTION_DURING_SSO_EXPIRATION, e);
        }
    }

    /**
     * Sleep for the duration specified by the <code>ssoReapInterval</code> property.
     */
    private void threadSleep() {

        try {
            Thread.sleep(ssoReapInterval * 1000L);
        } catch (InterruptedException e) {
            ;
        }

    }

    /**
     * Start the background thread that will periodically check for SSO timeouts.
     */
    private void threadStart() {

        if (thread != null) {
            return;
        }

        threadDone = false;
        String threadName = "SingleSignOnExpiration";
        thread = new Thread(this, threadName);
        thread.setDaemon(true);
        thread.start();

    }

    /**
     * Stop the background thread that is periodically checking for SSO timeouts.
     */
    private void threadStop() {

        if (thread == null) {
            return;
        }

        threadDone = true;
        thread.interrupt();
        try {
            thread.join();
        } catch (InterruptedException e) {
            ;
        }

        thread = null;

    }

    /**
     * The background thread that checks for SSO timeouts and shutdown.
     */
    public void run() {

        // Loop until the termination semaphore is set
        while (!threadDone) {
            threadSleep();
            processExpires();
        }

    }

    /**
     * Gets the number of sessions participating in SSO
     *
     * @return Number of sessions participating in SSO
     */
    public int getActiveSessionCount() {
        return this.cache.size();
    }

    /**
     * Gets the number of SSO cache hits
     *
     * @return Number of SSO cache hits
     */
    public int getHitCount() {
        return hitCount.intValue();
    }

    /**
     * Gets the number of SSO cache misses
     *
     * @return Number of SSO cache misses
     */
    public int getMissCount() {
        return missCount.intValue();
    }
}

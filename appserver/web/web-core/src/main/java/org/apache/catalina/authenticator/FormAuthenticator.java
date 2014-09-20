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

import org.apache.catalina.HttpRequest;
import org.apache.catalina.HttpResponse;
import org.apache.catalina.Realm;
import org.apache.catalina.Session;
import org.apache.catalina.deploy.LoginConfig;
import org.apache.catalina.deploy.SecurityConstraint;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.IOException;
import java.security.Principal;
import java.util.*;
import java.util.logging.Level;
import org.glassfish.grizzly.http.util.ByteChunk;
import org.glassfish.grizzly.http.util.CharChunk;
import org.glassfish.grizzly.http.util.MessageBytes;
import org.glassfish.logging.annotation.LogMessageInfo;

/**
 * An <b>Authenticator</b> and <b>Valve</b> implementation of FORM BASED
 * Authentication, as described in the Servlet API Specification, Version 2.2.
 *
 * @author Craig R. McClanahan
 * @author Remy Maucherat
 * @version $Revision: 1.8.2.2 $ $Date: 2008/04/17 18:37:04 $
 */

public class FormAuthenticator
    extends AuthenticatorBase {


    @LogMessageInfo(
            message = "Unexpected error forwarding or redirecting to login page",
            level = "WARNING"
    )
    public static final String UNEXPECTED_ERROR_FORWARDING_TO_LOGIN_PAGE = "AS-WEB-CORE-00004";


    // -------------------------------------------------- Instance Variables

    /**
     * Descriptive information about this implementation.
     */
    protected static final String info =
        "org.apache.catalina.authenticator.FormAuthenticator/1.0";


    // ---------------------------------------------------------- Properties


    /**
     * Return descriptive information about this Valve implementation.
     */
    @Override
    public String getInfo() {
        return (this.info);
    }


    // ------------------------------------------------------- Public Methods


    /**
     * Authenticate the user making this request, based on the specified
     * login configuration.  Return <code>true</code> if any specified
     * constraint has been satisfied, or <code>false</code> if we have
     * created a response challenge already.
     *
     * @param request Request we are processing
     * @param response Response we are creating
     * @param config Login configuration describing how authentication
     * should be performed
     *
     * @exception IOException if an input/output error occurs
     */
    @Override
    public boolean authenticate(HttpRequest request,
                                HttpResponse response,
                                LoginConfig config)
        throws IOException {

        // References to objects we will need later
        HttpServletRequest hreq =
          (HttpServletRequest) request.getRequest();
        HttpServletResponse hres =
          (HttpServletResponse) response.getResponse();
        Session session = null;

        String contextPath = hreq.getContextPath();
        String requestURI = request.getDecodedRequestURI();
        // Is this the action request from the login page?
        boolean loginAction =
            requestURI.startsWith(contextPath) &&
            requestURI.endsWith(Constants.FORM_ACTION);

        // Have we already authenticated someone?
        Principal principal = hreq.getUserPrincipal();
        // Treat the first and any subsequent j_security_check requests the
        // same, by letting them fall through to the j_security_check 
        // processing section of this method. 
        if (principal != null && !loginAction) {
            if (log.isLoggable(Level.FINE))
                log.log(Level.FINE, "Already authenticated '" + principal.getName() + "'");
            String ssoId = (String) request.getNote(Constants.REQ_SSOID_NOTE);
            if (ssoId != null) {
                getSession(request, true);
            }
            return (true);
        }

        // Have we authenticated this user before but have caching disabled?
        // Treat the first and any subsequent j_security_check requests the
        // same, by letting them fall through to the j_security_check 
        // processing section of this method. 
        if (!cache && !loginAction) {
            session = getSession(request, true);
            if (log.isLoggable(Level.FINE))
                log.log(Level.FINE, "Checking for reauthenticate in session " + session);
            String username =
                (String) session.getNote(Constants.SESS_USERNAME_NOTE);
            char[] password =
                (char[]) session.getNote(Constants.SESS_PASSWORD_NOTE);
            if ((username != null) && (password != null)) {
                if (log.isLoggable(Level.FINE))
                    log.log(Level.FINE, "Reauthenticating username '" + username + "'");
                principal =
                    context.getRealm().authenticate(username, password);
                if (principal != null) {
                    session.setNote(Constants.FORM_PRINCIPAL_NOTE, principal);
                    if (!matchRequest(request)) {
                        register(request, response, principal,
                                 Constants.FORM_METHOD,
                                 username, password);
                        return (true);
                    }
                }
                if (log.isLoggable(Level.FINE))
                    log.log(Level.FINE, "Reauthentication failed, proceed normally");
            }
        }

        // Is this the re-submit of the original request URI after successful
        // authentication?  If so, forward the *original* request instead.
        if (matchRequest(request)) {
            session = getSession(request, true);
            if (log.isLoggable(Level.FINE)) {
                String msg = "Restore request from session '" +
                             session.getIdInternal() + "'";
                log.log(Level.FINE, msg);
            }
            principal = (Principal)
                session.getNote(Constants.FORM_PRINCIPAL_NOTE);
            register(request, response, principal, Constants.FORM_METHOD,
                     (String) session.getNote(Constants.SESS_USERNAME_NOTE),
                     (char[]) session.getNote(Constants.SESS_PASSWORD_NOTE));
            String ssoId = (String) request.getNote(Constants.REQ_SSOID_NOTE);
            if (ssoId != null) {
                associate(ssoId, getSsoVersion(request), session);
            }
            if (restoreRequest(request, session)) {
                if (log.isLoggable(Level.FINE))
                    log.log(Level.FINE, "Proceed to restored request");
                return (true);
            } else {
                if (log.isLoggable(Level.FINE))
                    log.log(Level.FINE, "Restore of original request failed");
                hres.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return (false);
            }
        }

        // Acquire references to objects we will need to evaluate
        MessageBytes uriMB = MessageBytes.newInstance();
        CharChunk uriCC = uriMB.getCharChunk();
        uriCC.setLimit(-1);
        response.setContext(request.getContext());

        // No -- Save this request and redirect to the form login page
        if (!loginAction) {
            session = getSession(request, true);
            if (log.isLoggable(Level.FINE)) {
                String msg = "Save request in session '" +
                             session.getIdInternal() + "'";
                log.log(Level.FINE, msg);
            }
            saveRequest(request, session);

            //START Apache bug 36136: Refactor the login and error page forward
            /*
            RequestDispatcher disp =
                context.getServletContext().getRequestDispatcher
                (config.getLoginPage());
            try {
                disp.forward(hreq, hres);
                response.finishResponse();
            } catch (Throwable t) {
                log.warn("Unexpected error forwarding to login page", t);
            }
            */
            forwardToLoginPage(request, response, config);
            //END Apache bug 36136: Refactor the login and error page forward

            return (false);
        }

        // Yes -- Validate the specified credentials and redirect
        // to the error page if they are not correct
        Realm realm = context.getRealm();
        String username = hreq.getParameter(Constants.FORM_USERNAME);
        String pwd = hreq.getParameter(Constants.FORM_PASSWORD);
        char[] password = ((pwd != null)? pwd.toCharArray() : null);

        if (log.isLoggable(Level.FINE))
            log.log(Level.FINE, "Authenticating username '" + username + "'");
        principal = realm.authenticate(username, password);
        if (principal == null) {

            //START Apache bug 36136: Refactor the login and error page forward
            /*
            RequestDispatcher disp =
                context.getServletContext().getRequestDispatcher
                (config.getErrorPage());
            try {
                disp.forward(hreq, hres);
            } catch (Throwable t) {
                log.warn("Unexpected error forwarding to error page", t);
            }
            */
            forwardToErrorPage(request, response, config);
            //END Apache bug 36136: Refactor the login and error page forward

            return (false);
        }

        // Save the authenticated Principal in our session
        if (log.isLoggable(Level.FINE))
            log.log(Level.FINE, "Authentication of '" + username + "' was successful");
        if (session == null)
            session = getSession(request, true);
        session.setNote(Constants.FORM_PRINCIPAL_NOTE, principal);

        // If we are not caching, save the username and password as well
        if (!cache) {
            session.setNote(Constants.SESS_USERNAME_NOTE, username);
            session.setNote(Constants.SESS_PASSWORD_NOTE, password);
        }

        // Redirect the user to the original request URI (which will cause
        // the original request to be restored)
        requestURI = savedRequestURL(session);
        if (requestURI == null) {
            // requestURI will be null if the login form is submitted
            // directly, i.e., if there has not been any original request
            // that was stored away before the redirect to the login form was
            // issued. In this case, assume that the original request has been
            // for the context root, and have the welcome page mechanism take
            // care of it
            requestURI = hreq.getContextPath() + "/";
            register(request, response, principal, Constants.FORM_METHOD,
                     (String) session.getNote(Constants.SESS_USERNAME_NOTE),
                     (char[]) session.getNote(Constants.SESS_PASSWORD_NOTE));
            String ssoId = (String) request.getNote(Constants.REQ_SSOID_NOTE);
            if (ssoId != null) {
                associate(ssoId, getSsoVersion(request), session);
            }
        }

        if (log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, "Redirecting to original '" + requestURI + "'");
        }

        hres.sendRedirect(hres.encodeRedirectURL(requestURI));

        return (false);

    }


    // ------------------------------------------------------ Protected Methods

    @Override
    protected String getAuthMethod() {
        return HttpServletRequest.FORM_AUTH;
    }

    /**
     * Does this request match the saved one (so that it must be the redirect
     * we signaled after successful authentication?
     *
     * @param request The request to be verified
     */
    protected boolean matchRequest(HttpRequest request) {

      // Has a session been created?
      Session session = getSession(request, false);
      if (session == null)
          return (false);

      // Is there a saved request?
      SavedRequest sreq = (SavedRequest)
          session.getNote(Constants.FORM_REQUEST_NOTE);
      if (sreq == null)
          return (false);

      // Is there a saved principal?
      if (session.getNote(Constants.FORM_PRINCIPAL_NOTE) == null)
          return (false);

      // Does the request URI match?
      HttpServletRequest hreq = (HttpServletRequest) request.getRequest();
      String requestURI = hreq.getRequestURI();
      if (requestURI == null)
          return (false);
      return (requestURI.equals(sreq.getRequestURI()));

    }


    /**
     * Restore the original request from information stored in our session.
     * If the original request is no longer present (because the session
     * timed out), return <code>false</code>; otherwise, return
     * <code>true</code>.
     *
     * @param request The request to be restored
     * @param session The session containing the saved information
     */
    protected boolean restoreRequest(HttpRequest request, Session session)
            throws IOException {

        // Retrieve and remove the SavedRequest object from our session
        SavedRequest saved = (SavedRequest)
            session.getNote(Constants.FORM_REQUEST_NOTE);
        /*
         * PWC 6463046:
         * Do not remove the saved request: It will be needed again in case
         * another j_security_check is sent. The saved request will be
         * purged when the session expires.
        session.removeNote(Constants.FORM_REQUEST_NOTE);
         */
        session.removeNote(Constants.FORM_PRINCIPAL_NOTE);
        if (saved == null)
            return (false);

        // Swallow any request body since we will be replacing it
        // Need to do this before headers are restored as AJP connector uses
        // content length header to determine how much data needs to be read for
        // request body
        byte[] buffer = new byte[4096];
        InputStream is = request.getStream();
        while (is.read(buffer) >= 0) {
            // Ignore request body
        }

        // Modify our current request to reflect the original one
        request.clearCookies();
        Iterator<Cookie> cookies = saved.getCookies();
        while (cookies.hasNext()) {
            request.addCookie(cookies.next());
        }

        String method = saved.getMethod();
        boolean cachable = "GET".equalsIgnoreCase(method) ||
                "HEAD".equalsIgnoreCase(method);
        request.clearHeaders();
        Iterator<String> names = saved.getHeaderNames();
        while (names.hasNext()) {
            String name = names.next();
            // The browser isn't expecting this conditional response now.
            // Assuming that it can quietly recover from an unexpected 412.
            // BZ 43687
            if(!("If-Modified-Since".equalsIgnoreCase(name) ||
                    (cachable && "If-None-Match".equalsIgnoreCase(name)))) {
                Iterator<String> values = saved.getHeaderValues(name);
                while (values.hasNext()) {
                    request.addHeader(name, values.next());
                }
            }
        }

        request.setContentLength(saved.getContentLenght());

        request.clearLocales();
        Iterator<Locale> locales = saved.getLocales();
        while (locales.hasNext()) {
            request.addLocale(locales.next());
        }

        request.clearParameters();
        // setQueryStringEncoding is done inside request.clearParameters

        ByteChunk body = saved.getBody();

        if (body != null) {
            request.replayPayload(body.getBytes());

            // If no content type specified, use default for POST
            String savedContentType = saved.getContentType();
            if (savedContentType == null && "POST".equalsIgnoreCase(method)) {
                savedContentType = "application/x-www-form-urlencoded";
            }

            request.setContentType(savedContentType);
        }

        request.setMethod(method);
        request.setQueryString(saved.getQueryString());

        return true;

    }


    /**
     * Called to forward to the login page. may redirect current request to HTTPS
     *
     * @param request HttpRequest we are processing
     * @param response HttpResponse we are creating
     * @param config    Login configuration describing how authentication
     *                  should be performed
     */
    protected void forwardToLoginPage(HttpRequest request,
                                      HttpResponse response,
                                      LoginConfig config) {

        if (isChangeSessionIdOnAuthentication() && getSession(request, false) != null) {
            request.changeSessionId();
        }

        ServletContext sc = context.getServletContext();
        try {
            String loginPage = config.getLoginPage();
            if (!request.getRequest().isSecure()) {
                Realm realm = context.getRealm();
                if (realm != null) {
                    SecurityConstraint[] secConstraints =
                            realm.findSecurityConstraints(loginPage, "GET", context);
                    if (secConstraints != null &&
                            !realm.hasUserDataPermission
                                    (request, response,secConstraints, loginPage, "GET")) {
                        /*
                         * Note that hasUserDataPermission will have already
                         * issued a redirect to HTTPS unless redirects
                         * have been disabled, in which case it will have
                         * called sendError(FORBIDDEN)
                         */
                        return;
                    }
                }
            }
            RequestDispatcher disp = sc.getRequestDispatcher(loginPage);
            disp.forward(request.getRequest(), response.getResponse());
            //NOTE: is finishResponse necessary or is it unnecessary after forward
            response.finishResponse();
        } catch (Throwable t) {
            log.log(Level.WARNING, UNEXPECTED_ERROR_FORWARDING_TO_LOGIN_PAGE,
                    t);
        }
    }
    
    
    /**
     * Called to forward to the error page. may redirect current request to HTTPS
     *
     * @param request HttpRequest we are processing
     * @param response HttpResponse we are creating
     * @param config    Login configuration describing how authentication
     *                  should be performed
     */
    protected void forwardToErrorPage(HttpRequest request,
                                HttpResponse response,
                                LoginConfig config) {
        ServletContext sc = context.getServletContext();
        try {
            String errorPage = config.getErrorPage();
            if (!request.getRequest().isSecure()) {
                Realm realm = context.getRealm();
                if (realm != null) {
                    SecurityConstraint[] secConstraints =
                            realm.findSecurityConstraints(errorPage, "GET", context);
                    if (secConstraints != null &&
                            !realm.hasUserDataPermission
                                    (request, response,secConstraints, errorPage, "GET")) {
                        /*
                         * Note that hasUserDataPermission will have already
                         * issued a redirect to HTTPS unless redirects
                         * have been disabled, in which case it will have
                         * called sendError(FORBIDDEN).
                         */
                        return;
                    }
                }
            }
            RequestDispatcher disp = sc.getRequestDispatcher(errorPage);
            disp.forward(request.getRequest(), response.getResponse());
        } catch (Throwable t) {
            log.log(Level.WARNING, UNEXPECTED_ERROR_FORWARDING_TO_LOGIN_PAGE,
                    t);
        }
    }
    
    /**
     * Save the original request information into our session.
     *
     * @param request The request to be saved
     * @param session The session to contain the saved information
     */
    //START Apache bug 36136: Refactor the login and error page forward
    //private void saveRequest(HttpRequest request, Session session) {
    protected void saveRequest(HttpRequest request, Session session)
            throws IOException {
    //END Apache bug 36136: Refactor the login and error page forward

        // Create and populate a SavedRequest object for this request
        HttpServletRequest hreq = (HttpServletRequest) request.getRequest();
        SavedRequest saved = new SavedRequest();
        Cookie cookies[] = hreq.getCookies();
        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++)
                saved.addCookie(cookies[i]);
        }
        Enumeration names = hreq.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();
            Enumeration values = hreq.getHeaders(name);
            while (values.hasMoreElements()) {
                String value = (String) values.nextElement();
                saved.addHeader(name, value);
            }
        }

        saved.setContentLength(hreq.getContentLength());

        Enumeration locales = hreq.getLocales();
        while (locales.hasMoreElements()) {
            Locale locale = (Locale) locales.nextElement();
            saved.addLocale(locale);
        }

        // May need to acknowledge a 100-continue expectation
        ((HttpResponse)request.getResponse()).sendAcknowledgement();

        ByteChunk body = new ByteChunk();
        body.setLimit(request.getConnector().getMaxSavePostSize());

        byte[] buffer = new byte[4096];
        int bytesRead;
        InputStream is = request.getStream();

        while ( (bytesRead = is.read(buffer) ) >= 0) {
            body.append(buffer, 0, bytesRead);
        }

        // Only save the request body if there is something to save
        if (body.getLength() > 0) {
            saved.setContentType(hreq.getContentType());
            saved.setBody(body);
        }

        saved.setMethod(hreq.getMethod());
        saved.setQueryString(hreq.getQueryString());
        saved.setRequestURI(hreq.getRequestURI());

        // Stash the SavedRequest in our session for later use
        session.setNote(Constants.FORM_REQUEST_NOTE, saved);
    }


    /**
     * Return the request URI (with the corresponding query string, if any)
     * from the saved request so that we can redirect to it.
     *
     * @param session Our current session
     */
    //START Apache bug 36136: Refactor the login and error page forward
    //private String savedRequestURL(Session session) {
    protected String savedRequestURL(Session session) {
    //END Apache bug 36136: Refactor the login and error page forward

        SavedRequest saved =
            (SavedRequest) session.getNote(Constants.FORM_REQUEST_NOTE);
        if (saved == null)
            return (null);
        StringBuilder sb = new StringBuilder(saved.getRequestURI());
        if (saved.getQueryString() != null) {
            sb.append('?');
            sb.append(saved.getQueryString());
        }

        return (sb.toString());
    }

    private long getSsoVersion(HttpRequest request) {
        long ssoVersion = 0L;
        Long ssoVersionObj = (Long)request.getNote(
                Constants.REQ_SSO_VERSION_NOTE);
        if (ssoVersionObj != null) {
            ssoVersion = ssoVersionObj.longValue();
        }
        return ssoVersion;
    }
}

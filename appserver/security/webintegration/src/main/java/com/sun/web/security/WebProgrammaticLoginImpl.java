/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2018-2021] [Payara Foundation and/or its affiliates]
package com.sun.web.security;

import static com.sun.logging.LogDomains.SECURITY_LOGGER;
import static java.util.logging.Level.FINE;

import java.io.IOException;
import java.security.AccessControlException;
import java.util.logging.Logger;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletRequestWrapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.apache.catalina.Context;
import org.apache.catalina.Manager;
import org.apache.catalina.Session;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.RequestFacade;
import org.jvnet.hk2.annotations.Service;

import com.sun.enterprise.security.SecurityContext;
import com.sun.enterprise.security.auth.WebAndEjbToJaasBridge;
import com.sun.enterprise.security.web.integration.WebPrincipal;
import com.sun.enterprise.security.web.integration.WebProgrammaticLogin;
import com.sun.logging.LogDomains;

/**
 * Internal implementation for servlet programmatic login.
 *
 * @see com.sun.enterprise.security.ee.auth.login.ProgrammaticLogin
 *
 */
@Service
public class WebProgrammaticLoginImpl implements WebProgrammaticLogin {

    // Used for the auth-type string.
    public static final String WEBAUTH_PROGRAMMATIC = "PROGRAMMATIC";

    private static Logger logger = LogDomains.getLogger(WebProgrammaticLoginImpl.class, SECURITY_LOGGER);

    /**
     * Login and set up principal in request and session. This implements programmatic login for servlets.
     *
     * <P>
     * Due to a number of bugs in RI the security context is not shared between web container and ejb container. In order
     * for an identity established by programmatic login to be known to both containers, it needs to be set not only in the
     * security context but also in the current request and, if applicable, the session object. If a session does not exist
     * this method does not create one.
     *
     * <P>
     * See bugs 4646134, 4688449 and other referenced bugs for more background.
     *
     * <P>
     * Note also that this login does not hook up into SSO.
     *
     * @param user User name to login.
     * @param password User password.
     * @param request HTTP request object provided by caller application. It should be an instance of HttpRequestFacade.
     * @param response HTTP response object provided by called application. It should be an instance of HttpServletResponse.
     * This is not used currently.
     * @param realm the realm name to be authenticated to. If the realm is null, authentication takes place in default realm
     * @returns A Boolean object; true if login succeeded, false otherwise.
     * @see com.sun.enterprise.security.ee.auth.login.ProgrammaticLogin
     * @throws Exception on login failure.
     *
     */
    @Override
    public Boolean login(String user, char[] password, String realm, HttpServletRequest request, HttpServletResponse response) {
        // Need real request object not facade
        Request unwrappedCoyoteRequest = getUnwrappedCoyoteRequest(request);
        if (unwrappedCoyoteRequest == null) {
            return false;
        }

        // Try to login - this will set up security context on success
        WebAndEjbToJaasBridge.login(user, password, realm);

        // Create a WebPrincipal for tomcat and store in current request
        // This will allow programmatic authorization later in this request
        // to work as expected.

        SecurityContext securityContext = SecurityContext.getCurrent();

        WebPrincipal principal = new WebPrincipal(user, password, securityContext);
        unwrappedCoyoteRequest.setUserPrincipal(principal);
        unwrappedCoyoteRequest.setAuthType(WEBAUTH_PROGRAMMATIC);

        if (logger.isLoggable(FINE)) {
            logger.log(FINE, "Programmatic login set principal in http request to: " + user);
        }

        // Try to retrieve a Session object (not the facade); if it exists
        // store the principal there as well. This will allow web container
        // authorization to work in subsequent requests in this session.

        Session realSession = getSession(unwrappedCoyoteRequest);
        if (realSession != null) {
            realSession.setPrincipal(principal);
            realSession.setAuthType(WEBAUTH_PROGRAMMATIC);
            logger.fine("Programmatic login set principal in session.");
        } else {
            logger.fine("Programmatic login: No session available.");
        }

        return true;
    }
    
    /**
     * Logout and remove principal in request and session.
     *
     * @param request HTTP request object provided by caller application. It should be an instance of HttpRequestFacade.
     * @param response HTTP response object provided by called application. It should be an instance of HttpServletResponse.
     * This is not used currently.
     * @returns A Boolean object; true if login succeeded, false otherwise.
     * @see com.sun.enterprise.security.ee.auth.login.ProgrammaticLogin
     * @throws Exception any exception encountered during logout operation
     */
    @Override
    public Boolean logout(HttpServletRequest request, HttpServletResponse response) throws Exception {
        // Need real request object not facade
        Request unwrappedCoyoteRequest = getUnwrappedCoyoteRequest(request);
        if (unwrappedCoyoteRequest == null) {
            return false;
        }

        // Logout - clears out security context
        WebAndEjbToJaasBridge.logout();
        
        // Remove principal and auth type from request

        unwrappedCoyoteRequest.setUserPrincipal(null);
        unwrappedCoyoteRequest.setAuthType(null);
        logger.fine("Programmatic logout removed principal from request.");

        // Remove from session if possible.

        Session realSession = getSession(unwrappedCoyoteRequest);
        if (realSession != null) {
            realSession.setPrincipal(null);
            realSession.setAuthType(null);
            if (logger.isLoggable(FINE)) {
                logger.log(FINE, "Programmatic logout removed principal from " + "session.");
            }
        }

        return true;
    }
    
    
    // ################### Private Methods
    
    
    /**
     * Return the unwrapped <code>CoyoteRequest</code> object.
     */
    private static Request getUnwrappedCoyoteRequest(HttpServletRequest request) {
        Request unwrappedCoyoteRequest = null;
        
        ServletRequest servletRequest = request;
        try {
            ServletRequest prevServletRequest = null;
            while (servletRequest != prevServletRequest && servletRequest instanceof ServletRequestWrapper) {
                prevServletRequest = servletRequest;
                servletRequest = ((ServletRequestWrapper) servletRequest).getRequest();
            }

            if (servletRequest instanceof RequestFacade) {
                unwrappedCoyoteRequest = ((RequestFacade) servletRequest).getUnwrappedCoyoteRequest();
            }

        } catch (AccessControlException ex) {
            logger.fine("Programmatic login faiied to get request");
        }
        
        return unwrappedCoyoteRequest;
    }


    /**
     * Returns the underlying Session object from the request, if one is available, or null.
     *
     */
    private static Session getSession(Request request) {
        HttpSession session = request.getSession(false);

        if (session != null) {
            Context context = request.getContext();
            if (context != null) {
                Manager manager = context.getManager();
                if (manager != null) {
                    // Need to locate the real Session obj
                    String sessionId = session.getId();
                    try {
                        return manager.findSession(sessionId);
                    } catch (IOException e) {
                        // ignored
                        return null;
                    }
                }
            }
        }

        return null;
    }
}

/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2019] Payara Foundation and/or its affiliates. All rights reserved.
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
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package fish.payara.appserver.microprofile.service.security;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.MessagePolicy;
import javax.security.auth.message.callback.CallerPrincipalCallback;
import javax.security.auth.message.callback.PasswordValidationCallback;
import javax.security.auth.message.module.ServerAuthModule;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import fish.payara.appserver.microprofile.service.configuration.MicroProfileConfiguration;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.SEVERE;
import static javax.security.auth.message.AuthStatus.SEND_CONTINUE;
import static javax.security.auth.message.AuthStatus.SEND_FAILURE;
import static javax.security.auth.message.AuthStatus.SUCCESS;

public abstract class MicroProfileAuthModule implements ServerAuthModule {

    private String contextRoot = null;
    private CallbackHandler handler = null;
    private boolean securityEnabled = false;

    public static final String DEFAULT_USER_NAME = "mp";
    private static final String SAVED_SUBJECT = "Saved_Subject";
    private static final String USER_NAME = "userName";
    private static final String ORIG_REQUEST_PATH = "origRequestPath";
    private static final String LOGIN_PAGE = "/login.xhtml";
    private static final String ERROR_PAGE = "/error.xhtml";
    private static final Class[] SUPPORTED_MESSAGE_TYPES
            = new Class[]{HttpServletRequest.class, HttpServletResponse.class};
    private static final Logger LOGGER = Logger.getLogger(MicroProfileAuthModule.class.getName());

    protected abstract MicroProfileConfiguration getMicroProfileConfiguration();

    @Override
    public void initialize(MessagePolicy requestPolicy, MessagePolicy responsePolicy, CallbackHandler handler, Map options)
            throws AuthException {
        this.handler = handler;
        contextRoot = getMicroProfileConfiguration().getEndpoint();
        securityEnabled = Boolean.parseBoolean(getMicroProfileConfiguration().getSecurityEnabled());
    }

    @Override
    public Class[] getSupportedMessageTypes() {
        return SUPPORTED_MESSAGE_TYPES;
    }

    @Override
    public AuthStatus validateRequest(MessageInfo messageInfo, Subject clientSubject, Subject serviceSubject)
            throws AuthException {
        if (securityEnabled) {
            HttpServletRequest request = (HttpServletRequest) messageInfo.getRequestMessage();
            HttpServletResponse response = (HttpServletResponse) messageInfo.getResponseMessage();

            HttpSession session = request.getSession(true);

            Subject savedClientSubject = (Subject) session.getAttribute(SAVED_SUBJECT);
            String savedUsername = (String) session.getAttribute(USER_NAME);

            if (savedClientSubject != null && savedUsername != null) {
                // Caller authenticated before, re-apply authentication for this request
                return notifyContainerAboutLogin(clientSubject, savedUsername);
            }

            // See if the username / password has been passed in...
            String username = request.getParameter("j_username");
            char[] password = request.getParameter("j_password") != null
                    ? request.getParameter("j_password").toCharArray() : null;
            if ((username == null) || (password == null) || !request.getMethod().equalsIgnoreCase("post")) {
                // Credentials not passed in, show the login page
                return saveRequestAndForwardToLogin(session, request, response);
            }

            // Authenticate the details
            if (isCredentialsValid(clientSubject, username, password)) {
                notifyContainerAboutLogin(clientSubject, username);

                request.changeSessionId();

                // Save the Subject...
                session.setAttribute(SAVED_SUBJECT, clientSubject);

                // Save the userName
                session.setAttribute(USER_NAME, username);

                // Register the session as authenticated
                messageInfo.getMap().put("javax.servlet.http.registerSession", Boolean.TRUE.toString());

                return redirectBack(session, response);
            } else {
                return forwardToErrorPage(request, response);
            }
        } else {
            Callback[] callbacks = new Callback[]{new CallerPrincipalCallback(clientSubject, DEFAULT_USER_NAME)};

            try {
                handler.handle(callbacks);
            } catch (IOException | UnsupportedCallbackException ex) {
                LOGGER.log(SEVERE, null, ex);
            }

            return AuthStatus.SUCCESS;
        }
    }

    @Override
    public AuthStatus secureResponse(MessageInfo messageInfo, Subject serviceSubject) throws AuthException {
        return AuthStatus.SUCCESS;
    }

    @Override
    public void cleanSubject(MessageInfo messageInfo, Subject subject) throws AuthException {
        if (subject != null) {
            subject.getPrincipals().clear();
        }
    }

    private AuthStatus notifyContainerAboutLogin(Subject clientSubject, String username) throws AuthException {
        try {
            handler.handle(new Callback[]{new CallerPrincipalCallback(clientSubject, username)});
            return SUCCESS;
        } catch (IOException | UnsupportedCallbackException ex) {
            throw (AuthException) new AuthException().initCause(ex);
        }
    }

    private AuthStatus saveRequestAndForwardToLogin(HttpSession session, HttpServletRequest request, HttpServletResponse response) throws AuthException {

        // Save original request path
        String originalPath = request.getRequestURI();
        String queryString = request.getQueryString();
        if (queryString != null && !queryString.isEmpty()) {
            originalPath += "?" + queryString;
        }
        session.setAttribute(ORIG_REQUEST_PATH, originalPath);

        // Forward to login page
        try {
            request.getRequestDispatcher(LOGIN_PAGE)
                    .forward(request, response);

            return SEND_CONTINUE;
        } catch (Exception ex) {
            throw (AuthException) new AuthException().initCause(ex);
        }
    }

    private AuthStatus redirectBack(HttpSession session, HttpServletResponse response) throws AuthException {
        try {
            // Redirect to original path
            String origRequest = (String) session.getAttribute(ORIG_REQUEST_PATH);
            if (origRequest == null) {
                origRequest = contextRoot;
            }
            LOGGER.log(INFO, "Redirecting to {0}", origRequest);
            response.sendRedirect(response.encodeRedirectURL(origRequest));

            return SEND_CONTINUE;
        } catch (Exception ex) {
            throw (AuthException) new AuthException().initCause(ex);
        }
    }

    private boolean isCredentialsValid(Subject clientSubject, String username, char[] password) throws AuthException {
        PasswordValidationCallback pvCallback = new PasswordValidationCallback(
                clientSubject,
                username,
                password
        );

        try {
            handler.handle(new Callback[]{pvCallback});
        } catch (Exception ex) {
            throw (AuthException) new AuthException().initCause(ex);
        }
        return pvCallback.getResult();
    }

    private AuthStatus forwardToErrorPage(HttpServletRequest request, HttpServletResponse response) throws AuthException {
        try {
            request.getRequestDispatcher(ERROR_PAGE)
                    .forward(request, response);
            return SEND_FAILURE;
        } catch (Exception ex) {
            throw (AuthException) new AuthException().initCause(ex);
        }

    }
}

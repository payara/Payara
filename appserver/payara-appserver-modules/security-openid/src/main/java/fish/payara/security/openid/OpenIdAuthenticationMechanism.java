/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) [2018-2020] Payara Foundation and/or its affiliates. All rights reserved.
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
 */
package fish.payara.security.openid;

import fish.payara.security.annotations.OpenIdAuthenticationDefinition;
import static fish.payara.security.openid.api.OpenIdConstant.ERROR_DESCRIPTION_PARAM;
import static fish.payara.security.openid.api.OpenIdConstant.ERROR_PARAM;
import static fish.payara.security.openid.api.OpenIdConstant.EXPIRES_IN;
import static fish.payara.security.openid.api.OpenIdConstant.ID_TOKEN_HINT;
import static fish.payara.security.openid.api.OpenIdConstant.REFRESH_TOKEN;
import static fish.payara.security.openid.api.OpenIdConstant.STATE;
import static fish.payara.security.openid.api.OpenIdConstant.TOKEN_TYPE;
import static fish.payara.security.openid.api.OpenIdConstant.POST_LOGOUT_REDIRECT_URI;
import fish.payara.security.openid.api.OpenIdState;
import fish.payara.security.openid.api.RefreshToken;
import fish.payara.security.openid.controller.AuthenticationController;
import fish.payara.security.openid.controller.ConfigurationController;
import fish.payara.security.openid.controller.StateController;
import fish.payara.security.openid.controller.TokenController;
import fish.payara.security.openid.domain.LogoutConfiguration;
import fish.payara.security.openid.domain.OpenIdConfiguration;
import fish.payara.security.openid.domain.OpenIdContextImpl;
import fish.payara.security.openid.domain.RefreshTokenImpl;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import java.util.Optional;
import java.util.logging.Level;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.WARNING;
import java.util.logging.Logger;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.message.callback.CallerPrincipalCallback;
import javax.security.enterprise.AuthenticationException;
import javax.security.enterprise.AuthenticationStatus;
import static javax.security.enterprise.AuthenticationStatus.SEND_FAILURE;
import static javax.security.enterprise.AuthenticationStatus.SUCCESS;
import javax.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import javax.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import javax.security.enterprise.identitystore.CredentialValidationResult;
import static javax.security.enterprise.identitystore.CredentialValidationResult.INVALID_RESULT;
import static javax.security.enterprise.identitystore.CredentialValidationResult.NOT_VALIDATED_RESULT;
import javax.security.enterprise.identitystore.IdentityStoreHandler;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import static org.glassfish.common.util.StringHelper.isEmpty;

/**
 * The AuthenticationMechanism used to authenticate users using the OpenId
 * Connect protocol
 * <br/>
 * Specification Implemented :
 * http://openid.net/specs/openid-connect-core-1_0.html
 *
 * @author Gaurav Gupta
 */
//  +--------+                                                       +--------+
//  |        |                                                       |        |
//  |        |---------------(1) Authentication Request------------->|        |
//  |        |                                                       |        |
//  |        |       +--------+                                      |        |
//  |        |       |  End-  |<--(2) Authenticates the End-User---->|        |
//  |   RP   |       |  User  |                                      |   OP   |
//  |        |       +--------+                                      |        |
//  |        |                                                       |        |
//  |        |<---------(3) returns Authorization code---------------|        |
//  |        |                                                       |        |
//  |        |                                                       |        |
//  |        |------------------------------------------------------>|        |
//  |        |   (4) Request to TokenEndpoint for Access / Id Token  |        |
//  | OpenId |<------------------------------------------------------| OpenId |
//  | Connect|                                                       | Connect|
//  | Client | ----------------------------------------------------->|Provider|
//  |        |   (5) Fetch JWKS to validate ID Token                 |        |
//  |        |<------------------------------------------------------|        |
//  |        |                                                       |        |
//  |        |------------------------------------------------------>|        |
//  |        |   (6) Request to UserInfoEndpoint for End-User Claims |        |
//  |        |<------------------------------------------------------|        |
//  |        |                                                       |        |
//  +--------+                                                       +--------+
@Typed(OpenIdAuthenticationMechanism.class)
public class OpenIdAuthenticationMechanism implements HttpAuthenticationMechanism {

    private OpenIdConfiguration configuration;

    @Inject
    private OpenIdContextImpl context;

    @Inject
    private IdentityStoreHandler identityStoreHandler;

    @Inject
    private ConfigurationController configurationController;

    @Inject
    private AuthenticationController authenticationController;

    @Inject
    private TokenController tokenController;

    @Inject
    private StateController stateController;

    private static final Logger LOGGER = Logger.getLogger(OpenIdAuthenticationMechanism.class.getName());

    private static class Lock implements Serializable {
    }

    private static final String SESSION_LOCK_NAME = OpenIdAuthenticationMechanism.class.getName();

    /**
     * Creates an {@link OpenIdAuthenticationMechanism}.
     * <p>
     * If this constructor is used then {@link #setConfiguration(OpenIdAuthenticationDefinition) must be
     * called before any requests are validated.
     */
    public OpenIdAuthenticationMechanism() {
    }

    /**
     * Creates an {@link OpenIdAuthenticationMechanism} that has been defined
     * using an annotation
     *
     * @param definition
     */
    public OpenIdAuthenticationMechanism(OpenIdAuthenticationDefinition definition) {
        this();
        setConfiguration(definition);
    }

    /**
     * Sets the properties of the {@link OpenIdAuthenticationMechanism} as
     * defined in an {@link OpenIdAuthenticationDefinition} annotation or using
     * MP Config source. MP Config take precedence over
     * {@link OpenIdAuthenticationDefinition} annotation value
     *
     * @param definition
     * @return
     */
    public OpenIdAuthenticationMechanism setConfiguration(OpenIdAuthenticationDefinition definition) {
        this.configuration = configurationController.buildConfig(definition);
        return this;
    }

    @Override
    public AuthenticationStatus validateRequest(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpMessageContext httpContext) throws AuthenticationException {

        if (isNull(request.getUserPrincipal())) {
            LOGGER.fine("UserPrincipal is not set, authenticate user using OpenId Connect protocol.");
            // User is not authenticated
            // Perform steps (1) to (6)
            return this.authenticate(request, response, httpContext);
        } else {
            // User has been authenticated in request before

            // Try-catch-block taken from AutoApplySessionInterceptor
            // We cannot use @AutoApplySession, because validateRequest(...) must be called on every request
            // to handle re-authentication (refreshing tokens)
            // https://stackoverflow.com/questions/51678821/soteria-httpmessagecontext-setregistersession-not-working-as-expected/51819055
            // https://github.com/javaee/security-soteria/blob/master/impl/src/main/java/org/glassfish/soteria/cdi/AutoApplySessionInterceptor.java
            try {
                httpContext.getHandler().handle(new Callback[]{
                    new CallerPrincipalCallback(httpContext.getClientSubject(), request.getUserPrincipal())}
                );
            } catch (IOException | UnsupportedCallbackException ex) {
                throw new AuthenticationException("Failed to register CallerPrincipalCallback.", ex);
            }

            LogoutConfiguration logout = configuration.getLogoutConfiguration();
            boolean accessTokenExpired = this.context.getAccessToken().isExpired();
            boolean identityTokenExpired = this.context.getIdentityToken().isExpired();
            if (logout.isIdentityTokenExpiry()) {
                LOGGER.log(Level.FINE, "UserPrincipal is set, check if Identity Token is valid.");
            }
            if (logout.isAccessTokenExpiry()) {
                LOGGER.log(Level.FINE, "UserPrincipal is set, check if Access Token is valid.");
            }

            if ((accessTokenExpired || identityTokenExpired) && configuration.isTokenAutoRefresh()) {
                if (accessTokenExpired) {
                    LOGGER.fine("Access Token is expired. Request new Access Token with Refresh Token.");
                }
                if (identityTokenExpired) {
                    LOGGER.fine("Identity Token is expired. Request new Identity Token with Refresh Token.");
                }
                return this.reAuthenticate(httpContext);
            } else if ((logout.isAccessTokenExpiry() && accessTokenExpired)
                    || (logout.isIdentityTokenExpiry() && identityTokenExpired)) {
                context.logout(request, response);
                return SEND_FAILURE;
            } else {
                return SUCCESS;
            }
        }
    }

    private AuthenticationStatus authenticate(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpMessageContext httpContext) throws AuthenticationException {

        if (httpContext.isProtected() && isNull(request.getUserPrincipal())) {
            // (1) The End-User is not already authenticated
            return authenticationController.authenticateUser(configuration, request, response);
        }

        Optional<OpenIdState> receivedState = OpenIdState.from(request.getParameter(STATE));
        String redirectURI = configuration.buildRedirectURI(request);
        if (receivedState.isPresent()) {
            if (!request.getRequestURL().toString().equals(redirectURI)) {
                LOGGER.log(INFO, "OpenID Redirect URL {0} not matched with request URL {1}", new Object[]{redirectURI, request.getRequestURL().toString()});
                return httpContext.notifyContainerAboutLogin(NOT_VALIDATED_RESULT);
            }
            Optional<OpenIdState> expectedState = stateController.get(configuration, request, response);
            if (!expectedState.isPresent()) {
                LOGGER.fine("Expected state not found");
                return httpContext.notifyContainerAboutLogin(NOT_VALIDATED_RESULT);
            }
            if (!expectedState.equals(receivedState)) {
                LOGGER.fine("Inconsistent received state, value not matched");
                return httpContext.notifyContainerAboutLogin(INVALID_RESULT);
            }
            // (3) Successful Authentication Response : redirect_uri?code=abc&state=123
            return validateAuthorizationCode(httpContext);
        }
        return httpContext.doNothing();
    }

    /**
     * (3) & (4-6) An Authorization Code returned to Client (RP) via
     * Authorization Code Flow must be validated and exchanged for an ID Token,
     * an Access Token and optionally a Refresh Token directly.
     *
     * @param httpContext the {@link HttpMessageContext} to validate
     * authorization code from
     * @return the authentication status.
     */
    private AuthenticationStatus validateAuthorizationCode(HttpMessageContext httpContext) {
        HttpServletRequest request = httpContext.getRequest();
        HttpServletResponse response = httpContext.getResponse();
        String error = request.getParameter(ERROR_PARAM);
        String errorDescription = request.getParameter(ERROR_DESCRIPTION_PARAM);
        if (!isEmpty(error)) {
            // Error responses sent to the redirect_uri
            LOGGER.log(WARNING, "Error occurred in receiving Authorization Code : {0} caused by {1}", new Object[]{error, errorDescription});
            return httpContext.notifyContainerAboutLogin(INVALID_RESULT);
        }
        stateController.remove(configuration, request, response);

        LOGGER.finer("Authorization Code received, now fetching Access token & Id token");

        Response tokenResponse = tokenController.getTokens(configuration, request);
        JsonObject tokensObject = readJsonObject(tokenResponse.readEntity(String.class));
        if (tokenResponse.getStatus() == Status.OK.getStatusCode()) {
            // Successful Token Response
            updateContext(tokensObject);
            OpenIdCredential credential = new OpenIdCredential(tokensObject, httpContext, configuration);
            CredentialValidationResult validationResult = identityStoreHandler.validate(credential);

            // Register session manually (if @AutoApplySession used, this would be done by its interceptor)
            httpContext.setRegisterSession(validationResult.getCallerPrincipal().getName(), validationResult.getCallerGroups());
            return httpContext.notifyContainerAboutLogin(validationResult);
        } else {
            // Token Request is invalid or unauthorized
            error = tokensObject.getString(ERROR_PARAM, "Unknown Error");
            errorDescription = tokensObject.getString(ERROR_DESCRIPTION_PARAM, "Unknown");
            LOGGER.log(WARNING, "Error occurred in validating Authorization Code : {0} caused by {1}", new Object[]{error, errorDescription});
            return httpContext.notifyContainerAboutLogin(INVALID_RESULT);
        }
    }

    private AuthenticationStatus reAuthenticate(HttpMessageContext httpContext) throws AuthenticationException {
        HttpServletRequest request = httpContext.getRequest();
        HttpServletResponse response = httpContext.getResponse();
        synchronized (this.getSessionLock(httpContext.getRequest())) {
            boolean accessTokenExpired = this.context.getAccessToken().isExpired();
            boolean identityTokenExpired = this.context.getIdentityToken().isExpired();
            if (accessTokenExpired || identityTokenExpired) {

                if (accessTokenExpired) {
                    LOGGER.fine("Access Token is expired. Request new Access Token with Refresh Token.");
                }
                if (identityTokenExpired) {
                    LOGGER.fine("Identity Token is expired. Request new Identity Token with Refresh Token.");
                }

                AuthenticationStatus refreshStatus = this.context.getRefreshToken()
                        .map(rt -> this.refreshTokens(httpContext, rt))
                        .orElse(AuthenticationStatus.SEND_FAILURE);

                if (refreshStatus != AuthenticationStatus.SUCCESS) {
                    LOGGER.log(Level.FINE, "Failed to refresh token (Refresh Token might be invalid).");
                    context.logout(request, response);
                }
                return refreshStatus;
            }
        }

        return SUCCESS;
    }

    private AuthenticationStatus refreshTokens(HttpMessageContext httpContext, RefreshToken refreshToken) {
        Response response = tokenController.refreshTokens(configuration, refreshToken);
        JsonObject tokensObject = readJsonObject(response.readEntity(String.class));

        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            // Successful Token Response
            updateContext(tokensObject);
            OpenIdCredential credential = new OpenIdCredential(tokensObject, httpContext, configuration);
            CredentialValidationResult validationResult = identityStoreHandler.validate(credential);

            // Do not register session, as this will invalidate the currently active session (destroys session beans and removes attributes set in session)!
            // httpContext.setRegisterSession(validationResult.getCallerPrincipal().getName(), validationResult.getCallerGroups());
            return httpContext.notifyContainerAboutLogin(validationResult);
        } else {
            // Token Request is invalid (refresh token invalid or expired)
            String error = tokensObject.getString(ERROR_PARAM, "Unknown Error");
            String errorDescription = tokensObject.getString(ERROR_DESCRIPTION_PARAM, "Unknown");
            LOGGER.log(Level.FINE, "Error occurred in refreshing Access Token and Refresh Token : {0} caused by {1}", new Object[]{error, errorDescription});
            return AuthenticationStatus.SEND_FAILURE;
        }
    }

    private JsonObject readJsonObject(String tokensBody) {
        try (JsonReader reader = Json.createReader(new StringReader(tokensBody))) {
            return reader.readObject();
        }
    }

    private void updateContext(JsonObject tokensObject) {
        context.setOpenIdConfiguration(configuration);
        context.setTokenType(tokensObject.getString(TOKEN_TYPE, null));

        String refreshToken = tokensObject.getString(REFRESH_TOKEN, null);
        if (nonNull(refreshToken)) {
            context.setRefreshToken(new RefreshTokenImpl(refreshToken));
        }
        JsonNumber expiresIn = tokensObject.getJsonNumber(EXPIRES_IN);
        if (nonNull(expiresIn)) {
            context.setExpiresIn(expiresIn.longValue());
        }
    }

    private Object getSessionLock(HttpServletRequest request) {
        HttpSession session = request.getSession();
        Object lock = session.getAttribute(SESSION_LOCK_NAME);
        if (isNull(lock)) {
            synchronized (OpenIdAuthenticationMechanism.class) {
                lock = session.getAttribute(SESSION_LOCK_NAME);
                if (isNull(lock)) {
                    lock = new Lock();
                    session.setAttribute(SESSION_LOCK_NAME, lock);
                }

            }
        }
        return lock;
    }

}

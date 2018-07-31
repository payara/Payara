/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright (c) [2018] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.security.oidc;

import com.nimbusds.jwt.JWT;
import fish.payara.security.oidc.domain.OidcContextImpl;
import fish.payara.security.oidc.domain.OidcConfiguration;
import fish.payara.security.oidc.controller.TokenController;
import fish.payara.security.oidc.controller.ConfigurationController;
import java.io.StringReader;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.security.enterprise.AuthenticationException;
import javax.security.enterprise.AuthenticationStatus;
import javax.security.enterprise.authentication.mechanism.http.AutoApplySession;
import javax.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import javax.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import javax.security.enterprise.identitystore.CredentialValidationResult;
import javax.security.enterprise.identitystore.IdentityStoreHandler;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import static fish.payara.security.oidc.api.OidcConstant.ACCESS_TOKEN;
import static fish.payara.security.oidc.api.OidcConstant.EXPIRES_IN;
import static fish.payara.security.oidc.api.OidcConstant.IDENTITY_TOKEN;
import static fish.payara.security.oidc.api.OidcConstant.REFRESH_TOKEN;
import static fish.payara.security.oidc.api.OidcConstant.STATE;
import static fish.payara.security.oidc.api.OidcConstant.TOKEN_TYPE;
import fish.payara.security.oidc.api.OidcState;
import javax.ws.rs.core.Response.Status;
import static java.util.logging.Level.WARNING;
import static javax.security.enterprise.identitystore.CredentialValidationResult.INVALID_RESULT;
import static javax.security.enterprise.identitystore.CredentialValidationResult.NOT_VALIDATED_RESULT;
import fish.payara.security.annotations.OidcAuthenticationDefinition;
import static fish.payara.security.oidc.api.OidcConstant.ERROR_DESCRIPTION_PARAM;
import static fish.payara.security.oidc.api.OidcConstant.ERROR_PARAM;
import fish.payara.security.oidc.controller.AuthenticationController;
import fish.payara.security.oidc.controller.UserInfoController;
import fish.payara.security.oidc.domain.OidcNonce;
import java.util.Map;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import java.util.logging.Logger;
import javax.security.enterprise.credential.CallerOnlyCredential;
import static org.glassfish.common.util.StringHelper.isEmpty;

/**
 * The AuthenticationMechanism used for authenticate users using the OIDC
 * protocol
 * Specification : http://openid.net/specs/openid-connect-core-1_0.html

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
 //  |        |<------------------------------------------------------|        |
 //  |  OIDC  |                                                       |  OIDC  |
 //  | Client | ----------------------------------------------------->|Provider|
 //  |        |   (5) Fetch JWKS to validate ID Token                 |        |
 //  |        |<------------------------------------------------------|        |
 //  |        |                                                       |        |
 //  |        |------------------------------------------------------>|        |
 //  |        |   (6) Request to UserInfoEndpoint for End-User Claims |        |
 //  |        |<------------------------------------------------------|        |
 //  |        |                                                       |        |
 //  +--------+                                                       +--------+

@AutoApplySession
@Typed(OidcAuthenticationMechanism.class)
public class OidcAuthenticationMechanism implements HttpAuthenticationMechanism {

    private OidcConfiguration configuration;

    @Inject
    private OidcState state;

    @Inject
    private OidcNonce nonce;

    @Inject
    private OidcContextImpl context;

    @Inject
    private IdentityStoreHandler identityStoreHandler;

    @Inject
    private ConfigurationController configurationController;

    @Inject
    private AuthenticationController authenticationController;

    @Inject
    private TokenController tokenController;

    @Inject
    private UserInfoController userInfoController;

    private static final Logger LOGGER = Logger.getLogger(OidcAuthenticationMechanism.class.getName());

    /**
     * Creates an OIDCAuthenticationMechanism.
     * <p>
     * If this constructor is used then {@link #setDefinition(OIDCAuthenticationDefinition) must be
     * called before any requests are validated.
     */
    public OidcAuthenticationMechanism() {
    }

    /**
     * Creates an OIDCAuthenticationMechanism that has been defined using an
     * annotation
     *
     * @param definition
     */
    public OidcAuthenticationMechanism(OidcAuthenticationDefinition definition) {
        this();
        setConfiguration(definition);
    }

    /**
     * Sets the properties of the OIDCAuthenticationMechanism as defined in an
     * {@link OidcAuthenticationDefinition} annotation or using MP Config source.
     * MP Config take precedence over {@link OidcAuthenticationDefinition}
     * annotation value
     *
     * @param definition
     * @return
     */
    public OidcAuthenticationMechanism setConfiguration(OidcAuthenticationDefinition definition) {
        this.configuration = configurationController.buildConfig(definition);
        return this;
    }

    @Override
    public AuthenticationStatus validateRequest(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpMessageContext httpMessageContext) throws AuthenticationException {

        if (httpMessageContext.isProtected() && isNull(request.getUserPrincipal())) {
            // (1) The End-User is not already authenticated
            return authenticationController.authenticateUser(configuration, state, nonce, httpMessageContext);
        }

        if (request.getRequestURL().toString().equals(configuration.getRedirectURI())) {
            String recievedState = request.getParameter(STATE);
            if (nonNull(recievedState) && recievedState.equals(state.getState())) {
                // (3) Successful Authentication Response : redirect_uri?code=abc&state=123
                return validateAuthorizationCode(request, httpMessageContext);
            } else if (nonNull(recievedState) && !recievedState.equals(state.getState())) {
                LOGGER.fine("Inconsistent recieved state, value not matched");
                return httpMessageContext.notifyContainerAboutLogin(NOT_VALIDATED_RESULT);
            }
        }
        return httpMessageContext.doNothing();
    }

    /**
     * (3) & (4-6) 
     * An Authorization Code returned to Client (RP) via Authorization Code Flow
     * must be validated and exchanged for an ID Token, an Access Token and 
     * optionally a Refresh Token directly.
     *
     * @param request
     * @param context
     * @return
     */
    private AuthenticationStatus validateAuthorizationCode(HttpServletRequest request, HttpMessageContext httpMessageContext) {
        String error = request.getParameter(ERROR_PARAM);
        String errorDescription = request.getParameter(ERROR_DESCRIPTION_PARAM);
        if (!isEmpty(error)) {
            // Error responses sent to the redirect_uri
            LOGGER.log(WARNING, "Error occurred in reciving Authorization Code : {0} caused by {1}", new Object[]{error, errorDescription});
            return httpMessageContext.notifyContainerAboutLogin(INVALID_RESULT);
        }

        LOGGER.finer("Authorization Code received, now fetching Access token & Id token");

        Response response = tokenController.getTokens(configuration, request);
        String tokensBody = response.readEntity(String.class);
        JsonObject tokensObject = Json.createReader(new StringReader(tokensBody)).readObject();
        
        if (response.getStatus() == Status.OK.getStatusCode()) {
            // Successful Token Response
            updateContext(tokensObject);
            CallerOnlyCredential credential = new CallerOnlyCredential(context.getSubject());
            CredentialValidationResult validationResult = identityStoreHandler.validate(credential);
            return httpMessageContext.notifyContainerAboutLogin(validationResult);
        } else {
            // Token Request is invalid or unauthorized
            error = tokensObject.getString(ERROR_PARAM, "Unknown Error");
            errorDescription = tokensObject.getString(ERROR_DESCRIPTION_PARAM, "Unknown");
            LOGGER.log(WARNING, "Error occurred in validating Authorization Code : {0} caused by {1}", new Object[]{error, errorDescription});
            return httpMessageContext.notifyContainerAboutLogin(INVALID_RESULT);
        }
    }

    private void updateContext(JsonObject tokensObject) {
        String idToken = tokensObject.getString(IDENTITY_TOKEN);
        JWT idTokenJWT = tokenController.parseIdToken(configuration, nonce, idToken);
        Map<String, Object> claims = tokenController.validateIdToken(configuration, nonce, idTokenJWT);
        context.setIdentityToken(idToken);
        context.setIdentityTokenClaims(claims);

        String accessToken = tokensObject.getString(ACCESS_TOKEN, null);
        if (nonNull(accessToken)) {
            tokenController.validateAccessToken(context, accessToken, idTokenJWT.getHeader().getAlgorithm());
            context.setAccessToken(accessToken);
            JsonObject userInfo = userInfoController.getUserInfo(configuration, accessToken);
            context.setClaims(userInfo);
        }

        context.setProviderMetadata(configuration.getProviderMetadata().getDocument());
        context.setTokenType(tokensObject.getString(TOKEN_TYPE, null));
        context.setRefreshToken(tokensObject.getString(REFRESH_TOKEN, null));
        String expiresIn = tokensObject.getString(EXPIRES_IN, null);
        if (nonNull(expiresIn)) {
            context.setExpiresIn(Integer.parseInt(expiresIn));
        }
    }

}

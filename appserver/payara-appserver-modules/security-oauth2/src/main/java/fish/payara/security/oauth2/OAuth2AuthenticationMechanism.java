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
package fish.payara.security.oauth2;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.el.ELProcessor;
import javax.enterprise.inject.Typed;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.security.enterprise.AuthenticationException;
import javax.security.enterprise.AuthenticationStatus;
import javax.security.enterprise.authentication.mechanism.http.AutoApplySession;
import javax.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import javax.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import javax.security.enterprise.credential.RememberMeCredential;
import javax.security.enterprise.identitystore.CredentialValidationResult;
import javax.security.enterprise.identitystore.IdentityStoreHandler;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.glassfish.config.support.TranslatedConfigView;

import fish.payara.security.annotations.OAuth2AuthenticationDefinition;
import fish.payara.security.oauth2.api.OAuth2State;

/**
 * The AuthenticationMechanism used for authenticate users using the OAuth2 protocol
 *
 * @author jonathan coustick
 * @since 4.1.2.182
 */
@AutoApplySession
@Typed(OAuth2AuthenticationMechanism.class)
public class OAuth2AuthenticationMechanism implements HttpAuthenticationMechanism {

    private static final Logger logger = Logger.getLogger("OAuth2Mechanism");

    private final ELProcessor elProcessor;

    private String authEndpoint;
    private String tokenEndpoint;
    private String clientID;
    private char[] clientSecret;
    private String redirectURI;
    private String scopes;
    private String[] extraParameters;

    @Inject
    private OAuth2State state;

    @Inject
    private OAuth2StateHolder tokenHolder;

    @Inject
    private IdentityStoreHandler identityStoreHandler;

    /**
     * Creates an OAuth2AuthenticationMechanism.
     * <p>
     * If this constructor is used then {@link #setDefinition(OAuth2AuthenticationDefinition) must be
     * called before any requests are validated.
     */
    public OAuth2AuthenticationMechanism() {
        elProcessor = new ELProcessor();
        BeanManager beanManager = CDI.current().getBeanManager();
        elProcessor.getELManager().addELResolver(beanManager.getELResolver());
    }

    /**
     * Creates an OAuth2AuthenticationMechanism that has been defined using an annotation
     *
     * @param definition
     */
    public OAuth2AuthenticationMechanism(OAuth2AuthenticationDefinition definition) {
        this();
        setDefinition(definition);

    }

    /**
     * Sets the properties of the OAuth2AuthenticationMechanism as defined in an {@link OAuth2AuthenticationDefinition} annotation.
     *
     * @param definition
     * @return
     */
    public OAuth2AuthenticationMechanism setDefinition(OAuth2AuthenticationDefinition definition) {
        Config provider = ConfigProvider.getConfig();
        authEndpoint = getConfiguredValue(definition.authEndpoint(), provider, OAuth2AuthenticationDefinition.OAUTH2_MP_AUTH_ENDPOINT);
        tokenEndpoint = getConfiguredValue(definition.tokenEndpoint(), provider, OAuth2AuthenticationDefinition.OAUTH2_MP_TOKEN_ENDPOINT);
        clientID = getConfiguredValue(definition.clientId(), provider, OAuth2AuthenticationDefinition.OAUTH2_MP_CLIENT_ID);
        clientSecret = getConfiguredValue(definition.clientSecret(), provider, OAuth2AuthenticationDefinition.OAUTH2_MP_CLIENT_SECRET).toCharArray();
        redirectURI = getConfiguredValue(definition.redirectURI(), provider, OAuth2AuthenticationDefinition.OAUTH2_MP_REDIRECT_URI);
        scopes = getConfiguredValue(definition.scope(), provider, OAuth2AuthenticationDefinition.OAUTH2_MP_SCOPE);

        String[] params = definition.extraParameters();
        extraParameters = new String[params.length];
        for (int i = 0; i < params.length; i++) {
            extraParameters[i] = getConfiguredValue(params[i], provider, params[i]);
        }
        return this;
    }

    @Override
    public AuthenticationStatus validateRequest(HttpServletRequest request, HttpServletResponse response, HttpMessageContext httpMessageContext)
            throws AuthenticationException {

        if (httpMessageContext.isProtected() && request.getUserPrincipal() == null) {
            //Needs to login and has not already done so
            return redirectForAuth(httpMessageContext);
        }

        String recievedState = request.getParameter("state");
        if (request.getRequestURL().toString().equals(redirectURI) && recievedState != null) {
            //In the process of logging in
            if (recievedState.equals(state.getState())) {
                return validateCallback(request, httpMessageContext);
            } else {
                logger.log(Level.FINE, "Inconsistent recieved state. This may be caused by using the back button in the browser.");
                return httpMessageContext.notifyContainerAboutLogin(CredentialValidationResult.NOT_VALIDATED_RESULT);
            }
        } else {

            // Is not trying to login, has not logged in already and does not need to login
            logger.log(Level.FINEST, "Authentication mechanism doing nothing");
            return httpMessageContext.doNothing();
        }
    }

    /**
     * Called when the callback URL is hit, this gets the authorisation token and logs the user in
     *
     * @param request
     * @param context
     * @return
     */
    private AuthenticationStatus validateCallback(HttpServletRequest request, HttpMessageContext context) {
        logger.log(Level.FINER, "User Authenticated, now getting authorisation token");

        Map<String, String> formData = new HashMap<>();
        //Creates a new JAX-RS form with all paramters
        formData.put("grant_type", "authorization_code");
        formData.put("client_id", clientID);
        formData.put("client_secret", new String(clientSecret));
        formData.put("code", request.getParameter("code"));
        formData.put("state", state.getState());
        if (redirectURI != null && !redirectURI.isEmpty()) {
            formData.put("redirect_uri", redirectURI);
        }
        if (scopes != null && !scopes.isEmpty()) {
            formData.put("scope", scopes);
        }
        for (String extra : extraParameters) {
            String[] parts = extra.split("=");
            formData.put(parts[0], parts[1]);
        }

        OAuth2Client client = new OAuth2Client(tokenEndpoint, request.getRequestURL().toString(), formData);

        // Get back the result of the REST request
        Response response = client.authenticate();
        String resultString = response.readEntity(String.class);
        JsonObject result = readJsonObject(resultString);
        logger.log(Level.FINEST, "Response code from endpoint: {0}", response.getStatus());
        if (response.getStatus() != 200) {

            String error = result.getString("error", "Unknown Error");
            String errorDescription = result.getString("error_description", "Unknown");
            logger.log(Level.WARNING, "[OAUTH-001] Error occurred authenticating user: {0} caused by {1}", new Object[]{error, errorDescription});
            return context.notifyContainerAboutLogin(CredentialValidationResult.INVALID_RESULT);
        } else {

            tokenHolder.setAccessToken(result.getString("access_token"));
            tokenHolder.setRefreshToken(result.getString("refresh_token", null));
            tokenHolder.setScope(result.getString("scope", null));
            JsonNumber expiresIn = result.getJsonNumber("expires_in");
            if (expiresIn != null) {
                tokenHolder.setExpiresIn(expiresIn.longValue());
            }

            RememberMeCredential credential = new RememberMeCredential(resultString);
            CredentialValidationResult validationResult = identityStoreHandler.validate(credential);
            return context.notifyContainerAboutLogin(validationResult);
        }
    }

    private JsonObject readJsonObject(String result) {
        try (JsonReader reader = Json.createReader(new StringReader(result))) {
            return reader.readObject();
        }
    }

    /**
     * If the user is not logged in then redirect them to OAuth provider
     *
     * @param context
     * @return
     */
    private AuthenticationStatus redirectForAuth(HttpMessageContext context) {
        logger.log(Level.FINEST, "Redirecting for authentication to {0}", authEndpoint);
        StringBuilder authTokenRequest = new StringBuilder(authEndpoint);
        authTokenRequest.append("?client_id=").append(clientID);
        authTokenRequest.append("&state=").append(state.getState());
        authTokenRequest.append("&response_type=code");
        if (redirectURI != null && !redirectURI.isEmpty()) {
            authTokenRequest.append("&redirect_uri=").append(redirectURI);
        }
        if (scopes != null && !scopes.isEmpty()) {
            authTokenRequest.append("&scope=").append(scopes);
        }
        for (String extra : extraParameters) {
            authTokenRequest.append("&").append(extra);
        }

        return context.redirect(authTokenRequest.toString());
    }


    private String getConfiguredValue(String value, Config provider, String mpConfigKey){
        String result = value;
        Optional<String> configResult = provider.getOptionalValue(mpConfigKey, String.class);
        if (configResult.isPresent()) {
            return configResult.get();
        }
        result = TranslatedConfigView.expandValue(result);
        if (isELExpression(value)){
            result = (String) elProcessor.getValue(toRawExpression(result), String.class);
        }

        return result;
    }

    private static boolean isELExpression(String expression) {
        return !expression.isEmpty() && isDeferredExpression(expression);
    }

    private static boolean isDeferredExpression(String expression) {
        return expression.startsWith("#{") && expression.endsWith("}");
    }
    private static String toRawExpression(String expression) {
        return expression.substring(2, expression.length() - 1);
    }
}

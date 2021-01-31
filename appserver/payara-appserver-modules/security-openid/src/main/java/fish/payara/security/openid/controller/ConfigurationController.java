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
package fish.payara.security.openid.controller;

import static fish.payara.security.annotations.ClaimsDefinition.OPENID_MP_CALLER_GROUP_CLAIM;
import static fish.payara.security.annotations.ClaimsDefinition.OPENID_MP_CALLER_NAME_CLAIM;
import static fish.payara.security.annotations.LogoutDefinition.OPENID_MP_PROVIDER_NOTIFY_LOGOUT;
import static fish.payara.security.annotations.LogoutDefinition.OPENID_MP_LOGOUT_ON_ACCESS_TOKEN_EXPIRY;
import static fish.payara.security.annotations.LogoutDefinition.OPENID_MP_LOGOUT_ON_IDENTITY_TOKEN_EXPIRY;
import static fish.payara.security.annotations.LogoutDefinition.OPENID_MP_POST_LOGOUT_REDIRECT_URI;
import fish.payara.security.annotations.OpenIdAuthenticationDefinition;
import static fish.payara.security.annotations.OpenIdAuthenticationDefinition.OPENID_MP_CLIENT_ENC_ALGORITHM;
import static fish.payara.security.annotations.OpenIdAuthenticationDefinition.OPENID_MP_CLIENT_ENC_JWKS;
import static fish.payara.security.annotations.OpenIdAuthenticationDefinition.OPENID_MP_CLIENT_ENC_METHOD;
import static fish.payara.security.annotations.OpenIdAuthenticationDefinition.OPENID_MP_CLIENT_ID;
import static fish.payara.security.annotations.OpenIdAuthenticationDefinition.OPENID_MP_CLIENT_SECRET;
import static fish.payara.security.annotations.OpenIdAuthenticationDefinition.OPENID_MP_DISPLAY;
import static fish.payara.security.annotations.OpenIdAuthenticationDefinition.OPENID_MP_JWKS_CONNECT_TIMEOUT;
import static fish.payara.security.annotations.OpenIdAuthenticationDefinition.OPENID_MP_JWKS_READ_TIMEOUT;
import static fish.payara.security.annotations.OpenIdAuthenticationDefinition.OPENID_MP_PROMPT;
import static fish.payara.security.annotations.OpenIdAuthenticationDefinition.OPENID_MP_PROVIDER_URI;
import static fish.payara.security.annotations.OpenIdAuthenticationDefinition.OPENID_MP_REDIRECT_URI;
import static fish.payara.security.annotations.OpenIdAuthenticationDefinition.OPENID_MP_RESPONSE_MODE;
import static fish.payara.security.annotations.OpenIdAuthenticationDefinition.OPENID_MP_RESPONSE_TYPE;
import static fish.payara.security.annotations.OpenIdAuthenticationDefinition.OPENID_MP_SCOPE;
import static fish.payara.security.annotations.OpenIdAuthenticationDefinition.OPENID_MP_TOKEN_AUTO_REFRESH;
import static fish.payara.security.annotations.OpenIdAuthenticationDefinition.OPENID_MP_TOKEN_MIN_VALIDITY;
import static fish.payara.security.annotations.OpenIdAuthenticationDefinition.OPENID_MP_USE_NONCE;
import static fish.payara.security.annotations.OpenIdAuthenticationDefinition.OPENID_MP_USE_SESSION;
import static fish.payara.security.annotations.OpenIdProviderMetadata.OPENID_MP_AUTHORIZATION_ENDPOINT;
import static fish.payara.security.annotations.OpenIdProviderMetadata.OPENID_MP_END_SESSION_ENDPOINT;
import static fish.payara.security.annotations.OpenIdProviderMetadata.OPENID_MP_JWKS_URI;
import static fish.payara.security.annotations.OpenIdProviderMetadata.OPENID_MP_TOKEN_ENDPOINT;
import static fish.payara.security.annotations.OpenIdProviderMetadata.OPENID_MP_USERINFO_ENDPOINT;
import static fish.payara.security.openid.OpenIdUtil.getConfiguredValue;
import fish.payara.security.openid.api.OpenIdConstant;
import static fish.payara.security.openid.api.OpenIdConstant.AUTHORIZATION_CODE_FLOW_TYPES;
import static fish.payara.security.openid.api.OpenIdConstant.AUTHORIZATION_ENDPOINT;
import static fish.payara.security.openid.api.OpenIdConstant.END_SESSION_ENDPOINT;
import static fish.payara.security.openid.api.OpenIdConstant.HYBRID_FLOW_TYPES;
import static fish.payara.security.openid.api.OpenIdConstant.IMPLICIT_FLOW_TYPES;
import static fish.payara.security.openid.api.OpenIdConstant.JWKS_URI;
import static fish.payara.security.openid.api.OpenIdConstant.OPENID_SCOPE;
import static fish.payara.security.openid.api.OpenIdConstant.TOKEN_ENDPOINT;
import static fish.payara.security.openid.api.OpenIdConstant.USERINFO_ENDPOINT;
import fish.payara.security.openid.api.PromptType;
import fish.payara.security.openid.domain.ClaimsConfiguration;
import fish.payara.security.openid.domain.LogoutConfiguration;
import fish.payara.security.openid.domain.OpenIdConfiguration;
import fish.payara.security.openid.domain.OpenIdProviderMetadata;
import fish.payara.security.openid.domain.OpenIdTokenEncryptionMetadata;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static java.util.stream.Collectors.joining;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonObject;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import static org.glassfish.common.util.StringHelper.isEmpty;

/**
 * Build and validate the OpenId Connect client configuration
 *
 * @author Gaurav Gupta
 */
@ApplicationScoped
public class ConfigurationController {

    @Inject
    private ProviderMetadataContoller configurationContoller;

    private static final String SPACE_SEPARATOR = " ";

    /**
     * Creates the {@link OpenIdConfiguration} using the properties as defined
     * in an {@link OpenIdAuthenticationDefinition} annotation or using MP
     * Config source. MP Config source value take precedence over
     * {@link OpenIdAuthenticationDefinition} annotation value.
     *
     * @param definition
     * @return
     */
    public OpenIdConfiguration buildConfig(OpenIdAuthenticationDefinition definition) {
        Config provider = ConfigProvider.getConfig();

        String providerURI;
        JsonObject providerDocument;
        String authorizationEndpoint;
        String tokenEndpoint;
        String userinfoEndpoint;
        String endSessionEndpoint;
        String jwksURI;
        URL jwksURL;

        providerURI = getConfiguredValue(String.class, definition.providerURI(), provider, OPENID_MP_PROVIDER_URI);
        fish.payara.security.annotations.OpenIdProviderMetadata providerMetadata = definition.providerMetadata();
        providerDocument = configurationContoller.getDocument(providerURI);

        if (isEmpty(providerMetadata.authorizationEndpoint()) && providerDocument.containsKey(AUTHORIZATION_ENDPOINT)) {
            authorizationEndpoint = getConfiguredValue(String.class, providerDocument.getString(AUTHORIZATION_ENDPOINT), provider, OPENID_MP_AUTHORIZATION_ENDPOINT);
        } else {
            authorizationEndpoint = getConfiguredValue(String.class, providerMetadata.authorizationEndpoint(), provider, OPENID_MP_AUTHORIZATION_ENDPOINT);
        }
        if (isEmpty(providerMetadata.tokenEndpoint()) && providerDocument.containsKey(TOKEN_ENDPOINT)) {
            tokenEndpoint = getConfiguredValue(String.class, providerDocument.getString(TOKEN_ENDPOINT), provider, OPENID_MP_TOKEN_ENDPOINT);
        } else {
            tokenEndpoint = getConfiguredValue(String.class, providerMetadata.tokenEndpoint(), provider, OPENID_MP_TOKEN_ENDPOINT);
        }
        if (isEmpty(providerMetadata.userinfoEndpoint()) && providerDocument.containsKey(USERINFO_ENDPOINT)) {
            userinfoEndpoint = getConfiguredValue(String.class, providerDocument.getString(USERINFO_ENDPOINT), provider, OPENID_MP_USERINFO_ENDPOINT);
        } else {
            userinfoEndpoint = getConfiguredValue(String.class, providerMetadata.userinfoEndpoint(), provider, OPENID_MP_USERINFO_ENDPOINT);
        }
        if (isEmpty(providerMetadata.endSessionEndpoint()) && providerDocument.containsKey(END_SESSION_ENDPOINT)) {
            endSessionEndpoint = getConfiguredValue(String.class, providerDocument.getString(END_SESSION_ENDPOINT), provider, OPENID_MP_END_SESSION_ENDPOINT);
        } else {
            endSessionEndpoint = getConfiguredValue(String.class, providerMetadata.endSessionEndpoint(), provider, OPENID_MP_END_SESSION_ENDPOINT);
        }
        if (isEmpty(providerMetadata.jwksURI()) && providerDocument.containsKey(JWKS_URI)) {
            jwksURI = getConfiguredValue(String.class, providerDocument.getString(JWKS_URI), provider, OPENID_MP_JWKS_URI);
        } else {
            jwksURI = getConfiguredValue(String.class, providerMetadata.jwksURI(), provider, OPENID_MP_JWKS_URI);
        }
        try {
            jwksURL = new URL(jwksURI);
        } catch (MalformedURLException ex) {
            throw new IllegalStateException("jwksURI is invalid", ex);
        }
        String clientId = getConfiguredValue(String.class, definition.clientId(), provider, OPENID_MP_CLIENT_ID);
        char[] clientSecret = getConfiguredValue(String.class, definition.clientSecret(), provider, OPENID_MP_CLIENT_SECRET).toCharArray();
        String redirectURI = getConfiguredValue(String.class, definition.redirectURI(), provider, OPENID_MP_REDIRECT_URI);

        String scopes = Arrays.stream(definition.scope()).collect(joining(SPACE_SEPARATOR));
        scopes = getConfiguredValue(String.class, scopes, provider, OPENID_MP_SCOPE);
        if (isEmpty(scopes)) {
            scopes = OPENID_SCOPE;
        } else if (!scopes.contains(OPENID_SCOPE)) {
            scopes = OPENID_SCOPE + SPACE_SEPARATOR + scopes;
        }

        String responseType = getConfiguredValue(String.class, definition.responseType(), provider, OPENID_MP_RESPONSE_TYPE);
        responseType
                = Arrays.stream(responseType.trim().split(SPACE_SEPARATOR))
                        .map(String::toLowerCase)
                        .sorted()
                        .collect(joining(SPACE_SEPARATOR));

        String responseMode = getConfiguredValue(String.class, definition.responseMode(), provider, OPENID_MP_RESPONSE_MODE);

        String display = definition.display().toString().toLowerCase();
        display = getConfiguredValue(String.class, display, provider, OPENID_MP_DISPLAY);

        String prompt = Arrays.stream(definition.prompt())
                .map(PromptType::toString)
                .map(String::toLowerCase)
                .collect(joining(SPACE_SEPARATOR));
        prompt = getConfiguredValue(String.class, prompt, provider, OPENID_MP_PROMPT);

        Map<String, String> extraParameters = new HashMap<>();
        for (String extraParameter : definition.extraParameters()) {
            String[] parts = extraParameter.split("=");
            String key = parts[0];
            String value = parts[1];
            extraParameters.put(key, value);
        }

        // check if resource extra parameter is set
        String resource = "";
        if (extraParameters.containsKey(OpenIdConstant.RESOURCE)) {
            // get resource parameter from extra parameter
            resource = extraParameters.get(OpenIdConstant.RESOURCE);
            
            // remove resource from extra parameter
            extraParameters.remove(OpenIdConstant.RESOURCE);
        }
        
        boolean nonce = getConfiguredValue(Boolean.class, definition.useNonce(), provider, OPENID_MP_USE_NONCE);
        boolean session = getConfiguredValue(Boolean.class, definition.useSession(), provider, OPENID_MP_USE_SESSION);

        int jwksConnectTimeout = getConfiguredValue(Integer.class, definition.jwksConnectTimeout(), provider, OPENID_MP_JWKS_CONNECT_TIMEOUT);
        int jwksReadTimeout = getConfiguredValue(Integer.class, definition.jwksReadTimeout(), provider, OPENID_MP_JWKS_READ_TIMEOUT);

        String encryptionAlgorithm = provider.getOptionalValue(OPENID_MP_CLIENT_ENC_ALGORITHM, String.class).orElse(null);
        String encryptionMethod = provider.getOptionalValue(OPENID_MP_CLIENT_ENC_METHOD, String.class).orElse(null);
        String privateKeyJWKS = provider.getOptionalValue(OPENID_MP_CLIENT_ENC_JWKS, String.class).orElse(null);

        String callerNameClaim = getConfiguredValue(String.class, definition.claimsDefinition().callerNameClaim(), provider, OPENID_MP_CALLER_NAME_CLAIM);
        String callerGroupsClaim = getConfiguredValue(String.class, definition.claimsDefinition().callerGroupsClaim(), provider, OPENID_MP_CALLER_GROUP_CLAIM);

        Boolean notifyProvider = getConfiguredValue(Boolean.class, definition.logout().notifyProvider(), provider, OPENID_MP_PROVIDER_NOTIFY_LOGOUT);
        String logoutRedirectURI = getConfiguredValue(String.class, definition.logout().redirectURI(), provider, OPENID_MP_POST_LOGOUT_REDIRECT_URI);
        Boolean accessTokenExpiry = getConfiguredValue(Boolean.class, definition.logout().accessTokenExpiry(), provider, OPENID_MP_LOGOUT_ON_ACCESS_TOKEN_EXPIRY);
        Boolean identityTokenExpiry = getConfiguredValue(Boolean.class, definition.logout().identityTokenExpiry(), provider, OPENID_MP_LOGOUT_ON_IDENTITY_TOKEN_EXPIRY);

        boolean tokenAutoRefresh = getConfiguredValue(Boolean.class, definition.tokenAutoRefresh(), provider, OPENID_MP_TOKEN_AUTO_REFRESH);
        int tokenMinValidity = getConfiguredValue(Integer.class, definition.tokenMinValidity(), provider, OPENID_MP_TOKEN_MIN_VALIDITY);

        OpenIdConfiguration configuration = new OpenIdConfiguration()
                .setProviderMetadata(
                        new OpenIdProviderMetadata(providerDocument)
                                .setAuthorizationEndpoint(authorizationEndpoint)
                                .setTokenEndpoint(tokenEndpoint)
                                .setUserinfoEndpoint(userinfoEndpoint)
                                .setEndSessionEndpoint(endSessionEndpoint)
                                .setJwksURL(jwksURL)
                )
                .setClaimsConfiguration(
                        new ClaimsConfiguration()
                                .setCallerNameClaim(callerNameClaim)
                                .setCallerGroupsClaim(callerGroupsClaim)
                ).setLogoutConfiguration(
                        new LogoutConfiguration()
                                .setNotifyProvider(notifyProvider)
                                .setRedirectURI(logoutRedirectURI)
                                .setAccessTokenExpiry(accessTokenExpiry)
                                .setIdentityTokenExpiry(identityTokenExpiry)
                )
                .setEncryptionMetadata(
                        new OpenIdTokenEncryptionMetadata()
                                .setEncryptionAlgorithm(encryptionAlgorithm)
                                .setEncryptionMethod(encryptionMethod)
                                .setPrivateKeySource(privateKeyJWKS)
                )
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .setRedirectURI(redirectURI)
                .setScopes(scopes)
                .setResponseType(responseType)
                .setResponseMode(responseMode)
                .setExtraParameters(extraParameters)
                .setPrompt(prompt)
                .setDisplay(display)
                .setUseNonce(nonce)
                .setUseSession(session)
                .setJwksConnectTimeout(jwksConnectTimeout)
                .setJwksReadTimeout(jwksReadTimeout)
                .setTokenAutoRefresh(tokenAutoRefresh)
                .setTokenMinValidity(tokenMinValidity);
        
        // if resource isn't empty we have adfs mode and add resource to configuration
        if (! resource.isEmpty()) {
            configuration.setResource(resource);
        } else {
            // if empty set blank string to indicate we have standard openid implementation
            configuration.setResource("");
        }
        
        validateConfiguration(configuration);

        return configuration;
    }

    /**
     * Validate the properties of the OpenId Connect Client and Provider
     * Metadata
     */
    private void validateConfiguration(OpenIdConfiguration configuration) {
        List<String> errorMessages = new ArrayList<>();
        errorMessages.addAll(validateProviderMetadata(configuration));
        errorMessages.addAll(validateClientConfiguration(configuration));

        if (!errorMessages.isEmpty()) {
            throw new IllegalStateException(errorMessages.toString());
        }
    }

    private List<String> validateProviderMetadata(OpenIdConfiguration configuration) {
        List<String> errorMessages = new ArrayList<>();

        if (isEmpty(configuration.getProviderMetadata().getIssuerURI())) {
            errorMessages.add("issuer metadata is mandatory");
        }
        if (isEmpty(configuration.getProviderMetadata().getAuthorizationEndpoint())) {
            errorMessages.add("authorization_endpoint metadata is mandatory");
        }
        if (isEmpty(configuration.getProviderMetadata().getTokenEndpoint())) {
            errorMessages.add("token_endpoint metadata is mandatory");
        }
        if (configuration.getProviderMetadata().getJwksURL() == null) {
            errorMessages.add("jwks_uri metadata is mandatory");
        }
        if (configuration.getProviderMetadata().getResponseTypeSupported().isEmpty()) {
            errorMessages.add("response_types_supported metadata is mandatory");
        }
        if (configuration.getProviderMetadata().getResponseTypeSupported().isEmpty()) {
            errorMessages.add("subject_types_supported metadata is mandatory");
        }
        if (configuration.getProviderMetadata().getIdTokenSigningAlgorithmsSupported().isEmpty()) {
            errorMessages.add("id_token_signing_alg_values_supported metadata is mandatory");
        }
        return errorMessages;
    }

    private List<String> validateClientConfiguration(OpenIdConfiguration configuration) {
        List<String> errorMessages = new ArrayList<>();

        if (isEmpty(configuration.getClientId())) {
            errorMessages.add("client_id request parameter is mandatory");
        }
        if (isEmpty(configuration.getRedirectURI())) {
            errorMessages.add("redirect_uri request parameter is mandatory");
        }
        if (configuration.getJwksConnectTimeout() <= 0) {
            errorMessages.add("jwksConnectTimeout value is not valid");
        }
        if (configuration.getJwksReadTimeout() <= 0) {
            errorMessages.add("jwksReadTimeout value is not valid");
        }

        if (isEmpty(configuration.getResponseType())) {
            errorMessages.add("The response type must contain at least one value");
        } else if (!configuration.getProviderMetadata().getResponseTypeSupported().contains(configuration.getResponseType())
                && !AUTHORIZATION_CODE_FLOW_TYPES.contains(configuration.getResponseType())
                && !IMPLICIT_FLOW_TYPES.contains(configuration.getResponseType())
                && !HYBRID_FLOW_TYPES.contains(configuration.getResponseType())) {
            errorMessages.add("Unsupported OpenID Connect response type value : " + configuration.getResponseType());
        }

        Set<String> supportedScopes = configuration.getProviderMetadata().getScopesSupported();
        if (!supportedScopes.isEmpty()) {
            for (String scope : configuration.getScopes().split(SPACE_SEPARATOR)) {
                if (!supportedScopes.contains(scope)) {
                    errorMessages.add(String.format(
                            "%s scope is not supported by %s OpenId Connect provider",
                            scope,
                            configuration.getProviderMetadata().getIssuerURI())
                    );
                }
            }
        }

        return errorMessages;
    }

}

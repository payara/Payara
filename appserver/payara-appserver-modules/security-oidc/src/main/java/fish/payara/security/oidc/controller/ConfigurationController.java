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
package fish.payara.security.oidc.controller;

import fish.payara.security.oidc.domain.OidcConfiguration;
import static fish.payara.security.annotations.OidcAuthenticationDefinition.OIDC_MP_CLIENT_ID;
import static fish.payara.security.annotations.OidcAuthenticationDefinition.OIDC_MP_CLIENT_SECRET;
import static fish.payara.security.annotations.OidcAuthenticationDefinition.OIDC_MP_DISPLAY;
import static fish.payara.security.annotations.OidcAuthenticationDefinition.OIDC_MP_NONCE;
import static fish.payara.security.annotations.OidcAuthenticationDefinition.OIDC_MP_PROMPT;
import static fish.payara.security.annotations.OidcAuthenticationDefinition.OIDC_MP_REDIRECT_URI;
import static fish.payara.security.annotations.OidcAuthenticationDefinition.OIDC_MP_RESPONSE_MODE;
import static fish.payara.security.annotations.OidcAuthenticationDefinition.OIDC_MP_RESPONSE_TYPE;
import static fish.payara.security.annotations.OidcAuthenticationDefinition.OIDC_MP_SCOPE;
import static fish.payara.security.annotations.OidcProviderMetadata.OIDC_MP_AUTHORIZATION_ENDPOINT;
import static fish.payara.security.annotations.OidcProviderMetadata.OIDC_MP_JWKS_URI;
import static fish.payara.security.annotations.OidcProviderMetadata.OIDC_MP_TOKEN_ENDPOINT;
import static fish.payara.security.annotations.OidcProviderMetadata.OIDC_MP_USERINFO_ENDPOINT;
import static fish.payara.security.oidc.OIDCUtil.getConfiguredValue;
import static fish.payara.security.oidc.api.OidcConstant.AUTHORIZATION_ENDPOINT;
import static fish.payara.security.oidc.api.OidcConstant.JWKS_URI;
import static fish.payara.security.oidc.api.OidcConstant.OPENID_SCOPE;
import static fish.payara.security.oidc.api.OidcConstant.TOKEN_ENDPOINT;
import static fish.payara.security.oidc.api.OidcConstant.USERINFO_ENDPOINT;
import fish.payara.security.oidc.api.PromptType;
import fish.payara.security.oidc.domain.OidcProviderMetadata;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import static java.util.stream.Collectors.joining;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonObject;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import fish.payara.security.annotations.OidcAuthenticationDefinition;
import static fish.payara.security.annotations.OidcAuthenticationDefinition.OIDC_MP_CLIENT_ENC_ALGORITHM;
import static fish.payara.security.annotations.OidcAuthenticationDefinition.OIDC_MP_CLIENT_ENC_JWKS;
import static fish.payara.security.annotations.OidcAuthenticationDefinition.OIDC_MP_CLIENT_ENC_METHOD;
import static fish.payara.security.annotations.OidcAuthenticationDefinition.OIDC_MP_PROVIDER_URI;
import fish.payara.security.oidc.domain.OidcTokenEncryptionMetadata;
import static org.glassfish.common.util.StringHelper.isEmpty;
import static fish.payara.security.oidc.api.OidcConstant.AUTHORIZATION_CODE_FLOW_TYPES;
import static fish.payara.security.oidc.api.OidcConstant.IMPLICIT_FLOW_TYPES;
import static fish.payara.security.oidc.api.OidcConstant.HYBRID_FLOW_TYPES;
import java.util.HashMap;
import java.util.Map;

/**
 * Build and validate the OIDC client configuration
 *
 * @author Gaurav Gupta
 */
@ApplicationScoped
public class ConfigurationController {

    @Inject
    private ProviderMetadataContoller configurationContoller;

    private static final String SPACE_SEPARATOR = " ";

    /**
     * Creates the OidcConfiguration using the properties as defined in an
     * {@link OidcAuthenticationDefinition} annotation or using MP Config
     * source. MP Config source value take precedence over
     * {@link OidcAuthenticationDefinition} annotation value.
     *
     * @param definition
     * @return
     */
    public OidcConfiguration buildConfig(OidcAuthenticationDefinition definition) {
        Config provider = ConfigProvider.getConfig();

        String providerUri;
        JsonObject providerDocument;
        String authorizationEndpoint;
        String tokenEndpoint;
        String userinfoEndpoint;
        String jwksUri;

        providerUri = getConfiguredValue(String.class, definition.providerUri(), provider, OIDC_MP_PROVIDER_URI);
        fish.payara.security.annotations.OidcProviderMetadata providerMetadata = definition.providerMetadata();
        providerDocument = configurationContoller.getDocument(providerUri);

        if (isEmpty(providerMetadata.authorizationEndpoint()) && providerDocument.containsKey(AUTHORIZATION_ENDPOINT)) {
            authorizationEndpoint = getConfiguredValue(String.class, providerDocument.getString(AUTHORIZATION_ENDPOINT), provider, OIDC_MP_AUTHORIZATION_ENDPOINT);
        } else {
            authorizationEndpoint = getConfiguredValue(String.class, providerMetadata.authorizationEndpoint(), provider, OIDC_MP_AUTHORIZATION_ENDPOINT);
        }
        if (isEmpty(providerMetadata.tokenEndpoint()) && providerDocument.containsKey(TOKEN_ENDPOINT)) {
            tokenEndpoint = getConfiguredValue(String.class, providerDocument.getString(TOKEN_ENDPOINT), provider, OIDC_MP_TOKEN_ENDPOINT);
        } else {
            tokenEndpoint = getConfiguredValue(String.class, providerMetadata.tokenEndpoint(), provider, OIDC_MP_TOKEN_ENDPOINT);
        }
        if (isEmpty(providerMetadata.userinfoEndpoint()) && providerDocument.containsKey(USERINFO_ENDPOINT)) {
            userinfoEndpoint = getConfiguredValue(String.class, providerDocument.getString(USERINFO_ENDPOINT), provider, OIDC_MP_USERINFO_ENDPOINT);
        } else {
            userinfoEndpoint = getConfiguredValue(String.class, providerMetadata.userinfoEndpoint(), provider, OIDC_MP_USERINFO_ENDPOINT);
        }
        if (isEmpty(providerMetadata.jwksUri()) && providerDocument.containsKey(JWKS_URI)) {
            jwksUri = getConfiguredValue(String.class, providerDocument.getString(JWKS_URI), provider, OIDC_MP_JWKS_URI);
        } else {
            jwksUri = getConfiguredValue(String.class, providerMetadata.jwksUri(), provider, OIDC_MP_JWKS_URI);
        }

        String clientID = getConfiguredValue(String.class, definition.clientId(), provider, OIDC_MP_CLIENT_ID);
        char[] clientSecret = getConfiguredValue(String.class, definition.clientSecret(), provider, OIDC_MP_CLIENT_SECRET).toCharArray();
        String redirectURI = getConfiguredValue(String.class, definition.redirectURI(), provider, OIDC_MP_REDIRECT_URI);

        String scopes = Arrays.stream(definition.scope()).collect(joining(SPACE_SEPARATOR));
        scopes = getConfiguredValue(String.class, scopes, provider, OIDC_MP_SCOPE);
        if (isEmpty(scopes)) {
            scopes = OPENID_SCOPE;
        } else if (!scopes.contains(OPENID_SCOPE)) {
            scopes = OPENID_SCOPE + SPACE_SEPARATOR + scopes;
        }

        String responseType = getConfiguredValue(String.class, definition.responseType(), provider, OIDC_MP_RESPONSE_TYPE);
        responseType
                = Arrays.stream(responseType.trim().split(SPACE_SEPARATOR))
                        .map(String::toLowerCase)
                        .sorted()
                        .collect(joining(SPACE_SEPARATOR));

        String responseMode = getConfiguredValue(String.class, definition.responseMode(), provider, OIDC_MP_RESPONSE_MODE);

        String display = definition.display().toString().toLowerCase();
        display = getConfiguredValue(String.class, display, provider, OIDC_MP_DISPLAY);

        String prompt = Arrays.stream(definition.prompt())
                .map(PromptType::toString)
                .map(String::toLowerCase)
                .collect(joining(SPACE_SEPARATOR));
        prompt = getConfiguredValue(String.class, prompt, provider, OIDC_MP_PROMPT);

        Map<String, String> extraParameters = new HashMap<>();
        for (String extraParameter : definition.extraParameters()) {
            String[] parts = extraParameter.split("=");
            String key = parts[0];
            String value = parts[1];
            extraParameters.put(key, value);
        }

        boolean nonce = getConfiguredValue(Boolean.class, definition.useNonce(), provider, OIDC_MP_NONCE);

        String encryptionAlgorithm = provider.getOptionalValue(OIDC_MP_CLIENT_ENC_ALGORITHM, String.class).orElse(null);
        String encryptionMethod = provider.getOptionalValue(OIDC_MP_CLIENT_ENC_METHOD, String.class).orElse(null);
        String privateKeyJWKS = provider.getOptionalValue(OIDC_MP_CLIENT_ENC_JWKS, String.class).orElse(null);

        OidcConfiguration configuration = new OidcConfiguration()
                .setProviderMetadata(
                        new OidcProviderMetadata(providerDocument)
                                .setAuthorizationEndpoint(authorizationEndpoint)
                                .setTokenEndpoint(tokenEndpoint)
                                .setUserinfoEndpoint(userinfoEndpoint)
                                .setJwksUri(jwksUri)
                )
                .setEncryptionMetadata(
                        new OidcTokenEncryptionMetadata()
                                .setEncryptionAlgorithm(encryptionAlgorithm)
                                .setEncryptionMethod(encryptionMethod)
                                .setPrivateKeySource(privateKeyJWKS)
                )
                .setClientID(clientID)
                .setClientSecret(clientSecret)
                .setRedirectURI(redirectURI)
                .setScopes(scopes)
                .setResponseType(responseType)
                .setResponseMode(responseMode)
                .setExtraParameters(extraParameters)
                .setPrompt(prompt)
                .setDisplay(display)
                .setUseNonce(nonce);

        validateConfiguration(configuration);

        return configuration;
    }

    /**
     * Validate the properties of the OIDC Client and Provider Metadata
     */
    private void validateConfiguration(OidcConfiguration configuration) {
        List<String> errorMessages = new ArrayList<>();

        //validate provider metadata
        if (isEmpty(configuration.getProviderMetadata().getIssuerUri())) {
            errorMessages.add("issuer metadata is mandatory");
        }
        if (isEmpty(configuration.getProviderMetadata().getAuthorizationEndpoint())) {
            errorMessages.add("authorization_endpoint metadata is mandatory");
        }
        if (isEmpty(configuration.getProviderMetadata().getTokenEndpoint())) {
            errorMessages.add("token_endpoint metadata is mandatory");
        }
        if (isEmpty(configuration.getProviderMetadata().getJwksUri())) {
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

        //validate client configuration
        if (isEmpty(configuration.getClientID())) {
            errorMessages.add("client_id request parameter is mandatory");
        }
        if (isEmpty(configuration.getRedirectURI())) {
            errorMessages.add("redirect_uri request parameter is mandatory");
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
                            "%s scope is not supported by %s OIDC Provider",
                            scope,
                            configuration.getProviderMetadata().getIssuerUri())
                    );
                }
            }
        }

        if (!errorMessages.isEmpty()) {
            throw new IllegalStateException(errorMessages.toString());
        }
    }

}

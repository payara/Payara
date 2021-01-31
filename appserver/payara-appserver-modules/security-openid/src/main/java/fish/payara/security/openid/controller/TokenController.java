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

import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import static com.nimbusds.jose.jwk.source.RemoteJWKSet.DEFAULT_HTTP_SIZE_LIMIT;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWEDecryptionKeySelector;
import com.nimbusds.jose.proc.JWEKeySelector;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import com.nimbusds.jose.util.ResourceRetriever;
import com.nimbusds.jwt.EncryptedJWT;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import com.nimbusds.jwt.proc.JWTClaimsSetVerifier;
import static fish.payara.security.openid.OpenIdUtil.DEFAULT_JWT_SIGNED_ALGORITHM;
import fish.payara.security.openid.api.IdentityToken;
import static fish.payara.security.openid.api.OpenIdConstant.AUTHORIZATION_CODE;
import static fish.payara.security.openid.api.OpenIdConstant.CLIENT_ID;
import static fish.payara.security.openid.api.OpenIdConstant.CLIENT_SECRET;
import static fish.payara.security.openid.api.OpenIdConstant.CODE;
import static fish.payara.security.openid.api.OpenIdConstant.GRANT_TYPE;
import static fish.payara.security.openid.api.OpenIdConstant.REDIRECT_URI;
import static fish.payara.security.openid.api.OpenIdConstant.REFRESH_TOKEN;
import static fish.payara.security.openid.api.OpenIdConstant.RESOURCE;
import fish.payara.security.openid.api.RefreshToken;
import fish.payara.security.openid.domain.AccessTokenImpl;
import fish.payara.security.openid.domain.IdentityTokenImpl;
import fish.payara.security.openid.domain.OpenIdConfiguration;
import fish.payara.security.openid.domain.OpenIdNonce;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.text.ParseException;
import static java.util.Collections.emptyMap;
import java.util.Map;
import static java.util.Objects.isNull;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import javax.ws.rs.core.Response;

/**
 * Controller for Token endpoint
 *
 * @author Gaurav Gupta
 */
@ApplicationScoped
public class TokenController {

    @Inject
    private NonceController nonceController;

    /**
     * (4) A Client makes a token request to the token endpoint and the OpenId
     * Provider responds with an ID Token and an Access Token.
     *
     * @param configuration
     * @param request
     * @return a JSON object representation of OpenID Connect token response
     * from the Token endpoint.
     */
    public Response getTokens(OpenIdConfiguration configuration, HttpServletRequest request) {
        /**
         * one-time authorization code that RP exchange for an Access / Id token
         */
        String authorizationCode = request.getParameter(CODE);

        /**
         * The Client sends the parameters to the Token Endpoint using the Form
         * Serialization with all parameters to :
         *
         * 1. Authenticate client using CLIENT_ID & CLIENT_SECRET <br>
         * 2. Verify that the Authorization Code is valid <br>
         * 3. Ensure that the redirect_uri parameter value is identical to the
         * initial authorization request's redirect_uri parameter value.
         */
        Form form = new Form()
                .param(CLIENT_ID, configuration.getClientId())
                .param(CLIENT_SECRET, new String(configuration.getClientSecret()))
                .param(GRANT_TYPE, AUTHORIZATION_CODE)
                .param(CODE, authorizationCode)
                .param(REDIRECT_URI, configuration.buildRedirectURI(request));
        
        // check if resource parameter is set
        if (! configuration.getResource().isEmpty()) {
            // if resource is set add this to the token request
            form.param(RESOURCE, configuration.getResource());
        }

        //  ID Token and Access Token Request
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(configuration.getProviderMetadata().getTokenEndpoint());
        return target.request()
                .accept(APPLICATION_JSON)
                .post(Entity.form(form));
    }

    /**
     * (5.1) Validate Id Token's claims and verify ID Token's signature.
     *
     * @param idToken
     * @param httpContext
     * @param configuration
     * @return JWT Claims
     */
    public Map<String, Object> validateIdToken(IdentityTokenImpl idToken, HttpMessageContext httpContext, OpenIdConfiguration configuration) {
        JWTClaimsSet claimsSet;
        HttpServletRequest request = httpContext.getRequest();
        HttpServletResponse response = httpContext.getResponse();

        /**
         * The nonce in the returned ID Token is compared to the hash of the
         * session cookie to detect ID Token replay by third parties.
         */
        String expectedNonceHash = null;
        if (configuration.isUseNonce()) {
            OpenIdNonce expectedNonce = nonceController.get(configuration, request, response);
            expectedNonceHash = nonceController.getNonceHash(expectedNonce);
        }

        try {
            JWTClaimsSetVerifier jwtVerifier = new IdTokenClaimsSetVerifier(expectedNonceHash, configuration);
            claimsSet = validateBearerToken(idToken.getTokenJWT(), jwtVerifier, configuration);
        } finally {
            nonceController.remove(configuration, request, response);
        }

        return claimsSet.getClaims();
    }

    /**
     * Validate Id Token received from Successful Refresh Response.
     *
     * @param previousIdToken
     * @param newIdToken
     * @param httpContext
     * @param configuration
     * @return JWT Claims
     */
    public Map<String, Object> validateRefreshedIdToken(IdentityToken previousIdToken, IdentityTokenImpl newIdToken, HttpMessageContext httpContext, OpenIdConfiguration configuration) {
        JWTClaimsSetVerifier jwtVerifier = new RefreshedIdTokenClaimsSetVerifier(previousIdToken, configuration);
        JWTClaimsSet claimsSet = validateBearerToken(newIdToken.getTokenJWT(), jwtVerifier, configuration);
        return claimsSet.getClaims();
    }

    /**
     * (5.2) Validate the Access Token & it's claims and verify the signature.
     *
     * @param accessToken
     * @param idTokenAlgorithm
     * @param idTokenClaims
     * @param configuration
     * @return JWT Claims
     */
    public Map<String, Object> validateAccessToken(AccessTokenImpl accessToken, Algorithm idTokenAlgorithm, Map<String, Object> idTokenClaims, OpenIdConfiguration configuration) {
        Map<String, Object> claims = emptyMap();

        AccessTokenClaimsSetVerifier jwtVerifier = new AccessTokenClaimsSetVerifier(
                accessToken,
                idTokenAlgorithm,
                idTokenClaims,
                configuration
        );

        // https://support.okta.com/help/s/article/Signature-Validation-Failed-on-Access-Token
//        if (accessToken.getType() == AccessToken.Type.BEARER) {
//            JWTClaimsSet claimsSet = validateBearerToken(accessToken.getTokenJWT(), jwtVerifier, configuration);
//            claims = claimsSet.getClaims();
//        } else {
            jwtVerifier.validateAccessToken();
//        }

        return claims;
    }

    /**
     * Makes a refresh request to the token endpoint and the OpenId Provider
     * responds with a new (updated) Access Token and Refreshs Token.
     *
     * @param configuration
     * @param refreshToken Refresh Token received from previous token request.
     * @return a JSON object representation of OpenID Connect token response
     * from the Token endpoint.
     */
    public Response refreshTokens(OpenIdConfiguration configuration, RefreshToken refreshToken) {

        Form form = new Form()
                .param(CLIENT_ID, configuration.getClientId())
                .param(CLIENT_SECRET, new String(configuration.getClientSecret()))
                .param(GRANT_TYPE, REFRESH_TOKEN)
                .param(REFRESH_TOKEN, refreshToken.getToken());

        // Access Token and RefreshToken Request
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(configuration.getProviderMetadata().getTokenEndpoint());
        return target.request()
                .accept(APPLICATION_JSON)
                .post(Entity.form(form));
    }

    private JWTClaimsSet validateBearerToken(JWT token, JWTClaimsSetVerifier jwtVerifier, OpenIdConfiguration configuration) {
        JWTClaimsSet claimsSet;
        try {
            if (token instanceof PlainJWT) {
                PlainJWT plainToken = (PlainJWT) token;
                claimsSet = plainToken.getJWTClaimsSet();
                jwtVerifier.verify(claimsSet, null);
            } else if (token instanceof SignedJWT) {
                SignedJWT signedToken = (SignedJWT) token;
                JWSHeader header = signedToken.getHeader();
                String alg = header.getAlgorithm().getName();
                if (isNull(alg)) {
                    // set the default value
                    alg = DEFAULT_JWT_SIGNED_ALGORITHM;
                }

                ConfigurableJWTProcessor jwtProcessor = new DefaultJWTProcessor();
                jwtProcessor.setJWSKeySelector(getJWSKeySelector(configuration, alg));
                jwtProcessor.setJWTClaimsSetVerifier(jwtVerifier);
                claimsSet = jwtProcessor.process(signedToken, null);
            } else if (token instanceof EncryptedJWT) {
                /**
                 * If ID Token is encrypted, decrypt it using the keys and
                 * algorithms
                 */
                EncryptedJWT encryptedToken = (EncryptedJWT) token;
                JWEHeader header = encryptedToken.getHeader();
                String alg = header.getAlgorithm().getName();

                ConfigurableJWTProcessor jwtProcessor = new DefaultJWTProcessor();
                jwtProcessor.setJWSKeySelector(getJWSKeySelector(configuration, alg));
                jwtProcessor.setJWEKeySelector(getJWEKeySelector(configuration));
                jwtProcessor.setJWTClaimsSetVerifier(jwtVerifier);
                claimsSet = jwtProcessor.process(encryptedToken, null);
            } else {
                throw new IllegalStateException("Unexpected JWT type : " + token.getClass());
            }
        } catch (ParseException | BadJOSEException | JOSEException ex) {
            throw new IllegalStateException(ex);
        }
        return claimsSet;
    }


    /**
     * JWSKeySelector finds the JSON Web Key Set (JWKS) from jwks_uri endpoint
     * and filter for potential signing keys in the JWKS with a matching kid
     * property.
     *
     * @param configuration
     * @param alg the algorithm for the key
     * @param kid the unique identifier for the key
     * @return the JSON Web Signing (JWS) key selector
     */
    private JWSKeySelector getJWSKeySelector(OpenIdConfiguration configuration, String alg) {
        JWKSource jwkSource;
        JWSAlgorithm jWSAlgorithm = new JWSAlgorithm(alg);
        if (Algorithm.NONE.equals(jWSAlgorithm)) {
            throw new IllegalStateException("Unsupported JWS algorithm : " + jWSAlgorithm);
        } else if (JWSAlgorithm.Family.RSA.contains(jWSAlgorithm)
                || JWSAlgorithm.Family.EC.contains(jWSAlgorithm)) {
            ResourceRetriever jwkSetRetriever = new DefaultResourceRetriever(
                    configuration.getJwksConnectTimeout(),
                    configuration.getJwksReadTimeout(),
                    DEFAULT_HTTP_SIZE_LIMIT
            );
            jwkSource = new RemoteJWKSet(configuration.getProviderMetadata().getJwksURL(), jwkSetRetriever);
        } else if (JWSAlgorithm.Family.HMAC_SHA.contains(jWSAlgorithm)) {
            byte[] clientSecret = new String(configuration.getClientSecret()).getBytes(UTF_8);
            if (isNull(clientSecret)) {
                throw new IllegalStateException("Missing client secret");
            }
            jwkSource = new ImmutableSecret(clientSecret);
        } else {
            throw new IllegalStateException("Unsupported JWS algorithm : " + jWSAlgorithm);
        }
        return new JWSVerificationKeySelector(jWSAlgorithm, jwkSource);
    }

    /**
     * JWEKeySelector selects the key to decrypt JSON Web Encryption (JWE) and
     * validate encrypted JWT.
     *
     * @param configuration
     * @return the JSON Web Encryption (JWE) key selector
     */
    private JWEKeySelector getJWEKeySelector(OpenIdConfiguration configuration) {
        JWEKeySelector jweKeySelector;

        JWEAlgorithm jwsAlg = configuration.getEncryptionMetadata().getEncryptionAlgorithm();
        EncryptionMethod jweEnc = configuration.getEncryptionMetadata().getEncryptionMethod();
        JWKSource jwkSource = configuration.getEncryptionMetadata().getPrivateKeySource();

        if (isNull(jwsAlg)) {
            throw new IllegalStateException("Missing JWE encryption algorithm ");
        }
        if (!configuration.getProviderMetadata().getIdTokenEncryptionAlgorithmsSupported().contains(jwsAlg.getName())) {
            throw new IllegalStateException("Unsupported ID tokens algorithm :" + jwsAlg.getName());
        }
        if (isNull(jweEnc)) {
            throw new IllegalStateException("Missing JWE encryption method");
        }
        if (!configuration.getProviderMetadata().getIdTokenEncryptionMethodsSupported().contains(jweEnc.getName())) {
            throw new IllegalStateException("Unsupported ID tokens encryption method :" + jweEnc.getName());
        }

        jweKeySelector = new JWEDecryptionKeySelector(jwsAlg, jweEnc, jwkSource);
        return jweKeySelector;
    }

}

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

import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.BadJWTException;
import com.nimbusds.jwt.proc.JWTClaimsSetVerifier;
import static fish.payara.security.oidc.api.OidcConstant.AUTHORIZED_PARTY;
import static fish.payara.security.oidc.api.OidcConstant.NONCE;
import fish.payara.security.oidc.domain.OidcConfiguration;
import fish.payara.security.oidc.domain.OidcNonce;
import java.util.Date;
import java.util.List;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * Validates the ID token
 *
 * @author Gaurav Gupta
 */
public class IdTokenClaimsSetVerifier implements JWTClaimsSetVerifier {

    private final OidcConfiguration configuration;
    private final OidcNonce expectedNonce;

    public IdTokenClaimsSetVerifier(OidcConfiguration configuration, OidcNonce expectedNonce) {
        this.configuration = configuration;
        this.expectedNonce = expectedNonce;
    }

    /**
     * Validate ID Token's claims
     *
     * @param claims
     * @param context
     * @throws com.nimbusds.jwt.proc.BadJWTException
     */
    @Override
    public void verify(JWTClaimsSet claims, SecurityContext context) throws BadJWTException {

        int clockSkewInSeconds = 60;
        int clockSkewInMillis = clockSkewInSeconds * 1000;

        /**
         * The Issuer Identifier for the OpenID Provider (which is typically
         * obtained during Discovery) must exactly match the value of the iss
         * (issuer) Claim.
         */
        if (isNull(claims.getIssuer())) {
            throw new IllegalStateException("Missing issuer (iss) claim");
        }
//      Azure OIDC failing
//      "https://login.microsoftonline.com/f1195e3a-584e-4eba-867f-030b6d4cffa9/v2.0" == "https://login.microsoftonline.com/{tenantid}/v2.0"
//      if (!claims.getIssuer().equals(configuration.getProviderMetadata().getIssuerUri())) {
//          throw new IllegalStateException("Invalid issuer : " + configuration.getProviderMetadata().getIssuerUri());
//      }

        /**
         * Subject Identifier is locally unique and never reassigned identifier
         * within the Issuer for the End-User.
         */
        if (isNull(claims.getSubject())) {
            throw new IllegalStateException("Missing subject (sub) claim");
        }

        /**
         * Audience(s) claim (that this ID Token is intended for) must contains
         * the client_id of the Client (Relying Party) as an audience value.
         */
        final List<String> audience = claims.getAudience();
        if (isNull(audience) || audience.isEmpty()) {
            throw new IllegalStateException("Missing audience (aud) claim");
        }
        if (!audience.contains(configuration.getClientID())) {
            throw new IllegalStateException("Invalid audience (aud) claim " + audience);
        }

        /**
         * If the ID Token contains multiple audiences, the Client should verify
         * that an azp (authorized party) claim is present.
         */
        Object authorizedParty = claims.getClaim(AUTHORIZED_PARTY);
        if (audience.size() > 1 && isNull(authorizedParty)) {
            throw new IllegalStateException("Missing authorized party (azp) claim");
        }

        /**
         * If an azp (authorized party) claim is present, the Client should
         * verify that its client_id is the claim Value
         */
        if (audience.size() > 1
                && nonNull(authorizedParty)
                && !authorizedParty.equals(configuration.getClientID())) {
            throw new IllegalStateException("Invalid authorized party (azp) claim " + configuration.getClientID());
        }

        /**
         * The current time must be before the time represented by the exp
         * Claim.
         */
        long currentTime = System.currentTimeMillis();
        Date exp = claims.getExpirationTime();
        if (isNull(exp)) {
            throw new IllegalStateException("Missing expiration time (exp) claim");
        }
        if ((exp.getTime() + clockSkewInMillis) < currentTime) {
            throw new IllegalStateException("ID token is expired " + exp);
        }

        /**
         * The current time must be after the time represented by the iat Claim.
         */
        Date iat = claims.getIssueTime();
        if (isNull(iat)) {
            throw new IllegalStateException("Missing issue time (iat) claim");
        }
        if ((iat.getTime() - clockSkewInMillis) > currentTime) {
            throw new IllegalStateException("Issue time must be after current time " + iat);
        }

        /**
         * If a nonce was sent in the authentication request, a nonce claim must
         * be present and its value checked to verify that it is the same value
         * as the one that was sent in the authentication request to detect
         * replay attacks.
         */
        if (configuration.isUseNonce()) {

            final String nonce;

            try {
                nonce = claims.getStringClaim(NONCE);
            } catch (java.text.ParseException ex) {
                throw new IllegalStateException("Invalid nonce claim", ex);
            }

            if (isNull(nonce)) {
                throw new IllegalStateException("Missing nonce claim");
            }

            if (!expectedNonce.getValue().equals(nonce)) {
                throw new IllegalStateException("Invalid nonce claim : " + nonce);
            }
        }
//      If the acr Claim was requested, the Client SHOULD check that the asserted Claim Value is appropriate. The meaning and processing of acr Claim Values is out of scope for this specification. ??
//      If the auth_time Claim was requested, either through a specific request for this Claim or by using the max_age parameter, the Client SHOULD check the auth_time Claim value and request re-authentication if it determines too much time has elapsed since the last End-User authentication.
    }

}

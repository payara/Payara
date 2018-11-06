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
package fish.payara.security.openid.controller;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.BadJWTException;
import static fish.payara.security.openid.api.OpenIdConstant.NONCE;
import fish.payara.security.openid.domain.OpenIdConfiguration;
import static java.util.Objects.isNull;

/**
 * Validates the ID token
 *
 * @author Gaurav Gupta
 */
public class IdTokenClaimsSetVerifier extends TokenClaimsSetVerifier {

    private final String expectedNonceHash;

    public IdTokenClaimsSetVerifier(String expectedNonceHash, OpenIdConfiguration configuration) {
        super(configuration);
        this.expectedNonceHash = expectedNonceHash;
    }

    /**
     * Validate ID Token's claims
     *
     * @param claims
     * @throws com.nimbusds.jwt.proc.BadJWTException
     */
    @Override
    public void verify(JWTClaimsSet claims) throws BadJWTException {

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
            if (isNull(expectedNonceHash)) {
                throw new IllegalStateException("Missing expected nonce claim");
            }
            if (!expectedNonceHash.equals(nonce)) {
                throw new IllegalStateException("Invalid nonce claim : " + nonce);
            }
        }

//      5.5.1.  Individual Claims Requests
//      If the acr Claim was requested, the Client SHOULD check that the asserted Claim Value is appropriate. The meaning and processing of acr Claim Values is out of scope for this specification. ??
//      If the auth_time Claim was requested, either through a specific request for this Claim or by using the max_age parameter, the Client SHOULD check the auth_time Claim value and request re-authentication if it determines too much time has elapsed since the last End-User authentication.
    }

}

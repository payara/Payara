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

import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.BadJWTException;
import fish.payara.security.openid.api.AccessToken;
import static fish.payara.security.openid.api.OpenIdConstant.ACCESS_TOKEN_HASH;
import fish.payara.security.openid.domain.OpenIdConfiguration;
import static java.nio.charset.StandardCharsets.US_ASCII;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Map;

/**
 * Validates the Access token
 *
 * @author Gaurav Gupta
 */
public class AccessTokenClaimsSetVerifier extends TokenClaimsSetVerifier {

    private final AccessToken accessToken;

    private final Algorithm idTokenAlgorithm;

    private final Map<String, Object> idTokenClaims;

    public AccessTokenClaimsSetVerifier(
            AccessToken accessToken,
            Algorithm idTokenAlgorithm,
            Map<String, Object> idTokenClaims,
            OpenIdConfiguration configuration) {
        super(configuration);
        this.accessToken = accessToken;
        this.idTokenAlgorithm = idTokenAlgorithm;
        this.idTokenClaims = idTokenClaims;
    }

    @Override
    public void verify(JWTClaimsSet claims) throws BadJWTException {
        validateAccessToken();
    }

    public void validateAccessToken() {
        if (idTokenClaims.containsKey(ACCESS_TOKEN_HASH)) {

            //Get the message digest for the JWS algorithm value used in the header(alg) of the ID Token
            MessageDigest md = getMessageDigest(idTokenAlgorithm);

            // Hash the octets of the ASCII representation of the access_token with the hash algorithm
            md.update(accessToken.toString().getBytes(US_ASCII));
            byte[] hash = md.digest();

            // Take the left-most half of the hash and base64url encode it.
            byte[] leftHalf = Arrays.copyOf(hash, hash.length / 2);
            String accessTokenHash = Base64URL.encode(leftHalf).toString();

            // The value of at_hash in the ID Token MUST match the value produced
            if (!idTokenClaims.get(ACCESS_TOKEN_HASH).equals(accessTokenHash)) {
                throw new IllegalStateException("Invalid access token hash (at_hash) value");
            }
        }
    }

    /**
     * Get the message digest instance for the given JWS algorithm value.
     *
     * @param algorithm The JSON Web Signature (JWS) algorithm.
     *
     * @return The message digest instance
     */
    private MessageDigest getMessageDigest(Algorithm algorithm) {
        String mdAlgorithm = "SHA-" + algorithm.getName().substring(2);

        try {
            return MessageDigest.getInstance(mdAlgorithm);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("No MessageDigest instance found with the specified algorithm : " + mdAlgorithm, ex);
        }
    }

}

/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.jwtauth.jwt;

import static com.nimbusds.jose.JWSAlgorithm.RS256;
import static java.util.Arrays.asList;
import static javax.json.Json.createObjectBuilder;
import static org.eclipse.microprofile.jwt.Claims.exp;
import static org.eclipse.microprofile.jwt.Claims.groups;
import static org.eclipse.microprofile.jwt.Claims.iat;
import static org.eclipse.microprofile.jwt.Claims.iss;
import static org.eclipse.microprofile.jwt.Claims.jti;
import static org.eclipse.microprofile.jwt.Claims.preferred_username;
import static org.eclipse.microprofile.jwt.Claims.raw_token;
import static org.eclipse.microprofile.jwt.Claims.sub;
import static org.eclipse.microprofile.jwt.Claims.upn;

import java.io.StringReader;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonNumber;
import javax.json.JsonString;
import javax.json.JsonValue;

import org.eclipse.microprofile.jwt.Claims;

import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.SignedJWT;

/**
 * Parses a JWT bearer token and validates it according to the MP-JWT rules.
 * 
 * @author Arjan Tijms
 */
public class JwtTokenParser {
    
    private final List<Claims> requiredClaims = asList(iss, sub, exp, iat, jti, groups);
    
    public JsonWebTokenImpl parse(String bearerToken, String issuer, PublicKey publicKey) throws Exception {
        SignedJWT signedJWT = SignedJWT.parse(bearerToken);
        
        // MP-JWT 1.0 4.1 typ
        if (!checkIsJWT(signedJWT.getHeader())) {
            throw new IllegalStateException("Not JWT");
        }

        // 1.0 4.1 alg + MP-JWT 1.0 6.1 1
        if (!signedJWT.getHeader().getAlgorithm().equals(RS256)) {
            throw new IllegalStateException("Not RS256");
        }
        
        Map<String, JsonValue> rawClaims = 
                new HashMap<>(
                        Json.createReader(new StringReader(signedJWT.getPayload().toString()))
                            .readObject());
        
        // MP-JWT 1.0 4.1 Minimum MP-JWT Required Claims
        if (!checkRequiredClaimsPresent(rawClaims)) {
            throw new IllegalStateException("Not all required claims present");
        }
        
        // MP-JWT 1.0 4.1 upn - has fallbacks
        String callerPrincipalName = getCallerPrincipalName(rawClaims);
        if (callerPrincipalName == null) {
            throw new IllegalStateException("One of upn, preferred_username or sub is required to be non null");
        }
        
        // MP-JWT 1.0 6.1 2
        if (!checkIssuer(rawClaims, issuer)) {
            throw new IllegalStateException("Bad issuer");
        }
        
        if (!checkNotExpired(rawClaims)) {
            throw new IllegalStateException("Expired");
        }

        // MP-JWT 1.0 6.1 2
        if (!signedJWT.verify(new RSASSAVerifier((RSAPublicKey) publicKey))) {
            throw new IllegalStateException("Signature invalid");
        }
        
        rawClaims.put(
                raw_token.name(), 
                createObjectBuilder().add("token", bearerToken).build().get("token"));
        
        return new JsonWebTokenImpl(callerPrincipalName, rawClaims);
    }
        
    private boolean checkRequiredClaimsPresent(Map<String, JsonValue> presentedClaims) {
        for (Claims requiredClaim : requiredClaims) {
            if (presentedClaims.get(requiredClaim.name()) == null) {
                return false;
            }
        }

        return true;
    }

    private boolean checkNotExpired(Map<String, JsonValue> presentedClaims) {
        int currentTime = (int) (System.currentTimeMillis() / 1000);
        int expiredTime = ((JsonNumber) presentedClaims.get(exp.name())).intValue();

        return currentTime < expiredTime;
    }

    private boolean checkIssuer(Map<String, JsonValue> presentedClaims, String acceptedIssuer) {
        if (!(presentedClaims.get(iss.name()) instanceof JsonString)) {
            return false;
        }

        String issuer = ((JsonString) presentedClaims.get(iss.name())).getString();

        // TODO: make acceptedIssuers (set)
        return acceptedIssuer.equals(issuer);
    }

    private boolean checkIsJWT(JWSHeader header) {
        return header.getType().toString().equals("JWT");
    }
    
    private String getCallerPrincipalName(Map<String, JsonValue> rawClaims) {
        JsonString callerPrincipalClaim = (JsonString) rawClaims.get(upn.name());
        
        if (callerPrincipalClaim == null) {
            callerPrincipalClaim = (JsonString) rawClaims.get(preferred_username.name());
        }
        
        if (callerPrincipalClaim == null) {
            callerPrincipalClaim = (JsonString) rawClaims.get(sub.name());
        }
        
        if (callerPrincipalClaim == null) {
            return null;
        }
        
        return callerPrincipalClaim.getString();
    }

}

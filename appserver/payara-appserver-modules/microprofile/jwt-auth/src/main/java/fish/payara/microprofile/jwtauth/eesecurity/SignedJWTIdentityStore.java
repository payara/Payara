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
package fish.payara.microprofile.jwtauth.eesecurity;

import static java.util.Arrays.asList;
import static javax.security.enterprise.identitystore.CredentialValidationResult.INVALID_RESULT;
import static org.eclipse.microprofile.jwt.Claims.exp;
import static org.eclipse.microprofile.jwt.Claims.groups;
import static org.eclipse.microprofile.jwt.Claims.iat;
import static org.eclipse.microprofile.jwt.Claims.iss;
import static org.eclipse.microprofile.jwt.Claims.jti;
import static org.eclipse.microprofile.jwt.Claims.sub;
import static org.eclipse.microprofile.jwt.Claims.upn;

import java.io.StringReader;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.security.enterprise.identitystore.CredentialValidationResult;
import javax.security.enterprise.identitystore.IdentityStore;

import org.eclipse.microprofile.jwt.Claims;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.SignedJWT;

import fish.payara.microprofile.jwtauth.jwt.JsonWebTokenImpl;

/**
 * Identity store capable of asserting that a signed JWT token is valid according to
 * the MP-JWT 1.0 spec.
 * 
 * @author Arjan Tijms
 *
 */
public class SignedJWTIdentityStore implements IdentityStore {
    
    private final List<Claims> requiredClaims = asList(iss, sub, exp, iat, jti, upn, groups);
    
    public CredentialValidationResult validate(SignedJWTCredential signedJWTCredential) {

        try {
            SignedJWT signedJWT = SignedJWT.parse(signedJWTCredential.getSignedJWT());
            
            // MP-JWT 1.0 4.1 typ
            if (!checkIsJWT(signedJWT.getHeader())) {
                return INVALID_RESULT;
            }

            // 1.0 4.1 alg + MP-JWT 1.0 6.1 1
            if (!signedJWT.getHeader().getAlgorithm().equals(JWSAlgorithm.RS256)) {
                throw new IllegalStateException();
            }
            
            Map<String, JsonValue> rawClaims = 
                    new HashMap<>(
                            Json.createReader(new StringReader(signedJWT.getPayload().toString()))
                                .readObject());
            
            
            // MP-JWT 1.0 4.1 Minimum MP-JWT Required Claims
            if (!checkRequiredClaimsPresent(rawClaims)) {
                return INVALID_RESULT;
            }
            
            // MP-JWT 1.0 6.1 2
            if (!checkIssuer(rawClaims)) {
                return INVALID_RESULT;
            }
            
            if (!checkNotExpired(rawClaims)) {
                return INVALID_RESULT;
            }
            
            PublicKey publicKey = readPublicKey("/publicKey.pem");

            // MP-JWT 1.0 6.1 2
            if (!signedJWT.verify(new RSASSAVerifier((RSAPublicKey) publicKey))) {
                throw new IllegalStateException();
            }
            
            if (!(rawClaims.get("upn") instanceof JsonString)) {
                throw new IllegalStateException();
            }
            
            String userPrincipalName = ((JsonString) rawClaims.get("upn")).getString();
            List<String> groups = ((JsonArray) rawClaims.get("groups")).getValuesAs(JsonString.class).stream().map(t -> t.getString()).collect(Collectors.toList());
            
            return new CredentialValidationResult(
                    new JsonWebTokenImpl(
                            userPrincipalName, 
                            rawClaims), 
                    new HashSet<>(groups));
            
        } catch (Exception e) {
            e.printStackTrace();
        }

        return INVALID_RESULT;
    }
    
    public PublicKey readPublicKey(String resourceName) throws Exception {
        
        byte[] byteBuffer = new byte[16384];
        int length = getClass().getResource(resourceName).openStream().read(byteBuffer);
        
        String key = new String(byteBuffer, 0, length)
                            .replaceAll("-----BEGIN (.*)-----", "")
                            .replaceAll("-----END (.*)----", "")
                            .replaceAll("\r\n", "")
                            .replaceAll("\n", "")
                            .trim();

        return KeyFactory.getInstance("RSA")
                         .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(key)));
    }
   
    private boolean checkRequiredClaimsPresent(Map<String, JsonValue> presentedClaims) {
        for (Claims requiredClaim : requiredClaims) {
            if (presentedClaims.get(requiredClaim.name())  == null) {
                return false;
            }
        }
        
        return true;
    }
    
    private boolean checkNotExpired(Map<String, JsonValue> presentedClaims) {
        int currentTime = (int) (System.currentTimeMillis() / 1000);
        long expiredTime = ((JsonNumber) presentedClaims.get(exp.name())).intValue();
        
        return currentTime < expiredTime;
    }
    
    private boolean checkIssuer(Map<String, JsonValue> presentedClaims) {
        if (!(presentedClaims.get(iss.name()) instanceof JsonString)) {
            return false;
        }
        
        String issuer = ((JsonString) presentedClaims.get(iss.name())).getString();
        
        return !issuer.isEmpty(); // for now
    }
    
    private boolean checkIsJWT(JWSHeader header) {
        return header.getType().toString().equals("JWT");
    }
    

}

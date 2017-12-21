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

import static java.lang.Thread.currentThread;
import static javax.security.enterprise.identitystore.CredentialValidationResult.INVALID_RESULT;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import javax.security.enterprise.identitystore.CredentialValidationResult;
import javax.security.enterprise.identitystore.IdentityStore;

import fish.payara.microprofile.jwtauth.jwt.JsonWebTokenImpl;
import fish.payara.microprofile.jwtauth.jwt.JwtTokenParser;

/**
 * Identity store capable of asserting that a signed JWT token is valid according to
 * the MP-JWT 1.0 spec.
 * 
 * @author Arjan Tijms
 */
public class SignedJWTIdentityStore implements IdentityStore {
    
    private final JwtTokenParser jwtTokenParser = new JwtTokenParser();
    
    private String acceptedIssuer;
    
    public SignedJWTIdentityStore() {
        try {
            Properties properties = new Properties();
            properties.load(currentThread().getContextClassLoader().getResource("/payara-mp-jwt.properties").openStream());
            
            acceptedIssuer = properties.getProperty("accepted.issuer");
            
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load properties", e);
        }
    }
    
    public CredentialValidationResult validate(SignedJWTCredential signedJWTCredential) {

        try {
            JsonWebTokenImpl jsonWebToken = 
                    jwtTokenParser.parse(
                            signedJWTCredential.getSignedJWT(), 
                            acceptedIssuer, 
                            readPublicKey("/publicKey.pem"));
            
            List<String> groups = new ArrayList<String>(
                    jsonWebToken.getClaim("groups"));
            
            return new CredentialValidationResult(
                    jsonWebToken, 
                    new HashSet<>(groups));
            
        } catch (Exception e) {
            // Ignore
        }

        return INVALID_RESULT;
    }
    
    public PublicKey readPublicKey(String resourceName) throws Exception {
        
        byte[] byteBuffer = new byte[16384];
        int length = currentThread().getContextClassLoader()
                                    .getResource(resourceName)
                                    .openStream()
                                    .read(byteBuffer);
        
        String key = new String(byteBuffer, 0, length)
                            .replaceAll("-----BEGIN (.*)-----", "")
                            .replaceAll("-----END (.*)----", "")
                            .replaceAll("\r\n", "")
                            .replaceAll("\n", "")
                            .trim();

        return KeyFactory.getInstance("RSA")
                         .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(key)));
    }
    

}

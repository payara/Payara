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

import fish.payara.microprofile.jwtauth.jwt.JsonWebTokenImpl;
import fish.payara.microprofile.jwtauth.jwt.JwtTokenParser;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import static java.lang.Thread.currentThread;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import static java.util.logging.Level.FINEST;
import java.util.logging.Logger;
import javax.enterprise.inject.spi.DeploymentException;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.security.enterprise.identitystore.CredentialValidationResult;
import static javax.security.enterprise.identitystore.CredentialValidationResult.INVALID_RESULT;
import javax.security.enterprise.identitystore.IdentityStore;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import static org.eclipse.microprofile.jwt.config.Names.ISSUER;
import static org.eclipse.microprofile.jwt.config.Names.VERIFIER_PUBLIC_KEY;
import static org.eclipse.microprofile.jwt.config.Names.VERIFIER_PUBLIC_KEY_LOCATION;

/**
 * Identity store capable of asserting that a signed JWT token is valid according to
 * the MP-JWT 1.0 spec.
 * 
 * @author Arjan Tijms
 */
public class SignedJWTIdentityStore implements IdentityStore {
    
    private static final Logger LOGGER = Logger.getLogger(SignedJWTIdentityStore.class.getName());

    private static final String RSA_ALGORITHM = "RSA";

    private final JwtTokenParser jwtTokenParser;

    private final String acceptedIssuer;

    private final Config config;
    
    public SignedJWTIdentityStore() {
        config = ConfigProvider.getConfig();
        
        Optional<Properties> properties = readVendorProperties();
        acceptedIssuer = readVendorIssuer(properties)
                .orElseGet(() -> config.getOptionalValue(ISSUER, String.class)
                .orElseThrow(() -> new IllegalStateException("No issuer found")));
        
        jwtTokenParser = new JwtTokenParser(readEnabledNamespace(properties), readCustomNamespace(properties));
    }
    
    public CredentialValidationResult validate(SignedJWTCredential signedJWTCredential) {
        try {

            Optional<PublicKey> publicKey = readPublicKeyFromLocation("/publicKey.pem");
            if (!publicKey.isPresent()) {
                publicKey = readMPEmbeddedPublicKey();
            }
            if (!publicKey.isPresent()) {
                publicKey = readMPPublicKeyFromLocation();
            }
            if (!publicKey.isPresent()) {
                throw new IllegalStateException("No PublicKey found");
            }

            JsonWebTokenImpl jsonWebToken
                    = jwtTokenParser.parse(
                            signedJWTCredential.getSignedJWT(),
                            acceptedIssuer,
                            publicKey.get()
                    );
            
            List<String> groups = new ArrayList<>(
                    jsonWebToken.getClaim("groups"));
            
            return new CredentialValidationResult(
                    jsonWebToken, 
                    new HashSet<>(groups));
            
        } catch (Exception e) {
            LOGGER.log(FINEST, "Exception trying to parse JWT token.", e);
        }

        return INVALID_RESULT;
    }
    
    private Optional<Properties> readVendorProperties() {
        URL mpJwtResource = currentThread().getContextClassLoader().getResource("/payara-mp-jwt.properties");
        Properties properties = null;
        if (mpJwtResource != null) {
            try {
                properties = new Properties();
                properties.load(mpJwtResource.openStream());
            } catch (IOException e) {
                throw new IllegalStateException("Failed to load Vendor properties from resource file", e);
            }
        }
        return Optional.ofNullable(properties);
    }
    
    private Optional<String> readVendorIssuer(Optional<Properties> properties) {
        return properties.isPresent() ? Optional.ofNullable(properties.get().getProperty("accepted.issuer")) : Optional.empty();
    }
    
    private Optional<Boolean> readEnabledNamespace(Optional<Properties> properties){
        return properties.isPresent() ? Optional.ofNullable(Boolean.valueOf(properties.get().getProperty("enable.namespace", "false"))) : Optional.empty();
    }
    
    private Optional<String> readCustomNamespace(Optional<Properties> properties){
        return properties.isPresent() ? Optional.ofNullable(properties.get().getProperty("custom.namespace", null)) : Optional.empty();
    }

    private Optional<PublicKey> readMPEmbeddedPublicKey() throws Exception {
        Optional<String> key = config.getOptionalValue(VERIFIER_PUBLIC_KEY, String.class);
        if (!key.isPresent()) {
            return Optional.empty();
        }
        return createPublicKey(key.get());
    }

    private Optional<PublicKey> readMPPublicKeyFromLocation() throws Exception {
        Optional<String> locationOpt = config.getOptionalValue(VERIFIER_PUBLIC_KEY_LOCATION, String.class);

        if (!locationOpt.isPresent()) {
            return Optional.empty();
        }

        String publicKeyLocation = locationOpt.get();

        return readPublicKeyFromLocation(publicKeyLocation);
    }

    private Optional<PublicKey> readPublicKeyFromLocation(String publicKeyLocation) throws Exception {

        URL publicKeyURL = currentThread().getContextClassLoader().getResource(publicKeyLocation);

        if (publicKeyURL == null) {
            try {
                publicKeyURL = new URL(publicKeyLocation);
            } catch (MalformedURLException ex) {
                publicKeyURL = null;
            }
        }
        if (publicKeyURL == null) {
            return Optional.empty();
        }

        byte[] byteBuffer = new byte[16384];
        int length = publicKeyURL.openStream()
                .read(byteBuffer);
        String key = new String(byteBuffer, 0, length);
        return createPublicKey(key);
    }


    private Optional<PublicKey> createPublicKey(String key) throws Exception {
        try {
            return Optional.of(createPublicKeyFromPem(key));
        } catch (Exception pemEx) {
            try {
                return Optional.of(createPublicKeyFromJWKS(key));
            } catch (Exception jwksEx) {
                throw new DeploymentException(jwksEx);
            }
        }
    }

    private PublicKey createPublicKeyFromPem(String key) throws Exception {
        key = key.replaceAll("-----BEGIN (.*)-----", "")
                .replaceAll("-----END (.*)----", "")
                .replaceAll("\r\n", "")
                .replaceAll("\n", "")
                .trim();

        byte[] keyBytes = Base64.getDecoder().decode(key);
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(keyBytes);
        return KeyFactory.getInstance(RSA_ALGORITHM)
                .generatePublic(publicKeySpec);

    }

    private PublicKey createPublicKeyFromJWKS(String jwksValue) throws Exception {
        JsonObject jwks = parseJwks(jwksValue);
        JsonArray keys = jwks.getJsonArray("keys");
        JsonObject jwk = keys != null ? keys.getJsonObject(0) : jwks;

        // the public exponent
        byte[] exponentBytes = Base64.getUrlDecoder().decode(jwk.getString("e"));
        BigInteger exponent = new BigInteger(1, exponentBytes);

        // the modulus
        byte[] modulusBytes = Base64.getUrlDecoder().decode(jwk.getString("n"));
        BigInteger modulus = new BigInteger(1, modulusBytes);

        RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(modulus, exponent);
        return KeyFactory.getInstance(RSA_ALGORITHM)
                .generatePublic(publicKeySpec);
    }

    private JsonObject parseJwks(String jwksValue) throws Exception {
        JsonObject jwks;
        try {
            jwks = Json.createReader(new StringReader(jwksValue))
                    .readObject();
        } catch (Exception ex) {
            // if jwks is encoded
            byte[] jwksDecodedValue = Base64.getDecoder().decode(jwksValue);
            try (InputStream jwksStream = new ByteArrayInputStream(jwksDecodedValue)) {
                jwks = Json.createReader(jwksStream)
                        .readObject();
            }
        }
        return jwks;
    }



}

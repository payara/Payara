/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017-2019 Payara Foundation and/or its affiliates. All rights reserved.
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
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import javax.enterprise.inject.spi.DeploymentException;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.security.enterprise.identitystore.CredentialValidationResult;
import javax.security.enterprise.identitystore.IdentityStore;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.logging.Logger;

import static java.lang.Thread.currentThread;
import static java.util.logging.Level.FINEST;
import static javax.security.enterprise.identitystore.CredentialValidationResult.INVALID_RESULT;
import static org.eclipse.microprofile.jwt.config.Names.*;

/**
 * Identity store capable of asserting that a signed JWT token is valid
 * according to the MP-JWT 1.0 spec.
 *
 * @author Arjan Tijms
 */
public class SignedJWTIdentityStore implements IdentityStore {

    private static final Logger LOGGER = Logger.getLogger(SignedJWTIdentityStore.class.getName());

    private static final String RSA_ALGORITHM = "RSA";

    private final String acceptedIssuer;
    private final Optional<Boolean> enabledNamespace;
    private final Optional<String> customNamespace;

    private final Config config;

    public SignedJWTIdentityStore() {
        config = ConfigProvider.getConfig();

        Optional<Properties> properties = readVendorProperties();
        acceptedIssuer = readVendorIssuer(properties)
                .orElseGet(() -> config.getOptionalValue(ISSUER, String.class)
                .orElseThrow(() -> new IllegalStateException("No issuer found")));

        enabledNamespace = readEnabledNamespace(properties);
        customNamespace = readCustomNamespace(properties);
    }

    public CredentialValidationResult validate(SignedJWTCredential signedJWTCredential) {
        final JwtTokenParser jwtTokenParser = new JwtTokenParser(enabledNamespace, customNamespace);
        try {
            jwtTokenParser.parse(signedJWTCredential.getSignedJWT());
            String keyID = jwtTokenParser.getKeyID();

            Optional<PublicKey> publicKey = readDefaultPublicKey();
            if (!publicKey.isPresent()) {
                publicKey = readMPEmbeddedPublicKey(keyID);
            }
            if (!publicKey.isPresent()) {
                publicKey = readMPPublicKeyFromLocation(keyID);
            }
            if (!publicKey.isPresent()) {
                throw new IllegalStateException("No PublicKey found");
            }

            JsonWebTokenImpl jsonWebToken
                    = jwtTokenParser.verify(acceptedIssuer, publicKey.get());

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

    private Optional<Boolean> readEnabledNamespace(Optional<Properties> properties) {
        return properties.isPresent() ? Optional.ofNullable(Boolean.valueOf(properties.get().getProperty("enable.namespace", "false"))) : Optional.empty();
    }

    private Optional<String> readCustomNamespace(Optional<Properties> properties) {
        return properties.isPresent() ? Optional.ofNullable(properties.get().getProperty("custom.namespace", null)) : Optional.empty();
    }

    private Optional<PublicKey> readDefaultPublicKey() throws Exception {
        return readPublicKeyFromLocation("/publicKey.pem", null);
    }

    private Optional<PublicKey> readMPEmbeddedPublicKey(String keyID) throws Exception {
        Optional<String> key = config.getOptionalValue(VERIFIER_PUBLIC_KEY, String.class);
        if (!key.isPresent()) {
            return Optional.empty();
        }
        return createPublicKey(key.get(), keyID);
    }

    private Optional<PublicKey> readMPPublicKeyFromLocation(String keyID) throws Exception {
        Optional<String> locationOpt = config.getOptionalValue(VERIFIER_PUBLIC_KEY_LOCATION, String.class);

        if (!locationOpt.isPresent()) {
            return Optional.empty();
        }

        String publicKeyLocation = locationOpt.get();

        return readPublicKeyFromLocation(publicKeyLocation, keyID);
    }

    private Optional<PublicKey> readPublicKeyFromLocation(String publicKeyLocation, String keyID) throws Exception {

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
        try (InputStream inputStream = publicKeyURL.openStream()) {
            String key = new String(byteBuffer, 0, inputStream.read(byteBuffer));
            return createPublicKey(key, keyID);
        }
    }

    private Optional<PublicKey> createPublicKey(String key, String keyID) throws Exception {
        try {
            return Optional.of(createPublicKeyFromPem(key));
        } catch (Exception pemEx) {
            try {
                return Optional.of(createPublicKeyFromJWKS(key, keyID));
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

    private PublicKey createPublicKeyFromJWKS(String jwksValue, String keyID) throws Exception {
        JsonObject jwks = parseJwks(jwksValue);
        JsonArray keys = jwks.getJsonArray("keys");
        JsonObject jwk = keys != null ? findJwk(keys, keyID) : jwks;

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
        try (JsonReader reader = Json.createReader(new StringReader(jwksValue))) {
            jwks = reader.readObject();
        } catch (Exception ex) {
            // if jwks is encoded
            byte[] jwksDecodedValue = Base64.getDecoder().decode(jwksValue);
            try (InputStream jwksStream = new ByteArrayInputStream(jwksDecodedValue);
                    JsonReader reader = Json.createReader(jwksStream)) {
                jwks = reader.readObject();
            }
        }
        return jwks;
    }

    private JsonObject findJwk(JsonArray keys, String keyID) {
        if (Objects.isNull(keyID) && keys.size() > 0) {
            return keys.getJsonObject(0);
        }

        for (JsonValue value : keys) {
            JsonObject jwk = value.asJsonObject();
            if (Objects.equals(keyID, jwk.getString("kid"))) {
                return jwk;
            }
        }

        throw new IllegalStateException("No matching JWK for KeyID.");
    }

}

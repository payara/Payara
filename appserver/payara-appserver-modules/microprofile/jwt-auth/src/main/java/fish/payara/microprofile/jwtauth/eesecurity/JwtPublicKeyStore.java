/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017-2025 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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

import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.logging.Logger;

import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import static org.eclipse.microprofile.jwt.config.Names.VERIFIER_PUBLIC_KEY;
import static org.eclipse.microprofile.jwt.config.Names.VERIFIER_PUBLIC_KEY_LOCATION;

public class JwtPublicKeyStore {
    
    private static final Logger LOGGER = Logger.getLogger(JwtPublicKeyStore.class.getName());
    private static final String RSA_ALGORITHM = "RSA";
    private static final String EC_ALGORITHM = "EC";
        
    private final Config config;
    private final Supplier<Optional<String>> cacheSupplier;
    private final Duration defaultCacheTTL;
    private final Duration retainOnErrorDuration;
    private String keyLocation = "/publicKey.pem";

    /**
     * @param defaultCacheTTL Public key cache TTL 
     */
    public JwtPublicKeyStore(Duration defaultCacheTTL, Duration retainOnErrorDuration) {
        this.config = ConfigProvider.getConfig();
        this.defaultCacheTTL = defaultCacheTTL;
        this.retainOnErrorDuration = retainOnErrorDuration;
        this.cacheSupplier = new KeyLoadingCache(this::readRawPublicKey)::get;
    }

    /**
     * @param defaultCacheTTL Public key cache TTL
     * @param keyLocation location of the public key
     */
    public JwtPublicKeyStore(Duration defaultCacheTTL, Duration retainOnErrorDuration, Optional<String> keyLocation) {
        this(defaultCacheTTL, retainOnErrorDuration);
        this.keyLocation = keyLocation.orElse(this.keyLocation);
    }

    /**
     * 
     * @param keyID The JWT key ID or null if no key ID was provided
     * @return Public key that can be used to verify the JWT
     * @throws IllegalStateException if no public key was found
     */
    public PublicKey getPublicKey(String keyID) {
        return cacheSupplier.get()
            .map(key -> createPublicKey(key, keyID))
            .orElseThrow(() -> new IllegalStateException("No PublicKey found"));
    }
    
    private CacheableString readRawPublicKey() {
        CacheableString publicKey = JwtKeyStoreUtils.readKeyFromLocation(keyLocation, defaultCacheTTL, retainOnErrorDuration);
        
        if (!publicKey.isPresent()) {
            publicKey = readMPEmbeddedPublicKey();
        }
        if (!publicKey.isPresent()) {
            publicKey = JwtKeyStoreUtils.readMPKeyFromLocation(config, VERIFIER_PUBLIC_KEY_LOCATION, defaultCacheTTL, retainOnErrorDuration);
        }
        return publicKey;
    }

    private CacheableString readMPEmbeddedPublicKey() {
        String publicKey = config.getOptionalValue(VERIFIER_PUBLIC_KEY, String.class).orElse(null);
        return CacheableString.from(publicKey, defaultCacheTTL, retainOnErrorDuration);
    }

    private PublicKey createPublicKey(String key, String keyID) {
        try {
            return createPublicKeyFromPem(key);
        } catch (Exception pemEx) {
            try {
                return createPublicKeyFromJWKS(key, keyID);
            } catch (Exception jwksEx) {
                throw new DeploymentException(jwksEx);
            }
        }
    }

    private PublicKey createPublicKeyFromPem(String key) throws Exception {
        key = JwtKeyStoreUtils.trimPem(key);

        byte[] keyBytes = Base64.getDecoder().decode(key);
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(keyBytes);
        // Is there a better way to determine which key spec to use here?
        try {
            return KeyFactory.getInstance(RSA_ALGORITHM).generatePublic(publicKeySpec);
        } catch (InvalidKeySpecException invalidKeySpecException) {
            // Try ECDSA
            LOGGER.finer("Caught InvalidKeySpecException creating public key from PEM using RSA algorithm, " +
                    "attempting again using ECDSA");
            return KeyFactory.getInstance(EC_ALGORITHM).generatePublic(publicKeySpec);
        }
    }

    private PublicKey createPublicKeyFromJWKS(String jwksValue, String keyID) throws Exception {
        JsonObject jwks = JwtKeyStoreUtils.parseJwks(jwksValue);
        JsonArray keys = jwks.getJsonArray("keys");
        JsonObject jwk = keys != null ? JwtKeyStoreUtils.findJwk(keys, keyID) : jwks;

        // Check if an RSA or ECDSA key needs to be created
        String kty = jwk.getString("kty");
        if (kty == null) {
            throw new DeploymentException("Could not determine key type - kty field not present");
        }
        if (kty.equals("RSA")) {
            // the public exponent
            byte[] exponentBytes = Base64.getUrlDecoder().decode(jwk.getString("e"));
            BigInteger exponent = new BigInteger(1, exponentBytes);

            // the modulus
            byte[] modulusBytes = Base64.getUrlDecoder().decode(jwk.getString("n"));
            BigInteger modulus = new BigInteger(1, modulusBytes);

            RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(modulus, exponent);
            return KeyFactory.getInstance(RSA_ALGORITHM)
                    .generatePublic(publicKeySpec);
        } else if (kty.equals("EC")) {
            // Get x and y to create EC point
            byte[] xBytes = Base64.getUrlDecoder().decode(jwk.getString("x"));
            BigInteger x = new BigInteger(1, xBytes);
            byte[] yBytes = Base64.getUrlDecoder().decode(jwk.getString("y"));
            BigInteger y = new BigInteger(1, yBytes);
            ECPoint ecPoint = new ECPoint(x, y);

            // Get params
            AlgorithmParameters parameters = AlgorithmParameters.getInstance(EC_ALGORITHM);
            String crv = jwk.getString("crv");

            if (!crv.equals("P-256")) {
                throw new DeploymentException("Could not get EC key from JWKS: crv does not equal P-256");
            }
            parameters.init(new ECGenParameterSpec("secp256r1"));

            ECPublicKeySpec publicKeySpec = new ECPublicKeySpec(ecPoint, parameters.getParameterSpec(ECParameterSpec.class));
            return KeyFactory.getInstance(EC_ALGORITHM)
                    .generatePublic(publicKeySpec);
        } else {
            throw new DeploymentException("Could not determine key type - JWKS kty field does not equal RSA or EC");
        }
    }
}

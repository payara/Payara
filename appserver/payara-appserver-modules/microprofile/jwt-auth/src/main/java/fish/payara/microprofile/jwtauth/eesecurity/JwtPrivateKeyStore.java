/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017-2025 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Private License Version 2 only ("GPL") or the Common Development
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
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import java.util.function.Supplier;
import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import static org.eclipse.microprofile.jwt.config.Names.DECRYPTOR_KEY_LOCATION;

public class JwtPrivateKeyStore {

    private final Config config;
    private final Supplier<Optional<String>> cacheSupplier;
    private final Duration defaultCacheTTL;
    private final Duration retainOnErrorDuration;
    private String keyLocation = "/privateKey.pem";

    /**
     * @param defaultCacheTTL Private key cache TTL
     */
    public JwtPrivateKeyStore(Duration defaultCacheTTL, Duration retainOnErrorDuration) {
        this.config = ConfigProvider.getConfig();
        this.defaultCacheTTL = defaultCacheTTL;
        this.retainOnErrorDuration = retainOnErrorDuration;
        this.cacheSupplier = new KeyLoadingCache(this::readRawPrivateKey)::get;
    }

    /**
     * @param defaultCacheTTL Private key cache TTL
     * @param keyLocation location of the private key
     */
    public JwtPrivateKeyStore(Duration defaultCacheTTL, Duration retainOnErrorDuration, Optional<String> keyLocation) {
        this(defaultCacheTTL, retainOnErrorDuration);
        this.keyLocation = keyLocation.orElse(this.keyLocation);
    }

    private CacheableString readRawPrivateKey() {
        CacheableString privateKey = JwtKeyStoreUtils.readKeyFromLocation(keyLocation, defaultCacheTTL, retainOnErrorDuration);

        if (!privateKey.isPresent()) {
            privateKey = JwtKeyStoreUtils.readMPKeyFromLocation(config, DECRYPTOR_KEY_LOCATION, defaultCacheTTL, retainOnErrorDuration);
        }

        return privateKey;
    }

    public PrivateKey getPrivateKey(String keyId) {
        return cacheSupplier.get()
                .map(key -> createPrivateKey(key, keyId))
                .orElseThrow(() -> new IllegalStateException("No PrivateKey found"));
    }

    private PrivateKey createPrivateKey(String key, String keyId) {
        try {
            return createPrivateKeyFromPem(key);
        } catch (Exception pemEx) {
            try {
                return createPrivateKeyFromJWKS(key, keyId);
            } catch (Exception jwksEx) {
                throw new DeploymentException(jwksEx);
            }
        }
    }

    private PrivateKey createPrivateKeyFromPem(String key) throws Exception {
        key = JwtKeyStoreUtils.trimPem(key);

        byte[] keyBytes = Base64.getDecoder().decode(key);
        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(keyBytes);
        return KeyFactory.getInstance("RSA").generatePrivate(privateKeySpec);
    }

    private PrivateKey createPrivateKeyFromJWKS(String jwksValue, String keyId) throws Exception {
        JsonObject jwks = JwtKeyStoreUtils.parseJwks(jwksValue);
        JsonArray keys = jwks.getJsonArray("keys");
        JsonObject jwk = keys != null ? JwtKeyStoreUtils.findJwk(keys, keyId) : jwks;

        // Check if an RSA or ECDSA key needs to be created
        String kty = jwk.getString("kty");
        if (kty == null) {
            throw new DeploymentException("Could not determine key type - kty field not present");
        }
        if (kty.equals("RSA")) {
            // The modulus
            byte[] modulusBytes = Base64.getUrlDecoder().decode(jwk.getString("n"));
            BigInteger modulus = new BigInteger(1, modulusBytes);

            // The private exponent
            byte[] exponentBytes = Base64.getUrlDecoder().decode(jwk.getString("d"));
            BigInteger exponent = new BigInteger(1, exponentBytes);

            RSAPrivateKeySpec privateKeySpec = new RSAPrivateKeySpec(modulus, exponent);
            return KeyFactory.getInstance("RSA").generatePrivate(privateKeySpec);
        } else {
            throw new DeploymentException("Could not determine key type - JWKS kty field does not equal RSA or EC");
        }
    }
}

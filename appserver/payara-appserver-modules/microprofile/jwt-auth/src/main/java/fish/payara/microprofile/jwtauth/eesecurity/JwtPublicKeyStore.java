/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2017-2020] Payara Foundation and/or its affiliates. All rights reserved.
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
import static org.eclipse.microprofile.jwt.config.Names.VERIFIER_PUBLIC_KEY;
import static org.eclipse.microprofile.jwt.config.Names.VERIFIER_PUBLIC_KEY_LOCATION;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.inject.spi.DeploymentException;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.glassfish.grizzly.http.util.ContentType;

class JwtPublicKeyStore {
    
    private static final Logger LOGGER = Logger.getLogger(JwtPublicKeyStore.class.getName());
    private static final String RSA_ALGORITHM = "RSA";
    
        
    private final Config config;
    private final Supplier<Optional<String>> cacheSupplier;
    private final Duration defaultCacheTTL;
    
    /**
     * @param defaultCacheTTL Public key cache TTL 
     */
    public JwtPublicKeyStore(Duration defaultCacheTTL) {
        this.config = ConfigProvider.getConfig();
        this.defaultCacheTTL = defaultCacheTTL;
        this.cacheSupplier = new PublicKeyLoadingCache(this::readRawPublicKey)::get;
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
        CacheableString publicKey = readDefaultPublicKey();
        
        if (!publicKey.isPresent()) {
            publicKey = readMPEmbeddedPublicKey();
        }
        if (!publicKey.isPresent()) {
            publicKey = readMPPublicKeyFromLocation();
        }
        return publicKey;
    }
    
    private CacheableString readDefaultPublicKey() {
        return readPublicKeyFromLocation("/publicKey.pem");
    }

    private CacheableString readMPEmbeddedPublicKey() {
        String publicKey = config.getOptionalValue(VERIFIER_PUBLIC_KEY, String.class).orElse(null);
        return CacheableString.from(publicKey, defaultCacheTTL);
    }

    private CacheableString readMPPublicKeyFromLocation() {
        Optional<String> locationOpt = config.getOptionalValue(VERIFIER_PUBLIC_KEY_LOCATION, String.class);

        if (!locationOpt.isPresent()) {
            return CacheableString.empty(defaultCacheTTL);
        }

        String publicKeyLocation = locationOpt.get();

        return readPublicKeyFromLocation(publicKeyLocation);
    }
    
    private CacheableString readPublicKeyFromLocation(String publicKeyLocation) {

        URL publicKeyURL = currentThread().getContextClassLoader().getResource(publicKeyLocation);

        if (publicKeyURL == null) {
            try {
                publicKeyURL = new URL(publicKeyLocation);
            } catch (MalformedURLException ex) {
                publicKeyURL = null;
            }
        }
        if (publicKeyURL == null) {
            return CacheableString.empty(defaultCacheTTL);
        }
        
        try { 
            return readPublicKeyFromURL(publicKeyURL);
        } catch(IOException ex) {
            throw new IllegalStateException("Failed to read public key.", ex);
        }
    }
    
    private CacheableString readPublicKeyFromURL(URL publicKeyURL) throws IOException {
        
        URLConnection urlConnection = publicKeyURL.openConnection();
        Charset charset = Charset.defaultCharset();
        ContentType contentType = ContentType.newContentType(urlConnection.getContentType());
        if(contentType != null) {
            String charEncoding = contentType.getCharacterEncoding();
            if(charEncoding != null) {
                try {
                    if (!Charset.isSupported(charEncoding)) {
                        LOGGER.warning("Charset " + charEncoding + " for remote public key not supported, using default charset instead");
                    } else {
                        charset = Charset.forName(contentType.getCharacterEncoding());
                    }
                }catch (IllegalCharsetNameException ex){
                    LOGGER.severe("Charset " + ex.getCharsetName() + " for remote public key not support, Cause: " + ex.getMessage());
                }
            }
            
        }
        
        
        // There's no guarantee that the response will contain at most one Cache-Control header and at most one max-age directive.
        // Here, we apply the smallest of all max-age directives.
        Duration cacheTTL = urlConnection.getHeaderFields().entrySet().stream()
        		.filter(e -> e.getKey() != null && e.getKey().trim().equalsIgnoreCase("Cache-Control"))
        		.flatMap(headers -> headers.getValue().stream())
        		.flatMap(headerValue -> Stream.of(headerValue.split(",")))
        		.filter(directive -> directive.trim().startsWith("max-age"))
		        .map(maxAgeDirective -> {
					String[] keyValue = maxAgeDirective.split("=",2);
					String maxAge = keyValue[keyValue.length-1];
					try {
						return Duration.ofSeconds(Long.parseLong(maxAge));
					} catch(NumberFormatException e) {
						return null;
					}
				})
		        .filter(Objects::nonNull)
		        .min(Duration::compareTo)
		        .orElse(defaultCacheTTL);        	
        
        try (InputStream inputStream = urlConnection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, charset))){
            String keyContents = reader.lines().collect(Collectors.joining(System.lineSeparator()));
            return CacheableString.from(keyContents, cacheTTL);
        }
        
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
    
    private static class PublicKeyLoadingCache {
        
        private final Supplier<CacheableString> keySupplier;
        private Duration ttl;
        private long lastUpdated;
        private Optional<String> publicKey;
        
        
        public PublicKeyLoadingCache(Supplier<CacheableString> keySupplier) {
            this.ttl = Duration.ZERO;
            this.keySupplier = keySupplier;
        }
        
        public Optional<String> get() {
            long now = System.currentTimeMillis();
            if(now - lastUpdated > ttl.toMillis()) {
            	refresh();
            }
            
            return publicKey;
        }
        
        private synchronized void refresh() {
            long now = System.currentTimeMillis();
            if(now - lastUpdated > ttl.toMillis()) {
                CacheableString result = keySupplier.get();
                publicKey = result.getValue();
                ttl = result.getCacheTTL();
                lastUpdated = now;
            }
        }
        
    }
    
    private static class CacheableString {
    	
    	public static CacheableString empty(Duration cacheTTL) {
    		return from(null, cacheTTL);
    	}
    	
    	public static CacheableString from(String value, Duration cacheTTL) {
    		CacheableString instance = new CacheableString();
    		instance.cacheTTL = cacheTTL;
    		instance.value = value;
    		return instance;
    	}
    	
    	private String value;
    	private Duration cacheTTL;
    	
    	public Optional<String> getValue() {
    		return Optional.ofNullable(value);
    	}
    	
    	public Duration getCacheTTL() {
    		return cacheTTL;
    	}
    	
    	public boolean isPresent() {
    		return value != null;
    	}
    }
}

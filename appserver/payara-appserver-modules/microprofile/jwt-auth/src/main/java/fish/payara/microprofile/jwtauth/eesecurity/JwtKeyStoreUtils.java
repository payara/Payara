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

import org.eclipse.microprofile.config.Config;
import org.glassfish.grizzly.http.util.ContentType;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.time.Duration;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Thread.currentThread;

public class JwtKeyStoreUtils {

    private static final Logger LOGGER = Logger.getLogger(JwtKeyStoreUtils.class.getName());

    static String trimPem(String key) {
        return key.replaceAll("-----BEGIN (.*)-----", "")
                .replaceAll("-----END (.*)----", "")
                .replaceAll("\r\n", "")
                .replaceAll("\n", "")
                .trim();
    }

    static CacheableString readMPKeyFromLocation(Config config, String mpConfigProperty,
            Duration defaultCacheTTL, Duration retainOnErrorDuration) {
        Optional<String> locationOpt = config.getOptionalValue(mpConfigProperty, String.class);

        if (!locationOpt.isPresent()) {
            return CacheableString.empty(defaultCacheTTL);
        }

        String publicKeyLocation = locationOpt.get();

        return readKeyFromLocation(publicKeyLocation, defaultCacheTTL, retainOnErrorDuration);
    }

    static CacheableString readKeyFromLocation(String keyLocation, Duration defaultCacheTTL, Duration retainOnErrorDuration) {
        URL keyURL = currentThread().getContextClassLoader().getResource(keyLocation);

        if (keyURL == null) {
            try {
                keyURL = new URL(keyLocation);
            } catch (MalformedURLException ex) {
                keyURL = null;
            }
        }
        if (keyURL == null) {
            return CacheableString.empty(defaultCacheTTL);
        }

        try {
            return readKeyFromURL(keyURL, defaultCacheTTL, retainOnErrorDuration);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read key.", ex);
        }
    }

    private static CacheableString readKeyFromURL(URL keyURL, Duration defaultCacheTTL, Duration retainOnErrorDuration) throws IOException {
        URLConnection urlConnection = keyURL.openConnection();
        Charset charset = Charset.defaultCharset();
        ContentType contentType = ContentType.newContentType(urlConnection.getContentType());
        if (contentType != null) {
            String charEncoding = contentType.getCharacterEncoding();
            if (charEncoding != null) {
                try {
                    if (!Charset.isSupported(charEncoding)) {
                        LOGGER.warning("Charset " + charEncoding
                                + " for remote key not supported, using default charset instead");
                    } else {
                        charset = Charset.forName(contentType.getCharacterEncoding());
                    }
                } catch (IllegalCharsetNameException ex) {
                    LOGGER.severe("Charset " + ex.getCharsetName() + " for remote key not supported, Cause: "
                            + ex.getMessage());
                }
            }

        }

        // There's no guarantee that the response will contain at most one Cache-Control header and at most one max-age
        // directive. Here, we apply the smallest of all max-age directives.
        Duration cacheTTL = urlConnection.getHeaderFields().entrySet().stream()
                .filter(e -> e.getKey() != null && e.getKey().trim().equalsIgnoreCase("Cache-Control"))
                .flatMap(headers -> headers.getValue().stream())
                .flatMap(headerValue -> Stream.of(headerValue.split(",")))
                .filter(directive -> directive.trim().startsWith("max-age"))
                .map(maxAgeDirective -> {
                    String[] keyValue = maxAgeDirective.split("=", 2);
                    String maxAge = keyValue[keyValue.length - 1];
                    try {
                        return Duration.ofSeconds(Long.parseLong(maxAge));
                    } catch (NumberFormatException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .min(Duration::compareTo)
                .orElse(defaultCacheTTL);

        try (InputStream inputStream = urlConnection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, charset))) {
            String keyContents = reader.lines().collect(Collectors.joining(System.lineSeparator()));
            return CacheableString.from(keyContents, cacheTTL, retainOnErrorDuration);
        }
    }

    static JsonObject parseJwks(String jwksValue) throws Exception {
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

    static JsonObject findJwk(JsonArray keys, String keyID) {
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

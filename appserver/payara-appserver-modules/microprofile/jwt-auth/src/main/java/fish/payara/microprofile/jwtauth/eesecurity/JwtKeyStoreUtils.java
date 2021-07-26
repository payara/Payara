package fish.payara.microprofile.jwtauth.eesecurity;

import org.eclipse.microprofile.config.Config;
import org.glassfish.grizzly.http.util.ContentType;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
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
            Duration defaultCacheTTL) {
        Optional<String> locationOpt = config.getOptionalValue(mpConfigProperty, String.class);

        if (!locationOpt.isPresent()) {
            return CacheableString.empty(defaultCacheTTL);
        }

        String publicKeyLocation = locationOpt.get();

        return readKeyFromLocation(publicKeyLocation, defaultCacheTTL);
    }

    static CacheableString readKeyFromLocation(String keyLocation, Duration defaultCacheTTL) {
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
            return readKeyFromURL(keyURL, defaultCacheTTL);
        } catch(IOException ex) {
            throw new IllegalStateException("Failed to read key.", ex);
        }
    }

    private static CacheableString readKeyFromURL(URL keyURL, Duration defaultCacheTTL) throws IOException {
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
                } catch (IllegalCharsetNameException ex){
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


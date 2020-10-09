package fish.payara.microprofile.config.extensions.gcp;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.io.File;
import java.io.FileInputStream;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import org.glassfish.api.admin.ServerEnvironment;
import org.jvnet.hk2.annotations.Service;

import fish.payara.microprofile.config.extensions.gcp.model.Secret;
import fish.payara.microprofile.config.extensions.gcp.model.SecretResponse;
import fish.payara.microprofile.config.extensions.gcp.model.SecretsResponse;
import fish.payara.nucleus.microprofile.config.source.extension.ConfiguredExtensionConfigSource;
import fish.payara.security.oauth2.OAuth2Client;

@Service(name = "gcp-secrets-config-source")
public class GCPSecretsConfigSource extends ConfiguredExtensionConfigSource<GCPSecretsConfigSourceConfiguration> {

    private static final Logger LOGGER = Logger.getLogger(GCPSecretsConfigSource.class.getName());

    private static final String AUTH_URL = "https://www.googleapis.com/oauth2/v4/token";

    private static final String LIST_SECRETS_ENDPOINT = "https://secretmanager.googleapis.com/v1/projects/%s/secrets";
    private static final String GET_SECRETS_ENDPOINT = LIST_SECRETS_ENDPOINT + "/%s/versions/latest:access";

    private Client client = ClientBuilder.newClient();

    private OAuth2Client authClient;

    @Inject
    private ServerEnvironment env;

    @Override
    public void bootstrap() {
        String clientEmail = null;
        String privateKey = null;
        try {
            try (JsonParser parser = Json.createParser(new FileInputStream(getTokenFile()))) {
                while (parser.hasNext()) {
                    JsonParser.Event parseEvent = parser.next();
                    if (parseEvent == Event.KEY_NAME) {
                        final String keyName = parser.getString();
    
                        parser.next();
                        switch (keyName) {
                            case "client_email":
                                clientEmail = parser.getString();
                                break;
                            case "private_key":
                                privateKey = parser.getString();
                                break;
                        }
                        if (clientEmail != null && privateKey != null) {
                            break;
                        }
                    }
                }
    
                if (clientEmail == null || privateKey == null) {
                    throw new PropertyVetoException("Error reading JSON key file", new PropertyChangeEvent(configuration, "jsonKeyFile", null, null));
                }
            }
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Couldn't find or read the GCP key file, make sure it exists.", ex);
            return;
        }

        final SignedJWT jwt = buildJwt(
                // issuer
                clientEmail,
                // scope
                "https://www.googleapis.com/auth/cloud-platform");
        try {
            jwt.sign(new RSASSASigner(parsePrivateKey(privateKey)));

            Map<String, String> data = new HashMap<>();
            data.put("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer");
            data.put("assertion", jwt.serialize());
            this.authClient = new OAuth2Client(AUTH_URL, data);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | JOSEException e) {
            LOGGER.log(Level.WARNING, "An error occurred while signing the GCP auth token", e);
        }
    }

    private String authenticate() {
        final Response response = authClient.authenticate();
        final int status = response.getStatus();
        if (status == 200) {
            JsonObject data = response.readEntity(JsonObject.class);
            Integer expirySeconds = data.getInt("expires_in");
            authClient.expire(Duration.ofSeconds(expirySeconds));
            return data.getString("access_token");
        } else if (status == 400) {
            LOGGER.log(Level.WARNING, "Couldn't authenticate with GCP. Check your configuration options are correct.");
        }
        return null;
    }

    @Override
    public Map<String, String> getProperties() {

        Map<String, String> results = new HashMap<>();

        final String accessToken = authenticate();

        if (accessToken == null) {
            return results;
        }

        final WebTarget secretsTarget = client
                .target(String.format(LIST_SECRETS_ENDPOINT, configuration.getProjectName()));

        final Response secretsResponse = secretsTarget.request()
                .accept("application/json")
                .header("Authorization", "Bearer " + accessToken).get();

        if (secretsResponse.getStatus() != 200) {
            return results;
        }

        final List<Secret> secrets = secretsResponse
                .readEntity(SecretsResponse.class)
                .getSecrets();

        for (Secret secret : secrets) {
            final String secretName = secret.getName();
            results.put(secretName, getValue(secretName));
        }

        return results;
    }

    @Override
    public String getValue(String propertyName) {
        final String accessToken = authenticate();

        if (accessToken == null) {
            return null;
        }

        final WebTarget secretTarget = client
                .target(String.format(GET_SECRETS_ENDPOINT, configuration.getProjectName(), propertyName));

        final Response secretResponse = secretTarget
                .request()
                .accept("application/json")
                .header("Authorization", "Bearer " + accessToken)
                .get();

        if (secretResponse.getStatus() != 200) {
            return null;
        }

        final String value = secretResponse
                .readEntity(SecretResponse.class)
                .getPayload()
                .getData();

        return value;
    }

    @Override
    public boolean setValue(String name, String value) {
        return false;
    }

    @Override
    public boolean deleteValue(String name) {
        return false;
    }

    @Override
    public String getName() {
        return "gcp";
    }

    // Helpers

    private static SignedJWT buildJwt(final String issuer, final String scope) {
        Instant now = Instant.now();
        Instant expiry = now.plus(1, ChronoUnit.MINUTES);

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .audience(AUTH_URL)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(expiry))
                .claim("scope", scope)
                .build();

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .type(JOSEObjectType.JWT)
                .build();

        return new SignedJWT(header, claims);
    }

    private static PrivateKey parsePrivateKey(String privateKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
        String privateKeyContent = privateKey
                .replaceAll("\\n", "")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "");

        KeyFactory kf = KeyFactory.getInstance("RSA");

        PKCS8EncodedKeySpec keySpecPKCS8 = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyContent));
        return kf.generatePrivate(keySpecPKCS8);
    }

    private File getTokenFile() {
        return env.getConfigDirPath().toPath().resolve(configuration.getTokenFilePath()).toFile();
    }

}

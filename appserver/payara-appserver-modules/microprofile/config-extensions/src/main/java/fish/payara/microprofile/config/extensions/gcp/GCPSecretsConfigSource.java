package fish.payara.microprofile.config.extensions.gcp;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import org.jvnet.hk2.annotations.Service;

import fish.payara.microprofile.config.extensions.OAuth2Client;
import fish.payara.microprofile.config.extensions.gcp.model.Secret;
import fish.payara.microprofile.config.extensions.gcp.model.SecretResponse;
import fish.payara.microprofile.config.extensions.gcp.model.SecretsResponse;
import fish.payara.nucleus.microprofile.config.source.extension.ConfiguredExtensionConfigSource;

@Service(name = "gcp-secrets-config-source")
public class GCPSecretsConfigSource extends ConfiguredExtensionConfigSource<GCPSecretsConfigSourceConfiguration> {

    private static final String AUTH_URL = "https://www.googleapis.com/oauth2/v4/token";

    private static final String LIST_SECRETS_ENDPOINT = "https://secretmanager.googleapis.com/v1/projects/%s/secrets";
    private static final String GET_SECRETS_ENDPOINT = LIST_SECRETS_ENDPOINT + "/%s/versions/latest:access";

    private Client client = ClientBuilder.newClient();

    private OAuth2Client authClient;

    @Override
    public void bootstrap() {
        final SignedJWT jwt = buildJwt(
                // issuer
                "test-account@payara-bingo.iam.gserviceaccount.com",
                // scope
                "https://www.googleapis.com/auth/cloud-platform");
        try {
            jwt.sign(new RSASSASigner(getPrivateKey()));
            this.authClient = new OAuth2Client(AUTH_URL, jwt);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | JOSEException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Map<String, String> getProperties() {

        final String accessToken = authClient.getAccessToken();

        Map<String, String> results = new HashMap<>();

        final WebTarget secretsTarget = client
                .target(String.format(LIST_SECRETS_ENDPOINT, "payara-bingo"));

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
        final String accessToken = authClient.getAccessToken();

        final WebTarget secretTarget = client
                .target(String.format(GET_SECRETS_ENDPOINT, "payara-bingo", propertyName));

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

        new Exception(propertyName + " = " + value).printStackTrace();
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

        JWTClaimsSet claims = new JWTClaimsSet.Builder().issuer(issuer).audience(AUTH_URL).issueTime(Date.from(now))
                .expirationTime(Date.from(expiry)).claim("scope", scope).build();

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).build();

        return new SignedJWT(header, claims);
    }

    private static PrivateKey getPrivateKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
        String privateKey = "MY_PRIVATE_KEY";
        String privateKeyContent = privateKey.replaceAll("\\n", "").replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "");

        KeyFactory kf = KeyFactory.getInstance("RSA");

        PKCS8EncodedKeySpec keySpecPKCS8 = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyContent));
        return kf.generatePrivate(keySpecPKCS8);
    }

}

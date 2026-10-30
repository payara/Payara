/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2020-2021] Payara Foundation and/or its affiliates. All rights reserved.
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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonParser.Event;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import fish.payara.microprofile.config.extensions.oauth.OAuth2Client;
import fish.payara.microprofile.config.extensions.gcp.model.Secret;
import fish.payara.microprofile.config.extensions.gcp.model.SecretHolder;
import fish.payara.microprofile.config.extensions.gcp.model.SecretsResponse;
import fish.payara.nucleus.microprofile.config.source.extension.ConfiguredExtensionConfigSource;
import fish.payara.nucleus.microprofile.config.spi.MicroprofileConfigConfiguration;
import org.glassfish.api.admin.ServerEnvironment;
import org.jvnet.hk2.annotations.Service;

@Service(name = "gcp-secrets-config-source")
public class GCPSecretsConfigSource extends ConfiguredExtensionConfigSource<GCPSecretsConfigSourceConfiguration> {

    private static final Logger LOGGER = Logger.getLogger(GCPSecretsConfigSource.class.getName());

    private static final String AUTH_URL = "https://www.googleapis.com/oauth2/v4/token";

    private static final String LIST_SECRETS_ENDPOINT = "https://secretmanager.googleapis.com/v1/projects/%s/secrets";
    private static final String SECRET_ENDPOINT = LIST_SECRETS_ENDPOINT + "/%s";
    private static final String GET_SECRETS_VERSION_ENDPOINT = SECRET_ENDPOINT + "/versions/latest:access";
    private static final String CREATE_SECRET_VERSION_ENDPOINT = SECRET_ENDPOINT + ":addVersion";

    private Client client = ClientBuilder.newClient();

    private OAuth2Client authClient;

    @Inject
    private ServerEnvironment env;
    
    @Inject
    MicroprofileConfigConfiguration mpconfig;

    @Override
    public void bootstrap() {
        String clientEmail = null;
        String privateKey = null;
        try {
            final File tokenFile = getTokenFile();
            if (tokenFile == null) {
                LOGGER.warning("Couldn't find token file, make sure it's configured.");
            } else {
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
            }
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Couldn't find or read the GCP key file, make sure it exists.", ex);
        }

        Map<String, String> data = new HashMap<>();
        data.put("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer");

        if (clientEmail != null && privateKey != null) {
            try {
                final SignedJWT jwt = buildJwt(
                        // issuer
                        clientEmail,
                        // scope
                        "https://www.googleapis.com/auth/cloud-platform");
    
                jwt.sign(new RSASSASigner(parsePrivateKey(privateKey)));

                data.put("assertion", jwt.serialize());
            } catch (NoSuchAlgorithmException | InvalidKeySpecException | JOSEException e) {
                LOGGER.log(Level.WARNING, "An error occurred while signing the GCP auth token", e);
            }
        }

        this.authClient = new OAuth2Client(AUTH_URL, data);
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
                .accept(MediaType.APPLICATION_JSON)
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
    public Set<String> getPropertyNames() {
        return getProperties().keySet();
    }

    @Override
    public String getValue(String propertyName) {
        final String accessToken = authenticate();

        if (accessToken == null) {
            return null;
        }

        final WebTarget secretTarget = client
                .target(String.format(GET_SECRETS_VERSION_ENDPOINT, configuration.getProjectName(), propertyName));

        final Response secretResponse = secretTarget
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
                .get();

        final int status = secretResponse.getStatus();
        if (status != 200) {
            if (status != 400) {
                LOGGER.log(Level.WARNING, "Failed to get GCP secret. {0}", secretResponse.readEntity(String.class));
            }
            return null;
        }

        final String value = secretResponse
                .readEntity(SecretHolder.class)
                .getPayload()
                .getData();

        return value;
    }

    @Override
    public boolean setValue(String name, String value) {
        final String accessToken = authenticate();

        if (accessToken == null) {
            return false;
        }

        final WebTarget secretTarget = client
                .target(String.format(LIST_SECRETS_ENDPOINT, configuration.getProjectName()));

        final Response secretResponse = secretTarget
                .queryParam("secretId", name)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
                .post(Entity.entity(new Secret(), MediaType.APPLICATION_JSON));

        final int status = secretResponse.getStatus();
        if (status != 200 && status != 409) {
            LOGGER.log(Level.WARNING, "Failed to set GCP secret. {0}", secretResponse.readEntity(String.class));
            return false;
        }

        final WebTarget addSecretTarget = client
                .target(String.format(CREATE_SECRET_VERSION_ENDPOINT, configuration.getProjectName(), name));

        final Response addSecretResponse = addSecretTarget
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
                .post(Entity.entity(new SecretHolder(value), MediaType.APPLICATION_JSON));

        if (addSecretResponse.getStatus() == 200) {
            return true;
        }
        LOGGER.log(Level.WARNING, "Failed to set GCP secret. {0}", addSecretResponse.readEntity(String.class));
        return false;
    }

    @Override
    public boolean deleteValue(String name) {
        final String accessToken = authenticate();

        if (accessToken == null) {
            return false;
        }

        final WebTarget secretTarget = client
                .target(String.format(SECRET_ENDPOINT, configuration.getProjectName(), name));

        final Response secretResponse = secretTarget
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
                .delete();

        if (secretResponse.getStatus() == 200) {
            return true;
        }
        LOGGER.log(Level.WARNING, "Failed to delete GCP secret. {0}", secretResponse.readEntity(String.class));
        return false;
    }

    @Override
    public String getSource() {
        return "cloud";
    }

    @Override
    public String getName() {
        return "gcp";
    }
    
    @Override
    public int getOrdinal() {
        return Integer.parseInt(mpconfig.getCloudOrdinality());
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

        PKCS8EncodedKeySpec keySpecPKCS8 = new PKCS8EncodedKeySpec(new Base64(privateKeyContent).decode());
        return kf.generatePrivate(keySpecPKCS8);
    }

    private File getTokenFile() {
        final String fileName = configuration.getTokenFilePath();
        if (fileName != null) {
            return env.getConfigDirPath().toPath().resolve(configuration.getTokenFilePath()).toFile();
        }
        return null;
    }

}

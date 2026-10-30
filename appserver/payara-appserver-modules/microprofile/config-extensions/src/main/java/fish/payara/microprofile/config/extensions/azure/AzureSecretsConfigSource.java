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
package fish.payara.microprofile.config.extensions.azure;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import fish.payara.microprofile.config.extensions.oauth.OAuth2Client;
import fish.payara.microprofile.config.extensions.azure.model.Secret;
import fish.payara.microprofile.config.extensions.azure.model.SecretHolder;
import fish.payara.microprofile.config.extensions.azure.model.SecretsResponse;
import fish.payara.nucleus.microprofile.config.source.extension.ConfiguredExtensionConfigSource;
import fish.payara.nucleus.microprofile.config.spi.MicroprofileConfigConfiguration;
import java.io.File;
import java.nio.file.Files;
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
import java.util.stream.Stream;
import jakarta.inject.Inject;
import jakarta.json.JsonObject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.xml.bind.DatatypeConverter;
import org.glassfish.api.admin.ServerEnvironment;
import org.jvnet.hk2.annotations.Service;

@Service(name = "azure-secrets-config-source")
public class AzureSecretsConfigSource extends ConfiguredExtensionConfigSource<AzureSecretsConfigSourceConfiguration> {

    private static final Logger LOGGER = Logger.getLogger(AzureSecretsConfigSource.class.getName());

    private OAuth2Client authClient;
    private Client client = ClientBuilder.newClient();
    private static final String AUTH_URL = "https://login.microsoftonline.com/%s/oauth2/v2.0/token";
    private static final String SCOPE_URL = "https://vault.azure.net/.default";
    private static final String SECRETS_ENDPOINT = "https://%s.vault.azure.net/secrets";
    private static final String API_VERSION = "?api-version=7.1";

    @Inject
    private ServerEnvironment env;
    
    @Inject
    MicroprofileConfigConfiguration mpconfig;

    @Override
    public void bootstrap() {
        StringBuilder contentBuilder = new StringBuilder();
        try {
            final File tokenFile = getPrivateKeyFile();
            if (tokenFile == null) {
                LOGGER.warning("Couldn't find private key file, make sure it's configured.");
            } else {
                try (Stream<String> stream = Files.lines(tokenFile.toPath())) {
                    stream.forEach(s -> contentBuilder.append(s));
                }
            }
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Couldn't find or read the private key file, make sure it exists.", ex);
        }

        Map<String, String> data = new HashMap<>();
        String tenantId = configuration.getTenantId();
        String clientId = configuration.getClientId();
        if (tenantId == null || clientId == null) {
            LOGGER.warning("An error occurred while authenticating Azure to get a token, makes sure Azure Config Source has been configured with correct  configuration options.");
        } else {
            data.put("grant_type", "client_credentials");
            data.put("client_id", clientId);
            data.put("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
            data.put("scope", SCOPE_URL);

            try {
                final SignedJWT jwt = buildJwt(clientId, String.format(AUTH_URL, tenantId), configuration.getThumbprint());
                jwt.sign(new RSASSASigner(parsePrivateKey(contentBuilder.toString())));
                data.put("client_assertion", jwt.serialize());
            } catch (NoSuchAlgorithmException | InvalidKeySpecException | JOSEException e) {
                LOGGER.log(Level.WARNING, "An error occurred while signing the Azure auth token", e);
            }
            this.authClient = new OAuth2Client(String.format(AUTH_URL, tenantId), data);
        }
    }

    @Override
    public Map<String, String> getProperties() {
        Map<String, String> results = new HashMap<>();

        final String accessToken = authenticate();

        if (accessToken == null) {
            return results;
        }

        final WebTarget secretsTarget = client
                .target(String.format(SECRETS_ENDPOINT, configuration.getKeyVaultName()) + API_VERSION);

        final Response secretsResponse = secretsTarget
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
                .get();

        if (secretsResponse.getStatus() != 200) {
            return results;
        }

        final List<Secret> secrets = secretsResponse
                .readEntity(SecretsResponse.class)
                .getValue();

        for (Secret secret : secrets) {
            final String secretName = secret.getId().replace(String.format(SECRETS_ENDPOINT, configuration.getKeyVaultName()) + "/", "");
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
                .target(String.format(SECRETS_ENDPOINT, configuration.getKeyVaultName()) + "/" + propertyName + API_VERSION);

        final Response secretResponse = secretTarget
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
                .get();

        final int status = secretResponse.getStatus();
        if (status != 200) {
            if (status != 400) {
                LOGGER.log(Level.WARNING, "Failed to get Azure secret. {0}", secretResponse.readEntity(String.class));
            }
            return null;
        }

        final String value = secretResponse
                .readEntity(Secret.class)
                .getValue();

        return value;
    }

    @Override
    public boolean setValue(String secretName, String secretValue) {
        final String accessToken = authenticate();

        if (accessToken == null) {
            return false;
        }

        final WebTarget setSecretTarget = client
                .target(String.format(SECRETS_ENDPOINT, configuration.getKeyVaultName()) + "/" + secretName + API_VERSION);

        final Response setSecretResponse = setSecretTarget
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
                .put(Entity.entity(new SecretHolder(secretValue), MediaType.APPLICATION_JSON));

        if (setSecretResponse.getStatus() == 200) {
            return true;
        }
        LOGGER.log(Level.WARNING, "Failed to set Azure secret. {0}", setSecretResponse.readEntity(String.class));
        return false;
    }

    @Override
    public boolean deleteValue(String secretName) {
        final String accessToken = authenticate();

        if (accessToken == null) {
            return false;
        }

        final WebTarget secretTarget = client
                .target(String.format(SECRETS_ENDPOINT, configuration.getKeyVaultName()) + "/" + secretName + API_VERSION);

        final Response secretResponse = secretTarget
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
                .delete();

        if (secretResponse.getStatus() == 200) {
            return true;
        }
        LOGGER.log(Level.WARNING, "Failed to delete Azure secret. {0}", secretResponse.readEntity(String.class));
        return false;
    }

    @Override
    public String getSource() {
        return "cloud";
    }

    @Override
    public String getName() {
        return "azure";
    }
    
    @Override
    public int getOrdinal() {
        return Integer.parseInt(mpconfig.getCloudOrdinality());
    }

    private File getPrivateKeyFile() {
        final String fileName = configuration.getPrivateKeyFilePath();
        if (fileName != null) {
            return env.getConfigDirPath().toPath().resolve(configuration.getPrivateKeyFilePath()).toFile();
        }
        return null;
    }

    private static SignedJWT buildJwt(final String issuer, final String audience, final String thumbprint) {
        Instant now = Instant.now();
        Instant expiry = now.plus(1, ChronoUnit.MINUTES);
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(issuer)
                .audience(audience)
                .expirationTime(Date.from(expiry))
                .issueTime(Date.from(now))
                .issuer(issuer)
                .build();

        byte[] bytes = DatatypeConverter.parseHexBinary(thumbprint);

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .type(JOSEObjectType.JWT)
                .x509CertThumbprint(Base64URL.encode(bytes))
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

    private String authenticate() {
        if (authClient == null) {
            LOGGER.log(Level.WARNING, "Couldn't authenticate with Azure. Check your configuration options are correct.");
        } else {
            final Response response = authClient.authenticate();
            final int status = response.getStatus();
            if (status == 200) {
                JsonObject data = response.readEntity(JsonObject.class);
                Integer expirySeconds = data.getInt("expires_in");
                authClient.expire(Duration.ofSeconds(expirySeconds));
                return data.getString("access_token");
            } else if (status == 400) {
                LOGGER.log(Level.WARNING, "Couldn't authenticate with Azure. Check your configuration options are correct.");
            }
        }
        return null;
    }

}

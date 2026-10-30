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
package fish.payara.microprofile.config.extensions.hashicorp;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonException;
import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonParser.Event;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.glassfish.config.support.TranslatedConfigView;
import org.jvnet.hk2.annotations.Service;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import fish.payara.microprofile.config.extensions.hashicorp.model.SecretHolder;
import fish.payara.nucleus.microprofile.config.source.extension.ConfiguredExtensionConfigSource;
import fish.payara.nucleus.microprofile.config.spi.MicroprofileConfigConfiguration;

@Service(name = "hashicorp-secrets-config-source")
public class HashiCorpSecretsConfigSource extends ConfiguredExtensionConfigSource<HashiCorpSecretsConfigSourceConfiguration> {

    private static final Logger LOGGER = Logger.getLogger(HashiCorpSecretsConfigSource.class.getName());
    
    @Inject
    MicroprofileConfigConfiguration mpconfig;

    private Client client = ClientBuilder.newClient();
    protected String hashiCorpVaultToken;
    protected String vaultAddress;
    protected String secretsEnginePath;
    protected String secretsPath;
    protected int apiVersion;

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void bootstrap() {
        try {
            // Get the HashiCorp Vault token.
            hashiCorpVaultToken = TranslatedConfigView.getRealPasswordFromAlias("${ALIAS=HASHICORP_VAULT_TOKEN}");
            vaultAddress = removeForwardSlashFromSuffixAndPrefix(configuration.getVaultAddress());
            secretsEnginePath = removeForwardSlashFromSuffixAndPrefix(configuration.getSecretsEnginePath());
            secretsPath = removeForwardSlashFromSuffixAndPrefix(configuration.getSecretsPath());
            apiVersion = Integer.parseInt(configuration.getApiVersion());
        } catch (KeyStoreException | CertificateException | IOException | NoSuchAlgorithmException | UnrecoverableKeyException ex) {
            LOGGER.log(Level.WARNING, "Unable to get value from password aliases", ex);

        }
    }

    @Override
    public Map<String, String> getProperties() {
        Map<String, String> results = new HashMap<>();

        if (hashiCorpVaultToken == null) {
            printMisconfigurationMessage();
            return results;
        }

        //Use version 2 of API by default
        String secretsURL = vaultAddress + "/v1/" + secretsEnginePath + "/data/" + secretsPath;

        if (apiVersion == 1) {
            secretsURL = vaultAddress + "/v1/" + secretsEnginePath + "/" + secretsPath;
        }

        final WebTarget secretsTarget = client.target(secretsURL);
        try {
            final Response secretsResponse = secretsTarget
                    .request()
                    .accept(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + hashiCorpVaultToken)
                    .get();

            if (secretsResponse.getStatus() != 200) {
                LOGGER.log(Level.WARNING, "Unable to get secrets from the vault using the following URL: " + secretsURL
                        + ". Make sure all the configurtaion options has been entered correctly and HashiCorp Vault Token is correct");
                return results;
            }

            final String secretString = readSecretString((InputStream) secretsResponse.getEntity());

            try (final StringReader reader = new StringReader(secretString)) {
                return readMap(reader);
            }
        } catch (ProcessingException | JsonException | IOException ex) {
            LOGGER.log(Level.WARNING, "Unable to read secret value", ex);
        }

        return results;
    }

    @Override
    public Set<String> getPropertyNames() {
        return getProperties().keySet();
    }

    @Override
    public String getValue(String propertyName) {
        if (hashiCorpVaultToken == null) {
            printMisconfigurationMessage();
            return null;
        }
        return getProperties().get(propertyName);
    }

    @Override
    public boolean setValue(String secretName, String secretValue) {
        if (hashiCorpVaultToken == null) {
            printMisconfigurationMessage();
            return false;
        }

        Map<String, String> properties = getProperties();
        properties.put(secretName, secretValue);
        return modifySecret(properties);
    }
    
    private boolean modifySecret(Map<String, String> properties) {
        //Use version 2 of API by default
        String secretsURL = vaultAddress + "/v1/" + secretsEnginePath + "/data/" + secretsPath;

        if (apiVersion == 1) {
            secretsURL = vaultAddress + "/v1/" + secretsEnginePath + "/" + secretsPath;
        }

        final WebTarget target = client
                .target(secretsURL);

        Object payload;
        if (apiVersion == 1) {
            Map<String, Object> secrets = new HashMap<>(properties);
            payload = Json.createObjectBuilder(secrets).build().toString();
        } else {
            payload = new SecretHolder(properties);
        }

        final Response setSecretResponse = target
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + hashiCorpVaultToken)
                .put(Entity.entity(payload, MediaType.APPLICATION_JSON));

        if (setSecretResponse.getStatus() == 200) {
            return true;
        }
        LOGGER.log(Level.WARNING, "Failed to modify HashiCorp secret. {0}", setSecretResponse.readEntity(String.class));
        return false;
    }

    @Override
    public boolean deleteValue(String secretName) {
        if (hashiCorpVaultToken == null) {
            printMisconfigurationMessage();
            return false;
        }

        Map<String, String> properties = getProperties();
        properties.remove(secretName);
        return modifySecret(properties);
    }

    private String readSecretString(InputStream input) {
        try (JsonParser parser = Json.createParser(input)) {
            while (parser.hasNext()) {
                JsonParser.Event parseEvent = parser.next();
                if (parseEvent == Event.KEY_NAME) {
                    final String keyName = parser.getString();

                    parser.next();
                    if ("data".equals(keyName)) {
                        if (apiVersion == 1) {
                            return parser.getObject().toString();
                        }
                        return parser.getObject().getJsonObject(keyName).toString();
                    }
                }
            }
        }
        return null;
    }

    private Map<String, String> readMap(Reader input) throws JsonParseException, JsonMappingException, IOException {
        return mapper.readValue(input, new TypeReference<Map<String, String>>() {
        });
    }

    @Override
    public String getSource() {
        return "cloud";
    }

    @Override
    public String getName() {
        return "hashicorp";
    }
    
    @Override
    public int getOrdinal() {
        return Integer.parseInt(mpconfig.getCloudOrdinality());
    }
    
    private static void printMisconfigurationMessage() {
        LOGGER.warning("HashiCorp Secrets Config Source isn't configured correctly. "
                + "Make sure that the password aliases HASHICORP_VAULT_TOKEN exist.");
    }

    private String removeForwardSlashFromSuffixAndPrefix(String path) {
        if (path.charAt(path.length() - 1) == '/') {
            path = path.substring(0, path.length() - 1);
        }

        if (path.charAt(0) == '/') {
            path = path.substring(1);
        }
        return path;
    }

}

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
package fish.payara.microprofile.config.extensions.aws;

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
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.json.Json;
import jakarta.json.JsonException;
import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonParser.Event;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.glassfish.config.support.TranslatedConfigView;
import org.jvnet.hk2.annotations.Service;

import fish.payara.microprofile.config.extensions.aws.client.AwsRequestBuilder;
import fish.payara.nucleus.microprofile.config.source.extension.ConfiguredExtensionConfigSource;
import fish.payara.nucleus.microprofile.config.spi.MicroprofileConfigConfiguration;
import jakarta.inject.Inject;

@Service(name = "aws-secrets-config-source")
public class AWSSecretsConfigSource extends ConfiguredExtensionConfigSource<AWSSecretsConfigSourceConfiguration> {

    private static final Logger LOGGER = Logger.getLogger(AWSSecretsConfigSource.class.getName());

    private final ObjectMapper mapper = new ObjectMapper();

    private AwsRequestBuilder builder;
    
    @Inject
    MicroprofileConfigConfiguration mpconfig;

    @Override
    public void bootstrap() {
        try {
            // Find the access keys. Throws IllegalArgumentException if not found
            final String accessKeyId = TranslatedConfigView.getRealPasswordFromAlias("${ALIAS=AWS_ACCESS_KEY_ID}");
            final String secretAccessKey = TranslatedConfigView.getRealPasswordFromAlias("${ALIAS=AWS_SECRET_ACCESS_KEY}");

            this.builder = AwsRequestBuilder.builder(accessKeyId, secretAccessKey)
                    .region(configuration.getRegionName())
                    .serviceName("secretsmanager")
                    .version("2017-10-17")
                    .ContentType("application/x-amz-json-1.1")
                    .method(HttpMethod.POST)
                    .data(Json.createObjectBuilder().add("SecretId", configuration.getSecretName()).build());

        } catch (IllegalArgumentException ex) {
            printMisconfigurationMessage();
        } catch (UnrecoverableKeyException | KeyStoreException | CertificateException | NoSuchAlgorithmException
                | IOException ex) {
            LOGGER.log(Level.WARNING, "Unable to get value from password aliases", ex);
        }
    }

    @Override
    public void destroy() {
        this.builder = null;
    }

    @Override
    public Map<String, String> getProperties() {
        if (builder == null) {
            printMisconfigurationMessage();
            return new HashMap<>();
        }
        final Response response = builder
                .action("GetSecretValue")
                .build()
                .invoke();

        if (response.getStatus() != 200) {
            LOGGER.log(Level.WARNING, "Failed to get AWS secret. {0}", response.readEntity(String.class));
        } else {
            try {
                final String secretString = readSecretString((InputStream) response.getEntity());

                try (final StringReader reader = new StringReader(secretString)) {
                    return readMap(reader);
                }
            } catch (ProcessingException | JsonException | IOException ex) {
                LOGGER.log(Level.WARNING, "Unable to read secret value", ex);
            }
        }
        return new HashMap<>();
    }

    @Override
    public Set<String> getPropertyNames() {
        return getProperties().keySet();
    }

    @Override
    public String getValue(String propertyName) {
        if (builder == null) {
            printMisconfigurationMessage();
            return null;
        }
        return getProperties().get(propertyName);
    }

    @Override
    public boolean deleteValue(String value) {
        return modifySecret(HttpMethod.DELETE, value, null);
    }

    @Override
    public boolean setValue(String key, String value) {
        return modifySecret(HttpMethod.POST, key, value);
    }

    @Override
    public String getSource() {
        return "cloud";
    }

    @Override
    public String getName() {
        return "aws";
    }
    
    @Override
    public int getOrdinal() {
        return Integer.parseInt(mpconfig.getCloudOrdinality());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean modifySecret(String method, String key, String value) {
        if (builder == null) {
            printMisconfigurationMessage();
            return false;
        }

        Map<String, Object> properties = (Map) getProperties();
        switch (method) {
            case HttpMethod.POST:
                properties.put(key, value);
                break;
            case HttpMethod.DELETE:
                if (properties.remove(key) == null) {
                    return false;
                }
                break;
            default:
                throw new IllegalArgumentException("Unsupported HTTP method");
        }

        final Response response = builder
                .action("UpdateSecret")
                .data(Json.createObjectBuilder()
                    .add("ClientRequestToken", UUID.randomUUID().toString())
                    .add("SecretId", configuration.getSecretName())
                    .add("SecretString", Json.createObjectBuilder(properties).build().toString())
                    .build())
                .build()
                .invoke();
        
        if (response.getStatus() != 200) {
            LOGGER.log(Level.WARNING, "Failed to modify AWS secret. {0}", response.readEntity(String.class));
            return false;
        }
        return true;
    }

    private static String readSecretString(InputStream input) {
        try (JsonParser parser = Json.createParser(input)) {
            while (parser.hasNext()) {
                JsonParser.Event parseEvent = parser.next();
                if (parseEvent == Event.KEY_NAME) {
                    final String keyName = parser.getString();

                    parser.next();
                    if ("SecretString".equals(keyName)) {
                        return parser.getString();
                    }
                }
            }
        }
        return null;
    }

    private Map<String, String> readMap(Reader input) throws JsonParseException, JsonMappingException, IOException {
        return mapper.readValue(input, new TypeReference<Map<String, String>>() {});
    }

    private static void printMisconfigurationMessage() {
        LOGGER.warning("AWS Secrets Config Source isn't configured correctly. "
                + "Make sure that the password aliases AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY exist.");
    }

}
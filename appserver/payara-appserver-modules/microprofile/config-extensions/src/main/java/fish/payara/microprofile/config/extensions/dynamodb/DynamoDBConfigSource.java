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
package fish.payara.microprofile.config.extensions.dynamodb;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.json.stream.JsonParser;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response;

import org.glassfish.config.support.TranslatedConfigView;
import org.jvnet.hk2.annotations.Service;

import fish.payara.microprofile.config.extensions.aws.client.AwsRequestBuilder;
import fish.payara.nucleus.microprofile.config.source.extension.ConfiguredExtensionConfigSource;
import fish.payara.nucleus.microprofile.config.spi.MicroprofileConfigConfiguration;
import jakarta.inject.Inject;

@Service(name = "dynamodb-config-source")
public class DynamoDBConfigSource extends ConfiguredExtensionConfigSource<DynamoDBConfigSourceConfiguration> {

    public static final Set<String> SUPPORTED_DATA_TYPES = new HashSet<>(Arrays.asList("S", "N", "B", "BOOL", "NULL"));
    private static final Logger LOGGER = Logger.getLogger(DynamoDBConfigSource.class.getName());
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
                    .serviceName("DynamoDB")
                    .method(HttpMethod.POST)
                    .ContentType("application/json")
                    .data(Json.createObjectBuilder()
                            .add("TableName", configuration.getTableName())
                            .add("ProjectionExpression", configuration.getKeyColumnName() + "," + configuration.getValueColumnName())
                            .add("Limit", Integer.parseInt(configuration.getLimit()))
                            .build());

        } catch (IllegalArgumentException ex) {
            printMisconfigurationMessage();
        } catch (UnrecoverableKeyException | KeyStoreException | CertificateException | NoSuchAlgorithmException
                | IOException ex) {
            LOGGER.log(Level.WARNING, "Unable to get value from password aliases", ex);
        }
    }

    @Override
    public Map<String, String> getProperties() {
        if (builder == null) {
            printMisconfigurationMessage();
            return new HashMap<>();
        }
        final Response response = builder
                .action("Scan")
                .build()
                .invoke();

        if (response.getStatus() != 200) {
            LOGGER.log(Level.WARNING, "Failed to get data from DynamoDB. {0}", response.readEntity(String.class));
        } else {
            try {
                final JsonArray items = readItems((InputStream) response.getEntity());
                return readDataFromItems(items, configuration.getKeyColumnName(), configuration.getValueColumnName());
            } catch (ProcessingException | JsonException ex) {
                LOGGER.log(Level.WARNING, "Failed to read the data retrived from your DynamoDB", ex);
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

    public static Map<String, String> readDataFromItems(JsonArray items, String keyColumnName, String valueColumnName) {
        Map<String, String> results = new HashMap<>();

        for (JsonValue itemsJsonValue : items) {
            JsonObject keyColumn = itemsJsonValue.asJsonObject().getJsonObject(keyColumnName);
            if (keyColumn != null) {
                boolean isKeyColumnValueValid = true;
                String keyColumnValue = "";
                for (String keyFieldName : keyColumn.keySet()) {
                    if (SUPPORTED_DATA_TYPES.contains(keyFieldName)) {
                        keyColumnValue = keyColumn.get(keyFieldName).toString();
                    } else {
                        isKeyColumnValueValid = false;
                        printDataTypeNotSupportMessage();
                        break;
                    }
                }

                if (isKeyColumnValueValid) {
                    JsonObject valueColumn = itemsJsonValue.asJsonObject().getJsonObject(valueColumnName);
                    String valueColumnValue = "";
                    for (String valueFieldName : valueColumn.keySet()) {
                        if (SUPPORTED_DATA_TYPES.contains(valueFieldName)) {
                            valueColumnValue = valueColumn.get(valueFieldName).toString();
                        } else {
                            printDataTypeNotSupportMessage();
                            break;
                        }
                    }
                    results.put(keyColumnValue.replaceAll("^\"|\"$", ""), valueColumnValue.replaceAll("^\"|\"$", ""));
                }
            }
        }
        return results;
    }

    private static JsonArray readItems(InputStream input) {
        try (JsonParser parser = Json.createParser(input)) {
            while (parser.hasNext()) {
                JsonParser.Event parseEvent = parser.next();
                if (parseEvent == JsonParser.Event.KEY_NAME) {
                    final String keyName = parser.getString();
                    parser.next();
                    if ("Items".equals(keyName)) {
                        return parser.getArray();
                    }
                }
            }
        }
        return null;
    }

    @Override
    public String getSource() {
        return "cloud";
    }

    @Override
    public String getName() {
        return "dynamodb";
    }
    
    @Override
    public int getOrdinal() {
        return Integer.parseInt(mpconfig.getCloudOrdinality());
    }

    @Override
    public void destroy() {
        this.builder = null;
    }

    @Override
    public boolean setValue(String name, String value) {
        LOGGER.warning("DynamoDB Config source currently doesn't support setting config property");
        return false;
    }

    @Override
    public boolean deleteValue(String name) {
        LOGGER.warning("DynamoDB Config source currently doesn't support deleting config property");
        return false;
    }

    private static void printMisconfigurationMessage() {
        LOGGER.warning("DynamoDB Config Source isn't configured correctly. "
                + "Make sure that the password aliases AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY exist.");
    }

    private static void printDataTypeNotSupportMessage() {
        LOGGER.warning("The column you have configured with DynamoDB Config source has attributes that use a data type that is not currently supported. "
                + "Only the following types are supported: String, Binary, Boolean, Number and Null.");
    }
}

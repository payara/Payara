/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2017] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/master/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 * 
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at glassfish/legal/LICENSE.txt.
 * 
 *     GPL Classpath Exception:
 *     The Payara Foundation designates this particular file as subject to the "Classpath"
 *     exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *     file that accompanied this code.
 * 
 *     Modifications:
 *     If applicable, add the following below the License Header, with the fields
 *     enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyright [year] [name of copyright owner]"
 * 
 *     Contributor(s):
 *     If you wish your version of this file to be governed by only the CDDL or
 *     only the GPL Version 2, indicate your decision by adding "[Contributor]
 *     elects to include this software in this distribution under the [CDDL or GPL
 *     Version 2] license."  If you don't indicate a single choice of license, a
 *     recipient has the option to distribute your version of this file under
 *     either the CDDL, the GPL Version 2 or to extend the choice of license to
 *     its licensees as provided above.  However, if you add GPL Version 2 code
 *     and therefore, elected the GPL Version 2 license, then the option applies
 *     only if the new code is made subject to such option by the copyright
 *     holder.
 */

package fish.payara.microprofile.metrics.writer;

import fish.payara.microprofile.metrics.Constants;
import fish.payara.microprofile.metrics.MetricsHelper;
import fish.payara.microprofile.metrics.exception.NoSuchMetricException;
import fish.payara.microprofile.metrics.exception.NoSuchRegistryException;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriterFactory;

public abstract class JsonWriter implements OutputWriter {

    private final Writer writer;
    protected MetricsHelper helper;
    
    protected static final Logger LOGGER = Logger.getLogger(JsonWriter.class.getName());
    
    public JsonWriter(Writer writer, MetricsHelper helper) {
        this.writer = writer;
        this.helper = helper;
    }

    protected abstract JsonObject getJsonData(String registryName) throws NoSuchRegistryException;

    protected abstract JsonObject getJsonData(String registryName, String metricName) throws NoSuchRegistryException, NoSuchMetricException;

    @Override
    public void write(String registryName, String metricName) throws NoSuchRegistryException, NoSuchMetricException, IOException {
        serialize(getJsonData(registryName, metricName));
    }

    @Override
    public void write(String registryName) throws NoSuchRegistryException, IOException {
        serialize(getJsonData(registryName));
    }

    @Override
    public void write() throws IOException {
        JsonObjectBuilder payloadBuilder = Json.createObjectBuilder();
        for (String registryName : Constants.REGISTRY_NAMES_LIST) {
            try {
                payloadBuilder.add(registryName, getJsonData(registryName));
            } catch (NoSuchRegistryException e) { // Ignore
            }
        }
        serialize(payloadBuilder.build());
    }

    protected void serialize(JsonObject payload) throws IOException {
        JsonWriterFactory jsonWriterFactory = Json.createWriterFactory(null);
        jsonWriterFactory.createWriter(writer).writeObject(payload);
    }

    protected JsonObject getJsonFromMap(Map<String, Number> map) {
        JsonObjectBuilder payloadBuilder = Json.createObjectBuilder();
        for (Map.Entry<String, Number> entry : map.entrySet()) {
            payloadBuilder.add(entry.getKey(), entry.getValue().toString());
        }
        return payloadBuilder.build();
    }

    protected String getPairFromMap(Map<String, String> map) {
        if (map == null) {
            return null;
        }
        StringBuilder tagList = new StringBuilder();
        String delimiter = "";
        for (Map.Entry<String, String> entry : map.entrySet()) {
            tagList.append(delimiter).append(entry.getKey()).append('=').append(entry.getValue());
            delimiter = ",";
        }
        return tagList.toString();
    }

}

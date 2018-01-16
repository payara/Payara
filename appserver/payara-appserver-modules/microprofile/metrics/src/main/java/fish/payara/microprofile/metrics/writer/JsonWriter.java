/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2018] Payara Foundation and/or its affiliates. All rights reserved.
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

import fish.payara.microprofile.metrics.MetricsService;
import fish.payara.microprofile.metrics.exception.NoSuchMetricException;
import fish.payara.microprofile.metrics.exception.NoSuchRegistryException;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.logging.Logger;
import static java.util.stream.Collectors.joining;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriterFactory;
import static org.eclipse.microprofile.metrics.MetricRegistry.Type.APPLICATION;
import static org.eclipse.microprofile.metrics.MetricRegistry.Type.BASE;
import static org.eclipse.microprofile.metrics.MetricRegistry.Type.VENDOR;
import org.glassfish.internal.api.Globals;

public abstract class JsonWriter implements MetricsWriter {

    private final Writer writer;
    
    protected final MetricsService service;

    protected static final Logger LOGGER = Logger.getLogger(JsonWriter.class.getName());

    public JsonWriter(Writer writer) {
        this.writer = writer;
        this.service = Globals.getDefaultBaseServiceLocator().getService(MetricsService.class);
    }

    protected abstract JsonObjectBuilder getJsonData(String registryName) throws NoSuchRegistryException;

    protected abstract JsonObjectBuilder getJsonData(String registryName, String metricName) throws NoSuchRegistryException, NoSuchMetricException;

    @Override
    public void write(String registryName, String metricName) throws NoSuchRegistryException, NoSuchMetricException, IOException {
        if (APPLICATION.getName().equals(registryName)) {
            JsonObjectBuilder payloadBuilder = Json.createObjectBuilder();
            for (String appRegistryName : service.getApplicationRegistryNames()) {
                try {
                getJsonData(appRegistryName, metricName).build()
                        .entrySet()
                        .forEach(entry -> payloadBuilder.add(entry.getKey(), entry.getValue()));
                } catch (NoSuchMetricException e) {
                    //ignore
                }
            }
            JsonObject payload = payloadBuilder.build();
            if(payload.isEmpty()){
                throw new NoSuchMetricException(metricName);
            } else {
                serialize(payload);
            }
        } else {
            serialize(getJsonData(registryName, metricName).build());
        }
    }

    @Override
    public void write(String registryName) throws NoSuchRegistryException, IOException {
        if (APPLICATION.getName().equals(registryName)) {
            JsonObjectBuilder payloadBuilder = Json.createObjectBuilder();
            for (String appRegistryName : service.getApplicationRegistryNames()) {
                getJsonData(appRegistryName).build()
                        .entrySet()
                        .forEach(entry -> payloadBuilder.add(entry.getKey(), entry.getValue()));
            }
            serialize(payloadBuilder.build());
        } else {
            serialize(getJsonData(registryName).build());
        }
    }

    @Override
    public void write() throws IOException {
        JsonObjectBuilder payloadBuilder = Json.createObjectBuilder();
        JsonObjectBuilder applicationBuilder = Json.createObjectBuilder();
        for (String registryName : service.getAllRegistryNames()) {
            try {
                JsonObjectBuilder value = getJsonData(registryName);
                if (!BASE.getName().equals(registryName)
                        && !VENDOR.getName().equals(registryName)) {
                    value.build()
                            .entrySet()
                            .forEach(entry -> applicationBuilder.add(entry.getKey(), entry.getValue()));
                } else {
                    payloadBuilder.add(registryName, value.build());
                }
            } catch (NoSuchRegistryException e) { // Ignore
            }
        }
        JsonObject applicationObject = applicationBuilder.build();
        if (!applicationObject.isEmpty()) {
            payloadBuilder.add(APPLICATION.getName(), applicationObject);
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
            addValueToJsonObject(payloadBuilder, entry.getKey(), entry.getValue()); 
        }
        return payloadBuilder.build();
    }
    
    protected void addValueToJsonObject(JsonObjectBuilder payloadBuilder, String key, Number value){
        if (value instanceof Integer) {
            payloadBuilder.add(key, value.intValue());
        } else if (value instanceof Long) {
            payloadBuilder.add(key, value.longValue());
        } else if (value instanceof Double) {
            payloadBuilder.add(key, value.doubleValue());
        } else {
            payloadBuilder.add(key, value.toString());
        }
    }

    protected String getPairFromMap(Map<String, String> map) {
        if (map == null) {
            return null;
        }
        return map.entrySet()
                .stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(joining(","));
    }

}

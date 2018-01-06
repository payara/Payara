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

import fish.payara.microprofile.metrics.MetricsHelper;
import fish.payara.microprofile.metrics.exception.NoSuchMetricException;
import fish.payara.microprofile.metrics.exception.NoSuchRegistryException;
import java.io.Writer;
import java.util.Map;
import java.util.Map.Entry;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import org.eclipse.microprofile.metrics.Metadata;

public class JsonMetadataWriter extends JsonWriter {
    
    public JsonMetadataWriter(Writer writer, MetricsHelper helper) {
        super(writer, helper);
    }

    @Override
    protected JsonObject getJsonData(String registryName) throws NoSuchRegistryException {
        return getJsonFromMetricMetadataMap(helper.getMetadataAsMap(registryName));
    }

    @Override
    protected JsonObject getJsonData(String registryName, String metric) throws NoSuchRegistryException, NoSuchMetricException {
        return getJsonFromMetricMetadataMap(helper.getMetadataAsMap(registryName, metric));
    }

    private JsonObject getJsonFromMetricMetadataMap(Map<String, Metadata> metadataMap) {
        JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        for (Entry<String, Metadata> entry : metadataMap.entrySet()) {
            jsonObjectBuilder.add(entry.getKey(), getJsonFromObject(entry.getValue()));
        }
        return jsonObjectBuilder.build();
    }

    private JsonObject getJsonFromObject(Metadata metadata) {
        JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        jsonObjectBuilder.add("name", sanitizeMetadata(metadata.getName()));
        jsonObjectBuilder.add("displayName", sanitizeMetadata(metadata.getDisplayName()));
        jsonObjectBuilder.add("description",sanitizeMetadata(metadata.getDescription()));
        jsonObjectBuilder.add("type", sanitizeMetadata(metadata.getType()));
        jsonObjectBuilder.add("unit", sanitizeMetadata(metadata.getUnit()));
        jsonObjectBuilder.add("tags", getPairFromMap(metadata.getTags()));

        return jsonObjectBuilder.build();
    }

    private String sanitizeMetadata(String s) {
        if (s == null || s.trim().isEmpty()) {
            return "";
        } else {
            return s;
        }
    }

}

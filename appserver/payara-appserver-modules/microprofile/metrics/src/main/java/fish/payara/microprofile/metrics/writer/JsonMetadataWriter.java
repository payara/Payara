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
 *
 * *****************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */

package fish.payara.microprofile.metrics.writer;

import static fish.payara.microprofile.metrics.Constants.EMPTY_STRING;
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
    
    private static final String NAME = "name";
    private static final String DISPLAY_NAME = "displayName";
    private static final String DESCRIPTION = "description";
    private static final String TYPE = "type";
    private static final String UNIT = "unit";
    private static final String TAGS = "tags";

    public JsonMetadataWriter(Writer writer) {
        super(writer);
    }

    @Override
    protected JsonObject getJsonData(String registryName) throws NoSuchRegistryException {
        return getJsonFromMetadata(service.getMetadataAsMap(registryName));
    }

    @Override
    protected JsonObject getJsonData(String registryName, String metric) throws NoSuchRegistryException, NoSuchMetricException {
        return getJsonFromMetadata(service.getMetadataAsMap(registryName, metric));
    }

    private JsonObject getJsonFromMetadata(Map<String, Metadata> metadataMap) {
        JsonObjectBuilder payloadBuilder = Json.createObjectBuilder();
        for (Entry<String, Metadata> entry : metadataMap.entrySet()) {
            payloadBuilder.add(entry.getKey(), getJsonFromMetadata(entry.getValue()));
        }
        return payloadBuilder.build();
    }  
    
    private JsonObject getJsonFromMetadata(Metadata metadata) {
        JsonObjectBuilder payloadBuilder = Json.createObjectBuilder();
        payloadBuilder.add(NAME, sanitizeMetadata(metadata.getName()));
        payloadBuilder.add(DISPLAY_NAME, sanitizeMetadata(metadata.getDisplayName()));
        payloadBuilder.add(DESCRIPTION, sanitizeMetadata(metadata.getDescription()));
        payloadBuilder.add(TYPE, sanitizeMetadata(metadata.getType()));
        payloadBuilder.add(UNIT, sanitizeMetadata(metadata.getUnit()));
        payloadBuilder.add(TAGS, getPairFromMap(metadata.getTags()));
        return payloadBuilder.build();
    }

    private String sanitizeMetadata(String s) {
        if (s == null || s.trim().isEmpty()) {
            return EMPTY_STRING;
        } else {
            return s;
        }
    }

}

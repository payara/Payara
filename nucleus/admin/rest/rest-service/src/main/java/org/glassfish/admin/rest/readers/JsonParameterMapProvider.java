/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
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
 *
 * Portions Copyright [2017-2021] Payara Foundation and/or affiliates
 */
package org.glassfish.admin.rest.readers;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.json.stream.JsonParser;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.Provider;
import org.glassfish.api.admin.ParameterMap;

/**
 * @author Ludovic Champenois
 */
@Consumes(MediaType.APPLICATION_JSON)
@Provider
public class JsonParameterMapProvider implements MessageBodyReader<ParameterMap> {

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type.equals(ParameterMap.class);
    }

    @Override
    public ParameterMap readFrom(Class<ParameterMap> type, Type genericType,
            Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> headers,
            InputStream in) throws IOException {
        JsonObject obj;
        try (JsonParser parser = Json.createParser(in)) {
            if (parser.hasNext()){
                parser.next();
                obj = parser.getObject();
            } else {
                obj = JsonValue.EMPTY_JSON_OBJECT;
            }

            ParameterMap map = new ParameterMap();
            for (Entry<String, JsonValue> entry : obj.entrySet()) {
                JsonValue value = entry.getValue();
                if (value instanceof JsonArray) {
                    JsonArray array = (JsonArray) value;
                    for (int i = 0; i < array.size(); i++) {
                        map.add(entry.getKey(), getStringValue(array.get(i)));
                    }
                } else {
                    map.add(entry.getKey(), getStringValue(value));
                }
            }
            return map;

        } catch (Exception ex) {
            Logger.getLogger("org.glassfish.admin.rest").log(Level.SEVERE, null, ex);
            ParameterMap map = new ParameterMap();
            map.add("error", "Entity Parsing Error: " + ex.getMessage());

            return map;
        }
    }

    /**
     * @return a string representation of the passed JsonValue.
     */
    private static String getStringValue(JsonValue json) {
        if (json == null) {
            return "";
        }
        if (json instanceof JsonString) {
            return ((JsonString)json).getString();
        }
        return json.toString();
    }

    public static String inputStreamAsString(InputStream stream) throws IOException {
        StringBuilder sb;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream))) {
            sb = new StringBuilder();
            String line = null;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }
}

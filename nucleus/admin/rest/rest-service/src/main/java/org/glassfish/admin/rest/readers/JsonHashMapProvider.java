/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2014 Oracle and/or its affiliates. All rights reserved.
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
 */
// Portions Copyright [2017-2021] Payara Foundation and/affiliates

package org.glassfish.admin.rest.readers;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.json.Json;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.json.JsonValue.ValueType;
import jakarta.json.stream.JsonParser;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.Provider;

/**
 * @author Ludovic Champenois
 */
@Consumes(MediaType.APPLICATION_JSON)
@Provider
public class JsonHashMapProvider implements MessageBodyReader<HashMap<String, String>> {

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type.equals(HashMap.class);
    }

    @Override
    public HashMap<String, String> readFrom(Class<HashMap<String, String>> type, Type genericType,
            Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> headers,
            InputStream in) throws IOException {
        HashMap map = new HashMap();
        try (JsonParser parser = Json.createParser(in)) {
            JsonObject obj;
            if (parser.hasNext()){
                parser.next();
                obj = parser.getObject();
            } else {
                obj = JsonValue.EMPTY_JSON_OBJECT;
            }

            for (Entry<String, JsonValue> entry : obj.entrySet()) {
                JsonValue value = entry.getValue();
                if (value.getValueType() == ValueType.STRING){
                    map.put(entry.getKey(), ((JsonString) value).getString());
                } else {
                    map.put(entry.getKey(), value.toString());
                }
            }
            return map;

        } catch (JsonException ex) {
            Logger.getLogger("org.glassfish.admin.rest.readers").log(Level.FINER, null, ex);
            return map;
        }
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

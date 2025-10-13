/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2017-2021] Payara Foundation and/or affiliates

package org.glassfish.admin.rest.client.utils;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonException;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.json.stream.JsonParser;
import org.glassfish.api.logging.LogHelper;

/**
 * Methods for processing JSON
 * @author jdlee
 * @author jcoustick
 * @see org.glassfish.admin.rest.utils.JsonUtil
 */
public class Util {

    /**
     * Converts a String of JSON data into a map
     * If there is an error then an empty Map will be returned.
     * @param json
     * @return A {@link Map} of {@link String} || {@link Number}
     */
    public static Map processJsonMap(String json) {
        Map<String, Object> map;
        try (JsonParser parser = Json.createParser(new StringReader(json))) {
            if (parser.hasNext()){
                parser.next();
                map = processJsonObject(parser.getObject());
            } else {
                map = new HashMap<>();
            }
        } catch (JsonException e) {
            map = new HashMap<>();
        }
        return map;
    }

    /**
     * Converts a {@link JsonObject} into a {@link Map}
     * <p>
     * The {@link Map} entries can be of types {@link Boolean}, {@link List},
     * {@link Map}, {@link Number}, {@code null} and/or {@link String}.
     * </p>
     * @param jsonObject
     * @return
     */
    public static Map<String, Object> processJsonObject(JsonObject jsonObject) {
        Map<String, Object> map = new HashMap<>();
        try {
            for (String key : jsonObject.keySet()) {
                 JsonValue value = jsonObject.get(key);
                switch (value.getValueType()) {
                    case ARRAY:
                        map.put(key, processJsonArray((JsonArray) value));
                        break;
                    case OBJECT:
                        map.put(key, processJsonObject((JsonObject) value));
                        break;
                    case NULL:
                        map.put(key, null);break;
                    case STRING:
                        map.put(key, ((JsonString) value).getString());
                        break;
                    case NUMBER:
                        map.put(key, ((JsonNumber) value).numberValue());
                        break;
                    case TRUE:
                        map.put(key, Boolean.TRUE);
                        break;
                    case FALSE:
                        map.put(key, Boolean.FALSE);
                        break;
                    default:
                        map.put(key, value);
                        break;
                }
            }
        } catch (JsonException e) {
            LogHelper.log(RestClientLogging.logger, Level.SEVERE, RestClientLogging.REST_CLIENT_JSON_ERROR, e);
        }

        return map;
    }

    /**
     * Converts a {@link JsonArray} into a {@link List}
     * <p>
     * The {@link List} entries can be of types {@link Boolean}, {@link List},
     * {@link Map}, {@link Number} and/or {@link String}.
     * </p>
     * @param jsonArray
     * @return
     */
    public static List processJsonArray(JsonArray jsonArray) {
        List results = new ArrayList();

        try {
            for (JsonValue entry: jsonArray) {
                if (null == entry.getValueType()) {
                    results.add(entry);
                } else switch (entry.getValueType()) {
                    case ARRAY:
                        results.add(processJsonArray((JsonArray) entry));break;
                    case OBJECT:
                        results.add(processJsonObject((JsonObject) entry));break;
                    case STRING:
                        results.add(((JsonString) entry).getString());break;
                    case NUMBER:
                        results.add(((JsonNumber) entry).numberValue());break;
                    case TRUE:
                        results.add(Boolean.TRUE);break;
                    case FALSE:
                        results.add(Boolean.FALSE);break;
                    default:
                        results.add(entry);break;
                }
            }
        } catch (JsonException e) {
            LogHelper.log(RestClientLogging.logger, Level.SEVERE, RestClientLogging.REST_CLIENT_JSON_ERROR, e);
        }
        return results;
    }

}

/*
 *
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) 2023 Payara Foundation and/or its affiliates. All rights reserved.
 *
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/main/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 *  GPL Classpath Exception:
 *  The Payara Foundation designates this particular file as subject to the "Classpath"
 *  exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *  file that accompanied this code.
 *
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 *
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 *
 */

package org.glassfish.admingui.common.util;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonException;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JsonUtil {
    private JsonUtil() {

    }

    /**
     * Converts an object to a JsonValue
     * <p>
     * The object must be one of {@link JsonValue}, {@link Collection}, {@link Map}, {@link String},
     * {@link Integer}, {@link Long}, {@link Double}, {@link Boolean}, {@link BigInteger}, {@link BigDecimal},
     * a class that has a REST model or an array of one of the above.
     *
     * @param object The object to convert
     * @return resulting JsonValue
     * @throws JsonException If the object cannot be converted to a JsonValue
     */
    public static JsonValue getJsonValue(Object object) throws JsonException {
        JsonValue result;
        if (object == null) {
            result = JsonValue.NULL;
        } else if (object instanceof Collection) {
            result = processCollection((Collection) object);
        } else if (object instanceof Map) {
            result = processMap((Map) object);
        } else if (object instanceof String) {
            result = Json.createValue((String) object);
        } else if (object instanceof Boolean) {
            Boolean value = (Boolean) object;
            if (value) {
                result = JsonValue.TRUE;
            } else {
                result = JsonValue.FALSE;
            }
        } else if (object instanceof Double) {
            result = Json.createValue((Double) object);
        } else if (object instanceof Integer) {
            result = Json.createValue((Integer) object);
        } else if (object instanceof Long) {
            result = Json.createValue((Long) object);
        } else if (object instanceof JsonValue) {
            result = (JsonValue) object;
        } else if (object instanceof BigInteger) {
            result = Json.createValue((BigInteger) object);
        } else if (object instanceof BigDecimal) {
            result = Json.createValue((BigDecimal) object);
        } else if (object.getClass().isEnum()) {
            result = Json.createValue(object.toString());
        } else if (object instanceof InetAddress) {
            result = Json.createValue(object.toString());
        } else {
            Class<?> clazz = object.getClass();
            if (clazz.isArray()) {
                JsonArrayBuilder array = Json.createArrayBuilder();
                final int length = Array.getLength(object);
                for (int i = 0; i < length; i++) {
                    array.add(getJsonValue(Array.get(object, i)));
                }
                result = array.build();
            } else {
                throw new JsonException("Unable to convert object to JsonValue: " + object);
            }
        }

        return result;
    }

    /**
     * Converts a {@link Collection} of {@link JsonValue}s or other Json-compatible types into a {@link JsonArray}
     *
     * @param c
     * @return
     * @throws JsonException
     */
    private static JsonArray processCollection(Collection c) throws JsonException {
        JsonArrayBuilder result = Json.createArrayBuilder();
        for (Object item : c) {
            result.add(getJsonValue(item));
        }
        return result.build();
    }

    private static JsonObject processMap(Map map) throws JsonException {
        JsonObjectBuilder result = Json.createObjectBuilder();

        for (Map.Entry entry : (Set<Map.Entry>) map.entrySet()) {
            result.add(entry.getKey().toString(), getJsonValue(entry.getValue()));
        }

        return result.build();
    }

    public static void appendUnicodeEscaped(char ch, StringBuilder builder) {
        if ((ch > 0x7e) || (ch < 0x20)) {
            builder.append("\\u");
            String chStr = Integer.toHexString(ch);
            int len = chStr.length();
            for (int idx = 4; idx > len; idx--) {
                // Add leading 0's
                builder.append('0');
            }
            builder.append(chStr);
        } else {
            builder.append(ch);
        }
    }

    /**
     * Converts a {@link JsonObject} to a {@link Map}&lt;{@link String}, {@link Object}&gt;
     * where the values of all of the basic type ({@link Integer}, {@link String} etc.), and not {@link JsonValue}.
     * @param object The JsonObject to convert to a Map
     * @return
     * @since 5.0
     * @see #jsonValueToRaw
     */
    public static Map<String, Object> jsonObjectToMap(JsonObject object){
        Map<String, Object> result = new HashMap<>();
        Set<String> keys =object.keySet();
        for (String key: keys) {
            result.put(key, jsonValueToRaw(object.get(key)));
        }
        return result;
    }

    /**
     * Converts a JsonValue to the java standard type it represents.
     * <p>
     * {@link JsonArray} -> {@link List}&lt;{@link Object}&gt; <br>
     * {@link JsonObject} -> {@link Map}&lt;{@link String}, {@link Object}&gt; <br>
     * {@link JsonString} -> {@link String} <br>
     * {@link JsonNumber} -> {@link BigDecimal} <br>
     * {@link JsonValue#TRUE} -> {@link Boolean#TRUE} <br>
     * {@link JsonValue#FALSE} -> {@link Boolean#FALSE} <br>
     * {@link JsonValue#NULL} -> {@code null}
     * </p>
     * @param value
     * @return The java standard type represented by the {@link JsonValue}
     * @since 5.0
     */
    private static Object jsonValueToRaw(JsonValue value){
        JsonValue.ValueType type = value.getValueType();
        switch (type) {
            case STRING:
                return ((JsonString) value).getString();
            case NUMBER:
                return ((JsonNumber) value).bigDecimalValue();
            case TRUE:
                return Boolean.TRUE;
            case FALSE:
                return Boolean.FALSE;
            case NULL:
                return null;
            case OBJECT:
                return jsonObjectToMap((JsonObject) value);
            case ARRAY:
                return jsonArraytoArray((JsonArray) value);
            default:
                throw new JsonException("JsonValue is not a recognised ValueType");
        }
    }

    /**
     * Converts a {@link JsonArray} to a {@link List}&lt;{@link Object}&gt;
     * where the objects are all the standard java types represented by the values
     * in the array
     * @param array The JsonArray to convert to {@link List}
     * @return
     * @since 5.0
     * @see #jsonValueToRaw
     */
    private static List<Object> jsonArraytoArray(JsonArray array){
        List<Object> result = new ArrayList<>();
        for (JsonValue instance : array){
            result.add(jsonValueToRaw(instance));
        }
        return result;
    }
}

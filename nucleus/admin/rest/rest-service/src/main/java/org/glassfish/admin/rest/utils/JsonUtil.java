/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admin.rest.utils;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import jakarta.json.*;
import jakarta.json.JsonValue.ValueType;
import org.glassfish.admin.rest.composite.RestModel;
import org.glassfish.admin.rest.composite.metadata.Confidential;
import org.glassfish.admin.rest.model.ResponseBody;

/**
 * Various utility methods for processing JSON.
 * 
 * @author jdlee
 * @author jcoustick
 */
public class JsonUtil {
    public static final String CONFIDENTIAL_PROPERTY_SET = "@_Oracle_Confidential_Property_Set_V1.1_#";
    public static final String CONFIDENTIAL_PROPERTY_UNSET = null;
    
    private JsonUtil() { /* to prevent instantiation */ }

    /**
     * Converts an object to a JsonValue
     * <p>
     * The object must be one of {@link JsonValue}, {@link Collection}, {@link Map}, {@link ResponseBody}, {@link String},
     * {@link Integer}, {@link Long}, {@link Double}, {@link Boolean}, {@link BigInteger}, {@link BigDecimal},
     * a class that has a REST model or an array of one of the above.
     * @param object The object to convert
     * @return The resulting JsonValue
     * @throws JsonException If the object cannot be converted to a JsonValue
     */
    public static JsonValue getJsonValue(Object object) throws JsonException{
       return getJsonValue(object, true);
    }
    
    /**
     * Converts an object to a JsonValue
     * <p>
     * The object must be one of {@link JsonValue}, {@link Collection}, {@link Map}, {@link ResponseBody}, {@link String},
     * {@link Integer}, {@link Long}, {@link Double}, {@link Boolean}, {@link BigInteger}, {@link BigDecimal},
     * a class that has a REST model or an array of one of the above.
     * @param object The object to convert
     * @param hideConfidentialProperties
     * @return resulting JsonValue
     * @throws JsonException If the object cannot be converted to a JsonValue
     */
    public static JsonValue getJsonValue(Object object, boolean hideConfidentialProperties) throws JsonException {
        JsonValue result;
        if (object == null){
            result = JsonValue.NULL;
        } else if (object instanceof Collection) {
            result = processCollection((Collection)object);
        } else if (object instanceof Map) {
            result = processMap((Map)object);
        } else if (RestModel.class.isAssignableFrom(object.getClass())) {
            result = getJsonForRestModel((RestModel)object, hideConfidentialProperties);
        } else if (object instanceof ResponseBody) {
            result = ((ResponseBody)object).toJson();
        } else if (object instanceof String){
            result = Json.createValue((String) object);
        }  else if (object instanceof Boolean){
            Boolean value = (Boolean) object;
            if (value){
                result = JsonValue.TRUE;
            } else {
                result = JsonValue.FALSE;
            }
        } else if (object instanceof Double){
            result = Json.createValue((Double) object);
        } else if (object instanceof Integer){
            result = Json.createValue((Integer) object);
        } else if (object instanceof Long){
            result = Json.createValue((Long) object);
        } else if (object instanceof JsonValue){
            result = (JsonValue) object;
        } else if (object instanceof BigInteger){
            result = Json.createValue((BigInteger) object);
        } else if (object instanceof BigDecimal){
            result = Json.createValue((BigDecimal) object);
        } else if (object.getClass().isEnum()){
            result = Json.createValue(object.toString());
        } else if (object instanceof InetAddress) {
            result = Json.createValue(object.toString());
        }else {
            Class<?> clazz = object.getClass();
            if (clazz.isArray()) {
                JsonArrayBuilder array = Json.createArrayBuilder();
                final int lenth = Array.getLength(object);
                for (int i = 0; i < lenth; i++) {
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
     * Converts an object to a JsonValue
     * <p>
     * The object must be one of {@link JsonValue}, {@link Collection}, {@link Map}, {@link ResponseBody}, {@link String},
     * {@link Integer}, {@link Long}, {@link Double}, {@link Boolean}, {@link BigInteger}, {@link BigDecimal},
     * a class that has a REST model or an array of one of the above.
     * @param object The object to convert
     * @param hideConfidentialProperties
     * @return
     * @throws JsonException If the object cannot be converted to a JsonValue
     * @deprecated As of 5.0, replaced by {@link #getJsonValue(Object)} as a more accurately named method
     * with the removal of Jettison the return value is no longer JSONObject but JsonValue.
     */
    @Deprecated
    public static JsonValue getJsonObject(Object object, boolean hideConfidentialProperties) throws JsonException {
        return getJsonValue(object, hideConfidentialProperties);
    }

    public static JsonObject getJsonForRestModel(RestModel model, boolean hideConfidentialProperties) {
        JsonObjectBuilder result = Json.createObjectBuilder();
        for (Method m : model.getClass().getDeclaredMethods()) {
            if (m.getName().startsWith("get")) {
                String propName = m.getName().substring(3);
                propName = propName.substring(0,1).toLowerCase(Locale.getDefault()) + propName.substring(1);
                if (!model.isTrimmed() || model.isSet(propName)) { // TBD - remove once the conversion to the new REST style guide is completed
//              if (model.isSet(propName)) {
                    // Only include properties whose value has been set in the model
                    try {
                        result.add(propName, getJsonValue(getRestModelProperty(model, m, hideConfidentialProperties)));
                    } catch (Exception e) {
                    }
                }
            }
        }

        return result.build();
    }

    private static Object getRestModelProperty(RestModel model, Method method, boolean hideConfidentialProperties) throws Exception {
        Object object = method.invoke(model);
        if (hideConfidentialProperties && isConfidentialString(model, method)) {
            String str = (String)object;
            return (StringUtil.notEmpty(str)) ? CONFIDENTIAL_PROPERTY_SET : CONFIDENTIAL_PROPERTY_UNSET;
        } else {
            return object;
        }
    }

    private static boolean isConfidentialString(RestModel model, Method method) {
        if (!String.class.equals(method.getReturnType())) {
            return false;
        }
        // TBD - why aren't the annotations available from 'method'?
        return isConfidentialProperty(model.getClass(), method.getName());
    }

    public static boolean isConfidentialProperty(Class clazz, String getterMethodName) {
        // Try this class
        if (getConfidentialAnnotation(clazz, getterMethodName) != null) {
            return true;
        }
        // Try its interfaces
        for (Class<?> iface : clazz.getInterfaces()) {
            if (getConfidentialAnnotation(iface, getterMethodName) != null) {
                return true;
            }
        }
        return false;
    }

    private static Confidential getConfidentialAnnotation(Class clazz, String getterMethodName) {
        try {
            Method m = clazz.getDeclaredMethod(getterMethodName);
            return m.getAnnotation(Confidential.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Converts a {@link Collection} of {@link JsonValue}s or other Json-compatible types into a {@link JsonAray}
     * @param c
     * @return
     * @throws JsonException 
     */
    public static JsonArray processCollection(Collection c) throws JsonException {
        JsonArrayBuilder result = Json.createArrayBuilder();
        for (Object item: c){
            result.add(JsonUtil.getJsonValue(item));
        }
        return result.build();
    }

    public static JsonObject processMap(Map map) throws JsonException {
        JsonObjectBuilder result = Json.createObjectBuilder();

        for (Map.Entry entry : (Set<Map.Entry>)map.entrySet()) {
            result.add(entry.getKey().toString(), getJsonValue(entry.getValue()));
        }

        return result.build();
    }

    /**
     * Gets a String from a {@link JsonObject}
     * @param jsonObject
     * @param key
     * @param dflt returned if there is no mapping for the key
     * @return 
     */
    public static String getString(JsonObject jsonObject, String key, String dflt) {
        try {
            if (jsonObject.isNull(key)) {
                return null;
            }
            return jsonObject.getString(key, dflt);
        } catch (JsonException e) {
            return dflt;
        }
    }

    /**
     * Gets an integer from a {@link JsonObject}
     * @param jsonObject
     * @param key
     * @param dflt returned if there is no mapping for the key
     * @return 
     */
    public static int getInt(JsonObject jsonObject, String key, int dflt) {
        try {
            return jsonObject.getInt(key, dflt);
        } catch (JsonException e) {
            return dflt;
        }
    }
    
    /**
     * Puts a value into a {@link JsonObject} with the specified key. 
     * <p>
     * If the key already exists then a {@link JsonArray} is used to store all the values. This differs to {@link JsonObject#put(Object, Object)}
     * as put replaces the value if the key already exists.
     * @param jsonObject
     * @param key
     * @param value
     * @return 
     * @since 5.0
     */
    public static JsonObject accumalate(JsonObject jsonObject, String key, JsonValue value){
        JsonObjectBuilder jsonBuilder = Json.createObjectBuilder(jsonObject);
        if (jsonObject.containsKey(key)){
            JsonValue previous = jsonObject.get(key);
            if (previous instanceof JsonArray){
                JsonArrayBuilder prev = Json.createArrayBuilder((JsonArray)previous);
                prev.add(value);
                jsonBuilder.add(key, prev);
            } else {
                JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
                arrayBuilder.add(previous);
                arrayBuilder.add(value);
                jsonBuilder.add(key, arrayBuilder);
            }
        } else {
            jsonBuilder.add(key, value);
        }
        return jsonBuilder.build();
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
     * Converts a {@link JsonArray} to a {@link List}&lt;{@link Object}&gt;
     * where the objects are all the standard java types represented by the values
     * in the array
     * @param array The JsonArray to convert to {@link List}
     * @return 
     * @since 5.0
     * @see #jsonValueToRaw
     */
    public static List<Object> jsonArraytoArray(JsonArray array){
        List<Object> result = new ArrayList<>();
        for (JsonValue instance : array){
            result.add(jsonValueToRaw(instance));
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
    public static Object jsonValueToRaw(JsonValue value){
        ValueType type = value.getValueType();
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
    
}

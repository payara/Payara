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
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
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
 */
package org.glassfish.admin.rest.utils;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.glassfish.admin.rest.composite.RestModel;
import org.glassfish.admin.rest.composite.metadata.Confidential;
import org.glassfish.admin.rest.model.ResponseBody;

/**
 *
 * @author jdlee
 */
public class JsonUtil {
    public static final String CONFIDENTIAL_PROPERTY_SET = "@_Oracle_Confidential_Property_Set_V1.1_#";
    public static final String CONFIDENTIAL_PROPERTY_UNSET = null;

    public static Object getJsonObject(Object object) throws JSONException {
        return getJsonObject(object, true);
    }

    public static Object getJsonObject(Object object, boolean hideConfidentialProperties) throws JSONException {
        Object result;
        if (object instanceof Collection) {
            result = processCollection((Collection)object);
        } else if (object instanceof Map) {
            result = processMap((Map)object);
        } else if (object == null) {
            result = JSONObject.NULL;
        } else if (RestModel.class.isAssignableFrom(object.getClass())) {
            result = getJsonForRestModel((RestModel)object, hideConfidentialProperties);
        } else if (object instanceof ResponseBody) {
            result = ((ResponseBody)object).toJson();
        } else {
            Class<?> clazz = object.getClass();
            if (clazz.isArray()) {
                JSONArray array = new JSONArray();
                final int lenth = Array.getLength(object);
                for (int i = 0; i < lenth; i++) {
                    array.put(getJsonObject(Array.get(object, i)));
                }
                result = array;
            } else {
                result = object;
            }
        }

        return result;
    }

    public static JSONObject getJsonForRestModel(RestModel model, boolean hideConfidentialProperties) {
        JSONObject result = new JSONObject();
        for (Method m : model.getClass().getDeclaredMethods()) {
            if (m.getName().startsWith("get")) { // && !m.getName().equals("getClass")) {
                String propName = m.getName().substring(3);
                propName = propName.substring(0,1).toLowerCase(Locale.getDefault()) + propName.substring(1);
                if (!model.isTrimmed() || model.isSet(propName)) { // TBD - remove once the conversion to the new REST style guide is completed
//              if (model.isSet(propName)) {
                    // Only include properties whose value has been set in the model
                    try {
                        result.put(propName, getJsonObject(getRestModelProperty(model, m, hideConfidentialProperties)));
                    } catch (Exception e) {
                    }
                }
            }
        }

        return result;
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

    public static JSONArray processCollection(Collection c) throws JSONException {
        JSONArray result = new JSONArray();
        Iterator i = c.iterator();
        while (i.hasNext()) {
            Object item = getJsonObject(i.next());
            result.put(item);
        }

        return result;
    }

    public static JSONObject processMap(Map map) throws JSONException {
        JSONObject result = new JSONObject();

        for (Map.Entry entry : (Set<Map.Entry>)map.entrySet()) {
            result.put(entry.getKey().toString(), getJsonObject(entry.getValue()));
        }

        return result;
    }

    public static String getString(JSONObject jsonObject, String key, String dflt) {
        try {
            if (jsonObject.isNull(key)) {
                return null;
            }
            return jsonObject.getString(key);
        } catch (JSONException e) {
            return dflt;
        }
    }

    public static int getInt(JSONObject jsonObject, String key, int dflt) {
        try {
            return jsonObject.getInt(key);
        } catch (JSONException e) {
            return dflt;
        }
    }

    public static void put(JSONObject jsonObject, String key, Object value) {
        try {
            synchronized(jsonObject) {
                jsonObject.put(key, value!=null?value:JSONObject.NULL);
            }
        } catch (JSONException e) {
            // ignore. The exception is thrown only if the value is non-finite number
            // or if the key is null.
        }
    }

    public static void put(JSONArray jsonArray, JSONObject item) {
        synchronized(jsonArray) {
            jsonArray.put(item);
        }
    }
}

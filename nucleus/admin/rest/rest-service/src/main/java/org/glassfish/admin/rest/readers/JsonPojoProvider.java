/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.glassfish.admin.rest.readers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.glassfish.admin.rest.composite.CompositeUtil;
import org.glassfish.admin.rest.composite.RestModel;

/**
 *
 * @author jdlee
 */
@Provider
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class JsonPojoProvider<T extends RestModel> implements MessageBodyReader<T> {
    @Override
    public T readFrom(Class<T> type, Type type1, Annotation[] antns, MediaType mt, MultivaluedMap<String, String> mm, InputStream entityStream) throws WebApplicationException {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(entityStream));
            StringBuilder sb = new StringBuilder();
            String line = in.readLine();
            while (line != null) {
                sb.append(line);
                line = in.readLine();
            }

            JSONObject o = new JSONObject(sb.toString());
            T model = CompositeUtil.hydrateClass(type, o);
            return (T)model;
        } catch (Exception e) {
            throw new WebApplicationException(e);
        }
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return mediaType.equals(MediaType.APPLICATION_JSON_TYPE) && RestModel.class.isAssignableFrom(type);
    }

    private void setValue (Object model, String key, Object value) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        if (value == null) {
            return;
        }
        
        String setterName = "set" + key.substring(0,1).toUpperCase() + key.substring(1);
        System.out.println("Setter = " + setterName);
        Method method = null;
        try {
            method = model.getClass().getMethod(setterName, value.getClass());
        } catch (Exception ex) {
        }
        if (method == null) {
            Method[] methods = model.getClass().getMethods();
            for (Method m : methods) {
                if (m.getName().equals(setterName)) {
                    if (m.getParameterTypes()[0].isAssignableFrom(value.getClass())) {
                        method = m;
                        break;
                    }
                }
            }
        }
        if (method != null) {
            method.invoke(model, value);
        }
    }

    private Object getRealObject(Object o) throws JSONException {
        if (o.equals(JSONObject.NULL)) {
            return null;
        } else if (JSONArray.class.isAssignableFrom(o.getClass())) {
            JSONArray array = (JSONArray) o;
            List list = new ArrayList();
            for (int i = 0; i < array.length(); i++) {
                list.add(getRealObject(array.get(i)));
            }
            return list;
        }

        return o;
    }
}

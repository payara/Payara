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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.glassfish.admin.rest.composite.CompositeUtil;
import org.glassfish.admin.rest.composite.RestModel;
import org.glassfish.admin.rest.utils.Util;

/**
 *
 * @author jdlee
 */
//@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class RestModelListReader implements MessageBodyReader<List<RestModel>> {
    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        String submittedType = mediaType.toString();
        int index = submittedType.indexOf(";");
        if (index > -1) {
            submittedType = submittedType.substring(0, index);
        }
        return submittedType.equals(MediaType.APPLICATION_JSON) && List.class.isAssignableFrom(type) &&
                RestModel.class.isAssignableFrom(Util.getFirstGenericType(genericType));
    }

    @Override
    public List<RestModel> readFrom(Class<List<RestModel>> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        try {
            List<RestModel> list = new ArrayList<RestModel>();
            BufferedReader in = new BufferedReader(new InputStreamReader(entityStream));
            StringBuilder sb = new StringBuilder();
            String line = in.readLine();
            while (line != null) {
                sb.append(line);
                line = in.readLine();
            }

            JSONArray array = new JSONArray(sb.toString());
            Class<?> modelType = null;
            if (ParameterizedType.class.isAssignableFrom(genericType.getClass())) {
                final ParameterizedType pt = (ParameterizedType) genericType;
                modelType = (Class) pt.getActualTypeArguments()[0];
            }
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                RestModel model = (RestModel) CompositeUtil.instance().unmarshallClass(modelType, o);
                Set<ConstraintViolation<RestModel>> cv = CompositeUtil.instance().validateRestModel(model);
                if (!cv.isEmpty()) {
                    final Response response = Response.status(Status.BAD_REQUEST)
                            .entity(CompositeUtil.instance().getValidationFailureMessages(cv, model))
                            .build();
                    throw new WebApplicationException(response);
                }
                list.add(model);
            }
            return list;
        } catch (Exception e) {
            throw new WebApplicationException(e);
        }

    }
}

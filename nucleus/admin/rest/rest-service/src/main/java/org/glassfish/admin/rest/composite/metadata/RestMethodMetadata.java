/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.glassfish.admin.rest.composite.metadata;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.glassfish.admin.rest.composite.RestCollection;

/**
 *
 * @author jdlee
 */
public class RestMethodMetadata {
    private String httpMethod;
    private List<ParamMetadata> queryParameters = new ArrayList<ParamMetadata>();
    private Class<?> requestPayload;
    private String returnType;
    private String type = "item";
    private String path;

    public RestMethodMetadata(Method method, Annotation designator) {
        this.httpMethod = designator.getClass().getInterfaces()[0].getSimpleName();
        this.returnType = calculateReturnType(method);
        this.path = calculatePath(method);
        processParameters(method);
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public List<ParamMetadata> getQueryParameters() {
        return queryParameters;
    }

    public void setQueryParameters(List<ParamMetadata> queryParameters) {
        this.queryParameters = queryParameters;
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "RestMethodMetadata{" + "httpMethod=" + httpMethod + ", queryParameters=" + queryParameters + ", returnType=" + returnType + ", type=" + type + '}';
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("httpMethpd", httpMethod);
        o.put("path", path);
        if (requestPayload != null) {
            o.put("requestType", requestPayload.getName());
        }
        o.put("returnType", returnType);
        o.put("type", type);
        JSONArray array = new JSONArray();
        for (ParamMetadata pmd : queryParameters) {
            array.put(pmd.toJson());
        }
        o.put("queryParams", array);

        return o;
    }

    private String calculateReturnType(Method method) {
        String value;
        final Type grt = method.getGenericReturnType();
        if (ParameterizedType.class.isAssignableFrom(grt.getClass())) {
            final ParameterizedType pt = (ParameterizedType) grt;
            value = grt.toString();
            if (RestCollection.class.isAssignableFrom((Class) pt.getRawType())) {
                this.type = "collection";
                Class<?> gt = (Class<?>) pt.getActualTypeArguments()[0];
                value = gt.getName();
            }
        } else {
            value = ((Class<?>)grt).getName();
        }
        
        return value;
    }

    private String calculatePath(Method method) {
        Path p = method.getAnnotation(Path.class);
        return (p != null) ? p.value() : null;
    }

    private void processParameters(Method method) {
        Class<?>[] paramTypes = method.getParameterTypes();
        Annotation[][] paramAnnos = method.getParameterAnnotations();
        int paramCount = paramTypes.length;

        for (int i = 0; i < paramCount; i++) {
            boolean processed = false;
            boolean isPathParam = false;
            Class<?> paramType = paramTypes[i];
            for (Annotation annotation : paramAnnos[i]) {
                if (PathParam.class.isAssignableFrom(annotation.getClass())) {
                    isPathParam = true;
                }
                if (QueryParam.class.isAssignableFrom(annotation.getClass())) {
                    queryParameters.add(new ParamMetadata(paramType, (QueryParam)annotation, paramAnnos[i]));
                    processed = true;
                }

            }
            if (!processed && !isPathParam) {
                requestPayload = paramType;
            }
        }
    }
}

/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.admin.rest.composite.metadata;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.glassfish.admin.rest.composite.CompositeUtil;
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
    private Object context;

    public RestMethodMetadata(Object context, Method method, Annotation designator) {
        this.context = context;
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
        o.put("httpMethod", httpMethod);
        o.put("path", path);
        if (requestPayload != null) {
            JSONObject rp = new JSONObject();
            rp.put("type", requestPayload.getName());
            rp.put("properties", getProperties(requestPayload));
            o.put("request", rp);
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
                    queryParameters.add(new ParamMetadata(context, paramType, ((QueryParam)annotation).value(), paramAnnos[i]));
                    processed = true;
                }

            }
            if (!processed && !isPathParam) {
                requestPayload = paramType;
            }
        }
    }

    /**
     * This method will analyze the getters of the given class to determine its properties.  Currently, for simplicity's
     * sake, only getters are checked.
     * @param clazz
     * @return
     * @throws JSONException
     */
    private JSONArray getProperties(Class<?> clazz) throws JSONException {
        Map<String, ParamMetadata> map = new HashMap<String, ParamMetadata>();
        JSONArray props = new JSONArray();
        if (clazz.isInterface()) {
            Object model = CompositeUtil.instance().getModel(clazz);
            clazz = model.getClass();
        }

        for (Class<?> ifaces : clazz.getInterfaces()) {
            for (Method m : ifaces.getDeclaredMethods()) {
                String methodName = m.getName();
                if (methodName.startsWith("get")) {
                    String propertyName = methodName.substring(3,4).toLowerCase() + methodName.substring(4);
                    map.put(propertyName, new ParamMetadata(context, m.getReturnType(), propertyName, m.getAnnotations()));
                }
            }
        }

        for (Map.Entry<String, ParamMetadata> entry : map.entrySet()) {
            props.put(entry.getValue().toJson());
        }

        return props;
    }
}

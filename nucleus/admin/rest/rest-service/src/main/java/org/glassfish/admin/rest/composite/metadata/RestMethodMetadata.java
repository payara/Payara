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
import java.util.Locale;
import java.util.Map;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.glassfish.admin.rest.OptionsCapable;
import org.glassfish.admin.rest.composite.CompositeResource;
import org.glassfish.admin.rest.composite.CompositeUtil;
import org.glassfish.admin.rest.composite.RestCollection;
import org.glassfish.admin.rest.composite.RestModel;
import org.glassfish.admin.rest.utils.Util;

/**
 * This class encapsulates the metadata for the specified REST resource method.
 * @author jdlee
 */
public class RestMethodMetadata {
    private String httpMethod;
    private List<ParamMetadata> queryParameters = new ArrayList<ParamMetadata>();
    /**
     * The type of the incoming request body, if there is one.
     */
    private Class<?> requestPayload;
    /**
     * The type of the response entity.  If <code>isCollection</code> is true, this value reflects the type of the items
     * in the collection.
     */
    private Class<?> returnPayload;
    /**
     * Specifies whether or not the return payload is a collection (in which case <code>returnPayload</code> gives the
     * type of the items in the collection) or not.
     */
    private boolean isCollection = false;
    private OptionsCapable context;

    public RestMethodMetadata(OptionsCapable context, Method method, Annotation designator) {
        this.context = context;
        this.httpMethod = designator.getClass().getInterfaces()[0].getSimpleName();
        this.returnPayload = calculateReturnPayload(method);
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

    public Class<?> getRequestPayload() {
        return requestPayload;
    }

    public void setRequestPayload(Class<?> requestPayload) {
        this.requestPayload = requestPayload;
    }

    public Class<?> getReturnPayload() {
        return returnPayload;
    }

    public void setReturnPayload(Class<?> returnPayload) {
        this.returnPayload = returnPayload;
    }

    public boolean getIsCollection() {
        return isCollection;
    }

    public void setIsCollection(boolean isCollection) {
        this.isCollection = isCollection;
    }

    @Override
    public String toString() {
        return "RestMethodMetadata{" + "httpMethod=" + httpMethod + ", queryParameters=" + queryParameters + ", returnType=" 
                + returnPayload + ", isCollection =" + isCollection + '}';
    }

    /**
     * Build and return a JSON object representing the metadata for the resource method
     * @return
     * @throws JSONException
     */
    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        JSONArray array = new JSONArray();
        for (ParamMetadata pmd : queryParameters) {
            array.put(pmd.toJson());
        }
        o.put("queryParams", array);

        if (requestPayload != null) {
            JSONObject requestProps = new JSONObject();
            requestProps.put("isCollection", isCollection);
            requestProps.put("dataType", requestPayload.getSimpleName());
            requestProps.put("properties", getProperties(requestPayload));
            o.put("request", requestProps);
        }

        if (returnPayload != null) {
            JSONObject returnProps = new JSONObject();
            returnProps.put("isCollection", isCollection);
            returnProps.put("dataType", returnPayload.getSimpleName());
            returnProps.put("properties", getProperties(returnPayload));
            o.put("response", returnProps);
        }

        return o;
    }

    /**
     * Get the return type of the method.  If the return type is a generic type, then extract the first generic type
     * from the declaration via reflection.  This method does not take into account types with multiple generic types,
     * but, given the style guide, this should not be an issue.  If the style guide is modified to allow non-RestModel and
     * non-RestCollection returns (for example), this may need to be modified.
     * @param method
     * @return
     */
    private Class<?> calculateReturnPayload(Method method) {
        final Type grt = method.getGenericReturnType();
        Class<?> value = (Class<?>)method.getReturnType();
        if (ParameterizedType.class.isAssignableFrom(grt.getClass())) {
            final ParameterizedType pt = (ParameterizedType) grt;
            if (RestCollection.class.isAssignableFrom((Class) pt.getRawType())) {
                isCollection = true;
                value = Util.getFirstGenericType(grt);
            } else if (RestModel.class.isAssignableFrom((Class)pt.getRawType())) {
                value = Util.getFirstGenericType(grt);
            }
        }
        
        return value;
    }

    /**
     * Process the parameters for the method.  Any parameter marked <code>@PathParam</code> is ignored, since JAX-RS
     * handles setting that value, meaning its presence need not be exposed to the client.  Any parameter marked with
     * <code>@QueryParameter</code> is stored in the <code>queryParameter</code> list.  Anything left is considered the
     * type of the request body. There should be only one of these.
     * @param method
     */
    private void processParameters(Method method) {
        Type[] paramTypes = method.getGenericParameterTypes();
        Annotation[][] paramAnnos = method.getParameterAnnotations();
        int paramCount = paramTypes.length;

        for (int i = 0; i < paramCount; i++) {
            boolean processed = false;
            boolean isPathParam = false;
            Type paramType = paramTypes[i];
            for (Annotation annotation : paramAnnos[i]) {
                if (PathParam.class.isAssignableFrom(annotation.getClass())) {
                    isPathParam = true;
                }
                if (QueryParam.class.isAssignableFrom(annotation.getClass())) {
                    queryParameters.add(new ParamMetadata(context, (Class<?>)paramType, ((QueryParam)annotation).value(), paramAnnos[i]));
                    processed = true;
                }

            }
            if (!processed && !isPathParam) {
                requestPayload = Util.isGenericType(paramType) ?
                                 Util.getFirstGenericType(paramType) : (Class<?>)paramType;
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
    private JSONObject getProperties(Class<?> clazz) throws JSONException {
        Map<String, ParamMetadata> map = new HashMap<String, ParamMetadata>();
        JSONObject props = new JSONObject();
        if (clazz.isInterface()) {
            Object model = CompositeUtil.instance().getModel(clazz);
            clazz = model.getClass();
        }

        for (Class<?> ifaces : clazz.getInterfaces()) {
            for (Method m : ifaces.getDeclaredMethods()) {
                String methodName = m.getName();
                if (methodName.startsWith("get")) {
                    String propertyName = methodName.substring(3,4).toLowerCase(Locale.getDefault()) + methodName.substring(4);
                    map.put(propertyName, new ParamMetadata(context, m.getReturnType(), propertyName, m.getAnnotations()));
                }
            }
        }

        for (Map.Entry<String, ParamMetadata> entry : map.entrySet()) {
            props.put(entry.getKey(), entry.getValue().toJson());
        }

        return props;
    }
}

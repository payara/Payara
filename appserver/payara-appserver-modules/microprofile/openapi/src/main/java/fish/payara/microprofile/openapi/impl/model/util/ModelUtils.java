/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2018] Payara Foundation and/or its affiliates. All rights reserved.
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
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package fish.payara.microprofile.openapi.impl.model.util;

import static java.util.logging.Level.WARNING;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.ws.rs.BeanParam;
import javax.ws.rs.CookieParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

import org.eclipse.microprofile.openapi.models.Constructible;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.PathItem.HttpMethod;
import org.eclipse.microprofile.openapi.models.media.Schema.SchemaType;
import org.eclipse.microprofile.openapi.models.parameters.Parameter.In;

import fish.payara.microprofile.openapi.impl.model.OperationImpl;
import java.lang.reflect.GenericDeclaration;
import javax.ws.rs.Path;

public final class ModelUtils {

    private static final Logger LOGGER = Logger.getLogger(ModelUtils.class.getName());

    /**
     * The name of a variable in the model tree that is unrecognised.
     */
    public static final String UNKNOWN_ELEMENT_NAME = "?";

    /**
     * Private constructor to hide public one.
     */
    private ModelUtils() {
    }

    /**
     * Normalises a path string. A normalised path has:
     * <ul>
     * <li>no multiple slashes.</li>
     * <li>no trailing slash.</li>
     * </ul>
     * 
     * @param path the path to be normalised.
     */
    public static String normaliseUrl(String path) {
        if (path == null) {
            return null;
        }
        // Add leading slash
        path = "/" + path;
        
        // Remove multiple slashes
        path = path.replaceAll("/+", "/");

        // Remove trailing slash
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        // Assure that there is one slash
        if (path.isEmpty()) {
            path = "/";
        }
        return path;
    }

    /**
     * @param method the method to analyse.
     * @return the {@link HttpMethod} applied to this method, or null if there is
     *         none.
     */
    public static HttpMethod getHttpMethod(Method method) {
        if (method.isAnnotationPresent(GET.class)) {
            return HttpMethod.GET;
        }
        if (method.isAnnotationPresent(POST.class)) {
            return HttpMethod.POST;
        }
        if (method.isAnnotationPresent(PUT.class)) {
            return HttpMethod.PUT;
        }
        if (method.isAnnotationPresent(DELETE.class)) {
            return HttpMethod.DELETE;
        }
        if (method.isAnnotationPresent(HEAD.class)) {
            return HttpMethod.HEAD;
        }
        if (method.isAnnotationPresent(OPTIONS.class)) {
            return HttpMethod.OPTIONS;
        }
        if (method.isAnnotationPresent(PATCH.class)) {
            return HttpMethod.PATCH;
        }
        return null;
    }

    public static HttpMethod getHttpMethod(String method) {
        if (method.equalsIgnoreCase("GET")) {
            return HttpMethod.GET;
        }
        if (method.equalsIgnoreCase("POST")) {
            return HttpMethod.POST;
        }
        if (method.equalsIgnoreCase("PUT")) {
            return HttpMethod.PUT;
        }
        if (method.equalsIgnoreCase("DELETE")) {
            return HttpMethod.DELETE;
        }
        if (method.equalsIgnoreCase("HEAD")) {
            return HttpMethod.HEAD;
        }
        if (method.equalsIgnoreCase("OPTIONS")) {
            return HttpMethod.OPTIONS;
        }
        if (method.equalsIgnoreCase("PATCH")) {
            return HttpMethod.PATCH;
        }
        return null;
    }

    /**
     * Creates a new {@link Operation}, and inserts it into the {@link PathItem}.
     * 
     * @param pathItem   the {@link PathItem} to add the {@link Operation} to.
     * @param httpMethod the HTTP method of the operation to add.
     * @return the newly created {@link Operation}, or the existing operation if
     *         available.
     */
    public static Operation getOrCreateOperation(PathItem pathItem, HttpMethod httpMethod) {
        Operation operation = new OperationImpl();
        if (pathItem.readOperationsMap().get(httpMethod) != null) {
            return pathItem.readOperationsMap().get(httpMethod);
        }
        switch (httpMethod) {
        case GET:
            pathItem.setGET(operation);
            break;
        case POST:
            pathItem.setPOST(operation);
            break;
        case PUT:
            pathItem.setPUT(operation);
            break;
        case DELETE:
            pathItem.setDELETE(operation);
            break;
        case HEAD:
            pathItem.setHEAD(operation);
            break;
        case OPTIONS:
            pathItem.setOPTIONS(operation);
            break;
        case PATCH:
            pathItem.setPATCH(operation);
            break;
        case TRACE:
            pathItem.setTRACE(operation);
            break;
        default:
            throw new IllegalArgumentException("HTTP method not recognised.");
        }
        return operation;
    }

    public static Operation findOperation(OpenAPI api, Method method, String path) {
        Operation foundOperation = null;
        try {
            return api.getPaths().get(path).readOperationsMap().get(getHttpMethod(method));
        } catch (NullPointerException ex) {
            // Operation not found
        }
        return foundOperation;
    }

    public static void removeOperation(PathItem pathItem, Operation operation) {
        if (operation == null) {
            return;
        }
        if (operation.equals(pathItem.getGET())) {
            pathItem.setGET(null);
        }
        if (operation.equals(pathItem.getPOST())) {
            pathItem.setPOST(null);
        }
        if (operation.equals(pathItem.getPUT())) {
            pathItem.setPUT(null);
        }
        if (operation.equals(pathItem.getDELETE())) {
            pathItem.setDELETE(null);
        }
        if (operation.equals(pathItem.getHEAD())) {
            pathItem.setHEAD(null);
        }
        if (operation.equals(pathItem.getPATCH())) {
            pathItem.setPATCH(null);
        }
        if (operation.equals(pathItem.getOPTIONS())) {
            pathItem.setOPTIONS(null);
        }
        if (operation.equals(pathItem.getTRACE())) {
            pathItem.setTRACE(null);
        }
    }

    /**
     * Finds the {@link SchemaType} that corresponds to a given class.
     * 
     * @param type the class to map.
     * @return the schema type the class corresponds to.
     */
    public static SchemaType getSchemaType(Class<?> type) {
        if (Boolean.class.isAssignableFrom(type) || boolean.class.isAssignableFrom(type)) {
            return SchemaType.BOOLEAN;
        }
        if (Integer.class.isAssignableFrom(type) || int.class.isAssignableFrom(type)) {
            return SchemaType.INTEGER;
        }
        if (type == short.class || type == Short.class || type == long.class || type == Long.class
                || type == float.class || type == Float.class || type == double.class || type == Double.class) {
            return SchemaType.NUMBER;
        }
        if (type.isArray() || Iterable.class.isAssignableFrom(type)) {
            return SchemaType.ARRAY;
        }
        if (String.class.isAssignableFrom(type)) {
            return SchemaType.STRING;
        }
        return SchemaType.OBJECT;
    }

    /**
     * Finds a {@link SchemaType} that can represent both of the given types. If one
     * of the input values are null, this function returns the other. If both are
     * null, this function returns null.
     * 
     * @param type1 the first schema type.
     * @param type2 the second schema type.
     * @return a {@link SchemaType} that can represent both.
     */
    public static SchemaType getParentSchemaType(SchemaType type1, SchemaType type2) {
        if (type1 == null && type2 == null) {
            return null;
        }
        if (type1 == null) {
            return type2;
        }
        if (type2 == null) {
            return type1;
        }
        if (type1 == SchemaType.OBJECT || type2 == SchemaType.OBJECT) {
            return SchemaType.OBJECT;
        }
        if (type1 == SchemaType.STRING || type2 == SchemaType.STRING) {
            return SchemaType.STRING;
        }
        if (type1 != type2) {
            return SchemaType.STRING;
        }
        return type1;
    }

    public static boolean isRequestBody(Parameter parameter) {
        if (parameter.getDeclaredAnnotations().length == 0) {
            return true;
        }
        if (parameter.isAnnotationPresent(FormParam.class)) {
            return false;
        }
        if (parameter.isAnnotationPresent(QueryParam.class)) {
            return false;
        }
        if (parameter.isAnnotationPresent(MatrixParam.class)) {
            return false;
        }
        if (parameter.isAnnotationPresent(BeanParam.class)) {
            return false;
        }
        if (parameter.isAnnotationPresent(HeaderParam.class)) {
            return false;
        }
        if (parameter.isAnnotationPresent(PathParam.class)) {
            return false;
        }
        if (parameter.isAnnotationPresent(CookieParam.class)) {
            return false;
        }
        if (parameter.isAnnotationPresent(Context.class)) {
            return false;
        }
        if (parameter.isAnnotationPresent(Inject.class)) {
            return false;
        }
        if (parameter.isAnnotationPresent(Provider.class)) {
            return false;
        }
        return true;
    }

    public static In getParameterType(Parameter parameter) {
        if (parameter.isAnnotationPresent(PathParam.class)) {
            return In.PATH;
        }
        if (parameter.isAnnotationPresent(QueryParam.class)) {
            return In.QUERY;
        }
        if (parameter.isAnnotationPresent(HeaderParam.class)) {
            return In.HEADER;
        }
        if (parameter.isAnnotationPresent(CookieParam.class)) {
            return In.COOKIE;
        }
        return null;
    }

    public static String getParameterName(Parameter parameter) {
        if (parameter.isAnnotationPresent(PathParam.class)) {
            return parameter.getDeclaredAnnotation(PathParam.class).value();
        } else if (parameter.isAnnotationPresent(QueryParam.class)) {
            return parameter.getDeclaredAnnotation(QueryParam.class).value();
        } else if (parameter.isAnnotationPresent(HeaderParam.class)) {
            return parameter.getDeclaredAnnotation(HeaderParam.class).value();
        } else if (parameter.isAnnotationPresent(CookieParam.class)) {
            return parameter.getDeclaredAnnotation(CookieParam.class).value();
        }
        return null;
    }

    public static boolean isAnnotationNull(Annotation annotation) {
        if (annotation == null) {
            return true;
        }
        boolean allNull = true;
        for (Method m : annotation.annotationType().getDeclaredMethods()) {
            if (m.getParameterCount() == 0) {
                try {
                    Object value = m.invoke(annotation);
                    if (value != null) {
                        if (value.getClass().isArray() && Array.getLength(value) > 0) {
                            return false;
                        } else if (value instanceof Collection && !Collection.class.cast(value).isEmpty()) {
                            return false;
                        } else if (value instanceof Boolean && Boolean.class.cast(value)) {
                            return false;
                        } else if (value.getClass().equals(Class.class)
                                && !Class.class.cast(value).getTypeName().equals("java.lang.Void")) {
                            return false;
                        } else if (value.getClass().isEnum() && !Enum.class.cast(value).name().isEmpty()
                                && !Enum.class.cast(value).name().equalsIgnoreCase("DEFAULT")) {
                            return false;
                        } else if (String.class.isAssignableFrom(value.getClass()) && !value.toString().isEmpty()) {
                            return false;
                        } else if (value instanceof Annotation) {
                            allNull = isAnnotationNull((Annotation) value);
                        }
                    }
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                    LOGGER.log(WARNING, "Unable to access annotation element.", ex);
                }
            }
        }
        return allNull;
    }

    public static <E> E mergeProperty(E current, E offer, boolean override) {
        // Treat empty strings as null
        if (offer instanceof String && offer.toString().isEmpty()) {
            offer = null;
        }
        // Treat max or min integers as null
        if (offer instanceof Integer && (offer.equals(Integer.MAX_VALUE) || offer.equals(Integer.MIN_VALUE))) {
            offer = null;
        }
        E resolve = current;
        if (offer != null) {
            if (override) {
                resolve = offer;
            } else {
                if (current == null) {
                    resolve = offer;
                }
            }
        }
        return resolve;
    }

    /**
     * Set the reference property of an object, and clear every other field.
     */
    public static void applyReference(org.eclipse.microprofile.openapi.models.Reference<?> referee, String reference) {
        // Set the reference
        referee.setRef(reference);

        // For every field except the reference
        for (Field field : referee.getClass().getDeclaredFields()) {
            // Make the field accessible
            boolean accessible = field.isAccessible();
            field.setAccessible(true);
            try {
                Object currentValue = field.get(referee);
                // If the field is not equal to the reference
                if (currentValue != null && !field.getName().equals("ref")) {
                    // If the field is a collection, clear it
                    if (Collection.class.isAssignableFrom(field.getType())) {
                        Collection.class.cast(field.get(referee)).clear();
                        continue;
                    }
                    // If the field is an array, clear it
                    if (field.getType().isArray()) {
                        field.set(referee, field.getType().getClass().cast(new Object[0]));
                        continue;
                    }
                    // Otherwise, set it to null
                    field.set(referee, null);
                }
            } catch (Exception ex) {
                continue;
            } finally {
                field.setAccessible(accessible);
            }
        }
    }

    public static <T> void overwrite(T from, T to) {
        if (to != null && from != null) {
            for (Field f : to.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                try {
                    f.set(to, f.get(from));
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    // Ignore errors
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> void merge(T from, T to, boolean override) {
        if (from != null && to != null) {
            for (Field f : to.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                try {
                    // Get the new and old value
                    Object newValue = f.get(from);
                    Object currentValue = f.get(to);
                    if (newValue != null) {
                        if (newValue instanceof Map) {
                            for (Entry<Object, Object> entry : (Set<Entry<Object, Object>>) Map.class.cast(newValue)
                                    .entrySet()) {
                                if (!Map.class.cast(currentValue).containsKey(entry.getKey())) {
                                    Map.class.cast(currentValue).put(entry.getKey(), entry.getValue());
                                } else {
                                    merge(entry.getValue(), Map.class.cast(currentValue).get(entry.getKey()), override);
                                }
                            }
                        }
                        else if (newValue instanceof Collection) {
                            for (Object o : Collection.class.cast(newValue)) {
                                if (!Collection.class.cast(currentValue).contains(o)) {
                                    Collection.class.cast(currentValue).add(o);
                                }
                            }
                        }
                       else if (newValue instanceof Constructible) {
                            if (currentValue == null) {
                                f.set(to, newValue.getClass().newInstance());
                                currentValue = f.get(to);
                            }
                            merge(newValue, currentValue, override);
                        } else {
                            f.set(to, mergeProperty(f.get(to), f.get(from), override));
                        }
                    }
                } catch (IllegalArgumentException | IllegalAccessException | InstantiationException e) {
                    // Ignore errors
                }
            }
        }
    }
    
    public static org.eclipse.microprofile.openapi.models.Operation getOperation(Method method,
            OpenAPI api, Map<String, Set<Class<?>>> resourceMapping) {
        String path = getResourcePath(method, resourceMapping);
        if (path != null) {
            PathItem pathItem = api.getPaths().get(path);
            if (pathItem != null) {
                PathItem.HttpMethod httpMethod = getHttpMethod(method);
                return pathItem.readOperationsMap().get(httpMethod);
            }
        }
        return null;
    }

    public static String getResourcePath(GenericDeclaration declaration, Map<String, Set<Class<?>>> resourceMapping) {
        String path = null;
        if (declaration instanceof Method) {
            Method method = (Method) declaration;

            // If the method is a valid resource
            if (method.isAnnotationPresent(GET.class) || method.isAnnotationPresent(POST.class)
                    || method.isAnnotationPresent(PUT.class) || method.isAnnotationPresent(DELETE.class)
                    || method.isAnnotationPresent(HEAD.class) || method.isAnnotationPresent(OPTIONS.class)
                    || method.isAnnotationPresent(PATCH.class)) {
                if (method.isAnnotationPresent(Path.class)) {
                    path = getResourcePath(method.getDeclaringClass(), resourceMapping) + "/"
                            + method.getDeclaredAnnotation(Path.class).value();
                } else {
                    path = getResourcePath(method.getDeclaringClass(), resourceMapping);
                }
            }
        }
         if (declaration instanceof Class) {
            Class<?> clazz = (Class<?>) declaration;
            clazz.toString();
          

            // If the class is a resource and contains a mapping
            if (clazz.isAnnotationPresent(Path.class)) {
                for (Map.Entry<String, Set<Class<?>>> entry : resourceMapping.entrySet()) {
                    if (entry.getValue() != null && entry.getValue().contains(clazz)) {
                        path = entry.getKey() + "/" + clazz.getDeclaredAnnotation(Path.class).value();
                    }
                }
            }
        }
        return normaliseUrl(path);
    }   
}
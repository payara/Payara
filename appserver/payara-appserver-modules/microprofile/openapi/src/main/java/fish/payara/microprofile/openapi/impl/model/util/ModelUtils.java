/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2018-2023] Payara Foundation and/or its affiliates. All rights reserved.
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

import fish.payara.microprofile.openapi.impl.visitor.AnnotationInfo;
import fish.payara.microprofile.openapi.api.visitor.ApiContext;
import fish.payara.microprofile.openapi.impl.model.OperationImpl;
import fish.payara.microprofile.openapi.impl.visitor.OpenApiContext;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import static java.util.logging.Level.WARNING;
import java.util.logging.Logger;
import jakarta.inject.Inject;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.MatrixParam;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.openapi.models.Constructible;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.PathItem.HttpMethod;
import org.eclipse.microprofile.openapi.models.media.Schema.SchemaType;
import org.eclipse.microprofile.openapi.models.parameters.Parameter.In;
import org.glassfish.hk2.classmodel.reflect.AnnotatedElement;
import org.glassfish.hk2.classmodel.reflect.AnnotationModel;
import org.glassfish.hk2.classmodel.reflect.ExtensibleType;
import org.glassfish.hk2.classmodel.reflect.FieldModel;
import org.glassfish.hk2.classmodel.reflect.MethodModel;
import org.glassfish.hk2.classmodel.reflect.ParameterizedType;

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
     * @param context
     * @param method the method to analyse.
     * @return the {@link HttpMethod} applied to this method, or null if there is
     *         none.
     */
    public static HttpMethod getHttpMethod(OpenApiContext context, MethodModel method) {
        AnnotationInfo annotations = context.getAnnotationInfo(method.getDeclaringType());
        if (annotations.isAnnotationPresent(GET.class, method)) {
            return HttpMethod.GET;
        }
        if (annotations.isAnnotationPresent(POST.class, method)) {
            return HttpMethod.POST;
        }
        if (annotations.isAnnotationPresent(PUT.class, method)) {
            return HttpMethod.PUT;
        }
        if (annotations.isAnnotationPresent(DELETE.class, method)) {
            return HttpMethod.DELETE;
        }
        if (annotations.isAnnotationPresent(HEAD.class, method)) {
            return HttpMethod.HEAD;
        }
        if (annotations.isAnnotationPresent(OPTIONS.class, method)) {
            return HttpMethod.OPTIONS;
        }
        if (annotations.isAnnotationPresent(PATCH.class, method)) {
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
        if (pathItem.getOperations().get(httpMethod) != null) {
            return pathItem.getOperations().get(httpMethod);
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

    public static Operation findOperation(OpenApiContext context, OpenAPI api, MethodModel method, String path) {
        Operation foundOperation = null;
        try {
            return api.getPaths().getPathItem(path).getOperations().get(getHttpMethod(context, method));
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

    @SuppressWarnings("unchecked")
    public static String getSchemaName(ApiContext context, AnnotatedElement type) {

        assert type != null;
        // context and annotation can be null

        final Class<? extends Annotation>[] ANNOTATION_TYPES = new Class[] {
            org.eclipse.microprofile.openapi.annotations.media.Schema.class,
            jakarta.xml.bind.annotation.XmlRootElement.class,
            jakarta.xml.bind.annotation.XmlElement.class
        };

        for (Class<? extends Annotation> annotationType : ANNOTATION_TYPES) {

            AnnotationModel annotationModel;

            // Fetch the element annotations
            if (context != null && type instanceof ExtensibleType) {
                // Fetch the annotation from the cache
                ExtensibleType<?> implementationType = (ExtensibleType<?>) type;
                AnnotationInfo annotationInfo = context.getAnnotationInfo(implementationType);
                annotationModel = annotationInfo.getAnnotation(annotationType);
            } else {
                // Fetch the annotation manually
                annotationModel = type.getAnnotation(annotationType.getName());
            }

            // Fields can be named by their accessors
            if (annotationModel == null) {
                if (type instanceof FieldModel) {
                    final FieldModel field = (FieldModel) type;
                    final String accessorName = getAccessorName(field.getName());
                    for (MethodModel method : field.getDeclaringType().getMethods()) {
                        // Check if it's the accessor
                        if (accessorName.equals(method.getName())) {
                            annotationModel = type.getAnnotation(annotationType.getName());
                            break;
                        }
                    }
                }
            }

            // Get the schema name if the annotation exists
            if (annotationModel != null) {
                final String name = annotationModel.getValue("name", String.class);
                if (name != null && !name.isEmpty() && !name.equals("##default")) {
                    return name;
                }
            }
        }

        return getSimpleName(type.getName());
    }

    public static String getAccessorName(String fieldName) {
        char firstCharacter = Character.toUpperCase(fieldName.charAt(0));
        return "get" + firstCharacter + fieldName.substring(1);
    }

    public static SchemaType getSchemaType(ParameterizedType type, ApiContext context) {
        if(type.isArray()) {
            return SchemaType.ARRAY;
        } else {
            return getSchemaType(type.getTypeName(), context);
        }
    }

    /**
     * Finds the {@link SchemaType} that corresponds to a given class.
     * 
     * @param typeName the class to map.
     * @return the schema type the class corresponds to.
     */
    public static SchemaType getSchemaType(String typeName, ApiContext context) {
        if (String.class.getName().equals(typeName)) {
            return SchemaType.STRING;
        }
        if ("boolean".equals(typeName) || Boolean.class.getName().equals(typeName)) {
            return SchemaType.BOOLEAN;
        }
        if ("int".equals(typeName) || Integer.class.getName().equals(typeName)) {
            return SchemaType.INTEGER;
        }
        if ("short".equals(typeName)
                || "long".equals(typeName)
                || "float".equals(typeName)
                || "double".equals(typeName)
                || "bigdecimal".equals(typeName)
                || Short.class.getName().equals(typeName)
                || Long.class.getName().equals(typeName)
                || Float.class.getName().equals(typeName)
                || Double.class.getName().equals(typeName)
                || BigDecimal.class.getName().equals(typeName)) {
            return SchemaType.NUMBER;
        }
        if (typeName != null && typeName.endsWith("[]")) {
            return SchemaType.ARRAY;
        }
        Class clazz = null;
        try {
            clazz = context.getApplicationClassLoader().loadClass(typeName);
        } catch (Throwable app) {
            try {
                clazz = Class.forName(typeName);
            } catch (Throwable t) {
            }
        }
        if (clazz != null && (clazz.isArray() || Iterable.class.isAssignableFrom(clazz))) {
            return SchemaType.ARRAY;
        }
        return SchemaType.OBJECT;
    }

    public static boolean isMap(String typeName, ApiContext context) {
        Class clazz = null;
        try {
            clazz = context.getApplicationClassLoader().loadClass(typeName);
        } catch (Throwable app) {
            try {
                clazz = Class.forName(typeName);
            } catch (Throwable t) {
            }
        }
        return Map.class.isAssignableFrom(clazz);
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

    public static boolean isRequestBody(ApiContext context, org.glassfish.hk2.classmodel.reflect.Parameter parameter) {
        AnnotationInfo annotations = context.getAnnotationInfo(parameter.getMethod().getDeclaringType());
        if (annotations.getAnnotationCount(parameter) == 0) {
            return true;
        }
        return !annotations.isAnyAnnotationPresent(
                parameter, FormParam.class, QueryParam.class, MatrixParam.class,
                BeanParam.class, HeaderParam.class, PathParam.class,
                CookieParam.class, Context.class, Inject.class, Provider.class
        );
    }

    public static In getParameterType(ApiContext context, org.glassfish.hk2.classmodel.reflect.Parameter parameter) {
        AnnotationInfo annotations = context.getAnnotationInfo(parameter.getMethod().getDeclaringType());
        if (annotations.isAnnotationPresent(PathParam.class, parameter)) {
            return In.PATH;
        }
        if (annotations.isAnnotationPresent(QueryParam.class, parameter)) {
            return In.QUERY;
        }
        if (annotations.isAnnotationPresent(HeaderParam.class, parameter)) {
            return In.HEADER;
        }
        if (annotations.isAnnotationPresent(CookieParam.class, parameter)) {
            return In.COOKIE;
        }
        return null;
    }

    public static String getParameterName(ApiContext context, org.glassfish.hk2.classmodel.reflect.Parameter parameter) {
        AnnotationInfo annotations = context.getAnnotationInfo(parameter.getMethod().getDeclaringType());
        if (annotations.isAnnotationPresent(PathParam.class, parameter)) {
            return annotations.getAnnotationValue(PathParam.class, parameter);
        } else if (annotations.isAnnotationPresent(QueryParam.class, parameter)) {
            return annotations.getAnnotationValue(QueryParam.class, parameter);
        } else if (annotations.isAnnotationPresent(HeaderParam.class, parameter)) {
            return annotations.getAnnotationValue(HeaderParam.class, parameter);
        } else if (annotations.isAnnotationPresent(CookieParam.class, parameter)) {
            return annotations.getAnnotationValue(CookieParam.class, parameter);
        }
        return null;
    }

    public static boolean isVoid(ParameterizedType type) {
        if (type == null) {
            return true;
        }
        final String typeName = type.getTypeName();
        if (typeName == null) {
            return true;
        }
        if ("void".equals(typeName)) {
            return true;
        }
        if ("java.lang.Void".equals(typeName)) {
            return true;
        }
        return false;
    }

    public static boolean isAnnotationNull(Annotation annotation) {
        if (annotation == null) {
            return true;
        }
        for (Method m : annotation.annotationType().getDeclaredMethods()) {
            if (m.getParameterCount() == 0) {
                try {
                    Object value = m.invoke(annotation);
                    // NB. annotation attribute values cannot be null
                    if (value.getClass().isArray() && Array.getLength(value) > 0) {
                        return false;
                    } else if (value instanceof Boolean && ((Boolean) value).booleanValue()) {
                        return false;
                    } else if (value instanceof Class && value != Void.class) {
                        return false;
                    } else if (value instanceof Enum && !((Enum<?>) value).name().equalsIgnoreCase("DEFAULT")) {
                        return false;
                    } else if (value instanceof String && !value.toString().isEmpty()) {
                        return false;
                    } else if (value instanceof Annotation && !isAnnotationNull((Annotation) value)) {
                        return false;
                    }
                    // else it must be another primitive wrapper but we have no rules on them
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                    LOGGER.log(WARNING, "Unable to access annotation element.", ex);
                }
            }
        }
        return true;
    }

    public static <T> void extractAnnotations(
            AnnotationModel annotationModel,
            ApiContext context,
            String parameterName,
            String key,
            BiFunction<AnnotationModel, ApiContext, T> factory,
            BiConsumer<String, T> wrapperAddFunction) {

        if (wrapperAddFunction == null) {
            throw new IllegalArgumentException("null wrapperAddFunction. This is required to modify OpenAPI documents");
        }
        List<AnnotationModel> annotations = annotationModel.getValue(parameterName, List.class);
        if (annotations != null) {
            for (AnnotationModel annotation : annotations) {
                wrapperAddFunction.accept(
                        annotation.getValue(key, String.class),
                        factory.apply(annotation, context)
                );
            }
        }
    }

    public static <T> void extractAnnotations(
            AnnotationModel annotationModel,
            ApiContext context,
            String type,
            BiFunction<AnnotationModel, ApiContext, T> factory,
            Consumer<T> wrapperAddFunction) {

        if (wrapperAddFunction == null) {
            throw new IllegalArgumentException("null wrapperAddFunction. This is required to modify OpenAPI documents");
        }
        List<AnnotationModel> annotations = annotationModel.getValue(type, List.class);
        if (annotations != null) {
            for (AnnotationModel annotation : annotations) {
                wrapperAddFunction.accept(
                        factory.apply(annotation, context)
                );
            }
        }
    }
    
    public static <T> void mergeImmutableList(List<T> from, List<T> to, Consumer<List<T>> setFunction) {
        final List<T> list = new ArrayList<>();

        if (from != null) {
            list.addAll(from);
        }
        if (to != null) {
            list.addAll(to);
        }

        setFunction.accept(list);
    }

    public static Boolean mergeProperty(Boolean current, boolean offer, boolean override) {
        return mergeProperty(current, Boolean.valueOf(offer), override);
    }

    public static Boolean mergeProperty(boolean current, Boolean offer, boolean override) {
        return mergeProperty(Boolean.valueOf(current), offer, override);
    }

    public static Boolean mergeProperty(boolean current, boolean offer, boolean override) {
        return mergeProperty(Boolean.valueOf(current), Boolean.valueOf(offer), override);
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
            Object objectToTestAccessibility = Modifier.isStatic(field.getModifiers()) ? null : referee;
            boolean accessible = field.canAccess(objectToTestAccessibility);
            field.setAccessible(true);
            try {
                Object currentValue = field.get(referee);
                // If the field is not equal to the reference
                if (currentValue != null && !field.getName().equals("ref")) {
                    // If the field is a collection, clear it
                    if (Collection.class.isAssignableFrom(field.getType())) {
                        ((Collection<?>) field.get(referee)).clear();
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
                // Skip static or synthetic fields
                if (f.isSynthetic() || Modifier.isStatic(f.getModifiers())) {
                    continue;
                }
                f.setAccessible(true);
                try {
                    // Get the new and old value
                    Object fromValue = f.get(from);
                    Object toValue = f.get(to);

                    // If there is no 'from', ignore
                    if (fromValue == null) {
                        continue;
                    }

                    if (fromValue instanceof Map && toValue != null) {
                        Map<Object, Object> fromMap = (Map<Object, Object>) fromValue;
                        Map<Object, Object> toMap = (Map<Object, Object>) toValue;
                        for (Entry<Object, Object> entry : fromMap.entrySet()) {
                            if (!toMap.containsKey(entry.getKey())) {
                                toMap.put(entry.getKey(), entry.getValue());
                            } else {
                                merge(entry.getValue(), toMap.get(entry.getKey()), override);
                            }
                        }
                    } else if (fromValue instanceof Collection && toValue != null) {
                        Collection<Object> fromCollection = (Collection<Object>) fromValue;
                        Collection<Object> toCollection = (Collection<Object>) toValue;
                        for (Object o : fromCollection) {
                            if (!toCollection.contains(o)) {
                                toCollection.add(o);
                            }
                        }
                    } else if (fromValue instanceof Constructible) {
                        if (toValue == null) {
                            f.set(to, fromValue.getClass().getDeclaredConstructor().newInstance());
                            toValue = f.get(to);
                        }
                        merge(fromValue, toValue, override);
                    } else {
                        f.set(to, mergeProperty(f.get(to), f.get(from), override));
                    }
                } catch (IllegalArgumentException | IllegalAccessException | InstantiationException
                        | NoSuchMethodException | SecurityException | InvocationTargetException e) {
                    // Ignore errors
                }
            }
        }
    }

    public static String getSimpleName(String fqn) {
        String simpleName = fqn.substring(fqn.lastIndexOf('.') + 1);
        return simpleName.substring(simpleName.lastIndexOf('$') + 1);
    }

    public static <K, V> Map<K, V> readOnlyView(Map<K, V> map) {
        if (map == null) {
            return null;
        }
        return Collections.unmodifiableMap(map);
    }

    public static <T> List<T> readOnlyView(List<T> list) {
        if (list == null) {
            return null;
        }
        return Collections.unmodifiableList(list);
    }

    public static <T> List<T> createList() {
        return new ArrayList<>();
    }

    public static <T> List<T> createList(Collection<? extends T> items) {
        if (items == null) {
            return null;
        }
        return new ArrayList<>(items);
    }

    public static <K, V> Map<K, V> createMap() {
        return new LinkedHashMap<>();
    }

    public static <K, V> Map<K, V> createMap(Map<? extends K, ? extends V> items) {
        if (items == null) {
            return null;
        }
        return new LinkedHashMap<>(items);
    }

    public static <K, V> Map<K, V> createOrderedMap() {
        return new TreeMap<>();
    }

    public static <K, V> Map<K, V> createOrderedMap(Map<? extends K, ? extends V> items) {
        if (items == null) {
            return null;
        }
        return new TreeMap<>(items);
    }

    public static Map<String, Set<String>> buildEndpoints(Map<String, Set<String>> original, String contextRoot, Set<String> paths) {
        if (original == null || original.isEmpty()) {
            original = createOrderedMap();
        }
        if (!original.containsKey(contextRoot)) {
            original.put(contextRoot, paths);
        }
        return original;
    }
}
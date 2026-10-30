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
package fish.payara.microprofile.openapi.impl.visitor;

import fish.payara.microprofile.openapi.api.visitor.ApiContext;
import fish.payara.microprofile.openapi.impl.model.util.ModelUtils;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import static java.util.logging.Level.WARNING;
import java.util.logging.Logger;
import static java.util.stream.Collectors.toSet;

import java.util.Collections;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Application;
import java.lang.reflect.InvocationTargetException;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;
import org.glassfish.hk2.classmodel.reflect.AnnotatedElement;
import org.glassfish.hk2.classmodel.reflect.AnnotationModel;
import org.glassfish.hk2.classmodel.reflect.ClassModel;
import org.glassfish.hk2.classmodel.reflect.ExtensibleType;
import org.glassfish.hk2.classmodel.reflect.MethodModel;
import org.glassfish.hk2.classmodel.reflect.Type;

public class OpenApiContext implements ApiContext {

    private final Map<String, Type> allTypes;
    private final ClassLoader appClassLoader;
    private final OpenAPI api;
    private final Set<Type> allowedTypes;
    private final Map<String, Set<Type>> resourceMapping;
    private String path;
    private Operation operation;
    private AnnotatedElement annotatedElement;

    private static final Logger LOGGER = Logger.getLogger(OpenApiContext.class.getName());

    private Map<ExtensibleType<? extends ExtensibleType>, AnnotationInfo> parsedTypes = new ConcurrentHashMap<>();

    private Map<String, Set<APIResponse>> mappedExceptionResponses = new ConcurrentHashMap<>();

    public OpenApiContext(Map<String, Type> allTypes, Set<Type> allowedTypes, ClassLoader appClassLoader, OpenAPI api) {
        this.allTypes = allTypes;
        this.allowedTypes = allowedTypes;
        this.api = api;
        this.appClassLoader = appClassLoader;
        this.resourceMapping = generateResourceMapping();
    }

    public OpenApiContext(OpenApiContext parentApiContext, AnnotatedElement annotatedElement) {
        this.allTypes = parentApiContext.allTypes;
        this.allowedTypes = parentApiContext.allowedTypes;
        this.api = parentApiContext.api;
        this.appClassLoader = parentApiContext.appClassLoader;
        this.resourceMapping = parentApiContext.resourceMapping;
        this.parsedTypes = parentApiContext.parsedTypes;
        this.mappedExceptionResponses = parentApiContext.mappedExceptionResponses;
        this.annotatedElement = annotatedElement;
    }

    @Override
    public OpenAPI getApi() {
        return api;
    }

    @Override
    public String getPath() {
        if (path == null) {
            path = getResourcePath(annotatedElement);
        }
        return path;
    }

    @Override
    public Operation getWorkingOperation() {
        if (operation == null && annotatedElement instanceof MethodModel) {
            operation = getOperation((MethodModel) annotatedElement);
        }
        return operation;
    }

    @Override
    public void addMappedExceptionResponse(String exceptionType, APIResponse exceptionResponse) {
        if (exceptionType != null) {
            if(mappedExceptionResponses.containsKey(exceptionType)) {
                mappedExceptionResponses.get(exceptionType).add(exceptionResponse);
            } else {
                Set set = new HashSet<>();
                set.add(exceptionResponse);
                mappedExceptionResponses.put(exceptionType, set);
            }
        }
    }

    @Override
    public Map<String, Set<APIResponse>> getMappedExceptionResponses() {
        return Collections.unmodifiableMap(mappedExceptionResponses);
    }

    @Override
    public boolean isAllowedType(Type type) {
        return allowedTypes.contains(type);
    }

    @Override
    public boolean isApplicationType(String type) {
        return allTypes.containsKey(type);
    }

    @Override
    public Type getType(String type) {
        return allTypes.get(type);
    }

    @Override
    public ClassLoader getApplicationClassLoader() {
        return appClassLoader;
    }

    @Override
    public AnnotationInfo getAnnotationInfo(ExtensibleType<? extends ExtensibleType> type) {
        return (AnnotationInfo) parsedTypes.computeIfAbsent(type, key -> new AnnotationInfo(key));
    }

    /**
     * Generates a map listing the location each resource class is mapped to.
     */
    private Map<String, Set<Type>> generateResourceMapping() {
        Set<Type> classList = new HashSet<>();
        Map<String, Set<Type>> mapping = new HashMap<>();
        for (Type type : allowedTypes) {
            if (type instanceof ClassModel) {
                ClassModel classModel = (ClassModel) type;
                if (classModel.getAnnotation(ApplicationPath.class.getName()) != null) {
                    // Produce the mapping
                    AnnotationModel annotation = classModel.getAnnotation(ApplicationPath.class.getName());
                    String key = annotation.getValue("value", String.class);
                    Set<Type> resourceClasses = new HashSet<>();
                    mapping.put(key, resourceClasses);
                    try {
                        Class<?> clazz = appClassLoader.loadClass(classModel.getName());
                        Application app = (Application) clazz.getDeclaredConstructor().newInstance();
                        // Add all classes contained in the application
                        resourceClasses.addAll(app.getClasses()
                                .stream()
                                .map(Class::getName)
                                .filter(name -> !name.startsWith("org.glassfish.jersey")) // Remove all Jersey providers
                                .map(allTypes::get)
                                .filter(Objects::nonNull)
                                .collect(toSet()));
                    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                            | NoSuchMethodException | IllegalArgumentException
                            | SecurityException | InvocationTargetException ex) {
                        LOGGER.log(WARNING, "Unable to initialise application class.", ex);
                    }
                } else {
                    classList.add(classModel);
                }
            }
        }

        // If there is one application and it's empty, add all classes
        if (mapping.keySet().size() == 1) {
            Set<Type> classes = mapping.values().iterator().next();
            if (classes.isEmpty()) {
                classes.addAll(classList);
            }
        }

        // If there is no application, add all classes to the context root.
        if (mapping.isEmpty()) {
            mapping.put("/", classList);
        }

        return mapping;
    }

    private org.eclipse.microprofile.openapi.models.Operation getOperation(MethodModel method) {
        String resourcePath = getResourcePath(method);
        if (resourcePath != null) {
            PathItem pathItem = api.getPaths().getPathItem(resourcePath);
            if (pathItem != null) {
                PathItem.HttpMethod httpMethod = ModelUtils.getHttpMethod(this, method);
                return pathItem.getOperations().get(httpMethod);
            }
        }
        return null;
    }

    private String getResourcePath(AnnotatedElement declaration) {
        if (declaration instanceof MethodModel) {
            return getResourcePath((MethodModel) declaration);
        } else if (declaration instanceof ClassModel) {
            return getResourcePath((ClassModel) declaration);
        }
        return null;
    }

    private String getResourcePath(ClassModel clazz) {
        // If the class is a resource and contains a mapping
        AnnotationInfo annotations = getAnnotationInfo(clazz);
        if (annotations.isAnnotationPresent(Path.class)) {
            for (Map.Entry<String, Set<Type>> entry : resourceMapping.entrySet()) {
                if (entry.getValue() != null && entry.getValue().contains(clazz)) {
                    return ModelUtils.normaliseUrl(entry.getKey() + "/" + annotations.getAnnotationValue(Path.class));
                }
            }
        }
        return null;
    }

    private String getResourcePath(MethodModel method) {
        AnnotationInfo annotations = getAnnotationInfo(method.getDeclaringType());
        if (annotations.isAnyAnnotationPresent(method,
                GET.class, POST.class, PUT.class, DELETE.class, HEAD.class, OPTIONS.class, PATCH.class, Path.class)) {
            if (annotations.isAnnotationPresent(Path.class, method)) {
                // If the method is a valid resource
                return ModelUtils.normaliseUrl(getResourcePath(method.getDeclaringType()) + "/"
                        + annotations.getAnnotationValue(Path.class, method));
            }
            return ModelUtils.normaliseUrl(getResourcePath(method.getDeclaringType()));
        }
        return null;
    }
}

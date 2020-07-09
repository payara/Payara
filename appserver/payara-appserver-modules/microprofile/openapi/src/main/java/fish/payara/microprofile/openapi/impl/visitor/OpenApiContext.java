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
package fish.payara.microprofile.openapi.impl.visitor;

import fish.payara.microprofile.openapi.api.visitor.ApiContext;
import fish.payara.microprofile.openapi.impl.model.util.AnnotationInfo;
import fish.payara.microprofile.openapi.impl.model.util.ModelUtils;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import static java.util.logging.Level.WARNING;
import java.util.logging.Logger;
import static java.util.stream.Collectors.toSet;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.glassfish.hk2.classmodel.reflect.AnnotatedElement;
import org.glassfish.hk2.classmodel.reflect.AnnotationModel;
import org.glassfish.hk2.classmodel.reflect.ClassModel;
import org.glassfish.hk2.classmodel.reflect.MethodModel;
import org.glassfish.hk2.classmodel.reflect.Type;
import org.glassfish.hk2.classmodel.reflect.Types;

public class OpenApiContext implements ApiContext {

    private final Types allTypes;
    private final ClassLoader appClassLoader;
    private final OpenAPI api;
    private final Set<Type> allowedTypes;
    private final Map<String, Set<Type>> resourceMapping;
    private String path;
    private Operation operation;
    private AnnotatedElement annotatedElement;

    private static final Logger LOGGER = Logger.getLogger(OpenApiContext.class.getName());

    public OpenApiContext(Types allTypes, Set<Type> allowedTypes, ClassLoader appClassLoader, OpenAPI api) {
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
    public boolean isAllowedType(Type type) {
        return allowedTypes.contains(type);
    }

    @Override
    public boolean isApplicationType(String type) {
        return allTypes.getBy(type) != null;
    }

    @Override
    public Type getType(String type) {
        return allTypes.getBy(type);
    }

    @Override
    public ClassLoader getApplicationClassLoader() {
        return appClassLoader;
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
                        Application app = (Application) clazz.newInstance();
                        // Add all classes contained in the application
                        resourceClasses.addAll(app.getClasses()
                                .stream()
                                .map(Class::getName)
                                .filter(name -> !name.startsWith("org.glassfish.jersey")) // Remove all Jersey providers
                                .map(allTypes::getBy)
                                .filter(Objects::nonNull)
                                .collect(toSet()));
                    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
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
                PathItem.HttpMethod httpMethod = ModelUtils.getHttpMethod(method);
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
        AnnotationInfo annotations = AnnotationInfo.valueOf(clazz);
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
        AnnotationInfo annotations = AnnotationInfo.valueOf(method.getDeclaringType());
        if (annotations.isAnyAnnotationPresent(method,
                GET.class, POST.class, PUT.class, DELETE.class, HEAD.class, OPTIONS.class, PATCH.class)) {
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

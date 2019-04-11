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

import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.getOperation;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.getResourcePath;
import static java.util.logging.Level.WARNING;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Application;

import org.eclipse.microprofile.openapi.annotations.ExternalDocumentation;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.callbacks.Callback;
import org.eclipse.microprofile.openapi.annotations.callbacks.Callbacks;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.extensions.Extensions;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameters;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirements;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.security.SecuritySchemes;
import org.eclipse.microprofile.openapi.annotations.servers.Server;
import org.eclipse.microprofile.openapi.annotations.servers.Servers;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.openapi.annotations.tags.Tags;
import org.eclipse.microprofile.openapi.models.OpenAPI;

import fish.payara.microprofile.openapi.api.visitor.ApiContext;
import fish.payara.microprofile.openapi.api.visitor.ApiVisitor;
import fish.payara.microprofile.openapi.api.visitor.ApiVisitor.VisitorFunction;
import fish.payara.microprofile.openapi.api.visitor.ApiWalker;
import fish.payara.microprofile.openapi.impl.model.util.AnnotationInfo;

/**
 * A walker that visits each annotation and passes it to the visitor.
 */
public class OpenApiWalker implements ApiWalker {

    private static final Logger LOGGER = Logger.getLogger(OpenApiWalker.class.getName());

    private final OpenAPI api;
    private final Set<Class<?>> classes;
    private final Map<String, Set<Class<?>>> resourceMapping;

    public OpenApiWalker(OpenAPI api, Set<Class<?>> allowedClasses) {
        this.api = api;
        this.classes = new TreeSet<>((class1, class2) -> {
            if (class1.equals(class2)) {
                return 0;
            }
            // Subclasses first
            if (class1.isAssignableFrom(class2)) {
                return -1;
            }
            // Non contextual objects at the start
            if (!class1.isAnnotationPresent(ApplicationPath.class) && !class1.isAnnotationPresent(Path.class)) {
                return -1;
            }
            // Followed by applications
            if (class1.isAnnotationPresent(ApplicationPath.class)) {
                return -1;
            }
            // Followed by everything else
            return 1;
        });
        this.classes.addAll(allowedClasses);
        addInnerClasses(); // must happen before generating the resource mapping
        this.resourceMapping = generateResourceMapping(classes);
    }

    private void addInnerClasses() {
        List<Class<?>> topLevelClasses = new ArrayList<>(classes);
        for (Class<?> topLevelClass : topLevelClasses) {
            addInnerClasses(topLevelClass);
        }
    }

    private void addInnerClasses(Class<?> topLevelClass) {
        if (topLevelClass != null) {
            classes.addAll(Arrays.asList(topLevelClass.getDeclaredClasses()));
            if (topLevelClass.getSuperclass() != Object.class) {
                addInnerClasses(topLevelClass.getSuperclass());
            }
        }
    }

    @Override
    public void accept(ApiVisitor visitor) {
        // OpenAPI necessary annotations
        processAnnotations(OpenAPIDefinition.class, visitor::visitOpenAPI);

        // JAX-RS methods
        processAnnotations(GET.class, visitor::visitGET);
        processAnnotations(POST.class, visitor::visitPOST);
        processAnnotations(PUT.class, visitor::visitPUT);
        processAnnotations(DELETE.class, visitor::visitDELETE);
        processAnnotations(HEAD.class, visitor::visitHEAD);
        processAnnotations(OPTIONS.class, visitor::visitOPTIONS);
        processAnnotations(PATCH.class, visitor::visitPATCH);

        // JAX-RS parameters
        processAnnotations(QueryParam.class, visitor::visitQueryParam);
        processAnnotations(PathParam.class, visitor::visitPathParam);
        processAnnotations(HeaderParam.class, visitor::visitHeaderParam);
        processAnnotations(CookieParam.class, visitor::visitCookieParam);
        processAnnotations(FormParam.class, visitor::visitFormParam);

        // All other OpenAPI annotations
        processAnnotations(Schema.class, visitor::visitSchema);
        processAnnotations(Server.class, visitor::visitServer, Servers.class);
        processAnnotations(Servers.class, visitor::visitServers, Server.class);
        processAnnotations(Extensions.class, visitor::visitExtensions, Extension.class);
        processAnnotations(Extension.class, visitor::visitExtension, Extensions.class);
        processAnnotations(Operation.class, visitor::visitOperation);
        processAnnotations(Callback.class, visitor::visitCallback, Callbacks.class);
        processAnnotations(Callbacks.class, visitor::visitCallbacks, Callback.class);
        processAnnotations(APIResponse.class, visitor::visitAPIResponse, APIResponses.class);
        processAnnotations(APIResponses.class, visitor::visitAPIResponses, APIResponse.class);
        processAnnotations(Parameters.class, visitor::visitParameters, Parameter.class);
        processAnnotations(Parameter.class, visitor::visitParameter, Parameters.class);
        processAnnotations(ExternalDocumentation.class, visitor::visitExternalDocumentation);
        processAnnotations(Tag.class, visitor::visitTag, Tags.class);
        processAnnotations(Tags.class, visitor::visitTags, Tag.class);
        processAnnotations(SecurityScheme.class, visitor::visitSecurityScheme, SecuritySchemes.class);
        processAnnotations(SecuritySchemes.class, visitor::visitSecuritySchemes, SecurityScheme.class);
        processAnnotations(SecurityRequirement.class, visitor::visitSecurityRequirement, SecurityRequirements.class);
        processAnnotations(SecurityRequirements.class, visitor::visitSecurityRequirements, SecurityRequirement.class);

        // JAX-RS response types
        processAnnotations(Produces.class, visitor::visitProduces);
        processAnnotations(Consumes.class, visitor::visitConsumes);

        // OpenAPI response types
        processAnnotations(RequestBody.class, visitor::visitRequestBody);
    }

    @SafeVarargs
    private final <A extends Annotation, E extends AnnotatedElement> void processAnnotations(
            Class<A> annotationClass, VisitorFunction<A, E> annotationFunction, 
            Class<? extends Annotation>... alternatives) {
        for (Class<?> clazz : classes) {
            processAnnotation(clazz, annotationClass, annotationFunction, alternatives);
        }
    }

    @SafeVarargs
    private final <T, A extends Annotation, E extends AnnotatedElement> void processAnnotation(
            Class<T> annotatedClass, Class<A> annotationClass, VisitorFunction<A, E> annotationFunction,
            Class<? extends Annotation>... alternatives) {
        AnnotationInfo<T> annotations = AnnotationInfo.valueOf(annotatedClass);
        processAnnotation(annotatedClass, annotationClass, annotationFunction, annotations,
                new OpenApiContext(classes, api, getResourcePath(annotatedClass, resourceMapping)), alternatives);

        for (Field field : annotatedClass.getDeclaredFields()) {
            if (annotations.isAnnotationPresent(annotationClass, field)) {
                if (   annotationClass == HeaderParam.class
                    || annotationClass == CookieParam.class
                    || annotationClass == PathParam.class
                    || annotationClass == QueryParam.class) {
                    // NB. if fields are annotated as Param all methods have it
                    for (Method method : annotatedClass.getDeclaredMethods()) {
                        OpenApiContext context = new OpenApiContext(classes, api,
                                getResourcePath(method, resourceMapping),
                                getOperation(method, api, resourceMapping));
                        if (context.getWorkingOperation() != null) {
                            processAnnotation(field, annotationClass, annotationFunction, annotations, context,
                                    alternatives);
                        }
                    }
                } else {
                    processAnnotation(field, annotationClass, annotationFunction, annotations,
                            new OpenApiContext(classes, api, null), alternatives);
                }
            }
        }

        for (final Method method : annotatedClass.getDeclaredMethods()) {
            OpenApiContext context = new OpenApiContext(classes, api,
                    getResourcePath(method, resourceMapping),
                    getOperation(method, api, resourceMapping));
            processAnnotation(method, annotationClass, annotationFunction, annotations, context, alternatives);

            for (java.lang.reflect.Parameter parameter : method.getParameters()) {
                processAnnotation(parameter, annotationClass, annotationFunction, annotations, context, alternatives);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @SafeVarargs
    private static <A extends Annotation, E extends AnnotatedElement> void processAnnotation(
            AnnotatedElement element, Class<A> annotationClass, VisitorFunction<A, E> annotationFunction, 
            AnnotationInfo<?> annotations, ApiContext context, Class<? extends Annotation>... alternatives) {
        // If it's just the one annotation class
        // Check the element
        if (annotations.isAnnotationPresent(annotationClass, element)) {
            annotationFunction.apply(annotations.getAnnotation(annotationClass, element), (E) element, context);
        } else if (element instanceof Method && annotations.isAnnotationPresent(annotationClass)
                && !annotations.isAnyAnnotationPresent(element, alternatives)) {
            // If the method isn't annotated, inherit the class annotation
            if (context.getPath() != null) {
                annotationFunction.apply(annotations.getAnnotation(annotationClass), (E) element, context);
            }
        }
    }

    /**
     * Generates a map listing the location each resource class is mapped to.
     */
    private static Map<String, Set<Class<?>>> generateResourceMapping(Set<Class<?>> classList) {
        Map<String, Set<Class<?>>> resourceMapping = new HashMap<>();
        for (Class<?> clazz : classList) {
            if (clazz.isAnnotationPresent(ApplicationPath.class) && Application.class.isAssignableFrom(clazz)) {
                // Produce the mapping
                String key = clazz.getDeclaredAnnotation(ApplicationPath.class).value();
                Set<Class<?>> resourceClasses = new HashSet<>();
                resourceMapping.put(key, resourceClasses);

                try {
                    Application app = (Application) clazz.newInstance();
                    // Add all classes contained in the application
                    resourceClasses.addAll(app.getClasses());
                    // Remove all Jersey providers
                    resourceClasses.removeIf(resource -> resource.getPackage().getName().contains("org.glassfish.jersey"));
                } catch (InstantiationException | IllegalAccessException ex) {
                    LOGGER.log(WARNING, "Unable to initialise application class.", ex);
                }
            }
        }

        // If there is one application and it's empty, add all classes
        if (resourceMapping.keySet().size() == 1) {
            Set<Class<?>> classes = resourceMapping.values().iterator().next();
            if (classes.isEmpty()) {
                classes.addAll(classList);
            }
        }

        // If there is no application, add all classes to the context root.
        if (resourceMapping.isEmpty()) {
            resourceMapping.put("/", classList);
        }

        return resourceMapping;
    }
}

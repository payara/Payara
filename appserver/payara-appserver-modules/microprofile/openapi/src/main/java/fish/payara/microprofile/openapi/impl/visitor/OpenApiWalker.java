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

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.eclipse.microprofile.openapi.annotations.ExternalDocumentation;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.callbacks.Callback;
import org.eclipse.microprofile.openapi.annotations.callbacks.Callbacks;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
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

/**
 * A walker that visits each annotation and passes it to the visitor.
 */
public class OpenApiWalker implements ApiWalker {

    private final OpenAPI api;
    private final Set<Class<?>> classes;
    private final Map<String, Set<Class<?>>> resourceMapping;

    public OpenApiWalker(OpenAPI api, Set<Class<?>> allowedClasses, Map<String, Set<Class<?>>> resourceMapping) {
        this.api = api;
        this.resourceMapping = resourceMapping;
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
    }

    @Override
    public void accept(ApiVisitor visitor) {
        // OpenAPI necessary annotations
        processAnnotations(OpenAPIDefinition.class, visitor::visitOpenAPI, true);
        processAnnotations(Schema.class, visitor::visitSchema, true);

        // JAX-RS methods
        processAnnotations(GET.class, visitor::visitGET);
        processAnnotations(POST.class, visitor::visitPOST);
        processAnnotations(PUT.class, visitor::visitPUT);
        processAnnotations(DELETE.class, visitor::visitDELETE);
        processAnnotations(HEAD.class, visitor::visitHEAD);
        processAnnotations(OPTIONS.class, visitor::visitOPTIONS);

        // JAX-RS parameters
        processAnnotations(QueryParam.class, visitor::visitQueryParam);
        processAnnotations(PathParam.class, visitor::visitPathParam);
        processAnnotations(HeaderParam.class, visitor::visitHeaderParam);
        processAnnotations(CookieParam.class, visitor::visitCookieParam);
        processAnnotations(FormParam.class, visitor::visitFormParam);

        // All other OpenAPI annotations
        processAnnotations(Schema.class, visitor::visitSchema, true);
        processAnnotations(Server.class, visitor::visitServer, Servers.class, visitor::visitServers, true);
        processAnnotations(Extension.class, visitor::visitExtension, true);
        processAnnotations(Operation.class, visitor::visitOperation);
        processAnnotations(Callback.class, visitor::visitCallback, Callbacks.class, visitor::visitCallbacks);
        processAnnotations(APIResponse.class, visitor::visitAPIResponse, APIResponses.class,
                visitor::visitAPIResponses, true);
        processAnnotations(Parameter.class, visitor::visitParameter, true);
        processAnnotations(ExternalDocumentation.class, visitor::visitExternalDocumentation, true);
        processAnnotations(Tag.class, visitor::visitTag, Tags.class, visitor::visitTags, true);
        processAnnotations(SecurityScheme.class, visitor::visitSecurityScheme, SecuritySchemes.class,
                visitor::visitSecuritySchemes, true);
        processAnnotations(SecurityRequirement.class, visitor::visitSecurityRequirement, SecurityRequirements.class,
                visitor::visitSecurityRequirements, true);

        // JAX-RS response types
        processAnnotations(Produces.class, visitor::visitProduces);
        processAnnotations(Consumes.class, visitor::visitConsumes);
        processAnnotations(RequestBody.class, visitor::visitRequestBody);
    }

    private <A extends Annotation, A2 extends Annotation, E extends AnnotatedElement> void processAnnotations(
            Class<A> annotationClass, VisitorFunction<A, E> annotationFunction, Class<A2> altClass,
            VisitorFunction<A2, E> altFunction, boolean allowInterfaces) {
        for (Class<?> clazz : classes) {

            // Don't read interfaces if the annotation should only be read from concrete classes
            if (!allowInterfaces && (Modifier.isAbstract(clazz.getModifiers()) || clazz.isInterface())) {
                continue;
            }

            processAnnotation(clazz, annotationClass, annotationFunction, altClass, altFunction,
                    new OpenApiContext(api, getResourcePath(clazz, resourceMapping)));

            for (Field field : clazz.getDeclaredFields()) {
                processAnnotation(field, annotationClass, annotationFunction, altClass, altFunction,
                        new OpenApiContext(api, null));
            }

            for (Method method : clazz.getDeclaredMethods()) {

                processAnnotation(method, annotationClass, annotationFunction, altClass, altFunction,
                        new OpenApiContext(api, getResourcePath(method, resourceMapping), getOperation(method, api, resourceMapping)));

                for (java.lang.reflect.Parameter parameter : method.getParameters()) {
                    processAnnotation(parameter, annotationClass, annotationFunction, altClass, altFunction,
                            new OpenApiContext(api, getResourcePath(method, resourceMapping), getOperation(method, api, resourceMapping)));
                }
            }
        }
    }

    private <A extends Annotation, E extends AnnotatedElement> void processAnnotations(Class<A> annotationClass,
            VisitorFunction<A, E> function) {
        processAnnotations(annotationClass, function, false);
    }

    private <A extends Annotation, E extends AnnotatedElement> void processAnnotations(Class<A> annotationClass,
            VisitorFunction<A, E> function, boolean allowInterfaces) {
        processAnnotations(annotationClass, function, null, null, allowInterfaces);
    }

    private <A extends Annotation, A2 extends Annotation, E extends AnnotatedElement> void processAnnotations(
            Class<A> annotationClass, VisitorFunction<A, E> annotationFunction, Class<A2> altClass,
            VisitorFunction<A2, E> altFunction) {
        processAnnotations(annotationClass, annotationFunction, altClass, altFunction, false);
    }

    @SuppressWarnings("unchecked")
    private <A extends Annotation, A2 extends Annotation, E extends AnnotatedElement> void processAnnotation(
            AnnotatedElement element, Class<A> annotationClass, VisitorFunction<A, E> annotationFunction,
            Class<A2> altClass, VisitorFunction<A2, E> altFunction, ApiContext context) {
        // If it's just the one annotation class
        if (altClass == null) {
            // Check the element
            if (element.isAnnotationPresent(annotationClass)) {
                annotationFunction.apply(element.getDeclaredAnnotation(annotationClass), (E) element, context);
            } else if (element instanceof Method
                    && Method.class.cast(element).getDeclaringClass().isAnnotationPresent(annotationClass)) {
                // If the method isn't annotated, inherit the class annotation
                if (context.getPath() != null) {
                    annotationFunction.apply(
                            Method.class.cast(element).getDeclaringClass().getDeclaredAnnotation(annotationClass),
                            (E) element, context);
                }
            }
        } else {
            // If it's annotated with both
            if (element.isAnnotationPresent(annotationClass) || element.isAnnotationPresent(altClass)) {
                if (element.isAnnotationPresent(annotationClass)) {
                    annotationFunction.apply(element.getDeclaredAnnotation(annotationClass), (E) element, context);
                }
                if (element.isAnnotationPresent(altClass)) {
                    altFunction.apply(element.getDeclaredAnnotation(altClass), (E) element, context);
                }
            } else if (element instanceof Method && context.getPath() != null) {
                Class<?> declaringClass = Method.class.cast(element).getDeclaringClass();
                if (declaringClass.isAnnotationPresent(annotationClass)) {
                    annotationFunction.apply(declaringClass.getDeclaredAnnotation(annotationClass), (E) element,
                            context);
                }
                if (declaringClass.isAnnotationPresent(altClass)) {
                    altFunction.apply(declaringClass.getDeclaredAnnotation(altClass), (E) element, context);
                }
            }
        }
    }
}

/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2018-2020] Payara Foundation and/or its affiliates. All rights reserved.
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

import fish.payara.microprofile.openapi.api.visitor.ApiVisitor;
import fish.payara.microprofile.openapi.api.visitor.ApiVisitor.VisitorFunction;
import fish.payara.microprofile.openapi.api.visitor.ApiWalker;
import java.lang.annotation.Annotation;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
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
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
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
import org.glassfish.hk2.classmodel.reflect.AnnotatedElement;
import org.glassfish.hk2.classmodel.reflect.AnnotationModel;
import org.glassfish.hk2.classmodel.reflect.ClassModel;
import org.glassfish.hk2.classmodel.reflect.FieldModel;
import org.glassfish.hk2.classmodel.reflect.MethodModel;
import org.glassfish.hk2.classmodel.reflect.Type;
import org.glassfish.hk2.classmodel.reflect.Types;

/**
 * A walker that visits each filtered class type & it's members, scans for
 * OpenAPI annotations and passes it to the visitor.
 */
public class OpenApiWalker<E extends AnnotatedElement> implements ApiWalker {

    private final Set<Type> allowedTypes;
    private final OpenApiContext context;

    private Map<Class<? extends Annotation>, VisitorFunction<AnnotationModel, E>> annotationVisitor;
    private Map<Class<? extends Annotation>, Class<? extends Annotation>> annotationAlternatives;

    public OpenApiWalker(OpenAPI api, Types allTypes, Set<Type> allowedTypes, ClassLoader appClassLoader) {
        this.allowedTypes = new TreeSet<>(Comparator.comparing(Type::getName, String::compareTo));
        this.allowedTypes.addAll(allowedTypes);
        this.context = new OpenApiContext(allTypes, this.allowedTypes, appClassLoader, api);
    }

    @Override
    public void accept(ApiVisitor visitor) {
        for (Type type : allowedTypes) {
            if (type instanceof ClassModel) {
                processAnnotation((ClassModel) type, visitor);
            }
        }
    }

    public final void processAnnotation(ClassModel annotatedClass, ApiVisitor visitor) {
        AnnotationInfo annotations = context.getAnnotationInfo(annotatedClass);
        processAnnotation((E) annotatedClass, annotations, visitor, new OpenApiContext(context, annotatedClass));

        for (final MethodModel method : annotatedClass.getMethods()) {
            processAnnotation((E) method, annotations, visitor, new OpenApiContext(context, method));
        }

        for (final FieldModel field : annotatedClass.getFields()) {
            processAnnotation((E) field, annotations, visitor, new OpenApiContext(context, field));
        }

        for (final MethodModel method : annotatedClass.getMethods()) {
            for (org.glassfish.hk2.classmodel.reflect.Parameter parameter : method.getParameters()) {
                processAnnotation((E) parameter, annotations, visitor, new OpenApiContext(context, method));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void processAnnotation(E element, AnnotationInfo annotations, ApiVisitor visitor, OpenApiContext context) {

        for (Class<? extends Annotation> annotationClass : getAnnotationVisitor(visitor).keySet()) {
            VisitorFunction<AnnotationModel, E> annotationFunction = getAnnotationVisitor(visitor).get(annotationClass);
            Class<? extends Annotation> alternative = getAnnotationAlternatives().get(annotationClass);
            // If it's just the one annotation class
            // Check the element
            if (annotations.isAnnotationPresent(annotationClass, element)) {
                if (element instanceof FieldModel
                        && (annotationClass == HeaderParam.class
                        || annotationClass == CookieParam.class
                        || annotationClass == PathParam.class
                        || annotationClass == QueryParam.class)) {
                    FieldModel field = (FieldModel) element;
                    // NB. if fields are annotated as Param all methods have it
                    for (MethodModel method : field.getDeclaringType().getMethods()) {
                        OpenApiContext methodContext = new OpenApiContext(context, method);
                        if (methodContext.getWorkingOperation() != null) {
                            annotationFunction.apply(annotations.getAnnotation(annotationClass, element), element, methodContext);
                        }
                    }
                } else {
                    annotationFunction.apply(annotations.getAnnotation(annotationClass, element), element, context);
                }
            } else if (element instanceof MethodModel && annotations.isAnnotationPresent(annotationClass)
                    && (alternative == null || !annotations.isAnnotationPresent(alternative, element))) {
                // If the method isn't annotated, inherit the class annotation
                if (context.getPath() != null) {
                    annotationFunction.apply(annotations.getAnnotation(annotationClass), element, context);
                }
            }
        }
    }

    private Map<Class<? extends Annotation>, VisitorFunction<AnnotationModel, E>> getAnnotationVisitor(ApiVisitor visitor) {
        if (annotationVisitor == null) {
            annotationVisitor = new LinkedHashMap<>();

            // OpenAPI necessary annotations
            annotationVisitor.put(OpenAPIDefinition.class, visitor::visitOpenAPI);

            // JAX-RS methods
            annotationVisitor.put(GET.class, (annot, element, con) -> visitor.visitGET(annot, (MethodModel) element, con));
            annotationVisitor.put(POST.class, (annot, element, con) -> visitor.visitPOST(annot, (MethodModel) element, con));
            annotationVisitor.put(PUT.class, (annot, element, con) -> visitor.visitPUT(annot, (MethodModel) element, con));
            annotationVisitor.put(DELETE.class, (annot, element, con) -> visitor.visitDELETE(annot, (MethodModel) element, con));
            annotationVisitor.put(HEAD.class, (annot, element, con) -> visitor.visitHEAD(annot, (MethodModel) element, con));
            annotationVisitor.put(OPTIONS.class, (annot, element, con) -> visitor.visitOPTIONS(annot, (MethodModel) element, con));
            annotationVisitor.put(PATCH.class, (annot, element, con) -> visitor.visitPATCH(annot, (MethodModel) element, con));

            // JAX-RS parameters
            annotationVisitor.put(QueryParam.class, visitor::visitQueryParam);
            annotationVisitor.put(PathParam.class, visitor::visitPathParam);
            annotationVisitor.put(HeaderParam.class, visitor::visitHeaderParam);
            annotationVisitor.put(CookieParam.class, visitor::visitCookieParam);
            annotationVisitor.put(FormParam.class, visitor::visitFormParam);

            // All other OpenAPI annotations
            annotationVisitor.put(Schema.class, visitor::visitSchema);
            annotationVisitor.put(Server.class, visitor::visitServer);
            annotationVisitor.put(Servers.class, visitor::visitServers);
            annotationVisitor.put(Extensions.class, visitor::visitExtensions);
            annotationVisitor.put(Extension.class, visitor::visitExtension);
            annotationVisitor.put(Operation.class, visitor::visitOperation);
            annotationVisitor.put(Callback.class, visitor::visitCallback);
            annotationVisitor.put(Callbacks.class, visitor::visitCallbacks);
            annotationVisitor.put(APIResponse.class, visitor::visitAPIResponse);
            annotationVisitor.put(APIResponses.class, visitor::visitAPIResponses);
            annotationVisitor.put(Parameters.class, visitor::visitParameters);
            annotationVisitor.put(Parameter.class, visitor::visitParameter);
            annotationVisitor.put(ExternalDocumentation.class, visitor::visitExternalDocumentation);
            annotationVisitor.put(Tag.class, visitor::visitTag);
            annotationVisitor.put(Tags.class, visitor::visitTags);
            annotationVisitor.put(SecurityScheme.class, visitor::visitSecurityScheme);
            annotationVisitor.put(SecuritySchemes.class, visitor::visitSecuritySchemes);
            annotationVisitor.put(SecurityRequirement.class, visitor::visitSecurityRequirement);
            annotationVisitor.put(SecurityRequirements.class, visitor::visitSecurityRequirements);

            // JAX-RS response
            annotationVisitor.put(Produces.class, visitor::visitProduces);
            annotationVisitor.put(Consumes.class, visitor::visitConsumes);

            // OpenAPI response
            annotationVisitor.put(RequestBody.class, visitor::visitRequestBody);
        }
        return annotationVisitor;
    }

    private Map<Class<? extends Annotation>, Class<? extends Annotation>> getAnnotationAlternatives() {
        if (annotationAlternatives == null) {
            annotationAlternatives = new HashMap<>();
            annotationAlternatives.put(Server.class, Servers.class);
            annotationAlternatives.put(Servers.class, Server.class);
            annotationAlternatives.put(Extensions.class, Extension.class);
            annotationAlternatives.put(Extension.class, Extensions.class);
            annotationAlternatives.put(Callback.class, Callbacks.class);
            annotationAlternatives.put(Callbacks.class, Callback.class);
            annotationAlternatives.put(APIResponse.class, APIResponses.class);
            annotationAlternatives.put(APIResponses.class, APIResponse.class);
            annotationAlternatives.put(Parameters.class, Parameter.class);
            annotationAlternatives.put(Parameter.class, Parameters.class);
            annotationAlternatives.put(Tag.class, Tags.class);
            annotationAlternatives.put(Tags.class, Tag.class);
            annotationAlternatives.put(SecurityScheme.class, SecuritySchemes.class);
            annotationAlternatives.put(SecuritySchemes.class, SecurityScheme.class);
            annotationAlternatives.put(SecurityRequirement.class, SecurityRequirements.class);
            annotationAlternatives.put(SecurityRequirements.class, SecurityRequirement.class);
        }
        return annotationAlternatives;
    }

}

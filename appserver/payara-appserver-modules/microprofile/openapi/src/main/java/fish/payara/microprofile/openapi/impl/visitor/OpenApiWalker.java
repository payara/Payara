/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2018-2025] Payara Foundation and/or its affiliates. All rights reserved.
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

import fish.payara.microprofile.openapi.api.visitor.ApiVisitor;
import fish.payara.microprofile.openapi.api.visitor.ApiVisitor.VisitorFunction;
import fish.payara.microprofile.openapi.api.visitor.ApiWalker;
import fish.payara.microprofile.openapi.impl.model.ExtensibleImpl;
import fish.payara.microprofile.openapi.impl.model.media.SchemaImpl;
import java.lang.annotation.Annotation;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Negative;
import jakarta.validation.constraints.NegativeOrZero;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import static java.util.Collections.singletonList;
import java.util.List;

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
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBodySchema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponseSchema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirements;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirementsSet;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirementsSets;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.security.SecuritySchemes;
import org.eclipse.microprofile.openapi.annotations.servers.Server;
import org.eclipse.microprofile.openapi.annotations.servers.Servers;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.openapi.annotations.tags.Tags;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.glassfish.hk2.classmodel.reflect.AnnotatedElement;
import org.glassfish.hk2.classmodel.reflect.AnnotationModel;
import org.glassfish.hk2.classmodel.reflect.ClassModel;
import org.glassfish.hk2.classmodel.reflect.FieldModel;
import org.glassfish.hk2.classmodel.reflect.MethodModel;
import org.glassfish.hk2.classmodel.reflect.Type;

/**
 * A walker that visits each filtered class type & it's members, scans for
 * OpenAPI annotations and passes it to the visitor.
 */
public class OpenApiWalker<E extends AnnotatedElement> implements ApiWalker {

    private final Set<Type> allowedTypes;
    private final OpenApiContext context;

    private Map<Class<? extends Annotation>, VisitorFunction<AnnotationModel, E>> annotationVisitor;
    private Map<Class<? extends Annotation>, List<Class<? extends Annotation>>> annotationAlternatives;

    private final boolean scanBeanValidation;

    public OpenApiWalker(OpenAPI api, Map<String, Type> allTypes, Set<Type> allowedTypes, ClassLoader appClassLoader, boolean scanBeanValidation) {
        this.allowedTypes = new TreeSet<>(Comparator.comparing(Type::getName, String::compareTo));
        this.allowedTypes.addAll(allowedTypes);
        this.context = new OpenApiContext(allTypes, this.allowedTypes, appClassLoader, api);
        this.scanBeanValidation = scanBeanValidation;
    }

    @Override
    public void accept(ApiVisitor visitor) {
        for (Type type : allowedTypes) {
            if (type instanceof ClassModel) {
                processAnnotation((ClassModel) type, visitor);
            }
        }
        syncSchemas();
    }

    @SuppressWarnings("unchecked")
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

    private void processAnnotation(E element, AnnotationInfo annotations, ApiVisitor visitor, OpenApiContext context) {

        for (Class<? extends Annotation> annotationClass : getAnnotationVisitor(visitor).keySet()) {
            VisitorFunction<AnnotationModel, E> annotationFunction = getAnnotationVisitor(visitor).get(annotationClass);
            List<Class<? extends Annotation>> alternatives = getAnnotationAlternatives().get(annotationClass);

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
                    // if annotation requires merging from both class and method (like APIResponse(s) does, call class first
                    if (element instanceof MethodModel && (annotationClass == APIResponse.class || annotationClass == APIResponses.class)) {
                        if (annotations.isAnnotationPresent(annotationClass)) {
                            annotationFunction.apply(annotations.getAnnotation(annotationClass), element, context);
                        }
                    }
                    // process the annotation by its function
                    annotationFunction.apply(annotations.getAnnotation(annotationClass, element), element, context);
                }
            } else if (element instanceof MethodModel && annotations.isAnnotationPresent(annotationClass)) {
                boolean process = true;
                if (alternatives != null) {
                    for (Class<? extends Annotation> alternative : alternatives) {
                        if (annotations.isAnnotationPresent(alternative, element)) {
                            process = false;
                            break;
                        }
                    }
                }
                // If the method isn't annotated, inherit the class annotation
                if (process && context.getPath() != null) {
                    annotationFunction.apply(annotations.getAnnotation(annotationClass), element, context);
                }
            }
        }
        // at the end, propagate @Extension defined on method to the @APIResponses (see javadoc of propagateExtension).
        if (element instanceof MethodModel && annotations.isAnnotationPresent(Extension.class, element)) {
            OpenApiContext methodContext = new OpenApiContext(context, element);
            propagateExtension(methodContext);
        }
    }

    /**
     * Propagate @Extension from method-level down to @APIResponses as required
     * by MP OpenApi TCK (testExtensionPlacement,
     * <verbatim><code>
     * vr.body(opPath + ".responses.'503'", hasEntry(equalTo(X_OPERATION_EXT),
     * equalTo(TEST_OPERATION_EXT)));
     * </code></verbatim>
     *
     * This is nonsense, when this test will be removed, remove also this
     * method!
     */
    private void propagateExtension(OpenApiContext methodContext) {
        for (org.eclipse.microprofile.openapi.models.responses.APIResponse apiResponse : methodContext.getWorkingOperation().getResponses().getAPIResponses().values()) {
            if (apiResponse.getExtensions() == null) {
                // only if empty
                ExtensibleImpl.merge(methodContext.getWorkingOperation(), apiResponse, true);
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
            annotationVisitor.put(Path.class, (annot, element, con) -> {
                if (element instanceof MethodModel && element.getAnnotations().size() == 1) {
                    AnnotationModel annotationModel = element.getAnnotations().iterator().next();
                    if ("Path".equals(annotationModel.getType().getSimpleName())) {
                        visitor.visitGET(annot, (MethodModel) element, con);
                    }
                }
            });

            // JAX-RS parameters
            annotationVisitor.put(QueryParam.class, visitor::visitQueryParam);
            annotationVisitor.put(PathParam.class, visitor::visitPathParam);
            annotationVisitor.put(HeaderParam.class, visitor::visitHeaderParam);
            annotationVisitor.put(CookieParam.class, visitor::visitCookieParam);
            annotationVisitor.put(FormParam.class, visitor::visitFormParam);

            // Visit Schema objects
            annotationVisitor.put(Schema.class, visitor::visitSchema);
            annotationVisitor.put(XmlRootElement.class, visitor::visitSchema);

            // Bean Validation annotations
            if (scanBeanValidation) {
                annotationVisitor.put(NotEmpty.class, visitor::visitNotEmpty);
                annotationVisitor.put(NotBlank.class, visitor::visitNotBlank);
                annotationVisitor.put(Size.class, visitor::visitSize);
                annotationVisitor.put(DecimalMax.class, visitor::visitDecimalMax);
                annotationVisitor.put(DecimalMin.class, visitor::visitDecimalMin);
                annotationVisitor.put(Max.class, visitor::visitMax);
                annotationVisitor.put(Min.class, visitor::visitMin);
                annotationVisitor.put(Negative.class, visitor::visitNegative);
                annotationVisitor.put(NegativeOrZero.class, visitor::visitNegativeOrZero);
                annotationVisitor.put(Positive.class, visitor::visitPositive);
                annotationVisitor.put(PositiveOrZero.class, visitor::visitPositiveOrZero);
            }

            // All other OpenAPI annotations
            annotationVisitor.put(Server.class, visitor::visitServer);
            annotationVisitor.put(Servers.class, visitor::visitServers);
            annotationVisitor.put(Extensions.class, visitor::visitExtensions);
            annotationVisitor.put(Extension.class, visitor::visitExtension);
            annotationVisitor.put(Operation.class, visitor::visitOperation);
            annotationVisitor.put(Callback.class, visitor::visitCallback);
            annotationVisitor.put(Callbacks.class, visitor::visitCallbacks);
            annotationVisitor.put(APIResponse.class, visitor::visitAPIResponse);
            annotationVisitor.put(APIResponses.class, visitor::visitAPIResponses);
            annotationVisitor.put(APIResponseSchema.class, visitor::visitAPIResponseSchema);
            annotationVisitor.put(Parameters.class, visitor::visitParameters);
            annotationVisitor.put(Parameter.class, visitor::visitParameter);
            annotationVisitor.put(ExternalDocumentation.class, visitor::visitExternalDocumentation);
            annotationVisitor.put(Tag.class, visitor::visitTag);
            annotationVisitor.put(Tags.class, visitor::visitTags);
            annotationVisitor.put(SecurityScheme.class, visitor::visitSecurityScheme);
            annotationVisitor.put(SecuritySchemes.class, visitor::visitSecuritySchemes);
            annotationVisitor.put(SecurityRequirement.class, visitor::visitSecurityRequirement);
            annotationVisitor.put(SecurityRequirements.class, visitor::visitSecurityRequirements);
            annotationVisitor.put(SecurityRequirementsSet.class, visitor::visitSecurityRequirementSet);
            annotationVisitor.put(SecurityRequirementsSets.class, visitor::visitSecurityRequirementSets);

            // JAX-RS response
            annotationVisitor.put(Produces.class, visitor::visitProduces);
            annotationVisitor.put(Consumes.class, visitor::visitConsumes);

            // OpenAPI response
            annotationVisitor.put(RequestBody.class, visitor::visitRequestBody);
            annotationVisitor.put(RequestBodySchema.class, visitor::visitRequestBodySchema);
        }
        return annotationVisitor;
    }

    private Map<Class<? extends Annotation>, List<Class<? extends Annotation>>> getAnnotationAlternatives() {
        if (annotationAlternatives == null) {
            annotationAlternatives = new HashMap<>();
            annotationAlternatives.put(Server.class, singletonList(Servers.class));
            annotationAlternatives.put(Servers.class, singletonList(Server.class));
            annotationAlternatives.put(Extensions.class, singletonList(Extension.class));
            annotationAlternatives.put(Extension.class, singletonList(Extensions.class));
            annotationAlternatives.put(Callback.class, singletonList(Callbacks.class));
            annotationAlternatives.put(Callbacks.class, singletonList(Callback.class));
            annotationAlternatives.put(APIResponse.class, singletonList(APIResponses.class));
            annotationAlternatives.put(APIResponses.class, singletonList(APIResponse.class));
            annotationAlternatives.put(Parameters.class, singletonList(Parameter.class));
            annotationAlternatives.put(Parameter.class, singletonList(Parameters.class));
            annotationAlternatives.put(Tag.class, singletonList(Tags.class));
            annotationAlternatives.put(Tags.class, singletonList(Tag.class));
            annotationAlternatives.put(SecurityScheme.class, singletonList(SecuritySchemes.class));
            annotationAlternatives.put(SecuritySchemes.class, singletonList(SecurityScheme.class));
            annotationAlternatives.put(SecurityRequirement.class, new ArrayList<>() {
                {
                    add(SecurityRequirements.class);
                    add(SecurityRequirementsSet.class);
                    add(SecurityRequirementsSets.class);
                }
            });
            annotationAlternatives.put(SecurityRequirements.class, new ArrayList<>() {
                {
                    add(SecurityRequirement.class);
                    add(SecurityRequirementsSet.class);
                    add(SecurityRequirementsSets.class);
                }
            });
            annotationAlternatives.put(SecurityRequirementsSet.class, new ArrayList<>() {
                {
                    add(SecurityRequirement.class);
                    add(SecurityRequirements.class);
                    add(SecurityRequirementsSets.class);
                }
            });
            annotationAlternatives.put(SecurityRequirementsSets.class, new ArrayList<>() {
                {
                    add(SecurityRequirement.class);
                    add(SecurityRequirements.class);
                    add(SecurityRequirementsSet.class);
                }
            });
        }
        return annotationAlternatives;
    }
    
    private void syncSchemas() {
        OpenAPI api = context.getApi();
        for (Map.Entry<String, org.eclipse.microprofile.openapi.models.media.Schema> schemaEntry : context.getApi().getComponents().getSchemas().entrySet()) {
            if (schemaEntry.getValue() != null && schemaEntry.getValue().getProperties() != null) {
                for (Map.Entry<String, org.eclipse.microprofile.openapi.models.media.Schema> propSchemaEntry : schemaEntry.getValue().getProperties().entrySet()) {
                    if (propSchemaEntry.getValue() instanceof SchemaImpl) {
                        SchemaImpl schemaImpl = (SchemaImpl) propSchemaEntry.getValue();
                        if (schemaImpl.getImplementation() != null) {
                            String[] implQualified = schemaImpl.getImplementation().split("\\.");
                            org.eclipse.microprofile.openapi.models.media.Schema from = context.getApi().getComponents().getSchemas().get(implQualified[implQualified.length - 1]);
                            SchemaImpl.merge(from, schemaImpl, false, context);
                        }
                    }
                }
            }
        }
        api.getPaths().getPathItems().forEach((String s, PathItem t) -> {
            t.getOperations().forEach((PathItem.HttpMethod u, org.eclipse.microprofile.openapi.models.Operation v) -> {
                v.getResponses().getAPIResponses().forEach((String w, org.eclipse.microprofile.openapi.models.responses.APIResponse x) -> {
                    if (x.getContent() != null) {
                        x.getContent().getMediaTypes().forEach((y, z) -> {
                            if (z.getSchema() instanceof SchemaImpl) {
                                SchemaImpl toschema = (SchemaImpl) z.getSchema();
                                if (toschema.getImplementation() != null) {
                                    String[] implQualified = toschema.getImplementation().split("\\.");
                                    String schemaClassName = implQualified[implQualified.length - 1];
                                    if (schemaClassName.contains("$")) {
                                        schemaClassName = schemaClassName.substring(schemaClassName.indexOf("$") + 1);
                                    }
                                    org.eclipse.microprofile.openapi.models.media.Schema from = context.getApi().getComponents().getSchemas().get(schemaClassName);
                                    if (from != null) {
                                        SchemaImpl.merge(from, toschema, false, context);
                                    } else {
                                        for (org.eclipse.microprofile.openapi.models.media.Schema fromSchema : context.getApi().getComponents().getSchemas().values()) {
                                            if(fromSchema instanceof SchemaImpl) {
                                                SchemaImpl fromSchemaImpl = (SchemaImpl) fromSchema;
                                                if (fromSchemaImpl.getImplementation() != null
                                                        && fromSchemaImpl.getImplementation().equals(toschema.getImplementation())) {
                                                    SchemaImpl.merge(fromSchemaImpl, toschema, false, context);
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        });
                    }
                });
            });
        });

    }

}

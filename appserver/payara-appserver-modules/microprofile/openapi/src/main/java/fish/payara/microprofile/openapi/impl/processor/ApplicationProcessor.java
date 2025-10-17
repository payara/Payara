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
package fish.payara.microprofile.openapi.impl.processor;

import fish.payara.microprofile.openapi.api.processor.OASProcessor;
import fish.payara.microprofile.openapi.api.visitor.ApiContext;
import fish.payara.microprofile.openapi.api.visitor.ApiVisitor;
import fish.payara.microprofile.openapi.impl.config.OpenApiConfiguration;
import fish.payara.microprofile.openapi.impl.model.ExtensibleImpl;
import fish.payara.microprofile.openapi.impl.model.ExternalDocumentationImpl;
import fish.payara.microprofile.openapi.impl.model.OpenAPIImpl;
import fish.payara.microprofile.openapi.impl.model.OperationImpl;
import fish.payara.microprofile.openapi.impl.model.PathItemImpl;
import fish.payara.microprofile.openapi.impl.model.callbacks.CallbackImpl;
import fish.payara.microprofile.openapi.impl.model.media.ContentImpl;
import fish.payara.microprofile.openapi.impl.model.media.MediaTypeImpl;
import fish.payara.microprofile.openapi.impl.model.media.SchemaImpl;
import fish.payara.microprofile.openapi.impl.model.parameters.ParameterImpl;
import fish.payara.microprofile.openapi.impl.model.parameters.RequestBodyImpl;
import fish.payara.microprofile.openapi.impl.model.responses.APIResponseImpl;
import fish.payara.microprofile.openapi.impl.model.responses.APIResponsesImpl;
import fish.payara.microprofile.openapi.impl.model.security.SecurityRequirementImpl;
import fish.payara.microprofile.openapi.impl.model.security.SecuritySchemeImpl;
import fish.payara.microprofile.openapi.impl.model.servers.ServerImpl;
import fish.payara.microprofile.openapi.impl.model.tags.TagImpl;
import fish.payara.microprofile.openapi.impl.model.util.ModelUtils;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.isVoid;
import fish.payara.microprofile.openapi.impl.visitor.OpenApiWalker;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import fish.payara.microprofile.openapi.util.BeanValidationType;
import jakarta.validation.groups.Default;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response.Status;
import java.util.Map;
import java.util.stream.Stream;

import org.eclipse.microprofile.openapi.models.Components;
import org.eclipse.microprofile.openapi.models.ExternalDocumentation;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.Reference;
import org.eclipse.microprofile.openapi.models.callbacks.Callback;
import org.eclipse.microprofile.openapi.models.media.MediaType;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.media.Schema.SchemaType;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.eclipse.microprofile.openapi.models.parameters.Parameter.In;
import org.eclipse.microprofile.openapi.models.parameters.RequestBody;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;
import org.eclipse.microprofile.openapi.models.responses.APIResponses;
import org.eclipse.microprofile.openapi.models.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;
import org.eclipse.microprofile.openapi.models.servers.Server;
import org.eclipse.microprofile.openapi.models.tags.Tag;
import org.glassfish.hk2.classmodel.reflect.AnnotatedElement;
import org.glassfish.hk2.classmodel.reflect.AnnotationModel;
import org.glassfish.hk2.classmodel.reflect.ClassModel;
import org.glassfish.hk2.classmodel.reflect.EnumType;
import org.glassfish.hk2.classmodel.reflect.ExtensibleType;
import org.glassfish.hk2.classmodel.reflect.FieldModel;
import org.glassfish.hk2.classmodel.reflect.MethodModel;
import org.glassfish.hk2.classmodel.reflect.ParameterizedInterfaceModel;
import org.glassfish.hk2.classmodel.reflect.ParameterizedType;
import org.glassfish.hk2.classmodel.reflect.Type;

/**
 * A processor to parse the application for annotations, to add to the OpenAPI
 * model.
 */
public class ApplicationProcessor implements OASProcessor, ApiVisitor {

    private static final Logger LOGGER = Logger.getLogger(ApplicationProcessor.class.getName());

    /**
     * A list of all classes in the given application.
     */
    private final Map<String, Type> allTypes;

    /**
     * A list of allowed classes for scanning
     */
    private final Set<Type> allowedTypes;
    private final Set<Type> allowedResourceTypes;

    private final ClassLoader appClassLoader;

    private OpenApiWalker<?> apiWalker;

    /**
     * @param allTypes parsed application classes
     * @param allowedTypes filtered application classes for OpenAPI metadata
     * processing
     * @param appClassLoader the class loader for the application.
     */
    public ApplicationProcessor(Map<String, Type> allTypes, Set<Type> allowedTypes, Set<Type> allowedResourceTypes, ClassLoader appClassLoader) {
        this.allTypes = allTypes;
        this.allowedTypes = allowedTypes;
        this.allowedResourceTypes = allowedResourceTypes;
        this.appClassLoader = appClassLoader;
    }

    @Override
    public OpenAPI process(OpenAPI api, OpenApiConfiguration config) {
        if (config == null || !config.getScanDisable()) {
            this.apiWalker = new OpenApiWalker<>(
                    api,
                    allTypes,
                    config == null ? allowedTypes : config.getValidClasses(allowedTypes),
                    appClassLoader,
                    config == null || config.getScanBeanValidation()
            );
            apiWalker.accept(this);
        }
        return api;
    }

    // JAX-RS method handlers
    @Override
    public void visitGET(AnnotationModel get, MethodModel element, ApiContext context) {
        if (context.getPath() == null) {
            return;
        }

        // Get or create the path item
        PathItem pathItem = context.getApi().getPaths().getPathItems().getOrDefault(context.getPath(), new PathItemImpl());
        context.getApi().getPaths().addPathItem(context.getPath(), pathItem);

        OperationImpl operation = new OperationImpl();
        pathItem.setGET(operation);
        operation.setOperationId(element.getName());
        operation.setMethod(HttpMethod.GET);

        // Add the default response
        insertDefaultResponse(context, operation, element);
    }

    @Override
    public void visitPOST(AnnotationModel post, MethodModel element, ApiContext context) {
        if (context.getPath() == null) {
            return;
        }

        // Get or create the path item
        PathItem pathItem = context.getApi().getPaths().getPathItems().getOrDefault(context.getPath(), new PathItemImpl());
        context.getApi().getPaths().addPathItem(context.getPath(), pathItem);

        OperationImpl operation = new OperationImpl();
        pathItem.setPOST(operation);
        operation.setOperationId(element.getName());
        operation.setMethod(HttpMethod.POST);

        // Add the default request
        insertDefaultRequestBody(context, operation, element);

        // Add the default response
        insertDefaultResponse(context, operation, element);
    }

    @Override
    public void visitPUT(AnnotationModel put, MethodModel element, ApiContext context) {
        if (context.getPath() == null) {
            return;
        }

        // Get or create the path item
        PathItem pathItem = context.getApi().getPaths().getPathItems().getOrDefault(context.getPath(), new PathItemImpl());
        context.getApi().getPaths().addPathItem(context.getPath(), pathItem);

        OperationImpl operation = new OperationImpl();
        pathItem.setPUT(operation);
        operation.setOperationId(element.getName());
        operation.setMethod(HttpMethod.PUT);

        // Add the default request
        insertDefaultRequestBody(context, operation, element);

        // Add the default response
        insertDefaultResponse(context, operation, element);
    }

    @Override
    public void visitDELETE(AnnotationModel delete, MethodModel element, ApiContext context) {
        if (context.getPath() == null) {
            return;
        }

        // Get or create the path item
        PathItem pathItem = context.getApi().getPaths().getPathItems().getOrDefault(context.getPath(), new PathItemImpl());
        context.getApi().getPaths().addPathItem(context.getPath(), pathItem);

        OperationImpl operation = new OperationImpl();
        pathItem.setDELETE(operation);
        operation.setOperationId(element.getName());
        operation.setMethod(HttpMethod.DELETE);

        // Add the default response
        insertDefaultResponse(context, operation, element);
    }

    @Override
    public void visitHEAD(AnnotationModel head, MethodModel element, ApiContext context) {
        if (context.getPath() == null) {
            return;
        }

        // Get or create the path item
        PathItem pathItem = context.getApi().getPaths().getPathItems().getOrDefault(context.getPath(), new PathItemImpl());
        context.getApi().getPaths().addPathItem(context.getPath(), pathItem);

        OperationImpl operation = new OperationImpl();
        pathItem.setHEAD(operation);
        operation.setOperationId(element.getName());
        operation.setMethod(HttpMethod.HEAD);

        // Add the default request
        insertDefaultRequestBody(context, operation, element);

        // Add the default response
        insertDefaultResponse(context, operation, element);
    }

    @Override
    public void visitOPTIONS(AnnotationModel options, MethodModel element, ApiContext context) {
        if (context.getPath() == null) {
            return;
        }

        // Get or create the path item
        PathItem pathItem = context.getApi().getPaths().getPathItems().getOrDefault(context.getPath(), new PathItemImpl());
        context.getApi().getPaths().addPathItem(context.getPath(), pathItem);

        OperationImpl operation = new OperationImpl();
        pathItem.setOPTIONS(operation);
        operation.setOperationId(element.getName());
        operation.setMethod(HttpMethod.OPTIONS);

        // Add the default request
        insertDefaultRequestBody(context, operation, element);

        // Add the default response
        insertDefaultResponse(context, operation, element);
    }

    @Override
    public void visitPATCH(AnnotationModel patch, MethodModel element, ApiContext context) {
        if (context.getPath() == null) {
            return;
        }

        // Get or create the path item
        PathItem pathItem = context.getApi().getPaths().getPathItems().getOrDefault(context.getPath(), new PathItemImpl());
        context.getApi().getPaths().addPathItem(context.getPath(), pathItem);

        OperationImpl operation = new OperationImpl();
        pathItem.setPATCH(operation);
        operation.setOperationId(element.getName());
        operation.setMethod(HttpMethod.PATCH);

        // Add the default request
        insertDefaultRequestBody(context, operation, element);

        // Add the default response
        insertDefaultResponse(context, operation, element);
    }

    @Override
    public void visitProduces(AnnotationModel produces, AnnotatedElement element, ApiContext context) {
        if (element instanceof MethodModel && context.getWorkingOperation() != null) {
            for (APIResponse response : context.getWorkingOperation()
                    .getResponses().getAPIResponses().values()) {

                if (response != null) {
                    // Find the wildcard return type
                    if (response.getContent() != null
                            && response.getContent().getMediaType(jakarta.ws.rs.core.MediaType.WILDCARD) != null) {
                        MediaType wildcardMedia = response.getContent().getMediaType(jakarta.ws.rs.core.MediaType.WILDCARD);

                        // Merge the wildcard return type with the valid response types
                        //This keeps the specific details of a reponse type that has a schema
                        List<String> mediaTypes = produces.getValue("value", List.class);
                        for (String mediaType : mediaTypes) {
                            MediaType held = response.getContent().getMediaType(getContentType(mediaType));
                            if (held == null) {
                                response.getContent().addMediaType(getContentType(mediaType), wildcardMedia);
                            } else {
                                MediaTypeImpl.merge(held, wildcardMedia, true);
                            }
                        }
                        // If there is an @Produces, remove the wildcard
                        response.getContent().removeMediaType(jakarta.ws.rs.core.MediaType.WILDCARD);
                    }
                }
            }
        }
    }

    @Override
    public void visitConsumes(AnnotationModel consumes, AnnotatedElement element, ApiContext context) {
        if (element instanceof MethodModel && context.getWorkingOperation() != null) {
            RequestBody requestBody = context.getWorkingOperation()
                    .getRequestBody();

            if (requestBody != null) {
                // Find the wildcard return type
                if (requestBody.getContent() != null
                        && requestBody.getContent().getMediaType(
                                jakarta.ws.rs.core.MediaType.APPLICATION_JSON) != null) {
                    MediaType jsonMedia = requestBody.getContent().getMediaType(
                            jakarta.ws.rs.core.MediaType.APPLICATION_JSON);

                    // Copy the wildcard return type to the valid request body types
                    List<String> mediaTypes = consumes.getValue("value", List.class);
                    for (String mediaType : mediaTypes) {
                        requestBody.getContent().addMediaType(getContentType(mediaType), jsonMedia);
                    }
                    // If there is an @Consumes, removes the default
                    if (!mediaTypes.contains(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)) {
                        requestBody.getContent().removeMediaType(jakarta.ws.rs.core.MediaType.APPLICATION_JSON);
                    }
                }
            }
        }
    }

    @Override
    public void visitQueryParam(AnnotationModel param, AnnotatedElement element, ApiContext context) {
        addParameter(element, context, param.getValue("value", String.class), In.QUERY, null);
    }

    @Override
    public void visitPathParam(AnnotationModel param, AnnotatedElement element, ApiContext context) {
        addParameter(element, context, param.getValue("value", String.class), In.PATH, true);
    }

    @Override
    public void visitFormParam(AnnotationModel param, AnnotatedElement element, ApiContext context) {
        // Find the aggregate schema type of all the parameters
        SchemaType formSchemaType = null;

        if (element instanceof org.glassfish.hk2.classmodel.reflect.Parameter) {
            List<org.glassfish.hk2.classmodel.reflect.Parameter> parameters = ((org.glassfish.hk2.classmodel.reflect.Parameter) element)
                    .getMethod().getParameters();
            for (org.glassfish.hk2.classmodel.reflect.Parameter methodParam : parameters) {
                if (methodParam.getAnnotation(FormParam.class.getName()) != null) {
                    formSchemaType = ModelUtils.getParentSchemaType(
                            formSchemaType,
                            ModelUtils.getSchemaType(methodParam, context)
                    );
                }
            }
        }

        final Operation workingOperation = context.getWorkingOperation();
        if (workingOperation != null) {
            // If there's no request body, fill out a new one right down to the schema
            if (workingOperation.getRequestBody() == null) {
                workingOperation.setRequestBody(new RequestBodyImpl().content(new ContentImpl()
                        .addMediaType(jakarta.ws.rs.core.MediaType.WILDCARD, new MediaTypeImpl()
                                .schema(new SchemaImpl()))));
            }

            for (MediaType mediaType : workingOperation.getRequestBody().getContent().getMediaTypes().values()) {
                final Schema schema = mediaType.getSchema();
                if (schema != null) {
                    schema.setType(formSchemaType);
                }
            }
        }
    }

    @Override
    public void visitHeaderParam(AnnotationModel param, AnnotatedElement element, ApiContext context) {
        addParameter(element, context, param.getValue("value", String.class), In.HEADER, null);
    }

    @Override
    public void visitCookieParam(AnnotationModel param, AnnotatedElement element, ApiContext context) {
        addParameter(element, context, param.getValue("value", String.class), In.COOKIE, null);
    }

    @Override
    public void visitNotEmpty(AnnotationModel param, AnnotatedElement element, ApiContext context) {
        handleBeanValidationAnnotation(param, element, context, BeanValidationType.NOT_EMPTY);
    }

    @Override
    public void visitNotBlank(AnnotationModel param, AnnotatedElement element, ApiContext context) {
        handleBeanValidationAnnotation(param, element, context, BeanValidationType.NOT_BLANK);
    }

    @Override
    public void visitSize(AnnotationModel param, AnnotatedElement element, ApiContext context) {
        handleBeanValidationAnnotation(param, element, context, BeanValidationType.SIZE);
    }

    @Override
    public void visitDecimalMax(AnnotationModel param, AnnotatedElement element, ApiContext context) {
        handleBeanValidationAnnotation(param, element, context, BeanValidationType.DECIMAL_MAX);
    }

    @Override
    public void visitDecimalMin(AnnotationModel param, AnnotatedElement element, ApiContext context) {
        handleBeanValidationAnnotation(param, element, context, BeanValidationType.DECIMAL_MIN);
    }

    @Override
    public void visitMax(AnnotationModel param, AnnotatedElement element, ApiContext context) {
        handleBeanValidationAnnotation(param, element, context, BeanValidationType.MAX);
    }

    @Override
    public void visitMin(AnnotationModel param, AnnotatedElement element, ApiContext context) {
        handleBeanValidationAnnotation(param, element, context, BeanValidationType.MIN);
    }

    @Override
    public void visitNegative(AnnotationModel param, AnnotatedElement element, ApiContext context) {
        handleBeanValidationAnnotation(param, element, context, BeanValidationType.NEGATIVE);
    }

    @Override
    public void visitNegativeOrZero(AnnotationModel param, AnnotatedElement element, ApiContext context) {
        handleBeanValidationAnnotation(param, element, context, BeanValidationType.NEGATIVE_OR_ZERO);

    }

    @Override
    public void visitPositive(AnnotationModel param, AnnotatedElement element, ApiContext context) {
        handleBeanValidationAnnotation(param, element, context, BeanValidationType.POSITIVE);
    }

    @Override
    public void visitPositiveOrZero(AnnotationModel param, AnnotatedElement element, ApiContext context) {
        handleBeanValidationAnnotation(param, element, context, BeanValidationType.POSITIVE_OR_ZER0);
    }

    private void handleBeanValidationAnnotation(AnnotationModel param, AnnotatedElement element, ApiContext context, BeanValidationType type) {
        if (element instanceof FieldModel) {
            addSchemaProperty(param, element, context, type);
        } else if (element instanceof org.glassfish.hk2.classmodel.reflect.Parameter) {
            addSchemaParameter(param, element, context, type);
        }
    }

    private void addSchemaParameter(AnnotationModel param, AnnotatedElement element, ApiContext context, BeanValidationType type) {
        if (isSchemaHidden(element)) {
            return;
        }

        org.glassfish.hk2.classmodel.reflect.Parameter elementParam = (org.glassfish.hk2.classmodel.reflect.Parameter) element;
        SchemaImpl property = new SchemaImpl();
        property.setType(ModelUtils.getSchemaType(elementParam.getTypeName(), context));
        setPropertyValue(type, property, param);

        AnnotationModel pathParam = elementParam.getAnnotation(PathParam.class.getName());
        AnnotationModel queryParam = elementParam.getAnnotation(QueryParam.class.getName());
        AnnotationModel headerParam = elementParam.getAnnotation(HeaderParam.class.getName());

        AnnotationModel selectedParam = Stream.of(pathParam, queryParam, headerParam)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        if (selectedParam != null) {
            Parameter newParameter = new ParameterImpl();
            newParameter.setName(selectedParam.getValue("value", String.class));
            newParameter.setSchema(property);
            mergeParameter(context, newParameter);
            return;
        }


        if (elementParam.getMethod().getAnnotation(GET.class.getName()) != null || elementParam.getMethod().getAnnotation(DELETE.class.getName()) != null) {
            //GET and DELETE methods cannot have a request body, parameters should be passed via query/path param.
            return;
        }

        if (context.getWorkingOperation() == null) {
            return;
        }

        for (MediaType value: context.getWorkingOperation().getRequestBody().getContent().getMediaTypes().values()) {
            SchemaImpl.merge(property, value.getSchema(), false, context);
        }
    }

    private void addSchemaProperty(AnnotationModel param, AnnotatedElement element, ApiContext context, BeanValidationType type) {
        if (isSchemaHidden(element)) {
            return;
        }
        FieldModel field = (FieldModel) element;
        final ExtensibleType<?> declaringType = field.getDeclaringType();
        final String typeName = field.getTypeName();

        String schemaName = ModelUtils.getSchemaName(context, element);
        SchemaImpl schema = SchemaImpl.createInstance(param, context);

        if (Void.class.getName().equals(schema.getImplementation())) {
            schema.setImplementation(typeName);
        }


        String parentName = getParentSchemaName(context, declaringType);
        Components components = context.getApi().getComponents();
        Schema property = getParentSchemaOrCreate(element, context, typeName, schemaName, schema, parentName, components);
        setPropertyValue(type, property, param);

        SchemaImpl.merge(schema, property, false, context);
    }

    private static void mergeParameter(ApiContext context, Parameter newParameter) {
        final Operation workingOperation = context.getWorkingOperation();
        if (workingOperation != null) {
            for (Parameter parameter : workingOperation.getParameters()) {
                final String parameterName = parameter.getName();
                if (parameterName != null && parameterName.equals(newParameter.getName())) {
                    ParameterImpl.merge(newParameter, parameter, false, context);
                    return;
                }
            }
            workingOperation.addParameter(newParameter);
        } else {
            LOGGER.log(
                    SEVERE,
                    "Couldn't add {0} parameter, \"{1}\" to the OpenAPI Document. This is usually caused by declaring parameter under a method with an unsupported annotation.",
                    new Object[]{newParameter.getIn(), newParameter.getName()}
            );
        }
    }

    private boolean isSchemaHidden(AnnotatedElement element) {
        Boolean isSchemaHidden = false;
        AnnotationModel annotation = element.getAnnotation(org.eclipse.microprofile.openapi.annotations.media.Schema.class.getName());
        if (annotation != null) {
            isSchemaHidden = annotation.getValue("hidden", Boolean.class);
        }
        return isSchemaHidden;
    }

    private Schema getParentSchemaOrCreate(AnnotatedElement element, ApiContext context, String typeName, String schemaName, SchemaImpl schema, String parentName, Components components) {
        Schema parentSchema = components.getSchemas().getOrDefault(parentName, new SchemaImpl());
        components.addSchema(parentName, parentSchema);

        Schema property = parentSchema.getProperties().getOrDefault(schemaName, new SchemaImpl());
        parentSchema.addProperty(schemaName, property);

        if (schema.isRequired()) {
            parentSchema.addRequired(schemaName);
        }

        if (!schemaName.equals(element.getName()) && parentSchema.getProperties().containsKey(element.getName())) {
            parentSchema.removeProperty(element.getName());
        }

        if (property.getRef() == null) {
            property.setType(ModelUtils.getSchemaType(typeName, context));
        }
        return property;
    }

    private String getParentSchemaName(ApiContext context, ExtensibleType<?> declaringType) {
        String parentName = null;
        AnnotationModel classSchemaAnnotation = context.getAnnotationInfo(declaringType)
                .getAnnotation(org.eclipse.microprofile.openapi.annotations.media.Schema.class);

        if (classSchemaAnnotation != null) {
            parentName = classSchemaAnnotation.getValue("name", String.class);
        }
        if (parentName == null || parentName.isEmpty()) {
            parentName = declaringType.getSimpleName();
        }
        return parentName;
    }

    private void setPropertyValue(BeanValidationType type, Schema property, AnnotationModel param) {
        switch (type) {
            case NOT_EMPTY:
                handleNotEmptyProperty(property, param);
                break;
            case NOT_BLANK:
                property.setPattern("\\S");
                break;
            case SIZE:
                handleSizeProperty(property, param);
                break;
            case DECIMAL_MAX:
                if (property.getMaximum() == null) {
                    property.setMaximum(new BigDecimal(param.getValue("value", String.class)));
                }
                if (param.getValue("inclusive", Boolean.class) != null && property.getExclusiveMaximum() == null) {
                    property.setExclusiveMaximum((!param.getValue("inclusive", Boolean.class)));
                }
                break;
            case DECIMAL_MIN:
                if (property.getMinimum() == null) {
                    property.setMinimum(new BigDecimal(param.getValue("value", String.class)));
                }
                if (param.getValue("inclusive", Boolean.class) != null && property.getExclusiveMinimum() == null) {
                    property.setExclusiveMinimum((!param.getValue("inclusive", Boolean.class)));
                }
                break;
            case MAX:
                if (property.getMaximum() == null) {
                    property.setMaximum(BigDecimal.valueOf(param.getValue("value", Long.class)));
                }
                break;
            case MIN:
                if (property.getMinimum() == null) {
                    property.setMinimum(BigDecimal.valueOf(param.getValue("value", Long.class)));
                }
                break;
            case NEGATIVE:
                if (property.getMaximum() == null) {
                    property.setMaximum(BigDecimal.ZERO);
                }
                if (property.getExclusiveMaximum() == null) {
                    property.setExclusiveMaximum(true);
                }
                break;
            case NEGATIVE_OR_ZERO:
                if (property.getMaximum() == null) {
                    property.setMaximum(BigDecimal.ZERO);
                }
                break;
            case POSITIVE:
                if (property.getMinimum() == null) {
                    property.setMinimum(BigDecimal.ZERO);
                }
                if (property.getExclusiveMinimum() == null) {
                    property.setExclusiveMinimum(true);
                }
                break;
            case POSITIVE_OR_ZER0:
                if (property.getMinimum() == null) {
                    property.setMinimum(BigDecimal.ZERO);
                }
                break;
        }
    }

    private void handleNotEmptyProperty(Schema property, AnnotationModel param) {
        switch (property.getType()){
            case STRING:
                List<?> groupList = param.getValue("groups", List.class);
                if ((groupList == null || groupList.contains(Default.class.getName())) && property.getMinLength() == null) {
                    property.setMinLength(1);
                }
                break;
            case ARRAY:
                if (property.getMinItems() == null) {
                    property.setMinItems(1);
                }
                break;
            case OBJECT:
                if (property.getMinProperties() == null) {
                    property.setMinProperties(1);
                }
                break;
        }
    }
    private  void handleSizeProperty(Schema property, AnnotationModel param) {
        switch (property.getType()){
            case STRING:
                if (property.getMinLength() == null) {
                    property.setMinLength(param.getValue("min", Integer.class));
                }

                if (property.getMaxLength() == null) {
                    property.setMaxLength(param.getValue("max", Integer.class));
                }
                break;
            case ARRAY:
                if(property.getMinItems() == null) {
                    property.setMinItems(param.getValue("min", Integer.class));
                }

                if(property.getMaxItems() == null) {
                    property.setMaxItems(param.getValue("max", Integer.class));
                }
                break;
            case OBJECT:
                if(property.getMinProperties() == null) {
                    property.setMinProperties(param.getValue("min", Integer.class));
                }
                if(property.getMaxProperties() == null) {
                    property.setMaxProperties(param.getValue("max", Integer.class));
                }
                break;
        }
    }

    private static void addParameter(AnnotatedElement element, ApiContext context, String name, In in, Boolean required) {
        Boolean hidden = false;
        AnnotationModel paramAnnotation = element.getAnnotation(org.eclipse.microprofile.openapi.annotations.parameters.Parameter.class.getName());
        if (paramAnnotation != null) {
            hidden = paramAnnotation.getValue("hidden", Boolean.class);
        }
        if (hidden != null && hidden) {
            return;
        }

        Parameter newParameter = new ParameterImpl();
        newParameter.setName(name);
        newParameter.setIn(in);
        newParameter.setRequired(required);

        Boolean isSchemaHidden = false;
        AnnotationModel annotation = element.getAnnotation(org.eclipse.microprofile.openapi.annotations.media.Schema.class.getName());
        if (annotation != null) {
            isSchemaHidden = annotation.getValue("hidden", Boolean.class);
        }
        if (!isSchemaHidden) {
            SchemaImpl schema = new SchemaImpl();
            String defaultValue = getDefaultValueIfPresent(element);

            if (element instanceof org.glassfish.hk2.classmodel.reflect.Parameter) {
                org.glassfish.hk2.classmodel.reflect.Parameter parameter = (org.glassfish.hk2.classmodel.reflect.Parameter) element;
                schema.setType(ModelUtils.getSchemaType(parameter.getTypeName(), context));
            } else {
                FieldModel field = (FieldModel) element;
                schema.setType(ModelUtils.getSchemaType(field.getTypeName(), context));
            }

            if (schema.getType() == SchemaType.ARRAY) {
                schema.setItems(getArraySchema(element, context));
                if (defaultValue != null) {
                    schema.getItems().setDefaultValue(defaultValue);
                }
            } else if (defaultValue != null) {
                schema.setDefaultValue(defaultValue);
            }

            newParameter.setSchema(schema);
        }

        mergeParameter(context, newParameter);
    }

    private static SchemaImpl getArraySchema(AnnotatedElement element, ApiContext context) {
        SchemaImpl arraySchema = new SchemaImpl();
        List<ParameterizedType> parameterizedType;

        if (element instanceof org.glassfish.hk2.classmodel.reflect.Parameter) {
            org.glassfish.hk2.classmodel.reflect.Parameter parameter = (org.glassfish.hk2.classmodel.reflect.Parameter) element;
            parameterizedType = parameter.getParameterizedTypes();
        } else {
            FieldModel field = (FieldModel) element;
            parameterizedType = field.getParameterizedTypes();
        }

        arraySchema.setType(ModelUtils.getSchemaType(parameterizedType.get(0).getTypeName(), context));
        return arraySchema;
    }

    private static String getDefaultValueIfPresent(AnnotatedElement element) {
        Collection<AnnotationModel> annotations = element.getAnnotations();
        for (AnnotationModel annotation : annotations) {
            if (DefaultValue.class.getName().equals(annotation.getType().getName())) {
                try {
                    return annotation.getValue("value", String.class);
                } catch (Exception ex) {
                    LOGGER.log(WARNING, "Couldn't get the default value", ex);
                }
            }
        }
        return null;
    }

    @Override
    public void visitOpenAPI(AnnotationModel definition, AnnotatedElement element, ApiContext context) {
        OpenAPIImpl.merge(OpenAPIImpl.createInstance(definition, context), context.getApi(), true, context);
    }

    @Override
    public void visitSchema(AnnotationModel annotation, AnnotatedElement element, ApiContext context) {
        if (element instanceof ClassModel) {
            visitSchemaClass(null, annotation, (ClassModel) element, Collections.emptyList(), context);
        } else if (element instanceof EnumType) {
            vistEnumClass(annotation, (EnumType) element, context);
        } else if (element instanceof FieldModel) {
            visitSchemaField(annotation, (FieldModel) element, context);
        } else if (element instanceof MethodModel) {
            visitSchemaMethod(annotation, (MethodModel) element, context);
        } else if (element instanceof org.glassfish.hk2.classmodel.reflect.Parameter) {
            visitSchemaParameter(annotation, (org.glassfish.hk2.classmodel.reflect.Parameter) element, context);
        }
    }

    private void vistEnumClass(AnnotationModel schemaAnnotation, EnumType enumType, ApiContext context) {
        // Get the schema object name
        String schemaName = ModelUtils.getSchemaName(context, enumType);
        Schema schema = SchemaImpl.createInstance(schemaAnnotation, context);

        Schema newSchema = new SchemaImpl();
        context.getApi().getComponents().addSchema(schemaName, newSchema);
        if (schema != null) {
            SchemaImpl.merge(schema, newSchema, true, context);
        }
        if (schema == null || schema.getEnumeration() == null || schema.getEnumeration().isEmpty()) {
            //if the schema annotation does not specify enums, then all enum fields will be added
            for (FieldModel enumField : enumType.getStaticFields()) {
                final String enumValue = enumField.getName();
                if (!enumValue.contains("$VALUES")) {
                    newSchema.addEnumeration(enumValue);
                }
            }
        }

    }

    private Schema visitSchemaClass(
            Schema schema,
            AnnotationModel schemaAnnotation, ClassModel clazz,
            Collection<ParameterizedInterfaceModel> parameterizedInterfaces,
            ApiContext context) {

        // Get the schema object name
        String schemaName = ModelUtils.getSchemaName(context, clazz);

        // Add a new schema
        if (schema == null) {
            final Components components = context.getApi().getComponents();
            schema = components.getSchemas().getOrDefault(schemaName, new SchemaImpl());
            components.addSchema(schemaName, schema);
        }

        // If there is an annotation, parse its configuration
        if (schemaAnnotation != null) {
            SchemaImpl from = SchemaImpl.createInstance(schemaAnnotation, context);
            if (from.getImplementation() == null) {
                from.setImplementation(clazz.getName());
            }
            SchemaImpl.merge(from, schema, false, context);
        }

        for (FieldModel field : clazz.getFields()) {
            final String fieldName = field.getName();
            Boolean hidden = false;
            AnnotationModel fieldSchemaAnnotation = field
                    .getAnnotation(org.eclipse.microprofile.openapi.annotations.media.Schema.class.getName());
            if (fieldSchemaAnnotation != null) {
                hidden = fieldSchemaAnnotation.getValue("hidden", Boolean.class);
            }
            
            if (!Boolean.TRUE.equals(hidden)
                    && !field.isTransient()
                    && !fieldName.startsWith("this$")) {
                final Schema existingProperty = schema.getProperties().get(fieldName);
                final Schema newProperty = createSchema(null, context, field, clazz, parameterizedInterfaces);
                if (existingProperty != null) {
                    SchemaImpl.merge(existingProperty, newProperty, true, context);
                }
                schema.addProperty(fieldName, newProperty);
            }
        }

        if (schema.getType() == null) {
            schema.setType(ModelUtils.getSchemaType(clazz.getName(), context));
        }

        // If there is an extending class, add the data
        final ClassModel superClass = clazz.getParent();
        if (superClass != null && !superClass.getName().startsWith("java.")) {

            // Get the parent annotation
            AnnotationModel parentSchemAnnotation = context.getAnnotationInfo(superClass)
                    .getAnnotation(org.eclipse.microprofile.openapi.annotations.media.Schema.class);

            ParameterizedInterfaceModel parameterizedInterface = clazz.getParameterizedInterface(superClass);
            if (parameterizedInterface == null) {
                // Create a schema for the parent
                final Schema parentSchema = visitSchemaClass(null, parentSchemAnnotation, superClass, Collections.emptyList(), context);

                // Get the superclass schema name
                String parentSchemaName = ModelUtils.getSchemaName(context, superClass);

                // Link the schemas
                schema.addAllOf(new SchemaImpl().ref(parentSchemaName));

                // Add all the parent schema properties
                for (Entry<String, Schema> property : parentSchema.getProperties().entrySet()) {
                    schema.addProperty(property.getKey(), property.getValue());
                }
            } else {
                visitSchemaClass(schema, parentSchemAnnotation, superClass, parameterizedInterface.getParametizedTypes(), context);
            }
        }
        return schema;
    }

    private void visitSchemaMethod(AnnotationModel schemaAnnotation, MethodModel method, ApiContext context) {
        final ExtensibleType<?> declaringType = method.getDeclaringType();
        final String methodName = method.getName();
        final String typeName;
        if (methodName.toLowerCase().contains("set")) {
            typeName = method.getArgumentTypes()[0];
        } else {
            typeName = method.getReturnType().getTypeName();
        }
        visitSchemaFieldOrMethod(schemaAnnotation, method, declaringType, typeName, context);
    }

    private void visitSchemaField(AnnotationModel schemaAnnotation, FieldModel field, ApiContext context) {
        final ExtensibleType<?> declaringType = field.getDeclaringType();
        final String typeName = field.getTypeName();
        visitSchemaFieldOrMethod(schemaAnnotation, field, declaringType, typeName, context);
    }

    private void visitSchemaFieldOrMethod(AnnotationModel schemaAnnotation, AnnotatedElement fieldOrMethod,
            ExtensibleType<?> declaringType, String typeName, ApiContext context) {
        assert (fieldOrMethod instanceof FieldModel) || (fieldOrMethod instanceof MethodModel);
        Boolean hidden = schemaAnnotation.getValue("hidden", Boolean.class);
        if (hidden == null || !hidden) {
            // Get the schema object name
            String schemaName = ModelUtils.getSchemaName(context, fieldOrMethod);
            SchemaImpl schema = SchemaImpl.createInstance(schemaAnnotation, context);
            if(fieldOrMethod instanceof FieldModel
                    && Void.class.getName().equals(schema.getImplementation())) {
                FieldModel fieldModel = (FieldModel) fieldOrMethod;
                schema.setImplementation(fieldModel.getTypeName());
            }

            // Get the parent schema object name
            String parentName = getParentSchemaName(context, declaringType);

            // Get or create the parent schema object
            final Components components = context.getApi().getComponents();
            Schema property = getParentSchemaOrCreate(fieldOrMethod, context, typeName, schemaName, schema, parentName, components);

            SchemaImpl.merge(schema, property, false, context);
        }
    }

    private static void visitSchemaParameter(AnnotationModel schemaAnnotation, org.glassfish.hk2.classmodel.reflect.Parameter parameter, ApiContext context) {
        // If this is being parsed at the start, ignore it as the path doesn't exist
        if (context.getWorkingOperation() == null) {
            return;
        }
        Boolean hidden = schemaAnnotation.getValue("hidden", Boolean.class);
        if (hidden != null && hidden) {
            return;
        }
        // Check if it's a request body
        if (ModelUtils.isRequestBody(context, parameter)) {
            if (context.getWorkingOperation().getRequestBody() == null) {
                context.getWorkingOperation().setRequestBody(new RequestBodyImpl());
            }
            // Insert the schema to the request body media type
            MediaType mediaType = context.getWorkingOperation().getRequestBody().getContent()
                    .getMediaType(jakarta.ws.rs.core.MediaType.WILDCARD);
            Schema schema = SchemaImpl.createInstance(schemaAnnotation, context);
            SchemaImpl.merge(schema, mediaType.getSchema(), true, context);
            if (schema.getRef() != null && !schema.getRef().isEmpty()) {
                mediaType.setSchema(new SchemaImpl().ref(schema.getRef()));
            }
        } else if (ModelUtils.getParameterType(context, parameter) != null) {
            for (Parameter param : context.getWorkingOperation()
                    .getParameters()) {
                if (param.getName().equals(ModelUtils.getParameterName(context, parameter))) {
                    Schema schema = SchemaImpl.createInstance(schemaAnnotation, context);
                    SchemaImpl.merge(schema, param.getSchema(), true, context);
                    if (schema.getRef() != null && !schema.getRef().isEmpty()) {
                        param.setSchema(new SchemaImpl().ref(schema.getRef()));
                    }
                }
            }
        }
    }

    @Override
    public void visitExtension(AnnotationModel extension, AnnotatedElement element, ApiContext context) {
        String value = extension.getValue("value", String.class);
        String name = extension.getValue("name", String.class);
        Boolean parseValue = extension.getValue("parseValue", Boolean.class);
        if (name != null && !name.isEmpty()
                && value != null && !value.isEmpty()) {
            Object parsedValue = ExtensibleImpl.convertExtensionValue(value, parseValue);
            if (element instanceof MethodModel) {
                context.getWorkingOperation().addExtension(name, parsedValue);
            } else {
                context.getApi().addExtension(name, parsedValue);
            }
        }
    }

    @Override
    public void visitExtensions(AnnotationModel annotation, AnnotatedElement element, ApiContext context) {
        List<AnnotationModel> extensions = annotation.getValue("value", List.class);
        if (extensions != null) {
            extensions.forEach(extension -> visitExtension(extension, element, context));
        }
    }

    @Override
    public void visitOperation(AnnotationModel annotation, AnnotatedElement element, ApiContext context) {
        OperationImpl.merge(OperationImpl.createInstance(annotation, context), context.getWorkingOperation(), true);
        // If the operation should be hidden, remove it
        final Boolean hidden = annotation.getValue("hidden", Boolean.class);
        if (hidden != null && hidden) {
            ModelUtils.removeOperation(context.getApi().getPaths().getPathItem(context.getPath()),
                    context.getWorkingOperation());
        }
    }

    @Override
    public void visitCallback(AnnotationModel annotation, AnnotatedElement element, ApiContext context) {
        if (element instanceof MethodModel) {
            String name = annotation.getValue("name", String.class);
            Callback callbackModel = context.getWorkingOperation()
                    .getCallbacks().getOrDefault(name, new CallbackImpl());
            context.getWorkingOperation().addCallback(name, callbackModel);
            CallbackImpl.merge(CallbackImpl.createInstance(annotation, context), callbackModel, true, context);
        }
    }

    @Override
    public void visitCallbacks(AnnotationModel annotation, AnnotatedElement element, ApiContext context) {
        List<AnnotationModel> callbacks = annotation.getValue("value", List.class);
        if (callbacks != null) {
            callbacks.forEach(callback -> visitCallback(callback, element, context));
        }
    }

    @Override
    public void visitRequestBody(AnnotationModel annotation, AnnotatedElement element, ApiContext context) {
        if (element instanceof MethodModel || element instanceof org.glassfish.hk2.classmodel.reflect.Parameter) {
            RequestBody currentRequestBody = context
                    .getWorkingOperation().getRequestBody();
            if (currentRequestBody != null || element instanceof org.glassfish.hk2.classmodel.reflect.Parameter) {
                RequestBodyImpl.merge(RequestBodyImpl.createInstance(annotation, context), currentRequestBody, true, context);
            }
        }
    }

    @Override
    public void visitRequestBodySchema(AnnotationModel requestBodySchema, AnnotatedElement element,
            ApiContext context) {
        if (element instanceof MethodModel || element instanceof org.glassfish.hk2.classmodel.reflect.Parameter) {
            final RequestBody currentRequestBody = context.getWorkingOperation().getRequestBody();
            if (currentRequestBody != null) {
                Boolean hidden = requestBodySchema.getValue("hidden", Boolean.class);
                if (hidden != null && hidden) {
                    return;
                }
                final String implementationClass = requestBodySchema.getValue("value", String.class);
                final SchemaImpl schema = SchemaImpl.fromImplementation(implementationClass, context);

                for (MediaType mediaType : currentRequestBody.getContent().getMediaTypes().values()) {
                    mediaType.setSchema(schema);
                }
            }
        }
    }

    @Override
    public void visitAPIResponse(AnnotationModel annotation, AnnotatedElement element, ApiContext context) {
        APIResponseImpl apiResponse = APIResponseImpl.createInstance(annotation, context);
        Operation workingOperation = context.getWorkingOperation();

        // Handle exception mappers
        if (workingOperation == null) {
            if (element instanceof MethodModel && "toResponse".equals(element.getName())) {
                final MethodModel methodModel = (MethodModel) element;
                final String exceptionType = methodModel.getParameter(0).getTypeName();
                mapException(context, exceptionType, apiResponse);
            } else if (element instanceof ClassModel) {
                final ClassModel classModel = (ClassModel) element;
                for (ParameterizedInterfaceModel parameterizedInterface : classModel.getParameterizedInterfaces()) {
                    if (parameterizedInterface.getRawInterfaceName().equals(jakarta.ws.rs.ext.ExceptionMapper.class.getName())
                            && !parameterizedInterface.getParametizedTypes().isEmpty()) {
                        String exceptionType = parameterizedInterface.getParametizedTypes().toArray(new ParameterizedInterfaceModel[0])[0].getName();
                        mapException(context, exceptionType, apiResponse);
                    }
                }
            } else {
                LOGGER.warning(() -> "Unrecognised @APIResponse annotation position at: " + element.shortDesc());
            }
            return;
        }

        APIResponsesImpl.merge(apiResponse, workingOperation.getResponses(), true, context);

        // If an APIResponse has been processed that isn't the default
        String responseCode = apiResponse.getResponseCode();
        if (responseCode != null && !responseCode.isEmpty() && !responseCode
                .equals(APIResponses.DEFAULT)) {
            // If the element doesn't also contain a response mapping to the default
            AnnotationModel apiResponsesParent = element
                    .getAnnotation(org.eclipse.microprofile.openapi.annotations.responses.APIResponses.class.getName());
            if (apiResponsesParent != null) {
                List<AnnotationModel> apiResponses = apiResponsesParent.getValue("value", List.class);
                if (apiResponses.stream()
                        .map(a -> a.getValue("responseCode", String.class))
                        .noneMatch(code -> code == null || code.isEmpty() || code.equals(APIResponses.DEFAULT))) {
                    // Then remove the default response
                    workingOperation.getResponses()
                            .removeAPIResponse(APIResponses.DEFAULT);
                }
            } else {
                workingOperation.getResponses()
                        .removeAPIResponse(APIResponses.DEFAULT);
            }
        }
    }

    @Override
    public void visitAPIResponses(AnnotationModel annotation, AnnotatedElement element, ApiContext context) {
        APIResponsesImpl from = APIResponsesImpl.createInstance(annotation, context);
        List<AnnotationModel> responses = annotation.getValue("value", List.class);
        if (responses != null) {
            responses.forEach(response -> visitAPIResponse(response, element, context));
        }
        if (context.getWorkingOperation() != null) {
            APIResponsesImpl.merge(from, context.getWorkingOperation().getResponses(), true, context);
        }
    }

    @Override
    public void visitAPIResponseSchema(AnnotationModel apiResponseSchema, AnnotatedElement element,
            ApiContext context) {
        final APIResponseImpl response = APIResponseImpl.createInstance(apiResponseSchema, context);

        final OperationImpl operation = (OperationImpl) context.getWorkingOperation();

        // Handle exception mappers
        if (operation == null) {
            if (element instanceof MethodModel && "toResponse".equals(element.getName())) {
                final MethodModel methodModel = (MethodModel) element;
                final String exceptionType = methodModel.getParameter(0).getTypeName();
                mapException(context, exceptionType, response);
            } else {
                LOGGER.warning("Unrecognised annotation position at: " + element.shortDesc());
            }
            return;
        }

        // If response code hasn't been specified
        String responseCode = response.getResponseCode();
        if (responseCode == null || responseCode.isEmpty()) {
            assert element instanceof MethodModel;
            final MethodModel method = (MethodModel) element;

            if (isVoid(method.getReturnType())) {
                if (HttpMethod.POST.equals(operation.getMethod())) {
                    responseCode = "201";
                } else if (Arrays.asList(method.getArgumentTypes()).contains("jakarta.ws.rs.container.AsyncResponse")) {
                    responseCode = "200";
                } else {
                    responseCode = "204";
                }
            } else {
                responseCode = "200";
            }
        }
        response.setResponseCode(responseCode);

        // If the response description hasn't been specified
        final String responseDescription = response.getDescription();
        if (responseDescription == null || responseDescription.isEmpty()) {
            try {
                final int statusInt = Integer.parseInt(responseCode);
                final Status status = Status.fromStatusCode(statusInt);
                if (status != null) {
                    response.setDescription(status.getReasonPhrase());
                }
            } catch (NumberFormatException ex) {
                LOGGER.log(Level.FINE, "Unrecognised status code, description will be empty", ex);
            }
        }

        final APIResponses responses = operation.getResponses();

        // Remove the default response
        final APIResponse defaultResponse = responses.getAPIResponse(APIResponses.DEFAULT);
        if (defaultResponse != null) {
            responses.removeAPIResponse(APIResponses.DEFAULT);
            responses.addAPIResponse(responseCode, defaultResponse);
        }

        // Add the generated response
        APIResponsesImpl.merge(response, responses, true, context);
    }

    /**
     * When an exception mapper is encountered, register the mapped response and
     * find any operations already parsed that this exception mapper is applicable
     * to
     */
    private void mapException(ApiContext context, String exceptionType, APIResponseImpl exceptionResponse) {
        // Don't allow null responses
        if (exceptionResponse.getDescription() == null || exceptionResponse.getDescription().isEmpty()) {
            exceptionResponse.setDescription(ModelUtils.getSimpleName(exceptionType));
        }
        context.addMappedExceptionResponse(exceptionType, exceptionResponse);
        final String exceptionStatus = exceptionResponse.getResponseCode();

        if (exceptionStatus != null) {
            for (PathItem path : context.getApi().getPaths().getPathItems().values()) {
                for (Operation operation : path.getOperations().values()) {
                    if (((OperationImpl) operation).getExceptionTypes().contains(exceptionType)) {
                        operation.getResponses().addAPIResponse(exceptionStatus, exceptionResponse);
                    } 
                }
            }
        } else {
            LOGGER.fine("Failed to add mapped response as no response code was provided");
        }
    }

    @Override
    public void visitParameters(AnnotationModel annotation, AnnotatedElement element, ApiContext context) {
        List<AnnotationModel> parameters = annotation.getValue("value", List.class);
        if (parameters != null) {
            for (AnnotationModel paramAnnotation : parameters) {
                final Parameter parameter = ParameterImpl.createInstance(paramAnnotation, context);
                final Operation workingOperation = context.getWorkingOperation();
                if (workingOperation != null) {
                    workingOperation.addParameter(parameter);
                }
            }
        }
    }

    @Override
    public void visitParameter(AnnotationModel annotation, AnnotatedElement element, ApiContext context) {
        Parameter matchedParam = null;
        Boolean hidden = annotation.getValue("hidden", Boolean.class);
        if (hidden != null && hidden) {
            return;
        }
        Parameter parameter = ParameterImpl.createInstance(annotation, context);
        if(context.getPath().contains("{" + parameter.getName() + "}")){
            parameter.setRequired(true);
        }

        if (element instanceof org.glassfish.hk2.classmodel.reflect.Parameter) {
            matchedParam = findOperationParameterFor((org.glassfish.hk2.classmodel.reflect.Parameter) element, context);
        }
        if (element instanceof MethodModel) {
            matchedParam = findOperationParameterFor(parameter, (MethodModel) element, context);
        }
        if (matchedParam != null) {
            ParameterImpl.merge(parameter, matchedParam, true, context);

            // If a content was added, and a schema type exists, reconfigure the schema type
            if (matchedParam.getContent() != null
                    && !matchedParam.getContent().getMediaTypes().isEmpty()
                    && matchedParam.getSchema() != null
                    && matchedParam.getSchema().getType() != null) {
                SchemaType type = matchedParam.getSchema().getType();
                matchedParam.setSchema(null);

                for (MediaType mediaType : matchedParam.getContent().getMediaTypes().values()) {
                    if (mediaType.getSchema() == null) {
                        mediaType.setSchema(new SchemaImpl());
                    }
                    mediaType.getSchema()
                            .setType(ModelUtils.mergeProperty(mediaType.getSchema().getType(), type, false));
                }
            }
        }
    }

    private static Parameter findOperationParameterFor(
            Parameter parameter,
            MethodModel annotated,
            ApiContext context) {
        String name = parameter.getName();
        // If the parameter reference is valid
        if (name != null && !name.isEmpty()) {
            // Get all parameters with the same name
            List<org.glassfish.hk2.classmodel.reflect.Parameter> matchingMethodParameters = annotated.getParameters()
                    .stream()
                    .filter(x -> name.equals(ModelUtils.getParameterName(context, x)))
                    .collect(Collectors.toList());
            // If there is more than one match, filter it further
            In in = parameter.getIn();
            if (matchingMethodParameters.size() > 1 && in != null) {
                // Remove all parameters of the wrong input type
                matchingMethodParameters
                        .removeIf(x -> ModelUtils.getParameterType(context, x) != In.valueOf(in.name()));
            }
            if (matchingMethodParameters.isEmpty()) {
                return null;
            }
            // If there's only one matching parameter, handle it immediately
            String matchingMethodParamName = ModelUtils.getParameterName(context, matchingMethodParameters.get(0));
            // Find the matching operation parameter
            for (Parameter operationParam : context
                    .getWorkingOperation().getParameters()) {
                if (operationParam.getName().equals(matchingMethodParamName)) {
                    return operationParam;
                }
            }
        }
        return null;
    }

    /**
     * Find the matching parameter, and match it
     */
    private static Parameter findOperationParameterFor(
            org.glassfish.hk2.classmodel.reflect.Parameter annotated, ApiContext context) {
        String actualName = ModelUtils.getParameterName(context, annotated);
        if (actualName == null) {
            return null;
        }
        for (Parameter param : context.getWorkingOperation()
                .getParameters()) {
            if (actualName.equals(param.getName())) {
                return param;
            }
        }
        return null;
    }

    @Override
    public void visitExternalDocumentation(AnnotationModel externalDocs, AnnotatedElement element,
            ApiContext context) {
        if (element instanceof MethodModel) {
            ExternalDocumentation newExternalDocs = new ExternalDocumentationImpl();
            ExternalDocumentationImpl.merge(ExternalDocumentationImpl.createInstance(externalDocs), newExternalDocs, true);
            if (newExternalDocs.getUrl() != null && !newExternalDocs.getUrl().isEmpty()) {
                context.getWorkingOperation().setExternalDocs(newExternalDocs);
            }
        }
    }

    @Override
    public void visitServer(AnnotationModel server, AnnotatedElement element, ApiContext context) {
        if (element instanceof MethodModel) {
            Server newServer = new ServerImpl();
            context.getWorkingOperation().addServer(newServer);
            ServerImpl.merge(ServerImpl.createInstance(server, context), newServer, true);
        }
    }

    @Override
    public void visitServers(AnnotationModel annotation, AnnotatedElement element, ApiContext context) {
        List<AnnotationModel> servers = annotation.getValue("value", List.class);
        if (servers != null) {
            servers.forEach(server -> visitServer(server, element, context));
        }
    }

    @Override
    public void visitTag(AnnotationModel annotation, AnnotatedElement element, ApiContext context) {
        Tag from = TagImpl.createInstance(annotation, context);
        if (element instanceof MethodModel) {
            final List<Tag> tags = new ArrayList<>();
            tags.addAll(context.getApi().getTags());
            TagImpl.merge(from, context.getWorkingOperation(), true, tags);
            context.getApi().setTags(tags);
        } else {
            Tag newTag = new TagImpl();
            TagImpl.merge(from, newTag, true);
            if (newTag.getName() != null && !newTag.getName().isEmpty()) {
                context.getApi().addTag(newTag);
            }
        }
    }

    @Override
    public void visitTags(AnnotationModel annotation, AnnotatedElement element, ApiContext context) {
        if (element instanceof MethodModel) {
            List<AnnotationModel> tags = annotation.getValue("value", List.class);
            if (tags != null) {
                for (AnnotationModel tag : tags) {
                    visitTag(tag, element, context);
                }
            }
            List<String> refs = annotation.getValue("refs", List.class);
            if (refs != null) {
                for (String ref : refs) {
                    if (ref != null && !ref.isEmpty()) {
                        context.getWorkingOperation().addTag(ref);
                    }
                }
            }
        }
    }

    @Override
    public void visitSecurityScheme(AnnotationModel annotation, AnnotatedElement element, ApiContext context) {
        String securitySchemeName = annotation.getValue("securitySchemeName", String.class);
        SecurityScheme securityScheme = SecuritySchemeImpl.createInstance(annotation, context);
        if (securitySchemeName != null && !securitySchemeName.isEmpty()) {
            SecurityScheme newScheme = context.getApi().getComponents()
                    .getSecuritySchemes().getOrDefault(securitySchemeName, new SecuritySchemeImpl());
            context.getApi().getComponents().addSecurityScheme(securitySchemeName, newScheme);
            SecuritySchemeImpl.merge(securityScheme, newScheme, true);
        }
    }

    @Override
    public void visitSecuritySchemes(AnnotationModel annotation, AnnotatedElement element, ApiContext context) {
        List<AnnotationModel> securitySchemes = annotation.getValue("value", List.class);
        if (securitySchemes != null) {
            securitySchemes.forEach(securityScheme -> visitSecurityScheme(securityScheme, element, context));
        }
    }

    @Override
    public void visitSecurityRequirement(AnnotationModel annotation, AnnotatedElement element, ApiContext context) {
        if (element instanceof MethodModel) {
            String securityRequirementName = annotation.getValue("name", String.class);
            SecurityRequirement securityRequirement = SecurityRequirementImpl.createInstance(annotation, context);
            if (securityRequirementName != null && !securityRequirementName.isEmpty()) {
                SecurityRequirement model = new SecurityRequirementImpl();
                SecurityRequirementImpl.merge(securityRequirement, model);
                context.getWorkingOperation().addSecurityRequirement(model);
            }
        }
    }

    @Override
    public void visitSecurityRequirements(AnnotationModel annotation, AnnotatedElement element, ApiContext context) {
        List<AnnotationModel> securityRequirements = annotation.getValue("value", List.class);
        if (securityRequirements != null) {
            securityRequirements.forEach(securityRequirement ->
                    visitSecurityRequirement(securityRequirement, element, context));
        }
    }

    @Override
    public void visitSecurityRequirementSet(AnnotationModel annotation, AnnotatedElement element, ApiContext context) {
        if (element instanceof MethodModel) {
            SecurityRequirement securityRequirement = SecurityRequirementImpl.createInstances(annotation, context);
            context.getWorkingOperation().addSecurityRequirement(securityRequirement);
        }
    }

    @Override
    public void visitSecurityRequirementSets(AnnotationModel annotation, AnnotatedElement element, ApiContext context) {
        List<AnnotationModel> securityRequirementSetList = annotation.getValue("value", List.class);
        if (securityRequirementSetList != null) {
            securityRequirementSetList.forEach(securityRequirementSet ->
                    visitSecurityRequirementSet(securityRequirementSet, element, context));
        }
    }

    // PRIVATE METHODS
    private RequestBody insertDefaultRequestBody(ApiContext context,
            Operation operation, MethodModel method) {
        return insertRequestBody(context, operation, method, jakarta.ws.rs.core.MediaType.APPLICATION_JSON);
    }

    private RequestBody insertRequestBody(ApiContext context,
                                          Operation operation, MethodModel method, String type) {
        RequestBody requestBody = new RequestBodyImpl();

        // Get the request body type of the method
        org.glassfish.hk2.classmodel.reflect.ParameterizedType bodyType = null;
        int indexParam = 0;
        for (org.glassfish.hk2.classmodel.reflect.Parameter methodParam : method.getParameters()) {
            if (ModelUtils.isRequestBody(context, methodParam)) {
                bodyType = methodParam;
                indexParam = methodParam.getIndex();
                break;
            }
        }
        if (bodyType == null) {
            return null;
        }

        // Create the default request body with a wildcard mediatype
        MediaType mediaType = new MediaTypeImpl();
        AnnotationModel paramAnnotation = method.getParameter(indexParam).getAnnotation(org.eclipse.microprofile.openapi.annotations.media.Schema.class.getName());
        Boolean hidden = false;
        if (paramAnnotation != null) {
            hidden = paramAnnotation.getValue("hidden", Boolean.class);
        }
        if (hidden == null || !hidden) {
            mediaType.schema(createSchema(context, bodyType));
        }
        requestBody.getContent().addMediaType(type, mediaType);

        operation.setRequestBody(requestBody);
        return requestBody;
    }

    /**
     * Creates a new {@link APIResponse} to model the default response of a
     * {@link Method}, and inserts it into the {@link Operation} responses.
     *
     * @param context the API context.
     * @param operation the {@link Operation} to add the default response to.
     * @param method the {@link Method} to model the default response on.
     * @return the newly created {@link APIResponse}.
     */
    private void insertDefaultResponse(ApiContext context,
            OperationImpl operation, MethodModel method) {

        final APIResponsesImpl responses = new APIResponsesImpl();
        operation.setResponses(responses);

        // Add the default response
        APIResponse defaultResponse = new APIResponseImpl();
        responses.addAPIResponse(APIResponses.DEFAULT, defaultResponse);
        defaultResponse.setDescription("Default Response.");

        // Configure the default response with a wildcard mediatype
        MediaType mediaType = new MediaTypeImpl().schema(
                createSchema(context, method.getReturnType())
        );
        defaultResponse.getContent().addMediaType(jakarta.ws.rs.core.MediaType.WILDCARD, mediaType);

        // Add responses for the applicable declared exceptions
        for (String exceptionType : method.getExceptionTypes()) {
            final Set<APIResponse> mappedResponses = context.getMappedExceptionResponses().get(exceptionType);
            if (mappedResponses != null) {
                for (APIResponse mappedResponse : mappedResponses) {
                    if (mappedResponse != null) {
                        final String responseCode = ((APIResponseImpl) mappedResponse).getResponseCode();
                        if (responseCode != null) {
                            responses.addAPIResponse(responseCode, mappedResponse);
                        }
                    }
                }
            }
            operation.addExceptionType(exceptionType);
        }
    }

    /**
     * @return the {@link jakarta.ws.rs.core.MediaType} with the given name.
     * Defaults to <code>WILDCARD</code>.
     */
    private static String getContentType(String name) {
        String contentType = jakarta.ws.rs.core.MediaType.WILDCARD;
        try {
            jakarta.ws.rs.core.MediaType mediaType = jakarta.ws.rs.core.MediaType.valueOf(name);
            if (mediaType != null) {
                contentType = mediaType.toString();
            }
        } catch (IllegalArgumentException ex) {
            LOGGER.log(FINE, "Unrecognised content type.", ex);
        }
        return contentType;
    }

    private Schema createSchema(
            ApiContext context,
            ParameterizedType type) {
        return createSchema(null, context, type);
    }

    private Schema createSchema(
            Schema schema,
            ApiContext context,
            ParameterizedType type) {

        String typeName = type.getTypeName();
        List<ParameterizedType> genericTypes = type.getParameterizedTypes();
        SchemaType schemaType = ModelUtils.getSchemaType(type, context);

        if (schema == null) {
            schema = new SchemaImpl();
        }

        // if there is a known type, but not set in schema, use it
        if (schema.getType() == null) {
            schema.setType(schemaType);
        }

        // Set the subtype if it's an array (for example an array of ints)
        if (schemaType == SchemaType.ARRAY) {
            if (type.isArray()) {
                schemaType = ModelUtils.getSchemaType(type.getTypeName(), context);
                schema.setType(schemaType);
            } else if (!genericTypes.isEmpty()) { // should be something Iterable
                schema.setItems(createSchema(context, genericTypes.get(0)));
            }
        }

        // If the schema is an object, insert the reference
        if (schemaType == SchemaType.OBJECT) {
            if (insertObjectReference(context, schema, context.getType(typeName), typeName)) {
                schema.setType(null);
                schema.setItems(null);
            }
        }
        if (type instanceof AnnotatedElement) {
            AnnotatedElement element = (AnnotatedElement) type;
            final AnnotationModel schemaAnnotation = element
                    .getAnnotation(org.eclipse.microprofile.openapi.annotations.media.Schema.class.getName());
            if (schemaAnnotation != null) {
                SchemaImpl.merge(SchemaImpl.createInstance(schemaAnnotation, context), schema, false, context);
            }
        }

        return schema;
    }

    private Schema createSchema(
            Schema schema,
            ApiContext context,
            ParameterizedType type,
            ExtensibleType clazz,
            Collection<ParameterizedInterfaceModel> classParameterizedTypes) {

        if (schema == null) {
            schema = new SchemaImpl();
        }
        SchemaType schemaType = ModelUtils.getSchemaType(type, context);

        // If the annotated element is the same type as the reference class, return a null schema
        if (schemaType == SchemaType.OBJECT && type.getType() != null && type.getType().equals(clazz)) {
            schema.setType(null);
            schema.setItems(null);
            return schema;
        }

        if (type.getType() == null) {
            ParameterizedInterfaceModel classParameterizedType = findParameterizedModelFromGenerics(
                    clazz,
                    classParameterizedTypes,
                    type
            );
            String typeName = null;
            if (type.getTypeName() != null) {
                typeName = type.getTypeName();
            }
            if ((typeName == null || Object.class.getName().equals(typeName)) && classParameterizedType != null) {
                typeName = classParameterizedType.getRawInterfaceName();
            }

            schemaType = ModelUtils.getSchemaType(typeName, context);
            if (schema.getType() == null) {
                schema.setType(schemaType);
            }

            Schema containerSchema = schema;
            if (schemaType == SchemaType.ARRAY) {
                containerSchema = new SchemaImpl();
                schema.setItems(containerSchema);
            }
            if (classParameterizedType != null) {
                Collection<ParameterizedInterfaceModel> genericTypes = classParameterizedType.getParametizedTypes();
                if (genericTypes.isEmpty()) {
                    if (insertObjectReference(context, containerSchema, classParameterizedType.getRawInterface(), classParameterizedType.getRawInterfaceName())) {
                        containerSchema.setType(null);
                        containerSchema.setItems(null);
                    }
                } else if (classParameterizedType.getRawInterface() instanceof ClassModel) {
                    visitSchemaClass(containerSchema, null, (ClassModel) classParameterizedType.getRawInterface(), genericTypes, context);
                } else {
                    LOGGER.log(FINE, "Unrecognised schema {0} class found.", new Object[]{classParameterizedType.getRawInterface()});
                }
            } else if (!type.getParameterizedTypes().isEmpty()) {
                List<ParameterizedType> genericTypes = type.getParameterizedTypes();
                if (ModelUtils.isMap(typeName, context) && genericTypes.size() == 2) {
                    createSchema(containerSchema, context, genericTypes.get(0), clazz, classParameterizedTypes);

                    containerSchema = new SchemaImpl();
                    schema.setAdditionalPropertiesSchema(containerSchema);
                    createSchema(containerSchema, context, genericTypes.get(1), clazz, classParameterizedTypes);
                } else {
                    createSchema(containerSchema, context, genericTypes.get(0), clazz, classParameterizedTypes);
                }
            } else {
                return createSchema(containerSchema, context, type);
            }
            return schema;
        }

        return createSchema(schema, context, type);
    }

    private ParameterizedInterfaceModel findParameterizedModelFromGenerics(
            ExtensibleType<? extends ExtensibleType> annotatedElement,
            Collection<ParameterizedInterfaceModel> parameterizedModels,
            ParameterizedType genericType) {
        if (parameterizedModels == null
                || parameterizedModels.isEmpty()) {
            return null;
        }

        List<String> formalParamKeys = new ArrayList<>(annotatedElement.getFormalTypeParameters().keySet());
        int i = 0;
        for (ParameterizedInterfaceModel parameterizedModel : parameterizedModels) {
            if (formalParamKeys.get(i).equals(genericType.getFormalType())) {
                return parameterizedModel;
            }
            i++;
        }
        return null;
    }

    /**
     * Replace the object in the referee with a reference, and create the
     * reference in the API.
     *
     * @param context the API context.
     * @param referee the object containing the reference.
     * @param referenceClass the class of the object being referenced.
     * @return if the reference has been created.
     */
    private boolean insertObjectReference(ApiContext context, Reference<?> referee, AnnotatedElement referenceClass, String referenceClassName) {

        // Firstly check if it's been already defined (i.e. config property definition)
        for (Entry<String, Schema> schemaEntry : context.getApi().getComponents().getSchemas().entrySet()) {
            final Schema entryValue = schemaEntry.getValue();
            if (entryValue instanceof SchemaImpl) {
                final SchemaImpl entryValueImpl = (SchemaImpl) entryValue;
                final String implementationClass = entryValueImpl.getImplementation();
                if (implementationClass != null && implementationClass.equals(referenceClassName)) {
                    referee.setRef(schemaEntry.getKey());
                    return true;
                }
            }
        }

        // If the object is a java core class
        if (referenceClassName == null || referenceClassName.startsWith("java.")) {
            return false;
        }

        // If the object is a Java EE object type
        if (referenceClassName.startsWith("javax.") || referenceClassName.startsWith("jakarta.")) {
            return false;
        }

        // Check the class exists in the application
        if (!context.isApplicationType(referenceClassName)) {
            return false;
        }

        if (referenceClass != null && referenceClass instanceof ExtensibleType) {
            ExtensibleType referenceClassType = (ExtensibleType) referenceClass;
            final AnnotationModel schemaAnnotation = context.getAnnotationInfo(referenceClassType)
                    .getAnnotation(org.eclipse.microprofile.openapi.annotations.media.Schema.class);
            String schemaName = ModelUtils.getSchemaName(context, referenceClass);

            Schema schema = context.getApi().getComponents().getSchemas().get(schemaName);
            if (schema == null) {
                // Create the schema
                if (context.isAllowedType(referenceClassType)) {
                    visitSchema(schemaAnnotation, referenceClassType, context);
                } else if (referenceClassType instanceof ClassModel) {
                    apiWalker.processAnnotation((ClassModel) referenceClassType, this);
                } else {
                    LOGGER.log(FINE, "Unrecognised schema {0} class found.", new Object[]{referenceClassName});
                }
            }
            Schema createdReference = context.getApi().getComponents().getSchemas().get(schemaName);
            // Set the reference name, if the schema was created
            if (createdReference != null) {
                referee.setRef(schemaName);
            }

            return createdReference != null;
        }

        return false;
    }

}

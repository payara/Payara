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
package fish.payara.microprofile.openapi.impl.processor;

import static java.util.logging.Level.FINE;
import static java.util.logging.Level.WARNING;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Application;

import org.eclipse.microprofile.openapi.annotations.ExternalDocumentation;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.callbacks.Callback;
import org.eclipse.microprofile.openapi.annotations.callbacks.Callbacks;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
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
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.Reference;
import org.eclipse.microprofile.openapi.models.media.MediaType;
import org.eclipse.microprofile.openapi.models.media.Schema.SchemaType;
import org.eclipse.microprofile.openapi.models.parameters.Parameter.In;
import org.eclipse.microprofile.openapi.models.parameters.Parameter.Style;

import fish.payara.microprofile.openapi.api.processor.OASProcessor;
import fish.payara.microprofile.openapi.api.visitor.ApiContext;
import fish.payara.microprofile.openapi.api.visitor.ApiVisitor;
import fish.payara.microprofile.openapi.api.visitor.ApiWalker;
import fish.payara.microprofile.openapi.impl.config.OpenApiConfiguration;
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
import fish.payara.microprofile.openapi.impl.visitor.OpenApiWalker;

/**
 * A processor to parse the application for annotations, to add to the OpenAPI
 * model.
 */
public class ApplicationProcessor implements OASProcessor, ApiVisitor {

    private static final Logger LOGGER = Logger.getLogger(ApplicationProcessor.class.getName());

    /**
     * A list of all classes in the given application.
     */
    private final Set<Class<?>> classes;

    /**
     * @param appClassLoader the class loader for the application.
     */
    public ApplicationProcessor(Set<Class<?>> appClasses) {
        this.classes = appClasses;
    }

    @Override
    public OpenAPI process(OpenAPI api, OpenApiConfiguration config) {
        ApiWalker apiWalker = null;
        if (config == null) {
            apiWalker = new OpenApiWalker(api, classes, generateResourceMapping(classes));
        } else {
            apiWalker = new OpenApiWalker(api, config.getValidClasses(classes), generateResourceMapping(classes));
        }
        if (config == null || !config.getScanDisable()) {
            apiWalker.accept(this);
        }
        return api;
    }

    // JAX-RS method handlers

    @Override
    public void visitGET(GET get, Method element, ApiContext context) {
        if (context.getPath() == null) {
            return;
        }

        // Get or create the path item
        PathItem pathItem = context.getApi().getPaths().getOrDefault(context.getPath(), new PathItemImpl());
        context.getApi().getPaths().addPathItem(context.getPath(), pathItem);

        org.eclipse.microprofile.openapi.models.Operation operation = new OperationImpl();
        pathItem.setGET(operation);
        operation.setOperationId(element.getName());

        // Add the default request
        insertDefaultRequestBody(context, operation, element);

        // Add the default response
        insertDefaultResponse(context, operation, element);
    }

    @Override
    public void visitPOST(POST post, Method element, ApiContext context) {
        if (context.getPath() == null) {
            return;
        }

        // Get or create the path item
        PathItem pathItem = context.getApi().getPaths().getOrDefault(context.getPath(), new PathItemImpl());
        context.getApi().getPaths().addPathItem(context.getPath(), pathItem);

        org.eclipse.microprofile.openapi.models.Operation operation = new OperationImpl();
        pathItem.setPOST(operation);
        operation.setOperationId(element.getName());

        // Add the default request
        insertDefaultRequestBody(context, operation, element);

        // Add the default response
        insertDefaultResponse(context, operation, element);
    }

    @Override
    public void visitPUT(PUT put, Method element, ApiContext context) {
        if (context.getPath() == null) {
            return;
        }

        // Get or create the path item
        PathItem pathItem = context.getApi().getPaths().getOrDefault(context.getPath(), new PathItemImpl());
        context.getApi().getPaths().addPathItem(context.getPath(), pathItem);

        org.eclipse.microprofile.openapi.models.Operation operation = new OperationImpl();
        pathItem.setPUT(operation);
        operation.setOperationId(element.getName());

        // Add the default request
        insertDefaultRequestBody(context, operation, element);

        // Add the default response
        insertDefaultResponse(context, operation, element);
    }

    @Override
    public void visitDELETE(DELETE delete, Method element, ApiContext context) {
        if (context.getPath() == null) {
            return;
        }

        // Get or create the path item
        PathItem pathItem = context.getApi().getPaths().getOrDefault(context.getPath(), new PathItemImpl());
        context.getApi().getPaths().addPathItem(context.getPath(), pathItem);

        org.eclipse.microprofile.openapi.models.Operation operation = new OperationImpl();
        pathItem.setDELETE(operation);
        operation.setOperationId(element.getName());

        // Add the default request
        insertDefaultRequestBody(context, operation, element);

        // Add the default response
        insertDefaultResponse(context, operation, element);
    }

    @Override
    public void visitHEAD(HEAD head, Method element, ApiContext context) {
        if (context.getPath() == null) {
            return;
        }

        // Get or create the path item
        PathItem pathItem = context.getApi().getPaths().getOrDefault(context.getPath(), new PathItemImpl());
        context.getApi().getPaths().addPathItem(context.getPath(), pathItem);

        org.eclipse.microprofile.openapi.models.Operation operation = new OperationImpl();
        pathItem.setHEAD(operation);
        operation.setOperationId(element.getName());

        // Add the default request
        insertDefaultRequestBody(context, operation, element);

        // Add the default response
        insertDefaultResponse(context, operation, element);
    }

    @Override
    public void visitOPTIONS(OPTIONS options, Method element, ApiContext context) {
        if (context.getPath() == null) {
            return;
        }

        // Get or create the path item
        PathItem pathItem = context.getApi().getPaths().getOrDefault(context.getPath(), new PathItemImpl());
        context.getApi().getPaths().addPathItem(context.getPath(), pathItem);

        org.eclipse.microprofile.openapi.models.Operation operation = new OperationImpl();
        pathItem.setOPTIONS(operation);
        operation.setOperationId(element.getName());

        // Add the default request
        insertDefaultRequestBody(context, operation, element);

        // Add the default response
        insertDefaultResponse(context, operation, element);
    }

    @Override
    public void visitPATCH(PATCH patch, Method element, ApiContext context) {
        if (context.getPath() == null) {
            return;
        }

        // Get or create the path item
        PathItem pathItem = context.getApi().getPaths().getOrDefault(context.getPath(), new PathItemImpl());
        context.getApi().getPaths().addPathItem(context.getPath(), pathItem);

        org.eclipse.microprofile.openapi.models.Operation operation = new OperationImpl();
        pathItem.setPATCH(operation);
        operation.setOperationId(element.getName());

        // Add the default request
        insertDefaultRequestBody(context, operation, element);

        // Add the default response
        insertDefaultResponse(context, operation, element);
    }

    @Override
    public void visitProduces(Produces produces, AnnotatedElement element, ApiContext context) {
        if (element instanceof Method && context.getWorkingOperation() != null) {
            for (org.eclipse.microprofile.openapi.models.responses.APIResponse response : context.getWorkingOperation()
                    .getResponses().values()) {

                if (response != null) {
                    // Find the wildcard return type
                    if (response.getContent() != null
                            && response.getContent().get(javax.ws.rs.core.MediaType.WILDCARD) != null) {
                        MediaType wildcardMedia = response.getContent().get(javax.ws.rs.core.MediaType.WILDCARD);

                        // Copy the wildcard return type to the valid response types
                        for (String mediaType : produces.value()) {
                            response.getContent().put(getContentType(mediaType), wildcardMedia);
                        }
                        // If there is an @Produces, remove the wildcard
                        response.getContent().remove(javax.ws.rs.core.MediaType.WILDCARD);
                    }
                }
            }
        }
    }

    @Override
    public void visitConsumes(Consumes consumes, AnnotatedElement element, ApiContext context) {
        if (element instanceof Method) {
            org.eclipse.microprofile.openapi.models.parameters.RequestBody requestBody = context.getWorkingOperation()
                    .getRequestBody();

            if (requestBody != null) {
                // Find the wildcard return type
                if (requestBody.getContent() != null
                        && requestBody.getContent().get(javax.ws.rs.core.MediaType.WILDCARD) != null) {
                    MediaType wildcardMedia = requestBody.getContent().get(javax.ws.rs.core.MediaType.WILDCARD);

                    // Copy the wildcard return type to the valid request body types
                    for (String mediaType : consumes.value()) {
                        requestBody.getContent().put(getContentType(mediaType), wildcardMedia);
                    }
                    // If there is an @Consumes, remove the wildcard
                    requestBody.getContent().remove(javax.ws.rs.core.MediaType.WILDCARD);
                }
            }
        }
    }

    @Override
    public void visitQueryParam(QueryParam param, java.lang.reflect.Parameter element, ApiContext context) {
        org.eclipse.microprofile.openapi.models.parameters.Parameter newParameter = new ParameterImpl();
        newParameter.setName(param.value());
        newParameter.setIn(In.QUERY);
        newParameter.setStyle(Style.SIMPLE);
        newParameter.setSchema(new SchemaImpl().type(ModelUtils.getSchemaType(element.getType())));
        context.getWorkingOperation().addParameter(newParameter);
    }

    @Override
    public void visitPathParam(PathParam param, java.lang.reflect.Parameter element, ApiContext context) {
        org.eclipse.microprofile.openapi.models.parameters.Parameter newParameter = new ParameterImpl();
        newParameter.setName(param.value());
        newParameter.setRequired(true);
        newParameter.setIn(In.PATH);
        newParameter.setStyle(Style.SIMPLE);
        newParameter.setSchema(new SchemaImpl().type(ModelUtils.getSchemaType(element.getType())));
        context.getWorkingOperation().addParameter(newParameter);
    }

    @Override
    public void visitFormParam(FormParam param, java.lang.reflect.Parameter element, ApiContext context) {
        // Find the aggregate schema type of all the parameters
        SchemaType formSchemaType = null;
        for (java.lang.reflect.Parameter methodParam : element.getDeclaringExecutable().getParameters()) {
            if (methodParam.isAnnotationPresent(FormParam.class)) {
                formSchemaType = ModelUtils.getParentSchemaType(formSchemaType,
                        ModelUtils.getSchemaType(methodParam.getType()));
            }
        }

        // If there's no request body, fill out a new one right down to the schema
        if (context.getWorkingOperation().getRequestBody() == null) {
            context.getWorkingOperation().setRequestBody(new RequestBodyImpl().content(new ContentImpl()
                    .addMediaType(javax.ws.rs.core.MediaType.WILDCARD, new MediaTypeImpl().schema(new SchemaImpl()))));
        }

        // Set the request body type accordingly.
        context.getWorkingOperation().getRequestBody().getContent().get(javax.ws.rs.core.MediaType.WILDCARD).getSchema()
                .setType(formSchemaType);
    }

    @Override
    public void visitHeaderParam(HeaderParam param, java.lang.reflect.Parameter element, ApiContext context) {
        org.eclipse.microprofile.openapi.models.parameters.Parameter newParameter = new ParameterImpl();
        newParameter.setName(param.value());
        newParameter.setIn(In.HEADER);
        newParameter.setStyle(Style.SIMPLE);
        newParameter.setSchema(new SchemaImpl().type(ModelUtils.getSchemaType(element.getType())));
        context.getWorkingOperation().addParameter(newParameter);
    }

    @Override
    public void visitCookieParam(CookieParam param, java.lang.reflect.Parameter element, ApiContext context) {
        org.eclipse.microprofile.openapi.models.parameters.Parameter newParameter = new ParameterImpl();
        newParameter.setName(param.value());
        newParameter.setIn(In.COOKIE);
        newParameter.setStyle(Style.SIMPLE);
        newParameter.setSchema(new SchemaImpl().type(ModelUtils.getSchemaType(element.getType())));
        context.getWorkingOperation().addParameter(newParameter);
    }

    @Override
    public void visitOpenAPI(OpenAPIDefinition definition, AnnotatedElement element, ApiContext context) {
        OpenAPIImpl.merge(definition, context.getApi(), true);
    }

    @Override
    public void visitSchema(Schema schema, AnnotatedElement element, ApiContext context) {
        if (element instanceof Class) {

            // Get the schema object name
            String schemaName = (schema == null) ? null : schema.name();
            if (schemaName == null || schemaName.isEmpty()) {
                schemaName = Class.class.cast(element).getSimpleName();
            }

            // Add a new schema
            org.eclipse.microprofile.openapi.models.media.Schema newSchema = new SchemaImpl();
            context.getApi().getComponents().addSchema(schemaName, newSchema);

            // If there is an annotation
            if (schema != null) {
                SchemaImpl.merge(schema, newSchema, true, context.getApi().getComponents().getSchemas());
            } else {
                newSchema.setType(SchemaType.OBJECT);
                Map<String, org.eclipse.microprofile.openapi.models.media.Schema> fields = new LinkedHashMap<>();
                for (Field field : Class.class.cast(element).getDeclaredFields()) {
                    if (!Modifier.isTransient(field.getModifiers())) {
                        fields.put(field.getName(), createSchema(context, element, field.getType()));
                    }
                }
                newSchema.setProperties(fields);
            }

            // If there is an extending class, add the data
            if (Class.class.cast(element).getSuperclass() != null) {
                Class<?> superClass = Class.class.cast(element).getSuperclass();

                // If the super class is legitimate
                if (!superClass.equals(Object.class)) {

                    // Get the parent schema annotation
                    Schema parentSchema = superClass.getDeclaredAnnotation(Schema.class);

                    // Create a schema for the parent
                    visitSchema(parentSchema, superClass, context);

                    // Get the superclass schema name
                    String parentSchemaName = (parentSchema == null) ? null : parentSchema.name();
                    if (parentSchemaName == null || parentSchemaName.isEmpty()) {
                        parentSchemaName = Class.class.cast(superClass).getSimpleName();
                    }

                    // Link the schemas
                    newSchema.addAllOf(new SchemaImpl().ref(parentSchemaName));
                }
            }
        }
        if (element instanceof Field) {

            // Get the schema object name
            String schemaName = schema.name();
            if (schemaName == null || schemaName.isEmpty()) {
                schemaName = Field.class.cast(element).getName();
            }

            // Get the parent schema object name
            String parentName = null;
            if (Field.class.cast(element).getDeclaringClass().isAnnotationPresent(Schema.class)) {
                parentName = Field.class.cast(element).getDeclaringClass().getDeclaredAnnotation(Schema.class).name();
            }
            if (parentName == null || parentName.isEmpty()) {
                parentName = Field.class.cast(element).getDeclaringClass().getSimpleName();
            }

            // Get or create the parent schema object
            org.eclipse.microprofile.openapi.models.media.Schema parent = context.getApi().getComponents().getSchemas()
                    .getOrDefault(parentName, new SchemaImpl());
            context.getApi().getComponents().getSchemas().put(parentName, parent);

            org.eclipse.microprofile.openapi.models.media.Schema property = new SchemaImpl();
            parent.addProperty(schemaName, property);
            property.setType(ModelUtils.getSchemaType(Field.class.cast(element).getType()));
            SchemaImpl.merge(schema, property, true, context.getApi().getComponents().getSchemas());
        }
        if (element instanceof java.lang.reflect.Parameter) {

            // If this is being parsed at the start, ignore it as the path doesn't exist
            if (context.getWorkingOperation() == null) {
                return;
            }

            java.lang.reflect.Parameter parameter = (java.lang.reflect.Parameter) element;
            // Check if it's a request body
            if (ModelUtils.isRequestBody(parameter)) {
                if (context.getWorkingOperation().getRequestBody() == null) {
                    context.getWorkingOperation().setRequestBody(new RequestBodyImpl());
                }
                // Insert the schema to the request body media type
                MediaType mediaType = context.getWorkingOperation().getRequestBody().getContent()
                        .get(javax.ws.rs.core.MediaType.WILDCARD);
                SchemaImpl.merge(schema, mediaType.getSchema(), true, context.getApi().getComponents().getSchemas());
                if (schema.ref() != null && !schema.ref().isEmpty()) {
                    mediaType.setSchema(new SchemaImpl().ref(schema.ref()));
                }
            } else if (ModelUtils.getParameterType(parameter) != null) {
                for (org.eclipse.microprofile.openapi.models.parameters.Parameter param : context.getWorkingOperation()
                        .getParameters()) {
                    if (param.getName().equals(ModelUtils.getParameterName(parameter))) {
                        SchemaImpl.merge(schema, param.getSchema(), true,
                                context.getApi().getComponents().getSchemas());
                        if (schema.ref() != null && !schema.ref().isEmpty()) {
                            param.setSchema(new SchemaImpl().ref(schema.ref()));
                        }
                    }
                }
            }
        }
    }

    @Override
    public void visitExtension(Extension extension, AnnotatedElement element, ApiContext context) {
        if (extension.name() != null && !extension.name().isEmpty() && extension.value() != null
                && !extension.value().isEmpty()) {
            if (element instanceof Method) {
                context.getWorkingOperation().addExtension(extension.name(), extension.value());
            } else {
                context.getApi().addExtension(extension.name(), extension.value());
            }
        }
    }

    @Override
    public void visitOperation(Operation operation, AnnotatedElement element, ApiContext context) {
        OperationImpl.merge(operation, context.getWorkingOperation(), true);
        // If the operation should be hidden, remove it
        if (operation.hidden()) {
            ModelUtils.removeOperation(context.getApi().getPaths().get(context.getPath()),
                    context.getWorkingOperation());
        }
    }

    @Override
    public void visitCallback(Callback callback, AnnotatedElement element, ApiContext context) {
        if (element instanceof Method) {
            org.eclipse.microprofile.openapi.models.callbacks.Callback callbackModel = context.getWorkingOperation()
                    .getCallbacks().getOrDefault(callback.name(), new CallbackImpl());
            context.getWorkingOperation().getCallbacks().put(callback.name(), callbackModel);
            CallbackImpl.merge(callback, callbackModel, true, context.getApi().getComponents().getSchemas());
        }
    }

    @Override
    public void visitCallbacks(Callbacks callbacks, AnnotatedElement element, ApiContext context) {
        for (Callback callback : callbacks.value()) {
            visitCallback(callback, element, context);
        }
    }

    @Override
    public void visitRequestBody(RequestBody requestBody, AnnotatedElement element, ApiContext context) {
        if (element instanceof Method) {
            if (context.getWorkingOperation().getRequestBody() != null) {
                RequestBodyImpl.merge(requestBody, context.getWorkingOperation().getRequestBody(), true,
                        context.getApi().getComponents().getSchemas());
            }
        }
        if (element instanceof java.lang.reflect.Parameter) {
            if (context.getWorkingOperation().getRequestBody() != null) {
                RequestBodyImpl.merge(requestBody, context.getWorkingOperation().getRequestBody(), true,
                        context.getApi().getComponents().getSchemas());
            }
        }
    }

    @Override
    public void visitAPIResponse(APIResponse apiResponse, AnnotatedElement element, ApiContext context) {
        APIResponsesImpl.merge(apiResponse, context.getWorkingOperation().getResponses(), true,
                context.getApi().getComponents().getSchemas());

        // If an APIResponse has been processed that isn't the default
        if (apiResponse.responseCode() != null && !apiResponse.responseCode().isEmpty() && !apiResponse.responseCode()
                .equals(org.eclipse.microprofile.openapi.models.responses.APIResponses.DEFAULT)) {
            // If the element doesn't also contain a response mapping to the default
            if (Arrays.asList(element.getDeclaredAnnotationsByType(APIResponse.class)).stream()
                    .noneMatch(a -> a.responseCode() == null || a.responseCode().isEmpty() || a.responseCode()
                            .equals(org.eclipse.microprofile.openapi.models.responses.APIResponses.DEFAULT))) {
                // Then remove the default response
                context.getWorkingOperation().getResponses()
                        .remove(org.eclipse.microprofile.openapi.models.responses.APIResponses.DEFAULT);
            }
        }
    }

    @Override
    public void visitAPIResponses(APIResponses apiResponses, AnnotatedElement element, ApiContext context) {
        for (APIResponse response : apiResponses.value()) {
            visitAPIResponse(response, element, context);
        }
    }

    @Override
    public void visitParameter(Parameter parameter, AnnotatedElement element, ApiContext context) {
        org.eclipse.microprofile.openapi.models.parameters.Parameter matchedParam = null;

        if (element instanceof java.lang.reflect.Parameter) {
            // Find the matching parameter, and match it
            for (org.eclipse.microprofile.openapi.models.parameters.Parameter param : context.getWorkingOperation()
                    .getParameters()) {
                if (param.getName() != null
                        && param.getName().equals(ModelUtils.getParameterName((java.lang.reflect.Parameter) element))) {
                    matchedParam = param;
                }
            }
        }
        if (element instanceof Method) {
            // If the parameter reference is valid
            if (parameter.name() != null && !parameter.name().isEmpty()) {
                // Get all parameters with the same name
                List<java.lang.reflect.Parameter> matchingMethodParameters = Arrays
                        .asList(Method.class.cast(element).getParameters()).stream()
                        .filter(x -> ModelUtils.getParameterName(x).equals(parameter.name()))
                        .collect(Collectors.toList());
                // If there is more than one match, filter it further
                if (matchingMethodParameters.size() > 1 && parameter.in() != null
                        && parameter.in() != ParameterIn.DEFAULT) {
                    // Remove all parameters of the wrong input type
                    matchingMethodParameters
                            .removeIf(x -> ModelUtils.getParameterType(x) != In.valueOf(parameter.in().name()));
                }
                // If there's only one matching parameter, handle it immediately
                String matchingMethodParamName = ModelUtils.getParameterName(matchingMethodParameters.get(0));
                // Find the matching operation parameter
                for (org.eclipse.microprofile.openapi.models.parameters.Parameter operationParam : context
                        .getWorkingOperation().getParameters()) {
                    if (operationParam.getName().equals(matchingMethodParamName)) {
                        matchedParam = operationParam;
                    }
                }
            }
        }

        if (matchedParam != null) {
            ParameterImpl.merge(parameter, matchedParam, true, context.getApi().getComponents().getSchemas());

            // If a content was added, and a schema type exists, reconfigure the schema type
            if (matchedParam.getContent() != null && matchedParam.getSchema() != null
                    && matchedParam.getSchema().getType() != null) {
                SchemaType type = matchedParam.getSchema().getType();
                matchedParam.setSchema(null);

                for (MediaType mediaType : matchedParam.getContent().values()) {
                    if (mediaType.getSchema() == null) {
                        mediaType.setSchema(new SchemaImpl());
                    }
                    mediaType.getSchema()
                            .setType(ModelUtils.mergeProperty(mediaType.getSchema().getType(), type, false));
                }
            }
        }
    }

    @Override
    public void visitExternalDocumentation(ExternalDocumentation externalDocs, AnnotatedElement element,
            ApiContext context) {
        if (element instanceof Method) {
            org.eclipse.microprofile.openapi.models.ExternalDocumentation newExternalDocs = new ExternalDocumentationImpl();
            ExternalDocumentationImpl.merge(externalDocs, newExternalDocs, true);
            if (newExternalDocs.getUrl() != null && !newExternalDocs.getUrl().isEmpty()) {
                context.getWorkingOperation().setExternalDocs(newExternalDocs);
            }
        }
    }

    @Override
    public void visitServer(Server server, AnnotatedElement element, ApiContext context) {
        if (element instanceof Method) {
            org.eclipse.microprofile.openapi.models.servers.Server newServer = new ServerImpl();
            context.getWorkingOperation().addServer(newServer);
            ServerImpl.merge(server, newServer, true);
        }
    }

    @Override
    public void visitServers(Servers servers, AnnotatedElement element, ApiContext context) {
        for (Server server : servers.value()) {
            visitServer(server, element, context);
        }
    }

    @Override
    public void visitTag(Tag tag, AnnotatedElement element, ApiContext context) {
        if (element instanceof Method) {
            TagImpl.merge(tag, context.getWorkingOperation(), true, context.getApi().getTags());
        } else {
            org.eclipse.microprofile.openapi.models.tags.Tag newTag = new TagImpl();
            TagImpl.merge(tag, newTag, true);
            if (newTag.getName() != null && !newTag.getName().isEmpty()) {
                context.getApi().getTags().add(newTag);
            }
        }
    }

    @Override
    public void visitTags(Tags tags, AnnotatedElement element, ApiContext context) {
        if (element instanceof Method) {
            for (Tag tag : tags.value()) {
                visitTag(tag, element, context);
            }
            for (String ref : tags.refs()) {
                if (ref != null && !ref.isEmpty()) {
                    context.getWorkingOperation().addTag(ref);
                }
            }
        }
    }

    @Override
    public void visitSecurityScheme(SecurityScheme securityScheme, AnnotatedElement element, ApiContext context) {
        if (securityScheme.securitySchemeName() != null && !securityScheme.securitySchemeName().isEmpty()) {
            org.eclipse.microprofile.openapi.models.security.SecurityScheme newScheme = context.getApi().getComponents()
                    .getSecuritySchemes().getOrDefault(securityScheme.securitySchemeName(), new SecuritySchemeImpl());
            context.getApi().getComponents().addSecurityScheme(securityScheme.securitySchemeName(), newScheme);
            SecuritySchemeImpl.merge(securityScheme, newScheme, true);
        }
    }

    @Override
    public void visitSecuritySchemes(SecuritySchemes securitySchemes, AnnotatedElement element, ApiContext context) {
        for (SecurityScheme securityScheme : securitySchemes.value()) {
            visitSecurityScheme(securityScheme, element, context);
        }
    }

    @Override
    public void visitSecurityRequirement(SecurityRequirement securityRequirement, AnnotatedElement element,
            ApiContext context) {
        if (element instanceof Method) {
            if (securityRequirement.name() != null && !securityRequirement.name().isEmpty()) {
                org.eclipse.microprofile.openapi.models.security.SecurityRequirement model = new SecurityRequirementImpl();
                SecurityRequirementImpl.merge(securityRequirement, model);
                context.getWorkingOperation().addSecurityRequirement(model);
            }
        }
    }

    @Override
    public void visitSecurityRequirements(SecurityRequirements securityRequirements, AnnotatedElement element,
            ApiContext context) {
        for (SecurityRequirement requirement : securityRequirements.value()) {
            visitSecurityRequirement(requirement, element, context);
        }
    }

    // PRIVATE METHODS

    /**
     * Generates a map listing the location each resource class is mapped to.
     */
    private Map<String, Set<Class<?>>> generateResourceMapping(Set<Class<?>> classList) {
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

    private org.eclipse.microprofile.openapi.models.parameters.RequestBody insertDefaultRequestBody(ApiContext context,
            org.eclipse.microprofile.openapi.models.Operation operation, Method method) {
        org.eclipse.microprofile.openapi.models.parameters.RequestBody requestBody = new RequestBodyImpl();

        // Get the request body type of the method
        Class<?> bodyType = null;
        for (java.lang.reflect.Parameter methodParam : method.getParameters()) {
            if (ModelUtils.isRequestBody(methodParam)) {
                bodyType = methodParam.getType();
                break;
            }
        }
        if (bodyType == null) {
            return null;
        }

        // Create the default request body with a wildcard mediatype
        MediaType mediaType = new MediaTypeImpl().schema(createSchema(context, bodyType));
        requestBody.getContent().addMediaType(javax.ws.rs.core.MediaType.WILDCARD, mediaType);

        operation.setRequestBody(requestBody);
        return requestBody;
    }

    /**
     * Creates a new {@link APIResponse} to model the default response of a
     * {@link Method}, and inserts it into the {@link Operation} responses.
     * 
     * @param context   the API context.
     * @param operation the {@link Operation} to add the default response to.
     * @param method    the {@link Method} to model the default response on.
     * @return the newly created {@link APIResponse}.
     */
    private org.eclipse.microprofile.openapi.models.responses.APIResponse insertDefaultResponse(ApiContext context,
            org.eclipse.microprofile.openapi.models.Operation operation, Method method) {
        org.eclipse.microprofile.openapi.models.responses.APIResponse defaultResponse = new APIResponseImpl();
        defaultResponse.setDescription("Default Response.");

        // Create the default response with a wildcard mediatype
        MediaType mediaType = new MediaTypeImpl().schema(createSchema(context, method.getReturnType()));
        defaultResponse.getContent().addMediaType(javax.ws.rs.core.MediaType.WILDCARD, mediaType);

        // Add the default response
        operation.setResponses(new APIResponsesImpl().addApiResponse(
                org.eclipse.microprofile.openapi.models.responses.APIResponses.DEFAULT, defaultResponse));
        return defaultResponse;
    }

    /**
     * @return the {@link javax.ws.rs.core.MediaType} with the given name. Defaults
     *         to <code>WILDCARD</code>.
     */
    private String getContentType(String name) {
        String contentType = javax.ws.rs.core.MediaType.WILDCARD;
        try {
            javax.ws.rs.core.MediaType mediaType = javax.ws.rs.core.MediaType.valueOf(name);
            if (mediaType != null) {
                contentType = mediaType.toString();
            }
        } catch (IllegalArgumentException ex) {
            LOGGER.log(FINE, "Unrecognised content type.", ex);
        }
        return contentType;
    }

    private org.eclipse.microprofile.openapi.models.media.Schema createSchema(ApiContext context, Class<?> type) {
        org.eclipse.microprofile.openapi.models.media.Schema schema = new SchemaImpl();
        SchemaType schemaType = ModelUtils.getSchemaType(type);
        schema.setType(schemaType);

        // Set the subtype if it's an array (for example an array of ints)
        if (schemaType == SchemaType.ARRAY) {
            Class<?> subType = type.getComponentType();
            org.eclipse.microprofile.openapi.models.media.Schema subSchema = schema;
            while (subType != null) {
                subSchema.setItems(new SchemaImpl().type(ModelUtils.getSchemaType(subType)));
                subSchema = schema.getItems();
                subType = subType.getComponentType();
            }
        }

        // If the schema is an object, insert the reference
        if (schemaType == SchemaType.OBJECT) {
            if (insertObjectReference(context, schema, type)) {
                schema.setType(null);
                schema.setItems(null);
            }
        }

        return schema;
    }

    private org.eclipse.microprofile.openapi.models.media.Schema createSchema(ApiContext context, 
            AnnotatedElement annotatedElement, Class<?> type) {
        SchemaType schemaType = ModelUtils.getSchemaType(type);

        // If the annotated element is the same type as the reference class, return a null schema
        if (schemaType == SchemaType.OBJECT && type.equals(annotatedElement)) {
            org.eclipse.microprofile.openapi.models.media.Schema schema = new SchemaImpl();
            schema.setType(null);
            schema.setItems(null);
            return schema;
        } else {
            return createSchema(context, type);
        }  
    }
    
    /**
     * Replace the object in the referee with a reference, and create the reference
     * in the API.
     * 
     * @param context        the API context.
     * @param referee        the object containing the reference.
     * @param referenceClass the class of the object being referenced.
     * @return if the reference has been created.
     */
    private boolean insertObjectReference(ApiContext context, Reference<?> referee, Class<?> referenceClass) {

        // If the object is a java core class
        if (referenceClass == null || referenceClass.getName().startsWith("java.") || referenceClass.isPrimitive()) {
            return false;
        }

        // If the object is a Java EE object type
        if (referenceClass.getName().startsWith("javax.")) {
            return false;
        }

        // Check the class exists in the application
        if (!classes.contains(referenceClass)) {
            return false;
        }

        // Set the reference name
        referee.setRef(referenceClass.getSimpleName());

        // Create the schema
        visitSchema(referenceClass.getDeclaredAnnotation(Schema.class), referenceClass, context);

        return true;
    }

}
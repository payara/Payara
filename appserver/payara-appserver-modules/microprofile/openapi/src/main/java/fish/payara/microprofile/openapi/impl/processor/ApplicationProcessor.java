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
package fish.payara.microprofile.openapi.impl.processor;

import fish.payara.microprofile.openapi.api.processor.OASProcessor;
import fish.payara.microprofile.openapi.api.visitor.ApiContext;
import fish.payara.microprofile.openapi.api.visitor.ApiVisitor;
import fish.payara.microprofile.openapi.api.visitor.ApiWalker;
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
import fish.payara.microprofile.openapi.impl.model.util.AnnotationInfo;
import fish.payara.microprofile.openapi.impl.model.util.ModelUtils;
import fish.payara.microprofile.openapi.impl.visitor.OpenApiWalker;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import static java.util.Collections.singleton;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import static java.util.stream.Collectors.toSet;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.Reference;
import org.eclipse.microprofile.openapi.models.media.MediaType;
import org.eclipse.microprofile.openapi.models.media.Schema.SchemaType;
import org.eclipse.microprofile.openapi.models.parameters.Parameter.In;
import org.eclipse.microprofile.openapi.models.parameters.Parameter.Style;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.hk2.classmodel.reflect.AnnotatedElement;
import org.glassfish.hk2.classmodel.reflect.AnnotationModel;
import org.glassfish.hk2.classmodel.reflect.ClassModel;
import org.glassfish.hk2.classmodel.reflect.EnumType;
import org.glassfish.hk2.classmodel.reflect.ExtensibleType;
import org.glassfish.hk2.classmodel.reflect.FieldModel;
import org.glassfish.hk2.classmodel.reflect.MethodModel;
import org.glassfish.hk2.classmodel.reflect.Type;
import org.glassfish.hk2.classmodel.reflect.Types;
import org.glassfish.internal.data.ApplicationInfo;

/**
 * A processor to parse the application for annotations, to add to the OpenAPI
 * model.
 */
public class ApplicationProcessor implements OASProcessor, ApiVisitor {

    private static final Logger LOGGER = Logger.getLogger(ApplicationProcessor.class.getName());

    /**
     * A list of all classes in the given application.
     */
    private final Types allTypes;

    /**
     * A list of allowed classes for scanning
     */
    private final Set<Type> allowedTypes;

    private final ClassLoader appClassLoader;

    private OpenApiWalker apiWalker;

    public ApplicationProcessor(ApplicationInfo appInfo) {
        this(appInfo.getTypes(), filterTypes(appInfo), appInfo.getAppClassLoader());
    }

    /**
     * @param types parsed application classes
     * @param appClassLoader the class loader for the application.
     */
    public ApplicationProcessor(Types allTypes, Set<Type> allowedTypes, ClassLoader appClassLoader) {
        this.allTypes = allTypes;
        this.allowedTypes = allowedTypes;
        this.appClassLoader = appClassLoader;
    }

    /**
     * @return a list of all classes in the archive.
     */
    private static Set<Type> filterTypes(ApplicationInfo appInfo) {
        ReadableArchive archive = appInfo.getSource();
        return Collections.list(archive.entries()).stream()
                // Only use the classes
                .filter(clazz -> clazz.endsWith(".class"))
                // Remove the WEB-INF/classes and return the proper class name format
                .map(clazz -> clazz.replaceAll("WEB-INF/classes/", "").replace("/", ".").replace(".class", ""))
                // Fetch class type
                .map(clazz -> appInfo.getTypes().getBy(clazz))
                // Don't return null classes
                .filter(Objects::nonNull)
                .collect(toSet());
    }

    @Override
    public OpenAPI process(OpenAPI api, OpenApiConfiguration config) {
        if (config == null || !config.getScanDisable()) {
            this.apiWalker = new OpenApiWalker(
                    api,
                    allTypes,
                    config == null ? allowedTypes : config.getValidClasses(allowedTypes),
                    appClassLoader
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
    public void visitPOST(AnnotationModel post, MethodModel element, ApiContext context) {
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
    public void visitPUT(AnnotationModel put, MethodModel element, ApiContext context) {
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
    public void visitDELETE(AnnotationModel delete, MethodModel element, ApiContext context) {
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
    public void visitHEAD(AnnotationModel head, MethodModel element, ApiContext context) {
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
    public void visitOPTIONS(AnnotationModel options, MethodModel element, ApiContext context) {
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
    public void visitPATCH(AnnotationModel patch, MethodModel element, ApiContext context) {
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
    public void visitProduces(AnnotationModel produces, AnnotatedElement element, ApiContext context) {
        if (element instanceof MethodModel && context.getWorkingOperation() != null) {
            for (org.eclipse.microprofile.openapi.models.responses.APIResponse response : context.getWorkingOperation()
                    .getResponses().values()) {

                if (response != null) {
                    // Find the wildcard return type
                    if (response.getContent() != null
                            && response.getContent().getMediaType(javax.ws.rs.core.MediaType.WILDCARD) != null) {
                        MediaType wildcardMedia = response.getContent().getMediaType(javax.ws.rs.core.MediaType.WILDCARD);

                        // Copy the wildcard return type to the valid response types
                        List<String> mediaTypes = produces.getValue("value", List.class);
                        for (String mediaType : mediaTypes) {
                            response.getContent().addMediaType(getContentType(mediaType), wildcardMedia);
                        }
                        // If there is an @Produces, remove the wildcard
                        response.getContent().removeMediaType(javax.ws.rs.core.MediaType.WILDCARD);
                    }
                }
            }
        }
    }

    @Override
    public void visitConsumes(AnnotationModel consumes, AnnotatedElement element, ApiContext context) {
        if (element instanceof MethodModel && context.getWorkingOperation() != null) {
            org.eclipse.microprofile.openapi.models.parameters.RequestBody requestBody = context.getWorkingOperation()
                    .getRequestBody();

            if (requestBody != null) {
                // Find the wildcard return type
                if (requestBody.getContent() != null
                        && requestBody.getContent().getMediaType(javax.ws.rs.core.MediaType.WILDCARD) != null) {
                    MediaType wildcardMedia = requestBody.getContent().getMediaType(javax.ws.rs.core.MediaType.WILDCARD);

                    // Copy the wildcard return type to the valid request body types
                    List<String> mediaTypes = consumes.getValue("value", List.class);
                    for (String mediaType : mediaTypes) {
                        requestBody.getContent().addMediaType(getContentType(mediaType), wildcardMedia);
                    }
                    // If there is an @Consumes, remove the wildcard
                    requestBody.getContent().removeMediaType(javax.ws.rs.core.MediaType.WILDCARD);
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

        if (context.getWorkingOperation() != null) {
            // If there's no request body, fill out a new one right down to the schema
            if (context.getWorkingOperation().getRequestBody() == null) {
                context.getWorkingOperation().setRequestBody(new RequestBodyImpl().content(new ContentImpl()
                        .addMediaType(javax.ws.rs.core.MediaType.WILDCARD, new MediaTypeImpl()
                                .schema(new SchemaImpl()))));
            }

            // Set the request body type accordingly.
            context.getWorkingOperation().getRequestBody().getContent()
                    .getMediaType(javax.ws.rs.core.MediaType.WILDCARD).getSchema()
                    .setType(formSchemaType);
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

    private static void addParameter(AnnotatedElement element, ApiContext context, String name, In in, Boolean required) {
        org.eclipse.microprofile.openapi.models.parameters.Parameter newParameter = new ParameterImpl();
        newParameter.setName(name);
        newParameter.setIn(in);
        newParameter.setStyle(Style.SIMPLE);
        newParameter.setRequired(required);
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

        if (context.getWorkingOperation() != null) {
            context.getWorkingOperation().addParameter(newParameter);
        } else {
            LOGGER.log(
                    SEVERE,
                    "Couldn''t add {0} parameter, \"{1}\" to the OpenAPI Document. This is usually caused by declaring parameter under a method with an unsupported annotation.",
                    new Object[]{newParameter.getIn(), newParameter.getName()}
            );
        }
    }

    private static SchemaImpl getArraySchema(AnnotatedElement element, ApiContext context) {
        SchemaImpl arraySchema = new SchemaImpl();
        List<org.glassfish.hk2.classmodel.reflect.ParameterizedType> parameterizedType;

        if (element instanceof org.glassfish.hk2.classmodel.reflect.Parameter) {
            org.glassfish.hk2.classmodel.reflect.Parameter parameter = (org.glassfish.hk2.classmodel.reflect.Parameter) element;
            parameterizedType = parameter.getGenericTypes();
        } else {
            FieldModel field = (FieldModel) element;
            parameterizedType = field.getGenericTypes();
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
            visitSchemaClass(annotation, (ClassModel) element, context);
        } else if (element instanceof EnumType) {
            vistEnumClass(annotation, (EnumType) element, context);
        } else if (element instanceof FieldModel) {
            visitSchemaField(annotation, (FieldModel) element, context);
        } else if (element instanceof org.glassfish.hk2.classmodel.reflect.Parameter) {
            visitSchemaParameter(annotation, (org.glassfish.hk2.classmodel.reflect.Parameter) element, context);
        }
    }

    private void vistEnumClass(AnnotationModel schemaAnnotation, EnumType enumType, ApiContext context) {
        // Get the schema object name
        String schemaName = (schemaAnnotation == null) ? null : schemaAnnotation.getValue("name", String.class);
        if (schemaName == null || schemaName.isEmpty()) {
            schemaName = enumType.getSimpleName();
        }
        org.eclipse.microprofile.openapi.models.media.Schema schema = SchemaImpl.createInstance(schemaAnnotation, context);

        org.eclipse.microprofile.openapi.models.media.Schema newSchema = new SchemaImpl();
        context.getApi().getComponents().addSchema(schemaName, newSchema);
        if (schema != null) {
            SchemaImpl.merge(schema, newSchema, true, context);
        }
        if (schema == null || schema.getEnumeration().isEmpty()) {
            //if the schema annotation does not specify enums, then all enum fields will be added
            for (FieldModel enumField : enumType.getStaticFields()) {
                newSchema.addEnumeration(enumField);
            }
        }

    }

    private void visitSchemaClass(AnnotationModel schemaAnnotation, ClassModel clazz, ApiContext context) {
        // Get the schema object name
        String schemaName = (schemaAnnotation == null) ? null : schemaAnnotation.getValue("name", String.class);
        if (schemaName == null || schemaName.isEmpty()) {
            schemaName = clazz.getSimpleName();
        }
        org.eclipse.microprofile.openapi.models.media.Schema schema = SchemaImpl.createInstance(schemaAnnotation, context);

        // Add a new schema
        org.eclipse.microprofile.openapi.models.media.Schema newSchema = new SchemaImpl();
        context.getApi().getComponents().addSchema(schemaName, newSchema);

        // If there is an annotation
        if (schema != null) {
            SchemaImpl.merge(schema, newSchema, true, context);
        } else {
            newSchema.setType(SchemaType.OBJECT);
            Map<String, org.eclipse.microprofile.openapi.models.media.Schema> fields = new LinkedHashMap<>();
            for (FieldModel field : clazz.getFields()) {
                if (!field.isTransient()) {
                    fields.put(field.getName(), createSchema(context, clazz, field));
                }
            }
            newSchema.setProperties(fields);
        }

        // If there is an extending class, add the data
        if (clazz.getParent() != null) {
            ClassModel superClass = clazz.getParent();

            // If the super class is legitimate
            if (superClass != null) {

                // Get the parent schema annotation
                AnnotationModel parentSchemAnnotation = superClass.getAnnotation(Schema.class.getName());

                if (parentSchemAnnotation != null) {
                    // Create a schema for the parent
                    visitSchema(parentSchemAnnotation, superClass, context);

                    // Get the superclass schema name
                    String parentSchemaName = parentSchemAnnotation.getValue("name", String.class);
                    if (parentSchemaName == null || parentSchemaName.isEmpty()) {
                        parentSchemaName = superClass.getSimpleName();
                    }

                    // Link the schemas
                    newSchema.addAllOf(new SchemaImpl().ref(parentSchemaName));
                }
            }
        }
    }

    private static void visitSchemaField(AnnotationModel schemaAnnotation, FieldModel field, ApiContext context) {
        // Get the schema object name
        String schemaName = (schemaAnnotation == null) ? null : schemaAnnotation.getValue("name", String.class);
        if (schemaName == null || schemaName.isEmpty()) {
            schemaName = field.getName();
        }
        org.eclipse.microprofile.openapi.models.media.Schema schema = SchemaImpl.createInstance(schemaAnnotation, context);

        // Get the parent schema object name
        String parentName = null;
        AnnotationModel classSchemaAnnotation = field.getDeclaringType().getAnnotation(Schema.class.getName());
        if (classSchemaAnnotation != null) {
            parentName = classSchemaAnnotation.getValue("name", String.class);
        }
        if (parentName == null || parentName.isEmpty()) {
            parentName = field.getDeclaringType().getSimpleName();
        }

        // Get or create the parent schema object
        org.eclipse.microprofile.openapi.models.media.Schema parent = context.getApi().getComponents().getSchemas()
                .getOrDefault(parentName, new SchemaImpl());
        context.getApi().getComponents().getSchemas().put(parentName, parent);

        org.eclipse.microprofile.openapi.models.media.Schema property = new SchemaImpl();
        parent.addProperty(schemaName, property);
        property.setType(ModelUtils.getSchemaType(field.getTypeName(), context));
        SchemaImpl.merge(schema, property, true, context);
    }

    private static void visitSchemaParameter(AnnotationModel schemaAnnotation, org.glassfish.hk2.classmodel.reflect.Parameter parameter, ApiContext context) {
        // If this is being parsed at the start, ignore it as the path doesn't exist
        if (context.getWorkingOperation() == null) {
            return;
        }
        // Check if it's a request body
        if (ModelUtils.isRequestBody(parameter)) {
            if (context.getWorkingOperation().getRequestBody() == null) {
                context.getWorkingOperation().setRequestBody(new RequestBodyImpl());
            }
            // Insert the schema to the request body media type
            MediaType mediaType = context.getWorkingOperation().getRequestBody().getContent()
                    .getMediaType(javax.ws.rs.core.MediaType.WILDCARD);
            org.eclipse.microprofile.openapi.models.media.Schema schema = SchemaImpl.createInstance(schemaAnnotation, context);
            SchemaImpl.merge(schema, mediaType.getSchema(), true, context);
            if (schema.getRef() != null && !schema.getRef().isEmpty()) {
                mediaType.setSchema(new SchemaImpl().ref(schema.getRef()));
            }
        } else if (ModelUtils.getParameterType(parameter) != null) {
            for (org.eclipse.microprofile.openapi.models.parameters.Parameter param : context.getWorkingOperation()
                    .getParameters()) {
                if (param.getName().equals(ModelUtils.getParameterName(parameter))) {
                    org.eclipse.microprofile.openapi.models.media.Schema schema = SchemaImpl.createInstance(schemaAnnotation, context);
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
        if (annotation.getValue("hidden", Boolean.class)) {
            ModelUtils.removeOperation(context.getApi().getPaths().getPathItem(context.getPath()),
                    context.getWorkingOperation());
        }
    }

    @Override
    public void visitCallback(AnnotationModel annotation, AnnotatedElement element, ApiContext context) {
        if (element instanceof MethodModel) {
            String name = annotation.getValue("name", String.class);
            org.eclipse.microprofile.openapi.models.callbacks.Callback callbackModel = context.getWorkingOperation()
                    .getCallbacks().getOrDefault(name, new CallbackImpl());
            context.getWorkingOperation().getCallbacks().put(name, callbackModel);
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
            org.eclipse.microprofile.openapi.models.parameters.RequestBody currentRequestBody = context
                    .getWorkingOperation().getRequestBody();
            if (currentRequestBody != null || element instanceof org.glassfish.hk2.classmodel.reflect.Parameter) {
                RequestBodyImpl.merge(RequestBodyImpl.createInstance(annotation, context), currentRequestBody, true, context);
            }
        }
    }

    @Override
    public void visitAPIResponse(AnnotationModel annotation, AnnotatedElement element, ApiContext context) {
        APIResponseImpl apiResponse = APIResponseImpl.createInstance(annotation, context);
        APIResponsesImpl.merge(apiResponse, context.getWorkingOperation().getResponses(), true, context);

        // If an APIResponse has been processed that isn't the default
        String responseCode = apiResponse.getResponseCode();
        if (responseCode != null && !responseCode.isEmpty() && !responseCode
                .equals(org.eclipse.microprofile.openapi.models.responses.APIResponses.DEFAULT)) {
            // If the element doesn't also contain a response mapping to the default
            AnnotationModel apiResponsesParent = element.getAnnotation(APIResponses.class.getName());
            if (apiResponsesParent != null) {
                List<AnnotationModel> apiResponses = apiResponsesParent.getValue("value", List.class);
                if (apiResponses.stream()
                        .map(a -> a.getValue("responseCode", String.class))
                        .noneMatch(code -> code == null || code.isEmpty() || code.equals(org.eclipse.microprofile.openapi.models.responses.APIResponses.DEFAULT))) {
                    // Then remove the default response
                    context.getWorkingOperation().getResponses()
                            .removeAPIResponse(org.eclipse.microprofile.openapi.models.responses.APIResponses.DEFAULT);
                }
            } else {
                context.getWorkingOperation().getResponses()
                        .removeAPIResponse(org.eclipse.microprofile.openapi.models.responses.APIResponses.DEFAULT);
            }
        }
    }

    @Override
    public void visitAPIResponses(AnnotationModel annotation, AnnotatedElement element, ApiContext context) {
        List<AnnotationModel> responses = annotation.getValue("value", List.class);
        if (responses != null) {
            responses.forEach(response -> visitAPIResponse(response, element, context));
        }
    }

    @Override
    public void visitParameters(AnnotationModel annotation, AnnotatedElement element, ApiContext context) {
        List<AnnotationModel> parameters = annotation.getValue("value", List.class);
        if (parameters != null) {
            parameters.forEach(parameter -> visitParameter(parameter, element, context));
        }
    }

    @Override
    public void visitParameter(AnnotationModel annotation, AnnotatedElement element, ApiContext context) {
        org.eclipse.microprofile.openapi.models.parameters.Parameter matchedParam = null;
        org.eclipse.microprofile.openapi.models.parameters.Parameter parameter = ParameterImpl.createInstance(annotation, context);

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
                    && !matchedParam.getContent().values().isEmpty()
                    && matchedParam.getSchema() != null
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

    private static org.eclipse.microprofile.openapi.models.parameters.Parameter findOperationParameterFor(
            org.eclipse.microprofile.openapi.models.parameters.Parameter parameter,
            MethodModel annotated,
            ApiContext context) {
        String name = parameter.getName();
        // If the parameter reference is valid
        if (name != null && !name.isEmpty()) {
            // Get all parameters with the same name
            List<org.glassfish.hk2.classmodel.reflect.Parameter> matchingMethodParameters = annotated.getParameters()
                    .stream()
                    .filter(x -> name.equals(ModelUtils.getParameterName(x)))
                    .collect(Collectors.toList());
            // If there is more than one match, filter it further
            In in = parameter.getIn();
            if (matchingMethodParameters.size() > 1 && in != null) {
                // Remove all parameters of the wrong input type
                matchingMethodParameters
                        .removeIf(x -> ModelUtils.getParameterType(x) != In.valueOf(in.name()));
            }
            if (matchingMethodParameters.isEmpty()) {
                return null;
            }
            // If there's only one matching parameter, handle it immediately
            String matchingMethodParamName = ModelUtils.getParameterName(matchingMethodParameters.get(0));
            // Find the matching operation parameter
            for (org.eclipse.microprofile.openapi.models.parameters.Parameter operationParam : context
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
    private static org.eclipse.microprofile.openapi.models.parameters.Parameter findOperationParameterFor(
            org.glassfish.hk2.classmodel.reflect.Parameter annotated, ApiContext context) {
        String actualName = ModelUtils.getParameterName(annotated);
        if (actualName == null) {
            return null;
        }
        for (org.eclipse.microprofile.openapi.models.parameters.Parameter param : context.getWorkingOperation()
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
            org.eclipse.microprofile.openapi.models.ExternalDocumentation newExternalDocs = new ExternalDocumentationImpl();
            ExternalDocumentationImpl.merge(ExternalDocumentationImpl.createInstance(externalDocs), newExternalDocs, true);
            if (newExternalDocs.getUrl() != null && !newExternalDocs.getUrl().isEmpty()) {
                context.getWorkingOperation().setExternalDocs(newExternalDocs);
            }
        }
    }

    @Override
    public void visitServer(AnnotationModel server, AnnotatedElement element, ApiContext context) {
        if (element instanceof MethodModel) {
            org.eclipse.microprofile.openapi.models.servers.Server newServer = new ServerImpl();
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
        org.eclipse.microprofile.openapi.models.tags.Tag from = TagImpl.createInstance(annotation, context);
        if (element instanceof MethodModel) {
            TagImpl.merge(from, context.getWorkingOperation(), true, context.getApi().getTags());
        } else {
            org.eclipse.microprofile.openapi.models.tags.Tag newTag = new TagImpl();
            TagImpl.merge(from, newTag, true);
            if (newTag.getName() != null && !newTag.getName().isEmpty()) {
                context.getApi().getTags().add(newTag);
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
        org.eclipse.microprofile.openapi.models.security.SecurityScheme securityScheme = SecuritySchemeImpl.createInstance(annotation, context);
        if (securitySchemeName != null && !securitySchemeName.isEmpty()) {
            org.eclipse.microprofile.openapi.models.security.SecurityScheme newScheme = context.getApi().getComponents()
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
            org.eclipse.microprofile.openapi.models.security.SecurityRequirement securityRequirement = SecurityRequirementImpl.createInstance(annotation, context);
            if (securityRequirementName != null && !securityRequirementName.isEmpty()) {
                org.eclipse.microprofile.openapi.models.security.SecurityRequirement model = new SecurityRequirementImpl();
                SecurityRequirementImpl.merge(securityRequirement, model);
                context.getWorkingOperation().addSecurityRequirement(model);
            }
        }
    }

    @Override
    public void visitSecurityRequirements(AnnotationModel annotation, AnnotatedElement element, ApiContext context) {
        List<AnnotationModel> securityRequirements = annotation.getValue("value", List.class);
        if (securityRequirements != null) {
            securityRequirements.forEach(securityRequirement -> visitSecurityRequirement(securityRequirement, element, context));
        }
    }

    // PRIVATE METHODS
    private org.eclipse.microprofile.openapi.models.parameters.RequestBody insertDefaultRequestBody(ApiContext context,
            org.eclipse.microprofile.openapi.models.Operation operation, MethodModel method) {
        org.eclipse.microprofile.openapi.models.parameters.RequestBody requestBody = new RequestBodyImpl();

        // Get the request body type of the method
        org.glassfish.hk2.classmodel.reflect.ParameterizedType bodyType = null;
        for (org.glassfish.hk2.classmodel.reflect.Parameter methodParam : method.getParameters()) {
            if (ModelUtils.isRequestBody(methodParam)) {
                bodyType = methodParam;
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
     * @param context the API context.
     * @param operation the {@link Operation} to add the default response to.
     * @param method the {@link Method} to model the default response on.
     * @return the newly created {@link APIResponse}.
     */
    private org.eclipse.microprofile.openapi.models.responses.APIResponse insertDefaultResponse(ApiContext context,
            org.eclipse.microprofile.openapi.models.Operation operation, MethodModel method) {
        org.eclipse.microprofile.openapi.models.responses.APIResponse defaultResponse = new APIResponseImpl();
        defaultResponse.setDescription("Default Response.");

        // Create the default response with a wildcard mediatype
        MediaType mediaType = new MediaTypeImpl().schema(
                createSchema(context, method.getReturnType())
        );
        defaultResponse.getContent().addMediaType(javax.ws.rs.core.MediaType.WILDCARD, mediaType);

        // Add the default response
        operation.setResponses(new APIResponsesImpl().addAPIResponse(
                org.eclipse.microprofile.openapi.models.responses.APIResponses.DEFAULT, defaultResponse));
        return defaultResponse;
    }

    /**
     * @return the {@link javax.ws.rs.core.MediaType} with the given name.
     * Defaults to <code>WILDCARD</code>.
     */
    private static String getContentType(String name) {
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

    private org.eclipse.microprofile.openapi.models.media.Schema createSchema(
            ApiContext context,
            org.glassfish.hk2.classmodel.reflect.ParameterizedType type) {

        String typeName = type.getTypeName();
        List<org.glassfish.hk2.classmodel.reflect.ParameterizedType> genericType = type.getGenericTypes();

        org.eclipse.microprofile.openapi.models.media.Schema schema = new SchemaImpl();
        SchemaType schemaType = ModelUtils.getSchemaType(type, context);
        schema.setType(schemaType);

        // Set the subtype if it's an array (for example an array of ints)
        if (schemaType == SchemaType.ARRAY) {
            if (type.isArray()) {
                schemaType = ModelUtils.getSchemaType(type.getTypeName(), context);
                schema.setType(schemaType);
            } else if (!genericType.isEmpty()) { // should be something Iterable
                    schema.setItems(createSchema(context, genericType.get(0)));
            }
        }

        // If the schema is an object, insert the reference
        if (schemaType == SchemaType.OBJECT) {
            if (insertObjectReference(context, schema, type.getType(), typeName)) {
                schema.setType(null);
                schema.setItems(null);
            }
        }

        return schema;
    }

    private org.eclipse.microprofile.openapi.models.media.Schema createSchema(
            ApiContext context,
            AnnotatedElement annotatedElement,
            org.glassfish.hk2.classmodel.reflect.ParameterizedType type) {

        SchemaType schemaType = ModelUtils.getSchemaType(type, context);

        // If the annotated element is the same type as the reference class, return a null schema
        if (schemaType == SchemaType.OBJECT && type.getType().equals(annotatedElement)) {
            org.eclipse.microprofile.openapi.models.media.Schema schema = new SchemaImpl();
            schema.setType(null);
            schema.setItems(null);
            return schema;
        }
        return createSchema(context, type);
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

        // If the object is a java core class
        if (referenceClassName == null || referenceClassName.startsWith("java.")) {
            return false;
        }

        // If the object is a Java EE object type
        if (referenceClassName.startsWith("javax.")) {
            return false;
        }

        // Check the class exists in the application
        if (!context.isApplicationType(referenceClassName)) {
            return false;
        }

        if (referenceClass != null && referenceClass instanceof ExtensibleType) {
            ExtensibleType referenceClassType = (ExtensibleType) referenceClass;
            final AnnotationModel schemaAnnotation = AnnotationInfo.valueOf(referenceClassType)
                    .getAnnotation(Schema.class);
            if (schemaAnnotation != null) {
                String schemaName = schemaAnnotation.getValue("name", String.class);

                if (schemaName == null || schemaName.isEmpty()) {
                    schemaName = ModelUtils.getSimpleName(referenceClassName);
                }
                // Set the reference name
                referee.setRef(schemaName);

                org.eclipse.microprofile.openapi.models.media.Schema schema = context.getApi().getComponents().getSchemas().get(schemaName);
                if (schema == null) {
                    // Create the schema
                    if (context.isAllowedType(referenceClassType)) {
                        visitSchema(schemaAnnotation, referenceClass, context);
                    } else {
                        apiWalker.processAnnotations(singleton(referenceClassType), Schema.class, this::visitSchema);
                    }
                }

                return true;
            }
        }

        return false;
    }

}

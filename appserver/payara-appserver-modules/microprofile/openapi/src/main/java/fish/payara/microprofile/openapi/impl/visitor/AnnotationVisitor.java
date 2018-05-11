package fish.payara.microprofile.openapi.impl.visitor;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.microprofile.openapi.annotations.ExternalDocumentation;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.callbacks.Callback;
import org.eclipse.microprofile.openapi.annotations.callbacks.Callbacks;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirements;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.servers.Server;
import org.eclipse.microprofile.openapi.annotations.servers.Servers;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.openapi.annotations.tags.Tags;
import org.eclipse.microprofile.openapi.models.Components;
import org.eclipse.microprofile.openapi.models.Extensible;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.media.MediaType;
import org.eclipse.microprofile.openapi.models.parameters.Parameter.In;
import org.eclipse.microprofile.openapi.models.responses.APIResponses;

import fish.payara.microprofile.openapi.api.visitor.ApiContext;
import fish.payara.microprofile.openapi.api.visitor.ApiVisitor;
import fish.payara.microprofile.openapi.impl.model.ExtensibleImpl;
import fish.payara.microprofile.openapi.impl.model.ExternalDocumentationImpl;
import fish.payara.microprofile.openapi.impl.model.OpenAPIImpl;
import fish.payara.microprofile.openapi.impl.model.OperationImpl;
import fish.payara.microprofile.openapi.impl.model.callbacks.CallbackImpl;
import fish.payara.microprofile.openapi.impl.model.media.SchemaImpl;
import fish.payara.microprofile.openapi.impl.model.parameters.ParameterImpl;
import fish.payara.microprofile.openapi.impl.model.parameters.RequestBodyImpl;
import fish.payara.microprofile.openapi.impl.model.responses.APIResponseImpl;
import fish.payara.microprofile.openapi.impl.model.security.SecurityRequirementImpl;
import fish.payara.microprofile.openapi.impl.model.security.SecuritySchemeImpl;
import fish.payara.microprofile.openapi.impl.model.servers.ServerImpl;
import fish.payara.microprofile.openapi.impl.model.tags.TagImpl;
import fish.payara.microprofile.openapi.impl.model.util.ModelUtils;

public class AnnotationVisitor implements ApiVisitor {

    @Override
    public void visitClass(Class<?> clazz, ApiContext context) {
        OpenAPI api = context.getApi();
        // Handle @OpenApiDefinition
        handleOpenAPIAnnotation(api, clazz);
        // Handle @SecurityScheme
        handleSecuritySchemeAnnotation(clazz, api.getComponents());
        // Handle @Extension
        handleExtensionAnnotation(clazz, context.getApi());
    }

    @Override
    public void visitMethod(Method method, ApiContext context) {
        if (context.getPath() == null) {
            return;
        }
        org.eclipse.microprofile.openapi.models.Operation operation = ModelUtils.findOperation(context.getApi(), method,
                context.getPath());

        // Handle @SecurityScheme
        handleSecuritySchemeAnnotation(method, context.getApi().getComponents());

        if (operation != null) {
            // Handle @Operation
            handleOperationAnnotation(method, operation, context.getApi().getPaths().get(context.getPath()));
            // Handle @ExternalDocumentation
            handleExternalDocumentationAnnotation(method, operation);
            // Handle @Servers
            handleServersAnnotation(method, operation);
            // Handle @Tags
            handleTagsAnnotation(method, operation, context.getApi());
            // Handle @APIResponse
            handleAPIResponseAnnotation(method, operation, context.getApi().getComponents().getSchemas());
            // Handle @SecurityRequirement
            handleSecurityRequirementAnnotation(method, operation);
            // Handle @Parameter
            handleParameterAnnotation(method, operation);
            // Handle @Callback
            handleCallbacksAnnotation(method, operation);
            // Handle @Extension
            handleExtensionAnnotation(method, operation);
        }
    }

    @Override
    public void visitField(Field field, ApiContext context) {
        // Handle @Schema
        handleSchemaAnnotation(field, context.getApi());
    }

    @Override
    public void visitParameter(Parameter parameter, ApiContext context) {
        org.eclipse.microprofile.openapi.models.Operation operation = ModelUtils.findOperation(context.getApi(),
                (Method) parameter.getDeclaringExecutable(), context.getPath());

        if (operation != null) {
            // Handle @RequestBody
            handleRequestBodyAnnotation(parameter, operation, context.getApi().getComponents().getSchemas());
            // Handle @Schema
            handleSchemaAnnotation(parameter, operation, context.getApi().getComponents().getSchemas());
            // Handle @Parameter
            handleParameterAnnotation(parameter, operation);
        }
    }

    private void handleParameterAnnotation(Parameter parameter,
            org.eclipse.microprofile.openapi.models.Operation operation) {
        if (parameter.isAnnotationPresent(org.eclipse.microprofile.openapi.annotations.parameters.Parameter.class)) {
            org.eclipse.microprofile.openapi.annotations.parameters.Parameter annotation = parameter
                    .getDeclaredAnnotation(org.eclipse.microprofile.openapi.annotations.parameters.Parameter.class);
            for (org.eclipse.microprofile.openapi.models.parameters.Parameter param : operation.getParameters()) {
                if (param.getName().equals(ModelUtils.getParameterName(parameter))) {
                    ParameterImpl.merge(annotation, param, false, null);
                }
            }
        }
    }

    private void handleParameterAnnotation(Method method, org.eclipse.microprofile.openapi.models.Operation operation) {
        if (method.isAnnotationPresent(org.eclipse.microprofile.openapi.annotations.parameters.Parameter.class)) {
            org.eclipse.microprofile.openapi.annotations.parameters.Parameter annotation = method
                    .getDeclaredAnnotation(org.eclipse.microprofile.openapi.annotations.parameters.Parameter.class);
            // If the parameter reference is valid
            if (annotation.name() != null && !annotation.name().isEmpty()) {
                // Get all parameters with the same name
                List<Parameter> matchingMethodParameters = Arrays.asList(method.getParameters()).stream()
                        .filter(x -> ModelUtils.getParameterName(x).equals(annotation.name()))
                        .collect(Collectors.toList());
                // If there is more than one match, filter it further
                if (matchingMethodParameters.size() > 1 && annotation.in() != null
                        && annotation.in() != ParameterIn.DEFAULT) {
                    // Remove all parameters of the wrong input type
                    matchingMethodParameters
                            .removeIf(x -> ModelUtils.getParameterType(x) != In.valueOf(annotation.in().name()));
                }
                // If there's only one matching parameter, handle it immediately
                Parameter matchingMethodParam = matchingMethodParameters.get(0);
                // Find the matching operation parameter
                for (org.eclipse.microprofile.openapi.models.parameters.Parameter operationParam : operation
                        .getParameters()) {
                    if (operationParam.getName().equals(ModelUtils.getParameterName(matchingMethodParam))) {
                        ParameterImpl.merge(annotation, operationParam, false, null);
                    }
                }
            }
        }
    }

    private void handleSchemaAnnotation(Field field, OpenAPI api) {
        Class<?> clazz = field.getDeclaringClass();
        if (clazz.isAnnotationPresent(Schema.class)) {
            Schema annotation = clazz.getDeclaredAnnotation(Schema.class);

            // Get the actual schema name
            String schemaName = annotation.name();
            if (schemaName == null || schemaName.isEmpty()) {
                schemaName = clazz.getSimpleName();
            }

            // Find and correct the name of the correct schema
            updateSchemaName(api.getComponents(), clazz.getSimpleName(), schemaName);
            org.eclipse.microprofile.openapi.models.media.Schema model = api.getComponents().getSchemas()
                    .get(schemaName);
            if (model == null) {
                model = new SchemaImpl();
                api.getComponents().addSchema(schemaName, model);
            }
            SchemaImpl.merge(annotation, model, true, api.getComponents().getSchemas());

            // Start parsing the field in the same manner
            if (field.isAnnotationPresent(Schema.class) && !Modifier.isTransient(field.getModifiers())) {
                annotation = field.getDeclaredAnnotation(Schema.class);

                org.eclipse.microprofile.openapi.models.media.Schema property = new SchemaImpl();
                SchemaImpl.merge(annotation, property, true, api.getComponents().getSchemas());
                if (property.getRef() == null) {
                    property.setType(ModelUtils.getSchemaType(field.getType()));
                }

                schemaName = annotation.name();
                if (schemaName == null || schemaName.isEmpty()) {
                    schemaName = field.getName();
                }

                model.addProperty(schemaName, property);
            }
        }
    }

    private void handleSchemaAnnotation(Parameter parameter,
            org.eclipse.microprofile.openapi.models.Operation operation,
            Map<String, org.eclipse.microprofile.openapi.models.media.Schema> currentSchemas) {
        if (parameter.isAnnotationPresent(Schema.class)) {
            Schema annotation = parameter.getDeclaredAnnotation(Schema.class);
            // Check if it's a request body
            if (ModelUtils.isRequestBody(parameter)) {
                // Insert the schema to every request body media type
                for (MediaType mediaType : operation.getRequestBody().getContent().values()) {
                    SchemaImpl.merge(annotation, mediaType.getSchema(), true, currentSchemas);
                    if (annotation.ref() != null && !annotation.ref().isEmpty()) {
                        mediaType.setSchema(new SchemaImpl().ref(annotation.ref()));
                    }
                }
            } else if (ModelUtils.getParameterType(parameter) != null) {
                for (org.eclipse.microprofile.openapi.models.parameters.Parameter param : operation.getParameters()) {
                    if (param.getName().equals(ModelUtils.getParameterName(parameter))) {
                        SchemaImpl.merge(annotation, param.getSchema(), true, currentSchemas);
                        if (annotation.ref() != null && !annotation.ref().isEmpty()) {
                            param.setSchema(new SchemaImpl().ref(annotation.ref()));
                        }
                    }
                }
            }
        }
    }

    private void updateSchemaName(Components components, String oldName, String newName) {
        if (oldName.equals(newName)) {
            return;
        }
        org.eclipse.microprofile.openapi.models.media.Schema schema = components.getSchemas().get(oldName);
        if (schema == null) {
            return;
        }
        components.getSchemas().remove(oldName);
        components.addSchema(newName, schema);
    }

    private void handleOpenAPIAnnotation(OpenAPI api, Class<?> clazz) {
        if (clazz.isAnnotationPresent(OpenAPIDefinition.class)) {
            OpenAPIDefinition annotation = clazz.getDeclaredAnnotation(OpenAPIDefinition.class);
            OpenAPIImpl.merge(annotation, api, true);
        }
    }

    private void handleOperationAnnotation(Method method, org.eclipse.microprofile.openapi.models.Operation operation,
            PathItem pathItem) {
        if (method.isAnnotationPresent(Operation.class)) {
            Operation annotation = method.getDeclaredAnnotation(Operation.class);
            OperationImpl.merge(annotation, operation, true);
            if (annotation.hidden()) {
                ModelUtils.removeOperation(pathItem, operation);
            }
        }
    }

    private void handleExternalDocumentationAnnotation(Method method,
            org.eclipse.microprofile.openapi.models.Operation operation) {
        ExternalDocumentation annotation = null;
        if (method.isAnnotationPresent(ExternalDocumentation.class)) {
            annotation = method.getDeclaredAnnotation(ExternalDocumentation.class);
        } else if (method.getDeclaringClass().isAnnotationPresent(ExternalDocumentation.class)) {
            annotation = method.getDeclaringClass().getDeclaredAnnotation(ExternalDocumentation.class);
        } else {
            return;
        }
        operation.setExternalDocs(new ExternalDocumentationImpl());
        ExternalDocumentationImpl.merge(annotation, operation.getExternalDocs(), true);
        if (operation.getExternalDocs().getUrl() == null) {
            operation.setExternalDocs(null);
        }
    }

    private void handleServersAnnotation(Method method, org.eclipse.microprofile.openapi.models.Operation operation) {
        List<Server> declaredServers = new ArrayList<>();
        if (method.isAnnotationPresent(Server.class) || method.isAnnotationPresent(Servers.class)) {
            if (method.isAnnotationPresent(Server.class)) {
                Server annotation = method.getDeclaredAnnotation(Server.class);
                declaredServers.add(annotation);
            }
            if (method.isAnnotationPresent(Servers.class)) {
                Servers annotation = method.getDeclaredAnnotation(Servers.class);
                for (Server server : annotation.value()) {
                    declaredServers.add(server);
                }
            }
        } else if (method.getDeclaringClass().isAnnotationPresent(Server.class)
                || method.getDeclaringClass().isAnnotationPresent(Servers.class)) {
            if (method.getDeclaringClass().isAnnotationPresent(Server.class)) {
                Server annotation = method.getDeclaringClass().getDeclaredAnnotation(Server.class);
                declaredServers.add(annotation);
            }
            if (method.getDeclaringClass().isAnnotationPresent(Servers.class)) {
                Servers annotation = method.getDeclaringClass().getDeclaredAnnotation(Servers.class);
                for (Server server : annotation.value()) {
                    declaredServers.add(server);
                }
            }
        }

        for (Server annotation : declaredServers) {
            org.eclipse.microprofile.openapi.models.servers.Server server = new ServerImpl();
            ServerImpl.merge(annotation, server, true);
            operation.addServer(server);
        }
    }

    private void handleCallbacksAnnotation(Method method, org.eclipse.microprofile.openapi.models.Operation operation) {
        List<Callback> declaredCallbacks = new ArrayList<>();
        if (method.isAnnotationPresent(Callback.class) || method.isAnnotationPresent(Callbacks.class)) {
            if (method.isAnnotationPresent(Callback.class)) {
                Callback annotation = method.getDeclaredAnnotation(Callback.class);
                declaredCallbacks.add(annotation);
            }
            if (method.isAnnotationPresent(Callbacks.class)) {
                Callbacks annotation = method.getDeclaredAnnotation(Callbacks.class);
                for (Callback callback : annotation.value()) {
                    declaredCallbacks.add(callback);
                }
            }
        } else if (method.getDeclaringClass().isAnnotationPresent(Callback.class)
                || method.getDeclaringClass().isAnnotationPresent(Callbacks.class)) {
            if (method.getDeclaringClass().isAnnotationPresent(Callback.class)) {
                Callback annotation = method.getDeclaringClass().getDeclaredAnnotation(Callback.class);
                declaredCallbacks.add(annotation);
            }
            if (method.getDeclaringClass().isAnnotationPresent(Callbacks.class)) {
                Callbacks annotation = method.getDeclaringClass().getDeclaredAnnotation(Callbacks.class);
                for (Callback callback : annotation.value()) {
                    declaredCallbacks.add(callback);
                }
            }
        }

        for (Callback annotation : declaredCallbacks) {
            String callbackName = annotation.name();
            if (callbackName != null && !callbackName.isEmpty()) {
                org.eclipse.microprofile.openapi.models.callbacks.Callback model = operation.getCallbacks()
                        .getOrDefault(callbackName, new CallbackImpl());
                CallbackImpl.merge(annotation, model, true);
                operation.getCallbacks().put(callbackName, model);
            }
        }
    }

    private void handleTagsAnnotation(Method method, org.eclipse.microprofile.openapi.models.Operation operation,
            OpenAPI api) {
        List<Tag> annotations = new ArrayList<>();
        if (method.isAnnotationPresent(Tag.class) || method.isAnnotationPresent(Tags.class)) {
            if (method.isAnnotationPresent(Tag.class)) {
                Tag annotation = method.getDeclaredAnnotation(Tag.class);
                annotations.add(annotation);
            }
            if (method.isAnnotationPresent(Tags.class)) {
                Tags annotation = method.getDeclaredAnnotation(Tags.class);
                for (Tag tag : annotation.value()) {
                    annotations.add(tag);
                }
                for (String ref : annotation.refs()) {
                    if (ref != null && !ref.isEmpty()) {
                        operation.addTag(ref);
                    }
                }
            }
        } else if (method.getDeclaringClass().isAnnotationPresent(Tag.class)
                || method.getDeclaringClass().isAnnotationPresent(Tags.class)) {
            if (method.getDeclaringClass().isAnnotationPresent(Tag.class)) {
                Tag annotation = method.getDeclaringClass().getDeclaredAnnotation(Tag.class);
                annotations.add(annotation);
            }
            if (method.getDeclaringClass().isAnnotationPresent(Tags.class)) {
                Tags annotation = method.getDeclaringClass().getDeclaredAnnotation(Tags.class);
                for (Tag tag : annotation.value()) {
                    annotations.add(tag);
                }
                for (String ref : annotation.refs()) {
                    if (ref != null && !ref.isEmpty()) {
                        operation.addTag(ref);
                    }
                }
            }
        }

        for (Tag annotation : annotations) {
            TagImpl.merge(annotation, operation, true, api.getTags());
        }
    }

    private void handleRequestBodyAnnotation(Parameter parameter,
            org.eclipse.microprofile.openapi.models.Operation operation,
            Map<String, org.eclipse.microprofile.openapi.models.media.Schema> currentSchemas) {
        if (parameter.isAnnotationPresent(RequestBody.class)) {
            RequestBody annotation = parameter.getDeclaredAnnotation(RequestBody.class);
            if (operation.getRequestBody() == null) {
                operation.setRequestBody(new RequestBodyImpl());
            }
            RequestBodyImpl.merge(annotation, operation.getRequestBody(), true, currentSchemas);
        } else if (parameter.getDeclaringExecutable().isAnnotationPresent(RequestBody.class)) {
            RequestBody annotation = parameter.getDeclaringExecutable().getDeclaredAnnotation(RequestBody.class);
            if (operation.getRequestBody() == null) {
                operation.setRequestBody(new RequestBodyImpl());
            }
            RequestBodyImpl.merge(annotation, operation.getRequestBody(), true, currentSchemas);
        }
    }

    private void handleAPIResponseAnnotation(Method method, org.eclipse.microprofile.openapi.models.Operation operation,
            Map<String, org.eclipse.microprofile.openapi.models.media.Schema> currentSchemas) {
        if (method.isAnnotationPresent(APIResponse.class)) {
            APIResponse annotation = method.getDeclaredAnnotation(APIResponse.class);

            // Get the response name
            String responseName = annotation.responseCode();
            if (responseName == null || responseName.isEmpty()) {
                responseName = APIResponses.DEFAULT;
            }

            org.eclipse.microprofile.openapi.models.responses.APIResponse response = operation.getResponses()
                    .getOrDefault(responseName, new APIResponseImpl());
            APIResponseImpl.merge(annotation, response, true, currentSchemas);

            operation.getResponses().addApiResponse(responseName, response);
        }
    }

    private void handleSecuritySchemeAnnotation(AnnotatedElement element, Components components) {
        if (element.isAnnotationPresent(SecurityScheme.class)) {
            SecurityScheme annotation = element.getDeclaredAnnotation(SecurityScheme.class);

            org.eclipse.microprofile.openapi.models.security.SecurityScheme model = new SecuritySchemeImpl();
            SecuritySchemeImpl.merge(annotation, model, true);

            if (annotation.securitySchemeName() != null && !annotation.securitySchemeName().isEmpty()) {
                components.addSecurityScheme(annotation.securitySchemeName(), model);
            }
        }
    }

    private void handleSecurityRequirementAnnotation(Method method,
            org.eclipse.microprofile.openapi.models.Operation operation) {
        List<SecurityRequirement> requirements = new ArrayList<>();

        // If the method is annotated
        if (method.isAnnotationPresent(SecurityRequirement.class)
                || method.isAnnotationPresent(SecurityRequirements.class)) {
            if (method.isAnnotationPresent(SecurityRequirement.class)) {
                SecurityRequirement annotation = method.getDeclaredAnnotation(SecurityRequirement.class);
                requirements.add(annotation);
            }
            if (method.isAnnotationPresent(SecurityRequirements.class)) {
                SecurityRequirements annotation = method.getDeclaredAnnotation(SecurityRequirements.class);
                for (SecurityRequirement requirement : annotation.value()) {
                    requirements.add(requirement);
                }
            }
            // Else if the class is annotated, inherit it
        } else if (method.getDeclaringClass().isAnnotationPresent(SecurityRequirement.class)
                || method.getDeclaringClass().isAnnotationPresent(SecurityRequirements.class)) {
            if (method.getDeclaringClass().isAnnotationPresent(SecurityRequirement.class)) {
                SecurityRequirement annotation = method.getDeclaringClass()
                        .getDeclaredAnnotation(SecurityRequirement.class);
                requirements.add(annotation);
            }
            if (method.getDeclaringClass().isAnnotationPresent(SecurityRequirements.class)) {
                SecurityRequirements annotation = method.getDeclaringClass()
                        .getDeclaredAnnotation(SecurityRequirements.class);
                for (SecurityRequirement requirement : annotation.value()) {
                    requirements.add(requirement);
                }
            }
        }

        // Process all the elements to be added
        for (SecurityRequirement annotation : requirements) {
            org.eclipse.microprofile.openapi.models.security.SecurityRequirement model = new SecurityRequirementImpl();
            SecurityRequirementImpl.merge(annotation, model, true);

            if (annotation.name() != null && !annotation.name().isEmpty()) {
                operation.addSecurityRequirement(model);
            }
        }
    }

    private void handleExtensionAnnotation(AnnotatedElement element, Extensible extensible) {
        if (element.isAnnotationPresent(Extension.class)) {
            Extension annotation = element.getDeclaredAnnotation(Extension.class);
            ExtensibleImpl.merge(annotation, extensible, true);
        }
    }

}
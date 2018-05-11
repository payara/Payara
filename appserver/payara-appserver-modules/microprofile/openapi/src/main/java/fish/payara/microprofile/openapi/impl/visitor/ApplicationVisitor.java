package fish.payara.microprofile.openapi.impl.visitor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.Paths;
import org.eclipse.microprofile.openapi.models.Reference;
import org.eclipse.microprofile.openapi.models.media.MediaType;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.media.Schema.SchemaType;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.eclipse.microprofile.openapi.models.parameters.Parameter.In;
import org.eclipse.microprofile.openapi.models.parameters.RequestBody;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;
import org.eclipse.microprofile.openapi.models.responses.APIResponses;

import fish.payara.microprofile.openapi.api.visitor.ApiContext;
import fish.payara.microprofile.openapi.api.visitor.ApiVisitor;
import fish.payara.microprofile.openapi.impl.model.PathItemImpl;
import fish.payara.microprofile.openapi.impl.model.PathsImpl;
import fish.payara.microprofile.openapi.impl.model.media.MediaTypeImpl;
import fish.payara.microprofile.openapi.impl.model.media.SchemaImpl;
import fish.payara.microprofile.openapi.impl.model.parameters.ParameterImpl;
import fish.payara.microprofile.openapi.impl.model.parameters.RequestBodyImpl;
import fish.payara.microprofile.openapi.impl.model.responses.APIResponseImpl;
import fish.payara.microprofile.openapi.impl.model.responses.APIResponsesImpl;
import fish.payara.microprofile.openapi.impl.model.util.ModelUtils;

public class ApplicationVisitor implements ApiVisitor {

    @Override
    public void visitClass(Class<?> clazz, ApiContext context) {
    }

    @Override
    public void visitMethod(Method method, ApiContext context) {
        // The application visitor doesn't care about non contextual objects
        if (context.getPath() == null) {
            return;
        }
        // Create the paths if they're not already created
        Paths paths = context.getApi().getPaths();
        if (paths == null) {
            paths = new PathsImpl();
            context.getApi().setPaths(paths);
        }

        // If the path hasn't been added to the model, do so now
        if (!paths.containsKey(context.getPath())) {
            paths.put(context.getPath(), new PathItemImpl());
        }
        PathItem pathItem = paths.get(context.getPath());

        // Set the HTTP method type
        Operation operation = ModelUtils.getOrCreateOperation(pathItem, ModelUtils.getHttpMethod(method));
        operation.setOperationId(method.getName());

        // Add the default response
        APIResponses responses = operation.getResponses();
        if (responses == null) {
            responses = new APIResponsesImpl();
            operation.setResponses(responses);
        }
        insertDefaultResponse(context.getApi(), responses, method);

        insertDefaultRequestBody(context.getApi(), operation, method);
    }

    @Override
    public void visitField(Field field, ApiContext context) {
        // Ignore fields
    }

    @Override
    public void visitParameter(java.lang.reflect.Parameter parameter, ApiContext context) {
        // The application visitor doesn't care about non contextual objects
        if (context.getPath() == null) {
            return;
        }
        if (ModelUtils.getParameterType(parameter) == null) {
            return;
        }
        // Create a jersey parameter modelling the method parameter

        Parameter newParam = new ParameterImpl();
        String sourceName = null;
        if (parameter.isAnnotationPresent(PathParam.class)) {
            newParam.setIn(In.PATH);
            newParam.setRequired(true);
            sourceName = parameter.getDeclaredAnnotation(PathParam.class).value();
        } else if (parameter.isAnnotationPresent(QueryParam.class)) {
            newParam.setIn(In.QUERY);
            newParam.setRequired(false);
            sourceName = parameter.getDeclaredAnnotation(QueryParam.class).value();
        } else if (parameter.isAnnotationPresent(HeaderParam.class)) {
            newParam.setIn(In.HEADER);
            newParam.setRequired(false);
            sourceName = parameter.getDeclaredAnnotation(HeaderParam.class).value();
        } else if (parameter.isAnnotationPresent(CookieParam.class)) {
            newParam.setIn(In.COOKIE);
            newParam.setRequired(false);
            sourceName = parameter.getDeclaredAnnotation(CookieParam.class).value();
        }
        newParam.setName(sourceName);
        newParam.setSchema(new SchemaImpl().type(ModelUtils.getSchemaType(parameter.getType())));
        Operation operation = ModelUtils.findOperation(context.getApi(), (Method) parameter.getDeclaringExecutable(),
                context.getPath());
        operation.addParameter(newParam);
    }

    protected RequestBody insertDefaultRequestBody(OpenAPI api, Operation operation, Method method) {
        RequestBody requestBody = new RequestBodyImpl();

        // Get the return type of the variable
        Class<?> returnType = null;
        // If form parameters are provided, this stores the general schema type for all
        // of them
        SchemaType formSchemaType = null;
        for (java.lang.reflect.Parameter methodParam : method.getParameters()) {
            if (ModelUtils.isRequestBody(methodParam)) {
                returnType = methodParam.getType();
                break;
            }
            if (methodParam.isAnnotationPresent(FormParam.class)) {
                returnType = methodParam.getType();
                formSchemaType = ModelUtils.getParentSchemaType(formSchemaType,
                        ModelUtils.getSchemaType(methodParam.getType()));
            }
        }
        if (returnType == null) {
            return null;
        }

        // If there is an @Consumes, create a media type for each
        if (method.isAnnotationPresent(Consumes.class)) {
            String[] value = method.getDeclaredAnnotation(Consumes.class).value();
            for (String produceType : value) {
                MediaType mediaType = new MediaTypeImpl().schema(createSchema(api, returnType));
                requestBody.getContent().addMediaType(getContentType(produceType), mediaType);
            }
        } else {
            // No @Consumes, create a wildcard
            MediaType mediaType = new MediaTypeImpl().schema(createSchema(api, returnType));
            requestBody.getContent().addMediaType(getContentType("*/*"), mediaType);
        }

        // If there are form parameters, reconfigure the types
        if (formSchemaType != null) {
            for (MediaType mediaType : requestBody.getContent().values()) {
                mediaType.getSchema().setType(formSchemaType);
            }
        }

        operation.setRequestBody(requestBody);
        return requestBody;
    }

    /**
     * Creates a new {@link APIResponse} to model the default response of a
     * {@link Method}, and inserts it into the {@link APIResponses}.
     * 
     * @param responses the {@link APIResponses} to add the default response to.
     * @param method    the {@link Method} to model the default response on.
     * @return the newly created {@link APIResponse}.
     */
    protected APIResponse insertDefaultResponse(OpenAPI api, APIResponses responses, Method method) {
        APIResponse defaultResponse = new APIResponseImpl();
        defaultResponse.setDescription("Default Response.");

        // Check if there are produce types specified
        if (method.isAnnotationPresent(Produces.class)) {
            // If there is an @Produces, get the value
            String[] value = method.getDeclaredAnnotation(Produces.class).value();
            for (String produceType : value) {
                // For each @Produces type, create a media type and add it to the response
                // content
                MediaType mediaType = new MediaTypeImpl().schema(createSchema(api, method.getReturnType()));
                defaultResponse.getContent().addMediaType(getContentType(produceType), mediaType);
            }
        } else {
            // No @Produces, so create a wildcard response
            MediaType mediaType = new MediaTypeImpl().schema(createSchema(api, method.getReturnType()));
            defaultResponse.getContent().addMediaType(getContentType("*/*"), mediaType);
        }

        // Add the default response
        responses.addApiResponse(APIResponses.DEFAULT, defaultResponse);
        return defaultResponse;
    }

    protected String getContentType(String name) {
        try {
            javax.ws.rs.core.MediaType mediaType = javax.ws.rs.core.MediaType.valueOf(name);
            if (mediaType != null) {
                return mediaType.toString();
            }
        } catch (IllegalArgumentException ex) {
        }
        return javax.ws.rs.core.MediaType.WILDCARD;
    }

    protected Schema createSchema(OpenAPI api, Class<?> type) {
        Schema schema = new SchemaImpl();
        SchemaType schemaType = ModelUtils.getSchemaType(type);
        schema.setType(schemaType);

        // Set the subtype if it's an array (for example an array of ints)
        if (schemaType == SchemaType.ARRAY) {
            Class<?> subType = type.getComponentType();
            Schema subSchema = schema;
            while (subType != null) {
                subSchema.setItems(new SchemaImpl().type(ModelUtils.getSchemaType(subType)));
                subSchema = schema.getItems();
                subType = subType.getComponentType();
            }
        }

        if (schemaType == SchemaType.OBJECT) {
            if (insertObjectReference(api, schema, type)) {
                schema.setType(null);
                schema.setItems(null);
            }
        }
        return schema;
    }

    /**
     * Replace the object in the referee with a reference, and create the reference
     * in the API.
     * 
     * @param api            the OpenAPI object.
     * @param referee        the object containing the reference.
     * @param referenceClass the class of the object being referenced.
     * @return if the reference has been created.
     */
    protected boolean insertObjectReference(OpenAPI api, Reference<?> referee, Class<?> referenceClass) {

        // If the object is java.lang.Object, exit
        if (referenceClass.equals(Object.class)) {
            return false;
        }

        // Get the schemas
        Map<String, Schema> schemas = api.getComponents().getSchemas();

        // Set the reference name
        referee.setRef(referenceClass.getSimpleName());

        if (!schemas.containsKey(referenceClass.getSimpleName())) {
            // If the schema type doesn't already exist, create it
            Schema schema = new SchemaImpl();
            schemas.put(referenceClass.getSimpleName(), schema);
            schema.setType(SchemaType.OBJECT);
            Map<String, Schema> fields = new LinkedHashMap<>();
            for (Field field : referenceClass.getDeclaredFields()) {
                if (!Modifier.isTransient(field.getModifiers())) {
                    fields.put(field.getName(), createSchema(api, field.getType()));
                }
            }
            schema.setProperties(fields);
        }

        return true;
    }
}
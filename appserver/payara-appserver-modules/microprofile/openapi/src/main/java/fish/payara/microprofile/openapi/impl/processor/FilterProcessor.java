package fish.payara.microprofile.openapi.impl.processor;

import java.lang.reflect.Field;
import java.util.Map;

import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.callbacks.Callback;
import org.eclipse.microprofile.openapi.models.headers.Header;
import org.eclipse.microprofile.openapi.models.links.Link;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.eclipse.microprofile.openapi.models.parameters.RequestBody;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;
import org.eclipse.microprofile.openapi.models.servers.Server;
import org.eclipse.microprofile.openapi.models.tags.Tag;

import fish.payara.microprofile.openapi.api.processor.OASProcessor;
import fish.payara.microprofile.openapi.impl.config.OpenApiConfiguration;
import fish.payara.microprofile.openapi.impl.model.OpenAPIImpl;

public class FilterProcessor implements OASProcessor {

    private OASFilter filter;

    @Override
    public void process(OpenAPI api, OpenApiConfiguration config) {
        try {
            if (config.getFilter() != null) {
                filter = config.getFilter().newInstance();
            }
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        if (filter != null) {
            filterObject(api);
        }
    }

    private Object filterObject(Object object) {
        if (object != null) {

            // If the object is a map
            if (object instanceof Map) {
                for (Object item : Map.class.cast(object).values()) {
                    filterObject(item);
                }
            }

            // If the object is iterable
            if (object instanceof Iterable) {
                for (Object item : Iterable.class.cast(object)) {
                    filterObject(item);
                }
            }

            // If the object is a model item
            if (object.getClass().getPackage().getName().startsWith(OpenAPIImpl.class.getPackage().getName())) {

                // Visit each field
                for (Field field : object.getClass().getDeclaredFields()) {
                    boolean accessible = field.isAccessible();
                    try {
                        field.setAccessible(true);
                        Object fieldValue = field.get(object);
                        filterObject(fieldValue);
                    } catch (IllegalArgumentException | IllegalAccessException e) {
                    } finally {
                        field.setAccessible(accessible);
                    }
                }

                // Visit the object
                visitObject(object);
            }

            return object;
        }
        return null;
    }

    private Object visitObject(Object object) {
        if (object != null) {
            if (PathItem.class.isAssignableFrom(object.getClass())) {
                return filter.filterPathItem((PathItem) object);
            }
            if (Operation.class.isAssignableFrom(object.getClass())) {
                return filter.filterOperation((Operation) object);
            }
            if (Parameter.class.isAssignableFrom(object.getClass())) {
                return filter.filterParameter((Parameter) object);
            }
            if (Header.class.isAssignableFrom(object.getClass())) {
                return filter.filterHeader((Header) object);
            }
            if (RequestBody.class.isAssignableFrom(object.getClass())) {
                return filter.filterRequestBody((RequestBody) object);
            }
            if (APIResponse.class.isAssignableFrom(object.getClass())) {
                return filter.filterAPIResponse((APIResponse) object);
            }
            if (Schema.class.isAssignableFrom(object.getClass())) {
                return filter.filterSchema((Schema) object);
            }
            if (SecurityScheme.class.isAssignableFrom(object.getClass())) {
                return filter.filterSecurityScheme((SecurityScheme) object);
            }
            if (Server.class.isAssignableFrom(object.getClass())) {
                return filter.filterServer((Server) object);
            }
            if (Tag.class.isAssignableFrom(object.getClass())) {
                return filter.filterTag((Tag) object);
            }
            if (Link.class.isAssignableFrom(object.getClass())) {
                return filter.filterLink((Link) object);
            }
            if (Callback.class.isAssignableFrom(object.getClass())) {
                return filter.filterCallback((Callback) object);
            }
            if (OpenAPI.class.isAssignableFrom(object.getClass())) {
                filter.filterOpenAPI((OpenAPI) object);
            }
        }
        return null;
    }

}
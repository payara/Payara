package fish.payara.microprofile.openapi.api.visitor;

import org.eclipse.microprofile.openapi.models.OpenAPI;

/**
 * The context in which a class object is being visited.
 * For example, if a method is being visited, the context
 * will contain the current state of the {@link OpenAPI},
 * and the current path in the API.
 */
public interface ApiContext {

    /**
     * The current {@link OpenAPI} object being operated on.
     */
    OpenAPI getApi();

    /**
     * The path of the object currently being visited.
     * If the path is null, the object has no context
     * (e.g a POJO).
     */
    String getPath();
}
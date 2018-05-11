package fish.payara.microprofile.openapi.impl.visitor;

import org.eclipse.microprofile.openapi.models.OpenAPI;

import fish.payara.microprofile.openapi.api.visitor.ApiContext;

public class OpenApiContext implements ApiContext {

    private final OpenAPI api;
    private final String path;

    public OpenApiContext(OpenAPI api, String path) {
        this.api = api;
        this.path = normaliseUrl(path);
    }

    @Override
    public OpenAPI getApi() {
        return api;
    }

    @Override
    public String getPath() {
        return path;
    }

    /**
     * Normalises a path string. A normalised path has:
     * <ul>
     * <li>no multiple slashes.</li>
     * <li>no trailing slash.</li>
     * </ul>
     * 
     * @param path the path to be normalised.
     */
    private String normaliseUrl(String path) {
        if (path == null) {
            return null;
        }
        // Remove multiple slashes
        path = path.replaceAll("/+", "/");

        // Remove trailing slash
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

}
package fish.payara.microprofile.openapi.impl.rest.app.provider;

import static fish.payara.microprofile.openapi.impl.rest.app.OpenApiApplication.APPLICATION_YAML;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.ext.Provider;

/**
 * A filter that attempts to change the <code>Accept</code> header if the
 * <code>format</code> query parameter is provided.
 */
@Provider
@PreMatching
public class QueryFormatFilter implements ContainerRequestFilter {

    /**
     * A map of recognised media types that can be specified in a
     * <code>format</code> query parameter.
     */
    private static final Map<String, String> mappings;

    static {
        Map<String, String> map = new HashMap<>();
        map.put("yaml", APPLICATION_YAML);
        map.put("json", APPLICATION_JSON);
        mappings = Collections.unmodifiableMap(map);
    }

    /**
     * Filters incoming requests to change the <code>Accept</code> header based on
     * the <code>format</code> query parameter.
     */
    @Override
    public void filter(ContainerRequestContext request) throws IOException {
        String format = request.getUriInfo().getQueryParameters().getFirst("format");
        if (format != null && mappings.containsKey(format)) {
            request.getHeaders().putSingle(ACCEPT, mappings.get(format));
        }
    }

}
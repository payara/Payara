package fish.payara.microprofile.openapi.impl.rest.app;

import static fish.payara.microprofile.openapi.impl.rest.app.OpenApiApplication.OPEN_API_APPLICATION_PATH;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

import javax.ws.rs.ApplicationPath;

import org.glassfish.jersey.server.ResourceConfig;

import fish.payara.microprofile.openapi.impl.rest.app.provider.QueryFormatFilter;
import fish.payara.microprofile.openapi.impl.rest.app.provider.writer.JsonWriter;
import fish.payara.microprofile.openapi.impl.rest.app.provider.writer.YamlWriter;
import fish.payara.microprofile.openapi.impl.rest.app.service.OpenApiResource;

@ApplicationPath(OPEN_API_APPLICATION_PATH)
public class OpenApiApplication extends ResourceConfig {

    public static final String OPEN_API_APPLICATION_PATH = "/openapi";
    public static final String APPLICATION_YAML = TEXT_PLAIN;

    public OpenApiApplication() {
        register(OpenApiResource.class);
        register(QueryFormatFilter.class);
        register(YamlWriter.class);
        register(JsonWriter.class);
        property("payara-internal", "true");
    }

}
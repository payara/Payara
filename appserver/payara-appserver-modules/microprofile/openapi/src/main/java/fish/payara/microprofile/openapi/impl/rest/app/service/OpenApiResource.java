package fish.payara.microprofile.openapi.impl.rest.app.service;

import static fish.payara.microprofile.openapi.impl.rest.app.OpenApiApplication.APPLICATION_YAML;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.eclipse.microprofile.openapi.models.OpenAPI;

import fish.payara.microprofile.openapi.impl.OpenApiService;

@Path("/")
public class OpenApiResource {

    @GET
    @Produces({ APPLICATION_YAML, APPLICATION_JSON })
    public OpenAPI getResponse() {
        return OpenApiService.getInstance().getDocument();
    }
}
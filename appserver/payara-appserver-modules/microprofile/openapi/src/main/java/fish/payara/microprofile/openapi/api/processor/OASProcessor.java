package fish.payara.microprofile.openapi.api.processor;

import org.eclipse.microprofile.openapi.models.OpenAPI;

import fish.payara.microprofile.openapi.impl.config.OpenApiConfiguration;

public interface OASProcessor {

    void process(OpenAPI api, OpenApiConfiguration config);

}
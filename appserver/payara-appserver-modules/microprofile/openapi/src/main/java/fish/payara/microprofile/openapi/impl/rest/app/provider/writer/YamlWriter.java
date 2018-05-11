package fish.payara.microprofile.openapi.impl.rest.app.provider.writer;

import static fish.payara.microprofile.openapi.impl.rest.app.OpenApiApplication.APPLICATION_YAML;

import javax.ws.rs.Produces;
import javax.ws.rs.ext.Provider;

import fish.payara.microprofile.openapi.impl.rest.app.provider.ObjectMapperFactory;

/**
 * Writes the YAML response to the stream.
 */
@Provider
@Produces(APPLICATION_YAML)
public class YamlWriter extends AbstractWriter {

    public YamlWriter() {
        super(ObjectMapperFactory.createYaml());
    }

}
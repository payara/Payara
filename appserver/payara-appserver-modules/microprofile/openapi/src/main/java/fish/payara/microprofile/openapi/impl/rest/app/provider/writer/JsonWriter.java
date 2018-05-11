package fish.payara.microprofile.openapi.impl.rest.app.provider.writer;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import javax.ws.rs.Produces;
import javax.ws.rs.ext.Provider;

import fish.payara.microprofile.openapi.impl.rest.app.provider.ObjectMapperFactory;

/**
 * Writes the JSON response to the stream.
 */
@Provider
@Produces(APPLICATION_JSON)
public class JsonWriter extends AbstractWriter {

    public JsonWriter() {
        super(ObjectMapperFactory.createJson());
    }

}
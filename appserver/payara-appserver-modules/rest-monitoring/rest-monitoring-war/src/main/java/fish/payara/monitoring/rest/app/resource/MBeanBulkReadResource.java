package fish.payara.monitoring.rest.app.resource;

import java.io.StringReader;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import fish.payara.monitoring.rest.app.MBeanServerDelegate;
import fish.payara.monitoring.rest.app.handler.MBeanAttributeReadHandler;
import fish.payara.monitoring.rest.app.handler.MBeanAttributesReadHandler;
import fish.payara.monitoring.rest.app.handler.MBeanReadHandler;
import fish.payara.monitoring.rest.app.handler.ReadHandler;

/**
 * @author Krassimir Valev
 */
@Path("")
@RequestScoped
public class MBeanBulkReadResource {

    @Inject
    private MBeanServerDelegate mDelegate;

    /**
     * Returns the {@link String} form of the {@link JSONObject} resource from the ResourceHandler.
     *
     * @param content
     *            The JSON request payload, describing the beans and attributes to read.
     * @return The {@link String} representation of the MBeanRead/MBeanAttributeRead {@link JSONObject}.
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public String getReadResource(final String content) {
        try (JsonReader reader = Json.createReader(new StringReader(content))) {
            // the payload can be either a single request or a bulk one (array)
            JsonStructure struct = reader.read();
            switch (struct.getValueType()) {
                case ARRAY:
                    List<JsonObject> objects = struct.asJsonArray().stream()
                            .map(value -> handleRequest(value.asJsonObject()))
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .collect(Collectors.toList());

                    JsonArrayBuilder builder = Json.createArrayBuilder();
                    for (JsonObject jsonObject : objects) {
                        builder.add(jsonObject);
                    }

                    return builder.build().toString();
                case OBJECT:
                    return handleRequest(struct.asJsonObject()).orElse(JsonValue.EMPTY_JSON_OBJECT).toString();
                default:
                    return "invalid JSON structure";
            }
        }
    }

    private Optional<JsonObject> handleRequest(final JsonObject jsonObject) {
        // ignore non-read requests
        String type = jsonObject.getString("type", "");
        if (!"read".equalsIgnoreCase(type)) {
            return Optional.empty();
        }

        String mbean = jsonObject.getString("mbean", "");
        JsonValue attributes = jsonObject.getOrDefault("attribute", JsonValue.NULL);
        ReadHandler handler = getReadHandler(mbean, attributes);
        return Optional.of(handler.getResource());
    }

    private ReadHandler getReadHandler(final String mbean, final JsonValue attributes) {
        // attributes can be null, a string or a list of strings
        switch (attributes.getValueType()) {
            case ARRAY:
                String[] attributeNames = attributes.asJsonArray().stream()
                        .map(v -> ((JsonString) v).getString())
                        .toArray(String[]::new);
                return new MBeanAttributesReadHandler(mDelegate, mbean, attributeNames);
            case STRING:
                String attribute = ((JsonString) attributes).getString();
                return new MBeanAttributeReadHandler(mDelegate, mbean, attribute);
            default:
                return new MBeanReadHandler(mDelegate, mbean);
        }
    }

}

package fish.payara.ejb.http.protocol.rs;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.Consumes;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import fish.payara.ejb.http.protocol.InvokeMethodRequest;

@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class JsonbInvokeMethodMessageBodyReader implements MessageBodyReader<InvokeMethodRequest> {

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return InvokeMethodRequest.class.isAssignableFrom(type);
    }

    @Override
    public InvokeMethodRequest readFrom(Class<InvokeMethodRequest> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
                    throws IOException, WebApplicationException {
        try (JsonReader jsonReader = Json.createReader(entityStream)) {
            JsonObject json = jsonReader.readObject();
            String[] argTypeNames = json.getJsonArray("argTypes").stream().map(JsonbInvokeMethodMessageBodyReader::text)
                    .toArray(String[]::new);
            JsonArray actualTypes = json.getJsonArray("argActualTypes");
            String[] argActualTypeNames = actualTypes == null ? argTypeNames
                    : actualTypes.stream().map(JsonbInvokeMethodMessageBodyReader::text).toArray(String[]::new);
            return new InvokeMethodRequest(
                    json.getString("java.naming.security.principal", ""),
                    json.getString("java.naming.security.credentials", ""),
                    json.getString("lookup"),
                    json.getString("method"),
                    argTypeNames,
                    argActualTypeNames,
                    json.getJsonArray("argValues"),
                    (args, argTypes, classloader) -> toObjects(argTypes, (JsonArray) args));
        }
    }

    private static String text(JsonValue value) {
        return value instanceof JsonString ? ((JsonString) value).getString() : value.toString();
    }

    /**
     * Convert JSON encoded method parameter values to their object instances 
     */
    private static Object[] toObjects(Type[] argTypes, JsonArray jsonArgValues) {
        Object[] argValues = new Object[argTypes.length];
        for (int i = 0; i < jsonArgValues.size(); i++) {
            argValues[i] =  toObject(jsonArgValues.get(i), argTypes[i]);
        }
        return argValues;
    }

    private static Object toObject(JsonValue objectValue, Type type) {
        try (Jsonb jsonb = JsonbBuilder.create()) {
            return jsonb.fromJson(objectValue.toString(), type);
        } catch (Exception ex) {
            // cannot really happen. It is just from java.lang.AutoCloseable interface
            throw new InternalServerErrorException("Failed to deserialize argument of type: " + type.toString(), ex);
        }
    }

}

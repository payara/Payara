package fish.payara.ejb.http.protocol.rs;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import fish.payara.ejb.http.protocol.LookupRequest;

@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class JsonbLookupMessageBodyReader implements MessageBodyReader<LookupRequest> {

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return LookupRequest.class.isAssignableFrom(type);
    }

    @Override
    public LookupRequest readFrom(Class<LookupRequest> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws IOException, WebApplicationException {
        try (JsonReader jsonReader = Json.createReader(entityStream)) {
            JsonObject request = jsonReader.readObject();
            return new LookupRequest(request.getString("lookup"));
        }
    }

}

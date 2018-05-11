package fish.payara.microprofile.openapi.impl.rest.app.provider.writer;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.eclipse.microprofile.openapi.models.OpenAPI;

public abstract class AbstractWriter implements MessageBodyWriter<OpenAPI> {

    protected final ObjectMapper mapper;

    public AbstractWriter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type != null && OpenAPI.class.isAssignableFrom(type);
    }

    @Override
    public void writeTo(OpenAPI t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
            throws IOException, WebApplicationException {
        entityStream.write(mapper.writeValueAsBytes(t));
    }

}
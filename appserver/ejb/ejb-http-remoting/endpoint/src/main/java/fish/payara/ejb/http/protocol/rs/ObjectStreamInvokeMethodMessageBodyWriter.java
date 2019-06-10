package fish.payara.ejb.http.protocol.rs;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import fish.payara.ejb.http.protocol.InvokeMethodResponse;

@Provider
@Produces("application/x-java-object")
public class ObjectStreamInvokeMethodMessageBodyWriter implements MessageBodyWriter<InvokeMethodResponse> {

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return InvokeMethodResponse.class.isAssignableFrom(type);
    }

    @Override
    public void writeTo(InvokeMethodResponse response, Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
            throws IOException, WebApplicationException {
        new ObjectOutputStream(entityStream).writeObject(response);
    }

}

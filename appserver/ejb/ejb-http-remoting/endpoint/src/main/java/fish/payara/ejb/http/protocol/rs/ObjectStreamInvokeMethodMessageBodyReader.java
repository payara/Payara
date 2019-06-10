package fish.payara.ejb.http.protocol.rs;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import org.apache.commons.io.input.ClassLoaderObjectInputStream;

import fish.payara.ejb.http.protocol.InvokeMethodRequest;

@Provider
@Consumes("application/x-java-object")
public class ObjectStreamInvokeMethodMessageBodyReader implements MessageBodyReader<InvokeMethodRequest> {

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return InvokeMethodRequest.class.isAssignableFrom(type);
    }

    @Override
    public InvokeMethodRequest readFrom(Class<InvokeMethodRequest> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws IOException, WebApplicationException {
        try {
            InvokeMethodRequest request = (InvokeMethodRequest) new ObjectInputStream(entityStream).readObject();
            request.argDeserializer = (args, types, classloader) -> {
                try (ObjectInputStream ois = new ClassLoaderObjectInputStream(classloader, new ByteArrayInputStream((byte[])args))) {
                    return (Object[]) ois.readObject();
                } catch (Exception ex) {
                    throw new IllegalArgumentException(ex);
                }
            };
            return request;
        } catch (ClassNotFoundException ex) {
           throw new IllegalStateException(ex);
        }
    }

}

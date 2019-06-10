package fish.payara.ejb.http.protocol.rs;

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

import fish.payara.ejb.http.protocol.LookupRequest;

@Provider
@Consumes("application/x-java-object")
public class ObjectStreamLookupMessageBodyReader implements MessageBodyReader<LookupRequest> {

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return LookupRequest.class.isAssignableFrom(type);
    }

    @Override
    public LookupRequest readFrom(Class<LookupRequest> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws IOException, WebApplicationException {
        try {
            return (LookupRequest) new ObjectInputStream(entityStream).readObject();
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

}

package fish.payara.ejb.http.protocol.rs;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Consumes;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

@Provider
@Consumes("application/x-java-object")
public class ObjectStreamMessageBodyReader implements MessageBodyReader<Serializable> {

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return mediaType.getType().equals("application") && mediaType.getSubtype().equals("x-java-object");
    }

    @Override
    public Serializable readFrom(Class<Serializable> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws IOException, WebApplicationException {
        try {
            return (Serializable) new ObjectInputStream(entityStream).readObject();
        } catch (ClassNotFoundException ex) {
            throw new InternalServerErrorException("Class not found while de-serialising object stream as "
                    + type.getSimpleName() + " : " + ex.getMessage(), ex);
        }
    }

}

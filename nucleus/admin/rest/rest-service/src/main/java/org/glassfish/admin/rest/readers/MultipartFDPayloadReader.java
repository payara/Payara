/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.glassfish.admin.rest.readers;

import com.sun.enterprise.admin.remote.ParamsWithPayload;
import com.sun.enterprise.admin.remote.reader.MultipartProprietaryReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

/**
 *
 * @author martinmares
 */
@Provider
@Consumes(MediaType.MULTIPART_FORM_DATA)
public class MultipartFDPayloadReader implements MessageBodyReader<ParamsWithPayload> {
    
    private static MultipartProprietaryReader delegate = new MultipartProprietaryReader();

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return delegate.isReadable(type, mediaType.toString());
    }

    @Override
    public ParamsWithPayload readFrom(Class<ParamsWithPayload> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        return delegate.readFrom(entityStream, mediaType.toString());
    }
    
}

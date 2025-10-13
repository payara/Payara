/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 *
 * Portions Copyright [2017-2021] Payara Foundation and/or affiliates
 */
package org.glassfish.admin.rest.provider;

import fish.payara.admin.rest.streams.StreamWriter;
import fish.payara.admin.rest.streams.JsonStreamWriter;
import fish.payara.admin.rest.streams.XmlStreamWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.logging.Level;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import javax.xml.stream.XMLStreamException;
import org.glassfish.admin.rest.RestLogging;

/** Abstract implementation for entity writers to STaX API. This supports 
 * XML and JSON.
 *
 * @since 4.0
 * @author mmares
 */
public abstract class AbstractStaxProvider<T> extends BaseProvider<T> {
    
    public AbstractStaxProvider(Class desiredType, MediaType ... mediaType) {
        super(desiredType, mediaType);
    }
    
    @Override
    protected boolean isGivenTypeWritable(Class<?> type, Type genericType) {
        return desiredType.isAssignableFrom(type);
    }
    
    /** Marshalling implementation here.
     * 
     * @param proxy object to marshal
     * @param wr STaX for marshaling
     * @throws XMLStreamException 
     * @throws java.io.IOException 
     */ 
    protected abstract void writeContentToStream(T proxy, final StreamWriter wr) throws Exception;

    @Override
    public String getContent(T proxy) {
        throw new UnsupportedOperationException("Provides only streaming implementation");
    }
    
    /** Faster with direct stream writing
     */
    @Override
    public void writeTo(T proxy, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        StreamWriter writer = null;
        try {
            if ("xml".equals(mediaType.getSubtype())) {
                writer = new XmlStreamWriter(entityStream);
            } else {
                writer = new JsonStreamWriter(entityStream);
            }
            writeContentToStream(proxy, writer);
            writer.close();
        } catch (Exception ex) {
            RestLogging.restLogger.log(Level.SEVERE, RestLogging.CANNOT_MARSHAL, ex);
            throw new WebApplicationException(ex, Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

}

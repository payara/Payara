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
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
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
 */
package org.glassfish.admin.rest.provider;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.logging.Level;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.codehaus.jettison.mapped.MappedNamespaceConvention;
import org.codehaus.jettison.mapped.MappedXMLStreamWriter;
import org.glassfish.admin.rest.Constants;
import org.glassfish.admin.rest.RestLogging;
import org.jvnet.hk2.config.IndentingXMLStreamWriter;

/** Abstract implementation for entity writers to STaX API. This supports 
 * XML and JSON.
 *
 * @author mmares
 */
public abstract class AbstractStaxProvider<T> extends BaseProvider<T> {
    
    private static final XMLOutputFactory XML_FACTORY = XMLOutputFactory.newInstance();
    private static final MappedNamespaceConvention JSON_CONVENTION = new MappedNamespaceConvention();
    private static final MediaType ANY_XML_MEDIATYPE = new MediaType(MediaType.MEDIA_TYPE_WILDCARD, "xml");
    
    protected class PrePostFixedWriter {
        private String prefix;
        private String postfix;
        private XMLStreamWriter writer;

        public PrePostFixedWriter(XMLStreamWriter writer, String prefix, String postfix) {
            this.prefix = prefix;
            this.postfix = postfix;
            this.writer = writer;
        }

        public PrePostFixedWriter(XMLStreamWriter writer) {
            this(writer, null, null);
        }

        /** Must be written after marshaled entity
         */
        public String getPostfix() {
            return postfix;
        }

        /** Must be written before marshaled entity
         */
        public String getPrefix() {
            return prefix;
        }

        public XMLStreamWriter getWriter() {
            return writer;
        }
        
    }
    
    public AbstractStaxProvider(Class desiredType, MediaType ... mediaType) {
        super(desiredType, mediaType);
    }
    
    @Override
    protected boolean isGivenTypeWritable(Class<?> type, Type genericType) {
        return desiredType.isAssignableFrom(type);
    }
    
    protected static XMLStreamWriter getXmlWriter(final OutputStream os, boolean indent) throws XMLStreamException {
        XMLStreamWriter wr = XML_FACTORY.createXMLStreamWriter(os, Constants.ENCODING);
        if (indent) {
            wr = new IndentingXMLStreamWriter(wr);
        }
        return wr;
    }

    protected static XMLStreamWriter getJsonWriter(final OutputStream os, boolean indent) 
            throws UnsupportedEncodingException {
        return new MappedXMLStreamWriter(JSON_CONVENTION, new OutputStreamWriter(os, Constants.ENCODING));
    }
    
    /** Returns XML StAX API for any media types with "xml" subtype. Otherwise returns JSON StAX API
     */
    protected PrePostFixedWriter getWriter(final MediaType mediaType, final OutputStream os, boolean indent) 
            throws IOException {
        if (mediaType != null && "xml".equals(mediaType.getSubtype())) {
            try {
                return new PrePostFixedWriter(getXmlWriter(os, indent));
            } catch (XMLStreamException ex) {
                throw new IOException(ex);
            }
        } else {
            String callBackJSONP = getCallBackJSONP();
            if (callBackJSONP != null) {
                return new PrePostFixedWriter(getJsonWriter(os, indent),
                        callBackJSONP + "(",
                        ")");
            } else {
                return new PrePostFixedWriter(getJsonWriter(os, indent));
            }
        }
    }
    
    /** Marshalling implementation here.
     * 
     * @param proxy object to marshal
     * @param wr STaX for marshaling
     * @throws XMLStreamException 
     */ 
    protected abstract void writeContentToStream(T proxy, final XMLStreamWriter wr) throws XMLStreamException;

    @Override
    public String getContent(T proxy) {
        throw new UnsupportedOperationException("Provides only streaming implementation");
    }
    
    /** Faster with direct stream writing
     */
    @Override
    public void writeTo(T proxy, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        try {
            PrePostFixedWriter writer = getWriter(mediaType, entityStream, super.getFormattingIndentLevel() > -1);
            //Write it
            if (writer.getPrefix() != null) {
                entityStream.write(writer.getPrefix().getBytes(Constants.ENCODING));
            }
            writeContentToStream(proxy, writer.getWriter());
            if (writer.getPostfix() != null) {
                entityStream.write(writer.getPostfix().getBytes(Constants.ENCODING));
            }
        } catch (XMLStreamException uee) {
            RestLogging.restLogger.log(Level.SEVERE,RestLogging.CANNOT_MARSHAL, uee);
            throw new WebApplicationException(uee, Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
    
}

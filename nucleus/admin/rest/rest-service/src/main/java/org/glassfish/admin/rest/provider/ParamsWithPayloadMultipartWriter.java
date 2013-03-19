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

import com.sun.enterprise.admin.remote.ParamsWithPayload;
import com.sun.enterprise.admin.remote.writer.MultipartProprietaryWriter;
import com.sun.enterprise.v3.common.ActionReporter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import org.glassfish.api.ActionReport;

/**
 *
 * @author martinmares
 */
@Provider
@Produces("multipart/mixed")
public class ParamsWithPayloadMultipartWriter extends MultipartProprietaryWriter implements MessageBodyWriter<ParamsWithPayload> {
    
    private static final MediaType MULTIPART_MIXED = new MediaType("multipart", "mixed");
    
    private static final ActionReportJson2Provider arWriter = new ActionReportJson2Provider();
    
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return ParamsWithPayload.class.isAssignableFrom(type) && mediaType.isCompatible(MULTIPART_MIXED);
    }

    @Override
    public void writeTo(ParamsWithPayload proxy, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            final MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        Object value = httpHeaders.getFirst("MIME-Version");
        if (value == null) {
            httpHeaders.putSingle("MIME-Version", "1.0");
        }
        super.writeTo(proxy.getPayloadOutbound(), proxy.getParameters(), proxy.getActionReport(), entityStream, new MultipartProprietaryWriter.ContentTypeWriter() {
                        @Override
                        public void writeContentType(String firstPart, String secondPart, String boundary) {
                            StringBuilder ct = new StringBuilder();
                            ct.append(firstPart).append('/').append(secondPart);
                            if (boundary != null) {
                                ct.append("; boundary=").append(boundary);
                            }
                            httpHeaders.putSingle(HttpHeaders.CONTENT_TYPE, ct.toString());
                        }
                    });
    }
    
    @Override
    protected void writeActionReport(final Writer writer,
                                        final OutputStream underOS,
                                        final String boundary, 
                                        final ActionReport ar) throws IOException {
        //        //Inrtroducing boundery
//        if (isFirst) {
//            isFirst = false;
//        } else {
//            writer.write(EOL);
//        }
        multiWrite(writer, BOUNDERY_DELIMIT, boundary, EOL);
        //Headers
        multiWrite(writer, "Content-Disposition: file; name=\"ActionReport\"", EOL);
        multiWrite(writer, "Content-Type: ", MediaType.APPLICATION_JSON, EOL);
        //Data
        //Data
        writer.write(EOL);
        writer.flush();
        arWriter.writeTo((ActionReporter) ar, ar.getClass(), null, null, MediaType.APPLICATION_JSON_TYPE, null, underOS);
        underOS.flush();
        writer.write(EOL);
    }

    @Override
    public long getSize(ParamsWithPayload t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}

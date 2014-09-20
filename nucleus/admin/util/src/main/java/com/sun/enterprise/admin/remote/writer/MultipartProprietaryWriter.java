/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.admin.remote.writer;

import com.sun.enterprise.admin.remote.ParamsWithPayload;
import com.sun.enterprise.util.StringUtils;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.api.admin.Payload;
import org.glassfish.api.admin.Payload.Part;

/**
 *
 * @author martinmares
 */
public class MultipartProprietaryWriter implements ProprietaryWriter {
    
    public static interface ContentTypeWriter {
        void writeContentType(String firstPart, String secondPart, String boundary);
    }
    
    protected static final String BOUNDARY_BASE = "admfrmwrk_payload_";
    protected static final AtomicInteger boundary_int = new AtomicInteger();
    protected static final String EOL = "\r\n";
    protected static final String BOUNDERY_DELIMIT = "--";
    
    @Override
    public boolean isWriteable(final Object entity) {
        if (entity instanceof ParamsWithPayload) {
            ParamsWithPayload pwp = (ParamsWithPayload) entity;
            return pwp.getPayloadOutbound() != null && pwp.getPayloadOutbound().size() > 0;
        } else if (entity instanceof Payload.Outbound) {
            return ((Payload.Outbound) entity).size() > 0;
        }
        return false;
    }
    
    @Override
    public void writeTo(final Object entity,
                        final HttpURLConnection urlConnection) throws IOException {
        Payload.Outbound payload = null;
        ParameterMap parameters = null;
        ActionReport ar = null;
        if (entity instanceof ParamsWithPayload) {
            ParamsWithPayload pwp = (ParamsWithPayload) entity;
            payload = pwp.getPayloadOutbound();
            parameters = pwp.getParameters();
            ar = pwp.getActionReport();
        } else if (entity instanceof Payload.Outbound) {
            payload = (Payload.Outbound) entity;
        }
        writeTo(payload, parameters, ar,
                new OutputStream() {
                    private OutputStream delegate;
                    private OutputStream getDelegate() throws IOException {
                        if (delegate == null) {
                            delegate = urlConnection.getOutputStream();
                        }
                        return delegate;
                    }
                    @Override
                    public void write(int b) throws IOException {
                        getDelegate().write(b);
                    }
                    @Override
                    public void write(byte b[]) throws IOException {
                        getDelegate().write(b);
                    }
                    @Override
                    public void write(byte b[], int off, int len) throws IOException {
                        getDelegate().write(b, off, len);
                    }
                    @Override
                    public void flush() throws IOException {
                        getDelegate().flush();
                    }
                    @Override
                    public void close() throws IOException {
                        getDelegate().close();
                    }
                }, 
                new ContentTypeWriter() {
                        @Override
                        public void writeContentType(String firstPart, String secondPart, String boundary) {
                            StringBuilder ct = new StringBuilder();
                            ct.append(firstPart).append('/').append(secondPart);
                            if (boundary != null) {
                                ct.append("; boundary=").append(boundary);
                            }
                            urlConnection.addRequestProperty("Content-type", ct.toString());
                            urlConnection.setRequestProperty("MIME-Version", "1.0");
                        }
                    });
    }
    
    protected void writeParam(final Writer writer, 
                                 final OutputStream underOS,
                                 final String boundary, 
                                 final String key, 
                                 final String value) throws IOException {
//        //Inrtroducing boundery
//        if (isFirst) {
//            isFirst = false;
//        } else {
//            writer.write(EOL);
//        }
        multiWrite(writer, BOUNDERY_DELIMIT, boundary, EOL);
        //Headers
        //Headers - Disposition
        multiWrite(writer, "Content-Disposition: form-data; name=\"", key, "\"", EOL);
        //Data
        multiWrite(writer, EOL, value, EOL);
        writer.flush();
    }
    
    protected void writePayloadPart(final Writer writer,
                                        final OutputStream underOS,
                                        final String boundary, 
                                        final Part part) throws IOException {
//        //Inrtroducing boundery
//        if (isFirst) {
//            isFirst = false;
//        } else {
//            writer.write(EOL);
//        }
        multiWrite(writer, BOUNDERY_DELIMIT, boundary, EOL);
        //Headers
        multiWrite(writer, "Content-Disposition: file; name=\"", part.getName(), "\"; filename=\"", part.getName(), "\"", EOL);
        if (StringUtils.ok(part.getContentType())) {
            multiWrite(writer, "Content-Type: ", part.getContentType(), EOL);
        }
        for (Map.Entry<Object, Object> entry : part.getProperties().entrySet()) {
            String key = (String) entry.getKey();
            String lKey = key.toLowerCase(Locale.ENGLISH);
            //todo: JDK7: convert to String switch-case
            if (!"content-disposition".equals(lKey) && !"content-type".equals(lKey)) {
                if (!"content-transfer-encoding".equals(lKey)) {
                    key = "Content-" + key;
                }
                multiWrite(writer, key, ": ", String.valueOf(entry.getValue()), EOL);
            }
        }
        //Data
        writer.write(EOL);
        writer.flush();
        part.copy(underOS);
        underOS.flush();
        writer.write(EOL);
    }
    
    protected void writeActionReport(final Writer writer,
                                        final OutputStream underOS,
                                        final String boundary, 
                                        final ActionReport ar) throws IOException {
        throw new UnsupportedOperationException();
    }
    
    public void writeTo(final Payload.Outbound payload,
                        final ParameterMap parameters,
                        final ActionReport ar,
                        final OutputStream os,
                        final ContentTypeWriter contentTypeWriter) throws IOException {
        final String boundary = getBoundary();
        //Content-Type
        String ctType = "form-data";
        if (parameters == null || parameters.size() == 0) {
            ctType = "mixed";
        }
        contentTypeWriter.writeContentType("multipart", ctType, boundary);
        // Write content
        final Writer writer = new BufferedWriter(new OutputStreamWriter(os));
//        boolean isFirst = true;
        //Parameters
        if (parameters != null) {
            for (Map.Entry<String, List<String>> entry : parameters.entrySet()) {
                for (String value : entry.getValue()) {
                    writeParam(writer, os, boundary, entry.getKey(), value);
                }
            }
        }
        //ActionReport
        if (ar != null) {
            writeActionReport(writer, os, boundary, ar);
        }
        //Payload
        if (payload != null) {
            Iterator<Part> parts = payload.parts();
            while (parts.hasNext()) {
                writePayloadPart(writer, os, boundary, parts.next());
            }
        }
        // Write the final boundary string
        multiWrite(writer, BOUNDERY_DELIMIT, boundary, BOUNDERY_DELIMIT, EOL);
        writer.flush();
    }
    
    private String getBoundary() {
        return BOUNDARY_BASE + boundary_int.incrementAndGet();
    }
    
    protected static void multiWrite(Writer writer, String... args) throws IOException {
        for (String arg : args) {
            writer.write(arg);
        }
    }
    
}

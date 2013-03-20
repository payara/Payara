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

package com.sun.enterprise.admin.remote;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import org.glassfish.admin.payload.PayloadImpl;
import org.glassfish.api.admin.Payload;
import org.jvnet.mimepull.Header;


/** Payload implementation for ReST interface.
 *
 * @author mmares
 */
public class RestPayloadImpl extends PayloadImpl {

    public static class Outbound extends PayloadImpl.Outbound {

        private String complexType;

        public Outbound(boolean client2Server) {
            if (client2Server) {
                complexType = "multipart/form-data";
            } else {
                complexType = "multipart/mixed";
            }
        }

        @Override
        public String getComplexContentType() {
            return complexType;
        }

        @Override
        protected void writePartsTo(OutputStream os) throws IOException {
            throw new UnsupportedOperationException("Not supported for RestPauloadImpl.");
        }

        @Override
        public void writeTo(final OutputStream os) throws IOException {
            throw new UnsupportedOperationException("Not supported for RestPauloadImpl.");
        }

//        public MultiPart addToMultipart(MultiPart mp, Logger logger) {
//            if (mp == null) {
//                if (client2Server) {
//                    logger.finest("addToMultipart: Creating FormDataMultiPart for result");
//                    mp = new FormDataMultiPart();
//                } else {
//                    logger.finest("addToMultipart: Creating MultiPart [mixed] for result");
//                    mp = new MultiPart();
//                }
//            }
//            ArrayList<Payload.Part> parts = getParts();
//            if (logger.isLoggable(Level.FINEST)) {
//                logger.log(Level.FINEST, "addToMultipart: parts.size = {0}", parts.size());
//            }
//            int index = 0;
//            for (Payload.Part part : parts) {
//                index++;
//                String contentType = part.getContentType();
//                MediaType mt = new MediaType();
//                if (contentType != null && !contentType.isEmpty()) {
//                    int ind = contentType.indexOf('/');
//                    if (ind > -1) {
//                        mt = new MediaType(contentType.substring(0, ind), contentType.substring(ind + 1));
//                    } else {
//                        mt = new MediaType(contentType, MediaType.WILDCARD);
//                    }
//                }
//                BodyPart bp;
//                if (logger.isLoggable(Level.FINEST)) {
//                    logger.log(Level.FINEST, "addToMultipart[{0}]: name: {1}, type: {2}", new Object[]{index, part.getName(), mt});
//                }
//                if (client2Server) {
//                    bp = new FormDataBodyPart(part.getName(), part, mt);
//                } else {
//                    bp = new BodyPart(part, mt);
//                    ContentDisposition cd = ContentDisposition.type("file").fileName(part.getName()).build();
//                    if (logger.isLoggable(Level.FINEST)) {
//                        logger.log(Level.FINEST, "addToMultipart[{0}]: Content Disposition: {1}", new Object[]{index, cd});
//                    }
//                    bp.setContentDisposition(cd);
//                }
//                Properties props = part.getProperties();
//                for (Map.Entry<Object, Object> entry : props.entrySet()) {
//                    if (logger.isLoggable(Level.FINEST)) {
//                        logger.log(Level.FINEST, "addToMultipart[{0}]: Header: {1}: {2}",
//                                new Object[]{index, addContentPrefix((String) entry.getKey()), entry.getValue()});
//                    }
//                    bp.getHeaders().add(addContentPrefix((String) entry.getKey()),
//                            (String) entry.getValue());
//                }
//                mp.bodyPart(bp);
//            }
//            return mp;
//        }

//        private static String addContentPrefix(String key) {
//            if (key == null) {
//                return null;
//            }
//            String lKey = key.toLowerCase(Locale.ENGLISH);
//            //todo: JDK7: convert to String switch-case
//            if ("content-disposition".equals(lKey) ||
//                    "content-type".equals(lKey) ||
//                    "content-transfer-encoding".equals(lKey)) {
//                return key;
//            } else {
//                return "Content-" + key;
//            }
//        }
//
    }

    public static class Inbound extends PayloadImpl.Inbound {

        private List<Payload.Part> parts = new ArrayList<Payload.Part>();

        public Inbound() {
        }

//        private void add(BodyPart bodyPart, String name) throws WebApplicationException {
//            String mimeType = bodyPart.getMediaType().toString();
//            MultivaluedMap<String, String> headers = bodyPart.getHeaders();
//            Properties props = new Properties();
//            for (String key : headers.keySet()) {
//                props.setProperty(removeContentPrefix(key), headers.getFirst(key));
//            }
//            Object entity = bodyPart.getEntity();
//            if (entity == null) {
//                parts.add(PayloadImpl.Part.newInstance(mimeType, name, props, (InputStream) null));
//            } else if (entity instanceof BodyPartEntity) {
//                BodyPartEntity bpe = (BodyPartEntity) entity;
//                parts.add(PayloadImpl.Part.newInstance(mimeType, name, props, bpe.getInputStream()));
//            } else if (entity instanceof String) {
//                parts.add(PayloadImpl.Part.newInstance(mimeType, name, props, (String) entity));
//            } else {
//                throw new WebApplicationException(new Exception("Unsupported entity " + entity.getClass().getName()), Response.Status.BAD_REQUEST);
//            }
//        }

        public void add(String name, InputStream is, String mimeType, List<? extends Header> headers) {
            Properties props = headers2Properties(headers);
            parts.add(PayloadImpl.Part.newInstance(mimeType, name, props, is));
        }

        public void add(String name, String text,  List<? extends Header> headers) {
            Properties props = headers2Properties(headers);
            parts.add(PayloadImpl.Part.newInstance("text/plain", name, props, text));
        }

        private Properties headers2Properties(List<? extends Header> headers) {
            Properties result = new Properties();
            for (Header header : headers) {
                String hname = removeContentPrefix(header.getName());
                if (!result.contains(hname)) {
                    result.setProperty(hname, header.getValue());
                }
            }
            return result;
        }

//        public static Inbound parseFromFormDataMultipart(FormDataMultiPart mp, ParameterMap paramMap) throws WebApplicationException {
//            Inbound result = new Inbound();
//            if (mp == null) {
//                return result;
//            }
//            Map<String, List<FormDataBodyPart>> fields = mp.getFields();
//            for (String fieldName : fields.keySet()) {
//                for (FormDataBodyPart bodyPart : mp.getFields(fieldName)) {
//                    if (bodyPart.isSimple()) {
//                        if (paramMap != null) {
//                            paramMap.add(bodyPart.getName(), bodyPart.getValue());
//                        }
//                    } else {
//                        //It is part of Payload
//                        result.add(bodyPart, bodyPart.getName());
//                    }
//                }
//            }
//            return result;
//        }

//        public static ActionReport fillFromMultipart(MultiPart mp, Inbound inb, Logger logger) throws WebApplicationException {
//            if (logger == null) {
//                logger = AdminLoggerInfo.getLogger();
//            }
//            if (mp == null) {
//                return null;
//            }
//            if (inb == null) {
//                inb = new Inbound();
//            }
//            ActionReport result = null;
//            List<BodyPart> bodyParts = mp.getBodyParts();
//            int index = 0;
//            for (BodyPart bodyPart : bodyParts) {
//                index++;
//                String name = "noname";
//                ContentDisposition cd = bodyPart.getContentDisposition();
//                if (cd != null) {
//                    if (cd.getFileName() != null) {
//                        name = cd.getFileName();
//                    }
//                }
//                if (logger.isLoggable(Level.FINER)) {
//                    logger.log(Level.FINER, "--------- BODY PART [{0}] ---------", index);
//                    MultivaluedMap<String, String> headers = bodyPart.getHeaders();
//                    for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
//                        for (String value : entry.getValue()) {
//                            logger.log(Level.FINER, "{0}: {1}", new Object[]{entry.getKey(), value});
//                        }
//                    }
//                }
//                if (bodyPart.getMediaType().isCompatible(new MediaType("application", "json"))) {
//                    if (logger.isLoggable(Level.FINER)) {
//                        String body = bodyPart.getEntityAs(String.class);
//                        logger.log(Level.FINER, body);
//                    }
//                    result = bodyPart.getEntityAs(ActionReport.class);
//                } else {
//                    logger.log(Level.FINER, "   <<Content>>");
//                    inb.add(bodyPart, name);
//                }
//                if (logger.isLoggable(Level.FINER)) {
//                    logger.log(Level.FINER, "------- END BODY PART [{0}] -------", index);
//                }
//            }
//            return result;
//        }

        private static String removeContentPrefix(String key) {
            if (key == null) {
                return null;
            }
            String lKey = key.toLowerCase(Locale.ENGLISH);
            //todo: JDK7: convert to String switch-case
            if ("content-disposition".equals(lKey) ||
                    "content-type".equals(lKey) ||
                    "content-transfer-encoding".equals(lKey) ||
                    !lKey.startsWith("content-")) {
                return key;
            } else {
                return key.substring("content-".length());
            }
        }

        @Override
        public Iterator<Payload.Part> parts() {
            return parts.iterator();
        }

    }

}

/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admin.rest.shareable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.glassfish.admin.payload.PayloadImpl;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.api.admin.Payload;
import org.glassfish.jersey.media.multipart.*;
import org.glassfish.jersey.message.internal.ContentDisposition;

/** Payload implementation for ReST interface.
 *
 * @author mmares
 */
public class RestPayloadImpl extends PayloadImpl {
    
    public static class Outbound extends PayloadImpl.Outbound {
        
        private String complexType;
        private boolean client2Server;
        
        public Outbound(boolean client2Server) {
            this.client2Server = client2Server;
            if (client2Server) {
                complexType = MediaType.MULTIPART_FORM_DATA;
            } else {
                complexType = "multipart/mixed";
            }
        }
        
        public int size() {
            return getParts().size();
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
        
        public MultiPart addToMultipart(MultiPart mp) {
            if (mp == null) {
                if (client2Server) {
                    mp = new FormDataMultiPart();
                } else {
                    mp = new MultiPart();
                }
            }
            ArrayList<Payload.Part> parts = getParts();
            for (Payload.Part part : parts) {
                String contentType = part.getContentType();
                MediaType mt = new MediaType();
                if (contentType != null && !contentType.isEmpty()) {
                    int ind = contentType.indexOf('/');
                    if (ind > -1) {
                        mt = new MediaType(contentType.substring(0, ind), contentType.substring(ind + 1));
                    } else {
                        mt = new MediaType(contentType, MediaType.WILDCARD);
                    }
                }
                BodyPart bp;
                if (client2Server) {
                    bp = new FormDataBodyPart(part.getName(), part, mt);
                } else {
                    bp = new BodyPart(part, mt);
                    bp.setContentDisposition(ContentDisposition.type("file").fileName(part.getName()).build());
                }
                Properties props = part.getProperties();
                for (String key : props.stringPropertyNames()) {
                    bp.getHeaders().add(addContentPrefix(key), props.getProperty(key));
                }
                mp.bodyPart(bp);
            }
            return mp;
        }
        
        private static String addContentPrefix(String key) {
            if (key == null) {
                return null;
            }
            String lKey = key.toLowerCase();
            //todo: JDK7: convert to String switch-case
            if ("content-disposition".equals(lKey) ||
                    "content-type".equals(lKey) ||
                    "content-transfer-encoding".equals(lKey)) {
                return key;
            } else {
                return key.substring("content-".length());
            }
        }
        
    }
    
    public static class Inbound extends PayloadImpl.Inbound {
        
        private List<Payload.Part> parts = new ArrayList<Payload.Part>();
        
        private Inbound() {
        }
        
        private void add(BodyPart bodyPart, String name) throws WebApplicationException {
            String mimeType = bodyPart.getMediaType().toString();
            MultivaluedMap<String, String> headers = bodyPart.getHeaders();
            Properties props = new Properties();
            for (String key : headers.keySet()) {
                props.setProperty(removeContentPrefix(key), headers.getFirst(key));
            }
            Object entity = bodyPart.getEntity();
            if (entity == null) {
                parts.add(PayloadImpl.Part.newInstance(mimeType, name, props, (InputStream) null));
            } else if (entity instanceof BodyPartEntity) {
                BodyPartEntity bpe = (BodyPartEntity) entity;
                parts.add(PayloadImpl.Part.newInstance(mimeType, name, props, bpe.getInputStream()));
            } else if (entity instanceof String) {
                parts.add(PayloadImpl.Part.newInstance(mimeType, name, props, (String) entity));
            } else {
                throw new WebApplicationException(new Exception("Unsupported entity " + entity.getClass().getName()), Response.Status.BAD_REQUEST);
            }
        }
        
        public static Inbound parseFromFormDataMultipart(FormDataMultiPart mp, ParameterMap paramMap) throws WebApplicationException {
            Inbound result = new Inbound();
            if (mp == null) {
                return result;
            }
            Map<String, List<FormDataBodyPart>> fields = mp.getFields();
            for (String fieldName : fields.keySet()) {
                for (FormDataBodyPart bodyPart : mp.getFields(fieldName)) {
                    if (bodyPart.isSimple()) {
                        if (paramMap != null) {
                            paramMap.add(bodyPart.getName(), bodyPart.getValue());
                        }
                    } else {
                        //It is part of Payload
                        result.add(bodyPart, bodyPart.getName());
                    }
                }
            }
            return result;
        }
        
        public static Inbound parseFromFormDataMultipart(MultiPart mp) throws WebApplicationException {
            Inbound result = new Inbound();
            if (mp == null) {
                return result;
            }
            List<BodyPart> bodyParts = mp.getBodyParts();
            for (BodyPart bodyPart : bodyParts) {
                String name = "noname";
                ContentDisposition cd = bodyPart.getContentDisposition();
                if (cd != null) {
                    if (cd.getFileName() != null) {
                        name = cd.getFileName();
                    }
                }
                result.add(bodyPart, name);
            }
            return result;
        }
        
        private static String removeContentPrefix(String key) {
            if (key == null) {
                return null;
            }
            String lKey = key.toLowerCase();
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

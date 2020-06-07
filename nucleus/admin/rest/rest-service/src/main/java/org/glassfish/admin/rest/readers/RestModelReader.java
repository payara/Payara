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
 *
 * Portions Copyright [2017-2019] Payara Foundation and/or affiliates
 */
package org.glassfish.admin.rest.readers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Locale;
import java.util.Set;
import javax.json.Json;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.stream.JsonParser;
import javax.validation.ConstraintViolation;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import org.glassfish.admin.rest.Constants;
import org.glassfish.admin.rest.composite.CompositeUtil;
import org.glassfish.admin.rest.composite.RestModel;

/**
 *
 * @author jdlee
 */
@Provider
@Produces(Constants.MEDIA_TYPE_JSON)
@Consumes(Constants.MEDIA_TYPE_JSON)
public class RestModelReader<T extends RestModel> implements MessageBodyReader<T> {
    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations,
        MediaType mediaType) {
        String submittedType = mediaType.toString();
        int index = submittedType.indexOf(';');
        if (index > -1) {
            submittedType = submittedType.substring(0, index);
        }
        return submittedType.equals(Constants.MEDIA_TYPE_JSON) &&
                RestModel.class.isAssignableFrom(type);
    }

    @Override
    public T readFrom(Class<T> type, Type type1, Annotation[] antns, MediaType mt,
        MultivaluedMap<String, String> mm, InputStream entityStream) throws WebApplicationException, IOException {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(entityStream));
             JsonParser parser = Json.createParser(in)) {

            final Locale locale = CompositeUtil.instance().getLocale(mm);
            JsonObject o;
            if (parser.hasNext()){
                parser.next();
                o = parser.getObject();
            } else {
                o = JsonValue.EMPTY_JSON_OBJECT;
            }
            T model = CompositeUtil.instance().unmarshallClass(locale, type, o);
            Set<ConstraintViolation<T>> cv = CompositeUtil.instance().validateRestModel(locale, model);
            if (!cv.isEmpty()) {
                final Response response = Response.status(Status.BAD_REQUEST)
                        .entity(CompositeUtil.instance().getValidationFailureMessages(locale, cv, model))
                        .build();
                throw new WebApplicationException(response);
            }
            return model;
        } catch (JsonException ex) {
            throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity(ex.getLocalizedMessage()).build());
        }
    }
}

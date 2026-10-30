/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2019-2021 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package fish.payara.ejb.http.protocol.rs;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.Provider;

import fish.payara.ejb.http.protocol.InvokeMethodRequest;
import fish.payara.ejb.http.protocol.MediaTypes;

/**
 * Reads the {@link InvokeMethodRequest} in case of JSONB serialisation.
 *
 * In that case the {@link InvokeMethodRequest#argValues} are JSON representation of the original {@code Object[]} for
 * the method arguments.
 * 
 * @author Jan Bernitt
 */
@Provider
@Consumes(MediaTypes.JSON)
public class JsonbInvokeMethodMessageBodyReader implements MessageBodyReader<InvokeMethodRequest> {

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return InvokeMethodRequest.class.isAssignableFrom(type);
    }

    @Override
    public InvokeMethodRequest readFrom(Class<InvokeMethodRequest> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
                    throws IOException, WebApplicationException {
        try (JsonReader jsonReader = Json.createReader(entityStream)) {
            JsonObject json = jsonReader.readObject();
            String[] argTypeNames = json.getJsonArray("argTypes").stream().map(JsonbInvokeMethodMessageBodyReader::text)
                    .toArray(String[]::new);
            JsonArray actualTypes = json.getJsonArray("argActualTypes");
            String[] argActualTypeNames = actualTypes == null ? argTypeNames
                    : actualTypes.stream().map(JsonbInvokeMethodMessageBodyReader::text).toArray(String[]::new);
            return new InvokeMethodRequest(
                    json.getString("java.naming.security.principal", ""),
                    json.getString("java.naming.security.credentials", ""),
                    json.getString("lookup"),
                    json.getString("method"),
                    argTypeNames,
                    argActualTypeNames,
                    json.get("argValues"),
                    (args, method, argActualTypes, classloader) -> toObjects(method, argActualTypes, args));
        }
    }

    private static String text(JsonValue value) {
        return value instanceof JsonString ? ((JsonString) value).getString() : value.toString();
    }

    /**
     * Convert JSON encoded method parameter values to their object instances 
     */
    private static Object[] toObjects(Method invoked, Type[] argActualTypes, Object encodedArgValues) {
        try {
            Object[] argValues = new Object[argActualTypes.length];
            Type[] argFormalTypes = invoked.getGenericParameterTypes();
            if (argValues.length > 0) {
                JsonArray jsonArgValues = (JsonArray) encodedArgValues;
                for (int i = 0; i < jsonArgValues.size(); i++) {
                    Type argFormalType = argFormalTypes[i];
                    Type argType = argFormalType instanceof ParameterizedType ? argFormalType : argActualTypes[i];
                    argValues[i] =  toObject(jsonArgValues.get(i), argType);
                }
            }
            return argValues;
        } catch (InternalServerErrorException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new InternalServerErrorException(
                    "Failed to de-serialise method arguments from binary representation.", ex);
        }
    }

    private static Object toObject(JsonValue objectValue, Type type) {
        try (Jsonb jsonb = JsonbBuilder.create()) {
            return jsonb.fromJson(objectValue.toString(), type);
        } catch (Exception ex) {
            // cannot really happen. It is just from java.lang.AutoCloseable interface
            throw new InternalServerErrorException("Failed to deserialize argument of type: " + type.toString(), ex);
        }
    }

}

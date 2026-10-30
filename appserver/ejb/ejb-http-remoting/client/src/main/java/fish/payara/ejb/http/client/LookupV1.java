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
package fish.payara.ejb.http.client;

import static fish.payara.ejb.http.client.RemoteEJBContextFactory.JAXRS_CLIENT_SERIALIZATION;
import static java.lang.reflect.Proxy.newProxyInstance;
import static java.security.AccessController.doPrivileged;

import java.security.PrivilegedAction;
import java.util.Map;

import javax.naming.NamingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import fish.payara.ejb.http.protocol.ErrorResponse;
import fish.payara.ejb.http.protocol.LookupRequest;
import fish.payara.ejb.http.protocol.LookupResponse;
import fish.payara.ejb.http.protocol.MediaTypes;
import fish.payara.ejb.http.protocol.SerializationType;
import fish.payara.ejb.http.protocol.rs.ObjectStreamMessageBodyReader;
import fish.payara.ejb.http.protocol.rs.ObjectStreamMessageBodyWriter;

public class LookupV1 extends Lookup {

    private final Client client;
    private final WebTarget v1lookup;
    private final String mediaType;

    LookupV1(Map<String, Object> environment, Client client, WebTarget v1lookup) {
        super(environment);
        this.client = client
                .register(ObjectStreamMessageBodyWriter.class)
                .register(ObjectStreamMessageBodyReader.class);
        this.v1lookup = v1lookup
                .register(ObjectStreamMessageBodyReader.class)
                .register(ObjectStreamMessageBodyWriter.class);
        this.mediaType = environment.containsKey(JAXRS_CLIENT_SERIALIZATION)
                ? SerializationType.valueOf(
                        environment.get(JAXRS_CLIENT_SERIALIZATION).toString().toUpperCase()).getMediaType()
                : MediaTypes.JSON;
    }

    @Override
    Object lookup(String jndiName) throws NamingException {
        Entity<LookupRequest> body = Entity.entity(new LookupRequest(jndiName), mediaType);
        try (Response response = v1lookup.request(mediaType).buildPost(body).invoke()) {
            if (response.getStatus() == Status.CREATED.getStatusCode()) {
                String className = response.readEntity(LookupResponse.class).typeName;
                try {
                    Class<?> remoteBusinessInterface = Class.forName(className);
                    return newProxy(remoteBusinessInterface, client.target(response.getLocation()), mediaType, jndiName, environment);
                } catch (ClassNotFoundException ex) {
                    throw wrap("Local class " + className + " does not exist for JNDI name: " + className, ex);
                }
            }
            if (!response.getMediaType().toString().equals(mediaType)) {
                throw new NamingException(response.readEntity(String.class));
            }
            Exception ex = deserialise(response.readEntity(ErrorResponse.class));
            if (ex instanceof NamingException) {
                throw (NamingException) ex;
            }
            throw wrap("Lookup failed for `" + jndiName + "` [" + mediaType + "]. Remote exception: ", ex);
        }
    }

    static Exception deserialise(ErrorResponse error) {
        Exception ex = null;
        try {
            ex = (Exception) Class.forName(error.exceptionType).getConstructor(String.class).newInstance(error.message);
        } catch (Exception e) {
            ex = new NamingException("Remote exception: " + error.exceptionType + ": " + error.message);
        }
        if (error.cause != null) {
            ex.initCause(deserialise(error.cause));
        }
        return ex;
    }

    @SuppressWarnings("unchecked")
    private static <C> C newProxy(Class<C> remoteBusinessInterface, WebTarget invoke, String mediaType, String jndiName,
            Map<String, Object> jndiOptions) {
        return (C) newProxyInstance(doPrivileged((PrivilegedAction<ClassLoader>) remoteBusinessInterface::getClassLoader),
                new Class[] { remoteBusinessInterface },
                new EjbHttpProxyHandlerV1(mediaType, invoke, jndiName, jndiOptions));
    }

}

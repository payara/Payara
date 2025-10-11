/*
 *    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2019-2021] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *    The contents of this file are subject to the terms of either the GNU
 *    General Public License Version 2 only ("GPL") or the Common Development
 *    and Distribution License("CDDL") (collectively, the "License").  You
 *    may not use this file except in compliance with the License.  You can
 *    obtain a copy of the License at
 *    https://github.com/payara/Payara/blob/main/LICENSE.txt
 *    See the License for the specific
 *    language governing permissions and limitations under the License.
 *
 *    When distributing the software, include this License Header Notice in each
 *    file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 *    GPL Classpath Exception:
 *    The Payara Foundation designates this particular file as subject to the "Classpath"
 *    exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *    file that accompanied this code.
 *
 *    Modifications:
 *    If applicable, add the following below the License Header, with the fields
 *    enclosed by brackets [] replaced by your own identifying information:
 *    "Portions Copyright [year] [name of copyright owner]"
 *
 *    Contributor(s):
 *    If you wish your version of this file to be governed by only the CDDL or
 *    only the GPL Version 2, indicate your decision by adding "[Contributor]
 *    elects to include this software in this distribution under the [CDDL or GPL
 *    Version 2] license."  If you don't indicate a single choice of license, a
 *    recipient has the option to distribute your version of this file under
 *    either the CDDL, the GPL Version 2 or to extend the choice of license to
 *    its licensees as provided above.  However, if you add GPL Version 2 code
 *    and therefore, elected the GPL Version 2 license, then the option applies
 *    only if the new code is made subject to such option by the copyright
 *    holder.
 */

package fish.payara.ejb.http.client;

import fish.payara.ejb.http.protocol.ErrorResponse;
import fish.payara.ejb.http.protocol.InvokeMethodRequest;
import fish.payara.ejb.http.protocol.InvokeMethodResponse;
import fish.payara.ejb.http.protocol.MediaTypes;
import fish.payara.ejb.http.protocol.rs.InvokeMethodResponseJsonBodyReader;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Map;

import static java.util.Arrays.asList;
import static javax.naming.Context.SECURITY_CREDENTIALS;
import static javax.naming.Context.SECURITY_PRINCIPAL;

final class EjbHttpProxyHandlerV1 implements InvocationHandler {

    private final String mediaType;
    private final WebTarget invoke;
    private final String jndiName;
    private final Map<String, Object> jndiOptions;

    public EjbHttpProxyHandlerV1(String mediaType, WebTarget invoke, String jndiName, Map<String, Object> jndiOptions) {
        this.jndiName = jndiName;
        this.jndiOptions = jndiOptions;
        this.invoke = mediaType.equals(MediaTypes.JSON) ? invoke.register(InvokeMethodResponseJsonBodyReader.class) : invoke;
        this.mediaType = mediaType;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // Check for methods we should not proxy first
        if (args == null && method.getName().equals("toString")) {
            return toString();
        }
        if (args == null && method.getName().equals("hashCode")) {
            // unique instance in the JVM, and no need to override
            return hashCode();
        }
        if (args != null && args.length == 1 && method.getName().equals("equals")) {
            // unique instance in the JVM, and no need to override
            return equals(args[0]);
        }
        return invokeRemote(method, args);
    }

    private Object invokeRemote(Method method, Object[] args) throws Exception {
        InvokeMethodRequest request = createRequest(method, args);
        try (Response response = invoke
                .request(mediaType)
                .buildPost(Entity.entity(request, mediaType)).invoke()) {
            if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                return response
                        .readEntity(InvokeMethodResponse.class, InvokeMethodResponse.ResultType.of(method)).result;
            }
            ErrorResponse error = response.readEntity(ErrorResponse.class);
            throw LookupV1.deserialise(error);
        }
    }

    private InvokeMethodRequest createRequest(Method method, Object[] args) {
        String principal = jndiOptions.containsKey(SECURITY_PRINCIPAL)
                ? Lookup.base64Encode(jndiOptions.get(SECURITY_PRINCIPAL))
                : "";
        String credentials = jndiOptions.containsKey(SECURITY_CREDENTIALS)
                ? Lookup.base64Encode(jndiOptions.get(SECURITY_CREDENTIALS))
                : "";
        String[] argTypes = asList(method.getParameterTypes()).stream()
                .map(type -> type.getName())
                .toArray(String[]::new);
        String[] argActualTypes = args == null ? new String[0] : asList(args).stream()
                .map(arg -> arg == null ? null : arg.getClass().getName())
                .toArray(String[]::new);
        for (int i = 0; i < argTypes.length; i++) {
            if (argActualTypes[i] == null) {
                argActualTypes[i] = argTypes[i];
            }
        }
        return new InvokeMethodRequest(principal, credentials, jndiName, method.getName(), argTypes, argActualTypes,
                packArguments(args), null);
    }

    private Object packArguments(Object[] args) {
        Object argValues = args;
        if (MediaTypes.JAVA_OBJECT.equals(mediaType)) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                oos.writeObject(argValues);
            } catch (IOException e) {
                throw new UndeclaredThrowableException(e);
            }
            argValues = bos.toByteArray();
        }
        return argValues;
    }
}

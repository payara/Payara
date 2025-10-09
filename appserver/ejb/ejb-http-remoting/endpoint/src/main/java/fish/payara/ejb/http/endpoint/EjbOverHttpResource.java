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
package fish.payara.ejb.http.endpoint;

import static java.util.Arrays.asList;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Arrays;
import java.util.Base64;
import java.util.function.BiFunction;

import jakarta.json.bind.JsonbBuilder;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.glassfish.internal.api.Globals;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.data.ApplicationRegistry;

import com.sun.enterprise.security.ee.auth.login.ProgrammaticLogin;

import fish.payara.ejb.http.protocol.ErrorResponse; 
import fish.payara.ejb.http.protocol.InvokeMethodRequest;
import fish.payara.ejb.http.protocol.InvokeMethodResponse;
import fish.payara.ejb.http.protocol.LookupRequest;
import fish.payara.ejb.http.protocol.LookupResponse;
import fish.payara.ejb.http.protocol.MediaTypes;

@Path("/")
public class EjbOverHttpResource {

    private static final String INVOKER_V1_REL = "https://payara.fish/ejb-http-invoker/v1";
    private static final String INVOKER_V0_REL = "https://payara.fish/ejb-http-invoker/v0";

    private final EjbOverHttpService service;

    public EjbOverHttpResource() {
        this(new EjbOverHttpService() {

            @Override
            public Object getBean(String jndiName) throws NamingException {
                return new InitialContext().lookup(jndiName);
            }

            @Override
            public ClassLoader getAppClassLoader(String applicationName) {
                ApplicationRegistry registry = Globals.get(ApplicationRegistry.class);
                ApplicationInfo info = registry.get(applicationName);
                return info == null ? null : info.getAppClassLoader();
            }
        });
    }

    public EjbOverHttpResource(EjbOverHttpService backend) {
        this.service = backend;
    }

    @HEAD
    public Response discover() {
        return Response.ok()
                .link("jndi/lookup", INVOKER_V1_REL)
                .link("ejb/lookup", INVOKER_V0_REL)
                .build();
    }

    @POST
    @Path("jndi/lookup")
    @Produces(MediaTypes.JAVA_OBJECT)
    @Consumes(MediaTypes.JAVA_OBJECT)
    public Response lookupJavaSerialization(LookupRequest body) {
        return lookup(body, MediaTypes.JAVA_OBJECT);
    }

    @POST
    @Path("jndi/lookup")
    @Produces(MediaTypes.JSON)
    @Consumes(MediaTypes.JSON)
    public Response lookupJsonb(LookupRequest body) {
        return lookup(body, MediaTypes.JSON);
    }

    private Response lookup(LookupRequest body, String mediaType) {
        try {
            return Response
                    .status(Status.CREATED)
                    .type(mediaType)
                    .location(new URI("jndi/invoke"))
                    .entity(doLookup(body))
                    .build();
        } catch (Exception e) {
            return Response
                    .status(Status.BAD_REQUEST)
                    .type(mediaType)
                    .entity(new ErrorResponse(e))
                    .build();
        }
    }

    @POST
    @Path("jndi/invoke")
    @Produces(MediaTypes.JAVA_OBJECT)
    @Consumes(MediaTypes.JAVA_OBJECT)
    public Response invokeJavaSerilaization(InvokeMethodRequest body) {
        return invoke(body, MediaTypes.JAVA_OBJECT, (type, result) -> result);
    }

    @POST
    @Path("jndi/invoke")
    @Produces(MediaTypes.JSON)
    @Consumes(MediaTypes.JSON)
    public Response invokeJsonb(InvokeMethodRequest body) {
        return invoke(body, MediaTypes.JSON, this::serializeToJsonb);
    }

    private Reader serializeToJsonb(Type returnType, InvokeMethodResponse result) {
        // this is on the edge of JSON-B capabilities, combined with the fact that JAX-RS serialization would run outside the classloaders
        // that contain the class definitions.
        StringWriter output = new StringWriter();
        output.append("{\"type\":\"").append(result.type).append("\"");
        if (result != null) {
            output.append(",\"result\":");
            JsonbBuilder.create().toJson(result.result, returnType, output);
        }
        output.append("}");
        return new StringReader(output.toString());
    }

    private Response invoke(InvokeMethodRequest body, String mediaType, BiFunction<Type, InvokeMethodResponse, Object> resultSerializer) {
        try {
            return Response
                    .status(Status.OK)
                    .type(mediaType)
                    .entity(doInvoke(body, resultSerializer))
                    .build();
        } catch (Exception e) {
            return Response
                    .status(Status.BAD_REQUEST)
                    .type(mediaType)
                    .entity(new ErrorResponse(e))
                    .build();
        }
    }

    private LookupResponse doLookup(LookupRequest request) throws Exception {
        String jndiName = request.jndiName;
        return excuteInAppContext(jndiName, ejb -> {
            int bangIndex = jndiName.indexOf('!');
            if (bangIndex > 0) {
                String className = jndiName.substring(bangIndex + 1);
                try {
                    return new LookupResponse(
                            Class.forName(className, false, Thread.currentThread().getContextClassLoader()));
                } catch (ClassNotFoundException ex) {
                    NamingException nex = new NamingException("Unknown class: "+className);
                    nex.initCause(ex);
                    throw nex;
                }
            }
            // there should only be one interface otherwise plain name would not be allowed (portable names at least)
            // in fact, there can be some implementation-specific interfaces in the proxy as well
            return new LookupResponse(ejb.getClass().getInterfaces()[0]);
        });
    }

    private Object doInvoke(InvokeMethodRequest request, BiFunction<Type,InvokeMethodResponse,Object> resultMapper) throws Exception {
        return excuteInAppContext(request.jndiName, ejb -> {
            // Authenticates the caller and if successful sets the security context
            // *for the outgoing EJB call*. In other words, the security context for this
            // resource will not be changed.
            if (!request.principal.isEmpty()) {
                new ProgrammaticLogin().login(base64Decode(request.principal), base64Decode(request.credentials), null, true);
            }
            Class<?>[] argTypes = toClasses(request.argTypes);
            Class<?>[] argActualTypes = toClasses(request.argActualTypes);
            Method method = findBusinessMethodDeclaration(ejb, request.method, argTypes);
            Object result = method.invoke(ejb,
                    request.argDeserializer.deserialise(request.argValues, method, argActualTypes,
                            Thread.currentThread().getContextClassLoader()));

            return resultMapper.apply(method.getGenericReturnType(), new InvokeMethodResponse(result));
        });
    }

    private static Class<?>[] toClasses(String[] classNames) {
        return asList(classNames).stream().map(EjbOverHttpResource::toClass).toArray(Class[]::new);
    }

    private <T> T excuteInAppContext(String jndiName, EjbOperation<T> operation) throws Exception {
        if (!jndiName.startsWith("java:global/")) {
            throw new NamingException("Only global names are supported but got: " + jndiName);
        }
        if (jndiName.indexOf('/', 12) < 0) {
            throw new NamingException("Global name must contain application name but got: " + jndiName);
        }

        Thread currentThread = Thread.currentThread();
        String applicationName = jndiName.substring(12, jndiName.indexOf('/', 12));
        ClassLoader existingContextClassLoader = currentThread.getContextClassLoader();
        ClassLoader appClassLoader = service.getAppClassLoader(applicationName);
        if (appClassLoader == null) {
            throw new NamingException("Unknown application: " + applicationName);
        }
        try {
            currentThread.setContextClassLoader(appClassLoader);
            Object bean = service.getBean(jndiName);
            return operation.execute(bean);
        } finally {
            if (existingContextClassLoader != null) {
                currentThread.setContextClassLoader(existingContextClassLoader);
            }
        }
    }

    private static String base64Decode(String input) {
        return new String(Base64.getDecoder().decode(input));
    }

    private static Class<?> toClass(String name) {
        try {
            switch (name) {
            case "int":
                return int.class;
            case "long":
                return long.class;
            case "short":
                return short.class;
            case "byte":
                return byte.class;
            case "boolean":
                return boolean.class;
            case "float":
                return float.class;
            case "double":
                return double.class;
            case "char":
                return char.class;
            case "void":
                return void.class;
            default:
                return Class.forName(name, true, Thread.currentThread().getContextClassLoader());
            }
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Method findBusinessMethodDeclaration(Object ejb, String methodName, Class<?>[] argTypeClasses)
            throws NoSuchMethodException {
        for (Class<?> intf : ejb.getClass().getInterfaces()) {
            try {
                return intf.getMethod(methodName, argTypeClasses);
            } catch (NoSuchMethodException e) {
                // try further
            }
        }
        throw new NoSuchMethodException("No method matching " + methodName + "(" + Arrays.toString(argTypeClasses)
                + ") found in business interface");
    }

    /**
     * Needed because of the {@link Exception} thrown.
     */
    interface EjbOperation<T> {

        T execute(Object bean) throws Exception;
    }
}

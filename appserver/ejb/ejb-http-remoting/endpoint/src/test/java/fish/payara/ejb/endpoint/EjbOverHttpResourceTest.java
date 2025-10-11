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
package fish.payara.ejb.endpoint;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.URI;
import java.util.*;

import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;
import javax.naming.NamingException;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriBuilder;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.hamcrest.CoreMatchers;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import fish.payara.ejb.http.endpoint.EjbOverHttpResource;
import fish.payara.ejb.http.endpoint.EjbOverHttpService;
import fish.payara.ejb.http.protocol.ErrorResponse;
import fish.payara.ejb.http.protocol.InvokeMethodRequest;
import fish.payara.ejb.http.protocol.InvokeMethodResponse;
import fish.payara.ejb.http.protocol.LookupRequest;
import fish.payara.ejb.http.protocol.LookupResponse;
import fish.payara.ejb.http.protocol.MediaTypes;
import fish.payara.ejb.http.protocol.rs.ErrorResponseExceptionMapper;
import fish.payara.ejb.http.protocol.rs.JsonbInvokeMethodMessageBodyReader;
import fish.payara.ejb.http.protocol.rs.JsonbLookupMessageBodyReader;
import fish.payara.ejb.http.protocol.rs.ObjectStreamInvokeMethodMessageBodyReader;
import fish.payara.ejb.http.protocol.rs.ObjectStreamMessageBodyReader;
import fish.payara.ejb.http.protocol.rs.ObjectStreamMessageBodyWriter;

/**
 * A component test for the {@link EjbOverHttpResource} that tests with both JSONB and java object serialisation.
 * 
 * @author Jan Bernitt 
 */
@RunWith(Parameterized.class)
public class EjbOverHttpResourceTest {

    private static final String EJB_NAME = "java:global/myapp/RemoteCalculator";

    @Parameters(name = "{0}")
    public static Iterable<String> mediaTypes() {
        return Arrays.asList(MediaTypes.JAVA_OBJECT, MediaTypes.JSON);
    }

    /**
     * The {@link MediaType} currently tested
     */
    @Parameter
    public String mediaType;

    static class EjbOverHttpResourceTestApplication extends Application implements EjbOverHttpService {
        @Override
        public Set<Class<?>> getClasses() {
            return null;
        }

        @Override
        public Set<Object> getSingletons() {
            return new HashSet<>(Arrays.asList(new EjbOverHttpResource(this),
                    new ErrorResponseExceptionMapper()));
        }

        @Override
        public ClassLoader getAppClassLoader(String applicationName) {
            if ("unknownapp".equals(applicationName)) {
                return null;
            }
            return RemoteCalculator.class.getClassLoader();
        }

        @Override
        public Object getBean(String jndiName) throws NamingException {
            if (!jndiName.endsWith(RemoteCalculator.class.getSimpleName())) {
                throw new NamingException("No such bean: " + jndiName);
            }
            return new CalculatorBean();
        }
    }

    public interface RemoteCalculator {

        int add(int a, int b);

        List<Object> cyclic();

        Map<String, Object> getSettings();
    }

    /**
     * The annotations don't really have any effect here since the test doesn't do a real EJB or JNDI lookup.
     * This is just informal for the scenario tested here.
     */
    @Stateless
    @Remote(RemoteCalculator.class)
    public static class CalculatorBean implements RemoteCalculator {

        @Override
        public int add(int a, int b) {
            return a + b;
        }

        @Override
        public Map<String, Object> getSettings() {
            HashMap<String, Object> settings = new HashMap<>();
            settings.put("uuid", UUID.randomUUID());
            settings.put("list", Collections.singletonList(42));
            return settings;
        }

        @Override
        public List<Object> cyclic() {
            List<Object> cyclicObject = new ArrayList<>();
            cyclicObject.add(cyclicObject);
            return cyclicObject;
        }
    }

    private static HttpServer server;
    private static WebTarget target;

    @BeforeClass
    public static void setUp() {
        URI baseUri = localhostWithFreePort();
        server = spawnServer(baseUri, new EjbOverHttpResourceTestApplication());
        target = createClient(baseUri);
    }

    @AfterClass
    public static void tearDown() {
        if (server != null) {
            server.shutdown();
        }
    }

    private static URI localhostWithFreePort() {
        int port = 8080;
        try(ServerSocket ss = new ServerSocket(0)) {
            port = ss.getLocalPort();
        } catch (Exception e) {
            // try with 8080
        }
        URI baseUri = UriBuilder.fromUri("http://localhost/").port(port).build();
        return baseUri;
    }

    private static HttpServer spawnServer(URI baseUri, Application app) {
        ResourceConfig config = ResourceConfig.forApplication(app)
                .register(ObjectStreamMessageBodyReader.class)
                .register(ObjectStreamMessageBodyWriter.class)
                .register(ObjectStreamInvokeMethodMessageBodyReader.class)
                .register(JsonbInvokeMethodMessageBodyReader.class)
                .register(JsonbLookupMessageBodyReader.class);
        return GrizzlyHttpServerFactory.createHttpServer(baseUri, config);
    }

    private static WebTarget createClient(URI baseUri) {
        return ClientBuilder.newClient()
                .target(baseUri)
                .register(ObjectStreamMessageBodyReader.class)
                .register(ObjectStreamMessageBodyWriter.class);
    }

    @Test
    public void discover() {
        try (Response response = target.path("/").request().build("HEAD").invoke()) {
            assertEquals(Status.OK.getStatusCode(), response.getStatus());
            assertEquals(2, response.getLinks().size());
            int port = target.getUri().getPort();
            assertEquals("http://localhost:" + port + "/jndi/lookup",
                    response.getLink("https://payara.fish/ejb-http-invoker/v1").getUri().toString());
            assertEquals("http://localhost:" + port + "/ejb/lookup",
                    response.getLink("https://payara.fish/ejb-http-invoker/v0").getUri().toString());
        }
    }

    @Test
    public void lookup_SuccessInterfaceName() {
        LookupResponse response = lookupExpectSuccess(mediaType, EJB_NAME);
        assertEquals(RemoteCalculator.class.getName(), response.typeName);
        assertEquals("Stateless", response.kind);
    }

    @Test
    public void lookup_SuccessFullName() {
        LookupResponse response = lookupExpectSuccess(mediaType,
                "java:global/myapp/CalculatorBean!" + RemoteCalculator.class.getName());
        assertEquals(RemoteCalculator.class.getName(), response.typeName);
        assertEquals("Stateless", response.kind);
    }

    @Test
    public void lookup_ErrorFullNameWithoutPackage() {
        ErrorResponse response = lookupExpectError(mediaType, "java:global/myapp/CalculatorBean!RemoteCalculator");
        assertEquals("javax.naming.NamingException", response.exceptionType);
        assertNotNull(response.cause);
        assertEquals("java.lang.ClassNotFoundException", response.cause.exceptionType);
        assertEquals("RemoteCalculator", response.cause.message);
    }

    @Test
    public void lookup_ErrorNoGlobalJndiName() {
        ErrorResponse response = lookupExpectError(mediaType, "java:app/myapp/" + RemoteCalculator.class.getName());
        assertEquals("javax.naming.NamingException", response.exceptionType);
        assertEquals("Only global names are supported but got: java:app/myapp/fish.payara.ejb.endpoint.EjbOverHttpResourceTest$RemoteCalculator", response.message);
        assertNull(response.cause);
    }

    @Test
    public void lookup_ErrorMalformedGlobalJndiName() {
        ErrorResponse response = lookupExpectError(mediaType, "java:global/" + RemoteCalculator.class.getName());
        assertEquals("javax.naming.NamingException", response.exceptionType);
        assertEquals("Global name must contain application name but got: java:global/fish.payara.ejb.endpoint.EjbOverHttpResourceTest$RemoteCalculator", response.message);
        assertNull(response.cause);
    }

    @Test
    public void invoke_SuccessWithArguments() {
        InvokeMethodResponse response = invokeExpectSuccess(mediaType, EJB_NAME, "add",
                new String[] { int.class.getName(), int.class.getName() }, pack(1, 2));
        assertEquals("Actual type should be wrapper of int", Integer.class.getName(), response.type);
        assertEquals(3, response.result);
    }

    @Test
    public void invoke_SuccessWithComplexResult() {
        InvokeMethodResponse response = invokeExpectSuccess(mediaType, EJB_NAME, "getSettings", 
                new String[0], pack(new Object[0]));
        @SuppressWarnings("unchecked")
        Map<String, Object> settings = (Map<String, Object>) response.result;
        if (isJavaObjectSerialisation()) {
            assertSame(UUID.class, settings.get("uuid").getClass());
        } else {
            // this shows that JSONB is not capable of preserving the UUID while java serialisation is
            assertSame(String.class, settings.get("uuid").getClass());
        }
        @SuppressWarnings("unchecked")
        List<Object> list = (List<Object>) settings.get("list");
        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals(42, ((Number) list.get(0)).intValue());
    }

    @Test
    public void invoke_ErrorClassNotFound() {
        ErrorResponse response = invokeExpectError(mediaType, EJB_NAME, "add",
                new String[] { "undefined", "int" }, pack(1, 2));
        assertThat(response.message, CoreMatchers.endsWith("java.lang.ClassNotFoundException: undefined"));
        assertNotNull(response.cause);
    }

    @Test
    public void invoke_ErrorNoSuchApplication() {
        ErrorResponse response = invokeExpectError(mediaType, EJB_NAME.replace("/myapp/", "/unknownapp/"), "sub",
                new String[] { int.class.getName(), int.class.getName() }, pack(1, 2));
        assertEquals("javax.naming.NamingException", response.exceptionType);
        assertEquals("Unknown application: unknownapp", response.message);
        assertNull(response.cause);
    }

    @Test
    public void invoke_ErrorNoSuchMethodWrongName() {
        ErrorResponse response = invokeExpectError(mediaType, EJB_NAME, "sub",
                new String[] { int.class.getName(), int.class.getName() }, pack(1, 2));
        assertEquals("java.lang.NoSuchMethodException", response.exceptionType);
        assertEquals("No method matching sub([int, int]) found in business interface", response.message);
        assertNull(response.cause);
    }

    @Test
    public void invoke_ErrorNoSuchMethodWrongArgumentType() {
        ErrorResponse response = invokeExpectError(mediaType, EJB_NAME, "add",
                new String[] { float.class.getName(), int.class.getName() }, pack(1, 2));
        assertEquals("java.lang.NoSuchMethodException", response.exceptionType);
        assertEquals("No method matching add([float, int]) found in business interface", response.message);
        assertNull(response.cause);
    }

    @Test
    public void invoke_ErrorNoSuchEjb() {
        ErrorResponse response = invokeExpectError(mediaType, EJB_NAME+"x", "add",
                new String[] { int.class.getName(), int.class.getName() }, pack(1, 2));
        assertEquals("javax.naming.NamingException", response.exceptionType);
        assertEquals("No such bean: java:global/myapp/RemoteCalculatorx", response.message);
        assertNull(response.cause);
    }

    @Test
    public void invoke_ErrorMalformedArguments() {
        Object argValues = isJavaObjectSerialisation() ? new byte[] { 42 } : Collections.singletonMap("foo", "bar");
        ErrorResponse response = invokeExpectError(mediaType, EJB_NAME, "add",
                new String[] { int.class.getName(), int.class.getName() }, argValues);
        assertEquals("jakarta.ws.rs.InternalServerErrorException", response.exceptionType);
        assertEquals("Failed to de-serialise method arguments from binary representation.", response.message);
    }

    @Test
    public void invoke_CyclicResult() {
        if (isJavaObjectSerialisation()) {
            InvokeMethodResponse response = invokeExpectSuccess(mediaType, EJB_NAME, "cyclic", new String[0],
                    pack(new Object[0]));
            @SuppressWarnings("unchecked")
            List<Object> result = (List<Object>) response.result;
            assertEquals(1, result.size());
            assertSame(result, result.get(0));
        } else {
            ErrorResponse response = invokeExpectError(mediaType, EJB_NAME, "cyclic", new String[0],
                    pack(new Object[0]));
            assertEquals("java.lang.StackOverflowError", response.exceptionType);
        }
    }

    private static InvokeMethodResponse invokeExpectSuccess(String mediaType, String jndiName, String method,
            String[] argTypes, Object argValues) {
        Entity<InvokeMethodRequest> entity = invokeBody(mediaType, jndiName, method, argTypes, argValues);
        try (Response response = target.path("jndi/invoke").request(mediaType).buildPost(entity).invoke()) {
            assertNoError(response, Status.OK);
            return response.readEntity(InvokeMethodResponse.class);
        }
    }

    private static ErrorResponse invokeExpectError(String mediaType, String jndiName, String method, String[] argTypes,
            Object argValues) {
        Entity<InvokeMethodRequest> entity = invokeBody(mediaType, jndiName, method, argTypes, argValues);
        try (Response response = target.path("jndi/invoke").request(mediaType).buildPost(entity).invoke()) {
            assertNotEquals(Status.OK.getStatusCode(), response.getStatus());
            if (mediaType.equals(response.getMediaType().toString())) {
                return response.readEntity(ErrorResponse.class);
            }
            fail("Unexpected error response in media type "+response.getMediaType()+": "+response.readEntity(String.class));
            return null;
        }
    }

    private static LookupResponse lookupExpectSuccess(String mediaType, String jndiName) {
        Entity<LookupRequest> entity = lookupBody(mediaType, jndiName);
        try (Response response = target.path("jndi/lookup").request(mediaType).buildPost(entity).invoke()) {
            assertNoError(response, Status.CREATED);
            assertThat(response.getLocation().getPath(), endsWith("/jndi/invoke"));
            return response.readEntity(LookupResponse.class);
        }
    }

    private static ErrorResponse lookupExpectError(String mediaType, String jndiName) {
        Entity<LookupRequest> entity = lookupBody(mediaType, jndiName);
        try (Response response = target.path("jndi/lookup").request(mediaType).buildPost(entity).invoke()) {
            assertNotEquals(Status.CREATED.getStatusCode(), response.getStatus());
            return response.readEntity(ErrorResponse.class);
        }
    }

    private static Entity<LookupRequest> lookupBody(String mediaType, String jndiName) {
        return Entity.entity(new LookupRequest(jndiName), mediaType);
    }

    private static Entity<InvokeMethodRequest> invokeBody(String mediaType, String jndiName, String method,
            String[] argTypes, Object argValues) {
        return Entity.entity(new InvokeMethodRequest("", "", jndiName, method, argTypes, argTypes, argValues, null),
                mediaType);
    }

    private static void assertNoError(Response response, Status expectedStatus) {
        if (response.getStatus() == 400) {
            ErrorResponse error = response.readEntity(ErrorResponse.class);
            fail("Remote error: " + error.toString());
        }
        if (response.getStatus() != expectedStatus.getStatusCode()) {
            fail("Unexpected status: " + response.readEntity(String.class));
        }
    }

    private boolean isJavaObjectSerialisation() {
        return mediaType.equals("application/x-java-object");
    }

    /**
     * When using Java serialisation arguments have to be converted to a byte[] before the {@link InvokeMethodRequest}
     * is created since the objects cannot be de-serialised at the boundaries of the JAX-RS interface as the
     * application {@link ClassLoader} has to be used for that.
     */
    private Object pack(Object... argValues) throws AssertionError {
        if (!isJavaObjectSerialisation()) {
            return argValues;
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(argValues);
        } catch (IOException e) {
            throw new AssertionError("Failed to pack arguments: ", e);
        }
        return bos.toByteArray();
    }

}

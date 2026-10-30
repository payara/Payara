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

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.NotSerializableException;
import java.io.Serializable;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.ServerSocket;
import java.net.URI;
import java.util.*;

import jakarta.json.bind.annotation.JsonbCreator;
import jakarta.json.bind.annotation.JsonbProperty;
import javax.naming.Context;
import javax.naming.NamingException;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.UriBuilder;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.hamcrest.CoreMatchers;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import fish.payara.ejb.http.endpoint.EjbOverHttpResource;
import fish.payara.ejb.http.endpoint.EjbOverHttpService;
import fish.payara.ejb.http.protocol.SerializationType;
import fish.payara.ejb.http.protocol.rs.ErrorResponseExceptionMapper;
import fish.payara.ejb.http.protocol.rs.JsonbInvokeMethodMessageBodyReader;
import fish.payara.ejb.http.protocol.rs.JsonbLookupMessageBodyReader;
import fish.payara.ejb.http.protocol.rs.ObjectStreamInvokeMethodMessageBodyReader;
import fish.payara.ejb.http.protocol.rs.ObjectStreamMessageBodyReader;
import fish.payara.ejb.http.protocol.rs.ObjectStreamMessageBodyWriter;

/**
 * Testing the client with the endpoint running in a HTTP server.
 * 
 * This test does not do any real JNDI nor does it test the annotation based discovery of the endpoint service. Also
 * there is no application specific {@link ClassLoader} so class loading issues will not occur.
 *
 * @author Jan Bernitt
 */
@RunWith(Parameterized.class)
public class RemoteEJBContextTest {

    @BeforeClass
    public static void setUp() {
        serverLocation = localhostWithFreePort();
        server = spawnServer(serverLocation, new RemoteEJBContextTestApplication());
    }

    @AfterClass
    public static void tearDown() {
        if (server != null) {
            server.shutdown();
        }
    }

    static class RemoteEJBContextTestApplication extends Application implements EjbOverHttpService {
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
            if (!applicationName.equals("myapp")) {
                return null;
            }
            return RemoteEJBContextTestApplication.class.getClassLoader();
        }

        @Override
        public Object getBean(String jndiName) throws NamingException {
            if (!jndiName.endsWith(RemoteBean.class.getSimpleName())) {
                throw new NamingException("No such bean: " + jndiName);
            }
            return new RemoteBeanImpl();
        }
    }

    public static URI localhostWithFreePort() {
        int port = 8080;
        try(ServerSocket ss = new ServerSocket(0)) {
            port = ss.getLocalPort();
        } catch (Exception e) {
            // try with 8080
        }
        URI baseUri = UriBuilder.fromUri("http://localhost/").port(port).build();
        return baseUri;
    }

    public static HttpServer spawnServer(URI baseUri, Application app) {
        ResourceConfig config = ResourceConfig.forApplication(app)
                .register(ObjectStreamMessageBodyReader.class)
                .register(ObjectStreamMessageBodyWriter.class)
                .register(ObjectStreamInvokeMethodMessageBodyReader.class)
                .register(JsonbInvokeMethodMessageBodyReader.class)
                .register(JsonbLookupMessageBodyReader.class);
        return GrizzlyHttpServerFactory.createHttpServer(baseUri, config);
    }

    public interface RemoteBean {

        /**
         * Most basic test, JRE types only.
         */
        boolean isEmpty(String str);

        List<? extends List<String>> genericResultType();

        /**
         * Tests collection as well as no argument methods.
         */
        HashMap<String, Object> status();

        CustomSerializableType join(String... parts);

        CustomNonSerializableType failJavaReturnType();
        
        boolean failJavaArgumentType(CustomNonSerializableType arg1);

        List<CustomSerializableType> nonPrimitiveList();
    }

    public static class RemoteBeanImpl implements RemoteBean {

        @Override
        public boolean isEmpty(String str) {
            return str.isEmpty();
        }

        @Override
        public HashMap<String, Object> status() {
            HashMap<String, Object> status = new HashMap<>();
            status.put("a", 42);
            status.put("b", new ArrayList<>(Arrays.asList(3f, 4d)));
            return status;
        }

        @Override
        public CustomSerializableType join(String... parts) {
            StringJoiner joiner = new StringJoiner(".");
            for (String part : parts) {
                joiner.add(part);
            }
            return new CustomSerializableType(joiner.toString());
        }

        @Override
        public CustomNonSerializableType failJavaReturnType() {
            CustomNonSerializableType res = new CustomNonSerializableType();
            res.setValue("Only works in JSONB");
            return res;
        }

        @Override
        public boolean failJavaArgumentType(CustomNonSerializableType arg1) {
            return true;
        }

        @Override
        public List<? extends List<String>> genericResultType() {
            return new ArrayList<List<String>>(asList(new LinkedList<>(asList("a", "b", "c"))));
        }

        @Override
        public List<CustomSerializableType> nonPrimitiveList() {
            return Arrays.asList(new CustomSerializableType("a"), new CustomSerializableType("b"));
        }
    }

    public static class CustomSerializableType implements Serializable {

        public String value;

        @JsonbCreator
        public CustomSerializableType(@JsonbProperty("value") String value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CustomSerializableType that = (CustomSerializableType) o;
            return Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }

    public static class CustomNonSerializableType {

        public String value;

        public void setValue(String value) {
            this.value = value;
        }
    }

    private static HttpServer server;
    private static URI serverLocation;

    @Parameters(name = "{0}")
    public static Iterable<SerializationType> serializationTypes() {
        return Arrays.asList(SerializationType.values());
    }

    @Parameter
    public SerializationType serializationType;
    public String mediaType;

    private Context context;

    @Before
    public void setupContext() {
        mediaType = serializationType.getMediaType();
        Hashtable<String, Object> environment = new Hashtable<>();
        environment.put(Context.PROVIDER_URL, serverLocation.toString());
        environment.put(RemoteEJBContextFactory.JAXRS_CLIENT_SERIALIZATION, serializationType);
        context = new RemoteEJBContext(environment);
    }

    @Test
    public void remoteBeanMethodCall_Success() throws NamingException {
        RemoteBean bean = (RemoteBean) context.lookup("java:global/myapp/RemoteBean");
        assertFalse(bean.isEmpty("x"));
        assertTrue(bean.isEmpty(""));
    }

    @Test
    public void remoteBeanMethodCall_SuccessComplex() throws NamingException {
        RemoteBean bean = (RemoteBean) context.lookup("java:global/myapp/RemoteBean");
        HashMap<String, Object> status = bean.status();
        assertNotNull(status);
        assertEquals(42, ((Number) status.get("a")).intValue());
        @SuppressWarnings("unchecked")
        List<Object> list = (List<Object>) status.get("b");
        assertEquals(3f, ((Number) list.get(0)).floatValue(), 0.001f);
        assertEquals(4d, ((Number) list.get(1)).doubleValue(), 0.001d);
    }

    @Test
    public void remoteBeanMethodCall_SuccessCustomType() throws NamingException {
        RemoteBean bean = (RemoteBean) context.lookup("java:global/myapp/RemoteBean");
        CustomSerializableType result = bean.join("x", "y");
        assertNotNull(result);
        assertNotNull(result.value);
        assertEquals("x.y", result.value.toString());
    }

    /**
     * Illustrates how using Java serialisation forces types use in methods requires them to be {@link Serializable}. 
     */
    @Test
    public void remoteBeanMethodCall_ErrorResultNotSerializable() throws NamingException {
        RemoteBean bean = (RemoteBean) context.lookup("java:global/myapp/RemoteBean");
        try {
            CustomNonSerializableType result = bean.failJavaReturnType();
            assertEquals(SerializationType.JSON, serializationType);
            assertNotNull(result);
            assertEquals("Only works in JSONB", result.value);
        } catch (UndeclaredThrowableException ex) {
            assertEquals(SerializationType.JAVA, serializationType);
            assertSame(NotSerializableException.class, ex.getUndeclaredThrowable().getClass());
            assertEquals("fish.payara.ejb.http.client.RemoteEJBContextTest$CustomNonSerializableType", ex.getUndeclaredThrowable().getMessage());
        }
    }

    @Test
    public void remoteBeanMethodCall_ErrorArgumentNotSerializable() throws NamingException {
        RemoteBean bean = (RemoteBean) context.lookup("java:global/myapp/RemoteBean");
        try {
            boolean result = bean.failJavaArgumentType(new CustomNonSerializableType());
            assertEquals(SerializationType.JSON, serializationType);
            assertTrue(result);
        } catch (UndeclaredThrowableException ex) {
            assertEquals(SerializationType.JAVA, serializationType);
            assertSame(NotSerializableException.class, ex.getUndeclaredThrowable().getClass());
            assertEquals("fish.payara.ejb.http.client.RemoteEJBContextTest$CustomNonSerializableType", ex.getUndeclaredThrowable().getMessage());
        }
    }

    /**
     * Illustrates the effect on generics in the return type and the difference between using Java serialisation and
     * JSONB
     */
    @Test
    public void remoteBeanMethodCall_SuccessGenerics() throws NamingException {
        RemoteBean bean = (RemoteBean) context.lookup("java:global/myapp/RemoteBean");
        List<? extends List<String>> result = bean.genericResultType();
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(3, result.get(0).size());
        assertEquals(asList("a", "b", "c"), result.get(0));
        assertEquals(ArrayList.class, result.getClass());
        if (serializationType == SerializationType.JAVA) {
            // java serialisation preserves the exact types
            assertSame(LinkedList.class, result.get(0).getClass());
        }
        if (serializationType == SerializationType.JSON) {
            // while JSONB only can reconstruct what is in the type signatures
            // we do preserve the exact non generic type of the result (ArrayList) but not the type of the element
            assertNotSame(LinkedList.class, result.get(0).getClass());
        }
    }

    @Test
    public void remoteBeanLookup_ErrorUnknownBean() {
        assertLookupError("java:global/myapp/RemoteBeanXyz", "No such bean: java:global/myapp/RemoteBeanXyz");
    }

    @Test
    public void remoteBeanLookup_ErrorUnknownApplication() {
        assertLookupError("java:global/anotherapp/RemoteBean", "Unknown application: anotherapp");
    }

    @Test
    public void remoteBeanLookup_ErrorNonGlobalName() {
        assertLookupError("java:app/myapp/RemoteBean", "Only global names are supported but got: java:app/myapp/RemoteBean");
    }

    private void assertLookupError(String jndiName, String expectedErrorMessage) {
        try {
            RemoteBean bean = (RemoteBean) context.lookup(jndiName);
            assertNull("Should have failed", bean);
        } catch (NamingException ex) {
            assertEquals(expectedErrorMessage, ex.getMessage());
        }
    }

    @Test
    public void remoteBeanMethodCall_SuccessPojoList() throws NamingException {
        RemoteBean bean = (RemoteBean) context.lookup("java:global/myapp/RemoteBean");
        List<CustomSerializableType> result = bean.nonPrimitiveList();
        assertNotNull(result);
        assertEquals(2, result.size());
        assertThat(result, CoreMatchers.hasItems(new CustomSerializableType("a"), new CustomSerializableType("b")));
    }
}

package fish.payara.ejb.http.client;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.NotSerializableException;
import java.io.Serializable;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.ServerSocket;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.json.bind.annotation.JsonbCreator;
import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbPropertyOrder;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
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

        CustomNonSerializableType failJava();
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
        public CustomNonSerializableType failJava() {
            CustomNonSerializableType res = new CustomNonSerializableType();
            res.setValue("Only works in JSONB");
            return res;
        }

        @Override
        public List<? extends List<String>> genericResultType() {
            return new ArrayList<List<String>>(asList(new LinkedList<>(asList("a", "b", "c"))));
        }
    }

    public static class CustomSerializableType implements Serializable {

        public String value;

        @JsonbCreator
        public CustomSerializableType(@JsonbProperty("value") String value) {
            this.value = value;
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
    public static Iterable<String> mediaTypes() {
        return Arrays.asList(SerializationType.values()).stream().map(item -> item.name()).collect(Collectors.toList());
    }

    /**
     * The {@link MediaType} currently tested
     */
    @Parameter
    public String serializationType;
    public String mediaType;

    private Context context;

    @Before
    public void setupContext() {
        mediaType = SerializationType.valueOf(serializationType).getMediaType();
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

    @Test
    public void remoteBeanMethodCall_ErrorNotSerializable() throws NamingException {
        RemoteBean bean = (RemoteBean) context.lookup("java:global/myapp/RemoteBean");
        try {
            CustomNonSerializableType result = bean.failJava();
            assertEquals(SerializationType.JSON.toString(), serializationType);
            assertNotNull(result);
            assertEquals("Only works in JSONB", result.value);
        } catch (UndeclaredThrowableException ex) {
            assertEquals(SerializationType.JAVA.toString(), serializationType);
            assertSame(NotSerializableException.class, ex.getUndeclaredThrowable().getClass());
            assertEquals("fish.payara.ejb.http.client.RemoteEJBContextTest$CustomNonSerializableType", ex.getUndeclaredThrowable().getMessage());
        }
    }

    @Test
    public void remoteBeanMethodCall_SuccessGenerics() throws NamingException {
        RemoteBean bean = (RemoteBean) context.lookup("java:global/myapp/RemoteBean");
        List<? extends List<String>> result = bean.genericResultType();
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(3, result.get(0).size());
        assertEquals(asList("a", "b", "c"), result.get(0));
        assertEquals(ArrayList.class, result.getClass());
        if (serializationType.equalsIgnoreCase(SerializationType.JAVA.toString())) {
            assertSame(LinkedList.class, result.get(0).getClass());
        }
        if (serializationType.equalsIgnoreCase(SerializationType.JSON.toString())) {
            assertNotSame(LinkedList.class, result.get(0).getClass());
        }
    }
}

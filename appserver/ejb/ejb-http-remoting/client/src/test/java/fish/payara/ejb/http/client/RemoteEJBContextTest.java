package fish.payara.ejb.http.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.ServerSocket;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

        boolean isEmpty(String str);

        HashMap<String, Object> status();
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
}

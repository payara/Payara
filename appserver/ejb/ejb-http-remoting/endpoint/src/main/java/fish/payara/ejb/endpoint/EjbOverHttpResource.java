package fish.payara.ejb.endpoint;

import static java.util.Arrays.asList;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.Arrays;
import java.util.Base64;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.internal.api.Globals;
import org.glassfish.internal.data.ApplicationRegistry;

import com.sun.enterprise.security.ee.auth.login.ProgrammaticLogin;

import fish.payara.ejb.http.protocol.ErrorResponse;
import fish.payara.ejb.http.protocol.InvokeMethodRequest;
import fish.payara.ejb.http.protocol.InvokeMethodResponse;
import fish.payara.ejb.http.protocol.LookupRequest;
import fish.payara.ejb.http.protocol.LookupResponse;

@Path("jndi")
public class EjbOverHttpResource {

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
                return registry.get(applicationName).getAppClassLoader();
            }
        });
    }

    public EjbOverHttpResource(EjbOverHttpService backend) {
        this.service = backend;
    }

    @POST
    @Path("/lookup")
    @Produces("application/x-java-object")
    @Consumes("application/x-java-object")
    public Response lookupJavaSerialization(LookupRequest body) {
        return lookup(body, "application/x-java-object");
    }

    @POST
    @Path("/lookup")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response lookupJsonb(LookupRequest body) {
        return lookup(body, MediaType.APPLICATION_JSON);
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
    @Path("/invoke")
    @Produces("application/x-java-object")
    @Consumes("application/x-java-object")
    public Response invokeJavaSerilaization(InvokeMethodRequest body) {
        return invoke(body, "application/x-java-object");
    }

    @POST
    @Path("/invoke")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response invokeJsonb(InvokeMethodRequest body) {
        return invoke(body, MediaType.APPLICATION_JSON);
    }

    private Response invoke(InvokeMethodRequest body, String mediaType) {
        try {
            return Response
                    .status(Status.OK)
                    .type(mediaType)
                    .entity(doInvoke(body))
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
                return new LookupResponse(Class.forName(jndiName.substring(bangIndex + 1)));
            }
            // there should only be one interface otherwise plain name would not be allowed (portable names at least)
            // in fact, there can be some implementation-specific interfaces in the proxy as well
            return new LookupResponse(ejb.getClass().getInterfaces()[0]);
        });
    }

    private InvokeMethodResponse doInvoke(InvokeMethodRequest request) throws Exception {
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
            return new InvokeMethodResponse(method.invoke(ejb,
                    request.argDeserializer.deserialise(request.argValues, argActualTypes,
                            Thread.currentThread().getContextClassLoader())));
        });
    }

    private static Class<?>[] toClasses(String[] classNames) {
        return asList(classNames).stream().map(EjbOverHttpResource::toClass).toArray(Class[]::new);
    }

    private <T> T excuteInAppContext(String jndiName, EjbOperation<T> operation) throws Exception {
        if (!jndiName.startsWith("java:global/")) {
            throw new IllegalArgumentException("Only global names are supported but got: " + jndiName);
        }
        if (jndiName.indexOf('/', 12) < 0) {
            throw new IllegalArgumentException("Global name must contain application name but got: " + jndiName);
        }

        Thread currentThread = Thread.currentThread();
        String applicationName = jndiName.substring(12, jndiName.indexOf('/', 12));
        ClassLoader existingContextClassLoader = currentThread.getContextClassLoader();
        try {
            currentThread.setContextClassLoader(service.getAppClassLoader(applicationName));
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

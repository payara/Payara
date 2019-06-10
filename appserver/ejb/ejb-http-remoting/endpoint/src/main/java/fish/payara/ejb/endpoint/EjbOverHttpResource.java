package fish.payara.ejb.endpoint;

import static java.util.Arrays.asList;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Arrays;
import java.util.Base64;

import javax.naming.InitialContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
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

    @POST
    @Path("/lookup")
    public Response lookup(LookupRequest body, @Context HttpServletRequest request) {
        try {
            return Response
                    .status(Status.CREATED)
                    .type(request.getContentType())
                    .location(new URI("invoke"))
                    .entity(lookup(body))
                    .build();
        } catch (Exception e) {
            return Response
                    .status(Status.BAD_REQUEST)
                    .type(request.getContentType())
                    .entity(new ErrorResponse(e))
                    .build();
        }
    }

    @POST
    @Path("/invoke")
    public Response invoke(InvokeMethodRequest body, @Context HttpServletRequest request) {
        try {
            return Response
                    .status(Status.OK)
                    .type(request.getContentType())
                    .entity(invoke(body))
                    .build();
        } catch (Exception e) {
            return Response
                    .status(Status.BAD_REQUEST)
                    .type(request.getContentType())
                    .entity(new ErrorResponse(e))
                    .build();
        }
    }

    private static LookupResponse lookup(LookupRequest request) throws Exception {
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

    private static InvokeMethodResponse invoke(InvokeMethodRequest request) throws Exception {
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
            return new InvokeMethodResponse((Serializable) method.invoke(ejb,
                    request.argDeserializer.deserialise(request.argValues, argActualTypes,
                            Thread.currentThread().getContextClassLoader())));
        });
    }

    private static Class<?>[] toClasses(String[] classNames) {
        return asList(classNames).stream().map(EjbOverHttpResource::toClass).toArray(Class[]::new);
    }

    private static <T> T excuteInAppContext(String jndiName, EjbOperation<T> operation) throws Exception {
        if (!jndiName.startsWith("java:global/")) {
            throw new IllegalArgumentException("Only global names are supported but got: "+jndiName);
        }
        ApplicationRegistry registry = Globals.get(ApplicationRegistry.class);
        Thread currentThread = Thread.currentThread();
        String applicationName = jndiName.substring(12, jndiName.indexOf('/', 12));
        ClassLoader existingContextClassLoader = currentThread.getContextClassLoader();
        try {
            currentThread.setContextClassLoader(registry.get(applicationName).getAppClassLoader());
            Object bean = new InitialContext().lookup(jndiName);
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

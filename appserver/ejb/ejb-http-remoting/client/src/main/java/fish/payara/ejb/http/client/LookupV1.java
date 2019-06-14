package fish.payara.ejb.http.client;

import static fish.payara.ejb.http.client.RemoteEJBContextFactory.JAXRS_CLIENT_SERIALIZATION;
import static java.lang.reflect.Proxy.newProxyInstance;
import static java.security.AccessController.doPrivileged;
import static java.util.Arrays.asList;
import static javax.naming.Context.SECURITY_CREDENTIALS;
import static javax.naming.Context.SECURITY_PRINCIPAL;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.security.PrivilegedAction;
import java.util.Map;

import javax.naming.NamingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import fish.payara.ejb.http.protocol.ErrorResponse;
import fish.payara.ejb.http.protocol.InvokeMethodRequest;
import fish.payara.ejb.http.protocol.InvokeMethodResponse;
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
                new RemoteEjbOverHttpInvocationHandler(mediaType, invoke, jndiName, jndiOptions));
    }

    static final class RemoteEjbOverHttpInvocationHandler implements InvocationHandler {

        private final String mediaType;
        private final WebTarget invoke;
        private final String jndiName;
        private final Map<String, Object> jndiOptions;

        public RemoteEjbOverHttpInvocationHandler(String mediaType, WebTarget invoke, String jndiName, Map<String, Object> jndiOptions) {
            this.jndiName = jndiName;
            this.jndiOptions = jndiOptions;
            this.invoke = invoke;
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
            try (Response response = invoke.request(mediaType).buildPost(Entity.entity(request, mediaType)).invoke()) {
                if (response.getStatus() == Status.OK.getStatusCode()) {
                    return response.readEntity(InvokeMethodResponse.class).result;
                }
                ErrorResponse error = response.readEntity(ErrorResponse.class);
                throw deserialise(error);
            }
        }

        private InvokeMethodRequest createRequest(Method method, Object[] args) {
            String principal = jndiOptions.containsKey(SECURITY_PRINCIPAL)
                    ? base64Encode(jndiOptions.get(SECURITY_PRINCIPAL))
                    : "";
            String credentials = jndiOptions.containsKey(SECURITY_CREDENTIALS)
                    ? base64Encode(jndiOptions.get(SECURITY_CREDENTIALS))
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
                    throw new IllegalArgumentException("Failed to serailise method arguments for remote invocation");
                }
                argValues = bos.toByteArray();
            }
            return argValues;
        }
    }
}

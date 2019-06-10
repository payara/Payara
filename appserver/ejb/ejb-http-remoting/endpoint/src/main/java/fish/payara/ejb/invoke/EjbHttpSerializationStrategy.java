package fish.payara.ejb.invoke;

import java.io.PrintWriter;

import javax.ws.rs.core.MediaType;

public interface EjbHttpSerializationStrategy {

    /**
     * @return The {@link MediaType} this strategy is associated with for both request bodies and response bodies
     */
    String getMediaType();

    /**
     * Server => Client
     * 
     * @param data   Can be an arbitrary value returned by the endpoint to the client.
     * @param writer HTTP response's body to write the data to
     */
    void serialize(Object data, PrintWriter writer);

    /**
     * Client => Server
     * 
     * @param request The request body of an EJB method invocation over HTTP
     * @return The deserialised description of the EJB method invocation to perform
     */
    EjbMethodInvocation deserialise(EjbInvocationRequest request);

    /**
     * This is created on the boundary from the HTTP servlet request.
     */
    class EjbInvocationRequest {
        String principal;
        String credentials;
        String methodName;
        byte[] argTypes;
        byte[] argValues;
    }

    /**
     * This is created by the specific deserialisation strategy within the appropriate application context
     * {@link ClassLoader} wise
     */
    class EjbMethodInvocation {
        String methodName;
        Class<?>[] argTypes;
        Object[] argValues;
    }
}

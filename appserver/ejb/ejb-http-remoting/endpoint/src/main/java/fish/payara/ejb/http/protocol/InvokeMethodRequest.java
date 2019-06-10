package fish.payara.ejb.http.protocol;

import java.io.Serializable;

public class InvokeMethodRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    public final String principal;
    public final String credentials;

    public final String jndiName;
    public final String method;
    public final String[] argTypes;
    public final String[] argActualTypes;
    public final Object argValues;
    public transient ArgumentDeserializer argDeserializer;

    public InvokeMethodRequest(String principal, String credentials, String jndiName, String method, String[] argTypes,
            String[] argActualTypes, Object argValues, ArgumentDeserializer argDeserializer) {
        this.principal = principal;
        this.credentials = credentials;
        this.jndiName = jndiName;
        this.method = method;
        this.argTypes = argTypes;
        this.argActualTypes = argActualTypes;
        this.argValues = argValues;
        this.argDeserializer = argDeserializer;
    }

    @FunctionalInterface
    public interface ArgumentDeserializer {

        Object[] deserialise(Object args, Class<?>[] argActualTypes, ClassLoader classLoader);
    }
}

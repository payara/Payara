package fish.payara.ejb.http.protocol;

import java.io.Serializable;

public class InvokeMethodResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Actual type of the result
     */
    public final String type;
    public final Object result;

    public InvokeMethodResponse() {
        this(null, null); // for JSON
    }

    public InvokeMethodResponse(Object result) {
        this(result == null ? "" : result.getClass().getName(), result);
    }

    private InvokeMethodResponse(String type, Object result) {
        this.type = type;
        this.result = result;
    }
}

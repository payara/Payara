package fish.payara.ejb.http.protocol;

import java.io.Serializable;

public class InvokeMethodResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    public final String type;
    public final Serializable result;

    public InvokeMethodResponse(Serializable result) {
        this.type = result == null ? "" : result.getClass().getName();
        this.result = result;
    }
}

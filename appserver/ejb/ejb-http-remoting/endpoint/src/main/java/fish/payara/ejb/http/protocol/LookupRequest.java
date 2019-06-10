package fish.payara.ejb.http.protocol;

import java.io.Serializable;

public class LookupRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    public final String jndiName;

    public LookupRequest(String jndiName) {
        this.jndiName = jndiName;
    }
}

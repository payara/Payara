package fish.payara.ejb.http.protocol;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LookupRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("lookup")
    public final String jndiName;

    public LookupRequest(String jndiName) {
        this.jndiName = jndiName;
    }
}

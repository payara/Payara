package fish.payara.ejb.http.protocol;

import java.io.Serializable;

public class LookupResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    public final String typeName;
    public final String kind;

    public LookupResponse() {
        this(null, null); // for JSON
    }

    public LookupResponse(Class<?> ejbInterface) {
        this.typeName = ejbInterface.getName();
        this.kind = "Stateless"; // TODO hard coded for now until we support stateful as well
    }

    private LookupResponse(String typeName, String kind) {
        this.typeName = typeName;
        this.kind = kind;
    }

}

package fish.payara.ejb.http.protocol;

import java.io.Serializable;

public class LookupResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    public final String typeName;
    public final String kind;

    public LookupResponse(Class<?> ejbInterface) {
        this.typeName = ejbInterface.getName();
        this.kind = "Stateless"; // TODO hard coded for now until we support stateful as well
    }

}

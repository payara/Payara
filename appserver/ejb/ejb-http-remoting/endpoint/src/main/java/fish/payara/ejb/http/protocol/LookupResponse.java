package fish.payara.ejb.http.protocol;

import java.io.Serializable;

import javax.ejb.Stateful;
import javax.ejb.Stateless;

public class LookupResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    public final String typeName;
    public final String kind;

    public LookupResponse(Class<?> ejbInterface) {
        this.typeName = ejbInterface.getName();
        this.kind = ejbInterface.isAnnotationPresent(Stateful.class) 
                ? Stateful.class.getSimpleName()
                : Stateless.class.getSimpleName();
    }

}

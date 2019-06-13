package fish.payara.ejb.http.client;

import java.util.Map;

import javax.naming.NamingException;
import javax.ws.rs.client.WebTarget;

public class LookupV1 extends Lookup {

    private WebTarget v1lookup;

    LookupV1(Map<String, Object> environment, WebTarget v1lookup) {
        super(environment);
        this.v1lookup = v1lookup;
    }

    @Override
    Object lookup(String jndiName) throws NamingException {
        
        return null;
    }

}

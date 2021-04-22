package fish.payara.security.oidc.client.eltests;

import javax.inject.Named;

@Named
public class OpenidConfigBeanEL {
            
    public String getTokenEndpointURL() {
        return "http://localhost:8080/openid-server/webresources/oidc-provider";
    }
    
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.security.jaspic;

import java.util.Map;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.config.AuthConfigFactory;
import javax.security.auth.message.config.AuthConfigProvider;
import javax.security.auth.message.config.ClientAuthConfig;
import javax.security.auth.message.config.ServerAuthConfig;

/**
 *
 * @author steve
 */
public class SimpleSAMAuthConfigProvider implements AuthConfigProvider {
    
    private final Map properties;
    private Class samClass;
    
    public SimpleSAMAuthConfigProvider(Map properties, AuthConfigFactory factory, Class samClass) {
        this.properties = properties;
        this.samClass = samClass;
        if (factory != null) {
            factory.registerConfigProvider(this, "HttpServlet", null, "");
        }
    }

    @Override
    public ClientAuthConfig getClientAuthConfig(String layer, String appContext, CallbackHandler handler) throws AuthException {
        throw new AuthException("Unsupported");
    }

    @Override
    public ServerAuthConfig getServerAuthConfig(String layer, String appContext, CallbackHandler handler) throws AuthException {
        return new SimpleSAMConfig(layer, appContext, handler, properties, samClass);
    }

    @Override
    public void refresh() {
        // do nothing
    }
    
}

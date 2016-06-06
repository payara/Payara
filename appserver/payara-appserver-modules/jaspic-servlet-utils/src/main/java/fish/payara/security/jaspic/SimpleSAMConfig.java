/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.security.jaspic;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.config.ServerAuthConfig;
import javax.security.auth.message.config.ServerAuthContext;
import javax.security.auth.message.module.ServerAuthModule;

/**
 *
 * @author steve
 */
class SimpleSAMConfig implements ServerAuthConfig {
    
    private final String layer;
    private final String appContext;
    private final CallbackHandler handler;
    private final Map constructedProperties;
    private ServerAuthModule sam;
    private Class samClass;


    SimpleSAMConfig(String layer, String appContext, CallbackHandler handler, Map properties, Class samClass) {
        this.layer = layer;
        this.appContext = appContext;
        this.handler = handler;
        this.constructedProperties = properties;
        this.samClass = samClass;
        
    }

    @Override
    public ServerAuthContext getAuthContext(String authContextID, Subject serviceSubject, Map properties) throws AuthException {
        // combine constructed properties with passed in properties
        if (constructedProperties != null)
            properties.putAll(constructedProperties);
        
        if (sam == null) {
            try {
                sam = (ServerAuthModule)samClass.newInstance();
            } catch (InstantiationException | IllegalAccessException ex) {
                Logger.getLogger(SimpleSAMConfig.class.getName()).log(Level.SEVERE, null, ex);
                AuthException ae = new AuthException("Unable to instantiate an instance of the provided SAM class");
                ae.initCause(ex);
                throw ae;
            }
        }
        return new SimpleSAMAuthContext(authContextID, serviceSubject, properties, handler, sam);
    }

    @Override
    public String getMessageLayer() {
        return layer;
    }

    @Override
    public String getAppContext() {
        return appContext;
    }

    @Override
    public String getAuthContextID(MessageInfo messageInfo) {
        return null;
    }

    @Override
    public void refresh() {
    }

    @Override
    public boolean isProtected() {
        return false;
    }
    
}

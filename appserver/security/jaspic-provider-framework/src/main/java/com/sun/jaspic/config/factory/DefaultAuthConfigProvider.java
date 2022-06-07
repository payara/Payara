package com.sun.jaspic.config.factory;

import jakarta.security.auth.message.config.AuthConfigProvider;
import jakarta.security.auth.message.config.ClientAuthConfig;
import javax.security.auth.callback.CallbackHandler;
import jakarta.security.auth.message.AuthException;
import jakarta.security.auth.message.config.ServerAuthConfig;
import jakarta.security.auth.message.module.ServerAuthModule;
import jakarta.security.auth.message.config.AuthConfigFactory;


public class DefaultAuthConfigProvider implements AuthConfigProvider {

    private static final String CALLBACK_HANDLER_PROPERTY_NAME = "authconfigprovider.client.callbackhandler";
    private java.util.Map<String, String> providerProperties;
    private ServerAuthModule serverAuthModule;

    public DefaultAuthConfigProvider(ServerAuthModule serverAuthModule) {
        this.serverAuthModule = serverAuthModule;
    }

    public DefaultAuthConfigProvider(java.util.Map<String, String> properties, AuthConfigFactory factory) {
        this.providerProperties = properties;
        if(factory != null) {
            factory.registerConfigProvider(this, null, null, "Auto");
        }
    }

    @Override
    public ClientAuthConfig getClientAuthConfig(String s, String s1, CallbackHandler callbackHandler) throws AuthException {
        return null;
    }

    @Override
    public ServerAuthConfig getServerAuthConfig(String layer, String appContext, CallbackHandler handler) throws AuthException {
        return new DefaultServerAuthConfig(layer, appContext, handler == null ? createDefaultCallbackHandler() : handler,
                providerProperties, serverAuthModule);
    }

    @Override
    public void refresh() {

    }

    private CallbackHandler createDefaultCallbackHandler() throws AuthException {
        String callBackClassName = System.getProperty(CALLBACK_HANDLER_PROPERTY_NAME);

        if (callBackClassName == null) {
            throw new AuthException("No default handler set via system property: " + CALLBACK_HANDLER_PROPERTY_NAME);
        }

        try {
            return (CallbackHandler) Thread.currentThread().getContextClassLoader().loadClass(callBackClassName)
                            .getDeclaredConstructor()
                            .newInstance();
        } catch (Exception e) {
            throw new AuthException(e.getMessage());
        }
    }
}

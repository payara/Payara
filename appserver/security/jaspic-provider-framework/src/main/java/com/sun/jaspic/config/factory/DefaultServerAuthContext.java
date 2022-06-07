package com.sun.jaspic.config.factory;

import jakarta.security.auth.message.config.ServerAuthContext;
import jakarta.security.auth.message.module.ServerAuthModule;
import jakarta.security.auth.message.MessageInfo;
import jakarta.security.auth.message.AuthStatus;
import javax.security.auth.callback.CallbackHandler;
import jakarta.security.auth.message.AuthException;
import javax.security.auth.Subject;

public class DefaultServerAuthContext implements ServerAuthContext {

    private final ServerAuthModule serverAuthModule;

    public DefaultServerAuthContext(CallbackHandler handler, ServerAuthModule serverAuthModule) throws AuthException {
        this.serverAuthModule = serverAuthModule;
        serverAuthModule.initialize(null, null, handler, java.util.Collections.<String, Object> emptyMap());
    }

    @Override
    public AuthStatus validateRequest(MessageInfo messageInfo, Subject clientSubject, Subject serviceSubject) throws AuthException {
        return serverAuthModule.validateRequest(messageInfo, clientSubject, serviceSubject);
    }

    @Override
    public AuthStatus secureResponse(MessageInfo messageInfo, Subject serviceSubject) throws AuthException {
        return serverAuthModule.secureResponse(messageInfo, serviceSubject);
    }

    @Override
    public void cleanSubject(MessageInfo messageInfo, Subject subject) throws AuthException {
        serverAuthModule.cleanSubject(messageInfo, subject);
    }
}

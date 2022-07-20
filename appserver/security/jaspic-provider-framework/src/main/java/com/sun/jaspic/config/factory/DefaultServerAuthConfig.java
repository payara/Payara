/*
 * Copyright (c) 2015, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package com.sun.jaspic.config.factory;

import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import jakarta.security.auth.message.AuthException;
import jakarta.security.auth.message.MessageInfo;
import jakarta.security.auth.message.config.ServerAuthConfig;
import jakarta.security.auth.message.config.ServerAuthContext;
import jakarta.security.auth.message.module.ServerAuthModule;

/**
 * This class functions as a kind of factory for {@link ServerAuthContext} instances, which are delegates for the actual
 * {@link ServerAuthModule} (SAM) that we're after.
 * 
 * @author Arjan Tijms
 */
public class DefaultServerAuthConfig implements ServerAuthConfig {

    private String layer;
    private String appContext;
    private CallbackHandler handler;
    private Map<String, String> providerProperties;
    private ServerAuthModule serverAuthModule;

    public DefaultServerAuthConfig(String layer, String appContext, CallbackHandler handler,
        Map<String, String> providerProperties, ServerAuthModule serverAuthModule) {
        this.layer = layer;
        this.appContext = appContext;
        this.handler = handler;
        this.providerProperties = providerProperties;
        this.serverAuthModule = serverAuthModule;
    }

    @Override
    public ServerAuthContext getAuthContext(String authContextID, Subject serviceSubject,
        @SuppressWarnings("rawtypes") Map properties) throws AuthException {
        return new DefaultServerAuthContext(handler, serverAuthModule);
    }

    // ### The methods below mostly just return what has been passed into the
    // constructor.
    // ### In practice they don't seem to be called

    @Override
    public String getMessageLayer() {
        return layer;
    }

    /**
     * It's not entirely clear what the difference is between the "application context identifier" (appContext) and the
     * "authentication context identifier" (authContext). In early iterations of the specification, authContext was called
     * "operation" and instead of the MessageInfo it was obtained by something called an "authParam".
     */
    @Override
    public String getAuthContextID(MessageInfo messageInfo) {
        return appContext;
    }

    @Override
    public String getAppContext() {
        return appContext;
    }

    @Override
    public void refresh() {
    }

    @Override
    public boolean isProtected() {
        return false;
    }

    public Map<String, String> getProviderProperties() {
        return providerProperties;
    }

}

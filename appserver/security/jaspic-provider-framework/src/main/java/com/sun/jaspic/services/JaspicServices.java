/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
// Portions Copyright [2018-2021] [Payara Foundation and/or its affiliates]
package com.sun.jaspic.services;

import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import jakarta.security.auth.message.AuthException;
import jakarta.security.auth.message.MessageInfo;
import jakarta.security.auth.message.config.AuthConfig;
import jakarta.security.auth.message.config.AuthConfigFactory;
import jakarta.security.auth.message.config.AuthConfigFactory.RegistrationContext;
import jakarta.security.auth.message.config.AuthConfigProvider;
import jakarta.security.auth.message.config.ClientAuthConfig;
import jakarta.security.auth.message.config.ClientAuthContext;
import jakarta.security.auth.message.config.ServerAuthConfig;
import jakarta.security.auth.message.config.ServerAuthContext;

/**
 * This is based Helper class for 196 Configuration.
 */
public abstract class JaspicServices {

    protected static final AuthConfigFactory factory = AuthConfigFactory.getFactory();

    private ReadWriteLock readWriteLock;
    private Lock readLock;
    private Lock writeLock;

    protected String layer;
    protected String appCtxt;
    protected Map<String, Object> map;
    protected CallbackHandler callbackHandler;
    protected AuthConfigRegistrationWrapper listenerWrapper;

    protected void init(String layer, String appContext, Map<String, Object> map, CallbackHandler callbackHandler, RegistrationWrapperRemover removerDelegate) {
        this.layer = layer;
        this.appCtxt = appContext;
        this.map = map;
        this.callbackHandler = callbackHandler;
        if (this.callbackHandler == null) {
            this.callbackHandler = getCallbackHandler();
        }

        this.readWriteLock = new ReentrantReadWriteLock(true);
        this.readLock = readWriteLock.readLock();
        this.writeLock = readWriteLock.writeLock();

        listenerWrapper = new AuthConfigRegistrationWrapper(this.layer, this.appCtxt, removerDelegate);
    }

    public void setRegistrationId(String registrationId) {
        listenerWrapper.setRegistrationId(registrationId);
    }

    public AuthConfigRegistrationWrapper getRegistrationWrapper() {
        return listenerWrapper;
    }

    public void setRegistrationWrapper(AuthConfigRegistrationWrapper wrapper) {
        this.listenerWrapper = wrapper;
    }

    public AuthConfigRegistrationWrapper.AuthConfigRegistrationListener getRegistrationListener() {
        return listenerWrapper.getListener();
    }

    public void disable() {
        listenerWrapper.disable();
    }

    public Object getProperty(String key) {
        return map == null ? null : map.get(key);
    }

    public String getAppContextID() {
        return appCtxt;
    }

    public ClientAuthConfig getClientAuthConfig() throws AuthException {
        return (ClientAuthConfig) getAuthConfig(false);
    }

    public ServerAuthConfig getServerAuthConfig() throws AuthException {
        return (ServerAuthConfig) getAuthConfig(true);
    }

    public ClientAuthContext getClientAuthContext(MessageInfo info, Subject clientSubject) throws AuthException {
        ClientAuthConfig clientConfig = (ClientAuthConfig) getAuthConfig(false);
        if (clientConfig != null) {
            return clientConfig.getAuthContext(clientConfig.getAuthContextID(info), clientSubject, map);
        }

        return null;
    }

    public ServerAuthContext getServerAuthContext(MessageInfo info, Subject serviceSubject) throws AuthException {
        ServerAuthConfig serverAuthConfig = (ServerAuthConfig) getAuthConfig(true);
        if (serverAuthConfig != null) {
            return serverAuthConfig.getAuthContext(serverAuthConfig.getAuthContextID(info), serviceSubject, map);
        }

        return null;
    }

    protected AuthConfig getAuthConfig(AuthConfigProvider authConfigProvider, boolean isServer) throws AuthException {
        AuthConfig authConfig = null;

        if (authConfigProvider != null) {
            if (isServer) {
                authConfig = authConfigProvider.getServerAuthConfig(layer, appCtxt, callbackHandler);
            } else {
                authConfig = authConfigProvider.getClientAuthConfig(layer, appCtxt, callbackHandler);
            }
        }

        return authConfig;
    }

    protected AuthConfig getAuthConfig(boolean isServer) throws AuthException {

        ConfigData configData = null;
        AuthConfig authConfig = null;
        boolean disabled = false;
        AuthConfigProvider lastConfigProvider = null;

        try {
            readLock.lock();
            disabled = !listenerWrapper.isEnabled();
            if (!disabled) {
                configData = listenerWrapper.getConfigData();
                if (configData != null) {
                    authConfig = isServer ? configData.getServerConfig() : configData.getClientConfig();
                    lastConfigProvider = configData.getProvider();
                }
            }

        } finally {
            readLock.unlock();
            if (disabled || authConfig != null || (configData != null && lastConfigProvider == null)) {
                return authConfig;
            }
        }

        // d == null || (d != null && lastP != null && c == null)
        if (configData == null) {
            try {
                writeLock.lock();
                if (listenerWrapper.getConfigData() == null) {
                    AuthConfigProvider nextConfigProvider = factory.getConfigProvider(layer, appCtxt, getRegistrationListener());

                    if (nextConfigProvider != null) {
                        listenerWrapper.setConfigData(new ConfigData(nextConfigProvider, getAuthConfig(nextConfigProvider, isServer)));
                    } else {
                        listenerWrapper.setConfigData(new ConfigData());
                    }
                }
                configData = listenerWrapper.getConfigData();
            } finally {
                writeLock.unlock();
            }
        }

        return isServer ? configData.getServerConfig() : configData.getClientConfig();
    }

    /**
     * Check if there is a provider register for a given layer and appCtxt.
     */
    protected boolean hasExactMatchAuthProvider() {
        boolean exactMatch = false;

        AuthConfigProvider configProvider = factory.getConfigProvider(layer, appCtxt, null);

        if (configProvider != null) {
            for (String registrationId : factory.getRegistrationIDs(configProvider)) {
                RegistrationContext registrationContext = factory.getRegistrationContext(registrationId);
                if (layer.equals(registrationContext.getMessageLayer()) && appCtxt.equals(registrationContext.getAppContext())) {
                    exactMatch = true;
                    break;
                }
            }
        }

        return exactMatch;
    }

    /**
     * Get the callback default handler
     */
    protected CallbackHandler getCallbackHandler() {
        return null;
    }

}

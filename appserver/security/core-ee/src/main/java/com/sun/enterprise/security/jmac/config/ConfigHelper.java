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
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
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

package com.sun.enterprise.security.jmac.config;

import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.config.AuthConfig;
import javax.security.auth.message.config.AuthConfigFactory;
import javax.security.auth.message.config.AuthConfigFactory.RegistrationContext;
import javax.security.auth.message.config.AuthConfigProvider;
import javax.security.auth.message.config.ClientAuthConfig;
import javax.security.auth.message.config.ClientAuthContext;
import javax.security.auth.message.config.RegistrationListener;
import javax.security.auth.message.config.ServerAuthConfig;
import javax.security.auth.message.config.ServerAuthContext;

import org.glassfish.internal.api.Globals;

import com.sun.enterprise.security.jmac.AuthMessagePolicy;
import com.sun.enterprise.security.jmac.WebServicesDelegate;

/**
 * This is based Helper class for 196 Configuration.
 */
public abstract class ConfigHelper {

    protected static final AuthConfigFactory factory = AuthConfigFactory.getFactory();

    private ReadWriteLock readWriteLock;
    private Lock readLock;
    private Lock writeLock;

    protected String layer;
    protected String appCtxt;
    protected Map<String, ?> map;
    protected CallbackHandler callbackHandler;
    protected AuthConfigRegistrationWrapper listenerWrapper;

    protected void init(String layer, String appContext, Map<String, ?> map, CallbackHandler callbackHandler) {
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

        listenerWrapper = new AuthConfigRegistrationWrapper(this.layer, this.appCtxt);
    }

    public void setJmacProviderRegisID(String jmacProviderRegisID) {
        this.listenerWrapper.setJmacProviderRegisID(jmacProviderRegisID);
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
                    authConfig = isServer ? configData.serverConfig : configData.clientConfig;
                    lastConfigProvider = configData.provider;
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

        return isServer ? configData.serverConfig : configData.clientConfig;
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
    private CallbackHandler getCallbackHandler() {
        CallbackHandler callbackHandler = AuthMessagePolicy.getDefaultCallbackHandler();
        
        if (callbackHandler instanceof CallbackHandlerConfig) {
            ((CallbackHandlerConfig) callbackHandler).setHandlerContext(getHandlerContext(map));
        }

        return callbackHandler;
    }

    /**
     * This method is invoked by the constructor and should be overrided by subclass.
     */
    protected HandlerContext getHandlerContext(Map<String, ?> map) {
        return null;
    }

    private static class ConfigData {

        private AuthConfigProvider provider;
        private AuthConfig serverConfig;
        private AuthConfig clientConfig;

        ConfigData() {
        }

        ConfigData(AuthConfigProvider authConfigProvider, AuthConfig authConfig) {
            provider = authConfigProvider;
            
            if (authConfig == null) {
                serverConfig = null;
                clientConfig = null;
            } else if (authConfig instanceof ServerAuthConfig) {
                serverConfig = authConfig;
            } else if (authConfig instanceof ClientAuthConfig) {
                clientConfig = authConfig;
            } else {
                throw new IllegalArgumentException();
            }
        }
    }

    // Adding extra inner class because specializing the Linstener Impl class would
    // make the GF 196 implementation Non-Replaceable.
    // This class would hold a RegistrationListener within.
    public static class AuthConfigRegistrationWrapper {

        private String layer;
        private String appCtxt;
        private String jmacProviderRegisID;
        private boolean enabled;
        private ConfigData data;

        private Lock wLock;
        private ReadWriteLock rwLock;

        private AuthConfigRegistrationListener listener;
        private int referenceCount = 1;
        private WebServicesDelegate delegate = null;

        public AuthConfigRegistrationWrapper(String layer, String appCtxt) {
            this.layer = layer;
            this.appCtxt = appCtxt;
            this.rwLock = new ReentrantReadWriteLock(true);
            this.wLock = rwLock.writeLock();
            
            enabled = factory != null;
            listener = new AuthConfigRegistrationListener(layer, appCtxt);
            
            if (Globals.getDefaultHabitat() != null) {
                delegate = Globals.get(WebServicesDelegate.class);
            } else {
                try {
                    // For non HK2 environments
                    // Try to get WebServicesDelegateImpl by reflection.
                    delegate = (WebServicesDelegate) Thread.currentThread()
                                                           .getContextClassLoader()
                                                           .loadClass("com.sun.enterprise.security.webservices.WebServicesDelegateImpl")
                                                           .newInstance();
                    
                } catch (InstantiationException | IllegalAccessException | ClassNotFoundException ex) {
                }
            }
        }

        public AuthConfigRegistrationListener getListener() {
            return listener;
        }

        public void setListener(AuthConfigRegistrationListener listener) {
            this.listener = listener;
        }

        public void disable() {
            this.wLock.lock();
            
            try {
                setEnabled(false);
            } finally {
                this.wLock.unlock();
                data = null;
            }
            
            if (factory != null) {
                factory.detachListener(this.listener, layer, appCtxt);
                if (getJmacProviderRegisID() != null) {
                    factory.removeRegistration(getJmacProviderRegisID());
                }
            }
        }

        // detach the listener, but dont remove-registration
        public void disableWithRefCount() {
            if (referenceCount <= 1) {
                disable();
                if (delegate != null) {
                    delegate.removeListener(this);
                }
            } else {
                try {
                    this.wLock.lock();
                    referenceCount--;
                } finally {
                    this.wLock.unlock();
                }

            }
        }

        public void incrementReference() {
            try {
                this.wLock.lock();
                referenceCount++;
            } finally {
                this.wLock.unlock();
            }
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getJmacProviderRegisID() {
            return this.jmacProviderRegisID;
        }

        public void setJmacProviderRegisID(String jmacProviderRegisID) {
            this.jmacProviderRegisID = jmacProviderRegisID;
        }

        private ConfigData getConfigData() {
            return data;
        }

        private void setConfigData(ConfigData data) {
            this.data = data;
        }

        public class AuthConfigRegistrationListener implements RegistrationListener {

            private String layer;
            private String appCtxt;

            public AuthConfigRegistrationListener(String layer, String appCtxt) {
                this.layer = layer;
                this.appCtxt = appCtxt;
            }

            @Override
            public void notify(String layer, String appContext) {
                if (this.layer.equals(layer) && ((this.appCtxt == null && appContext == null) || (appContext != null && appContext.equals(this.appCtxt)))) {
                    try {
                        wLock.lock();
                        data = null;
                    } finally {
                        wLock.unlock();
                    }
                }
            }

        }
    }

}

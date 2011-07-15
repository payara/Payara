/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jaspic.config.helper;

import com.sun.jaspic.config.delegate.MessagePolicyDelegate;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.config.AuthConfigFactory;
import javax.security.auth.message.config.AuthConfigFactory.RegistrationContext;
import javax.security.auth.message.config.AuthConfigProvider;
import javax.security.auth.message.config.ClientAuthConfig;
import javax.security.auth.message.config.ServerAuthConfig;

/**
 *
 * @author Ron Monzillo
 */
public abstract class AuthConfigProviderHelper implements AuthConfigProvider {

    public static final String LAYER_NAME_KEY = "message.layer";
    public static final String ALL_LAYERS = "*";
    public static final String LOGGER_NAME_KEY = "logger.name";
    public static final String AUTH_MODULE_KEY = "auth.module.type";
    public static final String SERVER_AUTH_MODULE = "server.auth.module";
    public static final String CLIENT_AUTH_MODULE = "client.auth.module";
    private ReentrantReadWriteLock instanceReadWriteLock = new ReentrantReadWriteLock();
    private Lock rLock = instanceReadWriteLock.readLock();
    private Lock wLock = instanceReadWriteLock.writeLock();
    HashSet<String> selfRegistered;
    EpochCarrier epochCarrier;

    protected AuthConfigProviderHelper() {
        selfRegistered = new HashSet<String>();
        epochCarrier = new EpochCarrier();
    }

    protected final String getProperty(String key, String defaultValue) {
        String rvalue = defaultValue;
        Map<String, ?> properties = getProperties();
        if (properties != null && properties.containsKey(key)) {
            rvalue = (String) properties.get(key);
        }
        return rvalue;
    }

    protected String getLayer() {
        return getProperty(LAYER_NAME_KEY, ALL_LAYERS);
    }

    protected Class[] getModuleTypes() {
        Class[] rvalue = new Class[]{
            javax.security.auth.message.module.ServerAuthModule.class,
            javax.security.auth.message.module.ClientAuthModule.class
        };
        Map<String, ?> properties = getProperties();
        if (properties.containsKey(AUTH_MODULE_KEY)) {
            String keyValue = (String) properties.get(AUTH_MODULE_KEY);
            if (SERVER_AUTH_MODULE.equals(keyValue)) {
                rvalue = new Class[]{
                            javax.security.auth.message.module.ServerAuthModule.class
                        };
            } else if (CLIENT_AUTH_MODULE.equals(keyValue)) {
                rvalue = new Class[]{
                            javax.security.auth.message.module.ClientAuthModule.class
                        };
            }
        }
        return rvalue;
    }

    protected void oldSelfRegister() {
        if (getFactory() != null) {
            selfRegistered.clear();
            RegistrationContext[] contexts = getSelfRegistrationContexts();
            for (RegistrationContext r : contexts) {
                String id = getFactory().registerConfigProvider(this,
                        r.getMessageLayer(), r.getAppContext(),
                        r.getDescription());
                selfRegistered.add(id);
            }
        }
    }

    protected void selfRegister() {
        if (getFactory() != null) {
            wLock.lock();
            try {
                RegistrationContext[] contexts = getSelfRegistrationContexts();
                if (!selfRegistered.isEmpty()) {
                    HashSet<String> toBeUnregistered = new HashSet<String>();
                    // get the current self-registrations
                    String[] regID = getFactory().getRegistrationIDs(this);
                    for (String i : regID) {
                        if (selfRegistered.contains(i)) {
                            RegistrationContext c = getFactory().getRegistrationContext(i);
                            if (c != null && !c.isPersistent()) {
                                toBeUnregistered.add(i);
                            }
                        }
                    }
                    // remove self-registrations that already exist and should continue
                    for (String i : toBeUnregistered) {
                        RegistrationContext r = getFactory().getRegistrationContext(i);
                        for (int j = 0; j < contexts.length; j++) {
                            if (contextsAreEqual(contexts[j], r)) {
                                toBeUnregistered.remove(i);
                                contexts[j] = null;
                            }
                        }
                    }
                    // unregister those that should not continue to exist
                    for (String i : toBeUnregistered) {
                        selfRegistered.remove(i);
                        getFactory().removeRegistration(i);
                    }
                }
                // add new self-segistrations
                for (RegistrationContext r : contexts) {
                    if (r != null) {
                        String id = getFactory().registerConfigProvider(this,
                                r.getMessageLayer(), r.getAppContext(),
                                r.getDescription());
                        selfRegistered.add(id);
                    }
                }
            } finally {
                wLock.unlock();
            }

        }
    }

    protected CallbackHandler getClientCallbackHandler(CallbackHandler cbh)
            throws AuthException {
        if (cbh == null) {
            AuthException ae = new AuthException("AuthConfigProvider does not support null Client Callbackhandler");
            ae.initCause(new UnsupportedOperationException());
            throw ae;
        }
        return cbh;
    }

    protected CallbackHandler getServerCallbackHandler(CallbackHandler cbh) throws
            AuthException {
        if (cbh == null) {
            AuthException ae = new AuthException("AuthConfigProvider does not support null Server Callbackhandler");
            ae.initCause(new UnsupportedOperationException());
            throw ae;
        }
        return cbh;
    }

    public ClientAuthConfig getClientAuthConfig(String layer, String appContext,
            CallbackHandler cbh) throws AuthException {
        return new ClientAuthConfigHelper(getLoggerName(), epochCarrier,
                getAuthContextHelper(appContext, true),
                getMessagePolicyDelegate(appContext),
                layer, appContext,
                getClientCallbackHandler(cbh));
    }

    public ServerAuthConfig getServerAuthConfig(String layer, String appContext,
            CallbackHandler cbh) throws AuthException {
        return new ServerAuthConfigHelper(getLoggerName(), epochCarrier,
                getAuthContextHelper(appContext, true),
                getMessagePolicyDelegate(appContext),
                layer, appContext,
                getServerCallbackHandler(cbh));
    }

    public boolean contextsAreEqual(RegistrationContext a, RegistrationContext b) {
        if (a == null || b == null) {
            return false;
        } else if (a.isPersistent() != b.isPersistent()) {
            return false;
        } else if (!a.getAppContext().equals(b.getAppContext())) {
            return false;
        } else if (!a.getMessageLayer().equals(b.getMessageLayer())) {
            return false;
        } else if (!a.getDescription().equals(b.getDescription())) {
            return false;
        }
        return true;
    }

    /**
     * to be called by refresh on provider subclass, and after subclass impl.
     * has reloaded its underlying configuration system.
     * Note: Spec is silent as to whether self-registrations should be reprocessed.
     */
    public void oldRefresh() {
        if (getFactory() != null) {
            String[] regID = getFactory().getRegistrationIDs(this);
            for (String i : regID) {
                if (selfRegistered.contains(i)) {
                    RegistrationContext c = getFactory().getRegistrationContext(i);
                    if (c != null && !c.isPersistent()) {
                        getFactory().removeRegistration(i);
                    }
                }
            }
        }
        epochCarrier.increment();
        selfRegister();
    }

    public void refresh() {
        epochCarrier.increment();
        selfRegister();
    }

    public String getLoggerName() {
        return getProperty(LOGGER_NAME_KEY, AuthConfigProviderHelper.class.getName());
    }

    public abstract Map<String, ?> getProperties();

    public abstract AuthConfigFactory getFactory();

    public abstract RegistrationContext[] getSelfRegistrationContexts();

    public abstract AuthContextHelper getAuthContextHelper(String appContext,
            boolean returnNullContexts) throws AuthException;

    public abstract MessagePolicyDelegate getMessagePolicyDelegate(String appContext) throws AuthException;
}

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
package com.sun.jaspic.config.helper;

import com.sun.jaspic.config.delegate.MessagePolicyDelegate;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.security.auth.callback.CallbackHandler;
import jakarta.security.auth.message.AuthException;
import jakarta.security.auth.message.config.AuthConfigFactory;
import jakarta.security.auth.message.config.AuthConfigFactory.RegistrationContext;
import jakarta.security.auth.message.config.AuthConfigProvider;
import jakarta.security.auth.message.config.ClientAuthConfig;
import jakarta.security.auth.message.config.ServerAuthConfig;
import jakarta.security.auth.message.module.ClientAuthModule;
import jakarta.security.auth.message.module.ServerAuthModule;

/**
 *
 * @author Ron Monzillo
 */
public abstract class BaseAuthConfigProvider implements AuthConfigProvider {

    public static final String LAYER_NAME_KEY = "message.layer";
    public static final String ALL_LAYERS = "*";
    public static final String LOGGER_NAME_KEY = "logger.name";
    public static final String AUTH_MODULE_KEY = "auth.module.type";
    public static final String SERVER_AUTH_MODULE = "server.auth.module";
    public static final String CLIENT_AUTH_MODULE = "client.auth.module";

    private ReentrantReadWriteLock instanceReadWriteLock = new ReentrantReadWriteLock();
    private Lock writeLock = instanceReadWriteLock.writeLock();
    private HashSet<String> selfRegistered = new HashSet<>();
    private EpochCarrier epochCarrier = new EpochCarrier();

    @Override
    public ClientAuthConfig getClientAuthConfig(String layer, String appContext, CallbackHandler callbackHandler) throws AuthException {
        return new ClientAuthConfigImpl(
                getLoggerName(),
                epochCarrier,
                getAuthContextHelper(appContext, true),
                getMessagePolicyDelegate(appContext),
                layer,
                appContext,
                getClientCallbackHandler(callbackHandler));
    }

    @Override
    public ServerAuthConfig getServerAuthConfig(String layer, String appContext, CallbackHandler callbackHandler) throws AuthException {
        return new ServerAuthConfigImpl(
                getLoggerName(),
                epochCarrier,
                getAuthContextHelper(appContext, true),
                getMessagePolicyDelegate(appContext),
                layer,
                appContext,
                getServerCallbackHandler(callbackHandler));
    }

    public boolean contextsAreEqual(RegistrationContext context1, RegistrationContext context2) {
        if (context1 == null || context2 == null) {
            return false;
        }

        if (context1.isPersistent() != context2.isPersistent()) {
            return false;
        }

        if (!context1.getAppContext().equals(context2.getAppContext())) {
            return false;
        }

        if (!context1.getMessageLayer().equals(context2.getMessageLayer())) {
            return false;
        }

        if (!context1.getDescription().equals(context2.getDescription())) {
            return false;
        }

        return true;
    }

    @Override
    public void refresh() {
        epochCarrier.increment();
        selfRegister();
    }

    public String getLoggerName() {
        return getProperty(LOGGER_NAME_KEY, BaseAuthConfigProvider.class.getName());
    }

    protected final String getProperty(String key, String defaultValue) {
        Map<String, ?> properties = getProperties();
        if (properties != null && properties.containsKey(key)) {
            return (String) properties.get(key);
        }

        return defaultValue;
    }

    protected String getLayer() {
        return getProperty(LAYER_NAME_KEY, ALL_LAYERS);
    }

    protected Class<?>[] getModuleTypes() {
        Class<?>[] moduleTypes = new Class[] { ServerAuthModule.class, ClientAuthModule.class };

        Map<String, ?> properties = getProperties();
        if (properties.containsKey(AUTH_MODULE_KEY)) {
            String keyValue = (String) properties.get(AUTH_MODULE_KEY);

            if (SERVER_AUTH_MODULE.equals(keyValue)) {
                moduleTypes = new Class[] { ServerAuthModule.class };
            } else if (CLIENT_AUTH_MODULE.equals(keyValue)) {
                moduleTypes = new Class[] { ClientAuthModule.class };
            }
        }

        return moduleTypes;
    }

    protected void selfRegister() {
        if (getFactory() != null) {
            writeLock.lock();
            try {
                RegistrationContext[] contexts = getSelfRegistrationContexts();
                if (!selfRegistered.isEmpty()) {
                    HashSet<String> toBeUnregistered = new HashSet<String>();

                    // Get the current self-registrations
                    String[] registrationIDs = getFactory().getRegistrationIDs(this);

                    for (String registrationId : registrationIDs) {
                        if (selfRegistered.contains(registrationId)) {
                            RegistrationContext context = getFactory().getRegistrationContext(registrationId);
                            if (context != null && !context.isPersistent()) {
                                toBeUnregistered.add(registrationId);
                            }
                        }
                    }

                    // Remove self-registrations that already exist and should continue
                    for (String registrationId : toBeUnregistered) {
                        RegistrationContext context = getFactory().getRegistrationContext(registrationId);
                        for (int j = 0; j < contexts.length; j++) {
                            if (contextsAreEqual(contexts[j], context)) {
                                toBeUnregistered.remove(registrationId);
                                contexts[j] = null;
                            }
                        }
                    }

                    // Unregister those that should not continue to exist
                    for (String registrationId : toBeUnregistered) {
                        selfRegistered.remove(registrationId);
                        getFactory().removeRegistration(registrationId);
                    }
                }

                // Add new self-segistrations
                for (RegistrationContext context : contexts) {
                    if (context != null) {
                        String id = getFactory().registerConfigProvider(this, context.getMessageLayer(), context.getAppContext(),
                                context.getDescription());
                        selfRegistered.add(id);
                    }
                }
            } finally {
                writeLock.unlock();
            }

        }
    }

    protected CallbackHandler getClientCallbackHandler(CallbackHandler callbackHandler) throws AuthException {
        if (callbackHandler == null) {
            throw (AuthException) new AuthException("AuthConfigProvider does not support null Client Callbackhandler")
                    .initCause(new UnsupportedOperationException());
        }

        return callbackHandler;
    }

    protected CallbackHandler getServerCallbackHandler(CallbackHandler callbackHandler) throws AuthException {
        if (callbackHandler == null) {
            throw (AuthException) new AuthException("AuthConfigProvider does not support null Server Callbackhandler")
                    .initCause(new UnsupportedOperationException());
        }

        return callbackHandler;
    }

    public abstract Map<String, ?> getProperties();

    public abstract AuthConfigFactory getFactory();

    public abstract RegistrationContext[] getSelfRegistrationContexts();

    public abstract BaseAuthContextImpl getAuthContextHelper(String appContext, boolean returnNullContexts) throws AuthException;

    public abstract MessagePolicyDelegate getMessagePolicyDelegate(String appContext) throws AuthException;

}

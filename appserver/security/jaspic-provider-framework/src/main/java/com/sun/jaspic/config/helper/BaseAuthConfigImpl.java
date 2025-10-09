/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

import static java.util.logging.Level.FINE;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import jakarta.security.auth.message.AuthException;
import jakarta.security.auth.message.MessageInfo;

import com.sun.jaspic.config.delegate.MessagePolicyDelegate;

/**
 * Base class for the {@link ClientAuthConfigImpl} and {@link ServerAuthConfigImpl}.
 *
 * @author Ron Monzillo
 */
public abstract class BaseAuthConfigImpl {

    String loggerName;
    EpochCarrier providerEpoch;
    long epoch;
    MessagePolicyDelegate policyDelegate;
    String layer;
    String appContext;
    CallbackHandler callbackHandler;

    private ReentrantReadWriteLock instanceReadWriteLock = new ReentrantReadWriteLock();
    private Lock instanceReadLock = instanceReadWriteLock.readLock();
    private Lock instanceWriteLock = instanceReadWriteLock.writeLock();

    public BaseAuthConfigImpl(String loggerName, EpochCarrier providerEpoch, MessagePolicyDelegate mpDelegate, String layer,
            String appContext, CallbackHandler cbh) throws AuthException {
        this.loggerName = loggerName;
        this.providerEpoch = providerEpoch;
        this.policyDelegate = mpDelegate;
        this.layer = layer;
        this.appContext = appContext;
        this.callbackHandler = cbh;

        initialize();
    }

    public String getMessageLayer() {
        return layer;
    }

    public String getAppContext() {
        return appContext;
    }

    public String getAuthContextID(MessageInfo messageInfo) {
        return policyDelegate.getAuthContextID(messageInfo);
    }

    public void refresh() {
        try {
            initialize();
        } catch (AuthException ae) {
            throw new RuntimeException(ae);
        }
    }

    private void initialize() throws AuthException {
        instanceWriteLock.lock();
        try {
            this.epoch = providerEpoch.getEpoch();
            initializeContextMap();
        } finally {
            instanceWriteLock.unlock();
        }
    }

    private void doRefreshIfNeeded() {
        boolean hasChanged = false;
        instanceReadLock.lock();
        try {
            hasChanged = providerEpoch.hasChanged(epoch);
        } finally {
            instanceReadLock.unlock();
        }

        if (hasChanged) {
            refresh();
        }
    }

    private Integer getHashCode(Map<String, ?> properties) {
        if (properties == null) {
            return Integer.valueOf("0");
        }

        return Integer.valueOf(properties.hashCode());
    }

    private <M> M getContextFromMap(Map<String, Map<Integer, M>> contextMap, String authContextID, Map<String, Object> properties) {
        M context = null;

        Map<Integer, M> internalMap = contextMap.get(authContextID);
        if (internalMap != null) {
            context = internalMap.get(getHashCode(properties));
        }

        if (context != null) {
            if (isLoggable(FINE)) {
                logIfLevel(FINE, null, "AuthContextID found in Map: ", authContextID);
            }
        }

        return context;
    }

    @SuppressWarnings("unchecked")
    protected final <M> M getContext(Map<String, Map<Integer, M>> contextMap, String authContextID, Subject subject,
            Map<String, Object> properties)
            throws AuthException {

        M context = null;

        doRefreshIfNeeded();

        instanceReadLock.lock();
        try {
            context = getContextFromMap(contextMap, authContextID, properties);
            if (context != null) {
                return context;
            }
        } finally {
            instanceReadLock.unlock();
        }

        instanceWriteLock.lock();
        try {
            context = getContextFromMap(contextMap, authContextID, properties);
            if (context == null) {

                context = (M) createAuthContext(authContextID, properties);

                Map<Integer, M> internalMap = contextMap.get(authContextID);
                if (internalMap == null) {
                    internalMap = new HashMap<Integer, M>();
                    contextMap.put(authContextID, internalMap);
                }

                internalMap.put(getHashCode(properties), context);
            }
            return context;
        } finally {
            instanceWriteLock.unlock();
        }
    }

    protected boolean isLoggable(Level level) {
        return Logger.getLogger(loggerName).isLoggable(level);
    }

    protected void logIfLevel(Level level, Throwable t, String... msgParts) {
        Logger logger = Logger.getLogger(loggerName);

        if (logger.isLoggable(level)) {
            StringBuilder messageBuffer = new StringBuilder("");

            for (String m : msgParts) {
                messageBuffer.append(m);
            }

            String msg = messageBuffer.toString();

            if (!msg.isEmpty() && t != null) {
                logger.log(level, msg, t);
            } else if (!msg.isEmpty()) {
                logger.log(level, msg);
            }
        }
    }

    protected void checkMessageTypes(Class<?>[] supportedMessageTypes) throws AuthException {
        Class<?>[] requiredMessageTypes = policyDelegate.getMessageTypes();
        for (Class<?> requiredType : requiredMessageTypes) {
            boolean supported = false;
            for (Class<?> supportedType : supportedMessageTypes) {
                if (requiredType.isAssignableFrom(supportedType)) {
                    supported = true;
                }
            }

            if (!supported) {
                throw new AuthException("module does not support message type: " + requiredType.getName());
            }
        }
    }

    /**
     * Only called from initialize (while lock is held).
     */
    protected abstract void initializeContextMap();

    protected abstract <M> M createAuthContext(String authContextID, Map<String, Object> properties) throws AuthException;
}

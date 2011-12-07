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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.MessageInfo;

/**
 *
 * @author Ron Monzillo
 */
public abstract class AuthConfigHelper {

    String loggerName;
    EpochCarrier providerEpoch;
    long epoch;
    MessagePolicyDelegate mpDelegate;
    String layer;
    String appContext;
    CallbackHandler cbh;
    private ReentrantReadWriteLock instanceReadWriteLock = new ReentrantReadWriteLock();
    private Lock instanceReadLock = instanceReadWriteLock.readLock();
    private Lock instanceWriteLock = instanceReadWriteLock.writeLock();

    public AuthConfigHelper(String loggerName, EpochCarrier providerEpoch,
            MessagePolicyDelegate mpDelegate, String layer, String appContext,
            CallbackHandler cbh) throws AuthException {

        this.loggerName = loggerName;
        this.providerEpoch = providerEpoch;
        this.mpDelegate = mpDelegate;
        this.layer = layer;
        this.appContext = appContext;
        this.cbh = cbh;
        initialize();
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

    private Integer getHashCode(Map properties) {
        if (properties == null) {
            return  Integer.valueOf("0");
        }
        return Integer.valueOf(properties.hashCode());
    }

    private <M> M getContextFromMap(HashMap<String, HashMap<Integer, M>> contextMap,
            String authContextID, Map properties) {
        M rvalue = null;
        HashMap<Integer, M> internalMap = contextMap.get(authContextID);
        if (internalMap != null) {
            rvalue = internalMap.get(getHashCode(properties));
        }
        if (rvalue != null) {
            if (isLoggable(Level.FINE)) {
                logIfLevel(Level.FINE, null, "AuthContextID found in Map: ", authContextID);
            }
        }
        return rvalue;
    }

    protected final <M> M getContext(
            HashMap<String, HashMap<Integer, M>> contextMap, String authContextID,
            Subject subject, Map properties) throws AuthException {

        M rvalue = null;

        doRefreshIfNeeded();
        instanceReadLock.lock();
        try {
            rvalue = getContextFromMap(contextMap, authContextID, properties);
            if (rvalue != null) {
                return rvalue;
            }
        } finally {
            instanceReadLock.unlock();
        }

        instanceWriteLock.lock();
        try {
            rvalue = getContextFromMap(contextMap, authContextID, properties);
            if (rvalue == null) {

                rvalue = (M) createAuthContext(authContextID, properties);

                HashMap<Integer, M> internalMap = contextMap.get(authContextID);
                if (internalMap == null) {
                    internalMap = new HashMap<Integer, M>();
                    contextMap.put(authContextID, internalMap);
                }

                internalMap.put(getHashCode(properties), rvalue);
            }
            return rvalue;
        } finally {
            instanceWriteLock.unlock();
        }
    }

    protected boolean isLoggable(Level level) {
        Logger logger = Logger.getLogger(loggerName);
        return logger.isLoggable(level);
    }

    protected void logIfLevel(Level level, Throwable t, String... msgParts) {
        Logger logger = Logger.getLogger(loggerName);
        if (logger.isLoggable(level)) {
          StringBuffer msgB = new StringBuffer("");
            for (String m : msgParts) {
                msgB.append(m);
            }
            String msg = msgB.toString();
            if ( !msg.isEmpty() && t != null) {
                logger.log(level, msg, t);
            } else if (!msg.isEmpty()) {
                logger.log(level, msg);
            }
        }
    }

    protected void checkMessageTypes(Class[] supportedMessageTypes) throws AuthException {
        Class[] requiredMessageTypes = mpDelegate.getMessageTypes();
        for (Class requiredType : requiredMessageTypes) {
            boolean supported = false;
            for (Class supportedType : supportedMessageTypes) {
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
     *  Only called from initialize (while lock is held).
     */
    protected abstract void initializeContextMap();

    protected abstract <M> M createAuthContext(final String authContextID,
            final Map properties) throws AuthException;

    public String getAppContext() {
        return appContext;
    }

    public String getAuthContextID(MessageInfo messageInfo) {
        return mpDelegate.getAuthContextID(messageInfo);
    }

    public String getMessageLayer() {
        return layer;
    }

    public void refresh() {
        try {
            initialize();
        } catch (AuthException ae) {
            throw new RuntimeException(ae);
        }
    }
}

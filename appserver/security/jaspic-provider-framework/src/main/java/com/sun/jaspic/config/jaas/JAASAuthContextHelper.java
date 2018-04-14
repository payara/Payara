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
// Portions Copyright [2018] [Payara Foundation and/or its affiliates]
package com.sun.jaspic.config.jaas;

import com.sun.jaspic.config.helper.BaseAuthContextImpl;

import static java.security.AccessController.doPrivileged;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.INFO;
import static javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag.OPTIONAL;
import static javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag.REQUIRED;
import static javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag.REQUISITE;
import static javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag.SUFFICIENT;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;

/**
 *
 * @author Ron Monzillo
 */
public class JAASAuthContextHelper extends BaseAuthContextImpl {

    private static final String DEFAULT_ENTRY_NAME = "other";
    private static final Class<?>[] PARAMS = {};
    private static final Object[] ARGS = {};

    // may be more than one delegate for a given jaas config file
    private ReentrantReadWriteLock instanceReadWriteLock = new ReentrantReadWriteLock();
    private Lock instanceWriteLock = instanceReadWriteLock.writeLock();
    private ExtendedConfigFile jaasConfig;
    private final String appContext;
    private AppConfigurationEntry[] entry;
    private Constructor<?>[] constructors;

    public JAASAuthContextHelper(String loggerName, boolean returnNullContexts, ExtendedConfigFile jaasConfig, Map<String, ?> properties,
            String appContext) throws AuthException {
        super(loggerName, returnNullContexts);
        this.jaasConfig = jaasConfig;
        this.appContext = appContext;

        initialize();
    }

    private void initialize() {
        boolean found = false;
        boolean foundDefault = false;
        instanceWriteLock.lock();
        try {
            entry = jaasConfig.getAppConfigurationEntry(appContext);
            if (entry == null) {

                // NEED TO MAKE SURE THIS LOOKUP only occurs when registered for *
                entry = jaasConfig.getAppConfigurationEntry(DEFAULT_ENTRY_NAME);
                if (entry == null) {
                    entry = new AppConfigurationEntry[0];
                } else {
                    foundDefault = true;
                }
            } else {
                found = true;
            }
            constructors = null;
        } finally {
            instanceWriteLock.unlock();
        }

        if (!found) {
            if (!foundDefault) {
                logIfLevel(INFO, null, "JAASAuthConfig no entries matched appContext (", appContext, ") or (", DEFAULT_ENTRY_NAME,
                        ")");
            } else {
                logIfLevel(INFO, null, "JAASAuthConfig appContext (", appContext, ") matched (", DEFAULT_ENTRY_NAME, ")");
            }
        }
    }

    private <M> void loadConstructors(M[] template, String authContextID) throws AuthException {
        if (constructors == null) {
            try {
                final Class moduleType = template.getClass().getComponentType();
                constructors = (Constructor[]) AccessController.doPrivileged(new java.security.PrivilegedExceptionAction() {

                    @Override
                    public Object run()
                            throws java.lang.ClassNotFoundException, java.lang.NoSuchMethodException, java.lang.InstantiationException,
                            java.lang.IllegalAccessException, java.lang.reflect.InvocationTargetException {
                        Constructor[] ctor = new Constructor[entry.length];
                        ClassLoader loader = Thread.currentThread().getContextClassLoader();
                        for (int i = 0; i < entry.length; i++) {
                            ctor[i] = null;
                            String clazz = entry[i].getLoginModuleName();
                            try {
                                Class c = Class.forName(clazz, true, loader);
                                if (moduleType.isAssignableFrom(c)) {
                                    ctor[i] = c.getConstructor(PARAMS);
                                }

                            } catch (Throwable t) {
                                logIfLevel(Level.WARNING, null, "skipping unloadable class: ", clazz, " of appCOntext: ", appContext);
                            }
                        }
                        return ctor;
                    }
                });
            } catch (java.security.PrivilegedActionException pae) {
                AuthException ae = new AuthException();
                ae.initCause(pae.getCause());
                throw ae;
            }
        }
    }

    @Override
    protected final void refresh() {
        jaasConfig.refresh();
        initialize();
    }

    /**
     * This implementation does not depend on authContextID
     * 
     * @param <M>
     * @param template
     * @param authContextID (ignored by this context system)
     * @return
     * @throws AuthException
     */
    @Override
    public <M> boolean hasModules(M[] template, String authContextID) throws AuthException {
        loadConstructors(template, authContextID);

        for (Constructor<?> constructor : constructors) {
            if (constructor != null) {
                return true;
            }
        }

        return false;
    }

    /**
     * this implementation does not depend on authContextID
     * 
     * @param <M>
     * @param template
     * @param authContextID (ignored by this context system)
     * @return
     * @throws AuthException
     */
    @Override
    public <M> M[] getModules(M[] template, String authContextID) throws AuthException {
        loadConstructors(template, authContextID);
        ArrayList<M> list = new ArrayList<M>();

        for (int i = 0; i < constructors.length; i++) {
            if (constructors[i] == null) {
                list.add(i, null);
            } else {
                final int j = i;
                try {
                    list.add(j, doPrivileged(new PrivilegedExceptionAction<M>() {

                        @Override
                        @SuppressWarnings("unchecked")
                        public M run()
                                throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
                            return (M) constructors[j].newInstance(ARGS);
                        }
                    }));
                } catch (PrivilegedActionException pae) {
                    throw (AuthException) new AuthException().initCause(pae.getCause());
                }
            }
        }

        return list.toArray(template);
    }

    @Override
    public Map<String, ?> getInitProperties(int i, Map<String, ?> properties) {
        Map<String, Object> initProperties = new HashMap<String, Object>();

        if (entry[i] != null) {
            if (properties != null && !properties.isEmpty()) {
                initProperties.putAll(properties);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> options = (Map<String, Object>) entry[i].getOptions();
            if (options != null && !options.isEmpty()) {
                initProperties.putAll(options);
            }
        }

        return initProperties;
    }

    @Override
    public boolean exitContext(AuthStatus[] successValue, int i, AuthStatus moduleStatus) {
        if (entry[i] != null && constructors[i] != null) {
            LoginModuleControlFlag flag = entry[i].getControlFlag();

            if (REQUISITE.equals(flag)) {
                for (AuthStatus authStatus : successValue) {
                    if (moduleStatus == authStatus) {
                        return false;
                    }
                }

                return true;
            } else if (SUFFICIENT.equals(flag)) {
                for (AuthStatus s : successValue) {
                    if (moduleStatus == s) {
                        return true;
                    }
                }

                return false;
            }
        }

        return false;
    }

    @Override
    public AuthStatus getReturnStatus(AuthStatus[] successValue, AuthStatus defaultFailStatus, AuthStatus[] status, int position) {
        AuthStatus returnStatus = null;

        for (int i = 0; i <= position; i++) {
            if (entry[i] != null && constructors[i] != null) {

                LoginModuleControlFlag flag = entry[i].getControlFlag();

                if (isLoggable(FINE)) {
                    logIfLevel(FINE, null, "getReturnStatus - flag: ", flag.toString());
                }

                if (flag == REQUIRED || flag == REQUISITE) {
                    boolean isSuccessValue = false;
                    for (AuthStatus authStatus : successValue) {
                        if (status[i] == authStatus) {
                            isSuccessValue = true;
                        }
                    }

                    if (isSuccessValue) {
                        if (returnStatus == null) {
                            returnStatus = status[i];
                        }
                        continue;
                    }

                    if (isLoggable(FINE)) {
                        logIfLevel(FINE, null, "ReturnStatus - REQUIRED or REQUISITE failure: ", status[i].toString());
                    }
                    return status[i];
                } else if (flag == SUFFICIENT) {
                    if (exitContext(successValue, i, status[i])) {
                        if (isLoggable(FINE)) {
                            logIfLevel(FINE, null, "ReturnStatus - Sufficient success: ", status[i].toString());
                        }

                        return status[i];
                    }

                } else if (flag == OPTIONAL) {
                    if (returnStatus == null) {
                        for (AuthStatus authStatus : successValue) {
                            if (status[i] == authStatus) {
                                returnStatus = status[i];
                            }
                        }
                    }
                }
            }
        }

        if (returnStatus != null) {
            if (isLoggable(FINE)) {
                logIfLevel(FINE, null, "ReturnStatus - result: ", returnStatus.toString());
            }

            return returnStatus;
        }

        if (isLoggable(FINE)) {
            logIfLevel(FINE, null, "ReturnStatus - Default faiure status: ", defaultFailStatus.toString());
        }

        return defaultFailStatus;
    }
}

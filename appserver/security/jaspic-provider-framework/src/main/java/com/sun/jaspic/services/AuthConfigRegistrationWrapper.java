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

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import jakarta.security.auth.message.config.RegistrationListener;

// Adding package private class because specializing the listener implementation class would
// make the Payara JASPIC (JSR 196) implementation non-replaceable.
//
// This class would hold a RegistrationListener within.
public class AuthConfigRegistrationWrapper {

    private String layer;
    private String applicationContextId;
    private String jaspicProviderRegistrationId;
    private boolean enabled;
    private ConfigData data;

    private Lock wLock;
    private ReadWriteLock rwLock;

    private AuthConfigRegistrationListener listener;
    private int referenceCount = 1;
    private RegistrationWrapperRemover removerDelegate;

    public AuthConfigRegistrationWrapper(String layer, String applicationContextId, RegistrationWrapperRemover removerDelegate) {
        this.layer = layer;
        this.applicationContextId = applicationContextId;
        this.removerDelegate = removerDelegate;
        this.rwLock = new ReentrantReadWriteLock(true);
        this.wLock = rwLock.writeLock();

        enabled = JaspicServices.factory != null;
        listener = new AuthConfigRegistrationListener(layer, applicationContextId);
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

        if (JaspicServices.factory != null) {
            JaspicServices.factory.detachListener(this.listener, layer, applicationContextId);
            if (getJaspicProviderRegistrationId() != null) {
                JaspicServices.factory.removeRegistration(getJaspicProviderRegistrationId());
            }
        }
    }

    // Detach the listener, but don't remove-registration
    public void disableWithRefCount() {
        if (referenceCount <= 1) {
            disable();
            if (removerDelegate != null) {
                removerDelegate.removeListener(this);
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

    public String getJaspicProviderRegistrationId() {
        return this.jaspicProviderRegistrationId;
    }

    public void setRegistrationId(String jaspicProviderRegistrationId) {
        this.jaspicProviderRegistrationId = jaspicProviderRegistrationId;
    }

    public ConfigData getConfigData() {
        return data;
    }

    public void setConfigData(ConfigData data) {
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
            if (this.layer.equals(layer)
                    && ((this.appCtxt == null && appContext == null) || (appContext != null && appContext.equals(this.appCtxt)))) {
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
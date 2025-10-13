/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright 2016-2024 Payara Foundation and/or its affiliates
// Payara Foundation and/or its affiliates elects to include this software in this distribution under the GPL Version 2 license
/*
 * BaseContainerCallbackHandler.java
 *
 * Created on April 21, 2004, 11:56 AM
 */

package com.sun.enterprise.security.ee.authentication.jakarta.callback;

import com.sun.enterprise.security.SecurityServicesUtil;
import com.sun.enterprise.security.ssl.SSLUtils;
import com.sun.enterprise.security.store.PasswordAdapter;
import com.sun.enterprise.server.pluggable.SecuritySupport;
import com.sun.logging.LogDomains;
import jakarta.security.auth.message.callback.SecretKeyCallback;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.SecretKey;
import org.glassfish.epicyro.config.helper.BaseCallbackHandler;
import org.glassfish.internal.api.Globals;
import org.glassfish.security.common.MasterPassword;

/**
 * Base Callback Handler for Jakarta Authentication
 *
 * @author Harpreet Singh
 * @author Shing Wai Chan
 */
abstract class BaseContainerCallbackHandler extends BaseCallbackHandler {
    
    private static final String CLIENT_SECRET_KEYSTORE = "com.sun.appserv.client.secretKeyStore";
    private static final String CLIENT_SECRET_KEYSTORE_PASSWORD = "com.sun.appserv.client.secretKeyStorePassword";

    protected final static Logger _logger = LogDomains.getLogger(BaseContainerCallbackHandler.class, LogDomains.SECURITY_LOGGER);
    
    protected final SSLUtils sslUtils;
    protected final SecuritySupport securitySupport;
    protected final MasterPassword masterPasswordHelper;

    protected BaseContainerCallbackHandler() {
        if (Globals.getDefaultHabitat() == null) {
            sslUtils = new SSLUtils();
            securitySupport = SecuritySupport.getDefaultInstance();
            masterPasswordHelper = null;
            sslUtils.postConstruct();
        } else {
            sslUtils = Globals.getDefaultHabitat().getService(SSLUtils.class);
            securitySupport = Globals.getDefaultHabitat().getService(SecuritySupport.class);
            masterPasswordHelper = Globals.getDefaultHabitat().getService(MasterPassword.class, "Security SSL Password Provider Service");
        }
    }
    
    @Override
    protected KeyStore getTrustStore() {
        return sslUtils.getMergedTrustStore();
    }

    @Override
    protected KeyStore[] getKeyStores() {
        return securitySupport.getKeyStores();
    }

    @Override
    protected PrivateKey getPrivateKeyForAlias(String alias, int keystoreIndex) throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {
        return securitySupport.getPrivateKeyForAlias(alias, keystoreIndex);
    }

    @Override
    protected PrivateKeyEntry getPrivateKeyEntryFromTokenAlias(String certNickname) throws Exception {
        return sslUtils.getPrivateKeyEntryFromTokenAlias(certNickname);
    }

    @Override
    protected SecretKey getPasswordSecretKeyForAlias(String alias) throws GeneralSecurityException {
        PasswordAdapter passwordAdapter = null;

        try {
            if (SecurityServicesUtil.getInstance().isACC()) {
                passwordAdapter = new PasswordAdapter(
                        System.getProperty(CLIENT_SECRET_KEYSTORE),
                        System.getProperty(CLIENT_SECRET_KEYSTORE_PASSWORD).toCharArray());
            } else {
                passwordAdapter = masterPasswordHelper.getMasterPasswordAdapter();
            }}
        catch (IOException e) {
            throw new GeneralSecurityException(e);
        }

        return passwordAdapter.getPasswordSecretKeyForAlias(alias);
    }

    protected void processSecretKey(SecretKeyCallback secretKeyCallback) {
        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, "Jakarta Authentication: In SecretKeyCallback Processor");
        }

        String alias = ((SecretKeyCallback.AliasRequest) secretKeyCallback.getRequest()).getAlias();
        if (alias != null) {
            try {
                PasswordAdapter passwordAdapter = null;
                if (SecurityServicesUtil.getInstance().isACC()) {
                    passwordAdapter = new PasswordAdapter(System.getProperty(CLIENT_SECRET_KEYSTORE),
                            System.getProperty(CLIENT_SECRET_KEYSTORE_PASSWORD).toCharArray());
                } else {
                    passwordAdapter = masterPasswordHelper.getMasterPasswordAdapter();
                }

                secretKeyCallback.setKey(passwordAdapter.getPasswordSecretKeyForAlias(alias));
            } catch (Exception e) {
                if (_logger.isLoggable(Level.FINE)) {
                    _logger.log(Level.FINE, "JASPIC: In SecretKeyCallback Processor: " + " Error reading key ! for alias " + alias, e);
                }
                secretKeyCallback.setKey(null);
            }
        } else {
            // Dont bother about checking for principal
            // we dont support that feature - typically
            // used in an environment with kerberos
            // Principal p = secretKeyCallback.getPrincipal();
            secretKeyCallback.setKey(null);
            if (_logger.isLoggable(Level.WARNING)) {
                _logger.log(Level.WARNING, "jaspic.unsupportreadprinciple");
            }
        }
    }
}

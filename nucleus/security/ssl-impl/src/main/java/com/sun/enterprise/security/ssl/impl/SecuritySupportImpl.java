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
// Portions Copyright [2018] [Payara Foundation and/or its affiliates]"
package com.sun.enterprise.security.ssl.impl;

import static java.lang.System.getProperty;
import static java.util.Arrays.asList;
import static java.util.Arrays.copyOf;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.FINEST;
import static java.util.logging.Level.SEVERE;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Permission;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.PropertyPermission;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;

import org.glassfish.api.admin.ProcessEnvironment;
import org.glassfish.api.admin.ProcessEnvironment.ProcessType;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.embedded.Server;
import org.glassfish.logging.annotation.LogMessageInfo;
import org.glassfish.logging.annotation.LogMessagesResourceBundle;
import org.glassfish.logging.annotation.LoggerInfo;
import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;

import com.sun.enterprise.security.ssl.manager.UnifiedX509KeyManager;
import com.sun.enterprise.security.ssl.manager.UnifiedX509TrustManager;
//V3:Commented import com.sun.enterprise.config.ConfigContext;
import com.sun.enterprise.server.pluggable.SecuritySupport;

/**
 * This implements SecuritySupport used in PluggableFeatureFactory.
 *
 * @author Shing Wai Chan
 */
// TODO: when we have two SecuritySupport implementations,
// we create Habitat we'll select which SecuritySupport implementation to use.
@Service
@Singleton
public class SecuritySupportImpl extends SecuritySupport {

    @LogMessagesResourceBundle
    public static final String SHARED_LOGMESSAGE_RESOURCE = "com.sun.enterprise.security.ssl.LogMessages";

    @LoggerInfo(subsystem = "SECURITY - SSL", description = "Security - SSL", publish = true)
    public static final String SEC_SSL_LOGGER = "javax.enterprise.system.security.ssl";

    protected static final Logger _logger = Logger.getLogger(SEC_SSL_LOGGER, SHARED_LOGMESSAGE_RESOURCE);

    @LogMessageInfo(message = "The SSL certificate has expired: {0}", level = "SEVERE", cause = "Certificate expired.", action = "Check the expiration date of the certicate.")
    private static final String SSL_CERT_EXPIRED = "NCLS-SECURITY-05054";

    private static final String DEFAULT_KEYSTORE_PASS = "changeit";
    private static final String DEFAULT_TRUSTSTORE_PASS = "changeit";
    
    private static final Map<String, List<KeyStore>> keyStores = new ConcurrentHashMap<>();
    private static final Map<String, List<KeyStore>> trustStores = new ConcurrentHashMap<>();
    private static final Map<String, List<char[]>> keyStorePasswords = new ConcurrentHashMap<>();
    private static final Map<String, List<String>> tokenNames = new ConcurrentHashMap<>();
    
    private static final String DEFAULT_MAP_KEY = "key";

    private static boolean instantiated;
    private static boolean initialized;

    private Date initDate = new Date();

    @Inject
    private ServiceLocator serviceLocator;

    @Inject
    private ProcessEnvironment processEnvironment;

    @Inject
    @Optional
    private ServerEnvironment serverEnvironment;

    private MasterPasswordImpl masterPasswordHelper;

    public SecuritySupportImpl() {
        this(true);
    }

    protected SecuritySupportImpl(boolean init) {
        if (init) {
            initJKS();
        }
    }
   

    // --- implements SecuritySupport ---
    
    /**
     * This method returns an array of keystores containing keys and certificates.
     */
    @Override
    public KeyStore[] getKeyStores() {
        List<KeyStore> keyStoresList = keyStores.get(DEFAULT_MAP_KEY);
        
        return keyStoresList.toArray(new KeyStore[keyStoresList.size()]);
    }
    
    /**
     * This method returns an array of truststores containing certificates.
     */
    @Override
    public KeyStore[] getTrustStores() {
        List<KeyStore> trustStoresList = trustStores.get(DEFAULT_MAP_KEY);
        
        return trustStoresList.toArray(new KeyStore[trustStoresList.size()]);
    }
    
    /**
     * This method returns an array of token names in order corresponding to array of keystores.
     */
    @Override
    public String[] getTokenNames() {
        List<String> tokenNamesList = tokenNames.get(DEFAULT_MAP_KEY);
        
        return tokenNamesList.toArray(new String[tokenNamesList.size()]);
    }
    
    /**
     * @param token
     * @return a keystore
     */
    @Override
    public KeyStore getKeyStore(String token) {
        int tokenIndex = getTokenIndex(token);
        if (tokenIndex < 0) {
            return null;
        }

        return keyStores.get(DEFAULT_MAP_KEY).get(tokenIndex);
    }

    /**
     * @param token
     * @return a truststore
     */
    @Override
    public KeyStore getTrustStore(String token) {
        int tokenIndex = getTokenIndex(token);
        if (tokenIndex < 0) {
            return null;
        }

        return trustStores.get(DEFAULT_MAP_KEY).get(tokenIndex);
    }
    
    @Override
    public void reset() {
        
        // Get store file names
        String keyStoreFileName = System.getProperty(keyStoreProp);
        String trustStoreFileName = System.getProperty(trustStoreProp);
        
        // Get store passwords
        char[] keyStorePass = masterPasswordHelper.getMasterPassword();
        char[] trustStorePass = keyStorePass;
        
        if (keyStorePass == null || isACC() || (serverEnvironment != null && serverEnvironment.isMicro())) {
            String keyStorePassOverride = System.getProperty(KEYSTORE_PASS_PROP, DEFAULT_KEYSTORE_PASS);
            if (keyStorePassOverride != null) {
                keyStorePass = keyStorePassOverride.toCharArray();
            }
            
            String trustStorePassOverride = System.getProperty(TRUSTSTORE_PASS_PROP, DEFAULT_TRUSTSTORE_PASS);
            if (trustStorePassOverride != null) {
                trustStorePass = trustStorePassOverride.toCharArray();
            }
        }
        
        // re-load stores
        initStores(
            keyStoreFileName,
            keyStorePass,
            trustStoreFileName,
            trustStorePass);
        
        Arrays.fill(keyStorePass, ' ');
        Arrays.fill(trustStorePass, ' ');
    }

    @Override
    public KeyStore loadNullStore(String type, int index) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        KeyStore keyStore = KeyStore.getInstance(type);
        keyStore.load(null, keyStorePasswords.get(DEFAULT_MAP_KEY).get(index));
        
        return keyStore;
    }

    @Override
    public KeyManager[] getKeyManagers(String algorithm) throws IOException, KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {
        KeyStore[] keyStores = getKeyStores();

        ArrayList<KeyManager> keyManagers = new ArrayList<KeyManager>();
        for (int i = 0; i < keyStores.length; i++) {
            checkCertificateDates(keyStores[i]);
            
            KeyManager[] keyManagersPerStore = getKeyManagerFactory(keyStores[i], keyStorePasswords.get(DEFAULT_MAP_KEY).get(i), algorithm).getKeyManagers();
            if (keyManagersPerStore != null) {
                keyManagers.addAll(asList(keyManagersPerStore));
            }
        }

        KeyManager keyManager = new UnifiedX509KeyManager(
                keyManagers.toArray(new X509KeyManager[keyManagers.size()]),
                getTokenNames());

        return new KeyManager[] { keyManager };
    }

    @Override
    public TrustManager[] getTrustManagers(String algorithm) throws IOException, KeyStoreException, NoSuchAlgorithmException {
        
        ArrayList<TrustManager> trustManagers = new ArrayList<TrustManager>();
        for (KeyStore trustStore : getTrustStores()) {
            checkCertificateDates(trustStore);
            
            TrustManager[] trustManagersPerStore = getTrustManagerFactory(trustStore, algorithm).getTrustManagers();
            if (trustManagersPerStore != null) {
                trustManagers.addAll(asList(trustManagersPerStore));
            }
        }
        
        TrustManager trustManager;
        if (trustManagers.size() == 1) {
            trustManager = trustManagers.get(0);
        } else {
            trustManager = new UnifiedX509TrustManager(trustManagers.toArray(new X509TrustManager[trustManagers.size()]));
        }
        
        return new TrustManager[] { trustManager };
    }

    @Override
    public boolean verifyMasterPassword(char[] masterPass) {
        return Arrays.equals(masterPass, keyStorePasswords.get(DEFAULT_MAP_KEY).get(0));
    }

    @Override
    public void synchronizeKeyFile(Object configContext, String fileRealmName) throws Exception {
        // throw new UnsupportedOperationException("Not supported yet in V3.");
    }

    @Override
    public PrivateKey getPrivateKeyForAlias(String alias, int keystoreIndex) throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {
        checkPermission(KEYSTORE_PASS_PROP);

        Key key = keyStores
                    .get(DEFAULT_MAP_KEY)
                    .get(keystoreIndex)
                    .getKey(
                        alias, 
                        keyStorePasswords
                            .get(DEFAULT_MAP_KEY)
                            .get(keystoreIndex));
        
        if (key instanceof PrivateKey) {
            return (PrivateKey) key;
        }

        return null;
    }

    @Override
    public void checkPermission(String key) {
        try {
            // Checking a random permission to check if it is server.
            if (isEmbeddedServer() || serviceLocator == null || isACC() || isNotServerORACC()) {
                return;
            }

            AccessController.checkPermission(new RuntimePermission("SSLPassword"));
        } catch (AccessControlException e) {
            Permission permission = new PropertyPermission(key, "read");

            String message = e.getMessage();
            if (message != null) {
                message = message.replace(e.getPermission().toString(), permission.toString());
            }

            throw new AccessControlException(message, permission);
        }
    }
    
    /**
     * @return returned index
     */
    private int getTokenIndex(String token) {
        int tokenIndex = -1;
        if (token != null) {
            tokenIndex = tokenNames.get(DEFAULT_MAP_KEY).indexOf(token);
            if (tokenIndex < 0 && _logger.isLoggable(FINEST)) {
                _logger.log(FINEST, "token {0} is not found", token);
            }
        }

        return tokenIndex;
    }
    
    public boolean isACC() {
        return processEnvironment == null ? false : processEnvironment.getProcessType().equals(ProcessType.ACC);
    }

    public boolean isNotServerORACC() {
        return processEnvironment.getProcessType().equals(ProcessType.Other);
    }
    
    private void initJKS() {
        String keyStoreFileName = System.getProperty(keyStoreProp);
        String trustStoreFileName = System.getProperty(trustStoreProp);

        char[] keyStorePass = null;
        char[] trustStorePass = null;
        if (!isInstantiated()) {
            if (serviceLocator == null) {
                serviceLocator = Globals.getDefaultHabitat();
            }

            if (masterPasswordHelper == null && serviceLocator != null) {
                masterPasswordHelper = serviceLocator.getService(MasterPasswordImpl.class);
            }

            if (masterPasswordHelper != null) {
                keyStorePass = masterPasswordHelper.getMasterPassword();
                trustStorePass = keyStorePass;
            }
        }

        if (processEnvironment == null && serviceLocator != null) {
            processEnvironment = serviceLocator.getService(ProcessEnvironment.class);
        }

        if (serverEnvironment == null && serviceLocator != null) {
            serverEnvironment = serviceLocator.getService(ServerEnvironment.class);
        }

        /*
         * If we don't have a keystore password yet check the properties. Always do so for the app client case whether the
         * passwords have been found from master password helper or not.
         */
        if (keyStorePass == null || isACC() || (serverEnvironment != null && serverEnvironment.isMicro())) {
            String keyStorePassOverride = System.getProperty(KEYSTORE_PASS_PROP, DEFAULT_KEYSTORE_PASS);
            if (keyStorePassOverride != null) {
                keyStorePass = keyStorePassOverride.toCharArray();
            }
            String trustStorePassOverride = System.getProperty(TRUSTSTORE_PASS_PROP, DEFAULT_TRUSTSTORE_PASS);
            if (trustStorePassOverride != null) {
                trustStorePass = trustStorePassOverride.toCharArray();
            }
        }

        if (!initialized) {
            initStores(
                keyStoreFileName,
                keyStorePass,
                trustStoreFileName,
                trustStorePass);
            
            Arrays.fill(keyStorePass, ' ');
            Arrays.fill(trustStorePass, ' ');
            initialized = true;
        }
    }
    
    /**
     * This method will load keystore and truststore and add into corresponding list.
     * 
     * @param keyStoreFileName
     * @param keyStorePass
     * @param trustStoreFileName
     * @param trustStorePass
     *
     */
    private static void initStores(String keyStoreFileName, char[] keyStorePass, String trustStoreFileName, char[] trustStorePass) {
        try {
            
            // Create the initial lists to store the various data items
            List<KeyStore> keyStoresList = new ArrayList<KeyStore>();
            List<KeyStore> trustStoresList = new ArrayList<KeyStore>();
            List<char[]> keyStorePasswordsList = new ArrayList<char[]>();
            List<String> tokenNamesList = new ArrayList<String>();
            
            // Add the first item to each list 
            keyStoresList.add(loadKS(getProperty(KEYSTORE_TYPE_PROP, KeyStore.getDefaultType()), null, keyStoreFileName, keyStorePass));
            trustStoresList.add(loadKS(getProperty(TRUSTSTORE_TYPE_PROP, KeyStore.getDefaultType()), null, trustStoreFileName, trustStorePass));
            keyStorePasswordsList.add(copyOf(keyStorePass, keyStorePass.length));
            tokenNamesList.add(null); // This is slightly weird, but the original code did this too.
            
            // Atomically put each list in the concurrent maps holding that list
            // Note: this can either be the first insert for when this service is first initialized, or can
            // refresh existing ones.
            keyStores.put(DEFAULT_MAP_KEY, keyStoresList);
            trustStores.put(DEFAULT_MAP_KEY, trustStoresList);
            keyStorePasswords.put(DEFAULT_MAP_KEY, keyStorePasswordsList);
            tokenNames.put(DEFAULT_MAP_KEY, tokenNamesList);
        } catch (Exception ex) {
            _logger.severe("Failed to load key stores " + ex.getMessage());
            throw new IllegalStateException(ex);
        }
        
    }
   

    /**
     * This method will load keystore and truststore and add into corresponding list.
     *
     * @param tokenName
     * @param provider
     * @param keyStorePass
     * @param keyStoreFile
     * @param keyStoreType
     * @param trustStorePass
     * @param trustStoreFile
     * @param trustStoreType
     */
    protected synchronized static void loadStores(String tokenName, Provider provider, String keyStoreFile, char[] keyStorePass, String keyStoreType, String trustStoreFile, char[] trustStorePass, String trustStoreType) {
        try {
            keyStores.get(DEFAULT_MAP_KEY).add(loadKS(keyStoreType, provider, keyStoreFile, keyStorePass));
            trustStores.get(DEFAULT_MAP_KEY).add(loadKS(trustStoreType, provider, trustStoreFile, trustStorePass));
            keyStorePasswords.get(DEFAULT_MAP_KEY).add(Arrays.copyOf(keyStorePass, keyStorePass.length));
            tokenNames.get(DEFAULT_MAP_KEY).add(tokenName);
        } catch (Exception ex) {
            _logger.severe("Failed to load key stores " + ex.getMessage());
            throw new IllegalStateException(ex);
        }
    }

    
    /**
     * This method load keystore with given keystore file and keystore password for a given keystore type and provider. It
     * always return a non-null keystore.
     *
     * @param keyStoreType
     * @param provider
     * @param keyStoreFile
     * @param keyStorePass
     *
     * @retun keystore loaded
     */
    private static KeyStore loadKS(String keyStoreType, Provider provider, String keyStoreFile, char[] keyStorePass) throws Exception {
        KeyStore keyStore = null;

        if (provider != null) {
            keyStore = KeyStore.getInstance(keyStoreType, provider);
        } else {
            keyStore = KeyStore.getInstance(keyStoreType);
        }

        if (keyStoreFile != null) {
            try (BufferedInputStream stream = new BufferedInputStream(new FileInputStream(keyStoreFile))) {

                if (_logger.isLoggable(FINE)) {
                    _logger.log(FINE, "Loading keystoreFile = {0}, keystorePass = {1}", new Object[] { keyStoreFile, keyStorePass });
                }

                keyStore.load(stream, keyStorePass);
            }
        } else {
            keyStore.load(null, keyStorePass);
        }

        return keyStore;
    }
    
    private boolean isEmbeddedServer() {
        return !Server.getServerNames().isEmpty();
    }

    private static synchronized boolean isInstantiated() {
        if (!instantiated) {
            instantiated = true;
            return false;
        }

        return true;
    }
    
    /*
     * Check X509 certificates in a store for expiration.
     */
    private void checkCertificateDates(KeyStore keyStore) throws KeyStoreException {
        Enumeration<String> aliases = keyStore.aliases();
        
        while (aliases.hasMoreElements()) {
            Certificate certificate = keyStore.getCertificate(aliases.nextElement());
            if (certificate instanceof X509Certificate) {
                if (((X509Certificate) certificate).getNotAfter().before(initDate)) {
                    _logger.log(SEVERE, SSL_CERT_EXPIRED, certificate);
                }
            }
        }
    }
    
    private TrustManagerFactory getTrustManagerFactory(KeyStore trustStore, String algorithm) throws NoSuchAlgorithmException, KeyStoreException {
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                algorithm != null ? algorithm : TrustManagerFactory.getDefaultAlgorithm());
        
        trustManagerFactory.init(trustStore);
        
        return trustManagerFactory;
    }
    
    private KeyManagerFactory getKeyManagerFactory(KeyStore keyStore, char[] keyStorePassword, String algorithm) throws NoSuchAlgorithmException, UnrecoverableKeyException, KeyStoreException {
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(
                algorithm != null ? algorithm : KeyManagerFactory.getDefaultAlgorithm());
        
        keyManagerFactory.init(keyStore, keyStorePassword);
        
        return keyManagerFactory;
    }
    
}

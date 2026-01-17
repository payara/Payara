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
// Portions Copyright [2018-2025] [Payara Foundation and/or its affiliates]
package com.sun.enterprise.security.ssl;

import static java.util.logging.Level.FINE;

import java.io.IOException;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Permission;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Enumeration;
import java.util.PropertyPermission;
import java.util.logging.Logger;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509KeyManager;

import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.internal.api.Globals;
import org.jvnet.hk2.annotations.Service;

import com.sun.enterprise.security.SecurityLoggerInfo;
import com.sun.enterprise.security.common.Util;
import com.sun.enterprise.security.integration.AppClientSSL;
import com.sun.enterprise.server.pluggable.SecuritySupport;

/**
 * Handy class containing static functions.
 *
 * @author Harpreet Singh
 * @author Vivek Nagar
 * @author Shing Wai Chan
 */
@Service
@Singleton
public final class SSLUtils implements PostConstruct {

    private static final Logger LOGGER = SecurityLoggerInfo.getLogger();

    public static final String HTTPS_OUTBOUND_KEY_ALIAS = "com.sun.enterprise.security.httpsOutboundKeyAlias";
    private static final String DEFAULT_SSL_PROTOCOL = "TLS";

    @Inject
    private SecuritySupport securitySupport;

    private boolean hasKey;
    private KeyStore mergedTrustStore;
    private AppClientSSL appclientSsl;
    private SSLContext sslContext;

    @Override
    public void postConstruct() {
        try {
            // TODO: To check the right implementation once we support EE.
            if (securitySupport == null) {
                securitySupport = SecuritySupport.getDefaultInstance();
            }

            KeyStore[] keyStores = getKeyStores();

            if (keyStores != null) {
                for (KeyStore keyStore : keyStores) {
                    Enumeration<String> aliases = keyStore.aliases();
                    while (aliases.hasMoreElements()) {
                        if (keyStore.isKeyEntry(aliases.nextElement())) {
                            hasKey = true;
                            break;
                        }
                    }
                    if (hasKey) {
                        break;
                    }
                }
            }

            mergedTrustStore = mergingTrustStores(securitySupport.getTrustStores());
            getSSLContext(null, null, null);
        } catch (Exception ex) {
            LOGGER.log(FINE, "SSLUtils static init fails.", ex);
            throw new IllegalStateException(ex);
        }
    }

    SSLContext getSSLContext(String protocol, String algorithm, String trustAlgorithm) {
        try {
            // V3:Commented to break dependency on WebTier.
            // The SSLSocketFactory CTOR will now take care of setting the kmgr and tmgr
            // SSLSocketFactory.setManagers(getKeyManagers(), getTrustManagers());

            // Creating a default SSLContext and HttpsURLConnection for clients
            // that use Https
            if (protocol == null) {
                protocol = DEFAULT_SSL_PROTOCOL;
            }

            sslContext = SSLContext.getInstance(protocol);
            String keyAlias = System.getProperty(HTTPS_OUTBOUND_KEY_ALIAS);

            KeyManager[] keyManagers = getKeyManagers(algorithm);
            if (keyAlias != null && keyAlias.length() > 0 && keyManagers != null) {
                for (int i = 0; i < keyManagers.length; i++) {
                    keyManagers[i] = new J2EEKeyManager((X509KeyManager) keyManagers[i], keyAlias);
                }
            }
            sslContext.init(keyManagers, getTrustManagers(trustAlgorithm), null);

            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());

            // See https://github.com/eclipse-ee4j/glassfish/issues/15369
            SSLContext.setDefault(sslContext);
        } catch (Exception e) {
            throw new Error(e);
        }

        return sslContext;
    }

    public boolean verifyMasterPassword(char[] masterPass) {
        return securitySupport.verifyMasterPassword(masterPass);
    }

    public KeyStore[] getKeyStores() throws IOException {
        return securitySupport.getKeyStores();
    }

    public KeyStore getKeyStore() throws IOException {
        return getKeyStores()[0];
    }

    public KeyStore[] getTrustStores() throws IOException {
        return securitySupport.getTrustStores();
    }

    public KeyStore[] getTrustStore() throws IOException {
        return getTrustStores();
    }

    /**
     * This API is for temporary purpose. It will be removed once JSR 196 is updated.
     */
    public KeyStore getMergedTrustStore() {
        return mergedTrustStore;
    }

    public KeyManager[] getKeyManagers() throws Exception {
        return getKeyManagers(null);
    }

    public KeyManager[] getKeyManagers(String algorithm) throws IOException, KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {
        return securitySupport.getKeyManagers(algorithm);
    }

    public TrustManager[] getTrustManagers() throws Exception {
        return getTrustManagers(null);
    }

    public TrustManager[] getTrustManagers(String algorithm) throws IOException, KeyStoreException, NoSuchAlgorithmException {
        return securitySupport.getTrustManagers(algorithm);
    }

    public void setAppclientSsl(AppClientSSL ssl) {
        appclientSsl = ssl;
    }

    public AppClientSSL getAppclientSsl() {
        return appclientSsl;
    }

    public static String getKeyStoreType() {
        return System.getProperty(SecuritySupport.KEYSTORE_TYPE_PROP, KeyStore.getDefaultType());
    }

    public static String getTrustStoreType() {
        return System.getProperty(SecuritySupport.TRUSTSTORE_TYPE_PROP, KeyStore.getDefaultType());
    }

    /**
     * This method checks whether a private key is available or not.
     */
    public boolean isKeyAvailable() {
        return hasKey;
    }

    /**
     * Check whether given String is of the form [&lt;TokenName&gt;:]alias where alias is an key entry.
     *
     * @param certNickname
     * @return boolean
     */
    public boolean isTokenKeyAlias(String certNickname) throws Exception {
        boolean isTokenKeyAlias = false;
        if (certNickname != null) {
            int ind = certNickname.indexOf(':');
            KeyStore[] kstores = getKeyStores();
            int count = -1;
            String aliasName = null;
            if (ind != -1) {
                String[] tokens = securitySupport.getTokenNames();
                String tokenName = certNickname.substring(0, ind);
                aliasName = certNickname.substring(ind + 1);
                for (int i = 0; i < tokens.length; i++) {
                    if (tokenName.equals(tokens[i])) {
                        count = i;
                    }
                }
            }

            if (count != -1) {
                isTokenKeyAlias = kstores[count].isKeyEntry(aliasName);
            } else {
                for (KeyStore kstore : kstores) {
                    if (kstore.isKeyEntry(certNickname)) {
                        isTokenKeyAlias = true;
                        break;
                    }
                }
            }
        }
        return isTokenKeyAlias;
    }

    /**
     * Get a PrivateKeyEntry with certNickName is of the form [&lt;TokenName&gt;:]alias where alias is an key entry.
     *
     * @param certNickname
     * @return PrivateKeyEntry
     */
    public PrivateKeyEntry getPrivateKeyEntryFromTokenAlias(String certNickname) throws Exception {
        if (System.getProperty("java.vm.specification.version").compareTo("24") < 0) {
            checkPermission(SecuritySupport.KEYSTORE_PASS_PROP);
        }
        PrivateKeyEntry privKeyEntry = null;
        if (certNickname != null) {
            int ind = certNickname.indexOf(':');
            KeyStore[] kstores = getKeyStores();
            int count = -1;
            String aliasName = certNickname;
            if (ind != -1) {
                String[] tokens = securitySupport.getTokenNames();
                String tokenName = certNickname.substring(0, ind);
                aliasName = certNickname.substring(ind + 1);
                for (int i = 0; i < tokens.length; i++) {
                    if (tokenName.equals(tokens[i])) {
                        count = i;
                    }
                }
            }

            if (count != -1 && kstores.length >= count) {
                PrivateKey privKey = securitySupport.getPrivateKeyForAlias(aliasName, count);
                if (privKey != null) {
                    Certificate[] certs = kstores[count].getCertificateChain(aliasName);
                    privKeyEntry = new PrivateKeyEntry(privKey, certs);
                }
            } else {
                for (int i = 0; i < kstores.length; i++) {
                    PrivateKey privKey = securitySupport.getPrivateKeyForAlias(aliasName, i);
                    if (privKey != null) {
                        Certificate[] certs = kstores[i].getCertificateChain(aliasName);
                        privKeyEntry = new PrivateKeyEntry(privKey, certs);
                        break;
                    }
                }
            }
        }

        return privKeyEntry;
    }

    public static void checkPermission(String key) {
        try {
            // Checking a random permission to check if it is server.
            if (Util.isEmbeddedServer() || Globals.getDefaultHabitat() == null || Util.getInstance().isACC()
                || Util.getInstance().isNotServerOrACC()) {
                return;
            }

            if (System.getProperty("java.vm.specification.version").compareTo("24") < 0) {
                AccessController.checkPermission(new RuntimePermission("SSLPassword"));
            }
        } catch (AccessControlException e) {
            String message = e.getMessage();
            Permission permission = new PropertyPermission(key, "read");
            if (message != null) {
                message = message.replace(e.getPermission().toString(), permission.toString());
            }

            throw new AccessControlException(message, permission);
        }
    }

    public String[] getSupportedCipherSuites() {
        // postConstruct is already setting this.
        return HttpsURLConnection.getDefaultSSLSocketFactory().getSupportedCipherSuites();
    }

    private KeyStore mergingTrustStores(KeyStore[] trustStores) throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
        KeyStore mergedStore;
        // Code before loading multiple keystores always had index of 0
        try {
            mergedStore = securitySupport.loadNullStore("CaseExactJKS", 0);
        } catch (KeyStoreException ex) {
            mergedStore = securitySupport.loadNullStore("JKS", 0);
        }

        String[] tokens = securitySupport.getTokenNames();
        for (int i = 0; i < trustStores.length; i++) {
            Enumeration<String> aliases = trustStores[i].aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                Certificate cert = trustStores[i].getCertificate(alias);

                // Need to preserve the token:alias name format
                String alias2 = (i < tokens.length - 1) ? tokens[i] + ":" + alias : alias;

                String alias3 = alias2;
                boolean alreadyInStore = false;
                Certificate aCert;
                int count = 1;
                while ((aCert = mergedStore.getCertificate(alias3)) != null) {
                    if (aCert.equals(cert)) {
                        alreadyInStore = true;
                        break;
                    }
                    alias3 = alias2 + "__" + count++;
                }
                if (!alreadyInStore) {
                    mergedStore.setCertificateEntry(alias3, cert);
                }
            }
        }

        return mergedStore;
    }

    /**
     *
     *
     * @param alias the admin key alias
     * @param protocol the protocol or null, uses "TLS" if this argument is null.
     * @return the SSLSocketFactory from the initialized SSLContext
     */
    public SSLSocketFactory getAdminSocketFactory(String alias, String protocol) {
        return getAdminSSLContext(alias, protocol).getSocketFactory();
    }

    /*
     * @param alias the admin key alias
     *
     * @param protocol the protocol or null, uses "TLS" if this argument is null.
     *
     * @return the initialized SSLContext
     */
    public SSLContext getAdminSSLContext(String alias, String protocol) {
        try {
            if (protocol == null) {
                protocol = "TLS";
            }

            SSLContext adminSSLContextxt = SSLContext.getInstance(protocol);

            KeyManager[] keyManagers = getKeyManagers();
            if (alias != null && alias.length() > 0 && keyManagers != null) {
                for (int i = 0; i < keyManagers.length; i++) {
                    keyManagers[i] = new J2EEKeyManager((X509KeyManager) keyManagers[i], alias);
                }
            }
            adminSSLContextxt.init(keyManagers, getTrustManagers(), null);

            return adminSSLContextxt;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}

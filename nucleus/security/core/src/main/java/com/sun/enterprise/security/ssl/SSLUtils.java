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

package com.sun.enterprise.security.ssl;

import com.sun.enterprise.security.SecurityLoggerInfo;
import com.sun.enterprise.security.common.Util;
import java.io.IOException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Enumeration;
import java.security.cert.Certificate;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509KeyManager;


//V3:Commented import com.sun.enterprise.config.clientbeans.Ssl
import com.sun.enterprise.server.pluggable.SecuritySupport;
//V3:Commented import com.sun.web.security.SSLSocketFactory;
import com.sun.enterprise.security.integration.AppClientSSL;
import java.util.logging.*;
import com.sun.logging.*;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.Permission;
import java.util.PropertyPermission;
import javax.net.ssl.SSLSocketFactory;
import org.jvnet.hk2.annotations.Service;
import javax.inject.Inject;
import org.glassfish.hk2.api.PostConstruct;

import javax.inject.Singleton;

/**
 *  Handy class containing static functions.
 * @author Harpreet Singh
 * @author Vivek Nagar
 * @author Shing Wai Chan
 */
@Service
@Singleton
public final class SSLUtils implements PostConstruct {
    private static final String DEFAULT_OUTBOUND_KEY_ALIAS = "s1as";
    public static final String HTTPS_OUTBOUND_KEY_ALIAS = "com.sun.enterprise.security.httpsOutboundKeyAlias";
    private static final String DEFAULT_SSL_PROTOCOL = "TLS";

    private static final Logger _logger = SecurityLoggerInfo.getLogger();

    @Inject
    private SecuritySupport secSupp;

    private boolean hasKey = false;
    private KeyStore mergedTrustStore = null;
    private AppClientSSL appclientSsl = null;
    private SSLContext ctx = null;
    
    public void postConstruct() {
        try {
            //TODO: To check the right implementation once we support EE.
            if(secSupp == null){
                secSupp = SecuritySupport.getDefaultInstance();
            }
            KeyStore[] keyStores = getKeyStores();
            if (keyStores != null) {
                for (KeyStore keyStore : keyStores) {
                    Enumeration aliases = keyStore.aliases();
                    while (aliases.hasMoreElements()) {
                        String alias = (String) aliases.nextElement();
                        if (keyStore.isKeyEntry(alias)) {
                            hasKey = true;
                            break;
                        }
                    }
                    if (hasKey) {
                        break;
                    }
                }
            }
            mergedTrustStore = mergingTrustStores(secSupp.getTrustStores());
            getSSLContext(null, null, null);
        } catch(Exception ex) {
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "SSLUtils static init fails.", ex);
            }
            throw new IllegalStateException(ex);
        }
    }

    SSLContext getSSLContext(String protocol, String algorithm, String trustAlgorithm) {
        try {
            //V3:Commented to break dependency on WebTier.
            //The SSLSocketFactory CTOR will now take care of setting the kmgr and tmgr
            //SSLSocketFactory.setManagers(getKeyManagers(), getTrustManagers());

            // Creating a default SSLContext and HttpsURLConnection for clients
            // that use Https
            if (protocol == null) {
                protocol = DEFAULT_SSL_PROTOCOL;
            }
            ctx = SSLContext.getInstance(protocol);
            String keyAlias = System.getProperty(HTTPS_OUTBOUND_KEY_ALIAS);
            KeyManager[] kMgrs = getKeyManagers(algorithm);
            if (keyAlias != null && keyAlias.length() > 0 && kMgrs != null) {
                for (int i = 0; i < kMgrs.length; i++) {
                    kMgrs[i] = new J2EEKeyManager((X509KeyManager)kMgrs[i], keyAlias);
                }
            }
            ctx.init(kMgrs, getTrustManagers(trustAlgorithm), null);

            HttpsURLConnection.setDefaultSSLSocketFactory(ctx.getSocketFactory());
            //refer issue :http://java.net/jira/browse/GLASSFISH-15369
            SSLContext.setDefault(ctx);
        } catch (Exception e) {
            throw new Error(e);
        }
        return ctx;
    }

    public boolean verifyMasterPassword(final char[] masterPass) {
        return secSupp.verifyMasterPassword(masterPass);
    }

    public KeyStore[] getKeyStores() throws IOException{
        return secSupp.getKeyStores();
    }

    public KeyStore getKeyStore() throws IOException{
        return getKeyStores()[0];
    }

    public KeyStore[] getTrustStores() throws IOException{
        return secSupp.getTrustStores();
    }

    public KeyStore getTrustStore() throws IOException{
        return getTrustStores()[0];
    }

    /**
     * This API is for temporary purpose.  It will be removed once JSR 196
     * is updated.
     */
    public KeyStore getMergedTrustStore() {
        return mergedTrustStore;
    }

    public KeyManager[] getKeyManagers() throws Exception{
        return getKeyManagers(null);
    }
    public KeyManager[] getKeyManagers(String algorithm) throws IOException,
            KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException{
        return secSupp.getKeyManagers(algorithm);
    } 
    public TrustManager[] getTrustManagers() throws Exception{
        return getTrustManagers(null);
    }
    public TrustManager[] getTrustManagers(String algorithm) throws IOException,
            KeyStoreException, NoSuchAlgorithmException{
        return secSupp.getTrustManagers(algorithm);
    }

    
    public void setAppclientSsl(AppClientSSL ssl){
        appclientSsl = ssl;
    }

    public  AppClientSSL getAppclientSsl() {
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
     * Check whether given String is of the form [&lt;TokenName&gt;:]alias
     * where alias is an key entry.
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
                String[] tokens = secSupp.getTokenNames();
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
     * Get a PrivateKeyEntry with certNickName is of the form
     * [&lt;TokenName&gt;:]alias where alias is an key entry.
     * @param certNickname
     * @return PrivateKeyEntry
     */ 
    public PrivateKeyEntry getPrivateKeyEntryFromTokenAlias(
            String certNickname) throws Exception {
        checkPermission(SecuritySupport.KEYSTORE_PASS_PROP);
        PrivateKeyEntry privKeyEntry = null;
        if (certNickname != null) {
            int ind = certNickname.indexOf(':');
            KeyStore[] kstores = getKeyStores();
            int count = -1;
            String aliasName = certNickname;
            if (ind != -1) {
                String[] tokens = secSupp.getTokenNames();
                String tokenName = certNickname.substring(0, ind);
                aliasName = certNickname.substring(ind + 1);
                for (int i = 0; i < tokens.length; i++) {
                    if (tokenName.equals(tokens[i])) {
                        count = i;
                    }
                }
            }

            if (count != -1 && kstores.length >= count) {
                PrivateKey privKey = secSupp.getPrivateKeyForAlias(aliasName, count);
                if (privKey != null) {
                    Certificate[] certs = kstores[count].getCertificateChain(
                            aliasName);
                    privKeyEntry = new PrivateKeyEntry(privKey, certs);
                }
            } else {
                for (int i = 0; i < kstores.length; i++) {
                    PrivateKey privKey = secSupp.getPrivateKeyForAlias(aliasName, i);
                    if (privKey != null) {
                       Certificate[] certs =
                                kstores[i].getCertificateChain(
                                aliasName);
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
            if(Util.isEmbeddedServer() || Util.getDefaultHabitat() == null
                    || Util.getInstance().isACC() || Util.getInstance().isNotServerOrACC()){
                return;
            }
            Permission perm = new RuntimePermission("SSLPassword");
            AccessController.checkPermission(perm);
        } catch (AccessControlException e) {
            String message = e.getMessage();
            Permission perm = new PropertyPermission(key, "read");
            if (message != null) {
                message = message.replace(e.getPermission().toString(), perm.toString());
            }
            throw new AccessControlException(message, perm);
        }
    }
    
    public String[] getSupportedCipherSuites() {
         //postConstruct is already setting this.
         return  HttpsURLConnection.getDefaultSSLSocketFactory().getSupportedCipherSuites();
    }
    
    private KeyStore mergingTrustStores(KeyStore[] trustStores)
            throws IOException, KeyStoreException,
            NoSuchAlgorithmException, CertificateException {
        KeyStore mergedStore;
        try {
            mergedStore = secSupp.loadNullStore("CaseExactJKS", secSupp.getKeyStores().length - 1);
        } catch(KeyStoreException ex) {
            mergedStore = secSupp.loadNullStore("JKS", secSupp.getKeyStores().length - 1);
        }

        String[] tokens = secSupp.getTokenNames();
        for (int i = 0; i < trustStores.length; i++) {
            Enumeration aliases = trustStores[i].aliases();
            while (aliases.hasMoreElements()) {
                String alias = (String)aliases.nextElement();
                Certificate cert = trustStores[i].getCertificate(alias);

                //need to preserve the token:alias name format
                String alias2 = (i < tokens.length - 1)? tokens[i] + ":" + alias : alias;

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
     * @param alias  the admin key alias
     * @param protocol the protocol or null, uses "TLS" if this argument is null.
     * @return the SSLSocketFactory from the initialized SSLContext
     */
    public SSLSocketFactory getAdminSocketFactory(String alias, String protocol) {
        return getAdminSSLContext(alias, protocol).getSocketFactory();
    }

    /*
    * @param alias  the admin key alias
    * @param protocol the protocol or null, uses "TLS" if this argument is null.
    * @return the initialized SSLContext
    */
    public SSLContext getAdminSSLContext(String alias, String protocol) {
        try {
            if (protocol == null) {
                protocol = "TLS";
            }
            SSLContext cntxt = SSLContext.getInstance(protocol);
            KeyManager[] kMgrs = getKeyManagers();
            if (alias != null && alias.length() > 0 && kMgrs != null) {
                for (int i = 0; i < kMgrs.length; i++) {
                    kMgrs[i] = new J2EEKeyManager((X509KeyManager)kMgrs[i], alias);
                }
            }
            cntxt.init(kMgrs, getTrustManagers(), null);

            return cntxt;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}

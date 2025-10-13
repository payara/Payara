/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
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
// Portions Copyright [2016-2022] [Payara Foundation and/or its affiliates]
package org.glassfish.grizzly.config;

import jakarta.inject.Provider;
import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.grizzly.config.dom.Protocol;
import org.glassfish.grizzly.config.dom.Ssl;
import org.glassfish.grizzly.config.ssl.SSLImplementation;
import org.glassfish.grizzly.config.ssl.ServerSocketFactory;
import org.glassfish.grizzly.localization.LogMessages;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.glassfish.grizzly.config.dom.Ssl.TLS12;
import static org.glassfish.grizzly.config.dom.Ssl.TLS13;

/**
 * @author oleksiys
 */
public class SSLConfigurator extends SSLEngineConfigurator {

    private static final String PLAIN_PASSWORD_PROVIDER_NAME = "plain";
    private final static Logger LOGGER = GrizzlyConfig.logger();
    /**
     * SSL settings
     */
    private final Ssl ssl;
    protected final Provider<SSLImplementation> sslImplementation;
    private String sniCertAlias;

    /**
     * Static constants for initializeSSLContext
     */
    //Keystore properties
    public static final String KEYSTORE_ATTR = "keystore";
    public static final String KEYSTORE_PROP = "javax.net.ssl.keyStore";
    public static final String KEYSTORE_TYPE_ATTR = "keystoreType";
    public static final String KEYSTORE_TYPE_PROP =  "javax.net.ssl.keyStoreType";
    public static final String KEYSTORE_PASS_ATTR = "keystorePass";
    public static final String KEYSTORE_PASS_PROP = "javax.net.ssl.keyStorePassword";
    public static final String ADDITIONAL_KEY_STORES_ATTR = "additionalKeystores";
    public static final String ADDITIONAL_KEY_STORES_PROP = "fish.payara.ssl.additionalKeyStores";

    //Truststore properties
    public static final String TRUSTSTORE_ATTR = "truststore";
    public static final String TRUSTSTORE_PROP = "javax.net.ssl.trustStore";
    public static final String ADDITIONAL_TRUST_STORES_ATTR = "additionalTrustStores";
    public static final String ADDITIONAL_TRUST_STORES_PROP = "fish.payara.ssl.additionalTrustStores";
    public static final String TRUSTSTORE_TYPE_ATTR = "truststoreType";
    public static final String TRUSTSTORE_TYPE_PROP =  "javax.net.ssl.trustStoreType";
    public static final String TRUSTSTORE_PASS_ATTR = "truststorePass";
    public static final String TRUSTSTORE_PASS_PROP = "javax.net.ssl.trustStorePassword";

    //TLS properties
    public static final String TLS_SESSION_TIMEOUT_ATTR = "tlsSessionTimeout";
    public static final String TLS_SESSION_TIMEOUT_PROP = "javax.net.ssl.sessionTimeout";
    public static final String TLS_SESSION_CACHE_SIZE_ATTR = "tlsSessionCacheSize";
    public static final String TLS_SESSION_CACHE_SIZE_PROP = "javax.net.ssl.sessionCacheSize";

    //Default value properties
    public static final String DEFAULT_KEYSTORE_TYPE = "JKS";
    public static final String DEFAULT_KEYSTORE_PASSWORD = "changeit";

    @SuppressWarnings("unchecked")
    public SSLConfigurator(final ServiceLocator habitat, final Ssl ssl) {
        this.ssl = ssl;
        
        Provider<SSLImplementation> sslImplementationLocal;
        final ServiceHandle<SSLImplementation> handle = habitat.getServiceHandle(SSLImplementation.class, ssl.getClassname());
        if (handle != null) {
            sslImplementationLocal = new Provider<SSLImplementation>() {
                @Override
                public SSLImplementation get() {
                    return handle.getService();
                }
            };

        } else {
            final SSLImplementation impl = lookupSSLImplementation(habitat, ssl);
            if (impl == null) {
                throw new IllegalStateException("Can not configure SSLImplementation");
            }
            sslImplementationLocal = new Provider<SSLImplementation>() {
                @Override
                public SSLImplementation get() {
                    return impl;
                }
            };
        }

        sslImplementation = sslImplementationLocal;
        setNeedClientAuth(isNeedClientAuth(ssl));
        setWantClientAuth(isWantClientAuth(ssl));
        clientMode = false;
        sslContextConfiguration = new InternalSSLContextConfigurator();
    }

    /**
     * Return the <code>SSLImplementation</code>
     */
    public SSLImplementation getSslImplementation() {
        return sslImplementation.get();
    }
    
    /**
     * Set the cert override
     */
    public void setSNICertAlias(String alias) {
        sniCertAlias = alias;
    }
    
    /**
     * Configures the SSL properties on the given PECoyoteConnector from the SSL config of the given HTTP listener.
     */
    protected SSLContext configureSSL() {
        SSLContext newSslContext;

        final List<String> tmpSSLArtifactsList = new LinkedList<String>();
        try {
            newSslContext = initializeSSLContext();

            if (ssl != null) {
                // client-auth
                if (Boolean.parseBoolean(ssl.getClientAuthEnabled()) || "need".equals(ssl.getClientAuth())) {
                    setNeedClientAuth(true);
                }
                // ssl protocol variants
                if (Boolean.parseBoolean(ssl.getTls12Enabled())) {
                    tmpSSLArtifactsList.add(TLS12);
                }
                if (Boolean.parseBoolean(ssl.getTls13Enabled())) {
                    tmpSSLArtifactsList.add(TLS13);
                }
                if (tmpSSLArtifactsList.isEmpty()) {
                    logEmptyWarning(ssl, "WEB0307: All SSL protocol variants disabled for network-listener {0},"
                            + " using SSL implementation specific defaults");
                } else {
                    final String[] protocols = new String[tmpSSLArtifactsList.size()];
                    tmpSSLArtifactsList.toArray(protocols);
                    setEnabledProtocols(protocols);
                }
                tmpSSLArtifactsList.clear();
                // ssl3-tls-ciphers
                final String ssl3Ciphers = ssl.getSsl3TlsCiphers();
                if (ssl3Ciphers != null && ssl3Ciphers.length() > 0) {
                    final String[] ssl3CiphersArray = ssl3Ciphers.split(",");
                    for (final String cipher : ssl3CiphersArray) {
                        tmpSSLArtifactsList.add(cipher.trim());
                    }
                }
                final String[] ciphers = getJSSECiphers(tmpSSLArtifactsList);
                if (ciphers == null || ciphers.length == 0) {
                    logEmptyWarning(ssl, "WEB0308: All SSL cipher suites disabled for network-listener(s) {0}."
                            + "  Using SSL implementation specific defaults");
                } else {
                    setEnabledCipherSuites(ciphers);
                }
            }
            
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Enabled secure protocols={0}"
                        + "" + " ciphers={1}", new Object[]
                        {Arrays.toString(getEnabledProtocols()), Arrays.toString(getEnabledCipherSuites())});
            }
            
            return newSslContext;
        } catch (Exception e) {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.log(Level.WARNING,
                        LogMessages.WARNING_GRIZZLY_CONFIG_SSL_GENERAL_CONFIG_ERROR(), e);
            }
        }

        return null;
    }

    protected SSLContext initializeSSLContext() {
        SSLContext newSslContext = null;

        try {
            final ServerSocketFactory serverSF = getSslImplementation().getServerSocketFactory();
            if (ssl != null) {
                if ("need".equals(ssl.getClientAuth()) || Boolean.parseBoolean(ssl.getClientAuthEnabled())) {
                    setAttribute(serverSF, "clientAuthNeed", "true", null, null);
                }
                if (ssl.getCrlFile() != null) {
                    setAttribute(serverSF, "crlFile", ssl.getCrlFile(), null, null);
                }
                if (ssl.getTrustAlgorithm() != null) {
                    setAttribute(serverSF, "truststoreAlgorithm", ssl.getTrustAlgorithm(), null, null);
                }
                if (ssl.getKeyAlgorithm() != null) {
                    setAttribute(serverSF, "algorithm", ssl.getKeyAlgorithm(), null, null);
                }
                setAttribute(serverSF, "trustMaxCertLength", ssl.getTrustMaxCertLength(), null, null);
                
                //key store settings
                setAttribute(serverSF, KEYSTORE_ATTR, ssl.getKeyStore(), KEYSTORE_PROP, null);
                setAttribute(serverSF, ADDITIONAL_KEY_STORES_ATTR, ssl.getKeyStore(), ADDITIONAL_KEY_STORES_PROP, null);
                setAttribute(serverSF, KEYSTORE_TYPE_ATTR, ssl.getKeyStoreType(), KEYSTORE_TYPE_PROP, DEFAULT_KEYSTORE_TYPE);
                setAttribute(serverSF, KEYSTORE_PASS_ATTR, getKeyStorePassword(ssl), KEYSTORE_PASS_PROP, DEFAULT_KEYSTORE_PASSWORD);
                
                //trust store settings
                setAttribute(serverSF, TRUSTSTORE_ATTR, ssl.getTrustStore(), TRUSTSTORE_PROP, null);
                setAttribute(serverSF, ADDITIONAL_TRUST_STORES_ATTR, ssl.getTrustStore(), ADDITIONAL_TRUST_STORES_PROP, null);
                setAttribute(serverSF, TRUSTSTORE_TYPE_ATTR, ssl.getTrustStoreType(), TRUSTSTORE_TYPE_PROP, DEFAULT_KEYSTORE_TYPE);
                setAttribute(serverSF, TRUSTSTORE_PASS_ATTR , getTrustStorePassword(ssl), TRUSTSTORE_PASS_PROP, DEFAULT_KEYSTORE_PASSWORD);
                setAttribute(serverSF, TLS_SESSION_TIMEOUT_ATTR, ssl.getTlsSessionTimeout(), TLS_SESSION_TIMEOUT_PROP, null);
                setAttribute(serverSF, TLS_SESSION_CACHE_SIZE_ATTR, ssl.getTlsSessionCacheSize(), TLS_SESSION_CACHE_SIZE_PROP, null);
            } else {
                //key store settings
                setAttribute(serverSF, KEYSTORE_ATTR, null, KEYSTORE_PROP, null);
                setAttribute(serverSF, ADDITIONAL_KEY_STORES_ATTR, null, ADDITIONAL_KEY_STORES_PROP, null);
                setAttribute(serverSF, KEYSTORE_TYPE_ATTR, null, KEYSTORE_TYPE_PROP, DEFAULT_KEYSTORE_TYPE);
                setAttribute(serverSF, KEYSTORE_PASS_ATTR, null, KEYSTORE_PASS_PROP, DEFAULT_KEYSTORE_PASSWORD);
                
                //trust store settings
                setAttribute(serverSF, TRUSTSTORE_ATTR, null, TRUSTSTORE_PROP, null);
                setAttribute(serverSF, ADDITIONAL_TRUST_STORES_ATTR, null, ADDITIONAL_TRUST_STORES_PROP, null);
                setAttribute(serverSF, TRUSTSTORE_TYPE_ATTR, null, TRUSTSTORE_TYPE_PROP, DEFAULT_KEYSTORE_TYPE);
                setAttribute(serverSF, TRUSTSTORE_PASS_ATTR , null, TRUSTSTORE_PASS_PROP , DEFAULT_KEYSTORE_PASSWORD);
                
            }
            
            // cert nick name
            String certAlias = ssl != null ? ssl.getCertNickname() : null;
            if (sniCertAlias != null) {
                certAlias = sniCertAlias;
            }
            serverSF.setAttribute("keyAlias", certAlias);
            serverSF.init();
            newSslContext = serverSF.getSSLContext();
            CipherInfo.updateCiphers(newSslContext);
        } catch (IOException e) {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.log(Level.WARNING,
                        LogMessages.WARNING_GRIZZLY_CONFIG_SSL_GENERAL_CONFIG_ERROR(),
                        e);
            }
        }

        return newSslContext;
    }

    protected void logEmptyWarning(Ssl ssl, final String msg) {
        final StringBuilder name = new StringBuilder();
        for (NetworkListener listener : ((Protocol) ssl.getParent()).findNetworkListeners()) {
            if (name.length() != 0) {
                name.append(", ");
            }
            name.append(listener.getName());
        }
        LOGGER.log(Level.FINE, msg, name.toString());
    }

    @SuppressWarnings("UnusedDeclaration")
    public boolean isAllowLazyInit() {
        return ssl == null || Boolean.parseBoolean(ssl.getAllowLazyInit());
    }

    private static void setAttribute(final ServerSocketFactory serverSF, final String name, final String value,
            final String property, final String defaultValue) {
        serverSF.setAttribute(name, value == null ? System.getProperty(property, defaultValue) : value);
    }

    private static boolean isWantClientAuth(final Ssl ssl) {
        final String auth = ssl.getClientAuth();
        return auth != null && "want".equalsIgnoreCase(auth.trim());

    }

    private static boolean isNeedClientAuth(final Ssl ssl) {
        if (Boolean.parseBoolean(ssl.getClientAuthEnabled())) {
            return true;
        }

        final String auth = ssl.getClientAuth();
        return auth != null && "need".equalsIgnoreCase(auth.trim());

    }

    private static SSLImplementation lookupSSLImplementation(
            final ServiceLocator habitat, final Ssl ssl) {

        try {
            final String sslImplClassName = ssl.getClassname();
            if (sslImplClassName != null) {
                final SSLImplementation impl = Utils.newInstance(habitat,
                        SSLImplementation.class,
                        sslImplClassName, sslImplClassName);

                if (impl != null) {
                    return impl;
                } else {
                    if (LOGGER.isLoggable(Level.WARNING)) {
                        LOGGER.warning(LogMessages.WARNING_GRIZZLY_CONFIG_SSL_SSL_IMPLEMENTATION_LOAD_ERROR(sslImplClassName));
                    }
                    return SSLImplementation.getInstance();
                }
            } else {
                return SSLImplementation.getInstance();
            }
        } catch (Exception e) {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.log(Level.WARNING,
                        LogMessages.WARNING_GRIZZLY_CONFIG_SSL_GENERAL_CONFIG_ERROR(),
                        e);
            }
        }

        return null;
    }

    /**
     * Evaluates the given List of cipher suite names, converts each cipher suite that is enabled (i.e., not preceded by
     * a '-') to the corresponding JSSE cipher suite name, and returns a String[] of enabled cipher suites.
     *
     * @param configuredCiphers List of SSL ciphers to evaluate.
     *
     * @return String[] of cipher suite names, or null if none of the cipher suites in the given List are enabled or can
     *         be mapped to corresponding JSSE cipher suite names
     */
    private static String[] getJSSECiphers(final List<String> configuredCiphers) {
        Set<String> enabledCiphers = null;
        for (String cipher : configuredCiphers) {
            if (cipher.length() > 0 && cipher.charAt(0) != '-') {
                if (cipher.charAt(0) == '+') {
                    cipher = cipher.substring(1);
                }
                final String jsseCipher = getJSSECipher(cipher);
                if (jsseCipher == null) {
                    if (LOGGER.isLoggable(Level.WARNING)) {
                        LOGGER.warning(LogMessages.WARNING_GRIZZLY_CONFIG_SSL_UNKNOWN_CIPHER_ERROR(cipher));
                    }
                } else {
                    if (enabledCiphers == null) {
                        enabledCiphers = new HashSet<String>(configuredCiphers.size());
                    }
                    enabledCiphers.add(jsseCipher);
                }
            }
        }
        return ((enabledCiphers == null)
                ? null
                : enabledCiphers.toArray(new String[enabledCiphers.size()]));
    }
    /*
     * Converts the given cipher suite name to the corresponding JSSE cipher.
     *
     * @param cipher The cipher suite name to convert
     *
     * @return The corresponding JSSE cipher suite name, or null if the given
     * cipher suite name can not be mapped
     */

    private static String getJSSECipher(final String cipher) {
        final CipherInfo ci = CipherInfo.getCipherInfo(cipher);
        return ((ci != null) ? ci.getCipherName() : null);

    }
    // ---------------------------------------------------------- Nested Classes

    private final class InternalSSLContextConfigurator extends SSLContextConfigurator {

        public InternalSSLContextConfigurator() {
            super(false);
        }

        @Override
        public SSLContext createSSLContext() {
            return configureSSL();
        }
        
        // Grizzly 2.3.28 introduced a new method on the base class which must be overridden
        @Override
        public SSLContext createSSLContext(boolean throwException) {
            return configureSSL();
        }

        @Override
        public boolean validateConfiguration(boolean needsKeyStore) {
            return super.validateConfiguration(needsKeyStore);
        }

        @Override
        public void setKeyManagerFactoryAlgorithm(String keyManagerFactoryAlgorithm) {
            throw new IllegalStateException("The configuration is immutable");
        }

        @Override
        public void setKeyPass(String keyPass) {
            throw new IllegalStateException("The configuration is immutable");
        }

        @Override
        public void setKeyPass(char[] keyPass) {
            throw new IllegalStateException("The configuration is immutable");
        }

        @Override
        public void setKeyStoreFile(String keyStoreFile) {
            throw new IllegalStateException("The configuration is immutable");
        }

        @Override
        public void setKeyStorePass(String keyStorePass) {
            throw new IllegalStateException("The configuration is immutable");
        }

        @Override
        public void setKeyStorePass(char[] keyStorePass) {
            throw new IllegalStateException("The configuration is immutable");
        }

        @Override
        public void setKeyStoreProvider(String keyStoreProvider) {
            throw new IllegalStateException("The configuration is immutable");
        }

        @Override
        public void setKeyStoreType(String keyStoreType) {
            throw new IllegalStateException("The configuration is immutable");
        }

        @Override
        public void setSecurityProtocol(String securityProtocol) {
            throw new IllegalStateException("The configuration is immutable");
        }

        @Override
        public void setTrustManagerFactoryAlgorithm(String trustManagerFactoryAlgorithm) {
            throw new IllegalStateException("The configuration is immutable");
        }

        @Override
        public void setTrustStoreFile(String trustStoreFile) {
            throw new IllegalStateException("The configuration is immutable");
        }

        @Override
        public void setTrustStorePass(String trustStorePass) {
            throw new IllegalStateException("The configuration is immutable");
        }

        @Override
        public void setTrustStoreProvider(String trustStoreProvider) {
            throw new IllegalStateException("The configuration is immutable");
        }

        @Override
        public void setTrustStoreType(String trustStoreType) {
            throw new IllegalStateException("The configuration is immutable");
        }
       
    }

    private String getKeyStorePassword(Ssl ssl) {
        if (PLAIN_PASSWORD_PROVIDER_NAME.equalsIgnoreCase(ssl.getKeyStorePasswordProvider())) {
            return ssl.getKeyStorePassword();
        } else {
            return getStorePasswordCustom(ssl.getKeyStorePassword());
        }
    }

    private String getTrustStorePassword(Ssl ssl) {
        if (PLAIN_PASSWORD_PROVIDER_NAME.equalsIgnoreCase(ssl.getTrustStorePasswordProvider())) {
            return ssl.getTrustStorePassword();
        } else {
            return getStorePasswordCustom(ssl.getTrustStorePassword());
        }
    }

    private String getStorePasswordCustom(String storePasswordProvider) {
        try {
            final SecurePasswordProvider provider =
                    (SecurePasswordProvider) Utils.newInstance(storePasswordProvider);
            
            assert provider != null;
            return provider.getPassword();
        } catch (Exception e) {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.log(Level.WARNING,
                        LogMessages.WARNING_GRIZZLY_CONFIG_SSL_SECURE_PASSWORD_INITIALIZATION_ERROR(storePasswordProvider),
                        e);
            }
        }
        return null;
    }

    /**
     * This class represents the information associated with ciphers. It also maintains a Map from configName to
     * CipherInfo.
     */
    private static final class CipherInfo {
        private static final short TLS = 0x4;
        // The old names mapped to the standard names as existed
        private static final String[][] OLD_CIPHER_MAPPING = {
            // IWS 6.x or earlier
            {"rsa_null_md5", "SSL_RSA_WITH_NULL_MD5"},
            {"rsa_null_sha", "SSL_RSA_WITH_NULL_SHA"},
            {"rsa_rc4_40_md5", "SSL_RSA_EXPORT_WITH_RC4_40_MD5"},
            {"rsa_rc4_128_md5", "SSL_RSA_WITH_RC4_128_MD5"},
            {"rsa_rc4_128_sha", "SSL_RSA_WITH_RC4_128_SHA"},
            {"rsa_3des_sha", "SSL_RSA_WITH_3DES_EDE_CBC_SHA"},
            {"fips_des_sha", "SSL_RSA_WITH_DES_CBC_SHA"},
            {"rsa_des_sha", "SSL_RSA_WITH_DES_CBC_SHA"},
            // backward compatible with AS 9.0 or earlier
            {"SSL_RSA_WITH_NULL_MD5", "SSL_RSA_WITH_NULL_MD5"},
            {"SSL_RSA_WITH_NULL_SHA", "SSL_RSA_WITH_NULL_SHA"}
        };

        private static final Map<String,CipherInfo> ciphers =
                new HashMap<String,CipherInfo>();
        private static final ReadWriteLock ciphersLock = new ReentrantReadWriteLock();

        @SuppressWarnings({"UnusedDeclaration"})
        private final String configName;
        private final String cipherName;
        private final short protocolVersion;

        static {
            for (int i = 0, len = OLD_CIPHER_MAPPING.length; i < len; i++) {
                String nonStdName = OLD_CIPHER_MAPPING[i][0];
                String stdName = OLD_CIPHER_MAPPING[i][1];
                ciphers.put(nonStdName,
                        new CipherInfo(nonStdName, stdName, TLS));
            }
        }

        /**
         * @param configName name used in domain.xml, sun-acc.xml
         * @param cipherName name that may depends on backend
         * @param protocolVersion
         */
        private CipherInfo(final String configName,
                final String cipherName,
                final short protocolVersion) {
            this.configName = configName;
            this.cipherName = cipherName;
            this.protocolVersion = protocolVersion;
        }

        public static void updateCiphers(final SSLContext sslContext) {
            SSLServerSocketFactory factory = sslContext.getServerSocketFactory();
            String[] supportedCiphers = factory.getDefaultCipherSuites();
            
            ciphersLock.writeLock().lock();
            try {
                for (int i = 0, len = supportedCiphers.length; i < len; i++) {
                    String s = supportedCiphers[i];
                    ciphers.put(s, new CipherInfo(s, s, TLS));
                }
            } finally {
                ciphersLock.writeLock().unlock();
            }
        }

        public static CipherInfo getCipherInfo(final String configName) {
            ciphersLock.readLock().lock();
            try {
                return ciphers.get(configName);
            } finally {
                ciphersLock.readLock().unlock();
            }
        }

        public String getConfigName() {
            return configName;
        }

        public String getCipherName() {
            return cipherName;
        }

        @SuppressWarnings({"UnusedDeclaration"})
        public boolean isTLS() {
            return (protocolVersion & TLS) == TLS;
        }
    } // END CipherInfo
}
